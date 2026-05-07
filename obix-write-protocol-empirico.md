# oBIX Write Protocol — Comprobación Empírica

**Fecha**: 2026-04-28
**Contexto**: Pruebas con station Niagara N4 vía oBIX REST. Objetivo: confirmar full-duplex (read + write) y entender el modelo correcto de escritura sobre `BNumericWritable`.

---

## Setup

| Campo | Valor |
|-------|-------|
| Station host | `https://172.19.160.1` (Windows host accedido desde WSL2) |
| Cliente | `curl` |
| Auth | HTTP Basic (`-u TUNEL:Alser12345`) — usuario asignado a `BLegacyBasicAuthenticationScheme` |
| TLS | Self-signed → `-k` para ignorar verificación |
| Punto de prueba | `config/Drivers/CODIGOS/Amp Fan` (BNumericWritable) |

### URL escape Niagara

`Amp Fan` → `Amp$20Fan` en el path. Es el escape **propio de Niagara** (`$XX` = hex char), NO el URL-encode HTTP estándar (`%20`). En bash hay que escapar el `$`:

```bash
URL='https://172.19.160.1/obix/config/Drivers/CODIGOS/Amp$20Fan/'   # single quotes
# ó
URL="https://172.19.160.1/obix/config/Drivers/CODIGOS/Amp\$20Fan/"  # backslash
```

---

## Test 1 — READ (GET) ✅

```bash
curl -sk -u 'TUNEL:Alser12345' \
  'https://172.19.160.1/obix/config/Drivers/CODIGOS/Amp$20Fan/fallback/'
```

Respuesta:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<real val="23.0"
      href=".../Amp$20Fan/fallback/"
      is="/obix/def/baja:StatusNumeric"
      display="23,0 {ok}"
      displayName="Fallback"
      unit="obix:units/null"
      xmlns="http://obix.org/ns/schema/1.0"/>
```

**Confirmado**: lectura OK, status `{ok}`, val `23.0`. La station NO redirige `/obix/...` a `/login` cuando viene `Authorization: Basic` válido.

---

## Test 2 — WRITE incorrecto: `PUT fallback/` ❌

```bash
curl -sk -u 'TUNEL:Alser12345' -X PUT \
  -H 'Content-Type: text/xml' \
  --data '<real val="42.0" href=".../fallback/" xmlns="http://obix.org/ns/schema/1.0"/>' \
  'https://172.19.160.1/obix/config/Drivers/CODIGOS/Amp$20Fan/fallback/'
```

Respuesta HTTP `200 OK`, pero con error de aplicación oBIX en el body:

```xml
<err href=".../fallback/"
     display="Cannot translate: <real val='42.0' .../>"
     xmlns="http://obix.org/ns/schema/1.0"/>
```

Re-read confirma que el valor sigue en `23.0`. **No escribió.** Tampoco cambia agregando `is="/obix/def/baja:StatusNumeric"`.

### Causa raíz

`fallback` es un slot **read-only via oBIX**. Lo confirmamos inspeccionando el padre `Amp Fan/`. Solo dos slots tienen `writable="true"`: `facets` y `wsAnnotation`. Los demás (`out`, `in1..in16`, `fallback`) se exponen para lectura, NO para PUT.

El error `Cannot translate` no es de schema ni de contract — es de **routing**: no hay write handler registrado en ese path. Por diseño: `fallback` es metadata de configuración, modificable solo vía Workbench / BatchEditor / Fox.

---

## Test 3 — Inspección del padre ✅ (clave didáctica)

```bash
curl -sk -u 'TUNEL:Alser12345' \
  'https://172.19.160.1/obix/config/Drivers/CODIGOS/Amp$20Fan/'
```

Estructura relevante del `BNumericWritable`:

```xml
<real is="/obix/def/control:NumericWritable /obix/def/control:NumericPoint obix:Point" ...>
  <str  name="facets"            writable="true"/>
  <real name="out"               />                         <!-- read-only -->
  <real name="in1".."in16"       />                         <!-- read-only -->
  <real name="fallback"          />                         <!-- read-only -->
  <abstime name="overrideExpiration" />

  <op name="emergencyOverride" in="obix:real"/>             <!-- priority 1 -->
  <op name="emergencyAuto"/>
  <op name="override"          in="control:NumericOverride"/>
  <op name="auto"/>
  <op name="set"               in="obix:real"/>             <!-- priority default (16) -->

  <str name="wsAnnotation"     writable="true"/>
