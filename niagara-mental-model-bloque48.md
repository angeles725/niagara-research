# Bloque 48 — RBAC visibility en frontend: server is truth, client gating es UX

**Fecha**: 2026-05-06
**Método**: Investigación empírica READ-ONLY. Análisis del RBAC server-side de Niagara (Bloque 11), API real de bajaScript browser (Bloque 22), Subscriber events (Bloque 42), nav tree filtering (Bloque 35), patrones empíricos en Reflow-Clean-177 (`profiles.js` + `BReflowUserCommands.java` + `bajascript.js`).
**Fuentes primarias**:
- `BReflowUserCommands.java` (Reflow-Clean-177 `nmodsreflow-rt` — patrón canónico server-side getRoles)
- `BReflowAlarmCommands.canAcknowledgeAlarms` (decompilado — patrón `getPermissionsFor(svc).hasOperatorWrite()`)
- `reflow-frontend/src/store/modules/profiles.js` (authorizeLink engine 7-path + restrictNewContent dual mode)
- `bajaScript-ux.jar:rc/bs.built.min.js` (360 KB runtime — flagsChanged/facetsChanged callbacks)
- `bajaScript-ux.jar:rc/Component.js` (lifecycle events Bloque 22.808)
- Bloque 11 (RBAC server-side: BUser/BRole/BCategory/BPermissions 6 bits)
- Bloque 22 (BajaScript browser API)
- Bloque 35 (nav tree filtering por permissions)
- Bloque 38 (BSpace.canRead/canWrite/canInvoke server-side)
- Bloque 42 (Subscriber lifecycle + flagsChanged/facetsChanged)
- Bloque 44 (canAcknowledgeAlarms server-side call empírico)
- Bloque 46.G7 (LEVEL_1 Life Safety SIN enforcement automático)

**Versión analizada**: Honeywell OptimizerSupervisor-N4.14.0.162 + Reflow-Clean-177 réplica.

---

## 48.0 Contexto, scope, qué NO es este bloque

### ¿Qué ES este bloque?

Este bloque documenta **cómo una SPA externa hace gating de UI según permisos del usuario** cuando consume una station Niagara. Específicamente:

- Qué expone realmente bajaScript browser sobre permisos (poco — más de lo que parece y menos de lo que se necesita)
- Cómo el server filtra slots/nodos del nav tree ANTES de enviarlos al cliente (slot pruning automático)
- Por qué el cliente NO puede consultar BPermissions directamente (la API JS no lo expone)
- El patrón canónico empírico: server-side `canX()` calls que retornan `BBoolean` consumidos vía `serverSideCall`
- El patrón Reflow alternativo: role-name matching client-side con "profiles" custom (NO usa BPermissions)
- Por qué el client-side gating es PURO UX y nunca security boundary
- LEVEL_1 Life Safety sin RBAC enforcement automático (el caso edge más peligroso)

### ¿Qué NO es este bloque?

- **NO es el modelo RBAC server-side** — eso vive en Bloque 11. Acá lo recapitulamos lo justo para entender qué llega al cliente.
- **NO es authentication** — login, SCRAM, JSESSIONID viven en Bloques 18, 30, 47. Acá asumimos user autenticado.
- **NO es authorization de writes** — eso vive en Bloque 46 (priority array + RBAC en writes). Acá nos enfocamos en visibility (read access).
- **NO es i18n / facets formatting** — eso es Bloque 49 (pendiente).
- **NO inventa una API que no existe** — bajaScript NO expone `comp.canRead()/canWrite()/canInvoke()` directo. Documentamos el gap honestamente.

### Pregunta unificadora

> Mi SPA muestra un panel de control con sliders, tabs, botones de override. El usuario operador-junior NO debería ver el botón "Emergency Override" ni la tab "System Configuration". ¿Cómo lo gateo desde el frontend?

**Respuesta corta**: tres opciones, ordenadas de menos a más correcta:

1. **Esconder por flags del slot** (`hidden`, `readonly`) — Niagara filtra automáticamente, el slot no aparece en el component si el user no tiene permission. Para flags que SÍ aparecen, usar `flagsChanged` callback.
2. **Server-side `canX()` calls** — exponer un BOX command custom (`BReflowUserCommands.getRoles`, `BReflowAlarmCommands.canAcknowledgeAlarms`) que el server evalúa con `cx.getUser().getPermissionsFor(svc).hasOperatorWrite()` y retorna `BBoolean`. La SPA lo invoca y gatea según resultado.
3. **Role-name matching con profile config custom** (patrón Reflow) — la SPA pide los roles del user al login (`getRoles()` retorna CSV string), los matchea contra una collection de "profiles" declarativa con restrictions, oculta nodes según.

**Crítico**: NINGUNA de estas opciones es seguridad real. El server debe rechazar requests no autorizadas con `PermissionException` 403. El gating client-side es puro UX. Si la SPA expone un botón que el server rechaza, el botón debe estar oculto/deshabilitado — pero si el atacante lo invoca directo via fetch, el server es el que dice "no".

---

## 48.1 RBAC server-side: el modelo verdadero (recap Bloque 11)

### 48.1.1 La cadena BUser → BRole → BPermissions

**CONFIRMADO** (Bloque 11.1.1-11.1.4):

```
BUser ──── tiene N ────> BRole
                          │
                          └──── contiene ───> BPermissionsMap
                                                │
                                                └─ array indexado por categoryIndex
                                                    │
                                                    └─ BPermissions (6 bits "rwiRWI")
```

**BPermissions — granularidad 6 bits**:
- `r` = operator read | `w` = operator write | `i` = operator invoke
- `R` = admin read | `W` = admin write | `I` = admin invoke
- Encoding string: `"rwi"` (operator full), `"RWI"` (admin full), `"r"` (solo read), `"RW"` (admin RW sin invoke)
- **Normalización automática**: `ADMIN_WRITE` fuerza `OPERATOR_READ + OPERATOR_WRITE + ADMIN_READ`. No hay configs inconsistentes.

**BCategoryService**: mapea BCategoryMask 64-bit a índices [1..N]. Máximo 64 categorías per station. Los componentes pertenecen a 0+ categorías. La evaluación de permisos cruza la categoría del componente con la BPermissionsMap del user.

### 48.1.2 La API server-side canónica

```java
// La pregunta canónica: ¿este user puede hacer X sobre Y?
BPermissions perms = cx.getUser().getPermissionsFor(comp);
boolean canDo = perms.hasOperatorWrite();   // o .hasOperatorRead(), etc.
boolean canDoAdmin = perms.hasAdminWrite(); // bit superior
```

**CONFIRMADO** en código real (Bloque 44 + Bloque 51):

