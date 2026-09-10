# Block 838 — Backing up, restoring, and licensing a live JACE-8000 (QNX TITAN): the operator procedure

> **Focus:** backup-licensing-ops (new, document-mode §20). **Scope:** the end-to-end OPERATOR how-to for a
> field JACE-8000 before/around a change — the three backup types, the USB **clone** backup (button + LED),
> the console **restore** (microUSB + passphrase, factory-recovery trap), the lighter `.dist` rollback, the
> **system passphrase** (what it protects + a non-destructive verify), and **licensing** (license = XML, not
> software; online license-server pull via License Manager; station-restart-not-JACE-reboot). Distinct from
> the `jace8000` focus (which REs the embedded QNX platform): this is the *procedure*, not the internals.
> Answers the operator thread "qué respaldos tener y cómo, cómo restaurar, cómo activar la licencia".
>
> **Live context `[CERT-live]` (operator session 2026-09-07):** station **HM_Central**, a **Distech EC-Net 4 Pro**
> (OEM Niagara) JACE-8000, `Operating System = qnx-jace-n4-titan-am335x-hs`, **Baja/Daemon 4.3.58.18**,
> `armle-v7`, single CPU. Confirms TITAN/QNX (so USB clone applies) and an old (2017-era) baseline.
>
> **Sources (all three consulted):**
> - **FUENTE 2 (official Tridium help, N4.14.0.162, verbatim this session `[CERT-doc]`):**
>   `BackupRestore/PlatformAndStationBackupOptions1`, `BackupRestore/JCreatingAUSBBackup`,
>   `BackupRestore/JACE-8000USBBackaupAndRestoreFeatur`, `BackupRestore/RestoringFromABackup`,
>   `BackupRestore/RestoreOptions`, `Platform/aDistFileInstaller`, `Platform/aQnxPlatformAdminDifferences`,
>   `Platform/platDaemon-PlatformAdministration`, `Platform/platDaemon-LicenseManager`,
>   `Platform/ImportingAHostLicense-24F67E04`, `Platform/aLicenseFileApplications`,
>   `Platform/ChangingThePlatformsSystemPassphras`, `Platform/aPlatformSystemPassword`,
>   `J8Startup/ID-1208-00000237`, `J8Startup/J8PrepareNcForComm`, `J8Startup/pDistributionFile`. Hashes in `sources/SOURCES.md`.
> - **FUENTE 1 (corpus):** `jace8000` B459/B463/B469 (USB clone / Factory Recovery / serial console — the
>   platform side), `jace8000-sd` B674 (licenses/certificates live under the Host ID on the SD),
>   `license-diff` B386–B391/B442–B443 (licensed-vs-unlicensed on disk, SMA differential).
> - **FUENTE 3 (code):** `backup-rt.jar` BBackupService / BFoxBackupJob (B811 — the online `.dist` engine).

---

## 1. Three backup types — they are NOT interchangeable `[CERT-doc]` (PlatformAndStationBackupOptions1)

| Backup | Contents | Tool | Note |
|--------|----------|------|------|
| **Station copier** | station `.bog` + histories + alarms | Workbench | cross-model install |
| **Backup `.dist`** | `.bog` + histories + alarms + **module references + JVM/OS version + platform config** | Workbench → right-click station → *Backup Station* | needs a *clean* dist to downgrade |
| **Clone backup** | `.bog` + histories + alarms + **copies of modules + JVM + OS image + platform config** | Browser / **USB port** | **self-contained**; restore only to the **same model**; JACE-8000 only |

Key distinction: a `.dist`/BackupService backup holds only **pointers** to core modules — restoring it needs
Workbench **plus** a software database with matching `.dist` versions. The **clone** carries the whole image
(incl. the **QNX OS**), so it restores with no Workbench. `[CERT-doc]` JACE-8000USBBackaupAndRestoreFeatur §12–34.

## 2. USB clone backup — arm it, then the button dance `[CERT-doc]` (JCreatingAUSBBackup)

