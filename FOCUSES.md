# Niagara Research — Focus Index

> Multi-focus corpus (METHODOLOGY §16). Un target maduro con varios ejes paralelos de investigación.
> Todos los focuses comparten la numeración global de bloques (`niagara-mental-model-bloqueN.md`) y el
> mismo repo git/hook; se distinguen por su `RESEARCH-STATE-<focus>.md` y su topic key en engram
> (`research/niagara/<focus>/gaps`, `.../progress`).

| Focus | Estado | RESEARCH-STATE | Ámbito | Bloques |
|---|---|---|---|---|
| (base) | stopped | `RESEARCH-STATE.md` | Framework Niagara N4.14 completo (Capas 1-25) + audit Reflow v1.7.5 + OEM Honeywell/Spyder + native platform RE | B1–B130 |
| optimizersupervisor | paused | `RESEARCH-STATE-optimizersupervisor.md` | Install vivo OptimizerSupervisor N4.14.0.162 (config.bog de stations vivas) | B123 |
| platform-native | stopped | `RESEARCH-STATE-platform-native.md` | RE nativo de la plataforma (launchers, JNI, licensing/crypto, driver DLLs, daemon) | B124–B130 |
| protocols | stopped | `RESEARCH-STATE-protocols.md` | Wire-level de protocolos (Modbus/OPC/BACnet/Fox/LON/Sox) + integración LOGO!8 | B131–B137 |
| **modbus** | **stopped (22/22)** | `RESEARCH-STATE-modbus.md` | El **driver** Modbus completo de N4 (6 módulos Tridium + 2 OEM Honeywell, 188 clases medidas): árbol de componentes, config, modelo de puntos, lado servidor/esclavo, motor de adquisición, escritura/presets/file-records, diagnóstico, licencia, workflow de Workbench, capa OEM, migrador. **NO reabre `protocols`**: ese cerró el wire (B131), este el driver. Primeras citas del corpus a `docModbus` (87 topics) y a la guía TR100 Modbus (2082 líneas). Síntesis en B315 | B294–B315 |
| nmodsreflow | stopped | `RESEARCH-STATE-nmodsreflow.md` | Arquitectura backend del módulo OEM NiagaraMods Reflow v1.7.7 `-rt` (service, HTTP/WS, subsistemas) — CERRADO, hilo de seguridad consolidado | B138–B150 |
| nmodsreflow-ux | stopped | `RESEARCH-STATE-nmodsreflow-ux.md` | Capa cliente/browser del módulo NiagaraMods Reflow v1.7.7 `-ux` (módulo fino de registro/loaders + SPA Vue embarcada) — CERRADO, paridad frontend con el backend | B151-B155 |
| live-station | stopped | `RESEARCH-STATE-live-station.md` | Validación DINÁMICA (§12) de la station Niagara N4 VIVA en 127.0.0.1 (WSL mirrored). `live-install` → SECRETS DISCIPLINE. Etapa A (runtime) + Etapa B (14 defectos de B150 con usuario `API`) — CERRADO, 13/14 con veredicto vivo | B156–B162 |
| chihuahua | stopped | `RESEARCH-STATE-chihuahua.md` | Módulo dashboard Niagara N4 de FUENTE PROPIA (`com.angeles.chihuahua`) para BMS Honeywell MX60. Lectura directa. Tri-parte rt/ux/wb, RBAC write-gate, frontend ES5 IIFE. Documentado (C1-C14) + comparado con Reflow (B177) — CERRADO | B163–B177 |
| px-menu | **reabierto (18/31)** | `RESEARCH-STATE-px-menu.md` | PX Menu-Button/Dropdown: emulación de menú desplegable, sintaxis `.px`, gramática PxDecoder/Encoder, converters, workflow del editor. Cerrado 12/12 en 2026-07-06 (estático) y **REABIERTO 2026-07-26 con fase DINÁMICA §12 contra station viva** — evidencia `[CERT-live]` que **corrigió B187 y B189**. Suma el file space real (`^`=`shared/`), acceso HTTP/obix, el puente PX↔web sin módulo (`WebBrowser` + `/file/`) y el toggle de un botón RESUELTO. **2026-07-28 (B293)**: menú HEREDADO — ruta A shell HTML (navbar + iframe de contenido, herencia sobre TODAS las vistas) vs ruta B `PxInclude` (patrón OFICIAL de Tridium, solo `.px` propios); plantillas `.px` = ~250 en módulos, ninguna en la doc. 12 gaps hijo abiertos | B179–B190, **B289–B293** |
| px-editor | stopped | `RESEARCH-STATE-px-editor.md` | El PX Editor en amplitud: la herramienta (`pxEditor-wb`), catálogo completo de widgets/bindings, media/perfiles (Wb/Hx/Mobile), theming (Palladium/CSS), animación=data-binding. Continúa px-menu — CERRADO 6/6 | B191–B196 |
| px-editor-deep | **stopped** | `RESEARCH-STATE-px-editor-deep.md` | Profundizar pxEditor-wb (D: sidebars/studio/make/commands/field-editors) + módulos vecinos (X: webChart, templates, kitPxGraphics/Hvac/N4svg, svgBatik, bajaux). **CERRADO 11/11** (B198-B208 + síntesis B209). Grupo D (D1-D5) + Grupo X (X1-X6). Todo el subsistema PX deep documentado | B198–B209 |
| px-editor-core | **stopped** | `RESEARCH-STATE-px-editor-core.md` | La INFRAESTRUCTURA de pxEditor-wb nombrada-no-abierta — C1 event bus ✅B210, C2 API base ✅B211, C3 factory/WidgetInserter ✅B212, C4 util/property ✅B213, C5 fieldeditors ✅B214 + síntesis B215. **CERRADO 5/5**. 5 hilos: BPxEditor hub, selección=nexo (+§14), @AgentOn=extensión, undo=Command, delgado sobre bajaux | B210–B215 |
| nmodsreflow-builder | **reabierto (14/16)** | `RESEARCH-STATE-nmodsreflow-builder.md` | Reflow como CONSTRUCTOR de dashboards (ángulo PRODUCTO). Base 12/12 + **REABIERTO** grupos A/B/D/E (B228-B241): auto-binding, floorplans, licensing, backups, migración, diff-versiones, módulos, vistas, weather, history/CSV, users/profiles, nav/equipment, alarmas-UI, schedules. Solo falta grupo **C** (dinámico, pendiente OK usuario). **CERRADO 12/12** (B216-B227): stack/libs (B216, §14 d3 presente), modelo dashboard+persistencia **[CERT-live]** (B217), catálogo 20 widgets (B218), assets embebidos+ORD→URL (B219), upload=out-of-band (B220), motor JSON-Patch+control multiusuario (B221), Mapbox "3D"=2D (B222), editor+masonry (B223), render gauge/chart d3/iView (B224), síntesis Parte A (B225), chihuahua-builder+portabilidad (B226), modernización stack (B227) | B216–B227 |
| oem-honeywell-tail | **paused (9/17)** | `RESEARCH-STATE-oem-honeywell-tail.md` | Cola investigable OEM-Honeywell + framework NO cubierta, SEMBRADA del coverage audit `audits/2026-07-12-coverage-audit.md` (§16). 9 gaps cerrados U1-U9 (B242-B250): honIrmConfig-rt, firmware supply-chain, alarm layer, OEM analytics, utilidades honBacnetHelper/honLonsockClient, residuo AX/ASCOT, Forge Connect onboarding, residuo Centraline (= rebrand CentraLine→Honeywell), migradores PlantController/Modbus smart sensor. Abiertos: U10-U15 + U1b/U1c. Fuera de scope: U16 (207 lon* profiles) + U17 (41 lexicons) | B242–B250 |
| niagara-network-supervisor | **planned (0/5)** | `RESEARCH-STATE-niagara-network-supervisor.md` | El eje supervisor↔subordinada. Nace de B266 §266.1: `exportTags` NO es tagging (0/28 clases lo importan), es un join por Fox. 5 gaps: N1 **el riesgo `BSubstitutePxView` wb-vs-rt** (NEXT), N2 `niagaraDriver`, N3 la guía oficial YA preservada, N4 seguridad del canal de join, N5 reproducir el fallo (**requires-execution**). Ya documentado y NO re-investigar: B266 (runtime del join) + B267 (UI + `BPxViewTag`) | (ninguno aún) |
| px-tail | **planned (0/3)** | `RESEARCH-STATE-px-tail.md` | La COLA del subsistema PX: los 3 módulos con **0 entradas en CATALOG** tras cerrar los 5 focuses PX. Medido: `webEditors` **95** (NEXT — citado en 8 bloques, nunca abierto), `kitPxBuilding` **15** (la excepción con componentes Java tipados de B203), `galileoKitPx` **19** (kitPx de otro OEM) | (ninguno aún) |
| tags | **stopped (10/10)** | `RESEARCH-STATE-tags.md` | El subsistema de TAGGING donde B21 solo pasó por arriba (B21 = espinazo para ~159 clases; B82 ya cubrió los 29 OEM). 9 gaps: T1 API pública, T2 motor del diccionario, T3 **RELACIONES** (nunca abierto), T4 condiciones+neqlize (tag→query), T5 haystack completo, T6/T7 exportTags rt+UI, T8 UI/UX, T9 **200 archivos de doc oficial Tridium** (primera vez que el corpus usa `[CERT-doc]` de esta fuente) | B260–B270 |
| **email** | **stopped (10/10 + B334)** | `RESEARCH-STATE-email.md` | El módulo `email` como SUBSISTEMA de servicio: el motor SMTP que ENVÍA (`BEmailService` + `BOutgoingAccount` + sesión JavaMail `MailPlatformHandlerSe`), gate de licencia `tridium/email`, dependencia runtime JavaMail, inbound POP3/IMAP + `BEmailAlarmAcknowledger` (ack por reply-to UUID), OAuth2/XOAUTH2, security dashboard, capas wb/ux. 61 clases (rt 43 · ux 11 · wb 7). **NO reabre alarmas**: `BEmailRecipient` (alarma→correo) ya está en [B34] §34.6.5 (REMITTANCE); este focus es el SERVICIO que el corpus nunca abrió. Audit-first 2026-08-04 | B324– |
| **jsonToolkit** | **stopped (14/14 + B349)** | `RESEARCH-STATE-jsonToolkit.md` | El add-on `com.tridiumx.jsonToolkit` (namespace `tridiumx`, NO core) como MARSHALLER JSON bidireccional: outbound (schema tree → JSON, generado SÍNCRONO en el engine thread, sin transporte propio — output slot consumido por obix/BLink) + inbound (selectores JSONPath → escrituras/ack/export-markers, confía en el sender). Gate de licencia 3 capas (feature `tridium/jsonToolkit` + atributos import/export + SMA). 163 clases propias (Gson 2.9.0 + jayway-jsonpath DESCARTADOS). Relative schema cross-station (Fox `sys:`), inline Program escape hatch, alarm recipient (gemelo del email sin SMTP). Hallazgos seguridad inbound: export-reg SIN ACL, ack-attribution spoofable, arrayForEach sin guard. **Primera cita del corpus a `docJsonToolkit`** (114 files, 33 citados). Síntesis B349. 2 child gaps G1/G2 (requires-execution). Audit-first 2026-08-04 | B335–B349 |
| **electronicSignature** | **stopped (7/7 + B356)** | `RESEARCH-STATE-electronicSignature.md` | El add-on **TridiumPS** `electronicSignature` (+ `electronicSignatureRemote`), namespaces `com.tridiumx.ps.*` + `com.secured.*`, como la capa de firma electrónica **21 CFR Part 11**: punto asegurado por SUSTITUCIÓN DE TIPO (`BSecured*Writable`), verbos `*WithAuthentication` con re-auth (LDAP/local), reason obligatorio, segundo firmante (`BSecondaryRemoteAuthentication` + `BSecureUserMixIn` Level-2), audit `BSecuredTrendRecord`. Gate `tridium:eSignature` + `point.limit`. **Tesis (B356): CEREMONIA de firma FUERTE / ARTEFACTOS de cumplimiento DÉBILES.** FUERTE: pipeline fail-closed (B352), segundo firmante distinto+rol enforced+self-approval bloqueado (B353). DÉBIL (sin firma/sin auditar): reason solo no-vacío no del set (B352), certificación §11.100(c) = propiedad mutable `ESignAcknowledgement` (B355), audit trail plaintext purgeable (B351). UI=formulario, enforcement=el TIPO; credencial Base64 reversible browser `btoa`↔server (B354). **Refuta** que `signingService` (PKI) cumpliera Part 11 (B350). Ofuscación: decompilado string-scrubbed, bytecode/resources intactos. Cerrado 7/7 2026-08-05. Falta ES4-G1 (requires-execution) | B350–B356 |
| px-chart-classic | **stopped (8/8)** | `RESEARCH-STATE-px-chart-classic.md` | El sistema de charting **CLÁSICO** (`javax.baja.chart`, módulo `chart` Swing/Workbench) — el feed que px-editor-core y B201 declararon "otro focus". 67 clases distintas medidas (rt 5 / wb 62; API pública 35+9). 8 gaps: H1 modelo+jerarquía, H2 ejes/render, H3 binding a histories, H4 consumidores + §14 vs B199/B201, H5 impl `com.tridium.chart`, H6 PDF+HX, H7 tests, H8 split rt/wb. Pregunta transversal: por qué N4 arrastra DOS sistemas de charting | B251–B259 |

