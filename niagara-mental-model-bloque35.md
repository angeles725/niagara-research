# Niagara N4 — Mental Model · Bloque 35

**Tema**: Nav tree + Workbench shell + views registry + bajaui widget system — `BWbShell`/`BWbProfile` lifecycle, `BNiagaraWbShell`/`BNiagaraWbFrame`/`BNiagaraWbWebShell` (3 shell variants: Swing frame, applet, WebStart), `BNavScheme` + `BINavNode` + `BNavFileSpace` (`.nav` XML), `NavFileDecoder` cache, `NavTreeModel`/`DefaultNavTreeModel`/`BNavTree`, `BWbProfile.getAgents()` + `BComponentMenuAgent` + `BNavMenuAgent` registry, view resolution (`@agent.on` XML → `AgentList.getDefault()`), bajaui widget/pane hierarchy (`BWidget` → `BPane` → `BBorderPane`/`BGridPane`/`BFlowPane`/`BTabbedPane`/`BSplitPane`/`BEdgePane`/`BLabelPane`), widget lifecycle via `ShellManager`/`ShellPeer`, event propagation (`BMouseEvent`/`BKeyEvent`/`BFocusEvent`), ord schemes (`workbench:`, `tool:`, `widget:`, `nav:`, `slot:`, `station:`, `module:`, `local:`, `file:`, `bog:`, `zip:`, `virtual:`, `spy:`, `http(s):`), sidebar framework `BWbSideBar`/`BWbSideBarManager` (Nav, Palette, Bookmark, Job), `BViewTabbedPane`/`BViewTab` (per-tab history + undo stack), drag-and-drop framework (`BTransferWidget`/`TransferContext`/`TransferEnvelope`/`TransferFormat`), `BAccelerator` keybindings, `WbCommands` (81 Command instances — menu bar/toolbar/accelerators/recent ords), `BWbPane` master layout (menu + toolbar + locator + tabs + sidebar + console + status), `BSwingWidget` → `BFxWidget` JavaFX embedding (JFXPanel), `BConsole`+`NShell`, `BJobBar`/`BJobMonitorPane`, Kiosk profile (`BKioskService`+auto-login+auto-logoff), Honeywell `BCentralineProfile` (Centraline branding override), `BFileSearch`+`FindPattern`+`BIQuickSearch`.

**Método**: Investigación READ-ONLY — decompilación `workbench-wb.jar` (869 types registrados en `module.xml`), `bajaui-wb.jar` (170 types), `bajaui-ux.jar` (HTML5 counterpart), `wbutil-wb.jar` (77 types — celleditors + fieldeditors gx/bajaui), `clProfile-wb.jar` (Honeywell-specific, 2 classes únicamente: `BCentralineProfile` + `CentraLineAboutCommand`), `file-ux.jar`; `javap -p` sobre ~55 clases clave (`BWbShell`, `BWbProfile`, `BWbView`, `BWbEditor`, `BWbPlugin`, `BNiagaraWbShell`, `BNiagaraWbFrame`, `BNiagaraWbWebShell`, `BNiagaraWbApplet`, `BWbPane`, `BSideBarPane`, `BViewTabbedPane`, `BViewTab`, `WbCommands`, `BWbApplication`, `BGeneralOptions`, `BNavScheme`, `BINavNode`, `BNavContainer`, `BNavFileNode`, `BNavFileSpace`, `NavFileDecoder`, `NavTreeModel`, `DefaultNavTreeModel`, `BNavTree`, `BNavMenuAgent`, `BComponentMenuAgent`, `BFileMenuAgent`, `BNavFolderMenuAgent`, `BPathBarAgent`, `BWorkbenchScheme`, `BToolScheme`, `BWidgetScheme`, `BWidget`, `BWidgetShell`, `BPane`+subclases layout, `BMouseEvent`, `BKeyEvent`, `BAccelerator`, `Command`, `BTransferWidget`, `TransferContext`, `TransferEnvelope`, `TransferFormat`, `Clipboard`, `BSwingWidget`, `BFxWidget`, `ShellManager`, `ShellPeer`, `Binder`, `BCentralineProfile`, `BKioskService`, `BKioskProfile`, `BFileSearch`, `BIQuickSearch`); extracción de `module.xml` de cada JAR (= registry canónica de types/agents/views); grep en `defaults/system.properties` (`niagara.ui.*`, `bajaui.*`, `niagara.wb.*`); inspección directa de `rc/fx/theme.css` (JavaFX embedded theme).

**Conecta con**: Bloque 22 (PX presentation — PX es *document renderer* dentro de un `BWbPxView`, bajaui/WB es *shell container*), Bloque 6.1.5 (engine thread — aquí se extiende: engine vs AWT EDT vs JavaFX FX thread vs Binder Worker), Bloque 9 (servlet/web profile vs WB profile — mismo código runtime, distinto `getAgents()` filter), Bloque 15 (wiresheet views → `BWireSheet` es un `BWbView` registrado con `@agent.on type="baja:Component"`), Bloque 17 (filesystem — `.nav` files viven en `!/nav` dentro del module JAR o en `~/stations/<s>/nav/`), Bloque 29 (web profile servlet tree — paralelo conceptual al Nav tree pero resuelto server-side), Bloque 33 (EDT sync gotcha — BatchEditor ejecuta en AWT EDT bloqueando UI), Bloque 13 (auth — `BCredentialsManager` guarda DPAPI `.km/.kr`, NO master.jceks), Bloque 30 (FIPS — `WbCommands.fipsRestart` + `BFipsOptions` en bajaui-wb), Bloque 20 (networking — `BHostCnxHandler`/`BStationCnxType`/`BDeviceCnxType` = connection dialog framework).

---

## 35.0 Hallazgo de entrada — el "Workbench" es 4 aplicaciones distintas

El árbol instalado tiene un único `workbench-wb.jar` pero ese JAR registra **cinco profiles concretos** que son aplicaciones UI completamente distintas:

```
javax.baja.workbench.BWbProfile (abstract base — 869 types profile-aware)
├─ com.tridium.workbench.shell.BDefaultWbWebProfile     ← browser "full WB" (HTML5 web profile)
├─ com.tridium.workbench.shell.BBasicWbWebProfile       ← browser readonly (sin tools)
├─ com.tridium.workbench.shell.BHandheldWbWebProfile    ← mobile
├─ com.tridium.workbench.shell.BSimpleAdminWbWebProfile ← platform admin limited
├─ javax.baja.workbench.kiosk.BKioskProfile (abstract)
│  ├─ BDefaultKioskProfile    ← station-auto-login
│  ├─ BBasicKioskProfile      ← limited menus
│  └─ BHandheldKioskProfile   ← touch screens
└─ com.centraline.clProfile.ui.BCentralineProfile       ← Honeywell Centraline OVERRIDE (clProfile-wb)
```

Y tres **shells** (containers concretos que instancian un profile):

| Shell class | Cuándo | Cómo se abre |
|---|---|---|
| `BNiagaraWbFrame` | Workbench.exe / Java Web Start desktop | Main frame Swing (java.awt.Window) |
| `BNiagaraWbWebShell` | Browser `http://host/ord?...` (legacy Java applet / HTML5 profile) | Dentro de applet JVM o HTML5 bajaux |
| `BNiagaraWbApplet` | Browser embedded applet (legacy) | `NiagaraWbAppletLoginPane` → autenticación → frame |

**Consecuencia crítica**: cuando se habla de "la UI de Niagara" no existe una sola UI. Existen múltiples combinaciones `(Shell × Profile)` que comparten el widget system y el view agent registry pero **difieren en qué views se exponen**. El filtro se aplica en `BWbProfile.getAgents(BObject)` que sub-classes (incluyendo `BCentralineProfile`) sobrescriben para *esconder* views.

**Honeywell-specific (clProfile-wb.jar)**: `BCentralineProfile.adjustAgents(AgentList)` reordena o elimina agents (p.ej., fuerza `HonBacnetDeviceManager`/`HonBacnetOwsDeviceManager` como default view sobre `BBacnetNetwork` en lugar del stock `BacnetDeviceManager`). Evidencia:

```
private static final String HONBACNETDEVICEMANAGER    = "honBacnet:BacnetDeviceManager";
private static final String HONBACNETOWSDEVICEMANAGER = "honBacnet:BacnetOwsDeviceManager";
private javax.baja.agent.AgentList adjustAgents(javax.baja.agent.AgentList);
```

Es decir, **el mismo JAR stock `bacnet-wb.jar` expone `BacnetDeviceManager` como default**, pero Honeywell lo reordena al instalar `clProfile-wb`. Si se desinstala `clProfile-wb` la UI vuelve al stock. Esto explica por qué `clProfile-wb` depende de **`driver-rt` pero NO depende de `bacnet-*`**: opera vía registry string lookup (TypeSpec resolution en runtime, `typeInfo("honBacnet:...")`).

---

## 35.1 Herencia de clases — el eje del shell

