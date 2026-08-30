# B673 — JACE-8000 microSD QNX partitions read (raw, no mount): both P2 and P3 are QNX6 Power-Safe filesystems (magic 0x68191122, 1 KB blocks) — P2 is the LIVE Niagara filesystem (NRE 4.14.0.162, station `JACE_UMBRELLA` at `/home/niagara/stations/`, modules at `/opt/niagara/modules/`, ~249 MB / 699 inodes) and P3 is a small QNX6 recovery/maintenance slot (QNX bootloader + `n4-titan-am335x-maint.signed` + factory props, 11 inodes) (focus jace8000-sd, SD-G1; §12 [CERT-hw])

> **Focus:** `jace8000-sd` (§16). **Gap closed:** SD-G1 (read the QNX partitions 2 & 3 of the boot microSD).
> **Phase:** §12 dynamic — analysis of a **full raw image** of the operator's physical card.
> **Marker:** `[CERT-hw]` — the real card's bytes.
> **Sources:** `sources/probes/B672-jace8000-sd/{qnx-superblock-stats,qnx-p2-niagara-fs,qnx-p3-recovery}.txt`
> (parsed/extracted from the 4,018,143,232-byte raw image `jace-sd.img`, sha256 of image kept out of repo) ·
> `[CERT]` corpus [Block 672] (the FAT32 boot partition + card geometry), [Block 459]/[Block 473] (JACE-8000
> QNX + live station `JACE_UMBRELLA`), [Block 463] (recovery), [Block 392]/[Block 394] (signing).
> **SECRETS DISCIPLINE (live-install):** the QNX filesystems hold the station `config.bog`, keyring, and
> factory credentials. This block cites STRUCTURE (paths, versions, counts, image names) only; the raw image
> and any secret values stay in the scratchpad, never committed. `facPw` is masked wherever it appears.
>
> **Bottom line:** the two "Unknown" partitions Windows could not read are both **QNX6 Power-Safe
> filesystems**. **P2 (3.3 GB) is the live Niagara install** — `/opt/niagara/` (modules) and
> `/home/niagara/stations/JACE_UMBRELLA/config.bog`, running **NRE 4.14.0.162**, ~249 MB used across **699
> files/dirs**. **P3 (256 MB) is a tiny recovery/maintenance slot** — a QNX bootloader plus the **maintenance**
> and main signed firmware images and the factory props, just **11 inodes**. Read entirely **raw, read-only**
> (no mount: this WSL kernel has no `qnx6` driver and no sudo).

---

## §673.1 — How it was read: raw, because there is no `qnx6` mount here `[CERT-hw]`

The full card was imaged to `jace-sd.img` (4,018,143,232 B, `errs=0` — complete). This WSL2 kernel
(5.15.167.4-microsoft-standard-WSL2) has **no `qnx6` filesystem driver** (`/proc/filesystems` has no `qnx6`,
no module on disk) and the session has **no sudo**, so `mount -t qnx6` is impossible here. Instead the QNX6
structures were parsed **directly from the image** in Python (superblock) + `strings`/`grep` over the used
regions. READ-ONLY throughout; the card was never written. Full evidence in `sources/probes/B672-jace8000-sd/`.

## §673.2 — Both partitions are QNX6 Power-Safe filesystems `[CERT-hw]`

Superblock magic **`0x68191122`** (QNX6) at partition-relative offset **`0x2000`** in both P2 and P3;
**block size = 1024 B** each `[CERT-hw qnx-superblock-stats.txt]`. This confirms partition type `0xb1`
(from [Block 672] §672.2) resolves to the **QNX6 (fs-qnx6) Power-Safe** filesystem — the JACE-8000's on-disk
format, consistent with the QNX identity in [Block 459].

| Partition | QNX6 blocksize | used blocks (≈MB) | used inodes | Role |
|---|---|---|---|---|
| **P2** (3.3 GB) | 1024 | 243,492 (~249 MB) | **699** | live Niagara filesystem |
| **P3** (256 MB) | 1024 | 79,046 (~81 MB) | **11** | recovery / maintenance slot |

## §673.3 — P2 = the live Niagara filesystem `[CERT-hw]`

Raw string extraction from P2's used region (a station-log area at ~offset 881 MB) yields, verbatim
`[CERT-hw qnx-p2-niagara-fs.txt]`:

- **`Niagara Runtime Environment: 4.14.0.162`** — the NRE version on the JACE (matches the supervisor
  version elsewhere in the corpus).
- **`Saved /home/niagara/stations/JACE_UMBRELLA/config.bog`** — the running **station name `JACE_UMBRELLA`**
  and its database path. This is the **same station** whose `config.bog` was pulled live over Fox in
  [Block 473] — now confirmed on the physical media at `/home/niagara/stations/JACE_UMBRELLA/config.bog`.
- **`<file:/opt/niagara/modules/honeywellSylkDevice-rt.jar>`** — the Niagara install + module directory
  `/opt/niagara/modules/`.
- `*** Station Started (…ms)` / `Saving station...` — station lifecycle logs on the card.
- `channel 'niagaraProv'` (provisioning) and `Niagara tagdictionary …` (tag subsystem) present.

grep over the whole image (byte offsets all inside P2): `/opt/niagara` ×236, `/home/niagara` ×24,
`config.bog` ×7 `[CERT-hw]`. So the JACE-8000 runs its OS + Niagara from **P2**, with the **QNX standard
layout `/opt/niagara` (install) + `/home/niagara` (user/stations)** — the same paths [Block 462] described
for the JACE-8000 filesystem, now verified on the card.

