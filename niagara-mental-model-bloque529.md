# B529 — signing-pki SP-G9a CLOSED: live provider order — `DSA`/`SHA1withDSA` resolve to BC (bcstd 1.7801, BouncyCastleProvider), NOT BouncyCastleFipsProvider; §14-refines B441/B524's FIPS attribution

**Focus:** `signing-pki` · **Gap:** SP-G9a (live `Security.getProviders()` order) · **Mode:** §12 dynamic (observer agent via `-@javaagent`, read-only) · **Language:** English.

**Scope.** The live effective JCE provider order of the running `nre` JVM, captured with a standard
`-javaagent` observer (the Frida-independent path proven in [B528]). It **refines** the provider
attribution for the license DSA verify: the static `bin/policy/java.security` lists `provider.1=BCFKSWrap,
provider.2=BouncyCastleFipsProvider, provider.3=Sun` ([B441]), but the LIVE order puts the **general**
BouncyCastle `BC` (bcstd 1.7801) first, and `DSA`/`SHA1withDSA` resolve to it. SECRETS DISCIPLINE observed.

**Evidence.** `codegen/spg10-frida/javaagent/ProviderOrderProbe.java`; run transcript (below, captured live).
Markers per §3.

---

## 1. The live order (observer agent, `nre -@javaagent:…=providers -licenses`) — `[CERT-live]`

```
[g9a-probe] 1: BC 1.7801  (org.bouncycastle.jce.provider.BouncyCastleProvider)
[g9a-probe] 2: SUN 1.8   (sun.security.provider.Sun)
[g9a-probe] 3: SunRsaSign 1.8
[g9a-probe] 4: SunEC 1.8
[g9a-probe] 5: SunJCE 1.8
[g9a-probe] 6: SunJGSS 1.8
[g9a-probe] 7: SunSASL 1.8
[g9a-probe] 8: XMLDSig 2.14
[g9a-probe] 9: SunPCSC 1.8
[g9a-probe] 10: SunMSCAPI 1.8
[g9a-probe] 11: BCJSSE 1.0019 (org.bouncycastle.jsse.provider.BouncyCastleJsseProvider)
[g9a-probe] DSA          -> BC (org.bouncycastle.jce.provider.BouncyCastleProvider)
[g9a-probe] SHA1withDSA  -> BC (org.bouncycastle.jce.provider.BouncyCastleProvider)
[g9a-probe] SHA256withDSA -> BC (org.bouncycastle.jce.provider.BouncyCastleProvider)
```

## 2. What this refines — `[CERT]`/`[CERT-live]`

- **[B441]'s static provider list is the SHIPPED policy file, not the live effective order.** The
  shipped `bin/policy/java.security` puts `provider.1=BCFKSWrap, provider.2=BouncyCastleFipsProvider,
  provider.3=Sun`, but the LIVE JVM has `BC` (general, bcstd `bcprov-jdk18on-1.78.1`, dir
  `bin/ext/bcstd/`) at position 1 and does NOT expose `BCFKSWrap` or `BouncyCastleFipsProvider` in the
  effective list. This matches [B440]'s "BC registered dynamically at boot; JRE java.security is stock".
- **The license DSA verify is BouncyCastle-GENERAL (`BC` of bcstd), not BC-FIPS.** [B524] F1 said
  "BC-FIPS Java-side" — that attribution was too specific; the live resolution is
  `org.bouncycastle.jce.provider.BouncyCastleProvider` (general, 1.7801). The FIPS jars exist on disk
  (`bin/ext/bcfips/bc-fips-1.0.2.5.jar`) but are not the provider that wins `DSA` here.

## 3. Why the discrepancy is coherent (not a contradiction) — `[INFER]` grounded in both sides

Niagara launcher conditionally puts `bcstd` (no `fips140-2` feature, per [B440]) on the classpath and
registers the general provider at boot, ahead of the policy file's FIPS entries. The static file is the
*declared* template; the live registration order is the *effective* one. SP-G9b (the `fips140-2` branch)
would presumably flip this to the FIPS provider — that branch remains blocked-on-artifact.

## 4. Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | Live provider order captured | `[CERT-live]` | ProviderOrderProbe transcript above |
| 2 | DSA/SHA1withDSA -> BC (bcstd general), not FIPS | `[CERT-live]` | the three resolution lines |
| 3 | bcstd 1.7801 on the classpath; bcfips 1.0.2.5 exists but doesn't win | `[CERT]` | bin/ext/bcstd/bcprov-jdk18on-1.78.1.jar; bin/ext/bcfips/bc-fips-1.0.2.5.jar |
| 4 | Refines [B441]/[B524] provider attribution (not the crypto-sound conclusion) | `[CERT]` | [B441] policy lines vs live dump |

**Tally:** 2 `[CERT-live]`, 2 `[CERT]`, 1 `[INFER]` (explicit, grounded). No unmarked claims.

## 5. Connections & gap bookkeeping

- **Closes SP-G9a** (live `Security.getProviders()` now confirmed `[CERT-live]`).
- §14-REFINES [B524] F1's "BC-FIPS Java-side" → "BC **general (bcstd)** Java-side"; [B441] stays correct
  as the *shipped* policy, marked not-effective.
- Feeds SP-G9b (which is the FIPS-branch twin, still blocked-on-artifact).
- Open items: SP-G6, SP-G8 (+ blocked SP-G3a, SP-G4, SP-G9b).
