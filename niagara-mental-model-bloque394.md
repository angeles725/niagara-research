# Block 394 — Firmware supply-chain, byte-level (gap SP-G1): three integrity postures in one OEM install — HMI firmware is device-signed (ECDSA→Honeywell Product PKI), PanelBus IO firmware is a raw unsigned flash image, and the standalone reflash payload is AES-encrypted

> **Focus:** `signing-pki` (gap **SP-G1**). Decodes the byte-level signature scheme of the OEM firmware
> that [B243] documented only at the jar-wrapper layer. Continues the [B393] thesis (signs delivery, not
> data) *into the firmware payload itself*: once a firmware image leaves the signed jar and is flashed OTA
> onto a controller, is the image itself cryptographically protected?
>
> **Angle:** open the actual firmware payloads and determine, per image, whether device-side integrity
> (embedded signature / CRC) exists independently of the jar wrapper's per-entry `SHA-256-Digest`.
>
> **Sources (disk, `[CERT]` — `openssl asn1parse`/`x509`, entropy, `xxd`):**
> - `modules/honFirmwarePackage-rt.jar` → `res/HMIFirmware/HMI_FW_v1.5.4.26.frm`,
>   `res/panelbusFirmware/Pb_fw.bin`, `res/panelbusFirmware/Pb_fw_Snapon.bin`.
> - `Palettes_and_Misc/BACnetFFT_N4_Reflash/HW_TB3026B_FW/hddcv3b23vldff-firmware.bin` (standalone reflash).
> **Remittance `[REMITTANCE]`:** [B243] (jar wrapper: DigiCert G4 RSA-4096, per-entry SHA-256-Digest);
> [B393] (integrity asymmetry); [B392] (Honeywell Product PKI **RSA** branch for modules); [B126 §126.1]
> (the four schemes; ECDSA P-256 = the embedded-controller signer).

---

## 394.1 — Verdict: firmware integrity is HETEROGENEOUS across images `[CERT]`

The jar wrapper signs the *delivery* uniformly (every payload gets a `SHA-256-Digest` under a DigiCert-G4
RSA-4096 chain, [B243]). But the payloads themselves carry **three different device-side postures**: `[CERT]`

| Payload | Size | Entropy | Device-side integrity | Body |
|---|---|---|---|---|
| `HMI_FW_v1.5.4.26.frm` (HMI display) | 1.71 MB | 8.00 | **ECDSA P-256/SHA-256 signed** (full cert chain appended) | encrypted/compressed |
| `Pb_fw.bin` (PanelBus IO) | 124 KB | 6.80 | **NONE** (raw flash image) | plaintext-ish flash |
| `Pb_fw_Snapon.bin` (PanelBus SnapOn IO) | 382 KB | 8.00 | **NONE** found | encrypted/compressed |
| `hddcv3b23vldff-firmware.bin` (BACnet FFT / TB3026B, standalone) | 143 KB | 8.00 | none visible; **AES-encrypted** (confidentiality) | encrypted |

So even inside a single firmware bundle, the display firmware is device-signed while the I/O firmware is a
bare flash image with no signature of its own. Once the jar is unpacked and `Pb_fw.bin` pushed OTA, nothing
cryptographic protects it — the [B393] "signs delivery, not the artifact" pattern, reproduced one layer
down inside firmware. `[CERT]`

## 394.2 — HMI firmware: an appended ECDSA code-signing chain to Honeywell Product PKI `[CERT]`

`HMI_FW_v1.5.4.26.frm` header is `d4b8eb1d` + version/size fields; the body is high-entropy
(encrypted/compressed). Appended at the tail is a **device-side code-signing trailer** — two signer blocks,
each `{leaf cert, issuer cert, 70-byte ECDSA signature}`, parsed with `openssl asn1parse`: `[CERT]`
- Leaf @ off 1713376: `CN=CPO_MMI_FW_CODESIGN Code Signing, O=Honeywell International Inc., C=US` — the
  actual firmware signer (CPO_MMI = the HMI/MMI product line).
- CA @ off 1714152: `CN=Honeywell CodeSign CA` ← issuer `CN=Honeywell Product PKI` — **ECDSA P-256**
  (`prime256v1`, `ecdsa-with-SHA256`), valid 2016-11-17 → year 9999.
- Two `ECDSA SIGNATURE (r,s)` blobs (~69-70 bytes, DER `SEQ{INT r, INT s}`) at offs 1714065 and 1714749.

