# Bloque 261 — Tags (II): el motor del diccionario — el `n:` built-in, los índices y el import/export

> **Qué documenta**: la implementación privada del diccionario de tags — `com.tridium.tagdictionary` raíz (5),
> `tag/` (14) y `util/` (5): de dónde sale el diccionario `Niagara`, los **tags computados**, los dos índices
> de resolución, y el formato de import/export. Cierra el gap **T2** del focus `tags`.
>
> **Alcance**: 24 clases + `scope/BOrdScope` (aparecida en el barrido). Fuera: `condition/`, `neqlize/`,
> `relation/` (gaps T3, T4).
>
> **Fuentes** (decompilado vineflower):
> - `$C` = `…/tagdictionary/tagdictionary-rt/vineflower/com/tridium/tagdictionary/`
> - `$T` = `…/tagdictionary/tagdictionary-rt/vineflower/javax/baja/tagdictionary/`
> - Raíz: `/home/cristian/modules/Prototipos/modulos/organized/`
>
> **Método**: barrido delegado (tier `sonnet`) + verificación inline: **9 tokens** re-verificados y **2
> afirmaciones del barrido corregidas** (§261.7) — ambas eran sobre permisos, es decir el tipo de claim que
> más caro sale equivocar. Marcadores: `[CERT]` = fuente primaria; `[INFER]` = deducción. Bloque de EVIDENCIA.
> *(Disciplina aplicada tras la nota de método de [Bloque 260]: más cita, menos glosa.)*

---

## 261.1 — El diccionario `Niagara` sale de un `.bog` embebido `[CERT]`

```java
bogFile = (BBogFile)BOrd.make("module://" + moduleName + "/bog/Niagara.bog").resolve().get();
```
`$C/BNiagaraTagDictionary.java:175-177` `[CERT]`

`BNiagaraTagDictionary extends BSmartTagDictionary` y **no** tiene su contenido compilado: lo lee de
`module://<módulo>/bog/Niagara.bog` al arrancar (`started()` → `importFromBog(null)`,
`$C/BNiagaraTagDictionary.java:154-157`) `[CERT]`.

Lo que sí está compilado son los **`Id` como constantes estáticas** (`:98-143`) `[CERT]`: 35 tags del namespace
`n` — entre ellos `point`, `device`, `network`, `history`, `input`, `output`, `station`, `type`, `uuid`,
`schedule`, `template`, y los 8 `geoAddr*` — y **8 relaciones**: `child`, `parent`, `childPoint`,
`childNullProxyPoint`, `parentDevice`, `childDevice`, `parentNetwork`, `tagGroup`.

**`TAG_GROUP_RELATION` es concretamente** `Id.newId("n", "tagGroup")` (`:143`) `[CERT]`. `[INFER]` Con esto se
cierra el mecanismo que [Bloque 260] §260.4 dedujo desde la API: un componente "se une" a un grupo de tags
teniendo una relación `n:tagGroup` cuyo endpoint apunta al `BTagGroupInfo`.

## 261.2 — Los tags computados: 14 clases que son código, no datos `[CERT]`

El paquete `tag/` no contiene definiciones de tags sino **tags cuyo valor lo calcula Java en el momento**.
Todos extienden `BTagInfo`:

| Familia | Clases | Qué computan |
|---|---|---|
| Historia | `BAbstractHistoryTag` (base), `BHistoryMarkerTag`, `BHistoryIdTag` | marcador si el punto tiene history · el id de la history |
| Naturaleza del punto | `BInputTag`, `BOutputTag` | marcador según `BControlPoint` + `BIWritablePoint` + presencia de proxyExt |
| Capacidades | `BAlarmablePointMarkerTag`, `BHasPxViewTag` | marcador si hay `BAlarmSourceExt` · si hay `BAbstractPxView` (slot o registro de agentes) |
| Identidad | `BNameTag`, `BDisplayNameTag`, `BTypeTag`, `BStationTag`, `BOrdInSessionTag` | nombre, display name, tipo, station, ORD de sesión |
| Propagación | `BScopedTag` | propaga un valor **caminando los ancestros** |
| Grupos | `BTagGroupNameTag` | devuelve el `Id` de un grupo al expandirlo |

`[INFER]` Es la respuesta a una pregunta que [Bloque 21] no respondía: los tags "obvios" de una station
(`n:point`, `n:history`, `n:input`) **no están almacenados en ningún lado** — se derivan del componente en cada
consulta. Eso explica por qué el subsistema necesita índices (§261.3) para no recalcular todo cada vez.

