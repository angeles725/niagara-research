# Block 355 — `electronicSignature`: the §11.100(c) certification the signer swears to is a MUTABLE, unsigned, unaudited station property (`ESignAcknowledgement`) — the lexicon text is only its empty-fallback

> Focus **electronicSignature** — gap **ES7** (mutable `ESignAcknowledgement` vs baked lexicon). READ-ONLY. Corpus language: ENGLISH.
>
> Question (opened in [B350] §350.7): the §11.100(c) legally-binding certification statement — can the deployed text
> DIVERGE from the compliant declaration? Answer: YES. The active source of the certification is a plain `String` property
> `ESignAcknowledgement` on `BSecuredDashboardConfiguration`, flag **0** (fully editable), whose out-of-box default IS the
> compliant §11.100(c) text — but which any config-writer can replace with arbitrary text, silently and without a
> signature. The lexicon `certificate.message.*` fragments ([B350] cited them) are only the EMPTY-fallback, used when the
> property is blanked.
>
> Sources (primary, N4.14, vendor **TridiumPS**), root `.../electronicSignature-rt/`:
> - `vineflower/com/tridiumx/ps/model/BSecuredDashboardConfiguration.java:169-172,282,311-314,510-515,1103-1120` — the property, its class, and the `getLegalText` assembly (STRUCTURE).
> - `extracted/com/tridiumx/ps/model/BSecuredDashboardConfiguration.class` — **bytecode**: the default legal text string (token-verified by the driver).
> - `electronicSignature-ux.lexicon` `certificate.message.{first,second,third}String` — the fallback fragments ([B350] §350.6).
>
> Markers: `[CERT]` local primary (`file:line`, bytecode string) · `[INFER]` deduction.
> Layer 22 (license/security) + compliance axis. Block TYPE: **evidence** (security). Closes the last investigable gap of the focus; builds on [B350] (the lexicon text), [B354] (the `#legaltext` DOM slot this property feeds).
>
> ⚠ **OBFUSCATION CAVEAT ([B350] header).** Decompiled `.java` is string-scrubbed; the default certification text below was
> re-confirmed against the `.class` bytecode string pool, NOT trusted from the decompiled annotation.

---

## 355.1 — `ESignAcknowledgement`: a mutable String property whose default is the §11.100(c) text

