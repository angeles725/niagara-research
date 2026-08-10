# Bloque 421 — `webEditors-ux`: la capa web de field editors en Niagara N4

> Research de **`webEditors-ux`** — foco `px-tail`, gap **P1** (HIGH). Documenta el módulo que
> provee los field editors de la interfaz web de Niagara: el modelo de emparejamiento tipo↔editor,
> el patrón arquitectural Java (shim descriptor), el JS bundle, el ciclo de vida sobre bajaux, y las
> familias de editores que provee. El módulo nunca fue sujeto a pesar de ser la base nombrada en
> 8 bloques previos ([Bloque 199]: 7 menciones directas).
>
> **Version del sujeto**: `webEditors-ux` 4.14.0.162 (build 2024-05-28, `META-INF/module.xml:1`).
>
> **Sources (decompilado Vineflower, READ-ONLY)**:
> `/home/cristian/modules/Prototipos/modulos/organized/webEditors/webEditors-ux/vineflower/`
> (citas `file:line` relativas al directorio base, e.g. `javax/baja/webeditors/ux/BWebEditorsJsBuild.java:14`).
> `module.xml` leído desde `META-INF/module.xml`.
>
> **Method**: lectura directa de decompilado (95 clases vineflower); conteo MEDIDO; grepping
> estructural para taxonomía completa de `@AgentOn`. Sin re-decompilación.
>
> **Markers** (METHODOLOGY §3): `[CERT]` fuente primaria local (`file:line`) · `[INFER]` deducción.
>
> **Connections**: [Bloque 199] (webChart sobre webEditors — 7 menciones), [Bloque 202] (kitPx FE
> Wb↔Ux — patrón shim en kitPx), [Bloque 204] (bajaux Widget lifecycle + puente rt→web), [Bloque 214]
> (field editors de converters — misma mecánica `@AgentOn`, lado Workbench).

---

## 421.1 — Estructura del módulo: 95 clases, 17 paquetes `[CERT]`

El módulo `webEditors-ux` (runtime profile `ux`, autoload) contiene exactamente **95 clases** sobre
`vineflower/` — número MEDIDO, sin multiplicar pipelines duplicados (§ método). Se distribuye en 17
paquetes activos:

| Paquete | Clases | Rol |
|---|---|---|
| `com.tridium.webeditors.ux.fe.baja` | **49** | Editores de tipos baja/bql/gx/lonworks/control/bacnet/net |
| `com.tridium.webeditors.ux.wb.util` | 14 | Editores auxiliares (auth, usuarios, perfiles, roles) |
| `com.tridium.webeditors.ux.views` | 6 | Vistas property-sheet y managers de usuarios/roles |
| `com.tridium.webeditors.ux.servlets` | 5 | RPC handlers HTTP/box para resolución server-side |
| `com.tridium.webeditors.ux.menu` | 3 | Menu agents web para Component/BOG/NavNode |
| `javax.baja.webeditors.ux` | 2 | API pública: JS bundle + CSS bundle |
| `com.tridium.webeditors.ux.wb.file` | 2 | Destinos de descarga/transferencia de archivos |
| `com.tridium.webeditors.ux.util` | 2 | Interfaces utilitarias (`BIJavaScriptOrdChooser`, `BIJavaScriptDecorator`) |
| `com.tridium.webeditors.ux.transform` | 2 | Destinos de transformación (file download, station file) |
| `com.tridium.webeditors.ux.ssc` | 2 | SSC handler para `BTypeConfig` |
| `com.tridium.webeditors.ux.baja` | 2 | Type-extensions (`PasswordStrength`, `WebProfileConfig`) |
| `javax.baja.webeditors.mgr` | 1 | Interfaz `BIJavaScriptMgrAgent` |
| `javax.baja.webeditors.menu` | 1 | Interfaz `BIJavaScriptMenuAgent` |
| `com.tridium.webeditors.ux.wb.profile` | 1 | Gestión de perfil web |
| `com.tridium.webeditors.ux.wb.job` | 1 | Job-side integration |
| `com.tridium.webeditors.ux.wb.composite` | 1 | Editor compuesto (`BCompositeUxEditor`) |
| `com.tridium.webeditors.ux.ui.enums` | 1 | (enum auxiliar de UI) |

