# Block 482 — The Java crypto/trust internals behind licensing: the module-signature verifiers (JarSignatureRegistry → CertificateChainValidator PKIX + the embedded RSA-2048 TPK), the KeyRing/AES-256-GCM at-rest envelope, and LicenseUtil.verify/encode byte-exact canonicalization

> **Focus:** `licensing` (Java deep-dive half of the parallel pass with sibling session @Segundo, who takes the
> native `dsfspi`/`nverify` half). READ-ONLY, decompiled source only; no binary run. Markers per §3.
>
> **Sources:** `sources/decompiled/nre-ext/com/tridium/crypto/core/**` (verifiers, truststore, TPK),
> `sources/decompiled/nre-ext/com/tridium/nre/security/**` (KeyRing/AES), `organized/baja/…/com/tridium/sys/
> license/{LicenseUtil,LicenseFile,CertificateFile}.java`.

## §482.1 — Module-signature verifiers (nre.jar `com.tridium.crypto.core`) `[CERT]`

- **`JarSignatureRegistry`** — obtains signers via a `JarVerifier`, not `JarEntry.getCodeSigners()` directly:
  `codeSigners = new JarVerifier(file).verify()` (`cert/JarSignatureRegistry.java:401-402`), interned/deduped
  by `CertPath` (`:410-424`), cached by absolute path with mtime freshness (`:430,439-443`). Startup scan of
  `NiagaraFiles.getModulesPath()` for `.jar`/`.sjar` (`:62-103,350-351`). The registry file
  `<securitySigningPath>/signers` is stored **encrypted** via `KeyRingEncryptingOutputStream` (`:221`) /
  read via `KeyRingDecryptingInputStream` (`:153`); `VERSION=1`, CertPaths Java-serialized (`:256`).
- **`CertificateChainValidator.validateCertChain(CodeSigner, checkTpk)`** (`cert/CertificateChainValidator.java:120-157`):
  sorts the chain, builds an `X.509` CertPath, runs `CertPathValidator("PKIX")` with
  `PKIXParameters(trustAnchors)`, **`setRevocationEnabled(false)`** (no CRL/OCSP, `:146`), date = timestamp or
  now. **Trust anchors = system cacerts + user cacerts** (`rebuildTrustAnchors` from
  `cryptoManager.getSystemTrustStore()` + `getUserTrustStore()`, `:57-63`). Expiry via
  `codeSignerEnd.checkValidity` (`:159-177`); timestamp chain needs EKU `1.3.6.1.5.5.7.3.8`.
- **`checkTpk` semantics** (`:200-229`): after PKIX success, **only if `SecurityConstants.canCheckTpk()`** it
  calls `SecurityConstants.checkTpk("", leafCert)` and records `tpkChecked` — the TPK pin applies to the
  **leaf/end-entity** signer and is enforced only in a release build.

## §482.2 — The baked-in TPK = a hardcoded RSA-2048 key `[CERT]`

`SecurityConstants.java:12-307` is a hardcoded `byte[]` = an **RSA-2048 `SubjectPublicKeyInfo`** (DER
`30 82 01 22 … rsaEncryption … 02 03 01 00 01`, exponent 65537). `canCheckTpk() = TPK.length != 0` (`:312-314`);
`checkTpk(name, cert)` **byte-compares** `cert.getPublicKey().getEncoded()` to the TPK → `SecurityException("…
violates integrity check")` on mismatch (`:316-323`). The TPK doubles as the **cacerts integrity key**:
`CoreCryptoManager.verifyCacertsSignature` verifies a detached `cacerts.sig` with `SHA256withRSA` using the TPK
as an RSA public key (`CoreCryptoManager.java:900-917`). Also defines `AES_TRANSFORMATION="AES/GCM/NoPadding"`,
`GCM_T_LEN=128`.

- **System truststore** = `<java.home>/lib/security/cacerts` (`.bcfks` under FIPS), password
  `"jiUz!rHw2d8&i3kI"` (or `"changeit"` fallback) (`CoreCryptoManager.java:771-786`).
