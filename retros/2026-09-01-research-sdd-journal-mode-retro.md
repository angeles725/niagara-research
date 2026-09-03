<!-- review-status: pending -->
<!-- Marker lifecycle: the maintainer flips 'pending' above to 'applied <date> · kit <sha>' once this retro's proposed deltas are reviewed and applied (or 'dismissed') in the kit; sweep-retros.sh reads this marker to report which retros are still open (METHODOLOGY §18). -->
# Retro — niagara-research · research-sdd journal-mode (kit improvement) · 2026-09-01 · Research-SDD self-retrospective

> Run reviewed: NOT a focus/block run — a module-hardening + control-simulation session (ColdRoomPan-rt).
> Trigger: kit-fitness signal (an applied-engineering session produced NO block-shaped output; its process
> was captured ad-hoc in a client bitácora). Method: proposes ONE kit delta (a new "journal mode") plus the
> tools this session built. READ-ONLY on the kit — PROPOSES only; kit changes are human-reviewed (§18).

## Proposed kit deltas

> Only genuinely NEW items — anything the kit already encodes is under "Already covered".

| # | Proposed change | Target (file · §/section) | Evidence (block / commit / § / transcript ref) | Type | Priority |
|---|---|---|---|---|---|
| J1 | Add a "journal/diary mode" sibling to block mode: a lightweight PROCESS log for applied-engineering sessions (refactor / build / deploy / simulate) that produce no verified-knowledge block. Own folder (`journal/` or `bitacora/`), fixed structure (orientation · source-verification · changes · build&verify · closing), written WHILE working, OUTSIDE block numbering and `CATALOG.md`. | new `templates/journal-entry.md` | This session (ColdRoomPan-rt hardening + simulator) had no block-shaped output; process was captured ad-hoc in `Cliente/Leon-Guanjuato/bitacora/2026-08-31-coldroompan-rt-hardening.md` | new | MEDIUM |
| J2 | METHODOLOGY note distinguishing **modo bloque** (verified reusable knowledge → corpus) from **modo diario** (process log → journal), so applied work stops being forced into a block or lost between commits. | `METHODOLOGY.md` new § "modo diario vs modo bloque" | same | new | LOW |

For each delta above, one line of rationale:

- **J1** — WHY: applied-engineering sessions are real research-SDD work but have no home; the process (decisions, evidence, build gates) evaporates into commits/memory. COST: one template + a folder convention. IMPACT: auditable "how we got here" trace, separate from the knowledge corpus.
- **J2** — WHY: without the distinction, the operator/agent either shoehorns process into a block (noise in the corpus) or drops it. COST: a short doc section. IMPACT: clean separation of knowledge vs process.

## Already covered (dedupe — proof the retro read the kit first)

- Evidence markers ([CERT]/[INFER]/…) → already covered by `METHODOLOGY.md §3`; journal mode REUSES them, does not redefine.
- Propose-never-apply discipline for kit changes → already covered by `METHODOLOGY.md §18`; journal mode's closing section follows it.
- 3-source verification before asserting → already covered by the PROTOCOL; journal mode's "source-verification" section is that same discipline applied to an engineering task.
- Retro staging mechanism → already covered by `toolbelt/stage-retro.sh` + `templates/retro.template.md`; this proposal is being routed through exactly that.

## Anti-patterns observed (optional)

- A multi-step module-hardening session left its process only in a client-local bitácora + engram memory, not in any kit-visible artifact → the delta that would prevent it: J1.

## Tools built, adapted, or outgrown

| # | CREATED (path · purpose) | ADAPTED (kit tool · what the kit version could not express) | OUTGREW (kit tool · why stopped) | ORACLE (tool · what it SEEs, not recomputes) | VERDICT (decision · evidence) |
|---|---|---|---|---|---|
| T1 | `scratchpad/coldroom-sim.html` · interactive simulator of ColdRoomPan control logic (4 rooms: staging, valve→evap delay, defrost interlock, alarms, HOA override) | — | — | `coldroom-sim.html` · steps + renders the control logic exactly as the operator experiences it on the panel; surfaces staging/interlock/override behavior a static code reading misses | `keep-local` · target-specific to ColdRoomPan, but the pattern (a per-target control oracle) is a promote candidate if generalized |
| T2 | `scratchpad/coldroom-model.js` + `coldroom-tests.js` · pure control model + headless Node harness (76 assertions), run against the model AND against the JS extracted from the published artifact | — | — | — | `keep-local` · reusable PATTERN: extract Niagara control logic to a pure (no-Baja) model, unit-test standalone (niagaraTest does not run in WSL) — mirrors the build-n4 kit seed #5 |

## Metrics

- **Blocks reviewed**: 0 (not a focus run)  ·  **§14 cross-block corrections in this run**: 0  ·  **Rules skipped in practice**: n/a
- **Deltas proposed (new)**: 2  ·  **Already-covered lessons**: 4

## Honest verdict

This was NOT a research-sdd focus/block run, so most of the kit's block machinery does not apply — and that IS the finding. The one genuinely new thing for the kit is **journal mode** (J1/J2): a home for applied-engineering sessions that produce no block. Everything else this session relied on (evidence markers, propose-never-apply, 3-source verification, the retro-staging tool) the kit already covers. If the maintainer judges journal mode out of scope for research-sdd, the honest fallback is: dismiss J1/J2 and keep such process logs as client-local bitácoras (as done here).
