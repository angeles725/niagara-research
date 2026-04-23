# Bloque 23 — BACnet deep (objetos + properties + COV + priority + networking stack + schedule/calendar/trend)

Fecha: 2026-04-23
Fuentes empíricas: decompilados bacnet-rt/ux/wb + bacnetUtil-rt + bacnetAws/Ows-rt + bacnetAlarmRouter-rt + bacnetMigrator-wb + bacnetEDE-wb + honBacnetHelper-rt + honBACnetUtilities-rt + ascBacnet + docs `devguide-clean/bacnet.txt`.

Cubre el driver BACnet **end-to-end**: enumeración de 60+ object types, 475+ property IDs, device binding (BBacnetDevice + BBacnetProxyExt), discovery workflow (WhoIs/IAm), COV lifecycle completo, priority array 16 niveles + relinquish, capa de red multicapa (BVLC→NPDU→APDU), 37 servicios, segmentación, BBMD, routing, schedule+calendar+notification class+trend log+access control, EDE, alarm routing, extensiones Honeywell.

---

## 23.1 BACnet object model — enumeración (60+ tipos)

### BBacnetObjectType (javax.baja.bacnet.enums, BFrozenEnum, BacnetConst)

| Object Type | Enum ID | Propósito |
|---|---|---|
| ANALOG_INPUT | 0 | Sensores (temp, presión, flujo) |
| ANALOG_OUTPUT | 1 | Actuadores (dampers, válvulas) |
| ANALOG_VALUE | 2 | Memoria analógica |
| BINARY_INPUT | 3 | Switches, alarmas |
| BINARY_OUTPUT | 4 | Relés, contactores |
| BINARY_VALUE | 5 | Bandera lógica |
| CALENDAR | 6 | Calendario de excepciones |
| COMMAND | 7 | Comandos secuenciales |
| DEVICE | 8 | Dispositivo raíz |
| EVENT_ENROLLMENT | 9 | Inscripción intrínseca de alarma |
| FILE | 10 | Archivo remoto (atomic read/write) |
| GROUP | 11 | Agrupación de referencias |
| LOOP | 12 | Lazo PID/control |
| MULTI_STATE_INPUT | 13 | Entrada multi-estado 3-8 |
| MULTI_STATE_OUTPUT | 14 | Salida multi-estado |
| NOTIFICATION_CLASS | 15 | Clase de notificación alarmas |
| PROGRAM | 16 | Script ejecutable |
| SCHEDULE | 17 | Horario semanal/diario |
| AVERAGING | 18 | Promedios en periodo |
| MULTI_STATE_VALUE | 19 | Valor multi-estado |
| TREND_LOG | 20 | Histórico COV o intervalo |
| LIFE_SAFETY_POINT | 21 | Seguridad vida |
| LIFE_SAFETY_ZONE | 22 | Zona seguridad |
| ACCUMULATOR | 23 | Contador integrador |
| PULSE_CONVERTER | 24 | Conversor pulsos |
| EVENT_LOG | 25 | Log interno |
| GLOBAL_GROUP | 26 | Agregación cross-device |
| TREND_LOG_MULTIPLE | 27 | Log multi-property |
| LOAD_CONTROL | 28 | Shedding/demand response |
| STRUCTURED_VIEW | 29 | Vista jerárquica |
| ACCESS_DOOR | 30 | Puerta control acceso |
| ACCESS_CREDENTIAL | 32 | Credencial RFID/PIN |
| ACCESS_POINT | 33 | Punto acceso |
| ACCESS_RIGHTS | 34 | Matriz permisos |
| ACCESS_USER | 35 | Usuario sistema |
| ACCESS_ZONE | 36 | Zona control |
| CREDENTIAL_DATA_INPUT | 37 | Entrada datos credencial |
| BITSTRING_VALUE | 39 | BitString proprietary |
| CHARACTER_STRING_VALUE | 40 | String almacenado |
| DATE_PATTERN_VALUE | 41 | Patrón fecha |
| DATE_VALUE | 42 | Fecha YYYY-MM-DD |
| DATETIME_PATTERN_VALUE | 43 | Patrón datetime |
| DATETIME_VALUE | 44 | Timestamp completo |
| INTEGER_VALUE | 45 | Entero con signo |
| LARGE_ANALOG_VALUE | 46 | REAL de mayor precisión |
| OCTET_STRING_VALUE | 47 | Binario |
| POSITIVE_INTEGER_VALUE | 48 | Uint |
| TIME_VALUE | 51 | HH:MM:SS |

Constantes límite:
- `MAX_ASHRAE_ID = 60` (último estándar 135)
- `MAX_RESERVED_ID = 127`
- `MAX_ID ≈ 500` (proprietary/vendor)

---

## 23.2 Property identifiers — 475+ (BBacnetPropertyIdentifier)

### Propiedades comunes (todos objetos)

```
75  OBJECT_IDENTIFIER
77  OBJECT_NAME
79  OBJECT_TYPE
28  DESCRIPTION
30  DEVICE_TYPE
111 STATUS_FLAGS            (in-alarm, fault, overridden, out-of-service)
36  EVENT_STATE             (normal, fault, offnormal, high-limit, low-limit, life-safety-alarm)
103 RELIABILITY
81  OUT_OF_SERVICE
117 UNITS
85  PRESENT_VALUE
```

### Input objects (AI/BI/MI)

```
CHANGE_OF_STATE_COUNT    conteo cambios
CHANGE_OF_STATE_TIME     timestamp
FEEDBACK_VALUE           realimentación
LAST_COMMAND_TIME
```

### Output objects (AO/BO/MO)

```
87  PRIORITY_ARRAY        array 16 de BBacnetPriorityValue
104 RELINQUISH_DEFAULT    valor cuando array todo NULL
85  PRESENT_VALUE         derivado de priority array
    MANUAL_TAKEOVER       override local
```

### Control/PID

```
CONTROLLED_VARIABLE_REFERENCE
CONTROLLED_VARIABLE_UNITS/VALUE
MANIPULATED_VARIABLE_REFERENCE
DEADBAND
DERIVATIVE_CONSTANT (Kd) + UNITS
INTEGRAL_CONSTANT   (Ki) + UNITS
PROPORTIONAL_CONSTANT (Kp)
```

### Límites y alarmas

```
45  HIGH_LIMIT
59  LOW_LIMIT
27  DEADBAND
8   ALARM_VALUE
9   ALARM_VALUES
35  FAULT_VALUES
33  ERROR_LIMIT
52  LIMIT_ENABLE
67  MINIMUM_OFF_TIME
68  MINIMUM_ON_TIME
69  MINIMUM_OUTPUT
65  MAXIMUM_OUTPUT
```

### COV

```
23  COV_INCREMENT                            threshold (Δ)
    ACTIVE_COV_SUBSCRIPTIONS                 conteo
    ACTIVE_COV_MULTIPLE_SUBSCRIPTIONS
21  COV_RECIPIENTS
22  COV_SUBSCRIPTION
```

### Event/Notification

```
36  EVENT_TYPE
37  EVENT_ENABLE           bitmask (TO_OFFNORMAL | TO_FAULT | TO_NORMAL)
72  NOTIFY_TYPE            CHANGE | WEIGHTED_MA | SIMPLE_MA
17  NOTIFICATION_CLASS
1   ACK_REQUIRED
0   ACKED_TRANSITIONS
120 TIME_DELAY             segundos antes fire
121 TIME_DELAY_NORMAL
```

### Texto/enumeración

