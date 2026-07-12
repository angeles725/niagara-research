# Block 241 — Reflow: schedules (widget + edición vía WebScheduler nativo)

> **Qué documenta.** El frontend de schedules de Reflow: el widget `schedule-list`, cómo se editan los horarios
> (WebScheduler nativo de Niagara) y la página global de schedules. Gap BG25 (reapertura grupo E). **Cierra el
> grupo E.**
>
> **Alcance.** La cara cliente de schedules. Cross-ref al subsistema PX/hx (B194, B218).
>
> **Fuentes (primarias).** SPA beautificada (`BF:`). Barrido delegado (sonnet); tokens re-verificados.
>
> **Método / markers.** `[CERT]` = fuente primaria. `[INFER]` = deducción.

---

## 241.1 — El widget `schedule-list`: suscribe a ORDs pre-elegidos `[CERT]`

Componente `ScheduleList` (`BF:26947`). Usa el mixin genérico de suscripción Baja (compartido por muchas cards, no
específico de schedule): `subscribedOrds = card.config.schedule` — un array de **ORDs elegidos por el usuario** — que
resuelve y suscribe en vivo vía `$niagara.subscriber.subscribe(...)`. Es decir, el widget NO hace query BQL de
descubrimiento; se **suscribe directo** a los schedules que el usuario arrastró a la config de la card. Cada fila
muestra: nombre (`getDisplayName`), valor actual (`get("out").getValueDisplay()`), y la próxima línea de evento
(`getNextTime()`/`getNextValue().getValueDisplay()`) o "No future events scheduled" (`BF:26965`).

## 241.2 — Edición: el WebScheduler NATIVO de Niagara (no un editor propio) `[CERT]`

Reflow NO construye un editor de schedule propio — clickear un schedule abre la **vista hx nativa de Niagara** por
ORD. `ScheduleList.deviceLink` (`BF:27035`): elige la vista según el tipo —
`schedule:CalendarSchedule`→`view:schedule:WebCalendarScheduler`, si no `view:schedule:WebScheduler`— y hace
`$niagara.browser.open({ord: navOrd+"|"+view, saveable:true})` `[CERT]`. Es la MISMA delegación a la plataforma que
vimos en assets (B219) y ack (B240): Reflow embebe/navega la herramienta nativa en vez de reimplementarla.

## 241.3 — La página global de schedules `[CERT]`

El `cardClick` de la página `/schedules` (`BF:35053`) es un superset: mapea cada tipo a su vista nativa
(`WeeklySchedule`→`WebScheduler`, `CalendarSchedule`→`WebCalendarScheduler`, else→`WebTriggerScheduler`,
`TrendScheduleImport`→`scheduleHome`), y la abre como **popup dialog** (`schedulesState.linkStyle==="popup"` →
`$niagara.browser.open`, el patrón `BNiagaraWbDialog` de B185/PopupBinding) o como **página inline** vía
`$reflowLink({linkType:"ord", displayType:"page", viewOrd})` que resuelve por el renderer `hx`/`HX` (la misma
mecánica que la card `hx`, B218 §218.5) `[CERT]`.

## 241.4 — El store: descubrimiento por BQL `[CERT]`

El widget no tiene estado Vuex propio (es estado Baja en vivo por el mixin). Para la PÁGINA global hay dos módulos:
`schedules` (`BF:34821`, grupos/nav — `getScheduleGroups`/`getContainedSchedules`) y `scheduleData` (`BF:11563`,
`{folderCache, scheduleCache}`). La acción `LOAD_SCHEDULES` corre un **BQL de descubrimiento**:
`<folder>|bql:select * from schedule:WeeklySchedule, schedule:CalendarSchedule, schedule:TriggerSchedule[,
TrendN4:TrendScheduleImport]`, y filtra los componentes tagueados `r:ignore` (`BF:11579`) `[CERT]`. Es decir: la
página global DESCUBRE los schedules de la station por BQL; el widget del dashboard NO (solo suscribe a los
pre-elegidos).

## 241.5 — Conexiones y cierre del grupo E

- **[Block 218]** — la card `schedule-list` en el catálogo; §241 la reconstruye.
- **[Block 194]** (px/hx) / **[Block 185]** (PopupBinding) — el WebScheduler se abre por el mismo patrón
  `browser.open`/`BNiagaraWbDialog`; nexo con el subsistema PX.
- **[Block 229]** — el `BoundLabel` de floorplans usa el MISMO mixin de suscripción Baja que el `schedule-list`.
- **[Block 219]/[Block 240]** — el patrón "delegar a la herramienta nativa Niagara" (assets, ack, WebScheduler)
  es transversal.
- **CIERRE GRUPO E** (BG24-BG29 = B236-B241): alarmas, schedules, weather, history/CSV, users/profiles, nav/equipment
  completos. La reapertura queda en **14/16** — solo falta el grupo C (dinámico), pendiente de OK del usuario.
