# PROBE — SP-G3-native: does the NATIVE license gate detect the same signature flip the Java runtime rejects?

Complement to RUN-sp-g3-live.md (Java side, session "Primero"). Native side, session "Segundo".
Doctrine: READ-ONLY on all binaries and on Webs.license. No execution of nre.exe/harness; no
patching; no file writes to bin/ or security/. Pure static (r2) + data-layout (byte offsets) proof.

## Target / anchors
- nre.dll  sha256 606ff1c6a79d8bc4c52e21ff…  (== B424/B482 baseline; anchor confirmed)
- `?isFeaturePresent@LicenseUtil@@SA_NPEBD0@Z` — EXPORTED, GLOBAL FUNC,
  VA 0x180001f90 / file off 0x1390 (rabin2 -E). Offset re-anchored via the export table
  (symbol→address binding is authoritative; B424 twin-binary risk excluded). [CERT]

## Native gate body (r2 pdf @ 0x180001f90, READ-ONLY) — corroborates B126 §126.6, re-anchored
The entire function does, and only does:
  1. sprintf two needles: "<license vendor=\"%s\"" (str@0x18000ee98), "<feature name=\"%s\"" (str@0x18000eeb0)
  2. build path "%s%s" + "\security\licenses" (str@0x18000eec8) via Nre::getInstance() home
  3. NreLib::DirectoryListing::make(dir) — enumerate .license files
  4. substring-match the two needles over each file's text
  NO call to DsfSha1WithDsaSignature or any signature/DSA/crypto verify in the body. [CERT]

## Data-layout of Webs.license (grep -abo byte offsets, READ-ONLY) — size 16193
  <license vendor=   @ byte 0
  <feature name=     @ bytes 123, 243, 374, … , last @ 15963      <- ALL needles < 16094
  <signature>        @ byte 16094
  </signature>       @ byte 16169                                  <- signature region = [16094,16169)
  Primero's tamper offset 16153  ∈ [16094,16169)  = INSIDE the base64 DSA signature blob. [CERT]

## Verdict — the asymmetry B126 §126.6 predicted, settled without native execution
- The flipped byte (16153) lies strictly inside <signature>; every needle the native gate scans
  for lies strictly before 16094 and is byte-identical pre/post flip.
- Therefore isFeaturePresent's substring-match outcome is INVARIANT under this tamper (and under
  ANY signature-region tamper): the native gate keeps reporting the feature PRESENT. [INFER, built
  on two [CERT]: decompiled body has no verify; needles are disjoint from the tampered region.]
  This deductive proof is STRONGER than a single live observation: it holds for the whole
  signature region, not one offset.
- Java side (Primero, RUN-sp-g3-live.md): real DSA verify over [16094,16169) → REJECTS the same
  flip, fail-closed "Invalid signature", feature withheld. [CERT-live]
=> ASYMMETRY CONFIRMED: native fast-path text-match (blind to signature) vs Java LicenseManager
   (real DSA crypto, fail-closed). Defense-in-depth rests entirely on the Java layer, exactly as
   B126 §126.6 warned ("anything that consults only native isFeaturePresent is trusting unsigned text").

## NOT done (and why)
- Live native return value of isFeaturePresent on the tampered file was NOT observed. The sound
  paths are unavailable in this WSL/Linux toolset: a cold LoadLibrary+GetProcAddress harness would
  fault on the uninitialized Nre::getInstance() singleton; Linux frida (~/.local/bin) cannot inject
  into the Windows PE. Booting nre.exe normally would exercise the Java verifier (Primero's result),
  not isolate the native gate. Escalating to a Windows-side instrumented run is a heavier step left
  to the operator's explicit call; the static+data [CERT] proof already settles the question.
