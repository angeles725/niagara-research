# B534 — signing-pki: the HostId mirror EXECUTED — the "moved file" gate is NATIVE, so a Java `-javaagent` rewrite of `isLicenseHostIdValid()` does NOT unseat a wrong-host license (honest negative; the signature-only mirror still works)

**Focus:** `signing-pki` · **Mode:** §19 build/PoC + §12 live (disposable, reversible) · **Language:** English.

**Scope.** Execute the proposed "full mirror" (license signature + HostId) from [B533] on a wrong-host
license. Result: the signature rewrite works exactly as [B528]; the HostId rewrite compiles, loads, and
rewrites the right method — but the wrong-host license **still** gets relocated (`moved file`), proving the
HostId "moved file" decision is made by a NATIVE `sys.license` layer OUTSIDE the Java method we can
rewrite. This is a precise, useful correction to the "one Java method = HostId gate" model in [B532] §5.
SECRETS DISCIPLINE observed.

**Evidence.** `codegen/spg10-frida/javaagent/FullMirrorAgent.java`; run transcripts preserved in
`sources/probes/` (see RUN note). Markers per §3.

---

## 1. What was executed — `[CERT-live]`

Built `FullMirrorAgent` (reimpl of the [B528] agent extended with a second transformer) that, in `force`
mode, rewrites via ASM (COMPUTE_FRAMES):
- all six `LicenseUtil.verify(...)` → `return true`  (same as [B528]);
- `NodeLockedLicenseManager$NodeLockedLicense.isLicenseHostIdValid()` → `return true`.

Placed a **wrong-host but validly-signed** `Honeywell.license` (the real `Qnx-TITAN-44A2-…` copy from
`security/licenses/db/`) as the active license on the `Win-6E6E-…` host, then ran the `nre -licenses`
oracle with and without the agent.

## 2. Result — the signature mirror works; the HostId mirror does NOT — `[CERT-live]`

| Run | License listing | Features |
|---|---|---|
| baseline (no agent, wrong-host license) | `Honeywell.license` ABSENT (moved) | no `honeywell:*` |
| `-@javaagent:full-mirror.jar=force` | `Honeywell.license` **still ABSENT** | no `honeywell:*` |

The agent logged both rewrites (`verify ×6`, `isLicenseHostIdValid()Z`, class bytes 1825→1846), so it
loaded and transformed the correct method — yet the wrong-host license was still relocated. Root cause:
`LicenseFile.load()` (`LicenseFile.java:100-108`) does call `isLicenseHostIdValid()` in Java, but the
**"moved file" relocation happens in a NATIVE `sys.license` layer** (`common.dll`/`nre.dll` — the string
`moved file` has no Java source in the corpus, consistent with [B518]'s "the runtime's `sys.license`
layer relocates"). That native relocation runs independently of the Java boolean we rewrote.

## 3. The correction — `[CERT]`/`[INFER]`

[B532] §5 presented `isLicenseHostIdValid()` as "the one-line HostId gate" and therefore a copy-paste
mirror point. **This over-simplifies:** the Java method gates the *feature-withholding* path, but the
**file relocation ('moved file')** is a native, earlier decision. Mirroring the HostId fully would need
the **native** side (`getHostId()` fold inputs — [B424] — or the native `sys.license` relocation), not
just the Java boolean. The Java rewrite is necessary-but-not-sufficient.

## 4. What still held (no regression) — `[CERT-live]`

- Signature mirror: re-confirmed working (the `verify` rewrites loaded; [B528]'s `{invalid}→{valid}`
  flip on a tampered license is unchanged — that gate IS fully Java).
- Reversibility: `Honeywell.license` restored byte-identical (`sha256` == baseline `4a799453…`);
  live PIDs unchanged (`niagarad` 21348, `station` 18524).

## 5. Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | FullMirrorAgent loaded and rewrote both targets | `[CERT-live]` | agent log lines (`verify ×6`, `isLicenseHostIdValid()Z`) |
| 2 | Wrong-host license still moved under the full mirror | `[CERT-live]` | license listing absent + no honeywell:* features |
| 3 | `moved file` is a native string (no Java source) | `[CERT]` | grep 'moved file' over organized/*.java = 0 hits |
| 4 | Restore byte-identical + PIDs unchanged | `[CERT-live]` | sha256 == baseline; tasklist PIDs |

**Tally:** 3 `[CERT-live]`, 1 `[CERT]`, 1 `[INFER]` (native-layer location is reasoned from absence +
B518). No unmarked claims.

## 6. Connections & gap bookkeeping

- **§14-CORRECTS [B532] §5** (HostId is not a one-Java-method gate; the native `sys.license` relocation
  is the earlier, harder path).
- Refines [B533]'s "complete mirror = B528 + isLicenseHostIdValid()" to: **complete mirror additionally
  needs the native HostId fold/relocation, not just the Java method**.
- Feeds [B424] (the native fold inputs are the real surface for the HostId), and the hardening read:
  the HostId gate is stronger than the signature gate against a Java-only shim.
- Open items unchanged: only blocked-on-artifact SP-G3a / SP-G4 / SP-G9b; the two B533 dynamic follow-ups
  are now HALF-done (HostId executed → negative; two-boot persistence not run — still gated on isolate).
