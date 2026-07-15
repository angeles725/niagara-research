<!-- review-status: applied 2026-07-15 · kit fe02d16 -->
<!-- §14 applied: claim #22 attribution correction → niagara-mental-model-bloque81.md (EncryptDecrypt.encrypt, not KitpxUtils). 0 REFUTED / 0 DOWNGRADED (no error fixes). The 13 [CERT-a]→[CERT] escalations are OPTIONAL confidence-upgrades (audit §"Corrections to apply"); recorded authoritatively in this report, NOT rewritten inline this pass. -->
# Audit — Bloque 90 (honPlantController) + Bloque 81 (easy* Galileo) · Research-SDD certainty audit

> What was audited: `niagara-mental-model-bloque90.md` (BTP IPC / managed switch / JNI plantctrl /
> EagleHawk→PanelBus migrators) and `niagara-mental-model-bloque81.md` (easy* productivity suite).
> Sources re-verified against: the **vineflower** decompiled tree the blocks cite, which lives OUTSIDE
> the corpus at `/home/cristian/modules/Prototipos/modulos/organized/{honPlantController,easy*}/…-rt/vineflower/…`.
> Method: re-read the cited constant/declaration per claim in the exact decompiler variant (vineflower),
> prioritizing every `[CERT-a]` + a spot-check of `[CERT]`. READ-ONLY on the audited corpus (this report
> is the only thing written). Verdicts: ESCALATED · CONFIRMED · DOWNGRADED · REFUTED (METHODOLOGY §13).
>
> First-ever standalone run of §13 audit mode (no prior `audits/` dir existed on any target). This is the
> inaugural data point; the meta-observations at the end feed the kit retrospective.

## Audited claims

