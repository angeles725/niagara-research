# Block 148 — nmodsreflow.77 (`-rt`): capa util (cierre de superficie — bug de ventana en CompareRangeCalculator, taint funnel CommandHelpers)

> Research de **NiagaraMods Reflow v1.7.7 (build .75), paquete `util/` del runtime `-rt`**: las 8 clases
> utilitarias del módulo. Bloque de **cierre de superficie** (R11): rol de cada clase + los dos hallazgos
> sustantivos (un bug de correctitud en el cálculo del período de comparación, y el embudo que lleva el
> argumento del comando a un `BOrd`), más los negativos que corroboran el análisis de taint de B147. NO cubre
> el contrato de datos frontend↔-rt (R12).
>
> Focus: **nmodsreflow** (arquitectura backend `-rt`). Cierra el gap **R11**. Corpus language: Spanish
> (technical EN).
>
> Sources (primarias, JAR embarcado build .75, decompile Vineflower):
> `RT/` = `/home/cristian/modules/Prototipos/modulos/organized/nmodsreflow77/nmodsreflow77-rt/vineflower/com/niagaramods/nmodsreflow`
> `U/` = `RT/util`.
>
> Método: barrido citado (delegado) + verificación directa de la aritmética del bug y del taint funnel.
> Markers: `[CERT]` fuente primaria local (`file:line`) · `[INFER]` deducción anclada a líneas `[CERT]`.
>
> Capa 26 (OEM tercero NiagaraMods). Connects [Block 141] (History usa `RangeCalculator`/`CompareRangeCalculator`/
> `PointHelper`), [Block 146] (los command agents usan `CommandHelpers.ordFromArgument`), [Block 147]
> (los negativos de `StringUtils`/`Json` corroboran que no hay sanitización en `util/`).

---

## 148.1 — Mapa de la capa util `[CERT]`

| Clase | Rol | Líneas | Cita |
|---|---|---|---|
| `StringUtils` | static-util trivial: `countOccurrences(String,char)` | 15 | `U/StringUtils.java:3-4` |
| `Json` | serializa `BComponent`/`BFacets` → `com.tridium.json.JSONObject` | 97 | `U/Json.java:3-4,15-17` |
| `PointHelper` | recolecta `BControlPoint` bajo device/component | 50 | `U/PointHelper.java:11-13` |
| `NavNodeSerializer` | `StdSerializer<BINavNode>` (Jackson) → JSON de nav | 58 | `U/NavNodeSerializer.java:11,32` |
| `BDateRangeEnum` | `BFrozenEnum` de 15 rangos de fecha, default `today` | 64 | `U/BDateRangeEnum.java:12-13` |
| `RangeCalculator` | `BDateRangeEnum` → `BAbsTime[2]` del período actual | 300 | `U/RangeCalculator.java:8-59` |
| `CompareRangeCalculator` | ídem, pero el período de comparación (anterior) | 316 | `U/CompareRangeCalculator.java:8-59` |
| `CommandHelpers` | `ordFromArgument(BValue)` → `BOrd` para los command agents | 23 | `U/CommandHelpers.java:8` |

## 148.2 — Bug de correctitud: `CompareRangeCalculator.last30days` = ventana de 60 días `[CERT]`

`CompareRangeCalculator` devuelve el **período anterior** para los deltas de tendencia (usado por B141). Casi
todos los comparadores rolling desplazan exactamente un ancho de período. **`last30days` es el outlier**
`[CERT]` `U/CompareRangeCalculator.java:161-171`:

```
Calendar cal = Calendar.getInstance();
cal.add(5, -30);  Date end = cal.getTime();       // :164 → end = now-30d
cal.add(5, -60);  Date start = cal.getTime();     // :166 → start = now-30-60 = now-90d
result[0] = start; result[1] = end;               // ventana [now-90, now-30] = 60 días
```

El segundo `add` es acumulativo sobre el `Calendar` ya movido → produce una ventana de **60 días**
`[now-90, now-30]`, el doble del ancho pretendido de 30 días. Contraste con `last7days` `[CERT]`
`U/CompareRangeCalculator.java:196-204` (`add(5,-14)` → `start=now-14`; `add(5,7)` → `end=now-7`; ventana de
**7 días** correcta). `[INFER]` el segundo `add` de `last30days` debería ser `+30` (o el primero `-60`) para dar
`[now-60, now-30]`. **Efecto:** el preset de 30 días del "compare vs período anterior" (B141) muestrea el doble
de datos → números de comparación inflados. Es un bug de correctitud, no de seguridad.

