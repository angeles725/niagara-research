# Block 156 — Perfil pasivo de la station viva (Etapa A · B1 del focus `live-station`)

> **Bloque de reconocimiento dinámico** (METHODOLOGY §12): primer contacto READ-ONLY con una **station
> Niagara N4 VIVA** corriendo en `127.0.0.1` (WSL mirrored). Perfila su superficie de red, identidad y
> postura de seguridad **sin autenticar** — sólo handshakes TLS y banners que la station entrega a cualquier
> cliente. Ninguna escritura, ningún login (rung 1 de la escalera de invasividad §12).
>
> Focus: **live-station** (validación dinámica de la station real) — bloque INAUGURAL del focus, Etapa A
> (mapear el runtime vivo). Etapa B terminal verificará los 14 defectos de [Block 150] contra esta misma
> station con un usuario de prueba. Corpus language: Spanish (technical EN).
>
> **Sensibilidad: `live-install` → SECRETS DISCIPLINE.** Se cita ESTRUCTURA pública (puertos, certificado
> servido a cualquier cliente, banners de handshake, cabeceras HTTP) — NUNCA claves privadas, nonces de
> sesión, credenciales ni tokens. Cero secretos exfiltrados.
>
> Fuente (evidencia `[CERT-hw]` preservada vía `toolbelt/probe.sh`):
> - `sources/probes/bash-20260702T185430Z.txt` — recon pasiva (puertos, certs, fox hello, TLS posture).
> - `sources/probes/bash-20260702T185535Z.txt` — versión/CSP en `/login`, identidad de `:8080`.
>
> Markers: `[CERT-hw]` = medido en vivo contra la station real (cita el probe preservado) · `[CERT]` =
> re-cita `file:line` ya verificado en un bloque previo · `[INFER]` = deducción/postura.
>
> Capa 27 (runtime vivo). Inaugura el focus `live-station`; cruza con [Block 149]/[Block 150] (Reflow) y
> [Block 124]-[Block 130] (platform-native).

---

## 156.1 — Superficie de red viva `[CERT-hw]`

Barrido TCP contra `127.0.0.1` (`bash-20260702T185430Z.txt` §1). Sólo los servicios Niagara **seguros**
están abiertos; los canales en claro y de campo están cerrados:

| Puerto | Estado | Servicio | Nota |
|---|---|---|---|
| 4911 | **OPEN** | **Foxs** (Fox sobre TLS) | canal Fox de la station |
| 1911 | closed | Fox en claro | `[INFER]` deshabilitado — sólo foxs |
| 443 | **OPEN** | HTTPS (WebService) | UI + REST + WS |
| 80 | **OPEN** | HTTP | sólo redirige a 443 (§156.4) |
| 3011 | **OPEN** | Platform (niagarad, plano) | daemon de plataforma |
| 5011 | **OPEN** | Platform-TLS | plataforma sobre TLS |
| 502 | closed | Modbus TCP | no expuesto |
| 47808 | closed | BACnet/IP | no expuesto |
| 21/22/23 | closed | FTP/SSH/Telnet | no expuestos |
| 8080 | **OPEN** | `Embedthis-http` — **NO Niagara** | servicio vecino (§156.6) |

`[INFER]` La postura de exposición es correcta para Niagara: el par plano/seguro de cada servicio (1911/4911,
3011+5011) muestra el seguro abierto y, en Fox, el plano cerrado. No hay protocolos de campo (Modbus/BACnet)
escuchando en esta caja.

## 156.2 — Identidad de la station: fox hello handshake `[CERT-hw]`

El handshake Foxs de `:4911` (negociación de protocolo, previa al challenge de auth) entrega en claro la
identidad de la station a cualquier cliente (`bash-20260702T185430Z.txt` §8):

```
fox a 0 -1 fox hello
fox.version=s:1.0.2
hostName=s:DESKTOP-4AAQ77H
hostAddress=s:192.168.100.100
app.name=s:Station
fox a 1 -1 fox challenge      ← aquí paramos (rung 1: sin autenticar)
```

