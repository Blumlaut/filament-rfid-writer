package com.blumlaut.filamenttagwriter.data.model

import java.util.Locale

/**
 * Encodes/decodes the ELEGOO NTAG213 filament tag format.
 *
 * Based on community reverse-engineering (DnG-Crafts/ELG-RFID, Savion/elegoo-rfid-editor).
 * The ELEGOO official spec is WRONG and must not be used.
 *
 * Full tag is 180 bytes (45 pages × 4 bytes):
 *   Pages 0-3  (0x00-0x0F): System (UID, BCC, lock, CC file)
 *   Pages 4-15 (0x10-0x3F): NDEF URI section (points to elegoo.com)
 *   Pages 16-27 (0x40-0x6F): Filament data (our concern)
 *   Pages 28-39 (0x70-0x9F): Reserved
 *   Pages 40-44 (0xA0-0xAC): Config / Auth / Access control
 *
 * Filament data byte offsets (relative to tag start):
 *   0x40  Header (1B): 0x36
 *   0x41  Manufacturer Code (4B): big-endian
 *   0x45  Reserved (3B)
 *   0x48  Material Type (4B): ELEGOO 32-bit code, big-endian
 *   0x4C  Subtype Code (2B): 16-bit numeric, big-endian
 *   0x4E  Reserved (2B)
 *   0x50  Color RGB (3B): R, G, B
 *   0x53  Color Modifier (1B)
 *   0x54  Min Temp (2B): °C, big-endian
 *   0x56  Max Temp (2B): °C, big-endian
 *   0x58  Reserved / Bed Temps (4B)
 *   0x5C  Diameter (2B): hundredths of mm, big-endian
 *   0x5E  Weight (2B): grams, big-endian
 *   0x60  Production Date (2B): raw bytes
 */
object Epc256Encoder {

    const val HEADER: Byte = 0x36

    // Filament data starts at byte offset 0x40 = page 16
    const val FILAMENT_DATA_START_PAGE = 16  // decimal page 16 = 0x10
    const val FILAMENT_DATA_SIZE = 36        // 36 bytes of filament data (pages 16-24)
    const val FILAMENT_DATA_START_OFFSET = 0x40

    /**
     * Encode a Filament into a 36-byte array (filament data section only).
     * This maps to tag byte offsets 0x40–0x63 (pages 16-24).
     */
    fun encode(filament: Filament): ByteArray {
        val data = ByteArray(FILAMENT_DATA_SIZE)

        // Helper: write 16-bit big-endian
        fun put16(i: Int, value: Int) {
            data[i] = (value shr 8).toByte()
            data[i + 1] = value.toByte()
        }

        // Helper: write 32-bit big-endian
        fun put32(i: Int, value: Int) {
            data[i] = (value shr 24).toByte()
            data[i + 1] = (value shr 16).toByte()
            data[i + 2] = (value shr 8).toByte()
            data[i + 3] = value.toByte()
        }

        // 0x40: Header (1 byte)
        data[0] = HEADER

        // 0x41-0x44: Manufacturer Code (4 bytes, big-endian)
        put32(1, filament.manufacturerCode)

        // 0x45-0x47: Reserved (3 bytes, zeros) — already zero

        // 0x48-0x4B: Material Type (4 bytes, ELEGOO custom code)
        val materialCode = Materials.MATERIAL_CODES_REVERSE[filament.material.uppercase()]
            ?: Materials.MATERIAL_CODES_REVERSE["PLA"] ?: 0
        put32(8, materialCode)

        // 0x4C-0x4D: Subtype Code (2 bytes, big-endian)
        put16(12, filament.subtypeCode.toInt() and 0xFFFF)

        // 0x4E-0x4F: Reserved (2 bytes) — already zero

        // 0x50-0x52: Color RGB888 (3 bytes)
        data[16] = (filament.colorRgb shr 16).toByte()  // R
        data[17] = (filament.colorRgb shr 8).toByte()   // G
        data[18] = filament.colorRgb.toByte()            // B

        // 0x53: Color Modifier (1 byte)
        data[19] = filament.colorModifier

        // 0x54-0x55: Min Extruder Temp (2 bytes, big-endian)
        put16(20, filament.minTemp.toInt() and 0xFFFF)

        // 0x56-0x57: Max Extruder Temp (2 bytes, big-endian)
        put16(22, filament.maxTemp.toInt() and 0xFFFF)

        // 0x58-0x5B: Reserved / Bed Temps (4 bytes) — already zero

        // 0x5C-0x5D: Diameter (2 bytes, hundredths of mm, big-endian)
        put16(28, (filament.diameter * 100).toInt())

        // 0x5E-0x5F: Weight (2 bytes, grams, big-endian)
        put16(30, filament.weight)

        // 0x60-0x61: Production Date (2 bytes)
        put16(32, filament.productionDateRaw.toInt() and 0xFFFF)

        return data
    }

