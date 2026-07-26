# Manual de Comisionamiento — Honeywell JACE 8000

**Niagara N4 v4.14 | Primera Conexión y Configuración**

- **Proyecto:** Integración UN-RL1644ES24NM (Optimizer Unitary)
- **Elaborado por:** Ingeniería Alser
- **Fecha:** Marzo 2026

> Convertido a Markdown desde `documentacion-jace8000.docx` (manual original de Alser). El `.docx` acompaña este archivo en `docs/` para impresión/entrega.

> ⚠️ **CONFIDENCIAL:** contiene credenciales, Host ID y MAC reales del proyecto. No compartir con personal no autorizado.

## Índice de contenido

| Sección | Descripción | Página |
|--------:|-------------|-------:|
| 1 | Descripción del Equipo y Requisitos | 3 |
| 2 | Configuración de Red en la PC | 4 |
| 3 | Descubrimiento de IP del JACE | 5 |
| 4 | Acceso por Consola Serial (PuTTY) | 6 |
| 5 | Change Platform Defaults Wizard (Serial) | 7 |
| 6 | Factory Recovery — Recuperación de Fábrica | 8 |
| 7 | Primera Conexión desde Niagara Workbench | 10 |
| 8 | Change Platform Defaults Wizard (Workbench) | 11 |
| 9 | Commissioning Wizard — Configuración Completa | 12 |
| 10 | Software Installation — Módulos Instalados | 14 |
| 11 | Verificación Final del Sistema | 16 |
| 12 | Credenciales y Resumen de Configuración | 17 |

---

## 1. Descripción del Equipo y Requisitos

### 1.1 Hardware Principal

El Honeywell JACE 8000 (modelo WEB-8000, también denominado TITAN) es un controlador de automatización de edificios basado en Niagara 4 (N4). Actúa como servidor de estación, gateway de protocolos y supervisor de campo.

| Parámetro | Valor |
|-----------|-------|
| Producto | JACE-8000 (TITAN / WEB-8000) |
| Host ID | Qnx-TITAN-BB4C-D480-3C70-ACE4 |
| MAC Address (LAN1) | 34:08:E1:05:A6:DB |
| IP Default (LAN1) | 192.168.1.140 |
| Puerto Platform HTTP | 3011 |
| Puerto Platform HTTPS | 5011 |
| Puerto Fox (Station) | 4911 |
| Sistema Operativo | QNX 7 (tridium-qnx7-n4-titan-am335x) |
| Daemon Version | 4.9.1.30 → actualizado a 4.14.0.162 |
| Java VM | Azul ZRE Compact3 QNX7 ARM (1.8.0.412.20) |
| RAM Total | 1,048,576 KB (1 GB) |
| Almacenamiento Total | 3,492,848 KB (~3.4 GB SD Card) |
| Debug USB | Micro-USB / 115200 baud |

### 1.2 Controlador a Integrar

| Parámetro | Valor |
|-----------|-------|
| Modelo | Honeywell UN-RL1644ES24NM (Optimizer Unitary) |
| Tipo | Controlador programable de propósito general |
| I/O | 16 UI + 4 SSR + 4 Relay = 24 puntos totales |
| Protocolo principal | BACnet IP (puertos RJ45 x2 para daisy chain) |
| Puerto RS-485 | Para dispositivos Modbus externos (sensores, IO) |
| Herramienta de programación | Honeywell Spyder Tool (dentro de Niagara) |

### 1.3 Software Requerido

- Niagara Workbench N4.8 o superior (instalado en PC de ingeniería)
- PuTTY (terminal emulation — para acceso serial por USB micro)
- Advanced IP Scanner (para descubrir la IP del JACE en red)

### 1.4 Licencias Instaladas

| Licencia | Versión | Expiración |
|----------|---------|------------|
| Honeywell.license | Honeywell 4.15 | Never expires |
| Webs.license (Tridium) | Tridium 4.15 | Never expires |
| HoneywellCentraLine.license | HoneywellCentraLine 4.15 | Never expires |

---

## 2. Configuración de Red en la PC

Para conectarse al JACE 8000 por primera vez usando un cable Ethernet directo (sin switch/router), la PC debe configurarse en la misma subred que la IP default del JACE.

### 2.1 Configuración del Adaptador Ethernet

| Campo | Valor |
|-------|-------|
| IP de la PC (Adaptador Ethernet) | 192.168.1.50 (cualquier valor excepto .140) |
| Máscara de Subred | 255.255.255.0 |
| Puerta de Enlace | 192.168.1.1 (o dejar vacío) |
| DNS | No requerido |

