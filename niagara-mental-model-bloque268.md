# Bloque 268 — Tags (IX): la cara de usuario del diccionario — Workbench y navegador

> **Qué documenta**: la UI del diccionario de tags — `tagdictionary-wb/ui/` (4 clases) y `tagdictionary-ux/`
> (5): qué ve y qué hace un operador, en Workbench y en el browser. Cierra el gap **T8** del focus `tags`.
>
> **Contiene la CUARTA verificación de un claim de permisos de este focus** — y la cuarta que resulta en una
> falsa alarma evitada. Ver §268.4, que es tan importante como el contenido técnico.
>
> **Fuentes** (decompilado vineflower):
> - `$U` = `…/tagdictionary/tagdictionary-ux/vineflower/com/tridium/tagdictionary/ux/`
> - `$W` = `…/tagdictionary/tagdictionary-wb/vineflower/com/tridium/tagdictionary/ui/`
> - `$B` = `…/baja/baja/{vineflower,decompiled}/javax/baja/` (para resolver `BComponent`/`BIProtected`)
> - Raíz: `/home/cristian/modules/Prototipos/modulos/organized/`
>
> **Método**: mismo barrido delegado que [Bloque 267] + verificación inline: **6 tokens** re-verificados,
> incluida la cadena de tipos que desarma el hallazgo de §268.4. Marcadores: `[CERT]` = fuente primaria;
> `[INFER]` = deducción. Bloque de EVIDENCIA.

---

## 268.1 — Workbench: el manager y la migración a tag-groups `[CERT]`

`BTagDictionaryManager` es la vista principal, agente sobre el servicio y **exigiendo permiso de escritura**:

```java
   agent = {@AgentOn(
      types = {"tagdictionary:TagDictionaryService"},
      requiredPermissions = "W"
```
`$W/BTagDictionaryManager.java:62-64` `[CERT]`

Tabla de todos los diccionarios (path, nombre, tipo, status, versión, enabled, namespace, faultCause) con tres
comandos: **Import** (JSON o CSV), **Export**, y **Tag Group Report**.

El tercero es el interesante: llama a `BTagDictionaryService.tagsToTagGroup()` y, si encuentra componentes
candidatos, abre `BSelectTagGroupDialog` — una lista con búsqueda, checkboxes y dos columnas extra (Convert /
RemoveTags). Al guardar, **agrega las relaciones `TAG_GROUP_RELATION` y quita los tags individuales** de los
componentes afectados.

`[INFER]` Es la cara de usuario de la utilidad `updateTagGroupRelations()` que [Bloque 261] §261.6 documentó
—la que usa `Context.decoding` para saltear el candado `frozen`—. El operador ve un diálogo de migración; por
debajo se están reescribiendo slots de tags por relaciones.

**Y ahí está el riesgo** `[CERT]`: toda esa migración está envuelta en un `catch (Exception)` que solo hace
`printStackTrace()` (`$W/BTagDictionaryManager.java:202-204`). `[INFER]` Un fallo a mitad de camino deja
componentes **parcialmente migrados** —algunos con relación, otros aún con tags directos— sin rollback, sin
diálogo de error y sin registro operativo. Es el mismo patrón de falla silenciosa que ya apareció en
[Bloque 261] §261.8, [Bloque 262] §262.6 y [Bloque 264] §264.6: **cuatro veces en el mismo subsistema**.

## 268.2 — Navegador: el Tag Manager es bajaux puro `[CERT]`

```java
   agent = {@AgentOn(
      types = {"baja:Component"},
      requiredPermissions = "W"
```
`$U/BTagManager.java:14-18` `[CERT]` — `@NiagaraSingleton`, implementa `BIJavaScript` y `BIFormFactorMax`, y
sirve `module://tagdictionary/rc/TagUxManager.js`.

La cadena de build es la que [Bloque 204] documentó para bajaux: `BTagDictionaryJsBuild` empaqueta
`tagdictionary.built.min.js` declarando dependencia de `BWebEditorsJsBuild` + el `BCssResource` propio.

El segundo widget es `BRelationIdEditor` — `BIFormFactorMini` + **`BIOffline`**, sirviendo
`RelationIdEditor.js`. `[INFER]` Que sea *offline-capable* es coherente con lo que edita: un id de relación es
`namespace:name` ([Bloque 262] §262.2), texto puro que no necesita la station viva para componerse.

`[INFER]` Nota de amplitud: el Tag Manager se registra sobre **`baja:Component`** — o sea, aparece como
pestaña en **todo** componente para un usuario con escritura, sin filtro de "taggeabilidad". No hay chequeo
previo de si el tipo admite tags.

## 268.3 — La superficie RPC que alimenta al navegador `[CERT]`

`BTaggingRpcUtil` expone **6 métodos `@NiagaraRpc`** sobre transporte Box:

| Método | Devuelve |
|---|---|
| `getTagInfos` | tags disponibles con `isIdeal`/`isValid` para el componente objetivo |
| `getTagGroupInfos` | ídem para grupos, con ORD de slot-path como valor |
| `getTagInfosForTagGroup` | tags de un grupo puntual |
| `getTagDictionaryServiceOrd` | el ORD de navegación del servicio |
| `getTagDictionariesInfo` | namespace + display name de cada diccionario habilitado |
| `getRelationIds` | los ids de relación de todos los diccionarios |

