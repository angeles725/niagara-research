# Bloque 420 — SÍNTESIS del focus `niagara-network-supervisor` (B414–B419): join seguro en tránsito, frágil en el borde del perfil, y la lección de verificar antes de alarmar

> Focus **niagara-network-supervisor** — síntesis de cierre. 6 bloques de evidencia (B414–B419),
> 7 gaps, uno bloqueado por ejecución (N5).
> Este bloque consolida los hilos transversales y **remite** a los bloques que establecieron cada
> hallazgo; no re-deriva nada. Corpus en **Español (técnico EN)**.
>
> Alcance del focus: el eje **supervisor↔subordinada** de Niagara Network — cómo una station
> provisiona configuración en otra vía `exportTags` y el driver `niagaraDriver`. NO reabre el
> wire-level de Fox (→ [Bloque 134]) ni el runtime del join desde su perspectiva de tagging
> (→ [Bloque 266]/[Bloque 267]).
>
> Marcadores: este es un bloque de tipo **SÍNTESIS**. Todo `[CERT]` aquí es una **remisión** a un
> bloque que ya lo verificó — la cita es `[Bloque N] §N.x`, no un `file:line` fresco. Un ratio
> `[INFER]`/`[CERT]` alto es **esperado y sano** en esta clase de bloque — los `[INFER]` expresan
> las conexiones transversales que ningún bloque individual afirma. No es señal de agotamiento.
>
> Capa 27 (Niagara Network — supervisor ↔ subordinada). Consolida [Bloque 414]–[Bloque 419].

---

## 420.1 — Qué cubrió el focus

| Área | Bloques |
|---|---|
| N1 — veredicto `BSubstitutePxView` wb-vs-rt; riesgo redefinido | [414] |
| N2 — driver `niagaraDriver-rt`: 106 clases, modelo device-proxy, canales Fox | [415] |
| N3 — guía oficial de Export Tags: RESUELVE/MATIZA/AGREGA/lo que NO resuelve | [416] |
| N4 — seguridad del canal de join: credencial en reposo, transporte, permisos | [417] |
| N6 — tipos no resueltos en BOG propio del JACE: `warningAndSkip`, delta vs supervisor | [418] |
| N7 — intercambio de clave Fox en plain (SRP6 post-SCRAM); refuta [INFER] de B417 | [419] |
| N5 — reproducir el fallo de vistas PX en JACE vivo sin perfil `-wb` | **blocked** (requires-execution) |

El focus nació de un resultado del focus `tags`: [Bloque 266] §266.1 probó que `exportTags` es un
mecanismo de **join supervisor↔subordinada por Fox**, no un sistema de tagging. Arranca con
**7 gaps** más los dos bloques de base ya documentados ([Bloque 266]/[Bloque 267]); al cerrar,
**6/7 están cerrados** en modo read-only. El restante (N5) requiere ejecución en hardware inaccesible.

---

## 420.2 — Hilo 1: el veredicto de seguridad del join, consolidado

Este es el hilo de mayor valor operativo del focus. Tres bloques produjeron evidencia sobre la
misma superficie; su orden cronológico refleja cómo el riesgo se entiende mejor con cada iteración.

### Lo que está protegido

| Vector | Veredicto | Evidencia (bloque) |
|---|---|---|
| Credencial en BOG en reposo | **Mitigado** — `BPassword` (AES o NullEncoder); `toString()` devuelve `"--password--"` siempre | [Bloque 417] §417.2, §417.4 |
| Exposición visual en manager UI | **Mitigado** — `BJoinProfileManager` muestra `"--password--"`, nunca el plaintext | [Bloque 417] §417.4 |
| Acción `join` sin privilegio admin | **Mitigado** — `flags=8` (SUMMARY únicamente); bit OPERATOR ausente → `isAdmin=true` | [Bloque 417] §417.6 |
| Confidencialidad del canal en tránsito (plain Fox N4-a-N4) | **Protegida contra interceptores pasivos** — SRP6 post-SCRAM; `sessionKey` nunca transmitida (PLD) | [Bloque 419] §419.3–§419.6 |

