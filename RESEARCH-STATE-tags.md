# RESEARCH-STATE — focus: tags (ACTIVE)

> Multi-focus corpus (METHODOLOGY §16). Focus **BOOTSTRAPEADO 2026-07-24** a pedido explícito del usuario
> ("investigar todo lo relacionado con los tags"), inmediatamente después de cerrar `px-chart-classic` 8/8.
>
> **NO es un focus sobre terreno virgen** — y por eso el backlog es audit-first, no una lista de deseos.
> Cobertura previa verificada:
> - **[Bloque 21]** — *Tag Framework + Haystack 4 + BQL + NEQL*: 10 secciones (21.1 jerarquía
>   `BTagDictionary`, 21.2 mapeo Haystack 4, 21.3 diccionarios estándar vs custom, 21.4 workflow de export
>   tags, 21.5 BQL, 21.6 NEQL, 21.7 decisión operativa, 21.8 RPC de tag queries, 21.9 gotchas, 21.10
>   integración). **Es un bloque de ESPINAZO para ~159 clases.**
> - **[Bloque 82]** — diccionarios OEM Honeywell deofuscados (`honTagDictionary` / HBT Ontology +
>   `fcTagDict` / Forge Connect) + corrigendum a B21. **Los 29 de OEM ya están cubiertos** y quedan FUERA.
> - **[Bloque 5]** — tags dentro de ORD/BOG/queries/hierarchy.
>
> **Ángulo declarado (§b2)**: profundizar el subsistema de tagging donde B21 solo pasó por arriba, con dos
> ejes que el corpus nunca abrió — el sistema de **RELACIONES** (`relation/` + `docRelations`) y la
> **traducción tag→query** (`neqlize/`) — y sumar por primera vez la **documentación oficial de Tridium**
> como fuente `[CERT-doc]`.

<!-- research-state.v1 -->
schema: research-state.v1
covered_blocks: 255
gaps_closed: 0
known_gaps: 9
investigable_open: 9
requires_execution_open: 0
blocked_open: 0
<!-- /research-state.v1 -->

focus: tags
status: active
bootstrapped_on: 2026-07-24
block_prefix: niagara-mental-model-bloqueN.md (numeración global; próximo libre: B260)

## Pre-flight e2 — existencia + tamaño MEDIDO

Conteo sobre el pipeline **vineflower** (canónico), pipelines duplicados colapsados. Raíz:
`/home/cristian/modules/Prototipos/modulos/organized/`.

| Módulo | Clases | Desglose por paquete |
|---|---|---|
| `tagdictionary` | **78** | `-rt`: `javax/baja/tagdictionary` 20 + `/data` 2 · `com/tridium/tagdictionary/tag` 14 · `/condition` 9 · `/relation` 7 · `/neqlize` 6 · `/util` 5 · raíz 5 — `-ux`: 5 — `-wb/ui`: 4 |
| `haystack` | **37** | `-rt`: `com/tridium/haystack` 37 |
| `exportTags` | **44** | `-rt`: `/util` 10 · raíz 8 · `/tags` 7 · `/category` 3 — `-wb`: `/ui` 13 · `/tags/px` 3 |
| **Total código** | **159** | |
| `honTagDictionary` + `fcTagDict` | 29 | **FUERA DE SCOPE** — cubiertos por [Bloque 82] |

**Documentación oficial de Tridium** (fuente `[CERT-doc]`, NUNCA usada por el corpus):
`docTagging/` + `docRelations/` + `docExportTags/` = **200 archivos** `.txt`/`.html`.

## Gap-backlog (priorizado)

Formato canónico de 4 columnas exigido por `research-sdd-status.sh`.

| Priority | Gap | Type | Status |
|---|---|---|---|
| high | T1 API pública javax.baja.tagdictionary | decompiled-java | pending (NEXT) |
| high | T2 el motor del diccionario (tag + util + raiz) | decompiled-java | pending |
| high | T3 el sistema de RELACIONES | decompiled-java | pending |
| high | T4 condiciones + neqlize (tag a query) | decompiled-java | pending |
| medium | T5 haystack la implementacion completa | decompiled-java | pending |
| medium | T6 exportTags runtime | decompiled-java | pending |
| medium | T7 exportTags UI + integracion px | decompiled-java | pending |
| medium | T8 tagdictionary UI y UX | decompiled-java | pending |
| medium | T9 documentacion oficial Tridium | external-doc | pending |

### Detalle por gap

- **T1** — `javax.baja.tagdictionary` (20) + `/data` (2): el contrato público. Qué es un `BTagDictionary`, la
  jerarquía real, y qué expone a un módulo de terceros. Refina/confirma [Bloque 21] §21.1.
- **T2** — `com/tridium/tagdictionary/` raíz (5) + `/tag` (14) + `/util` (5): la implementación del motor.
- **T3** — `com/tridium/tagdictionary/relation` (7) + el doc oficial `docRelations`. **El eje que B21 solo
  roza** (35 menciones de "relation", ninguna sección propia). Qué es una relación en N4, cómo se declara,
  cómo se consulta, y su relación con el modelo Haystack.
- **T4** — `/condition` (9) + `/neqlize` (6): cómo una condición de tag se **traduce a una query NEQL**.
  Cruce directo con [Bloque 21] §21.6 (NEQL) y §21.8 (RPC de tag queries).
- **T5** — `com.tridium.haystack` (37): la implementación completa. B21 §21.2 documentó el *mapeo* del modelo;
  esto es el módulo entero (¿servidor Haystack? ¿ops REST? ¿encoding Zinc/JSON?).
- **T6** — `exportTags-rt`: raíz (8) + `/tags` (7) + `/util` (10) + `/category` (3). B21 §21.4 documentó el
  *workflow*; esto es el mecanismo.
- **T7** — `exportTags-wb`: `/ui` (13) + `/tags/px` (3). La integración con PX — cruce con los focuses PX ya
  cerrados.
- **T8** — `tagdictionary-wb/ui` (4) + `tagdictionary-ux` (5): la cara de usuario del diccionario.
- **T9** — los 200 archivos de `docTagging`/`docRelations`/`docExportTags`. **Fuente EXTERNA** → antes de
  autorizar un bloque debe pasar el gate **e3 SCOUT-BEFORE-BUILD** (veredicto `CERTIFIABLE-NOW` /
  `PARTIAL` / `INSUFFICIENT`) y preservarse en `sources/` con registro en `SOURCES.md` (§5).

## Blocked gaps

- none

## Clasificación (§8)

- **read-only-investigable**: **9** (T1-T9) → focus ACTIVO.
- **requires-execution**: 0. **blocked**: 0.
- **Coverage metric**: **0 / 9** (bootstrap).
- **Próximo gap**: **T1** (la API pública — todo lo demás cuelga del contrato).

## Historia de iteración

| It | Fecha | Gap | Bloque | Hallazgo | Delegado? · tier |
|---|---|---|---|---|---|
| (ninguna aún — bootstrap 2026-07-24) | | | | | |

**Resume condition**: focus ACTIVO recién bootstrapeado. NO re-bootstrapear. Fuentes ya medidas (e2 arriba).
Para T9 correr el gate e3 antes de autorizar el bloque.
