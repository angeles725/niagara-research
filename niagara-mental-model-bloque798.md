# B798 · Conformance baseline of the operator's modules at kit v0.17.0 (§20) `[CERT]`

> **§20 DOCUMENT-mode capture** (not gap-discovery). Runs the campaign-6 conformance toolbelt READ-ONLY over the
> operator's five real module artifacts at kit **v0.17.0** (niagara-tools main `c136e3b`) and records every row —
> the punch-list evidence for issue **#49** and the "before" picture for a future `verify-module.sh report` mode.
>
> **Sources (read-only)**: the operator modules under
> `Cliente/Leon-Guanjuato/{Paccadia/ColdRoomPan, Compresores/CompPan, Dashboard/DashboardPan}`; kit
> `build-n4-module-kit/toolbelt/{preflight,verify-module,slot-coverage,lint-timers}.sh` @ `c136e3b`.
> Every row was produced by the driver this run (existing `build/libs` jars used for `verify-module`; no rebuild).
> Method: run each check, transcribe. Markers: `[CERT]` reproduced tool output · `[INFER]` deduction.
>
> **Type:** `capture`. Connects [Block 787] (timer lint), [Block 788] (palette/lexicon), [Block 792] (dup-keys),
> [Block 797] (the `--plano` check, not yet in the gate).

## 798.1 — Environment `[CERT]`
`preflight.sh /mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162 <root>` → **PASS jdk8** (`/usr/lib/jvm/java-8-openjdk-amd64`,
via the PR8 `bin/java` fallback) · **PASS plugin-pin 7.6.17** · **PASS jar-lock**. Kit v0.17.0 (`c136e3b`).

## 798.2 — The baseline matrix `[CERT]`

| Artifact | verify-module (jar) | lint-timers | slot-coverage (type-set) | dup lexicon keys | palette entries |
|---|---|---|---|---|---|
| **ColdRoomPan-rt** | ALL PASS (8 cls major 52, signed, 6 types, baja 4.14) | **FAIL** BEvaporatorUnit (timer, no `stopped()`-cancel); PASS BDefrostController | **50.0%** — missing `DefrostMode,FanMode,StagingMode` | 0 | 3 |
| **CompPan-rt** | ALL PASS (4 cls, 1 type) | PASS BCompressorControl | 100.0% | 0 | 1 |
| **DashboardPan-rt** | ALL PASS (2 cls, 2 types) | (no timer classes → no FAIL) | 100.0% | 0 | 4 |
| **DashboardPan-ux** | ALL PASS (14 cls, 1 type) | (no timers) | 100.0% | 0 | 1 |
| **DashboardPan-wb** | — no jar (scaffold-only) | (no src timers) | — | 0 | 0 (empty scaffold) |

(`slot-coverage` "extra" entries are slot-level / enum-value lexicon keys with no declared TYPE — normal, not a
defect; only "missing" is a gap.)

## 798.3 — Non-conformances (punch-list for #49) `[CERT unless noted]`

1. **ColdRoomPan-rt · BEvaporatorUnit — timer leak** `[CERT]`: owns 4 `Clock.Ticket` fields, cancels them only on
   re-arm, has NO `stopped()` override → `lint-timers` FAIL. (The real defect [Block 787] documented and PR8's
   lint now catches.) Fix: add a `stopped()` that cancels all four tickets.
2. **ColdRoomPan-rt · slot-coverage 50%** `[CERT]`: types `DefrostMode`, `FanMode`, `StagingMode` are declared in
   `module-include.xml` but have NO `module.lexicon` display key → 3/6 uncovered. Fix: add the three lexicon keys.
3. **DashboardPan-ux · `--plano` would FAIL** `[INFER, grounded in B797]`: the current `verify-module` has no
   `--plano` check, so the ux jar shows ALL PASS; but per [Block 797] the SPA carries a stale `.frame{aspect-ratio:1247/771}`
   (≠ `IMG_W/IMG_H` 1248/891, masked by `#frame:auto`). Once `--plano` (issue #47) lands, this artifact fails until
   `.frame`'s numeric `aspect-ratio` is deleted.
4. **DashboardPan-wb — scaffold-only** `[CERT]`: no `build/libs` jar and an empty `module.palette` (the B788
   empty-`b:Folder` scaffold). Not a defect if `-wb` is intentionally unbuilt; a gap if a wb view is expected.

Everything else is conformant at v0.17.0: all four built jars pass the verify gate (bytecode 52 / signed / types /
baja 4.14 / non-empty palette), zero duplicate lexicon keys across all five artifacts, and CompPan-rt +
DashboardPan-rt/-ux at 100% slot-coverage.

## 798.4 — Kit implication → a future `verify-module.sh report` mode `[INFER, grounded]`
This matrix IS the shape of a `report` mode: one row per artifact aggregating verify-gate + lint-timers +
slot-coverage + dup-keys + palette, with a per-module PASS/GAP roll-up. The two real gaps (BEvaporatorUnit
`stopped()`, ColdRoomPan-rt 3 missing lexicon keys) are the concrete #49 punch-list; the `--plano` gap (#3) is
gated on issue #47 landing first.

## 798.5 — Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | 4/4 built jars pass verify gate ALL PASS at v0.17.0 | `[CERT]` | §798.2 driver run | Y |
| 2 | ColdRoomPan-rt BEvaporatorUnit FAILs lint-timers | `[CERT]` | §798.2/§798.3 | Y — reproduced |
| 3 | ColdRoomPan-rt slot-coverage 50% (3 types missing lexicon) | `[CERT]` | slot-coverage output | Y |
| 4 | Zero duplicate lexicon keys across all five artifacts | `[CERT]` | dup-keys=0 each | Y |
| 5 | DashboardPan-ux would fail `--plano` once #47 lands | `[INFER]` | [Block 797] | grounded |

**Tally:** `[CERT]` ×4 · `[INFER]` ×1. Capture block — ratio is not an exhaustion signal (§11).

## 798.6 — Connections & open gaps
- [Block 787] (timer lint origin), [Block 788] (palette/lexicon), [Block 792] (dup-key check), [Block 797] (`--plano`).
- Punch-list (issue #49): ColdRoomPan-rt BEvaporatorUnit `stopped()` + 3 missing lexicon keys. No new investigable
  gap — this is a snapshot; re-run after the #49 fixes to confirm GREEN.
