# B528 — signing-pki SP-G10a CLOSED: the license mirror, executed WITHOUT Frida — `nre -@javaagent` + ASM rewrites `LicenseUtil.verify` to `true` and the tampered license reports `{valid}`

**Focus:** `signing-pki` · **Gap:** SP-G10a (license-side mirror, runtime confirm) · **Mode:** §19 build/PoC + §12 live (disposable, reversible) · **Language:** English.

**Scope.** The runtime answer to "can a shim make the LICENSE verifier return valid", closing the half SP-G10 left open. The Frida Java bridge was unavailable (agent built gumjs-only, [B524]), but the launcher's own `-@<option>` pass-through opened the standard Java agent path: a 4-KB `-javaagent` built against the install's own `asm-9.6.jar` rewrites the six `com.tridium.sys.license.LicenseUtil.verify(...)` overloads to `return true` at class-definition time, and the `nre` oracle flips `{invalid}` → `{valid}`. SECRETS DISCIPLINE observed (HostId format only).

**Evidence.** `codegen/spg10-frida/javaagent/LicenseMirrorAgent.java` + `sources/probes/B528-spg10a-license-mirror-2026-08-26/` (RUN.md + agent source). Markers as §3.

---

## 1. Why we didn't need the Frida Java bridge after all — `[CERT-live]`

The launcher exposes a JVM-option pass-through: `nre.exe -@<option>` (help text "pass option to Java VM"). Under it:
- `-@verbose:class` shows every loaded class — confirmed `com.tridium.sys.license.LicenseUtil`,
  `LicenseFile`, `NLicenseManager` load from `modules/baja.jar` via
  `com.tridium.nre.bootstrap.BootstrapClassLoader`. (This already gave us "seeing Java" without Frida.)
- `-@javaagent:<jar>=<mode>` injects a standard JDK agent into the same JVM — the full
  `java.lang.instrument` API, no Frida involved.

## 2. The agent (reimplemented observer, METHODOLOGY §19) — `[CERT]`

`LicenseMirrorAgent.premain` registers a `ClassFileTransformer` that:
- (log mode) logs the `LicenseUtil` definition hook and passes bytes through untouched;
- (force mode) uses ASM 9.6 (`ClassWriter.COMPUTE_FRAMES`, required — without it the first attempt got
  `VerifyError: Expecting a stack map frame`) to replace each public static `boolean verify(...)` body
  with `ICONST_1; IRETURN` = `return true`.
- The six signatures rewritten: `verify([B[B[B)Z`, `([B[B[BLjava/lang/String;)Z`,
  `([B[BLjava/security/PublicKey;)Z`, `([B[BLjava/security/PublicKey;Ljava/lang/String;)Z`,
  `([B[BLjavax/baja/util/Version;Ljava/lang/String;)Z`, `([B[BLjavax/baja/util/Version;)Z`.

## 3. The oracle result (the mirror, both directions) — `[CERT-live]`

With `Webs.license` tamper-flipped in its signature base64 (sha256 `fc548614…` → `b376fc0b…`):

| Run | Verdict |
|---|---|
| `nre -licenses` (no agent) | `GRAVE … Webs.license {invalid: Invalid signature}` |
| `nre -@javaagent:…=force -licenses` | `Webs.license <Tridium> … {valid}` (all three licenses `{valid}`) |

The verifier's own consumer printed `{valid}` for the tampered license — the mirror exists for the
license half exactly as it does for the module half ([B524] F2). Reversibility: license restored
byte-identical (`sha256` == baseline), live PIDs unchanged (`niagarad` 21348, `station` 18524).

## 4. What this changes upstream — `[CERT]`/`[INFER]`

- **Retracts the blocked-on-tool verdict for SP-G10a**: the license mirror needs no Java-bridge agent —
  the launcher's `-@javaagent` path is sufficient. The wall was tool-choice, not a target capability gap.
- **Strengthens [B522] H5/H6**: the license gate is now proven bypassable in-process by a local
  privileged actor via a standard agent — same mitigation family as the module chokepoint (WDAC over
  `bin/`, FIM, and now: control who can attach agents / pass `-@javaagent`).

## 5. Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | `-@<option>` passes JVM options through | `[CERT-live]` | `-@verbose:class` dumped class loads this phase |
| 2 | LicenseUtil loads via BootstrapClassLoader from baja.jar | `[CERT-live]` | verbose-class transcript |
| 3 | ASM rewrite + COMPUTE_FRAMES fixes VerifyError | `[CERT-live]` | first run VerifyError vs second run clean boot |
| 4 | Tampered license flips `{invalid}` → `{valid}` under force | `[CERT-live]` | the two runs in §3, sha256 recorded |
| 5 | Restore byte-identical + PIDs unchanged | `[CERT-live]` | sha256 == baseline; tasklist PIDs |

**Tally:** 5 `[CERT-live]`, 0 `[CERT]`/`[INFER]` unmarked (static signatures cited, not re-derived).

## 6. Connections & gap bookkeeping

- **Closes SP-G10a** (and by transit the last open piece of SP-G10: both license and module mirrors now executed).
- Feeds [B522] H5/H6 with a second, agent-based interposition surface.
- Open items: SP-G9a, SP-G6, SP-G8 (+ blocked SP-G3a/SP-G4/SP-G9b).
