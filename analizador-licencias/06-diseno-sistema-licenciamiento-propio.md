# 06 — Diseño: sistema de licenciamiento firmado propio (esquema REF-License)

> **Alcance**: diseño + implementación de referencia de un sistema de licenciamiento
> **propio** (producto del usuario, no Niagara). Arquitectura inspirada en el patrón
> estándar de licenciamiento firmado (el mismo que usa la industria y que ya está
> documentado en `01-diagnostico-despliegue.md` y `00-manual-archival-licensing.md`),
> pero con esquema, claves y formatos propios. Implementación de referencia:
> `tools/licensador/licensador.py`.
>
> **Documento hermano**: `05-pentest-evasion-2026-08-01.md` — el pentest de N4 sirve
> aquí como **catálogo de lo que un atacante intentará** contra este esquema (y por
> qué cada intento falla si se implementa como se especifica).

## 1. Objetivo

Que el producto (p. ej. módulos reflow) requiera una **licencia firmada** emitida por
el fabricante (tú) para activar features, con estas propiedades:

- **No falsificable** por el cliente final: sin la clave privada no se puede emitir
  una licencia válida (asimetría RSA).
- **Atada a una máquina**: la licencia vale para un HostId específico.
- **Acotada en el tiempo y en features**: expiración + cantidad por feature.
- **Verificable offline**: toda la validación ocurre en el dispositivo, sin red.
- **Auditable**: cada evento de licencia queda en un log.

## 2. Modelo de amenazas (qué va a intentar el atacante)

Tomado de la evidencia del pentest N4 (L-4..L-18bis) y de los modos de fallo de
`01-diagnostico-despliegue.md` §5 — este esquema se diseña para resistir exactamente
estos intentos:

| # | Intento del atacante | Defensa | Referencia N4 |
|---|---|---|---|
| T1 | Copiar una licencia válida a otra máquina | Binding `hostId` (check 4) | F1 (hostId) |
| T2 | Editar el archivo (extender expiración, agregar features) | Firma RSA-PSS sobre payload canónico (check 2) | F2 (firma) |
| T3 | Reescribir el verificador para que acepte todo | Firma/digest del propio módulo + `verifyMode` estricto | L-14/L-15/L-18bis |
| T4 | Atrasar el reloj para evadir expiración | High-water mark `lastSeen` persistido + skew | F6/F7 (anti-reloj) |
| T5 | Robar la clave privada | Ceremonia de claves offline, clave cifrada, nunca en dispositivos | (control positivo N4: signing OEM) |
| T6 | Reutilizar una licencia revocada | Blacklist firmada por la misma raíz | — |
| T7 | Parchear el módulo propio para saltarse `checkFeature` | Digest manifest firmado + verificación al cargar | L-14 (digest error) |

**Regla de oro**: el dispositivo solo conoce la **clave pública**. Todo lo que exige
secreto vive en la máquina de emisión, que no está en campo.

## 3. Arquitectura

```
┌────────────────────┐   issue()   ┌──────────────────────┐
│ MÁQUINA DE EMISIÓN │────────────►│ ARTEFACTO .refl      │
│ (offline, segura)  │             │ REF1.<payload>.<sig> │
│  - raíz privada    │             └──────────┬───────────┘
│    (cifrada)       │                        │ deploy
└────────────────────┘                        ▼
┌─────────────────────────────────────────────────────────┐
│ DISPOSITIVO (producto del cliente)                      │
│  verifier: raíz pública embebida (recurso del módulo)   │
│  pipeline: PARSE→SIGNATURE→PRODUCT→HOST→TIME→FEATURE    │
│            →REVOCATION  (all-or-nothing)                │
│  estado:   lastSeen (anti-reloj), blacklist, audit log  │
└─────────────────────────────────────────────────────────┘
```

### 3.1 Jerarquía de claves

- **Raíz** (RSA-3072): solo en la máquina de emisión, PKCS#8 cifrada con passphrase,
  backup en lugar seguro. Firma licencias, blacklist y (opcional) el manifest de
  módulos.
- **Clave pública raíz**: embebida como recurso constante en el verificador (y en
  el manifest firmado del propio módulo, para que un atacante no la pueda reemplazar
  sin romper la firma del módulo — defensa T3).
- Extensión futura (no en v1): claves de producto intermedias firmadas por la raíz,
  para separar líneas de producto sin exponer la raíz.

### 3.2 Artefacto de licencia

Formato compacto y legible en texto (inspirado en JWS):

```
REF1.<base64url(payload JSON canónico)>.<base64url(firma RSA-PSS-SHA256)>
```

- **JSON canónico**: claves ordenadas, sin espacios, ASCII (`sort_keys=True`,
  `separators=(",",":")` en Python; equivalente en Java con un serializador
  canónico propio o Jackson + `@JsonInclude` y ordenamiento).
