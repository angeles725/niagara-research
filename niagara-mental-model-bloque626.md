# Block 626 — ports (PO-G7): platform daemon :3011/:5011 auth model — platform-user username/password, TLS-mode enum, full-host reach

> **What**: The static (code) characterization of the platform daemon's authentication/admission model on
> :3011 (plain) / :5011 (TLS), consolidating what the live probes ([B460]) and static launcher analysis
> ([B129]) established into the per-port auth-gate + reachability row. Answer: the daemon authenticates a
> **platform USERNAME + PASSWORD** — platform users, a host/OS-level user space SEPARATE from station
> `BUser`s — presented up-front by the client (no 401 challenge). The TLS posture is a four-state enum
> (`disabled` / `enabled` / `sslOnly` / `notLicensed`). Reach is full platform administration of the host.
> **Scope**: `platDaemon-rt` (`BDaemonSurrogate`, `BDaemonSSLStatus`, the `plat` command clients) + the
> `com.tridium.platform.daemon.BDaemonSession` seam. The 18-command set + live wire (403 no-401, TLS 1.3) are
> REMITTANCE to [B129]/[B460]/[B158]; the on-the-wire credential DIGEST frame is the requires-execution child
> PO-G7w ([B129] N6-wire). **Block type**: EVIDENCE (code) + consolidation.
> **Subject version**: Niagara N4.14.0.162.
> **Sources**:
> - `organized/platDaemon/platDaemon-rt/vineflower/com/tridium/platDaemon/BDaemonSurrogate.java`
> - `organized/platDaemon/platDaemon-rt/vineflower/com/tridium/platDaemon/BDaemonSSLStatus.java`
> - `organized/platDaemon/platDaemon-rt/vineflower/com/tridium/platDaemon/command/*Command.java` (the `plat`
>   CLI clients; string literals for "password" scrubbed to `ln` — cited by option flag + structure per §5)
> **Method**: vineflower; the credential-transport DIGEST is NOT statically recoverable (scrubbed + the wire
> frame is native) → held to PO-G7w. Markers: `[CERT]` `file:line`; `[INFER]` = consolidation/reasoning.

---

## 626.1 — Credential: platform username + password, presented up-front `[CERT]`

The `plat` command clients (`BFileGetCommand`, `BWatchStationCommand`, `BDetailsCommand`, `BSetTimeCommand`,
…) each accept `-usr:<username>` and `-pwd:<password>` for "the host's platform daemon", and `-noinput`
"will fail when username and password are missing or incorrect, instead of prompting and reading them from
stdin" `[CERT]` (`command/*Command.java`, the `-pwd:<…>` + `-noinput` help text; the literal "password" is
scrubbed to `ln`, cited by the option flag). So the credential is a **username + password** pair, supplied by
the caller (arg or stdin) and presented to the daemon — consistent with [B460]'s live finding that the daemon
returns 403 (not a 401 challenge): the client authenticates up-front, the daemon does not challenge.

These are **platform users**, a host/OS-level user space DISTINCT from station `BUser`s `[INFER via B129]`:
the daemon runs as a host service (`plat.exe`/SCM, [B129]) and governs the host, not a station's component
tree — its accounts are the platform-admin accounts, not the station RBAC users of [B11]. (This is why a
station operator credential does not open the platform daemon, and vice-versa.)

## 626.2 — Session handle: `BDaemonSurrogate` over `BDaemonSession` `[CERT]`

`BDaemonSurrogate extends BComponent` wraps a `BDaemonSession` (`com.tridium.platform.daemon.BDaemonSession`)
and is constructed via `make(BDaemonSession)` `[CERT]` (`BDaemonSurrogate.java:42,48,68-73`). It exposes
daemon operations — thread dumps, get/set/save log levels, daemon output (`DumpThreadsMessage`,
`GetLogLevelsMessage`, `SetLogLevelMessage`, `SaveLogLevelMessage`, `GetDaemonOutputMessage`) `[CERT]`
(imports `:7-11`). So the surrogate is the authenticated Java handle to a connected daemon session; the
message classes are the operation set (a superset with the 18 platform commands of [B129]).

## 626.3 — TLS posture: a four-state enum `[CERT]`

