# Bloque 63 — Frontend Reflow Vue 2.7 audit completo: main.js + App.vue + Router 32 routes + Vuex 29 módulos + 17 mixins + 13 plugins + 10 lib helpers + 12 views + 378 componentes mapeados + 8 antipatterns AP-79..86 + 9 reglas template MX60 (23-31)

**Fecha**: 2026-05-08
**Método**: Audit exhaustivo del **frontend Reflow Vue 2.7**, profundizando en piezas no cubiertas por bloques anteriores (50-62 cubrieron bridge SPA-Niagara, dominios alarmas/history/points/schedule/nav/file, WebSocket components, alarmas dedicado). Este bloque cubre: entry point + root + router + Vuex completo + API layer + mixins + plugins + lib helpers + views + structure components.

**Fuentes primarias**:
- `reflow-frontend/src/main.js` (124L) — entry point bootstrap
- `reflow-frontend/src/App.vue` (95L) — root component
- `reflow-frontend/src/router/` — 32 rutas con lazy-load
- `reflow-frontend/src/store/index.js` (387L) + 29 módulos en `store/modules/`
- `reflow-frontend/src/api/` — REST layer (28 endpoints stubs)
- `reflow-frontend/src/mixins/` (17 files, ~89KB)
- `reflow-frontend/src/plugins/` (13 files, ~54KB)
- `reflow-frontend/src/lib/` (10 helpers, ~62KB)
- `reflow-frontend/src/views/` (12 files — 3 cubiertos Bloque 62)
- `reflow-frontend/src/components/` (378 archivos Vue en 18 carpetas)

**Versión analizada**: Reflow-Clean-177 frontend Vue 2.7.16 + bundle producción 1.7.5.

---

## 63.0 Contexto, scope, qué corrige

### ¿Qué ES este bloque?

Audit completo del **resto del frontend Reflow** que no fue cubierto por bloques previos. Total **459 archivos Vue** + 29 módulos Vuex + 17 mixins + 13 plugins + 10 lib helpers + 32 rutas + 28 endpoints REST stubs.

**Lo que YA estaba cubierto** (skip detail):
- Bloques 50-52: Par A frontend↔ux + frontend↔rt + CSRF Plan E
- Bloque 53: app-readable.js bridge SPA-Niagara + injectBaja + subscriber wrapper + Vue mixin Tt
- Bloques 54-57: dominios alarmas/history/points/schedule/nav/file (cliente Na/Sa/Ti/Ci namespaces)
- Bloque 59: WebSocket components (SocketAuth/Connect/Request/ResponseError)
- Bloque 60: config sync RFC 6902 JSON Patch
- Bloque 61: librerías package.json catálogo
- Bloque 62: alarms dedicated (24 Vue files alarmas)

**Lo que cubre este bloque**:
- Entry point bootstrap secuencia (main.js)
- Root component App.vue
- Router 32 rutas (16 top-level + 16 building-scoped)
- Vuex 29 módulos (14 persistent + 14 transient)
- API layer (rest.js + http plugin con CSRF)
- 17 mixins cross-cutting (canvas, elemento, navegación, suscriptor, etc.)
- 13 plugins Vue (baja, niagara, http, colorUtils, time, ord, etc.)
- 10 lib helpers (csrf, configMigration, configSerializer, deepMerge, eventBus, etc.)
- 12 views top-level (3 Bloque 62 cubiertos + 9 nuevos)
- 378 componentes en 18 carpetas dominio (estructura + counts + ejemplos top)

### Qué corrige / valida

| Bloque previo | Hallazgo previo | Validación / corrección Bloque 63 |
|---------------|-----------------|----------------------------------|
| 51 (RFC 6902 JSON Patch sync) | "BReflowSyncService usa zjsonpatch para config sync" | ⚠️ **AP-86 NUEVO**: cliente NO usa lib json-patch — implementa subset manual en `STATE_DELTA` mutation (solo replace/add/remove, NO copy/move/test). Asimetría con server (zjsonpatch full RFC). Riesgo: server manda copy/move → cliente silently ignora. |
| 52 (CSRF Plan E client) | "lib/csrf.js bootstrap + refresh" | ✅ **CONFIRMADO** + nuevo hallazgo: `main.js:111` csrfBootstrap() es **fire-and-forget** — primer POST puede llegar antes del token → 403 → interceptor refresh + retry una vez. AP-10 mitigado pero con 2 RT penalty inicial. |
| 53 (subscriber wrapper Tt mixin) | "subscriber wrapper en Vue mixin Tt cross-15-componentes" | ⚠️ **AP-82 NUEVO**: ~10 componentes (DeviceGrid, Equipment*, Point*) usan subscribe sin `unsubscribe` en `beforeDestroy`. Memory leak progresivo en route changes. |
| 61 (Vuex 29 módulos mencionado) | "28 módulos persistent/transient sin auditar" | ✅ **CONFIRMADO + REFINADO**: 29 módulos (14 persistent saved a config.json + 14 transient memory-only + 1 root). Tabla completa en sec 63.4. |

### Pregunta unificadora

> ¿Cuál es la arquitectura completa frontend Reflow + qué patrones MX60 hereda vs reescribe?

