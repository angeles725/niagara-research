# Bloque 259 — Síntesis de cierre: el charting clásico de Niagara N4 (focus `px-chart-classic`)

> **Qué documenta**: el cierre del focus `px-chart-classic` — **8/8 gaps, bloques B251-B258**. No aporta
> evidencia nueva: consolida en **seis hilos transversales** lo que los ocho bloques establecieron por
> separado, y deja el estado del subsistema para quien retome.
>
> **Alcance**: el módulo `chart` completo (67 clases distintas: `chart-rt` 5 + `chart-wb` 62) y su relación
> con los consumidores. Todas las afirmaciones remiten a su bloque de origen; **las citas primarias viven
> allí**, no se repiten acá.
>
> **TIPO DE BLOQUE: SÍNTESIS** (§11) — un ratio `[INFER]`/`[CERT]` alto es ESPERADO y sano en un bloque de
> síntesis, y **no** es señal de agotamiento (el agotamiento ya se declaró por otra vía: investigable = 0).
> Marcadores: `[INFER]` = deducción de síntesis; `[CERT]` = remisión a evidencia ya citada.

---

## 259.1 — Hilo 1: dos motores paralelos, no una sucesión `[INFER]`

La pregunta que abrió el focus era por qué N4.14 arrastra dos sistemas de charting. La respuesta, con
evidencia: **no los arrastra — los mantiene**.

- **Ausencia probada**: cero `@Deprecated` en las 67 clases ([Bloque 254] §254.8).
- El factor que decide **no es la antigüedad sino el PERFIL**: Workbench/Swing → clásico; browser moderno →
  `webChart` D3 ([Bloque 199]); **browser Hx legacy → clásico rasterizado en servidor, sin interacción**
  ([Bloque 256] §256.3).
- No hay conversión ni ruteo entre ambos: quien quiere gráficos en los dos mundos **escribe dos
  implementaciones** (`analytics-wb` tiene `BAnalyticChartBinding` y `BAnalyticWebChartBinding`; `history-wb`
  tiene `BHistoryChart` y `BChartFile`) ([Bloque 254] §254.8).

`[INFER]` La lectura correcta para quien planifique trabajo sobre N4: el chart clásico **no es deuda técnica
en vías de retiro**, es el motor vigente de un perfil que sigue vivo. Apostar a que "webChart lo reemplaza" no
tiene respaldo en el código de 4.14.

## 259.2 — Hilo 2: la extensibilidad está partida en dos mitades `[INFER]`

Fue el hilo que más correcciones costó — y el más instructivo. Tres bloques hicieron falta para verlo entero:

| Capa | Mecanismo | ¿Se usa? | Origen |
|---|---|---|---|
| Bindings del chart | `@AgentOn(types={"chart:Chart"})` | **sí** (los 2 propios + `seriesTransform`) | [Bloque 253] §253.2 |
| Resolución automática dato→eje | `getAgents()` filtrado por `BAxis.TYPE` | **no** — nadie la ejerce | [Bloque 253] §253.5 + [Bloque 254] §254.3 |
| Elección manual de eje | **`getTypes()`** en el property sheet | **sí** — es la vía real | [Bloque 255] §255.6 |
| Renderers (ejes, swatches) | setters Java, uno estático | única opción | [Bloque 252] §252.5 |

`[INFER]` Conclusión: **la capa de datos es declarativa; la capa de dibujo es cableada.** Un OEM puede
registrar tipos y bindings, y sus ejes aparecerán en el desplegable del usuario. Lo que **no** puede es cambiar
declarativamente cómo se pintan los ejes: eso exige código que corra y llame a un setter estático.

**Lección de método** (§14 de [Bloque 253] §253.7): mi tesis inicial —"módulo pre-agentes"— fue una
generalización desde una sola capa leída. Corregirla exigió abrir la capa de binding, el mapa de consumidores
y la implementación privada. *No se concluye sobre la arquitectura de un módulo con un paquete leído.*

## 259.3 — Hilo 3: el diseño es ansioso, y todo el módulo paga `[INFER]`

Una decisión estructural recorre el subsistema entero: **cargar todo, ya**.

- `TableSeries` hace `Tables.slurp()` — materialización completa del `BITable` en memoria al construir la
  serie ([Bloque 251] §251.3).
- Como consecuencia, `analytics-rt` necesita `BChartRenderLimitConfiguration` con topes por tipo de gráfico
  (3.000 filas para agregación, 250.000 para ranking) — cuyo **enforcement no pudo verificarse**
  ([Bloque 254] §254.4).
- Y por el mismo costo, la exportación a PDF **traspasa el modelo en vez de copiarlo**, dejando al chart en
  pantalla momentáneamente vacío y obligando a un `refresh` reparador explícito ([Bloque 256] §256.1-2).
- La excepción elegante: `BoundTimeSeries` **sí** es incremental — páginas de 256 con filtro por cambio, de
  modo que una señal plana no consume memoria ([Bloque 255] §255.4).

