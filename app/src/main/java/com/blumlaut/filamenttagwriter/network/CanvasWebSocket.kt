package com.blumlaut.filamenttagwriter.network

import android.util.Log
import com.blumlaut.filamenttagwriter.data.model.CanvasMaterialData
import com.blumlaut.filamenttagwriter.data.model.CanvasTray
import com.blumlaut.filamenttagwriter.data.model.CanvasUnit
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * Raw WebSocket client for the ELEGOO Centauri Carbon CANVAS AMS protocol.
 *
 * Uses a raw TCP socket with manual WebSocket framing instead of OkHttp,
 * because the printer's WebSocket server is non-standard and rejects
 * OkHttp's handshake (EOFException on readHeader).
 *
 * Connects to `ws://<ip>:3030/websocket` and sends/receives JSON messages.
 * Uses Cmd 324 (GET_MATERIAL_DATA) to fetch filament tray information.
 *
 * See docs/canvas-api.md for full protocol reference.
 */
class CanvasWebSocket(private val printerIp: String, private val port: Int = 3030) {

    companion object {
        private const val TAG = "CanvasWebSocket"

        /**
         * Quick check: can we reach the printer at the given IP:port?
         * Performs a TCP connection + WebSocket handshake, then closes.
         * Returns true if the printer responded with a 101 Switching Protocols.
         */
        fun isPrinterReachable(ip: String, port: Int = 3030, timeoutMs: Long = 5000): Boolean {
            var socket: Socket? = null
            var inputStream: BufferedInputStream? = null
            var outputStream: BufferedOutputStream? = null
            return try {
                Log.d(TAG, "Reachability check: connecting to $ip:$port (timeout=${timeoutMs}ms)")
                val address = InetSocketAddress(ip, port)
                socket = Socket().apply {
                    soTimeout = timeoutMs.toInt()
                    connect(address, timeoutMs.toInt())
                }
                Log.d(TAG, "Reachability check: TCP connected")
                inputStream = BufferedInputStream(socket!!.getInputStream())
                outputStream = BufferedOutputStream(socket!!.getOutputStream())

                // Perform WebSocket handshake
                val key = generateWebSocketKey()
                val request = buildString {
                    append("GET /websocket HTTP/1.1\r\n")
                    append("Host: $ip:$port\r\n")
                    append("Upgrade: websocket\r\n")
                    append("Connection: Upgrade\r\n")
                    append("Sec-WebSocket-Key: $key\r\n")
                    append("Sec-WebSocket-Version: 13\r\n")
                    append("\r\n")
                }
                outputStream.write(request.toByteArray(Charsets.UTF_8))
                outputStream.flush()

                // Read response and check for 101
                var line: String
                var found101 = false
                while (true) {
                    line = readLine(inputStream!!)
                    if (line.isBlank()) break
                    if (line.startsWith("HTTP/")) {
                        val status = line.split(" ").getOrNull(1)
                        found101 = status == "101"
                    }
                }
                Log.d(TAG, "Reachability check $ip:$port -> ${if (found101) "OK" else "FAIL"}")
                found101
            } catch (e: Exception) {
                Log.e(TAG, "Reachability check $ip:$port failed: ${e.javaClass.simpleName}: ${e.message}", e)
                false
            } finally {
                try { outputStream?.close() } catch (_: Exception) {}
                try { inputStream?.close() } catch (_: Exception) {}
                try { socket?.close() } catch (_: Exception) {}
            }
        }

        private fun generateWebSocketKey(): String {
            val bytes = ByteArray(16)
            java.security.SecureRandom().nextBytes(bytes)
            return android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        }

        private fun readLine(input: BufferedInputStream): String {
            val buffer = StringBuilder()
            while (true) {
                val b = input.read()
                if (b == -1) return ""
                if (b == '\n'.code) return buffer.toString().trimEnd('\r')
                if (b != '\r'.code) {
                    buffer.append(b.toChar())
                }
            }
        }
    }

    private var socket: Socket? = null
    private var inputStream: BufferedInputStream? = null
    private var outputStream: BufferedOutputStream? = null
    private val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

    @Volatile private var _connectionState: ConnectionState = ConnectionState.Disconnected
    val connectionState: ConnectionState
        get() = _connectionState

