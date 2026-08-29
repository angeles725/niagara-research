# B657 — JACE-9000 serial DEBUG console: the USB-C "ATLAS System Shell" @115200 8N1, and how the JACE-9000 differs from the JACE-8000 (focus jace9000 bootstrap, J9K-0)

> **Focus:** `jace9000` (new, §16) — the JACE-9000 as a **live embedded controller reached over its serial
> DEBUG console**. Bootstrap block. Sibling of the closed `jace8000` focus [Block 459]–[Block 475].
> **Gap:** J9K-0 — what is the JACE-9000, and what exactly is the COM5 / 115200 / 8N1 serial connection on it?
> **Phase:** §12 dynamic (a real, commissioned JACE-9000 the operator is connected to over COM5; `live-install`
> → **SECRETS DISCIPLINE**: cite structure, never a credential/passphrase/key VALUE). Read-only.
> **Sources:** `[CERT-doc]` niagara-help J9-specific guides (`J9MtgWrg`, `J9Startup`, `J9BackupRestore`,
> `StationSecurity`) · `[CERT]` corpus [Block 459] [Block 448] [Block 466] [Block 467] + decompiled
> `platform-rt` · `[CERT-web]` Tridium JACE-9000 datasheet/FAQ (preserved, SOURCES.md) · `[CERT-live]`
> read-only serial probes this session (`sources/probes/B657-jace9000-serial/`).
>
> **Bottom line for the operator:** COM5/115200/8N1 is the **USB-C DEBUG port** of the JACE-9000, and what it
> speaks is the **"ATLAS System Shell"** — a menu-driven **platform-admin** interface (NOT a raw OS shell,
> NOT the Workbench TCP platform connection on :5011, and NOT the RS-485 field bus on COM1/COM2). The
> JACE-9000 is a **different machine** from the JACE-8000: an **NXP i.MX8M Plus quad-core (ARM) running
> Linux** on the internal **"ATLAS" platform**, not the JACE-8000's TI-AM335x/QNX. Only two things on that
> shell are read-only (Ping Host and the System Diagnostic submenu); everything else mutates the controller.

## §657.1 — What the JACE-9000 is (hardware + OS)

`[CERT-web]` Tridium's own JACE-9000 material and distributor spec pages agree on the platform (preserved in
`sources/`; SOURCES.md):

| Attribute | JACE-9000 | vs JACE-8000 [Block 459] |
|---|---|---|
| SoC / CPU | **NXP i.MX8M Plus, quad-core** (ARM Cortex-A53) | TI AM335x, single Cortex-A8 |
| OS | **Linux** | QNX Neutrino (RTOS) |
| Platform service class | `BSystemPlatformServiceAtlas` | `…QnxNpm6xx` |
| RAM / storage | 2 GB LPDDR4 / 8 GB microSD | 1 GB / 4 GB |
| Ethernet | 2× 10/100/1000 (GbE) | 2× 10/100 |
| Serial field bus | 2× isolated RS-485 (COM1/COM2) | 2× RS-485 (COM1/COM2) |
| DEBUG console | **USB-C** | micro-USB |
| Security | Secure boot, FIPS 140-2, TLS 1.3, encryption by default | TLS, no FIPS claim |
| Min. Niagara | 4.13+ | 4.x |

`[CERT]` The code confirms the platform split from the JACE-8000: the JACE-9000 platform service is a
distinct class that does **not** derive from the QNX base —
`public class BSystemPlatformServiceAtlas extends BSystemPlatformServiceNpsdk`
(`organized/platform/platform-rt/decompiled/com/tridium/platform/atlas/BSystemPlatformServiceAtlas.java:17-18`).
The JACE-8000 uses the `Qnx…Npm6xx` platform service ([Block 459] §459.2). "ATLAS" is Tridium's internal name
for the JACE-9000 hardware platform, and `Npsdk` (the base class) is the non-QNX / Linux SDK line.

`[CERT-live]` The live shell corroborates Linux, not QNX: the shell banner lists network interfaces as
`en0` / `en1` with `inet`/`inet6` fields (Linux `ip`-style naming), and the Host ID uses the `ATLAS-SD-…`
prefix — neither the QNX `Qnx-TITAN-…` Host ID ([Block 467]) nor QNX `/dev/ser*` naming.
> **⚠ §14 CORRECTED in [Block 665]:** the `ATLAS-SD-…` here was the *doc example*, not a live capture — the
> diagnostics timed out before a real banner read this session. The actual live Host ID (captured in B665)
> is **`ATLAS-1508-6000-2B0F-A7EA` = `ATLAS-…` (CPU-based, NO microSD)**. The Linux-not-QNX conclusion stands
> (confirmed live in B665: kernel 5.4 / i.MX8MP); only the `-SD-` prefix was wrong.

