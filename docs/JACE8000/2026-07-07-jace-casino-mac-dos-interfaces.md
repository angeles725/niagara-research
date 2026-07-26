# JACE-8000 CASINO — MAC address y las dos interfaces Ethernet

| Campo | Valor |
|-------|-------|
| **Fecha** | 2026-07-07 |
| **Proyecto (obra)** | CASINO — Enlace de 8 Spyder al WEB-8000 |
| **Corpus / repo** | niagara-research (parte práctica: comisionamiento en sitio) |
| **Equipo** | JACE-8000 / WEB-8000 "CASINO" — 192.168.1.140 |
| **Host ID** | Qnx-TITAN-44A2-A77A-8526-363E (Status: Perpetual) — *confirmado el 2026-07-19 en License Manager* |
| **Estación de ingeniería** | PC DESKTOP-4AAQ77H (PRUEBAS), Optimizer Supervisor N4.14 |

> Registro de la sesión del 2026-07-07 (transcript `819d8ac9`). Duda planteada en obra mientras
> se revisaba el panel de red del JACE: *"¿cuál es la MAC del JACE8000? ¿Por qué tiene dos interfaces?"*

## Contexto

Durante la conexión al JACE se abrió el panel de red (TCP/IP Configuration / interfaces) y
aparecían **dos "Physical Address"**, una por cada puerto Ethernet. La duda: cuál es "la" MAC
y por qué hay dos.

## Respuesta

### 1. En Niagara, "Physical Address" = MAC address

El JACE-8000 tiene **dos puertos Ethernet físicos onboard**, y cada uno tiene su propia MAC:

| Interface | ID | Physical Address (MAC) | Estado | IP |
|-----------|-----|------------------------|--------|-----|
| Interface 1 | `en0` (LAN1 / PRI) | `3C:E4:B0:EB:96:10` | Enabled | 192.168.1.140 |
| Interface 2 | `en1` (LAN2 / SEC) | `3C:E4:B0:EB:96:12` | Disabled | — |

Las dos MAC son **consecutivas** (`…96:10` y `…96:12`): el fabricante asigna un bloque
contiguo por dispositivo. Es normal.

### 2. Por qué dos interfaces

El JACE-8000 es un controlador de red, no un dispositivo simple. Los dos puertos existen para
**segmentar tráfico**:

- **`en0` (LAN1 / PRI)** → red de supervisión / IT — acá vive `192.168.1.140`, hacia el Supervisor / Workbench.
- **`en1` (LAN2 / SEC)** → normalmente una segunda red aislada (BACnet/IP de campo, otro segmento de controladores).

En este equipo `en1` está **deshabilitado** (`Adapter Enabled` sin tilde) → el JACE opera en
config de un solo brazo, perfectamente válida.

### 3. ⚠️ La MAC NO es el Host ID de la licencia

Error clásico. Para licenciar Niagara y para el binding del `.license` NO se usa la MAC, sino
el **Host ID** (identificador único del hardware, formato `Qnx-TITAN-XXXX-…`). Se consulta en
**Platform → License Manager** o en Platform Administration. Confundir MAC con Host ID lleva a
pedir/instalar la licencia equivocada.

## Notas para este equipo (CASINO)

- El Host ID real de este JACE, confirmado en License Manager el 2026-07-19, es
  **`Qnx-TITAN-44A2-A77A-8526-363E`** — **NO** es el `BE9D` del Plan de 8 Spyder. Son equipos
  distintos. (La MAC vista acá, `3C:E4:B0:EB:96:10`, nunca determina el Host ID; esto confirma
  justamente el punto 3 de arriba.)
- LAN2 (`en1`) queda deshabilitada salvo que se requiera un segundo segmento de red.
