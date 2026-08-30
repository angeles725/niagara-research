# B693 — JACE_UMBRELLA keyring trio (DAR1): the reversible-encoding key store is a serialized Java KeyRing plus a 32-byte master key that sits in the clear on the card

> Focus: **jace-data-at-rest** · Gap **DAR1** (the keyring trio: `.kr` / `.km` / `.fskey`). Sources: the three
> keyring files extracted READ-ONLY from SD P2 (`local-sd-image/`, via `tools/qnx6read.py`). Redacted evidence:
> `sources/probes/B693-jace-data-at-rest/keyring-structure.txt`.
> **SECRETS DISCIPLINE MÁXIMA (live-install):** ONLY format/size/entropy is cited. No key byte is shown — not
> even a prefix of the 32-byte master key. Retro-D1 lesson applied (mask before inspect; `grep -c` verified the
> evidence file carries zero key/hash blobs). Marker `[CERT-hw]` (SD artifact).

## 693.1 — Three files, three roles

[CERT-hw] The QNX filesystem carries three keyring files (P2 tree, focus `jace8000-sd` [Block 674]):

| path | size | file type | role |
|---|---|---|---|
| `/home/niagara/security/.kr` | 665 B | **Java serialization data, v5** | the station **KeyRing**, persisted |
| `/etc/km/.km` | 32 B | raw data, no header | a **256-bit master key**, stored in the clear |
| `/.fskey/.key` | 156 B | structured blob (header + body) | a **filesystem-layer key** |

## 693.2 — `.kr` is a serialized Java KeyRing

[CERT-hw] `/home/niagara/security/.kr` begins with the public Java serialization magic `AC ED 00 05`
(STREAM_MAGIC + STREAM_VERSION 5), followed by `TC_BLOCKDATALONG` (0x7A). It is a **serialized Java object** —
the Niagara KeyRing written to disk — whose block-data body is high-entropy (236 distinct byte values across
665 B), i.e. the wrapped key entries. No plaintext class name or key value is exposed; the entries are
encrypted blobs inside the serialized stream. (KeyRing API framework internals = REMITTANCE [Block 466] / base
corpus; DAR1 opens the ON-DISK file.)

## 693.3 — `.km` is a 32-byte master key stored WITHOUT wrapping (the decisive structural fact)

[CERT-hw] `/etc/km/.km` is **exactly 32 bytes, with no header, no magic, and no wrapping structure** — a bare
256-bit key blob. Its value is NOT shown. The structural point that matters: it is stored **in the clear on the
removable card** — there is no format framing, no salt, no KDF envelope around it. `km` = key material / master
key; by position (`/etc/km/`) and size it is the **candidate key-encryption-key** that unwraps the `.kr`
KeyRing entries.

This is the crux the whole focus turns on, and it is deferred to **DAR2** for the framework-semantic judgment:
if `.km` alone unwraps `.kr` (whose entries include the `reversibleEncodingKeySource="keyring"` key that
decrypts config.bog `BPassword`/reversible fields, B685 §685.1), then **SD possession = full decryption
offline** — and B466's "config.bog indescifrable off-machine" would need refining. If instead the effective
key is ALSO bound to hardware (ECC508 HSM / Host ID, focus `jace8000-qnx-native` [Block 677]/[Block 684]), then
`.km` is necessary but not sufficient off-hardware. DAR1 establishes only the STRUCTURE (a cleartext 32-byte
key is present on the card); DAR2 resolves the binding. [INFER flagged, not asserted here.]

## 693.4 — `.fskey/.key` is a structured filesystem key

[CERT-hw] `/.fskey/.key` is 156 B: a small structured blob (an 8-byte length/type header then key material,
value MASKED). By path (`/.fskey/`) it is a **filesystem-layer key** — distinct from the station KeyRing
(`.kr`) and the master key (`.km`). Its exact consumer (QNX filesystem encryption vs a Niagara file-store key)
is not determinable from the blob alone → candidate DAR2/DAR6 follow-up.

## Connections

- Machine-key vs passphrase encryption domains → focus `jace8000` [Block 466]; per-field GCM → [Block 393];
  reversible-encoding keyring source in the deployed config → [Block 685] §685.1. ECC508 HSM + Mocana crypto →
  focus `jace8000-qnx-native` [Block 677]/[Block 684]. Keyring file locations → focus `jace8000-sd` [Block 674].
  Weak-data-at-rest verdict this focus refines → [Block 684].

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | .kr = Java serialized object (magic AC ED 00 05), 665 B, high-entropy body | [CERT-hw] | od header + file(1) | grep-confirmed (magic) |
| 2 | .km = 32 raw bytes, no header/wrapping = a 256-bit key in the clear | [CERT-hw] | size + od (no magic) | measured, value masked |
| 3 | .fskey = 156 B structured blob = filesystem key | [CERT-hw] | size + od | measured, value masked |
| 4 | .km is the candidate KEK for .kr; SD-alone-decrypt = DAR2 | [INFER] | position/size + [Block 466] | flagged (not asserted) |

**Tally:** [CERT-hw] ×3 · [INFER] ×1. Ratio 0.33. Block TYPE = **EVIDENCE** (structural read). No key/hash byte
appears in the block or the evidence file (`grep -c` for hex/base64 key blobs = 0). 3/3 structural claims
grep/size-confirmed.

## Open gaps (this focus)

DAR1 CLOSED. Next: **DAR2** — the central question: does `.km` alone (cleartext 32-byte key on the card)
unwrap the `.kr` KeyRing and thus decrypt config.bog reversible fields OFFLINE, or is the effective key also
hardware-bound (ECC508/Host ID)? Bound it from disk + corpus (B466/B677/B684); full decryption proof is likely
requires-execution.
