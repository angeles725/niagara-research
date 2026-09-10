# Block 847 — HARBOR lighting Route B wiresheet: the kitControl 1-circuit template (verified BBooleanSelect)

> **Focus:** harbor-greenmax-lighting. **Scope:** the concrete, buildable **kitControl wiresheet** that realizes
> the B842 control model **without a custom module** — drawn as a 1-circuit template to replicate per circuit.
> Deepens B844 with the exact blocks/slots verified against the station's own decompiled `kitControl`.
>
> **Evidence:** the `BBooleanSelect` API is `[CERT]` — verbatim from `organized/kitControl/kitControl-rt/
> vineflower/com/tridium/kitControl/util/BBooleanSelect.java` (+ its base `BSwitch`). The wiresheet itself is
> `[INFER]` (engineering design) built on that verified API and the B841 `[CERT-live]` station facts.

---

## 1. The one block that does the 1-of-4 mux — `BBooleanSelect` `[CERT]`

`kitControl:BBooleanSelect` (extends `BMuxSwitch` → `BSwitch`) is a native 1-of-N boolean selector:

| Slot | Type | Role |
|------|------|------|
| `inA … inJ` | BStatusBoolean | up to 10 boolean inputs |
| `select` | BStatusEnum | which input flows to `out` |
| `numberValues` | int (3–10, default 3) | how many inputs are active |
| `zeroBasedSelect` | boolean (default **false**) | **1-based**: `select=1→inA, 2→inB, 3→inC, 4→inD` |
| `out` | BStatusBoolean | selected input (null if `select` out of range) |

Mapping verified in `getInStatusValue()`: `case 1: getInA() … case 4: getInD()`. So one block with
`numberValues=4` **is** the 4-horario selector. (`BEnumSwitch`, `BNumericSelect`, `BEqual`, `BAnd`, `BOr`,
`BBooleanWritable`, string/numeric consts also exist stock — used for the two-level part.)

## 2. The 1-circuit template (all stock kitControl)

```
  ── created ONCE for the whole site ──
  Config/Iluminacion/Horarios/
     Horario1  (BooleanSchedule) ─┐
     Horario2  (BooleanSchedule) ─┤
     Horario3  (BooleanSchedule) ─┤   the 4 masters; the operator edits only these
     Horario4  (BooleanSchedule) ─┘

  ── replicated PER CIRCUIT k (inside each GMnn panel) ──

  ┌─ STAGE 1 : schedule select (which horario) ────────────────────────────┐
  │  Horario1.out ─▶ inA                                                    │
  │  Horario2.out ─▶ inB   ┌───────────────────┐                            │
  │  Horario3.out ─▶ inC   │ SchedMux          │                            │
  │  Horario4.out ─▶ inD ─▶│ BBooleanSelect    │─ out ─┐                    │
  │  effectiveSel ───────▶ │ numberValues = 4  │       │                    │
  │                        └───────────────────┘       │                    │
  └────────────────────────────────────────────────────┼────────────────────┘
                                                        │ (valor programado)
  ┌─ STAGE 2 : HOA  (Auto / Hand / Off) ────────────────┼────────────────────┐
  │  (valor programado) ─────────────────────▶ inA  (1 = Auto)               │
  │  BooleanConst TRUE  ─────────────────────▶ inB  (2 = Hand = forzar ON)   │
  │  BooleanConst FALSE ─────────────────────▶ inC  (3 = Off  = forzar OFF)  │
  │        ┌───────────────────┐                                            │
  │        │ HoaMux            │                                            │
  │        │ BBooleanSelect    │─ out ─▶  HorR{k} . in10   ◀── existing writable
  │  HoaR{k} ▶ select          │                          (GM02–16: HorR{k};
  │        │ numberValues = 3  │                           GM01: SchdlGM01 R{k})
  │        └───────────────────┘                                            │
  └──────────────────────────────────────────────────────────────────────────┘

  in16 (fallback) of HorR{k} is left as-is (comms-fail safe state).
```

**Key property of this design:** everything resolves onto **`in10`** — the same slot the schedule uses today
(B841 §3 `[CERT-live]`). The HOA "Hand/Off/Auto" is done by *selection*, not by juggling priority slots, so
there is **no null-release problem** and no need to write `in8`. Override "para que enciendan" = `Hand` (inB).

## 3. Two-level selection (panel default + per-circuit) — optional `[INFER]`

To get "the circuit follows the panel unless you change it":
- `SelR{k}` (NumericWritable/EnumWritable) — values **0 = seguir tablero**, 1–4 = horario fijo. Default 0.
- `SelPanel` (one per GMnn) — 1–4.
- `effectiveSel = (SelR{k} == 0) ? SelPanel : SelR{k}` built with `BEqual(SelR{k},0)` gating a
  `BNumericSelect`/`BEnumSwitch` between `SelPanel` and `SelR{k}`; its out → `SchedMux.select`.

