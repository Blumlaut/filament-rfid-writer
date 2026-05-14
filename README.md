# Filament RFID Writer

<img src="https://raw.githubusercontent.com/Blumlaut/filament-rfid-writer/main/docs/logo.png" alt="Filament RFID Writer" width="96" align="left" style="margin-right:16px" />

An Android app that reads, writes, and catalogs NTAG213 RFID tags on 3D printer filament spools.

<br clear="left"/>

## Demo

![Demo](https://raw.githubusercontent.com/Blumlaut/filament-rfid-writer/main/docs/demo.mp4)

## Features

- **Read tags** — hold your phone near a filament spool tag to see its data
- **Write tags** — pick a filament from your catalog and write it to a blank tag
- **Catalog** — save, edit, and search your filament profiles
- **Smart autofill** — type a name like "Elegoo PLA+ White" and material, subtype, and color fill in automatically

## Screenshots

| Read Tag | Filament Form | Catalog |
|:---:|:---:|:---:|
| ![Read Tag](https://raw.githubusercontent.com/Blumlaut/filament-rfid-writer/main/docs/app-pic-1.jpg) | ![New Filament](https://raw.githubusercontent.com/Blumlaut/filament-rfid-writer/main/docs/app-pic-2.jpg) | ![Catalog](https://raw.githubusercontent.com/Blumlaut/filament-rfid-writer/main/docs/app-pic-3.jpg) |

## Credits

The tag encoding format is based on community reverse-engineering by [DnG-Crafts](https://github.com/DnG-Crafts/ELG-RFID) and [Savion](https://github.com/Savion/elegoo-rfid-editor). Many thanks to both for their thorough work. The official ELEGOO documentation contains errors and should not be relied upon.

## Building

```bash
./gradlew :app:assembleDebug
```

Requires Java 21.

## License

MIT, see [LICENSE](LICENSE).
