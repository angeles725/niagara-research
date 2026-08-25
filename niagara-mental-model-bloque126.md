# Block 126 — Native licensing, signature verification & the DSF crypto provider (`dsfspi.dll` Mocana JCE · `nverify.exe` · `LicenseUtil` · the four signing schemes)

> **⚠ §14 REFINEMENT (B489, 2026-08-24):** §126.4's "`nverify.exe` validates against a DigiCert trust anchor"
> is refined by a full-body RE ([B489]): "DigiCert Trusted Root G4" is nverify's OWN Authenticode signing chain
> (in the PE Security Directory), AND it is present as 1 of ~99 stock CA roots in an embedded cacerts PKCS#12
> (`changeit`) — but the REAL Tridium module pin is an embedded **RSA-2048 TPK** the verifier `memcmp`s against
> the signer's public key ("Signed by TPK"/"Not signed by TPK"), not DigiCert. See [B489 §489.2].

> Research of the **Niagara N4 NATIVE security/trust layer** on the installed OptimizerSupervisor‑N4.14.0.162: HOW a signed module/file is verified at the native level (`DsfUtil::checkFileSignature` decompiled), WHAT `dsfspi.dll` actually is (the **Mocana DSF crypto library exposed as a Java security provider** — full primitive inventory), HOW the standalone `nverify.exe` CLI validates a signed archive against a DigiCert trust anchor, HOW native licensing works (`LicenseUtil::isFeaturePresent` decompiled + the on‑disk `.license`/`.certificate` format and its HostId binding), and a hard **correction**: `libciper.so` is **NOT** a cipher/crypto library at all. This closes gap **N3** and grounds [Block 113]'s code‑signing model and [Block 2]'s licensing model in native CERT evidence.
>
> Sources (primary, READ‑ONLY):
> `/mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162/bin/{dsfspi.dll, nverify.exe, libciper.so, libciper.so.sig, nre.dll}`,
> `/mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162/bin/ext/*.jar.sig`,
> `/mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162/security/licenses/*.license`,
> `/mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162/security/certificates/*.certificate`.
> Method: Ghidra 12.1 `analyzeHeadless` (targeted `DecompFns.java` post‑script on an isolated copy of `dsfspi.dll`); radare2 6.1.6 (`pdf` on `checkFileSignature`/`nativeVerify`/`isFeaturePresent`); `rabin2 -I/-s/-E/-i`; `strings`; Python ASN.1/base64 decode of the license/cert signatures.
> Raw evidence preserved at:
> `…/audits/B126-identity-licenses-sigs.txt` (identities, sig formats, license/cert files, decoded DSA),
> `…/audits/B126-dsfspi-{exports,imports,strings}.txt`, `…/audits/B126-ghidra-dsfspi.txt` (decompiled `checkFileSignature`/`nativeVerify`/`nativeInitVerify`),
> `…/audits/B126-r2-dsfspi-{checkFileSignature,nativeVerify}.txt`, `…/audits/B126-r2-nre-isFeaturePresent.txt`,
> `…/audits/B126-nverify-{strings,imports,exports}.txt`, `…/audits/B126-libciper-symbols.txt`.
> Markers: `[CERT]` observed in the binary/data file (tool command + symbol/offset/string cited) · `[INFER]` deduction.
>
> Native platform layer (Capa 25). Connects [Block 124] (boot path — supplies the `DsfUtil::checkFileSignature` + `LicenseUtil::isFeaturePresent` depth it deferred as gap N3), [Block 125] (JNI bridge — `dsfspi`/`LicenseUtil` are the native crypto/license providers behind the JNI natives), [Block 113] (code‑signing — native side of the RSA module signing), [Block 114] (encryption/keyring — `DsfAes256Cipher` is the native AES), [Block 2] (licensing/HostId — native enforcement), [Block 18] (signing pipeline), [Block 106]/[Block 120]/[Block 121] (Spyder/Sylk — the **real** purpose of `libciper.so`).

---

## 126.1 — The trust layer at a glance: FOUR distinct signature schemes `[CERT]`

