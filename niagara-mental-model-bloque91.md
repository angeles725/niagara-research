# Bloque 91 — `honPlantControllerHMI` / `platHMI`: stack BACnet/MSTP propietario al panel HMI físico (JNI `hminpsdk`) + protocolo de aplicación sobre ConfirmedPrivateTransfer + crypto rota, deofuscado

> Investigación empírica de **`honPlantControllerHMI`** y su framework **`platHMI`** (347 java): el canal de comunicación entre el controlador BEATS ADV ([Bloque 90]) y su **panel HMI físico** (touch panel embebido Honeywell). Resultó ser un **stack BACnet/MS/TP propietario completo** (no el driver BACnet estándar de Tridium) con JNI nativo, un protocolo de aplicación RPC sobre `ConfirmedPrivateTransfer`, y un esquema de key-exchange con **defectos criptográficos graves verificados**.
>
> Fuentes: `organized/honPlantControllerHMI/honPlantControllerHMI-rt/vineflower/com/honeywell/{platHMI,honplantcontroller/hmi}/...`.
> Método: 1 sub-agente Explore + **verificación directa** de cada `extends`, `loadLibrary`, y lectura del código real de los 3 hallazgos crypto críticos (salt, IV, PBKDF2). `[CERT]` = verificado por mí; `[CERT-a]` = cita del sub-agente (capas, service IDs, payloads) no re-verificada; `[INFER]` = deducción.
>
> Capa 22 (OEM deofuscados), continúa [Bloque 90] (mismo módulo padre, núcleo). Conecta [Bloque 88] (BEATS/Nano: también vendorId=17 + ConfirmedPrivateTransfer Honeywell — patrón compartido), [Bloque 77] (Sylk/BACnet MS/TP), [Bloque 75] (seguridad — este bloque aporta el caso crypto más fuerte del corpus).

---

## 91.1 — Qué es `platHMI` + clases raíz verificadas `[CERT]`

`platHMI` **no** es un exportador de config ni un protocolo TCP: es un **driver BACnet/MS/TP propietario** sobre puerto serial dedicado, con JNI nativo en el hardware layer y una capa RPC propia sobre `ConfirmedPrivateTransfer` (Honeywell vendor ID=17). `[CERT-a]` para el rol; `[CERT]` para la jerarquía:

| Clase | Declaración verificada (archivo:línea) | Rol |
|-------|----------------------------------------|-----|
| `BHMIPlatformServiceNpsdk` | `extends BPlatformService implements BHMIPlatformService` (`platHMI/BHMIPlatformServiceNpsdk.java:24`) | Servicio de plataforma; carga `libhminpsdk.so`, gestiona puerto serial, hilo listener MSTP |
| `BHMINetwork` | `extends BBasicNetwork implements BIHMICommHelperParent, BIBacnetExportObject` (`platHMI/network/BHMINetwork.java:164`) | Nodo de red del driver (singleton `BHMINetwork.network()`) |
| `BHMIMstpLinkLayer` | `extends BComponent implements HmiMstpListener` (`platHMI/comm/BHMIMstpLinkLayer.java:73`) | Link layer MS/TP (recibe frames del nativo por callback) |
| `BHonHMIDevice` | `extends BDevice implements IAmListener, BIHonHMIHelperInterface` (`platHMI/network/device/BHonHMIDevice.java:340`) | El panel HMI físico (estado de cert/UUID/sesión) |
| `BHMICommHandler` | `extends BComponent implements IUnconfirmedPrivateTransferSender, IConfirmedPrivateTransferSender` (`honplantcontroller/hmi/comm/BHMICommHandler.java:77`) | Orquestador: login, alarmas, certs, sync, file transfer |

**"Npsdk" = Native Platform SDK** `[CERT]`: `BHMIPlatformServiceNpsdk.java:90` → `System.loadLibrary("hminpsdk")` → `libhminpsdk.so`. Sólo Linux (las primitivas MS/TP de hardware —apertura de puerto, framing, baud— viven en C/C++). Distinto del `plantctrl.so` del núcleo ([Bloque 90]).

---

## 91.2 — El stack de capas `[CERT-a]`

