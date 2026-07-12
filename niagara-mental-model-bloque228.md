# Block 228 — Reflow dashboard-builder (XII): auto-binding de puntos (equipment remap / comisionamiento)

> **Qué documenta.** Cómo Reflow reconoce y liga AUTOMÁTICAMENTE los puntos de un equipo Niagara cuando el usuario
> lo agrega — la feature de comisionamiento que hace útil a Reflow en campo. Gap BG14 (reapertura grupo A). Extiende
> B219 §219.3 (que solo nombró `point-matrix.json`).
>
> **Alcance.** La tabla `point-matrix`, el template por tipo de equipo, el matcher regex, el flujo `MAP_DEVICE`, el
> override manual y la persistencia compacta del binding. NO cubre floorplans (BG15/B229).
>
> **Fuentes (primarias).** SPA beautificada (1:1 `app.4509efb4.js` sha256 `81b82b83…`; `reflowVersion "1.7.7-75"`
> `BF:14013`): `scratchpad/reflow-app.beauty.js`, citada `BF:`. `rc/point-matrix.json` (109 entradas, leído directo).
> Barrido delegado (sonnet); tokens re-verificados por el driver.
>
> **Método / markers.** `[CERT]` = fuente primaria (`BF:` 1:1 · JSON parseado). `[INFER]` = deducción.

---

## 228.1 — La tabla `point-matrix.json`: 109 puntos canónicos con regex `[CERT]`

Array de 109 entradas (`rc/point-matrix.json`). Cada una:

```jsonc
{ "point": "Discharge Air Temp",              // nombre canónico
  "vav":"secondary","fcu":"secondary","ahu":"critical","rtu":"critical","mua":"critical",
  "whp":"secondary","boiler":"n/a","chiller":"n/a","coolingTower":"n/a","vfd":"n/a",  // prioridad por tipo
  "abbreviation":"DschAirTemp",
  "type":"Numeric Read Only",                 // hint de data-kind Niagara (opcional)
  "regex":"/d(i?)?s(c?h?a?r?g?e?)?\\s?air\\s?t(e?m?)?p(erature)?(?!...s(e?t?)?\\s?p...)/gmi" }
```

**10 tipos de equipo** (columnas): `vav, fcu, ahu, rtu, mua, whp, boiler, chiller, coolingTower, vfd` `[CERT]`
(mua = Makeup Air Unit, whp = Water-source Heat Pump). Cada valor de columna es un tier
`critical | important | secondary | available | n/a`. El **`regex`** (literal JS `gmi`) es el identificador que
matchea el nombre del punto REAL en la station. Los tipos coinciden en 3 sitios: la tabla, el registro de thumbnails
`Ci` (`BF:5033`) y el array `types` del módulo equipment (`BF:5712`).

## 228.2 — El template por tipo: filtrar + priorizar → 5 grupos `[CERT]`

Servicio `points()` (`BF:4919`, `get("/nmodsreflow/point-matrix.json")`) → `filter(t,e)` (`BF:5012`) descarta las
filas con `[tipo]==="n/a"` y ordena por tier (`critical > important > secondary > available`) → `pointMapData(t,e)`
(`BF:4939`) arma, para un tipo `e` (p. ej. `"vav"`), objetos compactos `{id, displayName:point, identifier:regex}`
y los reparte en 5 grupos por defecto: **Critical / Important / Secondary / Ungrouped / Hidden** (los de tier
`available` arrancan en `Hidden`). Es decir, **la prioridad de la tabla determina qué puntos se muestran por
defecto y en qué grupo** `[CERT]`. El template se cachea por tipo en `state.equipment.types[i].points/.groups`.

## 228.3 — El matcher: clase `ua`/Identifier (regex o nombre) `[CERT]`

`ua` (`BF:10672`) envuelve el `identifier` de un punto canónico:
- Detecta `type="regex"` si empieza con `/`, si no `"name"` (match literal de nombre Niagara, con soporte
  `#`→número y `*`→wildcard).
- `regex` getter (`BF:10708`): compila `new RegExp("^"+parsed+"$", "gmi")`.
- `match(t)` (`BF:10714`): `t.toLowerCase().match(this.regex)` — el test literal contra el nombre del slot/punto real.
- Variante hash-number: `"Compressor # Command"` → `#`→`([0-9]+)` expande un punto canónico en N concretos
  (Compressor 1/2/3).

