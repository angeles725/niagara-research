# Block 606 — protocols P4-dyn: the live Fox SCRAM handshake byte-trace on `foxs:4911` — method `n4digest`, keyExchange `null.1` (TLS-only, no in-band key exchange), SCRAM-SHA-256 with salt=16B and **iteration count `i=10000`** — which independently CONFIRMS B457's PBKDF2-10k on the Fox channel; frame flow hello→kerberos(off)→username→challenge→authMessage1/2→welcome

**Session**: 2026-08-29
**Focus**: `protocols` (gap P4-dyn — live Fox handshake byte-trace: runtime salt/iteration/nonce/clientProof +
frame stream on 1911/4911). §12 DYNAMIC. Also refines `platform-native` N6.
**Distribution / live target**: OptimizerSupervisor-N4.14.0.162, Fox at `foxs:4911` (`1911` plain-Fox CLOSED on
this station). `live-install` → SECRETS DISCIPLINE (nonces, salt, clientProof, serverSig, and station Fox IDs
`n4Id`/`n4SuperId` REDACTED; only structure/lengths/iteration count cited).
**Method**: READ-ONLY (§12 rung-1) — reused the `niagara-fox-backup`/`niagara-fox-client.py` Fox SCRAM client
([B471], jace8000) against THIS station, `--login-only`, `no·inline`.
**Primary source**: `[CERT-live]` `sources/probes/B606-fox-handshake/fox-scram-handshake-sanitized.txt`.
**Scope**: capture the CONCRETE runtime Fox login handshake — the byte-trace [B134] documented statically. Does
NOT re-derive the Fox framing/codec/opcodes ([B134] REMITTANCE). The platform-daemon handshake (3011/5011) is a
DIFFERENT protocol and remains blocked (§606.4).

---

## 606.1 Frame flow — the live Fox login sequence [CERT-live]

Each frame is `fox a <seq> -1 fox <verb>` with a `{ key=type:value }` body. Observed live `[CERT-live]`:

1. `>> fox hello` / `<< fox hello` — `fox.version=1.0.2`; the SERVER hello discloses `app.name=Station`,
   `hostName=DESKTOP-4AAQ77H`, `n4Id`/`n4SuperId` (station Fox identity — REDACTED).
2. `<< fox kerberos` — `useKerberos=z:f` (Kerberos negotiated OFF).
3. `>> fox username` — `username=API2`, `kerbKey=z:f`.
4. `<< fox challenge` — **`method=n4digest`**, `keyExchangeMethods=null.1`.
5. `>> fox clientKeyExchangeMethod` — `keyExchangeMethod=null.1` (client accepts).
6. `>> fox authMessage1` — `authInput=authInputScram`, `authHandshake1 = n,,n=API2,r=<cnonce>` (SCRAM
   client-first; GS2 header `n,,`).
7. `<< fox authMessage1` — `authHandshake1 = r=<cnonce+snonce>,s=<salt>,i=10000` (SCRAM server-first).
8. `>> fox authMessage2` — `authHandshake2 = c=biws,r=<combined-nonce>,p=<clientProof>` (SCRAM client-final).
9. `<< fox authMessage2` — `authHandshake2 = v=<serverSignature>` (SCRAM server-final, verified).
10. `<< fox welcome` — authenticated.

## 606.2 The SCRAM parameters — measured live [CERT-live]

- **Mechanism**: SCRAM-SHA-256, labelled `n4digest` at the Fox `challenge` (the same "Digest = SCRAM" identity
  [B457] found on the web login, here on the Fox wire).
- **keyExchange = `null.1`**: NO in-band key exchange is negotiated — confidentiality is provided by the outer
  `foxs` TLS tunnel, not by a Fox-level SRP/DH exchange. (The SRP6 path [P4-srp6] is a distinct scheme; this
  login used `null.1`.)
- **Iteration count `i=10000`** — measured live. Salt = 16 bytes (base64, redacted); clientProof / serverSignature
  = 32 bytes each (SHA-256, redacted). Client nonce = 16 random bytes base64; server extends it (verified: the
  client aborts if the server nonce does not start with the client nonce).

## 606.3 Cross-channel confirmation of B457 (the load-bearing finding) [CERT-live]

[B457] established the WEB login as SCRAM-SHA-256 with `UserKeyFactory` PBKDF2-HMAC-SHA256 at **10 000**
iterations (`[CERT]` code). This block measures the FOX channel's server-first `i=10000` LIVE — an independent
confirmation, on a different protocol and port, that the station's stored key derivation is PBKDF2-HMAC-SHA256 /
10 000. Two channels (web `/prelogin` and Fox `foxs:4911`) share ONE credential store and ONE iteration count —
consistent with a single `UserKeyFactory`. This upgrades [B134]'s static SCRAM description to a byte-level
`[CERT-live]` for the concrete handshake.

## 606.4 What remains blocked [CERT-live-adjacent]

- **1911 (plain Fox)** — CLOSED on this station (probe: port closed); only `foxs:4911` is live. So the "1911 OR
  4911" scope of P4-dyn resolves to 4911-only here.
- **Platform daemon 3011/5011 handshake** (protocols Platform-live-wire / jace8000 J3-G1) — a DIFFERENT protocol
  (niagarad), gated by PLATFORM (OS-level) credentials, not the station account. [B158] found 3011/5011 return
  403 without platform creds; `API2` is a station user, not a platform user. **Remains blocked-on-platform-creds**
  — not closable with the station credential in hand.
- **P4-srp6** (SRP6 key-exchange bytes) — this login negotiated `null.1`, not SRP; SRP6 bytes need a login that
  selects that method. Untested; stays as its own gap.

---

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | Fox login frame flow hello→kerberos→username→challenge→auth1/2→welcome | [CERT-live] | fox-scram-handshake-sanitized.txt | ✓ live |
| 2 | challenge method=n4digest, keyExchange=null.1 | [CERT-live] | sanitized trace | ✓ live |
| 3 | SCRAM server-first i=10000, salt 16B | [CERT-live] | sanitized trace | ✓ live |
| 4 | i=10000 confirms B457 PBKDF2-10k on Fox channel | [CERT-live]+[CERT] | live + [B457] | ✓ cross |
| 5 | 1911 closed, only foxs:4911 live | [CERT-live] | port probe | ✓ live |
| 6 | platform daemon 3011/5011 blocked-on-platform-creds | [CERT-live] | [B158] + no platform creds | ✓ prior |

**Marker tally**: [CERT-live] ×5, [CERT] ×1. [INFER] 0. Ratio 0. **Block type: EVIDENCE (§12 live).** CLOSES
P4-dyn. **§12 verdict: CONFIRMED** — live byte-trace matches B134's static Fox SCRAM and confirms B457's
iteration count on a second channel. Zero secrets exfiltrated (nonces/salt/proof/station-IDs redacted). Read-only.

## Connections

- [Block 134] — static Fox framing + SCRAM computation; this block is its live byte-trace.
- [Block 457] — web SCRAM PBKDF2-10k; §606.3 confirms i=10000 on the Fox channel.
- [Block 471]–[Block 474] — the Fox client tool (built for jace8000) reused here against the supervisor.
- [Block 158] — platform daemon 403-without-platform-creds; why Platform-live-wire stays blocked.
- protocols focus: P4-dyn closed; P4-srp6, Platform-live-wire, P2-*, P1/P3/P5/P6 (field hardware) remain.
