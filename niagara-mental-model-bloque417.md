# Bloque 417 — Niagara Network Supervisor (V): seguridad del canal de join — credenciales, transporte y permisos

> **Qué documenta**: el gap N4 — la superficie de seguridad del canal de join supervisor↔subordinada.
> Verifica: (a) cómo se almacena y serializa la credencial en `BJoinProfile`/`BConnectInfo`;
> (b) qué protección ofrece el transporte Fox cuando `useFoxs=false`; (c) qué permisos protegen la
> acción de join y el manager UI. Remite a [Bloque 266] §266.5, [Bloque 267] §267.4, [Bloque 414]
> y [Bloque 415] para la derivación del join; N4 abre la superficie de seguridad, no re-deriva el join.
>
> **Corrección §14**: refuta el `[INFER]` de [Bloque 267] §267.4 sobre exposición de credencial en
> el manager (ver §417.4).
>
> **Subject version**: Niagara N4 4.14.0.162 · build 2024-05-28
>
> **Fuentes** (decompilado Vineflower; aliases de ruta):
> - `$ET_RT` = `.../exportTags/exportTags-rt/vineflower/com/tridium/exporttags/`
> - `$ET_WB` = `.../exportTags/exportTags-wb/vineflower/com/tridium/exporttags/`
> - `$BAJA` = `.../baja/baja/vineflower/javax/baja/`
> - `$FOX`  = `.../fox/fox-rt/vineflower/com/tridium/fox/`
> - Raíz: `/home/cristian/modules/Prototipos/modulos/organized/`
>
> **Método**: lectura inline directa de 8 archivos + grep de líneas clave; 10 tokens `[CERT]` de peso
> verificados por número de línea antes de escribir; framework-semantic check de 2 claims de permisos
> (ver §417.5 y §417.6). Marcadores: `[CERT]` = fuente primaria (file:line); `[INFER]` = deducción.
>
> Superficie de seguridad. Conecta [Bloque 266], [Bloque 267], [Bloque 414], [Bloque 415].

---

## 417.1 — Prior coverage y fuentes localizadas `[CERT]`

Este bloque NO re-deriva el join. Remite:

- **[Bloque 266] §266.5** — estableció que `BConnectInfo` transporta `BClientCredentials` como parámetro
  de la acción `joinStation` sobre el canal Fox, y que con `useFoxs=false` el canal carece de TLS.
  También identificó el riesgo como hipótesis; N4 lo verifica con evidencia de código.
- **[Bloque 267] §267.4** — estableció que `BJoinProfileManager` declara `colDefaultUserPassword` como
  columna `Prop` visible, y planteó el `[INFER]` de exposición en tabla. §417.4 lo refuta.
- **[Bloque 415] §415.5** — verificó que `BPointChannel.useSharedKeyEncryption()` retorna `true`,
  es decir, el canal `"point"` aplica cifrado de clave compartida Fox.

**Fuentes localizadas para N4** (grep directo, sin re-decompilación):

| Clase | Ruta verificada |
|---|---|
| `BConnectInfo` | `$ET_RT/util/BConnectInfo.java` |
| `BJoinProfile` | `$ET_RT/BJoinProfile.java` |
| `BSubordinateJoinJob` | `$ET_RT/BSubordinateJoinJob.java` |
| `BJoinProfileManager` | `$ET_WB/ui/BJoinProfileManager.java` |
| `BPassword` | `$BAJA/security/BPassword.java` |
| `BClientCredentials` | `$BAJA/security/BClientCredentials.java` |
| `BUsernameAndPassword` | `$BAJA/security/BUsernameAndPassword.java` |
| `Flags` | `$BAJA/sys/Flags.java` |

---

## 417.2 — BConnectInfo: estructura y defaults de seguridad `[CERT]`

`BConnectInfo` extiende `BComponent` e implementa `BIDeferOwnership`. Sus propiedades declaradas son
`[CERT]` `$ET_RT/util/BConnectInfo.java:17-43`:

| Propiedad | Tipo | Default | Rol |
|---|---|---|---|
| `stationName` | `String` | `""` | nombre de la station origen |
| `hostOrd` | `BOrd` | `BOrd.NULL` | dirección IP de la station |
| `port` | `int` | `1911` | puerto Fox (plain) |
| `useFoxs` | `boolean` | **`false`** | ¿usar TLS? — defecto NO |
| `credentialStore` | `BClientCredentials` | `new BClientCredentials()` | contenedor de credenciales |

**`BClientCredentials`** es a su vez un `BComponent` con una sola propiedad `clientCredentials` de tipo
`BValue`, cuyo default es `new BUsernameAndPassword("", BPassword.DEFAULT)` `[CERT]`
`$BAJA/security/BClientCredentials.java:14-21`.

**`BUsernameAndPassword`** tiene dos propiedades: `username` (String) y `password` (BPassword) `[CERT]`
`$BAJA/security/BUsernameAndPassword.java:27-38`.

**`BPassword`** es una clase `BSimple` que almacena una instancia de `BAbstractPasswordEncoder`. La
implementación distingue:
- `BNullPasswordEncoder` — valor vacío (el DEFAULT)
- `BReversiblePasswordEncoder` — cifrado AES (clave proveniente del keyring o clave externa)
- `BPlainPasswordEncoder` — texto plano (modo legacy, sin cifrado en reposo)

`[CERT]` La serialización `BPassword.toString(Context)` devuelve siempre `"--password--"`, nunca el
valor real `[CERT]` `$BAJA/security/BPassword.java:365-366`.

---

## 417.3 — BJoinProfile: tipo real de defaultUserPassword (corrige B267 §267.4) `[CERT]`

`BJoinProfile.defaultUserPassword` está declarado como tipo **`BPassword`** con default
`BPassword.DEFAULT` `[CERT]` `$ET_RT/BJoinProfile.java:123`:

```java
public static final Property defaultUserPassword = newProperty(0, BPassword.DEFAULT, null);
```

No hay faceta `BFacets.FIELD_EDITOR` explícita para esta propiedad `[CERT]` `:77-79` — usa el
field editor por defecto de `BPassword`, que en Workbench muestra el campo enmascarado (bullets).

`BJoinProfile.defaultUseFoxs` también tiene default `false` `[CERT]` `$ET_RT/BJoinProfile.java:126`:

```java
public static final Property defaultUseFoxs = newProperty(0, false, null);
```

Tanto `BConnectInfo.useFoxs` como `BJoinProfile.defaultUseFoxs` tienen **default `false`** → el join
opera sobre Fox sin TLS salvo que el operador lo configure explícitamente.

---

## 417.4 — Refutación §14: exposición de contraseña en el manager UI `[CERT]`

**[Bloque 267] §267.4** planteó el siguiente `[INFER]`:
> "Un operador que active esa columna podría ver la credencial de join en una tabla."

Esta hipótesis queda **REFUTADA** por evidencia de código:

1. `BJoinProfileManager.colDefaultUserPassword` es `new Prop(BJoinProfile.defaultUserPassword, 1)`
   `[CERT]` `$ET_WB/ui/BJoinProfileManager.java:47` — flags=1 (READONLY), sólo lectura en la tabla.

2. El tipo de la propiedad es `BPassword`, no `String`. Workbench llama `toString()` para renderizar
   la celda. `BPassword.toString(Context)` retorna siempre `"--password--"` `[CERT]`
   `$BAJA/security/BPassword.java:365-366` — el valor real NUNCA aparece en la celda.

3. `BJoinProfileManager` tiene `@AgentOn(types={"exportTags:SubordinateExportTagNetworkExt"})`
   `[CERT]` `$ET_WB/ui/BJoinProfileManager.java:33-37` — el manager sólo carga en el perfil `-wb`
   (Workbench), jamás en un JACE en runtime.

**Veredicto**: la columna del manager UI muestra `"--password--"`, no el plaintext. La exposición
visual via manager está **mitigada** por el diseño de `BPassword`. `[B267 §267.4 — corrected in B417]`

---

## 417.5 — Flujo de credenciales en BSubordinateJoinJob.run() `[CERT]`

