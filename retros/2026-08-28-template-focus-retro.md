<!-- review-status: pending -->
<!-- kit-retro: include -->

# §18 Retrospective — template focus (2026-08-28)

**Run**: niagara-research, focus `template`, 2026-08-28
**Blocks written**: B577–B583 (7 investigable gaps T1–T7)
**Coverage**: 7/7 investigable gaps closed; 0 requires-execution
**Driver**: self-paced /research-sdd (Opus), AUDIT-FIRST seed delegated to sonnet, verified inline

---

## Summary

The cleanest run of the session, and the one where the retro-loop paid off: the AUDIT-FIRST sweep was launched
with the counting/scoping discipline proposed in the access-control + provisioning retros, and it **self-corrected
its own counts** (api 10 vs "27", wb 44 vs "65") and cited a concrete file per pattern. It also caught the single
most important framing fact — that `template` is NOT a virgin subsystem but a DEEPENING of the existing breadth
block **[Block 200]** — before any block was written, which kept the whole focus from re-deriving B200. One new
kit observation.

---

## Delta proposals

### D1 — the "is this really a NEW subsystem, or a deepening of an existing block?" check should be a FIRST-CLASS sweep output (NEW, HIGH)

**What happened.** The `template` focus was proposed (by me) as a "candidate NEW focus — module the corpus never
opened". The sweep immediately found **[Block 200]** already covers the `.ntpl` structure, BTemplateConfig, the
binding contract, the 3 types, the make/install/upgrade overview, the signature, the channel existence,
templateBulk, and the wb UI — a large REMITTANCE surface. The focus was correctly re-scoped as the DEEPENING of
what B200's header explicitly named out-of-scope (`api/impl`, `ApplicationTemplateInstaller`, `Mark/DeployToComp`).
This worked, but it worked because the sweep prompt happened to list B200 as a REMITTANCE candidate. If it hadn't,
the run could have re-derived a 160-line existing block.

**Proposed delta.** The AUDIT-FIRST sweep (METHODOLOGY §13) should make "prior-coverage reconciliation" an
explicit, REQUIRED first step and output section: before proposing gaps, grep the corpus for the
subsystem/module name and report the single most-comprehensive existing block, then frame every gap RELATIVE to
it ("B200 covers X breadth; this gap opens the Y internal B200 named out-of-scope"). The `platform-native` retro
(2026-08-07) already named this pattern as "PRIOR-COVERAGE → REMIT → DEEPEN"; this run is a second confirmation
that it should be codified into the sweep contract, not left to prompt luck.

---

## What went well (keep)

- **The counting discipline from the prior retros WORKED.** The sweep rendered every count with its package/dir
  scope, marked estimates, and self-corrected (api 10+17 not 27; wb 44 not 65). Zero count errors propagated into
  a block this run — a direct, measurable improvement over the 3 count errors of the access-control + provisioning
  run. This is evidence the D1 deltas from those retros are worth adopting.
- **Re-scoping to a deepening was announced, not silent.** The focus's RESEARCH-STATE and every block header state
  "B200 named this out-of-scope; T-n opens it" — the relationship to the breadth block is explicit and citable.
- **Each block closed a genuinely-excluded internal** (the api strategy layer, the installer, the transfer
  machinery, the manifest grammar, subtemplate cascade, the channel wire, the resolution layer) and tied back to
  B200's overview section it deepens.

---

## Child gaps surfaced (named, out of scope)

- `template-wb ui/` (44 classes, Workbench UI: BTemplateManager/BTemplateBogEditor/BTemplatePxEditor) — out of the
  engine angle; a low-priority tail focus if ever wanted.
- `com.tridium.sys.transfer` (`Mark`/`DeployToComp`/`ReplacingContext`) as a general framework primitive — used by
  templates here but a station-transfer subsystem in its own right; candidate cross-cut.
- `easyBinding` (119 classes, B200 X6) — still unopened, its own subsystem.
