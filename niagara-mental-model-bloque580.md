# Block 580 — The template manifest schema: `<template>` + ten child arrays — the declarative index of a `.ntpl`, where `settings`/`links`/`bindings` are typed `Value` entries (num/bool/str/cfg/in/out/px) with req + slotPath + min/max/units — the full grammar behind B200's binding contract

**Session**: 2026-08-28
**Focus**: `template` (gap T3 — the full `template-manifest.xml` grammar; deepens [Block 200 §200.3])
**Distribution**: Honeywell OptimizerSupervisor-N4.14.0.162
**Method**: READ-ONLY, inline bounded read of `TemplateManifest` (the bean) + `ManifestXMLReader` (the parser);
field list and XML tag/attr set token-verified inline.
**Primary sources** `[CERT]`:
- `organized/template/template-rt/vineflower/com/tridium/template/manifest/{TemplateManifest,ManifestXMLReader,
  ManifestXMLWriter,TemplateFileSpec}.java`.

**Scope**: the on-disk manifest that indexes a template. [Block 200 §200.3] covered the binding CONTRACT
(`BConfigBinding`/`BPasswordBinding`); T3 gives the full serialized SCHEMA that carries those bindings plus every
other template element. Does NOT re-open the `.ntpl` zip structure ([Block 200 §200.1]) — this is the manifest
INSIDE it.

---

## 580.1 The manifest bean = root metadata + ten arrays [CERT]

`TemplateManifest` `[CERT] :10-31` is a plain-Java bean (no `B`-type) — root metadata plus ten typed arrays:

**Root `<template>` attributes** `[CERT] :11-21`: `version` (default "1.0"), `vendor`, `description`, `info`,
`title`, `uID` (`BUuid`), `buildVersion`, `bogSignature` (the drift signature, [Block 200 §200.4]), `state`
(`BTemplateState`), `isApplication`, `isStation`.

**Ten child arrays** `[CERT] :22-30`:

| Element | Array type | Meaning |
|---|---|---|
| `<settings>` | `Value[]` | parameterized config values (the knobs a deployer fills) |
| `<links>` | `Value[]` | link specs (in/out wiring) |
| `<bindings>` | `Value[]` | the config/password bindings ([Block 200 §200.3]) |
| `<resources>` | `Resource[]` | referenced files (px/images/data) with location/source ords |
| `<subtemplates>` | `Subtemplate[]` | nested child templates (T4) |
| `<tags>` | `Tag[]` | tags to apply |
| `<dependencies>` | `BDependency[]` | module dependencies (checked by [Block 578] T2) |
| `<revisions>` | `Revision[]` | revision history |
| `<optionals>` | `BOrd[]` | optional components (kept/removed by T2/T5) |

## 580.2 The `Value` is a typed, constrained parameter [CERT]

`settings`, `links`, and `bindings` all serialize as `TemplateManifest.Value` entries. The XML attribute set
(from `ManifestXMLReader`) `[CERT]`: `n` (name), `typ` (type), `req` (required flag), `slotPath`, `v` (value),
`min`, `max`, `units`, plus `i`/`d` (info/description). The `typ` vocabulary is fixed `[CERT] :32-38`:
`num` (NUMERIC), `bool` (BOOLEAN), `str` (STRING), `cfg` (CONFIG), `in` (INPUT), `out` (OUTPUT), `px` (PX). So a
template parameter is **typed** (one of seven), **optionally required** (`req`), **slot-addressed** (`slotPath`),
and — for numerics — **range/unit-constrained** (`min`/`max`/`units`). This is the schema a deploy-time form is
generated from: the manifest declares "this template needs a `num` setpoint in [min,max] units °F at slotPath
…", and the deployer supplies `v`.

## 580.3 Root and resource attributes [CERT]

`ManifestXMLReader` `[CERT]` reads the `<template>` root attributes `id`/`title`/`version`/`vendor`/
`description`/`info`/`buildVersion`/`signature`/`state`/`isApplication`/`isStation`, and per-resource
`locationOrd`/`sourceOrd`/`ntplFileOrd`/`ord`/`type` for `<resources>`/`<subtemplates>`. The reader is XParser-
based; `ManifestXMLWriter` is the symmetric serializer. Only one example manifest exists in the official docs
(`guides-clean/Templates/ExampleTemplate.manifest*`), so this bean + reader ARE the authoritative grammar
(the API/format is code-only — [Block 200]-audit: `guide-search` returned 0 for the programmatic surface).

## 580.4 Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|-------|--------|----------|---------|
| 1 | TemplateManifest = plain bean: root metadata (version/vendor/title/uID/buildVersion/bogSignature/state/isApplication/isStation) + 10 arrays | [CERT] | TemplateManifest.java:10-31 | token-checked ✓ |
| 2 | Ten arrays: settings/links/bindings (Value), resources, subtemplates, tags, dependencies (BDependency), revisions, optionals (BOrd) | [CERT] | :22-30 | token-checked ✓ |
| 3 | Value attrs n/typ/req/slotPath/v/min/max/units (from ManifestXMLReader) | [CERT] | ManifestXMLReader.java (attr set) | token-checked ✓ |
| 4 | typ vocabulary fixed: num/bool/str/cfg/in/out/px | [CERT] | TemplateManifest.java:32-38 | token-checked ✓ |
| 5 | Reader/Writer XParser-based; manifest is the authoritative grammar (format code-only, 1 example doc) | [CERT] | ManifestXMLReader/Writer.java + [B200 audit] | token+logic ✓ |

**Marker tally**: [CERT] ×5 · [INFER] ×0. Block TYPE = EVIDENCE (decompilation). 5 of 5 rows token-verified
inline.

## Connections

- **[Block 200 §200.3]** — the binding CONTRACT (BConfigBinding/BPasswordBinding); T3 gives the manifest schema
  that serializes them as `<bindings>` Value entries.
- **[Block 577]** (T1) — `NewTemplateSource.save` builds this manifest (`collectTemplateInfo`/`addFilesToManifest`).
- **[Block 578]** (T2) — reads `<dependencies>` (module check) and `<optionals>` (keep/remove).
- **[Block 579]** (T5) — uses `optional` for the upgrade optional-component diff.
- **T4** (this focus) — the `<subtemplates>` array is the composition mechanism.

## Open gaps (this block)

- The `Resource` and `Subtemplate` sub-bean field detail (exact resource-ref resolution) is named — `Resource`
  folds into [Block 200 §200.1] (.ntpl entries), `Subtemplate` into T4. Focus continues at T4 (subtemplate
  composition).
