# Floorplans Domain — reflow-frontend/src/components/floorplans/

**Files**: 52 Vue components  
**Fidelity**: GOOD (78% per GAP-ANALYSIS — algunos edge cases inferidos, no verificados contra source of truth)  
**Element types**: image, icon, label, button, polygon/zone, arrow, text  

---

## 1. Overview

El dominio `floorplans` implementa un editor SVG interactivo de planos de planta para edificios. El flujo completo incluye: (a) un índice de edificios y pisos (`FloorPlans.vue`, `FloorGrid.vue`, `FloorsFull.vue`, `FloorsCompact.vue`), (b) un canvas de lectura en tiempo real (`FloorPlan.vue` → `FloorPlanCanvas.vue`) que renderiza elementos SVG enlazados a puntos Niagara vivos, y (c) un editor completo (`FloorPlanEditor.vue` → `CanvasPane.vue` + `PropsPane.vue` + toolbars) donde el usuario posiciona, redimensiona y configura elementos mediante drag, handles de resize (`Resizer.vue`) y paneles de propiedades por tipo. Los binding de puntos se realizan via `$niagara` plugin; las imágenes de fondo se persisten en el backend via `BReflowFileCommands`.

---

## 2. Entry Points

| Archivo | LOC | Rol |
|---------|-----|-----|
| `FloorPlans.vue` | 57 | Index page — lista todos los pisos de un edificio con navegación |
| `FloorPlan.vue` | 60 | Root del modo lectura — orquesta la vista read-only de un piso |
| `FloorPlanEditor.vue` | 535 | Root del modo edición — wrappea canvas, toolbars y side panels |
| `FloorPlanCanvas.vue` | 883 | Canvas read-only con elementos vivos (data-bound) |
| `CanvasPane.vue` | 646 | Canvas interactivo principal — drag/drop, selección, resize |

---

## 3. Components (grouped)

### 3.1 Canvas / Root (~7)

| Archivo | LOC | Fidelidad | Propósito |
|---------|-----|-----------|-----------|
| `FloorPlan.vue` | 60 | GOOD | Orquesta la vista read-only de un piso |
| `FloorPlanEditor.vue` | 535 | GOOD | Editor completo con canvas, toolbars y panels |
| `FloorPlanCanvas.vue` | 883 | GOOD | Canvas read-only con live data bindings |
| `CanvasPane.vue` | 646 | GOOD | Canvas interactivo principal (edit mode) |
| `FloorPlanPx.vue` | 84 | GOOD | Wrapper de unidades pixel con helpers de conversión de coordenadas |
| `CanvasForm.vue` | 417 | GOOD | Form para editar propiedades del canvas (fondo, dimensiones) |
| `Resizer.vue` | 111 | GOOD | Drag handles para redimensionar el elemento seleccionado |

### 3.2 Elements — por tipo (~9)

| Archivo | LOC | Fidelidad | Propósito |
|---------|-----|-----------|-----------|
| `ElementImage.vue` | 119 | GOOD | Renderiza un elemento imagen en el canvas |
| `ElementIcon.vue` | 126 | GOOD | Renderiza un elemento ícono en el canvas |
| `ElementLabel.vue` | 130 | GOOD | Label data-bound mostrando valor de punto en vivo |
| `ElementButton.vue` | 131 | GOOD | Botón clickeable colocado en el canvas |
| `ElementText.vue` | 118 | GOOD | Texto estático o editable en el canvas |
| `ElementSVGArrow.vue` | 543 | GOOD | Flecha SVG redimensionable en el canvas |
| `ElementSVGPolygon.vue` | 866 | GOOD | Polígono/zona SVG redimensionable en el canvas |
| `ArrowShape.vue` | 92 | GOOD | Shape visual SVG de una flecha (sub-renderer) |
| `ImageDisplay.vue` | 114 | GOOD | Imagen de fondo del plano con tint y opacity |

### 3.3 Properties Panes (~14)

| Archivo | LOC | Fidelidad | Propósito |
|---------|-----|-----------|-----------|
| `BasePane.vue` | 240 | GOOD | Base abstracta con layout y comportamiento compartido de side-panels |
| `PropsPane.vue` | 211 | GOOD | Panel derecho contextual — muestra panel según elemento seleccionado |
| `ElementsPane.vue` | 287 | GOOD | Panel lateral con lista de todos los elementos, selección y visibilidad |
| `ArrowProperties.vue` | 104 | GOOD | Propiedades del elemento flecha |
| `ButtonProperties.vue` | 74 | GOOD | Propiedades del elemento botón |
| `GroupProperties.vue` | 39 | GOOD | Propiedades de un grupo de elementos seleccionados |
| `IconProperties.vue` | 81 | GOOD | Propiedades del ícono (incluye icon picker) |
| `ImageProperties.vue` | 96 | GOOD | Propiedades del elemento imagen (URL, sizing, alt) |
| `LabelProperties.vue` | 121 | GOOD | Propiedades del label (formato, precisión, unidades) |
| `TextProperties.vue` | 99 | GOOD | Propiedades del texto (contenido, alineación) |
| `ZoneProperties.vue` | 120 | GOOD | Propiedades de zona/polígono (nombre, equipamiento vinculado) |
| `PositionSize.vue` | 111 | GOOD | Inputs numéricos para x/y y width/height precisos |
| `LabelBindings.vue` | 74 | GOOD | Editor de binding de label a punto de datos o expresión |
| `ElementsItem.vue` | 265 | GOOD | Fila individual en el panel de elementos |

