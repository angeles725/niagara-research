# Bloque #75 — Incidente de seguridad: módulo no firmado abre 443 + borra audit (threat model + hardening)

**Fecha**: 2026-05-24
**Tipo**: Investigación de seguridad DEFENSIVA — reconstrucción de cadena de ataque (threat model) + plan de remediación
**Trigger**: incidente real reportado por cliente — una maquila fue comprometida; código atacante abrió un socket HTTPS en 443, ejecutó payload y borró todo rastro. Sin artefactos forenses (el malware se autolimpió). Premisa de partida del cliente: "N4 requiere firma para que un módulo abra esos puertos" → **REFUTADA en este bloque**.
**Source READ-ONLY**:
- Corpus decompilado Vineflower: `/home/cristian/modules/Prototipos/modulos/organized/baja/baja/vineflower/`
- Install real: `/mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162/` (`defaults/system.properties`, `security/licenses/Webs.license`)
**Methodology**: threat modeling desde source decompilado (NO forense — no hay station ni logs del incidente). Distingue CONFIRMADO (con evidencia file:line) de INFERIDO (hipótesis plausible sin evidencia directa).
**Cross-ref**: Bloque #18 (module signing + permissions + CSRF), Bloque #26 (standalone signing playbook), Bloque #27 (network surface + puertos), Bloque #30 (key rotation + audit), Bloque #31 (audit queue semantics), `notes/03-security.md`, engram #332 (saga httpapi skipModuleValidation).

---

## §75.0 — El reframe: la firma nunca protegió la red ni el audit

La premisa del cliente —y la intuición común— es que el modelo de firma de módulos de Niagara impide que código no autorizado abra puertos. **Es falso.** La firma protege un subconjunto específico de capacidades (reflection, class access, MBeans), **no** la apertura de sockets ni el acceso al audit trail.

Solo **6 permission groups** exigen firma (`requiresSignature()` → `true`, método decompilado `n()`):
`REFLECTION`, `ACCESS_CLASS`, `MBEAN_PERMISSION`, `HSM_SIGNING`, `PROTECTION_DOMAIN`, `THIRD_PARTY_PERMISSION`.

`NETWORK_COMMUNICATION` **no está en esa lista**:
- `NetworkCommunicationPermissionGroup.java:22` — no overridea `requiresSignature()`, hereda el default `false`.
- `NiagaraPermissionGroupFactory.java:196` — el check `if (group.requiresSignature() && !codeSource.isSigned())` nunca se dispara para red.

Consecuencia: **un módulo NO firmado puede declarar `NiagaraSocketPermission` / `URLPermission` y abrir cualquier puerto, incluido 443.** El permiso se registra en la policy activa en `ModuleClassLoader.java:102`.

---

## §75.1 — La cadena de ataque reconstruida (CONFIRMADA con file:line)

| Paso | Qué pasó | Evidencia | Certeza |
|------|----------|-----------|---------|
| 1. Entrada | Credenciales del **platform daemon** (3011 plano / 5011 TLS). Solo pide usuario/password digest. | `BDaemonSession.java:263` (`acquireCredentials`) | CONFIRMADO el mecanismo; INFERIDO el cómo (brute-force / defaults `niagara`/`niagara`) |
| 2. Deploy | `.jar` malicioso **sin firmar** con `module.xml` declarando `NETWORK_COMMUNICATION hosts=* ports=443`. Transferido y commiteado al filesystem; station reiniciada. | `InstallScenario.java:956` (`FileTransferMessage`), `BModuleInstallCommand.java:129` | CONFIRMADO: el deploy NO valida firma del jar, solo credenciales |
| 3. Bypass de firma | El módulo carga limpio porque `NETWORK_COMMUNICATION.requiresSignature()=false` → `ModuleManager.verifyModuleSignature` no lanza excepción. Sin `<java-permissions>`, `DefaultModulesFileManager.makeManagedFile:209` ni siquiera llama a `validateCertChain`. | `ModuleManager.java:340`, `DefaultModulesFileManager.java:209`, `ModuleClassLoader.java:89-93` | CONFIRMADO |
| 4. Abre 443 | `new SSLServerSocket(443)` amparado en la `NiagaraSocketPermission` registrada. | `NetworkCommunicationPermissionGroup.java:88-97` | CONFIRMADO la capacidad |
| 5. Ejecuta payload | Dentro del módulo o vía el socket abierto. | — | INFERIDO |
| 6. Borra el rastro | `Sys.setAuditor(null)` apaga el audit **sin pedir permiso**; `HistoryDatabaseConnection.deleteHistory` borra el AuditHistory salteando el check Baja; `LoggingPermission("control")` silencia `java.util.logging`. | `Sys.java:178`, `HistoryDatabaseConnection.java:40`, `LoggingPermissionGroup.java:51` | CONFIRMADO las capacidades |