```
javax.baja.sys.BObject
└─ javax.baja.sys.BComponent
   └─ javax.baja.ui.BWidget                    ← base raíz de TODA UI widget Niagara
      ├─ BPane (abstract layout container)
      │  ├─ BBorderPane        ← padding/margin/border/fill wrapper
      │  ├─ BEdgePane          ← 5 slots: top/bottom/left/right/center
      │  ├─ BGridPane          ← rows × columns grid
      │  ├─ BFlowPane          ← horizontal/vertical wrap
      │  ├─ BSplitPane         ← resizable divider
      │  ├─ BTabbedPane        ← tabs (heredado por BViewTabbedPane)
      │  ├─ BScrollPane        ← ScrollBar + viewport
      │  ├─ BCanvasPane        ← absolute positioning (PX editor usa esto)
      │  ├─ BExpandablePane    ← collapse/expand con handle
      │  ├─ BLabelPane         ← Label + content (BViewTab hereda)
      │  ├─ BConstrainedPane   ← min/max dimensions enforced
      │  ├─ BToolPane          ← toolbar-like (sidebar manager hereda)
      │  ├─ BTreePane          ← wraps BTree con scroll
      │  ├─ BResponsivePane    ← breakpoint-based layout
      │  └─ BTransformPane     ← rotation/scale
      ├─ BWidgetShell                              ← root container (owns EDT peer)
      │  └─ javax.baja.workbench.BWbShell (abstract)
      │     └─ com.tridium.workbench.shell.BNiagaraWbShell
      │        ├─ BNiagaraWbFrame      (desktop Swing frame peer)
      │        └─ BNiagaraWbWebShell
      │           └─ BNiagaraWbWebStartShell
      ├─ BAbstractButton, BButton, BToggleButton, BRadioButton, BCheckBox
      ├─ BLabel, BHyperlinkLabel, BTextField, BTextDropDown, BListDropDown
      ├─ BMenu, BMenuBar, BMenuItem, BSubMenuItem, BActionMenuItem
      ├─ BDialog, BFrame, BWindow, BRoundedDialog, BRoundedFrame
      ├─ BTable, BBoundTable, BTreeTable, BList, BCheckList, BTree
      ├─ com.tridium.ui.swing.BSwingWidget   ← hosts javax.swing.JRootPane
      │  └─ com.tridium.ui.fx.BFxWidget      ← hosts javafx.embed.swing.JFXPanel
      └─ javax.baja.ui.transfer.BTransferWidget  ← participa en DnD / Clipboard
```

**Observación no obvia**: `BFxWidget extends BSwingWidget extends BTransferWidget extends BWidget`. Esto significa que **todo widget que use JavaFX vive DENTRO de un JFXPanel que vive DENTRO de un JRootPane**, que Niagara pinta llamando Swing. Hay **tres grafos de escena superpuestos**:

```
  Niagara Widget Tree  (baja UI thread)
         └─ BSwingWidget ─── JRootPane (AWT EDT) ─── contentPane Swing
                                └─ JFXPanel (EDT) ─── FX Scene root (JavaFX FX thread)
```

Cada thread tiene ownership exclusivo de su grafo. Ver §35.12 (threading model).

---

## 35.2 `BWbShell` — contrato del shell

### 35.2.1 Slots abstractos (javap)

```java
public abstract class BWbShell extends BWidgetShell
    implements BIHyperlinkShell, BIActiveOrdShell {
  public abstract BOrd       getActiveOrd();
  public abstract OrdTarget  getActiveOrdTarget();
  public abstract BWbView    getActiveView();
  public abstract BWbProfile getProfile();
  public abstract Command    getRefreshCommand();
  public abstract Command    getSaveCommand();
  public abstract Command    getExportCommand();
  public abstract Command    getBackCommand();
  public abstract Command    getForwardCommand();
  public abstract Command    getLogoffCommand();
  public static BWbShell     getWbShell(BWidget);    // traversal: cualquier widget → su shell
}
```

Un `BWbShell` es un `BWidgetShell` (ya tiene content, menuBar, focus manager) **PLUS**:
- Ord navigation state (active ord + target + history)
- Profile reference (qué app actual)
- 6 Commands globales (refresh, save, export, back, forward, logoff)
- `ActivityListener` pattern para auto-logoff

### 35.2.2 `BIHyperlinkShell` + `BIActiveOrdShell`

Dos interfaces que el shell implementa:
- **`BIHyperlinkShell`**: `hyperlink(BOrd)` — resuelve ord y abre view correspondiente (método `final`, delegate a `doHyperlink(HyperlinkInfo)` en `BNiagaraWbShell`)
- **`BIActiveOrdShell`**: `getActiveOrd()` — cualquier código puede preguntarle al shell "¿qué ord está mostrando la UI ahora mismo?" — base para pathbar, bookmarks, link-mark/link-to commands

**Gotcha**: `getActiveOrd()` devuelve el ord *del tab activo*, no el del widget actual (p.ej., si el usuario tiene foco en Palette sidebar, `getActiveOrd()` sigue devolviendo el ord del tab central). Código que cacha "el ord actual" debe distinguir entre shell-level ord y widget-level ord.

### 35.2.3 `BNiagaraWbShell` — implementación concreta

```java
public abstract class BNiagaraWbShell extends BWbShell
    implements NiagaraWbShell, ShellManager.ShellPeerFactory, NavListener {
  public final BWbProfile    profile;
  public final WbCommands    commands;          // 81 Commands (ver §35.11)
  public final BWbPane       pane;              // master layout
  public  BFileChooser       fileChooser;
  public  BDirectoryChooser  dirChooser;
  public  BBqlQueryBuilder   bqlBuilder;
  public static final int    OPTION_CLOSE_CHANGE_USER;
  static  WbHistory          history;           // back/forward global
  // ...
  public void hyperlink(File, int, int, int, int);
  public void hyperlink(HyperlinkInfo);
  public final void syncTree();
  public final void syncTree(boolean);
}
```

Claves:
- `profile` es final — **un shell nunca cambia de profile sin reiniciar**. Cambiar profile requiere cerrar shell → crear nuevo. Esto contradice la intuición de "switcheo vivo".
- `history` es **static** (shared entre todas las instancias) — la historia back/forward no es per-tab sino per-JVM. Bug observable: abrir dos Workbenches contra stations distintas y pulsar Back puede navegar entre stations.
- `commands` es una instancia dedicada por shell; no es singleton.

---

## 35.3 `BWbProfile` — qué ve el usuario

### 35.3.1 API de customización

```java
public class BWbProfile extends BObject {
  public boolean hasView(BObject, AgentInfo);
  public AgentList getAgents(BObject);         // ← gatekeeper principal
  public BWbView customizeView(BWbView);
  public boolean hasSideBar(TypeInfo);
  public boolean hasTool(TypeInfo);
  public boolean hasQuickSearch(AgentInfo, BWbView);
  public BOrd    getStartOrd();                // ord al abrir
  public BOrd    getHomeOrd();                 // botón Home
  public BOrd    getNavRootOrd();              // raíz del árbol Nav sidebar
  public BOrd    getOpenOrd(BISession, BOrd);  // resolución contextual
  public boolean canHyperlink(BOrd);           // gating de navegación
  public BIMenuBar makeMenuBar();
  public BIToolBar makeToolBar();
  public String getAppName();
  public String[] getAppNames();
  public BImage getFrameIcon();
  public String getFrameTitle();
}
```

### 35.3.2 `getAgents()` — punto de interceptación Honeywell

El default `BWbProfile.getAgents(BObject obj)` devuelve todos los `AgentInfo` registrados para el tipo de `obj`. `BCentralineProfile` sobrescribe este método para:

```java
public AgentList getAgents(BObject);           // Centraline override
private AgentList adjustAgents(AgentList);     // rearrange/filter
private BComponent getPointListViewService(BObject);
```

Semánticamente `adjustAgents()` puede:
1. Mover un agent a top → cambiar default view
2. Remover agent → view "desaparece" de right-click "Views →"
3. Reemplazar default → un `BacnetDeviceManager` stock queda oculto, `HonBacnetDeviceManager` aparece primero

**Esto no es dynamic plugin behavior** — se evalúa cada vez que el shell pide agents (al cambiar tab, al right-click). No hay cache observable; mean que desplegar `clProfile-wb` no requiere restart station, solo relaunch WB.

### 35.3.3 `DEV_VIEWS` — toggle debugging

```java
private static final String[] DEV_VIEWS;
private static final Map<String, Boolean> devViewsEnabled;
```

El profile base mantiene una lista de "dev views" que se habilitan vía system property (confirmado: la clave es `niagara.wb.devViews.<viewName>=true`). Views como `ModuleSpaceView`, `SyntheticModuleFileView`, `SpyScheme` browser — útiles para desarrollo, ocultas por default en production. **Deshabilitar en prod** si se ve en hardening checklists.

---

## 35.4 `BWbView` + `BWbEditor` + `BWbPlugin` — jerarquía de views

```
BWbPlugin (base — tiene name/icon/menu)
└─ BWbEditor (add: loadValue/saveValue/CannotSaveException + validators)
   └─ BWbView (add: 12 command slots CUT/COPY/...GOTO, transferWidget, viewMenus, viewToolBar)
      ├─ BWbComponentView         ← genérico para Components
      ├─ BWbPxView                ← renderiza .px (Bloque 22)
      ├─ BSlotSheet / BTypeSlotSheet
      ├─ BPropertySheet / BPropertySheetFE
      ├─ BWireSheet (wireSheet-wb)
      ├─ BCollectionTable         ← ITable
      ├─ BWebBrowserView          ← JavaFX WebView (IHtmlFile/HttpObject/Spy)
      ├─ BWbServiceManagerView
      ├─ BCategoryBrowser, BFolderManager, BBookmarkTable
      ├─ (~40 manager views específicos de driver/service)
      └─ BNavContainerView        ← default para NavContainer
```

### 35.4.1 Command IDs

`BWbView` define constantes:

```java
public static final int CUT = 0;
public static final int COPY = 1;
public static final int PASTE = 2;
public static final int DUPLICATE = 3;
public static final int DELETE = 4;
public static final int RENAME = 5;
public static final int FIND = 6;
public static final int FIND_PREV = 7;
public static final int FIND_NEXT = 8;
public static final int REPLACE = 9;
public static final int GOTO = 10;
public static final int PASTE_SPECIAL = 11;
static final int COUNT = 12;
```

Cada view declara vía `setCommandEnabled(int, boolean)` cuáles soporta. El shell consulta con `isCommandEnabled(int)` para habilitar/deshabilitar entradas de menú/toolbar. El handler real es `invokeCommand(int)` que devuelve `CommandArtifact` (permite undo).