```
Hardware UART
 └─ libhminpsdk.so (JNI)
     └─ BHMIPlatformServiceNpsdk        (bridge JNI, lock de puerto serial, hilo MstpTrunkListener)
         └─ BHMIMstpLinkLayer           (link layer MS/TP; HmiMstpFrame: srcAddr, data[], dataExpectingReply)
             └─ HMIComm / NpduProcessor (capa NPDU)
                 ├─ BHMIServerLayer      (APDU entrante; decodifica ConfirmedPrivateTransfer)
                 │    └─ HonPrivateTransferListener → HMIPrivateTransferListener (dispatch RPC)
                 └─ BHMIClientLayer      (APDU saliente: readProperty, writeProperty, confirmedPrivateTransfer)
```

`HonPrivateTransferListener` (singleton) enruta por BACnet **service choice** (`Map<Integer, Set<IPrivateTransferClient>>`, 18=Confirmed, 30=Unconfirmed) `[CERT-a]`. Nota: existe además `BHMIBacnetMstpLinkLayer extends BBacnetMstpLinkLayer` `[CERT-a]` — una extensión del driver BACnet **estándar** de Tridium para la red del plant controller, **distinta** del stack `platHMI` propietario.

---

## 91.3 — `platHMI/export`: descriptores BACnet (56 clases) `[CERT-a]`

No genera ficheros: es una jerarquía de **object descriptors** que exponen `BControlPoint` de Niagara como objetos BACnet al panel:
```
BHMIEventSource extends BComponent implements BIBacnetExportObject
 └─ BHMIPointDescriptor extends BHMIEventSource implements BIBacnetCovSource, BacnetPropertyListProvider
      ├─ Analog/Binary/MultiState × Input/Output/Value (+ Prioritized/Writable commandable)
      └─ BHon* (FastAccessList, EventLog…)
BHMIScheduleDescriptor (Boolean/Numeric/Enum/String) · BHMICalendarDescriptor · BHMINotificationClassDescriptor
BHMIExportTable (registro global, clave BHMIObjectIdentifier) · BHonHMIFastAccessList(s) (FALs para operadores)
```
Verificado `[CERT]` un nodo de la rama export: `BHonHMIDeviceExtDescriptor extends BHonCommonDeviceExtDescriptor implements BIHMIExportObject` (`platHMI/export/…:42`). Cada descriptor mapea punto→ObjectID con `presentValue/statusFlags/reliability/eventState/COV`; el registro vive en `BHMIExportTable` (`BHMINetwork.network().getHmiExportTable()`).

**datatypes (28) / enums (16) `[CERT-a]`**: primitivos BACnet reempaquetados como BComponents (`BHMIObjectIdentifier`, `BHMIDateTime`, `BHMIBitString`, `BHMIArray`, `BHMIObjectPropertyReference`…) + el frame físico `HmiMstpFrame`. Enums = enumerados BACnet estándar al sistema de tipos Niagara (`BHMIObjectType`, `BHMIPropertyIdentifier`, `BHMIErrorCode`, `BHMIEngineeringUnits`…).

---

## 91.4 — Protocolo de aplicación: 44 service IDs sobre ConfirmedPrivateTransfer `[CERT-a]`

La capa `services/request` re-implementa las primitivas APDU de BACnet (ConfirmedRequest, UnconfirmedRequest, ComplexAck, SimpleAck, Abort/Reject/Error). Dos servicios clave:
- **AtomicReadFile/AtomicWriteFile** (BACnet 6/7): transferencia de **archivos de idioma** (service 77 `REQUEST_LANGUAGE_FILE`) y firmware al panel.
- **ConfirmedPrivateTransfer** (BACnet 18, vendor 17): el **wrapper RPC central**; `serviceNumber` = opcode de aplicación (60–103). Todo el protocolo HMI viaja aquí.

Mapa de servicios de aplicación (`HMIPrivateTransferListener.handleConfirmedPrivateTransfer()` → switch → `BHMICommHandler.handle*()` → `byte[]`):

| Rango | Categoría |
|-------|-----------|
| 60–68 | Usuarios/auth (LOGIN, LOGOUT, MODIFY_PW, CREATE/DELETE/READ_USERS, **REQ_SALT(66)**, **REQ_RAND(67)**) |
| 70–71, 91, 103 | Alarmas (query points/unack, ack, present) |
| 77 | Archivo de idioma |
| 79–88 | Key exchange TLS-like (cert exchange, ECDH, verify, cipher change, derive/verify shared key) |
| 90, 94–102 | Dynamic commissioning sync (data/periodic sync, UUID r/w/notify, FAL sync) |

