# B474 — The JACE station is TLS 1.3-ONLY: the server refuses TLS 1.2 with a protocol-version alert (J11-G1 closed; §14 refines B468)

> **Focus:** `jace8000` (§16). **Gap:** J11-G1 — enumerate the accepted TLS versions ([Block 468] left it
> unverified because the local `openssl` could not offer legacy TLS).
> **Phase:** §12 dynamic (live at `192.168.1.140:443`). Read-only. `live-install`.
> **Block type: EVIDENCE (live).** **§14 refines [Block 468]** (noted back there).
> **Sources:** `[CERT-live]` Python `ssl` version-enumeration this session · [Block 468] (this focus).
>
> **Bottom line:** the station web (:443) is **TLS 1.3 only** — it **refused TLS 1.2 with a genuine server
> alert** (`TLSV1_ALERT_PROTOCOL_VERSION`) and accepted only TLS 1.3 (`TLS_AES_256_GCM_SHA384`). That is a
> *stronger* posture than [Block 468] could confirm; the earlier "TLS 1.0/1.1 unverified" caveat is now
> bounded — a server that rejects 1.2 does not accept 1.0/1.1.

## §474.1 — Measurement

Using Python's `ssl` with `minimum_version==maximum_version` pinned per version (a client that offers
individual versions where the local `openssl s_client` could not):

| Version | Result | Source of the "no" |
|---|---|---|
| TLS 1.0 | not offerable by this client | **client-side** (`NO_PROTOCOLS_AVAILABLE`) — untestable here |
| TLS 1.1 | not offerable by this client | **client-side** — untestable here |
| **TLS 1.2** | **refused by the SERVER** | **server alert** `TLSV1_ALERT_PROTOCOL_VERSION` — a real rejection |
| **TLS 1.3** | **ACCEPTED** | negotiated `TLSv1.3`, cipher `TLS_AES_256_GCM_SHA384` |

The distinction matters (the tool-failure-≠-result rule, [Block 468] applied the same care): TLS 1.0/1.1 give
a **client-side** `NO_PROTOCOLS_AVAILABLE` (this build cannot offer them), which is NOT evidence about the
server. But **TLS 1.2 produced a genuine server-side protocol-version alert** — the server actively rejects
it. A server configured TLS-1.3-only rejects every version below 1.3, so 1.0/1.1 are refused a fortiori even
though this client cannot probe them directly. Full certainty for 1.0/1.1 would need a client that offers them
(`nmap ssl-enum-ciphers`/`sslscan`, both absent locally) — but the practical answer is settled: **1.3-only**.

## §474.2 — §14 refinement to [Block 468]

[Block 468] §468.2 recorded "TLS 1.0/1.1 acceptance UNVERIFIED." This block **tightens** that: the server is
**TLS 1.3 only** (1.2 server-refused). So the transport posture is **stronger** than B468 could assert — the
weak-TLS worry is retired; the only real TLS weakness on this JACE is the **certificate** (expired default
platform cert on :5011, self-signed `ForRecoveryPurposes` on :443/:4911, [Block 468] §468.3), not the protocol
version. Remediation priority is unchanged: fix the certs; the protocol is already hardened.

## Self-verify

| # | Claim | Marker | Citation | Checked |
|---|---|---|---|---|
| 1 | :443 accepts TLS 1.3 (AES-256-GCM-SHA384) | [CERT-live] | this session | ✓ |
| 2 | :443 refuses TLS 1.2 with a server protocol-version alert | [CERT-live] | this session | ✓ TLSV1_ALERT_PROTOCOL_VERSION |
| 3 | TLS 1.0/1.1 untestable from this client (client-side no-protocols) | [CERT-live] | this session | ✓ honest bound |
| 4 | net: server is TLS 1.3-only; weak-TLS worry retired | [INFER] | from 1.2 server-refusal | reasoned, labeled |

Marker tally: [CERT-live] ×3 · [INFER] ×1 (the 1.0/1.1-refused-a-fortiori conclusion, explicitly labeled).
**Block type: EVIDENCE (live).** Ratio ≈ 0.

## Connections

- **[Block 468]** — the live security posture this refines (TLS section). **[Block 459]** — first cert/port
  sighting.

## Open gaps

**J11-G1 CLOSED** (practically — 1.3-only; a legacy-offering scanner would give the last 1% on 1.0/1.1).
Remaining child gaps are hardware/role-gated: J8-G3 (bit-48 for a non-super role), J3-G1, J5-G1, J7-G1, J2-G1.