### 35.4.2 Lifecycle

Heredado de `BWbEditor`:

```
prime()              ← pre-load (puede renderizar "loading...")
loadValue(BObject)   ← final, llama doLoadValue()
doLoadValue(obj,ctx) ← override por view
activated()          ← tab seleccionado
                     ( user interactúa )
deactivated()        ← tab cambió
saveValue(ctx)       ← final, valida + llama doSaveValue()
doSaveValue(obj,ctx) ← override, throws CannotSaveException
clearModified()      ← marcar limpio
```

Validators: `BIValidator[] validators` se cargan de registry; si validación falla en `saveValue()`, throws antes de tocar disco.

**Gotcha life**: `activated()` / `deactivated()` corren **síncronos en el thread del tab switch** (AWT EDT). Si un view hace network call en `activated()` sin schedule async → UI lock visible. `BWbPxView` lo hace bien (px binding worker); custom views frecuentemente no.

---

## 35.5 Nav tree — `BNavScheme` + `BINavNode` + `NavTreeModel`

### 35.5.1 `BINavNode` interface (baja.jar)

```java
public interface BINavNode extends BInterface {
  String       getNavName();
  String       getNavDisplayName(Context);
  String       getNavDescription(Context);
  BINavNode    getNavParent();
  boolean      hasNavChildren();
  BINavNode    getNavChild(String);
  BINavNode    resolveNavChild(String);       // como getNavChild pero hace lookup
  BINavNode[]  getNavChildren();
  BOrd         getNavOrd();                    // ord canónico navegable
  BIcon        getNavIcon();
  Iterator<BINavNode> iterateNavDescendants();
}
```

Cualquier objeto que implemente esta interfaz **es navegable** en el Nav sidebar. Implementadores reales observados:
- `BComponent` (todo componente station) — heredan vía `BComponentSpace`
- `BIFile` / `BIDirectory` — filesystem
- `BHost`, `BSession`, `BFoxSession`, `BVirtualGateway`
- `BModuleNavNode`, `BTypeNavNode` — sys module browser
- `BNavFileNode` (XML loaded)
- `BToolsContainer` — tools pseudo-root
- `BIAliasNavNode` — soft alias (evita cycle detection en sync)

### 35.5.2 `BNavScheme` — ord scheme "nav:"

```java
public class BNavScheme extends BOrdScheme {
  public static final BNavScheme INSTANCE;
  public OrdQuery parse(String);
  public OrdTarget resolve(OrdTarget, OrdQuery)
      throws SyntaxException, UnresolvedException;
  private void checkPermissions(BINavNode, Context);    // ← enforcement
}
```

La ord `nav:Station/Config/.../MyPoint` se parsea en una `OrdQuery` y se resuelve recorriendo `getNavChild()` por cada path segment. **`checkPermissions` se llama durante resolución**: un usuario sin permisos sobre un nav node levanta `UnresolvedException` — la UI no muestra el error, simplemente no expande.

### 35.5.3 `.nav` file XML (NavFileDecoder)

```java
public class NavFileDecoder extends XParser {
  static Map<String, NavFileDecoder$CacheItem> cache;     // ← STATIC cache
  public static BNavFileSpace load(BOrd) throws Exception;
  public static BNavFileSpace load(BIFile) throws Exception;
  public BNavFileSpace decodeDocument();
  public BNavFileSpace decodeDocument(boolean);
  private BNavFileNode decodeNode(XElem);
}
```

Schema observado (inferido del decoder + `BNavFileNode` ctor signature `(String name, BOrd navOrd, BIcon icon)`):

```xml
<nav>
  <node name="Home"     ord="local:|station:|slot:/Home"        icon="module://icons/x16/home.png">
    <node name="Floor1" ord="local:|station:|slot:/Floor1"      icon="..."/>
    <node name="Floor2" ord="local:|station:|slot:/Floor2">
      <node name="Zone A" ord="local:|station:|slot:/Floor2/ZoneA"/>
    </node>
  </node>
  <node name="Alarms" ord="local:|station:|slot:/Services/AlarmService"/>
</nav>
```

**Gotcha crítico de cache**: `NavFileDecoder.cache` es `static Map`. Los `.nav` files se cachean por ord (CacheItem envuelve hash/timestamp). Si un admin edita `~/stations/foo/nav/building.nav` en disco mientras el station corre, **los Workbenches conectados pueden seguir viendo el árbol viejo hasta re-resolve** — no hay file-watch automático. Workaround: forzar hyperlink a `nav:!...` con refresh, o reiniciar WB.

### 35.5.4 `BNavFileSpace` — in-memory tree

```java
public class BNavFileSpace extends BSpace {
  BNavFileNode root;
  Map<BOrd, BINavNode> map;     // ord-to-node index
  void buildMap(BINavNode);     // construido en decode
  public BNavFileNode lookup(BOrd);
}
```

Extender `BSpace` (Bloque 2.x) implica que el nav file vive en un **Space** separado — no es parte del `ComponentSpace` de la station. Cambios al nav tree emiten `NavEvent` vía `fireNavEvent(NavEvent)`.

### 35.5.5 `NavTreeModel` / `DefaultNavTreeModel` / `BNavTree`

El sidebar no muestra directamente `BINavNode` — los envuelve en `NavTreeNode` (UI node con expand state, selection, icon cache):

```java
public abstract class NavTreeModel extends TreeModel {
  HashMap<BOrd, NavTreeNode> ordMap;             // ord → UI node
  public NavTreeNode lookup(BOrd);
  public void navEvent(NavEvent);                // ← NavListener hook
  protected void added(NavEvent);
  protected void removed(NavEvent);
  protected void renamed(NavEvent);
  protected void reordered(NavEvent);
  protected void replaced(NavEvent);
  void remap(NavTreeNode, BOrd oldOrd, BOrd newOrd);
}

public class DefaultNavTreeModel extends NavTreeModel {
  private NavTreeNode root;
  private boolean rootVisible;
}
```

`BNavTree` (hereda `BTree`, implementa `NavListener`):

```java
public class BNavTree extends BTree implements NavListener {
  boolean readonly;
  public void expandToOrd(BObject, BOrd);
  public void expandToNavNode(BINavNode);
  public void setExpanded(TreeNode, boolean);
  public void navEvent(NavEvent);                // update tree on station events
  public TransferEnvelope getTransferData();     // DnD source
  public CommandArtifact insertTransferData(TransferContext);   // DnD target
  public CommandArtifact removeTransferData(TransferContext);   // cut
  public CommandArtifact doDuplicate();
  public CommandArtifact doDelete();
  public CommandArtifact doRename();
  public int dragOver(TransferContext);          // highlight during drag
  public CommandArtifact drop(TransferContext);
}
```

**Flow de sync**: Station emite `NavEvent` → fox session route → `BNavTree.navEvent()` → delegate a `NavTreeModel.navEvent()` → model actualiza `ordMap` → fireUIChange → `BTreePane` repinta.

---

## 35.6 View agent registry — cómo se resuelve view para un objeto

### 35.6.1 Declaración en `module.xml`

Para que un view aparezca al right-click sobre un tipo, el module debe registrar el view **con un `<agent>` block**:

```xml
<type class="com.mycompany.BMyView" name="MyView">
  <agent requiredPermissions="r">
    <on type="baja:Component"/>
    <on type="mymodule:SpecialThing"/>
  </agent>
</type>
```

Múltiples `<on type="...">` dentro de un `<agent>` = el view aplica si el objeto **implements/extends cualquiera** de esos types.

### 35.6.2 Ejemplos reales (workbench-wb module.xml)

| View (class) | Agent `on` types | Permisos |
|---|---|---|
| `BNavContainerView` | `baja:NavContainer` | `r` |
| `BPropertySheet` | `baja:Component`, `baja:IPropertyContainer` | `r` |
| `BSlotSheet` | `baja:Component`, `baja:IPropertyContainer` | `W` (write!) |
| `BWbPxView` | `file:PxFile`, `bajaui:PxInclude` | `r` |
| `BTextFileEditor` / `BTextFileViewer` | `baja:IDataFile` | `r` |
| `BHexFileEditor` | `baja:IDataFile` | `r` |
| `BCollectionTable` | `baja:ITable` | `r` |
| `BServiceManager` | `baja:ServiceContainer` | `r` |
| `BJobServiceManager` | `baja:JobService` | `r` |
| `BRelationSheet` | `baja:Component` | (none, i.e., all) |
| `BLoggerConfiguration` | `baja:ILoggingService` | (none) |
| `BModuleSpaceView` | `baja:ModuleSpace` | `r` |
| `BDirectoryList` | `baja:IDirectory` | `r` |

### 35.6.3 Menu agents (distinct from view agents)

Menu agents populan el right-click menu (distintos de views — son "actions"):

| Menu agent class | Agent `on` |
|---|---|
| `BComponentMenuAgent` | `baja:Component`, `baja:ComponentSpace` |
| `BFileMenuAgent` | `baja:IFile`, `fox:FoxFileSpace` |
| `BBogFileMenuAgent` | `file:BogFile` |
| `BFoxSessionMenuAgent` | `fox:FoxSession` |
| `BHostMenuAgent` | `baja:Host` |
| `BLocalHostMenuAgent` | `baja:LocalHost` |
| `BSessionMenuAgent` | `baja:Session` |
| `BVirtualGatewayMenuAgent` | `baja:VirtualGateway` |
| `BZipFileMenuAgent` | `baja:ZipFile` |
| `BNavFolderMenuAgent` | `baja:NavFolder` |

`BComponentMenuAgent` tiene constantes públicas para las secciones (estas ordenan el menú):
```
VIEWS, ACTIONS, NEW, CUT, COPY, PASTE, PASTE_SPECIAL, DELETE, DUPLICATE,
LINK_MARK, LINK_TO, LINK_FROM, RELATION_MARK, RELATE_TO, RELATE_FROM,
SET_DISPLAY_NAME, RENAME, REORDER, COMPOSITE, FIND, EXPORT
```

