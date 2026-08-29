# Niagara N4 — chihuahua-source (CS2): the rt model is a monitor/dashboard with ONE computed output (`effectiveSetpoint`) + software overload/antifreeze protections whose faulted-sensor behavior is directional — fail-SAFE for antifreeze (low-limit), fail-to-NON-trip for overload (high-limit)

**Focus**: chihuahua-source · **Gap**: CS2 (rt control/equipment model + protection/defensive) · **Session**: 2026-08-29 · **Block**: B650
**Sources** (`[CERT]` REAL source): `chihuahua-rt/src/com/angeles/chihuahua/components/` — `BChiUp.java` (2092 L), `BChiUpMonitor.java`, `BChiCarcamo.java`, `BChiDatalogger.java`, `BPlanta.java`, `BChiDashboardService.java` (1522 L), `../ChiLinkHelper.java`.

**Scope**: the runtime control/protection logic + defensive behavior of the production module. Safety axis parallels [B543] (kitControl fail-safe). Servlet write-auth = [B648] (REMIT).

---

## 650.1 Component model: containers + a one-shot factory + a service tick

`[CERT]` — `BChiUp` (packaged HVAC unit, `BChiUp.java:362`, ~36 slots: 22 read-only feedback like `ampCompresor1/2`/`tempZona`, 5 writable commands `setpoint`/`fanCmd`/`compNCmd`/`modoOperacion`, 7 threshold slots `sobrecarga*`/`antifrezze*`, 8 `protXActive` BStatusBoolean outputs, `alarmLatches` JSON, `effectiveSetpoint` READONLY-computed, `@NiagaraAction resetAlarmas`). `BChiCarcamo` (sump level `nivelCm` + 2 thresholds) and `BChiDatalogger` (`pressurePsi`/`pressureBar` + thresholds) are pure containers. `BPlanta` (`BPlanta.java:36`) is the plant root that idempotently `ensureChild`s the three monitors on `started()` (`:107-109`).

The **`XMonitor` = one-shot auto-factory, not a poll** `[CERT]` `BChiUpMonitor.java:71-157`: on `started()` it seeds `BChiUp` children from a static 60-entry `UP_DATA` array, then is passive (+ a self-heal pass for stale `planta==0.0`, `:249-278`). Live values arrive via **integrator-wired Workbench slot links**, not Monitor polling.

---

## 650.2 Does it CONTROL hardware? Mostly monitors; one computed output

`[CERT]` — the module does NOT directly command hardware. Command slots (`fanCmd`/`compNCmd`/`setpoint`) are linked by the integrator to physical BACnet points in Workbench; `protXActive` slots are "wired in Workbench to physical output channels" (`BChiUp.java:278-279`). The ONE output the module itself computes+writes is **`effectiveSetpoint`** (`recomputeEffectiveSetpoint`), propagated to the controller by an integrator-maintained link that `ChiLinkHelper.importLinks` restores (`ChiLinkHelper.java:648-656`). So chihuahua is a **monitor/dashboard + a schedule→effectiveSetpoint computed output**, plus SOFTWARE protection commands (§650.3) that write the command slots off. It is not an autonomous controller, but it DOES issue protective shutoffs.

---

## 650.3 The protection engine + the directional fault behavior (the safety finding)

`[CERT]` `BChiDashboardService.applyProtections` (`:1047+`, run every 10 s `controlTick` + immediately on COV of an amp slot via `changed()`→`scheduleCovEvaluate`, [B650§650.4]) evaluates thresholds and writes command slots off on a trip:
- **Overload** (`:65-83`, high-limit): `if (sobrecargaC1 > 0.001) if (ampC1 > sobrecargaC1) → writeBool(comp1Cmd,false)`; fan overload cascades off comp1+comp2; abanicos trip comp only (operator decision).
- **Antifreeze** (`:30-53`, low-limit): `if (antifrezze1 > 0.001) if (tempS1 < antifrezze1) → fanCmd+compCmd false`.

Sensor reads go through `readSlotVal` `[CERT]` `BChiDashboardService.java` (readSlotVal impl): it DOES guard status —
```java
if (sn.getStatus()!=null && (sn.getStatus().isFault() || sn.getStatus().isNull())) return 0.0;
return sn.getValue();
```
**A faulted/null sensor collapses to 0.0.** That has DIRECTIONAL safety consequences (the load-bearing CS2 finding):
- **Antifreeze (low-limit) → fails SAFE**: a faulted suction-temp reads 0.0, which is `< antifrezze1` (e.g. ~2 °C) → TRIPS → shuts fan/comp off. A dead temp sensor errs toward shutdown. ✓
- **Overload (high-limit) → fails to NON-trip**: a faulted amp sensor reads 0.0, which is NOT `> sobrecargaC1` → does NOT trip → **the overload protection silently disables if its current sensor faults.** A real overcurrent with a dead amp sensor goes undetected. ✗ (Mitigant: 0.0 A also reads as "equipment off", and the physical BACnet controller has its own protections — but the DASHBOARD-layer overload guard is fail-to-danger.)

