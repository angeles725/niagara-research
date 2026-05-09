# Frontend Domain — reflow-frontend (Vue 2.7 SPA — overview + cross-cutting)

**Coverage**: app-shell + store + plugins + mixins + lib + api + router + module-config + 19 small UI domains
**Files**: ~285 frontend entries (equipment/floorplans/alarms/history/buildings/config tienen docs dedicados)
**Source**: `reflow-frontend/src/` servido por `nmodsreflow-rt` JAR en `/nmodsreflow/`

---

## 1. Overview

Reflow es una SPA Vue 2.7 que se sirve desde el módulo Niagara `nmodsreflow-rt`. El bootstrap carga ViewUI (iView), 13 plugins propios/terceros, y monta la app en `#nmods-app`. El flujo inicial es: `csrfBootstrap()` fire-and-forget → Vue Router (hash mode, 37 rutas) → Vuex store (29 módulos) → `store.dispatch('load')` que hace GET `/nmodsreflow/config`, corre migraciones y comete `LOAD_STATE`. En producción, el backend Niagara también envía config vía WebSocket, disparando `LOAD_STATE` nuevamente. El canal BOX (BajaScript) permite suscribirse a puntos Niagara en tiempo real a través de `$baja` y `$niagara`.

La arquitectura es configurable: ningún dato de edificios/pisos/equipment está hardcodeado. Todo se interpreta desde la config JSON que el técnico arma en Workbench. El resultado es una app que renderiza de forma dinámica dashboards, mapas de pisos, alarmas, historiales y páginas personalizadas según esa config. El flujo de autenticación CSRF usa 403→refresh→retry manejado en `plugins/http.js`, con `lib/csrf.js` ejecutándose al arranque.

---

## 2. Entry Points

| Archivo | Descripción |
|---------|-------------|
| `reflow-frontend/src/main.js` | Bootstrap: Vue + ViewUI + 13 plugins + directivas + `csrfBootstrap()` |
| `reflow-frontend/src/App.vue` | Root component — 118 LOC, layout shell + socket modals |
| `reflow-frontend/src/router/index.js` | Vue Router hash mode — 37 rutas (21 top-level + 16 building-scoped) |
| `reflow-frontend/src/store/index.js` | Vuex root — 29 módulos + LOAD_STATE/STATE_DELTA/REPLACE_STATE |
| `reflow-frontend/src/plugins/baja.js` | `$baja` — bridge al runtime BajaScript (debe instalarse antes que niagara) |
| `reflow-frontend/src/plugins/niagara.js` | `$niagara` — alarm/subscriber/bql/history/nav/user helpers (269 LOC) |
| `reflow-frontend/src/api/rest.js` | Axios wrapper — 353 LOC, endpoints REST hacia `/nmodsreflow/` |
| `reflow-frontend/src/api/websocket.js` | WebSocket client — 155 LOC (comentado en main.js, stub Phase 5+) |
| `reflow-frontend/src/lib/csrf.js` | CSRF token bootstrap + 403-retry — 142 LOC |
| `reflow-frontend/src/lib/eventBus.js` | Vue instance global event bus — 5 LOC |

---

## 3. Components / Modules

### 3.1 App Shell (4 entradas)

| Archivo | Kind | LOC | Descripción |
|---------|------|-----|-------------|
| `src/main.js` | js-module | 123 | Bootstrap principal |
| `src/App.vue` | vue-component | 118 | Root component — socket modals, theme, layout |
| `src/components/config/menuTree.js` | js-module | 169 | Árbol de menú config (usa `$configMode`) |
| `src/components/weather/weatherIcons.js` | js-module | 251 | Mapa de íconos weather → FontAwesome |

### 3.2 Router (1 archivo — 37 rutas)

| Grupo | Rutas | Componente(s) |
|-------|-------|---------------|
| Top-level | `/`, `/alarms`, `/equipment`, `/schedules`, `/buildings`, `/histories`, `/pages/:id`, `/embed/:ord`, `/floors` | Home, AlarmsHome, EquipmentIndex, SchedulesHome, BuildingIndex, HistoriesHome, PageView, EmbedView, FloorsRedirectView |
| Alarms detail | `/alarms/console/:id`, `/alarms/console/:id/:type` | AlarmsHome, AlarmDetails |
| Equipment detail | `/equipment/device/:id`, `/equipment/group/:id`, `/equipment/type/:id`, `/equipment/:filter/:id` | DeviceDetailsView, EquipmentGroupView, EquipmentIndex |
| Schedules | `/schedules/group/:groupId`, `/schedules/:handle` | SchedulesHome |
| Histories | `/histories/view` | HistoryBuilder |
| Building-scoped (16) | `/buildings/:buildingId/...` — alarms, equipment, schedules, histories, floors, pages, wizard | BuildingLayout + mirrors de top-level |
| Debug | `/debug/color` | DebugColorView |

