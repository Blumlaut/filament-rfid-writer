# Privacy Policy — Filament RFID Writer

**Last updated:** 2026-05-15

## No Data Collection

Filament RFID Writer (**com.blumlaut.filamenttagwriter**) does not collect, transmit, sell, or share any personal data.

All data created or imported by the app is stored exclusively on your device and never leaves it.

## What Data the App Uses

- **Filament profiles** — names, material types, colors, temperatures, and other 3D printing parameters you create or that are read from physical RFID tags. Stored locally in a SQLite database on your device.
- **Printer connections** — IP addresses and names of 3D printers you manually add. Stored locally. Communication with printers occurs only over your local network via WebSocket and is initiated entirely by you.
- **NFC tag data** — read from or written to NTAG213 RFID tags held physically against your device. This data is processed locally and is not transmitted over any network.

## Permissions

The app requests the following Android permissions, all of which are required for its core functionality:

- **NFC** — to read from and write to NTAG213 RFID tags on filament spools.
- **Internet** — to communicate with 3D printers on your local network via WebSocket.
- **Network State** — to detect network connectivity for printer communication.

None of these permissions are used to collect or transmit personal data.

## Third-Party Services

This app does not integrate with any analytics, advertising, crash reporting, or other third-party services. No external SDKs transmit data.

## Data Deletion

All data is stored on your device. Uninstalling the app removes all stored data. You can also delete individual filament profiles or printer entries at any time within the app.

## Children

This app is not directed at children under 13 and does not knowingly collect data from children.

## Contact

If you have questions about this privacy policy, please open an issue on the [project repository](https://github.com/blumlaut/blumlauts-filament-tag-writer).
