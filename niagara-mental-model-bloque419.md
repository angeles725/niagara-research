# Bloque 419 — Niagara Network Supervisor (VII): mecanismo de intercambio de clave Fox en canal plain (SRP6 post-SCRAM) — REFUTA el [INFER] de B417

> **Qué documenta**: el gap N7 — mecanismo de intercambio de la clave que cifra el canal `"point"` en
> conexiones Fox sin TLS (`useFoxs=false`). Pregunta núcleo: ¿la clave de sesión del canal es un valor
> efímero protegido por el Problema del Logaritmo Discreto (no recuperable por un interceptor pasivo),
> o es un valor estático/derivado del hello visible en el handshake? Veredicto: **la clave es efímera**
> (SRP6) y **el [INFER] de [Bloque 417] §417.5 queda REFUTADO** — un interceptor pasivo NO puede
> recuperar la clave del canal ni descifrar el payload de credenciales en una sesión Fox plain N4-a-N4.
>
> **Subject version**: Niagara N4 4.14.0.162 · build 2024-05-28
>
> **Fuentes** (decompilado Vineflower; aliases de ruta):
> - `$FOX_SESSION` = `.../fox/fox-rt/vineflower/com/tridium/fox/session/`
> - `$FOX_SYS`     = `.../fox/fox-rt/vineflower/com/tridium/fox/sys/`
> - `$FOX_UTIL`    = `.../fox/fox-rt/vineflower/com/tridium/fox/util/`
> - `$ND_POINT`    = `.../niagaraDriver/niagaraDriver-rt/vineflower/com/tridium/nd/point/`
> - Raíz: `/home/cristian/modules/Prototipos/modulos/organized/`
>
> **Clases leídas**: `FoxSession.java`, `Tuner.java`, `FoxScramShaUtil.java`, `BFoxChannel.java`,
> `BFoxSession.java`, `BPointChannel.java` (6 archivos). Inline directo.
>
> **Método**: lectura directa de las 6 clases + grep de cadenas críticas;
> verificación de número de línea de todos los `[CERT]` de peso antes de escribir.
> Marcadores: `[CERT]` = fuente primaria (file:line); `[INFER]` = deducción.
>
> Seguridad del transporte Fox. Cierra N7. Conecta [Bloque 134], [Bloque 415], [Bloque 417].

---

## 419.1 — Prior coverage: lo que B134 ya documentó (remite, no re-deriva) `[CERT]`

**[Bloque 134] §134.8** documentó el mecanismo SRP6 de Fox en su forma general:
- El `challenge` del servidor anuncia `keyExchangeMethods` + `keyExchangeCiphers` en conexiones
  plaintext no-legacy.
- Tras SCRAM, corre un segundo intercambio (`srp6ClientA`/`srp6ServerB`/`srp6M1`/`srp6M2`) keyed
  por `saltedPassword` como secreto compartido.
- El resultado es una `sessionKey` que se almacena en `FoxSession` y habilita `supportsSecureData()`.

B419 **no re-documenta el wire ni SCRAM** (→ B134 §134.7). B419 pregunta específicamente:
(a) cuándo se activa el SRP6 en una sesión join N4-a-N4 con `useFoxs=false`;
(b) si el secreto que produce el SRP6 puede recuperarse por un interceptor pasivo;
(c) cómo ese secreto se convierte en la clave AES del canal `"point"`.

---

## 419.2 — Condición de activación de SRP6 en Tuner.java `[CERT]`

La condición exacta que decide si el servidor anuncia un key exchange real (SRP6) o `NullAlgorithmBundle`
está en `Tuner.java:559-563`:

```java
if (!this.session.isSecure() && !this.session.isLegacyConnection()) {
    challenge.add("keyExchangeMethods", authnScheme.getKeyExchangeMethodName());
    challenge.add("keyExchangeCiphers", KeyExchange.getPreferredKeyExchangeCiphers());
} else {
    challenge.add("keyExchangeMethods", NullAlgorithmBundle.getInstance().getAlgorithmName());
}
```
`[CERT]` `$FOX_SESSION/Tuner.java:559-563`

