# Block 560 — Remote access to a Niagara station over a Cloudflare Tunnel (`cloudflared`): what "connect + program" actually requires, the two tiers (browser HTTP vs Workbench TCP), and the mandatory security posture

**Session**: 2026-08-28
**Focus**: `access-control` (document-mode capture, METHODOLOGY §20 — operator-requested runbook)
**Type**: DOCUMENT / runbook. NOT a decompilation-evidence block. Niagara-side facts are `[CERT]` via cited
corpus blocks; `cloudflared` behavior is external product knowledge marked `[INFER-web]` (validate against
current Cloudflare docs — the `cloudflared` CLI evolves).
**Grounding sources**:
- Port/transport matrix `[CERT]`: [Block 460]/[Block 471] (JACE-8000 live: station `:443` SCRAM, Fox/foxs
  `:4911`, platform daemon `niagarad` `:3011/:5011`), [Block 474] (station TLS **1.3-only**, rejects 1.2).
- Hardening posture `[CERT]`: [Block 398]/[Block 490] (security checklist SEC-01..22), [Block 75] (the real
  intrusion incident this whole caution exists for).

**Question (operator):** *"Can I stand up a `cloudflared` tunnel so I can connect from an external computer and
PROGRAM the station?"* Short answer: **yes for browser operation trivially; yes for full Workbench programming
with extra TCP setup; but only behind Cloudflare Access + Niagara's own TLS, and it is a real attack-surface
decision, not a convenience toggle.**

---

## 560.1 What "connect + program" actually requires — the transport matrix [CERT]

The mistake is assuming Niagara remote access is one HTTP endpoint. It is not. Three distinct surfaces, three
transports `[CERT]` ([Block 460]/[Block 471]):

| You want to… | Client | Transport | Default port | HTTP? |
|---|---|---|---|---|
| View/operate (Hx, PX, oBIX, dashboards) | Browser | HTTPS | **443** | ✅ yes |
| Program the station (wiresheet, logic, points) | **Workbench** | Fox / **Foxs** (TLS) | **1911 / 4911** | ❌ raw TCP |
| Manage the platform (install modules, TLS certs, station lifecycle) | **Workbench Platform** | platform daemon `niagarad` | **3011 / 5011** (TLS) | ❌ raw TCP |

So "programming" = Foxs `:4911` + platform `:5011`, both **raw TCP over TLS**, NOT HTTP. The station is
**TLS-1.3-only** `[CERT]` ([Block 474]) — it rejects TLS 1.2, which matters when a proxy sits in the path.

## 560.2 Tier 1 — browser-only access (trivial, HTTP tunnel) [INFER-web]

For operation/visualization (NOT programming), a standard Cloudflare Tunnel is enough:
1. On a machine **inside the station's LAN**, install `cloudflared` and authenticate to your Cloudflare account.
2. Create a named tunnel and route an ingress rule to the station's HTTPS:
   ```
   cloudflared tunnel create niagara
   # config.yml ingress:
   #   - hostname: niagara.example.com
   #     service: https://STATION_LAN_IP:443
   #     originRequest:
   #       noTLSVerify: true   # only if the station uses its default self-signed cert (see 560.4)
   #   - service: http_status:404
   cloudflared tunnel route dns niagara niagara.example.com
   cloudflared tunnel run niagara
   ```
3. `niagara.example.com` now reaches the station's Hx/PX/oBIX from anywhere. **This is operation, not
   programming.**

## 560.3 Tier 2 — full Workbench programming (TCP tunnel) [INFER-web]

Workbench needs raw TCP (Foxs `:4911`, platform `:5011`), which a plain HTTP tunnel does not carry. Cloudflare
supports this, but it requires a client-side agent on the **remote** machine:

- **Origin side (station LAN):** add TCP ingress rules to the same tunnel:
  ```
  # config.yml ingress (add):
  #   - hostname: fox.niagara.example.com
  #     service: tcp://STATION_LAN_IP:4911
  #   - hostname: plat.niagara.example.com
  #     service: tcp://STATION_LAN_IP:5011
  ```
- **Remote side (where Workbench runs):** you cannot point Workbench at an HTTPS hostname for Fox. Run a local
  TCP forwarder that terminates the Cloudflare Access session and presents a **localhost port**:
  ```
  cloudflared access tcp --hostname fox.niagara.example.com  --url localhost:4911
  cloudflared access tcp --hostname plat.niagara.example.com --url localhost:5011
  ```
  Then in Workbench, Open Station → `foxs://localhost:4911`, Open Platform → `localhost:5011`.
- **Caveat:** each Niagara service needs its own hostname+forwarder; the two ports are independent. TLS is
  end-to-end Niagara↔Workbench (Cloudflare carries the TCP bytes), so Niagara's TLS-1.3 handshake still happens
  inside the tunnel — do not try to make Cloudflare terminate Foxs.

## 560.4 Mandatory security posture — this is a BMS on the internet [CERT + INFER-web]

Exposing a station, even by tunnel, is exactly the surface [Block 75]'s intrusion exploited. Non-negotiable:

1. **Put Cloudflare Access in front of every hostname** `[INFER-web]` — email/SSO/service-token gate BEFORE the
   request reaches the tunnel. Without Access, a DNS-routed tunnel is a public front door. For the Tier-2 TCP
   hostnames, Access is what `cloudflared access tcp` authenticates against.
2. **Keep Niagara's own TLS and auth intact** `[CERT]` — the station is TLS-1.3-only ([Block 474]) and uses
   SCRAM-SHA-256 ([Block 457]). Do not disable them because "the tunnel is encrypted". Defense in depth: tunnel
   TLS + Niagara TLS + SCRAM.
3. **Replace the default cert** `[CERT]` — [Block 398]/[Block 490] flag the factory `ForRecoveryPurposes`
   self-signed cert; `noTLSVerify: true` in 560.2 is only a stopgap for that default. Install a real cert and
   drop `noTLSVerify`.
4. **Do not expose the platform (`:5011`) unless you must** — platform access = install modules, change certs,
   station lifecycle. Expose Foxs for engineering; bring platform up only for the maintenance window, then tear
   the ingress rule down.
5. **Audit + least privilege** `[CERT]` — the remote engineering account should be a scoped user (categories +
   roles, [Block 11]/AC-focus), not `admin`; the corpus repeatedly finds exposed `admin` creds ([Block 468]).

## 560.5 Alternatives (tradeoffs) [INFER-web]

| Option | Programs Workbench? | Setup | Note |
|---|---|---|---|
| Cloudflare Tunnel + Access (this block) | ✅ (Tier 2 TCP) | medium | no inbound firewall holes; per-hostname Access policy |
| WireGuard / Tailscale (mesh VPN) | ✅ (native TCP) | low-medium | simplest for full Workbench; station reachable as a LAN peer; no per-service config |
| Traditional VPN (IPsec/OpenVPN) | ✅ | medium-high | classic; broad L3 access = larger blast radius if a client is compromised |
| Port-forward `:4911`/`:5011` on the router | ✅ | low | **do NOT** — this is the [Block 75] exposure pattern; no auth layer in front |

For a single engineer needing full Workbench, **Tailscale/WireGuard is usually less setup than the Tier-2 TCP
tunnel** (no per-service `cloudflared access` process). Cloudflare Tunnel wins when you also want browser access
published to a team behind SSO with no VPN client.

## 560.6 Verdict

- **Operate from a browser:** yes, one HTTP tunnel (560.2). Easy.
- **Program with Workbench:** yes, but it is TCP (Foxs `:4911` + platform `:5011`), so it needs `cloudflared
  access tcp` forwarders on the remote machine plus TCP ingress on the origin (560.3) — or a mesh VPN instead.
- **The real question is not feasibility, it is exposure.** Only with Cloudflare Access in front, Niagara TLS +
  SCRAM kept on, a real cert, a scoped (non-admin) account, and the platform port normally closed. Under those
  conditions it is a legitimate remote-engineering setup; without them it is the [Block 75] incident waiting to
  happen.

## Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|-------|--------|----------|---------|
| 1 | Programming = Foxs :4911 + platform :5011, raw TCP; operation = HTTPS :443 | [CERT] | [B460]/[B471] | corpus-cited ✓ |
| 2 | Station is TLS-1.3-only (rejects 1.2) | [CERT] | [B474] | corpus-cited ✓ |
| 3 | Default cert is factory ForRecoveryPurposes; a hardening finding | [CERT] | [B398]/[B490] | corpus-cited ✓ |
| 4 | Exposing station ports without an auth layer = the B75 incident pattern | [CERT] | [B75] | corpus-cited ✓ |
| 5 | cloudflared carries HTTP by default; TCP needs `cloudflared access tcp` + TCP ingress | [INFER-web] | Cloudflare product behavior (validate vs current docs) | labeled INFER ✓ |
| 6 | Mesh VPN (Tailscale/WireGuard) is often less setup for full Workbench | [INFER-web] | operational reasoning | labeled INFER ✓ |

**Marker tally**: [CERT] ×4 · [INFER-web] ×2 · [INFER] ×0 unlabeled. Block TYPE = DOCUMENT/runbook. Niagara-side
facts corpus-cited; tunnel mechanics honestly marked as external product knowledge.

## Connections

- **[Block 460]/[Block 471]** — the live port/transport facts (Foxs :4911, platform :3011/:5011) this runbook rests on.
- **[Block 474]** — TLS-1.3-only, why a terminating proxy must not sit inside the Foxs handshake.
- **[Block 398]/[Block 490]** — hardening checklist; default cert + exposed-admin findings apply directly.
- **[Block 75]** — the intrusion incident that makes "put Access in front" non-negotiable.
- **[Block 457]** — SCRAM login, the station-native auth kept on under the tunnel.

## Open gaps (this block)

- Not empirically validated against a live tunnel to THIS station (would be a §12 dynamic exercise): the exact
  `cloudflared access tcp` ↔ Foxs TLS-1.3 interaction (does Access's TCP proxy pass the 1.3 ClientHello cleanly?)
  is asserted from product behavior, not tested here. Marked `[INFER-web]`; promote to `[CERT-live]` only with a
  real tunnel test. Document-mode block — no gap-discovery; focus loop resumes at AC3.
