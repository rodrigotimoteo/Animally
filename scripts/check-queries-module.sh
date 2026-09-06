#!/usr/bin/env bash
# check-queries-module.sh — Phase 5e build gate
# Verifies QueriesModule binding count vs .sq file count.
# 31 .sq files exist; 27 bindings are expected (4 intentionally unbound).
# Unbound (infra/support, not injectable via QueriesModule):
#   - Insights.sq (aggregate counts, owned by SqlDelightInsightsRepository)
#   - SearchFts.sq / SearchIndexState.sq (FTS, owned by SearchRepositoryImpl / healing gate)
#   - SyncMetadata.sq (owned by SyncMetadataRepositoryImpl via database.syncMetadataQueries)
# See shared/src/commonMain/kotlin/com/github/rodrigotimoteo/animally/di/database/QueriesModule.kt KDoc.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SQ_DIR="${SQ_DIR:-$REPO_ROOT/shared/src/commonMain/sqldelight}"
MODULE_FILE="${MODULE_FILE:-$REPO_ROOT/shared/src/commonMain/kotlin/com/github/rodrigotimoteo/animally/di/database/QueriesModule.kt}"

# These tables are deliberately owned by their repository/engine because they
# are support projections or state, rather than feature repositories.
INTENTIONALLY_UNBOUND="Insights SearchFts SearchIndexState SyncMetadata"

if [[ ! -d "$SQ_DIR" ]]; then
  echo "FAIL: SQLDelight directory not found: $SQ_DIR" >&2
  exit 1
fi
if [[ ! -f "$MODULE_FILE" ]]; then
  echo "FAIL: QueriesModule not found: $MODULE_FILE" >&2
  exit 1
fi

sq_basenames="$(find "$SQ_DIR" -type f -name '*.sq' -print | sed -E 's|.*/||; s/\.sq$//' | sort)"
binding_types="$(grep -Eo 'single<[A-Za-z][A-Za-z0-9_]*Queries>' "$MODULE_FILE" | sed -E 's/^single<|>$//g' || true)"

sq_count="$(printf '%s\n' "$sq_basenames" | sed '/^$/d' | wc -l | tr -d ' ')"
binding_count="$(printf '%s\n' "$binding_types" | sed '/^$/d' | wc -l | tr -d ' ')"

echo "QueriesModule check:"
echo "  .sq files: $sq_count"
echo "  bindings:  $binding_count"
echo "  direct-owned: $INTENTIONALLY_UNBOUND"
echo ""

duplicate_bindings="$(printf '%s\n' "$binding_types" | sed '/^$/d' | sort | uniq -d)"
if [[ -n "$duplicate_bindings" ]]; then
  echo "FAIL: duplicate QueriesModule bindings:" >&2
  printf '  %s\n' "$duplicate_bindings" >&2
  exit 1
fi

is_intentionally_unbound() {
  local base="$1"
  for allowed in $INTENTIONALLY_UNBOUND; do
    [[ "$base" == "$allowed" ]] && return 0
  done
  return 1
}

is_known_schema() {
  local query_type="$1"
  local base="${query_type%Queries}"
  printf '%s\n' "$sq_basenames" | grep -Fxq "$base"
}

failures=0
while IFS= read -r base; do
  [[ -z "$base" ]] && continue
  query_type="${base}Queries"
  binding_matches="$(printf '%s\n' "$binding_types" | grep -Fx "$query_type" || true)"
  match_count="$(printf '%s\n' "$binding_matches" | sed '/^$/d' | wc -l | tr -d ' ')"

  if is_intentionally_unbound "$base"; then
    if [[ "$match_count" -ne 0 ]]; then
      echo "FAIL: $base.sq is direct-owned but is bound as $query_type" >&2
      failures=$((failures + 1))
    fi
  elif [[ "$match_count" -ne 1 ]]; then
    echo "FAIL: $base.sq requires exactly one binding for $query_type (found $match_count)" >&2
    failures=$((failures + 1))
  fi
done <<< "$sq_basenames"

while IFS= read -r query_type; do
  [[ -z "$query_type" ]] && continue
  if ! is_known_schema "$query_type"; then
    echo "FAIL: binding $query_type has no matching .sq schema file" >&2
    failures=$((failures + 1))
  elif is_intentionally_unbound "${query_type%Queries}"; then
    echo "FAIL: direct-owned schema ${query_type%Queries}.sq must not be bound" >&2
    failures=$((failures + 1))
  fi
done <<< "$binding_types"

echo ""
echo "  direct-owned schemas:"
for base in $INTENTIONALLY_UNBOUND; do
  printf '    %s.sq\n' "$base"
done

if [[ "$failures" -ne 0 ]]; then
  echo "FAIL: $failures query ownership contract violation(s)." >&2
  exit 1
fi

echo "OK: every feature schema has one binding and every support schema is explicitly direct-owned."
