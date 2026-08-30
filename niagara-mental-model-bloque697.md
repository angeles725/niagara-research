# B697 — JACE_UMBRELLA JRE crypto policy (DAR5): standard non-FIPS OpenJDK stack, unlimited-strength by default, weak TLS/algorithms disabled — FIPS off across every layer

> Focus: **jace-data-at-rest** · Gap **DAR5** (JRE crypto policy: java.security, cacerts.bcfks, policy jars).
> Source: `/opt/niagara/jre/lib/security/java.security` (SD P2, config — non-secret). Evidence:
> `sources/probes/B693-jace-data-at-rest/jre-crypto-policy.txt`. Marker `[CERT-hw]` (SD artifact). Crypto-provider
> / BC-FIPS / cacerts.bcfks MODEL = REMITTANCE focus `signing-pki` [Block 395]; DAR5 records this unit's active
> config. Thin block (mostly confirmation + remittance).

## 697.1 — Unlimited-strength crypto, standard providers

[CERT-hw] `crypto.policy` is **not set explicitly** in java.security → the JRE default applies, which for a
modern JDK (9+) is **unlimited**; both `policy/limited/` and `policy/unlimited/` jars ship on disk. The provider
list is the **standard Sun/OpenJDK stack** (9 providers: Sun, SunRsaSign, SunEC, SunJSSE, SunJCE, SunJGSS,
SunSASL, XMLDSig, SunPCSC) — **no Bouncy Castle FIPS provider is forced**. `keystore.type=jks`
(`keystore.type.compat=true`).

## 697.2 — Weak TLS/algorithms disabled

[CERT-hw] `jdk.tls.disabledAlgorithms` disables **SSLv3, TLSv1, TLSv1.1, RC4, DES, MD5withRSA** (and more) — a
modern hardening default consistent with the station's TLS-1.3-only transport ([Block 685] §685.3). So even the
JRE-level TLS floor rejects the legacy protocols.

## 697.3 — FIPS off across every layer (the consistent thread)

[CERT-hw]+[INFER] `cacerts.bcfks` (the BC-FIPS keystore, focus `signing-pki` [Block 395]) is present on disk but
is **not the active provider path** — the java.security provider list is the standard SunJCE stack, not
BCFIPS. This confirms **FIPS is off at the JRE layer**, matching `FIPSEnabled="false"` in the station config
([Block 685] §685.1) and the platform.bog defaults ([Block 692] §692.1). The FIPS machinery is SHIPPED
(BC-FIPS keystore, both policy jars) but not ENGAGED — the same "capable-but-not-turned-on" pattern seen across
the whole `jace-station-config` focus. (Contrast: the JACE-9000 is FIPS-by-requirement, focus `jace9000`
[Block 657].)

## Connections

- cacerts.bcfks / BC-FIPS keystore model → focus `signing-pki` [Block 395]. FIPS-off in station config →
  [Block 685] §685.1; platform defaults → [Block 692] §692.1. TLS-1.3-only transport → [Block 685] §685.3.
  FIPS-required sibling → focus `jace9000` [Block 657].

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | crypto.policy unset → unlimited default; both policy jars shipped | [CERT-hw] | java.security + p2-tree | grep-confirmed |
| 2 | standard 9 Sun providers, no BCFIPS forced | [CERT-hw] | security.provider.1-9 | grep-confirmed |
| 3 | disabledAlgorithms drops SSLv3/TLSv1/1.1/RC4/DES/MD5 | [CERT-hw] | jdk.tls.disabledAlgorithms | grep-confirmed |
| 4 | FIPS shipped but not engaged (bcfks present, SunJCE active) | [CERT-hw]+[INFER] | provider list + [Block 685] | reasoned |

**Tally:** [CERT-hw] ×3 · [INFER] ×1. Ratio 0.33. Block TYPE = **EVIDENCE** (config read, remittance-heavy).
Config file, no secrets. 3/3 structural claims grep-confirmed.

## Open gaps (this focus)

DAR5 CLOSED. Next investigable: **DAR6** — the focus-closing SYNTHESIS (exactly what SD possession yields vs
what needs live hardware). After DAR6, investigable → 0 → focus STOP (DAR2-G1 stays requires-execution).
