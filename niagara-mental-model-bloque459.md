# B459 — JACE-8000 architecture: a QNX/ARM embedded controller, not Linux or Windows (focus jace8000 bootstrap, J1)

> **Focus:** `jace8000` (new, §16) — the JACE-8000 as a **live embedded controller**. Bootstrap block.
> **Gap:** J1 — what architecture is the JACE-8000? Is it Linux, Windows, or something else?
> **Phase:** §12 dynamic (live device at `192.168.1.140`, `live-install` → SECRETS DISCIPLINE). Read-only.
> **Sources:** `[CERT-doc]` niagara-help (`guides-clean/NiagaraIT`, `.../Platform`, `.../BackupRestore`) ·
> `[CERT]` corpus [Block 382] [Block 385] [Block 424] · `[CERT-live]` probes against the live JACE this
> session · `[CERT-web]`/`[INFER]` public JACE-8000 hardware specs (datasheet not yet preserved — J1-follow).
>
> **Bottom line for the operator:** the JACE-8000 is **not a PC**. It is an embedded controller running the
> **QNX Neutrino real-time OS on an ARM Cortex-A8 SoC**, with the Niagara runtime hosted on an **Oracle
> HotSpot JVM**. Everything downstream — how you reach its filesystem, how the platform daemon behaves, how
> you recover it — follows from "QNX embedded appliance", not "Windows/Linux server".

## §459.1 — The direct answer: QNX, not Linux and not Windows

Tridium's own networking guide draws the line explicitly between the two host families in a Niagara system:

- `[CERT-doc]` "a PC running a Win32 or Win64–based operating system **and a remote controller running the
  QNX operating system**." — `guides-clean/NiagaraIT/HostNetworkingTechnologies-C84DAFC7.txt:13`
- `[CERT-doc]` supervisor/engineering host = "A PC or laptop running **Microsoft Windows, Linux, or
  Solaris** using the HotSpot JVM." — `...HostNetworkingTechnologies-C84DAFC7.txt:24`
- `[CERT-doc]` controller = "**An embedded JACE controller using the QNX operating system and the Oracle
  Hotspot Java Virtual Machine (JVM)**." — `...HostNetworkingTechnologies-C84DAFC7.txt:26`

So the two boxes in a Niagara deployment run different operating systems:

| Role | Host | OS | JVM |
|---|---|---|---|
| Supervisor / engineering (Workbench) | PC/laptop/server | Windows / Linux / Solaris | Oracle HotSpot |
| **Controller (the JACE-8000)** | **embedded appliance** | **QNX Neutrino (RTOS)** | **Oracle HotSpot** |

QNX Neutrino is a commercial POSIX microkernel real-time OS (BlackBerry/QNX Software Systems). It is neither
Linux nor Windows: it is closer to a POSIX Unix in API but is a proprietary RTOS with a microkernel design.
That single fact reframes every later question in this focus.

## §459.2 — The compute module: NPM6xx (ARM)

The platform service class name in the docs encodes the module and OS generation:

- `[CERT-doc]` "System Platform Service (**platform-SystemPlatformServiceQnxNpm6xx**) — This controller
  component is the **QNX implementation of SystemPlatformService in a station running on the JACE
  controller**." — `guides-clean/Platform/platform-SystemPlatformServiceQnxNpm6xx.txt:11-12`

`Npm6xx` = the **NPM (Niagara Powered Module) 6xx** compute board that the JACE-8000 is built around. The
`Qnx` prefix in that same class name is the platform-service variant selector: the framework ships a `Qnx`
implementation and a separate Windows implementation of the same platform services, chosen by host OS. (The
sibling `platform-NtpPlatformServiceEditorQnx` view confirms a whole family of QNX-only platform-service
editors — `guides-clean/Platform/AboutTheNtpPlatformServiceEditorQnx-3009090D.txt:1`.)

**Hardware specifics** (`[INFER]` / `[CERT-web]` — public JACE-8000 / WEB-8000 datasheet; not yet preserved,
see J1-follow):
- SoC: **TI Sitara AM335x** family (ARM Cortex-A8, ~1 GHz). The factory unit advertises its NIC OUI as
  "Texas Instruments" on a LAN scan — consistent with the AM335x MAC — which is how a factory JACE is located
  on DHCP.
- ~1 GB DDR3 RAM · onboard flash (eMMC) + a user **microSD** card holding the station · a small
  **battery-backed NVSRAM** for the real-time clock and crash-safe state.
