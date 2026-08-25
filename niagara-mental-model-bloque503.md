# Block 503 — `framework-drivers` FD9: `knxnetIp` — a fully Tridium-authored KNXnet/IP stack (no vendored SDK), tunneling + routing transport (UDP 3671 / multicast 224.0.23.12), a data-driven single-class DPT codec, capacity-quota licensing, and NO KNX Secure (plaintext-only)

> **Focus:** `framework-drivers`, gap **FD9** — KNXnet/IP building-automation driver (BEYOND the original U12
> list; surfaced by the bootstrap audit). Measured **189** classes under `com.tridiumX.knxnetIp` in `-rt` (the
> audit's "325 vf" counted `-wb`; RE-MEASURED). READ-ONLY, decompiled; no run. Markers §3.
> **Sources:** FUENTE 3 — `organized/knxnetIp/knxnetIp-rt/decompiled/…`. FUENTE 1 — [B496] (the SDK-bundling
> contrast), [B335]/[B350] (the `com.tridiumx` add-on family). FUENTE 2 — not consulted (decompilation gap).
> Evidence delegated to a `sonnet` sweep (nested sub-sweeps); ALL load-bearing file:line RE-VERIFIED inline.

## §503.1 — Provenance: 100% Tridium-authored, NO vendored SDK `[CERT]`

All 189 `-rt` classes are `com.tridiumX.knxnetIp.*`; grep for `tuwien.auto.calimero` (the usual Java KNX stack)
and any other 3rd-party KNX SDK = **0** `[CERT negative]`. **The opposite of FD1 opcUaCore** ([B496]): Tridium
wrote the entire KNXnet/IP wire stack themselves — frame parsing, HPAI, CEMI encode/decode, tunneling session
lifecycle, multicast routing — no SDK to inherit CVEs from, and no thin-shim pattern. `[INFER]`: the namespace is
`com.tridiumX` (capital X), distinct from core `com.tridium.*` and from the `com.tridiumx` (lowercase) add-on
family of [B335] jsonToolkit / [B350] electronicSignature — likely a separately-packaged/OEM-tier module; the
exact packaging label is not asserted here (recorded as an open sub-point).

## §503.2 — Component tree `[CERT]`

| Class | Base | Role |
|---|---|---|
| `BKnxNetwork` | `BDeviceNetwork` (impl `BIService`) | singleton driver; owns local interfaces, poll scheduler, the DPT-definitions store, license gate (`driver/BKnxNetwork.java:74`) |
| `BKnxDevice` | `BDevice` | one KNXnet/IP interface or router: `ipAddress`, `controlPortNumber`, `individualDeviceAddress`, `connectionMethod` (`driver/BKnxDevice.java:76`) |
| `BKnxProxyExt` | `BProxyExt` (impl `BIKnxPollable`) | abstract point base: `groupAddresses`, `dataValueTypeId` (DPT-ID string), write-only flag (`point/BKnxProxyExt.java:87`) |
| `BKnx{Numeric,Boolean,Enum,String}ProxyExt` | `BKnxProxyExt` | concrete per-type proxies → `BStatusNumeric`/`BStatusBoolean`/… |
| `BGroupDataManager` | — | central dispatcher: routes inbound group telegrams to the matching proxy by group address |

**Group address → point:** `BKnxProxyExt.groupAddresses` (primary + optional secondaries); an inbound telegram is
dispatched by `BGroupDataManager` to `setValue(CemiMessage)` → `decodeFromBytes()` → `readOk(BStatusValue)`.

## §503.3 — Transport: tunneling + routing (client-only) `[CERT]`

`connectionMethod` = DEFAULT (tunneling, unicast UDP) / PROXY (tunnel via another device) / ROUTING (multicast).
Wire constants (`knxSpec/KnxSpec.java`): **port 3671** (`:71`), **multicast `224.0.23.12`** (`:72`). Frame types:
`CORE_CONNECT_REQUEST` 0x0205, `TUNNELLING_REQUEST` 0x0420, `ROUTING_INDICATION` 0x0530. Tunneling via
`BTunnelConnection`/`BConnections`/`BEndPoint` (UDP receive loop); routing via `BKnxInstallation` +
`KnxMulticastSocket` (extends `java.net.MulticastSocket`). `[CERT]` **Tunneling-client-only**: inbound server-side
`CONNECT_REQUEST` is explicitly "processing not implemented" (`comms/BConnections.java` log) — N4 connects out to
KNX/IP gateways, never accepts inbound tunnels.

## §503.4 — DPT codec: one data-driven class, not one-class-per-DPT `[CERT]`

Distinctive design: **no per-DPT class files and no XML DPT table**. The whole datapoint-type codec is a single
generic class `knxDataDefs/BDataValueTypeDef` — `bytesToBValue(CemiMessageData)` (`:603` decode) /
`bytesFromBValue(BValue,BStatus)` (`:968` encode) — dispatching on an encoding-format enum (`bBoolean`/
`fFloatingPoint`/`nEnumeration`/`uUnsignedInteger`/`vSignedInteger`/`aCharacter`/`tUnicodeString`). Low-level bit
work in `KnxCodecFuncs` (KNX EIS5 16-bit float = sign+4-bit-exp+11-bit-mantissa ÷100). Each DPT (e.g. `DPST-9-1`)
is a **`BDataValueTypeDef` component stored live in the station tree** under a hidden `BKnxStationDataDefs` slot on
`BKnxNetwork`, with an import/export admin dialog — data-driven rather than compiled. `[INFER]`: elegant and
extensible (new DPTs = new components, no code), but it means the DPT table is station-config, not source.

## §503.5 — Addressing & discovery `[CERT]`

`BKnxAddress` subclasses over a single `int`: individual `area.line.device` (4/4/8 bits), group address auto-styled
by `/`-count — free (16-bit) / twoLevel (5/11) / threeLevel (5/3/8); group address 0 reserved/rejected. Discovery:
`SEARCH_REQUEST` (0x0201) multicast with a 10 s window (`BDiscoverDevicesJob`); `CoreSearchResponse` yields HPAI +
device-info DIB (individual address, IP, MAC, friendly name) + supported service families; auto-selects
proxy-routing when the discovered device's subnet differs from the local interface.

## §503.6 — License: dynamic feature name + capacity quotas `[CERT]`

`getFeature("tridium", TYPE.getModule().getModuleName())` (`driver/BKnxNetwork.java:170`) — the feature name is
the **module name resolved dynamically** (not a hardcoded `"knxnetIp"` string). Enforcement is by **numerical
capacity quotas**, not a boolean: `checkFeature(...)` reads `KNX_INSTALLATION_LIMIT_KEY` (`"none"`=-1=unlimited,
else integer) and blocks past `knxInstallationsLimit` (`comms/BKnxInstallation.java:272-280`); a second quota caps
`localInterfaces`. Feature = **`tridium:knxnetIp`** with two count sub-limits — a stricter license shape than the
boolean gates of FD5/FD7.

## §503.7 — Security: NO KNX Secure, plaintext-only `[CERT negative]`

grep of `-rt` for `javax.crypto`/`Cipher`/`AES`/`SecretKey`/`sessionKey` = **0** (the only near-hits are a
multicast-MAC prefix `01:00:5E` and an Ethernet MAC length constant — not crypto). `[CERT]` The driver implements
**neither KNXnet/IP Secure (AES-128 CCM session auth) nor KNX Data Secure**; all group telegrams are sent/received
in clear, and there is no key/credential storage. `[INFER]`: expected for a pre-KNX-Secure-era driver (KNX Secure
standardized 2018); security depends entirely on the KNX/IP network being isolated. Same "no in-band security"
verdict as M-Bus [B500], for the same reason (the protocol generation predates crypto).

## §503.8 — Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|---|---|---|---|
| 1 | 189 com.tridiumX classes, fully Tridium-authored, no Calimero/SDK | `[CERT]`/`[CERT neg]` | find=189; grep calimero=0 | PASS |
| 2 | BKnxNetwork=BDeviceNetwork, BKnxDevice=BDevice, BKnxProxyExt=BProxyExt; group-addr routing | `[CERT]` | `BKnxNetwork.java:74`; `BKnxDevice.java:76`; `BKnxProxyExt.java:87` | PASS |
| 3 | tunneling+routing; port 3671, multicast 224.0.23.12; tunneling-client-only | `[CERT]` | `KnxSpec.java:71-72`; `BConnections` "not implemented" | PASS |
| 4 | DPT codec = single data-driven BDataValueTypeDef, defs stored in station tree, not XML/per-DPT | `[CERT]`+`[INFER]` | `BDataValueTypeDef.java:603,968` | PASS |
| 5 | address styles (individual 4/4/8, group free/2-level/3-level); SEARCH_REQUEST discovery | `[CERT]` | `BKnxAddress*`; `KnxSpec` SEARCH_REQUEST | PASS |
| 6 | license dynamic module-name feature + numerical capacity quotas (installations, interfaces) | `[CERT]` | `BKnxNetwork.java:170`; `BKnxInstallation.java:272-280` | PASS |
| 7 | NO KNX Secure (crypto grep=0); plaintext-only; no key storage | `[CERT negative]` | grep crypto=0 | PASS |

**Tally:** 7 claims — 5 `[CERT]`/`[CERT negative]` load-bearing + 3 `[INFER]` (packaging label, config-driven-DPT
consequence, isolation-dependence) on cited code. Block TYPE = **EVIDENCE**; ratio low, FD9 CLOSED. All
load-bearing tokens re-verified inline.

## §503.9 — Connections & focus status

- **SDK-bundling axis:** knxnetIp is the pole opposite [B496]/[B499] — a large module (189 cls) that is
  fully hand-written, not a thin shim over a vendored SDK. Sets up the FD10 `abstractMqttDriver` question
  (does it bundle an MQTT SDK, or is it hand-rolled like this?).
- **Security:** joins [B500] mbus as "no in-band security, plaintext-only, protocol predates crypto" — distinct
  from the OPC-UA/oBIX/OpenADR blocks which at least have TLS. Feed to [B398]/[B490].
- **License:** the capacity-quota shape (two numerical sub-limits) is the strictest license model in the focus so
  far (vs boolean FD5/FD7, foreign-limit FD4).
- **Namespace:** `com.tridiumX` — flagged for a possible packaging/OEM-family follow-up (recorded, not seeded).
- **Focus status:** `framework-drivers` 8/10 (FD1–FD7, FD9 closed). NEXT = FD10 `abstractMqttDriver` (then FD8).
