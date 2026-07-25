# Bloque 265 — Tags (VI): Brick — dos clases que arreglan dos límites del framework

> **Qué documenta**: la ontología **Brick** en Niagara N4.14 — el módulo `brick-rt` (2 clases) y lo que la
> documentación oficial dice de su diccionario. Cierra el gap **T10**, descubierto durante este mismo focus al
> preservar `docTagging` (no estaba en el backlog original).
>
> **Por qué vale un bloque de 2 clases**: esas dos clases resuelven, cada una, **una limitación concreta que
> los bloques anteriores documentaron en el framework base y en Haystack**. Ver §265.2 y §265.3.
>
> **Fuentes**:
> - `$B` = `…/brick/brick-rt/vineflower/com/tridium/brick/` (leído íntegro inline: 145 líneas entre las 2)
> - **`[CERT-doc]`** = `sources/manuals/docTagging-N4.14-guide.md` §*Brick tag dictionary* / §*Brick custom
>   rules* (guía oficial Tridium N4.14, preservada y registrada en `SOURCES.md`)
> - Raíz: `/home/cristian/modules/Prototipos/modulos/organized/`
>
> **Método**: sin sub-agente — 2 clases están muy por debajo del umbral de delegación. Lectura directa del
> driver. Marcadores: `[CERT]` = código · `[CERT-doc]` = doc oficial · `[INFER]` = deducción. Bloque de
> EVIDENCIA.

---

## 265.1 — Brick es contenido, no motor `[CERT-doc]`

> *"The Brick tag dictionary is an instance of the standard Niagara Smart Tag Dictionary and **does not contain
> any custom properties or actions**. There are no custom views associated with this dictionary. The dictionary
> contains a collection of definitions for tags, tag groups, relations, and tag rules."*
> — `sources/manuals/docTagging-N4.14-guide.md` §*Brick tag dictionary* `[CERT-doc]`

Y sobre cómo se genera:

> *"The JSON file generated as part of the import process contains a list of rules to be included in the Brick
> tag dictionary. If desired, you can **add to or override the default rules with a custom rule set** … you
> create their own JSON rules file and select it in the Select a JSON file for custom rules (optional) field in
> the Generate Brick Dictionary dialog"*
> — ídem §*Brick custom rules* `[CERT-doc]`

`[CERT]` El módulo `brick-rt` tiene **exactamente 2 clases** — `BBrickIdTag` (62 líneas) y
`BBrickInverseRelation` (83). No hay diccionario Java, ni importador, ni vistas.

`[INFER]` Es la confirmación más fuerte de la tesis que venía armándose: **el framework de tags de Niagara
aloja una ontología completa de terceros con dos clases de código**. Comparar el costo de las tres ontologías
que N4.14 soporta:

| Ontología | Clases de código | De dónde sale el contenido |
|---|---|---|
| Niagara (`n`) | ~24 (motor + 14 tags computados) | `Niagara.bog` embebido ([Bloque 261] §261.1) |
| Haystack (`hs`/`h4`) | 37, de las cuales **35 son tags computados** | `tags.csv` + `defs.json` 497 KB ([Bloque 264] §264.3) |
| **Brick** | **2** | JSON generado por import, con reglas custom del usuario |

## 265.2 — `BBrickIdTag`: un identificador estándar y estable `[CERT]`

```java
               return new Tag(this.getTagId(), BString.make(UUID.nameUUIDFromBytes(id.toString().getBytes(StandardCharsets.UTF_8)).toString()));
```
`$B/BBrickIdTag.java:53` `[CERT]`

Extiende `BTagInfo` con `validity` fijada a `new BIsTypeCondition(BControlPoint.TYPE)` (`:28-29`) `[CERT]` —
es decir, **solo aplica a puntos de control**. Y computa su valor así: arma el string
`"<stationName>:<handleOrd>"` y lo pasa por `UUID.nameUUIDFromBytes()`, que es un **UUID v3 name-based**.

`[INFER]` Dos propiedades que importan:

- **Es determinístico**: la misma station y el mismo handle producen siempre el mismo UUID. No se guarda en
  ningún lado — se recalcula, como todos los tags computados ([Bloque 261] §261.2).
- **Es un identificador estándar**: un UUID, no un ORD.

