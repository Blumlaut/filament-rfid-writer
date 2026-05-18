package com.blumlaut.filamenttagwriter.data.model

import com.blumlaut.filamenttagwriter.ui.components.parseHexColor

/**
 * Data models for CANVAS AMS tray data from the printer's WebSocket API.
 *
 * Based on the ELEGOO Centauri Carbon CANVAS protocol (Cmd 324 GET_MATERIAL_DATA).
 * See docs/canvas-api.md for full protocol reference.
 */

data class CanvasMaterialData(
    val activeCanvasId: Int = -1,
    val activeTrayId: Int = -1,
    val autoRefill: Int = 0,
    val canvasList: List<CanvasUnit> = emptyList(),
) {
    /** All trays across all connected CANVAS units. */
    val allTrays: List<CanvasTray>
        get() = canvasList.flatMap { it.trayList }

    /** Whether any CANVAS unit is connected. */
    val hasConnectedCanvas: Boolean
        get() = canvasList.any { it.connected }
}

data class CanvasUnit(
    val canvasId: Int = 0,
    val connected: Boolean = false,
    val trayList: List<CanvasTray> = emptyList(),
)

data class CanvasTray(
    val trayId: Int = 0,
    val brand: String = "",
    val filamentType: String = "",
    val filamentName: String = "",
    val filamentCode: String = "",
    val filamentColor: String = "",
    val minNozzleTemp: Int = 0,
    val maxNozzleTemp: Int = 0,
    val status: Int = 0,
) {
    /** Whether this tray has a valid NFC tag code (not 0x00000). */
    val hasNfcTag: Boolean
        get() = filamentCode.isNotBlank() &&
                filamentCode != "0x00000" &&
                filamentCode != "0x000000" &&
                filamentCode != "0x0000000"

    /** Whether filament info is populated (not an empty tray). */
    val hasFilament: Boolean
        get() = filamentType.isNotBlank() && filamentType != "None"

    /**
     * Convert this tray data into a Filament object for saving to the catalog.
     * Uses the tray's filament_type to infer material/subtype, and defaults for missing fields.
     */
    fun toFilament(): Filament {
        val material = filamentType.uppercase()
        val colorRgb = filamentColor.parseHexColor()

        return Filament(
            name = filamentName.ifBlank { "${brand} $material" }.trim(),
            material = material,
            subtype = material,
            subtypeCode = Materials.SUBTYPE_CODES_REVERSE[material] ?: 0x0000,
            color = filamentColor.ifBlank { "#FFFFFF" },
            colorRgb = colorRgb,
            colorModifier = 0x00,
            minTemp = (minNozzleTemp.coerceIn(0, 300)).toShort(),
            maxTemp = (maxNozzleTemp.coerceIn(0, 300)).toShort(),
            diameter = 1.75f,
            weight = 1000,
        )
    }
}
