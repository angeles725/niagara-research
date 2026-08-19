# B470 — jace8000 focus synthesis: the JACE-8000 is a QNX appliance with two front doors, two encryption domains, and hardware-plus-vendor-signature-gated recovery — what that means for accessing, entering, and copying a station (focus jace8000 capstone)

> **Focus:** `jace8000` (§16) — **capstone/synthesis** consolidating B459–B469 (11 gaps: J1-J11).
> **Phase:** §12 dynamic (live JACE at `192.168.1.140`) + `[CERT-doc]`/`[CERT]`. Read-only. `live-install`.
> **Block type: SYNTHESIS** (a high design/reasoning ratio is expected and healthy, §11 — the load-bearing
> facts live in the cited evidence blocks; this block draws the threads).
> **Answers the operator's questions:** architecture (Linux/Windows? → **QNX**), how to access the station,
> how to enter the system/filesystem, how to enter the platform (and without Workbench), whether the platform
> can be RE'd to copy the `.bog`, and how to recover a station with no platform access.

## §470.1 — The seven threads

**1. It is a QNX appliance, not a PC ([Block 459]).** The JACE-8000 runs **QNX Neutrino (a real-time
microkernel OS) on an ARM Cortex-A8 (NPM6xx board)** with the **Oracle HotSpot JVM** — *neither Linux nor
Windows*. Every other answer follows from "embedded appliance" rather than "server": hardware recovery paths,
a machine-bound Host ID, a battery-backed SRAM played back on boot ([Block 465]), and a QNX filesystem.

**2. Two front doors, two credential stores ([Block 460]/[Block 461]).** The **station** (:443 web SCRAM,
:4911 Fox TLS) authenticates *station users* and serves the control app via the **bajaux ORD navigator**
(browse the whole tree with admin). The **platform daemon niagarad** (:3011 plain / :5011 TLS, both
**403-to-GET**) authenticates *platform accounts* and does OS-level admin. Losing one credential store does
not lose the other — the basis of recovery.

**3. Two at-rest encryption domains ([Block 466], refining [Block 464]).** The sharpest finding: files in the
**daemon User Home** (`/home/niagara`, the *running* station) are sealed with a **machine-only random key that
never leaves the box**; **portable** files (backups, exported stations) with a **passphrase-derived key**.
Consequence: a raw copy of the live `config.bog` is **un-decryptable off-device**; only a **BackupService
`.dist`** (re-encrypted to the passphrase key) yields secrets — and only with the passphrase.

**4. Filesystem = QNX, four ways in, unequal ([Block 462]).** `/opt/niagara` (System Home, install) +
`/home/niagara` (config + station). Routes: **platform File Transfer** (whole tree, platform login) · station
**`/file` space** (station files only, listing gated 403) · **serial QNX system shell** (micro-USB Debug port,
a *limited recovery menu*, not root) · **SSH (disabled)**. The clean whole-filesystem network route is
platform-gated.

**5. Recovery is hardware, and vendor-signature-gated ([Block 463]).** With no platform access: **(A)** restore
a **USB clone backup** (complete image, no Workbench); **(B)** **factory defaults** (BACKUP button, full wipe,
from read-only NVRAM); **(C)** **Platform Account Recovery** — serial console, ESC→option 8, which resets
credentials+passphrase **while keeping station data**, but requires a **Tridium-signed authorization key**
bound to the Host ID (24 h validity). **Recovery ≠ bypass.**

**6. Copying the `.bog` without Workbench is possible but walled ([Block 464]).** With **station admin** (which
we have), the practical route is the **station BackupService driven over a ported Fox client** (J8-G1) — no
platform login needed; the SCRAM half is already ported ([Block 457]). RE-ing the **platform** protocol
(Station Copier/File Transfer) is the harder route and only wins without station admin. **Neither defeats** the
machine-key/passphrase encryption or the vendor signature.

**7. Duplication is doubly pinned ([Block 467]/[Block 469]).** Cloning to another JACE runs, but the **Host
ID** (`Qnx-TITAN-…`, hardware-bound) pins the **license** and the **passphrase** pins portable **secrets** — so
the *engineering* clones while *licenses and sealed secrets do not travel*. A station is portable; a licensed,
secreted station on new hardware is not.

## §470.2 — The unifying idea

