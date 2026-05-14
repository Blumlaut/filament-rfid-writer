# Filament RFID Writer

An Android app that reads, writes, and catalogs NTAG213 RFID tags on 3D printer filament spools.

## Demo

<video src="docs/demo.mp4" controls></video>

## Features

- **Read tags** — hold your phone near a filament spool tag to see its data
- **Write tags** — pick a filament from your catalog and write it to a blank tag
- **Catalog** — save, edit, and search your filament profiles
- **Smart autofill** — type a name like "Elegoo PLA+ White" and material, subtype, and color fill in automatically

## Credits

The tag encoding format is based on community reverse-engineering by [DnG-Crafts](https://github.com/DnG-Crafts/ELG-RFID) and [Savion](https://github.com/Savion/elegoo-rfid-editor). Many thanks to both for their thorough work. The official ELEGOO documentation contains errors and should not be relied upon.

## Building

```bash
./gradlew :app:assembleDebug
```

Requires Java 21.

## License

MIT, see [LICENSE](LICENSE).
