# Special Events – Agent Memory
**Last updated:** v0.2.0 (batches 0–1 done; batch 2 next)
Read order: CLAUDE.md → docs/PLAN.md → STRUCTURE.md → the batch's section in docs/SPEC.md.

## Recently done
- Bootstrap: dev setup (CI workflows, ktlint/detekt config, editorconfig) and reusable skills `android-native-dev` + `release-checklist` copied from the Tehilim_Daily / ExtendedFileManager practice.

- v0.1.0 (batch 0): app shell + CI green. Unverified on a device: visuals/RTL of the shell, launcher icon look, edge-to-edge insets.
- Search (SPEC S17, batch 4b) added after the owner's request: text + date ranges + filters + refine-in-results + Hebrew natural-language queries.

- v0.2.0 (batch 1): calendar core. Parsha/festival/Hebrew-date logic is verified against pyluach goldens (2000–2100). Modern Israeli days (Yom HaShoah/Zikaron/Atzmaut/Yerushalayim), Omer, special Shabbatot (Shekalim/Zachor/…), Purim Meshulash, zmanim goldens are NOT done.

## Known issues / open items (need the owner)
- Feature spec for "אירועים מיוחדים" not yet provided → fill `docs/SPEC.md`, then define batches in `docs/PLAN.md`.
- **Signing key:** `app/sideload.jks` (password in app/build.gradle.kts) is committed on purpose so every CI build updates in place; NOT a store key. Never change it after users installed the app.
- Lint (ktlint/detekt) is advisory (`ignoreFailures`); flip to enforcing once stable.
- Package name / app id `com.ohadsam.specialevents` is tentative.

## Not verifiable in the sandbox (needs a device)
Notification timing, permission flows, visuals, TalkBack.

## Owner decisions needed (defaults are active until confirmed — SPEC Appendix A.1)
- Adar in a leap year (born/yahrzeit in plain Adar): default Adar II. 30 Cheshvan/Kislev in a short month: default 29th (day before). 29 Feb in a common year: default 28 Feb.

## Ideas (from batch 1)
- Show "Shabbat Mevarchim", special Shabbatot and Omer day in the calendar overlay; modern Israeli holidays with their postponement rules (needs owner confirmation).
- "Birthday falls on Shabbat/Yom Tov" and "bar mitzvah Shabbat" badges in the upcoming list (data is ready).
- Hebrew-letter year/day formatting helper for cards (גרשיים) — `HebrewDate.format()` exists.

## Ideas (from batch 0)
- Dynamic-color / palette picker in Settings (theme already supports `dynamicColor`).
- Show "what's new" automatically once after an update.
- English UI: add `values-en`, then make layout direction follow the locale.
