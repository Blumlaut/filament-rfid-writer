package com.blumlaut.filamenttagwriter.data.model

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Encodes/decodes the EPC-256 data format used on NTAG213 filament RFID tags.
 *
 * Reference: ELEGOO-RFID-Tag-Guide/README.md, Section 2 & 3
 *
 * Total: 32 bytes (256 bits) written to NTAG213 user memory pages 0x04–0x1B.
 * NTAG213 memory is organized in 4-byte blocks; non-aligned fields are zero-padded.
 */
object Epc256Encoder {

    const val HEADER: Byte = 0x36
    const val TAG_DATA_SIZE = 32

    /**
     * Encode a Filament into the 32-byte EPC-256 byte array.
     */
    fun encode(filament: Filament): ByteArray {
        val buffer = ByteBuffer.allocate(TAG_DATA_SIZE).order(ByteOrder.BIG_ENDIAN)

        // Header (1 byte)
        buffer.put(HEADER)

        // Manufacturer Code (4 bytes)
        buffer.putInt(filament.manufacturerCode)

        // Filament Code (2 bytes)
        buffer.putShort(filament.filamentCode)

        // Material Main (4 bytes, ASCII, left-padded with spaces to 4 chars)
        buffer.put(padAscii(filament.material, 4))

        // Material Subtype (4 bytes, ASCII)
        buffer.put(padAscii(filament.materialSubtype, 4))

        // Color RGB888 (3 bytes)
        buffer.put((filament.colorRgb shr 16).toByte())  // R
        buffer.put((filament.colorRgb shr 8).toByte())   // G
        buffer.put(filament.colorRgb.toByte())            // B

        // Diameter (2 bytes, hundredths of mm)
        buffer.putShort((filament.diameter * 100).toShort())

        // Weight (2 bytes, grams)
        buffer.putShort(filament.weight.toShort())

        // Production Date (2 bytes, YYMM)
        buffer.putShort(((filament.productionYear * 100 + filament.productionMonth).toShort()))

        // Reserved (8 bytes, zeros)
        buffer.put(ByteArray(8))

        return buffer.array()
    }

    /**
     * Decode a 32-byte EPC-256 byte array into a Filament.
     */
    fun decode(data: ByteArray): Filament {
        if (data.size < TAG_DATA_SIZE) throw IllegalArgumentException("Data too short: ${data.size} bytes")

        val buffer = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN)

        val header = buffer.get()  // skip header
        if (header != HEADER) {
            throw IllegalArgumentException("Invalid header: 0x${header.toInt() and 0xFF}")
        }

        val manufacturerCode = buffer.int
        val filamentCode = buffer.short

        val material = unpadAscii(buffer, 4)
        val materialSubtype = unpadAscii(buffer, 4)

        val r = buffer.get().toInt() and 0xFF
        val g = buffer.get().toInt() and 0xFF
        val b = buffer.get().toInt() and 0xFF
        val colorRgb = (r shl 16) or (g shl 8) or b

        val diameter = buffer.short.toFloat() / 100f
        val weight = buffer.short.toInt()
        val date = buffer.short.toInt()
        val productionYear = date / 100
        val productionMonth = date % 100

        // skip reserved 8 bytes
        buffer.position(buffer.position() + 8)

        return Filament(
            manufacturerCode = manufacturerCode,
            filamentCode = filamentCode,
            material = material,
            materialSubtype = materialSubtype,
            colorRgb = colorRgb,
            diameter = diameter,
            weight = weight,
            productionYear = productionYear,
            productionMonth = productionMonth,
        )
    }

    /**
     * Encode a Filament into NTAG213 page-aligned blocks for direct writing.
     * Pages 0x04–0x27 are user memory, each page is 4 bytes.
     * Returns a map of page address -> 4-byte block.
     */
    fun encodeNtagBlocks(filament: Filament): Map<Int, ByteArray> {
        val data = encode(filament)
        val blocks = mutableMapOf<Int, ByteArray>()

        // Data starts at page 0x04
        for (i in 0 until data.size step 4) {
            val page = 0x04 + (i / 4)
            val block = ByteArray(4)
            val end = minOf(i + 4, data.size)
            System.arraycopy(data, i, block, 0, end - i)
            blocks[page] = block
        }

        return blocks
    }

    /**
     * Decode from NTAG213 page blocks.
     */
    fun decodeNtagBlocks(pages: Map<Int, ByteArray>): Filament {
        val sortedPages = pages.entries.sortedBy { it.key }
        val data = sortedPages.flatMap { it.value.toList() }.toByteArray()
        return decode(data)
    }

    private fun padAscii(str: String, length: Int): ByteArray {
        val bytes = str.toByteArray(Charsets.US_ASCII)
        val padded = ByteArray(length)
        System.arraycopy(bytes, 0, padded, 0, minOf(bytes.size, length))
        return padded
    }

    private fun unpadAscii(buffer: ByteBuffer, length: Int): String {
        val bytes = ByteArray(length)
        buffer.get(bytes)
        return String(bytes, Charsets.US_ASCII).trim()
    }

    fun rgbToHex(rgb: Int): String {
        return String.format("#%06X", rgb and 0xFFFFFF)
    }

    fun hexToRgb(hex: String): Int {
        val clean = hex.removePrefix("#")
        return clean.toInt(16)
    }
}