Drop this and just point `SchedMux.select ← SelR{k}` if per-panel default is not needed.

## 4. Points added per circuit + what is reused `[INFER]`

- **Reused (already in the station):** `HorR{k}` writable (GM02–16) / `SchdlGM01 R{k}` (GM01); the
  `labelR{k}`/`tabR{k}` descriptions (B846); the `Relay[k].BO` status.
- **New per circuit (~6 objects):** 2× `BBooleanSelect` (SchedMux, HoaMux), 2× `BooleanConst` (TRUE/FALSE),
  `SelR{k}` (selector), `HoaR{k}` (HOA). Plus optional 2-level blocks.
- **New once:** `Horario1..4` + `SelPanel` per panel.

## 5. Scale & build order `[INFER]`

- Build **one circuit fully**, verify it, then **copy-paste** it per circuit and re-point `select` sources +
  labels. GM02–16 are half-built already (`R{k}`→`HorR{k}` + labels) so it is "insert the two mux blocks
  between `R{k}` and `in10`"; GM01 (old pattern) is the most work.
- **Cutover per circuit:** link `HoaMux.out → in10` and **unlink the old `R{k}`/`Schedule{k}` → in10** in the
  same step so a slot never has two writers and no circuit goes dark (B842 §7).
- Decommission the ~349 legacy schedules after validation; keep the 4 masters.
- No extra license: `kitControl` is standard `[INFER]` (stock module present in the station).

---

## Self-verify

| # | Claim | Marker | Evidence |
|---|-------|--------|----------|
| 1 | BBooleanSelect = native 1-of-N boolean mux: inA–inJ, select (BStatusEnum), numberValues 3–10, out | [CERT] | BBooleanSelect.java @NiagaraProperty + slots dump |
| 2 | 1-based mapping select=1→inA … 4→inD (zeroBasedSelect default false) | [CERT] | BBooleanSelect.getInStatusValue() switch; BSwitch.zeroBasedSelect |
| 3 | Two BBooleanSelect stages (SchedMux 4-way + HoaMux 3-way) feed the existing HorR{k}.in10 | [INFER] | design on the verified API + B841 in10 fact |
| 4 | HOA via selection (Auto/Hand/Off) avoids any priority-slot null-release | [INFER] | design; in10 is the schedule slot today (B841 §3 [CERT-live]) |
| 5 | Two-level selection via BEqual + BNumericSelect/BEnumSwitch on SelR{k}==0 | [INFER] | design; those blocks exist in kitControl (class listing) |
| 6 | Reuses existing HorR{k}/SchdlGM01 R{k} + labelR{k}; ~6 new objects/circuit | [INFER]/[CERT-live] | B846 station structure; design |
| 7 | Standard kitControl, no extra license | [INFER] | kitControl is a stock module present in the station |

**Tally:** 7 claims — 2 [CERT] (the BBooleanSelect API), 4 [INFER] (the wiresheet design), 1 mixed. Every [INFER]
is an explicit design choice on top of the verified block API.

## Connections
- **B844** — Route B (kitControl-only) narrative; this block is its concrete verified wiresheet.
- **B842** — the control model (4 masters + selector + HOA) this wiresheet implements.
- **B846** — the `HorR{k}`/`labelR{k}` station structure the template plugs into.
- **B843** — the custom-module alternative (encapsulates this same logic as one reusable component).

## Corrections (see B851)
- **CLOSED / SUPERSEDED by B851 (live pilot, 2026-09-10):** the Numeric→Enum `select` link is **accepted
  directly** in 4.3 (no converter) — B847-G1 closed. The HOA moved from **per circuit** to **per horario**
  (5 shared `Horn_HOA`), and the master count is **5, not 4** (`SchedMux numberValues=5`). The 1-circuit
  template here is still valid for the SchedMux half; the HoaMux is now built once per horario, not per circuit.

## Open gaps (RESEARCH-STATE-harbor-greenmax-lighting)
- **B847-G1** — *(CLOSED by B851)* Workbench link-type check: `SelR{k}.out` (StatusNumeric) links directly to `BBooleanSelect.select` (StatusEnum) in 4.3, no conversion block needed.
- **B847-G2** — Whether one shared `HoaMux` per panel (panel-level HOA) plus per-circuit is wanted, vs per-circuit only (object-count impact).
- **B847-G3** — *(DELIVERED, best-effort)* the 1-circuit template `.bog` is built and structurally validated (bog-nav): `clients/distech-merida-harbor/deliverables/GreenMAX_Circuit_Template.bog` (+ README). 8 components, 5 internal links wired; operator adds 4 external links. **Remaining:** import it into a real 4.3 Workbench to confirm — folds into B847-G1 (the Numeric→Enum `select` link is the risk point).
