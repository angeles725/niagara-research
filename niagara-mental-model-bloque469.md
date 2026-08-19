# B469 — JACE-8000 backup, distribution, and cloning: clone-backup vs BackupService .dist, the Distribution File Installer, and the two pins (Host ID + passphrase) that gate cloning (focus jace8000, J9)

> **Focus:** `jace8000` (§16). **Gap:** J9 — the backup types, the `.dist`/distribution mechanism, and what it
> takes to clone a JACE.
> **Phase:** §12 + `[CERT-doc]`. Read-only. `live-install` → SECRETS DISCIPLINE.
> **Block type: EVIDENCE (synthesis / consolidation).**
> **Sources:** `[CERT-doc]` niagara-help (`BackupRestore/JACE-8000USBBackaupAndRestoreFeatur`,
> `AXtoN4Migration/pMakeJACEbackups`) · [Block 463]/[Block 464]/[Block 466]/[Block 467] (this focus).
>
> **Bottom line:** there are **two** backups with different completeness: the **USB clone backup** is a
> *complete standalone image* of platform + station (restored via USB + serial), while the **BackupService
> `.dist`** contains the station files plus **pointers to core modules** and needs the **Distribution File
> Installer** (and a Supervisor holding matching `.dist`/modules) to restore. **Cloning to another JACE is
> gated twice:** the **Host ID** pins the license ([Block 467]) and the **System Passphrase** pins portable
> secrets ([Block 466]) — so a clone runs, but licenses and sealed secrets do not travel.

## §469.1 — Two backup types

`[CERT-doc]` `BackupRestore/JACE-8000USBBackaupAndRestoreFeatur.txt`:
- **Clone backup (USB button):** "the file created by a USB backup **contains a complete image of the platform
  and station**, including … required core software modules" (:13,:31). Self-contained; restore via **USB
  flash + USB-to-microUSB cable + terminal emulator** ([Block 463] §463.3). Needs **no Workbench, no
  Supervisor**.
- **BackupService backup (Workbench, `.dist`):** "A backup made by the BackupService includes only **pointers
  to required core software modules**" (:30), so restoring needs "its software database with **matching
  versions of all required core `.dist` files, OS `.dist` files, and software modules**" (:33), then "use the
  **Distribution File Installer** to restore the backup" (:34). Lighter file, but restore depends on a
  Supervisor/Workbench software database.

| | Clone backup (USB) | BackupService `.dist` |
|---|---|---|
| Contents | full platform+station image + modules | station files + module *pointers* |
| Made with | USB button (no WB) | Workbench BackupService (or Fox, [Block 464]) |
| Restore tool | serial + USB | **Distribution File Installer** (platform) |
| Needs a Supervisor? | no | yes (matching modules/`.dist`) |
| Contains config.bog | yes | yes |

## §469.2 — The distribution (`.dist`) mechanism

A `.dist` is a signed distribution archive; OS `.dist` and module `.dist` files carry the platform software,
and a station-backup `.dist` carries the station. Installing any of them is a **platform** operation
(Distribution File Installer / Software Manager), so it sits behind the platform login ([Block 460]) — and
`.dist` integrity is vendor-signed ([Block 392]/[Block 463]: `SignedDistFilter` validates OS/NRE/VM). The
`.bog`/`.dist` byte format itself is already documented ([Block 16]/[Block 17]/[Block 114]) — REMITTANCE.

## §469.3 — Cloning: it runs, but two pins don't travel

To clone one JACE onto another:
1. Make a clone/backup of the source (USB clone, or a BackupService `.dist`).
2. Restore it onto the target JACE.
3. **Pin 1 — license (Host ID):** the target has a **different `Qnx-TITAN-…` Host ID** ([Block 467]), so the
   source `.license` files **do not validate**. The target needs its **own** license. The station config
   clones; the license does not.
4. **Pin 2 — passphrase (secrets):** portable secrets in the `.dist` are sealed with the **source's
   passphrase-derived key** ([Block 466]); on the target you are **prompted for the source passphrase**
   ([Block 466] §466.2) or the sealed fields fail (passphrase mismatch). Raw daemon-home secrets (machine-key
   domain) never leave the source at all.

So a clone reproduces the **engineering** faithfully but is **not** a way to duplicate a licensed, fully-
secreted station onto new hardware without also re-licensing and supplying the source passphrase. That is the
vendor's deliberate anti-duplication design, consistent with the recovery ([Block 463]) and licensing
([Block 467]) stories.

## §469.4 — Backup as the recovery/exfil pivot (ties the focus together)

- **Recovery:** a clone/backup you own is the fastest path back ([Block 463] Route A) — no Tridium, no
  platform login for the USB clone.
- **`.bog` acquisition:** the BackupService `.dist` is the station-side no-Workbench route ([Block 464] Route
  2), decryptable with the passphrase.
- **Both** are bounded by the same two pins — which is why "back up the JACE" and "clone the JACE" are
  different guarantees: the first restores the *same* box perfectly; the second needs new licensing + the
  source passphrase.

## Self-verify

| # | Claim | Marker | Citation | Checked |
|---|---|---|---|---|
| 1 | USB clone = complete platform+station image incl. modules | [CERT-doc] | JACE-8000USBBackaupAndRestoreFeatur:13,31 | ✓ token |
| 2 | BackupService backup = station files + pointers to modules | [CERT-doc] | same:30 | ✓ token |
| 3 | restore BackupService backup needs matching .dist/modules + Distribution File Installer | [CERT-doc] | same:33-34 | ✓ token |
| 4 | .dist is vendor-signed (SignedDistFilter) | [CERT] | [Block 392]/[Block 463] | ✓ corpus |
| 5 | clone to different JACE fails licensing (Host ID pin) | [CERT] | [Block 467] | ✓ corpus |
| 6 | portable secrets sealed by source passphrase → prompt/mismatch | [CERT-doc] | [Block 466] §466.2 | ✓ |
| 7 | .bog/.dist byte format already documented | [CERT] | [Block 16]/[Block 17]/[Block 114] | ✓ corpus REMITTANCE |

Marker tally: [CERT-doc] ×4 · [CERT] ×3 (corpus) · [INFER] 0 load-bearing. **Block type: EVIDENCE.** Ratio ≈ 0.

## Connections

- **[Block 463]** (recovery routes) · **[Block 464]** (.bog routes) · **[Block 466]** (passphrase pin) ·
  **[Block 467]** (Host ID pin) · **[Block 16]/[Block 17]/[Block 114]** (.bog/.dist format, REMITTANCE).

## Open gaps

No new investigable gap. Remaining open items are all **requires-execution / hardware** child gaps: J3-G1
(platform handshake bytes), J5-G1 (per-file /file ACL), J7-G1 (JACE-8000 Alternate Boot menu capture), J8-G1
(build a Fox client to pull the .dist), J10-G1 (read Host ID live), J11-G1 (nmap TLS enum), J2-G1 (QNX mount
table). **Focus jace8000 static/live-read investigable set = exhausted.**
