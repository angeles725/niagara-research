# Block 279 — P3-mstp: MS/TP data-link framing is NOT in the Java corpus — the exact JNI boundary, the EMSTP host protocol that is not MS/TP, and the reclassification of the gap

> **TYPE: NEGATIVE FINDING with reclassification.** [B133] opened **P3-mstp** as *STATIC-investigable*, on
> the assumption that the MS/TP data-link framing (preamble `0x55 0xFF`, frame types, header-CRC/data-CRC,
> token passing) lived in the `mstp` classes of `bacnet-rt`. **It does not.** This block establishes where
> the Java stops, what is on the other side, and reclassifies the gap.
>
> The shape is identical to **P5-phys** in [B135] (LON TP/FT-10 line coding below `driver.write(ldvHandle,…)`)
> and to the native driver boundaries catalogued in [B127]. MS/TP is the third instance of the same pattern.
>
> **Sources**: Vineflower decompile of `bacnet-rt` and `platMstp-rt` via module-navigator; corpus-wide greps
> over 50 798 files; **and `niagara-help` guides-clean** (added in revision — see §279.9). No original
> Tridium source exists for these classes — `docSource` covers only `javax.baja.*`, and every MS/TP class
> is `com.tridium.*` `[CERT]`.
>
> ⚠️ **REVISED 2026-07-26.** The first version of this block ran only module-navigator and the `organized/`
> corpus, skipping two steps of the project's research protocol: it did not query **niagara-help**, and it
> did not check whether existing project blocks already covered MS/TP. Both omissions mattered. §279.9
> records what they turned up and **revises the verdict of §279.7** from "cannot be documented" to
> "cannot be documented *from the Java*, but Tridium's official EngNote documents much of the framing
> indirectly." The negative finding about the Java corpus (§279.3) stands unchanged and was independently
> confirmed by an existing block.
>
> Markers: `[CERT]` verbatim / verified absence · `[INFER]` derived. **Corpus language: ENGLISH.**

---

## 279.1 — Where MS/TP lives in the corpus `[CERT]`

`module-navigator search "*Mstp*"` returns 36 hits. Filtering to Tridium's own BACnet stack:

| Class | Module | Lines |
|---|---|---|
| `BBacnetMstpLinkLayer` | **bacnet-rt** `com.tridium.bacnet.stack.link.mstp` | 522 |
| `BBacnetMstpBaudRate` | bacnet-rt `com.tridium.bacnet.enums` | 63 |
| `BBacnetMstpUsageTimeout` | bacnet-rt `com.tridium.bacnet.enums` | 63 |
| `BBacnetMstpPlatformService` | **platMstp-rt** | 610 |
| `BBacnetMstpPlatformServiceEmstp` | platMstp-rt | 487 |
| `BBacnetMstpPlatformServiceNpsdk` | platMstp-rt | 282 |
| `BBacnetMstpPlatformServiceQnx` | platMstp-rt | 397 |
| `BBacnetMstpLinkParameters` | platMstp-rt | 112 |
| `MstpFrame` / `MstpListener` | platMstp-rt | 42 / 5 |
| `EmstpFrame` / `EmstpStateMachine` / `EmstpStats` | platMstp-rt | 176 / 777 / 253 |
| `EmstpCommandEnum` / `EmstpCommandPrefixEnum` / `EmstpStateEnum` | platMstp-rt | 45 / 31 / 14 |

The first structural fact `[CERT]`: **`com.tridium.bacnet.stack.link.mstp` contains exactly ONE class.**
Compare with the BACnet/IP link layer, whose framing (BVLC/BVLL) B133 §133.10 documented from
`bacnet-rt` directly. The MS/TP link package is nearly empty because the work happens elsewhere — and, as
§279.3 shows, not in Java at all.

The three `BBacnetMstpPlatformService*` variants (`Emstp`, `Npsdk`, `Qnx`) are per-platform backends, which
by itself signals a native dependency. `[INFER]`

---

