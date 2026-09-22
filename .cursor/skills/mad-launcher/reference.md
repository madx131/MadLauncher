# MAD Launcher — reference

## Layout (high signal)

| Area | Paths under `app/src/main/java/com/madlauncher/` |
|------|--------------------------------------------------|
| VM / home | `data/LauncherViewModel.kt`, `data/LocalStore.kt` |
| UI shell | `ui/LauncherScreen.kt`, `ui/HomeDesktop.kt`, `ui/FloatingWindow.kt` |
| Notifs | `notif/MadNotificationListener.kt`, `NotificationHub.kt`, `SystemBadgeHub.kt`, `NotifClassifier.kt`, `ShadeFilter.kt` |
| QS | `data/SystemQsMirror.kt`, `QsTileActions.kt`, `QsStateReader.kt`, `QsCatalog.kt`, `ui/QuickSettingsPanel.kt` |
| Nav | `a11y/MadAccessibilityService.kt`, `NavShortcutBridge.kt`, `LauncherCommands.kt`, `MadApp.kt`. Overlays gated by `experimentalNavShortcuts` (off by default). |
| ActionBox | `ui/ActionBox.kt`, `action/ActionApps.kt`, `CommandDetector.kt`, `IntentExecutor.kt` |
| Version | `app/build.gradle.kts` |

## Anti-patterns (learned the hard way)

1. **Snooze messaging to hide OEM shade, then badge active∪snoozed** → read chats look unread forever.
2. **Trust `click-tile mute/cell` exit code** → opens Settings / Internet panel instead of toggling.
3. **`homePressed` closes panels + Activity REORDER_TO_FRONT fires Home** → nav shortcuts appear broken; suppress Home on panel open.
4. **Wide static Discord/Instagram lists for “Notifications” window** → ignore; use ActionBox `@` package set only (messaging + email).
5. **BootBurst / parallel app load “optimizations”** → slowed HyperOS boot; keep sequential unless proven otherwise.
6. **Recents Clear all orphans TYPE_ACCESSIBILITY_OVERLAY** → service stays up but buttons gone; rebuild via watchdog + onResume + SystemUI window events (never rely on `onInterrupt` alone).

## Reconnect habit

If adb shows nothing or wrong device: `adb kill-server` → `adb start-server` → `adb devices -l`. Install only on a serial that is `device` (not `offline`).