`[INFER]` El patrón: el camino de **tabla/history** es ansioso y caro; el camino de **valor vivo** es
incremental y barato. Quien diseñe sobre este motor debería asumir que un chart de history con una consulta
grande se paga entero, de una vez, en el heap del Workbench.

## 259.4 — Hilo 4: cinco defectos con una causa estructural común `[CERT]`

El focus confirmó **cinco defectos** leyendo código, no ejecutándolo:

| # | Defecto | Efecto | Origen |
|---|---|---|---|
| 1 | `assignColors()`: `return` donde iba `continue` | si la 1ª serie ya tiene brush, **ninguna** posterior recibe color | [Bloque 252] §252.7-a |
| 2 | `BChartHeader` evalúa `title.length()` para el **subtítulo** | el slot `subtitle` es inútil sin `title` | [Bloque 252] §252.7-b |
| 3 | `BoundTimeSeries`: escritura sincronizada, lectura no | carrera real entre el poll de 2 Hz y el hilo de pintado | [Bloque 255] §255.7-a |
| 4 | `makeGradient()` emite dos stops en 0% | el color original del brush **nunca se rinde** (afecta a `BAreaChart`) | [Bloque 255] §255.7-b |
| 5 | Misma carrera en `BResourceManager` | ídem, con el `PollThread` de 1 s | [Bloque 255] §255.7-c |

Y [Bloque 257] encontró la causa estructural: **el módulo distribuido no contiene un solo test automatizado**
—cero JUnit, cero assertions— sino harnesses visuales que abren una ventana con datos aleatorios, uno de
ellos con el `main()` literalmente vacío.

`[INFER]` Los cinco defectos son exactamente los que una assertion trivial caza y una inspección visual no.
No es coincidencia: es consecuencia.

## 259.5 — Hilo 5: el reparto rt/wb es un principio, no un accidente `[INFER]`

`chart-rt` declara **una** dependencia (`baja`); `chart-wb` declara **catorce** ([Bloque 258] §258.1). Al
runtime va **solo lo que se serializa**: los 4 tipos registrados que pueden aparecer escritos en un `.px` o
`.bog` (`AxisDimension`, `AxisLocation`, `AxisBound`, `ColumnIdentifier`) más la convención de bits
`TrendFlags`, que no se registra por no ser un `BObject`.

`[INFER]` Ese reparto explica de raíz el hilo 1: el clásico **no puede** servir al browser por sí mismo porque
su motor no existe en el runtime. El puente Hx no contradice la regla — la confirma, porque necesita el `-wb`
cargado del lado servidor y por eso entrega una imagen sin interacción.

## 259.6 — Hilo 6: quien extiende, hereda todo `[INFER]`

Los consumidores no envuelven el chart clásico: lo **extienden** ([Bloque 254] §254.2). `BAggregationChart`,
`BAverageProfileChart`, `BEquipmentOperationChart`, `BRankingChart`, `BSpectrumChart` son subclases de
`BChart`; `honeywellSpyderTool` usa `BPieChart`/`BChartPane` directos ([Bloque 254] §254.7).

`[INFER]` Consecuencia práctica y verificable: **los gotchas de §259.3 y §259.4 son también gotchas de
Analytics, de la vista de histories y de la herramienta de comisionamiento Spyder.** El tope de 12 colores con
caída silenciosa a negro, el `slurp` completo, el búfer solo-AWT y los cinco defectos no están confinados al
módulo `chart`: se propagan por herencia a todo lo que dibuje gráficos en Workbench.

## 259.7 — Estado de cierre del focus

- **Cobertura**: **8 / 8** gaps (ratio 1.00). Bloques **B251-B258** + esta síntesis (B259).
- **STOP (§8)**: `read-only-investigable = 0`. `requires-execution` = 0. `blocked` = 0.
- **Único punto explícitamente NO resuelto**: el **enforcement** de `BChartRenderLimitConfiguration` — los
  valores están, los llamadores no aparecen en el decompilado disponible ([Bloque 254] §254.4). Reproducirlo
  exige una station viva con Analytics licenciado → **requires-execution**, fuera del alcance read-only.
- **Marcado UNVERIFIED, no bug**: el `first = sIndex - 1` de `BoundTimeSeries.trim()` ([Bloque 255] §255.7-d)
  y el `locs.length - 2` de `BDiscreteAxis.fromDisplaySpace()` ([Bloque 252] §252.7-i).

### Conexiones al resto del corpus

- **[Bloque 199]** (`webChart`), **[Bloque 201]** (`BMwChart`), **[Bloque 194]** (perfiles Wb/Hx/Mobile) — los
  tres bloques PX que nombraban el chart clásico sin abrirlo; este focus los cierra.
- **[Bloque 66]-[Bloque 68]** (analytics) — §259.6 responde de qué está hecho su render en Workbench.
- **[Bloque 211]/[Bloque 212]/[Bloque 214]** — el contraste de extensibilidad del hilo 2.
- **[Bloque 251]-[Bloque 258]** — los ocho bloques de evidencia que esta síntesis consolida.
