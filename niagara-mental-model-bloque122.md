# Block 122 — Spyder bundled **XML resource extract**: per-model fixed-IO + sensor catalog (G4b) and per-model store offsets/capacities (G6b) — the two read-only micro-gaps closed, STATIC loop CLOSED

> **Language note**: written in **English** (standing corpus-language preference for new Spyder-ecosystem blocks; legacy Blocks 1–114 are Spanish, cross-referenced by number).
>
> A deliberately **thin** iteration: not a new code distillation but a **data-extract pass** over the bundled XML resources that [Block 118] and [Block 120] flagged as "mechanism `[CERT]`, values not read". It reads the actual XML data files and upgrades the two residual micro-gaps:
> - **G4b** → upgrades [Block 118] §118.2/§118.5's per-model fixed-point and sensor values from `[CERT-doc]` (inherited from B116) to `[CERT]` (read verbatim from the IO XML).
> - **G6b** → upgrades [Block 120] §120.6's Model-8 (Micro BACnet) capacity claim from `[INFER]` to `[CERT]` (read verbatim from `BacnetSpyder.xml`/`LonSpyder.xml`) — and **corrects** it.
>
> Sources (READ-ONLY, bundled resource XML — not decompiled code, the data half of the I/O model):
> - IO catalog: `…/honeywellSpyderTool/honeywellSpyderTool/extracted/com/honeywell/honeywellXL10NextGen/io/IO_Common.xml`, `…/IO_Model{1,2,3}.xml`, `…/IO_Micro_Model{1..5}.xml`, `…/IO_RelayModel.xml`
> - Fixed-IO: `…/honeywellSpyderTool/honeywellSpyderTool/extracted/XL10NextGenXML/FixedIo_model{1,2,3}.xml`, `FixedIo_Dummy.xml`
> - Model registry / store layout: `…/honeywellBacnetSpyder/honeywellBacnetSpyder/extracted/XL10NextGenXML/BacnetSpyder.xml`, `…/honeywellLonSpyder/honeywellLonSpyder/extracted/XL10NextGenXML/LonSpyder.xml`
> (cited as `<file>:<line>`). Method: direct reading + grep token-confirmation of every load-bearing value against the XML.
>
> Markers: `[CERT]` value read verbatim from the bundled XML (`file:line`) · `[CERT-doc]` official downloaded doc (via [Block 116]) · `[INFER]` deduction.
>
> Layer 22 (deobfuscated OEM, resource data). **Strong connections**: [Block 118] (the `io/` code that PARSES these XML — values now closed), [Block 120] (the driver wire layer whose Model-8 capacity claim these resolve), [Block 116] (vendor doc — the `[CERT-doc]` values now confirmed code/data-side), [Block 117] (FB catalog), [Block 77] (drivers).

---

## 122.1 — What this block is (and why it is thin) `[CERT]`

[Block 118] §118.2 established that the Spyder I/O model is **XML-driven**: the parsing *mechanism* is Java (`SpyderIOModels.load` → `IOCommonInfo.load`/`IOModelInfo.load`/`IOFixedIOInfo.load`, all `[CERT]`), but the concrete *per-model values* live in bundled XML data files that were not opened. [Block 120] §120.6 said the same about per-model store offsets/capacities (`getFileVariables(name).getOffset()/getCapacity()` read from `BacnetSpyder.xml`/`LonSpyder.xml` at runtime). This block opens those exact files. No new code is interpreted; the value of the iteration is **promoting marker confidence** on data that the prior blocks pinned only to the vendor doc or to deduction.

The bundled XML files exist and are readable — they are **present** in the corpus (the residual gaps were "not yet read", not "missing artifact"). Inventory confirmed: `IO_Common.xml` (134 lines), `IO_Model1/2/3.xml`, `IO_Micro_Model1..5.xml`, `IO_RelayModel.xml`, `FixedIo_model1/2/3.xml`, `FixedIo_Dummy.xml`, plus the two 300 KB model registries `BacnetSpyder.xml` / `LonSpyder.xml`.

## 122.2 — G4b: the sensor catalog — concrete values `[CERT]` (upgrades [Block 118] §118.5)

[Block 118] §118.5 described `IOSensorType` (`name`/`sensorType`/`InputLow`…/`SensorLow`…) and said the values load from the XML `Sensors` element. Here is that table, read verbatim from `IO_Common.xml:71-83` (`<Sensors>` block):

