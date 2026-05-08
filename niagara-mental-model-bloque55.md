# Bloque 55 — History domain audit cliente↔server + AP-27 confirmado sistémico + pattern fetchMethod switch

**Fecha**: 2026-05-07
**Método**: Cross-reference triple (`Sa` cliente + `BReflowHistoryCommands.java` + 5 helper classes en `/history/`) sobre Reflow 1.7.5 + Reflow-Clean-177. Foco específico: **buscar AP-27 sistémico** según hipótesis del Bloque 54.10.
**Fuentes primarias**:
- `app-readable.js` líneas 13510-13750 (`Sa` namespace)
- `BReflowHistoryCommands.java` (112 líneas, 7 métodos)
- `HistoryData.java` (663 líneas — más grande del domain), `HistoryGroups.java` (112), `HistoryList.java` (355), `HistoryIO.java` (103), `HistoryGhostSubscriber.java` (26)
- Bloque 54 (template AP-27, regla 11)

**Versión analizada**: Reflow producción 1.7.5 + Reflow-Clean-177 commit actual.

---

## 55.0 Contexto, scope, qué corrige

### ¿Qué ES este bloque?

Audit del **dominio History** de Reflow — el segundo más complejo después de Alarms. Cubre series temporales, devices, groups, paginación temporal, comparison mode (compareHistories), exports CSV/JSON, y diferentes serializadores Jackson custom.

**Misión de este bloque** (3 puntos):
1. **Validar la hipótesis de Bloque 54.10**: AP-27 (RBAC bypass via `null` Context + thread custom) es **sistémico** en helpers Reflow, no aislado a Alarms.
2. **Documentar el pattern `fetchMethod` switch** del cliente — Reflow expone DOS paths (yi.json vs REST custom) para la misma operación con flag de configuración.
3. **Mapear el dominio** completo cliente↔server.

### Qué corrige

| Bloque | Sección | Afirmación previa | Corrección/Confirmación |
|--------|---------|-------------------|-------------------------|
| 54 | 54.4.1 (AP-27 hipótesis sistémica) | "AP-27 confirmado en Alarms — pendiente validar en otros dominios" | ✅ **CONFIRMADO SISTÉMICO** — History tiene 11 sites de `BOrd.make().get(null)` + 2 threads custom + 1 `doPrivileged`. Mismo patrón exacto. |
| 54 | 54.10 próximos hilos | "audits Java siguientes van a buscar AP-27" | ✅ Confirmado. Probable hallazgo similar en Nav/File/CSV — predicción metodológica. |
| 53 | 53.5.10 ($niagara namespace patrón) | "wrapper namespace pattern" | Extendido — `Sa` agrega el patrón `fetchMethod` switch (cliente puede elegir transport). |

### Pregunta unificadora

> Si AP-27 es deuda técnica sistémica de Reflow, ¿cómo encara MX60 el diseño server-side desde día 1 para evitar contagiarse?

**Respuesta corta**: regla 11 OBLIGATORIA aplicada SISTEMÁTICAMENTE en TODA helper class. Pre-commit hook que rechaza `BOrd.make(...).get(null)` o `.get()` sin Context. Threads custom prohibidos en Commands path — usar `BJobService` que propaga Context. `AccessController.doPrivileged` solo en operaciones que el user invocante NO debería poder hacer (operaciones de sistema interno, ej: cache rebuild).

---

## 55.1 Cliente — `Sa` namespace ($niagara.history) líneas 13514-13750

### 55.1.1 Forma exterior

```js
Sa = {
    get $baja() { return Vue.prototype.$baja; },
    get $component() { return Vue.prototype.$component; },
    get $color() { return Vue.prototype.$color; },           // ← propio del domain (color theming)
    get compareKey() { return "|compare|"; },                // ← magic separator

    // Métodos REST custom (m.a)
    list(opts)        { /* m.a.get("/nmodsreflow/station/histories") */ },
    loadList(opts)    { /* m.a.get("/nmodsreflow/station/histories") — duplicado interno de list */ },
    loadGroups()      { /* m.a.get("/nmodsreflow/station/history-groups") */ },

    // Métodos serverSideCall (yi)
    loadDeviceTree()  { /* yi.json(yi.spec.HISTORY, "getDeviceTree") */ },
    loadDevices()     { /* yi.json(yi.spec.HISTORY, "getDevices") */ },
    generate()        { /* yi.json(yi.spec.HISTORY, "makeHistories", null) */ },

    // Híbrido — fetchMethod switch
    data(config) {
        // Si config.fetchMethod === "http" → REST m.a.get("/nmodsreflow/station/history-data" + qs)
        // Else                              → yi.json(yi.spec.HISTORY, "getData", config)
    },

    buildHistoryQueryString(opts) { /* serialize opts to URL query string */ }
};
```

