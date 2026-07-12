# Analisis Forense: NiagaraMods Reflow v1.7.5 vs v1.7.7

**Fecha:** 2026-04-03
**Fuentes:**
- v1.7.5: `Reflow-Vine/nmodsreflow/nmodsreflow-rt/src` (decompilado con Vineflower, licenciamiento deshabilitado manualmente)
- v1.7.7: `modulos/reflow-177-vineflower/rt` (decompilado con Vineflower, licenciamiento intacto)

**Nota importante sobre la fuente v1.7.5:** El codigo fuente de v1.7.5 disponible fue previamente modificado para deshabilitar el sistema de licenciamiento (comentarios tipo `// licensing disabled - unlicensed build`). Por tanto, las diferencias en el subsistema de licenciamiento reflejan **la restauracion del codigo original** en v1.7.7, no necesariamente cambios nuevos entre versiones. Los cambios reales de v1.7.5 a v1.7.7 se identifican separadamente.

---

## Tabla Resumen de Cambios

| Archivo | Lineas Diff | Cambio Real | Ruido Decompilador | Categoria |
|---------|------------|-------------|-------------------|-----------|
| **BaseServlet.java** | ~310 | SI - Reestructuracion routing, AlarmQuery movido a POST, demo redirect | Renombrado variables (var5->var7) | HTTP/Routing |
| **BReflowService.java** | ~290 | SI - History group cache, licensing restaurado | Decompiler noise minimo | Core/Servicio |
| **License.java** | ~249 | PARCIAL - Restauracion licensing + posible mejora Niagara native | N/A (stub vs real) | Licenciamiento |
| **LicenseValidator.java** | ~221 | PARCIAL - Restauracion licensing (firma RSA, validacion host) | N/A (stub vs real) | Licenciamiento |
| **HistoryData.java** | ~169 | SI - Threading con PrivilegedAction + GhostSubscriber | Imports adicionales | Historia |
| **BReflowLicenseCommands.java** | ~150 | PARCIAL - Restauracion licensing | N/A (stub vs real) | Licenciamiento |
| **BReflowChannelService.java** | ~93 | NO - Solo ruido decompilador | method_311/312/297, lambda->clase anonima | WebSocket |
| **BReflowWebSocketAcceptor.java** | ~65 | NO - Solo ruido decompilador | method_0, method_295/291, lambda->Predicate | WebSocket |
| **LicenseManager.java** | ~52 | PARCIAL - Restauracion licensing | N/A (stub vs real) | Licenciamiento |
| **HistoryIO.java** | ~46 | SI - Cache de grupos de historia en archivo GZIP | N/A | Historia |
| **HistoryGhostSubscriber.java** | N/A (NUEVO) | SI - Subscribe/unsubscribe fantasma para historias | N/A | Historia |
| **AlarmData.java** | ~14 | SI - Campo ackTime agregado + lambda->clase anonima | method_365, field_282 | Alarmas |
| **AlarmSourceCollection.java** | ~12 | NO - Solo ruido decompilador | method_365, Comparator anonimo | Alarmas |
| **AlarmUuidArgs.java** | ~10 | NO - Solo ruido decompilador | field_282 (renombrado `end`) | Alarmas |
| **AlarmQueryResponse.java** | ~18 | SI - Cambiado de GET query string a POST JSON body | N/A | Alarmas/HTTP |
| **BReflowAlarmCommands.java** | ~10 | SI - Nuevo metodo `canAcknowledgeAlarms` | N/A | Alarmas |
| **HistoryList.java** | ~14 | NO - Solo ruido decompilador | lambda->Predicate/IntFunction | Historia |
| **HistoryGroups.java** | ~8 | NO - Solo ruido decompilador | method_316, method_329 | Historia |
| **HistoryFolderSerializer.java** | ~4 | POSIBLE - `getNavChildren().length > 0` -> `hasNavChildren()` | N/A | Historia |
| **BReflowSyncService.java** | ~30 | NO - Solo ruido decompilador | lambda->clase anonima, method_311 | Sync |
| **ConfigIO.java** | ~3 | NO - Solo ruido decompilador | VF renamed run() | Sync |
| **BackupManager.java** | ~51 | NO - Solo ruido decompilador | method_366, method_291 | Backups |
| **LicenseClient.java** | ~30 | PARCIAL - Restauracion licensing (HTTP a api.niagaramodules.com) | N/A (stub vs real) | Licenciamiento |
| **Feature.java** | ~6 | NO - Solo ruido decompilador | field_283 (renombrado `sku`) | Licenciamiento |
| **Query.java** | ~2 | NO - Solo ruido decompilador | method_363 (renombrado `map`) | Util |
| **IReflowCommand.java** | ~2 | NO - Solo ruido decompilador | method_0 (renombrado `run`) | WebSocket |
| **AsyncReflowCommand.java** | ~4 | NO - Solo ruido decompilador | method_0, VF renamed run() | WebSocket |
| **UX: BReflow.java** | 0 | NO | N/A | UX |
| **UX: BReflowConfig.java** | 0 | NO | N/A | UX |
| **UX: BReflowRedirect.java** | 0 | NO | N/A | UX |

