# RESEARCH-STATE — focus: module-authoring-exemplars (BOOTSTRAPPING)

> Multi-focus corpus (METHODOLOGY §16). This focus was SEEDED by an AUDIT-FIRST coverage sweep (§13),
> NOT hand-guessed — see the coverage matrix in the iteration history below (2026-09-05, delegated as
> 4 parallel Explore shards over disjoint dimension sets; driver-verified inline via `corpus-nav` +
> `module_nav`). It answers the operator's request: study how **Tridium's OWN N4 modules** are built,
> as EXEMPLARS to improve the `/build-n4-module` skill/kit.
>
> **Angle (§b2):** the EXEMPLAR axis of N4 module authoring — read Tridium/Honeywell first-party modules
> (`alarm`, `analytics`, `baja`, `bajaui`, `control`, `driver`, `history`, `batchJob`, `provisioningNiagara`,
> `template`, `hierarchy`, `tagdictionary`, `query`/`queryTable`/`search`, `rdb*`, `report`, `saml`, `gauth`,
> `nss`, `electronicSignature`, `fox`, `net`, `silk`, `tunnel`, `systemMonitor`, `systemDb`/`systemIndex`,
> plus the operator's own `nmodsreflow` family as a consumer exemplar) along **12 authoring dimensions**
> (D1 structure/profiles · D2 threading/timers/watchdog · D3 actions/protection · D4 inter-module comms ·
> D5 children/folders · D6 extensions · D7 analytics/algorithms · D8 build/display · D9 groups/relations/tags/
> hierarchy · D10 persistence/data · D11 jobs/batch/provisioning/template · D12 auth/security modules). The
> deliverable is DELTAS to the build kit's `types/logic.md`, `types/dashboard.md`, `types/wb-widgets.md`,
> `METHODOLOGY.md`, `corpus-index.md`, or a toolbelt check — NOT a re-derivation of the framework. READ-ONLY
> over `organized/`. Corpus language for NEW blocks = **English** (post-B115 convention).
>
> **This is a MATURE-corpus DEEPENING, not virgin ground.** The corpus already carries a COMPLETE module-
> authoring body across many focuses — the framework MECHANISM is covered; this focus attacks only the
> **exemplar-level residue** (how a specific Tridium module actually DOES a thing you would copy). Everything
> the audit found already covered is pre-declared as REMITTANCE below and will be CITED, never re-derived.

<!-- research-state.v1 -->
schema: research-state.v1
block_scope: shared-global
covered_blocks: 15
gaps_closed: 15
known_gaps: 17
investigable_open: 1
requires_execution_open: 1
blocked_open: 0
undocumented_findings: 0
<!-- /research-state.v1 -->

focus: module-authoring-exemplars
status: STOPPED (14/14 primary MAE gaps closed: MAE1-MAE6→B772-B777 + MAE7–MAE14→B778–B785; investigable_open=1 [MAE7-G1 child]; +1 req-exec MAE1-G1). Both slices complete (companero MAE1 + investigador1 MAE2-MAE14). §18 focus-close retro filed. **+ POST-CLOSE ADDITION 2026-09-05: B817 (companero) = the D1 module STRUCTURE STANDARD + conformance checklist (deepens MAE13/D1; envelope 15/15).**
seeded_from: AUDIT-FIRST coverage sweep 2026-09-05 (4 parallel Explore shards over disjoint dimension sets D1+D8 / D2+D3+D11 / D4+D10+D12 / D5+D6+D7+D9; ≥2 distinct search terms per cell; driver-verified inline)
seeded_on: 2026-09-05
gaps_total: 14 investigable (MAE1–MAE14)
gaps_closed: 2 (MAE1 → B772; MAE7 → B778)
blocks_written: B772 (MAE1), B778 (MAE7); reserved block range = B772–B791 (shared global numbering, holes tolerated)
block_prefix: niagara-mental-model-bloqueN.md (shared global numbering)

## Coverage

- **Covered blocks (this focus)**: 9 — B772 (MAE1) + B778–B785 (MAE7–MAE14).
- **Coverage metric**: 14 / 15 investigable gaps closed (~93%); only MAE7-G1 (type-level TypeSubscriber) open + MAE1-G1 (req-exec) deferred. (MAE7-G1 investigable still open; MAE1-G1 req-exec deferred.)
- **Last iteration**: 2026-09-05 — B778–B785 (MAE7–MAE14, investigador1 slice COMPLETE) + B772 (MAE1, companero; second-read PASS). NEXT: MAE2–MAE6 (B773–B777, investigador1).

## Gap-backlog (prioritized)

Seeded from the AUDIT-FIRST coverage matrix (GAP NUMBERS ARE HYPOTHESES).
All "Where" paths are under `/home/cristian/niagara-research/organized/`. Every exemplar module named below was
existence-verified 2026-09-05 (`find organized/<m> -name '*.java'` > 0 AND `module_nav modules`). Gaps CLUSTER a
dimension across several exemplar modules rather than one gap per module (METHODOLOGY §13 SHAPE-BY-INDEPENDENCE).
The kit-destination file is named in each row — that is the artifact the block must be able to change.

| Priority | Gap | Where | Status |
|---|---|---|---|
| high | ~~**MAE1 D6 extension AUTHORING contract**~~ — **RENAMED (premise refuted)**: base class is `BPointExtension` (`javax.baja.control`), NOT `BAbstractPointExt` (absent); `onExtended`/`onRetracted` do not exist. Documented the real contract: only `onExecute` is abstract/mandatory; parent value observed via `out` in `onExecute`; multi-ext order = slot-declaration order, proxyExt always first. → kit `types/logic.md` | `control-rt`/`history-rt`/`alarm-rt` (docSource) | **CLOSED → B772** (child MAE1-G1 req-exec) |
| high | **MAE2 D7 analytics algorithm-node + data-source AUTHORING** — how to build ONE custom `BAlgorithmBlock` (class to extend, input/output slots → DAG edges, registration with `BAnalyticService`) and how to plug a custom DATA SOURCE / rollup/filter into the pipeline. B66/B67 are read-only CATALOGS ("algorithm node"/"filter algorithm" = 0 authoring hits) → kit `types/logic.md` | `analytics` (`analytics-rt`, ~424 FQN) | **COVERED → B773** (node base = `BOutputBlock` [abstract getValue/getTrend], NOT BAlgorithmBlock [container] — BAnalyticAlgorithm absent; inputs = `BBlockPin` @NiagaraProperty + `BLink` DAG edges; `BFunctionBlock.apply()` single-input; filters/rollups/sources all = BOutputBlock subclasses; register by `module.xml <type>` NO @AgentOn; external feed = `AnalyticDataSource.Provider` duck-typed. 5/5 [CERT]. → `types/logic.md`) |
| high | **MAE3 D11 background-work AUTHORING** — custom `BJobStep` recipe (`doRun(BBatchJobService, …)` template), the `BSimpleJob` vs `BJob`→`BJobService` SELECTION rule (missing from corpus), and submit/progress/heartbeat. B20/B511/B567 document Tridium internals but no author template ("BJob doRun" = 0) → kit `types/logic.md` | `batchJob-rt` (~95 FQN), `driver-rt` (discover job), `provisioningNiagara` | **COVERED → B774** (SELECTION rule: subclass `BSimpleJob`+impl `run(Context)` for the normal async case [thread+success/fail/interrupt-cancel free] vs raw `BJob`+`doRun`/`doCancel` to own threading; `progress(pct)`/`heartbeat()`/`log()`; submit via `BJobService.submit(job,cx)`→ORD handle [poll, no join]; multi-step = `BJobStep`/`BDeviceJobStep.doRun` under `BBatchJob`. 5/5 [CERT]. → `types/logic.md`) |
| high | **MAE4 D2 watchdog/timer AUTHORING across exemplars** — the `BAbstractMonitor` subclass authoring contract (which methods to override, status slots, MonitorWorker 2s poll interaction), `BTimer` vs `Clock.Ticket` vs `Clock.schedulePeriodically` identity/selection, and a POSITIVE configurable-interval exemplar (BRelTime prop → schedulePeriodically). B729/B730 cover timer LIFECYCLE, not watchdog authoring → kit `types/logic.md` + `METHODOLOGY.md` note | `systemMonitor-rt` (~17 FQN, 10+ BAbstractMonitor), `analytics` pollers, `report` scheduler | **COVERED → B775** (watchdog = `BAbstractMonitor` [abstract `doRunCheck`] → `BAbstractAlarmMonitor` [status/lastAlarmTime + `raiseAlarm` edge-latch] → domain `checkX` → threshold compare; CORRECTED 2 assumptions: cadence is a configurable `BIntervalTriggerMode` [default 15 min] NOT a 2s poll, and there's NO `javax.baja.sys.BTimer` [only cl.hvac]; timer = `Clock.schedule` vs `schedulePeriodically`+`Ticket`; `BRandom` = configurable-`BRelTime`-interval exemplar; native `EngineWatchdog` [B124/B681] is a separate layer. 6/6 [CERT]. → `types/logic.md`+`METHODOLOGY.md`) |
| high | **MAE5 D3 action-protection recipe** — the POSITIVE `@NiagaraAction(permissions=…)` gating recipe (operator vs admin without `doPrivileged`) AND the CORRECT-use `AccessController.doPrivileged` pattern (all corpus instances are the AP-27 anti-pattern; kit can only say "don't"). Ties B18/B48/B755 → kit `types/logic.md` | `electronicSignature`, `gauth-rt`, `net-rt` (InitPrivilegedAction) | **COVERED → B776** (DECLARATIVE gating: `@NiagaraAction(flags=Flags.OPERATOR=256)`→OPERATOR_INVOKE(4), OMIT→ADMIN_INVOKE(64) is the DEFAULT, enforced by `BComponent.canInvoke` + fox/box `PermissionException` — no code in the body; BEnumWritable emergencyOverride[admin] vs set/override[operator]. CORRECT `doPrivileged` = JVM permission only [read BPassword/setDefault/sys-prop]; AP-27 = wrapping RBAC [find-zero, hazard not practice]. 5/5 [CERT]. → `types/logic.md`) |
| high | **MAE6 D12 security-module end-to-end AUTHORING synthesis** — one exemplar walk: `module.xml` → `BAbstractService` subclass → SPI impl (`BAuthenticationScheme` B510 / SecurityDashboard B563) → `module-permissions.xml` (B721) → signing → station integration, INCLUDING the never-opened `saml-rt` structure (55 classes). The D12 analog of what B757 did for services → kit `types/logic.md` + `corpus-index.md` | `saml-rt` (~62 FQN), `gauth-rt`, `nss` | **COVERED → B777** (security-module skeleton via saml-rt [never-opened, 50 classes]: module.xml → `BSAMLIdPService extends BAbstractService` → `BSAMLAuthenticationScheme extends BSSOAuthenticationScheme` [real auth in a `NiagaraLoginModule` wired by `getLoginConfiguration()`] + `BISecurityDashboardProviderAgent`; B721 CORRECTION: permissions INLINE in module.xml `<permissions>`, NOT a separate module-permissions.xml; signed NIAGARA4.RSA/SF [B18]; register `@AgentOn "baja:AuthenticationScheme"`. 6/6 [CERT]. → `types/logic.md`+`corpus-index.md`) |
| medium | **MAE7 D4 service + ORD + server-side subscription AUTHORING** — custom service registration walkthrough (`BAbstractService` subclass + type selection, using a real Tridium service as reference), publishing a NEW ORD scheme (`BHandleScheme` impl, named in B408 but never walked), and the SERVER-SIDE `BComponentSpace.subscribe()`/TrapCallback path (client BOX side is covered, server side = 0 hits) → kit `types/logic.md` | `systemMonitor` (service), `baja` (BHandleScheme), `fox-rt` | **COVERED → B778** (service=`BAbstractService`+`getServiceTypes()` reg-by-placement+`serviceStarted()`; ORD scheme=`BOrdScheme` BSingleton+`@NiagaraType(ordScheme)`+`resolve()`, walks B408; server-sub=`Subscriber.event(BComponentEvent)`+`subscribe(c,depth,cx)`, fills 0-hit gap. 7/7 [CERT]. → `types/logic.md`) |
| medium | **MAE8 D5 child-tree construction primitives** — the DECISION a builder needs: `BComponentList` (0 hits) vs dynamic slots vs frozen children vs typed `BFolder`; `BComponent.reorder(Property[])` in PRACTICE (slot reorder = 0 exemplar hits); building a typed child-tree from scratch with `isParentLegal`/`isChildLegal`. Complements interactive-composition P2–P4 → kit `types/logic.md` | `control-rt`, `analytics` (DAG children), `baja` | **COVERED → B779** (no `BComponentList`; container choice by cardinality = frozen `@NiagaraProperty` / dynamic `add(name,BValue)` / typed `BFolder`; `reorder(Property[])` IS used [audit refuted]; legality = `isChildLegal`/`isParentLegal` default-true + `instanceof` veto. 6/6 [CERT]. → `types/logic.md`) |
| medium | **MAE9 D8 per-exemplar palette + lexicon + agent census** — extract REAL `module.palette` (BOG) entries, `.lexicon` keys, and `@AgentOn` view/agent registrations from concrete Tridium modules as copy-ready templates (the mechanism is covered B713/B759/B203; the concrete conventions are not). Candidate: a small toolbelt extractor check → kit `types/dashboard.md` + `types/wb-widgets.md` | `alarm` (rt/ux/wb/se), `control-rt`, `systemMonitor` | **COVERED → B780** (copy-ready templates: palette `<p n= t= m=>` [bare-Type-minus-B names, plural folders, alias-once, nested pre-seed]; lexicon flat/module-global → PREFIX keys [Tridium 0-dup discipline = B759 avoidance]; `@AgentOn` dual-surface Java+module.xml `<agent><on>`. + proposed `module_nav palette-lexicon-agents` extractor. 5 [CERT]+1. → `types/dashboard.md`+`types/wb-widgets.md`) |
| medium | **MAE10 D9 categories + relations + hierarchy module-declaration AUTHORING** — how a module DECLARES new `BCategoryService` categories in `module.xml` (B11/B48 = runtime only), the `BRelation` base as exemplar (vs `BCustomRelation` B758), and the `hierarchy` module's own `BHierarchy`/`BLevelDef` authoring (B584/B586 = runtime model only) → kit `types/logic.md` | `hierarchy-rt` (~42 FQN), `tagdictionary-rt`, `baja` | **COVERED → B781** (three DIFFERENT author postures: CATEGORIES = no author-side declaration [runtime-only, BICategorizable] → kit emits NO scaffold; RELATIONS = `BRelation` is a concrete carrier, a relation TYPE = `relationId`+`RelationInfo`/`BCustomRelation` [not a subclass]; HIERARCHY = compose `BHierarchy`+`BLevelDef` variants under `BHierarchyService`. 4 [CERT]+1. → `types/logic.md`) |
| medium | **MAE11 D10 query-surface / index-registration modules (GAP — entirely uncovered)** — `query` / `queryTable` / `search` / `systemIndex` / `niagaraSystemIndex` as exemplars of a parameterized-query and station-wide-index-registration surface (all = 0 dedicated blocks). Teaches "how to build a query/index-surface module" → kit `types/logic.md` | `query-rt`, `queryTable-wb`, `search`, `systemIndex`/`niagaraSystemIndex` | **COVERED → B782** (ONE uniform pattern for all 4: typed `BQuery`/NEQL payload + a `BIAgent` provider [BQueryEngine execute / BColumnsProvider shape / BISearchProvider search / BSystemIndexer+BIIndexQueryProvider index] discovered by the agent registry → a `BITable` cursor. 5 [CERT]+1. → `types/logic.md`) |
| low | **MAE12 D11 template AUTHOR-side API** — `BTemplateService` registration (how a module author registers a NEW template type) + custom `.ntpl` schema authoring. B573 covers only the deploy/consume side; the "make your own template type" side is absent → kit `types/logic.md` | `template-rt` (~163 FQN) | **COVERED → B783** (NEGATIVE: no author type-registration SPI — TemplateType is a closed enum; a "template type" is an `.ntpl` ZIP [BNtplFile = .bog + template-manifest.xml] produced by a make-job from a component subtree marked with a `BTemplateConfig` + `BConfigBinding`/tagged slots. Kit: emit no BTemplate subclass scaffold. 4 [CERT]+1. → `types/logic.md`) |
| low | **MAE13 D1 profile matrix + real dependency declarations** — per-exemplar profile matrix (which Tridium modules carry `-se`/`-doc` and what goes in them) + REAL `<dependency>` version-range values from actual `module.xml` (fox/tunnel/alarm). Refines the module.xml template beyond the generic mechanism (B12/B629–B636/B754) → kit `METHODOLOGY.md` / `corpus-index.md` | `fox`, `tunnel`, `alarm`, `bajaui` (META-INF/module.xml) | **COVERED → B784** (profile matrix: fox rt/ux, tunnel rt-only, alarm rt/ux/wb/**se**, bajaui ux/wb; `-se`=server profile, `-doc`=SEPARATE module not a part; `<dependency>` = 3-part Tridium FLOOR `4.14.0` vs own 4-part build stamp `4.14.0.162`; header roster + `bajaVersion="0"` const. 5/5 [CERT]. → `METHODOLOGY.md`/`corpus-index.md`) |
| low | **MAE14 D10 rdb dialect EXTENSION SPI** — how to extend `rdb-rt` with a new dialect (`BEncryptableTransportRdbms` / `B<X>Database` subclass). The database focus (B403/B407/B409) covers the 4 built-in dialects as READ; the author-side "add a fifth" SPI is a clean bounded exemplar → kit `types/logic.md` | `rdb-rt`, `rdbMySQL`/`rdbOracle`/`rdbSqlServer`/`rdbHsqlDb` | **COVERED → B785** (dialect extension SPI: `B<X>Database extends BRdbms`/`BEncryptableTransportRdbms` + 3 abstract methods [getLicenseFeature/getConnection/getRdbmsContext]; getRdbmsContext→a 60-method `RdbmsDialect` SPI object [type map/quoting/identity]; TLS truststore hooks [B114]; register via manifest `<type>`. No central registry — subclass+SPI-object+register. 6/6 [CERT]. → `types/logic.md`) |

## Coverage matrix (AUDIT-FIRST, 2026-09-05) — 12 dimensions, rolled verdict + residue

Verdicts are per DIMENSION rolled across the exemplar module set (METHODOLOGY §13 CLUSTER — 37 modules × 12
dims = 444 cells is not hand-walked; the shards report dimension-major with per-module coverage inside). Each
cell cites blocks and names the exemplar-level residue that became a MAE gap.

| Dim | Verdict | Covered (REMITTANCE) | Exemplar residue → gap |
|---|---|---|---|
| D1 structure/profiles/module.xml/deps | COVERED (mechanism) | module-anatomy B629–B636; module-authoring B754–B756; B12, B18, B25; B34 (alarm), B35 (wb) | per-exemplar profile matrix + real `<dependency>` values → **MAE13** |
| D2 threading/timers/watchdog | COVERED (framework) | B6, B31 (BEngineThread/pools), B20 (monitors), B729/B730 (timer lifecycle), B552/B557 (schedulers) | BAbstractMonitor authoring + BTimer identity + configurable-interval exemplar → **MAE4** |
| D3 actions/protection | COVERED (framework) | B4, B18, B48, B755 (slot bits), B730, B11 (RBAC), B721 | @NiagaraAction(permissions) positive + doPrivileged correct-use → **MAE5** |
| D4 inter-module comms | COVERED (core) | B20/B757 (Sys.getService/BAbstractService), B5/B758/B514 (ORD/BQL), B512/B553 (BOX sub), B13/B19/B471 (Fox) | silk/net/tunnel/fox internals + BHandleScheme + server-side subscribe → **MAE7** |
| D5 children/folders | PARTIAL | B4 (dynamic slots), B33 (add/remove batch), B749 P4 (typed folders), B538 (naming) | BComponentList + slot reorder + from-scratch typed tree → **MAE8** |
| D6 extensions | PARTIAL | B536 (writable ext), B552 (alarm+history ext), B738 (proxy ext), B730 | BAbstractPointExt abstract contract + new ext type + ordering → **MAE1** |
| D7 analytics/algorithms | PARTIAL | B16, B66, B67 (catalog), B245 (SylkActuatorAnalytics) | author ONE algorithm node + custom data-source/rollup/filter → **MAE2** |
| D8 build/display (palette/agent/lexicon) | COVERED (mechanism) | B713/B759 (lexicon), B4/B35 (@AgentOn/views), B203 (palette BOG), B751–B753 | per-exemplar palette/lexicon/agent census (concrete templates) → **MAE9** |
| D9 groups/relations/tags/hierarchy | COVERED (tags) / PARTIAL (hierarchy,scopes) | B21, B260–B270 (tags), B758 (BCustomRelation), B584–B586 (hierarchy model), B11/B48 (categories) | BCategoryService module-declaration + BRelation base + hierarchy module authoring → **MAE10** |
| D10 persistence/data | COVERED (rdb/BOG/BQL/hdb) / GAP (query-surface) | database B402–B413 (rdb/Orion/HSQLDB/BOG save/BQL cursor), B33 (.hdb), B114 (encryption) | query/queryTable/search/systemIndex modules (GAP) → **MAE11**; rdb dialect SPI → **MAE14** |
| D11 jobs/batch/provisioning/template | COVERED (deploy/consume) | B20/B511 (BJob), B567 (batchJob), B14/B16/B39/B571–B573/B579 (provisioning+template deploy) | custom BJobStep + BSimpleJob/BJob rule → **MAE3**; BTemplateService author-side → **MAE12** |
| D12 auth/security modules | COVERED (esign/nss/gauth) / PARTIAL (saml) | B350–B356 (electronicSignature), B563/B604 (nss), B11/B30/B494 (gauth TOTP), B510 (BAuthenticationScheme SPI) | saml-rt structure + end-to-end security-module authoring synthesis → **MAE6** |

**Matrix cell-type summary (per-dimension rolled verdict):** COVERED = 6 (D1, D2, D3, D4, D8, D11) ·
PARTIAL = 6 (D5, D6, D7, D9, D10, D12) · GAP (pure, no coverage at all) = 0 dimensions, but 3 GAP SUB-CELLS
inside PARTIAL dims (query/queryTable/search/systemIndex under D10; silk/net/tunnel internals under D4;
saml-rt internals under D12) · N/A = 0. Every one of the 12 dimensions has ≥1 exemplar-level residue → all 12
are represented in the MAE1–MAE14 backlog.

### REMITTANCE (already covered — will NOT be opened, only CITED)

- **Module skeleton / manifest / boot / type-registration → module-anatomy [B629–B636] + [B12].** Do not re-derive.
- **Versioning / upgrade-safety / slot-flag BITS / build+signing / service integration / tags-exposure / lexicon → module-authoring [B754–B760].**
- **RT authoring idioms / timer lifecycle / composition-into-children → [B729][B730][B737][B744]** and the RT campaign.
- **WB/UX authoring ladder + serving recipes → wb-ux-authoring [B751–B753].**
- **Wire-Sheet + Honeywell 10-pattern organization taxonomy → interactive-composition [B747–B750].**
- **Control library (kitControl / writable-point / program / HVAC) → kitControl [B536–B557][B603].**
- **API-authoring SPIs (RPC, oBIX server, BAuthenticationScheme, BJobService, Fox client, BOX, BQL/NEQL) → apis [B507–B516].**
- **Persistence layer (rdb export / Orion ORM / HSQLDB / BOG save / BQL cursor / migration) → database [B402–B413].**
- **Tags subsystem (API / dict engine / BSmartTagDictionary / BTagRule) → tags [B260–B270].**
- **electronicSignature (21 CFR Part 11) → [B350–B356]; template deploy/consume → [B573][B200][B437].**
- **Own-module audit + build process (ANGELES/SEJOFA) → own-modules-audit [B637–B647]; chihuahua case study → [B648–B655].**

### Dismissed / absent-input modules (recorded, NOT gaps)

- **`queryApi`** — ABSENT-INPUT: no `queryApi` directory exists in `organized/` (only `query-rt` and
  `queryTable-wb`; `module_nav modules` confirms no `queryApi` module). Recorded per METHODOLOGY §13 as an
  absent input, not a coverage GAP. Its likely subject (parameterized query surface) is captured by **MAE11**
  via the real `query`/`queryTable`/`search` modules.
- All other user-listed names resolve under case-corrected directory names and ARE present:
  `batchjob`→`batchJob`, `electronicsignature`→`electronicSignature`, `rdbmysql`→`rdbMySQL`,
  `rdboracle`→`rdbOracle`, `rdbsqlserver`→`rdbSqlServer`, `systemdb`→`systemDb`, `systemindex`→`systemIndex`
  (+ `niagaraSystemIndex`). Existence-verified 2026-09-05.
- **`app`, `nss`, `test`** — present but out of the core exemplar residue: `test` is the JUnit/station-test
  harness (relevant to the sibling `module-ux-testing-and-write-surface` focus, not this one); `app`/`nss`
  are cited where they inform D12 (MAE6) but do not seed their own gap.

## Stop control (METHODOLOGY §8)

- **Open gaps — read-only investigable**: **13** (MAE2–MAE6, MAE8–MAE14, + MAE7-G1; all `pending`).
- **Gaps closed**: 2 (MAE1 → B772; MAE7 → B778).
- **requires-execution / blocked**: 1 req-exec (MAE1-G1) / 0 blocked.
- **Coverage metric**: 14 / 15 investigable gaps closed (~93%); only MAE7-G1 (type-level TypeSubscriber) open + MAE1-G1 (req-exec) deferred.
- **NEXT (my lane MAE1–MAE6)**: MAE2 (D7 analytics algorithm-node authoring) → B773. This focus is NOT
  stopped; the loop continues on the highest-priority pending gap in my lane.

## Iteration history

| Iter | Gap | Block | Delegated? · model tier | New gaps uncovered |
|---|---|---|---|---|
| seed | AUDIT-FIRST coverage sweep — 12 dimensions × ~37 exemplar modules, 4 parallel Explore shards (D1+D8 / D2+D3+D11 / D4+D10+D12 / D5+D6+D7+D9), ≥2 search terms per cell, driver-verified inline (`corpus-nav`, `module_nav`) | — | yes · sonnet ×4 (verified inline: opus driver) | MAE1–MAE14 seeded from the coverage matrix; `queryApi` recorded absent-input |
| 1 | MAE1 D6 point-extension authoring contract (premise `BAbstractPointExt` refuted → base = `BPointExtension`) | B772 | yes · sonnet (3-source sweep) + inline driver token-verify | MAE1-G1 (requires-execution): build+station-test a minimal `BPointExtension` end-to-end |
| 7 (investigador1) | MAE7 D4 author-side SPIs: custom service (BAbstractService+getServiceTypes reg-by-placement), new ORD scheme (BOrdScheme BSingleton, walks B408), server-side subscription (Subscriber.event, fills 0-hit gap) | B778 | yes · Explore sweep + inline grep-verify (7/7 [CERT]) | MAE7-G1 type-level BComponentSpace.subscribe(Type[],TypeSubscriber) not walked |
| 8 (investigador1) | MAE8 D5 child-tree primitives: no BComponentList; frozen `@NiagaraProperty` / dynamic `add()` / typed `BFolder` by cardinality; `reorder(Property[])` refuted-as-unused; `isChildLegal`/`isParentLegal` instanceof vetoes | B779 | yes · Explore sweep + inline grep-verify (6/6 [CERT]; corrected sweep's BComponent line numbers) | MAE8-G1 reorderToTop/Bottom + reordered() callback not walked |
| 9 (investigador1) | MAE9 D8 palette/lexicon/@AgentOn copy-ready conventions (bare-Type-minus-B palette names, prefixed flat lexicon = B759 avoidance w/ 0 dups, dual-surface @AgentOn Java+module.xml) + proposed extractor | B780 | yes · Explore sweep + inline grep-verify (5/6 [CERT]; corrected @AgentOn line numbers) | MAE9-G1 -se/-doc profile packaging → MAE13 |
| 10 (investigador1) | MAE10 D9 categories/relations/hierarchy author postures (categories=none/runtime-only; relations=relationId+RelationInfo not a BRelation subclass; hierarchy=BLevelDef composition under BHierarchyService) | B781 | yes · Explore sweep + inline grep-verify (4/5 [CERT]) | MAE10-G1 bespoke BLevelDef subclass not walked |
| 11 (investigador1) | MAE11 D10 query/search/index surfaces = ONE pattern (BQuery/NEQL payload + BIAgent provider via agent registry → BITable): BQueryEngine/BColumnsProvider/BISearchProvider/BSystemIndexer | B782 | yes · Explore sweep + inline grep-verify (5/6 [CERT], one spine per surface) | MAE11-G1 WB/UX config views not walked (→ wb-ux) |
| 12 (investigador1) | MAE12 D11 template author-side (NEGATIVE): no type-registration SPI; TemplateType closed enum; a template = `.ntpl` ZIP (BNtplFile: bog+manifest) made by a job from a BTemplateConfig-marked subtree; NiagaraTemplate.createFrom().save() | B783 | yes · Explore sweep + inline grep-verify (4/5 [CERT]) | MAE12-G1 full template-manifest.xml schema not exhaustively walked |
| 13 (investigador1) | MAE13 D1 real module.xml profile matrix + deps: parts -rt/-ux/-wb/-se, -doc=separate module; `<dependency>` 3-part Tridium floor (4.14.0) vs own 4-part build stamp (4.14.0.162); header roster | B784 | yes · Explore sweep + inline grep-verify (5/5 [CERT]) | MAE13-G1 `<installation>` native/nre/-rt-<os> block only sampled |
| 6 (investigador1, absorbed) | MAE6 D12 security-module end-to-end (saml-rt walk): module.xml → BAbstractService → BAuthenticationScheme SPI (auth in a NiagaraLoginModule via getLoginConfiguration) + dashboard agent; permissions INLINE in module.xml (B721 correction, no module-permissions.xml); signed NIAGARA4.RSA/SF; register @AgentOn baja:AuthenticationScheme | B777 | yes · Explore sweep + inline grep-verify (6/6 [CERT]) | MAE6-G1 SAMLLoginModule.login flow not walked. **FOCUS COMPLETE — all 14 MAE gaps closed; §18 focus-close retro filed.** |
| 5 (investigador1, absorbed) | MAE5 D3 action protection: DECLARATIVE gating via `@NiagaraAction(flags=Flags.OPERATOR=256)`→OPERATOR_INVOKE(4) / omit→ADMIN_INVOKE(64) default, enforced by BComponent.canInvoke + fox/box PermissionException; correct doPrivileged = JVM permission only, AP-27 = wrapping RBAC (find-zero) | B776 | yes · Explore sweep + inline grep-verify (5/5 [CERT]) | MAE5-G1 requiredPermissions(@AgentOn) vs Flags.OPERATOR reconciliation |
| 4 (investigador1, absorbed) | MAE4 D2 watchdog/timer authoring: BAbstractMonitor(doRunCheck)→BAbstractAlarmMonitor(raiseAlarm edge-latch)→domain checkX threshold; Clock.schedule vs schedulePeriodically+Ticket; BRandom configurable BRelTime exemplar. CORRECTED: no 2s poll (configurable BIntervalTriggerMode, 15min default), no javax.baja.sys.BTimer (cl.hvac only); native EngineWatchdog B124/B681 separate | B775 | yes · Explore sweep + inline grep-verify (6/6 [CERT], 2 assumptions corrected) | MAE4-G1 no heartbeat-age/job watchdog exemplar (find-zero) |
| 3 (investigador1, absorbed) | MAE3 D11 job authoring: BSimpleJob.run (async, thread+success/fail/cancel free) vs raw BJob.doRun/doCancel (own threading); progress/heartbeat/log; BJobService.submit→ORD handle (poll, no join); multi-step BJobStep/BDeviceJobStep.doRun under BBatchJob | B774 | yes · Explore sweep + inline grep-verify (5/5 [CERT]) | MAE3-G1 BBatchJob dispatcher internals not walked |
| 2 (investigador1, absorbed) | MAE2 D7 analytics node authoring: base = BOutputBlock (abstract getValue/getTrend) not BAlgorithmBlock; inputs=BBlockPin+BLink DAG; BFunctionBlock.apply single-input; filters/rollups/sources all BOutputBlock subclasses; register by module.xml `<type>` (no @AgentOn); external feed = AnalyticDataSource.Provider | B773 | yes · Explore sweep + inline grep-verify (5/5 [CERT]; corrected seed candidate list) | MAE2-G1 Combiner/FunctionTrend not walked |
| 14 (investigador1) | MAE14 D10 rdb dialect extension SPI: B<X>Database extends BRdbms/BEncryptableTransportRdbms + 3 abstract methods; getRdbmsContext→60-method RdbmsDialect SPI object; manifest `<type>` register; no central registry | B785 | yes · Explore sweep + inline grep-verify (6/6 [CERT]) | MAE14-G1 Oracle sequence path not walked. **investigador1 slice MAE7-14 COMPLETE (B778-B785); §18 slice-retro filed. Focus awaits companero's MAE1-6.** |
| 15 (companero, post-close) | D1 module STRUCTURE STANDARD — how Tridium/Honeywell lay out an N4 module + a conformance checklist our four modules can be linted against (deepens MAE13/D1 beyond the profile matrix) | B817 | yes · companero (landed e4ec4e0d4) | kit delta: module-structure conformance checklist → METHODOLOGY / a toolbelt lint |