GAP: GAP-ANALYSIS reporta 10 StubView + 1 ruta genuinamente faltante. Fidelity: FAIR (10 stubs).

### 3.3 Store (30 entradas — root + 29 módulos)

| Módulo | LOC | Persistent | Fidelity | Notas |
|--------|-----|-----------|---------|-------|
| `index.js` (root) | 386 | — | GOOD | LOAD_STATE / STATE_DELTA / REPLACE_STATE + 12 root actions |
| `theme` | 69 | ✅ | PERFECT | |
| `colors` | 267 | ✅ | GOOD | 13 TinyColor getters — fix S55 |
| `alarms` | 238 | ✅ | GOOD | `ignoredAlarmClasses` — fix S55 |
| `buildings` | 783 | ✅ | FAIR | 7 auth getters faltantes |
| `equipment` | 1130 | ✅ | POOR | `getPoints` stub, `getItemById` O(n) vs O(1), Proxy missing |
| `floorplans` | 268 | ✅ | GOOD | `REMOVE_FLOOR` cross-commit pattern |
| `histories` | 139 | ✅ | EXCELLENT | |
| `landing` | 61 | ✅ | EXCELLENT | |
| `navigation` | 365 | ✅ | GOOD | params swapped en `pageHasSubNavigation` |
| `pages` | 164 | ✅ | GOOD | |
| `dashboardCards` | 169 | ✅ | GOOD | |
| `schedules` | 294 | ✅ | GOOD | fix S55 |
| `weather` | 101 | ✅ | EXCELLENT | |
| `profiles` | 1026 | ✅ | GOOD | rewrite S57 — `authorizeLink` 62L, `getRouteTreeData` 189L |
| `alarmData` | 155 | — | GOOD | fix S54 — stripped to bundle |
| `equipmentData` | 27 | — | GOOD | fix S53 |
| `scheduleData` | 253 | — | GOOD | fix S56 — `bqlService` + `scheduleListService` |
| `pointMapData` | 592 | — | GOOD | |
| `floorEditor` | 1327 | — | FAIR→GOOD | fix S55 partial — 23 getters still missing |
| `weatherData` | 228 | — | GOOD | fix S55 |
| `documentData` | 78 | — | POOR | nombres de props incorrectos, missing `isCompact`/`villainMode` |
| `mouseData` | 96 | — | FAIR | single-value vs array-based |
| `historyCache` | 516 | — | GOOD | fix S56 — full rewrite con `historyService` |
| `menu` | 19 | — | GOOD | fix S53 |
| `notify` | 58 | — | POOR | arquitectura diferente — bundle usa toast `$emit`, recon usa queue |
| `license` | 209 | — | GOOD | fix S56 — 14 state props, 13 getters |
| `updates` | 62 | — | GOOD | fix S53 |
| `user` | 100 | — | GOOD | fix S55 — clipboard props + WS actions |
| `demo` | 77 | — | GOOD | fix S53 |

### 3.4 Plugins (13)

| Archivo | LOC | Export / instala |
|---------|-----|-----------------|
| `baja.js` | 48 | `$baja` — BOX/BajaScript bridge |
| `niagara.js` | 269 | `$niagara` — alarm/subscriber/bql/history/nav/user |
| `utils.js` | 69 | `$utils`, `$maps` |
| `timePlugin.js` | 89 | `$time` — ago/display/fromUtc/toMoment |
| `ord.js` | 42 | `$ord` — image(ordString) |
| `reflowLink.js` | 99 | `$reflowLink`, `$reflowLinkTitle`, `$reflowResolveRoute` |
| `gbo.js` | 72 | `$gbo` — str.pluralize/titleCase |
| `workbench.js` | 25 | `$workbench`, `$hasWidget`, `$documentElement` |
| `cookies.js` | 8 | `$cookies` — get/set/remove |
| `http.js` | 79 | `$http` — axios con 403→CSRF→retry |
| `colorUtils.js` | 163 | `$colorUtils` — TinyColor helpers |
| `configMode.js` | 11 | `$configMode` — flag para modo configuración |
| `labelForItem.js` | 36 | `$labelForItem` — label de items Niagara |

Fidelity: GOOD (count match) — GAP-ANALYSIS S47.

### 3.5 Mixins (18)