`[INFER]` `getTagInfos` devolviendo `isIdeal` conecta directo con [Bloque 263] §263.3: el `testIdealMatch(Type)`
que allí parecía un detalle interno **es lo que filtra la paleta de tags que ve el usuario en el navegador**.
Y como `BBooleanFilter` siempre devuelve `true`, la paleta será más amplia de lo que el nombre sugiere.

## 268.4 — CUARTA verificación de permisos: falsa alarma evitada `[CERT]`

El barrido reportó un hallazgo de seguridad: los 6 RPC declaran `permissions = "unrestricted"` `[CERT]`
(`$U/BTaggingRpcUtil.java:62,109,156,205,216,244`), y el filtro interno es:

```java
   private static boolean hasReadPermission(Object obj, Context cx) {
      return obj instanceof BIProtected ? ((BIProtected)obj).getPermissions(cx).hasOperatorRead() : true;
   }
```
`$U/BTaggingRpcUtil.java:350-352` `[CERT]`

Su conclusión: *"cualquier diccionario que NO implemente `BIProtected` devuelve `true` incondicionalmente…
los diccionarios no protegidos quedan expuestos a cualquier llamador autenticado"*.

**Verificado y desarmado** `[CERT]`:

```java
public class BComponent
extends BComplex
implements BISpaceNode,
BIProtected,
```
`$B/sys/BComponent.java:92-95`

**`BComponent` implementa `BIProtected`.** Y un `BTagDictionary` es un `BComponent` ([Bloque 260] §260.1).
`[INFER]` Por lo tanto la rama `: true` del ternario **nunca se alcanza para un diccionario**: el chequeo real
de `hasOperatorRead()` se ejecuta siempre. Es una guarda defensiva para objetos que no sean componentes, no un
agujero.

El diseño verificado es: **RPC invocable sin permiso especial + filtrado por lectura de operador sobre cada
objeto devuelto** — exactamente el mismo patrón que [Bloque 263] §263.7 encontró en la RPC de neqlize.

### Nota de método: cuatro de cuatro

Este focus acumula **cuatro claims de permisos provenientes de sub-agentes, y los cuatro requerían corrección
o acotación**:

| Bloque | Claim del barrido | Realidad verificada |
|---|---|---|
| [261] §261.7-1 | "el guard de adminWrite se saltea" | los `return` son **rechazos**; el hueco real es otro y menor |
| [261] §261.7-2 | "cualquiera puede exportar" | la invocación de acción ya pasa por el gate del framework |
| [266] §266.6 | "`flags=4` = nivel operador" | `flags=4` es **`Flags.HIDDEN`**, visibilidad, no permiso |
| [268] §268.4 | "diccionarios no protegidos quedan expuestos" | `BComponent` **implementa** `BIProtected`; la rama no se alcanza |

`[INFER]` **Cuatro de cuatro.** No es mala suerte con un agente: es una limitación estructural del barrido
delegado. Un sub-agente lee el archivo que le tocó y no tiene el modelo del framework en la cabeza —no sabe
qué significa `flags=4`, ni qué implementa `BComponent`—, así que interpreta la sintaxis local con semántica
de seguridad. **La regla adoptada** (declarada en [Bloque 266] §266.6 y confirmada acá): todo claim de
permisos se verifica contra la jerarquía de tipos y la semántica real del framework **antes** de escribirse.
En un corpus de referencia, cuatro falsos positivos de seguridad habrían sido cuatro decisiones mal informadas
para quien lo consulte.

## 268.5 — Otros gotchas `[CERT]`

- **Estado estático compartido**: `BTagDictionaryManager` guarda `exportDir`/`exportFile`/`importDir`/
  `importFile` como campos `static` de visibilidad de paquete, sin `volatile`
  (`$W/BTagDictionaryManager.java:76-79`). `[INFER]` Recuerdan la última ruta usada, compartida por todas las
  sesiones de la misma JVM.
- `BSelectFromListDialog` es un diálogo reutilizable con búsqueda y checkboxes; `BSelectTagGroupDialog` lo
  especializa. `TagDictionaryUiUtil` es una única factory estática.

## 268.6 — Conexiones

- **[Bloque 261]** §261.6 — el Tag Group Report es la cara de usuario de `updateTagGroupRelations()`, la
  utilidad que saltea el candado `frozen`.
- **[Bloque 263]** §263.3 — `testIdealMatch` es lo que filtra la paleta que se ve en el navegador (§268.3).
- **[Bloque 262]** §262.2 — `BRelationIdEditor` edita el `namespace:name` documentado allí.
- **[Bloque 204]** (bajaux) y **[Bloque 211]**/**[Bloque 214]** — los mecanismos de UI, reutilizados sin
  variación.
- **[Bloque 260]** §260.1 — la cadena `BTagDictionary` → `BComponent` → `BIProtected` que desarma §268.4.
- **Gap abierto**: T9 (doc oficial, preservada; último del focus).
