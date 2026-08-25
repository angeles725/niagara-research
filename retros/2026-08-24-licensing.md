<!-- kit-retro -->
<!-- review-status: pending -->
# Retro — niagara-research · licensing (document-mode §20 consolidation) · 2026-08-24 · Research-SDD self-retrospective

> Run reviewed: `licensing` documentation run — document-mode §20 consolidating the dispersed licensing thread
> (nre.jar / njre.dll / nre.dll / dsfspi.dll; authority-boundary blocks B442/B443/F-analysis).
> Trigger: USER-requested mid-run kit-quality retro + document-mode consolidation (NOT a focus-completion/STOP
> terminal — see delta D2, which is precisely about that missing auto-trigger).
> Method: a FRESH-CONTEXT agent read the current kit (`PROMPT-LOOP.md` + `METHODOLOGY.md` §5/§6/§8/§18/§20/§21,
> `toolbelt/tool-registry.md`, `corroborate_ghidra.py` / `CuratedEvidenceExporter.java`) FIRST, deduped against
> the current kit and the two prior native-RE retros, then proposes kit deltas. READ-ONLY on the kit — this
> report only PROPOSES; kit changes are human-reviewed and human-committed (METHODOLOGY §18).

## Run summary

A document-mode (§20 CAPTURE) run consolidating licensing knowledge scattered across earlier sessions. Two
signals of kit interest surfaced:

1. **A named-authority jar was never opened.** Block B442 explicitly NAMED `com.tridium.niagarad.license.*`
   living in `bin/ext/nre.jar`, and B443/F-analysis named `com.tridium.nre.subscription.*` as the
   subscription/entitlement authority boundary — yet the corpus never decompiled `nre.jar`, leaving the
   local-vs-server / entitlement picture as avoidable `[INFER]` across MULTIPLE sessions. This session finally
   decompiled it with the existing `decompile-java.sh` (Vineflower) — the source was reachable the whole time.

2. **A curated-evidence wrapper capped out; the raw decompile route was the pivot.** `decompile-native.sh
   ghidra-evidence` failed (exit 2, "evidence file cap exceeded") on all three symbol-rich licensing DLLs; the
   working pivot was the raw `ExportDecompiledC.java` `--script` route (symbols present → succeeded: njre 8
   fns, nre 1 fn). `corroborate-native.sh` (native-static.v1) succeeded on all three and was run BEFORE citing
   — RE corroboration-before-citation discipline followed correctly.

The RE discipline was clean. The two gaps worth kit attention are epistemic/process (D1, D2) plus one small
tool-registry clarification (D3). A correction to the record: the candidate's "default `max_files=8`" is
misattributed — see D3.

## Proposed kit deltas