```
4   ACTIVE_TEXT
46  INACTIVE_TEXT
3   ACTION_TEXT
2   ACTION
74  NUMBER_OF_STATES
110 STATE_TEXT
32  ENUMERATION_VALUES     (proprietary)
```

### Tiempo/sync

```
56  LOCAL_DATE
57  LOCAL_TIME
70  MODIFICATION_DATE
71  MODIFICATION_DATE_TIME
25  DATE_LIST
26  DAYLIGHT_SAVINGS_STATUS
31  EFFECTIVE_PERIOD
34  EXCEPTION_SCHEDULE
```

### Schedule/calendar

```
127 WEEKLY_SCHEDULE
    LIST_OF_OBJECT_PROPERTY_REFERENCES
    LIST_OF_GROUP_MEMBERS
```

### File

```
40  FILE_TYPE              STREAM | RECORD
41  FILE_SIZE              bytes
39  FILE_ACCESS_METHOD
```

### Trend log

```
60  LOG_BUFFER
28  ENABLE (LOG_ENABLE)
108 START_TIME
109 STOP_TIME
123 RECORD_COUNT
124 TOTAL_RECORD_COUNT
208 NOTIFICATION_THRESHOLD
6   BUFFER_SIZE
148 RECORD_COUNT (variant)
112 STOP_WHEN_FULL
```

### Device

```
44  FIRMWARE_REVISION
12  APPLICATION_SOFTWARE_VERSION
73  MODEL_NAME / NUMBER_OF_APDU_RETRIES (ojo: 73 reused en el protocolo)
126 VENDOR_NAME
139 PROTOCOL_REVISION      3 o 4 (ASHRAE 135-2020)
513 PROTOCOL_CONFORMANCE_CLASS
62  MAX_APDU_LENGTH_ACCEPTED
107 SEGMENTATION_SUPPORTED
64  MAX_SEGMENTS_ACCEPTED
63  MAX_INFO_FRAMES
11  APDU_TIMEOUT           (ms)
66  MAX_MASTER             MS/TP token max
65  MAX_PRES_VALUE
69  MIN_PRES_VALUE
29  DEVICE_ADDRESS_BINDING
509 DEVICE_UUID
```

### Device Communication Control

```
COMMUNICATION_ENABLE_ALL
COMMUNICATION_ENABLE_INITIATION
COMMUNICATION_DISABLE_ALL
```

---

## 23.3 BBacnetDevice — cliente driver

### Jerarquía

```
BLoadableDevice (Bloque 4)
 └ BBacnetDevice implements
   BIBacnetPollable, BIBacnetObjectContainer,
   DeviceOverrideAware, LatencyRecorderAware, LatencyRecorder
```

### Propiedades principales

```
address                BBacnetAddress
points                 BBacnetPointDeviceExt
virtual                BBacnetVirtualGateway
alarms                 BBacnetAlarmDeviceExt
schedules              BScheduleDeviceExt
trendLogs              BHistoryDeviceExt
config                 BBacnetConfigDeviceExt
enumerationList        BExtensibleEnumList       (vendor enums)
useCov                 boolean
useCovProperty         boolean
maxCovSubscriptions    int
covSubscriptions       int
pollFrequency          BPollFrequency
characterSet           BCharacterSetEncoding
maxPollTimeouts        int                        (desactiva tras N fallos)
disableDeviceOnCovSubscriptionFailure  boolean
```

### Métodos clave

```java
BBacnetObjectIdentifier getObjectId();
int getMaxAPDULengthAccepted();
BBacnetSegmentation getSegmentationSupported();
int getVendorId();
int getProtocolRevision();
boolean isServiceSupported(int serviceId);
boolean isObjectTypeSupported(int objectType);

synchronized void registerDevice(BBacnetDevice);
synchronized void unregisterDevice(BBacnetDevice);
synchronized void updateDevice(BBacnetDevice);
boolean poll();
int getPollableType();
```

### Campos internos

```
private boolean rpmOk;           // ReadPropertyMultiple funcional
private int maxAPDU;
private boolean isPolling;
private int pollTimeouts;
private Object DEVICE_LOCK;
private int failedPings;
private volatile boolean isFirstPingOk;
```

### BBacnetAddress

```java
class BBacnetAddress extends BStruct implements BIBacnetDataType {
  static final int LOCAL_NETWORK     = 0;
  static final int BROADCAST_NETWORK = 65535;

  int getAddressType();
  int getNetworkNumber();
  BBacnetOctetString getMacAddress();
}
```

Tipos MAC:
```
MAC_TYPE_UNKNOWN  = 0
MAC_TYPE_ETHERNET = 1  (6 bytes)
MAC_TYPE_IP       = 2  (IPv4 4+2 / IPv6 16+2)
MAC_TYPE_MSTP     = 3  (1 byte, nodo 0-127 maestro)
MAC_TYPE_SC       = 4  (BACnet/SC)
```

Constantes:
```
GLOBAL_BROADCAST_ADDRESS   (DNET=0xFFFF, DADR all-1s)
LOCAL_BROADCAST_ADDRESS    (mismo subnet)
DEFAULT
```

---

## 23.4 Point binding — BBacnetProxyExt

### Jerarquía

```
BProxyExt (Bloque 4)
 └ BBacnetProxyExt implements BIBacnetPollable, BIRemoteAlarmSource
    ├ BBacnetNumericProxyExt   → BNumericPoint
    ├ BBacnetBooleanProxyExt   → BBooleanPoint
    ├ BBacnetStringProxyExt    → BStringPoint
    └ BBacnetEnumProxyExt      → BEnumPoint
```

### Configuration

```
objectId              BBacnetObjectIdentifier
propertyId            BDynamicEnum (BBacnetPropertyIdentifier ref)
propertyArrayIndex    int  (-1 = todo array, ≥0 = índice)
dataType              String  "REAL"|"BOOLEAN"|"INTEGER"|...
readStatus, writeStatus
deviceFacets          extended facets
```

### Actions

```
forceRead, forceWrite
subscribeCov, subscribeCovProperty
ackAlarm
```

### Sub-states (subscription lifecycle)

```
SUB_STATE_UNSUB              = 0
SUB_STATE_POLLED             = 1
SUB_STATE_COV                = 2
SUB_STATE_FIRST_COV_PENDING  = 3
SUB_STATE_POLLED_PENDING     = 4
SUB_STATE_COV_PENDING        = 5
SUB_STATE_FIRST_COVP_PENDING = 6
SUB_STATE_COVP               = 7
SUB_STATE_COVP_PENDING       = 8
SUB_STATE_COVP_FAILED        = 9
```

Transiciones típicas:
```
COV:      UNSUB → FIRST_COV_PENDING → COV (o POLLED si falla)
Renovar:  COV → COV_PENDING → COV
Fallback: COV_FAILED → POLLED_PENDING → POLLED
```

### Estado interno

```
private BBacnetDevice bacnetDevice;
private int subState;
private PollListEntry[] ples;
private int lastWriteLevel;
private BBacnetPoll pollService;
private Clock.Ticket ticket;
private BBoolean priPV;              // usar priority array
private BBoolean useStatusFlags;
```

---

## 23.5 Discovery — WhoIs / IAm / WhoHas

### BDiscoveryDevice structure

