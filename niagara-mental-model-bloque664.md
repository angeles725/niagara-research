# B664 — jace9000 focus synthesis: the read-only serial-console mental model of a JACE-9000 (ATLAS System Shell over USB-C DEBUG), consolidated — and the four live-gated gaps left for the operator (focus jace9000, synthesis / STOP)

> **Focus:** `jace9000` (§16) — **STOP**: doc-investigable surface exhausted (9/13 gaps closed, B657–B663;
> 4 remaining are live-gated). This is the focus-closing synthesis (METHODOLOGY §8 terminal artifact).
> **Phase:** consolidation (DESIGN/synthesis block — a high non-[CERT] ratio is expected and healthy here;
> the primary evidence lives in the cited blocks).
> **Sources:** `[CERT]` corpus [Block 657]–[Block 663] · `[CERT-live]` this session's read-only probes
> (`sources/probes/B657-jace9000-serial/`) · `[CERT-doc]`/`[CERT-web]` as cited in those blocks.

## §664.1 — The mental model in one paragraph

A **JACE-9000** is an **NXP i.MX8M Plus quad-core / Linux** area controller on Tridium's internal **"ATLAS"**
platform — a *different machine* from the QNX/ARM JACE-8000 ([Block 657]). Its front **USB-C DEBUG** port
(what a PC sees as a COM port — COM5 here) speaks the **ATLAS System Shell**: a **menu-driven platform-admin**
interface at **115200 8N1**, gated by **admin-only** platform credentials (there is no lesser account —
[Block 660]). That shell is the *platform* layer (network, time, credentials, backups, reboot); it does
**not** reach the *station* (points, logic — a separate SCRAM/Fox world). The same menu is also reachable
over **SSH**, which is **off by default** ([Block 661]). Beneath normal login sit two **recovery** boots
(ESC → credential reset; SHUT-DOWN button → factory wipe — [Block 658]) and a passphrase-mismatch menu, none
of them read-only. Identity/licensing is anchored to a **Tridium microSD card** (`ATLAS-SD-…` Host ID —
[Block 662]). And COM1/COM2 are a *different* pair of ports — RS-485 field bus, not the console ([Block 663]).

## §664.2 — The three serial interfaces (the confusion map)

| Interface | Connector / place | Speed | Carries | Read-only? |
|---|---|---|---|---|
| **DEBUG** (= COM5 on the PC) | USB-C, front | 115200 8N1 | ATLAS System Shell (platform admin) | only via safe menu options |
| **COM1 / COM2** | 3-pos screw terminal, top | ≤115200 | RS-485 field bus (Modbus etc.) | n/a (field wiring) |
| **SSH** (network) | TCP 22, off by default | — | same ATLAS shell menu | only via safe menu options |

## §664.3 — The read-only playbook (operator runbook)

To use the JACE-9000 serial console **read-only** ([Block 657] §657.3–4, [Block 660]):

1. Connect PuTTY to the DEBUG COM port at **115200, 8, N, 1, flow control None**. Press **Enter** if no
   prompt ([Block 659]).
2. Log in with platform credentials — but know this login is **full admin** ([Block 660]); read-only is your
   discipline, not the account's.
3. **Safe (read) menu path:** main-menu **System Diagnostic Options** → the submenu is entirely `Display…` /
   `Trace…` (CPU, System Log, Niagara Daemon Threads, ARP, USB/CDC) — all read-only ([Block 657] §657.4).
4. **Do NOT touch:** Update Time / Update Network (can drop the LAN) / Change Password / Change Passphrase /
   Create-or-Restore SD Backup / Reboot; and never the ESC Boot-Options menu or the SHUT-DOWN button
   ([Block 658]). ⚠ **This firmware's main-menu numbering does NOT match the doc example** — confirm the
   live numbers before pressing anything ([Block 657] §657.3, gap J9K-10).
5. The session **idle-times-out and forces re-auth** ([Block 657] §657.4) — expect to log in again.

## §664.4 — Security posture (consolidated)