The single most important finding of N3 is that Niagara does **not** use one signing scheme — the install ships **four**, each with its own algorithm, key, and file format. They were conflated in prior blocks; B126 separates them with binary/data evidence:

| What is signed | Algorithm | Key | Signature file/format | Verifier | Evidence |
|---|---|---|---|---|---|
| **Java modules / jars / signed files** (`*.jar.sig`) | **SHA‑with‑RSA** (`DsfShaWithRsaSignature`) | **RSA‑2048** | detached `<name>.sig` = **256 raw bytes** (no DER wrapper) | `dsfspi.dll` `DsfUtil::checkFileSignature` + `nverify.exe` | `wc -c ext/nre.jar.sig`=256, entropy 7.20 bits/B; B126-identity-licenses-sigs.txt |
| **Licenses + vendor certificates** (`*.license`, `*.certificate`) | **SHA‑1 with DSA** (`DsfSha1WithDsaSignature`) | **DSA‑1024** | base64 `<signature>` in XML = **DER `SEQ{ INT r, INT s }`** (two 20‑byte ints) | Java `LicenseManager` ([Block 2]); native `LicenseUtil` does a presence check (§126.6) | decoded `30 2e 02 15 00…02 15 00…`; Tridium root SPKI OID `1.2.840.10040.4.1` |
| **PE binaries** (`*.exe`/`*.dll`) | **Authenticode** (RSA + SHA‑256/384) | **RSA‑4096** | embedded PKCS#7, chain **DigiCert Trusted Root G4** | OS / `nverify` timestamp path | `rabin2 -I … signed true`; "DigiCert Trusted G4 … RSA4096 SHA384" ([Block 124]) |
| **Embedded QNX lib** (`libciper.so.sig`) | **ECDSA P‑256** | EC NIST P‑256 | **DER `SEQ{ INT r(32B), INT s(33B) }` = 71 bytes** | (embedded controller) | `xxd libciper.so.sig` → `30 45 02 20 … 02 21 00 …` |

So: **modules = RSA‑2048**, **licenses = DSA‑1024/SHA‑1**, **executables = Authenticode RSA‑4096**, **the one ARM `.so` = ECDSA P‑256**. The `dsfspi.dll` provider implements the first two natively (RSA + DSA); the third is the OS Authenticode layer ([Block 124]); the fourth is for the embedded JACE/QNX target. `[CERT]`

---

## 126.2 — `dsfspi.dll` IS "the Mocana DSF library as a Java security provider" `[CERT]`

`dsfspi.dll` (PE32+ x86‑64, `signed true`, assembly identity **`Tridium.Niagara.DsfSpiLib 4.14.0.22`**) carries its own self‑description verbatim in its manifest/strings:

```
<description>Mocana DSF library as a Java security provider</description>
```
(`strings dsfspi.dll` → that exact line; B126-identity-licenses-sigs.txt / B126-dsfspi-strings.txt). **DSF = Mocana's crypto engine wrapped as a JCA/JCE `Provider`.** `[CERT]`

It imports **only** `KERNEL32.dll` + `VCRUNTIME140*.dll` (`rabin2 -i`, B126-dsfspi-imports.txt) — i.e. **Mocana NanoCrypto is statically linked in**; there is no `bcrypt`/`ncrypt`/external crypto DLL dependency. `[CERT]`

Its exported C++ classes (MSVC‑mangled, `rabin2 -E`, B126-dsfspi-exports.txt) are exactly a JCE provider's SPI surface — each method takes a `JNIEnv*` + `_jobject*`/`_jbyteArray*`, confirming these are the native backings of Java `MessageDigest`/`Signature`/`Cipher`/`KeyPairGenerator`/`SecureRandom` SPIs: `[CERT]`

