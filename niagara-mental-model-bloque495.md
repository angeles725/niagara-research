# Block 495 — `oem-honeywell-tail` U10: breadth survey of the other-vendor OEM drivers (Andover, Carrier CCN, McQuay, American AutoMatrix, MAXPRO, BACnetFFT) — license-feature map, two scope corrections, and the driver-level security standouts (McQuay hardcoded password, MAXPRO RTSP, axvelocity SSTI, BACnetFFT ungated firmware push)

> **Focus:** `oem-honeywell-tail`, gap U10 — other-vendor OEM drivers not yet distilled. Breadth survey (one
> profile per family), not deep distillation. READ-ONLY, decompiled source; no binary run. Markers §3.
> All modules are `vendor="Tridium"` (Tridium is the OEM publisher); the *protocol* vendor differs per module.
> Pre-flight PRESENT (11): `andoverAC256`(165), `andoverInfinity`(177), `ccn`(459), `mcquay`(108), `aaphp`(315),
> `aapup`(231), `maxpro`(111), `orion`(477), `alarmOrion`(99), `silk`(39), `axvelocity`(729), `BACnetFFTN4`(63).

## §495.1 — License-feature table `[CERT]`

| Module | `(vendor, feature)` | Gate site |
|---|---|---|
| andoverAC256 | `tridium:andoverAC256` | `BAndoverNetwork.java:152` |
| andoverInfinity | `tridium:andoverInfinity` | `BInfinityNetwork.java:287` |
| ccn | `tridium:ccn` → fallback `tridium:ccnl` (CCN-lite) | `BCcnNetwork.java:551,557` |
| mcquay | `tridium:mcquay` | `BMcQuayNetwork.java:153` |
| aaphp | `tridium:aaphp` | `BAaPhpNetwork.java:117` |
| aapup | `tridium:aapup` | `BPupNetwork.java:198` |
| maxpro | `tridium:remoteVideo` \| `tridium:maxpro` (by parent) | `BMaxproNetwork.java:82-85` |
| axvelocity | `tridium:axvelocity` | `BVelocityServlet.java:173` |
| orion / alarmOrion / silk / BACnetFFTN4 | **ungated** `[CERT negative]` | — |

## §495.2 — Vendor-family classification `[CERT]`

- **Honeywell family:** `maxpro` (Honeywell MAXPRO NVR video), `BACnetFFTN4` (`com.honeywell.bacnet` firmware/trend).
- **Genuinely other-vendor building/HVAC protocols:** `andoverAC256`+`andoverInfinity` (Andover Controls →
  Schneider), `ccn` (Carrier Comfort Network), `mcquay` (McQuay → Daikin Applied), `aaphp`+`aapup` (American
  AutoMatrix → Cylon/ABB). All are `BSerialNetwork`/`BBasicNetwork`-based device drivers.
- **NOT vendor drivers — Tridium/Apache infra (mis-scoped in the gap list):** `orion`+`alarmOrion` (Tridium Orion
  RDB/archiving framework, `BOrionService` restricted+security-dashboard), `silk` (Tridium SOAP web-services
  toolkit), `axvelocity` (Apache Velocity template engine).

## §495.3 — Two scope corrections `[CERT]`

1. **`axvelocity` = Apache Velocity template engine** (663 `.java` under `org/apache/velocity` + shaded
   commons-io, exposed via `BVelocityServlet`/`BVelocityPxView`), **NOT** the Andover Continuum "Velocity"
   access-control product implied by the gap list.
2. **`silk` = SOAP Web-Services toolkit** (`SoapClient`/`BSoapServlet`/`Wsdl`), **NOT** a Sylk/S-Bus actuator
   bus driver. (`SylkActuatorAnalytics` is a separate analytics module, out of this survey.)

## §495.4 — Absent `[CERT negative]`

- **`ccnl`** — not a module; only a fallback license *feature* inside `ccn` (CCN-lite tier).
- **`sylk`** — absent (a Sylk actuator driver is not installed).
- **`dedMicrosDvr`** — absent.

## §495.5 — Driver-level security standouts `[CERT]`

- **mcquay — hardcoded credential material:** fallback site password `"FFFFFFFF"` when the configured password
  isn't 8 hex chars (`BMcQuayNetwork.java:159`, again on read-fail `:186`) + a fixed protocol access code
  `"86672775"` in read requests (`:167-173`). Weak/known default embedded in the driver.
- **maxpro — RTSP posture self-report:** HTTP/RTSP-based; `getSecurityDashboardItems` (`BMaxproNetwork.java:95-120`)
  flags unencrypted RTSP camera streams to the Niagara Security Dashboard (`BISecurityDashboardProvider`).
- **axvelocity — SSTI + supply-chain surface:** a full server-side template engine reachable through a web
  servlet (template-injection class) that bundles a complete old Apache Velocity (known-CVE exposure). License
  gate enforced per web-op before render.
- **BACnetFFTN4 — ungated firmware push:** firmware-download jobs to Honeywell field controllers
  (`download/file`, `download/ops`), **no license feature** (rides the base `bacnet` license); risk surface is
  device firmware update. No embedded credential found.
- **ccn — unauthenticated inbound path:** `comm/BCcnUnsolicitedReceive.java` accepts inbound alarm messages off
  the bus with no protocol auth (inherent to CCN).
- Andover/AAM drivers: clean serial framing (Andover Infinity uses VT100 screen-scrape comms); AAM has serial
  file send/receive (firmware/config over serial, protocol-inherent, no crypto). No embedded credentials.

## §495.6 — Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|---|---|---|---|
| 1 | 11 present modules with license features per table; orion/alarmOrion/silk/BACnetFFTN4 ungated | `[CERT]`/`[CERT neg]` | gate sites in §495.1 | PASS |
| 2 | Honeywell (maxpro, BACnetFFTN4) vs other-vendor (Andover/ccn/mcquay/aaphp/aapup) vs infra (orion/silk/axvelocity) | `[CERT]` | module.xml + package roots | PASS |
| 3 | axvelocity = Apache Velocity (not Andover); silk = SOAP (not Sylk) | `[CERT]` | `org/apache/velocity`; `com/tridium/silk` | PASS |
| 4 | mcquay hardcoded pw "FFFFFFFF" + access code "86672775" | `[CERT]` | `BMcQuayNetwork.java:159,167-173,186` | PASS |
| 5 | maxpro flags RTSP to Security Dashboard; BACnetFFTN4 firmware push ungated | `[CERT]` | `BMaxproNetwork.java:95-120`; BACnetFFTN4 grep-neg | PASS |
| 6 | ccnl/sylk/dedMicrosDvr absent | `[CERT negative]` | pre-flight | PASS |

**Tally:** 6 claims, all `[CERT]`/`[CERT negative]`, 0 `[INFER]`.

## §495.7 — Connections & focus status

- Advances `oem-honeywell-tail` (U10). Security feed to [B490]: mcquay hardcoded credential + BACnetFFTN4
  ungated firmware push + axvelocity SSTI are driver-level analogues of the licensing/trust weaknesses.
- **oem-tail in-mission investigable set now covered:** U1-U9 ([B242-B250]), U1b/U1c ([B493]), U14 ([B494]),
  U10 ([B495]). Remaining U11-U13/U15 are LOW / out-of-mission (video, framework drivers [U12 SNMP done B476],
  data/service, doc-corpus) — not code-distillation targets.
