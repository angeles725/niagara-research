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
covered_blocks: 1
gaps_closed: 1
known_gaps: 8
investigable_open: 7
requires_execution_open: 0
blocked_open: 0
<!-- /research-state.v1 -->

focus: apis
status: active
seeded_from: AUDIT-FIRST 2-agent coverage sweep 2026-08-25 (delegated sonnet ×2; pre-flight verified inline)
seeded_on: 2026-08-25
gaps_total: 8 investigable (API1–API8)
gaps_closed: 1 (API1→B507)
blocks_written: B507 (API1)
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
| high | **API2 web-tier ORD-over-HTTP routing** — the `/ord`, `/file`, `/px` servlet contract: how an ORD in a URL becomes an HTTP response (WebServlet/OrdServlet/FileServlet), the WWW-Authenticate negotiation (N4 "Digest"=SCRAM, why Basic is ignored [B290]) | `web/web-rt/…/javax/baja/web/`, servlets | pending |
| high | **API3 oBIX server-side (`obix-rt`)** — N4 AS an oBIX host: the servlet the module registers, component→oBIX-object mapping, the server-side Watch contract. Closes the future-gap flagged in [B499 §499.8] | `obix/obix-rt/…` | pending |
| medium | **API4 BOX protocol wire** — the binary subscription wire BajaScript uses browser↔station: framing, opcodes, the subscribe/poll contract. Named-in-passing in [B19]/[B36]/[B42] but no dedicated wire block | `bajaScript*`, `bajaux*`, `web/web-rt` (BOX servlet) | pending |
| medium | **API5 BAuthenticationScheme SPI (framework side)** — the N4 contract for AUTHORING a custom auth scheme: `javax.baja.security.authn` SPI, scheme registration, the JAAS/session contract. [B494] covered OEM *implementations*; this is the platform framework | `baja/…/javax/baja/security/authn*` | pending |
| medium | **API6 Fox CLIENT programmatic API** — the public Java Fox-session surface for module authors (open session, remote slot read/write, channel types), distinct from the WIRE [B134] and the hand-rolled JACE client [B471–B473]. Pre-flight: NOT in docSource → locate the real package (baja jar / `com.tridium.fox`) | TBD (locate `javax.baja.fox`/`com.tridium.fox`) | pending — source-locate first |
| medium | **API7 BJob / JobService API** — the background-job authoring surface: `javax.baja.job.BJob`/`BJobService`, how a module submits/tracks a long-running job (used by provisioning/discovery). No dedicated block | `docSource/…/javax/baja/job/` | pending |
| low | **API8 BQL/NEQL programmatic + over-HTTP** — the CALL contracts (not the grammar, which is [B5]/[B21]): the Java `BIBqlQuery`/`BLocalBqlResolver` call site AND the BQL-over-HTTP query endpoint | `baja/…/javax/baja/bql*`, `web/web-rt` | pending |

### Recorded-not-seeded (PARTIAL "call-site cookbook" — open only if the above run dry)

The audit flagged several surfaces as PARTIAL because the corpus covers their ARCHITECTURE but not a
call-site cookbook: Control/Point write API (`BControlPoint`/`BNumericWritable` from Java; arch in [B6]/[B8]),
History query API (`HistorySpaceConnection.timeQuery`; arch in [B8]/[B410]), Tag/NEQL Java API (arch in
[B21]/[B263]), `Sys`/service-container call patterns (arch in [B20]), BProgram scripting (context in [B344]).
These risk being REMITTANCE re-hashes — NOT seeded as gaps; revisit only if API1–API8 close early.

## Stop control (METHODOLOGY §8)

- **Open gaps — read-only investigable**: **7** (API2–API8). Source-confirmed except API6 (source-locate first).
- **Gaps closed**: 1 (API1→B507).
- **requires-execution / blocked**: nHaystack REST + full BACnet-WS = blocked-on-source (absent), NOT counted.
- **Coverage metric**: 1 / 8.
- **NEXT**: API2 (web/ORD). Order: API2 (web/ORD) → API3 (oBIX server) → API5 (auth SPI) → API7 (jobs)
  → API4 (BOX) → API6 (Fox client, locate first) → API8 (BQL contracts).

## Iteration history

| It | Date | Gap closed | Block | New gaps uncovered |
|---|---|---|---|---|
| it.0 (bootstrap) | 2026-08-25 | — (AUDIT-FIRST seed) | — | 2 parallel `sonnet` audits (network/web/auth + programmatic/client/platform) mapped ~40 API surfaces; MOST already have dedicated blocks (REMITTANCE list above). 8 genuine uncovered surfaces seeded (API1–API8); nHaystack/BACnet-WS proven-absent; call-site-cookbook PARTIALs recorded-not-seeded. Pre-flight §e2 confirmed source for API1/API3/API5/API7/API8; API6 needs source-locate. |
| it.1 | 2026-08-25 | **API1** @NiagaraRpc — N4's annotation-driven HTTP RPC API. @NiagaraRpc method annotation (transports web/box/fox, permissions default "I"=Invoke, protectedTargets, isSecure); NiagaraRpcServlet POST /rpc/{method}/{ord}+batch, JSON in/out {"value":..}; marshalling com.tridium.json + implicit Context tail-arg + legacy fox-only whitelist (=replacement for pre-annotation RPC); 4-layer auth (session ctx + CsrfProtectedFilter on /rpc/* + isSecure HTTPS gate + RBAC), CLOSED-by-default (unrestricted=open); used widely by OEM UX RPC classes. `sonnet` sweep (15 tool-uses), all load-bearing re-verified inline (offsets discarded). verify-block exit 0, ratio 0.25. | **B507** | none net-new · yes · sonnet |