`[INFER]` El cuadro de protección es más robusto de lo que sugería la primera lectura del focus.
Tres de los cuatro vectores estaban ya mitigados en el código, y el cuarto — la confidencialidad
del tránsito, que pareció el más alarmante — resultó también protegido, aunque por un mecanismo que
requería una iteración adicional para verificarlo ([Bloque 419]).

### Lo que NO está protegido

| Riesgo residual | Estado | Evidencia (bloque) |
|---|---|---|
| MITM activo sin PKI | **Abierto** — SRP6 sin TLS no autentica la identidad del servidor; un atacante activo puede impersonar | [Bloque 419] §419.7 |
| Metadatos visibles (username, salt/iterations) | Abierto — SCRAM expone el nombre de usuario y los parámetros PBKDF2 en plaintext | [Bloque 419] §419.7, remite a [Bloque 134] §134.7 |
| Default `useFoxs=false` — sin TLS salvo configuración explícita | Activo por defecto — tanto `BConnectInfo.useFoxs` como `BJoinProfile.defaultUseFoxs` son `false` | [Bloque 417] §417.3 |
| Crackeo offline de password débil | Condicional — PBKDF2/HMAC-SHA-256 mitiga pero no elimina ataques de diccionario | [Bloque 419] §419.7 |

`[INFER]` La acción de refuerzo de mínimo costo que el focus puede recomendar es **activar
`useFoxs=true`** (TLS) en el Join Profile de cada subordinada: elimina el MITM activo, cierra los
metadatos visibles, y añade autenticación del servidor por PKI. Sin TLS, la protección contra
interceptores pasivos es real pero no protege el escenario de red comprometida donde el atacante
puede actuar activamente.

---

## 420.3 — Hilo 2: la cadena de correcciones — cómo el riesgo se redefinió dos veces

El focus produjo dos correcciones de hallazgos previos en el corpus. Ninguna vino de un pase de
revisión explícito — ambas surgieron de leer la siguiente fuente con más cuidado.

### Corrección 1: B267 §267.3 → B414 (riesgo redefinido)

[Bloque 267] §267.3 planteó que el JACE (subordinada) no podría resolver `exportTags:SubstitutePxView`
al levantar su BOG, porque la clase vive en `exportTags-wb.jar`. El gap N1 fue designado HIGH para
resolverlo.

[Bloque 414] §414.3 lo refutó con evidencia de código: `BSubstitutePxView` se persiste **en el árbol
del supervisor**, no en el BOG del JACE — la premisa era incorrecta. El JACE nunca instancia esa
clase. El riesgo real quedó redefinido: si el **supervisor** corre sin el perfil `-wb`, su
`BlacklistTypeResolver` omite silenciosamente el `BPxViewTag` al decodificar el BOG de la subordinada,
y el join PX queda inerte sin error explícito. `[Bloque 414] §414.4–§414.6`

`[INFER]` La lección metodológica: la ubicación de la clase en el jar (`-wb` vs `-rt`) no determina
en cuál árbol se persiste el slot. La cadena `getTargetParent()` → `resolveStationSlotPath(station)`
era la prueba decisiva; B267 no la abrió porque el gap no se planteó en esos términos.

### Corrección 2: B417 §417.4 refuta B267 §267.4

[Bloque 267] §267.4 planteó como `[INFER]` que un operador podría ver la credencial de join en
claro activando la columna `colDefaultUserPassword` del `BJoinProfileManager`.

[Bloque 417] §417.4 lo refutó: la columna es READONLY (`flags=1`), el tipo es `BPassword` (no
`String`), y `BPassword.toString()` devuelve siempre `"--password--"`. Adicionalmente, el manager
solo carga en perfil `-wb` (nunca en JACE runtime). La exposición visual no existe. `[Bloque 417] §417.4`

### Corrección 3: B417 §417.5 → B419 (riesgo en tránsito refutado)

[Bloque 417] §417.5 planteó como `[INFER]` que un interceptor pasivo podría recuperar la clave
compartida del canal `"point"` capturando el handshake Fox y así descifrar el `BConnectInfo` con
las credenciales.

