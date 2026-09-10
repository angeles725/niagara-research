# Block 842 — HARBOR lighting redesign: the unified control model (4 master schedules + per-panel/per-circuit selector + HOA override)

> **Focus:** harbor-greenmax-lighting. **Scope:** the control-logic DESIGN that satisfies the operator's ask,
> stated once, independent of how it is built. B843 realizes it as a **custom N4 module**; B844 realizes the
> same behavior with **kitControl only**. Both re-feed the existing `SchdlGMnn Rk` writables identified in
> **B841 §3–4**; nothing downstream of the Supervisor changes.
>
> **Evidence base:** B841 `[CERT-live]` (the current chain and priority levels). Design claims here are `[INFER]`
> (engineering proposal) except where they restate a B841 `[CERT-live]` fact. Marked per claim.

---

## 1. The requirement, precisely

From the operator (verbal) and the SEJOFA proposal (B841 §5), the target behavior is:

1. **4 general schedules** ("horarios") instead of ~330 per-circuit schedules — edited in **one place each**.
2. A **selector, HOA-style, with the 4 schedules as options**, so the operator assigns *which* horario a group
   obeys — **at panel level** ("estos tableros hacen caso al schedule 1/2/3/4").
3. **Per-circuit selection too** — inside a panel each output can independently pick which of the 4 it follows
   ("en el tablero 4, cada una de las 24 elige").
4. **Override to force ON** — a manual command that lights a circuit regardless of schedule.

Naming note (B841 §5): the proposal's phrase is "Selector **Manual/Horario**" (2-state) — this design is the
richer 4-schedule + per-circuit version the operator asked for; confirm it as in-scope or a priced extension.

## 2. Two orthogonal controls per output — do not conflate them `[INFER]`

The clean model separates **which schedule** from **auto-vs-manual**:

- **Schedule selection (1-of-4)** — answers "when on Auto, which horario do I obey?" Values: `H1 | H2 | H3 | H4`.
- **HOA mode (Hand / Off / Auto)** — answers "do I follow the schedule at all?" This is the classic Niagara
  Hand-Off-Auto override: `Auto` follows the selected schedule; `Hand` forces ON; `Off` forces OFF.

The operator's "4 opciones" is the **selection**; the "override para que enciendan" is **Hand**. Keeping them
separate means an output can be, e.g., "Auto on H2" today and flipped to "Hand" for a night event without
losing its schedule assignment.

## 3. Two-level selection: panel default + per-circuit override `[INFER]`

Selection resolves top-down so the operator gets both the fast path (assign a whole panel) and granularity
(tune one circuit):

```
perCircuitSel ∈ { FollowPanel, H1, H2, H3, H4 }   (default FollowPanel)
panelSel      ∈ { H1, H2, H3, H4 }
effectiveSel  = (perCircuitSel == FollowPanel) ? panelSel : perCircuitSel
```

Set a panel's `panelSel = H3` and every circuit still on `FollowPanel` moves to H3 in one action — the headline
win. Override one lobby circuit to `H1` and it ignores the panel default. This is exactly points 2 + 3.

## 4. The resolution pipeline for one circuit `[INFER]`

```
        H1.out ┐
        H2.out ┤
        H3.out ┼─▶  MUX(effectiveSel)  ──▶  scheduledValue
        H4.out ┘

  write:  scheduledValue ──▶ SchdlGMnn Rk . in10     (Auto path; priority 10, unchanged from today B841 §3)
          HOA override    ──▶ SchdlGMnn Rk . in8      (Hand=true / Off=false / Auto=release; priority 8)
```

- **`in10`** carries the *selected* master schedule instead of a dedicated one — the only structural change to
  the existing chain. `[CERT-live]` that in10 is the schedule slot today (B841 §3).
- **`in8`** (any priority `in1`–`in9`, above the schedule) carries the HOA override; releasing it (null) drops
  back to the schedule. `[CERT-live]` that in1–in15 are free today; `[INFER]` the choice of in8.
- **`in16`** stays the fallback. Untouched.
- Downstream (JACE reads `SchdlGMnn Rk` → BACnet `Relay[k].BO.in16`) is **unchanged** — the redesign lives
  entirely in how `SchdlGMnn Rk` is fed on the Supervisor. `[CERT-live]` (B841 §3).

## 5. The 4 master schedules `[INFER]`

