# Bloque 26 — NRE launcher C++ + DLLs nativas + standalone module signing ops

Fecha: 2026-04-23
Fuentes empíricas: análisis `bin/*.exe`, `bin/*.dll`, `bin/ext/*.jar`, `bin/install-data/`, `bin/policy/`, `jre/bin/*.dll`, `JxBrowser/7.39.0/*.dll`, `printout/*.dll`, `security/`, `defaults/nre.properties`, `system.properties`, `modules/*.jar` (FFmpeg embedded), `etc/gradle/*.gradle`, playbooks operacionales de gradle signing plugin.

Cubre la **capa nativa + operaciones de signing** que el Bloque 17 (filesystem) y Bloque 18 (module signing teoría) tocaron superficialmente:
1. NRE launcher C++ (nre.exe + njre.dll + nre.dll → JNI_CreateJavaVM)
2. 138 artefactos nativos (DLLs/SOs) + cadenas dependencia MSVC runtime
3. **Playbook operacional de firma standalone** (sin Workbench) — respuesta directa al pedido del usuario

---

## 26.1 NRE launcher architecture

### Dual-layer arquitectura

```
Thin native launcher (PE32+ ~50 KB)
  ↓ carga njre.dll (69 KB, PE32+ x64)
JavaLauncher::main() entry C++
  ↓ resuelve niagaraHome + niagaraUserHome
  ↓ carga nre.dll (115 KB, PE32+ x64)
JNI_CreateJavaVM()
  ↓
JVM Azul Zulu 1.8.0.412.20 (Win x64)
  ↓
Java framework layer (~150 MB JARs)
```

Total footprint: ~440 MB nativo (30-40% dist), ~1.2 GB total install.

### Inventario bin/ completo

| Ejecutable/DLL | Tamaño | Tipo | Propósito |
|---|---|---|---|
| **nre.exe** | 23 KB | PE32+ console x64 | Launcher genérico NRE |
| **station.exe** | 24 KB | PE32+ console x64 | Bootstrap BStation daemon |
| **wb.exe** / **wb_w.exe** | 108 KB | PE32+ console x64 | Workbench CLI / GUI |
| **niagarad.exe** | 23 KB | PE32+ console x64 | Windows service wrapper |
| **n4mig.exe** | 98 KB | PE32+ console x64 | Migration utility |
| **plat.exe** | 50 KB | PE32+ console x64 | Platform installer |
| **nverify.exe** | 517 KB | PE32+ console x64 | Signature verification CLI |
| **test.exe** | 50 KB | PE32+ console x64 | Test framework runner |
| **console.exe** | 96 KB | PE32+ console x64 | Interactive CLI REPL |
| **hdbt.exe** | 35 KB | PE32+ console x64 | Historical data/BACnet tool |
| **dataExportTool.exe** | 75 MB | PE32+ console x64 | Bulk history export |
| **uninstall.exe** | 392 KB | PE32+ console x64 | MSI/NSIS uninstaller |
| **nre.dll** | 115 KB | PE32+ DLL | Core NRE JNI bridge, classpath |
| **njre.dll** | 69 KB | PE32+ DLL | JVM loader, memory management |
| **common.dll** | 189 KB | PE32+ DLL | Utilidades C++ comunes |
| **cppunit.dll** | 200 KB | PE32+ DLL | Unit testing C++ (internal tests) |
| **msvcp140.dll** | 549 KB | PE32+ DLL | MSVC C++ STL runtime |
| **msvcr120.dll** | 932 KB | PE32+ DLL | MSVC C runtime v12 legacy |
| **vcruntime140.dll** | 92 KB | PE32+ DLL | VC++ runtime v14 |
| **vcruntime140_1.dll** | 34 KB | PE32+ DLL | VC++ v14 Update 1 |
| **libciper.so** | 123 KB | ELF 32-bit ARM EABI5 | Crypto ARM JACE |
| **libciper.so.sig** | 256 B | RSA-2048 sig | Firma sidecar |
| **opc.dll** | 176 KB | PE32+ DLL | OPC DA/COM client |
| **opcproxy.dll** | 105 KB | PE32+ DLL | OPC proxy client |
| **opccomn_ps.dll** | 61 KB | PE32+ DLL | OPC common services |
| **lon.dll** | 35 KB | PE32+ DLL | LonTalk protocol |
| **dsfspi.dll** | 359 KB | PE32+ DLL | Distributed filesystem SPI |
| **honImport.dll** | 59 KB | PE32+ DLL | Honeywell import utilities |
| **alarmDialog.dll** | 24 KB | PE32+ DLL | GUI alarm dialog |
| **trayIcon.dll** | 194 KB | PE32+ DLL | System tray icon manager |
| **pcapBacEther.dll** | 27 KB | PE32+ DLL | BACnet/Ethernet packet capture (WinPcap) |

### Entry points per-executable

| Ejecutable | Clase Java principal |
|---|---|
| wb.exe / wb_w.exe | `com.tridium.workbench.BWorkbench` |
| station.exe | `com.tridium.niagarad.BStation` |
| nre.exe | (generic launcher) |
| niagarad.exe | `com.tridium.niagarad.NiagaraDaemon` |
| n4mig.exe | `com.tridium.platform.Migration` |
| plat.exe | `com.tridium.platform.Installer` |
| console.exe | `com.tridium.console.NiagaraConsole` |
| test.exe | `com.tridium.test.TestRunner` |

---

## 26.2 njre.dll + nre.dll flujo JVM boot

### njre.dll (69 KB) responsabilidades

1. **Resolver dinámicamente JVM**:
   - Busca `jvm.dll` en `%NIAGARA_HOME%\jre\bin\server\jvm.dll` (preferido)
   - Fallback `%NIAGARA_HOME%\jre\bin\client\jvm.dll`
