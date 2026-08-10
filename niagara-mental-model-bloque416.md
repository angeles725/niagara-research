# Bloque 416 — Niagara Network Supervisor (III): la guía oficial de Export Tags — lo que resuelve, lo que matiza y lo que agrega

> **Qué documenta**: qué aporta la **guía oficial Tridium de Export Tags** sobre el mecanismo
> supervisor↔subordinada que el decompilado de B266/B267/B414/B415 ya describió desde el código.
> Cierra el gap **N3** del focus `niagara-network-supervisor`.
>
> **Método distinto a los tres bloques anteriores**: no es un barrido de código. Es un contraste
> **doc-vs-código** sobre la fuente preservada, organizado en cuatro categorías: lo que **resuelve**
> una pregunta abierta del corpus, lo que **matiza** un hallazgo, lo que **agrega** workflow y
> conceptos que el código no expone, y **lo que la doc NO resuelve**.
>
> **Subject version**: Niagara N4 4.14 (document revision: AX-3.7u1, last updated 2013-05-30)
>
> **Fuente `[CERT-doc]`** (preservada, gate e3 `CERTIFIABLE-NOW`, registrada en `SOURCES.md`):
> - `sources/manuals/docExportTags-N4.14-guide.md` — 86 secciones, 181 KB
>
> **Fuentes del corpus** referenciadas:
> - `[Bloque 266]` — runtime del join (`BSupervisorJoinJob`) y transporte de credenciales
> - `[Bloque 267]` — UI y `BPxViewTag` / distribución de vistas PX
> - `[Bloque 414]` — veredicto `BSubstitutePxView` supervisor-side (riesgo wb-vs-rt)
> - `[Bloque 415]` — `niagaraDriver-rt`: modelo device/proxy del join
>
> Marcadores: `[CERT-doc]` = doc oficial preservada · `[CERT]` = código ya citado en bloques previos ·
> `[INFER]` = deducción. Bloque de **DOC-SÍNTESIS** — ratio alto `[INFER]`/`[CERT]` es ESPERADO y
> sano; no indica agotamiento del gap (§11).

---

## 416.1 — RESUELVE: el flujo conceptual del Join y el origen de la "conexión extra" de B266 `[CERT-doc]`

[Bloque 266] documentó el runtime de `BSupervisorJoinJob` pero no su flujo conceptual completo
desde la perspectiva del operador. La doc lo explica y nombra las dos direcciones del Join:

> *"You can issue an export tag Join on a station from either the Supervisor station or from the
> subordinate station, as needed—you get the same results. Issuing a Join from the subordinate's
> Join Profile Manager ends with a built-in Job Log popup dialog that lists all changes made in the
> Supervisor as a result of the join."*
> — `sources/manuals/docExportTags-N4.14-guide.md` §*About a Join* `[CERT-doc]`

El Join desde la subordinada agrega una conexión inicial que [Bloque 266] reportó como parte del
protocolo sin explicar su razón:

> *"If the Join is issued from the subordinate station, the same process steps occur as shown in
> Figure 9, plus an extra, initial connection step from the subordinate to the Supervisor … This
> method allows a new subordinate to be added to the NiagaraNetwork of the Supervisor even in cases
> where the Supervisor is 'unaware' of the remote host."*
> — §*About the Join process* `[CERT-doc]`

`[INFER]` La conexión extra que [Bloque 266] §266.X vio en el código tiene propósito operativo
claro: permite que una subordinada nueva se añada a un supervisor que **no la conoce aún** — sin
intervención previa en el Supervisor. El `BSupervisorJoinJob` que B266 documentó es el mismo job
en ambas direcciones; lo que cambia es el iniciador y la presencia o ausencia de esa conexión
inicial.

## 416.2 — RESUELVE: las propiedades del JoinProfile y el transporte de credenciales de B266 §266.5 `[CERT-doc]`

[Bloque 266] §266.5 documentó que `BConnectInfo` transporta credenciales como **parámetro de
acción serializado** sobre Fox, sin nombrar exactamente qué credenciales ni cómo se configuran.
La doc nombra el conjunto completo de properties en el `JoinProfile`:

> *"Default Subordinate User — This is the local (JACE) station user account to be used by the
> Supervisor for Fox client connection … Default Subordinate User Password — This is an added
> property in AX-3.7u1 and later. The local (JACE) station user password … Default Subordinate
> IP Address … Default Subordinate Port … Default Subordinate Use Fox SSL (New starting in AX-3.7,
> by default false) … Default Subordinate Fox SSL Port"*
> — §*Editing the Join Profile* `[CERT-doc]`

`[INFER]` Las **seis properties "Default"** son exactamente lo que `BConnectInfo` (B266) empaqueta
para el Supervisor cuando la subordinada no existe aún en su `NiagaraNetwork`. El dato
operativamente más relevante es **`Default Subordinate Use Fox SSL = false`** por defecto: si el
operador no cambia este valor y el JACE soporta Foxs, las credenciales viajan sin TLS — exactamente
el riesgo anotado en [Bloque 266] §266.5 como investigable para N4.

La doc también confirma el fallo silencioso de autenticación que B266 mencionó:

> *"You must configure the Default Subordinate User Password when pre-engineering a subordinate
> station to join to a Supervisor. Failure to configure the password will result in authentication
> errors when the Supervisor attempts to make a connection to this subordinate JACE."*
> — §*Editing the Join Profile* `[CERT-doc]`

`[INFER]` Este error de autenticación aparece en el Job Log del Join (`sources/manuals/...` Fig. 22),
no como una excepción que propague al operador. Es una falla silenciosa desde el punto de vista de
la station: el Join fracasa, la subordinada no queda añadida, y solo el log lo documenta.

## 416.3 — RESUELVE: el rol del profile.bog como plantilla de la NiagaraStation en el Supervisor `[CERT-doc]`

[Bloque 266] §266.1 describió la descarga del BOG como parte del join sin explicar qué es el BOG
ni para qué sirve. La doc aclara que se trata de un archivo **separado** del BOG de la station:

> *"When you enable the JoinProfile … a new folder with a station profile.bog file is automatically
> created in the JACE station's file space … You routinely edit and save this bog file, as it can
> determine property values of the NiagaraStation (that represents this JACE) on the Supervisor.
> These values are written to the Supervisor upon any export tag Join."*
> — §*About the niagaraStation profile.bog file* `[CERT-doc]`

`[INFER]` El `profile.bog` no es la base de datos de la station; es una **plantilla editable** que
vive en el file space (`file:^joinProfiles/SupervisorStationName_profile.bog`) y que modela cómo
quedará la `NiagaraStation` del Supervisor después del join — incluyendo propiedades de sus device
extensions (Alarms, Users, etc.). Esto explica por qué [Bloque 415] §415.3 veía que las properties
de la NiagaraStation en el Supervisor "revertían" tras un rejoin: son sobrescritas desde el profile.bog
de la subordinada en cada Join.

> *"Note that upon any export tag 'Join', these property values overwrite any property values in
> the corresponding NiagaraStation component in the Supervisor station."*
> — §*Device extension properties importance in profile.bog file* `[CERT-doc]`

## 416.4 — MATIZA: `BSubstitutePxView` es supervisor-side por diseño (B414 confirmado) `[CERT-doc]`

[Bloque 414] §414.3 dedujo por análisis estático que `BSubstitutePxView` se persiste **en el árbol
del supervisor**, no en el BOG del JACE. La doc lo confirma desde la perspectiva operativa:

> *"Following a Join, the Supervisor station has a 'SubstitutePxView' slot created for each PxViewTag,
> at the designated 'Station Slot Path' under the NiagaraStation that represents that (subordinate)
> JACE."*
> — §*About the Supervisor SubstitutePxView* `[CERT-doc]`

Y sobre el mecanismo de sustitución de ORDs en runtime — que [Bloque 267] §267.X documentó por código:

> *"The Px XML (in the copied Px file) is actually unchanged from the original, as ord substitution
> happens at runtime."*
> — §*About the Supervisor SubstitutePxView* `[CERT-doc]`

`[INFER]` El veredicto de B414 queda **CERT-doc confirmado**: el slot `SubstitutePxView` se crea en
la `NiagaraStation` del supervisor (nunca en el JACE). El archivo .px copiado es idéntico al original;
la sustitución de ORDs es enteramente runtime en el supervisor. El riesgo de B414 (supervisor sin perfil
`-wb`) sigue en pie: si el supervisor no carga `exportTags-wb`, la clase `BSubstitutePxView` no
resuelve y el slot queda como tipo desconocido.

