# Block 476 — The install ships two SNMP modules, and the live station runs the newer `nSnmp` (NDriver): it has SNMPv3 USM, native traps, and typed proxy exts — scoping [Block 28] §28.6's install-wide "no v3" to the classic `snmp` module

> **Sources**: local corpus (decompiled + original Tridium source). Root: `/home/cristian/modules/Prototipos/modulos/organized`.
> `docSource/docSource-doc/extracted/…` = original Tridium source with real javadoc (highest fidelity); everything else is
> decompiled (`vineflower/` preferred; `decompiled/` and `pipeline/procyon/` are the same bytecode via other decompilers,
> used only where vineflower name-mangled identifiers to `ln`/`n`). All paths below are relative to that root.
> **Scope**: the two SNMP modules of N4.14.0.162 and what the newer one (`nSnmp`) actually implements — driver class,
> point scaling, enum resolution, SNMPv3, traps, licensing. This corrects the *scope* of [Block 28] §28.6, which measured
> only `snmp-rt.jar` and generalised "no v3" to the whole install. Motivated by a live cross-session integration
> (Panduit SNMP homelab → this N4), where the binding target had to be the module the live station really runs.
> **Markers**: `[CERT]` local primary source (`file:line`) · `[INFER]` deduction. No live station was polled; every claim is code.

## 476.1 — What [Block 28] §28.6 established, and its scope error `[CERT]`

[Block 28] §28.6 (`niagara-mental-model-bloque28.md:537`) inventoried the **classic** SNMP driver by unzipping `snmp-rt.jar`:
`BSnmpWalkMibJob`, `BSnmpTableWalkMibJob`, `SnmpReceiveTraps` (`:542-545`), the GetNextRequest walk, static `.mib` parsing,
trap→`BAlarmRecord`, and — at `:587-594` — "**Ausencia de v3**": a grep of `snmp-rt.jar` for `v3|usm|auth|priv` returned
nothing, so it concluded *"SNMPv3 con USM … no aparece en el distro Honeywell … este driver no sirve"*.

That measurement of `snmp-rt.jar` is **correct**. The **generalisation is not**: the same install also ships a second,
newer SNMP module — **`nSnmp`** — and that module has full SNMPv3 USM. §28.6.5's claim is true of the class it grepped and
false of the install. This block scopes it: **"no v3" belongs to the classic `snmp` module, not to N4.14.** [Block 28] §28.6
was edited with a pointer here.

## 476.2 — Two modules, and the live station runs the newer one `[CERT]`

Two distinct modules, both vendor Tridium, both `4.14.0.162`, both dated `2024-05-28`:

| Module | `module.xml` | `preferredSymbol` | Java root | Character |
|---|---|---|---|---|
| classic `snmp` | `snmp/snmp-rt/…/META-INF/module.xml` (`description="SNMP Driver"`) | `snmp` | `com.tridium.snmp.*` | MIB-walk jobs + `SnmpReceiveTraps` (the [Block 28] one) |
| **`nSnmp`** | `nSnmp/nSnmp-rt/extracted/META-INF/module.xml:2` (`description="SNMP Driver with NDriver Framework"`) | **`ns`** | `com.tridium.nSnmp.*` | NDriver-framework driver: typed proxy exts, v3 USM, native trap ext, agent side |

`nSnmp`'s network type is declared `<type name="SnmpNetwork" class="com.tridium.nSnmp.BSnmpNetwork"/>`
`[CERT]` `nSnmp/nSnmp-rt/extracted/META-INF/module.xml:26`, so its Baja namespace is **`ns:`**.

The live PRUEBAS station ([Block 123]) runs **`ns:SnmpNetwork@26587`** `[CERT]` (`niagara-mental-model-bloque123.md:65`).
Namespace `ns:` resolves to `nSnmp` (the `preferredSymbol`), **not** the classic `snmp`. Therefore the production
deployment uses `nSnmp`, and any binding work targets `com.tridium.nSnmp.*`, not the module [Block 28] measured. `[INFER]`
(namespace→module resolution) — but corroborated by the class name in the `.bog`.

## 476.3 — Typed proxy exts, and where scaling really lives `[CERT]`

`nSnmp` binds one **typed proxy ext per SNMP datatype** under `com.tridium.nSnmp.point`: `BSnmpNumericProxyExt`,
`BSnmpBooleanProxyExt`, `BSnmpEnumProxyExt`, `BSnmpStringProxyExt`, all extending `BSnmpProxyExt`. The base carries exactly
three configuration properties — `objectIdentifier`, `variableType`, `displayHint` — and an OID validity gate
`[CERT]` `nSnmp/nSnmp-rt/vineflower/com/tridium/nSnmp/point/BSnmpProxyExt.java:27-49`. **There is no `scale` slot on the ext.**

