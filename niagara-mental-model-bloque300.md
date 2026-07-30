# Block 300 — Diagnostics: the sign of the error code tells you where the fault is, the 20-value comm-status vocabulary, and the four network counters nobody can alarm on

> Focus **modbus**, gap **M6**. What the driver reports when a Modbus integration misbehaves, and how to
> read it: the internal error-code space (negative = local, positive = the slave said no), the 20-value
> `BCommStatusEnum` presented to the operator, the driver's own error codes, and what the official
> troubleshooting/debugging topics do and do not cover. READ-ONLY. Corpus language: ENGLISH.
>
> Sources (primary): `sources/decompiled/modbusCore-rt/` (Vineflower, jar `a0b67420…`) —
> `client/datatypes/BCommStatus`, `client/enums/BCommStatusEnum`, `ModbusErrorCodes`, `ModbusException`,
> `messages/ModbusMessageConst`.
> Official documentation: `sources/manuals/docModbus-N4.14-guide.md` §Troubleshooting,
> §Debugging messages, §Exception responses.
>
> Markers: `[CERT]` local primary source (`file:line`) · `[CERT-doc]` official Tridium guide (§topic) ·
> `[INFER]` deduction.
>
> Layer 26 (Communication protocols — driver focus). Connects [Block 294] (the four network counters and
> the spy page), [Block 295] (`setCommStatusOutput` on every poll), [Block 299] (exception code 5 treated
> as success), [Block 131] (the wire errors these codes describe).

---

## 300.1 — Two numbering systems, and the sign is the diagnosis `[CERT]`

`BCommStatus(int errorCode)` is a single `switch` that maps an internal code to the operator-visible enum
`[CERT]` `modbusCore-rt/…/client/datatypes/BCommStatus.java:54-117`. Read as a whole it reveals a design
decision the guide never states:

| Code | Enum value | Origin |
|---|---|---|
| **−8** | `disabled` | **local** — driver/station state |
| **−7** | `fault` | local |
| **−6** | `down` | local |
| **−5** | `lrcError` | local — ASCII checksum failed |
| **−4** / `default` | `otherError` | local |
| **−3** | `unknown` | local |
| **−2** | `okNotActive` | local — nothing polled yet |
| **−1** | `crcError` | local — RTU checksum failed |
| **0** | `ok` | — |
| **1** | `illegalFunction` | **the slave** (Modbus exception 01) |
| **2** | `illegalDataAddress` | the slave (02) |
| **3** | `illegalDataValue` | the slave (03) |
| **4** | `slaveDeviceFailure` | the slave (04) |
| **5** | `acknowledge` | the slave (05) |
| **6** | `slaveDeviceBusy` | the slave (06) |
| **7** | `negativeAcknowledge` | the slave (07) |
| **8** | `memoryParityError` | the slave (08) |
| **9** | `deviceTimeout` | **synthetic** — see below |
| **10** | `gatewayPathUnavailable` | the slave/gateway (0A) |
| **11** | `gatewayTargetDeviceFailedToRespond` | the slave/gateway (0B) |

`[INFER]` the operational rule that falls out: **a positive code means the slave answered and refused; a
negative code means no valid answer arrived.** That single distinction routes the whole investigation — a
positive code says the wiring, addressing and framing are working and the problem is configuration
(wrong address, unsupported function, out-of-range value); a negative code says the problem is below that
(cabling, baud/parity, framing mode, the device being down).

**Code 9 is not a Modbus exception.** The standard defines 01–08 plus 0A/0B; 09 is unassigned. Tridium
occupies it for `deviceTimeout`, and the driver assigns it wherever a response is missing — `[CERT]`
`BModbusClientDevice.java:610`, `:664` and `BModbusClientNumericProxyExt.java:357-358` all synthesise
`rsp.exceptionCode = 9` when `sendModbusMessage` returns null. `[INFER]` so a "9" in the UI is the driver
speaking, not the device.

## 300.2 — The negative constants are declared in a different file from the enum `[CERT]`

The negative codes are named in `ModbusMessageConst` `[CERT]`
`modbusCore-rt/…/messages/ModbusMessageConst.java:36,48-52`:

```
OK = 0 · CRC_ERROR = -1 · OK_NOT_ACTIVE = -2 · UNKNOWN = -3 · LRC_ERROR = -5
```

`[INFER]` note that −4, −6, −7 and −8 have **no named constant** even though `BCommStatus` handles all four;
they are written as bare literals at their call sites. That is why the mapping is only discoverable by
reading the `switch` — there is no single table in the source either.

Separately, the driver has its own error space for *internal* failures, `ModbusErrorCodes` `[CERT]`
`modbusCore-rt/…/ModbusErrorCodes.java:4-8`:

| Constant | Value | Meaning |
|---|---|---|
| `E_INVALID_FUNCTION_CODE` | 100 | a function code the driver itself refuses (thrown by the read methods of [Block 295] §295.4) |
| `E_COMMUNICATIONS_ERROR` | 101 | thrown when a poll gets no response |
| `REGISTER_NOT_POLLED_BY_DEVICE` | 102 | — |
| `DATA_NOT_AVAILABLE` | 103 | — |
| `MODBUS_TCP_COULD_NOT_CONNECT` | 104 | — |

These surface as `ModbusException`, not as comm status. `[INFER]` **102 is the interesting one for an
integrator**: `REGISTER_NOT_POLLED_BY_DEVICE` is the failure mode [Block 295] §295.2 predicted — a point
bound to a poll-config entry that no longer covers it, or that has been disabled.

## 300.3 — Where the status lands, and where it does not `[CERT]`

`setCommStatusOutput(entry, code)` is called on **every** poll iteration, on both the success and failure
paths `[CERT]` `BModbusClientDevice.java:594, 611, 620, 648, 665, 674` — so a `BDevicePollConfigEntry`
carries a live `readStatus` of type `BCommStatus`, default `BCommStatus(-2)` = `okNotActive`
`[CERT]` `…/client/datatypes/BDevicePollConfigEntry.java:54`.

The preset components carry the same type for the write direction — `writeStatus`, also defaulting to
`OK_NOT_ACTIVE` ([Block 299] §299.3).

What is **not** available this way are the four network-level counters — `totalCrcErrors`, `totalLrcErrors`,
`totalTransactionIdErrors`, `totalPartialRxMsgs` — which [Block 294] §294.7 established are private fields
rendered **only** on the spy page, not Niagara properties. `[INFER]` the practical gap: per-entry and
per-point status is bindable, historisable and alarmable; **network error rates are not**. An integrator
who wants to alarm on "this RS-485 trunk is degrading" has no property to bind to — only a spy page a human
must open. That is a real observability hole, and it is consistent with the guide's own advice, which sends
you to the spy viewer rather than to a point.

## 300.4 — What the official guide offers `[CERT-doc]`

§Debugging messages is a procedure, and a useful one: enable trace logging on the network, then
*"Right-click the station in Nav tree and click Spy … Click the stdout (standard output) hyperlink"* to see
the query/response cycle `[CERT-doc]`. It also documents a formatting difference worth knowing: the trace
*"breaks out the query to show fields on separate lines, and the received (response) in a single line (in
hex format)"*, and for `ModbusTCPNetwork` the driver *"sends a 6-byte leading TCP header `000000000006` in
each query, and omits the checksum byte in both sent and response messages"* `[CERT-doc]`.

`[INFER]` that hex string is the MBAP header of [Block 131] §131.3 with a zero transaction id, zero
protocol id and length 6 — i.e. the guide is showing the trace of a request whose transaction id has not
yet incremented, not a fixed header.

§Troubleshooting is two entries `[CERT-doc]`:

1. *"Read fault: illegal data address"* → check the point address against the vendor documentation. That is
   exception code **2** in §300.1's table.
2. A float/long point reading **zero or an impossibly large value while still reporting `{ok}`** → verify
   the byte-order settings on the parent device.

`[INFER]` the second entry is the guide's only acknowledgement of the byte-order problem, and it names the
exact symptom: **status stays `ok` because the transaction succeeded** — the bytes arrived intact, they were
just assembled in the wrong order. No status code can express that, which is why it needs a troubleshooting
entry. It is also the symptom that [Block 297] §297.5's missing BADC permutation would produce
*permanently*, with no setting able to fix it.

