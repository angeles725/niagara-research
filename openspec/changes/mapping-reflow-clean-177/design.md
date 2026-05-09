# Design: mapping-reflow-clean-177

**Phase**: sdd-design | **Date**: 2026-05-09
**Project**: niagara-research
**Source (READ-ONLY)**: `/home/cristian/modules/Prototipos/Reflow-Clean-177/`
**Output (WRITE)**: `/home/cristian/niagara-research/docs/mappings/reflow-clean-177/`
**Schema status**: LOCKED v1.0 — reusable for Analytics/MX60.

---

## 0. Architecture approach (one paragraph)

Documento mapping = artefacto estático versionado en git, no runtime. Forma: 1 `index.json` (queryable vía `jq`) + 1 `index.md` (humano paginado por dominio) + `schema.md` (contrato versionado) + N `domains/<x>.md` deep dives. El schema es **`core + extension`**: el bloque `core` es universal (10 campos), los bloques `extension` (`backend`, `frontend_vue`, `frontend_js`, futuro `analytics`) viven en namespaces propios y son aditivos. Generación = pipeline determinístico de 4 batches (skeleton → backend → frontend paralelo → validation), aprovechando los 3 docs existentes (`REFLOW-ARCHITECTURE-ANALYSIS.md`, `GAP-ANALYSIS.md`, `NIAGARA-INTEGRATION.md`) como fuente primaria — cero re-exploración del código en Tier 1.

---

## 1. Schema (LOCKED v1.0)

### 1.1 Core block — MANDATORIO en toda entrada

```json
{
  "schema_version": "1.0",
  "id": "string",
  "path": "string",
  "kind": "enum",
  "domain": "enum",
  "purpose": "string",
  "dependencies": ["string"],
  "loc": 0,
  "status": "enum",
  "source_doc": { "file": "string", "section": "string" },
  "verified_at": "YYYY-MM-DD"
}
```

| Campo | Tipo | Obligatorio | Descripción |
|-------|------|------|-------------|
| `schema_version` | string | Sí (top-level del documento, no por entrada) | `"1.0"` literal. Sirve para forward-compat. |
| `id` | string | Sí | Identificador estable. Regla: `path` relativo a repo root, normalizado (sin `./`, sin trailing slash). Ej: `nmodsreflow-rt/src/com/niagaramods/nmodsreflow/BReflowService.java`. **No** hash — el path ya es estable y legible. |
| `path` | string | Sí | Mismo valor que `id`. Se duplica para que jq queries por path funcionen sin truco. |
| `kind` | enum | Sí | Uno de: `java-class`, `vue-component`, `vue-view`, `js-store`, `js-mixin`, `js-plugin`, `js-api`, `js-lib`, `js-router`, `js-util`, `js-entry`, `config`, `resource-image`, `resource-icon`, `resource-template`, `compiled-class`, `compiled-jar`, `compiled-bundle`, `module-descriptor`. |
| `domain` | enum | Sí | Uno de: `service-container`, `ord-scheme`, `http-rest`, `http-websocket`, `bajascript-box`, `history`, `alarms`, `sync-config`, `backups`, `util`, `ux-widget`, `app-shell`, `state`, `dashboard`, `buildings`, `equipment`, `floorplans`, `config`, `navigation`, `cards`, `charts`, `points`, `profiles-rbac`, `schedules`, `pages`, `weather`, `maps`, `settings`, `websocket-ui`, `api-layer`, `mixins`, `plugins`, `lib`, `views`, `wizard`, `browser`, `layout`, `common`, `image-library`, `icons`, `build-artifact`, `excluded`. |
| `purpose` | string | Sí | 1 oración, máx ~150 chars. Ej: `"Root BComponent del módulo: 26 slots, lifecycle, spawnea SyncService + WebSocketAcceptor."` |
| `dependencies` | string[] | Opcional (vacío permitido) | Java: FQN de clase importada (`com.niagaramods.nmodsreflow.http.BaseServlet`). JS/Vue: ruta relativa al import (`@/store/modules/profiles`, `@/api/rest`, `@/components/cards/BaseCard`). |
| `loc` | integer | Sí | Líneas físicas (incluye comentarios y blancos). Para `compiled-*`/`resource-*`: `0`. |
| `status` | enum | Sí | `source` (Java/Vue/JS editable) \| `compiled` (.class/.jar/.bajadoc) \| `bundle` (minified `src/rc/`) \| `resource` (binary asset) \| `excluded` (node_modules, build outputs). |
| `source_doc` | object | Opcional | Cuando la entrada se sintetizó de un doc existente. `file`: path relativo. `section`: heading o anchor. Ej: `{file: "reflow-frontend/docs/REFLOW-ARCHITECTURE-ANALYSIS.md", section: "Backend Class Map › BReflowService"}`. |
| `verified_at` | string (ISO 8601 date) | Opcional | Solo se setea cuando un humano/sub-agent abrió el archivo y comparó. Si está ausente → entrada sintetizada sin verificación. |

**Regla de id**: para evitar colisiones cross-profile (ej. dos `BReflow.java`), `id` siempre incluye el profile: `nmodsreflow-rt/...` vs `nmodsreflow-ux/...`.

### 1.2 Extension block `backend` — para `kind: java-class | compiled-class | compiled-jar | module-descriptor`

```json
{
  "backend": {
    "profile": "rt",
    "package": "com.niagaramods.nmodsreflow",
    "bcomponent_type": "BComponent",
    "slots": 26,
    "actions": ["sync", "broadcast"],
    "rest_endpoints": [],
    "box_methods": [],
    "decompiled": false
  }
}
```

