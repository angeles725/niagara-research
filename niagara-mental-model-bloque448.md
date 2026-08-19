# Block 448 — The nrio wire protocol and the actrld daemon (gap B445-G3): 115200-baud RS-485 master/slave framing, UID-keyed logical addressing, and the native poller controlled over Thrift

**Focus:** base corpus (field-I/O drivers axis). Closes **B445-G3**. Continues [B445]; corrects [B445] §445.5 open item (baud now resolved).

**Origin:** child gap from [B445] — the actual RS-485 baud/framing on the IO bus, how the 1–16 address really gets assigned, and what/where the `actrld` daemon is.

**Scope:** (1) network serial defaults, (2) the `NrioMessage` frame layout + message catalogue, (3) module-type enum, (4) the discover→UID→SetLogicalAddress addressing mechanism, (5) `actrld` as a native daemon driven by a Thrift RPC. NOT: byte-exact timing, retry backoff tuning math.

**Sources:**
- **FUENTE 1 (corpus):** [B445] §445.2/§445.4 (address 1–16 read-only; actrld polls; station has no direct RS-485 path). [B446] (`MSG_RD_SCALE_OFFSET`).
- **FUENTE 2 (niagara-help):** `[CERT-doc]` `aActrldToNrio` (actrld = low-level access-control daemon on the host, polls processors, memory-compares IoStatus), `aEssentialNrioNetworkProperties`.
- **FUENTE 3 (code):** `[CERT]` `nrio-rt/.../BNrioNetwork.java`, `messages/NrioMessage.java`, `messages/NrioMessageConst.java`, `messages/SetLogicalAddressMessage.java`, `platNrio-rt/.../BNrioPlatformServiceAtlas*.class`.

---

## 448.1 — Serial defaults (resolves the B445 open item)

`BNrioNetwork` default properties: `[CERT]` (`BNrioNetwork.java:182-196`)

| Property | Default | Note |
|---|---|---|
| `baudRate` | **`BSerialBaudRate.baud115200`** | writable; RS-485 IO bus runs at 115200 by default |
| `portName` | `"COM2"` | JACE-8000 uses COM1/COM2 ([B445] §445.3) |
| `trunk` | `1` | binds the actrld instance |
| `monitor` | ping every **30 s** | `makePingMonitor(BRelTime.makeSeconds(30))` |
| `maxFailsUntilDown` | `3` | consecutive poll fails → device "down" |
| `minPushTime` | `300 ms` | throttle for pushing changes to points |
| `outputFailsafeConfig` | `(8, 180)` | comm-loss / startup timeouts ([B445] §445.4) |
| `pushToPoints`, `unsolicitedMsg*` | true / counters | supports module-initiated (unsolicited) IoStatus |

> **Corrects [B445] §445.5**, which left baud "not resolved" (the `M2mIoNetwork` decompile showed a mangled `BSerialBaudRate.inin`). `BNrioNetwork` shows the real constant: **`baud115200`**. `[CERT]`

## 448.2 — Frame layout and message catalogue

`NrioMessage.getByteArray()` serializes a compact fixed header + payload: `[CERT]` (`NrioMessage.java:78-89`)

```
[ address (1) ][ length = 2 + data.length (1) ][ type (1) ][ status (1) ][ data … ]
```

`HEADER_SIZE = 2`, `SOH = 2`; the `length` field covers `type + status + data`. The app-layer serializer writes **no trailing checksum** — integrity is left to the RS-485 transport / SOH framing (contrast the firmware path, which relies on Intel HEX checksums, [B447]). `status == 0` means OK. `[CERT]` (`NrioMessageConst:6-7`, `NrioMessage.java:47`)

**Address space & module types** (`NrioMessageConst`): `MAX_MODULE_ADDRESS = 16`, `BROADCAST_ADDR = 188` (0xBC). Device-type codes: `[CERT]`

| Code | Type | | Code | Type |
|---|---|---|---|---|
| 6 | BASE_READER | | 10 | **REMOTE_IO_V1** (= `Io16V1`, the IO-R-16) |
| 7 | REMOTE_READER | | 11 | REMOTE_IO_34_PRI (IO-R-34 primary) |
| 8 | REMOTE_IO | | 12 | REMOTE_IO_34_SEC (IO-R-34 secondary) |
| 9 | REMOTE_GP_IO | | | |

Type 10 = the `Io16V1` string the mounting guide shows at discovery ([B445] §445.4); the IO-R-34 appearing as two device rows ([B445] §445.2) is codes 11+12 — the two on-board controllers. `[CERT]`

**Message set** (master/slave request-response, `MSG_*` in `NrioMessageConst`): `[CERT]`

| # | Message | # | Message |
|---|---|---|---|
| 1 | QUERY_UNCONFIG | 10 | WR_CODE_DNLD_START |
| 2 | SET_LOGICAL_ADDRESS | 11 | WR_CODE_DNLD_STOP |
| 3 | PING | 12 | WR_CODE_DNLD_DATA |
| 4 | RESET_CR | 14 | RD_INFO_MEMORY |
| 5 | RD_BUILD_INFO | 15 | CLEAR_INFO_MEMORY |
| 6 | WR_CR_CONFIG | 18 | RD_SCALE_OFFSET ([B446]) |
| 7 | RD_CR_CONFIG | 20 | WR_IO_DEFAULT_START |
| 8 | IO_STATUS | 9 | WR_DO_DATA |

## 448.3 — Addressing: discover → UID → SetLogicalAddress

The 1–16 address is **assigned by the driver, keyed on the module's factory UID** — this is the mechanism behind [B445]'s "auto-derived at Discover, read-only": `[CERT]` (`SetLogicalAddressMessage.java`)

