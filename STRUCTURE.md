# Code map (read this instead of scanning the tree)
`app/src/main/java/com/ohadsam/specialevents/`
- `SpecialEventsApp`, `AppContainer` — application + manual DI (logger now; repos/prefs/scheduler as batches land).
- `MainActivity` — edge-to-edge, forced RTL, theme, `AppRoot`.
- `log/` — `AppLogger` (ring buffer + file), `LogBuffer`, `LogModel` (ported from Tehilim_Daily).
- `ui/theme/Theme.kt` — brand light/dark schemes (dynamic color optional). `ui/nav/` — `TopLevel` tabs, `Routes`, `AppRoot` (NavHost + bottom bar).
- `ui/common/PlaceholderScreen` — empty state for features landing later. `ui/settings/` — `SettingsScreen`, `LogsScreen`, `WhatsNewDialog`.
`app/src/main/assets/whatsnew.json` — current version's notes only. `app/sideload.jks` — committed on purpose (see build.gradle.kts).
Tests: `app/src/test` (log), `app/src/androidTest/SmokeTest`. CI: `.github/workflows/` (android-ci, latest-apk, release, ui-tests).
