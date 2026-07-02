# Block 171 — chihuahua MX60 (`-ux`): write-path frontend (WritePoint, XHR-POST vs baja-native, _bajaSetBroken)

> **QUÉ documenta:** el **write-path del frontend** del módulo **chihuahua** (MX60 Chihuahua BMS Dashboard, capa `-ux`): los dos mecanismos de escritura que conviven en el cliente ES5 IIFE — (1) **baja-native action invoke** (`baja.Ord.make(ord).get().then(p => p.<action>())`) y (2) **XHR POST a un servlet custom** (`/mx60/api/...`) —, el catálogo de qué escritura usa qué mecanismo, la realidad de `WritePoint._bajaSetBroken = true` (el setpoint **hoy cae siempre al XHR POST**, no a la rama `baja.set()`), la reconciliación contra la regla documentada "baja-native preferido" de `FRONTEND_ARCHITECTURE.md §6`, y el short-circuit de capability del lado cliente (`CapabilityStore.canWrite()`).
> **Foco:** **chihuahua** (`-ux`), MX60 Honeywell.
> **Idioma del corpus:** Español.
> **Fuentes:**
>   - alias **JS** = `chihuahua/chihuahua/chihuahua-ux/src/rc/js/`
>   - alias **DOC** = `chihuahua/FRONTEND_ARCHITECTURE.md` (raíz del módulo)
>   - `JS/lib/WritePoint.js` (172 líneas — helper canónico de escritura de punto; guard RBAC, guard in-flight, Branch 1 baja / Branch 2 REST / Branch 3 toast+reject, `_bajaSetBroken`, retry 423)
>   - `JS/app/AlarmLatchStore.js` (351 líneas — latch/unlatch via XHR POST, `resetAll` baja-native)
>   - `JS/app/UpThresholdStore.js` (224 líneas — threshold write-through via XHR POST)
>   - `JS/app/AlarmsManager.js` (450 líneas — `ackAlarms`/`ackAllFullDb` baja-native + `loadAll` read)
>   - `DOC §6` (regla "baja-native preferido sobre custom servlets")
> **Marcadores:** `[CERT]` = leído en fuente primaria, con cita `archivo:línea` **inmediatamente después del marcador**. `[INFER]` = deducción a partir de la fuente.
> **Nota:** `.env.local` **NO** fue leído (fuera de alcance por instrucción explícita).
> **Capa 26.** Continúa [Block 164] / [Block 170].

---

## 171.1 — Los dos mecanismos de escritura

El frontend de chihuahua escribe al equipo por **dos caminos distintos**, y a diferencia de Reflow (100 % baja-native, ver §171.5) los **mezcla**:

**Mecanismo A — baja-native action invoke.** Se resuelve un ORD con `baja.Ord.make(ord).get()` y sobre el componente devuelto se invoca una action o `set()` de Niagara. Es transaccional, pasa por el dispatcher estándar de Niagara y captura la identidad del operador vía `ContextThread`. Ejemplo canónico documentado `[CERT]` `DOC:222-228`:

```js
const result = await baja.Ord.make(ord).get().then(up => up.<action>(args));
```

**Mecanismo B — XHR POST a servlet custom.** Se abre un `XMLHttpRequest`, se setean `Content-Type: application/json` + `X-Requested-With: XMLHttpRequest` (el header que el guard del servlet exige — ver [Block 165]), y se hace `POST` a un endpoint bajo `/mx60/api/...`. La respuesta se parsea a mano y HTTP 200 **no** implica éxito (el servlet puede devolver 200 con `{error:...}`).

La regla del proyecto `[CERT]` `DOC:218-235` es **preferir A** ("baja-native preferido sobre custom servlets"), porque el patrón B con reflection sobre snapshots de cursor BQL **no persiste** al alarm DB y produce silent-failure (200 + counters incorrectos). Pero la realidad del código es **mixta**: las *actions* (ack/resetAll) sí son baja-native; los *writes de estado a slots custom* (setpoint por default, latch, unlatch, threshold) van por XHR POST.

---

## 171.2 — `WritePoint` y la realidad de `_bajaSetBroken = true`

`MX60.writePoint(ord, value)` es el helper canónico de escritura de punto. Su docstring lo describe como "BajaScript primary → REST fallback → Toast + reject" `[CERT]` `JS/lib/WritePoint.js:4-8` (tres ramas): Branch 1 baja, Branch 2 REST, Branch 3 toast+reject inviolable.

