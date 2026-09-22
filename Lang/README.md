# MAD Launcher language packs

JSON overlays are copied into the APK at build (`assets/lang`). Public builds do not need a GitHub token to use Hebrew, Russian, and the other bundled packs.

Distributor builds can still list and refresh this folder with the GitHub Contents API (`GET /repos/madx131/MadLauncher/contents/lang`) using a repository token. Do not treat this folder as a public CDN.

English UI is bundled in the app (`strings.xml`). `eng.json` is the translator master and is not installed as an overlay.

See `README.txt` in this folder for the ISO 639-2/T filename list (`heb.json`, `rus.json`, …).
