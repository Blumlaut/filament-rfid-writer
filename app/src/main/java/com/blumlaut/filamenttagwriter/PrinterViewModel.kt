package com.blumlaut.filamenttagwriter

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blumlaut.filamenttagwriter.data.local.FilamentDatabase
import com.blumlaut.filamenttagwriter.data.local.FilamentEntity
import com.blumlaut.filamenttagwriter.data.local.PrinterEntity
import com.blumlaut.filamenttagwriter.data.model.CanvasMaterialData
import com.blumlaut.filamenttagwriter.data.model.CanvasTray
import com.blumlaut.filamenttagwriter.data.model.Filament
import com.blumlaut.filamenttagwriter.data.model.Printer
import com.blumlaut.filamenttagwriter.data.model.PrinterStatus
import com.blumlaut.filamenttagwriter.data.model.PrinterAttributes
import com.blumlaut.filamenttagwriter.network.CanvasWebSocket
import com.blumlaut.filamenttagwriter.network.ConnectionState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * ViewModel managing printer connections, CANVAS polling, and filament import.
 */
class PrinterViewModel(private val database: FilamentDatabase) : ViewModel() {

    private val printerDao = database.printerDao()
    private val filamentDao = database.filamentDao()

    // --- Saved printers ---
    private val _printers = kotlinx.coroutines.flow.MutableStateFlow<List<Printer>>(emptyList())
    val printers: StateFlow<List<Printer>> = _printers

    init {
        viewModelScope.launch {
            printerDao.getAll().collect { entities ->
                _printers.value = entities.map { it.toPrinter() }
            }
        }
        viewModelScope.launch {
            filamentDao.getAll().collect { entities ->
                _catalogFilaments.value = entities
            }
        }
    }

    // --- Active WebSocket connections (one per printer) ---
    private val webSockets = mutableMapOf<String, CanvasWebSocket>()
    private val pollJobs = mutableMapOf<String, Job>()

    // --- Material data per printer ---
    private val _materialData = mutableStateOf<Map<String, CanvasMaterialData>>(emptyMap())
    val materialData: Map<String, CanvasMaterialData>
        get() = _materialData.value

    // --- Printer status per printer ---
    private val _printerStatus = mutableStateOf<Map<String, PrinterStatus>>(emptyMap())
    val printerStatus: Map<String, PrinterStatus>
        get() = _printerStatus.value

    // --- Printer attributes per printer ---
    private val _printerAttributes = mutableStateOf<Map<String, PrinterAttributes>>(emptyMap())
    val printerAttributes: Map<String, PrinterAttributes>
        get() = _printerAttributes.value

    // --- Connection states per printer ---
    private val _connectionStates = mutableStateOf<Map<String, ConnectionState>>(emptyMap())
    val connectionStates: Map<String, ConnectionState>
        get() = _connectionStates.value

    // --- IP validation ---
    var ipValidationState: MutableState<IpValidationState> = mutableStateOf(IpValidationState.Idle)
    private var validationJob: Job? = null

    // --- Active printer being viewed (for tray detail) ---
    var activePrinterId: MutableState<String?> = mutableStateOf(null)

    // --- Import status ---
    var importingPrinterId: MutableState<String?> = mutableStateOf(null)

    // --- Cached catalog for tray duplicate checks ---
    private val _catalogFilaments = mutableStateOf<List<FilamentEntity>>(emptyList())

    /**
     * Check if a tray's filament is already in the catalog.
     */
    fun isTrayInCatalog(tray: CanvasTray): Boolean {
        if (!tray.hasFilament) return true // Empty trays are "done"
        val filament = tray.toFilament()
        return _catalogFilaments.value.any { entity ->
            entity.name.equals(filament.name, ignoreCase = true) &&
            entity.colorRgb == filament.colorRgb
        }
    }

    /**
     * Check if all trays for a printer are already in the catalog.
     */
    fun allTraysInCatalog(printerId: String): Boolean {
        val trays = materialData[printerId]?.allTrays ?: return true
        return trays.isEmpty() || trays.all { isTrayInCatalog(it) }
    }

    /** Poll interval in seconds. */
    private val POLL_INTERVAL_SECONDS = 5L

    /**
     * Validate an IP address by attempting a WebSocket connection.
     * Uses the same reachability check as the polling logic.
     * Debounced: calling again cancels the previous validation.
     */
    fun validateIp(ipAddress: String) {
        if (ipAddress.isBlank()) {
            ipValidationState.value = IpValidationState.Idle
            validationJob?.cancel()
            return
        }

        // Clean IP (remove port if user pasted "192.168.1.1:3030")
        val cleanIp = ipAddress.split(":").first().trim()

        validationJob?.cancel()
        ipValidationState.value = IpValidationState.Validating
        validationJob = viewModelScope.launch(Dispatchers.IO) {
            val reachable = CanvasWebSocket.isPrinterReachable(cleanIp, port = 3030)
            withContext(Dispatchers.Main) {
                ipValidationState.value = if (reachable) {
                    IpValidationState.Valid
                } else {
                    IpValidationState.Invalid
                }
            }
        }
    }

