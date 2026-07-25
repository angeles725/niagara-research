# Bloque 260 — Tags (I): la API pública del framework de diccionarios de tags

> **Qué documenta**: el contrato público del subsistema de tagging de Niagara N4 — `javax.baja.tagdictionary`
> (20 clases) + `javax.baja.tagdictionary.data` (2): qué es un diccionario, cómo se monta y se descubre, cómo
> se modela un tag, la distinción **directo vs implícito**, y qué debe hacer un tercero para publicar su
> propio diccionario. Cierra el gap **T1** del focus `tags`.
>
> **NO es un re-tread de [Bloque 21]**. Aquel documentó el ESPINAZO (jerarquía, Haystack, BQL/NEQL, workflow
> de export) para ~159 clases. Este abre el contrato público a fondo: lo que un bloque de espinazo no captura.
> Donde solo confirma a B21, lo dice y sigue.
>
> **Alcance**: los dos paquetes públicos. La implementación privada `com.tridium.tagdictionary/**` (motor,
> relaciones, condiciones, neqlize) son los gaps T2-T4 y NO se abrió.
>
> **Fuentes** (decompilado vineflower, pipeline canónico):
> - `$T` = `…/tagdictionary/tagdictionary-rt/vineflower/javax/baja/tagdictionary/`
> - Raíz: `/home/cristian/modules/Prototipos/modulos/organized/`
>
> **Método**: barrido delegado (tier `sonnet`) + verificación inline del driver: **11 tokens load-bearing**
> re-verificados, incluido el hallazgo de seguridad de §260.6. Marcadores: `[CERT]` = fuente primaria
> (`file:line`); `[INFER]` = deducción. Bloque de EVIDENCIA.
>
> **NOTA DE MÉTODO — el ratio de este bloque mide al autor, no a la fuente.** Cerró en **0.74**
> (14 `[INFER]` / 19 `[CERT]`), muy por encima del umbral 0.5. En [Bloque 254] y [Bloque 255] un ratio alto
> señalaba agotamiento real de la evidencia; **acá NO**: el gap T1 recién abrió el subsistema y el focus tiene
> 8 gaps con ~137 clases por delante. Lo que este número dice es que **deduje de más** — sobre todo
> contrastando con el focus del chart recién cerrado. Se deja anotado en vez de reclasificar el bloque a
> "síntesis" para bajar el número: las secciones §260.1, §260.2 y §260.6 se apoyan en citas verificadas, pero
> el volumen de interpretación alrededor es excesivo para un bloque de apertura. Corrección para T2 en
> adelante: más cita, menos glosa.

---

## 260.1 — El diccionario es un COMPONENTE montado, no un tipo registrado `[CERT]`

Dos líneas definen toda la topología del subsistema:

```java
   public boolean isParentLegal(BComponent parent) {
      return parent instanceof TagDictionaryService;
   }
```
`$T/BTagDictionary.java:433-435` `[CERT]`

```java
   public Collection<TagDictionary> getTagDictionaries() {
      Collection<TagDictionary> result = new ArrayList<>();
      SlotCursor<Property> c = this.getProperties();

      while (c.next(TagDictionary.class)) {
```
`$T/BTagDictionaryService.java:386-390` `[CERT]`

`[INFER]` **Un diccionario de tags NO se registra: se monta.** Tiene que colgar como hijo directo de
`BTagDictionaryService` — `isParentLegal` lo rechaza en cualquier otro lado — y el servicio los descubre
**iterando sus propios slots**, no consultando el registro de tipos.

Esto es un contraste fuerte con el subsistema que acaba de documentar el corpus: el charting clásico resuelve
sus extensiones por `@AgentOn` y por `Sys.getRegistry()` ([Bloque 253] §253.5, [Bloque 255] §255.6). Acá **no
hay ni un `@AgentOn` ni una consulta al registro para descubrir diccionarios** — el único uso del registro en
todo el paquete es para poblar el enum de la acción `addDataPolicy`
(`Sys.getRegistry().getConcreteTypes(BDataPolicy.TYPE.getTypeInfo())`, `$T/BTagInfo.java:152-153`) `[CERT]`.

`[INFER]` Consecuencia práctica para un integrador: publicar un diccionario propio **no es cuestión de
registrar un agente**, sino de (a) declarar el tipo con `@NiagaraType` y (b) **instanciarlo dentro del
servicio en la station**. Es configuración de estación, no de módulo.

## 260.2 — El tagging ESTÁ licenciado (y con un límite opaco) `[CERT]`

```java
   public Feature getLicenseFeature() {
      return Sys.getLicenseManager().getFeature("tridium", "tags");
   }
```
`$T/BTagDictionaryService.java:346-348` `[CERT]`

Y cada diccionario, al arrancar, consulta un límite adicional:

```java
      String dictionaryLimitFault = (String)service.fw(501, "dictionary.limit", null, null, null);
      if (dictionaryLimitFault != null || service.isFault()) {
         this.fatalFault = true;
```
`$T/BTagDictionary.java:294-296` `[CERT]`