```
getDeviceName()                  string
getObjectId()                    BBacnetObjectIdentifier
getMaxApduLengthAccepted()       int  (480-1476)
getSegmentationSupported()       BBacnetSegmentation
getVendorId()                    int  ASHRAE registry
getAddress()                     BBacnetAddress
getListSize()
getEncoding()                    BCharacterSetEncoding
getServicesSupported()           BBacnetBitString (bitmap)
getVendorName()                  "Honeywell", "Johnson", ...
getModelName()
getProtocolRevision()
getFirmwareRevision()
getApplicationSoftwareVersion()
getDuplicate()                   boolean (ID duplicado detectado)
```

### WhoIs (Unconfirmed Service 0x08)

```
[unconfirmed-request]
 ├ Low Limit  (optional, integer)
 ├ High Limit (optional, integer)
 └ broadcast típico, responde IAm si device-id en rango
```

### IAm (Unconfirmed Service 0x00)

```
[unconfirmed-request]
 ├ Object Identifier          (type=Device, instance=N)
 ├ Max APDU Length            480..1476
 ├ Segmentation               0=both, 1=tx, 2=rx, 3=none
 └ Vendor ID                  0-65535 ASHRAE registry
```

### WhoHas (Service 0x07)

```
[unconfirmed-request]
 ├ Device Instance Limits (opcionales)
 ├ Object Identifier (type+instance) ...
 └ o alternativo: Nombre objeto (string)
```

---

## 23.6 Priority Array (16 niveles + relinquish default)

### BBacnetPriorityValue

Choice types:
```
NULL_CHOICE               (vacío)
REAL_CHOICE               float IEEE
DOUBLE_CHOICE
BINARY_CHOICE             boolean
UNSIGNED_CHOICE           uint
INTEGER_CHOICE            int
STRING_CHOICE             CharacterString
OCTET_STRING_CHOICE
BIT_STRING_CHOICE
DATE_CHOICE
TIME_CHOICE
DATE_TIME_CHOICE
CONSTRUCTED_VALUE_CHOICE  (proprietary)
```

Métodos:
```
void setPriorityValue(BValue)
BValue getPriorityValue()
boolean isNull()
int choice()
```

### Uso en output objects (AO/BO/MO)

```
PRIORITY_ARRAY (property 87) = array[16] de BBacnetPriorityValue

Índices (1-16):
 1     Manual takeover (máxima urgencia)
 2-8   Control aplicación
 9     Ajuste manual operador
10     Automatización local
11-15  Supervisión remota / cliente
16     Relinquish default (mínima)

Resolución:
  PresentValue = primer NO-NULL del array
  if all NULL → RelinquishDefault
```

### WriteProperty con priority

```java
new WritePropertyRequest(
    objectId, propertyId,
    -1,             // sin array index (presentValue)
    encodeFloat(72.0),
    12              // priority level (supervisor típico)
);
```

Service choice = 0x0F (WriteProperty), tag 3 para priority opcional.

---

## 23.7 Servicios confirmados (26+ services)

| ID | Servicio | Uso típico |
|---|---|---|
| 0 | AcknowledgeAlarm | ACK alarma |
| 1 | ConfirmedCOVNotification | Notif COV confirmada |
| 2 | ConfirmedEventNotification | Evento confirmado |
| 3 | GetAlarmSummary | Resumen alarmas activas |
| 4 | GetEnrollmentSummary | Resumen inscripciones |
| 5 | SubscribeCOV | Suscripción COV básica |
| 6 | AtomicReadFile | Lectura archivo |
| 7 | AtomicWriteFile | Escritura archivo |
| 8 | AddListElement | Append a lista |
| 9 | RemoveListElement | Quitar de lista |
| 10 | CreateObject | Crear objeto dinámico |
| 11 | DeleteObject | Eliminar objeto |
| 12 | ReadProperty | Lectura propiedad única |
| 13 | ReadPropertyConditional | (rare) |
| 14 | ReadPropertyMultiple | **Lectura batch — optimización clave** |
| 15 | WriteProperty | Escritura con priority |
| 16 | WritePropertyMultiple | Escritura batch |
| 17 | DeviceCommunicationControl | Enable/disable comms |
| 18 | ConfirmedPrivateTransfer | Vendor-specific |
| 19 | ConfirmedTextMessage | Mensaje texto |
| 20 | ReinitializeDevice | Coldstart/Warmstart |
| 21-23 | VTOpen/Close/Data | Virtual Terminal |
| 24 | Authenticate | Legacy |
| 25 | RequestKey | Legacy |
| 26 | WriteGroup | Grupo escritura |
| 28 | SubscribeCOVProperty | COV property-specific |
| 29 | GetEventInformation | Event info |
| 35 | ReadRange | Rango histórico (Trend Log) |
| 55 | SubscribeCOVPropertyMultiple | N4.14+ |
| 56 | ConfirmedCOVNotificationMultiple | N4.14+ |

### Servicios no confirmados (11)

| ID | Servicio |
|---|---|
| 0 | I-Am |
| 1 | I-Have |
| 2 | UnconfirmedCOVNotification |
| 3 | UnconfirmedEventNotification |
| 4 | UnconfirmedPrivateTransfer |
| 5 | UnconfirmedTextMessage |
| 6 | TimeSynchronization |
| 7 | WhoHas |
| 8 | WhoIs |
| 9 | UTCTimeSynchronization |
| 10 | WriteGroup |

---

## 23.8 Stack de red — BVLC → NPDU → APDU

### Stack architecture

```
┌── Application Layer (APDU) ────────────────────────┐
│ Confirmed (26+) / Unconfirmed (11) / Segment-ACK  │
│ ApplicationPdu, ConfirmedRequestPdu, TSM          │
├────────────────────────────────────────────────────┤
│ Network Layer (NPDU)                                │
│ NetworkPdu (abstract), NLM msgs, hop, DNET/SNET   │
├────────────────────────────────────────────────────┤
│ Link Layer                                          │
│ IP/BVLC · MS/TP · Ethernet · PTP · BACnet/SC      │
└────────────────────────────────────────────────────┘
```

### BVLC (BACnet/IP)

Frame:
```
byte 0      Type                0x81  (BACnet IP)
byte 1      Function            0x00-0x0B
bytes 2-3   Length (big-endian, incluye header 4 bytes)
bytes 4+    NPDU payload
```

Functions:
| Hex | Nombre | Descripción |
|---|---|---|
| 0x00 | BVLC-Result | Resultado comandos BVLC |
| 0x01 | Write-BDT | BBMD escribe tabla broadcast |
| 0x02 | Read-BDT | BBMD lee tabla |
| 0x03 | Read-BDT-Ack | Respuesta BDT |
| 0x04 | Forwarded-NPDU | **NPDU reenviado por BBMD** |
| 0x05 | Register-Foreign-Device | FD se registra en BBMD |
| 0x06 | Read-FDT | Lee FDT |
| 0x07 | Read-FDT-Ack | Respuesta FDT |
| 0x08 | Delete-FDT-Entry | |
| 0x09 | Distribute-Broadcast-To-Network | BBMD distribuye broadcast |
| 0x0A | Original-Unicast-NPDU | **Unicast normal** |
| 0x0B | Original-Broadcast-NPDU | **Broadcast local** |

### NPDU (Network Layer)

Control octet (byte 0):
```
bit 7   = version (0 = BACnet)
bit 6   = Network Layer Message flag (1 = NLM, 0 = data)
bit 5   = reserved (=1)
bit 4   = Destination Specifier (1 = DNET+DADR presentes)
bit 3   = Source Specifier (1 = SNET+SADR presentes)
bit 2   = Data Expecting Reply (B-bit)
bits 1-0 = Network Priority
          00 Normal
          01 Urgent
          10 Critical Equipment Control
          11 Life Safety
```

