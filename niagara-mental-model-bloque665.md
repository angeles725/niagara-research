# B665 — JACE-9000 live serial session (§12): the ATLAS shell runs on Ubuntu Core Linux 5.4 on an i.MX8M Plus, the serial login takes user+password only (no passphrase), the live main-menu numbering MATCHES the doc, and this unit runs with NO microSD — closing J9K-1/2/9/10 and correcting B657 (focus jace9000, J9K-2/J9K-9/J9K-10, §14→B657)

> **Focus:** `jace9000` (§16). **Gaps closed:** J9K-2 (diagnostic outputs), J9K-9 (passphrase-on-login),
> J9K-10 (live main-menu numbering), and J9K-1 UPGRADED to `[CERT-live]`.
> **Phase:** §12 dynamic — an authenticated, **read-only** serial session on the live JACE-9000, operator-
> authorized. Credential handled out-of-band (scratchpad file, shredded; never in argv/corpus — SECRETS
> DISCIPLINE); only `Display`/`Trace` diagnostic options used; session left **logged out**.
> **Sources:** `[CERT-live]` `sources/probes/B665-jace9000-live-shell/` (banner, menus, `top`, System Log,
> clean logout) · `[CERT]` corpus [Block 657]–[Block 664].
>
> **Bottom line for the operator:** the ATLAS System Shell is the menu front-end of a full **Ubuntu Core
> (snap-based) Linux 5.4** running on the **i.MX8M Plus** ("Honeywell i.MX8MP X8High35 Atlas"), with Niagara
> (`niagarad`) as a Linux process, **full-disk encryption**, and a hardware watchdog. Two facts here CORRECT
> [Block 657]: (1) this unit's Host ID is **`ATLAS-…` (CPU-based, NO microSD)**, not `ATLAS-SD-…`; (2) the
> live main-menu numbering **matches the Tridium doc exactly** (`3 = Ping Host`, `2 = Update Network`).

## §665.1 — The login flow: user + password, NO passphrase (closes J9K-9)

`[CERT-live]` (`probes/02-banner-mainmenu.txt`) the serial authentication is two prompts and no more:

```
login : <platform-user>
Password :
```
→ (successful auth) → screen-clear → banner + menu. **There is no system-passphrase prompt on a normal serial
login.** This closes **J9K-9**: the passphrase gates data-at-rest/decrypt (it appears only on the *System
Decrypt Failure* menu, [Block 658] §658.4, or during install/copy, [Block 466]) — **not** on each serial
login. The serial gate is platform username+password ([Block 660]), full-admin, and the password is **not
echoed**.

## §665.2 — The banner (closes part of J9K-2; corrects J9K-11 assumption)

`[CERT-live]` post-login banner (`probes/02-banner-mainmenu.txt`):

- `hostid: ATLAS-1508-6000-2B0F-A7EA` — **`ATLAS-…` format = CPU-derived Host ID → this unit is running
  WITHOUT a microSD card** ([Block 662] §662.1). §14 **corrects [Block 657] §657.1**, which asserted a live
  `ATLAS-SD-…`; that was the *doc example*, not this unit — the real live prefix is `ATLAS-` (no SD).
- `serial number: 90073556`
- `system time: Wed Dec 3 00:33:17 UTC 2025` — the RTC is **~9 months behind** wall-clock (session was
  2026-08-29). Explained by the System Log (§665.4): `chronyd … ntp.conf not found. Exit`, and both NICs down.
- `niagara daemon port: https 5011` — confirms the TLS-only platform daemon ([Block 657] §657.6) live.
- `en0`/`en1`: both `<NO-CARRIER,BROADCAST,MULTICAST,UP> … state DOWN`, MACs `00:01:f0:98:7e:a9`/`:a8` —
  Linux `ip`-style output; **both Ethernet ports have no link** on this bench unit.

## §665.3 — The live main menu MATCHES the doc (closes J9K-10; §14 corrects B657 §657.3)

`[CERT-live]` (`probes/02-banner-mainmenu.txt`) the main menu is, in order:
`1 Update System Time · 2 Update Network Settings · 3 Ping Host · 4 System Diagnostic Options · 5 Change
Current User Password · 6 Change System Passphrase · 7 Create SD Backup · 8 Restore SD Backup · 9 Reboot ·
L Logout`.

This is **identical to the Tridium doc example** ([Block 657] §657.2). §14 **corrects [Block 657] §657.3**,
which claimed the live numbering did NOT match the doc (`3` reaching Network Config). That claim rested on an
operator screen report; the real capture shows `3 = Ping Host` and `2 = Update Network Settings` — the
earlier "3 → Network Config" was **option 2**, not 3. **The documented read-only map is therefore correct for
this firmware:** the non-mutating options are **`3 Ping Host`** and **`4 System Diagnostic Options`**; all
others mutate.

## §665.4 — What the diagnostics reveal: Ubuntu Core Linux on i.MX8M Plus (closes J9K-1 live + J9K-2)

`[CERT-live]` `4 System Diagnostic Options` → the 8-option all-read-only submenu
(`probes/03-diagnostic-submenu.txt`); then:

**`1 Display CPU Usage` → `top`** (`probes/04-cpu-top-and-syslog.txt`):
- Linux `top`: `138 tasks`, load `0.76 0.82 0.82`, uptime 39 min; **4 CPU cores** (`cpuhp/0..3`,
  `migration/0..3`) → quad-core confirmed live; `MiB Mem: 1667.0 total` (~2 GB LPDDR4), 0 swap.