| Sensor | `SensorType` (short id) | DataCategory / DataType | InputLow→InputHigh | OutputLow→OutputHigh | SensorLow→SensorHigh | InputUnit (prec) |
|---|---|---|---|---|---|---|
| `Kntc20` | **0** | Temperature / celsius | 70200 → 1114 | 0 → 100 | -45 → 112 | ohms (0) |
| `Pt1000` | **1** | Temperature / celsius | 1000 → 1385 | 0 → 100 | -100 → 200 | ohms (0) |
| `CustomResistive` | **2** | Temperature / celsius | 500 → 10500 | 0 → 10 | 27 → 200000 | ohms (0) |
| `C7400A` | **3** | current / milliampere | 2.00 → 10.00 | 4.0 → 20.0 | 0 → 10.3 | volts (2) |
| `C7632A` | **4** | PartsperMillion / ppm | 0 → 10.00 | 0 → 2000 | 0 → 10.3 | volts (2) |
| `C7632B` | **5** | PartsperMillion / ppm | 0 → 10.00 | 0 → 2000 | 0 → 10.3 | volts (2) |
| `C7600B` | **6** | Percentage / percent | 2.00 → 10.00 | 0 → 100 | 0 → 10.3 | volts (2) |
| `H7655A` | **7** | Percentage / percent | 0 → 10.00 | 0 → 100 | 0 → 10.3 | volts (2) |
| `CustomVoltage` | **8** | Temperature / celsius | 0 → 10.00 | 0 → 10 | 0 → 10.3 | volts (2) |
| `On_Board_Pressure` (subtype `CustomVoltage`) | **9** | Pressure / pascal | 0 → 10.30 | 0 → 1.00 | -12.3 → 394 | volts (2) |
| `Pulse_Meter` | **10** | Unit-less / VAL-float | -1 → -1 | -1 → -1 | -1 → -1 | volts (2) |
| `Counter` | **11** | Unit-less / VAL-float | -1 → -1 | -1 → -1 | -1 → -1 | volts (2) |
| `Kntc20_TR20_Series` | **12** | Temperature / celsius | 10000 → 80000 | 0 → 100 | -45 → 112 | ohms (0) |

`[CERT]` `IO_Common.xml:71-83`. **This confirms code-side B118 §118.5 exactly**: the `sensorType` short-ids 0–12 are these; the special-cased Honeywell part-numbers `C7400A/C7632A/C7632B/C7600B/H7655A` ([Block 118] §118.5, `BModulatingInput.java:371-377`) are the **voltage (4-20 mA / 0-10 V) sensors** here (ids 3–7); the resistive sensors (`Kntc20`/`Pt1000`/`CustomResistive`/`Kntc20_TR20_Series`, ids 0/1/2/12) carry **ohms** input — grounding the "resistive ×1000 ohms→kΩ" scaling B118 saw. Note `Kntc20` input span is **decreasing** (70200→1114 ohms = NTC, resistance falls as temp rises), which is exactly why `BModulatingInput.setSensorLimits` swaps inverted spans (B118 §118.5).

The dynamic-property schema B118 §118.4 attributed to `IOCommonInfo.getDynamicFields` is also verbatim here: `SensorType`/`DataType`(BUnit)/`InputLow`/`InputHigh`/`OutputLow`/`OutputHigh`/`SensorLow`/`SensorHigh`/`InputUnit` with `n=`/`v=`/`type=`/`flags=` attributes — `IO_Common.xml:54-65` (e.g. `InputUnit … type="String" flags="h1"` = the hidden flag). The `ModulatingOutput` drive flavors B118 §118.3 split (Analog=0/Floating=1/Pwm=2/Actuator=3-subtype-Floating) are the `<Analog v="0">`/`<Floating v="1">`/`<Pwm v="2">`/`<Actuator v="3" subtype="Floating">` entries with their default property sets (`TravelTime` default **90.0**, `Period` **25.6**, etc.) — `IO_Common.xml:13-51`. `[CERT]`

## 122.3 — G4b: per-model fixed-IO — concrete pins, and the PVL6436AS/PVL6438NS confirmation `[CERT]` (upgrades [Block 118] §118.2)

