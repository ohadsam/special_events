---
name: android-native-dev
description: How to develop a native Android app (Kotlin + Jetpack Compose + Material 3, optional Glance widgets/Room/WorkManager) in a cloud sandbox with NO Android SDK, where GitHub Actions is the only build/test gate — token-efficient batch workflow, CI feedback loop, conventions, project bootstrap templates (CI/release/UI-test workflows, ktlint/detekt, version catalog). Use when starting a new native Android app, starting/continuing a development batch ("start batch N", "continue the plan"), setting up CI, or touching Gradle/build config. Distilled from ohadsam/Tehilim_Daily and ohadsam/ExtendedFileManager.
---

# Native Android development (sandbox + CI-gated)

Reusable across apps. Per-app facts (package, features, layout) live in that repo's `CLAUDE.md` + `docs/PLAN.md` — this skill is only the *way of working*. Pair with the `release-checklist` skill at the end of every batch.

## 0. The environment (assume this unless `CLAUDE.md` says otherwise)
- Cloud sandbox has JDK + Gradle but **no Android SDK; dl.google.com / Maven Central / external APIs are blocked**. **No Gradle task runs locally** (not even ktlint/detekt — they need AGP).
- **GitHub Actions is the only build/test gate.** "Verified" = CI green on the pushed branch. Read Kotlin/Gradle by eye before pushing; keep batches small so a CI failure is cheap.
- Anything needing a device (notifications timing, widget visuals, permission flows, TalkBack) is *not verifiable here* → record it under "Not verifiable" in `AGENT.md`, don't claim it works.
- Only work in the repo(s) in scope. Other repos are read-only references; never push to them.

## 1. Token-efficient batch workflow
1. Read `CLAUDE.md` → `docs/PLAN.md` (find next unchecked batch) → **only that batch's section** of `docs/SPEC.md` (grep the anchor). Never scan the tree first; use `STRUCTURE.md` as the code map.
2. Spec ambiguous? Tighten SPEC.md first, then code. Decisions already recorded in SPEC/PLAN are not re-litigated.
3. Implement in small commits; unit tests *with* the code (JUnit4, coroutines-test, Turbine/MockK as needed; pure logic first).
4. Reuse: grep for an existing helper before writing one. Files small & single-purpose (< ~300 lines). Comment the non-obvious *why* only.
5. Self-review the diff, then run the **`release-checklist`** skill. Fix findings before moving on.
6. Update docs at batch end: PLAN status, `AGENT.md` (Recently done / Open items / Not verifiable), What's New, `STRUCTURE.md` if files were added.
7. CI green → merge → next batch. **Never start a batch on red CI.**

Token rules: no full-file reads when `grep` + offset/limit works; read logs by grepping, never whole; summaries go in docs, not chat; one batch at a time; don't pre-create empty packages; don't re-derive facts already in docs.

Model tier: routine work (UI tweaks, strings, wiring a known pattern, tests for designed behavior, docs) → default/fast tier. Escalate for architecture/DB schema, security surfaces (encryption, permissions, exported components), data-loss-adjacent changes, and release reviews.

