# toolshost — Herramientas creadas en la sesión de pentest (minipc 192.168.0.50 · iC-Niagara-4.10.9.14)

> Todas las herramientas, scripts y utilidades generadas durante el pentest autorizado del pipeline
> (instalación SSH + licenciamiento Niagara). Cada archivo indica **para qué es** y **cómo se usa**.
> Evidencia completa de los resultados en `sources/probes/B317-pentest-2026-08-01/` y bloques
> `niagara-mental-model-bloque316..323.md` del corpus.
>
> ⚠ **Seguridad**: ningún archivo aquí contiene claves privadas ni secretos del objetivo. Las claves DSA
> de prueba (del atacante simulado) quedan solo en `sources/probes/B317-pentest-2026-08-01/forge/` como
> evidencia. La clave privada del vendor **nunca** se guarda en esta carpeta — se pasa por path al tool.

---

## 1. `license-tool/` — Tooling de licencias del OEM (Python)

| Archivo | Para qué es | Cómo se usa |
|---|---|---|
| `niagara-license-tool.py` | **El signer/verificador de licencias Niagara N4** byte-compatible con la plataforma (validado contra la `Honeywell.license` real y en vivo contra `nre.exe`). Subcomandos: `verify` (verifica firma offline contra el certificado del vendor), `sign` (re-firma una licencia), `rehost` (cambia `hostId` + re-firma — el flujo "pasar tu licencia a otra máquina" hecho legítimo), `gen` (genera una licencia desde cero). Reglas incorporadas: re-encoding canónico de `LicenseUtil.encode` (whitespace descartado, self-closing expandido) y formato DER `30 2c 02 14 [r:20] 02 14 [s:20]` con r,s bit159=0 (requisito de Sun JDK8 descubierto en vivo). | `python3 niagara-license-tool.py verify lic.xml cert.xml` · `rehost lic.xml Win-4D6F-169B-CEF1-8F57 vendor_key.pem` · `gen out.license Honeywell <hostid> 2027-12-31 "feat1,point.limit=500;feat2" vendor_key.pem` · Dependencias: python3 + cryptography + lxml + openssl |

## 2. `ghidra-scripts/` — Scripts de decompilación Ghidra headless (Java)

| Archivo | Para qué es | Cómo se usa |
|---|---|---|
| `DecompileLicense.java` | Decompila las funciones de licencia de un binario (needles: `isFeaturePresent`, `getHostId`, `getHostId0`, `checkFileSignature`, `parseDSASignature`) y escribe `decomp-out.txt` | `analyzeHeadless <proj> <name> -process <dll> -noanalysis -scriptPath <dir> -postScript DecompileLicense.java` |
| `DecompileCallers.java` | Decompila los llamadores de `isFeaturePresent` (`createVM`, `initFips`) → muestra cómo se gatea `-javaagent` con la feature `developer` | ídem (postScript `DecompileCallers.java`, salida `decomp-callers.txt`) |
| `DecompileHostId.java` | Decompila la derivación del HostId (`getHostId`, `getOrCreateHiddenKey`, `getOrCreateCachedProductIdKey`, `getVolume`, `getRegWinCurVerImpl`) → los 4 inputs del HostId | ídem (salida `decomp-hostid.txt`) |
| `DecompileDsfspi.java` | Decompila las clases criptográficas de `dsfspi.dll` (DSA/RSA signature, keypair generator, `parseDSASignature`, `parseDERInteger`) | ídem (salida `decomp-dsfspi.txt`) |
| `DecompileCheckFile.java` | Decompila `DsfUtil::checkFileSignature` (bounds defensivos de verificación de módulos) | ídem (salida `decomp-checkfile.txt`) |

Nota de entorno (Ghidra headless sin TTY): `export JAVA_HOME=/home/linuxbrew/.linuxbrew/opt/openjdk@21` +
`export JAVA_TOOL_OPTIONS="-Duser.home=<workdir>/ghidra-home -Djava.io.tmpdir=<workdir>/tmp"` con
`ghidra_12.1.2_PUBLIC` pre-creado en `ghidra-home/.config/ghidra/`.