### 2.2 Ruta en Windows

1. Panel de Control → Centro de Redes y Recursos Compartidos
2. Cambiar configuración del adaptador → Adaptador Ethernet
3. Propiedades → Protocolo de Internet versión 4 (TCP/IPv4)
4. Seleccionar 'Usar la siguiente dirección IP' e ingresar los valores

> ⚠️ **NOTA:** En esta sesión se identificó que la PC de ingeniería tiene múltiples adaptadores de red (Hyper-V virtual, ProtonVPN TAP, Wi-Fi). Es crítico asignar la IP estática al adaptador físico **'Ethernet 2' (Realtek PCIe GbE)** — NO al adaptador Wi-Fi ni a adaptadores virtuales.

### 2.3 Verificación de Conectividad

Una vez configurado el adaptador y conectado el cable Ethernet al puerto LAN1 del JACE, verificar con:

```
ping 192.168.1.140
```

Respuesta exitosa: 4 paquetes enviados, 0% perdidos.

### 2.4 Estado de LEDs del JACE durante conexión

| LED | Estado Normal | Significado |
|-----|---------------|-------------|
| STAT | Verde sólido | JACE encendido y operativo |
| BEAT | Amarillo parpadeando 1Hz | Niagara corriendo (heartbeat) |
| LAN1 Link | Verde o Ámbar | Ámbar = 100 Mbps, Verde = 1 Gbps |
| LAN1 Activity | Parpadeando | Tráfico de datos activo |

---

## 3. Descubrimiento de IP del JACE

El JACE 8000 de fábrica tiene asignada la IP 192.168.1.140 en LAN1. Si el dispositivo toma una dirección DHCP o diferente, usar Advanced IP Scanner para localizarlo por su MAC address.

> Advanced IP Scanner: JACE 8000 detectado en 192.168.1.140 (fabricante: Texas Instruments — procesador del JACE).

### 3.1 Identificación del JACE en el Scan

| Campo | Valor |
|-------|-------|
| Nombre | 192.168.1.140 (sin nombre DNS asignado aún) |
| IP | 192.168.1.140 |
| Fabricante identificado | Texas Instruments (procesador interno del JACE) |
| MAC Address | 34:08:E1:05:A6:DB |

> ⚠️ **NOTA:** El fabricante 'Texas Instruments' es correcto — el JACE-8000 usa un procesador Cortex A8 de TI (AM335x). Este es el método más rápido para localizar la IP real del JACE sin acceso serial.

---

## 4. Acceso por Consola Serial (PuTTY)

El puerto USB micro del frente del JACE (etiquetado DEBUG) permite acceso a la consola serial del sistema QNX. Es el método de recuperación cuando no hay acceso de red.

### 4.1 Requisitos de Hardware

- Cable USB Type-A a Micro-USB (mismo que smartphones Android antiguos)
- PC con PuTTY instalado
- JACE encendido y con el cable USB conectado al puerto DEBUG (frontal)

### 4.2 Configuración de PuTTY

| Parámetro | Valor |
|-----------|-------|
| Connection type | Serial |
| Serial line (COM port) | COM3 (verificar en Administrador de Dispositivos) |
| Speed (Baud rate) | 115200 |
| Data bits | 8 |
| Stop bits | 1 |
| Parity | None |
| Flow control | None |

### 4.3 Credenciales de Fábrica — Consola Serial

| Sistema | Usuario | Password |
|---------|---------|----------|
| Platform Daemon (Honeywell) | honeywell | webs |
| Platform Daemon (Tridium) | tridium | niagara |
| Shell QNX (root) | root | (vacía — solo Enter) |

> ⚠️ **ADVERTENCIA:** El JACE 8000 Honeywell usa **'honeywell' / 'webs'** como credenciales de fábrica del platform daemon — NO 'admin' / 'admin'. Intentar credenciales incorrectas más de 2 veces en el wizard de passphrase bloquea el acceso y requiere reboot.

---

## 5. Change Platform Defaults Wizard (Serial)

Al conectarse por primera vez vía serial con las credenciales de fábrica, el sistema muestra el Change Platform Defaults Wizard. Este wizard es obligatorio y debe completarse antes de poder usar el JACE.

### 5.1 Pasos del Wizard en Consola Serial

**Paso 1: Change the system passphrase**

El wizard solicita la passphrase actual del sistema. En el JACE Honeywell recién instalado, la passphrase de sistema corresponde a las mismas credenciales del platform.

> Error 'invalid current passphrase' — la passphrase de sistema es diferente a la password del platform.

