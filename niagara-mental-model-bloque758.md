# B758 · Semantic tags/relations authoring + northbound data exposure (oBIX/Fox/BOX/REST) + BQL from code — the module recipes

> **Scope**: how a module (a) tags/relates its components semantically and (b) exposes its data to the outside
> — the two things beyond raw slots that make a module queryable and integrable. Foco: **module-authoring** (MA5).
>
> **Sources**: FUENTE 3 — `honTagDictionary-rt` (`BHonTagDictionary`, `BIsPointProxyTypeRule`,
> `BEquipmentTypeTag`, `BCustomRelation`), `tagdictionary-rt/BSmartTagDictionary`, `haystack-rt`
> (`BContainmentRelation`/`BEquipRelation`/`BHsTagDictionary`), `obixDriver-rt` (`BObixServer`,
> `BObixOrdScheme`, `BControlPointAgent`, `BStationLobbyAgent`, `BBqlLobbyAgent`), `fox-rt/BFoxService`,
> `box-rt` (`BBoxServlet`, `QueryServlet`), `analytics-rt` (BQL-from-code), `neql-rt`/`bql-rt`. All file:line.
> FUENTE 1 — B5 (ORD/BQL/NEQL/tags/hierarchy), B749 P9 (semantic overlay), B727/B728 (oBIX server), B752 (our REST servlet).

---

## 758.1 — Ship a tag dictionary `[CERT]`
Subclass `BSmartTagDictionary`, set a `namespace` prop (the tag prefix, e.g. `"hon"`, `"hs"`), and seed tag
definitions in the constructor: for each tag name `tagInfoList.add(SlotPath.escape(name), new
BSimpleTagInfo(BMarker.DEFAULT))` for a MARKER tag, or `new BSimpleTagInfo(BString.DEFAULT)` for a VALUE tag,
guarded by a `get(...)==null` existence check so re-runs are idempotent (`BHonTagDictionary.java:37-40,66-251`).
Two ways a component gets tagged:
- **Direct**: a `BTagInfo` whose `getTag(entity)` returns a value (e.g. `BIdTag` emits `Tag(id,
  slotPathOrd)`).
- **Rule-based auto-tagging**: a `BTagRule` gated by a `BTagRuleCondition`. `BSmartTagDictionary`'s overlay
  engine (`getImpliedTag`/`addAllImpliedTags:101-167`) iterates `getTagRules()`, evaluates each rule's
  condition against the entity, and applies matching tags. Example: `BIsPointProxyTypeRule` +
  `BIsPointProxyTypeCondition.test = entity instanceof BControlPoint && proxyExt.getType().is(proxyExtType)`
  — a driver's points get classified by their proxy-ext TYPE with zero hand-tagging. `BEquipmentTypeTag` maps
  a point-folder's display NAME → an equip-type tag via a `lookupTable` facet.

## 758.2 — Relations `[CERT]`
A relation links two components in the entity graph. Author a custom one with `BCustomRelation extends
BRelationInfo` (`BCustomRelation.java:59-198`): declare a source scope (`entity` | `station`), a target type
(`BTypeSpec`), and inbound/outbound relation-id facet maps. At runtime it resolves targets by an ORD/NEQL
query (`BOrd.make(query).get() → BITable → cursor`) and emits `new BasicRelation(id, component, inbound?)`
per hit. Query the graph directly with `entity.relations().get(Id, dir)` / `getAll(Id, dir)` — each `Relation`'s
endpoint is the far component (Haystack's `BContainmentRelation` uses `equipRef`/`siteRef` this way,
`:88,108,133`). Classification (`equip`/`point` navigation) is carried by these relation EDGES + marker tags,
not by hard-coded `n:equip` strings.

## 758.3 — Expose a component over oBIX `[CERT]`
- The oBIX server is a `BWebServlet` mounted at `/obix` (`BObixServer.java:73-77`), with an `obix:` ORD scheme
  routed by registry def (`BObixOrdScheme.resolve:51-72`).
- **A component becomes an oBIX object via an `@AgentOn` agent**: subclass `BObixAgent`/`BIObixAgent`
  annotated `@AgentOn(types={"YourType"}, requiredPermissions="r")`; its `encode(OrdTarget, ObixEncoder)`
  wraps the value + status into an obix `Obj` with `val/min/max/unit/status` and an `href`
  (`BControlPointAgent.java:36,47-106` for `control:ControlPoint`). Lobby agents map obix URIs → station ords
  (`BStationLobbyAgent` → `"station:|" + decode(uri)`).