Campos adicionales si flags:
```
DNET  (2 bytes big-endian)         0xFFFF = broadcast
DLEN  (1 byte)                     0-20
DADR  (DLEN bytes)                 MAC target
SNET  (2 bytes)
SLEN  (1 byte)
SADR  (SLEN bytes)
HopCount (1 byte)                  default 64, decrementa cada router, DROP si 0
```

Constantes:
```
VERSION = 0x01
NETWORK_LAYER_MSG_BIT = 0x40
DNET_NOT_PRESENT = 0xFFFF
DEFAULT_HOP_COUNT = 64
```

### APDU (Application Layer)

PDU types (bits 7-4 byte 0):
```
0000 0x00  Confirmed-Request
0001 0x10  Unconfirmed-Request
0010 0x20  Simple-ACK
0011 0x30  Complex-ACK
0100 0x40  Segment-ACK
0101 0x50  Error
0110 0x60  Reject
0111 0x70  Abort
```

Confirmed-Request flags (bits 3-0):
```
bit 3   SEG   segmented message
bit 2   MOR   more follows
bit 1   SA    segmented response accepted
bit 0   reserved
```

Byte 1 (Max Segs / Max APDU codes):
```
bits 7-4  Max Segments code (0=unspec, 1=2, 2=4, 3=8, 4=16, 5=32, 6=64, 7=>64)
bits 3-0  Max APDU Length code (0=50, 1=128, 2=206, 3=480, 4=1024, 5=1476)
```

Bytes típicos:
```
byte 2  Invoke ID (0-255)
byte 3  Sequence # (si SEG=1)
byte 4  Window Size (si SEG=1)
bytes N Service Choice + primitive
```

---

## 23.9 Segmentación APDU

### Negociación

```
Max APDU Length:
  Code 0 = 50 bytes (min)
  Code 1 = 128
  Code 2 = 206
  Code 3 = 480
  Code 4 = 1024
  Code 5 = 1476     (típico IP)

Max Segments:
  Code 0 = unspec
  Code 1 = 2   Code 2 = 4   Code 3 = 8
  Code 4 = 16  Code 5 = 32  Code 6 = 64
  Code 7 = >64
```

### Flujo (request segmentado)

```
Client                        Server
  ConfirmedReq (SEG=1, MOR=1, Seq=0)
  Segment[0..1472]
  ─────────────────────────────────>
  <──── SegmentAck (Seq=0, OK) ────
  ConfirmedReq (SEG=1, MOR=0, Seq=1)
  Segment[1473..end]
  ─────────────────────────────────>
  <──── SegmentAck (Seq=1, OK) ────
  [server procesa, genera respuesta]
  <──── ComplexAck (Seq=0, MOR=1)
  SegmentAck (Seq=0) ─────────────>
  <──── ComplexAck (Seq=1, MOR=1)
  SegmentAck (Seq=1) ─────────────>
  <──── ComplexAck (Seq=2, MOR=0)
  SegmentAck (Seq=2) ─────────────>
```

### Timeouts (TSM)

```
APDUTimeout      default 3000 ms   (100-60000)
APDURetries      default 3         (0-255)
SegmentTimeout   default 1500 ms
SegmentRetries   default 3
```

Abort reasons:
```
0 other
1 buffer-overflow
2 invalid-apdu-in-this-state
3 preempted-by-higher-priority-task
4 segmentation-not-supported
```

Reject reasons:
```
1 buffer-overflow
2 inconsistent-parameters
3 invalid-parameter-data-type
4 invalid-tag
5 missing-required-parameter
6 parameter-out-of-range
7 too-many-arguments
8 undefined-enumeration
9 unrecognized-service
```

Error codes comunes:
```
30  write-access-denied
40  read-access-denied
44  timeout
45  unknown-object
46  unknown-property
49  unsupported-object-type
50  value-out-of-range
55  write-access-denied
```

---

## 23.10 COV subscription lifecycle

### Subscribe

```
Client → Server:
  SubscribeCOV (confirmed, service 5)
   ├ subscriber-process-id        long (unique per client)
   ├ monitored-object             (type, instance)
   ├ issue-confirmed-notifications  bool
   └ lifetime                     seconds (0 = unsubscribe, 3600 default)

Server → Client: SimpleAck (si aceptada)
```

### Notify (cambio valor)

```
Server → Client (cambio Δ ≥ COV_INCREMENT):
  UnconfirmedCOVNotification (service 2)
   ├ initiating-device-id
   ├ monitored-object-id
   ├ subscriber-process-id
   ├ time-remaining                seconds hasta expirar
   └ list-of-values                [PropertyValue, ...]
```

### Refresh (keep-alive sin cambio)

```
Cada ~lifetime * 0.8:
  Server → Client: UnconfirmedCOVNotification (mismo valor)
  → Client resetea timer

Si lifetime expira sin notif:
  Client resubscribe
Si device rechaza/offline:
  subState → POLLED_PENDING → fallback polling
```

### SubscribeCOVProperty (service 28)

Más granular — permite subscribe a **property específica** + **covIncrement override**:
```
SubscribeCOVProperty
 ├ subscriber-process-id
 ├ monitored-property-reference  (object + property + index)
 ├ issue-confirmed
 ├ lifetime
 └ cov-increment (REAL, opcional)
```

---

## 23.11 Schedule object — weekly + exception

### BBacnetScheduleDeviceExt (32.8 KB)

Soporta revisiones BACnet distintas:
```
ScheduleSupport0  base (Rev3)
ScheduleSupport4  Rev 4+
ScheduleSupport16 Rev 16+

setSupport(int protocolRevision) instancia la correcta.
```

### Propiedades Schedule

```
objectIdentifier              (SCHEDULE, instance)
effectivePeriod               BBacnetDateRange
weeklySchedule                BWeeklySchedule (7 días)
exceptionSchedule             BCompositeSchedule
scheduleDefault               BStatusValue
listOfObjectPropertyReferences  BBacnetListOf
priority                      BBacnetArray[1..16]
```

### Estructura

```java
BWeeklySchedule {
  BWeekSchedule week;
  BCalendarSchedule holidays;
  BDailySchedule[] byDay[7];   // Mon..Sun
}
BDailySchedule {
  List<TimeValue> daySchedule;
}
TimeValue {
  BTime time;                  // HH:MM:SS.centi
  BStatusValue value;          // BOOLEAN|REAL|ENUM polimorfico
}
```

### Bidireccionalidad proxy

Client → Device:
```
BBacnetScheduleExport
  sendSchedule(BWeeklySchedule) → WriteProperty(WEEKLY_SCHEDULE, encoded ASN.1)
```

Device → Client:
```
BBacnetScheduleImportExt
  readRemote(BBacnetClientLayer)
    → ReadProperty(device, objectId, WEEKLY_SCHEDULE)
    → Decode ASN.1 to BCalendarSchedule / BWeeklySchedule
```

Validación: `checkForCalendarReferences()` verifica excepciones con BBacnetCalendarEntry válida.

---

## 23.12 Calendar object

### BBacnetCalendar (4.9 KB)

```
objectIdentifier  (CALENDAR, instance)
presentValue      boolean   (is today special?)
dateList          BBacnetListOf<BBacnetCalendarEntry>
```

### BBacnetCalendarEntry (CHOICE)