## Focus activo

**(ninguno activo)** — `electronicSignature` CERRADO 7/7 el 2026-08-05 (B350-B356). Cola disponible sin re-bootstrap
(§16): `niagara-network-supervisor` (planned 0/5, arranca en N1), `px-tail` (planned 0/3, arranca en P1 webEditors),
`oem-honeywell-tail` (paused 9/17, U10-U15+U1b/U1c). Próximo bloque global: **B357**.

**(cerrado)** **electronicSignature** — CERRADO 7/7 el 2026-08-05 (B350-B356 = 6 evidence + síntesis B356). La capa
21 CFR Part 11 de TridiumPS. **Tesis (B356): CEREMONIA de firma FUERTE, ARTEFACTOS de cumplimiento DÉBILES.** El módulo
controla con rigor QUIÉN escribe y CÓMO se autentica — pipeline fail-closed con re-auth (B352/ES2), segundo firmante
distinto con rol Level-2 enforced server-side y self-approval bloqueado por hard-throw (B353/ES3+ES5), enforcement en el
TIPO no en la UI: `BSecured*Writable` no expone `set` plano, solo `*WithAuthentication` (B354/ES6). PERO deja los
ARTEFACTOS que dan fuerza legal y durabilidad a la firma como config ordinaria, sin firma ni auditoría: el reason es solo
no-vacío (no del `BReasonSet`, B352 §11.50a3), la certificación §11.100(c) es la propiedad **mutable** `ESignAcknowledgement`
flag-0 (el lexicon es solo su fallback-si-vacío, B355 §14 corrige B350), y el audit trail `BSecuredTrendRecord` es texto
plano `::` sin tamper-evidence, purgable sin firma vía `BHistoryMaintenance` (B351 §11.10e). Credencial de firma Base64
reversible extremo a extremo (browser `btoa` ↔ server decode, B354). Default de fábrica compliant; la postura en campo
depende de controles EXTERNOS (RBAC de station + backups off-station) que el módulo no provee. Único gap abierto:
**ES4-G1** (requires-execution) — reachability de las dos superficies sin firma para un rol no-super-user. Retro §18
2026-08-05: `retros/2026-08-05-electronicSignature.md`.

