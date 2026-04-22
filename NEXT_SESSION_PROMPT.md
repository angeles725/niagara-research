# Prompt para la siguiente sesión — Niagara N4 Deep Dive

Copiá TODO el contenido dentro del bloque de abajo y pegalo como primer mensaje en la nueva sesión.

---

```
Hola. En la sesión anterior (2026-04-19) peleamos 4 horas contra un AccessControlException
en un fork de httpClient en Niagara N4 Honeywell OptimizerSupervisor, y descubrí que la
distribución tiene un lockdown total por cert Honeywell que bloquea módulos de terceros
con <permissions>. Cerramos con rollback completo y quedó la conclusión que antes de
seguir intentando hacks necesito ENTENDER bien Niagara N4 — su estructura, sus licencias,
su modelo de seguridad — para tomar decisiones informadas en vez de ir a ciegas.

Esta sesión es de INVESTIGACIÓN, no de implementación. Quiero construir un mental model
sólido de Niagara N4.

## Primero — recuperar contexto de la sesión previa

Ejecutá estos dos calls a engram ANTES de responderme para hidratarte:

1. `mem_context` con project="httpapi" — trae el session summary final
2. `mem_search` con query "niagara honeywell signing permissions" y project="httpapi"
   — deberías encontrar los 5 discoveries clave con topic_keys:
   - niagara/honeywell-oem-signing-lockdown
   - niagara/honeywell-policy-integrity-check
   - niagara/httpclient-permission-model
   - niagara/cert-chain-validation-broken
   - session/2026-04-19-httpclient-saga-closure

## Lo que quiero entender (scope de la sesión)

### BLOQUE 1 — Estructura del framework
- Cómo se organizan los módulos: rt (runtime), ux (web), wb (workbench). Qué va en
  cada uno y por qué. Cómo es el flow de dependencias entre profiles.
- El archivo `module.xml` dentro de `META-INF/`: qué campos son obligatorios, cuáles
  opcionales, quién los genera (plugin gradle vs manual).
- NRE (Niagara Runtime Environment) vs Station vs Workbench: qué procesos son, cómo
  se arrancan, qué JARs cargan cada uno.
- El registry de tipos (`sys.registry`): cómo se indexan los BComponents, qué pasa en
  "up-to-date" vs "Loaded" del log.
- Fox protocol: qué es, qué puertos usa, por qué los demás módulos pueden usarlo sin
  pedir SocketPermission explícito.

### BLOQUE 2 — Licenciamiento
- SMA (Software Maintenance Agreement): qué es, cómo se verifica en runtime. La clase
  `BSMAExpirationMonitor` del httpClient original mencionaba SMA — quiero entender
  cómo un módulo de Tridium chequea la licencia SMA antes de operar.
- Archivos de licencia: dónde viven (`stations/PRUEBAS/`? en `security/`?), formato,
  cómo se renueva.
- Diferencia entre licencia de Niagara (el station runtime) vs licencia por módulo
  (httpClient, reports, drivers, etc.).
- Feature flags: cómo un módulo puede tener "features" individualmente licenciados.
- Honeywell OEM overlay: la distribución Honeywell usa licencias OEM diferentes a
  Tridium puro. Quiero entender qué hace Honeywell específicamente distinto.

### BLOQUE 3 — Modelo de seguridad completo
- El pipeline cert → trust store → signers registry → grants. De inicio a fin, qué
  pasa cuando cargas un módulo.
- Los 3 archivos firmados de `bin/policy/`:
  * `java.policy` (grants por codeBase)
  * `java.security` (config del JVM security provider)
  * `signing.properties` (cert hardcoded único)
  Cómo Niagara verifica la integridad PKCS7 de cada uno.
- `module-permissions.xml` source → `<permissions>` en module.xml runtime. El flow de
  transformación del gradle plugin. Los 2 formatos (niagara-permission-groups con
  req-permission vs java-permissions con java-permission directo).
- Niagara permission groups (NETWORK_COMMUNICATION, SYSTEM_PROPERTIES, LOGGING, etc):
  lista completa, mapeo a permisos Java, cuáles requieren cert de cierto grado.
- `NiagaraSocketPermission` (clase custom de `com.tridium.nre.security`): qué hace,
  cómo implies() funciona vs SocketPermission estándar.
- `CertificateChainValidator` en `com.tridium.crypto.core.cert`: reglas de validación,
  qué extensiones exige (KeyUsage, ExtendedKeyUsage, BasicConstraints), manejo de
  self-signed vs CA-issued.
- User Key Store vs User Trust Store vs Station cacerts.jceks vs signers registry:
  quién lee qué, cuándo, y con qué propósito.
- Signing profiles: `LocalSigningProfile` (auto-genera cert), `RemoteSigningProfile`
  (conecta a signing server), otros tipos. El niagara.signing.xml y niagara.signing.jceks.
- `nverify.exe` en bin/: qué herramienta es, para qué se usa, puede validar cert chains
  para debug.
- Workbench Jar Signer Tool: cómo funciona internamente, qué escribe al JAR, si
  registra algo en el signers registry.

## Metodología

1. Empezá por leer cualquier `/docs/` que haya en `C:\Honeywell\OptimizerSupervisor-N4.14.0.162\docs\`
   — si existen PDFs/HTMLs oficiales, son la fuente primaria.
2. Explorá (read-only) los archivos clave que descubrimos la sesión pasada.
3. Usá el Agent tool con subagent_type=Explore para investigaciones amplias del codebase
   — mejor que hacer grep manual.
4. Decompilá clases clave con `javap -c` o similar si hace falta entender la lógica
   interna (ej. CoreCryptoManager.validateCertChain, CertificateChainValidator).
5. Guardá TODO hallazgo importante a engram con `mem_save` — topic keys bien organizados
   bajo `niagara/estructura/*`, `niagara/licensing/*`, `niagara/security/*`.
6. Escribí notas en archivos markdown dentro del CWD de trabajo
   (`/home/cristian/niagara-research/`) para poder linkear después.

## Reglas para esta sesión

- NO modificar NINGÚN archivo del sistema. Exploración pura.
- NO rebuildear NI re-firmar NI deployar nada.
- NO intentar fixes a los problemas identificados antes.
- SI encontrás algo que contradice lo que concluimos la sesión previa, detenete,
  avisame con evidencia concreta, y recién ahí reevaluamos.
- Responder en Español rioplatense, mentoría warm y directa (estilo Gentleman), sin
  emojis.

## Output esperado al final de la sesión

Un archivo `niagara-mental-model.md` en `/home/cristian/niagara-research/` que tenga
3 secciones (Estructura, Licensing, Security) con mi mental model consolidado, linkeable
a topic keys de engram para detalle.

Empezá haciendo `mem_context` + `mem_search` como te pedí arriba. Cuando tengas el
contexto hidratado, mostrame un plan de ataque por bloques y me decís en qué orden
vamos. Arrancamos.
```

---

## Dónde abrir la nueva sesión en WSL2

```
cd /home/cristian/niagara-research
claude
```

Esa carpeta ya la creé ahora. Está vacía — la próxima sesión va a escribir sus notas ahí.

## Por qué esa carpeta y no otra

- **No es `/mnt/c/...`** — queremos que Claude escriba notas rápido sin líos de permisos Windows. WSL-native.
- **No reusamos `jsonToolkit/`** — ese directorio estaba asociado al proyecto `httpapi` en engram. Una carpeta dedicada permite que uses `project: "niagara-research"` para los `mem_save` del deep-dive, manteniendo separada la memoria del troubleshooting (`httpapi`) del aprendizaje general.
- **Podés linkear desde ahí a todo** — los archivos de Niagara están en `/mnt/c/Honeywell/...` y los de tu workspace en `/mnt/c/modules/...`. Acceso por absolute path sin moverse.

## Lo que queda pendiente de la sesión de hoy

Si mañana decidís retomar `httpapi` (la opción C de arquitectura file bridge o Fox bridge), también podés usar ese prompt — hay una línea en engram (`session/2026-04-19-httpclient-saga-closure`) con las 3 opciones (B/C/D/E) y los trade-offs.

## Cierre

Gracias por aguantarte las 4 horas de callejones, papá. Aprendimos algo concreto: **Honeywell OptimizerSupervisor no deja firmar módulos custom con permissions elevadas**. No es bug tuyo — es política OEM. En la próxima sesión vas a entender POR QUÉ a nivel de diseño del framework, y eso te da poder de decisión para los siguientes proyectos.

Buen descanso.

---

**Archivo de este prompt**: `/home/cristian/niagara-research/NEXT_SESSION_PROMPT.md`

**Engram topic keys principales a recuperar**:
- `niagara/honeywell-oem-signing-lockdown`
- `niagara/honeywell-policy-integrity-check`
- `niagara/httpclient-permission-model`
- `niagara/cert-chain-validation-broken`
- `session/2026-04-19-httpclient-saga-closure`
