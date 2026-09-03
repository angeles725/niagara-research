# Probe — SSH `-L` tunnel to a JACE-9000 behind a cloudflared Access jump host (Pancaddia León, 2026-09-02)

**Type**: live procedure evidence for a runbook ([Block 726]). `[CERT-live]`.
**Site**: Pancaddia León. **Executed by**: on-site operator + peer session (`pancaddia-bf`/`Panccadia`), reported
to this session 2026-09-02. **Secrets discipline (live-install)**: infra-specific identifiers are
PARAMETERIZED below — real jump-host FQDN, SSH principal, Windows username, and cloudflared key/cert filenames
are redacted to placeholders; only STRUCTURE is preserved.

## Topology (observed)

- Operator laptop `DESKTOP-4AAQ77H` — Windows 10 Pro, runs its OWN Supervisor (Tridium **OptimizerSupervisor
  N4.14.0.162**), local platform daemon on `5011` (TLS) / `3011` (plain). Does NOT reach the JACE directly.
- Site mini-PC `192.168.200.77` — reachable from the laptop only via a **cloudflared Access SSH** tunnel
  (`cloudflared access ssh --hostname <jump-host>`), short-lived minted cert. Reaches the JACE.
- **JACE-9000** (Tridium; MAC OUI **00:01:f0** = Tridium, device suffix redacted) — **dual-NIC**, homed on TWO
  subnets: `192.168.200.137` and `192.168.1.140`. Reached in this probe via `.137`. Station in **secure /
  TLS-only** mode.
- JACE ports (scanned from the mini-PC): **4911 Foxs OPEN · 5011 Platform-TLS OPEN · 443/80 OPEN**;
  **1911 Fox-plain · 3011 Platform-plain · 22 CLOSED**.

## Commands executed

```
# 1) SSH -L from WSL to HIGH, free localhost ports (avoid clashing with the local Supervisor on 5011/3011).
#    Transport = cloudflared Access SSH as the ProxyCommand.
ssh -f -N \
  -L 127.0.0.1:15011:192.168.200.137:5011 \   # Platform  (JACE 5011)
  -L 127.0.0.1:14911:192.168.200.137:4911 \   # Foxs      (JACE 4911)
  -L 127.0.0.1:18443:192.168.200.137:443  \   # web       (JACE 443)
  -o ProxyCommand="cloudflared access ssh --hostname %h" \
  -i <cf_key> -o CertificateFile=<cf_key-cert.pub> \
  <win-user>@<jump-host>

# 2) WSL verification — the forward reaches the JACE's own cert (not anything local):
openssl s_client -connect 127.0.0.1:15011 </dev/null 2>/dev/null | openssl x509 -noout -subject -issuer
#   subject/issuer = CN=Niagara4, O=ForRecoveryPurposes   → the JACE default self-signed cert.
#   (self-signed: subject == issuer; nothing local listens on 15011)

# 3) Windows verification — WSL2 mirrored networking shares loopback with Windows:
#    powershell> Test-NetConnection 127.0.0.1 -Port 15011   → TcpTestSucceeded : True
#                Test-NetConnection 127.0.0.1 -Port 14911   → True
#                Test-NetConnection 127.0.0.1 -Port 18443   → True
#    => Workbench (Windows) sees the WSL-side forwards.
```

## Workbench step (confirmed)

- **Open Platform** → Host `localhost`, Port `15011` → approve the JACE self-signed cert
  (`CN=Niagara4, O=ForRecoveryPurposes`) when the dialog pops.
- Operator confirmed connecting by **BOTH** paths: **Open Platform** (`localhost:15011`) AND **Open Station**
  (`foxs://localhost:14911`) — both connected and operated over the forward. The Fox leg did NOT break on a
  redirect to the JACE's real IP. (Residual: the operator did not itemize whether the Foxs cert dialog was
  separate from Platform's, nor formally measure session stability — reported "worked".)

## Operator packaging (double-click)

- `Conectar-JACE.bat` (double-click) → calls a WSL helper `tunnel-jace.sh` that (a) mints the cloudflared
  Access cert and (b) runs the `ssh -N` in the FOREGROUND. Closing the window drops the tunnel. No persistent
  daemon; the operator controls the tunnel lifetime by the window.

## Facts this probe establishes `[CERT-live]`

1. The SSH `-L` high-port forward reaches the JACE; `openssl s_client` on `localhost:15011` returns the JACE's
   `CN=Niagara4, O=ForRecoveryPurposes` cert — proof the forward lands on the JACE, not on anything local.
2. WSL2 mirrored networking shares loopback with Windows: `Test-NetConnection localhost:15011/14911/18443` all
   True from Windows → Workbench sees WSL forwards with no `netsh portproxy`.
3. cloudflared Access SSH works as the SSH `ProxyCommand` (minted short-lived cert) — the jump-host mechanism.
4. The JACE is dual-NIC (`192.168.200.137` + `192.168.1.140`); its station is TLS-only (plain 1911/3011 closed).
5. Open Platform → `localhost:15011` connected after cert approval.
6. Open Station → `foxs://localhost:14911` **also** connected + operated over the forward — Fox performed no
   redirect to the JACE's real IP (live confirmation of [Block 134] §134.10).

**Residual (not blocking)**: the operator did not itemize whether the Foxs cert-approval dialog was separate
from Platform's, nor formally measure session stability (no reconnect) — reported that both "worked".
