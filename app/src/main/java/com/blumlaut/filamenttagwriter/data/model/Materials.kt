package com.blumlaut.filamenttagwriter.data.model

/**
 * ELEGOO material and subtype code tables.
 *
 * Reverse-engineered from printer firmware by the community.
 * Sources: DnG-Crafts/ELG-RFID, Savion/elegoo-rfid-editor, ELEGOO-3D/ELEGOO-RFID-Tag-Guide#16
 */
object Materials {

    /**
     * 32-bit material type codes → material name.
     * Stored at tag offset 0x48-0x4B (big-endian).
     * Note: codes with high bit set (like PETG 0x80698471) are stored as negative signed Int.
     */
    val MATERIAL_CODES: Map<Int, String> = mapOf(
        0x00807665.toInt() to "PLA",
        0x80698471.toInt() to "PETG",
        0x00656683.toInt() to "ABS",
        0x00848085.toInt() to "TPU",
        0x00008065.toInt() to "PA",
        0x00678069.toInt() to "CPE",
        0x00008067.toInt() to "PC",
        0x00808665.toInt() to "PVA",
        0x00658365.toInt() to "ASA",
        0x42564F48.toInt() to "BVOH",
        0x00455641.toInt() to "EVA",
        0x48495053.toInt() to "HIPS",
        0x00005050.toInt() to "PP",
        0x00505041.toInt() to "PPA",
        0x00505053.toInt() to "PPS",
    )

    val MATERIAL_CODES_REVERSE: Map<String, Int> = MATERIAL_CODES.entries.associate { it.value to it.key }

    /**
     * 16-bit subtype codes → human-readable name.
     * Stored at tag offset 0x4C-0x4D (big-endian).
     *
     * High byte = material family, low byte = variant.
     * Some entries are not visible on the Centauri Carbon 2 UI.
     */
    val SUBTYPE_CODES: Map<Short, String> = mapOf(
        // PLA Family (0x00XX)
        0x0000.toShort() to "PLA",
        0x0001.toShort() to "PLA+",
        0x0002.toShort() to "PLA Pro",
        0x0003.toShort() to "PLA Silk",
        0x0004.toShort() to "PLA-CF",
        0x0005.toShort() to "PLA Carbon",
        0x0006.toShort() to "PLA Matte",
        0x0007.toShort() to "PLA Fluo",
        0x0008.toShort() to "PLA Wood",
        0x0009.toShort() to "PLA Basic",
        0x000A.toShort() to "RAPID PLA+",
        0x000B.toShort() to "PLA Marble",
        0x000C.toShort() to "PLA Galaxy",
        0x000D.toShort() to "PLA Red Copper",
        0x000E.toShort() to "PLA Sparkle",

        // PETG Family (0x01XX)
        0x0100.toShort() to "PETG",
        0x0101.toShort() to "PETG-CF",
        0x0102.toShort() to "PETG-GF",
        0x0103.toShort() to "PETG Pro",
        0x0104.toShort() to "PETG Translucent",
        0x0105.toShort() to "RAPID PETG",

        // ABS Family (0x02XX)
        0x0200.toShort() to "ABS",
        0x0201.toShort() to "ABS-GF",

        // TPU Family (0x03XX)
        0x0300.toShort() to "TPU",
        0x0301.toShort() to "TPU 95A",
        0x0302.toShort() to "RAPID TPU 95A",

        // PA Family (0x04XX)
        0x0400.toShort() to "PA",
        0x0401.toShort() to "PA-CF",
        0x0403.toShort() to "PAHT-CF",
        0x0404.toShort() to "PA6",
        0x0405.toShort() to "PA6-CF",
        0x0406.toShort() to "PA12",
        0x0407.toShort() to "PA12-CF",

        // Other Materials
        0x0500.toShort() to "CPE",
        0x0600.toShort() to "PC",
        0x0601.toShort() to "PCTG",
        0x0602.toShort() to "PC-FR",
        0x0700.toShort() to "PVA",
        0x0800.toShort() to "ASA",
        0x0900.toShort() to "BVOH",
        0x0A00.toShort() to "EVA",
        0x0B00.toShort() to "HIPS",
        0x0C00.toShort() to "PP",
        0x0C01.toShort() to "PP-CF",
        0x0C02.toShort() to "PP-GF",
        0x0D00.toShort() to "PPA",
        0x0D01.toShort() to "PPA-CF",
        0x0D02.toShort() to "PPA-GF",
        0x0E00.toShort() to "PPS",
        0x0E02.toShort() to "PPS-CF",
    )

    val SUBTYPE_CODES_REVERSE: Map<String, Short> = SUBTYPE_CODES.entries.associate { it.value to it.key }

    /** Material family codes (high byte of subtype). */
    val MATERIAL_FAMILIES: Map<String, Byte> = mapOf(
        "PLA" to 0x00,
        "PETG" to 0x01,
        "ABS" to 0x02,
        "TPU" to 0x03,
        "PA" to 0x04,
        "CPE" to 0x05,
        "PC" to 0x06,
        "PVA" to 0x07,
        "ASA" to 0x08,
        "BVOH" to 0x09,
        "EVA" to 0x0A,
        "HIPS" to 0x0B,
        "PP" to 0x0C,
        "PPA" to 0x0D,
        "PPS" to 0x0E,
    )

    /**
     * Get all material names sorted alphabetically.
     */
    fun getAllMaterials(): List<String> = MATERIAL_CODES.values.sorted()

    /**
     * Get subtypes for a given material family.
     */
    fun getSubtypesForMaterial(material: String): List<String> {
        val familyCode = MATERIAL_FAMILIES[material] ?: return emptyList()
        return SUBTYPE_CODES.entries
            .filter { (code, _) -> ((code.toInt() ushr 8) and 0xFF).toByte() == familyCode }
            .map { it.value }
            .sorted()
    }

    /**
     * Given a subtype name, return the parent material family.
     */
    fun getMaterialForSubtype(subtypeName: String): String? {
        val code = SUBTYPE_CODES_REVERSE[subtypeName] ?: return null
        val familyCode = ((code.toInt() ushr 8) and 0xFF).toByte()
        return MATERIAL_FAMILIES.entries.find { it.value == familyCode }?.key
    }

    /**
     * Resolve subtype code to human-readable name.
     */
    fun resolveSubtype(code: Short): String = SUBTYPE_CODES.getOrElse(code) { "Unknown (0x${code.toInt() and 0xFFFF})" }

    /**
     * Resolve material code to name.
     */
    fun resolveMaterial(code: Int): String = MATERIAL_CODES.getOrElse(code) { "Unknown" }
}
