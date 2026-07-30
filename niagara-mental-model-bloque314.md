# Block 314 — The Honeywell OEM layer: it supplies the discovery Tridium's driver does not have, by embedding the register map in code

> Focus **modbus**, gap **M9**. What `honeywellModbusDeviceManager` (14 classes) and
> `honeywellModbusSmartSensor` (25) add **on top of** the base driver reconstructed in B294–B313.
> [Block 94], [Block 95] and [Block 250] already covered these modules from the OEM/hardware angle — those
> findings are **remitted, not re-derived**. This block documents only the delta against the driver.
> READ-ONLY. Corpus language: ENGLISH.
>
> Sources (primary): `organized/honeywellModbusDeviceManager/**/vineflower/` (14 classes),
> `organized/honeywellModbusSmartSensor/**/vineflower/` (25 classes), N4.14.0.162.
>
> Markers: `[CERT]` local primary source (`file:line` / class inventory) · `[INFER]` deduction.
>
> Layer 26 (Communication protocols — driver focus). Connects [Block 304] (**the discovery absence this
> completes**), [Block 297] (the ProxyExt this OEM subclasses), [Block 94]/[Block 95]/[Block 250]
> (remitted), [Block 28] (cross-protocol discovery).

---

## 314.1 — What is already documented — remitted `[CERT]`

Not re-derived here:

- **[Block 94]** — the Honeywell Device Manager family: agnostic core + two transports (BACnet and Modbus),
  the firmware-OTA flow, BACnet commissioning with its instance-ID pool, and the finding that **firmware OTA
  ships without integrity guarantees** (§94.4).
- **[Block 95]** — Smart Edge Devices: the TR50 IAQ sensor series (BACnet **and** Modbus) and TR100 wall
  modules, both sitting on the shared `honIOBase` layer.
- **[Block 250]** §250.3 — `honeywellModbusSmartSensor` as the TR50 air-quality sensor, seen from the
  migration angle.

This block asks a different question: **what do these modules add to the Modbus driver of B294–B313?**

## 314.2 — The delta that matters: OEM discovery `[CERT]`

[Block 304] §304.1 measured **zero** `Discover`/`LearnJob` hits across every `modbus*` module — Tridium's
Modbus driver has no discovery, because the protocol has none to offer.

The Honeywell modules do `[CERT]` (class inventory):

| Class | Module |
|---|---|
| `BHonModbusDiscoverDevicesJob` | `honeywellModbusDeviceManager` (`…/modbus/jobs/`) |
| `BHonModbusDiscoveryDevice` | `…/modbus/device/` |
| `BIHonModbusDeviceLearn` · `BIHonModbusDeviceModel` | `…/modbus/learn/` |
| `HonModbusDeviceLearn` | `…/modbus/ui/` |
| `BHonModbusDiscoverPointsJob` | `honeywellModbusSmartSensor` (`…/modbus/jobs/`) |
| `BHonModbusSmartSensorDiscoveryPoint` | `…/modbus/config/` |

**This does not contradict [Block 304].** That block's measurement was explicitly scoped to the `modbus*`
trees, and these modules are named `honeywellModbus*` — outside that scope by construction. The two results
compose: `[INFER]` **discovery for Modbus exists in this install, but it is supplied by the OEM, not by the
driver.** Tridium ships the protocol machinery; Honeywell ships the device knowledge that makes discovery
possible at all.

Both a **device** learn (device manager) and a **point** learn (smart sensor) exist, matching the two levels
[Block 28] §28.1 identified as the canonical Niagara discovery pattern.

## 314.3 — How they can discover what Modbus cannot describe `[CERT]`

The mechanism is the interesting part. `SmartSensorModbusRegisterDetails` is 72 lines of **compiled-in
register constants** `[CERT]`
`honeywellModbusSmartSensor-rt/…/modbus/config/SmartSensorModbusRegisterDetails.java:4-15+`:

```java
public static final int TEMP_SENSOR_REGISTER_ID  = 1;
public static final int HUM_SENSOR_REGISTER_ID   = 2;
public static final int CO2_SENSOR_REGISTER_ID   = 3;
public static final int PM25_SENSOR_REGISTER_ID  = 4;
public static final int TVOC_SENSOR_REGISTER_ID  = 5;
public static final int PM1_SENSOR_REGISTER_ID   = 14;
public static final int PM10_SENSOR_REGISTER_ID  = 24;
public static final int AQI_SENSOR_REGISTER_ID   = 30;
public static final int TEMP_ALARM_LOW_LIMIT_ID  = 2030;
public static final int TEMP_ALARM_HIGH_LIMIT_ID = 2050;
public static final int TEMP_ALARM_DEADBAND_ID   = 2070;
public static final int TEMP_ALARM_TIME_DELAY_ID = 2090;
```

