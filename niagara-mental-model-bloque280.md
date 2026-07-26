# Block 280 — P3-sc: BACnet/SC transport — the BVLC-SC message codec, the 13 function codes, the 48-bit VMAC, and two unverifiable numbers in B23

> Closes **P3-sc**, open since [B133]. Unlike **P3-mstp** ([B279]), which turned out to be native, BACnet
> Secure Connect **is fully implemented in Java** — 40 classes across two packages in `bacnet-rt`, all
> decompilable. The gap was correctly classified by B133.
>
> Following the method correction recorded in B279 §279.9, this block ran **all three** protocol steps
> before concluding: project blocks first, then `niagara-help`, then module-navigator / `organized/`.
>
> **Sources**: Vineflower decompile of `bacnet-rt` (no original Tridium source — every class is
> `com.tridium.*`, and `docSource` covers only `javax.baja.*` `[CERT]`); `niagara-help` guides-clean;
> existing blocks B23/B27/B7. Markers: `[CERT]` verbatim · `[CERT-doc]` official Tridium documentation ·
> `[INFER]` derived. **Corpus language: ENGLISH.**

---

## 280.1 — Step 1: what the project already had `[CERT]`

**B23 §23.24** already carried a BACnet/SC section: a layer diagram, the certificate requirements
(`EKU=serverAuth+clientAuth`, self-signed root CA), the Workbench enrolment flow (CSR → sign → import →
configure `BScLinkLayer`), and the three class names `ScBvlcMessage`, `ScNpdu`, `BScLinkLayer`. It closes
with: *"Status en N4.14: arquitectura preparada; uso operacional limitado."*

**All three class names check out** `[CERT]`:

| Named in B23 | Actual location |
|---|---|
| `ScBvlcMessage` | `com.tridium.bacnet.stack.link.sc.message` — `public abstract`, 541 ln |
| `ScNpdu` | `com.tridium.bacnet.stack.link.sc.message` — `public final`, 114 ln |
| `BScLinkLayer` | `com.tridium.bacnet.stack.link.sc` — `public final`, 514 ln |

Two numbers in B23 do **not** check out — see §280.7.

**niagara-help** (step 2) holds two official guides — `Bacnet/MessageHandlingBACnetSC-4FC09B07.txt` and
`Bacnet/SettingUpAHub-A10110E5.txt` — plus `docs-text/docBacnet.txt` §7289. Notable from the hub guide
`[CERT-doc]`: setting up a hub requires *"a machine user required by BACnet Secure Connect. This user's sole
purpose is to associate an incoming request with…"* — i.e. Niagara maps an inbound SC connection onto a
**Niagara user account**, which is a station-security fact no code-only reading would surface.

---

## 280.2 — The implementation is entirely in Java `[CERT]`

Two packages, 40 classes:

**`com.tridium.bacnet.stack.link.sc`** — 17 classes, the connection/topology layer:

```
BAbstractConnectionManager (652)  BConnectionContainer (176)   BHubConnector (592)
BHubConnectorHealth (463)         BHubConnectorSubState (46)   BHubFunction (510)
BHubFunctionConnections (52)      BNodeSwitch (315)            BNodeSwitchConnections (94)
BScConfiguration (310)            BScCredentials (267)         BScDashboardProvider (216)
BScHubConnectorState (49)         BScLinkLayer (514)           NoConnectionException (9)
ScLinkLayerUtil (116)             VmacUtil (131)
```

**`…link.sc.message`** — 23 classes, the codec.

Contrast with B279: MS/TP had **one** class in `bacnet-rt` and every wire byte behind a JNI call. BACnet/SC
has forty, and the framing is in the codec. **No `native` method appears in either package** `[CERT]` —
consistent with SC riding on WebSocket/TLS, which the JVM already provides.

The topology vocabulary is the standard's: a node runs a **hub connector** (client role, `BHubConnector`),
a **hub function** (server role, `BHubFunction`), or a **node switch** (`BNodeSwitch`, direct-connect).

