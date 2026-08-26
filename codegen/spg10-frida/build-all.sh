#!/usr/bin/env bash
# build-all.sh — recompile the three mirror `-javaagent` JARs in bin/ from source.
#
# Requires a Niagara install whose jre/bin/javac.exe and bin/ext/asm-9.6.jar exist.
# Usage: ./build-all.sh [NIAGARA_HOME]
#   NIAGARA_HOME defaults to /mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162
#
# Output: bin/LicenseMirrorAgent.jar, bin/full-mirror-agent.jar, bin/ProviderOrderProbe.jar
# (writes inside this codegen dir only; never touches the install).
set -euo pipefail

cd "$(dirname "$0")"
HERE="$(pwd)"
NH="${1:-/mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162}"
ASM="$NH/bin/ext/asm-9.6.jar"
JAVAC="$NH/jre/bin/javac.exe"

[ -f "$ASM" ]  || { echo "ASM not found: $ASM" >&2; exit 2; }
[ -f "$JAVAC" ] || { echo "javac not found: $JAVAC" >&2; exit 2; }

rm -rf build && mkdir -p build/classes bin
"$JAVAC" -cp "C:\\Honeywell\\OptimizerSupervisor-N4.14.0.162\\bin\\ext\\asm-9.6.jar" \
  -d build/classes \
  javaagent/LicenseMirrorAgent.java javaagent/FullMirrorAgent.java javaagent/ProviderOrderProbe.java

pkg() { # pkg <PremainClass> <outjar>
  local premain="$1" outjar="$2"
  rm -rf build/pkg && mkdir -p build/pkg/META-INF
  cp -r build/classes/spg10 build/pkg/
  printf 'Premain-Class: %s\nCan-Retransform-Classes: true\n\n' "$premain" > build/pkg/META-INF/MANIFEST.MF
  (cd build/pkg && zip -qr "$HERE/$outjar" META-INF spg10)
  echo "built $outjar ($premain)"
}

pkg spg10.LicenseMirrorAgent bin/LicenseMirrorAgent.jar
pkg spg10.FullMirrorAgent    bin/full-mirror-agent.jar
# ProviderOrderProbe has no retransform (observer only)
rm -rf build/pkg && mkdir -p build/pkg/META-INF && cp -r build/classes/spg10 build/pkg/
printf 'Premain-Class: spg10.ProviderOrderProbe\n\n' > build/pkg/META-INF/MANIFEST.MF
(cd build/pkg && zip -qr "$HERE/bin/ProviderOrderProbe.jar" META-INF spg10)
echo "built bin/ProviderOrderProbe.jar (spg10.ProviderOrderProbe)"

echo "done."
