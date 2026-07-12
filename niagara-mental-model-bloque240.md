# Block 240 — Reflow: la UI de alarmas (widget, consola, ack, sonidos)

> **Qué documenta.** El frontend de alarmas de Reflow: el widget `AlarmDisplay`, la página de consola, el
> acknowledge (single/all) y las alarmas audibles. Gap BG24 (reapertura grupo E). Complementa B142 (backend
> alarmas, seguridad).
>
> **Alcance.** La cara cliente de alarmas. El backend (query BQL, doPrivileged) es B142.
>
> **Fuentes (primarias).** SPA beautificada (`BF:`). Barrido delegado (sonnet); tokens re-verificados.
>
> **Método / markers.** `[CERT]` = fuente primaria. `[INFER]` = deducción.

---

## 240.1 — El widget `AlarmDisplay` `[CERT]`

Componente `AlarmDisplay` (`BF:2440`). Config: `display` (`total|building|class|low|medium|high|console`),
`displayType` (`active|recordCount`), `console`/`consoleId`, `priorities`, `classes`, `building`, `link`, `title`.
Polea el conteo vía `$niagara.alarm.getAlarmList({…, countOnly:true})` en un intervalo (`consoleRefreshRate`, default
20s, `BF:2522`). Click navega a `/alarms/console/{consoleId}` o a un link de building/alarma. Es la tarjeta de
resumen de alarmas del dashboard (badge de conteo).

## 240.2 — La página de consola de alarmas `[CERT]`

Rutas: `/alarms` y `/alarms/console/:id` → componente de lista; `/alarms/console/:id/:type` → "Alarm Details"
(`BF:55160`). La lista carga vía `loadAlarms()` → `$niagara.alarm.getAlarmList({activeOnly, alarmConsole, timeRange,
building, unackOnly, priority, sourceArray, page})` (`BF:14931`) → `query(…)` (el path BQL/RPC de B142/B230). El
filtro de clase sale de `getClassList`. Las columnas visibles se controlan por flags per-console
(`sourceColumnTimestamp`, `alarmColumnAckState`…). Una vista agrupada por fuente ofrece un menú de Actions
(`ackRecent`/`hyperlink`/`notes`/View Alarms) + un botón **Acknowledge All**.

## 240.3 — Ack / ackAll: vía Baja NATIVO (no un command Reflow) `[CERT]`

- **Single ack** (`ack`, `BF:34305`): si `alarmConsole.acknowledgmentRequiresNote` abre un modal de nota, si no
  llama `sendAck(uuid)` → `$niagara.alarm.ackAlarmsByUuid`. Ese helper (`BF:14618`) va **directo por Baja nativo**:
  `$baja.Ord.make("alarm:").get()` → `ackAlarms({ids})` — invoca la action nativa `BAlarmService.ackAlarms`
  client-side, **bypaseando `BReflowAlarmCommands`** `[CERT]`.
- **Batch ackAll** (`BF:30959`): resuelve los UUIDs de las fuentes time-boxed vía `getUuidForSources` (RPC
  `wi.spec.ALARM`) y luego emite `ack-all` → modal de confirmación → `sendAck`.
- **Priority filter**: usa `priorityType` del console (`class` vs numérico `rangePriorities.low/high`).

**Nota de producto**: el ack NO usa el subsistema de comandos de Reflow — usa el `BAlarmService` nativo de Niagara
directamente desde el cliente. Es otra pieza donde Reflow delega a la plataforma (como los assets, B219).

## 240.4 — Alarmas audibles (sonidos) `[CERT]`

El loop de alarma audible vive en un helper module-level (no Vuex): `startAlarmSounds`/`checkAlarmSounds`/
`playAlarmSound`/`stopAlarmSounds`/`invokeSoundOrd` (`BF:15087`). `checkAlarmSounds(classes, console)` polea
`getAlarmsSinceTimestamp` (RPC `wi.spec.ALARM`), filtra transiciones `["fault","offnormal","alert"]`, y elige el
sound ORD de la prioridad MÁS ALTA que matchea, de `alarmConsole.sounds[class]`. `playAlarmSound` →
`invokeSoundOrd(ord)` → `$ord.sound(ord)` (reusa el resolver `image()`: `module://`→`/module/`, B219) → **`new
Audio(url); play()`** (`BF:15122`) `[CERT]`. Se dispara desde los refresh loops de consola/widget, gateado por
`alarmConsole.soundsEnabled`, con start/stop en mount/destroy. El picker de sonidos (`AlarmSoundsPicker`, con botón
de play inline) navega la `sound-library` por nav-RPC (`getNavChildren("module://nmodsreflow/sound-library")`,
confirma B219) y asigna `alarmConsole.sounds{class→ord}` por `UPDATE_CONSOLE`.

## 240.5 — El store de alarmas `[CERT]`

Dos módulos Vuex: `alarms` (`BF:4319`) tiene `consoles[]` — la config completa de cada consola
(`classPriorities`/`rangePriorities`, `priorityType`, `soundsEnabled`+`sounds`, `acknowledgmentRequiresNote`,
`consoleRefreshRate`, flags de columnas, `defaultTimeRange`); mutaciones `ADD/UPDATE/REMOVE/REORDER_CONSOLE`; getters
`priorityLabel`/`priorityColor`. `alarmData` (`BF:9706`) tiene `{loading, currentTimeRanges}` + getters
`priorityForRecord`/`inAlarmCount` (el conteo del badge). **No hay estado de polling/subscription** — los conteos se
piden on-demand por `getAlarmList`.

## 240.6 — Conexiones

- **[Block 142]** — el backend de alarmas (query BQL, doPrivileged, BQL injection); §240 es la cara cliente
  (widget/consola/ack/sonidos).
- **[Block 219]** §219.5 — la `sound-library` (11 MP3) que los sonidos audibles reproducen vía `$ord.sound()`.
- **[Block 218]** — la card `alarm` en el catálogo.
- **[Block 237]** — el CSV de alarmas server-side (`AlarmCSVResponse`) es el otro output de alarmas.
- **Cierre grupo E**: falta solo BG25 (schedules) para cerrar el grupo.
