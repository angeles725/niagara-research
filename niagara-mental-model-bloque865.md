# Block 865 — Spyder residuals: function-block property tables (B836-G1) + FTT-10 free-topology termination rules (B837-G1)

> **Focus:** spyder-commissioning. **Gaps closed:** B836-G1 (per-block property tables for commissioning-relevant
> Spyder function blocks) and B837-G1 (FTT-10 free-topology termination detail). These are residual children of
> B836 (Spyder Tool function-block reference) and B837 (LON/Classic Spyder path).
>
> **Sources:** `[CERT-doc]` — `niagara-help/guides-clean/HoneywellFunctionBlocks/` individual block guides (Pid.txt,
> Stager.txt, StageDriver.txt, OccupancyArbitrator.txt, RateLimit.txt, SetTemperatureMode.txt); registered in SOURCES
> at B836. FTT-10 values from `[CERT-doc]` — `niagara-help/docs-text/en2z1002-ge51r0923_-_Eagle_Controller_Communication_Interfaces.txt`
> (EN2Z-1002GE51 R0923) §LON Termination + §Free topology layout examples; corroborated by
> `docs-text/CIPer_Model_50_Installation_and_Commissioning_Instructions_-_31-00233.txt` (31-00233-03) and
> `docs-text/en1z1039-ge51r0923-_EAGLEHAWK_NX_Controller_Installation_Commissioning_Instructions.txt`.
> Both reference Echelon doc 078-0156-01F as the canonical full spec (not in our source set).

---

## 1. B836-G1 — Commissioning-relevant function-block property tables `[CERT-doc]`

The guides use four table types per block: **Logic Inputs**, **Analog Inputs** (with Low/High range, unconnected
and invalid behavior), **Outputs**, and **Setpoints/Configuration**. The tables below distil the commissioning-
relevant parameters (name, range, what happens on unconnected/invalid). Block behavior is from the guides
registered in SOURCES at B836.

### 1.1 PID (`-Pid.txt`)

**Analog Inputs** (all disable PID and set Output=0 when unconnected or invalid):

| Input | Low | High | Unconnected / invalid behavior |
|-------|-----|------|-------------------------------|
| sensor | −∞ | +∞ | PID disabled, Out=0 |
| setPt | −∞ | +∞ | PID disabled, Out=0 |
| tr (throttling range) | 0< | +∞ | PID disabled, Out=0 (also for tr=0) |
| intgTime (sec) | 0 | +∞ | unconnected → disabled; invalid/0 → Integral disabled only |
| dervTime (sec) | 0 | +∞ | unconnected/invalid/0 → Derivative disabled only |
| deadBand | 0 | <tr | unconnected/invalid/val<0 → DB=0 (no deadband) |
| dbDelay (sec) | 0 | 65535 | unconnected/invalid/0 → deadband active with no delay |

**Output:** `−200 to +200 %` or `0 to 100 %` (configured via Output Range setpoint).

**Setpoints / Configuration:**

| Name | Range | Meaning |
|------|-------|---------|
| revAct | 0–2 | 0=direct acting, 1=reverse acting, 2=tr-sign determines direction |
| bias | 0–100 % | proportional offset |
| Output Range | 0=−200..+200, 1=0..100 | selects output range |

**Key rule:** change to intgTime → must Stop + Start the Sequenced Control Engine (verbatim in guide).
Integral wind-up limit = 100 % of integral portion.

### 1.2 Stager (`-Stager.txt`)

Converts a 0–100 % command (typically PID output) into a count of stages ON.

**Analog Inputs** (all return stgsAct=0 when unconnected or invalid):

| Input | Low | High | Notes |
|-------|-----|------|-------|
| in % | 0 | 100 | Command percentage |
| maxStgs | 1 | 255 | Maximum stages available |
| minOn (sec) | 0 | 65535 | Minimum time stage stays ON once turned on |
| minOff (sec) | 0 | 65535 | Minimum time stage stays OFF once turned off |
| intstgOn (sec) | 0 | 65535 | Min time before next stage can be turned ON after the previous one |
| intstgOff (sec) | 0 | 65535 | Min time before next stage can be turned OFF after the previous one |

**Setpoints / Configuration:**

| Name | Range | Meaning |
|------|-------|---------|
| hyst | 0–100 | Switching hysteresis around switch points in % error |
| CPH | 0–60 | Cycles per hour (0=staging only, 1–60=thermostat cycler mode) |
| AnticAuth | 0–200 | Anticipator authority % (cycler only, CPH≠0) |

**Output:** `STAGES_ACTIVE` (0 to maxStgs count), passed to StageDriver.

