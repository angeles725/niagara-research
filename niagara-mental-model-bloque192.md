# Bloque 192 — Catálogo de widgets bajaui: inputs, contenedores, datos

> Research del focus **`px-editor`** (gap E2): el catálogo de widgets `bajaui` que un `.px` puede contener,
> MÁS ALLÁ de Label/Button/panes ya documentados (B182). Cubre inputs (checkbox/radio/toggle/textfield/slider),
> contenedores (tabbed/split/scroll) y datos (table/tree/list), con sus propiedades clave y el límite del
> catálogo (charts NO están en bajaui core). NO cubre bindings (E3).
>
> Sources (preservados §5): `sources/decompiled/bajaui-wb-widgets/` — `BAbstractButton`, `BToggleButton`,
> `BSlider`, `BTextField`, `BTabbedPane`, `BTable` (source original Tridium, `bajaui-wb`, `javax.baja.ui`).
> Barrido delegado (sonnet) 2026-07-06.
> Method: lectura READ-ONLY del decompilado. Markers (§3): `[CERT]` `file:line` · `[INFER]`.
>
> Capa PX (widgets). Connects [Block 36] (kitPx widgets), [Block 182] (panes), [Block 191] (paleta del editor).

---

## 192.1 — Jerarquía de botones: `BAbstractButton → BToggleButton → BCheckBox/BRadioButton` `[CERT]`

`BAbstractButton` (`BAbstractButton.java:68`, extends `BLabel`) es la base compartida: aporta
`focusTraversable` (`:86`), `buttonStyle` (`:115`) y la action `invokeAction` (`:65-67`). `[CERT]`

- `BToggleButton` (`BToggleButton.java:33-34`, extends `BAbstractButton`): botón de dos estados; prop
  `selected` (boolean, default false) — `BToggleButton.java:51`. `[CERT]`
- `BCheckBox` (extends `BToggleButton`): hereda `selected`, agrega `halign` (alineación del label vs la caja)
  — `BCheckBox.java:50`. `[CERT]`
- `BRadioButton` (extends `BToggleButton`): misma forma; la exclusividad de grupo NO es una propiedad, se
  maneja con `ToggleCommandGroup`. `[CERT]`

## 192.2 — Inputs: `BTextField`, `BSlider` `[CERT]`

| Widget | Extends | Props clave | Cita |
|---|---|---|---|
| `BTextField` | `BTextEditor` | `visibleColumns` (int, def 20), `expandHeight` (boolean); el texto lo maneja el `TextModel` heredado (`editable` en `BTextEditor.java:154`), NO un slot | `BTextField.java:80,109` |
| `BSlider` | `BWidget` (no botón) | `min` (0), `max` (100), `increment` (1), `value` (50), `orientation` | `BSlider.java:126,152,179,207,233` |

`[CERT]` Nota: `BSlider` extiende `BWidget` directo (no la jerarquía de botones). `[CERT]`

## 192.3 — Contenedores: `BTabbedPane`, `BSplitPane`, `BScrollPane` `[CERT]`

- `BTabbedPane` (`pane/BTabbedPane.java:91-92`, extends `BLabelPaneContainer`): tab strip. Props
  `tabPlacement` (`BAlign`, def top, `:110`), `showSingleTab` (`:140`), `paintFullBorder` (`:193`). Los tabs
  se agregan programáticamente con `addPane(String label, BWidget content)` (`pane/BLabelPaneContainer.java:63`). `[CERT]`
- `BSplitPane` (`pane/BSplitPane.java:103-104`, extends `BPane`): props `widget1`/`widget2` (`:119,145`),
  `dividerPosition` (`:228`), `orientation` (`:260`), `moveableDivider` (`:198`). `[CERT]`
- `BScrollPane` (`pane/BScrollPane.java:103-104`, extends `BPane`): props `content` (`:120`), `hpolicy`/`vpolicy`
  (`BScrollBarPolicy`, def asNeeded, `:205,234`). `[CERT]`

**Ausencias verificadas** `[CERT]`: NO existe `BZoomPane` ni un dashboard/report pane en este árbol de source;
el paquete `pane/` se completa con `BExpandablePane`, `BResponsivePane`, `BToolPane`, `BTransformPane`,
`BTreePane`. (Los `DashboardPane`/`ReportPane` mencionados en [Block 13] para N4.10 viven en OTRO módulo, no en
bajaui core — scope, no refutación.) `[INFER]`

## 192.4 — Datos: `BTable`/`BTree`/`BList` se alimentan por MODELO, no por props `.px` `[CERT]`

Los tres extienden `BTransferWidget` y reciben su data por un objeto **modelo**, NO por atributos `.px`: `[CERT]`

| Widget | Modelo | setModel | Props display |
|---|---|---|---|
| `BTable` | `TableModel` | `table/BTable.java:820` | `multipleSelection` (`:253`), `headerVisible` (`:281`), `hgridVisible`/`vgridVisible` (`:334,363`) |
| `BTree` | `TreeModel` | `tree/BTree.java:235` | `multipleSelection` (`:88`) |
| `BList` | `ListModel` | `list/BList.java:325` | `multipleSelection` (`:120`); `BCheckList` agrega checkbox por ítem |

**Consecuencia de authoring** `[INFER]`: NO se puede poblar una tabla/árbol con atributos en el `.px` — se
llena por binding (p.ej. `TableBinding`) o programáticamente. Es un límite duro para el menú/dashboards de datos.

## 192.5 — Boundary: charts/gauges/meters NO están en bajaui core `[CERT]`

Un `find` sobre todo `javax/baja` por `*chart*/*gauge*/*meter*` no devuelve nada `[CERT]`: los medidores y
gráficos viven en módulos APARTE (`kitPx` — `BAnalogMeter`/`BBargraph`, [Block 36]; `webChart`). El catálogo
`bajaui` core es el de widgets base; lo "rico" (charts) es kitPx/webChart. `[INFER]`

## 192.x — Connections

- **[Block 36]** — catálogo kitPx: los widgets "ricos" (meters/bargraph) que §192.5 marca fuera de bajaui core.
- **[Block 182]** — panes de layout: `BTabbedPane`/`BSplitPane`/`BScrollPane` son contenedores que complementan los panes de layout.
- **[Block 191]** — la paleta del editor ofrece estos widgets como prototipos.
- **[Block 13]** — lista de widgets N4.10 (DashboardPane/ReportPane): §192.3 aclara que esos no están en bajaui core.
- **E3** (próximo) — los bindings kitPx que animan estos widgets (Action, ButtonGroup, SetPoint…).
