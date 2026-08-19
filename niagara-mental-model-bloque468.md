# B468 — Live security posture of the JACE-8000: strong hardening (SSH/telnet/plaintext-Fox off, platform 403-to-GET, TLS 1.3, HSTS) undercut by default/expired certs and an exposed admin credential (focus jace8000, J11)

> **Focus:** `jace8000` (§16). **Gap:** J11 — the measured security posture of the live JACE.
> **Phase:** §12 dynamic (live at `192.168.1.140`). Read-only. `live-install` → SECRETS DISCIPLINE.
> **Block type: EVIDENCE (live).**
> **Sources:** `[CERT-live]` port/cert/header/TLS probes this session · `[CERT]` corpus [Block 398]
> (security-audit SEC checklist), [Block 459]–[Block 466] (this focus).
>
> **Bottom line:** the network hardening is **good** — SSH, telnet, and plaintext Fox are all off; the
> platform daemon refuses browsers (403-to-GET, no auth challenge); oBIX is off; the web enforces TLS 1.3 +
> HSTS + anti-clickjacking headers. The **weaknesses are the defaults**: the platform port serves an
> **expired default Tridium certificate**, the station/Fox ports serve the self-signed **`ForRecoveryPurposes`**
> cert (no real PKI), and the **`admin` credential is exposed** (pasted in chat → rotate). Weak-TLS acceptance
> (1.0/1.1) is **unverified** from this client and left as a child gap.

## §468.1 — Attack surface (ports)

`[CERT-live]` measured this session:

| Port | State | Service | Note |
|---|---|---|---|
| 22 SSH | **closed** | — | no network shell (hardening) |
| 23 telnet | **closed** | — | — |
| 80 HTTP | open | station | 302 → `/ord` (→ TLS) |
| 443 HTTPS | open | station web | TLS 1.3, HSTS, security headers |
| 1911 Fox plaintext | **closed** | — | plaintext engineering disabled (good) |
| 3011 platform HTTP | open | niagarad | 403-to-GET, no challenge |
| 4911 Fox TLS | open | station engineering | TLS-only |
| 5011 platform TLS | open | niagarad | TLS 1.3, 403-to-GET |

Only **five** ports are open and **three sensitive plaintext channels are off** (SSH, telnet, Fox-1911). The
platform daemon is reachable but not browsable ([Block 460]).

## §468.2 — Transport security

- `[CERT-live]` **:443** negotiates **TLS 1.3** (`TLS_AES_256_GCM_SHA384`) and returns
  `Strict-Transport-Security: max-age=63072000` (2 years), `X-Content-Type-Options: nosniff`,
  `X-Frame-Options: SAMEORIGIN`. **:5011** also negotiates TLS 1.3. Good modern transport defaults.
- `[CERT-live]` **:80** returns `302 → /ord` (the web funnels to the ORD navigator over TLS); no station data
  is served in the clear.
- `[CERT-live]` **TLS 1.0/1.1 acceptance is UNVERIFIED** — the local `openssl` build cannot *offer* TLS 1.0/1.1
  ("no protocols available" is a client-side refusal, not a server acceptance), so this probe cannot decide it.
  A tool that offers legacy versions (`nmap --script ssl-enum-ciphers`, `sslscan`) is required → **J11-G1**.
  (Recorded per the tool-failure ≠ result-zero rule: this is "not verified," never "weak TLS accepted.")

## §468.3 — Certificate posture (the real weakness)

`[CERT-live]`:
- **:5011 (platform)** presents the **default Tridium certificate** `CN=Niagara4, O=Tridium, C=US`, valid
  **2021-01-11 → 2022-01-11 — expired**. The platform admin channel is fronted by a stale factory cert.
