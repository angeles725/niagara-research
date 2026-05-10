#!/usr/bin/env jq -f
# validate-shard.jq — canonical per-shard schema validator for mx60-chihuahua
# Usage: jq -f scripts/validate-shard.jq shards/s1-backend-rt.json
# Output: empty on success; error messages on violations
#
# Covers ALL constraints per design §C1:
#   - core fields non-null
#   - kind/status enums (no vue-component, vue-view, js-store, js-mixin, js-plugin, js-router)
#   - purpose ≤150 chars
#   - id == path
#   - prohibited fields (from, caller, file, callers, used_by, edges, source_path, name)
#   - frontend_vue forbidden
#   - frontend_js + iife-* mutually exclusive
#   - frontend_iife required for iife-* kinds
#   - backend required for java-class kind
#   - iife_pattern enum: iife-window | iife-self | iife-named | iife-other
#   - subscriber_role enum: consumer | producer | none

def allowed_kinds:
  ["java-class", "config", "resource", "resource-image", "module-descriptor",
   "compiled-bundle", "iife-app", "iife-store", "iife-lib", "iife-util", "iife-entry"];

def allowed_statuses:
  ["source", "resource", "excluded", "generated"];

def allowed_iife_patterns:
  ["iife-window", "iife-self", "iife-named", "iife-other"];

def allowed_subscriber_roles:
  ["consumer", "producer", "none"];

def prohibited_fields:
  ["from", "caller", "file", "callers", "used_by", "edges", "source_path", "name"];

def is_iife_kind:
  . as $k | ["iife-app", "iife-store", "iife-lib", "iife-util", "iife-entry"] | contains([$k]);

# Validate a single entry, emit error strings
def validate_entry:
  . as $e |
  [
    # 1. Core fields non-null
    (if $e.id == null or $e.id == "" then "[\($e.id // "UNKNOWN")] id is null/empty" else empty end),
    (if $e.path == null or $e.path == "" then "[\($e.id // "UNKNOWN")] path is null/empty" else empty end),
    (if $e.kind == null then "[\($e.id)] kind is null" else empty end),
    (if $e.domain == null or $e.domain == "" then "[\($e.id)] domain is null/empty" else empty end),
    (if $e.purpose == null or $e.purpose == "" then "[\($e.id)] purpose is null/empty" else empty end),
    (if $e.loc == null then "[\($e.id)] loc is null" else empty end),
    (if $e.status == null then "[\($e.id)] status is null" else empty end),

    # 2. id == path
    (if $e.id != $e.path then "[\($e.id)] id != path (\($e.path))" else empty end),

    # 3. kind enum
    (if $e.kind != null and ([$e.kind] | inside(allowed_kinds) | not) then
       "[\($e.id)] invalid kind: \($e.kind)" else empty end),

    # 4. Forbidden kinds (reflow-only kinds must not appear)
    (if $e.kind != null and ($e.kind | IN("vue-component","vue-view","js-store","js-mixin","js-plugin","js-router")) then
       "[\($e.id)] forbidden kind (reflow-only): \($e.kind)" else empty end),

    # 5. status enum
    (if $e.status != null and ([$e.status] | inside(allowed_statuses) | not) then
       "[\($e.id)] invalid status: \($e.status)" else empty end),

    # 6. purpose ≤150 chars
    (if $e.purpose != null and ($e.purpose | length) > 150 then
       "[\($e.id)] purpose exceeds 150 chars (\($e.purpose | length) chars)" else empty end),

    # 7. Prohibited fields
    (prohibited_fields[] as $pf |
       if $e | has($pf) then "[\($e.id)] prohibited field present: \($pf)" else empty end),

    # 8. frontend_vue FORBIDDEN
    (if $e | has("frontend_vue") then "[\($e.id)] frontend_vue is FORBIDDEN in MX60" else empty end),

    # 9. frontend_js + iife-* mutually exclusive
    (if ($e.kind | is_iife_kind) and ($e | has("frontend_js")) and ($e.frontend_js != null) then
       "[\($e.id)] frontend_js must be null for iife-* kinds" else empty end),

    # 10. frontend_iife REQUIRED for iife-* kinds
    (if ($e.kind | is_iife_kind) and ($e.frontend_iife == null) then
       "[\($e.id)] frontend_iife required for kind \($e.kind) but is null" else empty end),

    # 11. backend REQUIRED for java-class
    (if $e.kind == "java-class" and $e.backend == null then
       "[\($e.id)] backend required for java-class but is null" else empty end),

    # 12. iife_pattern enum
    (if $e.frontend_iife != null and $e.frontend_iife.iife_pattern != null then
       if ([$e.frontend_iife.iife_pattern] | inside(allowed_iife_patterns) | not) then
         "[\($e.id)] invalid iife_pattern: \($e.frontend_iife.iife_pattern)"
       else empty end
     else empty end),

    # 13. subscriber_role enum
    (if $e.frontend_iife != null and $e.frontend_iife.subscriber_role != null then
       if ([$e.frontend_iife.subscriber_role] | inside(allowed_subscriber_roles) | not) then
         "[\($e.id)] invalid subscriber_role: \($e.frontend_iife.subscriber_role)"
       else empty end
     else empty end),

    # 14. dependencies key MUST be present (may be empty array)
    (if $e | has("dependencies") | not then
       "[\($e.id)] missing mandatory core field: dependencies" else empty end),

    # 15. backend.profile non-null and valid enum for java-class
    (if $e.kind == "java-class" and $e.backend != null then
       if $e.backend.profile == null then
         "[\($e.id)] backend.profile is null for java-class (must be rt|ux)"
       elif ([$e.backend.profile] | inside(["rt","ux"]) | not) then
         "[\($e.id)] backend.profile invalid value: \($e.backend.profile) (must be rt|ux)"
       else empty end
     else empty end),

    # 16. No top-level profile or decompiled (extension-block leak)
    (if $e | has("profile") then
       "[\($e.id)] top-level 'profile' field is forbidden (must be inside backend block)" else empty end),
    (if $e | has("decompiled") then
       "[\($e.id)] top-level 'decompiled' field is forbidden (must be inside backend block)" else empty end)
  ];

# Check #17: verified_at count threshold — only meaningful on index.json (has "entries" key)
def check_verified_at_count:
  if type == "object" and has("entries") then
    ([.entries[] | select(.verified_at != null)] | length) as $verified |
    if $verified < 40 then
      "VIOLATION: verified_at count \($verified) < 40 (REQ-9 requires ≥40 spot-checked entries)"
    else empty end
  else empty end;

# Main: handle both shard format (top-level array) and index.json format (.entries)
if type == "array" then
  [.[] | validate_entry] | flatten | .[]
elif type == "object" and has("entries") then
  ([.entries[] | validate_entry] | flatten | .[]),
  check_verified_at_count
elif type == "object" and has("id") then
  # Single entry
  [validate_entry] | flatten | .[]
else
  "ERROR: unrecognized input shape (expected array, {entries:[]}, or single entry object)"
end
