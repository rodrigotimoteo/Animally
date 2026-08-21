#!/usr/bin/env bash
# sim-e2e.sh — lightweight E2E helper for driving Animally on the iOS Simulator.
# Pure bash + xcrun + osascript. No external deps.
set -euo pipefail

SIM_UDID="${SIM_UDID:-20666568-4427-4300-86D9-F62127F4153A}"
BUNDLE_ID="com.github.rodrigotimoteo.animally.Animally"
APP_PATH="${APP_PATH:-$HOME/Library/Developer/Xcode/DerivedData/iosApp-acnynppqqosstfbcnbwfasfrneyu/Build/Products/Debug-iphonesimulator/Animally.app}"

# Calibration: Simulator window pinned at fixed origin; device (0,0) sits at
# window_pos + content offset (title bar + bezel). Override via env if needed.
WIN_X="${WIN_X:-40}"
WIN_Y="${WIN_Y:-40}"
CONTENT_OFFSET_X="${CONTENT_OFFSET_X:-27}"
CONTENT_OFFSET_Y="${CONTENT_OFFSET_Y:-80}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(dirname "$SCRIPT_DIR")"

usage() {
  cat <<EOF
Usage: sim-e2e.sh <subcommand> [args]

Subcommands:
  boot                 Boot simulator \$SIM_UDID and open Simulator.app
  install              Install app bundle (\$APP_PATH) onto simulator
  launch               Launch app ($BUNDLE_ID)
  terminate            Terminate app
  setup-window         Pin Simulator window to {$WIN_X,$WIN_Y} (deterministic coords)
  tap <x> <y>          Click at DEVICE point (converted to global screen coords)
  screenshot [name]    Save screenshot to /tmp/e2e/<name>.png (default: e2e)
  build                Build Debug-iphonesimulator from repo root

Env overrides:
  SIM_UDID            Simulator UDID           (default: $SIM_UDID)
  APP_PATH            App bundle path          (default: DerivedData path)
  WIN_X / WIN_Y       Pinned window origin     (default: 40 / 40)
  CONTENT_OFFSET_X/Y  Device->screen offset    (default: 27 / 80)
EOF
}

cmd_boot() {
  xcrun simctl boot "$SIM_UDID" 2>/dev/null || true
  open -a Simulator
  echo "Booted $SIM_UDID"
}

cmd_install() {
  if [[ ! -d "$APP_PATH" ]]; then
    echo "error: app bundle not found: $APP_PATH" >&2
    echo "run 'sim-e2e.sh build' first or set APP_PATH" >&2
    exit 1
  fi
  xcrun simctl install "$SIM_UDID" "$APP_PATH"
  echo "Installed $(basename "$APP_PATH")"
}

cmd_launch() {
  xcrun simctl launch "$SIM_UDID" "$BUNDLE_ID"
}

cmd_terminate() {
  xcrun simctl terminate "$SIM_UDID" "$BUNDLE_ID" 2>/dev/null || true
  echo "Terminated $BUNDLE_ID"
}

cmd_setup_window() {
  # Requires Accessibility permission for the calling terminal (System Preferences >
  # Privacy & Security > Accessibility).
  osascript <<EOF
tell application "System Events"
  tell process "Simulator"
    set frontmost to true
    set position of window 1 to {$WIN_X, $WIN_Y}
  end tell
end tell
EOF
  echo "Simulator window pinned to {$WIN_X,$WIN_Y}"
}

cmd_tap() {
  local dx="$1" dy="$2"
  local sx=$(( WIN_X + CONTENT_OFFSET_X + dx ))
  local sy=$(( WIN_Y + CONTENT_OFFSET_Y + dy ))
  osascript -e "tell application \"System Events\" to click at {$sx, $sy}" >/dev/null
  echo "tap($dx,$dy) -> screen($sx,$sy)"
}

cmd_screenshot() {
  local name="${1:-e2e}"
  mkdir -p /tmp/e2e
  xcrun simctl io "$SIM_UDID" screenshot "/tmp/e2e/${name}.png"
  echo "Saved /tmp/e2e/${name}.png"
}

cmd_build() {
  (
    cd "$REPO_ROOT"
    xcodebuild \
      -project iosApp/iosApp.xcodeproj \
      -scheme iosApp \
      -configuration Debug \
      -destination 'generic/platform=iOS Simulator' \
      build
  )
}

main() {
  if [[ $# -lt 1 ]]; then
    usage
    exit 1
  fi
  local cmd="$1"
  shift
  case "$cmd" in
    boot)        cmd_boot ;;
    install)     cmd_install ;;
    launch)      cmd_launch ;;
    terminate)   cmd_terminate ;;
    setup-window) cmd_setup_window ;;
    tap)
      [[ $# -eq 2 ]] || { echo "usage: sim-e2e.sh tap <x> <y>" >&2; exit 1; }
      cmd_tap "$1" "$2"
      ;;
    screenshot)  cmd_screenshot "${1:-}" ;;
    build)       cmd_build ;;
    help|-h|--help) usage ;;
    *)
      echo "unknown subcommand: $cmd" >&2
      usage
      exit 1
      ;;
  esac
}

main "$@"