**(previo)** — `jsonToolkit` CERRADO 14/14 el 2026-08-04 (B335-B348 + síntesis B349). Marshaller JSON
bidireccional add-on: outbound genera SÍNCRONO en el engine thread (bounded por debounce minWrite + timeout 30s
de queries + engine-cycle queue cap 1000/reject); sin transporte propio (marshaller, no pusher); inbound confía
en el sender (writes autorizados como runAsUser pero export-reg SIN ACL, ack-attribution spoofable); gate de
licencia 3 capas (feature + import/export + SMA). 2 child gaps requires-execution (G1 export-ACL / G2 ack-spoof).

**(previo)** **email** — CERRADO 10/10 el 2026-08-04 (B324-B333 + síntesis B334).
El módulo `email` de N4 como subsistema de servicio: 61 clases (rt 43 · ux 11 · wb 7), audit-first 10 gaps.
El corpus ya tiene `BEmailRecipient` (alarma→correo) en [B34] §34.6.5; este focus cubre lo que falta:
`BEmailService` + gate de licencia `tridium/email` + dependencia JavaMail (E1, NEXT), el pipeline de envío
SMTP con cola/rate-limit (E2), la sesión TLS `MailPlatformHandlerSe` (E3), inbound POP3/IMAP + ack por
reply-to (E4), OAuth2/XOAUTH2 (E5), security dashboard (E6), account base (E7), y las capas wb/ux (E8-E10).
Ver `RESEARCH-STATE-email.md`. Próximo bloque: **B324**.

