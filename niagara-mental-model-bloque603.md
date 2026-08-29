# Block 603 — KC13-G1 live station-wide safety-config audit: this station runs a 68-component refrigeration/HVAC control app with ZERO PID loops (so `disableAction=hold`/`rampTime=0` do not apply), but 5 logic blocks sit at the unsafe `propagateFlags=0` default and 3 writable points have a NULL fallback — the real exposure is status-propagation and relinquish-to-null, not loop behavior

**Session**: 2026-08-29
**Focus**: `kitControl` (gap KC13-G1 — station-wide safety-config audit, `requires-execution` → §12). This CLOSES
the last open kitControl gap.
**Distribution / live target**: OptimizerSupervisor-N4.14.0.162, `127.0.0.1` (`DESKTOP-4AAQ77H`), control app at
`Drivers/CODIGOS/`. `live-install` → SECRETS DISCIPLINE (component names + config cited; no values beyond
non-sensitive control config).
**Method**: READ-ONLY (§12 rung-1) oBIX config-tree enumeration with `API2`/SCRAM, `no·inline`. Walked
`/obix/config/`, read every child's safety slots (`propagateFlags`, `disableAction`, `rampTime`, `fallback`).
**Primary sources**:
- `[CERT-live]` `sources/probes/B603-kc13-safety-audit/{audit-tally.txt,live-safety-slots.txt}`.
- Safety semantics REMITTANCE `[CERT]` [Block 543] §543.6 (six unsafe-unless-configured defaults), [Block 539]
  (`propagateFlags` = AND-mask whitelist, default 0), [Block 550] (writable fallback guard).
**Scope**: measure the ACTUAL unsafe-config exposure of B543's six defaults on the live station. Does NOT
re-derive the static safety model ([B543] REMITTANCE) — it MEASURES which defaults are live.

---

## 603.1 The station's control app [CERT-live]

`Drivers/CODIGOS/` holds a **68-component** refrigeration/HVAC plant control: sensor points (`Temp Amb`,
`Temp Aba`, `Temp Reto`, `Temp Suc1/2`, `Hum`, compressor/fan amps `Amp Fan`/`Amp Comp1/2`/`Amp Aba1/2`),
kitControl logic blocks (`OneShot`, `StringSelect`, `GreaterThan`, `LessThanEqual`, `And`, `Add`), writable
setpoints/commands (`NumericWritable*`, `BooleanWritable*`, `FAN`, `StringWritable*`), alarm points
(`Alarma_Planta1/2/3`, `Conteno_Alarmas_Planta_*`), and high/low switches (`SW ALTA/BAJA 1/2`).
`sources/probes/B603-kc13-safety-audit/audit-tally.txt`.

## 603.2 The loop-specific B543 risks DO NOT APPLY — there are no loops [CERT-live]

**BLoopPoint (PID) count on this station = 0** (68 components scanned, none is a `kitControl:LoopPoint`).
Consequently B543's two loop-scoped unsafe defaults — `disableAction=hold` (freezes last command on disable)
and `rampTime=0` (no anti-slam rate limit) — have **no live surface here**: this station does its control with
discrete logic blocks + writable points, not modulating PID loops. This is itself the audit's first finding: a
station's safety exposure depends on WHICH control primitives it actually uses, and the loop risks B543
enumerated are latent, not present, on a discrete-logic plant like this one.

## 603.3 The REAL exposure #1 — `propagateFlags=0` on 5 of 6 logic blocks [CERT-live]

Of the six kitControl blocks that carry a `propagateFlags` slot, the live distribution is `{0: 5, 40: 1}`:

| propagateFlags | count | blocks |
|---|---|---|
| **0** (unsafe default — no input status propagates) | **5** | `GreaterThan`, `LessThanEqual`, `And`, `Add`, `GreaterThan1` |
| 40 (partial whitelist) | 1 | `StringSelect` |

Per [Block 539]/[Block 543], `propagateFlags` is an AND-mask whitelist of which input status flags reach the
output; **default 0 means a faulted / down / stale input does NOT flag the block's output** — the output reads
`{ok}` while its input is bad. This is live and load-bearing: `GreaterThan1`'s output is currently
`{alarm,unackedAlarm}` yet with `propagateFlags=0` a downstream consumer of a similar block would see a clean
status. Five blocks in this plant are at that default. `StringSelect`=40 is a nonzero partial whitelist
(propagates some flags; exact bit decode `[INFER]` — not decoded against `BStatus` constants this pass).

