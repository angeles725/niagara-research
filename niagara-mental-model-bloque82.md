# Bloque 82 — Diccionarios de tags OEM Honeywell deofuscados: `honTagDictionary` (HBT Ontology) + `fcTagDict` (Forge Connect) — y corrigendum al Bloque 21

> Investigación empírica de los **dos diccionarios de tags OEM** del corpus, que ATERRIZAN el framework abstracto del [Bloque 21] (Tag Framework + Haystack 4) sobre código real. Ambos extienden `BSmartTagDictionary` y, sorprendentemente, **ambos emiten el namespace `hon:`** y la ontología Honeywell `honcore:/honsmw:/honhvac:/honcorepr:`.
>
> 2 módulos: `honTagDictionary` (Honeywell corp — "HBT Ontology", auto-tagging semántico data-center/HVAC) y `fcTagDict` (Tridium — "Forge Connect Tag Dictionary", bridge topología-de-drivers → cloud).
> `honTagDictionary` traía ZKM (145 strings descifrados); `fcTagDict` decompilado limpio (vendor Tridium, sin ofuscar).
>
> Fuentes: `organized/honTagDictionary/honTagDictionary-rt/vineflower/com/honeywell/honTagDictionary/...` y `organized/fcTagDict/fcTagDict-rt/vineflower/com/tridium/fcTagDict/...` (+ `module.xml`, `module.palette`).
> Método: 2 sub-agentes + **verificación directa** de cada `extends`, del `module.xml` (identidad/vendor) y del **override de namespace** de `BFcTagDictionary`. `[CERT]` = verificado verbatim por mí; `[CERT-a]` = cita del sub-agente no re-verificada (palette, algoritmos); `[INFER]` = deducción.
>
> Capa 22 (OEM deofuscados), continúa [Bloque 81]. **ATERRIZA y corrige [Bloque 21]** (Tag Framework). Conecta [Bloque 5] (BQL/NEQL/ORD) y los drivers de [Bloques 77-78] (sobre cuya topología operan).

---

## 82.1 — Corrigendum al Bloque 21 + los dos diccionarios en conjunto `[CERT]`

El [Bloque 21] describió el framework de tag dictionaries de forma abstracta y listó `fcTagDict` por inferencia. Con el código real, **dos afirmaciones del Bloque 21 quedan refutadas** (verificadas verbatim):

| Claim Bloque 21 (línea 216) | Realidad verificada `[CERT]` | Evidencia |
|---|---|---|
| `fcTagDict` = namespace **`fc`** | Namespace **`hon`** (override hardcodeado) | `BFcTagDictionary.java:12-25`: `@NiagaraProperty(name="namespace", defaultValue="hon", override=true)` |
| `fcTagDict` = "Honeywell **Facility Commander** extensions" | "**Forge Connect** Tag Dictionary", **vendor Tridium** | `fcTagDict-rt/META-INF/module.xml`: `description="Forge Connect Tag Dictionary" vendor="Tridium" preferredSymbol="ftd"` |

> Lo que el Bloque 21 SÍ acertó `[CERT]`: ambos diccionarios custom **extienden `BSmartTagDictionary`** (no `BTagDictionary` directo) — confirmado en los dos. Sin `BSmartTagDictionary` no hay smartRules → no hay tags derivados, solo estáticos ([Bloque 21] regla clave).

**Consecuencia conceptual**: NO existe un namespace `fc:` propio en este corpus. El módulo se llama `fcTagDict` y usa el prefijo de tipos `ftd:` para sus *clases Niagara*, pero los *tags semánticos* que produce son `hon:*`. Es decir: **`honTagDictionary` y `fcTagDict` son dos productores del MISMO vocabulario `hon:`**, con enfoques distintos:

| | `honTagDictionary` | `fcTagDict` |
|---|---|---|
| Identidad | Honeywell corp — "HBT Ontology" | Tridium — "Forge Connect Tag Dictionary" |
| Symbol / vendor | `ht` / Honeywell | `ftd` / Tridium |
| Namespace de tags | `hon` (nativo) | `hon` (override) `[CERT]` |
| Clase dict | `BHonTagDictionary extends BSmartTagDictionary` (:39) `[CERT]` | `BFcTagDictionary extends BSmartTagDictionary` (:25) `[CERT]` |
| Foco | vocabulario semántico data-center/HVAC + fuzzy equipment-type | bridge físico: topología de drivers → modelo cloud |
| Tags distintivos | `BHonTypeTag`, `BEquipmentTypeTag` (fuzzy) + 60 tags `hon:*` estáticos | `BDriverSuppliedTag`, `BProtocolTag`, `BControllerNameTag`, `BPropertySuppliedTag` |
| Tags compartidos (mismo nombre, distinta impl) | `BIdTag`, `BPointLabelTag`, `BPathLabelTag`, `BCustomLookupTag`, `BCustomRelation`, `BIsPointProxyType{Rule,Condition}` | idem |
| Deps comunes | `tagdictionary-rt`, `control-rt`, `driver-rt` | + `alarm-rt`, `ndriver-rt` |

Ambos apuntan a la misma ontología Honeywell (`honcore:` core, `honhvac:` HVAC, `honsmw:` "smart...", `honcorepr:` core-primitives) y mapean el árbol Niagara (Network → Device → PointFolder/Equipment → ControlPoint/HistoryImport) a un grafo semántico para consumo cloud (Honeywell Forge / Mobile Supervisor).

---

## 82.2 — honTagDictionary: auto-tagging semántico de la HBT Ontology `[CERT]`

`BHonTagDictionary extends BSmartTagDictionary` (:39), namespace default `"hon"`, property `frozen` `[CERT]`. Identidad `[CERT]`: vendor Honeywell, `"Honeywell Tag Dictionary to generate station model based on HBT Ontology"`, symbol `ht`. Deps: `baja`, `tagdictionary-rt`, `control-rt`, `driver-rt`.

**Dos capas de contenido** `[CERT-a]`:

**A. ~60 tags `hon:*` estáticos** (vocabulario del namespace, `BSimpleTagInfo` registrados en el constructor) — claramente orientados a **data center / energía**:
- Equipos/infra: `iT`, `nonIT`, `pdu`, `ups`, `crac`, `crah`, `battery`, `generator`, `fuelCell`, `grid`, `rack`, `coldAisle`, `hotAisle`, `underFloor`, `plant`.
- KPIs de eficiencia: `pue`, `cue`, `wue`, `renewable`/`nonRenewable`, `capacity`, `demand`, `consumption`, `performance`, `uptime`/`downtime`.
- Estados/alarmas: `equipSts`, `faultSts`, `underMaintenanceSts`, `alarm`, `unAck`, `high`/`med`/`low`, `excellent`/`good`/`poor`.
- Strings: `FALname`, `locationName`, `rackNumber`, `equipName`, `position`.

**B. Tags derivados (reglas) — auto-clasificación** `[CERT]` (todos `extends BTagInfo`, verificados):
- `BHonTypeTag` (:48) — emite SIEMPRE el literal `"honhvac:Equipment"` (clasificador genérico) cuando el nodo está en posición de equipo (padre `BPointDeviceExt`/`BPointFolder` y nodo `BPointFolder`).
- `BEquipmentTypeTag` (:56) — emite el tipo **específico** (`"AHU"`, `"FCU"`, ...) desde su `lookupTable`. Propaga su `lookupTable` al `BHonTypeTag` hermano al cambiar (sincronización automática) `[CERT-a]`.
- `BIdTag` (:41) — identificador = `slotPathOrd` del componente; `validity` default `BAlways` (importado de `com.tridium.tagdictionary.condition`).
- `BPointLabelTag` (:39) — `DeviceDisplayName + sep + PointDisplayName`.
- `BPathLabelTag` (:52) — path de displayNames desde `startFromType` (default `BStation`) hasta el nodo.
- `BCustomLookupTag` (:53) — lookup configurable con 3 estrategias: `equalityCheck` / `typeCheck` (por `BTypeSpec`) / `stringValueStartWith`; emite `Tag(Id.newId(value), BMarker.MARKER)`.

