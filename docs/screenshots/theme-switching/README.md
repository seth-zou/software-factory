# Day/Dark theme switching — Automotive emulator evidence

Real `adb screencap` frames from the Android Automotive emulator. No HTML, mockups, or
design exports are used.

## Environment

| Item | Value |
| --- | --- |
| Host acceleration | None (`/dev/kvm` absent; `-accel off`, TCG software emulation) |
| AVD | `aaos33_x86_64` (created with `avdmanager`, not checked in) |
| System image | `system-images;android-33;android-automotive;x86_64` (revision 5) |
| Emulator | 37.2.12, `-gpu swiftshader_indirect -no-window -no-snapshot -wipe-data -memory 4096 -cores 4 -partition-size 4096 -audio none` |
| Display | 1920x1080 landscape, density 160 (`hw.lcd.width/height/density` in the local, non-checked-in AVD `config.ini`; the image defaults to 320x640 without this), HWC physical main display id read at runtime |
| Application id / Activity | `com.bmwelectric.snake` / `.MainActivity` |
| APK | `snake-automotive-debug.apk` installed with `adb install -r -g` |

Boot/install gating followed the repository AGENTS.md: install happened only after
`adb get-state` reported `device`, `sys.boot_completed=1`, and the
`activity/window/package/settings/input` services were found. The main HWC display id was
discovered with `adb shell dumpsys SurfaceFlinger --display-id`
(`Display ... (HWC display 0)`, physical id `4619827259835644672` at capture time).
`MainActivity` was confirmed resumed/focused and its window `HAS_DRAWN` before each capture.

Note on the system theme: this Automotive image locks the system night mode
(`dumpsys uimode` shows `mNightModeLocked=true mUiModeLocked=true`; `cmd uimode night yes`
is rejected and the effective mode stays light), so the live "no preference -> follow
system" frames show the Day path. The dark-system branch (`UI_MODE_NIGHT_YES` -> Dark with
no stored preference) is covered by `ThemeModeTest` / `SnakeAutomotiveUiTest` on the JVM
and Compose semantics layers.

## Capture commands

```bash
adb shell pm clear com.bmwelectric.snake
adb shell am start --activity-clear-task --user 0 \
  -n com.bmwelectric.snake/.MainActivity
adb exec-out screencap -p > ready-day.png                 # no preference, system light -> Day

# Toggle button center taken from a fresh `uiautomator dump` bounds, then tap it:
adb shell input tap 1819 56
adb exec-out screencap -p > ready-dark.png                # manual Dark, persisted

adb shell am force-stop com.bmwelectric.snake
adb shell am start --activity-clear-task --user 0 \
  -n com.bmwelectric.snake/.MainActivity
# Restored Dark verified via the toggle label; the re-render was byte-identical to
# ready-dark.png (see notes below), so no duplicate image is committed.

adb shell am force-stop com.bmwelectric.snake
adb shell am start --activity-clear-task --user 0 \
  -n com.bmwelectric.snake/.MainActivity --es demo_state playing
adb exec-out screencap -p > playing-dark.png              # frozen demo inherits stored Dark
adb shell input tap 1819 56
adb exec-out screencap -p > playing-day.png               # theme flips mid-round, score stays 120

adb shell am force-stop com.bmwelectric.snake
adb shell am start --activity-clear-task --user 0 \
  -n com.bmwelectric.snake/.MainActivity --es demo_state driving
adb exec-out screencap -p > driving-locked-day.png        # frozen demo inherits stored Day
adb shell input tap 1819 56
adb exec-out screencap -p > driving-locked-dark.png       # theme flips while DrivingLocked
```

`--es demo_state ...` is honored only when `ApplicationInfo.FLAG_DEBUGGABLE` is set;
release builds ignore the extra. The `playing` demo freezes live ticks and drives the real
state machine through six genuine eat-ticks (Comfort, +20 each) so the frozen frame shows
Current/Best 120. The toggle label names the *target* theme ("Day mode" while Dark and
"Dark mode" while Day); the current theme in every row below was confirmed from that label
in a `uiautomator dump`, not inferred from the image.