| Archivo | LOC | Grupo / propósito |
|---------|-----|------------------|
| `subscriberMixin.js` | 131 | Niagara — BajaScript subscribe lifecycle |
| `stateBaseMixin.js` | 79 | Niagara — state polling base |
| `elementMixin.js` | 374 | Dashboard — element drag/resize base |
| `canvasDragResizeMixin.js` | 237 | Dashboard — canvas drag/resize (store shape issue) |
| `dynamicColorMixin.js` | 296 | Colors — computed color bindings |
| `equipmentListMixin.js` | 155 | Equipment — list filtering/sorting |
| `equipmentMixin.js` | 159 | Equipment — item detail helpers |
| `historyListMixin.js` | 361 | History — list + pagination |
| `navItemMixin.js` | 104 | Navigation — nav item base |
| `navigationMixin.js` | 198 | Navigation — routing + active state |
| `checkedItemsMixin.js` | 48 | List — multi-select checkbox |
| `clipboardMixin.js` | 121 | List — clipboard copy via `$copyText` |
| `editorPaneMixin.js` | 91 | Config editor — panel open/close |
| `propertiesMixin.js` | 181 | Config editor — property form base |
| `summaryViewMixin.js` | 92 | Views — summary/detail toggle |
| `tweenMixin.js` | 61 | Animation — number tween |
| `buildingConfigMixin.js` | 43 | Buildings — config form helpers |
| `profileMixin.js` (profiles/) | 56 | Profiles — profile form helpers |

Fidelity: GOOD (84%) — GAP-ANALYSIS S49. `canvasDragResizeMixin` store shape mismatch.

### 3.6 Lib (10)

| Archivo | LOC | Rol |
|---------|-----|-----|
| `configMigration.js` | 653 | Migraciones de versión del estado Vuex |
| `configSerializer.js` | 26 | Serialización selectiva (excluye runtime keys) |
| `csrf.js` | 142 | Bootstrap CSRF — fire-and-forget + retry |
| `bajaHeartbeat.js` | 151 | Heartbeat BajaScript — keepalive |
| `alarmCache.js` | 61 | Cache de alarmas activas |
| `ord.js` | 90 | Helpers de ORD strings (parse/format) |
| `deepMerge.js` | 21 | Deep merge para LOAD_STATE |
| `utils.js` | 22 | Utilidades generales |
| `uuid.js` | 24 | Generador de UUIDs |
| `eventBus.js` | 5 | Vue instance global event bus |

### 3.7 API Layer (6)

| Archivo | LOC | Rol |
|---------|-----|-----|
| `rest.js` | 353 | REST — axios, todos los endpoints `/nmodsreflow/` |
| `box.js` | 331 | BOX — BajaScript RPC protocol (commands) |
| `websocket.js` | 155 | WebSocket — client stub (comentado en main.js — Phase 5+) |
| `bajascript.js` | 72 | BajaScript — subscriber helper sobre BOX |
| `external.js` | 63 | External — Aeris Weather API, Mapbox token |
| `index.js` | 11 | Re-exports centralizados |

### 3.8 Module Config (11)

| Archivo | LOC | Tipo | Descripción |
|---------|-----|------|-------------|
| `build.gradle.kts` | 45 | gradle | Root build — Vite + JAR assembly |
| `settings.gradle.kts` | 122 | gradle | Multi-project settings |
| `nmodsreflow-rt/nmodsreflow-rt.gradle.kts` | 97 | gradle | RT module build — deps + rc/ bundle |
| `nmodsreflow-ux/nmodsreflow-ux.gradle.kts` | 44 | gradle | UX module build — BajaScript widgets |
| `nmodsreflow/niagara-module.xml` | 2 | xml | Module descriptor raíz |
| `nmodsreflow-rt/module-include.xml` | 47 | xml | RT: BReflowService registrations |
| `nmodsreflow-rt/module-permissions.xml` | 10 | xml | RT: permisos HTTP |
| `nmodsreflow-rt/module.palette` | 6 | palette | RT: palette de componentes Workbench |
| `nmodsreflow-ux/module-include.xml` | 17 | xml | UX: BReflow/BReflowConfig/BReflowRedirect |
| `nmodsreflow-ux/module-permissions.xml` | 5 | xml | UX: permisos |
| `nmodsreflow-ux/module.palette` | 5 | palette | UX: palette widgets |

### 3.9 Small UI Domains

#### cards (18)

