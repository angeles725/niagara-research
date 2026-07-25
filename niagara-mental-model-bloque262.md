# Bloque 262 — Tags (III): el sistema de RELACIONES de Niagara N4

> **Qué documenta**: qué es una relación en N4, cómo se almacena, cómo se resuelve, y **qué pasa cuando su
> destino desaparece**. Cierra el gap **T3** del focus `tags`.
>
> **Por qué importa**: en 261 bloques, el corpus **nunca documentó las relaciones**. [Bloque 21] menciona
> "relation" 35 veces sin dedicarle una sección. Es terreno nuevo.
>
> **Primera vez que el corpus usa la documentación oficial de Tridium como fuente.** El gap pasó el gate
> **e3 SCOUT** con veredicto `CERTIFIABLE-NOW`, y la guía quedó preservada y registrada en `SOURCES.md`
> (§5) antes de escribir una línea. Eso permite contrastar **lo que Tridium dice** contra **lo que el código
> hace** — y en §262.6 esa diferencia importa.
>
> **Fuentes**:
> - `$R` = `…/tagdictionary/tagdictionary-rt/vineflower/com/tridium/tagdictionary/relation/` (7 clases)
> - `$T` = `…/tagdictionary/tagdictionary-rt/vineflower/javax/baja/tagdictionary/`
> - `$B` = `…/baja/baja/{vineflower,decompiled}/javax/baja/` (para `BRelation` / `BasicRelation`)
> - **`[CERT-doc]`** = `sources/manuals/docRelations-N4.14-guide.md` — guía oficial *Relations* de N4.14
>   (17 HTML extraídos de `organized/docRelations/**`, registrada en `sources/SOURCES.md`)
> - Raíz: `/home/cristian/modules/Prototipos/modulos/organized/`
>
> **Método**: barrido delegado (tier `sonnet`, 75 llamadas) + verificación inline: **10 tokens** re-verificados.
> Marcadores: `[CERT]` = código · `[CERT-doc]` = doc oficial preservada · `[INFER]` = deducción. Bloque de
> EVIDENCIA.

---

## 262.1 — Una relación son DOS cosas distintas bajo la misma interfaz `[CERT]`

Éste es el hecho que ordena todo lo demás. La interfaz `Relation` tiene **dos implementaciones con naturaleza
opuesta**:

**(a) `BRelation` — la relación ALMACENADA** (`javax.baja.sys.BRelation extends BStruct implements Relation`):

```java
    public static final Property relationId = BRelation.newProperty(0, "", …);
    public static final Property inbound = BRelation.newProperty(5, false, null);
    public static final Property sourceOrd = BRelation.newProperty(0, BOrd.NULL, null);
    protected BComponent direct;
```
`$B/sys/BRelation.java:43-48` `[CERT]`

Es un **slot de propiedad** real sobre un `BComponent`: `relationId` (el `namespace:name` como String),
`inbound` (oculto, flags=5), `sourceOrd` (el endpoint por ORD) y `relationTags` (facets). El campo `direct`
es una referencia en memoria, no persistida.

`[INFER]` Se guarda **en un solo extremo**. `activateRelation()` instala un `RelationKnob` de vuelta en el
extremo remoto, así que en runtime ambos extremos navegan la relación — pero en el `.bog` solo figura en el
que la declara.

**(b) `BasicRelation` — la relación IMPLÍCITA** (`javax.baja.tag.BasicRelation`):

```java
public final class BasicRelation implements Relation, Taggable {
   private final Id id;
   private final Entity endpoint;
   private final BOrd endpointOrd;
   private final Tags tags;
   private final boolean inbound;
```
`$B/tag/BasicRelation.java:7-12` `[CERT]`

`final`, todos los campos `final`: **valor inmutable que nunca se persiste**. Se calcula al momento de la
consulta y se descarta.

`[INFER]` La consecuencia práctica: preguntar "¿qué relaciones tiene este componente?" devuelve una mezcla de
cosas guardadas en disco y cosas calculadas al vuelo, indistinguibles por su interfaz. La API sí las separa
—`SmartRelations` declara `getDirectRelations()` y `getImpliedRelations()` por separado— pero el tipo
`Relation` que devuelven es el mismo.

## 262.2 — Lo que dice la documentación oficial `[CERT-doc]`

La guía oficial de Tridium define el modelo en términos gramaticales:

> *"entities can be thought of as **nouns**, while relationships can be thought of as **verbs** connecting two
> or more nouns (entities). For example: where AHU1 supplies VAV1, both AHU1 and VAV1 are nouns (entities) and
> 'supplies' is the verb (relationship). Taking it further, AHU1 (the subject) supplies (the verb/predicate)
> VAV1 (the object)."*
> — `sources/manuals/docRelations-N4.14-guide.md` §*Entity-Relationship Modeling* `[CERT-doc]`

