# RESEARCH-STATE — focus: jsonToolkit (ACTIVE 2/14)

> Multi-focus corpus (METHODOLOGY §16). Focus **BOOTSTRAPEADO 2026-08-04** a pedido explícito del usuario
> ("documentar también el módulo JSONTOOLKIT"), inmediatamente tras cerrar el focus `email` (B324-B334).
>
> **NO es terreno virgen** — backlog audit-first. Cobertura previa (solo menciones de paso, NINGUNA dedicada):
> - **[Bloque 20]** / **[Bloque 32]** — jsonToolkit listado entre "Honeywell enterprise modules" sin profundizar;
>   **B32.3** apuntó el riesgo CVE de la dependencia Jayway/jsonPath (hilo de seguridad, no cobertura).
> - **[Bloque 76]** — usó el `InlineJsonWriter` de jsonToolkit como INSPIRACIÓN para portar un generador JSON a
>   `chihuahua-rt`; lo referencia, no lo documenta.
> - **[Bloque 85]** — repite el riesgo CVE de deps (referencia a B32.3).
> NO hay fila en CATALOG ni focus previo.
>
> **Ángulo declarado (§b2)**: el módulo add-on `com.tridiumx.jsonToolkit` (namespace `tridiumx` = familia
> extendida, NO core) como marshaller JSON bidireccional de datos de station. Dos direcciones: **outbound**
> (generación de JSON dirigida por schema, alimentada por subscripciones COV + queries BQL/history) e **inbound**
> (selectores JSONPath que escriben setpoints / ackean alarmas / registran export markers). Gate de licencia
> `getFeature("tridium","jsonToolkit")` ("DR-JSON"). Primera cita del corpus a `docJsonToolkit` (115 archivos).

<!-- research-state.v1 -->
schema: research-state.v1
block_scope: shared-global
covered_blocks: 331
gaps_closed: 2
known_gaps: 14
investigable_open: 12
requires_execution_open: 0
blocked_open: 0
deferred_open: 0
undocumented_findings: 0
<!-- /research-state.v1 -->

focus: jsonToolkit
status: active
bootstrapped_on: 2026-08-04
block_prefix: niagara-mental-model-bloqueN.md (numeración global; próximo libre: B337)

## Pre-flight e2 — existencia + tamaño MEDIDO

Conteo sobre el pipeline **vineflower**. Raíz:
`/home/cristian/modules/Prototipos/modulos/organized/jsonToolkit/`.

| Artefacto | Clases propias `com.tridiumx.jsonToolkit` | Terceros empaquetados |
|---|---|---|
| `jsonToolkit-rt` | **147** | Gson 2.9.0 + jayway-jsonpath (~50) |
| `jsonToolkit-ux` | **8** | — |
| `jsonToolkit-wb` | **8** | Gson (~26 en wb) |
| **Total propias** | **163** | — |

Subpaquetes: `outbound/schema` (property 15 · support 12 · style 10 · subscription 9 · query 8 · config 8 ·
alarm · program · relative), `inbound` (selector 12 · routing · handler · exportMarker), `util` (10), `ux`, `ui`.
Doc oficial `docJsonToolkit/` = **115 archivos** (`[CERT-doc]`, nunca citada por el corpus).
Deps notables (module.xml): bql, chart, control, driver, entityIo, fox, gx, history, jsonSmart, ndriver,
program, query, schedule, serial.

## Dismissed file types / paquetes

- `com.google.gson` (**2.9.0**, ~26 wb) — OSS upstream (Google Gson); doc propia. Solo se documenta QUE está
  shaded + versión. NO se re-documenta.
- `com.jayway.jsonpath` (~50 rt, ~2.7-2.8) — OSS upstream (Jayway JsonPath); backend de los selectores inbound.
  Solo se cita como dependencia; NO se re-documenta.

## Coverage

- **Covered blocks**: 331 (corpus-wide, shared-global)
- **Coverage metric**: 2 / 14 closed
- **Last iteration**: 2026-08-04 — J2 closed (outbound schema model, B336)

## Gap-backlog (prioritized)

| Priority | Gap | Type | Status |
|---|---|---|---|
| high | J1 BJsonSchemaService — entrada, gate de licencia tridium/jsonToolkit (DR-JSON), SMA, threadpool, ciclo de vida | decompiled-java + doc | closed (B335) |
| high | J2 outbound schema model — BJsonSchema/Member/BoundMember/BoundSlotsContainer, seleccion de slots | decompiled-java + doc | closed (B336) |
| high | J3 subscription→output pipeline — COV event → JSON serializado, DONDE se escribe la salida (nucleo del valor) | decompiled-java + doc | pending |
| high | J4 outbound query — QueryRunner, dialecto BQL/history, timeout/bloqueo del engine thread | decompiled-java + doc | pending |
| high | J5 exporter/transport — BJsonExporter: como sale el JSON de la station (HTTP/file/fox) | decompiled-java + doc | pending |
| high | J6 inbound core + selectores — BJsonInbound, BJsonPath/selectores (JSONPath), routing (demux) | decompiled-java + doc | pending |
| high | J7 inbound handlers — BJsonSetPointHandler (nivel priority-array + runAsUser), alarm ack, export-marker registration (SEGURIDAD) | decompiled-java + doc | pending |
| medium | J8 outbound schema detail — property types (15), style/formatters (query/style), config/tuning (update strategy, name casing) | decompiled-java + doc | pending |
| medium | J9 relative schema — BRelativeJsonSchema/SubscriptionTable: agregacion cross-station por Fox | decompiled-java + doc | pending |
| medium | J10 inline/program writer — BInlineJsonWriter (referenciado por B76), integracion program-rt | decompiled-java + doc | pending |
| medium | J11 outbound alarm — BJsonAlarmRecipient + BIJsonAlarmDataResolver | decompiled-java + doc | pending |
| medium | J12 util / engine-cycle queues — LicenseLimit, BEngineCycleMessageQueue backpressure (drop/block) | decompiled-java | pending |
| low | J13 ux + wb layers — editores, BJsonToolkitRpcUtil (RPC), FormattedJsonParser (Gson) | decompiled-java + doc | pending |
| low | J14 doc-synthesis — grounding en docJsonToolkit (115 archivos [CERT-doc]) + lo que la doc NO resuelve | external-doc | pending |

## Iteration history

| # | Date | Gap closed | Block | Delegated? · model tier | New gaps uncovered |
|---|---|---|---|---|---|
| — | 2026-08-04 | (bootstrap — audit-first) | — | yes · sonnet (audit sweep) | 14 seeded |
| 1 | 2026-08-04 | J1 BJsonSchemaService | B335 | no · inline (constraint: 2 load-bearing license/service files) | 0 |
| 2 | 2026-08-04 | J2 outbound schema model | B336 | yes · sonnet (code+doc sweep) | 0 |

## Blocked gaps (each tagged with what it needs)

- none — all 14 gaps are read-only investigable (source + doc confirmed on disk).

## Stop control (primary = read-only-investigable exhaustion, METHODOLOGY §8)

- **Open gaps — read-only investigable**: 12   ← el loop ESTÁTICO para cuando esto llega a 0
- **Open gaps — requires-execution**: 0
- **Open gaps — blocked**: 0
- Consecutive iterations with empty backlog (secondary): 0/2
- Budget cap: none
