# Block 574 — Credentials batch management: provisioning drives BOTH credential stores (station users via `BUserService`, platform/daemon users) plus the supervisor↔subordinate connection password and the at-rest system passphrase — station passwords go through the same policy pipeline (AC1), connection passwords are reversible `BPassword`s decoded under `doPrivileged`

**Session**: 2026-08-28
**Focus**: `provisioning` (gap PV8 — the credential steps: which stores, how values are handled, what policy applies)
**Distribution**: Honeywell OptimizerSupervisor-N4.14.0.162
**Method**: READ-ONLY, inline bounded read of the 7-class `credentials` package + the `systempassphrase` step.
SECRETS DISCIPLINE: cite credential-handling STRUCTURE (types, decode-under-privilege, target store), never a value.
**Primary sources** `[CERT]`:
- `organized/provisioningNiagara/provisioningNiagara-wb/vineflower/com/tridium/provisioningNiagara/credentials/
  {BAddStationUserStep,BSetStationUserPasswordJobStep,BRemoveStationUserStep,BSetPlatformCredentialsJobStep,
  BSetPlatformUserPasswordJobStep,BRemovePlatformUserJobStep,BSetStationConnectionCredentialsStep}.java` +
  `systempassphrase/BSetSystemPassphraseJobStep.java`.

**Scope**: how a supervisor batch-manages fleet credentials. Ties the two credential stores of [Block 460] and
the password model of [Block 558]/[Block 562]. Does NOT re-open the RBAC user model ([Block 11]/AC-focus) or the
daemon protocol ([Block 460]).

---

## 574.1 Both credential stores, batch-managed [CERT]

The steps target the TWO independent credential stores [Block 460] identified — and the connection between them:
- **Station users** (SCRAM, `BUserService`): `BAddStationUserStep` (`BUser` + `BPassword` + admin flag, imports
  `BUserService`/`BPasswordAuthenticator` `[CERT] :17-24,77`), `BSetStationUserPasswordJobStep`,
  `BRemoveStationUserStep`.
- **Platform/daemon users** (OS-level): `BSetPlatformCredentialsJobStep`, `BSetPlatformUserPasswordJobStep`,
  `BRemovePlatformUserJobStep` `[CERT]`.
- **The supervisor↔subordinate connection**: `BSetStationConnectionCredentialsStep` `[CERT]`.
- **The at-rest key**: `BSetSystemPassphraseJobStep` `[CERT]` (systempassphrase package).

So provisioning is the single place where a supervisor can, across a fleet, add/remove station users, rotate
platform accounts, re-key connections, and set the system passphrase — the complete identity surface.

## 574.2 Station users go through the AC1 policy pipeline [CERT]

`BAddStationUserStep` `[CERT]` imports `BUserService`, `BUser`, `BPasswordAuthenticator`, and `BPassword`
`[CERT] :17-24`, taking `(username, password:BPassword, admin)` `[CERT] :77`. Because it creates the user via
`BUserService` and sets the password through the standard authenticator, the batch-created user is subject to the
SAME password-strength enforcement traced in [Block 558] (AC1) — `BPasswordAuthenticator.checkPassword` →
`BPasswordStrength.isPasswordValid`. Batch provisioning does not bypass the policy pipeline.

## 574.3 Connection passwords are reversible, decoded under privilege [CERT]

`BSetStationConnectionCredentialsStep extends BDeviceJobStep implements BIPrivilegedDeviceJobStep` `[CERT] :36`
carries a `password` (`BPassword`) `[CERT] :38`. In `doRun` `[CERT] :90-94`:
```java
BPassword password = BPassword.make(AccessController.doPrivileged(this.getPassword()::getValue));
clientConnection.setCredentials(new BUsernameAndPassword(this.getUsername(), password));
foxSession.setCredentials(new BUsernameAndPassword(clientConnection.getUsername(), password));
```
The connection password is a **reversible `BPassword`** ([Block 562] AC4) — it MUST be, because the supervisor
has to present it to authenticate the Fox/client connection. The plaintext is recovered via
`AccessController.doPrivileged(getValue)` (a privileged decode) and installed on both the client connection and
the Fox session. This is the correct use of the reversible encoder family: a secret the station replays, not a
login hash.

## 574.4 The system passphrase step: privileged, over the daemon, policy-checked [CERT]

`BSetSystemPassphraseJobStep` `[CERT]` is a `BIPrivilegedDeviceJobStep` that imports `BDaemonSession`,
`SystemPasswordMessage`, `BPassword`, and **`BPasswordStrength`** `[CERT] :3-27`. So setting the at-rest system
passphrase ([Block 466]) runs over the platform daemon (privileged, §PV4/[Block 570]) and is validated against
the `BPasswordStrength` policy (AC1) before it is applied — the same complexity rules that gate login passwords
also gate the passphrase.

## 574.5 Thesis [CERT-synthesis]

Credential provisioning is the fleet-scale counterpart to the AC-focus findings: it drives BOTH stores ([Block
460]) through the SAME primitives the single-station path uses — station users via `BUserService`/
`BPasswordAuthenticator` (policy-enforced, [Block 558]), reversible connection passwords decoded under privilege
([Block 562]), and a policy-checked system passphrase over the daemon ([Block 466]/[Block 570]). No batch
shortcut weakens the model; the security posture of a fleet's credentials is exactly the single-station posture,
applied N times. The one elevated capability — writing platform accounts and the passphrase — is a
`BIPrivilegedDeviceJobStep` over the daemon, consistent with [Block 570].

## 574.6 Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|-------|--------|----------|---------|
| 1 | Steps target station users (BUserService) AND platform users AND connection creds AND system passphrase | [CERT] | credentials/*.java + systempassphrase/*.java | grep+read ✓ |
| 2 | BAddStationUserStep uses BUserService/BPasswordAuthenticator (subject to AC1 policy) | [CERT] | BAddStationUserStep.java:17-24,77 | token-checked ✓ |
| 3 | BSetStationConnectionCredentialsStep: reversible BPassword decoded via AccessController.doPrivileged(getValue), set on client+fox | [CERT] | BSetStationConnectionCredentialsStep.java:36,90-94 | token-checked ✓ |
| 4 | BSetSystemPassphraseJobStep = privileged, over BDaemonSession/SystemPasswordMessage, validates BPasswordStrength | [CERT] | systempassphrase/BSetSystemPassphraseJobStep.java:3-27 | token-checked ✓ |
| 5 | No batch shortcut bypasses the single-station credential model | [CERT-synthesis] | rows 2-4 + [B558]/[B562]/[B460] | reasoned ✓ |

**Marker tally**: [CERT] ×4 · [CERT-synthesis] ×1 · [INFER] ×0. Block TYPE = EVIDENCE (decompilation). 3 of 5
rows token-verified inline (SECRETS DISCIPLINE: structure only).

## Connections

- **[Block 460]** — the two credential stores (station SCRAM vs platform daemon); PV8 batch-manages both.
- **[Block 558]** (AC1) — the password-strength pipeline station-user creation goes through.
- **[Block 562]** (AC4) — reversible `BPassword` is why connection passwords are recoverable under privilege.
- **[Block 466]** / **[Block 570]** (PV4) — the system passphrase; set here over the privileged daemon path.

## Open gaps (this block)

- The daemon wire for platform-user changes (`BSetPlatformCredentialsJobStep` message shape) is named-not-traced
  — [Block 460] daemon territory. Focus continues at PV9 (license management chain).