    /** Callback for material data received from the printer. */
    var onMaterialData: ((CanvasMaterialData) -> Unit)? = null
    var onConnectionStateChanged: ((ConnectionState) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    /** Cached printer ID from responses. */
    var printerId: String = ""
        private set

    /** Cached mainboard ID from responses. */
    var mainboardId: String = ""
        private set

    @Volatile private var _materialData: CanvasMaterialData = CanvasMaterialData()
    val materialData: CanvasMaterialData
        get() = _materialData

    @Volatile private var reading = false

    /**
     * Connect to the printer's WebSocket.
     */
    fun connect() {
        if (_connectionState != ConnectionState.Disconnected) return

        Log.d(TAG, "connect: ip=$printerIp, port=$port")
        _connectionState = ConnectionState.Connecting
        onConnectionStateChanged?.invoke(_connectionState)

        executor.submit {
            try {
                val address = InetSocketAddress(this@CanvasWebSocket.printerIp, this@CanvasWebSocket.port)
                socket = Socket().apply {
                    soTimeout = 5000
                    connect(address, 5000)
                }
                inputStream = BufferedInputStream(socket!!.getInputStream())
                outputStream = BufferedOutputStream(socket!!.getOutputStream())

                // Perform WebSocket handshake
                if (!performHandshake()) {
                    _connectionState = ConnectionState.Error("Handshake failed")
                    onConnectionStateChanged?.invoke(_connectionState)
                    cleanupSocket()
                    return@submit
                }

                Log.d(TAG, "WebSocket connected to $printerIp:$port")
                _connectionState = ConnectionState.Connected
                onConnectionStateChanged?.invoke(_connectionState)

                // Send initial material data request
                requestMaterialData()

                // Start reading loop
                reading = true
                while (reading && !Thread.currentThread().isInterrupted) {
                    try {
                        val frame = readFrame()
                        when (frame) {
                            is WebSocketFrame.Text -> handleIncomingMessage(frame.payload)
                            is WebSocketFrame.Close -> {
                                Log.d(TAG, "Server sent close frame")
                                break
                            }
                            is WebSocketFrame.Ping -> {
                                sendFrame(WebSocketFrame.Pong(byteArrayOf()))
                            }
                            is WebSocketFrame.Pong -> {
                                // Ignore pong responses
                            }
                            is WebSocketFrame.Binary -> {
                                // Ignore binary frames
                            }
                            null -> {
                                // Timeout or connection closed
                                Log.d(TAG, "readFrame returned null (timeout or closed)")
                                break
                            }
                        }
                    } catch (e: IOException) {
                        if (reading) {
                            Log.e(TAG, "Read error", e)
                        }
                        break
                    }
                }
                Log.d(TAG, "Read loop exited")
            } catch (e: Exception) {
                Log.e(TAG, "Connection failure", e)
                _connectionState = ConnectionState.Error(e.message ?: "Connection failed")
                onConnectionStateChanged?.invoke(_connectionState)
                onError?.invoke(e.message ?: "Connection failed")
            } finally {
                if (_connectionState == ConnectionState.Connected) {
                    _connectionState = ConnectionState.Disconnected
                    onConnectionStateChanged?.invoke(_connectionState)
                }
                cleanupSocket()
            }
        }
    }

    /**
     * Perform the WebSocket handshake (RFC 6455).
     */
    private fun performHandshake(): Boolean {
        val key = Companion.generateWebSocketKey()

        val request = buildString {
            append("GET /websocket HTTP/1.1\r\n")
            append("Host: $printerIp:$port\r\n")
            append("Upgrade: websocket\r\n")
            append("Connection: Upgrade\r\n")
            append("Sec-WebSocket-Key: $key\r\n")
            append("Sec-WebSocket-Version: 13\r\n")
            append("\r\n")
        }

        Log.d(TAG, "Handshake request to $printerIp:$port")
        outputStream?.write(request.toByteArray(Charsets.UTF_8))
        outputStream?.flush()

        // Read response
        val response = StringBuilder()
        var line: String
        while (true) {
            line = Companion.readLine(inputStream!!)
            if (line.isBlank()) break
            response.append(line).append("\n")
            if (line.startsWith("HTTP/")) {
                val status = line.split(" ").getOrNull(1)
                if (status != "101") {
                    Log.e(TAG, "Handshake failed: $line")
                    return false
                }
            }
        }

        Log.d(TAG, "Handshake response:\n$response")
        return true
    }

    /**
     * Send a WebSocket text frame.
     */
    private fun sendFrame(frame: WebSocketFrame) {
        if (outputStream == null) return

        val bytes = when (frame) {
            is WebSocketFrame.Text -> {
                buildWebSocketFrame(frame.payload.toByteArray(Charsets.UTF_8), 0x1, false)
            }
            is WebSocketFrame.Pong -> {
                buildWebSocketFrame(frame.payload, 0xA, false)
            }
            is WebSocketFrame.Close -> {
                buildWebSocketFrame(frame.payload, 0x8, false)
            }
            is WebSocketFrame.Binary -> {
                buildWebSocketFrame(frame.payload, 0x2, false)
            }
            is WebSocketFrame.Ping -> {
                buildWebSocketFrame(frame.payload, 0x9, false)
            }
        }

        try {
            outputStream?.write(bytes)
            outputStream?.flush()
        } catch (e: IOException) {
            Log.e(TAG, "Send error", e)
        }
    }

    /**
     * Build a WebSocket frame (unmasked, server→client direction).
     */
    private fun buildWebSocketFrame(payload: ByteArray, opcode: Byte, masked: Boolean): ByteArray {
        val length = payload.size
        val maskBytes = if (masked) 4 else 0
        val headerSize = when {
            length < 126 -> 2 + maskBytes
            length < 65536 -> 4 + maskBytes
            else -> 10 + maskBytes
        }
        val result = ByteArray(headerSize + length)
        var offset = 0

        // Byte 0: FIN bit + opcode
        result[offset++] = (0x80 or opcode.toInt()).toByte()

        // Byte 1: Mask bit + payload length
        val maskBit = if (masked) 0x80 else 0x00
        when {
            length < 126 -> {
                result[offset++] = (maskBit or length).toByte()
            }
            length < 65536 -> {
                result[offset++] = (maskBit or 126).toByte()
                result[offset++] = ((length ushr 8) and 0xFF).toByte()
                result[offset++] = (length and 0xFF).toByte()
            }
            else -> {
                result[offset++] = (maskBit or 127).toByte()
                val lenLong = length.toLong()
                result[offset++] = 0
                result[offset++] = 0
                result[offset++] = 0
                result[offset++] = 0
                result[offset++] = ((lenLong shr 24) and 0xFF).toByte()
                result[offset++] = ((lenLong shr 16) and 0xFF).toByte()
                result[offset++] = ((lenLong shr 8) and 0xFF).toByte()
                result[offset++] = (lenLong and 0xFF).toByte()
            }
        }

        // Payload
        payload.copyInto(result, offset)

        return result
    }

    /**
     * Read a WebSocket frame from the input stream.
     */
    private fun readFrame(): WebSocketFrame? {
        // Read first 2 bytes (FIN/opcode + length)
        val header = ByteArray(2)
        if (readFully(header) != 2) return null

        val opcode = header[0].toInt() and 0x0F
        val fin = (header[0].toInt() and 0x80) != 0
        val masked = (header[1].toInt() and 0x80) != 0
        var payloadLength = header[1].toInt() and 0x7F

        // Extended payload length
        var extraBytes = 0
        if (payloadLength == 126) {
            val ext = ByteArray(2)
            if (readFully(ext) != 2) return null
            payloadLength = ((ext[0].toInt() and 0xFF) shl 8) or (ext[1].toInt() and 0xFF)
            extraBytes = 2
        } else if (payloadLength == 127) {
            val ext = ByteArray(8)
            if (readFully(ext) != 8) return null
            payloadLength = ((ext[6].toInt() and 0xFF) shl 8) or (ext[7].toInt() and 0xFF)
            extraBytes = 8
        }

        // Mask key (client→client frames are masked)
        val maskKey = if (masked) {
            val key = ByteArray(4)
            if (readFully(key) != 4) return null
            key
        } else {
            null
        }

        // Read payload
        val payload = ByteArray(payloadLength.toInt())
        if (payloadLength > 0 && readFully(payload) != payload.size) return null

        // Unmask if needed
        maskKey?.let { key ->
            for (i in payload.indices) {
                payload[i] = (payload[i].toInt() xor key[i % 4].toInt()).toByte()
            }
        }

        return when (opcode) {
            0x1 -> WebSocketFrame.Text(String(payload, Charsets.UTF_8))
            0x8 -> WebSocketFrame.Close(payload)
            0x9 -> WebSocketFrame.Ping(payload)
            0xA -> WebSocketFrame.Pong(payload)
            else -> WebSocketFrame.Binary(payload)
        }
    }

    private fun readFully(buffer: ByteArray): Int {
        var total = 0
        while (total < buffer.size) {
            val count = inputStream?.read(buffer, total, buffer.size - total)
            if (count == null || count == -1) return total
            total += count
        }
        return total
    }

    /**
     * Send Cmd 324 (GET_MATERIAL_DATA) to request CANVAS tray data.
     */
    fun requestMaterialData() {
        if (_connectionState != ConnectionState.Connected) {
            Log.w(TAG, "Not connected, cannot request material data")
            return
        }

        val message = buildMaterialDataRequest()
        Log.d(TAG, "Sending GET_MATERIAL_DATA")
        sendFrame(WebSocketFrame.Text(message))
    }

    /**
     * Build a Cmd 324 request message.
     */
    private fun buildMaterialDataRequest(): String {
        val request = JSONObject().apply {
            put("Id", printerId)
            put("Data", JSONObject().apply {
                put("Cmd", 324)
                put("Data", JSONObject())
                put("RequestID", UUID.randomUUID().toString().replace("-", ""))
                put("MainboardID", mainboardId)
                put("TimeStamp", System.currentTimeMillis())
                put("From", 1)
            })
        }
        return request.toString()
    }

    /**
     * Handle an incoming WebSocket message.
     */
    private fun handleIncomingMessage(text: String) {
        try {
            Log.d(TAG, "Received message: ${text.take(500)}")
            val json = JSONObject(text)
            val id = json.optString("Id", "")
            if (id.isNotEmpty() && printerId.isEmpty()) {
                printerId = id
            }

            val data = json.optJSONObject("Data") ?: run {
                Log.w(TAG, "No Data object in message")
                return
            }
            val mainboardId = data.optString("MainboardID", "")
            if (mainboardId.isNotEmpty() && this.mainboardId.isEmpty()) {
                this.mainboardId = mainboardId
            }

            val cmd = data.optInt("Cmd", -1)
            val keyList = data.keys().run { mutableListOf<String>().also { while (this.hasNext()) it.add(this.next()) }}
            Log.d(TAG, "Message Cmd=$cmd, keys=$keyList")

            // Cmd 324 response contains material data
            // Response structure: {"Data":{"Cmd":324,"Data":{"canvas_list":[...]}}}
            // The actual tray data is nested inside Data.Data
            val innerData = data.optJSONObject("Data")
            if (cmd == 324 && innerData != null && innerData.has("canvas_list")) {
                val parsed = parseMaterialData(innerData)
                _materialData = parsed
                onMaterialData?.invoke(parsed)
                Log.d(TAG, "Received material data: ${parsed.allTrays.size} trays")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse message: $text", e)
        }
    }

    /**
     * Parse the material data from a Cmd 324 response.
     */
    private fun parseMaterialData(data: JSONObject): CanvasMaterialData {
        val activeCanvasId = data.optInt("active_canvas_id", -1)
        val activeTrayId = data.optInt("active_tray_id", -1)
        val autoRefill = data.optInt("auto_refill", 0)

        val canvasList = mutableListOf<CanvasUnit>()
        val canvasArray = data.optJSONArray("canvas_list")
        if (canvasArray != null) {
            for (i in 0 until canvasArray.length()) {
                val canvas = canvasArray.getJSONObject(i)
                val canvasId = canvas.optInt("canvas_id", 0)
                val connected = canvas.optInt("connected", 0) == 1

                val trayList = mutableListOf<CanvasTray>()
                val trayArray = canvas.optJSONArray("tray_list")
                if (trayArray != null) {
                    for (j in 0 until trayArray.length()) {
                        val tray = trayArray.getJSONObject(j)
                        trayList.add(CanvasTray(
                            trayId = tray.optInt("tray_id", 0),
                            brand = tray.optString("brand", ""),
                            filamentType = tray.optString("filament_type", ""),
                            filamentName = tray.optString("filament_name", ""),
                            filamentCode = tray.optString("filament_code", ""),
                            filamentColor = tray.optString("filament_color", ""),
                            minNozzleTemp = tray.optInt("min_nozzle_temp", 0),
                            maxNozzleTemp = tray.optInt("max_nozzle_temp", 0),
                            status = tray.optInt("status", 0),
                        ))
                    }
                }

                canvasList.add(CanvasUnit(canvasId, connected, trayList))
            }
        }

        return CanvasMaterialData(activeCanvasId, activeTrayId, autoRefill, canvasList)
    }

    /**
     * Disconnect from the printer.
     */
    fun disconnect() {
        reading = false
        try {
            outputStream?.write(buildWebSocketFrame(byteArrayOf(), 0x8, false))
            outputStream?.flush()
        } catch (_: Exception) { }
        cleanupSocket()
        _connectionState = ConnectionState.Disconnected
        onConnectionStateChanged?.invoke(_connectionState)
    }

    private fun cleanupSocket() {
        reading = false
        try {
            outputStream?.close()
            inputStream?.close()
            socket?.close()
        } catch (_: Exception) { }
        outputStream = null
        inputStream = null
        socket = null
    }

    /** Clean up resources. Call from ViewModel.onCleared(). */
    fun cleanup() {
        disconnect()
        executor.shutdown()
    }
}

/**
 * WebSocket frame types.
 */
sealed class WebSocketFrame {
    data class Text(val payload: String) : WebSocketFrame()
    data class Binary(val payload: ByteArray) : WebSocketFrame()
    data class Close(val payload: ByteArray) : WebSocketFrame()
    data class Ping(val payload: ByteArray) : WebSocketFrame()
    data class Pong(val payload: ByteArray) : WebSocketFrame()
}

/**
 * Connection state for a printer WebSocket.
 */
sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    object Connected : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}
