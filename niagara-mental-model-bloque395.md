# Block 395 — SP-G5: the vendor-certificate chain, cryptographically re-verified — every vendor cert (Tridium included) is signed by a HIDDEN embedded DSA root in `baja.jar`, not self-signed (corrects B392 §392.4)

> **Focus:** `signing-pki` (gap **SP-G5**). [B392 §392.4] asserted the DSA `.certificate` files chained to a
> "self-signed 2003 Tridium root". This block **independently re-verifies the DSA-160 signatures** and
> corrects that: the on-disk `Tridium.certificate` is **not** the root and **not** self-signed — all three
> vendor certs (Tridium, Honeywell, HoneywellCentraLine) are signed by a **separate embedded master DSA key**
> baked into `baja.jar`, which is the true root of the licensing/vendor trust domain (Domain B).
>
> **Method:** replicated `com.tridium.sys.license.LicenseUtil.encode` + `CertificateFile.load` semantics in
> Python (`cryptography`), extracted the embedded key bytes from the decompiled `LicenseUtil`, and ran the
> actual `SHA1withDSA` verification. `[CERT]` = the verification returned VALID/INVALID as stated.
>
> **Sources `[CERT]`:** `security/certificates/{Tridium,Honeywell,HoneywellCentraLine}.certificate`;
> `organized/baja/baja/vineflower/com/tridium/sys/license/{LicenseUtil,CertificateFile}.java`;
> tool `tools/niagara-license-tool.py` (canonicalization reference, [B323]).
> **Remittance:** [B322] (embedded-root model, `CertificateFile.java:74`→`getMasterPublicKey`); [B392]
> (three trust domains — this refines Domain B); [B126 §126.1]/[B387] (DSA+ECDSA master keys).

---

## 395.1 — What was verified `[CERT]`

`CertificateFile.load` (`CertificateFile.java:69-84`): removes `<signature>`, computes
`xml = LicenseUtil.encode(root)`, then `LicenseUtil.verify(xml, sig, new Version(version))`. For a cert with
`version="1.0"` that path selects `getMasterPublicKey()` — the **embedded DSA key** — with
`Signature.getInstance("DSA")` = SHA1withDSA (`LicenseUtil.java:718-724, 741-748`). `[CERT]`

The canonical bytes = `encode()` (`LicenseUtil.java:656-683`): `<qname attrs>\n`, then each content node
(child elements recurse; **text nodes written verbatim + `\n`**), then `</qname>\n`. The critical detail the
`.license`-validated tool missed: for a **certificate** the `<publicKey>` element's **base64 text is content**
that `encode()` includes RAW (with its MIME line-breaks). The tool's `canonical_encode` drops all text
(fine for licenses whose `<feature>` children are empty, wrong for certs). Reproducing the RAW-text encode is
what made verification succeed. `[CERT]`

## 395.2 — Result: all three vendor certs verify against the embedded DSA root `[CERT]`

| Certificate (`version="1.0"`) | vs its OWN pubkey | vs embedded master DSA | verdict |
|---|---|---|---|
| `Tridium.certificate` | INVALID | **VALID** | signed by embedded root, **NOT self-signed** |
| `Honeywell.certificate` | INVALID | **VALID** | signed by embedded root |
| `HoneywellCentraLine.certificate` | INVALID | **VALID** | signed by embedded root |

The embedded key ≠ the on-disk Tridium key: embedded `masterPublicKeyData` = **444-byte DER DSA-1024**
(sha256 `aed58673c799ab1e…`), whereas `Tridium.certificate`'s SPKI is **443 bytes** — a different key.
So the trust hierarchy of Domain B is: `[CERT]`

```
Embedded master DSA-1024 (baja.jar LicenseUtil.masterPublicKeyData, HIDDEN root, ships in every N4)
  ├─ signs → Tridium.certificate            (DSA-1024 vendor key)
  ├─ signs → Honeywell.certificate          (DSA-1024 vendor key)
  └─ signs → HoneywellCentraLine.certificate (DSA-1024 vendor key)
              └─ each vendor key then signs that vendor's *.license
```

## 395.3 — Two embedded roots: the DSA/ECDSA dual system in this build `[CERT]`

