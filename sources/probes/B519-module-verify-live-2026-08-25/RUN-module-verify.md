# PROBE — B519 live module-verification posture (read-only, WSL interop)
Host: OptimizerSupervisor N4.14.0.162 (operator's working supervisor, 11 station configs).

## moduleVerificationMode LIVE (defaults/system.properties)
niagara.moduleVerificationMode=low
#niagara.commandLinePropertyBlacklist=niagara.moduleVerificationMode,program.requireSigning,niagara.export.preventCSVInjection,\

## signing.properties build-time pin
issuerDN=CN\=Honeywell CodeSign RSA CA, OU\=ACS, O\=Honeywell International Inc., C\=US
subjectDN=C\=US, O\=Honeywell International Inc., CN\=Niagara4Modules Code Signing

## truststore anchor
-rwxrwxrwx 1 cristian cristian 958 Jan 15  2026 /mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162/security/truststore.jks

## nverify on a shipped module (Windows path) — ran the verify path, no SEVERE
nverify.exe C:\...\modulesbstractMqttDriver-rt.jar -> INFO Verifying archive (no failure emitted)
