# B760 · Consolidated actionable audit — ColdRoomPan / CompPan / DashboardPan against the whole authoring corpus (B729-B759), the sequenced punch-list

> **Scope**: the payoff of the module-authoring campaign — one sequenced, deploy-safe punch-list of exactly what
> to fix in our three modules, each item tied to the block that grounds it and tagged by risk/impact. This
> consolidates the RT campaign (B729-B750), WB/UX (B751-B753), the bits (B755), versioning (B754/B756),
> integration (B757), tags/exposure (B758), and lexicon/doc (B759) into action. Foco: **module-authoring** (MA7).
>
> **Sources**: FUENTE 1 — all of B729-B759 (cited per item), plus the prior audits B731/B742/B750/B753. DESIGN
> block — [INFER] recommendations, each grounded in a [CERT] block; our-module state facts are [CERT] from the
> sweeps.

---

## 760.1 — What is already CORRECT (do not touch) `[CERT]`
- **Lifecycle/engine safety**: timers arm in `started()`+`atSteadyState()` (fixed, B729); engine-thread
  discipline, degrade-honestly, catch(Throwable) in our handlers (B730).
- **WB layer**: our components sit at **rung 0** — no Manager/View/FieldEditor needed, and correctly ship none
  (B751/B753). Do NOT build wb.
- **UX layer**: DashboardPan is a servlet-SPA with REAL `OPERATOR_WRITE` fail-closed RBAC + CSRF + audit —
  the STRONGEST in the census, stronger than the vendor bajaux SPAs (B752/B753). Keep it.
- **Cross-module safety**: HOA is a plain `double` (not a shared enum) — correctly avoids the `Missing class`
  outage (B740).
- **Service/nav**: `BDashboardService` + `BRoomPanel` integrate via the framework defaults — minimal-correct
  (B757).

## 760.2 — The punch-list (ranked by impact ÷ risk, all deploy-safe) `[INFER, grounded per row]`

| # | Action | Module(s) | Grounds | Risk | Payoff |
|---|---|---|---|---|---|
| **1** | **Fill `CompPan-rt/module.lexicon`** (it is EMPTY → every compressor type/slot shows raw camelCase in the HMI/Workbench) | CompPan | B759 §759.5 | trivial (resource) | immediate legibility |
| **2** | **Curate SUMMARY pins** — mark only real I/O `SUMMARY`, HIDDEN internal timer actions, non-summary for interim state | all rt | B735/B747/B755 | trivial (flags) | declutters wire sheet + link picker |
| **3** | **Add units/precision facets** to every temp/pressure/percent slot | all rt | B735/B745 | low (facets) | live pin values render "-18.0 °C" with status color |
| **4** | **Compose `BEvaporatorUnit`'s 25 flat slots into domain children** (`timing/outputs/hoa/freeze`) + a typed folder for the evaporators | ColdRoomPan | B737/B749 P2/B750 | MEDIUM (new child types, link migration) | the biggest "no desborda" win |
| **5** | **Separate config (frozen child) from live-state** on the evaporator/compressor | ColdRoomPan/CompPan | B749 P3/B750 | medium | property sheet reads clean; HMI binds cleanly |
| **6** | **Ship pre-wired palette assembly templates** (`ColdRoom_2Evaps_Defrost`, a compressor rack) with flags baked in | ColdRoomPan/CompPan | B746/B749 P6/B750 | low (resource) | commissioning = drag-one-thing (avoids the hasDefrost trap) |
| **7** | **Give each block a distinct icon/SVG** | all rt | B738/B750 | low (resource) | visual recognition on the sheet |
| **8** | **Add a small tag dictionary** (namespace `angeles`, tags room/evaporator/compressor/defrost) | new -rt | B749 P9/B758 | medium (new component) | BQL discoverability; feeds a future flow view |

