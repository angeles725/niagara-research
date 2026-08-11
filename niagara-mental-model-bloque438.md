# Block 438 — The driver-UI long tail is pattern-repetition over B437: 48 modules extend the same device/point manager framework

> Research of the **protocol-driver Workbench-UI long tail** (focus `workbench`, gap WB12, LOW — a BUCKET gap).
> Scope: confirm the ~51-module driver-UI cluster is repetition of the [Block 437] framework, spot-check the
> inheritance, and HONESTLY LOG which modules are NOT individually documented (and why that is a deliberate
> scope choice, not an oversight). This is a closing/bucket block — it does NOT document each driver.
>
> Subject version: OptimizerSupervisor N4.14.0.162 — modules under `.../modules/*-wb.jar`.
>
> Sources: package/inheritance census over the decompiled corpus (`organized/*/*-wb/vineflower/`). Method:
> `rg` for subclasses of the [Block 437] base managers. Markers: `[CERT]` (measured/observed) · `[INFER]`
> deduction.
>
> Workbench UI framework. Connects [Block 437] (the framework these all extend), [Block 304] (modbus-wb, the one
> driver documented deeply), [Block 431] (the manager base under it all).

---

## 438.1 — Measured: 48 `-wb` modules extend the driver-UI framework `[CERT]`

A census for subclasses of the [Block 437] base managers (`B(N)?(Device|Point|Driver|Folder)Manager`) across
the decompiled corpus finds **48 distinct `-wb` modules** that extend the framework. `[CERT]` The driver-UI long
tail is therefore not 51 bespoke UIs — it is 48 modules repeating ONE pattern: subclass the device/point manager,
declare `@AgentOn` on the protocol's network/device types, and annotate the proxy-ext with `@MgrInclude` for
free columns ([Block 437] §437.4). `[CERT]`/`[INFER]`

## 438.2 — Spot-check: the inheritance holds across protocols `[CERT]`

Sampled concrete drivers (class-name tokens decompile mangled `l`; file names + `extends` clause real, cited by
existence): `[CERT]`

| Driver | Manager class | Extends |
|---|---|---|
| BACnet | `BBacnetPointManager` (`bacnet-wb`) | a driver-framework point manager |
| LonWorks | `BLonPointManager` (`lonworks-wb`) | a driver-framework point manager |
| KNX/IP | `BKnxDeviceManager` (`knxnetIp-wb`) | a driver-framework device manager |
| Modbus | (modbus-wb) | documented deeply in [Block 304] — same pattern |

`[INFER]` different wire protocols, identical UI skeleton — the manager/discover/add experience is uniform
because it is inherited, not re-authored.

## 438.3 — HONEST COVERAGE LOG: what is NOT individually documented `[CERT]`

Of the 48 framework-extending driver-UI modules, **~16 are named somewhere in `CATALOG.md` and ~32 are not
individually documented at block grade.** `[CERT]` This is a DELIBERATE scope decision, recorded here per the
no-silent-caps rule: `[INFER]`

- The tail is pattern-repetition over [Block 437]; a per-module block would restate the same framework with a
  different protocol's columns — low marginal value.
- The drivers that DO warrant their own treatment already have it (`modbus-wb` [Block 304]) or are covered by
  their protocol focus (bacnet [Block 271]-[Block 288], lon [Block 19]).
- The OEM driver UIs (Centraline/Honeywell/Galileo, a separate 46-module cluster) are mostly documented in the
  OEM blocks ([Block 77]-[Block 122], [Block 241]-[Block 250]).

**Not individually opened (examples from the 32):** `aaphp-wb`, `aapup-wb`, `abstractMqttDriver-wb`,
`andoverAC256-wb`, `ccn-wb`, `clCBus-wb`, `clEnoceanNetwork-wb`, `edgeIo-wb`, `flexSerial-wb`, `mbus-wb`,
`mcquay-wb`, `micros-wb`, … `[CERT]` `[INFER]` any of these is a candidate for a future targeted block IF a
specific protocol question arises — but as UI, each is [Block 437] with protocol-specific `@MgrInclude` columns.

## 438.4 — Self-verify

| # | Claim | Marker | Source |
|---|---|---|---|
| 1 | 48 distinct `-wb` modules extend the [Block 437] driver-UI base managers | `[CERT]` | census (rg over corpus) |
| 2 | Spot-check: bacnet/lonworks/knx have concrete manager subclasses (tokens mangled, extends real) | `[CERT]` | §438.2 |
| 3 | ~32 of the 48 are NOT individually documented — deliberate, logged, not silent | `[CERT]`/`[INFER]` | §438.3 |
| 4 | The tail is uniform because the UI is inherited from B437, not re-authored per driver | `[INFER]` | §438.1-438.2 |

**Marker tally**: `[CERT]` ≈ 8 · `[INFER]` 5 ([INFER]/[CERT] ≈ 0.6). Type: **BUCKET/closing block** — a high
inference ratio is EXPECTED here: the block's job is to MEASURE the tail and JUSTIFY not enumerating it, not to
decompile 32 modules. The measured counts (48 extenders, ~32 undocumented) are `[CERT]`; the "low marginal
value" scope judgment is `[INFER]`, stated as such. This closes WB12 and the `workbench` focus at 12/12.

## 438.5 — Connections

- **[Block 437]** — the framework every module in this tail extends; this block is its population census.
- **[Block 304]** — modbus-wb, the one driver UI documented deeply; the template for what a per-driver block
  would look like.
- **[Block 431]** — the manager base at the bottom of the inheritance chain.
- **`workbench` focus** — with WB12 closed, all 12 gaps are covered; the focus reaches investigable=0.

<!-- research-block: focus workbench, gap WB12 (driver-UI long tail bucket) — CLOSED; 48 extenders measured, ~32 deliberately not individually documented (no-silent-caps log) -->
