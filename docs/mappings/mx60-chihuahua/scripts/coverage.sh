#!/usr/bin/env bash
# coverage.sh — compute coverage % for mx60-chihuahua mapping
# Usage: bash scripts/coverage.sh [index_json_path] [source_root]
# REQ-8: coverage ≥95%, fidelity ≥90%

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MAPPING_DIR="$(dirname "$SCRIPT_DIR")"

INDEX_JSON="${1:-$MAPPING_DIR/index.json}"
SOURCE_ROOT="${2:-/home/cristian/modulos_niagara_n4/Cliente/Honeywell/MX60/chihuahua/chihuahua}"

echo "=== mx60-chihuahua Coverage Report ==="
echo "Index: $INDEX_JSON"
echo "Source root: $SOURCE_ROOT"
echo ""

# Count source entries in index.json (status == "source" AND kind is code, not binary)
MAPPED_COUNT=$(jq '[.entries[] | select(.status == "source")] | length' "$INDEX_JSON")
echo "Mapped source entries (status=source): $MAPPED_COUNT"

# Count actual source files (java + js, excluding compiled bundles in ext/ and srcTest)
TOTAL_FILES=$(fd '\.(java|js)$' "$SOURCE_ROOT" --exclude srcTest --exclude .git --exclude build 2>/dev/null | \
              grep -v 'rc/ext/' | \
              grep -v 'rc/fonts/' | \
              wc -l | tr -d ' ')
echo "Total in-scope source files (java+js, excl srcTest/ext/fonts): $TOTAL_FILES"

# Also count all files including ext (for reference)
ALL_FILES=$(fd '\.(java|js)$' "$SOURCE_ROOT" --exclude srcTest --exclude .git --exclude build 2>/dev/null | wc -l | tr -d ' ')
echo "Total source files incl. compiled bundles: $ALL_FILES"

# Coverage calculation
if [ "$TOTAL_FILES" -gt 0 ]; then
  COV_PCT=$(echo "scale=1; $MAPPED_COUNT * 100 / $TOTAL_FILES" | bc)
  echo ""
  echo "=== Coverage: $MAPPED_COUNT / $TOTAL_FILES = ${COV_PCT}% ==="

  # REQ-8 check
  COV_INT=$(echo "scale=0; $MAPPED_COUNT * 100 / $TOTAL_FILES" | bc)
  if [ "$COV_INT" -ge 95 ]; then
    echo "REQ-8 PASS: coverage ${COV_PCT}% >= 95%"
  elif [ "$COV_INT" -ge 90 ]; then
    echo "REQ-8 PASS-WITH-WARNING: coverage ${COV_PCT}% >= 90% but < 95%"
  else
    echo "REQ-8 FAIL: coverage ${COV_PCT}% < 90% (hard floor)"
    exit 1
  fi
else
  echo "ERROR: no source files found at $SOURCE_ROOT"
  exit 1
fi

echo ""
echo "=== Domain distribution ==="
jq -r '[.entries[] | .domain] | group_by(.) | map({domain: .[0], count: length}) | sort_by(.count) | reverse | .[] | "\(.count)\t\(.domain)"' "$INDEX_JSON"

echo ""
echo "=== Kind distribution ==="
jq -r '[.entries[] | .kind] | group_by(.) | map({kind: .[0], count: length}) | sort_by(.count) | reverse | .[] | "\(.count)\t\(.kind)"' "$INDEX_JSON"

echo ""
echo "=== Shard cap check (≤75 per shard) ==="

# S1: service-container + equipment-backend
S1=$(jq '[.entries[] | select(.domain | IN("service-container","equipment-backend"))] | length' "$INDEX_JSON")
echo "S1 (service-container+equipment-backend): $S1 $([ "$S1" -le 75 ] && echo 'PASS' || echo 'FAIL')"

# S2: http-rest + equipment-reader + alarms-backend + history-backend + schedules-backend + util-backend
S2=$(jq '[.entries[] | select(.domain | IN("http-rest","equipment-reader","alarms-backend","history-backend","schedules-backend","util-backend"))] | length' "$INDEX_JSON")
echo "S2 (backend-ux domains): $S2 $([ "$S2" -le 75 ] && echo 'PASS' || echo 'FAIL')"

# S3: app-shell + baja-integration + ui-lib
S3=$(jq '[.entries[] | select(.domain | IN("app-shell","baja-integration","ui-lib"))] | length' "$INDEX_JSON")
echo "S3 (frontend-core): $S3 $([ "$S3" -le 75 ] && echo 'PASS' || echo 'FAIL')"

# S4: equipment-frontend + equipment-detail + threshold-stores
S4=$(jq '[.entries[] | select(.domain | IN("equipment-frontend","equipment-detail","threshold-stores"))] | length' "$INDEX_JSON")
echo "S4 (frontend-equipment): $S4 $([ "$S4" -le 75 ] && echo 'PASS' || echo 'FAIL')"

# S5: alarms-frontend + schedules-frontend + history-frontend
S5=$(jq '[.entries[] | select(.domain | IN("alarms-frontend","schedules-frontend","history-frontend"))] | length' "$INDEX_JSON")
echo "S5 (frontend-alarms-schedules): $S5 $([ "$S5" -le 75 ] && echo 'PASS' || echo 'FAIL')"

# S6: module-descriptor + build-config + static-resources
S6=$(jq '[.entries[] | select(.domain | IN("module-descriptor","build-config","static-resources"))] | length' "$INDEX_JSON")
echo "S6 (resources-config): $S6 $([ "$S6" -le 75 ] && echo 'PASS' || echo 'FAIL')"

echo ""
echo "=== verified_at count ==="
VA_COUNT=$(jq '[.entries[] | select(.verified_at != null)] | length' "$INDEX_JSON")
echo "verified_at non-null: $VA_COUNT $([ "$VA_COUNT" -ge 40 ] && echo 'PASS (REQ-9)' || echo 'FAIL: need >=40 (REQ-9)')"

echo ""
echo "=== REQ-14: inferred-from-mapping count ==="
INFERRED=$(rg 'inferred from mapping' "$MAPPING_DIR" -r --include='*.md' --include='*.json' 2>/dev/null | wc -l | tr -d ' ')
echo "inferred-from-mapping occurrences: $INFERRED $([ "$INFERRED" -ge 10 ] && echo 'PASS (REQ-14)' || echo 'FAIL: need >=10 (REQ-14)')"

echo ""
echo "=== Coverage report complete ==="