## 760.3 — The versioning/upgrade DISCIPLINE to bake into every future change `[CERT/INFER]`
These are not one-time items — they are the RULES for every deploy that touches a slot (B754/B756):
1. **ADD, never retype or remove** a frozen slot with saved data; a simple-slot retype whose old `v=` can't
   parse into the new primitive = a station-won't-boot OUTAGE (B754 matrix, B739).
2. **Never remove/rename an enum tag** any station saved (also an OUTAGE); only ADD tags. Avoid renumbering
   ordinals too (breaks Fox sync).
3. **Bump `vendorVersion`** (`defaultModuleVersion` in the root build.gradle.kts) on every schema change — the
   human audit trail (nothing gates on it at decode, B754/B756).
4. **Back up `config.bog` before a schema-change deploy** — survivable changes still DROP data silently (only
   a printed warning count). "It booted" ≠ "the data survived" (B754 §754.5).
5. **If a slot's shape MUST change**: ADD-new + migrate-in-`started()` + leave-old-deprecated — there is no
   decoder convert hook (B754 §754.3).
6. **Target the LOWEST station you must support** (a 4.14 JACE rejects a 4.15-stamped jar); verify with
   `--target-version`; re-sign JACE-bound jars under the project CA via STORED repack (B756).

## 760.4 — Deploy sequence `[INFER]`
Slots into the existing backlog B742 as the "authoring completeness" batch, ordered by risk:
1. Same-day, zero-risk: **#1 lexicon, #2 SUMMARY, #3 facets, #7 icons, #6 palette templates** (resources/flags).
2. Additive components: **#8 tag dictionary** (test it doesn't perturb boot).
3. The structural change **#4 composition + #5 config/state split** — ONE careful pass per module, ADD-only,
   migrate links, test lifecycle/units (B729/B743), back up the `.bog` first. Evaporator first (biggest
   sprawl), then compressor.
Everything else in the corpus is already correct (§760.1).

## 760.5 — Not worth doing now `[INFER]`
- A `-doc`/help profile (B759) — low value vs the lexicon; skip until a client asks for in-Workbench help.
- A -wb layer (B751/B753) — our components don't cross the ladder's bar; building one is over-engineering.
- oBIX server agents (B758) — our REST servlet already exposes data with stronger RBAC; add oBIX only if a
  standards-based northbound client (a BMS/Node-RED bridge) needs it.

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | Already correct: lifecycle, wb rung-0, servlet-SPA RBAC, HOA=double, service/nav | [CERT] | B729/B751/B752/B740/B757 + our source |
| 2 | CompPan-rt lexicon is EMPTY (highest-trivial-payoff fix) | [CERT] | B759 §759.5 |
| 3 | Biggest structural win = compose evaporator 25 flat slots into domain children | [CERT/INFER] | B737 §B.3 (fact); ranking [INFER] |
| 4 | Versioning discipline: add-don't-retype, never remove/rename enum tag, bump vendorVersion, backup .bog | [CERT] | B754 matrix; B739/B740; B756 |
| 5 | Sequence: resources/flags first, tag dict, then composition (ADD-only, backup first) | [INFER] | B742 backlog + risk ordering |
| 6 | Skip -doc/-wb/oBIX now (low value vs cost) | [INFER] | B759/B751/B758 |

**Tally**: 2 [CERT], 1 [CERT/INFER], 3 [INFER]. High [INFER] expected (applied plan); every FACT cites a
block. No unmarked claims.

## Connections
- The whole authoring corpus: **B729-B759** (cited per item), the prior audits **B731**/**B742**/**B750**/
  **B753**, the versioning rules **B754**/**B756**, the bits **B755**. This block is the actionable index over
  all of it; the build-n4-module kit corpus-index (proposed retro) should point new module work here.

## Open gaps
- **B760-G1**: each punch-list item's worked implementation (slotomatic diffs, palette XML, tag dictionary,
  lexicon fill) — implementation tasks (requires-execution), not research. Track in B742.
