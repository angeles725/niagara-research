# Block 445 — Connecting a JACE-8000 to VYKON IO-R remote I/O modules (nrio driver): physical RS-485 wiring + station-side NrioNetwork configuration

**Focus:** base corpus (field-I/O drivers axis — extends [B86] Centraline/Honeywell PanelBus/OnboardIO, which covered a *different* remote-I/O family). First corpus coverage of the native **Tridium/VYKON `nrio`** driver and the **IO-R-16 / IO-R-34** clip-on expansion modules.

**Origin:** operator question — "how do you connect a JACE-8000 to an IOR". Clarified live: hardware is **VYKON** brand (Tridium's own line; same base hardware as the Honeywell WEB-8000 rebrand). "IOR" = **IO-R** = the **R**elay-equipped remote I/O module family (IO-R-16 / IO-R-34).

**Scope:** the end-to-end connection — (1) which modules exist and their capacity/power rules, (2) the physical RS-485 + power + ground wiring back to the JACE-8000, (3) the JACE-8000 port and bias/termination switch, (4) the station-side `NrioNetwork` → `NrioModule` → proxy-point model and the discover/commission workflow. NOT covered: per-point conversion math (scale/offset, thermistor curves), M2M onboard-I/O JACEs, firmware byte format (see gaps).

**Sources (all consulted; the three-source rule):**
- **FUENTE 1 (own corpus):** [B86] (PanelBus/OnboardIO/IOcreation — a *distinct* Honeywell RS-485 field-I/O family, not nrio), [B394] (PanelBus IO firmware = raw unsigned flash; different product). No prior block covers `nrio`/IO-R. CATALOG confirms zero `nrio` rows.
- **FUENTE 2 (niagara-help, official Tridium):** `[CERT-doc]`
  - Driver guides `guides-clean/Nrio/*`: `NrioConcepts`, `aNrioArchitecture`, `NrioSetup`, `aNrioNetwork`, `aEssentialNrioNetworkProperties`, `qsConfigureNrioNetwork`, `NrioDeviceManager`, `aNrioModule`, `aActrldToNrio`, `nrio-M2mIoNetwork`.
  - Hardware mounting & wiring (VYKON `-TRI` variants): `docs-text/IO-R-16_MtgWiring_-TRI.txt` (P/N 32327275-001 Rev B, 2017), `docs-text/IO-R-34_MtgWiring_-TRI.txt` (P/N 32327280-001 Rev B), `docs-text/JACE-8000_MtgWiringGuide_-TRI.txt`.
- **FUENTE 3 (code corpus):** `organized/nrio/nrio-rt/vineflower/com/tridium/nrio/BM2mIoNetwork.java` (baud property); modules present: `nrio`, `ndio`, `kitIo`, `nrioConversion`, `platNrio`, `platNdio`, `docNrio`, `docNdio`. `[CERT]`

---

## 445.1 — What "IO-R" is, and where it sits (the two networks)

The `nrio` driver ("**N**iagara **R**emote **I**nput/**O**utput") is the station interface to JACE controllers with onboard I/O **or** remote I/O expansion modules accessed over RS-485. `[CERT-doc]` (`NrioConcepts`)

Two network container types exist — pick by hardware: `[CERT-doc]` (`aNrioNetwork`, `qsConfigureNrioNetwork`)

| Container | Represents | For a JACE-8000? |
|---|---|---|
| `M2mIoNetwork` | Integral **onboard** I/O of an M2M JACE (JACE-202 Express). Port/Trunk/Baud are **read-only** (COM3, Trunk 1). | **No** — JACE-8000 has no onboard nrio I/O. |
| `NrioNetwork` | An **RS-485 trunk** to one or more **remote** IO modules. Port Name + Trunk are writable. | **Yes** — this is the JACE-8000 + IO-R case. |

Component hierarchy (`nrio` uses standard NiagaraNetwork architecture): `[CERT-doc]` (`aNrioArchitecture`)

```
Drivers (DriverContainer)
└── NrioNetwork            (one per RS-485 trunk; Port Name + Trunk)
    └── NrioModule         (one per physical IO-R device; Nrio16Module / Nrio34Module)
        └── Points (ext)   (the ONLY device extension nrio has)
            └── Nrio proxy points   (one per used I/O terminal)
```

Unlike most drivers, `Points` is the *sole* device extension under an `NrioModule` — the driver's only job is to configure and proxy real I/O terminals. `[CERT-doc]` (`aNrioArchitecture`, `aNrioModule`)

**The IO-R hardware family** (VYKON, expands a JACE-8000): `[CERT-doc]` (`IO-R-16_MtgWiring_-TRI` L16-26, `IO-R-34_MtgWiring_-TRI` L1108-1115)

| Module | Model | I/O | Station device | Power |
|---|---|---|---|---|
| **IO-R-16** | 14006 | 8 UI + 4 DO (Form-A relay) + 4 AO (0–10 V) | `Nrio16Module` (default name `io16_n`) | **15 Vdc input** (from an IO-R-34 or third-party 13.5–15.75 Vdc supply) |
| **IO-R-34** | 14022 | 16 UI + 10 DO (Form-A relay) + 8 AO | `Nrio34Module` | **24 Vac/dc** (own transformer; shares JACE-8000's input-power class) and can **output** 15 Vdc to power up to 4 IO-R-16 |

Point ratings: relay outputs 24 Vac/dc @ 0.5 A max (Form-A, MOV-suppressed, NOT for AC mains); AO 0–10 Vdc, 4 mA max, load ≥ 2500 Ω; UI accepts Type-3 10K thermistor / 0–100K resistive / 0–10 Vdc / 4–20 mA (needs the supplied **499 Ω** resistor across the input) / dry or pulsing dry contact. `[CERT-doc]` (`IO-R-16_MtgWiring_-TRI` L403-911)

**Requirement:** JACE-8000 support for nrio needs **Niagara 4.3 or later** (or NiagaraAX-3.8U3), the **`nrio` feature in the JACE license**, and the **`nrio` + `kitIo`** modules installed on the controller. `kitIo` supplies the non-linear (Generic Tabular / thermistor-curve) conversions. `[CERT-doc]` (`NrioSetup`, `IO-R-16_MtgWiring_-TRI` L53)

---

## 445.2 — Bus capacity and addressing (why IO-R-34 counts as two)

One RS-485 trunk (one `NrioNetwork`) supports **up to 16 module addresses (1–16)**. `[CERT-doc]` (`NdioBoardIOproperties` L26-28, `aNrioModule` L23)

- **Nrio16Module** = one I/O processor, one address, up to 16 points.
- **Nrio34Module** = **two controllers on one PCB** (primary + secondary), so it **consumes two of the 16 address slots**. In the Device Manager its `Address` column shows the primary, `SecAddr` the secondary. `[CERT-doc]` (`aNrioModule` L26-30, `nrio-NrioDeviceManager` L27-40)

Therefore one JACE-8000 RS-485 bus holds a **maximum of 16 IO-R-16 OR 8 IO-R-34** (or mixes: e.g. 2× IO-R-34 + 12× IO-R-16). A legacy `T-IO-16-485` counts the same as an IO-R-16. `[CERT-doc]` (`IO-R-16_MtgWiring_-TRI` L48-51, `IO-R-34_MtgWiring_-TRI` L1137-1140)

**Address is NOT set by a DIP switch and is NOT manually editable in Niagara.** It is auto-derived during the online **Discover**, together with the device's read-only 6-byte **Uid** (factory-assigned unique ID). An offline-added module has Address=0 / Uid=0 and sits in **fault** until matched to a discovered device. `[CERT-doc]` (`aNrioModule` L30, `NrioBoardManagerUsageNotes` L22, `aNrioOfflineEngineering` L24, `NdioBoardStatusProperties` L27)

> Need more than 16 modules, or a second trunk? Use a **separate `NrioNetwork` per RS-485 port**, each with its own unique Port Name and Trunk. `[CERT-doc]` (`aNrioNetwork`, `qsConfigureNrioNetwork`)

---

## 445.3 — Physical wiring back to the JACE-8000

### RS-485 comm (the actual "connection")
- Topology: a **continuous daisy-chain / multidrop** using **shielded twisted-pair, 18–22 AWG** (TIA/EIA-485). Wire **+→+, −→−, S→S** (shield). `[CERT-doc]` (`IO-R-16_MtgWiring_-TRI` L644-655)
- Each IO-R module carries the bus on the **lower 3 pins of its end-mounted 5-position connector**; modules **direct-chain** by seating one module's 5-pin plug into the next. `[CERT-doc]` (L27-33, L671-683)
- At the controller, land the trunk on **either JACE-8000 3-position RS-485 screw connector — COM1 (port A) or COM2 (port B)**. `[CERT-doc]` (`IO-R-16_MtgWiring_-TRI` L646-648; `JACE-8000_MtgWiringGuide_-TRI` L271-279)
- **`S` terminal is mandatory:** it is the **reference ground between the isolated RS-485 ports** of the JACE-8000 and the IO-R modules. Omitting it "may result in communication errors." Ground the **shield to earth at ONE end only** (e.g. at the JACE). `[CERT-doc]` (L268-269, L523-524, L651-652)

### The JACE-8000 side — bias/termination switch (critical, easy to miss)
Each JACE-8000 RS-485 port has an adjacent **3-position bias/termination switch**: `[CERT-doc]` (`JACE-8000_MtgWiringGuide_-TRI` L288-296, L308-317)

| Setting | Bias | Termination | Use when |
|---|---|---|---|
| **BIA** (default, middle) | 2.7K Ω | **none** | trunk not already biased; controller not at an end |
| **END** | 562 Ω | **150 Ω termination** | JACE-8000 is at **one physical end** of the trunk |
| **MID** | 47.5K Ω | none | JACE-8000 sits in the **middle** of an already-biased trunk |

Ports run up to **115,200 baud**; the connector is a 3-position screw terminal (RS485 A = COM1, RS485 B = COM2). `[CERT-doc]` (L306-309, L350-353)

### Power and ground (per module)
- **IO-R-16:** 15 Vdc input on the **P+ / P−** pins of the 5-position connector — from an adjacent IO-R-34 (mate the connectors; one IO-R-34 powers ≤ 4 IO-R-16) **or** a third-party 13.5–15.75 Vdc (±4% regulated, battery-backed recommended) supply. `[CERT-doc]` (L62-68, L554-642)
- **IO-R-34:** 24 Vac/dc (21.6–26.4 V) on its 2-pin power connector; **only the JACE-8000 and IO-R-34s share that transformer**. `[CERT-doc]` (`IO-R-34_MtgWiring_-TRI` L1155-1158)
- **Earth ground:** every module has a 0.187" spade lug — bond each to a nearby earth point with ≥ #16 AWG, kept short. `[CERT-doc]` (L529-535)
- **Voltage-drop budget:** max allowable drop P+/P− from supply to farthest module = **1.5 V**; each IO-R-16 draws ≤ 0.133 A / 2 W (all relays energized). Size the trunk-power gauge from the AWG drop table. `[CERT-doc]` (L136-171)
- **UPS strongly advised:** a power cycle *during* an nrio firmware upgrade (launched from the Device Manager) can brick the module. `[CERT-doc]` (L115-118, L1212-1215)

**Wiring order (do NOT energize until last):** ① earth ground → ② supply power (unenergized) → ③ RS-485 (+/−/S) → ④ field I/O → ⑤ apply power. `[CERT-doc]` (L506-527)

---

## 445.4 — Station-side configuration and commissioning workflow

Once wired and powered, commission in Workbench (station connection to the JACE-8000): `[CERT-doc]` (`IO-R-16_MtgWiring_-TRI` L935-969, `qsConfigureNrioNetwork`, `NrioDeviceManager`)

1. **Add the network.** In `Drivers`, add an `NrioNetwork` (Driver Manager → New, or copy from the **`nrio` palette**). Error "nrio module missing" ⇒ install `nrio` on the JACE and retry. `[CERT-doc]` (`aNrioNetwork`)
2. **Set the two essential properties** on the network's property sheet: `[CERT-doc]` (`aEssentialNrioNetworkProperties`)
   - **Port Name** = the JACE-8000 COM the trunk landed on — **`COM1` or `COM2`** (JACE-8000 row of the Port-Name table).
   - **Trunk** = a station-unique integer, 1, 2, … — this binds the network to the low-level **`actrld`** (access-control daemon) that actually polls that I/O. `[CERT-doc]` (`aActrldToNrio` L12)
   - (4.3+) optional **Output Failsafe Config**: `Comm Loss Timeout` (8–900 s, def 8) and `Startup Timeout` (8–900 s, def 600) applied to child modules; each child can disable them via its `OutputDefaultValues`.
3. **Discover the modules.** Open the network's **Nrio Device Manager** → **Discover**. Each IO-R-16 enumerates as device type **`Io16V1`**; use right-click **Wink Device** (cycles a relay audibly) to physically identify one. `[CERT-doc]` (`IO-R-16_MtgWiring_-TRI` L946-952)
4. **Add** the discovered devices (each becomes an `Nrio16Module` / `Nrio34Module`); rename to reflect location. Address + Uid populate automatically. `[CERT-doc]` (`NrioDeviceManager`)
5. **Verify online:** the module's **STATUS LED lit solid green = Online**; **blinking = NOT online**. The JACE's RS-485 port LEDs flash continuously with poll traffic. `[CERT-doc]` (L925-929)
6. **Firmware:** if the Device Manager's **Upgrade Firmware** button is active for a module, upgrade it (≈ < 2 min); do not interrupt power/comm during the job. `[CERT-doc]` (L959-965)
7. **Add points.** Under each module's **Points** extension, open the **Nrio Point Manager** and add one proxy point per used terminal. Point type is chosen by signal (and cannot be changed after add — only name/address/conversion/facets). `[CERT-doc]` (`aNrioModule` L46, `NrioPointManagerUISelection` L56)

**Terminal-signal → Nrio proxy-point mapping** (the boldface pairings in the wiring guide): `[CERT-doc]` (`IO-R-16_MtgWiring_-TRI` L686-911)

| Field signal | Nrio proxy point | Conversion |
|---|---|---|
| Type-3 10K thermistor | `ThermistorInputPoint` | Type-3 (or Tabular Thermistor via `kitIo` curve xml) |
| 0–100K resistive | `ResistiveInputPoint` | — |
| 0–10 Vdc | `VoltageInputPoint` | Linear |
| 4–20 mA (+499 Ω) | `VoltageInputPoint` | 500 Ohm Shunt → Linear |
| Dry contact | `BooleanInputPoint` | — |
| Pulse contact (≤ 20 Hz, dwell > 25 ms) | `CounterInputPoint` | — |
| Relay output | `RelayOutputWritable` | — |
| 0–10 Vdc analog out | `VoltageOutputWritable` | — |

**Polling model:** the `actrld` daemon (one per trunk, separate from the station) polls each processor and does a memory-compare against the previous `IoStatus`; only changes propagate up to the proxy points — the station never talks RS-485 directly. `[CERT-doc]` (`aActrldToNrio` L12-19)

---

## 445.5 — Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | JACE-8000 uses the `NrioNetwork` container (remote RS-485 I/O), not `M2mIoNetwork` (onboard, read-only COM3/Trunk1) | `[CERT-doc]` | `qsConfigureNrioNetwork`, `aEssentialNrioNetworkProperties` (JACE-8000 row) |
| 2 | Requires Niagara 4.3+, `nrio` license feature, and `nrio`+`kitIo` modules on the JACE | `[CERT-doc]` | `NrioSetup`; `IO-R-16_MtgWiring_-TRI` L53 |
| 3 | IO-R-16 = 8UI/4DO/4AO @15 Vdc; IO-R-34 = 16UI/10DO/8AO @24 Vac/dc, powers ≤4 IO-R-16 | `[CERT-doc]` | `IO-R-16_MtgWiring_-TRI` L16-26; `IO-R-34_MtgWiring_-TRI` L1108-1122 |
| 4 | Bus = 16 addresses; IO-R-34 uses two slots (primary+secondary on one PCB); max 16 IO-R-16 or 8 IO-R-34 | `[CERT-doc]` | `aNrioModule` L23-30; `IO-R-34_MtgWiring_-TRI` L1137-1140 |
| 5 | Address+Uid auto-derived at Discover, read-only, not DIP-set; offline add ⇒ Address 0 / fault | `[CERT-doc]` | `aNrioModule` L30; `NrioBoardManagerUsageNotes` L22; `aNrioOfflineEngineering` L24 |
| 6 | RS-485 daisy-chain, shielded TP 18–22 AWG, +/−/S; land on JACE-8000 COM1 or COM2; S = isolated-port reference ground (mandatory) | `[CERT-doc]` | `IO-R-16_MtgWiring_-TRI` L644-655, L268-269 |
| 7 | JACE-8000 RS-485 port bias switch BIA(default)/END(150 Ω term)/MID; up to 115,200 baud | `[CERT-doc]` | `JACE-8000_MtgWiringGuide_-TRI` L288-296, L306-309 |
| 8 | Commission = add NrioNetwork → set Port Name(COMn)+Trunk → Discover (`Io16V1`, Wink) → add module → STATUS LED solid green = online → add proxy points | `[CERT-doc]` | `IO-R-16_MtgWiring_-TRI` L935-969; `NrioDeviceManager` |
| 9 | Trunk binds to a low-level `actrld` daemon that polls processors and memory-compares IoStatus; station has no direct RS-485 path | `[CERT-doc]` | `aEssentialNrioNetworkProperties`; `aActrldToNrio` L12-19 |
| 10 | Voltage-drop limit 1.5 V P+/P−; IO-R-16 draws ≤0.133 A/2 W; UPS advised (power cycle during FW upgrade can brick module) | `[CERT-doc]` | `IO-R-16_MtgWiring_-TRI` L136-171, L115-118 |

**Tally:** 10 claims — 10 `[CERT-doc]` · 0 `[CERT]` · 0 `[INFER]` · 0 unmarked. No contradictions with prior blocks. No claim lacks a citation.

**Left out (named, not covered):** M2M onboard-I/O JACEs (`M2mIoNetwork` in depth). *(The other items originally left open here are now closed: conversion engine → [B446]; `.a43` firmware format → [B447]; RS-485 baud = **115200** and framing → [B448].)*

---

## 445.6 — Connections

- **Extends [B86]** (Centraline/Honeywell **PanelBus** + OnboardIO): that is a *separate* Honeywell RS-485 field-I/O family with its own driver; `nrio`/IO-R is the native Tridium/VYKON line. Do not conflate — different modules, different driver, different palette.
- **Relates to [B394]** (PanelBus IO firmware = raw unsigned flash): the nrio side also ships firmware images (`.a43`) pushed from the Device Manager — an unexamined integrity surface (see gap).
- **Relates to WEB-8000 memory** (`web8000-jace-factory-commissioning`, `client-license-qnx-titan-be9d`): the Honeywell WEB-8000 is the JACE-8000 rebrand; its IO modules are these same IO-R units (Honeywell `-31-005xx` doc variants exist alongside the VYKON `-TRI` ones).
- **License angle** ([B442]/[B443] license-diff): the `nrio` feature must be present in the JACE license — a licensing gate on hardware expansion.

## 445.7 — Child gaps (all closed)

- **B445-G1** — nrio conversion subsystem (conversions + `kitIo` thermistor curves). **CLOSED → [B446].**
- **B445-G2** — `.a43` IO firmware format + Device-Manager upgrade path (integrity/signing posture). **CLOSED → [B447].**
- **B445-G3** — IO-bus baud/framing + `actrld` daemon. **CLOSED → [B448]** (baud = 115200; native actrld over Thrift).