**Prereq (one-time):** Platform Administration → **Advanced Options** → check **`USB Backup Enabled`**. On a
QNX unit that dialog holds exactly three toggles — *SFTP/SSH Port, Daemon Debug, **USB Backup*** `[CERT-doc]`
(aQnxPlatformAdminDifferences §24; platDaemon-PlatformAdministration §54). Ticking it only enables the port —
**no reboot, no station stop** (Reboot is a separate button). **USB must be FAT32/FAT32X** (NTFS unsupported),
**flash stick ≤128 GB**, not an external HDD `[CERT-doc]` (JCreatingAUSBBackup §26,33).

**Backup can run while the station is running** — no control downtime `[CERT-doc]` (§12). Steps:
1. Controller powered on. 2. Insert USB → **LED on**. 3. **Hold** the backup/restore button until the **LED
flashes at medium speed** (100 ms), release. 4. Backup runs → **LED flashes slow** (1 s). 5. Done → prepares
for safe removal. 6. **LED off** → remove USB. Error = **rapid flash + 3 s pause** (no space / write-protected).
Result file name = `hostid_timestamp`, e.g. `Qnx-TITAN-D01C-…_20170912230355` `[CERT-doc]` (§47–108).

## 3. Restore from the USB clone — the delicate one `[CERT-doc]` (RestoringFromABackup, RestoreOptions)

Restore returns the unit to the backup's state; may target a **same-model** unit; **no Workbench** needed.
**Prereqs:** the USB; a **USB-to-microUSB cable** to the **Debug port**; a **terminal emulator (PuTTY) at
115200 / 8 / N / 1**; and the **system passphrase** (§33–42,58–68).

1. PuTTY set as above. 2. **Power OFF** the controller. 3. Connect microUSB PC→Debug port. 4. Insert the USB.
5. Power on and **hold** the button through boot (~5 s) until *"Backup/Restore button press detected…"* → release.
6. **⚠ 10-second countdown**: *"Press any key to restore… factory recovery will begin in 10 seconds."* — **press
any key** or the unit runs **factory recovery = ALL DATA LOST** (§91–104; the 4.7U1+ "USB-inserted skips
factory recovery" guard does **not** exist on 4.3). 7. Enter the **current system passphrase**. 8. USB mounts
(minutes), lists backups (`1) Abort · 2) Show other host IDs · 3) hostid_timestamp`) — type the number. 9. Prompt
*"is the backup's passphrase the same as the system passphrase? Y/N"* — if changed since the backup, enter the
**backup's** passphrase (§171–176). 10. Restore runs — **never interrupt** (pulling USB / power = non-functional
unit, §183–185). 11. Done → power-cycle. **Restore wipes the existing install** (licenses, TCP/IP, WiFi, platform
creds) and repopulates from the backup; restoring to a **different** host ID needs a **new license** (§122–132).

## 4. The lighter rollback + the upgrade rule `[CERT-doc]` (aDistFileInstaller)

Two tiers — use the lightest that fixes it: **station/config broken →** restore the **`.dist`** via Distribution
File Installer / Station Copier (remote-doable); **platform/OS broken →** the clone restore of §3.
**Upgrade rule:** to **upgrade** a controller use the **Commissioning Wizard**, **NOT** the Distribution File
Installer — a clean `.dist` install **wipes the file system** (downgrade / near-factory), it does not upgrade (§34–41).

## 5. System passphrase — the key to the whole safety net `[CERT-doc]` (aPlatformSystemPassword, ChangingThePlatformsSystemPassphras)

The system passphrase encrypts sensitive data in `config.bog` **and** in station **backup `.dist`** files, and is
demanded when copying stations or **restoring backups** (§11–16). *"If you do not know the passphrase for a `.dist`
file you cannot install it."* Complexity: **≥10 chars, ≥1 digit, ≥1 lower, ≥1 upper** (Changing… §48). It is
**distinct** from platform credentials and the station login.

