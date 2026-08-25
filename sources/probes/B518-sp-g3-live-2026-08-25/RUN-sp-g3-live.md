# PROBE — SP-G3-live: runtime license verifier fail-closed on tampered signature

Target: OptimizerSupervisor N4.14.0.162 (Honeywell OEM), operator's own ISOLATED test host.
Channel: WSL→Windows interop running the REAL `nre.exe` runtime (fresh JVM, same license
load/verify path the station uses at boot). Station was LIVE during the test
(https://localhost/ = 302, platform daemon :5011 = 403). SECRETS DISCIPLINE: HostId shown
as format `Win-XXXX-XXXX-XXXX-XXXX`; no secret values. Credentials never persisted.

## Reversibility (METHODOLOGY §12 / RUNBOOK-REVERSIBILIDAD pattern)
- backup-before-destroy: security/licenses + certificates copied to staging + sha256 manifest.
- independent oracle: `nre.exe -licenses` (fresh JVM re-reads security/), NOT the writing channel.
- guaranteed restore: bash `trap ... EXIT` restores original even on error.
- byte-identical proof: post-restore sha256 == baseline (CONFIRMED).
- live station NOT rebooted; nre is a separate JVM, invisible to the running station's in-memory license map.

## Baseline oracle (intact)
HostId=Win-XXXX-XXXX-XXXX-XXXX
  Honeywell.license {valid} · HoneywellCentraLine.license {valid} · Webs.license {valid}

## Tamper (Webs.license, Tridium DSA-signed): flip 1 byte in signature region, length preserved
offset 16153: 0x68 -> 0x69 (len unchanged = 16193)

## Oracle with tampered license (real runtime verifier)
GRAVE [baja] License file not loaded - Webs.license {invalid: Invalid signature}
  Webs.license {invalid: Invalid signature}      <- REJECTED, fail-closed
  Honeywell.license {valid} · HoneywellCentraLine.license {valid}   <- others unaffected

## Restore
restored sha256 == baseline sha256  ->  byte-identical restore CONFIRMED

## Verdict
The Java-side Niagara runtime license verifier performs a REAL cryptographic (DSA) signature
check and FAILS CLOSED on a 1-byte signature tamper: the license is "not loaded", its features
withheld. Upgrades SP-G3 PARTIAL -> [CERT-live]. (Offline replica already [CERT] in B397/B323.)
Open child nuance: whether a *required* feature's absence forces station process exit(-3/-6) at
full boot vs graceful feature-withholding — SP-G3-live proves verifier rejection, not the exit path.