---

## CLASE NUEVA: HistoryGhostSubscriber

**Ubicacion:** `history/HistoryGhostSubscriber.java`

```java
class HistoryGhostSubscriber extends Subscriber {
   BHistory history;
   Context field_284;

   public HistoryGhostSubscriber(BHistory history, Context ctx) {
      this.history = history;
      this.field_284 = ctx;
   }

   public void event(BComponentEvent bComponentEvent) {
      this.history.unsubscribe(this, this.field_284);
   }

   protected void unsubscribed(BComponent c, Context cx) {
      super.unsubscribed(c, cx);
   }
}
```

### Proposito
Es un **subscriber efimero ("fantasma")** que se suscribe a un `BHistory` y se auto-desuscribe inmediatamente cuando recibe el primer evento. El patron es conocido en Niagara como "touch subscription" - el acto de suscribirse y desuscribirse fuerza al sistema de historia a inicializar/refrescar su estado interno.

### Uso en v1.7.7
Se usa en `HistoryData.subscribeToHistory()` para "despertar" historias que no son de la estacion local antes de leer sus registros. Esto soluciona un problema donde historias remotas (via NiagaraNetwork) podian retornar datos vacios si nunca habian sido suscritas.

### Impacto
**Bug fix significativo** - Resuelve datos vacios en consultas de historias remotas (cross-station).

---

## Analisis Detallado por Subsistema

---

### 1. SUBSISTEMA HTTP / ROUTING

#### BaseServlet.java

**Cambios reales identificados:**

1. **Demo route redirect (NUEVA FUNCIONALIDAD)**
   - v1.7.5: La ruta `/demo/.*` NO tenia manejo especial en el else-if chain
   - v1.7.7: Se agrega `if (path.matches("/demo/.*")) { path = "/index.html"; }` al inicio del bloque else
   - **Impacto:** Las URLs de demo ahora redirigen a index.html (SPA routing), permitiendo deep-linking en demos

2. **AlarmQuery movido de GET a POST (CAMBIO IMPORTANTE)**
   - v1.7.5: `/station/alarms/query` se manejaba dentro del `doGet()` con parametros en query string
   - v1.7.7: `/station/alarms/query` se mueve al `doPost()` con body JSON
   - **Impacto:** Cambio de API - el frontend debe enviar POST en vez de GET para queries de alarmas
   - **Razon probable:** Los query strings tienen limite de longitud; filtros de alarmas complejos necesitan JSON body

3. **Reestructuracion del else-if chain**
   - v1.7.5: Cadena plana de `else if` para todas las rutas
   - v1.7.7: Cadena anidada con un bloque `else { if (path.matches(...)) }` que agrupa rutas secundarias
   - **Impacto funcional:** Ninguno - misma logica, diferente estructura. Probablemente un refactor del compilador o del desarrollador para mejorar legibilidad.

4. **pathFound para history-data**
   - v1.7.5: `HistoryChartDataResponse.serve()` se ejecutaba sin marcar `pathFound = true`
   - v1.7.7: Se agrega `pathFound = true` despues de servir history-data
   - **Impacto:** Bug fix - sin esto, despues de servir chart data, el servlet intentaba servir el archivo como FileResponse tambien