**(previo)** — `modbus` CERRADO 22/22 el 2026-07-30 (B294-B314 + síntesis B315).

**modbus** (el DRIVER, no el cable) — CERRADO 2026-07-30, 22 bloques B294-B315. Siete hilos (B315):
(1) **el cliente se configura, el servidor se vigila** — 6 vs 3 ProxyExt (un esclavo Niagara no puede exponer
string ni enum-bits), 3 vs 0 propiedades de red, y el servidor no tiene motor de poll porque responde en vez
de preguntar (sirve desde 4 mapas en memoria, O(rango) no O(puntos)); (2) **CUATRO defaults inseguros de
fábrica** — `usePresetMultipleRegister`=false hace que una escritura de 32/64 bits **NO sea atómica** (FC6 en
loop, el lector ve un valor roto: es un ajuste de CORRECTITUD que la guía presenta como rutina),
`criticalData`=false pierde el setpoint del maestro en un corte, el formato de dirección por defecto es **hex**
(tipear 40001 da 262145) y `pointPoll` por defecto = una transacción por punto; más un quinto por consecuencia:
la validación de base addresses **sólo corre en formato modbus**, así que el default la desactiva;
(3) **todo se serializa** — dispatcher de UN hilo por RED (los sockets por device dan aislamiento, no
throughput), cola FIFO con tope 256 y `QueueFullException` sin capturar, fragmentación 125/2000 con el techo
**partido a la mitad en ASCII**, y timing serial con umbrales en **ms fijos** en vez de la regla t3.5 relativa
al baud (~20 ms extra por trama a 115200); la serialización es JUSTA (nadie se adelanta); (4) **firma de código
repetida** — 4 loops copy-paste en Learn, pares Active/Possible clonados, 2 dispatchers gemelos, **2
`System.out.println` shippeados**, FC 23 con clase pero sin `case`, constantes MAX_READ/WRITE sin consumidor, y
el blob de persistencia reconstruido **una vez por byte**; (5) **la doc oficial describe otro producto** — 4
defectos MEDIDOS: jar equivocado en §Modules, el feature `modbus` **no existe** (son cuatro), la página de
límites es de **MS/TP**, y **0 menciones de 64-bit** en 87 topics mientras el driver trae 3 tipos de 64 bits;
(6) **dos §14 internos** — B307 corrige B303 (`byteCount` ES el código de excepción, ninguna ruta estaba mal) y
B308 matiza B295 (los envíos se serializan; corrige mi lectura de throughput); (7) **el discovery lo pone el
OEM** — el driver no tiene ninguno (0 hits, con control positivo contra bacnet-wb) porque el protocolo no lo
permite; Honeywell lo aporta **embebiendo el mapa de registros en código** (registro 3 = CO₂ en un TR50), y el
**TR100 es el contraejemplo**: sin módulo Modbus, sólo una guía de 2082 líneas que el integrador transcribe a
mano. **Lo NO resuelto y declarado**: el condicional de thread-safety de B311 (M22 re-scopeado a un focus
`driver-framework` inexistente — el corpus NO afirma que haya data race) y todo lo que necesita dispositivo
vivo (hereda `P1-dyn` de `protocols`). Retro §18: `retros/2026-07-30-modbus.md`, 6 deltas propuestos al kit.

