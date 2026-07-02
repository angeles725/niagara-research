# Block 172 — chihuahua MX60 (`-wb`): herramienta Workbench BatchLinkEditor (batch-link, validate + commit atómico) — sin equivalente en Reflow

> **WHAT** — Documenta la herramienta de *engineering-time* del módulo **chihuahua** (foco): `BBatchLinkEditor`, una vista de Workbench que permite a un integrador acumular enlaces de wire-sheet heterogéneos (Link From / Link To) a través del árbol de navegación de la estación, validarlos todos con un *dry-run* (`checkLink`), y confirmarlos de forma atómica en transacciones. Esta es una capacidad **exclusiva de chihuahua** que **Reflow (B138–B155) NO tiene**: Reflow no tiene parte `-wb` (Workbench).
>
> **Sources** — alias `WB` = `chihuahua-wb/src/com/angeles/chihuahua/wb/`. Ficheros leídos de primera mano: `WB/BBatchLinkEditor.java`, `WB/model/PendingLink.java`, `WB/model/PendingLinkBuilder.java`, `WB/model/LinkSlotNameUtil.java`, `WB/model/DirectionButtonUtil.java`, `WB/model/DirectionLabelUtil.java`, `WB/model/SearchResultUtil.java`. `.env.local` **NO** leído.
>
> **Markers legend** — `[CERT]` hecho verificado contra `file:line` real; `[INFER]` deducción razonada no confirmada al 100%. Formato de marker: va **FUERA** de la cita, nunca fusionado.
>
> **Capa 26.** Continúa [Block 163].

---

## 172.1 — Qué es la herramienta

`BBatchLinkEditor` es una **vista de componente de Workbench** (`extends BWbComponentView`) `[CERT]` `WB/BBatchLinkEditor.java:69`. Su propósito es que un integrador construya, en tiempo de ingeniería, **múltiples enlaces de wire-sheet a la vez** entre las ranuras (slots) de un componente `equip` y las ranuras de componentes remotos elegidos del árbol de la estación, en lugar de arrastrar enlaces uno por uno en el wire-sheet nativo.

El flujo conceptual es acumular → validar todo → confirmar atómicamente:
- El operador selecciona una ranura del `equip` (panel izquierdo), elige una dirección (`FROM`/`TO`), navega/busca un componente remoto y una de sus ranuras, y pulsa **Accept**: se crea un `PendingLink` acumulado en una lista `[CERT]` `WB/BBatchLinkEditor.java:563`.
- Al pulsar **Save All Links**, se validan TODOS los pares con un *dry-run* y solo entonces se confirman en transacción(es) `[CERT]` `WB/BBatchLinkEditor.java:921`.

El Javadoc de la clase describe explícitamente el objetivo: "accumulate heterogeneous wire-sheet links (Link From and Link To), validate all pairs with a dry-run, and commit atomically in one operation" `[CERT]` `WB/BBatchLinkEditor.java:46-49`.

## 172.2 — Registro e invocación (agent sobre baja:Component)

La vista se registra como **agent** mediante la anotación `@NiagaraType(agent = @AgentOn(types = "baja:Component", requiredPermissions = "rwi"))` `[CERT]` `WB/BBatchLinkEditor.java:66-68`. Consecuencias:
- Al declarar `types = "baja:Component"`, la herramienta aparece en el menú **Views** (clic derecho) de **cualquier** componente de la estación `[CERT]` `WB/BBatchLinkEditor.java:52-54`.
- `requiredPermissions = "rwi"` exige permisos read-write-invoke sobre el componente para poder abrirla `[CERT]` `WB/BBatchLinkEditor.java:67`.
- El descriptor de tipo Niagara se carga por slotomatic en el init del módulo: `public static final Type TYPE = Sys.loadType(BBatchLinkEditor.class)` `[CERT]` `WB/BBatchLinkEditor.java:72`. El procesador de anotaciones genera el companion `_BBatchLinkEditor` y emite el bloque agent en `META-INF/module.xml` `[CERT]` `WB/BBatchLinkEditor.java:53-54`.

La UI completa (split-panes: lista de slots del equip, toggle FROM/TO, árbol `BNavTree`, buscador, lista de remotos, lista de pendientes, botones) se construye en `doLoadValue(BObject value, Context cx)`, guardando `equip` y `savedCx` para reutilizar el contexto en las llamadas `checkLink` `[CERT]` `WB/BBatchLinkEditor.java:204-400`.

## 172.3 — Dry-run de validación durante la edición (checkLink)

Antes de confirmar nada, la herramienta usa `checkLink` como validación **no mutante** en dos momentos:
- Al poblar la lista de ranuras remotas, `isLinkable(...)` pre-filtra: llama a `target.checkLink(source, sourceSlot, targetSlot, savedCx)` y solo muestra las ranuras cuyo `check.isValid()` es cierto, para la dirección activa `[CERT]` `WB/BBatchLinkEditor.java:547-548`. En caso de excepción inesperada, incluye la ranura en vez de ocultarla (fail-open de UI) `[CERT]` `WB/BBatchLinkEditor.java:549-551`.
- En **Accept**, tras confirmar con un `BDialog` OK/Cancel, ejecuta otro `checkLink` para capturar el motivo de invalidez (`getInvalidReason()`, con guarda contra `null`) y lo almacena en el `PendingLink` `[CERT]` `WB/BBatchLinkEditor.java:604-612`. La entrada se etiqueta `[OK]` o `[!]` en la lista de pendientes según el resultado `[CERT]` `WB/BBatchLinkEditor.java:625-626`.

