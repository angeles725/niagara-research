# B696 — JACE_UMBRELLA station keystores (DAR4): the TLS keypair is the factory ForRecoveryPurposes self-signed cert; the trust/untrusted stores are empty

> Focus: **jace-data-at-rest** · Gap **DAR4** (station keystores: keystore.jceks / cacerts.jceks /
> untrusted.jceks / signing/signers). Sources: the four keystore files extracted READ-ONLY from SD P2
> (`/home/niagara/security/`). Redacted evidence: `sources/probes/B693-jace-data-at-rest/keystores.txt`.
> **SECRETS DISCIPLINE:** only public cert-subject metadata + format cited; no private key or keystore password
> extracted. Marker `[CERT-hw]` (SD artifact). Module-signing/PKI MODEL = REMITTANCE focus `signing-pki`
> [Block 392]–[Block 396]; DAR4 records THIS unit's deployed keystore inventory (those blocks don't cite these
> exact files).

## 696.1 — keystore.jceks: the TLS keypair = factory self-signed cert

[CERT-hw] `keystore.jceks` (4476 B) is a **JCEKS** keystore (magic `CE CE CE CE`). It holds the alias
**`default`** — the station's TLS keypair used by FoxService `foxsCert="default"` and WebService
`httpsCert="default"` ([Block 685] §685.3). Its public cert-subject tokens are `Niagara4` / **`ForRecoveryPurposes`**
/ `tridium` / `Tridium`. That subject is the **factory default self-signed TLS certificate** (the
"ForRecoveryPurposes" cert Niagara ships), NOT a CA-signed or site-provisioned cert.

This is the **on-disk confirmation** of the live-posture weakness the `security-audit` focus flagged on the
supervisor ([Block 398]: "cert TLS default ForRecoveryPurposes") and the JACE live verdict ([Block 468]): the
TLS-1.3-only transport (B685) terminates on a **self-signed factory cert**, so it protects confidentiality but
provides no real server-identity assurance — a client cannot distinguish this JACE from a MITM without pinning.
The private key for `default` lives in this keystore on the card (value NOT extracted).

## 696.2 — cacerts.jceks + untrusted.jceks: empty

[CERT-hw] Both `cacerts.jceks` (32 B) and `untrusted.jceks` (32 B) are **JCEKS keystores with zero entries**
(magic `CE CE CE CE` + the ~32-byte empty-keystore envelope). So this station has added **no custom trust
anchors** (cacerts.jceks empty) and marked **nothing explicitly untrusted** (untrusted.jceks empty) — the
station-level cert trust is entirely the JRE defaults (`cacerts` / `cacerts.bcfks`, DAR5) plus the vendor
`.certificate` chain. Consistent with the template-seed profile (focus `jace-station-config` [Block 692]).

## 696.3 — signing/signers: the module-signing store (remittance)

[CERT-hw] `signing/signers` (33021 B) is NOT a JCEKS (magic `00 2E 5B 61`) — it is the module-signing signer
store (the trust anchors used to verify JAR/module signatures). Its trust model — the Honeywell/Tridium →
DigiCert chain and the re-signing on OEM units — is REMITTANCE to focus `signing-pki` [Block 392]/[Block 395]
and the SD factory-image chain [Block 676]. DAR4 records only that the deployed signer store is present and
non-empty (33 KB) on this unit; the chain itself is already characterized.

## 696.4 — DAR4 takeaway for the data-at-rest question

[CERT-hw]+[INFER] The keystores add one at-rest exposure and one non-exposure:
- **Exposure:** the TLS **private key** for the `default` server cert sits in `keystore.jceks` on the card —
  SD possession yields it (protected only by the keystore password, itself derivable via the on-disk keyring,
  DAR2 [Block 694]). But because it is only the **factory ForRecoveryPurposes self-signed** key, its
  compromise impersonates a controller that already offers no real identity assurance — low marginal value.
- **Non-exposure:** no site-specific CA keys or private trust material was added (empty cacerts/untrusted).

## Connections

- TLS cert alias `default` in the deployed config → [Block 685] §685.3; default-cert weakness (live) →
  focus `security-audit` [Block 398] / [Block 468]. Keystore password derivable via keyring → [Block 694]
  (DAR2). Module-signing/PKI model → focus `signing-pki` [Block 392]/[Block 395]/[Block 676]. JRE default
  trust stores → DAR5 (next).

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | keystore.jceks = JCEKS, alias 'default' = factory ForRecoveryPurposes self-signed TLS cert | [CERT-hw] | magic CECECECE + subject strings | grep-confirmed |
| 2 | cacerts.jceks + untrusted.jceks are empty JCEKS (32 B each) | [CERT-hw] | size + magic | measured |
| 3 | signing/signers = non-JCEKS module-signer store (33 KB), model=remittance | [CERT-hw] | magic 002E5B61 | measured |
| 4 | TLS private key on card but only the factory self-signed key (low marginal value) | [CERT-hw]+[INFER] | 696.1 + [Block 398] | reasoned |

**Tally:** [CERT-hw] ×3 · [INFER] ×1. Ratio 0.33. Block TYPE = **EVIDENCE**. No private key / password byte in
block or evidence file (`grep -c` long-b64 = 0). Cert subjects are public metadata.

## Open gaps (this focus)

DAR4 CLOSED. Next investigable: **DAR5** (JRE crypto policy — java.security, cacerts.bcfks BC-FIPS, policy
limited vs unlimited — largely REMITTANCE to signing-pki; will close on the deployed delta). Then DAR6
synthesis. DAR2-G1 stays requires-execution.
