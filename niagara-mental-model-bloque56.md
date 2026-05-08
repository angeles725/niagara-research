# Bloque 56 — Points domain audit + ausencia de Java backend = decisión arquitectónica positiva

**Fecha**: 2026-05-07
**Método**: Audit del namespace `Ti` ($niagara.points) en app-readable.js + verificación de ausencia de `BReflowPointsCommands.java` o helpers Java específicos para points.
**Fuentes primarias**:
- `app-readable.js` líneas 5589-5625 (`Ti` namespace, definición pequeña)
- Search exhaustivo en `Reflow-Clean-177/.../commands/` y `/points/` — **0 archivos Java específicos de points**
- Bloque 53.5 (subscriber wrapper `me`), Bloque 49 (facets + status flags), Bloque 48 (RBAC visibility)

**Versión analizada**: Reflow producción 1.7.5 + Reflow-Clean-177.

---

## 56.0 Contexto, scope, qué corrige

### ¿Qué ES este bloque?

Audit corto del dominio Points — el más simple del lote. **Hallazgo principal**: Reflow NO tiene un wrapper pesado para points porque la abstracción canónica de BajaScript (subscribe + Ord.make + cursor) ya provee la API completa. La capa adicional sería **redundante**.

### Qué corrige / valida

| Bloque | Sección | Hallazgo | Validación |
|--------|---------|----------|-----------|
| 53.5.10 | $niagara namespace 14 sub-libs | "cada sub-lib individual requiere audit propio" | ✅ Validado — `points` es la sub-lib MÁS chica (1 método). No es indicador de incompletitud, es decisión positiva. |
| 53.5 | subscriber wrapper `me` (singleton + registry) | "wrapper para multiplexar subscribers Vue" | ✅ Confirmado: TODA la operación de points (read, subscribe, write) va por `me.subscribe()` directamente. NO hay wrapper redundante en `Ti`. |

### Pregunta unificadora

> Si MX60 va a tener componentes que muestran/escriben points, ¿necesito un wrapper como `$niagara.points` o uso BajaScript directo?

**Respuesta corta**: usá BajaScript directo (a través de los composables `useSubscribedOrds()` / `useSubscriber()` migrados del mixin `Tt`). Para writes, `point.invoke("override", value)` o `point.set(value, cx)` directo. **NO crear wrapper redundante**. Reflow validó esta decisión empíricamente.

---

## 56.1 `Ti` namespace — solo 1 método público

```js
Ti = {
    get $baja() { return Vue.prototype.$baja; },

    pointList: async function(
        ord,                                    // ord raíz
        sortByName = false,                     // sort opcional
        types = [
            "control:ControlPoint",
            "niagaraVirtual:NiagaraVirtualControlPoint",
            "kitControl:OneShot",
            "kitControl:BooleanConst", "kitControl:NumericConst",
            "kitControl:EnumConst",    "kitControl:StringConst",
            "vykonProUtil:GlobalNumericWritableCommand",
            "vykonProUtil:GlobalEnumWritableCommand",
            "vykonProUtil:GlobalBooleanWritableCommand",
            "vykonProUtil:GlobalStringWritableCommand"
        ],
        deepSearch = false,
        gatewayTypes = ["niagaraVirtual:NiagaraVirtualGatewayComponent"]
    ) {
        var component = await this.$baja.Ord.make(ord).get();
        var type = component.getType();
        var typesFilter = (types == null) ? null : (Array.isArray(types) ? types : [types]);
        var results = [];

        if (type.is("baja:INavNode")) {
            results = Si(ord, sortByName, typesFilter, null, null, gatewayTypes);
            // Si() es un helper externo recursivo (no en este namespace)
        }

        if (sortByName) {
            results.sort((a, b) => a.name < b.name ? -1 : a.name > b.name ? 1 : 0);
        }

        return results;
    }
};
```

### 56.1.1 Análisis del único método

