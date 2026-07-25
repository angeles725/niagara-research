# Bloque 270 — Síntesis de cierre: el subsistema de TAGS de Niagara N4 (focus `tags`)

> **Qué documenta**: el cierre del focus `tags` — **10/10 gaps, bloques B260-B269**. Consolida en cinco hilos
> lo que los diez bloques establecieron por separado, y deja el estado para quien retome.
>
> **Alcance**: `tagdictionary` (78 clases), `haystack` (37), `brick` (2), `exportTags` (44 — que resultó NO
> pertenecer al subsistema, §270.5), más 363 KB de documentación oficial de Tridium.
>
> **TIPO: SÍNTESIS** (§11) — ratio alto esperado y sano; el agotamiento ya se declaró por investigable = 0.
> Las citas primarias viven en los bloques de origen y no se repiten.

---

## 270.1 — Hilo 1: un motor genérico que aloja ontologías como contenido `[INFER]`

El hallazgo estructural del focus. Niagara N4.14 soporta **tres ontologías**, y el costo en código es
desproporcionadamente distinto:

| Ontología | Clases | Contenido |
|---|---|---|
| Niagara (`n`) | ~24 | `Niagara.bog` embebido ([Bloque 261] §261.1) |
| Haystack (`hs` + `h4`) | 37, **35 de ellas tags computados** | `tags.csv` 233 líneas + `defs.json` 497 KB ([Bloque 264] §264.3) |
| **Brick** | **2** | JSON generado, con reglas custom del usuario ([Bloque 265] §265.1) |

`[INFER]` El motor es `BSmartTagDictionary` y **las tres son instancias suyas**. Una ontología entera de
terceros entra con dos clases. Y lo que un diccionario necesita y el framework no da, lo agrega con una clase
chica: Brick necesitaba identificadores estándar y relaciones inversas, y resolvió cada una con una
([Bloque 265] §265.2-3).

**El punto de extensión que se usa en la práctica** es subclasificar `BTagInfo`/`BRelationInfo` y **montarlo**
en un diccionario — no el registro de agentes ([Bloque 260] §260.1). Coincide exactamente con lo que
[Bloque 254] §254.3 observó en el charting: los mecanismos declarativos existen, la extensión real pasa por
subclasificar y montar.

## 270.2 — Hilo 2: casi nada está almacenado `[INFER]`

Tres capas del subsistema resultaron ser **computadas en cada consulta**, no persistidas:

- **Los tags de station**: `n:point`, `n:history`, `n:input`/`n:output`, `n:hasPxView` se derivan mirando el
  componente ([Bloque 261] §261.2). Igual los 35 de Haystack ([Bloque 264] §264.4).
- **Las relaciones implícitas**: `BasicRelation` es `final` e inmutable, nunca se persiste ([Bloque 262]
  §262.1).
- **Los índices que hacen esto viable**: heap puro, construidos perezosamente al ejecutar NEQL y **destruidos
  en cada reboot** ([Bloque 269] §269.2).

`[INFER]` La contrapartida está documentada por Tridium: sin índice, **cada consulta NEQL sobre un tag
implícito es un barrido completo de la station**. El subsistema cambia almacenamiento por cómputo, y paga ese
cambio con índices que hay que reconstruir después de cada arranque.

Lo que **sí** se almacena: los tags directos, y las relaciones `BRelation` como slot de propiedad — en **un
solo extremo**, con un `RelationKnob` de vuelta en el otro ([Bloque 262] §262.1).

## 270.3 — Hilo 3: el fallo silencioso es un rasgo, no un descuido `[CERT]`

Cinco fallas silenciosas verificadas, en cinco clases distintas:

| Dónde | Qué pasa |
|---|---|
| Integridad referencial ([Bloque 262] §262.6) | borrar el destino de una relación deja la propiedad colgada; resolverla devuelve `null` y loguea a **FINE** (apagado en producción) |
| Validación NEQL al importar ([Bloque 261] §261.4) | predicado roto → `LOGGER.severe` y **continúa**; el tag entra y no matchea nada |
| Migración a tag-groups ([Bloque 268] §268.1) | todo el proceso en un `catch(Exception)` con `printStackTrace()`; fallo a mitad = componentes **parcialmente migrados**, sin rollback |
| Import en hilo de fondo ([Bloque 264] §264.6) | status a `fault` **sin loguear a WARNING** ni avisar |
| `BDataPolicy` ([Bloque 260] §260.7-b) | excepción → `Optional.empty()`, indistinguible de "no hay política" |

`[INFER]` El caso más peligroso en obra es el primero: **borrás un `BTagGroupInfo` y los componentes asignados
pierden sus tags sin un solo error visible**. No hay recolección ni tombstones. Un punto que "dejó de tener
tags" y nada lo reporta.

## 270.4 — Hilo 4: los controles existen, pero con dos llaves públicas `[CERT]`

El subsistema **sí** está licenciado y **sí** controla acceso — a diferencia del charting, donde la ausencia
de gates fue el hallazgo ([Bloque 254] §254.8):