`[INFER]` this is the direct answer to [Block 304] §304.1's structural point. Modbus discovery is impossible
in general because a slave cannot be asked what its registers mean — so the OEM **hard-codes the map for its
own hardware** and "discovers" against that. It is not protocol discovery; it is a built-in device profile
being applied. That works precisely because Honeywell controls both ends: it knows that on a TR50, register
3 is CO₂.

`[INFER]` and it is exactly the manual typing-from-the-vendor-datasheet that [Block 304] §304.5 described as
the standard Modbus workflow — done once, in code, by the vendor, instead of once per station by the
integrator.

## 314.4 — Eight typed sensors and four device models `[CERT]`

Rather than generic points, the module ships one class per measurement `[CERT]`
(`…/modbus/sensors/`, 8 classes): `BHonModbusTemperatureSensor`, `BHonModbusHumiditySensor`,
`BHonModbusCo2Sensor`, `BHonModbusPM1Sensor`, `BHonModbusPM25Sensor`, `BHonModbusPM10Sensor`,
`BHonModbusTVOCSensor`, `BHonModbusAQISensor` — matching the eight register constants of §314.3
one-for-one.

Four device models cover the hardware variants `[CERT]` (`…/modbus/device/`):
`BHonModbusTR503D`, `BHonModbusTR503N`, `BHonModbusTR505D`, `BHonModbusTR505N`, above the common
`BHonModbusSmartSensorDevice`. `[INFER]` the `3`/`5` and `D`/`N` suffixes are model variants of the TR50
series [Block 95] §95.2 documented from the hardware side.

Two configuration commands round it out `[CERT]` (`…/modbus/command/`):
`HonModbusConfigurationDownload`, `HonModbusConfigurationUpload`, over the shared
`HonModbusSmartSensorCommand`.

## 314.5 — The OEM subclasses the base point model `[CERT]`

`BHonModbusClientNumericProxyExt extends BModbusClientNumericProxyExt` `[CERT]`
`honeywellModbusSmartSensor-rt/…/modbus/point/BHonModbusClientNumericProxyExt.java:11`, alongside
`BHonModbusPointDeviceExt` and `BHonModbusPointFolder`.

`[INFER]` so the OEM does **not** bypass the driver: it extends the exact class [Block 297] §297.3
documented, inheriting the datatype model, the byte-order resolution ([Block 296] §296.1) and the
grouping decision ([Block 295] §295.1). Everything B294–B313 established about the base driver applies
underneath the Honeywell layer — including the unsafe defaults, since nothing here overrides
`usePresetMultipleRegister` or the address format.

There is also a dedicated point manager, `BHonModbusSmartSensorPointManager` `[CERT]` (`…/modbus/ui/`),
`[INFER]` the OEM counterpart of the nine-column `BModbusPointManager` of [Block 304] §304.3 — presumably
narrower, since the register map is fixed and most columns would have nothing to configure.

## 314.6 — Self-verify

`verify-block.sh` tally (COMPUTED — `adj` strips the header legend):

| Marker | raw | adj |
|---|---|---|
| `[CERT]` | 24 | 23 |
| `[CERT-doc]` | 3 | 3 |
| `[CERT-hw]` / `[CERT-live]` / `[CERT-web]` / `[CERT-a]` | 0 | 0 |
| `[INFER]` | 10 | 9 |
| **[INFER]/[CERT*] ratio** | | **9/26 = 0.35** |

Script exit 0.

**Block type: EVIDENCE.**

Load-bearing claims:

| # | Claim | Marker | Verified how |
|---|---|---|---|
| 1 | B94/B95/B250 cover the OEM/hardware angle | `[CERT]` | section headings of all three blocks read |
| 2 | The six discovery/learn classes exist in the two OEM modules | `[CERT]` | full class enumeration of both trees |
| 3 | These modules are outside [Block 304]'s `modbus*` scope | `[CERT]` | module names are `honeywellModbus*`; §304.1's query was scoped to `modbus*` |
| 4 | `SmartSensorModbusRegisterDetails` is 72 lines of register constants | `[CERT]` | `wc -l` + `:4-15` read verbatim |
| 5 | The eight register IDs as listed | `[CERT]` | `:4-11` verbatim |
| 6 | Eight sensor classes, one per measurement | `[CERT]` | directory enumeration of `…/sensors/` |
| 7 | Four TR50x device models | `[CERT]` | directory enumeration of `…/device/` |
| 8 | `BHonModbusClientNumericProxyExt extends BModbusClientNumericProxyExt` | `[CERT]` | `:11` |
| 9 | Two configuration commands | `[CERT]` | directory enumeration of `…/command/` |
| 10 | No TR100 class in either `honeywellModbus*` tree | `[CERT]` | `rg -l 'TR100'` over both → no files (ABSENCE, full-scope) |
| 11 | TR100 classes exist only under `honeywellBacnetWallModule` | `[CERT]` | `fd -e java 'TR100'` corpus-wide → 3 BACnet device classes |
| 12 | The TR100 Modbus guide is 2082 lines with the listed chapters | `[CERT-doc]` | `wc -l` + table of contents read |
| 13 | The guide had no prior citation in the corpus | `[CERT]` | `rg -l 'TR100_Modbus_Integration'` over all blocks → none |