> ⚠️ **NOTA:** La system passphrase del JACE 8000 Honeywell NO es la misma que la password de platform. Si el wizard serial no acepta ninguna passphrase conocida, la solución es proceder con el Factory Recovery desde Workbench.

**Paso 2: Create new platform account**

Crear una nueva cuenta de administrador del platform (sustituye la cuenta 'honeywell' de fábrica).

**Paso 3: Remove the default platform account**

El wizard elimina automáticamente la cuenta 'honeywell' de fábrica por seguridad.

---

## 6. Factory Recovery — Recuperación de Fábrica

El Factory Recovery es el procedimiento para restaurar el JACE a sus valores de fábrica cuando no es posible completar el commissioning normal, ya sea por passphrase desconocida, archivos corruptos, o boot loop.

### 6.1 Síntoma que Requiere Factory Recovery

El siguiente error en el boot log indica archivos de seguridad corruptos — el niagarad no puede arrancar:

```
AccessControlException: access denied ("java.util.PropertyPermission" "niagara.user.home" "read")
```

### 6.2 Procedimiento de Factory Recovery

1. Apagar el JACE (desconectar alimentación 24V)
2. Abrir la tapa frontal del JACE (flip panel) y localizar el botón BACKUP/RESTORE
3. Presionar y MANTENER el botón BACKUP/RESTORE mientras se conecta la alimentación
4. Mantener el botón presionado hasta que el LED BACKUP parpadee rápido (100ms ON / 100ms OFF)
5. Soltar el botón. El sistema inicia countdown de 10 segundos
6. NO presionar ninguna tecla — dejar que el countdown llegue a 0 para iniciar Factory Recovery automático
7. Esperar hasta que aparezca 'Please cycle power to proceed with recovery'
8. Apagar y encender el JACE (cycle power). El recovery procesa la imagen limpia automáticamente
9. Esperar 20-40 minutos hasta que el boot log muestre 'niagarad startup complete'

> ⚠️ **ADVERTENCIA:** NO apagar el JACE mientras el LED BACKUP parpadea lento (1s ON / 1s OFF). Interrumpir el proceso puede dejar el controlador inoperable.

### 6.3 Resultado del Factory Recovery Exitoso

| Mensaje en Boot Log | Significado |
|---------------------|-------------|
| Username/password reset to factory defaults | Credenciales restauradas a honeywell/webs |
| Cleaning JACE filesystem, N4 v4.9U1 | Sistema de archivos limpiado |
| Clean complete | Restauración completa |
| niagarad startup complete | Niagara arrancó correctamente |
| IP: 192.168.1.140 | IP de fábrica restaurada |

---

## 7. Primera Conexión desde Niagara Workbench

Una vez que el JACE ha completado el Factory Recovery y la IP 192.168.1.140 está activa, conectarse desde Niagara Workbench usando Open Platform.

### 7.1 Abrir Platform desde Workbench

Menú File → Open → Open Platform en Honeywell Optimizer Supervisor (Niagara Workbench).

### 7.2 Parámetros de Conexión al Platform

| Campo | Valor |
|-------|-------|
| Type | Platform TLS Connection |
| Host | 192.168.1.140 |
| Port | 5011 (HTTPS Platform Daemon) |
| Username | honeywell (credencial de fábrica post-recovery) |
| Password | webs |

### 7.3 Verificación de Certificado

El certificado TLS del JACE recién restaurado es auto-generado por Niagara. Es normal que muestre 'could not be validated'. Hacer clic en **ACCEPT** para proceder.

---

## 8. Change Platform Defaults Wizard (Workbench)

Al conectarse por primera vez al JACE desde Workbench con credenciales de fábrica, el sistema lanza automáticamente el Change Platform Defaults Wizard. Este proceso es obligatorio.

### 8.1 Paso 1: Configure the System Passphrase

| Campo | Valor usado |
|-------|-------------|
| New Passphrase | Alser12345 (mínimo 10 caracteres, 1 mayúscula, 1 número, sin espacios) |
| Confirm New Passphrase | Alser12345 |

> ⚠️ **ADVERTENCIA:** Anotar y guardar la System Passphrase en lugar seguro. Si se pierde, se pierde acceso a datos cifrados y se requiere nuevo Factory Recovery.

### 8.2 Paso 2: Create a Platform Account

| Campo | Valor |
|-------|-------|
| New Username | admin |
| New Password | Alser12345 |
| Confirm Password | Alser12345 |

### 8.3 Paso 3: Review and Finish

La pantalla de Review muestra el resumen de cambios:

- Update the platform system passphrase
- Add the platform user account: admin
- Remove the platform user account: honeywell (default account)

