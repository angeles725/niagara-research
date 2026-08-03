#!/usr/bin/env bash
# build-probe-module.sh — Build reproducible del módulo probe de L-18 (pentest autorizado).
#
# Construye pentestProbe-2.0.0.jar a partir de probe-module-v2/ y lo firma con la clave
# self-signed del pentest (pentest-keystore.jks, clave de PRUEBA, no secreto del objetivo).
#
# Uso:
#   bash build-probe-module.sh [salida.jar]
#   (por defecto escribe pentestProbe-v2-selfsigned.jar)
#
# Requiere: JDK 21 (javac/jar/jarsigner), el baja.jar del install para compilar la clase.
# La clase ProbePayload es un BSimple válido (TYPE via Sys.loadType) cuyo static-init
# demuestra ejecución de código; el test L-18 probó que el registro pasa pero la CARGA
# de la clase es bloqueada por verifyModuleSignature (modo efectivo noPreference).
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
BAJA_JAR="${BAJA_JAR:-/home/cristian/niagara-research/sources/probes/B317-pentest-2026-08-01/native/jars/baja.jar}"
KEYSTORE="${KEYSTORE:-$HERE/../../sources/probes/B317-pentest-2026-08-01/native/pentest-keystore.jks}"
STORE_PASS="${STORE_PASS:-pentest123}"
OUT="${1:-$HERE/pentestProbe-v2-selfsigned.jar}"

BUILD="$(mktemp -d)"
trap 'rm -rf "$BUILD"' EXIT

# 1. Compilar la clase contra baja.jar
mkdir -p "$BUILD/classes"
javac -cp "$BAJA_JAR" -d "$BUILD/classes" "$HERE/probe-module-v2/com/pentest/ProbePayload.java"

# 2. Armar el jar (module.xml + clases)
mkdir -p "$BUILD/jar/META-INF"
cp "$HERE/probe-module-v2/META-INF/module.xml" "$BUILD/jar/META-INF/"
cp -r "$BUILD/classes/com" "$BUILD/jar/"
( cd "$BUILD/jar" && jar cf "$BUILD/unsigned.jar" . )

# 3. Firmar con la clave self-signed del pentest
jarsigner -keystore "$KEYSTORE" -storepass "$STORE_PASS" "$BUILD/unsigned.jar" pentest >/dev/null 2>&1
cp "$BUILD/unsigned.jar" "$OUT"

echo "OK: $OUT"
echo "Verificar firma: jarsigner -verify $OUT"
