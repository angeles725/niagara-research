# Bloque 21 — Tag Framework + Haystack 4 + BQL + NEQL

Fecha: 2026-04-23
Fuentes empíricas: decompilados JARs + `docs-text/` + `devguide-clean/`.
JARs primarios: `tagdictionary-rt/ux/wb`, `haystack-rt`, `fcTagDict-rt`, `honTagDictionary-rt`, `exportTags-rt`, `bql-rt/ux`, `neql-rt`, `baja.jar` (javax.baja.tag, javax.baja.neql, javax.baja.bql).

Cubre el **semantic/query layer** transversal a todo Niagara: clasificación (tags), modelo semántico estándar (Haystack 4), diccionarios personalizados, y los dos lenguajes de consulta (BQL componente-oriented + NEQL entity-oriented).

---

## 21.1 Tag Framework core — BTagDictionary jerarquía

### Jerarquía de clases

```
BComponent
 └ BAbstractService (Bloque 20)
    └ BTagDictionaryService   (42.4 KB) — singleton, registrado en /Services
       Props:
        defaultNamespaceId    String       (default "n")
        tagRuleIndexEnabled   boolean      (default true)
        indexedTags           String       (CSV de tagIds a pre-indexar)
        neqlizeOptions        BNeqlizeOptions
        Niagara               BNiagaraTagDictionary (slot child)
        schemaVersion         int
       Actions:
        clearTagRuleIndex, invalidateAllTagIndexes, invalidateSingleTagIndex(BString)
        query(BString neqlQuery)            async RPC
        tagsToTagGroup                      UX helper
       Fields privados:
        impliedTagIndex       EntityTagIndex
        tagRuleIndex          TagRuleIndex
        relationRuleIndex     TagRuleIndex
        smartTagDictionaryCache  Map<String, Optional<SmartTagDictionary>>
       Métodos clave:
        Optional<Tag>       getImpliedTag(Id, Entity)
        Optional<Relation>  getImpliedRelation(Id, Entity)
        Collection<Tag>     getImpliedTags(Entity)
        static boolean      evaluateRuleOnEntity(TagRule, Entity)

BComponent
 └ BTagDictionary (20.3 KB) implements TagDictionary, BIStatus
    Props:
     namespace             String   immutable-after-start
     version               String   readonly-after-start
     enabled               boolean
     frozen                boolean  (true → no imports, no mutaciones)
     maxImportFileSize     int      default ≈50 MB
     neqlizeExcludedTags       String  ";" delimited
     neqlizeExcludedRelations  String  ";" delimited
     tagDefinitions        BTagInfoList        (frozen)
     tagGroupDefinitions   BTagGroupInfoList
     relationDefinitions   BRelationInfoList
     importDictionaryOrd   BOrd
    Actions:
     importDictionary(BOrd), exportDictionary(BOrd)
    Fault slots: fatalFault, namespaceFault

 └ BSmartTagDictionary (13.0 KB) extends BTagDictionary
    Props:
     tagRules BTagRuleList  (lista congelada de condiciones → tags implícitos)
    Métodos adicionales:
     Iterator<TagRule>      getRulesForTagId(Id)
     Iterator<TagRule>      getRulesForRelationId(Id)
     Optional<Tag>          getImpliedTag(Id, Entity)
     Optional<TagInfo>      getImpliedTagInfo(Id, Entity)
     void                   addAllImpliedTags(Entity, Collection<Tag>)
```

**Regla clave**: todos los diccionarios custom (fc, hon, hs4) extienden `BSmartTagDictionary`, NO `BTagDictionary` directo. Sin smartRules, un tag NO puede ser derivado — solo estático.

### TagInfo / TagGroupInfo / RelationInfo (definiciones)

```
BComponent
 └ BTagInfo (10.2 KB abstract) implements TagInfo, BIDataPolicy
    Props: validity (BTagRuleCondition)
    Actions: addDataPolicy
    Métodos:
     Id getTagId()                       (derivado del slot name)
     Optional<TagDictionary> getDictionary()
     boolean isIdealFor(Type)            filtra por BComponent Type
     boolean isValidFor(Entity)          filtra por instancia
     Optional<DataPolicy> getDataPolicy()

    Subclases concretas:
     BSimpleTagInfo                tag estático sin validez dinámica
     BDynamicEnumTagInfo           tag con valores discretos (choice)
     (Honeywell) BHonTypeTag, BEquipmentTypeTag, BPathLabelTag, BPointLabelTag, ...

 └ BTagGroupInfo (14.1 KB)
    Props:
     validity (BTagRuleCondition)
     tagList (BTagInfoList)       miembros del grupo
    Constante: TAG_GROUP_RELATION_STR = "tagGroupRelation"
    → los tag groups se materializan como RelationKnobs en BOG (slot tag, persistido)

 └ BRelationInfo (3.1 KB)
    Métodos: getRelationId(), getDictionary()
    Subclases:
     BComponentChildRelation, BComponentParentRelation (built-in)
     (Honeywell) BCustomRelation
```

