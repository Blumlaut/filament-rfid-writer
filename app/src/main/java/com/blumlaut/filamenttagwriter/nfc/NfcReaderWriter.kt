package com.blumlaut.filamenttagwriter.nfc

import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.Ntag213
import com.blumlaut.filamenttagwriter.data.model.Epc256Encoder
import com.blumlaut.filamenttagwriter.data.model.Filament

/**
 * Handles reading from and writing to NTAG213 RFID tags.
 *
 * Reference: ELEGOO-RFID-Tag-Guide/README.md, Section 3
 *
 * NTAG213 has 144 bytes of user memory starting at page 0x04.
 * Each page is 4 bytes. The EPC-256 filament data occupies pages 0x04–0x1B (32 bytes).
 *
 * Writing requires authentication with the tag password (default: 0xA0A1A2A3).
 * After writing, the tag may need to be locked to prevent further writes.
 */
object NfcReaderWriter {

    /**
     * Default NTAG213 password from ELEGOO spec.
     */
    val DEFAULT_PASSWORD = byteArrayOf(0xA0, 0xA1, 0xA2, 0xA3)

    /**
     * Read filament data from an NTAG213 tag.
     * Reads pages 0x04 through 0x1B (32 bytes of EPC-256 data).
     */
    fun readFilament(tag: Tag): Filament? {
        val ntag = Ntag213.get(tag)
        return try {
            ntag.connect()
            val pages = mutableMapOf<Int, ByteArray>()
            for (page in 0x04..0x1B step 4) {
                val block = ntag.readBlock(page)
                if (block != null) pages[page] = block
            }
            ntag.close()
            Epc256Encoder.decodeNtagBlocks(pages)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Write filament data to an NTAG213 tag.
     * Authenticates with password, writes pages 0x04–0x1B.
     */
    fun writeFilament(tag: Tag, filament: Filament, password: ByteArray = DEFAULT_PASSWORD): Boolean {
        val ntag = Ntag213.get(tag)
        return try {
            ntag.connect()

            // Authenticate
            ntag.authenticate(password)

            // Write data blocks
            val blocks = Epc256Encoder.encodeNtagBlocks(filament)
            for ((page, block) in blocks) {
                ntag.writeBlock(page, block)
            }

            ntag.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Check if the tag is an NTAG213.
     */
    fun isNtag213(tag: Tag): Boolean {
        return Ntag213.get(tag) != null
    }
}