- Niagara as Linux processes: **`niagarad`** (PID 2903, RES ~112 MB) and **`niagara-wrapper`** (PID 1225),
  all under `systemd` (PID 1).

**`3 Display System Log (Current)`** (`probes/04-cpu-top-and-syslog.txt`):
- `Linux version 5.4.0-1052-shiner (…gcc 9.4.0 Ubuntu…) … Ubuntu 5.4.0-1052.63-shiner 5.4.268` — arm64.
  **Closes J9K-1 with `[CERT-live]`**: the OS is **Ubuntu-based Linux, kernel 5.4** (was `[CERT-web]` only).
- `Machine model: Honeywell i.MX8MP X8High35 Atlas` — live confirmation of the **i.MX8M Plus** SoC and the
  "Atlas" platform ([Block 657] §657.1, J9K-8).
- **Ubuntu Core / snap-based**: kernel cmdline `snapd_recovery_mode=run snapd_system_disk=/dev/mmcblk2
  console=ttynull,115200`; `network-manager.networkmanager` runs as a snap. So ATLAS = Ubuntu Core, not a
  bespoke RTOS.
- `systemd 245 (… +APPARMOR +SELINUX … +SECCOMP …)`; **full-disk encryption** present:
  `system-fde\x2dhelper.slice`, `Reached target Local Encrypted Volumes`, `dm-0`/`dm-1` (device-mapper)
  ext4 volumes with `jbd2` journals.
- Storage: `mmcblk2: mmc2:0001 8GUF4R 7.28 GiB` → **8 GB eMMC**; root on `/dev/mmcblk2`.
- Hardware watchdog: `gpio-wdt watchdog: gpio_wdt has started`; `chronyd … ntp.conf not found. Exit`
  (explains the wrong clock).

This makes the JACE-9000 architecturally a **hardened Ubuntu Core appliance** (secure boot + FDE + AppArmor +
watchdog) hosting the Niagara JVM stack — a very different beast from the JACE-8000's QNX microkernel
([Block 459]).

## §665.5 — Read-only discipline held; session closed

Only `Display`/`Trace` options were used (no mutating key was ever sent); the session was then exited
(`X`) and **logged out** (`L` → `Logging out current user` → `login :`, `probes/05-clean-logout.txt`), and
COM5 was detached back to Windows. The `admin1` credential used to authenticate was handled via a shredded
scratchpad file and appears in no probe, block, or memory — **but it remains exposed in the chat transcript;
rotate it.**

## Self-verify

| # | Claim | Marker | Citation | Checked |
|---|---|---|---|---|
| 1 | Serial login = user+password, NO passphrase prompt | [CERT-live] | probes/02 | ✓ live |
| 2 | Live hostid = ATLAS-… (CPU-based, no microSD) — corrects B657 | [CERT-live] | probes/02 | ✓ live |
| 3 | niagara daemon port https 5011; en0/en1 DOWN | [CERT-live] | probes/02 | ✓ live |
| 4 | Live main menu matches doc (3=Ping, 2=Network) — corrects B657 §657.3 | [CERT-live] | probes/02 | ✓ live |
| 5 | Diagnostic submenu = 8 read-only Display/Trace options | [CERT-live] | probes/03 | ✓ live |
| 6 | top: 4 cores, 1667 MiB, systemd, niagarad PID 2903 | [CERT-live] | probes/04 | ✓ live |
| 7 | Kernel Linux 5.4.0-1052-shiner (Ubuntu 5.4.268) arm64 | [CERT-live] | probes/04 | ✓ live |
| 8 | Machine model "Honeywell i.MX8MP X8High35 Atlas" | [CERT-live] | probes/04 | ✓ live |
| 9 | Ubuntu Core (snapd_recovery_mode, snap NM); FDE (dm-0/1, fde slice) | [CERT-live] | probes/04 | ✓ live |
| 10 | 8 GB eMMC (mmcblk2 8GUF4R 7.28 GiB); gpio watchdog | [CERT-live] | probes/04 | ✓ live |

**Marker tally:** [CERT-live]=10, [CERT]=corpus refs, [INFER]=0 · **block type = EVIDENCE (§12 live)**. No
unmarked claims. Secret scan on the 162 KB capture: clean (no credential/key/token VALUES; the only
`password=` line is an empty wifi-wake variable). Host ID / serial / MAC are hardware identifiers, not
secrets.

## §14 corrections issued (back-pointers added to B657)

1. **[Block 657] §657.1** — live Host ID is `ATLAS-…` (no microSD), NOT `ATLAS-SD-…` (that was the doc
   example; B657's `[CERT-live]` on the SD prefix was unsupported — the diagnostics timed out before a real
   capture). Corrected here.
2. **[Block 657] §657.3** — the live main-menu numbering MATCHES the doc; the "does not match / 3→Network"
   claim was an operator screen mis-report (was option 2). Corrected here.

## Connections

- [Block 657] — bootstrap (corrected here on Host ID + menu numbering); this block is its live capstone.
- [Block 662] — Host ID formats: live confirms the `ATLAS-…` (CPU) branch and that this unit has no card.
- [Block 660] — the login this used is full-admin (no viewer role).
- [Block 459] — JACE-8000 QNX: the architectural contrast to this Ubuntu Core Linux appliance.

## Open gaps (RESEARCH-STATE-jace9000.md)

**All J9K gaps closed (13/13).** No investigable and no blocked-live remain. Focus fully closed; update the
synthesis [Block 664] STOP note to reflect live closure.
