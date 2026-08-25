# B521 — signing-pki: module verification is PER-MODULE with an independent verdict each — dynamic confirmation (§12, live tool observation)

**Focus:** `signing-pki` · **Gap:** confirms the granularity claim of [B519]/[B520] §3 dynamically · **Mode:** dynamic §12, read-only (vendor tool, no injection, no mutation). **Language:** English.

**Scope.** Answer empirically: does the module "watcher" verify **one module or many** — atomically, or one-by-one? Observed by driving the vendor's own `nverify.exe` (read-only verification utility) against single and multiple targets on the operator's live host. This is the legitimate dynamic half of the module-integrity thread; the interposition **execution** PoC remains refused ([B520] §6) — only the vendor verifier is exercised here. SECRETS DISCIPLINE observed.

**Markers:** `[CERT-live]` observed running `nverify.exe`; `[CERT]` code/prior block; `[INFER]` reasoned. Evidence: `sources/probes/B521-module-verify-granularity-2026-08-25/nverify-multi.txt`.

---

## 1. `nverify` contract (its own usage) — one, many, or a whole install

`nverify` with no target prints: `Usage: nverify [options] <target> [<target> <target>]  —  Verify a Niagara install, dist, or jar file(s)`, with `--validate-all-signatures` ("any artifact with multiple signatures will have all signatures and certificate chains validated"). `[CERT-live]`
→ The audit tool is explicitly **variadic**: it accepts a single jar, several jars, or a whole install/dist. So "one vs many" is a *processing-granularity* question, answered next.

## 2. Per-module, independent verdict — one failure does not taint the rest

`nverify --validate-all-signatures A B C` over three module targets: `[CERT-live]`
```
INFO   Verifying archive  abstractMqttDriver-rt.jar      <- its own verify event + verdict
INFO   Verifying archive  weather-rt.jar                 <- its own verify event + verdict
SEVERE Error opening / Verification failed  opcUaCore-rt.jar  <- its own independent failure
```
- **Each target is verified independently** — one `Verifying archive` event per artifact, one verdict per artifact. There is **no single atomic all-modules pass**; the verifier **iterates**. `[CERT-live]`
- **Per-file isolation:** the third target's failure did **not** abort or invalidate the first two — they completed with their own verdicts. A bad/absent module is dropped on its own; it does not poison the set. `[CERT-live]` (mirrors the license per-file isolation of [B518] §3.)
- This empirically confirms the static claim of [B519]/[B520] §3: module verification is **per-JAR, event-driven**, not a batch gate.

## 3. §14 correction — the third target failed because it was ABSENT, not locked

Verification discipline: an earlier inference that `opcUaCore-rt.jar`/`baja-rt.jar` "Error opening" meant they were **locked by the live `station.exe`** (PID 18524) was **wrong**. Direct check: both files **do not exist** under those names in `modules/` (`abstractMqttDriver-rt.jar` 5.37 MB and `weather-rt.jar` 204 KB exist; the other two are MISSING). The failure was a **missing-file** error, which is exactly why it demonstrates per-target isolation cleanly. No lock claim survives. `[CERT-live]`

## 4. How the process runs (the module-verify path, consolidated)

Per artifact, on demand (add-time / class-load / audit invocation): `[CERT]`/`[CERT-live]`
1. Locate the artifact + its detached RSA-2048 `%s.sig` sidecar (or in-JAR signature). [B482]/[B520]
2. Native `DsfUtil::checkFileSignature` (dsfspi.dll, the single chokepoint [B520]) verifies the signature; Java `CertificateChainValidator` does PKIX path validation to a root in `truststore.jks` + the embedded TPK pin. [B482]/[B489]
3. Emit a **per-artifact verdict**; strictness set by `moduleVerificationMode` (=`low` live, [B519]). Required-verify fail → `System.exit(-6)` (whole-station). [B482]/[B478]
There is **no periodic re-scan** and **no atomic all-modules gate** — the process is inherently per-module and event-triggered.

## 5. Feasibility note — WSL2 boundary (what dynamic work is/ isn't reachable)

`[CERT-live]`/`[INFER]`
- **Reachable from WSL2 (done this session):** running the Windows `nverify.exe`/`nre.exe` via interop, reversible file ops on `/mnt/c`, `curl` to the live station, process/service inspection. All read-only dynamic analysis works.
- **NOT reachable from WSL2:** injecting the live **Windows** JVM with the **Linux** Frida build (cannot inject a Windows PE). Windows Frida is provisioned but runs Windows-side, and its use was refused by the harness independently ([B520] §6). So the interposition PoC is blocked by two independent reasons; none of the *analysis* is.

## 6. Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | nverify accepts one / many / whole-install targets | `[CERT-live]` | usage string; `--validate-all-signatures` |
| 2 | Each target gets an independent verify event + verdict (iterates) | `[CERT-live]` | 2 `Verifying archive` + 1 independent failure for 3 targets |
| 3 | One target's failure does not abort the others (per-file isolation) | `[CERT-live]` | A,B verified; C failed alone |
| 4 | No atomic all-modules gate; per-JAR event-driven | `[CERT]`/`[CERT-live]` | §2 + [B482]/[B519]/[B520] |
| 5 | Third target failed because ABSENT, not locked (§14 self-correction) | `[CERT-live]` | opcUaCore-rt.jar/baja-rt.jar MISSING on disk |
| 6 | Read-only dynamic analysis is fully doable from WSL2 | `[CERT-live]` | interop nverify/nre/curl this session |

**Tally:** 5 `[CERT-live]`, 1 mixed `[CERT]`/`[CERT-live]`, 0 unmarked. One prior inference explicitly corrected (§3).

## 7. Connections & open gaps

- **Confirms dynamically** [B519]/[B520] §3 (per-JAR granularity); **pairs with** [B518] §3 (license per-file isolation).
- No new gap. SP-G3a (isolated-VM boot) and SP-G10 (interposition runtime, refused) remain open as before.
