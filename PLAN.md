# Blumlaut's Filament Tag Writer — Project Plan

## Goal

Build an Android app (Kotlin + Jetpack Compose) that can **read**, **write**, and **catalog** RFID
filament tags used on 3D printer spools. The tag format follows the ELEGOO EPC-256 specification.

## Reference

The authoritative spec for the RFID data format is the cloned ELEGOO guide:

> **`ELEGOO-RFID-Tag-Guide/README.md`**

All encoding rules, memory maps, field definitions, and example values come from this document.

---

## Tag Format (from ELEGOO spec)

| Aspect | Detail |
|--------|--------|
| **Tag type** | NTAG213 (NFC, 144 bytes user memory) |
| **Data format** | EPC-256, 32 bytes (256 bits) |
| **Memory range** | User memory pages 0x04–0x1B |
| **Block size** | 4 bytes per page; non-aligned fields zero-padded |
| **Auth** | 4-byte password (default `0xA0A1A2A3`), stored at page 0x2B |

### Fields (32 bytes total)

| Field | Bytes | Address | Notes |
|-------|-------|---------|-------|
| Header | 1 | 0x04 | Always `0x36` |
| Manufacturer Code | 4 | 0x05–0x08 | e.g. `0xEEEEEEEE` for ELEGOO |
| Filament Code | 2 | 0x09–0x0A | Internal manufacturer code |
| Material (Main) | 4 | 0x0B–0x0E | ASCII, e.g. `"PLA "` |
| Material (Subtype) | 4 | 0x0F–0x12 | ASCII, e.g. `"CF20"` |
| Color (RGB888) | 3 | 0x13–0x15 | R, G, B bytes |
| Diameter | 2 | 0x16–0x17 | Hundredths of mm (175 = 1.75mm) |
| Weight | 2 | 0x18–0x19 | Grams |
| Production Date | 2 | 0x1A–0x1B | YYMM format |
| Reserved | 8 | 0x1C–0x23 | Future use, write zeros |

See the ELEGOO guide for encoding tables (materials, colors, diameters, weights).

---

## App Architecture

```
app/
├── data/
│   ├── model/
│   │   ├── Filament.kt           # Data class mirroring EPC-256 fields
│   │   └── Epc256Encoder.kt      # Encode Filament ↔ 32-byte array ↔ NTAG page blocks
│   └── local/
│       ├── FilamentEntity.kt     # Room entity
│       ├── FilamentDao.kt        # CRUD DAO
│       └── FilamentDatabase.kt   # Room database singleton
├── nfc/
│   └── NfcReaderWriter.kt        # NTAG213 read/write via NfcAdapter + Ndef/Ntag213
├── ui/
│   ├── theme/                    # Compose theme (colors, typography)
│   └── screens/
│       ├── HomeScreen.kt         # Landing: Read, Write, Catalog buttons
│       ├── ReadScreen.kt         # NFC foreground dispatch → decode → show filament info
│       ├── WriteScreen.kt        # Pick filament from catalog → NFC foreground → write tag
│       ├── CatalogScreen.kt      # List all saved filaments (Room)
│       └── FilamentFormScreen.kt # Create/edit filament profile
└── MainActivity.kt               # NavHost, NFC intent forwarding
```

---

## Features

### 1. Read Tag

- User taps **"Read Tag"** on the home screen.
- App enables NFC foreground dispatch to catch any NTAG213 tag.
- On tag detected:
  - Authenticate with default password (`0xA0A1A2A3`).
  - Read pages 0x04–0x1B (32 bytes).
  - Decode via `Epc256Encoder.decode()` into a `Filament` object.
  - Display all fields in a readable card (material, color swatch, diameter, weight, date).
  - Offer **"Save to Catalog"** button to store in Room.

### 2. Write Tag

- User navigates to **Catalog**, selects (or creates) a filament profile.
- Taps **"Write to Tag"** on the filament detail.
- App enables NFC foreground dispatch.
- On tag detected:
  - Verify tag is NTAG213.
  - Authenticate with password.
  - Encode `Filament` → 32-byte array → page-aligned blocks via `Epc256Encoder.encodeNtagBlocks()`.
  - Write blocks to pages 0x04–0x1B.
  - Show success/failure feedback.

### 3. Local Catalog

- All filament profiles stored in a Room database (`filaments` table).
- **CatalogScreen**: scrollable list of saved filaments (color swatch + material + name).
- **FilamentFormScreen**: form to create/edit a filament with:
  - Name (free text)
  - Material (dropdown: PLA, PETG, ABS, TPU, PA, CPE, PC, PVA, ASA)
  - Material subtype (free text, max 4 chars: CF, HF, GF, etc.)
  - Color picker (color wheel or hex input → RGB888)
  - Diameter (dropdown: 1.75, 2.85, 3.0, custom)
  - Weight (number input in grams)
  - Production date (year + month pickers)
  - Manufacturer code (editable, default ELEGOO `0xEEEEEEEE`)
- Long-press to delete a filament from the catalog.

---

## NFC Implementation Details

- Use `NfcAdapter` with **foreground dispatch** (`enableForegroundDispatch`) to intercept tag
  intents while the read/write screen is active.
- Use `Ndef` or raw `Ntag213` tech for block-level read/write.
- Handle NFC permissions at runtime (Android 13+ doesn't require runtime NFC permission if
  declared in manifest, but check for NFC hardware availability).
- Disable foreground dispatch when navigating away from read/write screens.
- Show clear UX: "Hold phone near tag..." with spinner, then result card.

---

## Implementation Phases

| Phase | Tasks |
|-------|-------|
| **Phase 1 — Scaffold** | ✅ Project structure, Gradle, manifest, theme, navigation stubs |
| **Phase 2 — Data Layer** | Room database wired up, FilamentFormScreen to create/edit profiles, CatalogScreen listing from DB |
| **Phase 3 — NFC Read** | Foreground dispatch, NTAG213 detection, read pages, decode, display result |
| **Phase 4 — NFC Write** | Select filament, foreground dispatch, authenticate, write blocks, confirm |
| **Phase 5 — Polish** | Error handling, tag validation, color picker UX, app icon, release build config |

---

## Open Questions

- Should we support locking tags after write (NTAG213 lock bytes)?
- Should we allow custom passwords beyond the ELEGOO default?
- Should we support other NTAG sizes (215, 216) or stick to 213 only?
- Should the catalog support photos of spools?