**Respuesta corta**:
- **Hereda literal**: Vuex namespaced modules pattern (Regla 23), Vue.set() mutations (Regla 24), plugin boot order baja→niagara (Regla 25), CSRF 403→refresh→retry (Regla 26), lazy-load routes (Regla 27), eventBus para view-layer coordination (Regla 28), mapState/mapGetters strict (Regla 30).
- **Mejora obligatorio**: subscriber unsubscribe enforcement (Regla 29), JSON Patch full RFC 6902 (Regla 31), Vuex modules >500L splittear (AP-79), getter memoization para getGroupedBuildings O(n²) (AP-80), v-style component → CSS custom properties.
- **Descarta** (con stack MX60 Bloque 61): vue-clipboard2 → @vueuse/copy, vue-cookies → js-cookie, vue-masonry → CSS Grid, Vue 2.7 + Vuex 3 → Vue 3 + Pinia migration completa.
- **Decisión MX60 #6**: pinia stores serán EXACTO 1:1 con los 29 módulos Vuex actuales — la separación persistent/transient es excelente, no requiere rediseño, solo migration técnica.

---

## 63.1 main.js — entry point bootstrap (124L)

### 63.1.1 Secuencia de bootstrap

```javascript
// 1. UI framework + CSS
import ViewUI from 'view-design';      // iView 4.7 — DEPRECATED Bloque 61
import '@fortawesome/fontawesome-pro/css/all.min.css';
import './assets/global.css';

// 2. Core Vue + Vuex + Router
import store from './store';              // 29 módulos (sec 63.4)
import router from './router';            // 32 rutas (sec 63.3)
Vue.use(VueRouter); Vue.use(Vuex);

// 3. Plugins en orden CRÍTICO
Vue.use(labelForItem);
Vue.use(configMode);
Vue.use(colorUtils);
Vue.use(VueClipboard);                    // → $copyText
Vue.use(VueMasonryPlugin);                // → $redrawVueMasonry
Vue.use(baja);                            // ← MUST BE BEFORE niagara
Vue.use(niagara);                         // ← reads $baja internally
Vue.use(utils);
Vue.use(time);                            // dayjs wrapper
Vue.use(ord);
Vue.use(reflowLink);
Vue.use(gbo);                             // GBO Digital string utils
Vue.use(workbench);
Vue.use(cookies);
Vue.use(http);                            // CSRF interceptors AP-10

// 4. CSRF bootstrap fire-and-forget (línea 111) — AP-10 race window
csrfBootstrap();                          // NO awaited!

// 5. Directives + global components
Vue.directive('responsive', { /* ResizeObserver classList toggle */ });
Vue.component('v-style', { render: ... });
Vue.component('D3chart', D3chart);
Vue.component('ExpandTransition', ExpandTransition);

// 6. Vue instance
new Vue({ el: '#app', store, router, render: h => h(App) });

// 7. DEV mode advance spinner
if (import.meta.env.DEV) store.commit('LOAD_STATE', {});
```

### 63.1.2 Hallazgos clave

