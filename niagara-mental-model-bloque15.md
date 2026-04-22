# Niagara N4 — Bloque 15: Workbench editing deep (wiresheet + property sheet + nav + point/device manager)

Parte del mental model. Ver [INDEX.md](INDEX.md). Relacionado con Bloque 9.1 (Workbench overview), Bloque 4 (Baja slots + facets), Bloque 7 (drivers), Bloque 14 (Batch Editor + templates).

Este bloque profundiza **el workflow diario del editor** de Workbench. Cuatro views consumidos en 95% del tiempo: wiresheet, property sheet, nav tree, point/device managers. Cada uno tiene arquitectura específica aunque todos consumen el mismo modelo (BComponent + slots + ORD).

---

## 15.1 Arquitectura del wiresheet editor

### 15.1.1 MVC: BWsCanvas + WsController + glyph hierarchy

Módulo: `wiresheet-wb.jar`. Clases clave (verificadas en corpus decompilado):

**Model** — `BComponent` tree. El root del wiresheet es el BComponent abierto (típicamente `BFolder`). Links (BLink) + relations (BRelation) son parte del model.

**View** — `com.tridium.wiresheet.BWsCanvas` (extends `BTransferWidget`). Renderiza via jerarquía de **Glyphs** (Composite pattern):
- `RootGlyph` — raíz, contiene 2 capas (`LayerGlyph`): componentes + links
- `ComponentGlyph` (abstract) — 1 por BComponent
- `StdComponentGlyph` (concrete) — render standard con title bar + slot bars + footer
- `LinkGlyph` (abstract) — BLink / BRelation
- `LinkSnakeGlyph` (concrete) — implementa manhattan routing (H/V segments)

Spatial indexing: `RectangleMap componentGlyphMap` (hit testing O(log n)), `PointMap scaleMap` (link crossings).

**Controller** — `com.tridium.wiresheet.WsController`:
- Referencia `BWireSheetPane ws`, `WsSelection selection`, `WsState state`
- HashMap interno: `byName` (componentes por slot name), `byHandle` (por handle persistente)
- Métodos: `load(BComponent)`, `getAllComponentGlyphs()`, `transition(WsState)`

### 15.1.2 Rendering de un BComponent (glyphs)

Cada BComponent se renderiza como `StdComponentGlyph` (caja rectangular):

```
┌─────────────────────┐
│ [icon] ComponentName │ ← TitleBarGlyph
├─────────────────────┤
│ in1  [•]            │ ← SlotBarGlyph (inputs)
│ in2  [•]            │
│      ┌───────────┐  │
│      │  Control  │  │ ← ActionBarGlyph
│      └───────────┘  │
│            [•] out  │ ← SlotBarGlyph (outputs)
│            [•] sts  │
├─────────────────────┤
│ value: 42.5         │ ← PropertyBarGlyph
│ status: ok          │ ← FooterBarGlyph
└─────────────────────┘
```

Children glyphs internos:
- `TitleBarGlyph` — ícono (tipo BComponent) + nombre (slot name en parent)
- `SlotBarGlyph[]` — inputs/outputs. `getPotentialSlots(isOutput)` itera Slots con propertyFlag IN o OUT
- `ActionBarGlyph` — acciones invocables si `getActions()` no vacía
- `PropertyBarGlyph` — valor actual del `out` slot (si es ControlPoint)
- `FooterBarGlyph` — indicador status desde `BComponent.getStatus()`

Rendering: `paint(Graphics)` lee tema via `Theme.wiresheet()`, fillRect background, `paintChildren()`, stroke outline, `paintSelection()` si seleccionado.

### 15.1.3 `BWsAnnotation` — layout persistence

Las posiciones x,y NO son parte del BComponent BOG nativo. Se persisten en anotación separada:

```java
public final class BWsAnnotation extends BSimple {
  public final int p;           // grid x (wixels)
  public final int q;           // grid y (wixels)
  public final int wixelWidth;
  public final int wixelHeight;
  // Encoded as string: "p,q,wixelWidth,wixelHeight"
}
```