The `lonXL10NextGenFixedIo` XML that `IOFixedIOInfo.load` parses ([Block 118] §118.2, `IOFixedIOInfo.java:29-52`) is the `FixedIo_model*.xml` family (root element literally `<lonXL10NextGenFixedIo>`). Verbatim fixed points:

| File (model) | FixedIo points | Terminals | DontSensorCalibrate |
|---|---|---|---|
| `FixedIo_model1.xml` | `On_Board_Pressure` (ModInput, Pressure) + `Actuator` (ModOutput Floating, TravelTime 90.0) | Pressure **T1=31**; Actuator **T1=17, T2=18** | PinNo 41 |
| `FixedIo_model2.xml` | `On_Board_Pressure` only | **T1=31** | PinNo 41 |
| `FixedIo_model3.xml` | none (both commented out) | — | PinNo 41 |

`[CERT]` `FixedIo_model1.xml:23-30`, `FixedIo_model2.xml:16-22`, `FixedIo_model3.xml:22-28`. The same fixed points are mirrored in the per-model `IO_Model*.xml` `<Fixed>` sections with their pins: `IO_Model1.xml:32-34` (On_Board_Pressure Pin 31), `:66-69` (Actuator Pin 17 + Pin 18); `IO_Model2.xml:31-33` (pressure only, no actuator `<Fixed></Fixed>` at MO `:64-65`); `IO_Model3.xml` (no fixed pressure/actuator). `[CERT]`

**The headline G4b confirmation** — tying the abstract "model" to the real controller part-number, which B118 could only inherit from B116 as `[CERT-doc]`. The model→IO-XML binding is in the registry's `<ModelSpecificIOXmlName>` element:

| Controller part | LON model | Maps to IO XML | Fixed IO it gets | Cite |
|---|---|---|---|---|
| **PVL6436AS** | `Model4` | `IO_Model1.xml` | **On_Board_Pressure + Actuator** | `LonSpyder.xml:1569,1597` |
| **PVL6438NS** | `Model5` | `IO_Model2.xml` | **On_Board_Pressure only** | `LonSpyder.xml:2740,2767` |
| PUL6438S | `Model6` | `IO_Model3.xml` | none | `LonSpyder.xml:194` |

`[CERT]`. This is the **exact** claim B116 §116.4 made and B118 §118.2/§118.7 carried as "UNVERIFIABLE code-side / stays `[CERT-doc]`" (e.g. *"PVL6436AS = Actuator + on-board pressure sensor; PVL6438NS = pressure sensor only"*). **It is now read verbatim from the data → `[CERT]`. MATCH.** The doc↔code↔data ledger row in B118 §118.7 ("Per-model fixed-point lists … UNVERIFIABLE code-side") is hereby CLOSED.

**Micro (Model-8 BACnet / model_id=3 LON) fixed-IO** `[CERT]` (`IO_Micro_Model*.xml`): the Micro family relocates fixed pins to a smaller terminal block — Actuator moves from pins 17/18 (full) to **pins 12/13** (`IO_Micro_Model2.xml:56-59`, `IO_Micro_Model5.xml:57-60`); On_Board_Pressure moves to pin 18 (`IO_Micro_Model1/2`) or 14 (`IO_Micro_Model5`); BI/Pulse/Counter fixed pins are 19 or 14 depending on variant. Per-Micro-variant: Model1 = pressure(18)+no actuator; Model2 = pressure(18)+actuator(12/13); Model3 = none; Model4 = none (pins 14); Model5 = pressure(14)+actuator(12/13). The `RelayModel` adds a Pwm fixed slot and drops `CustomSensor` (`IO_RelayModel.xml:42-58`).

## 122.4 — G6b: BACnet per-model store offsets/capacities — and the honest Model-8 correction `[CERT]` (upgrades + corrects [Block 120] §120.6)

[Block 120] §120.3 showed `getStoreOffset(name)` → `ModelInfo.getFileVariables(name).getOffset()`, and §120.6 claimed (as `[INFER]`): *"Model 8 is the Micro controller class, with smaller capacities … the per-model `getStoreOffset`/`getFileVariables` capacities differ, so a transfer sized for a full Spyder will overrun a Micro."* The `<FileVariables>` blocks in `BacnetSpyder.xml` now let me check that claim against the data. **It is half right and half wrong, and the correction matters.**

