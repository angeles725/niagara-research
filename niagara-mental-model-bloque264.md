# Bloque 264 — Tags (V): el módulo `haystack` son DOS diccionarios, no un servidor

> **Qué documenta**: el módulo `haystack-rt` completo (37 clases): qué es, de dónde sale la ontología, y qué
> tags computa. Cierra el gap **T5** del focus `tags`.
>
> **Corrige una suposición de encuadre**: el gap se sembró preguntando *"¿es un diccionario, un servidor REST
> Haystack, o un cliente?"*. La respuesta es **inequívocamente diccionario** — y la ausencia del servidor
> importa tanto como su presencia. Ver §264.1.
>
> **Fuentes** (decompilado vineflower + los recursos embebidos en el jar):
> - `$H` = `…/haystack/haystack-rt/vineflower/com/tridium/haystack/`
> - `$D` = `…/haystack/haystack-rt/extracted/com/tridium/haystack/data/` (recursos)
> - Raíz: `/home/cristian/modules/Prototipos/modulos/organized/`
>
> **Método**: barrido delegado (tier `sonnet`, 53 llamadas) + verificación inline: **9 tokens** re-verificados
> y **1 ausencia re-medida por el driver** (§264.1), donde el barrido reportó "cero coincidencias" y el
> conteo real dio una — que resultó ser el hallazgo más fino del bloque. Marcadores: `[CERT]` = fuente
> primaria; `[INFER]` = deducción. Bloque de EVIDENCIA.

---

## 264.1 — No hay servidor Haystack. Y la única coincidencia lo confirma `[CERT]`

Barrido sobre las 37 clases buscando `servlet|hisRead|watchSub|watchPoll|pointWrite|invokeAction|zinc|BWebServlet`:
**un solo archivo coincide, y no es código** — es `$D/defs.json` `[CERT]`, el recurso de datos de la ontología
Haystack 4.

`[INFER]` El matiz es preciso y vale más que un "cero" liso: **el módulo conoce los nombres de las ops de
Haystack porque la ontología las define como conceptos** (`hisRead`, `watchSub`… son defs del estándar), pero
**no implementa ninguna**. No hay servlet, no hay ruta HTTP, no hay codificación Zinc/Trio, no hay cliente.

**Ámbito de la ausencia**: las 37 clases de `haystack-rt/vineflower/`. Un módulo servidor separado podría
existir en otra parte de la instalación — **UNVERIFIED**, no se buscó fuera de este módulo.

`[INFER]` Consecuencia para un integrador: instalar el módulo `haystack` **no expone la station a clientes
Haystack**. Da vocabulario semántico, no interoperabilidad de protocolo. Quien espere apuntarle SkySpark a un
JACE por tenerlo instalado, se va a encontrar sin endpoint.

## 264.2 — Son DOS diccionarios, uno por versión del estándar `[CERT]`

```java
public class BHsTagDictionary extends BSmartTagDictionary {
   public static final Property namespace = newProperty(1, "hs", null);
```
`$H/BHsTagDictionary.java:111-112` `[CERT]`

```java
public class BHaystack4TagDictionary extends BSmartTagDictionary {
   public static final Property namespace = newProperty(1, "h4", null);
```
`$H/BHaystack4TagDictionary.java:89-90` `[CERT]`

Ambos son `BSmartTagDictionary` — es decir, **instancias del mismo motor** documentado en [Bloque 260] y
[Bloque 261], montadas bajo el servicio como cualquier otro diccionario. Namespaces distintos: **`hs`**
(Haystack 3) y **`h4`** (Haystack 4).

Y hay un `BHaystack3To4MigrationJob` que recorre el espacio de componentes de la station reescribiendo tags y
relaciones `hs:*` a `h4:*` — con la **única verificación de permisos del módulo**: `checkIsSuperUser()`
(`$H/BHaystack3To4MigrationJob.java:165-169`) `[CERT]`.

`[INFER]` Que la migración exija superusuario y nada más lo exija dice cuál es la operación peligrosa: reescribir
la semántica de toda la station.

## 264.3 — La ontología viene embebida como datos, no como código `[CERT]`

| Estándar | Archivos en el jar | Tamaño real medido |
|---|---|---|
| **Haystack 3** (`hs`) | `tags.csv` + `equip.csv` | **233 líneas** (232 tags/relaciones + cabecera) y **229 líneas** (228 grupos equip) `[CERT]` |
| **Haystack 4** (`h4`) | `defs.json` + `protos.json` | **497.496 bytes** de `defs.json` `[CERT]` (658 defs según el importador) |
| Mapeo N4↔HS4 | `HS4toN4.json` | reglas de traducción, sobreescribible por el usuario |

`[CERT]` Ambos diccionarios admiten **archivos de merge del usuario**: `tagsMerge.csv`/`equipMerge.csv` para
`hs` (`$H/BHsTagDictionary.java:200-213`) y `haystack4NiagaraConfig.json` para `h4`
(`$H/Haystack4Importer.java:139-153`), que puede **sobreescribir el mapeo embebido**.

`[INFER]` Esto cierra el hilo que abrió el gap T10 ([Bloque 262] §Estado): igual que Brick, Haystack es
**contenido sobre el motor genérico**. La diferencia es de volumen, no de naturaleza — Brick son 2 clases y un
JSON; Haystack son 37 clases, pero **35 de ellas son tags computados**, no motor.

