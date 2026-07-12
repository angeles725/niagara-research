# Block 218 — Reflow dashboard-builder (III): el catálogo de widgets y cómo se agrega uno

> **Qué documenta.** El catálogo COMPLETO de tipos de tarjeta/widget que Reflow ofrece (la "paleta" que el
> usuario elige), el mapeo `type → componente` que los renderiza, el esquema/defaults de config por tipo, y el
> flujo real de "agregar un widget". Gap BG5 del focus `nmodsreflow-builder`. Responde "qué cosas se le pueden ir
> agregando al dashboard".
>
> **Alcance.** El registro de tipos, el dispatch de render, defaults por tipo y el add-flow. Cruza DOS fuentes:
> el bundle SPA (catálogo OFRECIDO) y un dashboard REAL de disco (catálogo USADO + schema real). NO cubre el
> editor visual/layout completo (BG4) ni el detalle de assets (BG7/BG8).
>
> **Fuentes (primarias).**
> - SPA beautificada (1:1 con `app.4509efb4.js` sha256 `81b82b83…`, build 1.7.7.75): temp
>   `scratchpad/reflow-app.beauty.js` (123 740 líneas), citada `BF:<línea>`.
> - **Dashboard REAL de disco** (station del cliente): `…/stations/HoneywellMX605132026/shared/reflow/config.json`
>   (247 854 B, `reflowVersion 1.7.5-43`, schema v14). Se cita por estructura/jq-path; probe sanitizado en
>   `sources/probes/B218-dashboard-catalog-real-20260712.txt`.
> - Barrido delegado (sonnet) sobre el beautified; tokens load-bearing re-verificados por el driver.
>
> **Método / markers.** `[CERT]` = leído en fuente primaria (`BF:` bundle 1:1 · `config.json` de disco por
> jq-path). `[INFER]` = deducción. **Divergencia de versión** (§217): el catálogo del bundle es 1.7.7; el
> dashboard real es 1.7.5 — mismo schema v14, catálogo estable. SECRETS DISCIPLINE: se citan TIPOS y KEYS de
> schema, nunca los valores del sitio del cliente.

---

## 218.1 — El catálogo: 20 tipos ofrecidos (registro `cardTypes`) `[CERT]`

El editor de tarjetas puebla su `<Select>` "Card Type" iterando el registro `cardTypes` (`BF:100416`), corroborado
por los switches `badgeForCard()` (`BF:91255`) y `nameForCard()` (`BF:91301`). Los **20 tipos canónicos** (string →
label de UI):

| type | Label | type | Label |
|---|---|---|---|
| `alarm` | Alarm Display | `hyperlink` | Hyperlink |
| `building-map` | Buildings Map | `point-display` | Niagara Point |
| `circle` | Circle | `point-list` | Niagara Point List |
| `equipment-type` | Device Summary | `schedule-list` | Schedule List |
| `divider` | Divider | `table` | Table |
| `equipment-list` | Equipment List | `toggle` | Toggle |
| `gage` | Gauge | `url` | URL |
| `historyChart` | History Chart | `weather-current` | Current Weather |
| `history` | History Sparkline | `weather-forecast` | Weather Forecast |
| `hx` | HX View | `weather-map` | Weather Map |

**19-vs-21 resuelto** `[CERT]`: el renderer (`BF:92554`) acepta además el **alias legacy `"gauge"`** junto a
`"gage"` (`"gage"===type || "gauge"===type ? …Gauge`). `"gauge"` NO está en el registro `cardTypes`, así que **no
se puede crear nuevo desde la UI** — sólo dashboards viejos con el string antiguo lo siguen renderizando. Conclusión:
**20 strings elegibles en el picker · 21 strings que el renderer reconoce** (20 + alias `gauge`).

**Validado contra el dashboard real** `[CERT]` (config de disco): usa 10 de los 20 tipos —`historyChart` (6),
`point-list` (6), `equipment-list` (3), `hyperlink` (3), `alarm` (2), `equipment-type` (2), `building-map`,
`circle`, `point-display`, `weather-current`— todos presentes en el registro. Un dashboard de producción típico
ejerce ~la mitad del catálogo.

## 218.2 — Dispatch `type → componente` (cadena `v-if`, no `<component :is>`) `[CERT]`

La resolución del tipo al componente Vue que lo dibuja es una cadena `v-if`/`v-else-if` en el render de preview del
editor (`BF:92544-92651`), no un `<component :is>` dinámico:

`alarm→AlarmDisplay` · `building-map→BuildingMap` · `gage/gauge→Gauge` · `equipment-list→EquipmentList` ·
`equipment-type→EquipmentType` · `point-list→PointList` · `point-display→NiagaraPoint` · `history→HistorySpark` ·
`historyChart→HistoryChart` · `hyperlink→Hyperlink` · `table→Table` · `toggle→ToggleCard` · `hx→HX` · `url→URLCard`
· `circle→CircleCard` · `divider→Divider` · `schedule-list→ScheduleList` · `weather-map→WeatherMap` (**gateado por
`license.limits.maps`**, `BF:92635`) · `weather-current`/`weather-forecast`→**un único componente compartido**
`Weather` (gateado por `weather.enabled`, `BF:92642`). Registro de imports en `components:{}` del editor
(`BF:100377`).

Dos gates de producto notables `[CERT]`: **Weather Map requiere licencia** (`license.limits.maps`) y las dos cards
de clima comparten componente y dependen de `weather.enabled`.

## 218.3 — Esquema y defaults de config por tipo `[CERT]`/`[INFER]`

Las tarjetas se crean **vacías** (`config:{}`, §218.4); la mayoría de tipos NO tiene objeto factory estático — aplican
**fallbacks perezosos inline** (`config.x || default`) en cada render. Schema combinado (código del bundle +
observado en el dashboard real):

