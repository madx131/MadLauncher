# MAD Launcher language packs

JSON overlays are copied into the APK at build (`assets/lang`). Public builds do not need a GitHub token to use Hebrew, Russian, and the other bundled packs.

The GitHub repo is public. Listing and downloading this folder works without a token. Uploading reports still needs a repository token. Do not treat this folder as a CDN.

English UI is bundled in the app (`strings.xml`). `eng.json` is the translator master and is not installed as an overlay.

See `README.txt` in this folder for the ISO 639-2/T filename list (`heb.json`, `rus.json`, …).
