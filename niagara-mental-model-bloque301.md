# Block 301 — Licensing: there is no `modbus` feature — there are four, one per palette; the TCP gateway spends the `modbusTcp` licence; and the guide's limits topic quotes an MS/TP example

> Focus **modbus**, gap **M7** (also closing **M1-lic** and **M1-gw**, opened by [Block 294]). Which
> licence features the Modbus driver actually checks, where the check lives, what limits the reference
> install's licence carries, and why the official "Limits imposed by the Modbus licenses" topic is
> misleading. READ-ONLY. Corpus language: ENGLISH.
>
> Sources (primary): `sources/decompiled/` — the five `-rt` network classes;
> `/mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162/security/licenses/*.license` (the reference install's
> own licence files).
> Official documentation: `sources/manuals/docModbus-N4.14-guide.md` §Modules,
> §Limits imposed by the Modbus licenses.
>
> **SECRETS DISCIPLINE** (METHODOLOGY §12, `live-install` material): this block cites licence
> **structure** — feature names, limit attribute names, expiry semantics, host-id *format* — and never
> reproduces a full host id, a signature, a certificate or any key material. Zero secret values.
>
> Markers: `[CERT]` local primary source (`file:line` / file) · `[CERT-doc]` official Tridium guide
> (§topic) · `[INFER]` deduction.
>
> Layer 26 (Communication protocols — driver focus). Connects [Block 294] (the four palettes these four
> features mirror; the gateway question), [Block 295]/[Block 298] (what the licensed networks do).

---

## 301.1 — Four features, one per palette `[CERT]`

Each concrete network class declares its own licence feature, and the four are distinct `[CERT]`:

| Network class | `getLicenseFeature()` returns | File |
|---|---|---|
| `BModbusAsyncNetwork` | `getFeature("tridium", "modbusAsync")` | `modbusAsync-rt/…/BModbusAsyncNetwork.java:182-183` |
| `BModbusTcpNetwork` | `getFeature("tridium", "modbusTcp")` | `modbusTcp-rt/…/BModbusTcpNetwork.java:51-52` |
| `BModbusSlaveNetwork` | `getFeature("tridium", "modbusSlave")` | `modbusSlave-rt/…/BModbusSlaveNetwork.java:100-101` |
| `BModbusTcpSlaveNetwork` | `getFeature("tridium", "modbusTcpSlave")` | `modbusTcpSlave-rt/…/BModbusTcpSlaveNetwork.java:97` |

All four are `public final` — `[INFER]` deliberately non-overridable, so a subclass cannot substitute a
cheaper feature.

**This corrects the guide's framing.** §Modules states the prerequisite as a single feature: *"you must
have a target host (remote controller) that is licensed for the feature `modbus`, or a PC host, which acts
as a Modbus Supervisor. The Supervisor must also be licensed for `modbus`"* `[CERT-doc]`. No such feature
is requested anywhere in the driver, and none exists in the reference install's licence — the vendor
namespace is `tridium` and the four names above are what `getFeature` asks for `[CERT]`.

`[INFER]` the mapping is exactly the four palettes of [Block 294] §294.2/§294.3: `modbusCore` has no
palette **and no licence feature** (it is never licensed independently because it is never deployed
independently), while each of the four transports is separately licensable. An integrator can therefore be
licensed for Modbus TCP and *not* for Modbus serial — the guide's singular "the feature modbus" hides that.

## 301.2 — The TCP gateway spends the `modbusTcp` licence — M1-gw closed `[CERT]`

[Block 294] §294.1 established that `BModbusTcpGateway extends BModbusTcpNetwork`, and [Block 294] left open
how it counts for licensing. Resolved by absence: `BModbusTcpGateway` **does not declare
`getLicenseFeature()`** `[CERT]` (grep over the file returns no match), so it inherits
`BModbusTcpNetwork`'s — and since that method is `final`, it could not have overridden it anyway.

`[INFER]` a TCP/serial gateway network is licensed as Modbus **TCP**, not as Modbus serial, even though
everything behind it is RTU/ASCII on RS-485. That is the commercially favourable reading for the
integrator: gatewaying a serial trunk needs no `modbusAsync` licence.

## 301.3 — What the reference install's licence actually grants `[CERT]`

The four Modbus features in `security/licenses/Webs.license` carry an identical limit set `[CERT]`:

```
feature name="modbusAsync"    expiration=<date> history.limit="none" point.limit="none"
                              schedule.limit="none" device.limit="none"
feature name="modbusSlave"    … same shape
feature name="modbusTcp"      … same shape
feature name="modbusTcpSlave" … same shape
```

So **all four limit attributes are `none`** — unlimited points, devices, histories and schedules *as far as
the Modbus features are concerned* `[CERT]`.

`[INFER]` therefore the binding constraint on a Modbus integration is **never the Modbus feature**; it is
the station-wide capacity in the host's own licence (the `globalCapacity` point/device ceiling). A capacity
exhaustion that shows up while adding Modbus points is a station-level limit, not a Modbus-licence limit —
which is exactly the wrong place most people look first.

