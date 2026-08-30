# B698 — What the SD yields (DAR6, focus synthesis): physical possession = near-total data-at-rest compromise, because the data-encryption key is a cleartext file on the same card the hardware root-of-trust protects

> Focus: **jace-data-at-rest** · Gap **DAR6** (synthesis) — FOCUS-CLOSING block. Sources: consolidates
> [Block 693]–[Block 697] + [Block 466]/[Block 684]. Block TYPE = **SYNTHESIS** (high [INFER] ratio expected).
> **SECRETS DISCIPLINE:** no secret used. Marker mix cites already-verified blocks.

## 698.1 — The recoverable-from-SD-alone ledger

[CERT-hw across DAR1–DAR5] With physical possession of the boot microSD and nothing else (offline, no live
hardware), an attacker gets:

| asset | on the SD? | protection | outcome |
|---|---|---|---|
| full station config structure (services/drivers/points/RBAC layout) | yes, cleartext BOG XML | none (only BPassword fields encrypted) | **exposed** (focus `jace-station-config` B685–B692) |
| the machine key `/etc/km/.km` | yes, **32-byte cleartext** | none | **exposed** (DAR1 B693) |
| the station KeyRing `.kr` + filesystem key `.fskey` | yes | wrapped by `.km` (also on card) | **unwrappable** (DAR1/DAR2) |
| config.bog reversible / `BPassword` field VALUES | derived | machine-key domain — key is `.km`, on card | **decryptable offline** (DAR2 B694; PoC = DAR2-G1 req-exec) |
| TLS server private key (`keystore.jceks` alias `default`) | yes | keystore pw (derivable via keyring) | **exposed**, but only the factory self-signed cert → low value (DAR4 B696) |
| all audit / security / log / alarm data (`.hdb`, `.adb`) | yes, cleartext | none, no off-box replica | **exposed** (B689) |
| OS + station LOGIN passwords (admin, operator, station users) | hashes only | **PBKDF2-HMAC-SHA256 ~10k, one-way** | **NOT reversible**; offline-attackable but dictionary-resistant (0/3 vs rockyou, DAR3 B695) |

## 698.2 — The one real wall, and why the hardware root-of-trust does not help here

[CERT-hw]+[INFER] The **only** asset the SD does not hand over outright is the set of **login-password hashes**
(DAR3): PBKDF2-SHA256 at ~10k iterations is one-way, and the actual passwords were absent from a 14 M-word
dictionary. That gates *interactive login*. It does **not** gate the *reversible secrets*, because those use the
machine key `.km`, which is a cleartext file on the card (DAR2).

The JACE-8000 has a genuine hardware root-of-trust — secure boot, encrypted firmware, the ATECC508 secure
element, de-privileged root-refusing daemons (focus `jace8000-qnx-native` [Block 684]). **None of it protects
data at rest on the removable card**, because:
- the ECC508 anchors **boot attestation and 802.1X**, not the data-encryption path (DAR2 [Block 694]);
- the data-at-rest key chain (`.km` → `.kr` → reversible key) is **entirely software, entirely on the SD**;
- so lifting the card **sidesteps the entire hardware root-of-trust** — none of those controls is in the
  data-at-rest path.

This is the mechanism behind [Block 684]'s verdict ("strong boot/firmware/process, WEAK data-at-rest") and the
§14 refinement of [Block 466] (DAR2): "non-exportable machine key" is a property of the platform TOOLS, not of
the storage medium.

## 698.3 — Verdict + operator actions

[INFER] **Physical possession of the JACE-8000 boot microSD = near-total compromise of the controller's
data-at-rest.** Everything the reversible-encoding layer protects is recoverable; only the one-way login hashes
resist, and only to the strength of the passwords. The removable, unencrypted-key nature of the boot media is
the exposure — there is no software mitigation available while the machine key lives on the card.

Operator actions (consolidated, this focus):
- Treat **physical custody of the SD** as the actual security control — it is the only one in this path.
- If the card was ever out of custody: **rotate everything** — OS `admin`/operator, station admin ([Block 468]),
  and any reversible secrets (integration passwords, the TLS cert if it were ever a real one). The machine key
  and reversible-field values must be assumed compromised on physical loss.
- The login hashes bought time (dictionary-resistant) — do not rely on it; assume crackable given a targeted
  wordlist, and rotate regardless.
- The TLS `default` cert is the factory self-signed ForRecoveryPurposes cert (DAR4) — replace with a real
  CA/site cert for genuine server identity (independent of the SD issue).

## Connections

- Consolidates DAR1 [Block 693] · DAR2 [Block 694] · DAR3 [Block 695] · DAR4 [Block 696] · DAR5 [Block 697].
  Weak-data-at-rest verdict mechanized → [Block 684]; §14 refine → [Block 466]; SD=compromise assertion →
  [Block 674]; deployed config → focus `jace-station-config` [Block 692]; rotate-admin → [Block 468].

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | SD yields config structure, machine key, reversible secrets, TLS key, audit data | [CERT-hw] | DAR1-4 + B689 | consolidated |
| 2 | only login hashes resist (one-way PBKDF2, dictionary-resistant) | [CERT-hw] | DAR3 B695 | cited |
| 3 | hardware root-of-trust (ECC508/secure boot) not in the data-at-rest path | [CERT]+[INFER] | DAR2 B694 / [Block 684] | reasoned |
| 4 | verdict: physical SD = near-total data-at-rest compromise; custody is the control | [INFER] | 698.1-698.2 | synthesized |

**Tally:** [CERT-hw] ×2 · [CERT] ×1 · [INFER] ×3. Ratio high — EXPECTED for SYNTHESIS. No secret used. All
facts cite already-verified blocks.

## Focus status

**DAR6 CLOSED → jace-data-at-rest investigable = 0 → focus STOP.** 6/7 investigable gaps closed (DAR1–DAR6);
one child gap **DAR2-G1** remains **requires-execution** (the actual decryption PoC — implement the KeyRing
unwrap with `.km` + Mocana AES-256-CBC to decrypt a config.bog BPassword field). No blocked-on-hardware gaps.
Next: §18 self-retrospective + push.