    /**
     * Save a printer to the database and connect to it.
     */
    fun addPrinter(name: String, ipAddress: String, port: Int = 3030) {
        val cleanIp = ipAddress.split(":").first().trim()
        Log.d(TAG, "addPrinter: name=$name, ip=$cleanIp, port=$port")
        viewModelScope.launch {
            val id = UUID.randomUUID().toString()
            val entity = PrinterEntity(
                id = id,
                name = name.ifBlank { cleanIp },
                ipAddress = cleanIp,
                port = port,
                lastSeen = System.currentTimeMillis(),
            )
            printerDao.insert(entity)
            // Connect directly with printer details (no need to wait for flow)
            connectPrinter(entity.toPrinter())
        }
        // Reset validation
        ipValidationState.value = IpValidationState.Idle
    }

    /**
     * Delete a printer and disconnect.
     */
    fun removePrinter(printer: Printer) {
        disconnectPrinter(printer.id)
        viewModelScope.launch {
            printerDao.delete(printer.toEntity())
        }
    }

    /**
     * Connect to a printer's WebSocket and start polling.
     */
    fun connectPrinter(printerId: String) {
        val printer = _printers.value.find { it.id == printerId }
        if (printer == null) {
            Log.w(TAG, "Printer not found in list: $printerId, list=${_printers.value.map { "${it.id}:${it.ipAddress}:${it.port}" }}")
            return
        }
        connectPrinter(printer)
    }

