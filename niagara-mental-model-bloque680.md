# B680 — JACE-8000 native field-bus drivers: four `*PlatformServiceQnx` JNI shims — `libplatmstp` (BACnet MS/TP over RS-485, `sendFrame`/`setBaudRate`/`setMaxInfoFrames` via QNX `DCMD_MSTP_TX_FRAME`), `libplatnrio` (Niagara Remote IO: `discover`/`enablePolling`/`sendRequest`), `libplatccn` (a Carrier-CCN-style bus: `readTable`/`readVariable`/unsolicited msgs on `/dev/ccn%d`), and `libserial` (raw RS-232/485 port) — the native edge-I/O layer under the Java drivers (focus jace8000-qnx-native, QN4; §19 [CERT])

> **Focus:** `jace8000-qnx-native` (§16). **Gap closed:** QN4 (the native field-bus / protocol drivers of the
> JACE). **Phase:** static RE, READ-ONLY. **Marker:** `[CERT]` from ARM ELF symbols + strings.
> **Sources:** `sources/probes/B672-jace8000-sd/qn4-fieldbus-symbols.txt` · binaries
> `local-sd-image/bin-arm/{libplatmstp,libplatnrio,libplatccn,libserial}.so` (gitignored; sha256 in the probe) ·
> `[CERT]` corpus [Block 131]/[Block 133] (BACnet/MS-TP wire), [Block 294]-[Block 315] (drivers), [Block 34]
> §34 (maxInfoFrames).
> **Bottom line:** the JACE's field I/O is driven by four thin **QNX platform-service** native libraries, each a
> JNI shim (`com.tridium.plat*.B*PlatformServiceQnx`) that exposes the low-level bus operations to the Java
> driver above it and does the actual talking to a QNX device node. They cover **BACnet MS/TP** (RS-485),
> **Niagara Remote IO (NRIO)**, a **Carrier-CCN-style HVAC bus**, and a **generic serial port**. None link
> crypto or sockets (`NEEDED` = only `libc++.so.1` + `libc.so.4`) — they are pure device drivers.

---

## §680.1 — `libplatmstp.so` = BACnet MS/TP (RS-485) `[CERT]`

JNI class `com.tridium.platMstp.BBacnetMstpPlatformServiceQnx` (`BBacnetMstpPlatformServiceQnx.cpp`). Entry
points `[CERT]`: `openDriver0`/`closeDriver0`, `sendFrame0`/`rcvFrame0`, `setBaudRate0`, `getAddress0`/
`setAddress0`, `setMaxInfoFrames0`, `setMaxMaster0`. It talks to the MS/TP UART via a QNX devctl —
string `DCMD_MSTP_TX_FRAME returned %d (errno %d)` `[CERT]`. So the JACE implements **BACnet MS/TP** framing
in a QNX resource-manager driver, and the Java `bacnet` driver (wire layer [Block 133]) drives it through these
JNI calls. The `maxInfoFrames`/`maxMaster` MS/TP parameters ([Block 34], the corpus's maxInfoFrames thread)
are set through `setMaxInfoFrames0`/`setMaxMaster0` here.

## §680.2 — `libplatnrio.so` = Niagara Remote IO (NRIO) `[CERT]`

JNI class `com.tridium.platNrio.BNrioPlatformServiceQnx`. Entry points `[CERT]`: `open0`/`close0`,
`discover0`, `enablePolling0`/`disablePolling0`, `sendRequest0`, `setPortParams0`, `waitForStatusChange0`.
This is the native driver for **NRIO** — Tridium's Niagara Remote I/O modules (the JACE-8000's expansion I/O
bus). `discover0` enumerates modules on the bus; `enablePolling0`/`disablePolling0` run the acquisition loop;
`waitForStatusChange0` is the event/interrupt wait. The Java NRIO driver sits on top.

## §680.3 — `libplatccn.so` = a Carrier-CCN-style HVAC bus `[CERT] + [INFER]`

