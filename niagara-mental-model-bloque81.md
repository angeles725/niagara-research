# Bloque 81 — Familia `easy*` Honeywell/Galileo: herramientas de productividad (Templating PX, Binding masivo, Healthy Building, Database cleanup) deofuscadas

> Investigación empírica de los cuatro módulos **`easy*`** de la suite OEM Honeywell/Galileo (mismo linaje que [Bloque 80]). No son drivers ni protocolos: son **herramientas de productividad de ingeniería** que se montan sobre el framework Niagara para acelerar tareas repetitivas de comisionamiento, visualización y mantenimiento de la station.
>
> 4 módulos: `easyTemplating` (plantillas de widgets PX reutilizables), `easyBinding` (binding masivo punto→imagen con paletas generadas), `easyHealthyBuilding` (dashboard IAQ con índice de salud por zona), `easyDatabaseManager` (auditoría y limpieza de la station database).
> Strings descifradas (ZKM); nombres internos `a`/`b`/`c` y rutas JS (XOR en `static{}`) aún ofuscados.
>
> Fuentes: `organized/easy{Templating,Binding,HealthyBuilding,DatabaseManager}/<m>-{rt,wb,ux}/vineflower/com/honeywell/...`
> Método: 4 sub-agentes en paralelo + **verificación directa** de cada declaración `extends` clave con `grep ^public class` + lectura de la línea de continuación. `[CERT]` = `extends`/línea verificada verbatim por mí; `[CERT-a]` = cita del sub-agente (slots, algoritmos, strings) no re-verificada; `[INFER]` = deducción.
>
> Capa 22 (OEM deofuscados), continúa [Bloque 80]. Conecta con [Bloque 4] (status bits), [Bloque 9] (UI/Servlets/BajaScript), [Bloque 36] (PX lifecycle) y [Bloques 47-51] (consumir Niagara desde frontend).

---

## 81.1 — La familia `easy*` en conjunto: ADN común `[CERT]`

Las cuatro raíces verificadas (`grep ^public class` + línea `extends`):

| Módulo | Clase raíz (verificada) | Rol |
|--------|------------------------|-----|
| easyTemplating | `BEasyTemplatingService extends BComponent implements BIService` (:30) | servicio de plantillas PX |
| easyBinding | `BEasyBindingSupportService extends BAbstractService` (:33) | servicio de soporte + jobs batch |
| easyHealthyBuilding | `BEasyHealthyBuildingService extends BAbstractService` (:79) | servicio de dashboard IAQ |
| easyDatabaseManager | `BEasyDatabaseManagerService extends BComponent implements BIService` (:48) | servicio de auditoría/limpieza DB |

**Patrones transversales `[CERT-a]`** (repetidos en los 4 — esto ES el ADN de la suite):
1. **Gate de licencia por feature name** propio: `honEasyTemplate`, `honEasyBinding`, `honEasyDatabaseManager`/`easyDatabaseManager`, `HBDashboard`. Sin feature → excepción `Unlicensed Feature ...`.
2. **UX vía `BIJavaScript`/`BJsBuild`**, no servlets propios: el widget raíz es un `BSingleton implements BIJavaScript, BIFormFactorMax` y el JS se sirve por el WebService estándar. El **path del bundle JS está XOR-ofuscado** en el `static{}` del `BJsBuild` (clave cíclica de 5 bytes) — vineflower no lo resolvió (residuo irreversible sin ejecutar).
3. **Backend del JS = RPCs Niagara** `@NiagaraRpc(transports=box/web)`, no endpoints HTTP. Métodos recurrentes: `checkFeatureLicense`, `checkEdgeController` (detecta OS `QNX`/`TITAN`), `getNavTree`/árbol de puntos.
4. **Lista de redes soportadas casi idéntica** en los 4 (filtro del nav-tree y del licenciamiento): `TrendN4:TrendNetwork`, `bacnet:BacnetNetwork`, `bacnetAws/OwsNetwork`, `sbc:SbcNetwork`, `clCBus:CBusNetwork`, `ipcCommBus:IPCNetwork`, `modbusTcp`, `mbus`, `niagaraDriver:NiagaraNetwork`. → conecta con los drivers de [Bloques 77-78].
5. **Detección de marca (brand) por licencia** → mismo mecanismo multi-OEM que `BGalileoService` ([Bloque 80.4]). `easyDatabaseManager` vive de hecho bajo el namespace `com.honeywell.galileo.point.data.manager.ux` y referencia `service:galileoPointViewer:PointListViewService` — **la suite easy\* es parte del stack Galileo**.