## 300.5 — What the guide does NOT resolve

- **the sign convention** (§300.1) — the single most useful diagnostic rule in the driver is nowhere stated;
- **that code 9 is synthetic** and not a Modbus exception;
- the **`ModbusErrorCodes` 100–104 space** (§300.2) entirely, including `REGISTER_NOT_POLLED_BY_DEVICE`;
- that the **network counters are spy-only** and cannot be alarmed on (§300.3);
- the `okNotActive` (−2) state — what an integrator sees on a freshly created entry before the first poll.

## 300.6 — Self-verify

`verify-block.sh` tally (COMPUTED — `adj` strips the header legend):

| Marker | raw | adj |
|---|---|---|
| `[CERT]` | 18 | 17 |
| `[CERT-doc]` | 7 | 6 |
| `[CERT-hw]` / `[CERT-live]` / `[CERT-web]` / `[CERT-a]` | 0 | 0 |
| `[INFER]` | 8 | 7 |
| **[INFER]/[CERT*] ratio** | | **7/23 = 0.30** |

Script exit 0. Ratio 0.30 — the highest in this focus so far, and expected: a diagnostics gap is mostly
*interpretation* of a small, fully-enumerated code space (one `switch`, two constant interfaces). The
evidence is not near exhaustion; there is simply less of it to cite per conclusion.

**Block type: EVIDENCE.**

Load-bearing claims:

| # | Claim | Marker | Verified how |
|---|---|---|---|
| 1 | The full −8..11 → enum mapping | `[CERT]` | `BCommStatus.java:54-117` read in full, every case enumerated |
| 2 | `BCommStatusEnum` has 20 values with the listed ordinals | `[CERT]` | `BCommStatusEnum.java:12,35-55` |
| 3 | Negative constants named in `ModbusMessageConst`; −4/−6/−7/−8 unnamed | `[CERT]` | `:36,48-52` + absence check over the same file |
| 4 | Code 9 is synthesised on a null response | `[CERT]` | three call sites: `BModbusClientDevice.java:610,664`, `BModbusClientNumericProxyExt.java:357-358` |
| 5 | `ModbusErrorCodes` = 100..104 with those names | `[CERT]` | `ModbusErrorCodes.java:4-8` |
| 6 | `setCommStatusOutput` called on both paths of both read methods | `[CERT]` | six call sites in `BModbusClientDevice.java` |
| 7 | `readStatus` defaults to `BCommStatus(-2)` | `[CERT]` | `BDevicePollConfigEntry.java:54` |
| 8 | Network counters are spy-only | `[CERT]` | re-stated from [Block 294] §294.7, re-checked: no `@NiagaraProperty` for them in `BModbusNetwork.java` |
| 9 | Guide's debugging procedure + the TCP header string | `[CERT-doc]` | verbatim in `docModbus-N4.14-guide.md` §Debugging messages |
| 10 | Guide's two troubleshooting entries | `[CERT-doc]` | verbatim, §Troubleshooting |

Tokens grep-confirmed in their cited source: **10 / 10**. Claim 3 contains an ABSENCE (four unnamed
constants) and was checked by grepping the whole constant interface for the values, not only the names.
No new sources preserved. Model tier: **no delegation — inline**.

## 300.x — Connections

- **[Block 294]** — §294.7's four counters; this block adds why their spy-only nature matters.
- **[Block 295]** — `setCommStatusOutput` is called from the read methods documented there; `REGISTER_NOT_POLLED_BY_DEVICE` is its predicted failure mode made concrete.
- **[Block 297]** — §300.4's second troubleshooting entry is the byte-order symptom; the missing BADC permutation makes it unfixable in one case.
- **[Block 299]** — exception 5 (`acknowledge`) is deliberately excluded from the write error test there.
- **[Block 131]** — the CRC/LRC failures behind codes −1 and −5, and the MBAP header the guide's trace shows.

**Gaps opened by this block**: none new. §300.3's observability hole is a finding, not an open question;
whether an integrator can work around it (e.g. a BQL query over entry `readStatus` values) is an
integration-design question, not a decompilation gap.