**Scope**: per BComponent per folder (no global). Copy+paste a otra carpeta genera nueva anotación.

**Storage**: property frozen `wsAnnotation` en cada BComponent, serializada en config.bog:
```xml
<component name="NumPoint1" type="baja:NumericPoint">
  <property name="wsAnnotation" value="10,20,8,0"/>
  ...
</component>
```

**Default**: `BWsAnnotation.DEFAULT = new BWsAnnotation(0, 0, 8, 0)` — esquina superior izquierda, width 8 wixels, height auto.

**Grid**: `WixelGrid` encoda ocupación por celda (hit testing + rubber-band). `wixel` = 1 píxel pantalla default, configurable.

---

## 15.2 Drop de palette en wiresheet

### 15.2.1 Workflow step-by-step

1. User arrastra componente desde Palette sidebar (`.palette` ZIP con BOG embebido — Bloque 14.9)
2. Cursor preview thumbnail
3. Drop en (x,y):
   - `BWsCanvas.drop(TransferContext)` invocado
   - TransferEnvelope con datos serializados
   - Niagara deserializa BOG → instancia BComponent nuevo
   - Asigna slot name único (`Component1`, `Component2`... via `WsController.auto` counter)
   - `parent.add(slotName, component)`
   - `BWsAnnotation.make(p, q)` — coordenadas drop
   - `component.setProperty("wsAnnotation", annotation)`
   - Reload canvas → nueva `StdComponentGlyph` en (p,q)

### 15.2.2 Validaciones + palette format

Solo permite drop si parent es BContainer (BFolder / BEquipment). Tipos no permitidos rechazan drop.

**Palette format** (ZIP):
- `palette.xml` — metadata
- `components/*.bog` — BOG files comprimidos de cada componente template

Deserialization via `Sys.deserialize()` desde BOG.

**Copy/paste intra-folder** usa mismo mechanism con `PaletteTransfer` transferable interno. **Gotcha**: no auto-increment IP addresses o BACnet instance IDs (Bloque 14.14 — workaround BajaScript).

---

## 15.3 Link dragging (creating BLink via UI)

### 15.3.1 Input/Output pin validation

Workflow:
1. Click+hold en output pin (`SlotBarGlyph` OUT)
2. `WsController.transition(LinkState)` — state transition
3. `LinkState` renderiza wire flotante desde pin a cursor
4. User drag sobre otro componente:
   - `LinkState.checkLink(targetComponent, targetSlot)`:
     - `sourceType = sourceSlot.getType()`
     - `targetType = targetSlot.getType()`
     - `sourceType.isAssignableFrom(targetType)` (Bloque 6.2)
   - Color verde si válido, rojo si incompatible + mensaje error
5. Release → si válido, `BLink.make()` + `folder.add("LinkN", link)` en BOG

Esta validación es **type-aware** (Bloque 4.2 type hierarchy + coercion rules).

### 15.3.2 Manhattan routing — algoritmo

`LinkSnakeGlyph.buildTiles()`:
- Calcula source (sourceP, sourceQ) + target (targetP, targetQ)
- Ruta H→V→H (o patrón alternativo) evitando overlaps
- Cada segment = `LinkGlyph.Tile` (rectángulo 2-3px wide)

Reroute dinámico: al mover componente, `MoveGlyphsCommand` triggers `LinkGlyph.layout()` → recalcula ruta. Caching en `LinkSnakeGlyph.tiles` reutiliza si positions no cambiaron.

Color coding por status: VALID (green), FAULT (red), ALARM (yellow).

**No verificable**: soporte de bezier curves o straight lines. Default = manhattan.

---

## 15.4 Selección múltiple + grupos

### 15.4.1 Selection modes

`WsSelection`:
```java
public void select(Glyph g);
public void unselect(Glyph g);
public void selectAll();       // Ctrl+A
public void unselectAll();     // ESC
public Glyph[] get();
public ComponentGlyph[] getComponentGlyphs();
public BComponent[] getComponents();
```