```java
// BReflowAlarmCommands.canAcknowledgeAlarms — patrón canónico server-side gating
public BValue canAcknowledgeAlarms(BComponent comp, BValue arg, Context cx) {
    BAlarmService alarmService = (BAlarmService) Sys.getService(BAlarmService.TYPE);
    return BBoolean.make(cx.getUser().getPermissionsFor(alarmService).hasOperatorWrite());
}
```

Y otro patrón empírico real:

```java
// BReflowUserCommands.getRoles — expone los roles del user actual
public BValue getRoles(BComponent comp, BValue arg, Context cx) throws Exception {
    BUser user = cx.getUser();
    return BString.make((String) user.getRoles());  // CSV string: "admin,operator,..."
}
```

**Contexto**: ambos son `BIServerSideCallHandler` con `@AgentOn(types={"..."}, requiredPermissions="r")`. Es el único patrón soportado para que la SPA pregunte sobre permisos al server — porque la API JS browser NO expone equivalente directo (ver 48.2).

### 48.1.3 Slot pruning: la filtración invisible

**CONFIRMADO** (Bloque 35.478, 1299 + Bloque 22.1140):

Cuando el browser pide un componente vía ORD resolution (`baja.Ord.make("station:|slot:/Drivers/Foo").get()`), el server:

1. Identifica el componente `Foo`
2. Para cada slot de `Foo`, evalúa `cx.getUser().getPermissionsFor(slot.getComponent()).hasOperatorRead()`
3. **Slots sin OPERATOR_READ son ELIMINADOS de la respuesta** — no aparecen en el Component que llega al browser
4. El cliente recibe un Component "podado" — solo los slots que el user puede leer

**Implicación directa**: el cliente NO PUEDE saber si un slot fue ocultado por permisos o si el slot no existe. Para el cliente, ambos casos son indistinguibles. Esto es **slot pruning automático** — la primera línea de defensa.

**Bug histórico** (Bloque 22.1145-1148):
- NCCB-632: HxPx Binding no chequeaba read permissions (slot pruning bypass)
- NCCB-643: HxPxSlider sin permission check
- NCCB-758: pxInclude no enforzaba permissions
- #16259: BHxPxValueBinding mangled permissions

Estos bugs muestran que el slot pruning depende de cada componente UI usar correctamente la API. NO es transparente al 100% — implementaciones custom pueden saltarlo si no llaman a `BPermissions.check()` antes de bind.

---

## 48.2 La API browser: qué expone realmente bajaScript

### 48.2.1 Lo que NO existe (gap documental crítico)

**HALLAZGO EMPÍRICO**: bajaScript browser **NO expone** los siguientes métodos sobre `baja.Component`:

```javascript
// ESTOS NO EXISTEN en la API JS pública
comp.canRead()           // ❌ no existe
comp.canWrite()          // ❌ no existe
comp.canInvoke()         // ❌ no existe
comp.getPermissions()    // ❌ no existe
slot.canRead()           // ❌ no existe
slot.requiresAdmin()     // ❌ no existe
baja.user.hasPermission(comp, 'w')  // ❌ no existe
```

**Por qué NO existen** (INFERIDO):

1. **El modelo Niagara es categoría-céntrico**. Para responder "¿puede este user leer este slot?", el server cruza:
   - `comp.getCategoryMask()` (64-bit bitmap)
   - `BCategoryService.resolveIndices(mask)` (mapeo a índices)
   - Para cada índice, `user.permissions[index]` (lookup en BPermissionsMap)
   - Combinación OR/AND según `BCategory.mode`
   
   Replicar esto client-side requiere enviar TODO el mapping al cliente — incluyendo BCategoryMask de cada componente, la BPermissionsMap completa del user, y el modo de cada BCategory. Es información sensible (cualquier user vería el modelo de permisos completo) y voluminosa.

2. **El slot pruning ya es la respuesta** en la mayoría de casos — si el slot no aparece en el Component, el user no puede leerlo. La SPA puede usar `comp.has('slotName')` como proxy de "user puede leer este slot".

3. **Niagara nunca priorizó SPAs externas**. Las UIs principales son Workbench (Java Swing, corre como super-user típicamente) y PX/HX (Niagara-rendered, server-side templates). Para esas UIs el server hace el gating al renderizar — no se necesita API JS.

### 48.2.2 Lo que SÍ existe — proxy indirecto vía slot presence

```javascript
// Patrón A: ¿el slot existe? Proxy de "puede leerlo"
require(['baja!'], function(baja) {
  baja.Ord.make('station:|slot:/Drivers/Foo').get().then(function(comp) {
    if (comp.has('sensitiveSlot')) {
      // user puede leer sensitiveSlot (server lo incluyó en la respuesta)
      var value = comp.get('sensitiveSlot');
    } else {
      // o el slot no existe O user no tiene OPERATOR_READ
      // — INDISTINGUIBLE desde el cliente
    }
  });
});
```

**Limitación crítica**: este proxy funciona solo para READ. Para WRITE/INVOKE no hay equivalente — el slot puede estar visible (READ ok) pero el user no tener WRITE/INVOKE. Detectar esto requiere intentar la operación y atrapar el `PermissionException` en el catch.

```javascript
// Patrón B: try/catch del invoke — detect-by-failure
comp.invoke('emergencyOverride', value)
  .then(function() {
    // user PUEDE invokear (no hubo permission exception)
  })
  .catch(function(err) {
    if (err && err.message && err.message.indexOf('PermissionException') >= 0) {
      // user NO puede invokear — bloquear UI
    }
    // otros errores: red, validation, etc.
  });
```

**GOTCHA G48-1 — Detect-by-failure es post-acción, no pre-acción**: el patrón B descubre la denegación DESPUÉS de intentar la operación. Si la operación tiene side effects (escribir en log, disparar alarm, etc.), el daño puede estar hecho aunque el server rechace. Para safety, NO usar detect-by-failure como gating UI primario — usar `canX()` server-side call (48.6) o flag-based hiding (48.4).

### 48.2.3 Lo que SÍ existe — Subscriber events para flags y facets

**CONFIRMADO** (Bloque 22.808 + Bloque 42.119-189):

```javascript
var sub = new baja.Subscriber();
sub.attach({
  flagsChanged: function(slot, cx) {
    // flags del slot cambiaron — re-evaluar visibility
    var flags = slot.getFlags();
    // BFlags: HIDDEN (0x40000), READONLY (0x100), OPERATOR (0x100), etc.
  },
  facetsChanged: function(slot, cx) {
    // facets cambiaron — range, units, format pueden cambiar dinámicamente
  },
  componentFlagsChanged: function(cx) {
    // flags del comp ENTERO (no de un slot) cambiaron
    // ej. el comp se volvió hidden o readonly al admin
  }
});
sub.subscribe(comp);
```

