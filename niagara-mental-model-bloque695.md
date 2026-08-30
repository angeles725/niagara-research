# B695 — JACE_UMBRELLA OS accounts (DAR3): 7 QNX accounts, only 2 can log in, both hashed with the same PBKDF2-HMAC-SHA256 primitive as the station — no password-aging policy

> Focus: **jace-data-at-rest** · Gap **DAR3** (`/etc/passwd` + `/etc/shadow`). Sources: both files extracted
> READ-ONLY from SD P2. Redacted evidence: `sources/probes/B693-jace-data-at-rest/os-accounts.txt`.
> **SECRETS DISCIPLINE (live-install):** no hash/salt byte is shown — only the format skeleton + algorithm; a
> real-person operator username is MASKED. Retro-D1 lesson applied (mask before inspect; `grep -c` verified the
> evidence file carries zero hash bytes). Marker `[CERT-hw]` (SD artifact).

## 695.1 — /etc/passwd: 7 accounts, 5 non-login

[CERT-hw] The QNX OS carries **7 accounts**:

| account | uid | gid | shell | login? |
|---|---|---|---|---|
| root | 0 | 0 | (none) | no |
| sshd | 50 | 50 | (none) | no |
| daemon | 100 | 100 | (none) | no |
| niagarad | 200 | 200 | (none) | no |
| station | 300 | 300 | (none) | no |
| admin | 401 | 401 | /bin/sh | **yes** |
| ⟨operator⟩ (masked) | 400 | 400 | /bin/sh | **yes** |

The five system accounts (root, sshd, daemon, niagarad, station) have **no shell → no interactive login**. This
confirms the de-privileged daemon model from the RE focus: `niagarad` (uid 200) is the platform daemon that
drops privileges and refuses root ([Block 679]); `station` (uid 300) is the de-privileged station owner
([Block 683]). Even **root has no shell**. Only two accounts — `admin` (uid 401) and one named operator account
(uid 400, masked) — have `/bin/sh`.

## 695.2 — /etc/shadow: PBKDF2-HMAC-SHA256, the same primitive as the station

[CERT-hw] Only the two login accounts carry a password hash (the five system accounts have none). The hash
format, read from the delimiter skeleton with all content masked, is:

```
@<version>,<iterations:5-digit>@<base64 salt>@<base64 hash: 43 chars + '='>=      (field length 142)
```

→ **PBKDF2-HMAC-SHA256, ~10 000 iterations, 32-byte (SHA-256) output.** This is the **same cryptographic
primitive** as the station's `config.bog` admin encoder `[pbkdf2-sha256.1]=salt:10000:hash` ([Block 685]
§685.5) — just a different serialization (`@v,iter@salt@hash=` in `/etc/shadow` vs `[...]=salt:iter:hash` in the
BOG). So the JACE uses one password-hashing scheme (PBKDF2-SHA256/10k) across BOTH the OS shadow file AND the
Niagara station database. [CERT-hw for the shadow skeleton; [Block 685] for the config parity]

## 695.3 — No password-aging policy; QNX-specific last-change

[CERT-hw] Both shadow entries have `min=0 max=0 warn=0` → **no password aging, no expiration, no warning
window**. The `lastchg` field is stored in **seconds** since epoch (QNX-specific; Linux uses days) — admin's
last change ≈ 2021-01, the operator account's ≈ 2026-08. This matches the deployed-RBAC finding that no
password-policy overrides exist ([Block 688] §688.4) — here on the OS layer too.

## 695.4 — Cross-reference: the crack attempt (structure, not re-crack)

[CERT-hw]+[INFER] These two shadow hashes plus the config.bog admin hash were the three targets of the
exhausted crack attempt (hashcat `-m 10900` = PBKDF2-HMAC-SHA256, rockyou 14.3 M, **0/3 recovered**, engram
2026-08-30) — the `-m 10900` choice is now confirmed correct against the on-disk format. DAR3 documents the
STRUCTURE; it does not re-crack. The takeaway: the hashes are offline-attackable from the SD (present in
cleartext-structured form), but the PBKDF2-10k work factor plus passwords absent from a 14 M-word dictionary
left them unrecovered. Combined with DAR2 ([Block 694]): the SD yields the **reversible** secrets outright (via
`.km`), while the **login** credentials remain behind PBKDF2 — a one-way hash the card exposes but does not
by itself open.

## Connections

- De-privileged daemon/station accounts → focus `jace8000-qnx-native` [Block 679]/[Block 683]. Config admin
  PBKDF2 parity → [Block 685] §685.5; no password policy → [Block 688]. Reversible-secret exposure via the
  keyring → [Block 694] (DAR2). SD-tree location of shadow → [Block 674]. Crack attempt → engram (2026-08-30).

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | 7 OS accounts; 5 no-shell, 2 login (admin uid401, operator uid400) | [CERT-hw] | /etc/passwd | grep-confirmed |
| 2 | root has no shell; niagarad/station de-privileged (uid200/300) | [CERT-hw] | /etc/passwd + [Block 679]/[Block 683] | confirmed |
| 3 | shadow hash = PBKDF2-HMAC-SHA256 ~10k iter, 32-byte out (skeleton, masked) | [CERT-hw] | /etc/shadow skeleton | measured, hash masked |
| 4 | same primitive as config.bog admin encoder | [CERT-hw] | [Block 685] §685.5 | cross-confirmed |
| 5 | no aging (min/max/warn=0); lastchg in seconds | [CERT-hw] | /etc/shadow fields | grep-confirmed |
| 6 | -m 10900 crack 0/3, dictionary-resistant | [CERT-hw]+[INFER] | engram + format match | cross-ref |

**Tally:** [CERT-hw] ×5 · [INFER] ×1. Ratio 0.2. Block TYPE = **EVIDENCE**. No hash/salt byte in block or
evidence file (`grep -c` = 0). Real-person operator username masked throughout.

## Open gaps (this focus)

DAR3 CLOSED. Next investigable: **DAR4** (station keystores — keystore.jceks / cacerts.jceks / untrusted.jceks
/ signing/signers: what THIS unit holds — TLS default-cert private key? signing keys?). Then DAR5 (JRE crypto
policy), DAR6 (synthesis). DAR2-G1 remains requires-execution.
