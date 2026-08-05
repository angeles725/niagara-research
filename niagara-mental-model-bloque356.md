# Block 356 — `electronicSignature` SYNTHESIS: the 21 CFR Part 11 module bolts the signing CEREMONY and leaves the COMPLIANCE ARTIFACTS as ordinary unsigned config — focus closed 7/7

> Focus **electronicSignature** — SYNTHESIS block (focus-closing). READ-ONLY. Corpus language: ENGLISH.
>
> Consolidates the focus opened by [B350] and evidenced by [B351]-[B355]: the TridiumPS `electronicSignature` (+
> `electronicSignatureRemote`) add-on, the 21 CFR Part 11 electronic-signature layer for Niagara N4.14. No new tool sweep;
> this block synthesizes the seven closed gaps (ES1-ES7) into the load-bearing threads and states what is NOT resolved.
> Block TYPE: **synthesis**. A high `[INFER]`/`[CERT]` ratio here is EXPECTED (§11) — the evidence lives in the cited blocks.
>
> Markers: `[CERT]`/`[INFER]` as in the cited blocks; this block cites BLOCKS, not files. Layer 22 + compliance axis.

---

## 356.1 — What the module is (ES1 · [B350])

`electronicSignature` is a first-party-adjacent **TridiumPS** (Tridium Professional Services) add-on, namespaces
`com.tridiumx.ps.*` + `com.secured.*`, that makes a Niagara control point require a **signed, re-authenticated,
reason-bearing, optionally dual-approved** write. It works by **TYPE SUBSTITUTION**: a normal `BNumericWritable` becomes a
`BSecuredNumericWritable` whose stock write verbs are replaced by `*WithAuthentication` twins. Gated by license feature
`tridium:eSignature` + `point.limit`. It maps to Part 11 across §11.10, §11.50, §11.100(c), §11.200. The decompiled Java is
string-scrubbed (literals → `n`/`ln`); bytecode + resources are intact — every string-dependent claim in this focus was
taken from bytecode/lexicon, never decompiled literals.

## 356.2 — The load-bearing thesis: STRONG ceremony, WEAK artifacts

The single organizing finding of the focus:

**The module rigorously controls WHO writes and HOW they authenticate, but leaves the ARTIFACTS that give a signature its
legal force and durability as ordinary, unsigned, unaudited station data.**

| Part 11 requirement | Mechanism | Verdict | Evidence |
|---|---|---|---|
| §11.10 signed write / §11.200(a) | `*WithAuthentication` verbs; pipeline `checkLicense → verifyCredentials → invokeSuperAction → audit`; **fail-closed** (throw + branch before the write) | **STRONG** | [B352] |
| §11.200(b) second, distinct signer | async queued approval; self-approval HARD-blocked; Level-2 role enforced server-side; approver email | **STRONG** | [B353] |
| Authentication substrate | real LDAP (JAAS) or local `BPasswordCache.validate` | **STRONG** (but credential is reversible Base64 on the wire, browser `btoa` ↔ server decode) | [B352]/[B354] |
| §11.50(a)(3) signature meaning (reason) | reason **mandatory** but only non-empty — NOT validated against the configured `BReasonSet` | **WEAK** | [B352] §352.4 |
| §11.100(c) binding certification | `ESignAcknowledgement` — a **mutable, flag-0, unsigned, unaudited** property; lexicon is only its empty-fallback | **WEAK** | [B355] |
| §11.10(e) protected audit trail | `BSecuredTrendRecord` = plaintext `::` string, no crypto tamper-evidence; `BHistoryMaintenance` purge actions are unauthenticated | **WEAK** | [B351] |

The front door (authenticate, distinct dual signer, fail-closed) is bolted. The back room — the reason vocabulary, the legal
declaration, the audit record — is ordinary config an admin can rewrite or clear without ever signing.

## 356.3 — The five secondary threads

1. **Type substitution is the real enforcement, not the UI** ([B354]). `BSecured*Writable` exposes NO plain `set`/`override`/
   `auto` — only `*WithAuthentication` + a `call` dispatcher. The bajaux dialog and the `PREFERRED` menu agent that strips
   `call` are a compliance FORM; a scripted caller can bypass the dialog but still cannot write without valid credentials
   the server checks. The credential is Base64-encoded client-side by the browser's `btoa`.

2. **The credential is reversible end-to-end** ([B352]/[B353]/[B354]). `username;base64(password)` — never hashed on the
   wire, in either the primary or secondary path; only TLS protects it. The persisted approval QUEUE, however, zeroes
   passwords before storage ([B353]) — so the exposure is the live invocation, not the `.bog`.