2. **LoadLibrary() + GetProcAddress("JNI_CreateJavaVM")**
3. **Configuración memoria default**: `-Xms48M -Xmx48M` (conservador)
4. **Error handling**: `"Error: Cannot load: %s or %s"` si ambos paths fallan

```c++
typedef jint (JNICALL *CreateJavaVM_t)(JavaVM **pvm, void **penv, void *args);
CreateJavaVM_t CreateJavaVM =
    (CreateJavaVM_t)GetProcAddress(jvmDll, "JNI_CreateJavaVM");
```

Strings desde njre.dll:
```
JNI_CreateJavaVM
Error: Cannot find JNI functions in %s
Error: CreateJavaVM failed %d
```

### nre.dll (115 KB) responsabilidades

**Construye java.class.path agregando**:
- `bin/ext/nre.jar` — core runtime
- `bin/ext/*.jar` — Jetty, Kotlin, BouncyCastle FIPS, etc
- `bin/ext/bcfips/` — FIPS provider
- `bin/ext/bcstd/` — BouncyCastle standard

**Inyecta JVM flags**:
```
-Djava.protocol.handler.pkgs=com.tridium.nre.protocol
-Djava.library.path=%NIAGARA_HOME%\bin
-Djava.class.path=<CONSTRUCTED>
-Djava.security.properties==%NIAGARA_HOME%\bin\policy\java.security
-Djava.security.manager
-Dniagara.home=%NIAGARA_HOME%
-Dniagara.home.url=file:///%NIAGARA_HOME%
-Dniagara.user.home=%NIAGARA_USER_HOME%
-Dniagara.platform.provider=<DETECTED>
-Dniagara.supported.runtime.profiles=<LIST>
-Dniagara.required.runtime.profiles=<LIST>
```

**Lee `defaults/nre.properties`** por-ejecutable:
```properties
station.java.options=-Dfile.encoding=UTF-8 -Xss512K -Xmx1024M
wb.java.options=-Dfile.encoding=UTF-8 -Xss512K -Xmx1024M
test.java.options=-Dfile.encoding=UTF-8 -Xss512K -Xmx1024M
nre.java.options=<generic>
softjace=false
```

Invoca `JNI_CreateJavaVM` con `JavaVMInitArgs` preparado.

### Resolución niagara_home + niagara_user_home

Orden búsqueda (nre.dll):
1. Variable entorno `niagara_home` / `niagara_user_home`
2. Registro Windows `HKLM\Software\Honeywell\Niagara\<version>\niagaraHome`
3. Fallback relativo `../` desde bin/

### JVM options memoria

- Default memoria `njre.dll`: `-Xms48M -Xmx48M`
- `-Xss512K` (stack per thread)
- `-Xmx1024M` (recomendado station/wb via nre.properties)
- `-Dfile.encoding=UTF-8`

### Debugging launcher

```
set nre_debug=1
nre.exe
→ logs niagara home, user home, java options (prefix "nre>")

JDWP remoto:
-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005
```

---

## 26.3 system.properties (station runtime)

Ubicación: `/defaults/system.properties` + override en `${niagara.user.home}/etc/system.properties`.

```properties
niagara.steadystate=10000                     # ms wait post-start
niagara.ipv6Enabled=false                     # IPv4 preferred
niagara.fox.circuitMaxReceiveBuffer=10240000  # 10 MB
niagara.ui.volatileBackBuffer=false           # No DirectDraw
niagara.ui.pxCache.max=10                     # pixel cache

# moduleVerificationMode (Bloque 18)
niagara.moduleVerificationMode=low
```

### niagara.moduleVerificationMode

| Modo | Acepta | Rechaza | Requisito |
|---|---|---|---|
| `low` (N4.14 default) | unsigned, self-signed, CA-signed, expired | modificados post-firma | ninguno |
| `medium` (futuro N4.9+) | self-signed (si en User Trust Store), CA-signed | unsigned, expired, invalid sig | cert en User Trust Store |
| `high` (futuro) | solo CA-signed | self-signed, unsigned, expired | cadena CA válida |

---

## 26.4 bin/ext/ — extensiones classpath

### Core

- `nre.jar` — Bootstrap NRE (firmado)
- `niagarad.jar` — Platform daemon Java (firmado)
- `splash.jar` — Splash screen

### Cryptografía

