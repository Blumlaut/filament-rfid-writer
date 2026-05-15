package com.blumlaut.filamenttagwriter.data.model

/**
 * Represents a connected 3D printer with CANVAS AMS support.
 */
data class Printer(
    val id: String = "",
    val name: String = "",
    val ipAddress: String = "",
    val port: Int = 3030,
    val lastSeen: Long = 0,
) {
    /** WebSocket URL for this printer. */
    val wsUrl: String
        get() = "ws://$ipAddress:$port/websocket"
}
