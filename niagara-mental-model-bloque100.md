# Bloque 100 — `ipcMigrator`: migrador de aplicaciones Spyder XL10 Next Gen → stack IPC 3036 (function blocks + I/O + Sylk), deofuscado

> Investigación empírica del módulo OEM Honeywell **`ipcMigrator`** (19 java, Workbench-only): la herramienta que **migra aplicaciones de control de los controladores Spyder XL10 Next Gen** (`honeywellLonSpyder`/`honeywellBacnetSpyder`) al **stack del controlador IPC 3036** ([Bloque 99]). Análogo al migrador del [Bloque 90] (EagleHawk→PanelBus), pero del lado Spyder→IPC.
>
> 1 módulo (`ipcMigrator-wb`, todo en contexto Workbench). Paquetes: `migrator` (10), `utils` (4), `ui` (4), `enums` (1).
>
> Fuentes: `organized/ipcMigrator/ipcMigrator-wb/vineflower/com/honeywell/ipcmigrator/...`.
> Método: 1 sub-agente Explore + **verificación directa** de cada `extends`. `[CERT]` = verificado por mí; `[CERT-a]` = cita del sub-agente (sub-migradores, function blocks, sensores, schedules) no re-verificada; `[INFER]` = deducción.
>
> Capa 22 (OEM deofuscados), continúa [Bloque 99]. Cierra la familia D (IPC) del barrido del corpus. Conecta [Bloque 99] (el stack IPC destino), [Bloque 77] (Spyder, el origen), [Bloque 90] (patrón de migrador hermano), [Bloque 88] (Sylk).

---

## 100.1 — Qué migra: Spyder XL10 → IPC 3036 `[CERT-a]`

**Desde**: controladores Spyder XL10 Next Gen (`honeywellLonSpyder:LonSpyder` / `honeywellBacnetSpyder:BacnetSpyder`). Cuatro tipos de artefacto: Station (`config.bog`), Spyder Library (`/UserDefined/`+`index.xml`), Exported Library (`.slb` ZIP), Palette (`.palette`). **Opera sobre archivos `.bog`** del FS local (NO sobre station viva, vía `ValueDocDecoder`), y emite `.bog`.

**Hacia**: el stack `ipcCommBus` ([Bloque 99]) — `BIPCNetwork → BIPCDevice → BIPCPointDeviceExt → BSequencedControlProgram → BApplicationFolder`. Modelo destino `BIPCDeviceModelEnum.ipc3036vav` (o genérico).

---

## 100.2 — Jerarquía verificada `[CERT]`

| Clase | `extends` verificado (archivo:línea) |
|-------|--------------------------------------|
| `IPCMigrator` | (sin extends — POJO coordinador) (`migrator/…:55`) |
| `MigratingJob` | `extends Thread` (`ui/…:53`) |
| `BSpyderToIPCMigrator` | `extends BWbNavNodeTool` (`ui/…:48`) |
| `BFunctionBlockMigrator` | `extends BComponent implements IMigrator` (`migrator/…:90`) |
| `BMigrationEntityEnum` | `extends BFrozenEnum` (`enums/…:15`) |

El contrato de los sub-migradores es la interfaz `IMigrator` (`migrate()` + `handlePostMigrate()`). **El patrón de ejecución es `Thread` directo, NO `BSimpleJob`** `[CERT]` — diferencia con el migrador del [Bloque 90] (que usa `BSimpleJob`). UI: `BSpyderToIPCMigrator extends BWbNavNodeTool` abre un diálogo (tipo de fuente, dir entrada/salida, checklist) y lanza el `MigratingJob`.

`BMigrationEntityEnum` (3 valores) `[CERT-a]`: `device` (ISpyderDevice completo), `application` (BIMacro standalone, default), `sylkdevice` (BSBusWallModule).

---

## 100.3 — Los sub-migradores `[CERT-a]`

Seis `BComponent implements IMigrator`, orquestados en secuencia por `IPCMigrator`:
- **`BFunctionBlockMigrator`**: traduce ~37 tipos de function block Spyder → IPC vía mapa estático de `MigratorUtil` (analog, control PID/Stager, logic And/Or/Xor, math, zone control, BAlarm→BNumericWritable+BAlarmSourceExt, BSchedule→BEnumSchedule…), con conversiones de tipo (BStatusNumeric → BHonStatusNumeric/BFiniteStatusBoolean).
- **`BIOMigrator`**: puntos físicos I/O — AI (14 tipos de sensor: NTC20K, Pt1000, C7400A, C7632A/B, pulse meter…), DI, AO analógico/floating (genera BO `_OPEN`/`_CLOSE`)/PWM, DO. Usa `TerminalAssignmentHandler`.
- **`BLinkMigrator`**: recrea los links del wiresheet Spyder en el árbol IPC (composite + normales, con traducción de slot names).
- **`BScheduleBlockMigrator`**: `BSchedule` Spyder (7 días + holidays, fechas/rangos, WeekDay-N, Thanksgiving) → `BEnumSchedule` IPC.
- **`BSoftwarePointsMigrator`**: network points (const/nv-input/setpoint/nv-output) → BNumeric/EnumConst/Writable.
- **`BSylkDeviceMigrator`**: `BSBusWallModule` ("Kingfisher") → dispositivos Sylk IPC (`BTR75X/TR71X/TR42/TR40SylkDevice`, C7400, actuador); convierte TR70→TR71 con advertencia.

**`TerminalAssignmentHandler` `[CERT-a]`**: cuenta puntos por tipo (BQL sobre la fuente) y consume la lista de terminales del modelo; si se agotan, agrega `BExpansionIODeviceExt` (expio3022h/9056h) automáticamente (máx 15 expansiones). `IPCMigrator` genera un reporte `.txt` con advertencias.

---

## 100.4 — Seguridad `[CERT-a]`

**[MEDIO CERT-a] Estado estático mutable entre threads.** `IPCMigrator.ioPriorityOverrideSlotMap` y `tempSetpointsMap` son `static HashMap`; el UI no bloquea el botón durante la migración (`MigratingJob` es Thread) → dos migraciones concurrentes corrompen los mapas (links mal asignados).

**[MEDIO CERT-a] Deserialización `.bog` sin validar el tipo resultado.** `decodeDocument(true)` + `add("ControlProgram", v)` sin verificar el tipo de `v` → un `.bog` malicioso en el directorio de stations puede tener efectos en su deserialización/constructor.

**[BAJO CERT-a] Filtro de seguridad por substring.** `copyALibraryFile()` excluye archivos cuyo nombre contiene `"security"` — criterio frágil (un `auth.xml`/`permissions.bog` pasaría). Posible path traversal en `getFileName()` de librería sin sanitizar; `outputDir` nullable tras excepción silenciada (NPE).

---

## 100.5 — Conexiones

- **[Bloque 99]** (IPC 3036): el **destino** — el migrador puebla `BIPCNetwork`/`BIPCDevice`/`BApplicationFolder`.
- **[Bloque 77]** (Spyder): el **origen** — aplicaciones LonSpyder/BacnetSpyder XL10 Next Gen.
- **[Bloque 90]** (honPlantControllerMigrator): patrón de migrador hermano (ambos `.bog`-based, sub-migradores especializados); difiere en `Thread` vs `BSimpleJob` y en origen/destino (Spyder→IPC vs EagleHawk→PanelBus).
- **[Bloque 88]** (Sylk): migra wall modules Sylk al stack IPC.
