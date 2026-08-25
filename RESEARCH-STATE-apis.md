# RESEARCH-STATE — focus: apis (PLANNED)

> Multi-focus corpus (METHODOLOGY §16). SEEDED by a two-agent AUDIT-FIRST coverage sweep (§13) on 2026-08-25
> that mapped EVERY Niagara N4 API surface against the corpus and separated REMITTANCE (already covered) from
> genuine gaps. Request: "investigate every API N4 exposes — any API." The finding: the corpus already has a
> DEDICATED block for almost every major surface; this focus targets only the API surfaces with **no dedicated
> treatment**, each source-confirmed by pre-flight (§e2).
>
> **Angle (§b2):** the N4 API SURFACES not yet given a dedicated block — read-only, decompiled-Java
> (`organized/…`) + Tridium javadoc (`organized/docSource/…`). Corpus language = **English**.
>
> **Scope:** API *surfaces* (programmatic/network/web/auth interfaces), NOT app-level API usage inside specific
> modules (Reflow/chihuahua already audited). Does NOT re-open the closed `framework-drivers` focus.

<!-- research-state.v1 -->
schema: research-state.v1
block_scope: shared-global
covered_blocks: 5
gaps_closed: 5
known_gaps: 8
investigable_open: 3
requires_execution_open: 0
blocked_open: 0
<!-- /research-state.v1 -->

focus: apis
status: active
seeded_from: AUDIT-FIRST 2-agent coverage sweep 2026-08-25 (delegated sonnet ×2; pre-flight verified inline)
seeded_on: 2026-08-25
gaps_total: 8 investigable (API1–API8)
gaps_closed: 5 (API1→B507 … API5→B510, API7→B511)
blocks_written: B507–B510 (API1–API3,API5), B511 (API7)
block_prefix: niagara-mental-model-bloqueN.md (shared global numbering)

## REMITTANCE — API surfaces ALREADY covered (will NOT be opened)

Confirmed by the audit; cite the existing block, do not re-derive:
Fox/Foxs wire [B134] · Sox ABSENT [B136] · oBIX client driver [B499] · BACnet wire [B133]/[B23]/[B127] ·
NiagaraNetwork supervisor↔sub [B414–B420] · servlet/BaseServlet + CSRF [B58] · hx profile [B433] ·
WebSocket/SocketServlet (Reflow-framed) [B59] · platform daemon :3011/:5011 [B129]/[B460] ·
SCRAM login [B457]/[B134] · api-access legit flow [B457–B458] · Baja SDK/object model [B4]/[B12] ·
ORD API [B5]/[B38] · BComponentSpace internals [B408] · station/BOG lifecycle [B10]/[B408]/[B411] ·
Alarm API [B8]/[B345] · BajaScript+subscriber [B36]/[B42]/[B47] · bajaux [B204] · PX client [B22]/[B194] ·
orion ORM [B412] · migration [B25].

## Proven-absent (recorded, NOT a gap)

- **nHaystack REST module** — pre-flight: NO `nhaystack`/`nHaystack` module under `organized/` (the `haystack`
  dir is the tag-model, not the REST server). N4 REST-via-Haystack is not installed here → `blocked-on-source`.
- **BACnet Web Services (Annex Q)** — the full Annex-Q module is absent; the small `bacnetOws` (14 cls) is the
  only BACnet-WS-adjacent module present (folded into API-note, low priority).

## Gap-backlog (prioritized) — genuine uncovered API surfaces, source-confirmed