Cuando el operador invoca la acción `join`, se ejecuta `BJoinProfile.doJoin(cx)` que crea y encola
un `BSubordinateJoinJob`. En `run()`:

**Paso 1 — Extracción de contraseña en claro (doPrivileged)**:
```java
defaultPassword = BPassword.make(
    AccessController.doPrivileged(
        this.profile.getDefaultUserPassword()::getValue
    )
);
```
`[CERT]` `$ET_RT/BSubordinateJoinJob.java:79`

`AccessController.doPrivileged()` suprime las comprobaciones de seguridad Java para acceder a
`BPassword.getValue()`. El retorno es el **plaintext** de la contraseña.

**Paso 2 — Construcción de BConnectInfo con credenciales**:
```java
BConnectInfo info = new BConnectInfo();
info.setStationName(Sys.getStation().getStationName());
info.setCredentials(new BUsernameAndPassword(defaultUserName, defaultPassword));
info.setUseFoxs(useFoxs);
info.setPort(port);
```
`[CERT]` `$ET_RT/BSubordinateJoinJob.java:84-113`

**Paso 3 — Sesión Fox temporal y envío del BConnectInfo como parámetro de acción**:
```java
BFoxSession session = (BFoxSession)BFoxSession.make(
    (BHost)supervisorStation.getAddress().get(),
    supervisorStation.getFoxPort(),
    supervisorStation.getClientConnection().getUseFoxs(), // ← flag TLS de la conexión
    supervisorStation.getClientConnection().getCredentials()
);
BPointChannel pointChannel = …session.getConnection().getChannels().get("point", BPointChannel.TYPE);
…
BOrd superJobOrd = (BOrd)invokeAction(
    pointChannel,
    BOrd.make("service:exportTags:SupervisorExportTagNetworkExt"),
    "joinStation",
    info   // ← BConnectInfo con credenciales serializado
);
```
`[CERT]` `$ET_RT/BSubordinateJoinJob.java:117-157`

El `BConnectInfo` (con el `BUsernameAndPassword` y su `BPassword`) es serializado como parámetro de
la acción `joinStation` y transmitido sobre el canal Fox `"point"`.

El canal `"point"` aplica cifrado de clave compartida Fox (`useSharedKeyEncryption()=true`, ya
documentado `[CERT]` en [Bloque 415] §415.5). Sin embargo, la clave compartida se negocia **durante
el handshake de la sesión Fox**: si la sesión Fox es plain (sin TLS), el intercambio de clave ocurre
sobre TCP sin cifrar.

`[INFER]` Un interceptor pasivo que capture el handshake Fox (TCP) puede recuperar la clave compartida
y descifrar el payload del canal `"point"`, obteniendo el `BConnectInfo` con las credenciales. El cifrado
de capa de canal Fox **no sustituye TLS** cuando la sesión de transporte es plain Fox.

> **`[B417 §417.5 — INFER REFUTADO en B419]`**: el mecanismo de key exchange es SRP6 post-SCRAM
> (no un valor estático ni recoverable del handshake). En plain Fox N4-a-N4, la `sessionKey` deriva
> de SRP6 y sus exponentes privados (`a`, `b`) nunca se transmiten → la clave AES del canal `"point"`
> NO es recuperable por un interceptor pasivo (PLD). Ver [Bloque 419] §419.7 para la refutación completa.

---

## 417.6 — Permisos sobre la acción de join: framework-semantic check `[CERT]`

**Framework-semantic check obligatorio** (regla adoptada del focus — en el focus `tags` 4/4 claims de
permisos resultaron falsos; aquí se verifican 2):

### Claim 1: la acción `join` es de nivel admin

`BJoinProfile.join` se declara con `flags=8` `[CERT]` `$ET_RT/BJoinProfile.java:134`:
```java
public static final Action join = newAction(8, null);
```

Cruzando contra la semántica real del framework `[CERT]` `$BAJA/sys/Flags.java:12,17,152-154`:
```java
public static final int SUMMARY   = 8;    // bit 3
public static final int OPERATOR  = 256;  // bit 8
public static boolean isAdmin(BComplex object, Slot slot) {
    return (object.getFlags(slot) & 256) == 0;
}
```