3. **Async dual-signature over Fox** ([B353]). Remote second-signature requests queue on the point-OWNING station (JACE or
   supervisor), default in-memory (lost on reboot) or opt-in `.bog`; the supervisor polls JACE queues over Fox; email
   notifies eligible approvers (wired, gated off by default).

4. **The license is checked per-write but from a cached boolean** ([B352] §352.5). Runtime feature/SMA expiry is not caught
   until a station restart re-evaluates it.

5. **Even structural edits are signed — except to the compliance artifacts** ([B354] §354.5 vs [B351]/[B355]). Deleting or
   renaming a secured point is signed + audited; yet purging that point's audit history ([B351]) or rewriting the legal
   certification ([B355]) is not. The asymmetry is internal and stark.

## 356.4 — Two §14 corrections issued in-focus

- [B355] → [B350]: the §11.100(c) certification text is NOT "baked" in the lexicon; the runtime source is the mutable
  `ESignAcknowledgement` property, and the lexicon is only its empty-fallback. [B350] carries a pointer.
- [B350] itself refuted the prior-corpus assumption that `signingService` (PKI) satisfied Part 11 — it does not; this module
  is the Part 11 mechanism.

## 356.5 — What is NOT resolved (honest boundary)

- **ES4-G1** (requires-execution): the exploitability of the two unsigned surfaces — audit purge ([B351]) and config-hub
  edits ([B355], incl. `ESignAcknowledgement`) — for a NON-super-user depends on the shipped RBAC role grants in a live
  `ProtectedDashboard` station. Both share one root cause (config-hub writes are not themselves signed) and one live probe.
  This is the only open gap; it needs a running station + a stock operator principal. Blocked-on-live-server.
- The method-body MECHANICS that were scrubbed were re-derived from bytecode where load-bearing; anything neither in
  bytecode nor a resource stayed `[INFER]` and is labeled as such in the source blocks.

## 356.6 — Consequence for a deployment

Out of the box the module is compliant (correct default certification, correct signed-write behavior). Its Part 11 posture
in the field, however, rests on OPERATIONAL controls the module does not provide: station RBAC that denies ordinary users
write access to `BSecuredDashboardConfiguration` and invoke access to `BHistoryMaintenance`, plus off-station audit-history
backups (since the trail is plaintext, `roll`-capable, and purgeable). An auditor should treat §11.10(a)/(b)/(d) and
§11.200 as satisfied by the module, and §11.10(e) + §11.100(c) as satisfied only if those external controls are in place
and verified.

---

## Self-verify

| # | Claim | Marker | Source |
|---|---|---|---|
| 1 | Module = TridiumPS type-substitution Part 11 layer, `tridium:eSignature` gate | `[CERT]` | [B350] |
| 2 | Signed-write pipeline fail-closed (throw+branch before write) | `[CERT]` | [B352] |
| 3 | Second signer distinct (self-approval blocked) + role-enforced | `[CERT]` | [B353] |
| 4 | Credential reversible Base64 end-to-end (btoa↔decode) | `[CERT]` | [B352]/[B354] |
| 5 | Reason mandatory but not set-validated | `[CERT]` | [B352] |
| 6 | `ESignAcknowledgement` mutable/unsigned; lexicon = fallback | `[CERT]` | [B355] |
| 7 | Audit trail plaintext, purgeable without signature | `[CERT]` | [B351] |
| 8 | Enforcement is the type, UI is a form | `[CERT]` | [B354] |
| 9 | STRONG-ceremony/WEAK-artifacts is the unifying thesis | `[INFER]` | synthesis of 1-8 |
| 10 | Field posture depends on external RBAC/backup controls | `[INFER]` | from 6,7 |

Marker tally (this block): `[CERT]` ×8 (each a pointer to a verified block) · `[INFER]` ×2 (the synthesis judgments). High
ratio EXPECTED for a synthesis block (§11) — it does not signal exhaustion; the focus is closed because every
investigable gap is closed, not because evidence ran out.

## Connections

- [Block 350] ES1 · [Block 351] ES4 · [Block 352] ES2 · [Block 353] ES3+ES5 · [Block 354] ES6 · [Block 355] ES7 — the six evidence blocks this synthesis consolidates.
- [Block 34] core history/alarm; [Block 324-334] the `email` service focus (approver-notify substrate); [Block 199]/[Block 204] webEditors/bajaux (the field-editor substrate); [Block 134] Fox (the dual-signature transport).

## Focus status

**electronicSignature CLOSED 7/7** (ES1-ES7). Blocks B350-B356 (7 total: 6 evidence + this synthesis). Read-only
investigable exhausted. One requires-execution gap remains: **ES4-G1** (live-permission reachability of the two unsigned
surfaces). Next: §18 self-retrospective, push, and FOCUSES.md mirror.