**Estos eventos son la única señal estandarizada** que la SPA recibe del server cuando algo relacionado a visibility cambia. Si un admin re-asigna roles al user actual mientras la SPA está activa, el server propagará `componentFlagsChanged` o `flagsChanged` según corresponda — la SPA puede re-evaluar y re-renderizar.

**INFERIDO**: el server NO emite un evento dedicado tipo "permissions changed for user X". El cambio se propaga indirectamente vía slot/component flag changes (que el server recompute al re-evaluar el RBAC). En la práctica, una sesión activa probablemente NO observa cambios de roles en vivo — el user tendría que re-loguearse para ver el efecto. **TODO 48-1**: validar empíricamente.

### 48.2.4 La API server-side `BSpace.canRead/canWrite/canInvoke` — sí existe pero NO es JS

**CONFIRMADO** (Bloque 38.295-303):

```java
// BSpace API — server-side, accesible desde Java/Workbench, NO desde JS
public class BSpace extends BComponent {
    public BPermissions getPermissionsFor(FilePath, Context);
    public BPermissions getPermissions(Context);
    public boolean canRead(OrdTarget);
    public boolean canWrite(OrdTarget);
    public boolean canInvoke(OrdTarget);
}
```

Estos métodos viven en `BSpace` y subclases (BFileSpace, BLocalizedFileSpace, BScopedFileSpace). Son la API canónica para el filesystem. **NO están expuestos a bajaScript** — el cliente JS no puede llamarlos directamente. Para pre-validar permisos a nivel de filesystem desde la SPA, el módulo `-rt` debe exponer un servlet o BOX command custom que internamente llame a `space.canRead(target)`.

**Implicación**: la API server-side existe (`canRead/canWrite/canInvoke`) pero requiere un wrapper Java custom para alcanzar al cliente JS. El cliente NO tiene proxy directo.

---

## 48.3 Slot pruning: el primer filtro automático

### 48.3.1 Cómo funciona el pruning

**CONFIRMADO** (Bloque 22.1140 + Bloque 35.478):

Cuando el cliente JS pide un Component:

```
1. baja.Ord.make("station:|slot:/Drivers/Foo").get()
2. → POST /box → message {r:1, t:"rt", c:"sys", k:"resolveOrd", b:{ord:"..."}}
3. Server resuelve ORD → obtiene BComponent Foo
4. Server itera slots de Foo:
   for each slot:
     if (cx.getUser().getPermissionsFor(slot.getComponent()).hasOperatorRead()) {
         include slot in response
     } else {
         omit slot
     }
5. Response: Component con SOLO los slots permitidos
6. Cliente recibe component "podado"
```

**Resultado**: el cliente nunca ve nombres de slots que no puede leer. La estructura del componente, desde la perspectiva del cliente, es ya filtrada.

### 48.3.2 Implicaciones prácticas para la SPA

```javascript
require(['baja!'], function(baja) {
  baja.Ord.make('station:|slot:/Drivers/HVAC1').get().then(function(comp) {
    var allSlots = comp.getSlots();  // SOLO los slots que user puede leer
    
    // Iterar y construir UI — automáticamente filtrada
    allSlots.eachWhile(function(slot) {
      // construir widget para este slot
    });
  });
});
```

La SPA NO necesita filtrar manualmente la lista — el server ya lo hizo. Pero **necesita saber qué espera** — si el código asume "siempre hay un slot `setpoint`" pero el user no tiene permission, `comp.get('setpoint')` retorna `undefined` y la SPA puede crashear. **Defensive coding obligatorio**: validar `comp.has(slotName)` antes de acceder.

### 48.3.3 Limitaciones del pruning

1. **Solo aplica a READ**. Si el user puede leer pero no escribir, el slot SÍ aparece — la SPA debe gatear writes por separado (48.6).

2. **No aplica al nivel de propiedades del slot**. Si el slot `setpoint` tiene facets `{minHidden: true, maxHidden: false}`, ambos facets viajan al cliente — el server no filtra dentro del slot.

3. **No aplica a actions/topics estructuralmente**. Las actions visibles en Workbench (ej. `emergencyOverride`) viajan en la estructura del component aunque el user no pueda invocarlas. El cliente las ve pero el server rechaza la invocación con `PermissionException`.

4. **Pruning es per-component, no per-tree**. Si el user puede leer `/Drivers/HVAC1` pero no `/Drivers/HVAC1/Sensor1`, hacer `comp.get('Sensor1')` desde el primer comp returna `undefined` (pruning de hijo). La navegación tree-wise debe defenderse contra holes.

---

## 48.4 Flags como señales UI: HIDDEN, READONLY, OPERATOR

### 48.4.1 BFlags — los 21 flags relevantes

**CONFIRMADO** (Bloque 4.564 + Bloque 4 slot system):

Niagara define 21 slot flags. Los relevantes para visibility UI:

| Flag | Bit | Significado | Comportamiento UI esperado |
|------|-----|-------------|----------------------------|
| `HIDDEN` | 0x40000 | Slot debe ocultarse en UI estándar | Widget no se renderiza |
| `READONLY` | 0x100 | Slot no aceptaría writes | Input deshabilitado, sin opción de edit |
| `OPERATOR` | 0x100 | Slot operable por nivel operator (no solo admin) | Botón visible incluso para operator |
| `SUMMARY` | 0x4 | Slot aparece en property sheet summary | Widget debe estar en vista resumen |
| `EXECUTE_ON_CHANGE` | 0x80 | Acción se ejecuta al cambio | UI debe debounce o confirmar |
| `NO_AUDIT` | 0x800 | Cambios no se auditan | (no UI direct) |

**Nota**: `OPERATOR` (0x100) y `READONLY` (0x100) tienen el mismo bit value pero diferente semántica según contexto (Property vs Action). Niagara usa el campo `slotType` para desambiguar.

### 48.4.2 Patrón flag-based hiding

```javascript
require(['baja!'], function(baja) {
  baja.Ord.make('station:|slot:/Drivers/HVAC1').get().then(function(comp) {
    var setpointSlot = comp.getSlot('setpoint');
    
    if (setpointSlot.getFlags() & baja.Flags.HIDDEN) {
      return;  // no renderizar
    }
    
    var input = renderSetpointWidget(comp.get('setpoint'));
    
    if (setpointSlot.getFlags() & baja.Flags.READONLY) {
      input.disabled = true;  // mostrar pero no editable
    }
  });
});
```

### 48.4.3 Reactividad: flagsChanged en vivo

**CONFIRMADO** (Bloque 42.119, 989):

```javascript
sub.attach({
  flagsChanged: function(slot, cx) {
    var newFlags = slot.getFlags();
    
    // Re-evaluar visibility del widget
    if (newFlags & baja.Flags.HIDDEN) {
      hideWidget(slot.getName());
    } else if (newFlags & baja.Flags.READONLY) {
      disableWidget(slot.getName());
    } else {
      enableWidget(slot.getName());
    }
  }
});
sub.subscribe(comp);
```

