---
name: release-checklist
description: Closing ritual to run after every batch, phase or fix in ANY native Android app repo, before calling it done — 4 persona code reviews (system architect, UI expert, UX expert, QA architect) with explicit security / appearance / performance / usability gates, version bump + What's New + changelog, docs, localization/RTL, in-app help + info buttons, permissions/security review, tests with CI-green gate, skills self-review, merge. Invoke by name ("run the release checklist") or whenever a change is being finished, shipped or wrapped up.
---

# Release checklist (reusable, native Android)

Generic version of the checklists proven in `ohadsam/Tehilim_Daily` and `ohadsam/ExtendedFileManager`. Read the app's `CLAUDE.md` first: its stack, package layout and conventions are the yardstick for the review. If the repo has app-specific gates, they live in its `CLAUDE.md` or in a "Project gates" section appended at the bottom of this file (step 7 keeps it current).

Run after every batch of `docs/PLAN.md` or standalone fix. **Do not skip or reorder** — later steps assume earlier ones (version bump needs review fixes applied; merge needs CI green). If a step finds nothing, say so explicitly and move on; never invent war stories — add only real incidents (step 7).

## 1. Code review — 4 separate persona passes, in order
Not one merged skim. Re-read every changed file in each pass; fix every finding before moving on.

1. **System architect.** Package layout from `CLAUDE.md` respected (data/domain/ui separation)? Duplicated logic that an existing helper covers (promote it and reuse)? Files/functions single-purpose (real split, not a threshold dodge)? New dependency justified and pinned in `gradle/libs.versions.toml` (no inline versions)? Background work in WorkManager/AlarmManager, not raw threads? Pure logic free of Android deps and clock-injected? Anything that quietly widens permissions? Non-obvious *why* commented; obvious code not?
2. **UI expert.** State hoisted, no business logic in composables, `@Preview` light+dark for non-trivial screens, Material 3 used consistently with sibling screens, spacing/typography tokens, consistent icons, dark + light + dynamic color (Android 12+), 200% font scale with no clipped text, widgets fine at min/max size. **Appearance gate:** does it look polished next to the existing screens?
3. **UX expert.** Discoverable and forgiving? Tap = primary action; long-press/overflow = secondary only. Destructive actions confirmed with wording that says what will happen, or undoable (snackbar Undo). Empty / loading / error states with copy and a retry. Touch targets ≥ 48dp, key actions reachable one-handed. Toolbars > ~3–4 actions grouped into dropdowns with subheadings. **Usability gate:** can a first-time user complete the app's core task in ≤ 2–3 taps?
4. **QA architect.** Unit tests for new logic, written *with* the code (not deferred). Edge cases: offline, first launch, empty data, permission denied, boundaries (first/last item, month/leap), midnight rollover, timezone change, reboot with pending alarms, process death, huge font, external mutation mid-operation. **Performance gate:** nothing heavy on main thread, lists lazy, no needless recomposition, cheap widget updates, startup not regressed. **Security gate:** see step 5.