Four `BooleanSchedule`s (e.g. `Config/Iluminacion/Horarios/Horario1..4`) on the **Supervisor** (where scheduling
already runs, B841 §8). Editing one changes every circuit currently pointed at it — the whole point. Keep them
in one folder so the operator has a single "edit the 4 horarios" screen. The ~330 legacy per-circuit schedules
are **decommissioned** (unlinked, then deleted after validation), not edited.

## 6. What the operator sees (control-object contract, both routes must expose) `[INFER]`

Per **panel**: `panelScheduleSel` (enum H1–H4), optional `panelHOA` (Hand/Off/Auto to move the whole panel).
Per **circuit**: `circuitScheduleSel` (enum FollowPanel/H1–H4), `circuitHOA` (Hand/Off/Auto), and a read-only
`effectiveState` + `effectiveSchedule` for feedback. These are the exact points the dashboard (B845) binds to,
and the acceptance surface for validation.

## 7. Migration & safety `[INFER]`

- **Additive first**: build the 4 masters + selectors and wire them to `in10` in parallel; the legacy schedule
  also sits on `in10` — Niagara takes the **last-written** at a priority, so leave exactly one writer per
  slot. Cleanest: unlink the legacy schedule from `in10` **as** the new selector is linked, circuit by circuit,
  so a circuit is never dark or double-driven.
- **Fail-safe**: if the selector/mux is unavailable, `in16` fallback (`true` today) still holds the relay in a
  known state — no circuit floats.
- **Reversibility**: keep the station backup (B838); the change is link-level and per-circuit, so a bad circuit
  is re-pointed to its old schedule in isolation.
- **Scale**: ~330 circuits. The design is identical per circuit → the real decision is *encapsulation*
  (B843 module = one reusable component ×330) vs *replication* (B844 kitControl = a wiresheet pattern ×330).
  That trade-off is the subject of B843/B844.

---

## Self-verify

| # | Claim | Marker | Evidence |
|---|-------|--------|----------|
| 1 | Requirement = 4 schedules + HOA-style 1-of-4 selector + per-circuit selection + force-ON | [CERT-live] | operator ask + SEJOFA PDF (B841 §5) |
| 2 | Proposal selector is 2-state (Manual/Horario); this design is the richer 4-way — flag scope | [CERT-live] | SEJOFA PDF p.2 |
| 3 | Separate "which schedule" (1-of-4) from "HOA mode" (Hand/Off/Auto) | [INFER] | design; classic Niagara HOA over a priority array |
| 4 | Two-level selection: effectiveSel = perCircuitSel==FollowPanel ? panelSel : perCircuitSel | [INFER] | design satisfying ask points 2+3 |
| 5 | Feed selected schedule to writable.in10 (schedule slot today) | [CERT-live]+[INFER] | in10 = schedule today (B841 §3); [INFER] re-feed |
| 6 | HOA override on a higher priority (in8); release → back to schedule | [INFER] | priority array in1–in15 free today (B841 §10 [CERT-live]); in8 choice [INFER] |
| 7 | Downstream JACE→BACnet unchanged; change confined to feeding SchdlGMnn Rk | [CERT-live] | B841 §3 chain |
| 8 | 4 master BooleanSchedules on the Supervisor; legacy per-circuit schedules decommissioned | [INFER] | design; scheduling runs on Supervisor (B841 §8 [CERT-live]) |
| 9 | Per-circuit migration (unlink legacy as new selector links) avoids dark/double-driven slot | [INFER] | Niagara last-writer-per-priority semantics |

**Tally:** 9 claims — 2 [CERT-live], 2 mixed [CERT-live]+[INFER], 5 [INFER]. Every [INFER] is an explicit design
choice, not a disguised fact; every factual anchor traces to B841 [CERT-live].

## Connections
- **B841** — the current chain, priority levels, and the `SchdlGMnn Rk` write targets this design re-feeds.
- **B843** — custom N4 module realization (encapsulated selector component ×panel/×circuit).
- **B844** — kitControl-only realization (replicated wiresheet, no module).
- **B845** — the operator dashboard binding the §6 control contract.
- **B838** — backup/restore reversibility for the migration (§7).

## Open gaps (RESEARCH-STATE-harbor-greenmax-lighting)
- **B842-G1** — Confirm the HOA granularity the client wants: per-circuit only, per-panel only, or both (affects point count and dashboard density).
- **B842-G2** — Decide the override priority level (in8 vs a dedicated emergency in1 + operator in8) and whether "Off" must beat a future emergency-ON.
- **B842-G3** — Whether the 4 masters need per-panel calendar exceptions (holidays) — a `CalendarSchedule` shared input — or 4 flat weekly schedules suffice.