**Ruta alternativa (entrada vía station, no daemon)**: superuser autenticado en la station puede usar un **BProgram** para ejecutar bytecode arbitrario sin firma (`BCode.java:202-214` — sin firma solo loguea warning con `program.requireSigning=false` default), corriendo bajo `ProgramProtectionDomain` que hereda los permisos globales de la JVM (`BCode.java:507-513`). Limitación: `Runtime.exec()` queda bloqueado salvo `BProgramService.allowProgramRuntimeExec=true` (default `false`).

---

## §75.2 — Los tres hallazgos críticos

| # | Hallazgo | Evidencia | Por qué importa |
|---|----------|-----------|-----------------|
| H1 | **El audit trail se apaga sin ningún permiso** | `Sys.setAuditor(null)` — `Sys.java:178-180`, asignación directa sin `checkPermission` | Explica el "no dejó rastro". No hay defensa de prevención nativa. |
| H2 | **Firma ≠ red.** Módulo sin firmar abre 443 | `ModuleManager.java:340` + `NetworkCommunicationPermissionGroup.java:22` | Refuta la premisa del cliente; es el eslabón explotado |
| H3 | **El deploy solo pide credenciales del daemon, no firma del jar** | `InstallScenario.java:956`, `BDaemonSession.java:263` | Es el punto de entrada real |

Secundarios: `HistoryDatabaseConnection.deleteHistory()` (`:40`) saltea el check de permisos de la capa Baja; `LoggingPermission("control")` (`LoggingPermissionGroup.java:51`) —sin firma— deja silenciar el logging entero.

---

## §75.3 — El enforcement real: fail-open vs fail-closed (precisión)

El parser de permisos **es fail-open**: `NiagaraPermissionGroupFactory.java:203-204` — el `throw` por "requires signature but not signed" cae en un `catch(Throwable)` que solo loguea `warning` + "Skipping element" y continúa el loop.

**Pero** el bloqueo real de carga vive en `ModuleManager.verifyModuleSignature` (`:340`), gobernado por `niagara.moduleVerificationMode`:

| Modo | Módulo sin firma + `NETWORK_COMMUNICATION` | Módulo sin firma + `REFLECTION` |
|------|---|---|
| `low` (**default en producción**) | **CARGA** (solo warning) | BLOQUEA |
| `medium` | **BLOQUEA** | BLOQUEA |
| `high` | **BLOQUEA** + rechaza self-signed (`ModuleManager.java:358-363`) | BLOQUEA |

Conclusión precisa: con `low`, un módulo no firmado que pide `REFLECTION`/`ACCESS_CLASS`/etc. **se rechaza** (fail-closed); uno que solo pide `NETWORK_COMMUNICATION` **carga igual** (fail-open). La barrera depende del modo, y el default `low` deja pasar el vector de red.

---

## §75.4 — Plan de hardening priorizado (settings reales)

### P0 — Corta el vector principal

| # | Acción | Setting real | Estado hoy | Cierra |
|---|--------|--------------|------------|--------|
| 1 | **Firma obligatoria de TODOS los módulos** | `niagara.moduleVerificationMode=high` en `defaults/system.properties:442` | `low` ⚠️ | El eslabón explotado: bloquea el módulo no firmado que abre 443 |
| 2 | Firma obligatoria de program objects | `program.requireSigning=true` | `false` | Vector BProgram (`BCode.java:214` hoy solo warning) |
| 3 | Blindar `skipModuleValidation` | Agregarlo al `commandLineBlacklist` (`Nre.java:839`) + confirmar sysprop ausente | No está en la blacklist ⚠️ | Activación por CLI (la licencia `Webs.license:40` tiene el feature, no se puede quitar) |