## 279.2 — The JNI boundary, exactly `[CERT]`

`BBacnetMstpPlatformServiceNpsdk` declares thirteen native methods:

```java
private native boolean init0();
private native void    cleanup0();
private native int     openDriver0(String var1, int var2, int var3, int var4, int var5);
private native void    closeDriver0(int var1);
private native void    rcvFrame0(int var1, MstpFrame var2);
private native void    sendFrame0(int var1, byte var2, byte[] var3, boolean var4);
private native void    setBaudRate0(int var1, int var2);
private native void    setMaxMaster0(int var1, int var2);
private native void    setMaxInfoFrames0(int var1, int var2);
private native void    setAddress0(int var1, int var2);
private native int     getAddress0(int var1);
private native void    setParameter0(int var1, String var2, String var3);
```

with the library loads `[CERT]`:

- `log.fine("Loading mstpnpsdk native library")` — `BBacnetMstpPlatformServiceNpsdk`
- `log.fine("BacnetMstp: Loading platmstp native library")` — `BBacnetMstpPlatformServiceQnx`

**The transmit signature is the whole story:**

```java
sendFrame0(int handle, byte destinationAddress, byte[] data, boolean dataExpectingReply)
```

Java hands the native side **a destination MAC, a payload, and one boolean**. Everything the gap asked for
lives past that call:

| P3-mstp asked for | Where it actually is |
|---|---|
| preamble `0x55 0xFF` | native |
| frame-type byte | native — derived from `dataExpectingReply` and the token state machine |
| header CRC-8 | native |
| data CRC-16 | native |
| token passing (Poll For Master, Reply To Poll For Master, token pass, `Nretry_token`) | native |
| `Nmax_master` / `Nmax_info_frames` | **configured** from Java (`setMaxMaster0`, `setMaxInfoFrames0`) but **enforced** natively |

And `MstpFrame`, the object crossing the boundary, is a bare DTO with no framing whatsoever `[CERT]`:

```java
public class MstpFrame {
   public static final int MAX_MSTP_NPDU_FRAME_SIZE = 501;
   public static final int MAX_MSTP_NPDU_EXT_FRAME_SIZE = 1497;
   byte addr;  byte[] data;  boolean dataExpectingReply;
   …plain getters/setters…
}
```

The two size constants are the only wire-level facts it carries: **501** bytes for a standard MS/TP NPDU
frame and **1497** for the extended-frame variant.

---

## 279.3 — The decisive negative: `0x55` does not exist in the Java corpus `[CERT]`

A corpus-wide grep for the MS/TP preamble across **50 798 decompiled files** returns **two** matches, and
neither is MS/TP:

```
[cloudIotHubDep-rt] EncodingCodes:110   return "SMALLLONG:0x55";
[docSource-doc]     BColor:395          darkOliveGreen = constant("darkOliveGreen", 0x556b2f);
```

An AMQP encoding code and a colour constant.

Likewise, `-i crc` over `platMstp-rt` returns **zero** matches, and `BBacnetMstpLinkLayer` contains no
`crc`, `checksum`, `preamble`, or frame-type constant. `[CERT]`

There is no CRC-8 table, no CRC-16 table, no `0xFF` preamble pairing, and no frame-type enumeration
anywhere in the Java for MS/TP. The absence is total and consistent across three independent probes.

**This is the finding.** P3-mstp cannot be closed by decompiling more Java, because the bytes it asks about
are never constructed in Java.

---

## 279.4 — EMSTP is not MS/TP `[CERT]`

The `Emstp*` classes look like the obvious place to keep digging, and they are a false lead worth
documenting so nobody repeats it.

`EmstpFrame`'s constants:

```java
private static final int PROTOCOL_EMSTP = 1;
private static final int COMMAND_PREFIX_MASK = 192;      // 0xC0
private static final int COMMAND_MASK = 63;              // 0x3F
private static final int STATUS_BYTE_MASK = 127;         // 0x7F
private static final int BUFFER_AVAIL_BIT_MASK = 128;    // 0x80
private static final int BUFFER_AVAIL = 128;
private static final int APP_TX_MESSAGES_WAITING = 1;
private static final int APP_TX_NO_MESSAGES_WAITING = 0;
private static final int APP_TX_DATA_EXPECTING_REPLY = 1;
private static final int APP_TX_DATA_NOT_EXPECTING_REPLY = 0;
```

and its serialisation writes a 2-byte big-endian length `[CERT]`:

```java
outputStream.write(this.payloadLength >> 8 & 0xFF);
outputStream.write(this.payloadLength & 0xFF);
```

This is a **command/status protocol between the host and an MS/TP co-processor or driver** — a command
byte split into a 2-bit prefix and a 6-bit opcode, a status byte with a buffer-available flag, and
messages-waiting / data-expecting-reply application flags. `EmstpStateMachine` (777 lines) is the state
machine **of that host protocol**, not of MS/TP token passing. `[INFER]` on the characterisation; the
constants are `[CERT]`.

No preamble, no CRC, no frame types, no `Nmax_master` logic. **EMSTP rides above the MS/TP framing, it does
not implement it.**

---

## 279.5 — What Java *does* control `[CERT]`

The gap is not a total loss: the MS/TP **configuration surface** is fully visible, and it is what an
integrator actually touches.

`BBacnetMstpBaudRate` — the six supported rates:

```java
BAUD_9600 = 9600;  BAUD_19200 = 19200;  BAUD_38400 = 38400;
BAUD_57600 = 57600;  BAUD_76800 = 76800;  BAUD_115200 = 115200;
```

Note **76800** is present (the MS/TP-standard rate that plain serial hardware often lacks) and **there is no
rate below 9600** — the 4800/2400 bauds some legacy devices use are not offered. `[CERT]`

`BBacnetMstpUsageTimeout` — three values, in milliseconds:

```java
MS_20 = 20;  MS_35 = 35;  MS_85 = 85;
```

These are `Tusage_timeout` from the MS/TP specification. `[INFER]` on the naming correspondence; the
constants are `[CERT]`.

Plus, through JNI: `setMaxMaster0`, `setMaxInfoFrames0`, `setAddress0` / `getAddress0` (the MAC address),
`setBaudRate0`, and a generic `setParameter0(handle, name, value)` string escape hatch.

So: **every MS/TP tunable is settable from Niagara; none of the framing is implemented there.**

---

## 279.6 — Why the native library is not in this install either `[CERT]`

Searching the whole 4.14 Supervisor install for an MS/TP shared object returns **no `.so` or `.dll`** — only
Spyder firmware images, Merlin/Spyder PDFs, and two `.bajadoc` files.

That is expected and consistent: this corpus is a **Windows x64 Supervisor**, and MS/TP is a JACE-side
capability. The `Qnx` platform-service variant names the target directly (QNX = the JACE-8000 OS,
cross-referenced in B126 where `libciper.so` was identified as the QNX-ARM Sylk/Spyder serial JNI library).

So the native side is not merely undecompiled — **it is not present on this machine at all**.

---

## 279.7 — Reclassification of P3-mstp

| | Before (B133) | After (this block) |
|---|---|---|
| Class | STATIC-investigable | **requires-native-RE + hardware-access** |
| Rationale | assumed the `mstp` classes held the framing | framing is entirely below `sendFrame0`/`rcvFrame0`; `0x55` absent from 50 798 Java files; no MS/TP native binary in this install |
| What would close it | more decompiling | (a) obtain `mstpnpsdk` / `platmstp` from a JACE image and Ghidra it — the B124-B130 native-RE method; or (b) a live RS-485 capture with a protocol analyser |

This mirrors **P5-phys** exactly (B135): *"the TP/FT-10 physical/link layer lives in the native `ldv`
adapter driver below `driver.write(ldvHandle,…)`, NOT in `lonworks-rt.jar`"*. Two field-bus protocols, same
architecture, same conclusion. `[CERT]` on the parallel — both quoted from their own blocks.