`[INFER]` `fw(501, "dictionary.limit", …)` es una llamada de framework por opcode — el mismo mecanismo opaco
que apareció en el export a PDF del chart ([Bloque 256] §256.1, `fw(303, …)`). Devuelve un mensaje de falla si
la cantidad de diccionarios activos superó un tope licenciado; **el valor del tope no es visible desde la API
pública**. Si devuelve no-nulo, el diccionario queda en `fatalFault` permanente para esa corrida.

`[INFER]` Contraste directo con el chart clásico, donde la ausencia probada de gates fue un hallazgo
([Bloque 254] §254.8 / [Bloque 251] §251.8 #8): **el tagging sí es una feature licenciada y con cupo**. Para
un integrador con una licencia ajustada —como las de v4.12 con SMA vencido que el corpus ya analizó— esto es
material: los diccionarios custom compiten por un cupo.

## 260.3 — El modelo: `Id` con namespace, y el marcador como valor por defecto `[CERT]`

La identidad de un tag es un `javax.baja.tag.Id` de la forma **`namespace:nombre`**, generado por
`TagDictionaryUtil.generateId(...)`, con inicialización perezosa e invalidación de caché al renombrar
(`$T/BTagInfo.java:87-94`). El `namespace` es una propiedad del diccionario (`"n"`, `"hs"`…) que **debe ser no
vacía y única entre los diccionarios corriendo**.

Tipos de valor:

| Tipo de tag | Clase | Cómo se expresa |
|---|---|---|
| **Marcador** (sin valor) | `BSimpleTagInfo` | `defValue = BMarker.MARKER` — es el **default** |
| Tipado | `BSimpleTagInfo` | cualquier `BIDataValue` en `defValue`, serializado como `valueType + default` |
| Enumerado | `BDynamicEnumTagInfo` | añade `enumRange: BEnumRange`; el ordinal se actualiza al cambiar el rango |

`[INFER]` Que el marcador sea el **valor por defecto** confirma la orientación Haystack del modelo: la mayoría
de los tags no llevan dato, solo presencia.

## 260.4 — Directo vs implícito: el framework solo gobierna lo implícito `[CERT]`

**Los tags directos no tienen tipo público en este paquete** — son propiedades del componente, gobernadas por
`javax.baja.tag.Entity` (otro paquete, fuera de alcance). Lo que este framework aporta es **todo lo
implícito**:

| Método | Dónde | Qué resuelve |
|---|---|---|
| `getImpliedTag(Id, Entity)` | `BTagDictionaryService` | un tag: índice de implícitos → reglas del Smart Dictionary → relaciones de tag-group |
| `getImpliedTags(Entity)` | `BTagDictionaryService` | todos: primero relaciones de grupo, luego todas las reglas |
| `getImpliedRelation(Id, Entity)` | `BTagDictionaryService` | ídem para relaciones |
| `getImpliedTagInfo(Id, Entity)` | `BSmartTagDictionary` | dentro de un diccionario: prueba las reglas en orden |
| `getValidTags(Entity)` | `BTagDictionary` | filtro por `validity.test(entity)` |

**La pieza central es `BSmartTagDictionary`**: extiende `BTagDictionary` y agrega `tagRules: BTagRuleList`. Una
`BTagRule` es una **condición → payload** (lista de tags + lista de grupos + lista de relaciones), y
`BScopedTagRule` le suma un `scopeList` que filtra por ruta.

La `validity` (un `BTagRuleCondition`, default `BAlways`) está en `BTagInfo` **y** en `BTagGroupInfo`
(`$T/BTagInfo.java:38-42`) — es el filtro de elegibilidad por definición, con dos vías: `testIdealMatch(Type)`
por tipo y `test(Entity)` por instancia.

`[INFER]` El mecanismo por el que un componente "se une" a un grupo de tags es una **relación**
(`TAG_GROUP_RELATION`): si existe un knob de relación con ese Id apuntando a un `BTagGroupInfo`, todos los tags
del grupo quedan implícitos. Eso convierte a las relaciones en infraestructura del tagging, no en un anexo —
y justifica que T3 sea un gap propio.

## 260.5 — `data/`: una política y un muerto `[CERT]`

- **`BDataPolicy`** — base abstracta de las políticas de datos (cómo se actúa sobre el dato de un tag). Se
  monta como hijo opcional de `BTagInfo` o `BTagGroupInfo`, **máximo uno**, y **un tag marcador no puede
  tener política** (`$T/BTagInfo.java:122-126`) `[CERT]`. `[INFER]` Coherente: sin valor no hay dato que
  gobernar.
- **`BTagGroupMonitor`** — `@Deprecated` en la línea 31 `[CERT]`, con todos los métodos `do*` vaciados. Lo
  único vivo es el guard que **bloquea agregarlo en runtime** permitiendo aún decodificarlo para migración; el
  servicio lo elimina solo al migrar `schemaVersion` de 0 a 2.

`[INFER]` Es el primer `@Deprecated` real que encuentra el corpus en estos dos focuses — recordar que en las
67 clases del chart clásico la búsqueda dio **cero** ([Bloque 254] §254.8). Acá el framework sí marca y
entierra a sus muertos.

## 260.6 — Hallazgo de seguridad: el bypass público del candado `frozen` `[CERT]`

`BTagDictionary` tiene una propiedad `frozen` que protege las definiciones de un diccionario. El guard vive en
`BInfoList`:

```java
   boolean checkContext(Context context) {
      return context == null
         ? true
         : !this.isRunning() && (context.equals(Context.decoding) || context.equals(Context.commit)) || context.equals(BTagDictionary.importContext);
   }
```
`$T/BInfoList.java:64-67` `[CERT]`

El último término acepta cualquier llamada cuyo contexto sea `BTagDictionary.importContext`. Y ese campo es:

```java
   public static Context importContext = new BasicContext();
```
`$T/BTagDictionary.java:156` `[CERT]`

**`public`, `static` y NO `final`.** `[INFER]` Dos consecuencias distintas y ambas incómodas:

1. **Bypass de lectura**: cualquier código en la JVM puede leer ese campo y pasarlo como contexto para
   **escribir en un diccionario congelado**. El candado `frozen` protege contra el error humano en la UI, no
   contra código.
2. **Superficie de manipulación**: al no ser `final`, otro módulo cargado en la station puede **reemplazar la
   instancia**. `[INFER]` No se afirma explotabilidad — habría que ver el orden de carga de módulos y qué
   valida el destino; se registra la forma, no un exploit.

`[INFER]` Encaja con el patrón que el corpus viene documentando: los controles de Niagara son fuertes en el
borde (`requiredPermissions` en la vista, [Bloque 254] §254.5) y laxos dentro del proceso.

## 260.7 — Otros gotchas verificados `[CERT]`

**a) `isImportRequired()` con AND triple** `[CERT]`:
```java
      return this.getTagDefinitions().iterator().hasNext()
            && this.getTagGroupDefinitions().iterator().hasNext()
            && this.getRelationDefinitions().iterator().hasNext()
```
`$T/BTagDictionary.java:326-328` — solo se considera "no hace falta importar" si **las tres** listas tienen al
menos un elemento. `[INFER]` Un diccionario legítimo con tags y grupos pero **cero relaciones** dispara una
importación automática al arrancar, desde `importDictionaryOrd` (default `local:|file:~shared`).

