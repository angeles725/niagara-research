# Block 380 — The rest of the njre JVM launcher, decompiled (Ghidra grade): `java()` orchestrator, `initPaths` classpath/JVM-selection, dynamic `loadDLL`, a hardcoded `-Xmx48M`, and the FIPS-gated Bouncy-Castle provider swap

> **Focus `platform-native` — Ghidra sub-pass NG2 (re-scoped).** B125 §125.2 already decompiled the three
> middle launcher functions `buildArgs` / `createVM` / `invokeJava` — so those are **REMITTANCE to [B125]**,
> NOT re-derived here. This block decompiles the FOUR launcher functions B125 did NOT open:
> `java()` (the orchestrator), `initPaths()` (path + classpath + JVM-DLL selection), `loadDLL()` (dynamic
> `JNI_CreateJavaVM` resolution — which grounds B125 §125.2's *inference* as `[CERT]`), and
> `buildVMOptions()` (the hardcoded default heap) — plus the three OS/license gates the launcher enforces
> before the JVM ever starts. This is the "documentation we missed" on the native launch path. READ-ONLY.
>
> Sources (primary): `/mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162/bin/njre.dll`
> (PE32+ x86-64, image base `0x180000000`, assembly identity `Tridium.Niagara.NJreLib 4.14.0.22`
> "Niagara JRE Wrapper"; sha256 in evidence). Ghidra recovered the `JavaLauncherWin32::*` symbol names
> from RTTI, so functions are cited by real name + entry.
> Method: Ghidra 12.1.2 `analyzeHeadless` + `tools/ghidra-scripts/DecompileByString.java` (string-anchored,
> introduced in [B379]); `rabin2 -z/-i` for string/import confirmation.
> Raw evidence: `audits/B380-njre-decomp-bystring.txt` (6 decompiled functions), `audits/B380-njre-ghidra.txt`.
> Markers: `[CERT]` observed in the decompiled body / binary (name@entry or `audits/…:line`) · `[INFER]` deduction.
>
> Native platform layer (Capa 25). Connects [Block 125] (completes its §125.2 launcher trace — buildArgs/
> createVM/invokeJava are B125's; this adds the other four functions), [Block 124] (boot path), [Block 126]
> (`LicenseUtil::isFeaturePresent` — the same native license gate reused here for FIPS + developer features).

---

## 380.1 — `java()` — the launch orchestrator + two pre-JVM gates `[CERT]`

`JavaLauncherWin32::java` (`?java@…@@UEAAHPEBDHPEAPEAD_N@Z` @ `0x180004470`,
`audits/B380-njre-decomp-bystring.txt:13-121`) is the top-level entry B124/B125 named but never decompiled.
It runs two gates, then a fail-fast pipeline:

- **Windows-version gate** (`:46-58`): builds an `OSVERSIONINFOEXW` for **major=10** and calls
  `VerifyVersionInfoW(..., 0x23, mask)` (mask over MAJOR/MINOR/SP-major via `VerSetConditionMask`); on
  failure → `"FATAL: This version of Windows is not supported by Niagara 4."` The native launcher
  hard-requires **Windows 10 / Server 2016+** before it will start the JVM. `[CERT]` (`rabin2 -i` confirms
  `VerifyVersionInfoW`+`VerSetConditionMask` imports).
- **Production-build banner** (`:70-82`): `SignatureUtil::isProductionBuild()` — if it returns false, prints
  `"**** DEVELOPER BUILD FOR INTERNAL TRIDIUM USE ONLY ****"`. So the same binary can be a dev build; the
  check is a signature-derived flag, not a license. `[CERT]`
- **Debug tracing**: the `java_debug` env var (`_dupenv_s`, `:60-68`) flips `this+8`, enabling all the
  `java> …` trace lines used throughout (that is where the anchor strings come from). `[CERT]`
- **Fail-fast pipeline** (`:87-113`): `initPaths → buildArgs → loadDLL → createVM → invokeJava → cleanup`,
  each guarded `if (-1 < ret)`. Any negative return aborts the chain. `buildArgs`/`createVM`/`invokeJava` are
  [B125 §125.2]; the other three are §380.2-380.4 below. `[CERT]`

---

## 380.2 — `initPaths()` — classpath, JVM-DLL selection, and the FIPS-gated BC provider `[CERT]`

`JavaLauncherWin32::initPaths` (`?initPaths@…@@AEAAHXZ` @ `0x180003ad0`, `:134-317`) assembles every path the
VM needs. Facts `[CERT]`:

- **JRE home**: honors the **`NIAGARA_JRE_HOME`** env var (`_dupenv_s`, `:188`); absent → `<niagaraHome>\jre`
  (`:194-195`). All roots (`niagaraHome`, `niagaraUserHome`, supported/required runtime profiles) come from
  `Nre::getInstance()` through the CFG dispatch thunk (`PTR__guard_dispatch_icall_180009398`); `niagaraUserHome`
  is `FileUtil::verifyPath`-checked.
- **Two JVM DLLs**: `jvmDll_0 = <jreHome>\bin\server\jvm.dll`, `jvmDll_1 = <jreHome>\bin\client\jvm.dll`
  (`:199-201`) — server first, client fallback (the fallback is exercised in `loadDLL`, §380.3).
- **Classpath build** (`:202-245`): merges `<niagaraHome>\bin` and `<jreHome>\bin` into the existing process
  `PATH`, **de-duplicating** by `strstr` (only prepends a dir if not already present, `:221-241`), into a
  **32000-byte** buffer; then sets the process `PATH` via `_putenv("path=%s")` (`:288`).
- **FIPS-gated Bouncy-Castle provider** (`:250-256`) — the striking finding:
  ```c
  bVar16 = LicenseUtil::isFeaturePresent("Tridium","fips140-2");
  pcVar5 = "%s\\bin\\ext\\bcstd";           // feature PRESENT  -> standard BC
  if (!bVar16) pcVar5 = "%s\\bin\\ext\\bcfips"; // feature ABSENT  -> BC-FIPS
  ```
  The mapping is inverted from the naive reading: the `fips140-2` license feature selects the **standard**
  Bouncy Castle dir (`bcstd`), and its ABSENCE selects the **FIPS-certified** provider (`bcfips`). `[CERT]`
  on the code; `[INFER]` on intent: the sane reading is that **`bcfips` is the default/forced provider and
  the `fips140-2` feature is a license to OPT OUT to the standard (non-FIPS) BC** — i.e. FIPS is on unless
  licensed off, not off unless licensed on. Either way, the crypto-provider directory on the classpath is
  license-controlled at launch. (Ties to [B126]'s `isFeaturePresent` and [B114]'s crypto.)
- **ext-jar append + overflow guard** (`:257-274`): `FUN_180003730` appends every jar under
  `<niagaraHome>\bin\ext` (and the chosen `bc*` dir) to the classpath; on buffer exhaustion →
  `"FATAL: Failed to append all items in extPath to classpath, insufficient buffer size"` + **`exit(0xf9)`
  (249)**. A trailing `;` is stripped (`:280-282`). `[CERT]`

So the launcher — not the JVM — decides the classpath, the crypto provider, and the `java.library.path`,
all before `JNI_CreateJavaVM`. `[CERT]`

---

## 380.3 — `loadDLL()` — dynamic `JNI_CreateJavaVM` resolution (grounds B125's inference) `[CERT]`

`JavaLauncherWin32::loadDLL` (`?loadDLL@…@@AEAAHXZ` @ `0x180004740`, `:623-701`):
`SetCurrentDirectoryA(<jreHome>\bin)` → `LoadLibraryA(jvmDll_0 server)`; on NULL, `LoadLibraryA(jvmDll_1
client)` (`:668-672`); on both NULL → `"Error: Cannot load: %s or %s"`. Then
**`DAT_18000f398 = GetProcAddress(hModule, "JNI_CreateJavaVM")`** (`:687`); NULL → `"Error: Cannot find JNI
functions"`; finally restores the original cwd. `[CERT]`

This **confirms as `[CERT]` what [B125 §125.2] could only mark `[INFER]`**: `JNI_CreateJavaVM` is resolved
by `GetProcAddress` from the dynamically-`LoadLibraryA`'d `jvm.dll` — which is exactly why it never appears
in `njre.dll`'s import table, and why `createVM` calls it indirectly through the stored pointer
`DAT_18000f398` via the CFG dispatch thunk. The server/client fallback is real, not assumed. `[CERT]`

---

## 380.4 — `buildVMOptions()` — the hardcoded 48 MB heap `[CERT]`

`JavaLauncherWin32::buildVMOptions` (`?buildVMOptions@…@@AEAAHPEAUJavaVMOption@@PEAHH@Z` @ `0x180004b00`,
`:793-809`) is tiny and blunt: it `_strdup`s **`-Xmx48M`** and **`-Xms48M`** straight into the
`JavaVMOption[]` array and bumps the count. So the native launcher bakes a **48 MB fixed heap** (min=max)
into the VM options before any config-derived arg. `[CERT]` (48 MB is a floor the platform layer sets; a
station's real heap is set elsewhere in the boot/config path — this is the native default, [INFER] that it
is later overridden.)

---

## 380.5 — `createVM()` agent-injection gate, decompiled in full (extends B125) `[CERT]`

[B125 §125.7] token-6 *named* the developer-license agent gate; here it is decompiled
(`createVM` @ `0x1800034d0`, `:751-768`). For every VM option, if the option string contains
**`javaagent`**, **`agentpath`**, or **`agentlib`** AND `LicenseUtil::isFeaturePresent("Tridium","developer")`
is false → `"FATAL: Can not use Java agent argument '%s' without a 'developer' feature in 'Tridium'
license."` and abort. `[CERT]`

This is a genuine security control: **`-javaagent`/`-agentpath`/`-agentlib` JVM instrumentation is blocked on
a production station** (any station whose Tridium license lacks the `developer` feature), preventing an
operator from injecting a Java agent into the station JVM at launch. It is a `strstr` (substring) test, so it
matches the option anywhere. The `JavaVMInitArgs.version = 0x10008` (JNI_1_8) and the CFG-dispatched
`JNI_CreateJavaVM` call are [B125 §125.2] — REMITTANCE. `[CERT]`

---

## 380.6 — Defensive-security summary `[CERT]`

1. **Agent-injection is license-gated** `[CERT]` (§380.5): `-javaagent/-agentpath/-agentlib` require the
   `developer` license feature — a real anti-instrumentation control on production stations.
2. **Crypto provider is license-selected at launch** `[CERT]` (§380.2): the Bouncy-Castle dir on the
   classpath (`bcfips` vs `bcstd`) is chosen by the `fips140-2` feature; the default (unlicensed) is the
   FIPS provider `bcfips` — good posture, but the mapping is counterintuitive and worth stating explicitly.
3. **OS floor enforced natively** `[CERT]` (§380.1): Windows 10/Server 2016+ required before JVM start.
4. **Dynamic JVM binding** `[CERT]` (§380.3): `JNI_CreateJavaVM` via `GetProcAddress`, server→client
   fallback — confirms B125's inference; no static JNI import to hook.
5. **Hardcoded 48 MB launcher heap** `[CERT]` (§380.4) — a native default, presumably overridden by config.

No secrets read; `njre.dll` is a distributable binary. Analysis on an isolated copy; nothing mutated.

---

## 380.7 — Self-verify

**Token re-checks** (load-bearing `[CERT]` re-confirmed):
1. `java()` Windows gate: `VerifyVersionInfoW` + major=10 + "not supported by Niagara 4" — ✓ (`decomp:46-58`, `rabin2 -i`, string @ `0x180009a90`).
2. `SignatureUtil::isProductionBuild()` → "DEVELOPER BUILD FOR INTERNAL TRIDIUM USE ONLY" @ `0x180009b78` — ✓ (`decomp:70-82`).
3. Pipeline `initPaths→buildArgs→loadDLL→createVM→invokeJava→cleanup`, each `-1 < ret` — ✓ (`decomp:87-113`).
4. `NIAGARA_JRE_HOME` env override else `<niagaraHome>\jre` — ✓ (`decomp:188-195`, string @ `0x180009c78`).
5. `jvmDll_0=…\bin\server\jvm.dll`, `jvmDll_1=…\bin\client\jvm.dll` — ✓ (`decomp:199-201`).
6. FIPS gate `isFeaturePresent("Tridium","fips140-2")` → `bcstd` present / `bcfips` absent — ✓ (`decomp:250-256`, strings @ `0x180009d18`/`0x180009d30`).
7. ext-append overflow → "insufficient buffer size" + `exit(0xf9)` — ✓ (`decomp:266-274`, string @ `0x180009d50`).
8. `loadDLL`: `LoadLibraryA(server)`→fallback `LoadLibraryA(client)`→`GetProcAddress("JNI_CreateJavaVM")`→`DAT_18000f398` — ✓ (`decomp:668-687`).
9. `buildVMOptions`: `_strdup("-Xmx48M")` + `_strdup("-Xms48M")` — ✓ (`decomp:802-807`, strings @ `0x18000a5a8`/`0x18000a5b0`).
10. `createVM` agent gate: `strstr javaagent/agentpath/agentlib` + `!isFeaturePresent("Tridium","developer")` → FATAL — ✓ (`decomp:751-768`, string @ `0x18000a340`).

**10/10 load-bearing tokens re-verified.**

**Marker tally**: `[CERT]` ≈ 22 · `[INFER]` 3 (the FIPS-mapping intent, the 48 MB later-override, the
production-flag source). Ratio `[INFER]/[CERT]` ≈ 0.14 — low; EVIDENCE block over decompiled bodies.
**Block type: EVIDENCE** (decompilation). This does NOT re-derive B125 — the overlap (`buildArgs`/`createVM`/
`invokeJava`) is explicitly remitted; the four decompiled functions here are new.

---

## 380.x — Connections

- **[Block 125]** — *completes its §125.2 launcher trace.* B125 decompiled `buildArgs`/`createVM`/`invokeJava`
  + the JNI natives; B380 adds the orchestrator `java()`, `initPaths`, `loadDLL`, `buildVMOptions`, and
  upgrades B125's "JNI_CreateJavaVM resolved by GetProcAddress" from `[INFER]` to `[CERT]` (§380.3). The
  developer-agent gate B125 tokenized is decompiled here (§380.5).
- **[Block 126]** — the same `LicenseUtil::isFeaturePresent` native gate B126 decompiled is reused at launch
  for the `fips140-2` (provider) and `developer` (agent) features.
- **[Block 124]** — the boot path that reaches `JavaLauncherWin32::java`.
- **[Block 114]** — the FIPS/standard BC provider selected here is the crypto layer B114 covers.
- **Forward — reopened focus**: NG3 (`plat.exe` DPAPI `systempw` + `CreateServiceA` decompiled), NG4
  (`libciper.so` QNX-ARM bodies), NG1-G1 (nverify `skip-*` gate sites). NG2 core closed; `nre.dll`
  NativePlatformProvider native BODIES (beyond B125's 3 samples) remain a possible NG2-follow.
