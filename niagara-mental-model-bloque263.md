# Bloque 263 — Tags (IV): el álgebra de condiciones y qué es realmente "neqlize"

> **Qué documenta**: cómo se expresa la condición de una regla de tags (`condition/`, 9 clases) y qué hace el
> paquete `neqlize/` (6 clases). Cierra el gap **T4** del focus `tags`.
>
> **Corrige una premisa MÍA**: al sembrar el backlog escribí *"T4 condiciones + neqlize (**tag→query**)"*,
> asumiendo que "neqlize" traducía condiciones a NEQL. **Es falso.** Ver §263.4 — el error era del que armó el
> backlog, no del código.
>
> **Alcance**: `condition/` + `neqlize/`. Fuera: `tag/`, `util/`, `relation/`, `scope/`, raíz.
>
> **Fuentes** (decompilado vineflower):
> - `$C` = `…/tagdictionary/tagdictionary-rt/vineflower/com/tridium/tagdictionary/`
> - Raíz: `/home/cristian/modules/Prototipos/modulos/organized/`
>
> **Método**: barrido delegado (tier `sonnet`) + verificación inline: **8 tokens** re-verificados, y **1
> afirmación del barrido acotada** (§263.7) que, tal como venía, habría quedado como una falsa alarma de
> seguridad. Marcadores: `[CERT]` = fuente primaria; `[INFER]` = deducción. Bloque de EVIDENCIA.

---

## 263.1 — El álgebra: 3 operadores y 4 hojas `[CERT]`

| Clase | Rol |
|---|---|
| `BAnd` · `BOr` · `BNot` | operadores lógicos (todos `BTagRuleCondition` + `Iterable<Predicate<Entity>>`) |
| `BAlways` · `BNever` | constantes true / false (`BAlways` es el default) |
| **`BBooleanFilter`** | **hoja**: guarda un predicado NEQL como string y lo evalúa con `NeqlEntityEvaluator.makePredicate` |
| **`BHasAncestor`** | **hoja** (extiende `BBooleanFilter`): recorre la cadena de ancestros aplicando el predicado |
| **`BHasRelation`** | **hoja** (extiende `BBooleanFilter`): sigue una cadena de relaciones por `relationId` |
| **`BIsTypeCondition`** | **hoja**: `entity.getType().is(matchType)` |

`[CERT]` `BNot` acepta **exactamente un hijo**: `checkAdd` llama a `handleMultipleChildren` si ya hay uno
(`$C/condition/BNot.java:107`).

`[INFER]` Lo interesante es que **tres de las cuatro hojas terminan en NEQL**: `BBooleanFilter` y sus dos
subclases evalúan un predicado NEQL. La condición de una regla de tags no es un DSL propio — es NEQL embebido
en un árbol booleano de componentes.

## 263.2 — El export es MÁS restrictivo que el runtime `[CERT]`

```java
} else if (((BAnd[])CompUtil.getDescendants(condition, BAnd.class)).length > 0) {
    results = lex.getText("export.no.nestedAnd");
} else if (((BOr[])CompUtil.getDescendants(condition, BOr.class)).length > 1) {
    results = lex.getText("export.limit.nestedOr");
} else if (((BNever[])CompUtil.getDescendants(condition, BNever.class)).length > 0) {
```
`$C/util/ExportUtil.java:480-484` `[CERT]`

En **runtime** los operadores anidan sin límite (los `BAnd`/`BOr` simplemente iteran sus hijos; no hay guarda
de profundidad). Al **exportar a CSV**: cualquier `BAnd` descendiente → rechazado; más de un `BOr` en total →
rechazado; cualquier `BNever` anidado → rechazado.

`[INFER]` Consecuencia concreta: **se pueden construir condiciones válidas y funcionales que no se pueden
exportar**. Un diccionario armado a mano en Workbench, con una condición razonablemente compleja, puede
resultar no-portable a otra station por CSV. El formato de intercambio es más pobre que el modelo en memoria.

## 263.3 — `testIdealMatch(Type)`: el pre-filtro de diseño `[CERT]`

Toda condición tiene dos evaluaciones: `test(Entity)` sobre la instancia viva, y `testIdealMatch(Type)` que
**opera sobre el TIPO, sin instancia**.

