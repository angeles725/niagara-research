# RESEARCH-STATE — focus: oem-honeywell-tail (PLANNED)

> Multi-focus corpus (METHODOLOGY §16). This focus was SEEDED from the coverage audit
> `audits/2026-07-12-coverage-audit.md` (applied 2026-07-15) — NOT hand-guessed. It collects the
> investigable tail the corpus has NOT yet covered: OEM-Honeywell + framework driver modules that are
> decompiled and present under `/home/cristian/modules/Prototipos/modulos/organized/` (and the
> `module-navigator` 926-JAR inventory) but carry no dedicated block.
>
> **Status: ACTIVE** — first block written (**B242**, 2026-07-15, honIrmConfig-rt spine). The loop must NOT
> re-BOOTSTRAP this focus (§16): it picks up this state and writes the next block against the highest-priority
> open gap. Corpus language for NEW blocks = **English** (matches the Spyder-era convention; the pre-B115
> legacy blocks are Spanish).
>
> Scope note: the coverage audit found ~90% coverage of the corpus's STATED mission (N4 mental model +
> Honeywell OEM stack + frontend + analytics + security) but only ~17–20% of the full decompiled universe
> (~340 distinct-logic modules). This focus is the prioritized, mission-relevant slice of that tail — led
> by the four HIGH-priority Honeywell modules — NOT an attempt to cover all 340 modules. The genuinely
> out-of-scope bulk (U16 207 LON vendor profiles, U17 41 lexicons) is explicitly excluded.
<!-- research-state.v1 -->
schema: research-state.v1
covered_blocks: 266
gaps_closed: 0
known_gaps: 0
investigable_open: 0
requires_execution_open: 1
blocked_open: 0
<!-- /research-state.v1 -->


focus: oem-honeywell-tail
status: active
seeded_from: audits/2026-07-12-coverage-audit.md
seeded_on: 2026-07-15
gaps_total: 15 investigable (U1 depth-covered by B242; sub-gaps U1b/U1c queued) + 2 blocked
gaps_closed: 9 (U1–U8 → B242–B249 · U9 → B250)
blocks_written: B242–B250
block_prefix: niagara-mental-model-bloqueN.md (shared global numbering; next free number derived live at loop time)

## Gap-backlog (prioritized) — from coverage audit §3

Every "where" path is under `/home/cristian/modules/Prototipos/modulos/organized/` unless noted, and was
grep-verified ABSENT from all 122+ block `.md` files by the coverage audit (mention count 0–1).