`BSnmpNumericProxyExt.setValue()` reports the device integer **raw**: `readOk(new BStatusNumeric(val))` with no scaling
`[CERT]` `.../point/BSnmpNumericProxyExt.java:20-28`. Scaling therefore cannot be "a facet on the ext". It lives in the
inherited proxy-conversion pipeline, because the type chain is:

```
BSnmpNumericProxyExt → BSnmpProxyExt → com.tridium.ndriver.point.BNProxyExt → javax.baja.driver.point.BProxyExt
```

`BNProxyExt` is a thin `extends BProxyExt` `[CERT]` `ndriver/ndriver-rt/vineflower/com/tridium/ndriver/point/BNProxyExt.java:9`,
and `BProxyExt` owns the `conversion` property (type `BProxyConversion`, default `BDefaultProxyConversion`) plus `deviceFacets`
`[CERT]` `docSource/docSource-doc/extracted/driver-rt/javax/baja/driver/point/BProxyExt.java:85,245`. The linear conversion is
`BLinearConversion extends BProxyConversion`, `make(double scale, double offset)`, forward `proxy = device*scale + offset`,
reverse `device = (proxy-offset)/scale` `[CERT]`
`docSource/.../javax/baja/driver/point/conv/BLinearConversion.java:34-47,89-112`.

**Consequence**: a ÷10 device scale (275 → 27.5) is `conversion = BLinearConversion.make(0.1, 0)`, **not** a `scale=0.1`
facet. The reverse equation makes writes self-correcting, so the same conversion is valid for phase-2 writable points.
Idle sentinels (`255`, `2147483647`) are scaled blindly by this pipeline (→ `25.5`, `214748364.7`) and must be gated by
status/range separately — the conversion does not filter them `[INFER]` (the conversion applies unconditionally per
`BLinearConversion.convert`).

Read/write direction is framework-enforced: `BSnmpProxyExt.getMode()` returns `writeonly`/`readonly` from
`getParentPoint().isWritablePoint()` `[CERT]` `.../point/BSnmpProxyExt.java:91-93`. So "map an OID read-only" means using a
**non-writable point type** (`BNumericPoint`/`BBooleanPoint`, not `…Writable`); the ext will not issue a write for it.

## 476.4 — Enum ranges are auto-populated from the MIB at learn time `[CERT]`

At runtime `BSnmpEnumProxyExt.setValue()` resolves the enum via a **`range` facet**:
`BEnumRange range = (BEnumRange)getDeviceFacets().getFacet("range"); … SnmpUtil.getEnum(var, range)`
`[CERT]` `.../point/BSnmpEnumProxyExt.java:24-33`. The question is who fills that facet. The learn/add path in the wb point
manager does it from the parsed MIB (read from `decompiled/`, because vineflower mangled this class):

- `BSnmpPointManager.java:732,755` — gate: `objSyn.equalsIgnoreCase("INTEGER") && entry.hasEnumRange()`.
- `:787` — `newFacets = BFacets.make(newFacets, BFacets.makeEnum(entry.getEnumRange()))` → the MIB's `INTEGER {…}` enumeration
  becomes the point's `range` facet.
- `:757,790-791` — an enum of **exactly two ordinals** is instead materialised as a `BBooleanPoint` with the two tags as
  `trueText`/`falseText` (`BFacets.makeBoolean(er.getTag(ords[0]), er.getTag(ords[1]))`).
- `:850,859-860` — `deviceFacets = pt.getFacets().newCopy(); ext = new BSnmpEnumProxyExt(); ext.setDeviceFacets(deviceFacets)`
  → the point facets (now carrying `range`) are copied into the ext's `deviceFacets`, which is exactly what §476's runtime
  read consumes.

The MIB model itself carries the enumeration: `OidEntry.enumRange` and `BMibListEntry.enumRange` (`BEnumRange`) with
`hasEnumRange()`/`getEnumRange()` `[CERT]` `.../mib/OidEntry.java`, `.../datatypes/BMibListEntry.java`.