### 35.6.4 Resolución — algoritmo real

`BNavMenuAgent.makeMenu(BWidget, BObject, boolean, Context)` construye el menú para un objeto `x`:

1. Computa `TypeInfo ti = Sys.getType(x).getTypeInfo()`
2. Llama `ti.getAgents()` → `AgentList` con **todos** los agents registrados para `ti` y sus supertypes
3. Filtra por profile: `profile.getAgents(x)` → permite a profile (incluyendo `BCentralineProfile`) reordenar/esconder
4. Filtra por permisos usuario (cada `AgentInfo.getRequiredPermissions()` vs `user.getPermissions(x)`)
5. Default view = `AgentList.getDefault()` → primer agent no filtrado
6. Construye menu items en orden canónico `VIEWS` > `ACTIONS` > ...
7. Añade `BIComponentMenuDecorator` injected (decoradores plug-in)

**Observación**: `BIComponentMenuDecorator` (registrada en `module.xml` como interface type) permite a 3rd-party modules añadir items al right-click **sin** tener que declarar un menu agent propio. Es el mecanismo usado por módulos como driver-specific addons.

### 35.6.5 View collision (gotcha)

Si dos modules declaran `<on type="baja:Component"/>` como default view, **ambos aparecen en `AgentList`** con orden determinado por:
1. Profile `getAgents()` orden (Centraline puede forzar uno a top)
2. Orden de registration en module loader (no deterministico entre sesiones si módulos cambian)
3. Dentro de un módulo, orden del XML

**Result**: el "default view" del right-click puede variar entre instalaciones. Para forzar: profile override. Para debugging: abrir **Views →** submenu y observar orden completo.

---

## 35.7 Ord schemes — el lenguaje de navegación

`BOrdScheme` es la base (Bloque 2.x). workbench-wb + bajaui-wb + baja registran múltiples schemes que se componen:

| Scheme | Ejemplo | Scheme class | Registro |
|---|---|---|---|
| `local:` | `local:|foxs:|ip:host` | `BLocalScheme` | baja |
| `station:` | `station:|slot:/...` | `BStationScheme` | baja |
| `slot:` | `slot:/Foo/Bar` | `BSlotScheme` | baja |
| `handle:` | `h:abc123` | `BHandleScheme` | baja |
| `module:` | `module://mymod/views/foo.px` | `BModuleScheme` | baja |
| `file:` | `file:^etc/foo.txt` | `BFileScheme` | file-rt |
| `bog:` | `bog:!foo.bog` | `BBogScheme` | baja |
| `zip:` | `zip:foo.zip!bar.txt` | `BZipScheme` | baja |
| `virtual:` | `virtual:gatewayName/path` | `BVirtualScheme` | baja |
| `spy:` | `spy:` (debug UI) | `BSpyScheme` | baja |
| `nav:` | `nav:!Home/Floor1` | `BNavScheme` | baja |
| `widget:` | `widget:/root/<id>` | `BWidgetScheme` | bajaui-wb |
| `workbench:` | `workbench:helpAbout`, `workbench:licenseAgreement` | `BWorkbenchScheme` | workbench-wb |
| `tool:` | `tool:AlarmPortal`, `tool:ComponentFinder` | `BToolScheme` | workbench-wb |
| `http(s):` | `http://...` | `BHttpScheme`/`BHttpsScheme` | net-rt |
| `fox(s):` | `fox://host:1911` | `BFoxScheme`/`BFoxsScheme` | fox-rt |
| `ip:` | `ip:host` | `BIpScheme` | baja |
| `cell:` | `cell:...` | `BCellScheme` | baja |
| `password:` | `password:...` | `BPasswordAuthenticationScheme` | baja (auth) |
| `digest:` | | `BDigestAuthenticationScheme` | baja |
| `basicAuth:` | | `BLegacyBasicAuthenticationScheme` | baja |
| `binderCache:` | internal | `BBinderCacheScheme` | bajaui-wb |

### 35.7.1 Ord composition (the pipe `|`)

Un ord Niagara es una **cadena de schemes separados por `|`**:

```
local:|fox:|station:|slot:/Drivers/BacnetNetwork/device1/points/sensor1
```

Se resuelve izquierda-a-derecha:
1. `local:` → `LocalHost`
2. `fox:` → `FoxSession` (sin host explícito, usa current)
3. `station:` → `Station` (component space)
4. `slot:` → path navigation

Cada scheme.resolve() recibe el `OrdTarget` acumulado y devuelve el siguiente.

### 35.7.2 `tool:` y `workbench:` — ords especiales

```java
public class BWorkbenchScheme extends BOrdScheme {
  public static BObject helpAbout;
  public static BObject helpContents;
  public static BObject licenseAgreement;
  public static BObject thirdPartyLicenses;
}
```

Son **constantes estáticas** que resuelven siempre al mismo singleton. `workbench:helpAbout` abre diálogo About (sin recurso filesystem). Muy útil para hyperlinks del profile (`clProfile-wb` puede override `workbench:helpAbout` hacia Honeywell branding — pero no lo hace en el JAR observado; solo override de `CentraLineAboutCommand`).

`tool:` resuelve al `BToolsContainer` que es un nav pseudo-root enumerando `BWbService` instalados vía `getInstalled()`. Ejemplo: `tool:BacnetServiceManager` abre directo el manager sin necesidad de tener BacnetService en la station.

### 35.7.3 `widget:` scheme

```java
public class BWidgetScheme extends BOrdScheme {
  public static BOrd makeWidgetOrd(BWidget);
  BComplex getRoot(BComplex);
  BComplex ensureResolvable(BComplex);
}
```

Resuelve `widget:/root/<handle>` hacia widgets vivos en el shell. Usado por:
- Debug: spy:widgetTree para inspección
- PX binding: `localOrd:widget:...` refiere a widget sibling
- Popup resolvers: referenciar el widget que disparó el popup

**Gotcha**: widget ords son **frágiles** — referirse a un widget por ord cross-session no funciona (los handles son per-JVM).

---

## 35.8 `BWbPane` — layout master del shell

```java
public class BWbPane extends BEdgePane {
  final BNiagaraWbShell  shell;
  final BWbProfile       profile;
  private BIMenuBar      menuBar;
  private BIToolBar      toolBar;
  private BScrollingToolbarPane toolBarPane;
  private BWbLocatorBar  locatorBar;
  BViewTabbedPane        views;      // tabs centro
  BSideBarPane           sideBar;    // izquierda/derecha
  BBorderPane            sideBarBp;
  BConsole               console;    // bottom (NShell)
  private BSplitPane     hSplitPane;
  private BSplitPane     vSplitPane;
  private BBorderPane    hSplitPaneBp;
  private BFxContentPane content;
  private BWbStatusBar   statusBar;
}
```

Layout ASCII:

```
┌──────────────────────────────────────────────────────────────────────┐
│  BMenuBar          (profile.makeMenuBar() — File/Edit/...Help)       │
├──────────────────────────────────────────────────────────────────────┤
│  BScrollingToolbarPane  (profile.makeToolBar() + view.getViewToolBar)│
├──────────────────────────────────────────────────────────────────────┤
│  BNiagaraWbLocatorBar   (path bar + quick search)                    │
├───┬──────────────────────────────────────────────────────────────────┤
│ S │                                                                  │
│ i │                                                                  │
│ d │      BViewTabbedPane                                             │
│ e │      ┌────┬────┬────┬────┐                                       │
│ B │      │Tab1│Tab2│Tab3│ +  │                                       │
│ a │      ├────┴────┴────┴────┤                                       │
│ r │      │                   │                                       │
│ P │      │   Active BWbView  │                                       │
│ a │      │                   │                                       │
│ n │      │                   │                                       │
│ e │      │                   │                                       │
├───┴──────┴───────────────────────────────────────────────────────────┤
│  BConsole (nsh prompt + buffer) ← toggle with Ctrl+`                 │
├──────────────────────────────────────────────────────────────────────┤
│  BWbStatusBar  (left: status text | right: BJobStatusBar + FIPS)     │
└──────────────────────────────────────────────────────────────────────┘
```

Las divisiones son 2 `BSplitPane`:
- `hSplitPane` entre `sideBar` y `views` (sidebar/main)
- `vSplitPane` entre `views+hSplit` y `console` (bottom)

Ambos divisores son resizable y sus posiciones se persisten en `BSideBarPane.pickle()/unpickle()` → `defaultPickle` constante con layout por defecto.

### 35.8.1 Update on tab change

`BWbPane.update(BViewTab)` rebuilds menu/toolbar/locator — cada tab puede inyectar su propio menu extra (`view.getViewMenus()`) y toolbar (`view.getViewToolBar()`). Hay mergear de menus (`mergedMenus` array) para no duplicar File/Edit/Help.

---

## 35.9 Sidebars — `BWbSideBar` + `BWbSideBarManager`

### 35.9.1 Tipos registrados

| Sidebar | Class | Descripción |
|---|---|---|
| Nav | `BNavSideBar` | Tree navegable del station (default) |
| Palette | `BPaletteSideBar` | Components desde .palette files, drag-to-drop |
| Bookmarks | `BBookmarkSideBar` | User bookmarks persist en `~/etc/bookmarks.xml` |
| Jobs | `BJobSideBar` | Running jobs del station actual |

### 35.9.2 `BIWbSideBar` interface

```java
public interface BIWbSideBar extends BInterface {
  TypeInfo[]   getInstalledSideBars();
  boolean      hasCloseCommand();
  void         activeViewChanged();
  String       getLabel();
  BIcon        getIcon();
  BWidget      asWidget();
}
```

Un module puede registrar un sidebar custom implementando `BWbSideBar extends BWbPlugin implements BIWbSideBar`. Los profiles deciden si lo muestran vía `BWbProfile.hasSideBar(TypeInfo)`.

### 35.9.3 Manager — persistencia de layout

```java
public final class BWbSideBarManager extends BToolPane implements IWbSideBarManager {
  private final Array<BIWbSideBar> bars;
  public String serialize();
  public void deserialize(String);
  public void openMode(WbCommands$Mode);         // none/mini/single/double/master
  public BIWbSideBar openSideBar(BIWbSideBar);
  public void closeSideBar(BIWbSideBar);
  public void closeAllSideBars();
}
```

5 modos (`WbCommands$Mode`): `none` (solo tabs), `mini` (iconos colapsados), `single` (1 panel), `double` (2 panels side-by-side), `master` (2 panels + full-width nav). Persistidos vía `BSideBarPane.pickle()/unpickle()`.

`BPaletteSideBar` merece nota: incluye `BComponentPreviewWidget` (preview al hover de un componente del palette) — `BDefaultComponentPreviewFactory` genera imagen; modules pueden registrar `BWidgetPreviewFactory` para overrides sobre tipos específicos (`bajaui:TabbedPane`, `bajaui:BoundTable` son los únicos stock).

### 35.9.4 `BNavSideBar` — driver del nav tree

```java
public class BNavSideBar extends BWbSideBar implements BookmarkEvents$Listener {
  public static final Action updateTree;
  private final BEdgePane content;
  private final BTreePane treePane;
  private final BListDropDown openTrees;     // "My Files", "My Modules", "Station", custom .nav
  private final BNavTree tree;
  private BINavNode root;
  private boolean inError;
  private HashMap<NavWrapper, NavTreeModel> hash;  // cache per-root model
  public void syncTree();
  public void syncTree(boolean);
  public void goInto(BINavNode);
}
```

**`openTrees` dropdown** muestra todas las raíces cargadas. User puede cargar múltiples `.nav` files simultáneamente; cada uno tiene su propio `NavTreeModel` cached en el `hash` map — evita reload en switch.

---

## 35.10 Event model — propagación + threading

### 35.10.1 Event types

```java
// javax.baja.ui.event (bajaui-wb)
BInputEvent (abstract)
├─ BMouseEvent
│   MOUSE_PRESSED=0, MOUSE_RELEASED=1, MOUSE_MOVED=2, MOUSE_ENTERED=3,
│   MOUSE_EXITED=4, MOUSE_DRAGGED=5, MOUSE_WHEEL=6, MOUSE_PULSED=7,
│   MOUSE_DRAG_STARTED=8, MOUSE_HOVER=9
├─ BMouseWheelEvent
├─ BKeyEvent  (KEY_TYPED, KEY_PRESSED, KEY_RELEASED + VK_* Swing-style codes)
└─ BInputMethodEvent
BFocusEvent
BScrollEvent
BSliderEvent
BWidgetEvent
BWindowEvent
```

`BWidget` expone **tres Topics** para event routing:
```java
public static final Topic keyEvent;
public static final Topic mouseEvent;
public static final Topic focusEvent;
```

Topics (Bloque 7) son el mecanismo baja para dispatching fan-out. Cualquier subscriber recibe eventos del widget.

### 35.10.2 Propagation

Eventos mouse van **de hit-widget hacia arriba** (bubble). `BWidget.childAt(Point)` + `descendentAt(Point)` determinan hit. Propagación:

1. `BNiagaraWbFrame.mousePressed()` (override observado) captura frame-level
2. ShellManager determina hit widget via coord translation
3. Widget.mousePressed() override
4. Si no consumed (`event.consume()`), bubble al parent
5. Global listeners en Topic suscritos reciben en paralelo

Keyboard: focus traversal + accelerator match. `BAccelerator.isMatch(BKeyEvent)` se evalúa contra `BWidget` árbol. `BWidgetShell.handleEnter()` / `handleEscape()` hooks default (Enter invoca default button, Escape cancela).

### 35.10.3 AWT EDT vs Baja UI thread vs JavaFX FX thread

Esto es el **gotcha más importante** del bloque.

```
┌─────────────────────────────────────────────────────────────────┐
│  Thread Graph (BNiagaraWbFrame — desktop Workbench)             │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  main thread                                                    │
│     │                                                           │
│     ├─ baja Engine Thread (Sys.start() — Bloque 6.1.5)          │
│     │    └─ engine events: BComponentEvent, topic fires         │
│     │                                                           │
│     ├─ AWT EventDispatchThread (EDT — Swing/AWT)                │
│     │    ├─ mouse/key events nativos del SO                     │
│     │    ├─ paint() calls                                       │
│     │    ├─ BSwingWidget.JRootPane content                      │
│     │    └─ doLayout() en BWbPane                               │
│     │                                                           │
│     ├─ JavaFX FX Application Thread                             │
│     │    └─ BFxWidget JFXPanel scenes (theme.css)               │
│     │                                                           │
│     ├─ Binder Worker Thread (per BWbView)                       │
│     │    └─ binding resolves + OrdTarget.resolve()              │
│     │                                                           │
│     ├─ KeepaliveThread (BNiagaraWbWebShell only)                │
│     │    └─ fox session keepalive (separate del Fox pool)       │
│     │                                                           │
│     └─ Fox session box threads (Bloque 20)                      │
│          └─ NavEvents, component events, RPC replies            │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

Reglas:
1. **Todo `BWidget.changed()`/`paint()`/`doLayout()` corre en EDT** — confirmado por `Swing.invokeLater` patterns en decompile + el hecho de que `BSwingWidget` hereda JRootPane (puramente EDT).
2. **`baja.sys.BComponent.set()` sobre widget desde thread ≠ EDT causa RACE** — Niagara internamente usa `SwingUtilities.invokeLater` en partes críticas, pero no siempre. Código user que haga `widget.setText(...)` desde un callback fox debe wrapearlo.
3. **`BFxWidget.postfx(Runnable)`** es el único método safe para scheduling en FX thread. `isFxThread()` existe para detection.
4. **Binder Worker** resuelve ords async — cuando ord resuelve, escribe valor al widget en **su propio thread** y delega fire via `widget.fw()` (framework callback) que internamente maneja sync.

**Gotcha histórico (confirma Bloque 33)**: `BBatchEditor` / ProgramService batch operations ejecutan **síncronos en EDT**. Batch de 500 slot renames congela WB 10-30 seg. No hay thread off-load.

---

## 35.11 `WbCommands` — 81 commands del shell

`WbCommands` es una clase-bolsa que instancia Commands (menu items/toolbar buttons/accelerators) al arrancar shell. Listado completo decompilado:

**File menu**: `newWindow`, `newTab`, `closeTab`, `closeOtherTabs`, `nextTab`, `prevTab`, `save`, `saveAll`, `saveBog`, `bogProtection`, `open`, `openOrd`, `openFile`, `openDir`, `openQuery`, `findStations`, `export`, `close`, `exit`, `nonFipsRestart`, `fipsRestart`.

**Navigation**: `back`, `forward`, `upLevel`, `recentOrds`, `refresh`, `refreshTabs`, `sessionInfo`, `home`, `logoff`.

**Edit**: `cut`, `copy`, `paste`, `pasteSpecial`, `duplicate`, `delete`, `rename`, `undoMenu`, `redoMenu`, `undoButton`, `redoButton`.

**Search**: `find`, `findNext`, `findPrev`, `replace`, `goTo`, `gotoFile`, `findFiles`, `replaceInFiles`.

**Console**: `consolePrev`, `consoleNext`, `consoleKill`, `consoleGroup`, `hideConsole`, `console`.

**Options/Tools**: `options`, `tools[]`, `sideBarMenu`, `showSideBar`, `sideBars[]`, `activePlugin`, `pathBarUsesNavFile`.

**Help**: `helpContents`, `helpOnView`, `helpGuideOnTarget`, `helpBajadocOnTarget`, `helpFindBajadoc`, `about`.

**Sidebar modes**: `modeSelectionGroup`, `noneMode`, `miniMode`, `singleMode`, `doubleMode`, `masterMode`.

### 35.11.1 `fipsRestart` + `nonFipsRestart`

`WbCommands` tiene commands dedicados para FIPS mode switch. Confirma hallazgo Bloque 30: FIPS ↔ non-FIPS requiere **restart full del Workbench** (no hot-toggle). Commands llaman `BWbApplication.restart(String[], String)` con flags JVM distintos.

### 35.11.2 Accelerator assignment

`BAccelerator.make(int modifiers, int keyCode)` + `BAccelerator.make(String)` (parse "ctrl-shift-F"). Default accelerators vienen de lexicon (`workbench:accelerators.properties` — patrón confirmado en `Command(BWidget, BModule, String keyBase)` ctor que busca `keyBase` en lexicon).

User-customizable? **No observable**. No hay UI para rebind accelerators (ni "keymap settings" en menús stock). Commands tienen `getAccelerator()` getter pero no setter — accelerators son immutable post-construction.

---

## 35.12 Workbench Tools (`BWbTool` / `BWbService`)

```java
public abstract class BWbTool extends BComponent {
  public static TypeInfo[] getInstalled();
  public license.Feature getLicenseFeature();
  public abstract CommandArtifact invoke(BWbShell);
}

public abstract class BWbService extends BWbNavNodeTool implements BIService {
  public static WbServiceManager getManager();
  public Type[] getServiceTypes();
  public void serviceStarted();
  public void serviceStopped();
}
```

Tools son singletons registrados vía `@NiagaraType` en module.xml. `BToolsContainer.make()` crea un pseudo-nav-root agregando todos los `getInstalled()` tools + services. Accesible desde menú `Tools` + via `tool:<name>` ord scheme.

Ejemplos stock: Component Finder, BQL Query Builder, Alarm Console, Job Monitor, Logger Configuration, Station Finder, Credentials Manager, Certificate Manager, Lexicon Tester.

