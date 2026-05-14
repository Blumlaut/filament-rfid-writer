package com.blumlaut.filamenttagwriter.data.model

/**
 * Represents a filament spool with all metadata that maps to the EPC-256 RFID tag format.
 *
 * Reference: ELEGOO-RFID-Tag-Guide/README.md
 *
 * The tag uses NTAG213 with 32 bytes of user data (EPC-256 format):
 *   - Header (1B): 0x36 EPC-256 identifier
 *   - Manufacturer Code (4B): e.g. 0xEEEEEEEE for ELEGOO
 *   - Filament Code (2B): internal manufacturer code
 *   - Material Main (4B): ASCII, e.g. "PLA "
 *   - Material Subtype (4B): ASCII, e.g. "CF20"
 *   - Color RGB888 (3B): e.g. 0xFF3700 for red
 *   - Diameter (2B): hundredths of mm, e.g. 175 = 1.75mm
 *   - Weight (2B): grams, e.g. 1000
 *   - Production Date (2B): YYMM, e.g. 2502 = Feb 2025
 *   - Reserved (8B): future use
 */
data class Filament(
    val id: String = "",                          // internal DB ID (UUID)
    val name: String = "",                        // user-friendly name
    val manufacturerCode: Int = 0xEEEEEEEE,       // 32-bit manufacturer identifier
    val filamentCode: Short = 0x0001,             // 16-bit internal code
    val material: String = "PLA",                 // up to 4 chars ASCII
    val materialSubtype: String = "",             // up to 4 chars ASCII (e.g. "CF", "HF")
    val color: String = "#FFFFFF",                // hex color string
    val colorRgb: Int = 0xFFFFFF,                 // RGB888 integer
    val diameter: Float = 1.75f,                  // mm (stored as hundredths on tag)
    val weight: Int = 1000,                       // grams
    val productionYear: Int = 25,                 // 2-digit year
    val productionMonth: Int = 2,                 // 1-12
)
