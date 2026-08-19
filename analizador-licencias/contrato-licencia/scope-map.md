# Mapa de alcance ↔ corpus — Acuerdo OEM Tridium (§3, §6) vs. investigación Niagara N4

> **Propósito.** Cruzar las áreas de evaluación que el Acuerdo OEM enumera (Secciones 3 y 6) contra lo que la
> investigación `niagara-research` **ya tiene documentado en disco**, para mostrar avance concreto y delimitar
> qué queda por hacer. Este documento es **planeación y documentación** — no ejecuta pruebas ni toca ningún
> sistema vivo. La ejecución de las pruebas de evasión requiere la autorización firmada auténtica (pendiente).
>
> **Regla de lectura.** "Cubierto" significa que el *diseño/mecanismo* está documentado con evidencia citada
> (marcadores `[CERT]`/`[CERT-doc]` de la metodología). **No** significa que la evasión esté demostrada: la
> demostración empírica es justamente la parte que espera autorización + entorno de laboratorio.
>
> Fecha: 2026-08-10 · Fuente de bloques: corpus `niagara-research` (numeración global).

---

## Resumen ejecutivo

| Estado | Áreas |
|---|---|
| **Estático cubierto** (mecanismo documentado) | host-id · firma de módulo · entitlements/features · certificados/trust · validación de integridad |
| **Parcial** (modelado, falta confirmación en vivo) | expiración (enforcement runtime) · revocación/CRL |
| **Gap = requiere autorización + laboratorio** | demostración empírica de evasión por área · fail-closed en boot con licencia manipulada · OTA PanelBus/HMI |

**Lectura de una línea:** el entendimiento de *cómo funciona* cada control ya está en gran parte hecho; lo que
falta es la **demostración dinámica**, que es exactamente lo que el Acuerdo autoriza a ejecutar *tras* la firma.

---

## 1. Identidad de host (host-id)

**Acuerdo:** §3 y §4 — "identidad de host" como control a evaluar; §6.1 "identidad de host".

| Aspecto | Estado | Bloque(s) | Nota |
|---|---|---|---|
| Algoritmo de derivación del Host ID | **Cubierto** `[CERT]` | **B424** | `getHostId` = *fold* XOR **no criptográfico** de 8 bytes sobre 4 fuentes (hidden key + RegisteredOwner + product id + volume serial de C:), vendor "tridium" hardcodeado. Corrige/sube B124 (INFER→CERT). |
| Ubicación (binario nativo) | **Cubierto** `[CERT]` | B424, B380 | `njre.dll` / `NreWin32`. |
| Base estática previa | Cubierto | B124 | Grado strings/RTTI; superado por B424. |

**Gap:** demostrar en laboratorio que una alteración de cualquiera de las 4 fuentes cambia (o no) el binding
→ **requiere autorización + host de prueba**.

## 2. Firma de módulo (module signature)

**Acuerdo:** §3, §4 "firma de módulo"; §6.1 "validación de firmas, certificados, módulos".

| Aspecto | Estado | Bloque(s) | Nota |
|---|---|---|---|
| Cadena real de firma de módulos | **Cubierto** `[CERT]` | **B392** | `Niagara4Modules Code Signing → Honeywell CodeSign RSA CA → Honeywell Product PKI RSA`. En el OEM hasta los módulos core de Tridium se re-firman con la PKI de Honeywell. |
| Verificador nativo | **Cubierto** `[CERT]` | **B379**, B321, B384 | `nverify`: 4 flags `skip-*` de bypass + pin TPK RSA-2048 (270 B) por `memcmp`. `System.exit(-6)` en firma requerida inválida = DoS. |
| Root oculto embebido | **Cubierto** `[CERT]` | **B395** | Los 3 vendor certs (Tridium incl.) firman contra un root DSA-1024 **oculto** en `baja.jar` (`masterPublicKeyData`); NO auto-firmados. Dual root DSA+ECDSA(v2). |
| Firma de la distribución `.dist` | **Cubierto** `[CERT]` | B393 | `SignedDistFilter` valida **solo** OS/NRE/VM. |

**Gap:** demostrar evasión (p. ej. efecto real de los flags `skip-*`, o un módulo con firma alterada) →
**requiere autorización + laboratorio**.

## 3. Entitlements / features / capacidades

**Acuerdo:** §3, §4 "entitlement"; §6.1 "entitlements".

| Aspecto | Estado | Bloque(s) | Nota |
|---|---|---|---|
| Gate nativo de feature | **Cubierto** `[CERT]` | **B126** §126.6 | `LicenseUtil::isFeaturePresent` = **coincidencia de TEXTO**, no verificación DSA. |
| Verificación DSA real (lado Java) | **Cubierto** `[CERT]` | B323, B395 | `LicenseManager` sí hace verificación criptográfica real (rechaza flip de 1 byte y cambio de payload). |
| Comportamiento sin licencia | **Cubierto** `[CERT]` | **B387** | Corre **UNCAPPED** (límites → `MAX_VALUE`), no deshabilitado; heap-limit → `exit(-3)`. |
| Qué cambia un license en disco | **Cubierto** `[CERT]` | B386 | Solo `security/`; el resto difiere por vendor/versión/usuario. |