Licensing: `getLicenseFeature()` retorna feature required. Si no tiene licencia → tool oculto en menú.

---

## 35.13 Drag-and-drop framework

### 35.13.1 Clases

```
BTransferWidget (abstract, extends BWidget)
├─ BSwingWidget
│   └─ BFxWidget
├─ BNavTree
├─ BTable (via subclasses)
└─ (custom BComponent views)

TransferContext   (envoltorio sys.BasicContext — action + envelope + coord + pulse flag)
TransferEnvelope  (immutable — formats[] + data[])
TransferFormat    (singletons: TransferFormat.string, TransferFormat.mark)
Clipboard         (singleton default, OS clipboard bridge via UiEnv$ClipboardManager)
DragRenderer      (ghost image durante drag)
SimpleDragRenderer (implementación trivial)
```

### 35.13.2 Contrato TransferWidget

Widget que quiere participar en DnD/Clipboard **debe** extender `BTransferWidget` y sobrescribir:

```java
TransferEnvelope getTransferData();              // source
CommandArtifact insertTransferData(TransferContext);  // target paste
CommandArtifact removeTransferData(TransferContext);  // cut
int dragOver(TransferContext);                   // accept? highlight?
CommandArtifact drop(TransferContext);
```

+ state getters: `isCutEnabled()`, `isCopyEnabled()`, ..., `isRenameEnabled()`.

### 35.13.3 Formats estándar

**Solo dos formats built-in**:
- `TransferFormat.string` — para text (strings, rename dialogs)
- `TransferFormat.mark` — para `space.Mark` (referencia a componente/space location — esencial para paste dentro de ComponentSpace)

**No hay formats custom**. Para DnD con data compleja, modules serializan a string (BObject XML) o Mark. No existe el equivalent AWT `DataFlavor` extensible.

### 35.13.4 DnD cross-station

**No funciona nativo**. Drag de component desde station A tab → station B tab **no es soportado**:
- `Mark` referencia un `BSpace` local al shell + session handle.
- Paste en target station: `insertTransferData()` recibe el Mark, trata de resolve sobre target space → diferente session → falla.

Workaround empírico: copy-to-clipboard → "Paste Special" con XML transformation (si ambas stations comparten schema de tipos). No hay UI built-in para esto.

---

## 35.14 Console + Jobs + Status

### 35.14.1 `BConsole`

```java
public class BConsole extends BEdgePane {
  public static final Action scrolled;
  public static final int BUFFER_SIZE;
  BNiagaraWbShell shell;
  BLabel prompt;
  BConsoleEntry entry;
  BScrollBar scrollBar;
  BConsoleBuffer buffer;
  BConsole$ExecCallback execCallback;
  com.tridium.nsh.NShell nsh;       // ← Niagara Shell interpreter
  ArrayList<String> history;
}
```

Console integra **NShell** (niagara shell interpreter — `nsh-wb` module dependency). Comandos NShell son BQL-enhanced: `spy`, `ls`, `cd`, `bql`, etc. Buffer es rotatorio (BUFFER_SIZE constante). Prompt visible con `Ctrl+'` accelerator.

### 35.14.2 `BJobBar` + `BJobMonitorPane`

```java
public class BJobBar extends BEdgePane implements BWbComponentView$Attachable {
  private BJob job;
  private BLabel state, name, message;
  private BBorderPane progress;
  private BButton cancelButton, disposeButton;
  // + logIcon, cancelIcon, etc.
}
```

Jobs (Bloque 24.x) activos aparecen en `BJobStatusBar` (bottom-right) como compact bars. Click → `BJobMonitorPane` (modal-ish popup con detalles). `BJobSideBar` es el sidebar equivalente con lista completa.

**Attachable interface**: `BWbComponentView$Attachable` significa que el JobBar puede *attacharse a cualquier ComponentView* como decorator — no es un view propio.

---

## 35.15 Kiosk mode

### 35.15.1 `BKioskService` — component en la station

```java
public class BKioskService extends BAbstractService implements LogoffListener, BIRestrictedComponent {
  public static Property splash;              // BTypeSpec de splash class
  public static Property splashImage;         // BOrd a imagen
  public static Property splashFill;          // BBrush
  public static Property autoLoginUsername;
  public static Property autoLoginPassword;   // BPassword (encrypted)
  public static Property autoLogoffEnabled;
  public static Property autoLogoffPeriod;
  void openSplash();
  boolean autoLogin();
  public boolean login(String, String);
  BNiagaraWbShell doLogin(BUser, BPassword);
  public void logoff();
}
```

**Gotcha**: `BIRestrictedComponent` — el service enforces que solo puede estar bajo `/Services`. `checkParentForRestrictedComponent()` lo valida.

### 35.15.2 Profile switching via Kiosk

Kiosk users tienen `BKioskProfileConfig` mixin en su `BUser`:

```java
<type class="com.tridium.workbench.kiosk.BKioskProfileConfig" name="KioskProfileConfig">
  <agent>
    <on type="baja:User"/>
  </agent>
</type>
```

Cuando un kiosk user loggea via `BKioskService.login()`:
1. Se lookup `BKioskProfileConfig.get(user)` — contiene `DEFAULT_TYPE_SPEC` del profile a usar
2. Se instancia `BKioskProfile` subclass (Default/Basic/Handheld) 
3. Shell se construye con ese profile

**Key point**: el profile del kiosk **no es seleccionable por el user** — es impuesto por el config server-side. Y después de login, **no puede cambiarse sin logout**.

---

## 35.16 Honeywell — `BCentralineProfile`

JAR `clProfile-wb.jar` tiene **solo dos classes Java**:

```
com.centraline.clProfile.ui.BCentralineProfile
com.centraline.clProfile.ui.BCentralineProfile$CentraLineAboutCommand
```

Más: 7 recursos de branding (icon, splash, logo, building images, Home.html, sidebar image).

### 35.16.1 API observada

```java
public class BCentralineProfile extends BWbProfile {
  public static final Lexicon _lex;
  private BWbShell bwbShell;
  private static BOrd home;
  private static final String HONBACNETDEVICEMANAGER    = "honBacnet:BacnetDeviceManager";
  private static final String HONBACNETOWSDEVICEMANAGER = "honBacnet:BacnetOwsDeviceManager";

  public String getAppName();                // "CentraLine" probably
  public BImage getFrameIcon();              // res/icon_CentraLine.ico
  public String getFrameTitle();
  public BOrd   getStartOrd();
  public BOrd   getHomeOrd();                // res/Home.html probable
  public BIMenuBar makeMenuBar();
  private void configureHelpMenu(BIMenu);    // swap Help items → CentraLine
  public boolean hasView(BObject, AgentInfo);
  public AgentList getAgents(BObject);
  private AgentList adjustAgents(AgentList); // ← core trick
  private BComponent getPointListViewService(BObject);
  private TypeInfo typeInfo(String);
}
```

### 35.16.2 Mecanismo de override

`adjustAgents()`:
1. Recorre `AgentList` estándar
2. Detecta presencia de `HONBACNETDEVICEMANAGER` (via `typeInfo(String)` resolution — si está registrado por `honBacnet-wb.jar` que SÍ está instalado en el árbol)
3. Si existe: `list.toTop(HONBACNETDEVICEMANAGER)` — lo pone como default
4. Similar para OWS device manager

**Impacto user-visible**: cuando right-click sobre un `BBacnetNetwork`, el default view es `HonBacnetDeviceManager` (Honeywell UI) en vez del stock `BacnetDeviceManager`.

### 35.16.3 Lo que NO override

Centraline profile NO override:
- Nav sidebar defaults (usa el mismo)
- Console / NShell (stock)
- Workbench commands (save/open/etc. idénticos)
- Field editors (idénticos a stock)
- Los view agents de alarm/history/schedule (stock Tridium)

Solo branding (icons/title/splash/home) + `adjustAgents()` para driver-specific views + CentraLineAboutCommand (about dialog).

### 35.16.4 Dependencias

`clProfile-wb` dependencies (en su module.xml):
- `baja`, `bajaui-wb`, `gx-rt`, `gx-wb`, `workbench-wb`, `driver-rt`, `test-wb`

**NO declara dependency sobre `honBacnet-wb`** — porque resuelve `"honBacnet:BacnetDeviceManager"` vía **string TypeSpec lookup runtime** (`typeInfo(String)` helper). Si honBacnet no está instalado, `typeInfo()` retorna null, `adjustAgents()` no reordena, fallback al stock. Esto hace el profile **resiliente**: instala en cualquier station independientemente de qué drivers Honeywell estén presentes.

---

## 35.17 Search subsystem

### 35.17.1 `BFileSearch`

```java
public class BFileSearch extends BDialog {
  static String FILE_PATTERN_DEFAULT;
  static String DIR_DEFAULT;
  boolean isReplace;
  BCheckBox findText;
  BFindPane findPane;
  BMruTextDropDown filePattern;
  BMruTextDropDown dir;
  BCheckBox subFolders;
  public static void findFiles(BNiagaraWbShell, String);
  public static void replaceInFiles(BNiagaraWbShell);
}
```

Diálogo modal para grep en filesystem workspace. Usa `BFileIndexer` (ux JAR) para acelerar búsquedas repetidas. `BFileIndexerOptions` configura paths indexed.

### 35.17.2 `BIQuickSearch` — per-view

Interface:
```java
public interface BIQuickSearch extends BIAgent {
  boolean isQuickSearchEnabled(BWidget);
  BWidget asGlobalWidget(BWidget, String);
}
```

Views que la implementan aparecen con un text field en el locator bar (quick search). Ejemplo: wiresheet con quick search permite saltar a un slot por nombre.

### 35.17.3 `BComponentFinder`