- **User truststore** = `<securityDir>/cacerts`, store password = the keyring key
  (`SecurityUtil.toHexChars(getKeyRingKey())`, `CoreStore.java:59-62`, keyName
  `"com.tridium.crypto.io.CoreKeyStore[UserTrustStore]"`).
- **⚠ Refinement to reconcile with [B392] (NOT asserted as a correction — child gap B482-G1):** [B392] named
  the module trust store as `security/truststore.jks` (password `changeit`). This deeper read of the ACTUAL
  `validateCertChain` shows the PKIX anchors are **system + user `cacerts`** plus the **RSA-2048 TPK leaf-pin**.
  These may be different stores serving different roles, or an earlier conflation. Recorded as-found `[CERT]`;
  B482-G1 opened to reconcile rather than overwrite B392.

- **`ASN1HostId`** (`cert/ASN1HostId.java`): an X.509 attribute, OID `1.3.6.1.4.1.4131.2` (Tridium IANA PEN
  4131), value `DERUTF8String(hostId)` in a `[0]` tagged object — the Niagara Host ID embedded into a cert so
  certs can be **bound to a specific host**.

## §482.3 — At-rest crypto: the KeyRing / AES-256-GCM envelope `[CERT]`

- **`Aes256PasswordManager`** (`nre/security/Aes256PasswordManager.java`): AES `"AES/GCM/NoPadding"`, tag 128 bit
  (`GCMParameterSpec(128, iv)`), legacy `"AES/CBC/PKCS5Padding"`. The AES key is **fetched from the KeyRing**
  (not password-derived): `getKey(name)` → `kr.getKey(name)`, auto-create if absent (`:193-206`).
  `AesAlgorithmBundle` v2 = 256-bit GCM (`:64-75`).
- **On-disk envelope** (`io/AESEncryptingOutputStream.java:30-79`): 16-byte random IV; first record
  `writeUTF(header)` where `header = bundle.encode([hexIV,"0"])`; body = `[int len][ciphertext]` per 4096-byte
  chunk. The SAME envelope protects the `signers` registry (§482.1) AND the subscription secrets (B480).
- **`KeyRing` (`.kr`/`.km`)** — two-level key protection:
  - `SimpleKeyRing` stores each alias as a 32-byte key **AES-GCM-encrypted under the KeyMaterial**
    (`SimpleKeyRing.java:516-542`; new keys = 32-byte `SecureRandom`, `:413-419`). `.kr` = VERSION 5, MAGIC
    `357109530`.
  - The KeyMaterial lives in `.km`: `KeyRingFactory.getInstance(securityDir, ".kr", ".km")` →
    `KeyMaterialFactory…getKeyMaterial()` → `SimpleKeyRing(dir, ".kr", km)` (`KeyRingFactory.java:19,62-70`).
  - So resolving an alias (e.g. `baja.licensing.subscription.ecKeyPair`): KeyMaterial(`.km`) decrypts the
    KeyRing(`.kr`) entry → recovered 32-byte key → `Aes256PasswordManager` decrypts the actual secret file.
  - Key roll interval `niagara.keyMaterialRollInterval` default 365 d; `.kr.rec` recovery copies.
- **Obfuscation caveat `[CERT]`:** the subscription package is symbol-obfuscated in this build (package
  `com.tridium.nre.n`, aliases `baja.licensing.subscription.*` → `baja.licensing.n.n`, secret files `.n`/`.n.r1`).
  The MECHANISM is `[CERT]`; the exact cleartext alias/filename strings were NOT recoverable here (they were
  documented in B480 from the non-obfuscated station-side classes).

## §482.4 — License signature verify + canonicalization (baja) `[CERT]`

- **`LicenseUtil.verify`** (`LicenseUtil.java:703-724`): `Signature.getInstance(alg).initVerify/update/verify`.
  Key by version: `version=="2.0"` → `getVersion2PublicKey()` = EC P-256 (`version2PublicKeyData`, OID
  id-ecPublicKey + prime256v1, `:478-570,749-755`); else → `getMasterPublicKey()` = **DSA-1024**
  (`masterPublicKeyData`, OID id-dsa `1.2.840.10040.4.1`, `:31-476,741-747`) → effective **SHA1withDSA** (JCA
  default). **No custom DER parsing** — the Base64-MIME-decoded signature is handed raw to `Signature.verify`
  and the JCA provider parses the DER `SEQUENCE{r,s}` internally (`LicenseFile.java:83`, `LicenseUtil.java:711`).
  Caller passes `signature/@algorithm` when present (`LicenseFile.java:172-179`; `CertificateFile.java:71-82`).
