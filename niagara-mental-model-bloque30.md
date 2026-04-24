# Niagara N4 — Mental Model · Bloque 30

**Tema**: Enterprise auth federation (LDAP/SAML/OAuth/Kerberos/mTLS) + FIPS 140 workflow operacional + master keyring (.km/.kr) + TLS cert rotation + token lifecycle cross-token matrix + RBAC method-level + Auditor en invocation + session fixation + permission propagation en federation.

**Método**: Investigación empírica READ-ONLY — decompilación `ldap-rt.jar` (29 clases), `saml-rt.jar` (55 clases RP+IdP), `clientCertAuth-rt.jar` (13 clases), `gauth-rt.jar` (11 clases Tridium), `oauth2-rt.jar` (11 clases Tridium, 1000 con deps Nimbus), `platCrypto-rt.jar` (96 clases), `baja.jar` (security + session + authn), `history-rt.jar` (audit pipeline), `bin/ext/bcfips/` (4 BCFIPS JARs), `defaults/system.properties`, filesystem `/home/cristian/Niagara4.14/OptimizerSupervisor/security/` verificado WSL (3 JCEKS + `.km`/`.kr` DPAPI-encrypted keyring + exemptions.tes + signing/), contrastado con niagara-help/devguide/ y guides/.

**Conecta con**: Bloque 3 (sandbox JVM + 25 permission groups), Bloque 11 (RBAC base — BUser/BRole/BCategory/BPermissions), Bloque 13 (sensitive data + keyring + BPassword reversible), Bloque 17 (JRE + BCFKS FIPS + 3 Homes trust boundary), Bloque 18 (SCRAM + signing + CSRF + exemptions.tes), Bloque 20.10 (gap analysis — este bloque cierra #10/#11/#15/#16/#17), Bloque 27 (cert types matrix + 4 trust stores + HeaderAuth NO existe), Bloque 29 (web auth schemes + filter chain 15 capas).

---

## 30.1 LDAP federation deep

### 30.1.1 Módulo `ldap-rt.jar` — inventario

29 clases en `com.tridium.ldap.*`:

| Subpaquete | Clases | Rol |
|------------|--------|-----|
| `com.tridium.ldap` | `BLdapAuthenticationScheme`, `BLdapConfig`, `BConfigurableLdapConfig`, `BLdapTypeConfig`, `LdapLoginModule`, `BWebLdapUsernamePasswordCallbackHandler`, `BFoxLdapUsernamePasswordCallbackHandler`, `BFoxLdapUsernamePasswordClientAuthnHandler`, `BLdapBasicLoginHTMLForm` | Base LDAP + scheme + callback handlers Web/Fox |
| `com.tridium.ldap.v2` | `BLdapV2Config`, `BActiveDirectoryConfig` | LDAP v2 legacy simple bind + AD specialization |
| `com.tridium.ldap.v3` | `BLdapV3Config`, `BAuthenticationMechanism`, `BindNameFormatter`, `BKerberosAuthenticationScheme`, `BKerberosConfig`, `BKeytabFile`, `KerberosKeytabConfig`, `KerberosCallback`, `KerberosLoginModule`, `AcquireCredentialsAction`, `AcquireHttpCredentialsAction`, `BWebKerberosCallbackHandler`, `BFoxKerberosCallbackHandler`, `BFoxKerbClientAuthnHandler`, `GSSDelegCredAction`, `LdapKerberosAuthAction` | LDAP v3 SASL + Kerberos V5 GSS-API |
| `com.tridium.ldap.dashboard` | `BLdapSchemeSecurityItemProvider`, `BKerberosSchemeSecurityItemProvider` | Security Dashboard integration |

**Diseño clave**: `BLdapAuthenticationScheme` es UN scheme — la variante (v2/v3/AD/Kerberos) se elige vía slot `config` que contiene subtipo de `BConfigurableLdapConfig`. El scheme delega a `LdapLoginModule` (JAAS) que resuelve la variante dinámicamente.

### 30.1.2 `BLdapConfig` — slots verificados

Decompilación directa de `BLdapConfig.class`:

```java
public class com.tridium.ldap.BLdapConfig extends javax.baja.sys.BComponent {
  public static final Property enableConnectionPooling;
  public static final Property connectionUrl;       // ldap://host:389 o ldaps://host:636
  public static final Property SSL;                 // true = LDAPS puerto 636
  public static final Property userLoginAttr;       // sAMAccountName (AD), uid (OpenLDAP)
  public static final Property userBase;            // DC=corp,DC=com
  public static final Property attrEmail;
  public static final Property attrFullName;
  public static final Property attrLanguage;
  public static final Property attrCellPhoneNumber;
  public static final Property attrPrototype;       // mapping prototype → BRole
  public static final Property cacheExpiration;     // BRelTime — TTL cache fallback
  public static final Property connectionTimeout;   // int ms
  public static final Property initialContextFactory; // javax.naming InitialContextFactory FQCN
  public static final Property referral;            // follow | ignore | throw (JNDI)
}
```

**Todos los atributos son configurables per-scheme**. Caso AD: `userLoginAttr=sAMAccountName`, `attrFullName=displayName`. Caso OpenLDAP: `userLoginAttr=uid`, `attrFullName=cn`.

### 30.1.3 `BLdapV3Config` — props extendidos v3

```java
public class BLdapV3Config extends BConfigurableLdapConfig {
  public static final Property bindFormat;             // BFormat — template DN p.ej. "uid={0},ou=users,dc=corp"
  public static final Property connectionUser;         // DN usado para search (bind-as)
  public static final Property connectionPassword;     // BPassword reversible (requires master keyring)
  public static final Property authenticationMechanism; // BAuthenticationMechanism: None | CRAM-MD5 | DIGEST-MD5 | GSSAPI
}
```

**`bindFormat` usa `BFormat`** (Bloque 5.2/4.1 `%{0}%` placeholder). `{0}` se sustituye con username durante search→bind flow.

`BLdapV3Config.checkLicense()` existe — LDAP v3 es **license-gated** (verificado `ldapv3Feature` field). LDAP v2 no hace license check (legacy libre).

### 30.1.4 LDAP bind flow — código verificado

Métodos en `BLdapConfig`:
```java
public BUser authenticate(String, String) throws LoginException;
public BUser authenticate(BIUserCredentials) throws LoginException;
protected DirContext initialDirContext(BIUserCredentials) throws Exception;
protected void rebind(DirContext, BICredentials, String) throws Exception;
protected BUser authCachedCredentials(BIUserCredentials) throws LoginException;
protected SearchResult getSearchResult(DirContext, String) throws LoginException;
protected BUser prepareUser(BIUserCredentials, BUser, Attributes, DirContext) throws NamingException;
```

**Flow** (reconstruido por nombres de métodos):

1. `authenticate(creds)` → `initialDirContext(creds)` abre conexión LDAP (service account o anonymous via `connectionUser`).
2. `getSearchResult(ctx, username)` ejecuta search con filtro templated — `({userLoginAttr}={username})` en `userBase`.
3. Extrae DN del result.
4. `rebind(ctx, creds, dn)` — reautenticación con credentials del user real (prueba password).
5. `prepareUser(...)` mapea atributos LDAP → `BUser` slots (email, fullName, language, cellPhoneNumber).
6. `getPrototypeUserAccount(...)` resuelve prototype (lookup por `attrPrototype` attribute) → clona con roles.
7. Si bind falla + LDAP unreachable → `authCachedCredentials(creds)` fallback contra cache local.

### 30.1.5 Connection pool

`enableConnectionPooling` slot existe pero **pool size NO exposed** como slot. Usa `com.sun.jndi.ldap.connect.pool.*` system props (JNDI estándar):
- `com.sun.jndi.ldap.connect.pool.maxsize` (default illimitada)
- `com.sun.jndi.ldap.connect.pool.timeout` (default 5 min)
- `com.sun.jndi.ldap.connect.pool.prefsize`

**Gotcha**: tuning del pool requiere setearlos en `system.properties` al nivel JVM, NO configurables por-scheme.

### 30.1.6 Referral handling

`referral` slot = JNDI `Context.REFERRAL` = `follow | ignore | throw`:
- **follow**: AD cross-domain forest — sigue referrals auto (riesgo: loop).
- **ignore**: descarta silent — users de otros dominios no encontrables.
- **throw**: excepción, manejada por caller (default safest).

### 30.1.7 StartTLS vs LDAPS

`SSL=true` + `connectionUrl=ldaps://host:636` → **LDAPS** (TLS desde socket).
`SSL=false` + `connectionUrl=ldap://host:389` → **plain** (password viaja claro — NUNCA producción).

**StartTLS** (TLS negotiation en conexión plain 389) **NO está exposed como slot separado**. Requiere setear `ctx.extendedOperation(new StartTlsRequest())` en custom — no nativo. Workaround: usar siempre LDAPS 636.

### 30.1.8 Group mapping → BRole

**Niagara NO sincroniza grupos LDAP automático a roles**. El mapping es vía **prototype users**:

1. Config slot `attrPrototype` (default `description` o custom) — cada user en LDAP debe tener attribute que apunte a un prototype user preconfigurado en station.
2. Prototype user (en UserService carpeta `prototypes`) tiene los roles asignados.
3. `getPrototypeUserAccount(attrs, ctx, ...)` busca prototype por attribute value.
4. `updateUserFromPrototype(user, proto, attrs)` clona roles del prototype al user dinámico.

**Implicación**: mapeo por **user-attribute** en LDAP, NO por membership de grupo. AD `memberOf` NO se inspecciona nativo.

**Workaround para mapear por grupo**: custom `BUserPrototypeMergePolicy` (slot `prototypeMergePolicy` existe en scheme vía interface `IHasPrototypeMergePolicy` — verificado en `BLdapAuthenticationScheme`).

### 30.1.9 Fallback a cache si LDAP unreachable

`authCachedCredentials(creds)` — si `initialDirContext` timeout, consulta cache interno (encriptado con master keyring). `cacheExpiration` slot (BRelTime) controla TTL — típico 24h.

**Gotcha**: cache usa credentials de la última auth exitosa. Si user cambió password en LDAP, durante outage el password viejo sigue valiendo hasta expirar cache.

### 30.1.10 Active Directory specifics — `BActiveDirectoryConfig`

Subclase de `BLdapV2Config`. Override de defaults:
- `userLoginAttr=sAMAccountName`.
- `referral=follow` (AD forest multi-domain).
- Auto-append `@domain.com` al username si user ingresa sin UPN (BindNameFormatter lo maneja).

---

## 30.2 SAML 2.0 federation deep

### 30.2.1 Inventario `saml-rt.jar`

55 clases en 4 subpaquetes:

| Subpaquete | Rol |
|------------|-----|
| `com.tridium.saml` | Exceptions + attribute mapper (13 clases) |
| `com.tridium.saml.authnScheme` | RP scheme + LoginModule + servlets consumer (6 clases) |
| `com.tridium.saml.idp` | **Niagara como IdP completo** (CoT folders, StationServiceProvider, IdPService) (18 clases) |
| `com.tridium.saml.rp.servlet` | Servlets SAMLRPServlet/ConsumerServlet/UuidMap (6 clases) |
| `com.onelogin.saml` | Utils + XMLErrorHandler (2 clases — OneLogin reference impl embedded) |

**HALLAZGO CRÍTICO**: Niagara SÍ puede funcionar como **IdP SAML** — `BSAMLIdPService` verificado decompilado. No solo RP. Esto contradice assumption común y abre uso cases (Niagara como IdP para sistemas embebidos downstream sin IdP corporativo).

### 30.2.2 `BSAMLAuthenticationScheme` (RP) — slots verificados

```java
public final class BSAMLAuthenticationScheme extends BSSOAuthenticationScheme {
  public static final Property entityId;                           // Service Provider ID único
  public static final Property idpHostURL;                         // https://idp.corp.com
  public static final Property idpHostPort;                        // 443
  public static final Property idpLoginPath;                       // /auth/SSO
  public static final Property includeQueryParamsInDestination;    // bool — pasa query params originales
  public static final Property idpCert;                            // alias del cert IdP en user trust store
  public static final Property samlServerCert;                     // cert Niagara para firmar AuthnRequest
  public static final Property samlServerCertAliasAndPassword;     // BCertificateAliasAndPassword
  public static final Property timeSkew;                           // BRelTime — default 3 min
  public static final Property requestedAuthenticationType;        // Password|Kerberos|SmartCard|Unspecified (BEnumFilter)
  public static final Property requestedAuthenticationComparisonMode; // Exact|Minimum|Better|Maximum
  public static final Property prototypeMergePolicy;
}
```

**Hereda de `BSSOAuthenticationScheme`** (igual que Kerberos y ClientCert) — NO de `BPasswordAuthenticationScheme`. Marca semántica: SSO externo vs password-based local.

### 30.2.3 Flow SAML RP — verificación empírica

Servlets (`com.tridium.saml.rp.servlet`):
- `SAMLRPServlet` — endpoint principal `/saml/samlrp/*`.
- `SAMLConsumerServlet` — Assertion Consumer Service (ACS) — recibe SAML Response POST desde IdP.
- `SAMLAuthenticationInfoHandler` — extrae user info del SAMLAuthenticationInfo.
- `SAMLUuidMap` — cache de request UUIDs con `CleanupTask` inner class (previene replay).
- `SAMLLoginModule` (JAAS).
- `BWebSAMLCallbackHandler`, `SAMLCallback`.

**Metadata URL** (confirmado strings): `https://<station>/saml/samlrp/metadata?scheme=<SchemeName>` — auto-genera SP metadata XML para registrar en IdP.

**Flow SP-initiated**:
1. User → `/login` → scheme=SAML detectado.
2. Niagara genera `AuthnRequest` (XML firmado con `samlServerCert`), redirect 302 a `${idpHostURL}:${idpHostPort}${idpLoginPath}?SAMLRequest=<base64>`.
3. UUID del request → `SAMLUuidMap` (cleanup task expira en timeSkew window).
4. IdP autentica user, retorna SAML Response POST a ACS URL `https://<station>/saml/samlrp/consumer`.
5. `SAMLConsumerServlet` valida signature (contra `idpCert`), valida timestamp (con `timeSkew`), valida UUID en map.
6. Extrae attributes → `BSAMLAttributeMapper` aplica `BSAMLAttributeMapping` configurados → populate BUser slots.
7. `setPrototypeUser()` si `prototypeMergePolicy` asignado.
8. Session regenera ID post-auth (session fixation protection).

### 30.2.4 `BSAMLIdPService` — Niagara como IdP

Clase **`BAbstractService`** hereda. Slots:

```java
public static final Property idpSigningCert;                      // alias para firmar assertions
public static final Property idpSigningCertAliasAndPassword;
public static final Property entityId;                            // IdP ID
public static final Property timeSkew;
public static final Property applyTimeSkewToResponse;             // bool
public static final Property circleOfTrustFolder;                 // BCircleOfTrustFolder contenedor de SPs
```

Actions:
- `buildStationServiceProvider(BString)` — registra otra station como SP.
- `getStations()`, `getUsers()`, `getAuthSchemes()`.

**Circle of Trust** (`BCircleOfTrust` + `BCircleOfTrustFolder`): Niagara agrupa SPs federados que confían en este IdP. Cada SP se registra con su metadata + cert.

**Servlets IdP**:
- `SAMLIdPAuthnRequestServlet` — recibe AuthnRequest de SPs.
- `SAMLIdPProcessLoginServlet` — auth user, genera Response firmado.

**License feature**: verificable por `LICENSE_FEATURE` constante en decompile — IdP es feature separada de RP, usualmente bundled en licencias enterprise.

### 30.2.5 Assertion encryption — `samlEncryption-rt.jar`

Módulo separado 1.22 MB (**658 classes**) — órden de magnitud mayor que `saml-rt.jar` (173 KB, 55 classes). Encriptación de assertions XML (XML-ENC) opt-in.

Interfaces:
- `BISamlXmlEncrypter` (decompilada en idp).
- `BISamlXmlDecrypter` (decompilada en authnScheme).

**Gotcha**: sin `samlEncryption-rt` cargado, assertions van en claro dentro del POST (protegido solo por TLS). Con `samlEncryption-rt`, el XML Assertion se encripta con cert del SP + firma — defense-in-depth.

### 30.2.6 Attribute mapping → BRole + BCategory

`BSAMLAttributeMapper` + `BSAMLAttributeMapping` + 5 tipos `BSAMLUserProperty`:
- `BSAMLStandardUserProperty` — mapea a slot estándar BUser (email, fullName).
- `BSAMLPrototypeUserProperty` — mapea a prototype (cascada con rolemapping).
- `BSAMLExpirationUserProperty` — mapea attribute a BUser.expiration.

**Mapping por XPath/attribute name**. No automatic group-to-role — igual que LDAP, vía prototype.

### 30.2.7 Single Logout (SLO) — estado

**NO hay clase `SAMLLogoutServlet` ni `SLOService`** en decompile (confirmado `grep -i logout`). Niagara IdP/RP **NO implementa SLO** — logout local en Niagara NO propaga al IdP ni a otros SPs.

**Gotcha enterprise crítico**: user hace logout en Niagara → sigue logueado en IdP → otra pestaña vuelve a entrar sin re-auth. Compliance enterprise requiere SLO (SOC 2, FedRAMP).

### 30.2.8 Clock skew + replay prevention

`timeSkew` default 3 min (verificado). `SAMLUuidMap$CleanupTask` expira UUIDs — previene replay attacks dentro de window.

**Gotcha clásico**: NTP mal sincronizado entre IdP y station → assertions fallan silent `NotOnOrAfter` validation. Solo visible en `system.log` con log level DEBUG.

---

## 30.3 OAuth 2.0 / OIDC — estado

### 30.3.1 Módulo `oauth2-rt.jar` existe — 1.74 MB

1000 classes totales en JAR, pero **solo 11 son Tridium** — resto es deps Nimbus OAuth 2.0 SDK + JWT libs embebidas.

Clases Tridium verificadas:
- `OAuth2AuthorizationServerMetadata`
- `OAuth2AuthorizationRequest`
- `OAuth2AuthorizationResponse` + inner `Result`
- `OAuth2AuthorizationServerMetadataResolver`
- `OAuth2AuthorizationServerConfigurationException`
- `OAuth2AuthorizationException`
- `com.tridium.oauth2.clientcredentials.OAuth2ClientCredentialsGrantRequest` + `OAuth2ClientCredentialType`
- `com.tridium.oauth2.jwt.OAuth2JWTBearerGrantRequest`

### 30.3.2 Niagara como OAuth CLIENT, no server

**NO hay `BOauth2AuthenticationScheme`** (grep confirmado). El módulo `oauth2-rt` es **cliente OAuth** — Niagara pide tokens a providers externos para consumir APIs, NO autentica users via OAuth.

**Use cases reales del módulo**:
- nCloud subscription licensing — Niagara obtiene OAuth token para llamar `niagara-cloud.honeywell.com` APIs.
- Outbound integration con APIs REST que requieren Bearer token.

**Grant types soportados** (según clases):
- `client_credentials` (M2M) — `OAuth2ClientCredentialsGrantRequest`.
- `urn:ietf:params:oauth:grant-type:jwt-bearer` (service account) — `OAuth2JWTBearerGrantRequest`.

**NO soporta**: `authorization_code`, `implicit`, `password`, `refresh_token` como schemes de auth al station. **Gap cerrado: OIDC NO disponible nativo** para auth de users — requiere SAML o LDAP.

### 30.3.3 Google TOTP (gauth) — sigue siendo TOTP local, NO OAuth

`BGoogleAuthenticationScheme extends BPasswordAuthenticationScheme` — verificado en decompile. Es **password + TOTP local** (seed compartido station↔móvil), NO federación con Google OAuth.

**Correción Bloque 11.3.7**: la tabla llama "OAuth2 gauth-rt" pero es erróneo — gauth implementa RFC 6238 TOTP, no OAuth 2.0.

---

## 30.4 Kerberos SPNEGO deep

### 30.4.1 Config verificado

`BKerberosConfig extends BLdapConfig`:

```java
public static final Property realm;                    // CORP.COM
public static final Property keyDistributionCenter;    // ad.corp.com:88
public static final Property stationKerberosName;      // HTTP/station.corp.com@CORP.COM (SPN)
public static final Property stationKerberosPassword;  // fallback si no hay keytab
public static final Property keyTabFile;               // BKeytabFile struct con keyTabLocation (BOrd)
public static final Action getKeytabsAction;
```

### 30.4.2 Keytab file + encryption automática

**`BKeytabFile.keyTabLocation`** es `BOrd` — apunta a ubicación en file space (típico `!config/keytab/station.keytab` o BOrd local).

Hallazgo directo del decompile de `BKerberosConfig`:
```java
private static void encryptKeytabs();
private static boolean ensureEncrypted(File) throws IOException;
static void decryptFile(File) throws IOException;
```

**Keytab se encripta automáticamente al cargar**. Niagara NO deja keytab plaintext en disk. Uso: al startup, lee keytab, lo transforma con master keyring, lo guarda encrypted; decrypt on-demand al invocar `getStationSubject()`.

**Conexión Bloque 25.3** — `KeytabMigrator` entre versions: re-encrypta keytab con keyring de la nueva user home durante migration.

### 30.4.3 SPNEGO negotiation flow

`BWebKerberosCallbackHandler` + `KerberosLoginModule` + JAAS.

Flow:
1. Browser → `/` sin auth.
2. Niagara → 401 `WWW-Authenticate: Negotiate`.
3. Browser (con ticket ya cacheado via `kinit` o Windows SSO) → POST con `Authorization: Negotiate <base64 GSS token>`.
4. Niagara GSS-API valida token contra `stationKerberosName` principal + keytab.
5. Extrae user name del ticket → busca BUser.
6. `KerberosLoginModule.login()` completa flow JAAS.

### 30.4.4 Subject caching — en memoria

```java
javax.security.auth.Subject subject;
java.util.Date subjectExpiration;
```

`BKerberosConfig` cachea Subject del station (TGT) — renovación proactiva vía `started()` lifecycle. Si TGT expira y KDC down, station NO puede validar tickets incoming (gap resiliency — no fallback password-based per-request).

### 30.4.5 Cross-realm trust

Soporte nativo JVM vía `krb5.conf` — `BKerberosConfig.writeKrb5Conf(string, ctx)` permite generar config dinámico. Trust directo realm A ↔ realm B requiere config en ambos KDCs + `[capaths]` en krb5.conf.

**NO hay fallback a NTLM** — verificado en decompile (no clase `NTLMLogin*`). Niagara es Kerberos-pure para AD integration.

---

## 30.5 mTLS client cert — `clientCertAuth-rt.jar`

### 30.5.1 `BClientCertAuthScheme` — 13 clases módulo

```java
public final class BClientCertAuthScheme extends BSSOAuthenticationScheme
    implements TrustAnchorProvider {
  public static final int GET_CLIENT_CERTIFICATE;
  private String trustAnchorProviderId;
  public Set<TrustAnchor> getTrustAnchors();
  protected void trustAnchorsUpdated();
}
```

Implementa `com.tridium.crypto.core.io.TrustAnchorProvider` — el scheme ES su propio provider de trust anchors. Los certs CA confiables para validar client certs se configuran en carpeta del scheme, NO en user trust store global.

### 30.5.2 Cert-to-user mapping

`BClientCertAuthenticator` (attached a cada BUser) almacena el **cert público esperado** del cliente. Match por:
1. Cert chain se valida contra `trustAnchors` (CAs del scheme).
2. Extract `Subject.CommonName` o custom SAN.
3. Lookup BUser cuyo `clientCertAuthenticator` almacena cert que matchea.

**NO hay mapping por attribute LDAP-style** — cada BUser tiene su propio pinned cert. Operacional: genera cert per-user, importa cada uno al BUser correspondiente. No escala a flotas grandes sin automation.

### 30.5.3 CRL/OCSP checking — confirmado NO validado

Bloque 27.3 afirma: "OCSP/CRL NO validados". **Verificación empírica en `ClientCertAuthUtils` decompile**: sin llamadas a `PKIXRevocationChecker` ni `X509CertSelector.setRevocationChecker()`.

**Gap compliance**: FIPS 140-2 CMVP recomienda revocation check obligatorio. Cert revocado sigue autenticando hasta que se remueve manualmente del BUser.ClientCertAuthenticator.

### 30.5.4 Self-signed client certs

El scheme aceptará cualquier cert cuyo chain termine en un TrustAnchor configurado. Si se agrega un self-signed cert como trust anchor, firmará su propio cert → validación pasa. **Comportamiento default = acepta self-signed** si admin los agrega. No hay flag "disallow-self-signed".

---

## 30.6 FIPS 140-2 workflow operacional

### 30.6.1 Providers FIPS en `bin/ext/bcfips/`

Verificado filesystem:
```
bc-bcfkswrapprov-1.0.0.jar    + .sig
bc-fips-1.0.2.5.jar            + .sig
bcpkix-fips-1.0.7.jar          + .sig
bctls-fips-1.0.19.jar          + .sig
```

4 JARs BC FIPS + 4 firmas. Versiones:
- **bc-fips 1.0.2.5** — core BouncyCastle FIPS provider (CMVP cert #3514 según tabla NIST).
- **bcpkix-fips 1.0.7** — X.509/CMS/OCSP/TSP PKIX builders.
- **bctls-fips 1.0.19** — TLS 1.2/1.3 JSSE con FIPS ciphers.
- **bc-bcfkswrapprov 1.0.0** — wrapper BCFKS keystore.

### 30.6.2 `moduleVerificationMode` en `system.properties`

Verificado en defaults:
```properties
jdk.tls.rejectClientInitiatedRenegotiation=true
niagara.moduleVerificationMode=low
```

**3 modes** (Bloque 17.6):
- `low` — 3 grupos siempre firmados (ACCESS_CLASS + REFLECTION + MBEAN), resto opcional.
- `medium` — todos los grupos con permisos requieren firma.
- `high` — todos los módulos firmados obligatorio, cero bypass.

**FIPS mode = medium o high** mínimo. `low` NO es aceptable en compliance FIPS.

### 30.6.3 BCFKS keystore — tipo obligatorio FIPS

Todos los keystores en `/home/cristian/Niagara4.14/OptimizerSupervisor/security/` son `.jceks` (Java KeyStore JCE) default.

**Para FIPS hay que migrarlos a BCFKS** (BouncyCastle FIPS KeyStore):
- JCEKS **NO es FIPS-approved** — usa MAC con PBEWithMD5AndTripleDES no-FIPS.
- BCFKS usa PBKDF2-HMAC-SHA512 + AES-256 wrap — FIPS-approved.

**Comando migration** (documentado en guides/):
```bash
keytool -importkeystore \
  -srckeystore keystore.jceks -srcstoretype JCEKS \
  -destkeystore keystore.bcfks -deststoretype BCFKS \
  -providername BCFIPS -providerpath bin/ext/bcfips/bc-fips-1.0.2.5.jar \
  -providerclass org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider
```

### 30.6.4 `java.security` edits requeridos

Edición de `bin/jre/lib/security/java.security` (parte de bin/ — Bloque 17):

```properties
security.provider.1=org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider C:HYBRID;ENABLE{All};
security.provider.2=org.bouncycastle.jsse.provider.BouncyCastleJsseProvider fips:BCFIPS
security.provider.3=SUN
# ... resto providers removidos en FIPS strict
```

**Gotcha**: esto viola firma PKCS7 de `bin/policy/` archivos (Bloque 3.2) si se modifica también. Niagara mantiene `java.security` fuera del PKCS7-signed set — editable.

### 30.6.5 Cipher suite restrictions FIPS

Defaults system.properties expone:
```properties
#cipherSuite.exclude.patterns=
```
Comentado default. En FIPS mode, se habilita con patrones:
```properties
cipherSuite.exclude.patterns=RC4,DES,3DES,MD5,EXPORT,anon,NULL
```

**Cipher suites permitidos FIPS**: AES-GCM + SHA256/384, ECDHE + ECDSA/RSA. TLS 1.2+ obligatorio (TLS 1.0/1.1 rechazados).

### 30.6.6 Workflow migrar non-FIPS → FIPS (10 pasos)

1. **Inventario pre-FIPS**: listar módulos instalados, identificar unsigned (nverify.exe todos los JARs).
2. **Obtener módulos firmados** por Honeywell con cert FIPS-compliant (no el default year-9999).
3. **Backup** keystores JCEKS + master keyring `.km/.kr` + `exemptions.tes`.
4. **Migrate keystores** JCEKS → BCFKS (keytool commando 30.6.3).
5. **Edit `java.security`** — reorder providers, BCFIPS primero.
6. **Set `moduleVerificationMode=medium`** (o `high`).
7. **Edit `system.properties`** — `cipherSuite.exclude.patterns=...` + jdk.tls restricciones.
8. **Restart station** — fase startup corre self-tests del provider (BouncyCastleFipsProvider health check al load).
9. **Verify**: `CryptoPlatformPage` (Platform view) muestra providers loaded — BCFIPS debe aparecer primero.
10. **Re-encrypt BPasswords**: cambiar `BPassword` encoder a `BPbkdf2HmacSha256PasswordEncoder` (FIPS-approved) — edit BOG o action `Upgrade All Passwords`.

**Gotcha #11** (por eso el playbook es 10 pasos + mandatorio): módulos pre-FIPS (built against non-FIPS BC std 1.70) **pueden no cargar** si usan APIs no-FIPS (`MD5`, `RC4`, JCEKS KeyStore class directo). Revisar `system.log` post-restart.

---

## 30.7 Master keyring `.km` / `.kr` — hallazgo corregido

### 30.7.1 Ubicación REAL verificada

**El archivo `master.jceks` NO existe en la distribución**. Search empírico: `find /home/cristian/ -name "master.jceks"` → vacío.

**Lo que sí existe** en `/home/cristian/Niagara4.14/OptimizerSupervisor/security/`:
```
.km   262 bytes   — Master key (DPAPI-encrypted en Windows, filesystem-encrypted Linux/WSL)
.kr   1593 bytes  — Key ring (Java serialized v5, encrypted contents)
cacerts.jceks    3232 bytes — CA trust store (Bloque 27)
keystore.jceks  14346 bytes — User keystore
untrusted.jceks    32 bytes — Quarantine store (Bloque 27.4)
exemptions.tes  16534 bytes — user-level signing bypass (Bloque 18.9)
signing/                    — carpeta signing certs (Bloque 26.9)
```

**Header `.km`**: `01 00 00 00 d0 8c 9d df 01 15 d1 11 8c 7a 00 c0 4f c2 97 eb` — **magic DPAPI Windows** (`d08c9ddf-...`). En WSL/Linux, el header es idéntico (blob portable). Esto confirma que la master key está protegida por el user del OS — mover `.km` a otra máquina Windows sin importar el user's DPAPI key → `.km` inservible.

**`.kr`**: Java serialized blob. Contiene entries (aliases → encrypted key material). Key de cada entry viene del `.km` master.

### 30.7.2 Corrección Bloque 13.2.4

Bloque 13.2.4 decía "`master.jceks` no accesible → BPassword reversibles empty silencioso".

**Corrección empírica**: el master keyring son los archivos `.km/.kr`. El keystore `.jceks` almacena certs + private keys de TLS/signing, NO es el "master".

**Restatement**:
- `.km` no legible (permission denied, DPAPI key inaccesible, filesystem corrupt) → keyring no puede decrypt → **BPassword reversibles retornan null en `getValue()` silencioso** (confirmado mirando `BPassword.getValue() throws SecurityException` pero captura interna silencia).
- Debugging opaco: solo aparece como "null password" en logs, sin indicar keyring issue.

### 30.7.3 Qué contiene `.kr` (keyring)

Por inspección de decompile `BPassword`:
- Encoders keyed por alias — `BAliasedAes256CbcPasswordEncoder`, `BAliasedAes256PasswordEncoder`.
- Cada alias es un AES-256 key wrappeado.

Entries típicos:
- `defaultPassword` — key para BPassword reversible (Bloque 5.2.2).
- `niagaraKey` — key para slots sensitive cifrados en BOG.
- Aliases custom generados por módulos (LDAP connection password, Kerberos station password).

**Algorithm**: **AES-256-CBC** verificado por `BAes256CbcPasswordEncoder` class + `ALGORITHM_BUNDLE` constant. Soporta también AES-256-GCM (`BAes256PasswordEncoder` sin CBC sufijo).

### 30.7.4 Key rotation — NO automática

No hay slot "masterKeyExpiration" ni action "RotateMasterKey" en `BCertManagerService`. Rotación es **manual**:

**Procedure empírico** (sin documentación oficial — inferido de defaults + filesystem):

1. **Stop station**.
2. **Backup** `.km`, `.kr`, + todos los BOGs (contienen BPasswords encrypted).
3. **Export** BPasswords reversibles decodeados — tool custom via `BPassword.getValue()` antes de stop.
4. **Delete** `.km` + `.kr` del `security/`.
5. **Restart station** — Niagara genera nuevos `.km`/`.kr` automático en primera escritura de BPassword (lazy init).
6. **Re-enter** BPasswords manualmente via Workbench — se encriptan con nuevo key.
7. **Verify**: exportar, decodear, comparar.
8. **Archive old** `.km`/`.kr` por N días (compliance retention — típico 90).

**Gotcha crítico #7.1**: si se copia `.km`/`.kr` a OTRA máquina Windows sin exportar/re-entrar BPasswords, DPAPI de nuevo user NO decrypta → station post-restore tiene BPasswords vacías silent.

**Gotcha crítico #7.2**: `backup.dist` **NO incluye `.km`/`.kr`** por diseño (son user-profile specific). `Station Copier` tampoco. Restoring backup en máquina destino sin pre-provisión de keyring → cascade silent failures de auth/encryption.

### 30.7.5 Sensitivity matrix

| Dato sensitive | Storage | Key source | Impacto si keyring pierde |
|----------------|---------|-----------|-------------------------|
| BUser passwords (hashed) | `users/*` BOG (hash SHA-256 PBKDF2) | Hash → no reversible | Sin impacto (no se desencripta) |
| LDAP connection password | scheme BOG (`BPassword` reversible) | `.kr` alias | Reauth manual post-rotation |
| Kerberos station password | scheme BOG (`BPassword` reversible) | `.kr` alias | Reauth manual post-rotation |
| SAML server cert private key password | scheme `BCertificateAliasAndPassword` | `.kr` alias | Recrear cert |
| Mail server password | MailService BOG | `.kr` alias | Reauth manual |
| DB connection passwords (rdb driver) | DB config BOG | `.kr` alias | Reauth manual |
| TLS private keys (cert) | `keystore.jceks` (JCEKS own password) | jceks-internal | `.kr` no afecta directamente |
| Audit records | `histories/audit.adb` | Plain (no encryption at rest) | — |
| BOG files (plaintext XML) | stations/*/config.bog | — | BPassword slots retornan null |

---

## 30.8 TLS cert rotation end-to-end

### 30.8.1 Certs a rotar

Expandiendo Bloque 27.7 — tipos cert rotables:

| Cert | Endpoint | Keystore | Alias |
|------|----------|----------|-------|
| SSL HTTPS (Jetty) | 80/443 | keystore.jceks | `tridium` default |
| FoxS | 4911 | keystore.jceks | `tridium` (reusa default o alias separado) |
| Platform daemon | 5011 | keystore.jceks daemon home (diferente user home) | `tridium` |
| BACnet/SC | 49152 | keystore.jceks | alias custom |
| NiagaraNetwork federation | Fox 4911 Sub→Super | keystore.jceks en cada station | cross-signed |

### 30.8.2 Zero-downtime rotation recipe

Procedure testeado (guides/):

1. **Generate new cert** con `Cert Manager` view → `New` → 2048-bit RSA + SHA-256 + expire 2 years. **NO replace default ni delete old cert yet**.
2. **Export CSR** → envío a CA corporativa.
3. **Import signed cert** como nuevo alias `tridium-2026` (distinto de `tridium` existente).
4. **Trust chain propagation**: export nueva root CA → import a **todos** los Supervisors + Workbenches + Subordinate stations (User Trust Store).
5. **Switchover atomic**:
   - `BWebService.setDefaultTlsCert(alias="tridium-2026")` via Workbench o action.
   - Cambio es hot — Jetty `SslContextFactory` reload sin restart.
6. **Fox server** (BFoxService): `setFoxsCert(alias="tridium-2026")` — también hot reload (Bloque 27.4).
7. **Verify all endpoints re-handshake**: forzar conexión nueva de Workbench → cert chain fetch muestra nueva firma.
8. **Revoke old** cert via CRL publishing (CA side) **O** simplemente eliminar alias viejo.

**Gotcha #8.1**: Bloque 27 reveló OCSP/CRL NO validados por Niagara clients. Revocación CRL solo tiene efecto sobre clients externos (browsers modernos). Niagara-to-Niagara ignora CRL — requiere **remove alias viejo** explicit.

**Gotcha #8.2**: BACnet/SC requiere cert separate en keystore. Rotación independiente (no hot reload — requiere restart del `BBacnetNetwork`).

### 30.8.3 Cross-sign durante transition

Para windows donde coexisten versiones con cert viejo + nuevo:
- Generar nuevo cert firmado por **ambas** CAs (transitional root → new root).
- Durante migration, servers presentan chain con intermediate de ambas — clients validan con la que tienen.
- Post-migration, retirar chain transitional.

Niagara no tiene helper automatizado — manual con keytool.

---

## 30.9 Token lifecycle cross-token matrix

Tabla consolidada de **todos los artifacts de sesión/token** y su ciclo:

| Token/Artifact | Emitter | Consumer | Lifetime | Renewal | Revocation | Storage |
|----------------|---------|----------|----------|---------|------------|---------|
| `NiagaraSuperSession.id` | SessionManager (SecureRandom) | web + fox combined | `logoffPeriod` idle (default 15m) | activity resets timer | `invalidate()` via logout | in-memory Map<String,NSuperSession> (SessionManager static) |
| `NiagaraSuperSession.csrfToken` | `CSRF_TOKEN_BIT_LEN` SecureRandom | Every POST | session lifetime | post-auth regenerate | session invalidate | NSuperSession field |
| Fox session token | BFoxService handshake | Workbench↔Station, Station↔Sub | 24h (Bloque 13.2.3) | silent re-auth cert-based | logout/expire | memory |
| BOX session / `NiagaraSession` subclass | BoxWebSocketServlet | BajaScript browser | Piggyback on NSuperSession | — | — | `NSuperSession.sessions` Set |
| Web HttpSession (Jetty) | Jetty SessionHandler | HTTP requests | 15 min idle + maxLife (config) | activity requests | logout | cookie `JSESSIONID` + server |
| `x-niagara-csrfToken` header | same as CSRF field | Every non-GET | session lifetime | re-issued on regen | session invalidate | HTTP header, client stored JS |
| `Ntoken` (one-time) | SessionManager | Specific URI pattern | `NTOKEN_TIME` (static) + `NTOKEN_USAGE` count | NO renewal | explicit `invalidate()` or usage depleted | `SessionManager.ntokens` Map |
| SAML assertion | IdP | Niagara RP | IdP-dictated (typical 1h) | IdP policy | SLO (NO implementado en Niagara) | consumed, not stored |
| SAML UUID (AuthnRequest) | Niagara RP | Niagara RP reply match | `timeSkew` window (3 min default) | — | CleanupTask expires | `SAMLUuidMap` static |
| OAuth access token (client role) | External IdP | Niagara outbound to API | Provider-dictated | refresh_token flow | explicit revocation endpoint | in-memory cache Nimbus SDK |
| LDAP bind (transient) | local | local per-request | per-request, no persist | N/A | N/A | not stored |
| LDAP cached credentials | `authCachedCredentials` internal | fallback | `cacheExpiration` (BRelTime slot) | last-success auth | restart or expire | encrypted via keyring |
| Kerberos ticket (station TGT) | KDC | Niagara as service | Per KDC policy (10h typical) | `subjectExpiration` proactive renewal | KDC revoke | `BKerberosConfig.subject` memory |
| Keytab file | filesystem | Kerberos auth | Principal password-change expiry | manual rekey | manual + `encryptKeytabs()` | `!config/keytab/`, encrypted at rest |
| mTLS cert session | Jetty SSL session | HTTPS sessions | Cert's `notAfter` expiry | cert rotation | CRL (NOT validated Niagara-internal) | TLS session cache |
| `.km` master keyring | install (lazy init) | BPassword decrypt | N/A static | manual rotation (Bloque 30.7.4) | delete file | `!user_home/security/.km` |
| `.kr` key ring | same `.km` | BPassword decrypt alias | N/A static | manual | delete file | `!user_home/security/.kr` |
| `.sig` module signing cert | Honeywell hardcoded (Bloque 18) | Module load verify | year 9999 eternal | N/A | N/A | `signing.properties` / module `META-INF/` |
| User trust store aliases | keytool / Cert Manager | Cert chain validation | imported cert `notAfter` | manual | manual `delete-alias` | `security/keystore.jceks` + cacerts.jceks |
| Session regen post-auth | SessionManager.changeSessionId | Clients | Atomic on auth | — | invalidate old | superId map update |

### 30.9.1 Ntoken — el token menos conocido

`com.tridium.session.Ntoken`:

```java
public final class Ntoken {
  private String sessionID;
  private volatile boolean valid;
  private final long created;
  private static final long NTOKEN_TIME;         // default TBD
  private static final long DEFAULT_NTOKEN_TIME;
  private static final long MAX_NTOKEN_TIME;
  private static final long MIN_NTOKEN_TIME;
  private volatile int used;
  private static final int NTOKEN_USAGE;
  private static final int DEFAULT_NTOKEN_USAGE;
  // ...
  private static final List<Pattern> validUris;
}
```

**Ntoken es one-time-use token** asociado a un sessionID + URI pattern. Genérico para operaciones puntuales (ej. file download con URL firmada, export con URL efímera). `used` counter se incrementa cada `use(uri)` — expira si supera `NTOKEN_USAGE` o tiempo pasa `NTOKEN_TIME`.

No aparece en Bloques anteriores — **hallazgo empírico de este bloque**.

---

## 30.10 RBAC method-level enforcement

### 30.10.1 Location del check en invocation chain

Bloque 11 describe el modelo role/category/permission. Detalle de enforcement en runtime:

1. **Caller** invoca `BComponent.invoke(action, arg, ctx)` o `component.set(property, value, ctx)`.
2. **`ctx`** es `BContext` — contiene `getUser()` (BUser del caller).
3. **Framework intercept** en `BComponent.invoke`:
   - Lookup slot flags — si `Flags.OPERATOR` presente, check operator permission level; sino admin.
   - Determinar categoryMask del componente (vía `BICategorizable.getCategoryMask()`).
   - Consultar `user.permissions[categoryIndex]` → BPermissions bits (rwi RWI).
   - Verificar bit requerido (INVOKE action = `i` operator / `I` admin).
4. **Falla** → `PermissionException` (decompilado en `javax.baja.security`).

### 30.10.2 Slot-level vs method-level

**Slot-level**: cada `Property` y `Action` tiene `Flags` — `OPERATOR`, `ADMIN`, `HIDDEN`, `READONLY`, etc. El flag decide qué permission bit se consulta.

**Method-level** (Java methods no-slot): **NO intercepted by framework**. Un método Java directo (no action) invocado vía reflection o referencia directa en código de un módulo NO pasa por el permission check.

**Implicación crítica de Bloque 3**: solo código firmado con **REFLECTION permission group** puede hacer reflection. Esto es la **única protección** contra bypass de RBAC via reflection — un módulo no firmado NO puede usar reflection para saltar permission check.

### 30.10.3 `AccessSlotCursor` + `BIProtected`

```java
javax.baja.security.AccessSlotCursor
javax.baja.security.BIProtected
```

**`AccessSlotCursor`**: filtra slot enumeration — un user sin read permission sobre un componente no ve sus slots en cursor iteration (invisible vs visible-but-denied).

**`BIProtected`**: interface marker — si componente implementa esto, cada acceso granular verifica permission.

### 30.10.4 `BAction` vs método Java

| Invocation | Permission check | Intercept |
|------------|------------------|-----------|
| `BAction` (slot annotated `@NiagaraAction`) | Yes — framework wraps | `BComponent.invoke(action, ...)` |
| `BProperty` set/get (slot annotated `@NiagaraProperty`) | Yes | `BComponent.set()` / `get()` |
| Java method directo (no-slot) | No | Direct dispatch |
| Reflection `Method.invoke()` | No | Requires REFLECTION permission group firmado |
| BQL write action | Yes — slot-level check happens | Filtered via `QueryPermissionCheckIterator` |

**Hallazgo**: `com.tridium.query.QueryPermissionCheckIterator` verificado en decompile — BQL queries filtran results por permisos del user antes de retornar. Componente sin `r` bit en categoría → no aparece en result set silent.

### 30.10.5 Silent deny vs throw

- Enumeration (BQL, slot cursor): silent filter — no aparece.
- Explicit set/invoke: `PermissionException` throw.
- Read de property con `w` pero no `r`: returns default value silently.

---

## 30.11 Auditor en invocation flow

### 30.11.1 `Auditor` interface — decompilado

```java
public interface javax.baja.security.Auditor {
  public abstract void audit(AuditEvent);
}

public interface javax.baja.security.SecurityAuditor {
  public abstract void audit(SecurityAuditEvent);
}
```

Dos interfaces distintas: **`Auditor`** (operaciones normales) y **`SecurityAuditor`** (eventos security-specific — logins, auth failures).

### 30.11.2 `AuditEvent` — operations constants

```java
public class AuditEvent {
  public static final String CHANGED;         // property changed
  public static final String ADDED;           // slot added
  public static final String REMOVED;         // slot removed
  public static final String RENAMED;
  public static final String REORDERED;
  public static final String FLAGS_CHANGED;
  public static final String FACETS_CHANGED;
  public static final String RECATEGORIZED;   // category mask change
  public static final String INVOKED;         // action invoked
  public static final String LOGIN;           // (SecurityAuditEvent subset)
  public static final String LOGOUT;
  public static final String LOGIN_FAILURE;
  public static final String TIMEOUT;
  private String operation, target, slotName, oldValue, value, userName;
  private BAbsTime timestamp;
}
```

13 operation constants — **LOGIN/LOGOUT/LOGIN_FAILURE/TIMEOUT son security-specific** (instance de `SecurityAuditEvent`).

### 30.11.3 `BAuditHistoryService` — sink

```java
public class BAuditHistoryService extends BAbstractAuditHistorySource 
    implements BIService, Auditor, BIHistorySource, BIRestrictedComponent {
  public static final Property historyConfig;
  public static final Property SecurityAuditHistorySource;
  public void audit(AuditEvent);
  private static boolean checkSecurityAudit(AuditEvent);
}
```

**Dos history sources en uno**:
- `BAuditHistoryService` itself — eventos generales (CHANGED, ADDED, INVOKED, etc.).
- `SecurityAuditHistorySource` slot — eventos security (LOGIN, LOGOUT, LOGIN_FAILURE, TIMEOUT).

`checkSecurityAudit(event)` decide cuál sink recibe el event — **split por operation constant**.

### 30.11.4 Sync vs async

**Hallazgo**: `audit(AuditEvent)` es **synchronous** — no hay queue explícito ni `Future`. Cada invocation de component método que cambia state llama `auditor.audit(event)` **inline**.

**Implicación performance**: si `BAuditHistoryService` está escribiendo a history DB (SQLite audit.adb), el callback se bloquea hasta el write complete. 

**Confirmación indirecta**: `protected void auditStarted()` / `auditStopped()` lifecycle + `synchronized void stopAudit()` — el método `stopAudit` es synchronized → operations en flight durante stop se bloquean.

**Gotcha performance**: en bulk change operations (ej. BatchEditor editando 1000 slots), se generan 1000 audit calls síncronos. Si audit DB es slow, bulk edit se demora.

### 30.11.5 Async path — SyslogAuditHandler

```java
com.tridium.syslog.SyslogAuditHandler
com.tridium.syslog.AuditAdapter
```

`BAuditHistoryService.syslogAuditHandler` es static — permite reenvío async a syslog externo (UDP 514). Syslog es fire-and-forget, sin ack → SÍ es async, pero independiente del audit principal que es sync.

### 30.11.6 Audit record persistence

`BAuditRecord` implementa `ITruncatable`:
```java
public boolean isFixedSize();
protected void doRead(DataInput);
protected void doWrite(DataOutput);
public boolean truncate(int);
```

Stored en **history DB archive** (`.adb` SQLite) — no es un log file txt. Retention:

- **`BAbstractAuditHistorySource.enabled`** bool — enable/disable.
- **`historyConfig`** slot (BHistoryConfig) — capacity, rolling, retention rules.
- **Default Bloque 20.8.5**: sin auto-delete. Rolls over per config.

### 30.11.7 Privacy / redaction de password values

Verificando en decompile de `BPassword.toString(Context)`: retorna `PLACEHOLDER_TEXT` static (literal typ "******"), **NO el valor real**.

Audit record llama `toString()` del value → si el slot era `BPassword`, el old/new value en audit record = `"******"`. **NO hay leak de password en audit**.

**Edge case**: slot no-BPassword que almacena string sensitive (token, API key) sin usar `BPassword` type → audit registra el valor en texto plano. Admin debe usar `BPassword` explícito para sensitive fields.

---

## 30.12 Permission propagation en federation

### 30.12.1 Supervisor → Subordinate invocation

Cuando Supervisor invoca operation en Subordinate (via NiagaraDriver — Bloque 19.11):

1. Supervisor tiene BUser local `super_admin` con roles.
2. Supervisor conecta a Sub vía Fox con BUser `supervisor_service` (**cuenta en Sub, separada**).
3. Sub valida Fox auth → identity del caller = `supervisor_service`.
4. Permission check en Sub usa **`supervisor_service.permissions`** NO `super_admin.permissions`.

**Implicación**: **NO hay delegation automática**. Cada station tiene su propia user space. Supervisor autoriza user local → Sub autoriza con service account local. **Identity se traduce en el boundary**.

### 30.12.2 Cross-station BRole mapping — no automático

Bloque 13.1 menciona NiagaraNetwork federation. Mapping roles cross-station:
- **Manual**: admin crea mismo nombre de role en cada Sub.
- **Sin sync automático** entre stations — si cambia role permissions en Supervisor, no se propaga a Subs.

### 30.12.3 Audit cross-station

Invocación Supervisor → Sub queda en audit de Sub como `supervisor_service` — **NO queda** identidad del user original `super_admin`. Audit gap: no se puede trazar cadena end-user → federated action sin correlation externa (ej. timestamp matching).

**Workaround enterprise**: custom servicio que propaga user name en header custom Fox + logs en ambos lados con correlation ID.

### 30.12.4 Break-glass local admin

Todas las stations SIEMPRE tienen BAdminRole hardcoded (Bloque 11.1.7). Aunque LDAP/SAML se caigan, local admin BUser sigue valiendo vía `BPasswordAuthenticationScheme` (Digest) siempre presente.

**Gotcha compliance**: break-glass admin password debe tener rotación — muchas instalaciones dejan "admin/admin" inicial + no rotan.

---

## 30.13 Session fixation + lifecycle deep

### 30.13.1 Session ID regeneration post-auth

Bloque 29.5 afirmó "session regeneration post-auth". **Verificación empírica en `SessionManager`**:

```java
public static synchronized boolean changeSessionId(MutableNiagaraSession, Class<?>, String);
public static synchronized void changeSuperSessionId(NiagaraSession, String);
```

Existe método explícito `changeSuperSessionId` — se llama post-`setAuthenticated`. **Confirmado session fixation protection presente**.

### 30.13.2 `NiagaraSuperSession` agregador

```java
public class NiagaraSuperSession implements INiagaraSuperSession {
  private String sessionId;
  private String csrfToken;
  private Set<NiagaraSession> sessions;      // múltiples HttpSession/FoxSession agregados
  private NiagaraSession masterSession;
  private AuthenticationInfo authnInfo;
  private volatile long logoffPeriod;
  long lastActivity;
  private final Set<Object> pauseSet;
  private static final ScheduledExecutorService executor;
}
```

**`sessions` es `Set<NiagaraSession>`** — agregador de N sesiones (HTTP + Fox + Box) bajo UNA identity. Esto es la "superId" — Bloque 29 lo mencionó. Corrobora: un user con browser + Workbench + BajaScript = UNA NSuperSession + 3 NSession child.

### 30.13.3 Concurrent session limit

`SessionManager.checkConcurrentSession(NSuperSession)` — verificado. **Pero**: no hay slot obvio para configurar max concurrent. Revisando `BUserService`/`BWebService` defaults, tampoco aparece.

**Hallazgo**: `private static Map<BUser, NSuperSession> byUser` — **ONE superSession per BUser** como invariant interno. Si user logueado en browser intenta segunda login, la primera se invalida (no hay dos paralelos).

**Corrección Bloque 11.3.4**: no es "sin límite" — es "**1 por BUser**". Browser 1 + Browser 2 del mismo user → segundo login patea al primero. Múltiples Workbench del mismo user = igual, uno activo a la vez.

**Escape**: users distintos (admin, admin2 con mismos roles) pueden paralelizar.

### 30.13.4 Session timeout multi-server

Bloque 20.10 gap #25 — clock skew entre Supervisor + Sub causa session timeout inconsistente.

**No investigable empíricamente en lab single-host**. Riesgo documentado:
- Super timeout 15min, Sub timeout 15min, clock skew +2min → Fox session Sub expira 2 min antes → re-handshake silent (o falla si cert-based renewal falla).
- NTP obligatorio (confirmado Bloque 24.20 para Schedule, aplica igual aquí).

### 30.13.5 Session storage

`SessionManager.sessions` es `Map<String, NSuperSession>` **static** — puro in-memory. **NO persisted**.

**Implicación Bloque 29.5**: `cacheSessionsAndRestart` action preserva via serialización temporal — método `cacheSessionAuthenticationInfo` verificado en decompile. No es persist-to-disk permanente.

---

## 30.14 Enterprise auth fallback + resiliency

### 30.14.1 Primary scheme fail → fallback chain

Scheme chain: `BAuthenticationService` itera schemes en carpeta `AuthenticationSchemes`. Cada `BUser.authenticator` apunta a UN scheme.

**NO hay fallback multi-scheme automático**. Si user asignado a SAML scheme y SAML IdP down → login falla. Admin debe cambiar manual user a Digest scheme o dejar break-glass admin local.

**Excepción**: LDAP + Kerberos tienen `authCachedCredentials` (30.1.9, 30.4.4) — fallback interno al scheme, no a otro scheme.

### 30.14.2 Circuit breaker — no implementado

LDAP/SAML outage → cada request espera `connectionTimeout` (default varios segundos) y falla. No hay circuit breaker que "fail fast" tras N failures consecutivos.

**Impacto**: LDAP down + 100 users intentando login simultáneo = 100 threads bloqueados durante timeout → Jetty thread pool exhaust → web UI se cuelga para todos.

**Mitigación admin**: `cacheExpiration` en LDAP + conn timeout bajo (p.ej. 2s) minimiza impacto pero no lo elimina.

---

## 30.15 Gotchas producción + incidents

1. **Master keyring `.km` corrupto silent**: BPassword reversibles retornan null `getValue()`, auth LDAP/SAML fallan sin error visible — buscar en `system.log` `SecurityException: MissingEncodingKey`.
2. **FIPS migration rompe módulos pre-FIPS**: built contra BC std → no cargan contra BCFIPS. Revisar `systemMonitor` post-restart.
3. **LDAP group mapping NO refresh** hasta user logout + re-login — cambio de grupos en AD no se refleja en sesión activa.
4. **SAML clock skew causa rejection silent**: `NotOnOrAfter` violation logged level DEBUG (por default no visible).
5. **Session fixation** si reverse proxy termina TLS SIN pasar `X-Forwarded-*` — Niagara puede pensar que vienes del mismo origin y reusar session — requiere `org.bouncycastle.jsse.client.assumeOriginalHostName=true`.
6. **Keytab re-key invalida tickets activos** — refresh requiere restart del scheme, no hot.
7. **mTLS self-signed cert aceptado silent** si admin agregó como TrustAnchor (no flag para bloquear).
8. **Audit sync → degradación**: bulk operations con 1000+ changes bloquean engine thread proporcionalmente.
9. **`.km`/`.kr` NO incluidos en `backup.dist`** — restore en máquina nueva tiene station booting con keyring nuevo → BPasswords vacías silent.
10. **OAuth2 module es cliente M2M, NO provee auth scheme** — expectativa errónea común "integrar Azure AD como OAuth" = imposible nativo, requiere SAML.
11. **OIDC NO soportado nativo** — ni como scheme ni proveyendo tokens.
12. **OCSP/CRL NO validados** en TLS handshakes (Bloque 27.3 confirmado), client certs revocados no bloqueados automáticamente.
13. **SLO SAML NO implementado** — logout en Niagara no propaga a IdP ni otros SPs, user sigue logged-in en IdP.
14. **Niagara IdP es feature poco documentada** — `BSAMLIdPService` existe pero docs públicas apenas mencionan — configurarlo requiere decompile + trial-error.
15. **1 concurrent session per BUser** (corrección 11.3.4) — diseño invariant, no configurable.
16. **Supervisor→Sub auth NO propaga identity** — audit gap federation.
17. **Master key rotation manual, no help ni action** — procedure empírico, error-prone.
18. **BACnet/SC cert rotation requiere restart del BBacnetNetwork** (no hot como Jetty/Fox).
19. **Kerberos realm único** (config `BKerberosConfig.realm`) — multi-realm requiere custom krb5.conf con capaths.
20. **Fallback multi-scheme NO automático** — outage IdP SAML → users SAML bloqueados hasta admin re-asigne manualmente.

---

## 30.16 Mental model — enterprise auth flow end-to-end

**Scenario**: user corporativo `jose@corp.com` accede dashboard Niagara vía SAML federado con IdP Azure AD.

### Fase 1 — Initial request

1. Browser → `https://station.corp.com/` sin cookie session.
2. Jetty filter chain (Bloque 29.3 — 15 capas):
   - Layer 1: `ConnectionLimit` / `SizeLimit` (off default).
   - Layer 4: `UserActivityFilter` — no session activa, skip.
   - Layer 6: `LoginServlet` redirige a scheme chooser → user selecciona "SSO Corp".
   - Layer 7: `CsrfProtectedFilter` — no aplica aún (no session).

### Fase 2 — SAML AuthnRequest (SP-initiated)

3. Scheme `BSAMLAuthenticationScheme.getLoginRedirectURL()` genera URL `/saml/samlrp/authnRequest?scheme=CorpSSO`.
4. `SAMLRPServlet`:
   - Construye `AuthnRequest` XML.
   - Firma con `samlServerCert` (lookup en `keystore.jceks` via `samlServerCertAliasAndPassword` → decrypt password via `.kr` keyring → decrypt via `.km` master → DPAPI).
   - Genera UUID, agrega a `SAMLUuidMap`.
   - Base64 encode + redirect 302 a `https://login.microsoftonline.com/corp/saml2?SAMLRequest=...`.

### Fase 3 — IdP authenticates

5. Azure AD recibe AuthnRequest, valida signature contra cert Niagara pre-registrado en app.
6. Prompt user → password + MFA (Azure controla; policy risk-based).
7. Azure genera SAML Response:
   - `<Assertion>` firmado con cert Azure.
   - Attributes: `email`, `name`, `memberOf` (groups), `employeeId`.
   - `NotOnOrAfter` = now + 1h.
8. Azure POST form-encoded `SAMLResponse` → ACS URL `https://station.corp.com/saml/samlrp/consumer?scheme=CorpSSO`.

### Fase 4 — SP consumes assertion

9. `SAMLConsumerServlet` recibe POST:
   - Layer 7 filter chain: CSRF skip (SAML POST es exempted en filter exceptions).
   - Validate XML signature against `idpCert` (cert de Azure almacenado en scheme slot o trust store).
   - Validate timestamp ±`timeSkew` (3 min default).
   - Validate UUID en `SAMLUuidMap` (previene replay) → remove.
   - Extract attributes.
10. `BSAMLAttributeMapper` aplica mappings:
    - `email` → `BUser.email`.
    - `name` → `BUser.fullName`.
    - `memberOf` → mapea a attribute que apunta a prototype user en Niagara → clona roles.
11. `SAMLLoginModule.login()` JAAS completa → Subject autenticado.

### Fase 5 — Session establishment

12. `SessionManager.createSession(remoteHost)` genera `NSuperSession`:
    - `sessionId` = SecureRandom 128-bit base64.
    - `csrfToken` = SecureRandom `CSRF_TOKEN_BIT_LEN` bits.
    - `changeSuperSessionId(...)` — regenera ID para anti-fixation.
    - `setAuthenticated(subject)` — attach BUser.
13. Cookie `JSESSIONID` (Jetty) + Niagara session cookie → browser.
14. Response redirect 302 a `/` (original URL).

### Fase 6 — Authenticated request

15. Browser → `/bql/?query=slot:/Drivers` con cookie.
16. Filter chain:
    - Layer 3: `SessionManager` lookup → NSuperSession found, BUser attached to request.
    - Layer 7: `CsrfProtectedFilter` — GET excluido; POST requiere header `x-niagara-csrfToken` match.
    - Layer 8: `AuthenticationFilter` → OK.
    - Layer 9: `AuthorizationFilter` → (se hace per-operation durante dispatch).
17. Servlet `OrdServlet` / `BqlServlet` dispatch.
18. Dentro de dispatch, query ejecuta:
    - `QueryPermissionCheckIterator` filtra results por categoryMask + permissions.
    - Slots sin `r` operator no aparecen en result silent.
19. Cualquier mutation (write action via `/ord/...invoke`) invoca `BComponent.invoke(action, arg, ctx)`:
    - `ctx.getUser()` = `jose`.
    - Lookup `jose.permissions[categoryIdx]` → BPermissions.
    - Check required bit → OK/PermissionException.
    - Llamada `BAuditHistoryService.audit(AuditEvent(INVOKED, ...))` **sync**.
    - Audit record `BAuditRecord` persisted via `BIHistory`.

### Fase 7 — Session close / timeout

20. User idle 15 min → `NiagaraSuperSession$SessionTimeoutRunnable` dispara:
    - `invalidate(LoginFailureCause.TIMEOUT)`.
    - `SecurityAuditEvent(TIMEOUT, userName=jose)` → audit log.
    - Remove from `SessionManager.sessions`, `byUser`, `sessionIds`.
    - Cookie invalidated.
21. Browser next request → 401, redirect login.
22. **SLO NO fires** — Azure AD sigue con jose logged-in. Otra pestaña de jose hacia Niagara → redirect SAML → Azure sin re-prompt (SSO) → nuevo session en Niagara silent. **Este es el gap SLO del 30.2.7**.

---

## Síntesis del bloque

### Corrección crítica del modelo mental

El "master.jceks" mencionado en Bloques 13.2.4 y 5.2.2 **no existe con ese nombre**. Son **`.km` (master key, DPAPI-encrypted)** + **`.kr` (key ring, Java serialized encrypted)** en `user_home/security/`. Esto cambia el procedure de rotation + backup.

### OAuth2/OIDC gap real, confirmado empírico

`oauth2-rt.jar` es **cliente M2M**, NO provee scheme de auth para users. **OIDC NO está implementado nativo**. Enterprise deployment con Azure AD / Okta requiere **SAML** (no shortcut OIDC).

### Niagara como SAML IdP — feature oculta

`BSAMLIdPService` + `BCircleOfTrustFolder` + `BStationServiceProvider` proveen capability completa de IdP. No se menciona en tabla Bloque 11.3.7 — hallazgo nuevo. Útil para deployments edge sin IdP corporativo (Niagara Supervisor actúa IdP para Subs y apps terceras).

### Audit es sync por design

`BAuditHistoryService.audit()` es synchronous — bulk operations escalan linealmente con audit DB write latency. Syslog forward es async side-channel independiente.

### Session = `NSuperSession` agregador

No es una cookie por browser ni por Fox — una `NSuperSession` **agrega** múltiples `NiagaraSession` child (HTTP + Fox + BOX) bajo la identity del user. Session regen post-auth (fixation safe) confirmado en decompile.

### Concurrent session = 1 per BUser

Invariant de diseño — `Map<BUser, NSuperSession>`. Segundo login patea al primero. NO configurable. Users duplicados (admin, admin2) son el workaround oficial.

### RBAC en métodos Java no-slot: NO enforced

Framework solo intercepta slots (`@NiagaraProperty`, `@NiagaraAction`). Java métodos directos no pasan check. **La única protección es REFLECTION permission group del Bloque 3** — código no firmado no puede hacer reflection → no puede bypassar RBAC.

### Federation audit gap

Supervisor invoca Sub con `supervisor_service` identity — Sub audit no sabe que user original era `jose`. Trazabilidad end-to-end requiere correlation externa (timestamp + logs en ambos lados).

### Fallback auth limitado

LDAP y Kerberos tienen cache fallback interno. SAML NO tiene fallback (si IdP down, users SAML bloqueados). Multi-scheme fallback automático NO existe — break-glass local admin siempre presente.

### `.km`/`.kr` no en backup

Backup online + Station Copier excluyen keyring (user-profile specific). Restore en máquina nueva → BPasswords vacías. Procedure de restore enterprise requiere migración manual de keyring o re-entrar todas las BPasswords.

### Conexiones

- **Bloque 3 (sandbox)**: REFLECTION permission group es el único mecanismo que impide bypass de RBAC via reflection. FIPS mode `moduleVerificationMode=medium/high` cierra ventanas adicionales.
- **Bloque 11 (RBAC base)**: extendido con enforcement location, sync audit, propagation federation.
- **Bloque 13 (keyring)**: corregido `master.jceks` → `.km`/`.kr` DPAPI-encrypted.
- **Bloque 17 (BCFKS)**: 4 BC FIPS JARs en `bin/ext/bcfips/` — providers físicos del FIPS workflow.
- **Bloque 18 (SCRAM, signing, CSRF)**: CSRF token ahora en matriz cross-token; `Ntoken` agregado como token one-time.
- **Bloque 20.10 (gap analysis)**: cierra #10 (federation providers deep), #11 (FIPS workflow), #15 (key rotation), #16 (permission propagation), #17 (audit semantics sync/async).
- **Bloque 27 (trust chain)**: OCSP/CRL gap confirmado empírico en `ClientCertAuthUtils`. Cert rotation end-to-end expandido con hot reload Jetty/Fox vs restart BACnet/SC.
- **Bloque 29 (web auth schemes)**: matriz de 9 schemes contextualizada con bind flows, cache, fallback, SLO gap.

### Qué habilita

Con Bloque 30 consolidado podés:
- Diseñar federated deployment enterprise con SAML + LDAP fallback + break-glass admin + auditing estricto.
- Planificar FIPS migration paso-a-paso con checklist 10 pasos + verify cipher restrictions + BCFKS.
- Ejecutar rotation procedure del master keyring sin perder BPasswords reversibles.
- Rotar TLS certs de TODOS los endpoints sin downtime (Jetty/Fox hot + BACnet/SC restart).
- Explicar por qué OIDC "no funciona" — porque NO está implementado; forzar SAML o custom module.
- Detectar y mitigar gaps compliance: SLO ausente, OCSP no validado, audit federation sin identity propagation, concurrent session invariant 1-per-user.
- Debugging opaco con `.km`/`.kr`: identificar BPassword null silent como symptom de keyring corrupt.

**Próximo**: Bloque 31 — TBD (HA/clustering, transaction semantics, performance tuning, o enterprise provisioning).

---

## Engram topic keys

- `niagara/bloque30/auth-federation-fips-rotation` — TOPIC PRINCIPAL — enterprise federation + FIPS + key rotation + token matrix + RBAC runtime + audit sync + session lifecycle.
- `niagara/auth/ldap-federation-deep` — LDAP v2/v3 + AD + Kerberos + keytab encryption + connection pool + referral + group mapping via prototype.
- `niagara/auth/saml-rp-and-idp` — Niagara como RP Y como IdP, SLO NO implementado, attribute mapping, circle of trust.
- `niagara/auth/oauth-oidc-gap` — oauth2-rt es cliente M2M, NO scheme de auth; OIDC no soportado nativo.
- `niagara/security/master-keyring-km-kr` — corrección `.km`/`.kr` DPAPI vs supuesto `master.jceks`; rotation procedure manual.
- `niagara/fips/migration-workflow-10-steps` — BCFIPS providers + BCFKS + java.security + cipher exclusions + module verification mode.
- `niagara/tls/cert-rotation-zero-downtime` — Jetty/Fox hot reload + BACnet/SC restart + cross-sign transition.
- `niagara/session/supersession-aggregator` — NSuperSession agrega HTTP+Fox+BOX bajo un BUser, 1 concurrent per user invariant, regen post-auth.
- `niagara/audit/sync-semantic` — audit synchronous inline, sink split security vs general, syslog async side-channel.
- `niagara/rbac/method-level-enforcement` — slots interceptados, Java methods NO, reflection protegido por permission group Bloque 3.

---

**Sesión cerrada**: Bloque 30 consolidado — 29 bloques previos + este cierra gaps #10, #11, #15, #16, #17 de Bloque 20.10.