| Campo | Obligatorio | Aplica a | Notas |
|-------|------|----------|-------|
| `profile` | Sí | todo `java-class`/`compiled-*` | `rt` \| `ux`. |
| `package` | Sí (si `kind=java-class`) | java-class | FQN del paquete. `null` para `compiled-jar`/`module-descriptor`. |
| `bcomponent_type` | Opcional | java-class | `BComponent`, `BIService`, `BIRestrictedComponent`, `BOrdScheme`, `BIJavaScript`, `BIAlarmSource`, etc. `null` si no aplica. |
| `slots` | Opcional | bcomponent_type != null | Número de Property/Action slots declarados. `null` si no aplica. |
| `actions` | Opcional (default `[]`) | bcomponent_type != null | Nombres de `@NiagaraAction`. |
| `rest_endpoints` | Opcional (default `[]`) | **solo** clases en `http/responses/` o `http/BaseServlet.java` | URL paths handled. Ej: `["GET /config", "POST /config"]`. |
| `box_methods` | Opcional (default `[]`) | **solo** clases en `commands/` | Nombres de método RPC BajaScript expuestos. |
| `decompiled` | Sí | todo `java-class` | `true` para `BReflowScheme`, `BReflow`, `BReflowConfig`, `BReflowRedirect` (CFR output). `false` para el resto. |

### 1.3 Extension block `frontend_vue` — para `kind: vue-component | vue-view`

```json
{
  "frontend_vue": {
    "component_dir": "equipment",
    "store_modules": ["equipment", "points", "profiles"],
    "emits": ["update", "delete"],
    "props": ["equipment", "editable"],
    "mixins": ["equipmentMixin", "subscriberMixin"],
    "plugins_used": ["$baja", "$niagara"],
    "route_name": null,
    "fidelity": "GOOD"
  }
}
```

| Campo | Obligatorio | Aplica a | Notas |
|-------|------|----------|-------|
| `component_dir` | Sí | vue-component | Subdir bajo `components/`. `null` si está en `views/` o root. |
| `store_modules` | Opcional (default `[]`) | vue-component/view | Módulos Vuex que el componente usa (`mapState`/`mapGetters`/`commit`). |
| `emits` | Opcional (default `[]`) | vue-component | Solo eventos clave (top 3-5). Detalle exhaustivo va al domain doc. |
| `props` | Opcional (default `[]`) | vue-component | Solo props clave (top 3-5). |
| `mixins` | Opcional (default `[]`) | vue-component/view | Mixins aplicados. |
| `plugins_used` | Opcional (default `[]`) | vue-component/view | `$baja`, `$niagara`, `$http`, `$time`, `$ord`, `$reflowLink`, `$gbo`, `$workbench`, `$cookies`, `labelForItem`, `configMode`, `colorUtils`, `utils`. |
| `route_name` | Opcional | vue-view | Nombre del route. `null` si no es view. |
| `fidelity` | Opcional | vue-component/view | `EXCELLENT` \| `GOOD` \| `FAIR` \| `POOR` (de GAP-ANALYSIS). `null` si no documentado. |

### 1.4 Extension block `frontend_js` — para `kind: js-*`

```json
{
  "frontend_js": {
    "module_type": "store",
    "persistent": true,
    "exports": ["state", "getters", "mutations", "actions"]
  }
}
```

| Campo | Obligatorio | Aplica a | Notas |
|-------|------|----------|-------|
| `module_type` | Sí | js-* | `store` \| `mixin` \| `plugin` \| `api` \| `lib` \| `router` \| `util` \| `entry`. |
| `persistent` | Sí (si `module_type=store`) | js-store | `true` si está en los 14 stores incluidos en `config.json` snapshot, `false` para los 15 transient. |
| `exports` | Opcional (default `[]`) | js-plugin / js-lib / js-util | Nombres de exports clave (top 5). Para stores Vuex: omitir (siempre son los 4 estándar). |

### 1.5 Top-level document structure

`index.json` raíz:

```json
{
  "schema_version": "1.0",
  "module": "reflow-clean-177",
  "source_repo": "/home/cristian/modules/Prototipos/Reflow-Clean-177/",
  "generated_at": "2026-05-09",
  "generator": "sdd-apply mapping-reflow-clean-177",
  "entries": [
    { "id": "...", "path": "...", "kind": "...", ... }
  ],
  "exclusions": [
    { "pattern": "reflow-frontend/node_modules/**", "reason": "third-party deps" },
    { "pattern": "nmodsreflow-rt/src/rc/**", "reason": "compiled webpack bundle" },
    { "pattern": "nmodsreflow-ux/build/**", "reason": "Gradle build output" }
  ]
}
```

Top-level es **objeto, no array**. Esto permite agregar metadata futura sin romper consumers (a diferencia de un array plano).

### 1.6 Forward-compat rules

1. **Adición de campos = no-breaking**. Consumers deben ignorar campos desconocidos. Documentado en `schema.md`.
2. **Adición de bloques de extensión = no-breaking**. `analytics`, `mx60`, futuros: bloque nuevo siblings de `backend`/`frontend_vue`. Cero edición de `core`.
3. **Cambio en `core` = breaking → bump `schema_version` a `2.0`**. Política explícita: si un nuevo módulo necesita campo core nuevo (ej. `module: string`), se evalúa primero si puede ir a extension. Solo se modifica core con justificación.
4. **Cambio de enum (`kind`, `domain`, `status`) = MINOR bump (`1.1`)**. Consumers que validen estricto deben tolerar valores nuevos.
5. **Eliminación de campo = MAJOR bump**.

### 1.7 Reusabilidad para Analytics/MX60 (prueba concreta)

Cuando el siguiente change construya el mapping de Analytics:

```json
{
  "id": "nmodsanalytic-rt/src/com/niagaramods/nmodsanalytic/algorithms/PIDController.java",
  "path": "nmodsanalytic-rt/src/com/niagaramods/nmodsanalytic/algorithms/PIDController.java",
  "kind": "java-class",
  "domain": "algorithm",
  "purpose": "PID controller block: setpoint tracking with anti-windup.",
  "dependencies": ["com.niagaramods.nmodsanalytic.BAnalyticService"],
  "loc": 312,
  "status": "source",
  "backend": {
    "profile": "rt",
    "package": "com.niagaramods.nmodsanalytic.algorithms",
    "bcomponent_type": "BAlgorithmBlock",
    "slots": 8,
    "actions": ["execute", "reset"],
    "decompiled": false
  },
  "analytics": {
    "algorithm_type": "control",
    "dag_role": "node",
    "aon_encoded": true,
    "execution_order": 12,
    "verdict": "MX60_NO"
  }
}
```

El bloque `core` no se tocó. `domain` recibe el valor `"algorithm"` (es enum extensible bajo regla 4). El bloque `analytics` agrega 5 campos específicos del dominio Analytics (algorithm_type, DAG role, AON encoding flag, execution order in DAG, MX60 verdict from bloque67). Cero migración del v1.0 — backward compatible.

### 1.8 Ejemplos completos (5 archivos representativos)

#### (a) BReflowService.java — Java service container

```json
{
  "id": "nmodsreflow-rt/src/com/niagaramods/nmodsreflow/BReflowService.java",
  "path": "nmodsreflow-rt/src/com/niagaramods/nmodsreflow/BReflowService.java",
  "kind": "java-class",
  "domain": "service-container",
  "purpose": "Root BComponent del módulo: 26 slots, lifecycle start/stop, spawnea SyncService + WebSocketAcceptor + ChannelService.",
  "dependencies": [
    "javax.baja.sys.BComponent",
    "javax.baja.naming.BIService",
    "javax.baja.sys.BIRestrictedComponent",
    "com.niagaramods.nmodsreflow.sync.BReflowSyncService",
    "com.niagaramods.nmodsreflow.http.sockets.BReflowWebSocketAcceptor",
    "com.niagaramods.nmodsreflow.http.sockets.BReflowChannelService"
  ],
  "loc": 468,
  "status": "source",
  "source_doc": {
    "file": "reflow-frontend/docs/REFLOW-ARCHITECTURE-ANALYSIS.md",
    "section": "Backend › Service Container › BReflowService"
  },
  "verified_at": "2026-05-09",
  "backend": {
    "profile": "rt",
    "package": "com.niagaramods.nmodsreflow",
    "bcomponent_type": "BIService, BIRestrictedComponent",
    "slots": 26,
    "actions": ["sync", "broadcast", "kickUser"],
    "rest_endpoints": [],
    "box_methods": [],
    "decompiled": false
  }
}
```

#### (b) Equipment.vue — Vue component

```json
{
  "id": "reflow-frontend/src/components/equipment/Equipment.vue",
  "path": "reflow-frontend/src/components/equipment/Equipment.vue",
  "kind": "vue-component",
  "domain": "equipment",
  "purpose": "Card principal de equipo: header, status, points list, link a editor.",
  "dependencies": [
    "@/components/cards/BaseCard",
    "@/components/points/PointList",
    "@/mixins/equipmentMixin",
    "@/store/modules/equipment"
  ],
  "loc": 287,
  "status": "source",
  "source_doc": {
    "file": "reflow-frontend/docs/GAP-ANALYSIS.md",
    "section": "Equipment domain (41 components)"
  },
  "frontend_vue": {
    "component_dir": "equipment",
    "store_modules": ["equipment", "points", "profiles"],
    "emits": ["update", "delete", "edit"],
    "props": ["equipment", "editable", "compact"],
    "mixins": ["equipmentMixin", "subscriberMixin"],
    "plugins_used": ["$baja", "$niagara", "$reflowLink"],
    "route_name": null,
    "fidelity": "GOOD"
  }
}
```

#### (c) niagara plugin — JS plugin

```json
{
  "id": "reflow-frontend/src/plugins/niagara.js",
  "path": "reflow-frontend/src/plugins/niagara.js",
  "kind": "js-plugin",
  "domain": "plugins",
  "purpose": "$niagara — RPC bridge para alarms/subscribers/BQL/history vía BajaScript BOX.",
  "dependencies": [
    "@/api/box",
    "@/api/bajascript",
    "@/lib/eventBus"
  ],
  "loc": 412,
  "status": "source",
  "source_doc": {
    "file": "reflow-frontend/docs/REFLOW-ARCHITECTURE-ANALYSIS.md",
    "section": "Frontend › Plugins › $niagara"
  },
  "frontend_js": {
    "module_type": "plugin",
    "exports": ["install", "queryAlarms", "subscribePoint", "runBQL", "getHistory"]
  }
}
```

#### (d) profiles store module — Vuex store

```json
{
  "id": "reflow-frontend/src/store/modules/profiles.js",
  "path": "reflow-frontend/src/store/modules/profiles.js",
  "kind": "js-store",
  "domain": "profiles-rbac",
  "purpose": "Vuex module: roles, permisos por path, authorizeLink, isPathAvailable.",
  "dependencies": [
    "@/api/rest",
    "@/lib/deepMerge"
  ],
  "loc": 198,
  "status": "source",
  "source_doc": {
    "file": "reflow-frontend/docs/GAP-ANALYSIS.md",
    "section": "State › Persistent stores › profiles"
  },
  "frontend_js": {
    "module_type": "store",
    "persistent": true
  }
}
```

#### (e) BaseServlet.java — HTTP front controller