## 416.5 — MATIZA: el Join es un merge con inteligencia, no una sobrescritura total `[CERT-doc]`

[Bloque 266] §266.1 documentó el merge a nivel de código; la doc lo contextualiza:

> *"A Join merges export tags and join profile information found in the remote subordinate station with
> any existing NiagaraStation representation, before making any changes. Existing Niagara proxy points,
> imported histories, and so on, are not arbitrarily overwritten—instead, merge 'intelligence' is
> applied. For example, points previously added using the Niagara Proxy Point Manager, and imported
> histories are typically retained."*
> — §*About a Join* `[CERT-doc]`

`[INFER]` El comportamiento es **híbrido**: componentes creados manualmente en el Supervisor coexisten
con los creados por export tags. Sin embargo, **las properties del profile.bog SÍ sobrescriben** las
properties de la NiagaraStation (§416.3). La "inteligencia de merge" aplica a **componentes** (no se
borran puntos proxy existentes), no a **properties** (que se sobreescriben desde el bog).

## 416.6 — MATIZA: CategoryFilters — top-down, no aditivos (gap category/ de B266) `[CERT-doc]`

[Bloque 266] documentó el paquete `category/` sin detallar la semántica de evaluación. La doc
aclara un comportamiento crítico:

> *"The order of CategoryFilters can be important, as they are evaluated in order from 'top down',
> as only one CategoryFilter (and associated category mask) is applied to any NiagaraStation. In
> other words, category masks are not 'multiplexed' or 'additive'."*
> — §*About the Category Filter Manager* `[CERT-doc]`

`[INFER]` Solo **un** CategoryFilter aplica por NiagaraStation — el primero que hace match en orden
top-down. Esto es un comportamiento no deducible del código sin la semántica de negocio: un ingeniero
que espere que múltiples filtros se sumen (estilo bitmask OR) obtendrá un resultado diferente.

La fuente de la clasificación es `StationInfo` — metadata que se añade en el JoinProfile de la
subordinada y se exporta al supervisor en cada Join:

> *"Station Info allows you to add string-value metadata about the subordinate (JACE) station … Upon a
> Join, this metadata is exported to the Supervisor as properties of the 'Station Info' extension of
> the NiagaraStation component that represents each JACE station."*
> — §*About Station Info* `[CERT-doc]`

## 416.7 — AGREGA: workflow de commissioning que el código no expone `[CERT-doc]`

La guía documenta el **flujo completo de puesta en marcha** que ningún barrido de código puede derivar:

**Orden de pasos de commissioning** `[CERT-doc]` §*Export Tags Quick Start*:

1. Instalar `exportTags` module en cada JACE.
2. Añadir `SupervisorExportTagNetworkExt` al `NiagaraNetwork` del Supervisor (una vez).
3. Por cada JACE: añadir `SubordinateExportTagNetworkExt` al `NiagaraNetwork` del JACE.
4. Habilitar el `JoinProfile` bajo la `NiagaraStation` que representa al Supervisor.
5. Verificar/editar el `profile.bog` del JACE.
6. "Tag up" la station: arrastrar export tags desde la paleta.
7. Ejecutar Join (desde Supervisor o desde JACE).

`[INFER]` Este workflow explica la topología exacta de los componentes que [Bloque 415] §415.2
documentó a nivel de clase (`BNiagaraNetwork`, `BNiagaraStation`): el `SubordinateExportTagNetworkExt`
se instala **como hijo del `NiagaraNetwork`** en la subordinada, y crea automáticamente un
`JoinProfile` bajo cada `NiagaraStation` en esa red. Solo el JoinProfile del Supervisor (la station
representada como `BNiagaraStation` en [B415]) se habilita.

## 416.8 — AGREGA: BFormat variables en Station Slot Paths `[CERT-doc]`

La doc introduce un mecanismo de variables de formato que el código de B266/B267 no nombra:

> *"%networkFolderPath% — This replicates container structure in the JACE station from the 'network
> downwards', that is, below the network (but including the device). Using the following station slot
> path value (for both PxViewTags and PointTags) can provide consistent results:
> slot:points/%networkFolderPath%"*
> — §*BFormat options in Station Slot Paths* `[CERT-doc]`

Variables disponibles: `%networkFolderPath%`, `%deviceFolderPath%`, `%parent.name%`, más la sintaxis
`$(slotName)` para referencias a `StationInfo` en `Station Folder Path` del supervisor.

`[INFER]` Estas variables son la capa de parametrización que permite **replicar jerarquía** del JACE
en el Supervisor sin escribir rutas absolutas en cada export tag. Son funcionalidad de la capa UI
(`exportTags-wb`) que los 106 archivos de `niagaraDriver-rt` (B415) nunca necesitarían resolver.

## 416.9 — AGREGA: licencia de "virtual points" — JACEs excluidos `[CERT-doc]`

Un hallazgo operativo directo del FAQ que el código no revela:

> *"Q: Can a JACE build its NiagaraNetwork from export tags in other JACEs?
> A: This is not recommended, because of the extra memory consumed by the 'Supervisor' JACE. Also,
> because JACEs are not typically licensed for Niagara 'virtual' components, they could not take
> advantage of 'PxViewTags'."*
> — §*Export tag FAQs* `[CERT-doc]`

`[INFER]` Los **Niagara virtual points** — los componentes que `BSubstitutePxView` crea dinámicamente
en runtime para valores en tiempo real (B267 §267.X) — requieren una feature de licencia que los JACEs
**no tienen por defecto**. Esto significa que un JACE-como-supervisor puede hacer el Join técnicamente,
pero las PxViews no funcionan. Conecta con el gap N5 (bloqueado): el fallo en un JACE real podría ser
de licencia además de (o en lugar de) el fallo de tipo wb-vs-rt documentado en B414.

## 416.10 — AGREGA: acción Validate en PxViewTag y herramienta de diagnóstico pre-Join `[CERT-doc]`

> *"Unlike other export tags, the PxViewTag has a right-click action, Validate. Issuing this command
> runs a 'Px View Tag Validation' job, with a resulting popup Job Log … Validation checks bound ords
> in the PxView for their ability to be 'virtualized' (resolved) on the Supervisor station."*
> — §*PxViewTag action* `[CERT-doc]`

`[INFER]` Esta acción de validación es una herramienta diagnóstica que el corpus no conocía: antes
de ejecutar un Join, el ingeniero puede verificar qué ORDs no se virtualizarán correctamente. Es
especialmente relevante para el caso de `slot:` relativo que genera warnings pero puede funcionar
de todas formas.

## 416.11 — AGREGA: ExportTagProgram — extensión del API post-Join `[CERT-doc]`

> *"The purpose of this Program is to provide a 'template' that automatically executes upon completion
> of an export tag Join, such that 'custom operations' can be automated … the line
> public BString onPostJoin() / Is the dynamic action (method) that gets called after the Join process
> is all finished."*
> — §*ComponentTag example (ExportTagProgram)* `[CERT-doc]`

`[INFER]` `onPostJoin()` es un hook de extensión del API de exportTags que [Bloque 266] no vio en
`BSupervisorJoinJob`. El programa se exporta como `ComponentTag`, ejecuta en el Supervisor al terminar
el Join, y por defecto se elimina (`Remove After Completion = true`). Es el punto de extensión para
automatizar creación de links u otras operaciones post-provisión.

## 416.12 — AGREGA: CategoryFilters no aplican a SystemHistoryImportTag `[CERT-doc]`

Un límite explícito de la funcionalidad que el código nunca expone como tal:

> *"In the initial release of export tags, CategoryFilters do not apply to histories created as a
> result of a SystemHistoryImportTag, only from HistoryImportTags. You must manually apply categories
> to histories created with SystemHistoryImportTags, if necessary."*
> — §*Adding CategoryFilters* `[CERT-doc]`

`[INFER]` El flag `Use Category Mask` que aparece en `SystemHistoryImportTag` (también en la doc, §
*SystemHistoryImportTag properties*) está marcado como **"future use only"** para ese tipo. Un
integrador que confíe en él para control de acceso obtendrá un agujero silencioso.