#### AlarmQueryResponse.java (CAMBIO REAL)

- v1.7.5: Lee query string de la URL (`req.getQueryString()`)
- v1.7.7: Lee JSON body del request (`req.getReader()` -> parse JSON -> extrae `query`)
- **Impacto:** Breaking change en la API - los clientes deben enviar POST con JSON body

---

### 2. SUBSISTEMA HISTORIA

#### HistoryData.java (CAMBIOS REALES SIGNIFICATIVOS)

1. **Threading con PrivilegedAction (NUEVA ARQUITECTURA)**
   - v1.7.5: `fromComponent()` iteraba las historias directamente en el thread del servlet
   - v1.7.7: Se crea una inner class `FromComponentTask implements Runnable` que ejecuta la consulta en un thread separado dentro de `AccessController.doPrivileged()`
   - **Razon:** Las consultas de historia pueden bloquear el thread del servlet por mucho tiempo. Ejecutarlas en un thread privilegiado separado evita timeouts y problemas de permisos.

2. **HistorySpaceConnection en vez de getHistories() (CAMBIO DE API)**
   - v1.7.5: `BIHistory[] histories = historyDb.getHistories()` - obtiene TODAS las historias y luego filtra
   - v1.7.7: `HistorySpaceConnection connection = historyDb.getConnection(null)` - obtiene una conexion y luego busca historias por ID con `connection.getHistory(BHistoryId.make(hId))`
   - **Impacto:** Mucho mas eficiente - en vez de cargar todas las historias en memoria, solo carga las solicitadas

3. **Ghost Subscriber para historias remotas (BUG FIX)**
   - v1.7.7 agrega `subscribeToHistory(h)` antes de leer registros cuando `!fromCurrentStation`
   - Usa `HistoryGhostSubscriber` para forzar inicializacion de historias remotas
   - **Impacto:** Resuelve datos vacios en historias cross-station

4. **Lookup por ID en vez de por nombre**
   - v1.7.5: Iteraba todas las historias y comparaba por `getDisplayName()` o `getId().encodeToString()`
   - v1.7.7: Busca directamente por `BHistoryId.make(hId)` via la conexion
   - **Impacto:** Mas robusto - no depende del display name que puede cambiar

#### HistoryIO.java (NUEVA FUNCIONALIDAD)

Se agregan 3 componentes nuevos:

1. **`refreshHistoryGroupCache()`** - Metodo publico que inicia un thread para refrescar el cache
2. **`writeHistoryGroupCache(JsonNode)`** - Serializa el arbol de grupos de historia a un archivo GZIP comprimido en el directorio home de la estacion
3. **`WriteHistoryGroupCacheTask`** - Inner class Runnable que obtiene el arbol de grupos y lo escribe

**Impacto:** Los grupos de historia ahora se cachean en disco como archivo GZIP. Esto evita recalcular el arbol de grupos en cada request, mejorando significativamente el rendimiento en estaciones con muchas historias.

#### HistoryFolderSerializer.java (POSIBLE MEJORA)

- v1.7.5: `if (folder.getNavChildren().length > 0)`
- v1.7.7: `if (folder.hasNavChildren())`
- **Impacto:** Mas eficiente - `hasNavChildren()` no necesita crear el array completo

---

### 3. SUBSISTEMA DE LICENCIAMIENTO

**Nota critica:** La version v1.7.5 disponible fue decompilada y luego manualmente modificada para deshabilitar todo el licenciamiento (stubs que retornan `true`, defaults de `enterprise`, limites de `9999`). Por tanto, la mayoria de las diferencias en este subsistema representan la **restauracion del codigo original**, no cambios entre v1.7.5 y v1.7.7.

Sin embargo, hay evidencia de al menos un cambio real: el soporte para **Niagara native licensing** (`hasNiagaraLicense()`).

#### License.java

El codigo restaurado muestra:
- Parsing de archivo XML `^niagaramods.license` con features, atributos, firma digital y expiracion
- Soporte dual: licencia nativa de Niagara (via `Sys.getLicenseManager()`) O archivo XML propietario
- Busqueda de vendor en 3 variantes: "NiagaraMods", "NiagaraModsOrg", "Tridium"
- Features con tipos: "license" (base) y "addon" (extension)
- Atributos por feature: buildings, floors, pages, devices, maps, station-type, officedemo

