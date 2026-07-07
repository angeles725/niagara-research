# Block 214 — Los field-editors de converters del PX Editor: `com.tridium.px.editor.fieldeditors` (3): BIEnumToSimpleFE, BINumericToSimpleFE, BIStatusToSimpleFE

> Research de la **INFRAESTRUCTURA de `pxEditor-wb`** — foco `px-editor-core`, gap **C5** (último). Documenta
> los 3 field-editors que editan el PAYLOAD de un converter en la celda del property-sheet:
> `BIEnumToSimpleFE`, `BINumericToSimpleFE`, `BIStatusToSimpleFE`. Corrige la suposición de nomenclatura (el
> `BI...` NO marca interfaz aquí: son clases concretas `BWbFieldEditor`). Distingue esta capa del
> `BConverterCE` (B198, que ELIGE el tipo de converter) y de `EventUtil` (B210, que clasifica los eventos).
>
> Sources (decompilado Vineflower, READ-ONLY):
> `/home/cristian/modules/Prototipos/modulos/organized/pxEditor/pxEditor-wb/vineflower/com/tridium/px/editor/fieldeditors/`
> (citas `file:line` relativas: `BIEnumToSimpleFE.java:67` = `.../fieldeditors/BIEnumToSimpleFE.java`).
> Method: lectura directa del decompilado (3 clases).
> Markers (canónico METHODOLOGY §3): `[CERT]` fuente primaria local (`file:line`) · `[INFER]` deducción.
>
> Capa infraestructura pxEditor-wb (núcleo). Connects [Block 198] (`BConverterCE` — elige el tipo),
> [Block 202] (kitPx field-editors — otro set, misma base `BWbFieldEditor`), [Block 210] (`EventUtil`/
> `BConverter.TYPE` — clasifica los eventos de cambio de converter).

---

## 214.1 — Los 3 son clases CONCRETAS `BWbFieldEditor` (el `BI...` NO es marca de interfaz) `[CERT]`

Pese al patrón de nombre `BI...FE` (que en otras partes del corpus marca un `BInterface` de Niagara), los 3
son **clases concretas que extienden `BWbFieldEditor`** directo — sin indirección de interfaz:

```java
// BIEnumToSimpleFE.java:67
public class BIEnumToSimpleFE extends BWbFieldEditor {
// BINumericToSimpleFE.java:46
public class BINumericToSimpleFE extends BWbFieldEditor {
// BIStatusToSimpleFE.java:34
public class BIStatusToSimpleFE extends BWbFieldEditor {
```

El `I` del nombre **NO** es el marker de interfaz Niagara aquí — hace eco del *converter-interface* que cada
uno edita (`IEnumToSimple`/`INumericToSimple`/`IStatusToSimple`, §214.2). Cada clase tiene su `TYPE` +
constructor + cuerpo UI completo, no un solo método abstracto. La base común es
`javax.baja.workbench.fieldeditor.BWbFieldEditor` (imports `:55`/`:34`/`:27`) — la **base FE bajaux/Workbench
genérica**, NO una px-editor-specific; misma base que los kitPx field-editors de B202.

## 214.2 — Registro por `@AgentOn` sobre el INTERFAZ del converter `[CERT]`

Cada FE se registra como agent sobre el **tipo interfaz** del converter (aplica a cualquier converter concreto
que implemente ese contrato, no a una clase hardcodeada):

| FE | `@AgentOn(types=…)` | Tipo runtime casteado | Cita |
|---|---|---|---|
| `BIEnumToSimpleFE` | `converters:IEnumToSimple` | `(BIEnumToSimple)loadValue` | reg `BIEnumToSimpleFE.java:57-61`, cast `:136` |
| `BINumericToSimpleFE` | `converters:INumericToSimple` | `(BINumericToSimple)value` | reg `BINumericToSimpleFE.java:36-40`, cast `:119` |
| `BIStatusToSimpleFE` | `converters:IStatusToSimple` | `(BIStatusToSimple)value` | reg `BIStatusToSimpleFE.java:29-33`, cast `:60` |

Es el mecanismo estándar de agent-registration de Niagara: bindea un subtipo `BWbFieldEditor` a un `Type`
(acá el interfaz del converter), de modo que el property sheet de Workbench auto-selecciona el FE correcto
para un slot tipado como ese converter.

## 214.3 — El contrato FE: `doLoadValue`/`doSaveValue` + sub-widgets por familia `[CERT]`

Los 3 overridan los mismos dos hooks protegidos (el contrato load/save de `BWbFieldEditor`):

| FE | `doLoadValue` | `doSaveValue` | Data type | Cita |
|---|---|---|---|---|
| Enum | `:134` | `:188` | `BEnumToSimpleMap` (`getMap`/`setMap`) | `BIEnumToSimpleFE.java:134,188,137,223` |
| Numeric | `:118` | `:165` | `BNumericToSimpleMap` | `BINumericToSimpleFE.java:118,165,120,181` |
| Status | `:59` | `:96` | 9 `BSimple` discretos (uno por bit) | `BIStatusToSimpleFE.java:59,96` |