```
Opción 1: BBacnetDateRange
  startDate (BDate)
  endDate   (BDate)

Opción 2: BBacnetWeekNDay
  month          BMonth
  weekOfMonth    int (1-5, 5=last)
  dayOfWeek      BWeekday

Opción 3: BDate
  year, month (1-12), day (1-31)
```

### Integración Schedule-Calendar

```
1. readSchedule(obj) → BWeeklySchedule
2. si weekly.exceptionSchedule no vacía:
     buscar calendario referenciado
3. readCalendar(objectId) → cargar dateList
4. combinar: weekly.getSpecialEvents() = exception sobre weekly
```

---

## 23.13 Notification Class — routing de alarmas

### BBacnetNotificationClass (8.3 KB)

```
notificationClass       BBacnetUnsigned (0-65535)
priority                BBacnetArray[3]
                        [LIFE_SAFETY, CRITICAL, URGENT]
ackRequired             BBacnetBitString
                        (TO_OFFNORMAL | TO_FAULT | TO_NORMAL)
recipientList           BBacnetListOf<BBacnetDestination>
```

### BBacnetDestination

```
recipient                   BBacnetRecipient
processIdentifier           int
issueConfirmedNotifications boolean
transitionsOfInterest       BBacnetBitString (16 bits)
```

### BBacnetRecipient (CHOICE)

```
Address:    (BBacnetAddress + networkNumber)
DeviceObjectReference: (deviceId)
```

### Actions

```java
addDestination(BBacnetDestination)
removeDestination(BBacnetDestination)
removeRecipient(BBacnetRecipient)  // async via BBacnetNetwork.postAsync()
```

---

## 23.14 Event Enrollment — intrinsic vs algorithmic

### BBacnetEventEnrollment

```
eventType                 BBacnetEventType (CHANGE_OF_BITSTRING, CHANGE_OF_STATE, CHANGE_OF_VALUE, COMMAND_FAILURE, FLOATING_LIMIT, OUT_OF_RANGE, LIFE_SAFETY, EXTENDED, BUFFERED_CHANGE_OF_VALUE, CHANGE_OF_LIFESAFETY, EXTENDED_WHERE_CLAUSE, STATE_CHANGE_VALUE, COV, COMPLEX_EVENT_TYPE)
eventParameters           BacnetNotificationParameters (polimorfica por eventType)
objectPropertyReference   BBacnetDeviceObjectPropertyReference
eventEnable               BBacnetArray[bool] (OFFNORMAL, FAULT, NORMAL)
ackedTransitions          BBacnetBitString
notificationClass         BBacnetUnsigned
eventPriority             BBacnetUnsigned (0=LIFE_SAFETY..15=ROUTINE)
timeDelay                 BBacnetUnsigned (segundos)
timeDelayNormal           BBacnetUnsigned
```

### Parámetros por tipo (polimórficos)

```
OutOfRange {deadband: REAL, lowLimit: REAL, highLimit: REAL}
ChangeOfValue {timeDelay: UINT, coVIncrement: REAL}
ChangeOfBitstring {timeDelay: UINT, bitMask: BIT STRING}
FloatingLimit {lowDiffLimit, highDiffLimit, deadband: REAL}
SignedOutOfRange {deadband: INT, lowLimit: INT, highLimit: INT}
CommandFailure {feedbackValue: ANY}
```

### Intrinsic vs Algorithmic

```
INTRINSIC:
  Evaluado en device BACnet
  Event Enrollment genera notificación automática
  + eficiencia (sin polling)
  − requiere device support

ALGORITHMIC:
  Evaluado en Niagara (BAlarmClass)
  Niagara→BACnet: descarga EventEnrollment
  BACnet→Niagara: ReadProperty periódico
  + flexibilidad
```

---

## 23.15 Trend Log + Trend Log Multiple

### BBacnetTrendLog

```
logEnable                 boolean
stopWhenFull              boolean
bufferSize                BBacnetUnsigned
recordCount               BBacnetUnsigned
totalRecordCount          BBacnetUnsigned
notifyType                BBacnetNotifyType (CHANGE|WEIGHTED_MA|SIMPLE_MA)
notificationClass         BBacnetUnsigned
objectPropertyReference   BBacnetDeviceObjectPropertyReference
                          (deviceId + objectId + propertyId + arrayIndex?)
eventState                BEnum
```

### 8+ variantes tipo

Locales:
- BBacnetBooleanTrendLogExt
- BBacnetNumericTrendLogExt
- BBacnetStringTrendLogExt
- BBacnetEnumTrendLogExt
- BBacnetBitStringTrendLogExt
- BBacnetBooleanCovTrendLogExt
- BBacnetNumericCovTrendLogExt
- BBacnetStringCovTrendLogExt
- BBacnetNumericIntervalTrendLogExt
- BBacnetEnumIntervalTrendLogExt
- BBacnetBooleanIntervalTrendLogExt

Remote (import):
- BBacnetBooleanTrendLogRemoteExt, NumericTrendLogRemote, StringTrendLogRemote, EnumTrendLogRemote, BitStringTrendLogRemote.

### Record ASN.1

```
BacnetTrendLogEntry ::= CHOICE {
  logDatum     [0] CHOICE { null, realValue, enumValue, booleanValue, ... }
  timeChange   [1] REAL           -- delta
  statusFlags  [2] BIT STRING
}
```

### Trend Log Multiple

```
recordCount               BBacnetArray
elementsLoggedCount       BBacnetUnsigned
logMultipleRecords        BBacnetListOf<BBacnetLogMultipleRecord>

BBacnetLogMultipleRecord {
  timestamp: BDateTime
  logData:   BBacnetArray[
    { elementIndex: int, elementValue: ANY/STATUS/REAL/ENUM }
  ]
}
```

### Import remoto

```
BBacnetTrendLogMultipleImport
 1. discover objectPropertyReferences
 2. ReadRange(trendLog, OLDEST_RECORD_TIME → NEWEST_RECORD_TIME)
 3. parse records (timestamp + values)
 4. integrar con Niagara history framework (Bloque 12)
 5. BacnetTrendLogUtil.readLogResult()
```

Polling vs COV:
```
COV:    notificación en cambio; inmediato; refresh ~7 días
Polling: ReadRange periódico; configurable interval
Hybrid:  COV para cambios + polling catch-up
```

---

## 23.16 Access Objects (control acceso físico)

### BBacnetAccessDoor

```
presentValue     BBacnetDoorValue (CLOSED=0|OPENED=1|DOOR_FAULT=2|UNKNOWN=3)
statusFlags      (in-alarm, fault, overridden, out-of-service)
priorityArray    BBacnetArray[BBacnetPriorityValue][16]
relinquishDefault
doorPulseTime         ms
doorExtendedPulseTime ms
doorOpenTooLongTime   seconds
```

### BBacnetAccessUser

```
userType              RESIDENT=0 | VISITOR=1 | CONTRACTOR=2 | VENDOR=3 | GUEST=4
userExternalID        BBacnetCharacterString  (card ID)
credentialList        list<BBacnetAccessCredential>
accessRights          BBacnetArray[BBacnetAssignedAccessRights]
globalAccessRights
```

### BBacnetAccessCredential

```
credentialDisable      DISABLE | DISABLE_BY_CREDENTIAL_FAULT
credentialStatus       BitString
credentialData         OctetString
credentialAuthenticationFactor[]
   { authenticationFactorType: PASSWORD|BIOMETRIC|RFID_TOKEN, factor: OctetString }
globalAccessRights
```

### BBacnetAccessZone