```json
{
  "id": "nmodsreflow-rt/src/com/niagaramods/nmodsreflow/http/BaseServlet.java",
  "path": "nmodsreflow-rt/src/com/niagaramods/nmodsreflow/http/BaseServlet.java",
  "kind": "java-class",
  "domain": "http-rest",
  "purpose": "HTTP front controller: routing por URL path a 18 response handlers; CSRF guard; sesión.",
  "dependencies": [
    "javax.baja.web.BWebServlet",
    "com.niagaramods.nmodsreflow.http.util.CsrfGuard",
    "com.niagaramods.nmodsreflow.http.responses.*"
  ],
  "loc": 312,
  "status": "source",
  "source_doc": {
    "file": "reflow-frontend/docs/REFLOW-ARCHITECTURE-ANALYSIS.md",
    "section": "Backend › HTTP REST › BaseServlet"
  },
  "backend": {
    "profile": "rt",
    "package": "com.niagaramods.nmodsreflow.http",
    "bcomponent_type": "BWebServlet",
    "slots": null,
    "actions": [],
    "rest_endpoints": [
      "GET /config", "POST /config", "DELETE /config",
      "GET /history", "POST /history",
      "GET /alarms", "POST /alarms/ack",
      "GET /schedules",
      "GET /backups", "POST /backups",
      "GET /files", "POST /files",
      "GET /weather"
    ],
    "box_methods": [],
    "decompiled": false
  }
}
```

---

## 2. File layout (LOCKED)

```
docs/mappings/reflow-clean-177/
├── README.md                    ← guía de uso + cómo extender + queries de ejemplo
├── schema.md                    ← contrato versionado v1.0 (sección 1 de este design)
├── index.md                     ← humano: tabla paginada por dominio (~535 filas)
├── index.json                   ← jq-queryable: objeto top-level con entries[] + exclusions[]
├── excluded.md                  ← lista paths excluidos + razón
└── domains/
    ├── backend.md               ← TODOS los 77 Java + module descriptors (un archivo)
    ├── frontend.md              ← overview frontend + dominios chicos (cards, charts, points, navigation, common, layout, weather, maps, schedules, pages, settings, websocket-ui, wizard, browser, profiles-rbac, dashboard, app-shell, state, mixins, plugins, lib, api-layer, views, util)
    ├── equipment.md             ← 41 components (warrant: domain crítico, surface UI compleja)
    ├── floorplans.md            ← 47 components (warrant: SVG canvas editor, mayor componente)
    ├── alarms.md                ← cross-stack: 5 Java + 22 Vue + AlarmCache + WS events
    ├── history.md               ← cross-stack: 12 Java + 22 Vue + 5 Jackson serializers
    └── buildings-config.md      ← buildings (27) + config (22) — acoplados via BReflowSyncService
```

### 2.1 Justificación de domain docs propios

Criterios para warrantar archivo propio:
1. **Volumen ≥20 archivos** EN EL DOMINIO, o
2. **Cross-stack** (backend + frontend acoplados), o
3. **Complejidad arquitectónica especial** (canvas editor, multi-user locking, etc.).

| Domain | Archivos | Cross-stack | Complejidad | Veredicto |
|--------|---------|-------------|-------------|-----------|
| **backend** | 77 Java | — | Service container + 24 endpoints + WS | **Sí** — agrupado en un solo doc por consistencia |
| **frontend** (overview + 17 dominios chicos) | ~150 | — | Variado | **Sí** — overview SPA + dominios <20 archivos |
| **equipment** | 41 | Vue + points + bajascript | Surface UI + CSV wizard + editor | **Sí** — criterio 1+3 |
| **floorplans** | 47 | Vue + SVG | Canvas editor con drag/resize/elementos | **Sí** — criterio 1+3 |
| **alarms** | 5J + 22V = 27 | Backend BIAlarmSource + Vue + WS push | BQL + UUID + ack flow | **Sí** — criterio 1+2 |
| **history** | 12J + 22V = 34 | Backend Jackson + Vue D3 charts | Serializers múltiples + builder UI | **Sí** — criterio 1+2 |
| **buildings + config** | 27V + 22V + Sync Java | Backend BReflowSyncService + Vue config cells | Multi-user locking + delta sync | **Sí** — criterio 1+2+3 |

Dominios NO warrant (van a `frontend.md`): cards (18), charts (11), points (11), navigation (17), common (23), settings (13), profiles (11), dashboard (12), pages (11), weather (6), maps (7), schedules (4), websocket-ui (4), wizard (8), browser (1), layout (5).

### 2.2 Plantilla obligatoria por domain doc (5 secciones)

Todo `domains/<x>.md` DEBE tener exactamente estas 5 secciones, en este orden:

1. **`## Overview`** — 1 párrafo: rol del dominio, scope (n archivos backend / n frontend), entry points clave.
2. **`## File inventory`** — tabla con `path | kind | purpose | loc | fidelity` (frontend) o `path | bcomponent_type | slots | loc | decompiled` (backend).
3. **`## Architecture`** — diagrama ASCII de relaciones inter-archivo (quién invoca a quién, store ↔ component, servlet ↔ command).
4. **`## Cross-references`** — links a otros domain docs cuando hay acoplamiento explícito (ej. equipment → points, alarms → websocket).
5. **`## Known issues / Gotchas`** — fidelity issues de GAP-ANALYSIS, decompiled flags, edge cases.

`README.md` enforça esta plantilla. `sdd-verify` la chequea.

### 2.3 `excluded.md` contenido

