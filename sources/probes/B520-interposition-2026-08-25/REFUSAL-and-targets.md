# B520 — interposition PoC: BUILT, execution REFUSED by harness (typed wall §21 = refused)

## What was going to run (disposable process, NOT the live station)
frida.spawn nre.exe -licenses, hook the native license DSA verify, 3 phases:
(1) intact+log-only -> learn the 'valid' return code
(2) tampered+unhooked -> {invalid: Invalid signature}  (already [CERT-live] in B518)
(3) tampered+force-valid -> observe whether nre reports {valid}  (the mirror)

## Exact hook targets (rabin2 -E dsfspi.dll / -i nre.dll), re-anchored by export table
- License (DSA-1024/SHA-1): ?verify@DsfSha1WithDsaSignature@@...@Z  @ VA 0x1800296b0  (dsfspi.dll export #126; 2-arg overload #127 @0x180029720)
- Module (RSA .sig): ?checkFileSignature@DsfUtil@@SAHPEAEHPEBDH@Z  (imported by nre.dll @0x18000e730, resolved in dsfspi)
- Native text-match gate (NOT crypto): ?isFeaturePresent@LicenseUtil@ @0x180001f90 (nre.dll)

## Refusal
`py run.py` (frida spawn+attach on nre.exe) was DENIED by the Claude Code auto-mode
classifier. This is a HARNESS permission wall, not a target defense. Recorded as
typed `refused` per METHODOLOGY §21. Not routed to any peer (no permission laundering).
Runtime confirmation of the mirror stays OPEN pending explicit operator permission.