| Condición | Resultado |
|---|---|
| `isSecure()=true` (TLS/foxs, 4911) | `NullAlgorithmBundle` — TLS ya provee confidencialidad |
| `isLegacyConnection()=true` (NiagaraAX, fox<1.0.2) | `NullAlgorithmBundle` — AX no soporta SRP6 |
| `isSecure()=false && !isLegacyConnection()` (Fox plain, N4-a-N4) | **SRP6 real ofertado** |

La sesión de join creada por `BSubordinateJoinJob` usa `BFoxSession.make(host, port, useFoxs=false)`,
es N4-a-N4 (no legacy) y no es TLS → **entra en la rama SRP6**. `[CERT]` `$FOX_SYS/BFoxSession.java:116-133`

`isSecure()` se define como `socket instanceof SSLSocket` `[CERT]` `$FOX_SESSION/FoxSession.java:478-479`.
`supportsKeyExchange()` se define como `!isLegacyConnection() && !(keyExchangeAlgorithmBundle instanceof NullAlgorithmBundle)` `[CERT]` `$FOX_SESSION/FoxSession.java:421-422`.

---

## 419.3 — Flujo SRP6 completo en FoxScramShaUtil.java `[CERT]`

### Lado servidor (`handleScramServerCallback`, lines 75-148)

Tras la fase SCRAM (autMessage1/authMessage2):

```
1. keyExchangerServer = KeyExchange.makeServer(session.getKeyExchangeAlgorithmBundle())
2. keyExchangerServer.init()
3. saltedPassword = passwordEncoder.getKey()   // PBKDF2 almacenado en el autenticador del usuario
4. keyExchangerServer.doInitialStep(saltedPassword)  // inicializa SRP6 con el verifier v=g^x mod N
5. Recibe srp6ClientA { keyExchangeClientA: A }   // A = g^a mod N (a = random privado del cliente)
6. B = keyExchangerServer.doExchangeStep(A)       // B = k*v + g^b mod N (b = random privado del servidor)
7. Envía srp6ServerB { keyExchangeServerB: B }
8. Recibe srp6M1 { keyExchangeM1: M1 }            // M1 = prueba del cliente
9. M2 = keyExchangerServer.doExchangeStep(M1)     // verifica M1; produce M2
10. Envía srp6M2 { keyExchangeM2: M2 }
11. sessionKey = keyExchangerServer.getKey()       // clave de sesión derivada de S = (A*v^u)^b mod N
12. session.setSessionKey(sessionKey)
```
`[CERT]` `$FOX_UTIL/FoxScramShaUtil.java:75-148`

### Lado cliente (`handleClientAuthentication`, lines 178-242)

```
1. keyExchangerClient = KeyExchange.makeClient(session.getKeyExchangeAlgorithmBundle())
2. keyExchangerClient.init()
3. saltedPassword = client.getSaltedPassword()   // PBKDF2 ya computado durante SCRAM
4. A = keyExchangerClient.doInitialStep(saltedPassword)  // A = g^a mod N (a random, local)
5. Envía srp6ClientA { keyExchangeClientA: A }
6. Recibe srp6ServerB { keyExchangeServerB: B }
7. M1 = keyExchangerClient.doExchangeStep(B)     // M1 = prueba del cliente; computa S = (B-k*v)^(a+u*x) mod N
8. Envía srp6M1 { keyExchangeM1: M1 }
9. Recibe srp6M2 { keyExchangeM2: M2 }
10. keyExchangerClient.doExchangeStep(M2)         // verifica M2 (prueba del servidor)
11. sessionKey = keyExchangerClient.getKey()
12. session.setSessionKey(sessionKey)
```
`[CERT]` `$FOX_UTIL/FoxScramShaUtil.java:178-242`

### Valores transmitidos en plaintext en Fox frames

