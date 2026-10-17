# Code map (read this instead of scanning the tree)
`app/src/main/java/com/ohadsam/specialevents/`
- `SpecialEventsApp`, `AppContainer` — application + manual DI (logger now; repos/prefs/scheduler as batches land).
- `MainActivity` — edge-to-edge, forced RTL, theme, `AppRoot`.
- `calendar/` — pure JVM (no Android): `HebrewCalendar`/`HebrewDate`, `HebrewNumerals`, `HebrewDay` (rollover), `SunCalc`, `HolyTimes` (candle lighting/havdalah), `Holidays` (Yom Tov days), `JewishCalendar` (festivals, fasts, Rosh Chodesh), `Parsha` (+`ParshaNames`, year-type pair table), `AnnualDates` (`OriginDate`, `DateRules`, `Occurrence`, `DualDate`), `Milestones`, `BarBatMitzvah`. `location/Places` — built-in places for zmanim.
- `log/` — `AppLogger` (ring buffer + file), `LogBuffer`, `LogModel` (ported from Tehilim_Daily).
- `ui/theme/Theme.kt` — brand light/dark schemes (dynamic color optional). `ui/nav/` — `TopLevel` tabs, `Routes`, `AppRoot` (NavHost + bottom bar).
- `ui/common/PlaceholderScreen` — empty state for features landing later. `ui/settings/` — `SettingsScreen`, `LogsScreen`, `WhatsNewDialog`.
`app/src/main/assets/whatsnew.json` — current version's notes only. `app/sideload.jks` — committed on purpose (see build.gradle.kts).
Tests: `app/src/test` (log, calendar; `GoldenCalendarTest` compares with `src/test/resources/golden/*.csv`, regenerate via `tools/gen_golden.py` + `pip install pyluach==2.3.0`), `app/src/androidTest/SmokeTest`. CI: `.github/workflows/` (android-ci, latest-apk, release, ui-tests).
