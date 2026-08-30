# B675 — JACE-8000 factory image + secure-boot wrapper: P3's `n4clean.tar.gz` is a LAYERED clean image (defaults + Tridium-only base cert + `signing.properties` → nested `jre.tar.gz` + a JAR-SIGNED `nre-core-update.tar.gz` → `nrecore.tar.gz`), and `n4-titan-am335x.signed` is a TI "CertISW" wrapper = ~0x350-byte cert header + ~27 MB payload + signature (focus jace8000-sd, SD-G2; §19 [CERT-hw])

> **Focus:** `jace8000-sd` (§16). **Gap closed:** SD-G2 (unpack `n4clean.tar.gz`; characterize the CertISW
> `.signed` image). **Phase:** §19 build — extracted with the `tools/qnx6read.py` reader (extended with a
> path→inode `extract`), then standard `tar`/`struct` parsing. **Marker:** `[CERT-hw]`.
> **Sources:** `sources/probes/B672-jace8000-sd/{factory-image-map.txt,certisw-header.txt}` ·
> `[CERT]` [Block 672]/[Block 674] (card + tree), [Block 392]/[Block 395] (trust anchors/signing).
> **SECRETS DISCIPLINE:** the factory image is vendor firmware and carries **no operator secrets** (it is the
> clean/default image); only its file MAP is recorded — extracted binaries stay in the scratchpad, not the repo.
>
> **Bottom line:** `n4clean.tar.gz` (43 MB) is the JACE-8000's **clean factory bootstrap**, packaged in
> **layers of signed tarballs**: the outer tar carries `opt/niagara/defaults` + `lib` + a **Tridium-only** base
> certificate + `security/policy/{java.policy,java.security,signing.properties}`, plus two nested payloads —
> `zip/jre.tar.gz` (the JRE) and `zip/nre-core-update.tar.gz`, which is itself a **JAR-signed** package
> (`META-INF/NIAGARA4.RSA/.SF`) wrapping `nrecore.tar.gz` + a native-libs checksum manifest. The bootable
> `n4-titan-am335x.signed` is a TI **CertISW** secure-boot image = a ~**0x350**-byte certificate header + the
> ~27 MB payload + signature material.

---

## §675.1 — `n4clean.tar.gz` is a layered, partly-signed factory image `[CERT-hw]`

Extracted from P3 (`tools/qnx6read.py … extract /n4clean.tar.gz`), a valid gzip, 43,313,692 B,
sha256 `da028f39…`. 26 top-level entries `[CERT-hw factory-image-map.txt]`:

```
opt/niagara/defaults/     bacnetObjectTypes.xml, system.properties, nre.properties, units*.xml,
                          migrator.properties, colorCoding.properties, lonStandardConversion.xml, …
opt/niagara/lib/          licenseAgreement.txt, readmeLicenses.txt
opt/niagara/security/certificates/Tridium.certificate     ← ONLY Tridium (no Honeywell)
opt/niagara/security/policy/   java.policy, java.security, signing.properties
zip/jre.tar.gz            → nested JRE (90 entries)
zip/nre-core-update.tar.gz→ nested SIGNED update package (7 entries):
     META-INF/{MANIFEST.MF, NIAGARA4.RSA, NIAGARA4.SF}   ← JAR code-signing (Niagara4)
     chk_niagara_natives                                  ← native-libs checksum manifest
     nrecore.tar.gz                                       ← nested again: the NRE core payload
```

**Layering:** `n4clean.tar.gz` → `zip/nre-core-update.tar.gz` (JAR-signed) → `nrecore.tar.gz`. The recovery
payload is thus delivered as **signed archives inside archives** — the same JAR-signing seen on the P3
`.signed` images ([Block 673]) and consistent with the code-signing thesis ([Block 392]).

## §675.2 — Clean image = Tridium base anchor only; OEM certs added at commissioning `[CERT-hw]`