**Casos de uso reales** donde flags cambian dinámicamente:
- Modo manual vs automático: en modo automático, ciertos slots de override están READONLY
- Estado de mantenimiento: cuando un equipo entra mantenimiento, slots críticos se vuelven HIDDEN
- Permisos contextuales: admin desactiva un sensor — slot pasa a HIDDEN para todos

**GOTCHA G48-2 — Flags vs permissions son cosas distintas**: HIDDEN/READONLY son metadata del SLOT, no del USER. Un slot READONLY es READONLY para TODOS los users. Un slot que aparece (no fue prunido) puede tener READONLY y ser editable solo por admin (write enforcement server-side adicional). Para gating per-user usar slot pruning + canX() calls, NO solo flags.

---

## 48.5 Navigation tree filtrado por permissions

### 48.5.1 Cómo Niagara filtra el nav tree

**CONFIRMADO** (Bloque 35.478 + Bloque 35.1299):

El nav tree (la jerarquía que se ve en Workbench/PX/HX como árbol de stations + drivers + folders + componentes) se construye server-side. El servidor:

```
1. Para cada nodo del nav tree:
   a. Identificar el AgentInfo del nodo (qué tipo de comp es)
   b. Llamar agent.getRequiredPermissions() — el agente declara qué permisos necesita
   c. Comparar con cx.getUser().getPermissionsFor(comp)
   d. Si user no tiene los required perms → omitir nodo del tree
2. Retornar el tree filtrado al cliente
```

**El cliente recibe un nav tree ya filtrado** — solo nodos que el user puede READ aparecen.

### 48.5.2 Implicaciones para una SPA externa

Si la SPA construye su propia navegación basada en el ORD tree de la station:

```javascript
require(['baja!'], function(baja) {
  baja.Ord.make('station:|slot:/').get().then(function(root) {
    // root.getChildren() retorna SOLO hijos visibles al user
    root.getChildren().each(function(child) {
      addNavNode(child.getNavName(), child.getOrd());
    });
  });
});
```

No hace falta filtrar — el server ya filtró. Pero **si la SPA tiene un nav tree HARDCODED** (lista estática de paths conocidos), debe defenderse: cualquier path puede fallar con 403 si el user no tiene permission.

### 48.5.3 Reflow: nav custom desacoplado del Niagara tree

Reflow NO usa el nav tree de Niagara directamente — construye su propia navegación basada en "pages", "buildings", "equipment" que vive en `^reflow/config.json`. El gating del nav es por la lógica `authorizeLink` (ver 48.7) que matchea el link contra `profile.restrictions.routes`.

Esto desacopla la navegación de la estructura ORD del station — pero también significa que el gating está en cliente, no en server. Es vulnerable: un user que conozca las rutas puede navegar manualmente cambiando la URL.

**GOTCHA G48-3 — Custom nav requiere re-implementar todo el RBAC**: cuando la SPA descarta el nav tree de Niagara, pierde el filtrado automático server-side. Tiene que re-implementar el gating. Reflow lo hace con `authorizeLink` — pero el server NO valida que el user pueda acceder a esas pages. El gating es 100% cliente. Atacante con DevTools puede saltarlo trivialmente.

---

## 48.6 Patrón canónico empírico: server-side `canX()` calls

### 48.6.1 La forma correcta

```java
// Server-side handler (Java, módulo -rt)
@NiagaraType(agent={@AgentOn(types={"nmodsreflow:ReflowService"}, requiredPermissions="r")})
public class BReflowAlarmCommands extends BComponent implements BIServerSideCallHandler {
    
    public BValue canAcknowledgeAlarms(BComponent comp, BValue arg, Context cx) {
        BAlarmService svc = (BAlarmService) Sys.getService(BAlarmService.TYPE);
        return BBoolean.make(cx.getUser().getPermissionsFor(svc).hasOperatorWrite());
    }
}
```

```javascript
// Client-side: SPA invoca via BOX
require(['baja!'], function(baja) {
  baja.Ord.make('service:nmodsreflow:ReflowService').get()
    .then(function(svc) {
      return svc.serverSideCall('canAcknowledgeAlarms');
    })
    .then(function(canAck) {
      if (canAck.encodeToString() === 'true') {
        showAckButton();
      }
    });
});
```

**Por qué este patrón es CORRECTO**:

1. **El server evalúa el RBAC nativo** — usa `cx.getUser().getPermissionsFor(svc).hasOperatorWrite()` que es la API canónica.
2. **El cliente NO necesita conocer el modelo de permisos** — solo invoca un yes/no remoto.
3. **El server ya valida la operación de read** (vía `requiredPermissions="r"` en `@AgentOn`).
4. **El cliente puede gatear UI confiable** — el server respondió con la verdad de RBAC del user actual.

### 48.6.2 Anti-patterns a evitar

**ANTI-PATTERN 1: Hard-codear roles en cliente**:
```javascript
// ❌ MAL
if (currentUser.role === 'admin') {
  showAckButton();
}
```
Problema: el cliente decide qué es "admin", el server puede no estar de acuerdo. Las roles del user pueden cambiar sin que el cliente se entere.

**ANTI-PATTERN 2: Asumir slot presence = full access**:
```javascript
// ❌ MAL
if (comp.has('emergencyOverride')) {
  showOverrideButton();
}
```
Problema: el slot puede estar visible (READ ok) pero el user no tener INVOKE. El click del botón fallaría con 403.

**ANTI-PATTERN 3: Detect-by-failure como gating primario**:
```javascript
// ❌ MAL como gating
comp.invoke('emergencyOverride').catch(function(err) {
  hideOverrideButton();  // demasiado tarde — ya se invocó
});
```
Side effect ya pasó. Para safety, NO usar.

### 48.6.3 Cuántos canX() exponer

**Recomendación práctica**: uno por cada acción privilegiada del UI. Para una SPA típica BMS:

```java
// nmodsreflow/-rt: BReflow*Commands
canAcknowledgeAlarms()       // alarm ack/clear
canModifySchedules()         // schedule editing
canOverrideSetpoints()       // priority array writes
canEditUsers()               // user/role management UI
canViewAuditLog()            // audit log access
canExportHistory()           // history download
canConfigureBuilding()       // building/equipment config
```

**Costo**: cada `canX()` es un round-trip BOX al cargar la SPA. Optimización: agruparlos en un solo `getUserCapabilities()` que retorna BBoolean[] o JSON. **Trade-off**: granularidad vs latency.

---

## 48.7 Reflow profiles pattern: gating por role-name client-side

