# Bloque 414 — Niagara Network Supervisor (I): resolución wb-vs-rt de `BSubstitutePxView` — veredicto N1

> **Qué documenta**: la cadena de tipos `BSubstitutePxView` → `BAbstractSubstitutePxView` y su distribución
> entre perfiles `-wb` y `-rt`, con el fin de cerrar el gap **N1** del focus
> `niagara-network-supervisor`: determinar si el riesgo de [Bloque 267] §267.3 es real (un JACE no puede
> resolver `exportTags:SubstitutePxView` al levantar su BOG) o si existe una pieza en `-rt` que lo mitiga.
>
> **Alcance**: `exportTags-wb`, `exportTags-rt` y `niagaraDriver-wb` — solo los archivos relevantes a la
> cadena de herencia PX y al mecanismo de deserialización BOG. NO re-documenta el join ni la UI (→ B266/B267).
>
> **Subject version**: Niagara N4 4.14.0.162 · build 2024-05-28 (`exportTags-rt.jar` / `exportTags-wb.jar`)
>
> **Fuentes** (decompilado; alias de ruta):
> - `$W` = `.../exportTags/exportTags-wb/decompiled/com/tridium/exporttags/`
> - `$E` = `.../exportTags/exportTags-rt/decompiled/com/tridium/exporttags/`
> - `$NW` = `.../niagaraDriver/niagaraDriver-wb/decompiled/com/tridium/nd/`
> - Raíz: `/home/cristian/modules/Prototipos/modulos/organized/`
> - `$RT_XML` = `.../exportTags/exportTags-rt/extracted/META-INF/module.xml`
> - `$WB_XML` = `.../exportTags/exportTags-wb/extracted/META-INF/module.xml`
>
> **Método**: lectura inline de los `module.xml` de ambos perfiles + cadena de herencia en los `.java`
> decompilados (CFR + Vineflower presentes para wb; CFR para -rt) + análisis de flujo de `doJoin()` para
> determinar en qué árbol de estación se persiste `BSubstitutePxView`. **5 tokens** verificados inline.
> Marcadores: `[CERT]` = fuente primaria (file:line); `[INFER]` = deducción.

---

## 414.1 — `SubstitutePxView` y `PxViewTag`: ÚNICAMENTE en `exportTags-wb`, ninguno en `-rt` `[CERT]`

El `module.xml` del perfil `-rt` registra **24 tipos**, ninguno PX-related
`[CERT]` `$RT_XML:28-65`. La sección `<types>` cierra en `FileImportTag` y no hay ninguna entrada con
`PxView`, `Substitute`, ni `Px`. Comparación exhaustiva:

| Tipo | `exportTags-rt` module.xml | `exportTags-wb` module.xml |
|---|---|---|
| `SubstitutePxView` | **ausente** | `:94` `[CERT]` |
| `PxViewTag` | **ausente** | `:93` `[CERT]` |
| `PxViewTagValidationJob` | **ausente** | `:95` `[CERT]` |
| `HistoryImportTag` … `FileImportTag` (7 tipos) | `:58-64` | ausentes |

El `module.xml` de `-wb` los declara bajo `runtimeProfile="wb"` `[CERT]`
(`exportTags-wb/extracted/META-INF/module.xml:2`). El `module.xml` de `-rt` deja visibles solo en modo
workbench, declarando explícitamente `exportTags-wb` como módulo-parte con perfil restringido
`[CERT]` (`exportTags-rt/extracted/META-INF/module.xml:90`):

```xml
<moduleParts>
  <modulePart name="exportTags-wb" runtimeProfile="wb"/>
</moduleParts>
```

`[INFER]` Esta declaración es la prueba formal de que un JACE en runtime solo carga `-rt` y nunca
registrará los tipos PX en su NRE.

## 414.2 — `BAbstractSubstitutePxView` (superclase): en `niagaraDriver-wb`, no en `-rt` `[CERT]`

