# B528 PROBE — SP-G10a license mirror via `nre -@javaagent` (Frida-independent)

Baseline Webs.license sha256 (intact): fc548614b7033f710729366292fadc7ded7957e600e701910d047cf1c6daec72
Tampered (signature base64 byte flips, offset signature-tag+20):  b376fc0b3bca6e4ee8f9e8e685fdea3473e5ecc514e7e4fa001850f8a1c4a001

Runs (all against the tampered license):
  1. nre -licenses (no agent)                          -> GRAVE ... Webs.license {invalid: Invalid signature}
  2. nre -@javaagent:<jar>=force -licenses              -> Webs.license <Tridium> ... {valid}  (the mirror)

Key mechanics:
  - nre.exe -@<option> = "pass option to Java VM" (help text)
  - -@verbose:class showed LicenseUtil/LicenseFile/NLicenseManager loading via
    com.tridium.nre.bootstrap.BootstrapClassLoader from modules/baja.jar
  - Agent = java.lang.instrument premain + ASM 9.6 (install's own bin/ext/asm-9.6.jar)
    ClassWriter.COMPUTE_FRAMES (without it: VerifyError "Expecting a stack map frame")
    rewrites the 6 public static boolean LicenseUtil.verify(...) to ICONST_1;IRETURN

Restore: Webs.license copied back from backup -> sha256 == fc548614… (byte-identical)
Live PIDs unchanged: niagarad=21348, station=18524.