`BSecuredDashboardConfiguration` (the module's config hub, `extends BComponent` `[CERT]` `:282`) declares a property
`ESignAcknowledgement` `[CERT]` (`:311-314`):
```java
public static final Property ESignAcknowledgement = newProperty(
   0,                                     // FLAG 0 — not readonly, not hidden, not operator-restricted
   " I hereby certify that (i) I am the user [username] identified above, (ii) [customername] notified me that it had
     assigned the above user account to me and provided me access to that account, and (iii) it is my intent that the
     above electronic signature be my legally binding signature just as if it were my handwritten signature.",
   BFacets.make("multiLine", BBoolean.TRUE));
```
with standard `getESignAcknowledgement()`/`setESignAcknowledgement(String)` accessors `[CERT]` (`:510-515`). The default
value is the verbatim §11.100(c) certification — confirmed in the **bytecode** string pool `[CERT]` (`strings
BSecuredDashboardConfiguration.class`: *"I hereby certify that (i) I am the user [username] identified above … my legally
binding signature just as if it were my handwritten signature."*), so it is a real compiled default, not a decompiler
artifact. Out of the box, the declaration is compliant.

The load-bearing fact is the **flag 0**: this is an ordinary read/write String slot. It is not `READONLY`, not `HIDDEN`,
carries no `*WithAuthentication` gate. Anyone who can write `BSecuredDashboardConfiguration` can call
`setESignAcknowledgement("anything")`.

## 355.2 — The property is the ACTIVE source; the lexicon is only the empty-fallback

The signing dialog's `#legaltext` ([B354] §354.2) is populated by a server call `getLegalText`, whose handler makes the
precedence explicit `[CERT]` (`BSecuredDashboardConfiguration.java:1103-1120`):
```java
} else if (values[0].equals("getLegalText")) {
   String acknowledgement = getESignature().getESignAcknowledgement()    // the PROPERTY, first
       .replace("[username]", "[" + userName + "]")
       .replace("[customername]", "[" + customerName + "]");
   if (acknowledgement.length() == 0) {                                  // ONLY if the property is empty
       acknowledgement = getLexicon().getText("certificate.message.firstString") + userName
           + getLexicon().getText("certificate.message.secondString") + customerName
           + getLexicon().getText("certificate.message.thirdString");
   }
   result.add("legalText", BString.make(acknowledgement));
```
So the text the signer reads and swears to is the PROPERTY value with `[username]`/`[customername]` token substitution; the
baked lexicon fragments are a fallback reached ONLY when the property is blanked. [B350] §350.6 cited the lexicon as the
"baked" certification — this block corrects the framing (§14): the lexicon is NOT the authoritative runtime source; the
mutable property is, and the lexicon is its default-if-empty. The `#` -commented full string seen in the ux lexicon is a
reference copy, not the active text.

## 355.3 — Editing the declaration is UNSIGNED and UNAUDITED

`BSecuredDashboardConfiguration extends BComponent` — it is NOT a `BSecured*Writable` `[CERT]` (`:282`). Therefore changing
`ESignAcknowledgement` is an ORDINARY Niagara config write: no `*WithAuthentication` verb ([B354] §354.3 applies only to the
secured points), no re-authentication ([B352]), no `BReasons`, and no `BSecuredTrendRecord` entry ([B351]) — the secured
history records point WRITES, not edits to the config hub. Consequence `[INFER, from the flag-0 property + non-secured
class]`: the legally-binding declaration that governs the meaning of EVERY signature on the station can be silently
rewritten by any user with write access to the config hub, with nothing in the Part 11 audit trail to show it changed.
An attacker (or a careless integrator) could:
- weaken the wording (delete "legally binding" / the intent clause) so signatures are no longer an admission;
- substitute misleading text so signers certify something other than what they believe;
- blank it, silently reverting to the lexicon default (benign, but still an unlogged change of the compliance artifact).

## 355.4 — The capstone of the front-strong/back-weak pattern

ES7 completes the thesis running through this focus: **the module bolts the signing CEREMONY and leaves the COMPLIANCE
ARTIFACTS as ordinary, unsigned config.**

| Part 11 clause | Mechanism | Enforcement | Block |
|---|---|---|---|
| §11.10 / §11.200(a) signed write | `*WithAuthentication` + re-auth + fail-closed | STRONG (server + type) | [B352] |
| §11.200(b) second signer | distinct-signer + role, self-approval blocked | STRONG (server-enforced) | [B353] |
| §11.50(a)(3) signature meaning (reason) | reason mandatory | WEAK — any non-empty string, not from the set | [B352] §352.4 |
| §11.100(c) binding certification | `ESignAcknowledgement` property | **WEAK — mutable, unsigned, unaudited** | **this block** |
| §11.10(e) protected audit trail | `BSecuredTrendRecord` history | WEAK — plaintext, purgeable without a signature | [B351] |

The front door (who may write, and how they authenticate) is rigorous; the artifacts that give a signature its legal force
and durability (the certification text, the reason vocabulary, the audit record) are ordinary station data an admin can
change or clear without signing. For an auditor, the module's §11.10(a)/(b)/(d)/§11.200 posture is defensible; its
§11.10(e) and §11.100(c) posture depends entirely on operational controls OUTSIDE the module (station RBAC, backups),
which the module neither provides nor enforces.

## 355.5 — Scope note: the default is compliant

This is a DESIGN-surface finding, not an out-of-box defect: a freshly deployed module ships the correct §11.100(c) text and
the compliant behavior. The gap is the ABSENCE of protection on the artifact, not a wrong default. Whether a stock RBAC
role actually grants write on `BSecuredDashboardConfiguration` to a non-super-user is the same live-permission question as
[B351]'s ES4-G1 (requires-execution) — the two share a root cause: config-hub writes are not themselves signed.

---

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | `ESignAcknowledgement` = String property, flag 0, multiLine, on BSecuredDashboardConfiguration | `[CERT]` | `BSecuredDashboardConfiguration.java:169-172,311-314` | ✅ read |
| 2 | Default value = verbatim §11.100(c) text (bytecode, not decompiler artifact) | `[CERT]` | `strings BSecuredDashboardConfiguration.class` | ✅ ran |
| 3 | get/setESignAcknowledgement accessors present | `[CERT]` | `:510-515` | ✅ read |
| 4 | `getLegalText` returns the PROPERTY (token-substituted); lexicon used ONLY if property empty | `[CERT]` | `:1103-1120` | ✅ read |
| 5 | BSecuredDashboardConfiguration extends BComponent (not a BSecured*Writable) | `[CERT]` | `:282` | ✅ read |
| 6 | Editing the property is unsigned/unaudited (ordinary config write) | `[INFER]` | from claims 1,5 + [B352]/[B351] | ⚠ deduction |
| 7 | Declaration can be weakened/substituted/blanked silently | `[INFER]` | from claims 1,4,6 | ⚠ deduction |
| 8 | §14 correction: lexicon is fallback, not the authoritative runtime source | `[CERT]` | `:1109-1116` (property-first, lexicon-if-empty) | ✅ read |

Marker tally: `[CERT]` ×6 · `[INFER]` ×2. [INFER]/[CERT] = 2/6 = 0.33 — healthy evidence block; the mechanism (mutable
property + property-first assembly + non-secured class) is `[CERT]`, the `[INFER]`s are the compliance consequences.

§14 CORRECTION to [B350] §350.6: [B350] framed the lexicon `certificate.message.*` as the "baked" §11.100(c) certification.
This block establishes the RUNTIME source is the mutable `ESignAcknowledgement` property; the lexicon is only its
empty-fallback. [B350]'s citation of the lexicon text is still valid as the DEFAULT wording, but "baked/immutable" was
wrong — corrected here and flagged in [B350] via a pointer.

## Connections

- [Block 350] — ES1: cited the §11.100(c) lexicon text (now understood as the property default/fallback, §14 above).
- [Block 354] — ES6: the `#legaltext` dialog slot this property feeds via `getLegalText`.
- [Block 351] — ES4 and [Block 352] — ES2: the other unprotected compliance artifacts; §355.4 consolidates the pattern.
- Core: `BComponent` config write semantics — an ordinary slot write, ungated by the module.

## Open gaps after this block

- **Investigable gaps: 0.** Every read-only-investigable gap (ES1-ES7) is closed. The focus has reached investigable
  exhaustion (§8) → focus-close SYNTHESIS + §18 retro + push.
- **ES4-G1** (requires-execution): live-permission reachability of both the audit purge ([B351]) AND the config-hub edits
  ([B355]) for a non-super-user RBAC role — one shared live probe. Remains blocked-on-live-server.
