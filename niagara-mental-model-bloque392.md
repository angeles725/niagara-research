# Block 392 — The module-signing trust anchor, reconciled against the live install: three trust domains (not one), the real Honeywell-rooted RSA chain, and why "any N4 accepts Tridium modules" is conditional

> **⚠ §14 CORRECTION (B489/B482, 2026-08-24):** where this block names the module trust store as
> `security/truststore.jks` (`changeit`), two independent code reads correct it: the Java `CertificateChainValidator`
> ([B482]) and the native `nverify.exe` ([B489]) both show the module-verify store is a **cacerts (P12/JKS) with
> ~99 stock CA roots** and the real Tridium-specific control is an **embedded RSA-2048 TPK pin** (byte-equality
> memcmp on the signer's public key), NOT a dedicated `truststore.jks`. The `changeit` password was right; the
> store identity and the "pin" mechanism are corrected. See [B489 §489.4].

> **Focus:** `signing-pki` (BOOTSTRAP + capstone). The signature/PKI surface of Niagara N4 as a
> subsystem. This block resolves the four-way trust-anchor contradiction that the corpus left open and
> that [Block 113 §113.5] explicitly flagged as "living in the live install, not in the decompiled corpus".
>
> **Angle (declared):** reconcile the *module code-signing trust anchor* against hard on-disk evidence
> from the licensed install, separate it from the two other trust domains the corpus conflated with it,
> and extract the real end-to-end certificate chain of a shipped module.
>
> **Sources:**
> - LIVE INSTALL (Honeywell OptimizerSupervisor-N4.14.0.162, licensed) `[CERT]` — decoded with
>   `openssl`/`keytool`/`unzip`, sha256 in `sources/SOURCES.md`:
>   `security/truststore.jks`, `security/certificates/{Tridium,Honeywell,HoneywellCentraLine}.certificate`,
>   `bin/policy/signing.properties`, `jre/lib/security/{cacerts,cacerts.bcfks,*.sig}`,
>   `modules/baja.jar` (`META-INF/NIAGARA4.{SF,RSA}`).
> - CODE (decompiled corpus `/home/cristian/modules/Prototipos/modulos/organized`) `[CERT]` — the JVM
>   verification path (`ModuleFile`, `ModuleManager`, `ModuleClassLoader`, `NModule`, `NTrustStore`).
> - CORPUS remittance `[REMITTANCE]`: [B18][B75][B113][B126][B321][B384][B386] (module signing);
>   [B287] (BACnet/SC certs); [B243] (firmware); [B350–B356] (Part 11 e-signature); [B335–B349] (jsonToolkit).
>
> **Scope:** module code-signing trust anchor + the two adjacent domains it was confused with. Does NOT
> re-derive the four signing schemes ([B126 §126.1]) or the native `nverify` internals ([B321][B384]) —
> those are cited.

---

## 392.1 — The finding in one line

The corpus tracked **one** "module trust anchor" and produced four irreconcilable labels for it
(*Honeywell CodeSign RSA CA* [B18] · *Tridium/Honeywell certs in truststore.jks* [B26] · *Angeles in
cacerts.bks* [B113] · *DigiCert G4* [B126]). On disk they are **not one anchor** — they are **three
distinct trust domains** with different algorithms, files, and verifiers. Once separated, every corpus
claim is individually correct; the contradiction was a category error. `[CERT]`

| Domain | What it authenticates | Algorithm | Trust-anchor file(s) | Verifier |
|---|---|---|---|---|
| **A. Module code-signing** (current) | `.jar` class manifest | **RSA-2048/SHA-256, standard X.509 JAR signing** | `security/truststore.jks` (JKS) + build pin `bin/policy/signing.properties` | Java `java.util.jar` + `CoreCryptoManager.validateCertChain` |
| **B. Licensing + legacy vendor identity** | `.license`, `.certificate` | **DSA-1024/SHA-1** (proprietary XML wrapper) | `security/certificates/*.certificate` | `LicenseManager` / `dsfspi.dll DsfSha1WithDsaSignature` |
| **C. TLS / Authenticode / PKI** | PE binaries, TLS peers | **RSA-2048/4096, DigiCert G4** | `jre/lib/security/cacerts` + `cacerts.bcfks` | OS Authenticode / Java TLS |

The user's question ("how module signatures work; how any N4 accepts modules signed with those
signatures") lives in **Domain A**. The DSA `Tridium.certificate` everyone points at is **Domain B** —
it does **not** verify module `.jar` signatures. `[CERT]`

---

## 392.2 — Domain A: the real module-signing chain (RSA, Honeywell-rooted) `[CERT]`

The runtime opens the module jar with **standard Java JAR verification**, not a proprietary format:
`com.tridium.util.jar.ModuleFile extends java.util.jar.JarFile`, constructed `verify=true` by default
(`ModuleFile.java:22-44`); a `verify=false` open requires the `NiagaraBasicPermission("LOAD_UNVERIFIED_MODULE")`
(`ModuleFile.java:28-34`). Signers come from the standard `JarEntry.getCodeSigners()`
(`ModuleEntry.java:80-81`) populated by the JVM when the entry stream is read. `[CERT]`

Decoding the PKCS#7 block of a shipped **core** module (`modules/baja.jar` → `META-INF/NIAGARA4.RSA`,
alongside `META-INF/NIAGARA4.SF`, both dated 2024-06-14) yields the full chain: `[CERT]`