JNI class `com.tridium.platCcn.BCcnPlatformServiceQnx`, device node **`/dev/ccn%d`** `[CERT]`. Entry points
`[CERT]`: `openDriverComm0`/`closeDriver0`, `readTable0`, `readObjectData0`, `readVariable0`, `autoVariable0`,
`changeTableName0`, `changeExtendedParams0`, `getNextUnsolicitedMessage0`. The operation vocabulary — read
**table** / **variable** / **object data**, plus **unsolicited messages** — matches the **Carrier Comfort
Network (CCN)** HVAC protocol `[INFER — the "CCN" name + table/variable/unsolicited semantics are CCN's; the
binary only says "Ccn"]`. So the JACE ships a native driver for a Carrier-CCN-style controller bus. (This is a
new protocol surface for the corpus; the wire details are a follow-up, QN4-G1.)

## §680.4 — `libserial.so` = generic RS-232/485 serial port `[CERT]`

JNI class `com.tridium.platSerial.qnx.BSerialPortQnx`. Entry points `[CERT]`: `available0`, `close0`,
`flush0`, and the modem-control lines `isCD0`/`isCTS0`/`isDSR0`/`isDTR0`/`isRI0`; strings `error setting serial
baud rate` / `error setting serial parity`. This is the low-level serial abstraction (open/read/write/flush +
RS-232 control lines + baud/parity) that the serial-based drivers use. It is the native side of the JACE's
COM/RS-485 ports ([Block 663]-style port map is the JACE-9000; this is the JACE-8000's serial native).

## §680.5 — Shape of the native driver layer `[CERT]`

All four follow one pattern: a `com.tridium.plat<X>.B<X>PlatformServiceQnx` JNI class, source
`B<X>PlatformServiceQnx.cpp`, `NEEDED` only `libc++.so.1` + `libc.so.4` (no crypto, no sockets — they are
device drivers), talking to a QNX device node / devctl. The **protocol logic and state machines live in the
Java driver**; these natives are the thin OS-specific transport. REMITTANCE: the BACnet/MS-TP wire format is
[Block 131]/[Block 133]; the driver framework is [Block 294]-[Block 315]. This block establishes only the
native ARM/QNX driver inventory specific to the JACE-8000 edge.

## §680.6 — Self-verify

| # | Claim | Marker | Cite |
|---|---|---|---|
| 1 | libplatmstp = BACnet MS/TP (sendFrame/rcvFrame/setBaudRate/setMaxInfoFrames/setMaxMaster; DCMD_MSTP_TX_FRAME) | [CERT] | qn4-fieldbus-symbols.txt |
| 2 | libplatnrio = NRIO (discover/enablePolling/sendRequest/setPortParams/waitForStatusChange) | [CERT] | qn4 evidence |
| 3 | libplatccn = CCN bus (readTable/readVariable/readObjectData/unsolicited; /dev/ccn%d) | [CERT] | qn4 evidence |
| 4 | libplatccn = Carrier Comfort Network (protocol identity) | [INFER] | §680.3 (name+semantics) → QN4-G1 |
| 5 | libserial = raw RS-232/485 (CD/CTS/DSR/DTR/RI, baud/parity) | [CERT] | qn4 evidence |
| 6 | all four: *PlatformServiceQnx JNI shims, NEEDED only libc++/libc.so.4 (pure device drivers) | [CERT] | readelf -d |

**Tally:** 6 claims — 5 [CERT], 1 [INFER] (#4, CCN=Carrier → QN4-G1). 0 unmarked.

## §680.7 — Connections

- **[Block 131]/[Block 133]** — BACnet + MS/TP wire format; `libplatmstp` is the JACE's native transport for it.
- **[Block 294]-[Block 315]** — the Niagara driver framework (Java) that sits on these native shims.
- **[Block 34]** — maxInfoFrames/maxMaster (set via `libplatmstp`'s JNI here).
- **[Block 678]** — `libnre` `NativePlatformProvider`; these are the per-bus sibling platform services.
- **[Block 677]** — `libdsfspi` (crypto) — note these drivers do NOT link it (no encryption on the field bus).
