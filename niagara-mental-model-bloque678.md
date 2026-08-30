# B678 — JACE-8000 native launcher chain: `nre` → `libnjre.so` (`JavaLauncherQnx`: `loadDLL`→`dlopen lib/arm/client/libjvm.so`→`createVM`) → the station JVM, and `libnre.so` = the REAL `NativePlatformProvider` (daemonize/addUser/changePassword/watchdog — live, not the Windows stubs of [Block 385]); the launcher `-D` set reveals an **ATECC508 HSM engine** and **802.1X** support (focus jace8000-qnx-native, QN2; §19 [CERT])

> **Focus:** `jace8000-qnx-native` (§16). **Gap closed:** QN2 (how QNX boots and spawns the Niagara JVM;
> "cómo se manda a llamar a Niagara sobre QNX"). **Phase:** static RE, READ-ONLY. **Marker:** `[CERT]` from
> the ARM ELF symbol tables + string constants.
> **Sources:** `sources/probes/B672-jace8000-sd/qn2-launcher-symbols.txt` · binaries in
> `local-sd-image/bin-arm/` (`nre`, `libnjre.so`, `libnre.so`; gitignored, sha256 in the probe) ·
> `[CERT]` [Block 380] (Windows njre.dll), [Block 385] (Windows nre.dll native stubs), [Block 124] (Windows
> boot path), [Block 474] (live OpenJDK), [Block 676] (`hsm-2.0.1.jar`).
>
> **Bottom line:** the JACE-8000 starts Niagara the same shape as Windows ([Block 124]/[Block 380]) but on QNX:
> the **`nre`** binary (QNX interp `/usr/lib/ldqnx.so.2`) links `libnre.so`+`libdsfspi.so`+`libcommon.so` and
> drives **`libnjre.so`**, whose C++ `JavaLauncherQnx` `loadDLL()`s the HotSpot **`lib/arm/client/libjvm.so`**
> via `dlopen`/`dlsym`, `buildArgs()`/`buildVMOptions()`, then `createVM()`/`invokeJava()`. **`libnre.so`** is
> the **real** `NativePlatformProvider` (daemonize, user/password management, watchdog, diagnostics — live on
> QNX, unlike the return-0 stubs [Block 385] found on the Windows supervisor). The launcher's baked-in `-D`
> properties expose two hardware/security facts new to the corpus: an **ATECC508 secure-element HSM engine**
> and **IEEE 802.1X** supplicant support.

---

## §678.1 — The launcher chain `nre → libnjre.so → libjvm.so` `[CERT]`

`nre` is the launcher process — ELF32 ARM, QNX interpreter `/usr/lib/ldqnx.so.2`, `NEEDED`
`libdsfspi.so`, `libnre.so`, `libcommon.so`, `libc++.so.1`, `libsocket.so.3`, `libc.so.4` `[CERT]`. It drives
`libnjre.so`, whose C++ class **`JavaLauncherQnx`** exports the launch sequence `[CERT qn2-launcher-symbols.txt]`:

```
JavaLauncherQnx::initPaths()  → JavaLauncherQnx::loadDLL()  → dlopen/dlsym libjvm.so
JavaLauncherQnx::buildArgs()  → JavaLauncherQnx::buildVMOptions()
JavaLauncherQnx::createVM()   → JavaLauncherQnx::invokeJava()/java()  → cleanup()
   (+ launchJava, readVmArgs, jniExitHandler)
```
The JVM it loads is HotSpot's **client** VM: string `%s/lib/arm/client/libjvm.so` (and an
`aarch32` variant) `[CERT]`, resolved under `niagara.home`. `libnjre.so`'s only dynamic-loader imports are
`dlopen`/`dlsym` `[CERT]` — i.e. the JVM is loaded at runtime, exactly the `loadDLL`+`createVM` pattern
[Block 380] documented for the Windows `njre.dll`. This is the ARM/QNX answer to "how Niagara's JVM starts".
(The live VM was seen as OpenJDK in [Block 474]; here the on-disk launcher confirms the HotSpot client
`libjvm.so` load path.)

## §678.2 — `libnre.so` = the REAL `NativePlatformProvider` (live, not stubs) `[CERT]`

