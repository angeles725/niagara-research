# Bloque 250 — U9 PlantController migration + Modbus smart sensor: `honPlantControllerMigrator` (EagleHawk/BEATS-Adv→PanelBus) + `honPlantControllerEHMigrator` (EagleHawk onboard-IO) + `honeywellModbusSmartSensor` (TR50 air-quality)

> Empirical coverage of gap U9 (coverage-audit `audits/2026-07-12-coverage-audit.md`): the PlantController
> migration machinery + the Modbus smart sensor. Measured pre-flight (§13 e2): `honPlantControllerMigrator` 68
> cls, `honeywellModbusSmartSensor` 25, `honPlantControllerEHMigrator` 8 (~101 total).
>
> **DELTA over [Bloque 90]**: B90 distilled `honPlantController` (BEATS/PanelBus IPC controller, BTP protocol)
> and only TOUCHED the migrators. This block goes deep on the migration flow + the EagleHawk onboard-IO plugin +
> the Modbus air-quality sensor.
>
> **Focus**: `oem-honeywell-tail`, gap U9 (LOW-MED). Block after B242–B249.
>
> **Sources**: `organized/{honPlantControllerMigrator,honPlantControllerEHMigrator,honeywellModbusSmartSensor}/**/vineflower/**`.
> **Method**: delegated `sonnet` sweep; 4 load-bearing claim-sets re-verified by me (the sweep had two Modbus
> alarm-register values wrong — corrected below). `[CERT]` = re-verified at cited `file:line`; `[CERT-a]` =
> sweep citation; `[INFER]` = deduction.
>
> Capa 22 (OEM). **Conecta fuerte**: [Bloque 90] (`honPlantController` — the migration TARGET), [Bloque 246]
> (`honBacnetHelper` — the descriptors the migrator strips), [Bloque 242] (BEATS station type), [Bloque 105]
> (IRM control), [Bloque 75] (PanelBus licensing).

---

## 250.1 — `honPlantControllerMigrator`: online EagleHawk/BEATS-Adv → PanelBus migration `[CERT]` / `[CERT-a]`

Migrates a legacy Honeywell station (EagleHawk/Ciper50/CP-NX, or a BEATS-Advanced-with-hmiPrivate) INTO the
`honPlantController` PanelBus/BEATS model. It runs as an **online** Niagara job against the running station (the
offline `config.bog` path is wb-side pre-scan only).

- **Station-type model** `[CERT]` (`enums/StationType.java:4-7`) — `EAGLEHAWK("EHN4/Ciper50/CP-NX")`,
  `BEATS_ADVANCED("Niagara Advanced Controller Station")`, `BEATS_ADVANCED_WITH_HMI_PRIVATE("hmiPrivate")`,
  `GENERIC`. **Matches [Bloque 90] certainty-audit claim #17 verbatim** (the audited StationType) — confirms the
  migrator and controller share the model. `StationCheckUtil.getStationType()` resolves via
  `BStationInfoAccessor` then BQL `select * from honPlantController:HonPlantControllerService` `[CERT-a]`.
- **Offline-file convention** `[CERT-a]`: `model/Const.java` `BOG_FILE="config.bog"` (same constant B90 found);
  the wb `StationParser` reads it via `ValueDocDecoder` for pre-migration inventory, gating a dir as a station
  only if it contains a literal `config.bog`.
- **Migration orchestration** `[CERT-a]` (`job/BOnlineMigrationJob extends BSimpleJob`): backup
  `config_<ts>.bog` under `migratorBackup/` → onboard-IO migration → `removeNetwork` → create `BHMINetwork` →
  migrate device/alarm-service/device-ext/event-log/notification-class/point-descriptor/schedule-descriptor/FAL/
  HMINetwork in strict order → `removeLegacyServices` → `removeHMIDevice` → `addHonPlantControllerServices`.
  Choices (`addHmiNetworkDriver`, `keepHonBacnetObjects`, `pointExportOption`, …) snapshot into a
  `GlobalPageModel.Home` singleton. `saveSync` uses `ValueDocEncoder`+`BogEncoderPlugin` (zipped `.bog`,
  `BogPasswordObjectEncoder` keyring for password fields).
- **Plugin extension points** `[CERT-a]`: `BCustomIOMigrator extends BObject` looks up
  `Sys.getRegistry().getType("honPlantControllerMigrator:CustomIOMigrator")` and keys subtypes by
  `supportedStationType()` — the seam Module 2 plugs into. A parallel `BFALHomeMigrator` registry migrates FAL
  homes for BEATS_ADVANCED/EAGLEHAWK.
- **Link to [Bloque 246]** `[CERT]`: `StationMigrationUtil` imports the honBacnetHelper descriptors
  (`com.honeywell.honbacnethelper.export.BHonBacnetAnalogInputDescriptor`, …AnalogOutput/AnalogValue/
  AnalogValuePrioritized/…, `:9-12`) — `removeHMIDescriptors()` strips **17** `BHonBacnet*Descriptor` classes
  (the exact ones B246 distilled) as it maps the old BACnet-export HMI model onto PanelBus points/schedules.

---

## 250.2 — `honPlantControllerEHMigrator`: EagleHawk onboard-IO plugin `[CERT]` / `[CERT-a]`

Implements the `CustomIOMigrator` extension point for EagleHawk onboard-IO hardware, converting EagleHawk 14-/
26-point IO boards into PanelBus "SnapOn IO" devices.

- **Registration** `[CERT]`: `eaglehawk/BOnboardIOMigrator extends BCustomIOMigrator`, `supportedStationType()
  → StationType.EAGLEHAWK` (`:91-92`) — the concrete plugin Module 1 loads.
