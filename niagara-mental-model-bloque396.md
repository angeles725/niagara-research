# Block 396 — SP-G7: the only optional integrity channel for the local record is syslog offload — TLS-capable transport but a PLAINTEXT record, no per-message signature; nothing makes the local audit/history tamper-evident

> **Focus:** `signing-pki` (gap **SP-G7**, spawned by [B393 §393.8]). [B393] proved the local data record
> (audit, history, backup, `.bog`) is unsigned. SP-G7 asks the complementary question: does Niagara offer
> any **optional** mechanism to add integrity / tamper-evidence to that record? Answer: **no — the only
> channel is syslog offload, which gives tamper-*resistance* (off-box copy) and transit integrity via TLS,
> but never tamper-*evidence* of the local record, and does not sign the record.**
>
> **Sources `[CERT]`:** `organized/baja/baja/vineflower/com/tridium/syslog/{SyslogAuditHandler,AuditAdapter}.java`;
> `organized/platform/platform-rt/vineflower/com/tridium/platform/syslog/BSyslogTransportProtocolEnum.java`;
> corpus-wide grep for signed-audit features. **Remittance:** [B75 §75.6] (syslog offload as the post-mortem
> defense), [B393] (data unsigned), [B112] (forensics/detection).

---

## 396.1 — Enumerating the optional channels `[CERT]`

Corpus-wide grep for a signed/tamper-evident audit feature returns **zero**: no `signedAudit`,
`secureAudit`, `tamperEvident`, `auditSignature`, no RFC 5848 signed-syslog, no HMAC over audit/history in
`com.tridium.syslog` / the audit path. The **only** integrity-adjacent channel that exists is **syslog
offload** ([B75 §75.6]). `[CERT]`

## 396.2 — What syslog offload actually provides `[CERT]`

- **Transport:** `BSyslogTransportProtocolEnum` = `udp` / `tcp` / **`tls`** (default `tcp`)
  (`BSyslogTransportProtocolEnum.java:UDP=0,TCP=1,TLS=2`). Choosing `tls` gives channel confidentiality +
  integrity **in transit** and authenticates the collector — real, but transport-only.
- **The record is plaintext.** `SyslogAuditHandler.publish` builds a string —
  `priority + timestamp + host + tag + ("SECURITY_AUDIT "|"AUDIT ") + event` — and ships it via
  `Message.print(msgString)` → `logManager.publish(message)` (`SyslogAuditHandler.java:31-46`). **No
  per-message signature, no HMAC, no sequence-integrity.** The SIEM trusts whatever the station emits.
- **Timing (the one real strength):** `publish` runs **synchronously inside `audit()`, before** any local
  delete ([B75 §75.6], `BAuditHistoryService.java:98-103`). So an off-box copy exists the instant the event
  is recorded — the record leaves before an attacker can purge the local store.

## 396.3 — Resistance, not evidence `[CERT]`/`[INFER]`

Syslog offload is **tamper-resistance**: it removes the *local* single point of deletion by duplicating the
event off-box. It is **not tamper-evidence**: `[CERT]`/`[INFER]`
- It cannot prove the *local* audit/history was not altered — there is no signature or hash on the local
  record to check against (`[B393]`).
- A **compromised station** (the [B75] threat) controls what it emits: it can forge, omit, or reorder
  records before they reach the collector; a station that runs `Sys.setAuditor(null)` simply stops emitting.
  The collector authenticates the *channel* (with TLS), not the *truthfulness* of the content.
- Default-off (`BSyslogSettings.enabled=false`, [B75 §75.6]) — absent unless deliberately configured.

Net: even the one optional channel does not close the [B393] gap. Niagara's data layer has **no
tamper-evidence mechanism at all** — signed or otherwise — for the local record; the best available control
is "copy it somewhere the attacker can't reach in time." `[INFER]`

---

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | No signed-audit / tamper-evident feature exists anywhere in the code corpus | [CERT] | corpus grep `signedAudit\|secureAudit\|tamperEvident\|auditSignature` = 0 |
| 2 | Syslog is the only integrity-adjacent optional channel | [CERT] | §396.1; [B75 §75.6] |
| 3 | Syslog transport supports UDP/TCP/TLS (default TCP) | [CERT] | `BSyslogTransportProtocolEnum.java` |
| 4 | The audit record is shipped as a plaintext string, no per-message signature/HMAC | [CERT] | `SyslogAuditHandler.java:31-46` (Message.print of concatenated string) |
| 5 | publish() runs synchronously before local delete (off-box copy exists first) | [CERT] | [B75 §75.6], `BAuditHistoryService.java:98-103` |
| 6 | Offload = tamper-resistance, not tamper-evidence; compromised station controls emission | [INFER] | §396.3; [B75] threat model |

**Tally:** 5 [CERT], 1 [INFER], 0 unmarked.

## Connections
- **Closes SP-G7**, the last read-only-investigable gap of `signing-pki` → **focus reaches investigable=0**.
- **Completes [B393]:** confirms no optional integrity channel closes the data-integrity gap; the only
  control is off-box duplication (resistance), not evidence.
- **Grounds [B75 §75.6]:** the recommended syslog defense works against local deletion but not against a
  lying station — an important scoping of that recommendation.

## Open gaps (final `signing-pki` backlog — all remaining are non-read-only)
- **SP-G3** — Java `LicenseManager` rejects a bad DSA signature (native gate text-match only). `[requires-execution]`
- **SP-G6** — CRL/revocation enforcement for BACnet/SC + TLS. `[requires-execution]`
- **SP-G8** — OTA receive path enforces the ECDSA firmware chain? `[requires-execution]`
- **SP-G4** — Tridium-rooted non-OEM `baja.jar` chain. `[blocked: requires-artifact]`
- **investigable_open = 0** → focus STOP candidate; remaining gaps need a live station or a stock install.
