# Special Events – Agent Memory
**Last updated:** v0.24.0 (next: batch 15 polish)
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

- v0.8.0: contacts + photos. Contact picker copies name/phone/email/photo once (no permission); import needs READ_CONTACTS at runtime. Photos are copied into files/photos (512px JPEG) and removed when replaced/deleted. Not verifiable here (device): picker/permission flows, photo EXIF rotation, import with real address books (date formats vary by account).

- v0.9.0: greetings (one-tap send). WhatsApp has NO silent-send API, so greeting prompts open the message ready and the user taps Send; the greeting history is confirmed by the user on return. Not verifiable here (device): wa.me/WhatsApp Business hand-off, mail/SMS compose with Hebrew text, notification tap routing to the greeting screen. Owner decisions still open: true auto-send via WhatsApp Business API / SMTP (see SPEC Appendix A.2).

- v0.10.0: batch 8b: voice greeting, song ideas, auto-SMS. Unverified on device: MediaRecorder on real mics, WhatsApp jid deep-link (undocumented extra, falls back to chooser), SMS multipart send, permission dialogs. Rule editor has no template chooser yet (uses best match).

- v0.11.0: batch 9: checklist + gift tracker. Unverified on device: layout at 200% font, Undo snackbar. Widget checkbox interaction comes with batch 10 (reuse ChecklistRepository.setDone).

- v0.12.0: batch 10: widgets. Unverified on device (Glance cannot be previewed here): look at min/max sizes, dark mode, RTL in the month grid, config activity flow, tick-box tap latency. Hebrew date does not roll at sunset.

- v0.13.0: batch 11: backup/export/import/share. Not done: scheduled folder backup (needs WorkManager - row 11c), CSV import with column mapping, photos in backups, settings in backups. Unverified on device: SAF pickers, .sevents VIEW filter matching in different file managers, share sheet, APK share installability.

- v0.14.0: batch 12: calendar sync (one-way import + one-way export to a local calendar). Not done: two-way sync, per-calendar export to Google account, incremental updates, Hebrew-date import. Unverified on device: local calendar visibility in Google Calendar app on various OEMs, permission flow, large calendars.

- v0.15.0: batch V1: views. Not done: saved views (named), group by owner group (after G1), unified single view-switcher screen (Upcoming and Calendar remain two tabs), Hebrew-month picker. Unverified on device: swipe vs scroll conflicts, long-press menu, bottom sheet at 200% font.

- v0.16.0: batch G1: groups. Not done: group photo and font/card styling (needs the card design of S1/P1), sharing a single group/template as its own file (backup covers it), reordering groups, recipients queue UI (T2). Unverified on device: editor layout at 200% font, emoji entry.

- v0.17.0: batch E1: free-text dates. Not done: English input, relative phrases ("לפני שבועיים"), parsing names/relations out of the same sentence (e.g. "אמא של דנה נולדה ב..."), speech input. Unverified on device: field layout, RTL punctuation (maqaf/geresh typed from phone keyboards).

- v0.18.0: batch H1: help system. Not done: help buttons on Search/Templates/Import/Logs screens, per-field info (i) buttons, first-run walkthrough (batch 15), English texts. Update the matching HelpTopic text whenever a screen changes (release-checklist step 5).

- v0.19.0: batch T1: greeting templates v2. Not done: formatting of text typed by hand in the message (only placeholder styles), SMS stays plain by nature, sharing one template as its own file (backup covers it), template reorder. Unverified on device: EXTRA_HTML_TEXT in different mail apps, WhatsApp markers around RTL text.

- v0.20.0: batch T2: multi-recipient sending. Honest limits: WhatsApp and e-mail still need one tap per recipient (no silent API) - only SMS is automatic; WhatsApp Business Cloud API / SMTP remain an owner decision (SPEC S18.7). Unverified on device: SMS to many numbers (carrier throttling), recipient list at 200% font.

- v0.21.0: batch S1: sharing. Not done: person card as an IMAGE (comes with P1 print/visuals), sender name in the file, QR code/link sharing, in-app sharing history. Unverified on device: ACTION_SEND with application/octet-stream to WhatsApp and file managers keeping the .sevents name; VIEW intent for received files.

- v0.22.0: batch P1: printing. Not done: yearly wall calendar (12 pages) and week/day sheets, stickers/emoji pack, JS-based layouts (HTML+CSS was enough), saving PDFs without the print dialog. Unverified on device: print dialog from Compose WebView, A5 sizing on printers, card-as-PNG on devices where an off-screen WebView draws blank (falls back to a message), Hebrew fonts available on the device.