**Statement of outcome — REVISED, see §279.9.** The preamble bytes, CRC polynomials and byte order still
cannot be obtained from this corpus. But **frame-type names, the CRC-16 / CRC-32 split, COBS encoding, both
state machines and the turnaround timings ARE documented** in Tridium's official EngNote, which the first
version of this block failed to consult. The CRC byte-order caution from B131 still cannot be applied —
there is no CRC *code* anywhere, only error counters.

---

## 279.9 — REVISION: what the two skipped protocol steps turned up `[CERT-doc]`

The project's research protocol is: **(1)** check the project's own `.md` blocks first, **(2)** decide with
the user whether to use `niagara-help` and/or `module-navigator`, **(3)** consult the `organized/` corpus.
The first version of this block did (3) and part of (2) — module-navigator only — and skipped (1) entirely.
Both skipped steps produced material findings.

### (1) An existing block had already reached the same conclusion

**B27 §(layer 21 table)**, verbatim `[CERT]`:

```
| 21 | Serial (no IP) | — | — | MSTP / RS-485 drivers | platMstp-rt, platSerial-rt, platNrio-rt | — | native DLL | local | none |
```

B27 had already classified `platMstp-rt` as **native DLL**. The §279.2-279.3 investigation independently
re-derived a conclusion the corpus already held. That is a *confirmation*, not a waste — but checking first
would have scoped the work correctly from the start.

**B7 §(BACnet transports)** had also already documented the link-layer slots.

### (2) `niagara-help` holds an official EngNote on MS/TP diagnostics

`guides-clean/EngNotes/bacnetUtil-Tokens.txt` (640 lines) documents the `bacnetUtil` **Tokens** component —
*"a BNumericPoint that can be tracked, alarmed, and charted like any other NumericPoint"* — exposing MS/TP
link-layer metrics. Because the metrics are named after what they count, **the document reveals framing
detail the Java never touches** `[CERT-doc]`:

**Frame types** (from the counter descriptions):

```
FT_TOKEN
FT_BACNET_DATA_EXPECTING_REPLY        FT_BACNET_DATA_NOT_EXPECTING_REPLY
FT_BACNET_EXT_DATA_EXPECTING_REPLY    FT_BACNET_EXT_DATA_NOT_EXPECTING_REPLY
```

**Three separate CRCs, not two:**

| Counter | Covers |
|---|---|
| `badheadercrc` | *"The number of bad header CRCs"* |
| `baddatacrc16` | *"bad data CRC16s (FT_BACNET_DATA_EXPECTING_REPLY and FT_BACNET_DATA_NOT_EXPECTING_REPLY)"* |
| **`baddatacrc32`** | *"bad data CRC32s (FT_BACNET_EXT_DATA_EXPECTING_REPLY and FT_BACNET_EXT_DATA_NOT_EXPECTING_REPLY)"* |

**Extended frames use CRC-32, standard frames CRC-16.** P3-mstp as scoped by B133 asked only about
"header-CRC/data-CRC" — there are three, and the third is tied to the extended-frame variant whose 1497-byte
limit §279.2 found in `MstpFrame`.

**Two state machines, both snapshot-able:**

```
master_state   — "MNSM snapshot":  0=init  1=idle  2=use token  3=wait for reply
                                   4=done with token  5=pass token  6=no token
                                   7=poll for master  8=answer data request
receive_state  — "RFSM snapshot":  0=idle  1=preamble  2=header  3=data
                                   4=skip data  5=cobs data  6=cobs crc
```

MNSM = Master Node State Machine, RFSM = Receive Frame State Machine — the ASHRAE 135 Clause 9 names.
**States 5 and 6 of the receiver are COBS** (Consistent Overhead Byte Stuffing), the extended-frame encoding
added by the MS/TP addendum. `[INFER]` on the expansion of the acronyms; the state lists are `[CERT-doc]`.

