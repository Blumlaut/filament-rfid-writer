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
                    Log.w(TAG, "Decode failed: ${e.javaClass.simpleName}: ${e.message}")
                    e.stackTrace.take(5).forEach { Log.w(TAG, "  at $it") }
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
        val tagId = tag.id.joinToString(" ") { String.format("%02X", it) }
        Log.d(TAG, "writeFilament started, tag ID: $tagId")

        val ml = MifareUltralight.get(tag)
        if (ml == null) return WriteResult.Error("Tag is not an NTAG213 / Mifare Ultralight")

        return try {
            Log.d(TAG, "Connecting to tag...")
            ml.connect()
            Log.d(TAG, "Connected")

            // Try writing without authentication first
            Log.d(TAG, "Attempting write without authentication...")
            val noAuthResult = tryWriteBlocks(ml, filament, authenticate = false)
            if (noAuthResult.success) {
                ml.close()
                Log.d(TAG, "Write succeeded without auth")
                return if (noAuthResult.errorMessage.isNullOrBlank()) {
                    WriteResult.Success
                } else {
                    WriteResult.Error(noAuthResult.errorMessage!!)
                }
            }
            Log.w(TAG, "No-auth write failed: ${noAuthResult.errorMessage}")

            // Failed without auth, try with NTAG213 password authentication
            Log.d(TAG, "Attempting auth + write...")
            val authResult = tryWriteBlocks(ml, filament, authenticate = true)
            ml.close()

            if (authResult.success) {
                Log.d(TAG, "Write succeeded with auth")
                return if (authResult.errorMessage.isNullOrBlank()) {
                    WriteResult.Success
                } else {
                    WriteResult.Error(authResult.errorMessage!!)
                }
            }
            Log.w(TAG, "Auth write failed: ${authResult.errorMessage}")
            WriteResult.Error("Write failed: ${authResult.errorMessage}")
        } catch (e: Exception) {
            try { ml.close() } catch (_: Exception) {}
            Log.e(TAG, "Write exception: ${e.javaClass.simpleName}: ${e.message}")
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
     * Read the filament data pages (16-24, decimal).
     * These correspond to byte offsets 0x40-0x63 in the full tag (36 bytes / 9 pages).
     *
     * Reads each page individually to avoid variable-length overlap issues
     * with MifareUltralight.readPages() on NTAG213 tags.
     */
    private fun readFilamentPages(ml: MifareUltralight): Map<Int, ByteArray> {
        val pages = mutableMapOf<Int, ByteArray>()

        // Read each page individually — readPages() may return >4 bytes on NTAG213,
        // so always take exactly the first 4 bytes (one page = 4 bytes)
        for (page in 16..24) {
            val response = ml.readPages(page)
            if (response != null && response.size >= 4) {
                val block = response.copyOfRange(0, 4)
                pages[page] = block
                val hex = block.joinToString(" ") { String.format("%02X", it) }
                Log.d(TAG, "Page $page (0x${String.format("%02X", page)}): [$hex]")
            } else {
                Log.w(TAG, "Page $page (0x${String.format("%02X", page)}): null or too short (${response?.size ?: 0} bytes)")
            }
        }
        return pages
    }

    private data class WriteAttemptResult(val success: Boolean, val errorMessage: String?)

    private fun tryWriteBlocks(ml: MifareUltralight, filament: Filament, authenticate: Boolean): WriteAttemptResult {
        return try {
            if (authenticate) {
                Log.d(TAG, "Authenticating with password 0xA0A1A2A3...")
                val authOk = authenticateNtag213(ml, DEFAULT_PASSWORD)
                if (!authOk) {
                    return WriteAttemptResult(false, "Authentication failed (wrong password or tag not password-protected)")
                }
                Log.d(TAG, "Authentication successful")
            }

            val blocks = Epc256Encoder.encodeNtagBlocks(filament)
            Log.d(TAG, "Writing ${blocks.size} blocks to tag")
            for ((page, block) in blocks) {
                val hex = block.joinToString(" ") { String.format("%02X", it) }
                Log.d(TAG, "Writing page $page (0x${String.format("%02X", page)}): [$hex]")
                ml.writePage(page, block)
                Log.d(TAG, "  Page $page written OK")
                Thread.sleep(10)
            }
            WriteAttemptResult(true, null)
        } catch (e: Exception) {
            Log.e(TAG, "Write attempt failed: ${e.javaClass.simpleName}: ${e.message}")
            WriteAttemptResult(false, "${e.javaClass.simpleName}: ${e.message}")
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
            Log.d(TAG, "Auth step 1: sending ${authCommand1.joinToString(" ") { String.format("%02X", it) }}")
            val response = ml.transceive(authCommand1)

            if (response == null) {
                Log.w(TAG, "Auth step 1: no response")
                return false
            }
            Log.d(TAG, "Auth step 1: response ${response.joinToString(" ") { String.format("%02X", it) }}")

            if (response.size < 4) {
                Log.w(TAG, "Auth step 1: response too short (${response.size} bytes)")
                return false
            }

            // Step 2: Send the random bytes back
            val authCommand2 = ByteArray(5)
            authCommand2[0] = 0x1B.toByte()
            System.arraycopy(response, 0, authCommand2, 1, 4)
            Log.d(TAG, "Auth step 2: sending ${authCommand2.joinToString(" ") { String.format("%02X", it) }}")
            val response2 = ml.transceive(authCommand2)
            Log.d(TAG, "Auth step 2: response ${response2?.let { it.joinToString(" ") { String.format("%02X", it) } } ?: "null"}")

            true
        } catch (e: Exception) {
            Log.w(TAG, "Auth exception: ${e.javaClass.simpleName}: ${e.message}")
            false
        }
    }
}
