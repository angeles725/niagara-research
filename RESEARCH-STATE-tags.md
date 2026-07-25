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
covered_blocks: 258
gaps_closed: 3
known_gaps: 9
investigable_open: 6
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
| high | T2 el motor del diccionario (tag + util + raiz) | decompiled-java | closed (B261) |
| high | T3 el sistema de RELACIONES | decompiled-java | closed (B262) |
| high | T4 condiciones + neqlize (tag a query) | decompiled-java | pending (NEXT) |
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

- **read-only-investigable**: **6** (T4-T9) → focus ACTIVO.
- **requires-execution**: 0. **blocked**: 0.
- **Coverage metric**: **3 / 9** (B260-B262).
- **Próximo gap**: **T4** (`condition/` 9 + `neqlize/` 6 — la traducción tag→query NEQL).
- **NOTA DE MÉTODO (§11)**: B260 cerró con ratio **0.74**, muy sobre el umbral. A diferencia de B254/B255 del
  focus anterior, acá **NO indica agotamiento** — T1 recién abrió el subsistema y quedan 8 gaps / ~137 clases.
  Indica **exceso de deducción del autor** (glosa comparativa contra el focus del chart). Corrección aplicada
  desde T2: más cita, menos interpretación.

## Historia de iteración

| It | Fecha | Gap | Bloque | Hallazgo | Delegado? · tier |
|---|---|---|---|---|---|
| it.3 | 2026-07-24 | **T3** — RELACIONES | **B262** | **Primer bloque del corpus sobre relaciones** (B21 las menciona 35 veces sin sección propia) y **primer uso de la doc oficial Tridium como `[CERT-doc]`** (gate e3 = CERTIFIABLE-NOW; guía preservada en `sources/manuals/docRelations-N4.14-guide.md`). **Una relación son DOS cosas bajo la misma interfaz**: `BRelation extends BStruct` = slot de propiedad ALMACENADO (relationId/inbound/sourceOrd + `direct` transitorio; se guarda en UN extremo, con `RelationKnob` de vuelta en el otro) vs `BasicRelation` = `final` inmutable NUNCA persistido, calculado por consulta. La doc oficial confirma el modelo sujeto-verbo-objeto ("AHU1 supplies VAV1") y que el Id de relación tiene la MISMA forma que el de un tag (`namespace:name`). Las 7 clases de `relation/` son **solo navegación estructural** (parent/child · device/network · device/point, con el trío que distingue `n:childPoint` = puntos con proxyExt real vs `n:childNullProxyPoint` = virtuales) y producen SOLO implícitas. **No hay clase inversa**: doble despacho dentro de la misma clase según el tipo de la entidad. **`n:tagGroup` NO sale de esas 7: es una `BRelation` ALMACENADA** — dato arquitectónico central, son dos subsistemas y no uno. **INTEGRIDAD REFERENCIAL — el hallazgo operativo**: `navEvent` maneja SOLO rename (`getId()==3`, único filtro); **el BORRADO no se maneja** → la propiedad `BRelation` sobrevive apuntando a nada, resolverla devuelve `null` y loguea a **FINE** (apagado en producción) ⇒ borrar un `BTagGroupInfo` deja componentes que **pierden silenciosamente los tags del grupo**, sin error ni recolección. Perf: `CompUtil.getDescendants()` en CADA consulta, sin caché. 10 tokens re-verificados. ratio 10/20 = 0.50 | sí · **sonnet** (75 tool-calls) + verificación inline |
| it.2 | 2026-07-24 | **T2** — motor | **B261** | El diccionario `Niagara` **no está compilado**: se lee de `module://<módulo>/bog/Niagara.bog` al arrancar. Compiladas sí están las 35 constantes `Id` del namespace `n` + **8 relaciones**; **`TAG_GROUP_RELATION` = `Id.newId("n","tagGroup")`** — cierra el mecanismo que B260 §260.4 dedujo. **Los tags de station son COMPUTADOS, no almacenados**: las 14 clases de `tag/` calculan su valor en cada consulta (`n:point`, `n:history`, `n:input/output`, `n:hasPxView`...) — por eso el subsistema necesita índices. **Dos índices, NINGUNO acotado**: `TagRuleIndex` (Id→Set<TagRule>, ConcurrentHashMap) y `EntityTagIndex` (BOrd→TagInfo[], synchronized) — sin cap, sin TTL, sin evicción; agregar/quitar un Id redimensiona TODOS los arrays (O(n entidades)). Import/export solo JSON y CSV (12 columnas, 13 tipos de valor cerrados); **la validación del predicado NEQL LOGUEA pero NO aborta** → un predicado roto entra y no matchea nada, falla silenciosa. Contra un archivo hostil no hay más protección que el `maxImportFileSize` de 1024 KB (CSV sin límite de filas; JSON sin límite de profundidad). **SEGUNDO bypass del candado `frozen`**: `Context.decoding` en `updateTagGroupRelations()`, sin chequeo de permisos en el sitio de llamada. §261.7: **2 afirmaciones del barrido CORREGIDAS, ambas sobre permisos** — (1) los `return` de `doImportDictionary` son RECHAZOS, no bypasses; lo real es que un diccionario VACÍO se importa sin pedir permiso; (2) "cualquiera puede exportar" era sobreafirmación (la invocación de acción ya pasa por el gate del framework) — el hallazgo real es la ASIMETRÍA: import chequea `hasAdminWrite()`, export no chequea nada y **descarta el contexto del invocador** (`submit(null)`). 9 tokens re-verificados. ratio 9/20 = **0.45** (bajó desde 0.74 de B260 tras aplicar la corrección de método) | sí · **sonnet** (24 clases) + verificación inline |
| it.1 | 2026-07-24 | **T1** — API pública | **B260** | **Un diccionario NO se registra: se MONTA.** `isParentLegal` exige que cuelgue como hijo directo de `BTagDictionaryService`, y el servicio los descubre **iterando sus propios slots** (`getProperties().next(TagDictionary.class)`) — ni un `@AgentOn` ni consulta al registro para descubrirlos (contraste fuerte con el chart, que extiende por agentes). Publicar un diccionario propio es configuración de ESTACIÓN, no de módulo. **El tagging SÍ está licenciado**: `getFeature("tridium","tags")` + un cupo opaco por diccionario vía `fw(501,"dictionary.limit")` que deja en `fatalFault` permanente si se excede — contraste directo con el chart clásico, donde la ausencia de gates fue hallazgo (B254 §254.8). Modelo: `Id` = `namespace:nombre`, y el **marcador es el valor por DEFAULT** (`BMarker.MARKER`), coherente con la orientación Haystack. El framework gobierna solo lo IMPLÍCITO (los tags directos viven en `javax.baja.tag.Entity`); la pieza central es `BSmartTagDictionary` con `tagRules`, y **un componente se une a un grupo mediante una RELACIÓN** (TAG_GROUP_RELATION) → las relaciones son infraestructura del tagging, lo que justifica T3 como gap propio. **HALLAZGO DE SEGURIDAD**: `public static Context importContext` (NO final) + `BInfoList.checkContext()` lo acepta ⇒ cualquier código de la JVM puede leerlo y **escribir en un diccionario `frozen`**; y al no ser final, otro módulo podría reemplazarlo (se registra la forma, no se afirma explotabilidad). Gotchas: `isImportRequired()` con AND triple (un diccionario con tags+grupos pero CERO relaciones dispara importación automática al arrancar), excepción tragada en `BDataPolicy`, caché estática compartida por JVM, `maxImportFileSize` oculto 1024 KB, y contratos JSON documentados SOLO por excepción en runtime. Primer `@Deprecated` real de estos dos focuses: `BTagGroupMonitor`. 11 tokens re-verificados | sí · **sonnet** (22 clases) + verificación inline |

**Resume condition**: focus ACTIVO recién bootstrapeado. NO re-bootstrapear. Fuentes ya medidas (e2 arriba).
Para T9 correr el gate e3 antes de autorizar el bloque.