Hacer clic en **FINISH** para aplicar los cambios.

---

## 9. Commissioning Wizard

El Commissioning Wizard es el proceso central de configuración del JACE. Instala el software, actualiza el sistema operativo, configura la red y registra las licencias.

### 9.1 Selección de Tareas Recomendadas

| Opción | Estado | Justificación |
|--------|--------|---------------|
| Request or install software licenses | MARCAR | Instalar las licencias del JACE |
| Set enabled runtime profiles | AUTO (gris) | Se configura automáticamente |
| Install a station from local computer | NO MARCAR | Station se crea después del commissioning |
| Install lexicons (idiomas) | NO MARCAR | No requerido para operación básica |
| Install/upgrade modules | AUTO (gris) | Se instala automáticamente |
| Install/upgrade core software | AUTO (gris) | Se instala automáticamente |
| Sync with local date and time | AUTO (gris) | Sincroniza con la fecha/hora de la PC |
| Configure TCP/IP network settings | MARCAR (opcional) | Si se requiere cambiar la IP del JACE |
| Configure system passphrase | NO MARCAR | Ya configurada en el paso anterior |
| Configure additional platform users | MARCAR (opcional) | Para agregar usuario adicional 'Alser' |

### 9.2 Enabled Runtime Profiles

| Perfil | Seleccionado | Descripción |
|--------|--------------|-------------|
| RUNTIME | Sí (fijo) | Módulos core Java — siempre activo |
| UX | Sí | Interfaz web HTML5/JavaScript — permite acceso desde browser |
| WB | Sí (se activa con UX) | Clases para Workbench — necesario para administración |
| SE | No disponible | Java SE completo — no disponible en JACE |
| DOC | No | Solo documentación — no necesario |

> ⚠️ **NOTA:** En el JACE 8000 Honeywell, los perfiles UX y WB están vinculados — al marcar UX se marca WB automáticamente y viceversa. Esto es comportamiento normal.

### 9.3 Licensing — Instalación de Licencias

Para el JACE 8000 Honeywell con licencias pre-cargadas en la SD card:

- **Con conexión a internet:** seleccionar 'Install INDIVIDUAL WEB-8000 (INCLUDES USD CARD)' — instala las 3 licencias automáticamente desde el servidor Honeywell.
- **Sin conexión a internet:** seleccionar 'Install one or more licenses from files' y navegar al archivo .jar de licencia.

---

## 10. Software Installation — Módulos Instalados

La pantalla Software Installation muestra todos los módulos Niagara disponibles para instalar en el JACE. Los módulos marcados en rojo son los requeridos por los Runtime Profiles seleccionados.

### 10.1 Módulos Críticos para Integración UN-RL1644ES24NM

| Módulo | Versión | Función |
|--------|---------|---------|
| bacnet-rt | Tridium 4.14.0.162 | Driver BACnet IP runtime — comunicación con controladores BACnet |
| bacnet-ux | Tridium 4.14.0.162 | Interfaz web BACnet |
| bacnet-wb | Tridium 4.14.0.162 | Interfaz Workbench BACnet |
| honeywellSpyderTool | Honeywell 4.14.0.10.5.64 | Herramienta de programación de lógica en Spyder/Optimizer |
| honeywellFunctionBlocks-rt | Honeywell 4.14.0.1.6.3 | Bloques de función Honeywell para lógica de control |
| honeywellFunctionBlocks-ux | Honeywell 4.14.0.1.6.3 | Interfaz web para Function Blocks |
| honeywellFunctionBlocks-wb | Honeywell 4.14.0.1.6.3 | Interfaz Workbench para Function Blocks |

### 10.2 Módulos NO Requeridos para este Proyecto

| Módulo | Razón para Excluir |
|--------|--------------------|
| modbus* (todos) | El UN-RL1644ES24NM ES usa BACnet IP — Modbus no aplica |
| honeywellBacnetSpyder | Solo para Spyder vía BACnet MS/TP — no aplica para modelo ES |
| honeywellLonSpyder | Solo para redes LON |
| honeywellSylkDevice* | Solo para bus Sylk |
| Cloud* / cloudConnector* | Sin conectividad cloud en este proyecto |

### 10.3 Distribution File Installation

| Archivo | Versión | Descripción |
|---------|---------|-------------|
| nre-config-titan-am335x | Tridium 4.14.0.162 | Configuración de hardware específica del JACE |
| nre-core-qnx7-armle-v7 | Tridium 4.14.0.162 | Core NRE para QNX7 ARM |
| azul-zre-compact3-qnx7-arm | Azul Systems 1.8.0.412.20 | Java Runtime Environment actualizado |
| tridium-qnx7-n4-titan-am335x | Tridium 4.14.0.24 | Distribución completa Niagara 4 para JACE |

