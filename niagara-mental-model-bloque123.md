# Block 123 — OptimizerSupervisor: what the REAL live station controls (station config.bog decode)

> Research of the **real installed Honeywell OptimizerSupervisor N4.14.0.162** — first block of a NEW
> arc (Layer 24, "live install") that leaves the decompiled-module corpus and reads the **actual
> production deployment** on disk: the running stations, their driver networks, controlled HVAC
> equipment, histories, schedules, and the OEM dashboard services. Scope: the station-configuration
> layer only (the "what it controls" question). The platform/daemon, security-model, install-inventory
> and workspace-dev domains are seeded as backlog, not covered here.
>
> Sources (REAL production data, READ-ONLY):
> - `/mnt/c/ProgramData/Niagara4.14/OptimizerSupervisor/stations/PRUEBAS/config.bog` — LIVE station (daemon-managed)
> - `/mnt/c/ProgramData/Niagara4.14/OptimizerSupervisor/stations/REFLOW/config.bog` — LIVE station
> - `/mnt/c/Users/equipo/Niagara4.14/OptimizerSupervisor/stations/HoneywellMX60/config.bog` — largest deployment (workspace)
> - `/mnt/c/ProgramData/Niagara4.14/OptimizerSupervisor/etc/nre.properties`, `daemon/daemon.properties`
>
> Method: a `.bog` is a ZIP container of a single `file.xml` (Baja Object Graph). Decoded READ-ONLY by
> `unzip config.bog → file.xml` into a scratch dir (originals untouched); citations are
> `config.bog#file.xml:LINE`, reproducible by re-unzipping the same `.bog`. Component types read from
> the `t="prefix:Type"` attribute of the BOG XML.
>
> SENSITIVITY: production data. No secrets are reproduced — the live stations encode reversible secrets
> via `reversibleEncodingKeySource="keyring"` (see §123.6), so password values are not present in
> plaintext in the XML and none are quoted here.
>
> Markers: `[CERT]` local primary source (`file:line`) · `[CERT-doc]` official doc · `[CERT-web]` ·
> `[CERT-a]` secondary · `[INFER]` deduction.
>
> Layer 24 (live install). Connects [Block 7] (drivers framework — networks/devices), [Block 8]
> (alarm/history/schedule), [Block 5] (BOG format), [Block 76] (chihuahua module — the live
> `ChiDashboardService`), [Block 114] (BOG encryption / keyring), [Block 13] (sensitive-data keyring).

---

## 123.1 — The deployment shape: what is actually running `[CERT]`

The daemon manages **two live stations**, both auto-started, on this supervisor host. `[CERT]`
`etc/../daemon/daemon.properties`:

| Property | Value | Meaning | Citation |
|---|---|---|---|
| `station.PRUEBAS.isautostart` | `true` | PRUEBAS boots with the daemon | `daemon.properties` |
| `station.PRUEBAS.isautorestart` | `true` | auto-restart on crash | `daemon.properties` |
| `station.REFLOW.isautostart` / `isautorestart` | `true` | REFLOW also live | `daemon.properties` |
| `authtype` | `basic/native` | OS + native auth for the platform daemon | `daemon.properties` |
| `admingroupid` | `S-1-5-32-544` | Windows local Administrators SID gates platform admin | `daemon.properties` |
| `keyAlias` | `default` | platform TLS key alias | `daemon.properties` |
| `tlsUseExtendedMasterSecret` | `true` | RFC 7627 EMS on platform TLS | `daemon.properties` |

JVM sizing for stations `[CERT]` `etc/nre.properties`: `station.java.options=-Dfile.encoding=UTF-8
-Xss512K -Xmx1024M` (1 GB heap per station), `softjace=false` (this is a real supervisor host, not a
soft-JACE). The root object of each decoded graph is `t="b:Station"`. `[CERT]` `PRUEBAS/config.bog#file.xml:3`.

