# Block 558 — N4 password-policy enforcement IS built-in: `BPasswordStrength` (an `IPropertyValidator`) wired through `BPasswordAuthenticator.checkPassword()` — correcting Block 11 §11.3.5 ("complexity NO built-in")

**Session**: 2026-08-28
**Focus**: `access-control` (gap AC1 — the password-policy enforcement chain; the first block of the new RBAC focus)
**Distribution**: Honeywell OptimizerSupervisor-N4.14.0.162
**Method**: READ-ONLY, inline bounded read of the validator + the authenticator caller chain; the enforcement
loop token-verified inline. Original Tridium javadoc (`docSource`) used for the type contract.
**Primary sources** `[CERT]`:
- `organized/baja/baja/vineflower/javax/baja/user/BPasswordStrength.java` (380 lines) +
  `organized/docSource/docSource-doc/extracted/baja/javax/baja/user/BPasswordStrength.java` (javadoc).
- `organized/baja/baja/vineflower/javax/baja/security/BPasswordAuthenticator.java` (checkPassword, :173-225).
- `organized/baja/baja/vineflower/com/tridium/user/BGlobalPasswordConfiguration.java` (:25-68) +
  `BUserPasswordConfiguration.java`.

**Scope**: [Block 11] §11.3.5 (line 313) states *"complexity enforcement NO built-in … Custom policy requiere
module custom con validation regex"* and repeats it at line 398 (*"Password complexity NO enforced nativo"*).
That is **FALSE** for N4.14 (and has been since Niagara AX 3.8). This block traces the built-in enforcement
chain end-to-end and issues the §14 correction. It does NOT re-open the RBAC concept model ([Block 11]) or the
auth-scheme SPI ([Block 510]) — REMITTANCE.

---

## 558.1 `BPasswordStrength` — a built-in `IPropertyValidator` with configurable complexity knobs [CERT]

`public class BPasswordStrength implements IPropertyValidator` `[CERT-doc]` (docSource header, `@author Bill
Smith`, `@since Niagara AX 3.8`). It is a first-class Baja type with six `@NiagaraProperty` knobs `[CERT]`:

| Property | Meaning | Note |
|----------|---------|------|
| `minimumLength` | min chars | `@since` AX 3.8 |
| `maximumLength` | max chars | **`@since Niagara 4.13`** (newer) |
| `minimumLowerCase` | min lower-case letters | |
| `minimumUpperCase` | min upper-case letters | |
| `minimumDigits` | min digits | |
| `minimumSpecial` | min non-alphanumeric | |

Three built-in presets `[CERT]` (`BPasswordStrength.java:88-91`):
- `DEFAULT = new BPasswordStrength(PasswordStrength.DEFAULT)`
- `FIPS_1 = new BPasswordStrength(PasswordStrength.FIPS_1)`
- `OFF = new BPasswordStrength(0, 0, 0, 0, 0, Integer.MAX_VALUE)` — **all minimums 0 ⇒ complexity disabled**.

So "off" is a *configured state*, not the absence of a feature. The default install ships `DEFAULT`, not `OFF`.

## 558.2 The candidate-password evaluator: `isPasswordValid(char[], Consumer)` [CERT]

`BPasswordStrength.isPasswordValid(char[] password, Consumer<Localizable> messageConsumer)` `[CERT] :204-243` is
the real check. It counts character classes and compares against the configured minimums:
```java
for (char character : password) {
   if (Character.isLetter(character)) {
      if (Character.isUpperCase(character)) upperCase++; else lowerCase++;
   } else if (Character.isDigit(character)) digits++;
   else special++;
}
if (len >= getMinimumLength() && len <= getMaximumLength()
    && digits >= getMinimumDigits() && lowerCase >= getMinimumLowerCase()
    && upperCase >= getMinimumUpperCase() && special >= getMinimumSpecial())
   return true;
else { /* build localized error list "user.password.notStrong" + requirements */ return false; }
```
A `String` overload `isPasswordValid(String) throws` `[CERT] :245-250` wraps it and throws a `BajaException`
carrying the localized unmet-requirements message. This is a real, non-regex, character-class enforcement — NOT
"custom module with regex validation".

## 558.3 The wiring: `BPasswordAuthenticator.checkPassword()` invokes it and REJECTS weak passwords [CERT]

The enforcement is not orphaned — it is called on every password change. `BPasswordAuthenticator` holds a
per-user `passwordConfig` (`BUserPasswordConfiguration`) property `[CERT] :57-74`. Its instance
`checkPassword(BPassword newPassword, Context)` `[CERT] :173-181` delegates to the static
`checkPassword(user, scheme, config, newPassword, context)` `[CERT] :186-225`:
```java
BPasswordStrength strength = scheme.getGlobalPasswordConfiguration().getPasswordStrength();   // :189
...
if (!strength.isPasswordValid(passChars.get(), messageRef::set))                              // :195
   throw ...                                                                                  // weak → REJECTED
...
config.changeIntervalCheck(scheme.getGlobalPasswordConfiguration());                          // :219 (history/age)
```
So a weak new password **throws and is rejected** at set-time. The policy source is the STATION-GLOBAL
`BGlobalPasswordConfiguration.passwordStrength` (`[CERT] :25-68`, default `BPasswordStrength.DEFAULT`, facet
`security=true`), read via `scheme.getGlobalPasswordConfiguration()`. `BPasswordStrength.validateSet(...)`
`[CERT] :253-345` additionally guards the CONFIG itself (you cannot set `minimumLength` below
`MINIMUM_ALLOWED_LENGTH`, cannot set incoherent min/max) — a second built-in guardrail on the policy, not the
password.

