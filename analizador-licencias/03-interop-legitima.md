# 03 — Interop legítima: leer y validar licencias propias sin alterar el check

> **Propósito**: cómo **leer** y **validar** licencias propias (inventario, capacity planning,
> verificación de integridad de un archivo recibido del vendor) usando la API pública y el formato
> del archivo — **sin modificar el check de validación** de la plataforma y sin producir firmas.

---

## 1. La API pública (lectura, no bypass)

Todo pasa por `javax.baja.license.LicenseManager`, obtenido con `Sys.getLicenseManager()`
(`niagara-mental-model.md:171-185`; `notes/02-licensing.md:246`). Implementación real:
`SubscriptionLicenseManager extends NLicenseManager` (`niagara-mental-model-bloque41.md:394`).

| Método | Qué devuelve | Uso legítimo |
|---|---|---|
| `getFeature(vendor, name)` | `Feature` o excepción | Leer atributos de una feature concreta |
| `checkFeature(vendor, name)` | void, lanza `FeatureNotLicensedException` | Probar si una feature está licenciada (try/catch) |
| `getFeatures()` | Iteración de la base de licencias | Inventario completo |
| `getLicenseMaintenanceExpiration(vendor)` | `Optional<Long>` (millis SMA) | Monitorear expiración de SMA por vendor |
| `Feature.get/get(String,String)/getb/geti` | Atributos string→valor | Leer `point.limit`, `device.limit`, `sma.exempt`, … |
| `Feature.check()` | valida existencia + no expirada | Verificación puntual |

Referencias: `notes/02-licensing.md:248-256`; `niagara-mental-model-bloque41.md:348-361`.

Ejemplo de lectura (patrón canónico de los módulos del corpus):

```java
Feature f = Sys.getLicenseManager().getFeature("tridium", "modbusTcp");
String dl = f.get("device.limit");          // "none" = ilimitado
boolean export = f.getb("export", false);
int points = f.geti("point.limit", 0);
```
(`sources/decompiled/modbusTcp-rt/com/tridium/modbusTcp/BModbusTcpNetwork.java:51-53`;
`notes/02-licensing.md:215-223`.)

Reglas de lectura:
- Atributo omitido o `"none"` = ilimitado (`niagara-mental-model-bloque14.md:64`).
- Acceso **string-based y typo-prone**: `getb("Sma.Exempt", false)` devuelve `false` en silencio —
  usar las cadenas exactas del archivo (`niagara-mental-model-bloque41.md:363-365`).

---

## 2. Parseo del archivo `.license` (fuera de la plataforma)

El archivo es XML plano; no requiere la plataforma para leerse (`notes/02-licensing.md:30-58`):

```xml
<license version="4.15" vendor="HoneywellCentraLine" hostId="Win-6E6E-10AC-D1DD-8276"
         expiration="2027-03-31" generated="2026-04-02">
  <feature name="clCbus" expiration="2027-03-31" history.limit="none" point.limit="none"
           schedule.limit="none" device.limit="none"/>
  <signature>MC4CFQDOSizKvGQPhgjQ7JjqUSRDEDz3Zg…</signature>  <!-- firma elidida; valor íntegro en niagara-mental-model-bloque126.md:146-152 -->
</license>
```
(instancia real en `niagara-mental-model-bloque126.md:146-152`).

Campos a extraer para un inventario:
- Atributos del root: `version`, `vendor`, `hostId`, `expiration` (maestra), `generated`.
- Por `<feature>`: `name`, `expiration` (override), y atributos libres de límites/SMA.
- `<signature>`: base64 de la firma DSA (para verificación §3).

Ubicación: `/security/licenses/` (alias) y `/security/licenses/db/<hostId>/` (canónico)
(`niagara-mental-model.md:121`; `notes/02-licensing.md:18`). Multi-vendor en el mismo host:
`Webs.license` (Tridium, 150+ features), `Honeywell.license` (27 features),
`HoneywellCentraLine.license` (1 feature) (`niagara-mental-model.md:113-122`).

En el corpus el parseo de la plataforma usa `XElem` (`Feature.load(XElem)`/`save()`),
`niagara-mental-model-bloque41.md:348-361` — el formato es el mismo que el XML del archivo.

---

## 3. Verificación de integridad de una licencia recibida (sin tocar el check)

Objetivo: confirmar que el `.license` que te entregó tu vendor es **auténtico e íntegro**, antes de
importarlo. No se altera nada del sistema; es una verificación offline.

1. **Reunir la contraparte pública**: el `{vendor}.certificate` del mismo vendor (raíz de confianza:
   `Tridium.certificate` con `<publicKey algorthm="DSA">`, OID `1.2.840.10040.4.1`, DSA-1024;
   `niagara-mental-model-bloque126.md:154`).