**El giro:** el flag de módulo `_bajaSetBroken` está **inicializado en `true` por default** `[CERT]` `JS/lib/WritePoint.js:39`. El comentario lo justifica: en iSMA-Niagara 4.13.2.18 (station MX60 Chihuahua) `baja p.set()` lanza `"Cannot read properties of undefined (reading 'getName')"` **siempre**, así que se marca roto de entrada para que el primer write de cada page-load vaya directo a REST `[CERT]` `JS/lib/WritePoint.js:30-38`.

Consecuencia — **la rama baja (Branch 1) está muerta bajo el default actual**: el `if` de entrada a Branch 1 exige `!_bajaSetBroken` `[CERT]` `JS/lib/WritePoint.js:136`, que con el default `true` es `false`, así que el flujo salta directo a `restFallback()` `[CERT]` `JS/lib/WritePoint.js:157-158`. El `POST` se arma contra `/mx60/api/setpoint` (o `cfg.api.setpoint`) `[CERT]` `JS/lib/WritePoint.js:89` con body `{ ord, value }` `[CERT]` `JS/lib/WritePoint.js:130`. La rama `p.set(value)` real vive en `[CERT]` `JS/lib/WritePoint.js:138` pero **nunca se ejecuta**, porque en todo el archivo `_bajaSetBroken` solo se asigna `true` (líneas 39, 143, 151) y **jamás vuelve a `false`** — es un latch unidireccional `[INFER]`.

**Reconciliación con la doc:** `DOC §6` dice "baja-native preferido", pero para el **setpoint** esa preferencia está anulada por default de fábrica — el setpoint es **XHR-POST-first, no baja-first** `[INFER]`. Esto no contradice la doc en su espíritu: §6 aplica a *actions* sobre components (ack/reset), y ésas sí son baja-native (§171.3). El setpoint es un `set()` de propiedad que iSMA 4.13.2 no soporta, de ahí el desvío a REST `[INFER]`.

Otros detalles de robustez de la rama REST (todos `[CERT]`):
- Retry HTTP **423 Locked** (lock por-ord del servlet, ver [Block 165]): espera 100 ms y reintenta hasta 3 veces `[CERT]` `JS/lib/WritePoint.js:96-104`.
- HTTP 200 **no** es éxito: parsea el body y rechaza si `parsed.error || parsed.status==='fail' || parsed.ok===false` `[CERT]` `JS/lib/WritePoint.js:108-124`.
- Guard in-flight por-ord (anti doble-click que produciría race físico) `[CERT]` `JS/lib/WritePoint.js:60-67`.
- Branch 3 (doble fallo baja+REST) → Toast **inviolable** + `reject` `[CERT]` `JS/lib/WritePoint.js:74-79`.

---

## 171.3 — Catálogo de writes (write × mecanismo × endpoint/ord × cita)

| Write (función) | Mecanismo | Endpoint / ORD | Cita |
|---|---|---|---|
| `writePoint(ord,v)` — setpoint | **B** XHR POST (baja diseñada pero muerta por `_bajaSetBroken`) | `POST /mx60/api/setpoint` `{ord,value}` | `[CERT]` `JS/lib/WritePoint.js:89,130,136,157` |
| `AlarmLatchStore.latch` | **B** XHR POST (optimista + rollback) | `POST /mx60/api/alarms/latch` `{ord,thresholdKey,note}` | `[CERT]` `JS/app/AlarmLatchStore.js:161,154-158` |
| `AlarmLatchStore.reset` (unlatch) | **B** XHR POST (optimista + rollback) | `POST /mx60/api/alarms/unlatch` `{ord,thresholdKey}` | `[CERT]` `JS/app/AlarmLatchStore.js:224,221` |
| `AlarmLatchStore.resetAll` | **A** baja-native action | `baja.Ord.make(ord).get() → up.resetAlarmas()` | `[CERT]` `JS/app/AlarmLatchStore.js:299-301` |
| `UpThresholdStore.set` | **B** XHR POST (write-through + re-fetch) | `POST /mx60/api/up/{ord}/threshold` `{name,value}` | `[CERT]` `JS/app/UpThresholdStore.js:109-111` |
| `AlarmsManager.ackAlarms` | **A** baja-native action | `baja.Ord.make('alarm:').get() → svc.ackAlarms({ids})` | `[CERT]` `JS/app/AlarmsManager.js:321-322` |
| `AlarmsManager.ackAllFullDb` | **Híbrido B+A**: POST collect-only, luego baja ack | `POST /mx60/api/alarms/ackAll` (solo junta UUIDs) → `svc.ackAlarms({ids})` | `[CERT]` `JS/app/AlarmsManager.js:369-371,412-413` |

**Reads (contexto, no write-path):** `UpThresholdStore.init` hace `GET /mx60/api/up/{ord}/thresholds` `[CERT]` `JS/app/UpThresholdStore.js:60`; `AlarmsManager.loadAll` hace `GET /mx60/api/alarms` `[CERT]` `JS/app/AlarmsManager.js:117`. El lado subscription/read está en [Block 170].