## 558.4 Per-user layer: `BUserPasswordConfiguration` [CERT]

Beyond global complexity, each `BPasswordAuthenticator` carries `BUserPasswordConfiguration` `[CERT]
BPasswordAuthenticator.java:107,246`, which contributes:
- `forceResetAtNextLogin` (`getForceResetAtNextLogin()` `:246`),
- password **history**/expiration interval, enforced via `config.changeIntervalCheck(...)` `:219`.

So N4's built-in password governance is TWO layers: station-global complexity (`BGlobalPasswordConfiguration` →
`BPasswordStrength`) + per-user lifecycle (`BUserPasswordConfiguration`: force-reset, history, expiration). Both
are native; neither needs a custom module.

## 558.5 §14 correction to [Block 11] [CERT]

[Block 11] §11.3.5 (line 313) and its summary (line 398) claim N4 has **no built-in password-complexity
enforcement** and that a custom regex module is required. **CORRECTED**: complexity enforcement is built-in
(`BPasswordStrength`, since AX 3.8), configurable (6 knobs + `maximumLength` since 4.13), preset-driven
(`DEFAULT`/`FIPS_1`/`OFF`), and actively enforced at password-set time by `BPasswordAuthenticator.checkPassword`
→ `BPasswordStrength.isPasswordValid`. The only true statement nearby is that the policy CAN be turned off
(`OFF` preset, all-zero minimums) — which is a configuration, and is NOT the default. [Block 11] gets a
back-pointer. What [Block 11] got right and stays: LDAP/AD delegation still inherits the directory's policy
(external schemes bypass `BPasswordStrength` because they do not use `BPasswordAuthenticator`).

## 558.6 Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|-------|--------|----------|---------|
| 1 | BPasswordStrength implements IPropertyValidator; 6 knobs incl. maximumLength @since 4.13; since AX 3.8 | [CERT-doc]/[CERT] | docSource BPasswordStrength.java:23,45-95; vineflower :97-99 | token-checked ✓ |
| 2 | Presets DEFAULT/FIPS_1/OFF; OFF = (0,0,0,0,0,MAX) disables complexity | [CERT] | BPasswordStrength.java:88-91 | token-checked ✓ |
| 3 | isPasswordValid(char[]) counts classes, compares to configured minimums, returns false→localized error | [CERT] | :204-243 | token-checked ✓ |
| 4 | checkPassword reads global passwordStrength and throws if !isPasswordValid (weak rejected at set-time) | [CERT] | BPasswordAuthenticator.java:186-195 | token-checked ✓ |
| 5 | Policy source = station-global BGlobalPasswordConfiguration.passwordStrength (default DEFAULT) | [CERT] | BGlobalPasswordConfiguration.java:25-68 | token-checked ✓ |
| 6 | Per-user BUserPasswordConfiguration adds forceReset + history via changeIntervalCheck | [CERT] | BPasswordAuthenticator.java:107,219,246 | token-checked ✓ |
| 7 | B11 §11.3.5 "complexity NO built-in / custom regex required" is FALSE → §14 correction | [CERT] | B11:313,398 vs claims 1-4 | logic-checked |
| 8 | LDAP/AD delegation still inherits directory policy (external schemes bypass BPasswordStrength) | [INFER] | scheme ≠ BPasswordAuthenticator path | reasoned |

**Marker tally**: [CERT] ×6 · [CERT-doc] ×1 (row 1 shared) · [INFER] ×1. Block TYPE = EVIDENCE (decompilation +
§14 correction). 6 of 8 rows token-verified inline (the enforcement chain end-to-end).

## Connections

- **[Block 11]** §11.3.5 — CORRECTED + back-pointed: password-complexity enforcement IS built-in.
- **[Block 510]** — the auth-scheme SPI: `BPasswordAuthenticationScheme` is the concrete scheme whose
  `getGlobalPasswordConfiguration()` feeds the policy read here.
- **[Block 457]** — SCRAM login: the credential this policy governs is what SCRAM later verifies.
- **[Block 398]/[Block 490]** — hardening checklist: password policy default = `DEFAULT` (not `OFF`) is a
  posture item; an install set to `OFF` is a finding.

## Open gaps (this block)

- The exact numeric values of `PasswordStrength.DEFAULT` / `FIPS_1` live in `com.tridium.user.PasswordStrength`
  (not opened here; the enum backing the presets) — low value, open on demand.
- `changeIntervalCheck` / password-history depth internals are named, not decompiled → folds into AC-audit or a
  BUserPasswordConfiguration child gap. Focus continues at AC2.