- **:443 and :4911 (station/Fox)** present the self-signed **`CN=Niagara4, O=ForRecoveryPurposes, C=US`** cert,
  **regenerated 2026-08-19** (valid 1 year). `ForRecoveryPurposes` is the N4 default recovery cert — no
  organizational PKI, so TLS gives encryption but **no server-identity trust** (MITM-susceptible without cert
  pinning). Its fresh date also dates the station's (re)commissioning to today.
- `[CERT]` This matches the corpus security thesis: the `ForRecoveryPurposes` default cert is an already-cataloged
  weak-default ([Block 398] SEC checklist; [Block 459] §459.4).

## §468.4 — Credential & configuration posture

- **Exposed admin credential** — the `admin` password was pasted in chat. **Operator action: rotate it.**
  (SECRETS DISCIPLINE: not recorded here.)
- **oBIX off, /file listing gated (403), /bajaux+/wb gated** ([Block 461]) — reduced servlet surface; a
  smaller attack surface than a fully-loaded station.
- **Positive controls confirmed:** platform daemon requires the platform handshake (403-to-GET, [Block 460]);
  station data is TLS-only; engineering (Fox) is TLS-only.

## §468.5 — Posture summary

| Dimension | Verdict |
|---|---|
| Network shells (SSH/telnet) | **off** — strong |
| Plaintext protocols (Fox 1911, HTTP data) | **off / redirected** — strong |
| Platform daemon exposure | reachable but handshake-gated (403) — acceptable |
| TLS version | **1.3** confirmed; legacy 1.0/1.1 **unverified** (J11-G1) |
| Server certificates | **weak** — expired default (platform) + self-signed `ForRecoveryPurposes` (station) |
| Credentials | **admin exposed → rotate**; platform passphrase-protected ([Block 466]) |
| Remediation priority | 1) rotate admin, 2) install a real TLS cert on :443/:4911/:5011, 3) verify/disable legacy TLS |

## Self-verify

| # | Claim | Marker | Citation | Checked |
|---|---|---|---|---|
| 1 | open ports = 80/443/3011/4911/5011; 22/23/1911 closed | [CERT-live] | this session | ✓ |
| 2 | :443 TLS 1.3 + HSTS 2y + nosniff + X-Frame SAMEORIGIN | [CERT-live] | this session | ✓ |
| 3 | :80 → 302 /ord | [CERT-live] | this session | ✓ |
| 4 | :5011 default Tridium cert expired 2022 | [CERT-live] | this session | ✓ openssl x509 |
| 5 | :443/:4911 ForRecoveryPurposes self-signed, regenerated 2026-08-19 | [CERT-live] | this session | ✓ |
| 6 | TLS 1.0/1.1 acceptance UNVERIFIED (client cannot offer) | [CERT-live] | this session | ✓ honest non-result |
| 7 | ForRecoveryPurposes = cataloged weak default | [CERT] | [Block 398] | ✓ corpus |

Marker tally: [CERT-live] ×6 · [CERT] ×1 (corpus) · [INFER] 0 load-bearing. **Block type: EVIDENCE (live).**
Ratio ≈ 0. RE-MEASURE applied: the TLS-1.0/1.1 result was re-tested and downgraded from a false "accepted" to
"unverified."

## Connections

- **[Block 459]** (ports/certs first sighting) · **[Block 460]** (platform 403) · **[Block 461]** (servlet
  surface) · **[Block 466]** (passphrase) · **[Block 398]** (`security-audit` — the corpus checklist this
  live posture instantiates on a JACE).

## Open gaps

New child **J11-G1** (requires-execution): enumerate accepted TLS versions/ciphers with `nmap
ssl-enum-ciphers`/`sslscan` (this client cannot offer legacy TLS). Queued: J9.

> **§14 refined in [Block 474]:** J11-G1 closed — the station :443 is **TLS 1.3 ONLY** (the server refuses TLS 1.2 with a protocol-version alert). The "1.0/1.1 unverified" caveat above is tightened: a 1.3-only server rejects all older versions. The weak-TLS worry is retired; the cert weakness (§468.3) stands.