**Gap:** demostrar la contradicción gate-texto vs. verificación-DSA como ruta de evasión → **requiere autorización**.

## 4. Expiración

**Acuerdo:** §3, §4 "expiración".

| Aspecto | Estado | Bloque(s) | Nota |
|---|---|---|---|
| Modelo de licencia / vencimiento | Cubierto | B126, B2 | Estructura de licencia y campos. |
| Root vendor "never-expires" | **Cubierto** `[CERT]` | B392 | Root Tridium 2003, params Sun por defecto, sin expiración. |
| **Enforcement de expiración en runtime** | **PARCIAL** `[INFER]` | — | El *efecto* en boot/runtime de una licencia vencida no está confirmado empíricamente. |

**Gap:** confirmar fail-closed/fail-open ante licencia expirada → **requiere autorización + station de prueba**.

## 5. Certificados y trust anchors

**Acuerdo:** §3, §4 "certificado"; §6.1 "certificados".

| Aspecto | Estado | Bloque(s) | Nota |
|---|---|---|---|
| Tres dominios de confianza | **Cubierto** `[CERT]` | **B392** | A módulos (RSA-2048 X.509, `truststore.jks` pass `changeit`) · B licencias/vendor (DSA-1024 XML `.certificate`) · C TLS/Authenticode (`cacerts.bcfks` BC-FKS). |
| Root DSA oculto | **Cubierto** `[CERT]` | B395 | Ver §2. |
| Stack de provider crypto | **Cubierto** `[CERT]` | **B440**, **B441** | Corre `bcfips`; BC en `provider.1/.2` **delante** de Sun vía override `==` de `bin/policy/java.security`; approved-only estricto NO activado. |
| Certs BACnet/SC + CRL | Modelado | B287 | `BIssuerCertAndCrl`; enforcement de revocación `[INFER]`. |
| Cert TLS por defecto (vivo) | Cubierto `[CERT-live]` | B156/158/162 | `ForRecoveryPurposes` default. |

**Gap:** enforcement real de CRL/revocación (BACnet/SC + TLS) → **requiere autorización + laboratorio** (= SP-G6).

## 6. Validación de integridad

**Acuerdo:** §3, §4 "validación de integridad"; §6.1 "controles de integridad".

| Aspecto | Estado | Bloque(s) | Nota |
|---|---|---|---|
| Asimetría firma código vs. datos | **Cubierto** `[CERT]` | **B393** | Niagara firma CÓDIGO+ENTREGA, **no** DATOS: backup `.dist`/audit/history/`.bog` **sin** firma/MAC/checksum; solo cifrado GCM per-campo. |
| Único canal de integridad opcional | **Cubierto** `[CERT]` | B396 | syslog offload (TLS transport, record **plano**, sin firma per-mensaje): resistencia, no evidencia. |
| Pin de integridad nativo | **Cubierto** `[CERT]` | B379 | TPK por `memcmp`. |
| Integridad de firmware (borde OT) | **Cubierto** `[CERT]` | B394 | 3 posturas: HMI ECDSA-firmado · PanelBus flash CRUDA sin firma · standalone AES-cifrado. |

**Gap:** demostrar manipulación de datos no firmados (audit/history/.bog) en laboratorio → **requiere autorización**.

---

## Anexo — Gaps abiertos que ya están tipificados en el corpus (todos requires-execution / requieren autorización)

Estos gaps del focus `signing-pki` **coinciden** con lo que el Acuerdo autorizaría a ejecutar. Ninguno es
investigable estáticamente; todos necesitan station/dispositivo de prueba **y** la firma auténtica:

| Gap corpus | Descripción | Mapea a área |
|---|---|---|
| SP-G3 (parcial) | Fail-closed en boot con licencia manipulada (falta `[CERT-live]`) | §3 entitlements + §4 expiración |
| SP-G6 | Enforcement CRL/revocación BACnet/SC + TLS | §5 certificados |
| SP-G8 | OTA PanelBus/HMI: ¿enforce cadena ECDSA o confía en la imagen? | §2 firma + §6 integridad |
| SP-G9a | `Security.getProviders()` en vivo (confirmar orden efectivo) | §5 certificados/provider |

**Conclusión operativa:** el trabajo de *comprensión* (estático, defensivo) está mayormente hecho y citado.
El trabajo pendiente es **exclusivamente la demostración dinámica**, que es precisamente el objeto autorizado
del Acuerdo (§6.1) y que **no se inicia hasta que exista la autorización firmada auténtica**.