Contratos verificados: `BIsTypeCondition` compara tipos (`:78`); `BAlways` → true; `BNever` → false; los
operadores recursan; y **`BBooleanFilter` devuelve `true` siempre** (`$C/condition/BBooleanFilter.java:69`)
`[CERT]` — porque un predicado NEQL no se puede evaluar sin entidad.

Quién lo llama: `BTagInfo.isIdealFor(Type)` (`$C/../javax/baja/tagdictionary/BTagInfo.java:96-98`) y el
equivalente en `BTagGroupInfo`.

`[INFER]` Sirve para que los editores de Workbench decidan **qué tags ofrecer en la paleta para un tipo de
componente, antes de tener una instancia**. Pero como `BBooleanFilter` siempre da `true`, el pre-filtro
**queda efectivamente desactivado para toda condición basada en NEQL** — que son las tres hojas más usadas
(§263.1). El atajo solo funciona con `BIsTypeCondition` y `BNever`.

## 263.4 — "Neqlize" NO traduce condiciones a NEQL `[CERT]`

Éste es el hallazgo del gap, y desmiente la premisa con la que lo sembré.

```java
   public static JSONObject getIdentifyingTagsRelations(String basePath, List<String> targetPaths, String optionsString, Context context) {
```
`$C/neqlize/BNeqlizeRpc.java:98` `[CERT]` — el entry point, expuesto como **RPC de Fox**.

**Lo que hace realmente**: dado un componente base y uno o más objetivos, encuentra el **conjunto MÍNIMO de
tags que identifica unívocamente a cada objetivo** dentro de un espacio de búsqueda. Es
**identificación inversa**: del componente al predicado, no de la condición a la query.

El algoritmo (`TagSetSearch` + `TagsCandidateIterator`): recorre los subconjuntos del conjunto de tags del
objetivo **en orden de tamaño creciente** (primero los singletons, después los pares…) y devuelve el primero
que **ningún otro** componente del espacio de búsqueda posea también.

El espacio lo fija `BNeqlizeMode` (`BFrozenEnum` de 3 valores) `[CERT]`:
`traverseIfPossible(0)` = endpoints de relaciones + descendientes · `traverseOnly(1)` = solo endpoints ·
`selectOnly(2)` = solo descendientes.

Salida: un `JSONObject` por ruta objetivo, con `{"tags": …, "relationId": …, "relationIsInbound": …}` en caso
de éxito, o `{"error": "noTagSetFound" | "notEndpointOrDescendant" | …}`.

`[INFER]` **Para qué existe**: es el motor detrás del "armame la query" de la UI. Un ingeniero señala un punto
en el árbol y Niagara le devuelve el `hasTags(...)` mínimo que lo selecciona — para pegarlo en un binding, una
regla o una consulta. Sin esto, habría que elegir los tags a mano y verificar la unicidad por prueba y error.

**Mi premisa del backlog estaba invertida**: no va de la condición a la query, va **del componente al conjunto
de tags**. Se registra el error para que el corpus no herede una definición equivocada.

## 263.5 — Qué excluye `neqlizeExcludedTags` y por qué `[CERT]`

`BNiagaraTagDictionary` trae los defaults cableados (`$C/BNiagaraTagDictionary.java:88,91`) `[CERT]`:

- **Tags excluidos**: `n:bindHints, n:displayName, n:geo*, n:hasPxView, n:history, n:alarmablePoint, n:name,
  n:node, …`
- **Relaciones excluidas**: `n:child, n:parent, n:tagGroup`

Se leen desde un `TypeSubscriber` que vigila **todas** las instancias de `BTagDictionary` y funde las
exclusiones de cada diccionario en las opciones del servicio
(`$C/../javax/baja/tagdictionary/BTagDictionaryService.java:1130-1152`), con override por llamada desde el
JSON de la RPC. `FilteredTagsMap.getFilteredTags()` las aplica antes de la búsqueda.

`[INFER]` La lógica es clara al ver **qué** se excluye: `n:name` y `n:displayName` identificarían el
componente por sí solos, y eso arruinaría el propósito — se busca un identificador **semántico**
(«el punto de temperatura de descarga de este AHU»), no uno nominal («el que se llama TempDesc»). Igual con
`n:parent`/`n:child`: son estructura, no significado. Si tras excluirlos no queda combinación única, la
respuesta es `noTagSetFound`.

## 263.6 — `BHasAncestor` no tiene guarda de ciclos; `BHasRelation` sí `[CERT]`

