# Block 844 — HARBOR lighting redesign Route B: kitControl-only (no custom module)

> **Focus:** harbor-greenmax-lighting. **Scope:** the concrete wiresheet design that realizes the B842
> unified control model (4 master schedules + per-panel/per-circuit selector + HOA override) using **only
> stock kitControl function blocks** — no custom Niagara module compiled, signed, or deployed. This is
> Route B; Route A (custom module) is B843. Both re-feed the same `SchdlGMnn Rk` `BooleanWritable`
> targets identified in **B841 §3–4** `[CERT-live]`. Nothing downstream of the Supervisor changes.
>
> **Evidence base:** B841 `[CERT-live]` (the current chain + priority slots), B842 `[INFER]` (the
> unified control model this block implements), B537 `[CERT]`/`[CERT-doc]` (the kitControl block
> catalog, decompiled). Design proposals here are `[INFER]` except where they restate a B841 or B537
> established fact. Marked per claim.

---

## 1. What kitControl blocks are actually available `[CERT]` (B537)

B537 cataloged 151 classes in `com.tridium.kitControl` across 10 packages; the blocks relevant to this
design, with their exact confirmed names:

| Block | Package | Role | B537 evidence |
|-------|---------|------|--------------|
| `BBooleanSelect` | util | N-way boolean MUX (up to 10 in) | B537 §537.3 util list + `BSwitch.java:125-142` |
| `BNumericSelect` | util | N-way numeric MUX (same family) | B537 §537.3 |
| `BBooleanSwitch` | util | 2-way boolean MUX (`inSwitch` routes `inTrue`/`inFalse`) | B537 §537.5 + `BBooleanSwitch.java:72-90` |
| `BNumericSwitch` | util | 2-way numeric MUX (same mechanics) | B537 §537.3/5 |
| `BEqual` | logic | equality comparison → boolean | B537 §537.3 logic list |
| `BAnd`, `BOr`, `BNot` | logic | boolean gates | B537 §537.3 |
| `BNumericConst` | constants | fixed numeric value source | B537 §537.3 |

`BBooleanSelect` behavior confirmed `[CERT]` (B537 `BSwitch.java:125-142`): a `select` enum ordinal
routes one of up to 10 inputs `inA..inJ`; `numberValues` (3–10) configures visible slots; `zeroBasedSelect`
toggles 0- vs 1-based indexing (default is 1-based, so ordinal 1 → `inA`, 2 → `inB`, etc.). Invalid
`select` → hold last value with invalid flag (not null, not zero).

**What is NOT in the confirmed catalog:** no dedicated `BHOASelector` or nullable-output boolean block
appeared in B537's util (37) or logic (13) inventories. The HOA override mechanism is therefore composed
from writables + links rather than from a single kitControl block (§4 below).

## 2. The 1-of-4 boolean MUX — recommended concrete wiresheet `[INFER]` with `[CERT]` anchor

### Three candidate approaches

**(a) 4 × `BEqual` + `BAnd` + `BOr` chain.**
For each schedule Hk: `BEqual(effectiveSel, k)` → `BAnd.inA=H_k.out, .inB=equal_result` → feed into a
4-input `BOr`. Output = whichever `BAnd` whose `BEqual` is true. This works and uses only blocks that
definitely exist, but it needs 4+4+1 = **9 blocks** per circuit, plus the `BAnd.nullOnInactive=true`
behavior (B537 §537.5) propagates null when `BEqual` is false, so the `BOr` sum-reduction sees nulls from
inactive lanes — verify this does not produce fault-status on the `BOr` output under the multi-input null
contract. It also requires that `effectiveSel` be a numeric (1–4) comparable via `BEqual`.

**(b) Chain of 3 × `BBooleanSwitch` blocks.**
`sw1 = BBooleanSwitch(inSwitch=(effectiveSel≤2), inTrue=sw2, inFalse=sw3)` where
`sw2 = BBooleanSwitch(inSwitch=(effectiveSel==1), inTrue=H1.out, inFalse=H2.out)` and
`sw3 = BBooleanSwitch(inSwitch=(effectiveSel==3), inTrue=H3.out, inFalse=H4.out)`. This uses 3 switch
blocks + 3 comparisons = 6 blocks per circuit, avoids the null-propagation hazard, but `BBooleanSwitch`
input selection is boolean-valued (it routes H_k.out which IS a boolean — fine). Still needs the comparison
blocks for `effectiveSel≤2` and `effectiveSel==1` etc.