- `app.name=Station` `[CERT-hw]` → es una **Station** (runtime de control), no un Supervisor ni Workbench.
- `hostName=DESKTOP-4AAQ77H`, `hostAddress=192.168.100.100` `[CERT-hw]` → identidad de host de la caja viva
  (coincide con el `eth3` local, confirmando que la station corre en esta misma máquina WSL).
- `fox.version=1.0.2` `[CERT-hw]` es la versión del **protocolo Fox**, NO la versión de Niagara (la de Niagara
  queda gated tras login, §156.4). El siguiente frame es `fox challenge` → autenticación SCRAM; no se avanza
  en Etapa A (eso es Etapa B con el usuario de prueba).

`[INFER]` Éstos son los **ground-truth vivos** re-medidos en esta fase (§12: no heredar de bloques previos);
se registran en §156.7 como ancla de identidad para Etapa B.

## 156.3 — Certificado TLS compartido: default `ForRecoveryPurposes` `[CERT-hw]`

Los tres puertos TLS (443, 4911, 5011) presentan el **MISMO** certificado — idéntico fingerprint SHA-256
(`bash-20260702T185430Z.txt` §4-6):

- `subject = CN=Niagara4, O=ForRecoveryPurposes, C=US` (issuer = subject → **autofirmado**).
- SAN `DNS:Niagara4`; clave **RSA 2048**; firma `sha256WithRSA`.
- Validez `Sep 17 2025 → Sep 17 2026`; SHA-256 `C1:01:41:B2:C4:C3:42:DB:E9:23:1A:06:DD:89:DE:CA:DC:F0:A5:FF:07:7D:F7:1F:90:E0:40:5F:D5:84:E5:D2`.

`[INFER]` `O=ForRecoveryPurposes` / `CN=Niagara4` es el certificado **por defecto de recuperación de fábrica**
de Niagara: la station NO tiene un certificado provisto (ni CA-firmado ni con hostname real). Un mismo cert
autofirmado en los 3 servicios ⇒ ningún cliente puede validar cadena; TLS aquí da cifrado, no autenticación de
servidor. Es una brecha de hardening de despliegue (no de código), coherente con una station de laboratorio.

## 156.4 — Postura HTTP/TLS `[CERT-hw]`

Cabeceras vivas (`bash-20260702T185430Z.txt` §2-3,9 · `bash-20260702T185535Z.txt`):

- **HTTP→HTTPS forzado:** `:80` responde `303 See Other → https://127.0.0.1/` `[CERT-hw]`. `:443/` responde
  `302 → /login` → `302 → /prelogin`. Todo el tráfico plano se eleva a TLS.
- **HSTS:** `Strict-Transport-Security: max-age=63072000` (2 años) `[CERT-hw]`.
- **TLS 1.3 únicamente:** TLS 1.0/1.1/1.2 rechazados (`Cipher is (NONE)`), sólo TLS 1.3 negocia
  (`TLS_AES_256_GCM_SHA384`) `[CERT-hw]`. Postura de transporte fuerte.
- **Cabeceras de seguridad** en `/login`: `X-Content-Type-Options: nosniff`, `X-Frame-Options: SAMEORIGIN`,
  `X-XSS-Protection: 1; mode=block`, `Cross-Origin-Opener-Policy: same-origin` `[CERT-hw]`.
- **Versión Niagara NO se filtra sin auth:** `/system/version`, `/doc/version.txt`, `/niagara/version` → todos
  `302` a login `[CERT-hw]`. Buena postura; la versión exacta queda para Etapa B (autenticada). El contexto de
  proyecto la infiere como N4.14.0.162 pero §12 exige medirla en vivo → pendiente de Etapa B, no heredada.

## 156.5 — La CSP viva confirma [Block 150] item 14 y delata a Reflow `[CERT-hw]`

La redirección `/login` emite una `Content-Security-Policy` completa (`bash-20260702T185535Z.txt`). Dos lecturas
de alto valor, ambas **medidas en vivo**:

1. **Confirma item 14 de [Block 150]** (CSP con `unsafe-inline`/`unsafe-eval`): la política viva incluye
   literalmente `script-src 'self' workbench 'unsafe-inline' 'unsafe-eval'` y `style-src ... 'unsafe-inline'`
   `[CERT-hw]`. Esto **eleva a `[CERT-hw]`** lo que [Block 149] §149 y [Block 150] item 14 afirmaban desde el
   decompilado (`BaseServlet.java:48` `[CERT]`): el hardware confirma el código.
2. **Reflow está vivo en esta station** `[CERT-hw]`: `connect-src` incluye `unsplash.niagaramodules.com` y hay
   `report-uri /csp-reports`. Ese origen unsplash es el que Reflow usa en `WeatherMapResponse` (item 11 de
   [Block 150] / `WeatherMapResponse.java:82,117` `[CERT]`). `[INFER]` Por tanto el módulo NiagaraMods Reflow
   v1.7.7 — toda la superficie mapeada en [Block 138]-[Block 155] — está **instalado y activo aquí**, y es un
   objetivo válido de la verificación dinámica de Etapa B.

`[INFER]` La CSP mezcla postura fuerte (default-src 'self') con las excepciones peligrosas que el corpus ya
había señalado; la Etapa B podrá probar si el input reflejado en errores ([Block 149]) es explotable bajo esta
CSP real.

## 156.6 — Servicio vecino `:8080` — NO es la station `[CERT-hw]`

`:8080` responde `Server: Embedthis-http` con `404` (`bash-20260702T185535Z.txt`), no es TLS y no presenta cert
Niagara. `[INFER]` Embedthis (Appweb/GoAhead) es un webserver embebido de firmware/appliance, ajeno al runtime
Niagara. Se registra como **servicio co-residente distinto** para no contaminar el perfil de la station; no
forma parte del runtime bajo estudio y queda fuera del scope del focus salvo que se decida perfilarlo aparte.

## 156.7 — Ground-truth vivo registrado (ancla §12 para Etapa B) `[CERT-hw]`

Identificadores **re-medidos en vivo** en esta fase (no heredados) — ancla de identidad y punto de re-chequeo
para toda Etapa B:

| Identificador | Valor vivo | Fuente |
|---|---|---|
| app.name | `Station` | fox hello §156.2 |
| hostName | `DESKTOP-4AAQ77H` | fox hello §156.2 |
| hostAddress | `192.168.100.100` | fox hello §156.2 |
| Fox proto | `1.0.2` | fox hello §156.2 |
| Cert SHA-256 | `C1:01:41:B2:…:E5:D2` | TLS §156.3 |
| Cert subject | `CN=Niagara4, O=ForRecoveryPurposes` | TLS §156.3 |
| Puertos vivos | 4911, 443, 80, 3011, 5011 (+8080 vecino) | barrido §156.1 |
| Módulo Reflow | activo (unsplash en CSP) | CSP §156.5 |

`[INFER]` **Identidad ≠ unidad física (§12):** estos identifican el PROGRAMA/servicio corriendo, no garantizan
en qué máquina física; aquí es coherente con localhost/WSL. Antes de cualquier interacción autenticada de
Etapa B se re-confirma que se apunta a esta misma station (mismo cert SHA-256 + hostAddress).

## 156.8 — Connections

- **[Block 149]** / **[Block 150]** — la CSP viva (§156.5) eleva a `[CERT-hw]` el item 14 (CSP inseg.) y
  confirma la presencia de Reflow (item 11, WeatherMap/unsplash). Etapa B verificará los 14 defectos aquí.
- **[Block 124]-[Block 130]** (platform-native) — los puertos platform 3011/5011 vivos son la superficie del
  daemon `niagarad` que aquel focus documentó estáticamente; Etapa A los confirma abiertos.
- **[Block 138]** — el módulo Reflow (service/HTTP-WS) cuya presencia viva se confirma en §156.5.
- **Focus `live-station`** — bloque inaugural; sienta el ground-truth (§156.7) sobre el que corre Etapa A
  (mapa del runtime) y Etapa B (verificación de los 14 defectos con usuario de prueba).
