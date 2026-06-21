# Bloque 85 — Model Sync: cómo el modelo semántico `hon:` de la station sube al cloud (JSON-LD → Azure Blob), canal asimétrico — `SentienceModelSync` + `fcModelSync`

> Investigación empírica del **motor de sincronización de modelo** que cierra el ciclo **tag → cloud**: toma el modelo semántico `hon:` que los tag dictionaries del [Bloque 82] derivan sobre la station, lo serializa en **JSON-LD** (ontología Honeywell `honsmw:/honcore:/honhvac:`) y lo sube al cloud. Es el `BModelSyncService` que el onboarding del [Bloque 84] instala.
>
> Dos familias paralelas: `SentienceModelSync` (Honeywell, N4 4.6, legacy, empaqueta el Azure Storage SDK) y `fcModelSync`+`fcModelSyncBacnet`+`fcModelSyncNiagara` (Tridium, 2023.14, "Forge Connect Model Sync") — el mismo split **hon (Honeywell) vs fc (Forge Connect)** del [Bloque 82].
>
> Fuentes: `organized/{SentienceModelSync,fcModelSync,fcModelSyncBacnet,fcModelSyncNiagara}/<m>-rt/vineflower/...` (+ `module.xml`, `META-INF/maven/.../pom.properties`). SentienceModelSync tiene 555 .java pero solo ~20 son código Honeywell (`com.honeywell.*`); el resto son SDKs empaquetados (Azure Storage v8, okhttp, gson, jackson, kotlin, commons-lang3).
> Método: 2 sub-agentes + **verificación directa** de cada `extends`, del uso real del Azure Storage SDK (`CloudBlockBlob`, pom.properties `version=8.0.0`) y del vendor/descripción de `fcModelSync`. `[CERT]` = verificado por mí; `[CERT-a]` = cita del sub-agente; `[INFER]` = deducción.
>
> Capa 22 (OEM deofuscados), continúa [Bloque 84]. **Cierra el ciclo tag→cloud**: [Bloque 82] (tags `hon:`) → [Bloque 85] (modelo al cloud). Usa [Bloque 83] (conectores) y [Bloque 84] (onboarding instala el service).

---

## 85.1 — Las dos familias + el canal asimétrico `[CERT]`

| Módulo | Vendor / N4 | Clase service verificada | Canal de subida del modelo |
|--------|-------------|--------------------------|----------------------------|
| `SentienceModelSync` | **Honeywell, 4.6 / v2.0.6** (legacy) | `BModelSyncService extends BAbstractService` (:90) | Azure Blob vía **Azure Storage SDK v8** (`CloudBlockBlob.upload()`) |
| `fcModelSync` | **Tridium, 2023.14** ("Forge Connect Model Sync Service") | `BModelSyncService extends BAbstractService` (:105) | Azure Blob vía cliente HTTP propio (`AzureBlobOutputStream`, `x-ms-version 2021-08-06`) |
| `fcModelSyncBacnet` | Tridium 2023.14 | `BBacnetModelDiscoverer extends BModelDiscoverer` (:26) | (soporte, no sube) |
| `fcModelSyncNiagara` | Tridium 2023.14 | `BNiagaraHistorySourceDiscoverer extends BHistorySourceDiscoverer` (:33) | (soporte, no sube) |

**HALLAZGO CENTRAL — canal asimétrico `[CERT-a]`** (coinciden ambos análisis): el modelo NO viaja por el AMQP/IoT Hub del [Bloque 83]. Va por un **side-channel de file upload**:

```
UPLINK del modelo (archivo):
  1. auth RPK ECDSA → IdentityJwt (Bloque 83.4)
  2. POST {FileUploadService}/api/fileupload/systems/{systemGuid}/request  (Bearer JWT)
        → respuesta: FileUploadSasUrl  (SAS URL de Azure Blob)
  3. PUT del ZIP a Azure Blob con la SAS URL
        SentienceModelSync: CloudBlockBlob.upload()  [Azure Storage SDK v8]
        fcModelSync:        AzureBlobOutputStream (HTTP PUT por bloques)

DOWNLINK del resultado (status/modelId):
  por IoT Hub AMQP C2D → BUpdateModelSyncStatusCommand / BUpdateModelIdCommand
```

El `FileUploadService` base URL sale del `getConnectionInfo("FileUploadService")` del connector ([Bloque 83]) — el mismo `/api/system/connections` que da el SAS del IoT Hub, pero una entrada distinta. **Telemetría de puntos → AMQP IoT Hub; archivo de modelo → Azure Blob; status del modelo → IoT Hub C2D.** Tres caminos, intencional.

> Paralelo [Bloque 82]: igual que `honTagDictionary` (Honeywell) vs `fcTagDict` (Forge Connect/Tridium) producen el mismo namespace `hon:`, aquí `SentienceModelSync` (Honeywell legacy) y `fcModelSync` (Tridium moderno) producen el **mismo JSON-LD `honsmw:`** por el **mismo canal Azure Blob**. `fcModelSync` es el reemplazo Tridium moderno del `SentienceModelSync` Honeywell viejo (4.6 vs 2023.14). No hay herencia entre ambos; la interfaz `BIModelExtractor` es propia de cada uno.