El paquete dominante `fe/baja` concentra el 52 % de las clases y es la CAPA CENTRAL de editores.

Dependencias declaradas en `module.xml`: `bajaux-rt`, `bajaux-ux`, `bajaScript-ux`, `js-ux`,
`web-rt`, `baja`, `control-rt`, `gx-rt`, `bql-rt`, más módulos de alarm/export/history/fox/platform.
`[CERT]` `META-INF/module.xml:3-22`.

## 421.2 — El patrón canónico: `BSingleton` + `BIJavaScript` + `@AgentOn` + `JsInfo` `[CERT]`

Todos los field editors del paquete `fe/baja` (y la mayoría del módulo) siguen el **mismo template
de 38 líneas**. `BBooleanEditor` es representativo:

```java
// BBooleanEditor.java:17-37
@NiagaraType(
   agent = {@AgentOn(
      types = {"baja:Boolean"}
   )}
)
@NiagaraSingleton
public class BBooleanEditor extends BSingleton implements BIJavaScript, BIFormFactorMini, BIOffline {
   public static final BBooleanEditor INSTANCE = new BBooleanEditor();
   public static final Type TYPE = Sys.loadType(BBooleanEditor.class);
   private static final JsInfo JS_INFO = JsInfo.make(
       BOrd.make("module://webEditors/rc/fe/baja/BooleanEditor.js"),
       BWebEditorsJsBuild.TYPE);
   // ...
   public JsInfo getJsInfo(Context cx) { return JS_INFO; }
}
```

`[CERT]` `com/tridium/webeditors/ux/fe/baja/BBooleanEditor.java:17-37`.

Los **cuatro elementos invariantes** del patrón:

1. **`BSingleton`** — NRE singleton; no hay estado de instancia, la clase es un descriptor.
2. **`BIJavaScript`** (de `javax.baja.web.js`) — la interfaz que declara `JsInfo getJsInfo(Context)`.
   El módulo NRE registra el tipo como agente de JS para su(s) tipo(s) baja.
3. **`@AgentOn(types={...})`** — el mecanismo de dispatch estándar de Niagara: el runtime resuelve
   el editor correcto para un slot de tipo X buscando el agente registrado sobre X en el registro NRE.
   Idéntica mecánica a la del Workbench (B214 §214.2) pero la clase resultante es radicalmente distinta.
4. **`JsInfo.make(BOrd, BuildType)`** — el descriptor inmutable que apunta al módulo JavaScript que
   implementa el editor; el `BOrd` es `module://webEditors/rc/fe/baja/<Nombre>.js`.

La clase Java no contiene lógica de UI: es una **declaración pura** de tipo↔JS-module.

## 421.3 — Diferencia arquitectural Wb↔Web: `BWbFieldEditor` vs `BIJavaScript` `[CERT]` + `[INFER]`

La capa Workbench (documentada en B202, B214) y la capa web de `webEditors` comparten el mecanismo
de dispatch (`@AgentOn`) pero difieren completamente en la base y el ciclo de vida:

| Dimensión | Wb field editor | Web field editor (`webEditors`) |
|---|---|---|
| Base Java | `BWbFieldEditor` (Swing) | `BSingleton` (sin UI Java) |
| Interfaz | `javax.baja.workbench.fieldeditor` | `BIJavaScript` (`javax.baja.web.js`) |
| Ciclo de vida | `doLoadValue(BObject)` / `doSaveValue()` | sólo `getJsInfo(Context)` |
| UI | Componente Swing concreto | módulo JavaScript (bajaux) |
| Método de dispatch | `BWbFieldEditor.makeFor(type)` | `BIJavaScript.forType(type,cx)` (bajaux) |
| Registro | `@AgentOn` en NRE | `@AgentOn` en NRE (idéntico) |