**Consequence**: enum points need **no manual per-point range table** — discovery auto-fills them from the compiled MIB,
provided the MIB compiled with its `INTEGER {…}` enumeration intact. Two-value enums (outlet off/on, enable/disable) arrive
as **Boolean**, not Enum. A manual `INTEGER→label` table is a *verification reference* (does the compiled enum match the
device's real values?), not a required input.

## 476.5 — SNMPv3 USM is present — SHA-only auth, DES/AES privacy `[CERT]`

`BSnmpDevice` carries the full USM stack `[CERT]`
`nSnmp/nSnmp-rt/decompiled/com/tridium/nSnmp/BSnmpDevice.java:99` (the `@NiagaraProperties` block):
`snmpVersion` (default **2**, `makeInt(null,1,3)`), `userName`, `securityLevel` (`BUsmSecurityLevel.noAuthNoPriv`),
`authenticationProtocol` (`BUsmAuthenticationProtocol.sha`), `authenticationPassphrase` (`BPassword`),
`privacyProtocol` (`UsmPrivacyProtocol.aes128`), `privacyPassphrase`, `contextName`, `engineID`. The whole
`com.tridium.nSnmp.version3.*` package exists (`SnmpV3Communicator`, `messageProcessingModel/SnmpMessageProcessingModel_V3`,
`securityModel/usmUser/BUsmUserTable`, `SnmpV3AlarmTrap`).

The offered algorithm set is narrow and matters for interop/FIPS:

- **Auth** — `BUsmAuthenticationProtocol` declares **only `sha`** (`@NiagaraEnum range={sha}`, `SHA=101`); **no MD5**
  `[CERT]` `.../version3/securityModel/usm/authentication/BUsmAuthenticationProtocol.java:22-25`.
- **Privacy** — `UsmPrivacyProtocol`: `DES=201`, `AES128=202`, `AES192=203`, `AES256=204` `[CERT]`
  `.../version3/securityModel/usm/privacy/UsmPrivacyProtocol.java:17-24`. The enum's own `DEFAULT` is `des`, but the
  device property default is `aes128`.

**Corrects [Block 28] §28.6.5**: SNMPv3 USM *is* in the install — in `nSnmp`, the module the live station runs. The classic
`snmp` module still lacks it, so §28.6.5 stands *for that module*. Interop rule that falls out of the SHA-only auth: the
per-device question is not "does it do v3?" but "does it do v3 with **SHA auth + AES privacy**?" — an MD5-auth-only device
cannot speak v3 to this driver at all `[INFER]` (no MD5 protocol is offered).

## 476.6 — Native trap reception, single-feature licensing, no FIPS code `[CERT]`

**Traps** are native to `nSnmp`, not inherited from the classic module. `BSnmpNetwork` has `snmpReceiveTraps` (default
`false`), `trapConfig` (a `BSnmpUdpCommConfig` on the traps port), and `snmpAlarmTableCapacity` (default `500`, up to
`250000`) `[CERT]` `.../BSnmpNetwork.java:92`. `BSnmpDevice` has a `traps` slot of type `BSnmpAlarmDeviceExt` `[CERT]`
`.../BSnmpDevice.java:99`, and `BSnmpAlarmDeviceExt` declares the six generic SNMP traps as built-in `BTrapType`s
(`coldStart`/`warmStart`/`linkDown`/`linkUp`/`authenticationFailure`/`egpNeighborLoss`) plus `ignoreUnrecognizedTraps`
`[CERT]` `.../alarm/BSnmpAlarmDeviceExt.java:70`. Vendor `NOTIFICATION-TYPE`s are added as further `BTrapType`
(`trapName`/`trapOid`/`genericType`/`specificType`/`variablesArray`, `.../alarm/BTrapType.java`). So a trap-driven alarm
path exists in `nSnmp`; it is gated only by inbound UDP/162 reaching the station, not by module capability.

**Licensing** is a single feature, no v3 sub-feature: `BSnmpNetwork.getLicenseFeature()` returns
`Sys.getLicenseManager().getFeature("tridium", "snmp")` `[CERT]` `.../BSnmpNetwork.java:376-377`. A station licensed for
`snmp` (the live one runs `ns:SnmpNetwork`, so it is) gets v3 at no extra licence cost; there is no `snmpV3` feature to find.

**FIPS**: no `fips`/`FIPS` token appears anywhere in the `nSnmp` module `[CERT]` (grep of the module tree, empty). FIPS
enforcement, where it applies, is in the JCE crypto layer (Mocana DSF, [Block 126]), not the driver. Since SHA auth and
AES-128/192/256 privacy are FIPS-approved and DES is not, a `SHA + AES` device config runs under FIPS; only the DES-privacy
option would be blocked `[INFER]` (algorithm-set reasoning, not a FIPS code path in this module).

## 476.7 — Self-verify

| Marker | Count |
|---|---|
| `[CERT]` | 21 |
| `[INFER]` | 5 |
| `[CERT-doc]`/`[CERT-live]`/web | 0 |

Ratio `[INFER]`/total ≈ 0.19. Primary source is code; the two `docSource` reads (`BProxyExt`, `BLinearConversion`) are
original Tridium javadoc source, the highest fidelity tier.

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | Two SNMP modules ship; `nSnmp` desc "SNMP Driver with NDriver Framework", `preferredSymbol=ns` | `[CERT]` | `nSnmp/nSnmp-rt/extracted/META-INF/module.xml:2`; classic `snmp/…/module.xml` `preferredSymbol=snmp` |
| 2 | `ns:SnmpNetwork` = `com.tridium.nSnmp.BSnmpNetwork` | `[CERT]` | `nSnmp/nSnmp-rt/extracted/META-INF/module.xml:26` |
| 3 | Live station runs `ns:SnmpNetwork@26587` → uses `nSnmp` | `[CERT]`/`[INFER]` | `niagara-mental-model-bloque123.md:65`; namespace→module by `preferredSymbol` |
| 4 | Numeric ext reports raw value, no `scale` slot | `[CERT]` | `BSnmpNumericProxyExt.java:20-28`; `BSnmpProxyExt.java:27-49` |
| 5 | Type chain reaches `BProxyExt`, which owns `conversion` + `deviceFacets` | `[CERT]` | `BNProxyExt.java:9`; `docSource/…/BProxyExt.java:85,245` |
| 6 | ÷10 = `BLinearConversion.make(0.1,0)`, forward `device*scale+offset`, reverse for writes | `[CERT]` | `docSource/…/conv/BLinearConversion.java:34-47,89-112` |
| 7 | Read-only is enforced by `getMode()`←`isWritablePoint()` | `[CERT]` | `BSnmpProxyExt.java:91-93` |
| 8 | Enum resolves via `range` `BEnumRange` in `deviceFacets` | `[CERT]` | `BSnmpEnumProxyExt.java:24-33` |
| 9 | Learn auto-fills `range` from MIB; 2-ordinal enum → Boolean | `[CERT]` | `BSnmpPointManager.java:732,755,787,757,790-791,850,859-860` |
| 10 | v3 USM stack present on `BSnmpDevice` | `[CERT]` | `BSnmpDevice.java:99` |
| 11 | Auth = SHA only (no MD5); priv = DES/AES-128/192/256 | `[CERT]` | `BUsmAuthenticationProtocol.java:22-25`; `UsmPrivacyProtocol.java:17-24` |
| 12 | Native traps: `snmpReceiveTraps`/`trapConfig`/`snmpAlarmTableCapacity` + `traps`=`BSnmpAlarmDeviceExt` | `[CERT]` | `BSnmpNetwork.java:92`; `BSnmpDevice.java:99`; `BSnmpAlarmDeviceExt.java:70` |
| 13 | Licence = single `tridium/snmp` feature, no `snmpV3` | `[CERT]` | `BSnmpNetwork.java:376-377` |
| 14 | No FIPS code in `nSnmp`; SHA+AES FIPS-clean, DES not | `[CERT]`/`[INFER]` | empty grep; algorithm-set reasoning |
| 15 | Interop gate: v3 needs SHA auth + AES priv per device | `[INFER]` | from claim 11 (no MD5 offered) |

## 476.8 — Connections

- **Corrects** [Block 28] §28.6.5 (scopes "no v3" to classic `snmp`) and adds the trap/typed-ext story §28.6 never saw.
- **Refines** [Block 123]: the live `ns:SnmpNetwork` is now attributed to the `nSnmp` module specifically.
- **Uses** the proxy-ext framework of [Block 7] (Container/Network/Device/Point, ProxyExt pipeline) — `nSnmp` exts are
  ordinary `BProxyExt`s, so the [Block 7] conversion/facets/tuning machinery applies unchanged.
- **Touches** licensing ([Block 40] feature list has `snmp`; [Block 301] "per-feature limit ≠ host `globalCapacity`") and
  FIPS/crypto provider ([Block 126] Mocana DSF).
- Advances the `nSnmp` slice of `oem-honeywell-tail` U12 ("Tridium framework drivers not deep-distilled"), without closing
  U12 (which spans OPC-UA/Modbus/M-Bus/oBIX/… as well).

## 476.9 — Open gaps

- **B476-G1** — SNMPv3 end-to-end wire behaviour (USM key localisation, engineID discovery, timeliness window) is
  code-present but unverified against a live agent — deferred, requires transport (the integration's WARP path).
- **B476-G2** — the `nSnmp` **agent side** (`com.tridium.nSnmp.point.agent.*`, `mib/MibAlarmTable`, `BSnmpAgent`,
  `localDevice` on `BSnmpNetwork`) — Niagara *serving* SNMP and its own MIB/alarm table — is out of scope here; not distilled.
- **B476-G3** — whether this station actually runs in FIPS mode (a station config, not visible in [Block 123]) is unverified;
  it does not change the SHA+AES conclusion.
