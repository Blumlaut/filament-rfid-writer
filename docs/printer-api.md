# ELEGOO Centauri Carbon — Printer API Reference

All communication uses a raw WebSocket at `ws://<ip>:3030/websocket` (non-standard server; OkHttp handshake fails, use raw TCP + manual framing).

No authentication required. Printer closes the socket after responses; reconnect for each request.

## Outgoing Message Format

```json
{
  "Id": "<printer-id-or-empty>",
  "Data": {
    "Cmd": <command_number>,
    "Data": <payload>,
    "RequestID": "<uuid-without-dashes>",
    "MainboardID": "<mainboard-id-or-empty>",
    "TimeStamp": <unix_epoch_seconds>,
    "From": 1
  }
}
```

- `Id` and `MainboardID` can be empty on first request; populate from responses afterward.
- `TimeStamp` is **seconds** (not milliseconds).

## Incoming Response

Two types arrive per request:

1. **Ack** on topic `sdcp/response/<mainboard-id>` — `Cmd` matches request, `Data.Ack: 0` = OK.
2. **Data push** on topic `sdcp/status/...`, `sdcp/attributes/...`, etc. — contains the actual payload.

Data pushes have no top-level `Data.Cmd`; identify by `Topic` or by presence of `Status`, `Attributes`, etc.

## Commands

| Cmd | Name | Payload | Response Topic |
|-----|------|---------|----------------|
| 0 | `GET_PRINTER_STATUS` | `{}` | `sdcp/status/<id>` |
| 1 | `GET_PRINTER_ATTR` | `{}` | `sdcp/attributes/<id>` |
| 128 | `SEND_PRINTER_START_PRINT` | — | — |
| 129 | `SEND_PRINTER_SUSPEND_PRINT` | — | — |
| 130 | `SEND_PRINTER_STOP_PRINT` | — | — |
| 255 | `SEND_PRINTER_SEND_FILE_END` | — | — |
| 259 | `GET_PRINTER_FILE_LIST` | `{Url: "/local"}` | — |
| 260 | `GET_PRINTER_FILE_DETAIL` | — | — |
| 320 | `GET_PRINTER_HISTORY_ID` | `{}` | — |
| 321 | `GET_PRINTER_TASK_DETAIL` | `{Id: "<task-id>"}` or `{}` | — |
| 322 | `DELETE_PRINTER_HISTORY` | — | — |
| 323 | `GET_PRINTER_HISTORY_VIDEO` | — | — |
| 324 | `GET_MATERIAL_DATA` | `{}` | — (see [canvas-api.md](canvas-api.md)) |
| 386 | `EDIT_PRINTER_VIDEO_STREAMING` | — | — |
| 387 | `EDIT_PRINTER_TIME_LAPSE_STATUS` | — | — |

## Cmd 0 — Printer Status

### Response (`sdcp/status/<mainboard-id>`)

```json
{
  "Status": {
    "CurrentStatus": [0],
    "TimeLapseStatus": 0,
    "PlatFormType": 0,
    "AmsConnectStatus": 1,
    "TempOfHotbed": 21.9,
    "TempOfNozzle": 23.2,
    "TempOfBox": 21.3,
    "TempTargetHotbed": 0,
    "TempTargetNozzle": 0,
    "TempTargetBox": 0,
    "CurrenCoord": "0.00,0.00,0.00",
    "CurrentFanSpeed": {
      "ModelFan": 0,
      "AuxiliaryFan": 0,
      "BoxFan": 0
    },
    "ZOffset": 0,
    "LightStatus": {
      "SecondLight": 0,
      "RgbLight": [0, 0, 0]
    },
    "PrintInfo": {
      "Status": 0,
      "CurrentLayer": 0,
      "TotalLayer": 0,
      "CurrentTicks": 0,
      "TotalTicks": 0,
      "Filename": "",
      "TaskId": "",
      "PrintSpeedPct": 100,
      "Progress": 0
    }
  },
  "MainboardID": "107514150107625d00004c0000000000",
  "TimeStamp": 1779048207,
  "Topic": "sdcp/status/107514150107625d00004c0000000000"
}
```

### `CurrentStatus` (array of integers)

| Value | Meaning |
|-------|---------|
| 0 | Idle / Standby |
| 1 | Printing (active job) |
| 8 | File transfer complete (triggers file list refresh) |

### `PrintInfo.Status`

| Value | Meaning |
|-------|---------|
| 0 | Idle |
| 1 | Printing |
| 2 | Paused |
| 6 | Paused (alternative — triggers pause handling in UI) |
| 8 | Print completed |
| 9 | Print failed / error |

### `PrintInfo` fields

