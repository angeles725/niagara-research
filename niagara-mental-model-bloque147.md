# Block 147 — nmodsreflow.77 (`-rt`): el taint source HTTP (`Query.method_363` URL-decode sin sanitizar; `QueryFilter` no cubre los params peligrosos)

> Research de **NiagaraMods Reflow v1.7.7 (build .75), el parser de query HTTP del runtime `-rt`**: cómo
> Reflow convierte el query string crudo en el `Map` que alimenta a TODOS los endpoints, y si algo aguas
> arriba mitiga el traversal/injection que B142/B144/B145/B146 encontraron aguas abajo. Cubre
> `http/util/Query` (`method_363`/`mapComplex`) y `alarms/QueryFilter` (`make`). Cierra el sub-gap **R13** y
> **cierra el hilo de seguridad del focus de punta a punta** (fuente→sink).
>
> Focus: **nmodsreflow** (arquitectura backend `-rt`). Cierra el gap **R13**. Corpus language: Spanish
> (technical EN).
>
> Sources (primarias, JAR embarcado build .75, decompile Vineflower):
> `RT/` = `/home/cristian/modules/Prototipos/modulos/organized/nmodsreflow77/nmodsreflow77-rt/vineflower/com/niagaramods/nmodsreflow`
> `Q` = `RT/http/util/Query.java` · `QF` = `RT/alarms/QueryFilter.java`.
>
> Método: lectura directa completa de ambas clases (203 líneas) + grep de callers/negativos. Markers:
> `[CERT]` fuente primaria local (`file:line`) · `[INFER]` deducción anclada a líneas `[CERT]`.
> Nota de decompilado: `method_363` es el nombre ofuscado por Vineflower del parser `map`; se cita tal cual.
>
> Capa 26 (OEM tercero NiagaraMods). Connects [Block 142] (BQL injection `uuid`), [Block 144] (traversal de
> escritura `file`), [Block 145] (`?file=` read traversal + overwrite), [Block 146] (BQL command).

---

## 147.1 — `Query.method_363`: URL-decode puro, cero sanitización `[CERT]`

El parser que alimenta TODOS los endpoints REST es minúsculo `[CERT]` `Q:12-24`:

```
String[] pairs = queryString.split("&");                 // :15
for (String pair : pairs) {
   int idx = pair.indexOf("=");                          // :18
   query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"),
                   URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));   // :19
}
```

Hace exactamente tres cosas: split por `&`, split por el primer `=`, y **`URLDecoder.decode(..., "UTF-8")`**
de clave y valor `[CERT]` `Q:4,15,18-19`. **No sanitiza, no escapa, no valida nada.**

`[INFER]` **El URL-decode no sólo preserva el taint: lo AGRAVA.** Es un transform que CONVIERTE secuencias
percent-encoded en los chars peligrosos que los sinks temen: `%2F`→`/`, `%2E%2E`→`..`, `%27`→`'`. Es decir,
un atacante que necesita `/`/`..` para el traversal de B144/B145 o `'` para la BQL injection de `uuid` (B142)
puede **percent-encodearlos** y `method_363` se los **decodifica de vuelta** al char crudo antes de entregarlos
al sink. Cualquier check naive de string aguas abajo (que hubiera buscado `/` literal) queda burlado.

**Bug de guard asimétrico** `[CERT]` `Q:18-19` vs `Q:32-33,38`: `method_363` NO verifica `idx > 0` antes de
`substring(0, idx)`; su gemela `mapComplex` SÍ lo hace (`idx > 0 ? ... : pair`). `[INFER]` → un param sin `=`
(p.ej. `?foo`) hace `indexOf`→-1 y `substring(0,-1)` lanza `StringIndexOutOfBoundsException`, tumbando el
parseo. Misma clase de asimetría (una ruta protegida, la gemela no) que la sanitización de B144.

## 147.2 — `QueryFilter.make`: filtro TIPADO, sólo para campos de alarma `[CERT]`