| Archivo | LOC | Fidelity | Propósito |
|---------|-----|---------|-----------|
| `BaseCard.vue` | 125 | EXCELLENT | Base container para todas las cards |
| `BaseFlipCard.vue` | 323 | EXCELLENT | Card con flip animation |
| `CardList.vue` | 397 | EXCELLENT | Lista de cards masonry |
| `CircleCard.vue` | 284 | EXCELLENT | Gauge circular con `limitColoring` |
| `DividerCard.vue` | 72 | EXCELLENT | Separador visual |
| `FeaturedCard.vue` | 72 | EXCELLENT | Hero card destacada |
| `Gauge.vue` | 328 | EXCELLENT | Gauge SVG |
| `GroupCard.vue` | 239 | EXCELLENT | Card de grupo |
| `GroupCardForm.vue` | 52 | EXCELLENT | Form inline de grupo |
| `HX.vue` | 112 | EXCELLENT | Horizontal expandable card |
| `Hyperlink.vue` | 270 | EXCELLENT | Card link externo |
| `NoteCard.vue` | 267 | EXCELLENT | Card de nota equipamiento |
| `NoteGrid.vue` | 139 | EXCELLENT | Grid de notas |
| `TableCard.vue` | 325 | EXCELLENT | Card tabla dinámica |
| `ToggleCard.vue` | 376 | EXCELLENT | Card con toggle de punto |
| `URLCard.vue` | 79 | EXCELLENT | Card embed URL |
| `table/Cell.vue` | 110 | EXCELLENT | Celda de tabla |
| `table/MobileRowCard.vue` | 88 | EXCELLENT | Row mobile |

Domain fidelity: **EXCELLENT (92%)** — GAP-ANALYSIS S49.

#### charts (11)

| Archivo | LOC | Fidelity | Propósito |
|---------|-----|---------|-----------|
| `D3chart.vue` | 3114 | GOOD | Motor D3 principal — registrado global en main.js |
| `Chart.vue` | 469 | GOOD | Wrapper chart con config |
| `ChartToolBar.vue` | 356 | GOOD | Toolbar zoom/export |
| `ChartExportPicker.vue` | 102 | GOOD | Picker formato export |
| `ChartTypePicker.vue` | 134 | GOOD | Picker tipo chart |
| `ContextMenu.vue` | 127 | GOOD | Context menu canvas |
| `GraphicReflow.vue` | 534 | GOOD | Reflow graphic overlay |
| `GraphicSelect.vue` | 306 | GOOD | Selector de gráficos |
| `Sparkline.vue` | 431 | GOOD | Sparkline inline |
| `TimeRangePicker.vue` | 190 | GOOD | Selector rango temporal |
| `DeltaSymbol.vue` | 41 | GOOD | Símbolo delta comparación |

Domain fidelity: **GOOD (87%)** — GAP-ANALYSIS S49. Issue: D3chart era local → registrado global en main.js.

#### common (26)

| Archivo | LOC | Fidelity | Propósito |
|---------|-----|---------|-----------|
| `OrdTree.vue` | 3242 | FAIR (10%) | Árbol ORD Niagara — **stub crítico** |
| `OrdTreeItem.vue` | 926 | FAIR | Item del árbol ORD |
| `OrdEmbed.vue` | 264 | FAIR | Embed de vista ORD |
| `BoundLabel.vue` | 432 | GOOD | Label con binding a punto Niagara |
| `ActiveColorPicker.vue` | 106 | FAIR | Color picker con estado activo |
| `LockedColorPicker.vue` | 145 | GOOD | Color picker bloqueado |
| `PreferredColorPicker.vue` | 236 | GOOD | Color picker con preferencia |
| `ImageSelect.vue` | 353 | GOOD | Selector de imagen |
| `IconBrowser.vue` | 389 | GOOD | Browser de íconos FA |
| `IconPicker.vue` | 144 | GOOD | Picker de ícono |
| `IconMarker.vue` | 79 | GOOD | Marcador con ícono |
| `IconTip.vue` | 27 | GOOD | Tooltip con ícono |
| `ActionForm.vue` | 205 | GOOD | Form de acción genérico |
| `URLEmbed.vue` | 105 | GOOD | Embed de URL |
| `URLInput.vue` | 54 | GOOD | Input URL |
| `ImageSelect.vue` | 353 | GOOD | Selector imagen |
| `LinkPicker.vue` | 37 | GOOD | Picker de link |
| `LoadingScreen.vue` | 102 | GOOD | Pantalla de carga |
| `LocationInput.vue` | 106 | GOOD | Input de localización |
| `TrialBanner.vue` | 83 | GOOD | Banner modo trial |
| `TrialModal.vue` | 67 | GOOD | Modal modo trial |
| `Underline.vue` | 29 | GOOD | Subrayado decorativo |
| `WhoDot.vue` | 116 | GOOD | Indicador de usuario activo |
| `WhoList.vue` | 103 | GOOD | Lista de usuarios conectados |
| `WhoUser.vue` | 93 | GOOD | Info de usuario individual |
| `DraggableTracker.vue` | 14 | GOOD | Tracker drag state |
| `ExpandTransition.vue` | 38 | GOOD | Transición expand — global en main.js |