## 261.3 — Dos índices distintos, ninguno acotado `[CERT]`

| Índice | Estructura | Clave | Sincronización | Cap / evicción |
|---|---|---|---|---|
| `TagRuleIndex` | `Map<Id, Set<TagRule>>` sobre `ConcurrentHashMap` (`$C/TagRuleIndex.java:16`) | `Id` de tag | `compute()` atómico por entrada | **ninguna** |
| `EntityTagIndex` | `HashMap<BOrd, TagInfo[]>` + `ConcurrentHashMap<Id,Integer>` + `WeakHashMap` de deduplicación (`$C/util/EntityTagIndex.java:22-24`) | `BOrd` de la entidad | `synchronized(tagInfoIndex)` en lectura y escritura | **ninguna** (salvo las `WeakReference` del deduplicador) |

`[CERT]` `EntityTagIndex` usa un centinela `NOT_PRESENT = new BSimpleTagInfo(BMarker.DEFAULT)` (`:25`) para
distinguir "calculado y ausente" de "no cacheado".

`[CERT]` Agregar o quitar un `Id` del índice **redimensiona todos los arrays existentes**:
`addIdToIndex()`/`removeIdFromIndex()` (`:130-160`).

`[INFER]` Dos consecuencias para una station grande: (a) el índice de entidades **crece sin techo** con la
cantidad de componentes indexados — no hay cap, ni TTL, ni evicción; (b) cambiar el conjunto de tags indexados
cuesta O(n entidades), no O(1).

## 261.4 — Import/export: JSON y CSV, con una validación que no falla `[CERT]`

Solo dos formatos, rechazados por extensión antes de abrir el archivo (`$C/util/ExportUtil.java:57`,
`$C/util/ImportUtil.java:194-198`) `[CERT]`.

- **CSV**: 12 columnas fijas (`$C/util/ImportExportConst.java:19-31`), con secciones `namespace` /
  `TagDefinitions` / `TagGroupDefinitions` / `RelationDefinitions` / `RuleDefinitions`, y **13 tipos de valor
  permitidos** (`AbsTime Boolean Marker Double DynamicEnum Float Integer Long Ord RelTime String TimeZone
  Unit`) — lista cerrada, sin extensibilidad.
- **JSON**: objeto único con claves `namespace`, `version`, `tags`, `tagGroups`, `relations`, `rules`.
- **Cross-namespace**: en CSV, si el valor trae `:` se parsea como `namespace:name`, si no se le antepone el
  namespace del diccionario (`$C/util/ImportUtil.java:616-617`); en JSON se emite `{"namespace":…, "name":…}`.

**El gotcha**: la validación del predicado NEQL **loguea pero no aborta** — `isNeqlPredicateValid()` devuelve
el error, el llamador hace `LOGGER.severe(...)` y **continúa** (`$C/util/ImportUtil.java:722-726`) `[CERT]`.
`[INFER]` Un predicado sintácticamente aceptable pero semánticamente roto entra al diccionario sin error y
simplemente **no matchea nada** en runtime — falla silenciosa, difícil de diagnosticar desde la UI.

**Protecciones contra un archivo hostil, más allá del `maxImportFileSize` de 1024 KB** ([Bloque 260] §260.7-d):
**ninguna encontrada en este paquete** `[CERT]` — el CSV se lee carácter a carácter sin límite de filas ni de
longitud de cadena (`$C/util/ImportUtil.java:839-860`), y el JSON usa `JSONTokener` sobre el stream completo
sin límite de profundidad ni de nodos. `[INFER]` El único freno real es el tamaño del archivo.

## 261.5 — `generateId()`: el namespace puede venir embebido en el nombre del slot `[CERT]`

`TagDictionaryUtil.generateId()` (`$C/util/TagDictionaryUtil.java:117-138`) `[CERT]`:
desescapa el nombre del slot con `SlotPath.unescape()`, y **si contiene `:` lo parte y usa esas dos partes
como namespace y nombre**; si no, resuelve el diccionario y antepone su namespace. Lanza
`IllegalStateException` si el componente no está montado, no hay diccionario, o el namespace está vacío — sin
fallback silencioso.

`[INFER]` Es el mecanismo por el que un diccionario puede alojar tags de **otro** namespace: se codifican en el
propio nombre del slot.

## 261.6 — El SEGUNDO bypass del candado `frozen` `[CERT]`

[Bloque 260] §260.6 documentó `BTagDictionary.importContext` como pase libre del guard `frozen`. Hay otro:

```java
         component.remove(property, Context.decoding);
```
`$C/util/TagDictionaryUtil.java:238` `[CERT]` — dentro de `updateTagGroupRelations()`, la utilidad de
migración que recorre los descendientes, detecta slots de tag con faceta `tg__`, borra los tags directos del
grupo y agrega en su lugar una `BRelation(n:tagGroup, ordDelGrupo)`.

`[INFER]` `Context.decoding` es el contexto que `BInfoList.checkContext()` ya aceptaba ([Bloque 260] §260.6).
O sea: el guard `frozen` tiene **dos** llaves públicas — `importContext` y `Context.decoding` — y esta segunda
se usa desde una utilidad de migración que no verifica permisos en el sitio de llamada.

## 261.7 — Nota de método: 2 afirmaciones del barrido corregidas `[CERT]`

Ambas eran sobre permisos. Se corrigen porque un claim de seguridad mal calibrado es peor que ninguno.

**1. "El chequeo de admin se saltea en el arranque".** El barrido lo presentó como que
`doImportDictionary` *"retorna antes del guard `hasAdminWrite()`"*. El código real
(`$T/BTagDictionary.java`, verificado en `$C/BNiagaraTagDictionary.java:204-219`):

```java
      if (this.getTagDefinitions().getSlotCount() != 0) {
         if (context == null) { return; }
         BUser user = context.getUser();
         if (user == null) { return; }
         if (!user.getPermissionsFor(this).hasAdminWrite()) { return; }
      }
      this.importFromBog(importContext);
```

Esos `return` son **rechazos**, no bypasses: si hay definiciones y falta contexto, usuario o `adminWrite`, la
importación **no ocurre**. Lo correcto es: **cuando el diccionario está VACÍO, el bloque entero se saltea y se
importa sin pedir permiso alguno**. `[INFER]` En el arranque eso es normal (la station carga su diccionario
built-in de un `.bog` de su propio módulo). El caso que sí queda abierto es un usuario **sin** `adminWrite`
invocando la acción sobre un diccionario Niagara vacío. Impacto bajo — restaura el diccionario estándar, no
carga un archivo del usuario — pero es una laguna real.

**2. "Cualquier usuario puede exportar el diccionario".** Sobreafirmado. Lo verificable:

```java
   public void doExportDictionary(BOrd file) throws Exception {
      new BTagDictionaryExportJob(this, file).submit(null);
   }
```
`$T/BTagDictionary.java:531-533` `[CERT]` — el job se somete con **contexto `null`** (no propaga el del
invocador), y `BTagDictionaryExportJob.run()` no verifica nada (`$C/BTagDictionaryExportJob.java:36-53`).
**Pero** invocar una acción sobre un componente ya pasa por el gate de invocación del framework, así que
"cualquier usuario" es falso.

`[INFER]` El hallazgo real es la **asimetría**: `doImportDictionary` chequea `hasAdminWrite()` explícitamente;
`doExportDictionary` no chequea nada **y descarta el contexto del invocador**. Para un diccionario OEM con
ontología propia ([Bloque 82]), exportar es exfiltrar el modelo semántico completo.

## 261.8 — Otros gotchas `[CERT]`

- **Excepción tragada en `importFromBog()`** (`$C/BNiagaraTagDictionary.java:285`): un fallo a mitad de import
  —después del `removeAll()` y antes de terminar los re-add— deja el diccionario **parcialmente vacío, sin
  rollback**.
- **`BOrdScope.includes()` devuelve `false` ante cualquier excepción** (`$C/scope/BOrdScope.java:83-85`):
  un ORD de scope colgado hace que la regla **nunca dispare**, en silencio.
- **`TagRuleIndex` no es `BComponent`** — no se descubre por slots ni es inspeccionable desde Workbench.
- **Sin ofuscación ZKM** en las 24 clases (ámbito: `tagdictionary/` raíz, `tag/`, `util/`).

## 261.9 — Conexiones

- **[Bloque 260]** — cierra tres de sus hilos: `TAG_GROUP_RELATION` concreto (§261.1), el segundo bypass de
  `frozen` (§261.6), y el import/export que allí solo aparecía como acciones (§261.4).
- **[Bloque 21]** — §261.2 responde algo que el espinazo no cubría: los tags de station son **computados**, no
  almacenados.
- **[Bloque 82]** — la asimetría import/export (§261.7-2) aplica a los diccionarios OEM Honeywell.
- **Gaps abiertos**: T3 (relaciones — `n:tagGroup` ya identificado acá), T4 (`condition/` + `neqlize/` — la
  validación NEQL de §261.4 se ejecuta allí), T5-T9.