#### LicenseValidator.java

El codigo restaurado muestra:
- Validacion de firma RSA SHA256 con clave publica embebida en el modulo
- Cadena de validacion: hostId -> firma -> expiracion -> feature expiration
- Soporte para licencias nativas de Niagara (bypass de validacion XML)
- Deteccion de tipo de estacion: supervisor, jace (QNX/LYX/WEBX/GC5/PXC), demo

#### LicenseManager.java

- `hasNiagaraLicense()`: Verifica si existe feature "reflow" en el license manager de Niagara bajo vendors NiagaraMods, NiagaraModsOrg, o Tridium
- `refreshLicense()`: Si tiene licencia nativa, solo recarga; si no, contacta `api.niagaramodules.com`
- `stationType()`: Detecta si es demo, jace, o supervisor

#### LicenseClient.java

El codigo restaurado muestra:
- HTTP client que contacta `http://api.niagaramodules.com/license/{hostId}`
- Descarga el archivo de licencia y lo guarda como `^niagaramods.license`
- Envia User-Agent: `nmodsreflow/1.0.0 (Niagara/{hostId})`

#### BReflowService.java - Seccion Licensing

- Defaults restaurados a valores restrictivos: `trial`, limites bajos (1-10), maps deshabilitados
- `licenseRefreshed()`: Logica completa de parsing de atributos de licencia con soporte para addons
- `setTrialLicense()`: Establece limites de trial (1 building, 1 floor, 10 equipment, 3 pages)
- Timer de refresh cada 24 horas (`86400000L` ms)

#### BReflowLicenseCommands.java

- `getLicenseStatus()`: Retorna JSON con todos los detalles de la licencia
- `refreshLicense()`: Busca el servicio por BQL y ejecuta `doRefreshLicense()`
- Logica completa de validacion de station-type y limites por feature attribute

---

### 4. SUBSISTEMA DE ALARMAS

#### AlarmData.java (CAMBIO REAL)

1. **Nuevo campo `ackTime` en la serializacion de alarmas**
   - v1.7.7 agrega: `record.put("ackTime", alarm.getAckTime().encodeToString())`
   - **Impacto:** El frontend ahora recibe la hora de acknowledgement de cada alarma. Util para reportes y auditorias.

2. **Lambda a clase anonima** - Ruido de decompilador (el `PrivilegedExceptionAction` lambda se decompila como clase anonima)

#### BReflowAlarmCommands.java (NUEVA FUNCIONALIDAD)

**Nuevo metodo: `canAcknowledgeAlarms()`**
```java
public BValue canAcknowledgeAlarms(BComponent comp, BValue arg, Context cx) throws Exception {
    BComponent alarmService = Sys.getService(BAlarmService.TYPE);
    return BBoolean.make(cx.getUser().getPermissionsFor(alarmService).hasOperatorWrite());
}
```
- Verifica si el usuario actual tiene permiso de escritura de operador en el AlarmService
- **Impacto:** Permite al frontend determinar si debe mostrar el boton de "Acknowledge" segun los permisos del usuario. Mejora de seguridad/UX.

#### AlarmSourceCollection.java, AlarmUuidArgs.java

Solo ruido de decompilador:
- `add()` renombrado a `method_365()`
- `end` renombrado a `field_282`
- Lambda de Comparator convertida a clase anonima

---

### 5. SUBSISTEMA WEBSOCKET

#### BReflowChannelService.java

**100% ruido de decompilador.** Cambios:
- `put()` -> `method_311()`, `method_301()`, `method_312()`, `method_303()`
- `set()` -> `method_297()`
- `who()` -> `method_364()`
- `add()` -> `method_316()`
- Lambda de `forEach` convertida a `BiConsumer` anonimo

Estos son metodos de Jackson `ObjectNode`/`ArrayNode` que Vineflower no pudo resolver correctamente (probablemente por obfuscacion de los JARs de Jackson incluidos en el modulo).