**(c) `BBooleanSelect` with `numberValues=4`. ← RECOMMENDED**
One block, `select` ordinal 1–4 maps directly to `inA=H1.out, inB=H2.out, inC=H3.out, inD=H4.out`.
Set `numberValues=4`, `zeroBasedSelect=false`. Feed `effectiveSel` (a numeric 1–4) into the `select`
slot. This is **1 block** for the full MUX. The `[CERT]` behavior (hold-with-invalid on bad select) means
if the panel selector is mis-configured, the circuit holds its last known state rather than flipping —
a safe failure mode.

**Type-compatibility note `[INFER]`:** `BBooleanSelect.select` is typed as "enum ordinal" in B537 source
(`BSwitch.java:125-142`). In Niagara N4, `NumericWritable.out` (a `StatusNumeric`) should satisfy an
ordinal input, but the exact slot type matching must be **confirmed in Workbench** before bulk replication
(link the panel `NumericWritable.out` → `BBooleanSelect.select` and verify Workbench accepts the link
without a conversion block). If it demands an `EnumWritable`, substitute a `BNumericConst` output or
an `EnumConst` block from the constants package `[CERT]` B537 §537.3.

### Recommended circuit wiresheet (one per circuit)

```
[Supervisor — per-circuit container for GM01 R1]

Inputs (from outside this container):
  H1.out ─────────────────────────────────┐
  H2.out ──────────────────────────────┐  │
  H3.out ───────────────────────────┐  │  │
  H4.out ────────────────────────┐  │  │  │
  panelSel.out (NumericWritable) ─┤  │  │  │
  perCircuitSel.out (Numeric     ─┤  │  │  │
    Writable, 0=FollowPanel 1-4) │  │  │  │

[FollowPanel resolver]
  BEqual(perCircuitSel.out, 0)          → isFollowPanel (boolean)
  BNumericConst(0)                      → BEqual.inB
  BNumericSwitch(
    inSwitch = isFollowPanel,
    inTrue   = panelSel.out,            ← use panelSel when FollowPanel
    inFalse  = perCircuitSel.out        ← use circuit-own when overriding
  )                                     → effectiveSel (numeric 1-4)

[Schedule MUX]
  BBooleanSelect(
    numberValues  = 4
    zeroBasedSelect = false
    select = effectiveSel
    inA    = H1.out
    inB    = H2.out
    inC    = H3.out
    inD    = H4.out
  )                                     → scheduledValue (boolean)

[Outputs]
  scheduledValue ──link──▶ SchdlGM01 R1 . in10   (schedule priority, unchanged from today B841 §3)
  Override_GM01_R1 . out ──link──▶ SchdlGM01 R1 . in8   (HOA override; see §4)
```

Block count per circuit: `BEqual` + `BNumericConst` + `BNumericSwitch` + `BBooleanSelect` = **4 blocks**
for the selection path, plus 1 separate `BooleanWritable` for the HOA override (§4).

## 3. The two-level selection in stock blocks `[INFER]`

This realizes B842 §3's formula:
`effectiveSel = (perCircuitSel == FollowPanel) ? panelSel : perCircuitSel`

- **`perCircuitSel`** is a `NumericWritable` on the Supervisor (range 0–4; 0 = FollowPanel, 1–4 = H1–H4).
  This is the per-circuit control point the operator writes from the dashboard.
- **`panelSel`** is a `NumericWritable` shared by all circuits on the same panel (range 1–4). Writing it
  moves every circuit still on FollowPanel to the new schedule in one action.
- **`BEqual(perCircuitSel.out, 0)`** → boolean flag `isFollowPanel`.
- **`BNumericSwitch`** uses `isFollowPanel` to pick between `panelSel` and `perCircuitSel`.
- The **`BBooleanSelect`** consumes the resolved `effectiveSel` (1–4) to pick the master schedule output.

All three selection blocks use confirmed-existing [CERT B537] primitives; only the wiring topology is [INFER].

