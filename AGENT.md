# Special Events – Agent Memory
**Last updated:** project bootstrap (no app code yet)
Read order: CLAUDE.md → docs/PLAN.md → STRUCTURE.md → the batch's section in docs/SPEC.md.

## Recently done
- Bootstrap: dev setup (CI workflows, ktlint/detekt config, editorconfig) and reusable skills `android-native-dev` + `release-checklist` copied from the Tehilim_Daily / ExtendedFileManager practice.

## Known issues / open items (need the owner)
- Feature spec for "אירועים מיוחדים" not yet provided → fill `docs/SPEC.md`, then define batches in `docs/PLAN.md`.
- Gradle project (wrapper, settings, build files) is created in batch 0; until `gradlew` exists the CI job skips build steps.
- Package name / app id `com.ohadsam.specialevents` is tentative.

## Not verifiable in the sandbox (needs a device)
Notification timing, permission flows, visuals, TalkBack.