Domain fidelity: **FAIR (62%)** — OrdTree es stub (10%). Bloquea image browsing, ORD linking, PX views.

#### navigation (16)

| Archivo | LOC | Fidelity | Propósito |
|---------|-----|---------|-----------|
| `Navigation.vue` | 235 | GOOD | Shell de navegación principal |
| `NavigationList.vue` | 355 | GOOD | Lista de ítems nav |
| `NavigationForm.vue` | 280 | GOOD | Form de configuración nav |
| `NavigationDropdownList.vue` | 178 | GOOD | Lista dropdown |
| `NavigationMobile.vue` | 187 | GOOD | Nav mobile |
| `NavigationStyles.vue` | 35 | GOOD | Estilos dinámicos nav |
| `NavDropdown.vue` | 396 | GOOD | Dropdown genérico |
| `NavDropdownBuilding.vue` | 396 | GOOD | Dropdown building-scoped |
| `NavDivider.vue` | 48 | GOOD | Divisor nav |
| `NavLabel.vue` | 95 | GOOD | Label nav |
| `NavLink.vue` | 28 | GOOD | Link nav |
| `SubnavColors.vue` | 119 | GOOD | Subnav con colores |
| `SubnavForm.vue` | 169 | GOOD | Form subnav |
| `SubnavList.vue` | 142 | GOOD | Lista subnav |
| `LinkHome.vue` | 31 | GOOD | Link home |
| `LinkLogout.vue` | 27 | GOOD | Link logout |

Domain fidelity: **GOOD** — GAP-ANALYSIS S47.

#### layout (5)

| Archivo | LOC | Fidelity | Propósito |
|---------|-----|---------|-----------|
| `Layout.vue` | 328 | EXCELLENT | Shell principal con sidebar + content |
| `Header.vue` | 184 | EXCELLENT | Header + nav |
| `Content.vue` | 30 | EXCELLENT | Área de contenido |
| `Footer.vue` | 59 | EXCELLENT | Footer |
| `Styles.vue` | 44 | EXCELLENT | Estilos dinámicos computados |

Domain fidelity: **EXCELLENT** — GAP-ANALYSIS S47.

#### dashboard (14)

| Archivo | LOC | Fidelity | Propósito |
|---------|-----|---------|-----------|
| `DashboardLayout.vue` | 343 | GOOD | Layout masonry de cards |
| `DashboardCard.vue` | 195 | GOOD | Wrapper de card en dashboard |
| `DynamicColorForm.vue` | 548 | GOOD | Form de reglas de color dinámico |
| `RowForm.vue` | 989 | GOOD | Form de fila de tabla |
| `DashboardPointListPoint.vue` | 161 | GOOD | Punto en lista de puntos |
| `DashboardTableColumnList.vue` | 134 | GOOD | Columnas de tabla dashboard |
| `DashboardTableRowList.vue` | 112 | GOOD | Filas de tabla dashboard |
| `HomeHero.vue` | 80 | GOOD | Hero landing |
| `HeroContent.vue` | 79 | GOOD | Contenido del hero |
| `LandingPage.vue` | 71 | GOOD | Página landing |
| `LandingBuildings.vue` | 59 | GOOD | Listado edificios en landing |
| `LandingCardList.vue` | 62 | GOOD | Lista de cards en landing |
| `LandingEquipment.vue` | 105 | GOOD | Equipamiento en landing |
| `LandingHistories.vue` | 137 | GOOD | Historiales en landing |

Domain fidelity: **GOOD** — GAP-ANALYSIS S47.

#### pages (11)

| Archivo | LOC | Fidelity | Propósito |
|---------|-----|---------|-----------|
| `PageList.vue` | 115 | GOOD | Lista de páginas — falta `vue-draggable` |
| `PageControl.vue` | 70 | GOOD | Controles de página |
| `PageForm.vue` | 380 | GOOD | Form de configuración de página |
| `PageFormLayout.vue` | 242 | GOOD | Layout del form |
| `PageGroupForm.vue` | 74 | GOOD | Form de grupo |
| `PageListConfigItem.vue` | 177 | GOOD | Item config |
| `PageListItem.vue` | 88 | GOOD | Item lista |
| `PageNiagara.vue` | 95 | GOOD | Página Niagara embed |
| `PageProfiles.vue` | 139 | GOOD | Perfiles en página |
| `PageWeb.vue` | 85 | GOOD | Página web embed |
| `AssociatedPages.vue` | 101 | GOOD | Páginas asociadas |