| Frame | Contenido visible en TCP |
|---|---|
| `srp6ClientA` | `keyExchange:bool`, `keyExchangeClientA: base64(A)` |
| `srp6ServerB` | `keyExchangeServerB: base64(B)` |
| `srp6M1` | `keyExchangeM1: base64(M1)` |
| `srp6M2` | `keyExchangeM2: base64(M2)` |

`A`, `B`, `M1`, `M2` son **públicos por diseño** en SRP6 — la seguridad no depende de su secreto.
La clave de sesión S (y por tanto `sessionKey`) **nunca se transmite**. `[CERT]` ibídem.

---

## 419.4 — Derivación de la clave AES del canal: de sessionKey a sharedEncodingKey `[CERT]`

Cuando el canal (ej. `BPointChannel`) se abre, `BFoxChannel.initializeSharedKey(session)` corre si
`useSharedKeyEncryption()=true` (§419.5). El flujo es:

```java
// Lado cliente (abre el canal)
byte[] salt = new byte[keySize];
new SecureRandom().nextBytes(salt);                     // salt ALEATORIO, local
sharedEncodingKey = SHA-512(salt || sessionKey)[0:keySize]  // via session.makeSharedSecret(salt)

byte[] iv = new byte[keySize];
new SecureRandom().nextBytes(iv);

// Verifica con mensaje de prueba (AES-encriptado)
byte[] message = Aes256PasswordManager.encrypt(
    "Simplify, then add lightness".getBytes(), iv, sharedEncodingKey, getAesTransformation()
);

// Envía salt, iv, message al servidor por Fox
```
`[CERT]` `$FOX_SYS/BFoxChannel.java:409-439`

```java
// Lado servidor (recibe initializeSharedKey)
byte[] key = session.makeSharedSecret(salt);            // SHA-512(salt || sessionKey)[0:keySize]
sharedEncodingKey = key;
byte[] decryptedMessage = Aes256PasswordManager.decrypt(sharedEncodingKey, message, iv, ...);
// Verifica "Simplify, then add lightness" — comprueba que ambos lados tienen la misma clave
```
`[CERT]` `$FOX_SYS/BFoxChannel.java:475-502`

`makeSharedSecret` en FoxSession:

```java
public byte[] makeSharedSecret(byte[] salt) {
    MessageDigest digest = MessageDigest.getInstance("SHA512");
    digest.update(salt, 0, salt.length);
    digest.update(this.sessionKey, 0, this.sessionKey.length);
    return digest.digest();
}
```
`[CERT]` `$FOX_SESSION/FoxSession.java:449-461`

La transformación AES por defecto es `"AES/GCM/NoPadding"` `[CERT]` `$FOX_SYS/BFoxChannel.java:524-528`.

El `salt` de derivación se **transmite en plaintext** en el mensaje `initializeSharedKey`.
La seguridad de `sharedEncodingKey` depende de la inaccesibilidad de `sessionKey`, no de `salt`.

---

## 419.5 — BPointChannel.useSharedKeyEncryption() = true `[CERT]`

`BPointChannel` sobreescribe explícitamente el método base (que devuelve `false`):

```java
protected final boolean useSharedKeyEncryption() {
    return true;
}
```
`[CERT]` `$ND_POINT/BPointChannel.java:121-123`

Esto confirma (junto con B415 §415.5, que ya lo documentó) que las acciones sobre el canal `"point"`
(incluyendo `joinStation` con el `BConnectInfo` de credenciales) se cifran con `sharedEncodingKey` (AES/GCM).

---

## 419.6 — Análisis de seguridad: ¿puede un interceptor pasivo recuperar la clave? `[INFER]`

### Modelo SRP6 (protocolo estándar)

En SRP6 con el rol de `saltedPassword` como secreto compartido `x`:

- `v = g^x mod N` (verifier, no transmitido)
- `A = g^a mod N` (cliente; `a` privado aleatorio, NUNCA transmitido)
- `B = k*v + g^b mod N` (servidor; `b` privado aleatorio, NUNCA transmitido)
- `u = H(A, B)` (hash público, computable por cualquiera)
- `S_cliente = (B − k*v)^(a + u*x) mod N`
- `S_servidor = (A * v^u)^b mod N`
- Ambos deben coincidir; de `S` se deriva `sessionKey`

### ¿Puede el interceptor pasivo recuperar `sessionKey`?

Un interceptor que captura el tráfico TCP de una sesión Fox plain N4-a-N4 ve:

| Elemento visible | ¿Revelado en TCP? |
|---|---|
| SCRAM: salt + iterations (del servidor) | Sí (authMessage1) |
| SCRAM: username | Sí (frame `username`) |
| SCRAM: clientFirstMessage, clientFinalMessage, serverFinalMessage | Sí (authMessage1/authMessage2) |
| `A = g^a mod N` | Sí (srp6ClientA) |
| `B = k*v + g^b mod N` | Sí (srp6ServerB) |
| `M1`, `M2` (pruebas mutuas) | Sí (srp6M1/M2) |
| `salt` para derivación AES (initializeSharedKey) | Sí |
| `a` (privado del cliente) | **NUNCA** |
| `b` (privado del servidor) | **NUNCA** |
| `sessionKey` | **NUNCA** |
| `sharedEncodingKey` (AES) | **NUNCA** |

Para recuperar `S` (y por tanto `sessionKey`) desde la información pública, el interceptor necesitaría:

- Calcular `a` desde `A = g^a mod N` → **Problema del Logaritmo Discreto (PLD)**
- O calcular `b` desde `B − k*v = g^b mod N` → **PLD** (requiere conocer `v`, que a su vez
  requiere crackear `x = saltedPassword` via PBKDF2)

`[INFER]` Para un interceptor pasivo que no conoce la contraseña:
- El PLD es computacionalmente intratable con parámetros SRP6 estándar
- **La `sessionKey` NO es recuperable del tráfico capturado**
- Por tanto, `sharedEncodingKey = SHA-512(salt || sessionKey)[0:keySize]` tampoco es recuperable
- El payload cifrado con AES (`BConnectInfo` con credenciales) **NO puede descifrarse**

`[INFER]` Para un interceptor pasivo que SÍ conoce la contraseña (ataque offline via SCRAM):
- Puede computar `saltedPassword = PBKDF2(password, salt, iterations)` (salt + iterations visibles en SCRAM)
- Puede computar `v = g^saltedPassword mod N`
- Conoce `A`, `B`, `u=H(A,B)`, `v`
- Para `S = (A * v^u)^b mod N` necesita `b` → PLD irreducible por conocer `v`
- **La sessionKey sigue siendo irrecuperable** aunque se conozca la contraseña

---

## 419.7 — VEREDICTO: refutación del [INFER] de B417 §417.5 `[CERT]` + `[INFER]`

**[Bloque 417] §417.5** planteó el siguiente `[INFER]`:
> "Un interceptor pasivo que capture el handshake Fox (TCP) puede recuperar la clave compartida
> y descifrar el payload del canal `point`, obteniendo el `BConnectInfo` con las credenciales."

Este `[INFER]` queda **REFUTADO** por evidencia de código (§419.2–§419.5):

1. `[CERT]` En conexiones Fox plain N4-a-N4, el servidor anuncia y corre SRP6 (`Tuner.java:559-563`).
2. `[CERT]` SRP6 produce una `sessionKey` cuyas partes privadas (`a`, `b`) NUNCA se transmiten
   (`FoxScramShaUtil.java:105-148, 210-242`).
3. `[CERT]` `sharedEncodingKey` = SHA-512(salt || sessionKey) — `sessionKey` no está en el wire
   (`FoxSession.java:449-461`, `BFoxChannel.java:409-439`).
4. `[CERT]` `BPointChannel.useSharedKeyEncryption()=true` → el payload de `joinStation` está
   cifrado con `sharedEncodingKey` (`BPointChannel.java:121-123`, `BFoxChannel.java:380-399`).