Bouncy Castle FIPS certified (priority #1 en security.providers):
```
bcfips/bc-fips-1.0.2.5.jar          base FIPS (AES/SHA/RSA)
bcfips/bcpkix-fips-1.0.7.jar        PKIX (X.509, CRL)
bcfips/bctls-fips-1.0.19.jar        TLS 1.2/1.3 FIPS
bcfips/bc-bcfkswrapprov-1.0.0.jar   BCFKS keystore wrapper
bcstd/                              Standard BouncyCastle (no FIPS)
```

### Web stack

```
jetty-all-compact3-9.4.54.v20240208.jar   servlet container
javax.servlet-api-3.1.0.jar                API 3.1
okhttp-4.12.0.jar                          HTTP client Kotlin
httpclient-4.5.13.jar + httpcore-4.4.13    Apache HTTP
```

### Otras libs

```
slf4j-api-2.0.9.jar + slf4j-jdk14           logging facade
jffi-1.3.12.jar + jffi-1.3.12-native.jar    FFI
jnr-ffi-2.2.15.jar, jnr-posix-3.1.18.jar    JNR FFI bindings
kotlin-stdlib-1.9.10.jar
commons-{logging,codec}.jar
asm-9.6.jar                                 bytecode ASM
orientdb-3.2.23.jar                         embedded graph DB
jose4j-0.9.6.jar                             JWT/JWE
libthrift-0.17.0.jar                         Thrift RPC
encoder-1.2.3.jar                            HTML/XML encoding
```

### JxBrowser (embedded Chromium)

```
jxbrowser-7.39.0.jar                        core API
jxbrowser-win64-7.39.0.jar                  Win64 native
jxbrowser-swing-7.39.0.jar                  Swing binding
jxbrowser-swt, jxbrowser-javafx             alternate UIs
```

Todos con `.jar.sig` sidecars (SHA-256).

---

## 26.5 Inventario nativo 138 artefactos

### Distribución por directorio

| Directorio | Tamaño | Artefactos |
|---|---|---|
| `/jre/bin/` | ~110 MB | 110+ DLLs Azul Zulu JRE |
| `/JxBrowser/7.39.0/` | ~289 MB | Chromium Win64 |
| `/bin/` | ~2.8 MB | Tridium custom + protocol drivers |
| `/modules/*.jar` | ~40 MB+ | FFmpeg + XProtect embedded |
| `/printout/` | ~1 MB | Word/Office .NET interop |

### Arquitectura binaria

**PE32+ Windows (primario, 117 DLLs)**:
```
Magic 020b (PE32+)
Arch x86-64
ImageBase 0x0000000180000000
Subsistema Windows GUI/Console
```

**ELF ARM EABI5 (QNX/JACE, 1 SO)**:
```
file: ELF 32-bit LSB shared object, ARM, EABI5 v1 (SYSV),
      dynamically linked, with debug_info
libciper.so (123 KB, debug symbols RETAINED)
```

**PE32 x86 .NET (4 DLLs en /printout/)**:
- Word.dll (484 KB) — Word 2003 PIA
- Office.dll (152 KB)
- WireSheetControl.dll (344 KB) — Excel renderer
- Interop.Shell32.dll (48 KB) — Shell/COM API

### MSVC runtime multiplexing

```
Universal CRT (real impl):
 ucrtbase.dll (1.1 MB) — VS2015+

MSVC 14.0 (VC2015+):
 VCRUNTIME140.dll (84 KB JRE / 92 KB /bin)
 MSVCP140.dll (614 KB JRE / 549 KB /bin)
 VCRUNTIME140_1.dll (34 KB) — Update 1 fixes
 MSVCP140_1.dll (35 KB) — STL fixes
 MSVCP140_2.dll (261 KB) — C++17 features

MSVC 12.0 (VC2013, legacy):
 MSVCR120.dll (932 KB)

48× api-ms-win-crt-*.dll (21-73 KB each) — forwarders a ucrtbase
```

### Cadena dependencia típica

```
Custom DLL (e.g. opc.dll)
 ├ MSVCP140.dll (C++ STL)
 │   └ VCRUNTIME140.dll
 │       └ api-ms-win-crt-runtime-l1-1-0.dll
 │           └ ucrtbase.dll (real impl)
 └ ole32.dll, OLEAUT32.dll (Windows COM)
```

---

## 26.6 Protocol drivers nativos

### OPC-UA (OLE for Process Control)

- **opc.dll** (176 KB) — Cliente OPC DA/COM
  - Imports: ole32.dll, OLEAUT32.dll, MSVCP140.dll
  - JNI exports: `Java_com_honeywell_opc_*`
- **opccomn_ps.dll** (61 KB) — Proxy/stub marshaling IID/CLSID
- **opcproxy.dll** (105 KB) — RPC proxy (RPCRT4.dll)

### LON (Local Operating Network EIA 709.1)

- **lon.dll** (35 KB) — LON protocol binding (imports common.dll, nre.dll, MSVCP140, VCRUNTIME140)
- **common.dll** (189 KB) — Utilidades plataforma (ADVAPI32, NETAPI32, WS2_32, KERNEL32)

### BACnet

- **pcapBacEther.dll** (27 KB) — BACnet over Ethernet (WinPcap based)
  - Imports: packet.dll (kernel driver), wpcap.dll (capture API)
  - **CRÍTICO**: WinPcap deprecated 2018, CVEs conocidos. Considerar deprecar y usar IP-based.
- `bacnet-rt.jar` — Pure Java core (sin nativo)

### KNX (Konnex EN 50090)

- `knxnetIp-rt.jar` + `knxnetIp-wb.jar` — Pure Java (NO binarios nativos)
- Estrategia: solo librerías Java

---

## 26.7 libciper.so — JNI bindings ARM

```
Tamaño: 123 KB
Arch: ELF 32-bit ARM EABI5 v1 SYSV
Buildtype: Debug (símbolos NO removidos — unexpected en production)
Dependencias: libc.so.4 (QNX)

Exports (40+ métodos JNI):
 Java_com_honeywell_comm_JNIRequest_init
 Java_com_honeywell_comm_JNIRequest_jniFileOpen
 Java_com_honeywell_comm_JNIRequest_jniFileClose
 Java_com_honeywell_comm_JNIRequest_jniFileStatus
 Java_com_honeywell_comm_JNIRequest_jniBuildFileBlockRecord
 Java_com_honeywell_comm_JNIRequest_jniReadFromFile
 Java_com_honeywell_comm_JNIRequest_jniGetReceivedIOCommand
 (Pattern building, public variable iteration, SYLK file parsing)

Interfaz: CipherModule JNI bridge (acceso a sensores/datos JACE)
Uso: gateway/industrial controllers ARM (edge devices)
Compilación: cruzada desde build Windows → ARM target
```

---

## 26.8 FFmpeg codec stack (embedded ffmpeg-wb.jar)

Extraído en runtime a temp dir:

| DLL | Tamaño | Propósito | Ver |
|---|---|---|---|
| avcodec-60.dll | 18.4 MB | Codec (H.264, VP9, AAC, etc) | FFmpeg 4.x |
| avcodec (cont) | | | |
| avformat-60.dll | 4.0 MB | Container/demuxer (MP4, MKV, FLAC) | FFmpeg 4.x |
| avfilter-9.dll | 6.0 MB | Video/audio filters | FFmpeg 4.x |
| swscale-7.dll | 1.5 MB | Image scaling/colorspace | FFmpeg 4.x |
| swresample-4.dll | 817 KB | Audio resampling | FFmpeg 4.x |
| avdevice-60.dll | 928 KB | Device I/O (camera, mic) | FFmpeg 4.x |
| avutil-58.dll | 2.0 MB | Utilities | FFmpeg 4.x |
| ffmpeg-wrapper.dll | 64 KB | JNI bridge | Custom |

Total JAR ffmpeg-wb: 34.9 MB. **Security surface crítico**: FFmpeg histórico alto CVE rate (parser de codecs).

---

## 26.9 Graphics + UI rendering

### AWT/Swing (Zulu Tier 1)

- **awt.dll** (1.5 MB) — Windowing (Windows GDI/USER32)
- **jawt.dll** (21 KB) — JAWA bridge
- **freetype.dll** (638 KB) — TrueType font rasterization

### JavaFX (Tier 2)

- **glass.dll** (260 KB) — Window Toolkit + eventos
- **jfxwebkit.dll** (75 MB) — Embedded Chromium WebKit (CRÍTICO para HTML5 UI)

### Prism rendering backends

- **prism_d3d.dll** (134 KB) — Direct3D 11 GPU primario
- **prism_es2.dll** (55 KB) — OpenGL ES 2.0 fallback
- **prism_sw.dll** (67 KB) — Software rasterizer CPU fallback
- **prism_common.dll** (65 KB) — Utilidades compartidas

### Font + imaging

```
fontmanager.dll (290 KB)      system fonts
javafx_font.dll (76 KB)       JavaFX font
javafx_iio.dll (169 KB)       Image I/O
mlib_image.dll (680 KB)       Media Library imaging
lcms.dll (242 KB)             Little CMS color mgmt
jpeg.dll (173 KB)             JPEG decoding
```

---

## 26.10 JxBrowser 7.39.0 (Chromium)

289 MB total (35% distribución).

```
chrome.dll (260 MB)                 37% JxBrowser footprint
 ├ chrome_elf.dll (1.3 MB)          ELF loader
 ├ libEGL.dll (494 KB)              EGL graphics API
 ├ libGLESv2.dll (7.6 MB)           OpenGL ES 2.0
 ├ vk_swiftshader.dll (5.1 MB)      Vulkan software rasterizer
 ├ vulkan-1.dll (969 KB)            Vulkan loader
 ├ d3dcompiler_47.dll (4.7 MB)      DirectX 11 shader compiler
 └ {ipc64/32, toolkit64/32, awt_toolkit64/32}.dll (tooling)
```

**Consideraciones ABI**:
- JxBrowser 7.39.0 tightly coupled con Chromium version
- Security patches requieren re-release de Niagara
- GPU driver vulnerabilities Direct3D/OpenGL critical

---

## 26.11 Criptografía native providers

### Pure Java (Bouncy Castle FIPS)

Priority #1 en security.providers:
```
security.provider.1=org.bouncycastle.jcajce.bcfkswrapprovider.BouncyCastleBCFKSWrapProvider
security.provider.2=org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider
security.provider.3=sun.security.provider.Sun
security.provider.4=sun.security.rsa.SunRsaSign
security.provider.5=sun.security.ec.SunEC
security.provider.6=com.sun.crypto.provider.SunJCE
```

### Native security (JRE)

```
sunec.dll (141 KB)           ECC provider (ECDSA, ECDH)
sunmscapi.dll (46 KB)         Windows MSCAPI
                              - Windows Certificate Store
                              - CNG (Crypto Next Generation)
j2pkcs11.dll (75 KB)          PKCS#11 (HSMs, hardware tokens)
j2pcsc.dll (26 KB)            Smart card reader (PC/SC)
j2gss.dll (50 KB)             GSS-API (Kerberos auth)
w2k_lsa_auth.dll (31 KB)      Windows LSA authentication
```

**Estrategia**: NO OpenSSL embebido — confía en JVM crypto providers.

**Cumplimiento FIPS 140-2 Level 1+**: Industrial systems Honeywell requieren. Niagara bundle FIPS stacks separados.

---

## 26.12 Document generation (Office Interop)

Ubicación: `/printout/`. Formato: PE32 x86 .NET CLR.

| DLL | Tamaño | Propósito |
|---|---|---|
| Word.dll | 484 KB | Word 2003 Primary Interop Assembly |
| Office.dll | 152 KB | Office library |
| WireSheetControl.dll | 344 KB | Excel worksheet renderer |
| Interop.Shell32.dll | 48 KB | Shell/COM API |

**Requisito**: MS Office instalado en host (Word 2003+).
**Uso**: Excel/Word export en Workbench reports.

---

## 26.13 Debugging + profiling + accessibility

### JVM debugging

```
jdwp.dll (206 KB)             Java Debug Wire Protocol
dt_socket.dll (32 KB)         JDWP socket transport
dt_shmem.dll (36 KB)          Shared memory transport
hprof.dll (163 KB)            CPU/memory profiler
instrument.dll (214 KB)       Instrumentation API
```

### Networking + compression

```
net.dll (105 KB)              sockets, HTTP
nio.dll (66 KB)               non-blocking I/O
zip.dll (88 KB)               ZIP/DEFLATE
unpack.dll (87 KB)            Pack200
```

### Accessibility (MSAA/JAB)

```
JAWTAccessBridge-64.dll (22 KB)
JavaAccessBridge-64.dll (153 KB)
WindowsAccessBridge-64.dll (215 KB)
```
**Seguridad**: potencialmente explotable para UI spoofing — disable si no required.

---

## 26.14 Java 2 SecurityManager policy

### Policy file

`bin/policy/java.policy` — invocado con `-Djava.security.manager`.

Ejemplo:
```
grant codeBase "file:${niagara.home}/bin/ext/nre.jar" {
  permission java.util.PropertyPermission "java.home", "read";
  permission java.util.PropertyPermission "os.*", "read";
  permission java.util.PropertyPermission "niagara.*", "read";
  permission java.lang.RuntimePermission "loadLibrary.nre";
  permission java.lang.reflect.ReflectPermission "suppressAccessChecks";
  permission com.tridium.nre.security.NiagaraBasicPermission "GET_PLATFORM_PROVIDER";
};

grant codeBase "file:${niagara.home}/bin/ext/niagarad.jar" {
  permission java.util.PropertyPermission "*", "read,write";
  permission java.util.logging.LoggingPermission "control";
  permission java.io.FilePermission "${niagara.user.home}${/}daemon", "read,write";
  permission java.io.FilePermission "${niagara.user.home}${/}daemon${/}*", "read,write";
  permission java.lang.RuntimePermission "accessClassInPackage.sun.misc";
  permission java.lang.RuntimePermission "accessClassInPackage.sun.security.*";
  permission com.tridium.nre.di.NreSupplierPermission "*";
};
```

Permisos críticos:
- `sun.misc` + `sun.security.x509`: reflexión internas
- FilePermission on `${niagara.user.home}/daemon/*`: runtime data
- NreSupplierPermission, NreInstancePermission: DI Niagara

---

## 26.15 Install-data + MSI resources

Ubicación: `bin/install-data/`

```
Powered_by_Niagara_4.bmp (21 KB)     splash
install.properties (14 KB)           config GUI installer
licenseAgreement.txt (23 KB)         EULA
sidebarImage.bmp (76 KB)
titleImage.bmp (105 KB)
version.properties (565 B)
```

### install.properties extracto

```properties
window.title=Optimizer Supervisor Installation Program
default.folder=C:\Honeywell\OptimisorSupervisor-N%version%
startMenu.folder=Optimizer Supervisor N%version%

shortcut.0.bin=bin\\wb_w.exe
shortcut.0.args=-profile:galileoSupervisor:GalileoProfile
shortcut.0.lnk=Optimizer Supervisor Workbench

shortcut.4.bin=bin\\plat.exe
shortcut.4.args=installdaemon
shortcut.4.lnk=Install Platform Daemon

finish.launchWbCommand="bin\\wb_w.exe -profile:galileoSupervisor:GalileoProfile"
```

### Flujo instalación

1. Accept EULA (`licenseAgreement.accept`)
2. Select folder destino
3. Create shortcuts (Workbench, Console, Platform Daemon)
4. Generate passphrase (10+ chars, mix case+digit+special)
5. Copy overlay directory + generate host ID + key material
6. Optionally start daemon / copy prior data

---

## 26.16 Verificación integridad + firma

### MANIFEST.MF (META-INF/)

17 KB, 490+ entradas. Central de verification.
```
Manifest-Version: 1.0
Created-By: 21.0.3 (Azul Systems, Inc.)

Name: ext/system/jffi-1.3.12-native.jar
SHA-256-Digest: P8Q9nZjtP8tribtyfwPkUMGAzDb5ByDgC/e6YisPqPw=

Name: nre.dll
SHA-256-Digest: YG/xxqedi8TFLiH/cG6SqXbAjt68VYBTxEMmRbZLKmk=

Name: bin/x86/vcruntime140.dll
SHA-256-Digest: jPHq9QZZryavzPJ31oW3EPcYawtHXiETCFWASsS31c=
```

Todas entradas firmadas SHA-256. Launcher verifica digest antes load.

### Certificados

- **NIAGARA4.SF** (17 KB) — signature file
- **NIAGARA4.RSA** (11 KB) — X.509 RSA certificate chain

**Mecanismo**: nverify.exe valida usando BouncyCastle.

### Truststores

**System** (Tridium CA root):
```
<niagara_home>/security/truststore.jks
 ├ Tridium.certificate
 ├ Honeywell.certificate (Honeywell CodeSign CA)
 └ HoneywellCentraLine.certificate
```

**User** (custom/self-signed):
```
<niagara_user_home>/certManagement/
 ├ Tridium Trust Store/     (CA confiables default)
 └ User Trust Store/         (importados manualmente)
     └ MyModuleSigningCert.pem
```

---

## 26.17 PLAYBOOK OPERACIONAL — Firmar módulos SIN Workbench

**Respuesta directa al pedido del usuario**: "cómo podemos firmar los módulos nosotros mismos sin tener que hacerlo dentro de workbench".

### Única vía standalone

Gradle plugin `com.tridium:niagara-signing-plugin:1.0.10` (classpath dependency en `etc/gradle/public_libraries.gradle`).

### Arquitectura firma dual

- **Embedded** (dentro JAR): estándar jarsigner, integrada en manifest
- **Sidecar .sig** (sidecar): **siempre 256 bytes exactos** (RSA-2048 raw signature)
- Ubicación: `modulo-name.jar` → `modulo-name.jar.sig` (mismo dir)

### Keystores soportados

| Tipo | Niagara default | Soporte |
|---|---|---|
| JCEKS | SÍ (XML profile) | Nativo |
| JKS | legado | keytool legacy |
| PKCS12 | no default | interop OpenSSL/CA |

---

### PASO 1: Crear signing profile

```bash
mkdir -p ~/my-signing-profiles
cd /ruta/a/modulo/gradle/root
gradlew :createProfile --profile-path ~/my-signing-profiles/custom.xml
```

### PASO 2: Editar XML profile

`~/my-signing-profiles/custom.xml`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd">
<properties>
  <comment>Code Signing Properties - Custom Organization</comment>

  <!-- Keystore -->
  <entry key="niagara.signing.storetype">JCEKS</entry>
  <entry key="niagara.signing.storepass">MyStore@Pass123</entry>

  <!-- Certificate gen defaults -->
  <entry key="niagara.signing.dname">C=US,ST=California,L=San Francisco,O=MyCompany Inc,OU=Engineering,CN=${alias}</entry>
  <entry key="niagara.signing.keyalg">RSA</entry>
  <entry key="niagara.signing.keysize">2048</entry>
  <entry key="niagara.signing.validity">365</entry>

  <!-- Profile type (restricted for self-signed) -->
  <entry key="niagara.signing.profileType">com.tridium.gradle.plugins.signing.profile.RestrictedSigningProfile</entry>

  <!-- TSA (opcional pero RECOMENDADO para producción) -->
  <entry key="niagara.signing.standardtsa">http://timestamp.digicert.com</entry>
</properties>
```

Campos dname:
- `C`: ISO 3166-1 alpha-2 (2 chars)
- `ST`: State/Province
- `L`: Locality/City
- `O`: Organization
- `OU`: Organizational Unit
- `CN`: Common Name (`${alias}` reemplazado por alias del cert)

### PASO 3: Generar certificado self-signed

```bash
gradlew :generateCertificate \
  --profile-path ~/my-signing-profiles/custom.xml \
  --alias MyModuleSigningCert

# Prompts:
# Enter private key password for 'MyModuleSigningCert': PrivateKey@123
# Re-enter private key password: PrivateKey@123
#
# Password requisitos:
#   min 10 chars, 1 digit, 1 lowercase, 1 uppercase

# Output:
# Certificate created successfully with alias: MyModuleSigningCert
```

### PASO 4 (alternativa keytool legacy)

```bash
keytool -genkeypair \
  -keystore ~/my-keystore.jceks \
  -storetype JCEKS \
  -storepass "MyStore@Pass123" \
  -keypass "PrivateKey@123" \
  -alias MyModuleSigningCert \
  -keyalg RSA \
  -keysize 2048 \
  -validity 365 \
  -dname "C=US,ST=California,L=San Francisco,O=MyCompany Inc,OU=Engineering,CN=MyModuleSigningCert"
```

### PASO 5: Configurar build.gradle del módulo

`my-module/build.gradle`:
```gradle
plugins {
  id 'com.tridium.niagara'
}

group = 'com.mycompany'
name = 'mymodule'
version = '1.0.0'

niagaraModule {
  moduleName = 'com.mycompany~mymodule~1.0.0'
  runtimeProfile = 'rt'
  bajaVersion = '4.14.0'
}

niagaraSigning {
  aliases.set(listOf("MyModuleSigningCert"))
  signingProfileFile.set(project.layout.projectDirectory.file("../my-signing-profiles/custom.xml"))
}
```

`my-module/gradle.properties`:
```properties
niagara_home=C:\\Honeywell\\OptimizerSupervisor-N4.14.0.162
niagara_user_home=C:\\Users\\equipo\\Niagara4.14\\OptimizerSupervisor

org.gradle.java.installations.auto-detect=false
org.gradle.java.installations.paths=C:\\Program Files\\Zulu\\zulu-8
```

### PASO 6: Build + firmar

```bash
gradlew clean
gradlew build

# Tasks ejecutados:
# > Task :compileJava
# > Task :jar
# > Task :signingJar   (firma JAR + genera .sig sidecar)
# > Build successful!
```

Output:
```
my-module/build/libs/mymodule-1.0.0.jar
my-module/build/libs/mymodule-1.0.0.jar.sig  (256 bytes exactos)
```

### PASO 7: Exportar certificado PEM

```bash
gradlew :exportCertificate \
  --profile-path ~/my-signing-profiles/custom.xml \
  --alias MyModuleSigningCert \
  --pem-file MyModuleSigningCert.pem

# Output:
# Certificate exported to: MyModuleSigningCert.pem (1200-1500 bytes)
```

Contenido:
```
-----BEGIN CERTIFICATE-----
MIIDXTCCAkWgAwIBAgIJAJ...
-----END CERTIFICATE-----
```

Verificación opcional:
```bash
openssl x509 -in MyModuleSigningCert.pem -text -noout | head -30
```

### PASO 8: Deploy

Opción A — copia directa:
```bash
cp build/libs/mymodule-1.0.0.jar     "C:\Honeywell\OptimizerSupervisor-N4.14.0.162\modules\"
cp build/libs/mymodule-1.0.0.jar.sig "C:\Honeywell\OptimizerSupervisor-N4.14.0.162\modules\"
```

Opción B — Software Manager (Workbench GUI detecta .sig automático).

### PASO 9: Importar cert en User Trust Store (si modo MEDIUM/HIGH)

Workbench connected to station:
```
Tools → Certificate Management → User Trust Store → Import → MyModuleSigningCert.pem
```

### PASO 10: Verificación

**Local** (pre-deploy):
```bash
# Via jarsigner
jarsigner -verify -verbose -certs build/libs/mymodule-1.0.0.jar
# Esperado:
# s = signature verified
# m = entry listed in manifest
# k = cert found in keystore

# Via nverify
/path/to/niagara/bin/nverify.exe build/libs/mymodule-1.0.0.jar

# Tamaño .sig
ls -lh build/libs/mymodule-1.0.0.jar.sig
# -rw-r--r-- ... 256 Apr 23 15:30
```

**Station** (post-deploy):
```
Tools → Module Info → seleccionar módulo
```
Estado:
- 🟢 Verde: "module signed with trusted CA"
- 🟡 Amarillo: "self-signed but in User Trust Store"
- 🔴 Rojo: errores (expirado, no confiable, modificado)

Logs:
```
<niagara_user_home>/logs/nre.log
grep "Module signature verification"
# [INFO] Module signature verification OK: mymodule-1.0.0.jar
```

---

## 26.18 Troubleshooting firma

| Error | Causa | Solución |
|---|---|---|
| "Module signature invalid" en módulo recién firmado | JAR modificado post-firma | `gradlew clean build` completo |
| "Certificate not trusted" en station | Self-signed + modo HIGH, o cert no importado | Cambiar modo a LOW/MEDIUM o import PEM en User Trust Store |
| "Signature failed: SHA256withRSA" | Algoritmo mismatch | Verificar keyalg=RSA, keysize≥2048 en XML |
| .sig file missing | Gradle task no corrió | `gradlew signingJar` explícito |
| "TSA connection failed" | Sin acceso internet | Comentar `niagara.signing.standardtsa` o usar TSA interna |
| "Module verification mode high" | Station rechaza self-signed | `niagara.moduleVerificationMode=medium` en system.properties |
| "Keystore was tampered with" | Password incorrecta o archivo corrupto | Verificar storepass en XML |
| "Invalid private key password" | password no coincide | Re-generar cert con generateCertificate |
| "TSA connection failed" gradle | red bloqueada | Comentar TSA en XML durante dev |

---

## 26.19 Configuración avanzada firma

### Múltiples certs en mismo profile

```bash
gradlew generateCertificate --alias Cert1
gradlew generateCertificate --alias Cert2
```

build.gradle modulo-1:
```gradle
niagaraSigning { aliases.set(listOf("Cert1")) }
```
build.gradle modulo-2:
```gradle
niagaraSigning { aliases.set(listOf("Cert2")) }
```

### CA-signed commercial (VeriSign/Thawte)

```bash
# 1. Generate CSR
gradlew :exportCertificate \
  --profile-path ~/my-signing-profiles/custom.xml \
  --alias MyCert \
  --csr-only \
  --pem-file MyCert.csr

# 2. Enviar CSR a CA → recibir signed-cert.pem

# 3. Import response
gradlew :importCertificate \
  --profile-path ~/my-signing-profiles/custom.xml \
  --alias MyCert \
  --pem-file signed-cert.pem
# Trust this certificate? (y/n) → y
```

### HSM (Hardware Security Module)

Para producción con Thales/YubiKey:

`my-custom-signing.properties`:
```properties
niagara.signing.profileType=com.tridium.gradle.plugins.signing.profile.JarSignerSigningProfile
jarsigner.cmd=jarsigner

jarsigner.args+=-providerClass
jarsigner.args+=sun.security.pkcs11.SunPKCS11
jarsigner.args+=-providerArg
jarsigner.args+=${profile.path}/pkcs11.conf
jarsigner.args+=-keystore
jarsigner.args+=NONE
jarsigner.args+=-storetype
jarsigner.args+=PKCS11
jarsigner.args+=-storepass
jarsigner.args=${jarsigner.storepass}
```
(Requiere drivers HSM instalados previamente).

### Re-firmar módulo existente

```bash
# 1. Extraer JAR
unzip mymodule-1.0.0.jar -d mymodule-extracted

# 2. Crear JAR sin firma
jar cvf mymodule-unsigned.jar -C mymodule-extracted .

# 3. Firmar con nuevo cert
gradlew signingJar \
  --input=mymodule-unsigned.jar \
  --profile-path ~/my-signing-profiles/custom.xml \
  --alias NewCertAlias
```

---

## 26.20 Matriz decisión método firma

```
¿Uso en producción?
 ├ SÍ
 │  ├ ¿Presupuesto CA comercial (VeriSign)?
 │  │  ├ SÍ → CA público
 │  │  └ NO → CA interno si disponible, o HSM
 │  └ ¿Múltiples stations a actualizar?
 │     ├ SÍ → HSM o CA interno (escalable)
 │     └ NO → self-signed + import en cada station
 └ NO (testing/dev)
    └ Self-signed → rápido, no requiere CA

¿Dónde guardar keys?
 ├ Dev → XML profile local (~/.tridium/security/)
 ├ Production → HSM
 └ Intermediate → CA-signed en XML profile en máquina aislada
```

---

## 26.21 Gotchas críticos firma

1. **Plaintext passwords en XML** — proteger permisos 600; guardar en máquina segura.
2. **TSA availability** — sin timestamping, módulo falla post-expiración del cert. TSA crítico para producción.
3. **256 bytes exactos** `.sig` para RSA-2048. No confundir con firma embedded (ASN.1, más grande).
4. **Certificados expirados** — sin TSA, falla a validar post-expiración. TSA obligatorio producción.
5. **Modo HIGH futuro** — Niagara planea cambiar default LOW → MEDIUM (4.9) → HIGH. Self-signed será rechazado. Prepare con CA.
6. **3 groups SIEMPRE requieren firma Tridium** (Bloque 18.4): ACCESS_CLASS, REFLECTION, MBEAN_PERMISSION. Self-signed NO aplica a estos groups.
7. **Gradle wrapper vs system** — usar `gradlew` (wrapper local) para evitar version mismatch.
8. **Keystore corruption** tras "tampered with" — re-crear con `createProfile`.
9. **Password requirements** — min 10 chars + 1 digit + 1 lowercase + 1 uppercase (gradle valida).
10. **Niagara.signing.storepass en XML plaintext** — file permissions 600 + segregate secrets en repo.

---

## 26.22 Checklist pre-deployment

- [ ] `gradlew clean build` completó sin errores
- [ ] `build/libs/mymodule-*.jar` existe (>1 KB)
- [ ] `build/libs/mymodule-*.jar.sig` existe (exactamente 256 bytes)
- [ ] `jarsigner -verify mymodule-*.jar` retorna "jar verified"
- [ ] Certificate exportado a PEM (1200+ bytes)
- [ ] Si modo MEDIUM/HIGH: cert importado en User Trust Store de cada station
- [ ] `niagara.moduleVerificationMode` compatible con tipo certificado
- [ ] Logs station: "Module signature verification OK"
- [ ] Module Info view: verde/amarillo (no rojo)
- [ ] Backup XML profile + keystore en ubicación segura

---

## 26.23 Security surface + risks

### Críticos

1. **FFmpeg** (34.9 MB) — alto CVE rate históricamente en parsers codec. Mitigación: sandboxing, permiso upload restrictivo.
2. **Chromium/JxBrowser** (260 MB chrome.dll) — requiere patching independiente. Mitigación: frequent Niagara releases.
3. **WinPcap** (pcapBacEther.dll) — deprecated desde 2018, CVEs conocidos. Mitigación: deprecar BACnet Ethernet, usar IP-based.
4. **MSVC runtime multiplexing** — potencial DLL boundary exception. Mitigación: unified CRT via ucrtbase.dll.
5. **Accessibility bridges** — explotable UI spoofing. Mitigación: disable si no required.

### Moderados

- Direct3D 11 GPU driver vulnerabilities (prism_d3d.dll)
- Font rasterization (freetype.dll)
- OpenSSL replacements pure Java (posible des-optimización)

---

## 26.24 Runtime loading timeline

```
FASE 1: Startup
 1. java.exe inicia
 2. jvm.dll cargado (8.3 MB)
 3. ucrtbase.dll, VCRUNTIME140 cargados
 4. JVM bootstrap

FASE 2: AWT/Swing init (si Workbench)
 1. awt.dll cargado (1.5 MB)
 2. freetype.dll fonts
 3. glass.dll JavaFX

FASE 3: Chromium embedding (si HTML5 UI)
 1. jfxwebkit.dll cargado (75 MB)
 2. chrome.dll + deps (289 MB JxBrowser)
 3. prism_d3d.dll GPU rendering

FASE 4: Module-specific on-demand
 - opc.dll cuando OPC-UA module enabled
 - lon.dll cuando LON module enabled
 - FFmpeg cuando ffmpeg-wb.jar activo
```

---

## 26.25 Shutdown + signal handling

### Station shutdown (Windows)

Strings en nre.dll:
```
niagarad: Niagara service shutdown initiated.
niagarad: Niagara service shutdown failed, failed to locate Niagara Daemon class.
niagarad: Niagara service shutdown complete, set service status to stopped.
```

Mecanismo:
1. `plat.exe installdaemon` crea Windows service "NiagaraDaemon"
2. SCM envía `SERVICE_CONTROL_STOP` → JVM recibe SIGTERM via shutdown hook
3. `java.lang.SecurityException` si permisos insuficientes

niagarad.exe actúa como service wrapper:
- SCM llama `ServiceMain()` con SERVICE_STOPPED
- niagarad.exe inicia station.exe + JVM
- Monitorea heartbeat; reinicia en crash

---

## 26.26 Directorios referencia

**Binarios/herramientas**:
- `/bin/nverify.exe` — verificador firma CLI
- `/jre/bin/keytool` — keystore (legacy)

**Plugin gradle**:
- Clase: `com.tridium.gradle.plugins.signing.profile.RestrictedSigningProfile`
- JAR: `/bin/ext/` (transitivo via `public_libraries.gradle`)

**Certificados sistema**:
- `/security/truststore.jks` — System Trust Store (Tridium/Honeywell roots)
- `/security/certificates/Tridium.certificate` — raíz Tridium PEM

**Documentación**:
- `/niagara-help/docs-text/docModuleSign.txt`
- `/niagara-help/devguide-clean/security/codeSigning.txt`
- `/niagara-help/guides-clean/ModuleSign/`

---

## Fuentes primarias leídas

1. `bin/*.exe` + `bin/*.dll` (nre.exe, station.exe, wb.exe, niagarad.exe, n4mig.exe, plat.exe, nverify.exe, nre.dll, njre.dll, common.dll, cppunit.dll, msvcp140, msvcr120, vcruntime140, opc.dll, lon.dll, dsfspi.dll, honImport.dll, alarmDialog.dll, trayIcon.dll, pcapBacEther.dll)
2. `bin/ext/` — Kotlin + BouncyCastle FIPS + JxBrowser 7.39 + Jetty 9.4.54 + OrientDB 3.2.23 + asm 9.6 + jose4j 0.9.6 + libthrift 0.17
3. `bin/install-data/` (install.properties, EULA, splash)
4. `bin/policy/java.policy` — Java 2 SecurityManager grants
5. `bin/x86/` — 32-bit fallback (ldvProxy, MSVC 32)
6. `jre/bin/*.dll` — Azul Zulu 1.8.0.412.20 Win x64 (awt, jawt, freetype, glass, jfxwebkit 75MB, prism_d3d/es2/sw, sunec, sunmscapi, j2pkcs11, j2pcsc, j2gss, w2k_lsa_auth, jdwp, dt_socket, dt_shmem, hprof, instrument)
7. `JxBrowser/7.39.0/*.dll` — chrome.dll 260MB + chrome_elf + libEGL + libGLESv2 + vk_swiftshader + vulkan-1 + d3dcompiler_47
8. `printout/*.dll` — Word/Office 2003 PIA + WireSheetControl + Interop.Shell32
9. `modules/ffmpeg-wb.jar` — avcodec-60 18.4MB + avformat-60 + avfilter-9 + swscale-7 + swresample-4 + avdevice-60 + avutil-58 + ffmpeg-wrapper
10. `libciper.so` 123KB ELF ARM EABI5 debug symbols retained
11. `META-INF/MANIFEST.MF` + `NIAGARA4.SF` + `NIAGARA4.RSA` (SHA-256 digests, X.509 RSA chain)
12. `etc/gradle/public_libraries.gradle` (niagara-signing-plugin:1.0.10)
13. `niagara-help/docs-text/docModuleSign.txt` + `devguide-clean/security/codeSigning.txt`
14. `defaults/nre.properties` + `system.properties`

Total: ≈138 artefactos nativos catalogados, 6 subsistemas (AWT+JavaFX+Chromium+Office+FFmpeg+Protocol drivers), 3 ABIs (PE32+ x64, PE32 x86 .NET, ELF ARM EABI5), cadenas MSVC runtime documentadas, playbook firma standalone 10 pasos con 9 troubleshooting entries.
