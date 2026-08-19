# Block 446 — nrio conversion subsystem (gap B445-G1): the three device→proxy conversions, kitIo thermistor-curve XML, and the NDIO→NRIO migration tool

**Focus:** base corpus (field-I/O drivers axis). Closes **B445-G1**. Continues [B445] (JACE-8000 ↔ IO-R connection).

**Origin:** child gap seeded by [B445] — the scale/offset and thermistor math that turns a raw IO-R terminal reading into an engineering value.

**Scope:** (1) the conversion classes under `nrio-rt/conv`, (2) the `kitIo` thermistor-curve XML format + catalogue, (3) how a curve is carried (XML file vs inline encoded string), (4) the `nrioConversion` module's real purpose. NOT: per-point facets/units UI, calibration statistics.

**Sources (three-source rule):**
- **FUENTE 1 (corpus):** [B445] §445.4 (point-type→proxy-point map). No prior block opened `conv`.
- **FUENTE 2 (niagara-help):** `[CERT-doc]` guides `NrioScaleOffsetCalculation(-1/-2)`, `NrioThermistorType3Chart`, `aNrio500ohmShuntConversion`, `NrioTabularThermistorNotes`, `NrioLinearCalibrationExt`.
- **FUENTE 3 (code):** `[CERT]` `organized/nrio/nrio-rt/vineflower/com/tridium/nrio/conv/*`, `organized/kitIo/kitIo-rt/extracted/xml/*.xml`, `organized/nrioConversion/nrioConversion-wb/.../BNdioToNrioConverter.java`.

---

## 446.1 — Where conversions live (NOT the `nrioConversion` module)

The proxy conversions are `BProxyConversion` subclasses in **`com.tridium.nrio.conv`** (module `nrio`), one per non-trivial signal path: `[CERT]`

| Class | Purpose | Chains a sub-conversion? |
|---|---|---|
| `BNrioThermistorType3Conversion` | Hardcoded Type-3 10K thermistor Ω→°C | no |
| `BNrioTabularThermistorConversion` | Arbitrary thermistor curve (Ω→°C) from a lookup table | no |
| `BNrioShunt500OhmConversion` | 4–20 mA path: volts across the 499 Ω shunt → current, then hands off | **yes** — wraps a secondary `BProxyConversion` (typically Linear) |

Straight 0–10 Vdc and 0–100K resistive inputs use the platform's standard Linear conversion (no nrio-specific class). `[CERT-doc]` (`aNrio500ohmShuntConversion`, [B445] §445.4)

The separate **`nrioConversion`** module is a **red herring for this gap**: it is a `-wb` **migration tool** (`BNdioToNrioConverter` + dialog) that converts a legacy **Ndio** (onboard-IO) station configuration into the newer **Nrio** model — not a runtime conversion. `[CERT]` (`nrioConversion-wb/.../ui/tool/BNdioToNrioConverter.java`)

## 446.2 — The 500 Ω shunt conversion (composition pattern)

`BNrioShunt500OhmConversion.convertDeviceToProxy()` only accepts an **nrio voltage input** whose device unit is convertible to volts; it maps the measured voltage to a current (the 499 Ω supplied resistor turns 4–20 mA into a voltage the UI can read), then **delegates to a secondary conversion** (`subConv.convertDeviceToProxy`) for the final engineering scale. The proxy→device direction throws — these inputs are read-only. `[CERT]` (`BNrioShunt500OhmConversion.java:52-75`)

This is the "500 Ohm Shunt → Linear" chain [B445] §445.4 named: a nrio-specific front stage + a generic linear back stage.

## 446.3 — Thermistor curves: XML catalogue + inline encoding

`BNrioTabularThermistorConversion` interpolates a value against an (ohms, celsius) table. The table is carried **two ways**: `[CERT]`

1. **As a `kitIo` XML resource** — `<thermistor><description>…</description><table><point ohms="…" celsius="…"/>…</table></thermistor>`. Five curves ship in `kitIo-rt/xml/`: `[CERT]`
   - `type_3.xml` (Type 3, 20 points, 0 Ω↔165 °C … 100 000 Ω↔−25 °C)
   - `te6300_10k.xml`, `rs271_0110.xml`, `precon_type_2_model_24.xml`, `precon_type_4_model_42.xml`
2. **As an inline encoded string** — `"Thermistor Type 3|0.0,165.0;610.0,110.0;…;100000.0,-25.0;"` (description `|` then `ohms,celsius;` pairs). This exact string is the class's `DEFAULT`/`NULL` seed, so a fresh point works with the Type-3 curve before any XML import. `[CERT]` (`BNrioTabularThermistorConversion.java` `DEFAULT_ENCODING`)

The two are the same 20-point Type-3 dataset in two serializations — the XML for Workbench import/export/edit, the string for persisted defaults. `kitIo` is exactly the module [B445] §445.1 flagged as required for "Generic Tabular" support. `[CERT-doc]` (`NrioSetup`)

## 446.4 — Scale/offset is stored ON the module

The IO-R processor itself holds per-channel scale/offset; the driver has a `ReadScaleOffsetMessage` (protocol type `MSG_RD_SCALE_OFFSET = 18`) to read it back, rather than computing everything host-side. `[CERT]` (`nrio/messages/ReadScaleOffsetMessage.java`, `NrioMessageConst`) — full protocol in [B448]. Host-side calibration is layered on top via the `NrioLinearCalibrationExt`. `[CERT-doc]` (`NrioLinearCalibrationExt`)

---

## 446.5 — Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | Conversions live in `com.tridium.nrio.conv`: Type3, TabularThermistor, Shunt500Ohm | `[CERT]` | `conv/*.java` |
| 2 | Shunt500Ohm accepts only nrio voltage input and delegates to a chained sub-conversion; read-only | `[CERT]` | `BNrioShunt500OhmConversion.java:52-75` |
| 3 | Thermistor curve = (ohms,celsius) table; carried as kitIo XML or as inline `desc|o,c;…` string | `[CERT]` | `type_3.xml`; `BNrioTabularThermistorConversion.java` DEFAULT_ENCODING |
| 4 | Five curves ship in kitIo: type_3, te6300_10k, rs271_0110, precon_type_2_model_24, precon_type_4_model_42 | `[CERT]` | `kitIo-rt/extracted/xml/` |
| 5 | `nrioConversion` module = Ndio→Nrio migration WB tool, not a runtime conversion | `[CERT]` | `BNdioToNrioConverter.java` |
| 6 | Module stores scale/offset; driver reads it via MSG_RD_SCALE_OFFSET=18 | `[CERT]` | `ReadScaleOffsetMessage.java`, `NrioMessageConst:32` |

**Tally:** 6 claims — 5 `[CERT]` · 1 `[CERT]`+`[CERT-doc]` · 0 `[INFER]` · 0 unmarked. No contradictions.

**Left out (named):** the interpolation algorithm's edge handling (out-of-range clamp vs extrapolate) in `convertTo`; the units/facets plumbing; calibration-ext statistics.

## 446.6 — Connections
- **Closes B445-G1**; completes [B445] §445.4 point-type map with the conversion engine behind it.
- **Protocol dependency** → [B448] (B445-G3): `MSG_RD_SCALE_OFFSET` is one message of the wire protocol documented there.
- **Migration angle** ties to legacy `ndio` (onboard-IO) — the `nrioConversion` tool exists to move AX-era Ndio stations onto Nrio.

## 446.7 — Open gaps
- **B446-G1** — the `convertTo` interpolation edge policy. **CLOSED → [B450]** (clamps at both ends; linear within band; bidirectional).
