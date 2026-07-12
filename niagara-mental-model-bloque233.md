# Block 233 — Reflow: migración de config entre versiones de schema

> **Qué documenta.** Cómo Reflow migra un `config.json` viejo a la versión de schema actual cuando el usuario
> actualiza el módulo: el trigger, el pipeline de transforms versionados, el backup pre-migración y el handshake
> `Client-Migration`. Gap BG18 (reapertura grupo B). Completa el ciclo de vida (con B231 backups, B232 licensing).
>
> **Alcance.** El sistema de migración client-side. Cross-ref B217 (el `Client-Migration` header ya notado) y B230
> (por qué el schema fue estable en 1.5→1.7).
>
> **Fuentes (primarias).** SPA beautificada (1:1 `app.4509efb4.js` sha256 `81b82b83…`):
> `scratchpad/reflow-app.beauty.js`, `BF:`. Barrido delegado (sonnet); tokens re-verificados por el driver.
>
> **Método / markers.** `[CERT]` = fuente primaria (`BF:` 1:1). `[INFER]` = deducción.

---

## 233.1 — El schema version y el trigger `[CERT]`

El schema del `config.json` tiene una versión objetivo constante: `La = 14` (`BF:13936`) — coincide con el
`version:14` observado live (B217 §217.8, B218). En `load()`, tras bajar `/nmodsreflow/config`, el objeto cargado
se pasa a `migrateState(config)` (`BF:14424`) **antes** de commitearlo al store (`LOAD_STATE`). La migración corre
si `!config.version || config.version < 14` (`BF:14534`) — sin campo version (config muy viejo) o por debajo de 14.

## 233.2 — El motor `migrateState`: backup + loop de transforms `[CERT]`

`migrateState` (`BF:14534`) es un pipeline **client-side, secuencial y versionado**:
1. Si corresponde migrar Y el usuario es el config/admin (`isConfig`): `SET_MIGRATION_ACTIVE(true)`, deep-clone del
   config, y **crea un backup pre-migración** (`create("Pre-migration v{oldVersion} {timestamp}")`, `BF:14544`)
   ANTES de tocar nada `[CERT]` (por eso hay `Pre-remap` y `Pre-migration` en el sistema de backups, B231).
2. **Loop** (`BF:14550`): mientras `version < 14`, busca `versions[version+1].migrate(config)`, aplica el transform,
   sube `version = next`, repite.
3. Éxito → `migrationStatus="success"`, `SET_MIGRATION_STATUS("success")`. Error en cualquier step → break,
   `"error"`. En ambos casos `SET_MIGRATION_ACTIVE(false)` al final.

## 233.3 — El registro de transforms versionados `[CERT]`

`versions` (`BF:3912`) es un registro de **13 pasos** de migración (versiones 2→14), cada uno un transform PURO de
la forma del config. Ejemplos verificados:
- **→14** (`BF:3905`): merge de `buildings.groups` dentro de `buildings.items` (unshift), luego `delete groups`.
- **→13**: strip de clases de alarma deshabilitadas de `classList`, setea `restrictNewAlarmClasses`.
- **→12**: re-liga las etiquetas de puntos de floorplan a `pointId` matcheando ORDs contra los puntos del equipo
  (o fallback a `style:"point"`) — nexo con el auto-binding (B228) y floorplans (B229).

Cada step es una función pura sobre el config deep-cloneado; el patrón se mantiene en todos.

## 233.4 — El handshake `Client-Migration` `[CERT]`

Si la migración tuvo éxito y el usuario es `isConfig`, `load()` llama de inmediato `saveState(true)` (`BF:14434`)
→ POST a `config_update` con header **`Client-Migration: r`** (`BF:14157`). Per B217 §217.4, ese header le dice al
servidor que **SALTE su backup incremental normal** en esa escritura — porque el backup pre-migración ya se tomó
client-side (§233.2), y un delta incremental sería inútil/dañino. Confirma y refina la hipótesis de B217.

## 233.5 — UX y supresión de saves durante la migración `[CERT]`

- **UX**: `checkMigration()` (`BF:57027`) muestra un modal en el cliente config: éxito → "Configuration Updated"
  ("…migrated to a new version. Your old configuration file was backed up automatically…"); error → "Configuration
  Error" (con guía para downgradear el módulo y restaurar del auto-backup). Los viewers (no-config) ven "Migrating
  configuration file, please wait" mientras `migrationActive` (`BF:117599`).
- **Supresión de saves**: la acción `save` es no-op mientras `migrationActive` (`if (stateLoaded &&
  !migrationActive) … else return`, `BF:14123`) — **no hay saves durante el loop de migración** `[CERT]`, evitando
  escrituras parciales/carreras. El subscriber de mutaciones también excluye las mutaciones de migración del
  save-loop.

## 233.6 — Conexiones

- **[Block 217]** §217.4 — el `Client-Migration` header; §233 muestra QUIÉN lo dispara (el saveState post-migración)
  y por qué (el backup ya está tomado).
- **[Block 231]** — el `Pre-migration` backup es otro tipo de snapshot automático (junto a Daily/Incremental/
  Pre-remap); ambos protegen ante operaciones riesgosas.
- **[Block 230]** — el schema fue estable (v14) en 1.5→1.7, así que ninguno de estos 13 transforms se disparó en ese
  salto; el pipeline existe para saltos de schema mayores.
- **[Block 228]/[Block 229]** — el transform →12 re-liga puntos de floorplan (nexo auto-binding + floorplans).
- **Cierre grupo B**: BG16 (licensing) + BG17 (backups) + BG18 (migración) + BG19 (diff-versiones) completos. Sigue
  grupo C (dinámico) y D (módulos + vistas).
