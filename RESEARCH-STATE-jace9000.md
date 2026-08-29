# RESEARCH-STATE — focus: jace9000 (live embedded controller, serial DEBUG console)

> Multi-focus corpus (METHODOLOGY §16). Focus **BOOTSTRAPPED 2026-08-29** — angle: the **JACE-9000 as a
> live embedded controller reached over its USB-C DEBUG serial console** (COM5 @ 115200 8N1, the "ATLAS
> System Shell"), READ-ONLY first. Sibling of the closed `jace8000` focus (B459–B475); the JACE-9000 is a
> DIFFERENT platform ("ATLAS", `BSystemPlatformServiceAtlas extends BSystemPlatformServiceNpsdk` — NOT the
> QNX class the JACE-8000 uses), so jace8000 facts TRANSFER only where re-confirmed.
>
> Subject = a real, commissioned JACE-9000 the operator is physically connected to over COM5 (an external
> FTDI FT232 USB-serial converter, `0403:6001`, bridged to WSL as `/dev/ttyUSB0` via usbipd when the operator
> attaches it). This is a `live-install` target → **SECRETS DISCIPLINE** (cite structure/format, never a
> credential/passphrase/key VALUE; authenticate out-of-band; zero secrets in blocks/sources/engram).
>
> Phase = **§12 dynamic** where live, **DISK-FIRST elsewhere** (the Tridium J9-specific docs answer most of
> the read-only surface without touching the device). Live probes are driver-inline (`no·inline` is the
> COMPLIANT tier for §12 — live credentials are never handed to a sub-agent).
>
> **User request driving the backlog:** "document everything related to COM5/115200/serial on the JACE-9000,
> read-only first." Grounded in the audit sweep of the 96 J9-specific Tridium docs + the jace8000 sibling
> corpus.
>
> **SECURITY / operator action:** the `admin1` platform credential + its password were pasted in chat →
> **ROTATE them** (a credential in the transcript is compromised, §772). The JACE-9000 platform is TLS-only
> on :5011 by requirement (B657); factory-default account/IP are documented public defaults.

<!-- research-state.v1 -->
schema: research-state.v1
method: dynamic-live
block_scope: shared-global
covered_blocks: 657
gaps_closed: 8
known_gaps: 13
investigable_open: 2
requires_execution_open: 0
blocked_open: 3
deferred_open: 0
undocumented_findings: 0
<!-- /research-state.v1 -->

## Coverage / open items

Coverage: **8/13 gaps closed (J9K-0 B657; J9K-1 + J9K-8 via `[CERT-web]`; J9K-4 B658; J9K-5 B659;
J9K-6 B660; J9K-7 B661; J9K-11 B662); investigable_open=2, blocked-live=3**. The DOC-answerable read-only surface can be
driven to close without the device; the 3 blocked gaps (J9K-2 diagnostic OUTPUTS, J9K-3 pre-login exposure,
J9K-9 passphrase-on-serial) need the operator's live serial session (paste or usbipd bridge). LIVE this
session (B657): console reachable, ATLAS System Shell, idle-timeout re-auth, System Diagnostic submenu
enumerated (8 read-only opts), and a §14-style correction — the live main-menu numbering does NOT match the
doc example (opt reached Network Config Utility, not Ping Host) → re-capture as J9K-10.

| Priority | Gap | Detail | Status |
|---|---|---|---|
| high | J9K-0 serial-console identity & architecture | JACE-9000 = ATLAS-platform N4 controller (not QNX/JACE-8000); COM5/115200/8N1 = USB-C DEBUG "ATLAS System Shell" (menu-driven platform admin), distinct from RS-485 field bus COM1/COM2 and from the TCP platform daemon :5011 | **covered B657** |
| high | J9K-1 exact OS name/kernel | **Linux** (Tridium datasheet/FAQ [CERT-web]); ATLAS platform (BSystemPlatformServiceAtlas→Npsdk, not QNX); en0/en1 inet/inet6 live banner corroborates. Exact kernel version = live-only (opt-4 System Log / SSH uname) | **covered B657** (kernel ver = J9K-2) |
| high | J9K-2 System Diagnostic Options (menu opt-4) surface | MENU enumerated live (B657, probes/04): 8 read-only opts — CPU Usage (Process/Thread), System Log (Current/All), Trace Route, ARP Table, Niagara Daemon Threads, USB/CDC Port Info. Per-option OUTPUT (actual CPU%/log/threads) still pending an operator paste | pending · blocked-live (content) |
| medium | J9K-3 pre-login vs post-login banner | post-login banner fields doc'd (hostid, daemon port, en0 inet — B657); does ANYTHING surface pre-auth? | pending (doc partial; pre-login state blocked-live) |
| high | J9K-4 boot-options & recovery surface (ESC at boot) | 2-option Boot menu; Platform Access Recovery (Tridium-signed Reset Auth Key, 24h token, Host-id-bound, KEEPS data); SHUT-DOWN-button factory wipe (deletes all data); System Decrypt Failure menu (4 opts). None read-only. §14 contrast to B463 (J8=8 opts, same crypto model) | **covered B658** |
| medium | J9K-5 DEBUG shell activation | normal serial login is always live (connect + press Enter, ConnectingToTheControllerDebugSyste:71); "special power-up mode" = WB-over-USB commissioning + the ESC/SHUTDOWN recovery boots, NOT day-to-day login (one [INFER] reconciliation → live-confirm) | **covered B659** |
| medium | J9K-6 read-only / non-admin platform account over serial | NO non-admin/viewer platform role exists: all platform users have "full equal privileges" (Commissioning:52); platform daemon = highest-level access. Serial read-only-ness = menu choice, not a limited account. Station RBAC is a separate layer | **covered B660** |
| medium | J9K-7 SSH as alternative shell path | SSH delivers the SAME menu shell (not raw OS), platform login still required (AA1F5AC0:25-26); SFTP/SSH disabled by default, TCP 22, standing caution to keep off. Whether ON on THIS unit = live | **covered B661** |
| medium | J9K-8 SoC/CPU spec | **NXP i.MX8M Plus, quad-core (ARM Cortex-A53)** — Tridium datasheet/FAQ [CERT-web] (vs JACE-8000 TI AM335x Cortex-A8, B459); 2GB LPDDR4 / 8GB microSD | **covered B657** |
| medium | J9K-9 System Passphrase prompt on serial login | does factory passphrase gate every serial login, or only install/copy? (B466 remittance) | pending · blocked-live |
| high | J9K-10 live main-menu numbering & read-only map | LIVE (B657): the doc example's per-number map is FIRMWARE-DEPENDENT — the key doc'd as `3 Ping Host` reached the Network Config Utility (mutating) on this unit; `4 → System Diagnostic` matched. Re-capture THIS firmware's exact main menu (operator paste) to replace the doc example. Diagnostic submenu = confirmed all-read-only | pending · needs operator paste of main menu |
| medium | J9K-11 microSD → Host ID dependency | ATLAS-SD-… (card-derived, Tridium-secret+CID validated at boot) vs ATLAS-… (CPU ID); card ties license to ID → PORTABLE between JACE-9000s; insert/remove CHANGES Host ID; non-Tridium card → Niagara won't run. Inverts B467 (J8 non-portable) | **covered B662** |
| low | J9K-12 RS-485 field ports (COM1/COM2) vs DEBUG | confirm COM1/COM2 RS-485 field bus is entirely separate from the USB-C DEBUG console | pending · investigable (doc) |

## REMITTANCES (answered by the closed jace8000 corpus — re-confirm only J9 deltas)

| Subject | jace8000 block | J9 delta to re-confirm |
|---|---|---|
| System Passphrase (two at-rest encryption domains) | B466 | doc says passphrase "applies to JACE-8000 and JACE-9000" — transfers; serial passphrase prompt = J9K-9 |
| Host ID hardware-binding & licensing | B467 | J9 format differs: ATLAS-SD-… / ATLAS-… (not Qnx-TITAN-…) → J9K-11 |
| Platform daemon (TCP :3011/:5011, 403-to-GET) | B460 | J9 = TLS-only :5011 by REQUIREMENT (not configurable) — B657 §; serial console is a SEPARATE transport |
| Station access (SCRAM, bajaux navigator) | B461 | serial shell does NOT reach the station layer (platform-only) |
| Recovery routes | B463 | J9 boot menu = 2 options (not 8) → J9K-4 |
| RS-485 field I/O bus @115200 | B448/B449 | COM1/COM2 field bus ≠ USB-C DEBUG console → J9K-12 |

## Iteration history

| Block | Gap | Delegated? · tier | Notes |
|---|---|---|---|
| B657 | J9K-0 | yes·sonnet (doc audit sweep) + inline verify | bootstrap: ATLAS platform; USB-C DEBUG "ATLAS System Shell" @115200 8N1; menu + read-only safety map; §-remittances to jace8000 |
| B658 | J9K-4 | no·inline (doc) | boot/recovery serial surface: 2-opt Boot menu, Platform Access Recovery (Tridium-signed, keeps data), SHUT-DOWN factory wipe, decrypt-failure menu; §14 contrast B463 |
| B659 | J9K-5 | yes·sonnet (doc sweep) + inline verify | shell activation: normal login always live (press Enter); "special power-up mode" = commissioning/recovery, not daily login; 1 [INFER] flagged |
| B660 | J9K-6 | no·inline (doc, sweep material) | platform accounts: all "full equal privileges", no non-admin/viewer role; read-only = menu choice not account; sharpens admin1 exposure |
| B661 | J9K-7 | no·inline (doc, sweep material) | SSH = same menu shell (not raw OS), platform login required; SFTP/SSH off by default TCP 22 + keep-off caution; matches B468 |
| B662 | J9K-11 | no·inline (doc, sweep material) | Host ID dual format ATLAS-SD-…/ATLAS-…; Tridium-secret+CID boot validation; card = portable license; non-Tridium card → Niagara down; inverts B467 |

## Next — LIVE-gated gaps (need the operator's serial session)

J9K-1, J9K-2, J9K-8, J9K-9 need the live device. Two capture routes: (A) operator pastes the serial output
from a Windows terminal (PuTTY/TeraTerm 115200 8N1); (B) operator bridges the FTDI to WSL via usbipd
(`BIND-una-vez-ADMIN.bat` then `CONECTAR.bat` → `/dev/ttyUSB0`) and the driver reads it read-only at 115200
8N1. Investigable-from-docs set (J9K-3/4/5/6/7/10/11/12) can be driven to close without the device.