La dirección (`FROM`/`TO`) determina quién es source y quién target — el componente que **posee** el `BLink` es el target `[CERT]` `WB/BBatchLinkEditor.java:538-546`.

## 172.4 — El flujo validate → commit-transaction (atomicidad y rollback)

El método clave es `doSaveValue(BObject value, Context cx)`, invocado directamente por el botón "Save All Links" (no vía `saveValue()`, que es `final` y hace no-op si `!isModified()`) `[CERT]` `WB/BBatchLinkEditor.java:372-377`. Ejecuta tres fases:

**Fase 1 — Validar TODO con `checkLink`, sin mutar.** Re-ejecuta `target.checkLink(...)` para cada `PendingLink`; si ALGUNA entrada es inválida (o una ranura ya no existe), acumula todos los motivos, los muestra en un `BDialog` y **retorna sin tocar la estación** `[CERT]` `WB/BBatchLinkEditor.java:938-983`. Esta es la **mitigación explícita de "no rollback"**: el Javadoc documenta que `SyncBuffer.commit()` aplica operaciones secuencialmente **sin rollback atómico**, por lo que hay que confirmar que TODOS los enlaces son válidos antes del primer `makeLink` `[CERT]` `WB/BBatchLinkEditor.java:884-891`.

**Fase 2 — Commit: una `Transaction` por `BComponentSpace`.** Los pendientes se agrupan por el `BComponentSpace` de su target usando un `LinkedHashMap` (preserva orden de inserción) `[CERT]` `WB/BBatchLinkEditor.java:989-1007`. Un batch mixto From+To toca dos componentes target distintos pero pueden compartir espacio; agrupar evita **transacciones anidadas**, que `Transaction.java:106` prohíbe explícitamente `[CERT]` `WB/BBatchLinkEditor.java:1021-1022`. Por cada espacio:
1. `Transaction tx = space.newTransaction(useCx)` — contexto fresco, no anidado `[CERT]` `WB/BBatchLinkEditor.java:1023`.
2. Por cada pendiente: `BLink link = target.makeLink(source, sourceSlot, targetSlot, tx)` `[CERT]` `WB/BBatchLinkEditor.java:1052`, luego `target.add(slotName, link, tx)` `[CERT]` `WB/BBatchLinkEditor.java:1068`.
3. `tx.commit()` — aplica todas las operaciones de ese espacio **atómicamente** `[CERT]` `WB/BBatchLinkEditor.java:1071`.

`makeLink` puede devolver un `BLink` o un `BConversionLink` cuando los tipos difieren pero existe adaptador; ambos son válidos para `add()` `[CERT]` `WB/BBatchLinkEditor.java:1050-1052`.

**Rollback ante fallo de commit.** Si `tx.commit()` (o cualquier op de la fase 2) lanza excepción, se captura, se muestra el error en un `BDialog` y **se preserva la lista de pendientes** para que el operador inspeccione y reintente `[CERT]` `WB/BBatchLinkEditor.java:1073-1080`. Nótese que la atomicidad es **por espacio**: cada `tx.commit()` es atómico dentro de su `BComponentSpace`, pero si hubiera varios espacios, un fallo en el segundo no revierte el primero ya confirmado `[INFER]`.

**Fase 3 — Persistir y limpiar.** En éxito, llama a `clearModified()` para bajar la bandera dirty de la vista (evita el prompt "save changes?" perpetuo, ya que el botón bypassa `saveValue()`) `[CERT]` `WB/BBatchLinkEditor.java:1091`, y limpia las listas de pendientes `[CERT]` `WB/BBatchLinkEditor.java:1093-1096`. Los links quedan persistidos por el commit; la estación los vuelca a `config.bog` en su propio ciclo BOG `[CERT]` `WB/BBatchLinkEditor.java:1085-1090`.

## 172.5 — Nombres de slot únicos en el batch (colisiones "Link")

Al añadir cada link, se genera un nombre de ranura único siguiendo la convención del `LinkCommand` nativo (`"Link"`, `"Link1"`, `"Link2"`, …) vía `LinkSlotNameUtil.generate(...)` `[CERT]` `WB/BBatchLinkEditor.java:1064-1066`. El predicado combina dos comprobaciones: `finalTarget.getSlot(n) != null` **más** un `Set<String>` de nombres ya **reservados** para ese target durante el mismo batch `[CERT]` `WB/BBatchLinkEditor.java:1057-1066`. Esto resuelve el caso en que varios FROM-links al mismo target no colisionen todos en "Link", porque `getSlot()` puede no ver ranuras añadidas bajo la transacción aún abierta `[CERT]` `WB/BBatchLinkEditor.java:1009-1014`.

