# B672 — JACE-8000 boot microSD (physical, read-only): a 4 GB card with THREE partitions — one FAT32 boot partition (mlo→u-boot→uEnv.txt→`n4-titan-am335x.signed`, a TI "CertISW" secure-boot image) that Windows reads, plus TWO QNX partitions it cannot; the FAT32 partition also carries the Honeywell WEBs golden-image factory defaults in plaintext (focus jace8000-sd bootstrap, SD1; §12/§14 confirms B459 AM335x)

> **Focus:** `jace8000-sd` (§16, NEW — bootstrap). **Gap closed:** SD1 (what is physically on the JACE-8000
> boot microSD, read-only). **Phase:** §12 dynamic — physical inspection of the operator's real card.
> **Marker:** `[CERT-hw]` — the actual hardware, not "the code should".
> **Sources:** `sources/probes/B672-jace8000-sd/` (README, `uEnv.txt`, `fac.properties.redacted`,
> `partition-table.txt`, `signed-image-header.txt`, per-file sha256) · `[CERT]` corpus [Block 459]
> (JACE-8000 = QNX / ARM Cortex-A8) — this block adds the exact SoC + boot chain.
> **SECRETS DISCIPLINE (live-install):** the card holds a factory credential; this block cites its STRUCTURE
> and masks the value (the preserved `fac.properties.redacted` masks `facPw`; the 40 MB firmware is recorded
> by hash only, not committed).
>
> **Bottom line:** the JACE-8000 boot SD is a **4 GB card with 3 partitions** — a small **FAT32 boot
> partition (128 MB, drive `D:`)** that Windows reads, and **two QNX partitions (~3.33 GB + 256 MB)** that
> Windows shows as RAW/Unknown. The FAT32 partition is a classic **TI AM335x boot chain**
> (`mlo` → `u-boot.img` → `uEnv.txt` → load+exec `n4-titan-am335x.signed`), where the firmware is a **TI
> "CertISW" secure-boot signed image**. It ALSO carries `fac.properties` — the **Honeywell WEBs golden-image
> v4.9.1.30 factory defaults** (factory user/password + the `192.168.1.140` network config) in **plaintext**.

---

## §672.1 — How it was read (read-only) `[CERT-hw]`

WSL does not auto-mount removable drives (only `C:`), and a `drvfs` read-only mount of `D:` did not attach.
The FAT32 boot partition was read **read-only via Windows PowerShell interop** from WSL: `Get-ChildItem`,
`Get-Content`, `Get-FileHash`, `Get-Disk|Get-Partition`, and a raw byte read of the image header. No file was
modified — READ-ONLY over the subject preserved. Evidence archived under
`sources/probes/B672-jace8000-sd/`.

## §672.2 — Card geometry: 3 partitions, only one Windows-readable `[CERT-hw]`

Physical disk (USB "Mass Storage Device", MBR, 4,018,143,232 B ≈ 4 GB):

| Part | Type | Size | Offset | Windows-visible |
|---|---|---|---|---|
| 1 | **FAT32 (XINT13)** | 134,217,728 B (128 MB) | 1,048,576 | **YES → `D:`** |
| 2 | Unknown (**QNX**) | 3,576,692,736 B (~3.33 GB) | 135,266,304 | no (RAW) |
| 3 | Unknown (**QNX**) | 268,435,456 B (256 MB) | 3,711,959,040 | no (RAW) |

`Get-Volume D:` → FAT32, Removable, Healthy, 133,160,960 B, ~92.5 MB free. Partitions 2 & 3 are the QNX
filesystems (the JACE-8000 is QNX, [Block 459]) — Windows assigns them no drive letter and cannot read them.
This is why the earlier attempt saw "only a small D:" — the bulk of the card is the QNX data/system region,
invisible to Windows. `[CERT-hw partition-table.txt]`

## §672.3 — The FAT32 boot partition: a TI AM335x boot chain `[CERT-hw]`