### 55.1.2 Hallazgo nuevo — pattern `fetchMethod` switch

`Sa.data()` es el primer ejemplo en el bundle de **flexibilidad de transport** elegible por el cliente:

```js
data: async function(config) {
    var opts = Object.assign({ compareHistories: "", comparing: false }, config);
    // ... validation, transformación de paths ...

    if (opts.fetchMethod === "http") {
        var queryString = this.buildHistoryQueryString(opts);
        var response = await m.a.get("/nmodsreflow/station/history-data" + queryString);
        return response.data;
    } else {
        return await yi.json(yi.spec.HISTORY, "getData", opts);
    }
}
```

**¿Por qué dos paths?**

Hipótesis razonables (no confirmadas por código):
1. **Migración WIP**: el equipo Reflow estaba migrando de REST a serverSideCall (o viceversa) y dejó ambos paths para A/B testing.
2. **Performance**: REST puede ser más rápido para responses grandes (sin overhead de BString JSON-blob double-wrap del serverSideCall).
3. **Streaming**: REST permite streaming response, serverSideCall no.
4. **Caching**: REST tiene cache HTTP nativo (Cache-Control headers), serverSideCall no.

**MX60 implication**:
- **NEW pattern** considerar: exponer `fetchMethod` cuando es realmente útil (responses streaming, cache HTTP). Para todo lo demás, **un solo path** evita confusion + reduce maintenance burden.
- **Decisión per-domain en MX60**: trends/charts probablemente justifican REST (streaming + cache); CRUD simple probablemente solo serverSideCall.

### 55.1.3 Otros patterns en `Sa`

- **`compareKey: "|compare|"`** — magic string separator para comparing histories. Si user crea un history con `|compare|` en el nombre, rompe. **AP-29 cousin** — fragile string format.
- **Path normalization**: `replace(/(local:\|?)?(history:\|?){0,2}/, "")` — strip de prefixes Niagara (similar a `pe.clean` del Bloque 53.5.15 pero específico de history). Regex compleja, fragile ante prefix variations.
- **`Sa.list()` y `Sa.loadList()` ambos llaman al mismo REST endpoint** — duplicación interna. Probable refactor incompleto.
- **Heavy comparison mode** (`compareHistories`, `comparing`) — feature compleja del UI Reflow, probable trasladable a MX60 como "split-screen trend comparison".

---

## 55.2 Server-side — `BReflowHistoryCommands.java` (112 líneas, 7 métodos)

### 55.2.1 Header (idéntico al template)

```java
@NiagaraType(agent={@AgentOn(types={"nmodsreflow:ReflowService"}, requiredPermissions="r")})
public class BReflowHistoryCommands extends BComponent implements BIServerSideCallHandler { ... }
```

### 55.2.2 Métodos (todos thin wrappers — patrón uniforme)

| Método | Strict typing | Helper class | `cx` propagation |
|--------|--------------|--------------|------------------|
| `getList(comp, arg, cx)` | ✅ rechaza no-BComponent (return null) | `new HistoryList(options)` | ❌ no pasa cx |
| `getQuickList(comp, arg, cx)` | ✅ | `new HistoryList(options).getNodeJson()` | ❌ |
| `getData(comp, arg, cx)` | ✅ | `HistoryData.fromComponent(options)` | ❌ |
| `getGroupNames(comp, arg, cx)` | ⚪ N/A (sin params) | `HistoryGroups.getGroupNames()` | ❌ |
| `getGroupTree(comp, arg, cx)` | ⚪ N/A | `HistoryGroups.getGroupTree()` | ❌ |
| `getDeviceTree(comp, arg, cx)` | ⚪ N/A | `HistoryGroups.getDeviceTree()` | ❌ |
| `getDevices(comp, arg, cx)` | ⚪ N/A | `HistoryGroups.getDevices()` | ❌ |

**Patrón uniforme**: ningún método propaga `cx` a los helpers. **Mismo gap que Alarms (sección 54.3.3)**.

### 55.2.3 Particularidad — `getData` con replace `\\/ → /`

```java
return BString.make((String)json.toString().replace("\\/", "/"));
```

Jackson serializa `/` como `\/` por defecto (heritage JSON escaping JS-friendly). Reflow lo des-escapa post-serialization. **MX60**: configurar Jackson con `JsonGenerator.Feature.ESCAPE_NON_ASCII` y disable de slash escaping en lugar de string replace post-hoc.

---

## 55.3 Server-side helpers — AP-27 confirmado sistémico

### 55.3.1 Counts empíricos en `/history/` directory

