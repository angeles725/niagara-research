# B460 — The JACE-8000 platform daemon (niagarad): what listens on :3011/:5011, why GET is 403, and how the platform connection differs from the station (focus jace8000, J3)

> **Focus:** `jace8000` (§16). **Gap:** J3 — the platform daemon on :3011 (HTTP) / :5011 (TLS): the platform
> protocol, the services it fronts, why it refuses a browser, and how "platform" access differs from
> "station" access.
> **Phase:** §12 dynamic (live at `192.168.1.140`). Read-only. `live-install` → SECRETS DISCIPLINE.
> **Sources:** `[CERT-live]` platform-daemon probes this session · `[CERT]` corpus [Block 381] [Block 124]
> [Block 125] [Block 129] (the `niagarad` daemon RE) · `[CERT-doc]` niagara-help (`AXtoN4Migration`).
>
> **Bottom line:** there are **two independent front doors** on a JACE. The **station** (:443 web, :4911 Fox)
> serves your control application and authenticates *station users*. The **platform daemon** `niagarad`
> (:3011 plain / :5011 TLS) is the OS-level admin service — install software, format flash, set TCP/IP, start
> the station, transfer files — and authenticates *platform accounts*, which live in the OS, not the station.
> Losing one does not lose the other; that split is the key to recovery (J7) and to pulling a `.bog` (J8).

## §460.1 — Live behavior: it is HTTP, but it answers nothing without the handshake

Probed this session against `192.168.1.140`:

- `[CERT-live]` **:3011** answers `HTTP/1.1 403 Forbidden` to **every** method — GET, POST, OPTIONS, HEAD, PUT
  — with only `x-frame-options: deny`, `Connection: close`, `Content-Length: 0`, and **no `WWW-Authenticate`
  header**. There is no 401 auth challenge to enumerate.
- `[CERT-live]` It *is* a real HTTP server (Jetty-class): a malformed request line returns
  `HTTP/1.1 400 No URI` / `<h1>Bad Message</h1>` — so it parses HTTP, it just blanket-403s any well-formed
  request that lacks the platform-protocol credentials.
- `[CERT-live]` **:5011** is the same daemon over **TLS 1.3** (`TLS_AES_256_GCM_SHA384`), also 403. Its cert
  is the default `CN=Niagara4, O=Tridium` (expired 2022, [Block 459] §459.4).

**Interpretation:** the platform daemon does not offer an HTTP auth *challenge*; it expects the caller to
present platform credentials **up front** inside the request (N4 uses a digest/nonce handshake over this HTTP
channel). A browser or a naive `curl` therefore always sees 403 — you cannot "click into" the platform. This
is deliberate hardening: no challenge → no scheme/realm leak → no easy credential probing. Reaching it means
speaking the platform protocol (Workbench does; a hand-rolled client must reproduce it — J8).

## §460.2 — What the daemon is (from the corpus RE)

The platform daemon is the process the corpus decompiled as `niagarad` on the Windows supervisor; the JACE
runs the QNX build of the same daemon:

- `[CERT]` [Block 381] — `plat.exe installdaemon` registers `niagarad.exe` as a **LocalSystem, auto-start**
  Windows service named "Niagara", description "Platform management service for Niagara tools". On the JACE
  the equivalent is a boot-started elevated QNX process. Either way the daemon runs at the **highest OS
  privilege** — anything that compromises it inherits that privilege ([Block 381] §381.2 threat note).
- `[CERT]` [Block 124]/[Block 125] — `niagarad` is the platform binary; `daemonize0` is its run-time service
  entry. [Block 129]/[Block 381] — it also owns `setsystempw` (the System Passphrase, sealed at rest), which
  is why the System Passphrase is a *platform* concept, not a station one (J6).

That privilege level is why the platform channel can do things the station cannot: repartition/format flash,
install OS `.dist` files, and read/write arbitrary files on the controller — the capabilities behind J5/J7/J8.

## §460.3 — Platform account ≠ station user (the two-credential model)

The docs make the split explicit:

- `[CERT-doc]` A platform connection is opened **to port 3011** and authenticates with **platform
  credentials** — e.g. after a `.dist` install + reboot you "re-open a platform connection (port 3011) using
  the **factory default credentials**" (`guides-clean/AXtoN4Migration/ID-1133-00000d9b.txt:52`).
