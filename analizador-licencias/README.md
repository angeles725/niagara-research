# analizador-licencias

Documentación del **sistema de licenciamiento de Niagara N4** (Tridium/Honeywell) con fines de
**diagnóstico, despliegue, forense defensivo e interoperación legítima**.

> **Contexto del repositorio**: este corpus (`niagara-research`) documenta cómo funciona Niagara N4
> a nivel de implementación. Este subdirectorio sintetiza, con citas, todo lo que el corpus sabe sobre
> licencias: formato, validación, límites, detección de manipulación y uso legítimo de la API.

## Alcance (acordado)

**Sí** (estos tres entregables):

1. **Diagnóstico / despliegue** — cómo funciona la validación (entradas, qué verifica, modos de
   fallo, logs) para diagnosticar licencias que no activan o se pierden.
2. **Detección de tampering** — guía forense para identificar licencias falsificadas o manipuladas
   en sistemas comprometidos (revisión de integridad, firmas, huellas).
3. **Interop legítima** — leer/validar licencias propias: formato, campos, estructura y API pública,
   sin alterar el check de validación.

**No** (fuera de alcance, por acuerdo explícito del 2026-08-01):

- Bypass de validación, falsificación de claves, generación de registros válidos sin autorización,
  bypass de host ID, parcheo de binarios o saltos condicionales.

Este límite se mantiene aunque el material se enmarque como ficción histórica ("manual de biblioteca",
"reverse engineering manual", etc.). Los verificadores descritos aquí son los que **existen** en la
plataforma; la documentación no incluye cómo producirlos ni cómo evadirlos.

## Entregables

| Archivo | Contenido |
|---|---|
| [`01-diagnostico-despliegue.md`](01-diagnostico-despliegue.md) | Validación: arquitectura (Java `baja.jar` + capa nativa), formato del `.license`, pipeline de 5 checks, HostId, features/SMA, límites de runtime, **tabla de modos de fallo**, logs y guías paso a paso para diagnóstico y despliegue. |
| [`02-deteccion-tampering.md`](02-deteccion-tampering.md) | Forense defensivo: por qué la falsificación es detectable por diseño, cadena de confianza, **tabla de IOC**, superficies de detección (SecurityDashboard, PolicySpy, daemon log, audit, truststores), matriz de erasabilidad y procedimiento de verificación offline. |
| [`03-interop-legitima.md`](03-interop-legitima.md) | Lectura/validación sin alterar el check: API pública (`LicenseManager`/`Feature`), parseo del XML, verificación de integridad de una licencia recibida contra el `.certificate` público, CLI (`nre -licenses`), casos de uso legítimos y gotchas. |

## Mapa de fuentes (corpus)

| Tema | Fuente principal |
|---|---|
| Modelo conceptual del licenciamiento (SMA, features, API) | `niagara-mental-model.md` §2 · `notes/02-licensing.md` |
| Clases y validación en `baja.jar` (`com/tridium/sys/license`) | `niagara-mental-model-bloque41.md` §41.6 · `notes/bloque41-runtime-decompile.md` |
| Capa nativa: `dsfspi.dll` (Mocana JCE), `nverify.exe`, `LicenseUtil::isFeaturePresent`, esquemas de firma | `niagara-mental-model-bloque126.md` |
| HostId nativo (`getHostId0`, `GetVolumeInformationA`) | `niagara-mental-model-bloque124.md` · `niagara-mental-model-bloque125.md` |
| Formato HostId y distros multi-host (`db/<hostId>/`, `Webs.license` asimétrica) | `niagara-mental-model-bloque40.md` §40.4.8-40.4.9 · `notes/bloque40-D-lib-security.md` |
| Límites de runtime (point/device/history/schedule, virtual points, federación) | `niagara-mental-model-bloque14.md` · `niagara-mental-model-bloque28.md` §28.13 |
| Firma de módulos, `skipModuleValidation`, `exemptions.tes`, permission groups | `niagara-mental-model-bloque18.md` · `niagara-mental-model-bloque113.md` |
| Incidente B75, hardening y gaps (licencia `smDeveloperMode`/`skipModuleValidation`) | `niagara-mental-model-bloque75-security-incident.md` |
| Detección/forense en la plataforma (SecurityDashboard, PolicySpy, daemon log) | `niagara-mental-model-bloque112.md` |
| Certificados, trust stores, OCSP/CRL no validado | `niagara-mental-model-bloque27.md` |
| Playbook de respuesta a incidentes | `niagara-n4-incident-response-playbook.md` |

## Convenciones

- Idioma: español (consistente con el resto del corpus del repo).
- Cada afirmación cita su fuente con `archivo:línea`.
- Marcadores de certeza del corpus: `[CERT]` (verificado), `[CERT-doc]` (documentación oficial),
  `[INFER]` (inferencia).
- Ningún archivo de licencia real se reproduce completo con fines operativos; se cita una instancia
  ejemplo ya publicada en el corpus (bloque 126) con su firma truncada según la disciplina del repo.