### Entity interface

```java
public interface Entity extends Taggable {
    Tags tags();
    Relations relations();
    Optional<BOrd> getOrdToEntity();
}
```

BComponent se auto-wrappea en Entity cuando pasa al tag framework (adapter pattern). Entity NO es BComponent — es la interfaz que el Tag/NEQL framework consume.

### Persistencia

Tags se persisten **indirectamente**:
- **Attribute tag** (implícito) → NO persiste como property. Se recomputa on-demand evaluando BTagRule sobre estado actual del BComponent. Un tag puede "desaparecer" si la condición se vuelve falsa (ephemeral).
- **Slot tag** (grupo via RelationKnob) → sí persiste en BOG como BRelation con id `"tagGroupRelation"`.
- **Implied relations** → NO persisten. `getImpliedRelation` es cálculo in-memory.

Impacto: `save/restore BOG` NO serializa tags directos. Cambios en BTagDictionary/rules cambian qué tags "tiene" el mismo componente sin tocarlo.

---

## 21.2 Haystack 4 model mapping

### defs.json + protos.json

Ubicación: `haystack-rt.jar → com/tridium/haystack/data/haystack-4-defs/`
- `defs/defs.json` (497 KB, ~3000 defs)
- `protos/protos.json` (194 KB, proto combinations)

Columnas principales de defs.json:
```json
{
  "meta": {"ver":"3.0"},
  "cols": [
    {"name":"def"},          // symbol (namespace:key)
    {"name":"is"},           // supertypes (herencia)
    {"name":"lib"},          // librería (lib:phIoT, lib:phScience, lib:phBuilding)
    {"name":"doc"},          // doc string
    {"name":"children"},     // proto requirements (NO herencia)
    {"name":"of"},           // cuantificación
    {"name":"mandatory"},    // required en proto
    {"name":"enum"},         // valores discretos
    {"name":"quantityOf"}
  ]
}
```

Ejemplos reales:
```json
{"def": "absorption", "is":["marker"], "lib":"lib:phIoT",
 "doc":"Cooling process using energy from heat source such as hot water"}

{"def": "ac-elec-meter", "is":["elec-meter"],
 "children":[{"avg":"marker","ac":"marker","current":"marker"}]}
```

### Marker vs Value tags

- **Marker** (`is: [marker]`): clasificación booleana, sin valor. Ej: `sensor`, `meter`, `air`, `point`.
  - En BTagInfo: `BSimpleTagInfo` sin tipo base.
- **Value** (`is: [number|str|bool|...]`): tag con valor tipado. Ej: `minVal`, `maxVal`, `units`, `area`.
  - En BTagInfo: `BDynamicEnumTagInfo` o `BSimpleTagInfo` con validator.

### Inheritance (acíclica)

```
ac-elec-meter  → is → elec-meter → is → meter → is → equip → is → entity
```

Lógica en `Haystack4Importer`:
```java
private void processDefTyping() {
  for (Def def : defMap.values()) {
    Def superDef = getSuperDef(def);
    if (superDef != null) def.superType = superDef;
  }
}
```

**GOTCHA**: `"children"` NO es herencia — son **combinations** (qué tags deben coexistir en el proto). Herencia es `"is"`.

### Pipeline de import Haystack 4

Clase: `com.tridium.haystack.Haystack4Importer` (31.2 KB).

```
resolveImportArtifacts()        carga defs.json, protos.json
 → importDefs()                  parse → defMap
 → processDefTyping()            construye cadenas de herencia
 → findSpecialDefs()             localiza "entity", "id", "geoPlace"
 → processChoices()              enums (dynamic choice tags)
 → createDictionaryItems()       genera BTagInfo + BTagGroupInfo
 → addTagsAndRelations()
 → addSubTypeTagRules()          reglas automáticas por herencia
 → addDeclaredTagRules()         reglas declaradas en importHaystack4Config.json
 → addProtoTagGroups()           protos.json → BTagGroupInfo
 → addNeqlizeExcludedTags()      optimización query
 → return BHaystack4TagDictionary
```

