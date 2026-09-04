# RESEARCH-STATE — focus: module-authoring (the remaining module-authoring axes — versioning/upgrade-safety, station integration, tags/data-exposure, lexicon/i18n/doc — + a consolidated actionable audit of our modules)

> Multi-focus corpus (METHODOLOGY §16). Focus **BOOTSTRAPEADO 2026-09-04** (operator picked
> "versioning/upgrade-safety", then "ve por todos" → cover ALL remaining module-authoring axes). Completes the
> module-authoring body alongside the RT campaign (B729-B750), organization (B749/B750), and WB/UX (B751-B753).
> NOT the framework overviews (B12/B20/B25/B5/B634/module-anatomy = REMITTANCE) — the concrete AUTHORING
> recipes + a safe/unsafe matrix + an actionable audit of ColdRoomPan/CompPan/DashboardPan. Code-grounded;
> synthesis blocks carry expected [INFER] with every FACT cited.

<!-- research-state.v1 -->
schema: research-state.v1
covered_blocks: 757
gaps_closed: 8
known_gaps: 8
investigable_open: 0
requires_execution_open: 0
blocked_open: 0
deferred_open: 0
undocumented_findings: 0
block_scope: shared-global
<!-- /research-state.v1 -->

focus: module-authoring
status: stopped (8/8 investigable closed incl. bonus bits block; 6 Explore sweeps synthesized into B754-B760; next free block B761)
bootstrapped_on: 2026-09-04
block_prefix: niagara-mental-model-bloqueN.md (numeración global; próximo libre: B761)

## Coverage

- **Covered blocks**: 757 corpus-wide (this focus: B754-B760) (shared-global)
- **Coverage metric**: 8 / 8 gaps closed
- **Deliverable**: B754 (versioning+matrix), B755 (bits), B756 (build/signing), B757 (integration), B758 (tags/exposure), B759 (lexicon/doc), B760 (consolidated audit) — feeds build-n4-module kit

## Gap-backlog (prioritized)

| Priority | Gap | Type | Status |
|---|---|---|---|
| high | MA1 — Module VERSIONING + upgrade mechanics: vendorVersion, BModule/manifest, dependency version checks, the Migration Framework, station upgrade flow | code | closed (B754 §754.1-4: NModule/Version, ModuleManager.resolve hard-fail, NO per-module migration hook, offline migration-rt) |
| high | MA2 — SAVED-DATA survival matrix: what schema changes are safe/unsafe over an existing .bog | code | closed (B754 §754.5-6: warningAndSkip vs unwrapped throw; SAFE add/reorder, LOSSY remove/rename, OUTAGE simple-retype + enum-tag remove) |
| high | MA3 — The BUILD + version-targeting + SIGNING toolchain | code | closed (B756: vendor stamp, target-lowest rule, plugin family, .jar vs .dist, project-CA + STORED repack) |
| medium | MA4 — Station INTEGRATION: BAbstractService + nav tree | code | closed (B757: register-by-placement, Sys.getService first-registered, BComponent is a BINavNode for free, virtual-node recipe) |
| medium | MA5 — TAGS/relations authoring + northbound DATA EXPOSURE | code | closed (B758: BSmartTagDictionary seed, BTagRule auto-tag, BCustomRelation, BObixAgent, Fox/BOX/QueryServlet, BQL-cursor) |
| medium | MA6 — LEXICON/i18n + the -doc/help profile authoring | code | closed (B759: module.lexicon key=type/slot module-global, toFriendly fallback, locale conventions, -doc profile; CompPan-rt lexicon EMPTY) |
| high | MA7 — CONSOLIDATED actionable audit of ColdRoomPan/CompPan/DashboardPan | synthesis+deliverable | closed (B760: what's already correct + 8-item ranked punch-list + versioning discipline + deploy sequence) |
| bonus | MA8 — the BIT models a module author works with (Flags/BStatus/BPermissions/BVersion), exact values | code | closed (B755 — added on operator mid-run request "checa los bits"; exact constants Flags.java:23-44, BStatus:46-53, BPermissions:26-31, BVersion:58-66) |

`tried:` (none blocked — all source is existing corpus blocks + real jars + our module repos; SOURCE-BEFORE-AGENT passes).

## Remittance (framework FACTS these build on — cited, not re-derived)

- Build/manifest/lifecycle: **B12**, module-anatomy **B629-636**. Services/app: **B20**. Migration/help/bajadoc:
  **B25**. ORD/BOG/BQL/tags/hierarchy: **B5**. Palette/nav reader: **B634**. Signing: **signing-pki / B725**.
  Schema-safety failures: **B739** (retype), **B740** (enum). oBIX exposure: **B727**/**B728**. Doc-profile
  exemplar: **B116** (docHoneywellSpyder). RT + WB/UX authoring: **B729-B753**.

## Iteration history

| # | Date | Gap closed | Block | Delegated? · model tier | New gaps uncovered |
|---|---|---|---|---|---|
| — | 2026-09-04 | (bootstrap — remaining module-authoring axes) | — | no · inline | MA1-7 seeded; 6 Explore sweeps launched |
| 1 | 2026-09-04 | MA1 + MA2 (versioning + saved-data matrix) | B754 | yes · 2× Explore (BModule mechanics + .bog matrix) | B754-G1/G2 |
| 2 | 2026-09-04 | MA8 (bits — operator mid-run request) | B755 | no · inline (exact constants grep) | B755-G1 (facet bits) |
| 3 | 2026-09-04 | MA3 (build/version-targeting/signing) | B756 | yes · Explore (our build files + devguide) | B756-G1 (plugin bytecode absent) |
| 4 | 2026-09-04 | MA4 (services + nav) | B757 | yes · Explore | B757-G1 |
| 5 | 2026-09-04 | MA5 (tags/relations + data exposure) | B758 | yes · Explore | B758-G1/G2 |
| 6 | 2026-09-04 | MA6 (lexicon/i18n/doc) | B759 | yes · Explore | B759-G1 |
| 7 | 2026-09-04 | MA7 (consolidated audit) | B760 | no · inline (synthesis of B729-B759 × our modules) | B760-G1 (implementation tasks) |

## Blocked gaps (each tagged with what it needs)

(none — all synthesis over existing corpus + jars + our repos.)

## Stop control (primary = read-only-investigable exhaustion, METHODOLOGY §8)

- **Open gaps — read-only investigable**: 0 (STOP — MA1-MA8 all closed)
- **Open gaps — requires-execution**: the B760 punch-list items (implementation, tracked in B742) + B754-G2/B759-G1
- **Open gaps — blocked**: 0
- Budget cap: none

## Dismissed file types

- (to be filled by the coverage pass.)