1. Discover **broadcasts** `QUERY_UNCONFIG` (msg 1) to address 188.
2. Each unconfigured module replies with its **6-byte UID** (`UnconfiguredModuleReply`).
3. The driver sends `SetLogicalAddressMessage` — built with `address = 188` (broadcast), a 6-byte `uid`, and the chosen `setAddress` (1–16) — so **only the module whose UID matches** adopts that logical address.

Because the address is a driver-assigned function of the immutable UID, the operator cannot edit it and there is no hardware DIP address ([B445] §445.2). An IO-R-34 consumes two slots because its PRI and SEC controllers each take a logical address. `[CERT]`

## 448.4 — actrld is a native daemon, driven over Thrift

`aActrldToNrio` says the actrld ("access control daemon") runs on the JACE host, separate from the station, polling processors and memory-comparing each new `IoStatus` against the last. `[CERT-doc]` The code shows **where it lives and how the station controls it**: the **`platNrio`** module exposes a **native platform service** `BNrioPlatformServiceAtlas` — an **Apache Thrift** service with `enableActrld` / `disableActrld` RPCs (sync + async processors). `[CERT]` (`platNrio-rt/.../BNrioPlatformServiceAtlas$*enableActrld*.class`, Thrift imports)

- **"Atlas"** = the JACE-8000 platform; so `actrld` is a **native JACE-8000 daemon**, and `platNrio` is the station-side Thrift client that enables/disables it per trunk.
- This closes the loop with [B445] §445.4 and the doc: the **station never speaks RS-485 directly** — it configures actrld (via Thrift), actrld polls the bus at 115200 baud, and only *changed* IoStatus is pushed up to the proxy points (throttled by `minPushTime`, gated by `pushToPoints`). The `Trunk` property is the binding between an `NrioNetwork` and its actrld instance. `[CERT]`

---

## 448.5 — Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | NrioNetwork default baud = `BSerialBaudRate.baud115200` (writable); portName COM2, trunk 1 | `[CERT]` | `BNrioNetwork.java:87,183-185` |
| 2 | Monitor ping 30 s; maxFailsUntilDown 3; minPushTime 300 ms; unsolicited IoStatus supported | `[CERT]` | `BNrioNetwork.java:182,190-191,194-196` |
| 3 | Frame = [address][length=2+data][type][status][data]; SOH=2, HEADER_SIZE=2; no app-layer checksum byte | `[CERT]` | `NrioMessage.java:78-89`, `NrioMessageConst:6-7` |
| 4 | MAX_MODULE_ADDRESS=16, BROADCAST_ADDR=188; type codes incl. REMOTE_IO_V1=10, IO_34 PRI=11/SEC=12 | `[CERT]` | `NrioMessageConst:5,10-15` |
| 5 | ~18 message types (query-unconfig, set-logical-address, ping, io-status, wr-do-data, code-dnld, rd-scale-offset…) | `[CERT]` | `NrioMessageConst:18-33` |
| 6 | Addressing: broadcast QUERY_UNCONFIG → module replies UID(6B) → SetLogicalAddress(uid, addr 1-16) to 188 | `[CERT]` | `SetLogicalAddressMessage.java:6-41` |
| 7 | Address is driver-assigned from immutable UID ⇒ read-only, no DIP; IO-R-34 = 2 slots (PRI+SEC) | `[CERT]` | claim 4+6, [B445] §445.2 |
| 8 | actrld = native daemon (platform "Atlas"=JACE-8000), enabled/disabled by platNrio via Apache Thrift RPC | `[CERT]` | `BNrioPlatformServiceAtlas$*enableActrld*`, thrift imports |
| 9 | Station never speaks RS-485 directly: actrld polls, memory-compares IoStatus, pushes only changes to points | `[CERT-doc]`+`[CERT]` | `aActrldToNrio`; `pushToPoints`/`minPushTime` |

**Tally:** 9 claims — 8 `[CERT]` · 1 `[CERT-doc]`+`[CERT]` · 0 `[INFER]` · 0 unmarked. Corrects one prior open item ([B445] §445.5 baud). No unresolved contradictions.

**Left out (named):** exact retry/backoff timing; the `status` byte's error-code table; whether the app frame's missing checksum means the serial layer (`SerialHelper`) adds parity/CRC; the full Thrift IDL of `BNrioPlatformServiceAtlas` beyond enable/disable.

## 448.6 — Connections
- **Closes B445-G3**; **corrects [B445] §445.5** (baud = 115200). Supplies the protocol layer referenced by [B446] (RD_SCALE_OFFSET) and [B447] (WriteDownLoad msgs 10/11/12).
- **actrld native** joins the platform-native thread ([B124]–[B130], [B379]–[B385]): a JACE-8000 ("Atlas") native daemon controlled by a Niagara module over Thrift — same platform-service pattern as other `plat*` modules.
- **Security note (transport):** the app frame carries no authentication and no app-layer checksum, and firmware downloads ride the same bus ([B447]) — consistent with the "unauthenticated field bus" posture of the corpus's other RS-485 drivers.

## 448.7 — Open gaps
- **B448-G1** — status semantics + retry/health machine. **CLOSED → [B449]** (`status==0`=OK; device down after 3 failed pings; download retries N times). Per-code error table is module-side (out of reach).
- **B448-G2** — platform service + trunk↔serial mapping. **CLOSED → [B449]** (platNrio spawns `/proc/boot/actrld -n <numTrunks>`; JNI `open0/discover0/enablePolling0`; JACE-8000 COM2 = `/dev/ser2` @115200; stats via `/dev/actrl<trunk>`).

> **Refined by [B449]:** [B448] §448.4 framed actrld control as a Thrift RPC; it is really a two-layer path — a platform-service RPC over a native impl that spawns `/proc/boot/actrld` and drives `/dev/ser2` via JNI.
