# Prompt — Mejorar module-navigator (próxima sesión)

**Abrir en**: `\\wsl.localhost\Ubuntu\home\cristian\Honeywell\OptimizerSupervisor-N4.14.0.162\module-navigator`

---

```
Hola. Vengo de una sesión de investigación (2026-04-20) donde usé module-navigator
contra 926 JARs de Niagara N4 Honeywell para construir un mental model completo
(Estructura, Licensing, Security). La investigación salió bien, pero en el camino
identifiqué gaps concretos de la herramienta que valen la pena atacar.

Esta sesión es para MEJORAR module-navigator. No investigación, no mental model:
trabajo de herramienta.

## Hidratar contexto primero

Ejecutá antes de responderme:

1. `mem_search` con query "niagara mental model 2026-04-20" y project="niagara-research"
   — trae los 3 bloques consolidados para entender qué preguntas respondió el tool
   y qué no pudo.
2. `mem_context` con project="niagara-research"
3. Leé `/home/cristian/niagara-research/niagara-mental-model.md` — el output final
   de la sesión previa. Te da idea concreta de qué comandos se usaron y dónde
   hubo que improvisar con `grep` o fallback.
4. Leé `ROADMAP.md`, `GAPS.md`, `VALIDATION_GAPS.md` de este directorio — ver
   qué ya está planeado vs los gaps nuevos que traigo.

## Gaps identificados empíricamente en sesión 2026-04-20

### GAP 1 — Bytecode-only classes (BLOQUEANTE para security research)

**Síntoma**: clases críticas quedaron "black box" porque Vineflower no las decompiló:
- `com.tridium.crypto.core.cert.CertificateChainValidator` — reglas exactas de validación
- `com.tridium.crypto.core.cert.CoreCryptoManager.validateCertChain()` — body del método
- `com.tridium.nre.security.NiagaraSocketPermission.implies()` — lógica de matching
- `PolicyIntegrityChecker` — cómo verifica PKCS7 en bin/policy/

**Impacto**: sub-agents tuvieron que inferir reglas de los callers en vez de leer
el código real. Perdió precisión.

**Propuesta**:
- Comando `javap <class> [-c] [-p]` — fallback automático para bytecode cuando
  no hay `.java` decompilado. Parse stdout y presentarlo legible.
- Integrar **Procyon** o **CFR** como fallback de Vineflower (ambos manejan
  mejor bytecode ofuscado/extraño).
- Flag `--force-decompile` en `source <class>` que pruebe 3 decompiladores en
  cascada y devuelva el mejor.

### GAP 2 — ZKM deobfuscation awareness

**Síntoma**: 37 de 95 callsites de `checkFeature` estaban ZKM-obfuscados. El tool
los contó como "unresolved" pero no intentó recuperación.

**Observación empírica**: `com.tridium.sys.Nre` (1,495 líneas) está marcada como
"ZKM v52 obfuscated" pero no hay comando que liste qué clases están afectadas
y cuánto del corpus está comprometido.

**Propuesta**:
- Comando `zkm-audit` — lista clases obfuscadas, estima ratio de unresolved
  por módulo.
- Comando `zkm-guess <class>` — heurística de mapping (strings comunes, method
  signatures, xref patterns) para sugerir el nombre real de métodos `a()`, `b()`, etc.
- Flag en `class` / `methods` que marque métodos ZKM explícitamente.

### GAP 3 — License XML parsing structured

**Síntoma**: `license-inspect` lista archivos, pero para responder "qué features
tiene `developer` con todos sus atributos" tuve que hacer `cat` + eyeball XML.

**Propuesta**:
- Comando `license-feature-detail <vendor> <feature>` — devuelve estructurado:
  todos los atributos del XML (`expiration`, `sma.exempt`, `moduleDev`,
  `skipModuleValidation`, `*.limit`, etc.) + paths donde aparece.
- Comando `license-cross-ref` — cruza archivos `.license` con callsites de
  `checkFeature(vendor, name)` en código. Detecta features licensed-pero-unused
  (potencial trimming) y features used-but-unlicensed (risk).
- Comando `license-verify-signature <file>` — valida la firma XML del archivo.

### GAP 4 — module.xml extraction/introspection

**Síntoma**: los sub-agents no pudieron ver el contenido real de un `META-INF/module.xml`
runtime. Sólo infirieron campos de los parsers (`DefaultModulesFileManager`).

**Propuesta**:
- Comando `module-xml <module>` — extrae y muestra el `META-INF/module.xml` real
  del JAR. Pretty-print XML.
- Comando `module-xml --permissions <module>` — solo el bloque `<permissions>`.
- Comando `module-xml --deps <module>` — solo `<dependencies>`.
- Schema doc: extraer de los parsers Java un schema inferido de todos los
  campos que leen + optional vs required.

### GAP 5 — Policy file integrity verification

**Síntoma**: `policy-inspect` muestra contenido de `bin/policy/java.policy` pero
NO verifica la firma PKCS7 embebida. Imposible saber si el archivo está íntegro
sin re-implementar PolicyIntegrityChecker.

**Propuesta**:
- Comando `policy-verify` — parsea bloque `BEGIN NIAGARA SIGNATURE`, corre
  `openssl cms -verify` o equivalente con el cert de signing.properties.
- Output: firmado-válido / firmado-inválido / sin-firma + cadena de certs usada.
- Mismo para `java.security` y `signing.properties`.

### GAP 6 — Cert chain simulation

**Síntoma**: `bypass-status` dice si el bypass está ACTIVE, pero NO hay forma
de simular "¿este JAR custom pasaría la validation?" sin cargarlo en Niagara.

**Propuesta**:
- Comando `cert-validate <jar>` — extrae CodeSigner chain del JAR, simula
  `CertificateChainValidator` con las reglas documentadas (self-signed check,
  issuerDN match vs signing.properties, timestamp, extensions).
- Output: PASS/FAIL + razón específica (ej. "issuerDN mismatch: expected
  'CN=Honeywell CodeSign RSA CA' got 'CN=My Dev CA'").
- Comando `cert-dump <jar>` — dump estructurado de toda la metadata del cert
  chain (issuer, subject, serial, validity, extensions, key usage).

### GAP 7 — Permission denial trace ("what would block X")

**Síntoma**: `permission-flow` muestra flujo genérico. No hay forma de simular
"si agrego esta clase con este permission group, ¿qué pasaría al cargar?".

**Propuesta**:
- Comando `simulate-load <jar>` — simula todo el flow: verifyJarEntrySignature
  → validateCertChain → permission group resolution → first permission check.
  Reporta cada etapa como PASS/FAIL con explicación.
- Comando `permission-deny-reasons <class> <action>` — dado una clase y una
  acción (ej. `java.net.Socket.connect`), lista las razones por las que
  podría denegarse bajo las reglas del install actual.

### GAP 8 — Config files index

**Síntoma**: no hay comando para parsear/buscar dentro de:
- `stations/*/config.bog`
- `defaults/system.properties`
- `platform/platform.properties`
- `security/` files varios

**Propuesta**:
- Comando `config <file>` — muestra file parseado (BOG XML, properties).
- Comando `config-grep <regex>` — busca en todos los archivos de config.
- Comando `config-prop <key>` — busca una propiedad específica across files.

### GAP 9 — Decompilation coverage report

**Síntoma**: no sé cuánto del corpus está decompilado OK vs parcial vs bytecode-only.
Importante para saber confiabilidad de búsquedas.

**Propuesta**:
- Comando `decompile-coverage` — reporte: X% top-level classes tienen `.java`,
  Y% son empty/stub, Z% tienen errores de decompilación.
- Comando `decompile-errors <module>` — lista clases que Vineflower falló en
  decompilar y el mensaje de error.

### GAP 10 — Narrative "ask" (LLM-assisted Q&A)

**Síntoma**: existe comando `ask` pero no sé qué tan profundo es. En sesión pura
muchas veces necesitaba cruzar 3-4 comandos manualmente cuando una query
natural hubiera sido más rápida.

**Propuesta**:
- Revisar `ask` actual: ¿usa LLM? ¿qué indexes consulta?
- Si no existe o es shallow, diseñar `ask-deep` que:
  * Acepta query natural
  * Hace plan: lista 5-10 subcomandos que correría
  * Ejecuta y sintetiza respuesta
- Opcional: integración con un LLM local (Ollama) para ofline mode.

### GAP 11 — Cross-reference station runtime vs code

**Síntoma**: no hay forma de linkear "este BComponent en config.bog → este `.java`
en corpus". `bog-trace` existe pero es limitado.

**Propuesta**:
- Extender `bog-trace` a nivel de instancia, no solo tipo.
- Comando `station-health` — chequea un `stations/{name}/` contra corpus:
  tipos usados vs tipos disponibles, módulos faltantes, licencias requeridas
  vs licenciadas.

### GAP 12 — NRE boot simulation

**Síntoma**: entender el orden de eventos en boot requería cruzar 4-5 clases
manualmente.

**Propuesta**:
- Comando `boot-sequence` — simula/documenta fases del arranque del NRE:
  JVM init → security manager install → policy load → registry rebuild →
  station.start → service init loop. Para cada fase: clases involucradas +
  permisos requeridos + puntos de fallo documentados.

## Metodología

1. Empezá leyendo los 4 documentos listados en "Hidratar contexto" arriba.
2. Priorizá: te propongo orden por impacto (1, 5, 6, 4, 3, 2, 7, 8, 9, 11, 12, 10)
   pero es discutible. Proponé tu ranking y lo acordamos.
3. Para cada gap aceptado:
   - Diseñá el comando (nombre, args, output format).
   - Identificá qué index/files necesita (puede requerir builder nuevo).
   - Implementá en `tools/module_nav_lib/` como módulo dedicado.
   - Registrá comando en `tools/module_nav.py`.
   - Verificá con caso real extraído de la sesión previa.
4. Actualizá README.md y ROADMAP.md con fases nuevas.
5. Guardá decisiones arquitectónicas a engram (project="module-navigator-dev"
   si no existe creálo, sino "niagara-research").

## Reglas

- Zero deps externas nuevas sin justificar (la tool es Python stdlib only, eso
  es una feature, no un bug).
- Si un gap requiere dep externa (ej. Procyon jar, Ollama), proponelo primero
  como opcional con fallback graceful.
- Tests: para cada comando nuevo, caso empírico de la sesión 2026-04-20 como
  regression.
- Español rioplatense, tono warm/directo, sin emojis.

## Primer paso concreto

Hidratá contexto, leé los docs, y mostrame:
1. Tu lectura de los 12 gaps: ¿cuáles coincidís? ¿cuáles revisarías? ¿hay algún
   gap adicional que veas ahora con código en mano?
2. Tu propuesta de ranking de impacto.
3. Plan de ataque para los primeros 3 gaps (diseño, no implementación todavía).

No toques código hasta que alineemos.

Arrancamos.
```

---

## Referencias engram principales

- `niagara/estructura/profiles-registry-fox` — Bloque 1 consolidado
- `niagara/licensing/sma-features-honeywell-overlay` — Bloque 2 consolidado
- `niagara/security/full-pipeline-cert-grants` — Bloque 3 consolidado
- Session summary 2026-04-20 — qué se hizo + qué faltó

## Paths clave

- Working dir: `/home/cristian/Honeywell/OptimizerSupervisor-N4.14.0.162/module-navigator/`
- Mental model generado: `/home/cristian/niagara-research/niagara-mental-model.md`
- CLI entry: `tools/module_nav.py`
- Indexes: `indexes/` (12 archivos, ~1.35 GB)