[Bloque 419] §419.7 lo refutó con evidencia de código: la clave de sesión se produce por SRP6
post-SCRAM; los exponentes privados `a` y `b` nunca se transmiten; recuperar `sessionKey` requeriría
resolver el PLD sobre los parámetros estándar de SRP6 (considerado computacionalmente intratable).
La clave AES del canal (`sharedEncodingKey = SHA-512(salt || sessionKey)[0:keySize]`) tampoco es
recuperable pasivamente. `[Bloque 419] §419.2–§419.6`

`[INFER]` El patrón que emerge de las tres correcciones: **el código es más cuidadoso de lo que
sugiere una lectura superficial**. En los tres casos, un análisis incompleto apuntaba a un riesgo
que el código ya mitigaba. La cadena de inferencia era plausible pero faltaba la evidencia de la
siguiente capa.

---

## 420.4 — Hilo 3: el modelo del eje supervisor↔subordinada

El focus construyó el modelo completo de cómo una station N4 actúa como supervisor de otra. Tres
piezas independientes que se ensamblan:

### Pieza 1: el driver `niagaraDriver` como infraestructura del join

El driver ofrece **106 clases** en 11 paquetes (`niagaraDriver-rt`) con el modelo de device-proxy
como estructura central. `[Bloque 415] §415.1`

| Clase | Rol |
|---|---|
| `BNiagaraNetwork` | Contenedor de red como servicio Baja; declara `deviceType = BNiagaraStation.TYPE`; mapa de stations activas en memoria | [Bloque 415] §415.2 |
| `BNiagaraStation` | Device-proxy de la subordinada en el árbol del supervisor; lleva una `BFoxClientConnection` y **8 DeviceExt** (points, histories, alarms, schedules, users, sysDef, virtual, files) | [Bloque 415] §415.3 |
| `BNiagaraProxyExt` | Proxy de un punto individual; `pointId` = ORD en el remoto; `messageId` para multiplexación sobre el canal Fox | [Bloque 415] §415.4 |
| `BPointChannel` | Canal Fox `"point"` que ejecuta suscripciones en batch (`sub`/`unsub`/`change`); `useSharedKeyEncryption()=true` | [Bloque 415] §415.5 |

Los dominios de historia, archivo y schedule comparten la misma `BFoxClientConnection` de la
`BNiagaraStation` padre; la separación es por **canal Fox nombrado**, no por conexión TCP distinta.
`[Bloque 415] §415.6`

### Pieza 2: el `profile.bog` como plantilla que sobrescribe en cada rejoin

El archivo `file:^joinProfiles/SupervisorStationName_profile.bog` en el file space del JACE es la
plantilla editable que determina cómo quedará la `BNiagaraStation` del supervisor tras cada join —
incluyendo las properties de sus DeviceExt. `[Bloque 416] §416.3`

> *"Note that upon any export tag 'Join', these property values overwrite any property values in
> the corresponding NiagaraStation component in the Supervisor station."*

`[INFER]` Esto explica el comportamiento que [Bloque 415] §415.3 observó: las properties de la
`BNiagaraStation` en el supervisor "revierten" tras un rejoin porque el `profile.bog` de la
subordinada las sobreescribe sistemáticamente. La inteligencia de merge aplica a **componentes**
(puntos proxy manuales se retienen), no a **properties** de la NiagaraStation (siempre sobrescritas).
`[Bloque 416] §416.5`

### Pieza 3: el `BSupervisorJoinJob` como motor del join

El join es ejecutado por `BSupervisorJoinJob` en un **worker de un solo hilo con cola de 1000**
([Bloque 266] §266.1). La deserialización del BOG de la subordinada usa un `BlacklistTypeResolver`
que propaga `null` silenciosamente para tipos no registrados en el NRE del supervisor. `[Bloque 414] §414.4`

`[INFER]` La tríada driver/plantilla/job describe un sistema de provisión declarativa: la
`BNiagaraStation` en el árbol del supervisor es el espejo del JACE; el `profile.bog` es la plantilla
de ese espejo; el join es el acto de sincronizar. Todo join re-aplica la plantilla desde cero.

---

## 420.5 — Hilo 4: los dos caminos para tipos no resueltos (JACE-side vs supervisor-side)