> Lectura arquitectónica: Galileo ([Bloque 80]) es la capa de *runtime/visualización web*; la familia `easy*` es la capa de *herramientas de ingeniería* (Workbench-time) del mismo producto. Comparten licencia, branding, detección de edge y lista de redes.

---

## 81.2 — easyTemplating: plantillas de widgets PX reutilizables (ETSO/ETCO + NTPL) `[CERT]`

**Problema que resuelve `[CERT-a]`**: crear una página/gráfico PX una vez y reutilizarla sobre N equipos sin recablear cada binding a mano.

Clases raíz verificadas `[CERT]`:
- `BEasyTemplatingService extends BComponent implements BIService` (:30)
- `BEasyTemplatingVirtualService extends BAbstractService` (:47) — soporte de espacios virtuales
- `BSimpleObjectFile extends BPxFile` (:24) — el tipo de archivo `.etso`
- `BEasyTemplatingBinding extends BBinding` (wb :61) — binding instanciable

**Dos mecanismos complementarios `[CERT-a]`**:

**A. Objetos reutilizables ETSO/ETCO** (la innovación): un archivo PX donde los ORDs de binding se **relativizan al token `$(var)`** y se embebe una **query NEQL** en el widget raíz (`local:|foxs:|station:|neql:n:child->(n:name="...")`). Al soltar el widget en cualquier gráfico, resuelve el componente destino automáticamente o pide selección interactiva (`BEtDataSourceFe`).
- `.etso` = Easy Template Simple Object; `.etco` = Complex Object (contiene varios ETSO).
- Los bindings nativos (`BPopupBinding` de kitPx) se sustituyen por `BEasyPopupBinding`/`BEasyHistoryPopupBinding` con ORD relativizado.
- Biblioteca en `~EasyTemplates/{SimpleObjects,ComplexObjects}/<lib>_lib/...`; runtime en `^etsoFiles/` + `^px/deploy/<name>/`.

**B. Generación de NTPL nativos** `[CERT-a]`: `BEasyTemplate.doGenerateTemplate()` → `EasyTemplatingGenerationHelper` produce un `.ntpl` estándar de Tridium (ZIP `template.bog` + `template-manifest.xml` + PX) desde cualquier componente, inyectando un `BEasyTemplatingLinker`, vistas PX dinámicas, la relación `n:parent` y tags `ntpl:*` (vendor hard-codeado `"Honeywell"`). Usa la API interna `com.tridium.template.*` (`TmplUtil.createInMemoryNtpl`, `ManifestXMLWriter`).

**Linker post-deploy** `[CERT-a]` (`BEasyTemplatingLinker`): dos timers — `LinkPoints` (15 s, crea `BLink` entre `BControlPoint` de igual nombre en subárboles source/target vía relación `n:parent`) y `ResetVariables` (1 s, refresca los bindings `.etso`). Bandera `isTemplateChanged`.

**Capa WB** `[CERT-a]`: `BEasyTemplatingSidebar extends BWbSideBar` (panel lateral con `.palette` generado en memoria, búsqueda, export/import/sync) + `BEasyTemplatingSimpleObjectCreator extends BWbTool` (wizard 6 pasos). Feature: `honEasyTemplate`.

---

## 81.3 — easyBinding: binding masivo punto→imagen + generador de paletas `[CERT]`

**Problema que resuelve `[CERT-a]`**: vincular masivamente puntos (Boolean/Numeric/Enum y sus variantes NiagaraVirtual) a widgets PX que cambian de **imagen** según el valor, sin programar cada link/converter.

Clases raíz verificadas `[CERT]`:
- `BEasyBindingSupportService extends BAbstractService` (:33) — acciones `updateVirtualEasyBindings` y `updatePxPagesWithEncryptedEasyPallet`.
- `BEasyBaseBinding extends BSecureBoundLabelBinding` (wb :25) — **dependencia crítica no pública**: `BSecureBoundLabelBinding` es de `com.tridiumx.ps.util` (Tridium **Professional Services**, no API pública). De él cuelgan `BEasyValueBinding`/`BEasyAlarmBinding`/`BEasyOverrideBinding`.
- `BEasyBindingWidget extends BWidget implements BIAgent` (wb :75) — el widget "todo en uno" (valor + alarma + override).
- `BHxPxEasyPicture extends BHxPxWidget implements BIAgent` (wb :45) — render web (Hx).

