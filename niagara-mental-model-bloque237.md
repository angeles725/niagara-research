# Block 237 — Reflow: pipeline de datos de history + export CSV (+ corrección §14 a B216)

> **Qué documenta.** Cómo Reflow OBTIENE los datos de series de tiempo para los charts (rango, sin downsampling),
> y cómo se exporta a CSV. Gap BG27 (reapertura grupo E). **Corrige B216 §216.1** (opencsv "exporta history").
>
> **Alcance.** El pipeline de consumo de datos (no el storage, que es B141). El render del chart es B224.
>
> **Fuentes (primarias).** Java RT: `http/responses/HistoryChartDataResponse.java`, `history/HistoryData.java`,
> `util/BDateRangeEnum.java`, `commands/BReflowCSVCommands.java`, `http/responses/AlarmCSVResponse.java`. SPA
> beautificada (`BF:`). Barrido delegado (sonnet); tokens re-verificados.
>
> **Método / markers.** `[CERT]` = fuente primaria. `[INFER]` = deducción. Contiene una corrección §14.

---

## 237.1 — El pipeline de datos: cursor completo, SIN downsampling `[CERT]`

`HistoryChartDataResponse.java` es un adaptador delgado: re-parsea el query-string a un `BComponent` sintético de
slots `BString` (`histories, style, range, limit, comparing, start, end, contextualRanges`) y delega a
`HistoryData.fromComponent` (thread privilegiado). El rango se resuelve por tag `BDateRangeEnum`; **los params
`start`/`end` existen pero NO se consumen** (rama muerta — `range` siempre gana) `[CERT]`.

**Hallazgo clave**: NO hay downsampling/stride server-side. `arrayForHistoryCollection` corre
`conn.timeQuery(history, start, stop).cursor()` (`HistoryData.java:128,148`) y emite **TODOS** los records del rango
como `[timestamp, value, label?]` — un walk de cursor directo, sin decimación `[CERT]`. El único límite es un cap
de count (`limit`) usado por el path sparkline, no por el chart ranged. La feature "contextual ranges" trae 1 record
extra fuera de cada borde (boundary padding para que la línea D3 no arranque/termine plana), NO es downsampling.

**Contraste con chihuahua**: chihuahua SÍ hace stride-downsampling (B174) para no volcar 100k puntos; Reflow manda
todo el rango crudo. Es una diferencia de escalabilidad — el chart de Reflow puede volcar rangos enormes al cliente.

## 237.2 — Vocabulario de rangos: `BDateRangeEnum` (15) `[CERT]`

`BDateRangeEnum.java:12` define 15 rangos: `lastHour, last8Hours, last12Hours, today, last24Hours, yesterday,
weekToDate, lastWeek, last7Days, last30Days, monthToDate, lastMonth, yearToDate, lastYear, last12Months` (default
`today`). El cliente pide el rango como **string de tag** (`range=lastHour`), no timestamps. `RangeCalculator.make`
(server) / `Ea`/`getRange` (cliente) convierten el tag a `BAbsTime[]` concretos. Asimetría menor: la lista cliente
`Oa` (`BF:13296`) omite `last30Days` que el enum server sí tiene `[INFER]` (probablemente inalcanzable desde el
picker estándar).

## 237.3 — El client fetch: dos transportes (HTTP y BQL) `[CERT]`

El módulo Vuex `histories` en la acción `data()` (`BF:13691`) despacha según `fetchMethod`: `"http"` →
`GET /nmodsreflow/station/history-data{queryString}` (→ `HistoryChartDataResponse`); si no → BQL
`wi.json(wi.spec.HISTORY, "getData", …)` `[CERT]`. Son dos transportes paralelos para el MISMO dato, elegidos por
`historiesState.fetchMethod`. `loadList`/`loadGroups` traen el catálogo de historias (`GET /station/histories`,
`/station/history-groups`).

## 237.4 — CSV: corrección — opencsv NO exporta history `[CERT]`

**Corrección §14 a B216 §216.1** (que dijo "opencsv → exportar history a CSV"). La realidad `[CERT]`:
- **`BReflowCSVCommands`** NO es un exportador de history — es un **IMPORTADOR de point-map CSV**: su método
  `loadPointMap` (`BReflowCSVCommands.java:36`) lee un CSV (`opencsv.CSVReader`, `:67`) con columnas
  `displayName, identifier, group, featured, hidden` para el wizard de mapeo de puntos (B228).
- **El export de history a CSV es 100% CLIENT-SIDE** `[CERT]`: `exportToCSV()` (`BF:54321`) arma el CSV
  (`Date,series1,…`) desde los datos ya en memoria del chart, alinea por timestamp y dispara un download
  `data:text/csv` (Blob/data-URI) — SIN round-trip al servidor.
- El ÚNICO CSV generado por el SERVIDOR es para **ALARMAS**: `AlarmCSVResponse.java:29-33` (`alarmData.csv`/
  `alarmSources.csv`, `text/csv`), disparado por `GET /nmodsreflow/station/alarms/csv?type=…` (`BF:32227`,
  `createObjectURL`).

Es decir, **opencsv se usa para (a) importar el point-map y (b) generar el CSV de alarmas — nunca para history**.

## 237.5 — Conexiones y corrección §14

**Corrección §14 aplicada a [Block 216] §216.1**: la fila de `opencsv` decía "Exportar datos de history a CSV" →
corregido a "importar point-map CSV + generar alarm CSV server-side; el export de history es client-side" (nota
insertada en B216).

- **[Block 141]** — el storage de history (GZIP cache); §237 es el pipeline de CONSUMO (query→cursor→JSON).
- **[Block 148]** — `CompareRangeCalculator` (el modo compare de los rangos); §237 usa `RangeCalculator`.
- **[Block 224]** — el `<d3chart>` que consume estos datos.
- **[Block 174]** (chihuahua) — chihuahua downsamplea (stride); Reflow no — diferencia de escalabilidad.
- **[Block 228]** — el point-map CSV que `BReflowCSVCommands` importa.