---

## 85.2 — El modelo: JSON-LD con la ontología Honeywell `[CERT]`

El extractor (`BContextDiscoveryModelExtractor extends BComponent implements BIModelExtractor`, SentienceModelSync :117 / fcModelSync :137) recorre la station y produce un grafo **JSON-LD OWL**. `BIModelExtractor extends BInterface` (:10) tiene un solo método: `File getModelFile(String siteName)`.

**Cómo extrae** `[CERT-a]`:
- Arranca por `rootEntityTag` (ej. `hon:Gateway`) vía NEQL (`BSearchService`).
- Recorre recursivamente las relaciones configuradas (`entityRelations` = `honhvac:containsProperty;honcore:containsElement`) usando la API `Relation` del componente.
- Por entidad extrae: `@id` (de `entityIdTag`), `@type` (de `entityTypeTag`), tags marker non-`hon`, multi-value tags, **facets de punto** (unit, min, max, precision, trueText/falseText), enum concepts.
- Cruza con `BCloudSentienceDevice` para incluir solo puntos con cloud proxy/history export (`isExportCloudOnlyPoints`).
- Escribe `hon:OnboardedVia` = **`"Forge Connect"`** (si `systemId` empieza con `GUID`) o **`"Niagara Cloud Connector"`** — el discriminador de las dos rutas.

**Schema** `[CERT-a]`: tipos `honsmw:Property` / `honsmw:PropertyBinding` / `honsmw:Attribute` / `honsmw:Concept`, `honcore:Controller` / `honcore:LogicalController`. `@context` apunta a `http://www.honeywell.com/models/sites/{siteName}#`; `owl:Ontology` importa `sentiencemodelwriter/1.0` + propertyroles `hvac`/`lighting`/`hospitality`. El `PropertyBinding` lleva `hasSystemGUID` (= el `id` del `BCloudConnector`, [Bloque 83.2]) + `hasBindingToID` (el sentiencePointId) — **es el pivote que liga el modelo semántico con la telemetría AMQP**. Serializado con `com.tridium.json.JSONWriter` (no Jackson, aunque esté empaquetado).

**Salida**: `{siteName}.jsonld` + `{siteName}_context.json` (config: unit Imperial/Metric, contextDiscoveryRequired, cache, retention) → zipeados a `{siteName}_CD_Model.zip` en `NiagaraSharedUserHome`.

---

## 85.3 — SentienceModelSync: la variante Honeywell legacy (Azure Storage SDK v8) `[CERT]`

`BModelSyncService extends BAbstractService` (:90), vendor Honeywell, N4 **4.6** v2.0.6. Slots `[CERT-a]`: `siteName`, `unit` (Imperial default), `contextDiscoveryRequired`, `modelExtractor`, `modelWriter` (`BModelWriterImpl`), `modelSyncStatus`/`modelId` (readonly, llenados por downlink). Acciones: `syncModel`, `updateUploadStatus`, `updateModelId`.

**Flujo `BModelSyncJob extends BSimpleJob` (:20)** `[CERT-a]`: `getModelFile()` → escribe `.jsonld` (50%) → `createContextDiscoveryProperties()` (`_context.json`) → `addFilesToZipFile()` (`_CD_Model.zip`, 75%) → borra fuentes → `modelWriter.uploadFileToSentience(zip)` → status `"MODEL UPLOADED WAITING FOR UPDATES.."`. **Siempre full sync, sin delta, sin scheduler** (trigger = acción `syncModel` manual o externa).

**Upload** `[CERT]`: `FileUploadHandler` usa `com.microsoft.azure.storage.blob.CloudBlockBlob` (Azure Storage SDK **v8.0.0**, verificado en pom.properties) → `blob.upload(...)` contra la SAS URL. La SAS URL se obtiene por REST (`FileUploader`, OkHttp3 + Gson) al endpoint `api/fileupload/systems/{guid}/request` (v1) o `/v2/request` (v2 con DataClassificationTags). Esto explica los 555 archivos: ~250 son el Azure Storage SDK (blob/table/file/queue) + okhttp/okio/kotlin/gson/jackson/commons-lang3 empaquetados.

**Downlink** `[CERT]`: `BUpdateModelSyncStatusCommand extends BCloudCustomCommand` (:14, name `"UpdateModelSyncStatus"`) + `BUpdateModelIdCommand` (`"UpdateModelId"`). Se registran en `BCloudSentienceDevice → BCloudCommandsDeviceExt → BCloudCommands`; el nCloudDriver ([Bloque 83.5]) recibe el C2D por IoT Hub y los despacha por nombre → escriben los slots `modelSyncStatus`/`modelId`.

---

## 85.4 — fcModelSync (Forge Connect) + soporte BACnet/Niagara `[CERT]`