### 48.7.1 La arquitectura Reflow

**CONFIRMADO** (`reflow-frontend/src/store/modules/profiles.js` + `BReflowUserCommands.java`):

Reflow NO usa BPermissions de Niagara para gating UI. Implementa un layer custom de "profiles" basado en role names matching:

```
1. Login → server expone roles del user vía BReflowUserCommands.getRoles()
   → retorna BString CSV: "admin,operator,floor1-supervisor"
2. SPA cachea el array de roles en Vuex (state.user.roles)
3. SPA mantiene una collection de "profiles" en ^reflow/config.json:
   profiles: [
     { id: "admin-profile", users: ["admin"], roles: ["admin"], restrictions: {...} },
     { id: "operator-profile", roles: ["operator"], restrictions: {...} },
     { id: "default-profile", restrictions: {...} }  // fallback
   ]
4. Para cada link del nav, llamar getters.authorizeLink({link, username, roles})
   → engine de 7 paths que matchea el link contra profile.restrictions
   → retorna true/false
```

### 48.7.2 El engine `authorizeLink` — 7 paths

**CONFIRMADO** (`profiles.js:209-380`, ~62 líneas):

```javascript
authorizeLink: function (state, getters, rootState, rootGetters) {
  return function (opts) {
    var link = opts.link;
    var username = opts.username;
    var roles = opts.roles;
    
    // Resolve profile (by username, then by role, then default)
    var p = getters.getProfileForUser(username, roles)
            || getters.activeProfile;
    
    var clean = link.startsWith('/') ? link.substring(1) : link;
    var path = clean.split('?')[0];
    
    // Path 1: schedules/group/  ──┐
    // Path 2: pages/             ──┤
    // Path 3a: histories/        ──┤
    // Path 3b: embed schedule    ──┤  (cada uno tiene lógica específica)
    // Path 3c: buildings sub     ──┤
    // Path 3d: equipment/types   ──┤
    // Path 3e: catch-all         ──┘
    
    // restrictNewContent dual mode:
    //   true  = whitelist (incluye solo lo que está en routes)
    //   false = blacklist (excluye lo que está en routes)
    
    return matched;
  };
}
```

### 48.7.3 Profile resolution: by-user → by-role → default

**CONFIRMADO** (`profiles.js:170-205`):

```javascript
getProfileForUser: function (state) {
  return function (username, roles) {
    // 1. Match por username explícito
    var userMatch = state.items.find(function (profile) {
      return profile.users.map(toLowerCase).includes(username.toLowerCase());
    });
    
    if (userMatch) return userMatch;
    
    // 2. Match por role (case-insensitive, trimmed)
    if (roles) {
      var roleArr = Array.isArray(roles) ? roles : [roles];
      var roleMatch = state.items.find(function (profile) {
        return profile.roles.some(function (pRole) {
          return roleArr.some(function (r) {
            return r.trim().toLowerCase() === pRole.trim().toLowerCase();
          });
        });
      });
      if (roleMatch) return roleMatch;
    }
    
    // 3. Default profile
    return state.items.find(function (p) { return p.id === 'default-profile'; });
  };
}
```

### 48.7.4 Pros y contras del patrón Reflow

| Pro | Contra |
|-----|--------|
| Independiente del modelo Niagara — funciona aunque BCategoryService cambie | NO usa BPermissions — duplica el modelo de gating |
| Permite gating granular sobre conceptos custom (pages, schedules, buildings) que NO existen en Niagara | El server NO valida — atacante con DevTools puede ver cualquier ruta |
| Configurable sin recompilar — los profiles viven en `^reflow/config.json` | Mantenimiento de profiles paralelo al RBAC server — drift garantizado |
| UX consistente — gating se ve igual en todos los nodos custom | Falsa sensación de seguridad — gating es client-only |
| Bypass del gating requiere DevTools — fricción mínima para users normales | Cualquier user puede invocar BOX directamente y obtener data restricted |

**GOTCHA G48-4 — Reflow profiles NO sustituyen BPermissions Niagara**: el patrón profiles es UX gating, NO security. Para PROTECCIÓN real de datos, el módulo `-rt` debe enforce RBAC server-side via `@AgentOn(requiredPermissions=...)` o `cx.getUser().getPermissionsFor(comp).hasX()` checks en cada handler. Reflow hace esto en algunos handlers (canAcknowledgeAlarms) pero NO en todos. Audit pendiente.

---

## 48.8 LEVEL_1 Life Safety: el caso edge sin enforcement automático

### 48.8.1 Recap del problema (Bloque 46.G7)

**CONFIRMADO** (Bloque 46.743, Bloque 46.G7):

> Por convención de safety en BAS, **LEVEL_1 (Manual Life Safety) DEBERÍA estar restringido a roles de supervisor de seguridad**. Niagara NO enforza esto automáticamente — es responsabilidad del commissioning configurar los BPermissions correctamente. Si la SPA expone un botón "Emergency Override" accesible a todos los operadores, hay un riesgo de seguridad real.

**El problema concreto**:
- `BWritablePoint.emergencyOverride()` es una action que escribe en LEVEL_1 (priority array)
- Niagara enforces `OPERATOR_INVOKE` o `ADMIN_INVOKE` para invocar la action
- Pero NO diferencia "puede invocar emergencyOverride" de "puede invocar set" — ambos son `INVOKE` plain
- Configurar permisos diferenciados por priority level requiere categorías custom + commissioning fino — algo que pocos sites hacen

### 48.8.2 Implicaciones para la SPA

```javascript
// Lo que Niagara permite por default (TODOS los operators):
comp.invoke('emergencyOverride', value);  // LEVEL_1, manual life safety

// Lo que Niagara permite por default (TODOS los operators):
comp.invoke('set', value);  // LEVEL_16, valor default

// Niagara los ve EQUIVALENTES desde el RBAC point of view (ambos requieren INVOKE).
```

**Si la SPA expone un botón "Emergency" sin gating extra**, cualquier operador puede dispararlo. El server NO va a rechazarlo basado en el priority level — solo en el INVOKE permission, que el operator tiene.

### 48.8.3 Patrón mitigación recomendado

**Opción A: gating client-side estricto + audit log**:
```javascript
function showEmergencyButton(currentUser) {
  // Hard-coded: solo profiles 'safety-supervisor' ven el botón
  if (currentUser.profile === 'safety-supervisor') {
    renderEmergencyButton(function onClick() {
      auditLog('emergencyOverride', currentUser.username);
      comp.invoke('emergencyOverride', value);
    });
  }
}
```
Mitigación parcial: oculta el botón para users normales. Pero un atacante puede saltarlo via console del browser.