- **Licencia**: feature `tridium/tags` + `Dictionary.limit` = **2 diccionarios por defecto**, ampliable en la
  licencia. Los que excedan quedan en fault y sus tags **desaparecen de la UI** ([Bloque 269] §269.1).
- **Permisos**: las vistas exigen escritura; las RPC son `unrestricted` pero **filtran cada objeto por
  `hasOperatorRead()`** ([Bloque 263] §263.7, [Bloque 268] §268.4).

**Pero el candado `frozen` tiene dos bypasses públicos** `[CERT]`: `BTagDictionary.importContext` — `public
static` y **no `final`** ([Bloque 260] §260.6) — y `Context.decoding`, usado por la utilidad de migración sin
verificar permisos en el sitio de llamada ([Bloque 261] §261.6).

`[INFER]` El patrón que el corpus viene viendo: **controles fuertes en el borde, laxos dentro del proceso**.
`frozen` protege contra el error humano en la UI, no contra código corriendo en la misma JVM.

## 270.5 — Hilo 5: dos correcciones de encuadre que el focus tuvo que hacer `[CERT]`

Este focus corrigió **dos premisas equivocadas, y una era mía**:

1. **`exportTags` NO pertenece al subsistema de tags** ([Bloque 266] §266.1). Ausencia probada: **0 de 28
   clases** importan nada del diccionario. Es un mecanismo de **join supervisor↔subordinada** por Fox con
   descarga de BOG. [Bloque 21] §21.4 lo metió bajo el título "Tag Framework", y **yo heredé el error** al
   sembrar T6/T7 en este focus.
2. **`neqlize` no traduce condiciones a NEQL** ([Bloque 263] §263.4). Yo sembré el gap como "tag→query"; es
   **identificación inversa** — del componente al conjunto mínimo de tags que lo identifica. La doc oficial
   confirmó después el caso de uso: **bindings PX reutilizables**, desde N4.9 ([Bloque 269] §269.3).

`[INFER]` Ambos errores tenían el mismo origen: **asumir el contenido por el nombre**. "Export tags" y
"neqlize" suenan a lo que no son.

## 270.6 — Nota de método: 4 de 4 claims de permisos requirieron corrección `[CERT]`

Registro aparte porque es el aprendizaje más transferible del focus. Los sub-agentes produjeron **cuatro
claims de seguridad, y los cuatro necesitaron corrección o acotación** ([Bloque 268] §268.4 tiene la tabla
completa): un `return` temprano leído como bypass, un `permissions="unrestricted"` reportado sin su filtro, un
`flags=4` leído como nivel de acceso cuando es `Flags.HIDDEN`, y una rama defensiva presentada como agujero
cuando `BComponent` implementa `BIProtected`.

`[INFER]` **No es mala suerte con un agente: es estructural.** Un sub-agente lee el archivo que le tocó sin el
modelo del framework en la cabeza, y le atribuye semántica de seguridad a sintaxis local. La regla adoptada
—verificar todo claim de permisos contra la jerarquía de tipos y la semántica real **antes** de escribirlo— es
lo que evitó cuatro falsos positivos en un corpus que otros van a consultar.

Contrapartida honesta: la disciplina de citas **se aflojó** en la segunda mitad del focus. Los ratios
`[INFER]/[CERT]` de B266 (0.82), B268 (0.73) y B267 (0.62) son altos porque los hallazgos eran **ausencias y
consecuencias** — una ausencia se cita una vez y todo lo demás es deducción. Va al retro §18 como delta
propuesto.

## 270.7 — Estado de cierre

- **Cobertura**: **10 / 10** (ratio 1.00). Bloques **B260-B269** + esta síntesis (B270). El backlog **creció
  durante el focus**: T10 (Brick) salió de leer la doc preservada en la iteración 3.
- **STOP (§8)**: `read-only-investigable = 0` · `requires-execution` = 0 · `blocked` = 0.
- **Fuentes nuevas para el corpus**: **primera vez que se usa la documentación oficial de Tridium** como
  `[CERT-doc]`. Tres guías preservadas y registradas (363 KB); `docExportTags` queda **sin bloque citante** a
  propósito — su contenido es Niagara Network, no tagging.
- **Puntero para un focus futuro**: `exportTags` (44 clases) merece un focus propio bajo el hilo
  **Niagara Network / supervisor**, junto con su guía oficial ya preservada. Ahí también entra el riesgo de
  §267.3 (`BSubstitutePxView` vive en el jar `-wb` pero se persiste en el destino).

### Conexiones al resto del corpus

- **[Bloque 21]** — corregido en §270.5; este focus le da al corpus el subsistema en profundidad que aquel
  cubría como espinazo.
- **[Bloque 82]** — los diccionarios OEM Honeywell son instancias del contrato documentado acá.
- **[Bloque 5]** — ORD/BOG: las relaciones almacenan un `BOrd`, y de ahí viene la `UnresolvedException` del
  §270.3.
- **Focuses PX (B179-B215)** — conectados por partida doble: `BPxViewTag` distribuye vistas a una flota
  ([Bloque 267] §267.2) y neqlize hace reutilizables sus bindings ([Bloque 269] §269.3).
- **[Bloque 254]** §254.3 — el mismo patrón de extensibilidad observado en el charting.