**Widget de 3 bindings simultáneos `[CERT-a]`**: un solo `BEasyBindingWidget` encapsula value+alarm+override y auto-configura el converter según el tipo del punto destino. El núcleo es `BEasyValueBinding.targetChanged()` → método `a(BWidget)`: si es `BBooleanPoint` mapea true/false→imagen; si `BNumericPoint` construye `BNumericToSimpleMap` desde un `BVector images`; si `BEnumPoint` un `BEnumToSimpleMap` por ordinal. Status→visual vía `getBit(8)` (alarma) y `getBit(32)` (override) — coherente con [Bloque 4] y con los converters de [Bloque 80.5].

**Doble familia de bindings `[CERT-a]`**: `BEasy*` (render PX en Workbench, sobre `BSecureBoundLabelBinding`) vs `BEb*` (`BEbValueBinding extends BValueBinding`, render web Hx — propaga el ORD a `valueOrd`/`alarmOrd`/`overrideOrd` del `BWebWidget` que consume el JS).

**Generador de paletas (Palette Builder Wizard, 5 pasos) `[CERT-a]`**: `BEasyPaletteBuilder extends BWbTool` escanea una estructura de carpetas `Folder_*/Widget_*/{ON,OFF,ALARM,OVERRIDE,STATE}/` (con `STATE/state.values` formato `valor=imagen`), construye widgets, **encripta las imágenes con AES** (`EncryptDecrypt.encrypt()` en `easybinding/util/EncryptDecrypt.java:54`; clave derivada del feature name `honEasyBinding` en `easybinding/util/KitpxUtils.java:38`; marcador final `{0x7F,0x7F}` en `EncryptDecrypt.java:61`) y **empaqueta un `<moduleName>-rt.jar` completo** con `JarOutputStream`. En web, `BHxPxEasyPicture.update()` desencripta y envía base64 al JS.
>
> `[§14 corregido por audit 2026-07-12 (audits/2026-07-12-certainty-audit.md, claim #22)]` — la clase dueña de la llamada al cifrado es `EncryptDecrypt`, NO `KitpxUtils` (que orquesta y aporta la clave). La sustancia (AES, fuente de la clave `honEasyBinding`, marcador `{0x7F,0x7F}`) fue CONFIRMED verbatim; solo se afinó la atribución de la clase.

**Dos jobs de migración batch `[CERT-a]`**: `BEasyBindingNiagaraVirtualSupportJob` (reescribe ORDs `NiagaraVirtual...` → `ControlPoint` real en todos los `.px`) y `BUpgradeToEncryptedEasyWidgets` (reemplaza `<Picture>`→`<EasyPicture>` en bytes del PX para migrar a paletas encriptadas).

> Hallazgos menores verificados por el sub-agente `[CERT-a]`: inconsistencia de vendor para "Trend" (`Trend_Control_Systems_Ltd` en rt vs `Tridium` en wb); residuo de un vendor externo `com.forestrock.genericUi.basic.BComp` (BComponent vacío usado como plantilla batchtool). Feature: `honEasyBinding`. Config UI: `^config/eb_config.xml`.

---

## 81.4 — easyHealthyBuilding: dashboard IAQ con índice de salud por zona `[CERT]`

**Qué hace `[CERT-a]`**: monitorea calidad de aire interior (IAQ) y calcula un **índice de salud compuesto por zona** sobre 7 métricas, con SPA React de visualización y alarmas tri-clasificadas.

Clases raíz verificadas `[CERT]`:
- `BEasyHealthyBuildingService extends BAbstractService` (:79)
- `BAbstractKpi extends BComponent` (:83, abstract) → `BNumericKpi extends BAbstractKpi` (:38, abstract) → `BCo2Kpi`/`BPm25Kpi`/`BTvocKpi`/`BTemperatureKpi`/`BHumidityKpi`/`BAchKpi` (:31). `BOccupancyKpi extends BBooleanKpi`.
- `BEasyHealthyBuildingZone extends BComponent` (:111) — contenedor de KPIs de una zona.
- `BEasyHealthyBuildingAlarmClass extends BAlarmClass` (:37).
- `BEasyHealthyBuildingOutOfRangeAlgorithm extends BOutOfRangeAlgorithm implements ISubscribeCallback` (:30) — algoritmo de alarma off-normal (el `implements` lo añadí yo, el sub-agente lo omitió).