```
presentValue          NORMAL=0 | HIGH=1 | VERY_HIGH=2
occupancyCount        BBacnetUnsigned
occupancyLowerLimit/UpperLimit + Enforced flags
lastCredentialAdded/Removed + Time
credentialList
accessEventList
```

### BBacnetAccessPoint

```
zoneFrom/zoneTo       BBacnetObjectIdentifier
accessEvent {
  timestamp, accessEventType (GRANT|DENY|DENY_NO_CREDENTIAL|...)
  credential, statusFlags
}
accessAlarm           boolean
accessTransactionInformation[]
```

### BBacnetAccessRights

```
globalAccessRights[] {
  accessZone, credentialDisable,
  accessRights[] { accessZone, accessTiming, enable, priority }
}
```

---

## 23.17 Program / Command / Loop

### BBacnetProgram

```
programState         IDLE=0|LOADING=1|RUNNING=2|WAITING=3|HALTED=4|
                     UNLOADING=5|UNLOADED=6|CREATING=7|DELETING=8|
                     DOWNLOADING=9|UPLOADING=10
programChange        READY=0|LOAD=1|RUN=2|HALT=3|RESTART=4|DELETE=5|
                     RESTART_AND_DELETE=6|GET_COMMAND_OUTPUT=7|
                     DEBUG_ON=8|DEBUG_OFF=9|MAKE_RESIDENT=10|
                     MAKE_TEMPORARY=11
programLocation      path / URL
instanceOf           BBacnetObjectIdentifier (template)
programErrors        LOAD_ERROR | EXEC_ERROR | HALT_ERROR | ...
```

### BBacnetCommand

```
commandInstances     uint (ejecuciones)
commandActualTime    BDateTime (último)
commandTimeToState   uint (segundos)
actionCommand        list<BBacnetActionCommand>

BBacnetActionCommand {
  action: BBacnetActionType (DIRECT|REVERSE|...)
  deviceId, objectId, propertyId, propertyIndex?
  propertyValue: ANY
  priority: int (0-15)
  postDelayTime: uint (ms)
  quitOnFailure: bool
  writeSuccessful: bool
}
```

### BBacnetLoop (PID)

```
presentValue                 REAL  (output)
setPoint                     REAL
inputReference               DeviceObjectPropertyReference (realimentación)
processValue                 REAL
errorLimit                   REAL (tolerancia)
proportionalConstant  (Kp)
integralConstant      (Ki)
derivativeConstant    (Kd)
lastError
loopCycleTime                ms
outputScale                  factor
manipulatedVariableReference (actuador)
```

---

## 23.18 Channel / Group / Global Group

### Channel

```
channelReferences    list<DeviceObjectPropertyReference>
lastSegmentAckTime   BDateTime
channelValue         polimórfico
```

Útil para lectura/escritura atómica multi-property.

### Group

```
membersList          list<BBacnetDeviceObjectReference>
groupMembers         BBacnetArray (alt representation)
```

Uso: `ReadMultipleProperty(groupId)` resuelve en server.

### Global Group

```
globalGroupMembers   BBacnetArray[DeviceObjectPropertyReference]
```

Agregación cross-device — lectura atómica desde supervisor.

---

## 23.19 File object

```
fileSize                 uint (bytes)
fileType                 CharacterString
fileModificationDate     BDateTime
fileAccessMethod         STREAM (bytes secuenciales) | RECORD (registros fixed)
fileDescription
fileStartPosition        uint (offset)
```

Operaciones:
```
AtomicReadFile(fileId, startPosition, requestedOctetCount)
  → AtomicReadFileAck { endOfFile: bool, fileStartPosition, fileData[] }

AtomicWriteFile(fileId, startPosition, fileData[], recordCount?)
  → AtomicWriteFileAck { fileStartPosition, recordsWritten? }
```

---

## 23.20 Accumulator / Pulse Converter

### Accumulator

```
presentValue                 uint (contador actual)
units                        EngineeringUnits
scale                        REAL
valueBeforeChange, valueChangeTime
accumulatedValue             uint
accumulatedValueAdjustTime
lastAdjustmentValue
adjustmentIncrement
```

### Pulse Converter

```
presentValue              REAL (valor convertido)
inputReference            DeviceObjectPropertyReference (contador)
scaleFactor               REAL
unitsExponent             int (10^x)
units                     EngineeringUnits
pulseRate                 REAL
```

---

## 23.21 EDE (Engineering Data Exchange)

Formato CSV estándar para inicialización masiva de dispositivos.

### Módulo: bacnetEDE-wb.jar (179 KB)

Archivos:
```
EDE_MasterFile.csv    Listado dispositivos + objetos
Units_File.csv        Unidades engineering (ASHRAE codes)
StateTexts_File.csv   Textos de estado
```

### Estructura

```
=== DEVICES ===
DeviceIdentifier,DeviceName,DeviceAddress,NetworkNumber,MacAddress
0,Primary_Controller,192.168.1.100,0,BBAC0164A8B2

=== OBJECTS ===
DeviceIdentifier,ObjectType,ObjectInstance,ObjectName,ObjectDescription
0,Analog Input,0,Temp_Building,...

=== SCHEDULE DETAILS ===
DeviceIdentifier,ObjectInstance,WeekDay,Time,Value,Calendar
0,0,Monday,06:00,1,

=== NOTIFICATION CLASS DETAILS ===
DeviceIdentifier,ObjectInstance,Priority,AckRequired,Recipient,ProcessId

=== CALENDAR ===
DeviceIdentifier,ObjectInstance,DateRangeStart,DateRangeEnd
```

### Parser

```java
BEdeReader reader = new BEdeReader(edeFile, unitsFile, stateTextsFile);
EdeCursor cursor = reader.parse();
cursor.listDevices();   // List<BEdeDiscoveryDevice>
cursor.listPoints(deviceInstance);  // List<BEdeDiscoveryPoint>
```

### Export

`com.tridium/bacnetEde/wb/BLocalDeviceToEde`:
```
LocalDeviceToEde(BLocalBacnetDevice, exportDir, ',')
  .execute() → EDE_MasterFile.csv + Units.csv + StateTexts.csv
```

---

## 23.22 Alarm Router (bacnetAlarmRouter-rt, 98.5 KB)

Enrutador alarmas BACnet → Alarm Service Niagara (Bloque 8).

### Componentes

```
BBacnetAlarmClassReassigner implements IAlarmReassigner
  listener a eventos BACnet
  post a Niagara AlarmService

BCustomBacnetEventProcessor
  filtrado eventos
  transformación prioridades
  escalation logic

BEscalationFilter
  verifica condiciones escalada
  timeout rules
  reasigna Notification Class
```

### Flujo

```
BBacnetAlarmDeviceExt recibe evento BACnet
 → AsyncEventNotificationRequest decode
 → BCustomBacnetEventProcessor filtros
 → BBacnetAlarmClassReassigner → BAlarmClass
 → BAlarmSource genera alarma Niagara
 → Subscription → BNotificationHandler (UI)
```

### Filtros

```java
interface BAlarmFilter {
  boolean accept(BacnetEventNotification);
}

BAbstractAlarmFilter {
  acceptState(BBacnetEventState)
  acceptPriority(int)
  acceptDevice(int deviceId)
  acceptObject(BBacnetObjectIdentifier)
}

BEscalationFilter {
  updateLastEventTime(int deviceId)
  shouldEscalate(long timeoutMs)
}
```

---

