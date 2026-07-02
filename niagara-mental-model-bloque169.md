# Block 169 — chihuahua MX60 (`-rt/-ux`): equipment reader + modelo de config/estado (auto-provisioning, slots persistentes)

> **WHAT** — Cómo el módulo **chihuahua** (cliente Honeywell MX60) descubre y auto-aprovisiona su parque de equipos (6 plantas × 3 tipos de monitor → 88 componentes de equipo), cómo el `ChiEquipmentReader` recorre ese árbol y lo serializa a JSON para `/api/equipment`, cómo se construye el JSON de `/api/config`, y dónde vive el estado mutable del dashboard (`userThemes` por usuario, `alarmLatches`, `auditLog` ring).
>
> **Focus:** **chihuahua** (Cliente/Honeywell/MX60).
> **Idioma:** Español.
>
> **Sources (alias):**
> - `RT` = `chihuahua-rt/src/com/angeles/chihuahua/components/`
> - `UX` = `chihuahua-ux/src/com/angeles/chihuahua/ux/`
>
> **Markers legend:**
> - `[CERT]` — verificado leyendo `file:line` de primera mano (código fuente del módulo).
> - `[INFER]` — deducción del autor a partir del código, no una línea literal.
>
> `.env.local` **NO** fue leído (fuera de alcance).
>
> **Capa 26.** Continúa [Block 163].

---

## 169.1 — El árbol de 4 niveles: jerarquía de auto-provisioning

La arquitectura v4 es una jerarquía de 4 niveles enraizada en el servicio de estación. Cada nivel se auto-crea a sí mismo o a sus hijos en su propio `started()`, de forma idempotente. El integrador solo suelta **un** componente (`ChiDashboardService`) en `Config > Services` y todo lo demás nace solo `[CERT]` `RT/BChiDashboardService.java:40-49`.

```
ChiDashboardService (BAbstractService)          ← nivel 1: servicio de estación
  └── Planta-N (BPlanta), N=1..6                 ← nivel 2: contenedor por planta
        ├── UpMonitor        (BChiUpMonitor)     ← nivel 3: monitor por tipo
        ├── CarcamoMonitor   (BChiCarcamoMonitor)
        └── DataloggerMonitor(BChiDataloggerMonitor)
              └── {BChiUp | BChiCarcamo | BChiDatalogger}  ← nivel 4: equipo
```

| Nivel | Clase | Se crea en `started()` de | Cita |
|-------|-------|----------------------------|------|
| 1 → 2 | `BPlanta` × 6 | `BChiDashboardService.started()` | `[CERT]` `RT/BChiDashboardService.java:251-279` |
| 2 → 3 | 3 monitores | `BPlanta.started()` vía `ensureChild()` | `[CERT]` `RT/BPlanta.java:102-129` |
| 3 → 4 | N equipos | `BChi*Monitor.started()` vía `ensureX()` | `[CERT]` `RT/BChiUpMonitor.java:159-174` |

**Ordering crítico:** el servicio siembra los slots `planta` + `label` del `BPlanta` **ANTES** de `add()`, porque Niagara auto-arranca el hijo en `add()` y el `started()` del `BPlanta` (y de sus monitores) leen esos slots de inmediato. Sembrarlos después es una carrera que "siempre pierde en el primer restart" `[CERT]` `RT/BChiDashboardService.java:258-267`.

---

## 169.2 — El seed filtrado por planta: cómo un monitor siembra solo lo suyo

Cada monitor lleva una tabla estática `*_DATA` con **todo** el parque de su tipo (las 6 plantas juntas), y en `started()` lee el índice de planta de su padre (`BPlanta.planta`) para filtrar y sembrar **solo** las filas de esa planta `[CERT]` `RT/BChiUpMonitor.java:192-247`.

