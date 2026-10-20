# AGENTS.md

## Scope and Priority

This file applies to the whole repository. If a more specific `AGENTS.md` is later added in a subdirectory, that nested file takes precedence for files below it. Follow system, workflow, and user instructions before this file.

## Project Shape

This repository is a small Gradle Android project for a BMW Electric Android Cockpit parked Snake game.

- Root Gradle project: `settings.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml`, Gradle wrapper.
- App module: `:snake-automotive`.
- Package/application id: `com.bmwelectric.snake`.
- Main UI: `snake-automotive/src/main/java/com/bmwelectric/snake/MainActivity.kt`.
- Game state machine: `snake-automotive/src/main/java/com/bmwelectric/snake/game/SnakeGameController.kt`.
- Score history model, ranking, and text persistence codec: `snake-automotive/src/main/java/com/bmwelectric/snake/game/ScoreHistory.kt`.
- Day/Dark theme mode parsing and resolution: `snake-automotive/src/main/java/com/bmwelectric/snake/theme/ThemeMode.kt`.
- JVM tests: `snake-automotive/src/test/java/com/bmwelectric/snake/game/SnakeGameControllerTest.kt`.
- Score history and theme JVM tests: `snake-automotive/src/test/java/com/bmwelectric/snake/game/ScoreHistoryTest.kt`, `snake-automotive/src/test/java/com/bmwelectric/snake/theme/ThemeModeTest.kt`.
- Compose instrumentation tests: `snake-automotive/src/androidTest/java/com/bmwelectric/snake/SnakeAutomotiveUiTest.kt`.
- Emulator screenshot evidence: `docs/screenshots/score-leaderboard/` and `docs/screenshots/theme-switching/`.
- No CI workflow is currently checked in; use the local Gradle wrapper commands below as the source of truth.

## First Read Path

Before editing, read these files in order:

1. `README.md`
2. `settings.gradle.kts`
3. `gradle/libs.versions.toml`
4. `snake-automotive/build.gradle.kts`
5. `snake-automotive/src/main/AndroidManifest.xml`
6. `snake-automotive/src/main/java/com/bmwelectric/snake/game/SnakeGameController.kt`
7. `snake-automotive/src/main/java/com/bmwelectric/snake/game/ScoreHistory.kt`
8. `snake-automotive/src/main/java/com/bmwelectric/snake/theme/ThemeMode.kt`
9. `snake-automotive/src/main/java/com/bmwelectric/snake/MainActivity.kt`
10. Relevant unit/UI tests under `snake-automotive/src/test` and `snake-automotive/src/androidTest`
11. Screenshot evidence READMEs under `docs/screenshots/` when touching UI, leaderboard, theme, or emulator validation guidance

For UI/design changes, also read the workspace design system at `knowledge/design-system/bmw-electric-android-cockpit/DESIGN.md` when it is available.

## Toolchain

- Use JDK 17.
- Use the checked-in Gradle wrapper: `./gradlew`.
- Use the Android SDK configured by `ANDROID_HOME` and `ANDROID_SDK_ROOT`; do not hard-code machine-local SDK paths in committed files.
- Keep dependency and plugin versions in `gradle/libs.versions.toml`. Do not upgrade dependencies unless the task explicitly requires it.
- The project uses Kotlin official style, AndroidX, non-transitive R classes, AGP 8.7.3, Kotlin 2.0.21, and Compose BOM 2024.12.01.

## Standard Commands

Run the smallest command set that covers the change. For broad changes, use:

```bash
ANDROID_HOME=<android-sdk> ANDROID_SDK_ROOT=<android-sdk> \
  ./gradlew :snake-automotive:assembleDebug \
            :snake-automotive:testDebugUnitTest \
            :snake-automotive:lintDebug \
            :snake-automotive:assembleDebugAndroidTest
```

Useful focused commands:

```bash
./gradlew :snake-automotive:assembleDebug
./gradlew :snake-automotive:testDebugUnitTest
./gradlew :snake-automotive:lintDebug
./gradlew :snake-automotive:assembleDebugAndroidTest
./gradlew :snake-automotive:connectedDebugAndroidTest
```