| # | Claim (short) | Original marker | Verdict | Real evidence (`file:line`) or why refuted/downgraded |
|---|---|---|---|---|
| 1 | `BHonPlantControllerService extends BAbstractService implements BIBTPPanelBusHandler` | `[CERT]` | CONFIRMED | `honplantcontroller/service/BHonPlantControllerService.java:95` — verbatim |
| 2 | Host gate: only runs "on Beats Adv" via `os.name`; Windows→offline, skips JNI | `[CERT]` | CONFIRMED | `service/BHonPlantControllerService.java:327-334` `isOnBeatsAdvController()` (`os.equals("LINUX")`) + `network/util/PlantCtrlCommon.java:50` `getProperty("os.name")` |
| 3 | Service `@NiagaraProperty`s: RSTPConfiguration, SwitchPortConfiguration, HMI (hidden), BTPConnectionListener (hidden), BTPDevice (hidden) | `[CERT-a]` | **ESCALATED** | `service/BHonPlantControllerService.java:96-100` — `RSTPConfiguration=newProperty(0,new BRSTPConfiguration())`, `SwitchPortConfiguration=…`, `HonPlantControllerHMI=newProperty(4,…)`, `BTPConnectionListener=newProperty(4,…)` (flag 4 = hidden) |
| 4 | BTP/switch ports: `SWITCH_PORT_SOCKET_PORT=10000`, `BTP_CONNECTION_SOCKET_PORT=11000` | `[CERT]` | CONFIRMED | `network/util/PlantCtrlCommon.java:13-14` — verbatim |
| 5 | Frame preamble `PREAMBLE_DATA = {85,-1}` → `0x55 0xFF` | `[CERT]` | CONFIRMED | `network/btp/comm/SocketPayloadMessageStructcure.java:12` — verbatim |
| 6 | `CMD_ID`: `0x80` request · `0x81` heartbeat · `0x82` error | `[CERT-a]` | **ESCALATED** | `network/btp/comm/BTPServer.java:38-40` — `CMD_ID_BTP_REQUEST=-128`(0x80), `CMD_ID_HEARTBEAT=-127`(0x81), `CMD_ID_BTP_ERROR_INFO=-126`(0x82) |
| 7 | reqId table 6/14/16/18/264 → respId 774/782/784/786/256 | `[CERT-a]` | **ESCALATED** | `btp/BTPConstants.java:23-28,77,94-95` (`FILE_DATA_READ_REQUEST=6`,`READ_REQUEST_ID=14`,`WRITE_PROPERTY_REQUEST=16`,`QUERY_REQUEST=18`,`DEVICE_DISCOVERY_REQID=264`; resp `774/782/784/786/256`) + used in `btp/comm/parser/BTPRequestHandler.java:28,36,52` |
| 8 | BTP object model: Device(8), IO Device Data(136), Terminal(137) | `[CERT-a]` | **ESCALATED** | `btp/BTPObjectTypes.java:5-6` (`BTP_IO_DEVICE_DATA=136`,`BTP_TERMINAL=137`) + `btp/BBTPDeviceObject.java:264-265,374` ("Protocol Object Types Supported: [8,136,137]") |
| 9 | Proprietary properties from 5000 (`5005 IO_TERMINAL_LIST`, `5012 TERMINAL_OVERRIDE_TIMER`) | `[CERT-a]` | **ESCALATED** | `btp/BTPPropertyTypes.java:34` (`IO_TERMINAL_LIST=5005`), `:41` (`TERMINAL_OVERRIDE_TIMER=5012`) |
| 10 | WriteProperty restricted: only Terminal(137) writable, else `WRITE_ACCESS_DENIED` | `[CERT-a]` | **ESCALATED** | `btp/BBTPTerminal.java:312,324,382,395,446` return `WRITE_ACCESS_DENIED`; enforced in `btp/comm/parser/BTPWritePropertyHandler.java:91,179,185` |
| 11 | BTP tree populated by BQL `select * from clPanelBus:PanelbusNetwork` | `[CERT-a]` | **ESCALATED** | `service/BHonPlantControllerService.java:362` — `BOrd.make("slot:/Drivers\|bql:select * from clPanelBus:PanelbusNetwork")` verbatim |
| 12 | `BRSTPConfiguration extends BComponent` (`network/rstp/…:168`) | `[CERT]` | CONFIRMED | `network/rstp/BRSTPConfiguration.java:168` — verbatim |
| 13 | `BSwitchPortConfiguration extends BComponent` (`network/switchport/…:81`) | `[CERT]` | CONFIRMED | `network/switchport/BSwitchPortConfiguration.java:81` — verbatim |
| 14 | RSTP `bridgePriority` default 49152, range → 61440 (step 4096) | `[CERT-a]` | **ESCALATED** | `network/rstp/BBridgePriorityEnum.java:61` `defaultValue="p49152"`, `:76` `P_49152=49152`, `:58-59` `p61440/61440` |
| 15 | JNI `System.loadLibrary("plantctrl")` (`comm/JNIRequest.java:20`) | `[CERT]` | CONFIRMED | `com/honeywell/comm/JNIRequest.java:20` — verbatim (inside `AccessController.doPrivileged`) |
| 16 | Offline migration reads `config.bog` (`BOG_FILE`) | `[CERT]` | CONFIRMED | `honPlantControllerMigrator …/model/Const.java:59` — `BOG_FILE="config.bog"` verbatim |
| 17 | `StationType` = 4 values (EAGLEHAWK/BEATS_ADVANCED/…_WITH_HMI_PRIVATE/GENERIC) | `[CERT]` | CONFIRMED | `honPlantControllerMigrator/enums/StationType.java:4-7` — verbatim; EAGLEHAWK display `"EHN4/Ciper50/CP-NX"` matches |
| 18 | **[SEC-1]** `new ServerSocket(serverAddr.getPort())` discards the `127.0.0.1` `InetAddress` → binds `0.0.0.0` | `[CERT]` | CONFIRMED | `network/btp/comm/BTPServer.java:176` (vineflower) — `this.socket = new ServerSocket(serverAddr.getPort());`; `serverAddr=new InetSocketAddress(hostname,port)` built two lines up, address dropped. Java `ServerSocket(int)` binds wildcard → finding holds |
| 19 | **[SEC-3]** Platform detection only by `os.name == LINUX` | `[CERT-a]` | **ESCALATED** | `service/BHonPlantControllerService.java:331` `if (os.equals("LINUX")) ret=true;` — any Linux Niagara self-qualifies as controller |
| 20 | Four `easy*` root classes at their cited lines | `[CERT]` | CONFIRMED | `BEasyTemplatingService.java:30`, `easybinding/service/BEasyBindingSupportService.java:33`, `easyhealthybuilding/service/BEasyHealthyBuildingService.java:79`, `point/data/manager/services/BEasyDatabaseManagerService.java:48` — class decls verbatim (extends on continuation line, as the block's method notes) |
| 21 | `BEasyBaseBinding extends BSecureBoundLabelBinding` — non-public Tridium **Professional Services** dep (`com.tridiumx.ps.util`) | `[CERT-a]` | **ESCALATED** | `easyBinding-wb/…/BEasyBaseBinding.java:4` `import com.tridiumx.ps.util.BSecureBoundLabelBinding;` + `:18` class decl |
| 22 | Image encryption AES-128, key derived from feature name `honEasyBinding`, end marker `{0x7F,0x7F}` | `[CERT-a]` | **ESCALATED** | `easybinding/util/EncryptDecrypt.java:48,54` (`SecretKeySpec(…,"AES")`, `Cipher.getInstance("AES")`), `:61` `new byte[]{127,127}` marker; key source `easybinding/util/KitpxUtils.java:38` `"honEasyBinding".getBytes()`. NOTE: literal `encrypt()` lives in `EncryptDecrypt`, not `KitpxUtils.encrypt` as the block phrased it (KitpxUtils orchestrates + owns the key) — attribution smudge, substance holds |
| 23 | Multi-OEM brand licensing (same mechanism as BGalileoService) | `[CERT-a]` | **ESCALATED** | `easybinding/util/EbLicenseUtil.java:48-63` — `getFeature` for `SaiaBurgessControls`, `Trend_Control_Systems_Ltd`, `Honeywell`, `HoneywellCentraLine`, `Alerton` on feature `honEasyBinding` |
| 24 | Systematic typo `"HISOTRY"` (for HISTORY) in constants | `[CERT-a]` | **ESCALATED** | present in `point/data/manager/common/PointDatabaseManagerConstants.java` (vineflower) |

## Metrics

- **Claims audited**: 24
- **ESCALATED**: 13  ·  **CONFIRMED**: 11  ·  **DOWNGRADED**: 0  ·  **REFUTED**: 0

## Honest verdict

The engine extracted **more certainty**, not error-catching — a clean sweep. All 13 audited `[CERT-a]`
(sub-agent-hedged) claims escalated to source-confirmed `[CERT]` with concrete `file:line`, and all 11
spot-checked `[CERT]` claims held verbatim. **Zero downgrades, zero refutations** across two dense blocks.

The audited author (a multi-agent Explore run) had **no fabrications** in this set, and the `[CERT-a]`
hedging was appropriately conservative: the marks flagged "not re-verified by me," not "shaky" — every one
was in fact correct in the code. The audit's value here is therefore pure **confidence-raising**: 25 claims'
worth of BTP protocol constants, object model, port bindings, and the SEC-1 wildcard-bind finding now rest
on cited primary source instead of a sub-agent's word.

Two honesty caveats worth recording:
1. The PROMPT-AUDIT residual-risk pattern ("imprecise attribution — right behavior, wrong owning class")
   appeared exactly **once, mildly** (claim #22: `KitpxUtils.encrypt` — the literal `encrypt()` is in
   `EncryptDecrypt.java`, driven by `KitpxUtils` which owns the key). Not enough to refute; the AES,
   key-source, and marker facts all confirmed. This is the same failure mode the kit's B100 note predicted.
2. I nearly issued a **false REFUTED** on claim #6 (the `0x82` error CMD_ID): my first grep missed
   `CMD_ID_BTP_ERROR_INFO=-126` at `BTPServer.java:40` and I saw error frames reusing `-128` elsewhere. A
   careful second read found the dedicated constant. This validates the §13 rule "if you can't reach it,
   DOWNGRADED — never rush to REFUTED": the most dangerous audit error is a confident false refutation.

## Corrections to apply (optional, METHODOLOGY §14)

- **B81.3, claim #22** — sharpen attribution: change "AES-128 (`KitpxUtils.encrypt`…)" to
  "AES (`EncryptDecrypt.encrypt()` at `easybinding/util/EncryptDecrypt.java:54`; key from feature name
  `honEasyBinding` in `KitpxUtils.java:38`; end marker `{0x7F,0x7F}` at `EncryptDecrypt.java:61`)".
  Transparent note: "corrected per audit — owning class of the cipher call is EncryptDecrypt, not KitpxUtils".
- No other corrections: nothing was refuted or downgraded. The blocks' `[CERT]`/`[CERT-a]` split was
  honest; the escalations can be applied in-place (13 `[CERT-a]` → `[CERT]` with the `file:line` above) if a
  §14 pass is run, but none are error fixes — they are confidence upgrades.
