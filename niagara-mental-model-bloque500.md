# Block 500 — `framework-drivers` FD5: `mbus` — the M-Bus (EN 13757) meter-reading driver over `basicDriver` (serial `MbusSerialComm` 8E1/300-baud + TCP `MbusSocketComm` gateway), the SND_NKE/REQ_UD2→RSP_UD telegram cycle with a Java-array DIF/VIF decoder, and its plaintext-only posture (zero EN 13757-3 encryption)

> **Focus:** `framework-drivers`, gap **FD5** — the M-Bus utility-meter driver (~118 cls). EN 13757 = European
> meter-bus standard (heat/water/gas/electricity). READ-ONLY, decompiled; no run. Markers §3.
> **Sources:** FUENTE 3 — `organized/mbus/mbus-rt/decompiled/…` (this artifact ships `decompiled/`). FUENTE 1 —
> [B499]/[B498] (driver-security pattern); `basicDriver` base (the audit's spotted reference module). FUENTE 2 —
> not consulted (decompilation gap). Evidence delegated to a `sonnet` sweep; ALL load-bearing file:line
> RE-VERIFIED inline (offsets discarded). **Scope note:** `mbus` rides the SHARED `basicDriver-rt` base
> (`BSerialNetwork`/`BBasicDevice`/`BBasicProxyExt`) — the serial-driver analogue of the shared libs behind
> opcUaCore [B496] and obix-rt [B499].

## §500.1 — Component tree over `basicDriver` `[CERT]`

```
BAbstractMbusNetwork  extends BSerialNetwork (basicDriver)   [abstract]
  ├─ BMbusNetwork        serial variant   — getLicenseConnectTypeName()="serial"
  └─ BMbusTcpIpNetwork   TCP-gateway variant — ="tcpip"
BMbusDevice          extends BBasicDevice   — one per meter
BMbusPointDeviceExt  extends BPointDeviceExt — point-set manager (REQ_UD2 cycle sizing)
BMbusProxyExt        extends BBasicProxyExt  — one Niagara point ↔ one data record
```
(`BAbstractMbusNetwork.java:119`, `BMbusDevice.java:93`.) `BMbusDevice` holds `primaryAddress` (0-250),
`secondaryAddress` (16-hex), `addressMode` (primary/secondary/enhanced), `identNumber`, `manufacturer`.
`BMbusProxyExt` binds a point to a record via `recordNumber` + `mbusDifCode`/`mbusVifCode`/`function`/`exponent`/
`mbusUnit`. **Record→point mapping:** `recordNumber` = 0-based index of the decoded record in the RSP_UD response.

## §500.2 — Transport: serial + TCP over one abstract base `[CERT]`

- **Serial `MbusSerialComm`** (extends `basicDriver` `SerialComm`). Default line config
  (`BAbstractMbusNetwork.java:341-342`): `setParity(even)`, `setBaudRate(baud300)` = **8E1 / 300 baud**, the
  EN 13757-2 physical default. Supported: 300–38400. `searchBaudRate` default also `baud300` (`:122`).
- **TCP `MbusSocketComm`** — plain Java `Socket` to an M-Bus-over-TCP gateway; default `192.168.1.10:6021`
  (`BMbusTcpIpNetwork.java:49`), `keepAlive=true`, reconnect on `SocketException`.
- Both link layers feed the same `MbusCommReceiver`/`MbusCommTransmitter` via `IMbusComm`.
- **`icmpPing` is a stub** (`MbusSocketComm.java:209-210` `return true`): the `pingPreConnect=true` gate
  (`:107`) therefore always "succeeds" `[CERT]` — pre-connect reachability is never actually tested.

## §500.3 — Telegram cycle + DIF/VIF decode `[CERT]`

Message classes (`MbusMessage` base; long frame = start `0x68`, short = `0x10`, ACK `0xE5`):
- **SND_NKE** C-Field `0x40` — bus init / FCB reset (`MbusSndNkeMessage`).
- **REQ_UD2** C-Field `0x5B` — request data; **toggles the device FCB each request** (alternating-bit
  reliability) (`MbusReqUd2Message`).
- **RSP_UD** long frame received when `input[0]==0x68` (`MbusCommReceiver`).

**Decoder** `MbusDecodeVariableFrame`: `decodeFixedDataHeader()` handles CI variants `0x7A`/`0x72`/`0x76` (the
12-byte header yields identNumber BCD, secondary address, 3-char packed manufacturer, version, deviceType),
then `decodeVariableDataBlocks()` walks DIF/DIFE/VIF/VIFE + data bytes per record. **The VIF→(quantity,unit,
exponent) table is a Java array** (`MbusVifConvertor` + extended/enhanced/error VIFE convertors), **NOT an XML
resource** `[CERT]` — grep of `mbus-rt` for VIF/DIF XML = none (contrast with bacnet's XML enum tables). Value
bytes decoded by DIF data-type in `MbusDataDecoderIEC870` (BCD/uintN/real32/date/string).
**Addressing:** primary A-Field 0-250 (broadcast 0xFF, "selected" 0xFD); secondary = 8-byte ID selected via
`MbusSndUdMessage`, then REQ_UD2 to A=0xFD.

## §500.4 — Reading model & discovery `[CERT]`

Poll scheduler fast/normal/slow = 30/45/90 s; per-device `pollFrequency`; `getPollGroupCode()` groups all
points on a device into one pass. Per-device flow: (optional SND_NKE + `initialisationDelay` 3 s) → (secondary
select if needed) → REQ_UD2 → parse RSP_UD → each `BMbusProxyExt` matches by `recordNumber` → `readOk(value)`;
extra REQ_UD2 cycles if `allowMultipleRecords`. Timings: responseTimeout 3 s, interMessageDelay 300 ms, retry 2.
**Discovery jobs:** primary scan 1-250 (`BMbusPrimaryDeviceSearchJob`), secondary wildcard scan narrowing the
8-byte space with mark `0x0F` (`BMbusSecondarySearchJob`), and live-point discovery reading one RSP_UD to
enumerate records as candidate points (`BMbusLivePointSearchDiscoveryJob`).

## §500.5 — License gate `[CERT]`

`checkFeature("tridium", "mbus")` (`BAbstractMbusNetwork.java:419`) with a **per-transport sub-key** enforced in
`getLicenseFeature()`: it throws `FeatureNotLicensedException` unless the sub-key matching
`getLicenseConnectTypeName()` is enabled — `"serial"` (`BMbusNetwork.java:28`) or `"tcpip"`
(`BMbusTcpIpNetwork.java:297`). Feature = **`tridium:mbus`**; serial and TCP transports are separately gated.

## §500.6 — Security: plaintext-only, no EN 13757-3 encryption `[CERT negative]`

- **No encryption anywhere.** grep of the whole `mbus-rt` for `javax.crypto`/`Cipher`/`SecretKey`/`AES`/
  `encrypt` = **0 hits**. The driver is **plaintext-only** on both serial and TCP; it implements **no
  EN 13757-3 secured-telegram support** (AES-128 mode 5/7). `[INFER]`: consistent with wired M-Bus's classic
  unauthenticated model — meter data (and the TCP-gateway link) cross the wire in clear; confidentiality/
  integrity depend entirely on physical/network isolation.
- **No credential/key storage** — no `KeyStore`/`SecretKey`/password slot. `[CERT]`
- **"password" is a data descriptor, not a credential:** `BMbusDescription.password` (`MbusExtendedVifConvertor`)
  is the standard EN 13757-3 extended-VIF code for a meter's access-code data field, not a stored secret. `[CERT]`
- **`icmpPing` stub** (§500.2) — a no-op reachability gate; benign but misleading if relied on operationally.

## §500.7 — Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|---|---|---|---|
| 1 | rides basicDriver: BAbstractMbusNetwork=BSerialNetwork, BMbusDevice=BBasicDevice, proxy=BBasicProxyExt | `[CERT]` | `BAbstractMbusNetwork.java:119`; `BMbusDevice.java:93` | PASS |
| 2 | serial default 8E1/300 baud; TCP default 192.168.1.10:6021; icmpPing stub=true | `[CERT]` | `BAbstractMbusNetwork.java:341-342`; `BMbusTcpIpNetwork.java:49`; `MbusSocketComm.java:209-210` | PASS |
| 3 | SND_NKE 0x40 / REQ_UD2 0x5B (FCB toggle) / long frame 0x68; RSP_UD decode | `[CERT]` | `MbusReqUd2Message`, `MbusSndNkeMessage`, `MbusCommReceiver` | PASS |
| 4 | DIF/VIF decoder = Java arrays (MbusVifConvertor), NOT XML | `[CERT]`/`[CERT neg]` | `MbusDecodeVariableFrame`; no VIF XML in module | PASS |
| 5 | poll fast/normal/slow 30/45/90s; primary + secondary-wildcard + live-point discovery | `[CERT]` | scheduler + `BMbus*SearchJob` classes | PASS |
| 6 | license `tridium:mbus` + serial/tcpip sub-keys | `[CERT]` | `BAbstractMbusNetwork.java:419`; `:28`/`:297` | PASS |
| 7 | NO encryption (crypto grep=0); plaintext-only; "password" VIF = data descriptor | `[CERT negative]` | grep `javax.crypto\|Cipher\|AES`=0; `MbusExtendedVifConvertor` | PASS |

**Tally:** 7 claims — 6 `[CERT]`/`[CERT negative]` load-bearing + 1 `[INFER]` (isolation-dependence) on cited
code. Block TYPE = **EVIDENCE**; ratio low, FD5 CLOSED. All load-bearing tokens re-verified inline.

## §500.8 — Connections & focus status

- **First block riding `basicDriver`** — the shared serial-driver base (`BSerialNetwork`/`BBasicDevice`/
  `BBasicProxyExt`). A dedicated `basicDriver` reference block remains a POSSIBLE future gap (recorded, not
  seeded; the audit spotted it at 24 cls).
- Security contrast across the focus: OPC UA client default STRONG ([B497]); oBIX default plaintext-Basic-over-http
  ([B499]); **M-Bus has no security layer at all by design** — the weakest of the three, but that is the protocol,
  not a Tridium defect. Feed to [B398]/[B490]: an M-Bus-over-TCP gateway on a routable network exposes meter
  data (and any writes) with zero transport protection.
- License model matches the focus pattern (`getFeature/checkFeature("tridium", <driver>)`), here with
  per-transport sub-keys (serial/tcpip) like [B499]'s foreign-limit sub-keys.
- **Focus status:** `framework-drivers` 5/10 (FD1–FD5 closed). NEXT = FD6 `openAdr`.