Domain fidelity: **GOOD (86%)** — GAP-ANALYSIS S49. Missing `vue-draggable` en PageList.

#### points (12)

| Archivo | LOC | Fidelity | Propósito |
|---------|-----|---------|-----------|
| `PointCell.vue` | 1107 | GOOD | Celda de punto — control principal |
| `PointCard.vue` | 816 | GOOD | Card de punto |
| `PointMap.vue` | 899 | GOOD | Mapa de puntos CSV |
| `GroupCell.vue` | 792 | GOOD | Celda de grupo de puntos |
| `NiagaraPoint.vue` | 457 | GOOD | Punto Niagara — binding BOX |
| `PointList.vue` | 263 | GOOD | Lista de puntos |
| `ClassicPointGroup.vue` | 201 | GOOD | Grupo clásico |
| `PointEdit.vue` | 167 | GOOD | Edit inline de punto |
| `PointInfoBadge.vue` | 160 | GOOD | Badge info de punto |
| `PointPicker.vue` | 162 | GOOD | Picker de punto |
| `PointGroup.vue` | 99 | GOOD | Grupo de puntos |
| `PointDelete.vue` | 73 | GOOD | Delete confirmación |

Domain fidelity: **GOOD (88%)** — GAP-ANALYSIS S49.

#### profiles (12)

| Archivo | LOC | Fidelity | Propósito |
|---------|-----|---------|-----------|
| `UserProfile.vue` | 194 | GOOD | Perfil de usuario |
| `UserProfileAccess.vue` | 147 | GOOD | Control de acceso |
| `UserProfileList.vue` | 170 | GOOD | Lista de perfiles |
| `UserProfileNav.vue` | 115 | GOOD | Nav de perfil |
| `UserProfileRoles.vue` | 104 | GOOD | Roles de usuario |
| `UserProfileStartPage.vue` | 81 | GOOD | Página de inicio de perfil |
| `UserProfileUsers.vue` | 213 | GOOD | Usuarios del perfil |
| `ConfigViewUsers.vue` | 217 | GOOD | Vista usuarios en config |
| `PageProfiles.vue` | 86 | GOOD | Profiles en página |
| `ProfileBanner.vue` | 71 | GOOD | Banner de perfil |
| `UserProfiles.vue` | 18 | GOOD | Shell de profiles |
| `UserProfilesControl.vue` | 19 | GOOD | Control de profiles |

Domain fidelity: **GOOD (88%)** — GAP-ANALYSIS S49.

#### schedules (4)

| Archivo | LOC | Fidelity | Propósito |
|---------|-----|---------|-----------|
| `ScheduleList.vue` | 147 | EXCELLENT | Lista de schedules |
| `ScheduleListItem.vue` | 115 | EXCELLENT | Item de schedule |
| `ScheduleListConfigItem.vue` | 348 | EXCELLENT | Config item de schedule |
| `ScheduleGroupForm.vue` | 151 | EXCELLENT | Form de grupo |

Domain fidelity: **EXCELLENT** — GAP-ANALYSIS S47.

#### settings (13)

| Archivo | LOC | Fidelity | Propósito |
|---------|-----|---------|-----------|
| `SettingsBackups.vue` | 421 | GOOD | Gestión de backups |
| `OptimizeConfig.vue` | 149 | GOOD | Optimización de config |
| `ColorPickerSettings.vue` | 81 | GOOD | Settings de color picker |
| `GlobalSettings.vue` | 71 | GOOD | Settings globales |
| `SettingsAutomatedBackups.vue` | 77 | GOOD | Backups automáticos |
| `SoftwareUpdates.vue` | 88 | GOOD | Actualizaciones de software |
| `SettingsWebCache.vue` | 74 | GOOD | Web cache settings |
| `SettingsRedirectReflowView.vue` | 53 | GOOD | Redirect config |
| `RestrictConfig.vue` | 20 | GOOD | Restricción de config |
| `ResetConfigControl.vue` | 19 | GOOD | Reset de config |
| `OpenWelcomeWizard.vue` | 13 | GOOD | Abre el wizard |
| `LogoSettings.vue` | 14 | GOOD | Settings de logo |
| `BackgroundSettings.vue` | 14 | GOOD | Settings de background |

