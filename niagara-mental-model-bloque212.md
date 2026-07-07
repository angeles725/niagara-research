# Block 212 — La capa de creación/inserción de widgets: `javax.baja.px.editor.factory` (10 clases): WidgetFactory + WidgetInserter (DTO) + 8 factories

> Research de la **INFRAESTRUCTURA de `pxEditor-wb`** — foco `px-editor-core`, gap **C3**. Documenta las 10
> clases de `javax.baja.px.editor.factory`: cómo un objeto soltado/pegado/elegido-por-wizard se convierte en
> un widget del canvas. `WidgetFactory` (base abstracta), `WidgetInserter` (el DTO-resultado, **NO** un
> insertador activo), los 8 factories concretos (Label/Picture/PxFile/JsFile/ImageFile/NavNode + Cloning +
> ImageCopying). Cierra el `WidgetInserter` que B201 (wizard/make) dejó fuera de scope. NO cubre util (C4) ni
> fieldeditors (C5); la LÓGICA de colocación real vive en el controller (fuera de este paquete).
>
> Sources (decompilado Vineflower, READ-ONLY):
> `/home/cristian/modules/Prototipos/modulos/organized/pxEditor/pxEditor-wb/vineflower/javax/baja/px/editor/factory/`
> (citas `file:line` relativas: `WidgetFactory.java:16` = `.../factory/WidgetFactory.java`).
> Method: lectura directa del decompilado (10 clases del paquete factory).
> Markers (canónico METHODOLOGY §3): `[CERT]` fuente primaria local (`file:line`) · `[INFER]` deducción.
>
> Capa infraestructura pxEditor-wb (núcleo). Connects [Block 211] (`PxEditorController` que ordena/despacha
> los factories), [Block 201] (make/wizard — `BMakeWidget` que también devuelve un `WidgetInserter`),
> [Block 191] (BPxEditor.cloneWidget usado por el cloning factory).

---

## 212.1 — `WidgetFactory`: base abstracta (editor + type, `make`, `canConvert` por tipo) `[CERT]`

Clase abstracta (no interfaz). Cada factory está ligada a un `Type` de widget destino y guarda el editor:

```java
// WidgetFactory.java:7-26
public WidgetFactory(BPxEditor editor, Type type) { this.editor = editor; this.type = type; }
public abstract WidgetInserter make(BObject[] var1);          // :16 — ÚNICO método de contrato abstracto
public boolean canConvert(BObject[] objects) {                 // :18-26
   for (i...) if (!objects[i].getType().is(this.type)) return false;
   return true;
}
```

| Elemento | Detalle | Cita |
|---|---|---|
| Tipo | clase **abstract** (no interfaz) | `WidgetFactory.java:7` |
| Estado | `editor: BPxEditor` + `type: Type` (widget destino) | `WidgetFactory.java:9-11` |
| Contrato | `abstract WidgetInserter make(BObject[])` | `WidgetFactory.java:16` |
| `canConvert` default | puramente por tipo: cada objeto soltado debe ser `.is(this.type)` | `WidgetFactory.java:18-26` |
| Prioridad | **NO existe** `getPriority()`/orden — el orden es externo (lista del controller, §212.5) | (ausencia verificada) |

## 212.2 — `WidgetInserter`: un DTO-resultado, NO un insertador `[CERT]`

**Corrección de nomenclatura**: pese al nombre, `WidgetInserter` no hace ninguna inserción — es un value-object
que transporta el RESULTADO de `make()`:

```java
// WidgetInserter.java:6-40
private BWidget[] widgets;
private Command auxCommand;      // side-effect opcional post-inserción
private int columnCount;         // ctor exige columnCount >= 1
// getters: getWidgets(), getAuxillaryCommand(), getColumnCount()  — sin insert()/doInsert()
```

No hay método `insert(...)`/`doInsert(...)` en la clase. `[INFER]` La colocación real en el árbol de
componentes PX (bounds/posición, `add()` a un contenedor) ocurre en el CONSUMIDOR del `WidgetInserter` —
casi seguro `BPxEditor`/controller (fuera de scope de este paquete; coincide con la deuda que B201 marcó: el
*inserter-object* es un DTO, la *conducta de inserción* vive en el controller). Notas:
- `auxCommand` — un `Command` de efecto lateral opcional (p.ej. `ApplyPxPropertiesToNewWidgets`, §212.4) que
  el controller presumiblemente ejecuta al colocar los widgets — hook de copia de propiedades post-inserción.
- `columnCount` — implica que el controller distribuye múltiples widgets devueltos en una grilla/columnas.
- **Convergencia**: `BMakeWidget` (wizard, B201) también devuelve un `WidgetInserter` — múltiples puntos de
  entrada (drop/paste/wizard) convergen en el mismo DTO.

## 212.3 — Los 8 factories concretos: qué acepta cada `canConvert` y qué construye `[CERT]`

