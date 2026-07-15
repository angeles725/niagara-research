# Bloque 246 — U5 Honeywell utility modules: `honBacnetHelper` (BACnet descriptor extensions + license gate + private-transfer) · `honLonsockClient` (LON-over-TCP RNI) · `honUtilityBacRestore` · `honDescriptionUtility`

> Empirical coverage of the four Honeywell OEM **utility** modules (coverage-audit gap U5,
> `audits/2026-07-12-coverage-audit.md`). Mixed sizes (measured pre-flight §13 e2): `honBacnetHelper` = **73
> classes** (the substantial one), `honLonsockClient` = 15, `honUtilityBacRestore` = 1, `honDescriptionUtility`
> = 1.
>
> **Focus**: `oem-honeywell-tail`, gap U5 (MED). Fifth block of the focus (after B242–B245).
>
> **Sources**: `organized/{honBacnetHelper,honLonsockClient,honUtilityBacRestore,honDescriptionUtility}/<m>-<rt|ux>/vineflower/com/honeywell/**`.
>
> **Method**: `honBacnetHelper` + `honLonsockClient` via a delegated `sonnet` structural sweep; the 4
> load-bearing constants re-verified by me. `[CERT]` = re-verified at the cited `file:line`; `[CERT-a]` = sweep
> citation not re-verified; `[INFER]` = deduction.
>
> Capa 22 (OEM). **Conecta fuerte**: [Bloque 7] (BACnet framework), [Bloque 242] (`honIrmConfig` — the SAME
> Honeywell BACnet private-transfer mechanism), [Bloque 19]/[Bloque 92] (LON), [Bloque 75] (licensing),
> [Bloque 87] (Centraline — shared OEM brand gate).

---

## 246.1 — `honBacnetHelper` (73 cls): Honeywell BACnet descriptor extensions `[CERT]` / `[CERT-a]`

"Provides extensions and helper classes on top of Bacnet driver" (`module.xml`). It re-skins every stock
Tridium BACnet object descriptor with Honeywell behavior, via a consistent pattern `[CERT-a]`:

- **Thin-subclass + interface-default-logic**: `BHonBacnet<Type>Descriptor extends BBacnet<Type>Descriptor
  implements BIHonCommon<Type>Descriptor` (e.g. `export/BHonBacnetAnalogValueDescriptor.java:46`). The concrete
  class is nearly empty — every lifecycle method (`started`/`changed`/`validate`/`readProperty`/…) is a
  one-line delegation to a `common*` **default method** on the `BIHonCommon*` interface, and a parallel set of
  `super*` bridge methods exposes the parent's protected lifecycle so those interface defaults can reach
  `super.xxx()` (Java interfaces can't call `super` directly). Added `@NiagaraProperty`s: `pointMode`
  (`BHonPointMode`), `pointInFAL`, `pointPrecision`, plus `niagaraOverrideValueSave` on prioritized variants.
- **Property-ID interception** `[CERT-a]`: `commonRead/WriteProperty` intercept specific BACnet property IDs
  (104 override-write, 36 EVENT_STATE, 117 UNITS via `BHonBacnetEngineeringUnits`, 103 RELIABILITY, 28
  DESCRIPTION) before falling through to `super`. `commonValidate` LIVE-READS BACnet RELIABILITY (prop 103) off
  the wire via `BBacnetComm.readProperty` to tell a real device fault from a local out-of-range fault.
- **License gate** `[CERT]` (`license/LicenseHandler.java`): `isHonBacnetDescriptorLicensed(feature)` —
  **`Sys.getHostId().startsWith("HON-NADV")`** (`:39`, const `BEATS_ADVANCED_HOSTID_NAME="HON-NADV"` `:17`)
  **auto-licenses** (bypasses the check, `:48`) on the BEATS-Advanced host-ID class; otherwise
  `Sys.getLicenseManager().getFeature("Honeywell", feature).check()` (`:59`), feature `"honBacnetUtil"`
  (`util/Constants.java:6`). An unlicensed station does not block — it **forces `BStatus.makeFault` + faultCause**
  on every descriptor (`util/HonBacnetUtil.java`), i.e. the points show FAULT rather than disappearing. Result
  cached per JVM. `[INFER]` The HON-NADV bypass means the module is free on genuine BEATS-Advanced controllers
  and license-gated elsewhere — a hardware-tied licensing model.
- **BACnet private-transfer dispatcher** `[CERT]` (`listener/HonPrivateTransferListener.java`): a singleton that
  routes BACnet **ConfirmedPrivateTransfer (id 18)** / **UnconfirmedPrivateTransfer (id 30)** for
  **`VENDOR_ID_HON = 17`** (`:22-24`) to registered `IPrivateTransferClient`s. **This is the SAME vendor
  private-transfer channel `honIrmConfig`'s `BIrmConfig implements IPrivateTransferClient` rides ([Bloque 242]
  §242.2)** — honBacnetHelper is the generic Honeywell BACnet-private-transfer plumbing; the Nano protocol is
  one client of it.
