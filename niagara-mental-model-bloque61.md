# Bloque 61 — Catálogo completo librerías + APIs Reflow `-rt` y `-ux` + reemplazos modernos MX60 + stack recomendado greenfield

**Fecha**: 2026-05-08
**Método**: Audit exhaustivo de **dependencies + APIs + tecnologías** usadas por Reflow producción 1.7.5. Cubre frontend Vite + 13 deps npm + 12 módulos Niagara core + JARs third-party (Jackson, zjsonpatch, Jetty, javax.servlet) + APIs Niagara framework (BComponent, BOrd, BService, BHistory, BAlarm, BUser, BFileSystem) + APIs externas (weather.niagaramodules.com). Para CADA library: propósito + ubicación de uso + cómo se trabaja + reemplazo MX60 recomendado + justificación técnica.

**Fuentes primarias**:
- `reflow-frontend/package.json` + `vite.config.js` + `src/main.js` entry point
- `nmodsreflow/nmodsreflow-rt/module-include.xml` + Gradle config
- `nmodsreflow/nmodsreflow-ux/module-include.xml`
- Imports Java grepeados cross-codebase
- URLs externas hardcoded (grep `https?://`)
- Bloques previos 50-60 (cross-reference patrones)

**Versión analizada**: Reflow-Clean-177 (réplica clean-room) + bundle producción 1.7.5 (Jul 2024).

---

## 61.0 Contexto, scope, qué corrige

### ¿Qué ES este bloque?

Bloque dedicado a tecnologías. Hasta ahora los Bloques 50-60 auditaron código + dominios + patterns + antipatterns. Este bloque cataloga la **infraestructura tecnológica** que sostiene Reflow:
- Cuáles librerías importa
- Cuál es el propósito de cada una
- Cómo se trabaja con ellas
- Cuáles están **deprecated / outdated / vestigial / current**
- Cuál sería el reemplazo MX60 con justificación técnica

Es el insumo para la **decisión de stack MX60**, sin la cual no podés arrancar greenfield rewrite.

### Por qué este bloque ANTES del cierre Reflow

Feedback del usuario: "antes de que cierres reflow, también quiero que veas qué librerías, APIs utiliza, para rt y ux, qué librerías utiliza y para qué propósito, en dónde las utiliza, cómo las utiliza, cómo es que se trabaja con esas librerías, cuál sería sus nuevas tecnologías actualizadas para reemplazar esas librerías/APIs."

Razón estratégica: el cierre Reflow + síntesis MX60 (Bloque 65) requiere conocer el stack tecnológico para producir backlog ejecutable. Sin este catálogo, las reglas template MX60 no tienen anchor en herramientas concretas.

### Hallazgos críticos del catálogo

1. **Frontend Vue 2.7.16 EOL Feb 2024** — extended support terminó. Migración Vue 3 OBLIGATORIA.
2. **Vuex 3.6.2 deprecated** — sucesor Pinia (oficial Vue core team).
3. **vue-router 3.6.5 línea Vue 2** — Router 4 obligatorio para Vue 3.
4. **view-design 4.7.0 (iView) EOL 2020** — sin updates 6+ años. UI kit migration crítico (150+ componentes).
5. **socket.io-client 2.5.0 = VESTIGIAL CONFIRMADO** (Bloque 51 ya lo notó) — 8KB dead code, NUNCA inicializado en producción.
6. **axios 0.21.4 = OUTDATED** — EOL 2025, upgrade a 1.7+ trivial.
7. **Vite 5.4.14 = CURRENT** — build moderno mantener.
8. **Backend usa Jetty 9.4 raw + javax.servlet 3.1** — Jetty 9.4 EOL 2024 pero N4.14 lo embebe.
9. **9 sites `AccessController.doPrivileged`** — Java 17+ deprecated, marked-for-removal. Migration path requerida.
10. **9 sites `new Thread()` raw** — sin BJobService pool (AP-42 + Regla 13 obligatoria).

---

## 61.1 Frontend — package.json deep dive (13 deps + 2 devDeps)

### 61.1.1 Tabla maestra de dependencias frontend