    /**
     * Connect to a printer's WebSocket and start polling (internal, takes Printer directly).
     */
    private fun connectPrinter(printer: Printer) {
        disconnectPrinter(printer.id) // clean up any existing connection

        Log.d(TAG, "connectPrinter: id=${printer.id}, ip=${printer.ipAddress}, port=${printer.port}")
        val ws = CanvasWebSocket(printer.ipAddress, printer.port)

        ws.onConnectionStateChanged = { state ->
            // Suppress intermediate state changes during polling to avoid UI flicker.
            // Only update for: initial Connected, initial Error, or explicit Disconnect.
            val currentState = _connectionStates.value[printer.id]
            val shouldUpdate = when (state) {
                is ConnectionState.Connected -> currentState != ConnectionState.Connected
                is ConnectionState.Connecting -> currentState == null || currentState == ConnectionState.Disconnected
                is ConnectionState.Error -> currentState !is ConnectionState.Connected
                is ConnectionState.Disconnected -> false
            }
            if (shouldUpdate) {
                _connectionStates.value = _connectionStates.value.toMutableMap().apply {
                    put(printer.id, state)
                }
            }
            if (state == ConnectionState.Connected) {
                // Update lastSeen
                viewModelScope.launch {
                    val entity = printerDao.getById(printer.id)
                    entity?.let {
                        printerDao.insert(it.copy(lastSeen = System.currentTimeMillis()))
                    }
                }
            }
        }

        ws.onPrinterStatus = { status ->
            _printerStatus.value = _printerStatus.value.toMutableMap().apply {
                put(printer.id, status)
            }
        }

        ws.onPrinterAttributes = { attrs ->
            _printerAttributes.value = _printerAttributes.value.toMutableMap().apply {
                put(printer.id, attrs)
            }
        }

        ws.onMaterialData = { data ->
            _materialData.value = _materialData.value.toMutableMap().apply {
                put(printer.id, data)
            }
            // Update lastSeen on data receipt
            viewModelScope.launch {
                val entity = printerDao.getById(printer.id)
                entity?.let {
                    printerDao.insert(it.copy(lastSeen = System.currentTimeMillis()))
                }
            }
        }

        ws.onError = { message ->
            Log.e(TAG, "Printer ${printer.id} error: $message")
        }

        webSockets[printer.id] = ws
        ws.connect()

        // Start polling job
        val job = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(POLL_INTERVAL_SECONDS * 1000)
                // Printer closes socket after each response, so always reconnect+poll
                if (ws.connectionState != ConnectionState.Connected) {
                    Log.d(TAG, "Polling: reconnecting printer ${printer.id}")
                    ws.connect()
                    delay(1500) // Wait for connection + response
                } else {
                    ws.requestMaterialData()
                }
            }
        }
        pollJobs[printer.id] = job

        // Initial request
        viewModelScope.launch(Dispatchers.IO) {
            delay(1000) // Give connection time to establish
            if (ws.connectionState == ConnectionState.Connected) {
                ws.requestMaterialData()
            }
        }
    }

    /**
     * Disconnect from a printer and stop polling.
     */
    fun disconnectPrinter(printerId: String) {
        pollJobs[printerId]?.cancel()
        pollJobs.remove(printerId)
        webSockets[printerId]?.cleanup()
        webSockets.remove(printerId)
        _connectionStates.value = _connectionStates.value.toMutableMap().apply {
            put(printerId, ConnectionState.Disconnected)
        }
    }

    /**
     * Auto-connect all printers that aren't already connected/connecting.
     * Called when the Printer tab becomes visible.
     */
    fun autoConnectIfNeeded() {
        _printers.value.forEach { printer ->
            val state = _connectionStates.value[printer.id]
            if (state != ConnectionState.Connected && state != ConnectionState.Connecting) {
                connectPrinter(printer.id)
            }
        }
    }

    /**
     * Disconnect all printers.
     * Called when the Printer tab is hidden (to save battery).
     */
    fun disconnectAll() {
        _printers.value.forEach { printer ->
            disconnectPrinter(printer.id)
        }
    }

    /**
     * Import a tray's filament data into the catalog.
     * If a filament with the same name+color already exists, skip it.
     */
    fun importTrayToCatalog(printerId: String, tray: CanvasTray) {
        importingPrinterId.value = printerId
        viewModelScope.launch {
            try {
                val filament = tray.toFilament()
                // Check if already in catalog (by name + color match)
                val existing = filamentDao.getAll().first().find { entity ->
                    entity.name.equals(filament.name, ignoreCase = true) &&
                    entity.colorRgb == filament.colorRgb
                }

                if (existing == null) {
                    val newFilament = filament.copy(id = UUID.randomUUID().toString())
                    val entity = FilamentEntity(
                        id = newFilament.id,
                        name = newFilament.name,
                        manufacturerCode = newFilament.manufacturerCode,
                        material = newFilament.material,
                        subtypeCode = newFilament.subtypeCode,
                        subtype = newFilament.subtype,
                        colorRgb = newFilament.colorRgb,
                        colorModifier = newFilament.colorModifier,
                        minTemp = newFilament.minTemp,
                        maxTemp = newFilament.maxTemp,
                        diameter = newFilament.diameter,
                        weight = newFilament.weight,
                        productionDateRaw = newFilament.productionDateRaw,
                    )
                    filamentDao.insert(entity)
                }
            } finally {
                delay(500) // Brief visual feedback
                importingPrinterId.value = null
            }
        }
    }

    /**
     * Import all trays from a printer at once.
     */
    fun importAllTrays(printerId: String) {
        val data = _materialData.value[printerId] ?: return
        importingPrinterId.value = printerId
        viewModelScope.launch {
            try {
                for (tray in data.allTrays) {
                    if (!tray.hasFilament) continue
                    val filament = tray.toFilament()
                    val existing = filamentDao.getAll().first().find { entity ->
                        entity.name.equals(filament.name, ignoreCase = true) &&
                        entity.colorRgb == filament.colorRgb
                    }
                    if (existing == null) {
                        val newFilament = filament.copy(id = UUID.randomUUID().toString())
                        val entity = FilamentEntity(
                            id = newFilament.id,
                            name = newFilament.name,
                            manufacturerCode = newFilament.manufacturerCode,
                            material = newFilament.material,
                            subtypeCode = newFilament.subtypeCode,
                            subtype = newFilament.subtype,
                            colorRgb = newFilament.colorRgb,
                            colorModifier = newFilament.colorModifier,
                            minTemp = newFilament.minTemp,
                            maxTemp = newFilament.maxTemp,
                            diameter = newFilament.diameter,
                            weight = newFilament.weight,
                            productionDateRaw = newFilament.productionDateRaw,
                        )
                        filamentDao.insert(entity)
                    }
                }
            } finally {
                delay(800)
                importingPrinterId.value = null
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        validationJob?.cancel()
        _printers.value.forEach { disconnectPrinter(it.id) }
    }

    // --- Entity conversions ---
    private fun PrinterEntity.toPrinter(): Printer {
        return Printer(
            id = id,
            name = name,
            ipAddress = ipAddress,
            port = port,
            lastSeen = lastSeen,
        )
    }

    private fun Printer.toEntity(): PrinterEntity {
        return PrinterEntity(
            id = id,
            name = name,
            ipAddress = ipAddress,
            port = port,
            lastSeen = lastSeen,
        )
    }

    companion object {
        private const val TAG = "PrinterViewModel"
    }
}

/**
 * States for IP address validation.
 */
sealed class IpValidationState {
    object Idle : IpValidationState()
    object Validating : IpValidationState()
    object Valid : IpValidationState()
    object Invalid : IpValidationState()
}