## 2. Version + What's New
- Bump `versionName` (features → minor, fixes → patch); `versionCode` every release (or derived from commit count — check the app's convention).
- In-app What's New (e.g. `assets/whatsnew.json`): only this version's user-facing bullets, in the app's language; the in-app version label shows the new number.
- Append a short, user-facing entry to `docs/CHANGELOG.md` (no implementation detail).

- **Update-in-place gate:** same `applicationId` + signing certificate, `versionCode` higher, every schema/preferences change migrated and migration-tested. A release that would force an uninstall is a blocker, not a note.

## 3. Docs
`docs/PLAN.md` status (adjust later batches if assumptions changed) · `docs/SPEC.md` if behavior diverged (fix the spec, not just the code) · `CLAUDE.md` if a convention/pattern changed · `AGENT.md` (Recently done / Open items / Not verifiable) · `STRUCTURE.md` file map if files were added · `README.md` if user-facing.

## 4. Localization & RTL
New strings in `values/strings.xml` (and every other shipped locale, e.g. `values-he/`); no hardcoded text. Check RTL for each new screen/widget/notification: `start/end` not `left/right`, mirrored directional icons, bidi-safe Latin/digits, locale-correct dates/numbers. If nothing changed, say so.

## 5. User guide, info buttons, security & permissions
- **Help/info:** if behavior changed, update in-app help (e.g. `assets/help.html` — verify every TOC `href="#x"` has a matching `id="x"`) and every ⓘ InfoButton's copy; stale help is worse than none. Add a new InfoButton for a genuinely non-obvious new flow. No user-visible change → say so.
- **Permissions:** each new permission justified in-app and in a manifest comment; prefer SAF/MediaStore/system pickers over broad access; exact-alarm / notification / location flows degrade gracefully when denied.
- **Data & security:** personal data stays local; nothing sensitive in logs/exports without opt-in; sensitive stored data in Keystore-backed/encrypted storage unless plaintext is genuinely fine; no cleartext traffic; exported components and deep links validate input; PendingIntents immutable; new dependencies scanned (confirm the CI scan job ran clean).

## 6. Tests & CI
Add/extend unit tests alongside the code. The sandbox cannot run Gradle: "tests pass" means the Android CI workflow (ktlint, detekt, lint, unit tests, assemble) is **green on the pushed branch** — push and read CI; never claim success from eyeballing. State which parts still need instrumented/emulator or on-device coverage.

## 7. Skills & this checklist
Do `.claude/skills/*` need updating? **Then ask explicitly whether this checklist itself needs a new/changed step** (a real gotcha, a recurring miss). Add concrete entries only for real incidents; otherwise say "no change".

Real incidents carried over from earlier apps (keep concrete; add new ones below):
- Room schema change needs `version` bump + `Migrations` entry in the SAME commit (release builds never wipe data).
- Navigation/lifecycle calls after a `suspend` repository call must run on `Dispatchers.Main` — under Compose test dispatchers the continuation resumes off-main and crashes.
- External-API assumptions must be checked by a live probe workflow, not by reading docs/memory.
- Domain data (schedules, halachic/legal/business rules) must be confirmed by the owner, not inferred from another repo or an unverified web summary.
- Glance widgets: data captured once in `provideGlance` goes stale after `update()` — collect flows / read state inside `provideContent`.
- Sealed-class additions must be handled in every `when` (planner, codec, list text, editor) — grep before pushing.
- Kotlin block comments NEST: a glob like `dir/*.json` inside a KDoc opens a second comment and breaks the whole file (ktlint reports "Unclosed comment"). Never write `/*` in comment text.
- A release that forces users to reinstall (changed applicationId/signing key/versionCode not increasing) loses their data - the `latest-apk.yml` guard compares against the published APK.
- GitHub Actions can refuse to start ("payments failed / spending limit"): that is not a code failure - stop, do not merge unverified, tell the owner.
- strings.xml: an unescaped apostrophe (`'`) breaks resource compilation with a cryptic "Failed to compile values resource" - use the typographic geresh or `\'`.
- Lint `ByteOrderMark`: never put a literal U+FEFF in source; write `"\uFEFF"`. When lint fails, the console shows only a count - CI prints the findings from `lint-results-debug.txt` in the failure summary.
- A first-run dialog covers the UI under test: emulator tests must dismiss it first (`AppRobot.dismissWelcome`).
- An emulator workflow that ends its test command with `|| echo ...` reports green when tests fail. Let the command fail, upload the results, and print the failing tests' messages as the LAST step so they are in the log tail.
- When a UI test fails to find a node, make the helper dump the semantics tree (all roots) into the failure message - one CI round then tells you what was on screen.
- A lambda parameter named like an outer value silently shadows it (`cell` row height vs grid `cell`): name the outer one for what it is.
- Collapsing/accordion screens hide rows from UI tests: the test must open the section first.
- KSP occasionally flakes (`Expected start of the object '{', but had 'EOF'`) — re-run CI, don't "fix".

## 8. Merge
Feature branch → push → PR → CI green → merge to `main`. Resolve conflicts for real; never force-push over others' work. Don't start the next batch on red CI.

## Project gates (app-specific; append here)
_Special Events:_
- `docs/SECURITY.md` (threat model, permission list) is updated whenever a permission, exported component, file format or share path changes.
- Every screen change updates its `HelpTopic` text in `strings.xml` (`help_<topic>_body`) - stale help is a defect.
- Schema changes: a `Migration` constant matched to `app/schemas/<n>.json` by a `MigrationSchemaVnTest`, verified by hand against the CI-committed JSON on the first push.
- Anything that sends messages (SMS, share) is opt-in, capped, and shows the count/cost before sending.
- Imported or shared files are untrusted: clamp, skip bad entries, force dangerous settings off, preview before writing.

## Done means
All four passes ran and findings fixed (incl. security, appearance, performance, usability gates); version + What's New + changelog updated; docs and PLAN status updated; localization/RTL and help/info checked; permissions/security reviewed; tests added and CI green; skills + this checklist self-reviewed; merged (or PR ready).