| Library | Versión | Categoría | Propósito | Ubicación uso | Cómo se usa | Reemplazo MX60 | Justificación |
|---------|---------|-----------|-----------|---------------|-------------|----------------|---------------|
| **vue** | ~2.7.16 | framework | SPA principal | 378 `.vue` components | `Vue.use()`, templates, computed/watch | **Vue 3.5+** | EOL Feb 2024, migración obligatoria |
| **vuex** | ~3.6.2 | state | State management 28 módulos | `store/index.js` + 28 módulos (14 persistent + 14 transient) | `new Vuex.Store()`, `commit/dispatch`, `mapState/mapGetters/mapActions` en 200+ components | **Pinia 2.2+** | Vuex deprecated; Pinia oficial Vue core; API más limpia + TS nativo |
| **vue-router** | ~3.6.5 | routing | Routing 32 routes | `router/index.js` + lazy imports | `Vue.use(VueRouter)`, `<router-view>`, `$router.push()` | **Vue Router 4.x** | Línea Vue 2; Router 4 diseñado para Vue 3 + TS |
| **view-design** | ~4.7.0 | ui-kit | UI framework iView | 150+ componentes (`<i-button>`, `<i-modal>`, `<i-table>`, `<i-tabs>`) | `Vue.use(ViewUI)` + iview.css en main.js | **Vuetify 3.5+** o **shadcn-vue** | EOL 2020, sin updates 6 años; Vuetify 3 ergonomic, shadcn-vue headless+Tailwind |
| **axios** | ~0.21.4 | http | Cliente REST | `api/rest.js`, `plugins/http.js`, 12+ vistas | `axios.create()`, interceptors CSRF (AP-10 mitigation), `$http.get/post()` | **axios 1.7+** | EOL 2025, upgrade trivial, mismo API |
| **socket.io-client** | ~2.5.0 | websocket | WS (VESTIGIAL) | `api/websocket.js` (stubs) | **NUNCA EJECUTADO** (Bloque 51 confirmed) | **REMOVER** | 8KB dead code, comentado `initSocket()` en main.js |
| **d3** | ^7.9.0 | charts | Visualización + polígonos | `D3chart.vue` (85L), polylabel layouts | `d3.scaleLinear()`, `d3.axisBottom()`, selecciones | **KEEP D3 7.9** o **recharts 2.10+** | D3 7.9 moderno (May 2024); recharts si team prefiere abstracción |
| **dayjs** | ~1.11.13 | utils | Fechas/timezone | `plugins/timePlugin.js`, 50+ sites | `dayjs().utc().tz().format()`, `.fromNow()` | **KEEP dayjs 1.11.13** | Moderno, 2KB, mantener |
| **vue-clipboard2** | ^0.3.3 | utils | Copy clipboard | `IconBrowser`, `common/*` | `$copyText()` plugin + `v-clipboard` | **@vueuse/core useClipboard** | 0.3.3 de 2020; @vueuse moderno + composable |
| **vue-cookies** | ^1.7.4 | utils | Cookies HTTP | `plugins/cookies.js` | `$cookies.set/get/remove()` | **js-cookie 3.x** o **Pinia persist** | 1.7.4 de 2019; js-cookie standard |
| **vue-drag-resize** | ^1.5.4 | ui | Drag-resize cards | Dashboards (cards resizables) | `<vue-drag-resize>` + `@resized/@dragging` | **@vueuse/core useResizeObserver + useDraggable** | 1.5.4 basic; @vueuse composable API moderna |
| **vue-masonry** | ^0.16.0 | ui | Layout masonry | FloorPlans, dashboards | `<v-masonry>`, `$redrawVueMasonry()` | **CSS Grid nativo** | Grid masonry CSS-native maduro 2024 |
| **vuedraggable** | ~2.24.3 | ui | Drag-drop lists (SortableJS) | Navigation reordering, task lists | `<draggable>` + v-model | **vuedraggable 4.x** o **SortableJS directo** | 2.24.3 línea Vue 2; 4.x para Vue 3 |
| **polylabel** | ^2.0.1 | utils | Polígono label position (Mapbox) | WeatherMap geographic labels | `polylabel(polygon)` → coordinate | **KEEP** si hay maps; sino remover | Modern stable; niche |
| **tinycolor2** | ^1.6.0 | utils | Color parsing/conversion | `colorUtils` plugin, theme editor, color picker | `tinycolor(str).toHex()`, `.lighten()`, `.saturate()` | **colord 2.9+** | colord moderno + ESM friendly + similar API |
| **vite** (dev) | 5.4.14 | build | Build tool | `vite.config.js` | Production build + dev server | **KEEP Vite 5.4** | Mayo 2024, latest stable |
| **@vitejs/plugin-vue2** (dev) | 2.3.3 | build | Vue 2 plugin Vite | `vite.config.js` | Compila SFC Vue 2 | **@vitejs/plugin-vue 5.x** (para Vue 3) | Migración con framework |

### 61.1.2 Build chain frontend detallado

**Vite config** (`reflow-frontend/vite.config.js`):
- Plugin custom `niagaraHtmlPlugin` inyecta RequireJS + Niagara scripts (BajaScript bootstrap, ver Bloque 53)
- Output: `js/app.[hash].js`, `js/chunk-vendors.[hash].js`, `css/[name].[hash].css`
- Base path producción: `/nmodsreflow/`
- Dev server: `localhost:3000` + fixtures mock para `/nmodsreflow/*` endpoints
- Rollup `manualChunks`: vendor + lazy chunks por route
- Target: ES2020+ (Vite default)

**Scripts**:
```json
"dev": "vite",
"build": "vite build",
"preview": "vite preview"
```

### 61.1.3 Outdatedness frontend — clasificación

| Status | Count | Libraries |
|--------|-------|-----------|
| ✅ **CURRENT** | 6 | d3, dayjs, polylabel, tinycolor2, vite, plugin-vue2 (en contexto Vue 2) |
| ⚠️ **OUTDATED** | 7 | axios, vue-clipboard2, vue-cookies, vue-drag-resize, vue-masonry, vuedraggable |
| 🔴 **DEPRECATED** | 4 | vue, vuex, vue-router, view-design |
| 💀 **VESTIGIAL** | 1 | socket.io-client |

**Edad promedio dependencies frontend**: ~3.2 años (May 2026 baseline).

**Bundle dead code**: ~8KB socket.io-client + ~150KB iView CSS (si se migra a Vuetify 3 con tree-shaking) = **~158KB potencial reducción**.