| File | Size | LastWrite | Role |
|---|---|---|---|
| `mlo` | 34,152 B | 2015-10-07 | TI AM335x **MLO/SPL** (ROM-loaded first-stage bootloader) |
| `u-boot.img` | 268,984 B | 2015-10-08 | **U-Boot** second-stage |
| `uEnv.txt` | 78 B | **2026-08-19** | U-Boot env / boot command |
| `n4-titan-am335x.signed` | 40,264,708 B (~40 MB) | **2026-08-19** | signed Niagara 4 firmware (the OS+station image) |
| `fac.properties` | 144 B | 2022-04-25 | Honeywell factory defaults (§672.5) |
| `System Volume Information\` | — | — | Windows metadata (not JACE content) |

**Boot command** (`uEnv.txt`, verbatim) `[CERT-hw]`:
```
uenvcmd=mmcinfo;fatload mmc 0 0x80FFFC00 n4-titan-am335x.signed;go 0x80FFFC00
```
Chain: SoC ROM → `mlo` (SPL) → `u-boot.img` → reads `uEnv.txt` → `mmcinfo` (init the SD) → `fatload` the
signed firmware from FAT (`mmc 0`) into RAM at **`0x80FFFC00`** → `go` (jump/execute) there. Classic TI AM335x
SD-boot. Note `mlo`/`u-boot` are dated 2015 (the immutable bootloader) while `uEnv.txt` + the `.signed` image
are dated **2026-08-19** — i.e. the firmware was (re)written recently; the bootloader was not.

## §672.4 — The firmware image: a TI "CertISW" secure-boot signed image `[CERT-hw]`

`n4-titan-am335x.signed` first bytes = `43 65 72 74 49 53 57 00` = ASCII **`CertISW`**
`[CERT-hw signed-image-header.txt]`. `CertISW` is TI's **certified secure-boot image** wrapper (AM335x GP/HS
X-Loader/image certificate format): a certificate/signature header prepended to the payload so the ROM/SPL
verifies it before `go`. The 40 MB payload is the signed Niagara-4 "Titan" (JACE-8000) firmware.
sha256 `15B5EBA1…D96E201`. This is the OT-edge instance of the signing thesis — code/delivery is
cryptographically gated even at the bootloader (REMITTANCE: [Block 394] found the JACE firmware ECDSA-signed
to the Honeywell Product PKI; [Block 392] the signing domains). The `.signed` internals (cert chain, payload
layout) are a child gap (SD-G2, requires binary tooling).

## §672.5 — `fac.properties`: Honeywell WEBs golden-image factory defaults, in plaintext `[CERT-hw]` — SECURITY

`fac.properties` (144 B) is the factory/commissioning defaults file. Structure `[CERT-hw fac.properties.redacted]`:

```
# Honeywell Webs Golden Image Version:4.9.1.30
facUser=honeywell
facPw=***  (masked — known Honeywell WEBs golden-image factory default)
facRoute=192.168.1.1
facIP=192.168.1.140
facNetmask=255.255.255.0
```

- The card is a **Honeywell WEBs (OEM Niagara) golden image, v4.9.1.30** — the OEM re-brand of the Tridium
  JACE image (consistent with the Honeywell-signed firmware, [Block 392]/[Block 394]).
- It carries a **factory user + password in plaintext** (`facUser`/`facPw`) — the well-known Honeywell WEBs
  default pair. **Security finding:** anyone who reads this SD in a card reader obtains the factory credential.
  The value is a documented default, so its risk is that it survives onto the deployed card unchanged.
- `facIP=192.168.1.140` **matches the operator's live JACE-8000** (jace8000 focus, [Block 459]/[Block 468]) —
  i.e. this card belongs to that unit's provisioning. Consistent with the standing operator action from the
  jace8000 focus: **rotate exposed credentials** and do not leave factory defaults on a fielded card.

## §672.6 — §14: confirms and sharpens B459's SoC identity `[CERT-hw]`

[Block 459] established the JACE-8000 as QNX on **ARM Cortex-A8** (Tridium board "NPM6xx"). This physical card
**confirms** that and adds the exact silicon: the boot files (`mlo`, `u-boot.img`, and the image name
`n4-titan-am335x`) are all **TI AM335x (Sitara)** artifacts — AM335x *is* an ARM Cortex-A8 SoC, so this is a
refinement, not a contradiction: **NPM6xx (Tridium board) = TI AM335x SoC**, codename **"Titan"**. No prior
block asserted the SoC part number; this one does, at `[CERT-hw]`.

## §672.7 — Open child gaps (seeded)

| Gap | What it needs | Type |
|---|---|---|
| SD-G1 | Read QNX partitions 2 & 3 (the real filesystem: station, config.bog, keyring, logs) | requires raw imaging (dd / Win32 Disk Imager) + a QNX6 Power-Safe reader → requires-execution |
| SD-G2 | `n4-titan-am335x.signed` internals: CertISW cert chain, payload layout, whether it decrypts/verifies offline | requires binary tooling (Ghidra/r2 + TI CertISW parser) → requires-execution |
| SD-G3 | Confirm the boot chain end-to-end on the live unit (does `go 0x80FFFC00` chain-verify the CertISW cert before exec) | live serial + boot capture → blocked (needs serial console, cf. jace8000 J7-G1) |

## §672.8 — Self-verify

| # | Claim | Marker | Cite |
|---|---|---|---|
| 1 | Read read-only via PowerShell interop; no file modified | [CERT-hw] | probes/README.md |
| 2 | 4 GB card, 3 partitions: FAT32 128 MB (D:) + 2 QNX (RAW to Windows) | [CERT-hw] | partition-table.txt |
| 3 | FAT32 boot files: mlo, u-boot.img, uEnv.txt, n4-titan-am335x.signed, fac.properties (+ sha256) | [CERT-hw] | README.md |
| 4 | Boot chain: mlo→u-boot→uEnv.txt→fatload+go 0x80FFFC00 | [CERT-hw] | uEnv.txt |
| 5 | Firmware magic = "CertISW" = TI AM335x secure-boot signed image, 40 MB | [CERT-hw] | signed-image-header.txt |
| 6 | fac.properties = Honeywell WEBs golden image 4.9.1.30 + factory creds + 192.168.1.140 (plaintext) | [CERT-hw] | fac.properties.redacted |
| 7 | SoC = TI AM335x ("Titan"); confirms/sharpens B459 ARM Cortex-A8 / NPM6xx | [CERT-hw] + [CERT] | §672.6; [Block 459] |
| 8 | bootloader dated 2015, uEnv+firmware dated 2026-08-19 (recent reflash) | [CERT-hw] | README.md file table |

**Tally:** 8 claims — 8 [CERT-hw] (one also [CERT] cross-ref). 0 unmarked. `[INFER]`: "MLO/SPL role" and
"CertISW = TI secure-boot wrapper" are standard TI AM335x facts, cited to the header/filenames; treated as
[CERT-hw] identification of the artifacts, with the deeper cert-chain semantics deferred to SD-G2.

## §672.9 — Connections

- **[Block 459]** — JACE-8000 = QNX / ARM Cortex-A8; this block adds the SoC (AM335x) and the boot chain.
- **[Block 463]/[Block 466]** — JACE-8000 recovery (USB clone / factory defaults / passphrase); the SD boot
  path is the mechanism those rely on.
- **[Block 394]/[Block 392]** — firmware is Honeywell-PKI signed; the "CertISW" wrapper is the OT-edge instance.
- **[Block 468]** — the live JACE-8000 hardening + exposed-cred action; `fac.properties` is a second plaintext
  credential source on this unit.
- **jace8000 focus** (RESEARCH-STATE-jace8000.md) — this new `jace8000-sd` focus is the physical-media sibling.
