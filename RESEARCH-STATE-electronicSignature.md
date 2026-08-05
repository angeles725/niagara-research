# RESEARCH-STATE — focus: electronicSignature (ACTIVE, 1/7)

> Multi-focus corpus (METHODOLOGY §16). Focus **BOOTSTRAPPED 2026-08-04** at the user's explicit request
> ("¿tenemos documentado el módulo de firma / que cumple 21 CFR Part 11?"), immediately after closing `jsonToolkit`.
>
> **NOT documented before** — audit-first. Prior corpus coverage: NONE dedicated. Precise search for
> `electronic signature` matched only [Block 34] (an FDA 21 CFR 11 *hypothesis* about where alarms should live —
> not this module). `niagara-help` has ZERO: `guide-search "21 CFR"|"part 11"|"electronic signature"` = 0 this
> session (only [Histories] "Audit trail management" exists, the record side). No CATALOG row, no prior focus.
> A third party had mis-identified `signingService` (PKI cert-signing) as the Part 11 module — refuted; the real
> module is `electronicSignature` (B350 §350.6).
>
> **Declared angle (§b2)**: the TridiumPS add-on `electronicSignature` (+ `electronicSignatureRemote`), namespaces
> `com.tridiumx.ps.*` + `com.secured.*`, as the 21 CFR Part 11 electronic-signature layer — a point is secured by
> TYPE SUBSTITUTION (`BSecured*Writable`) so every write verb requires a signed `*WithAuthentication` twin carrying
> re-authenticated credentials, a mandatory reason-for-change, an optional second signer, and a history audit record.
> License feature `tridium:eSignature` + `point.limit`. First corpus coverage of a **TridiumPS** (Professional
> Services) deliverable.
>
> ⚠ **Obfuscation caveat** (B350 header): the decompiled/vineflower Java is string-scrubbed (literals → `n`/`ln`);
> bytecode `.class` and `extracted/` resources are intact. This focus cites STRINGS from bytecode/resources, never
> from decompiled `.java`; method-body mechanics are `[INFER]` until re-derived from bytecode (`javap`) or dynamic.

<!-- research-state.v1 -->
schema: research-state.v1
block_scope: shared-global
covered_blocks: 347
gaps_closed: 2
known_gaps: 8
investigable_open: 5
requires_execution_open: 1
blocked_open: 0
deferred_open: 0
undocumented_findings: 0
<!-- /research-state.v1 -->

focus: electronicSignature
status: active
bootstrapped_on: 2026-08-04
block_prefix: niagara-mental-model-bloqueN.md (global numbering; next free: B352)

## Pre-flight e2 — existence + MEASURED size

Root: `/home/cristian/modules/Prototipos/modulos/organized/electronicSignature/` (+ `electronicSignatureRemote/`).
Own classes (distinct, `com.tridiumx.ps.*` + `com.secured.*`, excluding vendored Apache Commons IO/Codec and
servlet stubs): rt ≈30, ux 26, wb ≈30, remote 1. `module.xml` registers **39 component types** (B350 §350.2).
Source CONFIRMED reachable; bytecode intact (obfuscation is decompiler-output only). All gaps investigable.

## Coverage

| Gap | Question | Block | Status |
|---|---|---|---|
| ES1 | Identity, origin, type system, license gate, Part 11 mapping | B350 | closed |
| ES2 | Sign flow end-to-end (license→credential→reason→super-action→audit order) | — | pending |
| ES3 | Dual-signature / remote transport (queue, email notify, persistence) | — | pending |
| ES4 | Audit-trail protection + can BHistoryMaintenance purge it without a signature (§11.10(e))? | B351 | closed |
| ES4-G1 | Live-permission: does a stock RBAC role grant invoke on BHistoryMaintenance to a non-super-user? (requires-execution) | — | blocked-on-live-server |
| ES5 | Credential handling (Base64-not-encrypted? 500ms sleep? confirm from bytecode) | — | pending |
| ES6 | ux/wb layers (web editors, Swing credential dialog, PX/Hx bindings) | — | pending |
| ES7 | Mutable ESignAcknowledgement property vs baked lexicon — can it diverge? | — | pending |

## Backlog (investigable)

| Priority | Gap | Notes | Status |
|---|---|---|---|
| high | ES4 audit-trail protection + purge surface | §11.10(e) integrity — CLOSED B351: no crypto tamper-evidence + 3 unauthenticated purge actions | closed |
| high | ES2 sign flow end-to-end | re-derive method order from bytecode (scrubbed source) | pending |
| medium | ES5 credential handling | confirm Base64/sleep hypotheses from bytecode | pending |
| medium | ES3 dual-signature / remote transport | queue persistence in-memory vs BOG | pending |
| medium | ES6 ux/wb layers | acknowledgement capture in editors + PX binding | pending |
| low | ES7 mutable ESignAcknowledgement | divergence from baked lexicon | pending |

## Iteration history

| Block | Gap | Delegated? · model tier | Notes |
|---|---|---|---|
| B350 | ES1 | yes · sonnet (audit sweep) + inline verify | Foundation. Sweep mapped taxonomy/flow; driver re-verified all load-bearing citations against bytecode/resources (obfuscation caught: decompiled strings scrubbed). [CERT]×35 [CERT-doc]×3 [INFER]×15, ratio 0.39. |
| B351 | ES4 | yes · sonnet (purge sweep) + inline verify | No crypto tamper-evidence (plaintext `::` trend record); `BHistoryMaintenance extends BComponent` with 3 `newAction(0,…)` purge actions calling core `HistorySpaceConnection` directly, no signature/auth/BReasons; ordinary history, fullPolicy default `roll`. Driver re-verified all [CERT] (javap + source). [CERT]×35 [CERT-doc]×3 [INFER]×5, ratio 0.13. Opened child ES4-G1 (live-permission reachability). |

## Dismissed file types

Vendored `org.apache.commons.io` / `org.apache.commons.codec` (BeiderMorse/Caverphone phonetic codecs) and
`javax.servlet.*` stubs — third-party bundles, not the module; excluded from class counts and out of scope.