## 172.6 — Búsqueda en el árbol (Find/Select, DFS sin NEQL)

La herramienta incluye búsqueda de componentes en el árbol de la estación sin usar NEQL/`BSearchService`: `searchNavTree()` resuelve la raíz desde `equip.getComponentSpace().getRootComponent()` (no `Sys.getStation()`, que es null en el proceso CLIENTE de Workbench) `[CERT]` `WB/BBatchLinkEditor.java:695-698`, y hace un DFS pre-orden `collectNavMatches(...)` con `String.contains` case-insensitive sobre `getNavDisplayName()` `[CERT]` `WB/BBatchLinkEditor.java:738-752`. Los resultados se formatean con `SearchResultUtil.formatResult(...)` `[CERT]` `WB/BBatchLinkEditor.java:718`, y **Select** resuelve el índice `[N]` vía `SearchResultUtil.parseResultIndex(...)`, revela el nodo con `navTree.expandToNavNode(...)` y carga sus ranuras remotas `[CERT]` `WB/BBatchLinkEditor.java:814-836`.

## 172.7 — Helpers puros del paquete `model/` (cada uno WSL type-a testable)

El paquete `model/` contiene lógica **pura de Java SE, sin imports de Niagara**, deliberadamente extraída para poder cubrirla con tests JUnit type-a ejecutables en WSL sin arrancar la NRE (cualquier clase que extienda `BObject` dispara `Sys.loadType()` en su static init, no booteable en WSL) `[CERT]` `WB/model/DirectionLabelUtil.java:6-11`:

- **`PendingLink`** — DTO inmutable de 6 campos `String` (sourceOrd, sourceSlot, targetOrd, targetSlot, label, validationReason); sin tipos baja `[CERT]` `WB/model/PendingLink.java:16-49`. Semántica de dirección: FROM → source=remote/target=equip; TO → source=equip/target=remote `[CERT]` `WB/model/PendingLink.java:10-14`.
- **`PendingLinkBuilder.fromDirection(...)`** — factory que resuelve la tabla de verdad de dirección (quién es source/target y quién posee el BLink) desde un string `direction` case-insensitive y los pares ORD/slot `[CERT]` `WB/model/PendingLinkBuilder.java:32-62`.
- **`LinkSlotNameUtil.generate(Predicate<String> slotExists, String base)`** — genera el primer nombre libre `base`/`base+N`; el caller inyecta el predicado que envuelve el check baja `[CERT]` `WB/model/LinkSlotNameUtil.java:30-41`.
- **`DirectionButtonUtil.indicatorFor(active, button)`** — mapea la dirección activa al marcador `▶` (activo) / `"  "` (inactivo) del toggle `[CERT]` `WB/model/DirectionButtonUtil.java:26-30`.
- **`DirectionLabelUtil.directionLabelText(direction)`** — devuelve el texto de estado ("Direction: FROM - remote feeds equip slot" / "Direction: TO - equip slot feeds remote"); cualquier valor distinto de "TO" cae en FROM `[CERT]` `WB/model/DirectionLabelUtil.java:32-35`.
- **`SearchResultUtil.formatResult(idx, name, path)` / `parseResultIndex(item)`** — formatea `"[N] name — path"` (N 1-based) y parsea el prefijo `[N]` a índice 0-based, devolviendo `-1` en cualquier fallo (no-op seguro) `[CERT]` `WB/model/SearchResultUtil.java:29-61`.

## 172.8 — Nota `[INFER]` sobre persistencia

El Javadoc de la clase menciona una fase histórica que llamaba `setModified()` y anota `[INFER]` — "confirm at smoke that setModified() alone is sufficient for persistence" `[CERT]` `WB/BBatchLinkEditor.java:910-911`. La implementación final resuelta hace lo contrario: `clearModified()` tras un commit exitoso, ya que `tx.commit()` ya persistió al espacio vivo y la estación vuelca a `config.bog` por su propio ciclo `[CERT]` `WB/BBatchLinkEditor.java:1082-1091`. El `[INFER]` del Javadoc quedó obsoleto por la decisión final `[INFER]`.

## 172.x — Connections

- **[Block 163]** — Este bloque documenta la parte **`-wb`** de la estructura tri-partita de chihuahua (backend + frontend/UX + Workbench). `BBatchLinkEditor` es la única pieza de tiempo-de-ingeniería (Workbench desktop), distinta de las capacidades runtime documentadas en los otros bloques de chihuahua.
- **Reflow (B138–B155) — contraparte de comparación, net-add.** Reflow **NO tiene parte Workbench**: no existe un módulo `-wb` ni una vista `BWbComponentView` en Reflow. La capacidad de batch-linking en tiempo de ingeniería (acumular pares heterogéneos, validar con dry-run `checkLink`, y commit atómico por `BComponentSpace`) es una **net-add exclusiva de chihuahua** sin equivalente en Reflow. Cualquier comparación de features chihuahua↔Reflow debe registrar esta como gap **C10** a favor de chihuahua.
