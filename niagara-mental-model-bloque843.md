# Block 843 — HARBOR lighting redesign Route A: a custom N4 module (reusable schedule-selector component)

> **Focus:** harbor-greenmax-lighting. **Scope:** the engineering DESIGN for realizing the B842 control model
> as a **custom Niagara N4 module** on the Supervisor HM_BMS — a reusable `@NiagaraProperty`/`@NiagaraAction`
> component pair (panel container + per-circuit selector) that exposes the B842 §6 control contract, reads the
> 4 master schedules, and re-feeds the existing `SchdlGMnn Rk` writables. This is **one of two routes**: B844
> builds the identical behavior with kitControl only. Both implement **B842** (do not restate it — referenced).
>
> **Evidence base:** the live chain, priority levels, and write targets are **`[CERT-live]` from B841** (cited,
> not re-derived). Niagara-framework facts (slotomatic, the 16-level priority array, frozen enums) are `[CERT]`
> against the decompiled corpus (block/line cited). **Every design claim is `[INFER]`** — an engineering proposal,
> marked as such; nothing downstream is asserted as verified behavior of code that does not yet exist.
>
> **Sources:**
> - **B841 `[CERT-live]`** — current per-circuit chain, `SchdlGMnn Rk` BooleanWritable (in10 schedule / in16 fallback, in1–in15 free), schedules run on the Supervisor.
> - **B842 `[INFER]`** — the unified control model this module implements (4 masters, two-level selector, HOA, §6 contract, §7 migration).
> - **B838 `[CERT-live/doc]`** — HM_Central/HM_BMS on Baja 4.3.58.18; the 4.3→4.15 recompile boundary (§7).
> - **Corpus `[CERT]`** — B4/B12 (slotomatic + @NiagaraProperty/@NiagaraAction), B6 §6.2.6 + B7 §7.2.9 (16-level priority array), B4 §4.2.6 (BFrozenEnum + @NiagaraEnum/@Range).
> - **Team modules (pattern reference)** — ColdRoomPan / CompPan / DashboardPan: real split-design rt modules (`com.angeles.*`, pure-logic + `B…` Baja adapter, slotomatic, Java-8 build, verify gate).

---

## 1. Why a module, and where it lives `[INFER]`

The B842 logic is **identical for all ~330 circuits** (B841 §2 `[CERT-live]`) — the textbook case for one reusable
type instantiated N times, rather than ~330 hand-wired wiresheets (B844). A module gives: one place to fix the
mux/HOA logic, a versioned artifact, a clean `config.bog` (a handful of typed components instead of hundreds of
kitControl blocks), and stable slot names for the B845 dashboard to bind.

The module is installed on the **Supervisor HM_BMS (N4)** — that is where the `BooleanSchedule`s execute and where
`SchdlGMnn Rk` lives (B841 §3,§8 `[CERT-live]`). The JACE HM_Central and the BACnet `Relay[k].BO` path are **not
touched**. Station is **N4.3.58.18** → the rt jar targets **4.3** (B838 live). A future 4.3→4.15 upgrade forces a
**recompile** of this module (QNX/OS + API boundary, B838 §7) — flag it as a maintained artifact with that cost.

## 2. The two types — a panel container holding circuit children `[INFER]`

Mirror the team's split (pure logic + Baja adapter, like ColdRoomControl/BColdRoom and CompressorControl/BCompressorControl):

- **`BGreenMaxPanel extends BComponent`** — one instance per GreenMAX panel (16 total). It owns the 4 master-schedule
  **inputs** (so the 4 links land **once per panel**, not once per circuit), the panel-level default selector, and
  an optional panel HOA. It holds its circuits as **child components** and drives them each cycle.
- **`BGreenMaxCircuit extends BComponent`** — one instance per lighting output (~330 total), a child of its panel.
  It carries the per-circuit selector, the per-circuit HOA, the read-only feedback, and the **two outputs** linked
  to its `SchdlGMnn Rk` writable.