**Model registry confirmed** `[CERT]` `BacnetSpyder.xml`: `model_id="8"` = the **Micro BACnet family**, five devices — `ModelMicroBACnet1..5` = **PVB4024NS** (hw_id 51, `:2164`), **PVB4022AS** (52, `:1464`), **PUB4024S** (53, `:760`), **PUB1012S** (54, `:929`), **PVB0000AS** (55, `:1632`). Full-size = `model_id` 4/13/15 (`ModelBACnet1..4` = PVB6436AS/PVB6438NS/PUB6438S/PUB6438SR). **MATCH** with B120 §120.6 (which had the part-number list right).

**The store CAPACITIES are NOT smaller — they are essentially IDENTICAL to full-size** `[CERT]`. Compare PUB6438S (full, `model_id=4`, FileVariables at `BacnetSpyder.xml:239-278`) vs PUB4024S (Micro, `model_id=8`, `:814-856`):

| Config store | Full PUB6438S cap / size / offset | Micro PUB4024S cap / size / offset |
|---|---|---|
| `SvConfig` | 1 / 28 / 0 | 1 / 28 / 0 |
| `SvMapEntryConfig` | 500 / 6 / 32 | 500 / 6 / 32 |
| `NvConfig` | **500** / 11 / 3036 | **500** / 11 / 3036 |
| `ControlLoop` | **200** / 22 / 8540 | **200** / 22 / 8540 |
| `ControlConstants` | 60 / 4 / 12944 | 60 / 4 / 12944 |
| `Linearization` | 100 / 4 / 13188 | 100 / 4 / 13188 |
| `AnalogInput` | 8 / 22 / 13592 | 8 / 22 / 13592 |
| `DigitalInput` | 4 / **20** / 13772 | 4 / **22** / 13772 |
| `AnalogOutput` | 3 / 22 / **13856** | 3 / 22 / **13864** |
| `DigitalOutput` | 11 / 28 / 13926 | 11 / 28 / 13934 |
| `FloatingMotor` | 4 / **14** / 14238 | 4 / **18** / 14246 |

`[CERT]` (full config section `:241-263`, NvConfig `:243`/ControlLoop `:244`; Micro `:816-826`, NvConfig `:818`). The config-section **capacities match exactly across the entire Micro family** (verified PUB4024S, PUB1012S, PVB0000AS, PVB4024NS, all `model_id=8`). What actually differs:

1. **Store SIZES** (record widths) differ for a few stores: `DigitalInput` 20→**22**, `FloatingMotor` 14→**18** in Micro — which cascades the byte **offsets** of every store after them.
2. **The param-file byte LAYOUT is completely reordered** `[CERT]`. Full PUB6438S: `ControlNonVolatile`@**0**, …, `ApplVerNew`@**2059**, `DeviceName`@**2236**, `AlarmDisable`@**2258** (`:265,269,273,274`). Micro PUB4024S: `ApplVerNew`@**0**, `DeviceName`@**35**, `AlarmDisable`@**57**, `ControlNonVolatile`@**92** (`:840-843`). Same stores, same capacities, **totally different offsets**.
3. **The Micro adds a separate `FileOffset` store** (capacity 300, `:854`) written by `BBacnetFileOffsetWriter` ([Block 120] §120.2 File object 304), and reorders `WallModuleBusDeviceAddress` ahead of `WallModuleBusFailDetect`.

**What IS smaller in the Micro is the PHYSICAL I/O (the `<Wiring>` terminal counts), not the download-image store capacities** `[CERT]`:

| Model | `Wiring pins` | AI | DI | AO | DO | Misc |
|---|---|---|---|---|---|---|
| PUB6438S (full, id4) | **42** | 8 | 4 | 3 | 8 | 19 |
| PUB4024S (Micro, id8) | 25 | 5 | 0 | 2 | 4 | 14 |
| PVB4024NS (Micro, id8) | 25 | 5 | 0 | 2 | 4 | 14 |
| PUB1012S (Micro, id8) | 17 | 2 | 0 | 1 | 2 | 11 |
| PVB0000AS (Micro, id8) | **15** | 1 | 0 | 0 | 4 | 10 |

`[CERT]` (`BacnetSpyder.xml:283-300` full Wiring; Micro `:860-...`). These `<Wiring>` counts are exactly the `getMaxConfigurableIO` → `ModelInfo.getWiring().getAnalogInputs().getCount()` values [Block 118] §118.7 cited as model-gating — now read verbatim.