---

## 280.3 — The 13 BVLC-SC function codes `[CERT]`

`ScBvlcMessage.make(ByteBuffer)` dispatches on the first byte:

| Code | Message | Class |
|---|---|---|
| 0 | BVLC-Result | `ScBvlcResult` |
| 1 | Encapsulated-NPDU | `ScNpdu` |
| 2 | Address-Resolution | `AddressResolution` |
| 3 | Address-Resolution-ACK | `AddressResolutionAck` |
| 4 | Advertisement | `Advertisement` |
| 5 | Advertisement-Solicitation | `AdvertisementSolicitation` |
| 6 | Connect-Request | `ConnectRequest` |
| 7 | Connect-Accept | `ConnectAccept` |
| 8 | Disconnect-Request | `DisconnectRequest` |
| 9 | Disconnect-ACK | `DisconnectAck` |
| 10 | Heartbeat-Request | `HeartbeatRequest` |
| 11 | Heartbeat-ACK | `HeartbeatAck` |
| 12 | Proprietary-Message | `ScProprietaryMessage` |

An unknown function throws `ScReadMessageException` with `BBacnetErrorCode.bvlcFunctionUnknown`, logged at
`FINE` `[CERT]`. This is the complete Annex AB set — **no gaps, no proprietary extensions** beyond the
standard's own code 12. `[INFER]` on completeness relative to the standard.

---

## 280.4 — The BVLC-SC header, field by field `[CERT]`

```java
private void decode(ByteBuffer in) throws IOException, ScReadMessageException {
   int controlFlags = in.readUnsignedByte();
   checkControlFlags(controlFlags);
   this.messageId = in.readUnsignedShort();
   if (this.hasOriginatingVmac(controlFlags))  { long vmac = VmacUtil.readVmac(in); setOriginatingVmac(vmac); }
   if (this.hasDestinationVmac(controlFlags))  { long vmac = VmacUtil.readVmac(in); setDestinationVmac(vmac); }
   if ((controlFlags & 2) > 0)                 { this.destinationOptions = decodeHeaderOptions(in, true); }
   if (this.hasDataOptions(controlFlags))      { this.decodeDataOptions(in); }
   this.decodePayload(in);
   if (in.available() > 0)
      throw new ScReadMessageException("Should have reached end of message", BBacnetErrorCode.inconsistentParameters);
}
```

Wire layout:

```
+--------+--------------+------------------+-------------+-------------+--------------+---------+
| func   | controlFlags |   messageId      | [orig VMAC] | [dest VMAC] | [dest opts]  | payload |
| 1 byte | 1 byte       | 2 bytes unsigned |  6 bytes    |  6 bytes    | [data opts]  |         |
+--------+--------------+------------------+-------------+-------------+--------------+---------+
```

**Control-flag bits**, from the guard methods `[CERT]`:

| Bit | Mask | Meaning |
|---|---|---|
| 3 | `8` | Originating Virtual Address Flag |
| 2 | `4` | Destination Virtual Address Flag |
| 1 | `2` | Destination Options Flag |
| 0 | `1` | Data Options Flag |

`checkControlFlags` enforces `0 ≤ controlFlags ≤ 15` — *"Control Flags value must be between zero and 0x0F
inclusive"*, else `parameterOutOfRange` `[CERT]`. **Only the low nibble is defined.**

**A strict-decoder detail worth knowing**: the *base class* refuses three of the four flags outright —

```java
protected boolean hasOriginatingVmac(int controlFlags) throws ScReadMessageException {
   if ((controlFlags & 8) > 0)
      throw new ScReadMessageException("Bit 3 (Originating Virtual Address Flag) of the ControlFlag must be zero",
                                       BBacnetErrorCode.headerEncodingError);
   return false;
}
```

