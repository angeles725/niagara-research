# SDD Design — mapping-mx60

**Date**: 2026-05-09
**Phase**: sdd-design
**Reads**: proposal.md (engram #1248) + explore.md (engram #1247) + reflow schema.md (file)
**Reference template**: `docs/mappings/reflow-clean-177/` (archive #1219)

---

## A. MX60 schema (locked)

### A.0 Reuse policy

MX60 schema doc lives at `docs/mappings/mx60-chihuahua/schema.md` (created in apply S6/S0). It MUST declare:

```
schema_version: "1.0"
extensions: ["backend", "frontend_iife"]
extends: "../reflow-clean-177/schema.md"
```

Rationale: per reflow schema §"Versioning Rules", adding extension blocks is **no-bump**. Adding new `kind` enum values is normally MINOR (v1.1) — but reflow's own rule ("parsers must tolerate unknown enums") plus the additive `extensions` declaration in the envelope makes this a **forward-compatible additive extension** consumable by reflow's existing tooling. Hence: keep `schema_version: "1.0"`, declare `extensions: ["frontend_iife"]` in BOTH the envelope and schema.md, and document the new kinds as additive.

### A.1 New `kind` enum values (5)

Adds to reflow's `kind` enum WITHOUT removing existing values. Discriminator rules locked here so apply phase has no ambiguity.

| New `kind` | Path discriminator (rg pattern) | IIFE pattern check | Namespace assignment | Examples |
|-----------|--------------------------------|---------------------|----------------------|----------|
| `iife-app` | `chihuahua-ux/src/rc/js/app/(?!.*Store\.js$).*\.js` AND filename matches page/section module | top-level `(function(window){'use strict';var MX60=...` AND assigns one or more `MX60.<PageNoun>` | Page/section orchestrators | DashboardApp.js, AlarmsManager.js, AlarmsPage.js, EquipmentDetail.js, UpDetail.js, CarcamoDetail.js, DataloggerDetail.js, ScheduleView.js, HomeMap.js, Router.js, ConfigManager.js, SharedEnv.js |
| `iife-store` | filename ends with `Store.js` under `chihuahua-ux/src/rc/js/app/` | IIFE wrapper present | assigns `MX60.<Name>Store` with module-level state vars | EquipmentSnapshotStore.js, UpThresholdStore.js, ModoOverrideStore.js, OutputOverrideStore.js, CarcamoThresholdStore.js, DataloggerThresholdStore.js, AlarmLatchStore.js |
| `iife-lib` | path under `chihuahua-ux/src/rc/js/lib/` OR (under `app/` AND name matches `SubscriptionPool\|WritePoint\|Toast\|Confirm\|Dropdown\|Popover\|StatusResolver\|RelativeTime\|CsvExport\|BulkActionBar\|AlarmCards\|AlarmDetailsTable\|AlarmDetailPage\|AlarmModalActions\|AlarmNotesModal\|EquipmentCard\|EquipmentData\|LiveHistoryBuffer\|TimeRangePicker`) | IIFE wrapper present | assigns `MX60.<UtilName>` reusable across pages | SubscriptionPool.js, WritePoint.js, Toast.js, Confirm.js, etc. |
| `iife-util` | path under `chihuahua-ux/src/rc/js/util/` | IIFE wrapper present (or pure-function exposure on `MX60.<util>`) | small co-located helper | (per explore §1, 3 files in js/util/) |
| `iife-entry` | exactly `chihuahua-ux/src/rc/js/app/main.js` if it exists, otherwise the bootstrap file invoked by index.html | sets up `MX60.boot()` or invokes Router init at end of file | application entry point | bootstrap module (analogous to reflow `main.js`) |

**Decision tree (apply sub-agent uses this in order)**:

1. Java file → `kind: java-class` (reflow rule — unchanged).
2. Path under `rc/js/util/` → `iife-util`.
3. Path under `rc/js/lib/` → `iife-lib`.
4. Filename ends `Store.js` → `iife-store`.
5. File matches reusable utility name in lib list above → `iife-lib`.
6. File is the bootstrap entry → `iife-entry`.
7. Otherwise (under `rc/js/app/`) → `iife-app`.
8. Path under `rc/ext/` → `compiled-bundle` (reflow rule — third-party bundled JS).
9. Reflow rules apply unchanged for `config`, `module-descriptor`, `resource-image`, `resource-icon`, `resource-template`.

### A.2 `frontend_iife` extension block (locked JSON schema)

Applies to: entries with `kind` in `[iife-app, iife-store, iife-lib, iife-util, iife-entry]`.

```json
"frontend_iife": {
  "namespace": "MX60",
  "globals_written": ["MX60.AlarmsManager"],
  "globals_read": ["MX60.ConfigManager", "MX60.Router", "MX60.SubscriptionPool"],
  "iife_pattern": "iife-window",
  "load_order_hint": 12,
  "subscriber_role": "consumer"
}
```

| Field | Type | Mandatory | Notes |
|-------|------|-----------|-------|
| `namespace` | string | yes | Always `"MX60"` for this module. Future modules may use other roots. |
| `globals_written` | string[] | yes (may be `[]`) | All `MX60.X` assignments at top-level of IIFE body. Detect via rg `MX60\.\w+\s*=`. |
| `globals_read` | string[] | yes (may be `[]`) | All `MX60.X` reads (excluding self-writes). Detect via rg `MX60\.\w+(?!\s*=)`, dedupe and remove self. |
| `iife_pattern` | enum | yes | `iife-window` (`(function(window){...})(window);`), `iife-self` (`(function(self){...})(self);`), `iife-named` (top-level `var X=(function(){...})();`), `iife-other` (anything else, including `not-iife`). Single value. |
| `load_order_hint` | integer or null | yes | Inferred load order from `index.html` `<script>` tag position; `null` if file is not script-loaded directly (e.g. dynamic load). |
| `subscriber_role` | enum | yes | `consumer` (uses SubscriptionPool to read), `producer` (publishes via SubscriptionPool), `none`. Bloque #68 §68.5 hooks here. |

**Excluded from this block** (deliberately, per proposal D2 rationale):
- `iife_export_keys` — redundant with `globals_written`
- `vue_replacement_target` — belongs in `delta.json` not the index entry
- `module_type` — duplicates `kind` (kind already encodes app/store/lib/util/entry)

### A.3 `frontend_vue` block — N/A in MX60

MX60 has zero `.vue` files. Apply sub-agents MUST NOT emit `frontend_vue` blocks. Per-shard validator (C1) MUST reject any entry containing this block.

### A.4 `frontend_js` block — restricted use

Reflow's `frontend_js` block is reserved for entries whose `kind` is one of reflow's original JS kinds (`js-store`, `js-mixin`, `js-plugin`, `js-api`, `js-lib`, `js-router`, `js-util`, `js-entry`). MX60 entries use the new `iife-*` kinds → `frontend_iife` block instead. Apply sub-agents MUST NOT emit `frontend_js` for MX60 entries (the two blocks are mutually exclusive on `kind` discriminator).

### A.5 `backend` block — unchanged from reflow

Reused verbatim. All MX60 java-class entries (~17 source files) populate this block. `decompiled: false` for all (full source available, no CFR runs needed).

---

## B. Index.json envelope shape

Reused from reflow, with two additive fields. Apply sub-agents emit per-shard JSON; merge step assembles the final envelope.

```json
{
  "schema_version": "1.0",
  "module": "mx60-chihuahua",
  "extensions": ["backend", "frontend_iife"],
  "source_repo": "/home/cristian/modulos_niagara_n4/Cliente/Honeywell/MX60/chihuahua/chihuahua/",
  "generated_at": "2026-05-09T00:00:00Z",
  "generator": "sdd-apply mapping-mx60 v1",
  "entries": [ /* 100-130 */ ],
  "exclusions": [ /* see B.2 */ ]
}
```

### B.1 New envelope fields vs reflow

- **`extensions: string[]`** — declares which extension blocks the consumer should expect. Closes reflow W-2 (extension implicit-knowledge gap). Mandatory in MX60 envelope. Reflow can backfill this in a follow-up but MX60 ships with it from day one.

### B.2 `exclusions` — required from day one (closes reflow W-3)

Reflow shipped `exclusions: []` empty in JSON; the catalog lived in `excluded.md`. MX60 closes this gap: `exclusions[]` MUST be populated in `index.json`, machine-readable mirror of `excluded.md`.

Population strategy (apply T-S6 step):

1. Author `excluded.md` first (human-readable catalog with reasons).
2. `excluded.md` uses a fixed format:
   ```
   ### path/to/excluded
   **Reason**: <one-line reason>
   ```
3. Generator script (sd + jq) parses `excluded.md` into JSON entries:
   ```json
   { "path": "chihuahua-rt/srcTest/", "reason": "Test sources excluded per reflow convention; Niagara test discovery broken in plugin 7.3.40" }
   ```
4. The script outputs `exclusions.partial.json` which the merge step splices into the envelope.

Estimated entries: ~25 (16 test paths + .git + .gradle + .idea + build/ + audit-2026-05-06 + ext/ duplicates marker + decompiled-cache).

---

## C. JQ pipelines (canonical scripts)

All pipelines require `jq 1.7+` (string interpolation `\(.field)`) — already used by reflow toolchain. Each script is committed under `docs/mappings/mx60-chihuahua/scripts/` (created by apply T-S0).

### C1. Per-shard validator (`scripts/validate-shard.jq`)

Runs after each apply sub-agent returns its shard JSON. Fails loud if entry shape deviates. Emits a list of `{id, errors[]}` per failing entry; exits non-zero if any failures.

```jq
# validate-shard.jq — apply: jq -e -f scripts/validate-shard.jq shard-Sx.json
# Input: an array of entry objects (a single shard's contribution).
# Output: empty array on success, non-empty array of {id, errors} on failure.

def required_core: ["id","path","kind","domain","purpose","dependencies","loc","status"];

def allowed_kind: [
  "java-class","config","resource-image","resource-icon","resource-template",
  "compiled-class","compiled-jar","compiled-bundle","module-descriptor",
  "iife-app","iife-store","iife-lib","iife-util","iife-entry"
];

def allowed_status: ["source","compiled","bundle","resource","excluded"];

def prohibited_fields: ["from","caller","file","callers","used_by","edges","source_path","name"];

def validate_entry:
  . as $e
  | [
      # Core presence
      (required_core[] as $f | select(($e[$f] // null) == null) | "missing core field: \($f)"),
      # Kind enum
      (select(($e.kind | IN(allowed_kind[])) | not) | "invalid kind: \($e.kind)"),
      # Status enum
      (select(($e.status | IN(allowed_status[])) | not) | "invalid status: \($e.status)"),
      # Purpose ≤150 chars
      (select(($e.purpose | length) > 150) | "purpose >150 chars (\($e.purpose | length))"),
      # path == id
      (select($e.id != $e.path) | "id != path"),
      # dependencies array shape
      (select(($e.dependencies | type) != "array") | "dependencies must be array"),
      # Prohibited top-level fields
      (prohibited_fields[] as $f | select(($e[$f] // null) != null) | "prohibited field present: \($f)"),
      # frontend_vue forbidden in MX60
      (select(($e.frontend_vue // null) != null) | "frontend_vue forbidden in MX60"),
      # frontend_js + iife-* mutually exclusive
      (select(($e.frontend_js // null) != null and (($e.kind | startswith("iife-")))) | "frontend_js forbidden when kind is iife-*"),
      # frontend_iife required when kind is iife-*
      (select(($e.kind | startswith("iife-")) and (($e.frontend_iife // null) == null)) | "frontend_iife required for iife-* kinds"),
      # backend required when kind is java-class
      (select($e.kind == "java-class" and (($e.backend // null) == null)) | "backend block required for java-class"),
      # frontend_iife.iife_pattern enum
      (select(($e.frontend_iife // null) != null
              and (($e.frontend_iife.iife_pattern // null) | IN("iife-window","iife-self","iife-named","iife-other") | not))
       | "invalid iife_pattern: \($e.frontend_iife.iife_pattern)"),
      # frontend_iife.subscriber_role enum
      (select(($e.frontend_iife // null) != null
              and (($e.frontend_iife.subscriber_role // null) | IN("consumer","producer","none") | not))
       | "invalid subscriber_role")
    ];

[ .[] | {id: .id, errors: validate_entry} | select(.errors | length > 0) ]
```

Apply gate: `jq -e -f scripts/validate-shard.jq shard.json | jq -e 'length == 0'` — non-zero exit aborts merge.

### C2. Index.md generator (`scripts/build-index-md.sh`)

Bash + jq. Reads `index.json`, emits master markdown table grouped by domain.

```bash
#!/usr/bin/env bash
# build-index-md.sh — generates index.md from index.json
# Usage: ./scripts/build-index-md.sh > index.md
set -euo pipefail
IDX="${1:-index.json}"

cat <<'HDR'
# MX60 Chihuahua — Module Mapping Master Index

> **Schema**: v1.0 (extensions: backend, frontend_iife)
> Generated by `scripts/build-index-md.sh` from `index.json`.
> Do NOT hand-edit. Re-run the generator after `index.json` changes.

HDR

# Summary block
jq -r '
  "## Summary\n" +
  "- Total entries: \(.entries | length)\n" +
  "- Schema version: \(.schema_version)\n" +
  "- Generated: \(.generated_at)\n" +
  "- Source root: `\(.source_repo)`\n" +
  "- Exclusions: \(.exclusions | length)\n" +
  "\n### Kind distribution\n" +
  ( [ .entries[].kind ] | group_by(.) | map("- `\(.[0])`: \(length)") | join("\n") ) +
  "\n\n### Domain distribution\n" +
  ( [ .entries[].domain ] | group_by(.) | map("- `\(.[0])`: \(length)") | join("\n") )
' "$IDX"

echo
echo "## Entries by domain"
echo

# Per-domain tables
for DOMAIN in $(jq -r '[.entries[].domain] | unique | sort | .[]' "$IDX"); do
  echo "### $DOMAIN"
  echo
  echo "| ID | Kind | LOC | Purpose |"
  echo "|----|------|----:|---------|"
  jq -r --arg d "$DOMAIN" '
    .entries[]
    | select(.domain == $d)
    | "| `\(.id)` | \(.kind) | \(.loc) | \(.purpose) |"
  ' "$IDX"
  echo
done

# Exclusions appendix
echo "## Exclusions"
echo
echo "| Path | Reason |"
echo "|------|--------|"
jq -r '.exclusions[] | "| `\(.path)` | \(.reason) |"' "$IDX"
```

Why bash heredoc + jq vs pure jq: per #1231, markdown tables >50 rows are jq-generated; piping through bash for headers/section breaks keeps the jq scripts atomic and reusable for `xref.md` and `delta-vs-reflow.md`.

### C3. Coverage calculator (`scripts/coverage.sh`)

```bash
#!/usr/bin/env bash
# coverage.sh — emits coverage % (actionable_entries / total_source_files)
set -euo pipefail
IDX="${1:-index.json}"
ROOT="$(jq -r '.source_repo' "$IDX")"

# Total source files (post-exclusions) — exclude .git, .gradle, .idea, build/, audit-*, srcTest/
TOTAL=$(fd --type f \
  --exclude '.git' --exclude '.gradle' --exclude '.idea' --exclude 'build' \
  --exclude 'audit-*' --exclude 'srcTest' \
  . "$ROOT" | wc -l)

# Actionable entries: status in ["source", "config", "resource", "bundle"] (NOT "excluded")
ACTIONABLE=$(jq '[ .entries[] | select(.status != "excluded") ] | length' "$IDX")

PCT=$(jq -n --argjson a "$ACTIONABLE" --argjson t "$TOTAL" '($a / $t * 1000 | floor) / 10')

cat <<EOF
Total source files (post-fd-exclude): $TOTAL
Actionable entries in index.json:    $ACTIONABLE
Coverage %:                          $PCT
EOF

# Gate
jq -n --argjson p "$PCT" 'if $p < 95 then halt_error(1) else "OK" end'
```

Acceptance gate: `>= 95%` (matches reflow REQ-7 threshold).

### C4. Delta.json builder (`scripts/build-delta.sh` + `scripts/delta-classify.jq`)

Two stages: rg/jq classify candidates, then jq join produces `delta.json`.

```bash
#!/usr/bin/env bash
# build-delta.sh — produces delta.json
set -euo pipefail
MX60_IDX="${1:-index.json}"
REFLOW_IDX="${2:-../reflow-clean-177/index.json}"
MX60_ROOT="$(jq -r '.source_repo' "$MX60_IDX")"

# Stage 1 — extract identifying tokens from both sides
jq -r '.entries[] | {id, kind, domain, loc, purpose, deps: .dependencies}' "$REFLOW_IDX" > /tmp/reflow-flat.json
jq -r '.entries[] | {id, kind, domain, loc, purpose, deps: .dependencies}' "$MX60_IDX"   > /tmp/mx60-flat.json

# Stage 2 — port-marker scan in MX60 source (HEREDADO/REESCRITO signal)
rg --json -i 'ported from snls|ported from san luis|port of |ported verbatim|adapted from snls' \
  "$MX60_ROOT" --type java --type js \
  | jq -s 'map(select(.type=="match") | {file: .data.path.text, line: .data.line_number, text: .data.lines.text})' \
  > /tmp/port-markers.json

# Stage 3 — classify (jq script C4b)
jq -s -f scripts/delta-classify.jq \
  /tmp/mx60-flat.json /tmp/reflow-flat.json /tmp/port-markers.json \
  > delta.json
```

`scripts/delta-classify.jq`:

```jq
# delta-classify.jq
# inputs: $mx60 (array), $reflow (array), $markers (array)
# emits delta.json with envelope + deltas[]

. as [$mx60, $reflow, $markers]

# Helper: derive "name" from id (basename without ext)
def name_of(id): id | split("/") | last | sub("\\.(java|js|vue)$"; "");

# Helper: LOC delta percent (handles 0)
def loc_delta($a; $b):
  if ($b // 0) == 0 then null
  else (((($a // 0) - ($b // 0)) / ($b // 1)) * 100 | floor) end;

# Build name → reflow entry index
($reflow | map({key: name_of(.id), value: .}) | from_entries) as $reflow_by_name |
($mx60   | map({key: name_of(.id), value: .}) | from_entries) as $mx60_by_name |

# Marker file set
($markers | map(.file) | unique) as $marker_files |

# Status classifier per MX60 entry
def classify($mx60e; $reflow_match; $has_marker):
  if $reflow_match == null then "NUEVO"
  else
    (loc_delta($mx60e.loc; $reflow_match.loc)) as $delta |
    if $has_marker and ($delta != null and ($delta | fabs) <= 15) then "HEREDADO"
    elif $has_marker and ($delta != null and ($delta | fabs) > 30) then "REESCRITO"
    elif $delta != null and ($delta | fabs) <= 15 then "ANÁLOGO"
    elif $delta != null and ($delta | fabs) > 30 then "REESCRITO"
    else "ANÁLOGO"
    end
  end;

{
  schema_version: "1.0",
  module: "mx60-chihuahua",
  compared_against: "reflow-clean-177",
  generated_at: (now | todate),
  deltas: (
    # NUEVO + HEREDADO/REESCRITO/ANÁLOGO from MX60 perspective
    ($mx60 | map(
      . as $e |
      ($reflow_by_name[name_of($e.id)] // null) as $rmatch |
      ([$marker_files[] | select(contains(name_of($e.id)))] | length > 0) as $marker |
      {
        mx60_id: $e.id,
        reflow_id: ($rmatch.id // null),
        status: classify($e; $rmatch; $marker),
        loc_mx60: $e.loc,
        loc_reflow: ($rmatch.loc // null),
        loc_delta_pct: loc_delta($e.loc; ($rmatch.loc // 0)),
        evidence: (if $marker then "rg port-marker hit" else "name + LOC heuristic" end),
        bloque68_section: null,
        notes: ""
      }
    ))
    +
    # FALTA: reflow entries with no MX60 analog
    ($reflow | map(
      . as $r |
      select($mx60_by_name[name_of($r.id)] == null and ($r.kind == "java-class" or ($r.kind | startswith("vue-")) or ($r.kind | startswith("js-"))))
      | {
          mx60_id: null,
          reflow_id: $r.id,
          status: "FALTA",
          loc_mx60: null,
          loc_reflow: $r.loc,
          loc_delta_pct: null,
          evidence: "absent in MX60 source",
          bloque68_section: null,
          notes: ""
        }
    ))
  )
}
```

Post-script step: human spot-check pass populates `bloque68_section` for the §68.1-§68.5 entries (manual; ~25 rows).

Markdown render (`scripts/build-delta-md.sh`) follows same pattern as `build-index-md.sh` — jq groups by `status`, emits one table per status group plus the bloque #68 evidence index.

### C5. Xref builder (`scripts/build-xref.sh`)

`rg` scans MX60 source for `MX60.X` namespace assignments and reads, emits edges into `xref.json`.

```bash
#!/usr/bin/env bash
# build-xref.sh — produces xref.json
set -euo pipefail
ROOT="${1:-/home/cristian/modulos_niagara_n4/Cliente/Honeywell/MX60/chihuahua/chihuahua/chihuahua-ux/src/rc/js}"

# Stage 1 — namespace writes (defined_at): MX60.<X> = ...
rg --json '\bMX60\.([A-Z]\w*)\s*=' "$ROOT" --type js \
  | jq -s '
    map(select(.type=="match") | {
      symbol: ("MX60." + (.data.submatches[0].match.text)),
      file: .data.path.text,
      line: .data.line_number,
      kind: "defined_at"
    })
  ' > /tmp/xref-defs.json

# Stage 2 — namespace reads (used_at): MX60.<X> not followed by = or as own assignment lhs
rg --json '\bMX60\.([A-Z]\w*)\b' "$ROOT" --type js \
  | jq -s '
    map(select(.type=="match") | {
      symbol: ("MX60." + (.data.submatches[0].match.text)),
      file: .data.path.text,
      line: .data.line_number,
      kind: "used_at"
    })
  ' > /tmp/xref-reads-raw.json

# Stage 3 — subtract definitions from reads (a write site is also a read of LHS, exclude)
jq -s '
  .[0] as $defs | .[1] as $reads |
  (
    [ $defs[] | "\(.file):\(.line):\(.symbol)" ] | unique
  ) as $def_keys |
  $reads | map(select(("\(.file):\(.line):\(.symbol)" | IN($def_keys[])) | not))
' /tmp/xref-defs.json /tmp/xref-reads-raw.json > /tmp/xref-reads.json

# Stage 4 — Java FQN cross-refs (BChi* class refs in JS — REST endpoint coupling)
rg --json '\b(BChi[A-Z]\w*|Chi[A-Z]\w*Helper)\b' "$ROOT" --type js \
  | jq -s '
    map(select(.type=="match") | {
      symbol: .data.submatches[0].match.text,
      file: .data.path.text,
      line: .data.line_number,
      kind: "java_ref"
    })
  ' > /tmp/xref-java-refs.json

# Stage 5 — assemble xref.json envelope
jq -s -n --slurpfile defs /tmp/xref-defs.json \
        --slurpfile reads /tmp/xref-reads.json \
        --slurpfile java /tmp/xref-java-refs.json '
  {
    schema_version: "1.0",
    module: "mx60-chihuahua",
    generated_at: (now | todate),
    edges: (
      ($defs[0] | map(. + {usage_kind: "defines"})) +
      ($reads[0] | map(. + {usage_kind: "reads-global"})) +
      ($java[0]  | map(. + {usage_kind: "invokes-java"}))
    )
  }
' > xref.json
```

`xref.json` edge schema:

```json
{
  "symbol": "MX60.AlarmsManager",
  "file": "chihuahua-ux/src/rc/js/app/AlarmsManager.js",
  "line": 30,
  "usage_kind": "defines | reads-global | invokes-java"
}
```

Acceptance: ≥80 edges (proposal estimate 80-100). UpDetail.js ≥90% xref coverage of distinct `MX60.X` reads (R2 mitigation).

---

## D. Shard plan (locked from proposal §5)

All shard caps ≤25 entries (well under #1231 hard cap of 75). S1+S2+S6 run parallel; S3+S4+S5 parallel after backend; S7+S8 parallel after S1-S6.

| Shard | Domains | Entry-set discriminator (file glob) | Est. entries | Sub-agent prompt | Validation gate |
|-------|---------|-------------------------------------|--------------|--------------------|------------------|
| **S1: backend-rt** | service-container, equipment-backend | `chihuahua-rt/src/**/*.java` (8 files) | 10 | apply-template.md + scope-S1 | `jq -e -f scripts/validate-shard.jq shard-S1.json | jq -e 'length==0'` AND `jq '.[].kind' shard-S1.json | sort -u` returns only `["java-class"]` AND every entry has `backend` block |
| **S2: backend-ux** | http-rest, equipment-reader, alarms-backend, history-backend, schedules-backend, util-backend | `chihuahua-ux/src/**/*.java` (9 files) | 9 | apply-template.md + scope-S2 | same as S1 + rest_endpoints non-empty for BChiServlet |
| **S3: frontend-core** | app-shell, baja-integration, ui-lib | `chihuahua-ux/src/rc/js/app/{DashboardApp,Router,ConfigManager,SharedEnv,SubscriptionPool,WritePoint,Toast,Confirm,StatusResolver,Dropdown,Popover,RelativeTime,CsvExport}.js` + `lib/*.js` + `util/*.js` | ~14 | apply-template.md + scope-S3 | every entry has `frontend_iife`; `iife_pattern` ∈ enum; `globals_written` non-empty for non-entry kinds |
| **S4: frontend-equipment** | equipment-frontend, equipment-detail, threshold-stores | `chihuahua-ux/src/rc/js/app/{EquipmentData,EquipmentCard,EquipmentDetail,EquipmentSnapshotStore,HomeMap,UpDetail,CarcamoDetail,DataloggerDetail,LiveHistoryBuffer,TimeRangePicker,UpThresholdStore,ModoOverrideStore,OutputOverrideStore,CarcamoThresholdStore,DataloggerThresholdStore}.js` | ~15 | apply-template.md + scope-S4 + UpDetail special note | `frontend_iife` on all; UpDetail.js purpose ≤150 chars (HARD) |
| **S5: frontend-alarms-schedules** | alarms-frontend, schedules-frontend, history-frontend | `chihuahua-ux/src/rc/js/app/{AlarmsManager,AlarmsPage,AlarmCards,AlarmDetailsTable,AlarmDetailPage,AlarmLatchStore,AlarmModalActions,AlarmNotesModal,BulkActionBar,ScheduleView}.js` | ~12 | apply-template.md + scope-S5 | `frontend_iife` on all; subscriber_role correctly classified |
| **S6: resources-config** | module-descriptor, build-config, static-resources | `niagara-module.xml`, `module.palette` ×2, `module-permissions.xml` ×2, `module.lexicon` ×2, `*.gradle.kts` ×3, `gradle.properties`, `index.html`, `rc/img/**`, `rc/fonts/**`, `rc/ext/**` | ~25 | apply-template.md + scope-S6 (resources subset) | kinds restricted to `[config, module-descriptor, resource-image, resource-icon, compiled-bundle]`; loc=0 for binary |
| **S7: delta-vs-reflow** | cross-cutting | runs `scripts/build-delta.sh`; produces `delta.json` + `delta-vs-reflow.md` | 1 MD + 1 JSON | scripted (not freeform sub-agent) | `jq '.deltas | length' delta.json >= entry_count_mx60`; every status ∈ enum; bloque68_section populated for ≥5 rows |
| **S8: xref** | cross-cutting | runs `scripts/build-xref.sh`; produces `xref.json` + `xref.md` | ~80-100 edges | scripted | `jq '.edges | length' xref.json >= 80`; no orphan symbols (every `reads-global` has matching `defines` OR is in known-external list) |

**Parallelism model**: orchestrator launches S1+S2+S6 simultaneously (backend Java + static), then S3+S4+S5 (frontend JS, depend on confirming kind enum from S1-S2), then S7+S8 (need full index.json merged).

---

## E. Sub-agent apply prompt template

This is the highest-leverage artifact. Stored as `scripts/apply-template.md`. Every shard sub-agent receives a copy with `{{SHARD_NAME}}`, `{{SCOPE_GLOB}}`, `{{EST_ENTRIES}}`, `{{DOMAINS}}` substituted. Per #1231: ONE canonical JSON example, identical literal across siblings.

```markdown
# Apply Sub-Agent Prompt — Shard {{SHARD_NAME}}

You are the apply executor for shard `{{SHARD_NAME}}` of `mapping-mx60`.
Read-only on `/home/cristian/modulos_niagara_n4/**` (per project rules).

## Your scope

Map ONLY the files matching: `{{SCOPE_GLOB}}`
Estimated entries: ~{{EST_ENTRIES}}
Domains: {{DOMAINS}}

Source root: `/home/cristian/modulos_niagara_n4/Cliente/Honeywell/MX60/chihuahua/chihuahua/`

## Your output

A JSON array (NOT an object, just `[...]`) of entry objects, written to:
`docs/mappings/mx60-chihuahua/shards/shard-{{SHARD_NAME}}.json`

## Canonical entry example (LITERAL — match this shape EXACTLY)

```json
{
  "id": "chihuahua-ux/src/rc/js/app/AlarmsManager.js",
  "path": "chihuahua-ux/src/rc/js/app/AlarmsManager.js",
  "kind": "iife-app",
  "domain": "alarms-frontend",
  "purpose": "MX60 alarms page orchestrator: renders cards/details, drives ack flow via inline modal, polls via SubscriptionPool.",
  "dependencies": ["MX60.SubscriptionPool", "MX60.ConfigManager", "MX60.AlarmCards", "MX60.AlarmModalActions"],
  "loc": 410,
  "status": "source",
  "source_doc": {"file": "HANDOFF.md", "section": "AlarmsManager"},
  "verified_at": null,
  "frontend_iife": {
    "namespace": "MX60",
    "globals_written": ["MX60.AlarmsManager"],
    "globals_read": ["MX60.SubscriptionPool", "MX60.ConfigManager", "MX60.AlarmCards", "MX60.AlarmModalActions"],
    "iife_pattern": "iife-window",
    "load_order_hint": 22,
    "subscriber_role": "consumer"
  }
}
```

For Java entries, use this canonical example:

```json
{
  "id": "chihuahua-rt/src/com/angeles/chihuahua/components/BChiDashboardService.java",
  "path": "chihuahua-rt/src/com/angeles/chihuahua/components/BChiDashboardService.java",
  "kind": "java-class",
  "domain": "service-container",
  "purpose": "Root BAbstractService for MX60: orchestrates 6 plantas, controlTick at 10s, lock pool for write coordination.",
  "dependencies": ["BPlanta", "BAbstractService", "BIService"],
  "loc": 380,
  "status": "source",
  "source_doc": {"file": "HANDOFF.md", "section": "ChiDashboardService"},
  "verified_at": null,
  "backend": {
    "profile": "rt",
    "package": "com.angeles.chihuahua.components",
    "bcomponent_type": "BAbstractService",
    "slots": 12,
    "actions": ["clearCache", "refreshAll"],
    "rest_endpoints": [],
    "box_methods": [],
    "decompiled": false
  }
}
```

## Prohibited fields (MUST NOT appear in any entry)

NEVER emit any of these top-level fields:
- `from`
- `caller`
- `file` (use `id`/`path` instead)
- `callers`
- `used_by`
- `edges` (those go in xref.json, NOT here)
- `source_path` (use `id`)
- `name` (the `id` carries the identity)

NEVER emit `frontend_vue` (no Vue in MX60).
NEVER emit `frontend_js` for entries with `iife-*` kind (use `frontend_iife`).

## Kind decision tree (apply this in order)

1. Java file → `java-class`
2. Path under `rc/js/util/` → `iife-util`
3. Path under `rc/js/lib/` → `iife-lib`
4. Filename ends `Store.js` → `iife-store`
5. File matches reusable utility name (SubscriptionPool, WritePoint, Toast, Confirm, StatusResolver, Dropdown, Popover, RelativeTime, CsvExport, BulkActionBar, AlarmCards, AlarmDetailsTable, AlarmDetailPage, AlarmModalActions, AlarmNotesModal, EquipmentCard, EquipmentData, LiveHistoryBuffer, TimeRangePicker) → `iife-lib`
6. File is the bootstrap entry → `iife-entry`
7. Otherwise (under `rc/js/app/`) → `iife-app`
8. Path under `rc/ext/` → `compiled-bundle`
9. XML/.kts/.lexicon/.palette → `config` or `module-descriptor`
10. Image/font binary → `resource-image` or `resource-icon`

## frontend_iife block rules

- `namespace`: always `"MX60"`
- `globals_written`: list ALL `MX60.X = ...` assignments at IIFE top-level (rg `MX60\.\w+\s*=`)
- `globals_read`: list ALL `MX60.X` reads NOT in globals_written (deduped)
- `iife_pattern`: pick ONE of `iife-window`, `iife-self`, `iife-named`, `iife-other`
- `load_order_hint`: integer position from `index.html` `<script>` tags, or `null` if not script-loaded
- `subscriber_role`: `consumer` if file calls `SubscriptionPool.subscribe`, `producer` if it publishes, `none` otherwise

## Runtime claims policy (MANDATORY per #1238)

If you describe runtime behavior in `purpose` or `notes` (latency, timing, _bajaSetBroken state, controlTick interval observed), you MUST append `(inferred from mapping)` to the claim. Do NOT assert empirical runtime values that have not been measured. Source-declared values (e.g. `Clock.SECOND * 10` for tick interval) are factual and need no annotation.

## Validation BEFORE returning

1. Save your shard JSON to `docs/mappings/mx60-chihuahua/shards/shard-{{SHARD_NAME}}.json`
2. Run: `jq -e -f docs/mappings/mx60-chihuahua/scripts/validate-shard.jq docs/mappings/mx60-chihuahua/shards/shard-{{SHARD_NAME}}.json`
3. If it returns non-empty (errors), fix and re-run until empty.
4. Only return `status: done` after the validator passes.

## Citations

Every entry's `purpose` MUST be derivable from a file:line you can cite. Use `source_doc.file` to record the synthesis doc you read (HANDOFF.md, PORT-CHECKLIST.md, openspec/explore.md, or `null` if derived from direct file read alone).

## Language

Spanish (Rioplatense voseo) where natural in the `purpose` and `notes` fields, English for technical identifiers. No emojis. WHY behind decisions in long-form prose belongs in `domains/*.md`, NOT here.
```

---

## F. Delta-vs-reflow methodology (operational)

Step-by-step for shard S7. Implements proposal D4 dual-form.

### F.1 Inputs
- `index.json` (MX60, post-merge from S1-S6)
- `../reflow-clean-177/index.json` (frozen reference)
- MX60 source tree (for rg port-marker scan)

### F.2 Heuristics (locked)

| Status | Detection rule | Threshold |
|--------|----------------|-----------|
| **HEREDADO** | rg port-marker hit (`Ported from Snls\|Port of \|Ported verbatim\|Adapted from SnLs`) AND name match in reflow AND `\|loc_delta_pct\| ≤ 15` | ±15% LOC |
| **REESCRITO** | (rg port-marker hit AND `\|loc_delta_pct\| > 30`) OR (name match AND `\|loc_delta_pct\| > 30`) | >30% LOC |
| **ANÁLOGO** | name match (or purpose-keyword match) AND `15 < \|loc_delta_pct\| ≤ 30` AND no port-marker | 15-30% LOC |
| **NUEVO** | MX60 entry has no name analog in reflow AND name does not appear in reflow index `id` substring scan | — |
| **FALTA** | Reflow entry has no name analog in MX60 (only for `kind` ∈ `[java-class, vue-*, js-*]`) | — |

### F.3 Operational sequence

1. `scripts/build-delta.sh` runs (see C4) — produces draft `delta.json`.
2. Human spot-check pass (≥10 rows, all §68.1-§68.5 prescriptions): populate `bloque68_section` and adjust `status` if heuristic was wrong. Documented in `_validation.md`.
3. `scripts/build-delta-md.sh` renders `delta-vs-reflow.md` from `delta.json`.
4. Bloque #68 evidence index: jq query `jq '.deltas[] | select(.bloque68_section != null) | {section: .bloque68_section, status, mx60_id, reflow_id, evidence}'` → appended to `delta-vs-reflow.md` as a separate table.

### F.4 Acceptance gates

- Every MX60 entry appears in `delta.json` (1:1 with `index.json` `entries[]`).
- ≥5 rows have non-null `bloque68_section` (covers §68.1, §68.2, §68.3, §68.4, §68.5).
- Status enum strictly enforced (jq validator: `select(.deltas[].status | IN("HEREDADO","REESCRITO","FALTA","NUEVO","ANÁLOGO") | not)` returns empty).

---

## G. Domain document 5-section template

All `domains/*.md` files follow this exact structure (mirrors reflow `domains/backend.md`).

### Section structure (mandatory)

```markdown
# <Domain Name> Domain — mx60-chihuahua

**Profile coverage**: rt (N) + ux (N) = N classes / files
**Module**: chihuahua
**Source**: `<relative paths>`

---

## 1. Overview

One-paragraph prose (~10-15 lines) describing: what this domain does, where it lives in the
architecture, what it depends on, what depends on it, key cross-cutting concerns. Spanish
Rioplatense.

## 2. Entry Points

Table of files that bootstrap or anchor this domain in the runtime / app shell.

| File | Role | LOC | Notes |
|------|------|----:|-------|

## 3. Components / Classes

Tables (subsectioned by sub-domain) listing all entries.

### 3.1 <Sub-domain>
| Path | bcomponent_type or kind | slots / globals | actions / methods | LOC | Notes |
|------|--------------------------|-----------------|-------------------|----:|-------|

## 4. Data Flow / Integration Points

Prose + diagrams (text/ASCII OK) showing: REST endpoints exposed, BOX commands consumed,
SubscriptionPool subscriptions opened, namespace dependencies (`MX60.X` reads/writes),
threshold store access patterns. Cross-references to other domains.

## 5. Notes & Cross-References

- Bloque #68 §X.Y: <which transplant decision touches this domain>
- Delta vs reflow: <NUEVO/HEREDADO/REESCRITO summary>
- Inferred runtime claims (per #1238): list with explicit `(inferred)` tags
- Open questions / TODOs for future SDDs
```

### Style enforcement

- Length: ~5-30 KB per domain file (matches reflow range).
- Tables: rendered via jq from `index.json` slice for that domain (consistency with master `index.md`).
- File:line citations on key prose claims.
- No empirical runtime assertions without `(inferred from mapping)` tag.

### Template compliance gate

Apply phase verifies: each `domains/*.md` has all five top-level `## ` headers in order: `1. Overview`, `2. Entry Points`, `3. Components / Classes`, `4. Data Flow / Integration Points`, `5. Notes & Cross-References`.

```bash
# scripts/check-domain-template.sh
for F in domains/*.md; do
  HDRS=$(rg --no-line-number '^## \d+\. ' "$F" | head -5)
  EXPECTED="## 1. Overview"$'\n'"## 2. Entry Points"$'\n'"## 3. Components / Classes"$'\n'"## 4. Data Flow / Integration Points"$'\n'"## 5. Notes & Cross-References"
  [ "$HDRS" = "$EXPECTED" ] || { echo "FAIL: $F"; exit 1; }
done
echo "All domain files conform."
```

---

## H. Validation gates

Locked inventory. Each gate is a runnable command. Apply phase MUST pass these in order.

### H.1 Per-shard (after each apply sub-agent returns)

| Gate | Command | Failure mode |
|------|---------|--------------|
| schema-validator | `jq -e -f scripts/validate-shard.jq shards/shard-Sx.json | jq -e 'length==0'` | Reject and ask sub-agent to fix |
| no-prohibited-fields | (built into validate-shard.jq) | Same |
| size-cap | `jq 'length' shards/shard-Sx.json` ≤ 75 | Reject (split shard) |

### H.2 Per-domain (after each `domains/*.md` written)

| Gate | Command | Failure mode |
|------|---------|--------------|
| 5-section template | `scripts/check-domain-template.sh domains/<d>.md` | Reject and re-author |
| jq-rendered tables consistent | `diff <(jq render) domains/<d>.md sectioned tables` (manual review) | Re-render |

### H.3 Pre-merge (before assembling final `index.json`)

| Gate | Command | Threshold |
|------|---------|-----------|
| total entry count | `jq -s '[.[].[]] | length' shards/*.json` | ≥100, ≤130 |
| kind distribution | `jq -s '[.[].[].kind] | group_by(.) | map({kind:.[0],n:length})' shards/*.json` | iife-* present, java-class present, no `js-store` (all migrated to `iife-store`) |
| prohibited-fields scan | `jq -s '[.[].[] | keys[]] | unique | map(select(IN("from","caller","file","callers","used_by","edges","source_path","name")))' shards/*.json` | empty |
| id uniqueness | `jq -s '[.[].[].id] | (length) - (unique | length)' shards/*.json` | `0` |

### H.4 Post-merge (final assembly check)

| Gate | Command | Threshold |
|------|---------|-----------|
| coverage % | `scripts/coverage.sh index.json` | ≥95% |
| envelope shape | `jq -e 'has("schema_version") and has("module") and has("extensions") and has("entries") and has("exclusions")' index.json` | true |
| `verified_at` sample | `jq '[.entries[] | select(.verified_at != null)] | length' index.json` | ≥40 (REQ-7 spot-check) |
| `source_doc` presence | `jq '[.entries[] | select(.source_doc != null)] | length' index.json` | ≥80% of entries |
| fidelity sample (manual) | spot-check 40 entries vs source | ≥90% accurate |
| delta entry count | `jq '.deltas | length' delta.json` | == `index.json entries` count + reflow-FALTA count |
| xref edge count | `jq '.edges | length' xref.json` | ≥80 |
| UpDetail xref coverage (R2) | rg distinct `MX60.X` reads in UpDetail.js vs xref edges from UpDetail.js | ≥90% |

---

## Risks/mitigations carried into apply

| Risk | Severity | Mitigation in apply |
|------|----------|---------------------|
| **R-D1: schema kind enum drift** — sub-agents could invent a 6th `iife-*` value | MEDIUM | C1 validator rejects unknown kinds; apply-template.md decision tree enumerates 5 values exhaustively |
| **R-D2: globals_read false positives** — `MX60.X` regex catches comments/strings | LOW | rg `--type js` already excludes comments via JS-aware tokenizer; spot-check 5 random entries during S8 |
| **R-D3: HEREDADO/REESCRITO misclassification** — name-match heuristic mis-pairs unrelated files | MEDIUM | F.3 step 2 mandates human spot-check of all REESCRITO + ≥10 ANÁLOGO rows; documented in `_validation.md` |

---

## skill_resolution

- injected (compact rules from #1231 multi-shard sub-agent + #309 read-only/Rioplatense + #1238 inferred-from-mapping flagging applied throughout — see C1 validator's prohibited-fields rule, E template's runtime-claims policy, G template's `(inferred)` requirement)