| # | Proposed change | Target (file · §/section) | Evidence | Type | Priority |
|---|---|---|---|---|---|
| D1 | **NAME-THE-JAR ⇒ OPEN-THE-JAR.** When a block cites SPECIFIC classes in a REACHABLE named jar/binary as the AUTHORITY or BOUNDARY of a finding (e.g. "entitlement logic lives in `com.tridium.nre.subscription.*` in `nre.jar`"), that citation is a SELF-GENERATED decompile-me gap — REGISTER it in the backlog, do not leave the named source as an `[INFER]` boundary. The SOURCE-BEFORE-AGENT gate blocks UNreachable sources; this is its positive twin: a reachable-but-unopened authority source names the exact next gap. | `PROMPT-LOOP.md` HARD RULES (beside SOURCE BEFORE AGENT, ~L656) + tie-in `METHODOLOGY.md §13` (backlog derivation) | B442 named `com.tridium.niagarad.license.*` in `nre.jar`; B443/F-analysis named `com.tridium.nre.subscription.*` as the authority boundary — jar left un-decompiled across multiple sessions as `[INFER]`; opened this session with `decompile-java.sh` | new | HIGH |
| D2 | **A toolbelt WRAPPER failure / config-cap wall is a §18 capture signal.** Extend §18 "When it fires" so a typed §21 wall from a toolbelt wrapper (`blocked-on-tool`/`unavailable`, or a config-cap exit like ghidra-evidence's file cap) records a NARROW tooling-only retro note AT THE MOMENT of the wall — especially in runs with NO focus-completion/STOP terminal (document mode §20, one-off consolidations), where the current triggers never fire and the kit-signal is otherwise lost until a user asks. Not a full fresh-context retro per failure (that would spam and duplicate the focus-close net); a one-line pending tooling-delta captured while the failure is live. | `METHODOLOGY.md §18` ("When it fires", L1765) + cross-ref `§21` (the wall is already typed; §21 does not route to §18 today) | THIS session: `ghidra-evidence` exit 2 on nre/njre/dsfspi, §21 fallback correctly walked to ExportDecompiledC, but the retro fired ONLY when the user asked several turns later — document-mode run has no focus/STOP trigger | new | MEDIUM |
| D3 | **Clarify what `ghidra-evidence` IS and correct its cap in the registry.** `decompile-native.sh ghidra-evidence` (`corroborate-ghidra.py` / ghidra-corroboration.v1) is a BOUNDED STATIC program-model evidence exporter — `CuratedEvidenceExporter.java` declares "No decompilation or target execution"; it is NOT a decompiler and does NOT emit decompiled bodies. Its `--max-files` default is **64** (min 12), NOT 8 (8 is `corroborate-native.py`'s default). On a symbol-RICH binary that exceeds the curated cap, the decompiled-C tool is the raw `ExportDecompiledC.java` `--script` route — add a one-line "when the curated route caps out on a large/symbol-rich binary, use ExportDecompiledC" pointer. | `toolbelt/tool-registry.md` ("Ghidra batch routes", ~L103–139) | This session: ghidra-evidence "evidence file cap exceeded" on 3 DLLs → pivot to ExportDecompiledC succeeded; verified `corroborate_ghidra.py:223` default=64/min12 vs `corroborate_native.py:144` default=8; `CuratedEvidenceExporter.java:39` "No decompilation" | refinement | LOW |

For each delta, one line of rationale (WHY · cost · impact):

- **D1** — The highest-value lesson. The kit fights avoidable `[INFER]`, yet nothing tells a researcher that
  citing a named jar AS the boundary of a finding creates an obligation to open it. That omission cost the
  licensing thread its entitlement picture across multiple sessions. Cost: one HARD-RULE line. Impact: converts
  every "the logic lives in `<jar>`" citation into a registered decompile-me gap instead of a permanent `[INFER]`.
- **D2** — A real hole for terminal-less runs. Focus-completion already catches mid-run tool gaps for NORMAL
  runs (platform-native PN-C proved it — DecompileByString was captured at focus-close). The genuine gap is
  document-mode §20 / one-off runs that never reach a §18 trigger. Cost: one clause in "When it fires" + a
  §21→§18 cross-ref. Impact: a wrapper deficiency becomes a proposed toolbelt delta when it happens, not never.
- **D3** — Small but corrects an active misconception (ghidra-evidence read as a decompiler) and a wrong cap
  number, and names the right pivot. Cost: two registry lines. Impact: the next native run reaches for
  ExportDecompiledC directly instead of fighting the curated cap.

## Already covered (dedupe — proof the retro read the kit first)

- **RE corroboration-before-citation** (corroborate-native.sh run on all three DLLs before any `[CERT]` cite) →
  already `METHODOLOGY.md §5` (decompiler-dump offset provenance / twin-binary check) + `tool-registry.md`
  (native-static.v1 corroboration route). Clean instance, no new delta.
- **Heavy parallel-mapper fan-out for the consolidation** → already `PROMPT-LOOP.md` DOCUMENT CYCLE large-scale
  per-section-agent pattern + `METHODOLOGY.md §16` (driver owns shared writes, agents return cited findings).
- **ExportDecompiledC as the raw `--script` decompile route** and its **stripped-binary limitation** →
  already `tool-registry.md` ("Ghidra batch routes" + "Stripped-binary limitation" + PN-C from the 2026-08-07
  platform-native retro). NOTE: this session is symbol-RICH (ExportDecompiledC SUCCEEDED because symbols
  present) — the OPPOSITE failure mode from PN-C's stripped case, so D3 is not a PN-C duplicate but is a small
  sibling clarification.
- **SOURCE-BEFORE-AGENT don't-launch-at-unreachable-source gate** → already `PROMPT-LOOP.md` HARD RULES + BOOTSTRAP e2.
  D1 is its POSITIVE converse (open the reachable authority jar), which the gate does not state.
- **Walking the §21 fallback chain on a native wall + typed blocked-on-tool** → already `METHODOLOGY.md §21`.
  D2 does not duplicate §21 (which handles the WORKAROUND); it adds the §18 kit-LEARNING capture §21 omits.
- **SCOPING JUDGMENTS / GAP PREMISES ARE HYPOTHESES** → already `PROMPT-LOOP.md` INVESTIGATE step 3 / BOOTSTRAP e.
  Adjacent to D1 but distinct: those test a prior NEGATIVE judgment; D1 acts on a positive authority citation.

## Anti-patterns observed / target-doc observations

- **Candidate C is a TARGET-doc staleness, NOT a kit delta (honest call).** The project SessionStart protocol
  hook cites `organized/` at `/home/cristian/modules/Prototipos/modulos/organized`, which does not exist; the
  live corpus is `/home/cristian/niagara-research/organized/`. This is a broken path in the target's own
  `.claude/hooks/` config, fixable in the target — the kit already tells BOOTSTRAP c to adapt the hook with
  real source paths. There is a THIN general lesson ("re-verify SessionStart-hook paths still resolve over
  time"), but it is too slight to earn a kit rule and is arguably already implied by SOURCE-BEFORE-AGENT's
  proposal-existence extension. NOT proposed as a delta — flagged here so the target maintainer fixes the hook path.
- A named authority jar (`nre.jar`) sat un-decompiled as `[INFER]` across multiple sessions → the delta that
  prevents recurrence: **D1**.
- A curated wrapper's file-cap wall did not self-surface as a kit signal until the user asked → **D2**.

## Tools built, adapted, or outgrown

| # | CREATED (path · purpose) | ADAPTED (kit tool · what it could not express) | OUTGREW | ORACLE | VERDICT (decision · evidence) |
|---|---|---|---|---|---|
| T1 | — | `decompile-native.sh ghidra` via `ExportDecompiledC.java` (raw `--script` route) · used AS-IS on symbol-rich DLLs when `ghidra-evidence` capped out — no adaptation needed, just the right route choice | `decompile-native.sh ghidra-evidence` (curated static-model exporter) · capped out on symbol-rich DLLs; not the tool for decompiled bodies | — | `keep-kit` · both routes are kit tools; the lesson is ROUTING (D3), not a new tool. No script was written this run. |
| T2 | — | `decompile-java.sh` (Vineflower) · used AS-IS to finally open `nre.jar` | — | — | `keep-kit` · existing wrapper sufficient; the gap was PROCESS (D1 name-the-jar), not tooling. |

## Metrics

- **Blocks reviewed**: licensing document-mode §20 consolidation (authority-boundary blocks B442/B443/F-analysis;
  `nre.jar` decompiled this session; exact this-session block IDs not enumerated to the retro agent)  ·
  **§14 cross-block corrections in this run**: 0 (document-mode CAPTURE, not discovery)  ·
  **Rules skipped in practice**: 0 kit-rule violations — the RE discipline (corroborate-before-cite, §21 fallback) was followed
- **Deltas proposed (new)**: 2 new (D1 HIGH, D2 MED) + 1 refinement (D3 LOW)  ·  **Already-covered lessons**: 6
- **Candidate-vs-fact correction**: Candidate A's "default `max_files=8`" is misattributed — verified
  `corroborate_ghidra.py` default `--max-files=64` (min 12); `8` belongs to `corroborate_native.py` (native-static.v1).

## Honest verdict

This run surfaced one genuinely load-bearing delta (D1 name-the-jar ⇒ open-the-jar), one real process gap for
terminal-less runs (D2), and one small tool-registry clarification (D3) that also corrects a factual
misattribution in the candidate evidence. Candidate C is correctly NOT a kit delta (target-doc staleness). The
RE work itself was textbook kit discipline — corroboration before citation, §21 fallback walked correctly,
existing wrappers reused without hand-rolling — which is why the "Already covered" list is substantial and the
new-delta list is short and specific, as a healthy §18 retro should be.