`flags=8` es `SUMMARY` exclusivamente; el bit `OPERATOR` (256) **no está activo**. Por lo tanto:
- `Flags.isOperator(bjoinProfile, join)` = **false**
- `Flags.isAdmin(bjoinProfile, join)` = **true** (256 == 0 → admin-only)

**Veredicto claim 1**: CONFIRMADO. La acción `join` es de nivel **admin** en la taxonomía de permisos
de Baja. Un usuario con rol Operator NO puede invocarla desde la UI estándar. `[CERT]`

### Claim 2: la columna de password en el manager expone el valor en claro a cualquier operador

(Planteado como [INFER] en B267 §267.4 — ya refutado en §417.4.)

**Veredicto claim 2**: REFUTADO por `BPassword.toString()`. `[CERT]`

### Resultado del framework-semantic check

| Claim | Resultado |
|---|---|
| join es acción de nivel admin | CONFIRMADO (`flags=8`, OPERATOR bit ausente → isAdmin=true) |
| columna password expone plaintext a operadores | REFUTADO (`BPassword.toString()` → "--password--") |

**Ratio**: 1/2 confirmados, 1/2 refutados. Mejor que el focus `tags` (0/4), pero la regla de verificar
antes de escribir sigue siendo esencial — el [INFER] de B267 habría sido publicado como hallazgo
de seguridad sin esta verificación.

---

## 417.7 — Resumen del veredicto de seguridad

| Vector | Estado | Evidencia |
|---|---|---|
| **Credencial en BOG en reposo** | Mitigado por BPassword (AES o NullEncoder) | `[CERT]` BJoinProfile:123 + BPassword encoder hierarchy |
| **Exposición en manager UI** | Mitigado: columna muestra "--password--" | `[CERT]` BPassword.java:365-366 — Refuta B267 §267.4 |
| **Transporte con useFoxs=false** | **Riesgo real**: credencial protegida sólo por clave Fox negociada sin TLS | `[CERT]` BSubordinateJoinJob:120, BConnectInfo:42 + `[INFER]` keyex sin TLS |
| **Acción join sin permiso admin** | Mitigado: sólo admin puede invocarla (OPERATOR bit ausente) | `[CERT]` BJoinProfile:134 + Flags.java:152-153 |
| **Clave compartida Fox sin TLS** | `[INFER]` — mecanismo de key exchange no verificado en este bloque | Gap N7 abierto |

**Conclusión**: la superficie de mayor riesgo verificable es el **transporte plain Fox** cuando
`useFoxs=false` (ambos defaults). El BPassword mitiga el riesgo en reposo y en la UI, pero NO en
tránsito si el canal Fox opera sin TLS. El riesgo en tránsito depende de si el intercambio de clave
compartida Fox ofrece confidencialidad sin TLS — queda como `[INFER]` pendiente de verificación (N7).

---

## 417.8 — Open questions / gaps nuevos

- **N7 (LOW)**: mecanismo de intercambio de clave compartida Fox — ¿DH (clave de sesión efímera) o
  clave derivada del hello (visible en plaintext)? Si es el segundo, el cifrado del canal `"point"`
  no ofrece confidencialidad real sin TLS. Fuente: `$FOX/sys/BFoxSession.java` + el hello handshake.

---

## 417.9 — Conexiones

- **[Bloque 266] §266.5** — estableció la hipótesis: BConnectInfo como parámetro serializado sobre Fox
  sin TLS. B417 la verifica con file:line y añade el modelo de permisos y el framework-semantic check.
- **[Bloque 267] §267.4** — `[INFER]` de exposición de password en manager: REFUTADO en §417.4.
  Backlink: ver nota `[B267 §267.4 — corrected in B417]` en §417.4.
- **[Bloque 414]** §414.3 — join y BSubstitutePxView; BSubordinateJoinJob opera sobre la misma
  `BNiagaraStation` del supervisor documentada en B414.
- **[Bloque 415]** §415.5 — `BPointChannel.useSharedKeyEncryption()=true` remitido; B417 expone que
  sin TLS ese cifrado de canal es insuficiente si el key exchange ocurre en plaintext (→ N7).