Flujo (idéntico en los 3 monitores):
1. `readParentPlantaIndex()` — lee `getParent().getProperty("planta")` como `BStatusNumeric` → int 1..6; devuelve 0 si falta `[CERT]` `RT/BChiUpMonitor.java:181-190`.
2. Guard: si `plantaIdx < 1 || > 6` → warning + skip del seed `[CERT]` `RT/BChiUpMonitor.java:166-170`.
3. Loop sobre `UP_DATA`: `if (dataPlanta != plantaIdx) continue;` — filtro por planta `[CERT]` `RT/BChiUpMonitor.java:196-199`.
4. Idempotencia: `if (get(slotName) != null) { seq++; continue; }` — no re-crea slots existentes `[CERT]` `RT/BChiUpMonitor.java:202-206`.
5. Tras `add()`, siembra identidad (`label`, `planta`, `positionX`, `positionY`) vía overloads `set(Property, ...)` y añade `wsAnnotation` para el layout de Wire Sheet `[CERT]` `RT/BChiUpMonitor.java:208-237`.

**Segundo pase de self-heal (solo UP):** re-siembra `planta`/`label` de `BChiUp` legacy cuyo `planta == 0.0` (firma de despliegues previos a que el seeding estuviera cableado). El guard `!= 0.0` lo hace idempotente `[CERT]` `RT/BChiUpMonitor.java:249-278`.

**Formato del seed:** `{ slotName, label, planta, positionX, positionY }`. Naming del slot = id en mayúsculas con `-`→`_` (ej. `"up01-p1"` → slot `UP01_P1`, label `"UP01"`) `[CERT]` `RT/BChiUpMonitor.java:66-71` / `21-27`.

---

## 169.3 — Conteo real del parque (6 plantas × 3 tipos)

Conteos **verificados** directamente sobre las tablas seed (no sobre el javadoc, que está desactualizado — ver nota abajo):

| Tipo | Tabla seed | Filas | Cita |
|------|-----------|-------|------|
| UP (`BChiUp`, incluye carriers/especiales) | `UP_DATA` | **77** | `[CERT]` `RT/BChiUpMonitor.java:71-157` |
| Cárcamo (`BChiCarcamo`) | `CARCAMO_DATA` | **6** | `[CERT]` `RT/BChiCarcamoMonitor.java:61-68` |
| Datalogger (`BChiDatalogger`) | `DT_DATA` | **5** | `[CERT]` `RT/BChiDataloggerMonitor.java:61-67` |
| **TOTAL** | | **88** | `[INFER]` suma de las tres tablas |

Distribución de UP por planta (col 3 del seed): P1=25 (20 UP + 5 CARRIER), P2=4, P3=18, P4=4, P5=9, P6=17 `[CERT]` `RT/BChiUpMonitor.java:71-157`.

> **Nota de deriva (stale docs):** el javadoc del servicio afirma "Planta-3 → UpMonitor (15)" y el comentario del seed dice "60 UPs total" `[CERT]` `RT/BChiDashboardService.java:31` / `RT/BChiUpMonitor.java:68`, pero la tabla real tiene **18** filas en P3 y **77** UP en total. La estimación "~68 equipos" del brief también queda por debajo del conteo verificado (88) `[INFER]`. La fuente de verdad es la tabla, no el comentario — el propio módulo lo advierte: las fixtures del prototipo "han divergido y ya no se mantienen sincronizadas" `[CERT]` `RT/BChiUpMonitor.java:17-20`.

**Planta-6 / Oficinas (deuda técnica conocida):** las 15 UD (split) + 1 HiRef + 1 Liebert (CRAC) se modelan como `BChiUp` "solo para tracking", aceptando el mismatch físico; el guard de umbral-cero salta las protecciones inaplicables `[CERT]` `RT/BChiDashboardService.java:34-38` / `RT/BChiUpMonitor.java:138-139`.

---

## 169.4 — El id canónico: de slot name a id de frontend

El id que ve el frontend se deriva del slot name con **una sola regla uniforme**: minúsculas + reemplazar `_` por `-` `[CERT]` `UX/ChiEquipmentReader.java:314-318`.