with identical guards for bit 2 (destination VMAC) and bit 0 (data options). So for any message type that
does **not** override these, a peer setting those bits gets `headerEncodingError`. Only the addressed
message types (`AddressedMessage` and its subclasses) legitimately carry VMACs. `[INFER]` on which
subclasses override; the base-class refusal is `[CERT]`.

The decoder is also **strict about trailing bytes**: leftover input after the payload is
`inconsistentParameters`, not ignored. `[CERT]`

---

## 280.5 — VMAC is 48 bits `[CERT]`

`VmacUtil`:

```java
private static final long MAX_VMAC       = 281474976710655L;   // 0xFFFFFFFFFFFF = 2^48 - 1
private static final long MIN_VMAC       = 0L;
private static final long VMAC_BIT_MASK  = 281474976710655L;
public  static final long NULL_VMAC      = -1L;
public  static final long BROADCAST_VMAC = 281474976710655L;
…
do { vmac = random.nextLong() & 281474976710655L; } while (!isDeviceVmac(vmac));
```

So:

- **48 bits — six octets, the same width as an Ethernet MAC.** Carried on the wire as 6 bytes (`bytesToVmac(b0…b5)`).
- **`0xFFFFFFFFFFFF` is the broadcast VMAC**; `0` is excluded from device VMACs (`isDeviceVmac` requires
  `> 0` and `< MAX`); `-1` is Java's sentinel for "none", not a wire value. `[CERT]`
- **A device VMAC is randomly generated** and re-rolled until valid — there is no derivation from device
  instance or certificate. `[CERT]`

---

## 280.6 — Connection lifecycle parameters `[CERT]`

`BScConfiguration` slots:

| Slot | Default |
|---|---|
| `nodeMaxBvlcLength` | `MAX_SC_BVLC_LENGTH` (faceted min/max) |
| `nodeMaxNpduLength` | `MAX_NPDU_LENGTH` (faceted min/max) |
| `minimumReconnectTime` | **2 s** |
| `maximumReconnectTime` | **600 s** (10 min) |
| `connectWaitTimeout` | **10 s** |
| `disconnectWaitTimeout` | **10 s** |
| `webSocketWaitTimeout` | (present; value not read) |

The min/max reconnect pair (2 s → 600 s) is the standard's exponential-backoff window for a hub connector
retrying a lost connection. `[INFER]` on the backoff interpretation; the slots and defaults are `[CERT]`.

`BHubConnectorHealth` (463 ln) and `BScDashboardProvider` (216 ln) indicate a first-class health/diagnostic
surface, not traced here — gap **B280-G2**.

---

## 280.7 — Corrections to B23 §23.24 / §23.27

B23 states two concrete numbers. **Neither is verifiable in the code, and the transport model contradicts
one of them.** `[CERT]` (negative)

| B23 says | Finding |
|---|---|
| *"TCP (puerto IANA 49152)"* (§23.24) and *"BACnet/SC (TCP 49152 + TLS 1.3)"* (§23.27) | A corpus grep for `49152` over `bacnet-rt` returns **zero matches**. |
| *"TLS 1.3 (mutual auth)"* | A grep for `TLSv1` returns **zero matches** in `bacnet-rt`. |

What the code actually shows: SC connects over **WebSocket URIs**, validated by
`ScMessageUtil.checkWebSocketUri(uri)` and carried in `AddressResolutionAck.getWebSocketUris()` (a
**`List<String>`**) and in `BAbstractConnection`'s accept-URI handling `[CERT]`. The transport is
`IScWebSocket`, with `closeWebSocket(int statusCode, String reason)` — i.e. WebSocket close semantics.

Therefore `[INFER]`: the port is **whatever the `wss://` URI specifies**, per-connection and configurable —
not a fixed 49152 compiled into the driver. 49152 is the IANA-registered *default* for BACnet/SC in the
standard, and TLS is supplied by the WebSocket layer at whatever version the JVM/platform negotiates. B23's
numbers describe the specification, not this implementation, and should be read as such.

