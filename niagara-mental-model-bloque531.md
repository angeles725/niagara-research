# B531 — signing-pki SP-G6 CLOSED: BACnet/SC peer-cert path sets `PKIX … setRevocationEnabled(false)` — CRL infra is MODELLED but revocation is NOT enforced (upgrades B287's [INFER] to [CERT])

**Focus:** `signing-pki` · **Gap:** SP-G6 (CRL/revocation enforcement for BACnet/SC + TLS) · **Mode:** static code-read · **Language:** English.

**Scope.** The question "is revocation enforced?" now answered at source level for the BACnet/SC
certificate path: the CRL *infrastructure* exists (`BIssuerCertAndCrl`, `BCrlDescriptor`, `addCRLs`) — the
model is real — but the path-build verifier explicitly disables revocation. This upgrades [B287]'s
`[INFER]` on enforcement to a `[CERT]` on the code. SECRETS DISCIPLINE observed (no secret material).

**Evidence.** `organized/bacnet/bacnet-rt/{decompiled,vineflower,pipeline/procyon}/com/tridium/bacnet/stack/link/sc/authentication/BBacnetScAuthenticator.java` (all three agree). Markers per §3.

---

## 1. The modelled side: CRL infra is present and real — `[CERT]`

- `BIssuerCertAndCrl` (`…/sc/authentication/BIssuerCertAndCrl.java`) carries `crlDistributionPointUrls`,
  a `crlDescriptor` (scheduled `BCrlDescriptor` — default daily 02:00), a `checkCrlExpiration` action, and
  a generated CRL filename (`CRL_EXTENSION = ".crl"`); it parses `java.security.cert.X509CRL` via
  `CertificateFactory`.
- `BBacnetScAuthenticator.addCRLs(Set<X509CRL>)` fans CRLs out to each `BScLinkLayer`. So refresh +
  distribution are modelled — B287's reading of the type name was correct.

## 2. The enforced side: revocation is switched OFF in verify — `[CERT]`

`BBacnetScAuthenticator.verify(List<? extends Certificate> sortedClientCertChain)` builds the path with:

```java
PKIXBuilderParameters params = new PKIXBuilderParameters(trustAnchors, new X509CertSelector());
params.setRevocationEnabled(false);                    // <— revocation disabled
params.addCertStore(CertStore.getInstance("Collection", new CollectionCertStoreParameters(sortedClientCertChain)));
CertPathBuilder builder = CertPathBuilder.getInstance("PKIX");
PKIXCertPathBuilderResult result = (PKIXCertPathBuilderResult)builder.build(params);
```

So for the **peer certificate chain** the PKIX builder is told **not** to check revocation — a revoked
(but cryptographically-valid) peer certificate still verifies. The CRLs that `addCRLs` fans out are never
consulted by this `verify` path (there is no `PKIXRevocationChecker` and no `CertStore` carrying the CRLs
into `params`).

## 3. TLS sibling — `[CERT]` (prior evidence, cited)

[B482] already recorded the same posture for the module/`CertificateChainValidator` path
(`validateCertChain = PKIX, revocation DISABLED`). BACnet/SC and the Niagara TLS/module chain therefore
share the same gap: **trust-anchor + chain validation yes, revocation no.**

## 4. Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | CRL infra exists (issuer slot + daily descriptor + addCRLs fan-out) | `[CERT]` | BIssuerCertAndCrl.java fields/actions; BBacnetScAuthenticator.addCRLs:140-144 |
| 2 | PKIX verify disables revocation | `[CERT]` | BBacnetScAuthenticator.java:94 (vineflower) `params.setRevocationEnabled(false)` — same at :101 (cfr), :89 (procyon) |
| 3 | TLS/module chain parity (also disabled) | `[CERT]` | [B482] validateCertChain revocation DISABLED |
| 4 | Upgrades B287 [INFER]→[CERT] on enforcement | `[CERT]` | the code lines above vs B287 §2's "modelled, not just trust [CERT]/[INFER]" |

**Tally:** 4 `[CERT]`, 0 `[CERT-live]`, 0 `[INFER]`. No unmarked claims.

## 5. Connections & gap bookkeeping

- **Closes SP-G6.**
- §14-UPGRADES [B287] (revocation was `[INFER]`; now `[CERT]` disabled).
- Feeds the hardening checklist: enabling `setRevocationEnabled(true)` + wiring the CRL `CertStore` into the
  PKIX params is a concrete remediation (same family as H-items); note this is a *vendor code* change unless
  a property switch exists (none observed in this path).
- With SP-G6 closed, `signing-pki` has **no open investigable/executable gaps**: only blocked-on-artifact
  SP-G3a / SP-G4 / SP-G9b remain.