### 3.4 Style Editors (~6)

| Archivo | LOC | Fidelidad | Propósito |
|---------|-----|-----------|-----------|
| `ButtonStyle.vue` | 114 | GOOD | Editor de estilo visual del botón |
| `IconStyle.vue` | 146 | GOOD | Editor de color y tamaño del ícono |
| `LabelStyle.vue` | 146 | GOOD | Editor de fuente, color y fondo del label |
| `TextStyle.vue` | 105 | GOOD | Editor de fuente, tamaño y color del texto |
| `ZoneStyle.vue` | 96 | GOOD | Editor de fill, stroke y opacity de zona/polígono |
| `ImageTint.vue` | 75 | GOOD | Aplica overlay de tint al fondo del plano |

### 3.5 Tabs / Tools (~9)

| Archivo | LOC | Fidelidad | Propósito |
|---------|-----|-----------|-----------|
| `TabView.vue` | 86 | GOOD | Switcher entre paneles Properties / Style / Actions / States |
| `ActionsTab.vue` | 70 | GOOD | Panel de acciones disponibles para el elemento seleccionado |
| `DynamicColorTab.vue` | 93 | GOOD | Configuración de bindings de color dinámico por valores de punto |
| `StatesItem.vue` | 244 | GOOD | Fila de estado individual en editor de color dinámico o state-binding |
| `ToolbarTop.vue` | 369 | GOOD | Toolbar superior: insertar elementos, undo/redo, guardar, exportar |
| `ToolbarRight.vue` | 277 | GOOD | Toolbar derecho: zoom, toggle de capas |
| `ClipboardToolbar.vue` | 92 | GOOD | Botones de clipboard (cut, copy, paste) |
| `ZoomLevelPicker.vue` | 134 | GOOD | Selector de nivel de zoom del canvas |
| `ActionPoptipStub.vue` | 28 | GOOD | Stub placeholder del poptip de acciones en el editor |

### 3.6 Floor Management (~7)

| Archivo | LOC | Fidelidad | Propósito |
|---------|-----|-----------|-----------|
| `FloorPlans.vue` | 57 | GOOD | Index page listando todos los planos de un edificio |
| `FloorGrid.vue` | 249 | GOOD | Vista grid de pisos con thumbnails |
| `FloorsFull.vue` | 456 | GOOD | Vista full-page de todos los pisos con detalle expandido |
| `FloorsCompact.vue` | 419 | GOOD | Vista compacta de pisos en sidebar o widget |
| `FloorForm.vue` | 288 | GOOD | Form de creación/edición de metadatos de piso (nombre, nivel, imagen) |
| `FloorEquipment.vue` | 73 | GOOD | Lista de equipamiento asociado a un piso específico |
| `FloorAddMultiple.vue` | 120 | GOOD | Diálogo para agregar múltiples niveles de piso en bulk |

---

## 4. Cross-references

- **Mixins**: `canvasDragResizeMixin` (drag + resize de elementos en canvas), `elementMixin` (comportamiento base compartido por todos los Element*.vue)
- **Stores**: módulo Vuex `floorplans` (estado de canvas, elementos, selección, zoom, clipboard)
- **Point subscriptions**: `$niagara` plugin — `ElementLabel.vue` y `DynamicColorTab.vue` suscriben a valores de puntos en tiempo real
- **Backend file storage**: `BReflowFileCommands` (Java backend) — usado por `CanvasForm.vue` y `FloorForm.vue` para upload/retrieve de imágenes de fondo
- **Color utilities**: plugin de color o util compartido referenciado por `IconStyle.vue`, `LabelStyle.vue`, `ZoneStyle.vue`, `ImageTint.vue`
- **Router**: rutas hacia `FloorPlans.vue` y `FloorPlanEditor.vue` en el router principal Vue
- **ElementsPane.vue** ↔ **CanvasPane.vue**: bidireccional — selección en lista refleja en canvas y viceversa

---

## 5. Notes & Gotchas

- **Fidelidad 78%**: todos los entries son GOOD según el index, pero la cobertura de detalle (props, emits, mixins concretos) es parcialmente inferida desde GAP-ANALYSIS, no verified contra código fuente real — los archivos `.vue` no existen en el repo de análisis.
- **SVG vs imagen**: `FloorPlanCanvas.vue` (883 LOC) y `ElementSVGPolygon.vue` (866 LOC) son los componentes más complejos; manejan hit-testing SVG, path editing de polígonos y coordinate transforms.
- **ActionPoptipStub.vue** (28 LOC): stub deliberado — no es un componente incompleto sino un placeholder que evita romper el árbol cuando `ActionPoptip` real no está disponible en el contexto.
- **Pattern de estilo separado**: cada tipo de elemento tiene su `*Properties.vue` (lógica/data) y su `*Style.vue` (apariencia) por separado — convención consistente excepto en `ArrowProperties.vue` que parece unificar ambas.
- **BasePane.vue** (240 LOC): ancestro abstracto de todos los paneles laterales; cambios aquí afectan todos los `*Pane.vue` y `*Properties.vue`.
- **FloorPlanPx.vue** (84 LOC): wrapper de conversión de coordenadas — crítico para que los elementos pixel-based se rendericen correctamente en diferentes zoom levels.
- **Counts vs explore**: el explore estimó 47 components, el index real tiene 52 — la diferencia son los `*Style.vue` y los componentes de floor management que no se contabilizaron en el estimate inicial.
