<!-- review-status: pending -->
# Retro — niagara-research · platform-native (Ghidra-grade sub-pass) · 2026-08-07 · Research-SDD self-retrospective

> Run reviewed: `platform-native` focus, Ghidra-grade sub-pass B379–B383 (NG1–NG4 + synthesis). Trigger: focus-completion.
> The sub-pass REOPENED a focus declared static-closed on 2026-06-29 (B124–B130) to raise the EVIDENCE GRADE of
> four native binaries from strings/RTTI to decompiled function bodies.
> Method: a FRESH-CONTEXT agent read the current kit (`PROMPT-LOOP.md` + `METHODOLOGY.md` in full) FIRST, then the
> five blocks + `RESEARCH-STATE-platform-native.md` + `tools/README.md`, and proposes kit deltas. READ-ONLY on
> the kit — this report only PROPOSES; kit changes are human-reviewed and human-committed (METHODOLOGY §18).

## Run summary

The 2026-06-29 static loop closed 7/7 native gaps, but B124/B127–B130 worked at radare2/strings/RTTI grade and
even B126 read `nverify.exe` and `libciper.so` from strings/symbols alone. The sub-pass decompiled the FUNCTION
BODIES of the four load-bearing binaries and, in every case, surfaced a load-bearing security fact the
strings-grade block could not state:

- **B379 (NG1) `nverify.exe`** — 11 CLI options incl. FOUR `skip-*` bypass flags (B126 listed 5); the Tridium
  Public Key is a concrete 270-byte RSA-2048 blob pinned by raw `memcmp`. Corrects/extends B126 §126.4.
- **B380 (NG2) `njre.dll`** — PRIOR-COVERAGE CHECK caught that B125 §125.2 had ALREADY decompiled
  `buildArgs`/`createVM`/`invokeJava` → REMITTED to B125, re-scoped to the 4 uncovered functions + the FIPS-gated
  BouncyCastle provider swap + `-javaagent` license gate.
- **B381 (NG3) `plat.exe`** — B129 §129.7 had explicitly judged "decompilation not load-bearing"; that judgment
  was WRONG. LocalSystem account, `SERVICE_AUTO_START`, argv-passed passphrase, native complexity policy,
  DPAPI-no-entropy, `REG_BINARY` under HKLM were all load-bearing and only visible in the bodies.
- **B382 (NG4) `libciper.so`** — DWARF-rich QNX-ARM; upgraded B126 §126.5 symbol→body grade (Sylk masterslave
  file protocol, dual CRC-16/CRC-32, no crypto).
- **B383** — synthesis.

The run was disciplined: tiers declared (`no · inline`, security-exploitability = driver-model per MODEL TIER),
REMITTANCE used correctly, prior blocks read before sweeping. It also required a NEW tool mid-run
(`DecompileByString.java`) because the kit's `ExportDecompiledC.java` cannot select functions on a symbol-stripped
binary.

## Proposed kit deltas

| # | Proposed change | Target (file · §/section) | Evidence | Type | Priority |
|---|---|---|---|---|---|
| PN-A | Name **grade-upgrade reopen** as a distinct legitimate reopen category: a STOPPED focus may be reopened to raise the EVIDENCE GRADE of the SAME questions (strings/RTTI → decompiled bodies), not only for a new tool / new question. Its honesty discipline is PRIOR-COVERAGE → REMIT → DEEPEN so it never re-derives. | `METHODOLOGY.md §8` ("Reopening a STOPPED loop") | B379–B383 whole sub-pass; B380 REMITTANCE to B125; commit history 2026-08-07 | new | HIGH |
| PN-B | A prior block's **"we didn't go deeper because X is not load-bearing" scoping judgment is a HYPOTHESIS to test, not a closed door.** When a block records that it declined to decompile/read further for a stated reason, that reason is testable — treat it as a gap-worthy hypothesis, not a settled boundary. | `PROMPT-LOOP.md §INVESTIGATE step 3` (beside PRIOR COVERAGE CHECK); tie-in `METHODOLOGY.md §14` | B381 refutes B129 §129.7's "decompilation not load-bearing"; iteration-history it.10 | new | HIGH |
| PN-C | **Promote `DecompileByString.java`** (string-anchored Ghidra postScript) to the kit, or at minimum document `ExportDecompiledC.java`'s symbol-name-only limitation: `RSDD_FN_FILTER` filters function NAMES, so on a symbol-stripped binary (all user fns `FUN_*`) it cannot select the functions to export. String-anchoring is the fix. | `toolbelt/tool-registry.md` (ExportDecompiledC section, ~L107–117; distinct from the "Stripped-binary: debug-string recovery" NAME-recovery section) + `toolbelt/ghidra/` if promoted | B379/B380/B381 method blockquotes; `tools/README.md` DecompileByString row (created 2026-08-07) | new | HIGH |
| PN-D | Flag env-var propagation on the `decompile-native.sh --script` path as a reliability caveat (and cover it with the test suite). `RSDD_OUT` produced output on one invocation and silently none on another with the same config. | `toolbelt/tool-registry.md` (B57 arg-order reconciliation note, ~L124–130 — which already marks the `--script` path "currently untested") | this run's getter-vs-other invocation of ExportDecompiledC (B382 used it; NG1 getter run produced no output) | refinement | LOW |

Rationale (WHY it matters · cost · impact):