```
leaf   CN=Niagara4Modules Code Signing, O=Honeywell International Inc., C=US   RSA-2048  2023-09-06 → 9999
  ↑ issued by
CA     CN=Honeywell CodeSign RSA CA, OU=ACS, O=Honeywell International Inc.    RSA-4096  2017 → 9999
  ↑ issued by
root   CN=Honeywell Product PKI RSA, OU=ACS, O=Honeywell (self-signed)        RSA-4096  2017 → 9999
```

The leaf DN and issuer DN match `bin/policy/signing.properties` **byte-for-byte**
(`issuerDN=CN=Honeywell CodeSign RSA CA…`, `subjectDN=…CN=Niagara4Modules Code Signing`,
`serialNumber=1415098852177779243`, `notAfter=253370764800000` = year 9999, auto-generated 2024-06-14).
`signing.properties` is the **build-side pin**; the PKCS#7 chain is the **artifact-side proof** — they
agree. `[CERT]`

> **Major finding:** in this OEM build **even the core Tridium modules (`baja.jar`) are re-signed by
> Honeywell's PKI**, rooted at the self-signed `Honeywell Product PKI RSA` (RSA-4096). The trust anchor a
> Honeywell N4 needs is the Honeywell root, **not** Tridium. Confirmed identical on three independent
> `baja.jar` copies (the licensed install, the B317 pentest lab jar, and `Prototipos/modulos/baja.jar`). `[CERT]`

---

## 392.3 — Domain A trust store: `truststore.jks`, password `changeit`, one self-signed dev anchor `[CERT]`

`security/truststore.jks` is a **standard JKS** (magic `0xFEEDFEED`, provider SUN). Its integrity MAC is
protected by the **Java default password `changeit`** — `keytool -list -storepass changeit` opens it
cleanly. (Trusted-cert entries are plaintext regardless of password; a native JKS parse extracts them
**without** any password — the password only gates the integrity MAC and private-key decryption.) `[CERT]`

It contains **exactly one** entry: `[CERT]`

```
alias  = niagaramoduledev            (trustedCertEntry)
subject= C=MX, ST=CDMX, L=Mexico, O=SEJOFA, OU=Testing, CN=Security Audit   (self-signed)
key    = RSA-2048, sha256WithRSA     valid 2026-01-15 → 2027-01-15
sha256 = 83:7B:38:E8:AF:D4:F4:01:C4:82:86:CA:63:DD:E3:1E:ED:6D:37:40:7D:F7:AB:F1:15:53:EB:DA:42:4F:41:CD
```