**Opción B: server-side category-based gating (correcto)**:
```
1. Crear BCategory "EmergencyOverride" en BCategoryService
2. Asignar la categoría solo a slots/actions de LEVEL_1 (requiere commissioning)
3. Crear BRole "safety-supervisor" con OPERATOR_INVOKE solo sobre categoría "EmergencyOverride"
4. Asignar role solo a users autorizados
5. Server enforces automáticamente — operator normal recibe PermissionException al invocar emergencyOverride
```
Mitigación real. Requiere commissioning del proyecto.

**Opción C: módulo `-rt` custom con check explícito**:
```java
public class BReflowSafetyCommands extends BComponent implements BIServerSideCallHandler {
    public BValue invokeEmergencyOverride(BComponent comp, BValue arg, Context cx) {
        BUser user = cx.getUser();
        if (!user.getRoles().contains("safety-supervisor")) {
            throw new PermissionException("emergencyOverride requires safety-supervisor role");
        }
        // proceed with the actual override
    }
}
```
Mitigación real, programática. La SPA invoca este wrapper en lugar de `comp.invoke('emergencyOverride')` directo. El server enforces.

**Recomendación**: Opción B si el commissioning lo permite (cleanest), Opción C como fallback si no se puede tocar el modelo Niagara.

---

## 48.9 Riesgo "client-side gating": server is truth, siempre

### 48.9.1 La regla de oro

**Cualquier gating que ocurra solo en el cliente JavaScript NO es seguridad — es UX.**

Un atacante con acceso al browser (incluso solo DevTools) puede:
- Modificar el state Vuex/Redux directamente (`store.state.user.roles = ['admin']`)
- Llamar BOX commands desde la consola del browser
- Hacer fetch directo a endpoints del módulo `-rt`
- Cambiar URLs / rutas del SPA router
- Inyectar handlers que skip los `authorizeLink` checks

**Todo lo que la SPA permite ver/hacer client-side, el server debe poder rechazar server-side**.

### 48.9.2 Las dos capas de defensa

```
┌─────────────────────────────────────────────┐
│  LAYER 1: Client-side gating (UX)          │
│  - Hide buttons user can't use             │
│  - Disable inputs user can't write         │
│  - Filter nav tree to hide forbidden paths │
│  - Improves UX, NOT security               │
└─────────────────────────────────────────────┘
                  ↓
┌─────────────────────────────────────────────┐
│  LAYER 2: Server-side enforcement          │
│  - BPermissions check on every read        │
│  - BPermissions check on every write       │
│  - PermissionException on unauthorized op  │
│  - This is the SECURITY layer              │
└─────────────────────────────────────────────┘
```

Layer 1 es opcional. Layer 2 es obligatoria. **Si layer 2 está rota, layer 1 no salva nada**.

### 48.9.3 Cómo verificar que layer 2 está intacta

Auditoría empírica del módulo `-rt`:

```bash
# 1. Buscar todos los servlet handlers + BOX commands
grep -rn "@AgentOn\|getPermissions\|hasOperatorWrite\|hasAdminWrite" -- src/

# 2. Para cada handler crítico, verificar que tiene check explícito o requiredPermissions
# Ejemplo bueno:
@AgentOn(types={...}, requiredPermissions="W")  // admin write required
public class BReflowAdminCommands ...

# Ejemplo malo:
@AgentOn(types={...}, requiredPermissions="r")  // solo read — pero el handler hace writes
public class BSomeCommands {
    public BValue dangerousOp(BComponent comp, BValue arg, Context cx) {
        // NO hay check de hasOperatorWrite() — vulnerable
        applyDangerousChange();
    }
}
```

**GOTCHA G48-5 — `@AgentOn(requiredPermissions="r")` sobre operación que escribe**: el `requiredPermissions` declara qué necesita el AGENT (permission para llamar al método). Si el método después HACE writes (modifica components, ejecuta actions), el `requiredPermissions="r"` NO los cubre — el método debe checkear adicionalmente `cx.getUser().getPermissionsFor(target).hasOperatorWrite()`. Múltiples handlers Reflow tienen este pattern — audit pendiente.

### 48.9.4 Atacante razonable: lo que puede hacer trivialmente

```javascript
// En la consola del browser de la SPA Reflow autenticada:

// 1. Ver TODOS los profiles, ignorando gating
require(['baja!'], function(baja) {
  baja.Ord.make('service:nmodsreflow:ReflowService').get()
    .then(function(svc) { return svc.serverSideCall('getAllProfiles'); })
    .then(console.log);  // ← retorna data aunque la UI lo oculte
});

// 2. Invocar acción que la UI esconde
comp.invoke('emergencyOverride', 100);  // si server no checkea, se ejecuta

// 3. Saltar el authorizeLink — navegar directo a una ruta restricted
window.location.href = '/admin/configuration';
// si el componente AdminConfiguration NO checkea permission al montar, queda accesible

// 4. Modificar state Vuex en vivo
store.state.profiles.activeProfile = 'admin-profile';
// la UI se reactualiza creyendo que sos admin
```

Conclusión: el modelo de seguridad debe asumir que la SPA es código del atacante. **El servidor debe defenderse**.

---

## 48.10 Patrones recomendados (production-ready)

### 48.10.1 Stack completo de gating UI

```
┌────────────────────────────────────────────┐
│ 1. SLOT PRUNING (automatic, server-side)  │
│    Server omits slots user can't READ      │
│    SPA: comp.has(slotName) check           │
└────────────────────────────────────────────┘
            +
┌────────────────────────────────────────────┐
│ 2. FLAGS (HIDDEN/READONLY)                 │
│    SPA: respect flags + flagsChanged       │
│    Hides slots that ARE present but tagged │
└────────────────────────────────────────────┘
            +
┌────────────────────────────────────────────┐
│ 3. canX() SERVER-SIDE CALLS                │
│    Server: BBoolean.make(perms.hasX())     │
│    SPA: serverSideCall + gate UI on result │
└────────────────────────────────────────────┘
            +
┌────────────────────────────────────────────┐
│ 4. ROLES OBTAINED FROM SERVER              │
│    Server: BReflowUserCommands.getRoles()  │
│    SPA: cache roles, use for profile match │
└────────────────────────────────────────────┘
            +
┌────────────────────────────────────────────┐
│ 5. SERVER-SIDE ENFORCEMENT (mandatory)     │
│    Module -rt: hasOperatorWrite() checks   │
│    Server: PermissionException if denied   │
│    THIS IS THE ACTUAL SECURITY LAYER       │
└────────────────────────────────────────────┘
```

Capas 1-4 son UX. Capa 5 es seguridad. **NUNCA omitir capa 5**.

### 48.10.2 Ejemplo composición Vue

