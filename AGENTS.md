# AGENTS.md — Repo Guide for AI Agents

## What This Is

**Blumlaut's Filament Tag Writer** — an Android app (Kotlin + Jetpack Compose) that reads, writes, and catalogs NTAG213 RFID tags on 3D printer filament spools.

Tag data follows the **ELEGOO EPC-256 spec** (32 bytes). The reference doc is at:

> `ELEGOO-RFID-Tag-Guide/README.md` — *always check this before touching encoding/NFC logic.*

The full implementation plan is in `PLAN.md`. Read it before making structural changes.

---

## Stack

| Layer | Tech |
|-------|------|
| Language | Kotlin 2.1.20, JVM Target 21 |
| UI | Jetpack Compose + Material 3 |
| Navigation | Compose Navigation |
| Database | Room (KTX coroutines) |
| NFC | `android.nfc.tech.Ntag213` (raw block I/O) |
| Build | Gradle 8.13, AGP 8.12.2, KSP, Version Catalog |
| SDK | minSdk 26, targetSdk 35, compileSdk 35 |

---

## Project Layout

```
app/src/main/java/com/blumlaut/filamenttagwriter/
├── FilamentTagApp.kt              # Application singleton (init Room DB)
├── MainActivity.kt                # NavHost + NFC intent forwarding
├── data/
│   ├── model/
│   │   ├── Filament.kt            # Data class (mirrors EPC-256 fields)
│   │   └── Epc256Encoder.kt       # Filament ↔ 32-byte array ↔ NTAG page blocks
│   └── local/
│       ├── FilamentEntity.kt      # Room entity
│       ├── FilamentDao.kt         # CRUD DAO (Flow + suspend)
│       └── FilamentDatabase.kt    # Room DB singleton
├── nfc/
│   └── NfcReaderWriter.kt         # NTAG213 connect/auth/read/write
└── ui/
    ├── theme/                     # Compose theme (Color, Theme, Type)
    └── screens/                   # HomeScreen, ReadScreen, WriteScreen, CatalogScreen
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
- Data starts at NTAG213 page `0x04`, occupies pages `0x04–0x1B` (32 bytes).
- Default auth password: `0xA0A1A2A3`.
- When in doubt, cross-reference `ELEGOO-RFID-Tag-Guide/README.md` §2 and §3.

### Room / Data Layer
- `FilamentEntity` is the Room entity. `Filament` is the domain model.
- Map between them at the ViewModel/use-case boundary (not in DAO).
- DAO returns `Flow<List<FilamentEntity>>` for collections, `suspend` for single items.

### UI
- Compose only — no XML layouts (except `themes.xml` for the app theme entry point).
- Screens are stateless `@Composable` functions that take a `NavController`.
- Business logic belongs in ViewModels, not in screen composables.
- Use `Material3` components.

### Dependencies
- Always add new dependencies via `gradle/libs.versions.toml` (version catalog).
- Reference them in `app/build.gradle.kts` as `libs.<group>.<name>`.

### Build
- Run `./gradlew :app:assembleDebug` to build.
- Run `./gradlew :app:dependencies --configuration debugCompileClasspath` to inspect deps.
- Environment: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk`, `ANDROID_HOME=/opt/android-sdk`.

---

## Commit Rules

**Every change must be committed with a meaningful commit message.** Use conventional commits:

```
feat: add filament form screen with color picker
fix: handle NTAG213 auth failure gracefully
refactor: extract NFC foreground dispatch into helper
chore: update Compose BOM to 2025.04.01
docs: update PLAN.md with Phase 3 details
```

Never commit without a descriptive message. Never squash multiple unrelated changes into one commit.

---

## Current State (Phase 1 Complete)

- ✅ Project scaffold, Gradle, version catalog
- ✅ Data models + EPC-256 encoder/decoder
- ✅ Room database (entity, DAO, DB)
- ✅ NFC read/write skeleton
- ✅ Compose navigation stubs (4 screens)
- ⏳ Phase 2: Wire up Room, build FilamentFormScreen, make Catalog functional
- ⏳ Phase 3: NFC foreground dispatch, real tag read flow
- ⏳ Phase 4: NFC write flow with tag validation
- ⏳ Phase 5: Polish, error handling, release config