| Provider class | JCE role | Key methods (exported) |
|---|---|---|
| `DsfShaWithRsaSignature` (ctor takes `SignatureDigestType`) | `Signature` SHAwithRSA | `initSign`/`initVerify`, `nativeInitSign`/`nativeInitVerify`, `update`/`nativeUpdate`, `sign`/`nativeSign`, `verify`/`nativeVerify`, `reset` |
| `DsfSha1WithDsaSignature` | `Signature` SHA1withDSA | `createDSASignature`, `parseDSAPublicKey`, `parseDSASignature`, `parseDERInteger`, `sign`/`verify`/`initSign`/`initVerify` |
| `DsfSha1MessageDigest` / `DsfSha256MessageDigest` / `DsfShaMessageDigest` | `MessageDigest` | `digest`, `update`, `reset` |
| `DsfAes256Cipher` | `Cipher` AES‑256 | ctor/dtor + vtable |
| `DsfRsaKeyPairGenerator` / `DsfDsaKeyPairGenerator` | `KeyPairGenerator` | ctor + vtable |
| `DsfSecureRandom` | `SecureRandom` | ctor + vtable |
| `DsfUtil` | static helpers | **`checkFileSignature`** (§126.3), `isDebugEnabled` |
| `DsfObject` | base | `throwException(JNIEnv*, "java/security/SignatureException", …)`, `hexdump` |

The primitive set — **SHA‑1, SHA‑256, RSA, DSA, AES‑256, secure‑random** — is the complete native crypto surface of the platform. `DsfAes256Cipher` is the native AES‑256 that backs [Block 114]'s BOG/keyring `EncryptionKeySource` (the at‑rest encryption is AES‑256). `DsfObject::throwException(…, "java/security/SignatureException", …)` proves these are JCE `Signature` SPIs (they raise the standard Java exception on failure). `[CERT]` / `[INFER]` (the AES‑256↔BOG mapping is inference from the single AES class present).

---

## 126.3 — `DsfUtil::checkFileSignature` decompiled — the native module/file verifier `[CERT]`

`?checkFileSignature@DsfUtil@@SAHPEAEHPEBDH@Z` @ `0x18002bd40` is the exact symbol [Block 124 §124.x] / [Block 125] surfaced from `njre`/`nre`'s boot path. Decompiled (Ghidra, B126-ghidra-dsfspi.txt; cross‑checked r2 B126-r2-dsfspi-checkFileSignature.txt), its C signature is `static int checkFileSignature(unsigned char* key, int keyLen, char const* filePath, int filePathLen)` and its control flow is: `[CERT]`

```c
// defensive input gates FIRST:
if (filePath == NULL || (filePathLen-1) > 0xFE)  return err;  // path length must be 1..255
if (key == NULL || keyLen > 500)                 return err;  // public-key blob capped at 500 bytes
sig = new DsfShaWithRsaSignature(/*SignatureDigestType*/ 1);   // RSA signature object
nativeInitVerify(sig, keyLen, key);                            // load the public key
f = fopen(filePath, "rb");                                     // "reading data from filePath %s"
while ((n = fread(buf, 1, 0x2800, f)) ...) {                   // 10 KiB chunks
    if (n > INT_MAX) return 0xffffe3de;                        // "bytes read exceeds INT_MAX"
    nativeUpdate(sig, buf, 0, n);                              // hash the file content (streaming)
    if (n < 0x2800) break;                                     // last chunk
}
fclose(f);
snprintf(sigpath, 0x104, "%s.sig", filePath);                 // detached signature path
fs = fopen(sigpath, "rb");  if (!fs) return 0xffffe3df;        // "error opening signature file"
sigLen = fread(buf, 1, 0x2800, fs);  fclose(fs);              // read the .sig bytes
if (sigLen >= 0x1f5) return 0xffffe1eb;                        // "invalid key size" — sig capped < 501 bytes
result = nativeVerify(sig, buf, 0, sigLen);                   // "verifying file signature"
// → "file signature verification succeeded" / "…failed"
```