**Función**: dado un ord raíz, listar TODOS los points dentro (recursivo si `deepSearch`), filtrados por una **whitelist de types** Niagara/kitControl/vykonProUtil.

**Default whitelist**: 11 types canónicos de "puntos manipulables":
- `control:ControlPoint` → todos los Numeric/Boolean/String/Enum points estándar
- `niagaraVirtual:NiagaraVirtualControlPoint` → puntos virtuales (proxy hacia gateways)
- `kitControl:*Const` → constantes (4 tipos)
- `kitControl:OneShot` → trigger especial
- `vykonProUtil:Global*WritableCommand` → comandos globales (4 tipos)

**Hallazgo arquitectónico**: la whitelist es **conocimiento del domain BAS** que Reflow encapsula. MX60 hereda este filter, posiblemente extendiéndolo (e.g., `bacnet:BacnetPoint`, `lonworks:*`).

### 56.1.2 Helper externo `Si(...)`

`Si` es una función recursiva que NO está dentro de `Ti` — es un helper externo definido cerca (no auditado en detalle aquí). Su signature aparente: `Si(ord, sortByName, typesWhitelist, ?, ?, gatewayTypes) → array of points`.

**Para MX60**: la recursión profunda con type filtering + gateway crossing es **algoritmo BAS no trivial**. Vale la pena extraerlo como utility separado, NO inlinear en cada componente.

### 56.1.3 Callsites del bundle

```
Línea 11147: Ti.pointList(r, o.equipment.unrestrictDeviceTypes)
Línea 11235: Ti.pointList(u, r.equipment.unrestrictDeviceTypes)
```

Solo **2 callsites en TODO el bundle** (123,301 líneas). Ambos vienen del **Equipment Manager** Vuex module (líneas 5648+, definición de `ji`) — un dominio pesado de Reflow que gestiona configuración de equipos (AHU, Boiler, Chiller, Cooling Tower, FCU, MUA, etc).

**Conclusión**: `Ti.pointList` es **utility específico** para el Equipment Manager, no una API general de points. El resto del bundle accede a points vía:
1. **Subscriber wrapper `me`** (Bloque 53.5) — lee/observa values de points
2. **BajaScript directo** — writes (`point.set(...)`, `point.invoke("override", ...)`)
3. **BatchResolve** — lectura masiva (Bloque 53.5.6)

---

## 56.2 Ausencia de Java backend — decisión correcta

### 56.2.1 Verificación empírica

Búsquedas realizadas:
- `find Reflow-Clean-177 -name "*Point*Commands*"` → **0 archivos**
- `find Reflow-Clean-177 -name "*.java" -path "*points*"` → **0 archivos**
- `grep "yi.spec.POINTS" app-readable.js` → 0 hits (POINTS no está en los 7 typeSpecs declarados en sección 53.5.12.2)

**Confirmación**: Reflow NO tiene Commands Java específicos para points. Todas las operaciones server-side van vía:
- `BControlPoint.set()` directo (BajaScript proxy)
- `BControlPoint.invoke("override", BString)` (BajaScript proxy)
- `BControlPoint.cursor()` para listings (BajaScript proxy)
- BQL via `$niagara.bql.query("station:|...|bql:select * from control:ControlPoint where ...")`

### 56.2.2 Por qué es decisión correcta

**Niagara provee toda la API canonical para points sin necesidad de wrapper**:

| Operación | API BajaScript | Necesita wrapper Java? |
|-----------|----------------|------------------------|
| Read value | `point.getOut()` o `point.get("out").getValue()` | ❌ No |
| Subscribe to changes | `subscriber.subscribe([point])` + `attach("changed", cb)` | ❌ No |
| Write value | `point.set(value, cx)` o `point.invoke("override", BValue)` | ❌ No |
| List all points under tree | BQL via `$niagara.bql.query(...)` | ❌ No |
| Get facets/units/range | `point.getFacets()` (Bloque 49) | ❌ No |
| Get status flags | `point.get("out").getStatus()` (Bloque 49) | ❌ No |
| RBAC check | `point.hasOperatorRead/Write` o slot pruning automático | ❌ No |

