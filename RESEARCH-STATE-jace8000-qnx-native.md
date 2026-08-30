# RESEARCH-STATE — focus: jace8000-qnx-native (bootstrapped 2026-08-30)

> Multi-focus corpus (METHODOLOGY §16). Focus **BOOTSTRAPEADO 2026-08-30** a pedido del operador
> ("qué más se puede documentar del JACE-8000: herramientas, seguridad, protocolos, cómo se llama a QNX").
> Es el **gemelo QNX/ARM del focus `platform-native`** (que hizo los binarios de WINDOWS, B124-130/B379-385/
> B424-425). Fuente: los binarios ARM extraídos READ-ONLY del microSD del JACE-8000 a `local-sd-image/bin-arm/`
> (gitignored; imagen cruda en `local-sd-image/`, focus `jace8000-sd` B672-676).
>
> **Profile (AUDIT-FIRST):** 13 ELF32 ARM little-endian, **CON tabla de símbolos** (no stripped, a diferencia
> del lado Windows): libdsfspi.so (2115 FUNC), libcommon.so (1885), libnre.so (783), libnjre.so (444),
> libserial.so (167), libplatccn.so (146), station (134), libpower.so (128), libbacnet.so (110), niagarad
> (111), libplatmstp.so (116), libplatnrio.so (106), nre (81). Símbolos → RE mucho más tractable.
>
> **Ángulo:** cómo el JACE arranca y ejecuta Niagara sobre QNX (launcher nativo), la cripto de reposo en ARM
> (libdsfspi), los drivers de field-bus nativos (protocolos), el daemon de plataforma, el OS QNX (IFS/boot), y
> un veredicto de seguridad consolidado. READ-ONLY estático (Ghidra 12.1.3 + r2). REMITTANCE: el lado Windows
> = platform-native; protocolos wire genéricos = protocols/modbus/bacnet focuses; acceso vivo = jace8000.

<!-- research-state.v1 -->
schema: research-state.v1
block_scope: shared-global
covered_blocks: 678
gaps_closed: 6
known_gaps: 9
investigable_open: 2
requires_execution_open: 0
blocked_open: 1
deferred_open: 0
undocumented_findings: 0
<!-- /research-state.v1 -->

focus: jace8000-qnx-native
status: active
bootstrapped_on: 2026-08-30
block_prefix: niagara-mental-model-bloqueN.md (numeración global; próximo libre: B677)

## Coverage

- **Covered blocks**: 673 corpus-wide (this focus: B677) (shared-global)
- **Coverage metric**: 6 / 8 investigable closed
- **Last iteration**: 2026-08-30 — QN6 closed (B682, QNX invocation chain; payload encrypted → IFS unpack blocked QN6-G1)

## Gap-backlog (prioritized)

| Priority | Gap | Type | Status |
|---|---|---|---|
| high | QN1 libdsfspi.so — ARM crypto SPI: keyring/machine-key, config.bog + .km/.kr en/decrypt at rest (sibling of B425 dsfspi.dll) | decompiled-arm | closed (B677 — Mocana NanoCrypto static, AES-256-CBC, NIST CTR-DRBG) |
| high | QN2 nre + libnre.so + libnjre.so — native launcher chain on QNX: how the JVM/station is spawned (sibling of B124/B380) | decompiled-arm | closed (B678 — JavaLauncherQnx dlopen libjvm.so; libnre=live NativePlatformProvider; ATECC508 HSM + 802.1X) |
| high | QN3 niagarad — the platform daemon binary on QNX (:3011/:5011) (sibling of plat.exe B381) | decompiled-arm | closed (B679 — thin JVM launcher of NiagaraDaemon; drops privileges to niagarad user, refuses root; §14 vs B381 LocalSystem) |
| medium | QN4 field-bus native drivers — libplatmstp (BACnet MS/TP), libplatnrio (Niagara Remote IO), libplatccn, libserial | decompiled-arm | closed (B680 — 4 *PlatformServiceQnx JNI drivers: MS/TP, NRIO, CCN/dev-ccn, serial) |
| medium | QN5 libcommon.so + libbacnet.so — common native runtime + BACnet native | decompiled-arm | closed (B681 — EngineWatchdog+NetCfgIo+OpenSSL libcrypto 2nd stack; libbacnet=BACnet/Ethernet /dev/bn) |
| medium | QN6 QNX-OS boot / IFS — invocation chain + Neutrino version | binary-unpack | closed (B682 — chain ROM→MLO→u-boot→go→CertISW; QNX 7.0.0; payload ENCRYPTED, IFS not extractable offline) |
| medium | QN6-G1 QNX IFS contents (procnto banner, startup script, driver list) | live-device or device-bound key | blocked (payload encrypted; needs live serial or ECC508 key) |
| low | QN7 libpower.so + station binary — power/watchdog + station launcher specifics | decompiled-arm | pending |
| high | QN8 SECURITY VERDICT — consolidate JACE-8000 posture (B460/461/466/468/672/674/676 + ARM crypto) | synthesis | pending |

## Remittance (no son gaps — ya cubiertos)

- Windows native platform (nre.exe/njre.dll/nre.dll/plat.exe/dsfspi.dll) → focus `platform-native`.
- Live station/platform access, Fox client, config.bog pull → focus `jace8000` (B459-475).
- The SD filesystem, factory image, signing chain → focus `jace8000-sd` (B672-676).
- Generic wire protocols (Fox/BACnet/Modbus/LON) → protocols/modbus/bacnet focuses.

## Iteration history

| # | Date | Gap closed | Block | Delegated? · model tier | New gaps uncovered |
|---|---|---|---|---|---|
| — | 2026-08-30 | (bootstrap — AUDIT-FIRST profile of 13 ARM ELFs) | — | no · inline (readelf/tool gate) | 8 seeded |
| 1 | 2026-08-30 | QN1 libdsfspi.so crypto | B677 | no · inline (symbol/string inventory readelf/nm) | QN1-G1 (HW-RNG runtime), QN1-G2 (KDF params) |
| 2 | 2026-08-30 | QN2 launcher chain | B678 | no · inline (readelf/nm/strings) | QN2-G1 (ECC508 key-material extent) |
| 3 | 2026-08-30 | QN3 niagarad daemon | B679 | no · inline (readelf/nm/strings) | 0 |
| 4 | 2026-08-30 | QN4 field-bus drivers | B680 | no · inline (readelf/nm/strings) | QN4-G1 (CCN=Carrier wire) |
| 5 | 2026-08-30 | QN5 libcommon+libbacnet | B681 | no · inline (readelf/nm/strings) | QN5-G1 (OpenSSL version/TLS routing) |
| 6 | 2026-08-30 | QN6 QNX invocation + payload opacity | B682 | no · inline (entropy + binwalk) | QN6-G1 (IFS contents, blocked-encrypted) |

## Blocked gaps (each tagged with what it needs)

- QN6-G1 — needs: the live JACE (serial `uname -a`/`pidin`/`/proc/boot`) or the device-bound payload key. tried: static entropy (~7.998) + binwalk (0 sigs) on the SD payload → encrypted/opaque, no offline unpack.

## Stop control (primary = read-only-investigable exhaustion, METHODOLOGY §8)

- **Open gaps — read-only investigable**: 2 (QN7, QN8) → loop runs
- **Open gaps — requires-execution**: 0
- **Open gaps — blocked**: 1 (QN6-G1 IFS contents, encrypted)
- Budget cap: none (operator: open + continue automatically)

## Dismissed file types

- none (binary RE focus; source = the extracted ARM ELFs).