Sobre la identidad:

> *"The Relation Id is comprised of a dictionary and name, generally displayed as two pieces of text separated
> by a colon (:) … `<dictionaryNamespace>:<name>`"*
> — ídem §*Relation Id Structure* `[CERT-doc]`

Y sobre los dos tipos, confirmando §262.1 desde el lado del producto:

> *"When adding a relation, your choices are limited to relations that are defined in the any of the Tag
> Dictionaries on your system. **Implied relations are determined automatically** and applied to a component by
> the system. Implied relations are defined in a SmartTagDictionary under its Tag Rules folder (BTagRuleList).
> When an application queries for the relations on a component in the station, the SmartTagDictionary executes
> code that interprets the Tag Rules against the given component"*
> — ídem §*Types of Relations* `[CERT-doc]`

`[INFER]` La doc y el código coinciden: **el Id de una relación tiene exactamente la misma forma que el de un
tag** (`namespace:name`), y las relaciones directas están limitadas a las **definidas en un diccionario** — no
se pueden inventar en el momento desde la UI.

## 262.3 — Las 7 clases: navegación estructural, y nada más `[CERT]`

Las 7 de `relation/` extienden `BRelationInfo` y **solo producen `BasicRelation`** (implícitas). Se agrupan en
tres pares/tríos que recorren la jerarquía de componentes:

| Grupo | Clases | Id | Qué navega |
|---|---|---|---|
| Jerarquía genérica | `BComponentParentRelation` · `BComponentChildRelation` | `n:parent` · `n:child` | `BComplex` ↔ padre/hijos que sean `Entity` |
| Device ↔ Network | `BParentNetworkRelation` · `BChildDeviceRelation` | `n:parentNetwork` · `n:childDevice` | `BDevice` ↔ su `BDeviceNetwork` ancestro |
| Device ↔ Point | `BParentDeviceRelation` · `BChildPointRelation` · `BChildNullProxyPointRelation` | `n:parentDevice` · `n:childPoint` · `n:childNullProxyPoint` | `BDevice` ↔ sus `BControlPoint` descendientes |

`[CERT]` La distinción del último trío es fina y útil: `BChildPointRelation` devuelve los puntos cuyo
`proxyExt` **no** es `BNullProxyExt` (puntos reales, ligados a campo) y `BChildNullProxyPointRelation` los que
**sí** lo son (puntos virtuales / sin ligar). `BParentDeviceRelation` no discrimina.

`[INFER]` Para un integrador: `n:childPoint` es la consulta que responde *"qué puntos de este device están
realmente ligados al campo"* — y esa distinción no existe en la jerarquía de componentes, solo en las
relaciones.

## 262.4 — Dirección: no hay clase inversa, hay doble despacho `[CERT]`

`Relation` define `INBOUND = true` / `OUTBOUND = false`. Cada `BRelationInfo` responde **a ambos extremos
dentro de la misma clase**, decidiendo la dirección según el tipo de la entidad que recibe
(`$R/BParentDeviceRelation.java:45-57`) `[CERT]`: si le pasan un `BDevice`, emite relaciones `inbound` hacia
cada punto; si le pasan un `BControlPoint`, emite una `outbound` hacia el device.

`[INFER]` **El inverso no está materializado ni existe como clase separada: se computa a demanda.**
`BComponentParentRelation` y `BComponentChildRelation` sí son clases distintas, pero porque tienen **Ids
distintos** (`n:parent` vs `n:child`) — no son una relación y su inversa.

## 262.5 — El trace de `n:tagGroup`: es ALMACENADA, no implícita `[CERT]`

[Bloque 260] §260.4 dedujo que un componente se une a un grupo de tags mediante una relación `TAG_GROUP_RELATION`,
y [Bloque 261] §261.1 la identificó como `Id.newId("n","tagGroup")`. Ahora el mecanismo completo:

1. `BTagDictionaryService.getTagGroupRelations(entity)` pide `relations.getAll(TAG_GROUP_RELATION, 2)` —
   dirección OUT (`$T/BTagDictionaryService.java:664-679`).
2. `ComponentRelations.filter()` escanea los **slots de propiedad** del componente buscando los de tipo
   `BRelation` con ese id.
3. El endpoint se resuelve al `BTagGroupInfo` y se llama `addAllImpliedTags(entity, tags)`.

`[CERT]` **Ninguna de las 7 clases de `relation/` produce `n:tagGroup`.** Esa relación se guarda como
propiedad `BRelation` cuando el componente se asigna al grupo.

`[INFER]` Es el dato arquitectónico más importante del gap: las 7 clases son **navegación estructural**
(padre/hijo/device/network/punto) y el mecanismo de tag-groups es **otra cosa** — almacenamiento. Quien vaya a
construir sobre relaciones necesita saber que está ante dos subsistemas, no uno.

