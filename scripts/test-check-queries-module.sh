#!/usr/bin/env bash
# Contract tests for check-queries-module.sh. Uses temporary schema fixtures
# so duplicate, missing, and unknown ownership errors stay executable.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CHECKER="$SCRIPT_DIR/check-queries-module.sh"
fixture_dir="$(mktemp -d)"
trap 'rm -rf "$fixture_dir"' EXIT

expect_failure() {
  if SQ_DIR="$fixture_dir/sq" MODULE_FILE="$fixture_dir/QueriesModule.kt" bash "$CHECKER" >/dev/null 2>&1; then
    echo "FAIL: expected query ownership fixture to fail" >&2
    exit 1
  fi
}

mkdir -p "$fixture_dir/sq"
touch "$fixture_dir/sq/Animal.sq" "$fixture_dir/sq/Owner.sq"

printf '%s\n' \
  'single<AnimalQueries> { get<Db>().animalQueries }' \
  'single<AnimalQueries> { get<Db>().animalQueries }' \
  > "$fixture_dir/QueriesModule.kt"
expect_failure

printf '%s\n' \
  'single<AnimalQueries> { get<Db>().animalQueries }' \
  'single<MissingQueries> { get<Db>().missingQueries }' \
  > "$fixture_dir/QueriesModule.kt"
expect_failure

printf '%s\n' \
  'single<AnimalQueries> { get<Db>().animalQueries }' \
  'single<OwnerQueries> { get<Db>().ownerQueries }' \
  > "$fixture_dir/QueriesModule.kt"
SQ_DIR="$fixture_dir/sq" MODULE_FILE="$fixture_dir/QueriesModule.kt" bash "$CHECKER" >/dev/null

echo "Query ownership contract fixtures passed."