| Archivo | LOC | `BOrd.make().get()` no-Context | `new Thread` | `AccessController.doPrivileged` |
|---------|----:|-------------------------------:|-------------:|--------------------------------:|
| `HistoryData.java` | 663 | **6 sites** (líneas 53, 97, 199, 268, 322, 360) | **1 thread** ("BReflowHistoryData.FromComponentTask.Task") | **1 doPrivileged** |
| `HistoryGroups.java` | 112 | **4 sites** (líneas 25, 51, 80, 98) | 0 | 0 |
| `HistoryList.java` | 355 | **2 sites** (líneas 155, 294) | 0 | 0 |
| `HistoryIO.java` | 103 | 0 | **1 thread** ("HistoryIO.refreshHistoryGroupCache.Task") | 0 |
| `HistoryGhostSubscriber.java` | 26 | 0 | 0 | 0 |
| **Total** | **1,259** | **12 sites** | **2 threads** | **1 doPrivileged** |

### 55.3.2 Sites concretos

```java
// HistoryData.java
Línea 53:  BOrd.make("history:").resolve().get();           // sin Context
Línea 97:  BOrd.make("history:").resolve().get();           // sin Context
Línea 199: BOrd.make("history:").resolve().get();           // sin Context
Línea 268: BOrd.make("history:").resolve().get();           // sin Context
Línea 322: BOrd.make(firstOrd).get(null);                   // EXPLICIT null
Línea 360: BOrd.make(lastOrd).get(null);                    // EXPLICIT null
Línea 69-71: AccessController.doPrivileged(new PrivilegedExceptionAction() {
                Thread thread = new Thread(task, "BReflowHistoryData.FromComponentTask.Task");
                thread.start();
                ...
             });

// HistoryGroups.java
Línea 25:  BOrd.make("history:").get();                     // sin Context
Línea 51:  BOrd.make("history:").get();                     // sin Context
Línea 80:  BOrd.make("history:").get();                     // sin Context
Línea 98:  BOrd.make("history:").get();                     // sin Context

// HistoryList.java
Línea 155: BOrd.make("station:|" + cfg.getSourceHandle()...).resolve().get();   // sin Context
Línea 294: BOrd.make("history:").resolve().get();                                // sin Context

// HistoryIO.java
Línea 65: Thread thread = new Thread(task, "HistoryIO.refreshHistoryGroupCache.Task");
```

### 55.3.3 Veredicto: AP-27 sistémico confirmado

**Hipótesis del Bloque 54.10 confirmada**.

Reflow tiene **al menos 23 sites de RBAC bypass potencial** entre Alarms (9 + 1 thread + 1 doPrivileged) y History (12 + 2 threads + 1 doPrivileged). Es **deuda técnica sistémica**, no aislada.

**Severidad agregada**: lo que el Bloque 54.4.2 marcó como "DEPENDE de ThreadLocal" para los ord lookups sin thread custom **se vuelve riesgo más grande** cuando ves que el patrón es uniforme — un solo refactor que introduzca thread pooling o async execution pyramid rompe el RBAC en MÚLTIPLES dominios simultáneamente.

**Estimación final**: si Reflow se desplegara en multi-tenant donde RBAC importa, AP-27 sería **HIGH severity unilateralmente** porque la presunción "ThreadLocal cubre" es frágil contra cualquier evolución.

### 55.3.4 GOOD pattern observado — `BOrd.make(...).resolve().get()`

Los helpers de history usan `BOrd.make("history:").resolve().get()` — el `.resolve()` antes del `.get()` valida que el ord apunta a algo concreto.

**MX60 implication — POSITIVE**: replicar `.resolve().get(cx)` sobre `.get(cx)` directo cuando uses ords con prefixes complejos. `.resolve()` da un ord canónico antes de leerlo. Beneficio: detección temprana de ords mal formados.

### 55.3.5 GOOD pattern — `HistoryGhostSubscriber.java`

Solo 26 líneas, sin `BOrd.make` problemático, sin threads custom. Es una **micro-utility class** — patrón "Single Responsibility" llevado al extremo. **MX60 implication**: los helpers chicos y específicos son más fáciles de auditar y mantener. Resistir la tentación de "all-in-one" classes de 600+ líneas (como `HistoryData.java`).

### 55.3.6 BAD pattern adicional descubierto — `HistoryIO.refreshHistoryGroupCache`

```java
// HistoryIO.java línea 65
Thread thread = new Thread(task, "HistoryIO.refreshHistoryGroupCache.Task");
```

Cache rebuild en thread custom. **A diferencia del thread de `HistoryData.query`**, este tiene una excusa razonable: cache rebuild es operación INTERNA que no debe respetar RBAC del user (es un job de sistema). Pero usar `new Thread(task)` raw NO es el patrón correcto.