## §657.2 — COM5/115200/8N1 = the USB-C DEBUG port, speaking the ATLAS System Shell

`[CERT-doc]` The JACE-9000 hardware guide defines the port and its exact line settings verbatim:

- "DEBUG : The DEBUG port is a **USB-C port for serial debug communications to the controller** only."
  — `niagara-help/guides-clean/J9MtgWrg/ShutdownAndDebug-0B4C4735.txt:40`
- "Default DEBUG port settings are: **115200, 8, N, 1** (baud rate, data bits, parity, stop bits)."
  — `…/J9MtgWrg/ShutdownAndDebug-0B4C4735.txt:47`
- The startup guide repeats the PuTTY settings independently: `Speed (baud) : 115200` … `Stop bits : 1`
  over a "USB-C port on a JACE-9000" —
  `…/J9Startup/ConnectingToTheControllerDebugSyste-A6400BFD.txt:25,46,50`

So **115200 8N1 is the vendor default** for this port (the operator's "115600" is a typo for 115200). When
the USB-C cable is plugged into a PC, Windows enumerates a virtual COM port — COM5 on the operator's
machine, "COM3" in the Tridium example. The physical converter in this deployment is an external FTDI FT232
(`0403:6001`).

What answers on that port is the **ATLAS System Shell**, a platform-login menu:

- `[CERT-doc]` banner + menu (post-login) —
  `…/J9BackupRestore/CreatingManualBackup-34333AD3.txt:29,31,34,47-56`:
  `ATLAS System Shell` · `hostid: ATLAS-SD-…` · `niagara daemon port: https 5011` · then options
  `1 Update System Time / 2 Update Network Settings / 3 Ping Host / 4 System Diagnostic Options /
  5 Change Current User Password / 6 Change System Passphrase / 7 Create SD Backup / 8 Restore SD Backup /
  9 Reboot / L Logout`.
- `[CERT-doc]` scope of the shell: "provides simple, menu-driven, text-prompt access to basic Niagara
  platform settings, including IP network settings, platform credentials, system time, and
  enabling/disabling SFTP/SSH and Telnet, as well as creating or restoring system backups."
  — `…/J9Startup/J8AboutTheNcSystemShellMenu-AA1F5AC0.txt:13-16`
- `[CERT-doc]` auth gate: "NOTE: Login requires **admin-level platform credentials**."
  — `…/J9MtgWrg/ShutdownAndDebug-0B4C4735.txt:49`

This is a **platform** interface: it manages the box (network, time, credentials, backups), it does **not**
reach the **station** layer (points, control logic, BQL) — that lives behind the station's SCRAM login over
Fox/HTTP ([Block 461], remittance). The serial console and the TCP platform daemon on **:5011** are two
different transports to the same platform authority; on the JACE-9000 that daemon is **TLS-only** (§657.6).

## §657.3 — The read-only safety map (and a live correction to the doc menu)

`[CERT-doc]` From the documented main menu, only **`3 Ping Host`** and **`4 System Diagnostic Options`** are
non-mutating. Everything else changes controller state: `1` time, `2` network (can drop the LAN), `5`/`6`
credentials/passphrase, `7`/`8` SD backup/restore (`8` is destructive), `9` Reboot. `L` Logout ends the
session.

> **⚠ §14 CORRECTED in [Block 665]:** a real login capture (B665 §665.3) shows the live main menu **DOES
> match the doc** (`3 = Ping Host`, `2 = Update Network Settings`). The "3 → Network Config" below was
> option **2**, an operator screen mis-report — not a firmware numbering difference. The documented
> read-only map (`3 Ping`, `4 Diagnostic`) is correct for this build. The paragraph below is retained for
> the audit trail.

`[CERT-live]` **The live unit's main-menu NUMBERING does not match the doc example.** In this session the
operator selected the main-menu key documented as `3 Ping Host` and instead reached the **Network
Configuration Utility** (`Enter new value, '.' to clear the field or '<cr>' to keep existing value` /
`Hostname < atlashost > :` — `sources/probes/B657-jace9000-serial/05-network-config-utility.png`), a
**mutating** path. Whereas option `4` did land on the documented **System Diagnostic Options** (§657.4). So:
the doc's per-number mapping is **firmware-version-dependent and must be re-confirmed live** on each unit;
`4 → System Diagnostic` is confirmed here, but "3 = Ping = safe" is **not** trustworthy for this build. The
only key-independent safety rule that held live: inside the Network Configuration Utility, pressing
**`<cr>` keeps the existing value** — an accidental entry can be backed out by pressing Enter through every
field without typing a value (never `.`, which clears it).

## §657.4 — Live session findings (read-only, this session)

`[CERT-live]` Read-only probes over `/dev/ttyUSB0 @115200 8N1` (operator-authorized; the driver typed **no
credentials** — the operator's prior session had authenticated and then idle-timed-out). Captures in
`sources/probes/B657-jace9000-serial/`:

1. **Menu prompt.** A bare `CR` elicits `Enter Choice :` (main-menu selection prompt) — the shell is idle
   until prompted (`01-menu-prompt.txt`).
2. **Idle timeout → forced re-auth.** Sending a menu choice after the session went idle returned
   `Timed out waiting for input, please re-authenticate to continue` / `Press ENTER to continue`
   (`02-idle-timeout-reauth.txt`). The ATLAS shell enforces an inactivity timeout — a real security
   property (an unattended DEBUG cable does not stay logged in).
3. **Logout path.** `CR` on that screen → `Logging out current user`, an ANSI screen-clear
   (`ESC[2J ESC[2H`), then `login :` (username-first) (`03-logout-to-login.txt`).
4. **System Diagnostic submenu (option 4).** The operator captured the full **ATLAS System Diagnostic
   Menu** (`04-atlas-system-diagnostic-menu.png`) — **8 read-only options** plus exit:
   `1 Display CPU Usage (Process) · 2 Display CPU Usage (Thread) · 3 Display System Log (Current) ·
   4 Display System Log (All) · 5 Trace Route To Host · 6 Display ARP Table · 7 Display Niagara Daemon
   Threads · 8 Display USB/CDC Port Info · X Exit`. Every entry is `Display…`/`Trace…` — a genuinely
   read-only diagnostics surface (the safe place to gather CPU/log/daemon/ARP/USB telemetry). Their
   per-option OUTPUT (actual numbers) is not yet captured → gap J9K-2.
5. **Live hostname** = `atlashost` (a default-looking hostname; identifier, not a secret).

## §657.5 — What this bootstrap does NOT resolve (open gaps)

- **J9K-2** — the actual OUTPUT of each System Diagnostic option (CPU %, current/all system log, Niagara
  daemon thread dump, ARP table, USB/CDC info). Menu enumerated live; content pending an operator paste.
- **J9K-3** — whether anything is exposed **pre-login** (the post-login banner is doc'd; the pre-auth state
  is live-only).
- **J9K-4** — the JACE-9000 **Boot Options** menu (ESC at boot): the doc shows only **2** options
  (`1 Reset platform credentials / 2 Continue with boot`) vs the JACE-8000's 8-option menu, and a Platform
  Access Recovery screen printing `Host id` + a Tridium-issued `Token`
  (`…/J9Startup/J9ResetPlatformCredentials.txt:76-79,84,91`) — doc-answerable next.
- **J9K-10** — a clean re-capture of THIS firmware's **main-menu numbering** (see §657.3), to replace the
  doc example with the live map.

## §657.6 — Security posture (platform)

`[CERT-doc]` The JACE-9000 platform daemon is **TLS-only, and that is not configurable**: "State: **TLS
only**. This is the **required, and only, option for the JACE-9000**." and "Daemon HTTPS Port: **5011**. This
is the required setting for the JACE-9000." — on the JACE-8000 both are changeable/downgradable
(`…/StationSecurity/ConfiguringSecurePlatformCommunication.txt:35,37`). `[CERT-web]` The datasheet adds
FIPS 140-2, TLS 1.3, secure boot, and encryption of all communications by default. `[CERT-doc]` Factory
state: IPv4 `192.168.1.140`, LAN2 disabled, daemon on HTTPS `5011`
(`…/J9Startup/Factory-shippedState-3468FA83.txt:18,20`); factory account/passphrase are documented public
defaults (values not reproduced — SECRETS DISCIPLINE).

**Operator action:** the `admin1` platform credential + password were pasted into chat → the transcript is
an exfil surface; treat it as compromised and **rotate it** (METHODOLOGY §772).

## Self-verify

| # | Claim | Marker | Citation | Checked |
|---|---|---|---|---|
| 1 | COM5/115200/8N1 = USB-C DEBUG port | [CERT-doc] | ShutdownAndDebug-0B4C4735.txt:40,47 | ✓ grep |
| 2 | 115200 8N1 repeated in startup guide | [CERT-doc] | ConnectingToTheControllerDebugSyste-A6400BFD.txt:46,50 | ✓ grep |
| 3 | Port speaks "ATLAS System Shell" + 9-option menu | [CERT-doc] | CreatingManualBackup-34333AD3.txt:29,47-56 | ✓ grep |
| 4 | Login requires admin-level platform credentials | [CERT-doc] | ShutdownAndDebug-0B4C4735.txt:49 | ✓ grep |
| 5 | Platform service class extends Npsdk (not QNX) | [CERT] | BSystemPlatformServiceAtlas.java:17-18 | ✓ grep |
| 6 | TLS-only :5011 required for JACE-9000 | [CERT-doc] | ConfiguringSecurePlatformCommunication.txt:35,37 | ✓ grep |
| 7 | Factory IP 192.168.1.140 / HTTPS 5011 | [CERT-doc] | Factory-shippedState-3468FA83.txt:18,20 | ✓ grep |
| 8 | Boot Options = 2 options + Host id/Token | [CERT-doc] | J9ResetPlatformCredentials.txt:76-79,84,91 | ✓ grep |
| 9 | SoC i.MX8M Plus quad-core / OS Linux / 2GB / 8GB / FIPS | [CERT-web] | Tridium datasheet+FAQ; Stromquist (SOURCES.md) | ✓ 2 sources |
| 10 | Idle timeout forces re-auth; logout → login: | [CERT-live] | probes/02,03 | ✓ live |
| 11 | System Diagnostic Menu = 8 read-only options | [CERT-live] | probes/04 (operator screenshot) | ✓ live |
| 12 | Live main-menu numbering ≠ doc (opt reached Network Config) | [CERT-live] | probes/05 (operator screenshot) | ✓ live |
| 13 | JACE-8000 = QNX/AM335x (contrast) | [CERT] | [Block 459] §459.1-2 | ✓ corpus |

**Marker tally:** [CERT-doc]=6, [CERT]=2, [CERT-web]=1, [CERT-live]=3, [INFER]=0 · ratio [INFER]/[CERT]=0 ·
**block type = EVIDENCE (bootstrap, multi-source)**. No unmarked claims. SECRETS DISCIPLINE: no secret VALUES
captured; live hostname/Host-ID prefix are identifiers, not credentials.

## Connections

- **Sibling focus jace8000** — [Block 459] (QNX/ARM architecture, the contrast), [Block 460] (platform
  daemon :3011/:5011), [Block 461] (station SCRAM — the layer the serial shell does NOT reach), [Block 463]
  (recovery routes; J9 boot menu differs → J9K-4), [Block 466] (System Passphrase — doc says it applies to
  both), [Block 467] (Host ID — J9 uses `ATLAS-SD-…`, not `Qnx-TITAN-…`).
- **RS-485 field bus** — [Block 448]/[Block 449]: 115200-baud RS-485 field I/O over COM1/COM2 is a SEPARATE
  port from the USB-C DEBUG console; do not confuse the two.
- **Security-audit / signing-pki** — [Block 398] (live posture checklist), [Block 392] (PKI): the JACE-9000
  TLS-only + FIPS posture is the hardened end of that spectrum.

## Open gaps (registered in RESEARCH-STATE-jace9000.md)

J9K-2 (diagnostic outputs, live), J9K-3 (pre-login exposure, live), J9K-4 (boot menu, doc), J9K-5 (shell
activation, doc), J9K-6 (non-admin account, doc), J9K-7 (SSH path, doc), J9K-9 (passphrase-on-serial, live),
J9K-10 (live main-menu numbering), J9K-11 (microSD→Host ID, doc), J9K-12 (COM1/COM2 vs DEBUG, doc).
J9K-1 (OS=Linux) and J9K-8 (SoC=i.MX8M Plus) closed here via `[CERT-web]`.