`[CERT]` base BWbFieldEditor: B214 §214.1, confirmado en `BBooleanEditor.java:23` (sin `extends
BWbFieldEditor`). `[INFER]` resolución JS-side vía `fe.getDefaultConstructor` — nombrado en B202
§202.3 como el mecanismo bajaux client-side de resolución de editors web (fuera del scope de este
módulo Java).

**Contraste con kitPx Ux** (B202 §202.3): `BUxGenericFieldEditor` (kitPx) implementa
`BIJavaScriptWidget` (sub-interfaz de `BIJavaScript`, usada en el ciclo bajaux Widget). Los editores
de `webEditors` implementan `BIJavaScript` directamente — la diferencia indica que el despacho de
`webEditors` opera a nivel de property-sheet (el sheet resuelve y monta los JS-modules) y NO pasa
por el Widget lifecycle de bajaux. `[CERT]` ausencia de `BIJavaScriptWidget` en todos los 95 archivos
de `webEditors-ux/vineflower/` (grep verificado). `[INFER]` la consecuencia de montaje.

## 421.4 — Familias de editores en `fe/baja`: 66 tipos y el editor de fallback `[CERT]`

El paquete `fe/baja` registra **66 tipos baja distintos** a través de sus 49 clases. El editor de
**fallback** `BDefaultSimpleEditor` cubre 26 tipos misceláneos en una sola declaración:

```java
// BDefaultSimpleEditor.java:18-20
@NiagaraType(
   agent = {@AgentOn(
      types = {"bacnet:BacnetOctetString", "baja:BitSet", "baja:ClassSpec", "baja:Dimension",
               "baja:NameMap", "baja:Uuid", "bajaui:Accelerator", "bql:BqlInterval",
               "gx:EllipseGeom", "gx:LineGeom", "gx:PolygonGeom", "lonIp:IpAddress",
               "lonIp:TimeStamp", "lonworks:AuthenticationKey", "lonworks:Broadcast",
               "lonworks:Implicit", "lonworks:Local", "lonworks:LonByteArray",
               "lonworks:LonRouteTable", "lonworks:LonString", "lonworks:ModifyFlags",
               "lonworks:NeuronId", "lonworks:ProgramId", "lonworks:SubnetNode",
               "net:InternetAddress", "orion:OrionTypeId", "orion:SchemaVersion"}
   )}
)}
```

`[CERT]` `com/tridium/webeditors/ux/fe/baja/BDefaultSimpleEditor.java:18-20`.

Los demás editores son 1-a-1 o 1-a-2 tipos. Familias principales:

| Familia | Tipos cubiertos | Editor(es) |
|---|---|---|
| Tiempo | `AbsTime`, `AbsTimeRange`, `Date`, `RelTime`, `Time`, `TimeRange` | 6 editores dedicados |
| Numérico | `Double`, `Float`, `Integer`, `Long` | `BNumericEditor`, `BIntegerEditor`, `BLongEditor` |
| Enum | `DynamicEnum`, `EnumRange`, `EnumSet`, `FrozenEnum` | 4 editores |
| Status/Control | `Status`, `StatusValue`, `Override` | 3 editores |
| Seguridad | `Password`, `Permissions`, `PermissionsMap`, `X509Certificate`, etc. | 8+ editores |
| Identidad | `Ord`, `OrdList`, `TypeSpec`, `Icon`, `Marker` | 5 editores |
| String/Bool | `String`, `Boolean` | 2 editores |
| Facets/Format | `Facets`, `Format`, `Unit` | 3 editores |
| Red/Protocolo | `lonworks:*`, `lonIp:*`, `bacnet:*`, `net:*` (via fallback) | `BDefaultSimpleEditor` |

