# Software Factory

This repository contains a Jetpack Compose Android Automotive demo module for the BMW Electric Android Cockpit parked Snake game.

## Modules

- `:snake-automotive` - Android Automotive application module for the parked Snake experience.

## Build

```bash
./gradlew :snake-automotive:assembleDebug
./gradlew :snake-automotive:testDebugUnitTest
```

The module is intentionally local-only. The in-app Vehicle State control is a demo/simulator control for validation and screenshots; it is not a production vehicle-signal integration.

## Scores and local leaderboard

- The Current score tile updates live while a round is playing; Best is the persisted running maximum.
- Every collision-ended round is recorded once with score, speed profile, completion time and a monotonic sequence id.
- History is stored locally in device-protected SharedPreferences (`bmwSnakeScoreHistory`, ranked, capped at 10). The leaderboard sorts by score descending and breaks ties by most recent completion, so the order is stable and deterministic. Malformed persisted rows are skipped and the top five rows are shown in the cockpit panel.
- See `docs/screenshots/score-leaderboard/README.md` for Automotive emulator evidence.

## Day/Dark theme

- The top-bar theme toggle switches the cockpit between Day and Dark immediately, in every round state including DrivingLocked; switching theme never starts, pauses, resumes or otherwise mutates a game round.
- On first launch (no stored preference) the app follows the Android system theme and keeps following system `uiMode` changes until the user toggles manually.
- A manual selection is persisted locally in device-protected SharedPreferences (`snake-local-theme` / `bmwSnakeThemePreference`, values `day`/`dark`) and restored on later launches; it keeps taking priority over system theme changes until app data is cleared. Missing or malformed values are ignored and fall back to the system theme, and a failed write still applies the chosen theme for the current session.
- See `docs/screenshots/theme-switching/README.md` for Automotive emulator evidence.
