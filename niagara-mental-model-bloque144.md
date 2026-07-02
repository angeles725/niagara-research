# Block 144 — nmodsreflow.77 (`-rt`): subsistema backups (path traversal por sanitización asimétrica, cero autorización, ops destructivas GET)

> Research de **NiagaraMods Reflow v1.7.7 (build .75), paquete `backups/` del runtime `-rt`**: cómo Reflow
> crea, lista, aplica, borra, renombra y resetea backups del `config.json`. Cubre `BackupManager` y las 6
> Response HTTP (`BackupList/Create/Apply/Destroy/Rename/Reset`). NO cubre el scheduler que dispara el daily
> (vive en `BReflowService`, B138) ni el router/servlet que mapea los `serve` (B138/B140 — relevante para la
> pregunta de auth upstream).
>
> Focus: **nmodsreflow** (arquitectura backend `-rt`). Cierra el gap **R8**. Corpus language: Spanish
> (technical EN).
>
> Sources (primarias, JAR embarcado build .75, decompile Vineflower):
> `RT/` = `/home/cristian/modules/Prototipos/modulos/organized/nmodsreflow77/nmodsreflow77-rt/vineflower/com/niagaramods/nmodsreflow`
> `BM` = `RT/backups/BackupManager.java`. Responses: `RT/http/responses/Backup*Response.java`.
>
> Método: decompile Vineflower del JAR embarcado + lectura directa + grep de tokens/callers. Markers:
> `[CERT]` fuente primaria local (`file:line`) · `[INFER]` deducción anclada a líneas `[CERT]`.
> Nota de decompilado: `method_363`=parse de query en `http/util/Query` (R13, taint source); se cita tal cual.
>
> Capa 26 (OEM tercero NiagaraMods). Connects [Block 138] (el scheduler del daily y el router de servlets),
> [Block 140] (los broadcasts `config-reload`/`config-refresh` van por el canal WS), [Block 143] (un backup ES
> una copia del `config.json` de B143; apply lo sobrescribe), [Block 142] (misma capa Response, misma ausencia
> de gating), [Block 75]/[Block 113] (skipModuleValidation), [Block 139] (licensing bypass).

---

## 144.1 — Qué es un backup y dónde vive `[CERT]`

`BackupManager` es una **clase static-util** (ctor privado, sólo constantes) `[CERT]` `BM:19,25`. Un backup
es una **copia byte-a-byte del `^reflow/config.json`** (el archivo de config compartido de B143) a
`^reflow/backups/<name>.json` `[CERT]` `BM:220-221`. NO es un dist de station, NO es un zip, NO es la station
entera: es **un solo archivo JSON**, sin gzip/zip `[INFER]` (grep negativo). El nombre lo provee el cliente
(param `file`), **no es timestamped** `[CERT]` `BM:221` (concatenación del nombre).

**Daily/incremental:** `createDailyBackup()`→`create("Daily Backup", true)` y `createIncrementalBackup()`
(sólo si edad > 1 h y `!hasDefaultConfiguration()`) `[CERT]` `BM:20-23,96-104`. **No hay scheduler en esta
clase** (ni `Timer` ni `Clock.schedule`) `[INFER]` — el trigger vive en `BReflowService` (B138). **No hay
retención/pruning:** daily/incremental sólo sobrescriben sus dos nombres fijos; los backups user-named se
acumulan sin cota `[INFER]`.

## 144.2 — Path traversal por sanitización ASIMÉTRICA `[CERT]`