- `[CERT-doc]` The daemon has its **own user home**, separate from the Workbench/station homes: "a Workbench
  User Home (for people), and a **platform daemon User Home** (for the daemon server processes)"
  (`guides-clean/AXtoN4Migration/r_WindowsNiagaraUserHomes.txt:14`); on Windows it is
  `C:\ProgramData\Niagara4.x\<brand>` (`:25`). On the JACE the daemon home is a QNX flash path (J2).

Consequences that matter operationally:

| | Station (:443 web, :4911 Fox) | Platform daemon (:3011/:5011) |
|---|---|---|
| Authenticates | **station users** (in `config.bog`, SCRAM-SHA-256) | **platform accounts** (OS-level) |
| Serves | your control app, PX, histories, alarms | OS admin: install, TCP/IP, flash, station start/stop, files |
| Credential store | inside the station database | outside the station, on the controller OS |
| If you lose the other one | station can still be admin'd from platform | platform still works if station is corrupt/gone |

The last row is the whole point: **the platform daemon is the recovery surface for a broken station**, and a
broken/locked platform is recovered from *hardware* (J7) — because each front door has an independent
credential store, no single lost password bricks the box.

## §460.4 — Why this frames J5 / J7 / J8

- **J5 (filesystem):** the platform daemon's File Transfer / file services are the *network* route into the
  QNX tree at daemon privilege — but only after the platform handshake succeeds.
- **J7 (recovery):** platform is how you fix a dead station without touching hardware; if platform itself is
  locked, the hardware button/serial/USB routes (which the daemon cannot gate) take over.
- **J8 (pull the `.bog` without Workbench):** the daemon speaks a documented-enough HTTP+digest platform
  protocol; reproducing it (as `api-access` reproduced the SCRAM web login) is the RE path to drive its file
  transfer / station-copier from a script. The live 403-to-GET is the proof that the naive path is closed and
  the handshake must be ported.

## Self-verify

| # | Claim | Marker | Citation | Checked |
|---|---|---|---|---|
| 1 | :3011 returns 403 to all HTTP methods, no WWW-Authenticate | [CERT-live] | this session | ✓ GET/POST/OPTIONS/HEAD/PUT all 403 |
| 2 | It is an HTTP server (400 "No URI" on malformed) | [CERT-live] | this session | ✓ |
| 3 | :5011 = same daemon over TLS 1.3 AES-256-GCM | [CERT-live] | this session | ✓ openssl |
| 4 | Daemon = niagarad, highest-privilege, auto-start | [CERT] | [Block 381] §381.2 | ✓ corpus |
| 5 | Daemon owns setsystempw / System Passphrase | [CERT] | [Block 129]/[Block 381] | ✓ corpus |
| 6 | Platform connection = port 3011, platform (not station) creds | [CERT-doc] | AXtoN4Migration/ID-1133-00000d9b.txt:52 | ✓ token |
| 7 | Platform daemon has its own User Home separate from people | [CERT-doc] | r_WindowsNiagaraUserHomes.txt:14,25 | ✓ token |

Marker tally: [CERT-live] ×3 · [CERT] ×2 (corpus) · [CERT-doc] ×2 · [INFER] 0 in load-bearing claims (the
digest-handshake interpretation in §460.1 is explicitly labeled interpretation). **Block type: EVIDENCE.**
Ratio ≈ 0. Investigable evidence for J3's *behavioral* surface is well-covered; the daemon's exact
wire-protocol bytes are deferred to J8 (RE), not this framing block.

## Connections

- **[Block 459]** — architecture; this refines the ":3011/:5011 = platform, 443/4911 = station" split.
- **[Block 381]/[Block 124]/[Block 125]/[Block 129]** (`platform-native`) — the decompiled `niagarad`/`plat`
  daemon; this block is its live, JACE-facing behavior.
- Forward: **J5** (file services), **J7** (platform as recovery surface), **J8** (port the platform protocol).

## Open gaps

J8 child: **J3-G1** — the exact platform-protocol handshake bytes (digest scheme, nonce, the servlet path
Workbench POSTs to) — deferred to J8's RE. Queued: J2, J4, J5, J6, J7, J8, J9, J10, J11.
