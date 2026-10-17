# Special Events (אירועים מיוחדים)

אפליקציית אנדרואיד נייטיב של "אירועים מיוחדים". **המפרט עדיין לא נכתב** — ממתינים לפירוט מהבעלים; עד אז אין לנחש פיצ'רים.
**קרא קודם `docs/PLAN.md` (באצ'ים וסטטוס) ו-`docs/SPEC.md` (מפרט). אל תסרוק את הקוד לפני כן.**

## Stack (defaults from `android-native-dev` skill; change here when decided)
Kotlin, Jetpack Compose (Material 3), manual DI (`AppContainer`), Room, DataStore, WorkManager + AlarmManager, single `:app` module,
package-by-feature. minSdk 26, targetSdk = latest stable. Package (tentative): `com.ohadsam.specialevents`. Versions only in `gradle/libs.versions.toml`.

## Package layout (grows by batch — do not pre-create empty packages)
`data/` (repos, `db/`, `prefs/`) · `domain/` · `ui/<feature>/` · `log/` (AppLogger). Add `widget/`, `reminders/` only if the spec needs them.

## Conventions
- UI strings only in `values/strings.xml` (Hebrew default; English optional). RTL: `start`/`end`, never `left`/`right`; font scale to 200%.
- Business logic never in composables; state hoisted; `@Preview` light+dark; empty/loading/error states on every screen.
- Date/time logic is pure Kotlin taking `Clock`/`LocalDate` params (unit-testable). Reuse before duplicating; small single-purpose files; comment only the non-obvious *why*.
- Touch targets ≥ 48dp, WCAG AA both themes, `contentDescription` on icons. Tap = primary action; long-press/overflow = secondary.
- Log via `AppLogger`; no personal data in exports without opt-in.

## Sandbox constraints
Cloud sandbox: JDK + Gradle, **no Android SDK, Google/Maven hosts blocked** → no Gradle task runs locally.
**GitHub Actions (`.github/workflows/android-ci.yml`) is the only build/test gate**: push, then read CI. Read Kotlin by eye before pushing; small batches.
Other repos (e.g. `Tehilim_Daily`, `ExtendedFileManager`) are read-only references — never push to them.

## Skills (reusable, copy `.claude/skills/*` to other app repos)
- `android-native-dev` — batch workflow, CI feedback loop, conventions, bootstrap templates. **Follow it for token efficiency.**
- `release-checklist` — run after every batch (4 persona reviews; security/appearance/performance/usability gates; version, What's New, docs, CI green, merge).

## Release process
**Autonomy (owner, 2026-10-17):** after a batch is done, CI is green and `release-checklist` ran, merge to `main` and continue to the next batch automatically.
Merge = fast-forward `git push origin <dev-branch>:main` (owner-authorized; no PR needed). Merging to `main` publishes the release `latest` APK.
Branch → push → CI green → `release-checklist` → merge. Bump `versionName` and What's New each user-visible batch.
