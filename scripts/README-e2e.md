# sim-e2e — iOS Simulator E2E helper

Lightweight CLI driver for Animally on the iOS Simulator. Pure bash + `xcrun` + `osascript`. No external deps (no idb).

## Usage

```bash
scripts/sim-e2e.sh <command> [args]
```

| Command | What it does |
|---|---|
| `boot` | Boot `$SIM_UDID`, open Simulator.app |
| `install` | Install built `.app` onto booted sim |
| `launch` | Launch app bundle |
| `terminate` | Kill app bundle |
| `setup-window` | Pin Simulator window to fixed origin — **run before any taps** |
| `tap <x> <y>` | Click at DEVICE point coordinates |
| `screenshot [name]` | Save screenshot to `/tmp/e2e/<name>.png` |
| `build` | Build iosApp scheme (Debug, iOS Simulator) |

Typical flow:

```bash
scripts/sim-e2e.sh boot
scripts/sim-e2e.sh build
scripts/sim-e2e.sh install
scripts/sim-e2e.sh setup-window
scripts/sim-e2e.sh launch
scripts/sim-e2e.sh tap 200 400
scripts/sim-e2e.sh shot-after-tap   # screenshot name is free-form
```

## Coordinate math (why setup-window matters)

Simulator clicks go through AppleScript global screen coordinates. If the
window floats anywhere, device→screen mapping drifts and recorded taps hit
the wrong UI element ("stale coordinate" bug class).

Fix: `setup-window` pins the window origin to a fixed point (`40,40`).
Conversion used by `tap`:

```
global = window_pos + content_offset + device_point
content_offset = (27, 80)   # calibrated: distance from window top-left
                            # to device screen top-left inside Simulator chrome
```

Example: device tap `(200, 400)` with window at `(40,40)` → screen click at
`(267, 520)`.

`tap` reads the **live** window position via System Events before each click,
so it stays correct even if the window was nudged after `setup-window`.

## Env overrides

| Var | Default |
|---|---|
| `SIM_UDID` | `20666568-4427-4300-86D9-F62127F4153A` |
| `APP_PATH` | `~/Library/Developer/Xcode/DerivedData/iosApp-acnynppqqosstfbcnbwfasfrneyu/Build/Products/Debug-iphonesimulator/Animally.app` |
| `WINDOW_POS` | `40,40` |
| `CONTENT_OFFSET_X` / `CONTENT_OFFSET_Y` | `27` / `80` |

If Simulator chrome changes (new Xcode version resizes the title bar),
re-calibrate: take a screenshot of the full screen, find the device screen's
top-left pixel, subtract the window position → new offset values.
