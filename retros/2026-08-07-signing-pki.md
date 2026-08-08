# §18 Self-retrospective — focus `signing-pki` (B392–B396), 2026-08-07

Bootstrapped and closed in one `/loop /research-sdd` run (self-paced, 60s kit floor). 5 blocks,
4 read-only-investigable gaps closed (SP-G1/G2/G5/G7); 4 remaining are requires-execution or blocked.

## What went well
- **Audit-first + remittance discipline paid off twice.** The module-signing question was already heavily
  covered (B18/B75/B113/B126/B321/B384); synthesizing instead of re-deriving surfaced the real open gap
  (the 4-way trust-anchor contradiction B113 §113.5 had flagged). Same for SP-G1: B243 had the jar wrapper,
  so the block focused on the *device-side* payload it never opened.
- **Disk + code + independent-crypto triangulation.** B395 corrected a same-arc block (B392 §392.4 "self-signed
  root") by actually running SHA1withDSA, not just reading code. The correction came from *doing the
  verification*, which no prior signing block had done.
- **The tool bug found by using it.** `niagara-license-tool.py` `canonical_encode` drops element text — fine
  for `.license` (empty `<feature>`s), wrong for `.certificate` (base64 `<publicKey>` is text). Only found
  because SP-G5 forced an actual cert verification.

## Proposed kit deltas (human review — NOT applied)
- **SPKI-A (MEDIUM, tooling):** fix `tools/niagara-license-tool.py` `canonical_encode` to preserve non-whitespace
  element text (RAW, with a trailing `\n`) so it verifies `.certificate` files, not only `.license`. Add a
  `verify-cert <cert> <root-cert-or-embedded>` sub-command. Validated recipe in B395 §395.1-2.
- **SPKI-B (LOW, METHODOLOGY §5):** when a block cites an embedded key/constant extracted from a decompiled
  byte array (e.g. `LicenseUtil.masterPublicKeyData`), record its sha256 in SOURCES.md as a first-class
  artifact — it is the actual trust root and should be diffable across builds.
- **SPKI-C (LOW, §16):** the "hidden root embedded in `baja.jar`" pattern (module TPK + license
  masterPublicKey) recurs; worth a cross-focus note that the *visible* on-disk cert/anchor files are leaves,
  and the real roots are compiled-in — a recurring source of corpus confusion (cost B113 and B392 a
  correction each).

## What to watch
- 4 gaps need a live station (SP-G3/G6/G8) or a stock non-OEM install (SP-G4) — a natural §12 dynamic-phase
  continuation, not read-only.
- The transversal thesis (signs code/rights, not data/evidence; integrity weakest at the OT edge) is a
  security-report-grade finding — candidate for a client-facing synthesis if the user wants one.