**MX60 implication**:
- Para cache rebuilds y operaciones internas: usar `BJobService.createWorker()` o equivalent, NO `new Thread(task)` raw.
- `BJobService` provee Context propio (system context), audit trail, monitoring.
- `new Thread(task)` raw NO tiene ninguno de los beneficios y es más difícil de testear.

---

## 55.4 REST endpoints específicos del domain

```
GET /nmodsreflow/station/histories       → Sa.list / Sa.loadList → ¿Java handler?
GET /nmodsreflow/station/history-data    → Sa.data (cuando fetchMethod=http) → ¿Java handler?
GET /nmodsreflow/station/history-groups  → Sa.loadGroups → ¿Java handler?
```

**Audit pendiente — BaseServlet.java**: estos REST endpoints son servidos por BaseServlet `/*` (Bloque 51 confirmed). Audit del BaseServlet revelaría:
- Mapping path → method
- Si BaseServlet propaga `Context` correctamente al delegar a HistoryData (probablemente sí, dado que HTTP requests vienen con session authenticated → `req.getAttribute("niagara.context")`)
- Si hay validation/sanitization en el path antes de invocar helpers

**Predicción**: BaseServlet probablemente ES el path donde sí se propaga Context (porque es un servlet HTTP y Niagara binds Context al request). El bug AP-27 se activa solo cuando los helpers caen al `get(null)` con thread custom que pierde el ThreadLocal.

---

## 55.5 Síntesis MX60 implications — incrementales

| # | Patrón | Tag | Razón |
|---|--------|-----|-------|
| 74 | `Sa.data()` `fetchMethod` switch entre yi.json y REST | **NEW pattern (selective)** | Útil para responses streaming/cacheables (history-data). MX60 → exponer solo cuando hay diferencia real en use case. |
| 75 | `BOrd.make(...).resolve().get(cx)` (con `.resolve()`) | **KEEP** | Defense-in-depth contra ords mal formados. Patrón mejor que `.get(cx)` directo cuando hay prefix processing. |
| 76 | Path normalization regex `replace(/(local:\|?)?(history:\|?){0,2}/, "")` | **IMPROVE** | Frágil ante variaciones de prefix. MX60 → parser explícito de ord scheme. |
| 77 | Magic separator `compareKey = "|compare|"` | **IMPROVE** (AP-29 cousin) | Fragile ante user input que contiene la separator. MX60 → array structures, no string concatenation. |
| 78 | Methods duplicados (`Sa.list` y `Sa.loadList`) | **SKIP / cleanup** | Refactor incompleto. MX60 → no replicar duplicados. |
| 79 | Thin Commands wrappers para helpers complejos | **KEEP pattern** | Single responsibility, testable. Pero ojo: helpers DEBEN respetar regla 11. |
| 80 | `HistoryGhostSubscriber` micro-class 26 líneas | **KEEP pattern** | Single responsibility extreme — preferir multi-helpers chicos vs all-in-one 663-line class. |
| 81 | Cache rebuild en thread custom | **IMPROVE** | MX60 → `BJobService.createWorker()` no `new Thread()` raw. |
| 82 | Jackson `\\/` post-replace | **IMPROVE** | Configurar Jackson `JsonGenerator.Feature.WRITE_SLASH_AS_FORWARDSLASH` o equivalente. |

### Resumen agregado

- **35 KEEP** (+4): BOrd.resolve().get(cx) defensive, fetchMethod selective pattern, micro-utility classes, Commands→helpers thin
- **27 IMPROVE** (+2): path normalization regex parser, BJobService no raw threads
- **8 NEW** (sin cambios — fetchMethod selective ya cubierto bajo "considerar")
- **5 SKIP** (sin cambios)

**Tabla MX60 acumulada: 75 entries** (algunos overlaps con 73 del Bloque 54).

---

## 55.6 Antipatterns — ya existentes confirmados

No hay AP nuevos identificados en este audit. **AP-27 confirmado sistémico** (12 nuevos sites confirmados). AP-22 también probablemente sistémico (paginación skip+take in helpers — no audited en detalle pero predicción razonable).

---

## 55.7 Próximos hilos

- **Bloque 56 — Points domain**: cliente `Ti` en app-readable.js. Java backend desconocido (no hay BReflowPointsCommands en el listado — probable que use BajaScript directo sin Commands wrapper).
- **Bloque 57 — Schedule + Nav + File combined**: 3 dominios MEDIUM en un bloque eficiente. AP-27 search en cada uno.
- **Bloque 58 — closeout** + audit BaseServlet.java (CRÍTICO para confirmar Context propagation en path REST).