This does not make B23 wrong about BACnet/SC's design — the layer diagram and certificate requirements are
sound. It makes two of its figures **unsourced from code**, and worth marking so nobody cites them as
verified Niagara behaviour.

---

## 280.8 — Self-verify

| Claim | Evidence | Marker |
|---|---|---|
| P3-sc is fully static (unlike P3-mstp) | 40 classes in two `com.tridium.bacnet.stack.link.sc*` packages; no `native` methods | `[CERT]` |
| B23's three class names all exist | `class` lookup on each, with package and modifiers | `[CERT]` |
| 13 function codes, 0–12 | the `make()` switch, quoted in full | `[CERT]` |
| Unknown function → `bvlcFunctionUnknown` | verbatim throw | `[CERT]` |
| Header order func/flags/msgId/VMACs/opts/payload | `decode()` quoted in full | `[CERT]` |
| Control flags limited to 0x0F | `checkControlFlags` message verbatim | `[CERT]` |
| Bits 3/2/1/0 = origVmac/destVmac/destOpts/dataOpts | the three guard methods + the `& 2` test | `[CERT]` |
| Base class refuses bits 3, 2, 0 | all three throw `headerEncodingError` | `[CERT]` |
| Trailing bytes → `inconsistentParameters` | verbatim | `[CERT]` |
| VMAC = 48 bits, broadcast = all-ones | `MAX_VMAC`/`BROADCAST_VMAC` = 281474976710655 = 2^48−1 | `[CERT]` |
| Device VMAC randomly generated | `random.nextLong() & MASK` in a validity loop | `[CERT]` |
| Reconnect window 2 s → 600 s | `BScConfiguration` defaults | `[CERT]` |
| `49152` absent from bacnet-rt | grep → zero matches | `[CERT]` (negative) |
| `TLSv1` absent from bacnet-rt | grep → zero matches | `[CERT]` (negative) |
| Transport is WebSocket URIs | `checkWebSocketUri`, `getWebSocketUris()`, `IScWebSocket`, `closeWebSocket` | `[CERT]` |
| Hub setup needs a Niagara machine user | official guide `SettingUpAHub`, quoted | `[CERT-doc]` |
| ⇒ port comes from the URI, not a constant | composed from the above | `[INFER]` |
| Only addressed message types carry VMACs | base refusal + existence of `AddressedMessage` | `[INFER]` |

Tally: **[CERT] 14 / [CERT-doc] 1 / [INFER] 3.**

---

## 280.x — Connections and gap status

- **B133** — opened P3-sc and correctly called it static. **CLOSED here.** §280.4's BVLC-SC header is the
  SC counterpart of B133 §133.10's BVLC/BVLL header for BACnet/IP: same role, different layout (SC has
  messageId and VMACs; IP has neither).
- **B279** — the contrasting outcome for the sibling gap. MS/TP native, SC pure Java.
- **B23 §23.24 / §23.27** — **two figures corrected** in §280.7.
- **B27** — layer table; BACnet/SC belongs with the IP-based transports, not the serial ones.

### Gap status

| ID | Gap | Class |
|---|---|---|
| **P3-sc** | BACnet/SC transport | **CLOSED** |
| **B280-G1** (new) | `HeaderOption` / `SecurePathHeaderOption` / `ProprietaryHeaderOption` / `UnsupportedHeaderOption` — the header-option TLV encoding was not decoded. | STATIC-investigable |
| **B280-G2** (new) | The connection/topology layer: `BAbstractConnectionManager` (652), `BHubFunction` (510), `BHubConnector` (592), `BNodeSwitch` (315), `BHubConnectorHealth` (463). Only the codec was traced. | STATIC-investigable |
| **B280-G3** (new) | `BScCredentials` (267) — certificate handling, and how the "machine user" of the hub guide binds to an SC connection. | STATIC-investigable |
| **B280-G4** (new) | Live confirmation that the negotiated port/TLS version follow the `wss://` URI rather than a fixed 49152/1.3. | requires-execution |