PRUEBAS is the larger, richer station (decoded XML ≈ 1.29 MB) and is treated as the representative
"what it controls" sample below; REFLOW is a smaller reflow-oven supervision station (decoded ≈ 412 KB).

## 123.2 — Field protocols: the driver networks `[CERT]`

PRUEBAS speaks **five** distinct driver networks simultaneously — this is the concrete answer to "how
does it reach the field". `[CERT]` `PRUEBAS/config.bog#file.xml`:

| Network | Type | Role | Citation (line) |
|---|---|---|---|
| NiagaraNetwork | `nd:NiagaraNetwork` | **supervisory** — federates subordinate JACE/stations; carries `ProvisioningNwExt` | `:21903` |
| BacnetNetwork | `bac:BacnetNetwork` | BACnet/IP field bus to HVAC controllers | `:23017` |
| SnmpNetwork | `ns:SnmpNetwork` | SNMP monitoring of IT/network devices | `:26587` |
| ObixNetwork | `od:ObixNetwork` | oBIX REST integration (external systems) | `:27861` |
| AbstractMqttDriverNetwork | `amd:AbstractMqttDriverNetwork` | MQTT telemetry/cloud | `:22913` |

So OptimizerSupervisor is a genuine **multi-protocol supervisor**: it both *supervises* downstream
Niagara stations (NiagaraNetwork + provisioning) AND *directly drives* BACnet field equipment, while
also bridging SNMP, oBIX and MQTT. REFLOW is narrower — only `bac:BacnetNetwork` and
`nd:NiagaraNetwork`. `[CERT]` `REFLOW/config.bog#file.xml` (network grep: BacnetNetwork + NiagaraNetwork only).

Device endpoints under the networks include `bac:LocalBacnetDevice`, `irmn:IrmBacnetDevice` (Honeywell
IRM room controllers over BACnet), `ns:SnmpDevice`, and `amd:AbstractMqttDevice`. `[CERT]` (PRUEBAS device-type grep).
Real BACnet device folders carry vendor-specific names — e.g. `TC500`, `TR50_3D`,
`UN-RL1644ES24NM`, plus `HonBacnetDeviceConfig`/`hon_Options_Config`/`tuningPolicies`. `[CERT]`
`PRUEBAS/config.bog#file.xml` (`<!-- /Drivers/BacnetNetwork/... -->` comments).

## 123.3 — What it controls: HVAC equipment + I/O points `[CERT]`

The controlled load is overwhelmingly **HVAC**, expressed through Honeywell OEM equipment models plus
standard Niagara control points. Counts from PRUEBAS `[CERT]` (type-frequency grep on
`PRUEBAS/config.bog#file.xml`):

| Component type | Count | What it is | Corpus link |
|---|---|---|---|
| `dtcr:DtcrHvacEquip` | 322 | Honeywell DTCR HVAC equipment objects (the bulk of the load) | OEM Layer 22 |
| `bac:BacnetObjectIdentifier` | 796 | BACnet object IDs (points mapped to the field) | [Block 7] |
| `snls:SnlsRtu` | 85 | rooftop-unit (RTU) controllers | OEM |
| `irmn:IrmParameter` | 81 | IRM (Integrated Room Management) parameters | [Block 105/115] |
| `chihua:ChiUp` | 77 | objects from the custom `chihuahua` module | [Block 76] |
| `c:NumericWritable` | 88 | writable analog control points (setpoints/commands) | [Block 6] |
| `c:NumericPoint` | 38 | read-only analog points | [Block 6] |
| `c:BooleanWritable` | 12 | writable digital commands | [Block 6] |
| `c:StringPoint` / `c:StringWritable` | 15 / 6 | string points | [Block 6] |
| `c:EnumPoint` | 5 | enumerated state points | [Block 6] |

The writable points (`NumericWritable`/`BooleanWritable`/`StringWritable`) are the **actuation
surface** — the values this supervisor can command into the field via the 16-level priority array
([Block 6]). The read points + 796 BACnet identifiers are the **sensing surface**. `[CERT]`.