Tokens grep-confirmed in their cited source: **9 / 9**. Claim 3 is a **scope reconciliation**, not a
correction: [Block 304]'s negative stands exactly as measured, and this block extends the picture rather
than overturning it. Stating that distinction precisely mattered — the sloppy version ("B304 was wrong
about discovery") would have been false.

§314.5's closing sentence about the OEM point manager being "presumably narrower" is marked `[INFER]` and
is **not** verified — the class was inventoried, not read. Flagged rather than asserted.

`[CERT-doc]`: the TR100 guide is cited in §314.7. **Self-correction recorded**: I first wrote that this
guide was out of scope because [Block 95] §95.4 documents the TR100 as the BACnet variant. Opening the file
before committing showed the opposite — it is titled *"TR100 Wall Module — Modbus Integration Guide"* and is
2082 lines of Modbus content. The claim was wrong and was replaced by §314.7, which turned out to be the
better finding. Cost of checking: one command.

No new sources preserved. Model tier: **no delegation — inline**.

## 314.7 — The TR100 is the counter-example, and it proves the rule `[CERT]` / `[CERT-doc]`

The TR50 has an OEM module with an embedded register map (§314.3). The **TR100 wall module does not** —
for Modbus.

Measured `[CERT]`:

- `rg -l 'TR100'` over both `honeywellModbus*` trees → **no files**;
- the only TR100 classes in the corpus are BACnet — `honeywellBacnetWallModule-rt/…/bacnet/device/`
  holds `BHonBacnetTR100T`, `BHonBacnetTR100TH`, `BHonBacnetTR100THC`;
- yet a **2082-line** official guide exists: *"TR100 Wall Module — Modbus Integration Guide"*
  (`niagara-help/docs-text/TR100_Modbus_Integration_Guide_31-00748.txt`) `[CERT-doc]`, whose chapters are
  *Setting Up Modbus Network*, *Modbus Object Points*, *Modbus Scaling Factor Usage*, and then point groups
  by function — Alert, Device, Display Values, Fan Control, General Setting and Monitoring, HVAC Mode
  Control.

`[INFER]` so Honeywell integrates its own two devices **two different ways**:

| Device | Modbus support | How points are created |
|---|---|---|
| TR50 (IAQ sensor) | OEM module with discovery | register map compiled in (§314.3) → learn job |
| TR100 (wall module) | **none — generic driver only** | integrator types them from a 2082-line PDF |

That is [Block 304] §304.1's thesis with both cases in one vendor's product line: **without an OEM module,
Modbus integration is manual transcription from the vendor's document.** The TR100 guide *is* the register
map — it just lives in a document instead of in a class, and a human is the compiler.

`[INFER]` the guide's own structure confirms the workflow [Block 304] §304.5 described: it opens by telling
the integrator to set up the Modbus network, then lists the object points to create by hand. Its *"Modbus
Scaling Factor Usage"* chapter is the documentary counterpart of the datatype/byte-order decisions
[Block 297] §297.4–§297.5 documented in code.

**First citation of this guide in the corpus** — verified: `rg -l 'TR100_Modbus_Integration'` over all
blocks returned no prior hit.

## 314.x — Connections

- **[Block 304]** — §304.1's measured absence of discovery in the base driver; §314.2 shows who supplies it and §314.3 how.
- **[Block 297]** — the ProxyExt the OEM subclasses; its whole type model applies beneath this layer.
- **[Block 94]**, **[Block 95]**, **[Block 250]** — remitted (§314.1); the OEM/hardware view, not repeated.
- **[Block 28]** — the two-level (device + point) discovery pattern these jobs follow.

**Gaps opened by this block**: none for this focus. The OEM layer's own behaviour (firmware OTA integrity,
the sensor alarm limits at registers 2030–2090) belongs to the **`oem-honeywell-tail`** focus, which is
paused at 9/17 with U10–U15 open — that is where a deeper pass on these modules belongs, not here.