## 3. `forge/` — Generadores de artefactos de evasión (ataque simulado)

| Archivo | Para qué es | Cómo se usa |
|---|---|---|
| `forge-keys.sh` | Genera los keypairs DSA del atacante: **q=224** (openssl 3.x default — formato que la plataforma rechaza con `error decoding signature bytes`) y **q=160** (compatible con la plataforma) | `bash forge-keys.sh` (genera `attacker_dsa.pem`, `attacker_dsa160.pem`, `attacker_dsa_pub.der`) |
| `make-forge160.py` | Falsifica un certificado de vendor + licencia auto-firmados con la clave DSA-160 del atacante (con el typo histórico `algorthm`). Sirvió para el test L-4: la plataforma los rechaza con `{invalid: Invalid signature}` porque la firma del certificado debe verificar contra la clave raíz embebida en `baja.jar` | `python3 make-forge160.py` (genera `PentestVendor160.certificate` + `.license`) |
| `make-probes.py` | Genera 6 licencias-sonda que rompen UN check cada una (`vendor="Tridium"`): hostId ajeno, `generated` futuro, `expiration` pasada, sin firma, firma basura, firmada por atacante → para ver en vivo el orden de los 5 checks | `python3 make-probes.py` |
| `make-probes36h.py` | Genera la sonda del límite de la gracia de 36 h del check `generated` (anti-reloj-atrasado con tolerancia `MILLIS_IN_36_HOURS`) | `python3 make-probes36h.py` |
| `make-javaagent-lic.py` | Genera la licencia falsa con la feature literal `developer` (sin firma) que **evade el gate nativo de `-javaagent`** (test L-11) | `python3 make-javaagent-lic.py` |

## 4. `protocol-ps1/` — Protocolo deploy → oracle → restore (PowerShell)

Todos siguen el runbook de reversibilidad (B318): backup → plant → oracle (`nre -licenses`) → restore
byte-idéntico → verificación en pase separado. Se ejecutan en la minipc vía SSH con
`powershell -NoProfile -ExecutionPolicy Bypass -EncodedCommand <base64>` (UTF-16LE).

