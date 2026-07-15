# RESEARCH-STATE — focus: oem-honeywell-tail (PLANNED)

> Multi-focus corpus (METHODOLOGY §16). This focus was SEEDED from the coverage audit
> `audits/2026-07-12-coverage-audit.md` (applied 2026-07-15) — NOT hand-guessed. It collects the
> investigable tail the corpus has NOT yet covered: OEM-Honeywell + framework driver modules that are
> decompiled and present under `/home/cristian/modules/Prototipos/modulos/organized/` (and the
> `module-navigator` 926-JAR inventory) but carry no dedicated block.
>
> **Status: PLANNED** — backlog committed, **0 blocks written yet**. The loop must NOT re-BOOTSTRAP this
> focus (§16): it picks up this state and writes its first block against the highest-priority open gap.
> Corpus language for NEW blocks = **English** (matches the Spyder-era convention; the pre-B115 legacy
> blocks are Spanish).
>
> Scope note: the coverage audit found ~90% coverage of the corpus's STATED mission (N4 mental model +
> Honeywell OEM stack + frontend + analytics + security) but only ~17–20% of the full decompiled universe
> (~340 distinct-logic modules). This focus is the prioritized, mission-relevant slice of that tail — led
> by the four HIGH-priority Honeywell modules — NOT an attempt to cover all 340 modules. The genuinely
> out-of-scope bulk (U16 207 LON vendor profiles, U17 41 lexicons) is explicitly excluded.

focus: oem-honeywell-tail
status: planned
seeded_from: audits/2026-07-12-coverage-audit.md
seeded_on: 2026-07-15
gaps_total: 15 investigable + 2 blocked
gaps_closed: 0
block_prefix: niagara-mental-model-bloqueN.md (shared global numbering; next free number derived live at loop time)

## Gap-backlog (prioritized) — from coverage audit §3

Every "where" path is under `/home/cristian/modules/Prototipos/modulos/organized/` unless noted, and was
grep-verified ABSENT from all 122+ block `.md` files by the coverage audit (mention count 0–1).

| Pri | ID | Gap | Where (`organized/…`) | State | Status |
|---|---|---|---|---|---|
| **HIGH** | U1 | **honIrmAppl + honIrmConfig** — IRM/BEATS application + config layers (completes the triad with B105 `honIrmControl`) | `honIrmAppl/`, `honIrmConfig/` | investigable | open |
| **HIGH** | U2 | **honFirmwarePackage + honeywellVersionManager** — firmware packaging + version mgmt (supply-chain; ties to B94 OTA + B75/B113 signing arc) | `honFirmwarePackage/`, `honeywellVersionManager/` | investigable | open |
| **HIGH** | U3 | **honAlarmConsole + honAlarmExt** — Honeywell alarm console + alarm extensions (OEM layer over B8/B34 alarm) | `honAlarmConsole/`, `honAlarmExt/` | investigable | open |
| MED-HIGH | U4 | **SylkActuatorAnalytics + lonHoneywellAnalytics** — Honeywell OEM analytics (ties to B66–68 + B88 Sylk) | `SylkActuatorAnalytics/`, `lonHoneywellAnalytics/` | investigable | open |
| MED | U5 | Honeywell utility modules — BACnet helper, BAC restore, lonsock client, description utility | `honBacnetHelper/`, `honUtilityBacRestore/`, `honLonsockClient/`, `honDescriptionUtility/` | investigable | open |
| MED | U6 | **honeywellAXPlatinum(+HR), honeywellASC** — legacy AX / ASCOT-adjacent OEM (B107 covered `ascCommon/ascBacnet/ascLon`, NOT `honeywellASC`) | `honeywellAXPlatinum/`, `honeywellAXPlatinumHR/`, `honeywellASC/` | investigable | open |
| MED | U7 | **Forge Connect onboarding + model-sync variants** — only `fcModelSync` (B85) + `honCloudEasyOnboard` (B84) covered; `fcEasyOnboard` has 0 mentions | `fcEasyOnboard/`, `fcModelSyncBacnet/`, `fcModelSyncNiagara/` | investigable | open |
| MED | U8 | **Centraline residue** — AHU/Heating PX graphics, LON IO r5, profile, station-upgrade tool, extensions, printout, DIN symbols | `CentralineAhuPx/`, `CentralineHtgPx/`, `CentralineLONIOr5/`, `clProfile/`, `clStationUpgradeTool/`, `clExtensions/`, `clPrintout/`, `DINsymbol/` | investigable | open |
| LOW-MED | U9 | **Honeywell Modbus smart-sensor + plantController migrators** (B95 covered BACnet TR50; B90 touched migrators) — partial | `honeywellModbusSmartSensor/`, `honPlantControllerMigrator/`, `honPlantControllerEHMigrator/` | investigable | open |
| LOW-MED | U10 | **Other-vendor OEM drivers** — Andover, Carrier CCN, McQuay, AAP, MAXPRO, Orion, Silk, axvelocity, BACnet FFT | `andoverAC256/`, `andoverInfinity/`, `ccn/`, `mcquay/`, `aaphp/`, `aapup/`, `maxpro/`, `orion/`, `alarmOrion/`, `silk/`, `axvelocity/`, `BACnetFFTN4/` | investigable | open |
| LOW | U11 | **Video subsystem** — entire Tridium/OEM video stack, no block | `nvideo/`, `naxisVideo/`, `remoteVideo/`, `videoDriver/`, `videoMigrator/`, `baseRtsp/`, `xprotect/`, `maxpro/` | investigable (out of Honeywell-BMS mission) | open |
| LOW | U12 | **Tridium framework drivers not deep-distilled** — OPC-UA, Modbus framework, M-Bus, SNMP, oBIX, OpenADR, weather | `opcUaClient/Core/Server/`, `opc/`, `modbusCore/Async/Tcp*/Slave*/`, `mbus/`, `snmp*/`, `nSnmp/`, `obixDriver/`, `openAdr/`, `weather/`, `weatherUnderground/` | investigable (mostly out of mission) | open |
| LOW | U13 | **Data + service framework** — RDBMS integration, system DB, reporting, search, dashboard, virtual (B28 only touched virtual) | `rdb*/`, `systemDb/`, `orientSystemDb/`, `report/`, `search/`, `dashboard/`, `niagaraVirtual/` | investigable (out of mission) | open |
| LOW-MED | U14 | **Extended auth/identity** beyond B11/B30 RBAC+federation — SAML, OAuth2, LDAP, gauth, client-cert, e-signature | `saml/`, `samlEncryption/`, `oauth2/`, `ldap/`, `gauth/`, `clientCertAuth/`, `electronicSignature*/` | investigable (security-relevant) | open |
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

## Stop control (METHODOLOGY §8)

- **Open gaps — read-only investigable**: **15** (U1–U15). ← focus NOT stopped; PLANNED, 0 blocks written.
- **Open gaps — requires-execution**: **1** (B-1/G8 — shared with Spyder focus).
- **Open gaps — blocked (missing artifact)**: **1** (B-2/G5b — shared with Spyder focus).
- Recommended first block: **U1** `honIrmAppl` + `honIrmConfig` (closes the IRM/BEATS triad next to B105).
  Before authoring, run the PROMPT-LOOP e2 existence+size pre-flight (measured count over the real dir,
  METHODOLOGY §13 "measured count, never a hand-guess"; collapse duplicate procyon/vineflower trees).

**Resume condition**: open this focus by picking U1 (highest priority) and running the NORMAL CYCLE. This is
a PLANNED focus — do not re-bootstrap it; the backlog above is the seed.