Modos:
- **Single click** — selecciona, deselect rest
- **Ctrl+click** — toggle add/remove individual
- **Shift+click** — range select (inferido, no verificado bytecode)
- **Rubber-band drag** — `RubberBandState` renderiza rect + `getComponentGlyphsInBounds()` al release

Undo/redo: selección NO genera undo artifact. Move/delete/resize SÍ (`MoveGlyphsCommand.doInvoke()` con artifact + `artifact.undo()`).

### 15.4.2 Grupos — no existe clase explícita

**Hallazgo empírico**: el corpus NO contiene clase `BWsGroup`. La funcionalidad "grupos" es inferida:
- User crea BFolder nested, organiza componentes ahí
- UI collapse/expand es de navigator, no del wiresheet
- Menú "Arrange" permite align/distribute pero sin group container visual

---

## 15.5 Copy/paste + shortcuts de teclado

Implementation extends `BTransferWidget`:
- `doCopy()` → serializa selected components a clipboard BOG
- `doCut()` → copy + delete
- `doPaste()` → deserialize + auto-increment names + add to parent

Shortcuts (registrados en `WsCommands`):

| Shortcut | Acción |
|----------|--------|
| Ctrl+C | doCopy |
| Ctrl+X | doCut |
| Ctrl+V | doPaste |
| Ctrl+Z | undoManager.undo |
| Ctrl+Y | undoManager.redo |
| Delete | doDelete |
| Ctrl+A | selectAll |
| F2 | doRename |
| Arrow keys | move selected glyphs 1 wixel |
| Ctrl+Arrow | move fast (10 wixels) |
| Escape | unselectAll |

---

## 15.6 Performance + limitaciones wiresheet

- Redraw: `RootGlyph.paint()` O(n) componentes. 500+ componentes → 100-200ms en máquina standard.
- Spatial: `RectangleMap` con bucketing 20x20 → hit test O(log n) incluso con 1000s glyphs.
- Link routing: O(1) por link (no reroute global por move).
- Dirty region: `repaint()` en glyph específico, no canvas completo.
- Scroll memory: `BWireSheetPane.scrollMemory` HashMap persist scroll position per-folder on reopen.
- Canvas max: `WixelGrid` 1000x1000 wixels default = ~1000x1000 px.
- **NO zoom in/out nativo** en Workbench wiresheet (sí en web wiresheet ux).
- Relaciones: max 1 BRelation per BComponent pair (enforcement core Niagara).

---

## 15.7 State machine del wiresheet

`WsState` interface con 5 states:

```
NormalState (default)
  ├─ mousePressed on glyph → MoveState
  ├─ mousePressed on output pin → LinkState
  ├─ mouseDragged on canvas → RubberBandState
  └─ keyPressed → shortcuts (Ctrl+C, Delete, etc.)

MoveState
  ├─ mouseMoved → actualiza posiciones
  └─ mouseReleased → MoveGlyphsCommand artifact → NormalState

LinkState
  ├─ mouseMoved → wire flotante + validate
  ├─ mouseReleased → BLink.make si válido → NormalState
  └─ Escape → cancela → NormalState

RubberBandState
  ├─ mouseDragged → expand rect
  └─ mouseReleased → selecciona en bounds → NormalState

ResizeState
  ├─ mouseMoved → resize
  └─ mouseReleased → ResizeCommand artifact → NormalState
```

---

## 15.8 Property Sheet editor

### 15.8.1 Arquitectura — BWbComponentView + ComponentTableModel

`javax.baja.workbench.component.table.BWbComponentView` extends `BWbView` extends `BWbEditor`. Módulo: `workbench-wb.jar`.

MVC:
- Model: `BComponent` con todos sus slots
- View: tabla renderizada (HTML5 o AX legacy según profile)
- Controller: ciclo `BWbEditor.loadValue()` / `saveValue()`