## 4. The HOA Hand/Off/Auto override `[INFER]`

### Design choice: a nullable `BooleanWritable` as the override point

The clearest stock mechanism does not need additional logic blocks. For each circuit, add one dedicated
**`BooleanWritable`** (call it `Override_GM01_R1`):

| Operator action | Write to `Override_GM01_R1` | Effect on `SchdlGM01 R1` |
|----------------|-----------------------------|--------------------------|
| **Hand (force ON)** | `true` (any priority, e.g. in10 of the override writable) | `Override.out = true` → in8 = true → relay ON regardless of schedule |
| **Off (force OFF)** | `false` | `Override.out = false` → in8 = false → relay OFF regardless of schedule |
| **Auto (follow schedule)** | **release / null** | `Override.out = null` → in8 released → `SchdlGM01 R1` falls through to in10 (scheduledValue) |

The link is: `Override_GM01_R1.out` → `SchdlGM01 R1.in8`. When `Override.out` is null (relinquished),
the link carries null-status to in8, which is exactly "slot relinquished" in the priority-array model.
The `[CERT-live]` B841 §3 establishes that in1–in15 are free today, so in8 is available.

**Why a writable, not a logic block?** A `BBooleanConst` or a `BAnd` output is always valid — there is no
stock kitControl logic block with a stock "output null on condition" slot other than `BBooleanSwitch` with
a null `inTrue`/`inFalse` input. A `BooleanWritable` can be released (null output) by the operator or
dashboard write action, giving clean three-state behavior without additional composed logic.

**Dashboard binding `[INFER]`:** the B845 dashboard writes to `Override_GM01_R1` directly via a Niagara
action or a bound control — "Hand" sends `true`, "Off" sends `false`, "Auto" releases (null). Any
Workbench operator can also right-click the writable and use Override/Release.

**Optional panel-level HOA `[INFER]`:** add one more `BooleanWritable` (`PanelOverride_GM01`) linked to
every circuit's `in7` (above in8). An operator "Hand all" action writes `true` to the panel writable and
overrides all circuits in the panel. B842-G1 flags whether this granularity was confirmed in scope.

## 5. The 4 master BooleanSchedules and decommissioning the legacy ~330 `[INFER]`

Four `BooleanSchedule` objects on the Supervisor — confirmed scheduling location `[CERT-live]` B841 §3/8 —
placed in a dedicated folder (e.g., `Config/Iluminacion/Horarios/H1..H4`). Editing any one of the four
changes every circuit currently pointed at it via `BBooleanSelect`.

**Decommissioning `[INFER]` (follow B842 §7 migration strategy):**
1. Build the new container for one circuit (GM01 R1).
2. Link `BBooleanSelect.out → SchdlGM01 R1.in10` AND keep the legacy `Schedule1.out → in10` link temporarily
   absent — never two writers on the same priority slot at once.
3. Validate behavior.
4. Unlink `Schedule1.out → in10`, then delete `Schedule1`.
5. Repeat per circuit. Once all ~330 circuits are migrated, the `GMnnilum` proxy sub-folders lose their
   per-circuit schedules and can be trimmed to just the `SchdlGMnn Rk` writables.

## 6. Scale reality: ~330 replications `[INFER]`

**The numbers:**
- 16 panels × average ~21 circuits = ~330 circuit-level block containers.
- Each container = 4 logic blocks (`BEqual`, `BNumericConst`, `BNumericSwitch`, `BBooleanSelect`) + 1
  `BooleanWritable` (override) + 1 `NumericWritable` (perCircuitSel) = **6 objects per circuit**.
- Plus 1 `NumericWritable` (panelSel) per panel = 16 objects.
- Total new Supervisor objects: ~330 × 6 + 16 + 4 master schedules ≈ **2000 new components**.
- The 4 master schedules replace ~330 schedules → net delta ≈ +2000 − 330 = **+1670 objects**.

**Station size impact `[INFER]`:** the Supervisor is an N4 station running on Baja 4.3.58.18. A Supervisor
typically handles thousands of components; +1670 objects is significant but within the normal operating
range for an N4 Supervisor with dashboards and a real network. Monitor station memory and execution time
after a pilot panel is deployed.

