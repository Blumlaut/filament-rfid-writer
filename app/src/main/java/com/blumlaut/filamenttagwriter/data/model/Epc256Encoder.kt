package com.blumlaut.filamenttagwriter.data.model

import java.nio.ByteBuffer
import java.nio.ByteOrder

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
    const val FILAMENT_DATA_SIZE = 32        // 32 bytes of filament data
    const val FILAMENT_DATA_START_OFFSET = 0x40

    /**
     * Encode a Filament into a 32-byte array (filament data section only).
     * This maps to tag byte offsets 0x40–0x5F.
     */
    fun encode(filament: Filament): ByteArray {
        val buffer = ByteBuffer.allocate(FILAMENT_DATA_SIZE).order(ByteOrder.BIG_ENDIAN)

        // 0x40: Header (1 byte)
        buffer.put(HEADER)

        // 0x41-0x44: Manufacturer Code (4 bytes, big-endian)
        buffer.putInt(filament.manufacturerCode)

        // 0x45-0x47: Reserved (3 bytes, zeros)
        buffer.put(0x00)
        buffer.put(0x00)
        buffer.put(0x00)

        // 0x48-0x4B: Material Type (4 bytes, ELEGOO custom code)
        val materialCode = Materials.MATERIAL_CODES_REVERSE[filament.material.uppercase()]
            ?: Materials.MATERIAL_CODES_REVERSE["PLA"] ?: 0
        buffer.putInt(materialCode)

        // 0x4C-0x4D: Subtype Code (2 bytes, big-endian)
        buffer.putShort(filament.subtypeCode)

        // 0x4E-0x4F: Reserved (2 bytes)
        buffer.put(0x00)
        buffer.put(0x00)

        // 0x50-0x52: Color RGB888 (3 bytes)
        buffer.put((filament.colorRgb shr 16).toByte())  // R
        buffer.put((filament.colorRgb shr 8).toByte())   // G
        buffer.put(filament.colorRgb.toByte())            // B

        // 0x53: Color Modifier (1 byte)
        buffer.put(filament.colorModifier)

        // 0x54-0x55: Min Extruder Temp (2 bytes, big-endian)
        buffer.putShort(filament.minTemp)

        // 0x56-0x57: Max Extruder Temp (2 bytes, big-endian)
        buffer.putShort(filament.maxTemp)

        // 0x58-0x5B: Reserved / Bed Temps (4 bytes)
        buffer.putInt(0)

        // 0x5C-0x5D: Diameter (2 bytes, hundredths of mm, big-endian)
        buffer.putShort((filament.diameter * 100).toInt().toShort())

        // 0x5E-0x5F: Weight (2 bytes, grams, big-endian)
        buffer.putShort(filament.weight.toShort())

        // 0x60-0x61: Production Date (2 bytes)
        buffer.putShort(filament.productionDateRaw)

        // Remaining bytes zeroed by ByteBuffer allocation
        return buffer.array()
    }

    /**
     * Decode a 32-byte filament data section into a Filament.
     * Expects data starting at tag byte offset 0x40.
     */
    fun decode(data: ByteArray): Filament {
        if (data.size < FILAMENT_DATA_SIZE) {
            throw IllegalArgumentException("Data too short: ${data.size} bytes (need $FILAMENT_DATA_SIZE)")
        }

        val buffer = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN)

        // 0x40: Header
        val header = buffer.get()
        if (header != HEADER) {
            throw IllegalArgumentException("Invalid header: 0x${header.toInt() and 0xFF} (expected 0x${HEADER.toInt() and 0xFF})")
        }

        // 0x41-0x44: Manufacturer Code
        val manufacturerCode = buffer.int

        // 0x45-0x47: Reserved
        buffer.position(buffer.position() + 3)

        // 0x48-0x4B: Material Type
        val materialCode = buffer.int
        val material = Materials.resolveMaterial(materialCode)

        // 0x4C-0x4D: Subtype Code
        val subtypeCode = buffer.short
        val subtype = Materials.resolveSubtype(subtypeCode)

        // 0x4E-0x4F: Reserved
        buffer.position(buffer.position() + 2)

        // 0x50-0x52: Color RGB
        val r = buffer.get().toInt() and 0xFF
        val g = buffer.get().toInt() and 0xFF
        val b = buffer.get().toInt() and 0xFF
        val colorRgb = (r shl 16) or (g shl 8) or b

        // 0x53: Color Modifier
        val colorModifier = buffer.get()

        // 0x54-0x55: Min Temp
        val minTemp = buffer.short

        // 0x56-0x57: Max Temp
        val maxTemp = buffer.short

        // 0x58-0x5B: Reserved / Bed Temps
        buffer.position(buffer.position() + 4)

        // 0x5C-0x5D: Diameter
        val diameterRaw = buffer.short.toInt() and 0xFFFF
        val diameter = diameterRaw.toFloat() / 100f

        // 0x5E-0x5F: Weight
        val weight = buffer.short.toInt() and 0xFFFF

        // 0x60-0x61: Production Date
        val productionDateRaw = buffer.short

        return Filament(
            manufacturerCode = manufacturerCode,
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
        return String.format("#%06X", rgb and 0xFFFFFF)
    }

    fun hexToRgb(hex: String): Int {
        val clean = hex.removePrefix("#")
        return clean.toInt(16)
    }
}