**Token retry:**

> *"MS/TP is allowed to resend the FT_TOKEN up to a maximum of 2 total if the first times out after tUsage
> timeout."* — `retrytokencnt`, values 0 or 1.

**Turnaround timing, baud-dependent** (`n40bitdelay`, coprocessor mode):

> *"tTurnaround, baud rate dependant: 6ms@9600, 4ms@19200, 3ms@38400, 2ms@57600 and above"*

**Two hardware generations**, and this is what EMSTP actually is:

| Mode | Hardware |
|---|---|
| **Legacy Mode** | Tridium485–2_r06 and earlier |
| **Coprocessor Mode** | Tridium485–2_r09 and later |

§279.4 characterised EMSTP as "a host/co-processor command protocol" from its constants alone. The EngNote
**confirms it by name**: coprocessor mode is a distinct hardware generation with its own silence-timer
handling (*"silencetimer (coprocessor mode)"* vs *"silencetimer (legacy mode)"*, and *"tUsageStart is
greater than tUsageEnd, N/A for coprocessor mode"*). `[CERT-doc]`

### A triple contradiction on `maxInfoFrames`

Three sources, three different values `[CERT]`:

| Source | Says |
|---|---|
| **B7** (this corpus) | *"Max Info Frames (default 1, max 50)"* |
| **EngNote** (Tridium official) | *"ranges from 1-127, defaults to 50"* |
| **Code** — `BBacnetMstpLinkLayer` | `defaultValue = "20"`, `facets = BFacets.makeInt(1,100)` |

**The code wins**: default **20**, range **1–100**. Both B7 and Tridium's own EngNote are wrong, in
different directions. B7 is corrected in §279.10.

For completeness, the verified slot defaults `[CERT]`:

```java
portName    = "COM1"                          mstpTrunk  = 0
baudRate    = baud_9600                       mstpAddress = 0        (facets 0..127)
maxMaster   = 127     (facets 0..127)         maxInfoFrames = 20     (facets 1..100)
supportExtendedFrames = false                 usageTimeout = ms_20
txThrottle  = 10      (facets 0..20)
```

`maxMaster = 127` agrees with the EngNote. `supportExtendedFrames = false` means **CRC-32/COBS frames are
off by default**.

---

## 279.10 — Corrections

| Target | Was | Is |
|---|---|---|
| **B7** (BACnet/MSTP slots) | *"Max Info Frames (default 1, max 50)"* | **default 20, range 1–100** — `BBacnetMstpLinkLayer` `[CERT]`. §279.9 |
| **§279.7 of this block** | "META 2 cannot be completed from this corpus" | The *Java* cannot supply it; Tridium's EngNote supplies frame types, the CRC-16/CRC-32 split, COBS, both state machines and turnaround timings as `[CERT-doc]`. §279.9 |
| **Tridium EngNote** `bacnetUtil-Tokens.txt` | *"maxinfoframes … ranges from 1-127, defaults to 50"* | Contradicted by the shipped code (20 / 1–100). Recorded, not fixable. |

---

## 279.8 — Self-verify

| Claim | Evidence | Marker |
|---|---|---|
| `com.tridium.bacnet.stack.link.mstp` has one class | `package` query → 1 class, `BBacnetMstpLinkLayer` | `[CERT]` |
| 13 native methods on the Npsdk service | full declaration list quoted | `[CERT]` |
| `sendFrame0` carries only (addr, data, DER flag) | signature verbatim | `[CERT]` |
| Native libs named `mstpnpsdk` and `platmstp` | the two `log.fine` load messages | `[CERT]` |
| `MstpFrame` has no framing | full class quoted (42 lines) | `[CERT]` |
| Frame size limits 501 / 1497 | `MAX_MSTP_NPDU_FRAME_SIZE` / `…EXT_FRAME_SIZE` | `[CERT]` |
| `0x55` absent from the Java corpus | grep over 50 798 files → 2 unrelated hits | `[CERT]` (negative) |
| No CRC in platMstp-rt or the link layer | two independent greps, zero matches | `[CERT]` (negative) |
| EMSTP is a host/co-processor command protocol | its constant set + 2-byte BE length | `[CERT]` constants / `[INFER]` characterisation |
| Six baud rates, none below 9600 | full enum quoted | `[CERT]` |
| Three usage timeouts 20/35/85 ms | full enum quoted | `[CERT]` |
| No MS/TP native binary in this install | `fd -e so -e dll` → none | `[CERT]` (negative) |
| Same pattern as P5-phys (LON) | B135's own wording quoted | `[CERT]` |
| B27 already classified platMstp as native DLL | B27 layer-21 table row, quoted | `[CERT]` |
| Frame type names FT_TOKEN / FT_BACNET_[EXT_]DATA_[NOT_]EXPECTING_REPLY | EngNote counter descriptions | `[CERT-doc]` |
| Three CRCs: header, data CRC-16, data CRC-32 (extended) | `badheadercrc` / `baddatacrc16` / `baddatacrc32` descriptions | `[CERT-doc]` |
| MNSM 9 states, RFSM 7 states incl. COBS | both snapshot value lists quoted | `[CERT-doc]` |
| Token retry max 2 total | `retrytokencnt` description, quoted | `[CERT-doc]` |
| tTurnaround 6/4/3/2 ms by baud | `n40bitdelay` description, quoted | `[CERT-doc]` |
| Legacy vs Coprocessor = Tridium485-2 r06 vs r09 | EngNote column headers | `[CERT-doc]` |
| `maxInfoFrames` really 20, range 1–100 | `defaultValue = "20"`, `BFacets.makeInt(1,100)` | `[CERT]` |
| MNSM/RFSM acronym expansion | matched against ASHRAE 135 Clause 9 naming | `[INFER]` |

Tally: **[CERT] 14 / [CERT-doc] 6 / [INFER] 3.**

---

## 279.x — Connections and gap status

- **B133** — opened P3-mstp; **reclassified here**, not closed. B133's APDU work is unaffected: the APDU is
  link-independent, which is precisely why it survives this finding intact.
- **B135 / P5-phys** — the LON precedent. Identical architecture and identical conclusion.
- **B127** — the native driver boundary catalogue (`lon.dll`, `opc.dll`, `pcapBacEther.dll`).
  `mstpnpsdk` / `platmstp` belong on that list and are **not currently on it**.
- **B126** — `libciper.so` as the QNX-ARM serial JNI library; the `Qnx` platform-service variant points at
  the same target platform.
- **B131** — the Modbus RTU CRC byte-order finding whose caution could not be applied here.
- **B136 / P6-jace** — the same "this is a Supervisor, that capability lives on a JACE" boundary.

### Gap status after this block

| ID | Gap | Class |
|---|---|---|
| **P3-mstp** | MS/TP framing | **RECLASSIFIED** → requires-native-RE (Ghidra on `mstpnpsdk`/`platmstp` from a JACE image) **or** requires-execution (live RS-485 capture) |
| **B279-G1** (new) | `EmstpStateMachine` (777 ln) + `EmstpCommandEnum` — the **host↔co-processor** protocol. Not MS/TP, but a real undocumented Tridium protocol, and fully decompilable. | STATIC-investigable |
| **B279-G2** (new) | `BBacnetMstpLinkLayer` (522 ln) read only for framing evidence; its actual role (queueing, retry, link-layer state exposed to the network layer) not traced. | STATIC-investigable |
| **B279-G3** (new) | `mstpnpsdk` / `platmstp` should be added to B127's native-boundary catalogue. | bookkeeping |
| **P3-sc** | BACnet/SC transport — **META 3, next**. Unlike MS/TP, `stack.link.sc.*` classes were observed in `bacnet-rt` by B133, so this one is expected to be genuinely static-investigable. | STATIC-investigable |
