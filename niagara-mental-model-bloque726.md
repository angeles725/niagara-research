# Block 726 — Remote Workbench to a JACE behind a jump host over SSH port-forwarding: the localhost high-port pattern, why Fox does NOT break it (no IP redirect), and the Platform-only path for backups/commissioning

**Session**: 2026-09-02
**Focus**: `access-control` (document-mode capture, METHODOLOGY §20 — operator-requested runbook; extends [Block 560])
**Type**: DOCUMENT / runbook. Niagara-side facts are `[CERT]` via cited corpus blocks and `[CERT-doc]` via the
official help guides; the tunnel mechanism is `[CERT-live]` — confirmed end-to-end against a real JACE-9000 at
a customer site (Pancaddia León, 2026-09-02, reported by the on-site operator/peer session). SSH/WSL behavior
is `[CERT-live]` from that same probe.
**Grounding sources**:
- Fox wire / redirect semantics `[CERT]`: [Block 134] §134.10 (`FoxsRedirectException(port)` — the ONLY Fox
  redirect is plaintext→TLS and carries a **port**, never an IP; `hostName`/`hostAddress` in the welcome frame
  is informational identity, `FoxSession.java:517-530`), corroborated live by [Block 471]/[Block 473]
  (hand-rolled Fox client to `foxs:4911`, no redirect).
- Transport matrix + tunnel runbook `[CERT]`: [Block 560] (cloudflared runbook — this block is the **SSH `-L`
  variant** of its Tier-2 TCP path), [Block 460]/[Block 471] (live ports: Foxs `:4911`, platform `:3011/:5011`).
- TLS-1.3-only station `[CERT]`: [Block 474]. Default self-signed cert `[CERT]`: [Block 398]/[Block 490].
- Cert approval / Allowed Hosts `[CERT-doc]`: official help `StationSecurity/SSLFixingErrorConditions.txt`,
  `Platform/SSLAllowedHostsTab.txt` (N4.14.0.162). Registered in `sources/SOURCES.md`.
- Live procedure `[CERT-live]`: `sources/probes/2026-09-02-jace-ssh-tunnel-panccadia.md` — the on-site runbook
  (commands + `openssl s_client` cert readback + Windows `Test-NetConnection` results), secrets parameterized.

---

## 726.1 The scenario — Workbench cannot reach the JACE directly [CERT-live]

The operator's laptop runs its **own** local Supervisor (OptimizerSupervisor N4.14.0.162): a local platform
daemon on `localhost:5011` (TLS) / `3011` (plain, disabled) and a local Station. The **JACE-9000** is NOT on
the laptop's segment. The only path is:

```
laptop (Workbench, Windows 10, DESKTOP-4AAQ77H)
  → cloudflared Access SSH  (ProxyCommand: cloudflared access ssh --hostname <jump-host>, short-lived cert)
  → site mini-PC (192.168.200.77)
  → JACE-9000  (dual-NIC: 192.168.200.137 AND 192.168.1.140; reached via .137)
```

The mini-PC reaches the JACE; the laptop reaches only the mini-PC (through a **cloudflared Access SSH** jump
host — the SSH transport is itself proxied through cloudflared with a minted short-lived cert). The **JACE-9000
is dual-homed** on two subnets (`192.168.200.137` + `192.168.1.140`); this probe used `.137`. JACE open ports
(scanned from the mini-PC): **4911** (Foxs), **5011** (Platform TLS), **443/80** (web). Closed: **1911** (plain
Fox), **3011** (plain Platform), **22**. The station is in **secure / TLS-only** mode — every usable transport
into the JACE is already TLS-wrapped, there is no plaintext port to accidentally lean on. `[CERT-live]`

## 726.2 The forward — SSH `-L` to high, non-clashing localhost ports [CERT-live]

Because the laptop already runs a Supervisor on `5011`/`3011`, forward the JACE's ports to **high, free**
localhost ports so they do not collide with the local daemon:

