# RESEARCH-STATE — focus: niagara-network-supervisor (PLANNED)

> Focus **PLANIFICADO** el 2026-07-24 al cerrar el focus `tags`. **0 bloques escritos.**
> §16: un focus *planned* ya tiene su RESEARCH-STATE y su backlog commiteados — **el loop NO debe
> re-BOOTSTRAPEARLO**: toma este estado y escribe su primer bloque contra el gap de mayor prioridad.
>
> **Origen**: [Bloque 266] §266.1 probó que `exportTags` **NO pertenece al subsistema de tags** (0 de 28
> clases importan nada del diccionario) — es un mecanismo de **join supervisor↔subordinada** por Fox.
> Se documentó de urgencia en B266/B267 porque los gaps ya estaban abiertos en el focus `tags`, pero el
> módulo pide su propio eje bajo **Niagara Network / supervisor**.
>
> Corpus en **Español (técnico EN)**. Numeración global de bloques; próximo libre al planificar: **B271**.
> Engram topic key: `research/niagara/niagara-network-supervisor/{gaps,progress}`.

<!-- research-state.v1 -->
schema: research-state.v1
covered_blocks: 416
gaps_closed: 3
known_gaps: 6
investigable_open: 2
requires_execution_open: 0
blocked_open: 1
block_scope: shared-global
undocumented_findings: 0
<!-- /research-state.v1 -->

focus: niagara-network-supervisor
status: planned
planned_on: 2026-07-24

## Ángulo declarado (§b2)

El eje **supervisor ↔ subordinada** de Niagara Network: cómo una station provisiona configuración en otra.
`exportTags` es la puerta de entrada (ya parcialmente documentado en B266/B267); el eje completo incluye el
driver `niagaraDriver` que ambos usan.

## Lo que YA está documentado (no re-investigar)

- **[Bloque 266]** — el runtime del join: `BSupervisorJoinJob` (Fox → descarga de BOG → merge → BQL de tags
  habilitados → provisión de proxies), los 7 tipos de export tag, el paquete `category/`, la auditoría, y el
  worker de **un solo hilo** con cola de 1000.
- **[Bloque 267]** — la UI (`ui/` 13 clases) y **`BPxViewTag`**: la distribución de vistas PX a una flota con
  reescritura de ORDs en runtime vía `BSubstitutePxView`.
- **Fuente `[CERT-doc]` YA PRESERVADA**: `sources/manuals/docExportTags-N4.14-guide.md` (86 secciones, 181 KB,
  registrada en `SOURCES.md` **sin bloque citante** — este focus debe hacer el back-fill al citarla).

## Gap-backlog (priorizado)

| Priority | Gap | Type | Status |
|---|---|---|---|
| high | N1 el riesgo BSubstitutePxView wb-vs-rt | decompiled-java | **closed (B414)** |
| high | N2 niagaraDriver el driver que sostiene el join | decompiled-java | **closed (B415)** |
| medium | N3 la guia oficial de exportTags | external-doc | **closed (B416)** |
| medium | N4 seguridad del canal de join | decompiled-java | pending |
| low | N5 reproducir el fallo de tipo en un JACE | requires-execution | blocked |
| low | N6 cómo Niagara maneja tipos no resueltos en BOG de la propia station | decompiled-java | pending |

### Detalle por gap

- **N1 (HIGH)** — **el riesgo que motiva el focus.** [Bloque 267] §267.3 verificó que `BSubstitutePxView` y
  `BPxViewTag` viven en **`exportTags-wb.jar`**, pero `doJoin()` **persiste instancias de
  `BSubstitutePxView` en el espacio virtual de la estación DESTINO**. Un JACE típicamente **no** carga el
  perfil `-wb` ⇒ no podría resolver el tipo `exportTags:SubstitutePxView` al levantar ese slot virtual.
  **No reproducido** — verificado sólo por la ubicación de las clases. Investigar de forma read-only: ¿hay un
  `exportTags-rt` que también declare el tipo? ¿el `module.xml` lo exporta? ¿`BAbstractSubstitutePxView` (la
  superclase) vive en `-rt`? Es la pregunta que decide si el riesgo es real o si hay una pieza que no vimos.