## §673.4 — P3 = a QNX6 recovery / maintenance slot `[CERT-hw]`

P3 has only **11 inodes** / ~81 MB. Its strings `[CERT-hw qnx-p3-recovery.txt]`:

- **`QNX v1.2b Boot Loader`** + `Invalid OS Image` / `Missing OS Image` / `Unsupported Multi-Boot`
  (bootloader + its error strings).
- **`n4-titan-am335x-maint.signed`** — a **maintenance** firmware image, distinct from the main
  `n4-titan-am335x.signed` on P1 ([Block 672]); the main image name + `u-boot.img` + the identical
  `uenvcmd=…go 0x80FFFC00` boot command also appear here.
- **`META-INF/NIAGARA1.RSA` / `NIAGARA4.RSA` / `NIAGARA4.SF`** — JAR code-signing members: the `.signed`
  images are **signed archives** (ties the OT-edge image to the signing thesis, [Block 392]/[Block 394];
  two signature slots — a v1 and a v4 — suggest dual/legacy signing).
- `# Honeywell Webs Golden Image Version:4.9.1.30` and `facUser=honeywell` / `facPw=***` — the **factory
  props appear on P3 as well** (a second on-card copy of the plaintext factory credential; [Block 672]
  §672.5 security finding applies here too — masked).

Interpretation: **P3 is the factory recovery/maintenance partition** — a self-contained QNX boot + signed
maintenance/main firmware + factory config, the on-card companion to the recovery mechanics documented from
docs in [Block 463]. `[CERT-hw for the strings; INFER for the "recovery partition" role synthesis]`

## §673.5 — Whole-card model (with [Block 672]) `[CERT-hw]`

```
JACE-8000 boot microSD (4 GB, MBR)
├─ P1  FAT32  128 MB   (Windows-readable, [Block 672]) — AM335x boot chain:
│                       mlo → u-boot.img → uEnv.txt → n4-titan-am335x.signed (CertISW) + fac.properties
├─ P2  QNX6   3.3 GB   — LIVE Niagara: /opt/niagara (modules), /home/niagara/stations/JACE_UMBRELLA/config.bog,
│                       NRE 4.14.0.162, ~249 MB / 699 inodes
└─ P3  QNX6   256 MB   — recovery/maintenance: QNX bootloader + n4-titan-am335x-maint.signed + factory props,
                        11 inodes
```
The card is therefore **self-sufficient**: bootloader + OS/Niagara + recovery all on one SD. Pulling it stops
the JACE (it boots from here); imaging it read-only, as done, does not alter it.

## §673.6 — What is NOT yet done (residual child gap)

- **SD-G1b (requires-execution):** the **full recursive file tree + per-file extraction** of P2/P3 (e.g. the
  complete `/opt/niagara` module list, the station files, keyring `.km`/`.kr`, logs) is **not** enumerated —
  the evidence here is raw string/offset analysis, not a walked directory tree. Closing it needs either a
  `qnx6`-capable mount (a kernel with `CONFIG_QNX6FS_FS`, e.g. a full Linux VM or a custom WSL kernel) or a
  complete QNX6 tree parser (superblock → root inode → directory blocks). The raw image is preserved in the
  scratchpad for that pass. `config.bog` itself is already available decoded via the live Fox pull ([Block 473]).

## §673.7 — Self-verify

| # | Claim | Marker | Cite |
|---|---|---|---|
| 1 | Full 4 GB image read, errs=0; QNX read raw (no qnx6 mount, no sudo) | [CERT-hw] | §673.1; probes README |
| 2 | P2 & P3 both QNX6 (magic 0x68191122 @0x2000, blocksize 1024) | [CERT-hw] | qnx-superblock-stats.txt |
| 3 | P2: 699 inodes, ~249 MB used; P3: 11 inodes, ~81 MB used | [CERT-hw] | qnx-superblock-stats.txt |
| 4 | P2 = Niagara FS: NRE 4.14.0.162, station JACE_UMBRELLA, /home/niagara/stations, /opt/niagara/modules, config.bog | [CERT-hw] | qnx-p2-niagara-fs.txt |
| 5 | JACE_UMBRELLA matches the live-Fox station of B473 | [CERT-hw] + [CERT] | §673.3; [Block 473] |
| 6 | P3 = recovery: QNX v1.2b bootloader, n4-titan-am335x-maint.signed, NIAGARA1/4.RSA JAR signatures, factory props | [CERT-hw] | qnx-p3-recovery.txt |
| 7 | factory credential also present on P3 (2nd on-card copy, masked) | [CERT-hw] | qnx-p3-recovery.txt |
| 8 | full recursive tree/extraction still open (SD-G1b) | [CERT-hw] (negative) | §673.6 |

**Tally:** 8 claims — 8 [CERT-hw] (one also [CERT] cross-ref). The "recovery partition role" of P3 is an
[INFER] synthesis flagged in §673.4; every underlying string is [CERT-hw]. 0 unmarked.

## §673.8 — Connections

- **[Block 672]** — the FAT32 boot partition + card geometry; this block completes the card (P2/P3).
- **[Block 459]/[Block 462]** — JACE-8000 QNX + `/opt/niagara`+`/home/niagara` layout, now verified on media.
- **[Block 473]** — the live Fox pull of `JACE_UMBRELLA/config.bog`; same station, here on the physical card.
- **[Block 463]** — recovery mechanics (docs); P3 is the on-card recovery/maintenance slot.
- **[Block 392]/[Block 394]** — signing; the `.signed` images are JAR-signed (NIAGARA1/4.RSA), OT-edge instance.
