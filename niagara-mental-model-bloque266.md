# Bloque 266 — Tags (VII): "Export Tags" NO es parte del subsistema de tags — y §14 a B21

> **Qué documenta**: el runtime de `exportTags` (28 clases) — qué es realmente, cómo funciona el *join*
> supervisor↔subordinada, y por qué **no pertenece al subsistema de tagging**. Cierra el gap **T6** del focus
> `tags`.
>
> **Contiene una corrección §14 a [Bloque 21] §21.4** y, de paso, corrige la premisa con la que yo mismo
> sembré los gaps T6 y T7. Ver §266.1.
>
> **Alcance**: `exportTags-rt`. La UI (`exportTags-wb`) es el gap T7.
>
> **Fuentes** (decompilado vineflower):
> - `$E` = `…/exportTags/exportTags-rt/vineflower/com/tridium/exporttags/`
> - Raíz: `/home/cristian/modules/Prototipos/modulos/organized/`
>
> **Método**: barrido delegado (tier `sonnet`) + verificación inline: **7 tokens** re-verificados y **1
> afirmación del barrido corregida** (§266.6) — la tercera consecutiva sobre permisos, ver la nota de patrón
> allí. Marcadores: `[CERT]` = fuente primaria; `[INFER]` = deducción. Bloque de EVIDENCIA.

---

## 266.1 — §14 a [Bloque 21] §21.4: el nombre engaña `[CERT]`

**Ausencia probada, y es el hallazgo del gap**: barrido sobre las **28 clases** de `exportTags-rt` buscando
`com.tridium.tagdictionary|javax.baja.tagdictionary|javax.baja.tag.` → **0 archivos** `[CERT]`.

`exportTags` **no importa absolutamente nada del subsistema de tags**. No usa `BTagDictionary`, ni `Id`, ni
`Tag`, ni `Relation`, ni el `ImportUtil`/`ExportUtil` del diccionario ([Bloque 261] §261.4).

**Qué es en realidad** `[INFER]`: un mecanismo de **join entre estaciones** de Niagara Network. La estación
*subordinada* aloja componentes `BNiagaraExportTag` que declaran **qué ofrece**; al disparar un join, el
*supervisor* se conecta por Fox, descarga el BOG de la subordinada y **provisiona proxies en su propio árbol**
(puntos, imports de history, imports de archivo, schedules).

`[INFER]` La palabra "tag" acá significa **etiqueta declarativa de exportación**, no tag semántico de
diccionario. Son dos subsistemas con nombres parecidos y cero código en común.

**La corrección**: [Bloque 21] se titula *"Tag Framework + Haystack 4 + BQL + NEQL"* e incluye una sección
**§21.4 "Export tags — workflow entre stations"**. Meter ese workflow dentro del bloque del framework de tags
**induce a pensar que son el mismo subsistema**. No lo son. Yo mismo heredé el error al sembrar este focus:
puse T6 y T7 en el backlog de `tags` dando por hecho que `exportTags` era la capa de intercambio del
diccionario. La evidencia dice que no.

`[INFER]` Lo correcto sería tratar `exportTags` como parte del hilo **Niagara Network / supervisor**, no del
de tagging. Se documenta acá porque el gap ya estaba abierto y el trabajo está hecho, pero **queda anotado
para que el corpus no perpetúe la confusión**.

## 266.2 — El mecanismo: Fox + descarga de BOG + provisión de proxies `[CERT]`

```java
         BFoxSession session = (BFoxSession)BFoxProxySession.make(
```
`$E/BSupervisorJoinJob.java:142` `[CERT]`

```java
                     ValueDocDecoder decoder = new ValueDocDecoder(stationFile);
```
`$E/BSupervisorJoinJob.java:203` `[CERT]`

El flujo verificado, de punta a punta:

1. **Disparo**: por el supervisor (`BJoinAction`, un MixIn sobre `BNiagaraStation` — el "Join" del clic
   derecho) o por la subordinada (`BJoinProfile.doJoin()`).
2. **Sesión Fox** del supervisor hacia la subordinada (`:142`).
3. **Metadatos**: llamada remota a `getJoinInfo`, que devuelve un `BJoinInfo` con el ORD del archivo de
   estación, el ORD de la estación supervisora y la ruta de carpeta.
4. **Descarga del BOG**: el archivo de estación de la subordinada se baja y se decodifica con
   `ValueDocDecoder` (`:203`). **El payload es un BOG binario** — el grafo de objetos de la station —, no
   JSON ni CSV.
5. **Merge** del árbol decodificado dentro del `BNiagaraNetwork` del supervisor.
6. **Consulta BQL** sobre la subordinada para listar sus export tags habilitados, filtrando por nombre de
   estación supervisora y `status.isOk`.
7. **Por cada tag**: `preJoin → doJoin → postJoin`, creando el proxy correspondiente y registrando el slot en
   un `targetSlotInfoList`.
8. **Limpieza** de targets caducos, aplicación de la máscara de categorías, y **evento de auditoría**.

`[INFER]` Es, en esencia, **replicación de configuración por Fox con provisión automática**: la subordinada
declara y el supervisor materializa. Explica por qué el módulo vive en el mundo de `niagaraDriver` y no en el
de tagging.

## 266.3 — Los 7 tipos de "export tag" `[CERT]`

Ninguno es un tag de diccionario: todos son `BComponent` montados en el árbol de la subordinada que declaran
qué provisionar en el supervisor.

