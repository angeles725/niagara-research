# Bloque 248 — U7 Forge Connect onboarding: `fcEasyOnboard` (cloud device onboarding) + `fcModelSync{Bacnet,Niagara}` (cloud model-sync write path)

> Empirical coverage of gap U7 (coverage-audit `audits/2026-07-12-coverage-audit.md`): the Forge Connect cloud
> onboarding trio. Measured pre-flight (§13 e2): `fcEasyOnboard` = 14 classes, `fcModelSyncBacnet` = 2,
> `fcModelSyncNiagara` = 2.
>
> **SCOPE NOTE**: these are **Tridium** modules (`vendor="Tridium"`, package `com.tridium.fcEasyOnboard.*`,
> vendorVersion 2023.14), NOT Honeywell OEM — Forge Connect is Tridium's cloud platform. They were flagged by
> the coverage audit because [Bloque 84] (`honCloudEasyOnboard`, the Honeywell-branded equivalent) + [Bloque 85]
> (`fcModelSync`) were covered and these were not. BUT they ARE mission-relevant: the environment enum includes
> an **`HBT_PRODUCTION` (Honeywell Building Technologies) cloud** target — Honeywell rides Forge Connect.
>
> **Focus**: `oem-honeywell-tail`, gap U7 (MED). Block after B242–B247.
>
> **Sources**: `organized/{fcEasyOnboard,fcModelSyncBacnet,fcModelSyncNiagara}/<m>-rt/vineflower/com/tridium/**`
> + module.xml. **Method**: read inline. `[CERT]` = observed by me at the cited `file:line`; `[INFER]` =
> deduction.
>
> Capa 22 (adjacent). **Conecta fuerte**: [Bloque 84] (`honCloudEasyOnboard` — Honeywell-branded onboarding),
> [Bloque 85] (`fcModelSync` + the Sentience/Forge/Azure-IoT cloud stack), [Bloque 7] (BACnet), Fox blocks.

---

## 248.1 — `fcEasyOnboard`: cloud device onboarding service `[CERT]`

`BEasyOnboard extends BAbstractService implements ICloudConfiguration` (`fcEasyOnboard/BEasyOnboard.java`) —
the service that registers a Niagara station with the Forge Connect cloud.

- `@NiagaraProperty`s (`BEasyOnboard.java:28-56`) `[CERT]`: `deviceRegistrationURL`, `deviceAuthenticationURL`,
  `registrationURL`, `systemType`, `migratingAKnownDevice` (boolean), and `environment` (type `BEnvironment`,
  default `cbProduction`).
- `@NiagaraAction`s (`:58-62`) `[CERT]`: **`easyOnBoard`** (run the onboarding) + `clean` (tear down cloud
  config).
- **`BEnvironment extends BFrozenEnum`** (`env/BEnvironment.java:16-20`) `[CERT]` — the target cloud environment:
  `CB_PRODUCTION=0`, **`HBT_PRODUCTION=1`** (Honeywell Building Technologies), `QA=2`, `STAGING=3`,
  **`UAE_PRODUCTION=4`** (a regional deployment). `[INFER]` So one onboarding module serves multiple cloud
  tenants/regions including a dedicated Honeywell HBT production cloud — the multi-brand pattern reappears at
  the cloud tier.
- **Jobs** `[CERT]`: `BOnboardJob extends BSimpleJob` (runs the registration flow) + `BCleanCloudConfigJob
  extends BSimpleJob` (removes it). `[INFER]` `easyOnBoard` spawns `BOnboardJob`, which POSTs to
  `deviceRegistrationURL`/`deviceAuthenticationURL` to register+authenticate the device, then wires up the
  IoT-Hub/Sentience connectors.
- **Cloud helpers** `[CERT-a]` (from MANIFEST): `CloudAlarmsHelper`, `CloudAlertsHelper`, `CloudEventsHelper`,
  `CloudConnectorHelper`, `ActivityStatus`, `ActivityLogger`, `nUtils` — the alarm/alert/event forwarding glue.