- **Subpackages** `[CERT-a]`: `export/common/` (the shared `BIHonCommon*` logic), `export/datatypes/`
  (Honeywell BACnet ASN.1 structs `BHonBacnetAlarmPriority`/`ScheduleStateText implements BIBacnetDataType`),
  `export/enums/` (`BHonPointMode` ordinals auto/overridden/manual/down/outOfService/disabled/stale/fault/alarm/
  unackedAlarm; `BHonBacnetEngineeringUnits`), `extensions/` (`BLoopPointBypassProxyExt` — bypass override for
  kitControl `BLoopPoint`), `util/` (`HonBacnetUtil` facade — syncs IP from `en1`/`en2` adapters). Sentinel
  `IgnoreAllChangesContext extends BasicContext` (empty marker) tags internal bulk updates so `changed()`
  handlers can skip them.

---

## 246.2 — `honLonsockClient` (15 cls): LON over TCP to an Echelon RNI `[CERT]` / `[CERT-a]`

"Connect Lonsock Rni Interfaces" — replaces a local LON adapter with a **remote Echelon LNS RNI (Remote
Network Interface) server reached over plain TCP**.

- **Transport** `[CERT]`: `lonsockclient/LonsockRniClient.java:43` `DEFAULT_PORT = 3830`; the client binds
  `new BIpAddress(serverIp, 3830)` (`:58`). `BLonsockTcpCommConfig extends BTcpCommConfig` (`MAX_LINKLAYER_LENGTH
  = 4096`) plugs a `LonsockLinkMessage`/`LonsockMessageFactory` into the Niagara comm stack `[CERT-a]`.
- **RNI wire protocol** `[CERT]` (`lonsockrnimsg/RniMessage extends NMessage`): packet types `MSG_TYPE_REGISTER
  = 128`, `MSG_TYPE_OPEN = 129`, `SET_DOMAIN = 131`, `SET_DEFAULT_DOMAIN = 132`, `GET_DOMAIN = 133`,
  `MSG_TYPE_LONTALK = 134` (`:8-13`), one `RniMsg*` subclass each. `LonsockMessageFactory` dispatches by peeked
  packet type; unknown → `LonException` `[CERT-a]`.
- **Session** `[CERT-a]`: `open(rniDeviceName)` sends `RniMsgRegister(Sys.getHostId())` to enumerate the RNI's
  interface names, then `RniMsgOpen(deviceName)`, then `SetDomain`/`GetDomain` to sync the station's LON
  domain/subnet/node onto the RNI-hosted interface. `LonsockListener implements ICommListener` demuxes
  unsolicited `RniMsgLonTalk` into a poll queue vs. sequence-matched responses (wait/notify).
  `BLonPlatformServiceLonsock extends BLonPlatformService` (`MAX_CONNECTIONS = 32`) manages one client per named
  connection; `BLonsockRniConnect extends BComponent` (default `deviceName="CLON"`) live-enumerates interfaces
  on IP change.