`QueryFilter` (paquete `alarms`) construye un filtro tipado a partir del `Map` `[CERT]` `QF:26-112`. Los
campos que maneja son **exclusivamente de la query de alarmas** `[CERT]` `QF:16-24`: `timeRange`
(`BDynamicTimeRange.decodeFromString` `:30`), `ackState` (match `"unack"` `:39`), `active` (match `"true"`
`:45`), `byClasses`/`bySources` (split `,` `:52,63`), `byThresholdLow`/`High` (parse `Double`→`int` `:75,86`),
`page` (parse numérico `:97`), `countOnly` (match `"true"` `:106`).

`[INFER]` Para esos campos hay **coerción de tipo** (thresholds/page: no-numérico → `null`/default; active/
ackState/countOnly: match contra literales) — una validación de entrada leve. Pero los campos string
(`byClasses`/`bySources`) se guardan opacos; B142 ya mostró que `AlarmData` filtra esos en Java post-cursor,
no vía BQL, así que ahí no hay injection.

## 147.3 — El punto que cierra el hilo: los params peligrosos NO pasan por QueryFilter `[CERT]`

Grep confirma que `QueryFilter.make` **no referencia** `file`, `query` ni `uuid` `[CERT]` (grep negativo sobre
`QF`). Esos —los tres params que alimentan el traversal (B144/B145 `file`), el BQL command (B146 `query`) y la
BQL injection (B142 `uuid`)— **bypassean por completo el filtro tipado** y van directo del `Map` decodificado
de `method_363` al sink `[CERT]`:

- `BackupDestroyResponse.java:13` → `query.get("file")` → `BackupManager.destroy` (delete traversal).
- `ConfigResponse.java:27` → `query.get("file")` → `findFile(new FilePath(location))` (read traversal).
- `BackupApplyResponse.java:18` → `query.get("file")` → `BackupManager.apply` (overwrite traversal).

`[INFER]` **Conclusión end-to-end:** entre la fuente (query string crudo) y el sink (FilePath/BOrd/BQL) el
ÚNICO transform es `URLDecoder.decode` — que ayuda al atacante, no lo frena. `QueryFilter` existe pero es una
capa lateral para la query de alarmas que los params peligrosos ni tocan. **Nada aguas arriba mitiga el
traversal/injection**: los hallazgos de B142/B144/B145/B146 son explotables de punta a punta, no teóricos.

## 147.4 — Connections y cierre del hilo de seguridad

- **[Block 142]** — la BQL injection `uuid` recibe el valor URL-decodeado sin filtro; `%27`→`'` habilita la
  ruptura de la cláusula BQL.
- **[Block 144]** — el `file` del traversal de escritura (destroy/apply/rename) sale de este parser; `%2F`/`%2E`
  reconstruyen `/`/`.` para el `..`.
- **[Block 145]** — `?file=` (read traversal) y el overwrite consumen el mismo `Map`.
- **[Block 146]** — el `query` del BQL command también viene de acá.

**Nota de seguridad cross-focus (CERRADA end-to-end — R13 completa la cadena fuente→sink):** R13 es la pieza
que faltaba para afirmar explotabilidad, no sólo presencia. El parser HTTP de Reflow URL-decodifica el input
crudo sin sanitizar (agravando el taint), y el único filtro tipado (`QueryFilter`) es lateral y no cubre los
params peligrosos. Con esto la superficie agregada del focus queda **completamente caracterizada y confirmada
explotable**: (1) config/backups mutables sin auth por REST (bypass del gate `"r"` de B146, que además cabalga
el `@AgentOn` y no el dato); (2) traversal de lectura y escritura de `.json` arbitrario + wipe de config
(B144/B145), con el char peligroso reconstruible por percent-encoding (B147); (3) BQL arbitrario read-level con
Context nulo (B146) y BQL injection `uuid` (B142); (4) `doPrivileged` anchos sobre input del cliente
(B141/B142/B143); (5) audit trail forjable (headers `Client-*`, B145); (6) todo sobre una plataforma donde la
validación de módulo puede apagarse (`skipModuleValidation`, B75/B113) y el licensing RSA tiene bypass (B139).
`[INFER]` El hilo de seguridad está cerrado; el **NEXT-ACTION** natural al agotar el focus es un bloque de
**síntesis cross-focus** (nmodsreflow × platform-security) que consolide esta cadena. Quedan investigables sólo
R11 (util) y R12 (contrato de datos), de baja prioridad, más R3 (casi-cerrado).