---

## 21.3 Diccionarios estándar vs custom

| Namespace | Clase | Archivo | Rol | # Tags aprox |
|-----------|-------|---------|-----|--------------|
| `n` | BNiagaraTagDictionary (14.2 KB) | tagdictionary-rt.jar | Built-in Niagara, HARD-CODED, frozen | ~50 |
| `hs` | BHsTagDictionary (18.6 KB) | haystack-rt.jar | Compat Haystack 3 vía CSV | ~1800 |
| `hs4` | BHaystack4TagDictionary (13.3 KB) | haystack-rt.jar | Haystack 4 nativo vía JSON | ~3000 |
| `fc` | BFcTagDictionary (0.5 KB) | fcTagDict-rt.jar | Honeywell Facility Commander extensions | ~30 |
| `hon` | BHonTagDictionary (0.3 KB) | honTagDictionary-rt.jar | Honeywell corporativo | ~40 |
| `phScience` | (NO shipped) | — | Referencia externa a Project Haystack, no incluido en OptimizerSupervisor | — |

### BNiagaraTagDictionary

HARD-CODED (no lee JSON/CSV). `frozen=true` desde init. Tags incluidos: `n:abs`, `n:ac`, `n:equip`, `n:meter`, `n:point`, `n:sensor`, `n:site`, `n:unit`, etc.

### BHsTagDictionary (Haystack 3 compat)

Constants:
```java
HS_NAME_SPACE = "hs"
TAGS_CSV_FILE_VERSION = "3.0"
TAGS_IMPORT_FILE_NAME = "tags.csv"
EQUIP_IMPORT_FILE_NAME = "equip.csv"
HS_KIND_BOOL = "b", HS_KIND_NUMBER = "n", HS_KIND_STR = "s"
```
Importa CSV para compatibilidad legacy. Doble formato (tags.csv + equip.csv) corresponde a especificación Haystack 3.

### BFcTagDictionary (FC custom)

Tags propios: `BIdTag`, `BPathLabelTag`, `BPointLabelTag`, `BControllerNameTag`, `BProtocolTag`, `BDriverSuppliedTag`, `BPropertySuppliedTag`, `BCustomLookupTag`.
Condiciones custom: `BIsPointProxyTypeCondition` (filtra entity si es PointProxy de cierto tipo).

### BHonTagDictionary (Honeywell corp)

Tags: `BHonTypeTag`, `BEquipmentTypeTag`, `BIdTag`, `BPathLabelTag`, `BPointLabelTag`, `BCustomLookupTag`.
Relación custom: `BCustomRelation`.
Más simple que fc (no lookups complejos).

### Resolución cuando hay colisión

`BTagDictionaryService` itera:
1. `getSmartTagDictionaries()` (cached)
2. `getTagDictionaries()` (all)
Primera match gana. **Best practice**: usar namespace qualified siempre (`hs4:ac`, no `ac`).

---

## 21.4 Export tags — workflow entre stations

Módulo: `exportTags-rt.jar`.

### BNiagaraExportTag (abstract, 17.0 KB)

Propósito: **Sincronizar tags entre Supervisor y Subordinate** en red Niagara.

```java
Props:
  status             BStatus
  enabled            boolean
  supervisorStation  BNameList   (destinos)
  stationSlotPath    BOrd        (component target)

Ciclo:
  long preJoin()    validación
  long join()       sincroniza tags via Fox
  long postJoin()   post

Subclases:
  BSupervisorExportTagNetworkExt    (lado supervisor)
  BSubordinateExportTagNetworkExt   (lado subordinate)
```

### Export a archivo

```java
// BTagDictionary
public Action exportDictionary;
public void doExportDictionary(BOrd outputOrd);

// BHsTagDictionary extiende
public void doMakeImportFiles(BOrd outputFolder);
// Genera tags.csv + equip.csv + metadata.json
```

Workflow típico supervisor-side:
```
/Services/TagDictionaryService/Haystack → exportDictionary(/files/export/)
 → /files/export/tags.csv
   /files/export/equip.csv
   /files/export/metadata.json
```

---

## 21.5 BQL — Baja Query Language

### Arquitectura general

Pipeline: **tokenizer → parser → AST → compile → cursor execution**.

