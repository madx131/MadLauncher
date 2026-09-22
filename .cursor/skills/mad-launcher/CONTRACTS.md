# MAD Launcher — contracts & smoke

Invariants for `main`. A change that breaks one is a regression, not a style preference.

## Invariants

1. **Nav shortcuts**  
   Off by default (`experimentalNavShortcuts`). No overlays and no MAD intercept until Settings → Extras → Experimental is turned on. Placement data is kept. When on: overlay taps open the **system** notification shade / extended Quick Settings (double-swipe) unless MAD windows Experimental is also on. MAD Messages / System & apps / Quick Settings windows stay behind Settings → Extras → Experimental (off by default). When MAD windows is on, taps open MAD floating windows and must not *only* open the stock shade. Recents / Overview is the **system** screen until Settings → Extras → Experimental → MAD Recents window is on (`experimentalMadRecents`, off by default).

2. **Shade ↔ MAD share keys**  
   Dismiss / mark-read in MAD cancels the same notification `key` in the shade. Shade remove clears MAD. Badges = **active** + `Ranking.canShowBadge` only (never snooze ghosts).

3. **QS stock tiles**  
   Sound/mute = ringer cycle via `QsTileActions`. Cell = mobile data (never `ACTION_INTERNET_CONNECTIVITY` / Wi‑Fi Internet panel).

4. **Widget touch**  
   Short tap = widget. Horizontal swipe = home page. Long-press = edit. One owner per mode (`MadAppWidgetHostView`).

5. **Settings apply immediately**  
   Theme, icons, gestures, fonts, and docks persist as soon as the control changes. There is no Apply bar and no unapplied-changes prompt. Icon shape + icon pack from `AppearanceSettings` apply to dock, home, and drawer.

6. **Home suppress on panel open**  
   Opening a panel via nav shortcuts suppresses false Home so `homePressed` does not immediately close it.

7. **ActionBox grammar**  
   `@` messaging + email (open chat), `!` social (open composer), `:` searchable (curated + dynamic `SearchableInfo` discovery).

8. **Wallpaper + theme**  
   Wallpaper pick prefers system crop-and-set when available; MAD fill / no-parallax still applies after. Palettes are **System** (Android colors), **Nord**, and **Wallpaper**. Wallpaper palette seeds surfaces; MAD accent remains primary/accent. Light/dark follows the **system** night mode (no MAD-only Dark/Light override).

## 15-minute phone smoke (`855db4bc`)

1. Accessibility + NLS enabled. Nav overlays stay hidden until Settings → Extras → Experimental → Nav bar shortcuts is on; then 2 overlays visible in another app.  
2. Bell → system notification shade (Experimental off). With Experimental on: MAD Notifications; dismiss one → shade + badge update.  
3. Gear → extended Quick Settings shade (Experimental off). With Experimental on: MAD QS; Sound cycles; cell = mobile data.  
4. Message → badge; open app → clears.  
5. Widget: tap / swipe page / long-press edit. Third-party widgets stay visible after a page change.  
6. Settings → Icons → shape + pack apply immediately to home/dock/drawer. Long-press wallpaper menu has System settings next to Launcher Settings.  
7. Themes: System / Nord / Wallpaper; light/dark follows system; Wallpaper + accent still brand-colored.  
8. Set wallpaper → system crop (or MAD fallback) → palette refreshes.  
9. ActionBox `:yt` / discovered `:` targets still work.

Emulator is OK for Compose/icons/ActionBox. Prove a11y + shade on the phone.
