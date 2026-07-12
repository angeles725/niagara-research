# Block 224 — Reflow dashboard-builder (IX): render de gauges y charts (+ correcciones §14 a B216/B218)

> **Qué documenta.** Cómo Reflow DIBUJA los widgets de dato-visual: los dos "gauges" (`gage`/`gauge` vs `circle`),
> el `historyChart` y el sparkline. Gap BG6 del focus `nmodsreflow-builder`. **Corrige dos afirmaciones previas**
> (§14): B216 §216.4 (d3 "ausente") y B218 §218.3 (circle "SVG custom").
>
> **Alcance.** El render interno de gauge/chart. La config de esos tipos es B218; el catálogo es B218.
>
> **Fuentes (primarias).** SPA beautificada (1:1 `app.4509efb4.js` sha256 `81b82b83…`):
> `scratchpad/reflow-app.beauty.js`, citada `BF:`. Barrido delegado (sonnet); tokens re-verificados por el driver.
>
> **Método / markers.** `[CERT]` = fuente primaria (`BF:` 1:1). `[INFER]` = deducción. Este bloque contiene
> **correcciones §14** de bloques previos, marcadas como tales.

---

## 224.1 — Hay DOS "gauges" distintos, con render diferente `[CERT]`

El catálogo (B218) ofrece dos tipos que parecen medidores pero se renderizan distinto:

| Tipo | Componente | Render | config |
|---|---|---|---|
| `gage`/`gauge` | `Gauge` | **SVG bespoke** (dial ~270° hecho a mano) | `{ord, lower, upper}` |
| `circle` | `CircleCard` | **wrapper de iView `<Circle>`** (3rd-party) | `{ord, lower, upper, circleStyle}` |

Esto **corrige B218 §218.3** `[CERT]`, que agrupó ambos como "gauge = SVG custom": sólo el `Gauge` es SVG propio.

## 224.2 — El `Gauge`: SVG hecho a mano (dash-array sobre un path fijo) `[CERT]`

`Gauge` (`BF:16025-16068`) dibuja un dial fijo de ~270° con un `<path>` SVG, renderizado DOS veces: un track
(`stroke:trackColor`) y un path de valor (`stroke:strokeColor`, `stroke-dasharray:dashArray`, `stroke-linecap`
round). El valor se codifica como longitud visible de arco: `dashArray` (`BF:16432`) toma `max≈515` (largo del path,
de `data()`), calcula la fracción `a=(valor-lower)/(upper-lower)`, y devuelve `"<Math.floor(max*a)>,1000"` `[CERT]`.
Es la técnica clásica de "gauge por stroke-dasharray" — sin librería de charts, sin d3.

## 224.3 — El `circle`: envuelve el `<Circle>` de iView (NO es SVG propio) `[CERT]` — corrige §14

`CircleCard` (`BF:23449`) **no** hace matemática SVG propia: delega enteramente al componente `<i-circle>` de
**iView/View-Design** (el set de props `percent / stroke-color / trail-color / stroke-width / stroke-linecap /
dashboard` es exactamente la API de `Circle` de iView) `[CERT]`. Reflow sólo computa el prop `percent`
(`BF:23521`): `round(|valor-lower| / (upper-lower) * 100)`, clamp 0/100. `circleStyle==="gauge"` mapea al prop
`dashboard` de iView (dial abierto) vs anillo completo por defecto.

**Corrección §14 a B218 §218.3**: el gauge `circle` NO es "SVG custom" — es un componente 3rd-party (iView Circle);
su matemática de circunferencia/dasharray vive en la librería iView, no en el bundle de Reflow (`[INFER]`/thin).

## 224.4 — `historyChart` = D3.js (componente `<d3chart>`) — corrige §14 a B216 `[CERT]`

**Corrección §14 mayor a B216 §216.4** (que declaró "d3 = 0, AUSENTE"): **D3.js SÍ está presente y es el motor de
charts** `[CERT]`. El `historyChart` se renderiza con un componente cuyo tag literal es `<d3chart>` (`BF:20428`),
registrado `Vue.component("D3chart", …)` (`BF:121730`). Sus internals usan el idiom canónico de d3-selection:
`select(container).append("svg")`, `.selectAll(...).data(...).enter().append(...)` (`BF:120024`), escalas, brush y
transiciones, con un nodo `.attr("class","d3-tooltip")`. Los imports son d3-selection bajo module ids webpack
`cb29`/`898b` (`BF:119418`).

**Por qué el grep original falló**: D3 está **aliaseado bajo module ids de webpack** (`UQ["o"]`=`select`, etc.), no
sobrevive como string literal `"d3"`. El único `"d3"` que aparecía era un PARÁMETRO de query de history
(`style:"d3"` enviado a `/nmodsreflow/station/history-data`) — colisión de nombre, no evidencia de la librería. Los
`chartType` disponibles son más ricos de lo asumido: `area, line, bar, heatmap, scatter` (`BF:53777`), default
`area`. **Lección (§14 + regla del kit "bundle-evidence quality")**: un grep negativo de un nombre de librería sobre
un bundle MINIFICADO/aliaseado NO prueba ausencia — el alias de webpack borra el nombre. La presencia se prueba por
el IDIOM (data-join) o el tag del componente, no por el string del paquete.

## 224.5 — `history` (sparkline) = el mismo `<d3chart>` en modo compacto `[CERT]`

El tipo `history` (sparkline) reusa el MISMO componente `<d3chart>` con `staticClass:"sparkline"` (`BF:20427`) y un
flag `sparkline` true que saltea ejes/leyenda/brush y appendea el grupo del chart directamente (`BF:120647`). No es
un render separado — es el chart D3 con un booleano que simplifica.

## 224.6 — Correcciones §14 (resumen) y conexiones

**Correcciones §14 aplicadas** (se anotan también en los bloques origen):
1. **B216 §216.4** — "d3 ausente" → **d3 PRESENTE** (aliaseado bajo module ids; motor de `historyChart`/sparkline).
   El eje "ausencias verificadas" de B216 se refina: `axios` sí ausente (confirmado, wrapper propio), pero `d3` NO.
2. **B218 §218.3** — gauge `circle` "SVG custom" → **wrapper de iView `<Circle>`**; sólo `gage`/`gauge` es SVG propio.

- **[Block 216]/[Block 218]** — corregidos aquí; ver notas §14 insertadas en ellos.
- **[Block 222]** — Mapbox (building-map) es el otro render "pesado"; junto con d3 (charts) e iView (circle), el
  stack de render es más rico que "todo SVG a mano".
- **Hacia BG13 (modernización)**: d3 + iView (View-Design, ligado a Vue 2) + Masonry.js + mapbox-gl son las libs de
  render a evaluar para un stack moderno (iView está EOL con Vue 2; d3 sigue vigente).
- **Hacia BG10 (síntesis)** y **BG11 (chihuahua)**: chihuahua dibuja sus gauges/estados en ES5 propio (B170/B171);
  comparar con este stack (d3/iView/SVG).
