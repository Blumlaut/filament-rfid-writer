# Retrieving CANVAS (AMS) Filament Data from the Centauri Carbon

The ELEGOO Centauri Carbon exposes CANVAS module data over its WebSocket API.
The web UI doesn't surface this information, but it's fully accessible via the protocol.

## Connection

- **URL:** `ws://<printer-ip>:3030/websocket`
- **Protocol:** JSON messages over WebSocket (no authentication required)
- **Printer ID:** returned in responses as `Id` (e.g. `979d4C788A4a78bC777A870F1A02867A`)

## Message Format

Every outgoing message follows this structure:

```json
{
  "Id": "",
  "Data": {
    "Cmd": <command_number>,
    "Data": <command_payload>,
    "RequestID": "<uuid-without-dashes>",
    "MainboardID": "",
    "TimeStamp": <unix_ms>,
    "From": 1
  }
}
```

## Getting CANVAS Filament Data

Send command **324** (`GET_MATERIAL_DATA`) with an empty payload:

```json
{
  "Id": "",
  "Data": {
    "Cmd": 324,
    "Data": {},
    "RequestID": "a1b2c3d4e5f6...",
    "MainboardID": "",
    "TimeStamp": 1778831235000,
    "From": 1
  }
}
```

### Response

The printer acknowledges with `Cmd: 324, Ack: 0`, then pushes the material data on the topic `sdcp/response/<mainboard-id>`:

```json
{
  "active_canvas_id": 0,
  "active_tray_id": -1,
  "auto_refill": 1,
  "canvas_list": [
    {
      "canvas_id": 0,
      "connected": 1,
      "tray_list": [
        {
          "tray_id": 0,
          "brand": "ELEGOO",
          "filament_type": "PETG",
          "filament_name": "PETG",
          "filament_code": "0x00000",
          "filament_color": "#000000",
          "min_nozzle_temp": 220,
          "max_nozzle_temp": 250,
          "status": 0
        },
        {
          "tray_id": 1,
          "brand": "ELEGOO",
          "filament_type": "PLA",
          "filament_name": "PLA",
          "filament_code": "0x00000",
          "filament_color": "#44F1FF",
          "min_nozzle_temp": 190,
          "max_nozzle_temp": 230,
          "status": 0
        }
      ]
    }
  ],
  "Ack": 0
}
```

## Field Reference

### Top-level

| Field | Type | Description |
|-------|------|-------------|
| `active_canvas_id` | int | Currently active CANVAS unit (0 = first/only unit) |
| `active_tray_id` | int | Currently loaded tray (-1 = none loaded into extruder) |
| `auto_refill` | int | Auto-refill mode enabled (1) or disabled (0) |

### Per canvas (`canvas_list[]`)

| Field | Type | Description |
|-------|------|-------------|
| `canvas_id` | int | CANVAS unit index (0-based) |
| `connected` | int | Unit is physically connected (1) or not (0) |

### Per tray (`tray_list[]`)

| Field | Type | Description |
|-------|------|-------------|
| `tray_id` | int | Slot position (0–3 for 4-bay CANVAS) |
| `brand` | string | Filament brand (e.g. `"ELEGOO"`) |
| `filament_type` | string | Material type (`"PLA"`, `"PETG"`, `"TPU"`, etc.) |
| `filament_name` | string | Display name |
| `filament_code` | string | NFC/RFID tag code (`"0x00000"` = no tag read) |
| `filament_color` | string | Hex color (`#RRGGBB`) |
| `min_nozzle_temp` | int | Minimum recommended nozzle temp (°C) |
| `max_nozzle_temp` | int | Maximum recommended nozzle temp (°C) |
| `status` | int | Tray status (0 = OK/ready) |

## Notes

- The printer reports `AmsConnectStatus: 1` in its periodic status updates when a CANVAS unit is connected.
- `filament_code: "0x00000"` means the CANVAS hasn't read an NFC tag for that tray — or the tag isn't in a format the CANVAS recognizes.
- This data is returned on-demand (after Cmd 324) and may also be pushed periodically as part of status updates.
