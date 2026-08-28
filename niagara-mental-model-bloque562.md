# Block 562 — The password-encoder chain: N4 splits credentials into HASHED (login, one-way PBKDF2-HMAC-SHA256, 10k iterations) vs ENCRYPTED (replayable secrets, reversible AES-256) — the fork is `isReversible`, and the login iteration count is low by modern guidance

**Session**: 2026-08-28
**Focus**: `access-control` (gap AC4 — the credential-storage encoder family; how `BPassword` encodes, the
one-way vs reversible split, and the security posture of each)
**Distribution**: Honeywell OptimizerSupervisor-N4.14.0.162
**Method**: READ-ONLY, inline bounded read of the 10 encoder classes + `BPassword`; algorithm params, class
hierarchy, and the default-selector token-verified inline.
**Primary sources** `[CERT]`:
- `organized/baja/baja/vineflower/javax/baja/security/{BAbstractPasswordEncoder,BPbkdf2HmacSha256PasswordEncoder,
  BReversiblePasswordEncoder,BAbstractAes256PasswordEncoder,BAes256PasswordEncoder,BAes256CbcPasswordEncoder,
  BAliasedAes256PasswordEncoder,BAliasedAes256CbcPasswordEncoder,BPlainPasswordEncoder,BNullPasswordEncoder}.java`
  + `BPassword.java`.

**Scope**: how N4 STORES a credential at rest, the encoder taxonomy, and the security consequences. Complements
[Block 558] (AC1, which check password STRENGTH at set-time) — this is what happens to the bytes AFTER. Does NOT
re-open the trust-anchor/PKI thread ([Block 392]) or the at-rest field-encryption thread ([Block 393]) —
REMITTANCE (but connects).

---

## 562.1 The base contract and the fork [CERT]

`abstract class BAbstractPasswordEncoder extends BObject implements BIAgent` `[CERT] :19` declares the encoder
SPI: `encode(SecretChars)`, `parse(String)`, `getValue()`, `getEncodingType()`, **`isReversible()`**,
`validate(SecretChars)`, `getEncodedValue()` `[CERT] :52-95`. The whole taxonomy pivots on one method —
`getDefaultEncodingType(boolean isReversible)` `[CERT] :98`:
```java
return isReversible ? BAes256PasswordEncoder.ENCODING_TYPE
                    : BPbkdf2HmacSha256PasswordEncoder.ENCODING_TYPE;
```
So the framework picks the algorithm purely by **whether the credential must be recovered later**: a login
password (never recovered, only verified) → PBKDF2 hash; a secret the station must REPLAY (send to an SMTP
server, a device, a subordinate station) → AES-256 encryption.

## 562.2 Family A — HASHED (login): `BPbkdf2HmacSha256PasswordEncoder`, one-way [CERT]

`final class BPbkdf2HmacSha256PasswordEncoder extends BAbstractPasswordEncoder` `[CERT] :25`. `encode()`
`[CERT] :41-46`:
```java
byte[] salt = new byte[16]; new SecureRandom().nextBytes(salt);   // 16-byte random salt
this.iterationCount = 10000;                                      // fixed 10,000 iterations
byte[] key = Pbkdf2.deriveKey(salt, this.iterationCount, password.get(), ALGORITHM_BUNDLE);
```
It stores `salt + iterationCount + derivedKey` and can only `validate()` a candidate (re-derive and compare) —
**there is no decrypt path**. This is the correct construction for a login password: salted, one-way,
per-password random salt. **Security note `[CERT]`+`[INFER]`:** the iteration count is **hard-coded at 10,000**.
That was reasonable for the AX-era design but is **low by modern guidance** (OWASP's 2023 baseline for
PBKDF2-HMAC-SHA256 is ~600,000). It is not configurable on this encoder. Not a break, but a brute-force-cost
hardening gap worth recording for a compliance review.

## 562.3 Family B — ENCRYPTED (replayable): the reversible AES-256 subtree [CERT]

`abstract class BReversiblePasswordEncoder extends BAbstractPasswordEncoder` `[CERT] :16` adds the recovery
surface: `getSecretBytes()` (DECRYPT — returns the plaintext) `[CERT] :37`, `validate`, `transcode`, plus an
`encryptionKey` Optional and a `usesExternalEncryptionKey` flag `[CERT] :18-19,71-83`. The concrete tree
`[CERT]`:
```
BReversiblePasswordEncoder
└─ BAbstractAes256PasswordEncoder            (:26 extends BReversible)
   └─ BAes256PasswordEncoder                 (:17, stores a `cipher` field)  ← reversible DEFAULT
      └─ BAes256CbcPasswordEncoder           (:15)
   └─ BAliasedAes256PasswordEncoder
      └─ BAliasedAes256CbcPasswordEncoder    (:15)
```
These exist because some credentials CANNOT be hashed — the station has to hand the cleartext to a remote party
(outgoing SMTP account [Block 324], a device/driver password, Fox credentials to a subordinate [Block 414]).
`getSecretBytes()` recovering the plaintext is **by design, not a defect**. The security question is the KEY:
- **Non-aliased AES** uses a station-managed key — recoverable on-box (ties to the machine-key / passphrase
  at-rest domains, [Block 466]/[Block 393]).
