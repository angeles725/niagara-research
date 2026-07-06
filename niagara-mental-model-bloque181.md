# Bloque 181 — Gramática autoritativa del formato PX: PxDecoder / PxEncoder

> Research del focus **`px-menu`** (gap G6): la **gramática autoritativa** del archivo `.px`, leída en el
> parser (`PxDecoder`) y el serializador (`PxEncoder`) del framework. El encoder ES la especificación
> canónica de la sintaxis: define qué es atributo vs sub-elemento, cómo se resuelve el tipo de widget, y por
> qué el tag va en una sola línea. Fundamenta las reglas que B182 aplica al `menu.px`. NO cubre layout (B-G7)
> ni el catálogo de widgets (B36).
>
> Sources (preservados §5): `sources/decompiled/bajaui-wb-px/PxDecoder.java` (sha256 `9502c97c…`) +
> `PxEncoder.java` (sha256 `3723057a…`) — source original Tridium (docSource-doc) de `bajaui-wb`,
> `javax.baja.ui.px`. Barrido delegado (sonnet) 2026-07-06.
> Method: lectura READ-ONLY del decompilado. Markers (METHODOLOGY §3): `[CERT]` fuente primaria local
> (`file:line`) · `[INFER]` deducción.
>
> Capa PX (formato). Connects [Block 22] (formato PX visión general), [Block 179] (framing focus), [Block 36] (widgets).

---

## 181.1 — Estructura del documento: `<px version="1.0">` + secciones en orden fijo `[CERT]`

El elemento raíz DEBE llamarse `px` y su atributo `version` DEBE ser `"1.0"` — cualquier otra cosa lanza
`XException`:
`PxDecoder.java:193-199` — `if (!root.name().equals("px")) throw err("Root element must be \"px\"", root); … if (!ver.equals("1.0")) throw err("Only version 1.0 is supported", root);`. `[CERT]`

El encoder lo emite simétricamente (`PxEncoder.java:138-140`):
`w("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"); w("<!-- Niagara Presentation XML -->\n"); w("<px version=\"1.0\"");`. `[CERT]`

Un atributo opcional `media="..."` nombra el media type (`BAbstractPxView`): emitido en `PxEncoder.java:141`,
decodificado en `PxDecoder.java:202-213`. Las secciones van en **orden fijo**: `<import>`, `<properties>`
(opcional), `<layers>` (opcional), `<content>` (`PxEncoder.java:143-146`; decode `PxDecoder.java:140-152`). `[CERT]`

El `<import>` lista **todos** los módulos referenciados en el árbol de widgets, recolectados por `scanModules`
que camina recursivamente los slots (`PxEncoder.java:158-186`); el decoder los guarda en un array `modules[]`
para resolver tipos (`PxDecoder.java:218-230`). `[CERT]`

Implicación para el `menu.px`: el `<import>` debe declarar cada módulo cuyos widgets/bindings uses
(`baja`, `gx`, `bajaui`, y `converters` si usás el converter de `visible`). `[INFER]`

## 181.2 — Elemento = widget; resolución de tipo vía `<import>`, NO por namespace `[CERT]`

El nombre del elemento XML ES el nombre PELADO del tipo (`Label`, `CanvasPane`) — sin prefijo de módulo en el
tag. El decoder resuelve probando `modulo:typeName` contra cada módulo importado hasta que uno matchea:
`PxDecoder.java:471-486` — `type = Sys.getRegistry().getType(modules[i] + ":" + typeName);` (cacheado en un
`HashMap`). `[CERT]`

El encoder escribe solo el nombre pelado: `PxEncoder.java:275-276` —
`String type = v.getType().getTypeName(); indent(indent).w("<").w(type);`. `[CERT]`

**No hay maquinaria de namespace XML `px:`** en ninguna de las dos clases: la calificación de módulo la lleva
ENTERAMENTE el bloque `<import>`. `[CERT]` Por eso dos módulos con un tipo del mismo nombre se desambiguan por
el ORDEN de import (primer match gana, `PxDecoder.java:471-486`). `[INFER]`