Clases clave:
- `ComponentTableModel` — especialización TableModel, columnas configurables tipo `PropertyColumn`
- `PropertyColumn` — une tabla a una `Property` específica, extrae valor via `getValue(BComponent)`
- Registra auto para `BComponent.type` events via `registerForComponentEvents()`

### 15.8.2 Rendering de slots

Columnas estándar:
- **Name** — displayName o slot name
- **Value** — editor interactivo (texto, spinner, dropdown, etc.)
- **Type** — BString, BDouble, BBoolean, BEnumRange
- **Flags** — bitmask visual (r=READONLY, t=TRANSIENT, h=HIDDEN, s=SUMMARY, o=OPERATOR per Bloque 4.1.2)
- **Facets** — metadata (editable via popup Edit Facets)

Filtering:
- Show only OPERATOR visible
- Hide HIDDEN (toggle)
- Show only modified (dirty tracking)

Agrupación: category groups por facet "category" (ej. "Network", "Timing", "Advanced") vs flat list.

### 15.8.3 FieldEditor resolution — facets driven rendering

Resolución per slot:

1. **Facet `fieldEditor`** check → custom editor class FQ name (ej. `com.tridium.ColorFieldEditor`)
2. **Default agent registration**: `@AgentOn(types="baja:String")` → `BStringFE`, `"baja:Boolean"` → `BBooleanFE`
3. **Fallback**: genérico `BWbFieldEditor` si no registration

Facets driven customization (Bloque 4.3.2):
- `BFacet("min", 0.0) + BFacet("max", 100.0)` → spinner con range
- `BFacet("units", "degF")` → label unit al lado
- `BFacet("precision", 2)` → format 2 decimales
- `BFacet("trueText", "ON") + BFacet("falseText", "OFF")` → boolean custom labels
- `BFacet("range", BEnumRange)` → dropdown con enum values
- `BFacet("multiLine", true)` → BStringFE abre textarea

FieldEditor implementations verificadas:
- `BStringFE` — BTextField o textarea
- `BNumberFE` — spinner
- `BBooleanFE` — checkbox con custom labels
- `BEnumFE` / `BDropDownFE` — combobox
- `BOrdFE` — text + browse button para component select

Registry: `BWbProfile` mantiene agentes. Lookup lazy (primera use caches).

### 15.8.4 Edit flow — dirty → validate → save atomic

1. **Load**: `BWbEditor.loadValue(BComponent, Context)` → crea FieldEditor, carga valor
2. **Edit**: user modifica widget. No persiste todavía — UI state only.
3. **Parse + validate**: Enter/Tab → `parseValue(string)` → target type (parse error → rollback). Facet validation (min/max, enum allowed, BIValidator). Invalid → tooltip error, no advance.
4. **Dirty flag**: Workbench marca component dirty (UI asterisk o rojo). NO committed a BOG aún.
5. **Save explícito** (Ctrl+S / Save button):
   - `BWbEditor.saveValue(BComponent, Context)` → batch `BComponent.set(Property, value, Context)` per dirty slot
   - Triggers `changed()` callbacks
   - BOG atomic write (Bloque 5.2.3)
6. **Revert** (Ctrl+Z o Revert): discard dirty sin commit → UI vuelve a last-saved

`CannotSaveException` si validation falla post-parse.

### 15.8.5 Flags + actions + topics rendering

Además de properties:
- **Flags**: columna visual chars + right-click "Config Flags" dialog
- **Actions**: row por action, button invokable. Si tiene param, abre Form dialog → submit → `BComponent.invoke(Action, param, Context)`
- **Topics**: row read-only (events no editables)

Composite management: right-click "Composite" → dialog Edit Composite — toggle visibility de slots individuales (persistent per-station).

---

## 15.9 Nav tree (navigator)

### 15.9.1 Arquitectura

`javax.baja.workbench.nav.tree.BNavTree` — BTree especializado. Módulo `workbench-wb.jar`.