JARs:
- `bql-rt.jar` (163 clases, runtime)
- `bql-ux.jar` (builder JavaScript)
- `baja.jar` (`javax.baja.bql.*`)

### Tokens reservados (com.tridium.bql.compiler.Constants)

Palabras clave:
```
SELECT FROM WHERE ORDER BY ASC DESC
DISTINCT ALL DEPTH STOP
AS HAVING TOP
NULL CURRENT_DATE CURRENT_TIME CURRENT_ABSTIME
TYPE_SPEC TYPE CLASS_SPEC
```

Operadores:
```
ADD (+) SUBTRACT (-) MULTIPLY (*) DIVIDE (/) MOD (%)
EQUAL (=) NOT_EQUAL (!=)
GREATER (>) GREATER_OR_EQUAL (>=)
LESS (<) LESS_OR_EQUAL (<=)
LIKE IN
AND OR NOT
```

### EBNF reconstruido

```ebnf
BQL_QUERY           := SELECT_CLAUSE FROM_CLAUSE [WHERE_CLAUSE] [ORDER_CLAUSE] [TOP_CLAUSE]
SELECT_CLAUSE       := SELECT [DISTINCT | ALL] projection_list
projection_list     := projection_item ("," projection_item)*
projection_item     := path [AS IDENTIFIER]
                     | function_name "(" [path] ")" [AS IDENTIFIER]
FROM_CLAUSE         := FROM typespec [DEPTH NUMBER] [STOP]
WHERE_CLAUSE        := WHERE predicate
predicate           := expression (AND expression)*
                     | expression (OR expression)*
expression          := compare_expr
compare_expr        := additive_expr (relational_op additive_expr)*
additive_expr       := multiplicative_expr ((+|-) multiplicative_expr)*
multiplicative_expr := unary_expr ((*|/|%) unary_expr)*
unary_expr          := [NOT | MINUS] primary_expr
primary_expr        := LITERAL | path | function | "(" expression ")"
path                := IDENTIFIER ("." IDENTIFIER)*
function            := FUNC_NAME "(" [expression ("," expression)*] ")"
ORDER_CLAUSE        := ORDER BY order_item ("," order_item)*
order_item          := path [ASC | DESC]
TOP_CLAUSE          := TOP NUMBER
LITERAL             := STRING | NUMBER | BOOLEAN | NULL | BQL_TIME
```

### Clases internas

Compilador (`com.tridium.bql.compiler`):
```
BqlTokenizer   lexical: peek(), next(), push(Token), reset(String)
Token          tipo + lexema + pos
BqlParser      top-level parser
SelectParser   SELECT/FROM/WHERE/ORDER/HAVING/DEPTH
ExprParser     expresiones con precedencia
Constants      enum de 40+ tipos
```

Modelo (`com.tridium.bql`):
```
BSelect extends BQuery      AST root (extent, projection, predicate, order)
BTop extends BQueryNode     LIMIT (limit: long)
Projection                  columnas (all | some | path-based)
Ordering                    ORDER BY con comparator tipo-aware
Quantifier                  pipeline DISTINCT/TOP/GROUP
SelectQuery extends BqlQuery ejecutable
```

Ejecución (`com.tridium.bql.expression` + `com.tridium.bql.query`):
```
ExprEngine            eval lazy (AND/OR/compare/LIKE/IN)
BBqlEngine extends BQueryEngine   ejecuta BSelect → BITable
BogCursor             DFS sobre object graph con type filter
ProjectionTableCursor materializa columnas proyectadas
BAggregateTable, BDistinctTable, BTopTable   wrappers en memoria
```

### Ciclo de ejecución

```
String BQL
 → BqlTokenizer.reset(bql)
 → BqlParser.parse() → BqlQuery (AST)
 → SelectParser.parse() → BSelect
 → ExprParser.parse() → Expressions
 → BBqlEngine.doCompile(BSelect) → ICompiledQuery
 → BBqlEngine.execute(Context) → BITable<BIObject>  (lazy)
 → Cursor.next() → slot access + type checks + filter
 → ProjectionTableCursor → columns materialized
```

### Filtros de tipo (`com.tridium.bql.filter.*`)

- `BStringFilter` — pattern match, wildcard `*`, case sensitivity flag
- `BIntegerFilter / BDoubleFilter / BFloatFilter` — numeric range
- `BAbsTimeFilter` — abs time range
- `BBooleanFilter` — eq
- `BEnumFilter` — enumerated
- `BFacetsFilter` — composición
- `BNullFilter` — IS NULL
- `BRangeFilter` — numeric con bounds