**8 clases `fe/baja` sin `@AgentOn`**: son descriptores auxiliares no registrados como agentes
directos — editores de contraseña especializados (`BMgrStringEditor`, `BDefaultPasswordEditor`,
`BConfirmPasswordEditor`), editores de host (`BHostNameEditor`, `BHostOrdEditor`), el editor compacto
`BDynamicEnumCompactEditor`, `BFlexBlobEditor`, e `BIUserCredentialsEditor` (interfaz, no clase).
`[CERT]` `com/tridium/webeditors/ux/fe/baja/BMgrStringEditor.java:17` (`@NiagaraType` sin `@AgentOn`).

## 421.5 — Form factors: `BIFormFactorMini`, `Compact`, `Max` y `BIOffline` `[CERT]`

Niagara web usa interfaces de "form factor" para que el property sheet seleccione la variante de
widget adecuada según el contexto de renderizado:

| Interface | Semántica | Clases en `fe/baja` |
|---|---|---|
| `BIFormFactorMini` | Editor inline (celda de property sheet) | **todos** los 48 agentes |
| `BIFormFactorCompact` | Variante compacta (espacio reducido) | 12 clases (incluye `BOrdEditor`, `BFacetsEditor`, `BEnumRangeEditor`) |
| `BIFormFactorMax` | Vista completa (página propia) | vistas (`BPropertySheet`, `BMultiSheet`) |

`[CERT]` `BOrdEditor.java:31` (`implements BIJavaScript, BIFormFactorMini, BIFormFactorCompact, BIOffline`).

`BIOffline` marca un editor que puede funcionar sin conexión activa a la estación. **47 de 49**
clases `fe/baja` implementan `BIOffline`; las 2 excepciones son `BCertificateEditor` (online-only,
manipula certificados X.509 del station) e `BIUserCredentialsEditor` (interfaz). `[CERT]`
`BCertificateEditor.java:22` (sin `BIOffline` en la declaración de implements).

## 421.6 — El JS bundle: `BWebEditorsJsBuild` y sus dependencias `[CERT]`

La clase pública `BWebEditorsJsBuild` (en el paquete `javax.baja.webeditors.ux`) es el **punto de
entrada del JS bundle** del módulo. Extiende `BJsBuild` con:

```java
// BWebEditorsJsBuild.java:22-28
private BWebEditorsJsBuild() {
    super(
        "webEditors",
        BOrd.make("module://webEditors/rc/webEditors.built.min.js"),
        new Type[]{BBajauxJsBuild.TYPE, BExportJsBuild.TYPE, BWebEditorsCssResource.TYPE}
    );
}
```

`[CERT]` `javax/baja/webeditors/ux/BWebEditorsJsBuild.java:22-28`.

El bundle `webEditors.built.min.js` depende de **dos builds upstream**:
- `BBajauxJsBuild` — el build base de bajaux (Widget framework, B204)
- `BExportJsBuild` — el build del módulo export-ux

Y agrega la hoja de estilos `BWebEditorsCssResource` (7 CSS files: estructura FE, CompositeEditor,
PropertySheetDragSupport, ProfileEditor, NavTreeStyle, etc.). `[CERT]`
`javax/baja/webeditors/ux/BWebEditorsCssResource.java:20-32`.

Cada FE individual declara `JsInfo.make(BOrd, BWebEditorsJsBuild.TYPE)` — el `buildType` establece
la dependencia: el runtime sabe que cargar un FE requiere tener cargado el bundle `webEditors`. El
`BOrd.make("module://webEditors/rc/fe/baja/BooleanEditor.js")` es la ruta canónica al módulo JS
individual dentro del bundle. `[CERT]` `BBooleanEditor.java:26`.

## 421.7 — Las vistas del property sheet: `BPropertySheet` y `BMultiSheet` `[CERT]`

Las **vistas** web (equivalentes web del Wb property sheet) también siguen el patrón `BSingleton +
BIJavaScript + @AgentOn`, pero con `BIFormFactorMax` (vista completa, no inline):

