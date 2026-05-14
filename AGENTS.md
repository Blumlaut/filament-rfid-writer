# AGENTS.md — Repo Guide for AI Agents

## What This Is

**Filament RFID Writer** — an Android app (Kotlin + Jetpack Compose) that reads, writes, and catalogs NTAG213 RFID tags on 3D printer filament spools.

The tag encoding format is based on **community reverse-engineering** by [DnG-Crafts/ELG-RFID](https://github.com/DnG-Crafts/ELG-RFID) and [Savion/elegoo-rfid-editor](https://github.com/Savion/elegoo-rfid-editor). The official ELEGOO documentation contains errors and **must not** be used as reference.

> **Always check `Epc256Encoder.kt` and `NfcReaderWriter.kt` before touching encoding/NFC logic** — they contain detailed byte-offset comments.

---

## Stack

| Layer | Tech |
|-------|------|
| Language | Kotlin 2.3.20, JVM Target 21 |
| UI | Jetpack Compose + Material 3 Expressive |
| Navigation | Compose Navigation |
| Database | Room 2.7 |
| NFC | `MifareUltralight` API (NTAG213 is a Ultralight EV1 variant) |
| Build | Gradle 9.5, AGP 9.1, KSP 2.3, Version Catalog |
| SDK | minSdk 26, targetSdk 35 |

---

## Project Layout

```
app/src/main/java/com/blumlaut/filamenttagwriter/
├── FilamentTagApp.kt              # Application singleton (init Room DB)
├── MainActivity.kt                # NavHost + NFC intent forwarding
├── FilamentViewModel.kt           # Shared ViewModel (catalog, CRUD, autofill)
├── data/
│   ├── model/
│   │   ├── Filament.kt            # Data class (mirrors tag fields)
│   │   ├── Epc256Encoder.kt       # Filament ↔ 36-byte array ↔ NTAG page blocks
│   │   ├── Materials.kt           # Material/subtype codes + default temps
│   │   └── NameParser.kt          # Smart autofill (parse name → material, subtype, color)
│   └── local/
│       ├── FilamentEntity.kt      # Room entity
│       ├── FilamentDao.kt         # CRUD DAO (Flow + suspend)
│       └── FilamentDatabase.kt    # Room DB singleton
├── nfc/
│   └── NfcReaderWriter.kt         # NTAG213 connect/auth/read/write
└── ui/
    ├── theme/                     # M3 Expressive theme (Color, Theme, Type)
    └── screens/                   # Home, Read, Write, Catalog, FilamentForm
```

```
gradle/libs.versions.toml          # All dependency versions — add new deps here
settings.gradle.kts                 # Repos + project name
build.gradle.kts                    # Root (plugin declarations only)
app/build.gradle.kts                # App module (plugins, android config, deps)
```

---

## Key Conventions

### NFC / Tag Encoding
- **All encoding logic** lives in `Epc256Encoder.kt` and `NfcReaderWriter.kt`.
- Tag format is **big-endian**, fields are **zero-padded to 4-byte blocks** on the tag.
- Filament data starts at **page 16 (decimal, 0x10)**, occupies **pages 16–24** (36 bytes, byte offsets 0x40–0x63).
- Pages 0–15 are system/NDEF, pages 25+ are reserved/config.
- Default auth password: `0xA0A1A2A3` (two-step NTAG213 password protocol).
- Uses `MifareUltralight` API (not `Ntag213` directly) — NTAG213 is a Mifare Ultralight EV1 variant.
- When in doubt, read the inline comments in `Epc256Encoder.kt` and `NfcReaderWriter.kt`.

### Room / Data Layer
- `FilamentEntity` is the Room entity. `Filament` is the domain model.
- Map between them at the ViewModel boundary (not in DAO).
- DAO returns `Flow<List<FilamentEntity>>` for collections, `suspend` for single items.

### UI
- Compose only — no XML layouts (except `themes.xml` for the app theme entry point).
- Screens are stateless `@Composable` functions that take a `NavController`.
- Business logic belongs in ViewModels, not in screen composables.
- Use **Material 3 Expressive** components (Foundational intensity):
  - Shape scale: extraSmall (4dp) → extraLarge (28dp)
  - FAB uses `CircleShape` (explicit, not inherited)
  - Color swatches use `CircleShape` with semantic `outline` border
  - Forms use `ExposedDropdownMenuBox` for dropdown fields
  - Search bars use `extraLarge` pill shape
- M3 Expressive skill installed at `@.agents/skills/material-3-expressive/`.

### Dependencies
- Always add new dependencies via `gradle/libs.versions.toml` (version catalog).
- Reference them in `app/build.gradle.kts` as `libs.<group>.<name>`.

### Build
- Run `./gradlew :app:assembleDebug` to build.
- Run `./gradlew :app:dependencies --configuration debugCompileClasspath` to inspect deps.
- Environment: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk`, `ANDROID_HOME=/opt/android-sdk`.
- Java 21 is forced via `org.gradle.java.home` in `gradle.properties`.

---

## Commit Rules

**Every change must be committed with a meaningful commit message.** Use conventional commits:

```
feat: add filament form screen with color picker
fix: handle NTAG213 auth failure gracefully
refactor: extract NFC foreground dispatch into helper
chore(deps): update Compose BOM to 2025.08.00
docs: update README with new features
```

Never commit without a descriptive message. Never squash multiple unrelated changes into one commit.

---