> **The corrected synthesis (replaces B120 §120.6 `[INFER]`)**: a full-sized download would misalign on a Micro **not because the store capacities are smaller (they are not — NvConfig 500, ControlLoop 200 are identical) but because the per-store byte OFFSETS and the param-file LAYOUT are model-specific** (plus a couple of record-size and store-set differences). This is precisely why the writers resolve `getStoreOffset(name)` per-model at runtime ([Block 120] §120.3): the per-model offset map is the thing that absorbs the layout divergence. The "smaller" dimension is the **physical terminal count** (15–25 pins vs 42), which gates how many points can be placed (`getMaxConfigurableIO`), not the size of the transferred image. `[CERT]`

## 122.5 — G6b: LON parallel + the ShortStack stores, concrete `[CERT]`

`LonSpyder.xml` mirrors the structure, with the LON-specific differences B120 §120.7 described now carrying numbers. LON Micro = `model_id=3` (the Model-8 key is BACnet-only; LON uses 1/3/11/12/14). Sampled PUL4024S (`MicroModel3`, `LonSpyder.xml:814`) FileVariables `[CERT]`:

- `NvConfig` cap **220** (vs BACnet's 500 — LON's NV-based architecture sizes this differently), `ControlLoop` 200, `ControlConstants` 60, `Linearization` 100, `AnalogInput` 8, `DigitalOutput` 8 — config capacities again **match full-size** within the LON line.
- **`LonSiData` cap 5548 + `LonAppInitData` cap 266** `[CERT]` — these are the **Echelon ShortStack** stores B120 §120.7 located as store **33 (LonSiData)** / **34 (LonAppInitData)** (`toNetBytes(0)`/`toNetBytes(1)`). The capacities are per-model (the default/Dummy device carries 700/88; PUL4024S carries 5548/266) — confirming the ShortStack image size is model-driven, as B120 inferred.
- LON physical wiring also shrinks for Micro: PUL6438S(full) **42 pins** AI8/DI4/AO3/DO8 → PUL4024S(Micro) **25 pins** AI5/DI0/AO2/DO4 → PUL1012S **16 pins** AI2/DI0/AO1/DO2 (`LonSpyder.xml:194,814,1199` regions). `[CERT]`

This makes the LON ShortStack-store claim of [Block 120] §120.7 (previously `[CERT]` for the mechanism + `[CERT-a]` for the store numbers) now also `[CERT]` on the **store capacities/offsets** data side.

## 122.6 — doc↔code↔data ledger (honest) `[CERT]`/`[INFER]`

| Claim | Prior status | Now | Evidence |
|---|---|---|---|
| Sensor short-ids 0–12 + their Input/Output/Sensor spans | `[CERT-doc]` (B118 §118.5) | **`[CERT]`** | `IO_Common.xml:71-83` |
| Dynamic-field schema names/types/flags | `[CERT]` mechanism, values open | **`[CERT]`** | `IO_Common.xml:54-65` |
| PVL6436AS = pressure + actuator; PVL6438NS = pressure only | `[CERT-doc]` (B116/B118 §118.7 "UNVERIFIABLE code-side") | **`[CERT]`** | `FixedIo_model1/2.xml`; `LonSpyder.xml:1569,1597 / 2740,2767` |
| Per-model fixed-pin assignments (full + Micro) | `[CERT-doc]` | **`[CERT]`** | `IO_Model*.xml`, `IO_Micro_Model*.xml` |
| Model 8 = Micro BACnet family (PVB/PUB 40xx/10xx/0000) | `[CERT]` (B120 §120.6) | **`[CERT]`** (re-confirmed) | `BacnetSpyder.xml` ModelMicroBACnet1-5 |
| Model 8 has **smaller store capacities** | `[INFER]` (B120 §120.6) | **CORRECTED → `[CERT]`**: capacities IDENTICAL; what differs = byte OFFSETS/layout + a few record sizes + a FileOffset store + physical wiring count | `BacnetSpyder.xml` full `:239-278` vs Micro `:814-856` |
| Micro physical I/O is smaller | implied | **`[CERT]`**: 15–25 pins vs 42; AI 1–5 vs 8 | `<Wiring>` counts both files |
| LON ShortStack stores 33/34 capacities | `[CERT-a]` (B120 §120.7) | **`[CERT]`**: LonSiData/LonAppInitData per-model caps | `LonSpyder.xml` PUL4024S |