This is the **researcher's own dev code-signing identity** (O=SEJOFA / CN="Security Audit"), added under
the `niagaramoduledev` alias so self-signed dev modules chain to a trusted anchor (links [B317] pentest /
`native/resign/` / `sejofa-dev-cert.pem`). Two corpus corrections follow: `[CERT]`
- **SEJOFA is LIVE here**, not "deprecated in favour of Angeles" as [B113 §113.2.2] inferred from
  decompiled code. In this install the active module-dev anchor is SEJOFA; no "Angeles" alias and no
  `cacerts.bks` file exists (see §392.4).
- The Honeywell/Tridium **vendor** trust anchors are **not** inside `truststore.jks` (it has only SEJOFA).
  They are the separate DSA `.certificate` files of Domain B, and the Honeywell **code-signing** root is
  the baked-in TPK managed by the non-decompiled crypto runtime (§392.6). [B26]'s "Tridium/Honeywell certs
  in truststore.jks" does **not** hold for this install. `[CERT]`

---

## 392.4 — Domain B: the DSA vendor certificates, and the ".bks" that is really ".bcfks" `[CERT]`

`security/certificates/*.certificate` are a **proprietary XML wrapper**, not X.509 files:
`<certificate version="1.0" vendor="…" generated="…" expiration="never">` wrapping a base64 **DSA-1024**
public key plus a base64 `<signature>`. Decoded: `[CERT]`

| File | vendor | generated | public key | `<signature>` |
|---|---|---|---|---|
| `Tridium.certificate` | Tridium | **2003-07-16** | DSA-1024 | 47 B = DER `SEQ{INT r, INT s}` (DSA-160) |
| `Honeywell.certificate` | Honeywell | 2006-10-12 | DSA-1024 | 46 B (DSA-160) |
| `HoneywellCentraLine.certificate` | HoneywellCentraLine | 2014-01-13 | DSA-1024 | 46 B (DSA-160) |

The Tridium key uses the **Sun/JDK default DSA-1024 domain parameters** (P begins
`fd:7f:53:81:1d:75:12:29:52:df:4a:9c:2e:ec:e4:e7…` — the well-known shared FIPS-186 example P/Q/G), not
custom parameters. Each vendor cert carries its own DSA-160 signature → a **vendor-authorization chain**.

> **⚠ CORRECTED by [B395] (2026-08-07, cryptographic re-verification):** the chain is **NOT** "rooted at a
> self-signed 2003 Tridium key". `Tridium.certificate` is **not self-signed** — all three vendor certs
> (Tridium included) verify against a **separate embedded DSA-1024 master key hidden in `baja.jar`**
> (`LicenseUtil.masterPublicKeyData`, 444 B, distinct from the 443 B on-disk Tridium key). That embedded
> key is the true Domain-B root. See [B395 §395.2].

This matches [B126 §126.1] (licenses/certs = DSA-1024/SHA-1) and
[B387] (signature verified via hardcoded DSA+ECDSA master keys). These certs authenticate **licenses and
vendor identity**, not module `.jar`s. `[CERT]`

**Resolving [B113]'s "cacerts.bks":** no `cacerts.bks` exists anywhere in the install. The only `.jks` is
`security/truststore.jks` (Domain A). What B113 loosely called ".bks BouncyCastle" is
`jre/lib/security/cacerts.bcfks` — a **BouncyCastle FIPS keystore (BC-FKS, PBES2-encrypted)** at the JRE
level (Domain C), *not* a module trust store. `[CERT]`

---

## 392.5 — Domain C, and the self-protecting truststores `[CERT]`

`jre/lib/security/cacerts` (JKS) and `cacerts.bcfks` (BC-FKS FIPS) hold the TLS/Authenticode anchors
(DigiCert G4 family, [B126 §126.4-5]). Notable: **both carry a detached `.sig` sidecar of exactly 256
bytes** (`cacerts.sig`, `cacerts.bcfks.sig`) — i.e. the JRE trust stores are themselves signed with the
**same raw RSA-2048/256-byte scheme** used for module `.jar.sig` and for the native tamper checks. The
platform signs its own trust stores. `[CERT]`

