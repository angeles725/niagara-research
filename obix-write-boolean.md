# oBIX Write Protocol — BooleanWritable (Comprobación Empírica)

**Fecha**: 2026-04-29
**Contexto**: Validación empírica del protocolo `POST /set/` para `BBooleanWritable` en una station Niagara N4. Complementa `obix-write-protocol-empirico.md` (que cubrió `BNumericWritable`).

---

## Setup

| Campo | Valor |
|-------|-------|
| Station host | `https://172.19.160.1` (Windows host accedido desde WSL2) |
| Cliente | `curl` |
| Auth | HTTP Basic (`-u TUNEL:Alser12345`) — usuario asignado a `BLegacyBasicAuthenticationScheme` |
| TLS | Self-signed → `-k` para ignorar verificación |
| Punto de prueba | `config/Drivers/CODIGOS/SW ALTA 1` (BBooleanWritable) |
| Path oBIX escapado | `/obix/config/Drivers/CODIGOS/SW$20ALTA$201/` (`$20` = space) |

---

## Test 1 — Inspección del padre ✅ (clave didáctica)

```bash
curl -sk -u 'TUNEL:Alser12345' \
  'https://172.19.160.1/obix/config/Drivers/CODIGOS/SW$20ALTA$201/'
```

Estructura relevante del `BBooleanWritable`:

```xml
<bool val="true"
      is="/obix/def/control:BooleanWritable /obix/def/control:BooleanPoint obix:Point"
      display="true {ok} @ def">
  <str  name="facets"            val="trueText=s:true|falseText=s:false" writable="true"/>
  <bool name="out"               val="true" />                    <!-- read-only -->
  <bool name="in1".."in16"       val="false" />                   <!-- read-only -->
  <bool name="fallback"          val="true" />                    <!-- read-only -->
  <abstime name="overrideExpiration" />
  <reltime name="minActiveTime"      writable="true"/>
  <reltime name="minInactiveTime"    writable="true"/>
  <bool name="setMinInactiveTimeOnStart" writable="true"/>

  <op name="emergencyActive"   />                                 <!-- priority 1, set to true -->
  <op name="emergencyInactive" />                                 <!-- priority 1, set to false -->
  <op name="emergencyAuto"     />
  <op name="active"            in="control:Override"/>            <!-- priority 8, set to true with duration -->
  <op name="inactive"          in="control:Override"/>            <!-- priority 8, set to false with duration -->
  <op name="auto"              />
  <op name="set"               in="obix:bool"/>                   <!-- priority 16 (default) -->

  <str name="wsAnnotation" writable="true"/>
</bool>
```

### Modelo de prioridades BooleanWritable → operaciones oBIX

| Acción Workbench | oBIX op | Body | Priority efectiva |
|------------------|---------|------|-------------------|
| Right-click → Set | POST `set/` body=`<bool val="true|false"/>` | `obix:bool` | 16 (default) |
| Right-click → Active (override) | POST `active/` body=`<obj is="control:Override">` | con duración | 8 |
| Right-click → Inactive (override) | POST `inactive/` body=`<obj is="control:Override">` | con duración | 8 |
| Right-click → Auto | POST `auto/` (sin body) | — | release override |
| Emergency Active | POST `emergencyActive/` (sin body) | — | 1 (highest) |
| Emergency Inactive | POST `emergencyInactive/` (sin body) | — | 1 (highest) |
| Emergency Auto | POST `emergencyAuto/` | — | release emergency |

---

## Test 2 — WRITE idempotente: `POST set/ val=true` (cuando ya era true) ✅

Test no destructivo — escribir el mismo valor que ya tiene confirma el protocolo sin alterar nada.

```bash
curl -sk -u 'TUNEL:Alser12345' -X POST \
  -H 'Content-Type: text/xml' \
  --data '<bool val="true" xmlns="http://obix.org/ns/schema/1.0"/>' \
  'https://172.19.160.1/obix/config/Drivers/CODIGOS/SW$20ALTA$201/set/'
```

Response (HTTP 200, sin `<err>`):

```xml
<bool val="true"
      is="/obix/def/control:BooleanWritable /obix/def/control:BooleanPoint obix:Point"
      display="true {ok} @ def"
      ...>
```

Re-read confirma estado intacto. **El protocolo funciona** — Niagara aceptó el body sin generar error.

---

## Test 3 — WRITE destructivo: cycle `true → false → true` ✅

Confirmador definitivo de que el valor REAL cambia.

```bash
URL='https://172.19.160.1/obix/config/Drivers/CODIGOS/SW$20ALTA$201'
AUTH='TUNEL:Alser12345'

# Estado inicial
curl -sk -u "$AUTH" "$URL/out/" | grep -oE 'val="[^"]*"|display="[^"]*"'
# → val="true" | display="true {ok} @ def"

# Step 1: WRITE false
curl -sk -u "$AUTH" -X POST -H 'Content-Type: text/xml' \
  --data '<bool val="false" xmlns="http://obix.org/ns/schema/1.0"/>' \
  "$URL/set/"
# → HTTP 200, body confirma val="false"

# Step 2: Re-read
curl -sk -u "$AUTH" "$URL/out/"
# → val="false" | display="false {ok} @ def"

# Step 3: RESTORE WRITE true
curl -sk -u "$AUTH" -X POST -H 'Content-Type: text/xml' \
  --data '<bool val="true" xmlns="http://obix.org/ns/schema/1.0"/>' \
  "$URL/set/"
# → HTTP 200, body confirma val="true"

# Step 4: Re-read final
curl -sk -u "$AUTH" "$URL/out/"
# → val="true" | display="true {ok} @ def"
```