---

## 246.3 — The two singletons `[CERT-a]`

- **`honUtilityBacRestore`** — `BBacAdapterRestore extends BComponent` (under a `BBacnetNetworkLayer`): saves
  which network adapter the BACnet/IP link bound to and RE-APPLIES it after restart (snapshot on port `isOk()`,
  restore on `stationStarted`, retry every 20s until `retryTime`). **Brand/license gated**: only runs for OEM
  brands `CentraLine`/`ComfortPoint`/`Webs`/`WebsOpen`/`SBC`/`Trend`/`HoneywellMVC` or an `"HPSS"` license
  feature (`hpssBacnetFixNetworkInterface`); else writes `"No valid license found!"` and no-ops. `[INFER]` A
  targeted fix for an adapter-binding bug that only ships to licensed OEM brands.
- **`honDescriptionUtility`** — `BHonDescriptionExtension extends BPointExtension` (`:25`), slot
  `descriptionPropertyNames` (CSV, default `"description"`): merges the named source-point slot values into the
  parent point's `BAlarmSourceExt` metadata facets so alarm records/HMI can display them. `[CERT-a]` The default
  string is stored **XOR-obfuscated** (5-byte key) and decoded at class-init — anti-string-scraping of the jar,
  not a hidden secret (plaintext = `"description"`); the class also has mangled `a()/b()/c()` method names.

---

## 246.4 — Conexiones

- **[Bloque 242]** (`honIrmConfig`): `honBacnetHelper`'s `HonPrivateTransferListener` (VENDOR_ID_HON=17,
  ConfirmedPrivateTransfer id 18) is the **generic Honeywell BACnet private-transfer plumbing** that
  `BIrmConfig implements IPrivateTransferClient` (the Nano protocol) rides on — the transport layer under B242's
  §242.2 commissioning channel.
- **[Bloque 7]** (BACnet): `honBacnetHelper` subclasses the whole `BBacnet*Descriptor` object model; its
  property-ID interception + live RELIABILITY read operate on the BACnet wire.
- **[Bloque 19] / [Bloque 92]** (LON): `honLonsockClient` is a LON transport (Echelon RNI over TCP:3830) — an
  alternate to a local LON adapter.
- **[Bloque 75]** (licensing): the `HON-NADV` host-ID auto-license bypass + `getFeature("Honeywell", …)` gate is
  the same license-as-gate pattern; here tied to a hardware host-ID class.
- **[Bloque 87]** (Centraline): `honUtilityBacRestore`'s brand list (CentraLine/ComfortPoint/Webs/WebsOpen/SBC/
  Trend/HoneywellMVC) is the multi-brand OEM gate again, a variant of the B242/B244 7-brand set.

---

## 246.5 — Self-verify

- **Re-verified by me** (`[CERT]`): HON-NADV license bypass + `getFeature("Honeywell",…)`
  (`LicenseHandler.java:17,39,48,59`), `VENDOR_ID_HON=17` / private-transfer ids 18 & 30
  (`HonPrivateTransferListener.java:22-24`), feature `"honBacnetUtil"` (`Constants.java:6`), Lonsock
  `DEFAULT_PORT=3830` (`LonsockRniClient.java:43,58`) + RNI `MSG_TYPE_*` (`RniMessage.java:8-13`),
  `BHonDescriptionExtension extends BPointExtension` (`:25`). `[CERT-a]` = the sweep's structural citations
  (descriptor delegation pattern, subpackage roles, session handshake, XOR default). `[INFER]` = the
  licensing-model + targeted-fix deductions.
- **Block TYPE**: EVIDENCE. `honBacnetHelper` is substantial (73 cls) and richly cited; the private-transfer
  link to B242 is the highest-value finding. U5 covered.
- **New gaps queued**: none net-new. Next per RESEARCH-STATE-oem-honeywell-tail: U6
  `honeywellAXPlatinum(+HR)` + `honeywellASC` (MED), or U1b/U1c.