**Fuzzy matching de equipment type** (`TagUtil`) `[CERT-a]` — el corazón del auto-tagging:
1. `lookupTableResult()` invierte el `BFacets` `Tipo=kw1,kw2,...` a `Map<keyword, Tipo>` (ej. `"air handling unit" → "AHU"`).
2. `findEquipmentType()` cuenta palabras del keyword contenidas (substring) en el `displayName` (lowercase); si `coincidencias/total*100 > 60%` es candidato; gana el de más coincidencias.
- Equipment types reconocidos: `AHU, FCU, VAV, RTU, Chiller, Boiler, ElectricMeter, GasMeter, ExhaustFan, Pump, WaterHeater, HotWaterSystem, ChilledWaterSystem, CoolingTower, Freezer, VRF, IDU, HeatPump, Lighting, WaterMeter, Meter`.

**Relaciones** `[CERT]`: `BCustomRelation extends BRelationInfo` (:61) construye BQL en runtime (`station:|<path>|bql:select * from <relatedObjectType>`) y agrega `BasicRelation` inbound/outbound. En la palette: `hon:hasNetwork`/`honcore:containsElement` → DeviceNetwork; `hon:hasDevice` → Device; `hon:hasEquipment`/`honcore:containsEquipment` → PointFolder; `hon:hasPoint`/`honhvac:containsProperty` → ControlPoint/HistoryImport `[CERT-a]`.

**Reglas por proxyType** `[CERT]`: `BIsPointProxyTypeRule extends BTagRule` (:26) + `BIsPointProxyTypeCondition extends BTagRuleCondition` (:41) — `test()` pasa si el `proxyExt` del `BControlPoint` `.is(proxyExtType)`. Permite tags distintos por driver (Bacnet/Modbus/...). No usadas en las palettes incluidas; disponibles para configs custom `[CERT-a]`.

**Palettes predefinidas** `[CERT-a]`: `GenericNetworkDevicePointsDictionary` (network→device→point) y `MobileSupervisorDevicePointsDictionary` (completa, con regla de equipo). El "gateway" mapea `cloudConnector:CloudConnector` → `honcore:ControlSegment` (dependencia implícita con el módulo `cloudConnector`, no declarada en module.xml).

---

## 82.3 — fcTagDict: Forge Connect — bridge topología-de-drivers → cloud `[CERT]`

`BFcTagDictionary extends BSmartTagDictionary` (:25), **namespace override a `"hon"`** `[CERT]`. Vendor Tridium, `"Forge Connect Tag Dictionary"`, symbol `ftd`. `started()` solo llama `super.started()` `[CERT-a]`.

A diferencia de honTagDictionary (que define vocabulario), fcTagDict **extrae datos de la topología física del driver** y los expone como tags `hon:`. Sus tags distintivos `[CERT]` (todos `extends BTagInfo`):
- `BDriverSuppliedTag` (:24) — lee un slot por `slotName` del `ProxyExt` (o `BHistoryImport`), solo tipo `BString`. Ej. `historyImportId` ← slot `id`.
- `BProtocolTag` (:16) — sube al `BDevice` ancestro y devuelve `parent.getParent().getType().getModule().getModuleName()` → el **nombre del módulo driver** (`"bacnet"`, `"modbus"`...).
- `BControllerNameTag` — displayName del `BDevice` ancestro.
- `BPropertySuppliedTag` — concatena N slots del ProxyExt con separador (default `.`).

Comparte con honTagDictionary (misma firma, impl propia) `[CERT-a]`: `BIdTag`, `BPointLabelTag`, `BPathLabelTag`, `BCustomLookupTag`, `BCustomRelation extends BRelationInfo`, `BIsPointProxyType{Rule,Condition}`.