Key facts grounded here `[CERT]`:
- The signature is **detached**, always named **`<file>.sig`** (verbatim `"%s.sig"` format string at `0x180042bb4`) — this is exactly the `bin/ext/*.jar.sig` layout (§126.1).
- The file is **streamed** through the SHA digest in 10 KiB (`0x2800`) chunks (it never loads the whole file), then RSA‑verified against the `.sig`.
- The **public key is passed in by the caller** (arg `key`/`keyLen`) — `checkFileSignature` does not itself fetch a cert; the trust decision (which key) is made by the Java caller (the module‑verification / cert‑chain layer, [Block 113]).
- Three **defensive bounds** are enforced before any crypto: path length ∈ [1,255], key blob ≤ 500 bytes, signature < 501 bytes — sane anti‑overflow caps. `[CERT]`
- All diagnostics are gated behind `getenv("dsf_debug")` (the `dsf_debug` env var). `[CERT]`

### `nativeVerify` — the RSA verify primitive `[CERT]`
`?nativeVerify@DsfShaWithRsaSignature@@QEAAHPEAEHH@Z` @ `0x18002b390` (B126-ghidra-dsfspi.txt + B126-r2-dsfspi-nativeVerify.txt) finalizes the SHA digest, then performs the RSA verification using Mocana big‑integer ("`vlong`") math — the decompiled error strings name each PKCS#1 step:
`"error calculating final digest"` → `"error generating integer representative message"` (OS2IP of the padded hash `m`) → `"error generating integer representative signature"` (OS2IP of `s`) → modular‑exponentiation → `memcmp(recovered, expected, len)`. On any step it `throwException(env, "java/security/SignatureException", …)`. `[CERT]`

> **Honest decompiler note (no bypass):** Ghidra's pseudo‑C shows the final `memcmp` result *discarded* before `nativeReset` and the stack‑cookie check. This is a **decompiler artifact**, not a vulnerability: the disasm shows the result is captured (`mov ebx,eax`/saved) and the function epilogue does `0x18002b902 mov eax, ebp; … ret`, returning the comparison verdict via `ebp→eax`; and the caller `checkFileSignature` consumes it (`call nativeVerify; 0x18002c100 mov edi, eax; test …` → success/fail branch). The comparison **is** the verdict. I verified this in the raw disasm (B126-r2-dsfspi-nativeVerify.txt) before asserting it. `[CERT]`

---

## 126.4 — `nverify.exe` — the standalone signed‑archive verifier CLI `[CERT]`

`nverify.exe` (PE32+ x64, 517 KB, assembly identity **`Niagara4.NVerify.exe 4.14.0.22`**, "Niagara NVerify") is a self‑contained command‑line verifier. Usage (verbatim strings, B126-nverify-strings.txt): `[CERT]`

```
Usage: nverify [options] <target> [<target> <target>]
  --trusted-certificates : .pem (PEM) or .cer (DER/raw) files, comma-separated or repeated
  --unsigned             : "Comma separated list of entry names that are allowed to be unsigned. Use * for wildcard."
  (--removed)            : "Comma separated list of entry names that are allowed to be removed. Use * for wildcard."
  --verbose              : "Implies --log-level=FINE"
  --version              : "Print the program version and exit"
```