Interface `javax.baja.nav.BINavNode`:
- `getNavDisplayName(Context)` — label
- `getNavIcon()` — BIcon
- `getNavChildren()` — array lazy-load
- `hasNavChildren()` — fast check
- `getNavName()` — id único bajo parent
- `getNavOrd()` — BOrd resolver a componente

Clases:
- `NavTreeNode` — wrapper TreeNode encapsula BINavNode, children lazy (load on expand)
- `NavTreeModel` + `DefaultNavTreeModel` — TreeModel; `lookup(BOrd)` → fast ORD→treenode map
- FireNavEvent (renamed/added/removed/reordered/replaced) → modelo actualiza
- `NavTreeController` — mouse/keyboard, double-click expand, right-click popup

### 15.9.2 Scopes

Root `BNavRoot.INSTANCE` (singleton) monta hosts + namespaces:
- `local:|station:` — station local con Drivers, Services, Config, etc.
- `local:|file:` — filesystem server
- `local:|module:` — módulos instalados con palette types
- `fox:` — stations remotas via Fox protocol

Cada scope tiene BINavNode raíz especializada. Expansión triggers lazy load.

### 15.9.3 Drag-from-nav (ref vs copy)

- Default (no modifier): crea **BOrd reference** (symbolic link). Component original permanece, wiresheet renderiza via ORD resolution.
- Con **Ctrl**: **copy** (clone subtree). Nuevo component con nombres únicos bajo target parent.

`BNavTree.getTransferData()` → `TransferEnvelope`; `drop(TransferContext)` → `insertTransferData()` vs `removeTransferData()` (move).

### 15.9.4 Search + filter

Top toolbar search box:
- Pattern → filter nodes por navDisplayName (case-insensitive substring)
- Wildcards (* = 0+ chars) soportados
- Clear button revela árbol completo

Implementación: `DefaultNavTreeModel` filtra children on-demand. Filter state transient (no persiste).

### 15.9.5 Fox remote stations

Workflow:
1. Click "Add Host" → dialog host/port/credentials
2. Workbench crea `BFoxProxySession` (reference counting + "interest" multi-user)
3. Node `fox:<hostname>` aparece en tree
4. Expand → `BFoxProxySession.rpc(BOrd, methodName, args)` async RPC
5. Credentials cached en `user/security/credentials/credentials.xml` (encriptadas AES)

Loading spinner durante fetch remote tree. No bloquea UI.

---

## 15.10 Point Manager view

### 15.10.1 Arquitectura + scope

`BPointManager` extends `BFolderManager` (módulo `driver-wb.jar`, package `javax.baja.driver.ui.point`). `getTargetType()` = `BControlPoint.TYPE` — solo muestra subclases de BControlPoint.

Internas: `PointModel` (tabla data), `PointController` (selección + events), `PointState` (filtros persist).

Scope: **N BControlPoints bajo un subtree** (típicamente device con proxy points o BPointFolder).

### 15.10.2 Columns + sort + filter

Columnas:
- **Out / Current Value** — valor + status visual
- **Type** — NumericPoint, BooleanPoint, EnumPoint, StringPoint (+ Writable variants)
- **Facets** — units, range, precision summary
- **Extensions** — iconos HistoryExt, AlarmSourceExt, ProxyExt
- **Last Update** — timestamp último cambio
- **Status** — ok/alarm/fault/offline
- **Display Name** — nombre UI

Sort por cualquier columna. Típico: por Status (alarms first), Last Update, Type.

Filter:
- Por status (show only alarms/faults/offline)
- Por extensions (show only with history/alarms)
- Por recent activity (timestamp > N min)
- Por facets (units = "°C")
- Búsqueda textual

### 15.10.3 Bulk operations

Right-click context menu con multi-select:
1. **Add Extensions** — aplicar HistoryExt/AlarmSourceExt/custom a N puntos. Dialog params, atomic apply
2. **Configure Alarms** — threshold, priority 0-255, severity 1-5, alarm class
3. **Bulk enable/disable polling**
4. **Copy/move to folder**
5. **Delete** bulk
6. **Refresh/resync** — re-read valores remotos

