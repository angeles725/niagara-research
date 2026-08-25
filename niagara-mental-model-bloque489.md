# Block 489 — `nverify.exe`, the native module/dist signature verifier: a C reimplementation of Java JAR-signing (SHA-256 manifest + PKCS#7) whose trust store is an embedded cacerts PKCS#12 (`changeit`, 99 roots) plus an RSA-2048 TPK memcmp pin — closing B482-G1 and reconciling B392 vs B482 with certainty

> **Focus:** `licensing` — the NATIVE module-verifier. **Native RE by sibling session `Segundo`
> (2026-08-24, `r2 pdf` + `objdump -d` at the same VA, byte-for-byte agreement); consolidated by `Primero`.**
> READ-ONLY: only static RE + carving the embedded stores; no binary executed. Closes **B482-G1** and answers
> the module-trust-store question with two independent corroborating reads (this native pass + the Java
> `validateCertChain` of [B482]). Markers §3.
>
> **Source:** `bin/nverify.exe` (sha256 `b0358926…`, PE32+ console, Mocana static, no crypt32/bcrypt). Model =
> Java JAR-signing reimplemented in C (`MANIFEST.MF` + `.SF` + detached PKCS#7).

## §489.1 — What nverify checks `[CERT corroborated A(r2)+B(objdump)]`

- **Digests — SHA-256 ONLY** (no MD5/SHA-1 path): whole-manifest (`SHA-256-Digest-Manifest`, `fcn.140009ed0`,
  mismatch "Checksum for manifest does not match signature") + per-file (`SHA-256-Digest`, `fcn.14000a580`,
  "Invalid checksum for %s"). memcmp 32 bytes (`@0x140038084`).
- **PKCS#7:** detached CMS via Mocana; `fcn.140002290` attaches detached data + counts signers → 0 signers →
  reject `-3`; verify `fcn.140029490` (Mocana backend, `ERR_PKCS7_*`).

## §489.2 — B482-G1 ANSWER: the trust store, THREE distinct facts `[CERT]`

1. **"DigiCert Trusted Root G4" is nverify's OWN Authenticode chain**, embedded in the PE Security Directory
   (@file 0x7ea00) — NOT a program anchor. **This corrects [B126 §126.4]'s "DigiCert G4 pinned" framing** for
   the module verifier.
2. **Default store = an embedded PKCS#12 `cacerts`**, password `"changeit"` (UTF-16LE, `mov r9d,0x12`), in
   `.data @0x140055000` (122,556 B; parsed by `fcn.14000b330` from `main @0x140005453`). Carved + decrypted =
   **99 stock CA roots** (SSL.com, VeriSign, IdenTrust, DigiCert Assured ID G3, **DigiCert Trusted Root G4**, …)
   — DigiCert G4 IS an anchor, but as **1 of 99 stock CAs, not a dedicated pin**. Chain validation
   (`fcn.140002b50`) uses `X509_isRootCertificate` and requires the chain root to be in the store
   (`fcn.14000b2a0`) else `0xffffe246` "No ultimately trusted cert found" → "Certificate chain validation
   failed". Operator `--trusted-certificates` (.der/.cer/.pem) also load.
3. **The real Tridium-specific pin = an embedded RSA-2048 "TPK" public key** (`@0x140072ec0`, DER RSAPublicKey
   270 B, exp 65537). `fcn.140002b50` extracts the signer's pubkey and `memcmp`s it against that blob → match
   "Signed by TPK"; mismatch "Not signed by TPK" → `0xffffe247` → chain fail. Gated by a runtime flag (getter
   `fcn.140009220` reads `byte[0x14007c03d]`); default 0 → TPK pin ACTIVE (relaxed if the operator supplies
   `--trusted-certificates`). `[CERT opcodes; flag semantics INFER]`

## §489.3 — Global decision `[CERT corroborated]`

nverify passes ONLY if: (a) manifest checksum OK, (b) all per-file digests OK with no missing entries, (c) ≥1
signer + CMS verify OK, (d) chain root in the cacerts P12 (or `--trusted-certificates`), (e) with the TPK gate
active, `signer.pubkey == TPK`. Codes: `-3` no signers, `0xffffe246` no root, `0xffffe247` not-TPK, `-4`
chain-create fail. **No unconditional bypass/short-circuit was found.**

## §489.4 — RECONCILIATION of [B392] vs [B482] (B482-G1 CLOSED) `[CERT]`

Two independent code reads now AGREE, so this is corrected with certainty (not a blind overwrite):
- The Java `CertificateChainValidator` ([B482 §482.1-2]) uses **system+user cacerts + the RSA-2048 TPK leaf-pin**.
- This native `nverify` uses **an embedded cacerts PKCS#12 (`changeit`, 99 roots) + the RSA-2048 TPK memcmp pin**.
- **Correction to [B392]:** the module trust anchor is NOT a dedicated `security/truststore.jks`. B392's
  `changeit` password was right, but the store is the **cacerts (P12/JKS) with 99 stock roots**, and the real
  Tridium-specific control is the **TPK RSA-2048 pin** (a byte-equality pin on the signer key), NOT a CA in the
  store. A `truststore.jks` file may still exist on disk for another purpose, but it is not what gates module
  signatures. B392 gets a §14 pointer here.

## §489.5 — Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|---|---|---|---|
| 1 | nverify = C reimpl of JAR-signing; SHA-256-only digests (manifest + per-file) | `[CERT+corrob]` | `fcn.140009ed0/14000a580`; memcmp @0x140038084 | PASS |
| 2 | Detached PKCS#7/CMS via Mocana; 0 signers → −3 | `[CERT+corrob]` | `fcn.140002290/140029490` | PASS |
| 3 | Default store = embedded cacerts P12 (changeit, 99 roots); root must be in store else 0xffffe246 | `[CERT+corrob]` | `.data @0x140055000`; `fcn.14000b330/14000b2a0` | PASS (corrects B126 §126.4 framing) |
| 4 | Tridium pin = embedded RSA-2048 TPK memcmp on signer pubkey → 0xffffe247 if mismatch; flag-gated | `[CERT opcodes]`/`[INFER flag]` | `@0x140072ec0`; `fcn.140002b50`; getter `fcn.140009220` | PASS |
| 5 | Global pass = manifest+per-file digests + ≥1 signer + root-in-store + TPK; no unconditional bypass | `[CERT+corrob]` | decision `fcn.140002b50` | PASS |
| 6 | Reconciles B392/B482: store = cacerts P12 (changeit) + TPK RSA-2048 pin, NOT a dedicated truststore.jks | `[CERT]` | this pass + [B482 §482.1-2] | PASS (closes B482-G1; corrects B392) |

**Tally:** 6 claims, all `[CERT]`/`[CERT+corrob]` (1 flag-semantics `[INFER]`). Native RE credit: sibling session
`Segundo`. Corroboration: r2 `pdf` and `objdump -d` agree byte-for-byte at each decision.

## §489.6 — Connections & open gaps

- **Closes B482-G1**; **corrects [B392]** (truststore.jks → cacerts P12 + TPK pin) and **[B126 §126.4]**
  (DigiCert G4 is nverify's own Authenticode chain + 1-of-99 stock CA, not the module pin). Both get §14 pointers.
- Native counterpart of [B482] (Java module verifiers); with [B484] (dsfspi crypto) + [B485] (launcher gates)
  the native licensing/signing surface is now covered.
- **B489-G1** persist the carved evidence (`nverify.embedded_p12.der`, `allcerts.pem` = 99 roots, the chain/PKCS7/digest
  disasm incl. `nverify.chain-tpk.disasm.txt`) to `sources/native-corroboration/` for SOURCES (Segundo to persist + sha256). **DONE** — both registered in `sources/SOURCES.md`.
- **B478-G1** native watchdog `shmem` (createWatchdog0 in nre.dll, backend common.dll+nre.dll) — liveness, not
  licensing; Segundo scouted it, closes on request.