- **N2 (HIGH)** — `niagaraDriver`: el driver sobre el que corre todo el join (`BNiagaraStation`,
  `BNiagaraNetwork`, `BNiagaraProxyExt`, los imports de history/file/schedule que B266 nombró sin abrir).
  **Medir antes de sembrar sub-gaps** (regla e2: contar sobre `vineflower/`, NO sumar pipelines).
- **N3 (MED)** — los 86 apartados de `docExportTags-N4.14-guide.md`, **ya preservados y con gate e3 pasado**.
  Aplicar el patrón de [Bloque 269]: RESUELVE / MATIZA / AGREGA / **lo que la doc NO resuelve**. Hacer el
  back-fill de la celda de bloque citante en `SOURCES.md`.
- **N4 (MED)** — el canal de join como superficie de seguridad: `BConnectInfo` transporta credenciales como
  **parámetro de acción serializado** sobre Fox ([Bloque 266] §266.5), y con `useFoxs=false` viaja sin TLS.
  Además la contraseña es una columna `Prop` normal en `BJoinProfileManager` ([Bloque 267] §267.4).
  **Aplicar la regla adoptada**: todo claim de permisos se verifica contra la semántica real del framework
  antes de escribirse (4 de 4 fallaron en el focus `tags`).
- **N5 (BLOQUEADO, requires-execution)** — reproducir el fallo de N1 exige una station viva sin el jar `-wb`.
  **No cuenta como investigable** (§8).
- **N6 (LOW, pendiente)** — B414 §414.5 no pudo resolver read-only qué hace Niagara cuando una station
  propia carga su BOG con un tipo no resuelto (p.ej. `exportTags:PxViewTag` en un JACE). Requiere encontrar
  `ValueDocDecoder` o el mecanismo de arranque de estación en las fuentes decompiladas de `baja`/`nre`.

## Blocked gaps

- N5 — needs: station viva (JACE-class supervisor sin perfil `-wb`) · tried: análisis estático de BlacklistTypeResolver (B414 §414.4) → confirma omisión silenciosa lógicamente, pero no produce fallo observado; hardware inaccesible.

## Clasificación (§8)

- **read-only-investigable**: **2** (N4, N6). **requires-execution / blocked**: 1 (N5).
- **Coverage metric**: **3 / 6** (3 bloques escritos, N1, N2 y N3 cerrados).
- **Próximo gap**: **N4**.

## Historia de iteración

| It | Fecha | Gap | Bloque | Hallazgo | Delegado? · tier |
|---|---|---|---|---|---|
| (bootstrap) | 2026-07-24 | — | — | focus PLANNED | — |
| 1 | 2026-08-09 | N1 | B414 | riesgo B267 MITIGADO por diseño: BSubstitutePxView en árbol del SUPERVISOR (no BOG del JACE); riesgo real = SUPERVISOR sin wb | no · inline |
| 2 | 2026-08-09 | N2 | B415 | niagaraDriver-rt: 106 clases; BNiagaraNetwork→BNiagaraStation (device-proxy)→BNiagaraProxyExt (pointId+mid)+BPointChannel (sub batch Fox); imports history/file/schedule por canal Fox nombrado | no · inline (sonnet) |

| 3 | 2026-08-09 | N3 | B416 | guía oficial exportTags: RESUELVE flujo Join+credenciales, MATIZA SubstitutePxView supervisor-side y merge-inteligente, AGREGA workflow commissioning+BFormat+licencia virtual-points+CategoryFilter top-down; la doc NO resuelve BlacklistTypeResolver ni credenciales-en-claro ni worker/cola | no · inline |

**Resume condition**: focus ACTIVE desde It 1. Próximo: N4 (seguridad canal de join).
