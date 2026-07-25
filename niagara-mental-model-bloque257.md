# Bloque 257 — Chart clásico (VII): los "tests" no son tests — y eso explica los defectos

> **Qué documenta**: el paquete `com.tridium.chart.test` (8 clases) — qué son realmente, qué contratos
> ejercitan, y qué revela su naturaleza sobre la práctica de ingeniería del módulo. Cierra el gap **H7** del
> focus `px-chart-classic`.
>
> **Hallazgo principal**: no hay **ni un solo test automatizado** en el módulo `chart`. Lo que hay son
> **harnesses visuales manuales**. Ver §257.1 y la conexión causal con los defectos del focus en §257.4.
>
> **Fuentes** (decompilado vineflower, **leídas íntegras inline por el driver**; las 8 clases suman 450
> líneas):
> - `$T` = `…/chart/chart-wb/vineflower/com/tridium/chart/test/`
> - Raíz: `/home/cristian/modules/Prototipos/modulos/organized/`
>
> **Método**: sin sub-agente. Lectura directa + tres barridos de ausencia. Marcadores: `[CERT]` = fuente
> primaria; `[INFER]` = deducción. Bloque de EVIDENCIA.

---

## 257.1 — Ausencia probada: cero testing automatizado `[CERT]`

Tres mediciones sobre las 8 clases del paquete:

| Barrido | Resultado |
|---|---|
| `rg -c 'org.junit\|testng\|assert\|Assert\.'` | **0 archivos** `[CERT]` |
| `rg -l 'public static void main'` | **6 de 8** `[CERT]` |
| `rg -c 'BFrame'` | **5 archivos** (`LineTest`, `BarTest`, `AreaTest`, `PieTest`, `StackedBarTest`) `[CERT]` |

`[CERT]` **No hay framework de test, no hay assertions, no hay aserción de ningún tipo.** Las 8 clases se
reparten así:

| Clase | Qué es |
|---|---|
| `LineTest`, `BarTest`, `AreaTest`, `PieTest`, `StackedBarTest` | **Harnesses visuales**: un `main()` que arma un modelo con datos aleatorios, mete el chart en un `BFrame` y lo abre en pantalla |
| `TestSeries`, `TestTimeSeries` | **Generadores de datos sintéticos** — implementaciones de `Series` que no leen nada real |
| `TimeSeriesTest` | Dos helpers de construcción de ejes… y un `main()` **vacío** (§257.3) |

## 257.2 — Cómo es un "test" acá `[CERT]`

`LineTest` completo en su esencia (`$T/LineTest.java:25-42`):

```java
   public static void main(String[] args) {
      int rows = Integer.parseInt(args[0]);
      double max = Double.parseDouble(args[1]);
      int count = Integer.parseInt(args[2]);
```
`$T/LineTest.java:25-28` `[CERT]`

Toma **tres argumentos de línea de comandos** (filas, valor máximo, cantidad de series), arma un
`SimpleChartModel` con `count` series de datos aleatorios, y:

```java
      BFrame f = new BFrame();
      f.setContent(new BBorderPane(new BChartPane(chart)));
      f.setScreenBounds(200.0, 10.0, 700.0, 500.0);
      f.open();
```
`$T/LineTest.java:39-42` `[CERT]`

`[INFER]` El "resultado del test" es **una ventana que un humano mira**. No hay salida verificable, no hay
código de retorno, no hay comparación contra un esperado. Es un banco de pruebas de desarrollo, no una
verificación.

Detalle útil que sí aporta: `makeAxis()` (`:16-23`) muestra la receta canónica de configurar un
`BNumericAxis` — `setMinAuto(false)` + `setAxisMin(BDouble.make(0.0))` + `setMaxAuto(true)`, o sea **piso fijo
en cero y techo automático**, que es la configuración habitual de un gráfico de magnitudes. Y `LineTest:32`
pasa `BFacets.make("units", BUnit.getUnit("kilowatt hour"))` a la segunda columna — confirma que los facets de
unidad viajan por columna hasta el eje ([Bloque 252] §252.3).