    /**
     * Decode a 32-byte filament data section into a Filament.
     * Expects data starting at tag byte offset 0x40.
     *
     * Uses direct array access instead of ByteBuffer to avoid
     * potential byte-sign-extension issues on Android runtime.
     */
    fun decode(data: ByteArray): Filament {

        require(data.size >= FILAMENT_DATA_SIZE) {
            "Data too short: ${data.size} bytes (need $FILAMENT_DATA_SIZE)"
        }

        // Helper: read unsigned byte at index
        fun u8(i: Int): Int = data[i].toInt() and 0xFF

        // Helper: read 16-bit big-endian at index
        fun u16(i: Int): Int = (u8(i) shl 8) or u8(i + 1)

        // Helper: read 32-bit big-endian at index
        fun u32(i: Int): Int = (u8(i) shl 24) or (u8(i + 1) shl 16) or (u8(i + 2) shl 8) or u8(i + 3)

        // 0x40: Header
        val header = u8(0)
        require(header == (HEADER.toInt() and 0xFF)) {
            "Invalid header: 0x${header.toString(16).uppercase()}"
        }

        // 0x41-0x44: Manufacturer Code
        val manufacturerCode = u32(1)

        // 0x45-0x47: Reserved (skip)

        // 0x48-0x4B: Material Type
        val materialCode = u32(8)
        val material = Materials.resolveMaterial(materialCode)

        // 0x4C-0x4D: Subtype Code
        val subtypeCode = u16(12).toShort()
        val subtype = Materials.resolveSubtype(subtypeCode)

        // 0x4E-0x4F: Reserved (skip)

        // 0x50-0x52: Color RGB
        val r = u8(16)
        val g = u8(17)
        val b = u8(18)
        val colorRgb = (r shl 16) or (g shl 8) or b

        // 0x53: Color Modifier
        val colorModifier = data[19]

        // 0x54-0x55: Min Temp
        val minTemp = u16(20).toShort()

        // 0x56-0x57: Max Temp
        val maxTemp = u16(22).toShort()

        // 0x58-0x5B: Reserved / Bed Temps (skip)

        // 0x5C-0x5D: Diameter
        val diameterRaw = u16(28)
        val diameter = diameterRaw.toFloat() / 100f

        // 0x5E-0x5F: Weight
        val weight = u16(30)

        // 0x60-0x61: Production Date (if present)
        val productionDateRaw = if (data.size > 34) u16(32).toShort() else 0

        val name = synthesizeName(
            Filament(
                manufacturerCode = manufacturerCode,
                material = material,
                subtypeCode = subtypeCode,
                subtype = subtype,
                colorRgb = colorRgb,
            )
        )

        return Filament(
            manufacturerCode = manufacturerCode,
            name = name,
            material = material,
            subtypeCode = subtypeCode,
            subtype = subtype,
            colorRgb = colorRgb,
            color = rgbToHex(colorRgb),
            colorModifier = colorModifier,
            minTemp = minTemp,
            maxTemp = maxTemp,
            diameter = diameter,
            weight = weight,
            productionDateRaw = productionDateRaw,
        )
    }

    /**
     * Encode a Filament into NTAG213 page-aligned blocks for writing.
     * Filament data starts at page 16 (decimal), occupies pages 16–23 (32 bytes / 8 pages).
     * Returns a map of page address -> 4-byte block.
     */
    fun encodeNtagBlocks(filament: Filament): Map<Int, ByteArray> {
        val data = encode(filament)
        val blocks = mutableMapOf<Int, ByteArray>()

        for (i in 0 until data.size step 4) {
            val page = FILAMENT_DATA_START_PAGE + (i / 4)
            val block = ByteArray(4)
            val end = minOf(i + 4, data.size)
            System.arraycopy(data, i, block, 0, end - i)
            blocks[page] = block
        }

        return blocks
    }