**Build workflow recommendation `[INFER]`:**
1. Build the 4 master schedules and the 16 `panelSel` writables first.
2. Create one "template" container (e.g., a `BComponent` named `Circuit_Template`) with the 4 logic blocks
   pre-wired and the `panelSel` and `H1..H4` links placeholder-labeled.
3. Copy-paste the container 329 times. Workbench supports bulk copy-paste within a wiresheet.
4. For each copy, update two links: `BBooleanSelect.out → SchdlGMnn Rk.in10` and
   `Override.out → SchdlGMnn Rk.in8`.
5. Update the `panelSel` link to point at the correct panel's `panelSel` writable.

**Drift risk `[INFER]`:** if the logic must change after deployment (e.g., add a 5th schedule option),
every one of the ~330 containers must be individually edited. There is no "update template and propagate"
mechanism in stock Niagara. This is the principal operational liability of Route B.

**Config.bog bloat `[INFER]`:** ~2000 new BOG nodes increase config.bog file size and station load time.
For a Supervisor with dashboards and historian, this is manageable; for a JACE (not the case here — the
redesign lives on the Supervisor), it could matter. The Supervisor is HM_BMS, not the field JACE
HM_Central; BOG bloat on the Supervisor is a performance question, not a field-device resource concern.

## 7. Trade-offs: Route B (kitControl) vs Route A (custom module, B843) `[INFER]`

| Dimension | Route B — kitControl only | Route A — custom module |
|-----------|--------------------------|------------------------|
| **Build effort** | ~330 copy-paste operations; link ~660 outputs manually | Write once; instantiate the component ×330 |
| **Niagara expertise needed** | Any Workbench-trained tech can inspect/maintain | Requires Java/Niagara SDK developer to modify |
| **N4 major version upgrade** | Components survive; no recompile; fully upgrade-safe | Module must be recompiled and re-signed per SDK contract |
| **Logic change propagation** | Edit ~330 copies individually → high drift risk | Edit one class → all instances update on next deploy |
| **Station BOG size** | +~1670 objects (more XML, larger bog file) | Small object count (one container type ×330 light instances) |
| **Dashboard binding** | ~330 × 2 = 660 override/selector points to bind | Same point count, but naming may be cleaner via typed slots |
| **Module signing / cert** | Not required | Required for N4 (module certificate, Niagara developer license) |
| **Debugging** | Visible in wiresheet; any tech can trace a link | Requires source access + Java build toolchain |
| **Fail on block-name unavailability** | None — blocks confirmed in B537 [CERT] | None — but SDK breakage on N4 upgrade possible |

**Summary judgment `[INFER]`:** Route B is the right choice when the operator/integrator has no Niagara
module development capability or wants zero long-term module maintenance risk. The principal cost is the
one-time build labor (~330 copy-paste + re-link sessions) and the permanent inability to fix logic globally.
Route A dominates if the operator expects logic iteration or has the developer resources.

## 8. Migration and fail-safe (summary) `[INFER]`

Reference B842 §7 for the migration strategy. Route B adds no new risks beyond that design:
- The priority array `[CERT-live]` B841 §3 ensures `in16` fallback (`true`) holds the relay if the
  Supervisor loses network or the new logic produces null.
- Per-circuit migration (unlink legacy → link new → validate → delete legacy) keeps circuits live throughout.
- A station backup `[CERT-live]` B841 + B838 before each wave of ~20-circuit migrations limits blast radius.

---

## Self-verify