### Agregaciones

`com.tridium.bql.function.*`:
```
BCount BSum BAvg BMin BMax   extends BIAggregator
  aggregate(BObject)   procesa un valor
  BValue commit()      resultado final
```

**Limitación**: NO hay `GROUP BY` nativo — agregaciones se aplican sobre el conjunto resultante completo (no por grupo de columna).

### Integración con ORD

Sintaxis compuesta:
```
station:|slot:/|bql:select * from alarm:AlarmSourceExt
history:/demo/AuditHistory|bql:select * where operation like 'Invoked'
ip:<host>|foxs:|station:|slot:/|bql:select * from control:ControlPoint
```

Componentes consultables típicos:
- `alarm:AlarmSourceExt`
- `control:ControlPoint`
- `schedule:DailySchedule`
- `baja:Component` (catch-all)

Acceso a properties vía dot notation: `status.alarm`, `parent.name`, `alarmInhibit.boolean`.

### Operadores finos

**LIKE** (`BStringFilter`):
- Wildcard `*` (NO `%`, NO `?`)
- `name like 'Cool*'` prefix; `'*Cool*'` substring; `'C*ol'` middle
- Case-sensitive por default (flag matchCase opcional)

**IN**:
```sql
where status in (alarmed, unacknowledged)
```

**Null**:
```sql
value = null     -- IS NULL
value != null    -- IS NOT NULL
```

### Strings / casing

- String literals: comillas simples `'valor'`, escape con `\'`
- Tokens reservados case-INSENSITIVE (`SELECT` = `select` = `SeLeCt`)
- Identificadores: case-SENSITIVE (siguen reglas Baja, camelCase típico)
- Whitespace auto-skipped por tokenizer

### Security

Clase: `QueryPermissionCheckIterator` (en baja.jar).
```
TRUSTED_QUERY_HANDLERS: Set<Type>   // skip permission check
QueryPermissionCheckIterator.make(Iterator, User)   // aplica ACL
```
- Filas filtradas por permisos del User (Context)
- `absoluteOrd` puede retornar null si no hay read access

### BajaScript integration

`rc/util/parseBql.js` + `rc/builder/BqlQueryBuilder.js`:
```
toSelect(bqlBody)           String → bql:Select (RPC async)
toBqlBody(select)           reverse
getProjection(select)       extrae columnas
getExtent(select)           extrae FROM typespec
getPredicateInfo(select)    extrae WHERE expressions
```
RPC gateway: `box:BqlRpc` (compila/descompila server-side).

### Ejemplo completo

```sql
station:|slot:/|bql:select
  parent.name   as 'Point Name',
  parent.out    as 'Point Status',
  alarmInhibit.boolean as 'Inhibited'
from alarm:AlarmSourceExt
where alarmInhibit.boolean = 'true'
  and parent.name like 'Cool*'
order by parent.name asc
top 50
```

### BQL gotchas

- DISTINCT materializa en memoria → O(N) mem (peligro en result sets grandes)
- ORDER BY con null: se trata como mínimo
- `STOP` en FROM: DFS se corta al encontrar componente marcado `stop` (útil para scoping)
- Subscription queries (streaming) NO soportadas — modelo pull-based con cursor lazy
- Remote queries via `BFoxBqlResolver` (Fox protocol)

---

## 21.6 NEQL — Niagara Entity Query Language

### Qué es / cuándo usarlo

NEQL opera sobre **entidades taggables** combinando:
- **Tags** (namespace:key + BIDataValue o marker)
- **Relations** (aristas dirigidas entre entidades, con Id y dirección)
- **SmartTagDictionary** (reglas que implican tags dinámicamente)

A diferencia de BQL (tree semantics, slots), NEQL opera en el **entity space** (grafo de entidades taggables).

### Gramática EBNF oficial

