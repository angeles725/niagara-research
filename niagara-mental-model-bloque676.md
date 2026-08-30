# B676 — JACE-8000 factory core signing + payload inventory: the `nrecore` payload is signed by Tridium's GENUINE public DigiCert code-signing cert (`CN=Tridium, Inc` → `DigiCert SHA2 Assured ID Code Signing CA` → `DigiCert Assured ID Root CA`, RSA-2048/SHA-256) — NOT the Honeywell PKI that re-signs deployed modules ([Block 392]); the NRE core = a BouncyCastle-FIPS + Jetty runtime (17 jars + 12 native `.so`); and the `CertISW` boot wrapper is TI GP-format (no X.509 chain) (focus jace8000-sd, SD-G2b; §19 [CERT-hw])

> **Focus:** `jace8000-sd` (§16). **Gap closed:** SD-G2b (decode the boot-image/payload cert chain; inventory
> `nrecore.tar.gz`). **Phase:** §19 build — extracted the nested factory tarballs with `tools/qnx6read.py` +
> `tar`, decoded the PKCS#7 signature with `openssl`. **Marker:** `[CERT-hw]` (public PKI info, not secret).
> **Sources:** `sources/probes/B672-jace8000-sd/factory-signing-chain.txt` · `[CERT]` [Block 675] (image
> layers), [Block 392]/[Block 395] (signing domains), [Block 380] (FIPS provider).
> **Preserved media:** the full 4 GB raw image + extracted payloads are kept OUT of the repo at
> `/home/cristian/jace8000-sd-image/` (contains secrets; sha256 of the image recorded in its README).
>
> **Bottom line:** the factory NRE core (`nrecore.tar.gz`, 20 MB) is a **BouncyCastle-FIPS + Jetty** Java
> runtime (17 jars incl. `bc-fips`, `bctls-fips`, `hsm`, `nre.jar`, `niagarad.jar`, `runtime.jar`) plus 12 QNX
> native `.so` and the `niagarad`/`nre`/`station` binaries. Its `META-INF/NIAGARA4.RSA` is a real PKCS#7 chain
> signed by **Tridium's public DigiCert code-signing certificate** (`Tridium, Inc / Niagara Modules` →
> `DigiCert SHA2 Assured ID Code Signing CA` → `DigiCert Assured ID Root CA`, RSA-2048/SHA-256) — the genuine
> Tridium factory anchor, distinct from the Honeywell Product PKI that re-signs OEM-deployed modules
> ([Block 392]). The `n4-titan-am335x.signed` **CertISW** wrapper carries **no X.509 chain** — it is TI
> GP-format (a raw hash/signature header), so the certificate story lives at the JAR layer, not the boot header.

---

## §676.1 — `nrecore.tar.gz` = the NRE core runtime `[CERT-hw]`

Extracted (P3 → n4clean → nre-core-update → nrecore, 19,992,545 B, 57 entries)
`[CERT-hw factory-signing-chain.txt]`:
- **12 native `.so`** (QNX/ARM): `libnre`, `libnjre`, `libdsfspi`, `libcommon`, `libbacnet`, `libplatccn`,
  `libplatform`, `libplatmstp`, `libplatnrio`, `libpower`, `libserial`.
- **Binaries:** `niagarad`, `nre`, `station`; plus `ext/`.
- **17 core jars:** `bc-fips-1.0.2.jar`, `bctls-fips-1.0.10.jar`, `bcpkix-fips-1.0.3.jar`,
  `bc-bcfkswrapprov-1.0.0.jar`, `bcprov-jdk15on-1.66.jar`, `bcpkix-jdk15on-1.66.jar`, `bctls-jdk15on-1.66.jar`,
  `hsm-2.0.1.jar`, `jetty-all-compact3-9.4.26.jar`, `jettyWrapper.jar`, `javax.servlet-api-3.1.0.jar`,
  `encoder-1.2.2.jar`, `nre.jar`, `niagarad.jar`, `runtime.jar`, `tridium-JavaMail-1.5.2.1.jar`,
  `tridium-activation-1.0.jar`.

So the JACE core boots a **BouncyCastle-FIPS crypto stack + an embedded Jetty** under `nre`/`niagarad`; the
FIPS jars corroborate the FIPS-gated provider seen natively in [Block 380]. The 173 application modules of the
deployed station ([Block 674]) are layered on top of this core at commissioning.

## §676.2 — The factory payload is Tridium/DigiCert-signed (RSA-2048/SHA-256) `[CERT-hw]`

