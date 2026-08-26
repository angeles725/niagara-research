# B530 — signing-pki SP-G8 CLOSED (static/DISK-FIRST): the OTA firmware receive path trusts the unpacked `.fw` image — no signature enforcement, and the header CRC is computed but NEVER validated (`crcValid = true` hardcoded)

**Focus:** `signing-pki` · **Gap:** SP-G8 (PanelBus/HMI OTA receive path: ECDSA enforced or jar-unpacked trust?) · **Mode:** static code-read (DISK-FIRST — the gap was `requires-execution` in name only) · **Language:** English.

**Scope.** Answer the SP-G8 question from the decompiled `honeywellDeviceManager` module (the actual OTA delivery path that B94/B98/B243 point to), rather than `honIrmConfig` itself — the premise had conflated the OTA *transport* with the config *channel*. The verdict is static and unanimous across three decompiler pipelines. SECRETS DISCIPLINE observed (this is object structure, not secrets).

**Evidence.** `organized/honeywellDeviceManager/honeywellDeviceManager-rt/{decompiled,vineflower,pipeline/procyon}/com/honeywell/devicemanager/util/{FirmwareDownloadHelper,FirmwareHeaderParser}.java`. Markers per §3.

---

## 1. The receive path, traced — `[CERT]`

Firmware is delivered as a **`.fw` ZIP** (`ZIP_FILE_EXTENSION = ".fw"`), unpacked with `BZipFile`/`BZipSpace`
(`FirmwareDownloadHelper.extractFirmwareFiles`, `:157-161`), then each file is **header-parsed + stored**:
- `FirmwareHeaderParser.decode()` (`:220-280`) reads a **plain binary header**: `headerLength`,
  `headerVersion`, **`crc`** (`setCrc(byteBuffer.readInt())`, `:231`), `fileSequence`, `productType`,
  device tag/MCU/version strings, etc. The `crc` field is a 4-byte integer in the header — no signature,
  no certificate, no ECDSA anywhere.
- `FirmwareDownloadHelper` (`:275-315`) splits header from payload, decodes the header, computes the CRC…
  …then **`boolean crcValid = true;`** (`:298` in cfr, `:284` in procyon) — the computed CRC is logged
  (`LOG.finest("File %s: CRC %d calculated CRC %d …")`) but **never compared** against the header's `crc`.
  The branch that would reject (`if (!crcValid)`) is dead code because the constant is `true`.

## 2. Verdict (the SP-G8 question, answered) — `[CERT]`

**The OTA receive path trusts the unpacked image.** There is:
- **no signature verification** — `grep` for `java.security.Signature`/`MessageDigest`/`KeyStore`/
  `ECDSASignature` across the whole `honeywellDeviceManager` module returns **zero** hits;
- **no ECDSA chain enforcement** — the HMI firmware-supply-chain signature ([B394]'s "HMI ECDSA-signed → Honeywell Product PKI") exists at the *distributor* end, but the *receive* end does not re-verify it;
- **the one integrity field present (CRC) is not enforced** — `crcValid` is hardcoded `true` in three
  independent decompiler outputs (cfr, vineflower, procyon), so even the weak CRC is decorative.

This **completes [B394]'s asymmetry to the receive side**: the signature protects the delivered artifact;
the receiver blobs it onto the device by "copy to station → BACnet `atomicWriteFile`" with no
cryptographic gate.

## 3. Why this was DISK-FIRST, not requires-execution — process note

The backlog typed SP-G8 `requires-execution` because it was framed as "confirm whether the receive path
enforces the chain". Per PROMPT-LOOP DISK-FIRST, the answer is fully determined by the decompiled code
(all three pipelines agree), so no live probe was warranted — confirming B94/B98's own "heredó el problema
de integridad (sin firma/hash)" note and extending it with the *hardcoded `crcValid = true`* detail.

## 4. Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | `.fw` is a ZIP; unpacked via BZipFile/BZipSpace | `[CERT]` | FirmwareDownloadHelper.java:42-43,157-161 |
| 2 | Header has a `crc` field, no signature fields | `[CERT]` | FirmwareHeaderParser.java:231 (`setCrc(byteBuffer.readInt())`) |
| 3 | CRC computed but never compared (`crcValid = true` hardcoded) | `[CERT]` | FirmwareDownloadHelper.java:296-299 (vineflower), 318-321 (cfr), 282-285 (procyon) |
| 4 | Zero crypto signatures in the OTA module | `[CERT]` | grep Signature/MessageDigest/KeyStore = 0 hits |
| 5 | All three decompilers agree | `[CERT]` | the 3 pipeline paths cited above |

**Tally:** 5 `[CERT]`, 0 `[CERT-live]`, 0 `[INFER]`. No unmarked claims.

## 5. Connections & gap bookkeeping

- **Closes SP-G8** (DISK-FIRST static verdict: receiver trusts unpacked image, no signature, CRC unenforced).
- Corroborates [B394] (asymmetry reaches OT edge) and [B98] (thermostat firmware "hereda el mismo problema"); refines [B243] by pinning where the chain *should* be enforced but is not.
- Feeds the hardening thread: an OTA receive-path integrity gate (verify the vendor ECDSA signature, or at minimum enforce the CRC) is a concrete remediation candidate under the H-family.
- Open items: SP-G6 (CRL/revocation), and blocked SP-G3a/SP-G4/SP-G9b.
