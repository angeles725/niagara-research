# Bloque 242 — `honIrmConfig` deep: the IRM Nano / BEATS config-runtime spine (manager · Nano command protocol · FB factory · OEM brand · crypto) — delta over B88

> Empirical deep-dive of the OEM Honeywell module **`honIrmConfig`** — the config/runtime layer of the
> **BEATS / IRM Nano** programmable unitary controller (PUC). This is a DEPTH block over [Bloque 88], which
> covered `honIrmConfig`'s config-tool root + Nano-over-BACnet protocol at `[CERT-a]` depth while sharing a
> block with `honeywellSylkDevice`. This block goes deep on the **481-class `-rt` runtime**: the manager
> spine, the Nano command protocol wire format, the function-block factory architecture, the OEM/multi-brand
> licensing model, and — new — the module's **own crypto layer** (with a hardcoded-key weakness).
>
> **Focus**: `oem-honeywell-tail` (coverage-audit gap U1, `audits/2026-07-12-coverage-audit.md`). First block
> of the focus. It also completes the IRM/BEATS triad: [Bloque 88] (config tool + Sylk model),
> [Bloque 105] (`honIrmControl`, the control FB library), and this block (the config-runtime spine those two
> rest on).
>
> **Sources**: `organized/honIrmConfig/honIrmConfig-rt/vineflower/com/honeywell/irmnano/**` (481 distinct
> classes, measured; `-ux` 9, `-wb` 51, `-doc` help — deferred to follow-up blocks). Cross-universe relations
> via `module-navigator` over the full 926-submodule / 51,299-`.java` organized/ index. Framework base-class
> contract via **docSource** original Tridium source (rule a): `docSource-doc/extracted/bacnet-rt/javax/baja/bacnet/BBacnetDevice.java`.
>
> **Method**: 2 read-only sweeps (source-reconstruction + module-nav/docSource relational axis) + **direct
> token-verification by me** of the 6 load-bearing claims. `[CERT]` = re-verified by me against the cited
> `file:line`; `[CERT-a]` = sweep citation (module-nav output, secondary file:line) not re-verified; `[INFER]`
> = deduction. **ZKM caveat**: `honIrmConfig` is Zelix-KlassMaster obfuscated (`class` lookups report
> `ZKM: YES` on the spine classes), so string/constant static findings are lower-confidence and possibly
> non-exhaustive — where obfuscation limits confidence it is flagged inline.
>
> Capa 22 (OEM deofuscados). **Conecta fuerte**: [Bloque 88] (la otra mitad: config tool + Sylk),
> [Bloque 105] (`honIrmControl` — cuelga de esta capa), [Bloque 94] (Device Manager OTA), [Bloque 101]
> (`airFlowBalancer`), [Bloque 115] (`spyderToIrmNxMigrator` — migra hacia este modelo), [Bloque 75] (seguridad).

---

## 242.1 — Scope & delta over B88 `[CERT]`

[Bloque 88] established the config-tool ROOT — `BIrmBacnetDevice extends BBacnetDevice implements BIHonBacnetDevice`
(verified verbatim at `honIrmConfig-rt/.../manager/BIrmBacnetDevice.java:591`) — and the fact that the transport
is the **Nano protocol over BACnet**. It did so at `[CERT-a]` depth, sharing one block with `honeywellSylkDevice`,
so roughly half a block for a **481-class** module.

This block distils what B88 did not reach: the **three-role manager tier**, the **Nano command wire format**
(framing, opcode enum, response codes), the **extension-point FB factory**, the **model-feature capability
matrix**, the **7-brand OEM licensing**, the module's **own AES/PBKDF2 crypto layer**, and the **inherited
`BBacnetDevice` framework contract** (COV/polling/lifecycle) it does NOT reimplement. It also carries two §14
refinements (transport nuance to B88; call-direction precision to B105) — see §242.4 and §242.5.

> Note (coverage-audit accuracy): U1 was listed as an uncovered gap (grep mention 0–1), but `honIrmConfig`
> was in fact partially covered by B88. This block treats U1 as a DEPTH gap over B88, not virgin coverage —
> recorded as a §18 retro observation on the coverage audit.

---

