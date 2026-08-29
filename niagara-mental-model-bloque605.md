# Block 605 — px-menu B290-G1: the oBIX servlet IS reachable from a non-browser client that implements N4's SCRAM handshake (HTTP 200), while an HTTP Basic client is rejected (401) — because the account's scheme is `DigestScheme` (N4 "Digest" = SCRAM-SHA-256, not RFC 7616), the "bypass a Basic account" premise inverts: SCRAM is the ONLY non-browser path in

**Session**: 2026-08-29
**Focus**: `px-menu` (gap B290-G1 — reach the oBIX servlet with the DIGEST scheme from a non-browser client
implementing the SCRAM handshake, bypassing a Basic account). §12 DYNAMIC.
**Distribution / live target**: OptimizerSupervisor-N4.14.0.162, `127.0.0.1` (`DESKTOP-4AAQ77H`), `live-install`
→ SECRETS DISCIPLINE.
**Method**: READ-ONLY (§12 rung-1) paired probe — HTTP Basic vs the SCRAM-SHA-256 client (`niagara-n4-client.py`,
[B457]) — with `API2`, `no·inline`.
**Primary source**: `[CERT-live]` `sources/probes/B605-obix-scram-nonbrowser/result.txt`.
**Scope**: settle B290-G1's live question (can a non-browser reach oBIX?) and correct its "bypassing a Basic
account" framing. Reuses the [B457] SCRAM handshake; the whole B600–B604 campaign already exercised this path —
this block makes it the explicit, paired [CERT-live] proof and reads the account's actual scheme.

---

## 605.1 The paired result [CERT-live]

Same account (`API2`), same endpoint (`/obix/about`), two clients `[CERT-live]`
`sources/probes/B605-obix-scram-nonbrowser/result.txt`:

| client (non-browser) | result |
|---|---|
| HTTP **Basic** (`curl -u`) | **HTTP 401** — Authentication failed |
| **SCRAM-SHA-256** (`niagara-n4-client.py`, [B457] handshake) | **HTTP 200** — 1604 B oBIX About |

A non-browser client reaches oBIX **iff** it speaks N4's real login handshake. A generic Basic/RFC-7616-Digest
client cannot log in — matching [B457]'s finding that N4's "Digest" scheme is SCRAM-SHA-256 over the login
servlet, NOT RFC 7616.

## 605.2 The premise inverts: SCRAM is the entry, not the bypass [CERT-live]

The gap was phrased "bypassing a Basic account". The live account's scheme, read from the station `[CERT-live]`:
`API2.authenticationSchemeName = DigestScheme`. So the account is NOT a Basic account — it is a `DigestScheme`
(= SCRAM) account, and SCRAM is the ONLY way a non-browser gets in; there is no Basic credential to bypass. The
correct statement of the finding (GAP PREMISES ARE HYPOTHESES): **on a station whose users use `DigestScheme`,
a non-browser integration MUST implement the SCRAM handshake — Basic is rejected outright (401), so "bypass" is
the wrong frame; SCRAM is the sanctioned programmatic door.** (An N4 station CAN assign a `HTTPBasicScheme` to a
specific user — [B157] used one such account — in which case Basic works for that user; this station's `API2`
does not have one.)

## 605.3 Consequence for the px-menu use case [CERT-live]/[INFER]

The px-menu focus wanted oBIX data (for a dropdown/menu fed from station points) reachable from a non-browser
relay. Live answer: reachable, via SCRAM — exactly the recipe the whole B600–B604 campaign used to read the
component tree, histories, watches, and alarms. A menu relay outside Workbench therefore needs the SCRAM client,
not a Basic fetch. Whether a `.px` `WebBrowser` widget itself can carry that session is B291-G1/G2 (separate
gaps, client-render dependent) — `[INFER]` here, not tested.

---

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | Basic → 401 on /obix/about | [CERT-live] | result.txt | ✓ live |
| 2 | SCRAM-SHA-256 non-browser → 200 on /obix/about | [CERT-live] | result.txt | ✓ live |
| 3 | API2 authenticationSchemeName = DigestScheme (=SCRAM) | [CERT-live] | result.txt | ✓ live |
| 4 | N4 "Digest" = SCRAM, not RFC7616 | [CERT] | [B457] REMITTANCE | ✓ prior |

**Marker tally**: [CERT-live] ×3, [CERT] ×1, [INFER] ×1 (px-render consequence). Ratio 1/4=0.25. **Block type:
EVIDENCE (§12 live).** CLOSES B290-G1. **§12 verdict: CONFIRMED** (SCRAM reaches oBIX from non-browser) with a
PREMISE CORRECTION (no Basic account to "bypass"; SCRAM is the entry). Zero secrets. Read-only.

## Connections

- [Block 457] — the SCRAM handshake this proof reuses; N4 "Digest" = SCRAM-SHA-256.
- [Block 157] §157 — a `HTTPBasicScheme` account existed (`API`) where Basic worked; contrast with `API2`/`DigestScheme` here.
- [Block 600]–[Block 604] — the campaign that already exercised this non-browser SCRAM path end-to-end.
- px-menu focus: B290-G1 closed; B290-G2 (oBIX write, `⚠ CONFIG MUTATION`), B291-G1/G2, B292-G1 remain (write/client-render).
