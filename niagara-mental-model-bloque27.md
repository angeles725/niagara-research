# Niagara N4 — Mental Model · Bloque 27

**Tema**: Network surface completa + catálogo puertos + certManagement end-to-end + trust chain pipeline + cert lifecycle + HeaderAuthentication + lockdown operacional

**Método**: Investigación empírica READ-ONLY — source decompilado (`javap -p -constants`), `defaults/system.properties` 589 líneas, `defaults/platform.bog` XML, modules `platCrypto-rt.jar` / `platform-rt.jar` / `web-rt.jar` / `fox-rt.jar` / `platDaemon-rt.jar` / `baja.jar` / `clientCertAuth-rt.jar` / `ldap-rt.jar` / `saml-rt.jar`, directorios reales en `OptimizerSupervisor-N4.14.0.162` (Install Home) + `/home/cristian/Niagara4.14/OptimizerSupervisor/` (User Home), contrastado con `niagara-help/devguide/architecture.html` y `niagara-help/guides/Provisioning/*Certificate*.html`.

**Conecta con**: Bloque 1 (framework), Bloque 3 (sandbox), Bloque 10 (platform/station), Bloque 11 (auth/session), Bloque 13.2 (Fox wire), Bloque 17 (filesystem forensics), Bloque 18 (signing + SCRAM + CSRF), Bloque 19.17 (BOX), Bloque 22.12 (BOX muxing), Bloque 23.10 (BACnet/SC), Bloque 26 (NRE + playbook signing).

---

Bloques previos tocaron certificados y puertos de refilón — cada uno a su ángulo. Bloque 17 enumeró directorios `security/` a nivel install y user. Bloque 18 profundizó en **signing** específicamente (cert Honeywell hardcoded `signing.properties`, `.sig` 256B raw RSA). Bloque 13.2 documentó el **wire protocol** Fox (frames, sensitive data). Bloque 11 cubrió los **9 auth schemes** conceptualmente. Bloque 23 documentó **BACnet/SC** TLS 1.3 sobre TCP 49152. Bloque 26 enlistó los ~138 binarios nativos.

Lo que **faltaba consolidar** — y es el foco de este bloque — es la superficie completa de red de una station Niagara N4.14 operacional: **qué puertos escucha, con qué ceremonia TLS, qué cert/trust store usa para cada uno, cómo se rota sin downtime, qué se firewall-eable, cómo endurecer para producción**. Documentación Honeywell/Tridium dispersa esto en 20+ HTML (`provisioningNiagara-PlatformConnection.html`, `GeneratingACertificate-D2BCD92F.html`, `architecture.html`). Acá lo unifico con empirismo de source decompilado.

El hallazgo más importante al entrar: **todos los puertos server-side en Niagara son `BServerPort` — un BComponent con 4 slots editables desde `!config` (publicServerPort / localServerPort / ipProtocol / adapter)**. Eso significa que **NO hay puerto hardcoded** en una station — hasta el Fox port es configurable runtime bog-edit. Y los defaults vienen de constants Java (`DEFAULT_SSL_PORT=5011`) ó del `.ntpl` de creación de station.

---

## 27.1 Catálogo unificado de puertos (TCP/UDP)

### 27.1.1 Puertos Niagara nativos — tabla maestra

| # | Puerto | Proto | TLS? | Componente escucha | Dónde se configura | Bog slot / constant | Clase Java | Ingress/Egress | Auth requerida |
|---|--------|-------|------|---------------------|--------------------|----------------------|-----------|----------------|----------------|
| 1 | **3011** | TCP | NO | `niagarad` (daemon C/C++) | `!daemon/platform.bog` + registry Windows | `BPlatform.localDaemonPort` | `com.tridium.platform.BPlatform` | in | SCRAM-SHA256 (platform credentials separadas, Bloque 10.1.2) |
| 2 | **5011** | TCP | TLS 1.2+ | `niagarad` | Install registry `HKLM\Software\Honeywell\Niagara\...` + `BPlatformSSLSettings.sslPort` | `DEFAULT_SSL_PORT = 5011` | `com.tridium.platform.BPlatformSSLSettings` | in | SCRAM + cert server (daemon cert) |
| 3 | **1911** | TCP | NO | station (Fox listener) | `NiagaraNetwork/FoxService/foxPort` bog | slot `BFoxService.foxPort` (tipo `BServerPort`) | `com.tridium.fox.sys.BFoxService` | in/out | Digest/SCRAM (BUser) |
| 4 | **4911** | TCP | TLS 1.2+ | station (FoxS listener) | `FoxService/foxsPort` bog | slot `BFoxService.foxsPort` | `com.tridium.fox.sys.BFoxService` | in/out | SCRAM + cert server+opcional client (mTLS) |
| 5 | **80** | TCP | NO | Jetty (WebService) | `WebService/httpPort` bog | slot `BWebService.httpPort` | `javax.baja.web.BWebService` | in | cookie session + CSRF |
| 6 | **443** | TCP | TLS 1.2+ | Jetty HTTPS | `WebService/httpsPort` bog | slot `BWebService.httpsPort` | `javax.baja.web.BWebService` | in | cookie session + CSRF + cert server |
| 7 | **47808** | UDP | NO (BACnet link) | BACnet IP link layer | `BacnetNetwork/LinkLayer/udpPort` bog | `BBacnetIpLinkLayer` | `com.tridium.bacnet.ip.*` (Bloque 23) | in/out | BACnet password (capa 7) |
| 8 | **49152** | TCP | TLS 1.3 | BACnet/SC hub | `BacnetScNetwork/hubPort` bog | `BBacnetScLinkLayer` | Bloque 23.10 | in | cert mTLS obligatorio |
| 9 | **224.0.1.84** | UDP mcast | NO | Fox multicast discovery | `FoxService/multicastEnabled` bog | slot `BFoxService.multicastEnabled` | `com.tridium.fox.session.MulticastServer` | in/out | none (discovery only) |
| 10 | **FF02::137** | UDPv6 mcast | NO | Fox multicast IPv6 | `niagara.ipv6Enabled=true` system.properties | — | `MulticastServer` | in/out | none |
| 11 | **n/a** | UDP | — | Platform daemon discovery | multicast local | — | `niagarad` C side | in | none |
| 12 | **25 / 465 / 587** | TCP | STARTTLS opc | SMTP client (EmailService) | `EmailService/smtpPort` bog | — | email-rt | out | SMTP AUTH |
| 13 | **161 / 162** | UDP | SNMPv3 opc | snmp-rt driver | `SnmpNetwork/port` bog | — | snmp-rt | in/out | SNMP community / v3 user |
| 14 | **502** | TCP | NO | Modbus/TCP driver | `ModbusTcpNetwork/port` bog | — | modbusTcp-rt | out | none (plaintext) |
| 15 | **4840 / 4843** | TCP | UASC opc | OPC UA client (opc.dll) | per-Device config | — | opcUa-rt | out | UA user+cert |
| 16 | **389 / 636** | TCP | TLS opc | LDAP auth | `AuthService/LdapScheme/url` bog | — | ldap-rt | out | simple bind / GSSAPI |
| 17 | **88** | TCP+UDP | Kerberos | Kerberos auth | `ldap-rt BKerberosAuthenticationScheme` | — | ldap-rt v3 | out | keytab |
| 18 | **n/a** | TCP | TLS | SAML IdP HTTP redirect/POST | `SAMLAuthenticationService/IdpUrl` bog | — | saml-rt | out (browser redirect) | SAML assertion signed |
| 19 | **9010 / 9011** | TCP | TLS opc | JMX (optional) | JVM flags `-Dcom.sun.management.jmxremote.port=9010` | nre.properties override | JDK `jdk.management.*` | in | JMX auth (passwd/ssl) |
| 20 | **1628** | UDP | NO | LON/IP tunneling (opc) | `LonNetwork/IpLinkLayer/port` | — | lonworks-rt | in/out | LON auth (layer 7) |
| 21 | **Serial (no IP)** | — | — | MSTP / RS-485 drivers | `platMstp-rt`, `platSerial-rt`, `platNrio-rt` | — | native DLL | local | none |
| 22 | **443 (outbound)** | TCP | TLS | nCloud licensing + Device Registration | `LicenseService` + `niagara.webbrowser.urlWhitelist` | system.properties | okhttp-4.12 client | out | mTLS cloud cert |

No hay un "puerto BOX propio". **BOX viaja sobre HTTP/S 80/443 multiplexado** — handshake `POST /box` → upgrade WebSocket `/wsbox` (ver Bloque 19.17 + 22.12). Mismo flag Cookie/CSRF que cualquier servlet Jetty.

### 27.1.2 Dónde viven los defaults — source empírica

**Fox 1911/4911** — verificado en `niagara-help/devguide/architecture.html`:

> Fox is a multiplexed peer to peer protocol which sits on top of a TCP connection. The default port for Fox connections is 1911. FoxS is the encrypted protocol that encapsulates all Fox communications in a secure transmission. The default port for FoxS is 4911.

**Platform 3011/5011** — verificado en `guides/Provisioning/provisioningNiagara-PlatformConnection.html`:

> number (defaults to 3011 for a connection that is not secure; defaults to 5011 for a secure connection)

**5011** también es constant Java: `com.tridium.platform.BPlatformSSLSettings.DEFAULT_SSL_PORT = 5011` (verificado via `javap -p -constants` `platform-rt.jar`).

**HTTP 80 / HTTPS 443** — son defaults Jetty. `javax.baja.web.BWebService` expone `httpPort` y `httpsPort` como `BServerPort` (no como `int`). El default se pone en el bog template (`defaults/workbench/newStations/*.ntpl`) al crear la station — NO es una constant Java. Por eso podés crear una station con HTTP 8080 directamente desde el wizard sin re-compilar nada.

### 27.1.3 `BServerPort` — anatomía del tipo

Toda definición de puerto server-side en Niagara N4 ES un `BServerPort` (`javax.baja.firewall.BServerPort`). NO es un `int` suelto. El tipo tiene 4 slots (verificado `javap -p baja.jar`):