```ebnf
<statement>       := <full_select> | <filter_select> | <traverse>

<full_select>     := "select" <tag_list> "where" <predicate>
<filter_select>   := <predicate>                          // bare predicate
<traverse>        := "traverse" <relation> ("where" <predicate>)?

<tag_list>        := <tag> ("," <tag>)*
<tag>             := [<namespace> ":"] <key>
<relation>        := [<namespace> ":"] <key> <direction>
<direction>       := "->" | "<-"                          // outbound / inbound

<predicate>       := <cond_or>
<cond_or>         := <cond_and> ("or" <cond_and>)*
<cond_and>        := <term> ("and" <term>)*
<term>            := <comparison> | <tag_path> | <not> | <relation_seq>

<comparison>      := <comparable> <cmp_op> <comparable>
                   | <tag_path> "like" <regex>
<cmp_op>          := "=" | "!=" | "<" | "<=" | ">" | ">="

<comparable>      := <value> | <tag_path>
<value>           := <number> | <boolean> | <string>

<tag_path>        := [<relation>]* <tag>
<not>             := ("not" | "!") <negatable>
<negatable>       := "(" <predicate> ")" | <tag>

<word>            := [a-zA-Z_][a-zA-Z0-9_]*
```

Precedencia (menor → mayor): `OR` → `AND` → términos (NOT, compare, tags, relations).

### NEQL vs BQL (tabla comparativa)

| Aspecto | NEQL | BQL |
|---------|------|-----|
| API de datos | Entity / Taggable / Relations | Component tree, Slots |
| Navegación | Relaciones (1-hop o cadena) | Tree semantics (parent.parent...) |
| Proyección columnar | NO (devuelve entidades) | Sí (columnas arbitrarias) |
| Agregación | NO | Sí (count/sum/avg/min/max) |
| Scope | Entidades taggables | Componentes en subsistema |
| Extent | Implícito (all compatible) | Explícito (`FROM <type>`) |
| ORDER BY / TOP | NO (select ignorado) | Sí |

**Uso típico**:
- NEQL → equipamiento taggado, búsquedas semánticas, selección en Analytics/navigation.
- BQL → reportes con agregación, proyecciones múltiples, alarma/history queries.

### Arquitectura interna

Tokenizer: `com.tridium.neql.NeqlTokenizer`
```
Constants: WHITESPACE WORD EOF INTEGER BOOLEAN STRING OPERATOR
           SELECT TRAVERSE WHERE COLON AND OR NOT
           EQUAL NOT_EQUAL GREATER GREATER_OR_EQUAL LESS LESS_OR_EQUAL LIKE
           TRAVERSE_IN TRAVERSE_OUT
```

Flujo: `NeqlParser.parse()` → AST root → subparsers (`SelectParser`, `TraverseParser`, `ExprParser.or() → and() → term()`).

AST (`javax.baja.neql.*`):
```
Expression (abstract)
 ├ GetTagExpression
 ├ LiteralExpression
 ├ BinaryExpression
 │  ├ AndExpression, OrExpression
 │  ├ ComparisonExpression
 │  │  ├ EqualExpression, NotEqualExpression
 │  │  ├ LessExpression, LessOrEqualExpression
 │  │  ├ GreaterExpression, GreaterOrEqualExpression
 │  └ LikeExpression
 ├ NotExpression
 ├ EvalOnExpression          traversal
 ├ TraverseOutExpression, TraverseInExpression
 ├ ContextExpression         facet {key}
 └ GetRelationExpression
```

Evaluador: `NeqlEntityEvaluator` (stateless, thread-safe).
```java
public static Predicate<Entity> makePredicate(String query, Context cx);
public boolean evalBoolean(Context cx, Entity self, Entity root, Expression e);
public Object   eval        (Context cx, Entity self, Entity root, Expression e);
```
Cada `Expression.evaluate(Entity, Context)` devuelve `Collection<?>`:
- Tags → `Collection<BIDataValue>`
- Compare → `Collection<BBoolean>` (SINGLE_TRUE/SINGLE_FALSE)
- Relaciones → `Collection<Entity>`

### Traversal de relaciones

Modelo: `javax.baja.sys.BRelation extends BStruct implements Relation`.
```
Props: relationId (namespace:key), inbound (bool), relationTags (facets),
       sourceOrd (BOrd al endpoint)
Métodos: getId(), isInbound(), isOutbound(), getEndpointOrd(), getEndpoint(), tags()
```

Iteradores DFS:
- `EvalOnIterator` (com.tridium.neql) — DFS con stack de `RelatedEntityIterator`; resuelve primera relación desde root, empila iteradores para cada endpoint.
- `RelatedEntityIterator` — itera `Iterator<Relation>` obtenido de `entity.relations().getAll(id)`.

### Scheme ORD `neql:`

```java
public class BNeqlScheme extends BQueryScheme {
    public static final BNeqlScheme INSTANCE;
    public OrdQuery parse(String queryString);
}
```