**`BPropertySheet`** — el property sheet estándar, agente sobre `baja:Struct`:

```java
// BPropertySheet.java:16-26
@NiagaraType(
   agent = {@AgentOn(
      types = {"baja:Struct"},
      requiredPermissions = "r"
   )}
)
@NiagaraSingleton
public class BPropertySheet extends BSingleton implements BIFormFactorMax, BIOffline, BIPropertySheet {
   private static final JsInfo JS_INFO = JsInfo.make(
       BOrd.make("module://webEditors/rc/wb/PropertySheet.js"), BWebEditorsJsBuild.TYPE);
```

`[CERT]` `com/tridium/webeditors/ux/views/BPropertySheet.java:16-26`.

**`BMultiSheet`** — sheet multi-tab, agente sobre `baja:Component` y `workbench:PropertySheet`:
`[CERT]` `com/tridium/webeditors/ux/views/BMultiSheet.java:17-20`.

`BPropertySheet` es el **consumidor principal** de todos los FE de `fe/baja`: cuando un slot de un
`Struct` se muestra en la web, el property sheet resuelve el FE correspondiente al tipo del slot vía
el mecanismo de agentes, obtiene su `JsInfo`, y lo monta en el panel. `[INFER]` — el flujo de
resolución JS-side es fuera del scope Java de este módulo (B204 §204.5 documenta `forType`/`JsInfo`).

Las 4 vistas adicionales en `views/`:
- `BRoleManager` — agente sobre `baja:RoleService`, `wbutil:RoleManager`
- `BUserManager` — agente sobre `baja:UserService`, `wbutil:UserManager`
- `BUxCategoryBrowser` — agente sobre `baja:CategoryService`, `wbutil:Category*`, `baja:Component`
- `BIPropertySheet` — interfaz marker para las implementaciones de property sheet

## 421.8 — Extensiones del patrón: `@NiagaraRpc` como puente Java↔JS `[CERT]`

Algunos FE extienden el patrón con métodos `@NiagaraRpc` que el código JavaScript puede invocar
como llamadas server-side. Ejemplo clave: `BOrdEditor` expone un mapa de `BIJavaScriptOrdChooser`
disponibles (uno por esquema de ORD):

```java
// BOrdEditor.java:47-69
@NiagaraRpc(permissions = "unrestricted", transports = {@Transport(type = TransportType.box)})
public static JSONObject getJavaScriptOrdChoosersMap(Context cx) {
    JSONObject map = new JSONObject();
    for (TypeInfo info : Sys.getRegistry().getConcreteTypes(BIJavaScriptOrdChooser.TYPE.getTypeInfo())) {
        for (TypeInfo agentOn : info.getAgentInfo().getAgentOn()) {
            if (agentOn.is(BOrdScheme.TYPE)) {
                // map[scheme-id] = {dn, t}
            }
        }
    }
    return map;
}
```

`[CERT]` `com/tridium/webeditors/ux/fe/baja/BOrdEditor.java:47-69`.

`BIJavaScriptOrdChooser` (interfaz en `ux.util`) extiende `BIJavaScript` — es un subtipo de JS
agent que el `BOrdEditor` descubre en runtime vía el registro de tipos, permitiendo extensibilidad
del chooser de ORDs por terceros. `[CERT]` `com/tridium/webeditors/ux/util/BIJavaScriptOrdChooser.java:9`.

Los servlets RPC centrales en `servlets/`:
- `BComponentRpc` — get/set `BCategoryMask` para `BIProtected` (verificación permisos `adminWrite`)
- `BResolveServerSideRpc` — convierte valores BSON a string server-side con merge de facets
- `BTypeConfigRpc` — consulta/sincroniza `BTypeConfig` + `IConfigurable` vía JSON
- `BValueDocEncodingUtilRpc` — (encoding utilidades)
- `PaletteServlet` — servlet de paleta

`[CERT]` `servlets/BComponentRpc.java:25,48` (`@NiagaraRpc` getCategoryMask/setCategoryMask).