| Priority | Gap | Where (`organized/…`) | Status |
|---|---|---|---|
| high | ~~**API1 NiagaraRpc / NiagaraRpcServlet**~~ — the N4 HTTP RPC API: `javax.baja.rpc.NiagaraRpc` + the `NiagaraRpcServlet` web endpoint (how modules expose server-side calls over HTTP; request/response envelope; auth). This is the concrete "general N4 web API" the audit flagged as a GAP | `baja/baja/vineflower/javax/baja/rpc/`, `web/web-rt/…/javax/baja/web/servlets/NiagaraRpcServlet.java` | **COVERED → B507** (method annotation @NiagaraRpc: transports web/box/fox, permissions default "I"=Invoke, isSecure; NiagaraRpcServlet POST /rpc/{method}/{ord}+batch, JSON {"value":..}; marshalling com.tridium.json + Context tail-arg + legacy fox-only whitelist; 4-layer auth session+CSRF(/rpc/*)+HTTPS+RBAC CLOSED-by-default [unrestricted=open]; used widely by OEM UX RPC classes B245/B493/B244) |
| high | ~~**API2 web-tier ORD-over-HTTP routing**~~ — the `/ord`, `/file`, `/px` servlet contract: how an ORD in a URL becomes an HTTP response (WebServlet/OrdServlet/FileServlet), the WWW-Authenticate negotiation (N4 "Digest"=SCRAM, why Basic is ignored [B290]) | `web/web-rt/…/javax/baja/web/`, servlets | **COVERED → B508** (servlet mount table /ord→OrdServlet //file→FileServlet //wb //rpc; ORD dispatch OrdTargetFilter→WebOpFilter→ViewFilter[profile]→render; /file traversal guard + .bog block; AUTH: N4 Digest=SCRAM-SHA-256 WWW-Authenticate 3-leg handshake via ScramServer; Basic scheme-gated NOT in-code-disabled §14-refines-B290; profile=per-account hx default) |
| high | ~~**API3 oBIX server-side (`obix-rt`)**~~ — N4 AS an oBIX host: the servlet the module registers, component→oBIX-object mapping, the server-side Watch contract. Closes the future-gap flagged in [B499 §499.8] | `obixDriver-rt/…javax/baja/obix/driver/` | **COVERED → B509** (BObixServer=BWebServlet+Soaplet at /obix +soap/wsdl/xsd, child of BObixNetwork, license export→403; lobby-agent map; REST GET/PUT/POST + SOAP read/write/invoke; server Watch BObixWatchService per-user checkUser lease30s; writes RBAC-gated set(..,user)→PermissionErr; SEC: /obix/config=whole tree + /obix/ord=arbitrary ORD, NO read allowlist [only continuousControl curated], corroborated live by B458) |
| medium | **API4 BOX protocol wire** — the binary subscription wire BajaScript uses browser↔station: framing, opcodes, the subscribe/poll contract. Named-in-passing in [B19]/[B36]/[B42] but no dedicated wire block | `bajaScript*`, `bajaux*`, `web/web-rt` (BOX servlet) | pending |
| medium | ~~**API5 BAuthenticationScheme SPI (framework side)**~~ — the N4 contract for AUTHORING a custom auth scheme: `javax.baja.security.authn` SPI, scheme registration, the JAAS/session contract. [B494] covered OEM *implementations*; this is the platform framework | `baja/…/javax/baja/authn/` | **COVERED → B510** (REMITTANCE-checked genuine delta vs B494; BAuthenticationScheme abstract getSchemeName/getLoginConfiguration[JAAS]/getDefaultAuthenticator + hooks + login(); 2-level registration @AgentOn-type + service-folder-instance; BUser.authenticationSchemeName binding; authenticate() orchestration scheme.login→BUser principal→setAuthenticated; NiagaraLoginModule + BCallbackHandler per-transport discovery; shipped n4digest/n4HTTPbasic/session; forward-pointer added B494) |
| medium | **API6 Fox CLIENT programmatic API** — the public Java Fox-session surface for module authors (open session, remote slot read/write, channel types), distinct from the WIRE [B134] and the hand-rolled JACE client [B471–B473]. Pre-flight: NOT in docSource → locate the real package (baja jar / `com.tridium.fox`) | TBD (locate `javax.baja.fox`/`com.tridium.fox`) | pending — source-locate first |
| medium | ~~**API7 BJob / JobService API**~~ — the background-job authoring surface: `javax.baja.job.BJob`/`BJobService`, how a module submits/tracks a long-running job (used by provisioning/discovery). No dedicated block | `baja/…/javax/baja/job/` | **COVERED → B511** (BJob=abstract BComponent [not Runnable], doRun/doCancel hooks + BSimpleJob/BRunnableJob; BJobState 6-state unknown/running/canceling/canceled/success/failed; submit→doSubmitAction mounts TRANSIENT dynamic slot, ForkJoinPool threadsPerCPU=2; progress(-1..100)+heartbeat TRANSIENT|READONLY not-BOG, readLogFrom incremental; retention 3-per-type+10min; live at /Services/JobService Fox-observable + /spy; ~237 subclasses [B500/B39]; no job-specific RBAC) |
| low | **API8 BQL/NEQL programmatic + over-HTTP** — the CALL contracts (not the grammar, which is [B5]/[B21]): the Java `BIBqlQuery`/`BLocalBqlResolver` call site AND the BQL-over-HTTP query endpoint | `baja/…/javax/baja/bql*`, `web/web-rt` | pending |

### Recorded-not-seeded (PARTIAL "call-site cookbook" — open only if the above run dry)

The audit flagged several surfaces as PARTIAL because the corpus covers their ARCHITECTURE but not a
call-site cookbook: Control/Point write API (`BControlPoint`/`BNumericWritable` from Java; arch in [B6]/[B8]),
History query API (`HistorySpaceConnection.timeQuery`; arch in [B8]/[B410]), Tag/NEQL Java API (arch in
[B21]/[B263]), `Sys`/service-container call patterns (arch in [B20]), BProgram scripting (context in [B344]).
These risk being REMITTANCE re-hashes — NOT seeded as gaps; revisit only if API1–API8 close early.

## Stop control (METHODOLOGY §8)

- **Open gaps — read-only investigable**: **3** (API4, API6, API8). Source-confirmed except API6.
- **Gaps closed**: 5 (API1→B507 … API5→B510, API7→B511).
- **requires-execution / blocked**: nHaystack REST + full BACnet-WS = blocked-on-source (absent), NOT counted.
- **Coverage metric**: 5 / 8.
- **NEXT**: API4 (BOX wire). Order: API4 (BOX wire)
  → API4 (BOX) → API6 (Fox client, locate first) → API8 (BQL contracts).

## Iteration history

| It | Date | Gap closed | Block | New gaps uncovered |
|---|---|---|---|---|
| it.0 (bootstrap) | 2026-08-25 | — (AUDIT-FIRST seed) | — | 2 parallel `sonnet` audits (network/web/auth + programmatic/client/platform) mapped ~40 API surfaces; MOST already have dedicated blocks (REMITTANCE list above). 8 genuine uncovered surfaces seeded (API1–API8); nHaystack/BACnet-WS proven-absent; call-site-cookbook PARTIALs recorded-not-seeded. Pre-flight §e2 confirmed source for API1/API3/API5/API7/API8; API6 needs source-locate. |
| it.1 | 2026-08-25 | **API1** @NiagaraRpc — N4's annotation-driven HTTP RPC API. @NiagaraRpc method annotation (transports web/box/fox, permissions default "I"=Invoke, protectedTargets, isSecure); NiagaraRpcServlet POST /rpc/{method}/{ord}+batch, JSON in/out {"value":..}; marshalling com.tridium.json + implicit Context tail-arg + legacy fox-only whitelist (=replacement for pre-annotation RPC); 4-layer auth (session ctx + CsrfProtectedFilter on /rpc/* + isSecure HTTPS gate + RBAC), CLOSED-by-default (unrestricted=open); used widely by OEM UX RPC classes. `sonnet` sweep (15 tool-uses), all load-bearing re-verified inline (offsets discarded). verify-block exit 0, ratio 0.25. | **B507** | none net-new · yes · sonnet |
| it.2 | 2026-08-25 | **API2** N4 web-tier routing + auth negotiation. Servlet mount table (web.xml): /ord→OrdServlet //file+/module→FileServlet //wb→WbServlet //view/all/ord //rpc→NiagaraRpcServlet[API1]. ORD dispatch pipeline: OrdTargetFilter(parse+BOrd.resolve, sql blocked)→WebOpFilter→ViewFilter(profile+viewid override)→OrdServlet render(hx/bajaux servletview vs px/wb via WbServlet). /file traversal guard (| ../) + canRead blocks dirs/.bog/.bog.gz; no /px mount. AUTH HEADLINE: N4 Digest=SCRAM-SHA-256 (BHttpDigestCallbackHandler SCHEME_NAME=SCRAM, hash SHA-256), WWW-Authenticate 3-leg handshake via com.tridium.nre.auth.ScramServer. Basic handler present+functional but scheme-GATED (not in-code disable) → §14 REFINE of B290 (back-pointer added). Profile=per-account BWebProfileConfig default hx:HTML5HxProfile. `sonnet` sweep (36 tool-uses), all load-bearing re-verified inline. verify-block exit 0, ratio 0.38. | **B508** | none net-new (§14 refine B290) · yes · sonnet |
| it.3 | 2026-08-25 | **API3** oBIX SERVER (N4 as oBIX host) — CLOSES future-gap B499 §499.8. BObixServer extends BWebServlet implements Soaplet, mounts /obix (+/obix/soap/wsdl/xsd), child of BObixNetwork; license export sub-key gates→403. Lobby-agent map (@AgentOn obixDriver:ObixLobby): about/config/ord/continuousControl/histories/alarms/bql/contract/units/watchService. REST GET(encode)/PUT(serviceWrite parent.set(..,user))/POST(serviceInvoke) + SOAP XElemTunnel read/write/invoke. Server Watch BObixWatchService.make→BObixWatch per-user (checkUser reference-identity, lease 30s/15s, Subscriber change-detect). SEC HEADLINE: /obix/config=whole station tree (station:|slot:), /obix/ord=arbitrary BOrd.resolve — NO oBIX read allowlist (only requiredPermissions=r metadata); only /obix/continuousControl curated (BObixExportFolder). Writes RBAC-enforced; reads bounded by read-RBAC but whole-tree enumerable — corroborated live by B458 [CERT-live]. `sonnet` sweep (22 tool-uses), all load-bearing re-verified inline. verify-block exit 0, ratio 0.35. | **B509** | none net-new · yes · sonnet |
| it.4 | 2026-08-25 | **API5** BAuthenticationScheme SPI (framework AUTHORING contract) — REMITTANCE-checked vs B494 (that was a 1-paragraph impl summary; this is the delta). Base javax.baja.authn.BAuthenticationScheme (abstract getSchemeName/getLoginConfiguration[JAAS Configuration]/getDefaultAuthenticator + hooks supportsRemoteUsers/getKeyExchangeMethodName + concrete login(CallbackHandler)→LoginContext); 2-LEVEL registration (@AgentOn baja:AuthenticationScheme type-registry + INSTANCE added to BAuthenticationService.authenticationSchemes folder — @AgentOn alone ≠ folder); BUser.authenticationSchemeName slot (default DigestScheme) binds user→scheme. Login orchestration BAuthenticationService.authenticate(): scheme.login→JAAS LoginModule→subject.getPrincipals(BUser)→setReadOnly→session.setAuthenticated(subject). NiagaraLoginModule.commit adds BUser principal; scheme.getAgentOn(BCallbackHandler.class) discovers per-transport handler (=B508 web SCRAM/Basic handlers). Shipped: n4digest(DigestScheme,default,key-exchange)/n4HTTPbasic/session(throws on getDefaultAuthenticator). getSchemeName()=on-wire token. Forward-pointer added B494 §494.1. `sonnet` REMITTANCE-aware sweep (18 tool-uses), all load-bearing re-verified inline. verify-block exit 0, ratio 0.25. | **B510** | none net-new (extends B494, not correction) · yes · sonnet |
| it.5 | 2026-08-25 | **API7** BJob/JobService background-job API — REMITTANCE-checked (B20 §20.7 = 40-line summary, no dedicated block). BJob=abstract BComponent (job IS a live component, not a Runnable); author impl doRun/doCancel or subclass BSimpleJob/BRunnableJob; doSubmit framework callback. BJobState 6-state (unknown/running/canceling/canceled/success/failed, isComplete). submit(cx)→BJobService.doSubmitAction (doPrivileged): mounts job as TRANSIENT dynamic slot (typeName+'?') under /Services/JobService, ForkJoinPool executor threadsPerCPU=2 default. progress(-1..100)+heartbeat, jobState/progress TRANSIENT|READONLY (not persisted to BOG); JobLog + readLogFrom(seq) incremental tail; submit returns job ORD → Fox subscription. cancel→canceling+interrupt (JobCancelException cooperative); retention ServiceManager.houseKeeping jobMaxCountPerType=3 + jobMinAgeToKeep=600000ms(10min), post-submit not timer; dispose throws if running. Exposure: live Fox-observable, spy() /spy view, BIRestrictedComponent one-per-container; RBAC=standard action perms, no job-specific type. ~237 subclasses (RE-MEASURED from sweep's 42): BStationSaveJob/BNDiscoveryJob/BBatchJob. `sonnet` sweep (29 tool-uses), all load-bearing re-verified inline. verify-block exit 0, ratio 0.27. | **B511** | none net-new · yes · sonnet |
