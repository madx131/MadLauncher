---
name: mad-launcher
description: >-
  MAD / Madl Android launcher (Kotlin, Compose, com.madlauncher) conventions:
  versioning, devices, notifications/badges, QS mirroring, nav shortcuts, adb
  ship workflow. Use when working in S:/projects/MadLauncher, MAD/Madl launcher, HyperOS/Xiaomi
  home, NotificationListener, Quick Settings, nav bar overlays, or ActionBox @ apps.
---

# MAD Launcher

Native Android launcher at `S:/projects/MadLauncher` (`com.madlauncher`). Primary user: Mike. Do not commit unless asked.

## Products

Same codebase, two apps:

| Flavor | applicationId | What it is |
|--------|---------------|------------|
| `launcher` (default) | `com.madlauncher` | MAD home. `assembleLauncherDebug` → also copied to `app/build/outputs/apk/debug/app-debug.apk` |
| `actionbox` | `com.madlauncher.actionbox` | Action Box only — not a home app. `assembleActionboxDebug` → `app/build/outputs/apk/actionbox/debug/app-actionbox-debug.apk` |

Action Box opens a translucent field over the current home. Do not monkey-launch it as HOME.

## Devices and ship

| Device | Serial | Notes |
|--------|--------|--------|
| POCO F5 Pro (primary) | `855db4bc` | Xiaomi / HyperOS |
| Samsung Tab SM-T500 | `R9WNB0PHKVJ` | Secondary |
| Redmi Note 7 (lavender) | `d191ccfb` | **MAD OS** — system default; do not require Accessibility |

```powershell
$sdk = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
# Disconnect / reconnect: kill-server then start-server
& $sdk -s 855db4bc install -r "S:\projects\MadLauncher\app\build\outputs\apk\debug\app-debug.apk"
& $sdk -s 855db4bc shell "am force-stop com.madlauncher; monkey -p com.madlauncher -c android.intent.category.LAUNCHER 1"
```

Build MAD: `cd S:/projects/MadLauncher; .\gradlew.bat :app:assembleLauncherDebug`  
Build Action Box: `.\gradlew.bat :app:assembleActionboxDebug`  
(`:app:assembleDebug` builds both.)

Assemble also writes `versions/{Product}-{versionName}-latest.apk` and demotes the previous `-latest` for that product.

## Versioning (`app/build.gradle.kts`)

- Feature bumps after `0.9.15`: **+0.0.05** on the third segment when appropriate
- Patch / fix: bump **fourth** segment (e.g. `0.9.30.18` → `0.9.30.19`) and `versionCode` +1
- Ship after meaningful fixes: bump → assembleDebug → install on connected device
- Local archive: `versions/` (gitignored APKs). New build = `MAD-0.9.116.11-latest.apk` / `ActionBox-0.9.116.11-latest.apk`. GitHub share file stays `releases/MAD-launcher-latest.apk`.

## Notifications and badges (native-like)

**Source of truth:** status-bar shade for live unread.

- **`@` apps** = `ActionApps` Messaging + Email packages (`installedMessagingPkgs`). Keep them **in the shade** (do **not** long-snooze).
- **Notifications window** = one full-shade copy. Internal tabs: Messages (`NotifClassifier.isMessagingWindow`) vs System & apps (everything else + media). Do **not** split into two floating windows.
- **Badges** = **active** notifications + `Ranking.canShowBadge` + `max(1, number)`; skip group summaries. Do **not** count year-long snooze / `hiddenFromShade` ghosts.
- **Two-way sync:** shade dismiss/open → MAD drops that key; MAD dismiss/mark-read → `cancelNotification` on the same key. If the row is **still active** after cancel (HyperOS / ColorOS / some EMUI), snooze that key only — do **not** snooze when cancel already worked (Pixel / Samsung / MAD OS). Do **not** year-snooze `@` apps just to hide them from the shade while they still show in MAD.
- Opening a messaging/email app → `NotificationHub.requestClearPackage` (clear shade + MAD for that package).

Key files: `MadNotificationListener.kt`, `NotificationHub.kt`, `SystemBadgeHub.kt`, `NotifClassifier.kt`, `ShadeFilter.kt`.

**Avoid:** treating snoozed shade-hide as unread; messaging “always unsuppress while snoozed”; counting dismissed keys in badges.
- **Active shade only:** never merge snoozed/`hiddenFromShade` into Messages or System & apps. Legacy year-snooze was removed.

## Quick Settings

System row: `Settings.Secure` `sysui_qs_tiles`. Mirror via `SystemQsMirror`.

