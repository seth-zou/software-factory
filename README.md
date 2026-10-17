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
