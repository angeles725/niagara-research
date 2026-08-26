# §18 Retro — focus: signing-pki (native HostId surface + toolkit closure B532–B535) — 2026-08-26

<!-- review-status: pending -->

> Self-retrospective (METHODOLOGY §18). Proposes kit deltas only; does NOT edit the kit.
> Evidence references: blocks B532–B535, commits 3d78a2e…b041540, RESEARCH-STATE-signing-pki.md
> (added SP-G11 blocked), codegen/spg10-frida/README.md + build-all.sh + bin/*.jar.

---

## Run summary

- **Focus:** signing-pki — final stretch after the two prior retros closed B518–B524 and B525–B531.
  This segment produced B532 (licensing watch-map), B533 (persistent mirror vector), B534 (HostId mirror
  executed → honest negative), B535 (native fold re-anchored + hook point), the SP-G11 future gap, and the
  reproducible toolkit (README + build-all.sh + 3 compiled JARs).
- **Session:** operator-driven Q&A thread (mirror → HostId → which-process → clone → persistence → "did you
  do commit/push/engram/retros"), all answered with live/static evidence instead of prose.
- **Notable:** the HostId turn produced a genuine negative result that CORRECTED the prior block's model
  (B532 said "one Java method = HostId gate"; B534 proved the moved-file gate is native). Also: the build
  procedure existed only as narrative in blocks — Engram carried NO procedure memory until this segment.

---

## Proposed kit deltas

| # | Title | Evidence | Priority | Kit file / section |
|---|---|---|---|---|
| D1 | A NEGATIVE dynamic result is a first-class finding, not a "failed" probe, and it must §14-CORRECT the model it refutes in the SAME segment. B534 executed the "full mirror" and got an honest negative (wrong-host license still `moved file` under a Java-only rewrite) — which then §14-corrected B532's "one Java method = HostId gate". The kit's dynamic-phase rules reward positive reproduction but have no explicit "negative → correction" pairing step; the correction only happened because the operator kept asking. | B532 §5 ("mirror point") vs B534 §3 ("moved file is native"); the correction was operator-prompted. | **MEDIUM** | `METHODOLOGY.md` §12 / §14: when a requires-execution step returns a NEGATIVE, the pairing rule should be "record the negative AND check whether any prior block asserted the positive; if so, §14 the prior block in the same pass." |
| D2 | Reusable build artifacts (compiled JARs + build script + README) must be COMMITTED adjacent to their source, not left in scratch, and the HOW-TO must be mirrored to Engram as a PROCEDURE observation — a corpus narrative mention is not recall-findable. This segment found the repo had source `.java` but no JARs/build/how-to in Engram; the operator's "¿lo tenemos?" surfaced it. | codegen/spg10-frida/ (source only before `b041540`); Engram search "compile javac asm-9.6 jar" returned 0 before obs 7391. | **MEDIUM** | `METHODOLOGY.md` §19 (`codegen/` holds PoC source AND build output) + §20 (document-mode Engram mirror): extend to "a §19 deliverable's build procedure is itself a documented artifact — commit the build script + compiled output and mirror the procedure to Engram, not only the findings." |
| D3 | When a WALL is hit, the fallback should read the TWO-BLOCK chain, not just the launcher: B533's persistence answer (`station.java.options`) and B535's fold re-anchor both came from reading *adjacent* files (`nre.properties`, `nre.dll` exports) AFTER two prior tool-walls. The kit's §21.2 fallback chains are per-artifact-class; there's no "also read the target's own config surface (properties/launcher/help) before escalating tooling" rung. | B533 (nre.properties:46) + B535 (r2 over nre.dll) both unblocked without new tools. | **LOW** | `METHODOLOGY.md` §21.2: add a general rung "the target's OWN config/help surface (launcher `-help`, `.properties`, exported symbols) is an instrument; exhaust it before provisioning". |
| D4 | A deliberately-deferred dual-use step (HostId clone / native force) needs a typed GAP, not just a prose deferral — otherwise it vanishes from the backlog. SP-G11 was queued only after the operator asked; the closing message had the procedure but no state row until then. | SP-G11 row added to RESEARCH-STATE (blocked, needs second machine + scope decision) after operator "dejalo pendiente". | **LOW** | `METHODOLOGY.md` §8: a `refused`/deferred-by-policy step must be re-typed into the backlog (as `blocked (requires-…)`) in the same pass, with its `tried:`/`needs:` clauses, exactly like a tool wall. |

---

## Reinforced observations (already in kit — not new deltas)

| Obs | Kit coverage | Notes |
|---|---|---|
| Reversibility held through every live fixture | §12 backup + independent oracle + byte-identical restore | B534 wrong-host license restored `sha256 == 4a799453`; live PIDs unchanged. |
| TOOL-BEFORE-AGENT / fallback recorded honestly | §21.2 ghidra→r2 | B535: ghidra UNUSABLE → r2 rung; rung recorded, no ghidra-grade claim. |
| One-block-per-commit + push after each | PROMPT-LOOP LOOP CONTINUATION | B532–B535 each its own commit; HEAD == origin/main throughout. |
| Marker discipline on mixed native+dynamic | §3 markers | B534/B535 tally `[CERT]`/`[CERT-live]` explicit, no unmarked. |

---

## Open gaps at close

- **SP-G11** (blocked) — second-machine field test (mirror A/B + HostId clone C), queued by operator; artifacts ready.
- **SP-G3a / SP-G4 / SP-G9b** (blocked-on-artifact) — unchanged.
- Corpus-wide archive debt (16 stale `covered_blocks`, 2 UPPERCASE backlogs) — still pending separate reconciliation (from the prior retro's D3).
