package com.blumlaut.filamenttagwriter.nfc

import android.nfc.Tag
import android.nfc.tech.MifareUltralight
import android.util.Log
import com.blumlaut.filamenttagwriter.data.model.Epc256Encoder
import com.blumlaut.filamenttagwriter.data.model.Filament

/**
 * Handles reading from and writing to NTAG213 RFID tags.
 *
 * Uses MifareUltralight API (NTAG213 is a Mifare Ultralight EV1 variant).
 *
 * Based on community reverse-engineering (DnG-Crafts/ELG-RFID, Savion/elegoo-rfid-editor).
 *
 * NTAG213 has 144 bytes of user memory starting at page 4.
 * Pages 4-15: NDEF URI section (elegoo.com)
 * Pages 16-23: Filament data (32 bytes)
 *
 * Writing requires authentication with the tag password (default: 0xA0A1A2A3).
 */
object NfcReaderWriter {

    private const val TAG = "NfcReaderWriter"

    /**
     * Default NTAG213 password from ELEGOO spec.
     */
    val DEFAULT_PASSWORD = byteArrayOf(0xA0.toByte(), 0xA1.toByte(), 0xA2.toByte(), 0xA3.toByte())

    /** Result of a read operation. */
    sealed class ReadResult {
        data class Success(val filament: Filament) : ReadResult()
        data class Error(val message: String) : ReadResult()
    }

    /** Result of a write operation. */
    sealed class WriteResult {
        object Success : WriteResult()
        data class Error(val message: String) : WriteResult()
    }

    /**
     * Read filament data from an NTAG213 tag.
     * Reads pages 16 through 23 (32 bytes of filament data at byte offset 0x40-0x5F).
     *
     * NOTE: Must be called immediately after tag discovery (within ~2s)
     * before Android's NFC service drops the RF link.
     */
    fun readFilament(tag: Tag): ReadResult {
        val tagId = tag.id.joinToString(" ") { String.format("%02X", it) }
        Log.d(TAG, "readFilament started, tag ID: $tagId")
        Log.d(TAG, "tech list: ${tag.techList.joinToString(", ")}")

        val ml = MifareUltralight.get(tag)
        if (ml == null) {
            Log.w(TAG, "Tag is not MifareUltralight compatible")
            return ReadResult.Error("Tag is not an NTAG213 / Mifare Ultralight")
        }

        return try {
            Log.d(TAG, "Connecting to tag...")
            ml.connect()
            Log.d(TAG, "Connected, max transceive length: ${ml.maxTransceiveLength}")

            val pages = readFilamentPages(ml)
            Log.d(TAG, "Read ${pages.size} page blocks")

            ml.close()
            Log.d(TAG, "Tag connection closed")

            if (pages.isNotEmpty()) {
                val rawBytes = pages.entries.sortedBy { it.key }
                    .flatMap { it.value.toList() }
                    .map { String.format("%02X", it) }
                    .joinToString(" ")
                Log.d(TAG, "Raw filament data: $rawBytes")

                try {
                    val filament = Epc256Encoder.decodeNtagBlocks(pages)
                    Log.d(TAG, "Decoded: ${filament.material} / ${filament.subtype} / ${filament.color} / ${filament.diameter}mm / ${filament.weight}g")
                    ReadResult.Success(filament)
                } catch (e: Exception) {
                    Log.w(TAG, "Decode failed: ${e.message}")
                    val firstPage = pages.values.firstOrNull()
                    val headerByte = firstPage?.get(0)
                    if (headerByte != null && headerByte.toInt() and 0xFF == 0x00) {
                        ReadResult.Error("Tag filament section is blank (no filament data written yet)")
                    } else {
                        ReadResult.Error("Tag data not in recognized format: ${e.message}")
                    }
                }
            } else {
                Log.w(TAG, "No data read from filament pages")
                ReadResult.Error("Could not read tag data")
            }
        } catch (e: Exception) {
            try { ml.close() } catch (_: Exception) {}
            Log.e(TAG, "Read failed: ${e.javaClass.simpleName}: ${e.message}")
            ReadResult.Error("Failed to read tag: ${e.message}")
        }
    }