- **Module deps** `[CERT]` (module.xml): `nConnector-rt`, `nIotHubConnector-rt`, `nIotHubDep-rt`,
  `nSentienceConnector-rt` (Tridium 2023.14) — i.e. it sits ON TOP of the Tridium IoT-Hub + Sentience cloud
  connectors ([Bloque 85] cloud stack), not a standalone cloud client.

---

## 248.2 — `fcModelSync{Bacnet,Niagara}`: the cloud model-sync WRITE path `[CERT]`

Two thin (2-class) support modules that plug driver-specific discovery + cloud write-back into the model-sync
framework:

- **`fcModelSyncBacnet`** `[CERT]`: `BBacnetModelDiscoverer extends BModelDiscoverer` (discovers the BACnet
  point model to push to the cloud) + `BacnetCloudWriter extends LinkingCloudWriter`
  (`fcModelSyncBacnet/BacnetCloudWriter.java:14`) — implements `getPointActivePriority`/
  `updatePointRemoteActivePriority`/`configureTargetPoint` (`:15-32`), i.e. a **cloud command writes a BACnet
  point via its priority array**.
- **`fcModelSyncNiagara`** `[CERT]`: `BNiagaraHistorySourceDiscoverer extends BHistorySourceDiscoverer`
  (discovers Niagara history sources) + `FoxCloudWriter implements ICloudWriter, Interest`
  (`fcModelSyncNiagara/FoxCloudWriter.java:30`) — `write(BCloudWriteInfo, cloudProxyPoint, …)` (`:31`) pushes a
  cloud write down to a station point **through a Fox cloud-proxy point** (`com.tridium.fox.sys.BFoxClientConnection`).

`[INFER]` So the model-sync half is the cloud→station WRITE-BACK path: the cloud can command a BACnet point
(via priority) or a Niagara point (via Fox proxy), the mirror of [Bloque 85]'s model push (station→cloud).

---

## 248.3 — Conexiones

- **[Bloque 84]** (`honCloudEasyOnboard`): the Honeywell-BRANDED onboarding module; `fcEasyOnboard` is the
  Tridium base, and its `HBT_PRODUCTION` environment is the Honeywell cloud those two share.
- **[Bloque 85]** (`fcModelSync` + Sentience/Forge/Azure-IoT stack): `fcEasyOnboard` depends on the same
  `nIotHubConnector`/`nSentienceConnector`; `fcModelSync{Bacnet,Niagara}` are the driver-specific writers of the
  B85 model-sync framework.
- **[Bloque 7]** (BACnet): `BacnetCloudWriter` writes BACnet points via the priority array — cloud-driven BACnet
  command.
- **Fox blocks**: `FoxCloudWriter` rides `BFoxClientConnection` — cloud write-back over the Niagara Fox protocol.

---

## 248.4 — Self-verify

- **Claims observed by me** (`[CERT]`): `BEasyOnboard` service decl + slots + actions
  (`BEasyOnboard.java:28-62`), `BEnvironment` ordinals incl. HBT/UAE (`env/BEnvironment.java:16-20`), the two
  jobs, the module deps (module.xml), the two model-sync writers (`BacnetCloudWriter.java:14-32`,
  `FoxCloudWriter.java:30-31`). `[CERT-a]` = the Cloud*Helper set (from MANIFEST, not read). `[INFER]` = the
  onboarding flow + write-back-path deductions.
- **Block TYPE**: EVIDENCE. U7 covered. Honest scope note: Tridium (not Honeywell) modules, but the HBT cloud
  environment + the B84/B85 lineage make them mission-adjacent.
- **New gaps queued**: none. Next per RESEARCH-STATE-oem-honeywell-tail: U8 Centraline residue
  (`CentralineAhuPx`/`CentralineHtgPx`/`clProfile`/`clStationUpgradeTool`/…), or U1b/U1c.