**7 métricas, tags namespace `ehb:` `[CERT-a]`**: `co2`, `pm25`, `tvoc`, `temperature`, `humidity`, `occupancy`, `airChangesPerHour` (ACH). Discovery: `neql:ehb:schedule or ehb:sensor`.

**Motor de cálculo `IaqCalculatorExt` (POJO, no BComponent) `[CERT-a]`** — escala de score impar **1/3/5/7/9** por métrica, con rangos por defecto parametrizables vía JSON. Ej. CO2: `[0,600]→9`, `601-800→7`, `801-1500→5`, `1501-1800→3`, `>1800→1` (ppm). Temperatura siempre convertida a Celsius antes de evaluar (default `[18,21]°C`).

**Scoring dual (clave del diseño) `[CERT-a]`**:
- **Metric Score** = `kpiScore × kpiWeight` (calidad real). Pesos default: Temp/Humedad 9, CO2/PM2.5/Occupancy/ACH 5, TVOC 1.
- **Awareness Score** = 100 % si el KPI tiene datos válidos, ≈11 % si no (`weight` en vez de `9×weight`).
- `healthyPercentage = min(metricScore%, awarenessScore%)` → **penaliza la ausencia de datos**: un sensor sin lectura arrastra el índice aunque el resto sea perfecto.
- Zona = suma ponderada de sus KPIs; edificio = media aritmética de zonas válidas.

**Wellness de 5 niveles `[CERT-a]`**: `excellent/good/fair/poor/inadequate` (dos umbralizaciones distintas: a nivel zona corta en 90/75/60; a nivel KPI en 90/70/50/30). Alarmas mapeadas a `low/high/urgent`. **No referencia ningún estándar nombrado** (WELL/RESET/ASHRAE/WHO) aunque los umbrales son consistentes con ellos.

**Capa UX `[CERT-a]`**: SPA **React 16 + D3 v4 + moment + Semantic UI** embebida vía `BIJavaScript`/`BJsBuild`; bundle `easyHealthyBuilding.built.min.js`. RPC `BEasyHealthyBuildingRpc extends BComponent` con 30+ endpoints (`getKpiTrendOfZone`, `getKpiDetailsOfEachZone`, `buildingNotifications`...). Feature `HBDashboard` (licenciamiento por nº de zonas: small=2, medium=5, enterprise=∞, trial=5/30 días). Integra opcionalmente `honAlarmConsole`.

---

## 81.5 — easyDatabaseManager: auditoría y limpieza de la station database (BOG, NO SQL) `[CERT]`

**Hallazgo a destacar `[CERT-a]`**: pese al nombre "DatabaseManager", **NO toca SQL/JDBC/HDB**. Gestiona la **station database = el BOG** (grafo de componentes Niagara). Cero `import java.sql.*`, cero `DriverManager`. Las únicas "queries" son **BQL y NEQL** sobre el BOG.

Clases raíz verificadas `[CERT]`:
- `BEasyDatabaseManagerService extends BComponent implements BIService` (:48) — en `serviceStarted()` autoelimina el legacy `PointDatabaseManagerService` (cleanup de versión Trend anterior, "PDM").
- `BPointFindJob extends BJob` (:47), `BPointDeleteJob extends BJob` (:26), `BSecurePointDeleteJob extends BJob` (:26, puntos con firma electrónica), `BBackupStationJob extends BSimpleJob` (:50).
- `BUnusedPointsConfiguration extends BComponent` (:37) + `BUnwantedPointsConfiguration` — contenedores de criterios.

**Wizard de 5 pasos `[CERT-a]`**: (1) LicenseStatus — cuenta puntos activos vs licencia (`bql:select count(1) from control:ControlPoint where proxyExt != null` por red); (2) Unused — config de qué extensiones excluir; (3) Unwanted — criterios por nombre/displayName; (4) Review — `BPointFindJob` corre los filtros y serializa resultados a `file:^edm/edm.xml`; (5) Delete — backup `.dist` opcional (`BBackupStationJob`) + eliminación masiva (`parent.remove(property)` resolviendo cada ítem por handle ORD) con logs de auditoría `^edm/ItemsSelected.txt` / `ItemsDeleted.txt`.

