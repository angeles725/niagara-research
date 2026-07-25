# Bloque 269 — Tags (X): la documentación oficial — lo que resuelve, lo que matiza y lo que agrega

> **Qué documenta**: qué aporta la **documentación oficial de Tridium** sobre tagging que el decompilado no
> dice. Cierra el gap **T9** — el último del focus `tags`.
>
> **Método distinto a los ocho bloques anteriores**: no es un barrido de código. Es un contraste
> **doc-vs-código** sobre las fuentes preservadas, organizado en tres categorías: lo que **resuelve** un
> `UNVERIFIED` previo, lo que **matiza** un hallazgo, y lo que **agrega** conceptos que el código no expone.
>
> **Fuentes `[CERT-doc]`** (preservadas bajo el gate e3 `CERTIFIABLE-NOW` y registradas en `SOURCES.md`):
> - `sources/manuals/docTagging-N4.14-guide.md` — 93 secciones, 182 KB
> - *(`docExportTags-N4.14-guide.md` quedó preservada pero **este bloque no la cita**: su contenido es el
>   workflow de `exportTags`, que [Bloque 266] §266.1 estableció que NO pertenece a este subsistema. Se deja
>   registrada en `SOURCES.md` sin bloque citante, para un focus futuro de Niagara Network.)*
> - `sources/manuals/docRelations-N4.14-guide.md` — 17 secciones (ya usada en [Bloque 262])
>
> Marcadores: `[CERT-doc]` = doc oficial preservada · `[CERT]` = código ya citado en bloques previos ·
> `[INFER]` = deducción. Bloque de **SÍNTESIS DOCUMENTAL** — el ratio alto es esperado y no indica agotamiento
> (§11).

---

## 269.1 — RESUELVE: el límite de licencia opaco de [Bloque 260] `[CERT-doc]`

[Bloque 260] §260.2 documentó que cada diccionario consulta `fw(501, "dictionary.limit", …)` al arrancar y
queda en `fatalFault` si se excede un tope **cuyo valor no es visible desde la API pública**. Quedó como
UNVERIFIED. La doc lo resuelve:

> *"The tags license is required to use the TagDictionaryService and tag dictionaries on a station. The
> **Dictionary.limit** attribute limits the number of tag dictionaries available for the system. Any
> dictionaries added above the limit for the license will be in fault. When a dictionary is in fault, **the
> tags in that dictionary are not available in the Edit Tags dialog**. **By default, you are limited to the
> first two tag dictionaries.** However, the Dictionary.limit attribute is configurable on the license in the
> same manner as are device limits."*
> — `docTagging-N4.14-guide.md` §*License requirements* `[CERT-doc]`

`[INFER]` **La consecuencia operativa es fuerte y concreta**: por defecto **solo dos diccionarios**. El
diccionario `Niagara` (`n`) está siempre presente ([Bloque 261] §261.1), así que **queda uno libre**. Montar
Haystack completo son **dos** diccionarios (`hs` + `h4`, [Bloque 264] §264.2) → con licencia por defecto, uno
de los tres queda **en fault**, y sus tags **desaparecen del diálogo de edición** sin que nada explique por
qué.

`[INFER]` Para quien trabaja con licencias ajustadas —el caso de las v4.12 con SMA vencido que el corpus ya
analizó— esto es material de planificación: **agregar Brick o un diccionario OEM propio requiere ampliar
`Dictionary.limit` en la licencia**, igual que se amplía el límite de devices. No es una configuración de
station.

## 269.2 — MATIZA: los índices sin techo de [Bloque 261] `[CERT-doc]`

[Bloque 261] §261.3 registró que ni `TagRuleIndex` ni `EntityTagIndex` tienen cap, TTL ni evicción, y dedujo
crecimiento sin techo con el tamaño de la station. La doc agrega el dato que faltaba:

> *"The index, which is built as NEQL queries are executed, maps tags to the tag rules that imply those tags.
> This index should not require a significant amount of system memory. NOTE: The type of memory being used by
> the tag rule and implied tag indexes is **heap memory**. So these indexes are **not stored persistently but
> are built dynamically after every station reboot** when NEQL queries are submitted."*
> — `docTagging-N4.14-guide.md` §*Tag Rule Index* `[CERT-doc]`

`[INFER]` **El matiz corrige el alcance de mi deducción, no el hecho**: los índices efectivamente no tienen
evicción, pero **son transitorios** — viven en heap, se construyen perezosamente al ejecutarse consultas NEQL,
y **mueren en cada reinicio de la station**. No hay crecimiento acumulativo entre reboots. El riesgo real no
es la fuga a largo plazo sino la **presión de heap dentro de una sesión larga en un JACE**, que es
exactamente lo que la doc advierte al mencionar sus limitaciones de memoria.

La doc también explica **para qué** existen, con un ejemplo que el código no da:

> *"a NEQL search on an unindexed tag (ex.: `c:city`) requires that every tag rule that contains this tag be
> evaluated; every component throughout the station be evaluated for this tag; and this evaluation must be
> done every time a search for this tag is initiated."*
> — ídem §*Implied tags index* `[CERT-doc]`

`[INFER]` Sin índice, **cada consulta NEQL sobre un tag implícito es un barrido completo de la station**. Eso
justifica el diseño ansioso que [Bloque 261] documentó.

## 269.3 — CONFIRMA: para qué sirve neqlize, en palabras de Tridium `[CERT-doc]`

[Bloque 263] §263.4 dedujo que neqlize es identificación inversa al servicio del "armame la query" de la UI, y
§263.5 dedujo que se excluye `n:name` para forzar un identificador semántico. La doc confirma ambas cosas y
nombra el caso de uso:

> *"In Niagara 4.9 and later, there is added support for **tag-based NEQL query Ords**. The
> TagDictionaryService features several added properties to specify certain tags and relations to be excluded
> when **converting slot path Ords to NEQL query Ords**. For example, a tag-based NEQL query **Px Ord binding**
> using the `n:name` tag would **hurt the reusability of a graphic** because the bound component would have to
> be named the same under a different base component. Using the `n:ordInSession` or Haystack `hs:id` tag would
> be equivalent to using an absolute slot path Ord."*
> — `docTagging-N4.14-guide.md` §*Neqlize options* `[CERT-doc]`

`[INFER]` El caso de uso es **bindings de PX reutilizables**: convertir un binding atado a una ruta de slot en
uno atado a una consulta semántica, para que **el mismo gráfico sirva bajo cualquier equipo** sin depender de
nombres ni rutas. Conecta directamente con [Bloque 267] §267.2 (distribución de vistas PX a una flota): son
dos soluciones al mismo problema —reutilizar un gráfico— por caminos distintos, una por sustitución de ORDs y
otra por consulta semántica.

`[CERT-doc]` También fecha la funcionalidad: **Niagara 4.9 en adelante**.

## 269.4 — AGREGA: tres conceptos que el código no expone `[CERT-doc]`

**a) Ad Hoc tags — tags sin diccionario**

> *"You can add Ad Hoc Tags to any station object to provide additional semantic information **without using
> an installed tag dictionary**. Ad Hoc tags are tags that you create directly from the Edit Tags dialog box.
> **These tags are not found in any tag dictionary.** … useful for development or testing purposes"*
> — §*Adding Ad Hoc tags* `[CERT-doc]`

`[INFER]` Todo el focus documentó el tagging como algo gobernado por diccionarios: los ids se generan contra
el namespace del diccionario ([Bloque 261] §261.5), las relaciones directas se limitan a las definidas
([Bloque 262] §262.2). Los Ad Hoc son la **vía de escape**: un tag arbitrario, sin definición, sin validez, sin
política de datos. La doc misma recomienda no usarlos en producción.

**b) Choice tags — el enum que implica un marcador**

> *"The choice def is added as a **BDynamicEnum** tag with the choice values included in the value's
> BEnumRange. **Tag rules are added to imply the corresponding choice value tag based on the selected enum
> value.** For example, if the `pipeFluid` tag on a component is set to 'water', a **water marker tag will be
> implied** on that component."*
> — §*Choice tags* `[CERT-doc]`

`[INFER]` Explica para qué existe `BDynamicEnumTagInfo` ([Bloque 260] §260.3), que en el código era solo "un
tag con `BEnumRange`". El patrón real: **un tag enumerado + reglas que derivan un marcador del valor
elegido**. Es composición de los dos mecanismos que el focus documentó por separado — tag tipado ([260]) y
regla implícita ([263]) — para modelar taxonomías de Haystack 4.

**c) Offline tagging — un descubrimiento adicional al montaje**

> *"Offline tagging in a station when no dictionaries are found: The system **searches for tag dictionaries**
> in the following locations: all **palettes of installed tag dictionary modules**, in the
> **`user-home/tagDictionary` folder** where custom tag dictionaries are stored, also searches for implied
> tags"*
> — §*Online tagging versus offline tagging* `[CERT-doc]`

`[INFER]` **Matiza [Bloque 260] §260.1**: allí establecí que un diccionario "no se registra, se monta" bajo el
servicio, y eso sigue siendo cierto **para una station viva**. Pero en modo **offline** —editando un `.bog`
sin station corriendo— hay una vía de descubrimiento distinta: el sistema **busca** en las paletas de los
módulos instalados y en una carpeta del user-home. Son dos regímenes, no uno.

## 269.5 — Lo que la doc NO resuelve `[INFER]`

Honestidad sobre los límites de esta fuente: la doc oficial es **guía de producto**, no especificación
técnica. No aporta nada sobre lo que más importaría verificar de los hallazgos del focus:

- **No menciona** la falta de integridad referencial al borrar el destino de una relación ([Bloque 262]
  §262.6) — el comportamiento silencioso que más puede morder en obra.
- **No menciona** los dos bypasses del candado `frozen` ([Bloque 260] §260.6, [Bloque 261] §261.6).
- **No menciona** que la validación de predicados NEQL al importar loguea y continúa ([Bloque 261] §261.4).
- **No menciona** las cuatro fallas silenciosas (migración parcial, import en hilo de fondo, etc.).

`[INFER]` No es sorprendente —una guía de usuario documenta el camino feliz—, pero **fija el valor relativo de
las dos fuentes**: la doc explica **intención y operación**; solo el decompilado revela **qué pasa cuando algo
sale mal**. Los hallazgos operativamente más peligrosos de este focus salieron todos del código, ninguno de la
doc.

## 269.6 — Conexiones

- **[Bloque 260]** §260.2 — **UNVERIFIED resuelto**: `Dictionary.limit` = 2 por defecto, ampliable en la
  licencia (§269.1).
- **[Bloque 261]** §261.3 — **matizado**: los índices no tienen evicción pero son de heap y **transitorios**
  (§269.2); §261.5 y §260.3 quedan explicados por los Choice tags (§269.4-b).
- **[Bloque 263]** §263.4-5 — **confirmado por la fuente oficial**, con el caso de uso nombrado: bindings PX
  reutilizables, desde N4.9 (§269.3).
- **[Bloque 262]** §262.2 — la doc de relaciones ya se había usado allí.
- **[Bloque 267]** §267.2 — neqlize y la distribución de vistas PX resuelven el mismo problema por caminos
  distintos (§269.3).
- **Estado del focus**: con T9 cerrado, **read-only-investigable = 0 → STOP (§8)**. Sigue la síntesis de
  cierre.
