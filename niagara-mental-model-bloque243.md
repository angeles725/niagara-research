# Bloque 243 — U2 firmware supply-chain: `honFirmwarePackage` (code-signed firmware delivery vehicle) + `honeywellVersionManager` (`BHonVersion` tuple)

> Empirical coverage of the OEM Honeywell **firmware supply-chain** pair (coverage-audit gap U2,
> `audits/2026-07-12-coverage-audit.md`): `honFirmwarePackage` (how controller firmware is packaged +
> shipped) and `honeywellVersionManager` (how versions are modelled/compared). Both are **THIN** by measured
> pre-flight (§13 e2) — this is a proven-absence-heavy coverage closure, not a code sweep:
> `honFirmwarePackage` has **0 Java classes** (`fase1-recon: class_count=0, tiene_codigo_java=false`) — it is a
> RESOURCE bundle; `honeywellVersionManager` is **exactly 1 class**.
>
> **Focus**: `oem-honeywell-tail`, gap U2 (HIGH). Second block of the focus (after B242).
>
> **Sources**: `organized/honFirmwarePackage/honFirmwarePackage-rt/extracted/**` (module.xml, MANIFEST.MF,
> SERVER1.{SF,RSA}, the firmware payloads under `res/`) and
> `organized/honeywellVersionManager/honeywellVersionManager-rt/vineflower/com/honeywell/versionmanager/BHonVersion.java`.
>
> **Method**: read inline (small gap — no delegated sweep). Firmware payload sizes + magic bytes, jar-signing
> manifest/signature, and the code-signing certificate chain observed DIRECTLY by me. `[CERT]` = observed by me
> at the cited path; `[INFER]` = deduction. This is a DISTRIBUTED artifact (a shipped signed module), NOT a
> live-install — citing firmware STRUCTURE (sizes, magic, signer identity, public cert chain) is in scope; no
> private key material exists in a signed jar.
>
> Capa 22 (OEM). **Conecta fuerte**: [Bloque 94] (Device Manager OTA — the delivery mechanism), [Bloque 242]
> (`honIrmConfig` firmware-download instance numbers — the receiving side), [Bloque 75] + [Bloque 113]
> (module-signing arc — this is it in practice), [Bloque 90] (`honPlantController` / PanelBus — the firmware
> targets).

---

## 243.1 — Scope: two thin modules, one supply-chain story `[CERT]`

The coverage audit paired these as "firmware packaging + version mgmt". The measured pre-flight (§13 e2)
re-scoped U2 sharply: neither is a code subsystem.

- `honFirmwarePackage-rt`: **0 classes**, `tiene_codigo_java=false` (`fase1-recon.json`); `module.xml` `<types/>`
  is EMPTY — it is a payload container, not code.
- `honeywellVersionManager-rt`: **1 class** (`BHonVersion.java`).

So U2 closes as: firmware "packaging" is a **signed resource bundle** (not a subsystem), and "version mgmt" is
a **single value-struct** (not a manager service). The value of this block is the SUPPLY-CHAIN evidence — what
ships, how it is signed, who signs it — plus how it ties to the OTA (B94) and receiving (B242) sides.

---

## 243.2 — `honFirmwarePackage`: a firmware DELIVERY vehicle `[CERT]`

`honFirmwarePackage-rt.jar` is a Niagara module whose ENTIRE purpose is to carry controller firmware binaries
as module resources. `module.xml` (`extracted/META-INF/module.xml`):
`vendor="Honeywell" vendorVersion="4.14.0.0.1.56"`, `preferredSymbol="hfp"`, `installable="true"`,
`autoload="true"`, and a self-describing `description="SnapOnIO FW v0.5.0.28 and HMI FW v1.5.4.26"`. Only
dependency is `baja` (Tridium 4.14); `<types/>` is empty (no code). `buildMillis=1727161370907`
(2024-09-24), `buildHost="azu-hce-vbf-w25"` (an Azure Honeywell build agent).

The payload — 3 firmware images under `extracted/res/` `[CERT]`:

| Payload | Size (bytes) | Leading magic (hex) | Target |
|---|---|---|---|
| `res/HMIFirmware/HMI_FW_v1.5.4.26.frm` | 1,714,824 | `d4b8eb1d 00000000 00000000 00000000` | controller HMI display firmware |
| `res/panelbusFirmware/Pb_fw.bin` | 124,048 | `20000000 06032000 ffffffff ffffffff` | PanelBus IO firmware |
| `res/panelbusFirmware/Pb_fw_Snapon.bin` | 382,323 | `20000000 01032000 ffffffff ffffffff` | PanelBus SnapOn IO firmware |

`[INFER]` The two PanelBus images share an identical 4-byte header prefix (`20000000`) and a near-identical
second word differing only in one byte (`06032000` vs `01032000`) — a variant/board-id discriminator between
the standard PanelBus and the "SnapOn" IO variant, consistent with the module description naming "SnapOnIO FW".
The `.frm` HMI image uses a different container magic (`d4b8eb1d`). Bundle also carries the usual
`module.palette` + `-rt.lexicon` (no code).

---

## 243.3 — Code-signing: the B75/B113 signing arc in practice `[CERT]`

The module is a **signed jar** — the module-signing hardening ([Bloque 113]) observed on a real Honeywell
artifact, and the payload integrity guarantee for firmware delivery.