> **Patrón compartido con [Bloque 88]**: BEATS/Nano también usa ConfirmedPrivateTransfer con vendorId=17 Honeywell. Es el mecanismo RPC propietario común de la familia BEATS sobre BACnet.

---

## 91.5 — Seguridad: el key-exchange está roto `[CERT]` (leído en código)

`platHMI` implementa su propio handshake (cert X.509 + ECDH + AES-CBC + PBKDF2). Verifiqué los tres defectos críticos leyendo el código, no por cita:

**[CRÍTICO CERT] REQ_SALT (service 66) devuelve siempre 32 bytes de ceros.** `BHMICommHandler.java:351`:
```java
public byte[] handleRequestSalt() throws Exception {
   byte[] encryptedSalt = new byte[64];   // declarado y NUNCA usado
   byte[] rand = new byte[32];             // NUNCA se randomiza
   AsnOutputStream asnOut = new AsnOutputStream();
   asnOut.writeOctetString(rand);          // escribe 32×0x00
   return asnOut.toByteArray();
}
```
Contraste directo: `handleRequestRand()` (línea 343) **sí** hace `new SecureRandom().nextBytes(rand)`. El salt es predecible → invalida cualquier derivación de clave que dependa de él.

**[CRÍTICO CERT] IV de AES-CBC constante = `0x55…` y compartido.** `BHonPlantControllerHMIInitializationHandler.java:196`:
```java
public static byte[] createIV() {
   byte[] rand = new byte[16];
   for (int i = 0; i < rand.length; i++) rand[i] = 85; // 0x55
   return rand;
}
```
Y es `static IvParameterSpec iv = new IvParameterSpec(createIV())` (línea 33) → **el mismo IV para todo AES-CBC** (login, key exchange, cambio de cipher). Viola CBC (IV único e impredecible por mensaje) → habilita replay y análisis de patrones entre sesiones.

**[ALTO CERT] PBKDF2 con 2 iteraciones.** `BHonPasswordEncoder.java:35`: `private static final int ITERATION_COUNT = 2;` — costo trivial para un atacante (NIST SP 800-132 recomienda ≥600 000).

**[ALTO CERT-a] Inconsistencia encode/validate** en `BHonPasswordEncoder`: un `encode(SecretChars)` hace un esquema manual SHA-256(pwd+salt)→SHA-256(pwd+hash) mientras `validate()` usa `Pbkdf2.deriveKey()` → contraseñas guardadas por ese path nunca validan. Además `MessageDigest.getInstance("Sha-256")` (typo del nombre JCE estándar "SHA-256") puede lanzar `NoSuchAlgorithmException` en JVM estrictas.

**[MEDIO CERT-a] `static byte[] sharedKeyBytes` no-final** (race expone clave parcialmente inicializada). UUID service 100 con rama `else` muerta (mismo retorno para match y no-match). `REF_HMI_FW_VERSION = "HMI_FW_v1.5.1.27"` hardcodeada como gate de actualización.

**[BAJO INFER] REQ_SALT/REQ_RAND sin check de autorización**: cualquier nodo del bus MS/TP que envíe ConfirmedPrivateTransfer vendor 17 obtiene material de autenticación (sin verificar dirección fuente ni sesión previa).

---

## 91.6 — Conexiones

- **[Bloque 90]**: mismo módulo padre (`honPlantController`). El núcleo habla con la app móvil BLE (BTP); este habla con el panel HMI físico (BACnet/MSTP serial). Dos JNI distintos: `plantctrl.so` vs `hminpsdk.so`.
- **[Bloque 88]** (BEATS/Nano): patrón RPC compartido — ConfirmedPrivateTransfer + vendorId=17 Honeywell sobre BACnet.
- **[Bloque 77]** (Sylk/BACnet MS/TP): mismo transporte serial en la cadena de campo.
- **[Bloque 75]** (seguridad): este bloque aporta el **caso criptográfico más fuerte del corpus** — handshake propietario con salt cero, IV constante y PBKDF2 trivial, todos verificados en fuente.