---

## 392.6 — Who verifies, when, and the two new operational facts `[CERT]`

Two Java gates, both delegating the actual chain build to the non-decompiled crypto core: `[CERT]`
1. **Add-time:** `ModuleManager.verifyModuleSignature` (`ModuleManager.java:330-404`) → per `CodeSigner`,
   `CoreCryptoManager.validateCertChain(signer, checkTpk)` (`:353`). Failure → `ModuleException`, the load
   aborts, `SEVERE "Could not verify module signature"` (`:300-313`). Module does **not** load.
2. **Class-load-time:** `ModuleClassLoader.verifyJarEntrySignature` (`ModuleClassLoader.java:374-528`),
   called before `defineClass` (`:226-228`), again `validateCertChain(codeSigner, shouldCheckTpk)` (`:417`).

**`checkTpk`** (TPK = Tridium Public Key = the baked-in trust anchor) is the second arg to every
`validateCertChain`; it is forced true when `module.xml` declares `<java-permissions>` (`NModule.java:445`)
or the entry is under `com/tridium/` or `javax/baja/` (`ModuleClassLoader.java:382`), gated globally by
`SecurityConstants.canCheckTpk()`. `[CERT]`

**New fact #1 — a failed signature is a DoS vector.** In `verifyJarEntrySignature`, any exception escaping
the try when verification was required is caught at `:520-522` and runs **`System.exit(-6)`** — an
invalid/untrusted signature on a required-verification module **kills the station process**, it does not
merely refuse the class. Not previously in the corpus. `[CERT]`

**New fact #2 — the system module truststore is protected only by `changeit`** (§392.3). Not previously in
the corpus. Combined with `ITrustStore.setCertificateEntry/deleteEntry` being mutable at runtime
([B113 §113.2.1]), anchor injection needs only filesystem/daemon access, not any crypto break. `[CERT]`

**`skipModuleValidation`** stays as [B113 §113.1] documented: sysprop **AND** license feature
`tridium/developer{skipModuleValidation}` (`ModuleClassLoader.java:552-553`), caching-once, only disables
the **cert-chain** step (`this.validateCertChain = requiresSignature && !SKIP_MODULE_VALIDATION`, `:93`) —
the `java.util.jar` integrity check still runs. `[REMITTANCE]`

---

## 392.7 — The corrected answer to the user's premise `[INFER]`

"Any Niagara N4 accepts modules signed with those signatures" is **false as an unconditional claim**. An
N4 accepts a module **iff the module's signer certificate chains to a root in *that* install's trust
anchor (TPK/system truststore)**. Tridium modules are universally accepted on **stock** N4 because
Tridium's root is the factory-default anchor in every stock install. An OEM (Honeywell) **replaces the
anchor with its own root and re-signs even the core Tridium modules** (§392.2). Therefore a Honeywell-signed
module would **not** validate on a stock-Tridium N4 (different root), and vice versa, unless the foreign
root is imported into the truststore — which is exactly what §392.3's SEJOFA anchor does for dev modules.
The "universality" is the universality of a **shared factory anchor**, not of "any signature". `[INFER]`
(Contrast against a genuine non-OEM Tridium install is **not reproduced** — all three `baja.jar` copies on
disk are Honeywell OEM builds; the Tridium-rooted case is inferred from the architecture, see gap SP-G6.)