- `extracted/META-INF/MANIFEST.MF`: `Sealed: true`, `Implementation-Vendor: Honeywell`,
  `Implementation-Version: 4.14.0.0.1.56`, and a **per-entry `SHA-256-Digest`** for every resource — INCLUDING
  the firmware payloads (`Name: res/panelbusFirmware/Pb_fw_Snapon.bin` + its digest). So each firmware image is
  integrity-bound by the manifest.
- `extracted/META-INF/SERVER1.SF`: `Signature-Version: 1.0`, `SHA-256-Digest-Manifest`, `Created-By: 1.8.0_241
  (Oracle Corporation)` — standard JAR signature file over the manifest.
- `extracted/META-INF/SERVER1.RSA` — the PKCS#7 signature block. Certificate chain (observed via
  `openssl pkcs7 -print_certs`) `[CERT]`:
  1. **`CN=Honeywell International Inc., O=Honeywell International Inc., L=Morris Plains, ST=New Jersey, C=US`** — the leaf SIGNER.
  2. `CN=DigiCert Trusted G4 Code Signing RSA4096 SHA384 2021 CA1` — issuing CA.
  3. `CN=DigiCert Trusted Root G4` — root.

`[INFER]` This is a genuine commercial **code-signing** identity (DigiCert G4 Code Signing, RSA-4096/SHA-384),
not a self-signed or platform default — so firmware shipped in this module is authenticated to Honeywell
International Inc. at the jar layer. The trust decision at install time is Niagara's module-signing verification
([Bloque 113]); the on-controller firmware flashing then rides the OTA path ([Bloque 94]) into the
firmware-file instance numbers `honIrmConfig` exposes (`FIRMWARE_FILE_INST_NUM`/`BLE_`/`PERIPHERAL_`,
[Bloque 242] §242.2).

---

## 243.4 — `honeywellVersionManager`: `BHonVersion`, a 6-tuple version struct `[CERT]`

The whole module is one class: `com.honeywell.versionmanager.BHonVersion extends BStruct` (final). It is a
value-type modelling a **6-component version** as a dotted string, default `"0.0.0.0.0.0"`:

- `@NiagaraProperty value` (String, default `"0.0.0.0.0.0"`, `flags=1` = read-only/hidden) —
  `BHonVersion.java:16-24`.
- Positional layout `[CERT]` (`BHonVersion.java`): `NIAGARA_MAJOR/MINOR/PATCH = 0/1/2`, `TOOL_MAJOR/MINOR/PATCH
  = 3/4/5` — i.e. the string encodes BOTH the Niagara framework version (first 3) and the Honeywell TOOL
  version (last 3).
- `niagaraVersion()` / `toolVersion()` split the string into `int[3]` slices; `compareNiagaraVersion()` /
  `compareToolVersion()` wrap them in `javax.baja.util.Version` and delegate `compareTo`. On any parse failure
  both fall back to `NULL.niagaraVersion()` (the `"0.0.0.0.0.0"` singleton) — **exception-swallowing**, so a
  malformed version string silently compares as zero rather than throwing.

`[INFER]` This is a shared helper for gating features/firmware by BOTH the platform (Niagara) and the Honeywell
tool version simultaneously — the natural consumer is the model-feature/version-compatibility logic seen in
`honIrmConfig` ([Bloque 242] §242.2, `hardwareCompatibility`/`functionBlockVersion`) and the OTA path. The
swallow-to-zero fallback is a low-severity robustness smell (a bad version string downgrades to "oldest",
which could mis-gate an upgrade).

---

## 243.5 — Conexiones

- **[Bloque 94]** (Device Manager OTA): the DELIVERY mechanism that pushes these firmware images to controllers;
  `honFirmwarePackage` is the packaged payload it distributes.
- **[Bloque 242]** (`honIrmConfig`): the RECEIVING side — `BIrmBacnetDevice`'s `FIRMWARE_FILE_INST_NUM`
  (65536), `BLE_FIRMWARE_FILE_INST_NUM` (262144), `PERIPHERAL_FIRMWARE_FILE_INST_NUM` (327680) are the BACnet
  file objects these images are written into on the controller.
- **[Bloque 75] + [Bloque 113]** (module-signing arc): this block is that arc on a real artifact — a
  DigiCert-G4 code-signed, SHA-256-manifest-sealed module whose signed entries include the firmware binaries.
- **[Bloque 90]** (`honPlantController` / PanelBus): the `Pb_fw*.bin` PanelBus IO firmware targets the same
  PanelBus stack B90 distilled (BTP/PanelBus, EagleHawk→PanelBus migrators).

---

## 243.6 — Self-verify

- **All claims observed by me directly** (`[CERT]`, no delegated sweep): firmware sizes + magic bytes (`stat` +
  `xxd`), module.xml identity, MANIFEST/SF signing structure, the DigiCert-G4 → Honeywell code-signing chain
  (`openssl pkcs7`), and `BHonVersion.java` source. `[INFER]` = the 4 deductions (PanelBus variant discriminator,
  commercial-signing conclusion, BHonVersion consumer, swallow-to-zero smell).
- **Block TYPE**: EVIDENCE (thin-module, proven-absence-heavy). Low `[INFER]`/`[CERT]` ratio; the modules ARE
  small (0 code + 1 class) — U2 is now fully covered as a supply-chain pair, not left with hidden depth.
- **New gaps queued**: none net-new from U2 (both modules exhausted). Next per RESEARCH-STATE-oem-honeywell-tail:
  U3 `honAlarmConsole`+`honAlarmExt` (HIGH), or U1b/U1c to finish `honIrmConfig`.