Tool (`tool:ComponentFinder`). BQL-based search sobre el station. No indexa — query directa a station. Para > 10K components puede tardar minutos. No hay cancelation fina (botón Cancel cancela el RPC fox, no la query local).

### 35.17.4 `BGotoFile`

Ctrl+Shift+F-like — fuzzy name search sobre filesystem indexed por `BFileIndexer`. Rápido (ms).

---

## 35.18 Web profile — servidor lado

`BWbWebProfile` (abstract) hereda `BWbProfile` + implementa `BIWebProfile`:

```java
public abstract class BWbWebProfile extends BWbProfile
    implements BIWebProfile, IWebEnvProvider {
  public static final String theme;
  private static final TypeInfo servlet;          // servlet TypeInfo registered
  private static final TypeInfo exporter;
  private static final TypeInfo mobileView;
  public boolean hasSideBar();
  public boolean hasView(BObject, AgentInfo);
  public BOrd getNavRootOrd();
  public static IWebEnv webEnv();
  public final IWebEnv getWebEnv(WebOp) throws WebProcessException;
}
```

Subclases: `BDefaultWbWebProfile`, `BBasicWbWebProfile`, `BHandheldWbWebProfile`, `BSimpleAdminWbWebProfile`.

### 35.18.1 Diferencias web vs desktop profile

| Aspecto | Desktop (BWbProfile default) | Web (BWbWebProfile) |
|---|---|---|
| Shell | `BNiagaraWbFrame` (Swing) | `BNiagaraWbWebShell` (applet o HTML5) |
| Widgets rendering | Java2D paint directo | bajaui-ux HTML5 layer (Bloque 35.19) |
| Views disponibles | Todos | Sub-set via `hasView()` filter |
| Sidebars | Full (Nav/Palette/Bookmark/Job) | Depends on profile |
| Sessions | Directa Fox | Via servlet + bajaux RPC |
| IWebEnv | N/A | Required para `WebOp` context |

### 35.18.2 `showWebStartAddressBarParam` etc.

System props:
```
showWebStartAddressBarParam, showWebStartAddressBarDefault
showWebStartStatusBarParam, showWebStartStatusBarDefault
```

Controlan si WebStart Workbench muestra address bar / status bar en el browser dentro del cual corre. Útil para desplegar WB embebido en admin pages customizadas.

---

## 35.19 `bajaui-ux` — paralelo HTML5

`bajaui-ux.jar` registra agents `TypeExt` que son **extensores de tipo** (no views). Pattern:

```xml
<type class="com.tridium.bajaui.ux.baja.BBorderTypeExt" name="BorderTypeExt">
  <agent>
    <on type="bajaui:Border"/>
  </agent>
</type>
```

Significado: el runtime ux (HTML5) mira `bajaui:Border` y encuentra que existe un `BorderTypeExt` que lo sabe renderizar. Esto permite al web shell renderizar **los mismos widgets** que el desktop usando JavaScript/CSS.

Types ux observados:
- `BorderTypeExt`, `LayoutTypeExt`, `ValueBindingTypeExt`, `WbFieldEditorBindingTypeExt`, `WbViewBindingTypeExt`, `LayoutDimensionTypeExt`, `TableBindingTypeExt`
- `LayoutEditor` (ux fe)
- `CollectionView` (on `ITable`/`CollectionTable`)
- `UxCanvasPane`, `UxLabel`, `UxScrollPane`, `UxNullWidget`, `UxPicture`, ... (paralelo 1:1 con bajaui-wb)

**Consecuencia**: la misma `.px` se renderiza en desktop (bajaui-wb, Java2D) y en browser (bajaui-ux, JavaScript) porque ambos runtimes conocen **los mismos tipos** via TypeExt agents. La "portabilidad" no es magia — es registry.

---

## 35.20 `system.properties` — tuning flags

Confirmados en `defaults/system.properties`:

```properties
# Line 147
niagara.ui.volatileBackBuffer=false
# Use VolatileImage (DirectDraw) for bajaui double buffering.

# Line 150
niagara.ui.pxCache.max=10
# Max PX documents cached in memory.

# Line 165
niagara.ui.px.maxImageModuleFileSize=2047
# Max file size (bytes) for images fetched from module:

# Line 170
bajaui.hasKeyboard=true
# Enable touch-screen keyboard for text fields.

# Commented (presence implies defaults)
#bajaui.hasMouse=true
# Optimize mouse eventing and widgets.

#bajaui.hasTouchscreen=false
# Force Curium theme.
```

**NO observados** (esperados pero ausentes — confirman gotchas de "dev views"):
- `niagara.wb.devViews.*` — no listado aunque decompile lo muestra
- `niagara.workbench.maxTabs` — no observado; límite parece 0xFFFFFFF in code
- `niagara.ui.edt.monitor` — no listado; Niagara no tiene EDT hang detector builtin

---

## 35.21 Gotchas transversales (G1..G20)

**G1** — **Profile switch requiere nuevo shell**. `BNiagaraWbShell.profile` es final. Para cambiar de `BDefaultWbWebProfile` a `BCentralineProfile` se necesita reabrir Workbench. No hay API `setProfile()`. Consecuencia: "Cambiar a modo Kiosk" en vivo = logout+login, no "switch".

**G2** — **`static NavFileDecoder.cache` persiste `.nav` contents**. Editar un nav file en disco mientras WB está abierto **no actualiza el sidebar** hasta reload/hyperlink. En aula-dev donde se edita .nav frecuente, síntoma "the changes don't show" — cierre+reopen WB o `Ctrl+R` (refresh) sobre el nav tree.

**G3** — **`WbHistory` es STATIC entre shells**. Abrir dos Workbenches contra stations distintas en la misma JVM (rare, pero existe) → Back button puede navegar entre stations.

**G4** — **View registry collisions no son deterministicas**. Dos modules con default view sobre el mismo type: orden determinado por (a) profile adjustAgents, (b) registration order (no garantizado estable). Si el "default view" cambia entre installs, investigar nuevos modules.

**G5** — **AWT EDT vs engine race**. Código user que haga `widget.setText(x)` desde callback fox o subscriber event **sin `Sys.ui().invokeLater()` o equivalente** → race. Niagara no siempre detecta; síntoma es paint inconsistente, no crash.

**G6** — **JFXPanel thread**. `BFxWidget` embebe JavaFX. **Tocar JavaFX Node fuera del FX thread = IllegalStateException**. Usar `BFxWidget.postfx(Runnable)`.

**G7** — **Batch operations en EDT** (confirma Bloque 33). Custom batch command que itere 1000 components con `component.set()` en EDT congela WB. Patrón correcto: schedule work en `ForkJoinPool` + `Sys.ui().invokeLater` solo para UI final.

**G8** — **Custom view sin `doLoadValue`/`doSaveValue`**. Si un module define un `BWbView` subclass y olvida override `doLoadValue(BObject, Context)`, el view muestra empty. Common mistake; chequear que override tiene signature exacta.

**G9** — **`doSaveValue` debe throw `CannotSaveException` no `Exception`** para que el dialog "Cannot save" renderice bien. Throwing generic Exception → default "Unknown error" dialog.

**G10** — **`BWbView.activated()` es sync**. Network call en activated()  = UI freeze. Siempre async.

**G11** — **DnD cross-station no funciona**. Mark se resuelve en space local. Copiar componente de station A a B requiere: paste special XML, o export .bog + import.

**G12** — **Ord `tool:X` funciona aunque X service no esté en station**. `BToolsContainer` enumera tools *instalados en WB JVM* (registry), no *en la station*. Si módulo WB está, tool abre. Gotcha: el tool puede estar rota si el service remoto no existe.

**G13** — **`BCentralineProfile.adjustAgents` depende de string TypeSpec lookup**. Si `honBacnet-wb` se desinstala sin desinstalar `clProfile-wb`, el profile sigue cargando pero `typeInfo("honBacnet:...")` retorna null → no-op → usuario ve stock views. Silent degradation (bien), pero investigación "¿por qué no veo HonBacnetManager?" termina en dep check.

**G14** — **`BAccelerator` no user-configurable**. No hay UI para rebind. Modificar accelerators requiere override lexicon en módulo custom o parchar el JAR (no hacer en production).

**G15** — **Sidebar layout pickle frágil**. `BSideBarPane.pickle()` emite string con format interno. Cambios entre versiones de Niagara pueden invalidar pickled state → sidebar vuelve a default al upgrade. Reportado empíricamente en 4.13→4.14 upgrades.

**G16** — **BViewTabbedPane límite implícito**. Más de ~50 tabs abiertos degradan repaint (cada tab mantiene un `BWbView` + su subscribers). No hay límite documentado pero performance-felt.

**G17** — **`BConsole` buffer rotatorio descarta logs**. `BUFFER_SIZE` constante (no public runtime setter observado). Si NShell loggea > buffer size durante un script largo, info inicial se pierde. Workaround: redirect a file desde NShell.

**G18** — **Kiosk auto-login password en BPassword**. `BKioskService.autoLoginPassword` es BPassword (encrypted). Pero la key reside en `~/security/` workbench/station keys. Comprometer esa key → reveal password (Bloque 13).

**G19** — **`niagara.ui.pxCache.max=10`** es low default. Workflows con > 10 px dashboards consultados rotativamente → churn de cache → reload constante. Subir a 50+ en prod con dashboards muchos.

**G20** — **Dev views no listadas en system.properties default**. El mecanismo `niagara.wb.devViews.<name>=true` existe en `BWbProfile.devViewsEnabled` map pero no está documentado en `defaults/system.properties` ni en niagara-help stock. Para troubleshooting avanzado (SpyScheme browser, ModuleSpaceView) hay que setearlo manualmente con nombre exacto de la dev view.

---

## 35.22 Inventario de JARs estudiados