```java
public class BServerPort extends BComponent {
    public static final Property publicServerPort;   // lo que ven clientes externos
    public static final Property localServerPort;    // lo que realmente escucha el socket
    public static final Property ipProtocol;         // TCP / UDP / TCP_IPv4 / TCP_IPv6
    public static final Property adapter;            // "en0" / "eth0" / "" = todas
}
```

Esto es **crítico**. `publicServerPort` ≠ `localServerPort` permite **port forwarding interno**: la JVM bindea socket en `localServerPort=8443`, pero tras un iptables/firewall rule el cliente llega por 443. Niagara lo sabe y emite respuestas coherentes con `publicServerPort` en redirects absolutos (Location headers, Fox HELLO, etc.). El `adapter` permite bind a interface específica — útil para stations multi-NIC (p.ej., WAN management + LAN BACnet).

Hay también `LOCALHOST_INTERFACE = "127.0.0.1"` hardcoded en la clase, usado cuando la UI Workbench bindea el platform listener del daemon local-only (no exposa 5011 a LAN).

El método privado `updateFirewallRules()` indica que **Niagara intenta gestionar reglas firewall del host OS** (Windows Defender / iptables) cuando cambian estos slots. No verifiqué todos los casos, pero en Supervisor Windows el servicio niagarad.exe tiene privilegios para tocar Windows Firewall vía netsh. En Linux requiere CAP_NET_ADMIN o sudo — si no lo tiene, las reglas se "intentan" silenciosamente y cliente externo NO puede conectar. Gotcha operacional importante.

---

## 27.2 Firewall matrix operacional

Tabla canónica para dimensionar reglas de red en deployment real.

| Origen | Destino | Puerto | Proto | Obligatorio | Rationale |
|--------|---------|--------|-------|-------------|-----------|
| Workbench (admin PC) | Supervisor | **5011** TCP | TLS | SÍ | Platform commissioning (instalar dist, módulos, licencias) |
| Workbench | Supervisor | **4911** TCP | TLS | SÍ | Fox station admin (Workbench view station) |
| Workbench | Supervisor | **443** TCP | TLS | recomendado | HTML5/UX — previsualización UI |
| Workbench | Subordinate JACE | **5011** TCP | TLS | SÍ | Platform commissioning JACE |
| Workbench | Subordinate JACE | **4911** TCP | TLS | SÍ | Fox admin JACE |
| Supervisor station | Subordinate station | **4911** TCP | TLS | SÍ | NiagaraNetwork federation — fox channel p2p |
| Supervisor station | Subordinate station | **5011** TCP | TLS | opcional | Provisioning jobs (BProvisioningCopyStep etc.) |
| Browser/User | Supervisor | **443** TCP | TLS | SÍ | HTML5 web UI + BOX WebSocket |
| Browser/User | Supervisor | **80** TCP | HTTP | NO (disable) | legacy. Redirigir a 443 via `httpsOnly=true` |
| Supervisor station | Subordinate (BACnet network) | **47808** UDP | NO (BACnet) | SÍ si integra BACnet/IP | BACnet whois/readProperty/COV |
| Supervisor station | Subordinate (BACnet BBMD) | **47808** UDP | NO | SÍ | Foreign Device Registration (FDR) a BBMD |
| Supervisor | BACnet/SC hub | **49152** TCP | TLS 1.3 | SÍ si BACnet/SC | BACnet Secure Connect cluster |
| Supervisor | nCloud.honeywellcloud.com / tridium.com | **443** TCP | TLS | SÍ si subscription license | Licensing check + Device Registration |
| Supervisor | LDAP/AD | **389 / 636** | TCP | SÍ si LDAP auth | Bind + search |
| Supervisor | Kerberos KDC | **88** | TCP+UDP | SÍ si Kerberos auth | AS-REQ / TGS-REQ |
| Supervisor | SMTP server | **25 / 465 / 587** | TCP | SÍ si EmailRecipient | Alarm dispatch |
| Supervisor | Modbus device | **502** | TCP | SÍ si Modbus/TCP | Register read/write (plaintext!) |
| Supervisor | OPC UA server | **4840 / 4843** | TCP | SÍ si OPC UA | Browse + subscribe |
| SNMP manager | Supervisor | **161** | UDP | opcional | Polling station MIB |
| Supervisor | SNMP trap receiver | **162** | UDP | opcional | Traps de alarmas |
| JMX console | Supervisor JVM | **9010 / 9011** | TCP | NO prod | Monitoreo JVM (heap, threads) — **desactivar en prod** |

**Regla de oro operacional**: para una Supervisor típica con N subordinados, abrir **exclusivamente 443 ingress desde browser + 4911 bidireccional hacia subordinados + 5011 bidireccional temporal durante commissioning**. Cerrar 1911 (Fox plain), 80 (HTTP plain), 3011 (platform plain), 9010/9011 (JMX). Los drivers específicos (BACnet/Modbus) se abren solo en la pierna privada del Supervisor hacia la red de control — nunca hacia Internet.

### 27.2.1 NAT + reverse proxy — gotcha SNI

Cuando una Supervisor vive tras reverse proxy (HAProxy, nginx, F5, etc.) que enruta por SNI, **requiere** `org.bouncycastle.jsse.client.assumeOriginalHostName=true` en `system.properties`. Sin eso, conexiones FOXS salientes desde la Supervisor **NO envían SNI extension** en el ClientHello, el reverse proxy no puede encaminar, y la conexión falla silent después del TCP handshake. Está documentado directamente en `defaults/system.properties`:

```properties
# This system property, when set to 'true', will configure Bouncy Castle JSSE to include server
# name parameters in the TLS handshake for outgoing (client) connections. This is required for FOXS
# connections that must traverse a reverse proxy that routes requests to hosts based on the SNI extension
# contained in the ClientHello message. If a target host is specified by IP address, and this property is
# set to 'true', a reverse DNS lookup may be used to obtain the hostname.
#org.bouncycastle.jsse.client.assumeOriginalHostName=false
```

Y advierte: "if the name service is not trustworthy, enabling reverse name lookup may be susceptible to man-in-the-middle attacks". Gotcha de defensa en profundidad: SNI buena contra SNI mala.

---

## 27.3 TLS boundaries + cipher suites

### 27.3.1 TLS versions soportadas

`javax.baja.security.crypto.BSslTlsEnum` (verificado `javap -p -constants baja.jar`):

```java
public static final int TLSV_1   = 0;   // TLS 1.0  — desaconsejado
public static final int TLSV_1_1 = 1;   // TLS 1.1  — desaconsejado
public static final int TLSV_1_2 = 2;   // TLS 1.2  — default práctico
public static final int TLSV_1_3 = 3;   // TLS 1.3  — soportado N4.13+
public static final BSslTlsEnum DEFAULT;  // inicializado en static{}
```

Cada servicio con TLS (WebService, FoxService, daemon SSL) tiene slot `*minProtocol` (p.ej. `httpsMinProtocol`, `foxsMinProtocol`) que acepta uno de esos 4 valores. "Min" = "aceptar este valor O superior". Setearlo a `TLSV_1_2` rechaza TLS 1.0/1.1 clients — dependerá del stakeholder qué controllers legacy quedan fuera.

BACnet/SC (Bloque 23.10) usa **TLS 1.3 obligatorio** por spec — no configurable.

### 27.3.2 Cipher suite groups

`javax.baja.security.crypto.BTlsCipherSuiteGroup`:

```java
public static final int RECOMMENDED = 0;  // subset curado por Tridium/BouncyCastle
public static final int SUPPORTED  = 1;   // todo lo que BC/JSSE implementa
```

El subset `RECOMMENDED` es deliberadamente minimalista (curadamente solo AEAD modes, ECDHE key exchange, AES-GCM / ChaCha20-Poly1305). `SUPPORTED` abre legacy — CBC modes, RSA key exchange puro — para compat con controllers JACE antiguos. La lista exacta se resuelve runtime via `TlsCipherSuiteGroup.getCipherSuiteGroup()` (NRE-level, `com.tridium.nre.security`) según FIPS mode y provider activo.

Override granular: `cipherSuite.exclude.patterns=` en `system.properties`:

```properties
# This system property allows the user to restrict which cipher suite lists can be used for TLS connections.
# Any cipher suite that contains one of the specified comma-separated value will be filtered out.
#cipherSuite.exclude.patterns=
```

**Uso típico hardening**: `cipherSuite.exclude.patterns=RSA_,CBC,3DES` para forzar PFS + AEAD. Filtro de substring (no regex) — cuidado con match accidental.

### 27.3.3 Extended Master Secret (RFC 7627)

`BPlatformSSLSettings.useExtendedMasterSecret` slot (String). Mitiga Triple Handshake attack. Sólo hace sentido en TLS 1.2 — en 1.3 es implícito. Default tiene constant `DEFAULT_USE_EXTENDED_MASTER_SECRET` pero no pude extraer el valor exacto por visibility.

### 27.3.4 Client-initiated renegotiation — DoS mitigation

Hardcoded enabled en `defaults/system.properties`:

```properties
jdk.tls.rejectClientInitiatedRenegotiation=true
```

Comentario en el archivo: "A well known DoS attack can be initiated by servers that support client initiated renegotiation". Esta es una configuración JDK-level (no Niagara-level) activada por default en la distro. NO tocar a menos que se necesite compat con hardware legacy que renegocia.

### 27.3.5 OCSP / CRL — revocation validation

No encontré evidencia empírica de que Niagara valide OCSP para certs server ni client por default. La clase `BCertManagerService` provee `TrustStore` y `KeyStore` pero **no** tiene método `enableOcsp()`. La validación de cert chain es path-build + signature check + validity dates — no revocation.

**Gotcha production**: si un cert está revocado por la CA interna vía CRL, Niagara lo seguirá aceptando hasta que expire naturalmente o lo remuevas manual de la trust store. Para entornos regulados (energy sector + NIST) esto no cumple. Mitigación: short validity periods (1 año) + rotación agresiva + script externo que limpia trust store.

### 27.3.6 Exclude root CA from chain — SHA1 legacy

```properties
#niagara.web.excludeRootCAFromCertChainForSigAlgs=SHA1WITHRSA
```

Cuando server cert chain tiene root CA con SHA1 signature (obsoleto), clients modernos rechazan el chain aunque ellos tengan ese root en su store. Niagara puede omitir el root del ServerHello — el cliente usa su propio root store para validar. Workaround para coexistencia chain moderno + trust store mixta.