`libnre.so` (`NEEDED` `libcommon.so`+`libdsfspi.so`+`libc.so.4`) exports the `nre_platform_NativePlatformProvider*`
JNI surface `[CERT]` — the platform operations the daemon offers over :3011/:5011:
`daemonize0` (Tridium variant), `addUserAccount0`, `addUserToGroup0`, `changeUserPassword0`, `getAccountXml0`,
`createWatchdog0`/`destroyWatchdog0`, `enableSystemLogging0`, `canRead/WriteSystemLogMessages0`,
`executeNativeDiagnosticsCommand0`, `dumpThreads0`, `allowPlatformDaemonRestart0`,
`checkForKeyMaterialUpgrade0`, `getComputerName0`/`getComputerDomain0`, `getAllFileSystemNames0`, …

**§14 vs [Block 385]:** on the Windows supervisor, [Block 385] found `addUserAccount0`,
`getSystemPassword0`, `executeNativeDiagnosticsCommand0` were **return-0 STUBS** (real only on embedded
JACE/QNX). This ARM `libnre.so` is the **live implementation** of exactly that provider — confirming B385's
hypothesis on the real embedded target. `checkForKeyMaterialUpgrade0` ties to the keyring ([Block 466]/
[Block 677]); `daemonize0` is the platform daemon fork (sibling of `plat.exe`, [Block 381]).

## §678.3 — Two hardware/security facts from the launcher `-D` set `[CERT]` (new to the corpus)

The baked-in JVM properties in `libnjre.so` reveal platform capabilities `[CERT]`:
- **Hardware HSM / secure element:** `-Dniagara.hsm.engine=com.tridium.hsm.provider.qnx.Ecc508QnxRmEngine`
  + `-Dniagara.hsm.type=%s`. The JACE-8000 has a **Microchip ATECC508 CryptoAuthentication secure element**,
  driven by a QNX resource-manager engine (`Ecc508QnxRmEngine`) — the Java side is `hsm-2.0.1.jar` ([Block 676]).
  This is a **hardware root of trust / key-storage element** not previously in the corpus: at-rest key
  protection ([Block 466]/[Block 677]) can be anchored in the ECC508, not just the software keyring. `[CERT
  that the engine exists; INFER on how much key material actually lives in the ECC508 → QN2-G1 live/decompile.]`
- **IEEE 802.1X port authentication:** `-Dniagara.ieee8021x.supported=true` + `wpa_supplicant`/`wpa_cli`
  config paths + PKI cert dir + adapters `en0`/`dm0` — the JACE can do 802.1X-authenticated network access.
- Also: an embedded **`dhcpd`** provider (adapters `tiw_sap0`/`dm1`), `niagara.filestore.shared.memory=/dev/shmem`,
  and the daemon webserver thread-pool sizing (`min=5,max=25,selector=4,acceptor=1`).

## §678.4 — Self-verify

| # | Claim | Marker | Cite |
|---|---|---|---|
| 1 | nre = QNX launcher (interp ldqnx.so.2) linking libnre/libdsfspi/libcommon | [CERT] | readelf -d nre |
| 2 | libnjre.so JavaLauncherQnx: initPaths/loadDLL/buildArgs/buildVMOptions/createVM/invokeJava | [CERT] | nm -D libnjre.so |
| 3 | JVM loaded via dlopen `lib/arm/client/libjvm.so` (HotSpot client) | [CERT] | strings + dlopen/dlsym imports |
| 4 | libnre.so = live NativePlatformProvider (daemonize/addUser/changePassword/watchdog/diag) | [CERT] | nm -D libnre.so |
| 5 | §14: these are LIVE on QNX vs Windows return-0 stubs (B385) | [CERT] + [CERT] | §678.2; [Block 385] |
| 6 | ATECC508 HSM engine (Ecc508QnxRmEngine) + hsm-2.0.1.jar | [CERT] | -Dniagara.hsm.engine string; [Block 676] |
| 7 | IEEE 802.1X + dhcpd + shmem filestore from -D set | [CERT] | strings |

**Tally:** 7 claims — 7 [CERT] (one [INFER] on ECC508 key-material extent → QN2-G1). 0 unmarked.

## §678.5 — Connections

- **[Block 380]/[Block 124]** — the Windows `njre.dll` loadDLL/createVM + boot path; this is the QNX twin.
- **[Block 385]** — the Windows return-0 platform stubs; `libnre.so` is their live embedded implementation.
- **[Block 381]** — `plat.exe` daemon (Windows); `daemonize0` + [Block 679 QN3] `niagarad` are the QNX daemon.
- **[Block 677]** — `libdsfspi.so` crypto (linked by `nre`/`libnre.so`); `checkForKeyMaterialUpgrade0` pairs with it.
- **[Block 676]** — `hsm-2.0.1.jar` = the Java side of the ATECC508 engine named here.
- **[Block 474]** — the live OpenJDK VM; §678.1 is the on-disk HotSpot client load path.
