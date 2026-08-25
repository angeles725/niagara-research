# Block 485 — The native launcher license gates (`nre.dll`/`njre.dll` `createVM`): `-javaagent`/`-agentpath` is refuse-to-launch gated on the `developer` feature, FIPS is a `fips140-2`-gated boolean (not a VM option), and there is NO silent flag injection

> **Focus:** `licensing` — native launcher gates. **Native RE by sibling session `Segundo`
> (2026-08-24, radare2 + Ghidra, offsets identical across both methods); consolidated by `Primero`.**
> READ-ONLY: only static RE over the DLLs; no binary executed. Markers §3.
>
> **Sources:** `bin/nre.dll` (sha256 `606ff1c6…`), `bin/njre.dll` (sha256 `7007ff82…`). Confirms/extends
> [B319] (the 4.10.9.14 `-javaagent` gate) and [B380] (njre provider) on this 4.14 build; complements the
> already-corroborated `isFeaturePresent @0x180001f90` ([B126 §126.6]/native corroboration).

## §485.1 — `createVM` (nre @0x180006dc0 / njre @0x1800034d0) `[CERT corroborated]`

No static `jvm` import: `loadDLL` does `LoadLibraryA("…\bin\server\jvm.dll" | client)` + `GetProcAddress
("JNI_CreateJavaVM")` (proc global nre `0x18001a480`). Builds `JavaVMInitArgs` inline: `version=0x10008`
(JNI_1_8), `nOptions=global(0x18001a498)`, `options=global JavaVMOption[](0x18001a478)`, `ignoreUnrecognized=0`;
calls via `__guard_dispatch_icall @0x180006f0e`. `buildArgs` emits `-Dniagara.home`, `-Djava.class.path`,
`-Djava.security.properties==…\bin\policy\java.security`, `-Djava.security.manager`, `-Dniagara.platform.provider`;
`buildVMOptions` parses the `nre/station/wb/test.java.options` props.

## §485.2 — `-javaagent`/`-agentpath`/`-agentlib` = refuse-to-launch on the `developer` feature `[CERT corroborated, both DLLs]`

`createVM` walks the VM options and `strstr` for `"javaagent"` (@0x180006e67) / `"agentpath"` / `"agentlib"`; on
match → `LicenseUtil::isFeaturePresent("Tridium","developer")` (→ `0x180001f90`, call @0x180006ebe). **If the
feature is ABSENT:** prints `"FATAL: Can not use Java agent argument '%s' without a '%s' feature in '%s' license."`
(@0x180006f54) and **`return -1` → the VM is NOT created.** Identical gate in `njre` @0x1800035ce (FATAL
@0x18000366f). So a `-javaagent` on a station/WB without a `developer` license aborts the launch — it is a
refuse-to-launch gate, not a silent strip.

## §485.3 — FIPS is a `fips140-2`-gated boolean, not a VM option `[CERT corroborated]`

There is **no `-Dfips` VM option**. `initFips` (ONLY in `nre.dll` @0x1800071b0) is gated by
`isFeaturePresent("Tridium","fips140-2")` (@0x1800071e7); without the license it is skipped. It reads argv
`-fips` / `-fips=true|false` or parses `etc\options\bajaui-FipsOptions.options` (`startWorkbenchInFipsMode`) and
sets ONLY a bool `[obj+0x10130]=1/0`. The BouncyCastle-FIPS dir `%s\bin\ext\bcfips` is fixed in `initPaths`, not
as a VM option. (Corroborates the Java-side `Nre.verifyFipsLicense` gate, [B481 §481.6].)

## §485.4 — Roles `[CERT]`

- **`njre.dll`** = base launcher (`JavaLauncher/Win32`, entry `java()`) — has the `-javaagent` gate, NOT FIPS.
- **`nre.dll`** = Niagara superset (`NreLauncher`, entry `nre()`) — adds `initFips`, `defaultToNonFIPS`, and the
  Niagara `-D` args.

## §485.5 — Net finding `[CERT]`

The suspicion of **silent `-javaagent`/`-Dfips` injection is DISPROVEN with evidence.** What actually exists at
the `JNI_CreateJavaVM` edge is TWO refuse-to-launch **license** gates: (a) `developer` feature over
`-javaagent`/`-agentpath`/`-agentlib` (both launchers), (b) `fips140-2` feature over a FIPS-mode boolean (nre
only). Everything else in `createVM` is ordinary option assembly from properties.

## §485.6 — Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|---|---|---|---|
| 1 | createVM loads jvm.dll dynamically + JNI_CreateJavaVM; JavaVMInitArgs JNI_1_8; buildArgs -D set | `[CERT+corrob]` | nre @0x180006dc0 / njre @0x1800034d0 | PASS |
| 2 | -javaagent/-agentpath/-agentlib → isFeaturePresent("Tridium","developer"); absent → FATAL + return -1 (VM not created), both DLLs | `[CERT+corrob]` | nre @0x180006e67-0f54; njre @0x1800035ce | PASS |
| 3 | No -Dfips option; initFips (nre only) gated by fips140-2 → sets a bool; bcfips dir in initPaths | `[CERT+corrob]` | nre @0x1800071b0-071e7 | PASS |
| 4 | njre = base launcher (no FIPS); nre = Niagara superset (+initFips +Niagara -D) | `[CERT]` | entries java()/nre() | PASS |
| 5 | No silent flag injection — two refuse-to-launch license gates only | `[CERT]` | §485.2-485.3 | PASS |

**Tally:** 5 claims, all `[CERT]` (4 explicitly r2+Ghidra corroborated). Native RE credit: sibling session `Segundo`.

## §485.7 — Connections & open gaps

- Confirms [B319] (`-javaagent` developer gate) on the 4.14 build and extends it with the FIPS boolean gate;
  complements [B481 §481.6] (Java fips140-2 gate) and [B484] (native crypto core).
- **B485-G1** the `nverify.exe` X.509 chain + trust anchor (→ answers **B482-G1**: `truststore.jks` vs
  `cacerts`+TPK) — Segundo's `nverify` worker in-flight → block B486.