| Factory | `canConvert` acepta | Construye | Cita |
|---|---|---|---|
| `LabelFactory` | `BLabel` (hereda) | subtipo fino de `ImageCopyingWidgetFactory`, type `BLabel.TYPE`; sin `make()` propio | `LabelFactory.java:6-9` |
| `PictureFactory` | `BPicture` (hereda) | ídem, type `BPicture.TYPE` | `PictureFactory.java:6-9` |
| `PxFileFactory` | `BPxFile.TYPE` (un `.px`) | un `BPxInclude` por objeto: setea su `BOrd` (relativizado vía `BOrdFE.isRelativize`), y **decodifica el px con `PxDecoder`** sólo para medir tamaño preferido y setear layout | `PxFileFactory.java:19-53` (decode `:39`, layout `:47`) |
| `JsFileFactory` | `BJavascriptFile.TYPE` | un `BWebWidget`, `setJs(BOrd.make(file.getFilePath()))`, layout fijo 400×400 | `JsFileFactory.java:13,29` |
| `ImageFileFactory` | `BIImageFile.TYPE` | un `new BLabel(img)` por archivo, dimensionado a la imagen; además provee utilidades `convertImages/convertImage/convertImageOrd` (copia física de assets, §212.4) | `ImageFileFactory.java:53-77` (build `:71`) |
| `NavNodeFactory` | `BINavNode.TYPE` **con override** | ver abajo | `NavNodeFactory.java:16-56` |
| `WidgetCloningFactory` | `BWidget.TYPE` (cualquier widget) | clon, §212.4 | `WidgetCloningFactory.java:10-35` |
| `ImageCopyingWidgetFactory` | (base de Label/Picture) | clon + localización de imágenes, §212.4 | `ImageCopyingWidgetFactory.java:8-32` |

**`NavNodeFactory` es especial** (`NavNodeFactory.java`): su `canConvert` (`:32-55`) OVERRIDEA el base para
**excluir** componentes cuyo `getComponentSpace()` sea `BModulePaletteNode` o un `BBogSpace` respaldado por
`BPaletteFile` — rechaza nodos del árbol de paleta aunque sean type-compatibles, para no tratar una entrada de
paleta como nav-target. Y su `make()` (`:21-30`) NO construye widget directo: **abre un wizard `BMakeWidget`**
y devuelve `dialog.getWidgetInserter()` sólo si el usuario confirma (`r == 1`), si no `null` — es el puente al
paquete make/wizard (B201): `NavNodeFactory` delega la elección de tipo de widget a `BMakeWidget`.

## 212.4 — Cloning vs Image-copying: dos caminos especiales `[CERT]`

**`WidgetCloningFactory`** (`WidgetCloningFactory.java:10-35`) — ligada al `BWidget.TYPE` genérico (matchea
CUALQUIER widget; se usa para copy/paste de widgets ya en el canvas). **NO** hace round-trip por
`PxEncoder`/`PxDecoder` (ninguna referencia a esas clases en el archivo) — el clon se hace vía
`editor.cloneWidget(widgets.get(i))` (`:30`), un método de `BPxEditor` (que internamente sí usa encode/decode
— B211 §211.1 — pero no visible en esta capa). Devuelve:

```java
// WidgetCloningFactory.java:33
return new WidgetInserter(newWidgets, new ApplyPxPropertiesToNewWidgets(editor, widgets, newWidgets));
```

— el slot `auxCommand` se puebla con un comando que copia propiedades visuales de los originales a los clones
tras la inserción.

**`ImageCopyingWidgetFactory`** (`ImageCopyingWidgetFactory.java:8-32`, clase package-private) — extiende
`WidgetFactory` directo (no `WidgetCloningFactory`, aunque le delega). Su `make()` (`:13-23`):
1. Llama `WidgetCloningFactory.makeClonedWidgetInserter(editor, objects)` (reuso estático, `:19`) para clonar
   el/los widget(s) soltados.
2. Luego `convertImagesToLocalEditor` (`:25-31`) construye un `ImageFileFactory` y llama `convertImages(widget)`
   por cada clon — recorre sus propiedades y reescribe los valores `BImage` vía `convertImageOrd`, para que la
   imagen se **copie físicamente al espacio ord de la station destino** en vez de apuntar a un origen
   remoto/original.

Como `LabelFactory` y `PictureFactory` extienden esta clase, soltar un Label o Picture (que puede llevar
imágenes embebidas) dispara este camino clonar+localizar-imágenes, no una construcción plana. La copia física
(cuando el asset es un recurso de módulo sobre `MAX_IMAGE_FILE_SIZE`, `ImageFileFactory.java:45`) se hace vía
`pxFileSpace.makeFile(...)` + `BajaFileUtil.pipe` dentro de un `BProgressDialog.Worker` (`ImageFileFactory.java:241-261`).

## 212.5 — Dispatch: sin prioridad, first-match en el controller `[CERT]`

No existe `getPriority()`/hook de orden en ninguna de las 10 clases (ausencia verificada por inspección). El
orden lo posee enteramente la lista del controller (B211: `List<WidgetFactory> factories` sembrada 8-wide,
`PxEditorController.java:79-115`) iterando con `canConvert` first-match. El único mecanismo in-factory que
afecta el match es el override de exclusión de `NavNodeFactory` (§212.3) — exclusión, no prioridad.

## 212.x — Connections

- **[Block 211]** (`PxEditorController`) — este bloque abre las 10 clases que aquel bloque sólo nombró como
  `List<WidgetFactory>`/`getWidgetInserter`; el orden/despacho first-match vive allá, la creación acá.
- **[Block 201]** (make/wizard) — `NavNodeFactory.make()` abre `BMakeWidget` y devuelve su `WidgetInserter`
  (§212.3); cierra el `WidgetInserter` que B201 dejó fuera de scope y confirma la convergencia drop↔wizard.
- **[Block 191]** (BPxEditor) — `WidgetCloningFactory` clona vía `editor.cloneWidget` (§212.4); confirma que
  el encode/decode real de clonado vive en `BPxEditor`, no en el factory.
- **[Block 210]** (bus de eventos) — la inserción de un widget nuevo por el controller es lo que dispara los
  `PxWidgetEvent`/`PxCompoundWidgetEvent` (ADDED) documentados allí; este bloque es el lado "creación", B210 el
  lado "notificación".
- **[C4 util]** — `convertImages`/`convertImageOrd` y la reescritura de props tocan utilidades transversales;
  el gap C4 (`EventUtil`/`Reflector`/`SelectedWidgets`) profundizará el plumbing compartido.