**Diferencia con Batch Editor (Bloque 14.11)**: Point Manager acciones directas + inmediatas (sin license gate extra); Batch Editor es tabular + stage-based con rollback limitado + license `provisioning` gate.

Uso típico:
- Point Manager: comisionamiento inicial
- Batch Editor: updates masivos post-deployment

---

## 15.11 Device Manager view

### 15.11.1 Arquitectura

`BDeviceManager` extends `BFolderManager` (driver-wb.jar, package `javax.baja.driver.ui.device`). Scope: todos los `BDevice` en un `BDeviceNetwork`.

Pattern: Network → Device Manager view → **Database pane** (devices configurados) + **Discovered pane** (devices descubiertos pending add).

`supportsTemplates()=true` → habilita **DevTemplateMgr tab** con template matching/deployment (Bloque 14.12 Template/Match/Bind).

Internas: `DeviceModel`, `DeviceController`, `DeviceState`.

### 15.11.2 Discovery workflow

1. **Iniciar**: click "Discover"
   - Dialog params: IP range, port (BACnet 47808 default), timeout (30-60s IP, 2-5 min MSTP)
2. **Job asíncrono**:
   - BACnet: I-Am broadcast + get-device-list
   - LON: domain learn + address table
   - Modbus: TCP connect/scan
   - OPC UA: browse address space
3. **Resultado en Discovered pane**: tabla con device name, vendor, model, network address, type
4. **Selection + matching**:
   - Con Dev Template Mode: Templates pane muestra `.ntpl` compatibles auto-filtrados por device type
5. **Add/instantiate**:
   - Sin template: crea BDevice crudo (disabled)
   - Con template: wizard params (address, instance, zone) → instantiate → activate
6. **Result**: devices aparecen en Database pane

### 15.11.3 Actions

Per device:
- Discover (re-run)
- Add device manually (name + address + type)
- Rename
- Enable/Disable live toggle
- Test connection / Ping
- View Properties (property sheet)
- Learn points — re-query device object list / NV table / registers
- Deploy template

---

## 15.12 Point Manager vs Property Sheet — cuándo usar cada

| Aspecto | Point Manager | Property Sheet |
|---------|---------------|----------------|
| Scope | N puntos bajo subtree | 1 componente único |
| Visualización | Tabla sinc columnas | Árbol slots jerárquico |
| Caso uso | Comisionamiento masivo | Config granular individual |
| Multi-select | Nativo | N/A (1 component) |
| Bulk edit | Directo (context menu) | Manual per-component |
| Extensions | Columna con iconos | Property sheet del ext |
| Facets compare | Side-by-side en columnas | Scroll per-punto |
| Add extension a N | 1-click bulk | N veces |

Flujo típico: (1) Point Manager descubre + add + bulk config → (2) Property Sheet fine-tune individuales → (3) Point Manager verify bulk visibles.

---

## 15.13 Workflow end-to-end de un proyecto (5 fases)

### Fase 1 — Commissioning inicial
1. File → New Station wizard (nombre, ubicación, platform port 5011)
2. Install licenses en `~/security/licenses/*.license`
3. Start station via platform daemon (Bloque 10.1)
4. Workbench → station status = green

### Fase 2 — Driver + discovery
5. Drag driver module (ej. `bacnet-rt`) → `/Drivers` folder
6. Configure BACnetNetwork: IP range, BBMD, port 47808
7. Open Device Manager → Discover
8. Review discovered → multi-select → Add → Database pane

### Fase 3 — Bind proxy points
9. Open device → Point Manager
10. Filter + select (ej. 80 de 150 objects)
11. Add Points → BControlPoint + BProxyExt vinculado
12. Configure polling: pollRate=1s (típico), cov=enabled, covIncrement=0.5

### Fase 4 — Control logic
13. Create wiresheet en `/Control`
14. Drag kitControl blocks (Add, Multiply, PID, Timer)
15. Link points → blocks → actuators (BLink)
16. Test live: setpoint change → damper change real-time (Bloque 6.1)
17. Fallback logic: manual override, safety interlocks