| # | Claim | Marker | Evidence |
|---|-------|--------|----------|
| 1 | `BBooleanSelect`: N-way MUX, `select` ordinal, `inA..inJ`, `numberValues` 3–10, `zeroBasedSelect`, 1-based default | [CERT] | B537 §537.3/5, `BSwitch.java:125-142` |
| 2 | `BNumericSwitch`: 2-way numeric MUX, `inSwitch` boolean routes `inTrue`/`inFalse` | [CERT] | B537 §537.3/5 |
| 3 | `BEqual`, `BAnd`, `BOr`, `BNot`: confirmed logic package (B537 §537.3) | [CERT] | B537 §537.3 logic list |
| 4 | `BNumericConst`: confirmed constants package | [CERT] | B537 §537.3 constants list |
| 5 | Invalid `select` → hold-with-invalid (not null, not zero) | [CERT] | B537 §537.5, `BBooleanSwitch.java:72-90`, `BSwitch.java:125-142` |
| 6 | `BBooleanSelect` recommended over `BEqual`+`BAnd`+`BOr` chain or chained `BBooleanSwitch` | [INFER] | design choice (§2): 4 blocks vs 9 or 6 blocks; equivalent behavior, fewer links |
| 7 | Type-compatibility of `NumericWritable.out` → `BBooleanSelect.select` must be verified in Workbench | [INFER] | `select` typed as "enum ordinal" (B537); exact N4 slot type not code-confirmed in this block |
| 8 | FollowPanel resolver: `BEqual(perCircuitSel, 0)` → `BNumericSwitch(inSwitch, panelSel, perCircuitSel)` | [INFER] | design realizing B842 §3 formula |
| 9 | HOA override via nullable `BooleanWritable.out` → `SchdlGMnn Rk.in8`; null = Auto (release) | [INFER] | composed from `[CERT-live]` B841 §3 (in1-15 free) + writable null-release semantics |
| 10 | No dedicated kitControl HOA block confirmed in B537 catalog | [CERT] | B537 §537.3 util (37) + logic (13) — none named BHOASelector or equivalent |
| 11 | 4 master BooleanSchedules on Supervisor; legacy per-circuit schedules decommissioned | [INFER] | B842 §5; scheduling runs on Supervisor `[CERT-live]` B841 §8 |
| 12 | ~330 circuits × 6 objects = ~2000 new Supervisor components; net +1670 after legacy removal | [INFER] | arithmetic from B841 §2 circuit counts |
| 13 | No "update template and propagate" in stock Niagara → drift risk on logic change | [INFER] | Niagara N4 wiresheet behavior; no templating system in kitControl |
| 14 | Route B survives N4 major upgrade without recompile; Route A requires module recompile | [INFER] | kitControl is platform-bundled; custom modules are SDK-compiled against a version |
| 15 | Priority in8 is free; in16 fallback stays; downstream JACE unchanged | [CERT-live] | B841 §3 (in10=schedule, in16=fallback, in1-15 free) |

**Tally:** 15 claims — 5 [CERT] (B537 block-catalog and behavior), 1 [CERT] (absence of HOA block),
9 [INFER] (design choices, composition, scale arithmetic, drift risk, upgrade analysis),
0 unmarked assertions. No [CERT-live] claims — all live facts are attributed to B841 which holds them.

## Connections

- **B841** — the `[CERT-live]` evidence floor: the chain `Schedule.out → in10 → JACE → BACnet.in16`,
  the free priority slots in1–in15, and the ~330 circuits this pattern must cover.
- **B842** — the unified control model this block realizes in kitControl wiring; §3/7 are the design
  sources for the FollowPanel formula and migration strategy.
- **B843** — Route A (custom module); §7 of this block is the head-to-head comparison.
- **B537** — the kitControl function-block catalog; all confirmed block names and behaviors in this block
  trace here.
- **B838** — the backup/restore procedure; each migration wave should be preceded by a clone backup of
  HM_BMS and HM_Central.
- **B845** — the operator dashboard; it binds to `perCircuitSel`, `panelSel`, and `Override` writables
  produced by this design.

## Open gaps

- **B844-G1** — Verify `NumericWritable.out` → `BBooleanSelect.select` link compatibility in Workbench on
  N4 4.3.x without a conversion block. If a type mismatch is reported, characterize the required conversion
  (e.g., `BNumericToStatus` or an `EnumConst`) and update the circuit template accordingly.
- **B844-G2** — Measure the real config.bog size delta after deploying the template to one full panel
  (~20–32 circuits). If the Supervisor station load time or memory footprint increases beyond acceptable
  bounds, evaluate switching to the Program block / BQL expression approach (`BBqlExprComponent` is listed
  in B537 util but not decompiled in this block — gap for that path).
- **B844-G3** — Enumerate the writable naming per panel beyond GM01 (B841-G3 is still open): if any
  panel uses a different naming convention for `SchdlGMnn Rk`, the link step in §6 must be adapted per
  panel before bulk replication, or the copy-paste will silently link to a wrong or nonexistent slot.