- **PN-A** — §8 today frames reopen around NEW work (new tool, new question, hardware bench). A grade-upgrade
  reopen asks the SAME questions at higher fidelity, and its risk is re-derivation, not scope-creep. Naming it
  (and its REMIT-don't-re-derive discipline) tells a future run this is a legitimate, honest reopen rather than
  redundant churn. Cost: one paragraph. Impact: legitimizes exactly the sub-pass that found 4 security facts.
- **PN-B** — this is the highest-value lesson of the run. The kit already treats gap PREMISES and name-implied
  KINDS as hypotheses, but nothing tells a researcher to distrust a prior block's *meta-judgment about what
  wasn't worth investigating*. That judgment cost the corpus B381's findings for six weeks. Cost: one line beside
  PRIOR COVERAGE CHECK. Impact: converts every "not load-bearing" note into a testable reopen candidate.
- **PN-C** — a concrete toolbelt gap that stalled the run until a tool was hand-written. Symbol-stripped native
  binaries (Mocana-static, packers) are common enough that the next native target will hit the same wall.
  Cost: promote one 1-file postScript + a test, or one caveat paragraph. Impact: no re-invention next time.
- **PN-D** — low-severity but real: the registry itself admits the `--script` env path is untested, and this run
  is a live data point that it can silently no-op. Cost: one caveat line (+ optionally a test). Impact: the next
  user checks for empty output instead of trusting the exit path.

## Already covered (dedupe — proof the retro read the kit first)

- **PRIOR COVERAGE CHECK before a tool sweep** (B380 read B125 before re-decompiling) → already `PROMPT-LOOP.md §INVESTIGATE step 3`.
- **REMITTANCE as a valid gap closure** (B380 remits `buildArgs`/`createVM`/`invokeJava` to B125 "no new substance") → already `PROMPT-LOOP.md step 6` / `METHODOLOGY.md §8`.
- **§14 REFUTE / correct a prior claim transparently** (B379→B126 §126.4, B380/B381 refute prior premises) → already `METHODOLOGY.md §14`. (PN-B extends the *trigger* to a scoping judgment, not the mechanism.)
- **GAP PREMISES ARE HYPOTHESES** (the reopen renamed/re-scoped premises) → already `PROMPT-LOOP.md BOOTSTRAP e`.
- **TOOL-BEFORE-AGENT** (driver ran Ghidra `analyzeHeadless` before any sweep) → already `PROMPT-LOOP.md HARD RULES`.
- **MODEL TIER: keep genuine security-exploitability reasoning inline on the driver** (recorded `no · inline`) → already `PROMPT-LOOP.md §INVESTIGATE MODEL TIER`.
- **Register a near-zero/falsified tool result so it isn't re-attempted** (FLOSS near-zero on DWARF/unobfuscated `libciper.so`, B382 tool note) → already `METHODOLOGY.md §5` (falsified artifacts) + the tool-note convention.
- **`block_scope: shared-global`** for the shared `niagara-mental-model-bloque` prefix → already `METHODOLOGY.md §7`.

## Anti-patterns observed

- B129 §129.7 recorded "decompilation would only add the exact argv-dispatch order, not load-bearing" and it was
  wrong (B381) → the delta that prevents recurrence: **PN-B**.
- `ExportDecompiledC.java`'s `RSDD_FN_FILTER` (name regex) is useless on a stripped binary whose user functions
  are all `FUN_*`; a tool had to be written mid-run → **PN-C**.
- `RSDD_OUT` silently produced no output on one ExportDecompiledC invocation while succeeding on another → **PN-D**.

## Tools built, adapted, or outgrown

| # | CREATED (path · purpose) | ADAPTED (kit tool · what it could not express) | OUTGREW | ORACLE | VERDICT (decision · evidence) |
|---|---|---|---|---|---|
| T1 | `niagara-research/tools/ghidra-scripts/DecompileByString.java` · decompiles the functions that REFERENCE a matched string — selects export targets without symbols | `toolbelt/ghidra/ExportDecompiledC.java` · filters by function NAME (`RSDD_FN_FILTER`), so cannot select functions on a symbol-stripped binary where all user fns are `FUN_*` | — | — | `promote` · generic string-anchored selection for stripped binaries; not target-specific; needs a `toolbelt/tests/` companion (→ PN-C) |
| T2 | — | `FLOSS (flare-floss)` · used on `libciper.so`; near-zero over plain `strings`+DWARF on an unobfuscated, debug-carrying library | FLOSS on DWARF-rich unobfuscated targets · adds nothing | — | `no` (for this target class) · record the near-zero result so a future DWARF pass does not re-attempt it (B382 tool note) |

## Metrics

- **Blocks reviewed**: 5 (B379–B383) · **§14 cross-block corrections/extensions in this run**: 4 (B379→B126 §126.4; B380 remit+refute B125-premise; B381 refute B129 §129.7; B382 upgrade B126 §126.5) · **Rules skipped in practice**: 0 (the run was kit-disciplined)
- **Deltas proposed (new)**: 3 (PN-A, PN-B, PN-C) + 1 refinement (PN-D) · **Already-covered lessons**: 8

## Honest verdict

This run surfaced genuinely new material. Two epistemic deltas (PN-A grade-upgrade reopen, PN-B "not load-bearing
is a hypothesis") are not encoded anywhere in the current kit — the kit treats gap premises and name-implied kinds
as hypotheses but never a prior block's *scoping* judgment, and §8 names reopen-for-new-work but not
reopen-to-raise-grade. One concrete tool gap (PN-C) stalled the run until a script was hand-written. The rest of the
run was textbook kit discipline (PRIOR-COVERAGE → REMIT → DEEPEN, tiers declared), which is why the "Already covered"
list is long and the new-delta list is short and load-bearing — as a healthy retro should be.