```
# HIGH ports so they never collide with the laptop's own Supervisor on 5011/3011.
# Transport = cloudflared Access SSH as the ProxyCommand (short-lived minted cert).
ssh -f -N \
  -L 127.0.0.1:15011:192.168.200.137:5011 \   # Platform
  -L 127.0.0.1:14911:192.168.200.137:4911 \   # Foxs
  -L 127.0.0.1:18443:192.168.200.137:443  \   # web (optional)
  -o ProxyCommand="cloudflared access ssh --hostname %h" \
  -i <cf_key> -o CertificateFile=<cf_key-cert.pub> \
  <win-user>@<jump-host>
```

| JACE service | JACE port | Local forward | Workbench uses |
|---|---|---|---|
| Platform (niagarad TLS) | 5011 | `localhost:15011` | Open Platform → `localhost:15011` |
| Station (Foxs / Fox-TLS) | 4911 | `localhost:14911` | Open Station → `foxs://localhost:14911` |
| Web (Hx/PX/oBIX) | 443 | `localhost:18443` | browser → `https://localhost:18443` |

**Prove the forward lands on the JACE `[CERT-live]`** — before opening Workbench, read the cert the forward
serves:

```
openssl s_client -connect 127.0.0.1:15011 </dev/null 2>/dev/null | openssl x509 -noout -subject -issuer
#   → subject/issuer = CN=Niagara4, O=ForRecoveryPurposes
```

Subject == issuer (self-signed) and it is the JACE's default `ForRecoveryPurposes` cert — proof the forward
reaches the JACE (nothing local listens on 15011), not the laptop's own Supervisor.

**WSL2 note `[CERT-live]`**: the tunnel ran inside **WSL** while Workbench runs on **Windows**. WSL2 mirrored
networking shares loopback with the Windows host, so a WSL-side `-L` forward is reachable from Windows —
confirmed `Test-NetConnection 127.0.0.1 -Port 15011/14911/18443` = **True** for all three from Windows. No
`netsh portproxy` was needed.

