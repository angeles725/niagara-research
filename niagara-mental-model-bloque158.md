# Block 158 — Platform daemon HTTP-guardado + cierre de Etapa A (mapa del runtime vivo)

> **Bloque de reconocimiento dinámico** (METHODOLOGY §12, rung 1 read-only): perfila los puertos platform
> (3011/5011) y **cierra Etapa A** del focus `live-station` consolidando el mapa del runtime vivo (B156+B157).
> Cubre el gap A5 y sella A2 (versión) como **ausencia probada** con el acceso disponible. Ninguna escritura.
>
> Focus: **live-station** — Etapa A (mapa del runtime), gaps A5 + cierre A2. Corpus language: Spanish (tech EN).
>
> **Sensibilidad `live-install` → SECRETS DISCIPLINE.** Sólo códigos HTTP / banners de handshake; cero
> credenciales, cero cuerpos sensibles. Credencial `API` sólo en tránsito (config efímero, no preservado).
>
> Fuente (`[CERT-hw]` preservada): `sources/probes/bash-20260702T191016Z.txt` (platform 3011/5011) +
> `sources/probes/bash-20260702T191119Z.txt` (A2 ausencia probada). Cruza [Block 156]/[Block 157] y
> [Block 124]-[Block 130] (platform-native).
>
> Markers: `[CERT-hw]` medido en vivo · `[CERT]` re-cita bloque previo · `[INFER]` deducción. Capa 27.

---

## 158.1 — El "platform daemon" 3011/5011 es HTTP(S) Jetty, guardado `[CERT-hw]`

Contra lo que sugiere el nombre "platform" (protocolo binario tipo Fox), los puertos 3011/5011 responden
**HTTP sobre Jetty**, no un protocolo platform crudo (`bash-20260702T191016Z.txt`):

| Estímulo | Puerto | Respuesta |
|---|---|---|
| lectura pasiva (sin enviar) | 3011 | (sin banner no-solicitado) |
| `GET / HTTP/1.0` | 3011 (plano) | `HTTP/1.1 403 Forbidden`, `x-frame-options: deny`, `Connection: close` |
| línea suelta (`niagarad\r\n`) | 3011 | `HTTP/1.1 400 No URI` (parser Jetty) |
| `GET /` tras TLS | 5011 (TLS) | `HTTP/1.1 403 Forbidden` + `Strict-Transport-Security` |

`[INFER]` En N4.14 la conexión de plataforma viaja **sobre HTTP(S)** (el par 3011 plano / 5011 TLS), servida
por el mismo stack Jetty que la station, y **rechaza con 403** cualquier acceso sin las credenciales de
*plataforma* (que son de nivel OS/daemon, distintas de los usuarios de station como `API`). No hay banner ni
versión pre-auth aquí — a diferencia del fox hello de station (B156 §156.2), el platform daemon **no** revela
identidad sin autenticar. Concuerda con la superficie nativa documentada estáticamente en [Block 124]-[Block 130].

## 158.2 — A2 (versión exacta): ausencia probada con el acceso disponible `[CERT-hw]`

La versión Niagara **no se filtra por NINGÚN canal alcanzable** con el usuario read-level `API`
(`bash-20260702T191119Z.txt` — cierre por proven-absence, §8):

| Canal | Resultado |
|---|---|
| `/spy/versions`, `/doc/version.txt`, `/about` | 404 (no montados) |
| `/module/baja`, `/module/bajaScript` | **403** (prohibidos a este usuario) |
| `/ord/module:|module:baja` | 404 |
| grep version-quad en shell station autenticada | **vacío** |
| grep version-quad en `/prelogin` público | **vacío** |
| platform 3011/5011 | 403 (necesita creds de plataforma) |

`[CERT-hw]` La versión exacta queda **NO re-medida en vivo**: está bloqueada tras credenciales de plataforma o
un usuario de station con permiso de módulos. `[INFER]` Por §12 **NO se hereda** el `N4.14.0.162` del contexto
de proyecto — se marca A2 como `blocked-on-platform/admin-creds`. Es además un dato de postura: un usuario
mínimo no puede fingerprintear la versión (buena minimización). El `fox.version=1.0.2` de B156 es del protocolo
Fox, no del build Niagara.

## 158.3 — Cierre de Etapa A: mapa del runtime vivo `[CERT-hw]`

Consolidación de B156+B157+B158 — lo que se sabe de la station VIVA sin haber tocado nada:

| Dimensión | Estado vivo | Bloque |
|---|---|---|
| Puertos | 4911 foxs, 443 https, 80→https, 3011/5011 platform-HTTP(S), +8080 vecino Embedthis | B156 §156.1 |
| Identidad | `app.name=Station`, host `DESKTOP-4AAQ77H` / `192.168.100.100`, Fox proto 1.0.2 | B156 §156.2 |
| TLS | cert default `ForRecoveryPurposes` (mismo en 3 puertos), TLS1.3-only, HSTS 2a | B156 §156.3-4 |
| Auth | usuario `API` = **HTTPBasicScheme** (Basic directo, sin SCRAM) | B157 §157.1 |
| Superficie web | `/ord/*` viva; `/spy /about /nav /hx` no montados; `/wb` 403 | B157 §157.2 |
| **Reflow vivo** | servlet en **`/nmodsreflow/`**; `config` → 200 JSON read-level; `file`,`reflow`,`rc` → 200 | B157 §157.3-4 |
| Platform | 3011/5011 HTTP(S) guardado (403 sin creds de plataforma) | B158 §158.1 |
| Versión | **no disclosed** a read-level (ausencia probada) | B158 §158.2 |

`[INFER]` **Etapa A completa** para lo alcanzable con el usuario de prueba: el runtime vivo está mapeado y los
paths reales de Reflow (`/nmodsreflow/*`) están fijados. Lo que queda es **Etapa B terminal**: verificar los 14
defectos de [Block 150] §150.2 contra estos paths. Los de LECTURA (V6/V8/V11-read/V13) son rung-1 autenticado;
los de ESCRITURA/destructivos (V1-V5, V7, V9, V10, V12) requieren la escalera supervisada §12
(backup-before-destroy + oracle cross-canal + OK por paso). A2 (versión) queda abierto pero NO bloquea Etapa B.

## 158.4 — Connections

- **[Block 156]** / **[Block 157]** — este bloque cierra el mapa que ambos abrieron (perfil pasivo + auth).
- **[Block 124]-[Block 130]** (platform-native) — la superficie nativa de plataforma que aquel focus estudió
  estáticamente aquí se ve viva como HTTP(S) guardado en 3011/5011 (§158.1).
- **[Block 150]** §150.2 — la tabla de 14 defectos es el backlog de Etapa B, ahora sobre paths reales
  `/nmodsreflow/*` (B157 §157.3).
- **Focus `live-station`** — Etapa A CERRADA (A1/A3/A4 cerrados, A5 cerrado, A2 blocked-on-creds); próximo:
  Etapa B terminal supervisada.