> **Caveat P0.1**: al subir a `high`, los módulos custom deben estar firmados con un cert que encadene al trust anchor de Honeywell (`signing.properties`). Validar el pipeline de firma antes de aplicar en producción o la station no levanta módulos legítimos. Ver Bloque #26 (standalone signing playbook) + Bloque #18.

### P1 — Reduce la superficie de entrada (red/OS)

- **Daemon TLS-only**: `BPlatformSSLSettings.sslOnly=true` (Platform Admin UI) → desactiva 3011 plano, deja solo 5011 TLS. Requiere licencia crypto ssl (`Webs.license:35`, activa).
- **Firewall externo** para 3011/5011 restringido a IPs de admin + 443 entrante. ⚠️ **Gap nativo**: N4 no tiene IP filtering en el daemon — va a nivel red/OS.
- ⚠️ **Gap nativo**: el daemon **no tiene account lockout** (el de `BUserService` solo cubre sesiones de station Fox/HTTP). Brute-force libre. Mitigar con fail2ban/IPS a nivel OS + credenciales fuertes (cambiar defaults).

### P2 — Detección / evidencia (no previene, pero responde al "no dejó rastro")

- **Syslog offload a SIEM externo**: `BSyslogSettings.enabled=true` + `serverHost` (hoy `false`). El `publish` es **síncrono dentro de `audit()`, antes** del borrado local (`BAuditHistoryService.java:98-103`, `SyslogAuditHandler.java:18-55`). Si hubiera estado activo, la evidencia completa estaría fuera de la station. **Audit a prueba de borrado local.**
- **Platform daemon log**: vive a nivel OS, independiente del audit de la station (`platformLogEnabled=true` default). Un atacante que solo compromete la station no lo borra.
- **Alertar** sobre warnings logger `loader` "No code signers for entry %s in module %s" (constante `ModuleClassLoader.java:73`, logger `Logger.getLogger("loader")` en `:600`, emitido `~:400`) y `program.notSigned` (`BCode.java:214`). [Corregido v2: el logger es `loader`, no `sys.module`; verificado en corpus contra B112.]

---

## §75.5 — Gaps sin mitigación nativa (aceptar / mitigar fuera de N4)

| Gap | Evidencia | Mitigación posible |
|-----|-----------|--------------------|
| `Sys.setAuditor(null)` sin guard ni permiso | `Sys.java:178` | Solo P2 (syslog captura antes del borrado) |
| `HistoryDatabaseConnection.deleteHistory()` saltea check Baja | `HistoryDatabaseConnection.java:40` | Syslog offload |
| `ProgramProtectionDomain` hereda policy de `CodeSource(null)` amplia | `BCode.java:507` | Editar `.policy` file manualmente para acotar `SocketPermission` de programs (avanzado) |
| Daemon sin lockout / sin IP filtering / sin granularidad para deshabilitar solo deploy | `BDaemonSession.java` | Red/OS (firewall, IPS) |
| Licencia con `smDeveloperMode` (`Webs.license:129`) + `skipModuleValidation` (`:40`) activos | — | No se quitan de la licencia; bloquear su activación vía blacklist (P0.3) |

---

## §75.6 — Veredicto

La firma de módulos de Niagara N4.14 **no es** un control de apertura de puertos ni de protección del audit. Con `moduleVerificationMode=low` (default de fábrica en este OEM Honeywell), un atacante con credenciales del platform daemon puede deployar un módulo no firmado que abre 443 y borra su propio rastro con APIs que no piden permiso. El incidente es **plausible sin ningún exploit de la firma** — es el comportamiento esperado de la configuración por defecto.

El quick-win de mayor ratio impacto/esfuerzo es **`niagara.moduleVerificationMode=high`**. La defensa que habría dado evidencia post-mortem es el **syslog offload a un SIEM externo**. Ambos están desactivados por default.

---

**Bloque cerrado**: 2026-05-24. Investigación read-only sobre source decompilado + install real. Sin validación empírica contra station viva (pendiente opcional: confirmar que `moduleVerificationMode=high` bloquea un módulo no firmado de prueba antes de tocar producción). Engram: `niagara/security/exploit-443-investigation` (obs #2174, #2176, #2180).