### Fase 5 — Alarms + History + UI
18. Point Manager bulk Add AlarmSourceExt a critical (threshold, priority, class)
19. Bulk Add HistoryExt trending points (interval 900s, capacity 10000)
20. Create `.px` dashboard, drag widgets, bind a puntos
21. Configure users/roles/categories (Bloque 11.1)
22. Save Station + Backup (Bloque 10.3)
23. Test alarm generate + history query + access RBAC

---

## 15.14 Gotchas operacionales + shortcuts

### Concurrencia y persistencia

1. **2 Workbench concurrentes editando** mismo componente → **last write wins**. No merge. Sin prompt antes.
2. **Dirty state + close tab sin save** → prompt "discard changes?" (configurable default = ask)
3. **Workbench disconnect mid-edit** → cache local dirty mantiene. Reconnect reconcilia sin pérdida típicamente (sin conflicto concurrente).
4. **Station restart con dirty** → cambios pierden. Best practice: save antes de restart.

### Performance operacional

5. **Offline vs online editing**: agregar driver module nuevo = requiere restart. Agregar puntos / cambiar polling = live.
6. **Network discovery latency**: BACnet IP fast (30s/200 devices), MSTP 9600 baud lento (5-10 min red grande).
7. **Point polling limits** (empírico):
   - 1000-2000 puntos @ 1s = safe
   - 5000 puntos @ 5s = safe
   - 5000 puntos @ 1s = marginal (CPU + network saturation)
8. **Device offline proxy points** siguen contando hacia `point.limit` (Bloque 14.1.2). No auto-delete.

### Edit semantics

9. **Copy+paste con ORD externos**: links apuntan a ORIGINALS. Rename original → unresolved ORD. Workaround: usar "Duplicate" (Ctrl+D) que re-targets links internos.
10. **Deep copy**: Ctrl+D duplica subtree completo recursivo. References externas preservan (no re-target).
11. **Save de station grande** (10k+ componentes) = **asíncrono** via engine thread. UI no bloquea, engine events queuean.
12. **Facets NO enforced en load** — solo en edit UI. API programática puede crear valores fuera range.
13. **FieldEditor custom no registrado** → fallback genérico sin custom UI.

### Shortcuts productividad

| Shortcut | Acción |
|----------|--------|
| Ctrl+double-click | Abre componente en new tab |
| F5 | Refresh view (re-query values) |
| Ctrl+H | Home (root station tree) |
| Ctrl+Shift+O | Quick open by displayName |
| Middle-click tab | New tab (browser-style) |
| Right-click Properties | Context menu driver-specific actions |

### Best practices

14. **Backup antes de Batch Editor** (sin undo en Batch)
15. **Test en staging → Station Copier** (Bloque 10.3.5) a prod
16. **Monitor `point.limit`** via `/spy/sysManagers/licenseManager`
17. **Enable history + alarms** para auditoría + alertas
18. **Assign roles antes de deploy** — restrict operator write a critical points

---

## 15.15 Hallazgos críticos del bloque

1. **Glyph hierarchy composite pattern** — wiresheet usa `RootGlyph → LayerGlyph → ComponentGlyph → SlotBarGlyph/ActionBarGlyph/PropertyBarGlyph/FooterBarGlyph`. Spatial indexing con `RectangleMap` bucketing para O(log n) hit testing.

2. **`BWsAnnotation`** persistencia de layout es propiedad frozen `wsAnnotation` per-BComponent per-folder, serializada como string `"p,q,wixelWidth,wixelHeight"` en config.bog. Copy a otra carpeta genera nueva anotación independiente.

3. **State machine en WsController** — 5 states (Normal/Move/Link/RubberBand/Resize) coordinan UX. Transitions based en mouse events.

4. **Link routing manhattan-only** — no bezier/straight. H/V segments con reroute dinámico cached. LinkSnakeGlyph.tiles reuse si positions unchanged.

