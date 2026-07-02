# Block 173 — chihuahua MX60 (`-rt`): export/import de links (ChiLinkHelper, ords slot-path estables, gotcha setpoint)

> **WHAT** — Documenta el subsistema de EXPORT/IMPORT de links del módulo **chihuahua** (gap C11): cómo `ChiLinkHelper` serializa los `BLink` del árbol de componentes a `file:^exports/chih-links.json` usando ORDs slot-path estables (sobreviven al re-drag), cómo `importLinks` los re-establece de forma idempotente, y el gotcha histórico de pérdida de setpoint/effectiveSetpoint.
> **Focus:** **chihuahua** (Honeywell/MX60). Español.
> **Sources (alias):**
> - `RT` = `chihuahua-rt/src/com/angeles/chihuahua/components/`
> - `RT/ChiLinkHelper.java` — helper estático serialize/parse/collect/import.
> - `RT/BChiDashboardService.java` — `@NiagaraAction` `exportLinks`/`importLinks` + delegados `doExportLinks`/`doImportLinks`.
> - `.env.local` **NO leído** (fuera de scope).
> **Markers:** `[CERT]` = verificado contra código con file:line real · `[INFER]` = deducción razonada no literal.
> **Capa 26.** Continúa [Block 163].

---

## 173.1 — Superficie de acción: dos `@NiagaraAction` en el service

El subsistema se expone al integrador como dos acciones frozen sobre `BChiDashboardService`, invocables por Workbench (right-click → Actions). Se declaran vía `@NiagaraAction` `[CERT]` `RT/BChiDashboardService.java:81` (`exportLinks`) y `[CERT]` `RT/BChiDashboardService.java:84` (`importLinks`), con sus slots auto-generados por Slot-o-Matic `[CERT]` `RT/BChiDashboardService.java:181` y `[CERT]` `RT/BChiDashboardService.java:198`.

Cada acción es un **delegado fino** hacia `ChiLinkHelper` — toda la lógica vive en el helper (que sólo importa `javax.baja.*`, sin dependencia a `chihuahua-ux`) `[CERT]` `RT/ChiLinkHelper.java:24`:

- `doExportLinks()` → `collectLinks(this)` + `buildJson(...)` + `writeFile(...)`, y loguea el conteo a INFO `[CERT]` `RT/BChiDashboardService.java:1452`.
- `doImportLinks()` → `readFile()` + `parseJson(...)` + `importLinks(Sys.getStation(), dtos)`, resolviendo ORDs contra la raíz de estación `Sys.getStation()` `[CERT]` `RT/BChiDashboardService.java:1489`.

Ambos delegados envuelven todo en try/catch → `LOG.warning`, nunca abortan la estación `[CERT]` `RT/BChiDashboardService.java:1462`.

> Nota de deploy: agregar slots frozen nuevos obliga a bump de `vendorVersion` (`./deploy.sh rt --bump`) `[CERT]` `RT/BChiDashboardService.java:80`.

---

## 173.2 — Formato de export: ORD slot-path para estabilidad ante re-drag

El corazón de la estabilidad es `_ordOf(BComponent)`: serializa cada extremo del link con `c.getSlotPathOrd().encodeToString()` `[CERT]` `RT/ChiLinkHelper.java:398`, y **sólo** si eso lanza `UnresolvedException` cae al handle ORD `c.getHandleOrd().encodeToString()` `[CERT]` `RT/ChiLinkHelper.java:402`.

Por qué importa: el slot-path ORD (`slot:/Planta1/UpMonitor/...`) es **estable frente a re-drag** — si el integrador borra y vuelve a arrastrar el mismo componente en el mismo lugar del árbol, el path se conserva; el handle ORD (interno, basado en el id de instancia) NO sobreviviría. `[INFER]` Ésta es la garantía de portabilidad que permite re-importar un export tomado antes de un re-arrastre.

