# JACE-8000 CASINO — Estado real (nuevo de fábrica) y siguientes pasos

| Campo | Valor |
|-------|-------|
| **Fecha** | 2026-07-19 |
| **Proyecto (obra)** | CASINO |
| **Corpus / repo** | niagara-research (parte práctica: comisionamiento en sitio) |
| **Equipo** | JACE-8000 / WEB-8000 "CASINO" — 192.168.1.140 |
| **Host ID** | **Qnx-TITAN-44A2-A77A-8526-363E** (Status: Perpetual) |
| **Estación de ingeniería** | PC DESKTOP-4AAQ77H (PRUEBAS), Optimizer Supervisor N4.14.0.162 |

> **Corrección respecto a la versión anterior de este documento:** las capturas del 2026-07-19
> demuestran que el JACE está **nuevo de fábrica** — NO tenía una Fase 0 comisionada ni una
> station corriendo. También se confirmó que **NO es el JACE del Plan de 8 Spyder** (ese es
> Host ID `BE9D`, N4.12; éste es `44A2`, sin runtime N4). Son equipos distintos.

## Estado real del equipo (verificado por capturas)

Fuente: `img/2026-07-19-license-manager.png`, `img/2026-07-19-application-director-vacio.png`,
`img/2026-07-19-platform-administration.png`, `img/2026-07-19-software-manager.png`.

| Dato | Valor observado | Implicación |
|------|-----------------|-------------|
| Host ID | `Qnx-TITAN-44A2-A77A-8526-363E` | Con este ID se pide/valida la licencia |
| Host ID Status | **Perpetual** | El equipo tiene derecho de licencia perpetua (no vencido) |
| Licenses (License Manager) | **Vacío** — solo `Tridium.certificate` | ⚠️ **Falta instalar la licencia** |
| Application Director | **Sin stations** | ⚠️ **No hay ninguna station instalada** |
| Daemon Version | 4.9.1.30 | Daemon base de fábrica |
| Operating System | tridium-qnx7-n4-titan-am335x-hs **4.9.1.18** | Firmware de fábrica, sin actualizar |
| Niagara Runtime | **Unknown** | ⚠️ **El runtime N4 no está instalado** |
| Enabled Runtime Profiles | **rt** (solo) | Faltan UX / WB |
| Software Manager | Todos los módulos = "Not Installed (**Requires Commissioning**)" | Confirma: falta comisionar |
| Local Date / Time Zone | **31 ene 2021** / UTC (+0) | ⚠️ Fecha/hora incorrectas → sincronizar |
| Model / Product / Serial | TITAN / JACE-8000 / None | — |
| Aviso | **WARNING: HTTP enabled** | ⚠️ Endurecer: usar solo HTTPS/TLS |
| RAM / Disco | 1 GB (504 MB libre) / 3.3 GB libre | OK |

**Diagnóstico:** JACE-8000 nuevo de fábrica. Ya se puede abrir el Platform (login funciona), pero
falta **todo el commissioning**: licencia + runtime N4 + módulos + fecha + perfiles UX/WB + crear
la station. El nodo "Station (CASINO)" que aparece en el Nav es una **definición de conexión**, no
una station instalada (el Application Director está vacío).

## Prerequisitos antes de comisionar

- Workbench (Optimizer Supervisor N4.14) instalado y **licenciado en la PC** → ✅ ya conectado.
- **Acceso a internet en la PC**: el wizard busca la licencia por Host ID `44A2…` en el licensing
  server de Honeywell/Tridium. Sin internet → instalar la licencia desde archivo `.license`.
- **Distribution N4** del JACE disponible en el Workbench, en versión compatible con la licencia.
- La major version del Workbench debe coincidir con la del controlador (N4 con N4).

## Siguientes pasos — Commissioning del JACE (fuente: *JACE Niagara 4 Install and Startup Guide*, docJaceN4Startup)

### Paso 1 — Anotar el Host ID
`Qnx-TITAN-44A2-A77A-8526-363E`. Es la clave para pedir/instalar la licencia. La MAC **no** sirve para esto.

### Paso 2 — Change Platform Defaults (si no se hizo)
Al conectar por Platform, Workbench obliga a: crear **System Passphrase**, crear **nueva cuenta de
platform** y **eliminar la cuenta de fábrica**. Anotar la passphrase en lugar seguro.

### Paso 3 — Commissioning Wizard
`Nav → clic derecho en Platform → Commissioning Wizard`. Vienen todos los pasos preseleccionados
(menos lexicons). En orden:

1. **Request or install software licenses** → *Install licenses from the license server* (busca por Host ID `44A2…`). Sin internet: *Install one or more licenses from files* y elegir el `.license`. El tool impide instalar una licencia de otro Host ID.
2. **Install certificates** → instalar el `Tridium.certificate` (ya presente).
3. **Set enabled runtime profiles** → RUNTIME (fijo) + **UX** + **WB** (al marcar UX se marca WB).
4. **Install a station from local computer** → *Don't transfer a station* (se crea después) o instalar una si ya existe.
5. **Install lexicons** → *omitir* (recomendado).
6. **Install/upgrade modules** → core preseleccionados + los del proyecto (BACnet, `honeywellSpyderTool`, etc. si aplican).
7. **Install/upgrade core software from distribution files** → instala el **runtime N4** (resuelve *Niagara Runtime: Unknown*). Read-only para equipo nuevo.
8. **Sync with local date and time** → corrige la fecha (31-ene-2021 → hoy) y la zona horaria.
9. **Configure TCP/IP network settings** → hostname, IP definitiva de la red del cliente, máscara, gateway, DNS.
10. **Remove platform default user account** → obligatorio (no se puede comisionar con la cuenta de fábrica).
11. **Configure additional platform daemon users** → opcional.
12. **Review → Finish** → el JACE aplica todo y **reinicia** (esperar ~20-40 min en un equipo nuevo).

### Paso 4 — Crear y arrancar la Station
Tras el reboot y reconexión:
- `New Station Wizard` para crear la station, o instalar una existente con el **Station Copier**.
- Arrancarla desde **Application Director**, con **AUTO-START** habilitado.

### Paso 5 — Endurecimiento
- Deshabilitar **HTTP** (dejar solo HTTPS/TLS) → resuelve el `WARNING: HTTP enabled`.
- Migrar la conexión de platform a **TLS** (puerto 5011) una vez comisionado.

### Paso 6 — (Si aplica a este proyecto) enlace de dispositivos de campo
Recién con la station corriendo se agregan drivers/redes (BACnet, etc.). El enlace de 8 Spyder
del `Plan_Comisionamiento_Spyder_WEB8000.docx` corresponde a **otro** JACE (Host ID `BE9D`), no a éste.

## Manual generado

Se generó el manual de arranque en Word: **`Manual_Inicio_JACE8000_CASINO.docx`** (esta carpeta),
basado en el *JACE Niagara 4 Install and Startup Guide* oficial + el estado real de este equipo.

## Pendiente de confirmar

- [ ] ¿Hay internet en la PC para bajar la licencia del server, o hace falta el archivo `.license` del Host `44A2…`?
- [ ] Versión exacta del distribution N4 a instalar (la determina la licencia).
- [ ] Módulos del proyecto CASINO a instalar (¿driver BACnet? ¿Spyder? ¿otros?).
- [ ] Hostname e IP definitiva de la red del cliente.
- [ ] ¿Se reutiliza una station existente o se crea nueva?
