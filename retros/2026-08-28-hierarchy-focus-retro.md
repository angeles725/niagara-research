<!-- review-status: pending -->
<!-- kit-retro: include -->

# §18 Retrospective — hierarchy focus (2026-08-28)

**Run**: niagara-research, focus `hierarchy`, 2026-08-28
**Blocks written**: B584–B590 (7 investigable gaps H1–H7)
**Coverage**: 7/7 investigable gaps closed; 0 requires-execution
**Driver**: self-paced /research-sdd (Opus), AUDIT-FIRST seed delegated to sonnet, verified inline

---

## Summary

A textbook focus: a genuinely-unopened engine, a rich official doc section used for the first time, seven blocks
that each closed a real mechanic and corroborated against the guide where possible. The prior-coverage
reconciliation (the delta proposed in the template retro) worked cleanly — the sweep confirmed B5/B565/B387 are
shallow BEFORE any block, so nothing re-derived them. One kit observation, small.

---

## Delta proposals

### D1 — a rich official-doc section should trigger a SOURCES.md batch registration up front (NEW, MEDIUM)

**What happened.** This focus made the FIRST corpus use of `niagara-help/guides-clean/Hierarchies/` — a complete
32-file official guide section (AboutLevelDefinitions, CachingHierarchies, HierarchyScopes, ContextParameters,
Permissions, per-type LevelDef guides). Several blocks cited it as `[CERT-doc]` (B584 AboutLevelDefinitions, B585
CachingHierarchies, B586 HierarchyScopes, B588 ContextParameters, B589 Permissions). But the citations were made
ad-hoc per block; the 32-file section was not registered in `sources/SOURCES.md` as a batch at focus bootstrap.
Per METHODOLOGY §5, every cited external artifact should be in SOURCES.md (path · type · origin · date · sha256 ·
citing blocks) — this focus cited ~5 of the 32 and the registration is now scattered/incomplete.

**Proposed delta.** When an AUDIT-FIRST sweep discovers a DEDICATED official doc section (a `guides-clean/<Topic>/`
directory with many files) that the corpus has never used, the focus bootstrap should register the SECTION in
`sources/SOURCES.md` once (as a directory-level source with the file count + sha256 of the manifest), and blocks
then cite specific files under it — rather than each block independently discovering it. This mirrors how the
`tags` focus (B260–B270) first registered the official Tridium doc as a batch. A one-line §5 rule: "a
newly-used official doc SECTION is registered at focus bootstrap, not per-block."

---

## What went well (keep)

- **Prior-coverage reconciliation (template-retro D1) worked.** The sweep's STEP 0 confirmed B5 §5.3.3 is a
  ~1-page overview and B565 is the role seam only, framing all 7 gaps as "the engine B5 never opened". Zero
  re-derivation of B5.
- **Doc corroboration raised confidence on the model blocks.** B584 (the level-def taxonomy) matched the official
  `AboutLevelDefinitions` guide almost verbatim — a `[CERT]` + `[CERT-doc]` pairing that is much stronger than
  decompile-only.
- **The engine turned out to be genuinely coherent** — level defs → cache/on-demand (unified by contextParams) →
  parallel scopes → permission-baked tree → ORD scheme → dual transport. Each block tied to the next; the closing
  synthesis (B590 §590.4) reads as one system, not seven disconnected findings. That coherence is a sign the gap
  decomposition (from the sweep) was right.

---

## Child gaps surfaced (named, out of scope)

- `hierarchy-ux` / `hierarchy-wb` UI (~12 classes: BHierarchySpaceTypeExt, BIHierarchyChartFactory, menu agents) —
  out of the engine angle; a low-priority UI tail.
- `HierarchyServlet` — the thin web servlet behind the BOX `load` command; low value.
- `BHierarchyTags` — the tag property bag on BHierarchy/BGroupLevelDef; unclassified, low value.
- `fw(501, "hierarchy.limit")` — the opaque capacity-limit fw protocol (shared across the service tier); a
  cross-cut into the `fw(...)` framework-internal call convention, not hierarchy-specific.