| Field | Type | Description |
|-------|------|-------------|
| `Status` | int | Print state (see table above) |
| `CurrentLayer` | int | Current layer being printed |
| `TotalLayer` | int | Total layers in the job |
| `CurrentTicks` | int | Elapsed time ticks |
| `TotalTicks` | int | Estimated total time ticks |
| `Filename` | string | Currently printing `.gcode` filename |
| `TaskId` | string | Unique task identifier (empty when idle) |
| `PrintSpeedPct` | int | Print speed as percentage (100 = normal) |
| `Progress` | int | Overall print progress percentage (0–100) |

### Temperature fields

| Field | Unit | Description |
|-------|------|-------------|
| `TempOfNozzle` | °C | Current nozzle temperature |
| `TempTargetNozzle` | °C | Target nozzle temperature (0 = off) |
| `TempOfHotbed` | °C | Current bed temperature |
| `TempTargetHotbed` | °C | Target bed temperature (0 = off) |
| `TempOfBox` | °C | Current chamber temperature |
| `TempTargetBox` | °C | Target chamber temperature |

### Fan speeds (`CurrentFanSpeed`)

| Field | Range | Description |
|-------|-------|-------------|
| `ModelFan` | 0–100 | Part/model cooling fan |
| `AuxiliaryFan` | 0–100 | Side/auxiliary fan |
| `BoxFan` | 0–100 | Chamber exhaust fan |

### Other status fields

| Field | Description |
|-------|-------------|
| `AmsConnectStatus` | CANVAS AMS connected (1) or not (0) |
| `CurrenCoord` | Current XYZ position as `"X.Y,Z.W,A.B"` string |
| `ZOffset` | Z-axis offset (mm) |
| `TimeLapseStatus` | Timelapse recording active (1) or not (0) |
| `PlatFormType` | Platform type identifier |

## Cmd 1 — Printer Attributes

### Response (`sdcp/attributes/<mainboard-id>`)

```json
{
  "Attributes": {
    "Name": "Centauri Carbon",
    "MachineName": "Centauri Carbon",
    "BrandName": "ELEGOO",
    "ProtocolVersion": "V3.0.0",
    "FirmwareVersion": "V1.4.46",
    "XYZsize": "218.88x128.88x220",
    "MainboardIP": "192.168.178.80",
    "MainboardMAC": "80:9d:65:57:f8:ca",
    "MainboardID": "107514150107625d00004c0000000000",
    "SDCPStatus": 0,
    "NetworkStatus": "wlan",
    "UsbDiskStatus": 0,
    "Capabilities": ["FILE_TRANSFER", "PRINT_CONTROL", "VIDEO_STREAM"],
    "SupportFileType": ["gcode"],
    "DevicesStatus": {
      "SgStatus": 1,
      "ZMotorStatus": 1,
      "XMotorStatus": 1,
      "YMotorStatus": 1
    },
    "CameraStatus": 1,
    "RemainingMemory": 6227693568
  },
  "MainboardID": "107514150107625d00004c0000000000",
  "TimeStamp": 1779048273,
  "Topic": "sdcp/attributes/107514150107625d00004c0000000000"
}
```

### Key attributes

| Field | Description |
|-------|-------------|
| `Name` / `MachineName` | Printer display name |
| `BrandName` | Manufacturer ("ELEGOO") |
| `FirmwareVersion` | Firmware version string |
| `XYZsize` | Build volume as `"XxYxZ"` string (mm) |
| `MainboardID` | Unique printer identifier (use in `Id` field) |
| `RemainingMemory` | Free storage (bytes) |
| `Capabilities` | Supported features list |
| `DevicesStatus` | Motor/sensor health (1 = OK) |
| `CameraStatus` | Camera available (1) or not (0) |
| `NetworkStatus` | Network interface ("wlan", "eth", etc.) |

## Cmd 321 — Task Detail

Request with `{Id: "<task-id>"}` for a specific task, or `{}` for current task list.

### Response

```json
{
  "Data": {
    "Cmd": 321,
    "Data": {
      "Ack": 0,
      "HistoryDetailList": [
        {
          "TaskId": "<id>",
          "TaskName": "path/to/file.gcode",
          "ErrorStatusReason": 0
        }
      ]
    }
  }
}
```

`ErrorStatusReason: 45` indicates a print error (shown as warning in UI).

## Video Stream

- **MJPEG stream:** `http://<ip>:3031/video`
- Available when `CameraStatus: 1` in attributes.

## Connection Notes

- Printer sends `Ping` frames; respond with `Pong` or connection drops.
- After sending a response, the printer typically closes the WebSocket.
- Reconnect for each new request (current polling pattern: connect → request → wait for response → reconnect).
- Poll status every ~5 seconds when connected.