Two hosts appear under `security/licenses/db/`, one QNX-family (a JACE controller) and one Windows
(the supervisor) `[CERT]` — host-id **format** is `<platform>-XXXX-XXXX-XXXX-XXXX`; the values are not
reproduced here per the secrets discipline. Their expiry semantics differ: the QNX host's Modbus features
carry `expiration="never"`, the Windows host's carry a dated expiry `[CERT]`. `[INFER]` the usual Tridium
split — a controller licence is perpetual, a supervisor licence is term-based and tracks the SMA. A lapsed
supervisor licence would therefore stop Modbus on the supervisor while the JACE kept running its own.

## 301.4 — The guide's limits topic documents a different protocol — M1-lic closed `[CERT]`

§Limits imposed by the Modbus licenses, in full, illustrates its subject with this XML `[CERT-doc]`:

```xml
<feature name="mstp" expiration="2025-01-31" port.limit="5"/>
```

and then explains `port.limit` as *"the number of MS/TP trunks (RS-485 ports) that can be used"*, adds the
EIA-485 load-factor rule (*"31 (full load) to up to 127 (quarter load) devices"*), and closes with *"Other
device or platform limits in the license's `modbus` feature also apply."* `[CERT-doc]`

Measured against the reference install `[CERT]`: the string `port.limit` occurs **exactly once** in
`Webs.license`, and it is on the `mstp` feature — **not on any of the four Modbus features**, none of which
carries a `port.limit` attribute at all.

`[INFER]` so the topic is a BACnet MS/TP page repurposed for the Modbus guide: its example, its attribute
and its device-count rule all belong to MS/TP. What survives as genuinely applicable is the *electrical*
part — RS-485 load factors are a property of the bus, not of the protocol riding it, so the 31/127 figure
does apply to a `modbusAsync` trunk. The `port.limit` mechanism does not, in this licence. And the closing
sentence points at a `modbus` feature that §301.1 showed does not exist.

This closes **M1-lic** with a negative finding: there is no evidence that `port.limit` is a Modbus feature
attribute; the one measurement available (this install's licence) says it is not. A different licence SKU
could in principle carry one — the claim is bounded to what is measurable here.

## 301.5 — What the guide does NOT resolve

- that there are **four** features rather than one (§301.1), so partial licensing is possible;
- that `modbusCore` is **never** licensed separately;
- that the **gateway** is licensed as TCP (§301.2);
- that in practice the **station capacity**, not the Modbus feature, is the ceiling (§301.3);
- that its own limits example is MS/TP (§301.4).

## 301.6 — Self-verify

`verify-block.sh` tally (COMPUTED — `adj` strips the header legend):

| Marker | raw | adj |
|---|---|---|
| `[CERT]` | 20 | 19 |
| `[CERT-doc]` | 6 | 5 |
| `[CERT-hw]` / `[CERT-live]` / `[CERT-web]` / `[CERT-a]` | 0 | 0 |
| `[INFER]` | 7 | 6 |
| **[INFER]/[CERT*] ratio** | | **6/24 = 0.25** |

Script exit 0.

**Block type: EVIDENCE.**

Load-bearing claims:

| # | Claim | Marker | Verified how |
|---|---|---|---|
| 1 | Four distinct `getFeature("tridium", …)` names, one per network | `[CERT]` | all four files read at the cited lines |
| 2 | All four `getLicenseFeature()` are `public final` | `[CERT]` | signature at each cited line |
| 3 | `BModbusTcpGateway` declares no `getLicenseFeature()` | `[CERT]` | grep over the file → no match (ABSENCE, re-derived: the inherited method is `final`, so an override is impossible by construction) |
| 4 | Guide asks for a singular feature named `modbus` | `[CERT-doc]` | verbatim, §Modules |
| 5 | No feature named `modbus` exists in the install's licence | `[CERT]` | enumeration of all `feature name="…modbus…"` matches → exactly the four |
| 6 | The four features carry `point.limit`/`device.limit`/`history.limit`/`schedule.limit` = `none` | `[CERT]` | attribute values read directly from `Webs.license` |
| 7 | `port.limit` occurs once, on `mstp` | `[CERT]` | `rg -c 'port.limit'` → 1, and the matching line is the `mstp` feature |
| 8 | Two hosts under `licenses/db/`, QNX and Windows, with different expiry semantics | `[CERT]` | directory listing + the `expiration` attribute of each |
| 9 | Guide's limits topic text (port.limit explanation, 31/127 load rule) | `[CERT-doc]` | verbatim, §Limits imposed by the Modbus licenses |

Tokens grep-confirmed in their cited source: **9 / 9**. Claims 3, 5 and 7 are ABSENCES; each was derived by
enumeration over the full relevant scope (the file / all `feature name` matches / all `port.limit`
occurrences) rather than a single spot check, per the RE-MEASURE rule.

**Secrets check**: no host id, signature, certificate, key or credential value appears in this block. Only
attribute names, limit values (`none`), expiry semantics and the host-id format are cited. Model tier:
**no delegation — inline**.

## 301.x — Connections

- **[Block 294]** — §294.2's "four palettes, `modbusCore` has none" is mirrored exactly by "four licence features, `modbusCore` has none"; §294.1's gateway-is-a-network is what makes §301.2 fall out.
- **[Block 298]** — the two slave features (`modbusSlave`, `modbusTcpSlave`) license the server role documented there.
- **[Block 279]** — the MS/TP material the guide's limits topic actually describes belongs to that block's subject, not this one.

**Gaps opened by this block**: none. **Gaps closed beyond M7**: **M1-lic** (§301.4, negative finding) and
**M1-gw** (§301.2), both opened by [Block 294].