## Cola de trabajo para la próxima sesión (sembrada 2026-07-24)

Tres focuses listos para tomar **sin re-bootstrapear** (§16: un focus `planned` ya tiene estado y backlog
commiteados). En orden sugerido:

1. **`niagara-network-supervisor`** (planned 0/5) — arranca en **N1**, el riesgo verificado en B267 §267.3:
   `BSubstitutePxView` vive en `exportTags-wb.jar` pero `doJoin()` lo **persiste en el espacio virtual de la
   estación DESTINO**; un JACE que no carga el perfil `-wb` no podría resolver el tipo. **No reproducido** —
   sólo verificado por la ubicación de las clases. N1 es read-only: revisar si existe un `exportTags-rt` que
   declare el tipo, qué exporta el `module.xml`, y dónde vive la superclase `BAbstractSubstitutePxView`. Eso
   decide si el riesgo es real. La guía oficial (`docExportTags`, 86 secciones) ya está preservada y espera
   back-fill de su celda en `SOURCES.md`.
2. **`px-tail`** (planned 0/3) — arranca en **P1**, `webEditors` (95 clases): el módulo más citado del corpus
   que nunca fue sujeto (8 bloques lo mencionan, 7 veces solo en B199).
3. **`oem-honeywell-tail`** — sigue **pausado en 9/17** desde el 2026-07-15 (B242-B250). Gaps abiertos:
   U10-U15 + U1b/U1c. No necesita autorización nueva para reanudarse: pausó por fin de sesión, no por STOP.

