# Filament RFID Writer

<img src="https://raw.githubusercontent.com/Blumlaut/filament-rfid-writer/main/docs/logo.png" alt="Filament RFID Writer" width="96" align="left" style="margin-right:16px" />

An Android app that reads, writes, and catalogs NTAG213 & NTAG215 RFID tags on 3D printer filament spools.

<br clear="left"/>

## Demo

![Demo](https://raw.githubusercontent.com/Blumlaut/filament-rfid-writer/main/docs/demo.mp4)

## Features

- **Read tags** — hold your phone near a filament spool tag to see its data
- **Write tags** — pick a filament from your catalog and write it to a blank tag
- **Catalog** — save, edit, and search your filament profiles
- **Filament catalog search** — search 7,000+ filaments from 50+ manufacturers via the bundled [SpoolmanDB](https://github.com/Donkie/SpoolmanDB) database; tap a result to auto-fill name, material, color, temperature range, diameter, and weight
- **Smart autofill** — type a name like "Elegoo PLA+ White" and material, subtype, and color fill in automatically
- **Network Connectivity** — configure your printer's IP and see which filaments are currently installed, wirelessly import their profiles into the app!

## Screenshots

| Read Tag | Filament Form | Catalog | Printers |
|:---:|:---:|:---:|:---:|
| ![Read Tag](https://raw.githubusercontent.com/Blumlaut/filament-rfid-writer/main/docs/app-pic-1.jpg) | ![New Filament](https://raw.githubusercontent.com/Blumlaut/filament-rfid-writer/main/docs/app-pic-2.jpg) | ![Catalog](https://raw.githubusercontent.com/Blumlaut/filament-rfid-writer/main/docs/app-pic-3.jpg) | ![Printers](https://raw.githubusercontent.com/Blumlaut/filament-rfid-writer/main/docs/app-pic-4.png) |

## Credits

The tag encoding format is based on community reverse-engineering by [DnG-Crafts](https://github.com/DnG-Crafts/ELG-RFID) and [Savion](https://github.com/Savion/elegoo-rfid-editor). Many thanks to both for their thorough work. The official ELEGOO documentation contains errors and should not be relied upon.

The filament catalog search is powered by [SpoolmanDB](https://github.com/Donkie/SpoolmanDB) by [Donkie](https://github.com/Donkie) — a community-maintained database of 3D printing filaments and manufacturers. Used under the [MIT License](https://github.com/Donkie/SpoolmanDB/blob/main/LICENSE). The database is downloaded at build time and bundled as an offline asset.

## Support

If you find this app useful, consider buying me a coffee!

[![Support me on Ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/blumlaut)

## Building

```bash
./gradlew :app:assembleDebug
```

Requires Java 21.

## License

MIT, see [LICENSE](LICENSE).