- **Topology conversion** `[CERT-a]`: `migrate()` dispatches on 4 `OnboardIOMigrationOptions`
  (`16UIO_4UIO_8DO`/`16UIO_16UIO`/`16UIO_8DO`/`16UIO`, tied to `DEVICE_14IO`/`DEVICE_26IO`) into hardcoded
  point-address maps (e.g. binaryOutput addr 1-4 → `sio_address_do_05..08`).
- **License gate** `[CERT]`: `if (station.isRunning() && !panelbusNetwork.checkIfLicensed()) throw new
  LicenseException(...)` (`BOnboardIOMigrator.java:121-122`; dedicated `LicenseException extends
  RuntimeException`; lexicon "Panelbus network is not licensed"). `[INFER]` The onboard-IO migration is gated on
  a licensed PanelBus network — the target must be a paid PanelBus deployment.
- `EagleHawkStationUtil.isEagleHawkStation()` = BQL `clOnboardIO:OnboardIONetwork` present OR
  `honEagleHawkHMI:HonEagleHawkHmiService` present `[CERT-a]`.

---

## 250.3 — `honeywellModbusSmartSensor`: the TR50 air-quality sensor `[CERT]`

`BHonModbusSmartSensorDevice extends BModbusAsyncDevice implements BIHonModbusDevice, ISmartSensorDevice` — a
Modbus (async stack) driver for the Honeywell **TR50-series indoor-air-quality sensors**. `SUPPORTED_MODELS =
{TR50-5D, TR50-5N, TR50-3D, TR50-3N}`; model chain `BHonModbusTR503N` (base) → `TR505N` (adds PM2.5/TVOC/PM1/
PM10) → `TR505D`/`TR503D` (display variants).

- **Sensor register map** `[CERT]` (`config/SmartSensorModbusRegisterDetails.java:4-14`): `TEMP=1`, `HUM=2`,
  `CO2=3`, `PM25=4`, `TVOC=5`, `PM1=14`, `PM10=24`, `AQI=30`; alarm-config registers e.g.
  `TEMP_ALARM_LOW_LIMIT=2030`, `HIGH_LIMIT=2050`, `DEADBAND=2070` (bidirectional addr↔propId maps per sensor
  type). Device-info registers `[CERT-a]`: `SERIAL_NUMBER=1030`, `MODEL_NAME=1000`, `FIRMWARE=96`,
  `BLE_FIRMWARE=98`, `DATABASE_REVISION=210`.
- **Modbus function codes** `[CERT]` (`utils/HonModbusUtil.java:62-65`): `READ_HOLDING_REG=3`, `READ_INPUT_REG=4`,
  `WRITE_SINGLE_REGISTER=6` (+ `READ_COIL=1`, `WRITE_SINGLE_COIL=5`). `readModbus()` builds a `ModbusReadRequest`
  and sends via `BModbusAsyncNetwork.sendSync()`.
- **Static model-driven discovery** `[CERT-a]`: `BHonModbusDiscoverPointsJob extends BSimpleJob` adds one
  `BHonModbusSmartSensorDiscoveryPoint` per `device.getListOfSupportedSensors()` — NO live bus scan; discovery
  is purely off the Java-hardcoded per-model sensor map.
- **Config-sync state machine** `[CERT-a]`: `BHonSyncStateEnum` driven off the config-version register (210) —
  full sync / delta sync / a warning if the device's config version is newer than the tool's.

---

## 250.4 — Conexiones

- **[Bloque 90]** (`honPlantController`): the migration TARGET — U9 is the machinery that moves EagleHawk/
  BEATS-Adv stations onto B90's PanelBus/BEATS model; shares `StationType` (claim #17) + `config.bog`.
- **[Bloque 246]** (`honBacnetHelper`): the migrator strips the 17 `BHonBacnet*Descriptor` classes B246 distilled
  — a direct code dependency, mapping the old BACnet-export HMI model onto PanelBus.
- **[Bloque 242]** (`honIrmConfig`/BEATS): `BEATS_ADVANCED` station type is the same BEATS controller family.
- **[Bloque 105]** (IRM control) + Modbus: `honeywellModbusSmartSensor` is a Modbus device analogous to the
  Modbus FBs B105 covered, here a concrete TR50 air-quality product with a full register map.
- **[Bloque 75]** (licensing): the EagleHawk migrator's `panelbusNetwork.checkIfLicensed()` gate.

---

## 250.5 — Self-verify

- **Re-verified by me** (`[CERT]`): `StationType` ordinals (`enums/StationType.java:4-7`, vs B90 #17), EH
  license gate + `supportedStationType=EAGLEHAWK` (`BOnboardIOMigrator.java:91-92,121-122`), Modbus sensor +
  alarm register map + function codes (`SmartSensorModbusRegisterDetails.java:4-14`, `HonModbusUtil.java:62-65`
  — **corrected** the sweep's mislabeled alarm limits: LOW=2030/HIGH=2050/DEADBAND=2070), the honBacnetHelper
  descriptor import in the migrator (`StationMigrationUtil.java:9-12`). `[CERT-a]` = the sweep's structural flow
  citations. `[INFER]` = the licensing/deployment deductions.
- **Block TYPE**: EVIDENCE. U9 covered as a depth block over B90 (migration flow + EagleHawk plugin) + a
  concrete Modbus product.
- **New gaps queued**: none. Next per RESEARCH-STATE-oem-honeywell-tail: U10 (other-vendor OEM drivers — 12
  modules), or U1b/U1c.
