# Probe — [CERT-live] end-to-end confirmation of the login-less dashboard pattern (B728)

> **SECRETS DISCIPLINE (METHODOLOGY §5/§21):** the injected credential VALUE is redacted. Structure only:
> a station user named `API` bound to `HTTPBasicScheme`, password `<PASSWORD-REDACTED>`. The reverse-proxy
> injects `Authorization: Basic base64("API:<PASSWORD-REDACTED>")`. Do not reconstruct the value from this file.
>
> **Attribution (relayed `[CERT-live]`):** NOT run by the author of this block. Executed and reported by a peer
> session (`Panccadia`, socket `uds:/tmp/cc-socks-1000/191176.sock`) at the customer site (Pancaddia León),
> relayed over cross-session messages on 2026-09-03. This session authored the block and preserved the report.
> Target install: JACE-9000 "atlashost", Niagara 4.15.3.28.

## What was verified (as reported by the peer)

| Test | Result | Meaning |
|---|---|---|
| `GET https://<JACE>/dashboardpan/` with `Authorization: Basic base64("API:<PASSWORD-REDACTED>")` | **HTTP 200** (`text/html`) | dashboard renders with the injected credential — mechanism works |
| `GET https://<JACE>/dashboardpan/` with NO auth | **HTTP 302** (login) | without the header the station still gates → not anonymous |
| `GET .../bajaux` (unrelated path) | HTTP 403 | the peer's earlier objection; does NOT apply to the dashboard servlet path |

The `API` user already carried `HTTPBasicScheme` (same scheme as the reference "Mercato" install).

## Public-exposure deployment (as reported)

- Cloudflare **named tunnel** `panccadia-dashboard` (UUID prefix `6fd1074b-…`), healthy, 4 connections.
- Connector runs as a **scheduled task on a separate mini-PC** (distinct from the SSH box).
- **Ingress:** `panccadia.angeles-group.org` → `https://192.168.200.137:443` with `noTLSVerify`.
- **Header injection:** a Cloudflare **Transform Rule** (zone ruleset `default`, phase `http_request_late_transform`)
  adds `Authorization: Basic base64("API:<PASSWORD-REDACTED>")` when `http.host eq panccadia.angeles-group.org`.
- **DNS:** CNAME (proxied) to the `cfargotunnel.com` target.
- **Pattern: OPEN — NO Cloudflare Access.** By explicit user decision, replicating the Mercato install.
- Public URL: `https://panccadia.angeles-group.org/dashboardpan/`

## Honest residuals

- The **open** pattern was chosen against B728 §728.3 step 1 (Cloudflare Access = the recommended primary gate).
  This is a conscious user deviation, recorded — not an endorsement. On an open URL the ONLY remaining gate is the
  `API` user's RBAC scope (read-only + restricted categories), so those must actually be enforced.
- The credential travelled in cleartext over the cross-session channel during relay; if it was meant to be secret
  it should be rotated. (This residual is about the relay, not the deployment.)
- The peer did not report whether the `API` user is confirmed read-only with categories restricted; that guardrail
  is asserted-necessary here but NOT verified on this install.