- Front panel: a **micro-USB "DEBUG"** serial console port, a **USB backup/restore** host port, a
  **BACKUP/RESTORE** button, dual Ethernet (PRI/SEC), RS-232/485 serial, and an option WiFi module.

These belong to a dedicated hardware block once the datasheet is preserved; here they only frame the OS answer.

## §459.3 — The software stack, top to bottom

```
   Niagara station (config.bog, modules)         <- your engineering
   ───────────────────────────────────────
   Niagara runtime (baja / nre)                  <- Java framework
   Oracle HotSpot JVM (ARM build)     [CERT-doc]
   ───────────────────────────────────────
   niagarad platform daemon           [CERT-live] :3011/:5011
   ───────────────────────────────────────
   QNX Neutrino RTOS (microkernel)    [CERT-doc]  ARM Cortex-A8
   ───────────────────────────────────────
   NPM6xx board (TI Sitara AM335x)    [CERT-doc/web]
```

This is corroborated from the **binary** side by the corpus's native RE, which is why the JACE differs from
the Windows supervisor that `platform-native` decompiled:

- `[CERT]` [Block 382] — `libciper.so` (a JACE platform native) is a **QNX-ARM ELF with DWARF** implementing
  the Sylk master/slave file-transfer protocol. It only exists/runs on the embedded QNX side.
- `[CERT]` [Block 385] — several `nre.dll` natives (`executeNativeDiagnosticsCommand0`, `addUserAccount0`,
  `getSystemPassword0`) are **return-0 STUBS on the Windows supervisor** — they are live only on the
  **embedded JACE/QNX** host. The OS split is visible at the JNI boundary: platform operations like OS user
  management and system-password handling are QNX-native, absent on Windows.
- `[CERT]` [Block 424] — `getHostId` on the Windows supervisor is a non-crypto fold-XOR over volume serial +
  registry owner. The JACE derives its Host ID from **hardware** instead (J10) — a difference that only makes
  sense because the JACE is a distinct hardware appliance, not a PC install.

## §459.4 — Live surface measured this session (`[CERT-live]`)

Read-only probes against `192.168.1.140` (SECRETS DISCIPLINE: structure only; the `admin` password was used
out-of-band from scratchpad and never recorded):

| Port | State | Finding |
|---|---|---|
| 22 (SSH) | **closed** | no SSH — QNX shell is not exposed over the network (JACE hardening default) |
| 23 (telnet) | closed | no telnet |
| 80 (HTTP) | open | station web (redirects to TLS) |
| 443 (HTTPS) | open | station web — **SCRAM-SHA-256 login CONFIRMED live** with `admin` (`api-access` B457 tool) |
| 1911 (Fox plaintext) | closed | plaintext Fox disabled (good posture) |
| 3011 (platform HTTP) | open | niagarad platform daemon — **GET → 403 Forbidden** (platform protocol only) |
| 4911 (Fox TLS) | open | Fox over TLS |
| 5011 (platform TLS) | open | niagarad platform daemon over TLS — **GET → 403 Forbidden** |

Further live facts:
- `[CERT-live]` **oBIX is NOT enabled** here: `/obix/`, `/obix/about` → HTTP 404 after a successful login
  (the `obix` module is not running on this JACE — unlike the station in B457/B458). Station data access must
  go through Fox / web servlets, not oBIX (J4).
- `[CERT-live]` **TLS identities:** `:5011` presents the **default Tridium platform cert**
  (`CN=Niagara4, O=Tridium, C=US`, valid 2021-01-11 → **2022-01-11, expired**). `:443` and `:4911` present a
  self-signed `CN=Niagara4, **O=ForRecoveryPurposes**, C=US` cert **freshly generated 2026-08-19** (valid 1
  year) — the default N4 recovery cert, and its today's date means the station was (re)commissioned recently.
  `ForRecoveryPurposes` as the live TLS anchor is the known weak-default already tracked in the security
  focus ([Block 398] SEC checklist).

The two 403s are the load-bearing live finding for the next gaps: **the platform daemon does not answer a
plain HTTP GET** — it speaks the Niagara platform protocol with its own auth handshake. You cannot "browse"
the platform; entering it (J3) and scripting it without Workbench (J8) is a protocol problem, not a URL.

## §459.5 — Why the architecture answer governs the rest of the focus

