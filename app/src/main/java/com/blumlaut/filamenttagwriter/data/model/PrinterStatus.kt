package com.blumlaut.filamenttagwriter.data.model

/**
 * Printer state from Cmd 0 (GET_PRINTER_STATUS).
 * See docs/printer-api.md for full protocol reference.
 */

/**
 * High-level printer print state.
 */
enum class PrintState(val code: Int) {
    Idle(0),
    Printing(1),
    Paused(2),
    PausedAlt(6),
    Done(8),
    Error(9);

    companion object {
        fun fromCode(code: Int): PrintState =
            entries.find { it.code == code } ?: Idle
    }
}

/**
 * Full printer status snapshot from Cmd 0 response.
 * Pushed on topic `sdcp/status/<mainboard-id>`.
 */
data class PrinterStatus(
    val currentStatusCodes: List<Int> = emptyList(),
    val timeLapseStatus: Int = 0,
    val platformType: Int = 0,
    val amsConnectStatus: Int = 0,
    val tempNozzle: Float = 0f,
    val tempNozzleTarget: Float = 0f,
    val tempHotbed: Float = 0f,
    val tempHotbedTarget: Float = 0f,
    val tempBox: Float = 0f,
    val tempBoxTarget: Float = 0f,
    val currentCoord: String = "0.00,0.00,0.00",
    val fanModel: Int = 0,
    val fanAuxiliary: Int = 0,
    val fanBox: Int = 0,
    val zOffset: Float = 0f,
    val printInfo: PrintInfo = PrintInfo(),
) {
    /** Whether CANVAS AMS is connected. */
    val hasAms: Boolean
        get() = amsConnectStatus == 1

    /** Whether the printer is actively printing. */
    val isPrinting: Boolean
        get() = printInfo.state == PrintState.Printing

    /** Whether the printer is heating (targets > 0 but not yet printing). */
    val isHeating: Boolean
        get() = tempNozzleTarget > 0f || tempHotbedTarget > 0f

    /** Overall print state derived from PrintInfo.Status. */
    val printState: PrintState
        get() = printInfo.state
}

/**
 * Print job information nested inside PrinterStatus.
 */
data class PrintInfo(
    val status: Int = 0,
    val currentLayer: Int = 0,
    val totalLayer: Int = 0,
    val currentTicks: Int = 0,
    val totalTicks: Int = 0,
    val filename: String = "",
    val taskId: String = "",
    val printSpeedPct: Int = 100,
    val progress: Int = 0,
) {
    val state: PrintState
        get() = PrintState.fromCode(status)

    /** Whether there is an active or recent print job. */
    val hasJob: Boolean
        get() = taskId.isNotBlank() || filename.isNotBlank()

    /** Layer progress as fraction 0..1. */
    val layerProgress: Float
        get() = if (totalLayer > 0) currentLayer.toFloat() / totalLayer else 0f

    /** Time progress as fraction 0..1. */
    val timeProgress: Float
        get() = if (totalTicks > 0) currentTicks.toFloat() / totalTicks else 0f
}

/**
 * Printer attributes from Cmd 1 (GET_PRINTER_ATTR).
 * Pushed on topic `sdcp/attributes/<mainboard-id>`.
 */
data class PrinterAttributes(
    val name: String = "",
    val machineName: String = "",
    val brandName: String = "",
    val protocolVersion: String = "",
    val firmwareVersion: String = "",
    val xyzSize: String = "",
    val mainboardIP: String = "",
    val mainboardMAC: String = "",
    val mainboardId: String = "",
    val networkStatus: String = "",
    val usbDiskStatus: Int = 0,
    val capabilities: List<String> = emptyList(),
    val supportFileType: List<String> = emptyList(),
    val cameraStatus: Int = 0,
    val remainingMemory: Long = 0,
    val devicesStatus: DevicesStatus = DevicesStatus(),
) {
    /** Whether camera/video stream is available. */
    val hasCamera: Boolean
        get() = cameraStatus == 1

    /** Free storage in human-readable MB. */
    val remainingMemoryMB: Long
        get() = remainingMemory / (1024 * 1024)
}

data class DevicesStatus(
    val sgStatus: Int = 0,
    val zMotorStatus: Int = 0,
    val xMotorStatus: Int = 0,
    val yMotorStatus: Int = 0,
) {
    val allHealthy: Boolean
        get() = sgStatus == 1 && zMotorStatus == 1 && xMotorStatus == 1 && yMotorStatus == 1
}
