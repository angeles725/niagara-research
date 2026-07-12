# Block 231 — Reflow: versionado del dashboard (sistema de backups)

> **Qué documenta.** El sistema de backups/snapshots del `config.json` de Reflow como capacidad de PRODUCTO: los
> tipos de backup (automáticos + manuales), cómo se crean, y cómo se restaura. Gap BG17 (reapertura grupo B).
> Reencuadra B144 (que vio `BackupManager` desde SEGURIDAD: path traversal).
>
> **Alcance.** El ciclo de vida de los snapshots del dashboard. La superficie de seguridad (traversal, zero-auth)
> está en B144; aquí el mecanismo de producto.
>
> **Fuentes (primarias).** Java RT: `com/niagaramods/nmodsreflow/backups/BackupManager.java` (249 líneas, leído
> directo) + `http/responses/Backup*Response.java` (6 clases). Evidencia de disco: los backups reales de la station
> `HoneywellMX605132026/shared/reflow/backups/`.
>
> **Método / markers.** `[CERT]` = fuente primaria (`file:line` · archivo en disco). `[INFER]` = deducción.

---

## 231.1 — Cuatro tipos de backup `[CERT]`

El `config.json` del dashboard se versiona con snapshots en `^reflow/backups/`. Tipos observados (evidencia de
disco + código):

| Tipo | Origen | Nombre en disco |
|---|---|---|
| **Daily** | automático diario (overwrite) | `Daily Backup` |
| **Incremental** | automático cada ≥1h si cambió del default | `Incremental Backup` |
| **Pre-remap** | automático antes de un auto-binding masivo | `Pre-remap Backup <fecha>` |
| **Manual** | el usuario crea con nombre | `Backup <fecha>` |

En la station real: 2 `Backup <fecha>` (~204 KB, manuales), 1 `Incremental Backup` (247 KB), y **13 `Pre-remap
Backup <fecha>`** (42-200 KB) `[CERT]` (disco) — los pre-remap son los más numerosos, señal de que el usuario hizo
varios remaps de equipos (B228) a lo largo del tiempo.

## 231.2 — `BackupManager`: crear, aplicar, renombrar, destruir `[CERT]`

`BackupManager` (`BackupManager.java:19`) expone la mecánica, cada operación en un `Thread` propio:
- `create(filename, overwrite)` (`:47`) → `CreateBackupTask` (`:202`): copia el `config.json` actual a
  `^reflow/backups/<filename>`.
- `apply(filename)` (`:53`) → `ApplyBackupTask` (`:159`): **restore** — copia el backup de vuelta sobre el config
  activo.
- `destroy(filename)` (`:59`), `rename(oldName, newName)` (`:74`) — con guards de existencia y de nombre duplicado
  (`:87` "already exists").
- `method_366(filename)` (`:36`, "age") — edad del backup en ms (`Long.MAX_VALUE` si no existe), usado para el
  gate incremental.

## 231.3 — Los automáticos: daily (overwrite) e incremental (1h + skip-if-default) `[CERT]`

- **Daily** (`createDailyBackup`, `:96`): `create("Daily Backup", true)` — un único slot que se sobrescribe cada día.
- **Incremental** (`createIncrementalBackup`, `:100`): sólo crea si `age("Incremental Backup") > 3600000L` (1h,
  `INCREMENTAL_TIMEOUT` `:20`) **Y** `!hasDefaultConfiguration()` (`:101`). Es decir: no hace backup incremental si
  pasó menos de 1h, ni si el dashboard sigue siendo el default de fábrica `[CERT]`.
- `hasDefaultConfiguration()` (`:106`) / `jsonIsDefaultConfiguration()` (`:133`): comparan el config actual contra
  el default — evitan versionar un dashboard vacío/de fábrica (coherente con el default de 2 cards de B217 §217.8).

## 231.4 — Pre-remap: proteger antes del auto-binding `[CERT]`/`[INFER]`

Los `Pre-remap Backup <fecha>` (13 en disco `[CERT]`) se crean **antes de un remap de equipos** (el auto-binding
masivo de B228) — un snapshot de seguridad por si el regex-match liga puntos mal. El método explícito no está en
`BackupManager` (que solo define Daily/Incremental) → el nombre "Pre-remap" se pasa a `create(...)` desde el flujo
de remap del cliente/bulk-wizard (B228 §228.4) `[INFER]` (por el nombre + el contexto B228; no se leyó el call-site
exacto que lo dispara). Es la contraparte de snapshot del `lockFromRemap` (B228 §228.5): dos mecanismos para no
perder bindings manuales.

## 231.5 — Restore y la superficie REST `[CERT]`

Restaurar = `apply(filename)`. Las 6 `Backup*Response` exponen el ciclo por HTTP: `BackupCreateResponse`,
`BackupApplyResponse` (restore), `BackupListResponse` (listar), `BackupRenameResponse`, `BackupDestroyResponse`,
`BackupResetResponse` (reset a default). Todas son `serve` GET-shaped con `?file=` (B144 documentó que son
GET-triggerable sin auth — el ángulo de seguridad; aquí el punto de producto es que **el usuario gestiona sus
versiones desde la UI**: crear un snapshot con nombre, listar, restaurar, borrar, resetear a fábrica).

## 231.6 — Conexiones

- **[Block 144]** — documentó `BackupManager` y las `Backup*Response` desde SEGURIDAD (traversal por sanitización
  asimétrica, zero-auth, ops destructivas GET); §231 es el mismo código como sistema de versionado de producto.
- **[Block 228]** §228.4/228.5 — el auto-binding masivo que dispara los `Pre-remap Backup`; el `lockFromRemap` es
  la protección complementaria.
- **[Block 217]** — el `config.json` que se versiona; `hasDefaultConfiguration` referencia el mismo default de 2
  cards visto live (§217.8).
- **[Block 230]** — el `Client-Migration` header hace que un `config_update` de migración SALTE el backup
  incremental (B217 §217.4) — nexo con el ciclo de vida de versiones.
- **Hacia BG16/BG18**: la migración (BG18) y el licensing (BG16) completan el grupo B (ciclo de vida).