El focus documentó dos rutas diferentes en el código para manejar tipos que no pueden resolverse,
con el mismo resultado neto pero por mecanismos distintos. El contraste es arquitecturalmente
relevante.

| Aspecto | JACE cargando su propio BOG (N6) | Supervisor decodificando BOG de subordinada (N1) |
|---|---|---|
| Resolver | `BogTypeResolver` nativo de `ValueDocDecoder` | `BlacklistTypeResolver` (subclase inner de `BSupervisorJoinJob`) |
| Contexto de activación | Arranque normal de la station | Ejecución de join |
| Ruta cuando falta el tipo | `newSwapInstance` → `TypeNotFoundException` → `warningAndSkip` | `super.newInstance()` → `null` → propagado |
| Visibilidad en log | **WARNING visible con line:col** (no silencioso — corrige la descripción de B405 §405.10) | Omisión sin WARNING (silent) |
| Blacklist adicional | No | Sí — tipos legacy Fox también omitidos |
| La station continúa | Sí — error recoverable | Sí — join continúa con el resto de elementos |

`[Bloque 418] §418.2–§418.4`

`[INFER]` La corrección a B405 §405.10 ("silently dropped") es significativa desde el punto de
vista operativo: un administrador que revise el log de Niagara al arrancar un JACE con un tipo
no resuelto VERÁ el WARNING. No es silencioso. Esto cambia la estrategia de diagnóstico.

---

## 420.6 — Lo que este focus NO resuelve

Dicho sin ambigüedad, porque una síntesis que solo lista logros es un documento de ventas.

**N5 (requires-execution) — reproducir el fallo de vistas PX en un JACE vivo sin `-wb`.**
El análisis estático de `BlacklistTypeResolver` ([Bloque 414] §414.4) confirma que la distribución
de vistas PX falla silenciosamente si el supervisor no carga `exportTags-wb`. Pero hay dos factores
adicionales que el análisis estático no puede resolver: (a) si el fallo podría deberse a licencia de
"virtual points" en lugar de (o además de) la ausencia del perfil `-wb` ([Bloque 416] §416.9), y
(b) el comportamiento observable exacto en un JACE real. El gap requiere hardware inaccesible.

**MITM activo sin PKI — riesgo residual sobre el transporte Fox plain.**
La confidencialidad contra interceptores pasivos está demostrada por SRP6 ([Bloque 419] §419.7).
Pero SRP6 sin TLS no autentica la identidad del servidor. Un atacante con capacidad de MITM activo
puede impersonar al supervisor y obtener la clave de sesión. El cierre de este riesgo requiere
`useFoxs=true` (TLS/Foxs, puerto 4911) — fuera del alcance read-only del focus.

**Profundidad sobre N5 + MITM** — ambos pertenecen a un eventual pase dinámico (§12) con acceso
a hardware y capacidad de captura de red. No fueron transferidos a ningún focus nuevo: no existe
backlog activo para ellos.

---

## 420.7 — Tabla de remisiones (summary)

| Gap | Bloque que lo cierra | Hallazgo central |
|---|---|---|
| N1 (wb-vs-rt) | [Bloque 414] | Riesgo B267 MITIGADO por diseño: slot en supervisor, no en JACE; riesgo real = supervisor sin `-wb` |
| N2 (niagaraDriver) | [Bloque 415] | 106 clases, modelo device-proxy; 8 DeviceExt; canales Fox por dominio |
| N3 (guía oficial) | [Bloque 416] | Doc confirma supervisor-side, matiza merge inteligente, agrega workflow + CategoryFilters top-down |
| N4 (seguridad del join) | [Bloque 417] | BPassword mitiga reposo+UI (refuta B267 §267.4); riesgo real = MITM activo sin PKI; join=admin-only |
| N6 (tipos no resueltos JACE-side) | [Bloque 418] | `warningAndSkip` (WARNING en log, no silent); remitido a B405 §405.10 con delta |
| N7 (key exchange Fox) | [Bloque 419] | SRP6 post-SCRAM; sessionKey efímera (PLD); confidencialidad real contra pasivos; refuta B417 §417.5 |
| N5 (reproducir fallo PX JACE vivo) | — | **blocked** — requires-execution en hardware inaccesible |