| Categoría | Slot | Id frontend |
|-----------|------|-------------|
| UP estándar | `UP01_P1` | `up01-p1` |
| Carrier | `CARRIER_1` | `carrier-1` |
| Cárcamo | `C5_P2` | `c5-p2` |
| Datalogger | `DT_P1_HP` | `dt-p1-hp` |
| Especial (sin sufijo `_P`) | `LABORATORIO` | `laboratorio` |

`[CERT]` `UX/ChiEquipmentReader.java:36-46`. La regla única funciona porque los especiales nunca tuvieron underscore en su id, así que lowercase los deja idénticos `[CERT]` `UX/ChiEquipmentReader.java:304-310`.

**Clasificación por prefijo** (usada por el helper de alarmas al parsear ords de driver): `UP*`→up, `DT*`→datalogger, `C[0-9]*`→carcamo (el dígito guarda contra otros equipos C-prefijados) `[CERT]` `UX/ChiEquipmentReader.java:350-361`.

**Planta del equipo:** primario = lee el slot numérico `planta`; fallback = parsea `_P{n}` del slot name `[CERT]` `UX/ChiEquipmentReader.java:381-421`.

---

## 169.5 — El reader: recorrido del árbol y serialización a `/api/equipment`

`ChiEquipmentReader` es una clase `final` con constructor privado (todo estático). Su raíz **no** es el árbol de driver v2 sino el servicio: `station:|slot:/Services/ChiDashboardService` `[CERT]` `UX/ChiEquipmentReader.java:70`.

Recorrido (`readAllFromService`): loop externo Planta-1..6, y por cada planta lee los 3 monitores por nombre (`UpMonitor`/`CarcamoMonitor`/`DataloggerMonitor`), y de cada monitor itera sus hijos `BComponent` `[CERT]` `UX/ChiEquipmentReader.java:132-144` / `175-214`.

**Arquitectura de testabilidad (2 capas):** como `getProperty`/`get`/`getPropertiesArray` son `final` en Niagara 4.14 (no se pueden stubbear), la capa que lee slots (`readSlotsFromUp` etc., requiere estación Windows) se separa de la capa que serializa (`serializeUpData` etc., Java puro WSL-testable) mediante DTOs planos `UpData`/`CarcamoData`/`DataloggerData` `[CERT]` `UX/ChiEquipmentReader.java:48-57` / `855-946`.

**Discriminación de fallas (nullable readers):** los readers de telemetría devuelven `null` cuando el `BStatus` tiene fault/down/stale/disabled/null, y registran el código en un mapa disperso `pointStatus` que solo lleva los puntos NO-ok `[CERT]` `UX/ChiEquipmentReader.java:560-630` / `650-663`. Los umbrales (`sobrecarga*`, `antifrezze*`, `umbral*`) usan `nullIfZero`: 0.0 = "no configurado por el operador" → `null` al frontend (REQ-701) `[CERT]` `UX/ChiEquipmentReader.java:515-524`.

**Forma del DTO de equipo (`/api/equipment`):** objeto raíz con `_meta` + array `equipment` `[CERT]` `UX/ChiEquipmentReader.java:270-275`. Cada UP:

```json
{ "id","type":"up","label","planta","position":{x,y},"status","ord","scheduleOrd",
  "summary":{ tempZona, ..., effectiveSetpoint, "pointStatus":{...} },
  "alarmLatches":{...} }
```