`BModelSyncService extends BAbstractService` (:105), vendor **Tridium**, "Forge Connect Model Sync Service", 2023.14. Mismo extractor/schema/canal que SentienceModelSync, pero **sin empaquetar el Azure Storage SDK**: usa un `AzureBlobOutputStream` propio (HTTP PUT por bloques, `x-ms-version 2021-08-06`) sobre el SAS URL del `FileUploadService`. Más liviano (26 java vs 555). Logger `modelSync.sentience.service` (mismo namespace → código emparentado).

**Sub-módulos de soporte** `[CERT]` (enriquecen el modelo, NO son extractores ni implementan `BIModelExtractor`):
- **`fcModelSyncBacnet`**:
  - `BBacnetModelDiscoverer extends BModelDiscoverer` (:26) — agente sobre `bacnet:BacnetDevice`; corre discovery BACnet nativo y escribe `description` (Object Description) + `units` (property id 117) en el `proxyExt` **antes** de la extracción → modelo más rico.
  - `BacnetCloudWriter extends LinkingCloudWriter` — **writeback** cloud→station para puntos BACnet (lee prioridad activa `bac:17`, configura `priorityArray`).
- **`fcModelSyncNiagara`**:
  - `BNiagaraHistorySourceDiscoverer extends BHistorySourceDiscoverer` (:33) — correlaciona `BNiagaraHistoryImport` remotas con el proxy point Niagara real (resuelve el OrdQuery del source).
  - `FoxCloudWriter implements ICloudWriter, Interest` — writeback cloud→station para puntos nDriver vía protocolo **Fox** (`BFoxClientConnection`/`BPointChannel`).

> Estos discoverers/writers muestran el patrón de extensibilidad por protocolo: el core fcModelSync extrae el grafo genérico, y cada driver (BACnet, Niagara) aporta un discoverer que enriquece sus puntos + un cloud writer que ejecuta las escrituras downlink en su protocolo nativo. Conecta con el control remoto del [Bloque 83.5].

---

## 85.5 — Síntesis: el ciclo completo tag → cloud

**El pipeline end-to-end de Honeywell BMS-to-cloud, ahora completo en el mental model**:

```
[Bloque 82] tag dictionaries (honTagDictionary/fcTagDict)
    → derivan el modelo semántico hon: sobre la station (on-demand, NO en BOG)
[Bloque 84] honCloudEasyOnboard
    → instala BModelSyncService + BContextDiscoveryModelExtractor (scope, rootTag, relations)
[Bloque 85] model sync
    → extrae el grafo → JSON-LD honsmw: → ZIP → Azure Blob (SAS URL side-channel)
[Bloque 83] nCloudDriver
    → telemetría/alarmas/historiales por AMQP IoT Hub; PropertyBinding liga modelo↔datos por systemGUID+pointId
    → comandos downlink (write/invoke + UpdateModelSyncStatus/ModelId) por IoT Hub C2D
```

**Tres canales distintos al mismo backend Sentience/Forge** (clave operacional):
1. **Datos** (puntos/alarmas/historiales) → AMQP IoT Hub ([Bloque 83]).
2. **Modelo** (JSON-LD del building) → Azure Blob vía SAS URL ([Bloque 85]).
3. **Control/estado** (comandos, status del modelo) → IoT Hub C2D.

**Paralelo hon/fc (consistente con [Bloque 82])**: Honeywell shippeó primero la versión propia (`honTagDictionary` + `SentienceModelSync`, N4 4.6, ~2020, empaqueta SDKs pesados); Tridium luego unificó con la versión "Forge Connect" (`fcTagDict` + `fcModelSync`, 2023.14, liviana). Ambas convergen en el mismo namespace `hon:` y el mismo JSON-LD `honsmw:`.

**Seguridad ([Bloque 75])**:
- El SAS URL de Azure Blob es un **token de escritura temporal** a un container del tenant cloud — su fuga permitiría subir/sobreescribir el modelo del site.
- El modelo JSON-LD expone la **topología completa del building** (equipos, puntos, jerarquía) — es información sensible de reconocimiento si el blob/SAS se compromete.
- Versión legacy 4.6 (SentienceModelSync) empaqueta Azure Storage SDK v8 + okhttp + jackson + commons-lang3 — **superficie de CVEs de dependencias** (mismo riesgo que el [Bloque 32.3] señaló para jsonToolkit/Jayway). Un site 4.14 corriendo el módulo 4.6 arrastra esas libs viejas.

**Para MX60**: el patrón JSON-LD + ontología + SAS-URL-upload es la referencia si MX60 necesita exportar un modelo semántico a un consumidor cloud. El cliente Azure Blob liviano de `fcModelSync` (sin SDK pesado) es preferible al SDK v8 empaquetado de SentienceModelSync.

**Pendiente conocido**: el detalle interno del Azure Storage SDK v8 (255 clases) no se destiló (es SDK estándar Microsoft, no Honeywell). `BModelWriterImpl`/`createContextDiscoveryProperties` citados vía sub-agente `[CERT-a]`. Con esto, **la familia cloud Honeywell queda completa** (Bloques 83-84-85): conectividad + auxiliares + model sync.
