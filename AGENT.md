# Special Events – Agent Memory
**Last updated:** v0.1.0 (batch 0 done; batch 1 next)
Read order: CLAUDE.md → docs/PLAN.md → STRUCTURE.md → the batch's section in docs/SPEC.md.

## Recently done
- Bootstrap: dev setup (CI workflows, ktlint/detekt config, editorconfig) and reusable skills `android-native-dev` + `release-checklist` copied from the Tehilim_Daily / ExtendedFileManager practice.

- v0.1.0 (batch 0): app shell + CI green. Unverified on a device: visuals/RTL of the shell, launcher icon look, edge-to-edge insets.
- Search (SPEC S17, batch 4b) added after the owner's request: text + date ranges + filters + refine-in-results + Hebrew natural-language queries.

## Known issues / open items (need the owner)
- Feature spec for "אירועים מיוחדים" not yet provided → fill `docs/SPEC.md`, then define batches in `docs/PLAN.md`.
- **Signing key:** `app/sideload.jks` (password in app/build.gradle.kts) is committed on purpose so every CI build updates in place; NOT a store key. Never change it after users installed the app.
- Lint (ktlint/detekt) is advisory (`ignoreFailures`); flip to enforcing once stable.
- Package name / app id `com.ohadsam.specialevents` is tentative.

## Not verifiable in the sandbox (needs a device)
Notification timing, permission flows, visuals, TalkBack.

## Ideas (from batch 0)
- Dynamic-color / palette picker in Settings (theme already supports `dynamicColor`).
- Show "what's new" automatically once after an update.
- English UI: add `values-en`, then make layout direction follow the locale.