**Observaciones del catálogo:**
- El `ackAllFullDb` es la prueba viva de la migración de patrón: el POST al servlet quedó reducido a **collect-only** (junta la lista de UUIDs), y el ack real lo hace baja-native, porque el ack por servlet "corría OK pero no persistía el record state al alarm DB (BQL cursor devuelve snapshots detached)" `[CERT]` `JS/app/AlarmsManager.js:271-282,343-348`.
- `resetAll` bypassa `writePoint` a propósito (llama `baja.Ord` directo), por eso lleva su propio guard RBAC `[CERT]` `JS/app/AlarmLatchStore.js:274-286`.
- `latch`/`unlatch`/`threshold` siguen siendo XHR-POST-servlet: son writes de estado a slots custom (`alarmLatches`, thresholds UP) con semántica optimista + rollback, no actions estándar de Niagara `[INFER]`.

---

## 171.4 — Short-circuit de capability del lado cliente

Todos los writes chequean `MX60.CapabilityStore.canWrite()` **antes** de disparar baja o XHR, y si devuelve `false` muestran Toast "Sin permiso de escritura" y hacen `reject`/`return` temprano. Está replicado en cada call-site de escritura:

| Write | Guard capability |
|---|---|
| `writePoint` | `[CERT]` `JS/lib/WritePoint.js:48-57` |
| `resetAll` | `[CERT]` `JS/app/AlarmLatchStore.js:278-286` |
| `ackAlarms` | `[CERT]` `JS/app/AlarmsManager.js:301-310` |
| `ackAllFullDb` | `[CERT]` `JS/app/AlarmsManager.js:354-363` |

**Punto clave:** el guard es explícitamente **DECORATIVO** — el comentario de `WritePoint` lo dice literal: "CapabilityStore is DECORATIVE — the servlet 403 is authoritative" `[CERT]` `JS/lib/WritePoint.js:44-47`. La autoridad real de autorización vive en el backend (RBAC del servlet, ver [Block 164]); el short-circuit cliente solo mejora UX (evita disparar el path baja/REST y previene el `BoxError` de unhandled rejection para roles viewer) `[CERT]` `JS/lib/WritePoint.js:44-54`. Nótese que `latch`/`reset`/`UpThresholdStore.set` **no** llevan este guard cliente — dependen enteramente del 403 del servidor `[INFER]`.

---

## 171.5 — Contraste con Reflow (todo baja-native)

La `DOC §6` nombra a Reflow (módulo OEM `nmodsreflow`) como el **reference**: "100 % baja-native para todas las actions (ack, reset, etc.) — zero servlets de action wrapping" `[CERT]` `DOC:235`. La investigación de Reflow (B154/B155, ver [Block 170] y las notas de wiring cliente↔backend) confirma un frontend donde **cada** write viaja por `baja.Ord.make(...).get().then(c => c.<action>())`, sin ningún servlet de action.

chihuahua es el **counterpart mixto**: adoptó baja-native solo donde el servlet demostró silent-failure (ack, resetAll, y el ack-step de `ackAllFullDb`), pero mantiene XHR-POST para setpoint (forzado por el bug de `p.set()` en iSMA 4.13.2 → `_bajaSetBroken=true`), latch, unlatch y threshold `[INFER]`. La diferencia arquitectónica es medible: Reflow = 0 servlets de write; chihuahua = ≥4 endpoints POST de write vivos (`setpoint`, `alarms/latch`, `alarms/unlatch`, `up/{ord}/threshold`) más `ackAll` en modo collect-only `[INFER]`.

---

## 171.x — Connections

- **[Block 164]** — el gate RBAC del **servidor** que está detrás de todos estos writes; es la autoridad real (403) que el `CapabilityStore.canWrite()` cliente solo decora (§171.4).
- **[Block 165]** — la superficie de endpoints POST que consume el Mecanismo B (`/mx60/api/setpoint` con lock 423, `/api/alarms/latch|unlatch|ackAll`, `/api/up/{ord}/threshold`); guards `X-Requested-With`, envelope de error 200-con-`{error}`, `ReentrantLock` por-ord.
- **[Block 170]** — el **lado subscription/read** (seed, polling, `seedFromEquipment`, `loadAll`) que hidrata los stores que estos writes mutan de forma optimista.
- **Reflow B154/B155** — modelo de write **100 % baja-native, zero servlets de action** (`DOC:235`); es el counterpart de comparación frente al modelo **mixto XHR-POST + baja** de chihuahua (§171.5).
