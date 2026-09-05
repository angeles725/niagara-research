<!-- kit-retro: jace8000-sd focus · 2026-08-30 · scope: DYNAMIC-SETUP §1b (raw-image path) + METHODOLOGY §19/§21 (no-mount FS parser) + §3 [CERT-hw] + PROMPT-LOOP SECRETS DISCIPLINE -->
<!-- review-status: applied 2026-09-05 · kit 185ad74 · shipped: D1 (DYNAMIC-SETUP §1c, PR #446 7349004), D2 (§21.2 unmountable-media rung, PR #448 185ad74), D3 (PROMPT-LOOP, PR #445 fe88d17), D4 (§3 offline media is -hw, PR #434 272e1ad) -->

# §18 Self-Retrospective — focus: jace8000-sd (2026-08-30)

**Corpus:** niagara-research · **Focus:** jace8000-sd (B672–B675, 4 blocks)
**Run:** 2026-08-30 · blocks B672–B675, gaps 4/6 closed (SD1, SD-G1, SD-G1b, SD-G2), 1 requires-execution + 1 blocked-live
**Retro agent:** §18 fresh-context self-retrospective (propose-only; no kit edits)

---

## Kit files deduped against

- `research-sdd/PROMPT-LOOP.md` (876 lines — read HOT sections + SECRETS DISCIPLINE block lines 768–799 + promote-tool line 646)
- `research-sdd/METHODOLOGY.md` (2328 lines — §3 markers, §5 preservation, §6 tools, §10 self-provisioning/CREATE cases lines 890–1024, §19 build/PoC lines 2067–2160, §21 wall lines 2220–2285)
- `research-sdd/toolbelt/tool-registry.md` (349 lines — full)
- `research-sdd/toolbelt/DYNAMIC-SETUP.md` (228 lines — full)
- Fleet-wide grep confirmation: `physicaldrive|wsl --mount|imager|dd of=` and `unmountable|filesystem parser|inode walk|superblock` across all `*.md` (excl. retros) — raw-disk imaging absent everywhere; the only superblock hits are SquashFS-specific firmware carving, not a general no-mount FS-parser technique.

---

## Summary of proposed deltas

| # | Title | Priority | Type | Kit target |
|---|---|---|---|---|
| D1 | Raw-image a removable/physical disk on WSL when `wsl --mount` fails (PowerShell `\\.\PhysicalDrive` → `.img`) + the FileStream-EOF / Int32-overflow gotchas | HIGH | PROMOTE | DYNAMIC-SETUP.md — new §1c |
| D2 | No-mount filesystem parser as a §19 CREATE tool + a named fallback rung for the "exotic/unmountable filesystem" artifact class | HIGH | PROMOTE | METHODOLOGY.md §21.2 fallback chain + §19 note |
| D3 | A raw disk/media image is itself secret-bearing: scratchpad-only, commit ONLY the derived tree/manifest (names+sizes+sha256) + masked identifiers | MED | ABSORB | PROMPT-LOOP.md SECRETS DISCIPLINE + METHODOLOGY §5 |
| D4 | `[CERT-hw]` one-line clarification: offline physical-media imaging (a device's real storage read in a reader) earns `[CERT-hw]` | LOW | ABSORB | METHODOLOGY.md §3 |

Candidate NOT proposed as a delta (dedupe → already covered by existing machinery):
- **In-run tool promotion to the shared toolbelt** (`tools/qnx6read.py` is reusable across any QNX6 image). The kit already fully encodes this: METHODOLOGY §10 CREATE case ("add it to the retro's TOOLS table for promote/absorb/keep-local/no verdict"), §19 line ~2046 (promote = tool moves into `$KIT/toolbelt/` with a companion test), PROMPT-LOOP line 646. No new rule is needed — this is applied in the TOOLS section below (verdict: `promote`), not proposed as a delta.

---

## D1 — Raw-image a removable/physical disk on WSL when `wsl --mount` fails

**Priority:** HIGH
**Type:** PROMOTE (new content — no analogue anywhere in the kit)

### Evidence

- B672 §672.1 (`[CERT-hw]`): "WSL does not auto-mount removable drives (only `C:`), and a `drvfs` read-only mount of `D:` did not attach. The FAT32 boot partition was read read-only via Windows PowerShell interop from WSL."
- B673 §673.1 (`[CERT-hw]`): "The full card was imaged to `jace-sd.img` (4,018,143,232 B, `errs=0` — complete)."
- RESEARCH-STATE-jace8000-sd.md `tried:` clause (SD-G1): "Windows/drvfs cannot read the QNX partitions (shown RAW/Unknown, no drive letter)… Next rung = raw `dd`/imager of disk 1 partitions 2/3 then a QNX6 reader."
- Run facts supplied by the driver: `wsl --mount --bare` FAILED on a USB card reader (removable media, error `0x8007000f`); the working path was a native PowerShell raw read of `\\.\PhysicalDrive` to a `.img`, then analysis of the image from WSL. Two silent gotchas in the naive PowerShell FileStream loop: (a) it stopped early because a spurious 0-length read was treated as EOF; (b) `[Math]::Min` bound the int/int overload and truncated a >2 GB size to Int32. The robust version needed seek-per-chunk + `[long]` casts + a size-forced loop.

### Deduplication result

Searched the whole kit (all `*.md`) for `physicaldrive`, `wsl --mount`, `imager`, `dd of=`, `raw image of`, `image the (card|disk|drive)` → **zero hits outside retros.**

- DYNAMIC-SETUP §1 (network mirrored networking) — network only; explicitly says it does NOT cover disks/USB.
- DYNAMIC-SETUP §1b (USB device reach via usbipd) — attaches a whole USB DEVICE to WSL so it appears at `/sys/bus/usb/devices/…`; it does NOT cover `wsl --mount [--bare]` of a physical disk, nor a PowerShell raw `\\.\PhysicalDrive` image, and it carries none of the FileStream gotchas. A card reader attached via usbipd would still leave WSL without a `qnx6` driver (the D2 problem) — a different, complementary path.
- DYNAMIC-SETUP §5 (serial/COM) and §6 (SSH) — live-service acquisition, not offline media imaging.
- tool-registry.md — has no "raw disk imaging" / physical-media capture row.

Genuinely new. The kit has no documented recipe for "get the bytes of a physical/removable disk into an analyzable image on WSL", and the two PowerShell gotchas are exactly the kind of silent-failure scar the kit prizes (a truncated image or an early-EOF image looks complete and poisons every downstream claim).

### Proposed landing

**DYNAMIC-SETUP.md** — add a new subsection after §1b (before §2):

> **## 1c. Raw-image a physical/removable disk (when `wsl --mount` fails)**
>
> WSL auto-mounts only `C:`. A removable disk (SD in a card reader, USB stick) is often NOT reachable by `drvfs`, and `wsl --mount --bare \\.\PHYSICALDRIVEn` frequently FAILS on removable media (`error 0x8007000f — the device is not ready / cannot be found`). usbipd (§1b) attaches the DEVICE but still leaves WSL without a driver for an exotic on-disk filesystem (→ §1d / the no-mount parser). The reliable path is **raw-image on the Windows side, analyze on the WSL side**:
>
> 1. From **Windows PowerShell** (elevated), find the disk: `Get-Disk` / `Get-Disk | Get-Partition` → note the `Number` and total `Size`. Confirm it is the right disk by size before reading (`\\.\PhysicalDrive<Number>`).
> 2. Raw-read the whole physical disk to a `.img` with a **robust** FileStream loop. THREE gotchas make the naive loop silently wrong:
>    - **A 0-length read is NOT EOF on `\\.\PhysicalDrive`.** The naive `while (($n = $fs.Read(...)) -gt 0)` loop stops early on a spurious short/zero read and produces a TRUNCATED image that looks complete. Drive the loop by a **known total size** (`Get-Disk … .Size`), not by "read returned 0".
>    - **Int32 overflow on a >2 GB disk.** `[Math]::Min($remaining, $chunk)` binds the `int,int` overload and truncates any size above 2,147,483,647 to Int32 — silently capping a 4 GB read. Cast every size/offset to `[long]` and compute the chunk min in `[long]`.
>    - **Seek per chunk.** Seek to the running byte offset before each `Read` rather than trusting sequential position across a raw device handle.
>    Record `errs=0` / the exact byte count against `Get-Disk.Size` as the completeness oracle (B673: `4,018,143,232 B, errs=0`).
> 3. From **WSL**, analyze the `.img` read-only (partition table, per-partition offsets, `file`, `strings`, a userspace FS parser — §1d). Never write back to the physical device.
>
> **The raw `.img` is secret-bearing** (it contains every partition incl. keyrings/shadow/config) — it stays in the scratchpad, never `sources/` or the repo (SECRETS DISCIPLINE; retro D3).
>
> **Evidence:** niagara-research jace8000-sd B672/B673 — a 4 GB JACE-8000 boot microSD in a USB card reader; `wsl --mount --bare` failed `0x8007000f`; imaged via PowerShell `\\.\PhysicalDrive` to `jace-sd.img` (4,018,143,232 B, errs=0), analyzed from WSL.

---

## D2 — No-mount filesystem parser as a §19 CREATE tool + a fallback rung for the "unmountable filesystem" artifact class

**Priority:** HIGH
**Type:** PROMOTE (new named technique + new fallback rung — no analogue in the kit)

### Evidence

- B673 §673.1 (`[CERT-hw]`): "This WSL2 kernel (5.15.167.4-microsoft-standard-WSL2) has no `qnx6` filesystem driver (`/proc/filesystems` has no `qnx6`, no module on disk) and the session has no sudo, so `mount -t qnx6` is impossible here. Instead the QNX6 structures were parsed directly from the image in Python."
- B674 §674.1 + header (`[CERT-hw]`, `§19` / §10 tool born in-run): "`tools/qnx6read.py` reads the QNX6 Power-Safe structures straight from the image: superblock at partition+`0x2000` (magic `0x68191122`); inode file with 2-level block indirection; directory walk with short + Longfile-backed long names… **`OFF=12` auto-detected by validating inode 1 as a directory**."
- B674 §674.1 — the parse was validated against an internal oracle: "the walk reproduces the exact inode counts from the superblock (P2 → 98 dirs + 599 files = 697 ≈ 699 used inodes)."
- B675 — the same reader was EXTENDED in-run with a path→inode `extract` mode to pull `n4clean.tar.gz` (43 MB) for §19 unpacking (UPDATE-IN-USE case, §10).
- `tools/qnx6read.py` (present on disk; superblock parse, `_detect_off`, 2-level `_read_file` indirection, dir walk).

### Deduplication result

- METHODOLOGY §6 "Protocol / binary-format reconstruction" (managed-member-first → known-plaintext → falsify-by-physical-impossibility) — that pattern is for an OPAQUE WIRE FORMAT or unknown binary RECORD layout you must reverse-engineer. A filesystem with a documented structure (QNX6 magic `0x68191122`, known superblock/inode/dir layout) is a different job: you write a structural WALKER against a known-but-undriven format, you do not reverse-engineer an unknown one.
- METHODOLOGY §10 CREATE case — covers authoring a tool from scratch and recording promote/absorb, but names no specific technique for "read an FS you cannot mount".
- METHODOLOGY §19 build/PoC — oracle-anchored, round-trip byte-diff; a no-mount FS parser fits §19 (it is a build deliverable with an oracle — the superblock's own inode counts), but §19 never names this deliverable class, and the "reproduce the on-disk counts" oracle is a distinct oracle shape from the canonical round-trip byte-diff.
- METHODOLOGY §21.2 fallback chains — enumerate native / JVM / .NET / PDF / PCAP / firmware / APK / JS artifact classes. There is **no "exotic/unmountable filesystem" class** and no rung for it. The closest, SquashFS, is handled only inside the firmware chain via `squashfs-extract.sh` (unsquashfs), i.e. a shipped tool, not the general "no driver, no sudo → write a userspace parser" move.

Genuinely new on two axes: (1) a NAMED technique — "when you cannot mount an exotic FS (no kernel driver AND no sudo), write a read-only userspace parser from the on-disk structures as a §19 CREATE tool, validated by reproducing the superblock's own counts"; (2) a NEW §21 fallback rung/class for unmountable filesystems.

### Proposed landing

**METHODOLOGY.md §21.2** — add a fallback rung to the artifact-class list:

> - **Exotic / unmountable filesystem (no kernel driver, or no `mount` privilege):** `native mount → loopback+driver in a VM → read-only userspace parser`. If `/proc/filesystems` lacks the type (no driver) and there is no sudo (no `mount`), the last rung is NOT a wall: **write a read-only userspace parser** against the on-disk structures (superblock → inode/allocation table → directory walk) as a §19 CREATE tool (§10). Validate the parse by reproducing a structure the FS itself records (used-inode/used-block counts from the superblock) — a parser that reproduces the on-disk counts is trustworthy; a mismatch is the finding. Preserve the parser in `tools/` with a `promote` verdict if the format recurs across targets.

**METHODOLOGY.md §19** — add a bullet after "Oracle-anchored":

> - **A read-only filesystem/format parser is a legitimate §19 deliverable, and its oracle is self-describing structure — not a round-trip byte-diff.** When a gap needs the contents of a filesystem or container you cannot mount (no driver / no privilege), a from-scratch read-only parser (superblock → inode → directory walk) is a valid `requires-execution` deliverable. Its ORACLE is a count the artifact records about itself: the parse must reproduce the superblock's used-inode / used-block totals (B674: walked 98 dirs + 599 files = 697 ≈ 699 used inodes from the QNX6 superblock). This is the read-side sibling of the round-trip byte-diff — the diff validates re-emission; the count-reproduction validates enumeration. **Evidence:** niagara-research jace8000-sd B673/B674 — `tools/qnx6read.py`, a read-only QNX6 Power-Safe reader (2-level block indirection, short + Longfile long names, `OFF=12` auto-detected by validating inode 1 as a directory), built because the WSL kernel had no `qnx6` driver and the session had no sudo.

---

## D3 — A raw disk/media image is itself secret-bearing: scratchpad-only, commit only the derived tree/manifest

**Priority:** MED
**Type:** ABSORB into PROMPT-LOOP SECRETS DISCIPLINE (+ a §5 cross-note)

### Evidence

- B672 header: "the 40 MB firmware is recorded by hash only, not committed"; "the preserved `fac.properties.redacted` masks `facPw`".
- B673 header: "the raw image and any secret values stay in the scratchpad, never committed. `facPw` is masked wherever it appears"; "sha256 of image kept out of repo".
- B674 header: "every secret-bearing file is listed by name + size only — no contents dumped; the Host ID in the license path is masked; the raw image and any extracted bytes stay in the scratchpad, never committed." §674.3 lists keyrings/shadow/keystores by name+size only.
- B675 header: "only its file MAP is recorded — extracted binaries stay in the scratchpad, not the repo."
- What WAS committed: the partition table, boot-chain file table, masked `fac.properties.redacted`, the full P2/P3 tree listings (`qnx-p2-tree.txt` / `qnx-p3-tree.txt`) with the Host ID masked (`Qnx-TITAN-44A2-****-****-363E`), and per-file sha256 — i.e. a TREE/MANIFEST, never the bytes.

### Deduplication result

PROMPT-LOOP SECRETS DISCIPLINE (lines 768–799) already mandates, for `live-install` targets: cite STRUCTURE never VALUE; the redaction checklist (shadow hashes, PSK, IMEI, VPN keys, neighbor identifiers); "the conversation is an exfil surface"; the LIVE-WRITE recipe (sha256 backup, never the body). METHODOLOGY §19 "out-of-tree deliverable" references a deliverable BY PATH + SHA-IDENTITY (manifest hash), never by copying it in. METHODOLOGY §5 preserves external evidence under `sources/`.

So the PRINCIPLE is covered. What is NOT explicit — and is the genuinely new nuance — is the **conflict between "preserve your primary [CERT-hw] evidence under `sources/probes/`" (§12/§5) and "never write keys/keyrings/shadow into `sources/`" (SECRETS DISCIPLINE)** when the primary evidence IS a full raw disk image. A raw media image is normally where a probe capture would go; but a full image contains `/etc/shadow`, keyrings, `keystore.jceks`, and `config.bog`, so it must NOT land in `sources/`. The resolution the run used — image in scratchpad, commit only a derived tree/manifest (names + sizes + sha256) with identifiers masked — is worth naming so the next physical-media run does not reflexively drop the image into `sources/probes/`. (Application of the existing rule, hence ABSORB not a new rule.)

### Proposed absorption

**PROMPT-LOOP.md SECRETS DISCIPLINE** — add after the REDACTION CHECKLIST:

> **A full raw disk/media image is itself secret-bearing evidence — scratchpad-only.** When the primary `[CERT-hw]` evidence is a whole-disk/media image (a `dd`/PowerShell raw image of a physical card or drive), it contains every partition's secrets (`/etc/shadow`, keyrings/keystores, `config.bog`, factory credentials) and therefore must NOT be preserved under `sources/probes/` the way an ordinary probe capture is. Keep the raw image (and any extracted secret-bearing binaries) in the SCRATCHPAD; commit ONLY the DERIVED tree/manifest — a listing of paths + sizes + per-file sha256, with Host IDs and credential values masked — plus a redacted copy of any small structured secret file (e.g. `fac.properties.redacted`). Anchor the image's identity by its `sha256` recorded OUT of the repo. This is the physical-media reading of the §19 "reference the deliverable by PATH + SHA-IDENTITY, never by copying it in" rule. **Evidence:** jace8000-sd B672–B675 — 4 GB image + extracted firmware kept in scratchpad; the committed evidence is the partition/file tree (`qnx-p2-tree.txt`/`qnx-p3-tree.txt`, names+sizes), masked `fac.properties.redacted`, masked Host ID, and per-file sha256.

---

## D4 — `[CERT-hw]` clarification: offline physical-media imaging earns `[CERT-hw]`

**Priority:** LOW
**Type:** ABSORB (one-line clarification into §3)

### Evidence

- All four blocks (B672–B675) mark their disk-read findings `[CERT-hw]` and justify it in each header ("the actual hardware, not 'the code should'" / "the real card's bytes" / "the real card's filesystem, parsed inode-by-inode").
- The evidence channel is a card read in a card reader (offline), NOT a live-network probe (§12) and NOT a live-serial console (§12 / DYNAMIC-SETUP §5) — the two channels §3's `[CERT-hw]` examples describe.

### Deduplication result

§3 defines `[CERT-hw]` as "verified empirically against the live system/device — the real hardware/server responding, NOT 'the code should'." The physical-inspection extension (line 73) already stretches `[CERT-hw]` beyond a "responding" device to PHYSICAL captures (device photos, indicator-lamp readings, display-state images) "when the image file is archived under `sources/probes/` and cited by filename."

Reading a device's real STORAGE MEDIA offline (a card in a reader) is neither "the device responding" nor a photo, but it is squarely the same family as the physical-inspection extension — it is the device's actual bytes, not "the code should", captured at rest. The marker use is already defensible and consistent; the gap is only that §3 does not SAY so, which could invite a future reviewer to challenge a physical-media `[CERT-hw]` as "not the device responding". A one-line clarification removes that ambiguity. Low priority because nothing was actually mis-marked.

### Proposed absorption

**METHODOLOGY.md §3** — extend the physical-inspection-evidence bullet (line 73):

> **Physical-inspection AND physical-media evidence** — device photos, indicator-lamp readings, display-state images, AND the bytes read directly off a device's own storage media offline (a microSD / eMMC / disk read in a reader, imaged read-only) — are `[CERT-hw]`. It is the device's actual data at rest, not "the code should", and it is the storage-media sibling of the visual physical-inspection case. Cite the preserved capture (a photo by filename, or a partition/tree listing + per-file sha256 for a media image — noting that a full raw image itself stays scratchpad-only per SECRETS DISCIPLINE). Bytes read off a device's real media outrank a code claim exactly as a live probe does (`[CERT-hw]` > `[CERT]`).

---

## Tools used / acquired this focus

| Tool | Path | Case (§10) | Verdict | WHY |
|---|---|---|---|---|
| `tools/qnx6read.py` | `niagara-research/tools/qnx6read.py` | CREATE (B674), then UPDATE-IN-USE (B675 added `extract` mode) | **promote** | Read-only QNX6 Power-Safe reader (superblock → 2-level inode indirection → dir walk w/ short + Longfile long names, `OFF=12` auto-detect). Built because the WSL kernel has no `qnx6` driver and the session had no sudo. Target-local today but format-generic — reusable against ANY QNX6 image (any QNX-based controller/appliance). To promote: copy to `toolbelt/` (e.g. `toolbelt/qnx6read.py`), add a companion test over a small fixture image, and add a tool-registry row under a new "QNX6 / unmountable-FS userspace reader" artifact type. Recorded here per §10 CREATE case + the D2 no-mount-parser technique.
| Windows PowerShell interop (`Get-Disk`/`Get-Partition`/`Get-Content`/`Get-FileHash` + raw `\\.\PhysicalDrive` FileStream) | (host-side, via WSL interop) | INSTALL (pre-existing) | keep-local (recipe → D1) | The physical-media capture channel. The reusable knowledge is the D1 recipe + gotchas, not a script.
| `tar` / `struct` / `strings` / `grep` | system | — | — | Standard analysis of the extracted image.

No `install-tool.sh` recipe was needed; no external tool was downloaded.

## FOCUSES.md / TARGETS.md status

- `jace8000-sd` is a NEW focus bootstrapped 2026-08-30 (physical-media sibling of the `jace8000` focus B459–B475). Confirm it is registered in `FOCUSES.md`; register/refresh the niagara-research row in the kit's `TARGETS.md` to reflect the 4 new blocks (B672–B675) at focus-commit time (living-mirror rule).
- `block_scope: shared-global` is correctly declared in RESEARCH-STATE-jace8000-sd.md (the corpus shares one global `niagara-mental-model-bloqueN` prefix).

## Proposed kit delta verdict (§18 propose-never-apply)

All deltas above are PROPOSALS for the kit maintainer's review. This file edits no kit file. The reviewer should:
1. Accept / reject / modify each delta.
2. D1 (HIGH) and D2 (HIGH) are the highest value — both name capabilities the kit has ZERO coverage of today (raw physical-disk imaging on WSL; no-mount userspace FS parser), each with hard-won silent-failure scars.
3. D3 (MED) is an absorption resolving a real tension (preserve-evidence vs never-commit-secrets) for whole-disk images — low editorial risk.
4. D4 (LOW) is a one-line §3 clarification; the marker use was already correct.
5. Separately, action the `tools/qnx6read.py` **promote** verdict (TOOLS table) — it is the highest-value reusable artifact this focus produced and is the concrete instance behind D2.