## 421.9 — Paquetes secundarios: menu, ssc, wb/util `[CERT]`

**Menu agents** (`menu/`, 3 clases) — el mismo patrón `BSingleton + BIJavaScriptMenuAgent + @AgentOn`
aplicado a la barra de menú web:
- `BComponentJavaScriptMenuAgent` — agente sobre `baja:Component`, JS `rc/wb/menu/componentMenuAgent.js`
- `BBogFileJavaScriptMenuAgent` — agente sobre `baja:BogFile`
- `BNavNodeJavaScriptMenuAgent` — agente sobre `baja:NavNode`

`[CERT]` `com/tridium/webeditors/ux/menu/BComponentJavaScriptMenuAgent.java:17-30`.

**`wb/util`** (14 clases) — editores especializados de la capa de gestión de usuarios/seguridad,
sin `@AgentOn` directo la mayoría pero con 3 `BIJavaScriptMgrAgent` (sub-tipo de `BIJavaScript`)
que SÍ registran agentes:
- `BMobileWebProfileConfigMgrAgent` — agente sobre `web:MobileWebProfileConfig`
- `BRoleHierarchiesMgrAgent` — agente sobre `hierarchy:RoleHierarchies` (requiredPermissions `W`)
- `BWebProfileConfigMgrAgent` — agente sobre (varios perfiles)

`[CERT]` `wb/util/BMobileWebProfileConfigMgrAgent.java:16-19` (`@AgentOn(types={"web:MobileWebProfileConfig"})`).

El editor compuesto `BCompositeUxEditor` (`wb/composite/`) sigue el patrón con `BIFormFactorMax`
(no mini): es una vista completa para editar componentes compuestos. `[CERT]`
`com/tridium/webeditors/ux/wb/composite/BCompositeUxEditor.java:17`.

## 421.x — Connections

- **[Bloque 199]** (webChart sobre webEditors): B199 §199.4 documentó que `webChart` monta sus
  settings sobre `nmodule/webEditors/rc/wb/PropertySheet` (con `nested:true`) y que `SamplingPeriodEditor`
  extiende `nmodule/webEditors/rc/fe/baja/OverrideRelTimeEditor`. Este bloque confirma la base Java
  de esa dependencia: `BPropertySheet.java` (§421.7) y el JS bundle `BWebEditorsJsBuild` (§421.6)
  son el sustrato que B199 nombraba sin abrir.

- **[Bloque 202]** (kitPx FE Wb↔Ux): B202 §202.3 describió `BUxGenericFieldEditor implements
  BIJavaScriptWidget`. Este bloque aclara que `webEditors` usa `BIJavaScript` directamente (no la
  sub-interfaz `Widget`) — el dispatch web de `webEditors` es distinto del Widget lifecycle de kitPx
  (§421.3). El patrón shim es el mismo en concepto pero difiere en la interfaz base y el contexto
  de mounting.

- **[Bloque 204]** (bajaux lifecycle): B204 §204.5 documentó `BIJavaScript → BIAgent → getJsInfo(Context)`
  y el puente rt→web. Los FE de `webEditors` implementan ese contrato (`getJsInfo` retorna el `JsInfo`
  con `BOrd` + `buildType`, §421.2). La resolución JS-side (`fe.getDefaultConstructor`) opera en el
  cliente — fuera del scope Java de este módulo.

- **[Bloque 214]** (FE de converters `@AgentOn`): B214 documentó `BIEnumToSimpleFE`/`BINumericToSimpleFE`/
  `BIStatusToSimpleFE` registrados por `@AgentOn` sobre el tipo-interfaz del converter — la MISMA
  mecánica NRE que §421.2 usa en la web. La diferencia: el lado Wb extiende `BWbFieldEditor` (Swing,
  doLoadValue/doSaveValue), el lado web extiende `BSingleton` (descriptor JS puro). Ambos lados
  coexisten como capas independientes no solapadas del mismo sistema de dispatch.