---

## 61.2 Backend Java — module-include + JARs + imports

### 61.2.1 Niagara framework modules (10 módulos core requeridos)

| Módulo Niagara | Categoría | Propósito en Reflow | Subset usado | Reemplazo / alternativa |
|----------------|-----------|--------------------|--------------|------------------------|
| **baja-rt** | core obligatorio | Baja runtime (BComponent, BOrd, serialization) | BComponent, BOrd.make/get, property binding, action invocation | N/A — core obligatorio |
| **alarm-rt** | core | Alarm management | BAlarmService, BAlarmRecord, queryAlarm, AlarmData (Bloque 54) | Mantener N4.14 |
| **bacnet-rt** | core | BACnet driver integration | Subset: device discovery, point mapping (dashboard) | N/A si hay BACnet devices |
| **box-rt** | core | Box ORM database | Minimal (Reflow NO usa Box ORM intensivo) | Considerar relacional si overhead |
| **bql-rt** | core | Baja Query Language | BReflowBQLCommands (equipment, alarms, history) | BQL 4.14 nativo, sin reemplazo |
| **control-rt** | core | Control points, PID, scheduling | Equipment control, command execution | N/A core |
| **driver-rt** | core | Driver framework base | BACnet, Modbus abstractions | N/A core |
| **history-rt** | core | History storage + Cursor | BHistoryService, BHistory, Cursor, timeQuery (Bloque 60) | Considerar BHistoryDatabase particionado MX60 |
| **net-rt** | core | Network services | WebSocket Jetty, HttpServlet | Jetty embebido N4.14 |
| **platform-rt** | core | Platform services | BUserService, BUser, BUserManager, BFileSystem | API stable |
| **schedule-rt** | core | Scheduling cron/events | SchedulesDataResponse, scheduled exports | N/A core |
| **web-rt** | core | Web module base | HttpServlet raw (NO BWebServlet, AP-3) + WebSocketServlet Jetty | Bloque 51 confirmed: raw, no wrapper |

### 61.2.2 Third-party JARs (inferidos de imports)

| JAR | Versión estimada (N4.14 bundled) | Propósito | Ubicación uso | CVEs / Status | Reemplazo MX60 |
|-----|----------------------------------|-----------|---------------|---------------|----------------|
| **jackson-core** | 2.15+ | JSON serialization (ObjectMapper) | HistoryObjectMapper, ConfigUpdateResponse, JSON parsing | ✅ Sin CVEs abiertos | Mantener; 2.17+ MX60 |
| **jackson-databind** | 2.15+ | Object↔JSON binding, custom serializers | StdSerializer custom (HistoryDeviceSerializer, etc), JsonNode | ✅ 2.15+ patched | jackson-databind 2.17+ MX60 |
| **flipkart-zjsonpatch** | 0.4.12 (approx) | RFC 6902 JSON Patch | BReflowSyncService ConfigSyncTask (Bloque 60) | ⚠️ Audit necesario CVEs parsing | Upgrade 0.4.14+ o RFC 7396 JSON Merge Patch |
| **org.eclipse.jetty** | 9.4.x | WebSocket servlet + HTTP server | WebSocketServlet, ServletUpgradeRequest, @WebSocket | 🔴 Jetty 9.4 EOL 2024; pero N4.14 lo embebe | Jetty 11/12 si rewriting async (constraint N4.14 lo limita) |
| **javax.servlet** | 3.1 | Servlet API | HttpServlet, HttpServletRequest, HttpSession | ⚠️ javax→jakarta rename Jakarta EE 9+ | Jakarta.servlet en N4.x futuras |
| **com.tridium.json** | N/A (Niagara native) | JSON wrapper interno | Configuration JSON, API responses | ✅ Internal Niagara | Keep N4.14 native |
| **java.security** (JDK) | N/A | AccessController.doPrivileged | 9 sites cross-bloques (reflection-based serialization) | 🔴 Deprecated Java 17+, marked-for-removal | StackWalker / VarHandle (Java 11+) |
| **java.io.File** (JDK) | N/A | File I/O | BFileSystem casts | ⚠️ Soft deprecated en favor de NIO | java.nio.file.Path/Files MX60 |

### 61.2.3 Niagara framework APIs usadas — counts cross-codebase

```
AccessController.doPrivileged()    →  9 sites   (Bloque 60: 1 dentro ConfigSyncTask, 8 en helpers)
BOrd.make().get()                  → 15+ sites  (AP-27 cross-bloques 53-60: ~66 total ord lookups)
BDirectory.getChildren()/findFile  → 15 sites   (AP-33 + AP-60 + AP-61)
new Thread() raw                   →  9 sites   (AP-42 + Bloque 60: 5 BReflowSyncService + 2 BackupManager + 2 favorites)
BFileSystem.INSTANCE               → 18 imports
BHistoryService / BHistory         →  5+ sites
BAlarmService / BAlarmRecord       →  2 imports (deeper coverage Bloque 62)
BUserService / BUser               →  0 explícito, vía BReflowUserCommands (Bloque 53)
BJobService                        →  0 calls    ← CRÍTICO: NO se usa thread pool nativo
```