---

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | The "module trust anchor" is three distinct domains, not one | [CERT] | §392.1 disk table; A=truststore.jks/signing.properties, B=*.certificate DSA, C=cacerts.bcfks |
| 2 | Module jars use standard `java.util.jar` verification (verify=true) | [CERT] | `ModuleFile.java:22-44`, `ModuleEntry.java:80-81` |
| 3 | Real chain: Niagara4Modules Code Signing (RSA-2048) → Honeywell CodeSign RSA CA (RSA-4096) → Honeywell Product PKI RSA (self-signed root) | [CERT] | `baja.jar META-INF/NIAGARA4.RSA` PKCS#7 decode; matches `signing.properties` |
| 4 | Even core Tridium `baja.jar` is Honeywell-signed in the OEM build | [CERT] | 3 independent baja.jar copies, identical chain |
| 5 | `truststore.jks` password = `changeit`; one self-signed SEJOFA dev anchor `niagaramoduledev` | [CERT] | `keytool -storepass changeit`; native JKS parse |
| 6 | SEJOFA is live here (corrects B113 "SEJOFA deprecated→Angeles"); no cacerts.bks exists | [CERT] | truststore.jks single entry; `fd -e bks` → none |
| 7 | `.certificate` = XML wrapper over DSA-1024, Tridium root 2003, Sun default DSA params, expiration=never | [CERT] | openssl decode of the three certs; P prefix `fd7f5381…` |
| 8 | cacerts + cacerts.bcfks each carry a 256-byte RSA-2048 `.sig` sidecar | [CERT] | `stat` on `*.sig`; file type BC-FKS |
| 9 | Invalid required-verification signature at class-load → `System.exit(-6)` | [CERT] | `ModuleClassLoader.java:520-522` |
| 10 | "Any N4 accepts any Tridium-signed module" is conditional on shared factory anchor | [INFER] | §392.7; Tridium-rooted contrast NOT reproduced (SP-G6) |
| 11 | The DSA `Tridium.certificate` does NOT verify module `.jar` signatures (Domain B ≠ A) | [CERT]/[INFER] | Domain separation §392.1/§392.4; module path uses RSA CodeSigner (claim 2/3) — [INFER] that B never touches module load |

**Tally:** 9 [CERT], 1 [INFER], 1 [CERT]/[INFER] mixed, 0 unmarked. Every central claim carries a citation.

## Connections
- **Corrects [B113 §113.2.2]** (SEJOFA→Angeles, ".bks"): SEJOFA is the live anchor here; the file is
  `truststore.jks` + JRE `cacerts.bcfks`, no `cacerts.bks`. [B113] is edited with a pointer to §392.3-4.
- **Resolves the 4-way anchor contradiction** flagged by [B113 §113.5], [B18], [B26], [B126] — each was
  describing a different domain (§392.1).
- **Extends [B126 §126.1]** (four signing schemes) with the *decoded end-to-end module chain* and the
  distinction current-RSA-X.509 (modules) vs legacy-DSA-`.certificate` (licenses/vendor).
- **Extends [B75]** (unsigned module opens 443): adds `System.exit(-6)` as the *other* failure edge and
  the `changeit` anchor-injection path.
- **Remits** native `nverify`/`checkFileSignature` to [B321][B384]; BACnet/SC certs to [B287]; Part 11 to
  [B356]; firmware to [B243]; jsonToolkit inbound-trust to [B349].

## Open gaps (seed the `signing-pki` backlog)
- **SP-G1** — Firmware supply-chain: decode the actual algorithm/format of `honFirmwarePackage` (byte-level), not just "code-signed vehicle" [B243]. `[investigable]`
- **SP-G2** — Data-integrity asymmetry (**biggest hole**): is the `.dist` backup signed or checksummed? [B33] describes it unsigned; audit/history/`.bog` carry no signature. Confirm on disk. `[investigable]`
- **SP-G3** — Native license gate `LicenseUtil::isFeaturePresent` is a **text match**, not a DSA verify [B126 §126.6]; confirm the Java `LicenseManager` actually rejects a bad DSA signature. `[requires-execution]`
- **SP-G4** — Reproduce a Tridium-rooted (non-OEM) `baja.jar` chain to settle §392.7 empirically. `[requires-artifact]`
- **SP-G5** — Verify the vendor-cert chaining canonicalization: does `Honeywell.certificate`'s DSA-160 signature verify against the Tridium root key? Needs the signed-byte canonical form. `[investigable]`
- **SP-G6** — CRL/revocation enforcement for BACnet/SC and TLS (`BIssuerCertAndCrl`, [B287]) — modelled, enforcement `[INFER]`. `[requires-execution]`