Pure resolution logic (the mux + HOA truth table) lives in a **plain `LightingResolver` class with no Baja imports**,
JUnit-testable in WSL (the team's `*Control.java` convention; ColdRoomPan/CompPan). The `B…` classes are thin adapters.

## 3. Slots — the B842 §6 contract, concretely `[INFER]`

Using the team's annotation style (`@NiagaraProperty` → slotomatic generates fields/getters between markers, B4/B12 `[CERT]`):

**`BGreenMaxPanel`:**
```java
@NiagaraProperty(name="panelScheduleSel", type="BScheduleSel",      // frozen enum H1|H2|H3|H4
    defaultValue="BScheduleSel.H1", flags=Flags.OPERATOR)
@NiagaraProperty(name="panelHoa", type="BHoaMode",                  // frozen enum Hand|Off|Auto
    defaultValue="BHoaMode.Auto", flags=Flags.OPERATOR)
// 4 master-schedule inputs — linked ONCE per panel from the 4 masters (§5)
@NiagaraProperty(name="h1In", type="BStatusBoolean", flags=Flags.READONLY|Flags.TRANSIENT)
@NiagaraProperty(name="h2In", type="BStatusBoolean", flags=Flags.READONLY|Flags.TRANSIENT)
@NiagaraProperty(name="h3In", type="BStatusBoolean", flags=Flags.READONLY|Flags.TRANSIENT)
@NiagaraProperty(name="h4In", type="BStatusBoolean", flags=Flags.READONLY|Flags.TRANSIENT)
```

**`BGreenMaxCircuit`:**
```java
@NiagaraProperty(name="circuitScheduleSel", type="BCircuitSel",     // frozen enum FollowPanel|H1|H2|H3|H4
    defaultValue="BCircuitSel.FollowPanel", flags=Flags.OPERATOR)
@NiagaraProperty(name="circuitHoa", type="BHoaMode",
    defaultValue="BHoaMode.Auto", flags=Flags.OPERATOR)
// read-only feedback for the dashboard (B842 §6 / B845)
@NiagaraProperty(name="effectiveState",    type="BStatusBoolean", flags=Flags.READONLY|Flags.TRANSIENT)
@NiagaraProperty(name="effectiveSchedule", type="BStatusEnum",     flags=Flags.READONLY|Flags.TRANSIENT)
// outputs → linked to the circuit's SchdlGMnn Rk writable (§4)
@NiagaraProperty(name="autoOut", type="BStatusBoolean", flags=Flags.READONLY|Flags.TRANSIENT) // → .in10
@NiagaraProperty(name="ovrOut",  type="BStatusBoolean", flags=Flags.READONLY|Flags.TRANSIENT) // → .in8
@NiagaraAction(name="forceRecalc", flags=Flags.HIDDEN)             // optional manual poke
```

Frozen enums via `@NiagaraEnum`+`@Range` on a `BFrozenEnum` subclass (B4 §4.2.6 `[CERT]`) — `BScheduleSel`,
`BCircuitSel`, `BHoaMode` — so each selector renders as a **dropdown** (facets `range`→`BEnumRange`, B4/B15 `[CERT]`),
which is exactly the "HOA-style selector with 4 options" the operator asked for (B842 §1).

## 4. How one circuit resolves and writes `[INFER]` (implements B842 §4)

Each execute cycle, the **panel** loops its `BGreenMaxCircuit` children and, per child, runs the pure resolver:

```
effectiveSel = (circuitScheduleSel == FollowPanel) ? panelScheduleSel : circuitScheduleSel   // B842 §3
scheduled    = MUX(effectiveSel) over {h1In..h4In}                                            // B842 §4
autoOut      = {scheduled}                                                    → SchdlGMnn Rk.in10  (priority 10)
ovrOut       = Hand:{true}  |  Off:{false}  |  Auto:{null-status}             → SchdlGMnn Rk.in8   (priority 8)
effectiveState/effectiveSchedule = feedback for the dashboard
```

- **`in10`** receives the *selected* master instead of a dedicated schedule — the single structural change (B841 §3 `[CERT-live]` that in10 is the schedule slot). A `BStatusBoolean`→`in10` **data link** carries it.
- **`in8`** carries HOA. Releasing Auto is done by writing a **null-status** value: in a 16-level priority array a
  NULL at an index **relinquishes that level** and the array falls through to the next non-null (in10) — `[CERT]`
  B6 §6.2.6 / B7 §7.2.9,214. So `ovrOut` with an invalid/null status on Auto drops cleanly back to the schedule.
  (Choice of **in8** is `[INFER]`; B842-G2 / B843-G2 debate a dedicated emergency level.)
- **`in16`** fallback stays untouched; JACE→BACnet `Relay[k].BO.in16` downstream is unchanged (B841 §3 `[CERT-live]`).

Writing occurs through standard **BLinks** from the circuit's output slots to the writable's `in8`/`in10` — these are
the only new links into the existing chain, one pair per circuit. (Alternative: a programmatic `set()` on the writable
from the adapter; links are preferred for visibility and the team's `bog-audit` proxy-link checks.)

## 5. Reading the 4 master schedules `[INFER]`

Four `BooleanSchedule`s at e.g. `Config/Iluminacion/Horarios/Horario1..4` on the Supervisor (B842 §5). Each panel's
`h1In..h4In` are **linked once** from those four `.out`s (16 panels × 4 = **64 links total**, vs 330×4 if each circuit
linked directly — the container design collapses the link count). The circuits read the masters **through their panel**.
Links are preferred over hard-coded ORDs so there is no `station:|slot:` literal in source (the team's `ord-literal`
lint WARN, build skill). Editing one master changes every circuit pointed at it — the headline win (B842 §5).

## 6. Deployment shape and scale `[INFER]`

**Shape:** one `BGreenMaxPanel` per GreenMAX panel (**16**), each holding its circuit children (7–32 each, **~330**
total, B841 §2). This matches the physical topology and the per-panel dashboards (`GreenMAXnn TabALxx.px`, B841 §4),
and keeps the 4-master links at the panel boundary.

**Instantiation at scale** — ~330 children is too many to place by hand:
- Build **one template panel** fully wired (4 master links + children + the `in8/in10` links for each child), then
  **copy per panel** and re-point the children's output links to that panel's `SchdlGMnn Rk` writables.
- Or drive instantiation with a **BajaScript / Program object** that reads the existing `GMnnilum` folders and spawns
  a circuit child + its two links per `SchdlGMnn Rk` found — the fastest path to 330 and the least error-prone, since
  the writable names are the authority (confirm naming first — B841-G3).
- The per-circuit migration is itself additive and reversible (B842 §7): link the new `autoOut`→`in10` **as** the
  legacy schedule is unlinked from `in10`, circuit by circuit, so no slot is ever dark or double-driven.

## 7. Build & deploy reality on 4.3 `[CERT(process)/INFER]`

The team's discipline (build skill) applies verbatim — this is an **rt logic** module (`types/logic.md`):
1. Source split: pure `LightingResolver` (+ JUnit) and `BGreenMaxPanel`/`BGreenMaxCircuit`/enum adapters; package `com.angeles.GreenMaxLighting` (team convention). `defaultModuleVersion` in the GROUP `build.gradle.kts`.
2. Build in **WSL with Java 8** + **slotomatic** for the rt profile (`@NiagaraProperty/@NiagaraAction` edits must land
   in the annotation **and** the generated region **and** imports — build skill Hard Rules), then **sign** the jar.
3. **Verify gate** (`verify-module.sh`): bytecode **major 52**, NIAGARA4.SF signed, `module.xml` types resolve;
   pre-gate lints (delays/timers, `lint-write-path` for every OPERATOR slot, `slot-coverage` lexicon). A jar that has
   not passed the gate does not reach the station.
4. **Install on the Supervisor HM_BMS** (where schedules run), not the JACE. Target **4.3** — and record that a
   **4.3→4.15 upgrade forces a recompile** of this module (B838 §7 `[CERT-doc]`).
5. After load, **triage the console** (`triage-console.sh`) for own-module load/exception rows before calling it clean.

## 8. Trade-offs vs the kitControl route (B844) `[INFER]`

**Route A (this module) PROS:** one reusable component — one place to fix logic; versioned + signed artifact;
**clean `config.bog`** (≈16 panels + ≈330 typed circuit children vs hundreds of kitControl blocks + a web of links);
stable slot names → **easy, terse dashboard binding** (B845); the 4-master link count collapses to 64 at the panel
boundary; JUnit-testable pure resolver.

**Route A CONS:** you must **build, sign, and deploy a module** (dev effort up front, a toolchain, the verify gate);
**recompile on every N4 major upgrade** (4.3→4.15, B838 §7); it **adds a maintained artifact** the client's future
integrators must keep (source, build env, signing); heavier than "just wiring blocks" if the logic never needs to
change. B844 (kitControl) trades all of that for **no module** at the cost of **~330 replicated wiresheet patterns**
and a far busier bog. The decision hinges on: does HARBOR want a maintained software artifact, or a self-contained
station an EC-Net integrator can service with Workbench alone? (Open — the route recommendation is B842/B844's synthesis.)

## 9. Migration & fail-safe — see B842 §7 `[INFER]`

Do not restate: the cutover is **additive, per-circuit** (link `autoOut`→`in10` as the legacy schedule is unlinked),
the **`in16` fallback** holds any circuit in a known state if the module is unavailable, and reversibility rests on the
B838 station backup plus the link-level, per-circuit nature of the change. The module changes **only how `SchdlGMnn Rk`
is fed** on the Supervisor (B842 §4).

---

## Self-verify

| # | Claim | Marker | Evidence |
|---|-------|--------|----------|
| 1 | Schedules run on the Supervisor; `SchdlGMnn Rk` = local BooleanWritable, in10 schedule / in16 fallback, in1–in15 free; JACE→BACnet unchanged | [CERT-live] | B841 §3,§8,§10 |
| 2 | ~330 circuits across 16 panels, identical logic → reusable type ×N | [CERT-live] | B841 §2 (counts); [INFER] the "reusable type" conclusion |
| 3 | Station is Baja 4.3.58.18 → target 4.3; 4.3→4.15 forces a module recompile | [CERT-live]+[CERT-doc] | B838 live context + B838 §7 (QNX6.5→7.0 / OS boundary) |
| 4 | slotomatic generates fields/getters from @NiagaraProperty/@NiagaraAction between markers | [CERT] | corpus B4 §§161–181, B12 §§26,102–106 |
| 5 | 16-level priority array: scan in1→in16, highest non-null wins, writing NULL relinquishes a level | [CERT] | corpus B6 §6.2.6, B7 §7.2.9,§214 |
| 6 | Frozen enums via BFrozenEnum + @NiagaraEnum/@Range render as a dropdown (facets range→BEnumRange) | [CERT] | corpus B4 §4.2.6,§§244–255; B15 §300 |
| 7 | Module design: BGreenMaxPanel container + BGreenMaxCircuit children, pure resolver + Baja adapter split | [INFER] | design; mirrors team ColdRoomPan/CompPan split |
| 8 | §3 slot set (panelScheduleSel/circuitScheduleSel/HOA/effectiveState+Schedule/autoOut→in10/ovrOut→in8) realizes the B842 §6 contract | [INFER] | design mapping onto B842 §4,§6 |
| 9 | 4 masters linked once per panel (64 links) not per circuit (1320) | [INFER] | design; container collapses link count |
| 10 | Auto release = null-status value on in8 drops through to in10 | [INFER] built on [CERT] | relinquish semantics B7 §214; in8 choice is the [INFER] |
| 11 | Deploy on Supervisor HM_BMS via rt jar + Java-8 slotomatic + sign + verify gate (major 52) | [INFER]+[CERT(process)] | build-n4-module SKILL; schedules on Supervisor B841 §8 |
| 12 | Instantiation at scale = template-copy or BajaScript/Program spawn ×330 | [INFER] | design; writable names are the authority (B841-G3) |
| 13 | Trade-offs vs B844 (module PROS/CONS) | [INFER] | engineering comparison |
| 14 | Migration additive/per-circuit + in16 fail-safe | [INFER] | references B842 §7 |

**Tally:** 14 claims — 3 framework `[CERT]` (corpus-cited), 1 `[CERT-live]` + 1 mixed `[CERT-live]+[CERT-doc]`,
1 mixed `[INFER]`-on-`[CERT]`, 8 `[INFER]` design proposals. Every `[INFER]` is an explicit engineering choice, not a
disguised fact; every factual anchor traces to B841 `[CERT-live]`, B838, or a cited corpus block. No invented file:line.

## Connections
- **B841** — the `[CERT-live]` evidence floor: the chain, priority levels, and the `SchdlGMnn Rk` write targets this module re-feeds.
- **B842** — the control model this module implements (4 masters, two-level selector, HOA, §6 contract, §7 migration). Do not duplicate; this block is the *how*.
- **B844** — the kitControl-only route; §8 is the head-to-head trade-off.
- **B845** — the operator dashboard binds the §3 OPERATOR/READONLY slots (the B842 §6 contract).
- **B838** — 4.3 platform + the 4.3→4.15 recompile boundary (§7 here) + backup for migration reversibility.
- **Team modules (ColdRoomPan/CompPan/DashboardPan)** — the pure-logic + Baja-adapter split, slotomatic, Java-8 build, and verify-gate discipline §7 reuses.

## Open gaps (RESEARCH-STATE-harbor-greenmax-lighting)
- **B843-G1** — Write mechanism for the HOA override: a data link carrying a null-status `BStatusBoolean` to `in8` vs a programmatic `set()`/override action on the `SchdlGMnn Rk` writable — confirm which relinquishes cleanly on this 4.3 BooleanWritable (bench-test on a real station/writable → `[CERT-live]`).
- **B843-G2** — Override priority level: `in8` for operator HOA vs a dedicated emergency `in1` + operator `in8`, and whether "Off" must beat a future emergency-ON (ties to B842-G2).
- **B843-G3** — Scale instantiation: prove the BajaScript/Program spawn-per-`SchdlGMnn Rk` approach against the real Supervisor bog (depends on B841-G3 writable-naming confirmation) and measure Supervisor resource headroom for ~330 new components + links.
</content>
</invoke>