**APIs Baja utilizadas (subset)**:
- `BComponent` — base class para slots/properties
- `BOrd` — object reference; `BOrd.make()` + `.get()` pattern
- `BObject` — serializable value type
- `BProperty` — component property binding
- `BService` / `BAbstractService` — singleton service pattern (BReflowService, BReflowSyncService, BReflowChannelService, BReflowWebSocketAcceptor)
- `BHistoryService.getHistoryGroupNames()` — query history groups
- `BHistory` + `Cursor` — time-series records iterator
- `BAlarmService` + `BAlarmRecord` — alarmas (Bloque 54 + 62)
- `BUserService` + `BUserManager` — auth indirect
- `BFileSystem.INSTANCE` — station filesystem
- `BIFile` + `BIDirectory` — file/directory interfaces
- `BDynamicTimeRange` — BQL time range queries

---

## 61.3 APIs externas (no-Niagara, no-framework)

### 61.3.1 Catálogo de servicios externos

| API | Endpoint | Propósito | Protocolo | Auth | Exposure | Reemplazo MX60 |
|-----|----------|-----------|-----------|------|----------|----------------|
| **weather.niagaramodules.com** | `http://weather.niagaramodules.com/maps?config=...&host=...` | Mapas climáticos overlays | **HTTP (inseguro)** | Query string `config + host` | **MEDIUM** — hostId exposed (AP-57 Bloque 60) | Proxy backend + HTTPS + API keys env vars; o OpenWeatherMap + OpenStreetMap |

