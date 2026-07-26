# Block 286 — BACnet/SC header options: the 1-byte marker (MORE / must-understand / has-data / type), and a must-understand policy that only bites on destination options

> Closes **B280-G1**. [B280] §280.4 showed *where* header options sit in the BVLC-SC message — decoded when
> `controlFlags & 2` (destination) or bit 0 (data) — and that the base `ScBvlcMessage` refuses those bits
> outright. This block documents the option encoding itself and, more usefully, **when a peer's option
> makes Niagara reject the whole message**.
>
> **Sources**: Vineflower decompile of `com.tridium.bacnet.stack.link.sc.message` (`com.tridium.*`, no
> `docSource`). Markers: `[CERT]` verbatim · `[INFER]` derived. **Corpus language: ENGLISH.**

---

## 286.1 — The three steps

**Step 1** — only B280 mentions header options in the corpus; nothing to reuse. `[CERT]`
**Step 2** — `find "BACnet SC header option"` → **zero matches** `[CERT]` (negative). **Fifth negative for
encoding internals**, exactly as the loop's calibration predicted. The prediction itself is now
corroborated: niagara-help covers hardware/diagnostics/workflows (B285), not codecs.
**Step 3** — code.

---

## 286.2 — The header marker: one byte, four fields `[CERT]`

```java
private static int     readOptionType(int headerMarker) { return headerMarker & 31; }      // 0x1F
private static boolean hasData(int headerMarker)        { return (headerMarker & 32) > 0; } // 0x20
private static boolean mustUnderstand(int headerMarker) { return (headerMarker & 64) > 0; } // 0x40
```

and on the encode side:

```java
public final void encode(DataOutput out, boolean hasMore) throws IOException {
   int encodedHeaderMarker = this.headerMarker;
   if (hasMore) encodedHeaderMarker |= 128;                                                 // 0x80
   out.writeByte(encodedHeaderMarker);
   this.encodeHeaderData(out);
}
```

```
 7   6   5   4 3 2 1 0
┌───┬───┬───┬──────────┐
│ M │ U │ D │  type    │   M = MORE options follow      (0x80)
└───┴───┴───┴──────────┘   U = must-understand           (0x40)
                           D = has data                  (0x20)
                           type = option type 0..31      (0x1F)
```

Note **`MORE` is not stored** — it is applied at encode time from the `hasMore` argument and read back out
of the marker only during the decode loop. The persisted `headerMarker` carries type + U + D. `[INFER]`
from the constructor, which sets only bits 5 and 6:

```java
protected HeaderOption(int optionType, boolean mustUnderstand, boolean hasData) {
   int headerMarker = optionType;
   if (hasData)        headerMarker = optionType | 32;
   if (mustUnderstand) headerMarker |= 64;
   this.headerMarker = headerMarker;
}
```

**Only three option types are recognised** `[CERT]`:

| Type | Class |
|---|---|
| **1** | `SecurePathHeaderOption` |
| **31** (0x1F — the maximum the 5-bit field can hold) | `ProprietaryHeaderOption` |
| anything else | `UnsupportedHeaderOption` |

---

## 286.3 — The must-understand policy: destination options only `[CERT]`

This is the operationally important part. From `HeaderOption.make(ByteBuffer, boolean isDestinationOption)`:

```java
switch (readOptionType(headerMarker)) {
  case 1:
    if (isDestinationOption)
      throw new ScReadMessageException("Secure path destination header option not supported",
                                       BBacnetErrorCode.inconsistentParameters, headerMarker);
    headerOption = new SecurePathHeaderOption(headerMarker);
    break;
  case 31:
    if (isDestinationOption && mustUnderstand(headerMarker))
      throw new ScReadMessageException("Proprietary destination header option not understood",
                                       BBacnetErrorCode.headerNotUnderstood, headerMarker);
    headerOption = new ProprietaryHeaderOption(headerMarker);
    break;
  default:
    if (isDestinationOption && mustUnderstand(headerMarker))
      throw new ScReadMessageException("Unknown destination header option not understood",
                                       BBacnetErrorCode.headerNotUnderstood, headerMarker);
    headerOption = new UnsupportedHeaderOption(headerMarker);
}
```

The decision table:

| Option type | As a **data** option | As a **destination** option |
|---|---|---|
| 1 — Secure Path | accepted | **always rejected** — `inconsistentParameters` |
| 31 — Proprietary | accepted | rejected **only if must-understand** — `headerNotUnderstood` |
| other — Unknown | accepted | rejected **only if must-understand** — `headerNotUnderstood` |

Three readings:

1. **Must-understand is enforced on destination options and ignored on data options.** A peer can set the
   must-understand bit on a *data* option and Niagara will happily park it in an
   `UnsupportedHeaderOption` and carry on. `[CERT]`
2. **Secure Path is refused as a destination option unconditionally** — not "not understood" but
   `inconsistentParameters`, a different error class. Secure Path describes the path a message travelled;
   it is meaningless as a per-destination directive, and Niagara says so with a structural error rather
   than a comprehension one. `[INFER]` on the rationale; the code path is `[CERT]`.