`RangeCalculator` (período actual) no tiene este defecto `[INFER]`; sí depende del TZ default de la JVM
(`Calendar.getInstance()`) `[CERT]` `U/RangeCalculator.java:63` — riesgo de diseño (si el TZ de la station
difiere del de la JVM, las ventanas se corren), no un bug de código.

## 148.3 — `CommandHelpers.ordFromArgument`: el embudo del taint hacia BOrd `[CERT]`

Es el helper único que los command agents (B146) usan para coercionar el argumento del comando a un `BOrd`
`[CERT]` `U/CommandHelpers.java:8-18`:

```
if (arg.getType().is(BOrd.TYPE)) { ord = (BOrd)arg; }
else if (arg.getType().equals(BComponent.TYPE)) {
   if (comps.get("ord") != null) { ord = BOrd.make(comps.get("ord").toString()); }   // :15
} else { ord = BOrd.make(arg.toString()); }                                           // :18
```

`[INFER]` Es **pura extracción de parámetro, sin auth ni validación**: convierte el argumento provisto por el
caller directo en un `BOrd` vía `BOrd.make(arg.toString())`/`BOrd.make(comp.get("ord")...)`. No resuelve ni
dereferencia el ORD acá (el traversal/autorización vive aguas abajo donde se resuelve), pero es **el embudo**
que lleva el argumento no confiable → ORD. Corrobora el taint source de B147: la sanitización/permiso NO está
en `util/`.

## 148.4 — Negativos que corroboran B147 `[CERT]`

- **`StringUtils` NO sanitiza:** su único método es `countOccurrences` `[CERT]` `U/StringUtils.java:4` — un
  contador de chars. No puede ser el sanitizador del path de taint de B147.
- **`Json` NO concatena inseguro:** construye vía la API `.put(key,value)` de `com.tridium.json.JSONObject`
  `[CERT]` `U/Json.java:3-4,15-17` → comillas/control chars escapados por la librería, sin riesgo de
  JSON-injection por valores. Sólo expone metadata de componente (name/ord/slotPath/type/icon), no secretos.
- **`NavNodeSerializer` usa Jackson `writeStringField`** `[CERT]` `U/NavNodeSerializer.java:34` → escaping
  correcto, sólo metadata de nav. `[INFER]` (el loop de validación de tipos traga excepciones silenciosamente,
  benigno).

`[INFER]` Estos negativos confirman: **no existe escaping/sanitización en `util/`** que pudiera interceptar el
ORD/param contaminado — consistente con la conclusión end-to-end de B147.

## 148.5 — Otros (quirks no-seguridad) `[CERT]`

- **`PointHelper` — código muerto:** `getPointsForComponent` pasa `deep=false` y la recursión interna también,
  así que la rama "deep" nunca se alcanza `[CERT]` `U/PointHelper.java:32,44` `[INFER]` — sólo junta puntos de
  hijos directos.
- **`BDateRangeEnum`** — 15 rangos (`lastHour`..`last12Months`), default `today` `[CERT]`
  `U/BDateRangeEnum.java:12-13`.

## 148.6 — Connections

- **[Block 141]** — History usa `RangeCalculator`/`CompareRangeCalculator` (el bug de `last30days` afecta su
  comparación de 30 días) y `PointHelper`.
- **[Block 146]** — los command agents coercionan su argumento vía `CommandHelpers.ordFromArgument` (el embudo
  del BQL/ORD arbitrario que corre a read-level).
- **[Block 147]** — los negativos de `StringUtils`/`Json`/`NavNodeSerializer` confirman que la capa util no
  aporta ninguna sanitización que mitigue el taint; la cadena fuente→sink sigue sin defensa.

`[INFER]` R11 es un bloque de cierre de superficie: la capa util es en su mayoría plumbing correcto, con un
único bug de correctitud (`last30days`, ventana doble) y un helper que confirma —no agrega— el taint de B147.
No mueve la nota de seguridad cross-focus, ya cerrada end-to-end en B147 §147.4. Queda R12 (contrato de datos)
como último gap investigable antes del bloque de síntesis cross-focus terminal.