---

## 11. Verificación Final del Sistema

Una vez completado el Commissioning Wizard y el reboot final del JACE, verificar que todos los servicios estén activos y el sistema esté operativo.

### 11.1 Resumen del Commissioning Completado

| Paso | Resultado |
|------|-----------|
| Stop running applications | Success |
| Update authentication | Success |
| Update system passphrase | Success |
| Install files to remote host | Success — 140+ módulos instalados |
| Update system date/time/time zone | Success — sincronizado con PC (CST UTC-6) |
| Update operating system | Success — QNX7 actualizado |
| Reboot | Success — JACE reiniciado correctamente |

### 11.2 Verificación de Puertos de Red

| Puerto | Protocolo | Función | Estado |
|--------|-----------|---------|--------|
| 3011 | HTTP | Platform Daemon HTTP | Activo |
| 5011 | HTTPS/TLS | Platform Daemon HTTPS | Activo — Puerto de conexión principal |
| 4911 | Fox/TLS | Conexión a Station | Activo cuando hay station corriendo |
| 47808 | UDP | BACnet IP | Activo — para comunicación BACnet |

### 11.3 Verificación de Conectividad desde PowerShell

```powershell
Test-NetConnection 192.168.1.140 -Port 5011
```

Resultado esperado: `TcpTestSucceeded: True`

---

## 12. Credenciales y Resumen de Configuración

> ⚠️ **ADVERTENCIA:** Este documento contiene información confidencial. Guardar en lugar seguro y no compartir con personal no autorizado.

### 12.1 Credenciales del Sistema

| Sistema | Usuario | Password / Passphrase | Notas |
|---------|---------|-----------------------|-------|
| Platform Daemon (Workbench) | admin | Alser12345 | Cuenta principal de administración |
| Platform Daemon (adicional) | Alser | Alser12345 | Cuenta adicional configurada |
| System Passphrase | — | Alser12345 | Cifrado de la SD card |
| Station (a crear) | Por definir | Por definir | Se configura al crear la station |
| Fábrica (eliminada) | honeywell | webs | Cuenta de fábrica — ELIMINADA |

### 12.2 Configuración de Red

| Parámetro | Valor Actual | Valor en Campo |
|-----------|--------------|----------------|
| IP JACE LAN1 | 192.168.1.140 | Por definir según red del cliente |
| Máscara de Subred | 255.255.255.0 | Por definir |
| Gateway | 192.168.1.1 | Por definir |
| LAN2 | Deshabilitada | Habilitar si se requiere segunda red |
| WiFi | Deshabilitado | Mantener deshabilitado |

> ⚠️ **NOTA:** Para cambiar la IP del JACE al instalarlo en campo: Platform Administration → Change TCP/IP Settings → asignar IP de la red del cliente → el JACE hace reboot automático.

### 12.3 Software Instalado — Versión Final

| Componente | Versión |
|------------|---------|
| Niagara N4 Platform | 4.14.0.162 |
| NRE Core | 4.14.0.162 |
| Java Runtime (Azul ZRE) | 1.8.0.412.20 |
| QNX OS | 7 (tridium-qnx7-n4-titan-am335x-hs 4.9.1.18 → actualizado) |
| Licencia Honeywell | 4.15 — Never Expires |
| Licencia Webs (Tridium) | 4.15 — Never Expires |
| Licencia HoneywellCentraLine | 4.15 — Never Expires |
| Módulo bacnet | 4.14.0.162 |
| honeywellSpyderTool | 4.14.0.10.5.64 |
| honeywellFunctionBlocks | 4.14.0.1.6.3 |
| Runtime Profiles | rt, ux, wb |

### 12.4 Próximos Pasos

1. Crear nueva Station en el JACE: Nav → 192.168.1.140 → Platform → Application Director → New Station
2. Agregar BACnet Network en la Station para descubrir el UN-RL1644ES24NM
3. Configurar la IP del UN-RL1644ES24NM en la misma subred del JACE
4. Usar honeywellSpyderTool para programar la lógica de control en el Optimizer Unitary
5. Cambiar IP del JACE a la red definitiva del cliente antes de instalación en campo
6. Realizar backup de la station y la configuración del platform antes de entrega

---

*— Fin del Documento —*

*Alser Ingeniería | Manual JACE 8000 | Niagara N4 v4.14 | Marzo 2026*