**Wrappers Java solo agregan valor cuando**:
1. Hay business logic compleja server-side que no se puede expresar en BQL pura (ej: `AlarmData.query` con post-processing complejo).
2. Hay agregación de datos de múltiples fuentes (ej: `HistoryData` cruza history database + facets + display formatting).
3. Hay enforcement de validation/RBAC custom (no cubierto por slot pruning automático).

Para points "vanilla" — read value, write value, subscribe — ninguna de estas justificaciones aplica. **MX60 hereda esta decisión: NO crear wrapper Java para points básicos**.

### 56.2.3 Cuándo SÍ crear wrapper en MX60

Casos legítimos para `BMx60PointsCommands`:
1. **Bulk operations**: write a 50 points atomically con rollback. BajaScript no soporta atomicity.
2. **Computed properties**: derivar valores que NO viven como slots (ej: "promedio de los últimos 5 reads"). BQL puede pero solo con history/timestamp data.
3. **Custom validation**: rules de negocio que requieren estado externo (ej: "no permitir setpoint X si la temperatura ambiente Y < threshold").
4. **Workflow state machines**: emergencyOverride con priority levels y audit (Bloque 48 LEVEL_1 Life Safety).

**Default**: **NO crear wrapper**. Solo agregar Commands Java cuando un caso concreto lo justifique.

---

## 56.3 Síntesis MX60 implications — minimal pero potente

| # | Patrón Reflow | Tag | Razón |
|---|---------------|-----|-------|
| 83 | Ausencia de `BReflowPointsCommands` — decisión consciente de NO wrappear lo que BajaScript ya cubre | **KEEP (decisión arquitectónica)** | MX60 → mismo principio: **NO wrappear lo que BajaScript ya cubre canónicamente**. Wrappers solo cuando agregan valor real (atomicity, business logic, validation custom). |
| 84 | `Ti.pointList` con whitelist de 11 types BAS canónicos | **KEEP (extender)** | MX60 → mantener la whitelist pero abrir a config (per-cliente o per-deployment). Agregar `bacnet:`, `lonworks:` si target. |
| 85 | Helper recursivo externo (`Si`) con gateway-crossing | **KEEP** | Algoritmo BAS no trivial. MX60 → utility separado testeable, no inlined. |
| 86 | Composable + BajaScript directo > namespace wrapper para domains simples | **NEW principio MX60** | "Default a abstracciones bajas; wrappear solo lo que justifica la complejidad." Ahorra LOC, evita capas redundantes. |

### Resumen agregado

- **37 KEEP** (+2 Bloque 56): no wrappear lo cubierto por BajaScript, whitelist BAS extensible, helpers recursivos específicos
- **27 IMPROVE** (sin cambios)
- **9 NEW** (+1): principio "abstracciones bajas por default"
- **5 SKIP** (sin cambios)

**Tabla MX60 acumulada: 78 entries**.

---

## 56.4 Antipatterns

**Ninguno nuevo en este bloque**. Points domain es clean — sin AP-27 (porque no hay helpers Java), sin AP-22 (no hay paginación), sin AP-26 (no hay magic toString fallback).

**Esto es un finding positivo**: cuando NO hay wrapper Java, NO hay deuda técnica server-side. Validación adicional de la decisión arquitectónica.

---

## 56.5 Próximos hilos

- **Bloque 57** — Schedule + Nav + File domains combinados. Predicción: AP-27 confirmado en BReflowNavCommands + BReflowFileCommands (porque siguen el patrón de Alarms/History). Schedule probablemente sin Java helper específico (usa BQL via `sa.query`).
- **Bloque 58** — Backups + Matrix + Util closeout + audit BaseServlet.java (CRÍTICO).