The factory image ships **only `Tridium.certificate`** in `security/certificates/` and the base
`signing.properties`. The **deployed** P2 filesystem ([Block 674] §674.2) additionally carries
`Honeywell.certificate` + `HoneywellCentraLine.certificate` and the three `.license` files under the
Host-ID directory. So the Honeywell/CentraLine OEM trust anchors and the licenses are **applied during
commissioning/licensing, not baked into the clean image** — a concrete confirmation of the multi-domain
trust-anchor model in [Block 392] (Tridium base vs Honeywell OEM PKI), observed here as a before/after on the
same device's media.

## §675.3 — `n4-titan-am335x.signed` = TI "CertISW" secure-boot wrapper `[CERT-hw]`

Header of the P3 copy (27,149,316 B) `[CERT-hw certisw-header.txt]`: magic `CertISW\0`, then u32 fields with
`[11]=0x350` (=848, plausible **cert-header length**), `[12]=0x19e40b4` (=27,148,020, plausible **payload
length** — file size minus header/sig), and trailing `0x7ca18bd7 / 0x84b8d7c5 / 0x7340ed2c` (signature/hash
material). So the layout is **cert header (~0x350 B) → payload (~27 MB) → signature** — TI's GP/HS secure-boot
image the ROM/SPL verifies before `go 0x80FFFC00` ([Block 672] §672.3). Field *roles* are `[INFER]` (sizes
line up with the file); the bytes are `[CERT-hw]`.

**Build delta:** the P3 factory `.signed` is 27 MB; the P1 **deployed** `.signed` is 40 MB ([Block 672]) — a
different (larger, commissioned) build than the clean factory copy. `[CERT-hw sizes; INFER "commissioned vs clean"]`

## §675.4 — Residual `[CERT-hw]`

- **SD-G2b (requires binary tooling):** decode the X.509/ASN.1 **certificate chain inside the CertISW header**
  (who signs the boot image, key sizes) and unpack `nrecore.tar.gz` to inventory the factory native/module set.
  The outer structure is mapped; the ASN.1 cert decode is the deeper, lower-value step.
- **SD-G3 (blocked):** live serial confirmation that the SPL/ROM actually verifies the CertISW cert before
  `go` — needs a boot capture on the unit.

## §675.5 — Self-verify

| # | Claim | Marker | Cite |
|---|---|---|---|
| 1 | n4clean.tar.gz extracted (43 MB, valid gzip, sha256 da028f39…) via the reader's extract mode | [CERT-hw] | factory-image-map.txt |
| 2 | Outer tar = defaults + lib + Tridium-only cert + security/policy + 2 nested payloads | [CERT-hw] | factory-image-map.txt |
| 3 | nre-core-update.tar.gz is JAR-signed (NIAGARA4.RSA/.SF) and wraps nrecore.tar.gz + chk_niagara_natives | [CERT-hw] | factory-image-map.txt |
| 4 | Clean image has only Tridium anchor; Honeywell/CentraLine + licenses added at commissioning (present on P2) | [CERT-hw] | §675.2; [Block 674] |
| 5 | n4-titan-am335x.signed = CertISW magic + ~0x350 cert header + ~27MB payload + signature | [CERT-hw bytes] + [INFER roles] | certisw-header.txt |
| 6 | P3 factory .signed (27MB) ≠ P1 deployed .signed (40MB) | [CERT-hw] | [Block 672]; certisw-header.txt |

**Tally:** 6 claims — 6 [CERT-hw] (two carry inline [INFER] on field-role/build interpretation). 0 unmarked.

## §675.6 — Connections

- **[Block 674]** — the P3 tree that held `n4clean.tar.gz`; the P2 deployed certs/licenses this compares against.
- **[Block 672]** — the P1 deployed `.signed` (40 MB) + boot chain the CertISW image plugs into.
- **[Block 392]/[Block 395]** — trust-anchor domains (Tridium base vs Honeywell OEM); §675.2 is a live before/after.
- **[Block 673]** — the NIAGARA1/4.RSA JAR signatures first seen as strings; here mapped in the archive layers.