**FULL-DUPLEX BOOLEAN CONFIRMADO**:
- `val` cambió `true` → `false` → `true`
- Cambio visible inmediatamente en re-read (síncrono)
- `display @ def` en ambos casos confirma priority 16 (default), no override
- Restore exitoso, sistema vuelve al estado inicial

---

## Diferencias críticas vs `BNumericWritable`

El doc original (`obix-write-protocol-empirico.md`) probó contra `BNumericWritable`. Si bien el `set/` funciona igual, las **otras ops difieren significativamente**:

| Op | NumericWritable | BooleanWritable |
|---|---|---|
| `set` | sí, in: `obix:real` | sí, in: `obix:bool` ✅ misma forma |
| Override con valor | UNA op `override` con body `<obj is="control:NumericOverride">` que incluye `value` | DOS ops separadas: `active/` (true) e `inactive/` (false), body `<obj is="control:Override">` SIN value (el target value está implícito en el nombre de la op) |
| Auto release | `auto/` (sin body) | `auto/` (sin body) — igual |
| Emergency con valor | UNA op `emergencyOverride` con body `<real val="X"/>` | DOS ops: `emergencyActive/` y `emergencyInactive/` (sin body, target value implícito) |

**Implicación práctica**: si un Worker o cliente quiere implementar "override por X minutos" para BooleanWritable, NO basta el `pointTypeMap` lineal del modelo Numeric — necesita lógica condicional `if value === true → active else inactive`.

---

## Conclusiones operativas

1. **`POST /set/` con `<bool val="..."/>` funciona igual que con `<real val="..."/>`** — el modelo del doc Numeric se aplica al caso simple Boolean. Mismo header `Content-Type: text/xml`, misma forma de escapar el path Niagara, misma respuesta XML.

2. **El cambio es síncrono** — el body del POST response YA refleja el nuevo valor, no hace falta esperar/poll. El re-read inmediato confirma. Latencia round-trip ~50-100ms en LAN local.

3. **`display @ def`** en la respuesta confirma que `set/` escribe en priority 16 (default). NO genera override automático. Para overrides intencionales, usar las ops específicas (`active`/`inactive`/`emergencyActive`/`emergencyInactive`).

4. **Para el caso oBIX boolean override CON DURACIÓN** (priority 8), el modelo es:
   ```bash
   # Override a TRUE por 5 minutos
   curl -sk -u "$AUTH" -X POST -H 'Content-Type: text/xml' \
     --data '<obj is="/obix/def/control:Override" xmlns="http://obix.org/ns/schema/1.0">
               <reltime name="duration" val="PT5M"/>
             </obj>' \
     "$URL/active/"

   # Override a FALSE por 5 minutos
   curl -sk -u "$AUTH" -X POST -H 'Content-Type: text/xml' \
     --data '<obj is="/obix/def/control:Override" xmlns="http://obix.org/ns/schema/1.0">
               <reltime name="duration" val="PT5M"/>
             </obj>' \
     "$URL/inactive/"
   ```
   **NO probado empíricamente todavía** — si llega a hacer falta, validar antes.

---

## Cheat sheet final

```bash
HOST='https://172.19.160.1'
AUTH='TUNEL:Alser12345'
POINT='/obix/config/Drivers/CODIGOS/SW$20ALTA$201'

# READ valor actual
curl -sk -u "$AUTH" "$HOST$POINT/out/"

# WRITE priority default (16) — TRUE
curl -sk -u "$AUTH" -X POST -H 'Content-Type: text/xml' \
  --data '<bool val="true" xmlns="http://obix.org/ns/schema/1.0"/>' \
  "$HOST$POINT/set/"

# WRITE priority default (16) — FALSE
curl -sk -u "$AUTH" -X POST -H 'Content-Type: text/xml' \
  --data '<bool val="false" xmlns="http://obix.org/ns/schema/1.0"/>' \
  "$HOST$POINT/set/"

# OVERRIDE priority 8 — Active por 5 min (NO probado empíricamente)
curl -sk -u "$AUTH" -X POST -H 'Content-Type: text/xml' \
  --data '<obj is="/obix/def/control:Override" xmlns="http://obix.org/ns/schema/1.0">
            <reltime name="duration" val="PT5M"/>
          </obj>' \
  "$HOST$POINT/active/"

# OVERRIDE priority 8 — Inactive por 5 min
curl -sk -u "$AUTH" -X POST -H 'Content-Type: text/xml' \
  --data '<obj is="/obix/def/control:Override" xmlns="http://obix.org/ns/schema/1.0">
            <reltime name="duration" val="PT5M"/>
          </obj>' \
  "$HOST$POINT/inactive/"

# RELEASE override (vuelve a auto)
curl -sk -u "$AUTH" -X POST "$HOST$POINT/auto/"

# EMERGENCY priority 1 (sin body, solo POST)
curl -sk -u "$AUTH" -X POST "$HOST$POINT/emergencyActive/"
curl -sk -u "$AUTH" -X POST "$HOST$POINT/emergencyInactive/"
curl -sk -u "$AUTH" -X POST "$HOST$POINT/emergencyAuto/"
```

---

## Cross-references

- `obix-write-protocol-empirico.md` — versión Numeric del mismo protocolo (probado contra `Drivers/CODIGOS/Amp Fan`)
- `niagara-mental-model-bloque7.md` §7.3 — descripción inicial del oBIX (interfaces `BIObixWritable`, `BIObixInvocable`)
- engram topic_key: `niagara/drivers/obix-write-protocol-boolean`
- SDD relacionado: `casino/sdd/manual-auto-write` (proyecto niagara-casino, write path Layer 5)
- Caso de uso productivo: `OPTIMIZER/fanControl` en station Hollywood Casino (a implementar en sdd-apply Phase 2)
