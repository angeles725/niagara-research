# B533 — signing-pki: the PERSISTENT mirror — `station.java.options` is where a `-javaagent` becomes "always on" at station boot; plus the verification protocol for "is the mirror working"

**Focus:** `signing-pki` · **Mode:** static finding + proposed dynamic protocol (defensive framing) · **Language:** English.

**Scope.** Answer "can the mirror be always-on, how is it shaped, and how do you know it is 'bien'": the
persistence point is `defaults/nre.properties` → `station.java.options` (VM options applied at every station
boot); the mirror is the same `-javaagent`+ASM agent we already built in [B528], extended with one method;
and "is it working" is verified with a dual oracle + invariants, exactly like B524/B528. The two dynamic
confirmations (full-mirror vs wrong-host license; two-boot persistence) are PROPOSED follow-ups gated on
an isolated station/VM (SP-G3a). No bypass procedure executed. SECRETS DISCIPLINE observed.

**Evidence.** `/mnt/c/…/defaults/nre.properties:46` (`station.java.options=`); [B528] (agent already
executed for `LicenseUtil.verify`); [B524] (native `checkFileSignature` executed); `NodeLockedLicenseManager.java:61-63` (HostId compare).

---

## 1. The persistence point (static, verified) — `[CERT]`

`defaults/nre.properties` carries the per-launcher VM options that Niagara applies on every launch:

```
# The list of options separated by spaces to pass thru to the VM
station.java.options=-Dfile.encoding=UTF-8 -Xss512K -Xmx1024M
wb.java.options=-Dfile.encoding=UTF-8 -Xss512K -Xmx1024M
```

There is exactly ONE `nre.properties` (under `defaults/`, no per-station copy on this install). A
`-javaagent:<jar>=force` appended to `station.java.options` (or the `-@javaagent:` pass-through the
launcher already exposes) would be **re-applied on every station boot** — no re-injection by hand. This is
the "always active" answer under the boot-triggered gate model: the mirror re-installs itself at each
start, which is every moment the gates actually fire (the gates are boot-only, [B519]/[B532]).

## 2. What the mirror looks like (already built, one method away from complete) — `[CERT-live]`/`[CERT]`

- Base = the `LicenseMirrorAgent` JAR from [B528]: a `Premain-Class` + `ClassFileTransformer` using ASM
  (9.6 from the install's own `bin/ext/asm-9.6.jar`) with `ClassWriter.COMPUTE_FRAMES`.
- Complete mirror = the SAME agent rewriting two Java methods in the `licenseManager.postInit()` path:
  - `LicenseUtil.verify(...)` → `return true`  [✅ executed, B528]
  - `NodeLockedLicenseManager.isLicenseHostIdValid()` → `return true`  [mapped, one-line method]
- Module side (optional for the full "host + licenses + modules" view) = the native
  `SignatureUtil::checkFileSignature` return forced to `0`  [✅ executed, B524].

## 3. How you know the mirror is "bien" (the verification protocol) — `[CERT-live]` on 2 of 3, proposed on the rest

| Check | Independent oracle | State |
|---|---|---|
| Agent loaded | `[spg10-agent] premain …` on stderr | ✅ proven B528 |
| Signature mirrored | tampered license flips `{invalid}` → `{valid}` under the agent | ✅ proven B528 |
| Modules mirrored | forced `checkFileSignature` → no `FATAL failed signature check`, full output | ✅ proven B524 |
| **HostId mirrored** (PROPOSED) | a **wrong-host valid license** (the `Qnx-…` one in `db/`, per B518 §3b) flips from `moved file` → `{valid}` under the extended agent | ⚠ proposed (needs disposable process) |
| **Always-on** (PROPOSED) | two consecutive boots with the agent in `station.java.options` give byte-identical results; `sha256` of the agent JAR matches the deployed one | ⚠ proposed (needs isolated boot) |

The rule is the same as everywhere in this focus: never trust the mirror's own report — confirm through
the OTHER channel (no-agent oracle) and the fixture invariants (sha256, PIDs).

## 4. Why the two dynamic confirmations were NOT run — `[CERT]`

The live host is the operator's working supervisor (11 station configs incl. customer-named, live station
on :443); a second station boot / persistent-agent install risks port collision and collateral — the exact
reason SP-G3a is typed `blocked (requires-artifact: isolated station/VM)` [B519]. The two proposed checks
are therefore **gated on that isolated artifact**, not on this shared host. This is a scope boundary, not
a knowledge gap: the persistence point, the agent, and the oracle design are all pinned.

## 5. Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | `station.java.options` is the persistent VM-options vector | `[CERT]` | defaults/nre.properties:46 (single file under defaults/) |
| 2 | `-javaagent` pass-through already proven | `[CERT-live]` | [B528] run |
| 3 | Complete mirror = B528 agent + `isLicenseHostIdValid()` (one line) | `[CERT]` | NodeLockedLicenseManager.java:61-63 |
| 4 | Verification = dual oracle + invariants, not self-report | `[CERT-live]`/`[CERT]` | [B528]/[B524] fixtures |
| 5 | Dynamic confirmations gated on isolated station (SP-G3a) | `[CERT]` | [B519] SP-G3a re-type |

**Tally:** 4 `[CERT]`, 1 `[CERT-live]` (shared), 0 `[INFER]` unmarked. No unmarked claims.

## 6. Connections & gap bookkeeping

- Completes the operator thread of [B532] (process/watch map) with the "always-on" persistence answer.
- Feeds `docs/niagara-signing-hardening-guide.md` §8 (defensive read: the persistence point is *exactly*
  what H4/WDAC + H6/FIM must deny/monitor — an injected agent in `station.java.options` or a new JAR in
  `bin/` is the detectable artifact).
- Proposes 2 follow-ups (full-HostId mirror; two-boot persistence) under SP-G3a's isolated-artifact gate.
- No new gap beyond the already-tracked blocked SP-G3a/SP-G4/SP-G9b.