    /**
     * Write filament data to an NTAG213 tag.
     * Tries without authentication first, falls back to NTAG213 password auth.
     */
    fun writeFilament(tag: Tag, filament: Filament): WriteResult {
        val ml = MifareUltralight.get(tag)
        if (ml == null) return WriteResult.Error("Tag is not an NTAG213 / Mifare Ultralight")

        return try {
            ml.connect()

            // Try writing without authentication first
            val writeResult = tryWriteBlocks(ml, filament, authenticate = false)
            if (writeResult) {
                ml.close()
                return WriteResult.Success
            }

            // Failed without auth, try with NTAG213 password authentication
            val authResult = tryWriteBlocks(ml, filament, authenticate = true)
            ml.close()

            if (authResult) {
                WriteResult.Success
            } else {
                WriteResult.Error("Failed to write tag (authentication may be required with a different password)")
            }
        } catch (e: Exception) {
            try { ml.close() } catch (_: Exception) {}
            WriteResult.Error("Failed to write tag: ${e.message}")
        }
    }

    /** Check if the tag is an NTAG213 / Mifare Ultralight. */
    fun isNtag213(tag: Tag): Boolean {
        return MifareUltralight.get(tag) != null
    }

    /** Get a human-readable tag info string. */
    fun getTagInfo(tag: Tag): String {
        val techList = tag.techList.joinToString(", ")
        val id = tag.id.joinToString("") { String.format("%02X", it) }
        return "Tag ID: $id\nTech: $techList"
    }

    /**
     * Read the filament data pages (16-23, decimal).
     * These correspond to byte offsets 0x40-0x5F in the full tag.
     */
    private fun readFilamentPages(ml: MifareUltralight): Map<Int, ByteArray> {
        val pages = mutableMapOf<Int, ByteArray>()

        // Filament data: pages 16-23 (decimal), step 4 for 4-byte page reads
        for (page in 16..23 step 4) {
            val block = ml.readPages(page)
            if (block != null) {
                pages[page] = block
                val hex = block.joinToString(" ") { String.format("%02X", it) }
                Log.d(TAG, "Page $page (0x${String.format("%02X", page)}): [$hex]")
            } else {
                Log.w(TAG, "Page $page (0x${String.format("%02X", page)}): null")
            }
        }
        return pages
    }

    private fun tryWriteBlocks(ml: MifareUltralight, filament: Filament, authenticate: Boolean): Boolean {
        return try {
            if (authenticate) {
                authenticateNtag213(ml, DEFAULT_PASSWORD)
            }

            val blocks = Epc256Encoder.encodeNtagBlocks(filament)
            for ((page, block) in blocks) {
                ml.writePage(page, block)
                Thread.sleep(10)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Write failed: ${e.message}")
            false
        }
    }

    /**
     * Authenticate with NTAG213 using the two-step password protocol.
     *
     * Step 1: Send 0x1B + 4-byte password
     * Step 2: Tag responds with 4 random bytes
     * Step 3: Send 0x1B + 4 random bytes from step 2
     */
    private fun authenticateNtag213(ml: MifareUltralight, password: ByteArray): Boolean {
        return try {
            // Step 1: Send password
            val authCommand1 = ByteArray(5)
            authCommand1[0] = 0x1B.toByte()
            System.arraycopy(password, 0, authCommand1, 1, 4)
            val response = ml.transceive(authCommand1)

            if (response == null || response.size < 4) return false

            // Step 2: Send the random bytes back
            val authCommand2 = ByteArray(5)
            authCommand2[0] = 0x1B.toByte()
            System.arraycopy(response, 0, authCommand2, 1, 4)
            ml.transceive(authCommand2)

            true
        } catch (e: Exception) {
            Log.w(TAG, "Auth failed: ${e.message}")
            false
        }
    }
}
