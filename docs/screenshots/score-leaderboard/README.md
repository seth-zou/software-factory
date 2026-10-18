# Snake score & leaderboard — Automotive emulator evidence

Real `adb screencap` frames from the Android Automotive emulator. No HTML, mockups, or
design exports are used.

## Environment

| Item | Value |
| --- | --- |
| Host acceleration | None (`/dev/kvm` absent; `-accel off`, TCG software emulation) |
| AVD | `aaos33_x86_64` (created with `avdmanager`, not checked in) |
| System image | `system-images;android-33;android-automotive;x86_64` (revision 5) |
| Emulator | 37.2.12, `-gpu swiftshader_indirect -no-window -no-snapshot -wipe-data -memory 4096 -cores 4 -partition-size 4096 -audio none` |
| Display | 1920x1080 landscape, density 160 (HWC physical main display id read at runtime) |
| Application id / Activity | `com.bmwelectric.snake` / `.MainActivity` |
| APK | `snake-automotive-debug.apk` installed with `adb install -r -g` |

Boot/install gating followed the repository AGENTS.md: install only after
`adb get-state` reported `device`, `sys.boot_completed=1`, and the
`activity/window/package/settings/input` services were found. The main HWC display id was
discovered with `adb shell dumpsys SurfaceFlinger --display-id`
(`Display ... (HWC display 0)`, physical id `4619827259835644672` at capture time).

## Capture commands

```bash
adb shell am force-stop com.bmwelectric.snake

# Ready (fresh data)
adb shell pm clear com.bmwelectric.snake
adb shell am start --activity-clear-task --user 0 \
  -n com.bmwelectric.snake/.MainActivity
adb exec-out screencap -d 4619827259835644672 -p > ready.png

# Playing, frozen with a non-zero score (debug build only)
adb shell am start --activity-clear-task --user 0 \
  -n com.bmwelectric.snake/.MainActivity --es demo_state playing
adb exec-out screencap -d 4619827259835644672 -p > playing.png

# Driving locked (debug build only)
adb shell am start --activity-clear-task --user 0 \
  -n com.bmwelectric.snake/.MainActivity --es demo_state driving
adb exec-out screencap -d 4619827259835644672 -p > driving-locked.png
```

`game-over.png` and `playing-live.png` come from unassisted real play (taps on Start /
speed / Restart / Exit): the snake ran, ate energy, and collided. `playing-live.png` is a
mid-round frame captured ~2 s after Start; `game-over.png` is the final collision frame
after five real rounds.

`--es demo_state ...` is honored only when `ApplicationInfo.FLAG_DEBUGGABLE` is set;
release builds ignore the extra. The `playing` demo freezes live ticks and drives the real
state machine through six genuine eat-ticks (Comfort, +20 each) so the frozen frame shows
Current 120 instead of a zero frame; energy is deterministically placed one cell ahead of
the head only in this debug path. The leaderboard rows shown behind it are the real rounds
played earlier on the same install.

## Frames

| File | State | What it proves | Size | SHA-256 |
| --- | --- | --- | --- | --- |
| `ready.png` | Ready, fresh data | Empty leaderboard state, Best 000, Start enabled | 1920x1080, 119045 B | `0872415162c1559115bf57fb888addfac1000da6325e774434e1cf2a09ecbdb6` |
| `playing.png` | Playing (frozen demo) | Live score tile Current **120**, Best 120, five persisted history rows | 1920x1080, 125067 B | `0a44445496b3f083399da19c88c4b529b7650a75eecc010bf3b8bd51c2d762f4` |
| `playing-live.png` | Playing (real round) | Real ticking round mid-play, Current **020**, prior round already on board | 1920x1080, 108362 B | `36fb635f90ec5398783339fd5e6073c239b8d3e5fbb82ee197b665084db1e55d` |
| `game-over.png` | Game over | Collision overlay "Score 030. Rank 1 of 5. Best 030."; ranked board 030 Dynamic / three 020 Comfort ties ordered newest-first / 010 Eco; newest row outlined in BMW Blue | 1920x1080, 134074 B | `7c2a8970d6716a27799c1379783a1f77873fbb938852e4fb565bd79b9c636b3c` |
| `driving-locked.png` | DrivingLocked | Persistent amber LOCK banner, "Unavailable while driving" overlay, Driving selected; Start/Pause/Restart/Exit/speed/d-pad disabled | 1920x1080, 122098 B | `b1c210e6c92009e7c8c8ef7277410e4ad5a77c7693c90657a5d993eb2224143f` |

All five hashes and byte sizes differ, so the states are distinct frames rather than the
same capture reused. Two `playing.png` captures four seconds apart were byte-identical,
confirming ticks are frozen for the screenshot. Each frame was visually inspected; the
MainActivity window was confirmed `HAS_DRAWN`/resumed via `dumpsys window`/`dumpsys
activity` before capture.