## 758.4 — Fox / BOX / REST, and BQL from code `[CERT]`
- **Fox** (`BFoxService`, ports 1911 / 4911-TLS) = the binary component-subscription channel between stations
  and Workbench. **BOX** (`BBoxServlet` at `/box`) = Fox-over-HTTP for the browser (BajaScript).
- **The generic REST query path** = `QueryServlet` (`box-rt`, `doGet:118`): parse the URL tail into an ORD,
  resolve it, apply a `BFilterSet` (BQL filter) with offset/limit/sort, stream rows via a cursor. Contrast
  with our `DashboardPan` servlet (B752): `QueryServlet` is the GENERIC ORD-addressable table endpoint; ours is
  a bespoke `BWebServlet` returning hand-shaped JSON for one dashboard — same `BWebServlet` substrate,
  different granularity.
- **BQL/NEQL from code** — the cursor pattern (memorize):
  ```java
  BITable<?> t = (BITable<?>) BOrd.make("station:|slot:/|bql:select ... from <module:Type> where ...")
                                  .get(Sys.getStation());
  TableCursor<?> c = t.cursor();
  while (c.next()) { ... c.cell(column) ... }
  c.close();
  ```
  (`BAnalyticService.java:1663-1685`.) `from <module:Type>` is how BQL selects by station type — the same
  mechanism a semantic dashboard uses. oBIX even exposes BQL as `/obix/bql/<query>` (`BBqlLobbyAgent`).

## 758.5 — Application to our modules `[INFER]`
- We have NO tag dictionary today. Adding one (namespace `angeles`, marker tags `room`/`evaporator`/
  `compressor`/`defrost`, a rule keyed on our component TYPE) makes our equipment queryable by NEQL and feeds
  a discoverability layer (B749 P9 / B750). Low risk (a dictionary is an additive component).
- We already expose data via our REST servlet with real RBAC (B752) — stronger than shipping oBIX agents with
  visibility-only perms. If a northbound client (a BMS, a Node-RED bridge, B748) needs standard oBIX, the
  `BObixServer` + a `BObixAgent` per type is the additive path; keep the servlet for the dashboard.

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | Tag dict = subclass BSmartTagDictionary + namespace + seed BSimpleTagInfo(marker/value) in ctor | [CERT] | BHonTagDictionary.java:37-40,66-251 |
| 2 | Auto-tagging via BTagRule+BTagRuleCondition (e.g. proxyExt type); equip tag from folder name | [CERT] | BSmartTagDictionary:101-167; BIsPointProxyTypeCondition; BEquipmentTypeTag |
| 3 | BCustomRelation resolves targets by ORD/NEQL query → BasicRelation; query via entity.relations().get/getAll | [CERT] | BCustomRelation.java:59-198; BContainmentRelation:88-133 |
| 4 | oBIX: BObixServer=/obix BWebServlet; component→oBIX via BObixAgent @AgentOn; lobby maps uri→ord | [CERT] | BObixServer.java:73-77; BControlPointAgent.java:36-106 |
| 5 | Fox 1911/4911; BOX /box; QueryServlet generic ORD+BQL; contrast our bespoke servlet | [CERT] | BFoxService:159-161; BBoxServlet; QueryServlet.doGet:118 |
| 6 | BQL from code: BOrd station:|slot:/|bql:… → BITable → cursor; oBIX exposes /obix/bql | [CERT] | BAnalyticService.java:1663-1685; BBqlLobbyAgent |

**Tally**: 6 [CERT], §758.5 [INFER]. No unmarked claims.

## Connections
- **B5** (ORD/BQL/NEQL/tags), **B749** P9 / **B750** (semantic overlay + our discoverability gap), **B727**/
  **B728** (oBIX server exposure), **B752** (our REST servlet + RBAC), **B748** (a Node-RED bridge would use
  oBIX/MQTT). Forward: **B759** (audit).

## Open gaps
- **B758-G1**: honTagDictionary tag literals are string-obfuscated (structure readable, names not) — [zero on
  literal tag names].
- **B758-G2**: an MQTT-northbound exposure recipe (honMqttDriver is a CONSUMER; publishing our data over MQTT
  is a separate path) — deferred to a Node-RED-bridge PoC (B748-G2).