This is exactly [Block 560] §560.3's Tier-2 pattern with **SSH `-L` in place of `cloudflared access tcp`**: raw
TCP forwarding, one forward per service, TLS end-to-end Niagara↔Workbench (the transport only shuttles TCP
bytes; it never terminates the JACE's TLS).

## 726.3 Non-standard ports work — the connect dialogs take host + port [CERT-doc + CERT-live]

Workbench's **Open Platform** and **Open Station** dialogs take **Host** and **Port** as separate fields;
`5011`/`4911` are only defaults. Pointing Platform at `localhost:15011` (or Station at `foxs://localhost:14911`)
is a supported, ordinary connection — the high port is not special. Confirmed live: the operator connected Open
Platform to `localhost:15011` and received the JACE cert.

## 726.4 Fox does NOT redirect to the JACE's real IP — the tunnel is safe [CERT]

The central fear with any localhost tunnel is: *after the Fox handshake, does the station tell Workbench "my
real address is 192.168.200.137, reconnect there" — which the laptop cannot reach?* **No.** From the wire
([Block 134] §134.10, verbatim against the code):

- The **only** redirect Fox defines is the **foxs-only redirect**: a *plaintext* `fox` client hitting a
  foxs-only server gets a `fox/redirect` tuning frame carrying the **foxs PORT** (not an IP), and the client
  throws `FoxsRedirectException(port)` to retry over TLS (`Tuner.java:409-413`,
  `FoxSession.java:495-499,568-569`). It is a plain→TLS **port** upgrade, never an IP move.
- Connecting straight to `foxs://localhost:14911` means there is **no redirect at all** — you are already on
  TLS.
- The `hostName`/`hostAddress` carried in the welcome frame (`FoxSession.java:517-530`) is the server's
  **informational identity**, NOT a reconnection instruction. Workbench does not tear down the session to dial
  that address.
- Corroborated live: [Block 471]/[Block 473] drove a hand-rolled Fox client to `foxs:4911` end-to-end with no
  redirect behavior.
- **Confirmed at this site `[CERT-live]`** (probe, 2026-09-02): the operator opened **Open Station →
  `foxs://localhost:14911`** over the forward and it **connected and operated** — the Fox leg did NOT break on
  a redirect to the JACE's real IP (`192.168.200.137`), which the laptop cannot reach. This is the live
  confirmation of [Block 134] §134.10 for a tunneled Workbench. (Honest residual: the operator reported "both
  connected and worked" but did NOT itemize whether the Foxs cert-approval dialog was separate from Platform's,
  nor formally measure session stability — see Open gaps.)

**The one place the "redirect to real IP" fear IS real** — and why it does not apply here: **station-to-station**
navigation, where a Supervisor opens a Fox *client* connection to a subordinate whose **Address** is configured
as the real IP in the Supervisor's `NiagaraNetwork`. There the **Supervisor** (server-side) dials that IP. That
is not this case: `File > Open > Open Station` from Workbench opens a **direct** session to `localhost:14911`
and stays there.

## 726.5 Platform alone covers backups / commissioning / TCP-IP — Fox is only for logic [CERT]

You do **not** need the Fox/Station forward for platform-level work. The Platform connection
(`localhost:15011`) gives: **backup/restore**, the **commissioning** wizard, **TCP/IP** configuration,
software/license install, and station lifecycle. The **Station** (Foxs `:4911`) connection is needed only to
program the running station — wiresheet, control logic, points ([Block 560] §560.1 transport split). So if the
task is *backup the JACE, commission it, or fix its network settings*, bring up **only** `localhost:15011` and
skip Fox entirely. (JACE backup also has a Workbench-free USB path — `sources/SOURCES.md`
`JACE-8000USBBackaupAndRestoreFeatur`, [Block 459]/[Block 463]/[Block 469] — orthogonal to the tunnel.)

## 726.6 Certificate gotchas over the tunnel — trust-on-first-use, and the local-Supervisor coexistence [CERT-doc]

Workbench uses **trust-on-first-use**, not strict hostname (CN) verification — which is *why* connecting to
`localhost` does not fail on a hostname mismatch:

1. **The JACE presents its factory self-signed cert** (default `ForRecoveryPurposes`, [Block 398]/[Block 490]).
   On first connect Workbench pops the approval dialog. Compare **Issued By** vs **Subject** (identical for a
   self-signed cert); if you recognize it, approve — it lands in **Allowed Hosts**. `[CERT-doc]`
   `SSLFixingErrorConditions.txt` ("If you recognize the name, you can manually approve the certificate").
2. **Hostname mismatch is not fatal** — it is precisely the "approve the certificate exemption" flow. The FIPS
   troubleshooting guide states it literally for the platform: *"reconnect using File > Open > Open Platform,
   and approve the certificate exemption when it pops up."* SSH only moves TCP bytes; the TLS is end-to-end
   inside the tunnel, so this is **not** a real MITM.
3. **Coexistence with the local Supervisor — the honest nuance** `[CERT-doc]` (`SSLAllowedHostsTab.txt`): the
   Allowed Hosts exemption is keyed by *the IP/domain used to connect* **plus that cert's public key** — *"The
   approved host exemption ... is only valid when a client connects using the IP address or domain name that
   was used when the exemption was created ... The same is true if a new self-signed certificate is generated
   on the host."* Because BOTH the local Supervisor and the JACE are reached as **`localhost`** but present
   **different** certs, expect a **fresh approval prompt for the JACE** even though `localhost` was already
   trusted for the local Supervisor. The two exemptions **coexist** in the Allowed Hosts list — no overwrite,
   no breakage. This is the only surprise; approve the JACE's cert when it pops.
4. **TLS-1.3-only** `[CERT]` [Block 474]: irrelevant with SSH `-L` (SSH never touches the TLS), but if a
   TLS-*terminating* proxy is ever inserted mid-path it must speak 1.3 — the station rejects 1.2.

## 726.7 Security posture (unchanged from [Block 560] §560.4) [CERT + INFER]

The tunnel does not lower the bar. Keep the station's own TLS + SCRAM on; replace the default self-signed cert;
use a scoped (non-`admin`) engineering account ([Block 468] repeatedly finds exposed admin creds); and **do not
expose the raw ports to the internet without an auth layer in front** (VPN / Cloudflare Access) — that exposure
is the [Block 75] intrusion pattern. **Platform port hygiene**: `:5011` grants full control (install modules,
change certs, station lifecycle) — bring its forward up only for the maintenance window, then drop it. An SSH
`-L` forward through an authenticated jump host already puts SSH auth in front of the ports, which is the
mitigating property here.

## 726.8 Tridium's "recommended pattern" — there isn't a special one [CERT + INFER]

Tridium blesses no specific tunnel product. The programming transports are plain TCP (Foxs `:4911` + Platform
`:5011`), so **any** TCP tunnel or VPN carries them — SSH `-L`, cloudflared, WireGuard/Tailscale. Their guidance
is the security posture above ([Block 560] §560.5). For a single engineer needing full Workbench, a **mesh VPN**
(Tailscale/WireGuard) makes the JACE a LAN peer with no per-service forward and is often less setup than
per-service TCP forwarding; SSH `-L` wins when you already have an authenticated SSH jump host to the site (this
case).

## 726.9 The runbook, condensed [CERT-live]

1. `ssh -f -N -L 127.0.0.1:15011:JACE:5011 -L 127.0.0.1:14911:JACE:4911 [-L 127.0.0.1:18443:JACE:443]
   -o ProxyCommand="cloudflared access ssh --hostname %h" -i <cf_key> -o CertificateFile=<cf_key-cert.pub>
   <win-user>@<jump-host>`.
2. (WSL) prove it reaches the JACE: `openssl s_client -connect 127.0.0.1:15011` → `CN=Niagara4,
   O=ForRecoveryPurposes` (the JACE's own default cert).
3. (Windows) confirm Workbench sees the WSL forward: `Test-NetConnection 127.0.0.1 -Port 15011` → `True`.
4. Workbench → **Open Platform** → `localhost:15011` → **approve the JACE cert** when the dialog pops (fresh
   exemption even if `localhost` was already trusted for the local Supervisor).
5. Platform now covers **backup / commissioning / TCP-IP**. No Fox needed for those.
6. Only to program logic: **Open Station** → `foxs://localhost:14911`. No IP redirect breaks it — confirmed
   live at this site (connected + operated over the forward). `[CERT-live]`

## 726.10 Packaging for the operator — double-click `.bat` [CERT-live]

To hand the tunnel to a non-technical operator, the on-site setup wrapped it as **`Conectar-JACE.bat`**
(double-click) → a WSL helper **`tunnel-jace.sh`** that (a) mints the cloudflared Access cert and (b) runs the
`ssh -N` in the **foreground**. Closing the window drops the tunnel — no persistent daemon, tunnel lifetime is
the window's lifetime. This is a UX packaging of §726.2, not a different mechanism.

## Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|-------|--------|----------|---------|
| 1 | SSH `-L` high-port forward (15011→5011, 14911→4911, 18443→443) reaches the JACE; `openssl s_client localhost:15011` returns `CN=Niagara4, O=ForRecoveryPurposes` (JACE default cert) | [CERT-live] | probe 2026-09-02 (sources/probes/…-panccadia.md) | live-confirmed ✓ |
| 2 | WSL2 mirrored networking shares loopback with Windows; `Test-NetConnection localhost:15011/14911/18443`=True from Windows hits the WSL forward; no netsh proxy needed | [CERT-live] | same probe | live-confirmed ✓ |
| 3 | Open Platform/Open Station take host+port; non-standard port works | [CERT-doc]+[CERT-live] | connect dialogs; live connect | doc+live ✓ |
| 2b | Transport is cloudflared Access SSH (ProxyCommand, minted short-lived cert); JACE is dual-NIC (.200.137 + .1.140), station TLS-only | [CERT-live] | same probe | live-confirmed ✓ |
| 3b | Operator packaging: Conectar-JACE.bat → WSL tunnel-jace.sh mints cert + foreground ssh -N (close window = drop tunnel) | [CERT-live] | same probe | live-confirmed ✓ |
| 4 | Fox's only redirect is plain→TLS carrying a PORT, not an IP; hostAddress is informational | [CERT] | [B134]§134.10, FoxSession.java:495-499,517-530,568-569; Tuner.java:409-413 | corpus/code-cited ✓ |
| 5 | Direct `foxs://localhost:14911` performs no redirect at all — Open Station connected + operated over the forward at this site (no break to the JACE's real IP) | [CERT]+[CERT-live] | [B134]§134.10; [B471]/[B473]; probe 2026-09-02 | corpus+live ✓ |
| 6 | The IP-redirect fear applies only to station-to-station (Supervisor dials configured Address), not direct Open Station | [INFER] | NiagaraNetwork client-connection model | labeled INFER ✓ |
| 7 | Platform connection covers backup/commissioning/TCP-IP; Fox only for logic/points | [CERT] | [B560]§560.1 | corpus-cited ✓ |
| 8 | Workbench trust-on-first-use; hostname mismatch → approve exemption, not fatal | [CERT-doc] | SSLFixingErrorConditions.txt | doc-cited ✓ |
| 9 | Allowed Hosts exemption keyed by host+cert public key; local Supervisor + JACE both "localhost" with different certs coexist, JACE prompts fresh | [CERT-doc] | SSLAllowedHostsTab.txt | doc-cited ✓ |
| 10 | Station is TLS-1.3-only (rejects 1.2) | [CERT] | [B474] | corpus-cited ✓ |
| 11 | Default cert is factory ForRecoveryPurposes | [CERT] | [B398]/[B490] | corpus-cited ✓ |
| 12 | Exposing raw ports without an auth layer = the B75 pattern | [CERT] | [B75] | corpus-cited ✓ |

**Marker tally**: [CERT] ×6 · [CERT-doc] ×3 (one shared with CERT-live) · [CERT-live] ×6 (incl. the Fox-leg
confirmation) · [INFER] ×1 labeled · 0 unlabeled claims. Block TYPE = DOCUMENT/runbook. Live facts marked `[CERT-live]` (on-site probe preserved at
`sources/probes/2026-09-02-jace-ssh-tunnel-panccadia.md`, run by the operator/peer session — not by the author
of this block); Niagara internals corpus/code-cited; cert flow doc-cited; the station-to-station carve-out
honestly `[INFER]`.

## Connections

- **[Block 560]** — the cloudflared runbook this block is the **SSH `-L` variant** of; §560.1 transport split,
  §560.4 security posture, §560.5 alternatives all carry over. B560 updated with a pointer to here.
- **[Block 134]** §134.10 — the Fox redirect semantics (port-only, no IP) that make the localhost tunnel safe.
- **[Block 471]/[Block 473]** — live hand-rolled Fox client to `foxs:4911`, corroborating "no redirect".
- **[Block 474]** — TLS-1.3-only, why a terminating proxy (not SSH `-L`) must speak 1.3.
- **[Block 398]/[Block 490]** — default self-signed cert; the approval prompt in §726.6.
- **[Block 459]/[Block 463]/[Block 469]** — JACE backup paths (Workbench platform + USB clone).
- **[Block 75]** — the intrusion incident that makes "auth layer in front" non-negotiable.
- **[Block 468]** — exposed-admin finding; use a scoped account.

## Open gaps (this block)

- **CLOSED** (2026-09-02): the Fox/Station leg (`foxs://localhost:14911`) is now confirmed `[CERT-live]` — the
  operator opened Open Station over the forward and it connected + operated, with no redirect break to the
  JACE's real IP. Two fine-grained sub-facts remain UN-itemized (honest residual, not blocking): whether the
  Foxs cert-approval dialog was separate from Platform's (expected separate — different service, same host
  identity), and formal session-stability measurement (no reconnect/retry) — the operator reported it "worked"
  but did not measure. If those are ever needed at claim-grain, one targeted question to the site closes them.
  Document-mode block — no gap-discovery; the `access-control` focus stays `stopped`.