`TestSeries` genera ruido puro: `this.data[i] = maxValue * rand.nextDouble()` (`$T/TestSeries.java:29`)
`[CERT]`, y trae **otra expansión hardcodeada** hermana de las ya documentadas: si `min == max`, ensancha
±5.0 (`$T/TestSeries.java:39-41`) `[CERT]` — comparar con el ±10.0 de `BNumericAxis` ([Bloque 252] §252.7-e) y
el 0–10 de `JoinTable` ([Bloque 251] §251.4). **Tres constantes distintas para el mismo problema**, en tres
clases distintas.

## 257.3 — El test que no hace nada `[CERT]`

```java
   public static void main(String[] args) {
   }
```
`$T/TimeSeriesTest.java:25-26` `[CERT]`

`TimeSeriesTest` define dos helpers (`makeNumericAxis`, `makeTimeAxis`) y **su `main()` está vacío**. La clase
que debería ejercitar la serie temporal — la parte del módulo con la carrera de datos documentada en
[Bloque 255] §255.7-a — no ejercita nada.

`[INFER]` Es un esqueleto que quedó sin escribir y se embarcó igual en el jar de producción de N4.14.

## 257.4 — Por qué esto importa: la explicación de los defectos `[INFER]`

Este focus lleva **cinco defectos confirmados** en el módulo, todos hallados por lectura de código:

| # | Defecto | Bloque |
|---|---|---|
| 1 | `assignColors()` usa `return` donde iba `continue` → las series posteriores a la primera coloreada quedan sin color | [Bloque 252] §252.7-a |
| 2 | `BChartHeader` evalúa `title.length()` para decidir si pinta el **subtítulo** | [Bloque 252] §252.7-b |
| 3 | Carrera de datos: `sample()` sincronizado, `getValue()`/`getSampleCount()` no | [Bloque 255] §255.7-a |
| 4 | `ChartUtil.makeGradient()` emite dos stops en 0% → el color original nunca se rinde | [Bloque 255] §255.7-b |
| 5 | Misma carrera en `BResourceManager` | [Bloque 255] §255.7-c |

`[INFER]` **Los cinco son exactamente el tipo de defecto que un test automatizado caza y una inspección visual
no.** Los defectos 1, 2 y 4 son verificables con una assertion trivial (¿cuántas series quedaron sin brush?
¿se pintó el subtítulo con título vacío? ¿cuántos stops distintos tiene el gradiente?). Los 3 y 5 son
condiciones de carrera que ningún vistazo a una ventana revela.

Y son invisibles para el banco de pruebas que **sí** existe: `LineTest` con datos aleatorios abre una ventana
donde un gradiente ligeramente distinto o una serie negra de más pasan por decisión de diseño. La ausencia de
§257.1 no es una curiosidad de inventario — **es la causa estructural del patrón de defectos que este focus
documentó**.

`[INFER]` Matiz honesto: esto describe el paquete `test` **embarcado en el jar distribuible**. Que Tridium no
tenga tests automatizados en otro repositorio interno no es demostrable desde acá — lo que sí es un hecho es
que el módulo distribuido no trae ninguno, y que el código distribuido contiene esos cinco defectos.

## 257.5 — Conexiones

- **[Bloque 252]** y **[Bloque 255]** — los cinco defectos que §257.4 explica.
- **[Bloque 251]** §251.4 y **[Bloque 252]** §252.7-e — la tercera constante de expansión min==max (§257.2).
- **[Bloque 199]** (`webChart`) — queda abierta la comparación de práctica de testing entre el motor clásico y
  el moderno; no se investigó y **no se afirma nada** al respecto.
- **`niagara-mental-model-bloque-test-infrastructure.md`** — el bloque de infraestructura de test del corpus;
  este hallazgo es un caso concreto que lo complementa por contraste.
- **Gap abierto**: H8 (split rt/wb, 5 clases) — el último del focus.