**tags** (subsistema de tagging) — CERRADO 2026-07-24, 11 bloques B260-B270. Cinco hilos (B270): (1) **motor
genérico que aloja ontologías como CONTENIDO** — N4.14 soporta TRES (Niagara ~24 clases · Haystack 37, 35 de
ellas tags computados · **Brick 2**); el punto de extensión real es subclasificar `BTagInfo`/`BRelationInfo` y
MONTAR, no el registro de agentes; (2) **casi nada está almacenado** — tags de station y relaciones implícitas
se computan por consulta, y los índices que lo hacen viable son de heap y **mueren en cada reboot**; sin
índice, cada NEQL sobre un tag implícito es un barrido completo de la station; (3) **el fallo silencioso es un
rasgo** — 5 verificadas, la peor: borrar un `BTagGroupInfo` deja componentes que **pierden sus tags sin un solo
error visible** (log a FINE, sin recolección); (4) **controles con dos llaves públicas** — licencia
`Dictionary.limit`=**2 por defecto** y RPC filtradas por `hasOperatorRead()`, pero el candado `frozen` tiene
DOS bypasses (`importContext` public non-final + `Context.decoding`); (5) **dos correcciones de encuadre**:
`exportTags` NO pertenece al subsistema (0/28 clases lo importan; §14 a B21 §21.4) y `neqlize` NO traduce
tag→query sino que hace identificación INVERSA (premisa mía, corregida). Hallazgos operativos: `Dictionary.limit`
default 2 ⇒ montar Haystack completo con licencia base deja un diccionario EN FAULT; `tz` de Haystack es la zona
horaria de la JVM, igual para toda la station. **Primera vez que el corpus usa la doc oficial Tridium como
`[CERT-doc]`** (3 guías, 363 KB preservadas). Nota de método: **4 de 4 claims de permisos de sub-agentes
requirieron corrección** — limitación estructural del barrido delegado, no mala suerte.

**(cerrado)** — `px-chart-classic` CERRADO 8/8 el 2026-07-24 (B251-B258 + síntesis B259).

**COLA DE PX pendiente** (verificado 0 entradas en CATALOG): `webEditors` 95 clases · `galileoKitPx` 19 ·
`kitPxBuilding` 15 = 129 clases.

**px-chart-classic** (charting CLÁSICO `javax.baja.chart`) — CERRADO 2026-07-24, 8/8 gaps, 9 bloques
B251-B259. Seis hilos (B259): (1) **dos motores PARALELOS, no sucesivos** — cero `@Deprecated`, decide el
PERFIL: Workbench=clásico · browser moderno=webChart · **browser Hx=clásico rasterizado sin interacción**;
(2) extensibilidad PARTIDA — declarativa en datos (`@AgentOn` en bindings, `getTypes()` en el property sheet),
cableada en dibujo (renderers solo por setter); (3) diseño ANSIOSO — `Tables.slurp()` carga la tabla entera,
de ahí los topes de `BChartRenderLimitConfiguration` y el traspaso-no-copia del export a PDF; el único camino
incremental es `BoundTimeSeries` (páginas de 256 + filtro por cambio); (4) **5 defectos confirmados** con
causa estructural común: **el módulo distribuido NO trae un solo test automatizado** (B257); (5) el split
rt/wb es un principio — al runtime solo lo SERIALIZABLE (4 tipos + `TrendFlags`), 1 dependencia vs 14;
(6) quien extiende HEREDA — Analytics, la vista de histories y `honeywellSpyderTool` extienden `BChart`, así
que todos los gotchas se propagan. §14 x3: B253 corrige B252 (tesis "pre-agentes" era generalización
indebida), B254 matiza B253 x2, B256 matiza B254 (el tercer caso Hx).

