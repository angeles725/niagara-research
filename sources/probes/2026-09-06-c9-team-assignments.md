# Campaign 9 — team assignments (lead: investigador, 2026-09-06)

Fallback channel: cross-session delivery to offline machines is queuing/dropping; this file is the source of truth.
Pull niagara-research and read your section. Peer messages in English (Cristian's standing rule). Reply to
`investigador` with branch/tip/SHA per item. @viewer is directed by Cristian directly — not assigned here.

## State
- Campaign 8 CLOSED: niagara-tools main `1109c0f` (close `c0447c2`), tag `v0.19.0`, bats 338/338, `C8_CLOSE=1` gate
  12/12, 21 retros folded. SDD change archived at `openspec/changes/archive/2026-09-05-build-n4-module-campaign8/`.
- Campaign 9 OPEN: SDD change `build-n4-module-campaign9` (hybrid store; explore running, propose next).
  Waves (Cristian-confirmed): W1 = S20 CompPan time-slice rotation (FIRST client PR) + S7 + S18-lint kit lints;
  W2 = S12-A write-server + S12-B servlet config step-up + unified audit (URGENT; no JSON credential store;
  audit-append failure never fails the write); W3 = S18/S13 alarm PoC + S19 lint + S5-cont matrix W14-W22 + K22/§5.
  Setpoint retype REJECTED. airDefrost rooms 1/2/4 = STATION-ONLY trial, no module change.
- Divergence (viewer echo): tunnel main moved to `9acb47c` — best-effort Supabase `public.change_log` row per
  successful /write (user_email/user_id/room/slot/label/old_value/new_value/area/ok), JWT-bearer only, no step-up.
  S12-A RED `qa/c9-s12-write-server` `24adcba` sits on `e4b42b0` (1 file
  `instalacion/pipeline/test/write-server.config-login.test.mjs`, cases S12A-1..7, expects a `buildServer(cfg,deps)`
  seam guarding main()). Audit-sink reconciliation is OPEN for the C9 design phase.

## QA (VERIFY authority) — test-first, qa/* RED branches, named mutations, exact-count pins, bite table per branch
0. Post-hoc C8 confirmation: pull main, `C8_CLOSE=1 bats tests/c8-close.bats` → 12/12; `git tag --points-at c0447c2`
   → v0.19.0. Flag anything off (fix-forward).
1. S20 rotation RED (client repo niagara-panccadia-leon, re-check main tip; seed = campaign9-research-candidates.md
   S20): swap after `rotationInterval` for the least-hours idle compressor; no swap below interval;
   `rotationInterval=0` → step output byte-identical golden; make-before-break vs `stageDelay`; no swap with one
   available; HOA OFF excluded / HAND untouched; no stage-up on dischargeHigh; LP floor; hours ledger unaffected.
2. S18/S13 alarm PoC RED (client): BAlarmSourceExt per B827 on CR-3 freeze + CP-1 low-suction, edge-triggered only
   (fires once on normal→offnormal, toNormal on recovery, started() re-seed) + schema-risk SAFE pin.
3. S12-A re-pin prep (tunnel): draft change_log-sink variants of S12A-4/S12A-6 rebased on 9acb47c; HOLD the push
   until design names the sink. S12A-1/2/3/5/7 stay valid.
4. `tests/c9-close.bats` skeleton (`C9_CLOSE=1`) mirroring c8-close.bats, tool-pin list TODO-per-PR.

## investigador1 (Opus research + second reads) — push to niagara-research, [CERT]/[INFER], one `[ev:]` per paragraph
1. C9 propose input: one-page evidence map per slice (S20, S12-A, S12-B, S7, S18-lint, S19, S18/S13, S5-cont,
   K22/§5): citation (block or RED branch+tip), the one sentence apply must build, seam, schema-risk expectation.
   Upgrade S12-A to [CERT] from the viewer echo above (base e4b42b0, tunnel main 9acb47c, sink reconciliation open).
2. S20 design evidence: CompressorControl.java at the re-anchored client tip — insertion point in `step` after the
   target computation, `cmdSince[]` as the per-compressor clock, make-before-break sequencing against
   stageDelay/minOn/minOff, and the "0 = disabled byte-identical" proof strategy (golden step output).
3. S12 audit-sink reconciliation brief: change_log schema vs Cristian's ask (who/what/old→new/when + explicit
   logout) vs the B829 station-side trail; recommend the single sink + what step-up/logout adds; servlet (S12-B)
   writes the same schema.
4. Second reads on every C9 fold/doc draft companero produces.

## companero (research-sdd lanes, apply-ready drafts, tools) — push to niagara-research, send SHAs
1. S20 apply-package (FIRST client PR): `rotationInterval` (BRelTime, SUMMARY|OPERATOR, facets MIN, 0 = disabled =
   today byte-identical) + `rotationMode` (enum, make-before-break default) per the kit slot-type doctrine; exact
   insertion point in `CompressorControl.step`; gating (minOn/minOff/stageDelay/HOA OFF excluded/HAND untouched/no
   stage-up on dischargeHigh/LP floor/no swap with one available); hours ledger unaffected; schema-risk SAFE
   expectation; FASE 1/2/3 explainer for the client. Re-anchor the client tip before citing.
2. Defrost trial link-list (rooms 1/2/4) with bog-nav: per-room, per-unit exact links (evapOut OR resistanceOut →
   fan relay, AND fan-mode≠OFF, hasDefrost=true, fallback=false) from the Cuarto3 pattern. Station-only. Ready for
   Cristian's green light.
3. S12 plan update (`2026-09-06-c9-s12-config-login-audit-plan.md`): fold the 9acb47c change_log sink; single-sink
   recommendation; step-up/logout delta; S12-B same schema. investigador1 writes the brief, you own the plan doc.
4. Drafting checklist additions from the C8 close: `toolbelt/facets-lint.sh` does not exist (facets =
   `verify-module.sh --src facets-req`); `wave3.md` is an openspec file, not a kit doc (kit-links L1 scans bare
   `X.md` tokens in root kit docs). Keep improving bog-nav/module-find as needs appear.
