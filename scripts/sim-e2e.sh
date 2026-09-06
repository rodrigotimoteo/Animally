#!/usr/bin/env bash
# sim-e2e.sh — lightweight E2E helper for driving Animally on the iOS Simulator.
# Pure bash + xcrun + osascript. No external deps.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(dirname "$SCRIPT_DIR")"
BUNDLE_ID="com.github.rodrigotimoteo.animally.Animally"
DERIVED_DATA_PATH="${DERIVED_DATA_PATH:-$REPO_ROOT/.build/ios-simulator}"
APP_PATH="${APP_PATH:-$DERIVED_DATA_PATH/Build/Products/Debug-iphonesimulator/Animally.app}"
E2E_OUTPUT_DIR="${E2E_OUTPUT_DIR:-/tmp/e2e}"

# Calibration: Simulator window pinned at fixed origin; device (0,0) sits at
# window_pos + content offset (title bar + bezel). Override via env if needed.
WIN_X="${WIN_X:-40}"
WIN_Y="${WIN_Y:-40}"
CONTENT_OFFSET_X="${CONTENT_OFFSET_X:-27}"
CONTENT_OFFSET_Y="${CONTENT_OFFSET_Y:-80}"

select_simulator() {
  if [[ -n "${SIM_UDID:-}" ]]; then
    printf '%s\n' "$SIM_UDID"
    return
  fi

  local discovered
  discovered="$(xcrun simctl list devices available | awk -F '[()]' '/iPhone/ && /Shutdown|Booted/ { print $2; exit }')"
  if [[ -z "$discovered" ]]; then
    discovered="$(xcrun simctl list devices available | awk -F '[()]' '/(iPad|iPhone)/ { print $2; exit }')"
  fi
  if [[ -z "$discovered" ]]; then
    echo "error: no available iOS Simulator device found; set SIM_UDID explicitly" >&2
    exit 1
  fi
  printf '%s\n' "$discovered"
}

SIM_UDID="${SIM_UDID:-}"

window_position() {
  osascript -e 'tell application "System Events" to tell process "Simulator" to get position of window 1' |
    tr -d ' ' | tr ',' ' '
}

ensure_simulator_selected() {
  if [[ -z "$SIM_UDID" ]]; then
    SIM_UDID="$(select_simulator)"
  fi
}

ensure_booted() {
  ensure_simulator_selected
  xcrun simctl bootstatus "$SIM_UDID" -b >/dev/null
}

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
  screenshot [name]    Save screenshot to \$E2E_OUTPUT_DIR/<name>.png (default: e2e)
  build                Build Debug-iphonesimulator from repo root

Env overrides:
  SIM_UDID            Simulator UDID           (default: first available iPhone/iPad)
  DERIVED_DATA_PATH   Xcode output directory   (default: repo/.build/ios-simulator)
  APP_PATH            App bundle path          (default: derived-data app path)
  E2E_OUTPUT_DIR      Screenshot directory    (default: /tmp/e2e)
  WIN_X / WIN_Y       Pinned window origin     (default: 40 / 40)
  CONTENT_OFFSET_X/Y  Device->screen offset    (default: 27 / 80)
EOF
}

cmd_boot() {
  ensure_simulator_selected
  if [[ "$(xcrun simctl list devices | awk -v udid="$SIM_UDID" '$0 ~ udid { print }')" != *Booted* ]]; then
    xcrun simctl boot "$SIM_UDID"
  fi
  ensure_booted
  open -a Simulator
  echo "Booted $SIM_UDID"
}

cmd_install() {
  ensure_booted
  if [[ ! -d "$APP_PATH" ]]; then
    echo "error: app bundle not found: $APP_PATH" >&2
    echo "run 'sim-e2e.sh build' first or set APP_PATH" >&2
    exit 1
  fi
  xcrun simctl install "$SIM_UDID" "$APP_PATH"
  echo "Installed $(basename "$APP_PATH")"
}

cmd_launch() {
  ensure_booted
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
  local live_x live_y
  read -r live_x live_y <<< "$(window_position)"
  local sx=$(( live_x + CONTENT_OFFSET_X + dx ))
  local sy=$(( live_y + CONTENT_OFFSET_Y + dy ))
  osascript -e "tell application \"System Events\" to click at {$sx, $sy}" >/dev/null
  echo "tap($dx,$dy) -> screen($sx,$sy)"
}

cmd_screenshot() {
  local name="${1:-e2e}"
  ensure_booted
  mkdir -p "$E2E_OUTPUT_DIR"
  xcrun simctl io "$SIM_UDID" screenshot "$E2E_OUTPUT_DIR/${name}.png"
  echo "Saved $E2E_OUTPUT_DIR/${name}.png"
}

cmd_build() {
  (
    cd "$REPO_ROOT"
    xcodebuild \
      -project iosApp/iosApp.xcodeproj \
      -scheme iosApp \
      -configuration Debug \
      -derivedDataPath "$DERIVED_DATA_PATH" \
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
