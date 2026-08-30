# B704 — DAR2-G1: the reversible-decrypt PoC design is confirmed (AES-256-GCM, keyring-aliased) — but this seed station's config.bog has ZERO reversible fields, so there is nothing to decrypt (§14 corrects B694's cipher)

> Focus: **jace-data-at-rest** · Gap **DAR2-G1** (the decryption PoC). Sources: decompiled
> `javax/baja/security/BAbstractAes256PasswordEncoder.java` + `com/tridium/util/PasswordUtil.java` (corpus
> `organized/`) + the config.bog encoded-secret inventory (SD). **SECRETS DISCIPLINE:** no secret shown. Marker
> `[CERT]` for decompiled code, `[CERT-hw]` for the SD inventory.

## 704.1 — The reversible-decrypt design, confirmed from code

[CERT] The reversible `BPassword` fields (config.bog values encrypted with `reversibleEncodingKeySource="keyring"`)
are handled by `BAbstractAes256PasswordEncoder extends BReversiblePasswordEncoder`. Its decrypt path
(`getSecretBytes`, `BAbstractAes256PasswordEncoder.java:62-90`):

1. The encoded value parses into **iv** (16 random bytes, hex), **cipher** (hex), and a **keyAlias** — via the
   `CryptographicAlgorithmBundle` format (`[<algo>]=…`).
2. The key is fetched from the **KeyRing by alias**: `Aes256PasswordManager.getManager(provider.getKeyRing(),
   keyAlias).decryptSecret(cipher, iv, aesTransformation)`.
3. The cipher is **`AES/GCM/NoPadding`** (default `aesTransformation`, line 206), i.e. **AES-256-GCM** with a
   16-byte IV.

So the decrypt is: `parse [algo]=iv|cipher|alias → KeyRing.getKey(alias) (unwrapped by /etc/km/.km) →
AES-256-GCM.decrypt(cipher, key, iv)`. This confirms the DAR2 architecture ([Block 694]) at code level.

## 704.2 — §14 correction to [Block 694]: AES-GCM, not CBC

[Block 694] §694.2 called the at-rest cipher "Mocana AES-256-**CBC**" (citing the DSF provider [Block 677]).
That is the cipher the DSF **provider** offers, but the **reversible-field encoder** specifically uses
**AES-256-GCM** (`BAbstractAes256PasswordEncoder` → `AES/GCM/NoPadding`) — consistent with [Block 393]'s
"per-field `BPassword` GCM" finding. §14 REFINE: the reversible data-at-rest cipher is **GCM**; the CBC mention
in B694 was the provider's general capability, not this path. (Back-pointer added to [Block 694].)

## 704.3 — The premise dissolves for THIS station: zero reversible fields

[CERT-hw] The PoC needs a ciphertext to decrypt. The config.bog of `JACE_UMBRELLA` contains **exactly one**
encoded secret: `[pbkdf2-sha256.1]=…` — the admin login hash, which is a **one-way PBKDF2** (not reversible,
[Block 685] §685.5 / [Block 695]). A `grep` for reversible `[aes…]=` encoders returns **zero**. This seed
station stored **no reversible secret at all**: no EmailService password, no remote-station credential, no
driver password (consistent with focus `jace-station-config` — no email, no supervisor join, one relay point).

So DAR2-G1's PoC has **no target on this card**. This is a premise dissolution (PROBE-THE-PREMISE, METHODOLOGY
§21), not a tooling wall: even a perfect decryptor has nothing to run against here. The DAR2 conclusion ("the SD
yields the reversible secrets") is **architecturally sound and would hold on any station that stored reversible
secrets**, but is **vacuous for this seed station** — its only stored credential is the one-way admin hash,
which no key recovers (only a crack, exhausted 0/3, [Block 695]).

## 704.4 — Honest status of the working PoC

[INFER] The full working decryptor (against a station that DOES have reversible fields) would still need one
more RE step: `com.tridium.nre.security.KeyRing` + `Aes256PasswordManager` (the `.kr`-parse and `.km`-unwrap
mechanism) are in the **nre core**, NOT in the decompiled `organized/` corpus (only the `javax.baja.nre.security`
doc is present). Building it = decompile the KeyRing class from `nrecore` ([Block 676]) + reimplement the
serialized-KeyRing parse + AES-GCM. That work is **not needed to close DAR2-G1** — because this station has no
reversible ciphertext to validate it — so it is recorded as an OPTIONAL future gap (**DAR2-G2**, requires a
station with reversible secrets), not a blocker.

## Connections

- Refines [Block 694] (DAR2, §14 cipher correction). Reversible encoder architecture → `BAbstractAes256PasswordEncoder`.
  Per-field GCM → [Block 393]. Admin one-way hash → [Block 685] §685.5 / [Block 695]. KeyRing on card → [Block 693].
  Seed-station (no reversible secrets configured) → [Block 692]. nre core source → [Block 676].

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | reversible encoder = AES-256-GCM, 16-byte IV, key from KeyRing by alias | [CERT] | BAbstractAes256PasswordEncoder.java:38-90,206 | code-read |
| 2 | §14: cipher is GCM not CBC (B694 cited provider's CBC) | [CERT] | line 206 + [Block 393] | corrected |
| 3 | config.bog has ONE encoded secret: [pbkdf2-sha256.1] one-way; ZERO reversible | [CERT-hw] | grep count | measured |
| 4 | PoC premise dissolves — no reversible ciphertext on this station | [CERT-hw]+[INFER] | 704.3 | reasoned |
| 5 | full decryptor would need nre-core KeyRing RE (DAR2-G2, optional) | [INFER] | organized/ absence | scoped |

**Tally:** [CERT] ×2 · [CERT-hw] ×2 · [INFER] ×2. Ratio ~0.5. Block TYPE = **ANALYSIS/§19-design**. No secret
shown. §14 back-pointer to B694 git-verified.

## Open gaps (this focus)

DAR2-G1 CLOSED (design confirmed + premise dissolved — nothing reversible to decrypt on this seed station).
Optional future: **DAR2-G2** (working decryptor, requires a station that stored reversible secrets + nre-core
KeyRing RE) — recorded, not scheduled. `jace-data-at-rest` requires-execution now 0.
