# B475 — The Fox backup gate decoded: permission bit 48 = ADMIN_READ|ADMIN_WRITE on BackupService, so the .bog pull needs admin-write — a plain operator cannot do it (focus jace8000, J8-G3 closed from disk)

> **Focus:** `jace8000` (§16). **Gap:** J8-G3 — is the Fox backup pull ([Block 472]/[Block 473]) reachable by
> any authenticated user, or only by admins? (i.e. what is permission bit 48?)
> **Phase:** §12 — **answered DISK-FIRST from decompiled source**, no live user created (the live grant is
> conserved: disk evidence settles it, §12 "a grant is a ceiling"). Read-only.
> **Block type: EVIDENCE (source).**
> **Sources:** `[CERT]` `javax.baja.security.BPermissions` (baja, Tridium original source) · [Block 472]
> (the `has(48)` gate) · [Block 473] (admin passed live).
>
> **Bottom line:** bit 48 decodes to **`ADMIN_READ | ADMIN_WRITE`** on the `BackupService` component. The Fox
> backup channel calls `service.getPermissions(ctx).has(48)`, i.e. `(mask & 48) == 48` — the caller must hold
> **admin-write** on BackupService. An **operator-level** user (operator read/write/invoke only) is **denied**
> (`PermissionException`). So the no-Workbench `.bog` pull is **NOT available to any authenticated user** — it
> is **admin-write-gated**; `admin` (super user) passed live because it holds all permissions.

## §475.1 — The Niagara permission model (6 bits)

`[CERT]` `BPermissions.java` — permissions are a bitmask `[oRead][oWrite][oInvoke][aRead][aWrite][aInvoke]`
(`:24-30`) with these exact values (`:307-317`):

| Constant | Hex | Dec |
|---|---|---|
| `OPERATOR_READ` | 0x0001 | 1 |
| `OPERATOR_WRITE` | 0x0002 | 2 |
| `OPERATOR_INVOKE` | 0x0004 | 4 |
| (unused) | 0x0008 | 8 |
| `ADMIN_READ` | 0x0010 | 16 |
| `ADMIN_WRITE` | 0x0020 | 32 |
| `ADMIN_INVOKE` | 0x0040 | 64 |

Note the deliberate nibble split: operator bits in `0x0_`, admin bits in `0x_0`, with `0x08` unused. So
**48 = 0x30 = 0x10 | 0x20 = `ADMIN_READ | ADMIN_WRITE`.**

## §475.2 — What `has(48)` requires

`[CERT]` `has(int required) { return (mask & required) == required; }` (`BPermissions.java:132-135`). With
`required = 48`, the check passes only if **both** `ADMIN_READ` (0x10) and `ADMIN_WRITE` (0x20) are set in the
user's effective permission mask **on the BackupService component**.

The normalization rules make this collapse to a single requirement: `[CERT]` "`if ((mask & ADMIN_WRITE) != 0)
mask |= (OPERATOR_READ | OPERATOR_WRITE | ADMIN_READ)`" (`:55`) — **holding `ADMIN_WRITE` auto-adds
`ADMIN_READ`**. So the effective gate is simply: **the user must have `ADMIN_WRITE` on `BackupService`.**

## §475.3 — Who can, who cannot

- **Super user / admin** (all permissions, `allPermissions`, `:199`) → has ADMIN_WRITE → **can** pull the
  backup. This is why the `admin` account succeeded live ([Block 473]).
- **Operator role** (only `OPERATOR_READ/WRITE/INVOKE` on the service's category) → **no** ADMIN_WRITE →
  `has(48)` false → `service.backup` throws `PermissionException` ([Block 472] §472.3) → **cannot** pull the
  backup.
- **Read-only admin** (`ADMIN_READ` but not `ADMIN_WRITE`) → 0x10, not 0x30 → **cannot**.

Niagara permissions are per-component via the user's role → category assignments, so a non-super account could
only pull a backup if it were explicitly granted **admin-write on the BackupService's category** — an
unusual, deliberate grant, not a default.

## §475.4 — Security consequence for J8

The no-Workbench `.bog` route ([Block 464]/[Block 473]) is **doubly gated**, not merely login-gated: (1) a
valid station login (SCRAM, [Block 471]) AND (2) **admin-write permission on BackupService** (this block).
Combined with the at-rest encryption ([Block 466]), the exposure is: a leaked **admin/super** credential lets
an attacker pull the engineering `config.bog` over Fox with no Workbench — but a leaked **operator** credential
does **not**, and even a pulled `.bog`'s secrets stay encrypted. This sharpens the operator's risk model: the
`admin` credential is the crown jewel (hence the standing "rotate it" action); operator accounts do not open
the backup channel.

## Self-verify

| # | Claim | Marker | Citation | Checked |
|---|---|---|---|---|
| 1 | permission bits: ADMIN_READ=0x10, ADMIN_WRITE=0x20, ADMIN_INVOKE=0x40 | [CERT] | BPermissions.java:307-317 | ✓ |
| 2 | 48 = 0x30 = ADMIN_READ \| ADMIN_WRITE | [CERT] | arithmetic on :313,:315 | ✓ |
| 3 | has(48) = (mask & 48)==48 (both bits required) | [CERT] | BPermissions.java:132-135 | ✓ |
| 4 | ADMIN_WRITE normalizes to also imply ADMIN_READ | [CERT] | BPermissions.java:55 | ✓ |
| 5 | backup gate = getPermissions().has(48) | [CERT] | [Block 472] §472.3 | ✓ |
| 6 | admin (all perms) passed live; operator would be denied | [CERT]/[INFER] | [Block 473] + model | ✓ (operator = reasoned from model) |

Marker tally: [CERT] ×5 · [INFER] ×1 (the operator-denied conclusion, a direct deduction from the decoded
model — explicitly labeled). **Block type: EVIDENCE (source).** Ratio ≈ 0. DISK-FIRST: no live write spent.

## Connections

- **[Block 472]/[Block 473]** — the backup gate and the live pull this decodes. **[Block 466]** — the at-rest
  encryption that compounds this gate. Cross-corpus: the BPermissions model also underlies the RBAC findings
  of the `chihuahua`/`security-audit` focuses.

## Open gaps

**J8-G3 CLOSED** (admin-write-gated, from disk — no live user created). Remaining child gaps are
hardware/serial-only: J3-G1 (platform handshake bytes), J5-G1 (per-file /file ACL), J7-G1 (JACE-8000 Alternate
Boot menu capture), J2-G1 (QNX mount table). All need physical Debug-port access. **The J8 thread and the
network-reachable investigable set are exhausted.**