```javascript
// composables/useCapability.js
import { ref, onMounted } from 'vue';

export function useCapability(serviceName, methodName) {
  var allowed = ref(false);
  
  onMounted(async () => {
    const baja = await import('baja!');
    const svc = await baja.Ord.make('service:' + serviceName).get();
    const result = await svc.serverSideCall(methodName);
    allowed.value = result.encodeToString() === 'true';
  });
  
  return { allowed };
}

// AlarmList.vue
<template>
  <button v-if="canAck" @click="ackSelected">Acknowledge</button>
</template>
<script setup>
import { useCapability } from '@/composables/useCapability';
const { allowed: canAck } = useCapability('nmodsreflow:ReflowService', 'canAcknowledgeAlarms');
</script>
```

### 48.10.3 Patrón "permission-aware widget"

```javascript
// PointWidget.vue — wrapper que se gatea solo
<template>
  <div v-if="canRead">
    <span>{{ display }}</span>
    <input v-if="canWrite && !readonly" v-model="value" @blur="commit" />
    <button v-if="canInvoke" @click="invokeAction">{{ actionLabel }}</button>
  </div>
</template>
<script>
export default {
  computed: {
    canRead() { return this.comp && this.comp.has(this.slotName); },
    canWrite() { return this.canRead && !this.flags.READONLY; },
    canInvoke() { return this.actionAllowed; },  // viene de canX() server call
    readonly() { return this.flags.READONLY; }
  },
  mounted() {
    this.sub = new baja.Subscriber();
    this.sub.attach({
      flagsChanged: (slot) => { this.flags = parseFlags(slot.getFlags()); },
      changed: (prop) => { this.value = prop.getValue(); }
    });
    this.sub.subscribe(this.comp);
  },
  beforeDestroy() {
    this.sub.detach();
    this.sub.unsubscribe();
  }
};
</script>
```

---

## 48.11 Gotchas transversales

**G48-1 — Detect-by-failure es post-acción**: usar `comp.invoke(...).catch(PermissionException)` como gating descubre la denegación DESPUÉS de que el request salió. Si la operación tiene side effects (audit log, alarm trigger), el daño puede estar hecho. NO usar como gating primario.

**G48-2 — Flags vs permissions son cosas distintas**: HIDDEN/READONLY son metadata del SLOT (igual para todos los users). Slot pruning es per-user (basado en BPermissions). No confundir — usar AMBOS en combinación.

**G48-3 — Custom nav requiere re-implementar todo el gating**: si la SPA descarta el nav tree de Niagara, pierde slot pruning automático. Tiene que re-implementar el gating. Reflow lo hace con `authorizeLink` pero sin enforcement server.

**G48-4 — Reflow profiles NO sustituyen BPermissions**: el patrón profiles es UX gating, NO security. El módulo `-rt` debe enforce RBAC en cada handler.

**G48-5 — `@AgentOn(requiredPermissions="r")` sobre operación que escribe**: `requiredPermissions` cubre solo la INVOCACIÓN del agente. Si el handler hace writes internamente, debe checkear `hasOperatorWrite()` adicionalmente. Audit pendiente en Reflow handlers.

**G48-6 — `comp.has(slotName)` retorna `undefined` para slots prunidos**: defensive coding obligatorio. NO asumir que un slot esperado siempre está. Wrap accesos con `if (comp.has(...))` o try/catch.

**G48-7 — La API JS NO expone `comp.canRead/canWrite/canInvoke`**: hay que usar slot pruning + canX() server calls + role-based gating. El gap NO es bug, es decisión de diseño Niagara (modelo categoría-céntrico que no se traslada bien a cliente JS).

**G48-8 — LEVEL_1 sin RBAC diferenciado per-priority**: emergencyOverride requiere mismo `INVOKE` que `set`. Niagara no enforces "solo safety-supervisor puede LEVEL_1". Mitigación: BCategory custom + commissioning, o module `-rt` wrapper con check programático.

**G48-9 — `flagsChanged` puede no propagarse si user re-asigna roles en vivo**: no validado empíricamente. Cambio de roles probablemente requiere logout + login para verse efectivamente. INFERIDO. TODO 48-1.

**G48-10 — `getRoles()` retorna CSV string, no array**: el patrón Reflow `BReflowUserCommands.getRoles` devuelve `BString.make(user.getRoles())` — CSV. Cliente debe `.split(',').map(s => s.trim())` para usar como array.

**G48-11 — Detect-by-failure trae 403 con body lexicon-ized**: cuando catch `PermissionException`, el body del error es localizado (`error.permission.denied` o similar). NO confiar en match string-based — usar status code 403.

**G48-12 — Vuex state con `roles[]` es atacable trivialmente**: `store.state.user.roles` es modificable desde DevTools. NO usar como única fuente de verdad para gating. Server siempre debe re-checkear.

**G48-13 — `componentFlagsChanged` no incluye details del cambio**: el callback recibe solo `cx`, no qué flag cambió ni cuál es el valor nuevo. La SPA debe re-leer `comp.getFlags()` después del callback.

---

## 48.12 TODOs y validaciones pendientes

**TODO 48-1**: Validar empíricamente si el server propaga `componentFlagsChanged` o `flagsChanged` cuando un admin re-asigna roles a un user con sesión activa. INFERIDO que NO — probablemente requiere logout + login para que el user vea cambios. Verificar con dual-session test.

**TODO 48-2** ✅ RESOLVED 2026-05-07 (engram topic_key `niagara-mental-model/bloque48-todo2-resolved` + commit reflow-clean-177 `719e341`): Audit empírico ejecutado contra los 7 handlers `@AgentOn(requiredPermissions="r")` en `nmodsreflow-rt` (`BReflow{Alarm,BQL,CSV,File,History,Nav,User}Commands.java`, 24 métodos BOX totales). **Formulación original REFUTADA**: 0 writes en los 24 métodos — todos son reads por design. Los writes reales (ack alarmas, set point, modify config) van por canales separados (BajaScript directo a `BAlarmService`, REST POST con `CsrfGuard.validate()`, `comp.set(value, priority)` directo) donde Niagara native RBAC enforcement aplica en el target component, no en el wrapper handler. `hasOperatorWrite()` aparece UNA SOLA vez en todo `-rt` (`BReflowAlarmCommands:111` `canAcknowledgeAlarms`) y solo como gating UI ("decile al cliente qué botón mostrar"), no como enforcement — consistente con el modelo 5-layer del 48.10.1. **Hallazgo lateral real** (más sutil que el TODO original): 6 handlers reciben `Context cx` y lo descartan en resolves de ORD — `BReflowBQLCommands:92` usaba `ord.get(null)` explícito (FIXED a `ord.get(cx)`); 5 sitios restantes usan `ord.get()` no-arg (BReflowFileCommands:86,127 + BReflowCSVCommands:79 + BReflowNavCommands:67,96) que dependen de ThreadLocal Context fallback (Bloque 38.314 + 38.G8 leak vector) — deferred como defensive coding follow-up. **TODO reformulado para futuras integraciones**: la pregunta correcta no es "¿hay writes sin `hasOperatorWrite` check?" sino "¿hay resolves que dependen implícitamente del ThreadLocal Context en lugar de pasar `cx` explícito?".