**Findings críticos**:
- URL **HTTP no HTTPS** — credentials/queries en cleartext (man-in-the-middle)
- `getHostId()` query param → information disclosure (AP-57)
- Cache key sin config hash → stale cross-region (Implication #135)
- **0 APIs externas adicionales** detectadas en `nmodsreflow-rt/src/`

### 61.3.2 Decisiones MX60

> **NEW Implication 140 — Weather API HTTPS + opaque tokens**: nunca pass-through hostId. Use opaque session token rotated 24h, o eliminar hostId si servicio externo no lo necesita. Migrar a HTTPS obligatorio.

> **NEW Implication 141 — External API allowlist**: MX60 debe mantener allowlist explícita de domains externos accesibles desde station. Default deny. Cualquier nuevo API requiere security review.

---

## 61.4 Patrones de uso de las APIs Niagara

### 61.4.1 BajaScript (frontend → backend)

**Subset usado**: ord, slot, comp, action, subscribe, lease (vía Niagara workbench plugins). NO encontrado en `reflow-frontend/src/` directo — APIs accedidas vía REST/WebSocket (Bloque 53 documenta el bridge real `injectBaja` en bundle webpack).

**Location dev**: `reflow-frontend/plugins/baja.js` — mock/stub para dev server.

### 61.4.2 WebSocketServlet vs BWebServlet (AP-3 confirmed)

**Hallazgo Bloque 51**: Reflow implementa `WebSocketServlet` raw (Jetty), NO `BWebServlet` wrapper.

**Implicación**: sin protecciones BWebServlet automáticas (CSRF, auth layer agregada Niagara). Reflow debe implementar manual:
- CsrfGuard.java (143L) — implementado, pattern KEEP (Bloque 58)
- BUT: NO se aplica a WebSocket upgrade (AP-43 CSWSH, Bloque 59)

> **MX60 decision pending**: ¿BWebServlet wrapper o raw + custom guards explícitos? Bloque 64 evaluará.

### 61.4.3 AccessController.doPrivileged — 9 sites

**Pattern**: `AccessController.doPrivileged((PrivilegedExceptionAction<Void>) () -> { ...; return null; })`.

**Propósito**: escape SecurityManager restrictions para reflection-based JSON serialization + privileged ops.

**Riesgo**: Java 17+ deprecated, marked-for-removal. Niagara N4.14 es Java 11/8 — compatible aún, pero migration path requerida.

**MX60 strategy**:
- Java 16+: `sealed class` + `record` types eliminan reflection necesidad
- Java 11: `VarHandle` + `MethodHandles.Lookup` reemplaza reflection con type safety
- Profile reflection-based serialization cost vs direct `@JsonProperty` annotations

### 61.4.4 BJobService — 0 sites (HALLAZGO CRÍTICO)

**Reflow NO usa BJobService**. Spawns threads directamente (AP-42, AP-49, Bloque 59-60).

**MX60 obligatorio**:
- Regla 13 (Bloque 59): BoundedThreadPoolExecutor o BJobService nativo
- Regla 18 (Bloque 60): cx propagation explícita en thread spawn
- Pre-commit hook: `grep "new Thread\(" -r src/` → REJECT en MX60

### 61.4.5 BFileSystem — 15 sites (AP-33, AP-60, AP-61)

**Pattern Reflow**: `BFileSystem.INSTANCE.getStationHome().getFileSpace().findFile(new FilePath(loc))`.

**Riesgo**: path traversal sin canonicalize (AP-33 file disclosure, AP-60 favorites traversal, AP-61 backup apply asymmetry).

**MX60 obligatorio (Regla 12 Bloque 57 + Regla 19 Bloque 60)**:
- Whitelist explícita de prefixes permitidos
- `Path.normalize()` + `startsWith(allowedRoot)` check
- RBAC explícito `hasFilePermission`
- Audit logging por acceso
- Rate limiting anti-enumeration

### 61.4.6 BOrd.make().get() — 15+ sites (AP-27)

**Pattern Reflow**: `BOrd.make("history:")` o `BOrd.make("station:|slot:/Equipment")`.

**Riesgo**: AP-27 cx propagation ausente (~66 sites cross-bloques 53-60).

**MX60 obligatorio (Regla 11 Bloque 53)**:
- `BOrd.makeComponent(station)` type-safe
- `cx` propagation end-to-end (Commands → helpers → ord lookups)
- NUNCA `.get(null)` o `.get()` sin context en async/thread paths

### 61.4.7 Historical APIs (BHistoryService, BHistory, Cursor)

**Pattern Reflow** (Bloque 60 HistoryDataResponse): `BHistoryDatabase historyDb = (BHistoryDatabase)BOrd.make("history:").resolve().get()`. + `Cursor` lazy iteration.

**Buenos patterns**:
- `.resolve()` defense-in-depth contra ords malformados ✅ KEEP
- Cursor lazy (no full materialization) ✅ KEEP
- timeQuery + recordQuery overloads ✅ KEEP

**Mejoras MX60**:
- Cache singleton `BHistoryDatabase` en BReflowService (Implication #130)
- Stream-based cursor API en lugar de iterator
- Lazy pagination con cursor tokens

### 61.4.8 Alarm APIs

Detalle Bloque 62 (próximo bloque dedicado). Subset usado: BAlarmService singleton + BAlarmRecord immutable + query filter + time range.

### 61.4.9 User/Auth APIs (Bloque 53)

**Pattern Reflow**: indirect vía `BReflowUserCommands`. NO direct import `BUserService` en código Reflow auditado — todo va vía `cx.getUser().getUsername()` + Commands.

**Decisión MX60**: KEEP pattern indirect (less coupling). API stable N4.14.

---

## 61.5 Build tooling — frontend + backend

### 61.5.1 Frontend build

| Component | Tool | Versión | Status |
|-----------|------|---------|--------|
| Build | Vite | 5.4.14 | ✅ CURRENT |
| Plugin Vue 2 | @vitejs/plugin-vue2 | 2.3.3 | ⚠️ TRANSITIONAL (cuando migración Vue 3 → @vitejs/plugin-vue 5.x) |
| Module bundler | Rollup (interno Vite) | 4.x | ✅ CURRENT |
| Target | ES2020+ | — | ✅ |
| Package manager | npm (default) | — | ⚠️ Recomendar pnpm 8+ MX60 |

### 61.5.2 Backend build

| Component | Tool | Versión | Status |
|-----------|------|---------|--------|
| Build | Gradle | desconocido (probable 7-8) | ⚠️ Verificar si 8+ |
| Plugins | `com.tridium.niagara` + `com.tridium.vendor` + `com.tridium.convention.niagara-home-repositories` | N4.14 | ✅ |
| Plugin signing | `com.tridium.niagara-signing` | — | ⚠️ DISABLED (commented out) — habilitar para producción |
| Java version | JDK 8 (Zulu 8 explicit path) | 8 | ⚠️ N4.14 soporta 11+; upgrade recomendado MX60 |
| Repositories | mavenCentral + flatfile (`niagara_home/!bin/ext`, `!modules`) | — | ✅ |
| Output JARs | nmodsreflow-rt.jar + nmodsreflow-ux.jar | — | ✅ |

### 61.5.3 CI/CD

**No GitHub Actions encontrado** — local build only (clean-room build detectada).

**MX60 recomendado**:
- GitHub Actions / GitLab CI: build + test + sign
- Pre-commit hooks: spotless format + checkstyle + spotbugs + grep antipatterns (AP-27, AP-42, AP-49, AP-60)
- Branch protection: require CI green + 1 approval

---

## 61.6 Outdatedness analysis — clasificación + acción

### 61.6.1 Tabla maestra (May 2026 baseline)

| Library | Versión | Status | Acción MX60 | Effort |
|---------|---------|--------|-------------|--------|
| vue | 2.7.16 | 🔴 DEPRECATED | Vue 3.5+ migration | 2-3 sprint |
| vuex | 3.6.2 | 🔴 DEPRECATED | Pinia 2.2+ rewrite | 1 sprint |
| vue-router | 3.6.5 | 🔴 DEPRECATED | Vue Router 4 upgrade | 0.5 sprint |
| view-design | 4.7.0 | 🔴 DEPRECATED | Vuetify 3.5+ o shadcn-vue (150+ comp audit) | 1-2 sprint Vuetify, 2-3 sprint headless |
| axios | 0.21.4 | ⚠️ OUTDATED | 1.7+ upgrade | 4-8 horas |
| socket.io-client | 2.5.0 | 💀 VESTIGIAL | REMOVER | 2-4 horas |
| d3 | 7.9.0 | ✅ CURRENT | KEEP o migrar recharts | 1 sprint si recharts |
| dayjs | 1.11.13 | ✅ CURRENT | KEEP | 0 |
| vue-clipboard2 | 0.3.3 | ⚠️ OUTDATED | @vueuse/core useClipboard | 2-4 horas |
| vue-cookies | 1.7.4 | ⚠️ OUTDATED | js-cookie 3.x o Pinia persist | 2-4 horas |
| vue-drag-resize | 1.5.4 | ⚠️ OUTDATED | @vueuse/core | 4-8 horas |
| vue-masonry | 0.16.0 | ⚠️ OUTDATED | CSS Grid nativo | 4-8 horas |
| vuedraggable | 2.24.3 | ⚠️ OUTDATED | vuedraggable 4.x o SortableJS | 4-8 horas |
| polylabel | 2.0.1 | ✅ CURRENT | KEEP | 0 |
| tinycolor2 | 1.6.0 | ✅ CURRENT | KEEP o colord | 0-4 horas |
| vite | 5.4.14 | ✅ CURRENT | KEEP | 0 |
| jackson-core | 2.15+ | ✅ CURRENT | jackson 2.17+ MX60 | 0 |
| flipkart-zjsonpatch | 0.4.12 | ⚠️ OUTDATED | 0.4.14+ | 2-4 horas |
| jetty | 9.4.x | 🔴 DEPRECATED | Jetty 11/12 (constraint N4.14) | depende N4 upgrade |
| javax.servlet | 3.1 | ⚠️ TRANSITIONAL | Jakarta.servlet futuras N4.x | depende N4 upgrade |
| Java backend | 8 | ⚠️ OUTDATED | Java 11+ | 2-3 días testing + 1-2 sprint refactor |
| Gradle | 7-8 (TBD) | TBD | 8.5+ | 1 día |

### 61.6.2 Effort estimado total migration MX60

| Frontend | Effort |
|----------|--------|
| Vue 2 → Vue 3 + composition API | 2-3 sprint |
| Vuex → Pinia (28 stores) | 1 sprint |
| iView → Vuetify 3 (150+ comp) | 1-2 sprint |
| 7 utility lib upgrades | 1 sprint |
| **Total frontend** | **5-7 sprint** |

| Backend | Effort |
|---------|--------|
| Java 8 → Java 11 upgrade | 1-2 sprint |
| Reglas 11+13+18 (cx propagation) — refactor 66+ AP-27 sites | 2-3 sprint |
| Reglas 12+19 (filesystem sanitizer) — refactor AP-33+60+61 | 1 sprint |
| Reglas 14-17 (WebSocket hardening) | 1-2 sprint |
| Custom BWebServlet wrapper o validation guards | 0.5-1 sprint |
| **Total backend** | **5.5-9 sprint** |

**Total proyecto MX60 greenfield rewrite estimado**: **~10-16 sprint = 5-8 meses con 1-2 devs full-time**.

---

## 61.7 Stack MX60 recomendado — síntesis

### 61.7.1 Frontend stack (greenfield)

| Capa | Recomendación | Alternativa | Justificación |
|------|--------------|-------------|---------------|
| **Framework** | **Vue 3.5+** | React 18, Svelte 4 | Menor rewrite vs Vue 2, equipo familiar |
| **Language** | **TypeScript strict** | — | day-1 obligatorio, mejor DX |
| **State** | **Pinia 2.2+** | Zustand, Redux Toolkit | Oficial Vue core, composable, type-safe |
| **Routing** | **Vue Router 4.x** | TanStack Router | Oficial, lazy code-splitting nativo |
| **UI Kit** | **Vuetify 3.5+** | shadcn-vue (headless) | Maduro, batteries-included, 3.5M dl/week |
| **Charts** | **D3 7.9 (KEEP)** o **recharts 2.10+** | visx, ECharts | D3 si team expert; recharts ergonomic |
| **HTTP** | **axios 1.7+** | fetch + ofetch | Pattern interceptors establecido |
| **WebSocket** | **WebSocket nativo** | — | Remover socket.io-client |
| **Dates** | **dayjs 1.11.13** (KEEP) | date-fns 3+, Temporal API | 2KB, moderno |
| **Colors** | **colord 2.9+** | culori | Modern ESM |
| **Clipboard** | **@vueuse/core useClipboard** | — | Composable native |
| **Cookies** | **js-cookie 3.x** | Pinia persist | Standard |
| **Drag** | **@vueuse/core useDraggable** | SortableJS | Composable |
| **Masonry** | **CSS Grid nativo** | — | Native maduro 2024 |
| **Resize** | **@vueuse/core useResizeObserver** | — | Composable |
| **Build** | **Vite 5.4+** | Rspack, esbuild | Latest stable |
| **Polyfills** | **NONE** | — | ES2020+ targets, modern browsers |
| **Testing unit** | **Vitest 1.0+** | Jest | Vite native, Jest compatible |
| **Testing E2E** | **Playwright 1.40+** | Cypress | Browser automation moderno |
| **Component test** | **@vue/test-utils 2.4+** | — | Oficial |
| **Linting** | **ESLint 8.x + TypeScript-ESLint 6.x + Prettier** | Biome | Standard |
| **Package manager** | **pnpm 8+** | npm, yarn berry | Faster, workspace native |

### 61.7.2 Backend stack (Niagara N4.14 constraint)

| Capa | Recomendación | Justificación |
|------|--------------|---------------|
| **Runtime** | **Niagara N4.14** | Constraint |
| **Java version** | **Java 11+** (upgrade desde 8) | sealed classes, records, var, text blocks |
| **APIs Niagara** | Subset auditado: BComponent, BOrd, BService, BHistory, BAlarm, BFileSystem, BUser | Stable N4.14 |
| **Web layer** | Decisión Bloque 64: BWebServlet wrapper vs raw + guards | TBD |
| **Async** | **BJobService** o **BoundedThreadPoolExecutor** (Regla 13) | Anti-AP-42 |
| **Thread context** | **cx propagation explícita** (Regla 18) | Anti-AP-49 |
| **Filesystem** | **PathValidator + canonicalize + RBAC + audit** (Regla 19) | Anti-AP-33+60+61 |
| **JSON** | **Jackson 2.17+** | Sin CVEs, current |
| **JSON Patch** | **flipkart-zjsonpatch 0.4.14+** o **RFC 7396 Merge Patch** | Audit CVEs |
| **CSRF** | **CsrfGuard pattern** Bloque 58 KEEP literal | + Origin validation WebSocket (Regla 14) |
| **Reflection** | **VarHandle / MethodHandles.Lookup (Java 11+)** | Reemplaza AccessController.doPrivileged donde aplique |
| **Build** | **Gradle 8.5+** | Build cache nativo |
| **Quality** | **Spotless 6.x + SpotBugs 4.8+ + Checkstyle 10.x + ErrorProne 2.24+** | Anti-AP cross-board |
| **Testing** | **JUnit 5 + Mockito 5.x + AssertJ 3.24+ + Niagara station test harness** | Standard |
| **Logging** | **SLF4J + Logback** (Niagara native) | Standard |
| **CI/CD** | **GitHub Actions** + pre-commit hooks (anti-AP grep) | NO existe en Reflow |

### 61.7.3 APIs externas

| Servicio | Recomendación |
|----------|---------------|
| Weather | Proxy backend MX60 con HTTPS + API keys env vars + cache extendido (24h vs 1h) + cache key incluye config hash |
| Mapbox | Si necesario, server-side proxy con API key oculta cliente |
| **General** | Allowlist explícita de domains externos en `application.yaml`. Default deny. Security review por nuevo API. |

---

## 61.8 Antipatterns adicionales descubiertos en audit libs (AP-63+)

### 61.8.1 Frontend

| # | Severity | Título | Site / pattern |
|---|----------|--------|----------------|
| AP-63 | LOW | Vestigial dependency socket.io-client | `package.json` + `api/websocket.js` (8KB dead code) |
| AP-64 | MEDIUM | iView/view-design EOL 2020 sin updates | 150+ componentes (`<i-button>`, `<i-modal>`, `<i-table>`, ...) — CSS exposed potencial XSS sin maintenance |
| AP-65 | LOW | Vue 2.7 EOL Feb 2024 — sin security patches futuros | 378 `.vue` components — aging foundation |
| AP-66 | LOW | axios 0.21.4 EOL 2025 — patches minimal | api/rest.js + plugins/http.js + 12+ vistas |

### 61.8.2 Backend

| # | Severity | Título | Site / pattern |
|---|----------|--------|----------------|
| AP-67 | MEDIUM | `AccessController.doPrivileged` Java 17+ deprecated marked-for-removal | 9 sites (Bloque 60) — migration path requerida |
| AP-68 | LOW | Java 8 outdated — N4.14 soporta Java 11 con sealed classes / records | JDK 8 path Zulu 8 explicit |
| AP-69 | LOW | Niagara signing plugin disabled en build | `build.gradle` commented out |
| AP-70 | LOW | NO BJobService usage — todo `new Thread()` raw | 9 sites cross-codebase (peer de AP-42 + AP-49) |
| AP-71 | LOW | NO CI/CD — local-only build | sin GitHub Actions / GitLab |

**TOTAL AP-1..AP-71 post-Bloque 61** = **71 antipatterns identificados**.

---

## 61.9 MX60 implications — continuación desde #139

| # | Tag | Descripción |
|---|-----|-------------|
| 140 | NEW | Weather API HTTPS obligatorio + opaque tokens (no hostId pass-through). Regla 14 mention. |
| 141 | NEW | External API allowlist en config — default deny, security review por nuevo API. Regla 14 expand. |
| 142 | IMPROVE | Frontend Vue 2 → Vue 3.5+ migration obligatoria (EOL Feb 2024). 378 components. |
| 143 | IMPROVE | Vuex 3 → Pinia 2.2+ rewrite (28 stores). Composable + type-safe. |
| 144 | IMPROVE | vue-router 3 → 4.x upgrade (línea Vue 3). Lazy splitting nativo. |
| 145 | IMPROVE | view-design (iView) → Vuetify 3.5+ o shadcn-vue. Audit 150+ componentes. |
| 146 | IMPROVE | axios 0.21.4 → 1.7+ upgrade trivial. |
| 147 | SKIP | Remover socket.io-client (vestigial 8KB dead code). |
| 148 | KEEP | Vite 5.4 mantener — moderno. Build chain estable. |
| 149 | KEEP | dayjs 1.11.13, d3 7.9, polylabel, tinycolor2/colord — modernos. |
| 150 | NEW | TypeScript strict day-1 obligatorio MX60. |
| 151 | IMPROVE | Java 8 → Java 11+ backend (sealed classes, records, var). |
| 152 | IMPROVE | Gradle → 8.5+ (build cache nativo). |
| 153 | NEW | CI/CD GitHub Actions + pre-commit hooks (anti-AP grep) obligatorio MX60. |
| 154 | NEW | Niagara signing plugin habilitado para producción. |
| 155 | IMPROVE | jackson 2.17+ MX60 (vs 2.15 actual). |
| 156 | IMPROVE | flipkart-zjsonpatch 0.4.14+ audit CVEs o RFC 7396 Merge Patch alternativo. |
| 157 | IMPROVE | AccessController.doPrivileged → VarHandle / MethodHandles.Lookup (Java 11+) donde aplique. |
| 158 | NEW | BJobService obligatorio para async — 0 sites en Reflow es ROOT CAUSE de AP-42, 49, 70. |
| 159 | NEW | Stack frontend = Vue 3 + Pinia + Vuetify 3 + Vite 5 + TS strict + Vitest + Playwright + pnpm. |
| 160 | NEW | Stack backend = Niagara N4.14 + Java 11+ + Gradle 8.5+ + Jackson 2.17 + Spotless+SpotBugs+Checkstyle+ErrorProne + JUnit 5. |

**Total MX60 implications post-Bloque 61**: **160 entries** (139 previos + 21 nuevos: 8 NEW + 11 IMPROVE + 1 SKIP + 1 KEEP).

---

## 61.10 Predicciones / hipótesis a verificar

1. **socket.io-client never initialized**: verificar en producción logs que `initSocket()` is truly never called — confirma vestigial al 100%.
2. **iView component coverage Vuetify 3**: audit 150+ usages para identificar gaps de migration (ej: Vuetify NO tiene equivalent directo de `<i-tree>` pero sí `<v-treeview>`).
3. **BFileSystem path traversal empírico**: test CSV export con paths `../../../etc/passwd` — confirmar AP-33 / AP-60 / AP-61.
4. **BOrd context safety pre-Sys.getStation()**: verificar 15+ calls si alguno ocurre pre-station init (race en startup).
5. **AccessController overhead**: profile reflection-based serialization cost vs `@JsonProperty` annotations (microbenchmark con JMH).
6. **Jetty WebSocket perf 1000 deltas concurrent**: benchmark BReflowChannelService throughput con/sin CSRF interceptors.
7. **Jackson 2.15 + zjsonpatch 0.4.12 compat**: test cross-version regression antes de upgrade Jackson 2.17.
8. **Dayjs DST transitions**: verificar 50+ sites timezone usage no causan sync delta mismatches.
9. **iView CSS dead bytes**: medir bundle size con/sin iView para confirmar 150KB potential reducción Vuetify 3 tree-shaking.
10. **Gradle version**: confirmar versión actual (no aparece en output) para planning upgrade path.

---

## 61.11 Cierre — qué cambia en plan de bloques restantes

### 61.11.1 Inserts en bloques pendientes

- **Bloque 62 (Alarmas dedicado)**: proceder sin cambios; APIs alarm stable.
- **Bloque 63 (Frontend Vue audit)**: agregar sub-task "Audit 378 `.vue` files para Vue 3 composition API patterns" + "Identificar componentes con `<i-*>` view-design para Vuetify 3 migration mapping".
- **Bloque 64 (`-ux` modules + rt remanentes)**: agregar sub-task "Map 150+ view-design usages → Vuetify 3 candidate components" + "Decisión BWebServlet wrapper vs raw + custom guards".
- **Bloque 65 (Cierre Reflow)**: incluir "Stack MX60 final decision document" referenciando este Bloque 61.
- **Bloque 66+ (Analytics)**: sin cambios.

### 61.11.2 Síntesis hallazgos clave para greenfield MX60

1. **Frontend rewrite candidato perfecto**: Vue 2.7 EOL + 4 deprecated libs (vuex, vue-router, view-design, socket.io) + 7 outdated utilities = enough leverage para greenfield rewrite con Vue 3 + Pinia + Vuetify 3 + TS.

2. **Backend conservar 70%**: Niagara APIs estables (N4.14), patterns KEEP (BasicContext wrapping, CopyOnWriteArrayList, Jetty annotations, IReflowCommand polymorphism). Pero hard refactor: 9 raw `new Thread()` + 9 doPrivileged + 66+ AP-27 sites + 150KB CSS reducción Vuetify 3.

3. **Security hardening crítico**: Reglas 11-19 (cross-bloques 53-60) son **decisiones arquitectónicas obligatorias** — no opcional, no negociable. Sin ellas MX60 hereda la deuda completa.

4. **Test harness obligatorio día 1**: Vitest + Playwright + JUnit 5 + Mockito + Niagara station test harness. Reflow tiene 0 tests visibles — gap crítico.

5. **CI/CD obligatorio día 1**: GitHub Actions + pre-commit hooks anti-AP (grep `new Thread\(`, `BOrd.make\(.*\)\.get\(\)` sin cx, filesystem concat). Reflow es local-only build.

6. **Total effort estimado**: 10-16 sprint = 5-8 meses con 1-2 devs full-time. Framework migration + security refactor son los dos epics más grandes.

---

**End of Bloque 61** — catálogo completo librerías + APIs + tecnologías Reflow + reemplazos modernos MX60.

**Siguiente**: Bloque 62 (Alarmas Reflow dedicado — backend Java + frontend Vue + integración Niagara N4 Alarm Console).