On HyperOS, `cmd statusbar click-tile` works for **TileServices**; stock tokens (`mute`, `cell`, `wifi`) often **no-op** (exit 0 but no effect).

- Prefer `QsTileActions` for stock: **Sound/mute** = `AudioManager` ringer cycle (Sound → Vibrate → Mute); **cell** = mobile data toggle (never `ACTION_INTERNET_CONNECTIVITY` — that is Wi‑Fi/Internet).
- Labels: `QsStateReader.liveLabel` for mute/sound.
- Do not fall back Sound → Sound Settings UI for a tap that should cycle profiles.

Key files: `SystemQsMirror.kt`, `QsTileActions.kt`, `QsStateReader.kt`, `LauncherViewModel.toggle`.

## Nav shortcuts

**Off by default** (`experimentalNavShortcuts` in AppearanceSettings). Settings hub does not list them; turn on under Settings → Extras → Experimental, then open the placements editor.

When off: do not show nav-bar overlay buttons, do not intercept nav taps, keep saved placements.

When on: overlays/gestures use saved placements. On **MAD OS** (Note 7 / priv-app), do **not** send the user to Accessibility settings. Nav shortcuts use a11y overlays unless SystemUI actually owns them (`/system/etc/mad/systemui_native` or `STATUS_BAR_SERVICE`). Auto-enable the service in the background. Sideloaded installs still use a11y overlays in `MadAccessibilityService`.

Open panels via:

1. `MadApp.openLauncherPanel(panel)` — no-op if nav shortcuts experimental is off; stock shade when MAD windows Experimental is off; MAD windows when on (`panelOpener` + `LauncherCommands.request`)
2. `MainActivity` extras `open=notifications|quicksettings|systemnotifs`

Home race: opening a **MAD** panel suppresses `goHome` / `homePressed` briefly (`LauncherCommands.isHomeSuppressed`). `homePressed` closes Notifications/QS/System (intentional for real Home).

Do not reopen BootBurst-style boot paths that made cold start slower.

## Recents

**Off by default** (`experimentalMadRecents`). Pinch-out / Recents actions open the **system Overview**. Settings → Extras → Experimental → MAD Recents window turns on MAD's `RecentsPanel` (needs Usage access). The drawer Recent row is unrelated.

## ActionBox

- `@` = messaging + email apps; `:` = curated catalog **plus** dynamic `SearchableInfo` discovery.
- `@`: pick app → pick contact → **open that chat** (ready to type). Do not compose/send from MAD.
- `!`: pick social app → **open the composer** (Facebook/Instagram: leave Reels/feed, tap What's on your mind / Create; do not fill/post).
- **Your commands**: open an app, or a **quick combo** (`@` `!` `#` `*`) with a pre-selected app and optional contact. Not free-form scripts.
- Command pickers rank the **system default** app first. Users edit each command under Settings → Edit dock/s → ActionBox Settings → that command.
- `update` / `updates` / `/u` opens GetApps or Play Store on the updates list (user taps Update all). Hidden: type **do a barrel roll** for a 360° spin.

## Language packs and reports

- Overlay JSON lives in GitHub `lang/` (ISO 639-2/T names: `heb.json`, `rus.json`). English is bundled `strings.xml`; `eng.json` is the translator master and is not shipped in the APK.
- Packs are copied into `assets/lang` at build so public APKs can apply Hebrew and others without a GitHub token.
- Distributor builds can still list and refresh via the GitHub Contents API (`GET /repos/madx131/MadLauncher/contents/lang`) with `MAD_REPO_TOKEN`. Do not use Google Drive or unauthenticated raw URLs.
- Share MAD: GitHub raw APK URLs are private. Send the public APK file from Settings → Extras → Share MAD.
- Compatibility reports: Settings → Extras → Not working → Re-check (probe only) → confirm upload to `reports/compat/`.
- Bug / free-word: Settings → Extras message form → `reports/messages/`.

## Contracts

Before changing notifs / QS / nav / icons / widgets, read [CONTRACTS.md](CONTRACTS.md). Prefer fixing contract breaks over open-ended HyperOS debugging.

## After non-trivial changes

1. Bump patch version + versionCode  
2. `assembleDebug`  
3. `adb install -r` on the connected target (phone if present, else tablet)  
4. Smoke the area touched (nav tap, QS sound/cell, WhatsApp badge) — full list in CONTRACTS.md

## Deeper reference

See [reference.md](reference.md) for file map and anti-patterns.