## 242.2 — The manager spine: three tier-manager roles, not one `[CERT]` / `[CERT-a]`

`honIrmConfig`'s orchestration is split across three `manager`/`commiss` classes, each a distinct Niagara type:

- **`BIrmBacnetDevice extends BBacnetDevice implements BIHonBacnetDevice`** `[CERT]`
  (`manager/BIrmBacnetDevice.java:591`, 4,358 lines, ZKM). The BACnet **device shadow** — the network identity
  of a physical BEATS controller. It bakes firmware-download as a first-class concern: fields
  `FIRMWARE_FILE_INST_NUM = 65536`, `BLE_FIRMWARE_FILE_INST_NUM = 262144`,
  `PERIPHERAL_FIRMWARE_FILE_INST_NUM = 327680`, `RETRY_COUNT = 10`, and device-swap state
  (`isSwapActive`/`isAutoSwappedIn`) `[CERT-a]` (`manager/BIrmBacnetDevice.java:682-705`). It is a terminal
  leaf (no subclasses) `[CERT-a]` (module-nav `hierarchy`).
- **`BIrmControlManager extends BComponent`** `[CERT-a]` (`manager/BIrmControlManager.java:606`; Niagara type
  `IrmControlManager`). The **program orchestrator**: a large `@NiagaraProperties` block (`:269-570`) exposing
  `deviceModelName`, `applicationType`, `functionBlockId/Family/Version`, `numberOf{Folders,FunctionBlocks,Links}`,
  `memoryUsage`, `controllerHardwareFeatures`, `teachingMode`, `onlineDebugEnabled`, `isSynchronized`/
  `synchronizationStatus`, `lastCommissioned`; and `@NiagaraActions` (`:575-604`)
  `readFileDirectory`/`readFile`/`writeFile`/`resetCounters`/`countResources`/`addToSubscription`/`removeFromSubscription`.
- **`BIrmConfig extends BComponent implements BIHonBacnetConfig, IPrivateTransferClient`** `[CERT-a]`
  (`commiss/BIrmConfig.java:367`; Niagara type `IrmConfig`). The **commissioning/config service**;
  `IPrivateTransferClient` ties vendor commands to BACnet **Confirmed Private Transfer**.

The 94-class `manager` package additionally holds the subscription/notification machinery
(`SubscriptionManager`), commissioning jobs (`NanoJobManager`, `BIrmBatchOperationJob`), the
backup/restore structs (`BNanoFBBackupDetails`), the wire encoders (`BeatsDeviceDataEncoder`/`Decoder`),
and error/diagnostic state (`ControllerErrorHandler`, `NanoError`) `[CERT-a]`.

---

## 242.3 — The Nano command protocol (wire format) `[CERT]` / `[CERT-a]`

The vendor protocol between station and controller is a compact binary command protocol carried inside the
transport (§242.4). Its Java model:

- **Frame header** — `protocol/NanoCmd.java:13-17` `[CERT]`: 4-index header
  (`CMD_VERSION=0`, `CMD_ID=1`, `TRANSACTION_ID=2`, `RESP_CODE=3`, `COMMAND_HEADER_LENGTH=4`).
- **Opcodes** — `protocol/NanoCmdIds.java` `[CERT-a]`: an ordinal-encoded enum where enum position = wire
  opcode (`NULL, ECHO, GET_CHILDREN, GET_CHILDREN_DETAILS, GET_VALUES, GET_INFO, … CREATE_CHILD,
  SET_PROPERTIES, DELETE_CHILD, FLUSH_PARENT, SET_LINK, WRITE_FILE, SET_ENVIRONMENT,
  SET_START_STOP_DDC_CTRL_LOOP, … SET_CONTROLLER_PASSWORD`), with explicit `RESERVED_0X1C..RESERVED_0X40`
  placeholders confirming positional opcode assignment.
- **Response codes** — `protocol/NanoResponseCodes.java` `[CERT-a]`: `DDC_OK`, `DDC_ERROR`, CRC/storage/memory
  errors, and password errors `DDC_ERR_PASSWORD`/`DDC_ERR_PASSWORD1`.