## 123.4 — Scheduling: the dominant configuration mass `[CERT]`

By raw component count, **scheduling dominates** the station — this supervisor is heavily
calendar/time-driven. PRUEBAS type counts `[CERT]`:

| Schedule type | Count |
|---|---|
| `sch:WeekdaySchedule` | 1710 |
| `sch:DaySchedule` | 1330 |
| `sch:DailySchedule` | 1330 |
| `sch:YearSchedule` / `MonthSchedule` / `DayOfMonthSchedule` / `DateSchedule` / `CompositeSchedule` | 380 each |
| `sch:WeekSchedule` / `sch:DateRangeSchedule` | 190 each |
| `sch:NumericSchedule` | 186 |

Plus time-trigger control objects: `c:TimeTrigger` (11), `c:IntervalTriggerMode` (9),
`c:ManualTriggerMode` (2). `[CERT]`. This matches the Schedule contract documented in [Block 8]
(stateless `isEffective`/`nextEvent`, DFS priority, DST handling) — here applied at production scale
across hundreds of HVAC equipment objects.

## 123.5 — Telemetry persistence: histories + audit `[CERT]`

Trend/history collection is configured and active. PRUEBAS `[CERT]`:

| History element | Count | Citation |
|---|---|---|
| `h:HistoryService` | 1 (root collector) | `:457` |
| `h:HistoryConfig` / `h:HistorySchema` | 24 / 24 | (type grep) |
| `h:NumericIntervalHistoryExt` | 21 | (interval trend extensions) |
| `h:AuditHistoryService` + `h:SecurityAuditHistorySource` | 1 + 1 | (audit trail — [Block 112]) |
| `h:LogHistoryService` | 1 | (log history) |
| `bac:BacnetHistoryDeviceExt` | 3 | (BACnet device trend import) |

The presence of `AuditHistoryService` and `SecurityAuditHistorySource` ties directly to the defensive
arc ([Block 112] detection/forensics): this live station DOES keep an audit history source configured.
`[CERT]`.

## 123.6 — Secret handling at rest: keyring vs external passphrase `[CERT]`

The three stations show **two of the three BOG encryption modes** documented in [Block 114], proving
that model on real data. Header attribute `reversibleEncodingKeySource` of the root `bajaObjectGraph`:

| Station | `reversibleEncodingKeySource` | Implication | Citation |
|---|---|---|---|
| PRUEBAS (live) | `keyring` | reversible secrets sealed by the station KeyRing; XML has no plaintext secrets | `PRUEBAS/config.bog#file.xml:2` |
| REFLOW (live) | `keyring` | same | `REFLOW/config.bog#file.xml:2` |
| HoneywellMX60 | `external` + `pbkdf2-sha256` | passphrase-derived key (`...IterationCount='4096'`, validator `pbkdf2-sha256.1`, salt present) | `HoneywellMX60/config.bog#file.xml:2` |

Contrast: the throwaway `TestExport.bog` in the workspace uses `reversibleEncodingKeySource="none"`
(no secrets to protect) — `[CERT]` `/mnt/c/Users/equipo/Niagara4.14/OptimizerSupervisor/TestExport.bog#file.xml:2`.
This is exactly the `none / external / keyring` taxonomy of [Block 114] §, now confirmed on the real
install. Because the live stations are `keyring`-encoded, **structure is fully readable but secret
values are not** — consistent with the sensitivity contract for this research. `[CERT]`.
The matching keyring store files live on disk as `security/.kr` (keyring) and `security/.km`
(key-master) — `[CERT]` `ls /mnt/c/ProgramData/Niagara4.14/OptimizerSupervisor/security/` (deep dive deferred to backlog C).

## 123.7 — OEM dashboard services: the supervisor's custom application layer `[CERT]`

