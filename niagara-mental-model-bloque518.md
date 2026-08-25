# B518 — signing-pki SP-G3-live: the Niagara runtime license verifier fails closed on a tampered DSA signature (dynamic §12, live-install)

**Focus:** `signing-pki` · **Gap:** SP-G3 (PARTIAL → closed for the runtime-verifier claim) · **Mode:** dynamic phase (METHODOLOGY §12) · **Language:** English (convention since B115).

**Scope.** Upgrade the SP-G3 finding from `[CERT]`-offline (a `LicenseUtil.verify` replica, B323/B397) to `[CERT-live]` against the **real Niagara runtime**: does the shipping runtime reject a license whose DSA signature has been altered by a single byte, and does it fail *closed* (withhold the license) rather than open? Out of scope here: whether a *required* feature's absence drives the full station process to `System.exit(-3/-6)` at boot (named as a child nuance below); the native-side gate asymmetry (delegated to the peer native session — B126 §126.6).

**Sources consulted (three, per protocol).**
- FUENTE 1 (corpus): [B126 §126.6] native `LicenseUtil::isFeaturePresent` = text match; [B323] byte-compatible signer/verifier validated live; [B397] §12 first live pass — verifier proven to reject sig+payload tampering *offline*; [B482] `LicenseUtil.verify` canonicalization; [B483] license/`<signature>` anatomy; [B488] limit-enforcement (`System.exit(-3)`). SP-G3 backlog row in `RESEARCH-STATE-signing-pki.md`.
- FUENTE 2 (niagara-help): licensing is documented as workflow (License Manager), not as a verify-internals surface — no runtime tamper-behavior topic (consistent with prior signing-pki passes; the fail-behavior is a code/runtime fact, not a doc one).
- FUENTE 3 (live runtime, §12): the real `nre.exe` runtime executed via WSL→Windows interop against the operator's own isolated OptimizerSupervisor N4.14.0.162 install, station LIVE at the time (`https://localhost/`=302, platform daemon `:5011`=403).

**Evidence markers:** `[CERT-live]` = observed against the running Niagara runtime; `[CERT]` = verbatim in local code/prior block; `[INFER]` = reasoned. **SECRETS DISCIPLINE (live-install):** HostId is shown structurally as `Win-XXXX-XXXX-XXXX-XXXX`; no secret values; test credentials never persisted to corpus/memory/git. Preserved probe evidence: `sources/probes/B518-sp-g3-live-2026-08-25/RUN-sp-g3-live.md` (+ `oracle-01-baseline.txt`, `oracle-02-wronghost.txt`).

---

## 1. Setup and reversibility (the write-supervised protocol actually followed)

This gap required a WRITE to the subject (swapping a license file), so it ran under the METHODOLOGY §12 / `RUNBOOK-REVERSIBILIDAD` protocol:

1. **backup-before-destroy** — `security/licenses` + `security/certificates` copied to a staging dir with a `sha256` manifest (15 files) BEFORE any mutation. `[CERT-live]`
2. **independent oracle** — the state is read back through `nre.exe -licenses` (a *fresh JVM* that re-reads `security/` and re-runs the verifier), never through the channel that wrote the file. `[CERT-live]`
3. **guaranteed restore** — the mutation ran inside a shell `trap … EXIT` that restores the original file even on error/interrupt. `[CERT-live]`
4. **byte-identical proof** — post-restore `sha256(Webs.license)` == baseline `fc548614…daec72`. CONFIRMED. `[CERT-live]`
5. **no live reboot** — `nre -licenses` is a separate JVM; the already-running station holds its licenses in an in-memory map loaded at its own boot and is invisible to the transient file swap. The production-adjacent `HoneywellMX60` station was never targeted; only `Webs.license` (Tridium) was touched, for seconds. `[CERT-live]` / `[INFER]` (invisibility to the running station is reasoned from the load-once model, [B481]).

## 2. Baseline (intact) — the oracle's known-good state

`nre.exe -licenses` on the untouched install: `[CERT-live]`

```
Niagara Licensing
HostId=Win-XXXX-XXXX-XXXX-XXXX
Certificates:  Honeywell / HoneywellCentraLine / Tridium  — all {valid} [expires: never]
Licenses (perpetual):
  Honeywell.license <Honeywell> [expires: 2027-03-31] {valid}
  HoneywellCentraLine.license <HoneywellCentraLine> [expires: 2027-03-31] {valid}
  Webs.license <Tridium> [expires: 2027-03-31] {valid}
```