- `status` UP = `"cooling"` si fan/comp1/comp2 en ON (null-safe con `Boolean.TRUE.equals`), si no `"standby"` `[CERT]` `UX/ChiEquipmentReader.java:683-686`.
- `status` cárcamo/datalogger = `"offline"` si `state == -1`, si no `"online"` `[CERT]` `UX/ChiEquipmentReader.java:749` / `778`.
- `ord` se reconstruye programáticamente: `DRIVER_TREE_ORD + "/Planta" + plantaIdx + "/UpMonitor/" + slotName` `[CERT]` `UX/ChiEquipmentReader.java:687-688`.
- `alarmLatches` se emite **fuera** de `summary`, como valor JSON crudo (nunca null; `"{}"` por defecto) `[CERT]` `UX/ChiEquipmentReader.java:737-741`.
- `_meta` lleva `site`, `totalCount`, `byType`, `byPlanta`, `plantas[]` (labels `Planta1..Planta5`,`Oficinas`), `positionUnit:"percent"` y `zones` (polígonos SVG por planta) `[CERT]` `UX/ChiEquipmentReader.java:808-848` / `74-82`.

---

## 169.6 — `/api/config`: HEAD constante + monitorOrds programático + TAIL

`/api/config` **no** es una constante hardcodeada monolítica: es `HEAD + monitorOrds{...} + TAIL`, donde el bloque intermedio se construye en loop para no mantener 18 líneas a mano `[CERT]` `UX/BChiServlet.java:378-423`.

- **HEAD** (`CONFIG_JSON_BASE_HEAD`): `siteName`, `navigation[]` (home/alarms/schedules/equipment/histories), y el bloque `api{}` con ~13 endpoints; termina en coma para encadenar `monitorOrds` `[CERT]` `UX/BChiServlet.java:83-107`.
- **monitorOrds** (dinámico): 18 entradas = `{ups,carcamos,dataloggers} × Planta 1..6`. Clave `"{typeKey}-{n}"` (ej. `ups-1`), valor `MONITOR_ORD_PREFIX + "/Planta" + n + "/" + monitor` `[CERT]` `UX/BChiServlet.java:394-420`.
- **TAIL** (`CONFIG_JSON_BASE_TAIL`): empieza en coma; lleva `colors`, `pollMs` (home 8000 / alarmCounts 20000 / restFallbackMs 5000), `alarms` (maxStored 200 / dedupWindowMs 30000), `bajaDebounceMs` `[CERT]` `UX/BChiServlet.java:115-132`.

**Contraste `/api/config` vs `/api/equipment`:** `config` es **estático + estructura** (navegación, endpoints, ords de monitores, colores, tuning de polling) construido con constantes + un doble loop determinista; `equipment` es **datos vivos** (telemetría, status, latches) leídos del árbol de componentes en tiempo real `[INFER]`.

---

## 169.7 — Dónde vive el estado mutable del dashboard (slots persistentes)

`BChiDashboardService` declara tres slots de estado mutable vía `@NiagaraProperty`. La persistencia depende del flag `TRANSIENT`:

| Slot | Persistente | Formato | Cita |
|------|-------------|---------|------|
| `userThemes` | **Sí** (.bog) — sin `TRANSIENT` | `"user1=theme;user2=theme;"` | `[CERT]` `RT/BChiDashboardService.java:61-67` |
| `auditLog` | **Sí** (.bog) — sin `TRANSIENT` | JSON-lines, ring ≤ 500 | `[CERT]` `RT/BChiDashboardService.java:68-76` |
| `controlLockContentionCount` | **No** — `SUMMARY \| TRANSIENT` | int | `[CERT]` `RT/BChiDashboardService.java:54-60` |

### 169.7.1 — `userThemes` (tema por usuario)
Formato plano `user=theme;` en un único slot String. `getThemeForUser(user)` parsea el mapa y devuelve `"light"`/`"dark"` (default `"dark"` si falta) `[CERT]` `RT/BChiDashboardService.java:562-582`. `setThemeForUser(user, theme)` hace upsert; rechaza usernames con `;` o `=` para proteger el formato plano, y solo acepta valores `"light"`/`"dark"`. Ambos `synchronized` para POSTs concurrentes `[CERT]` `RT/BChiDashboardService.java:590-620`.