2. **Verificar la firma**: `<signature>` base64 → DER `SEQUENCE{INTEGER r(20B), INTEGER s(20B)}`
   (DSA, SHA-1) → verificar contra la clave pública del certificate. Herramientas estándar de
   crypto/ASN.1 sirven; la plataforma lo hace con `LicenseUtil.verify` (baja.jar) y el engine nativo
   `DsfSha1WithDsaSignature` (dsfspi.dll) (`niagara-mental-model-bloque41.md:373-388`;
   `niagara-mental-model-bloque126.md:53,157`).
3. **Coherencia de campos**: `hostId` == el del destino; `generated` ≤ hoy; `expiration` no vencida;
   vendor con `.certificate` presente (`notes/02-licensing.md:60-67`).
4. **Importar por la vía oficial** (`licenses/inbox/` → LicenseManager valida → mueve a
   `db/<hostId>/`), que re-ejecuta los mismos 5 checks y **descarta el archivo entero** ante
   cualquier falla (`niagara-mental-model-bloque40.md:683-691`; `notes/02-licensing.md:60-68`).

> Qué NO hacer: no re-firmar archivos, no fabricar `{vendor}.certificate`, no editar atributos para
> "arreglar" una licencia vencida — cualquier edición invalida la firma y el archivo se descarta
> (all-or-nothing). El camino legítimo es pedir re-emisión al vendor.

---

## 4. Herramientas CLI y superficie

- `nre -licenses`: lista features + propiedades del host (`notes/02-licensing.md:259`).
- `plat.exe` (vista de plataforma): resumen de licencia (`niagara-mental-model-bloque14.md:239-241`).
- JMX: MBean `com.tridium.sys.license:type=LicenseManager` para monitoreo
  (`niagara-mental-model-bloque20.md:293`).
- Workbench `Tools → License Manager`: UI de import y lista por vendor
  (`niagara-mental-model-bloque40.md:683-691`).

---

## 5. Casos de uso legítimos

1. **Inventario de features por estación**: `getFeatures()` por vendor; documentar qué límites
   aplican (`point.limit`, `device.limit`, `history.limit`, `schedule.limit`, `camera.limit`,
   `foxStream.limit`, `Dictionary.limit`…) (`niagara-mental-model-bloque14.md:63-80`;
   `niagara-mental-model-bloque269.md:29-46`).
2. **Capacity planning**: leer límites y comparar con el modelo real — recordando que virtual points
   y federación no inflan el count local (`niagara-mental-model-bloque28.md:1274-1314`).
3. **Monitoreo de expiración**: `getLicenseMaintenanceExpiration(vendor)` + alarmas
   `LicenseExpiresIn`/`LicenseExpired` (`notes/02-licensing.md:136-140`;
   `niagara-mental-model-bloque14.md:227-233`).
4. **Checklist pre-despliegue**: verificar hostId, generated, expiration, certificate del vendor y
   firma (§3) antes de importar; confirmar que la raíz de `/security/licenses/` quedó como alias
   bit-exacto de `db/<hostId>/` (`niagara-mental-model-bloque40.md:607-610`).
5. **Verificación post-cambio de hardware**: tras NIC swap/VM clone el hostId cambia; validar con
   `Sys.getHostId()` y pedir re-emisión para el nuevo (`niagara-mental-model-bloque41.md:534`).

---

## 6. Gotchas

- **String-based, typo-prone**: los atributos se leen por cadena exacta; un typo devuelve el default
  silencioso (`niagara-mental-model-bloque41.md:363-365`).
- **Reloj**: el check `generated ≤ now` y la SMA usan el reloj local; nCloud usa el suyo
  (`notes/02-licensing.md:60-67`; `niagara-mental-model-bloque32.md:339-345`).
- **Case del hostId**: la comparación normaliza con `.toUpperCase()` en la plataforma
  (`niagara-mental-model-bloque139.md:92-93,127`).
- **`"none"` ≠ número**: leer límites como strings; `"none"` es ilimitado (`notes/02-licensing.md:215-223`).
- **License sin módulo = nada**: p. ej. `axvelocity` sin `axvelocity-rt.jar` instalado deja `.pxvm`
  inop aunque la license exista (`niagara-mental-model-bloque36.md:G6`).

---

## Fuentes

- `notes/02-licensing.md` — API, esquema XML, pipeline, límites
- `niagara-mental-model.md` §2 — LicenseManager, SMA
- `niagara-mental-model-bloque41.md` §41.6 — `Feature`, `LicenseUtil`, `SubscriptionLicenseManager`
- `niagara-mental-model-bloque126.md` — formato de firma DSA y verificación
- `niagara-mental-model-bloque14.md` / `bloque28.md` — límites, counting
- `niagara-mental-model-bloque40.md` §40.4.x — layout, import