**Lógica "unused" `[CERT-a]`** (`UnusedPointsFilter`): conjunto total de `ControlPoint` **menos** los que están "en uso" (en PxView, con LinkMark, AlarmExt, HistoryExt, HistoryImport, Analytics tag `Id.newId("a","a")`, PLV Favorites, TrendLinkedPlot, o EasyTemplate `.etso/.etco`). → **referencia explícita a los otros módulos easy\***: un punto usado por una plantilla `easyTemplating` cuenta como "en uso".

**Lógica "unwanted" `[CERT-a]`**: BQL dinámico `where displayName.toLowerCase like '%term%' or name ... like ...` con escaping de caracteres especiales (`%`→`\%`, `*`→`\*`, etc.). Entidades: `BControlPoint`, `BHistoryImport`, `BControlSchedule`/`BScheduleExport`.

Capa UX `[CERT-a]`: `BEasyDatabaseManagerWidget extends BSingleton implements BIJavaScript, BIFormFactorMax`, agente sobre 18 tipos de DeviceNetwork (permiso `RWI`), JS por `BJsBuild`. Feature `honEasyDatabaseManager` (legacy Trend: `honPointDatabaseManager`). Edge controllers detectados por OS `QNX`/`TITAN`.

> Gotcha verificado por el sub-agente `[CERT-a]`: typo sistemático `"HISOTRY"` (por "HISTORY") en 3 constantes, y nombres de logger inconsistentes (`easyDatabaseManager`/`PointsDatabaseManager`/`EasyDatabaseManager`). No afectan funcionalidad pero confunden el grep de logs en campo.

---

## 81.6 — Síntesis: la suite de productividad de Galileo

**Mapa de la familia `easy*`** — cuatro herramientas, un mismo producto:

| Módulo | Tiempo | Qué acelera | Artefacto que produce/consume | Feature |
|--------|--------|-------------|-------------------------------|---------|
| easyTemplating | WB | replicar páginas PX sobre N equipos | `.etso`/`.etco` + `.ntpl` (NEQL + `$(var)`) | `honEasyTemplate` |
| easyBinding | WB | bindear puntos a imágenes en masa | paleta `-rt.jar` con imágenes AES-128 | `honEasyBinding` |
| easyHealthyBuilding | runtime | dashboard IAQ + scoring de salud | índice por zona (React/D3) | `HBDashboard` |
| easyDatabaseManager | WB | auditar/limpiar la station DB (BOG) | `^edm/edm.xml` + backup `.dist` | `honEasyDatabaseManager` |

**Por qué importan para MX60 / trabajo Honeywell**:
1. **Patrón de plantilla con ORD relativizado (`$(var)` + NEQL)** de `easyTemplating` es la respuesta nativa Niagara al problema que una SPA externa ([Bloques 47-51]) resuelve en JS: cómo apuntar un widget genérico a "el punto correcto de este equipo". Si MX60 necesita templating de vistas, este es el mecanismo probado.
2. **easyBinding** demuestra cómo empaquetar assets visuales (imágenes encriptadas) dentro de un JAR de módulo generado en caliente — y cómo el render web (Hx) desencripta y entrega base64. Patrón reutilizable para proteger assets de marca.
3. **easyHealthyBuilding** es un caso completo de SPA React montada en Niagara vía `BIJavaScript`/`BJsBuild` con backend RPC — plantilla directa para cualquier dashboard custom (análogo de Reflow/MX60 pero en-Workbench, no externo).
4. **easyDatabaseManager** es una **herramienta de mantenimiento operacional**: auditar puntos huérfanos y limpiar la DB antes de un backup/migración. Útil como checklist en audits Honeywell ([Bloque 40-41]). Ojo: el "unused" cuenta dependencias de los otros `easy*` y de Analytics.

**Paralelo con Galileo ([Bloque 80])**: Galileo = runtime/visualización web (SignalR hubs); `easy*` = herramientas de ingeniería (Workbench). Comparten licencia, branding multi-OEM, detección de edge (`QNX`/`TITAN`) y la misma lista de redes soportadas. `easyDatabaseManager` vive literalmente bajo `com.honeywell.galileo.*` y referencia `galileoPointViewer` — **confirmado que la suite easy\* es parte del stack Galileo**.

**Pendiente conocido (irreversible, ZKM)**: nombres de clases internas (`a`/`b`/`c`) y rutas de bundles JS XOR-ofuscadas en los `static{}` de los `BJsBuild` — no recuperables sin ejecutar el código. El contenido de los `.js` minificados (`JavaEasyWidget.js`, bundles React) no se analizó en profundidad (fuera del alcance de la decompilación Java).