Formatos:
```
neql:<query>
slot:/a/b|neql:n:point
sys:|neql:n:device and hs:equip
ip:<host>|foxs:|station:|slot:/|neql:hs:ahu
```

Resultado: `BQueryResult`
```java
public class BQueryResult extends BObject {
    Iterator<Entity> getResults();
    Stream<Entity>   stream();
    long             count();
}
```

### Handler API

```java
public interface BINeqlQueryHandler extends BIQueryHandler {
    CloseableIterator<Entity> query(OrdTarget scope, OrdQuery query);
}
public abstract class BNeqlEntityQueryHandler extends BObject implements BINeqlQueryHandler {
    protected abstract Iterator<? extends Entity> getIteratorForScope(OrdTarget scope);
    public CloseableIterator<Entity> query(OrdTarget scope, OrdQuery query) {
        return filterByQuery(getIteratorForScope(scope), query);
    }
}
```

### Context expressions (facets)

Sintaxis `{facetName}`:
```
neql: hs:area > {minArea}
```
Se resuelve desde `BFacets` pasado al resolve:
```java
BFacets facets = BFacets.make("minArea", 100);
query.resolve(root, facets).get();
```

### Ejemplos reales

```neql
n:point                                    // todos los puntos
n:point and hs:sensor                      // puntos sensor
hs:equip and hs:hvac and hs:area > 150
hs:chiller                                 // chillers (si dict los define)
traverse n:childPoint->                    // navegación explícita
n:point and n:name like ".*Temp.*"         // regex match
hs:equip and not hs:sensor                 // negación
hs:area > {minArea}                        // facet context
select n:name, n:displayName where n:point // select ignorado, devuelve entidades
```

---

## 21.7 BQL vs NEQL — cuándo usar cuál (decisión operativa)

```
Necesito ...
 ├ reportes con columnas específicas y agregados → BQL
 ├ filtrar nav tree / analytics hierarchy por tags → NEQL
 ├ query sobre history/alarm con WHERE y TOP → BQL
 ├ encontrar equipamiento por tags haystack → NEQL
 ├ proyección scalar (length(name), parent.out) → BQL
 ├ traversal de relaciones dirigidas → NEQL (traverse rel->)
 ├ count(*), sum(value), max(x) → BQL
 └ "entidades que tengan tag n:point y están tagueadas como hs:air" → NEQL
```

Ambos coexisten. NEQL es capa **semantic** (tags). BQL es capa **structural** (slots + properties + tree). Analytics + Haystack tagging → NEQL. Alarm/history reporting → BQL.

---

## 21.8 Tag queries: RPC y optimización

### BNeqlizeRpc (tagdictionary-rt)

Clase `com.tridium.tagdictionary.neqlize.BNeqlizeRpc` (19.7 KB).

```java
public static JSONObject getServiceExcludedTagsRelations(Context ctx);
public static JSONObject getIdentifyingTagsRelations(
    String baseComponentOrd,
    List<String> tags,
    String neqlQuery,
    Context ctx
);
```

Sintetiza queries NEQL automáticamente desde selecciones UI (TagSetSearch, RelationSearch) y cachea resultados en `FilteredTagsMap`.

### BNeqlizeOptions (excluded tags)

```java
class BNeqlizeOptions extends BStruct {
  Property defaultExcludedTags;     // globales
  Property useDefaultExcludedTags;  // flag
  Property customExcludedTags;      // override
  String getExcludedTags();         // combined
  Set<String> getExcludedTagFilters();  // parsea ";" delimited + wildcards
}
```

Separador: **semicolon**, NO comma (comma es parte del pattern). Wildcards permitidos:
```
hs:*Energy;hs:*Power;n:test*
```

**Impacto performance**: saltear tags comunes acelera NEQL en supervisors con 10k+ components.

### TagRuleIndex (3 niveles de cache)

```
Level 1: smartTagDictionaryCache    Map<String, Optional<SmartTagDictionary>>
Level 2: TagRuleIndex               compiled rules index
Level 3: EntityTagIndex             per-entity tag cache
```

Invalidar uno NO invalida los otros. Full consistency requiere `invalidateAllTagIndexes()`.

Impact:
- **Enabled** (default): getImpliedTag O(1).
- **Disabled**: O(n) eval de todas las rules por query.

---

## 21.9 Gotchas operacionales cross-bloque