## 2. CI feedback loop
- Push, then `gh run list --limit 3` / `gh run watch <id> --exit-status` (blocking, cheap). `gh` can't download logs (403) → use `mcp__github__get_job_logs` with a large `tail_lines`; if saved to a file, `grep` it for `e: file`, `FAILED`, `What went wrong`. Never read a whole log.
- Cheapest diagnosis: the CI failure-summary step also emits `::error::` annotations → `gh api repos/OWNER/REPO/check-runs/<job_id>/annotations -q '.[].message'` (job id from `gh run view <run> --json jobs -q '.jobs[0].databaseId'`). Raw `get_job_logs` costs 5–10k tokens of Gradle stack trace; use `tail_lines` ≤ 70 and only as a fallback.
- Compile errors surface in one pass: fix all, push once. KSP sometimes flakes (`Expected start of the object '{', but had 'EOF'`) → re-run, don't "fix".
- Manual workflows via `mcp__github__actions_run_trigger`: `release.yml` (R8 release APK/AAB), `ui-tests.yml` (emulator smoke tests; half-size JPEG screenshots printed in the log between `SHOTS_BEGIN/SHOTS_END` — decode the base64 chunks and view with Read), plus any live-API `*-probe.yml`.
- External APIs must be verified by a probe workflow, never assumed from docs/memory (sandbox can't reach them).
- Lint (ktlint/detekt) starts **advisory** (`ignoreFailures = true`) while the codebase settles; flip to enforcing once stable (note it in PLAN).

## 3. Stack defaults (override in the app's CLAUDE.md)
Kotlin, Compose + Material 3, single `:app` module, package-by-feature, **manual DI** (`AppContainer`) unless the app is big enough for Hilt (fewer blind-compile risks), Room, DataStore, WorkManager (+ AlarmManager for exact user-visible times), OkHttp + kotlinx.serialization (no Retrofit), Glance for widgets, Coil for images if needed. minSdk 26, targetSdk/compileSdk = latest stable. Versions pinned only in `gradle/libs.versions.toml`.

Package layout (grow by batch; don't pre-create empty packages):
`data/` (repos, `db/`, `prefs/`) · `domain/` (models, use cases, pure logic) · `ui/<feature>/` · `widget/` · `reminders/` · `log/` (AppLogger).

## 4. Conventions that matter on mobile
- UI strings only in `values/strings.xml` (+ `values-he/` or `values-en/` per app); no hardcoded text. Use `start/end`, never `left/right`; mirror directional icons; support font scale to 200%.
- Business logic never in composables; state hoisted; every screen has `@Preview` (light+dark) and empty/loading/error states.
- Time/date logic is pure Kotlin taking `Clock`/`LocalDate` params → unit-testable without Android.
- Touch targets ≥ 48dp, WCAG AA contrast in both themes, every icon has `contentDescription`.
- Primary action = single tap; long-press/overflow = secondary. Toolbars with > ~3–4 actions → grouped dropdown with subheadings, ordered by use frequency. One hamburger/primary navigation. Destructive actions confirmed with specific wording or undoable (snackbar Undo).
- Prefer system pickers/SAF/MediaStore and the narrowest permission; explain every permission in-app; degrade gracefully when denied.
- Log through an `AppLogger` (levels, tags, ring buffer + file); no personal data in exports without opt-in.
- Room: any schema change = `version` bump + `Migrations` entry in the same commit (release never wipes data). Debug may wipe.
- Background work in WorkManager/AlarmManager, never raw threads. Navigation/lifecycle calls after a `suspend` call run on `Dispatchers.Main`.
- Glance widgets: never capture data once in `provideGlance`; collect flows / read state inside `provideContent`.
- Room schema export + parallel Gradle: `kspDebugKotlin` and `kspReleaseKotlin` both write `app/schemas/.../N.json` and can read a half-written file (`JsonDecodingException ... database.entities[0]`). Add `tasks.matching { it.name == "kspReleaseKotlin" }.configureEach { mustRunAfter("kspDebugKotlin") }`.
- Never write `/*` inside a KDoc/comment (globs like `schemas/*.json`): Kotlin block comments nest and the file fails to parse.
- Sealed-class additions must be handled in every `when` — grep before pushing.

## 5. Versioning & signing
- `versionName` bumped per release (feature → minor, fix → patch). `versionCode` monotonic: e.g. `100 + git commit count` so every CI build is an in-place update. (CI checkout needs `fetch-depth: 0`.)
- Sideload key committed on purpose (`app/sideload.jks`) lets every CI build update in place; it is **not** a store key. For Play: `KEYSTORE_*` secrets in `release.yml` (users reinstall once). Never change/delete the sideload key after users installed it.

## 6. Bootstrapping a new app (what to copy)
`templates/` in this skill holds the proven files — copy, then adjust names:
- `editorconfig` → `.editorconfig` · `gitignore` → `.gitignore` · `gradle.properties` · `config/detekt.yml`
- `workflows/android-ci.yml` (ktlint, detekt, lintDebug, unit tests, androidTest compile, assembleDebug/Release; skips cleanly until `gradlew` exists) · `workflows/latest-apk.yml` (merge to main → release `latest` with a versioned APK) · `workflows/release.yml` (tag/manual → signed APK+AAB+mapping) · `workflows/ui-tests.yml` (emulator, screenshots)
- `libs.versions.toml` = known-good version baseline (AGP 8.9.2, Kotlin 2.1.20, Compose BOM 2025.05.01 …). Bump deliberately, then confirm via CI; newer is not automatically better when you can't compile locally.
- Gradle wrapper (8.14.x), `settings.gradle.kts` (google/mavenCentral, `FAIL_ON_PROJECT_REPOS`), root + `app/build.gradle.kts` (ktlint+detekt plugins, `ignoreFailures`, signing configs, R8 + shrinkResources on release) — take them from a sibling app (`ohadsam/Tehilim_Daily`) as the reference.
- Docs skeleton: `CLAUDE.md` (≤ ~60 lines: purpose, stack, layout, conventions, sandbox, token rules), `docs/PLAN.md` (batch table + status), `docs/SPEC.md`, `docs/CHANGELOG.md`, `AGENT.md` (agent memory: recently done / open items / not verifiable), `STRUCTURE.md` (code map).
- First batch = Foundation: Gradle project, version catalog, CI, theme, nav shell (RTL if needed), AppLogger skeleton, What's New + version display. Get CI green on an empty app *before* building features.