| Archivo | Para qué es |
|---|---|
| `run-test.ps1` | Planta el certificado+licencia falsificados (L-1/L-2), corre el oracle, restaura y limpia `db\` |
| `run-probes.ps1` | Planta la sonda `vendor="iSMA CONTROLLI"` sin firma (L-1: `No certificate for vendor`) |
| `run-probes2.ps1` | Planta las 6 sondas etapa-por-etapa con `vendor="Tridium"` (L-3) |
| `run-probes3.ps1` / `run-probes4.ps1` | Pruebas de formato DER (DSA-224 vs padded) (L-3/L-4) |
| `run-probes5.ps1` / `run-probes6.ps1` | Pruebas de la gracia de 36 h (primera ronda, L-8) |
| `run-probes7.ps1` | Prueba definitiva del límite de 36 h (`generated=2026-08-04` → rechazo) (L-8) |
| `verify-clean.ps1` | Verifica la restauración: `certificates\` = solo `Tridium.certificate`, `licenses\` = `db`+`inbox`, sha256 de `Tridium.certificate` |
| `final-clean.ps1` | Limpieza final de la canonicalización en `db\<hostId>\` + chequeo de árbol pristino |
| `clean-residue.ps1` | Elimina copias residuales que el license manager escribió en `db\` (residuos invisibles a un `Remove-Item` naíf de la raíz) |

## 5. `hostid-tests/` — Tests de re-uso de licencia / hostId (L-12 + validación del tool)

| Archivo | Para qué es |
|---|---|
| `run-hostid-test.ps1` | Test L-12: planta la `Honeywell.license` ORIGINAL (T-A: se mueve a `db\<hostId-ajeno>\`) y la EDITED con hostId cambiado (T-B: `{invalid: Invalid signature}`) |
| `run-real-sig.ps1` | Planta la licencia real con hostId editado pero firma REAL intacta → `{invalid: Invalid signature}` (aisla: el problema es el body, no el DER) |
| `run-tool-check.ps1` / `run-tool-check2.ps1` | Validan las firmas del tool contra la plataforma (versión pre-fix con decode error y post-fix) |
| `run-clean.ps1` | Prueba la firma "limpia" (r,s bit159=0) → la plataforma llega al check criptográfico (`Invalid signature`) |
| `run-variants.ps1` / `run-variants2.ps1` / `run-variants3.ps1` | Matriz de formatos DER (minimal / fixed20 / pad21) contra la plataforma → descubrimiento del requisito bit159=0 |
| `run-gen.ps1` / `run-gen2.ps1` | Validan en vivo la licencia `gen`-erada desde cero con el tool (aceptada hasta la criptografía) |
| `post-clean.ps1` / `final-clean.ps1` | Limpieza y verificación pristina tras los tests |

## 6. `session-scripts/` — Scripts de sesión (recon / gates / limpieza)

| Archivo | Para qué es |
|---|---|
| `jre-tools-check.ps1` | Inventario del JRE embebido (¿`jcmd`/`jstack`/`keytool`?) + procesos java + conectividad internet de la minipc (sin gateway → sin internet) |
| `javaagent-gate-test.ps1` / `javaagent-gate2.ps1` / `javaagent-gate3.ps1` | Tests del gate `-javaagent` de `createVM` (L-11): línea base con FATAL, plantado de la licencia falsa `developer`, restore |
| `post-ja-verify.ps1` | Verificación de estado pristino tras los tests del javaagent (PIDs, sha256, árbol de licencias) |

---

## Flujo típico de uso

```bash
# 1. Generar claves de prueba (solo para tests de ataque simulado)
bash toolshost/forge/forge-keys.sh

# 2. Falsificar / sondear (tests de evasión)
python3 toolshost/forge/make-forge160.py        # certificado+licencia falsos
python3 toolshost/forge/make-probes.py          # sondas de los 5 checks
python3 toolshost/forge/make-probes36h.py       # sonda de la gracia de 36 h
python3 toolshost/forge/make-javaagent-lic.py   # licencia para el bypass -javaagent

# 3. Plantar/probar/restaurar en la minipc (scp + EncodedCommand, ver protocol-ps1/)
#    scp <artefacto> ASUS@192.168.0.50:C:/Users/ASUS/pentest-staging/
#    powershell -NoProfile -ExecutionPolicy Bypass -EncodedCommand <b64 de run-*.ps1>

# 4. Tooling legítimo del OEM (control positivo)
python3 toolshost/license-tool/niagara-license-tool.py \
    rehost Honeywell.license Win-4D6F-169B-CEF1-8F57 vendor_dsa_private.pem
python3 toolshost/license-tool/niagara-license-tool.py \
    verify Honeywell.license Honeywell.certificate
# -> dejar el .license en security\licenses\inbox\ de la minipc (import legítimo)

# 5. Decompilación (Ghidra headless, ver ghidra-scripts/)
#    export JAVA_HOME=/home/linuxbrew/.linuxbrew/opt/openjdk@21
#    export JAVA_TOOL_OPTIONS="-Duser.home=<workdir>/ghidra-home -Djava.io.tmpdir=<workdir>/tmp"
#    analyzeHeadless <proj> <name> -process nre.dll -noanalysis \
#        -scriptPath toolshost/ghidra-scripts -postScript DecompileLicense.java
```

## Referencias del corpus

- B316 — veredictos del pentest (I-1..I-8, L-1..L-12) · B317 — kit de evasión · B318 — runbook de
  reversibilidad · B319 — capa nativa text-match + bypass `-javaagent` · B320 — HostId (4 inputs) ·
  B321 — `dsfspi.dll` · B322 — capa Java `baja.jar` (delta single-root) · B323 — este tooling (validación
  en vivo).