- **`LicenseUtil.encode` canonicalization** (`:645-689`): `<qname` + attributes **in document order** (no sort,
  no XML-escape) + `>\n` + children (recurse `XElem`; `XText` → raw text + `\n`) + `</qname>\n`. `encode(String)`
  writes chars truncated to bytes (**Latin-1/ASCII, NOT UTF-8**). The `<signature>` element is **stripped before
  encoding** (`LicenseFile.java:170-171`; `CertificateFile.java:74`) so signed bytes never include the sig.
  This byte-exact scheme is why an OEM tool must reproduce it exactly to make verification pass (cf. [B323]).

## §482.5 — Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|---|---|---|---|
| 1 | JarSignatureRegistry uses JarVerifier.verify(), caches by path+mtime, persists encrypted `signers` | `[CERT]` | `JarSignatureRegistry.java:401-443,221` | PASS |
| 2 | validateCertChain = PKIX, anchors system+user cacerts, revocation DISABLED, TPK pins leaf if canCheckTpk | `[CERT]` | `CertificateChainValidator.java:120-229,146,57-63` | PASS |
| 3 | TPK = hardcoded RSA-2048 SPKI (exp 65537); byte-compare pin; also verifies cacerts.sig (SHA256withRSA) | `[CERT]` | `SecurityConstants.java:12-323`; `CoreCryptoManager.java:900-917` | PASS |
| 4 | System cacerts pw `jiUz!rHw2d8&i3kI`/changeit; user cacerts pw = keyring key | `[CERT]` | `CoreCryptoManager.java:771-805`; `CoreStore.java:59-62` | PASS |
| 5 | ASN1HostId = X.509 attr OID 1.3.6.1.4.1.4131.2, DERUTF8String(hostId) | `[CERT]` | `ASN1HostId.java:12-49` | PASS |
| 6 | At-rest = AES-256-GCM (tag128), key from KeyRing; envelope 16B IV + UTF header + [len][ct] chunks | `[CERT]` | `Aes256PasswordManager.java:193-216`; `AESEncryptingOutputStream.java:30-79` | PASS |
| 7 | KeyRing two-level: KeyMaterial(.km) → KeyRing(.kr) entry (AES-GCM) → recovered key → secret file | `[CERT]` | `SimpleKeyRing.java:413-542`; `KeyRingFactory.java:19,62-70` | PASS |
| 8 | LicenseUtil.verify: DSA-1024 (SHA1withDSA) default vs ECDSA P-256 for version 2.0; JCA parses DER, no custom | `[CERT]` | `LicenseUtil.java:703-755`; `LicenseFile.java:83` | PASS |
| 9 | encode() canonicalization: doc-order attrs, \n after tags, raw text, Latin-1 bytes, sig stripped first | `[CERT]` | `LicenseUtil.java:645-689`; `LicenseFile.java:170-171` | PASS |

**Tally:** 9 claims, 9 `[CERT]`, 0 `[INFER]` in core claims (obfuscation caveat noted for subscription aliases).

## §482.6 — Connections & open gaps

- **Deepens** [B392] (module signing) with the ACTUAL verifier path, [B395] (master keys) with the TPK's role,
  [B480] (at-rest) with the KeyRing two-level mechanism, [B323]/[B477] (encode canonicalization).
- **Native counterpart:** @Segundo's `dsfspi` pass documents the byte-level DER/DSA/RSA that JCA hides here.
- **B482-G1** reconcile the module trust store: [B392] `truststore.jks` vs this block's system+user `cacerts`
  + RSA-2048 TPK — do both exist / which path validates modules? (do NOT overwrite B392 until confirmed).
- **B482-G2** `JarVerifier.getCodeSigners`/manifest logic (referenced, not read); `com/tridium/nre/security/km/**`
  (how `.km` KeyMaterial itself is protected — possibly host/QNX-bound).
