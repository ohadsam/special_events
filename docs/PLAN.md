# PLAN — Special Events (אירועים מיוחדים)
Status legend: [ ] todo · [~] in progress · [x] done. After each batch: CI green → `release-checklist` → version + What's New → update this file.
Constraint: no local Gradle (see CLAUDE.md); CI is the gate. Keep batches small. Spec details: `docs/SPEC.md` `## S<N>`.
Order is by dependency: pure logic first (cheap to test), then data, UI, engines, integrations, polish.

| # | Batch | Spec | Version | Status |
|---|---|---|---|---|
| 0 | Foundation: Gradle project + wrapper, version catalog, theme (light/dark/dynamic), RTL nav shell, AppLogger, What's New + version, APK naming, CI green, `latest` release workflow | S0 | 0.1.0 | [x] |
| 1 | Calendar core (pure Kotlin): Hebrew↔Gregorian, annual-date rules, age/milestones/anniversary names, bar/bat mitzvah, parasha, holidays, zmanim; golden tests + Hebcal probe workflow | S1 | 0.2.0 | [x] |
| 2 | Data model: Room (events, tags, rules, recipients, templates, checklist, sent-log, occurrence cache), DataStore settings, repositories, migrations/schema export, tests | S2 | 0.3.0 | [x] |
| 3 | Core UI: Upcoming home, quick-add + full editor (dual date auto-fill), event screen, tags/colors/emoji suggestions, search/filter, settings shell | S3,S15 | 0.4.0 | [x] |
| 4 | Calendar views: agenda/day/week/month/year, pinch-zoom, Hebrew/Gregorian/dual, overlays (holidays, parasha, bar-mitzvah Shabbat) | S4 | 0.5.0 | [x] |
| 4b | חיפוש מתקדם: טקסט + תאריכים/טווחים עבריים ולועזיים + פילטרים משולבים + חיפוש בתוך חיפוש (refine) + חיפוש טבעי בעברית + הצעות/חיפושים שמורים | S17 | 0.5.5 | [x] |
| 5 | Reminder engine: planner (pure) + Shabbat/Yom-Tov shifting, AlarmManager, receivers, boot/TZ repair, channels per sound/vibration profile, notification actions, permission health | S5 | 0.6.0 | [x] |
| 6 | Reminders UI + smart milestone suggestions + per-type defaults/reset | S6 | 0.7.0 | [x] |
| 7 | Contacts & photos: link contact, import birthdays, photo picker, avatar | S7 | 0.8.0 | [ ] |
| 8a | Greetings: templates (built-in + custom, variables), recipients ("me"/contact/manual), one-tap WhatsApp/email/SMS, timing rules (tzeit/midnight/morning/evening), sent-history | S8 | 0.9.0 | [ ] |
| 8b | Greetings extras: voice message recording + share, song suggestion links; true auto-send (SMS; email/WhatsApp-API gated on owner decision) | S8 | 0.10.0 | [ ] |
| 9 | Per-event checklist + gift tracker + templates | S9 | 0.11.0 | [ ] |
| 10 | Widgets (Glance): upcoming, single event + interactive checklist, countdown, Hebrew date, mini-month; config screens | S10 | 0.12.0 | [ ] |
| 11 | Backup/export/import/share: JSON (versioned), scheduled folder backup, encrypted backup, CSV/ICS, `.sevents` open-with, share-APK | S11 | 0.13.0 | [ ] |
| 12 | Calendar sync: CalendarContract import/export (Google + others), dedupe, dedicated calendar | S12 | 0.14.0 | [ ] |
| 13 | Print & rich visuals: WebView/JS print layouts, PDF, greeting-card image, emoji/sticker pack | S13 | 0.15.0 | [ ] |
| 14 | Security & privacy hardening: app lock, FLAG_SECURE, Keystore secrets, threat model, permission audit, dependency scan | S14 | 0.16.0 | [ ] |
| 15 | Polish: a11y pass, onboarding, help/ⓘ, Baseline Profile, emulator UI tests + screenshots, signing, README | S15,S16 | 0.17.0 → 1.0.0 | [ ] |

## Per-batch routine (skill `android-native-dev` + `release-checklist`)
1) read spec slice 2) implement + unit tests 3) push, CI green (`gh run watch`) 4) 4-persona review 5) version + What's New + docs 6) merge to `main` (release `latest` APK is built).
Each batch ends with a short **"suggestions"** note (in AGENT.md → Ideas) of what could improve that area.

## Test strategy
- **Unit (JVM, pure)**: calendar golden tables, rule edge-cases, planner/shift, templates, codecs, backup schema. Written with each batch.
- **Probe workflows (manual)**: Hebcal conversion/parasha cross-check against the live service.
- **Instrumented (emulator, `ui-tests.yml`)**: smoke flows (add event, view switch, reminder fires), Room migration tests, screenshots for visual review.
- **Static**: ktlint, detekt, Android lint, dependency graph. Performance: Baseline Profile (B15), no main-thread DB/IO, lazy lists.

## Autonomy
Owner asked (2026-10-17) to plan then develop continuously batch by batch, push to GitHub, merge to main. Open questions go to `AGENT.md` → Open items (and `SPEC.md` Appendix A), not as blockers; defaults marked **[החלטה]** apply meanwhile.

## Ideas backlog (not scheduled — decide after the core ships)
Gift ideas by relation/age · "year in review" stats · shared family calendar via exported link · relationship graph ("אמא של…") · launcher shortcuts + Quick-Settings "add event" tile · share-target (add event from shared text) · yahrzeit extras (candle reminder, link to Tehilim Daily for ilui nishmat) · Wear OS glance · DND-aware delivery · localized (Yiddish/Russian/French) greetings · widget themes marketplace.
