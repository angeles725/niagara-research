<!-- review-status: dismissed 2026-09-05 · no kit deltas (self-declared "adds no rule") -->
# Retro (research-sdd) — the live oBIX oracle as a commissioning decision-maker: valve-governed suction killed Fase 3, and unreliable instrumentation killed pressure control

- **Date**: 2026-09-04
- **Focus**: PANCCADIA León — CompPan (rack de compresores), decisión Fase 1 / Fase 2 / Fase 3 durante comisionamiento en vivo.
- **Mode**: consulting on a LIVE subject. A peer session ("Panccadia") read the running JACE via an oBIX tunnel every 1–2 min and fed readings back; this session held the code + the physics and made the calls. Extends the 2026-09-03 `obix-oracle` and `commissioning-map-consulting` retros with a concrete decision the oracle drove.
- **Status**: PROPOSED note for the research-sdd methodology (how a live oracle turns a design question into an evidenced decision). Adds no rule.

---

## What the oracle settled (that code review alone could not)

1. **The deploy is live — proven by behavior, not by asking.** `dischargeHighAlarm=false` with `dischargeHighLimit` still `0` (old code would force it true on any real discharge) + `condenser1` staying commanded-on-with-fault instead of being dropped/restaged = the NEW code (0=disabled, amps-decoupled) is running. Behavior is a better deploy-confirmation than a changelog.

2. **The suction is governed by LOAD (room valves), not by compressor staging.** The decisive readings: suction moved −11 psi with StagesOn UNCHANGED (valves closed); and a stage change that DID move suction always coincided with a valve change → confounded. Method that made this legible: a peer-defined **"clean event" criterion** = StagesOn changes AND the 4-room valve signature is identical between two reads. We never got a clean one because valves dominate. Physical read: when rooms satisfy and valves close, the running compressors pump the suction line down (5 psi); when they open, it rises (~25). Operating suction top ≈ 25 psi (operator-confirmed).

3. **Therefore Fase 2/3 (pressure modulation) does not fit this plant; Fase 1 (demand-count) is robust.** Fase 3's R404A curve targets ~44 psi for a −6 °C room (coilTD 8), but the plant tops at 25 → it would shed the rack. And the curve clamps at −30 °C SST (~18 psi gauge), so it can't even express this plant's low, valve-driven range. Fase 1 does not read the suction at all → immune to all of this.

4. **The suction/amps instrumentation is unreliable — a second reason to stay off pressure control.** `suctionPressure2` froze at exactly `130.5342` for ~3 cycles then recovered (intermittent stuck, worse than dead). The amps sensors read low/zero on running compressors (operator confirmed the 3 run). KEY CODE GAP surfaced: `isValid()` checks only null/invalid **status**, NOT staleness — a frozen sensor reads "valid", so a frozen PRIMARY would poison any pressure-based staging. `selectSuction` trusts the primary blindly (mismatch only alarms, never falls back to Fase 1).

## Methodology lessons

- **A live oracle can decide a design question that static analysis leaves open.** "Is Fase 3 worth wiring?" was unanswerable from the code (the curve is correct, the logic is sound); only the live plant's *actual* suction range answered it (no). Prefer an evidenced "doesn't fit this plant" over shipping a correct-but-inapplicable feature.
- **Define the clean-signal criterion up front and let the oracle filter.** Telling the oracle "only report a stage change with an unchanged valve signature, else mark it CONFOUNDED" turned a noisy 1-min stream into decision-grade evidence and cut the channel noise (silence the confounded/plain ticks).
- **Trust the operator over the sensor when they conflict, then explain the physics.** I twice over-read the amps ("only 1 compressor pumping") after the operator said the amps sensor is unreliable and all 3 run — the pressures (not the amps) are the honest physical signal for "is it actually cooling". Own the correction fast.
- **Timing gate for a live deploy**: a `-rt` change needs a station restart; with the plant cooling and a pumpdown excursion open, build+verify now and defer the deploy to an operator-chosen window. Do not restart a live refrigeration plant to ship an optimization.

## Open follow-ups (not this session)

- Optional code hardening if pressure control is ever wanted: staleness detection in `isValid()`, and fall to Fase 1 (demand-count) on `suctionMismatch` instead of trusting the primary.
- Field: replace/inspect `suctionPressure2` (intermittent stuck); confirm the amps CT scaling; set `suctionLowLimit` to a real ~3–5 psi floor (works in Fase 1 too — protects against the observed pumpdown).
