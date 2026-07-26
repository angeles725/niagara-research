# Block 285 — EMSTP: Tridium's undocumented host↔co-processor protocol for MS/TP — 17 commands, a 4-value prefix, a 10-state bring-up sequence, and the SAM4S firmware behind the JNI boundary

> Closes **B279-G1**. [B279] established that MS/TP framing lives in native code behind
> `sendFrame0`/`rcvFrame0`, and identified the `Emstp*` classes as *"a host/co-processor command protocol —
> a false lead for framing, but a real undocumented Tridium protocol"*. This block documents that protocol.
>
> **EMSTP is the only fully-decompilable layer of Niagara's MS/TP stack.** It does not carry MS/TP frames —
> it *commands the device that does*. Documenting it is the closest a static reader can get to the wire
> without a JACE image.
>
> This is also the first second-order gap where **step 2 paid off**: the official EngNote
> `bacnetUtil-Tokens.txt` supplies the hardware identity, the queue depth and the firmware version string —
> none of which are in the code.
>
> **Sources**: Vineflower decompile of `com.tridium.platMstp` (no `docSource`; `com.tridium.*`) +
> `niagara-help/guides-clean/EngNotes/bacnetUtil-Tokens.txt`. Markers: `[CERT]` verbatim ·
> `[CERT-doc]` official Tridium doc · `[INFER]` derived. **Corpus language: ENGLISH.**

---

## 285.1 — The command byte: 2-bit prefix + 6-bit opcode `[CERT]`

```java
public enum EmstpCommandPrefixEnum {
   REQ(0), ACK(64), NAK(128), USOL(192);
   private static final int PREFIX_MASK = 192;      // 0xC0
}
```

```java
private static final int COMMAND_MASK = 63;         // 0x3F
int command = commandByte & 63;
```

So every EMSTP command byte decomposes as:

```
 7 6 5 4 3 2 1 0
┌───┬───────────┐
│ P │  opcode   │      P = REQ 00 · ACK 01 · NAK 10 · USOL 11
└───┴───────────┘      opcode = 0..63
```

**Four message dispositions**, and the fourth is the interesting one: **`USOL` = unsolicited** — the
co-processor can speak without being asked. That is what makes received MS/TP frames and keep-alive
expiries possible as pushes rather than polls. `[INFER]` on the expansion of the abbreviation; the constant
is `[CERT]`.

This resolves the two masks B279 §279.4 could only report as raw numbers
(`COMMAND_PREFIX_MASK = 192`, `COMMAND_MASK = 63`).

---

## 285.2 — The 17 commands `[CERT]`

```java
public enum EmstpCommandEnum {
   MSTP_NO_MESSAGE(0),          MSTP_COMMAND_UNKNOWN(255),
   MSTP_START(16),              MSTP_STOP(17),
   MSTP_GET_STATS(18),          MSTP_RESET_STATS(19),
   MSTP_KEEP_ALIVE_EXPIRED(20), MSTP_DEVICE_DOWN(21),
   MSTP_SET_ADDR(32),           MSTP_GET_ADDR(33),
   MSTP_SET_MAX_MASTER(34),     MSTP_SET_USAGE(35),
   MSTP_SET_INFO_FRAMES(36),    MSTP_SET_TX_THROTTLE(37),
   MSTP_APP_TX(48),             MSTP_APP_RX(50),
   MSTP_OK_TO_SEND(52),         MSTP_KEEP_ALIVE(53);
}
```

The opcodes cluster in fours by high nibble — a deliberate grouping `[INFER]`:

| Range | Group | Commands |
|---|---|---|
| `0x00` | idle | `NO_MESSAGE` |
| `0x10–0x15` | **session control** | `START`, `STOP`, `GET_STATS`, `RESET_STATS`, `KEEP_ALIVE_EXPIRED`, `DEVICE_DOWN` |
| `0x20–0x25` | **configuration** | `SET_ADDR`, `GET_ADDR`, `SET_MAX_MASTER`, `SET_USAGE`, `SET_INFO_FRAMES`, `SET_TX_THROTTLE` |
| `0x30–0x35` | **data plane** | `APP_TX`, `APP_RX`, `OK_TO_SEND`, `KEEP_ALIVE` |
| `0xFF` | sentinel | `MSTP_COMMAND_UNKNOWN` — returned by the lookup on no match |

### The configuration group maps 1:1 onto B279's JNI surface

| B279 native method | EMSTP command |
|---|---|
| `setAddress0` | `MSTP_SET_ADDR` (32) |
| `getAddress0` | `MSTP_GET_ADDR` (33) |
| `setMaxMaster0` | `MSTP_SET_MAX_MASTER` (34) |
| *(usageTimeout slot)* | `MSTP_SET_USAGE` (35) |
| `setMaxInfoFrames0` | `MSTP_SET_INFO_FRAMES` (36) |
| *(txThrottle slot)* | `MSTP_SET_TX_THROTTLE` (37) |
| `sendFrame0` | `MSTP_APP_TX` (48) |
| `rcvFrame0` | `MSTP_APP_RX` (50) |