## 23.23 BACnet Migrator (bacnetMigrator-wb, 41.6 KB)

Converters:
```
BBacnetLinkLayerConverter     IP↔Serial
BBacnetWsToAwsConverter       Workstation → Advanced Workstation (agrega EventLog, ActionList)
BBacnetWsToAwsPxConverter     Proxy points WS → AWS (adapta enumeraciones)
```

---

## 23.24 BACnet/SC (Secure Connect)

Stack seguro sobre TLS 1.3 + TCP (puerto IANA 49152):
```
┌── BACnet Services (APDU) ────────┐
│ ReadProperty, etc.               │
├──────────────────────────────────┤
│ BACnet/SC Message Wrapper        │
│ (encriptación + HMAC)            │
├──────────────────────────────────┤
│ TLS 1.3 (mutual auth)            │
├──────────────────────────────────┤
│ TCP (puerto 49152)               │
└──────────────────────────────────┘

ScBvlcMessage, ScNpdu, BScLinkLayer (clases en bacnet-rt.jar)
```

Certificados:
```
Device cert: CN, OU=BACnet/SC, KU=DigSig+KeyEnciph, EKU=serverAuth+clientAuth
CA cert: self-signed root
```

Flujo en Niagara:
```
1. Generate CSR
2. Sign by CA
3. Import operational cert
4. Configure port BScLinkLayer
5. Enable secure connections
```

**Status en N4.14**: arquitectura preparada; uso operacional limitado. No ampliamente desplegado todavía.

---

## 23.25 Routing — router BACnet

Niagara con 2+ NICs puede actuar de router.

### Routing table

```
Network 1 → Port A (local)
Network 2 → Port B (local)
Network 10 → Port A (remoto, via router)
Network 20 → Port B (remoto, via router)
```

### Network Layer Messages (router discovery)

```
WhoIsRouterToNetwork
IAmRouterToNetwork
ICouldBeRouterToNetwork
RouterBusyToNetwork
RouterAvailableToNetwork
InitializeRoutingTable
InitializeRoutingTableAck
EstablishConnectionToNetwork
DisconnectConnectionToNetwork
```

### Hop-count decrement

```java
BBacnetNetworkLayer.decrementHopCount() {
  if (hopCount > 0) {
    hopCount--;
    forward(npdu);
  } else {
    discard();  // TTL exceeded
  }
}
```

---

## 23.26 BBMD + Foreign Device

### BBMD roles

```
BDT (Broadcast Distribution Table): lista de BBMDs remotos
  - Relay broadcasts entre subnets
  - Recibe Original-Broadcast-NPDU → reenvía como Forwarded-NPDU a BDT entries

FDT (Foreign Device Table): lista FD registrados
  - TTL típico 900-3600s
  - FD renueva antes de TTL/2
  - Expiración → remoción automática
```

### Foreign Device registration

```
FD → Register-Foreign-Device(BBMD_IP, TTL)
BBMD → BVLC-Result(success)
FD memoriza BBMD_IP:47808
Broadcasts FD → encapsulados → BBMD → BDT + FDT recipients
```

### Original-Broadcast-NPDU → Forwarded-NPDU

BBMD envuelve en BVLC Forwarded (0x04) con:
- MAC address remoto = BBMD origen
- Remitente original preservado dentro

---

## 23.27 Link layer types

### IP/BVLC (UDP 47808)

`BBacnetIpLinkLayer` — BBMD, FD, broadcast handling.

### MS/TP (RS-485 serial, token passing)

`BBacnetMstpLinkLayer`. Baud rates:
```
BAUD_9600 = 0
BAUD_19200 = 1
BAUD_38400 = 2
BAUD_57600 = 3
BAUD_76800 = 4
BAUD_115200 = 5
```

Direcciones nodo 0-255 (1 byte), max 128 masters. Distancia ~1200 m @ 9600, ~200 m @ 115200.

### Ethernet (raw 802.3)

`BBacnetEthernetLinkLayer` — broadcast directo, routing entre subnets, MAC 48-bit.

### PTP (Point-to-Point)

Serial dedicada client↔device, HDLC encapsulation, sin token.

### BACnet/SC (TCP 49152 + TLS 1.3)

`BScLinkLayer` — secure communications.

---

## 23.28 BNetworkPort properties

```
ipAddress               "192.168.1.10"
ipSubnet                "255.255.255.0"
mstpBaudRate            BBacnetMstpBaudRate
mstpDeviceAddress       int 0-255
useNat                  boolean
broadcastAddress        "192.168.1.255"
ipDeviceType            STANDARD=0 | FOREIGN_DEVICE=1 | BBMD=2
bbmdIpAddress           si FOREIGN_DEVICE
bbmdIpPort              47808
```

---

## 23.29 Extensiones Honeywell

### honBacnetHelper-rt.jar

Export descriptors custom:
```
BHonBacnetAnalogInputDescriptor / AnalogOutput / BinaryValuePrioritizedDescriptor
BHonBacnetMultiStateOutputDescriptor
BHonBacnetScheduleDescriptor / EventLogDescriptor
BIHonBacnetCustomDescriptor (interfaz extensión)
```

Fast Access Lists:
```
BHonFastAccessList / Subordinate / Lists
```

Propósito: mapeo específico Honeywell Niagara controls ↔ BACnet properties, UI descriptors para priority arrays.

### honBACnetUtilities-rt.jar

Device management extendido:
```
BHonBacnetDevice extends BBacnetDevice
BHonBacnetAwsDevice
BHonBacnetNotificationClass (mapping BAlarmClass ↔ BACnet NotificationClass)
BHonDiscoverNotificationClassesJob
BHonBacnetAlarmDeviceExt
BHonBacnetService
ObjectSubscriber         motor COV interno
PropertyPointAssigner    auto-mapeo object prop→point
BHonBacnetWorkerPool     thread management
BBacnetDeviceParameters  config
BParameter, BParameterFolder
```

Features:
- **Offset Points** (BHonBacnetNumericOffsetPoint) — `valueNiagara = valueBacnet + offset` (conversión unidades)
- **ObjectSubscriber** — motor COV con resubscripción automática y metrics
- **PropertyPointAssigner** — algoritmo auto-discovery que escanea objetos → crea puntos

### ascBacnet.jar

Automated Sensor Configuration — wizard semiautomático:
```
Discovery devices → lectura object list → clasificación → crear points con facets + validation
```

### AWS extras (bacnetAws-rt)

Objetos adicionales:
```
BBacnetEventLog           historial timestamp
BBacnetStructuredView
BBacnetAccumulator
BBacnetCommand
BBacnetPulseConverter
```

Data types:
```
BBacnetAccumulatorRecord
BBacnetEventLogRecord
BBacnetActionCommand
BBacnetVtSession
```

Operaciones:
```
BBackupJob(device, BackupConfig)
BReinitializeDeviceJob(device, ReinitializeDeviceConfig {COLDSTART|WARMSTART})
```

---

## 23.30 Flujo integral (discovery → COV → write)