> **Finding:** the firmware roots at **`Honeywell Product PKI`** — the SAME master PKI as the module chain
> ([B392]: `Honeywell Product PKI RSA` → `Honeywell CodeSign RSA CA` → `Niagara4Modules`), but via a
> **parallel ECDSA branch** (`Honeywell Product PKI` → `Honeywell CodeSign CA` → `CPO_MMI_FW_CODESIGN`).
> Honeywell runs one root with two algorithm branches: **RSA for Niagara modules, ECDSA P-256 for embedded
> firmware** (ECDSA fits constrained controllers, and matches [B126 §126.1]'s "embedded = ECDSA P-256").
> The HMI controller verifies this ECDSA chain before flashing — device-side authenticity independent of
> the jar. `[CERT]` / `[INFER]` (that the controller enforces it, not just carries it, is inference —
> requires the controller bootloader, out of corpus). `[CERT]`

## 394.3 — PanelBus IO firmware: a raw flash image, no signature `[CERT]`

`Pb_fw.bin` (entropy 6.80 = not encrypted) is a **raw microcontroller flash image**: header
`20000000 06032000 ffffffff ffffffff` followed by ASCII build metadata `2024-09-24 07:02` and part id
`FW_XF821`; the tail is dense flash data (only 2 trailing `0xFF` pad bytes). **No DER, no embedded
certificate, no ECDSA signature, no CRC block before the padding.** `Pb_fw_Snapon.bin` shares the same
`20000000…` header but a high-entropy (8.00) body — encrypted/compressed — with **no embedded cert found**.
Neither PanelBus image carries device-side cryptographic integrity; both rely entirely on the jar wrapper's
`SHA-256-Digest` for authenticity, which evaporates the moment the image is extracted and flashed. `[CERT]`

## 394.4 — Standalone reflash payload: AES-encrypted, block-cipher fingerprint `[CERT]`/`[INFER]`

`hddcv3b23vldff-firmware.bin` (BACnet FFT reflash for TB3026B hardware, shipped outside honFirmwarePackage
in `Palettes_and_Misc/`) is **fully encrypted** (entropy 8.00, no printable strings). Its first 16 bytes
repeat at offset 0x20 (`777e58fa ab86e2a1 411054c2 300481ec` at both 0x00 and 0x20) — the fingerprint of a
**16-byte block cipher with identical plaintext blocks producing identical ciphertext (ECB-mode or an
IV-less block layout)**, i.e. AES-family. No visible signature/DER block. This payload protects
**confidentiality**, not (visibly) authenticity — the opposite emphasis of the ECDSA-signed HMI firmware.
`[CERT]` on entropy + the 16-byte repeat; `[INFER]` on AES-ECB specifically (the repeat is diagnostic but
the key/mode are not recoverable from the corpus). `[CERT]`

## 394.5 — Why this matters `[INFER]`

Three postures, one install, one vendor: **display firmware = ECDSA-signed** (authenticity),
**IO firmware = unsigned raw flash** (delivery-only integrity), **reflash payload = AES-encrypted**
(confidentiality). The strongest link — the ECDSA device chain — sits on the HMI; the PanelBus IO firmware,
which actually drives physical outputs, is the weakest (no device-side signature). An attacker who can
substitute a `Pb_fw.bin` *before* it is re-wrapped/flashed (e.g. on the engineering workstation, or a
tampered palette) faces only the jar `SHA-256-Digest` — and [B393] already showed the jar-external data
path is where integrity disappears. This extends the [B393] asymmetry into the OT edge: **the closer to the
physical I/O, the weaker the cryptographic integrity.** `[INFER]`

---

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | Firmware integrity is heterogeneous across payloads in one bundle | [CERT] | §394.1 table (entropy + DER scan of the 4 images) |
| 2 | HMI `.frm` carries an appended ECDSA P-256/SHA-256 code-signing chain | [CERT] | asn1parse: 2 signer blocks {cert,cert,ECDSA sig} at tail |
| 3 | Chain: CPO_MMI_FW_CODESIGN → Honeywell CodeSign CA → Honeywell Product PKI | [CERT] | x509 subject/issuer of the tail certs |
| 4 | Same root as modules but ECDSA branch (modules = RSA branch, [B392]) | [CERT]/[INFER] | "Honeywell Product PKI" root name shared; RSA vs ECDSA branch |
| 5 | PanelBus `Pb_fw.bin` is a raw unsigned flash image (header + date + part id) | [CERT] | entropy 6.80, no DER, `2024-09-24`/`FW_XF821`, 2-byte ff pad |
| 6 | Standalone BACnet firmware is AES-encrypted (16-byte block repeat) | [CERT]/[INFER] | entropy 8.00; 0x00 block == 0x20 block; ECB = INFER |
| 7 | Controller *enforcement* of the ECDSA chain is not proven from corpus | [INFER] | bootloader out of scope; carriage ≠ enforcement |
| 8 | The closer to physical I/O, the weaker the device-side integrity | [INFER] | §394.5, HMI signed vs PanelBus unsigned |

**Tally:** 4 [CERT], 2 [CERT]/[INFER] mixed, 2 [INFER], 0 unmarked.

## Connections
- **Closes SP-G1.** Extends [B243] (which stopped at the jar wrapper) with the device-side payload schemes.
- **Extends [B392]:** the Honeywell Product PKI root has a **second (ECDSA) branch** for firmware, parallel
  to the RSA module branch.
- **Reinforces [B393]:** the signs-delivery-not-data asymmetry recurs *inside* firmware (HMI signed,
  PanelBus not), and pushes it to the OT edge.
- **Remits** jar-wrapper cert chain + per-entry digests to [B243]; the ECDSA-P256 scheme identity to
  [B126 §126.1]; OTA/receiving side to [B94]/[B242].

## Open gaps (updated `signing-pki` backlog)
- **SP-G5** (NEXT) — does `Honeywell.certificate`'s DSA-160 sig verify against the Tridium root key? `[investigable]`
- **SP-G7** — any optional integrity channel for the local data record beyond SIEM offload ([B75])? `[investigable]`
- **SP-G8** (new) — is the PanelBus/HMI OTA *receive* path ([B242] `honIrmConfig`) verifying the ECDSA
  chain, or trusting the jar-unpacked image? (device-side enforcement, likely `[requires-execution]`).
- **SP-G3**, **SP-G6** `[requires-execution]`; **SP-G4** `[blocked: requires-artifact]`.