**Honest residual**: I sampled representative devices per family (one full + the Micro variants) rather than transcribing all 22 BACnet + ~20 LON device blocks; the config-capacity invariance was checked across all five `model_id=8` BACnet devices and the full set of part-numbers, so the Model-8 conclusion is solid. The full per-device offset transcription of every store for every model is mechanical and not load-bearing for any open claim → not exhaustively copied (would be data-dump, not insight).

## 122.7 — Self-verify + marker tally

**Token checks (load-bearing `[CERT]`, grep/read-confirmed verbatim against the XML) — 10 checked, 10 pass:**
1. Sensor table ids 0–12 + Kntc20 `70200→1114 / -45→112` — `IO_Common.xml:71` ✓
2. C7400A id3 current 4-20 mA / 2-10 V, H7655A id7 percent — `IO_Common.xml:74,78` ✓
3. Dynamic-field schema `InputUnit … flags="h1"` etc. — `IO_Common.xml:54-65` ✓
4. FixedIo Model1 Actuator T1=17/T2=18 TravelTime 90.0 + Pressure T1=31 — `FixedIo_model1.xml:23-24` ✓
5. FixedIo Model2 pressure-only — `FixedIo_model2.xml:16` ✓
6. PVL6436AS→IO_Model1.xml (actuator) / PVL6438NS→IO_Model2.xml (no actuator) — `LonSpyder.xml:1597,2767` ✓
7. Model8 = ModelMicroBACnet1-5 PVB4024NS/PVB4022AS/PUB4024S/PUB1012S/PVB0000AS — `BacnetSpyder.xml:2164,1464,760,929,1632` ✓
8. Micro NvConfig cap **500** = full NvConfig cap 500 (NOT smaller) — `BacnetSpyder.xml:818` vs full `:243` ✓
9. Micro param-file reordered: ApplVerNew@0 / ControlNonVolatile@92 (vs full ControlNonVolatile@0 / ApplVerNew@2059) — `BacnetSpyder.xml:840,843` vs `:265,269` ✓
10. Micro physical wiring 25 pins AI5 vs full 42 pins AI8 + LON LonSiData cap 5548 — `BacnetSpyder.xml:860`; `LonSpyder.xml` PUL4024S ✓

**Marker tally**: ≈ **34 `[CERT]`** (all values read verbatim from the bundled XML) · ≈ 3 `[CERT-doc]` (the B116 claims now confirmed) · 1 `[INFER]` (the residual-sampling note). **`[INFER]`/`[CERT]` ratio ≈ 0.03** — the lowest of the Spyder arc, expected: this is a pure data-extract where the XML IS the ground truth. **The investigable evidence for G4b and G6b is now exhausted** — there is no deeper data layer behind these XML files.

**Artifacts**: this block file created; CATALOG regenerated; INDEX.md + RESEARCH-STATE.md updated; engram mirrored. **G4b + G6b COVERED.**

## 122.8 — Connections

- **[Block 118]** — closes its residual micro-gap **G4b**: the sensor `sensorType` ids + spans (§118.5) and the per-model fixed-point values (§118.2/§118.7, the "UNVERIFIABLE code-side" ledger row) are now `[CERT]` from the IO XML. The parsing mechanism B118 documented is here fed its actual data.
- **[Block 120]** — closes its residual micro-gap **G6b** and **corrects** §120.6: Model-8 (Micro) capacities are identical to full-size; the real distinction is byte offset/layout + physical wiring count, which is why `getStoreOffset(name)` is per-model. The LON ShortStack stores 33/34 (§120.7) get concrete capacities.
- **[Block 116]** — the vendor-doc source whose physical-points/model-gating values (§116.4, e.g. PVL6436AS vs PVL6438NS) are now confirmed verbatim in the bundled data → `[CERT-doc]` promoted to `[CERT]`.
- **[Block 117]** — FB catalog; the ModulatingOutput drive-flavor defaults (TravelTime/Period) and the sensor model feed FB pins.
- **[Block 77]** — the drivers that read these registries (`BacnetSpyderModels`/`LonSpyderModels`) at runtime to size the download image.
