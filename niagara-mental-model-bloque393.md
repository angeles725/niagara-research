# Block 393 — The integrity asymmetry (gap SP-G2): Niagara signs CODE and DELIVERY, but leaves DATA unsigned — station backup, audit, history, and the `.bog` document carry no signature, MAC, or checksum

> **Focus:** `signing-pki` (gap **SP-G2**, the biggest asymmetry flagged by [B392]'s signature-surface map).
> Continues the arc [B75] (vector) → [B112] (forensics) → [B113] (signing hardening) → [B114] (data at-rest
> confidentiality) → [B392] (trust-anchor reconciliation). This block closes the integrity question that
> [B114 §114.0] opened but scoped out: *encryption ≠ integrity* — does anything actually sign the data?
>
> **Angle:** for each data artifact a station produces (backup `.dist`, audit trail, history/trend, config
> `.bog`), determine whether it carries cryptographic integrity (signature / MAC / checksum) or only
> confidentiality (encryption) or nothing — and contrast against the DELIVERY artifacts (install
> distributions) which [B392] left as an open comparison.
>
> **Sources:**
> - LIVE INSTALL `[CERT]` — `unzip`/`xxd` over `cleanDist/*.dist` (install distributions), sha256 in SOURCES.
> - CODE (decompiled + `javap`/`strings` ground-truth on `.class`, the decompiler redacts security tokens
>   to `n`) `[CERT]`: `SignedDistFilter`, `BAxOfflineBackup`/`BBackupService`, `BAuditRecord`,
>   `history/db/*`, `ValueDocEncoder`/`ValueDocDecoder`, `AxPasswordUtil`, `BAbstractAes256PasswordEncoder`.
> - CORPUS remittance `[REMITTANCE]`: [B33] (history `.hdb`/`.adb`), [B75] (audit deletable), [B114]
>   (`.bog` encryption pipeline, keyring), [B392] (three trust domains).

---

## 393.1 — The verdict in one line

**The thesis "Niagara signs code, not data" holds.** No data artifact carries a document-level signature,
MAC, or checksum. The only cryptographic integrity on any data is **per-field**, on password values sealed
with the modern GCM encoder — it never covers the artifact. Delivery artifacts (install distributions) are
signed; the data the station produces is not. `[CERT]`

| Artifact | Signature | MAC/HMAC | Checksum | Encrypted | Tamper-evident |
|---|---|---|---|---|---|
| Station backup `.dist` | No | No | No | Yes (per-entry PBE/AES) | **No** |
| Audit trail | No | No | No | No | **No** |
| History (trend) | No | No | No | No | **No** |
| `.bog` document | No | No | No | No (plaintext XML) | **No** |
| `.bog` modern password field | — | GCM tag | — | AES-256-GCM | Yes (that field only) |
| Install dist (OS/NRE/VM) | **Yes** (detached `.sign`/`.sig`) | — | — | — | **Yes (it is CODE)** |

---

## 393.2 — Delivery IS signed: the install-distribution `.dist` (disk evidence) `[CERT]`

A `.dist` is a plain ZIP with `META-INF/dist.xml` (a manifest carrying vendor/version/model, **no per-file
digest**) plus payload tarballs each with a **detached signature sidecar**. Two schemes by target: `[CERT]`
- Device-reset dist (`clean-dist-1-honeywell-nxubc.dist`): `npsdkUpdates/autorun.tar.gz` +
  `autorun.tar.gz.sign` = **512 bytes = raw RSA-4096**.
- NRE runtime dist (`nre-clean-honeywell-IPC.dist`): `nre_user.tar.gz.sig` / `clean.tar.gz.sig` =
  **71 bytes = ECDSA P-256** (DER `SEQ{INT r(32B), INT s(33B)}`, the 4th scheme of [B126 §126.1], the
  embedded-QNX signer).

Integrity of a distribution = the **detached signature over each payload tarball**, not a per-file digest
in the manifest. This extends [B26] (which documented only the gradle *signing toolchain*, RSA-2048/JCEKS),
never the on-disk `.sign`/`.sig` distribution signatures. `[CERT]`

## 393.3 — `SignedDistFilter` verifies ONLY install parts, never station backups `[CERT]`

The signature gate is narrow by design. `com.tridium.install.SignedDistFilter.acceptInstallable`: `[CERT]`
- `SignedDistFilter.java:50` — `if (!(inst instanceof BDistribution)) return true;` (non-distributions pass
  unverified).
- `:58` — only `BOsPart` / `BNrePart` / `BVmPart` trigger `checkSignature`.
- `:62-63` — anything else → `return true` (accepted).
- `:87` — `mgr.validateCertChain(entry, true)` — the code/firmware cert-chain check (Domain A/C of [B392]),
  **not a data check**.

So the signature machinery covers **OS/NRE/VM install media**, not the station's own backup. `[CERT]`

## 393.4 — Station backup `.dist`: encrypted per-entry, unsigned, unverified on restore `[CERT]`

`BAxOfflineBackup`/`BBackupService` build the station backup as a ZIP whose sensitive entries are encrypted
(`PBEDecryptingInputStream`, `AESStreamEncryption.pbeToKeyRing`; the `.kr`/`.km` keyring material per
[B114]). Ground-truth strings over all of `backup-rt` show only the generic bytecode `Signature` attribute
and `verifyDependencies` / "Verifying backup file dependencies" — a **module-version** check, not integrity.
No `MessageDigest` / `SHA` / `HMAC` / `checksum`. The restore path in `BBackupService` has no
`validateCert` / `verifySign` / `distVerified`. `[CERT]`

**Fail-mode:** a station `.dist` altered outside its encrypted fields restores with **no detection**. The
only barrier is confidentiality of the sensitive entries. This confirms [B392]'s SP-G2 hypothesis and
[B114 §114.0]'s framing (signature = code integrity; backup = data confidentiality only). `[CERT]`

## 393.5 — Audit trail: plaintext, mutable, no tamper-evidence `[CERT]`

`com.tridium.history.audit.BAuditRecord` (javap): plain `String` fields `target/slotName/oldValue/value/
userName` with **public setters** (`setValue`, `setUserName`, …), `implements ITruncatable` with
`truncate(int)`, and **no digest/hash/signature field**. Class strings across `history-rt`: only the
bytecode `Signature` attribute; no `digest/hmac/sha/crc/seal/tamper`. `[CERT]`

**Fail-mode:** an audit record can be edited, truncated, or deleted with no cryptographic evidence —
mechanically the same conclusion as [B75] (auditor shut off / history deleted without a permission check).
The audit that would record a signature bypass is itself the least protected artifact. `[CERT]`

## 393.6 — History (trend): no cryptographic integrity `[CERT]`

`history-rt` incl. `com/tridium/history/db/` (`BLocalDbHistory`, `BHistoryDbTable`, `LocalDbConnection`) —
the custom binary `.hdb` format of [B33] — carries no `MessageDigest`/`SHA`/`HMAC`/`CRC`/`seal` in class
strings. Delete/modify (incl. `deleteHistory`, [B75]) is undetectable at the integrity layer. The `.adb`
alarm/audit store [B33 flagged "investigate separately"] shares the finding (§393.5). `[CERT]`

## 393.7 — `.bog`: the key nuance — encryption ≠ integrity `[CERT]`

The config document itself is **plaintext and unsigned**. `ValueDocEncoder` (writes the bog) has no
`digest/mac/hmac/sign/checksum/crc` and no cipher — it emits XML in the clear; `ValueDocDecoder.readHeader`
parses only `version` + `PBEEncodingInfo`, **no digest**. Integrity exists only **per password field**: `[CERT]`
- Legacy AX: `AES/CBC/PKCS5Padding` — confidentiality **without MAC** (`AxPasswordUtil.java:112`).
- Modern: `AES/GCM/NoPadding` — **authenticated** encryption; the GCM tag makes *that field* tamper-evident
  (`BAbstractAes256PasswordEncoder.java:184`).
- User login passwords: `pbkdf2hmacsha256` — a one-way verification hash, **not** a data MAC
  (`AxPasswordUtil.java:63,280`).

**Fail-mode:** tampering with any non-encrypted part of the bog (components, links, slot values — i.e. the
bulk of the config) is **not detected** on load `[INFER, on CERT that ValueDocEncoder computes no
digest/mac]`; tampering a legacy-CBC password field is **not detected** (`[CERT]`); tampering a modern-GCM
field **fails decryption** (auth tag) — the single tamper-evident point. `[CERT]`

## 393.8 — Why this matters (the asymmetry as a security property) `[INFER]`

Niagara's cryptographic effort protects **"who may run what"** — modules ([B392] RSA X.509), distributions
(§393.2 RSA-4096/ECDSA), firmware ([B243]), licenses/vendor identity ([B126] DSA). It does **not** protect
**"what happened and can't be denied"** — audit, history, backup, config. An attacker who reaches the
filesystem or daemon can rewrite the record of their own actions without breaking any signature, because
there is no signature on the record to break. The complement of [B392]'s `changeit`/`System.exit(-6)`
findings: the code-trust perimeter is hardened; the data-integrity perimeter does not exist. The only
non-repudiation Niagara ships at the data layer is the **21 CFR Part 11** e-signature *ceremony*
([B356]) — and [B356] already showed its *artifacts* (the trend record, the certification statement) are
themselves unsigned plaintext. Same asymmetry, one layer up. `[INFER]`

---

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | Install distributions are signed with a detached sidecar; no per-file digest in manifest | [CERT] | disk: `autorun.tar.gz.sign`=512B, `*.tar.gz.sig`=71B; dist.xml has no digest |
| 2 | Device-reset dist = RSA-4096 (512B); NRE dist = ECDSA P-256 (71B) | [CERT] | `wc -c` on sidecars; 71B = DER SEQ{r,s} P-256 per [B126 §126.1] |
| 3 | `SignedDistFilter` verifies only BOsPart/BNrePart/BVmPart; everything else returns true | [CERT] | `SignedDistFilter.java:50,58,62-63,87` |
| 4 | Station backup `.dist` = per-entry PBE/AES encryption, no signature/MAC/checksum; restore unverified | [CERT] | backup-rt strings; no MessageDigest/SHA/HMAC; no validateCert in restore |
| 5 | `BAuditRecord` = plain String fields, public setters, ITruncatable, no digest | [CERT] | javap on BAuditRecord; history-rt strings |
| 6 | History `.hdb`/`.adb` carry no crypto integrity | [CERT] | history/db class strings (no digest/hmac/crc) |
| 7 | `.bog` document is plaintext XML, unsigned; only per-field password encryption | [CERT] | `ValueDocEncoder` (no digest/mac/cipher); `ValueDocDecoder.readHeader` |
| 8 | Legacy password = AES-CBC (no MAC); modern = AES-GCM (per-field tamper-evident); login = pbkdf2-hmac-sha256 | [CERT] | `AxPasswordUtil.java:112,63,280`; `BAbstractAes256PasswordEncoder.java:184` |
| 9 | Thesis: signs code/delivery, not data | [CERT] | §393.1 table = union of claims 1-8 |
| 10 | Data-integrity asymmetry is a security property (attacker rewrites own record unsigned) | [INFER] | §393.8; complements [B75][B356] |

**Tally:** 9 [CERT], 1 [INFER], 0 unmarked. Central claim (thesis) is [CERT].

## Connections
- **Closes SP-G2**; answers the biggest hole of [B392]'s surface map with file:line.
- **Confirms [B114 §114.0]** (encryption ≠ integrity) at the artifact level and **[B392]** SP-G2 hypothesis.
- **Grounds [B75]** mechanically: the audit is deletable because it is *structurally* unsigned/mutable,
  not merely permission-gated.
- **Extends [B26]**: on-disk distribution `.sign`/`.sig` (RSA-4096 / ECDSA P-256) vs the gradle toolchain.
- **Parallels [B356]**: Part 11 signs the *act*, not the *proof of the act* — same asymmetry one layer up.
- **Remits** `.hdb`/`.adb` format to [B33]; keyring `.kr`/`.km` to [B114]; module signing to [B392].

## Open gaps (updated `signing-pki` backlog)
- **SP-G1** (NEXT) — decode the byte-level scheme of `honFirmwarePackage` firmware delivery [B243]. `[investigable]`
- **SP-G5** — does `Honeywell.certificate`'s DSA-160 signature verify against the Tridium root key? `[investigable]`
- **SP-G3** — confirm Java `LicenseManager` rejects a bad DSA signature (native gate is text-match only, [B126 §126.6]). `[requires-execution]`
- **SP-G6** — CRL/revocation enforcement for BACnet/SC + TLS ([B287]). `[requires-execution]`
- **SP-G4** — reproduce a Tridium-rooted (non-OEM) `baja.jar` chain ([B392 §392.7]). `[blocked: requires-artifact]`
- **SP-G7** (new, from §393.8) — is there ANY optional integrity channel for data? (syslog offload to SIEM
  is the only tamper-*resistance* [B75 §75.6]; confirm nothing signs the local record.) `[investigable]`
