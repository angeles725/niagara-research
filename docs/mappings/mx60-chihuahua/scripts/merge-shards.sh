#!/usr/bin/env bash
# merge-shards.sh
# Merges all S1-S6 shard JSONs into the final index.json.
# Run from: docs/mappings/mx60-chihuahua/
# Usage: bash scripts/merge-shards.sh
# Requires: jq >= 1.6

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(dirname "$SCRIPT_DIR")"
OUT="$ROOT/index.json"

cd "$ROOT"

jq -s '{
  schema_version: "1.0",
  module: "mx60-chihuahua",
  extensions: ["backend","frontend_iife"],
  source_repo: "/home/cristian/modulos_niagara_n4/Cliente/Honeywell/MX60/chihuahua/chihuahua",
  generated_at: (now | todate),
  generator: "sdd-apply mapping-mx60 v1",
  entries: ([.[].entries] | flatten | group_by(.id) | map(.[0])),
  exclusions: ([.[].exclusions // []] | flatten | unique)
}' \
  shards/s1-backend-rt.json \
  shards/s2-backend-ux.json \
  shards/s3-frontend-core.json \
  shards/s4-frontend-equipment.json \
  shards/s5-frontend-alarms-schedules.json \
  shards/s6-resources-config.json \
  > "$OUT"

echo "Merged index.json written to: $OUT"
echo "Entry count: $(jq '.entries | length' "$OUT")"
echo "Exclusion count: $(jq '.exclusions | length' "$OUT")"
