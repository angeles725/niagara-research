# Block 146 — nmodsreflow.77 (`-rt`): los 8 command agents `BReflow*Commands` (tabla de autorización, y por qué el gate `"r"` no protege el REST)

> Research de **NiagaraMods Reflow v1.7.7 (build .75), paquete `commands/` del runtime `-rt`**: los 8
> `BReflow*Commands` (Alarm/BQL/CSV/File/History/License/Nav/User) como superficie de comandos server-side,
> con foco en la **autorización real por comando** — el gating que B142/B143/B144/B145 no vieron en la capa
> Response. Cubre el tipo, el `requiredPermissions` de cada uno, las acciones expuestas, y los vectores
> sensibles (BQL arbitrario, refresh de licencia, traversal de filesystem). **CORRIGE/REFINA** el framing de
> "cero autorización" de B143-B145.
>
> Focus: **nmodsreflow** (arquitectura backend `-rt`). Cierra el gap **R10**. Corpus language: Spanish
> (technical EN).
>
> Sources (primarias, JAR embarcado build .75, decompile Vineflower):
> `RT/` = `/home/cristian/modules/Prototipos/modulos/organized/nmodsreflow77/nmodsreflow77-rt/vineflower/com/niagaramods/nmodsreflow`
> `CMD/` = `RT/commands`.
>
> Método: decompile Vineflower del JAR embarcado + lectura directa + grep de tokens. Markers:
> `[CERT]` fuente primaria local (`file:line`) · `[INFER]` deducción anclada a líneas `[CERT]`.
>
> Capa 26 (OEM tercero NiagaraMods). Connects [Block 139] (el `licenseCommand`/refresh), [Block 140]
> (dispatch WS de comandos bajo `doPrivileged`), [Block 141]/[Block 142] (los commands delegan a
> `HistoryData`/`AlarmData` statics), [Block 143]/[Block 144]/[Block 145] (los Response REST que **bypassean**
> este gate), [Block 75]/[Block 113] (skipModuleValidation), [Block 139] (licensing bypass).

---

## 146.1 — Tabla de autorización: los 8 gatean a `"r"`, uniforme `[CERT]`

Las 8 clases son idénticas en forma: `extends BComponent implements BIServerSideCallHandler`, y todas
declaran su permiso **dentro de `@AgentOn`** (el gate está en la registración del agente contra
`nmodsreflow:ReflowService`), uniformemente `requiredPermissions = "r"` `[CERT]`:

| Command | `requiredPermissions` | Cita |
|---|---|---|
| `BReflowBQLCommands` | `"r"` | `CMD/BReflowBQLCommands.java:29` |
| `BReflowFileCommands` | `"r"` | `CMD/BReflowFileCommands.java:23` |
| `BReflowUserCommands` | `"r"` | `CMD/BReflowUserCommands.java:18` |
| `BReflowLicenseCommands` | `"r"` | `CMD/BReflowLicenseCommands.java:27` |
| `BReflowNavCommands` | `"r"` | `CMD/BReflowNavCommands.java:22` |
| `BReflowCSVCommands` | `"r"` | `CMD/BReflowCSVCommands.java:26` |
| `BReflowHistoryCommands` | `"r"` | `CMD/BReflowHistoryCommands.java:21` |
| `BReflowAlarmCommands` | `"r"` | `CMD/BReflowAlarmCommands.java:24` |

**Resultado: los 8 gatean a read-level `"r"`. Ninguno declara `"rw"`, `"iw"`, admin ni operator.** Esto
generaliza el hallazgo de B142 (que había visto `"r"` sólo en Alarm): es el patrón uniforme, no algo
específico de Alarm. `[INFER]` **Corrección al framing de B143-B145:** la capa de comandos NO es literalmente
"cero autorización" — exige permiso de **lectura** sobre `ReflowService`. Pero (§146.4) ese es el gate más
débil, y (§146.5) el REST lo bypassa.

**Único check de permiso en cuerpo de método en las 8 clases:** `BReflowAlarmCommands.canAcknowledgeAlarms`
`[CERT]` `CMD/BReflowAlarmCommands.java:99-101` (`cx.getUser().getPermissionsFor(alarmService).hasOperatorWrite()`)
— pero sólo DEVUELVE un booleano informativo para la UI; **no gatea ninguna operación** `[INFER]`.

## 146.2 — El gate está mal escalado: ops potentes detrás de `"r"` `[CERT]`

**`BReflowBQLCommands` — ejecución de BQL/ORD arbitrario, CONFIRMADA.** Un string del cliente se hace ORD y
se resuelve `[CERT]` `CMD/BReflowBQLCommands.java:50,64,68`:

```
ord = BOrd.make(comps.get("query").toString());   // :50 (o :64 fallback arg)
...
BObject obj = ord.get(null);                        // :68 — resolución con Context NULL
```

`[INFER]` El `ord.get(null)` resuelve con **Context nulo/sistema**, es decir NO filtrado por el control de
acceso del usuario llamante. Combinado con el `doPrivileged` del dispatch WS (B140), el BQL corre sin filtro
de permisos del caller. Gate: sólo `"r"` `:29`, sin check en cuerpo. `[INFER]` Cualquier caller con lectura
sobre `ReflowService` corre `select * from …`/cualquier ORD, y los resultados no se filtran por permiso.