5. **No existe `BWsGroup` explícito** — "grupos" son inferidos via BFolder nested. No collapse/expand visual nativo en wiresheet. Menú "Arrange" solo permite align/distribute.

6. **No zoom in/out nativo** en Workbench wiresheet. Web wiresheet (ux) sí tiene zoom. Canvas limit 1000x1000 wixels default.

7. **FieldEditor resolution 3-level**: facet `fieldEditor` class → default agent `@AgentOn(types=...)` → fallback genérico. Configurable per-type y per-slot.

8. **Facets driven value editors** — min/max spinner, units label, precision format, trueText/falseText boolean, range EnumRange dropdown, multiLine textarea. Facets no enforce en load, solo en edit UI.

9. **Last write wins** sin merge — 2 Workbench concurrentes editando sobrescriben silenciosamente. No lock, no notification. Gotcha operacional serio.

10. **Dirty state** tracked en Workbench local hasta Save explícito. Engine thread save async — UI no bloquea en stations grandes.

11. **BFoxProxySession** con reference counting + "interest" multi-user + AES credentials cache. Remote tree lazy load via RPC async.

12. **Drag-from-nav default = BOrd reference**, Ctrl modifier = copy (clone subtree).

13. **BPointManager + BDeviceManager extienden BFolderManager** — `getTargetType()=BControlPoint.TYPE` en Point Manager, `supportsTemplates()=true` en Device Manager (integración Template/Match/Bind Bloque 14.12).

14. **Point Manager bulk actions directas** — sin license gate extra. Batch Editor (Bloque 14.11) requiere license `provisioning`. Complementarios, no duplicados.

15. **Polling limits empíricos**: 1000-2000 @ 1s safe, 5000 @ 5s safe, 5000 @ 1s marginal. Número accionable para capacity planning.

16. **Device offline proxy points siguen contando** hacia `point.limit` — no auto-delete. Operational gotcha para devices desconectados persistentemente.

---

## 15.16 Conexiones con otros bloques

- **Bloque 4 (Baja lifecycle)**: slots + facets + flags consumidos por FieldEditor resolution; `added()`/`removed()` dispara point counting update.
- **Bloque 5 (ORD + BOG)**: nav tree browsing ORDs, `wsAnnotation` serializada en BOG, link source/target ORDs.
- **Bloque 6 (Control)**: wiresheet rendering del event-driven engine; BLink creation via drag visualiza el link model Bloque 6.2; kitControl blocks drag-drop.
- **Bloque 7 (Drivers)**: Point/Device Manager son las vistas principales de drivers; discovery workflow ejecuta `BLonLearnJob` / BACnet I-Am broadcast / Modbus scan.
- **Bloque 8 (History/Alarm/Schedule)**: extensions (HistoryExt, AlarmSourceExt) agregables bulk via Point Manager.
- **Bloque 9.1 (Workbench core)**: BWsCanvas extiende BTransferWidget, Theme.wiresheet() integration con gx, bajaui facets pipeline.
- **Bloque 10.3 (Backup)**: Save Station persist layout + Station Copier promueve a prod.
- **Bloque 11 (RBAC)**: user/role/category afecta qué slots visibles en property sheet (flag OPERATOR + category permissions).
- **Bloque 13.1 (Niagara Network)**: nav tree Fox scope + BFoxProxySession para stations remotas.
- **Bloque 14 (Templates + Batch Editor)**: Device Manager integra DevTemplateMgr (Template/Match/Bind). Point Manager bulk ≠ Batch Editor (license).
- **Bloque 16 (próximo — Provisioning)**: `BNiagaraNetworkJob` es Supervisor-scale del workflow end-to-end (N stations en paralelo).
- **Bloque 17 (Filesystem)**: credentials.xml AES encryption en User Home.

---

## Engram topic keys

- `niagara/ui/wiresheet-editor-glyphs-state-machine`
- `niagara/ui/property-sheet-nav-tree-fieldeditor`
- `niagara/ui/point-device-manager-workflow-end-to-end`
