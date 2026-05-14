package com.blumlaut.filamenttagwriter

import android.nfc.Tag
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blumlaut.filamenttagwriter.data.local.FilamentDatabase
import com.blumlaut.filamenttagwriter.data.local.FilamentEntity
import com.blumlaut.filamenttagwriter.data.model.Epc256Encoder
import com.blumlaut.filamenttagwriter.data.model.Filament
import com.blumlaut.filamenttagwriter.data.model.Materials
import com.blumlaut.filamenttagwriter.nfc.NfcReaderWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.UUID

/**
 * ViewModel for the filament catalog and NFC operations.
 */
class FilamentViewModel(private val database: FilamentDatabase) : ViewModel() {

    private val dao = database.filamentDao()

    /** Flow of all saved filaments from the database. */
    private val _catalog = kotlinx.coroutines.flow.MutableStateFlow<List<Filament>>(emptyList())
    val catalog: StateFlow<List<Filament>> = _catalog

    init {
        viewModelScope.launch {
            dao.getAll().collect { entities ->
                _catalog.value = entities.map { it.toFilament() }
            }
        }
    }

    /** Last detected NFC tag. Set by MainActivity when a tag is scanned. */
    var lastTag: MutableState<Tag?> = mutableStateOf(null)

    /** Result of the last NFC read operation. */
    var nfcReadResult: MutableState<NfcReaderWriter.ReadResult?> = mutableStateOf(null)

    /** Whether an NFC read is in progress. */
    var isReadingTag: MutableState<Boolean> = mutableStateOf(false)

    /** Current bottom-nav tab. Used to route NFC intents intelligently. */
    var currentTab: MutableState<String> = mutableStateOf("read")

    enum class Tab { READ, CATALOG, WRITE }

    /**
     * Save a filament to the catalog.
     */
    fun saveFilament(filament: Filament) {
        viewModelScope.launch {
            val entity = filament.toEntity()
            dao.insert(entity)
        }
    }

    /**
     * Delete a filament from the catalog.
     */
    fun deleteFilament(filament: Filament) {
        viewModelScope.launch {
            val entity = filament.toEntity()
            dao.delete(entity)
        }
    }

    /**
     * Get a filament by ID.
     */
    fun getFilamentById(id: String): Filament? {
        return _catalog.value.find { it.id == id }
    }

    /**
     * Clear the last detected NFC tag (call after processing).
     */
    fun clearLastTag() {
        lastTag.value = null
    }

    /**
     * Read filament data from a tag on a background thread.
     */
    fun readTag(tag: Tag) {
        nfcReadResult.value = null
        isReadingTag.value = true
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                NfcReaderWriter.readFilament(tag)
            }
            nfcReadResult.value = result
            isReadingTag.value = false
            Log.d("FilamentViewModel", "Read complete: ${result::class.simpleName}")
        }
    }

    /**
     * Clear the NFC read result.
     */
    fun clearNfcReadResult() {
        nfcReadResult.value = null
    }

    /**
     * Set the current tab and clear stale NFC results when switching.
     */
    fun setCurrentTab(tab: String) {
        if (currentTab.value != tab) {
            currentTab.value = tab
            if (tab != "read") {
                nfcReadResult.value = null
                isReadingTag.value = false
            }
            if (tab != "write") {
                nfcWriteResult.value = null
                isWritingTag.value = false
            }
        }
    }

    /** Selected filament for writing. */
    var selectedFilamentForWrite: MutableState<Filament?> = mutableStateOf(null)

    /** Result of the last NFC write operation. */
    var nfcWriteResult: MutableState<NfcReaderWriter.WriteResult?> = mutableStateOf(null)

    /** Whether an NFC write is in progress. */
    var isWritingTag: MutableState<Boolean> = mutableStateOf(false)

    /**
     * Write the selected filament to a tag on a background thread.
     */
    fun writeTag(tag: Tag) {
        val filament = selectedFilamentForWrite.value
        if (filament == null) {
            android.util.Log.w("FilamentViewModel", "No filament selected for write")
            return
        }
        nfcWriteResult.value = null
        isWritingTag.value = true
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                NfcReaderWriter.writeFilament(tag, filament)
            }
            nfcWriteResult.value = result
            isWritingTag.value = false
            Log.d("FilamentViewModel", "Write complete: ${result::class.simpleName}")
        }
    }

    /**
     * Clear the NFC write result.
     */
    fun clearNfcWriteResult() {
        nfcWriteResult.value = null
    }

    /**
     * Create a new empty filament with a unique ID and default values.
     */
    fun createNewFilament(): Filament {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR) % 100
        val month = cal.get(Calendar.MONTH) + 1
        val prodRaw = ((year shl 8) or month).toShort()
        return Filament(
            id = UUID.randomUUID().toString(),
            name = "",
            manufacturerCode = 0xEEEEEEEE.toInt(),
            material = "PLA",
            subtypeCode = 0x0000,
            subtype = "PLA",
            color = "#FFFFFF",
            colorRgb = 0xFFFFFF,
            colorModifier = 0x00,
            minTemp = 190,
            maxTemp = 230,
            diameter = 1.75f,
            weight = 1000,
            productionDateRaw = prodRaw.toShort(),
        )
    }

    // --- Entity <-> Filament conversion ---

    private fun FilamentEntity.toFilament(): Filament {
        return Filament(
            id = id,
            name = name,
            manufacturerCode = manufacturerCode,
            material = material,
            subtypeCode = subtypeCode,
            subtype = subtype,
            color = Epc256Encoder.rgbToHex(colorRgb),
            colorRgb = colorRgb,
            colorModifier = colorModifier,
            minTemp = minTemp,
            maxTemp = maxTemp,
            diameter = diameter,
            weight = weight,
            productionDateRaw = productionDateRaw,
        )
    }

    private fun Filament.toEntity(): FilamentEntity {
        return FilamentEntity(
            id = id,
            name = name,
            manufacturerCode = manufacturerCode,
            material = material,
            subtypeCode = subtypeCode,
            subtype = subtype,
            colorRgb = colorRgb,
            colorModifier = colorModifier,
            minTemp = minTemp,
            maxTemp = maxTemp,
            diameter = diameter,
            weight = weight,
            productionDateRaw = productionDateRaw,
        )
    }
}