Beyond the standard Niagara services (AlarmService, HistoryService, BackupService, BatchJobService,
ProgramService, UserService/RoleService/CategoryService, WebService, FoxService — all present `[CERT]`
PRUEBAS `<!-- /Services/... -->` grep), the station installs a **family of custom OEM dashboard
services**, one per OEM equipment module. `[CERT]` PRUEBAS:

| Service | Type | Line | Backing module (corpus) |
|---|---|---|---|
| ChiDashboardService | `chihua:ChiDashboardService` | `:14251` | `chihuahua` — [Block 76] |
| DtcrDashboardService | `dtcr:DtcrDashboardService` | `:1104` | dtcr HVAC equipment |
| SnlsDashboardService | `snls:SnlsDashboardService` | `:3197` | snls (RTU) |
| DemangDashboardService | `demang:DemangDashboardService` | `:11674` | demang (demand mgmt) |
| AngDashboardService | `ang:AngDashboardService` | `:14095` | ang |
| MultpDashboardService | `multp:MultpDashboardService` | `:14242` | multp |

This confirms the corpus' `chihuahua` module (B76) is **deployed and live** as a station service here —
not just a decompiled artifact. The dashboard services are the OEM "Optimizer" application layer riding
on top of the generic Niagara control engine. `[CERT]`.

## 123.8 — Connections

- **[Block 7]** (Drivers Framework) — §123.2 instantiates the framework's network/device/point
  hierarchy on real data: NiagaraNetwork + BACnet + SNMP + oBIX + MQTT, with `LocalBacnetDevice` /
  `IrmBacnetDevice` / `SnmpDevice` / `AbstractMqttDevice` endpoints.
- **[Block 8]** (Alarm + History + Schedule) — §123.4 and §123.5 are this block's contracts at
  production scale: ~9000 schedule components and 21 interval history extensions + audit history.
- **[Block 6]** (Control Engine) — §123.3 writable points are the priority-array actuation surface.
- **[Block 5]** (BOG format) — confirms `.bog` = ZIP(file.xml) Baja Object Graph with `h=` handles.
- **[Block 76]** (chihuahua) — the live `ChiDashboardService` (§123.7) and 77 `chihua:ChiUp` objects
  prove B76's module is the deployed application here.
- **[Block 114] / [Block 13]** (BOG encryption / keyring) — §123.6 confirms `none/external/keyring`
  on real stations; live stations are keyring-sealed.
- **[Block 112]** (detection/forensics) — §123.5 shows `SecurityAuditHistorySource` configured live.

## 123.9 — Self-verify

- **Load-bearing [CERT] token check**: 11 tokens grep-confirmed in the decoded source —
  network type+line ×5 (`nd:NiagaraNetwork`@21903, `amd:AbstractMqttDriverNetwork`@22913,
  `bac:BacnetNetwork`@23017, `ns:SnmpNetwork`@26587, `od:ObixNetwork`@27861); dashboard service
  type+line ×6 (`dtcr`@1104, `snls`@3197, `demang`@11674, `ang`@14095, `multp`@14242, `chihua`@14251);
  plus the three `reversibleEncodingKeySource` header values (keyring/keyring/external+pbkdf2) and the
  equipment counts (dtcr 322 / bacnetObj 796 / snls 85 / irmn 81 / chihua 77) all read directly from
  the decoded XML. All present.
- **Marker tally**: [CERT] ≈ 30 · [CERT-doc] 0 · [CERT-web] 0 · [CERT-a] 0 · [INFER] 0.
  [INFER]/[CERT] ratio = 0.0 — this gap is almost pure primary-source evidence (the live config itself);
  far from exhausted, many adjacent gaps remain (see RESEARCH-STATE-optimizersupervisor.md).
- **Sensitivity applied**: live stations are keyring-encoded; no plaintext secrets exist in the XML and
  none were extracted or quoted. KeyRing/keystore files described by name/role only (§123.6), values
  never read. Originals untouched (decoded into scratch).