**This is the missing link of B279.** B279 §279.5 listed the configuration surface visible from Java and
noted every tunable is settable but no framing is implemented there. EMSTP is *how* those settings cross:
each JNI setter becomes an EMSTP command byte to the co-processor. The `txThrottle` and `usageTimeout`
slots B279 found on `BBacnetMstpLinkLayer` have commands of their own, confirming they are pushed down
rather than applied host-side. `[INFER]` on the mapping; both sides are `[CERT]`.

Note `MSTP_APP_TX(48)` and `MSTP_APP_RX(50)` skip 49 and 51 — B279 §279.4 found
`APP_TX_MESSAGES_WAITING` / `APP_TX_DATA_EXPECTING_REPLY` flags, so the odd values are plausibly reserved
for variants. Not verified — gap **B285-G1**.

---

## 285.3 — The bring-up sequence is a state machine `[CERT]`

```java
public enum EmstpStateEnum {
   IDLE,
   ENTER_COPROCESSOR_MODE,
   SET_ADDR,
   SET_MAX_MASTER,
   SET_MAX_INFO,
   SET_USAGE_TIMEOUT,
   START_TOKEN_PASSING,
   CHECK_TRANSMIT,
   CHECK_RECEIVE,
   SET_TX_THROTTLE;
}
```

Ten states, and the first seven read as a **strict initialisation order** `[INFER]`:

```
IDLE
  └→ ENTER_COPROCESSOR_MODE      ← the legacy/coprocessor switch of B279 §279.4
       └→ SET_ADDR                (MAC address)
            └→ SET_MAX_MASTER
                 └→ SET_MAX_INFO
                      └→ SET_USAGE_TIMEOUT
                           └→ START_TOKEN_PASSING   ← only now does the node join the ring
                                └→ CHECK_TRANSMIT ⇄ CHECK_RECEIVE   (steady state)
```

Two observations:

1. **Token passing starts last.** Address, max-master, max-info-frames and usage timeout are all pushed
   *before* `START_TOKEN_PASSING` — the node is fully configured before it participates in the ring. That
   ordering matters on a live MS/TP segment, where a node joining with a wrong `Max_Master` disrupts the
   token. `[INFER]`
2. **`SET_TX_THROTTLE` sits outside the sequence**, declared last after the two steady-state checks. It is
   plausibly applied on demand rather than at bring-up. `[INFER]` — gap **B285-G2**.
3. **`ENTER_COPROCESSOR_MODE` is an explicit state**, which corroborates B279 §279.4's reading that legacy
   and co-processor are two distinct operating modes and the host must actively switch. `[CERT]` on the
   state's existence.

`EmstpStateMachine` is 777 lines; only its state vocabulary is documented here. The transition logic and
timeout handling were not traced — gap **B285-G3**.

---

## 285.4 — What the EngNote adds that the code cannot `[CERT-doc]`

`bacnetUtil-Tokens.txt`, the official Tridium EngNote:

**The co-processor is an Atmel SAM4S:**

> *"SAM4S coprocessor mode: incremented every millisecond when there is no tx or rx character on the serial
> bus, reset on tx or rx of byte"* (the `silencetimer` description)
> *"The hardware index of the USART used on the coprocessor (sam4s)"*

**Its firmware carries a version string:**

> *"A version string embedded in the coprocessor firmware, such as **`"MS/TP Coprocessor 2.249, Jan 12 2022"`**"*

`EmstpStats` reads it back `[CERT]`:
```java
this.version = new String(versionBytes, StandardCharsets.US_ASCII).trim();
```

**The protocol byte is `0x01`, confirmed from the other side:**

> *"In coprocessor mode, if the first byte of a message from the host does not start with **0x01**, this
> count is incremented"*

That matches `PROTOCOL_EMSTP = 1` from B279 §279.4 — code and documentation agree, and the EngNote adds
that a mismatch is *counted* as an error statistic rather than silently dropped.

**The receive queue is five deep:**

> *"The number of messages received from the coprocessor and awaiting processing by the link layer. This
> queue is **5 messages** deep."*

**Malformed input is one counter:**

> *"An unknown protocol byte, command, or inconsistent emstp message length was received from the
> coprocessor."*

— three distinct failure modes collapsed into a single statistic, so a diagnostic session cannot
distinguish them from the counter alone. `[INFER]`