- **Filesystem (J2/J5):** it is a **QNX** tree (POSIX paths, `/mnt`-style flash mounts), reached either
  through the platform daemon's File Transfer or the QNX serial console — not a Windows share, not SSH.
- **Platform entry (J3/J8):** the daemon is a QNX process answering a binary/HTTP-hybrid platform protocol;
  RE to pull a `.bog` without Workbench means porting that protocol, exactly as `api-access` ported the SCRAM
  web login. The 403-to-GET proves a naive HTTP client won't work.
- **Recovery (J7):** because it is an appliance, recovery has **hardware** paths a PC lacks — the front
  BACKUP/RESTORE button and USB **clone backup**, which the docs say back up "the entire platform and station
  … **without requiring the backup functions of Workbench**" (`guides-clean/BackupRestore/
  JACE-8000USBBackaupAndRestoreFeatur.txt:12-13`). That is a recovery route that needs **no platform login at
  all** — the seed of J7.
- **Licensing (J10):** hardware-bound Host ID (not the Windows fold-XOR) is why you cannot simply clone a
  station onto a different JACE and expect it to license.

## Self-verify

| # | Claim | Marker | Citation | Checked |
|---|---|---|---|---|
| 1 | JACE controller runs the QNX operating system | [CERT-doc] | HostNetworkingTechnologies:13,26 | ✓ token "QNX operating system" present |
| 2 | Supervisor/PC runs Windows/Linux/Solaris (contrast) | [CERT-doc] | HostNetworkingTechnologies:24 | ✓ |
| 3 | JACE uses Oracle HotSpot JVM | [CERT-doc] | HostNetworkingTechnologies:26 | ✓ |
| 4 | Compute module = NPM6xx; QNX platform-service impl | [CERT-doc] | SystemPlatformServiceQnxNpm6xx:11-12 | ✓ |
| 5 | libciper.so is a QNX-ARM native (embedded-only) | [CERT] | [Block 382] | ✓ corpus |
| 6 | Some nre natives are QNX-live / Windows-stub | [CERT] | [Block 385] | ✓ corpus |
| 7 | SCRAM-SHA-256 login works live on :443 | [CERT-live] | this session | ✓ "authenticated (scram-sha256)" |
| 8 | oBIX disabled (404 after login) | [CERT-live] | this session | ✓ |
| 9 | Platform daemon :3011/:5011 GET → 403 | [CERT-live] | this session | ✓ |
| 10 | :5011 default Tridium cert expired 2022; :443/:4911 ForRecoveryPurposes 2026-08-19 | [CERT-live] | this session | ✓ openssl x509 |
| 11 | SoC = TI Sitara AM335x, RAM/flash/microSD specifics | [INFER]/[CERT-web] | public datasheet (not preserved) | ⚠ marked non-CERT; J1-follow |
| 12 | USB clone backup works "without Workbench" | [CERT-doc] | JACE-8000USBBackaupAndRestoreFeatur:12-13 | ✓ |

Marker tally: [CERT-doc] ×6 · [CERT] ×3 (corpus) · [CERT-live] ×5 · [CERT-web]/[INFER] ×1 (isolated to
hardware specifics, explicitly flagged). [INFER]/[CERT] ratio ≈ 0.07. **Block type: EVIDENCE + bootstrap
framing** — low ratio expected; the one [INFER] is quarantined to un-preserved datasheet specifics and opens
a follow-up (J1-follow: preserve the JACE-8000 datasheet), it is not load-bearing for the architecture answer.

## Connections

- **[Block 382] / [Block 385] / [Block 424]** (`platform-native`) — the binary-side evidence that the JACE is
  QNX/ARM and differs from the Windows supervisor. This focus is the LIVE, appliance-facing complement.
- **[Block 457] / [Block 458]** (`api-access`) — the SCRAM login + oBIX tooling reused here; note oBIX is off
  on THIS JACE (J4 divergence).
- **[Block 398]** (`security-audit`) — `ForRecoveryPurposes` default cert as a live weak-default.

## Open gaps (queued this bootstrap)

J2 (QNX fs layout & boot) · J3 (platform daemon) · J4 (accessing the station) · J5 (entering the filesystem)
· J6 (platform auth / passphrase) · J7 (recovery without platform) · J8 (platform-protocol RE → .bog without
Workbench) · J9 (backup/dist/cloning) · J10 (Host ID & licensing) · J11 (live security posture).
**J1-follow:** preserve the JACE-8000 / WEB-8000 hardware datasheet to promote §459.2 specifics to [CERT-doc].