```markdown
# Excluded paths

| Pattern | Reason | Approx files |
|---------|--------|--------------|
| `reflow-frontend/node_modules/**` | Third-party deps; not source. | ~thousands |
| `nmodsreflow-rt/src/rc/**` | Compiled webpack bundle. Only `app-readable.js` indexed (kind: compiled-bundle). | ~30 |
| `nmodsreflow-ux/build/**` | Gradle build output (.jar, .class, .bajadoc). | ~15 |
| `**/.git/**` | VCS metadata. | n/a |
| `**/dist/**` | Build output. | n/a |
```

---

## 3. Generation pipeline (4 batches, sub-agent layout)

### 3.1 Pipeline overview

```
Batch A (sequential, 1 sub-agent)
   └── Skeleton: index.md + index.json + schema.md + README.md + excluded.md
        Reads: 3 docs + fd tree
        Writes: 5 files (index entries con backend ext from doc; frontend_vue ext stub)
        Duration: 1 sub-agent call
        Dependency: ninguna
        ↓
Batch B (sequential after A, 1 sub-agent)
   └── Backend deep dive: domains/backend.md
        Reads: index.json + 77 Java paths + REFLOW-ARCHITECTURE-ANALYSIS.md
        Writes: domains/backend.md + ENRIQUECE entries backend en index.json
        Duration: 1 sub-agent call
        Dependency: Batch A done
        ↓
Batch C (PARALLEL, 5 sub-agents simultáneos)
   ├── C1: domains/frontend.md (overview + 17 dominios chicos)
   ├── C2: domains/equipment.md
   ├── C3: domains/floorplans.md
   ├── C4: domains/alarms.md (cross-stack: lee también 5 Java alarms)
   └── C5: domains/history.md (cross-stack: lee también 12 Java history) + domains/buildings-config.md
        Reads: index.json + GAP-ANALYSIS.md + sus archivos del dominio
        Writes: 1 domain doc cada uno + ENRIQUECE entries frontend en index.json
        Duration: 5 sub-agents en paralelo (1 wall-clock unidad)
        Dependency: Batch B done (necesita backend ext para cross-stack docs)
        ↓
Batch D (sequential after C, 1 sub-agent)
   └── Validation: spot-check + jq + grep coverage + schema validate
        Reads: index.json + 40 archivos sample + 7 domain docs
        Writes: validation report (no edits)
        Duration: 1 sub-agent call
        Dependency: Batch C done
```

### 3.2 Batch detail

#### Batch A — Skeleton (1 sub-agent, sequential)

**Input read**:
- `reflow-frontend/docs/REFLOW-ARCHITECTURE-ANALYSIS.md` (full)
- `reflow-frontend/docs/GAP-ANALYSIS.md` (full)
- `reflow-frontend/docs/NIAGARA-INTEGRATION.md` (full)
- `fd . /home/cristian/modules/Prototipos/Reflow-Clean-177/ -t f` para obtener todos los paths
- `wc -l` por archivo source para LOC

**Output write**:
- `docs/mappings/reflow-clean-177/index.json` — 535 entries con `core` completo. `backend` ext poblado donde el doc lo da (LOC, slot count para BReflowService). `frontend_vue` ext stub (solo `component_dir`, `fidelity` from GAP-ANALYSIS).
- `docs/mappings/reflow-clean-177/index.md` — tabla paginada por domain.
- `docs/mappings/reflow-clean-177/schema.md` — copia de la sección 1 de este design.
- `docs/mappings/reflow-clean-177/README.md` — uso, queries jq de ejemplo, cómo extender para Analytics.
- `docs/mappings/reflow-clean-177/excluded.md` — 5 patterns excluidos.

**Por qué sequential**: todos los batches siguientes leen `index.json`. Es la fundación.

#### Batch B — Backend deep dive (1 sub-agent, sequential)

**Input read**:
- `docs/mappings/reflow-clean-177/index.json` (Batch A output)
- 77 archivos Java (sample completo — son pocos)
- `REFLOW-ARCHITECTURE-ANALYSIS.md` re-lectura focal

**Output write**:
- `domains/backend.md` con sub-secciones: Service container (BReflowService, BReflowScheme), HTTP REST (BaseServlet + 18 responses), WebSocket (5 sockets), BajaScript BOX (7 commands), History (12 archivos incluye json/), Alarms (5), Sync (4), Util (7), UX widgets (3 nmodsreflow-ux).
- ENRIQUECE en `index.json`: completa `backend.actions`, `backend.rest_endpoints`, `backend.box_methods`, `backend.decompiled` para los 77 Java.

**Por qué sequential a Batch A y antes de C**: `domains/alarms.md` y `domains/history.md` (Batch C) son cross-stack y necesitan que las entries backend ya estén enriquecidas para hacer cross-references correctas.

#### Batch C — Frontend deep dives (5 sub-agents, PARALLEL)

Los 5 sub-agents corren simultáneamente porque escriben archivos distintos y solo hacen merge en `index.json` por path-prefix disjoint.

| Sub-agent | Reads | Writes | Updates index.json (entries por path prefix) |
|-----------|-------|--------|----------------------------------------------|
| **C1** frontend.md | GAP-ANALYSIS + ~150 .vue/.js de dominios chicos | `domains/frontend.md` | `src/components/{cards,charts,common,points,navigation,settings,profiles,dashboard,pages,weather,maps,schedules,websocket,wizard,browser,layout}/**`, `src/{api,plugins,mixins,lib,store/modules,router}/**`, `src/{main.js,App.vue}`, `src/views/**` (excepto Equipment, Floorplans, Alarms, History, Buildings views) |
| **C2** equipment.md | GAP-ANALYSIS + 41 .vue equipment | `domains/equipment.md` | `src/components/equipment/**` |
| **C3** floorplans.md | GAP-ANALYSIS + 47 .vue floorplans | `domains/floorplans.md` | `src/components/floorplans/**` |
| **C4** alarms.md | 22 .vue alarms + 5 Java alarms ya enriquecidas + AlarmCache lib | `domains/alarms.md` | `src/components/alarms/**` (Java alarms ya done en B) |
| **C5** history.md + buildings-config.md | 22 .vue history + 12 Java history + 27 .vue buildings + 22 .vue config | `domains/history.md` + `domains/buildings-config.md` | `src/components/{histories,buildings,config}/**` |

**Concurrency control**: cada sub-agent edita ÚNICAMENTE entries cuyo path matchea su prefix. Como los prefixes son disjoint, no hay race condition. La mecánica concreta: cada sub-agent lee `index.json`, modifica solo sus entries, escribe el JSON completo. **NO funciona** con sub-agents simultáneos sobre un mismo file. **Solución**: cada sub-agent escribe a un fragmento `index.<domain>.partial.json`, y un paso final (parte de Batch D) hace merge.

**Decisión**: para evitar concurrency en index.json, los sub-agents C1-C5 escriben **solo sus domain docs**. Los enriquecimientos a `index.json` los recolecta Batch D al final, leyendo cada domain doc y back-poblando los campos faltantes. Trade-off: Batch D más pesado, pero zero concurrency risk.

#### Batch D — Validation + index.json final merge (1 sub-agent, sequential)

**Input read**:
- `index.json` (post-Batch B)
- 7 domain docs (Batch B + C output)
- 40 archivos sample para spot-check (estratificado, ver §4.3)

**Output write**:
- ENRIQUECE final en `index.json` los campos `frontend_vue.props`, `frontend_vue.emits`, `frontend_vue.mixins`, `frontend_vue.store_modules`, `frontend_vue.plugins_used` extraídos de los domain docs.
- Validation report (en engram + opcionalmente `validation-report.md`):
  - jq parse OK
  - count: ≥530 entries (allow 5 excludidos)
  - spot-check: fidelity per file vs entry
  - schema validation: cada entry tiene los mandatory fields según `kind`
  - domain doc template: 5 secciones presentes en cada uno

**Por qué sequential**: depende de los 5 outputs de Batch C completos.

### 3.3 Justificación parallelism

- **A → B**: secuencial. B necesita el skeleton para enriquecer.
- **B → C**: secuencial. C4/C5 cross-stack necesitan backend ext done.
- **C1-C5**: paralelo. 5 prefixes de path disjoint, escriben docs distintos. Speedup ~5x sobre sequential.
- **C → D**: secuencial. D consolida todo.

Wall-clock: ~4 unidades en lugar de ~9 si todo fuera secuencial.

---

## 4. Validation strategy (sdd-verify)

### 4.1 Mandatory checks

| Check | Tool | Pass criterion |
|-------|------|----------------|
| **JSON parse** | `jq '.' index.json > /dev/null` | exit 0 |
| **Top-level shape** | `jq -e '.schema_version=="1.0" and .entries and .exclusions' index.json` | true |
| **Entry count** | `jq '.entries \| length' index.json` | ≥530 |
| **Domain backend coverage** | `jq '[.entries[] \| select(.domain=="history")] \| length' index.json` | ≥34 (12 Java + 22 Vue) |
| **Domain alarms coverage** | `jq '[.entries[] \| select(.domain=="alarms")] \| length' index.json` | ≥27 |
| **Mandatory core fields** | `jq -e '.entries \| all(.id and .path and .kind and .domain and .purpose and (.loc \| type=="number") and .status)' index.json` | true |
| **Backend ext present** | `jq -e '[.entries[] \| select(.kind=="java-class")] \| all(.backend.profile and (.backend.decompiled \| type=="boolean"))' index.json` | true |
| **Decompiled flag accuracy** | `jq '[.entries[] \| select(.backend.decompiled==true) \| .id]' index.json` | exactamente: BReflowScheme, BReflow (ux), BReflowConfig, BReflowRedirect |
| **No node_modules** | `jq '[.entries[] \| select(.path \| contains("node_modules"))] \| length' index.json` | 0 |
| **Domain doc 5-sec template** | `rg -c '^## (Overview\|File inventory\|Architecture\|Cross-references\|Known issues)' domains/*.md` | 5 por archivo |

### 4.2 Coverage table esperada

| Domain | Entries esperadas | Origen |
|--------|-------------------|--------|
| service-container | 2 | BReflowService, BReflowScheme |
| http-rest | 22 | BaseServlet + 18 responses + 3 util |
| http-websocket | 5 | sockets/ |
| bajascript-box | 7-8 | commands/ |
| history | 34 | 12 Java + 22 Vue |
| alarms | 27 | 5 Java + 22 Vue |
| sync-config | 6 | sync/ + ConfigIO + ReflowSyncResponse |
| backups | 1 | BackupManager |
| util | 7 | util/ |
| ux-widget | 3 + 6 JS | nmodsreflow-ux |
| equipment | 41 | components/equipment |
| floorplans | 47 | components/floorplans |
| buildings | 27 | components/buildings |
| config | 22 | components/config |
| navigation | 17 | components/navigation |
| common | 23 | components/common |
| cards | 18 | components/cards |
| charts | 11 | components/charts |
| points | 11 | components/points |
| profiles-rbac | 11 | components/profiles |
| dashboard | 12 | components/dashboard |
| settings | 13 | components/settings |
| weather | 6 | components/weather |
| maps | 8 | components/maps + map |
| pages | 11 | components/pages |
| schedules | 4 | components/schedules |
| websocket-ui | 4 | components/websocket |
| wizard | 8 | components/wizard |
| browser | 1 | components/browser |
| layout | 5 | components/layout |
| views | 13 | views/ |
| state | 30 | store/index + 29 modules |
| mixins | 17 | mixins/ |
| plugins | 13 | plugins/ |
| lib | 10 | lib/ |
| api-layer | 6 | api/ + index |
| app-shell | 3 | main.js, App.vue, router/index.js |
| image-library | 22 | JPGs |
| icons | 6 | PNGs |
| build-artifact | ~15 | nmodsreflow-ux/build |
| **TOTAL** | **~570** | (margen 530-570 aceptable) |

### 4.3 Spot-check sample (40 files, estratificado)

Estratificación: ~5 archivos × 8 dominios clave. Selection rule: 1 más grande (LOC), 1 más chico, 1 mediana aleatoria, 1 con fidelity FAIR/POOR si existe, 1 con cross-references explícitas.

| Dominio | 5 sample files |
|---------|----------------|
| **backend service** | BReflowService.java, BReflowScheme.java, ConfigIO.java, BReflowSyncService.java, BackupManager.java |
| **backend http** | BaseServlet.java, una response handler grande, una chica, CsrfGuard.java, SocketServlet.java |
| **backend history** | HistoryData.java, HistoryRecordSerializer.java, HistoryGroups.java, HistoryIO.java, HistoryObjectMapper.java |
| **backend alarms** | ReflowAlarmSource.java, AlarmData.java, AlarmSourceCollection.java, QueryFilter.java, AlarmUuidArgs.java |
| **frontend equipment** | Equipment.vue, EquipmentEditor.vue (largo), EquipmentList.vue, EquipmentCsvWizard.vue, una chica |
| **frontend floorplans** | FloorplanCanvas.vue (warranty), FloorplanElement*.vue (3), PropsPane.vue |
| **frontend alarms** | AlarmConsole.vue, AlarmTable.vue, AlarmRow.vue, AlarmPriorities.vue, AlarmAck.vue |
| **frontend store** | store/index.js, profiles.js (persistent), buildings.js (FAIR), equipment.js (POOR), un transient |

Para cada archivo: abrir, comparar contra entry. Métricas:
- `purpose` accuracy (subjetivo, score 0-1)
- `loc` ±5%
- `dependencies` ≥80% capturadas
- `kind/domain/status` correctos
- ext fields correctos cuando aplica

**Fidelity score**: promedio sobre los 40. **Pass**: ≥0.90. **Fail**: <0.90 → identificar dominio peor → re-batch ese dominio.

### 4.4 Cross-stack consistency checks

- Para cada Java alarm class, debe haber link en `domains/alarms.md`.
- Para cada Vue history component que usa `$niagara.getHistory`, link en `domains/history.md` apunta a `BReflowHistoryCommands`.
- `BReflowSyncService` referenciado desde `domains/buildings-config.md` y desde `index.md` row de `BReflowService`.

---

## 5. Trade-offs y alternativas rechazadas

### 5.1 YAML vs JSON sidecar

**Elegido**: JSON.
**Rechazado**: YAML.
**Razón**: `jq` es estándar Unix, ergonómico para query single-line. `yq` existe pero menos universal y más lento. JSON es subset de JS — los tests pueden hacer `require()` sin parser. YAML tiene más sintaxis (anchors, refs) que invita inconsistencia entre los 535 entries. JSON force schema rigidez. Trade-off: JSON menos legible que YAML para humanos → se compensa con `index.md` (humano) sibling.

### 5.2 Per-file markdown vs flat index

**Elegido**: flat `index.json` + `index.md` paginado por dominio + 7 domain docs.
**Rechazado**: 535 archivos `.md` (uno por entry).
**Razón**: 535 archivos = imposible navegar, git diffs ruidosos, query sin engine custom. Flat index permite `jq` cualquier query en O(1) lectura. Domain docs de profundidad balancean detalle. Trade-off: domain docs duplican parcialmente data del JSON → mitigado porque docs son prosa+arquitectura, JSON es estructurado.

### 5.3 Automated AST parsing vs synthesis from existing docs

**Elegido**: synthesis from 3 existing docs (`REFLOW-ARCHITECTURE-ANALYSIS.md`, `GAP-ANALYSIS.md`, `NIAGARA-INTEGRATION.md`) + spot-check 40 files.
**Rechazado**: full AST parsing (javac/babel).
**Razón**: AST parsing requiere setup (javac classpath roto por dependencias missing, babel config para Vue 2.7), tiempo de implementación >2x. Los 3 docs existentes cubren ≥90% del mapping con LOC, slot counts, fidelity. Spot-check de 40 valida que el synthesis no se desvía. Trade-off: ≤10% de inferencia → mitigado por campo `verified_at` que distingue entries verificadas vs sintetizadas, y por re-batch automático si fidelity <90%.

### 5.4 Hash-based id vs path-based id

**Elegido**: path-based id (mismo valor que `path`).
**Rechazado**: hash SHA-1 de path.
**Razón**: path es estable, legible, debuggeable. Hash es opaco — para humanos `jq` queries serían imposibles. La única razón para hash sería colisiones, pero el path ya es único por construcción del filesystem. Trade-off: si en el futuro un archivo se renombra, el id cambia → aceptable, es lo correcto (es otro archivo conceptualmente).

### 5.5 Single domain doc vs per-domain docs

**Elegido**: 7 domain docs (backend + frontend overview + 5 dominios warrant).
**Rechazado**: 1 mega-doc con todo, o 1 doc por subdir (29 docs).
**Razón**: 1 mega-doc no escala lectura. 29 docs fragmentan acoplamientos cross-domain (alarms cross-stack quedaría partido en 2 archivos). 7 docs balanceados = un dominio = una lectura completa.

### 5.6 Concurrent index.json edits vs deferred merge

**Elegido**: deferred merge en Batch D.
**Rechazado**: 5 sub-agents editando index.json simultáneamente con file locks.
**Razón**: file locks no son atómicos en sub-agent context. Race conditions silenciosas = corrupción JSON. Deferred merge es trivialmente correcto: cada sub-agent escribe su domain doc; Batch D hace una sola pasada de back-population. Trade-off: Batch D más pesado → aceptable, sigue siendo 1 sub-agent.

---

## 6. Risks y mitigaciones

| Risk | Likelihood | Impact | Mitigation | Residual |
|------|-----------|--------|------------|----------|
| 3 docs fuente desactualizados desde 2026-04-06 | Medium | Medio (entries con purpose/LOC desfasados) | Spot-check 40 archivos en Batch D; campo `verified_at`; re-batch si fidelity <90% | Bajo: 10% de drift es aceptable y queda flageado |
| Schema v1.0 insuficiente para Analytics (algorithm DAG, AON encoding) | Low-Medium | Alto (rework si el contrato no aguanta) | Diseño `core + extension` aditivo; ejemplo concreto §1.7 con bloque `analytics`; regla forward-compat documentada | Bajo: el bloque `analytics` ya está prototipado |
| Sub-agents C1-C5 divergen en estilo/calidad | Medium | Medio (domain docs inconsistentes) | Plantilla obligatoria 5 secciones (§2.2); README.md enforça; Batch D valida `rg -c '^## (Overview...)'` | Bajo: validation falla = re-trabajo focalizado |
| Concurrent edit a `index.json` corrompe JSON | Medium si naïve | Alto | **Deferred merge** en Batch D; sub-agents C solo escriben domain docs; D back-pobla index | Cero |
| Decompiled Java introduce ruido (4 clases) | Low | Bajo | Campo `decompiled: true` flag explícito; domain doc `Known issues` cita CFR header | Cero |
| `node_modules`/`src/rc/` indexed por accidente | Low | Medio (entry count inflado) | Excluded patterns en `index.json.exclusions[]`; `fd` con `-E` flags; validation check `contains("node_modules")` debe ser 0 | Cero |
| `index.json` 535 entries crece a >5MB | Low | Bajo (jq lento) | LOC actual estimada 200-400KB; si supera 1MB → split por domain | Cero |
| Bloques 63-67 ya consumiendo ad-hoc no migran al mapping | Medium | Bajo (mapping ignored) | README incluye 3+ jq queries y 2+ rg cross-domain como onramp; mencionar en commit message | Bajo: adopción depende de utilidad demostrada |
| Cross-stack docs (alarms, history) se quedan stale cuando se re-genera backend o frontend | Low | Medio | `verified_at` por entry; cross-references explícitas en plantilla; sdd-archive registra fecha | Bajo |
| Mapeo de `dependencies` para Vue es ambiguo (imports relativos `./`, alias `@/`, mixins via plugin global) | Medium | Bajo | Regla explícita schema §1.1: alias `@/` resuelto, no FQN. Patrón uniforme en todos los sub-agents. | Bajo |

---

## 7. ADR-style decisions (consolidado)

| # | Decision | Rationale | Rejected alternative |
|---|----------|-----------|----------------------|
| **D1** | Schema `core + extension`, top-level objeto con `entries[]` | Reusabilidad Analytics/MX60; metadata futura sin breaking | Flat array (sin metadata expansion); Schema monolítico (no reusable) |
| **D2** | JSON sidecar + Markdown sibling | jq estándar; humanos leen md | YAML (menos universal); per-file md (no escala) |
| **D3** | Synthesis from 3 docs + spot-check 40 archivos | 90% de info ya existe; AST parsing costo >2x | Full AST parsing (javac/babel) |
| **D4** | path-based id (no hash) | legible, debuggeable, ya único | SHA-1 hash (opaco) |
| **D5** | 7 domain docs (backend + frontend + 5 warrants) | Balance entre granularidad y acoplamientos cross-domain | 1 mega-doc; 29 per-subdir docs |
| **D6** | Pipeline 4 batches con C en paralelo (5 sub-agents) | Speedup ~5x sobre sequential; prefixes disjoint | Todo sequential; sub-agents paralelos editando index.json (race) |
| **D7** | `index.json` enrichment via deferred merge en Batch D | Cero race conditions; trivialmente correcto | File locks en sub-agents (no atómicos) |
| **D8** | Forward-compat: adición de campos/bloques = no-breaking | Schema vivirá ≥3 changes (Analytics + MX60 + futuro) | MAJOR bump por cualquier change (forces rework) |

---

## 8. Open questions / assumptions

1. **Asumido**: GAP-ANALYSIS.md sigue válido a 2026-05-09 (último update 2026-04-06). Spot-check lo confirmará.
2. **Asumido**: jq disponible en entorno consumer. Standard en Unix; documentar como dependencia en README.
3. **Asumido**: bloques 63-67 podrán adoptar el mapping en sus próximas sesiones. No hay forcing function — adopción es opt-in.
4. **Pendiente**: si un entry necesita versionar history (ej. fidelity cambió de FAIR a GOOD entre snapshots), v1.0 no soporta. Decisión: out of scope para v1.0; v2.0 podría agregar `history: [{verified_at, fidelity, ...}]`.

---

## 9. Resumen ejecutivo

Schema `core + extension v1.0` LOCKED — 10 campos universales (`id`, `path`, `kind`, `domain`, `purpose`, `dependencies`, `loc`, `status`, `source_doc?`, `verified_at?`) + 3 extension blocks (`backend`, `frontend_vue`, `frontend_js`) + bloque `analytics` ya prototipado para reuse Analytics/MX60. File layout: 5 archivos top-level + 7 domain docs (backend, frontend overview, equipment, floorplans, alarms, history, buildings-config) bajo `docs/mappings/reflow-clean-177/`. Pipeline: 4 batches con paralelismo en frontend deep dives (Batch C = 5 sub-agents simultáneos sobre prefixes disjoint), `index.json` enrichment via deferred merge en Batch D para evitar race conditions. Validation: jq parse + 11 schema checks + spot-check estratificado 40 archivos (≥0.90 fidelity = pass). 8 ADR documentados con alternativas rechazadas. Forward-compat: schema_version 1.0 → 1.x para enums extensibles, → 2.0 solo si core cambia.