Domain fidelity: **GOOD (87%)** — GAP-ANALYSIS S49. Issue: `baja.getClientId()` header source.

#### weather (6)

| Archivo | LOC | Fidelity | Propósito |
|---------|-----|---------|-----------|
| `Weather.vue` | 620 | EXCELLENT | Widget principal weather |
| `WeatherDisplay.vue` | 234 | EXCELLENT | Display de datos weather |
| `WeatherConfig.vue` | 169 | EXCELLENT | Config del widget |
| `WeatherMap.vue` | 115 | EXCELLENT | Mapa weather (Aeris) |
| `AerisWeather.vue` | 150 | EXCELLENT | Integración Aeris API |
| `WeatherSettings.vue` | 15 | EXCELLENT | Settings simples |

Domain fidelity: **EXCELLENT (94%)** — GAP-ANALYSIS S49. Fix: `v-responsive` directive.

#### websocket — Vue components (4)

| Archivo | LOC | Fidelity | Propósito |
|---------|-----|---------|-----------|
| `SocketAuth.vue` | 173 | EXCELLENT | Modal autenticación multi-user |
| `SocketConnect.vue` | 139 | EXCELLENT | Modal conexión |
| `SocketRequest.vue` | 237 | EXCELLENT | Modal pedido de control |
| `SocketResponseError.vue` | 33 | EXCELLENT | Modal error de respuesta |

Domain fidelity: **EXCELLENT (91%)** — GAP-ANALYSIS S49. (Los 6 java-class de websocket están en backend.md)

#### wizard (8)

| Archivo | LOC | Fidelity | Propósito |
|---------|-----|---------|-----------|
| `WelcomeWizard.vue` | 754 | GOOD | Wizard de bienvenida — multi-step |
| `StepWeather.vue` | 231 | GOOD | Paso: configurar weather |
| `StepBuilding.vue` | 88 | GOOD | Paso: nombre de edificio |
| `StepColors.vue` | 87 | GOOD | Paso: paleta de colores |
| `StepLogoHero.vue` | 88 | GOOD | Paso: logo y hero |
| `StepTitle.vue` | 52 | GOOD | Paso: título |
| `StepFinish.vue` | 97 | GOOD | Paso: finalizar |
| `WizardTitle.vue` | 41 | GOOD | Header del wizard |

Domain fidelity: **GOOD (84%)** — GAP-ANALYSIS S49. Issue: mixed ES5/ES6, `browserDismissed` simplificado.

#### browser (1)

| Archivo | LOC | Fidelity | Propósito |
|---------|-----|---------|-----------|
| `ImageBrowser.vue` | 343 | GOOD | Browser de imágenes subidas |

#### map (1) / maps (7)

| Archivo | LOC | Fidelity | Propósito |
|---------|-----|---------|-----------|
| `map/Map.vue` | 38 | FAIR | Shell del mapa Mapbox — stub |
| `maps/MglMap.vue` | 43 | FAIR | Mapbox GL map — dev stub |
| `maps/MglMarker.vue` | 21 | FAIR | Marcador Mapbox |
| `maps/MglPopup.vue` | 22 | FAIR | Popup Mapbox |
| `maps/MglNavigationControl.vue` | 17 | FAIR | Control navegación |
| `maps/MglAttributionControl.vue` | 16 | FAIR | Control atribución |
| `maps/MglGeocoderControl.vue` | 21 | FAIR | Control geocoder |
| `maps/MglRasterLayer.vue` | 18 | FAIR | Capa raster |

Domain fidelity: **FAIR (65%)** — GAP-ANALYSIS S49. Todos son stubs de dev — requieren `npm install v-mapbox mapbox-gl`.

#### views (12)

| Archivo | LOC | Fidelity | Propósito |
|---------|-----|---------|-----------|
| `AlarmsHome.vue` | 636 | FAIR | Vista principal alarmas |
| `AlarmDetails.vue` | 559 | FAIR | Detalle de alarma |
| `SchedulesHome.vue` | 405 | EXCELLENT | Vista schedules |
| `EquipmentGroupView.vue` | 346 | FAIR | Vista grupo equipamiento |
| `DeviceDetailsView.vue` | 350 | FAIR | Detalle de dispositivo |
| `PageView.vue` | 216 | GOOD | Vista de página configurable |
| `BuildingFloorsView.vue` | 122 | GOOD | Vista pisos de edificio |
| `EmbedView.vue` | 110 | GOOD | Vista embed ORD |
| `Home.vue` | 108 | GOOD | Vista home / landing |
| `BuildingFloorDetailView.vue` | 52 | GOOD | Detalle de piso |
| `FloorsRedirectView.vue` | 26 | GOOD | Redirect de floors legacy |
| `DebugColorView.vue` | 28 | GOOD | Debug de colores |