### 169.7.2 — `auditLog` (ring buffer)
JSON-lines delimitado por `\n`; shape `{"ts","user","action","ord","oldValue","newValue"}` `[CERT]` `RT/BChiDashboardService.java:70-71`. `appendAudit(jsonLine)` añade y recorta el ring vía `_trimAuditRing`, que dropea líneas por la cabeza (más viejas primero) al pasar `MAX_AUDIT_ENTRIES = 500` `[CERT]` `RT/BChiDashboardService.java:641-725`. Ambos `synchronized` sobre el mismo monitor que los themes. **Boundary:** `chihuahua-rt` NO importa `chihuahua-ux`; los helpers son String/slot puros, sin librería JSON `[CERT]` `RT/BChiDashboardService.java:636-638`.

### 169.7.3 — `alarmLatches` (persistido por-equipo, no en el servicio)
Los latches de alarma **no** viven en el servicio sino en el slot `alarmLatches` de **cada `BChiUp`** (`.bog`, JSON `"{}"` cuando no hay latches) `[CERT]` `UX/ChiEquipmentReader.java:472-477` / `900-904`. El servicio los orquesta: en `started()` **reconcilia** `trippedFlags` (en-memoria, se vacía en restart) desde el JSON durable de `alarmLatches`, mapeando cada key vía `LATCH_TO_TRIPPED_KEY`, para no perder ~10s de enforcement `[CERT]` `RT/BChiDashboardService.java:331-439` / `756-770`. El auto-latch en el flanco de subida escribe entradas `{"latched":true,"latchedAt":<epochMs>,"latchedBy":"system-cov","note":...}` bajo el lock por-ord `[CERT]` `RT/BChiDashboardService.java:1387-1422`.

---

## 169.8 — Ciclo de vida y concurrencia (contexto de soporte)

- **controlTick** cada 10s vía `ScheduledExecutorService` (portable entre iSMA 4.13 / Honeywell 4.14, evita `BRelTime`) — evalúa protecciones de todos los `BChiUp`, purga latches viejos (>30 días, cada 60º tick), recomputa setpoints de SCHEDULE y sincroniza slots de protección `[CERT]` `RT/BChiDashboardService.java:299-324` / `788-833`.
- **Locking:** `ReentrantLock` por-ord con `tryLock(500ms)`; en timeout incrementa `controlLockContentionCount`. Orden de lock: serviceLock → upLock → slotLock `[CERT]` `RT/BChiDashboardService.java:475-498` / `221-225`.
- **Acciones Workbench:** `exportLinks`/`importLinks` delegan en `ChiLinkHelper` para volcar/restaurar todos los links de componentes a `^exports/chih-links.json` `[CERT]` `RT/BChiDashboardService.java:1452-1497`.

---

## 169.x — Connections

- **[Block 163]** — bloque predecesor de esta capa; este bloque lo continúa (`Capa 26`).
- **[Block 166]** — `alarmLatches` es estado **compartido** con el subsistema de alarmas: el mismo slot por-`BChiUp` que aquí se lee para el DTO de equipo es el que produce/consume el pipeline de alarmas (`ChiAlarmHelper.latchAlarm` en `chihuahua-ux`, cuyo shape replica `_autoLatchProtection` en `-rt`) `[CERT]` `RT/BChiDashboardService.java:1362-1366`. Ver Block 166 para el lado de alarmas.
- **Contraparte comparativa — Reflow (B143/B145):** el modelo de estado compartido de chihuahua (slots `@NiagaraProperty` persistidos en `.bog`: `userThemes`, `auditLog`, `alarmLatches` por-equipo) es el análogo del `config.json` shared-state de Reflow documentado en B143/B145. Diferencia clave `[INFER]`: chihuahua persiste el estado **dentro del station database** (slots Niagara, sobreviven restart vía `.bog`) en lugar de un archivo JSON externo; el ring de `auditLog` y el mapa plano `userThemes` son el equivalente funcional al shared-state file de Reflow, pero atados al ciclo de vida del componente Niagara.