The three `.certificate` files (the embedded-root-signed vendor certs of [B395]) validate, and the three `.license` files validate against them. This is the state SP-G3-live perturbs and must restore.

## 3. The tamper and the runtime's response — fail-closed CONFIRMED

Target: `Webs.license` (Tridium-vendored, DSA-1024/SHA-1 signed per [B484]/[B395]). Mutation: flip **one byte inside the signature region**, length preserved — offset 16153, `0x68 → 0x69`, file length unchanged at 16193 bytes (so the parse succeeds and the failure is cryptographic, not structural). `[CERT-live]`

Oracle with the tampered file in place: `[CERT-live]`

```
GRAVE [baja] License file not loaded - Webs.license {invalid: Invalid signature}
...
Licenses (perpetual):
  Honeywell.license           {valid}
  HoneywellCentraLine.license {valid}
  Webs.license                {invalid: Invalid signature}
```

Three facts, all `[CERT-live]`:
- **Real cryptographic verify.** A 1-byte change in the signature bytes → `{invalid: Invalid signature}`. The runtime is checking the DSA signature, not merely parsing or string-matching the file. This is the runtime confirmation of the offline replica result (B397), now on the shipping `baja` verifier.
- **Fail-closed, not fail-open.** The reaction is *"License file not loaded"* — the license and every feature it grants are **withheld**, logged at `GRAVE`. A tampered license buys nothing; it does not degrade to "accept anyway".
- **Per-file isolation.** The other two licenses stay `{valid}`. One bad license is dropped; it does not poison the whole license set. `[CERT-live]`

## 3b. Second validity gate isolated live — HostId binding (a valid signature is not enough)