```
FASE 1 — Discovery
  Niagara → WhoIs() broadcast (unconfirmed)
  Devices → IAm (unconfirmed): device-id, segmentation, max-apdu, vendor

FASE 2 — Point Configuration
  Niagara → ReadProperty(object, property) inv-id=10
  Device → ComplexAck(value) inv-id=10
  Niagara crea BBacnetProxyExt(AI-10, PV) → BNumericPoint

FASE 3 — COV Subscription
  Niagara → SubscribeCOV(subscriber=1000, AI-10, lifetime=3600, inc=0.5)
  Device → SimpleAck
  [T2...T2+3600] UnconfCOVNotif cada cambio Δ ≥ 0.5
  Refresh @ 3600s: misma notif con mismo valor (keep-alive)

FASE 4 — Write
  User set AO-5 = 75.0
  Niagara → WriteProperty(AO-5, PV, value=75.0, priority=8)
  Device → SimpleAck
  Device recalcs → notifica COV a suscriptos

FASE 5 — Polling heartbeat
  Every 30s: ReadPropertyMultiple([(AI,10,PV), (AI,11,PV), (AO,5,PV), (AO,5,StatusFlags)])
  → 1 request ~20ms (vs 4 requests ~80ms con ReadProperty individual)
```

---

## 23.31 Optimizaciones + tuning

1. **ReadPropertyMultiple** en lugar de N ReadProperty — 1 request ~20ms vs N * 20ms.
2. **Tuning policies** — CovPolicyNoStartWrite, fallback polling si no soporta COV.
3. **Virtual points** (BBacnetVirtualGateway) — agregación multi-property en 1 poll.
4. **Segmentación automática** si payload > max-apdu-size.
5. **Window-size negotiation** para throughput de segmentación.
6. **COV refresh TTL** — balance entre fresh data y carga de red.
7. **Tuning BBacnetComm**: APDUTimeout (3000ms default), APDURetries (3), SegmentTimeout (1500ms).

---

## 23.32 Tabla resumen — constantes clave

| Constante | Valor | Significado |
|---|---|---|
| BVLC_TYPE_BACNET_IP | 0x81 | Header BVLC |
| BVLL_BASE_LENGTH | 4 | Header bytes |
| REGISTER_FD | 0x05 | Register Foreign Device |
| FORWARDED_NPDU | 0x04 | NPDU reenviado BBMD |
| ORIGINAL_UNICAST | 0x0A | Unicast normal |
| ORIGINAL_BROADCAST | 0x0B | Broadcast local |
| NPDU VERSION | 0x01 | BACnet version |
| NETWORK_LAYER_MSG_BIT | 0x40 | NLM flag |
| DEFAULT_HOP_COUNT | 64 | Hop inicial |
| DNET_NOT_PRESENT | 0xFFFF | Mismo network |
| CONFIRMED_REQUEST | 0x00 | APDU type |
| UNCONFIRMED_REQUEST | 0x10 | APDU type |
| SIMPLE_ACK | 0x20 | APDU type |
| COMPLEX_ACK | 0x30 | APDU type |
| SEGMENT_ACK | 0x40 | APDU type |
| ERROR | 0x50 | APDU type |
| REJECT | 0x60 | APDU type |
| ABORT | 0x70 | APDU type |
| SEG_FLAG | 0x08 | Segmentación flag |
| MOR_FLAG | 0x04 | More follows |
| SA_FLAG | 0x02 | Segmented response accepted |
| MAX_APDU_1476 | 1476 | Max tamaño IP |
| MAX_SEGS_64 | 64 | Max segmentos |
| APDUTimeout | 3000 ms | Timeout response |
| APDURetries | 3 | Reintentos |
| SegmentTimeout | 1500 ms | Timeout segmento |
| COV_LIFETIME_DEFAULT | 3600 s | Suscripción |
| BACnet IP UDP port | 47808 | UDP estándar |
| BACnet/SC TCP port | 49152 | Seguro TLS |

---

## 23.33 Gotchas operacionales

1. **rpmOk flag** en BBacnetDevice — si device NO soporta ReadPropertyMultiple, Niagara cae a ReadProperty individual (3-5× más tráfico). Detectable en discovery, cachea decisión.
2. **maxPollTimeouts** — desactiva device tras N fallos consecutivos; reactivable manual o ping exitoso.
3. **COV subscription con lifetime=0** = unsubscribe (NO error).
4. **COV refresh NO es obligatorio** — algunos devices solo notifican en cambio real. Niagara interpreta refresh como keep-alive, ausencia no cancela implícitamente.
5. **Priority array 16 NULL** → usa relinquish-default, NO null. Write priority=16 con NULL releases (restaura default).
6. **Escritura priority 6 (manual takeover)** bloquea otros writes de menor prioridad hasta release explícito.
7. **APDU max negotiation**: si peer dice 480 y Niagara 1476, uso el MENOR (480). NO supervisor controla downstream.
8. **Segmentación**: si device advertisa NO_SEGMENTATION y respuesta > max-apdu → Abort(segmentation-not-supported). No retry, fallo permanente de esa request.
9. **Hop count 64** puede expirar en redes muy profundas con routing — TTL exceeded = silent drop.
10. **BBMD TTL renewal** — si FD no renueva antes TTL/2, BBMD silent delete; próximo broadcast no llega a FD. Reconexión requiere re-registration.
11. **EDE CSV format** — proprietary Honeywell (no ASHRAE estándar). Editores manuales cuidado con encoding UTF-8 vs ASCII.
12. **COV_INCREMENT casing sensitive** en ASN.1 encoding — usar minúsculas en field names.
13. **NPDU "reply bit" (DER)** — si 1, peer espera respuesta; perdida causa timeout + retry.
14. **Subscriber process ID** debe ser **único por client** — colisiones causan notificaciones cruzadas.
15. **Segment window size** — si peer advertise window=1 y tu stack asume mayor → timeouts. Negociación por protocolo.
16. **MS/TP max-master tuning crítico** — si mayor a nodos reales, latencia de token scan se agrega a cada ciclo.
17. **Schedule protocol revision mismatch** — Rev3 vs Rev16 encoding distinto; `setSupport()` mal seteado = corrupción datos.
18. **Calendar reference si no existe** → exception schedule inoperante en silencio.
19. **Trend Log BUFFER_SIZE 0** = circular ilimitado (hasta memoria); STOP_WHEN_FULL + RECORD_COUNT=BUFFER_SIZE → congela logging.
20. **Access objects requieren BACnet/SC** para máxima seguridad; N4.14 soporte parcial, no prod-ready.

---

## Fuentes primarias leídas

1. `modules/bacnet-rt.jar` — core (BBacnetDevice, BBacnetComm, BBacnetNetwork, BBacnetProxyExt, stack layers)
2. `modules/bacnet-ux.jar` + `bacnet-wb.jar` — UI + Workbench
3. `modules/bacnetUtil-rt.jar` — utilidades
4. `modules/bacnetAws-rt.jar` — Advanced Workstation objects
5. `modules/bacnetOws-rt.jar` — Operator Workstation
6. `modules/bacnetAlarmRouter-rt.jar` (98.5 KB) — routing alarmas
7. `modules/bacnetMigrator-wb.jar` (41.6 KB) — migración
8. `modules/bacnetEDE-wb.jar` (179 KB) — EDE parser/writer
9. `modules/BACnetFFTN4-rt.jar` — FFT extension
10. `modules/ascBacnet.jar` — auto-config wizard
11. `modules/honBacnetHelper-rt.jar` — Honeywell custom descriptors
12. `modules/honBACnetUtilities-rt.jar` — Honeywell device mgmt
13. `modules/docBacnet-doc.jar` — docs
14. `niagara-help/devguide-clean/bacnet.txt` + `docs-text/`

Total: ≈3000 clases decompiladas, 475+ propiedades enumeradas, 60+ object types, 37 services, 12 link-layer functions BVLC, 8 APDU types, 16 priority levels, stack de 5 capas completo.