**Non-destructive verify `[INFER]` (built on the `[CERT-doc]` "old passphrase required" rule):** Platform
Administration → **System Passphrase** → enter the given value as **old**, and the **same** value as new + confirm
→ OK. Accept ⇒ correct (it validated the old) **and unchanged** (same value = no-op); reject ⇒ wrong — learned
**before** any risky move. **Two-passphrase caveat (§3):** a backup decodes with the passphrase in force **when it
was made** — so **make the backup after confirming the passphrase**, and both coincide by construction.

## 6. Licensing — license ≠ software `[CERT-doc]` (aLicenseFileApplications, ImportingAHostLicense, platDaemon-LicenseManager)

A `.lic` is a small **XML**: Host ID + a list of `<feature>` (station, web, drivers, capacity…) + a per-feature
**`expiration`** (the SMA date), e.g. `<feature name="station" expiration="2025-04-01" …/>` (§36–40). It **does
not contain or install software**; the SMA date only sets the **highest** Niagara version you may run (a ceiling,
not a floor — a new-SMA license runs fine on old 4.3).

**Pull from the online license server** (JACE has Internet): Platform → **License Manager** → **Import** → **"Import
licenses from the licensing server"** (shown only if the PC has Internet). Workbench **silently searches by Host ID**
and installs `[CERT-doc]` (ImportingAHostLicense §12,45–46; auto-retrieved during commissioning — J8PrepareNcForComm
§24, ID-1208-00000237 §29,38–48). **Prerequisite:** a license for **this exact Host ID must already exist on the
server** — if none, a **License Request Form opens in the browser** showing the Host ID (§73–75); the JACE never
mints a license itself. **Effect on the live unit:** installing the license does **not reboot the JACE**; a
*"Licensing Complete"* window asks whether to **restart the STATION** (Yes/No — your timing) for it to take effect
(§58–59). A station restart pauses control briefly (seconds–min); it is **not** a controller reboot. **View/back up:**
License Manager `View` shows the file; **`Export File` → `.lar`** is your license backup `[CERT-doc]` (platDaemon-LicenseManager §43–49).

## 7. Version-jump reality: 4.3 → 4.15 is a MAJOR migration, not an update

