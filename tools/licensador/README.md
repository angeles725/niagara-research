# licensador — REF-License (implementación de referencia)

Sistema de **licenciamiento firmado propio**: la máquina solo conoce la clave pública
(raíz embebida), las licencias son artefactos `REF1.<payload>.<firma>` RSA-PSS-SHA256
con binding de máquina (`REF-…`), expiración, features con cantidad y blacklist.

**Diseño completo (esquema, amenazas, pipeline, modos de fallo, port Java)**:
[`analizador-licencias/06-diseno-sistema-licenciamiento-propio.md`](../../analizador-licencias/06-diseno-sistema-licenciamiento-propio.md)

## Requisitos

- Python 3.9+ y `cryptography` (`pip install cryptography`)

## Uso rápido

```bash
# 1. Ceremonia de claves (una sola vez; la PRIVADA nunca sale de la maquina de emision)
python3 licensador.py genkeys --out-dir keys

# 2. Emitir una licencia para una maquina (HostId = salida de `hostid` en esa maquina)
HOST=$(python3 licensador.py hostid)
python3 licensador.py issue --key keys/root-private.pem --product reflow-oven \
    --host "$HOST" --licensee "Cliente" --feature pid:4 --feature logging \
    --expires 2027-08-02T00:00:00+00:00 --out lic.refl

# 3. Verificar en el dispositivo (raiz publica embebida + archivo de estado anti-reloj)
python3 licensador.py verify --root keys/root-public.pem --file lic.refl \
    --product reflow-oven --state state.json
# -> RESULTADO: OK | E_SIGNATURE | E_HOST | E_TIME_EXPIRED | ...

# 4. Revocar un serial (blacklist firmada por la misma raiz)
python3 licensador.py revoke --key keys/root-private.pem --serial <SERIAL> --out blacklist.refl
python3 licensador.py verify --root keys/root-public.pem --file lic.refl \
    --product reflow-oven --blacklist blacklist.refl
```

## Subcomandos

| Comando | Función |
|---|---|
| `genkeys` | genera `root-private.pem` + `root-public.pem` (RSA-3072) |
| `issue` | emite una licencia `.refl` (features `NAME` o `NAME:QTY`) |
| `verify` | corre el pipeline completo (exit 0 = OK, 1 = inválido, 2 = error de uso) |
| `inspect` | muestra los claims sin verificar |
| `hostid` | calcula el `REF-…` de la máquina actual |
| `revoke` | firma una blacklist de seriales |
| `selftest` | matriz de aceptación A1–A10 (13 pruebas) |

## Matriz de aceptación

`python3 licensador.py selftest --tmp /tmp/refl-selftest` — verifica: licencia válida,
host ajeno (`E_HOST`), tamper de 1 byte (`E_SIGNATURE`), expirada (`E_TIME_EXPIRED`),
feature faltante (`E_FEATURE_LOCKED`), qty insuficiente/justa/ilimitada, revocada
(`E_REVOKED`), rollback de reloj (`E_CLOCK`), producto ajeno (`E_PRODUCT`), corrupta
(`E_PARSE`).

## Notas de producción

- Cifrar la clave privada (PKCS#8 `BestAvailableEncryption`) y seguir la ceremonia del
  diseño §7; la raíz pública se **embebe** en el verificador del producto.
- El `hostid` de Linux usa `machine-id` + MAC; el de Windows usa el serial de volumen
  + MAC. El port Java debe usar los mismos identificadores y el mismo `SALT` en todas
  las plataformas.
- La verificación de runtime (`checkFeature`) se hace contra las concesiones resueltas
  en el arranque; ver diseño §4.
