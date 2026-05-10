#!/usr/bin/env bash
# build-index-md.sh
# Generates index.md from index.json — human-readable Markdown table.
# Run from: docs/mappings/mx60-chihuahua/
# Usage: bash scripts/build-index-md.sh
# Requires: jq >= 1.6

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(dirname "$SCRIPT_DIR")"
JSON="$ROOT/index.json"
OUT="$ROOT/index.md"

if [[ ! -f "$JSON" ]]; then
  echo "ERROR: index.json not found at $JSON. Run merge-shards.sh first." >&2
  exit 1
fi

TOTAL=$(jq '.entries | length' "$JSON")
GENERATED_AT=$(jq -r '.generated_at' "$JSON")

{
  echo "# MX60 Chihuahua — Module Mapping Index"
  echo ""
  echo "**Module**: \`mx60-chihuahua\`  "
  echo "**Schema version**: 1.0  "
  echo "**Generated at**: ${GENERATED_AT}  "
  echo "**Total entries**: ${TOTAL}  "
  echo ""
  echo "## Kind distribution"
  echo ""
  echo "| Kind | Count |"
  echo "|------|------:|"
  jq -r '
    [.entries[].kind]
    | group_by(.)
    | map("| \(.[0]) | \(length) |")
    | .[]
  ' "$JSON"
  echo ""
  echo "## Domain distribution"
  echo ""
  echo "| Domain | Count |"
  echo "|--------|------:|"
  jq -r '
    [.entries[].domain]
    | group_by(.)
    | map("| \(.[0]) | \(length) |")
    | .[]
  ' "$JSON"
  echo ""
  echo "## Entries (sorted by path)"
  echo ""
  echo "| path | kind | domain | purpose | loc | status |"
  echo "|------|------|--------|---------|----:|--------|"
  jq -r '
    [.entries]
    | .[0]
    | sort_by(.path)
    | .[]
    | "| \(.path) | \(.kind) | \(.domain) | \(.purpose) | \(.loc) | \(.status) |"
  ' "$JSON"
  echo ""
  echo "## Exclusions"
  echo ""
  echo "| path | reason |"
  echo "|------|--------|"
  jq -r '
    .exclusions[]
    | "| \(.path) | \(.reason) |"
  ' "$JSON"
  echo ""
  echo "_See [excluded.md](excluded.md) for full exclusion documentation._"
} > "$OUT"

echo "index.md written to: $OUT"
echo "Row count (entries): $(grep -c '^|' "$OUT" || true) pipe-rows total (includes header/separator/exclusions)"