**b) Excepción tragada en `BDataPolicy.getDataPolicyForTag()`** `[CERT]`:
```java
         } catch (Exception var3) {
            var3.printStackTrace();
         }
```
`$T/data/BDataPolicy.java:26-28` — cualquier fallo se convierte en `Optional.empty()` con un stack trace a
stderr. `[INFER]` Indistinguible de "no hay política definida".

**c) Caché estática compartida** `[CERT]`: `private static Map<String, Optional<SmartTagDictionary>>
smartTagDictionaryCache` (`$T/BTagDictionaryService.java:153`) — compartida por **todas** las instancias del
servicio en la misma JVM.

**d) `maxImportFileSize`** — propiedad **oculta** (flags=4), default **1024 KB**
(`$T/BTagDictionary.java:145`), aplicada en `checkImportFileSize(BIFile)`.

**e) Contratos documentados solo por excepción**: `BTagRuleCondition.encodeToJson/decodeFromJson` lanzan
`LocalizableRuntimeException("tagdictionary", "export.noEncodeMethod", …)` si no se sobreescriben
(`$T/BTagRuleCondition.java:42-48`; mismo patrón en `BTagRuleScope`). `[INFER]` Una subclase de terceros
compila perfecto y **falla recién al exportar**.

**f) Unicidad de namespace evaluada solo contra diccionarios válidos** — un diccionario deshabilitado con el
mismo namespace no bloquea a otro; el conflicto aparece al re-habilitarlo.

## 260.8 — Conexiones

- **[Bloque 21]** — confirma su §21.1 (jerarquía) y §21.3 (estándar vs custom) desde el contrato, y aporta lo
  que un bloque de espinazo no tenía: montaje/descubrimiento (§260.1), licenciamiento (§260.2), y el bypass de
  `frozen` (§260.6).
- **[Bloque 82]** — los diccionarios OEM Honeywell (`honTagDictionary`, `fcTagDict`) son instancias de este
  contrato; su namespace y su montaje siguen las reglas de §260.1.
- **[Bloque 253]/[Bloque 255]** (chart) — el contraste de mecanismo de extensión: allá registro de agentes,
  acá montaje en el servicio.
- **[Bloque 254]** §254.8 — contraste de licenciamiento: el chart no tiene gate, el tagging sí (§260.2).
- **[Bloque 256]** §256.1 — el mismo patrón de llamada de framework por opcode (`fw(303)` allá, `fw(501)`
  acá).
- **Gaps que este bloque deja servidos**: T3 (las relaciones aparecen como infraestructura del tagging,
  §260.4), T4 (`neqlizeExcludedTags`/`neqlizeExcludedRelations` son slots del diccionario — la exclusión de la
  traducción a NEQL se configura acá y se ejecuta en `neqlize/`), T2 (el motor), T8 (la UI).