- **Transport abstraction** — `network/BINanoProtocolService.java:1-26` `[CERT-a]`: the interface every
  transport implements — `runCommand(NanoCmd, INanoResponse, int)`, `runCommandWait(NanoCmd)`,
  `installCallback(INanoDeviceStatus)`, `reInitializePollService()`. This is the single seam through which all
  Nano commands flow, regardless of the underlying wire.

---

## 242.4 — Transport: BACnet is production; §14 refinement of B88 `[CERT]`

**B88 said "Nano over BACnet". That is correct as the PRODUCTION transport, and this block confirms it — with
a refinement, not a refutation (§14 scope-clarification).**

`BINanoProtocolService` (§242.3) has exactly **two** implementations in `com.honeywell.irmnano.network`:

1. **`BBacnetProtocolService`** (1,147 lines) `[CERT-a]` — the production transport, carries `NanoCmd` frames
   over BACnet (via `BIrmConfig`'s `IPrivateTransferClient`, §242.2). This is B88's "Nano over BACnet".
2. **`BSimulationProtocolService`** (374 lines) `[CERT]` — a **real raw-TCP** implementation for dev/simulation:
   `@NiagaraProperty portNumber` defaults to **47616** (`network/BSimulationProtocolService.java:54`), `hostAddress`
   defaults to `"localhost"` (`:53`), and it constructs `new Socket(hostAddress, portNumber)` (`:244` `[CERT-a]`),
   shipping the identical `NanoCmd` protocol over a plain TCP socket. It is the **only** raw socket in the entire
   `-rt` module (`grep 'new (Socket|ServerSocket|DatagramSocket)'` → one hit, `BSimulationProtocolService`) `[CERT]`.

**`protocol.ip` (28 classes) and `protocol.ble` (4 classes) are NOT alternate wire transports** `[CERT-a]`. They
are `NanoCmd`-payload **configuration surfaces**: `BIPConfiguration`, `BWIFIConfiguration`, `BRSTPConfiguration`,
`BEthernetConfiguration`, `BNetworkTimeServerConfiguration` (all `extends BComponent`, not `BINanoProtocolService`)
let the BACnet-carried Nano protocol remotely configure the **device's own** onboard IP/WiFi/RSTP/NTP and
BLE-commissioning-radio settings. The `ble` package (`BBLEConfiguration`, `BBLEPasscodeValidity`, read/write jobs)
carries **no radio API** — zero `BluetoothGatt`/`BluetoothAdapter` references; all "Bluetooth"/"BLE" hits are
Niagara property getters and firmware-status enum plumbing `[CERT-a]`. The BLE stack itself lives below the Java
runtime.

> **§14 refinement of B88** `[INFER]` from the above: the accurate statement is "BACnet is the sole production
> wire transport; IP/WiFi/BLE are configuration TARGETS the Nano protocol writes into the device, not
> transports; and a genuine non-BACnet TCP transport exists but only in the simulation implementation
> (`:47616`)." B88 is not wrong — it named the production path — but the IP/BLE packages must not be read as
> parallel transports.

---

## 242.5 — The function-block factory: extension-point architecture `[CERT]`

`honIrmConfig` is where the IRM Nano function-block ENGINE lives (B105 established `BNanoFunctionBlock` is
here). This block distils HOW the factory assembles the FB library — and it is **not a static switch**:

- **`fbfactory/NanoFbFactory.java:112`** `[CERT]`:
  `registry.getTypes(registry.getType("honIrmConfig:INanoFactory"))` — the factory scans the Niagara **type
  registry** for every module that registers the `honIrmConfig:INanoFactory` extension point, instantiates each
  (`:119`), and builds a family-id → factory map (`fbLibraries`, `:71`). Dispatch is `createNanoFunctionBlock(int
  familyId, int fbTypeId)` by matching `getFamilyId()` `[CERT-a]`.
- **`fbfactory/BINanoFactory`** `[CERT-a]` — the extension-point interface (`getFamilyId`, `getFamilyName`,
  `createNanoFunctionBlock(int)`, `getFunctionBlockVersion`).
- **`fbfactory/BNanoFunctionBlock extends BComponent implements BINanoControl, BIIRMSupportedComponent`**
  `[CERT-a]` (2,534 lines) — the base class every function block extends.

**Refinement of B105 (call-direction precision)** `[CERT-a]`: module-nav `module-calls honIrmControl-rt
honIrmConfig-rt` = **474 call edges** (top callees: `BNanoFunctionBlock.started` ×71, `.descendantsStarted` ×25,
`.changed` ×22, `.executeHoneywellComponent` ×15, `.getConfigPropertiesList` ×10). The reverse
(`honIrmConfig-rt` → `honIrmControl-rt`) = **0 edges**. So although the two modules declare a MUTUAL Niagara
module dependency (manifest level), the **runtime call direction is strictly one-way**: `honIrmControl`'s ~163
concrete FBs (VAV/light/arithmetic/Sylk under `com.honeywell.irm.*`) plug up into `honIrmConfig`'s engine
(`com.honeywell.irmnano.*`) as `BINanoFactory` implementations; the engine never calls the catalog. The
namespace split (`irm.*` legacy vs `irmnano.*` Nano-era) mirrors this layering `[INFER]`.

---

## 242.6 — Model features + OEM multi-brand licensing `[CERT]` / `[CERT-a]`

- **Per-model capability matrix** — `modelfeature/BModelFeaturesEnum extends BFrozenEnum` `[CERT-a]`
  (`:174`, ordinals `:175+`): `MEASUREMENT_TYPE=0`, `CUSTOM_SENSOR=4`, `BLE_CONFIGURATION=30`,
  `APPLICATION_FILE_ENCRYPTION=36`, `WIFI_CERTIFICATE_SUPPORT=44`, `PERIPHERAL_FW_DOWNLOAD=45`,
  `DEVICE_CONFIG_DATA_SUPPORT=48`, `MODBUS_ENHANCEMENTS=49`. Supported models enumerated in
  `modelfeature/BBEATSSupportedModels implements BISupportedModels`.
- **OEM brand gate** — `brand/BrandHandler.java` `[CERT]`: `getFeature("Tridium", "brand")` (`:24`) →
  `feature.get("brandId")` (`:26`), constants `FEATURE_BRAND="brand"` (`:16`), `KEY_BRANDID="brandId"` (`:17`).
  Brand identity is read from the **Niagara license file** (`javax.baja.license.LicenseManager`/`Feature`).
- **7 rebadged OEM brands** — the `brand` package ships one device-model table per OEM behind interface
  `IBEATSDeviceModels` `[CERT-a]`: `AlertonDeviceModels`, `CentralineDeviceModels`, `HBSDeviceModels`,
  `SBCDeviceModels`, `SMBDeviceModels`, `TrendDeviceModels`, `WEBSDeviceModels`. Example
  `brand/TrendDeviceModels.java` maps model number → part number (`70 → "RS0844ES24NM"`, `120 → "VA75T24NM"`).
  The single BEATS controller platform is licensed/rebadged for **at least 7 third-party BMS brands** (Alerton,
  Centraline, HBS, SBC, SMB, Trend, WEBS).

---

## 242.7 — Commissioning · balancer · custom sensor · diagnostics `[CERT-a]`

- **Commissioning handshake** — `commiss/BServicePinDevice` (`:52`), `BDiscoveryDeviceEx`,
  `BNanoOfflineDiscoveryJob`, `NanoServicePinMessage`, `IrmYouAreMessage` — a service-pin / discovery /
  "You-Are" pairing flow for binding a physical controller.
- **Airflow balancer (19)** — `balancer/BBalancingObject extends BVector` (`:194`) is the core balancing data
  object; `BLearnBalancingDataJob` / `BTeachBalancingDataJob` drive the learn/teach balancing workflow against
  the controller (ties to [Bloque 101] `airFlowBalancer`).
- **Custom sensor (5)** — `customsensor/BCustomSensorConfig`, `BCustomSensorTypeEnum` — user-defined analog
  sensor curve/type config, separate from the built-in sensor library.
- **Controller diagnostics (4)** — `controllerdiagnostics/BIrmControllerDiagnostics extends BComponent` (`:56`);
  `BDiagnosticFileReadJob` reads diagnostic logs off the controller.
- **Application-file lifecycle** — `applicationfile/ApplicationFileDownloadHandler` +
  `BApplicationFileDownloadStatusEnum` / `…FailureCauseEnum` (ties to `APPLICATION_FILE_ENCRYPTION` feature bit).

---

## 242.8 — Inherited framework contract: `BBacnetDevice` (docSource, rule a) `[CERT-a]`

`BIrmBacnetDevice` does NOT reimplement device I/O — it inherits it. From the ORIGINAL Tridium source
`docSource-doc/extracted/bacnet-rt/javax/baja/bacnet/BBacnetDevice.java` (2,962 lines):
`public class BBacnetDevice extends BLoadableDevice implements BacnetConst, BIBacnetPollable,
BIBacnetObjectContainer, DeviceOverrideAware, LatencyRecorder…` (`:271-279`). So the effective chain is
`BIrmBacnetDevice → BBacnetDevice → BLoadableDevice`.

Inherited framework contract (all in `BBacnetDevice`, not IRM-specific): the device-ext model
(`points`/`alarms`/`schedules`/`trendLogs`/`config`), **COV subscription** machinery
(`useCov`, `maxCovSubscriptions`, `subscribeCov(BBacnetProxyExt)` `:2249`, `subscribeCovProperty` `:2321`),
**polling/ping** (`pollFrequency`, `poll()` `:792`, `doPing()` `:1554`, `addPolledPoint`), the component
lifecycle (`started()` `:1097`, `descendantsStarted()` `:1222`, `changed()` `:1370`), device-identity
resolution (`updateDeviceInfo` `:1464`, `lookupBacnetObject` `:2645`, `isObjectTypeSupported`/`isServiceSupported`
`:967-994`), and cross-cutting `DeviceOverrideAware`/`LatencyRecorder` instrumentation. `[INFER]` The 4,358
IRM-specific lines layer BLE/IP/BACnet-Nano-command orchestration ON TOP of this base, rather than
reimplementing COV/polling — a clean use of the BACnet framework as substrate.

Sibling context `[CERT-a]` (module-nav `hierarchy BBacnetDevice`): `BBacnetDevice` has 8 direct Honeywell
children — `BAscotBacnetDevice`, `BHMIDevice`, `BHonBacnetSmartSensorDevice`, `BHonBacnetWallModuleDevice`
(+TR100 variants), `BThermostatBacnetDevice`, `BIrmBacnetDevice`, … — so the "wrap a Honeywell device as a
`BBacnetDevice` subclass" recipe is a repeated pattern across the OEM stack, not unique to IRM.

---

## 242.9 — Security posture (crypto layer) `[CERT]` / `[CERT-a]`

`honIrmConfig` owns its own crypto layer in `com.honeywell.irmnano.network` to secure the Nano channel. It
carries a **sound primary path AND a weak fallback** side by side:

- **🔴 Hardcoded fallback AES key** `[CERT]` — `network/AesSymmetricCryptographer.java:114`:
  `byte[] key = new byte[]{105, 114, 109, 110, 52, 101, 110, 99, 114, 121, 112, 116, 105, 111, 110, 49}`,
  wrapped `new SecretKeySpec(key, "AES")` (`:123`). The literal decodes to ASCII **`"irmn4encryption1"`**
  (verified). `getKey(serialNo)` XORs this fixed 16-byte string with the device serial number; `getTheCipherText`
  falls back to it whenever no password-derived key is supplied. Since the base key is baked into the shipped
  module and the serial number is discoverable (device label / BACnet device-instance / discovery response),
  this is **obfuscation, not key secrecy**, for any commissioning flow that hits the fallback path. (Vuln
  research on a distributed decompiled artifact — same class of finding as the SEC-1/SEC-3 items in the
  2026-07-12 certainty audit; not a live credential.)
- **🟠 Key material logged at FINEST** `[CERT]` — `AesSymmetricCryptographer.java:68`:
  `…ENCRYPTION_COMPRESSION_LOG.finest("Encryption Key " + TextUtil.bytesToHexString(key.getEncoded()))`. Raw AES
  key bytes are hex-dumped to the log if FINEST logging is ever enabled in the field.
- **🟢 Sound primary path** `[CERT]` — the same class exposes `getSecureNanoSecret(BPassword)` (`:103`) deriving
  the key from a password, and `network/SecureNanoKeyStore` uses **`PBKDF2WithHmacSHA256`, 10,000 iterations,
  128-bit** key `[CERT-a]`. So the weak fallback COEXISTS with a legitimate KDF — the risk is the fallback, not
  the design intent.
- **Auth surface** `[CERT-a]` — `BIrmConfig implements IPrivateTransferClient`: BACnet Confirmed Private
  Transfer has no protocol-level auth beyond what the app layer adds; the Nano password state machine
  (`manager/BIrmCtrlSecStateEnum`, `BPasswordStatusEnum`, `NanoResponseCodes.DDC_ERR_PASSWORD`, opcode
  `SET_CONTROLLER_PASSWORD`) is the only gate. Whether EVERY opcode enforces a password/session check, or only
  a subset, is a follow-up trace (queued gap).
- **Minor** `[CERT-a]`: `System.out` hex-dump leak (non-secret) at `manager/BIrmProgram.java:3848-3851`.
- **ZKM caveat** `[CERT-a]`: spine classes are Zelix-obfuscated; `module-nav security-audit` returned 0 files
  (index/name gap, NOT a clean bill). Treat the crypto findings above as verified-where-cited but the overall
  security surface as **not exhaustively scanned**.

---

## 242.10 — Conexiones

- **[Bloque 88]** (`honIrmConfig` config-tool + `honeywellSylkDevice`): the shallow root this block deepens. B88
  named `BIrmBacnetDevice` and "Nano over BACnet"; this block adds the manager tier, wire format, factory,
  brand, and crypto — and refines the transport statement (§242.4).
- **[Bloque 105]** (`honIrmControl`): the ~163 concrete function blocks that plug INTO this module's
  `BINanoFactory`/`BNanoFunctionBlock` engine. §242.5 confirms the runtime call direction is one-way
  (`honIrmControl → honIrmConfig`, 474 edges; 0 reverse) — a precision add over B105's layering statement.
- **[Bloque 94]** (Device Manager OTA): the firmware-download instance numbers in `BIrmBacnetDevice`
  (`FIRMWARE_FILE_INST_NUM`, `BLE_/PERIPHERAL_`) tie the config layer to the OTA/firmware arc.
- **[Bloque 101]** (`airFlowBalancer`): the `balancer` package's learn/teach jobs are the IRM-side of airflow
  balancing.
- **[Bloque 115]** (`spyderToIrmNxMigrator`): migrates Spyder programs INTO this IRM Nano model — module-nav
  shows `FunctionBlockMigrator`/`PhysicalPointMigrator` consuming `BNanoFunctionBlock`.
- **[Bloque 75]** (seguridad): the hardcoded-AES-key + FINEST-log-leak findings (§242.9) extend the security arc
  into the IRM config channel; the real trust boundary is the Nano protocol + firmware, as B105 also concluded.

---

## 242.11 — Self-verify

- **Tokens re-verified by me (`[CERT]`, 6/6 load-bearing)**: (1) AES key literal → `"irmn4encryption1"`
  (`AesSymmetricCryptographer.java:114`), (2) sim-transport `portNumber=47616` + sole-socket
  (`BSimulationProtocolService.java:54`), (3) `NanoCmd` framing (`NanoCmd.java:14,17`), (4) factory
  extension-point (`NanoFbFactory.java:112`), (5) brand license gate (`BrandHandler.java:24`), (6)
  `BIrmBacnetDevice extends BBacnetDevice` (`:591`).
- **Block TYPE**: EVIDENCE (decompilation). Marker tally is dominated by `[CERT]`/`[CERT-a]` with few `[INFER]`
  (only the layering/transport deductions) → healthy for an evidence block; the module has MORE investigable
  depth (`-wb` 51 classes, `-ux` 9, per-opcode auth trace, modbus/schedule packages) → focus NOT exhausted.
- **New gaps queued** (see RESEARCH-STATE-oem-honeywell-tail): U1b `honIrmConfig-wb` Workbench UI (51 cls) +
  `-ux` (9); U1c per-opcode auth enforcement trace over `NanoCmdIds`; then U2–U15.