Sub-widgets, uno distinto por familia de converter:

- **Enum→Simple**: un `BTable` (`:72`) con inner `Model`/`CellRenderer`/`Controller` (`:234-360`); filas = ordinales/tags del `BEnumRange` (`rangeOrdinals`/`rangeTags`, `:138-149`), doble-clic en la celda editable abre `BWbFieldEditor.dialog(...)` (`:315`) para elegir el `BSimple` mapeado; más un `defTable`/`defaultCE` (`BWbCellEditor`, `:181`) para el default; comandos `SetValues`/`ClearValues` (`:280-299,362-398`) aplican bulk a filas seleccionadas.
- **Numeric→Simple**: un `BCellTable` (`:75`) con 3 columnas editables `min`/`max`/`value` (cada celda `BDoubleCE` o `BWbCellEditor.makeFor`); comandos `Add`/`Remove`/`Up`/`Down` (`:236-346`) y un `SyncRanges` `ToggleCommand` (`:313`) que auto-encadena `max[i]→min[i+1]` vía `checkSync()` (`:199`).
- **Status→Simple**: nueve `BWbCellEditor`, uno por bit de status — `alarm`/`disabled`/`fault`/`down`/`stale`/`overridden`/`nullStatus`/`unackedAlarm`/`ok` (`:39-47`), cargados en `doLoadValue` (`:61-78`), en un `BLabeledCellTable` (`:38`, `initTable()` `:153`). Caso especial: si `getAlarm()` es un `BBrush` (converter status→color), agrega botones "useFg"/"useBg" (`:80-93`, métodos `:110-151`) que cargan cada slot desde `BStatus.alarmFg/alarmBg` — o sea el converter puede mapear bits de status a COLORES, no sólo simples.

Ninguno define contratos marker de un método — cada uno es un FE concreto completo.

## 214.4 — Relación con la maquinaria FE/CE genérica `[CERT]`

- **Base**: `BWbFieldEditor` (base FE estándar de Workbench, no px-editor-specific).
- **Delegación de celdas**: los 3 delegan cada valor de celda a `javax.baja.workbench.celleditor.BWbCellEditor`
  vía la factory `BWbCellEditor.makeFor(...)` (`BIEnumToSimpleFE.java:181`, `BINumericToSimpleFE.java:141,158`,
  `BIStatusToSimpleFE.java:61-69`) — el dispatch estándar cell-editor-por-tipo-de-BSimple, reusado no
  reimplementado.
- **Único acoplamiento px-editor**: los 3 importan `com.tridium.px.editor.BPxEditorPane` SÓLO para lookups de
  texto/lexicon (`BPxEditorPane.text(...)`/`lexicon()`, `:3` en los 3) — la base/contrato FE es bajaux genérico;
  sólo los strings localizados son px-editor-scoped.

## 214.5 — Distinción de capas: CE elige el tipo, FE edita el payload, EventUtil clasifica `[INFER]`

Ninguno de los 3 referencia `BConverter`, `BConverterCE` (B198) ni `EventUtil` (B210) directo (sin import ni
símbolo). El vínculo es **estructural, no de código** — tres capas complementarias NO solapadas `[INFER]`:

1. **`BConverterCE`** (B198) — el **cell-editor** que presenta/elige QUÉ converter usar en la celda (p.ej.
   `EnumToSimple` vs `NumericToSimple` para un slot).
2. **estos 3 `...FE`** — los **field-editors** invocados una vez que un converter de una familia ya está
   elegido: editan el DATO INTERNO del converter (`BEnumToSimpleMap`, `BNumericToSimpleMap`, o los 9
   `BSimple` de status), NO la elección de tipo.
3. **`EventUtil.getEventType`** (B210 §210.6) — clasifica genéricamente los eventos de cambio de slot
   `BConverter.TYPE` (converter add/change/remove); no referencia estos FE ni ellos a él.

## 214.x — Connections

- **[Block 198]** (`BConverterCE`) — la capa de ARRIBA: elige el tipo de converter; estos FE editan su payload
  una vez elegido (§214.5). Cierra la pregunta "cómo se edita un converter en la celda" que B198 rozó.
- **[Block 202]** (kitPx field-editors) — otro set de FE, MISMA base `BWbFieldEditor` (§214.1); confirma que la
  base FE es transversal bajaux, no duplicada por subsistema.
- **[Block 210]** (`EventUtil`/`BConverter.TYPE`) — el cruce que EventUtil hace a converters clasifica los
  eventos que los CAMBIOS hechos por estos FE producen; capa de eventos, no de edición (§214.5).
