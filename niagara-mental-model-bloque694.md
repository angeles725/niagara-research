# B694 — Does SD possession decrypt the station? (DAR2): the machine-key domain is anchored in an ON-DISK software keyring, not the ECC508 — so physical SD possession gives all key material offline (§14 refines B466's threat model)

> Focus: **jace-data-at-rest** · Gap **DAR2** (the central question). Sources: DAR1 disk evidence [Block 693] +
> corpus [Block 466]/[Block 677]/[Block 678]/[Block 674]. Block TYPE = **ANALYSIS** (reasoning over cited
> evidence — a higher [INFER] ratio is expected and healthy; every inference names its evidence). Marker use:
> `[CERT-hw]` for disk facts, `[CERT-doc]`/`[CERT]` for cited corpus, `[INFER]` for the synthesis.
> **SECRETS DISCIPLINE:** no key value is used or shown — this is a structural/threat-model argument.

## 694.1 — The question, and the two candidate answers

Does physically possessing the boot microSD let an offline attacker DECRYPT the station's reversible/`BPassword`
fields (config.bog, `reversibleEncodingKeySource="keyring"`, [Block 685] §685.1)? Two hypotheses:

- **(H1) Software keyring on disk** — the decryption key chain lives entirely in on-card files (`.km` + `.kr`).
  → SD alone suffices.
- **(H2) Hardware-anchored** — the effective key is sealed in the **ATECC508 HSM** ([Block 678]) / bound to the
  Host ID. → SD is necessary but NOT sufficient off-hardware.

[Block 678] explicitly left this OPEN: "at-rest key protection can be anchored in the ECC508, not just the
software keyring … `[INFER on how much key material actually lives in the ECC508 → QN2-G1]`". DAR2 resolves it
with disk evidence.

## 694.2 — The disk evidence decides for H1

[CERT-hw]+[CERT] The verdict is **H1 (software keyring on disk)**:

1. `/etc/km/.km` is a **32-byte AES-256 key stored in the clear** on the card — no wrapping, no seal, no KDF
   envelope (DAR1 [Block 693] §693.3). **A key that is hardware-sealed is not left as a bare plaintext file on
   persistent storage** — it would live as a sealed blob unsealed only in RAM. Its plaintext presence at rest
   IS the proof that this key path does not gate on the ECC508. [INFER, from the structural fact]
