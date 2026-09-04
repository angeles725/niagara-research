# Niagara Research — Focus Index

> Multi-focus corpus (METHODOLOGY §16). Un target maduro con varios ejes paralelos de investigación.
> Todos los focuses comparten la numeración global de bloques (`niagara-mental-model-bloqueN.md`) y el
> mismo repo git/hook; se distinguen por su `RESEARCH-STATE-<focus>.md` y su topic key en engram
> (`research/niagara/<focus>/gaps`, `.../progress`).

| Focus | Estado | RESEARCH-STATE | Ámbito | Bloques |
|---|---|---|---|---|
| **web-hmi** | **active (1/1 seeded, B724; deploy-target hardware reference)** | `RESEARCH-STATE-web-hmi.md` | HARDWARE del panel táctil Honeywell WEB-HMI sobre el que se despliega el dashboard **DashboardPan** (target = **WEB-HMI10/CF**). Destilado SÓLO de 3 datasheets oficiales Honeywell (FUENTE 2): 31-00389 Product Data · 31-00456 User Guide · 31-00390 Install. **FUENTE-1 (corpus) = FUENTE-3 (decompilado) = ZERO** (verificado `corpus-nav find "WEB-HMI"` → No matches; es hardware OEM, ausente de organized/). B724 cubre: (1) lineup+tabla de la familia · (2) specs WEB-HMI10/CF (10.1" 1280×800 WXGA capacitivo, i.MX8M Mini Cortex-A53, Linux 2GB) · (3) modelo de display Linux+Chromium HTML5 = "el archivo compatible es una URL", fit nativo del servlet /dashboardpan/ [INFER] · (4) power/mount/connectivity · (5) implicaciones responsive/kiosk a 1280×800 táctil (targets, no hover, kiosk, viewport, CA cert). REMITTANCE: UI stack B9; controller-hw JACE B657/B672-683. Gaps abiertos: WH1-G1 versión Chromium (datasheet no la da) · WH1-G2 lockdown kiosk · WH1-G3 inconsistencias doc (SD/USB). NEXT WH2 (live-panel probe) → B725. | B724– |
| **station-organization** | **stopped (5/5, B716–B720; deliverable docs/station-organization.md)** | `RESEARCH-STATE-station-organization.md` | STATION ENGINEERING: dónde van los puntos de campo (TC500/IO-R-34) vs la lógica de control, y la estructura recomendada — how-to (`docs/station-organization.md`). Nace de la pregunta del operador. Estructura de 2 capas: proxy points bajo su dispositivo en el driver (points-only) + lógica por EQUIPO aparte, enlazada; navegación por tags+hierarchy. 5 gaps: SO1 driver/points · SO2 equipment/logic · SO3 linking · SO4 nav (hierarchy+tags) · SO5 reuse+síntesis. REMITTANCE: framework-drivers B496-506, NRIO B680/B687, hierarchy B584-590, tags B260-270, template B577, chihuahua B648-655; kitControl (lib de control) DEFERRED (planned). NEXT SO1→B716. | B716– |
| **module-dev-workflow** | **stopped (5/5, B711–B715; deliverable docs/module-dev-workflow.md)** | `RESEARCH-STATE-module-dev-workflow.md` | El PROCESO end-to-end + la mecánica de las tools para programar módulos N4, como RUNBOOK (`docs/module-dev-workflow.md`). Distinto de `module-best-practices` (reglas): esto es el HOW-TO/proceso — qué hace `@NiagaraType`/Slotomatic/gradle-niagara/niagara-signing y el loop edit→build→sign→deploy→test→debug. 5 gaps: WF1 toolchain · WF2 codegen mechanics · WF3 authoring artifacts · WF4 dev loop runbook · WF5 test/debug+deliverable. REMITTANCE: MBP5 B709, own-modules-audit B637-639, module-anatomy B630-636. NEXT WF1→B711. | B711– |
| **module-best-practices** | **stopped (6/6, B705–B710; deliverable docs/module-best-practices.md)** | `RESEARCH-STATE-module-best-practices.md` | Guía ACCIONABLE rt/ux/wb + cross-cutting + build, destilada de module-anatomy/own-modules-audit/chihuahua-source/workbench. CERRADO 6/6: MBP1 rt (8 patrones/7 anti/5 fixes) · MBP2 ux (thin-shim+JS, requiredPermissions=visibilidad, server-RBAC, ES5-strict, Fox-sub+REST) · MBP3 wb (regla cuándo-se-necesita, Manager/View/FieldEditor, wb-invisible-al-daemon) · MBP4 cross-cutting (BPermissions+BCategoryService, audit, engine-thread errors, signing) · MBP5 build (gradle-niagara, Slotomatic mode, signing angelessignerCA, version-targeting, deploy loop) · MBP6 catálogo-exemplar+roadmap. Entregable docs/module-best-practices.md. Top fixes: chihuahua fault-path (safety) + mcpbridge RBAC (security). | B705– |
| **jace-history-audit** | **stopped (5/5 investigable, B699–B703; deliverable tools/hdbread.py)** | `RESEARCH-STATE-jace-history-audit.md` | El TRACE operativo real de la seed station JACE_UMBRELLA: qué grabaron sus stores de history/audit/alarm, leído del SD (`local-sd-image/`, P2). `live-install` → SECRETS DISCIPLINE (schema/counts/tipos; identidades enmascaradas). **Ángulo:** NO el formato .hdb (ya en focus `database` B402-413) sino el CONTENIDO + un lector .hdb propio (§19). VEREDICTO (B703): el trace operativo confirma seed-station (B692) + weak-data-at-rest (B698). HD1 formato .hdb (magic A106F11E + HistoryConfig XML + registros EN CLARO) + lector tools/hdbread.py · HD2 SecurityHistory 58 login/1 fail (single-operator) · HD3 LogHistory=fox+NRIO discovery churn vs IO-34 caído · HD4 alarm.adb=15 alarmas NRIO pingFail/Success routed nowhere · HD5 3 batchJob .hdb vacíos + síntesis. Todo cleartext+rewritable en la tarjeta. | B699–B703 |
| **jace-data-at-rest** | **stopped (6/6 investigable, B693–B698; DAR2-G1 req-exec)** | `RESEARCH-STATE-jace-data-at-rest.md` | Qué obtiene un atacante OFFLINE con la posesión física del microSD del JACE-8000: keyrings (`.kr`/`.km`/`.fskey`), creds OS (`/etc/shadow`+`passwd`), keystores JCEKS, política crypto JRE — leídos READ-ONLY (`local-sd-image/`, P2 QNX6). `live-install` → **SECRETS DISCIPLINE MÁXIMA** (sólo estructura/algoritmos/tamaños; nunca valores; Host ID enmascarado). **Pregunta central:** ¿los campos reversibles/`BPassword` del config.bog (`reversibleEncodingKeySource=keyring`) se descifran con SÓLO el SD, o la clave está atada al hardware (ECC508/Host ID)? Profundiza B466/B684/B689/B685§685.5. VEREDICTO (B698): posesión física del SD = compromiso casi total de datos-en-reposo. DAR1 keyrings (.kr=Java KeyRing serializado, .km=clave AES-256 EN CLARO en la tarjeta, .fskey) · DAR2 §14 refina B466: la clave at-rest vive en keyring SOFTWARE en disco, NO en el ECC508 → SD=todo el material offline (PoC=DAR2-G1 req-exec) · DAR3 7 cuentas OS (5 sin login), shadow=PBKDF2-SHA256~10k = mismo primitivo que config.bog, sin aging · DAR4 keystore.jceks=TLS keypair factory ForRecoveryPurposes self-signed, cacerts/untrusted vacíos · DAR5 JRE non-FIPS estándar, FIPS shipped-not-engaged · DAR6 síntesis: único muro = hashes de login 1-vía (PBKDF2, resistió rockyou 0/3); el root-of-trust hardware NO está en la ruta de datos. Acción: custodia física del SD = el control real; rotar si estuvo fuera de custodia. | B693–B698 |
| **jace-station-config** | **stopped (8/8 investigable, B685–B692; SC4-G1 req-exec)** | `RESEARCH-STATE-jace-station-config.md` | La CONFIGURACIÓN REAL DESPLEGADA del controlador de campo JACE-8000 (station `JACE_UMBRELLA`), leída del `config.bog` del microSD READ-ONLY (`local-sd-image/`, P2 QNX6 via `tools/qnx6read.py`). Data-at-rest OFFLINE, no hardware vivo. `live-install` → SECRETS DISCIPLINE (estructura, no valores). **Ángulo:** NO los internals del framework (ya cubiertos) sino QUÉ corre esta unidad y en qué difiere del SUPERVISOR. `config.bog` = BOG XML EN CLARO (ZIP+file.xml, 51 KB); solo campos `BPassword` cifrados GCM (B393). Skeleton medido en bootstrap: station FIPS=off/keyring, **23 Services**, **2 redes** (NiagaraNetwork `nd` upstream + NrioNetwork `nrio` field IO), tagging pesado (`td:` ×144). VERDICT: JACE_UMBRELLA = template `NewJACEProvisioningStation.ntpl v1.5` + delta mínimo (1 punto relé ro1 + 1 admin) = STATION SEMILLA, no app de campo poblada. SC1 21 Services (template-hardened, TLS1.3-only, sin email/BACnet/apps) · SC2 0 BNiagaraStation (no supervisor, no subordinadas; inbound-supervisor no determinable) · SC3 1 IO-34 (down) con 1 punto relé · SC4 1 admin super-user, sin políticas, AXscheme on, ref categoría-3 colgante (SC4-G1 req-exec) · SC5 3 audit trails locales, 0 egress · SC6 tags 100% stock, 0 aplicados · SC7 0 objetos Program · SC8 síntesis edge-vs-supervisor. Seguridad: fuerte plataforma/débil discreción de despliegue (B684). | B685–B692 |
| **jace8000-sd** | **stopped (5/6 gaps; only SD-G3 blocked-on-hardware)** | `RESEARCH-STATE-jace8000-sd.md` | El microSD de arranque FÍSICO del JACE-8000, leído READ-ONLY (`[CERT-hw]`, §12). Pedido operador 2026-08-30. SD de 4 GB, 3 particiones: FAT32 128 MB boot (la única que ve Windows, `D:`) + 2 QNX (RAW). Partición boot = cadena AM335x `mlo`→`u-boot.img`→`uEnv.txt`(`fatload+go 0x80FFFC00`)→`n4-titan-am335x.signed` (imagen secure-boot TI "CertISW" de 40 MB, firmada Honeywell PKI B394). `fac.properties` = defaults de fábrica Honeywell WEBs golden-image v4.9.1.30 con credencial en texto plano (SECRETS: enmascarada) + `facIP=192.168.1.140` (= JACE vivo del focus jace8000). §14 afina B459: SoC = TI AM335x "Titan" (= ARM Cortex-A8 NPM6xx). Evidencia `sources/probes/B672-jace8000-sd/`. **B673 (SD-G1):** imagen raw completa (4GB, errs=0) → las 2 particiones "Unknown" son **QNX6 Power-Safe** (magic 0x68191122, bloque 1KB), leídas raw (este kernel WSL no tiene driver qnx6 ni hay sudo). **P2 (3.3GB)** = filesystem Niagara VIVO: NRE 4.14.0.162, station **JACE_UMBRELLA** en `/home/niagara/stations/` (= la de B473), módulos `/opt/niagara/modules/`, config.bog, ~249MB/699 inodos. **P3 (256MB)** = slot de recovery/mantenimiento QNX6: QNX v1.2b boot loader + `n4-titan-am335x-maint.signed` + firmas JAR NIAGARA1/4.RSA + factory props, 11 inodos. **B674 (SD-G1b):** árbol QNX6 COMPLETO leído con lector propio `tools/qnx6read.py` (sin driver qnx6 ni sudo — superblock→inode file indirección 2 niveles→dir walk short+long names, OFF=12). **P2 = filesystem raíz QNX Niagara** (98 dirs/599 files): `/opt/niagara` (bin 10 .so, jre completo, defaults, **modules/ 173 JARs**, security/certificates+licenses bajo Host ID Qnx-TITAN-44A2-****-****-363E) + `/home/niagara` (registry.db, security keyring `.kr`/keystores, **stations/JACE_UMBRELLA/config.bog**+alarm.adb+history .hdb). Keyrings en disco: `/.fskey/.key`, `/etc/km/.km`, `/home/niagara/security/.kr`. **P3 = recovery** (8 files): boot chain + `maint/n4-titan-am335x-maint.signed` + **`n4clean.tar.gz` 43MB (imagen factory)**. SECURITY: posesión física del SD = exposición total de datos en reposo (shadow, keyrings, config.bog, audit) + creds de fábrica → rotar. **B675 (SD-G2):** `n4clean.tar.gz` (43MB) = imagen factory en capas de tarballs firmados (defaults + cert Tridium-only + policy → `jre.tar.gz` + `nre-core-update.tar.gz` firmado NIAGARA4.RSA → `nrecore.tar.gz`); OEM Honeywell certs+licencias se agregan en commissioning (§14 vive B392). `n4-titan-am335x.signed` = wrapper TI **CertISW** (cert ~0x350B + payload ~27MB + firma). **B676 (SD-G2b):** `nrecore` = core BC-FIPS/Jetty (17 jars + 12 .so + niagarad/nre/station); `NIAGARA4.RSA` = cadena PKCS#7 **Tridium→DigiCert SHA2 Assured ID Code Signing→DigiCert Assured ID Root**, RSA-2048/SHA-256 (cert genuino Tridium/DigiCert, NO el Honeywell PKI que re-firma los módulos desplegados B392 — el re-firmado OEM es en commissioning); CertISW = formato TI GP (sin X.509 en el header). Imagen cruda 4GB + payloads preservados en `local-sd-image/` (gitignored). Solo queda SD-G3 (blocked, serie viva). Sibling físico del focus `jace8000`. | B672–B676 |
| **jace8000-qnx-native** | **stopped (8/8 investigable; QN6-G1 blocked)** | `RESEARCH-STATE-jace8000-qnx-native.md` | El gemelo QNX/ARM del focus `platform-native` (que hizo los binarios WINDOWS). RE estático READ-ONLY de los 13 binarios ARM del microSD del JACE-8000 (`local-sd-image/bin-arm/`, gitignored; **con símbolos** — no stripped). Ghidra 12.1.3 + r2. Backlog: QN1 libdsfspi cripto ✅B677 (Mocana NanoCrypto static, AES-256-CBC, NIST CTR-DRBG — twin de B425), QN2 launcher ✅B678 (JavaLauncherQnx→dlopen libjvm.so; libnre=live NativePlatformProvider; **ATECC508 HSM engine + 802.1X**), QN3 niagarad ✅B679 (launcher fino de NiagaraDaemon; dropea privilegios al user niagarad, rechaza root — §14 vs plat.exe LocalSystem B381), QN4 field-bus ✅B680 (BACnet MS/TP, NRIO, CCN /dev/ccn, serie — todos *PlatformServiceQnx JNI), QN5 ✅B681 (libcommon=watchdog+netcfg+**OpenSSL 2º stack cripto**; libbacnet=BACnet/Ethernet), QN6 ✅B682 (cadena de invocación QNX ROM→MLO→u-boot→go; QNX 7.0.0; **payload cifrado** → IFS no extraíble offline, QN6-G1 blocked), QN7 ✅B683 (PowerdQnx UPS/batería; station launcher de-privileged uid300), QN8 ✅B684 veredicto: fuerte en boot/firmware/proceso (secure boot+payload cifrado+ECC508+FIPS+daemons sin root), DÉBIL en datos en reposo (partición QNX6 en claro → SD=compromiso total). REMITTANCE: Windows=platform-native, vivo=jace8000, SD=jace8000-sd. | B677– |
| **alarm-webhook** | **stopped (6/6, investigable=0; retro pending)** | `RESEARCH-STATE-alarm-webhook.md` | Los seams Java que toca un alarm-recipient webhook custom (`BMiWebhookRecipient extends BRecoverableRecipient`: serializa `BAlarmRecord`→JSON, POST a backend Node→Telegram). Pedido de la sesión `Telegram` (teammate) 2026-08-30. DEEPENING code-level de [B34] §34.6. AW1 (B666) anatomía de `BRecoverableRecipient` — sin constructor (lifecycle `started/stopped`), 1 `RetryThread` lazy, cola persistente = 1 `<uuid>.xml` (ValueDocEncoder) en `file:^^alarm/<name>AlarmQueue/` (**§14 corrige B34 §34.6.4**: NO `alarm/recipients/{name}/`); único override = `sendAlarm`. AW2 (B667) threading: `sendAlarm` corre INLINE en el thread único compartido `"Alarm:ServiceWorker"`, alimentado por `CoalesceQueue` UNBOUNDED (Integer.MAX_VALUE) → un POST bloqueante frena TODO el routing y arriesga OOM (confirma/refina §34.1.3 + G1); `BRecoverableRecipient` desacopla REINTENTOS, no el primer envío. AW3 (B668) module.xml: `<dependency name="alarm-rt">` + `<type>` build-generado de `@NiagaraType`; `routeAlarm` NO se declara (heredada); permisos socket/URL + `alarm` FilePermission. AW4 (B669) token = `@NiagaraProperty type="BPassword"` leído con `AccessController.doPrivileged(pwd::getValue)` (igual que `BBasicEmailClientAuthenticator` SMTP), reversible, cifrado por keyring `.km/.kr` DPAPI. **REABIERTO** 2º pedido Telegram: AW5 (B670) getters exactos de `BAlarmRecord` para el toJson (7/8 ok; **corrige `isAckRequired`→`getAckRequired`**; `getAlarmFacet`→BObject/null, `getFormattedAlarmDataValue`→String/"", keys vía `getAlarmData().list()`); AW6 (B671) ACK externo vía oBIX: `POST /obix/alarm/<uuid>/ack` con `<obj><str name="ackUser"/></obj>` (contrato solo `ackUser`, sin `ackData`), uuid = misma clave del webhook, BASIC auth, **el invoke del ack NO tiene gate de permiso** (solo force-clear exige admin-write). 1 gap hijo AW3-G1 (requires-execution). | B666–B671 |
| **ports** | **stopped (7/7 + synthesis B627)** | `RESEARCH-STATE-ports.md` | Referencia POR-PUERTO de una instalación N4 (station+plataforma): para cada puerto que ESCUCHA — qué es · dónde se configura (qué `BService`) · gate de auth/permiso · qué alcanza (blast-radius). Consolidación+gap-fill; internals de protocolo = REMITTANCE (B398 scan vivo, B134 Fox, B129/B460 daemon, B133/B280 BACnet, B29/B508 web, B476/B498/B503 drivers). Gaps nuevos: exposición/auth por-puerto no consolidada + puertos nunca caracterizados (Modbus :502, SNMP :161 public, BACnet/SC /hub en :443, OPC-UA :52443, Fox multicast, firewall/BServerPort). AUDIT-FIRST 2026-08-29. 7/7 investigable cerrados (B620-B626) + tabla maestra B627; PO-G7w (wire digest) requires-execution. Sorpresas: BACnet/SC /hub en :443 (no puerto aparte, Niagara-auth); OPC-UA :52443 UNWIRED (refutado); Modbus/SNMP-public/BACnet/KNX = unauth-by-design bounded by export map. | B620–B627 |
| **graphql-admin** | **stopped (8/8 + synthesis B619)** | `RESEARCH-STATE-graphql-admin.md` | Factibilidad + arquitectura de referencia de una capa **GraphQL DIY para ADMINISTRAR** una station N4 desde un **módulo dashboard** propio, anclada en la superficie servlet/API/RBAC real. Hecho establecido: N4 **no** trae GraphQL nativo (solo la constante MIME `application/graphql` en `httpClient-wb`). EVIDENCE-grounded DESIGN: cada bloque lee el seam Java (WebOp/Context, `@NiagaraRpc`, `OrdTarget.canRead`, classloader, BOX channel, dashboard-ux) y mapea cómo GraphQL encaja. READ-ONLY sobre disco, sin probes en vivo. AUDIT-FIRST 2026-08-29; transporte primitivo = REMITTANCE al focus `apis` (B507–B516). 8/8 investigable cerrados (B611-B618) + síntesis B619. Verdict: DIY buildable, módulo separado; GraphQL agrega ergonomía no capacidad. | B611–B619 |
| **module-anatomy** | **stopped (8/8, MA1–MA8; §14→B12; retro pending)** | `RESEARCH-STATE-module-anatomy.md` | El ESQUELETO de un módulo N4 — estructura/organización + cómo se CONSTRUYE y DISTRIBUYE — reconstruido desde CÓDIGO (clases reader/registry/classloader/install), el DEEPENING code-side de [B12]/[B25] (breadth doc-side). Modelo de referencia desde módulos reales Tridium/Honeywell; el módulo propio del operador `com.angeles.chihuahua` [B163–B177] = CASO DE ESTUDIO en la síntesis (desviaciones = mejoras concretas). READ-ONLY disco. AUDIT-FIRST 2026-08-29. 3 HIGH (MA1 manifest reader ModuleManifest/BModulePart/NModule · MA2 boot scan BootEnv/ModuleManager/ClassScanner · MA3 type-registration pipeline @NiagaraType→Registry) · 2 MED (MA4 jar layout · MA5 daemon install command) · 2 LOW (MA6 palette reader · MA7 permissions→NiagaraPolicy) · MA8 síntesis+chihuahua. REMIT: schema B12/B76, profiles B12, build B12/B176, signing ENTERO, classloader B617. NEXT B629. | B629– |
| **own-modules-audit** | **stopped (investigable 8/8 + synthesis, B637-B647; MCP-G2 req-exec; retro pending)** | `RESEARCH-STATE-own-modules-audit.md` | Auditoría de los módulos PROPIOS del operador (vendor ANGELES/SEJOFA) contra el esqueleto de referencia [B636]. Nace del pedido "puedes ver los demas modulos". REAL SOURCE (operator-pointed): `/home/cristian/modulos_niagara_n4/` (chihuahua build tree + niagara-tools). OMB1-3 = proceso de build real (variantes Clean+Slotomatic+Build vs Clean+Build · version-targeting por PATH de SDK niagara_home 4.13/4.14/4.15 · firma ANGELES · tests). OMA1-8 = audit por-módulo de los jars reales (systemic over-permission, sdash 2186 clases, mcpbridge MCP, datacenter-ux 220, httpClientGAngeles=exemplar, ANGELES-namespace, small SEJOFA). REMIT: nmodsreflow B138-155, chihuahua B636. Correcciones operador: version-targeting DELIBERADO (reframe B636 dev#2), firma activa=ANGELES. NEXT B637. | B637– |
| **chihuahua-source** | **stopped (8/8, CS1-CS8, B648-B655; §14→B636/B640; retro pending)** | `RESEARCH-STATE-chihuahua-source.md` | Auditoría SOURCE-LEVEL del módulo `chihuahua` — el ÚNICO en producción [B643] — contra el template de referencia [B647]. Lee la fuente real `/home/cristian/modulos_niagara_n4/.../chihuahua/` (rt 17 java, ux 25, wb 13 + front-end Three.js/Chart.js + audit interno 2026-05-06). Pregunta clave: ¿el write-path del servlet ENFORCE RBAC (ChiRbacHelper+ChiAuditHelper) — bien, a diferencia del bypass de mcpbridge [B643]? CS3 servlet-auth · CS2 rt control/protección · CS6 reconciliar audit-2026-05-06 · CS1 build vs template · CS4 helpers datos · CS5 front-end ES5 · CS7 wb BatchLinkEditor · CS8 síntesis production-readiness. NEXT B648. | B648– |
| (base) | stopped | `RESEARCH-STATE.md` | Framework Niagara N4.14 completo (Capas 1-25) + audit Reflow v1.7.5 + OEM Honeywell/Spyder + native platform RE | B1–B130 |
| **video** | **document 4/4** | `RESEARCH-STATE-video.md` | DOCUMENT-mode (§20): 4 how-to de integración de cámara AXIS (M2025-LE homelab propio) en N4 — V1 driver nativo `naxisVideo` licenciado, V2 embed relay MJPEG en Web Widget, V3 PxImage snapshot, V5 módulo propio (gate=firma, no feature). Cross-finding: N4 "Digest"=SCRAM≠RFC7616. Secuencias `[INFER]` (4 gaps de validación en vivo). Deliverable `docs/video-axis-n4-integration.md` | B453–B456 |
| **jace8000** | **stopped (16/23 gaps, investigable=0; B459-B474)** | `RESEARCH-STATE-jace8000.md` | The JACE-8000 as a **live embedded QNX controller** (`192.168.1.140`, `live-install` §12). Angle: architecture (QNX Neutrino / ARM Cortex-A8 NPM6xx / HotSpot JVM — **not Linux/Windows**), accessing the station (SCRAM live), entering the QNX filesystem, the platform daemon (:3011/:5011, 403-to-GET), **platform entry without Workbench + RE the platform protocol to pull the station `.bog`**, and station recovery when platform access is lost (USB clone backup / Factory Recovery / serial console). Reuses B457 SCRAM tool. B459 = architecture bootstrap. **Operator action: rotate exposed `admin` creds** | B459– |
| **jace9000** | **CLOSED (13/13; B657-B665; §12 live)** | `RESEARCH-STATE-jace9000.md` | The JACE-9000 as a **live embedded controller reached over its serial DEBUG console** (COM5 @ 115200 8N1 = USB-C DEBUG = the **"ATLAS System Shell"**, menu-driven platform admin). Sibling of `jace8000` but a **different machine**: **NXP i.MX8M Plus quad-core / Linux / ATLAS platform** (`BSystemPlatformServiceAtlas extends …Npsdk`, not QNX), Host ID `ATLAS-SD-…`, **TLS-only :5011 by requirement**, FIPS 140-2. B657 bootstrap = console identity + read-only menu safety map + `[CERT-live]` (idle-timeout re-auth, System Diagnostic submenu = 8 read-only opts; live main-menu numbering ≠ doc example). READ-ONLY §12; live gaps need operator serial paste. **Operator action: rotate exposed `admin1` creds** | B657– |
| **api-access** | **document 2/2** | `RESEARCH-STATE-api-access.md` | DOCUMENT-mode (§20): acceso programático legítimo a una estación N4 viva (estación propia del operador, cuenta `API2`). B457 = login (SCRAM-SHA-256 + acceptEula); B458 = extracción oBIX (op query History POST/GET, paginación, delta incremental, config dump). `[CERT]` código + `[CERT-live]` cross-session. Tools en `sources/probes/B457-n4-login/`. Acción: rotar credenciales expuestas | B457–B458 |
| **database** | **stopped (11/11 + synthesis B413)** | `RESEARCH-STATE-database.md` | La capa de PERSISTENCIA de N4 como subsistema — no el formato de cada archivo (ya en B5/B33/B34/B114/B393) sino la mecánica viva que el corpus nunca abrió: ciclo de guardado del BOG (trigger/dirty flag), modelo BComponentSpace/BSpace, ejecución BQL contra el space, migración de BOG entre versiones, y sobre todo el EXPORT a RDBMS externo (rdb-rt, alarmOrion, HSQLDB embebido) — el puente base-interna↔SQL-externa. 11 gaps DB1-DB11 cerrados (B402-B412) + síntesis B413. STOPPED 2026-08-09 | B402–B413 |
| optimizersupervisor | paused | `RESEARCH-STATE-optimizersupervisor.md` | Install vivo OptimizerSupervisor N4.14.0.162 (config.bog de stations vivas) | B123 |
| platform-native | **stopped (15/15, investigable=0)** | `RESEARCH-STATE-platform-native.md` | RE nativo de la plataforma (launchers, JNI, licensing/crypto, driver DLLs, daemon). Base estática B124-B130 (grado strings/RTTI). **Sub-pass Ghidra-grade 2026-08-07 (B379-B383)**: decompilación de CUERPOS de función — nverify (4 flags skip-* + pin TPK RSA-2048 por memcmp), njre launcher (provider FIPS-gated + gate anti-agente), plat.exe (daemon LocalSystem/auto-start + passphrase argv + policy + DPAPI REG_BINARY/HKLM), libciper.so (Sylk masterslave file-transfer + dual CRC, no crypto), síntesis B383. NG1-G1 (B384) + NG2b (B385) CERRADOS. **Reabierto 2026-08-10 (B424-B425)**: dos dumps Ghidra de 1-ago SIN capturar → `investigable=0` era FALSO. NG5 (B424): `getHostId` = fold XOR no-cripto de 8 bytes sobre 4 fuentes (hidden key + RegisteredOwner + product id + volume serial C:), vendor "tridium" hardcoded, corrige/sube B124 INFER→CERT. NG6 (B425): SPI crypto DSF (`dsfspi.dll`) = fachada JCE fina que delega TODO a Mocana NanoCrypto estático (AES-256-CBC, CTR-DRBG sembrado solo por timers). **RE-STOPPED investigable=0** | B124–B130, **B379–B385**, **B424–B425** |
| **license-diff** | **stopped (6/6, investigable=0)** | `RESEARCH-STATE-license-diff.md` | Diff LICENCIADA vs SIN-LICENCIA en disco (B386-B391), corrected by live 4.10 addendum B442 and authorization differential B443. Portable verdict: the first observed divergence is license discovery (valid records present vs absent), which produces `tridium:nre` present vs absent and the expected station/platform decision; parent `security/` presence is version-dependent. Modules/bin/config remain vendor/version/user axes. | B386–B391, B442–B443 |
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
| oem-honeywell-tail | **stopped (in-mission DONE: 13/15; B242–B250, B493–B495)** | `RESEARCH-STATE-oem-honeywell-tail.md` | Cola investigable OEM-Honeywell + framework NO cubierta, SEMBRADA del coverage audit `audits/2026-07-12-coverage-audit.md` (§16). In-mission CERRADO 2026-08-24: U1–U9 (B242–B250) + U1b/U1c (B493) + U14 (B494) + U10 (B495). Restan 2 shared requires-execution/blocked (G8/G5b). **U11-U15 reclasificados LOW/out-of-mission**; U12 (framework drivers) escindido al focus `framework-drivers`. Fuera de scope: U16 (207 lon* profiles) + U17 (41 lexicons) | B242–B250, B493–B495 |
| **framework-drivers** | **stopped (10/10, B496–B506)** | `RESEARCH-STATE-framework-drivers.md` | Escisión de `oem-honeywell-tail` U12: los módulos DRIVER de framework Tridium (no-OEM). AUDIT-FIRST 2026-08-25 (§13). REMITTANCE: `modbus*`=focus modbus B294–B315, SNMP=B476. 10 drivers cerrados: opcUaCore(B496)/Client(B497)/Server(B498), obixDriver(B499), mbus(B500), openAdr(B501), opc-DA(B502), knxnetIp(B503), abstractMqttDriver(B504), weather(B505) + síntesis B506. 5 ejes: bundling-SDK/driver-vs-Service/escalera-seguridad/license-zoo/3-namespaces | B496–B506 |
| **kitControl** | **planned (0/12, KC1–KC12)** | `RESEARCH-STATE-kitControl.md` | El eje CONTROL-PROGRAMMING de N4: librería nativa de function blocks (`kitControl`), modelo de writable-point (`javax.baja.control`), módulo `program` (lógica freeform), las REGLAS de cableado (links/ejecución/priority-array) y las librerías de control HVAC (`honeywellFunctionBlocks`, `honIrmControl`, `clHVAC*`). Nace del pedido del operador (docs kitControl + cómo se programan los módulos + reglas + control HVAC). AUDIT-FIRST 2026-08-28. Engine half REMITTANCE (B6/B429); ataca CATALOG+RULES+HVAC-app. NEXT KC1→B536 | B536– |
| **apis** | **planned (0/8, API1–API8)** | `RESEARCH-STATE-apis.md` | "Todas las APIs de N4" → AUDIT-FIRST 2026-08-25 (2 agentes) mapeó ~40 superficies; casi todas ya tienen bloque dedicado (REMITTANCE: Fox B134, servlets/CSRF B58, hx B433, WebSocket B59, SCRAM B457, BajaScript/BOX B36/B42, Baja SDK B4, ORD B5, api-access B457-458…). 8 gaps con superficie SIN cubrir: NiagaraRpc/RpcServlet, ORD-over-HTTP routing, oBIX server-side (obix-rt), BOX wire, BAuthenticationScheme SPI, Fox client API, BJob/JobService, BQL-call/over-HTTP. nHaystack/BACnet-WS proven-absent | — |
| **niagara-network-supervisor** | **stopped (6/7, investigable=0 + síntesis B420)** | `RESEARCH-STATE-niagara-network-supervisor.md` | El eje supervisor↔subordinada. Nace de B266 §266.1: `exportTags` NO es tagging (0/28 clases lo importan), es un join por Fox. 6/7 gaps cerrados (N1–N4, N6, N7); N5 blocked (requires-execution). Veredicto de seguridad: BPassword mitiga reposo+UI; SRP6 protege tránsito contra pasivos; riesgo residual = MITM activo sin PKI. Modelo: device-proxy BNiagaraStation + 8 DeviceExt + canales Fox nombrados + profile.bog plantilla. Síntesis B420. | B414–B420 |
| px-tail | **stopped (3/3, investigable=0)** | `RESEARCH-STATE-px-tail.md` | La COLA del subsistema PX: los 3 módulos con 0 entradas en CATALOG, ahora CERRADOS. `webEditors` (95, B421): FE web = `BSingleton`+`@AgentOn` que delega toda la UI a JS (contraste total con el Swing de Workbench). `kitPxBuilding` (15, B422): la excepción con código = agregación multi-input con prioridad (`BEquipment` procesa 2-6 booleanos en `changed()`), no rendering. `galileoKitPx` (19, B423): NO es OEM independiente = Honeywell Galileo (mismo vendor que `easyBinding`), obfuscado + license-gated, implementa RBAC por PIN sobre widgets PX. | B421–B423 |
| **workbench** | **stopped (12/12 + síntesis B439)** | `RESEARCH-STATE-workbench.md` | La INFRAESTRUCTURA del Workbench Swing como herramienta de ingeniería: core UI (`bajaui`/`gx`/`workbench-wb`), framework de managers/views/tablas, wire sheet, property sheet, nav tree, field editors no-PX. Nace de confirmar que de **202 módulos `-wb`**, solo 2 subsistemas están a fondo (edición PX + charting) y 120 no se mencionan nunca. EXCLUYE por REMITTANCE: PX (`px-*`, B179-B215/B289-B293/B421-B423) y charting (`chart*`/`webChart`, B251-B259/B368-B377). Bootstrapeado 2026-08-10 con audit-first coverage matrix (§13). Arc del framework Swing (WB01-WB05+WB07): widget model (B427) → shell/nav (B428) → wire sheet (B429) → property sheet+field editors (B430) → managers (B431) → commands/undo/transfer (B432). | B427–B432 |
| tags | **stopped (10/10)** | `RESEARCH-STATE-tags.md` | El subsistema de TAGGING donde B21 solo pasó por arriba (B21 = espinazo para ~159 clases; B82 ya cubrió los 29 OEM). 9 gaps: T1 API pública, T2 motor del diccionario, T3 **RELACIONES** (nunca abierto), T4 condiciones+neqlize (tag→query), T5 haystack completo, T6/T7 exportTags rt+UI, T8 UI/UX, T9 **200 archivos de doc oficial Tridium** (primera vez que el corpus usa `[CERT-doc]` de esta fuente) | B260–B270 |
| **email** | **stopped (10/10 + B334)** | `RESEARCH-STATE-email.md` | El módulo `email` como SUBSISTEMA de servicio: el motor SMTP que ENVÍA (`BEmailService` + `BOutgoingAccount` + sesión JavaMail `MailPlatformHandlerSe`), gate de licencia `tridium/email`, dependencia runtime JavaMail, inbound POP3/IMAP + `BEmailAlarmAcknowledger` (ack por reply-to UUID), OAuth2/XOAUTH2, security dashboard, capas wb/ux. 61 clases (rt 43 · ux 11 · wb 7). **NO reabre alarmas**: `BEmailRecipient` (alarma→correo) ya está en [B34] §34.6.5 (REMITTANCE); este focus es el SERVICIO que el corpus nunca abrió. Audit-first 2026-08-04 | B324– |
| **jsonToolkit** | **stopped (14/14 + B349)** | `RESEARCH-STATE-jsonToolkit.md` | El add-on `com.tridiumx.jsonToolkit` (namespace `tridiumx`, NO core) como MARSHALLER JSON bidireccional: outbound (schema tree → JSON, generado SÍNCRONO en el engine thread, sin transporte propio — output slot consumido por obix/BLink) + inbound (selectores JSONPath → escrituras/ack/export-markers, confía en el sender). Gate de licencia 3 capas (feature `tridium/jsonToolkit` + atributos import/export + SMA). 163 clases propias (Gson 2.9.0 + jayway-jsonpath DESCARTADOS). Relative schema cross-station (Fox `sys:`), inline Program escape hatch, alarm recipient (gemelo del email sin SMTP). Hallazgos seguridad inbound: export-reg SIN ACL, ack-attribution spoofable, arrayForEach sin guard. **Primera cita del corpus a `docJsonToolkit`** (114 files, 33 citados). Síntesis B349. 2 child gaps G1/G2 (requires-execution). Audit-first 2026-08-04 | B335–B349 |
| **electronicSignature** | **stopped (7/7 + B356)** | `RESEARCH-STATE-electronicSignature.md` | El add-on **TridiumPS** `electronicSignature` (+ `electronicSignatureRemote`), namespaces `com.tridiumx.ps.*` + `com.secured.*`, como la capa de firma electrónica **21 CFR Part 11**: punto asegurado por SUSTITUCIÓN DE TIPO (`BSecured*Writable`), verbos `*WithAuthentication` con re-auth (LDAP/local), reason obligatorio, segundo firmante (`BSecondaryRemoteAuthentication` + `BSecureUserMixIn` Level-2), audit `BSecuredTrendRecord`. Gate `tridium:eSignature` + `point.limit`. **Tesis (B356): CEREMONIA de firma FUERTE / ARTEFACTOS de cumplimiento DÉBILES.** FUERTE: pipeline fail-closed (B352), segundo firmante distinto+rol enforced+self-approval bloqueado (B353). DÉBIL (sin firma/sin auditar): reason solo no-vacío no del set (B352), certificación §11.100(c) = propiedad mutable `ESignAcknowledgement` (B355), audit trail plaintext purgeable (B351). UI=formulario, enforcement=el TIPO; credencial Base64 reversible browser `btoa`↔server (B354). **Refuta** que `signingService` (PKI) cumpliera Part 11 (B350). Ofuscación: decompilado string-scrubbed, bytecode/resources intactos. Cerrado 7/7 2026-08-05. Falta ES4-G1 (requires-execution) | B350–B356 |
| **security-audit** | **stopped (investigable=0; 3/4 + tool, B398-B401)** | `RESEARCH-STATE-security-audit.md` | Consolida el hilo de SEGURIDAD disperso (~34 bloques: B75/B112/B113/B114/B160/B317/B384/B392-397) en un checklist accionable (SEC-01..18) + una HERRAMIENTA operativa `tools/niagara-security-audit.py` (read-only, inspecciona niagara_home + puertos vivos). **B398 (bootstrap):** auditó el SUPERVISOR DE PRODUCCIÓN vivo = **13 hallazgos (5 crít/4 alto/4 med)**. Confirmó en vivo: moduleVerificationMode=low, truststore changeit, ACLs security/ Modify para Authenticated Users (B316 L-6), Webs.license con developer{skipModuleValidation=true}+smDeveloperMode (B18), cert TLS default ForRecoveryPurposes, blacklist off, program.requireSigning off, syslog off, HTTP 80 abierto, .bog plaintext. Combinación más peligrosa live: SEC-01+03+06 = precondiciones del incidente B75 co-ocurren en el supervisor. 4 gaps SA-G1..G4 (NEXT SA-G4 threat-model client-facing) | B398 |
| **signing-pki** | **stopped (8/8, investigable=0; B392-B396 + §12 live-validation B397)** | `RESEARCH-STATE-signing-pki.md` | La superficie de FIRMA/PKI de N4 como subsistema — el hilo antes DISPERSO (B18/B75/B113/B126/B321/B384 módulos · B2/B126 licencias · B350-356 Part 11 · B335-349 jsonToolkit · B287 BACnet/SC · B243 firmware) formalizado. **Capstone B392 (bootstrap):** reconcilia la contradicción de 4 vías del trust anchor con evidencia de disco del install vivo. TESIS: el corpus confundió TRES dominios de confianza como uno — A módulos (RSA-2048 X.509 estándar, `truststore.jks` password `changeit` + pin `signing.properties`), B licencias/vendor legacy (DSA-1024 XML `.certificate`, root Tridium 2003 params Sun never-expires), C TLS/Authenticode (cacerts+cacerts.bcfks BC-FKS, firmados con `.sig` RSA-2048). Cadena real de `baja.jar`: Niagara4Modules Code Signing → Honeywell CodeSign RSA CA → **Honeywell Product PKI RSA** (en OEM hasta los módulos CORE de Tridium están re-firmados por Honeywell). Corrige B113 (SEJOFA VIVO no "Angeles"; no hay `cacerts.bks`). Nuevos: `System.exit(-6)` en firma inválida requerida = DoS; truststore password default. REFINA la premisa "cualquier N4 acepta módulos Tridium" = condicional al anchor de fábrica compartido. 6 gaps SP-G1..G6 (NEXT SP-G2 integridad de datos: .dist/audit/history/.bog sin firma) | B392– |
| px-chart-classic | **stopped (8/8)** | `RESEARCH-STATE-px-chart-classic.md` | El sistema de charting **CLÁSICO** (`javax.baja.chart`, módulo `chart` Swing/Workbench) — el feed que px-editor-core y B201 declararon "otro focus". 67 clases distintas medidas (rt 5 / wb 62; API pública 35+9). 8 gaps: H1 modelo+jerarquía, H2 ejes/render, H3 binding a histories, H4 consumidores + §14 vs B199/B201, H5 impl `com.tridium.chart`, H6 PDF+HX, H7 tests, H8 split rt/wb. Pregunta transversal: por qué N4 arrastra DOS sistemas de charting | B251–B259 |
| **webChart** | **stopped (9/9)** | `RESEARCH-STATE-webChart.md` | El framework `webChart` (`webChart-{rt,ux}`, 12 clases Java + 59 JS autorales) como SUBSISTEMA en AMPLITUD: el motor de render bajaux (`line/` 11 capas), modelo de series/escalas/sampling (`model/`), catálogo de tipos (line/donut/gauge/boxTable), comandos+interacciones, field editors, pipeline de export, la extensión `BIChartFactory`, y el `.chart` persistido. Bootstrapeado 2026-08-05 tras cerrar el hilo de charting de reports. **NO re-deriva** [B199] (espinazo: servlets, 4 series types, gauge) ni [B367] (veredicto de banda) — ambos REMITTANCE. 9 gaps W1-W9 + síntesis B377 + §18 retro | B368–B377 |
| **reports** | **stopped (9/9)** | `RESEARCH-STATE-reports.md` | El módulo core `report` (`report-{rt,ux,wb}`, `javax.baja.report.*` + `com.tridium.report.*`, 49 clases) como SUBSISTEMA: pipeline **scheduled grid→bytes→file/email** con cola serializada de un hilo. Nace de un pedido de cliente (reportes con rango + tabla tipo Excel + chart con marcas de alarma). **Tesis CONFIRMADA (B362): el entregable NO es una feature de `report` — el módulo aporta solo el wrapper schedule+entrega; las TRES patas de datos (tabla history, marcas de alarma, chart con bandas) son código CUSTOM rt-profile.** Raíz doble: `BBqlGrid` es visor de COMPONENTES (records `BStruct` sin `ordInSession` → NPE) + split de perfil rt/wb (chart/PDF/PX no cargan en station). 9 gaps R1-R9 cerrados. **NO reabre** charts (B199/B251-B259) ni alarmas (B44/B240): REMITTANCE | B357-B365 |
| **access-control** | **stopped (8/8, investigable=0)** | `RESEARCH-STATE-access-control.md` | El subsistema de AUTORIZACIÓN/RBAC de N4 (users/roles/permissions/categories/password-policy + enforcement/audit) como focus dedicado — el hilo antes DISPERSO (B11 modelo, B30 slot-level, B48 frontend, B341 runAsUser, B398/B490 hardening, B435 WB UI, B494/B510 auth-schemes). AUDIT-FIRST 2026-08-28. 8 gaps AC1-AC8 (B558-B566): AC1 **corrige B11 §11.3.5** (complejidad SÍ built-in `BPasswordStrength`), AC2 `BUserPrototypes` (merge roles=union expande), AC3 `BCategoryService` (ORD-prefix, cap 256≠64), AC4 encoders (hash PBKDF2-10k vs AES-reversible), AC5 SecurityDashboard SPI, AC6 audit-wiring (pluggable, tamper-evident-free), AC7 `BRoleHierarchies`, AC8 `UserMonitor`. + B560 runbook cloudflared (document-mode). §18 retro pending | B558–B566 |
| **sys-transfer** | **stopped (5/5, investigable=0)** | `RESEARCH-STATE-sys-transfer.md` | La primitiva GENÉRICA de transferencia `com.tridium.sys.transfer` (16 clases) — el motor detrás de cut/copy/paste/move/delete de Workbench, drag-drop, y el DeployToComp/ReplacingContext que templates (B578/B579) consumen. Genuinamente sin abrir (prior coverage = solo consumidores). Seed directo 2026-08-28. 5 gaps: ST1 factory+dispatch, ST2 DeployToComp+ReplacingContext, ST3 component strategies, ST4 file strategies+results, ST5 remote+WB consumer. B595-B599: ST1 factory+dispatch (make routes by target×source×action), ST2 DeployToComp polimórfico + ReplacingContext=handle-preservation (localiza B578/B579), ST3 component strategies (copy/move/reparent/export/delete), ST4 file strategies + TransferResult.undo() (WB paste undoable), ST5 remote (TransferCodec fox) + WB consumer (TransferUtil). §18 retro pending | B595–B599 |
| **hierarchy** | **stopped (7/7, investigable=0)** | `RESEARCH-STATE-hierarchy.md` | El motor de NAVEGACIÓN CUSTOM de N4 (`hierarchy`): árboles alternos por secuencia de LEVEL DEFINITIONS (Group/List/Query/Relation). Genuinamente sin abrir — [B5 §5.3.3] es overview shallow, [B565] solo el seam de roles, [B387] fila de licencia. AUDIT-FIRST 2026-08-28. 7 gaps: H1 modelo de level-defs, H2 caching (HierarchyCacheBuilder + fw(1300-1304)), H3 scopes+QueryUtil ForkJoin, H4 scheme ORD, H5 on-demand+contextParams, H6 permisos en el árbol, H7 transporte BOX/Fox. Primera cita del corpus a `guides-clean/Hierarchies/` (32 files). B584-B590: H1 modelo level-defs (tree, Group/Entity), H2 caching (job-built, mask baked, no auto-invalidation), H3 scopes+QueryUtil ForkJoin CPUs×8, H4 scheme ORD (leaf→componente real), H5 nav STATELESS por contextParams, H6 permisos (categoría AC3, ortogonal a rol B565), H7 transporte BOX/Fox. §18 retro pending | B584–B590 |
| **template-wb** | **stopped (5/5, investigable=0)** | `RESEARCH-STATE-template-wb.md` | La capa UI Workbench del subsistema template (`com.tridium.template.ui`, ~65 clases) — deepening de [B200 §200.6] (overview). Motor rt ya cerrado (B577-583). TW1 editores de binding Config+IO (3528 L, sustancial), TW2 import Excel (B200 solo cubrió export), TW3 wizard installapp (13 clases), TW4 Relation editor, TW5 integración .ntpl en WB. B591-B594 (4 bloques): TW1 editores Config+IO (BConfigBinding keyed-by-handle), TW2 import Excel (BulkDeployWorkbook POI + wizard), TW3 wizard install (backup-then-install), TW4+TW5 colapsados (Relation editor + .ntpl deployable + FindUsages/MakeModule). Techo honesto acertado. REMITTANCE: PxEditor→B191/198, BOG→B15, tags→B260-270. §18 retro pending | B591–B594 |
| **template** | **stopped (7/7, investigable=0)** | `RESEARCH-STATE-template.md` | El motor GENÉRICO de templates `com.tridium.template` como focus — el DEEPENING de lo que **[B200] nombró fuera de scope**: `api/impl/*TemplateSource` (T1), `ApplicationTemplateInstaller` (T2), `Mark`/`DeployToComp` de UpgradeUtil (T5), gramática XML del manifest (T3), subtemplates (T4), wire del BTemplateChannel (T6), resolución de TemplateManager + memory scheme (T7). AUDIT-FIRST 2026-08-28. NO re-deriva B200 (amplitud), B573 (wrapper provisioning), ni easyBinding (B36/B81). B1-B7 cerrados (B577-B583): T1 api façade+strategy, T2 ApplicationTemplateInstaller (ReplacingContext swap), T5 UpgradeUtil (save→transfer→restore por handle), T3 manifest schema (typed Value params), T4 subtemplates (containment, version-gated cascade), T6 BTemplateChannel wire, T7 TemplateManager resolution+memory scheme. §18 retro pending | B577–B583 |
| **provisioning** | **stopped (10/10, investigable=0)** | `RESEARCH-STATE-provisioning.md` | El subsistema de FLEET/provisioning `provisioningNiagara` como focus dedicado — antes DISPERSO (B16, B39 46-steps, B472 backup-over-Fox, B511 BJob, B14). AUDIT-FIRST 2026-08-28. 10 gaps PV1-PV10 (B567-B576): PV1 batchJob engine genérico (BBatchJob extends BJob, @AgentOn driver:DeviceNetwork), PV2 niagaraProv Fox channel (two-credential, no bypass), PV3 software dist (combining→1 txn/station, PBE dist), PV4 bootstrap/discovery (privileged over daemon, factory-default creds, reciprocal), PV5 async-action protocol, PV6 BProvisioningRobot escape-hatch (arbitrary Program, super-user-gated), PV7 template deploy (wrapper sobre módulo template), PV8 credentials batch (ambos stores, AC1-policy), PV9 license dist (portal/local, DSA-signed), PV10 ux BOX RPC (unrestricted+self-gate). §18 retro pending | B567–B576 |
| **interactive-composition** | **stopped (10/10, investigable=0)** | `RESEARCH-STATE-interactive-composition.md` | (B749-B750 añadidos: censo de ORGANIZACIÓN de ~30 módulos Honeywell — 5 sweeps Explore paralelos. Taxonomía B749 = 10 patrones P1-P10: P1 reusar la espina de Points del framework (BApplicationLogic=BPointDeviceExt, BMacro=BPointFolder), P2 partir por dominio en device-extensions, P3 tres planos config/estado/wire-map como bloques separados, P4 folders tipados con isParentLegal, P5 esqueleto congelado + población dinámica, P6 reuso vía Macro/palette templates (Venom/IRM apps = palette-only), P7 management separado de contenedores, P8 categoría=taxonomía de paquete no runtime, P9 tags/relations como overlay semántico, P10 substrato base compartido honIOBase. Playbook aplicado B750 → ColdRoomPan/CompPan/DashboardPan: 5 gaps accionables P2/P3/P4/P6/P9, secuencia deploy-safe. 4 gaps requires-execution abiertos.) El Wire Sheet de Niagara COMO superficie de flujo tipo Node-RED (node-red = 0 hits en corpus) y cómo hacer NUESTROS módulos interactivos, descubribles y sin desbordar ("que no sea cansino"). BOOTSTRAPEADO 2026-09-03 (pedido del operador: esquema base/hijos/padres/flujo + recomendaciones con tecnología nueva). NO re-deriva el skeleton (module-anatomy) ni el editor wiresheet (B429) — REMITTANCE. IC1+IC4 (B747): tabla término-a-término WireSheet↔Node-RED [CERT] (glyph=nodo, SlotBar hotspot=pin, BLink=cable, palette=drag, wsAnnotation=layout en BOG, valores vivos con color de estado vía PropertyBarGlyph, sin deploy=edit-is-effect), + los 3 motores FB del install (Niagara/kitControl live · Spyder compile→download). Cierra B735-G1 (pin=SUMMARY, `SlotBarGlyph.java:56`). IC2+IC3 (B748): playbook rankeado por impacto÷costo (SUMMARY-curation, componer hijos, facets/units, iconos, palette templates, tags) + conexiones modernas (capa low-code/Node-RED sobre oBIX/MQTT/REST; Niagara=motor de control, low-code=presentación). §18 retro pending | B747–B748 |

| **wb-ux-authoring** | **stopped (5/5, investigable=0)** | `RESEARCH-STATE-wb-ux-authoring.md` | Cómo los módulos CONSTRUYEN sus capas Workbench (-wb) y navegador (-ux) — censo de código (4 sweeps Explore) sobre Tridium core + ~10 módulos Honeywell + nuestros DashboardPan/chihuahua. BOOTSTRAPEADO 2026-09-03 (operador: "investiga todo sobre WB y UX de los módulos"). NO re-deriva el FRAMEWORK (B9/B22/B29/B427-432/B706/B707 = REMITTANCE). B751 (WB): la escalera "cuánto wb es suficiente" (rung 0 nada → 1 FieldEditor → 2 Manager → 3 View/Editor), recetas Manager/View/FieldEditor/Command→BJob, y la innovación Honeywell = framework de PLUGIN de device-model (BIHonDeviceModel). B752 (UX): 3 recetas de servido (servlet-SPA / vista bajaux @AgentOn / PX), 2 dialectos de canal bajaux (serverSideCall vs baja.rpc), taxonomía de bindings PX, y el contraste RBAC (vendors=unrestricted; los nuestros=OPERATOR_WRITE fail-closed). B753 (playbook): nuestros componentes están en rung 0 (no necesitan wb); mantener el servlet-SPA + RBAC real; PX/Fox/plugin solo on-need. 2 gaps requires-execution abiertos. | B751–B753 |

| **module-authoring** | **stopped (8/8, investigable=0)** | `RESEARCH-STATE-module-authoring.md` | Los ejes restantes de autoría de módulos (operador: eligió "versionado/upgrade-safety", luego "ve por todos"). 6 sweeps Explore. NO re-deriva el framework (B12/B20/B25/B5/B634/module-anatomy). B754 (versionado+matriz): version en module.xml→NModule→Version; boot resuelve deps required≤installed o FALLO DURO; NO hay hook de migración por módulo (migration-rt es OFFLINE); MATRIZ de supervivencia de datos = warningAndSkip (sobrevive) vs throw sin envolver (outage) — SAFE add/reorder/add-tag, LOSSY remove/rename, OUTAGE simple-retype + remove/rename enum-tag. B755 (BITS, pedido a mitad de corrida): Flags 32-bit (SUMMARY=8/HIDDEN=4/READONLY=1/TRANSIENT=2/OPERATOR=256…), BStatus 8-bit (DISABLED=1…UNACKED=128), BPermissions (OPERATOR_WRITE=2…ADMIN_INVOKE=64), BVersion relation bits (MEETS_MINIMUM=115). B756 (build/firma): vendor stamp, target-más-bajo, familia de plugins, .jar vs .dist, project-CA angelessigner + STORED repack. B757 (integración): BAbstractService registra-por-colocación, Sys.getService primero-registrado, todo BComponent es BINavNode gratis. B758 (tags/exposición): BSmartTagDictionary, BTagRule auto-tag, BCustomRelation, BObixAgent, Fox/BOX/QueryServlet, BQL-cursor. B759 (lexicon/doc): key=tipo/slot module-global + toFriendly fallback; CompPan-rt lexicon VACÍO; perfil -doc. B760 (AUDITORÍA accionable): lo que ya está bien + punch-list de 8 items rankeado + disciplina de versionado + secuencia deploy. §18 retro pending. | B754–B760 |

## Focus activo

**`jace8000`** (**STOPPED, investigable=0**, B459–B470) — **BOOTSTRAPEADO y CERRADO 2026-08-19** en una corrida
`/research-sdd` heavy contra el **JACE-8000 VIVO** en `192.168.1.140` (fase §12 dinámica, `live-install` →
SECRETS DISCIPLINE). Nace del pedido del usuario de documentar TODO sobre un JACE-8000: arquitectura, acceso a
la station, entrar al filesystem/sistema, entrar al platform (y sin Workbench), RE del protocolo platform para
copiar el `.bog`, y recuperación sin acceso al platform. 11 gaps J1-J11 + síntesis B470. **7 siete hilos (B470):**
(1) es un **appliance QNX** (QNX Neutrino / ARM Cortex-A8 NPM6xx / HotSpot JVM — **ni Linux ni Windows**, B459);
(2) **dos puertas, dos almacenes de credenciales** — station (:443 SCRAM users, :4911 Fox) vs platform daemon
niagarad (:3011/:5011, 403-to-GET, cuentas OS + passphrase) (B460/B461); (3) **dos dominios de cifrado en reposo**
— daemon-home = clave-solo-de-la-máquina (config.bog vivo **indescifrable fuera del equipo**) vs portable/.dist =
clave derivada de la passphrase (B466, §14 refina B464); (4) **filesystem QNX** /opt/niagara + /home/niagara, 4
rutas desiguales (File Transfer platform / /file station / consola serial / SSH-off) (B462); (5) **recuperación =
hardware + firma de Tridium** — USB clone / factory defaults / Platform Account Recovery (serial opt-8, conserva
datos, **key firmada por Tridium** atada al Host ID, 24h) → recovery≠bypass (B463); (6) **copiar el `.bog` sin
Workbench** es posible vía **BackupService station-side** (admin + cliente Fox = J8-G1) pero ningún RE vence el
cifrado/firma (B464); (7) **clonar está doblemente clavado** — Host ID (licencia) + passphrase (secretos) (B467/
B469). **Tesis:** Niagara protege "quién puede correr/poseer qué" con hardware+firmas vendor mucho más fuerte que
la lectura por un operador autorizado — instancia viva de la tesis B392. **Postura viva:** hardening fuerte
(SSH/telnet/Fox-1911 off, TLS1.3+HSTS) minado por certs default (platform expirado 2022, ForRecoveryPurposes) +
**credencial admin expuesta → ROTAR** (B468). Quedan **7 gaps hijo requires-execution** (J8-G1 cliente Fox para el
.dist, J3-G1 bytes del handshake platform, J11-G1 nmap TLS, etc.). SOURCES §5: 15 guías niagara-help registradas.

**BREAKTHROUGH Fox (B471-B474, §12 live, 2026-08-19):** se construyó desde cero un **cliente Fox** (stdlib) y se
demostró J8 **end-to-end contra el JACE vivo**: (B471) login SCRAM-SHA-256 por foxs:4911 con auth mutua
verificada (`app.name=Station`, method `n4digest`); (B472) RE del mecanismo de backup-over-Fox (canal `backup`
→ circuito → `{save=false}` read-only → stream ZIP; gate = permiso bit 48); (B473) **se extrajo el
`config.bog` vivo** (`.dist` 200.680 B, sha256 805139cf…, 43 entradas, station `JACE_UMBRELLA/config.bog` 7367 B)
**sin Workbench, read-only, solo con admin de station** — SECRETS DISCIPLINE: el `.dist` quedó en scratchpad,
solo se commiteó un manifiesto estructural (nombres+tamaños, Host ID enmascarado). El 2º hello **refinó** B459/B465
en vivo: **QNX 7.0.X + OpenJDK 25.412** (no HotSpot), station `JACE_UMBRELLA`, `hostId=Qnx-TITAN-44A2-****-****-363E`
(cierra J10-G1), `app.version=4.14.0.162`. (B474) J11-G1 cerrado: station **TLS 1.3-ONLY** (el server rechaza 1.2),
§14 refina B468. Tools nuevos: `sources/probes/B471-fox-client/{niagara-fox-client.py, niagara-fox-backup.py}`.
**Veredicto J8 probado:** con admin de station se obtiene el `.bog` sin Workbench; los secretos siguen cifrados
en reposo (clave-máquina/passphrase, B466). J8-G3 cerrado disk-first (B475: bit48=ADMIN_READ|ADMIN_WRITE, operador denegado). **Quedan 4 gaps hijo
DIFERIDOS a próxima sesión — solo cerrables con acceso físico al puerto serial Debug** (J7-G1 menú Alternate
Boot, J2-G1 tabla de montaje QNX, J3-G1 bytes del handshake niagarad, J5-G1 ACL por-archivo /file). Set
investigable-por-red EXHAUSTO. Próximo bloque global: **B476**.

**(previo)** **`signing-pki`** (**STOPPED, investigable=0**, B392-B396) — **BOOTSTRAPEADO y CERRADO 2026-08-07** en una
corrida `/loop`. Nace del pedido del usuario de entender "cómo funcionan las firmas de los módulos y cómo
cualquier N4 acepta módulos firmados con dichas firmas" + "la parte de seguridad". El hilo de firmas estaba
DISPERSO sin focus propio; ahora formalizado. 5 bloques, 4 gaps investigables cerrados (SP-G1/G2/G5/G7); los
4 restantes son requires-execution (live station) o blocked (install Tridium puro).

- **B392** (capstone) — reconcilia la contradicción de 4 vías del trust anchor: TRES dominios distintos
  (A módulos RSA-2048 X.509 `truststore.jks`+`signing.properties` · B licencias/vendor DSA-1024 `.certificate`
  · C TLS/Authenticode cacerts.bcfks). Cadena real `baja.jar` = Niagara4Modules Code Signing → Honeywell
  CodeSign RSA CA → **Honeywell Product PKI RSA** (en OEM hasta los módulos core de Tridium se re-firman con
  la PKI de Honeywell). Corrige B113. Nuevos: `System.exit(-6)`=DoS; truststore password default `changeit`.
  Refina la premisa: "cualquier N4 acepta módulos Tridium" es CONDICIONAL al anchor de fábrica compartido.
- **B393** (SP-G2) — la asimetría: Niagara firma CÓDIGO+ENTREGA, NO DATOS. Backup `.dist`/audit/history/`.bog`
  sin firma/MAC/checksum; solo cifrado per-campo (GCM tamper-evident solo ese campo). SignedDistFilter solo
  valida OS/NRE/VM.
- **B394** (SP-G1) — firmware byte-level: 3 posturas (HMI ECDSA-firmado→Honeywell Product PKI rama ECDSA;
  PanelBus IO flash CRUDA sin firma; standalone AES-cifrado). La asimetría llega al borde OT.
- **B395** (SP-G5) — verificación cripto independiente: los 3 vendor certs (Tridium incl.) firmados por un
  root DSA-1024 OCULTO embebido en `baja.jar` (`masterPublicKeyData`), NO auto-firmados. Corrige B392 §392.4.
  Dual root embebido DSA+ECDSA(v2).
- **B396** (SP-G7) — único canal de integridad opcional = syslog offload (TLS transport pero record plano,
  sin firma por-mensaje): tamper-resistance, no evidence. Nada firma el registro local.

**TESIS TRANSVERSAL:** Niagara protege criptográficamente "quién puede correr qué" (módulos/dist/firmware/
licencias, con roots OCULTOS embebidos en `baja.jar` = el patrón TPK) pero NO "qué pasó y no se puede negar"
(datos/evidencia sin firma). Y cuanto más cerca del I/O físico, más débil la integridad (B394). Próximo
bloque global: **B397**. Continuación posible: los 4 gaps requires-execution/blocked (fase §12 contra station
viva) o un focus nuevo.

**(previo)** **`platform-native`** (reabierto 11/12) — **Sub-pass Ghidra-grade, 2026-08-07, B379-B383.** Nace del pedido
del usuario de "investigar internals con descompiladores, lo más profundo posible". El focus estaba declarado
STATIC-closed (B124-B130) pero la mayoría a grado strings/RTTI; solo B125/B126 usaron Ghidra, y B126 leyó
nverify/libciper por strings. Este sub-pass decompiló los CUERPOS de función de los 4 binarios nativos núcleo,
revelando hechos de seguridad que el grado-strings no podía: **B379** nverify (11 opciones incl. 4 flags `skip-*`
de bypass + pin TPK RSA-2048 de 270B por `memcmp`; corrige B126 §126.4), **B380** njre launcher (provider BC
gated por licencia FIPS, gate anti-`-javaagent`, `loadDLL` sube B125 INFER→CERT; remite buildArgs/createVM a
B125), **B381** plat.exe (daemon = **LocalSystem + auto-start**; setsystempw passphrase por argv + policy de
complejidad nativa + DPAPI sin entropía + REG_BINARY bajo HKLM; refina B129 §129.3), **B382** libciper.so
(protocolo Sylk masterslave file-transfer QNX-ARM con DWARF: 496B records/485B blocks, dual CRC-16-CCITT+CRC-32,
sin cripto; sube B126 §126.5 a grado-cuerpo), **B383** síntesis. Método disciplinado: PRIOR-COVERAGE → REMIT →
DEEPEN (atrapó 2 premisas falsas antes de re-derivar). Tool nuevo: `tools/ghidra-scripts/DecompileByString.java`
(decompilación anclada por string para binarios sin símbolos). Quedan investigables NG1-G1 (gate sites nverify)
+ NG2b CERRADOS (B384/B385). **B384**: los 3 gate sites skip-* (--skip-signature-check salta el verify entero = bypass total confirmado). **B385**: taxonomía de los 107 nativos de nre.dll — hallazgo corrector: executeNativeDiagnosticsCommand0/addUserAccount0/getSystemPassword0 son STUBS return-0 en el supervisor Windows (solo embebido JACE/QNX); vivos = DPAPI encrypt/decrypt + AuthenticationUtil. **Sub-pass STOPPED, investigable=0.** Retro §18 + addendum. Próximo bloque global: **B386**.

**(previo)** **`webChart`** (stopped 9/9) — BOOTSTRAPEADO y CERRADO 2026-08-05 en una sesión (W1-W9, B368-B376). El framework de
charting moderno como subsistema en amplitud (71 artefactos: 12 Java + 59 JS). Nace del hilo de charting de reports (B366/B367): tras probar que la
banda de alarma es custom en las 4 rutas, el usuario eligió documentar el framework completo. 9 gaps audit-first
W1-W9 en `RESEARCH-STATE-webChart.md`. **W1 CERRADO B368** — el motor de render es un sistema de capas hecho a mano
sobre **D3 v3** (`d3.svg.line`, `d3.behavior.zoom`, API legacy): `runLayers` = dispatcher duck-typed promise-chained
sobre array de 9 capas hardcodeado; `DataLayer` full-redraw (regenera el `d` completo cada pasada, sin
incrementalidad) con branch tree por tipo de serie; coloreo por status gated (liga B367); Y autoscale solo bajo zoom
& `!isLocked()`, ticks delegados al modelo; zoom = rescale IN-MEMORY sin round-trip al servlet; motor CERRADO (sin
registry ni hook de primitiva). §14: `chartLimitMode` es del `ValueScale`/modelo (W2), no de la capa de eje.
**W2 CERRADO B369** — modelo de series/escalas/sampling. Autoscale = precedencia de 3 niveles por facets
(`facetsLimitMode` off/inclusive/locked, default off; override per-serie `chartLimitMode`). **HALLAZGO CLAVE:**
`samplingType` default=`average` GLOBAL por chart → un pico que cruza el límite se **PROMEDIA y desaparece** de la
línea (`samplingUtil.js:243` sum/count); el status se combina con OR (:232), así que el **color** de alarma
sobrevive pero la **altura** no; y un solo setting global no puede preservar a la vez los cruces `<12` (necesita
`min`) y `>28` (necesita `max`). Segundo motivo estructural de que marcar los cruces sea custom en webChart
(remittance decisión-relevante a reports/B362). **W3-W9 CERRADOS** en paralelo (B370-B376): W5 field editors
(5 grupos de settings sobre webEditors), W6 export (CSV sampleado + client-print + .chart; sin imagen/xlsx),
W3 catálogo (2 agentes: line + gauge; donut code-only; **gauge SIN zonas de límite**), W4 comandos (event bus +
6 Commands + drag→seriesFactory), W7 server (**DEFECTO de permisos** /schedule+/boxTable sendError sin return →
W7-G1 requires-execution; sin sampling server-side), W8 extensión (BIChartFactory marker + **sin license gate**;
seam agrega serie no primitiva de dibujo), W9 .chart (JSON definition + 12 time-range presets; tabs≠multi-chart).

**Hilos transversales (5):** (1) motor de render **hecho a mano sobre D3 v3** legacy; (2) **ningún surface dibuja
límites de alarma** (line/gauge/modelo) — coherente con B366/B367; (3) **abierto en factory, cerrado en dibujo** —
chart-factory sin licencia (B375) alimenta series, pero la banda sigue siendo fork del DataLayer (B368); (4) el
**sampling `average` borra los cruces** (B369) y el CSV los exporta sampleados (B371) → la ruta interactiva de B362
pierde fidelidad salvo max/min, y uno global no cubre `<12` Y `>28`; (5) charting **gratis** + read-gated por
`canRead()` salvo el defecto W7. Child gap abierto: **W7-G1** (requires-execution). **Síntesis capstone B377** +
**§18 retro** `retros/2026-08-05-webChart.md` (3 deltas WC-A fan-out paralelo de gaps independientes / WC-B split
defecto-estático-vs-explotabilidad-runtime / WC-C reconciliación por distinción de capa). Focus cerrado 10 bloques
B368-B377. Próximo bloque global: **B378**.

**(previo, cerrado)** — `reports` CERRADO 9/9 el 2026-08-05 (B357-B365 + síntesis B362). §18 retro:
`retros/2026-08-05-reports.md`. Addenda build-vs-buy B366 (Analytics) + B367 (webChart-bandas) — no reabren.

**reports** (el módulo core `report` como subsistema) — CERRADO 2026-08-05, 9 bloques B357-B365. Nació de un
pedido REAL de cliente (`/research-sdd reportes generados con rangos, analiticas con graficas y muestreo de
alarmas`): reporte sobre un RANGO de fecha/hora/mes → tabla tipo Excel + un CHART (Y=PSI, X=fecha adaptable) que
MARQUE los cruces de límite de alarma (bandas <12 / >28 crítica, 15-25 normal) con timestamp por alarma.
**Veredicto (B362, síntesis): SÍ se puede construir, pero el módulo `report` aporta SOLO el wrapper de schedule +
entrega file/email; las TRES patas de datos son código CUSTOM rt-profile.** Los seis hilos: (R1/B357) espinazo
`BReportService`→`BReportSource`→`BReport`→recipient, grid BQL, cola de un hilo, exporters CSV/text; (R2/B358) el
rango vive en `?period=`/BQL ORD, no en el módulo — preset relativo zero-code, rango libre = custom; (R4/B359)
**tabla history BLOQUEADA** — `BBqlGrid` es visor de COMPONENTES (antepone `select ordInSession`, `BatchResolve`),
y `BHistoryRecord` es `BStruct` sin `ordInSession` → NPE; 0 history grids; (R5/B360) alarmas SÍ consultables por
BQL (`alarm:|bql:...from openAlarms`, todos los campos incl. highLimit/lowLimit/presentValue en el facet bag) pero
mismo muro `BStruct`→NPE; gate operator-read; (R6/B361) **chart programado BLOQUEADO en el límite de perfil rt/wb**
— report-wb+chart-wb son perfil wb, no cargan en station; único chart = PDF MANUAL de Workbench vía
`BPxInclude`→`BChartPane`; sin API de bandas → custom en todo camino; (R3/B363) export = CSV/text, cada celda
stringificada a `BString`, BOM UTF-8 para Excel, **xlsx nativo ausente** (custom); (R7/B364) capa web = visor
read-only de tabla, paginación client-side con tope 3000 filas BQL, sort deshabilitado, **cero chart**; (R8/B365)
el builder de Workbench hardcodea `select ordInSession` → **la tabla history del cliente NO es autorizable por UI
stock** (§14 confirma B359 desde el tooling). Raíz doble reutilizada: `BBqlGrid`=visor de componentes +
split rt/wb. **Costo (B362 §362.5, [INFER])**: entregable completo ~110-190h → ~$5.5k-9.5k tarifa regional LATAM,
~$13k-23k integrador US/EU; ítem dominante = el renderer del chart con bandas (bloqueado dos veces). Alternativa
más barata = ruta interactiva webChart (otra necesidad: on-demand, sin push, sin tabla Excel).

**Addendum build-vs-buy (B366, 2026-08-05)** — NO reabre reports (sigue 9/9). Pregunta: ¿comprar la licencia de
**Niagara Analytics** entrega el chart de rango PSI con bandas de alarma, evitando el renderer custom que B362
marcó como costo dominante? **NO.** Analytics tiene 7 tipos de chart analíticos (Spectrum/Ranking/LoadDuration/...),
ninguno es un trend con bandas de límite; su frontend autoral tiene **0 código de alarma/banda**; sus charts se
construyen SOBRE el framework `webChart` (`BIChartFactory`), así que hereda el techo de webChart; y la guía oficial
encuadra alarma-vs-chart como salidas **paralelas**, nunca overlay. Veredicto: comprar Analytics recorta las patas
de **detección** (algoritmo→registro de alarma) y **agregación** (trend wrappers), pero NO la pata cara (chart con
bandas) — custom en ambos caminos. **Confirma y refuerza B362.** Abre el hilo "¿webChart renderiza regions/
y-grid-lines ligados a la extensión de alarma de un punto?" → próximo bloque, bajo la línea B199, NO bajo reports.

**B367 webChart-bandas (2026-08-05)** — cierra el hilo que abrió B366; bajo la línea de charting B199, NO es gap de
reports. ¿webChart renderiza bandas de límite de alarma nativas? **NO tampoco.** Solo colorea el sample por status
(`isAlarm()`→color, sin banda); el motor de render es un **array de 11 capas HARDCODEADO** en `line/Line.js:25` sin
`LimitLayer` y sin `addLayer`/registry → agregar la banda = **forkear la composición**, no es plugin. Sweep del
módulo: 0 `limitLine`/`threshold`/`band`. Clincher estructural: el feed `{t,v,r,s}` **no transporta valores de
límite**. Doc de developer + niagara-help: silencio (ceros reales). **VEREDICTO: la banda `<12`/`>28` es custom
INTRÍNSECO en las 4 rutas de charting** (report / BChart clásico / Analytics / webChart) — confirma B362 por 5ta
vez. webChart sigue siendo la ruta más barata (heredas el motor, forkeas una capa), pero NO porque las bandas sean
nativas. Orden de costo: webChart-fork < BChart-subclass < headless-desde-cero. **La pregunta de charting queda
CERRADA.**

Cola disponible sin re-bootstrap (§16): `niagara-network-supervisor` (planned 0/5, N1), `px-tail`
(planned 0/3, P1 webEditors), `oem-honeywell-tail` (paused 9/17, U10-U15+U1b/U1c). Opcional no-encolado: focus
`webChart` completo (el framework solo fue tocado en B199; el motor de capas/modelo de series queda sin mapear en
amplitud — pero nada de eso cambia el veredicto de la banda). Próximo bloque global: **B368**.

**(cerrado)** — `electronicSignature` CERRADO 7/7 el 2026-08-05 (B350-B356).

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
