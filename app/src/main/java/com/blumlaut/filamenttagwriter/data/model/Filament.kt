package com.blumlaut.filamenttagwriter.data.model

import com.blumlaut.filamenttagwriter.data.local.FilamentEntity
import java.util.Calendar

/**
 * Represents a filament spool with all metadata that maps to the ELEGOO NTAG213 tag format.
 *
 * Based on community reverse-engineering (DnG-Crafts/ELG-RFID, Savion/elegoo-rfid-editor).
 * The ELEGOO official spec is incorrect and should not be referenced.
 *
 * Tag layout (byte offsets within the full 180-byte tag):
 *   0x00-0x0F  System (UID, BCC, lock bytes, CC file)
 *   0x10-0x3F  NDEF URI section (elegoo.com) — pages 4-15
 *   0x40       Header (1B): 0x36
 *   0x41-0x44  Manufacturer Code (4B): e.g. 0xEEEEEEEE for ELEGOO
 *   0x45-0x47  Reserved/padding (3B)
 *   0x48-0x4B  Material Type (4B): ELEGOO custom 32-bit code
 *   0x4C-0x4D  Subtype Code (2B): 16-bit numeric lookup code
 *   0x4E-0x4F  Reserved (2B)
 *   0x50-0x52  Color RGB888 (3B)
 *   0x53       Color Modifier (1B)
 *   0x54-0x55  Min Extruder Temp (2B): °C, big-endian
 *   0x56-0x57  Max Extruder Temp (2B): °C, big-endian
 *   0x58-0x5B  Reserved / Bed Temps (4B)
 *   0x5C-0x5D  Diameter (2B): hundredths of mm, big-endian
 *   0x5E-0x5F  Weight (2B): grams, big-endian
 *   0x60-0x61  Production Date (2B): raw bytes
 *   0x62+      Reserved
 */
data class Filament(
    val id: String = "",                              // internal DB ID (UUID)
    val name: String = "",                            // user-friendly name
    val manufacturerCode: Int = 0xEEEEEEEE.toInt(),   // 32-bit manufacturer identifier
    val material: String = "PLA",                     // material name (lookup key)
    val subtypeCode: Short = 0x0000,                  // 16-bit numeric subtype code
    val subtype: String = "PLA",                      // human-readable subtype (derived from subtypeCode)
    val color: String = "#FFFFFF",                    // hex color string
    val colorRgb: Int = 0xFFFFFF,                     // RGB888 integer
    val colorModifier: Byte = 0x00,                   // color modifier byte
    val minTemp: Short = 190,                         // min extruder temperature °C
    val maxTemp: Short = 230,                         // max extruder temperature °C
    val diameter: Float = 1.75f,                      // mm (stored as hundredths on tag)
    val weight: Int = 1000,                           // grams
    val productionDateRaw: Short = 0,                 // raw 16-bit production date from tag
)

/**
 * Human-readable diameter string (e.g. "1.75mm").
 */
val Filament.diameterString: String
    get() = "${"%.2f".format(diameter)}mm"

/**
 * Convert this domain model to a Room entity.
 */
fun Filament.toEntity(): FilamentEntity = FilamentEntity(
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

/**
 * Generate a 16-bit production date raw value from the current date.
 * Format: (year % 100) << 8 | (month 1-12)
 */
fun generateProductionDateRaw(): Short {
    val cal = Calendar.getInstance()
    val year = cal.get(Calendar.YEAR) % 100
    val month = cal.get(Calendar.MONTH) + 1
    return ((year shl 8) or month).toShort()
}