## Frames

| File | State | What it proves | Size | SHA-256 |
| --- | --- | --- | --- | --- |
| `ready-day.png` | Ready, fresh data (`pm clear`) | First launch with no stored preference follows the light system theme; toggle offers "Dark mode"; empty leaderboard, Best 000 | 1920x1080, 120330 B | `b526a78c3bcb9e9f6e3a5eb19cadfd587ba686fc7fe832cb56afe9c3b9905460` |
| `ready-dark.png` | Ready after one tap | Manual switch to Dark applies immediately; toggle offers "Day mode"; Ready overlay and score unchanged | 1920x1080, 119045 B | `0872415162c1559115bf57fb888addfac1000da6325e774434e1cf2a09ecbdb6` |
| `playing-dark.png` | Playing (frozen demo, restored pref) | After `force-stop` relaunch the persisted Dark preference is restored; frozen score Current/Best **120** | 1920x1080, 108381 B | `6d1d3d9b87f3962fd64054a9b9ea72faa9b1262cb5daf64c7ce81462c7014708` |
| `playing-day.png` | Playing, after toggle during the round | Day palette applied without leaving Playing; Current/Best stay **120**, round, speed and board untouched | 1920x1080, 109328 B | `a700e8d66acf4f37b64842bf26d0f97938ffc1ee3172e89fe6a5c26765208ad8` |
| `driving-locked-day.png` | DrivingLocked | Day palette with persistent amber LOCK banner and "Unavailable while driving" overlay; Driving selected; all game controls disabled | 1920x1080, 124127 B | `79dd5cb9074244b328c4042727056db5151081c4584ebe3b0ab31a41c7adcaa9` |
| `driving-locked-dark.png` | DrivingLocked, after toggle | Theme switches while locked; banner/overlay remain, Start/Pause/Restart/Exit/speed/d-pad stay disabled; lock is not bypassed | 1920x1080, 122276 B | `500933c7ab3bddbb6880ee92b8e669cb781c29f780d15e22d535bd548dcf5773` |

All six hashes and byte sizes are pairwise different, so the Day/Dark pairs are distinct
frames. Two `playing-dark.png` captures four seconds apart were byte-identical
(`6d1d3d9b…`), confirming ticks stay frozen for the screenshot; after the in-round toggle
the hierarchy still reported `Snake board Playing, score 120, best 120`. A plain
`force-stop` relaunch with the persisted Dark preference re-rendered Ready to a frame
byte-identical with `ready-dark.png`, and the hierarchy showed the "Day mode" label,
demonstrating preference restore without a duplicate image. Each frame was visually
inspected.

## Instrumented test caveat

`:snake-automotive:connectedDebugAndroidTest` does not run on this image: all seven Compose
UI tests (including the two pre-existing ones) fail before test logic with

```
java.lang.RuntimeException: Unable to resolve activity for: Intent { act=android.intent.action.MAIN
  cat=[android.intent.category.LAUNCHER] cmp=com.bmwelectric.snake.test/androidx.activity.ComponentActivity }
    at androidx.test.core.app.InstrumentationActivityInvoker.startActivity(...)
```

The harness activity is correctly declared exported in the merged androidTest manifest; the
image's PackageManager cannot resolve activities hosted by the `com.bmwelectric.snake.test`
package — the AndroidX test host/package resolution instability documented in the
repository AGENTS.md for no-KVM Automotive images. The androidTest APK compiles
(`assembleDebugAndroidTest` passes), the same assertions are exercised here by manual
launch + `uiautomator` evidence on real emulator frames, and theme resolution/persistence
logic is additionally covered by JVM unit tests (`ThemeModeTest`, 5 cases) that pass via
`testDebugUnitTest`.