#### BReflowWebSocketAcceptor.java

**Casi 100% ruido de decompilador.** Cambios:
- `run()` -> `method_0()` en IReflowCommand
- `has()` -> `method_295()`, `get()` -> `method_291()`
- `spy()` -> `method_367()`
- `cx` -> `field_281`
- Lambda de `removeIf` convertida a `Predicate` anonimo
- Lambda de `doPrivileged` convertida a clase anonima

#### SocketServlet.java, AsyncReflowCommand.java, IReflowCommand.java

Solo ruido de decompilador (method_0, VF renamed comments).

---

### 6. SUBSISTEMA SYNC

#### BReflowSyncService.java

**Casi 100% ruido de decompilador.** Cambios:
- Lambdas convertidas a clases anonimas
- `put/set/get` -> `method_311/297/291`
- VF renamed comments en metodos `run()`

#### ConfigIO.java

Solo VF renamed comments (3 lineas).

---

### 7. SUBSISTEMA BACKUPS

#### BackupManager.java

Solo ruido de decompilador:
- `age()` -> `method_366()`
- `get()` -> `method_291()`

---

### 8. SUBSISTEMA UX (Frontend Java)

**Los 3 archivos UX son identicos entre v1.7.5 y v1.7.7:**
- `BReflow.java` - Sin cambios
- `BReflowConfig.java` - Sin cambios
- `BReflowRedirect.java` - Sin cambios

---

## Bug Fixes Identificados

| # | Bug | Archivo | Impacto |
|---|-----|---------|---------|
| 1 | **Historias remotas retornan datos vacios** | HistoryData.java + HistoryGhostSubscriber.java | Alto - historias cross-station no funcionaban sin suscripcion previa |
| 2 | **pathFound no se seteaba para history-data** | BaseServlet.java | Medio - despues de servir chart data, el servlet intentaba servir FileResponse |
| 3 | **`ackTime` faltaba en respuesta de alarmas** | AlarmData.java | Bajo - el frontend no podia mostrar cuando se hizo acknowledge |

---

## Nuevas Funcionalidades

| # | Feature | Archivos | Descripcion |
|---|---------|----------|-------------|
| 1 | **Cache de grupos de historia en disco** | HistoryIO.java, BReflowService.java | Los grupos de historia se cachean como archivo GZIP. Refresh programable por timer diario. Nuevas propiedades: `historyGroupCacheRefresh`, `historyGroupRefreshTime`, accion `refreshHistoryGroupCache` |
| 2 | **Threading privilegiado para consultas de historia** | HistoryData.java | Las consultas de historia se ejecutan en threads separados con `AccessController.doPrivileged()`, evitando bloqueos del servlet |
| 3 | **Lookup de historias por ID directo** | HistoryData.java | Usa `HistorySpaceConnection.getHistory(BHistoryId)` en vez de iterar todas las historias |
| 4 | **Demo deep-linking** | BaseServlet.java | Rutas `/demo/*` ahora redirigen a index.html para soporte SPA |
| 5 | **Permiso de acknowledge de alarmas** | BReflowAlarmCommands.java | Nuevo comando `canAcknowledgeAlarms` que verifica permisos del usuario |
| 6 | **Alarm query via POST** | BaseServlet.java, AlarmQueryResponse.java | Queries de alarmas ahora usan POST con JSON body en vez de GET con query string |
| 7 | **HistoryFolderSerializer optimizado** | HistoryFolderSerializer.java | `hasNavChildren()` en vez de `getNavChildren().length > 0` |

---

## Cambios de Seguridad

| # | Cambio | Impacto |
|---|--------|---------|
| 1 | `canAcknowledgeAlarms` verifica `hasOperatorWrite()` | El frontend puede ocultar el boton de acknowledge si el usuario no tiene permisos |
| 2 | Alarm query movido a POST | Evita que parametros de filtrado sensibles aparezcan en URLs/logs del servidor |
| 3 | `AccessController.doPrivileged()` en consultas de historia | Las consultas de historia se ejecutan con privilegios del sistema, no del servlet thread |

---

## Breaking Changes