Niagara on the JACE protects **"who can run/own what"** with **hardware + vendor signatures** (Host ID→license,
Tridium-signed recovery key, signed `.dist`, machine-bound at-rest key) far more strongly than it protects
against an *authorized* operator reading their own engineering. This is the same thesis the corpus reached for
the framework at large ([Block 392] signing-pki: strong on identity/authorization, and the closer to physical
I/O, the more the integrity leans on hardware) — here instantiated on live hardware. The practical upshot for
the operator: **you can fully access and back up a station you have credentials for; you cannot extract another
party's sealed secrets or relicense their station onto your hardware by protocol RE alone.**

## §470.3 — Operator answers (direct)

| Question | Answer | Block |
|---|---|---|
| Is the JACE Linux/Windows? | **Neither — QNX Neutrino on ARM + HotSpot JVM** | B459 |
| How do I access the station? | SCRAM-SHA-256 login → bajaux ORD navigator (`/ord/station:\|slot:/`) / Fox :4911 | B461 |
| How do I enter the system/filesystem? | QNX `/opt/niagara` + `/home/niagara`; via platform File Transfer, station `/file`, or serial console | B462 |
| How do I enter the platform? | platform login to niagarad :3011/:5011 (Workbench, or a ported handshake); browser gets 403 | B460 |
| Enter platform without Niagara/Workbench? | port the platform protocol (harder), or use the station BackupService via a Fox client (with station admin) | B460/B464 |
| RE to copy the `.bog`? | yes via BackupService `.dist` (station admin + passphrase to read secrets); raw live `.bog` is machine-key-sealed | B464/B466 |
| Recover a station without platform access? | USB clone restore / factory defaults / Tridium-signed Platform Account Recovery (keeps data) | B463 |

## §470.4 — What remains (requires-execution / hardware)

All open items are `requires-execution` child gaps, not static-investigable: **J8-G1** (build a Fox client to
pull the `.dist` — the concrete no-Workbench `.bog` grab), **J3-G1** (platform handshake bytes), **J7-G1**
(capture the JACE-8000 Alternate Boot menu on serial), **J10-G1** (read this unit's Host ID live), **J11-G1**
(`nmap ssl-enum-ciphers` for legacy-TLS), **J5-G1** (per-file `/file` ACL), **J2-G1** (QNX mount table).
Highest operator value: **J8-G1** (a working Fox backup client) and **J11-G1** (finish the TLS posture).

## Self-verify

| # | Claim | Marker | Citation | Checked |
|---|---|---|---|---|
| 1 | JACE = QNX/ARM/JVM appliance | [CERT-doc] | [Block 459] | ✓ |
| 2 | two front doors + two credential stores | [CERT-live]/[CERT] | [Block 460]/[Block 461] | ✓ |
| 3 | two at-rest encryption domains (machine-key vs passphrase) | [CERT-doc] | [Block 466] | ✓ |
| 4 | four filesystem routes, unequal privilege | [CERT-doc]/[CERT-live] | [Block 462] | ✓ |
| 5 | recovery = hardware + Tridium-signed key | [CERT-doc] | [Block 463] | ✓ |
| 6 | .bog without Workbench via BackupService (station admin) | [CERT-live]/[INFER] | [Block 464] | ✓ |
| 7 | cloning doubly pinned (Host ID + passphrase) | [CERT] | [Block 467]/[Block 469] | ✓ |

Marker tally: synthesis of already-certified blocks; no new primary claims. **Block type: SYNTHESIS** — draws
threads across B459-B469; each thread's evidence is in its cited block. [INFER] here = the cross-block thesis
(§470.2), resting entirely on [CERT]/[CERT-doc]/[CERT-live] evidence blocks.

## Connections

- Consolidates **B459-B469** (this focus). Cross-focus: **[Block 392]/[Block 395]** (signing-pki thesis),
  **[Block 398]** (security-audit), **[Block 424]** (getHostId), **[Block 457]/[Block 458]** (api-access
  SCRAM/oBIX), **[Block 124]/[Block 381]** (platform-native niagarad).

## Focus status

**jace8000 STOPPED — investigable_open = 0, 11/11 primary gaps closed (J1-J11) + this synthesis. 7
requires-execution child gaps queued for a hardware/Fox-client session.**