---

## 27.4 `certManagement/` + `security/` directorios — layout y contenido real

Bloque 17 listó los directorios; este bloque entra al contenido concreto y al runtime que los lee.

### 27.4.1 Install Home — `security/`

Path: `/home/cristian/Honeywell/OptimizerSupervisor-N4.14.0.162/security/` (WSL, Windows `C:\Honeywell\OptimizerSupervisor-N4.14.0.162\security\`).

```
security/
├── certificates/
│   ├── Honeywell.certificate           (835 B)
│   ├── HoneywellCentraLine.certificate (845 B)
│   └── Tridium.certificate             (833 B)
├── licenses/
│   ├── Honeywell.license               (2.3 KB)
│   ├── HoneywellCentraLine.license     (366 B)
│   ├── Webs.license                    (16 KB — con skipModuleValidation feature)
│   ├── db/
│   │   ├── Qnx-TITAN-BB4C-D480-3C70-ACE4/    (per hostId JACE)
│   │   └── Win-6E6E-10AC-D1DD-8276/          (per hostId Supervisor)
│   └── inbox/
└── truststore.jks                      (958 B — JKS legacy)
```

Los archivos `.certificate` NO son PEM estándar. Son **XML custom**:

```xml
<?xml version="1.0"?>
<certificate version="1.0" vendor="Tridium" generated="2003-07-16" expiration="never">
 <publicKey algorthm="DSA">MIIBtzCCASwGByqGSM44BAEwggEfAoGBAP1/U4EddRIpUt9KnC7s...</publicKey>
 <signature>MC0CFQCWeuny190mtvpsHDo9UIJoLlEBPAIUVm/vjfqReAvrHUwoXIH//ik9dcE=</signature>
</certificate>
```

Tridium cert tiene `expiration="never"` (año 9999 equivalente, ver Bloque 18.1.5). Confirma el patrón — los 3 vendors Tridium/Honeywell/HoneywellCentraLine tienen certs eternos hardcoded en el install. Son certs **license-issuing** (autoridad que firma `.license` XMLs), no son los mismos que firman módulos (ese es otro Tridium cert hardcoded en `signing.properties`).

**Algoritmo DSA** (Digital Signature Algorithm) — legacy. DSA es más débil que RSA 2048 moderno. Probablemente histórico desde 2003 — el certificado tiene fecha `generated="2003-07-16"`. Niagara nunca rotó. Confirma el patrón de "ancla de confianza inmutable" — Honeywell asume que la infra Tridium permanece intacta a perpetuidad.

`truststore.jks` JKS — formato legacy Java. Contiene trust anchors que el daemon C++ lee para validar firmas `.license` y `.certificate`. 958 bytes es tamaño mínimo — pocos aliases, probablemente sólo 1-3 root certs.

### 27.4.2 User Home — `security/` + `certManagement/`

Paths (verificados empíricos WSL mount):

```
/home/cristian/Niagara4.14/OptimizerSupervisor/
├── security/
│   ├── .km                        (262 B — metadata key ring?)
│   ├── .kr                        (1.6 KB — key ring encrypted)
│   ├── cacerts.jceks              (3.2 KB — JCEKS encrypted)
│   ├── exemptions.tes             (16.5 KB — permission exemptions, Bloque 18.9)
│   ├── keystore.jceks             (14.3 KB — JCEKS encrypted)
│   ├── signing/
│   │   └── signers/               (cache de signers aceptados)
│   └── untrusted.jceks            (32 B — empty/initial)
└── certManagement/
    ├── angelesca.pem              (5.6 KB — Root CA self-signed)
    ├── default.pem                (1.3 KB — "Niagara4" self-signed server cert)
    ├── devmodulesigning.pem       (3.3 KB — SEJOFA code-signing cert)
    ├── sejofa_codesigningcs.csr   (2 KB — CSR pendiente)
    └── sejofa_codesigningcs.pem   (4.8 KB — cert CA-signed)
```

**`certManagement/`** — **zona de trabajo del operador humano**. Es donde Workbench guarda exportaciones PEM de certificados para compartir con otros systems, los CSRs que genera, y las imported CAs. No tiene formato rígido — cada `.pem` es independiente. El usuario opera con esto manualmente via `Platform > Certificate Management` o vía filesystem directo.

Inspección empírica de 3 certs reales del usuario (output `openssl x509 -noout -subject -issuer -dates`):

```
default.pem:
  subject=CN=Niagara4, O=ForRecoveryPurposes, C=US
  issuer= CN=Niagara4, O=ForRecoveryPurposes, C=US    (self-signed)
  notBefore=Sep 17 18:24:46 2025
  notAfter= Sep 17 18:24:46 2026                     (1 año — típico)

devmodulesigning.pem (SEJOFA):
  subject=CN=Security Audit Dev, O=SEJOFA, C=MX
  issuer= CN=Security Audit Dev, O=SEJOFA, C=MX       (self-signed)
  notBefore=Jan 15 23:05:25 2026
  notAfter= Jan 15 23:05:00 2046                      (20 años — signing)
  SHA1 Fingerprint=FC:96:08:54:60:13:4B:22:92:74:48:52:44:DD:0C:70:FA:B2:43:95

angelesca.pem (User's CA):
  subject=CN=angelesCA, OU=Automatizacion, O=ANGELES, L=Chihuahua, ST=Chihuahua, C=MX
  issuer= CN=angelesCA, ...                           (self-signed root)
  notBefore=Apr 19 20:24:09 2026
  notAfter= Apr 19 20:24:00 2090                      (64 años — root CA)
```

Patrón que se repite: **server certs 1 año, signing certs 20 años, root CAs 60+ años**. Self-signed porque dev/pre-prod.

**`security/`** User Home — **zona runtime protegida**. Es donde el runtime station/workbench lee y guarda credenciales activamente. Los 3 `.jceks` son **JCEKS (Java Cryptography Extension Key Store)** — formato Java encriptado con clave derivada de master password. Validación: `file` devuelve `Java JCE KeyStore`. Intento `keytool -storepass ''` da error "tampered or wrong password" — confirmación de que están protegidos.

| Archivo | Rol | Formato | Typical content |
|---------|-----|---------|-----------------|
| `keystore.jceks` | **User Key Store** — private keys + certs del usuario (client auth, SSL server) | JCEKS | aliases "tridium" (server default), "fox", custom user aliases |
| `cacerts.jceks` | **User Trust Store** — CA roots + self-signed peer certs confiables | JCEKS | Honeywell root, peer Supervisor cert, corporate CA |
| `untrusted.jceks` | **User Untrusted Store** — quarantine para certs rechazados pero conocidos | JCEKS | certs que el usuario clickeó "Reject" en Platform UI |
| `exemptions.tes` | **Permission exemptions binary** (Bloque 18.9) | Tridium binary | bypass granular user-level |
| `.km` + `.kr` | **Master key + key ring** — clave derivada del user password | encrypted | protege los `.jceks` |
| `signing/signers/` | Cache de certificados de signers de módulos ya aceptados | various | empty en pristine install |

El cert Tridium `Tridium.certificate` del install (XML custom) NO vive aquí — vive al nivel install. Pero su equivalente X.509 lo carga el `CertManagerService` vía conversión binaria en `BPKCS7CertificateHandler` / `BPEMCertificateHandler` (clases en `javax/baja/platCrypto/certs/`).

### 27.4.3 Daemon Home — `security/`

Daemon Home nativo Windows: `C:\ProgramData\Honeywell\Niagara4.14\OptimizerSupervisor\security\`. No verificado empírico WSL (no montado en el sandbox actual), pero la doc + código en `BCertManagerService` confirma que **daemon tiene sus propios key/trust stores separados** del user:

```java
// com.tridium.platcrypto.core.BCertManagerService
private NTrustStore userTrustStore;
private NTrustStore userUntrustedStore;
private NTrustStore systemTrustStore;   // ← el Daemon/System Trust Store
private NKeyStore   keyStore;           // ← key store unificado (user-level API)
private NExemptionStore exemptionStore;
```

**3 trust stores, 1 key store**. El `systemTrustStore` es el del daemon; es el que valida certs de clientes que se conectan a 5011. Cuando Workbench commisiona un JACE, el daemon del JACE valida el cert del Workbench contra su systemTrustStore. El `userTrustStore` es el del station JVM — valida peers Fox, SMTP servers, LDAP servers, etc. Son diferentes scopes.

El `userUntrustedStore` es único en la industria: certs que explícitamente rechazaste pero querés mantener memoria de que existen (no reaprobar accidentalmente). Interesante decisión diseño Tridium.

### 27.4.4 Runtime lookup order — dónde lee cada servicio

Flow empírico decompilado de `BCertManagerService.getClientSocketFactory()` / `getServerSocketFactory()`:

**Server cert** (cuando Jetty/Fox server acepta conexión inbound):
1. Slot `BWebService.httpsCert` (ó `BFoxService.foxsCert`) — alias string
2. Lookup en `keyStore` (User Key Store)
3. Si alias no existe → SSL handshake falla (alert unknown_key)
4. Private key decrypted con `keyPassphrase` slot (ó "" si `tridium` default)

**Client cert validation** (cuando station se conecta outbound o acepta mTLS):
1. Recibe peer cert chain
2. `systemTrustStore` — buscar trust anchor (para BACnet/SC, platform calls)
3. Si no hit → `userTrustStore` (fallback)
4. Si no hit en ninguno → `userUntrustedStore` (si está, rechazar explícito; si no, UI prompt)
5. Si operador clickea "Accept permanent" → agrega a `userTrustStore`
6. Si "Reject permanent" → agrega a `userUntrustedStore`

**Importante**: el orden es `system FIRST, user SECOND`. Esto significa que **si un cert está en system trust store explícitamente marcado como distrustable**, el user trust store no lo puede overridar. Para certs del daemon (5011 client authentication) cualquier cambio requiere tocar ProgramData — que normalmente solo admin puede.

### 27.4.5 `sw/` versioned signer cache

Al nivel Install Home hay `sw/` con subdirs numéricos (`0.0.1`, `0.1.1`, `1.0`, ...). Es cache de versiones pasadas de módulos/binarios usado durante migraciones (Bloque 25). Relevante acá porque los JARs cachados retienen sus `.sig` firmas — si hacés rollback a una versión anterior, el signer cert que la firmó debe seguir en trust store o validación falla.

Gotcha: **rotar el signing cert sin retener el anterior en trust store impide rollbacks**. Operacional: nunca borrar cert antiguo, siempre agregar el nuevo junto al viejo.

---

## 27.5 Trust chain pipeline end-to-end

### 27.5.1 Los 3 niveles de trust + 1 de exemption

| Nivel | Clase runtime | Path típico | Audiencia | Quién escribe |
|-------|---------------|--------------|-----------|---------------|
| **System Trust Store** | `BCertManagerService.systemTrustStore` → `BPlatTrustStore` | Daemon Home `security/` | niagarad (5011), platform-level TLS | platform admin (5011 credentials) |
| **User Trust Store** | `BCertManagerService.userTrustStore` → JCEKS en User Home | `cacerts.jceks` | Station JVM (4911, 443, SMTP/LDAP out) | station admin (BUser with write perms) |
| **User Untrusted Store** | `userUntrustedStore` | `untrusted.jceks` | Station JVM blacklist | mismo que User Trust Store |
| **Exemption Store** | `exemptionStore` → `NExemptionStore` | `exemptions.tes` | permission granular bypass (Bloque 18.9) | operator privileged |

La distinción **daemon vs station** es clave y se mantiene estricta. **Un cert importado vía Workbench UI "Platform > Certificate Management" para el daemon (5011) NO queda visible para la station (4911/443)**. Son 2 import operations separadas. Workbench UI pone tabs separadas ("User Key Store", "System Key Store", "User Trust Store", "System Trust Store") — cada tab habla con un backend distinto.

### 27.5.2 API de conversión — formato handlers

`javax/baja/platCrypto/certs/`:
- `BICertificateFormatHandler` — interface base
- `BPEMCertificateHandler` — `.pem` PEM base64 (RFC 7468)
- `BPKCS7CertificateHandler` — `.p7b` `.p7c` PKCS7 DER/PEM

No encontré `BPKCS12CertificateHandler` explícito — PKCS12 (`.p12`/`.pfx`) se maneja probablemente via keytool directamente (Java built-in). El import-export UI Workbench acepta los 3 formatos comunes: PEM, PKCS7 chain, PKCS12 para private+cert bundle.

Conversión de los XML `<certificate>` custom Honeywell a X.509 DER sucede en carga — no hay handler dedicado visible, probablemente está en `com.tridium.crypto.*` nivel NRE (no examiné).

### 27.5.3 Validation chain walk — pseudocode

Reconstrucción del flow `BCertManagerService.getClientSocketFactory()` (no extraí el source completo pero las dependencias definen la lógica):

```java
// Al aceptar cert chain remoto en handshake TLS:
X509Certificate[] chain = peer.getPeerCertificates();

// 1. Path build: ordenar chain (peer → intermediate → root)
CertPath path = CertificateFactory.getInstance("X.509")
    .generateCertPath(Arrays.asList(chain));

// 2. Signature validation cada link
for (int i = 0; i < chain.length - 1; i++) {
    chain[i].verify(chain[i+1].getPublicKey());
}

// 3. Validity dates
for (X509Certificate c : chain) c.checkValidity();

// 4. Trust anchor lookup
X509Certificate root = chain[chain.length - 1];
boolean inSystem = systemTrustStore.containsAlias(root);
boolean inUser   = !inSystem && userTrustStore.containsAlias(root);
boolean inUntrusted = userUntrustedStore.containsAlias(root);

if (inUntrusted) throw new CertificateException("Cert explicitly rejected");
if (!inSystem && !inUser) {
    // Prompt interactive user en Workbench
    // En station headless: reject default (conservador)
}

// 5. NO OCSP, NO CRL verification (empírico — no evidence en source)
```

La falta de OCSP lo confirmo por ausencia de `sun.security.provider.certpath.OCSP` o Bouncy Castle `org.bouncycastle.cert.ocsp` usages en `platCrypto-rt.jar`. Si alguien tiene evidence contraria, verificar.

### 27.5.4 Self-signed vs CA-signed — decisión

En Niagara field typical:
- **Dev + pre-prod** → self-signed. Cada station genera su `tridium` alias self-signed al primer boot. `Generate Self-Signed` from Workbench UI. `default.pem` del User del ejemplo es un self-signed con 1 año.
- **Producción single-site** → self-signed + import a Workbench trust store manual. Cada PC admin hace "Accept permanent" una vez.
- **Producción multi-site federated** → interna CA. `angelesca.pem` del ejemplo es un root CA interno del operador, con 64 años validity, firmando cada subordinate cert. Import root CA una vez, chain validation hace el resto.
- **Producción con compliance (NIST/IEC 62443)** → CA pública (DigiCert, Sectigo) + OCSP stapling en reverse proxy (no en Niagara) + short validity 90 días + automation rotación.

Niagara NO nace con automation para Let's Encrypt / ACME. Hay que implementar externo (script que rota `default.pem`, keytool import, reset station).

### 27.5.5 Cert command-line tools disponibles

`bin/nverify.exe` (517 KB) — verify signatures de módulos, `.dist`, certs (Bloque 17.1.1). No es keytool general.

Java `keytool` está embebido en `jre/bin/keytool.exe`. Funciona pero **NO sabe de JCEKS ni del formato cert XML Honeywell**. Para workflows Niagara-native siempre preferir Workbench UI o API `BCertManagerService`.

No hay `certManager.exe` dedicado. Todo el certManagement es GUI (Workbench) o programático (RPC).

---

## 27.6 Matriz comparativa — los 7 tipos de certificado en Niagara

Crítico distinguir — NO son intercambiables, cada uno tiene su slot, su keystore, su validity típica.

| Tipo | Propósito | Keystore | Clase/Slot | Validez típica | Rotación | Impacto si expira | Formato |
|------|-----------|----------|------------|----------------|----------|-------------------|---------|
| **Signing cert Honeywell** | Firma de módulos oficiales | hardcoded en `signing.properties` | `security/certificates/Tridium.certificate` XML | year 9999 (eternal) | N/A | N/A | XML custom DSA |
| **Signing cert user** | Firmar módulos propios (Bloque 18 + 26) | User Key Store | alias custom | 20 años típico | Re-firmar todos los módulos | módulos no cargan en MEDIUM/HIGH | X.509 + private key |
| **HTTPS server cert (Jetty 443)** | Autenticación server HTTPS | User Key Store | `BWebService.httpsCert` alias | 1-3 años | Generate new + setHttpsCert + restart Jetty | web UI down, browser warning | X.509 |
| **FoxS server cert (4911)** | Auth server FoxS TLS | User Key Store | `BFoxService.foxsCert` alias | 1-3 años | similar HTTPS + station restart fox server | federation + Workbench can't connect | X.509 |
| **Daemon cert (5011)** | Auth server platform SSL | System Key Store (daemon) | `BPlatformSSLSettings.keyAlias` | 1-3 años | Platform UI + niagarad restart | Workbench can't commission | X.509 |
| **Client cert (mTLS / clientCertAuth)** | Auth mTLS para cliente | User Key Store cliente + trust server | `BClientCertAuthScheme` config | 1-2 años | Reissue + re-enrollment per user | ese user no puede login cert-based | X.509 + private key |
| **SAML IdP signing cert** | Verificar aserciones SAML entrantes | User Trust Store | `SAMLAuthenticationService.idpCert` | IdP-controlled | copy+paste from IdP | SSO roto, usuarios no pueden login SAML | X.509 |
| **BACnet/SC operational cert** | mTLS BACnet/SC cluster | User Key Store cliente + Trust hub/node | BACnet device config | 1-5 años | Re-enrollment in BACnet Device Management | BACnet/SC device offline | X.509 |
| **NiagaraNetwork federation cert** | Supervisor↔Subordinate (FoxS a scale) | ambos lados User Key Store + cross-trust | FoxS slot reutilizado | igual FoxS | propagate via Provisioning job | subordinates pierden conexión Supervisor | X.509 |

**Puntos críticos**:

1. **HTTPS cert ≠ FoxS cert obligatoriamente**. Puedes usar uno distinto por puerto — `httpsCert` y `foxsCert` son slots independientes. En práctica se usa el mismo (1 cert para station, cubre 443 y 4911).

2. **Daemon cert vive en otro key store**. Rotar 5011 cert NO rota 4911/443. Es error operacional frecuente — "rotamos el cert" pero solo tocaron el de station.

3. **Signing cert Honeywell es IMMORTAL (year 9999)**. Signing cert user es el que sí expira — y cuando expira, todos los módulos firmados con él dejan de cargar en modo `medium`/`high`. Mitigación: rotar con anticipación, re-firmar módulos, deployar actualizados antes de que expire el viejo.

4. **Client cert mTLS es per-user**. Cada BUser tiene su propio cert emitido. Rotar implica N-operaciones. Si tenés 500 users clientCertAuth, necesitás automation — no hay API bulk-rotation nativa.

---

## 27.7 Cert lifecycle flows

### 27.7.1 Creación — 3 vías

**Vía 1: Workbench UI (más común)**
`Platform Connection > Certificate Management > New`
→ dialog con DN fields (CN, O, L, ST, C, email)
→ key algorithm (RSA 2048 / 4096 / EC P-256/P-384)
→ validity days
→ alias
→ gen KeyPair + self-signed X.509 → salva en User Key Store JCEKS

Internamente llama a `BCertificateManagementRpc` (`com.tridium.platcrypto.web.BCertificateManagementRpc`, 50 KB de source — es la clase gorda). Esta RPC talk con el daemon via `BPlatKeyStore.setKeyEntry()`.

**Vía 2: keytool directo (menos oficial)**
```bash
keytool -genkeypair -alias myalias \
    -keyalg RSA -keysize 2048 \
    -dname "CN=mystation, O=SEJOFA, C=MX" \
    -validity 730 \
    -keystore keystore.jceks -storetype JCEKS \
    -storepass "mypass"
```
Funciona pero bypasea UI Niagara — hay que cerrar Workbench antes o el Workbench tiene el archivo lockeado. Operacionalmente frágil.

**Vía 3: API Java (automation)**
`BCertManagerService.getKeyStore().setKeyEntry(alias, privateKey, password, chain)`
`BCertManagerService` es BService, accesible via ord `local:|station:|slot:/Services/PlatformServices/CertManagerService`. Programable desde BProgramObject o external client via FOX RPC.

### 27.7.2 CSR + CA signing flow

Cuando necesitás que una CA externa firme tu cert:

1. **Generate keypair local** (vía cualquiera de las 3 vías arriba)
2. **Export CSR**: Workbench "Generate CSR" → dumps PEM PKCS10 a `certManagement/foo.csr`. Ejemplo real user: `sejofa_codesigningcs.csr` (2 KB).
3. **Envíar CSR a CA** (DigiCert web form, corporate internal CA, HashiCorp Vault PKI, etc.)
4. **Recibir cert firmado** — PEM o DER
5. **Import back**: Workbench "Import Signed Certificate" → lee `sejofa_codesigningcs.pem` → aplica a alias existente → ahora el alias tiene private key (ya existía) + cert firmado CA (nuevo)
6. **Deploy chain**: importar root CA de la CA en User Trust Store, para que clientes que hagan TLS lo validen

Clase relevante: `com.tridium.platcrypto.signing.GenerateCertificateAndSubmitCsr` (9 KB) + `CheckOnboardingApproval` + `GetSigningResult`. Todo el flow on-boarding está codificado — probablemente soporta integración con Signing Service centralizado Honeywell (para Customer managed PKI scale).

### 27.7.3 Export / import formats

| Formato | Extensión | Contiene | Uso |
|---------|-----------|----------|-----|
| PEM | `.pem` `.crt` `.cer` | 1 cert base64 | import/export single cert |
| PKCS7 | `.p7b` `.p7c` | chain de certs | distribute CA root + intermediates |
| PKCS12 | `.p12` `.pfx` | cert + private key encrypted | backup de identity, deploy to new host |
| JCEKS | `.jceks` | keystore encriptado Java | keystore nativo Niagara (no portable fuera de Java) |
| CSR PKCS10 | `.csr` | pub key + DN firmado | submit a CA externa |

Niagara UI expone todos. PKCS12 es el único que viaja con private key — tratarlo como secret (no commit en git).

### 27.7.4 Rotación sin downtime — pattern cross-sign

Naive rotation = rotación con downtime: stop station → replace cert → start station. Clientes se desconectan durante el gap.

Pattern "cross-sign" (limitado soporte nativo):
1. Generar cert NEW con misma key material o nueva
2. Agregar cert NEW a User Trust Store de TODOS los clientes ANTES de rotar server
3. Cuando todos los clients tienen NEW en trust store, reemplazar server cert
4. Clientes establecen nuevas conexiones con NEW; conexiones viejas drenan
5. Remove OLD de trust stores después

En Niagara NO hay "dual cert" nativo (server con 2 certs simultáneos). Hay que secuenciar. En FoxS, la sesión Fox 24h persistente (Bloque 13.2.3) **NO se reconnecta automático cuando cert cambia mid-session** — se mantiene con cert viejo hasta que expira el session timeout o se rompe el TCP. Gotcha: durante rotación, sesiones viejas pueden quedar con cert ya desaparecido del key store server — nuevos handshakes fallan mientras los viejos siguen OK hasta su timeout natural. Impredecible.

Mitigación agresiva: `BFoxService.setFoxsEnabled(false)` + `setFoxsEnabled(true)` después del cert swap — fuerza disconnect todo + reconnect con cert nuevo. Downtime ~segundos en federation.

### 27.7.5 FIPS mode — workflow alterado

`BPlatformSSLSettings.fipsMode = true` cambia radicalmente el runtime:

1. Provider JCE default se reemplaza por Bouncy Castle FIPS (`bc-fips-1.0.2.5.jar` en `bin/ext/bcfips/`, ver Bloque 17.5.7)
2. **Keystore format obligatorio pasa a BCFKS** (Bouncy Castle FIPS Key Store). Los JCEKS existentes NO son válidos. Hay que migrar:
   ```bash
   keytool -importkeystore -srckeystore old.jceks -srcstoretype JCEKS \
           -destkeystore new.bcfks -deststoretype BCFKS \
           -providername BCFIPS
   ```
3. Cipher suites restringen a FIPS-approved — SHA1/RC4/MD5/DES eliminadas forcibly
4. Key sizes mínimos — RSA 2048+, EC P-256+, AES 128+
5. Algorithms no-approved (ChaCha20-Poly1305 no está en FIPS 140-2 legacy) son rejected

**Gotcha**: activar FIPS **sin haber migrado keystores a BCFKS hace que la station no bootee**. El daemon arranca, spawnea JVM, JVM intenta cargar `keystore.jceks` con provider FIPS que NO entiende JCEKS, excepción, station fault. Mitigación: migrar primero keystores, después flip `fipsMode`, después restart.

Detalle adicional: **Bloque 3.10 vs FIPS mode** — `skipModuleValidation` funciona también en FIPS, pero el cert chain del signing cert tiene que ser SHA256-only (SHA1 módulos viejos rechazados). Al activar FIPS en un site con módulos legacy hay que re-firmarlos todos antes.

### 27.7.6 `niagara.signingRequester.*` — Signing Service centralizado

En `defaults/system.properties`:

```properties
# retry delay between onboarding approval checks (in millis)
#niagara.signingRequester.approvalCheckRetryDelay=15000
# defaults to 6hours of attempts every 15seconds
#niagara.signingRequester.approvalCheckMaxAttempts=1440
# retry delay between get signing results checks (in millis)
#niagara.signingRequester.signingResultsCheckRetryDelay=1000
# defaults to 30minutes of attempts every 1second
#niagara.signingRequester.signingResultsCheckMaxAttempts=1800
```

Esto indica que hay un **Signing Service centralized** — sites con PKI managed Honeywell central — donde una station "requester" sube su CSR y espera **6 HORAS** para approval humano, después polling hasta 30 minutos para receiver el cert firmado. Uso: deployment de fleet de 1000+ JACEs donde una CA central emite certs per-device tras approval operacional.

El `BAbstractSigningRequester` (45 KB, `platCrypto-rt.jar`) es la super-clase de task scheduled que maneja este async flow. Feature importante para scale enterprise — no existe en small deployments.

---

## 27.8 HeaderAuthentication + Cookie + Session auth deep

Bloque 11 cubrió los **9 auth schemes** conceptualmente (Digest, SCRAM, SAML, Kerberos, Cert, LDAP, Google TOTP, HTTP Basic, AX Digest). Este bloque profundiza en **cookies, headers, CSRF integration** — la plomería real HTTP que mantiene la sesión.

### 27.8.1 Cookies emitidas por WebService

Clase `com.tridium.web.CookieUtil` (verificado `javap -p web-rt.jar`):

```java
public static final String CNAME_USERID;              // "niagara_userid"
public static final String CNAME_ENCRYPTED_USERID;    // userid encrypted variant
public static final String CNAME_AUTH_SCHEME;         // "niagara_auth_scheme"
public static final String CNAME_SESSIONID;           // "niagara_session"
public static final String CNAME_SUPER_SESSION_ID;    // session identity supra
public static final String CNAME_SSO_SCHEME;          // which SSO handler
public static final String CNAME_CURRENT_SSO_SCHEME;
public static final String CNAME_CURRENT_FORM_ID;     // form login token
public static final String CNAME_CURRENT_SCHEME_ID;
public static final String CNAME_ORIGIN_URI;          // redirect target tras login
public static final String CNAME_FAILURE_CAUSE;       // human readable fail
public static final String CNAME_FAILURE_INFO;
public static final int COOKIE_AGE;
```

**10 cookies distintas**. No son todas emitidas siempre — depende del auth scheme activo y del estado (pre-login, post-login, SSO redirect). Los principales:

- `niagara_session` — session ID, lifetime tied to `BWebService.sessionTimeout`
- `niagara_userid` — username actual (NO contiene password)
- `niagara_auth_scheme` — scheme que autenticó (Digest / SAML / etc.)
- `niagara_origin_uri` — URL pre-redirect al hacer login en forma externa

Métodos clave:
```java
public static Cookie createCookie(String name, String value, int maxAge, boolean secure);
public static Cookie createEncryptedCookie(String name, String value, int maxAge);
public static String  getDecryptedCookieValue(Cookie c);
```

`createEncryptedCookie` usa AES con key derivada — `getEncryptionKey()` es private, la key vive solo en memoria de la station. Si matás la JVM se invalidan todas las sessions (no persisten a disco). Esto es feature: rebooted station = forced re-login todos.

Métodos `encodeCookie()` hace URL encoding RFC 6265 para valores con caracteres especiales.

### 27.8.2 SameSite — `BSameSiteEnum`

`com.tridium.web.BSameSiteEnum`:

```java
public static final int NONE   = 0;  // cross-site allowed (requires secure)
public static final int LAX    = 1;  // default moderno — same-site except top-level nav
public static final int STRICT = 2;  // same-site solamente
```

Bloque 9.3.5 ya mencionó el gotcha "sameSite=None requires secure=true". Confirmado en spec RFC 6265bis — browsers modernos (Chrome 80+, Firefox 96+) rechazan silently cookies `SameSite=None` sin `Secure` flag.

**Gotcha expandido**: una station con HTTP (80) + `sameSite=NONE` pierde cookies en todo request cross-site. UI HTML5 ux hosted en iframe de otro dominio (dashboard corporate) no recibe cookie niagara_session, cada request es "anonymous" → redirect login loop. Debugging: F12 browser > Application > Cookies > ver si `niagara_session` está. Solución: deployar con HTTPS (443) + cert válido + `sameSite=NONE; Secure`. O cambiar a `sameSite=LAX` y mover dashboard al mismo dominio.

### 27.8.3 Session fixation protection

No encontré clase explícita `SessionFixationProtection` pero el patrón usado es:

1. Al login exitoso → `LoginServlet` genera **session ID nueva** (discard la pre-login del `niagara_session` cookie anónima)
2. Invalida antigua session en `SessionManager` interno
3. Set-Cookie nueva con `SameSite` + `Secure` + `HttpOnly`

Mitiga fixation attack. Confirmación indirecta: `com.tridium.web.servlets.LoginServlet` (6 KB) referencia `PreloginServlet` (2.8 KB) — split entre "usuario anónimo browsing público" y "usuario autenticado post-login", cada uno con su session distinta.

### 27.8.4 CSRF token vs session token relation

Bloque 18.5 cubrió CSRF. Adición acá:

- Session token = `niagara_session` cookie (`HttpOnly` — JS no ve)
- CSRF token = header `x-niagara-csrfToken` (JS SÍ ve — lo manda en cada POST/PUT/DELETE)
- **Session token vida > CSRF token vida** — CSRF rota cada request (nuevo token en response header), session dura 15min idle / 8h absolute.
- Invalidar CSRF no mata session. Rotación transparente para JS (BajaScript reemplaza el token stored).

`CsrfProtectedFilter` intercepta todas las mutation requests. Si falta el header o no matchea el token esperado para esa session, responde **403 Forbidden + WWW-Authenticate: custom**. La request NO llega al servlet handler.

### 27.8.5 `BHTTPBasicAuthenticationScheme` + legacy

En `com.tridium.authn`:
- `BHTTPBasicAuthenticationScheme` — HTTP Basic moderno
- `BLegacyBasicAuthenticationScheme` — compat
- `BDigestAuthenticationScheme` — Digest moderno (scheme name: "digest")
- `BLegacyDigestAuthenticationScheme` — compat
- `BSessionIdAuthenticationScheme` — internal, para reuse de session entre requests HTTP

El "legacy" existe porque versiones AX → N4 migration mantienen clientes viejos con protocolo pre-SCRAM. Un Supervisor N4.14 acepta ambos por default (downgrade attack risk). Hardening prod: disable schemes legacy explicitly desde `AuthenticationService`.

### 27.8.6 Dónde NO hay BHeaderAuthenticationScheme

**Confirmado empírico**: NO existe `BHeaderAuthenticationScheme` en `baja.jar` ni en `web-rt.jar` ni en ldap/saml/clientCertAuth. Niagara NO soporta nativamente "SSO via pre-authenticated header" tipo Apache `REMOTE_USER` o `X-Forwarded-User` injection desde reverse proxy.

Workaround que se usa en deployments real:
1. Reverse proxy (nginx/apache mod_auth_*) hace SAML/OAuth externa
2. Envía header `X-Forwarded-User: alice@sejofa.mx`
3. Niagara **NO reconoce el header** — ningún scheme lo mapea a BUser
4. Operador implementa custom `BAuthenticationScheme` en módulo propio que hereda de `BAuthenticationScheme` y lee el header

Existen implementaciones 3rd party pero no viene out-of-the-box. Es un gap documented. Opción alternativa: usar SAML-rt directo desde browser hacia IdP, sin intermediario, si el IdP soporta SP-initiated flow.

### 27.8.7 Tabla de schemes + cookies emitidas

| Scheme | Class | Cookies set | Header required |
|--------|-------|-------------|------------------|
| Digest | `BDigestAuthenticationScheme` | `niagara_session`, `niagara_userid`, `niagara_auth_scheme=digest` | `Authorization: Digest ...` en challenge |
| HTTP Basic | `BHTTPBasicAuthenticationScheme` | similares | `Authorization: Basic base64(user:pass)` |
| Session reuse | `BSessionIdAuthenticationScheme` | (reads `niagara_session`) | — |
| Client Cert | `BClientCertAuthScheme` (clientCertAuth-rt) | session + `niagara_auth_scheme=cert` | TLS handshake mTLS |
| LDAP | `BLdapAuthenticationScheme` (ldap-rt) | similares Digest | — |
| Kerberos | `BKerberosAuthenticationScheme` (ldap-rt v3) | session + scheme=kerberos | `Authorization: Negotiate ...` SPNEGO |
| SAML | `BSAMLAuthenticationScheme` (saml-rt) | session + sso_scheme=saml + origin_uri | SAMLRequest/Response POST |
| Google TOTP | Google 2FA scheme (Bloque 11.2.8) | session + niagara_userid post-2FA | form field `totp` |

---

## 27.9 Wire protocols — resumen cruzado con puertos

Para cada protocolo: puerto, primeros bytes handshake, keep-alive, timeout defaults. Detalles completos están en los bloques referenciados — acá el resumen operacional.

### 27.9.1 Fox (1911) — plain

Referencia: Bloque 13.2 (wire protocol), 19.11 (federation).
- Primeros bytes handshake: frames Fox binary custom — `fox a 1\n` ascii preamble + auth challenge
- `foxs` prefix distingue de plain
- Keep-alive: `niagara.fox.keepAliveInterval=5000` (ms)
- SO timeout: `niagara.fox.soTimeout=60000`
- Request timeout: `niagara.fox.requestTimeout=60000`
- Max server sessions: 100
- Max queue size: 32
- Circuit chunk size: 4096 bytes

### 27.9.2 FoxS (4911) — TLS wrapped

Mismo wire protocol que Fox, encapsulado en TLS. El handshake TLS típico + después los bytes Fox. Server cert = slot `BFoxService.foxsCert`. Si `supportLegacyClients` enabled → acepta TLS 1.0/1.1 también (security smell).

`supportLegacyClients` tiene 2 enumeradores distintos según generic (Supervisor) vs local (JACE) — `GENERIC_SUPPORT_LEGACY_CLIENTS_RANGE` vs `LOCAL_SUPPORT_LEGACY_CLIENTS_RANGE`. JACE tiene default más permisivo para integration con controllers viejos.

### 27.9.3 Platform daemon 5011 — custom binary TLS

No tengo source completo del daemon C++ pero el protocol wire (inferido de `niagara-help/devguide/releaseNotes.txt`):
- TLS handshake
- SCRAM-SHA256 authentication (como Fox, Bloque 18.6)
- Framing custom "niagarad" — mensajes typed con IDs numéricos
- Keep-alive implícito (TCP level)
- Session timeout: `niagara.daemonsession.timeout=60000`
- Stream timeout: `niagara.daemonsession.streamtimeout=500`

Cuando cliente Workbench hace "Platform > Connect" a 5011, el handshake es:
1. TCP connect 5011
2. TLS ClientHello → ServerHello con cert daemon
3. Cliente valida cert → prompt "Accept"
4. TLS handshake complete
5. Cliente envía SCRAM client-first-message con platform username
6. Server responde server-first-message (nonce)
7. Cliente envía client-final-message con proof
8. Server responde server-final-message
9. Session establecida, cliente puede hacer RPC (start station, upload dist, list modules)

**Platform credentials** son diferentes de BUser de la station. Viven en configuración OS del daemon — Windows tiene un registry key, Linux tiene `/etc/niagara/daemon.properties` o similar. 2 cuentas separadas por design (Bloque 10.1.2).

### 27.9.4 HTTP 80 / HTTPS 443 — Jetty embedded

Stack Jetty 9.4.54 (verificado `bin/ext/jetty-all-compact3-9.4.54.v20240208.jar` Bloque 17.1.2).

Handshake HTTPS estándar + TLS. Después request HTTP normal. Content servido por servlets Niagara (BWebServlet children, Bloque 9.3).

WebSocket upgrade para BOX: client envía `Upgrade: websocket`, server responde `101 Switching Protocols`. Bloque 22.12 tiene detalle del envelope BOX.

### 27.9.5 BACnet/SC (49152) — handshake TLS 1.3 new

Bloque 23.10 cubre BACnet/SC completo. Acá sólo: puerto **49152** (primero de la range "Dynamic/Private" IANA, deliberadamente escogido por ASHRAE para evitar colisión con servicios registrados). Proto TCP con TLS 1.3 mandatorio. mTLS obligatorio — tanto hub como node deben presentar cert. Handshake:
1. TCP 49152
2. TLS 1.3 handshake con ClientCertificate en el ClientHello (TLS 1.3 permite cert en primer flight)
3. Ambos certs validados contra trust store compartida
4. Tras TLS, BACnet Virtual Link Layer (BVLC) frames custom BACnet/SC flavor (Connect-Request, Connect-Accept, Heartbeat, ...)
5. Heartbeat period configurable, típico 30s

---

## 27.10 Listener configuration + lockdown production

### 27.10.1 `!config/` bog edits — dónde desactivar

Cada puerto tiene su bog slot editable. Workflow típico (vía Workbench o edición manual del bog con station stopped):

**Desactivar HTTP 80, forzar HTTPS only**:
```
Station > Services > WebService
  httpEnabled = false
  httpsEnabled = true
  httpsOnly = true
  requireHttpsForPasswords = true
```

**Desactivar Fox 1911, forzar FoxS only**:
```
Station > Services > NiagaraNetwork > FoxService
  foxEnabled = false
  foxsEnabled = true
  foxsOnly = true
  foxsMinProtocol = tlsv1_2
  supportLegacyClients = none  (o el valor mínimo del enum)
```

**Desactivar Platform plain 3011 (solo Supervisor + JACE)**:
- Editar `!daemon/platform.bog` — el TcpIpPlatformService maneja esto
- O desde Workbench `Platform > View > Enable/Disable SSL`
- `BPlatformSSLSettings.sslOnly = true`
- `BPlatformSSLSettings.sslEnabled = true`
- Cuando `sslOnly=true`, 3011 simplemente no se bindea

**Desactivar Fox multicast discovery (LAN scan)**:
```
FoxService.multicastEnabled = false
```

Discovery multicast (224.0.1.84 / FF02::137) es útil en dev pero leak información en prod. Hardening = off.

**Desactivar JMX (9010/9011)**:
En `nre.properties` o `station.properties`, remover flags `-Dcom.sun.management.jmxremote.port=9010`. Por default JMX **no está activado** salvo que alguien lo agregó deliberadamente. Verificar con `netstat -an | grep 9010`.

### 27.10.2 `BHttpProxyService` CIDR restrictions

Bloque 20.2 ya documentó `BHttpProxyService` para egress. En reso acá: para restringir salidas HTTP outbound a internet (p.ej., para EmailService, Licensing, ObixClient), definir CIDR `deny` en el proxy service. Evita exfiltración de datos si un módulo malicioso hace HTTP call random. No existe equivalente general para TCP egress — solo HTTP.

### 27.10.3 Lockdown recipe — prod hardening checklist

```
### WebService
httpEnabled = false
httpsEnabled = true
httpsOnly = true
httpsPort.publicServerPort = 443
httpsPort.adapter = (bind a NIC específica)
httpsMinProtocol = tlsv1_2
cipherSuiteGroup = recommended
requireHttpsForPasswords = true
gzipEnabled = true  (reducir bandwidth)

### FoxService
foxEnabled = false
foxsEnabled = true
foxsOnly = true
foxsMinProtocol = tlsv1_2
multicastEnabled = false
supportLegacyClients = none

### PlatformSSLSettings (daemon)
sslEnabled = true
sslOnly = true
sslPort = 5011
sslAlgType = tlsv1_2
cipherSuiteGroup = recommended
fipsMode = true (si compliance requiere)

### system.properties
niagara.moduleVerificationMode=high   (NO low)
cipherSuite.exclude.patterns=RSA_,CBC,3DES,SHA1
jdk.tls.rejectClientInitiatedRenegotiation=true
niagara.webbrowser.disabled=true  (si no usás Workbench browser)
niagara.commandLinePropertyBlacklist=niagara.moduleVerificationMode,...

### AuthenticationService
  remove BHTTPBasicAuthenticationScheme (legacy)
  remove BLegacyDigestAuthenticationScheme
  keep BDigestAuthenticationScheme (SCRAM) + BSAMLAuthenticationScheme (SSO corporate)
  password complexity policy: enable external validator (no nativo, Bloque 11.3.5)

### UserService
  per-user session timeout 15min
  auto logoff active
  max concurrent sessions per user = 2
  account lockout 5 failures / 30s

### AlarmService
  enable audit
  enable mail recipient con SMTP+STARTTLS
  no permitir clearAlarm permission a guest/operator

### BProvisioningService (si Supervisor)
  limitar execution a admin role solamente
  InstallCertificateStep con target User Trust Store explícito

### Firewall OS
  inbound: 443, 4911, 5011 (commissioning only), drop rest
  outbound: 443 (nCloud + HTTPS APIs), 4911 (subordinates), 25/587 (SMTP), 389/636 (LDAP), 88 (Kerberos)
  drop: 80, 1911, 3011, 9010, 9011, BACnet ports si no se usa
```

### 27.10.4 Logs — dónde mirar conexiones rechazadas

- `!logs/system.log` — niagara-general
- `!logs/auth.log` (si se activa) — login attempts y denegaciones
- `!logs/security.log` — cert validations, TLS handshake failures
- Daemon: `<Daemon Home>/logs/niagarad.log` — errores plat-level, cert rejections a 5011
- `EngineManager > $HogsPage` (Bloque 20.5) — si hay listener thread congestionado

Request específica de auditar TLS: **agregar** `-Djavax.net.debug=ssl:handshake:verbose` al JVM flags (nre.properties). Produce output masivo en stderr — deja mensaje por cada ClientHello/ServerHello/Certificate frame. Útil para diagnosticar "por qué TLS falla" en incidents — **pero desactivar después** o consume CPU + disk.

---

## 27.11 Gotchas + production incidents

Novedades/expansiones sobre gotchas ya listados en INDEX. Numerados para que INDEX los pueda referenciar.

### 27.11.1 Platform daemon 3011 plain abierto por default

**Empírico en N4.14**: out-of-the-box post-install, el daemon escucha **AMBOS** 3011 (plain) y 5011 (SSL). No hay `sslOnly=true` default. Un operator tiene que hacer la configuración explícita post-install para cerrar 3011. En deployments legacy se dejaba para debugging remoto "seguro detrás de VPN" — pero si un operador expone 3011 a WAN sin notar, SCRAM auth protege el login pero **todo el wire es plaintext** incluyendo comandos administrativos tras login. Hardening obligatorio: `sslOnly=true`.

### 27.11.2 `moduleVerificationMode=low` hardcoded en defaults

Ya mencionado en Bloque 17.6 + 18.3.1. Reiteración: esto **desactiva cert chain validation** completamente para módulos firmados. Un `.jar` con `.sig` generado por cualquier cert no-expired será cargado. En LOW, el atacante que logra escribir a `modules/` (via vulnerability filesystem access) puede firmar con self-signed + plant + ejecutar código arbitrario en la station. Mitigación mandatory para prod: `niagara.moduleVerificationMode=high` + import de CA en trust store. Pero rompe compat con módulos de vendors que no firman con CA issued.

### 27.11.3 Trust store corruption → startup falla cómo

Si `cacerts.jceks` está corrompido (truncated, wrong password, disk error), `BCertManagerService.serviceStarted()` lanza excepción. La station sale de SERVICE_STARTING estado y no llega a RUNNING. En `system.log` aparece stack trace con `java.io.IOException: Keystore was tampered with` o `java.security.NoSuchProviderException`.

Recovery:
1. Stop station
2. Backup `security/` dir completo
3. Si existe backup previo → restore
4. Si no → regenerar vacío: borrar `cacerts.jceks` + `keystore.jceks` + `.km` + `.kr`; al re-start station crea stores vacías con master password
5. Re-import todos los certs necesarios

**Consecuencia operacional**: sin los jceks la station pierde todas sus identities. FoxS cert, HTTPS cert, client cert mTLS — todo se regenera. Impacto multi-hora. Backup offline (ADB + JCEKS incluidos) es CRITICAL para disaster recovery.

### 27.11.4 OCSP NO verificado — cert revocado sigue aceptándose

Como §27.3.5, Niagara no valida OCSP. Un cert comprometido + revocado por la CA sigue siendo válido en Niagara hasta que expire. Impacto: si le roban la private key a un subordinate, el Supervisor lo seguirá aceptando en la federación hasta que el cert venza.

Mitigación: short validity (90 días) + rotación automatizada + script externo que revisa CRL y remueve certs revocados de trust stores.

### 27.11.5 FIPS mode no re-acepta JCEKS legacy — boot fail

Ya detalle en §27.7.5. Secuencia forzada: migrar JCEKS → BCFKS **antes** de activar fipsMode. Checklist operacional mandatorio.

### 27.11.6 Port collision en `publicServerPort` vs `localServerPort`

Si configurás `BServerPort.localServerPort = 8443` pero el OS ya tiene otro proceso escuchando 8443, el station boot falla con `BindException: address already in use`. No es gotcha Niagara sino OS — pero la **station NO te dice cuál puerto colisionó** en el log por default. Hay que correr `netstat -an | grep LISTEN` manualmente. Workbench mejora el UX en 4.12+ mostrando "port in use" en el Status slot del WebService, pero algunos drivers no implementan aún el reporting.

### 27.11.7 `supportLegacyClients` default permite TLS 1.0

En `BFoxService` el slot `supportLegacyClients` viene con default en un enum range que incluye TLS 1.0. Si no lo tocás, clientes con TLS 1.0 se conectan. Compliance audit trigger. Hardening: `supportLegacyClients=none` + `foxsMinProtocol=tlsv1_2`.

### 27.11.8 Mismo cert 443 + 4911 → rotación sincronizada

Si usás el mismo alias (`tridium` típico) en `httpsCert` y `foxsCert`, rotar ese alias impacta ambos servicios simultáneamente — cortando Workbench (443 + 4911) y browser (443). Mitigación: usar 2 aliases distintos para 443 y 4911. La mitad del Workbench usa 5011 para commissioning (diferente cert), así que el blackout durante rotación 443+4911 puede tolerarse si es planificado.

### 27.11.9 Fox session 24h + cert rota mid-session

Ya mencionado. Expansión: la session FoxS tiene lifetime 24h antes de re-auth forzado. **Si rotás el cert durante una session activa, la session NO detecta el cambio y sigue usando el cert cached en el TLS context**. El TLS context vive en memoria JVM cliente + server; hasta que el TCP se rompa (timeout, error, restart), el cert viejo sigue activo.

Gotcha adicional: si el cert viejo expira durante la session activa, TLS **NO re-valida validity dates en data frames posteriores** — solo en handshake. Así que la session continúa "inválida" pero funcional. En el próximo handshake sí falla.

### 27.11.10 SAML IdP cert rotation — no auto

Cuando el IdP rota su signing cert (ADFS, Okta, Auth0), el Niagara `SAMLAuthenticationService` no lo detecta automático. Las aserciones SAML firmadas con cert nuevo fallan validation. Usuarios quedan bloqueados login SAML. Mitigación: monitor proactivo del IdP cert expiry + manual update en Niagara SAMLIdPService.

IdPs buenos publican metadata en URL; Niagara podría pollearlo pero no lo hace default. Feature gap.

### 27.11.11 `niagara_session` cookie NOT Secure en HTTP

Si tenés HTTP enabled (port 80), el cookie `niagara_session` se emite **sin** flag Secure (obviamente — el cliente no tiene TLS para usarlo). MITM en LAN puede robar la cookie y replay → session hijacking. Por eso `httpsOnly=true` es lockdown mandatory. Y `requireHttpsForPasswords=true` bloquea al menos el login POST via HTTP, aunque la session post-login sigue viajando plain.

### 27.11.12 `Webs.license` tiene `skipModuleValidation` feature

Bloque 18.3.2 lo mencionó. Expansion: el `Webs.license` (16 KB) en `security/licenses/` contiene feature `skipModuleValidation=true`. El `Honeywell.license` (2.3 KB) **NO** lo contiene. Esto significa:

- Si tu station está licensed con Webs license → `-Dniagara.classLoader.skipModuleValidation=true` FLAG + license feature AND → bypass
- Si tu station está licensed con Honeywell license → FLAG solo no es suficiente, falla

Importante para cross-licensing: Webs es más permisivo para devs (pre-prod), Honeywell es stricter para prod. Hardening deliberado del OEM.

### 27.11.13 Multicast discovery filtrable por OS firewall silent

`FoxService.multicastEnabled=true` por default. Si el host OS tiene firewall bloqueando multicast (Windows Defender Public profile bloquea por default), el multicast SALE (egress) pero las respuestas de otros hosts NO LLEGAN. La station "no encuentra" otras stations en la LAN, cuando en realidad están ahí. Diagnóstico confuso. Mitigación: abrir puerto UDP 224.0.1.84 inbound en firewall, O `multicastEnabled=false` + descubrir manualmente por IP.

---

## 27.12 Mental model — cuándo usar qué + diagrama de commissioning

### 27.12.1 Decisión quick — qué puerto y cert para qué

```
¿Query API station (REST/BOX/WebSocket)?
   → HTTPS 443 + session cookie + CSRF
   → Cert: httpsCert (BWebService)

¿Supervisor ↔ Subordinate (federation)?
   → FoxS 4911 mTLS
   → Cert: foxsCert ambos lados + cross-trust

¿Workbench commissioning (upload dist, module install)?
   → Platform 5011 SSL
   → Cert: BPlatformSSLSettings.keyAlias (daemon)
   → Auth: platform credentials (≠ BUser station)

¿Integración BACnet legacy?
   → 47808/UDP plain
   → (no TLS — spec BACnet/IP antiguo)

¿Integración BACnet moderno?
   → 49152/TCP BACnet/SC + TLS 1.3 mTLS
   → Cert: BACnet device-specific

¿Auth SSO corporate?
   → SAML via 443 browser flow
   → Cert IdP signing → import en User Trust Store
   
¿Auth usuario técnico sin cert?
   → Digest SCRAM-SHA256 sobre HTTPS 443 o FoxS 4911

¿Auth usuario high-assurance?
   → ClientCertAuth mTLS sobre 4911/443
   → Cert per-user en User Key Store cliente
```

### 27.12.2 Flow commissioning end-to-end — caso Workbench→JACE nuevo

Secuencia cronológica (cuando clickeás "Platform > Connect" hasta que la station está running):

```
0. PRE: JACE tiene niagarad running, port 5011 abierto, cert daemon self-signed default.

1. WB: abre TCP a JACE:5011
   ├─ TLS ClientHello (WB)
   ├─ ServerHello + cert daemon self-signed (JACE)
   ├─ WB: cert desconocido → prompt operator "Accept this certificate?"
   └─ Operator: Accept permanent → cert agregado a WB User Trust Store (~/Niagara4.14/OptimizerSupervisor/security/cacerts.jceks)

2. WB: SCRAM authentication
   ├─ client-first-message (username platform)
   ├─ server-first-message (nonce)
   ├─ client-final-message (proof)
   └─ server-final-message (authenticated)

3. WB: platform session establecida → RPC calls
   ├─ list_modules (list actuales en modules/)
   ├─ list_stations (list /stations/)
   ├─ host_id (para license)
   └─ free_space (storage check)

4. WB: "Install New Station" wizard
   ├─ upload dist file (cleanDist/tridium-qnx7-...)
   ├─ dist extracted en nueva station folder
   ├─ newStationConfig bog merged in
   ├─ license.xml uploaded (para ese hostId)
   ├─ tridium.certificate + honeywell.certificate pushed si no presentes
   └─ upload user module jars adicionales

5. WB RPC: start_station(stationName)
   ├─ niagarad spawn nuevo JVM child
   ├─ JVM carga NRE + classpath dinámico
   ├─ BCertManagerService.serviceStarted → carga JCEKS keystores (si ya existen)
   ├─ generate self-signed "tridium" alias si User Key Store vacío
   ├─ BFoxService.started → bindea foxsPort 4911, usa "tridium" alias
   ├─ BWebService.started → bindea httpsPort 443, usa "tridium" alias
   └─ Fase de boot Bloque 10 avanzando hasta RUNNING

6. WB: conecta Fox a new station:4911
   ├─ TCP 4911 + TLS handshake
   ├─ cert "tridium" (self-signed new station) presentado
   ├─ WB: cert desconocido → prompt "Accept?" (SEGUNDA aceptación, cert distinto del 5011)
   ├─ Accept → cert a WB User Trust Store (ahora tiene 2 certs del mismo JACE: 5011 daemon + 4911 station)
   └─ SCRAM auth con credenciales BUser admin (distinto del platform credentials)

7. WB: Fox session establecida, operator puede ver Nav tree, editar bog, etc.

ARCHIVOS CAMBIADOS EN EL JACE DURANTE ESTE FLOW:
- stations/<name>/config.bog (nuevo)
- stations/<name>/history/ (empty init)
- stations/<name>/alarm/ (empty init)
- shared/ o similar (para shared file space)
- security/licenses/<hostId>/ (license archived)
- modules/ (nuevos jars si el operador agregó)

ARCHIVOS CAMBIADOS EN EL WORKBENCH:
- ~/Niagara4.14/OptimizerSupervisor/security/cacerts.jceks (2 certs nuevos)
- ~/Niagara4.14/OptimizerSupervisor/etc/recentOrds.xml (nueva ord del JACE)
- logs del Workbench

OPERACIONES QUE TRIVIAN ESTE FLOW:
- Si JACE está detrás de NAT → publicServerPort ≠ localServerPort (p.ej., WB ve 5011 pero daemon bindea 5011 interno)
- Si JACE tiene fipsMode=true → keystores son BCFKS, necesitas Workbench con misma config
- Si WB tiene `moduleVerificationMode=high` → signing cert del WB module set tiene que validar
```

### 27.12.3 Conexiones cruzadas con otros bloques — cheat sheet

- **Quiero auditar todas las surfaces de red abiertas**: §27.1 catálogo → §27.10.3 lockdown recipe → Bloque 20 service monitoring
- **Quiero rotar un cert sin romper federation**: §27.7.4 cross-sign → Bloque 13.1 Niagara Network → Bloque 16.10 provisioning job InstallCertificateStep
- **Quiero activar FIPS mode**: §27.7.5 flow → Bloque 17.5.7 BouncyCastle FIPS → Bloque 18 re-firmar módulos
- **Quiero implementar SSO SAML**: §27.8.6 header auth NO existe → Bloque 11.2.6 SAML → import IdP signing cert en User Trust Store (§27.4.2)
- **Quiero auditar login failures**: §27.10.4 logs → Bloque 11.3.5 lockout → Bloque 18.5 CSRF
- **Quiero configurar firewall OS**: §27.2 matrix → §27.10.3 recipe → Bloque 10 platform commissioning ports
- **Quiero debuggear "BACnet/SC no conecta"**: §27.1 port 49152 → Bloque 23.10 wire detail → §27.4 certManagement (mTLS cert)

### 27.12.4 Resumen conceptual — la superficie de red de una station

Una station Niagara N4.14 típica expuesta en red "escucha" en **al menos 5 sockets TCP** (3011/5011 daemon, 1911/4911 station Fox, 443 Jetty), **1-2 UDP** (47808 BACnet si aplica, 224.0.1.84 Fox multicast), y **puede abrir N outbound** (licensing 443, SMTP, LDAP/AD, BACnet outbound, etc.).

Cada listener está respaldado por:
- Un **puerto Java** modelado como `BServerPort` (pub + local + protocol + adapter)
- Un **cert server** (si TLS) con alias en User/System Key Store
- Un **TLS config** (minProtocol + cipher group + extended master secret)
- Un **auth scheme asociado** (SCRAM para Fox/Platform, cookie-based para HTTP, mTLS opcional para 4911/443)
- **3 trust stores** que validan clients (System, User, Untrusted)

La **distinción crítica de trust boundaries** es:
- Install Home (read-only, Tridium/Honeywell authority — certs XML custom, signing cert hardcoded)
- Daemon Home (niagarad OS account — System Trust Store, 5011 identity)
- User Home Workbench o station (User Trust Store — peers, servers outbound)

Tres niveles de "trust", tres cuentas OS, tres scope de acción separados por design. Rotación, auditoría, backup e incident response tienen que respetar estas fronteras o generan incidents.

El paradigma mental útil: **pensá la station como un device con múltiples NICs virtuales cada uno con su cert, su política, su log de auditoría**. Como un router enterprise moderno. No es "la station tiene HTTPS": es "la station tiene 5+ interfaces TLS, cada una con lifecycle independiente".

---

## Síntesis del bloque

Bloque 27 consolidó la **superficie de red completa** de una Niagara N4.14 productiva:

- **22 puertos catalogados** (10 core Niagara + 12 drivers/auxiliar), con defaults verificados empíricos vs doc oficial.
- **`BServerPort` como tipo unificador** — 4 slots (public/local/proto/adapter), posibilita NAT + port forwarding interno sin fragilidad.
- **TLS boundary matrix** — 3 versiones soportadas (1.0..1.3), 2 cipher groups, FIPS mode opt-in con BCFKS obligatorio, SNI gotcha para reverse proxies.
- **certManagement directory empírico** — 3 zonas (install XML custom, user certManagement/ + security/, daemon ProgramData), cada una con propósito específico.
- **7 tipos de certificado distintos** — signing Honeywell eternal, signing user 20y, HTTPS 1-3y, FoxS 1-3y, daemon 1-3y, client mTLS 1-2y, SAML IdP, BACnet/SC.
- **3-level trust chain** (System/User/Untrusted) + exemption store — lookup order strict, NO OCSP/CRL validation (gap!).
- **Cert lifecycle flows** — 3 vías generación, CSR+CA flow, rotación cross-sign limitada, FIPS migration mandatory JCEKS→BCFKS.
- **Cookie + auth stack deep** — 10 cookie names, SameSite enum, session fixation protection post-login, CSRF separate from session, BHeaderAuthenticationScheme **NO existe** (gap SSO headers).
- **Lockdown recipe operacional** — checklist exacto para hardening production (disable HTTP/Fox plain, fipsMode, moduleVerificationMode=high, cipher exclude patterns).
- **13 gotchas nuevos** — 3011 abierto default, OCSP ausente, FIPS boot fail sin migration, sessions con cert cached rotation-blind, Webs.license permisivo vs Honeywell.license strict.
- **Commissioning flow end-to-end** — Workbench → JACE:5011 → install station → JACE:4911, 2 aceptaciones cert distintas, archivos cambiados lado JACE + lado WB.

**Conecta con**: Bloque 3 (sandbox JVM — policy + trust root), 10 (commissioning), 11 (auth runtime), 13.2 (Fox wire), 17 (filesystem forensics), 18 (signing deep), 19.17 (BOX), 23.10 (BACnet/SC TLS 1.3), 26 (native launcher + JRE).