Same reversible protocol, different vector: instead of breaking the signature, I placed a **validly-signed** `Honeywell.license` that is bound to a **different HostId** (a real `Qnx-TITAN-…` license from the install's own `db/`, i.e. correct signature, wrong machine) as the active license on this `Win-XXXX` host. `[CERT-live]`

```
INFORMACIÓN [sys.license] moved file:!security/licenses/Honeywell.license
HostId=Win-XXXX-XXXX-XXXX-XXXX
Licenses (perpetual):
  HoneywellCentraLine.license {valid} · Webs.license {valid}     ← Honeywell.license GONE
Features:  (all honeywell:* features ABSENT)
```

Findings, all `[CERT-live]`:
- **HostId is a hard, independent gate.** A cryptographically-valid license whose HostId ≠ this machine is **not honored** — its features vanish. This is a *different* rejection path from §3: no "Invalid signature" here; the signature was fine, the *binding* failed.
- **The reaction is "moved file", not "load anyway".** The runtime's `sys.license` layer **relocates** a wrong-host license out of the active `security/licenses/` set (into the `db/<HostId>/` sort area). Fail-closed again, by removal.
- **Two of three validity gates now proven live.** Gate-1 signature (§3) and gate-2 HostId (§3b) each fire independently. Gate-3 is the date window (`[expires: …]`, [B483]) — visible in every listing, not separately tampered here.
- **Reversibility held through a runtime side-effect.** The "moved file" action is itself a write by the runtime; the post-test whole-tree sha256 manifest was verified **byte-identical to baseline** (the moved file overwrote its identical twin in `db/`, and the active file was restored from backup). No residue. `[CERT-live]`

## 3c. Who checks, who controls, how you know it runs (live process model)

Answering the "which/how-many processes, who controls, how is it active" cluster from live observation + corpus: `[CERT-live]` + `[CERT]`(cited)
- **Exactly two long-lived processes consume licenses, confirmed live** (`tasklist`): `niagarad.exe` (the platform daemon — runs its OWN platform-feature license manager, [B478]) and `station.exe` (the station JVM — loads `security/licenses` into an in-memory feature map at boot, [B481]). A third, `nre.exe`/`njre`, checks **on demand / at launch**, not continuously (it is the oracle used here). `[CERT-live]`
- **Who controls it:** the Windows service **`Niagara`** (`Running`) = `niagarad`, which **supervises** the station and treats license-failure exit codes `-3`/`-6` as non-recoverable ([B478]). So the daemon is both a license consumer and the station's lifecycle controller. `[CERT-live]`/`[CERT]`
- **How you know it is active:** service `Running` + `station.exe`/`niagarad.exe` in the task list + `https://localhost/`=302 + platform daemon `:5011`=403. `[CERT-live]`
- **What if it is off / not watching:** licensing is checked **at load** (boot), then the features live in memory. Node-locked licensing has **NO runtime re-watcher** ([B481]/[B487]); only the *subscription* mode runs a live entitlement watchdog. So once the station is up, nothing continuously re-verifies the on-disk license — which is exactly why the transient swap-and-restore here was invisible to the running `station.exe`. `[CERT]`(cited)/`[INFER]`

## 3d. The root of trust — what makes the check "see it right", and how it could be fooled

- **What anchors correctness ("ver todo bien"):** the verifier trusts a **hidden master public key embedded in `baja.jar`** (DSA-1024, plus a v2 ECDSA-P256 root) against which every `.license`/`.certificate` signature is checked ([B395]/[B482]). You cannot forge a license without the corresponding **private** key, which Tridium/Honeywell hold — that is why the 1-byte flip (§3) is caught. `[CERT]`(cited), corroborated live (§3).
- **How it could be "confused" (see something different):** NOT by faking the signature (proven fail-closed). The soft spot is **gate-2's input**: the HostId is a **non-cryptographic fold-XOR of machine attributes** (hidden key + RegisteredOwner + product id + C: volume serial), vendor hardcoded ([B424]). Forging a signature is infeasible; **spoofing the machine attributes that derive a target HostId** is the realistic vector — a genuine license for HostId *H* would then validate on any machine coerced to present *H*. The crypto gate is strong; the identity gate rests on spoofable host inputs. `[CERT]`(cited) — this is the live-confirmed shape of the [B424] finding, not a new exploit (no forging attempted).

## 3e. The native half — asymmetry confirmed on the SAME tampered file (peer native session)

The peer native session closed the native side READ-ONLY (no `nre.exe` execution, no patching, no `bin/`/license writes). Evidence: `sources/probes/B518-sp-g3-native-2026-08-25/RUN-sp-g3-native.md`.
- **Anchor (B424 satisfied):** `nre.dll` sha256 `606ff1c6…` == baseline; `?isFeaturePresent@LicenseUtil@@…` is an **exported** symbol (VA `0x180001f90`), so the offset re-anchors via the export table — no twin-binary risk. `[CERT]`
- **Native gate body (r2 `pdf`), re-anchored [B126 §126.6]:** the whole function is `sprintf` of two needles (`<license vendor="%s"`, `<feature name="%s"`) + `\security\licenses` + `DirectoryListing` + a **substring match**. **Zero DSA/signature calls.** `[CERT]`
- **Dynamic result WITHOUT native execution (and stronger for it):** Webs.license `<signature>` spans bytes **[16094, 16169)**; the §3 tamper offset **16153 is inside** it. Every needle the native gate matches lies **< 16094** (last `<feature name=` @15963). Therefore the native text-match is **invariant to that flip — and to any tamper of the entire signature region** — and keeps reporting the feature **PRESENT**. Deductive over two `[CERT]` facts (body has no verify + needles disjoint from the tampered region), covering the whole signature region, not one offset. `[CERT]`/`[INFER]`
- **Not done (honest boundary):** the live native return over the tampered file was not observed — a cold `LoadLibrary`+`GetProcAddress` harness faults on an early `Nre::getInstance()` deref, and Linux Frida cannot inject the Windows PE. Escalation to a Windows-instrumented run is left to explicit operator decision; the static+data `[CERT]` already settles the claim.

**Combined verdict (both halves, same file):** Java runtime **rejects** the signature-tampered license fail-closed `[CERT-live]`; the native `LicenseUtil::isFeaturePresent` text-match **accepts** it (feature still PRESENT) `[CERT]`. The [B126 §126.6] licensing-integrity **asymmetry is now empirically confirmed on both sides** — the native quick-gate is a text scan, only the Java load path is cryptographic.

## 4. What this does and does NOT settle (§14 precision)

- SETTLED: the **Java/runtime license verifier fails closed on signature tampering** — SP-G3's central claim, now `[CERT-live]`. Corrects nothing; it *upgrades* B397's offline result and stands beside [B126 §126.6]'s native text-match as the Java half of the asymmetry.
- NOT settled here (child nuance): whether a *required-but-missing* feature forces the **station process** to `System.exit(-3/-6)` at full boot ([B488]) vs the runtime simply withholding features and running degraded. SP-G3-live proves **verifier rejection**, not the **process-exit path** — those are different fail modes. Named as **SP-G3a** (requires a full throwaway-station boot with a required feature tampered; still `requires-execution`).
- NOT in this block: the **native** gate's behavior on the same tampered file (does `nverify`/native `LicenseUtil` text-match miss it?) — delegated to the peer native session under the same reproducible method.

## 5. Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | Runtime rejects a 1-byte signature flip in `Webs.license` | `[CERT-live]` | `GRAVE [baja] … Webs.license {invalid: Invalid signature}` via `nre.exe -licenses` |
| 2 | Reaction is fail-CLOSED (license not loaded, features withheld) | `[CERT-live]` | "License file not loaded"; `Webs.license {invalid}` in the Licenses listing |
| 3 | The check is cryptographic (length preserved, parse OK) | `[CERT-live]` | file length unchanged 16193; failure text = "Invalid signature", not a parse error |
| 4 | Other licenses unaffected (per-file isolation) | `[CERT-live]` | Honeywell/CentraLine `.license` remain `{valid}` |
| 5 | Restoration byte-identical; subject intact | `[CERT-live]` | post-restore sha256 == baseline `fc548614…daec72` |
| 6 | Station was live during the test | `[CERT-live]` | `https://localhost/`=302; platform daemon `:5011`=403 |
| 7 | HostId gate rejects a valid-signature/wrong-host license (moved out, features withheld) | `[CERT-live]` | `[sys.license] moved file … Honeywell.license`; honeywell:* features absent |
| 8 | Whole-tree restore byte-identical after the runtime's own "moved file" write | `[CERT-live]` | post-test manifest == baseline; live oracle back to 3×{valid} |
| 9 | Two long-lived license consumers live: niagarad + station; service `Niagara` Running | `[CERT-live]` | `tasklist`; `Get-Service Niagara` = Running |
| 10 | Native `isFeaturePresent` = text scan, no signature check (re-anchored) | `[CERT]` (peer) | nre.dll export `0x180001f90`; r2 body = 2 needles + substring, 0 DSA calls |
| 11 | Native gate invariant to the §3 signature tamper → asymmetry confirmed | `[CERT]`/`[INFER]` (peer) | `<signature>`=[16094,16169), tamper@16153 inside; needles all <16094 |
| 12 | Root of trust = embedded DSA/ECDSA public key in baja.jar | `[CERT]` (cited) | [B395]/[B482]; forging needs the private key |
| 13 | HostId = non-crypto fold-XOR of spoofable machine attrs (soft gate) | `[CERT]` (cited) | [B424] |
| 14 | Node-locked has NO post-boot re-watcher; check is load-time | `[CERT]` (cited) | [B481]/[B487] |
| 15 | Station process-exit on required-missing at boot | — | UNVERIFIED — child gap SP-G3a |

**Tally:** 9 `[CERT-live]`, 5 `[CERT]` (cited/peer, not re-derived), 1 `[INFER]` (explicit), 0 unmarked, 1 explicitly UNVERIFIED (named child gap SP-G3a). No claim without a marker.

## 6. Connections

- **Closes** the runtime-verifier half of **SP-G3** (was PARTIAL, B397). **Spawns SP-G3a** (station process-exit path).
- **Upgrades** [B397] offline `[CERT]` → `[CERT-live]`; complements [B323] (byte-compatible signer/verifier), [B482] (canonicalization), [B483] (signature anatomy), [B395] (the vendor-cert chain the signature roots into).
- **Pairs with** [B126 §126.6]: the native text-match gate vs this Java crypto gate = the licensing-integrity asymmetry, now half-proven live.
- **Reversibility lineage:** same protocol as [B318]/`RUNBOOK-REVERSIBILIDAD` (authorized-pentest reversibility).

## 7. Open gaps after this block

- **SP-G3a** (new, `requires-execution`): full throwaway-station boot with a *required* feature's license tampered — observe `System.exit(-3/-6)` vs degraded run.
- **SP-G9a** (`requires-execution`): live `Security.getProviders()` on the running station (next in this session).
- **SP-G6** (`requires-execution`): CRL/revocation enforcement for BACnet/SC + TLS.
- **SP-G8** (`requires-execution`): PanelBus/HMI OTA ECDSA-chain enforcement (needs OT edge hardware).
- **SP-G4 / SP-G9b** (`blocked`, requires-artifact): stock non-OEM install / `fips140-2`-licensed install.