**Mapeo ontológico en la palette** `[CERT-a]` (`module.palette`, reglas por tipo):
- `cloudConnector:CloudConnector` → `honType="honcore:ControlSegment"` (gateway)
- `driver:DeviceNetwork` → `honcore:LogicalController`
- `driver:Device` → `honcore:Controller`
- `control:ControlPoint` / `driver:HistoryImport` → `honsmw:Property`
- `pointSignalType` (typeCheck): `NumericPoint→honcorepr:Analog`, `BooleanPoint→honcorepr:Digital`, `EnumPoint→MultistateSignal`
- Reglas por ProxyExt: `driver:ProxyExt` (description/pointType/propertyName/location), `bacnet:BacnetProxyExt` (objectType/profileName/deviceType).

> Diferencia estructural central: fcTagDict opera sobre `ProxyExt`/`BDevice`/módulo-de-driver (**el modelo FÍSICO**), mientras honTagDictionary opera sobre nombres/jerarquía (**el modelo lógico/semántico**). Son complementarios: fcTagDict puebla los tags de identidad técnica que el cloud necesita para hablar con el dispositivo; honTagDictionary clasifica qué ES el equipo. Lexicon vacío en ambos (sin localización).

---

## 82.4 — Síntesis: el patrón de auto-tagging `hon:` para Honeywell cloud

**Qué resuelven (juntos)**: convertir una station Niagara "cruda" (drivers, devices, puntos sin semántica) en un **grafo semántico `hon:`** que los productos cloud Honeywell (Forge Connect, Mobile Supervisor) consumen sin que el integrador tagee a mano. El integrador instancia una palette (Generic o MobileSupervisor) y el `BSmartTagDictionary` deriva los tags y relaciones on-demand vía reglas — **nunca se persisten en el BOG** ([Bloque 21]).

**Cómo encaja en lo ya conocido**:
- ATERRIZA [Bloque 21]: el framework abstracto (`BSmartTagDictionary`/`BTagInfo`/`BTagRule`/`BRelationInfo`) ahora tiene dos implementaciones OEM reales leídas verbatim.
- Usa [Bloque 5] (BQL/NEQL/ORD): `BCustomRelation` lanza `bql:select * from <type>` en runtime para resolver relaciones — **ojo de performance**: con `findObjectRelationFrom=station` el scope es `slot:/` (toda la station) `[CERT-a]`.
- Opera sobre [Bloques 77-78]: los tags `BProtocolTag`/`BIsPointProxyTypeCondition` clasifican según el driver real (Bacnet/Modbus/Spyder/C-Bus...).

**Para MX60 / trabajo Honeywell**:
1. Si MX60 necesita exponer datos a un consumidor semántico (dashboard externo, cloud, Analytics), el patrón de **palette de auto-tagging** es la vía nativa — no taggear puntos a mano sino definir reglas por tipo/proxyType.
2. El **fuzzy equipment-type matching** de `TagUtil` (umbral 60% de palabras) es replicable para clasificar equipos por displayName — pero es frágil ante nombres no convencionales; documentar el diccionario de keywords es obligatorio.
3. Saber que **`fc` no es un namespace** evita un bug de queries: NEQL/BQL contra `fc:*` no devuelve nada; el namespace real es `hon:` (corrige el [Bloque 21]).

**Corrigendum aplicado**: actualizar el [Bloque 21] línea de namespaces — `fc` NO es "Honeywell Facility Commander namespace ~30 tags"; `fcTagDict` es el módulo "Forge Connect" de Tridium que **emite `hon:`**. Los 5 namespaces reales del corpus: `n` (hard-coded), `hs`/`hs4` (Haystack), `hon` (Honeywell, producido por DOS módulos: honTagDictionary + fcTagDict). `phScience` referenciado pero no shipped (sigue válido).

**Pendiente conocido**: el contenido exacto de las palettes (`module.palette`) se citó vía sub-agente `[CERT-a]` (URL-encoded `$3a`=`:`); los nombres de clase internos de honTagDictionary que seguían ZKM no aplican aquí (las clases públicas tienen nombres limpios). Dependencia implícita `cloudConnector` no declarada formalmente — confirmar si está en el corpus para un futuro bloque.
