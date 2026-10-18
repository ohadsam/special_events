# Special Events – Agent Memory
**Last updated:** v0.7.0 (batches 0–6 done; batch 7 next)
Read order: CLAUDE.md → docs/PLAN.md → STRUCTURE.md → the batch's section in docs/SPEC.md.

## Recently done
- Bootstrap: dev setup (CI workflows, ktlint/detekt config, editorconfig) and reusable skills `android-native-dev` + `release-checklist` copied from the Tehilim_Daily / ExtendedFileManager practice.

- v0.1.0 (batch 0): app shell + CI green. Unverified on a device: visuals/RTL of the shell, launcher icon look, edge-to-edge insets.
- Search (SPEC S17, batch 4b) added after the owner's request: text + date ranges + filters + refine-in-results + Hebrew natural-language queries.

- v0.2.0 (batch 1): calendar core. Parsha/festival/Hebrew-date logic is verified against pyluach goldens (2000–2100). Modern Israeli days (Yom HaShoah/Zikaron/Atzmaut/Yerushalayim), Omer, special Shabbatot (Shekalim/Zachor/…), Purim Meshulash, zmanim goldens are NOT done.

- v0.3.0 (batch 2): Room v1 (events, tags, event_tags) + DataStore settings + `EventTimeline`. Other tables (reminders, recipients, templates, checklist, sent-log) are added with their batch + a migration. Occurrence cache table deferred: timeline is computed in memory; add the table only if profiling on thousands of events needs it. DAO behaviour is covered by androidTest `EventRepositoryTest` (runs in ui-tests.yml, compile-checked in CI).
- CI commits `app/schemas` automatically when Room's schema changes (bot commit with [skip ci]) — `git pull --rebase` before the next push.

- v0.4.0 (batch 3): core UI. The FAB opens the full editor (no separate quick-add sheet yet — the editor only needs type, name, date). Not verifiable here (needs a device): layout/RTL look of cards and editor, Material DatePicker in Hebrew locale, keyboard/IME behaviour. ViewModels have no unit tests yet (logic lives in tested pure helpers); add Turbine/fake-container tests when the first ViewModel bug appears.

- v0.5.0 (batch 4): calendar views. Not verifiable here (device): pinch-zoom feel/conflicts with scrolling, cell density at 200% font scale, RTL arrow direction, month-cell text clipping. Modern Israeli days and Omer are not in the overlay yet.

- v0.5.5 (batch 4b): advanced search. Natural-language parser covers the phrases in `SearchSuggestions` and the cases in `SearchTest`; unknown words fall back to a text step. The engine only searches occurrences (default window: next 365 days; a date-range chip changes it). Not verifiable here: keyboard/IME search action behaviour, chip layout in RTL.

- v0.6.0 (batch 5): reminder engine. DB is v2 (migration SQL guarded by `MigrationSchemaTest` against the committed schema JSON). New events get the type defaults automatically; rule editing UI is batch 6. Reminders older than 12 h at the moment of a re-plan (device off) are dropped. NOT verifiable here (needs a device): alarm delivery in Doze, exact-alarm permission flow, channel sound/vibration, snooze across Shabbat, notification text layout.

- v0.7.0: reminder editor + suggestions. Global editable defaults per type are NOT built (reset uses `ReminderDefaults`); suggestions cannot be dismissed permanently yet. Not verifiable here: system tone picker result handling, TimePicker look in RTL.

## Known issues / open items (need the owner)
- Feature spec for "אירועים מיוחדים" not yet provided → fill `docs/SPEC.md`, then define batches in `docs/PLAN.md`.
- **Signing key:** `app/sideload.jks` (password in app/build.gradle.kts) is committed on purpose so every CI build updates in place; NOT a store key. Never change it after users installed the app.
- Lint (ktlint/detekt) is advisory (`ignoreFailures`); flip to enforcing once stable.
- Package name / app id `com.ohadsam.specialevents` is tentative.

## Not verifiable in the sandbox (needs a device)
Notification timing, permission flows, visuals, TalkBack.

## Owner decisions needed (defaults are active until confirmed — SPEC Appendix A.1)
- Adar in a leap year (born/yahrzeit in plain Adar): default Adar II. 30 Cheshvan/Kislev in a short month: default 29th (day before). 29 Feb in a common year: default 28 Feb.

## Ideas (v0.7.0)
- Dismiss a suggestion for good (remember its id); edit the per-type default rule sets in Settings; duplicate a rule to other events; bulk-apply a rule to all birthdays.

## Ideas (from batch 5)
- Yahrzeit candle reminder anchored to sunset (rule time = "N minutes before sunset") instead of a fixed 16:00; per-rule custom sound picker (profile `sound:<uri>` is already supported); quiet hours; "mark as greeted" action (batch 8); a daily digest notification option; reminders inheriting from tags.

## Ideas (from batch 4b)
- Voice search (system speech-to-text intent) into the same phrase box; search inside notes with highlights; "events with no reminder / no greeting sent this year" once those exist (batches 5, 8); export search results as CSV/print (batches 11, 13); pin a saved search as a home widget.

## Ideas (from batch 4)
- Day cells should expose a spoken summary (date + number of events) for TalkBack; week-number column; swipe left/right to change period; "dots instead of emoji" density setting; show Shabbat times in the week/day view; drag an event to another day.

## Ideas (from batch 3)
- Quick-add bottom sheet (name + type + date only) from the FAB; swipe actions on cards (archive / delete with Undo snackbar).
- Duplicate event; share event as text; photo/contact link (batch 7); unsaved-change draft restore after process death.
- Time zone aware "today" for travellers; list header showing today's Hebrew date and parasha.

## Ideas (from batch 1)
- Show "Shabbat Mevarchim", special Shabbatot and Omer day in the calendar overlay; modern Israeli holidays with their postponement rules (needs owner confirmation).
- "Birthday falls on Shabbat/Yom Tov" and "bar mitzvah Shabbat" badges in the upcoming list (data is ready).
- Hebrew-letter year/day formatting helper for cards (גרשיים) — `HebrewDate.format()` exists.

## Ideas (from batch 0)
- Dynamic-color / palette picker in Settings (theme already supports `dynamicColor`).
- Show "what's new" automatically once after an update.
- English UI: add `values-en`, then make layout direction follow the locale.