---

## 4. Cross-references

- **Frontend → Backend REST**: `api/rest.js` + `$http` → `/nmodsreflow/config`, `/nmodsreflow/config_update`, `/nmodsreflow/config_delta`, `/nmodsreflow/weather`, `/nmodsreflow/alarms`, `/nmodsreflow/histories`, `/nmodsreflow/schedules`, `/nmodsreflow/backups`, `/nmodsreflow/files`
- **Frontend → Backend BOX**: `api/box.js` + `$baja` + `$niagara` → BReflowAlarmCommands, BReflowHistoryCommands, BReflowNavCommands, BReflowBQLCommands, BReflowCSVCommands, BReflowFileCommands, BReflowLicenseCommands, BReflowUserCommands
- **Frontend → Backend WebSocket**: `api/websocket.js` → BReflowWebSocketAcceptor → BReflowChannelService (config-refresh, control-request, config-reload, delta-sync)
- **Store interactions**: `equipment.js` (POOR — `getPoints` stub bloquea NiagaraPoint.vue); `buildings.js` (FAIR — 7 auth getters bloquean profiles + navigation); `documentData.js` (POOR — `villainMode`/`isCompact` afectan layout responsivo)
- **Plugin coupling**: `niagara.js` lee `$baja` (debe instalarse segundo); `colorUtils.js` recibe el store como segundo arg de Vue.use(); `reflowLink.js` consume el router para `$reflowResolveRoute`
- **Mixin reuse**: `subscriberMixin` usado en NiagaraPoint + PointCell + BoundLabel; `dynamicColorMixin` usado en DashboardCard + CardList + navigation; `equipmentMixin` + `equipmentListMixin` compartidos entre vistas de equipment y dashboard
- **Build → Deploy**: Vite → `dist/` con `base: '/nmodsreflow/'` → copiado a `nmodsreflow-rt/rc/` → empaquetado en JAR por gradle → deploy al módulo Niagara

---

## 5. Notes & Gotchas

- **WebSocket stub**: `api/websocket.js` está comentado en `main.js` (Phase 5+). En producción, el backend envía config vía WS → `LOAD_STATE`. Sin este canal, la app solo carga config por REST y no recibe deltas en tiempo real.
- **OrdTree crítico**: `common/OrdTree.vue` + `OrdTreeItem.vue` son stubs (fidelity 10%). Bloquean toda la navegación de árbol Niagara: image browsing, ORD linking, PX views, embed views.
- **Mapbox stubs**: `map/` y `maps/` son todos stubs de dev — requieren `npm install v-mapbox mapbox-gl` + token Mapbox. Sin esto, el feature de weather maps (mapa geográfico de estaciones) no funciona.
- **CSRF bootstrap**: `csrfBootstrap()` en main.js es fire-and-forget. El primer POST puede recibir 403, que `plugins/http.js` maneja con refresh→retry automático. No hay race condition visible, pero es un gotcha en entornos con CORS activo.
- **Store POOR/FAIR**: `equipment` (POOR) afecta `getPoints` stub — NiagaraPoint.vue no puede obtener el listado real. `notify` (POOR) usa arquitectura diferente al bundle. `documentData` (POOR) — `villainMode`/`isCompact`/`configContentStyles` missing afectan layout responsivo. `buildings` (FAIR) — 7 auth getters faltantes bloquean restricciones de perfil.
- **GAP-ANALYSIS fidelity flags**: Overall 82% GOOD. Gaps concentrados en: 5 POOR stores, ~70 sub-componentes missing (floor plan editor panes, config editor sub-forms, Niagara integration stubs), 10 P0 bugs verificados (ver VERIFICATION-REPORT.md).
- **Router**: 37 rutas en fuente vs 36 recon (1 missing genuina) + 10 StubView. `building-scoped` rutas duplican toda la jerarquía bajo `/buildings/:buildingId/`.
- **src/rc/ excluido**: el directorio `nmodsreflow-rt/rc/` contiene el bundle Vite compilado — excluido del mapeo (ver `excluded.md`). No editar directamente.
- **v-responsive directiva**: registrada en `main.js` pero faltaba en varios componentes que la usan (`Weather.vue`, algunos points). Fix documentado en GAP-ANALYSIS S49.
- **Módulo demo**: `store/modules/demo.js` (fix S53) — solo existe para desarrollo local con datos mock. No se envía al backend de producción.