## 262.6 — INTEGRIDAD REFERENCIAL: el rename sí, el borrado NO `[CERT]`

La pregunta que a un integrador le importa de verdad: ¿qué pasa con una relación cuando su destino cambia?

**Al renombrar: se arregla sola** `[CERT]`. `BTagDictionaryService.navEvent()` escucha eventos de navegación y
actualiza el ORD guardado:

```java
   public final void navEvent(NavEvent event) {
      if (event.getId() == 3 && event.getParent() instanceof BComponent) {
```
`$T/BTagDictionaryService.java:999-1000` — el id 3 es el rename; de ahí sale un
`knob.getRelation().setEndpointOrd(...)`.

**Al borrar: no pasa nada** `[CERT]`. Ese `if` es el **único** filtro de `navEvent` — no hay rama para borrado
(barrido sobre `$T/BTagDictionaryService.java`: `event.getId() ==` aparece una sola vez). Y cuando después
alguien intenta resolver el endpoint muerto:

```java
      } catch (UnresolvedException | IllegalStateException var3) {
         if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Could not resolve the endpoint of the tag group relation " + …);
         }
         return null;
```
`$T/BTagDictionaryService.java:684-693` `[CERT]`

`[INFER]` El resultado concreto:

- La propiedad `BRelation` **sobrevive indefinidamente** en el componente apuntando a algo que ya no existe.
- Al resolverla se devuelve `null` y se loguea **a nivel FINE** — que en una station de producción está
  apagado. **El fallo es invisible.**
- **No hay recolección de basura ni tombstones**: nada limpia esas relaciones colgadas.

`[INFER]` Traducción operativa: si borrás un `BTagGroupInfo` de un diccionario, todos los componentes que
estaban asignados a ese grupo quedan con una relación muerta que no da error, no aparece en ningún log
operativo, y simplemente **deja de aportar los tags del grupo**. Un punto que "perdió sus tags" sin que nada
lo reporte. **Ámbito de esta ausencia**: verificado sobre todo `tagdictionary-rt/vineflower/` y sobre
`ComponentRelations` en `baja`.

Las relaciones **implícitas** no sufren esto: se calculan de la jerarquía viva en cada consulta (§262.3).

## 262.7 — Gotchas de rendimiento y de contrato `[CERT]`

- **Escaneo completo de descendientes en cada consulta** `[CERT]`:
  `return (BControlPoint[])CompUtil.getDescendants(device, BControlPoint.class);`
  (`$R/BParentDeviceRelation.java:61`) — **sin caché**. `[INFER]` Cada resolución de relaciones implícitas de
  un device recorre su subárbol completo; con cientos de puntos por device y muchos devices, el costo se
  multiplica. Contrasta con los índices de [Bloque 261] §261.3, que cachean **tags** pero no esto.
- **`ComponentRelations.set()` devuelve `null`** en sus dos sobrecargas — un llamador que use el retorno se
  come un NPE.
- **`ComponentRelations.add()` exige un `BRelation` concreto** y lanza `IllegalArgumentException` ante un
  `BasicRelation`, aunque la interfaz `Relations` declare `add(Relation)`. `[INFER]` Requisito de tipo
  concreto no expresado en el contrato: no podés agregar una relación implícita como si fuera almacenada.
- **Ruido de decompilación** `[CERT]`: el campo `TAG_GROUP_RELATION` aparece con su nombre real en unos
  archivos y como `BNiagaraTagDictionary.n` en otros pipelines. Es el mismo `Id.newId("n","tagGroup")`
  (`$C/BNiagaraTagDictionary.java:143`); se anota para que una lectura cruzada no lo confunda con dos cosas.

## 262.8 — Conexiones

- **[Bloque 260]** §260.4 y **[Bloque 261]** §261.1 — cierra el hilo `TAG_GROUP_RELATION` de punta a punta, y
  corrige la impresión de que sería una relación implícita: es **almacenada** (§262.5).
- **[Bloque 21]** — le da al corpus la sección de relaciones que aquel bloque nunca tuvo.
- **[Bloque 5]** (ORD/BOG) — `sourceOrd` es un `BOrd`, así que las relaciones heredan la semántica de
  resolución de ORDs; el fallo de §262.6 es una `UnresolvedException` de ese subsistema.
- **[Bloque 82]** — los diccionarios OEM Honeywell pueden definir sus propias relaciones por el mismo
  mecanismo (§262.2, doc oficial).
- **Gaps abiertos**: T4 (`condition/` + `neqlize/`), T5 (haystack), T6-T8 (exportTags, UI), T9 (el resto de
  la doc oficial — `docTagging` 94 HTML y `docExportTags` 88, aún sin preservar).