El desarrollador **conoce** el riesgo: `create` limpia el nombre con un regex que **quita separadores de path**
(`/ \` y `< > : " | ? * #`) antes de construir el `FilePath` `[CERT]` `BM:215`:

```
this.filename = this.filename.replaceAll("(<|>|:|\"|\\/|\\\\|\\||\\?|\\*|#)", "");   // create :215
```

Pero ese mismo scrub **falta** en tres de las cuatro operaciones que toman un nombre del cliente. La
asimetría (misma clase, mismo regex disponible) es la prueba de que la omisión es un **bug**, no diseño:

| Op | Concat del nombre del cliente | ¿Sanitizado? | Efecto `[INFER]` |
|---|---|---|---|
| create | `BM:221` `"^reflow/backups/" + filename + ".json"` | **SÍ** `BM:215` | traversal bloqueado |
| **destroy** | `BM:64` `"^reflow/backups/" + filename + ".json"` | **NO** | **DELETE** de `.json` arbitrario |
| **apply** | `BM:174` `"^reflow/backups/" + filename + ".json"` | **NO** | **READ** de `.json` arbitrario → sobrescribe config |
| **rename** | `BM:89` `"^reflow/backups/" + oldName + ".json"` | **NO** (`oldName`; `newName` sí `BM:83`) | **MOVE** de `.json` arbitrario |

`[CERT]` líneas exactas confirmadas por grep. `[INFER]` **Como el scrub que falta es justamente el que quita
`/` y `\`, un `filename` sin sanitizar puede contener separadores y `..`**, de modo que `FilePath` resuelve
fuera de `^reflow/backups/`. El alcance real del escape depende de que `FilePath` honre `..`/`/` en el ORD `^`
(station home); dado que el propio código trata esos chars como peligrosos en `create`, el traversal es la
lectura más natural.

- **destroy** `[CERT]` `BM:59-69`: `file`→`:64` concat sin sanitizar→`:69` `backupFile.delete()`. Único guard:
  un check de existencia `findFile==null` `[CERT]` `BM:65-67`, no un check de traversal. `[INFER]`
  `file=../../<x>` borra un `.json` arbitrario alcanzable desde station home.
- **apply** `[CERT]` `BM:168-199`: `filename` sin sanitizar `:174`; copia byte-a-byte ese archivo **sobre**
  `^reflow/config.json` (`makeFile` config `:173`) **sin validar el contenido** `[INFER]` — cualquier `.json`
  (o `../`-alcanzado) se instala como config vivo. Broadcast `config-reload` al canal WS `[CERT]`
  `BackupApplyResponse.java:30`.
- **rename** `[CERT]` `BM:74-91`: `newName` SÍ se limpia `:83`, pero `oldName` NO `:89` → `move()` `:91`
  reubica un `.json` arbitrario `../`-alcanzado dentro de `backups/`.

## 144.3 — Cero autorización en las 6 Response `[CERT]`

Las seis Response son `public static boolean serve(HttpServletRequest, HttpServletResponse)` y **ninguna**
declara `requiredPermissions` ni hace check de permiso: grep de `requiredPermissions`/`hasAdminWrite`/
`hasOperatorWrite`/`checkPermission`/`getPermissionsFor` sobre todo el subsistema → **cero** `[CERT]`
(grep negativo sobre `RT/backups/` + `Backup*Response.java`). `[INFER]` Las cinco operaciones mutantes/
destructivas (Create/Apply/Destroy/Rename/Reset) son alcanzables al nivel que el router de servlets admita,
sin gate de admin/write en clase — mismo patrón bare-authenticated que los favoritos de sync (B143).

Todas leen `req.getQueryString()` (params GET-style), **no** un body POST `[CERT]`
`BackupDestroyResponse.java:13`, `BackupApplyResponse.java:18`, `BackupRenameResponse.java:13`. `[INFER]` →
las mutaciones destructivas son disparables por requests **GET-shaped** (CSRF-friendly). Además el `author` de
apply/reset sale de headers controlados por el cliente (`Client-Username`/`Client-Id`) `[CERT]`
`BackupApplyResponse.java:27-28`.

| Response | Método | Llama | Riesgo `[INFER]` |
|---|---|---|---|
| List | GET | lista `^reflow/backups` | exfil: enumera + expone backups (cada uno = config completo de B143) |
| Create | GET | `BackupManager.create(file)` | `success:true` echo antes del thread async |
| **Apply** | GET | `BackupManager.apply(file)` `:20` | traversal → overwrite de config sin validar |
| **Destroy** | GET | `BackupManager.destroy(file)` `:15` | traversal → delete arbitrario |
| **Rename** | GET | `BackupManager.rename(file,name)` `:16` | traversal (oldName) → move arbitrario |
| **Reset** | GET | `delete(^reflow/config.json)` `:20` + `weather.png` `:23` | wipe de config sin auth ni token |

`BackupResetResponse` no parsea params: borra directo `^reflow/config.json` `[CERT]`
`BackupResetResponse.java:20` y `weather.png` `:23` en el thread del servlet, luego broadcast `config-refresh`
`[CERT]` `:35`. `[INFER]` wipe destructivo del config vivo, sin auth ni confirmación.

## 144.4 — Contraste positivo y otros defectos `[CERT]`

- **Sin `doPrivileged`:** a diferencia de history/alarms/sync (B141/B142/B143), **ningún** bloque
  `AccessController.doPrivileged` aparece en el subsistema backups `[CERT]` (grep negativo). Es el único
  subsistema sin el patrón — pero eso NO lo hace seguro: la falla acá es autorización + traversal, no
  privilege scope.
- **Threading:** `Thread` crudo (create `:49-50`, apply `:55-56`); destroy/rename corren síncronos en el
  thread del servlet `[CERT]`.
- **Excepciones tragadas:** catches vacíos en `ApplyBackupTask` `:195` y `CreateBackupTask` `:242` `[CERT]`
  — fallo silencioso; los endpoints devuelven `success:true` antes/independiente del resultado async `[INFER]`.
- **apply no valida el backup:** copia byte-a-byte sobre `config.json` sin parseo/schema `[INFER]` `BM:173-185`.
- **TOCTOU-ish:** loop de espera que sondea el tamaño de `config.json` para "estabilizarlo" antes de copiar
  `[CERT]` `BM:228-234` — lee un archivo que se escribe concurrentemente.

## 144.5 — Connections

- **[Block 138]** — el scheduler del daily/incremental vive en `BReflowService`; el router de servlets mapea
  los `serve` (la única pregunta de auth abierta: si el dispatcher hace un gate upstream — fuera de estos 7
  archivos; dentro del subsistema la autorización es cero).
- **[Block 140]** — los broadcasts `config-reload`/`config-refresh` de apply/reset van por el canal WS de B140.
- **[Block 143]** — un backup ES una copia del `config.json` de sync; `apply` lo sobrescribe (y B143 ya mostró
  que ese config se muta sin perms vía `sync-delta`). `destroy`/`reset` pueden borrarlo.
- **[Block 142]** — misma capa Response, misma ausencia de gating de permisos.
- **[Block 75]/[Block 113]** — `skipModuleValidation`. **[Block 139]** — licensing bypass.

**Nota de seguridad cross-focus (REFORZADA desde B143 §143.7 — R8 es el pico):** el subsistema backups
aporta la superficie **más grave** del focus, y de una clase distinta a los anteriores (no privilege scope
sino **autorización rota + path traversal + destrucción**). Cuadro agregado actualizado: (1) history/alarms/
sync (B141/B142/B143) corren bloques `doPrivileged` anchos sobre input del cliente; (2) B143 persiste un
JSON-Patch del cliente a `config.json` sin perms; (3) B142 tiene BQL injection a read-level; (4) **B144 suma
path traversal (delete/overwrite/move de `.json` arbitrario) + wipe de config, TODO sin autorización y
GET-triggerable (CSRF)**; (5) todo ello descansa en que el módulo esté firmado/validado, pero B75/B113
mostraron que la validación de módulo puede apagarse vía `skipModuleValidation` y B139 documentó el bypass del
licensing RSA. `[INFER]` La superficie agregada (config-write no autorizado + traversal destructivo + BQL
injection + múltiples doPrivileged anchos + validación de módulo desactivable + licensing con bypass) es ya
material suficiente para un **bloque de síntesis cross-focus** (nmodsreflow × platform-security) como
NEXT-ACTION al agotar el focus. Queda anotada; no se resuelve en R8 (read-only, cruza focuses). El sub-gap
R13 (`http/util/Query.method_363`) es el taint source común a TODOS estos params `file`/`query`.
