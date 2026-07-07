# Bloque 206 — Los commands de nivel editor (`commands/`): patrón por remisión + responsive/border/apply-props

> Research del focus **`px-editor-deep`** (gap D4): el paquete `commands/` de nivel editor (14 clases) — las
> mutaciones del árbol de widgets FUERA de `studio/` (que B205 cubrió). El patrón `Command`/`Artifact` (undo/redo)
> ya está establecido (B198/B201/B205) → se cierra por REMISIÓN; este bloque documenta la SUSTANCIA NUEVA: la familia
> Insert, y los wrappers estructurales (responsive, border, apply-px-properties, goto-ord). Bloque conciso de cierre
> del grupo D. Con esto los 5 subsistemas internos de `pxEditor-wb` (D1-D5) quedan documentados.
>
> Sources (preservados §5): `sources/decompiled/pxEditor-wb/commands/` — 14 `.java` (Vineflower). Barrido inline
> 2026-07-06; token-check de clases clave literal. Method: lectura READ-ONLY del decompilado.
> Markers (§3): `[CERT]` `file:line` · `[INFER]`. Tipo: EVIDENCE block. Citas relativas a `commands/`.
>
> Capa PX (herramienta / commands). Connects [Block 205] (studio commands, patrón), [Block 198] (undo=Command),
> [Block 200] (PxProperties del template), [Block 194] (responsive/media).

---

## 206.1 — El patrón, por remisión a B205 `[CERT]`

Todos los commands de nivel editor extienden `javax.baja.ui.Command` y devuelven un `CommandArtifact` con `redo()`/
`undo()` — el MISMO contrato que los geometry commands de `studio/` (B205 §205.5) y que B198/B201 establecieron para todo
el editor PX. Confirmado literal: `Delete`/`Insert`/`Rename`/`AddBorder`/`RemoveBorder`/`AddResponsive`/`RemoveResponsive`/
`GotoOrd`/`NewWidget`/`ApplyPxPropertiesToNewWidgets` → `extends Command`; `InsertDynamic`/`InsertFrozen` → `extends Insert`
(`commands/InsertDynamic.java:9`, `InsertFrozen.java:10`). **Cierre por remisión**: el mecanismo undo/redo no agrega
sustancia sobre B205 — se remite a `[Block 205] §205.5` + "sin nueva sustancia" en el contrato. `[CERT]`

## 206.2 — La familia Insert + Delete/Rename `[CERT]`

`Insert` (`commands/Insert.java:10`, `abstract extends Command`) es la base de inserción de widgets; dos concretos:
**`InsertFrozen`** (inserta en un slot frozen/reservado del pane) e **`InsertDynamic`** (inserta como propiedad dinámica —
el caso general, el que usa el `AddGeometryTracker` de B205 al commitear geometría nueva, y el wizard Make de B201).
`NewWidget` (`commands/NewWidget.java:13`) crea un widget nuevo. `Delete`/`Rename` (`extends Command`) son las mutaciones
básicas. Todos con su `Artifact` undoable. `[CERT]`

## 206.3 — Wrappers estructurales: responsive y border `[CERT]`

Dos pares de commands ENVUELVEN widgets seleccionados en un pane contenedor (y el inverso los desenvuelve):

- **`AddResponsive`/`RemoveResponsive`** — envuelve cada widget en un **`javax.baja.ui.pane.BResponsivePane`**
  (`commands/AddResponsive.java:11,37`, `this.panes[i] = new BResponsivePane()`): el mecanismo de **responsive design** de
  PX — un pane que adapta el layout de su hijo según el form-factor/tamaño (relaciona con la capa media Wb/Hx/Mobile de
  B194). Es la única evidencia en el focus del responsive layout editable desde el editor. `[CERT]`
- **`AddBorder`/`RemoveBorder`** — envuelve en un `javax.baja.ui.pane.BBorderPane` con un `javax.baja.ui.BBorder`
  (`commands/AddBorder.java:9,14,36`): agrega un borde decorativo alrededor del widget. `[CERT]`

Ambos son el patrón "morph estructural": el Artifact guarda el widget original y el pane wrapper para poder deshacer el
envoltorio. `[INFER]` (sobre el patrón redo/undo)

## 206.4 — `GotoOrd`, `ApplyPxPropertiesToNewWidgets`, edición de propiedades `[CERT]`

- **`GotoOrd`** (`commands/GotoOrd.java:16`): navega a un ORD — resuelve `orig.get(shell.getActiveOrdTarget())` y abre la
  vista destino; loguea a `Logger.getLogger("pxEditor")` si falla. Es el "seguir hyperlink/ir a" desde el editor. `[CERT]`
- **`ApplyPxPropertiesToNewWidgets`** (`commands/ApplyPxPropertiesToNewWidgets.java:26`, `final extends Command`): aplica
  las PX custom-properties (`PxPropertyComponentArray`, B200/§200.3) a los widgets recién creados — el auto-apply de las
  propiedades expuestas del template/página a un widget nuevo, fireando `PxPropertyEvent` (`:17`). Conecta el sistema de
  PX-properties (templates B200) con la inserción de widgets. `[CERT]`
- **`EditPropertiesContext`** + **`BEditPropertiesDialog`**: el contexto/diálogo de edición de propiedades desde el editor
  (soporte del EditProperties). `[CERT]` (nombres; no abiertos en detalle)

## 206.5 — Connections

- **[Block 205]** (studio commands): mismo contrato `Command`/`Artifact`; D4 cierra el patrón por remisión y agrega los
  commands estructurales (responsive/border) y de navegación (GotoOrd) que `studio/` no tiene.
- **[Block 198]** (undo=Workbench Command): confirmado una vez más — el editor PX no tiene pila de undo propia en ninguno
  de sus paquetes de commands.
- **[Block 200]** (PxProperties del template): `ApplyPxPropertiesToNewWidgets` es el puente que aplica las PX
  custom-properties (el modelo `BConfigBinding`/`PxProperty` de los templates) a widgets nuevos en el editor.
- **[Block 194]** (media/responsive): `AddResponsive`→`BResponsivePane` es el mecanismo editable de responsive layout,
  complementando la capa de perfiles Wb/Hx/Mobile de B194.
- **Cierre del grupo D**: con D4, los 5 subsistemas internos de `pxEditor-wb` quedan documentados — D1 sidebars (B198),
  D2 studio (B205), D3 make (B201), D4 commands (B206), D5 field-editors (B202). Quedan solo módulos vecinos X4/X6.