| Pri | ID | Gap | Where (`organized/…`) | State | Status |
|---|---|---|---|---|---|
| **HIGH** | U1 | **honIrmConfig** — IRM/BEATS config-runtime layer (completes the triad with B105 `honIrmControl`) | `honIrmConfig/…-rt/vineflower` | investigable | **DEPTH-COVERED → B242** (honIrmConfig-rt spine: manager tier, Nano protocol, FB factory, OEM brand, crypto; §14 refines B88 transport + B105 call-direction) |
| note | U1-appl | **honIrmAppl** — resource-only palette jar (`class_count:0`, `tiene_codigo_java:false` per fase1-recon) — NO decompiled Java | `honIrmAppl/…-rt.jar` | blocked-on-source (no code) | closed by proven-absence (resource-only; not a code module) |
| MED | U1b | **honIrmConfig `-wb` (51 cls) + `-ux` (9 cls)** — Workbench UI (field editors, menu agents, views) + UX server-side-call handlers of the IRM Nano config tool | `honIrmConfig/…-wb`, `…-ux` vineflower | investigable | **COVERED → B493** (no IRM auth gate — stock `ri` + 3 `unrestricted` peripheral RPCs; UI edits IP/WiFi/app/terminal config, delegates device auth to rt) |
| MED | U1c | **Per-opcode auth trace** — does EVERY `NanoCmdIds` opcode enforce a password/session check, or only a subset? (B242 §242.9 left this open) | `honIrmConfig/…-rt/…/protocol`, `…/manager` sec state machine | investigable | **COVERED → B493** (auth is UNIFORM transport-AES for ALL opcodes, not per-opcode; key = irmn4encryption1⊕serial default / MD5(pw); real gate is firmware-side BIrmCtrlSecStateEnum) |
| **HIGH** | U2 | **honFirmwarePackage + honeywellVersionManager** — firmware packaging + version mgmt (supply-chain) | `honFirmwarePackage/` (0 code), `honeywellVersionManager/` (1 cls) | investigable | **COVERED → B243** (both THIN: honFirmwarePackage = code-signed resource-only firmware delivery vehicle w/ 3 payloads — HMI + PanelBus IO fw, DigiCert-G4→Honeywell signer; honeywellVersionManager = 1-class BHonVersion 6-tuple. Ties B94 OTA + B242 fw inst-nums + B75/B113 signing) |
| **HIGH** | U3 | **honAlarmConsole + honAlarmExt** — Honeywell alarm console + alarm extensions (OEM layer over B8/B34 alarm) | `honAlarmConsole/` (5 cls), `honAlarmExt/` (5 cls) | investigable | **COVERED → B244** (honAlarmExt = alarm-delay/transient-suppression: BHonAlarmClass/BHonConsoleRecipient buffer+BDelayFilterState UNKNOWN/DELAYED/SENT/IGNORED; honAlarmConsole = brand-aware JS console BIFormFactorMax + BQL RPC summary, license-gated, same 7-brand set as B242. ZKM on console) |
| MED-HIGH | U4 | **SylkActuatorAnalytics + lonHoneywellAnalytics** — Honeywell OEM analytics (ties to B66–68 + B88 Sylk) | `SylkActuatorAnalytics/` (4 cls), `lonHoneywellAnalytics/` (0 code) | investigable | **COVERED → B245** (SylkActuatorAnalytics = actuator cycle-count wear analytics: BSylkActuatorService + RPC NEQL query n:history/ActuatorCycleCount → JS bar-graph widget. lonHoneywellAnalytics = resource-only LON IAQ device-interface bundle IAQCo2/IAQMulti.lnml, signed by Honeywell Product PKI — 2nd signer vs B243 DigiCert) |
| MED | U5 | Honeywell utility modules — BACnet helper, BAC restore, lonsock client, description utility | `honBacnetHelper/` (73 cls), `honLonsockClient/` (15), 2×1 | investigable | **COVERED → B246** (honBacnetHelper = BACnet descriptor extensions w/ BIHonCommon default-logic + HON-NADV license bypass + HonPrivateTransferListener VENDOR_ID_HON=17/PT-18/30 = same channel as B242 BIrmConfig; honLonsockClient = LON-over-TCP Echelon RNI port 3830; honUtilityBacRestore + honDescriptionUtility = 1-cls each) |
| MED | U6 | **honeywellAXPlatinum(+HR), honeywellASC** — legacy AX / ASCOT-adjacent OEM (B107 covered `ascCommon/ascBacnet/ascLon`, NOT `honeywellASC`) | `honeywellAXPlatinum/` (3), `HR/` (1), `honeywellASC/` (1) | investigable | **COVERED → B247** (TRIVIAL legacy-AX residue: AXPlatinum = AX-era Px UI — BHonAnimator 9-frame image widget + BHonPalette root; HR = high-res palette variant; honeywellASC = 1-cls stub AscVav — closes B107's "honeywellASC not covered" note as proven-thinness) |
| MED | U7 | **Forge Connect onboarding + model-sync variants** (TRIDIUM modules, not Honeywell — but HBT_PRODUCTION cloud env) | `fcEasyOnboard/` (14), `fcModelSync{Bacnet,Niagara}/` (2+2) | investigable | **COVERED → B248** (fcEasyOnboard = BEasyOnboard ICloudConfiguration cloud device onboarding — deviceRegistration/AuthURL + BEnvironment CB/HBT/QA/STAGING/UAE prod + easyOnBoard/clean jobs, deps nIotHubConnector/nSentienceConnector. fcModelSync = cloud→station WRITE path: BacnetCloudWriter via priority + FoxCloudWriter via Fox proxy) |
| MED | U8 | **Centraline residue** — AHU/Heating PX, LON IO r5, profile, station-upgrade tool, extensions, printout, DIN symbols | 8 mods (clPrintout 24, clStationUpgradeTool 11, clExtensions 2, clProfile 1 + 4 resource-only) | investigable | **COVERED → B249** (KEY: clStationUpgradeTool = CentraLine→Honeywell REBRAND migration — n4mig.exe -filePassPhrase:Centraline + rewrites clBACnetUtilities:*→hon:* in BOG/px/nav; clProfile = CentraLine skin steering to Honeywell agents. EXPLAINS the multi-brand pattern of B242/244/246/248. clPrintout = station-doc PDF gen + clPrintout.exe, 9-brand+expiry gate) |
| LOW-MED | U9 | **Honeywell Modbus smart-sensor + plantController migrators** — DELTA over B90 | honPlantControllerMigrator (68), honeywellModbusSmartSensor (25), honPlantControllerEHMigrator (8) | investigable | **COVERED → B250** (honPlantControllerMigrator = online EagleHawk/BEATS-Adv→PanelBus migration job, StationType=B90 #17, strips the 17 B246 BHonBacnet*Descriptors, CustomIOMigrator plugin registry; EHMigrator = EagleHawk onboard-IO→SnapOnIO plugin, panelbus license gate; ModbusSmartSensor = TR50 air-quality register map TEMP1/HUM2/CO2 3/PM25 4/AQI30, fn codes 3/4/6) |
| LOW-MED | U10 | **Other-vendor OEM drivers** — Andover, Carrier CCN, McQuay, AAP, MAXPRO, Orion, Silk, axvelocity, BACnet FFT | `andoverAC256/`, `andoverInfinity/`, `ccn/`, `mcquay/`, `aaphp/`, `aapup/`, `maxpro/`, `orion/`, `alarmOrion/`, `silk/`, `axvelocity/`, `BACnetFFTN4/` | investigable | **COVERED → B495** (breadth survey: 11 present; scope fixes axvelocity=Apache Velocity & silk=SOAP; standouts mcquay hardcoded pw FFFFFFFF, maxpro RTSP, axvelocity SSTI, BACnetFFTN4 ungated fw push) |
| LOW | U11 | **Video subsystem** — entire Tridium/OEM video stack, no block | `nvideo/`, `naxisVideo/`, `remoteVideo/`, `videoDriver/`, `videoMigrator/`, `baseRtsp/`, `xprotect/`, `maxpro/` | investigable (out of Honeywell-BMS mission) | open |
| LOW | U12 | **Tridium framework drivers not deep-distilled** — OPC-UA, Modbus framework, M-Bus, SNMP, oBIX, OpenADR, weather. **SNMP/`nSnmp` slice distilled by [Block 476]** (two modules, `ns:`=nSnmp NDriver, typed proxy exts + `BLinearConversion` scaling, enum `range` auto-populate, v3 USM SHA-only, native traps, single `tridium/snmp` licence feature); corrects [Block 28] §28.6's install-wide "no v3". Rest still open. | `opcUaClient/Core/Server/`, `opc/`, `modbusCore/Async/Tcp*/Slave*/`, `mbus/`, `snmp*/`, `nSnmp/` (SNMP done B476), `obixDriver/`, `openAdr/`, `weather/`, `weatherUnderground/` | investigable (mostly out of mission) | open (SNMP slice done) |
| LOW | U13 | **Data + service framework** — RDBMS integration, system DB, reporting, search, dashboard, virtual (B28 only touched virtual) | `rdb*/`, `systemDb/`, `orientSystemDb/`, `report/`, `search/`, `dashboard/`, `niagaraVirtual/` | investigable (out of mission) | open |
| LOW-MED | U14 | **Extended auth/identity** beyond B11/B30 RBAC+federation — SAML, OAuth2, LDAP, gauth, client-cert, e-signature | `saml/`, `samlEncryption/`, `oauth2/`, `ldap/`, `gauth/`, `clientCertAuth/`, `electronicSignature*/` | investigable (security-relevant) | **COVERED → B494** (pluggable BAuthenticationScheme: SAML SP+IdP, oauth2=outbound client, LDAP simple-bind TLS-off default, Kerberos unconditional deleg, gauth weak TOTP, clientCert exact-pin self-anchor; eSignature=B350-356) |
| LOW | U15 | **Tridium doc corpus (niagara-help)** — ~80 `doc*` bundles + the `niagara-help` tree un-synthesized (only `docHoneywellSpyder`=B116 done) | `organized/doc*/`, `Honeywell/…/niagara-help/` | investigable (doc-synthesis, not code-distillation) | open |

### Out-of-scope (recorded, will NOT be opened as gaps)

- **U16** — 207 `lon*` vendor profile drivers (`organized/lon{Aaon…Zytron}/`): LonMark XIF device profiles;
  B19/B92 cover the LON framework + Honeywell LON wizards. Per-vendor profiles are repetitive XIF data with
  near-zero unique logic → covering them would inflate block count with no new knowledge.
- **U17** — 41 `niagaraLexicon*` translation bundles (`organized/niagaraLexicon*/`): i18n strings only.

## Blocked / non-read-only gaps (tagged with what they need) — from coverage audit §3

- **B-1 (G8)** — Spyder→IRM round-trip migration FIDELITY (B115 lossy FB reconstructions). Needs a migrated
  `.bog` on a **live IRM/BEATS controller**. `requires-execution / hardware`. (Also tracked in the Spyder
  focus `RESEARCH-STATE.md` as G8 — same gap, single source of truth is the Spyder state; listed here only
  because the coverage audit re-surfaced it.)
- **B-2 (G5b)** — `tasowizSupport` module internals (CV-AHU/VAV/LCBS wizard templates). Module is **absent**
  from `organized/` (confirmed: no such dir); referenced only by string + reflection. `blocked-on-artifact`.
  (Also tracked in the Spyder focus as G5b — same gap.)

## Iteration history

| It | Date | Gap closed | Block | New gaps uncovered |
|---|---|---|---|---|
| it.1 | 2026-07-15 | **U1 core** — `honIrmConfig-rt` spine (manager tier BIrmBacnetDevice/BIrmControlManager/BIrmConfig · Nano command protocol NanoCmd/NanoCmdIds/NanoResponseCodes · extension-point FB factory · BModelFeaturesEnum · 7-brand OEM licensing · own AES/PBKDF2 crypto layer w/ hardcoded-key weakness · inherited BBacnetDevice contract). §14 refines B88 (transport: BACnet sole prod, sim-TCP :47616, IP/BLE=config not transports) + B105 (call-direction one-way honIrmControl→honIrmConfig, 474 edges/0 reverse). Delegated: 2 sweeps · sonnet (vineflower recon + module-nav/docSource relational). 6/6 load-bearing tokens re-verified by me. | **B242** | U1b (-wb 51 + -ux 9 UI), U1c (per-opcode auth trace); honIrmAppl closed by proven-absence (resource-only) |
| it.2 | 2026-07-15 | **U2** — firmware supply-chain (both THIN, e2 re-scoped): honFirmwarePackage = 0-code resource-only firmware DELIVERY module (3 payloads: HMI_FW_v1.5.4.26.frm 1.7MB + PanelBus Pb_fw.bin/Pb_fw_Snapon.bin, magic-byte variant discriminator), jar code-signed DigiCert-G4 RSA4096/SHA384 → Honeywell International Inc., per-entry SHA-256 manifest incl. firmware. honeywellVersionManager = 1 class BHonVersion (BStruct, 6-tuple Niagara+Tool version, swallow-to-zero compare). Read inline (no sweep). All [CERT] observed by me. | **B243** | none net-new (both modules exhausted); honFirmwarePackage closed by proven-absence on code axis |
| it.3 | 2026-07-15 | **U3** — OEM alarm layer (both SMALL, ~10 cls, read inline): honAlarmExt = alarm-delay/transient-suppression over Tridium alarm fw (BHonAlarmClass extends BAlarmClass + BHonConsoleRecipient extends BConsoleRecipient, both add enableAlarmDelay/delayTime/sendDelayBufferOnShutdown + buffer via doRouteAlarm/handleAlarm; BDelayFilterState UNKNOWN/DELAYED/SENT/IGNORED — send only if still active after delay). honAlarmConsole = brand-aware JS console (BHonAlarmConsole implements BIJavaScript,BIFormFactorMax) + BQL RPC summary (getMultiSourceSummary→JSON, BDynamicTimeRange+BFilterSet, license-gated getBrandFromLicenseFile, same 7-brand set as B242). ZKM on console (1 method decompile-failed). | **B244** | none net-new (both exhausted) |
| it.4 | 2026-07-15 | **U4** — OEM analytics (SMALL, read inline): SylkActuatorAnalytics (4 cls) = actuator cycle-count wear analytics — BSylkActuatorService extends BAbstractService (autoConfigureNiagaraNetwork action) + BSylkActuatorToolRPC NEQL query `n:history and s:ActuatorCycleCount and s:ActuatorName` → JSON bar-graph + BSylkActuatorWidget JS (BIJavaScript/BIFormFactorMax/BIOffline). lonHoneywellAnalytics (0 code) = resource-only LON IAQ device-interface bundle (IAQCo2/IAQMulti .lnml XLonDevice, programID 80000c0a46040402). SIGNING: signed by Honeywell Product PKI RSA (OU=ACS) — a 2ND internal signer vs B243's external DigiCert-G4. | **B245** | none net-new (both exhausted) |
| it.5 | 2026-07-15 | **U5** — Honeywell utilities (honBacnetHelper 73 + honLonsockClient 15 + 2×1; delegated sonnet sweep, 4 tokens re-verified): honBacnetHelper = BACnet descriptor extensions (thin-subclass + BIHonCommon default-method logic, property-ID interception 104/36/117/103/28, live RELIABILITY read); license gate HON-NADV host-id auto-bypass + getFeature("Honeywell","honBacnetUtil"); HonPrivateTransferListener VENDOR_ID_HON=17 ConfirmedPT=18/UnconfirmedPT=30 = SAME channel B242 BIrmConfig rides. honLonsockClient = LON-over-TCP Echelon RNI (DEFAULT_PORT 3830, RniMessage MSG_TYPE 128-134, register/open/setdomain handshake). honUtilityBacRestore = BAC adapter save/restore (brand-gated). honDescriptionUtility = point→alarm metadata propagation (XOR-obfuscated default). | **B246** | none net-new |
| it.6 | 2026-07-15 | **U6** — legacy AX/ASCOT residue (TRIVIAL ~5 cls, inline): honeywellAXPlatinum = AX-era Px UI (BHonAnimator extends BWidget 9-frame animator + BHonPalette root); HR = BHonHrPalette high-res variant; honeywellASC = 1-cls stub AscVav — closes B107 "honeywellASC not covered" as proven-thinness. | **B247** | none |
| it.7 | 2026-07-15 | **U7** — Forge Connect onboarding (TRIDIUM not Honeywell, but HBT cloud env; ~18 cls inline): fcEasyOnboard (14) = BEasyOnboard ICloudConfiguration cloud onboarding (deviceRegistration/AuthURL, BEnvironment CB/HBT/QA/STAGING/UAE prod, easyOnBoard/clean → BOnboardJob/BCleanCloudConfigJob, deps nIotHubConnector/nSentienceConnector). fcModelSyncBacnet+Niagara (2+2) = cloud→station WRITE path (BacnetCloudWriter via priority, FoxCloudWriter via Fox proxy). | **B248** | none |

## Stop control (METHODOLOGY §8)

- **Open gaps — read-only investigable (IN-MISSION)**: **0**. All in-mission gaps closed 2026-08-24:
  U1–U9 (B242–B250), U1b/U1c (B493), U14 (B494), U10 (B495). ← focus effectively STOPPED for its mission scope.
- **Gaps closed**: **U1–U9, U1b, U1c, U10, U14** + honIrmAppl/honFirmwarePackage/lonHoneywellAnalytics & 4
  Centraline bundles by proven-absence.
- **Open gaps — LOW / OUT-OF-MISSION (not code-distillation targets)**: U11 (video stack), U12 (Tridium
  framework drivers — SNMP slice done B476), U13 (data/service framework), U15 (doc corpus). These are outside
  the OEM-Honeywell-BMS mission; open a dedicated focus if ever wanted.
- **Open gaps — requires-execution**: **1** (B-1/G8, shared w/ Spyder). **blocked**: **1** (B-2/G5b, shared).
- **Coverage metric**: **13 / 15** investigable in-mission gaps closed; remaining 2 tracked lines are shared
  requires-execution/blocked; U11-U15 reclassified LOW/out-of-mission.

**Resume condition**: in-mission scope is DONE. To go further, either (a) a dedicated `video`/`framework-drivers`
focus for U11-U13/U15, or (b) the shared requires-execution gaps against a live system.
