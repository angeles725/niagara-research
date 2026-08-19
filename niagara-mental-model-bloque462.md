# B462 — Entering the JACE-8000 filesystem: the QNX tree (/opt/niagara, /home/niagara) and its four access routes, each with a different privilege and scope (focus jace8000, J5)

> **Focus:** `jace8000` (§16). **Gap:** J5 — how do you get into the JACE's filesystem, where does the
> station live on disk, and what does each route reach?
> **Phase:** §12 dynamic (live at `192.168.1.140`) + `[CERT-doc]`. Read-only. `live-install` → SECRETS DISCIPLINE.
> **Sources:** `[CERT-doc]` niagara-help (`AXtoN4Migration/ControllerPlatformHomes`, `Platform/aFileTransferClient`,
> `Platform/aPlatformSystemPassword`, `Edge10Startup/SystemShellEdge10Startup`) · `[CERT-live]` `/file` probe ·
> `[CERT]` corpus [Block 187] (px-menu file space), [Block 459]/[Block 460]/[Block 461] (this focus).
>
> **Bottom line:** the JACE runs a **QNX filesystem** with two Niagara roots — **`/opt/niagara`** (System
> Home, the install) and **`/home/niagara`** (daemon User Home, which "contains configuration data and the
> installed and running station", i.e. `config.bog`). There are **four ways in**, and they do NOT reach the
> same things: the **platform File Transfer Client** (needs a platform login) reaches the *whole* tree at
> daemon privilege; the **station `/file` space** (station login) reaches only the station's own file
> directories; the **serial QNX system shell** (micro-USB Debug port) is a *limited recovery menu*, not a
> root prompt; and **SSH is disabled** here. That asymmetry is exactly why losing platform access blocks a
> network `.bog` grab and pushes you to hardware recovery (J7).

## §462.1 — The QNX filesystem layout (where things live)

`[CERT-doc]` `guides-clean/AXtoN4Migration/ControllerPlatformHomes.txt`:

| Niagara alias | QNX path | Contents | line |
|---|---|---|---|
| **System Home** (`niagara_home`) | **`/opt/niagara`** | the Niagara install: runtime (`nre`), modules, `bin`, JRE | :22,:47-48 |
| **daemon User Home** (`niagara_user_home`) | **`/home/niagara`** | "**configuration data and the installed and running station**" | :24-25,:57-58 |

- `[CERT-doc]` "The actual location of the System Home folder for a controller is: **/opt/niagara**." (:22)
- `[CERT-doc]` "the daemon User Home … is: **/home/niagara** … Contains configuration data and the installed
  and running station." (:25,:58)
- In Workbench the System Home is browsed under **Platform > Remote File System** (:22).

So the station database you would want to copy lives under `/home/niagara/stations/<StationName>/config.bog`
(the `stations/` convention; the exact station name is read from the running station — [Block 461]). The
per-user/daemon config sits in `/home/niagara/etc/` (referenced in the docs as `~etc`, e.g.
`~etc/system.properties`).

## §462.2 — Route 1: platform File Transfer Client (whole tree, daemon privilege)

`[CERT-doc]` `Platform/aFileTransferClient.txt`:
- "The File Transfer Client allows you to **copy files and/or folders in both directions** between your
  Workbench PC and a remote platform. You can also use it to **delete** files and folders." (:12-13)
- Example: copy `~etc/system.properties` off the controller to edit, then copy it back (:19).
- **Caveat:** do not use it to copy *modules* — "runtime profile types are not applied, nor are module
  dependencies … Always use the platform **Software Manager**" (:23-24). No Undo (:21-22).

This is the **network route to pull `config.bog`** — but it lives behind the **platform daemon login**
([Block 460]): no platform credentials → no File Transfer. This is the single most important constraint for
J7/J8: the clean, whole-filesystem network route is platform-gated.

## §462.3 — Route 2: the station `/file` space (station login, station files only)

- `[CERT-live]` `/file/` and `/file/^` → **HTTP 403** as `admin` — the file servlet is present but a bare
  **directory listing is gated**; access is per-file within an allowed root, not a browse-the-disk grant.
- `[CERT]` [Block 187] (px-menu) already mapped the station file space: the ORD `file:` scheme with roots
  **`^` = the station's `shared/`** and **`!` = the station home**, served over HTTP `/file/…` and resolvable
  in PX/ORD. Scope is the **station's own file directories** (under `/home/niagara/stations/<name>/`), NOT the
  OS tree — you cannot reach `/opt/niagara` or another user's files this way.

So the station channel gives you *station files* (PX graphics, `shared/`, station-scoped config) with station
credentials, but not the platform-level filesystem. Directory enumeration is denied; you must know the path.

## §462.4 — Route 3: the serial QNX system shell (micro-USB Debug port, limited menu)

`[CERT-doc]` `Edge10Startup/SystemShellEdge10Startup-AF36144A.txt` (the QNX system-shell family; the JACE-8000
exposes the same via its front **micro-USB DEBUG** port):
- "Any **QNX-based** … device has a '**system shell**,' providing **low-level access to a few basic platform
  settings**. Using a **special power-up mode** and a serial connection via a **USB-to-MicroUSB cable
  connected to the Debug port**, you can access this system shell." (:12-13)
- "system shell is also available via **SSH** (providing that SSH …" is enabled) (:13) — **not here**: SSH is
  closed ([Block 459]).
