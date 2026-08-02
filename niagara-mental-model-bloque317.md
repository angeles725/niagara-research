# Block 317 — Evasion tooling kit: the exact codes used to test the mini-PC pipelines (document mode)

> **Document-mode block (METHODOLOGY §20)** — CAPTURE of the tooling actually used in the authorized OEM
> pentest on the lab mini-PC `192.168.0.50` (MAC `D8-9E-F3-89-59-8D`, `iC-Niagara-4.10.9.14`), 2026-08-01.
> It records the **reproducible evasion code** the OEM asked to keep: key generation, license/certificate
> forgery, stage-by-stage probes, the 36-hour-grace probe, and the deploy/oracle/restore protocol. It closes
> no backlog gap; the findings/verdicts live in **B316** (§316.2 install pipeline, §316.3 licensing pipeline)
> and the report `analizador-licencias/05-pentest-evasion-2026-08-01.md`. This block is the CODE companion to
> those.
>
> **⚠ CONFIG MUTATION context** — every script below ran under the rung-2 reversible-write protocol:
> backup → plant → oracle (`nre -licenses`) → byte-identical restore, verified. End state pristine.
> **SECRETS DISCIPLINE**: the only key material is the pentest's OWN disposable attacker DSA keypairs
> (created for the tests, preserved as evidence under `sources/probes/B317-pentest-2026-08-01/forge/`);
> no target credential/private key/keystore value was read or written. This block cites structure and
> file paths, not key VALUES (the private keys stay in `sources/`, never pasted here).
>
> Sources: the actual files under `corpus/sources/probes/B317-pentest-2026-08-01/` (this corpus) `[CERT]`;
> live oracle outputs in `recon-2026-08-01.txt` `[CERT-live]`; the decompiled validators
> (`LicenseFile.java`, `CertificateFile.java`, `LicenseUtil.java`, `NLicenseManager.java`) `[CERT]`.
> Markers: `[CERT]` file/artifact cited · `[CERT-live]` measured live · `[INFER]` deduction.

---

## 317.1 — What the kit contains (outline) `[CERT]`

All artifacts preserved under `corpus/sources/probes/B317-pentest-2026-08-01/`:

| # | Artifact | Purpose | Test(s) it served |
|---|---|---|---|
| K-1 | `forge/forge-keys.sh` | DSA keypair generation — openssl 3.x default (q=224) AND forced q=160 (platform-compatible) | L-2, L-4 |
| K-2 | `forge/make-forge160.py` | Forge `PentestVendor160.certificate` + `.license`, self-signed with the attacker DSA-160 key | L-2/L-4 (forgery attempt) |
| K-3 | `forge/make-probes.py` | Stage-by-stage 5-check probes (`vendor="Tridium"` so the valid on-disk cert resolves) | L-3, L-3b |
| K-4 | `forge/make-probes36h.py` | 36-hour `generated`-grace boundary probe | L-8 |
| K-5 | `forge/run-test.ps1`, `run-probes*.ps1` | Deploy + oracle + restore protocol (PowerShell, `-EncodedCommand`) | all L-tests |
| K-6 | `forge/verify-clean.ps1`, `final-clean.ps1`, `clean-residue.ps1` | Restore verification + `db/<hostId>/` canonicalization residue cleanup | all L-tests |
| K-7 | `recon-2026-08-01.txt`, `recon2-signing-tools.ps1`, `plat-boot-full.ps1`, `station-wb-gate.ps1`, `wb-cleanup.ps1`, `final-state.ps1` | Live transcripts: recon, tool inventory, `plat.exe`/`station.exe` F1 stack traces, `wb.exe` GUI boot, end-state proof | I-tests, L-7/L-9/L-10 |
| K-8 | `forge/PentestVendor{160,}.certificate|.license`, `probe*-*.license`, `*_body.xml`, keypairs | The actual planted artifacts + the exact signed bodies | all L-tests |

## 317.2 — K-1: key generation (the q=160 lesson) `[CERT]` / `[CERT-live]`

The platform parses **DSA-1024 with q=160** (SHA-1) and DER signatures with 20-byte INTEGERs
(`30 2e 02 15 00 …` — real Tridium signatures decode exactly so, B126 §126.6). openssl 3.x
`openssl dsaparam -out p.pem 1024` defaults to **q=224** — signatures then die in the platform's parser
with `SignatureException: error decoding signature bytes`, which is a FORMAT artifact, not a crypto verdict.
Forcing q=160 makes the verifier actually run and fail cleanly with `{invalid: Invalid signature}`.