**(pausado)** — `oem-honeywell-tail` en 9/17 (B242-B250, 2026-07-15); abiertos U10-U15 + U1b/U1c.
`nmodsreflow-builder` CERRADO 12/12 (2026-07-12, B216-B227). §18 retro pendiente de correr.

**nmodsreflow-builder** (ángulo PRODUCTO/BUILDER) — CERRADO 2026-07-12, 12 bloques B216-B227. Cómo Reflow crea/edita/
actualiza dashboards y agrega contenido dentro del módulo. Hallazgos: servidor delgado (persiste/parchea JSON opaco),
composición 100% cliente Vue; "editá-y-se-actualiza" = JSON-Patch RFC-6902 (fast-json-patch cliente / flipkart server)
+ control multiusuario cooperativo; catálogo 20 widgets (add=dropdown, no paleta drag); layout=masonry; editor=iframe
live-preview; assets=delegados a servlets nativos Niagara (`/module/`, `/ord/`), upload=out-of-band (Workbench);
"3D"=Mapbox 2D. **§14**: d3 SÍ presente (aliaseado, corrige B216); circle=iView (corrige B218). Validación **[CERT-live]**
contra station N4 viva (B217 §217.8) + dashboard real de disco (B218). Parte B (B226): chihuahua NO tiene builder
(dashboard fijo) pero tiene **3D real Three.js** que Reflow no, y lidera en RBAC/audit; plan de portabilidad 5 piezas.
Modernización (B227): Vue2 EOL→Vue3/Pinia/Vite/TS, mapbox→MapLibre, mantener JSON-Patch+d3.

**(px-editor-core cerrado)** — `px-editor-core` CERRADO 5/5 (2026-07-06, B210-B214 evidencia + B215 síntesis).
Infra interna de pxEditor-wb: C1 event bus (B210), C2 API base root (B211), C3 factory/WidgetInserter (B212),
C4 util/property (B213), C5 fieldeditors converters (B214). Síntesis B215: 5 hilos (BPxEditor hub-and-spoke,
selección=nexo +§14 corrige B211, @AgentOn=mecanismo de extensión uniforme, undo=Command en la infra, capa
delgada sobre bajaux). §14: B213→B211 (SelectedWidgets dispara PxWidgetEvent, no PxSelectionEvent). **Todo el
subsistema PX de Niagara N4 documentado end-to-end**: 4 focuses (px-menu B179-190, px-editor B191-196,
px-editor-deep B198-209, px-editor-core B210-215) + síntesis B197/B209/B215.

**px-editor-deep** (capa herramienta/render deep) — CERRADO 2026-07-06, 11 bloques B198-B208 + síntesis B209.
Grupo D interno (sidebars B198, studio B205, make B201, commands B206, field-editors B202) + Grupo X vecinos
(webChart B199, templates B200, packs B203, svgBatik B208, bajaux B204, easyBinding B207 OEM Honeywell). 4 hilos
transversales (B209): bajaux base web unificadora, 2 sistemas chart, undo=Command, alto nivel sobre kitPx.

**Sesión 2026-07-06 — subsistema PX**: 19 bloques (B179-B197). `px-menu` CERRADO 12/12 (B179-B190, el menú +
formato/gramática). `px-editor` CERRADO 6/6 (B191-B196, el editor en amplitud). `B197` síntesis cross-focus
(7 capas). Coverage-audit honesto: espinazo completo, ~35-40% del universo de clases PX → pendientes en
`px-editor-deep`.

