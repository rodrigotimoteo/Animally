#!/usr/bin/env bash
# check-queries-module.sh — Phase 5e build gate
# Verifies QueriesModule binding count vs .sq file count.
# 30 .sq files exist; 27 bindings are expected (3 intentionally unbound).
# Unbound (infra/support, not injectable via QueriesModule):
#   - SearchFts.sq / SearchIndexState.sq (FTS, owned by SearchRepositoryImpl / healing gate)
#   - SyncMetadata.sq (owned by SyncMetadataRepositoryImpl via database.syncMetadataQueries)
# See shared/src/commonMain/kotlin/com/github/rodrigotimoteo/animally/di/database/QueriesModule.kt KDoc.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SQ_DIR="$REPO_ROOT/shared/src/commonMain/sqldelight"
MODULE_FILE="$REPO_ROOT/shared/src/commonMain/kotlin/com/github/rodrigotimoteo/animally/di/database/QueriesModule.kt"

sq_count=$(find "$SQ_DIR" -name "*.sq" | wc -l | tr -d ' ')
binding_count=$(grep -c 'single<.*Queries>' "$MODULE_FILE" || true)
# Known intentionally-unbound .sq files (documented in QueriesModule KDoc)
expected_unbound=3
expected_binding=$((sq_count - expected_unbound))

echo "QueriesModule check:"
echo "  .sq files:        $sq_count"
echo "  bindings:         $binding_count"
echo "  expected unbound: $expected_unbound ($sq_count - $expected_unbound = $expected_binding)"
echo ""

# List .sq basenames vs bound Queries types for audit
echo "  .sq files:"
find "$SQ_DIR" -name "*.sq" | sort | sed 's|.*/||'
echo ""
echo "  bound Queries (from QueriesModule.kt):"
grep 'single<.*Queries>' "$MODULE_FILE" | sed -E 's/.*single<([^>]+)>.*/\1/' | sort
echo ""

if [[ "$binding_count" -eq "$expected_binding" ]]; then
  echo "OK: binding count matches $sq_count - $expected_unbound (3 infra tables intentionally unbound)."
  exit 0
else
  echo "FAIL: expected $expected_binding bindings for $sq_count .sq files (sq - 3 unbound), got $binding_count." >&2
  echo "If a new .sq was added, add its Queries binding to QueriesModule.kt or document it as intentionally unbound." >&2
  exit 1
fi
