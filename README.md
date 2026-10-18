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