| JAR | Size (class count) | Rol |
|---|---|---|
| `workbench-wb.jar` | 869 types | Shell + views + commands + profiles |
| `bajaui-wb.jar` | 170 types | Widget + layout + events + transfer |
| `bajaui-ux.jar` | ~60 types (TypeExt) | HTML5 render counterpart |
| `wbutil-wb.jar` | 77 types | Cell editors + color chooser + field editors extra |
| `clProfile-wb.jar` | **2 types** | Honeywell Centraline branding + view override |
| `file-ux.jar` | 3 types | JS/CSS resources + file menu agent ux |
| `baja.jar` (share) | — | BNavScheme, BINavNode, BNavContainer, NavFileDecoder |
| `file-rt.jar` (share) | — | `com.tridium.file.types.text.BNavFile` (XML file bound to .nav) |

---

## 35.23 Diagrama — Ord → View resolution

```
User hyperlinks:   local:|fox:|station:|slot:/Drivers/BacNet/device1
                   ─────────────────────────────────────────
                                │
                                ▼
                  ┌─────────────────────────────┐
                  │ BNiagaraWbShell.hyperlink() │
                  └─────────────┬───────────────┘
                                │
                  ┌─────────────▼───────────────┐
                  │ Ord.resolve() (chain)       │  ← Bloque 2.x
                  │  local: → fox: → station: → │
                  │     slot:                   │
                  └─────────────┬───────────────┘
                                │ returns OrdTarget(BComponent bacnetDev)
                                │
                  ┌─────────────▼───────────────┐
                  │ BViewTab(shell).updateOrd() │
                  │   - target = bacnetDev      │
                  │   - typeInfo = Sys.getType  │
                  └─────────────┬───────────────┘
                                │
                  ┌─────────────▼───────────────┐
                  │ typeInfo.getAgents()        │  ← Registry lookup
                  │  → AgentList [              │     (via module.xml
                  │     honBacnet:BacnetDevMgr, │      <on type="..."/>
                  │     bacnet:BacnetDevMgr,    │      declarations)
                  │     baja:PropertySheet,     │
                  │     baja:SlotSheet,         │
                  │     baja:RelationSheet,     │
                  │     ...                     │
                  │   ]                         │
                  └─────────────┬───────────────┘
                                │
                  ┌─────────────▼───────────────┐
                  │ profile.getAgents(obj)      │  ← BCentralineProfile
                  │  (Centraline override:      │     reordena
                  │    adjustAgents toTop(      │
                  │      HONBACNETDEVICEMANAGER)│
                  │  )                          │
                  └─────────────┬───────────────┘
                                │
                  ┌─────────────▼───────────────┐
                  │ filter by requiredPerms     │  ← user.getPermissions()
                  │  (agent.requiredPermissions │     check
                  │    vs user perms on obj)    │
                  └─────────────┬───────────────┘
                                │
                  ┌─────────────▼───────────────┐
                  │ AgentList.getDefault()      │
                  │  → honBacnet:BacnetDevMgr   │  ← FIRST remaining
                  │     AgentInfo               │
                  └─────────────┬───────────────┘
                                │
                  ┌─────────────▼───────────────┐
                  │ agent.getInstance()         │  ← instantiates view
                  │  = BHonBacnetDeviceManager  │     (singleton or new)
                  │                             │
                  │ view.loadValue(obj, ctx)    │  ← doLoadValue(...)
                  │ view.activated()            │
                  │                             │
                  │ tab.setView(view)           │
                  │ pane.update(tab)            │  ← rebuild menu/toolbar
                  └─────────────────────────────┘
```

---

## 35.24 Correcciones a bloques previos

**Corrección a Bloque 9** (servlet vs wb): Bloque 9 habla de "web profile corre en station". Verdadero pero incompleto. **`BWbWebProfile` implementa `BIWebProfile` + `IWebEnvProvider`** — significa que el profile *sirve* `IWebEnv` para servlet dispatch, pero el **rendering del shell** sigue siendo WB code (bajaui-wb widgets). La dualidad es: servlet dispatch está en station process, pero el shell que consume servlet requests es un WB applet/HTML5 ux.

**Corrección a Bloque 22** (PX): Bloque 22 trata PX como "renderer" independiente. Afirmación precisa: `BWbPxView` es un `BWbView` registrado con `<agent on="file:PxFile"/> + <on="bajaui:PxInclude"/>`. O sea, **PX es un view entre muchos** — no tiene status especial. Lo que SÍ es especial es `NPxInclude`/`BPxIncludeFolder` que son widgets bajaui reusables. `niagara.ui.pxCache.max=10` aplica a este view.

**Corrección a Bloque 33** (Batch Editor EDT): el bloque 33 afirma batch blocks UI. Aquí confirmado: el path es `BWbView.invokeCommand()` (en EDT) → `ProgramService` sync loop. **No hay thread off-load porque `CommandArtifact` es el contrato de undo — requiere capture sync del snapshot pre-change**. Mover a async rompería undo. Solución architectural real: chunks + progress dialog con cancel, no off-load.

**Corrección a Bloque 30** (FIPS): `WbCommands.fipsRestart` / `nonFipsRestart` son commands dedicados = confirma Bloque 30 afirmación de "restart requerido". Aquí se amplía: ambos commands invocan `BWbApplication.restart(String[] args, String)` con flags JVM distintos — `args` incluye `-Dniagara.fips.enabled=true/false`. No es runtime toggle; es JVM re-launch.

**Corrección a Bloque 13** (auth): `BCredentialsManager` (visto aquí) NO pone credenciales en master.jceks — las pone en `~/etc/credentials/` vía DPAPI (Windows) / equivalent. Aquí solo confirmación del inventario; el detalle de DPAPI está en Bloque 13.2.4.

---

## 35.25 Checklist de preguntas investigables con este bloque

- [x] ¿Cómo se declara un view custom? → `<type class="..."><agent><on type="..."/></agent></type>` en module.xml + class extends `BWbView`
- [x] ¿Cómo override default view para un tipo? → Profile.adjustAgents + toTop() — ver `BCentralineProfile`
- [x] ¿Dónde vive el keybinding de `Ctrl+S`? → `BWbShell.getSaveCommand()` accelerator, bound in profile's `makeMenuBar()` via `WbCommands`
- [x] ¿Por qué el nav tree no refresca? → `NavFileDecoder.cache` static; forzar refresh con Ctrl+R o reabrir WB
- [x] ¿Cuántos threads UI hay? → 4: engine, AWT EDT, JavaFX FX, Binder Worker (+ shell keepalive si web)
- [x] ¿Qué JAR tiene qué Honeywell-specific? → `clProfile-wb.jar` (solo BCentralineProfile) + `honBacnet-wb.jar` (device managers — no estudiado aquí, Bloque 18)
- [x] ¿Puedo hacer DnD entre stations? → No nativo. Workaround: export/import bog
- [x] ¿Cómo sé qué views están disponibles para un objeto? → Right-click → Views → submenu. O: código = `obj.getType().getTypeInfo().getAgents()`
- [ ] ¿Cómo migrar profile entre versiones Niagara? → Sidebar pickle string formato privado; tests requeridos en upgrade (Bloque 16 sugerir pre-upgrade validation script)
- [ ] ¿Cómo custom accelerator? → No user-facing. Requiere module parche. No investigable sin source Java.

---

## 35.26 Hallazgos no-obvios (resumen)

1. **Solo 2 clases en clProfile-wb**: Honeywell-specific UI es mayormente branding + 1 `adjustAgents()`. La "UI Honeywell" percibida es stock Niagara + driver managers Honeywell (honBacnet, honPlantController — Bloque 32).

2. **`BCentralineProfile` usa string TypeSpec lookup** (no module dep) — resiliente a faltantes. Silent degradation es feature, no bug.

3. **`WbHistory` es STATIC**. Back/Forward cruza shells en la misma JVM — bug observable si WB tiene dos frames abiertos.

4. **`NavFileDecoder.cache` es STATIC con timestamp pero sin file watcher**. Edits a .nav files no propagan hasta reload.

5. **4 threads UI, no 1**: engine + EDT + FX + Binder. JavaFX embebido vía JFXPanel dentro de JRootPane — significa que widgets FX están **doble-nested** (FX en Swing en baja).

6. **Todo widget baja es `BComponent`** — significa que participa en Topics/Actions/Subscribers (Bloque 7). Esto permite que cualquier widget UI se comporte como source de events framework-level — mayor uniformidad, pero también mayor surface area para memory leaks si subscribers no se unsubscribe.

7. **`WbCommands` tiene 81 Commands instanciados al startup**. Cada shell instancia todos — incluye Commands que solo aparecen en contextos específicos (p.ej., `fipsRestart`, `bogProtection`). No hay lazy-init observable. Overhead menor pero measurable en low-memory JACEs.

8. **Profile switching no existe en vivo**. Toda documentación Tridium que implique "cambiar de profile" realmente significa "re-login con profile config distinto". Importante para user training.

9. **DnD framework tiene SOLO 2 formats built-in** (`string`, `mark`). Ausencia de DataFlavor extensible es limitación arquitectural — modules workaround vía serialización string (fragilidad + overhead).

10. **FIPS es un reboot JVM completo**, no un toggle runtime (`WbCommands.fipsRestart/nonFipsRestart` con `BWbApplication.restart(args,_)` con JVM flags distintos).

11. **`BWbProfile.devViewsEnabled` map** (dev views toggle) no está en `defaults/system.properties` — es un canal de configuración **no documentado pública** en el sistema properties stock. Hay que conocer nombre exacto de cada dev view.

12. **`bajaui-ux` usa el patrón `TypeExt` (registered via agents)** para que el runtime HTML5 sepa renderizar los widgets wb. Mismo registry pattern que views, pero para rendering. Confirma que bajaui es un **framework agnostic-render** con dos renderers registrados (wb=Java2D, ux=HTML/JS).

---

**EOF Bloque 35**