- La firma cubre **exactamente** los bytes del payload canónico. Cualquier byte
  distinto invalida la firma (T2).

Claims del payload:

| Campo | Tipo | Ejemplo | Notas |
|---|---|---|---|
| `ver` | int | 1 | versión del esquema |
| `product` | str | `reflow-oven` | qué producto habilita |
| `licensee` | str | `Planta Chihuahua` | quién |
| `hostId` | str | `REF-4D6F169BCEF1` | binding de máquina (check 4) |
| `serial` | str | `ULID/hex aleatorio` | id único, usado en blacklist |
| `issued` | ISO8601 UTC | `2026-08-02T00:00:00+00:00` | check 5 |
| `expires` | ISO8601 UTC \| null | `2027-08-02T00:00:00+00:00` | null = perpetua |
| `features` | map | ver abajo | qué habilita y en qué cantidad |

`features`: `{ "<nombre>": {"qty": <int|null>, "opts": {...}} }` — `qty=null`
significa ilimitado. `opts` para parámetros por feature (límites de runtime,
subfeatures).

### 3.3 HostId propio (`REF-…`)

Derivación propia (no copia del esquema N4; mismo concepto genérico de binding):

```
REF-<12 hex>  =  "REF-" + SHA-256( volSerial || macPrimaria || machineGuid || SALT )[0:6 bytes]
```

- Entradas estables y de bajo nivel (en Windows: serial de volumen del disco de
  sistema + MAC de la NIC primaria; en Linux: `/etc/machine-id` + MAC).
- `SALT = "REF-LICENSE/v1"` (evita que el hash sea intercambiable con otro uso).
- Determinístico, estable entre reinicios; **cambia si cambia el hardware** →
  comportamiento documentado: se re-emite la licencia (ver §7). Es *feature*, no
  bug: es lo que impide T1.
- La implementación de referencia (`hostid` subcommand) usa `uuid.getnode()` +
  `machine-id`; la versión de producción Java usa los identificadores elegidos por
  ti, con el mismo formato de salida.

## 4. Pipeline de verificación (all-or-nothing)

Orden fijo; cada check falla con un **código de error distinto** para diagnóstico:

| # | Check | Código de error | Qué valida |
|---|---|---|---|
| 1 | `PARSE` | `E_PARSE` | formato `REF1.a.b`, base64url válido, JSON válido |
| 2 | `SIGNATURE` | `E_SIGNATURE` | firma RSA-PSS-SHA256 contra la raíz pública embebida |
| 3 | `PRODUCT` | `E_PRODUCT` | `product` == producto del verificador |
| 4 | `HOST` | `E_HOST` | `hostId` == `refHostId()` actual |
| 5 | `TIME` | `E_TIME_NOT_YET` / `E_TIME_EXPIRED` | `issued ≤ now+skew` y `expires ≥ now−skew` (skew típico 5 min) |
| 6 | `FEATURE` | `E_FEATURE_LOCKED` / `E_FEATURE_QTY` | en runtime, `checkFeature(name, qty)` contra claims |
| 7 | `REVOCATION` | `E_REVOKED` | `serial` no está en la blacklist firmada vigente |

- **Anti-reloj (T4)**: se persiste `lastSeen` (máximo `now` observado, en un archivo
  de estado del módulo). Si `now < lastSeen − skew` → `E_CLOCK` (rollback detectado)
  y se registra en el audit log. El reloj no se puede atrasar sin que el sistema lo
  note, y adelantarlo solo acelera la expiración.
- **Runtime**: el verificador resuelve una vez en el arranque y mantiene en memoria
  las concesiones (`grant` por feature). `checkFeature(name, qty)`:
  - sin licencia válida → feature bloqueado (equivalente a la
    `FeatureNotLicensedException` documentada en `01` §3);
  - con licencia → `qty` se descuenta del pool mientras el feature esté activo
    (conteo en memoria; re-verificación periódica opcional).
- **Audit**: cada evento (verify OK, fallo, grant, revocación aplicada, E_CLOCK)
  se escribe al log del módulo con timestamp y serial.

## 5. Anti-tampering del módulo propio (T3/T7)

- **Digest manifest**: al construir el módulo, se genera `MANIFEST` (sha-256 de cada
  jar/clase) firmado con la raíz. El loader del módulo verifica el manifest **antes
  de cargar cualquier clase**; fallo → no arranca (modo `strict`) o log de alerta
  (modo `warn`, para diagnóstico en campo).
- **`verifyMode`**: propiedad de configuración `strict|warn` (análogo conceptual al
  `moduleVerificationMode` de N4, L-18bis). Default recomendado: `strict`.