**TODO 48-3**: Verificar si bajaScript browser expone alguna API para enumerate categories del user actual. Búsqueda preliminar en `bs.built.min.js` no encontró `categoryService` ni `getUserCategories`. INFERIDO ausente.

**TODO 48-4**: Investigar si `BCategoryService.resolveIndices(mask)` está expuesto vía algún BOX endpoint genérico. Si lo está, la SPA podría reconstruir el mapping cliente-side (con costo de seguridad — exponer el modelo de permisos completo).

**TODO 48-5**: Validar el patrón Reflow `getAllRoles()` — actualmente retorna `BRoleService.getEnabledRoleIds()` como CSV. ¿Cualquier user puede invocarlo (`requiredPermissions="r"`) y obtener la lista completa de roles del station? Si sí, es information disclosure (atacante mapea el modelo de roles).

**TODO 48-6**: Documentar el flujo de actualización de roles cuando un admin modifica BUserService en vivo. ¿Las sesiones activas se invalidan? ¿El user ve el cambio inmediatamente o solo en próximo login? Niagara docs no son explícitas.

**TODO 48-7**: Implementar y testear el patrón "Opción C" del 48.8.3 — module `-rt` wrapper para emergencyOverride con check explícito de `safety-supervisor` role. Cuantificar overhead vs seguridad.

---

## 48.13 Próximos pasos

### Para implementar gating UI en SPA externa Niagara (orden recomendado)

1. **Identificar acciones privilegiadas**: listar todas las acciones del UI que requieren permisos (writes, invokes, navigation a routes admin).

2. **Implementar `BReflowUserCommands.getRoles` equivalente** (o reusar el de Reflow): expone los roles del user actual via BOX serverSideCall.

3. **Implementar canX() server-side calls** para cada acción privilegiada — patrón canónico Bloque 48.6.1.

4. **Wire useCapability composable** (Vue) o equivalente (React/vanilla): cachea results de canX() y los expone reactivos a los componentes.

5. **Subscriber con flagsChanged + facetsChanged** en cada widget — actualiza visibility/readonly cuando server cambia metadata.

6. **Defensive coding en accesos slot**: SIEMPRE `comp.has(slotName)` antes de `comp.get(slotName)`.

7. **Audit del módulo `-rt`**: verificar que CADA handler con write/invoke check `hasOperatorWrite()` o `hasOperatorInvoke()` server-side. NO confiar solo en `@AgentOn(requiredPermissions="...")`.

8. **Si LEVEL_1 está expuesto**: implementar gating con BCategory custom (ideal) o module `-rt` wrapper (workaround).

9. **NUNCA usar `if (currentUser.role === 'admin')`**: usar canX() calls que el server evalúa con `cx.getUser().getPermissionsFor(...).hasAdminWrite()`.

10. **Documentar en código**: cada gate UI debe tener comment indicando QUÉ canX() o QUÉ flag está checkeando, y dónde está el enforcement server-side correspondiente. Drift entre cliente y server es la fuente más común de bugs RBAC.

### Para investigación futura

- Validar TODOs 48-1..48-7
- Comparar overhead de N canX() calls vs un solo getUserCapabilities() agregado
- Investigar si `BCategoryService` puede exponer un read-only endpoint para que SPA reconstruya el mapping (trade-off seguridad)

---

## Fuentes y referencias cruzadas

| Afirmación | Fuente empírica | Bloque ref |
|------------|-----------------|------------|
| BPermissions = 6 bits "rwiRWI" | `BPermissions.class` decompilado | Bloque 11.1.4 |
| BCategoryService = 64-bit bitmap, max 64 categorías | `BCategoryService.class` | Bloque 11.1.3 |
| Slot pruning automático server-side | Bloque 22.1140 + bug history NCCB-632 | Bloque 22.1140 + 35.478 |
| `cx.getUser().getPermissionsFor(comp).hasOperatorWrite()` patrón canónico | `BReflowAlarmCommands.canAcknowledgeAlarms` decompilado | Bloque 44.468 + 51.180 |
| `BReflowUserCommands.getRoles()` retorna BString CSV | `nmodsreflow-rt/.../commands/BReflowUserCommands.java:50-52` | — |
| `@AgentOn(requiredPermissions="r")` cubre solo invocación del agent | Annotation decompilada `BReflowUserCommands.java:31` | — |
| bajaScript NO expone canRead/canWrite/canInvoke directo | grep en `bs.built.min.js` (360 KB) — 0 matches | — |
| `BSpace.canRead/canWrite/canInvoke(OrdTarget)` SÍ existe server-side | Bloque 38.295-303 | Bloque 38 |
| Subscriber events: flagsChanged, facetsChanged, componentFlagsChanged | `Component.js` lifecycle (Bloque 22.808) | Bloque 22.808 + 42.119-189 |
| Nav tree filtering por permissions | Bloque 35.478 + Bloque 35.1299 | Bloque 35.478 |
| Reflow `getProfileForUser` username → roles → default | `profiles.js:170-205` decompiled bundle line 9030-9092 | — |
| Reflow `authorizeLink` 7-path engine + restrictNewContent dual mode | `profiles.js:209-380` (62 líneas) | — |
| LEVEL_1 sin RBAC enforcement automático per-priority | Bloque 46.G7 + Bloque 46.743 | Bloque 46.G7 |
| HxPx Binding bugs históricos NCCB-632/643/758/16259 | Bloque 22.1145-1148 | Bloque 22 |
| Display permissions NO atributo password nativo PX | Bloque 22.1158-1160 | Bloque 22 |
| `@AgentOn(types=..., requiredPermissions="r")` annotation pattern | `BReflowAlarmCommands.java` + `BReflowUserCommands.java` | — |
| `BFlags`: HIDDEN (0x40000), READONLY (0x100), OPERATOR (0x100) | Bloque 4.564 + slot system | Bloque 4 |
| ORD resolution falla si no permissions client-side | Bloque 22.1155 "Client-side: ORD resolution falla si no permissions" | Bloque 22 |
| `BFormat.display(value, user, permissions)` oculta si no read perm | Bloque 22.1160 | Bloque 22 |
| `BAdminRole` super-user hardcoded otorga BPermissionsMap.SUPER_USER | Bloque 11.1.7 | Bloque 11 |
| Reflow store/modules/profiles.js + UserProfileRoles.vue + UserProfile.vue | grep empírico Reflow-Clean-177 | — |