Also `[CERT]`: a faulted THRESHOLD slot → 0.0 → `> 0.001` false → the whole protection for that channel is SKIPPED (both directions). Latches are **permanent** (hysteresis deleted, `BChiUp.java:1534-1537`) — a trip stays until `doResetAlarmas` (`:2022-2076`) clears `alarmLatches`, syncs `protXActive`, and `clearTripped`. This is consistent with [B543]'s kitControl finding: a status-guard exists (good, better than raw reads), but the 0.0-collapse is fail-safe only for low-limit protections. **Recommendation**: for high-limit (overload) protections, treat a faulted amp sensor as a FAULT alarm (not 0.0), so a dead sensor is surfaced rather than silently disabling protection.

---

## 650.4 Defensive posture (strong at the component layer)

`[CERT]` — extensive guards: null parent/property in every Monitor `readParentPlantaIndex` (`BChiCarcamoMonitor.java:93-100`); NaN/Inf check with 0.0 fallback (`BChiUp.java:1686-1695`); epsilon write-loop guard (`:1707-1711`); `getSetpoint()` null → fallback **18.0** ("safe base"); malformed `alarmLatches` JSON → reset to `{}` (`:1794-1800`); **event-thread must-not-throw** (ADR-D7, `:1644-1649` swallows Throwable); null-service no-op (R-11, `:1633-1640`); steady-state boot guard on link import (`ChiLinkHelper.java:669-676`, refuses to write placeholder setpoints to live controllers before `Sys.atSteadyState()`). Per-Ord `ReentrantLock` serializes protection eval. This is a genuinely defensive codebase — the directional overload-fault gap (§650.3) is the one hole in an otherwise careful design.

`ChiLinkHelper` `[CERT]` is a station-side **link backup/restore** (not a wb editor): `collectLinks` walks `getLinks()`/`getKnobs()` → `chih-links.json` (hand-rolled JSON, atomic write); `importLinks` recreates BLinks with the steady-state guard, `getSlot()` (not `get()`, which throws for Action slots) and `cleanStaleLinks`. It persists link topology across station restarts.

---

## 650.5 Build/quality notes

`[CERT]` — **stale slotomatic**: `BChiUp.java:279,346,1268` carry `AWAITING SLOTOMATIC REGEN (hash 3236798429)` — the 8 `protXActive` slots + `resetAlarmas` action were added AFTER the last slotomatic run. This is exactly [B637]'s variant rule violated: `@Niagara*` slots changed without a Clean+Slotomatic+Build. It currently compiles (slot fields hand-present, `:1272`) but the AUTO region hash is stale — run `gradlew slotomatic` before the next release. Clean signals: **no `System.out`, no TODO/FIXME** across all 7 files. Tech-debt (documented): `BChiUp` is 2092 L (grew from "pure container" — stale Javadoc); 17 Oficinas floor units modeled as `BChiUp` despite physical type mismatch ("Option A"); `purgeAlarmLatches` (~100 L) embeds a hand-rolled JSON scanner (no external lib — a shop pattern, cf. ChiJsonUtil).

---

## Self-verify

| # | Claim | Marker | Citation | Checked |
|---|---|---|---|---|
| 1 | BChiUp=packaged unit ~36 slots; Monitor=one-shot factory (UP_DATA seed, not poll); BPlanta ensureChild ×3 | [CERT] | BChiUp.java:362 · BChiUpMonitor.java:71-157 · BPlanta.java:107-109 | ✅ read |
| 2 | module = monitor/dashboard; only effectiveSetpoint self-computed+written; commands+protX via integrator links | [CERT] | BChiUp.java:278-279 · ChiLinkHelper.java:648-656 | ✅ read |
| 3 | readSlotVal collapses faulted/null BStatusNumeric to 0.0 | [CERT] | BChiDashboardService.java readSlotVal | ✅ read verbatim |
| 4 | antifreeze (low-limit) faulted→0.0<thr→TRIP=fail-SAFE; overload (high-limit) faulted→0.0>thr false→NO trip=fail-to-danger | [CERT] | applyProtections :30-83 + readSlotVal | ✅ read verbatim |
| 5 | permanent latches (hysteresis deleted); doResetAlarmas is the only clear | [CERT] | BChiUp.java:1534-1537,2022-2076 | ✅ read |
| 6 | strong defensive guards (null/NaN/epsilon/JSON/event-thread ADR-D7/steady-state) | [CERT] | BChiUp.java:1644-1711 · ChiLinkHelper.java:669-676 | ✅ read |
| 7 | stale slotomatic AWAITING REGEN on protXActive (B637 variant rule); no System.out/TODO | [CERT] | BChiUp.java:279,1268 + grep | ✅ read+grep |

**Tally**: [CERT] ×7 · [INFER] ×0 · real-source block. The safety-critical `readSlotVal` + applyProtections directions token-checked verbatim. Sweep left the fault-guard "in service layer, unsurveyed"; driver read it to resolve the safety question.

## Connections

- **[B543]** — kitControl fail-safe thread; same status-guard-but-directional finding. **[B648]** — servlet write-auth (the write commands here are separate from the servlet). **[B637]** — slotomatic variant rule (stale hash here). **[B647]/[B636]** — chihuahua audit/template.
- Forward: CS6 (the internal audit-2026-05-06 may already note the overload-fault or latch behavior), CS8 (verdict: strong defensive design + one overload-fault recommendation + run slotomatic).

## Gaps uncovered

- None new for the backlog. The physical BACnet controller's own overload protection (whether it backstops the dashboard-layer fail-to-danger) is out of scope (integrator config, not module source) — noted for CS8's recommendation.