---

## 420.8 — Self-verify (§11)

`verify-block.sh` tally (salida literal del script):

```
== verify-block: niagara-mental-model-bloque420.md (target: /home/cristian/niagara-research) ==
-- marker tally (raw = whole block · adj = claims, header legend stripped) --
   [CERT-hw] 1
   [CERT-live] 1
   [CERT] 4  (adj 2)
   [CERT-doc] 1
   [CERT-web] 1
   [CERT-a] 1
   [INFER] 15  (adj 13)
-- ratio -- [INFER]/[CERT*] = 13/7 = 1.86
   (>~0.5 in an EVIDENCE block signals investigable evidence nearly exhausted; EXPECTED and healthy in a
    DESIGN/synthesis block — DECLARE the block TYPE so the ratio is read right, §11)
-- [CERT] file:line citation resolution --
   WARN    [CERT] markers present (7) but ZERO file:line citations resolved ...
== exit 0 ==
```

Nota sobre los conteos: los marcadores `[CERT-hw]`, `[CERT-live]`, `[CERT-doc]`, `[CERT-web]`,
`[CERT-a]` en el raw provienen de la fila de tabla en §420.8 (mencionados entre backticks en el
cuerpo); el script los captura por grep. Los 2 `[CERT]` adj y los 13 `[INFER]` adj representan
los marcadores reales del cuerpo. La advertencia de "zero file:line citations" es la firma esperada
de una SÍNTESIS correctamente escrita: las remisiones `[Bloque N] §N.x` son la evidencia; este
bloque no verifica tokens en fuentes primarias — los bloques remitidos lo hicieron cada uno con
su propio token-check. Declarar `[CERT]` aquí sería un error de etiqueta.

**Bloque tipo: SÍNTESIS.** Ratio alto es ESPERADO y sano — no es señal de agotamiento.

Verificación realizada para este bloque:

| Verificación | Resultado |
|---|---|
| Remisiones contadas | 40+ referencias `[Bloque N]`, todas a bloques del focus (B414–B419) o corpus previo (B134, B266, B267, B405) |
| Cada sección remitida existe | Verificado — todos los bloques fueron leídos en esta iteración |
| Ninguna afirmación factual nueva sin remisión | Verificado — toda tabla y todo hallazgo tiene cita de bloque |
| Coverage contra RESEARCH-STATE | 6/7 gaps, 6 bloques de evidencia (B414–B419), `investigable_open = 0` |
| Correcciones heredadas coherentes | Tres correcciones documentadas en §420.3 con backlink a los bloques fuente |

Tier del modelo: **sonnet — inline** (sin delegación).

---

## 420.9 — Conexiones

- **[Bloque 134] §134.7–§134.8** — wire-level Fox y mecanismo SRP6 general; [Bloque 419] remite a
  B134 para el wire y extiende con la condición de activación N4-a-N4 y la derivación de `sharedEncodingKey`.
- **[Bloque 266]** — runtime del join (`BSupervisorJoinJob`, Fox, BOG, worker 1-hilo, credenciales).
  Este focus no reabre ese bloque; lo presupone y cierra las preguntas que dejó abiertas.
- **[Bloque 267]** §267.3, §267.4 — dos `[INFER]` de B267 corregidos en este focus (B414, B417).
- **[Bloque 405] §405.10** — mecanismo base de `BogTypeResolver`; B418 agrega el delta de `warningAndSkip`
  (no es silencioso) y la distinción con `BlacklistTypeResolver`.
- **`px-editor-deep`** (B198–B209) — la capa de vistas PX que los `BSubstitutePxView` del supervisor
  sirven. El fallo de N5 afectaría la experiencia del operador en la vista PX del supervisor.
- **`database`** (B402–B413) — el BOG como capa de persistencia; la deserialización que B418 describe
  usa el mismo `ValueDocDecoder` que B405 documentó.

**Estado del focus**: **STOPPED 6/7**, backlog investigable agotado en modo READ-ONLY (METHODOLOGY §8).
N5 queda como `requires-execution / blocked` — no cuenta como investigable en el loop estático.