## 228.4 — El flujo de auto-bind: `MAP_DEVICE` `[CERT]`

La acción Vuex `MAP_DEVICE` (`BF:11205`) es el motor:
1. **Lee los puntos REALES del equipo**: `LOAD_NIAGARA_POINTS(ord)` (`BF:11215`) → lista los hijos por navegación
   baja/ORD → `SET_N4_CHILDREN`. Aquí se leen los slots reales de la station.
2. **Matchea**: itera los puntos del template, y por cada uno `new ua(identifier)` (`BF:11226`) y prueba
   `n4children.some(child => match(child.displayName) || match(child.name))` (`BF:11232`); **el primer match gana** y
   escribe `point.ord = child.ord` + `niagaraName`.
3. **3 modos** (según flags): `mapPoints` (device nuevo, bulk-add wizard), `ordChange:{from,to}` (el ORD del device
   cambió — re-matchea contra los puntos nuevos, con fallback a `identifierHashMap`), y default (refresh de un device
   ya mapeado).
4. Resultado a `SET_MAP` → `state.pointMapData` (staging, aún no el registro final).

**Disparadores** `[CERT]`: el **wizard bulk "agregar varios equipos"** (`BF:82808`, dispatcha `MAP_DEVICE` por
device) y el **remap de un equipo** (`EquipmentItemRemap`, `BF:84961`, `ordChange` cuando el usuario re-apunta un
equipo a otro ORD).

## 228.5 — Override manual: per-punto + lock `[CERT]`

Dos capas de corrección cuando el regex falla:
- **Per-punto** (`PointMap`, `BF:79600`): `pointOrdChanged` deja al usuario elegir un hijo específico del device, o
  abrir un modal `OrdTree` para navegar y elegir CUALQUIER ORD de la station (`isExternal:true`); commit a
  `POINT_UPDATE`, sobrescribiendo el `ord` auto-matcheado de ese punto. `resetDisplayName`/`updateDisplayName`
  permiten renombrar la etiqueta sin tocar el binding.
- **Lock** (`lockFromRemap` device / `lockRemap` tipo, `BF:84520`): un checkbox que EXCLUYE el device de los barridos
  de auto-remap masivo (`!lockFromRemap` filtra, `BF:80877`), con tooltip "This device is locked". Protege los
  bindings corregidos a mano de ser pisados por un re-scan futuro.

## 228.6 — Persistencia compacta del binding `[CERT]`/`[INFER]`

El binding NO se guarda como array completo de objetos-punto; se compacta al escribir (`we()`, `BF:3872`, llamado
por `ADD_ITEM`/`UPDATE_ITEM`): `state.equipment.items[n].points` = mapa disperso **`{pointId: ORD-relativo | null}`**
(relativizado contra el ORD del device), más mapas hermanos `displayNames` / `badges` / `externals` (solo overrides
que difieren del template). El getter `getPoints` (`BF:6186`) reconstruye el array completo on-demand mergeando el
template del tipo + los mapas guardados (con memo cache corto). **Thin** `[INFER]`: no se trazó la llamada de red
final que envía `state.equipment.items` a la JACE (la persistencia se vio hasta la capa de mutación Vuex; el
transporte cae en el mismo `save`/`config_update` de B217 `[INFER]`).

## 228.7 — Conexiones

- **[Block 219]** §219.3 — nombró `point-matrix.json` (109 puntos + regex); §228 reconstruye el flujo completo de
  consumo (template → matcher → MAP_DEVICE → persist).
- **[Block 218]** — los widgets `equipment-list`/`equipment-type`/`point-list` consumen estos bindings resueltos.
- **[Block 217]** — el binding compactado se persiste por el mismo pipeline de save (config_update/delta).
- **Hacia BG15 (B229)**: los floorplans auto-colocan "featured points" de estos equipos ya mapeados
  (`addEquipmentLabels`).
- **Hacia BG11/chihuahua**: chihuahua liga puntos por ORD-slot estable a mano (B171/B173), sin regex auto-match —
  un builder portado ganaría mucho con esta tabla `point-matrix`.
