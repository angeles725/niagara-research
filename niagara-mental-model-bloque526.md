# B526 — signing-pki: dynamic-vs-static consistency verification of B518–B525 (kit toolbelt audit §11/§14)

**Focus:** `signing-pki` · **Mode:** audit / self-verify (kit toolbelt) · **Language:** English.

**Scope.** Run the kit's consistency instruments over the dynamic thread (B518–B525) to confirm the live
findings are *acorde* with the static, already-verified corpus — and fix the two dynamic-vs-static gaps the
instruments surfaced (a missing §14 reciprocal backlink in B520, and three fabricated-cite rows in
SOURCES.md). This block records the verification, not new subject findings. SECRETS DISCIPLINE observed.

---

## 1. Dynamic → static agreement (the cross-check the operator asked for)

| Dynamic claim (live) | Static anchor (already `[CERT]`) | Verdict |
|---|---|---|
| License DSA verify never enters `dsfspi`'s `DsfSha1WithDsaSignature` during `-licenses` [B524] F1 | `com/tridium/sys/license/LicenseUtil.java:172-181` = `Signature.getInstance(...)` (pure JCE); `bin/policy/java.security:72-73` `provider.2=BouncyCastleFipsProvider` ahead of Sun [B441]; no `fips140-2` feature → `bcfips` branch [B440] | **AGREE** — the live census and the static code both put the license verify in the Java BC-FIPS provider, not the native DSA |
| Module chokepoint `nre.dll SignatureUtil::checkFileSignature` IS the enforcement signal [B524] F2 | `DsfUtil::checkFileSignature` is the module `.sig` verifier [B520]/[B482]; force-invalid live produced `FATAL … failed signature check` (probe log preserved) | **AGREE** — the live return-value flip and the static single-chokepoint mapping are consistent |
| `moduleVerificationMode=low`, blacklist commented, `program.requireSigning` off [B519] | same lines in `defaults/system.properties` (442/447/474) re-read this pass | **AGREE** (re-measured, not inherited) |
| Per-JAR verdict granularity [B521] | `checkFileSignature` one-file-at-a-time + `JarSignatureRegistry` per-jar [B482] | **AGREE** |

## 2. Instrument results (kit toolbelt)

- `verify-corrections.sh`: B524's §14 correction of B520 now has the reciprocal backlink (added
  `> Corrected in B524` in `niagara-mental-model-bloque520.md` §1). B524 no longer FAILs. (21 remaining
  FAILs are pre-existing backlink debt from old blocks B21/B82/B90/B107/… — noted for the §18 retro, not
  this run's scope.)
- `verify-sources.sh`: **exit 0** after fixing the three `FABRICATED-CITE` rows — B518/B519/B521 now
  reference the exact SOURCES.md filenames (`RUN-sp-g3-live.md`, `RUN-module-verify.md`,
  `nverify-multi.txt`) instead of only the probe directory.
- `verify-parity.sh` docs/niagara-signing-hardening-guide.md vs B524: exit 0 (no load-bearing hex tokens
  in a text deliverable — nothing to check).
- `verify-block.sh` on B518/B519/B520/B521/B524/B525: all exit 0; `[INFER]/[CERT*]` ratios 0.07–0.24
  (healthy for dynamic/synthesis blocks).
- `tools/gen-catalog.py`: regenerates cleanly including B524/B525.

## 3. Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | Dynamic license-verify finding agrees with static JCE/BC-FIPS anchor | `[CERT]` | organized/baja/baja/decompiled/com/tridium/sys/license/LicenseUtil.java:172-181 |
| 2 | Dynamic module-chokepoint finding agrees with static single-chokepoint mapping | `[CERT-live]`+`[CERT]` | sources/probes/B524…/logs/nre_mm2_force.txt (`FATAL … failed signature check`) + [B520] |
| 3 | §14 reciprocal backlink restored on B520 | `[CERT]` | niagara-mental-model-bloque520.md §1 "Corrected in B524" |
| 4 | SOURCES.md fabricated-cites resolved (verify-sources exit 0) | `[CERT]` | verify-sources.sh run → exit 0 |
| 5 | verify-block ratios + gen-catalog clean | `[CERT]` | instrument runs above |

**Tally:** 4 `[CERT]`, 1 `[CERT-live]` (shared), 0 `[INFER]`, 0 unmarked.

## 4. Connections & notes

- Closes the internal-consistency debt of the B524 §14 correction + the B518–B521 probe-cite wiring.
- Pre-existing debt surfaced to the §18 retro (21 old backlinks; corpus-wide covered_blocks drift in
  other focuses) — NOT touched here to keep the change scoped.
- No new gap. Open items unchanged: SP-G9a, SP-G10a, SP-G6, SP-G8 (+ blocked SP-G3a/SP-G4/SP-G9b).
