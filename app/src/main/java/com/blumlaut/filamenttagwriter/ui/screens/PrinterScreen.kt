package com.blumlaut.filamenttagwriter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.blumlaut.filamenttagwriter.IpValidationState
import com.blumlaut.filamenttagwriter.PrinterViewModel
import com.blumlaut.filamenttagwriter.data.model.CanvasTray
import com.blumlaut.filamenttagwriter.data.model.PrintState
import com.blumlaut.filamenttagwriter.data.model.Printer
import com.blumlaut.filamenttagwriter.network.ConnectionState
import com.blumlaut.filamenttagwriter.ui.components.ColorSwatch
import com.blumlaut.filamenttagwriter.ui.components.isLightColor
import com.blumlaut.filamenttagwriter.ui.components.parseHexColor
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrinterScreen(
    viewModel: PrinterViewModel,
) {
    val printers by viewModel.printers.collectAsStateWithLifecycle()
    val materialData = viewModel.materialData
    val printerStatus = viewModel.printerStatus
    val printerAttributes = viewModel.printerAttributes
    val connectionStates = viewModel.connectionStates
    val importingPrinterId = viewModel.importingPrinterId.value

    var showAddDialog by remember { mutableStateOf(false) }
    var deleteCandidate by remember { mutableStateOf<Printer?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current

    // Auto-connect when tab becomes visible, disconnect when hidden
    DisposableEffect(lifecycleOwner) {
        val lifecycle = lifecycleOwner.lifecycle
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> {
                    viewModel.autoConnectIfNeeded()
                }
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> {
                    viewModel.disconnectAll()
                }
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Printers") },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add Printer")
                    }
                },
            )
        },
    ) { padding ->
        if (printers.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.CallReceived,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No printers added",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Add a printer to view its status and CANVAS AMS filament slots.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(24.dp))
                FilledTonalButton(
                    onClick = { showAddDialog = true },
                    shape = MaterialTheme.shapes.extraLarge,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Printer", fontWeight = FontWeight.Bold)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(printers, key = { it.id }) { printer ->
                    PrinterCard(
                        printer = printer,
                        connectionState = connectionStates[printer.id],
                        materialData = materialData[printer.id],
                        printerStatus = printerStatus[printer.id],
                        _printerAttributes = printerAttributes[printer.id],
                        isImporting = importingPrinterId == printer.id,
                        isTrayInCatalog = { tray -> viewModel.isTrayInCatalog(tray) },
                        allCatalogued = viewModel.allTraysInCatalog(printer.id),
                        onConnect = { viewModel.connectPrinter(printer.id) },
                        onDisconnect = { viewModel.disconnectPrinter(printer.id) },
                        onDelete = {
                            deleteCandidate = printer
                            showDeleteDialog = true
                        },
                        onImportTray = { tray ->
                            viewModel.importTrayToCatalog(printer.id, tray)
                        },
                        onImportAll = { viewModel.importAllTrays(printer.id) },
                    )
                }
            }
        }
    }

    // Add printer dialog
    if (showAddDialog) {
        AddPrinterDialog(
            viewModel = viewModel,
            onDismiss = { showAddDialog = false },
        )
    }

    // Delete confirmation dialog
    if (showDeleteDialog && deleteCandidate != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Remove Printer") },
            text = { Text("Remove '${deleteCandidate?.name}' from your printers?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteCandidate?.let { viewModel.removePrinter(it) }
                        showDeleteDialog = false
                        deleteCandidate = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun PrinterCard(
    printer: Printer,
    connectionState: ConnectionState?,
    materialData: com.blumlaut.filamenttagwriter.data.model.CanvasMaterialData?,
    printerStatus: com.blumlaut.filamenttagwriter.data.model.PrinterStatus?,
    _printerAttributes: com.blumlaut.filamenttagwriter.data.model.PrinterAttributes?, // unused, reserved for future use
    isImporting: Boolean,
    isTrayInCatalog: (CanvasTray) -> Boolean,
    allCatalogued: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onDelete: () -> Unit,
    onImportTray: (CanvasTray) -> Unit,
    onImportAll: () -> Unit,
) {
    val isConnected = connectionState == ConnectionState.Connected
    val isConnecting = connectionState == ConnectionState.Connecting
    val hasCachedData = materialData != null && materialData.allTrays.isNotEmpty()

    // M3 Expressive: extraLarge shape for printer card
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: printer name, IP, connection status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Connection status dot
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(
                                    when (connectionState) {
                                        is ConnectionState.Connected -> MaterialTheme.colorScheme.primary
                                        is ConnectionState.Connecting -> MaterialTheme.colorScheme.tertiary
                                        is ConnectionState.Error -> MaterialTheme.colorScheme.error
                                        else -> MaterialTheme.colorScheme.outlineVariant
                                    }
                                ),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = printer.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = "${printer.ipAddress}:${printer.port}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Connection toggle + delete
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilledTonalIconButton(
                        onClick = {
                            if (isConnected || isConnecting) onDisconnect() else onConnect()
                        },
                        shape = CircleShape,
                    ) {
                        Icon(
                            imageVector = if (isConnected || isConnecting) Icons.Filled.Wifi else Icons.Filled.WifiOff,
                            contentDescription = if (isConnected) "Disconnect" else "Connect",
                            tint = if (isConnected) MaterialTheme.colorScheme.onSecondaryContainer
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Delete printer",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            // Connection status text
            if (isConnecting) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Connecting...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else if (connectionState is ConnectionState.Error && !hasCachedData) {
                Text(
                    text = "Error: ${connectionState.message}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            // Printer status overview (when connected or has recent data)
            if (printerStatus != null) {
                Spacer(modifier = Modifier.height(12.dp))
                PrinterStatusOverview(status = printerStatus)
            }

            // Tray grid - always show if we have cached data, or when connected
            if (isConnected || hasCachedData) {
                Spacer(modifier = Modifier.height(12.dp))
                CanvasTrayGrid(
                    materialData = materialData,
                    isConnected = isConnected,
                    isImporting = isImporting,
                    isTrayInCatalog = isTrayInCatalog,
                    allCatalogued = allCatalogued,
                    onImportTray = onImportTray,
                    onImportAll = onImportAll,
                )
            }
        }
    }
}

/**
 * Compact printer status overview: state badge, temperatures, print progress.
 */
@Composable
private fun PrinterStatusOverview(
    status: com.blumlaut.filamenttagwriter.data.model.PrinterStatus,
) {
    val printState = status.printState
    val printInfo = status.printInfo

    // State color
    val stateColor = when (printState) {
        PrintState.Printing -> MaterialTheme.colorScheme.primary
        PrintState.Paused, PrintState.PausedAlt -> MaterialTheme.colorScheme.tertiary
        PrintState.Done -> MaterialTheme.colorScheme.secondary
        PrintState.Error -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    // State label
    val stateLabel = when (printState) {
        PrintState.Printing -> "Printing"
        PrintState.Paused, PrintState.PausedAlt -> "Paused"
        PrintState.Done -> "Done"
        PrintState.Error -> "Error"
        else -> if (status.isHeating) "Heating" else "Idle"
    }

    // Surface container for the status section
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Top row: state badge + temperatures
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // State badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    // Animated state dot
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(stateColor),
                    )
                    Text(
                        text = stateLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = stateColor,
                    )
                }

                // Temperature pills
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TempPill(
                        icon = Icons.Filled.LocalFireDepartment,
                        current = status.tempNozzle,
                        target = status.tempNozzleTarget,
                        label = "Nozzle",
                    )
                    TempPill(
                        icon = Icons.Filled.GridOn,
                        current = status.tempHotbed,
                        target = status.tempHotbedTarget,
                        label = "Bed",
                    )
                }
            }

            // Print progress (only when there's a job)
            if (printInfo.hasJob) {
                Spacer(modifier = Modifier.height(8.dp))

                // Filename
                Text(
                    text = printInfo.filename.takeLastWhile { it != '/' },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                // Progress bar
                if (printInfo.progress > 0 || printState == PrintState.Printing) {
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { printInfo.progress.toFloat() / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(MaterialTheme.shapes.extraSmall),
                        color = stateColor,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    )
                }

                // Details row: layers + progress %
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (printInfo.totalLayer > 0) {
                            "Layer ${printInfo.currentLayer}/${printInfo.totalLayer}"
                        } else {
                            "—"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "${printInfo.progress}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun TempPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    current: Float,
    target: Float,
    label: String,
) {
    val isHeating = target > 0f

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(14.dp),
            tint = if (isHeating) MaterialTheme.colorScheme.errorContainer
                   else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column {
            Text(
                text = "${current.toInt()}°",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (isHeating) {
                Text(
                    text = "/ ${target.toInt()}°",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * 2x2 tray grid matching the printer's CANVAS AMS layout.
 */
@Composable
private fun CanvasTrayGrid(
    materialData: com.blumlaut.filamenttagwriter.data.model.CanvasMaterialData?,
    isConnected: Boolean,
    isImporting: Boolean,
    isTrayInCatalog: (CanvasTray) -> Boolean,
    allCatalogued: Boolean,
    onImportTray: (CanvasTray) -> Unit,
    onImportAll: () -> Unit,
) {
    val trays = materialData?.allTrays ?: emptyList()
    val activeTrayId = materialData?.activeTrayId ?: -1

    val slotCount = 4
    val traysMap = trays.associateBy { it.trayId }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Import all button
        if (!allCatalogued) {
            FilledTonalButton(
                onClick = onImportAll,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isImporting && isConnected,
            ) {
                if (isImporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Import All New Filaments", fontWeight = FontWeight.Bold)
            }
        }

        // 2x2 grid
        Grid2x2TrayDisplay(
            slotCount = slotCount,
            traysMap = traysMap,
            activeTrayId = activeTrayId,
            isTrayInCatalog = isTrayInCatalog,
            onImportTray = onImportTray,
        )

        // Status indicator
        if (!isConnected) {
            Row(
                modifier = Modifier.padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 1.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Cached data — reconnecting...",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

@Composable
private fun Grid2x2TrayDisplay(
    slotCount: Int,
    traysMap: Map<Int, CanvasTray>,
    activeTrayId: Int,
    isTrayInCatalog: (CanvasTray) -> Boolean,
    onImportTray: (CanvasTray) -> Unit,
) {
    val slots = (0 until slotCount).map { index ->
        traysMap[index] ?: CanvasTray(
            trayId = index,
            filamentType = "None",
            filamentColor = "",
        )
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        // Left column (slots 1, 2)
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f),
        ) {
            slots.getOrNull(0)?.let {
                TraySlot(tray = it, isActive = it.trayId == activeTrayId,
                    isInCatalog = isTrayInCatalog(it), onImport = onImportTray)
            }
            slots.getOrNull(1)?.let {
                TraySlot(tray = it, isActive = it.trayId == activeTrayId,
                    isInCatalog = isTrayInCatalog(it), onImport = onImportTray)
            }
        }
        // Right column (slots 4, 3)
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f),
        ) {
            slots.getOrNull(3)?.let {
                TraySlot(tray = it, isActive = it.trayId == activeTrayId,
                    isInCatalog = isTrayInCatalog(it), onImport = onImportTray)
            }
            slots.getOrNull(2)?.let {
                TraySlot(tray = it, isActive = it.trayId == activeTrayId,
                    isInCatalog = isTrayInCatalog(it), onImport = onImportTray)
            }
        }
    }
}

@Composable
private fun TraySlot(
    tray: CanvasTray,
    isActive: Boolean,
    isInCatalog: Boolean,
    onImport: (CanvasTray) -> Unit,
) {
    val colorRgb = tray.filamentColor.parseHexColor(0x808080)
    val trayColor = if (tray.hasFilament) {
        Color(0xFF000000L or (colorRgb and 0xFFFFFF).toLong())
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        // Slot number badge
        MaterialTheme(
            colorScheme = MaterialTheme.colorScheme.copy(
                primary = MaterialTheme.colorScheme.outlineVariant,
            ),
        ) {
            Surface(
                modifier = Modifier.size(24.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "${tray.trayId + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Colored circle (filament swatch)
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(trayColor)
                .then(
                    if (tray.hasFilament) {
                        Modifier.border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    } else {
                        Modifier.border(2.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), CircleShape)
                    }
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (tray.hasFilament) {
                if (isActive) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.25f)),
                    )
                }
                Text(
                    text = tray.filamentType.take(6),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isLightColor(colorRgb)) Color.Black else Color.White,
                    fontWeight = FontWeight.Bold,
                )
            } else {
                Icon(
                    Icons.Filled.Circle,
                    contentDescription = "Empty",
                    tint = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Tray details + import button
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            if (tray.hasFilament) {
                Text(
                    text = tray.filamentName.ifBlank { tray.filamentType },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
                    if (isActive) {
                        Text(
                            text = "●",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "Active",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    if (tray.hasNfcTag) {
                        Text(
                            text = "NFC",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (isInCatalog) {
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 2.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Text(
                                text = "Saved",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                } else {
                    FilledIconButton(
                        onClick = { onImport(tray) },
                        shape = CircleShape,
                        modifier = Modifier.size(24.dp),
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "Save to catalog",
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            } else {
                Text(
                    text = "Empty",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPrinterDialog(
    viewModel: PrinterViewModel,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var ip by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("3030") }

    LaunchedEffect(ip) {
        if (ip.isNotBlank()) {
            delay(800)
            viewModel.validateIp(ip)
        } else {
            viewModel.validateIp("")
        }
    }

    val validationState = viewModel.ipValidationState.value
    val isValidating = validationState is IpValidationState.Validating
    val isValid = validationState is IpValidationState.Valid
    val isInvalid = validationState is IpValidationState.Invalid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Printer") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name (optional)") },
                    placeholder = { Text("My Centauri Carbon") },
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Filled.DeviceHub, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                )

                OutlinedTextField(
                    value = ip,
                    onValueChange = { ip = it },
                    label = { Text("IP Address") },
                    placeholder = { Text("192.168.1.100") },
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = isInvalid,
                    supportingText = {
                        when {
                            isValidating -> Text("Checking connection...")
                            isValid -> Text("Printer found!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            isInvalid -> Text("Cannot reach printer", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    leadingIcon = {
                        when {
                            isValidating -> CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            isValid -> Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = "Valid",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            isInvalid -> Icon(
                                Icons.Filled.Cancel,
                                contentDescription = "Invalid",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp),
                            )
                            else -> Icon(Icons.Filled.Wifi, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = when {
                            isValid -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            isInvalid -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                            else -> Color.Unspecified
                        },
                    ),
                )

                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it.filter { c -> c.isDigit() } },
                    label = { Text("Port") },
                    placeholder = { Text("3030") },
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Filled.CallToAction, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = port.toIntOrNull() ?: 3030
                    viewModel.addPrinter(name, ip, p)
                    onDismiss()
                },
                enabled = ip.isNotBlank(),
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Text("Add & Connect", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