`[CERT-doc]` (pDistributionFile §23) *"Niagara 4.8 contains an OS upgrade from **QNX6.5 to QNX7.0**"* — a 4.3→4.15
jump crosses the controller-OS boundary. Plus: the **SMA/expiration** gate must cover the target release (a 2017
license won't authorize 4.15); it is **Distech EC-Net (OEM)** so the upgrade uses Distech's distribution; custom
modules must be **recompiled**; JACE-8000 resource headroom must be checked. The HARBOR proposal **excludes** "major
upgrade" — a separate project. Do it **only** on a real driver (end-of-support, security, a needed feature), with
the clone USB of 4.3 as the rollback.

## 8. Gotchas (each a real failure mode)

1. Missing the **10-second key press** on restore → factory recovery **wipes everything** (no 4.7 guard on 4.3).
2. **NTFS/exFAT** USB → clone silently unusable; only FAT32/FAT32X.
3. Relying on the **`.dist`** as a full rollback → it's only module *pointers*; the **clone** is the full image.
4. Losing the **system passphrase** → the backup cannot be decoded — you lose access to encrypted data.
5. Expecting the license to "bring" 4.15 → it only **authorizes** up to its SMA; software is installed separately.
6. Assuming the JACE auto-downloads a license that was **never issued to its Host ID** → you get a request form, not a license.
7. Using the **Distribution File Installer to "upgrade"** → it wipes; upgrades go through the Commissioning Wizard.
8. Interrupting a running **restore** → possible non-functional controller.

---

## Self-verify

| # | Claim | Marker | Evidence |
|---|-------|--------|----------|
| 1 | Three backup types; clone = self-contained incl. QNX OS, JACE-8000 only | [CERT-doc] | PlatformAndStationBackupOptions1; JACE-8000USBBackaupAndRestoreFeatur §12–34 |
| 2 | USB Backup Enabled lives in Advanced Options (QNX: 3 toggles); enabling = no reboot/stop | [CERT-doc] | aQnxPlatformAdminDifferences §24; platDaemon-PlatformAdministration §54 |
| 3 | USB must be FAT32/FAT32X ≤128 GB flash; backup runs with station running | [CERT-doc] | JCreatingAUSBBackup §12,26,33 |
| 4 | Backup steps + LED states + filename hostid_timestamp | [CERT-doc] | JCreatingAUSBBackup §47–108 |
| 5 | Restore needs microUSB + PuTTY 115200 8N1 + system passphrase | [CERT-doc] | RestoringFromABackup §33–42,58–68 |
| 6 | 10-sec countdown → factory recovery (ALL DATA LOST); no 4.7 guard on 4.3 | [CERT-doc] | RestoringFromABackup §91–112 |
| 7 | Restore wipes licenses/TCP-IP/WiFi/creds; different host ID needs new license; never interrupt | [CERT-doc] | RestoringFromABackup §122–132,183–185 |
| 8 | Two passphrases: backup decodes with the passphrase in force when made | [CERT-doc] | RestoringFromABackup §40–42,171–176 |
| 9 | `.dist` installer wipes/downgrades; upgrade uses Commissioning Wizard | [CERT-doc] | aDistFileInstaller §34–41 |
| 10 | System passphrase encrypts config.bog + `.dist`; complexity ≥10/1/1/1; distinct from creds | [CERT-doc] | aPlatformSystemPassword §11–16; ChangingThePlatformsSystemPassphras §47–48 |
| 11 | Non-destructive verify = re-enter same value as old+new in Set System Passphrase | [INFER] | derived from the [CERT-doc] "old passphrase required" rule (Changing… §47) |
| 12 | License = XML (Host ID + features + expiration/SMA); ceiling not floor; no software bundled | [CERT-doc] | aLicenseFileApplications §36–40 |
| 13 | License Manager Import → "from the licensing server" searches by Host ID; needs license pre-issued to that Host ID | [CERT-doc] | ImportingAHostLicense §12,45–46,73–75; ID-1208-00000237 §29,38–48 |
| 14 | Installing license → no JACE reboot; prompts STATION restart (Yes/No) to take effect | [CERT-doc] | ImportingAHostLicense §58–59 |
| 15 | License Manager Export File → `.lar` backup; View shows file | [CERT-doc] | platDaemon-LicenseManager §43–49 |
| 16 | 4.8 = QNX 6.5→7.0 OS upgrade → 4.3→4.15 crosses the OS boundary | [CERT-doc] | pDistributionFile §23 |
| 17 | HM_Central = TITAN/QNX EC-Net JACE-8000 on 4.3.58.18 | [CERT-live] | operator Platform Administration view 2026-09-07 |

**Tally:** 17 claims — 15 [CERT-doc], 1 [CERT-live], 1 [INFER] (the verify technique, explicitly derived). No unmarked assertions.

## Connections
- **jace8000 B459/B463/B469** — USB clone / Factory Recovery / serial console from the platform-RE angle; this block is the operator procedure over the same features.
- **jace8000-sd B674** — licenses + certificates live under the Host ID on the SD (`security/{certificates,licenses}`), the on-disk side of §6.
- **license-diff B386–B391 / B442–B443** — licensed-vs-unlicensed on disk and the SMA/authorization differential; §6/§7 SMA gate.
- **B811** — `backup-rt` BBackupService / BFoxBackupJob: the code behind the online `.dist` (`Backup Station`).
- **PANCCADIA/HARBOR operational context** — the proposal excludes major upgrades (§7 here).

## Open gaps (RESEARCH-STATE-backup-licensing-ops)
- **B838-G1** — Live-capture the actual restore console session on an authorized JACE (real menu numbering, mount timing) → `[CERT-live]`.
- **B838-G2** — Distech EC-Net licensing specifics: is the license server Distech-branded vs the Tridium `licensing.tridium.com`, and the exact host/port the JACE dials (firewall rule).
- **B838-G3** — The `.dist` restore procedure in detail (Distribution File Installer vs Station Copier decision + passphrase prompts) as its own recipe.