3. **An unrecognised option without must-understand is preserved, not discarded** — `UnsupportedHeaderOption`
   keeps `private byte[] headerData` and exposes `getHeaderData()` `[CERT]`. Niagara retains bytes it does
   not understand rather than dropping them, which is what a forwarding node must do. `[INFER]` on the
   forwarding motive.

The error carries the offending `headerMarker` as a third constructor argument, and only for destination
options `[CERT]`:

```java
throw new ScReadMessageException("Message header is incomplete", BBacnetErrorCode.messageIncomplete,
                                 isDestinationOption ? headerMarker : 0);
```

— so a BVLC-Result rejecting the message can tell the peer *which* option byte broke it, but only in the
destination case. `[INFER]`

Two overridable hooks, `checkMustUnderstandFlag` and `checkDataFlag`, are **empty in the base class**
`[CERT]` — subclasses tighten the rules further. Not traced per-subclass; gap **B286-G1**.

---

## 286.4 — `ProprietaryHeaderOption`: vendor-scoped TLV `[CERT]`

```java
private int    vendorId;
private int    proprietaryType;
private byte[] proprietaryHeaderData;
private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

public static ProprietaryHeaderOption make(boolean mustUnderstand, int vendorId,
                                           int proprietaryType, byte[] proprietaryHeaderData) {
   …
   ScMessageUtil.checkUnsignedShort(vendorId, "vendorId");
   …
}

// encode
out.writeShort(this.vendorId);
…
// decode
int headerDataLength = in.readUnsignedShort();
```

So a proprietary option is:

```
[marker: type=31 | U | D] [length: 2 bytes] [vendorId: 2 bytes] [proprietaryType] [data…]
```

**`vendorId` is validated as an unsigned short** — the ASHRAE vendor registry id, the same namespace as the
`vendors.xml` documented in B271 §271.14 (where Alerton = 18). A vendor can therefore extend BACnet/SC
headers without colliding with anyone else, and Niagara will carry those bytes through even when it cannot
interpret them. `[INFER]`; the validation is `[CERT]`.

**Header data length is a 2-byte unsigned field** — so up to 65 535 bytes per option.

---

## 286.5 — Self-verify

| Claim | Evidence | Marker |
|---|---|---|
| niagara-help: nothing on SC header options | one query, zero matches (5th consecutive for encoding) | `[CERT]` (negative) |
| Marker fields: 0x1F type / 0x20 data / 0x40 must-understand / 0x80 more | the three readers + `\|= 128` in `encode` | `[CERT]` |
| MORE is applied at encode, not stored | constructor sets only bits 5 and 6 | `[INFER]` |
| Three recognised types: 1, 31, default | the `switch`, quoted in full | `[CERT]` |
| Secure Path always rejected as destination option | `if (isDestinationOption) throw … inconsistentParameters` | `[CERT]` |
| Proprietary/unknown rejected only when must-understand | `if (isDestinationOption && mustUnderstand(...))` twice | `[CERT]` |
| ⇒ must-understand is ignored on data options | derived from the guards | `[INFER]` |
| Unknown options preserve their bytes | `UnsupportedHeaderOption.headerData` + `getHeaderData()` | `[CERT]` |
| Error carries the marker, destination-only | `isDestinationOption ? headerMarker : 0` | `[CERT]` |
| `checkMustUnderstandFlag` / `checkDataFlag` empty in base | both bodies are `{}` | `[CERT]` |
| Proprietary option is vendorId + type + data | fields + `writeShort(vendorId)` + `readUnsignedShort()` length | `[CERT]` |
| vendorId validated as unsigned short | `ScMessageUtil.checkUnsignedShort(vendorId, "vendorId")` | `[CERT]` |
| Same vendor namespace as `vendors.xml` | cross-ref with B271 §271.14 | `[INFER]` |

Tally: **[CERT] 9 / [INFER] 4.**

---

## 286.x — Connections and gaps

- **B280** — **G1 closed here.** §286.3 explains what happens to the options whose *presence* §280.4
  documented.
- **B271 §271.14** — the ASHRAE vendor-id namespace `ProprietaryHeaderOption` uses.
- **B133** — the classic BACnet/IP BVLC has no equivalent option mechanism; SC's header options are new in
  Annex AB. `[INFER]`

| ID | Gap | Class |
|---|---|---|
| **B286-G1** (new) | Per-subclass `checkMustUnderstandFlag` / `checkDataFlag` overrides — the base implementations are empty, so each option type may impose extra rules. | STATIC-investigable |
| **B286-G2** (new) | Where Niagara *emits* header options — nothing observed constructing a `SecurePathHeaderOption` on the send path; it may be decode-only. | STATIC-investigable |
| **Next** | **B280-G2** — the SC connection/topology layer (hub connector, hub function, node switch), where step 2 **does** have material. | STATIC-investigable |