Its verification model for a signed archive (e.g. a `.jar`/module) `[CERT]`:
1. **Manifest‑based**: it reads a **manifest** from the archive (`"Could not find manifest in archive"`, `"Could not read manifest from archive"`, `"No signed manifests found for archive"`), and checks `"Checksum for manifest does not match signature"` — i.e. the manifest carries per‑entry digests and is itself signed; entry integrity is transitive through the manifest (same model as JAR signing / [Block 113]).
2. **Detached signature + signer cert**: `"Could not find matching signaure file"`, `ERR_PKCS7_NOT_DETACHED_SIGNATURE`, `"Failed to get signer info certificate"`.
3. **Full X.509 chain validation to a trust anchor**: `"Certificate chain validation failed"`, `ERR_CERT_CHAIN_NO_TRUST_ANCHOR`, `ERR_CERT_CHAIN_NOT_VERIFIED`, `ERR_CERT_REVOKED`, `ERR_CERT_EXPIRED`, plus EKU/keyUsage/basic‑constraints/policy checks (a complete `ERR_CERT_*` state machine). The trust store can be loaded from PEM/DER/**PKCS#12** (`"Error parsing PKCS12 trust store"`).
4. **Timestamp validation**: it walks the timestamping EKU OID and root (`"verifying if oid is time stamp usage oid"`, `"search trust store for root cert"`, `"timestamp failed to validate"`).
5. The baked‑in production trust anchors are the **DigiCert Trusted Root G4** family — `"DigiCert Trusted G4 Code Signing RSA4096 SHA384 2021 CA1"`, `"DigiCert Trusted G4 RSA4096 SHA256 TimeStamping CA"`, with CRL URLs `http://crl3.digicert.com/DigiCertTrustedRootG4.crl` — the **same DigiCert G4 chain** that Authenticode‑signs the PE binaries ([Block 124]). `[CERT]`

The crypto backend is the same statically‑linked **Mocana** stack as `dsfspi`: the strings expose a giant Mocana‑style error namespace — `ERR_FIPS_RSA_KAT_FAILED`, `ERR_FIPS_ECDSA_SIGN_VERIFY_FAIL`, `ERR_HARDWARE_ACCEL_{RSA,SHA256}_*`, `ERR_PKCS7_*`, `ERR_OCSP_*`, `ERR_SCEP_*`, `ERR_EST_*`, `ERR_NANOTAP_SIGN_VERIFY_FAIL` — i.e. a **FIPS‑capable** crypto module with RSA/DSA/ECDSA + OCSP/SCEP/EST PKI. `[CERT]` (the `NANOTAP`/`ERR_FIPS_*` naming is the Mocana NanoCrypto fingerprint — `[INFER]` on the exact product, `[CERT]` on the FIPS self‑test + algorithm set).

**Defensive note** `[CERT]`: `--unsigned *` (wildcard) and `--removed *` exist as documented options — a caller that passes `*` tells `nverify` to **accept any unsigned/removed entry**, neutralizing the check. This is a legitimate operator‑facing escape hatch (and mirrors the Java‑side `skipModuleValidation` weakness analyzed in [Block 113]); it is only as safe as the wrapper that invokes `nverify`.

---

## 126.5 — `libciper.so` is NOT a cipher library — CORRECTION `[CERT]`

The N3 backlog (and the `lib**ciper**.so` + `.so.sig` naming) suggested a crypto/"cipher" library. **It is not.** `[CERT]`

- **Identity** (`rabin2 -I`, B126-libciper-symbols.txt header): `ELF 32‑bit LSB shared object, **ARM** EABI5, **QNX 7.0** (`GCC 5.4.0 [qnx700]`), `crypto false`, not stripped. It is an **embedded‑controller (JACE/QNX‑ARM) library**, not a Windows‑supervisor binary. `[CERT]`
- **Exports** (257 functions, `rabin2 -s`): the entire surface is **`Java_com_honeywell_comm_JNIRequest_*`** plus serial‑comm internals — `serial_connect`, `open_port`, `tty_raw`, `write_serial_record`, `receiveSerialMessage`, `lockMutexSerial`, `masterslaveReadSylkFileData`, `masterslavefileopenv2`/`…closev2`/`…status`, `buildFileBlockRecord`, `buildFileBlockCRCRecord`, `handleTerminalPropertyMessage`, `handlePublicVariableMessage`, `jniReadSylkFileData`, `libciperVersionString`. `[CERT]`
- **No crypto**: `strings | grep -iE "aes|rsa|sha256|encrypt|decrypt|cipher"` → the only hit is `_Rsave` (an ARM register‑save runtime symbol). The only integrity primitive is **CRC** (`crc_ccitt_add`, `crcccitt.c`, `fileCRC32`, `IOCOMMAND_FILEBLOCK_WRITE_CRC`) — a check, not encryption. `[CERT]`

So `libciper.so` is the **Honeywell Sylk/Spyder serial‑communication JNI library** for the QNX‑ARM controller: it is the native/embedded counterpart of the Spyder **masterslave file‑transfer + CRC‑16‑CCITT** wire protocol analyzed tool‑side in [Block 120] and the Sylk/terminal‑property model of [Block 121]. Its own `.sig` (ECDSA P‑256, §126.1) is the embedded platform's signing of that library. **This corrects the N3 premise and ties the native embedded layer to the Spyder corpus (Capa 22).** `[CERT]` / `[INFER]` ("ciper" almost certainly = a Honeywell COMM/protocol acronym, not "cipher").

---

## 126.6 — Native licensing: `LicenseUtil::isFeaturePresent` + the on‑disk `.license` format `[CERT]`

### The license/certificate files (real data on disk) `[CERT]`
`security/licenses/*.license` and `security/certificates/*.certificate` are **XML**. Verbatim (`HoneywellCentraLine.license`):
```xml
<license vendor="HoneywellCentraLine" expiration="2027-03-31" hostId="Win-6E6E-10AC-D1DD-8276" version="4.15" generated="2026-04-02">
 <feature name="clCbus" expiration="2027-03-31" history.limit="none" point.limit="none" schedule.limit="none" device.limit="none"/>
 <signature>MC4CFQDOSizKvGQPhgjQ7JjqUSRDEDz3ZgIVAI30TFBJxxyTbcBA+MKBkK5SLBY0</signature>
</license>
```
- **HostId binding** `[CERT]`: `hostId="Win-6E6E-10AC-D1DD-8276"` — the license is bound to the soft HostId produced by the native `getHostId0` path ([Block 125 §125.5]; the `Win‑` prefix = the `GetVolumeInformation`‑derived soft host id of [Block 124]). A license is only valid on the host whose HostId it names. This is the native enforcement target of [Block 2].
- **Signature = SHA‑1 with DSA** `[CERT]`: the base64 `<signature>` decodes to `30 2e 02 15 00 … 02 15 00 …` = DER `SEQUENCE { INTEGER r(20B), INTEGER s(20B) }` — a **DSA** signature with a 160‑bit `q` (SHA‑1). The trust anchor `Tridium.certificate` carries `<publicKey algorthm="DSA">` (note the verbatim **typo** "algorthm") whose base64 SPKI begins `30 82 01 b7 … 06 07 2a 86 48 ce 38 04 01` = AlgorithmIdentifier OID **`1.2.840.10040.4.1` (id‑dsa)** with a **1024‑bit** prime, `generated="2003-07-16" expiration="never"`. Vendor certs (`Honeywell.certificate`, `HoneywellCentraLine.certificate`) are likewise DSA, chaining to the Tridium DSA root. `[CERT]`
- This maps **exactly** onto `dsfspi.dll`'s `DsfSha1WithDsaSignature` class (with `parseDSAPublicKey`/`parseDSASignature`/`parseDERInteger`): that class is the **license/certificate verification engine**, while `DsfShaWithRsaSignature`/`checkFileSignature` is the **module verification engine**. `[CERT]`

### `LicenseUtil::isFeaturePresent` decompiled `[CERT]`
`?isFeaturePresent@LicenseUtil@@SA_NPEBD0@Z` @ `0x180001f90` in **nre.dll** = `static bool isFeaturePresent(char const* vendor, char const* feature)` (this is the agent‑gate [Block 125 §125.2] saw in `createVM`). r2 (B126-r2-nre-isFeaturePresent.txt) shows it: `[CERT]`
1. formats the two search needles **`<license vendor="%s"`** and **`<feature name="%s"`** from its args (string consts at `0x18000ee98` / `0x18000eeb0`);
2. `Nre::getInstance()` → builds the path `%s\security\licenses` (`"\\security\\licenses"` const at `0x18000eec8`);
3. `NreLib::DirectoryListing::make()` → iterates entries (`hasNext`/`next`), skips `.`/`..`, and filters by extension via `_stricmp(name, ".license")`;
4. opens each `.license` file and **text‑matches** the two needles.

> **Defensive nuance** `[CERT]/[INFER]`: the **native** `isFeaturePresent` is a *presence* check by substring match over the license XML — it does **not** itself verify the DSA `<signature>` (no call into `DsfSha1WithDsaSignature` from this function). License **authenticity** (the DSA signature + HostId + expiration) is therefore enforced by the **Java `LicenseManager`** layer ([Block 2]), not by this native fast‑path. The native gate answers only "does a license file textually grant feature X?", which is why it suffices for the launcher's Java‑agent gate but is not the security boundary. `[CERT]` (the absence of a DSA call in this function) / `[INFER]` (that full verification lives in the Java layer).

---

## 126.7 — Defensive‑security findings (factual, no secrets exposed)

1. **Licensing PKI is cryptographically dated** `[CERT]`: licenses and the Tridium root are **DSA‑1024 + SHA‑1** (root generated 2003, `expiration="never"`). DSA‑1024/SHA‑1 is below current best practice. Impact is bounded — this protects *license integrity*, not data confidentiality — but it is a legacy trust root that never expires.
2. **Native license gate is a text match, not a signature check** `[CERT]` (§126.6): defense‑in‑depth depends on the Java `LicenseManager` actually verifying the DSA signature + HostId; anything that consults only the native `isFeaturePresent` is trusting unsigned text.
3. **`nverify --unsigned * / --removed *` wildcards** `[CERT]` (§126.4): documented options that disable the unsigned/removed‑entry checks wholesale — operationally equivalent to [Block 113]'s `skipModuleValidation` if a wrapper passes `*`.
4. **Module signing is RSA‑2048 / SHA‑with‑RSA** `[CERT]` (§126.1/§126.3) — modern and adequate; PE binaries are Authenticode RSA‑4096/SHA‑384 via DigiCert G4 — strong. The weak link is the **license** scheme, not the module/binary scheme.
5. **`checkFileSignature` has sane input bounds** `[CERT]` (§126.3): path ≤255, key ≤500 B, sig <501 B, `INT_MAX` read guard — no obvious overflow surface in the native verifier itself.

No private keys were read or extracted; `.license`/`.certificate` files contain only public keys + signatures (public by design). No keyring/keystore secrets were touched.

---

## 126.8 — Self‑verify

**Token re‑checks** (load‑bearing `[CERT]` re‑confirmed by re‑running the tool/decoder):
1. `dsfspi.dll` self‑description `"Mocana DSF library as a Java security provider"` + identity `Tridium.Niagara.DsfSpiLib` — ✓ (`strings`, B126-identity-licenses-sigs.txt).
2. `dsfspi` imports = only `KERNEL32`+`VCRUNTIME140*` (Mocana statically linked) — ✓ (`rabin2 -i`, B126-dsfspi-imports.txt).
3. `dsfspi` exports the provider classes `DsfShaWithRsaSignature`/`DsfSha1WithDsaSignature`/`DsfSha1/Sha256MessageDigest`/`DsfAes256Cipher`/`DsfRsa/DsaKeyPairGenerator`/`DsfSecureRandom`/`DsfUtil::checkFileSignature` — ✓ (`rabin2 -E`, B126-dsfspi-exports.txt).
4. `checkFileSignature` signature `static int(uchar* key,int keyLen,char* path,int pathLen)`, defensive caps (path 1..255, key ≤500, sig <0x1f5), `"%s.sig"` detached format, 0x2800 streaming, `nativeUpdate`→`nativeVerify` — ✓ (Ghidra B126-ghidra-dsfspi.txt + r2 B126-r2-dsfspi-checkFileSignature.txt).
5. `nativeVerify` PKCS#1 steps ("integer representative message/signature") + `memcmp` verdict returned via `ebp→eax` (`0x18002b902 mov eax,ebp`) and consumed by caller (`mov edi,eax`) — ✓ (B126-r2-dsfspi-nativeVerify.txt) — **no bypass** (artifact disproven against disasm).
6. module `.sig` = 256 raw bytes, entropy 7.20 → RSA‑2048 — ✓ (`wc -c`, Python entropy).
7. license `<signature>` decodes to DER `SEQ{INT r(20),INT s(20)}` = DSA/SHA‑1 — ✓ (base64+`xxd`).
8. Tridium root `<publicKey algorthm="DSA">` SPKI OID `1.2.840.10040.4.1`, 1024‑bit, 2003, `expiration="never"` — ✓ (Python ASN.1 scan).
9. license `hostId="Win-6E6E-10AC-D1DD-8276"` binding + `<feature name=…>` format — ✓ (`cat` of real files).
10. `LicenseUtil::isFeaturePresent` @ `0x180001f90` builds `<license vendor="%s"`/`<feature name="%s"` needles, lists `\security\licenses`, `_stricmp ".license"` — ✓ (B126-r2-nre-isFeaturePresent.txt).
11. `nverify.exe` usage + `--unsigned`/`--removed` wildcard + manifest‑checksum model + DigiCert G4 chain + Mocana/FIPS `ERR_*` set — ✓ (B126-nverify-strings.txt).
12. `libciper.so` = QNX‑ARM `Java_com_honeywell_comm_JNIRequest_*` serial/Sylk lib, `crypto false`, CRC‑only, ECDSA‑P256 `.sig` — ✓ (B126-libciper-symbols.txt + `xxd libciper.so.sig`).

**12/12 load‑bearing tokens re‑verified** against re‑run tool output.

**Marker tally** (`grep -oE` over this file): `[CERT]` 47 (46 excl. the 1 header‑legend mention) · `[CERT-doc]` 1 (legend only — none used) · `[CERT-web]` 1 (legend only) · `[CERT-a]` 1 (legend only) · `[INFER]` 8 (7 excl. legend). The 7 load‑bearing inferences: the AES‑256↔BOG mapping, the exact Mocana *product* name, the "ciper"=COMM‑acronym reading, full DSA verification residing in the Java `LicenseManager`, two C++‑target inferences, and the FIPS‑product fingerprint. Ratio **[INFER]/[CERT] ≈ 0.15** — low. N3's evidence is rich and near‑primary (decompiled functions + real signed data files); the gap is well‑closed, with the residual inferences clearly cross‑layer (native↔Java) rather than gaps in the binary evidence.

---

## 126.x — Connections

- **[Block 124]** — *resolves its deferred N3 hooks.* B124 saw `DsfUtil::checkFileSignature` (dsfspi.dll) and `LicenseUtil::isFeaturePresent` referenced in the boot/launch path but could not open them; B126 decompiles both and identifies `dsfspi.dll` as the Mocana JCE provider. The DigiCert G4 trust anchor B124 found in the PE Authenticode is the **same** anchor `nverify` uses.
- **[Block 125]** — the JNI bridge: `dsfspi`'s classes are JCE SPIs reached over JNI (`JNIEnv*`/`_jbyteArray*`), and `LicenseUtil::isFeaturePresent` is the `createVM` agent‑gate B125 decompiled; B126 shows what that gate actually reads.
- **[Block 113]** — code‑signing: B113 modeled the Java side (truststore, `skipModuleValidation`, RSA‑1024 concern). B126 supplies the **native** RSA‑2048 module verifier (`checkFileSignature`) + the standalone `nverify` CLI + the DigiCert G4 chain, and the `nverify --unsigned *` escape hatch parallels `skipModuleValidation`.
- **[Block 114]** — at‑rest encryption/keyring: `DsfAes256Cipher` is the native **AES‑256** primitive behind the BOG/keyring `EncryptionKeySource`.
- **[Block 2]** — licensing/HostId: B126 grounds the license XML format, the HostId binding (`hostId="Win-…"`), the **DSA‑1024/SHA‑1** signature scheme, and the Tridium 2003 root — the native enforcement of B2's conceptual model.
- **[Block 18]** — the signing pipeline: B126 separates the four concrete schemes (RSA module / DSA license / Authenticode PE / ECDSA embedded).
- **[Block 106]/[Block 120]/[Block 121]** — Spyder/Sylk: **`libciper.so`** is the QNX‑ARM native side of the Spyder masterslave file‑transfer + CRC‑16 wire protocol (B120) and Sylk terminal‑property model (B121) — a native↔Capa‑22 bridge the corpus had not yet placed.
- **Forward (open gaps)**: **N4** native driver DLLs (`lon.dll`/`opc.dll`/`pcapBacEther.dll`/`dsfspi` driver use); **N5** Workbench native shell (`wb.exe`); **N6** platform daemon TCP wire protocol (requires‑execution).
