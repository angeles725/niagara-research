# Block 151 — nmodsreflow.77 (`-ux`): esqueleto del módulo cliente (3 view-agents `BIJavaScript` sobre `ReflowService`, identidad del módulo)

> Research de **NiagaraMods Reflow v1.7.7 (build .75), módulo `-ux` (runtimeProfile `ux`)**: el esqueleto de
> la capa cliente — las 3 clases Java que registran las vistas del UI (`BReflow`, `BReflowConfig`,
> `BReflowRedirect`), cómo se enganchan a `ReflowService` como view-agents, a qué loader JS apunta cada una, y
> la identidad del módulo (`module.xml`/`module.palette`). Es el PRIMER bloque del focus frontend
> `nmodsreflow-ux` (paridad con el backend `-rt`, B138-B150). NO cubre los loaders JS en sí (U2) ni la SPA
> embarcada (U3).
>
> Focus: **nmodsreflow-ux** (capa cliente `-ux`). Cierra el gap **U1**. Corpus language: Spanish (technical EN).
>
> Sources (primarias, JAR embarcado build .75, decompile Vineflower):
> `UX/` = `/home/cristian/modules/Prototipos/modulos/organized/nmodsreflow77/nmodsreflow77-ux/vineflower`
> Clases: `UX/com/niagaramods/nmodsreflow/ux/BReflow*.java`. Config: `UX/META-INF/module.xml`,
> `UX/module.palette`.
>
> Método: lectura directa completa de las 3 clases (35 líneas c/u) + `module.xml`/`module.palette`. Markers:
> `[CERT]` fuente primaria local (`file:line`) · `[INFER]` deducción anclada a líneas `[CERT]`.
>
> Capa 26 (OEM tercero NiagaraMods). Connects [Block 138] (`nmodsreflow:ReflowService`, el tipo sobre el que
> montan estos agents), [Block 145] (el matiz de permiso `rw` de la vista config vs el REST sin gate),
> [Block 146] (mismo mecanismo `@AgentOn requiredPermissions`), [Block 149] (los loaders JS que estas clases
> referencian sirven la SPA cuyo contrato documentó B149).

---

## 151.1 — Identidad del módulo `-ux` `[CERT]`

`module.xml` `[CERT]` `UX/META-INF/module.xml:2`:

| Campo | Valor | Cita |
|---|---|---|
| `name` | `nmodsreflow-ux` | `module.xml:2` |
| `moduleName` | `nmodsreflow` | `module.xml:2` |
| `runtimeProfile` | **`ux`** | `module.xml:2` |
| `vendor` / `vendorVersion` | `NiagaraMods` / **`1.7.7.75`** | `module.xml:2` |
| `preferredSymbol` | `nmflow` | `module.xml:2` |
| `buildMillis` | `1755192498499` (~2025-08) | `module.xml:2` |
| `nre` / `autoload` / `installable` | `true` / `true` / `true` | `module.xml:2` |

`[INFER]` `buildMillis 1755192498499` ≈ 14-ago-2025 — sella el build `.77` del lado `-ux` (mismo vendorVersion
`1.7.7.75` que el `-rt`). Dependencias declaradas `[CERT]` `module.xml:3-17`: `nmodsreflow-rt 1.7` (el backend
del focus anterior) + Tridium `baja`, `web-rt`, `bql-rt`, `bacnet-rt`, `alarm-rt`, `history-rt`, `box-rt`,
`schedule-rt`, `control-rt`, `driver-rt`, `net-rt`, `platform-rt` (todos 4.6). `[INFER]` el set de deps refleja
qué subsistemas del `-rt` toca el UI (history/alarm/bql/schedule/bacnet).

`module.palette` `[CERT]` `UX/module.palette:1-5` es **trivial**: un único `b:Folder` vacío, sin componentes
paletteados. `[INFER]` el módulo no aporta objetos arrastrables a Workbench — es puro registro de vistas.

## 151.2 — Las 3 view-agents `BIJavaScript` `[CERT]`

Las tres clases son idénticas en forma: `extends BSingleton implements BIJavaScript, BIFormFactorMax`, todas
**view-agents montados sobre `nmodsreflow:ReflowService`** vía `@AgentOn` `[CERT]`:

| Clase | Registro (`name`) | `requiredPermissions` | Loader JS (`JsInfo` ORD) | Cita |
|---|---|---|---|---|
| `BReflow` | `Reflow` | **`r`** | `module://nmodsreflow/niagara/reflow.js` | `BReflow.java:14-23` |
| `BReflowConfig` | `ReflowConfig` | **`rw`** | `module://nmodsreflow/niagara/reflow_config.js` | `BReflowConfig.java:14-23` |
| `BReflowRedirect` | `ReflowRedirect` | **`r`** | `module://nmodsreflow/niagara/reflow_redirect.js` | `BReflowRedirect.java:14-23` |

Mecánica común `[CERT]` (idéntica en las 3, ej. `BReflow.java:20-33`):
- `BSingleton` con `INSTANCE`/`TYPE` estáticos `[CERT]` `:21-22` → vista sin estado (una instancia global).
- `implements BIJavaScript` → `getJsInfo(Context)` devuelve un `JsInfo.make(BOrd.make("module://.../<loader>.js"))`
  `[CERT]` `:23,32-34` → así Niagara sabe **qué JS cargar** para renderizar la vista en el browser/Workbench.
- `implements BIFormFactorMax` `[CERT]` `:20` → `[INFER]` la vista pide el form-factor máximo (pantalla
  completa), coherente con una SPA que toma todo el viewport.
- `@AgentOn(types = {"nmodsreflow:ReflowService"})` `[CERT]` `:14-18` → la vista aparece cuando el usuario
  selecciona un `ReflowService` (B138) en el árbol.

`[INFER]` Modelo: el `-ux` NO contiene la SPA; es la **capa de registro** que le dice a Niagara "para el
ReflowService hay 3 vistas, y cada una arranca con este archivo JS". Los loaders (U2) bootstrapean la SPA real
(servida desde `rc/`, U3).

## 151.3 — El matiz de permiso: la vista config gatea `rw`, el REST no gatea nada `[CERT]`

`BReflow` (vista principal) y `BReflowRedirect` declaran `requiredPermissions = "r"` `[CERT]`
`BReflow.java:17`, `BReflowRedirect.java:17`; pero **`BReflowConfig` declara `"rw"`** `[CERT]`
`BReflowConfig.java:17` (confirmado también en `module.xml:26`). `[INFER]` Es decir: para que la **vista** de
configuración aparezca/cargue en Workbench, el usuario necesita permiso de escritura sobre el ReflowService.

**Cross-ref crítico con B145/B146:** este `"rw"` es un gate del **view-agent** (`@AgentOn`, controla la
visibilidad/carga de la vista en el UX), NO una enforcement sobre el **path HTTP** de mutación. B145 mostró que
`ConfigUpdateResponse`/`ConfigDeltaResponse` (los que realmente escriben `config.json`) **no declaran ningún
`requiredPermissions`** (B145 §145.1) y B149 §149.1 mostró que `BaseServlet` no agrega gate. `[INFER]` Por lo
tanto el `"rw"` de `BReflowConfig` es **cosmético desde el punto de vista de seguridad**: esconde el botón/vista
de config al usuario read-only en Workbench, pero un request HTTP directo a `/config-update` bypassa esa vista
por completo. Es el mismo patrón "gate en el agente, no en el dato" que B146 §146.5 identificó para los
comandos — reconfirmado desde el lado UX. Alimenta la síntesis B150.

## 151.4 — Connections

- **[Block 138]** — `nmodsreflow:ReflowService` es el tipo sobre el que montan los 3 view-agents; el `-ux`
  depende de `nmodsreflow-rt 1.7` (`module.xml:9`).
- **[Block 145]/[Block 146]** — el `"rw"` de `BReflowConfig` gatea la VISTA, no el endpoint REST de config-write
  (que B145 mostró sin gate); mismo patrón "gate en el agente, no en el dato" de B146 §146.5. Refuerza B150.
- **[Block 149]** — los 3 loaders JS (`reflow.js`/`reflow_config.js`/`reflow_redirect.js`) arrancan la SPA cuyo
  contrato de datos documentó B149; se sirven vía `module://` (el mismo namespace `rc`/módulo de `FileResponse`
  B149 §149.4).
- **[Block 50]/[Block 51]** — auditaron el `-ux`/frontend de v1.7.5; este bloque es el mismo esqueleto en `.77`
  con rigor `file:line` (paridad).

`[INFER]` Próximos gaps del focus: U2 (la cadena de loaders JS `reflow*.js` + `lib/{loader,resolver,hyperlink}.js`
que estas clases referencian), luego U3 (la SPA embarcada minificada).