1. **Tag IDs case-SENSITIVE** — `hs:air` ≠ `hs:Air`. Todos los defs Haystack están en lowercase.
2. **Namespace qualifier obligatorio** — usar `hs4:ac` no `ac`. Resolution order: smart dicts primero, luego all dicts; primera match gana.
3. **Tags NO persisten en BOG como properties** — son derived on-demand evaluando rules. Backup/restore NO pierde tags, pero cambios en dict cambian qué tags "tiene" el mismo componente sin tocarlo.
4. **`children` en Haystack ≠ herencia** — son proto combinations (requeridas juntas). Herencia es `is`.
5. **Frozen dictionaries** — `frozen=true` bloquea imports/mutaciones. Cambios → crear nuevo dict + switchover via `defaultNamespaceId`.
6. **BQL DISTINCT materializa todo en memoria** — O(N) riesgo en result sets grandes.
7. **BQL NO tiene GROUP BY** — agregaciones solo sobre set completo.
8. **BQL wildcard es `*`, NO `%` ni `?`** — distinto a SQL clásico.
9. **BQL case-insensitive para keywords, case-sensitive para identifiers** — SELECT = select, pero `Name` ≠ `name`.
10. **NEQL shortest-path ambiguity** — `n:child->n:name="X"` evalúa SOLO primer hijo. Workaround: `traverse n:child-> where n:name="X"`.
11. **NEQL sin cycle detection** — stack EvalOnIterator puede crecer si hay ciclos bidireccionales en relaciones.
12. **NEQL SELECT ignorado** — proyección no soportada; siempre devuelve entidades completas.
13. **phScience namespace referenciado pero NO shipped** — lib externa de Project Haystack, no incluida en OptimizerSupervisor.
14. **CSV Haystack 3 es compatibility shim** — Haystack4Importer (JSON) es el flujo moderno.
15. **Entity ≠ BComponent** — es interface adapter; BComponent se auto-wrappea cuando pasa al tag framework.
16. **Implied relations ephemeral** — `getImpliedRelation` es cálculo in-memory, NO persiste.

---

## 21.10 Integración cross-bloque

- **Bloque 6** (BComponent/Slots/Facets): Tags son capa ortogonal a Properties/Slots/Facets — no mutuamente excluyentes.
- **Bloque 14** (Templates): Instance de template hereda tags implícitas porque las rules se re-evalúan sobre la copia (no copia atributos, re-deriva).
- **Bloque 15.9** (Nav tree): Nav puede filtrarse con NEQL — `n:child-> where hs:equipment`.
- **Bloque 16** (Analytics): `QueryLevelDef` en Analytics pipelines usa NEQL para selección de equipment (jerarquía basada en tags).
- **Bloque 20.4+** (Services): `BTagDictionaryService` extends `BAbstractService`, registrado en `/Services`.

---

## Fuentes primarias leídas

1. `modules/tagdictionary-rt.jar` — BTagDictionary (20.3 KB), BTagDictionaryService (42.4 KB), BSmartTagDictionary (13.0 KB), BTagInfo/BTagGroupInfo/BRelationInfo, BTagRule/BTagRuleCondition, BNeqlizeRpc (19.7 KB), TagDictionaryUtil (19.6 KB).
2. `modules/haystack-rt.jar` — BHaystack4TagDictionary (13.3 KB), BHsTagDictionary (18.6 KB), Haystack4Importer (31.2 KB), defs.json (497 KB), protos.json (194 KB).
3. `modules/fcTagDict-rt.jar` — BFcTagDictionary, tags + conditions custom FC.
4. `modules/honTagDictionary-rt.jar` — BHonTagDictionary, tags + conditions + relations custom Honeywell.
5. `modules/exportTags-rt.jar` — BNiagaraExportTag, BSupervisorExportTagNetworkExt, BSubordinateExportTagNetworkExt.
6. `modules/bql-rt.jar` + `bql-ux.jar` + `baja.jar::javax.baja.bql` — 163 clases runtime, builder JS.
7. `modules/neql-rt.jar` + `baja.jar::javax.baja.neql` — evaluator, tokenizer, expression tree.
8. `niagara-help/devguide-clean/{neql,entityModel,bql}.txt` — specs oficiales.
9. `niagara-help/docs-text/{docEngNotes,docKitControl,NiagaraAnalyticsFrameworkGuide}.txt` — ejemplos reales.

Total: ≈900 clases descompiladas, 2 JSONs parseados, 3 gramáticas reconstruidas.