### 1.3 StageDriver (`-StageDriver.txt`)

Energizes individual stage outputs from the count produced by Stager.

**Analog Inputs:**

| Input | Low | High | Notes |
|-------|-----|------|-------|
| nStagesActive | 0 | 255 | From Stager output; unconnected/invalid → all stages off |
| runtimeReset | 0 | 255 | Stage number to reset runtime to 0 (LL_RUNEQ only); 0/unconnected=no reset |

**Configuration Parameters:**

| Name | Values | Meaning |
|------|--------|---------|
| leadLag | 0=LL_FILO, 1=LL_FOFO, 2=LL_RUNEQ | First-in-last-off / First-in-first-off / Runtime equalization |
| maxStgs | 1–255 | Must match Stager maxStgs exactly when directly connected |

**Outputs:** Stage1–Stage5 (0/1); `stgStatusOut` (internal, for StageDriverAdd chain).
Runtime stored as float in minutes per stage; range 0–31.92 years.
**Note:** only 95 stages visible on the wiresheet; use Show Stages or Link Editor for higher stage numbers.

### 1.4 OccupancyArbitrator (`-OccupancyArbitrator.txt`)

Arbitrates effective occupancy from schedule + wall module + network + sensor.

**Analog Inputs** (all use value 255=OCCNUL on unconnected/invalid):

| Input | Valid values | Notes |
|-------|-------------|-------|
| scheduleCurrentState | 0,1,3,255 | 0=Occ,1=Unocc,3=Standby,255=Null |
| WMOverride | 0,1–3,255 | Same encoding; wall-module override |
| NetworkManOcc | 0,1–3,255 | Network-commanded occupancy |
| OccSensorState | 0,1,255 | 0=Occ,1=Unocc,255=Null |

**Outputs:** `EFF_OCC_CURRENT_STATE` (0–3: Occ/Unocc/Bypass/Standby);
`MANUAL_OVERRIDE_STATE` (0–3 or 255=Null).

**Configuration:**

| Name | Range | Meaning |
|------|-------|---------|
| netLastInWins | 0–1 | 0=network wins, 1=last-in wins arbitration |
| occSensorOper | 0–2 | 0=Conference room, 1=Unoccupied Cleaning Crew, 2=Unoccupied Tenant |

### 1.5 RateLimit (`-RateLimit.txt`)

Limits the rate of output change to prevent fast actuator movement.

**Analog Inputs:**

| Input | Low | High | Notes |
|-------|-----|------|-------|
| in | −∞ | +∞ | The value to follow |
| startInterval (sec) | 0 | 65535 | Rate-limiting period after enable; 0=permanent |
| startVal | −∞ | +∞ | Output value while disabled (unconnected → output=in) |
| upRate (chg/sec) | 0< | +∞ | Max upward change per second; 0/unconnected=no limit |
| downRate (chg/sec) | 0< | +∞ | Max downward change per second; 0/unconnected=no limit |

**Output:** any float (rate-limited version of `in`). When disabled, output=`startVal`.

### 1.6 SetTemperatureMode (`-SetTemperatureMode.txt`)

Arbitrates effective HVAC mode (COOL/REHEAT/HEAT/EMERG_HEAT/OFF) from system switch, network command, and temperatures.

**Analog Inputs:**

| Input | Low | High | Notes |
|-------|-----|------|-------|
| sysSwitch | 0 | 255 | SS_AUTO=0,SS_COOL=1,SS_HEAT=2,SS_EMERG_HEAT=3,SS_OFF=255 |
| cmdMode | 0 | 255 | CMD_AUTO=0,HEAT=1,COOL=2,OFF=3,EMERG_HEAT=4 |
| spaceTempSensor | — | — | Used in VAV mode arbitration |
| supplyTempSensor | — | — | Supply <70 °F → COOL/REHEAT in VAV mode |

**Outputs:** `effTempMode` (COOL=0/REHEAT=1/HEAT=2/EMERG_HEAT=3/OFF=255);
`effSetpt` (the active setpoint for the effective mode).

**Configuration:**

| Name | Range | Meaning |
|------|-------|---------|
| controlType | 0–1 | 0=CVAHU, 1=VAV (different arbitration tables) |
| behaviorType | Legacy/Enhanced | Legacy=normal Spyder behavior; Enhanced=default SS_AUTO if unconnected |

### 1.7 Network I/O (NVI / NVO / NCI)

For the **LON Spyder**, NVs are the network interface — not standalone function blocks with a property
table of their own, but wiring primitives:

| Type | Role | Properties configured in NV Config View |
|------|------|----------------------------------------|
| NVI (Network Variable Input) | incoming data from the LonWorks network to FBs | Name, SNVT type, Internal Data Type (fixed NV: only InternalDataType editable) |
| NVO (Network Variable Output) | outgoing data from FBs to the network | Name, SNVT type |
| NCI (Network Config Input) | persistent configuration value (survives restart) | Name, SNVT type, default value |
| Many-to-One NV | combines multiple sources into one FB input | Name, member NVs |

For the **BACnet Spyder**, BACnet Objects play the same role (configured in the BACnet Object Manager View).
There is no discrete "network I/O function block" with a numeric property table — the NV is wired on
the wiresheet and its properties are edited in the NV Config View.

### 1.8 Alarms

The **AlarmsView** (`honeywellSpyderTool-AlarmsView.txt`) lists 6 alarm categories emitted by the Spyder:
Sensor Alarms, Invalid Configuration, Network Communication, Control Alarms (from alarm-block connections),
Control Execution Alarm (>1 s engine cycle), and Node Disabled. There is **no discrete "Alarm" function
block** with configurable input/output/setpoint tables in the available HoneywellFunctionBlocks guide set —
alarm blocks are configured implicitly (sensor pin assignments → Sensor Alarms; alarm blocks in the
wiresheet → Control Alarms). The guide set does not expose a standalone Alarm FB property sheet.

**Scoped gap note:** Schedule function blocks are similarly absent — schedules are handled as Niagara
schedule objects or network inputs (NVI) feeding OccupancyArbitrator, not as a discrete function block
with its own property table.

---

## 2. B837-G1 — FTT-10 free-topology termination rules `[CERT-doc]`

The canonical reference is the Echelon **FTT-10A Free Topology Transceiver User's Guide (078-0156-01F)**,
cited by every Honeywell JACE/NPB/controller install guide (`12997(LON)-C.txt` L121; `NPB-8000-LON_InstallSheet.txt`
L113; `WEB-645_Install_-_62-0432.txt` L535; etc.) but not included in our local source set. The values
below are drawn from Honeywell equipment guides that repeat the normative values, confirmed across three
independent Honeywell documents.

### 2.1 Terminator values and count `[CERT-doc]`

(From Eagle Controller Communication Interfaces EN2Z-1002GE51 §Termination; corroborated by CIPer Model 50
31-00233-03 §LonWorks and EAGLEHAWK NX EN1Z-1039GE51.)

| Network topology | Resistor value | Count per network |
|-----------------|---------------|-------------------|
| **Free topology** (star/mixed/random) | **52.3 Ω** | **1** (one terminator per channel) |
| **Bus/daisy-chain topology** | **105 Ω** | **2** (one at each physical end) |

The XAL-Term2 termination module has a jumper with three positions: "PARK" (no termination), "FTT/LPT BUS
TOPOLOGY" (105 Ω), and "FTT/LPT FREE TOPOLOGY" (52.3 Ω). The FTT-10A network is **polarity-insensitive** —
no polarity check when wiring. For the NPB-8000-LON on a JACE, free-topology deployments need exactly one
52.3 Ω terminator placed anywhere on the channel.

Contrast with BACnet MS/TP: that uses 120 Ω at each line end, with bias + termination combined in the
JACE's END switch (`B835 §5 / B840`).

### 2.2 Cable and distance rules `[CERT-doc]`

(From EN2Z-1002GE51 §Free topology layout examples, Fig. 79.)

| Parameter | Value | Notes |
|-----------|-------|-------|
| Max **node-to-node** wire distance | **320 m** (1,050 ft) | Free topology; any single path between two nodes |
| Max **total wire length** | **500 m** (1,640 ft) | Sum of all wire on the channel |
| Cable type (Honeywell docs) | Level IV 22 AWG (Belden 9D220150) or plenum 9H2201504 | Non-shielded, twisted-pair, solid conductor |
| Signal rate | **78.125 kbaud** fixed | TP/FT-10 physical layer; no baud setting (B135/B137) |

Allowed layouts: any combination of daisy-chain, loop, star, or mixed, as long as no single node-to-node
path exceeds 320 m and total wire stays ≤ 500 m.

When total wire limits are exceeded, FTT-10A repeaters can extend the network — each repeater adds one
full segment budget (e.g., for a doubly-terminated bus with JY(St)Y cable: +900 m per repeater). `[CERT-doc]`
EN2Z-1002GE51 Fig. 79 note.

### 2.3 Max nodes per channel `[INFER]`

The Echelon FTT-10A standard supports **64 nodes per channel** at 78.125 kbaud (the standard FTT-10A
segment limit). This value is not stated verbatim in the Honeywell equipment guides consulted; it comes
from the Echelon FTT-10A spec (078-0156-01F) which the Honeywell docs defer to. Treat as `[INFER]` until
confirmed from the Echelon guide itself.