```java
         for (BComponent obj = (BComponent)entity; obj != null; obj = getParent(obj)) {
```
`$C/condition/BHasAncestor.java:34` `[CERT]` — un `for` que sube por `getParent()`, **sin conjunto de
visitados**. Y `getParent()` no es solo contención: cae también en la relación `n:parent`
(`$C/condition/BHasAncestor.java:44-45`).

Su hermana sí se protege:
```java
         HashSet<Entity> visited = new HashSet<>();
```
`$C/condition/BHasRelation.java:66`, con el chequeo `visited.contains(obj)` en `:77` `[CERT]`.

`[INFER]` La asimetría es el hallazgo: como `n:parent` es una relación **que un usuario puede crear a mano**
([Bloque 262] §262.1), es posible construir un ciclo de `n:parent` en la station. `BHasRelation` lo
sobreviviría; **`BHasAncestor` entraría en bucle hasta agotar la pila**. No se pudo reproducir —queda como
riesgo estructural verificado en el código, no como incidente observado— pero la asimetría entre dos clases
hermanas es evidencia suficiente de que una de las dos olvidó la guarda.

## 263.7 — Precisión sobre permisos: la RPC es `unrestricted`, pero filtra `[CERT]`

El barrido reportó que la RPC de neqlize tiene *"permissions: unrestricted"*, a secas. Es cierto pero
incompleto, y así solo habría sido una falsa alarma:

`[CERT]` `permissions = "unrestricted"` aparece en `$C/neqlize/BNeqlizeRpc.java:72,93`. **Pero** el propio
archivo filtra por permiso al resolver cada objetivo:

```java
         return target != null && target.getPermissions(context).hasOperatorRead() ? target : null;
```
`$C/neqlize/BNeqlizeRpc.java:364` `[CERT]`

`[INFER]` El diseño es defensa en profundidad correcta: **la RPC es invocable sin permiso especial, pero solo
devuelve componentes sobre los que el usuario invocante tiene lectura de operador**. Un usuario limitado
obtiene resultados limitados, no un error. Contrasta con la asimetría import/export de [Bloque 261] §261.7-2,
donde el export **descarta** el contexto: acá el contexto se propaga y se usa.

## 263.8 — Otros gotchas `[CERT]`

- **Validación NEQL que no valida** `[CERT]`: `isNeqlPredicateValid()` comprueba con `BOrd.parse()` que el
  esquema sea `neql` (`$C/util/ImportUtil.java:876-880`), **sin verificar que el cuerpo del predicado sea
  parseable**. `[INFER]` Aceptaría `neql:garbage`. Sumado a que el llamador solo loguea `SEVERE` y continúa
  ([Bloque 261] §261.4), un predicado basura entra al diccionario en silencio.
- **`BBooleanFilter.predicate` no es `volatile`** y `changed()` lo pone en `null`
  (`$C/condition/BBooleanFilter.java:98`) mientras otro hilo puede estar dentro de `getFilterPredicate()`.
- **Costo exponencial sin techo**: `TagsCandidateIterator` recorre hasta 2^N − 1 subconjuntos, con N = tags no
  excluidos del objetivo. No hay cap. `[INFER]` Mitigado en la práctica porque los subconjuntos chicos se
  prueban primero y casi siempre alcanzan, pero un objetivo con muchos tags y sin combinación única paga el
  peor caso completo.
- **Sin caché entre llamadas**: `FilteredTagsMap` memoiza dentro de **una** llamada y se descarta.
- **Sin ofuscación ZKM** en las 15 clases (ámbito: `condition/` + `neqlize/`).

## 263.9 — Conexiones

- **[Bloque 21]** §21.6 (NEQL) — este bloque muestra dónde NEQL se usa *dentro* del framework de tags: como
  cuerpo de las hojas de condición (§263.1) y como objetivo de la identificación inversa (§263.4).
- **[Bloque 261]** §261.4 — la validación NEQL que loguea y no aborta: acá se ve por qué es peor de lo que
  parecía (§263.8).
- **[Bloque 262]** §262.1 — que `n:parent` sea una relación creable a mano es lo que vuelve real el riesgo de
  ciclo de §263.6.
- **[Bloque 260]** §260.1 — el `TypeSubscriber` que funde exclusiones de todos los diccionarios refuerza que
  el descubrimiento es por montaje, no por registro.
- **Gaps abiertos**: T5 (haystack), T6-T8 (exportTags + UI), T9 (doc oficial, ya preservada), T10 (Brick).