**`BReflowLicenseCommands.refreshLicense` — acción mutante detrás de `"r"`** `[CERT]`
`CMD/BReflowLicenseCommands.java:159-169`: localiza el service por BQL `:160` y llama `srv.doRefreshLicense()`
`:169` (re-validación, cruza B139). `[INFER]` acción state-changing gateada sólo a read-level.

**`BReflowNavCommands.bformat` — superficie eval-ish:** `BFormat.format(format, ord.get())` `[CERT]`
`CMD/BReflowNavCommands.java:41` evalúa un format del cliente contra un ORD del cliente `[INFER]`.

## 146.3 — Traversal de filesystem (lectura) en File/CSV `[CERT]`

`BReflowFileCommands` expone **sólo** `listFiles` `[CERT]` `CMD/BReflowFileCommands.java:33`; toma el path del
cliente crudo en `findFile(new FilePath(path))` sin sanitizar `[CERT]` `:64`. `[INFER]` disclosure del árbol
de archivos por traversal, **read-only** — mismo taint que B145 `?file=`. `BReflowCSVCommands.loadPointMap`
repite el patrón `[CERT]` `CMD/BReflowCSVCommands.java:56` (`findFile(new FilePath(filePath))`), read/parse.

## 146.4 — Hallazgos negativos que colapsan sub-claims de la tesis `[CERT]`

- **NO hay file write/delete:** `BReflowFileCommands` tiene sólo `listFiles` `[CERT]` `:33` — sin
  makeFile/delete/write. El vector "file write" NO está en la capa de comandos (la escritura destructiva vive
  en backups/config, B144/B145).
- **NO hay user management:** `BReflowUserCommands` sólo lee roles — `getRoles` devuelve
  `user.getRoles()` (los del **propio** caller) `[CERT]` `CMD/BReflowUserCommands.java:28-30` y `getAllRoles`
  devuelve `service.getEnabledRoleIds()` `[CERT]` `:33-35`. **Sin** create/delete/setPassword/mutación de
  permisos `[CERT]` (grep negativo). El vector de escalación por gestión de usuarios NO existe acá.

`[INFER]` Estos dos negativos **corrigen** una suposición del backlog (R10 nombraba "User" como superficie
sensible): la superficie real de User es introspección de roles del propio caller, no CRUD.

## 146.5 — Por qué el gate `"r"` NO protege el REST (mecanismo del bypass) `[CERT]`

El gate `requiredPermissions = "r"` vive en la **registración `@AgentOn` del agente** — se aplica sólo cuando
la operación se alcanza **a través del command agent**. Pero el trabajo real lo hacen **statics sin check de
permiso**: `AlarmData.query(...)` `[CERT]` `CMD/BReflowAlarmCommands.java` (delegación a `AlarmData.*`),
`HistoryData.fromComponent(...)`, `BackupManager.*`, `ConfigIO.*`. `[INFER]` Como B143/B144/B145 mostraron que
los Response REST invocan `BackupManager`/`ConfigIO`/`AlarmData`/`applyConfig` **directamente** (no a través
del command gateado), el `requiredPermissions = "r"` **no cubre la superficie REST**: el gate está en el
agente, no en el dato. Ese es el mecanismo concreto del bypass.

## 146.6 — Connections y veredicto de la nota de seguridad

- **[Block 139]** — `refreshLicense` (`:169`) dispara la re-validación del licensing de B139, a read-level.
- **[Block 140]** — el dispatch WS de estos comandos corre bajo `doPrivileged`; el BQL con Context nulo (§146.2)
  hereda esa elevación.
- **[Block 141]/[Block 142]** — los commands History/Alarm delegan a `HistoryData`/`AlarmData` statics (los
  mismos que el REST invoca sin gate).
- **[Block 143]/[Block 144]/[Block 145]** — los Response REST **bypassean** el gate `"r"` de esta capa
  llamando a los statics directo. Ese es el punto que cierra la nota.
- **[Block 75]/[Block 113]** — `skipModuleValidation`. **[Block 139]** — licensing bypass.

**Nota de seguridad cross-focus (AFINADA — R10 corrige el framing sin bajar la severidad):** el veredicto es
**REVISE-and-CONFIRM**. (1) **REVISE:** la capa de comandos NO es "cero autorización" — los 8 gatean a
`"r"` (read-level). El framing preciso es **"autorización read-level, sin tiering de privilegio"**, no
"sin autorización". (2) **CONFIRM el riesgo:** el gate está mal escalado — BQL arbitrario con Context nulo
(`:68`), refresh de licencia (`:169`) y traversal de fs (`:64`) están todos detrás de mero `"r"`. (3)
**CONFIRM la tesis para el REST:** el gate `"r"` cabalga el agente `@AgentOn`, mientras la lógica real está en
statics sin permiso, así que los Response REST (B143/B144/B145) que llaman esos statics directo **no** están
protegidos por él → la superficie REST sigue siendo alcanzable sin autorización más allá de la sesión.
`[INFER]` El cuadro agregado para la síntesis cross-focus (nmodsreflow × platform-security) queda así:
config/backups mutables sin auth por REST (bypass del gate `"r"`) + BQL arbitrario read-level con Context nulo
+ traversal lectura/escritura + audit trail forjable + `doPrivileged` anchos, todo sobre una plataforma donde
la validación de módulo puede apagarse (`skipModuleValidation`, B75/B113) y el licensing tiene bypass (B139).
Es el NEXT-ACTION natural al cerrar el focus.