### 2.4 Implication for JACE commissioning

A typical JACE-to-Spyder LON installation (one JACE + 1–few Spyder controllers) is far within the 500 m
total-wire and 64-node limits. The practical rule is:
1. Use free topology (star/random cable runs typical in a panel room).
2. Install exactly **one 52.3 Ω terminator** at any point on the channel.
3. Keep any run ≤ 320 m; total cable ≤ 500 m.
4. No polarity concern when terminating.

---

## Self-verify

| # | Claim | Marker | Evidence |
|---|-------|--------|----------|
| 1 | PID: sensor/setPt/tr/intgTime unconnected → Out=0, Output range −200..+200 or 0..100, revAct 0-2, bias 0-100%, dbDelay 0-65535 sec | [CERT-doc] | honeywellFunctionBlocks-Pid.txt Tables 01-04 |
| 2 | Stager: maxStgs 1-255, minOn/minOff/intstgOn/intstgOff 0-65535 sec, hyst 0-100, CPH 0-60 | [CERT-doc] | honeywellFunctionBlocks-Stager.txt Tables 01-04 |
| 3 | StageDriver: nStagesActive 0-255, leadLag 0/1/2 (FILO/FOFO/RUNEQ), maxStgs 1-255; 95-stage wiresheet limit | [CERT-doc] | honeywellFunctionBlocks-StageDriver.txt Tables 01-02 + Note |
| 4 | OccupancyArbitrator: state values Occ=0/Unocc=1/Bypass=2/Standby=3/Null=255; netLastInWins 0-1; occSensorOper 0-2 | [CERT-doc] | honeywellFunctionBlocks-OccupancyArbitrator.txt Tables 01-03 |
| 5 | RateLimit: upRate/downRate 0<..+∞ chg/sec; startInterval 0-65535 sec; 0=no limit | [CERT-doc] | honeywellFunctionBlocks-RateLimit.txt Tables 01-03 |
| 6 | SetTemperatureMode: controlType 0=CVAHU/1=VAV; sysSwitch/cmdMode enumerations; supply<70°F → COOL/REHEAT | [CERT-doc] | honeywellFunctionBlocks-SetTemperatureMode.txt Tables 01-06 |
| 7 | NVI/NVO/NCI: wiring primitives in NV Config View, not standalone FBs with numeric property tables | [CERT-doc] | HoneywellSpyder/ApplicationNVConfigurationView.txt |
| 8 | No discrete Alarm or Schedule FB with property tables in the available guide set | [CERT-doc] | honeywellSpyderTool-AlarmsView.txt + full guide-list audit |
| 9 | Free topology: 52.3 Ω × 1 per channel; bus topology: 105 Ω × 2 (one per end) | [CERT-doc] | EN2Z-1002GE51 §Termination; 31-00233-03 §LonWorks; EN1Z-1039GE51 §LONWORKS |
| 10 | Max node-to-node 320 m, max total wire 500 m (free topology) | [CERT-doc] | EN2Z-1002GE51 Fig. 79 caption |
| 11 | Echelon 078-0156-01F is the canonical FTT-10 spec; referenced by 12997(LON)-C, NPB-8000-LON, WEB-645, W-NXS, T-603, T-645 | [CERT-doc] | multiple docs-text files listing "078-0156-01F" |
| 12 | Max 64 nodes per FTT-10 channel | [INFER] | Echelon FTT-10A standard (not in local source set); treat as unconfirmed until 078-0156-01F consulted |

**Tally:** 12 claims — 11 [CERT-doc], 1 [INFER] (max node count). No unmarked assertions.

## Connections

- **B835** — BACnet MS/TP path (contrast: 120 Ω both-ends vs 52.3 Ω single for FTT-10)
- **B836** — the function-block reference whose property detail this block fills in
- **B837** — the LON/Classic Spyder path whose termination gap (claim 5 of B837) this block closes
- **B840** — operator wiring runbook (bias + termination for the BACnet side)

## Open gaps

- **B837-G1-rem** — max node count (64?) per FTT-10A channel from Echelon 078-0156-01F: cited in 7+ local docs but the guide itself is not in our source set. Value is standard and well-attested, but not yet confirmed from a local file.
- **B835-G4** — live commissioning probe on the operator's JACE (authorized) — requires hardware; cannot be done without live station access.
- **B839-G1 (partial)** — verbatim Honeywell nomenclature-key table for model-number digit/letter decode; TRADELINE catalog not publicly accessible; decode stays [INFER].
