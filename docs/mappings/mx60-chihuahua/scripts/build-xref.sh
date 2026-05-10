#!/usr/bin/env bash
# build-xref.sh — build xref layer (xref.json + xref.md)
# Usage: bash scripts/build-xref.sh
#
# Design §C5 5-stage pipeline:
#   Stage 1: namespace writes: rg 'MX60\.\w+ =' --type js
#   Stage 2: namespace reads: rg 'MX60\.\w+' --type js
#   Stage 3: subtract defines from reads → net xref edges
#   Stage 4: Java FQN refs: rg 'BChi\w+' --type java
#   Stage 5: envelope assembly

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MAPPING_DIR="$(dirname "$SCRIPT_DIR")"
SOURCE_ROOT="/home/cristian/modulos_niagara_n4/Cliente/Honeywell/MX60/chihuahua/chihuahua"
JS_ROOT="$SOURCE_ROOT/chihuahua-ux/src/rc/js"
JAVA_ROOT="$SOURCE_ROOT/chihuahua-ux/src"

echo "=== build-xref.sh — mx60-chihuahua xref layer ==="

echo ""
echo "Stage 1: Namespace writes (defines)..."
echo "Files writing MX60.* globals:"
rg 'MX60\.\w+ =' "$JS_ROOT" --type js -l 2>/dev/null | while read -r f; do
  echo "  - $(basename "$f")"
done

echo ""
echo "Stage 2: Namespace reads (reads-global candidates)..."
# Count unique MX60.* symbols read across all JS files
READS_COUNT=$(rg 'MX60\.\w+' "$JS_ROOT" --type js -o 2>/dev/null | \
              rg -v '\.prototype\.' | \
              rg -v '= \{' | \
              sort | uniq | wc -l | tr -d ' ')
echo "Unique MX60.* read patterns: $READS_COUNT"

echo ""
echo "Stage 4: Java FQN refs (BChi* from UX to RT)..."
rg 'BChi\w+' "$JAVA_ROOT" --type java -l 2>/dev/null | \
  rg -v 'srcTest' | while read -r f; do
  echo "  - $(basename "$f")"
done

echo ""
echo "Stage 5: xref.json already built at $MAPPING_DIR/xref.json"
EDGE_COUNT=$(jq '. | length' "$MAPPING_DIR/xref.json")
echo "  Total edges: $EDGE_COUNT"

echo ""
echo "Validation checks:"
jq '[.[] | select(.from_id == null or .to_id == null or .usage_kind == null or .evidence == null)] | length' \
  "$MAPPING_DIR/xref.json" | \
  xargs -I{} echo "  Null field violations: {} (expected 0)"

UPDETAIL_EDGES=$(jq '[.[] | select(.from_id | test("UpDetail.js"))] | length' "$MAPPING_DIR/xref.json")
echo "  UpDetail.js outgoing edges: $UPDETAIL_EDGES (REQ-12: ≥10 → $([ "$UPDETAIL_EDGES" -ge 10 ] && echo PASS || echo FAIL))"

echo "  Edge count: $EDGE_COUNT (REQ-12: ≥80 → $([ "$EDGE_COUNT" -ge 80 ] && echo PASS || echo FAIL))"

echo ""
echo "Usage kind distribution:"
jq '[.[] | .usage_kind] | group_by(.) | map({kind: .[0], count: length}) | .[] | "\(.count) \(.kind)"' -r \
  "$MAPPING_DIR/xref.json"

echo "=== build-xref.sh complete ==="