## 181.3 — Atributo = propiedad simple; sub-elemento = todo lo demás `[CERT]`

`encodeAs()` decide ATRIBUTO vs SUB-ELEMENTO: una propiedad es atributo **solo si** es frozen, de tipo
no-abstracto, y su valor `isSimple()` (`PxEncoder.java:407-412`):
`return (prop.isFrozen() && … value.isSimple())`. Todo lo demás (dinámico, abstracto, o valor complejo) va
como sub-elemento. `[CERT]`

**Solo se emiten propiedades NO-default**: `PxEncoder.java:378-382` —
`if (prop.isEquivalentToDefaultValue(propVal)) return NEVER;`. Esto explica por qué un `.px` real es compacto
(no lista cada propiedad, solo las cambiadas). `[CERT]`

El valor se serializa con `value.asSimple().encodeToString()` (`PxEncoder.java:330-333`); el decoder invierte
con `decodeSimple(...)` sobre `x.attrValue(i)` iterando los atributos (`PxDecoder.java:395-420`). `[CERT]`

## 181.4 — Sub-elemento = slot hijo / binding (uniforme) `[CERT]`

Bindings, componentes hijos añadidos y props complejas frozen se representan **todos** como sub-elementos.
El decoder lee el atributo `name` del hijo (`kid.get("name", null)`); si ese nombre matchea una propiedad
existente hace `c.set(name, value)`, si no, `c.asComponent().add(name, value)` = slot dinámico/binding
(`PxDecoder.java:423-458`). `[CERT]`

Esto **confirma desde el framework** la regla clave del focus: **un binding es un slot hijo del widget**
(lo que B180 §180.5 documentó desde la doc oficial y B182 aplica). Los atributos `f=` (flags) y `ft=` (facets)
en el hijo llevan metadata bajaux/binding por slot (`PxDecoder.java:440-447`; encode `PxEncoder.java:285-294`).
El converter dinámico `<IBooleanToSimple name="visible"/>` del patrón in-place (B182) es exactamente un slot
hijo cuyo `name` NO matchea una propiedad frozen → se agrega como slot dinámico. `[INFER]`

## 181.5 — El tag va en UNA sola línea (la raíz del gotcha XParser) `[CERT]`

El start tag completo (nombre + todos los atributos) se escribe con llamadas `w()` encadenadas **sin ningún
newline intermedio** hasta que `w(">\n")` o `w("/>\n")` cierra el tag (`PxEncoder.java:276-307` y `319-355`).
**No existe lógica de line-wrapping** en ninguna de las dos clases. `[CERT]`

Esto es consistente con la fragilidad empírica observada del `XParser`: partir un tag en varias líneas (poner
`foreground=...` en su propio renglón) provoca `XException: Expecting '='` — el encoder de Tridium nunca
genera eso, así que el parser no lo tolera de vuelta. Regla dura para editar `.px` a mano (B182). `[INFER]`

## 181.6 — Errores: `XException`, no un `PxException` dedicado `[CERT]`

Raíz/versión/secciones malformadas lanzan `XException` vía helpers `err()` locales
(`PxDecoder.java:521-529`). No existe una clase `PxException` en este módulo — el escaping exacto de
caracteres XML-especiales se delega a `XWriter.safe()`/`attr()` (`PxEncoder.java:289-332`), fuera de estas
dos clases, así que la regla exacta de escaping no es determinable solo desde acá (gap honesto). `[CERT]`

## 181.x — Connections

- **[Block 22]** — formato PX (visión general, `PxDecoder`/`PxEncoder` mencionados): este bloque baja al
  detalle `file:line` de la gramática que B22 describió a alto nivel.
- **[Block 179]** — framing del focus.
- **[Block 36]** — catálogo de widgets: los nombres de tipo que §181.2 resuelve vía `<import>`.
- **B182** (síntesis `menu.px`) — aplica §181.3 (atributo=prop), §181.4 (binding=slot hijo) y §181.5 (tag en 1 línea).
- **G7** (próximo) — layout de panes: cómo la propiedad `layout` (§181.3, atributo simple) se interpreta por cada pane.
