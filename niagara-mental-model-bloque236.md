# Block 236 — Reflow: el subsistema de clima (weather)

> **Qué documenta.** Las cards de clima (`weather-current`/`weather-forecast`), de dónde saca la data y cómo se
> configura la ubicación. Gap BG26 (reapertura grupo E). Completa la vista de clima que B222 empezó (weather-map).
>
> **Alcance.** El componente Weather + la fuente de datos + config de ubicación. El weather-MAP (radar Mapbox) es
> B222.
>
> **Fuentes (primarias).** SPA beautificada (`BF:`, 1:1 `app.4509efb4.js` sha256 `81b82b83…`). Barrido delegado
> (sonnet); tokens re-verificados por el driver.
>
> **Método / markers.** `[CERT]` = fuente primaria. `[INFER]` = deducción.

---

## 236.1 — Un componente `Weather` compartido para current + forecast `[CERT]`

`weather-current` y `weather-forecast` renderizan con el MISMO componente `Weather` (`BF:2081`,
`"weather-current"==type ? Weather : "weather-forecast"==type ? Weather`), gateado por `!weatherDisabled`. Labels
(`BF:100470`): current → "Current Weather"; forecast → "Weather Forecast".
- **Current** (`BF:25650`): temp, high/low, `weatherData.current.summaryShort`, icono, ubicación + panel de detalles
  (viento, humedad, presión).
- **Forecast** (`BF:25722`): itera `weatherData.forecast[]` (día, icono, high/low) mapeado de los `periods` de Aeris.

Config de la card: `{title, titleStyle, titleColor, locationDisplay, locationCustomDisplay, textColor, accentColor,
forecastColor}` (`BF:99583`).

## 236.2 — La fuente de datos: AerisWeather vía cloud-proxy de NiagaraModules `[CERT]`

El provider subyacente es **AerisWeather** (el error-state linkea a `status.aerisweather.com`, `BF:25719`). Reflow
NO llama a Aeris directo — pasa por el **cloud-proxy de NiagaraModules** (mismo patrón que el radar de B222):
- URLs base (`BF:9843`): `https://weather.niagaramodules.com/observations`, `.../forecasts`, `.../maps` (maps = B222).
- El módulo Vuex `weatherData` (fetch/estado) en `updateCurrentConditions` (`BF:10126`) hace DOS llamadas HTTP con
  el wrapper propio (`.get`, **no axios** — B216 §216.4): `GET {observations}/{loc}?…&host={license.hostId}` +
  `GET {forecasts}/{loc}?…limit=5…&host={license.hostId}` `[CERT]`. El `host={license.hostId}` es el MISMO patrón de
  gating por licencia que el mapa (B222) — el proxy de NiagaraModules valida el hostId.
- Hay dos módulos Vuex bajo el namespace weather: `weather` (settings) y `weatherData` (fetch/estado). La respuesta
  se mapea (`Yn`) a `{current, forecast}` leyendo `tempF/tempC, weather, icon, humidity, windSpeed, periods[]…`.

**Implicación de producto**: el clima depende del **cloud de NiagaraModules** (con el hostId de la station) — igual
que el radar (B222). Sin conexión a ese cloud, no hay clima. Es una dependencia externa notable.

## 236.3 — Config de ubicación: una por station (con override per-card) `[CERT]`

El módulo settings `weather` (`BF:8828`) define UNA ubicación por station: `locationType` (`city`/`zip`/`coord`),
`locationZip`, `locationCity`, `locationLat`/`locationLon`, `units` ("imperial"), `enabled`, + settings de
icono/color. El getter `aerisLocationString` (`BF:9985`) resuelve el string de ubicación Aeris según `locationType`
(`zip`→zip; `coord`→"{lat},{lon}"; `city`→city). Una card PUEDE overridear con su propia
`config.locationType/Zip/Lat/City` (`BF:100155`, gana sobre el global). El gate global es `weather.enabled`
(`weatherDisabled`, `BF:100509`) — presente en toda la UI de clima y en la visibilidad de nav.

## 236.4 — Conexiones

- **[Block 222]** §222.4 — el `weather-map` (radar) usa el mismo cloud `weather.niagaramodules.com` + hostId; §236
  agrega `observations`/`forecasts` (los datos de current/forecast). El weather-map se gatea por
  `license.limits.maps` (B232); las cards current/forecast por `weather.enabled` (distinto).
- **[Block 218]** — `weather-current`/`weather-forecast` en el catálogo (comparten componente).
- **[Block 232]** — el `license.hostId` que el proxy de clima valida.
- **Hacia adelante**: BG27 (history/CSV), BG24/BG25 (alarmas/schedules), BG28/BG29 (users/nav) — resto del grupo E.