**And the two modes differ in silence measurement**: `silencetimer` has separate legacy and co-processor
descriptions, and *"tUsageStart is greater than tUsageEnd, N/A for coprocessor mode"* — a legacy-only
metric. `[CERT-doc]`

---

## 285.5 — What EMSTP is, stated plainly

Putting B279 and this block together `[INFER]`:

```
Niagara Java (BBacnetMstpLinkLayer, platform service)
   │  JNI: sendFrame0 / rcvFrame0 / setMaxMaster0 / …
   ▼
native mstpnpsdk / platmstp
   │  EMSTP: [0x01][prefix|opcode][2-byte BE length][payload]
   ▼
SAM4S co-processor firmware ("MS/TP Coprocessor 2.249")
   │  ← the MS/TP framing lives HERE: preamble 0x55 0xFF, frame types,
   │    header CRC-8, data CRC-16 / CRC-32 (extended), COBS, token passing
   ▼
RS-485 bus
```

So Niagara's MS/TP is a **three-tier** design: a Java link layer that configures and queues, a native
shim that speaks EMSTP, and a dedicated microcontroller that owns the wire. B279's negative finding was
correct and this block explains *why* the framing is absent from Java — it is absent from the host
entirely.

---

## 285.6 — Self-verify

| Claim | Evidence | Marker |
|---|---|---|
| Command byte = 2-bit prefix + 6-bit opcode | `PREFIX_MASK = 192`, `COMMAND_MASK = 63`, `commandByte & 63` | `[CERT]` |
| Four prefixes REQ/ACK/NAK/USOL = 0/64/128/192 | enum quoted in full | `[CERT]` |
| USOL = unsolicited (co-processor can push) | name expansion | `[INFER]` |
| 17 commands with the listed opcodes | enum quoted in full | `[CERT]` |
| Opcodes cluster by high nibble into 4 groups | derived from the values | `[INFER]` |
| Config commands map 1:1 to B279's JNI setters | both lists side by side | `[INFER]` |
| `MSTP_COMMAND_UNKNOWN(255)` is the lookup fallback | `return MSTP_COMMAND_UNKNOWN;` after the loop | `[CERT]` |
| Ten states, listed | enum quoted in full | `[CERT]` |
| First seven are a bring-up order ending in START_TOKEN_PASSING | derived from the ordering | `[INFER]` |
| `ENTER_COPROCESSOR_MODE` is an explicit state | present in the enum | `[CERT]` |
| Co-processor is a SAM4S | EngNote, two quotes | `[CERT-doc]` |
| Firmware version string readable | EngNote quote + `EmstpStats` US_ASCII decode | `[CERT-doc]` + `[CERT]` |
| Protocol byte 0x01 confirmed from the doc side | EngNote quote | `[CERT-doc]` |
| Receive queue is 5 deep | EngNote quote | `[CERT-doc]` |
| Three failure modes share one counter | EngNote quote | `[CERT-doc]` / `[INFER]` on the consequence |
| Silence measurement differs by mode | two `silencetimer` entries + the tUsage note | `[CERT-doc]` |
| Three-tier architecture | composed from B279 + this block | `[INFER]` |

Tally: **[CERT] 8 / [CERT-doc] 6 / [INFER] 7.**

---

## 285.x — Connections and gaps

- **B279** — **G1 closed here.** §285.2 supplies the link B279 §279.5 was missing: how the Java-visible
  tunables actually reach the hardware. §285.5 explains *why* §279.3's negative finding was inevitable.
- **B279 §279.4** — the raw constants (`0xC0`, `0x3F`, `PROTOCOL_EMSTP = 1`) are now decoded.
- **B127** — the native-boundary catalogue; `mstpnpsdk` / `platmstp` still owe it an entry (B279-G3).
- **B126** — `libciper.so` as the QNX-ARM serial JNI library; the same JACE-side hardware territory.

| ID | Gap | Class |
|---|---|---|
| **B285-G1** (new) | Opcodes 49 and 51, skipped between `APP_TX(48)` and `APP_RX(50)`/`OK_TO_SEND(52)` — plausibly reserved variants. | STATIC-investigable |
| **B285-G2** (new) | Why `SET_TX_THROTTLE` sits outside the bring-up sequence. | STATIC-investigable |
| **B285-G3** (new) | `EmstpStateMachine`'s 777 lines: transition logic, retries, timeout handling. Only the state vocabulary is documented. | STATIC-investigable |
| **B285-G4** (new) | `EmstpStats` (253 ln) — the full statistic set and how it maps onto the EngNote's counter table. | STATIC-investigable |
| **P3-mstp** | unchanged — still requires-native-RE for the actual framing (now known to live in SAM4S firmware, not even in the host native libs). | requires-native-RE |
| **Next** | **B280-G1** — BACnet/SC header-option TLV encoding. | STATIC-investigable |