Hardened by design: **TLS-only :5011** platform daemon (not downgradable, unlike JACE-8000), **FIPS 140-2 /
TLS 1.3 / secure boot** ([Block 657]); **SSH/SFTP off by default** with a keep-off caution ([Block 661]);
**idle-timeout re-auth** on the serial shell ([Block 657]); credential recovery requires a **Tridium-signed,
Host-id-bound, 24-hour** authorization ([Block 658]). Weak points are operational, not architectural: the
platform layer has **no least-privilege account** (every login is full control — [Block 660]), and in THIS
deployment the **`admin1` credential was exposed in chat → rotate it** ([Block 657] §657.6). A non-Tridium
microSD card is a **denial-of-service foot-gun**: swap it and Niagara won't run ([Block 662]).

## §664.5 — JACE-9000 vs JACE-8000 (delta)

| Axis | JACE-8000 | JACE-9000 | Block |
|---|---|---|---|
| SoC / OS | TI AM335x Cortex-A8 / **QNX** | NXP i.MX8M Plus quad / **Linux** | [Block 657]/[Block 459] |
| Platform class | `…QnxNpm6xx` | `BSystemPlatformServiceAtlas` (Npsdk) | [Block 657] |
| DEBUG connector | micro-USB | **USB-C** | [Block 657] |
| Platform daemon | :3011/:5011, downgradable | **:5011 TLS-only, fixed** | [Block 657] |
| Boot menu | 8 options | **2 options** | [Block 658]/[Block 463] |
| Host ID | `Qnx-TITAN-…`, fixed, not portable | `ATLAS-SD-…`, **card-portable** | [Block 662]/[Block 467] |
| Recovery crypto | Tridium-signed, 24h, keeps data | **same model** | [Block 658]/[Block 463] |

## §664.6 — The four live-gated gaps (next session, operator-driven)

These need the operator's serial session (paste, or the usbipd bridge — `CONECTAR.bat` → `/dev/ttyUSB0`),
because no on-disk source answers them:

- **J9K-2** — the OUTPUT of each System Diagnostic option (CPU %, System Log incl. the live kernel string,
  Niagara Daemon Threads, ARP, USB/CDC). Menu enumerated live ([Block 657]); contents pending.
- **J9K-3** — whether anything is exposed **pre-login** (the post-login banner is known; pre-auth is unseen).
- **J9K-9** — whether the **system passphrase** gates each serial login, or only install/copy ([Block 466]).
- **J9K-10** — **this firmware's exact main-menu numbering** (the live map that replaces the doc example).

Fastest closes: paste (a) the full main menu → J9K-10, and (b) System Diagnostic → **3 Display System Log
(Current)** and **1 Display CPU Usage** → J9K-2 + a live confirm of the Linux kernel ([Block 657] J9K-1).

## Self-verify

| # | Claim | Marker | Citation |
|---|---|---|---|
| 1 | JACE-9000 = i.MX8M Plus / Linux / ATLAS; DEBUG=ATLAS shell @115200 8N1 | [CERT] | [Block 657] |
| 2 | Boot/recovery surface (2-opt menu, factory wipe) none read-only | [CERT] | [Block 658] |
| 3 | Normal shell always live (press Enter) | [CERT] | [Block 659] |
| 4 | No non-admin platform account | [CERT] | [Block 660] |
| 5 | SSH = same menu, off by default | [CERT] | [Block 661] |
| 6 | Host ID card-based, portable, non-Tridium card = Niagara down | [CERT] | [Block 662] |
| 7 | COM1/COM2 RS-485 field bus ≠ DEBUG console | [CERT] | [Block 663] |
| 8 | Live: idle-timeout re-auth; Diagnostic submenu = 8 read-only opts | [CERT-live] | probes/02,04 |

**Marker tally:** [CERT]=7 (corpus), [CERT-live]=1, [INFER]=0 new · **block type = SYNTHESIS** (consolidation;
ratio not an exhaustion signal — the focus STOP is driven by investigable_open=0, not by this ratio). All
substantive evidence is carried by the cited blocks.

## Connections

Consolidates [Block 657]–[Block 663] (this focus). Siblings: jace8000 [Block 459]–[Block 469] (QNX
predecessor); RS-485 field bus [Block 448]/[Block 449]; System Passphrase [Block 466]; security/PKI
[Block 398]/[Block 392].

## STOP declaration

**jace9000 STOPPED** — investigable_open=0. 8 blocks (B657 bootstrap → B664 synthesis), 9/13 gaps closed
(doc + web + live), 4 live-gated child gaps registered for an operator-driven session. Tools: none installed
(pure doc/web/live-read focus). §18 self-retrospective: pending (delegated). Remote: push at focus-close.
