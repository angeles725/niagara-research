# Block 450 — nrio data-handling loose ends (gaps B446-G1 + B447-G1, partial B447-G2): table interpolation clamps at both ends, device-type→firmware-image map, and the MSP430 reset vector

**Focus:** base corpus (field-I/O drivers). Closes **B446-G1** and **B447-G1**; partially closes **B447-G2** (image-level triage; deep RE deferred).

**Origin:** grandchild gaps from [B446] and [B447] — the interpolation edge policy, the `Txxxx_p_n` firmware naming / device-type mapping, and a cheap triage of the MSP430 payload.

**Sources:** `[CERT]` `nrio-rt/.../conv/BNrioTabularThermistorConversion.java`, `BNrioThermistorType3Conversion.java`; `nrio-rt/decompiled/.../BNrioNetwork.java` (`getFirmwareFiles`); `nrio-rt/.../extracted/download/*.a43` (Intel HEX byte inspection). Continues [B446]/[B447].

---

## 450.1 — Interpolation clamps out-of-range, linear within band (B446-G1)

Both thermistor conversions share one `convertTo(src, srcArray, destArray)`: `[CERT]` (`BNrioTabularThermistorConversion.java`, `BNrioThermistorType3Conversion.java`)

- **Below table** (`src < srcArray[0]`) → returns `destArray[0]` (**clamp**, no extrapolation).
- **At/above top** (`src ≥ srcArray[last]`) → returns `destArray[last]` (**clamp**).
- **Within** → find the band `srcArray[i] ≤ src < srcArray[i+1]` and linearly interpolate: `v1 - (p1 - src)*(v1 - v2)/(p1 - p2)`.

The conversion is **bidirectional**: device→proxy passes `(ohms, ohmsArray, celsiusArray)`; proxy→device swaps to `(celsius, celsiusArray, ohmsArray)`. `[CERT]` So an out-of-range sensor reads as a **pinned end-of-curve value**, never a wild extrapolation — a fail-safe-ish behavior, but it means a shorted/open sensor shows the boundary temperature rather than an obvious fault (the point's status handles fault separately). Requires the table arrays be monotonic (they are, in all shipped curves — [B446] §446.3). This resolves B446-G1's "edge policy" question: **clamp, both ends**.

## 450.2 — Device-type → firmware-image map (B447-G1)

`BNrioNetwork.getFirmwareFiles(BNrioDeviceTypeEnum type)` finds the module's **`/download`** resource dir (the `vineflower` copy mangled this to `/n`; `decompiled/` shows the real `/download`), then selects images whose filename **contains** a 4-digit model tag: `[CERT]` (`BNrioNetwork.java` `getFirmwareFiles`)

| DeviceTypeEnum | Tag | Ships as | Protocol code ([B448]) |
|---|---|---|---|
| `remoteReader` | `2030` | `T2030_1_34.a43` | 7 |
| `remoteInputOutput` | `2034` | `T2034_1_34.a43` | 8 |
| `baseBoardReader` | `2029` | `T2029_1_34.a43` | 6 |
| `io16` | `2041` | `T2041_2_2.a43` | — (legacy T-IO-16) |
| **`io16V1`** | `2101` | `T2101_2_2.a43` | **10 (the IO-R-16)** |
| **`io34`** | `2102` | `T2102_2_2.a43` | 11 (IO-R-34 primary) |
| **`io34sec`** | `2102` | `T2102_2_2.a43` | 12 (IO-R-34 secondary) |

Findings: `[CERT]`
- The modern **IO-R-16** (`io16V1`, code 10 = `Io16V1` at discovery) flashes **T2101**; the **legacy T-IO-16** (`io16`, code…) flashes T2041 — two distinct images, confirming they are different boards that merely *count the same* on the bus ([B445] §445.2).
- **IO-R-34 primary and secondary share one image** (`2102`) — the same firmware runs on both on-board controllers.
- Selection is a substring match (`getFileName().indexOf(tag) > 0`), and the method does **no version/authenticity check** — reinforcing [B447] §447.3 (no crypto gate; also no host-side version guard here). The `Txxxx_p_n` suffix (`_1_34`, `_2_2`) is the firmware **version** (major_minor) `[INFER]` — the code keys only on the 4-digit model tag, so the suffix is informational.

This resolves B447-G1's "naming + mapping" and the "does host refuse a wrong image" sub-question: **the host selects strictly by device type, but performs no authenticity check on the bytes it flashes.**

## 450.3 — MSP430 reset vector (B447-G2, partial)

Cheap image-level triage of `T2034_1_34.a43` (Intel HEX, [B447] §447.1): the MSP430 **interrupt vector table** occupies `0xFFE0–0xFFFF`; the **reset vector at `0xFFFE–0xFFFF`** holds bytes `00 11` → little-endian **`0x1100`**, exactly the first data record's load address. `[CERT]` (byte dump: last record `:10FFF000…8C11 0011AD`; first `:10110000…`)

So execution starts at `0x1100` and code fills `0x1100–0xFFFF` — a normal, contiguous MSP430F2xx flash image with no bootloader indirection visible at the image level. Full instruction-level disassembly (entry logic, whether `ReadBuildInfoMessage`'s version string is embedded, RS-485 command dispatch) remains **requires-RE** and is left as a gap.

---

## 450.4 — Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | Interpolation clamps: `src<first`→dest[0], `src≥last`→dest[last]; linear within band | `[CERT]` | `convertTo()` in both conv classes |
| 2 | Conversion is bidirectional (swap src/dest arrays for proxy→device) | `[CERT]` | `convertDeviceToProxy`/`convertProxyToDevice` |
| 3 | `getFirmwareFiles` reads `/download` dir, matches 4-digit model tag by substring | `[CERT]` | `BNrioNetwork.java` (decompiled) |
| 4 | Map: io16V1→2101, io34/io34sec→2102, remoteReader→2030, remoteInputOutput→2034, baseBoardReader→2029, io16→2041 | `[CERT]` | same method |
| 5 | IO-R-34 primary+secondary share image 2102; IO-R-16(V1) uses 2101 vs legacy io16 uses 2041 | `[CERT]` | same method |
| 6 | Host selects by device type only — no version/authenticity check before flash | `[CERT]` | `getFirmwareFiles` + [B447] §447.3 |
| 7 | `.a43` MSP430 reset vector (0xFFFE) = 0x1100 = image entry/first record | `[CERT]` | byte dump of `T2034_1_34.a43` |

**Tally:** 7 claims — 6 `[CERT]` · 1 `[CERT]`+`[INFER]` (version-suffix meaning) · 0 unmarked. No contradictions.

**Left out (named):** MSP430 instruction-level RE (B447-G2 deep); whether the *module* rejects a mismatched-type image (firmware-side, not host-side); interpolation accuracy vs the doc's ±1 % span claim.

## 450.5 — Connections
- **Closes B446-G1 + B447-G1**, partially B447-G2 (image triage). Completes the [B446] conversion story and the [B447] firmware story.
- **Cross-ref [B448]** device-type codes (10/11/12) now line up with firmware tags (2101/2102).
- **Security** ([B447] §447.3, signing-pki): host-side firmware selection is type-keyed but unauthenticated — the "no crypto at the edge" thesis holds at both the image and the selection layer.

## 450.6 — Open gaps
- **B447-G2 (remaining)** — MSP430 disassembly of a `T2xxx.a43` image: entry logic, embedded build/version string, RS-485 command dispatch, and whether the module validates image-vs-hardware. Requires native RE.