- v0.23.0: batch 14: security. Not done: encrypting the database at rest (SQLCipher + Keystore), BiometricPrompt with crypto binding, hiding widget content when locked, in-app dependency vulnerability scan (Dependabot PRs only). Unverified on device: the system confirm screen flow and the stop/start around it (authInProgress guard), FLAG_SECURE in recents, behaviour when the user removes the device lock while the setting is on (the setting is ignored, not enforced).

- v0.24.0: auto-backup to SAF folder (WorkManager, retention, status) and CSV import with column mapping (11c). Unverified on device: SAF folder write/prune, periodic worker.

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

## Ideas (v0.8.0)
- Re-sync a linked contact (name/photo changed) on demand; import via vCard/CSV files; crop UI for photos; show contact photo in month cells; WhatsApp-style initials avatar when no photo.

## Ideas (v0.9.0)
- Greeting language per recipient; attach a picture/greeting card (batch 13); schedule a greeting for later; per-recipient default channel; group greetings (several recipients); remember the last template used per event.

## Ideas (v0.10.0)
- Template chooser per auto-send rule; delivery receipts (SmsManager sentIntent) to mark failures; WhatsApp Business API / SMTP for true auto-send (owner decision); trim/waveform for voice; share voice via email/SMS MMS; remember last song opened.

## Ideas (v0.11.0)
- Reorder items by drag; link a gift to a contact wish-list; remember last year's gift per person (avoid repeating); checklist items as reminders; shared checklist export with the event; gift ideas by relation/age/budget (from the backlog).

## Ideas (v0.12.0)
- Widget style options (transparency, theme, font size) like Tehilim Daily; widget filtered by tag/type; Hebrew date after sunset; birthdays-with-photo widget; lock-screen/Wear glance; resize-aware layouts (fewer rows when small); week-strip widget.

## Ideas (v0.13.0)
- Scheduled backup to a chosen folder (SAF + WorkManager) with N copies; CSV import with column mapping; include photos in a zip; include settings; share a single event as text/ICS from its screen; Android Auto Backup rules for the DB; compare-and-merge import (update changed events); backup reminder if none in 30 days.

## Ideas (v0.14.0)
- Two-way sync with external ids (needs a DB column); choose Google account calendar to write into; add alarms to exported entries; import only a date range; detect Hebrew-calendar events and set HEBREW basis; periodic refresh of the dedicated calendar via WorkManager.

## Ideas (v0.15.0)
- Named saved views; group-by owner group once G1 lands; Hebrew month/year picker; a today-centred infinite week strip; quick filter by upcoming N days; swipe between day/week in the Upcoming list; export the current view (feeds P1 print).

## Ideas (v0.16.0)
- Reorder groups by drag; group photo; per-group card font and background (with P1); import a group template shared by someone else (S1); group birthdays widget; default group by event type; auto-suggest a group from relation text.

## Ideas (v0.17.0)
- Voice dictation of the date; fill title/gender from the same sentence ("דנה, בת 46"); English month names; relative dates; show a mini month calendar with the parsed day highlighted; learn the user's habitual format.

## Ideas (v0.18.0)
- First-run walkthrough with coach marks; per-field info buttons (date rules, tzeit method); animated GIF/illustrations in the guide; help search from the settings screen; share a guide section; link from the notification permission banner to the right topic.

## Ideas (v0.19.0)
- Bold/italic buttons for free text in the greeting editor; per-recipient nickname placeholder ({כינוי}) stored on the event; template import from a shared .sevents; template of the day suggestion; greeting language per recipient.

## Ideas (v0.20.0)
- WhatsApp Business Cloud API / SMTP behind a settings opt-in (owner decision); schedule a send time spread to avoid carrier limits; delivery receipts (sentIntent) to show failed numbers; per-recipient nickname stored on the person; skip recipients who already greeted via another channel.

## Ideas (v0.21.0)
- Person card as a picture (P1); show sender name and message in the receive dialog; QR code for a single event; share to a family group chat with a recurring update; link a received event to its sender for later updates.

## Ideas (v0.22.0)
- Year wall calendar (12 pages); weekly planner; address labels/envelopes from contacts; card with a hand-drawn signature; sticker pack (needs the owner's choice); save PDF straight to a chosen folder via PdfDocument; QR code on the card linking to a message.

## Ideas (v0.23.0)
- Encrypted database (SQLCipher); BiometricPrompt (strong) with a Keystore key; lock the widgets' details; quick-lock button in the toolbar; app-lock grace period after a notification tap; panic wipe option; audit log of backups/exports.

## Ideas (v0.24.0)
- Backup-age reminder notification; Google-Contacts CSV preset; encrypted auto-backup (password stored via Keystore); restore from the auto-backup folder in one tap.

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