## 603.4 The REAL exposure #2 — 3 writable points relinquish to NULL [CERT-live]

Writable points were checked for B543's fallback guard (a null fallback means the point goes **null** — not to a
safe value — when all priority levels relinquish). Live result:
- **NULL fallback (unsafe): 3** — `Conteno_Alarmas_Planta_2`, `Conteno_Alarmas_Planta_4`, `BooleanWritable1`.
- **Safe non-null fallback (sample): `NumericWritable`=22.0, `FAN`=true, `BooleanWritable`=false** — these
  relinquish to a real value (and were observed running `@ def`, i.e. currently on that fallback level).

Severity is role-dependent (honest scoping): the two `Conteno_Alarmas_Planta_*` are alarm COUNTERS (a null
counter on relinquish is a data-integrity nuisance, not a control hazard), whereas `BooleanWritable1` with a
null fallback IS the B543 concern if it commands equipment — a relinquish leaves it null rather than at a
fail-safe state. The audit flags it; deciding the fail-safe direction is an operator/application call.

## 603.5 Audit verdict (operator-facing) [CERT-live]

| B543 unsafe default | Live exposure on this station |
|---|---|
| `disableAction=hold` | **N/A** — 0 loops |
| `rampTime=0` | **N/A** — 0 loops |
| `propagateFlags=0` (status masking) | **PRESENT — 5 logic blocks** (GreaterThan/LessThanEqual/And/Add/GreaterThan1) |
| null writable `fallback` | **PRESENT — 3 points** (2 alarm counters + BooleanWritable1) |
| `BLoopAlarmAlgorithm` alarm-only | N/A — no loops |
| clHVAC status-strip | N/A — no clHVAC blocks in this app |

Operator recommendation (from [B543] §543.6, now targeted): set `propagateFlags` to include
`fault|down|stale` on the 5 logic blocks so bad inputs surface at the output; set a fail-safe non-null
`fallback` on `BooleanWritable1` (and decide whether the alarm counters should default to 0). No PID-loop
hardening is needed here because there are no loops.

## 603.6 What this does NOT resolve

- The exact bit decode of `propagateFlags=40` (StringSelect) against `BStatus` constants — `[INFER]`; a code
  read of `javax/baja/status/BStatus` bit values would make it `[CERT]`.
- Whether the 5 `propagateFlags=0` blocks feed a SAFETY-critical output vs a display — the audit measures
  config exposure, not the wiring downstream (that is a per-application trace, out of this gap's scope).

---

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | 68-component refrigeration/HVAC app under Drivers/CODIGOS/ | [CERT-live] | audit-tally.txt | ✓ live |
| 2 | 0 BLoopPoint → disableAction/rampTime risks N/A | [CERT-live] | audit-tally.txt | ✓ live |
| 3 | propagateFlags distribution {0:5, 40:1}; 5 blocks named | [CERT-live] | audit-tally.txt | ✓ live |
| 4 | GreaterThan1 out is {alarm,unackedAlarm} live | [CERT-live] | live-safety-slots.txt | ✓ live |
| 5 | 3 writables NULL fallback (2 counters + BooleanWritable1) | [CERT-live] | audit-tally.txt | ✓ live |
| 6 | 3 sampled writables safe non-null fallback (22.0/true/false) | [CERT-live] | live-safety-slots.txt | ✓ live |
| 7 | propagateFlags semantics (AND-mask, default 0) | [CERT] | [B539]/[B543] REMITTANCE | ✓ prior |
| 8 | propagateFlags=40 exact bit decode | [INFER] | not decoded | honest gap |

**Marker tally**: [CERT-live] ×6, [CERT] ×1 (remittance), [INFER] ×1. Ratio 1/7 = 0.14. **Block type: EVIDENCE
(§12 live audit).** CLOSES KC13-G1 → kitControl focus now investigable=0 AND requires-execution=0.
**§12 verdict: CONFIRMED (scoped)** — B543's loop risks NOT-REPRODUCED (no loops); its status-propagation and
null-fallback risks CONFIRMED PRESENT and quantified. Zero secrets. Read-only.

## Connections

- [Block 543] §543.6 — the six unsafe-unless-configured defaults; this block MEASURES which are live.
- [Block 539] — `propagateFlags` = AND-mask whitelist, default 0.
- [Block 550] — writable fallback guard (null fallback = unsafe).
- [Block 600] — the oBIX read surface this audit walked.
- kitControl focus: KC13-G1 was the sole remaining (requires-execution) gap — now closed.