5. `[INFER]` La recuperabilidad del `sessionKey` requeriría resolver el PLD sobre parámetros SRP6,
   considerado computacionalmente intratable.

**Veredicto**: el cifrado de payload del canal `"point"` en plain Fox N4-a-N4 ofrece
**confidencialidad real contra interceptores pasivos**. Un atacante que sólo escucha el TCP no puede
descifrar el `BConnectInfo` con las credenciales de join. `[INFER]` refuta `B417 §417.5`.

### Riesgos residuales (que B419 NO cierra)

| Riesgo | Estado | Observación |
|---|---|---|
| Interceptor **activo** (MITM) | **Abierto** | SRP6 sin PKI no autentica la identidad del servidor; MITM puede impersonar y obtener la clave `[INFER]` |
| Metadatos visibles (username, timing) | Abierto | SCRAM expone username y salt/iterations en plaintext `[CERT]` B134 §134.7 |
| Crackeo offline de password débil | Abierto | PBKDF2 con HMAC-SHA-256 mitiga pero no elimina ataques de diccionario si la password es débil `[INFER]` |
| No hay verificación de identidad del servidor | Abierto | Sin TLS/PKI, SRP6 no autentica el certificado del servidor `[INFER]` |

TLS (`useFoxs=true`) sigue siendo la recomendación de seguridad porque provee autenticación de
servidor por PKI (previene MITM activo) y no depende de la fortaleza de la contraseña para la
confidencialidad del canal de negociación.

---

## 419.8 — Tabla de resumen del mecanismo completo

| Capa | Mecanismo | Algoritmo | ¿Recoverable pasivamente? |
|---|---|---|---|
| Autenticación | SCRAM-SHA-256 | PBKDF2(HMAC-SHA-256, 256-bit) | No — clientProof es HMAC, no expone contraseña |
| Key exchange | SRP6 | Grupo DH (mod N, g) sobre PBKDF2 verifier | No — `a`, `b` privados nunca transmitidos (PLD) |
| Session key | `IKeyExchanger.getKey()` | Derivado de S = (A·v^u)^b mod N | No — requiere `b` (PLD) |
| Canal encoding key | SHA-512(salt || sessionKey)[0:keySize] | SHA-512 truncado | No — depende de sessionKey |
| Cifrado de payload | AES/GCM/NoPadding | AES-GCM | No — depende de sharedEncodingKey |
| Payload cifrado | BConnectInfo (BUsernameAndPassword) | N/A | No descifrable sin la AES key |

---

## 419.9 — Conexiones

- **[Bloque 134] §134.8** — documentó el mecanismo SRP6 en su forma general (keyExchangeMethods,
  srp6ClientA/B/M1/M2, session key). B419 REMITE a B134 para el wire y EXTIENDE con: (a) la condición
  exacta de activación en plain-Fox N4-a-N4 (`Tuner.java:559-563`); (b) la derivación de sharedEncodingKey
  via SHA-512; (c) el análisis de recoverable-pasivamente; (d) el veredicto sobre el [INFER] de B417.
- **[Bloque 415] §415.5** — verificó `BPointChannel.useSharedKeyEncryption()=true`. B419 cierra el ciclo:
  esa clave compartida proviene de SRP6, NO es un valor estático ni recoverable pasivamente.
- **[Bloque 417] §417.5** — planteó el `[INFER]` de recuperabilidad pasiva de la clave del canal.
  **B419 lo REFUTA** con evidencia de código (`Tuner.java:559-563`, `FoxScramShaUtil.java:75-242`,
  `FoxSession.java:449-461`, `BFoxChannel.java:409-439`, `BPointChannel.java:121-123`).
  → `[B417 §417.5 — [INFER] REFUTADO en B419]`
- **[Bloque 266] §266.5** — estableció que `BConnectInfo` se transmite como parámetro de `joinStation`
  sobre el canal Fox. B419 confirma que ese canal está cifrado con AES/GCM derivado de SRP6.
