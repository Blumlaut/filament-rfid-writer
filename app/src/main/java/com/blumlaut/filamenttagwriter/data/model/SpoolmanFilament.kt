package com.blumlaut.filamenttagwriter.data.model

/**
 * Single filament entry from the SpoolmanDB community database.
 *
 * Source: https://github.com/Donkie/SpoolmanDB
 * Licensed: MIT
 *
 * This is a read-only reference database. It is used solely for UI autocomplete
 * and does NOT affect tag encoding (which uses ELEGOO-specific codes from Materials.kt).
 */
data class SpoolmanFilament(
    val id: String,
    val manufacturer: String,
    val name: String,
    val material: String,
    val density: Float?,
    val weight: Float,
    val spoolWeight: Int?,
    val spoolType: String?,
    val diameter: Float,
    val colorHex: String?,
    val extruderTemp: Int?,
    val extruderTempRange: Pair<Int, Int>?,
    val bedTemp: Int?,
    val bedTempRange: Pair<Int, Int>?,
    val finish: String?,
    val pattern: String?,
    val translucent: Boolean,
    val glow: Boolean,
)

/**
 * Mapping from SpoolmanDB material names to ELEGOO tag-compatible material + subtype.
 *
 * When a user selects a SpoolmanDB filament, this mapping converts the material
 * name into the correct ELEGOO material code and subtype code for tag encoding.
 *
 * If no mapping exists, the user must select material/subtype manually.
 */
object SpoolmanMaterialMapper {

    /**
     * Maps a SpoolmanDB material string to an ELEGOO (material family, subtype) pair.
     * Returns null if the material has no ELEGOO equivalent.
     */
    fun mapToElegoo(spoolmanMaterial: String): Pair<String, String>? {
        return MATERIAL_MAP[spoolmanMaterial]
    }

    /**
     * Check if a SpoolmanDB material has an ELEGOO equivalent.
     */
    fun hasElegooEquivalent(spoolmanMaterial: String): Boolean = MATERIAL_MAP.containsKey(spoolmanMaterial)

    /**
     * Full mapping table: SpoolmanDB material → (ELEGOO material family, ELEGOO subtype).
     *
     * ELEGOO material family determines the 32-bit material code on the tag.
     * ELEGOO subtype determines the 16-bit subtype code on the tag.
     * Both are defined in Materials.kt.
     */
    private val MATERIAL_MAP: Map<String, Pair<String, String>> = mapOf(
        // PLA family
        "PLA" to ("PLA" to "PLA"),
        "PLA+" to ("PLA" to "PLA+"),
        "PLA-CF" to ("PLA" to "PLA-CF"),
        "PLA+WOOD" to ("PLA" to "PLA Wood"),

        // PETG family
        "PETG" to ("PETG" to "PETG"),
        "PETG-CF" to ("PETG" to "PETG-CF"),
        "PETG-CF10" to ("PETG" to "PETG-CF"),
        "PETG-GF" to ("PETG" to "PETG-GF"),
        "PETG Pro" to ("PETG" to "PETG Pro"),

        // ABS family
        "ABS" to ("ABS" to "ABS"),
        "ABS+" to ("ABS" to "ABS"),
        "ABS-GF" to ("ABS" to "ABS-GF"),
        "ABS+GF20" to ("ABS" to "ABS-GF"),

        // ASA family
        "ASA" to ("ASA" to "ASA"),
        "ASA-CF" to ("ASA" to "ASA"),
        "ASA-GF" to ("ASA" to "ASA"),

        // TPU family
        "TPU" to ("TPU" to "TPU"),
        "TPU-55D" to ("TPU" to "TPU"),
        "TPU-85A" to ("TPU" to "TPU"),
        "TPU-90A" to ("TPU" to "TPU"),
        "TPU-95A" to ("TPU" to "TPU 95A"),
        "TPU-CF" to ("TPU" to "TPU"),
        "TPE" to ("TPU" to "TPU"),

        // PA (Nylon) family
        "PA" to ("PA" to "PA"),
        "PA-CF" to ("PA" to "PA-CF"),
        "PA12" to ("PA" to "PA12"),
        "PA12-CF" to ("PA" to "PA12-CF"),
        "PA6" to ("PA" to "PA6"),
        "PA6-CF" to ("PA" to "PA6-CF"),
        "PA6-GF" to ("PA" to "PA"),
        "PAHT-CF" to ("PA" to "PAHT-CF"),

        // PC family
        "PC" to ("PC" to "PC"),
        "PC+ABS" to ("PC" to "PC"),
        "PCABS" to ("PC" to "PC"),
        "PC-CF" to ("PC" to "PC"),
        "PCPBT" to ("PC" to "PC"),
        "PCPBT-CF" to ("PC" to "PC"),
        "PCTG" to ("PC" to "PCTG"),

        // PVA family
        "PVA" to ("PVA" to "PVA"),

        // HIPS family
        "HIPS" to ("HIPS" to "HIPS"),

        // PP family
        "PP" to ("PP" to "PP"),
        "PP-CF" to ("PP" to "PP-CF"),
        "PP-GF" to ("PP" to "PP-GF"),

        // PPS family
        "PPS" to ("PPS" to "PPS"),
        "PPS-CF" to ("PPS" to "PPS-CF"),

        // PPA family
        "PPA" to ("PPA" to "PPA"),
        "PPA-CF" to ("PPA" to "PPA-CF"),
        "PPA-GF" to ("PPA" to "PPA-GF"),

        // CPE family
        "CPE" to ("CPE" to "CPE"),

        // PET-CF — no dedicated PET family in ELEGOO, map to PETG as closest
        "PET-CF" to ("PETG" to "PETG-CF"),
    )
}