`connectedDebugAndroidTest` is valuable when the Automotive emulator is healthy, but it can fail on no-KVM or partially booted AVDs because of AndroidX test host/package resolution or PackageManager instability. Record the exact failure and caveat honestly; do not report it as passed unless it actually passes.

## Kotlin and Compose Rules

- Keep business rules in `SnakeGameController` when possible; keep Compose UI as a view/controller layer.
- Keep score history ranking, encoding, decoding, and bounds in `ScoreHistory.kt`; do not duplicate leaderboard ordering in Compose.
- Keep theme storage parsing and system/manual resolution in `ThemeMode.kt`; Compose should consume the resolved mode.
- Preserve deterministic state-machine behavior and focused tests for safety rules, history ranking, and theme fallback.
- Use Compose semantics, `contentDescription`, visible focus, disabled states, stable dimensions, and keyboard/touch support.
- Maintain landscape-first cockpit layout. Do not introduce marketing pages, decorative animation, or dense driving-mode text.
- Keep text and controls within stable containers at automotive display sizes.
- Prefer existing local palette, spacing, and component patterns over new abstractions.

## BMW Cockpit Design Requirements

The UI must stay aligned with the BMW Electric Android Cockpit design system:

- Use the design tokens represented in `CockpitPalette` for Carbon Black, Graphite Panel, Alpine White, BMW Blue, Electric Green, Energy Cyan, and Alert Amber roles.
- BMW Blue is for focus, selected state, and primary interaction; do not use it as decoration.
- Driving/cockpit controls should target the preferred `72.dp` touch size where practical and never regress below the design system minimum touch target.
- Maintain dark and day mode support, legible contrast, stable score/value areas, and reduced-motion-friendly behavior.
- Safety and locked states must use persistent, glanceable banners rather than transient messages.
- The theme toggle label names the target theme (`Day mode` while Dark is active, `Dark mode` while Day is active); preserve this behavior unless tests and screenshot evidence are deliberately updated.

## Persistence and Local Data

The app intentionally stores only local, device-protected preferences so it can run under Android Automotive locked-user conditions:

- Best score: `snake-local-score` preferences, key `bmwSnakeBestScore`.
- Score history: `snake-local-score` preferences, key `bmwSnakeScoreHistory`, newline rows encoded as `score|SpeedProfile|completedAtMillis|sequence`.
- Manual theme: `snake-local-theme` preferences, key `bmwSnakeThemePreference`, values `day` or `dark`.
- Malformed score history rows must be skipped without crashing; malformed or missing theme values must fall back to following the Android system theme.
- Score history is ranked by score descending, then completed time descending, then sequence descending; it is capped at 10 persisted rows, with five rows visible in the cockpit panel.
- Failed preference reads/writes should surface a storage warning where applicable but must not block current-session play or theme switching.

## Safety and State Machine Invariants

Driving safety is a hard gate:

- Switching to `VehicleState.Driving` must immediately set `RoundState.DrivingLocked`.
- While driving, start, pause/continue, restart, exit, speed, and direction actions must not resume or mutate active play.
- Returning to `VehicleState.Parked` from driving must reset to Ready and must not auto-resume a previous game.
- Reverse direction input must be ignored; rapid legal input must remain stable.
- Pause must freeze ticks; continue must resume ticks.
- Collisions and best-score persistence must remain covered by JVM tests.
- Collision-ended rounds, including zero-score collisions, must be recorded exactly once. Exiting mid-round must not record history.
- Repeated equal-score rounds must rank newest first and keep a monotonic sequence id.
- Theme toggling must not start, pause, resume, restart, exit, or otherwise mutate a round, including while `DrivingLocked`.

When changing these behaviors, update the focused tests in the same commit:

- State machine and driving safety: `SnakeGameControllerTest`.
- Leaderboard ranking, codecs, and malformed persistence handling: `ScoreHistoryTest`.
- Theme parsing and system/manual resolution: `ThemeModeTest`.
- Compose semantics, locked controls, and theme UI behavior: `SnakeAutomotiveUiTest`.