`BDaemonSSLStatus extends BFrozenEnum` with ranges `disabled(0)` / `enabled(1)` / `sslOnly(2)` /
`notLicensed(3)` `[CERT]` (`BDaemonSSLStatus.java:12-19`). This is the daemon's TLS admission mode:
- `disabled` — plaintext daemon only (:3011).
- `enabled` — both plaintext :3011 and TLS :5011 accepted.
- `sslOnly` — TLS :5011 only; plaintext refused (the HARDENED state).
- `notLicensed` — TLS unavailable (no license).

This is the code form of [B129]'s `secure = (port != 3011)` (port 3011 plaintext, 5011 TLS) and adds the
`sslOnly` hardening flag. Live: :5011 negotiated TLS 1.3 AES-256-GCM ([B460]).

## 626.4 — Per-port answer `[INFER]`

| Dimension | Platform daemon :3011 (plain) / :5011 (TLS) |
|---|---|
| What it is | The host platform daemon (`plat.exe`/SCM) — installs/starts/stops stations, files, backup, reboot, IP, license |
| Configured | Host daemon config (not `config.bog`); TLS mode = `BDaemonSSLStatus` (disabled/enabled/sslOnly/notLicensed) |
| Auth gate | Platform USERNAME + PASSWORD (platform users, separate user space from station `BUser`s), presented up-front (no 401 challenge, [B460]) |
| Reachability | Full platform administration of the HOST — equivalent to OS admin of the controller ([B129] 18-command set) |
| Mitigations | `sslOnly` (refuse plaintext :3011); loopback/interface binding; strong platform passwords; network isolation of the platform port |
| Open (requires-execution) | The on-the-wire credential DIGEST frame — PO-G7w ([B129] N6-wire); observable only live |

Operator guidance `[INFER]`: the platform daemon is the highest-value port (host admin). Set `sslOnly` to
refuse plaintext :3011, isolate the platform port to a management network, and treat platform credentials as
host-root-equivalent. Unlike the station ports (Fox/Web, SCRAM+RBAC), platform auth is a separate
username/password space — a compromise of it is a compromise of the whole controller, not one station role.

## 626.5 — Connections

- **[B129]** — static platform daemon (plat.exe/SCM, `secure=(port!=3011)`, 18 commands, why the wire is
  Java); **[B460]** — live (403 no-401, TLS 1.3, two-credential model); **[B158]** — HTTP-saved wire. B626
  consolidates these into the per-port auth-gate + reach row and adds the `BDaemonSSLStatus` states.
- **[B11]** — station RBAC users (the CONTRAST: platform users are a separate space).
- **PO-G7w** — the requires-execution child (live credential frame).
- Forward: **PO-G8** synthesis.

---

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | `plat` clients take `-usr`/`-pwd` (username+password) or stdin; `-noinput` fails if missing/incorrect | `[CERT]` | command/*Command.java (`-pwd:<…>`, `-noinput` help) | ✓ read |
| 2 | Credential presented up-front (consistent with 403 no-401) | `[CERT via B460]` | [B460] + #1 | ✓ remittance |
| 3 | Platform users = separate user space from station `BUser`s | `[INFER via B129]` | [B129] host-service model | ✓ reasoned |
| 4 | `BDaemonSurrogate extends BComponent` wraps `BDaemonSession`; exposes daemon ops | `[CERT]` | BDaemonSurrogate.java:42,48,68-73,7-11 | ✓ read |
| 5 | `BDaemonSSLStatus` enum = disabled/enabled/sslOnly/notLicensed | `[CERT]` | BDaemonSSLStatus.java:12-19 | ✓ read |
| 6 | Reach = full host platform administration (18 commands) | `[CERT via B129]` | [B129] | ✓ remittance |
| 7 | Wire credential digest frame is requires-execution (PO-G7w) | `[INFER]` | scrubbed + native wire, [B129] N6-wire | ✓ reasoned |

**Tally**: `[CERT]` = 3 · `[CERT via B129/B460]` = 2 · `[INFER]` = 2. **Ratio** ≈ 0.4. Block type = EVIDENCE +
consolidation. PO-G7 (read-only auth model) closed; PO-G7w (live wire digest) remains requires-execution.
**Tokens checked**: the `-usr`/`-pwd`/`-noinput` client help, `BDaemonSurrogate` session wrap + message
imports, and the `BDaemonSSLStatus` enum ranges read directly. The scrubbed "password"→`ln` literal was cited
via the option flag, not the garbled token (§5); the credential DIGEST is explicitly deferred to PO-G7w.