El JSON emitido por `buildJson` tiene forma `{"version":1,"exportedAt":<epoch-ms>,"links":[...]}` `[CERT]` `RT/ChiLinkHelper.java:165`, y cada link es un objeto con `sourceOrd`, `sourceSlot`, `targetOrd`, `targetSlot`, `enabled` `[CERT]` `RT/ChiLinkHelper.java:175`. Los strings se escapan con `_esc` (sólo `\` y `"`) `[CERT]` `RT/ChiLinkHelper.java:94`. Nótese que `unresolved` es campo del DTO en memoria pero **NO se serializa** al JSON `[CERT]` `RT/ChiLinkHelper.java:62`.

Archivo destino: `^exports/chih-links.json` `[CERT]` `RT/ChiLinkHelper.java:39`.

---

## 173.3 — Recolección: recorrido del árbol, incoming + outgoing, dedup

`collectLinks(root)` recorre recursivamente el subárbol desde el service y arma una lista deduplicada `[CERT]` `RT/ChiLinkHelper.java:416`. Por cada nodo captura links en dos direcciones:

- **Incoming** (el nodo es target): `node.getLinks()`, tomando `link.getSourceComponent()` y saltando links colgantes (`src == null`) durante el export `[CERT]` `RT/ChiLinkHelper.java:427`.
- **Outgoing** (el nodo es source): itera `node.getKnobs()` y por cada `Knob` toma `knob.getLink()` `[CERT]` `RT/ChiLinkHelper.java:451`.

La dedup usa un `LinkedHashMap` keyed por `LinkDTO.key()` = `"sourceOrd|sourceSlot->targetOrd|targetSlot"` `[CERT]` `RT/ChiLinkHelper.java:68`, preservando orden de inserción (primero gana) `[CERT]` `RT/ChiLinkHelper.java:418`. Durante la recolección, si CUALQUIER extremo no resuelve su slot-path ord, se marca `dto.unresolved = true` para que el import lo salte después `[CERT]` `RT/ChiLinkHelper.java:443`.

---

## 173.4 — Escritura atómica del archivo

`writeFile(json)` sigue el patrón atómico tmp-then-move `[CERT]` `RT/ChiLinkHelper.java:506`:

1. `_ensureExportsDir()` crea `^exports/` si no existe `[CERT]` `RT/ChiLinkHelper.java:490`.
2. Escribe a `^exports/chih-links-tmp.json` en UTF-8 `[CERT]` `RT/ChiLinkHelper.java:40` / `[CERT]` `RT/ChiLinkHelper.java:518`.
3. Borra el final existente y hace `move(tmp → final)` `[CERT]` `RT/ChiLinkHelper.java:537`.

Lectura simétrica: `readFile()` devuelve `null` si el archivo no existe (que `doImportLinks` trata como "no export file found" y aborta con WARNING) `[CERT]` `RT/ChiLinkHelper.java:544` / `[CERT]` `RT/BChiDashboardService.java:1483`.

El parser `parseJson` es un scanner por profundidad de llaves (brace-depth), tolerante a comillas escapadas, llaves anidadas en slot paths y coma final; descarta objetos con cualquier campo requerido vacío `[CERT]` `RT/ChiLinkHelper.java:209` / `[CERT]` `RT/ChiLinkHelper.java:294`.

---

## 173.5 — Re-establecimiento idempotente de links (importLinks)

`importLinks(station, dtos)` recrea los `BLink` en la estación con estas fases por DTO `[CERT]` `RT/ChiLinkHelper.java:657`:

- **(a)** Salta `dto.unresolved` → `skipped++` `[CERT]` `RT/ChiLinkHelper.java:686`.
- **(b)** Resuelve `targetOrd` vía `BOrd.make(...).resolve(station, null)`; falla → WARNING + skip `[CERT]` `RT/ChiLinkHelper.java:698`.
- **(c)** Verifica que el `targetSlot` exista con `getSlot` (ver §173.6) `[CERT]` `RT/ChiLinkHelper.java:723`.
- **(c.5)** **Idempotencia**: si ya existe un link sano equivalente, lo cuenta como `kept` y NO crea duplicado `[CERT]` `RT/ChiLinkHelper.java:733`.
- **(d)** `cleanStaleLinks(target, targetSlot)` — quita links colgantes (source==null) antes de recrear `[CERT]` `RT/ChiLinkHelper.java:740`.
- **(e)** Construye nombre de slot único `SlotPath.escape("link_"+src+"_"+tgt)` con sufijo `_N` ante colisión `[CERT]` `RT/ChiLinkHelper.java:743`.
- **(f)** `new BLink(...)` + `target.add(...)` + `link.activate()` `[CERT]` `RT/ChiLinkHelper.java:753`.

El guard de idempotencia real está en `_linkAlreadyPresent`: compara `targetSlot`, `sourceSlotName` y que el source vivo resuelva al mismo slot-path ord; un link colgante (source==null) NO cuenta como presente para forzar limpieza+recreación `[CERT]` `RT/ChiLinkHelper.java:590`. El `ImportResult` lleva tres contadores: `created` / `skipped` / `kept` `[CERT]` `RT/ChiLinkHelper.java:78`.

Detalle de robustez: tras `activate()` se verifica `link.isActive()` — si el source no resolvía al momento del import (dependencia de orden de creación), el framework deja el link montado pero inactivo; en ese caso se **remueve** el link huérfano y se cuenta como `skipped`, evitando falso positivo en el summary `[CERT]` `RT/ChiLinkHelper.java:763`.

---

## 173.6 — El gotcha setpoint/effectiveSetpoint: estado actual (FIXED + guard)

**Contexto del gotcha (memoria previa):** `importLinks` perdía el link de setpoint/effectiveSetpoint tras un re-add. Raíz documentada: un filtro `get()` sobre slots de tipo Action + `effectiveSetpoint` siendo un slot computado (no linkable).

Verificación contra el código actual — **está CORREGIDO**, en dos frentes distintos:

**Frente 1 — el filtro `get()` sobre Actions: FIXED.** La fase (c) ahora usa `target.getSlot(dto.targetSlot)` en lugar de `get(...)` `[CERT]` `RT/ChiLinkHelper.java:723`. El comentario in-situ explica exactamente la regresión histórica: `BComplex.get(String)` sólo resuelve Properties y lanza `NoSuchSlotException` para slots Action/Topic, lo que **silenciosamente descartaba todo link cuyo target es una Action** — p.ej. la acción `set` de un writable point, que es precisamente cómo están cableados los links de setpoint/effectiveSetpoint (139 de esos links en el export de campo). `getSlot` devuelve el Slot para Property, Action Y Topic, o null `[CERT]` `RT/ChiLinkHelper.java:717`. Por tanto la causa raíz "filtro `get()` sobre Action slots" ya NO está presente.

**Frente 2 — `effectiveSetpoint` computado y la propagación de valor inicial: mitigado con guard de steady-state.** `effectiveSetpoint` no es target del link sino la fuente cuyo valor alimenta la Action `set`. El riesgo actual no es "perder" el link sino que `BLink.activate()` hace **propagación de valor inicial**: al reactivar un link cuyo target es una Action, escribe de inmediato el valor actual del source al controlador BACnet físico `[CERT]` `RT/ChiLinkHelper.java:649`. Si el import corre antes de que `BChiUp.effectiveSetpoint` haya recomputado su valor real, se comandaría el placeholder transitorio al equipo vivo `[CERT]` `RT/ChiLinkHelper.java:665`.

La defensa: `importLinks` **rechaza importar** mientras `!Sys.atSteadyState()`, devolviendo todos los DTOs como `skipped` `[CERT]` `RT/ChiLinkHelper.java:669`. `Sys.atSteadyState()` es true sólo cuando todo componente arrancó post-boot, momento en que el recompute de effectiveSetpoint ya corrió — haciendo la precondición segura automática en vez de depender de que el operador espere `[CERT]` `RT/ChiLinkHelper.java:662`.

**Conclusión:** el gotcha de pérdida de setpoint está resuelto (getSlot); el aspecto de effectiveSetpoint computado se reencuadra como riesgo de comando-de-placeholder, hoy cubierto por el guard `atSteadyState`. `[INFER]`

---

## 173.x — Connections

- **[Block 163]** — bloque previo de la cadena (Capa 26); este bloque lo continúa.
- **[Block 172]** — el `BatchLinkEditor` de Workbench (`-wb`) también manipula links, pero desde la capa de edición UI; es complementario a este export/import runtime, no lo reemplaza. `[INFER]`
- **Reflow** — no tiene equivalente de export/import de links; el subsistema de ORDs slot-path serializados a `^exports/chih-links.json` es específico de chihuahua MX60 y no existe en el módulo Reflow. `[INFER]`