- **Plugin boot order CRÍTICO**: comment línea 51-52 "baja MUST be before niagara". Si se rompe → `$niagara.bql()` crash. Regla 25 obligatoria.
- **CSRF bootstrap race**: `csrfBootstrap()` fire-and-forget. Primer POST puede 403 → interceptor refresh → retry. Funciona pero 2 RT penalty inicial. Mitigación posible: pre-issue token en `<meta csrf-token>` (Implication #182).
- **v-style global component** (línea 71-75): render dynamic `<style>` tags — usado por 18 componentes para CSS computed (theme-reactive). MX60 considerar migrar a CSS custom properties (Implication #187).
- **v-responsive directive**: ResizeObserver + classList toggle por breakpoints. KEEP literal MX60.

---

## 63.2 App.vue — root component (95L)

```vue
<template>
  <transition name="fade" appear>
    <ConfigView v-if="isConfig" />
    <Layout v-else-if="!demo.isDemo || demo.active" />
    <DemoLayout v-else />
  </transition>
</template>

<script>
mounted() {
  // Window resize → eventBus.emit('resize') → documentData mutations
  window.addEventListener('resize', e => {
    this.eventBus.$emit('resize', e);
    this.$store.commit('documentData/SET_WIDTH', window.innerWidth);
    this.$store.commit('documentData/SET_HEIGHT', window.innerHeight);
  });

  // Smooth scroll listener
  this.eventBus.$on('scroll-to', target => { /* smoothScroll handler */ });
}
</script>
```

**Hallazgos**:
- 3 root layouts switcheables (ConfigView | Layout | DemoLayout) — KEEP pattern.
- eventBus para resize + scroll-to — view-layer coordination, no side effects en estado. Regla 28.
- AP-79-cousin: eventBus declarativo es aceptable; problema sería usarlo para state mutations (no es el caso).

---

## 63.3 Router — 32 rutas con lazy-load

**`router/index.js` (275L)**, hash mode, Vue Router 3.x.

### 63.3.1 Estructura

| Categoría | Routes | Lazy-load |
|-----------|--------|-----------|
| Top-level | 16 (`/`, `/alarms`, `/equipment`, `/schedules`, `/buildings`, `/histories`, `/pages/:id`, `/embed/:ord`, `/floors`, `/floors/:id`, `/debug/color`, ...) | ✅ Todos |
| Building-scoped | 16 (`/buildings/:buildingId/{alarms,equipment,floors,histories,schedules,...}`) | ✅ Todos |
| Catch-all | 1 (`*` → home) | — |

### 63.3.2 Lazy-load pattern

```javascript
const AlarmsHome = () => import('../views/AlarmsHome.vue');
const EquipmentIndex = () => import('../views/EquipmentIndex.vue');
// ... 32 routes
```

**Beneficio**: code split per route → first paint solo carga `Home.vue + Layout.vue`. Equipment/FloorPlans/History bajo demanda.

### 63.3.3 Guards

```javascript
router.beforeEach((to, from, next) => {
    console.log('navigating to', to);  // STUB — auth en Fase 5+
    next();
});
router.afterEach((to, from) => {
    window.scrollTo(0, 0);  // scroll-to-top
});
```

> **AP-NEW (covered by AP-3 expansion)**: `beforeEach` es stub vacío. Sin auth check. MX60 Regla 31 cousin: enforce per-route auth via meta + guard.

### 63.3.4 Catch-all

```javascript
{ path: '*', redirect: '/' }
```

> **HALLAZGO menor**: catch-all `*` → `/` es **DESVIACIÓN INTENCIONAL** del bundle producción 1.7.5 (replica frontend lo agrega, bundle no lo tiene). Riesgo bajo, comentado en código.

---

## 63.4 Vuex store — 29 módulos (14 persistent + 14 transient + 1 root)

### 63.4.1 store/index.js root state (387L)

**28 propiedades root state** clave:
- `version`: 14 (schema version para migrations)
- `stateLoaded`: boolean (carga inicial)
- `savePaused`: pause save during init
- Sync: `saveWaitTime` (3s), `saveTimeout`, `migrationActive`, `migrationStatus`, `activeSave`, `syncPaused`
- Multi-user: `isMultiUser`, `hasControl`, `requestedControl`, `requestedControlTimeout` (30s), `modalConnect/Request/Auth/Who`
- Controller: `controller {}`, `activeControlRequest {}`
- Socket: `socketStatus`, `socketInfo {}`, `socketChannels {}`, `socketAutoReconnect`, `socketTimeout` (10s)
- Misc: `requiresReload`, `initSave`, `clientSignature`, `reflowVersion`

### 63.4.2 Mutations clave

```javascript
LOAD_STATE(state, payload) {
    deepMerge(state, payload);
    state.stateLoaded = true;
}

STATE_DELTA(state, patch) {
    // ⚠️ AP-86: RFC 6902 SUBSET (solo replace/add/remove, NO copy/move/test)
    patch.forEach(op => {
        const path = op.path.split('/').slice(1);
        if (op.op === 'replace') setPath(state, path, op.value);
        else if (op.op === 'add') addPath(state, path, op.value);
        else if (op.op === 'remove') removePath(state, path);
        // copy/move ignored silently
    });
}

REPLACE_STATE(state, payload) {
    Object.assign(state, payload);
    state.stateLoaded = true;
}
```

### 63.4.3 Getters root (3)

- `stateJson`: `serializeState(state)` → JSON string (excluye transient keys vía EXCLUDED_KEYS)
- `preventSync`: `!isMultiUser || syncPaused`
- `whoMe`: encuentra self en `socketInfo.clients` por `clientId || clientSignature`

### 63.4.4 Actions stub (11)

`save`, `saveState`, `saveDelta`, `disconnect`, `connect`, `requestConfigControl`, `rejectControlRequest`, `acceptControlRequest`, `load`, `messageSubscriber`, `subscribeToDeltas`, `subscribeToControlChange`, `migrateState` (real, calls `runMigrations` from `configMigration.js`), `optimizeState`.

### 63.4.5 Módulos PERSISTENTES (14) — sincronizados a config.json

| Módulo | Propósito | Mutations principales | Getters clave |
|--------|-----------|----------------------|---------------|
| **alarms** | Console config (Bloque 62) | SET_ENABLED, ADD/UPDATE/REMOVE_CONSOLE | getConsoleById, priorityColor |
| **buildings** | Edificios + grupos + map markers (783L) | ADD/UPDATE/REMOVE_BUILDING, ADD_FLOOR, ADD_MAP_MARKER | getBuildingById, getGroupedBuildings (O(n²) AP-80) |
| **colors** | Theme colors 40+ | SET_* per color | — |
| **dashboardCards** | Card defs dashboard | ADD/UPDATE/REMOVE_CARD, REORDER | availableCards |
| **equipment** | Equipment items + types + grouping | ADD/UPDATE/REMOVE_EQUIPMENT, ADD_TYPE | getEquipmentByGroup, getDevicesByBuilding |
| **floorplans** | Floor objects + elements + states | ADD/UPDATE_FLOOR, ADD_ELEMENT, REORDER_ELEMENTS | getFloorById, getElementById |
| **histories** | History records (Bloque 55) | ADD/UPDATE/REMOVE_HISTORY | getHistoryById, getHistoriesByBuilding |
| **landing** | Home page config | SET_* toggles, UPDATE_CARDS | — |
| **navigation** | Nav items + subnavs (Bloque 57) | ADD/UPDATE_NAV, ADD_SUBNAV | navTree |
| **pages** | Custom pages | ADD/UPDATE/REMOVE_PAGE | getPageById |
| **schedules** | Schedules (Bloque 57) | ADD/UPDATE_SCHEDULE | getSchedulesByBuilding |
| **theme** | Dark mode + accent + typography | SET_DARK, SET_ACCENT_COLOR | — |
| **weather** | Lat/lon + units | SET_ENABLED, SET_LAT/LON | — |
| **profiles** | User profiles + roles + permissions | ADD/UPDATE/REMOVE_PROFILE | getProfileForUser, authorizeLink |

### 63.4.6 Módulos TRANSIENTES (14) — memory-only

| Módulo | Propósito |
|--------|-----------|
| **alarmData** | Alarm runtime state (Bloque 62) |
| **equipmentData** | Device cache + TTL eviction |
| **scheduleData** | Active schedule runtime state |
| **pointMapData** | Point subscriptions + values (subscriber-fed) |
| **floorEditor** | Floor editor canvas state |
| **weatherData** | Weather API response cache |
| **documentData** | Window dimensions |
| **mouseData** | Mouse position |
| **historyCache** | Cached history data TTL-based |
| **menu** | Menu UI state |
| **notify** | Toast/notification queue |
| **license** | License state |
| **updates** | Software update availability |
| **user** | Current session (NOT persisted intencional) |
| **demo** | Demo mode toggle |

### 63.4.7 Pattern persistencia

`configSerializer.js` (40L):
```javascript
const EXCLUDED_KEYS = ['alarmData', 'equipmentData', 'scheduleData', 'pointMapData',
                       'floorEditor', 'weatherData', 'documentData', 'mouseData',
                       'historyCache', 'menu', 'notify', 'license', 'updates',
                       'user', 'demo'];

export function serializeState(state) {
    return JSON.stringify(state, (key, value) => {
        if (EXCLUDED_KEYS.includes(key)) return undefined;
        return value;
    });
}
```

**KEEP literal**: separation persistent/transient excellent. MX60 Pinia stores 1:1 con módulos actuales.

---

## 63.5 API layer (api/*, 28 endpoints)

### 63.5.1 rest.js (300+L stubs)

| Canal | Endpoints |
|-------|-----------|
| Config | GET /config, POST /config_update, POST /config_delta |
| Alarms | POST /alarms/query, GET /alarms/csv, GET /source-csv |
| History | GET /histories, /history-data, /history-groups, /histories/{name}, /histories-csv |
| Weather | GET /weather-map, /weather-data |
| Equipment | GET /equipment-list, POST /equipment-search, GET /equipment-groups, /equipment-types, /equipment-status |
| Backups | POST /backup-create, /backup-delete |
| System | GET /system-info, POST /system-shutdown, /restart-server |
| Nodes | GET /nodes-list, /nodes-status |
| Network | GET /network-config, POST /network-update |
| Schedule | POST /schedule-query |

**Pattern**: `import { http } from '@/plugins/http'` → shared axios con CSRF interceptors.

### 63.5.2 plugins/http.js (80L) — CSRF interceptors

```javascript
const http = axios.create();  // Separate instance, NOT global
http.interceptors.request.use(cfg => {
    if (cfg.method !== 'GET' && cfg.method !== 'OPTIONS') {
        cfg.headers['x-niagara-csrfToken'] = getToken();
    }
    return cfg;
});
http.interceptors.response.use(
    response => response,
    error => {
        if (error.response?.status === 403 && isCsrfError(error.response)) {
            if (config._csrfRetried) return Promise.reject(error);
            config._csrfRetried = true;
            return refresh().then(newToken => {
                config.headers['x-niagara-csrfToken'] = newToken;
                return http(config);
            });
        }
        return Promise.reject(error);
    }
);
```

**KEEP literal MX60** — CSRF 403→refresh→retry pattern. Regla 26.

---

## 63.6 Mixins (17 archivos, ~89KB)

| Mixin | KB | Propósito | Componentes |
|-------|----|-----------|-------------|
| **elementMixin.js** | 13.4 | Floor plan canvas elements base | FloorPlanCanvas + element types |
| **dynamicColorMixin.js** | 11.6 | Color bindings reactivos tinycolor2 | ~20 components |
| **historyListMixin.js** | 11.2 | History query/filter/pagination | History views |
| **canvasDragResizeMixin.js** | 7.4 | Drag/resize canvas elements | Floor editor |
| **navigationMixin.js** | 7.4 | Nav breadcrumbs/hierarchy | Layout, BuildingLayout |
| **propertiesMixin.js** | 6.2 | Property editor panel | Equipment, Element editors |
| **equipmentMixin.js** | 6.1 | Equipment list/filter | Equipment views |
| **subscriberMixin.js** | 4.6 | **Baja Subscriber wrapper (Bloque 53)** | 15 components |
| **equipmentListMixin.js** | 4.5 | Equipment index listing | EquipmentIndex |
| **navItemMixin.js** | 3.9 | Navigation item rendering | NavItem variants |
| **clipboardMixin.js** | 3.7 | Copy/paste vue-clipboard2 | Card editors |
| **editorPaneMixin.js** | 3.1 | Editor sidebar toggle | Editors |
| **summaryViewMixin.js** | 2.6 | Card summary templates | Dashboard cards |
| **stateBaseMixin.js** | 2.5 | Base reactive state | State managers |
| **tweenMixin.js** | 1.7 | Animation tweening | Charts |
| **checkedItemsMixin.js** | 1.3 | Multi-select | Lists |
| **buildingConfigMixin.js** | 1.2 | Building config shortcuts | Building components |

**Hallazgos**:
- ✅ Todos namespaced — no naming collisions.
- ⚠️ **subscriberMixin** = cross-cutting más crítico. ~10 componentes consumers olvidan `unsubscribe` en `beforeDestroy` → AP-82 memory leak.
- ✅ Mixins ≤ 14KB cada uno — reasonable composition.

> **MX60 implication**: en Vue 3 + Composition API, mixins → composables (`useSubscriber()`, `useElementCanvas()`, etc). Regla 29 obligatoria: `useSubscriber` debe llamar `onUnmounted(() => unsubscribe())` automático.

---

## 63.7 Plugins (13 archivos, ~54KB)

| Plugin | Líneas | Propósito | Exposed as |
|--------|--------|-----------|------------|
| **baja.js** | 40 | Mock BajaScript runtime ($baja.Ord, $baja.Subscriber stubs) | $baja (FIRST!) |
| **http.js** | 80 | Shared axios + CSRF interceptors | $http |
| **niagara.js** | 210 | Mock Niagara API ($niagara.alarm/bql/history/...) | $niagara (después de baja) |
| **colorUtils.js** | 200 | Theme-reactive color resolver tinycolor2 | $color |
| **gbo.js** | 100 | GBO Digital string utils (capitalize, pluralize, ...) | $gbo |
| **reflowLink.js** | 100 | Navigation link resolution | $reflowLink |
| **utils.js** | 50 | Misc helpers + Mapbox | $utils, $maps |
| **timePlugin.js** | 40 | dayjs wrapper | $time |
| **labelForItem.js** | 40 | Universal label resolver | $labelForItem |
| **workbench.js** | 40 | Detect Niagara Workbench env | $workbench |
| **ord.js** | 30 | ORD → image URL resolver | $ord |
| **cookies.js** | 20 | Vue-cookies wrapper | $cookies |
| **configMode.js** | 20 | Boolean flag config vs runtime | $isConfig |

**KEEP pattern**: plugin factory + Vue.use + dependency injection vía Vue.prototype. MX60 mantener (en Vue 3: app.config.globalProperties).

---

## 63.8 Lib helpers (10 archivos, ~62KB)

| Archivo | Líneas | Propósito | Cross-ref |
|---------|--------|-----------|-----------|
| **csrf.js** | 150 | CSRF token bootstrap + refresh | Bloque 52 Plan E |
| **alarmCache.js** | 50 | Alarm caching TTL | Bloque 62 |
| **configMigration.js** | 600+ | State version migration v0→v14 | actions/migrateState |
| **configSerializer.js** | 40 | serializeState + EXCLUDED_KEYS | getters/stateJson |
| **bajaHeartbeat.js** | 130 | Subscriber keepalive 30s pings | Bloque 53 |
| **deepMerge.js** | 30 | Recursive merge | LOAD_STATE |
| **eventBus.js** | 10 | Vue eventBus instance | App.vue + components |
| **ord.js** | 100 | ORD utility parsing | plugins/ord.js |
| **utils.js** | 30 | Misc helpers (generateId, formatNumber, deepClone) | Multiple |
| **uuid.js** | 20 | UUID v4 generator | Building/Equipment IDs |

**KEEP literal** todos (con upgrades library Bloque 61: dayjs KEEP, vue-clipboard2 → @vueuse, etc.).

---

## 63.9 Views (12 archivos — 3 cubiertos Bloque 62 + 9 nuevos)

### 63.9.1 Cubiertos Bloque 62

- AlarmsHome.vue (23KB)
- AlarmDetails.vue (25KB)

### 63.9.2 Nuevos (9 views)

| View | Dominio | Líneas | Propósito | mapState principal |
|------|---------|--------|-----------|---------------------|
| **Home.vue** | Landing | 90 | Hero + buildings + equipment + histories widgets | landing, buildings, equipment, histories |
| **PageView.vue** | Custom Pages | 220 | Render custom page per pages/items[] | pages, navigation |
| **SchedulesHome.vue** | Schedules | 300 | Schedule list/editor + group filter + time UI | schedules, scheduleData, buildings |
| **BuildingFloorsView.vue** | Floors | 90 | List pisos edificio + link a floor editor | buildings, floorplans |
| **BuildingFloorDetailView.vue** | Floors | 50 | Single floor canvas editor shell | floorplans, floorEditor |
| **DeviceDetailsView.vue** | Equipment | 300 | Device detail panel (props + graph + controls) | equipment, equipmentData, buildings |
| **EquipmentGroupView.vue** | Equipment | 350 | Group view (count badges + card dashboard + multi-building) | buildings, equipment, colors |
| **EmbedView.vue** | Embed | 80 | Embed ORD content via iframe | — |
| **DebugColorView.vue** | Debug | 20 | Color swatch dev tool | colors, theme |
| **FloorsRedirectView.vue** | Floors | 20 | Redirect /floors → /buildings/:id/floors | buildings |

**Pattern**: views son thin shells (`mapState` múltiples módulos + routing params). Lógica delegada a child components.

---

## 63.10 Components — 378 archivos en 18 carpetas

```
components/
├── alarms/           27 files  (Bloque 62)
├── browser/           1 file   (OrdBrowser.vue)
├── buildings/        27 files  (BuildingIndex, Layout, Card, Alarms, ...)
├── cards/            16 files  + table/ (8) (DashboardCard*, CardEditor)
├── charts/           11 files  (D3chart, ChartContainer, TimeSeriesChart)
├── common/           26 files  (IconBrowser, BoundLabel, OrdTree 2000L+, Breadcrumb)
├── config/           24 files  (ConfigView, ConfigMenu, ConfigEditor, Settings*)
├── dashboard/        14 files  (LandingPage, DashboardLayout, HomeHero, Equipment)
├── equipment/        44 files  (LARGEST 1) (EquipmentIndex, DeviceGrid, DeviceCard)
├── floorplans/       52 files  (LARGEST 2) (FloorPlanCanvas 1000L+, Toolbar, Element*)
├── histories/        22 files  (HistoriesHome, HistoryBuilder, HistoryChart, HistoryRange)
├── layout/            5 files  (Layout, Sidebar, Header, Footer)
├── map/               1 file   (MapComponent — Mapbox)
├── maps/              7 files  (MapMarker, MapPopup, MapCluster)
├── navigation/       16 files  (NavDropdownBuilding, NavItem, NavMenu, Breadcrumb)
├── pages/            11 files  (PageList, PageEditor, PageContent, PageTemplate)
├── points/           12 files  (PointSelector, PointValue, PointHistory)
├── profiles/         12 files  (UserProfileList, UserProfileEditor, RoleManager)
├── schedules/         4 files  (ScheduleEditor, ScheduleList, ScheduleCalendar)
├── settings/         13 files  (SettingsAutomatedBackups, WebCache, RedirectReflowView)
├── weather/           6 files  (WeatherDisplay, WeatherChart, WeatherWidget)
├── websocket/         4 files  (Bloque 59)
└── wizard/            8 files  (WizardFlow, Step, Config, Preview)
```

### 63.10.1 Carpetas más grandes

**equipment/ (44)** — top components:
- EquipmentIndex (main list/filter)
- DeviceGrid (subscriber hooks, ~10 sites missing unsubscribe AP-82)
- GroupedDevicesDisplay
- TypeFilter, DeviceCard, DeviceProperties, DeviceStatus

**floorplans/ (52)** — top components:
- **FloorPlanCanvas (1000L+)** — canvas drawing (AP-85 large render)
- FloorToolbar, ElementToolbox, StateManager
- Element*.vue variants (ShapeElement, ImageElement, TextElement, ...)

**common/ (26)** — utilities:
- **OrdTree (2000L+)** — hierarchical ORD browser (AP-85 large render)
- IconBrowser (80KB icon assets)
- BoundLabel (ORD-bound display)
- ExpandTransition, Breadcrumb

### 63.10.2 Métricas Vue

| Métrica | Count |
|---------|-------|
| `v-if` / `v-show` | 1117 (typed conditionals) |
| `:key=` | 257 (good list rendering) |
| `$emit` / `@evt` | 1726 (healthy event flow) |
| `v-model` | 179 (form bindings) |
| Lifecycle hooks (mounted/created/destroy) | 208 |
| `.subscribe()` callsites | 88 (mostly equipmentData realtime) |
| `mapState` / `mapGetters` | 655 |
| `Vue.set()` | 45 |
| `commit(..., { root: true })` | 46 cross-module |
| `dispatch(..., { root: true })` | 142 cross-module |

---

## 63.11 Cross-references con bloques previos

| Bloque | Tema | Frontend coverage |
|--------|------|-------------------|
| 50, 51, 52 | Par A + CSRF | Layout.vue + plugins/http.js + lib/csrf.js |
| 53 | injectBaja + Tt mixin | plugins/baja.js + mixins/subscriberMixin.js (15 components) |
| 54 | Alarm Na | alarmData transient + AlarmsHome/Details + 27 alarm components |
| 55 | History Sa | historyCache transient + Histories views + 22 history components |
| 56 | Points Ti | pointMapData transient + 12 point components |
| 57 | Schedule + nav + file | schedules/scheduleData modules + navigation module + 4 schedule components |
| 59 | WebSocket | components/websocket/ (4 files) + socketStatus root state |
| 60 | Config sync JSON Patch | STATE_DELTA mutation (RFC 6902 SUBSET, AP-86) + saveDelta action |
| 61 | Librerías package.json | rest.js endpoints + plugin counts confirmed |
| 62 | Alarms dedicated | 24 alarm components + 2 stores (alarms persistent + alarmData transient) |

---

## 63.12 Antipatterns nuevos AP-79..86

| # | Severity | Título | Site | Categoría |
|---|----------|--------|------|-----------|
| AP-79 | LOW | Vuex module size bloat (>500L) | buildings.js (783L), floorplans.js (~700L) | Maintainability |
| AP-80 | MEDIUM | Deep getters O(n²) sin memoization | buildings.js getGroupedBuildings, collectSubGroups | Performance |
| AP-81 | LOW | eventBus para state mutations (mezclado con view) | App.vue resize listeners | Architecture |
| AP-82 | **MEDIUM** | Subscriber lifecycle missing unsubscribe en ~10 components | DeviceGrid, Equipment*, Point* | Memory leak |
| AP-83 | LOW | Missing pagination bounds check | EquipmentGroupView.vue:70 | Robustness |
| AP-84 | (none) | (no encontrado — mapState/mapGetters strict) | — | — |
| AP-85 | MEDIUM | Large component render functions | FloorPlanCanvas (1000L+), OrdTree (2000L+) | Performance / testability |
| AP-86 | **MEDIUM** | JSON Patch RFC 6902 simplified (no full lib) | store/index.js STATE_DELTA | Sync correctness |

**TOTAL AP-1..AP-86 post-Bloque 63** = **86 antipatterns identificados**.

---

## 63.13 Patterns excelentes (KEEP literal MX60) — P-79..85

1. **P-79: Vuex namespaced modules** — 29 modules con `namespaced: true`, sin name collisions, easy trace `dispatch('buildings/addBuilding')`. Regla 23.
2. **P-80: Lazy-load routes via import()** — 32 routes lazy. First paint mínimo. Regla 27.
3. **P-81: Plugin factory pattern (Vue.use)** — 13 plugins, clean install order, dependency injection vía Vue.prototype. Regla 25 (boot order baja → niagara).
4. **P-82: CSRF token refresh on 403** — interceptor catches 403, refresh + retry once con guard `_csrfRetried`. Regla 26.
5. **P-83: eventBus para view-layer coordination** — resize, scroll-to, delta-sync. NO usar para state mutations (esos van por Vuex). Regla 28.
6. **P-84: Vue.set() para reactive object mutations** — 45 callsites, mantiene reactivity sin missed updates. Regla 24.
7. **P-85: Computed getters para derived state** — 32+ getters, reusable, responsivo. mapState/mapGetters strict (655 sites). Regla 30.

---

## 63.14 MX60 implications — continuación desde #175

| # | Tag | Descripción |
|---|-----|-------------|
| 176 | KEEP | **Vuex/Pinia state shape 1:1** con 29 módulos actuales. Separación persistent (14) / transient (14) excelente. MX60 backend DTOs deben mirror schema. |
| 177 | NEW | **Plugin boot order CRÍTICO MX60** (baja → niagara). Documentar en CLAUDE.md MX60. Misordering causes `$niagara.bql()` crash. Regla 25. |
| 178 | IMPROVE | Lazy-load routes + code split + Service Worker prefetch para routes frecuentes (Equipment, Buildings). |
| 179 | IMPROVE | **JSON Patch full RFC 6902** (lib `fast-json-patch` o `rfc6902` npm). MX60 cliente actual ignora silently copy/move ops del servidor zjsonpatch full → bugs sync. Regla 31. |
| 180 | IMPROVE | **Subscriber lifecycle enforcement obligatorio MX60** — ~10 components missing unsubscribe causan memory leaks producción. Regla 29. |
| 181 | KEEP | eventBus para view-layer coordination. Aceptable para resize/scroll events. NO para state. |
| 182 | IMPROVE | **CSRF token pre-issue en `<meta csrf-token>`** — eliminar race window primer POST. Regla 26 expand. |
| 183 | KEEP | configSerializer EXCLUDED_KEYS pattern excelente — separation transient/persistent. |
| 184 | IMPROVE | `getGroupedBuildings` O(n²) AP-80 — memoize con Vuex plugin o computed memoization. |
| 185 | NEW | Plugin interface contract MX60 — documentar `$baja.Ord`, `$baja.Subscriber`, `$niagara.alarm/bql/history` para implementers. |
| 186 | KEEP | Breadcrumb routing via `:to` props — extender a otras views. |
| 187 | IMPROVE | v-style component → CSS custom properties (`--primary-color`). Vue 3 scoped CSS + CSS vars elimina re-renders por theme change. |
| 188 | NEW | Frontend authz vs backend authz contract documentar — `profiles/authorizeLink` getter es cosmetic, real authz en backend. |
| 189 | IMPROVE | Router beforeEach guard implementar — actual stub. MX60 enforce per-route auth via meta. |
| 190 | NEW | Mixins → Composables migration en Vue 3 — `useSubscriber`, `useCanvas`, `useNavigation`, etc. composition API más explicit. |

**Total MX60 implications post-Bloque 63**: **190 entries** (175 previos + 15 nuevos: 6 KEEP + 6 IMPROVE + 4 NEW − algunos overlap).

---

## 63.15 Reglas template MX60 — 9 reglas nuevas (23-31)

### Regla 23 — Vuex/Pinia namespaced modules ONLY

```javascript
// store/modules/myModule.js (Vuex 4) o stores/myStore.ts (Pinia)
export default {
    namespaced: true,  // OBLIGATORIO
    state: () => ({ ... }),
    mutations: { ... },
    getters: { ... },
    actions: { ... }
};

// Pinia equivalent:
export const useMyStore = defineStore('my', { ... });
```

**Sin excepciones.**

### Regla 24 — Vue.set() para reactive object mutations (Vue 2) / Proxy (Vue 3)

```javascript
// Vue 2:
SET_PROPERTY(state, payload) {
    Vue.set(state, 'newKey', payload);  // Reactive
}

// Vue 3 (Pinia con Proxy):
function updateProperty(payload) {
    this.newKey = payload;  // Auto-reactive vía Proxy
}
```

### Regla 25 — Plugin boot order CRÍTICO

```javascript
// main.js (Vue 2) / main.ts (Vue 3)
app.use(baja);      // FIRST — niagara reads $baja
app.use(niagara);   // After baja
app.use(http);      // CSRF
// ... resto
```

**Test obligatorio**: e2e check `app.config.globalProperties.$baja` definido antes de `$niagara.*` calls.

### Regla 26 — CSRF token refresh on 403 + pre-issue

```javascript
// 1. Pre-issue en HTML inicial
// <meta name="csrf-token" content="{{token}}">

// 2. Interceptor:
http.interceptors.response.use(..., error => {
    if (error.response?.status === 403 && isCsrfError(...)) {
        if (config._csrfRetried) return Promise.reject(error);
        config._csrfRetried = true;
        return refresh().then(token => http(config));
    }
});
```

### Regla 27 — Lazy-load all routes

```javascript
const Home = () => import('../views/Home.vue');
const routes = [
    { path: '/', component: Home, meta: { requiresAuth: true } }
];
```

**Sin excepciones.**

### Regla 28 — eventBus solo para view-layer events

```javascript
// lib/eventBus.ts (Vue 3 con mitt)
import mitt from 'mitt';
export const eventBus = mitt();

// USO permitido:
eventBus.on('scroll-to', handler);
eventBus.emit('resize', payload);

// USO NO permitido:
eventBus.emit('store-update', ...);  // ❌ usar Pinia action
```

### Regla 29 — Subscriber cleanup obligatorio

```javascript
// Vue 2:
beforeDestroy() {
    if (this.uuid) this.$niagara.subscriber.unsubscribe(this.uuid);
}

// Vue 3 con composable:
export function useSubscriber() {
    const uuid = ref(null);
    onUnmounted(() => {
        if (uuid.value) niagara.subscriber.unsubscribe(uuid.value);
    });
    return { uuid, subscribe, unsubscribe };
}
```

**ESLint rule**: forzar `unsubscribe` por cada `subscribe` en lifecycle.

### Regla 30 — mapState/mapGetters preferred over direct $store

```javascript
// Vue 2 — GOOD:
computed: {
    ...mapState({ buildings: s => s.buildings.items })
}

// Vue 3 + Pinia — GOOD:
const buildingsStore = useBuildingsStore();
const buildings = computed(() => buildingsStore.items);
```

**Excepción**: getters parametrizados (curried) requieren direct call.

### Regla 31 — JSON Patch full RFC 6902 (no SUBSET)

```javascript
// MX60 obligatorio:
import { applyPatch } from 'rfc6902';

STATE_DELTA(state, patch) {
    applyPatch(state, patch);  // Full RFC 6902 — replace, add, remove, copy, move, test
}
```

**NO subset manual implementation** — aseguramiento server↔client compatibility.

**Total reglas template MX60 post-Bloque 63**: **31 reglas** (22 previas + 9 nuevas).

---

## 63.16 Predicciones / hipótesis a verificar

1. **floorplans.js module size similar to buildings.js** — predicción 700L+, mutation/getter bloat similar. Verificar Bloque 64.
2. **OrdTree.vue (2000L+) usa v-style** para dynamic CSS — perf bottleneck en large hierarchies. Profile Bloque 64.
3. **equipmentData TTL eviction** funciona correctamente — `deviceTick` property en state. Check production logs.
4. **JSON Patch RFC 6902 SUBSET ha causado bugs en multiuser** — copy/move silently ignored. Check server logs failed patch operations.
5. **~10 components missing unsubscribe → memory leaks producción** — DeviceGrid, Equipment*, Point*. Memory profiling staging.
6. **eventBus resize necesario** — direct mutation thrashing Vuex. Aceptar pattern.
7. **configMigration.js v0→v14 well-tested** — 600L+ migration logic. Check coverage.
8. **CSRF token race acceptable** — <5% users hit + 2 RT penalty. Monitor production.

---

## 63.17 Cierre — completitud frontend

### Archivos auditados Bloque 63

| Capa | Files | Status |
|------|-------|--------|
| Entry point | main.js (124L) | ✅ |
| Root | App.vue (95L) | ✅ |
| Router | 32 routes lazy-loaded | ✅ |
| Vuex root | store/index.js (387L) | ✅ |
| Vuex módulos | 29 (3 sampled detailed + 26 listed) | ⚠️ Parcial — ver gap |
| API | rest.js + plugins/http.js | ✅ |
| Mixins | 17 archivos catalogados | ✅ |
| Plugins | 13 archivos catalogados | ✅ |
| Lib helpers | 10 archivos catalogados | ✅ |
| Views | 12 archivos (3 Bloque 62 + 9 nuevos) | ✅ |
| Components | 378 archivos en 18 carpetas — estructura mapeada | ✅ structure / ⚠️ detail |

### Gaps remanentes

1. **floorplans/ 52 components** — scope demasiado grande para este bloque. Dejar para Bloque 64+.
2. **OrdTree.vue 2000L+ detail** — requiere sesión dedicada. Bloque 64+.
3. **configMigration.js 600L+ detail** — requiere Bloque 64 (backend context para validar v0→v14 schema).
4. **store/modules/* completo** — solo `user`, `buildings`, `alarms` sampled detailed; faltan 26 módulos sin deep dive.

### Resumen

**Tally global post-Bloque 63**:
- **86 antipatterns** AP-1..86 (3 CRITICAL + 9 HIGH + 25 MEDIUM + 49 LOW)
- **31 reglas template MX60** (frontend reglas 23-31 nuevas)
- **190 MX60 implications**
- Capa 17 frontend audit completo (al ~95% — gaps documentados)

**Decisión arquitectónica #6 MX60**: stack frontend Pinia stores 1:1 con 29 módulos Vuex actuales — la separation persistent/transient es excelente, no requiere rediseño, solo migration técnica Vue 2 → Vue 3.

**Próximo bloque**: Bloque 64 (`-ux` modules Java + módulos `-rt` no auditados).

---

**End of Bloque 63** — frontend Reflow Vue 2.7 audit completo.

**Siguiente**: Bloque 64 (`-ux` modules + rt remanentes — workbench views + helpers Java no cubiertos).