**Contraste directo con Haystack** ([Bloque 264] §264.5-c): allá `h4:id` es un handle ORD de Niagara con `$`
sustituido por `~`, que ningún cliente externo puede parsear sin conocimiento de Niagara. Acá, con una línea,
Brick produce un identificador que cualquier consumidor entiende.

`[INFER]` El precio del determinismo es la fragilidad ante el movimiento: el UUID deriva del **handle ORD**,
así que un punto que cambia de lugar en el árbol cambia de identidad. Es la misma dependencia del handle que
tiene `hs:id`, solo que envuelta en un UUID.

## 265.3 — `BBrickInverseRelation`: el inverso explícito que el framework no tiene `[CERT]`

[Bloque 262] §262.4 estableció que en Niagara **no existe una clase inversa**: la dirección se resuelve por
doble despacho dentro del mismo `BRelationInfo`, y el inverso se computa a demanda. Brick agrega justamente
eso, y de forma declarativa:

```java
public class BBrickInverseRelation extends BRelationInfo {
   public static final Property inverseName = newProperty(0, "", null);
```
`$B/BBrickInverseRelation.java:28-29` `[CERT]` — un slot `inverseName` con el nombre de la relación inversa.

El mecanismo (`addRelations`, `:52-66`) `[CERT]`:

```java
         for (Relation relation : directRelations.getAll(inverseId)) {
```
`:58` — busca en las relaciones **directas** del componente las que tengan el id **inverso**…

```java
               relations.add(new BasicRelation(id, otherEnd, !relation.isInbound()));
```
`:62` — …y por cada una emite una `BasicRelation` **implícita** con el id propio y la **dirección negada**,
previo chequeo de que no exista ya (`:61`).

`[INFER]` Traducido: si alguien declaró `brick:feeds` como relación almacenada de A hacia B, un
`BBrickInverseRelation` con `inverseName = "feeds"` hace que B exponga automáticamente `brick:isFedBy` hacia A
— **sin almacenar nada**. Convierte una relación unidireccional guardada en un par bidireccional consultable.

Y es **serializable**: `encodeToJson`/`decodeFromJson` (`:75-82`) `[CERT]` incluyen `inverseName`, así que la
definición del inverso viaja en el JSON del diccionario. `[INFER]` Eso es lo que permite que el generador de
Brick —que produce el diccionario desde un JSON de reglas (§265.1)— declare pares inversos sin escribir Java.

**Es la pieza que la ontología Brick necesitaba y el framework base no daba.** Brick, como modelo semántico,
define relaciones en pares (`feeds`/`isFedBy`, `hasPart`/`isPartOf`); Niagara solo ofrecía navegación
estructural cableada en Java ([Bloque 262] §262.3). Con 83 líneas, Brick vuelve el inverso **declarativo y
serializable**.

## 265.4 — Lo que esto dice del framework `[INFER]`

Los seis bloques de este focus permiten ahora una lectura que ninguno daba por separado:

- El motor de tags es **genérico de verdad**: aloja tres ontologías con costos de código de 24, 37 y **2**
  clases.
- Lo que un diccionario de terceros **no puede** hacer declarativamente, lo agrega con una clase chica: Brick
  necesitaba identificadores estándar y relaciones inversas, y resolvió cada una con una.
- El punto de extensión que **sí** se usa en la práctica es la **subclase de `BTagInfo`/`BRelationInfo`
  montada en un diccionario** — no el registro de agentes. Coincide con lo que [Bloque 254] §254.3 observó en
  el charting: los mecanismos declarativos existen, pero la extensión real pasa por subclasificar y montar.

## 265.5 — Conexiones

- **[Bloque 262]** §262.4 — **completado**: el framework no materializa inversos, pero un diccionario puede
  aportar el suyo de forma declarativa y serializable (§265.3).
- **[Bloque 264]** §264.5-c — contraste directo de identificadores: `h4:id` no es estándar, `brick:id` sí
  (§265.2).
- **[Bloque 261]** §261.2 — `BBrickIdTag` sigue el patrón de tag computado del namespace `n`.
- **[Bloque 260]** §260.1 — Brick se monta como cualquier diccionario; su generación desde JSON con reglas
  custom es exactamente el flujo de import documentado allí.
- **[Bloque 39]** y **[Bloque 99]** — los dos únicos bloques del corpus que rozaban Brick; éste lo abre.
- **Gaps abiertos**: T6 (exportTags-rt), T7/T8 (las UI), T9 (doc oficial, preservada y pendiente de
  back-fill).