- **Aliased AES** (`BAliasedAes256*`) keys off a **keystore alias** — the AES key lives in the keystore, so the
  ciphertext is bound to a key the operator controls (stronger; `usesExternalEncryptionKey`).

## 562.4 The degenerate encoders — `plain` and `null` [CERT]

- `BPlainPasswordEncoder extends BAbstractPasswordEncoder`, `ENCODING_TYPE = "plain.1"` `[CERT] :16-19` —
  stores the password **in cleartext**. Legacy/interop only; selecting it defeats every protection above.
- `BNullPasswordEncoder` `[CERT] :18` — accepts only an EMPTY password (`throws` on a nonempty one `:40`) — the
  "no credential" marker, not an encoder.

Their mere presence is a migration/legacy risk: an upgraded or imported `BPassword` whose `encodingType` parses
to `plain.1` is a plaintext credential on disk.

## 562.5 `BPassword` — the value that carries the encoder [CERT]

`final class BPassword extends BSimple implements BIComparable, BIPasswordValidator, BIUnlinkable` `[CERT] :38`
holds a single `BAbstractPasswordEncoder encoder` `[CERT] :41`. Its decode path `[CERT] :92-192` distinguishes
already-encoded vs raw input, reconstructs the encoder from the stored `encodingType`
(`BAbstractPasswordEncoder.make(encodingType)`), and — for reversible encoders — re-encrypts on decode gated by
`pContext.getDecryptionKeySource() != EncryptionKeySource.none` `[CERT] :162`, i.e. the transcode-on-load tied to
the encryption-key source. A `fallback.getPasswordEncoder()` path `[CERT] :204-220` supports encoder migration
(re-encode from a fallback's stored value). So `BPassword` is the seam where an upgrade can MIGRATE a credential
from one encoder to another (e.g. plain→PBKDF2, or re-key an AES secret).

## 562.6 Thesis [CERT-synthesis]

N4 does the RIGHT structural thing: it never hashes what it must replay and never stores-recoverable what it only
verifies — the `isReversible` fork (§562.1) enforces that split. The two real posture items are: (1) the login
hash is **PBKDF2-HMAC-SHA256 with a fixed 10,000 iterations** — sound algorithm, iteration count low by 2020s
guidance and not tunable; (2) reversible secrets are only as safe as their **key source** — prefer the
**aliased** AES encoders (keystore-held key) over station-managed keys, whose ciphertext is recoverable on-box
([Block 466]/[Block 393]). And `plain.1` must never appear in a production `BPassword`.

## 562.7 Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|-------|--------|----------|---------|
| 1 | Base SPI abstract encode/parse/isReversible/validate; selector getDefaultEncodingType → AES if reversible else PBKDF2 | [CERT] | BAbstractPasswordEncoder.java:19,52-98 | token-checked ✓ |
| 2 | PBKDF2 encoder: 16-byte SecureRandom salt, iterationCount=10000, Pbkdf2.deriveKey; validate-only (one-way) | [CERT] | BPbkdf2HmacSha256PasswordEncoder.java:41-46 | token-checked ✓ |
| 3 | 10k iterations is low vs modern guidance (OWASP ~600k) and not configurable | [CERT]+[INFER] | :45 + external guidance | flagged ✓ |
| 4 | BReversiblePasswordEncoder adds getSecretBytes (decrypt) + external-key flag; AES-256 subtree extends it | [CERT] | BReversiblePasswordEncoder.java:16-83; BAbstractAes256PasswordEncoder.java:26; BAes256PasswordEncoder.java:17 | token-checked ✓ |
| 5 | Aliased AES keys off a keystore alias (external key) vs station-managed non-aliased | [CERT] | BAliasedAes256*.java:15 + usesExternalEncryptionKey | token+logic ✓ |
| 6 | BPlainPasswordEncoder = "plain.1" cleartext; BNullPasswordEncoder = empty-only | [CERT] | BPlainPasswordEncoder.java:16-19; BNullPasswordEncoder.java:40 | token-checked ✓ |
| 7 | BPassword carries the encoder, migrates via fallback, transcodes reversible on decode gated by EncryptionKeySource | [CERT] | BPassword.java:38-220 | token-checked ✓ |

**Marker tally**: [CERT] ×6 · [CERT]+[INFER] ×1 (iteration-count guidance). Block TYPE = EVIDENCE
(decompilation). 6 of 7 rows token-verified inline. Encoder count = **10** (audit said 8; the two abstract bases
+ aliased CBC variant were uncounted).

## Connections

- **[Block 558]** (AC1) — the STRENGTH check at set-time; this is the STORAGE after.
- **[Block 393]** — at-rest field encryption (GCM per-field) — the same reversible-secret problem at the BOG level.
- **[Block 466]** — JACE at-rest key domains (machine-key vs passphrase) = the key source that decides how
  recoverable a non-aliased AES password is.
- **[Block 324]** — the outgoing SMTP account: a canonical consumer of a REVERSIBLE password.
- **[Block 392]** — the PKI/keystore thread (where an aliased AES key would live).

## Open gaps (this block)

- `EncryptionKeySource` enum values (none/…​) and the exact transcode-on-decode key plumbing are named, not fully
  enumerated — folds into an at-rest child gap ([Block 393]/[Block 466] territory). `AES256Cbc` vs the default
  AES mode difference (CBC padding) is named, low value. Focus continues at AC5 (SecurityDashboard SPI).