| # | Cambio | Impacto | Mitigacion |
|---|--------|---------|------------|
| 1 | **AlarmQuery GET -> POST** | El frontend JS que usaba `GET /station/alarms/query?range=...` debe cambiar a `POST /station/alarms/query` con JSON body `{"query": "range=..."}` | Actualizar el modulo UX al mismo tiempo |
| 2 | **Defaults de licencia mas restrictivos** | `trial` en vez de `enterprise` como default; limites de 1/10/3 en vez de 9999 | Solo afecta instalaciones nuevas sin licencia |
| 3 | **Nuevas propiedades en BReflowService** | `historyGroupCacheRefresh` y `historyGroupRefreshTime` son propiedades nuevas | Se agregan automaticamente con defaults; no rompe estaciones existentes |

---

## Ruido de Decompilador vs Cambios Reales

### Patrones de ruido de Vineflower identificados

1. **Renombrado de metodos Jackson:** `put()` -> `method_311()`, `set()` -> `method_297()`, `get()` -> `method_291()`, `has()` -> `method_295()` - Esto ocurre porque el modulo embebe JARs de Jackson con ofuscacion parcial y Vineflower no puede resolver los nombres.

2. **Renombrado de campos:** `end` -> `field_282`, `ctx` -> `field_284`, `sku` -> `field_283`, `cx` -> `field_281` - Vineflower no pudo recuperar los nombres originales de variables locales/campos.

3. **Lambdas -> clases anonimas:** Todas las expresiones lambda de Java 8 se decompilan como clases anonimas (`new Predicate() { ... }`, `new BiConsumer() { ... }`, etc.). Esto sugiere diferencias en la version de Vineflower usada o en los flags de decompilacion.

4. **`// $VF: renamed from:`** - Comentarios de Vineflower indicando que renombro un metodo/campo. Estos son puramente informativos.

5. **`method_0` para `run()`** en IReflowCommand - Vineflower renombro el metodo `run()` de la interfaz, propagandose a todas las implementaciones.

### Estadisticas

- **Archivos con SOLO ruido de decompilador:** 18 de 28 archivos con diferencias
- **Archivos con cambios reales:** 10 archivos
- **Archivos sin ningun cambio:** 3 (todos UX)
- **Clases nuevas:** 1 (HistoryGhostSubscriber)

---

## Conclusion: Vale la Pena Actualizar a v1.7.7?

### SI, la actualizacion es recomendable. Las razones:

1. **Bug fix critico de historias remotas:** El `HistoryGhostSubscriber` resuelve un problema real donde las consultas de historia cross-station (via NiagaraNetwork) podian retornar datos vacios. Este es el cambio mas impactante de la version.

2. **Mejora significativa de rendimiento en historias:**
   - Cache de grupos en disco (GZIP) evita recalcular en cada request
   - Lookup directo por ID en vez de iterar todas las historias
   - Threading con `PrivilegedAction` evita bloqueos del servlet

3. **Mejora de seguridad en alarmas:** El nuevo `canAcknowledgeAlarms` permite control de permisos granular, y mover alarm queries a POST es una mejora de seguridad estandar.

4. **Riesgo bajo:** Solo hay 1 breaking change funcional (AlarmQuery GET->POST) que requiere que el frontend UX se actualice simultaneamente. Las nuevas propiedades del servicio tienen defaults sensatos.

### Riesgos a considerar:

- El frontend UX debe actualizarse para enviar alarm queries como POST
- En una instalacion sin licencia, los defaults cambian de "enterprise ilimitado" a "trial restrictivo" (1 building, 10 equipment, 3 pages)
- El cache de grupos de historia agrega un archivo al directorio de la estacion y un timer diario

### Impacto estimado por subsistema:

| Subsistema | Impacto v1.7.7 |
|------------|----------------|
| Historia | ALTO - Bug fix + rendimiento |
| Alarmas | MEDIO - Seguridad + ackTime |
| HTTP/Routing | BAJO - Demo links + pathFound fix |
| WebSocket | NINGUNO - Solo decompiler noise |
| Sync | NINGUNO - Solo decompiler noise |
| UX Java | NINGUNO - Identico |
| Licenciamiento | N/A - Restauracion de codigo, no cambio real |