2. The at-rest crypto engine is `libdsfspi.so` = **Mocana NanoCrypto AES-256-CBC**, a SOFTWARE JCE provider that
   consumes key material from the keyring ([Block 677] §677.5: "the engine under the keyring/at-rest material on
   the card"). It is not an ECC508 driver. [CERT [Block 677]]
3. The chain bottoms out entirely on the card: `.km` (master key, plaintext) → unwraps `.kr` (serialized Java
   KeyRing, [Block 693] §693.2) → the reversible-encoding key → decrypts config.bog fields. **And even if a QNX
   filesystem-encryption layer sits underneath, its key `.fskey/.key` is ALSO a plaintext file on the card**
   ([Block 693] §693.4) — so there is no rung of the chain that requires a secret not present on the SD. [INFER,
   from DAR1 structure]
4. The **ECC508 is for a different job**: boot attestation / hardware root-of-trust and **802.1X** network
   authentication ([Block 678] §678.5 — `Ecc508QnxRmEngine`, `wpa_supplicant`, PKI cert dir). Nothing requires
   it in the config.bog field-decryption path. [CERT [Block 678] for the engine's role]

**Conclusion:** with the SD, an offline attacker holds every key needed to decrypt the station's reversible
secrets. The "machine-key domain" is a **software keyring on removable storage**, not a hardware vault.

## 694.3 — §14 refinement of [Block 466]

[Block 466] stated (from Tridium docs, `[CERT-doc]`): a raw daemon-home copy yields secrets "sealed with a
**non-exportable machine key** → **secrets unrecoverable off-box, period**," and flagged a "raw filesystem grab
(bypassing those tools — e.g. a hypothetical serial/root copy)" as the untested caveat.

**§14 REFINE (threat-model scoping, not a flat reversal):**
- B466's verdict holds for the **NETWORK / platform-tool threat model** it actually tested (J8, [Block 473]):
  over Fox you get an encrypted `.dist`; the tools never hand you the machine key. TRUE.
- B466's "unrecoverable off-box, **period**" is **REFUTED for the PHYSICAL-SD threat model**: the machine key
  is not "non-exportable" — it is `/etc/km/.km`, a 32-byte cleartext file on the boot card (DAR1). The
  "hypothetical raw filesystem grab" B466 could not test is exactly what the SD image is, and it yields the key.
- Net: **"non-exportable" is a property of the platform TOOLS, not of the storage.** Physical media possession
  bypasses it. This upgrades [Block 674]'s "SD = full compromise of data-at-rest" from an assertion to a
  mechanism: the machine key itself is on the card.

(Back-pointer added to [Block 466].)

## 694.4 — Honest boundary: the decryption PoC is requires-execution

[INFER] This block proves the key MATERIAL and the ALGORITHM are all present on the SD with no hardware gate —
a strong, evidence-based verdict. It does NOT execute the decryption. The DEFINITIVE proof (implement the
Niagara KeyRing unwrap with `.km`, derive the reversible-encoding key, and decrypt one config.bog `BPassword`
field via Mocana AES-256-CBC) is **requires-execution** → new child gap **DAR2-G1**. Until then the claim is
"all material present, no hardware dependency in the software path" — not "field X decrypted to value Y." This
also provides disk-side evidence bearing on [Block 678]'s QN2-G1 (it does not fully close it: "how much lives
in the ECC508" for boot/802.1X still needs the live/decompile, but the DATA-AT-REST path is answered).

## Connections

- Refines [Block 466] (machine-key domain, §14 back-pointer) and [Block 674] (SD=compromise, mechanism now
  named). Resolves the data-at-rest half of [Block 678]'s QN2-G1. Keyring structure → [Block 693] (DAR1).
  Crypto engine → [Block 677]. Deployed reversible-encoding source → [Block 685] §685.1. Passphrase domain
  (portable `.dist`, still passphrase-gated) → [Block 466] (unchanged).

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | at-rest key path = software keyring on disk (H1), not ECC508 (H2) | [INFER] | 694.2 items 1-4 | reasoned from cited evidence |
| 2 | .km plaintext-at-rest ⇒ not hardware-sealed | [INFER] | [Block 693] §693.3 | structural |
| 3 | at-rest engine = software Mocana AES-256-CBC (libdsfspi) | [CERT] | [Block 677] §677.5 | corpus-cited |
| 4 | ECC508 = boot/802.1X, not the field-decrypt path | [CERT] | [Block 678] §678.5 | corpus-cited |
| 5 | every rung's key (.km, .kr, .fskey) is on the card | [CERT-hw] | [Block 693] | disk-confirmed |
| 6 | §14: B466 "unrecoverable off-box, period" refuted for physical-SD model | [CERT-doc]+[INFER] | [Block 466] + DAR1 | reasoned refine |
| 7 | decryption PoC not executed → DAR2-G1 requires-execution | [INFER] | boundary statement | honest scope |

**Tally:** [CERT-hw] ×1 · [CERT] ×2 · [CERT-doc] ×1 · [INFER] ×3. Ratio ~0.4 — EXPECTED for an ANALYSIS block
(reasoning over already-verified evidence, not new extraction). No secret used or shown. §14 back-pointer to
B466 added (git-verified below).

## Open gaps (this focus)

DAR2 CLOSED (verdict) + uncovered **DAR2-G1** (requires-execution: the actual decryption PoC). Next investigable:
**DAR3** (/etc/shadow + /etc/passwd — the QNX OS accounts, structure; hashes masked; cross-ref the exhausted
crack attempt).