**px-editor** (capa UI/PX, amplitud) — CERRADO 2026-07-06, 6 bloques B191-B196. El PX Editor completo más
allá del menú: la herramienta `pxEditor-wb` (B191: BPxEditor/BStudio/BMakeWidget wizard, load/save/clone por
PxEncoder/Decoder) → catálogo de widgets bajaui (B192: botones/inputs/contenedores/datos-por-modelo) → los 9
bindings kitPx (B193: split BBinding/BValueBinding) → media/perfiles (B194: Wb permisivo, Hx agent-gated,
Mobile whitelist, bajaux sin PxMedia) → theming (B195: Palladium Java vs `.ux-*` CSS) → animación=data-binding
(B196). Junto con px-menu (B179-B190), el subsistema PX queda reconstruido end-to-end.

**(px-menu cerrado)** — `px-menu` CERRADO 2026-07-06 (12/12 gaps, B179-B190).

**px-menu** (capa UI/PX) — CERRADO 2026-07-06, 12 bloques B179-B190. Cómo construir un "Menu Button /
Dropdown" (estilo SLDS) en el PX Editor perfil Workbench. **Framing** (B179: sin widget nativo, `BMenu*`=Swing
WB, 2 patrones) → **workflow oficial del editor** (B180, docGraphics.txt `[CERT-doc]`) → **gramática/sintaxis**
(B181 PxDecoder/Encoder + tag-1-línea, B182 layout panes + §14 BBorderPane≠5-regiones, B183 valores gx) →
**motor del binding** (B184 converters + BIBooleanToSimple type-guard, B186 BValueBinding) → **los 2 patrones**
(B185 PopupBinding, B186 in-place) → **ords/includes** (B187, B188) → **síntesis** (B189 menu.px completo) →
**round-trip** (B190 Parser). Tres capas de evidencia: decompilado `[CERT]`, doc oficial `[CERT-doc]`, `.px`
reales. Deliverable: `scratchpad/menu.px`. Sin fase dinámica pendiente (todo read-only static).

**(base cerrado)** — `chihuahua` CERRADO 2026-07-02 (14/14 subsistemas + comparación con Reflow).

**chihuahua** (fuente propia) — CERRADO 2026-07-02, 15 bloques B163-B177. Módulo dashboard MX60 (Honeywell, dominio agua/bombeo,
6 plantas). Tri-parte `chihuahua-{rt,ux,wb}`, servlet `BChiServlet` en `/mx60/` con dispatch puro + guards
CSRF-lite, **RBAC write-gate (`checkCanWrite`) en cada endpoint mutante** (el contraste agudo con Reflow, que
no gatea). Bootstrap con B163 (esqueleto) + backlog de 14 gaps (barrido de auditoría §13). La comparación
chihuahua↔Reflow y el análisis de brechas son bloques de síntesis POSTERIORES (pedido del usuario).

**live-station** (dinámico §12) — CERRADO 2026-07-02, 7 bloques B156-B162. Primera validación `[CERT-hw]`
end-to-end de la station Niagara N4 VIVA. Etapa A mapeó el runtime (Reflow 1.7.7 en `/nmodsreflow/`, usuario
`API`=HTTPBasicScheme, cert default). Etapa B verificó los 14 defectos de B150: **config-write sin auth
CONFIRMADO** (V1-V3/V12, read-level sobrescribe config, restore byte-idéntico), backups **auth-gated** (V4/V10
NO reproducen — §14 corrige B144), reads 500 con payloads triviales, V7/V8 (BQL) diferido al canal WS
(requires-execution). Cero secretos exfiltrados; station intacta (`bf70f28f`). §14: refina tesis uniforme de
B150 §150.1 (gate NO uniforme: config abierto, backups gated).

**nmodsreflow-ux** (capa cliente `-ux`) — CERRADO 2026-07-02, 5 bloques B151-B155, superficie cliente
completamente mapeada (registro de vistas → loaders/iframe → SPA Vue 2.6.14 → wiring REST/WS → seguridad
cliente). §14: corrigió B50 (Vue 2.7→2.6.14). Confirmó B143/B144/B145 desde el cliente. NEXT-ACTION =
verificación dinámica sobre station viva (requiere hardware/decisión humana).

**nmodsreflow** (backend `-rt`) — CERRADO 2026-07-02, 13 bloques B138-B150, superficie completamente mapeada,
síntesis de seguridad cross-focus en B150. Residual R3 (mount `/module/<name>/`) no perseguido.