| type | keys de config (schema) | defaults (código) | evidencia |
|---|---|---|---|
| `circle`/`gage` | `ord, lower, upper` | `ord:null, lower:0, upper:100` | `BF:93137,93209,93279` + real `{ord,lower,upper}` |
| `historyChart` | `chartType, chartColor, range, customYAxis, history, hyperlink, hyperlinkBuilding, title` | `chartType:"area", chartColor:"primary", range:"lastHour"` | `BF:96262,96293,96335` + real |
| `hyperlink` | `title, titleSize, titleColor, textPosition, iconName, iconStyle, iconColor, buttonLabel, backgroundImage, backgroundPosition, link, view, allowed` | `iconName:"far fa-question-circle", backgroundImage:null, textPosition:"center"` | `BF:97250-97420` + real |
| `alarm` | `display, displayType, console, title, link, priorities` | escrito en mount: `display:"total", displayType:"active", title:"Active Alarms"` | `BF:92990` + real |
| `equipment-type` | `typeId, showAll, devices/equipment, view, ord, schedule, hyperlinkStyle, title` | `view:"table"` | `BF:94360` + real |
| `weather-map` | `lat, lon, zoom, type, style, roads, interstates, counties` | `defaultConfig` `zoom:"8", type:"radar", style:"dark"`, merge `Object.assign(defaultConfig, card.config)` | `BF:26882-26927` |
| `divider` | `dividerType, orientation` | `dividerType:"hidden", orientation:"left"` | `BF:100296` |
| `toggle` | `ord, actionOrd` | ambos `null`; `actionOrd` cae a `ord` | `BF:95837,96113` |
| `table` | `columns[], rows[]` | vacíos; se llenan con "Add a column" | `BF:98322,101256` |
| `point-list` | `points, showIcons, statusColor, title` | `[INFER]` fallback perezoso (no factory) | real (config disco) |

`point-list`/`schedule-list`/`equipment-list` no se trazaron al 100% en el código (`[INFER]` mismo patrón perezoso);
sus keys reales salen del config de disco. `weather-map` es el ÚNICO con `defaultConfig` estático mergeado — patrón
distinto (`BF:26882`).

## 218.4 — Cómo se AGREGA un widget (dropdown, no paleta drag) `[CERT]`

El add-flow NO es una paleta arrastrable estilo Canva — es un **dropdown dentro del panel editor**:

1. `newCard()` (`BF:91353`) crea una card en blanco `{id, enabled:true, type:"", config:{}}` y la commitea vía
   `dashboardCards/ADD_CARD` (`BF:8399`, genera `id` guid).
2. Abre de inmediato el drawer lateral `dashboardCardEditor` (evento `dive`) para esa card.
3. Dentro, un `<Select>` "Card Type" (`BF:92432`) poblado por `cardTypes` (`BF:100416`); elegir opción dispara
   `cardTypeChanged` (`BF:100536`) que setea `{type}` vía `mutateCard`.
4. Editar una card existente = mismo panel vía `cardClicked` (`BF:91372`).

El registro `cardTypes` es un objeto PLANO (sin agrupación por categoría, alfabético por key en el source) `[CERT]`.
Cada edición dispara el auto-save debounced (B217 §217.4). Es decir: **"ir agregando cosas" = crear card en blanco →
elegir tipo en un dropdown → configurar en el drawer**, con persistencia automática. El editor visual/layout completo
(mover/redimensionar, masonry) es BG4.

## 218.5 — Widgets especiales (qué incrustan) `[CERT]`

- **`hx` (HX View)** — incrusta una **vista Niagara por ORD en un `<iframe>`** (`BF:23047`, prop `ord`, con
  back-nav); el ORD objetivo vive en `config.view.baseOrd` (`BF:91330`). Es el puente a las vistas Hx nativas de la
  station DENTRO de una tarjeta del dashboard.
- **`url` (URL)** — incrusta una URL externa arbitraria en OTRO `<iframe>` (`BF:23319`), componente/iframe distinto
  de `hx`.
- **`building-map` (Buildings Map)** — renderiza vía **Mapbox GL** (`this.$maps.Mapbox`); su estado (mapa/markers)
  vive en el módulo Vuex global `buildings`, NO en `card.config` (por eso la card real tiene `config` vacío) `[CERT]`.
  Es la vista "3D"=2D de B216 §216.3.
- **`divider`** — sólo layout, sin fuente de datos (`dividerType`, `orientation`, `BF:100296`).

## 218.6 — Conexiones

- **[Block 216]** §216.3 — el `building-map` confirma "3D"=Mapbox 2D; §218.2 agrega el gate de licencia `maps`.
- **[Block 217]** — §218.4 completa el disparador de `ADD_CARD` y el auto-save; §218.1 valida la forma `cards[]`
  con 20 tipos concretos. La divergencia de versión (1.7.5 disco vs 1.7.7 bundle) se hereda de §217 (schema v14).
- **Evidencia dinámica NUEVA**: el dashboard real de disco (`HoneywellMX605132026`) es la primera fuente
  `config.json` RICA del corpus (26 cards, 10 tipos) — valida el catálogo con datos de producción y aporta el
  schema real de cada tipo usado.
- **Hacia adelante**: BG4 (cómo el editor/drawer produce las mutaciones y el layout masonry), BG6 (render interno
  de `circle`/`historyChart`), BG7 (`hyperlink.backgroundImage` + `file:^Imagenes/` — assets), BG8 (upload de esas
  fotos), BG9 (`building-map`/`weather-map` Mapbox), BG11 (chihuahua: qué tipos replicar).