## 264.4 — Las 37 clases: casi todas son tags computados `[CERT]`

Siguiendo el patrón que [Bloque 261] §261.2 encontró en el diccionario `n`, el grueso del módulo son
`BTagInfo` que **calculan su valor en cada consulta**:

| Familia | Tags que computa |
|---|---|
| Identidad | `hs:id` (handle ORD) · `h4:id` (string de ruta) |
| Estado del punto | `curVal`, `curStatus`, `curErr`, `curDis` (`@Deprecated`), `kind`, `enum` |
| Facets numéricos | `unit`, `maxVal`, `minVal` (sobre `BNumericFacetTag`) |
| Historias | `hisInterpolate`, `hisMode`, `hisStatus`, `hisErr` — leídos de `BHistoryExt` |
| Punto escribible | `writeVal`, `writeStatus`, `writeErr`, `writeLevel` (1-17, donde 17 = relinquish default) |
| Referencias | `equipRef`, `networkRef` (tags) + relaciones implícitas `BEquipRelation`, `BSiteRelation`, `BSpaceRelation`, `BSystemRelation` |
| Zona horaria | `tz` |

`[INFER]` Las relaciones implícitas (`equipRef`/`siteRef`/`spaceRef`/`systemRef`) son la pieza que conecta
este bloque con [Bloque 262]: son `BRelationInfo` que **caminan la jerarquía buscando el ancestro con el
marcador correspondiente**. Es el mismo mecanismo de navegación estructural, aplicado al modelo semántico de
Haystack en vez de al de Niagara.

## 264.5 — Tres defectos semánticos `[CERT]`

**a) `tz` es global de la station, no por entidad** `[CERT]`:
```java
      BTimeZone tz = BAbsTime.now().getTimeZone();
```
`$H/BTzTag.java:36` — es la zona horaria **de la JVM**. `[INFER]` Todo componente de la station reporta el
mismo `tz`, sin importar dónde esté físicamente el equipo. En Haystack, `tz` es una propiedad **de la
entidad** — un site en Cancún y otro en Chihuahua deberían diferir. Para una integración multi-sitio esto es
un dato incorrecto, no una limitación cosmética.

**b) `equipRef` tiene dos implementaciones que no coinciden** `[CERT]`: `BEquipRefTag` sube hasta el
`BDevice` ancestro más cercano y devuelve su handle ORD (`$H/BEquipRefTag.java:38-42`), mientras
`BEquipRelation`/`BContainmentRelation` buscan el ancestro con el **marcador `equip`**. `[INFER]` Son
criterios distintos: un `BDevice` no necesariamente está tagueado como `equip`, y un componente tagueado como
`equip` no necesariamente es un `BDevice`. Coexisten dos respuestas posibles para la misma pregunta.

**c) `h4:id` no es un ref estándar de Haystack** `[CERT]`: se arma como
`"nspace:<station>~slot:.<ruta>"` (`$H/BHaystack4IdTag.java:43-71`) — un handle ORD de Niagara con `$`
sustituido por `~`. `[INFER]` Un cliente Haystack externo que espere un `@ref` o un UUID no lo va a parsear
sin conocimiento específico de Niagara. Coherente con §264.1: el módulo no fue pensado para hablar con
clientes externos.

## 264.6 — Otros gotchas `[CERT]`

- **Import en hilo de fondo que falla en silencio**: `BHaystack4TagDictionary.started()` lanza un hilo de
  importación si el diccionario está vacío; el `catch` pone el status en `fault` **sin loguear a WARNING ni
  avisar al operador** (`$H/BHaystack4TagDictionary.java:112-124`) `[CERT]`. `[INFER]` Mismo patrón de falla
  silenciosa que ya apareció en [Bloque 261] §261.8 y [Bloque 262] §262.6 — es un rasgo del subsistema, no un
  descuido aislado.
- **Sin gates de licencia** en las 37 clases (barrido `license|LicenseManager|BLicense` → 0) `[CERT]`.
  `[INFER]` El licenciamiento está en el servicio de tags ([Bloque 260] §260.2), no por diccionario: quien
  paga la feature `tags` puede montar Haystack sin costo adicional.
- **`dis` no lo genera este módulo** `[CERT]`: figura en la lista de exclusiones pero no hay `BDisTag`. `[INFER]`
  Debe venir del `BDisplayNameTag` del namespace `n` ([Bloque 261] §261.2) — el modelo Haystack se apoya en el
  diccionario de Niagara para su campo de display.
- **Sin ofuscación ZKM** en las 37 clases.

## 264.7 — Conexiones

- **[Bloque 21]** §21.2 — aquel bloque documentó el *mapeo* Haystack a nivel espinazo; éste documenta el
  módulo y **corrige la impresión de que hubiera protocolo**: no lo hay (§264.1).
- **[Bloque 260]**/**[Bloque 261]** — Haystack es una **instancia** del motor genérico; sus 35 tags computados
  siguen el mismo patrón que los del namespace `n`.
- **[Bloque 262]** — las relaciones implícitas `equipRef`/`siteRef`/`spaceRef` usan el mecanismo de navegación
  estructural documentado allí.
- **T10 (Brick)** — §264.3 refuerza la tesis: las ontologías en N4 son **contenido**, no motores.
- **Gaps abiertos**: T6-T8 (exportTags + UI), T9 (doc oficial, preservada y pendiente de back-fill), T10
  (Brick).