</real>
```

### Modelo de prioridades NumericWritable → operaciones oBIX

| Acción Workbench | oBIX op | Priority efectiva |
|------------------|---------|-------------------|
| Right-click → Set | POST `set/` body=`<real val="X"/>` | 16 (default) |
| Right-click → Override | POST `override/` body=`<obj is="control:NumericOverride">` | 8 |
| Right-click → Auto | POST `auto/` (sin body) | release override |
| Emergency Override | POST `emergencyOverride/` body=`<real val="X"/>` | 1 (highest) |
| Emergency Auto | POST `emergencyAuto/` | release emergency |

---

## Test 4 — WRITE correcto: `POST set/` ✅

```bash
curl -sk -u 'TUNEL:Alser12345' -X POST \
  -H 'Content-Type: text/xml' \
  --data '<real val="42.0" xmlns="http://obix.org/ns/schema/1.0"/>' \
  'https://172.19.160.1/obix/config/Drivers/CODIGOS/Amp$20Fan/set/'
```

Re-read del `out/`:

```xml
<real val="42.0"
      display="42,0 {ok} @ def"
      ...
      is="/obix/def/baja:StatusNumeric"/>
```

**FULL-DUPLEX CONFIRMADO**.
- `val` cambió de `23.0` → `42.0`
- `display` ahora muestra `@ def` indicando priority 16 (default = donde escribe `set`)
- Ningún `<err>` en la respuesta del op invocation
- Cambio inmediato (no requiere poll), confirma que la escritura es síncrona

---

## Conclusiones operativas

1. **oBIX NO escribe via PUT al slot que se ve en el árbol.** Solo PUT a slots con `writable="true"` explícito. En `BNumericWritable` esos son `facets` y `wsAnnotation`, nada más.

2. **Para escribir el valor operacional**, se invoca una `op` con `POST + XML body`. La op a usar depende de la prioridad deseada (`set`, `override`, `emergencyOverride`).

3. **Los slots `fallback` y `inN` NO son escribibles vía oBIX.** `fallback` es config — modificable solo por Workbench / BatchEditor / Fox. Los `inN` son inputs operacionales que el Niagara llena internamente; para "forzar" un input se usa la op correspondiente.

4. **HTTP Basic Auth funciona** SI el usuario tiene `BLegacyBasicAuthenticationScheme` (no el `BDigest...` default). Si fuera Digest, `curl` requeriría `--digest`.

5. **`Cannot translate`** en una respuesta `200 OK` significa: el verbo HTTP no tiene handler en ese path, NO un problema de XML/schema. Es la forma oBIX de decir "method not allowed" pero a nivel de aplicación.

---

## Cheat sheet final

```bash
HOST='https://172.19.160.1'
AUTH='TUNEL:Alser12345'
POINT='/obix/config/Drivers/CODIGOS/Amp$20Fan'

# READ valor actual
curl -sk -u "$AUTH" "$HOST$POINT/out/"

# WRITE priority default (16)
curl -sk -u "$AUTH" -X POST -H 'Content-Type: text/xml' \
  --data '<real val="42.0" xmlns="http://obix.org/ns/schema/1.0"/>' \
  "$HOST$POINT/set/"

# OVERRIDE priority 8 con duración
curl -sk -u "$AUTH" -X POST -H 'Content-Type: text/xml' \
  --data '<obj is="/obix/def/control:NumericOverride" xmlns="http://obix.org/ns/schema/1.0">
            <real name="value"    val="55.0"/>
            <reltime name="duration" val="PT5M"/>
          </obj>' \
  "$HOST$POINT/override/"

# RELEASE override (vuelve a auto)
curl -sk -u "$AUTH" -X POST "$HOST$POINT/auto/"

# EMERGENCY (priority 1 — máxima)
curl -sk -u "$AUTH" -X POST -H 'Content-Type: text/xml' \
  --data '<real val="99.0" xmlns="http://obix.org/ns/schema/1.0"/>' \
  "$HOST$POINT/emergencyOverride/"
```

---

## Cross-references

- `niagara-mental-model-bloque7.md` §7.3 — descripción inicial del oBIX (interfaces `BIObixWritable`, `BIObixInvocable`)
- engram topic_key: `niagara/drivers/obix-write-protocol`