- Con esto, parchear el verificador para saltarse `checkFeature` implica re-firmar
  el manifest, y eso requiere la raíz privada (T5) — el mismo principio de la doble
  barrera que bloqueó a L-14/L-15 en N4.

## 6. Modos de fallo y diagnóstico

Tabla de referencia para soporte (mismo espíritu que `01` §5):

| Código | Causa típica | Diagnóstico |
|---|---|---|
| `E_PARSE` | archivo corrupto/truncado, copia manual con salto de línea | re-deploy del `.refl` |
| `E_SIGNATURE` | licencia no emitida por tu raíz, o archivo editado | re-emitir; no editar a mano |
| `E_PRODUCT` | licencia de otro producto | emitir para el producto correcto |
| `E_HOST` | licencia de otra máquina (T1), o hardware cambiado | re-emitir para el HostId actual (`hostid`) |
| `E_TIME_NOT_YET` / `E_TIME_EXPIRED` | reloj del dispositivo mal seteado, o expiró | ajustar reloj / renovar |
| `E_CLOCK` | reloj atrasado respecto del máximo visto (T4) | corregir reloj; revisar si hubo manipulación |
| `E_FEATURE_LOCKED` / `E_FEATURE_QTY` | feature no incluido, o cantidad agotada | emitir licencia con el feature/cantidad |
| `E_REVOKED` | licencia revocada (cliente no pago, máquina perdida) | renovar contrato; emitir nueva |

## 7. Flujo operativo

1. **Ceremonia de claves** (una vez): máquina offline → `genkeys` → raíz privada
   cifrada + backup seguro; pública embebida en el verificador.
2. **Emisión**: `issue --product reflow-oven --host <REF-…> --feature "PID:4" --expires 2027-08-02` →
   archivo `.refl` (nunca se toca a mano; T2).
3. **Deploy**: copiar `.refl` a `licenses/` del dispositivo + importar (comando o
   file-drop watcher). El verificador lo valida en el próximo arranque.
4. **Renovación**: nueva licencia con nuevo `serial`; la anterior se revoca.
5. **Revocación**: `revoke --serial …` → firma una blacklist (JSON firmado con la
   misma raíz) que se distribuye con el producto/actualización; check 7 la aplica.
6. **Auditoría**: revisar el log de licencias del dispositivo en soporte.

## 8. Especificación Java (para integrar en el módulo real)

Clases mínimas (esquema propio; nombres de ejemplo):

- `RefLicenseManager` — carga `.refl` en boot, corre el pipeline, expone
  `checkFeature(String name, int qty)`.
- `RefHostId` — `static String get()` (derivación §3.3 con identificadores elegidos).
- `RefKeyStore` — raíz pública embebida (recurso firmado por el manifest).
- `RefLicenseException` — jerarquía por código de error (§6).
- Verificación RSA-PSS-SHA256: `Signature.getInstance("SHA256withRSAandMGF1")`
  (disponible en JVM de N4; si la JVM es muy vieja, fallback RSA PKCS#1 v1.5 — se
  decide al portar; el artefacto indica `ver` para permitir migración).

## 9. Implementación de referencia

`tools/licensador/licensador.py` — CLI Python (stdlib + `cryptography`):

```
genkeys   --out-dir DIR [--bits 3072]          # genera raíz privada + pública
issue     --key PRIV --product P --host H      # emite licencia .refl
          [--expires ISO] [--feature NAME[:QTY]]...
verify    --root PUB --file LIC [--host H]     # corre el pipeline; exit 0 = OK
inspect   --file LIC                           # muestra claims sin verificar
hostid    [--salt S]                           # calcula REF-… de esta máquina
revoke    --key PRIV --serial S --out BL       # firma blacklist
selftest  --tmp DIR                            # matriz de pruebas (positivo +
                                               # T1..T4 + corrupción)
```

La implementación es la **especificación ejecutable**: el `selftest` verifica el
comportamiento de todos los checks; el port Java debe pasar la misma matriz.

## 10. Pruebas de aceptación (matriz)

| # | Escenario | Esperado |
|---|---|---|
| A1 | emitir + verificar en la misma máquina | `OK` |
| A2 | verificar en HostId distinto (T1) | `E_HOST` |
| A3 | modificar 1 byte del payload (T2) | `E_SIGNATURE` |
| A4 | expirar la licencia (emitir con expiración pasada) | `E_TIME_EXPIRED` |
| A5 | feature no incluido | `E_FEATURE_LOCKED` |
| A6 | qty insuficiente | `E_FEATURE_QTY` |
| A7 | serial en blacklist | `E_REVOKED` |
| A8 | reloj atrasado (T4) | `E_CLOCK` |
| A9 | producto distinto | `E_PRODUCT` |
| A10 | archivo corrupto | `E_PARSE` |

A1–A10 corren en `selftest`.