The 4.14 build carries **two** embedded roots in `LicenseUtil` (the dual-key system of [B387]; the
iC-Niagara lab build of [B322] had only the DSA one): `[CERT]`
- `masterPublicKeyData` = **DSA-1024** (444 B DER, sha256 `aed58673…`) — selected for `version` ≠ `2.0`.
- `version2PublicKeyData` = **ECDSA secp256r1/P-256** (91 B DER, sha256 `7d766e9c…`) — selected only when
  the license/cert declares `version="2.0"` (`LicenseUtil.verify(...,Version,...):718-724`).

All three shipped vendor certs are `version="1.0"` → they route to the DSA root. The ECDSA v2 root is present
but unused by these certs — the newer signing path, ready but not yet the vendor-cert format here. `[CERT]`

## 395.4 — Why it matters `[INFER]`

This is the licensing/vendor analog of the module **TPK** ([B392 §392.6]): the true root of the license +
vendor-cert domain is **hidden inside `baja.jar`**, identical in every N4, and it is what makes "any N4
accepts Tridium/OEM licenses" true — not the visible `Tridium.certificate` file (which is itself just a
signed leaf). Consequence for tampering: swapping `Tridium.certificate` on disk does nothing unless the new
key is signed by the embedded root; but anyone who can **rebuild `baja.jar` with their own
`masterPublicKeyData`** owns the entire license + vendor trust domain (the DSA analog of the module
`skipModuleValidation`/`changeit` levers). The DSA-1024 root remains the weak link ([B392]: 1024-bit, Sun
default params) — now confirmed as the *actual* verifying key, not a display artifact. `[INFER]`

---

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | Cert sig path = encode(root minus signature) → SHA1withDSA vs getMasterPublicKey for version≠2.0 | [CERT] | `CertificateFile.java:69-84`, `LicenseUtil.java:718-748` |
| 2 | Canonical form includes the `<publicKey>` base64 text RAW (+\n), which the license tool dropped | [CERT] | `encode()` L656-683; raw-text variant = only one that verifies |
| 3 | All 3 vendor certs verify VALID against the embedded master DSA key | [CERT] | independent SHA1withDSA verify (§395.2 table) |
| 4 | Tridium.certificate is NOT self-signed (443 B on-disk ≠ 444 B embedded; INVALID vs own key) | [CERT] | key-size diff + verify results — **corrects [B392 §392.4]** |
| 5 | Embedded master = DSA-1024 (444 B, sha256 aed58673…); v2 = ECDSA P-256 (91 B, sha256 7d766e9c…) | [CERT] | extracted from LicenseUtil byte arrays + parsed |
| 6 | version="2.0" routes to the ECDSA root; all shipped certs are 1.0 → DSA | [CERT] | `LicenseUtil.verify(...,Version):718-724` |
| 7 | Embedded root (not the on-disk file) is the true license/vendor trust anchor | [INFER] | §395.4; parallels module TPK [B392] |

**Tally:** 6 [CERT], 1 [INFER], 0 unmarked. The central claim (embedded-root, not self-signed) is [CERT].

## Connections
- **Closes SP-G5** with independent cryptographic proof (not just code reading).
- **CORRECTS [B392 §392.4]:** `Tridium.certificate` is a signed leaf, not a self-signed root; the root is
  the hidden embedded `masterPublicKeyData` in `baja.jar`. [B392] edited with a pointer.
- **Confirms [B322]** (`CertificateFile.java:74`→`getMasterPublicKey`) and extends it to the 4.14 dual-key
  build; **grounds [B387]** (DSA+ECDSA master keys) with the exact byte sizes/fingerprints.
- **Completes Domain B** of [B392]'s three-domain map.

## Open gaps (updated `signing-pki` backlog)
- **SP-G7** (NEXT) — any optional integrity channel for the local data record beyond SIEM offload ([B75])? `[investigable]`
- **SP-G3** (native `LicenseUtil::isFeaturePresent` text-match vs Java DSA verify), **SP-G6** (CRL),
  **SP-G8** (OTA ECDSA enforcement) — `[requires-execution]`.
- **SP-G4** — Tridium-rooted non-OEM `baja.jar` — `[blocked: requires-artifact]`.
- After SP-G7 the focus reaches investigable=0 → STOP candidate.