    /**
     * Decode from NTAG213 page blocks.
     * Expects pages starting from page 16 (decimal).
     */
    fun decodeNtagBlocks(pages: Map<Int, ByteArray>): Filament {
        val sortedPages = pages.entries.sortedBy { it.key }
        val data = sortedPages.flatMap { it.value.toList() }.toByteArray()
        return decode(data)
    }

    fun rgbToHex(rgb: Int): String {
        return String.format(Locale.US, "#%06X", rgb and 0xFFFFFF)
    }

    /**
     * Synthesize a human-friendly name from a decoded Filament.
     * Format: "Red PLA", "Carbon Fiber PETG", "Matte Black ABS", etc.
     *
     * Uses the subtype if it adds info beyond the material (e.g. "PLA-CF" → "Carbon Fiber PLA"),
     * otherwise just the material. Prepends color name when recognizable.
     */
    fun synthesizeName(filament: Filament): String {
        val colorName = rgbToColorName(filament.colorRgb)
        val typeLabel = subtypeToNameLabel(filament.material, filament.subtype)
        return if (colorName != null) {
            "$colorName $typeLabel"
        } else {
            typeLabel
        }
    }

    /**
     * Map subtype to a descriptive label. If subtype == material, just use material.
     * Otherwise extract the modifier (e.g. "PLA-CF" → "Carbon Fiber PLA").
     */
    private fun subtypeToNameLabel(material: String, subtype: String): String {
        if (subtype == material) return material

        // Handle known subtype patterns
        val base = subtype.removePrefix(material).trimStart('-', '+')
        return when {
            subtype.contains("CF", ignoreCase = true) -> "Carbon Fiber $material"
            subtype.contains("GF", ignoreCase = true) -> "Glass Fiber $material"
            subtype.contains("Silk", ignoreCase = true) -> "Silk $material"
            subtype.contains("Matte", ignoreCase = true) -> "Matte $material"
            subtype.contains("Wood", ignoreCase = true) -> "Wood $material"
            subtype.contains("Marble", ignoreCase = true) -> "Marble $material"
            subtype.contains("Galaxy", ignoreCase = true) -> "Galaxy $material"
            subtype.contains("Sparkle", ignoreCase = true) -> "Sparkle $material"
            subtype.contains("Fluo", ignoreCase = true) -> "Fluorescent $material"
            subtype.contains("Pro", ignoreCase = true) -> "$material Pro"
            subtype.contains("Translucent", ignoreCase = true) -> "Translucent $material"
            subtype.contains("Copper", ignoreCase = true) -> "Copper $material"
            base.isNotEmpty() -> "$base $material"
            else -> material
        }
    }

    /**
     * Map RGB to a color name if it's close to a well-known color.
     * Returns null for colors that don't match any preset (use hex instead).
     */
    private fun rgbToColorName(rgb: Int): String? {
        val r = (rgb shr 16) and 0xFF
        val g = (rgb shr 8) and 0xFF
        val b = rgb and 0xFF

        // Check against known colors with tolerance
        val knownColors = listOf(
            "Black" to 0x000000,
            "White" to 0xFFFFFF,
            "Red" to 0xFF0000,
            "Green" to 0x00FF00,
            "Blue" to 0x0000FF,
            "Yellow" to 0xFFFF00,
            "Orange" to 0xFF8000,
            "Purple" to 0x800080,
            "Pink" to 0xFF69B4,
            "Cyan" to 0x00FFFF,
            "Brown" to 0x8B4513,
            "Gray" to 0x808080,
            "Grey" to 0x808080,
        )

        var bestName: String? = null
        var bestDist = 15000 // tolerance threshold (~55 per channel)

        for ((name, target) in knownColors) {
            val tr = (target shr 16) and 0xFF
            val tg = (target shr 8) and 0xFF
            val tb = target and 0xFF
            val dr = r - tr
            val dg = g - tg
            val db = b - tb
            val dist = dr * dr + dg * dg + db * db
            if (dist < bestDist) {
                bestDist = dist
                bestName = name
            }
        }

        return bestName
    }
}