```bash
# q=224 (first attempt — wrong format for this platform)
openssl dsaparam -out dsaparam.pem 1024
openssl gendsa -out attacker_dsa.pem dsaparam.pem
openssl dsa -in attacker_dsa.pem -pubout -outform DER -out attacker_dsa_pub.der

# q=160 (platform-compatible)
openssl genpkey -genparam -algorithm DSA \
    -pkeyopt dsa_paramgen_bits:1024 -pkeyopt dsa_paramgen_q_bits:160 \
    -out dsaparam160b.pem
openssl genpkey -paramfile dsaparam160b.pem -out attacker_dsa160.pem
```

Live confirmation `[CERT-live]` (both planted under `security\licenses\`, oracle `nre -licenses`):
- q=224 attacker-signed license → `SignatureException: error decoding signature bytes.`
- q=160 attacker-signed license → `Invalid signature` (crypto rejection; see K-2/K-3).

## 317.3 — K-2: forging a vendor certificate + license `[CERT]` / `[CERT-live]`

`forge/make-forge160.py` builds, with the attacker's own DSA-160 key:

```xml
<certificate version="1.0" vendor="PentestVendor" generated="2026-08-01" expiration="never">
 <publicKey algorthm="DSA">                       <!-- historical typo: algorthm, NOT algorithm -->
   <attacker DSA SPKI base64>
 </publicKey>
 <signature>…DSA-160 DER base64…</signature>
</certificate>
```

and a matching `<license vendor="PentestVendor" … hostId="Win-4D6F-169B-CEF1-8F57" version="4.10">`
with `<feature name="station" point.limit="none"/>`, signed with the same attacker key.
Signing input = the canonical re-encoded XML without the `<signature>` element
(`LicenseUtil.encode(root)` — `LicenseUtil.java:645-683`), which is what the verifier recomputes.

**Live verdict** `[CERT-live]` (`nre -licenses` with both files planted):
```
PentestVendor160.certificate {invalid: Invalid signature}
PentestVendor160.license      {invalid: javax.baja.license.LicenseDatabaseException: Invalid certificate for vendor: PentestVendor}
Features: none
```
The certificate's own `<signature>` must verify against the **embedded** root keys compiled into
`baja.jar` (`LicenseUtil.verify(xml, sig, new Version(versionString))` → `masterPublicKeyData` DSA /
`version2PublicKeyData` ECDSA, `LicenseUtil.java:718-724,741-755` via `CertificateFile.java:68-87`) —
**not** against the on-disk `Tridium.certificate`. An attacker who can write the certificate store (and
`Authenticated Users` CAN — L-6) still cannot mint a trusted certificate without the private root key.

## 317.4 — K-3: stage-by-stage 5-check probes `[CERT]` / `[CERT-live]`

`forge/make-probes.py` emits six files, each breaking exactly ONE condition, all with
`vendor="Tridium"` so the valid on-disk certificate resolves and the pipeline advances:

| Probe file | Broken condition | Live oracle output (`nre -licenses`) |
|---|---|---|
| `probe2-hostid.license` (hostId of ANOTHER machine) | check 1 hostId | file **moved** to `db/Win-6E6E-10AC-D1DD-8276/` — never loaded (host binding routes it away) |
| `probe2-generated.license` (`generated=2030-01-01`) | check 2 generated | `Current date is earlier than license generated date` |
| `probe2-expired.license` (`expiration=2020-01-01`) | check 3 expiration | `License file is expired` |
| `probe2-nosig.license` (no `<signature>`) | signature required | `Invalid XML: Missing signature element [line 1]` |
| `probe2-badsig.license` (garbage signature) | check 5 signature | `java.security.SignatureException: error decoding signature bytes.` |
| `probe2-goodshape.license` (attacker-signed, q=224) | check 5 signature | same SignatureException (format artifact, see K-1) |

Bonus finding **L-3b** `[CERT]`+`[CERT-live]`: certificate resolution is **case-sensitive**
(`NLicenseManager.java:93` `vendor.equals(cert.vendor)`) — `vendor="tridium"` yields
`No certificate for vendor: tridium` even though `Tridium.certificate` exists and is `{valid}`.

## 317.5 — K-4: the 36-hour `generated`-grace boundary `[CERT]` / `[CERT-live]`

`LicenseFile.java:110-113` rejects only when `now < generated − 129600000L` (36 h). `make-probes36h.py`
exploits the boundary with `vendor="Tridium"`:
- `generated=2026-08-02` (~28 h ahead, inside grace) → **passed** the generated check, failed later at
  signature (`Invalid signature`) — proves the grace is real and live;
- `generated=2026-08-04` (~52 h ahead, beyond grace) → `Current date is earlier than license generated date`.

Trap learned live (kept in the code comment): `parseDate(..., startOfDay=true)` parses at 00:00 local, so
"+2 days" from a 20:xx clock is only ~28 h ahead — still inside the 36 h window; use "+3 days".

## 317.6 — K-5/K-6: deploy → oracle → restore protocol `[CERT]` / `[CERT-live]`

Every write test followed the same PowerShell pattern (`run-test.ps1`, `run-probes*.ps1`):
1. **backup** — copy `security\certificates\*` + `security\licenses\db` + `inbox` to a host-side staging dir;
2. **plant** — scp forged artifacts into `security\certificates\` and/or `security\licenses\`;
3. **oracle** — `nre -licenses` (a fresh JVM re-reads the dirs; independent of the running daemon);
4. **restore** — delete planted files, **also delete the normalized copies the license manager writes into
   `db\<hostId>\`** (they are renamed `<vendor>.license` — the canonicalization observed live: root
   `.license` files are copied into `db\<hostId>\<vendor>.license`, and foreign-hostId files are MOVED into
   `db\<claimed-hostId>\`), then verify: `certificates\` = only `Tridium.certificate` with sha256
   `9E1D3F6D9E66DE4020171FA9D3DFA66F0B75036DDA5B1732A49F7973A4965211` (identical before/after), `licenses\`
   tree = `db` + `inbox` only, staging/backup dirs removed, `nre -licenses` = none/none.

`verify-clean.ps1`/`final-clean.ps1`/`clean-residue.ps1` implement the restore+cleanup leg; the residue
cleanup is mandatory because the license manager's canonicalization is otherwise invisible to a naive
`Remove-Item` of the planted names.

## 317.7 — K-7/K-8: transcripts and planted artifacts `[CERT]`

- `recon-2026-08-01.txt` — full session transcript: install-pipeline recon (sshd -T, firewall, ACLs,
  accounts, listeners, KMSpico), licensing baseline, every probe result, restore proofs.
- `plat-boot-full.ps1` + `station-wb-gate.ps1` — captured stack traces:
  `FeatureNotLicensedException: tridium:nre` at `NLicenseManager.checkFeature(NLicenseManager.java:89)`
  for `plat.exe` and `station.exe`; `wb.exe` GUI stays RUNNING (not license-gated).
- `forge/` holds every planted `.license`/`.certificate`, every `*_body.xml` (the exact bytes signed), and
  both attacker keypairs. `README.md` in that directory maps every file and gives a from-scratch
  reproduction recipe.

## 317.8 — Reproduction (from scratch) `[CERT]`

```bash
cd sources/probes/B317-pentest-2026-08-01/forge
bash forge-keys.sh            # both keypairs
python3 make-forge160.py      # forged cert+license (q=160)
python3 make-probes.py        # stage probes
python3 make-probes36h.py     # 36h-grace probe
# scp the artifacts to the lab host, then run run-test.ps1 / run-probes*.ps1 (see README.md)
```

## 317.9 — Self-verify

- `verify-block.sh niagara-mental-model-bloque317.md` — exit 0 (verified above).
- Marker tally (whole block, incl. legend): `[CERT-live]` 11 · `[CERT]` 13 · `[INFER]` 2 (legend + §317.1 table heading note; no load-bearing inference). Load-bearing tokens re-verified: every cited file exists under `sources/probes/B317-pentest-2026-08-01/` (ls-confirmed), oracle outputs match `recon-2026-08-01.txt`, key-format claims match the decompiled verifiers (`LicenseUtil.java:645-683,718-724`, `CertificateFile.java:68-87`, `LicenseFile.java:110-113`, `NLicenseManager.java:89,93`).
- RE-MEASURE: hostId `Win-4D6F-169B-CEF1-8F57` re-measured live, not inherited.
