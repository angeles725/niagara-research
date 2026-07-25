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
covered_blocks: 256
gaps_closed: 1
known_gaps: 9
investigable_open: 8
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
| high | T1 API pública javax.baja.tagdictionary | decompiled-java | closed (B260) |
| high | T2 el motor del diccionario (tag + util + raiz) | decompiled-java | pending (NEXT) |
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

- **read-only-investigable**: **8** (T2-T9) → focus ACTIVO.
- **requires-execution**: 0. **blocked**: 0.
- **Coverage metric**: **1 / 9** (B260).
- **Próximo gap**: **T2** (el motor del diccionario).

## Historia de iteración

| It | Fecha | Gap | Bloque | Hallazgo | Delegado? · tier |
|---|---|---|---|---|---|
| it.1 | 2026-07-24 | **T1** — API pública | **B260** | **Un diccionario NO se registra: se MONTA.** `isParentLegal` exige que cuelgue como hijo directo de `BTagDictionaryService`, y el servicio los descubre **iterando sus propios slots** (`getProperties().next(TagDictionary.class)`) — ni un `@AgentOn` ni consulta al registro para descubrirlos (contraste fuerte con el chart, que extiende por agentes). Publicar un diccionario propio es configuración de ESTACIÓN, no de módulo. **El tagging SÍ está licenciado**: `getFeature("tridium","tags")` + un cupo opaco por diccionario vía `fw(501,"dictionary.limit")` que deja en `fatalFault` permanente si se excede — contraste directo con el chart clásico, donde la ausencia de gates fue hallazgo (B254 §254.8). Modelo: `Id` = `namespace:nombre`, y el **marcador es el valor por DEFAULT** (`BMarker.MARKER`), coherente con la orientación Haystack. El framework gobierna solo lo IMPLÍCITO (los tags directos viven en `javax.baja.tag.Entity`); la pieza central es `BSmartTagDictionary` con `tagRules`, y **un componente se une a un grupo mediante una RELACIÓN** (TAG_GROUP_RELATION) → las relaciones son infraestructura del tagging, lo que justifica T3 como gap propio. **HALLAZGO DE SEGURIDAD**: `public static Context importContext` (NO final) + `BInfoList.checkContext()` lo acepta ⇒ cualquier código de la JVM puede leerlo y **escribir en un diccionario `frozen`**; y al no ser final, otro módulo podría reemplazarlo (se registra la forma, no se afirma explotabilidad). Gotchas: `isImportRequired()` con AND triple (un diccionario con tags+grupos pero CERO relaciones dispara importación automática al arrancar), excepción tragada en `BDataPolicy`, caché estática compartida por JVM, `maxImportFileSize` oculto 1024 KB, y contratos JSON documentados SOLO por excepción en runtime. Primer `@Deprecated` real de estos dos focuses: `BTagGroupMonitor`. 11 tokens re-verificados | sí · **sonnet** (22 clases) + verificación inline |

**Resume condition**: focus ACTIVO recién bootstrapeado. NO re-bootstrapear. Fuentes ya medidas (e2 arriba).
Para T9 correr el gate e3 antes de autorizar el bloque.