- Typical use: **troubleshooting** and **IP-address recovery** — "in the case of IP address misconfiguration,
  you can use the serial system shell … to set the Edge device's IP address." (:20,:23)

Crucially this is a **menu of a few settings** (network settings, ping, reboot, platform-daemon control,
update TCP/IP), **not an arbitrary QNX root shell** — you cannot `cat config.bog` from it. It is a *recovery*
console, reachable with physical access and **no network and no platform login**, which is why it belongs to
J7 (recovery) rather than to general file access.

## §462.5 — Route 4: SSH — disabled

`[CERT-live]` TCP/22 **closed** ([Block 459]). On QNX Edge/JACE devices SSH, when enabled, would also front
the system shell — but Tridium ships it **off** by default and it is off here. So no SSH filesystem route.

## §462.6 — The passphrase gate on any copy (forward to J6/J8)

`[CERT-doc]` `Platform/aPlatformSystemPassword.txt:52` — the "Workbench platform tools (**Station Copier,
File Transfer Client or Backup**) … **convert files to use the correct encryption key** for the target." So a
`config.bog` moved off the JACE is **not** plaintext for its protected fields: the **System Passphrase**
encrypts sensitive values at rest, and the platform tools re-encrypt on transfer. A raw byte-copy of
`config.bog` therefore carries **passphrase-encrypted secrets** you cannot read without the passphrase — a
central fact for J6 (at-rest secrets) and J8 (what a stolen `.bog` actually yields).

## §462.7 — Route comparison

| Route | Credential | Reaches | Enumerate dirs? | Gets `config.bog`? |
|---|---|---|---|---|
| Platform File Transfer (:5011) | **platform** login | whole QNX tree (`/opt/niagara`, `/home/niagara`) | yes | **yes** (secrets passphrase-encrypted) |
| Station `/file` space (:443) | **station** login | station file dirs only (`^`/`!`) | no (403) | no (only station files, path-known) |
| Serial system shell (USB Debug) | physical (+ some menu items gated) | a few platform settings (menu) | no | no |
| SSH (:22) | — | — (disabled) | — | — |

## Self-verify

| # | Claim | Marker | Citation | Checked |
|---|---|---|---|---|
| 1 | niagara_home = /opt/niagara (System Home) | [CERT-doc] | ControllerPlatformHomes:22,47-48 | ✓ token "/opt/niagara" |
| 2 | daemon User Home = /home/niagara; holds config + running station | [CERT-doc] | ControllerPlatformHomes:25,57-58 | ✓ token |
| 3 | File Transfer Client copies both directions + delete; module caveat | [CERT-doc] | aFileTransferClient:12-13,23-24 | ✓ |
| 4 | /file/ 403 as admin (listing gated) | [CERT-live] | this session | ✓ |
| 5 | station file space roots ^=shared, !=home over /file | [CERT] | [Block 187] | ✓ corpus |
| 6 | QNX system shell via USB Debug = limited menu; SSH-gated too | [CERT-doc] | SystemShellEdge10Startup:12-13,20 | ✓ token |
| 7 | SSH disabled (22 closed) | [CERT-live] | [Block 459] | ✓ |
| 8 | platform tools re-encrypt files to the target passphrase key | [CERT-doc] | aPlatformSystemPassword:52 | ✓ token |

Marker tally: [CERT-doc] ×6 · [CERT-live] ×2 · [CERT] ×2 (corpus) · [INFER] 0 load-bearing (the
`stations/<name>/config.bog` path is the documented convention; station name read live in [Block 461]).
**Block type: EVIDENCE.** Ratio ≈ 0.

## Connections

- **[Block 460]** — the platform daemon that gates Route 1; **[Block 461]** — the station channel behind Route 2.
- **[Block 187]** (px-menu) — the station `file:` space (`^`/`!`) — REMITTANCE, not re-derived.
- Forward: **J6** (System Passphrase / at-rest secrets), **J7** (serial shell + hardware recovery),
  **J8** (File Transfer as the `.bog` route and why platform login is the gate).

## Open gaps

Queued: J7, J8, J2, J6, J9, J10, J11, J3-G1. New child **J5-G1**: confirm a *specific* `/file/` path read
succeeds for `admin` while listing is denied (directory-listing vs file-read ACL) — minor, folded into J8.