`BSubstitutePxView` extiende `BAbstractSubstitutePxView` `[CERT]`
(`exportTags-wb/decompiled/com/tridium/exporttags/tags/px/BSubstitutePxView.java:36`):

```java
public final class BSubstitutePxView
extends BAbstractSubstitutePxView {
```

La superclase **no vive en `exportTags-wb`** ni en ningún módulo `-rt`. Vive en
**`niagaraDriver-wb`** `[CERT]`
(`niagaraDriver-wb/decompiled/com/tridium/nd/ui/px/BAbstractSubstitutePxView.java:107`):

```java
public abstract class BAbstractSubstitutePxView
extends BDynamicPxView {
```

Cadena de herencia completa (todos los eslabones en perfiles `-wb`):

```
BSubstitutePxView        [exportTags-wb]
  └─ BAbstractSubstitutePxView [niagaraDriver-wb]
       └─ BDynamicPxView       [baja o bajaui-ux — no investigado en esta iteración]
```

`[INFER]` Incluso si hipotéticamente se moviera `BSubstitutePxView` a `-rt`, no podría cargarse porque
`BAbstractSubstitutePxView` tampoco está en `-rt`. El acoplamiento a `-wb` es en toda la cadena.

## 414.3 — Dónde se persiste `BSubstitutePxView`: en el árbol del SUPERVISOR, no del JACE `[CERT]`

Este es el hallazgo arquitectural que resuelve el riesgo de [Bloque 267] §267.3. El `doJoin()` de
`BPxViewTag` crea el slot `BSubstitutePxView` en `this.getTargetParent()` `[CERT]`
(`exportTags-wb/decompiled/com/tridium/exporttags/tags/px/BPxViewTag.java:307`):

```java
BSubstitutePxView view = (BSubstitutePxView)NiagaraVirtualUtil.findInstance(
    (BComponent)this.getTargetParent(), (IFilter)filter, (Type)BSubstitutePxView.TYPE);
```

`getTargetParent()` devuelve `this.targetParent` `[CERT]`
(`exportTags-rt/decompiled/com/tridium/exporttags/BNiagaraExportTag.java:253`),
que se asigna en `preJoin()` resolviendo `stationSlotPath` **relativo al parámetro `station`** `[CERT]`
(`exportTags-rt/decompiled/com/tridium/exporttags/BNiagaraExportTag.java:220`):

```java
this.targetParent = this.resolveStationSlotPath(station, job, cx);
```

El parámetro `station` en el contexto de `BSupervisorJoinJob` es el **objeto `BNiagaraStation`
que el SUPERVISOR mantiene en su propio árbol** para representar a la subordinada — un proxy, no
la station real del JACE. El espacio `slot:virtual` que `doJoin()` construye
(`$W/tags/px/BPxViewTag.java:266`) vive dentro de ese proxy, en el `config.bog` del SUPERVISOR.

`[INFER]` El JACE (subordinada) **nunca persiste `exportTags:SubstitutePxView` en su propio BOG**.
Almacena únicamente los tags de declaración (`BPxViewTag`, `BPointTag`, etc.). El riesgo de B267 §267.3
apuntaba al lado equivocado: era el SUPERVISOR quien necesitaba resolver `BSubstitutePxView`, no el JACE.

## 414.4 — Mecanismo de deserialización BOG para tipos no resueltos: `BlacklistTypeResolver` `[CERT]`

`BSupervisorJoinJob` usa un `BlacklistTypeResolver` propio al decodificar el BOG de la subordinada
`[CERT]` (`exportTags-rt/decompiled/com/tridium/exporttags/BSupervisorJoinJob.java:300`):