`META-INF/NIAGARA4.RSA` (PKCS#7, 7692 B) decodes to a 3-cert chain:

| Role | Subject | Issuer |
|---|---|---|
| leaf | `C=US, ST=Virginia, L=Richmond, O=Tridium, Inc, OU=Niagara Modules, CN=Tridium, Inc` | DigiCert SHA2 Assured ID Code Signing CA |
| intermediate | DigiCert SHA2 Assured ID Code Signing CA | DigiCert Assured ID Root CA |
| root | DigiCert Assured ID Root CA | (self) |

Signature: **`sha256WithRSAEncryption`, RSA 2048-bit** `[CERT-hw]`.

**This is the genuine, publicly-chained Tridium code-signing certificate** (DigiCert public CA) — the factory
base anchor. It **contrasts with [Block 392]**, which found the OEM-**deployed** modules re-signed to the
**Honeywell Product PKI**. Reconciled as a before/after on the same device's own media:
- **Factory image (this block):** Tridium → DigiCert (public CA). The clean image also ships only
  `Tridium.certificate` ([Block 675] §675.2).
- **Deployed (B392 / [Block 674]):** Honeywell/CentraLine certs added, modules re-signed to Honeywell PKI.

So the OEM re-signing is a **commissioning-time** transform, not baked into the Tridium factory core — a
concrete [CERT-hw] confirmation of the multi-domain trust model in [Block 392].

## §676.3 — The `CertISW` boot wrapper has NO X.509 chain (TI GP-format) `[CERT-hw]`

Scanning the `n4-titan-am335x.signed` CertISW header (first 0x400) finds **no ASN.1 SEQUENCE (`0x30 0x82`)**;
the region around the `0x350` length field is high-entropy (`b6fd95ca3e5b418a…`) — a raw hash/signature, not a
DER certificate `[CERT-hw factory-signing-chain.txt]`. So the `CertISW` wrapper is a **TI GP (general-purpose)
image header with a raw signature block**, not an HS X.509 certificate chain. The answer to "is there a
certificate chain inside the boot image?" is **no** — the verifiable certificate chain lives at the **JAR
layer** (§676.2), and the boot image relies on TI GP-format integrity + the SPL/U-Boot chain ([Block 672]).
Full TI-GP header field decode is out of scope (low value; the X.509 question is settled).

## §676.4 — Focus close-out `[CERT-hw]`

With SD-G2b closed, the `jace8000-sd` read-only + requires-execution set is exhausted. Only **SD-G3** remains,
and it is **blocked** (needs a live serial boot capture to confirm the ROM/SPL verifies the GP signature
before `go 0x80FFFC00` — same hardware wall as jace8000 J7-G1). The full raw image is preserved out-of-repo at
`/home/cristian/jace8000-sd-image/` for any future pass.

## §676.5 — Self-verify

| # | Claim | Marker | Cite |
|---|---|---|---|
| 1 | nrecore = 12 native .so + niagarad/nre/station + 17 jars (BC-FIPS + Jetty + nre/runtime) | [CERT-hw] | factory-signing-chain.txt |
| 2 | NIAGARA4.RSA = PKCS#7 chain Tridium → DigiCert SHA2 Assured ID Code Signing CA → DigiCert Assured ID Root CA | [CERT-hw] | factory-signing-chain.txt |
| 3 | signature = sha256WithRSAEncryption, RSA-2048 | [CERT-hw] | factory-signing-chain.txt |
| 4 | factory anchor = Tridium/DigiCert; Honeywell PKI re-signing is commissioning-time (B392/B674) | [CERT-hw] + [CERT] | §676.2; [Block 392] |
| 5 | CertISW header has no X.509 (no 0x3082 in first 0x400; high-entropy sig) → TI GP-format | [CERT-hw] | factory-signing-chain.txt |
| 6 | BC-FIPS core corroborates the FIPS provider of B380 | [CERT-hw] + [CERT] | §676.1; [Block 380] |

**Tally:** 6 claims — 6 [CERT-hw] (two with [CERT] cross-refs). 0 unmarked.

## §676.6 — Connections

- **[Block 675]** — the layered factory image whose innermost payload this block signs-verifies + inventories.
- **[Block 392]/[Block 395]** — trust-anchor domains; §676.2 is the concrete factory-vs-deployed observation.
- **[Block 674]** — the deployed P2 (Honeywell certs + 173 modules) this contrasts against.
- **[Block 380]** — the FIPS-gated native provider; the BC-FIPS jars here are its Java side.
- **[Block 672]** — the boot chain the (GP-format) CertISW image plugs into.