| Clase | Qué crea en el supervisor |
|---|---|
| `BPointTag` | un `BControlPoint` tipado + `BNiagaraProxyExt` apuntando al `slotPathOrd` de la subordinada |
| `BHistoryImportTag` | uno o varios `BNiagaraHistoryImport` |
| `BSystemHistoryImportTag` | ídem, con `systemTagPatterns` |
| `BFileImportTag` | un `BNiagaraFileImport`, resolviendo variables `$(stationName)`/`$(currentLocation)` |
| `BComponentTag` | copia profunda de las propiedades escribibles del componente padre |
| `BScheduleImportTag` | un schedule + `BNiagaraScheduleImportExt` |
| `BScheduleExportTag` | un `BNiagaraScheduleExport` del lado de la subordinada |

`[INFER]` El `supervisorStation` (`BNameList`) de cada tag permite que **una misma subordinada sirva a varios
supervisores con conjuntos distintos**: la consulta BQL del paso 6 filtra por nombre.

## 266.4 — Las categorías: control de acceso post-join `[CERT]`

El paquete `category/` (3 clases) aplica el sistema estándar de **categorías** de Niagara a lo que se
provisiona. `BCategoryFilter` compara patrones glob contra las propiedades del MixIn `BStationInformation` de
la estación unida; `BCategoryFilterExt` evalúa sus hijos **en orden de slot y gana el primero**; el
`BCategoryMask` resultante se aplica a la estación y se propaga a los ORDs de histories y archivos creados.

`[INFER]` Un filtro vacío es comodín. Y como el orden de evaluación depende de la **posición en el árbol de
componentes**, reordenar hijos cambia qué categoría se aplica — un efecto lateral que no es obvio desde la UI.

## 266.5 — Seguridad: lo que sí está `[CERT]`

- **Auditoría siempre**: el evento se escribe en el `finally`, con éxito o fracaso, incluyendo el usuario
  (`$E/BSupervisorJoinJob.java:469-487`).
- **Alarma ante fallo**: `BSupervisorExportTagNetworkExt` es `BIAlarmSource` y emite un `BAlarmRecord` con
  host, puerto, usuario y causa.
- **Autenticación**: la propia sesión Fox exige credenciales. La subordinada además valida que la estación
  supervisora exista en su `NiagaraNetwork` y tenga un `BJoinProfile`.
- **Credenciales**: `BConnectInfo` transporta `BClientCredentials` como **parámetro de acción** sobre el canal
  Fox. `[INFER]` No se escribe a archivo ni se loguea, pero **viaja serializado como dato de componente** —
  el canal Fox es lo único que lo protege. Con `useFoxs` en falso, viaja por un canal sin TLS.

## 266.6 — Corrección al barrido: `flags = 4` es OCULTO, no un permiso `[CERT]`

El barrido reportó que la acción `joinStation` tiene *"`flags = 4`. En el sistema de permisos de acciones de
Niagara esto es nivel operador — cualquier operador autenticado puede invocarla"*.

**Es incorrecto.** `[CERT]` En Niagara, `flags = 4` es **`Flags.HIDDEN`** — un flag de **visibilidad de slot**,
no de permiso. El propio corpus ya lo documentó así: [Bloque 260] §260.7-d registra `maxImportFileSize` con
"flags=4 (oculto)", y [Bloque 261] usa el mismo criterio.

`[INFER]` Lo que `flags = 4` significa realmente: la acción **no se muestra en la UI por defecto**. El control
de acceso de una acción en Niagara lo da el **permiso de invocación sobre el componente**, no un número en la
declaración del slot. Así que ni "cualquier operador puede", ni lo contrario — el flag simplemente **no habla
de permisos**.

**Nota de patrón**: es la **tercera corrección consecutiva sobre permisos** en este focus — [Bloque 261]
§261.7 (dos), [Bloque 263] §263.7 (una), y ésta. Las tres eran del mismo tipo: el barrido leyó un mecanismo de
Niagara (un `return` temprano, un `permissions="unrestricted"`, un `flags=4`) y le atribuyó una semántica de
seguridad que no tiene. `[INFER]` Conclusión de método para lo que queda del focus y para el retro: **todo
claim de permisos que venga de un sub-agente se verifica contra la semántica real del framework antes de
escribirse** — el costo de un falso positivo de seguridad en un corpus de referencia es alto.

## 266.7 — Otros gotchas `[CERT]`

- **Serialización estricta**: `BExportTagWorker` es un `BWorker` de **un solo hilo** con cola de 1000 por
  defecto — todos los joins del supervisor se serializan; si la cola se llena, `NotRunningException`.
- **Polling sin timeout**: `BSubordinateJoinJob` sondea el estado del job del supervisor cada segundo **sin
  límite superior** — si el supervisor cuelga, el hilo de la subordinada espera indefinidamente.
- **Tres excepciones tragadas** en `BSupervisorJoinJob`: al remover targets caducos (`:380-392`, `catch`
  vacío), en `remoteHello` (`:193-199`, que silenciosamente deja `swapForEdgeLite` en falso y puede decodificar
  el BOG como el tipo equivocado) y en `matchOrder()` (`:618-620`).
- **Creación automática de BOG**: `BJoinProfile` crea un BOG vacío si el configurado no existe, como efecto
  lateral del arranque.
- **Sin ofuscación ZKM** en las 28 clases.

## 266.8 — Conexiones

- **[Bloque 21]** §21.4 — **corregido**: el workflow de export tags no pertenece al framework de tags
  (§266.1).
- **[Bloque 5]** (ORD/BOG) — el payload del join es un BOG binario decodificado con `ValueDocDecoder`.
- **[Bloque 260]** §260.7-d y **[Bloque 261]** — el criterio de `flags` que permite corregir al barrido
  (§266.6).
- **Hilo Niagara Network / supervisor** — es donde este módulo pertenece conceptualmente; queda como puntero
  para un focus futuro sobre `niagaraDriver`.
- **Gaps abiertos**: T7/T8 (las UI, en curso), T9 (doc oficial preservada).