```java
private static class BlacklistTypeResolver
extends ValueDocDecoder.BogTypeResolver {
    ...
    public BValue newInstance(ValueDocDecoder decoder, BComplex parent,
            String propName, Property prop, String typeStr) {
        BValue result = super.newInstance(decoder, parent, propName, prop, typeStr);
        if (result == null) {
            return null;  // tipo no resuelto → elemento omitido  // :310
        }
        if (BFoxChannel.isBlacklistedLegacyType((Type)result.getType())) {
            ...
            decoder.skip();
            return null;
        }
        return result;
    }
}
```

`[CERT]` `super.newInstance()` es el resoltor estándar de Niagara
(`exportTags-rt/decompiled/com/tridium/exporttags/BSupervisorJoinJob.java:310`);
cuando el tipo no está registrado en el NRE, devuelve `null`. El `BlacklistTypeResolver` propaga ese
`null` sin lanzar excepción, y el elemento queda ausente del árbol decodificado.

`[INFER]` Consecuencia: si el SUPERVISOR corre sin `exportTags-wb` (perfil `-rt` o `-se` — p.ej. un
JACE-class actuando de supervisor), `BPxViewTag` no puede resolverse al decodificar el BOG de la
subordinada → `doJoin()` jamás se llama → **no se crea ningún slot `BSubstitutePxView`** en el árbol
virtual del supervisor. La distribución de vistas PX falla silenciosamente, sin error en el log de join
sobre los otros tags (PointTag, HistoryImportTag, etc.).

## 414.5 — Riesgo real redefinido `[INFER]`

El gap N1 preguntaba si el JACE-destino puede resolver `SubstitutePxView`. La respuesta es que no
necesita hacerlo. Pero existen dos riesgos concretos, distintos al planteado en B267:

| # | Superficie | Condición | Efecto |
|---|---|---|---|
| R1 | SUPERVISOR sin perfil wb | Supervisor es JACE-class (rt/se solamente) | `BPxViewTag` omitido en decode → no se crean `BSubstitutePxView` → join PX inerte, sin error explícito |
| R2 | JACE (subordinada) con `BPxViewTag` en BOG | Workbench agregó `BPxViewTag` al JACE; JACE reinicia | JACE no resuelve `exportTags:PxViewTag` → placeholder no funcional en árbol runtime |

R2 requiere un paso extra de investigación (cómo Niagara maneja tipos no resueltos en BOG de la propia
station al arranque) que no se pudo resolver read-only en esta iteración → encolado como gap nuevo N6.

## 414.6 — VEREDICTO: ¿riesgo real o mitigado? `[CERT]` + `[INFER]`

**El riesgo como lo formuló B267 §267.3 está MITIGADO POR DISEÑO**: `BSubstitutePxView` no se persiste
en el BOG del JACE. No hay ningún tipo equivalente en `exportTags-rt` (verificado sobre los 24 tipos del
`module.xml`) porque no hace falta: el JACE nunca instancia `BSubstitutePxView`.

**El riesgo REAL es en el SUPERVISOR**: requiere `exportTags-wb` (+ `niagaraDriver-wb` para la superclase)
cargado en su NRE para que el join PX funcione. Si el supervisor es un JACE-class, la distribución de
vistas PX falla silenciosamente.

## 414.7 — Conexiones

- **[Bloque 267]** §267.3 — gap que motiva esta investigación; la premisa del riesgo (JACE no resuelve
  `SubstitutePxView`) queda **corregida**: el slot vive en el árbol del SUPERVISOR, no del JACE. Añadir
  en B267 una nota: *"B414 §414.3 corrige la localización: BSubstitutePxView persiste en el SUPERVISOR,
  no en el BOG del JACE."*
- **[Bloque 266]** — runtime del join (`BSupervisorJoinJob`, Fox, BOG, worker 1-hilo); §266.5 credenciales.
  B414 profundiza exclusivamente en la resolución de tipo PX, que B266 no tocó.
- **[Bloque 258]** §258.1 — el criterio `-rt`/`-wb` que hace visible el riesgo de perfil; el mismo patrón
  aplicado aquí a `exportTags`.