## Automotive Runtime and Screenshots

Use an Android Automotive API 33 x86_64 AVD for the most stable local validation path. API 34/35 images and partially booted API 33 images have shown PackageManager, `system_server`, `CarService`, and locked-user instability.

On hosts without KVM, prefer software rendering and no audio:

```bash
ANDROID_HOME=<android-sdk> ANDROID_SDK_ROOT=<android-sdk> ANDROID_AVD_HOME=<avd-home> \
  <android-sdk>/emulator/emulator \
  -avd <api33-automotive-x86_64-avd> \
  -wipe-data -no-snapshot -no-window -no-boot-anim -no-metrics \
  -gpu swiftshader_indirect -accel off -memory 4096 -cores 4 \
  -partition-size 4096 -audio none
```

Before installing:

```bash
adb get-state
adb shell service check activity
adb shell service check window
adb shell service check package
adb shell service check settings
adb shell service check input
adb shell getprop sys.boot_completed
```

Install only after `adb get-state` is `device` and core services are available; preferably wait for `sys.boot_completed=1`. Installing too early can trigger `PackageManagerInternal.freeStorage` NPE, `Too early to start activity`, `RUNNING_LOCKED`, or package/session hangs.

The app and `MainActivity` are `directBootAware=true`. Best-score storage uses `createDeviceProtectedStorageContext()` so the app can run when Automotive user state is locked.

For screenshot validation:

- Use `adb shell dumpsys SurfaceFlinger --display-id` to find the physical main HWC display at runtime; do not hard-code a display id.
- Start fresh per state with `adb shell am force-stop com.bmwelectric.snake` before launching.
- Capture with `adb exec-out screencap -d <physical-main-display-id> -p > <name>.png`.
- Confirm window manager/activity evidence shows `com.bmwelectric.snake/.MainActivity` focused or resumed, and the window has drawn.
- Reject splash screens, black frames, wrong secondary displays, and repeated identical frames.
- Visually inspect each screenshot and compare file sizes/hashes so Ready, Playing, and Driving Locked are distinct.

The app supports debuggable-only screenshot helpers:

```bash
adb shell am start --display <launch-display-id> --activity-clear-task --user 0 \
  -n com.bmwelectric.snake/.MainActivity --es demo_state playing
adb shell am start --display <launch-display-id> --activity-clear-task --user 0 \
  -n com.bmwelectric.snake/.MainActivity --es demo_state driving
```

`demo_state=playing` freezes ticks to keep Playing stable for screenshots. These extras must remain guarded by `ApplicationInfo.FLAG_DEBUGGABLE`; release/non-debuggable builds must ignore them.

Committed screenshot artifacts are allowed only when the task explicitly asks for visual evidence or UI validation. Store durable evidence under the existing `docs/screenshots/<feature>/` folders with a README that records emulator image, launch/capture commands, display-id discovery, file sizes, SHA-256 hashes, visual inspection notes, and any connected-test caveat. Do not commit transient emulator logs, APKs, Gradle reports, or screenshots outside those evidence folders.

## Git Hygiene

- Keep commits focused. Do not modify business implementation when the task is documentation-only.
- Never commit `build/`, `.gradle/`, screenshots, emulator output, credentials, local SDK paths, keystores, or generated reports.
- Stage explicit paths; do not use `git add -A`.
- Before commit, check `git status --short`, `git diff --cached --stat`, staged file sizes, and staged content for obvious sensitive values.
- Commit on the current feature branch, push normally, then fetch/read back the remote branch SHA.
- Never force-push, rewrite shared history, or skip hooks unless the user explicitly authorizes it.

## Definition of Done

A change is done only when:

- The implementation matches the requested scope and this repository's current structure.
- Required tests/build checks for the touched area have run, or failures are documented with exact commands and causes.
- Automotive screenshots, when required, are real emulator `adb screencap` outputs and have been visually verified.
- Any connected-test caveat is explicit and not disguised as a pass.
- The diff contains only intended files.
- The commit is pushed to the current branch and the remote SHA is read back.