## 416.13 — Lo que la doc NO resuelve `[INFER]`

La guía oficial es documentación de **producto para integradores** — no especificación técnica. No
aporta nada sobre los failure modes que solo el decompilado revela:

**a) Silencio de `BlacklistTypeResolver` (B414 §414.4)**
La guía no menciona qué ocurre cuando el supervisor carga una `NiagaraStation` con un slot de tipo
`exportTags:SubstitutePxView` y no tiene `exportTags-wb.jar`. El "happy path" documenta que el slot
se crea; el "sad path" (supervisor sin -wb) es invisible para la doc.

**b) Transporte de credenciales en claro con `useFoxs=false`**
La doc menciona `Default Subordinate Use Fox SSL = false` como valor por defecto y documenta la
configuración de Foxs, pero **no advierte** que con `useFoxs=false` las credenciales (`Default
Subordinate User Password`) viajan en texto sin cifrar sobre el canal Fox. El riesgo de N4 — que
[Bloque 266] §266.5 dejó abierto — no aparece en la guía.

**c) Password como property normal en `BJoinProfileManager` (B267 §267.4)**
La guía muestra un "placeholder" para la contraseña en la UI del Join Profile Manager, pero no
documenta si la contraseña queda cifrada en el BOG de la station o en texto. El decompilado de B267
mostró que es una property Baja normal.

**d) Modelo de worker y cola del Join (B266)**
La doc habla del Job Log y del JobService como mecanismo de consulta de resultados, pero no documenta
que el Join corre en un **worker de un solo hilo con cola de 1000** (B266). Para un integrador que
lance joins masivos concurrentes, este límite es invisible.

**e) Failure modes de join parcial**
La doc dice "the Join job log provides troubleshooting data" cuando hay errores, pero no documenta
qué estado queda la `NiagaraStation` en el Supervisor si el Join falla a mitad — por ejemplo, si
el merge del profile.bog tuvo éxito pero la provisión de puntos proxy falló. El `BSupervisorJoinJob`
de B266 revela las fases; la doc solo documenta el resultado final.

**f) Bitmapping de CategoryMask**
La doc menciona que la category mask usa "hexadecimal notation, reflecting a bitmapped weighting by
category indices, e.g. '14' or 'ff'", pero no documenta el algoritmo de peso por índice ni cómo se
mapean nombres de categoría a bits. El código es la única fuente para eso.

`[INFER]` El patrón es idéntico al de B269 para el focus `tags`: la guía documenta **intención y
operación** (el camino feliz); solo el decompilado revela **qué pasa cuando algo sale mal**. Los
hallazgos operativamente más riesgosos — credenciales en claro, tipos no resueltos silenciosos,
modelo de worker — salieron todos del código, ninguno de la doc.

## 416.14 — Conexiones

- **[Bloque 266]** §266.5 — **RESUELTO parcialmente**: las seis properties "Default" del JoinProfile
  son exactamente el `BConnectInfo` del código; `useFoxs=false` por defecto confirma la superficie de
  seguridad (§416.2). La ausencia de advertencia sobre transport-en-claro en la doc es AGREGA-negativo
  (§416.13-b).
- **[Bloque 266]** §266.1 — **MATIZADO**: el merge del BOG descargado no es la station BOG, sino el
  `profile.bog` template del file space del JACE (§416.3). La "inteligencia de merge" aplica a
  componentes, no a properties (§416.5).
- **[Bloque 267]** §267.X — **CONFIRMADO `[CERT-doc]`**: `BSubstitutePxView` es supervisor-side, el
  .px copiado no se modifica, la sustitución de ORDs es runtime en el supervisor (§416.4).
- **[Bloque 414]** §414.3 — **REFORZADO**: veredicto wb-vs-rt confirmado por la doc. El riesgo
  residual (`BlacklistTypeResolver`) sigue sin documentación oficial (§416.13-a).
- **[Bloque 415]** §415.3 — **MATIZADO**: las properties de la NiagaraStation que revertían tras
  rejoin se explican ahora: son sobrescritas desde el profile.bog de la subordinada (§416.3).
  El licenciamiento de "virtual points" (JACEs excluidos) conecta con el riesgo de N4 y el gap N5
  bloqueado (§416.9).
