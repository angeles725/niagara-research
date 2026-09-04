# B755 · The bit models a module author actually works with — slot Flags, BStatus, BPermissions, and the BVersion relation bits (exact values, code-verified)

> **Scope**: the four bit-field models that recur across module authoring, each with its EXACT constant values
> read from source, which bits you set daily, and how they combine. Requested consolidation ("check the bits
> you work with / the important ones"). This is the single reference for: slot `Flags` (what shows/links/
> persists), `BStatus` (the 8 condition bits), `BPermissions` (the RBAC bits our servlets gate on), and the
> `BVersion` relation bit-flags (dependency satisfaction). Foco: **module-authoring** (cross-cutting reference).
>
> **Sources**: FUENTE 3 — `organized/baja/baja/{vineflower,decompiled}/` : `javax/baja/sys/Flags.java`,
> `javax/baja/status/BStatus.java`, `javax/baja/security/BPermissions.java`,
> `organized/platform/platform-rt/vineflower/com/tridium/install/BVersion.java`. All values verbatim with
> file:line. FUENTE 1 — B735 (flags in the link picker/wire sheet), B736 (BStatus model), B708/B752 (RBAC),
> B754 (versioning). NEW here = one place, exact bits, combination rules.

---

## 755.1 — Slot `Flags` — a 32-bit field on every slot `[CERT, Flags.java:23-44]`
Set via `@NiagaraProperty(flags=…)` / `newProperty(flags,…)`; each has an integer, a one-letter code (as
shown in Workbench), and a name. **★ = the ones you set daily.**

| Bit (dec / hex) | Code | Name | Meaning |
|---|---|---|---|
| **1** / 0x1 | `r` | **READONLY ★** | value not user-editable (pair with TRANSIENT for computed outputs) |
| **2** / 0x2 | `t` | **TRANSIENT ★** | NOT persisted to the `.bog` (runtime-only state) |
| **4** / 0x4 | `h` | **HIDDEN ★** | removed from property sheet, link picker, AND nav |
| **8** / 0x8 | `s` | **SUMMARY ★** | the wire-sheet PIN set (B735/B747: pin = summary, exactly) |
| **16** / 0x10 | `a` | **ASYNC ★** | action runs off the calling thread (timer/IO callbacks) |
| 32 / 0x20 | `n` | NO_RUN | not executed by the engine |
| **64** / 0x40 | `d` | **DEFAULT_ON_CLONE ★** | reset to default when the component is cloned (avoids stale calc state) |
| 128 / 0x80 | `c` | CONFIRM_REQUIRED | UI confirmation before invoking an action |
| **256** / 0x100 | `o` | **OPERATOR ★** | operator-writable (vs admin) — pairs with the RBAC bits below |
| 512 / 0x200 | `x` | EXECUTE_ON_CHANGE | re-execute when this slot changes |
| **1024** / 0x400 | `f` | **FAN_IN ★** | property may receive MULTIPLE inbound links |
| 2048 / 0x800 | `A` | NO_AUDIT | writes not written to the audit log |
| 4096 / 0x1000 | `p` | COMPOSITE | slot participates in composite/proxy |
| 8192 / 0x2000 | `R` | REMOVE_ON_CLONE | slot removed when cloned |
| 16384 / 0x4000 | `m` | METADATA | metadata slot |
| 32768 / 0x8000 | `L` | LINK_TARGET | eligible link target marker |
| 65536 / 0x10000 | `N` | NON_CRITICAL | e.g. a link flagged non-critical (won't fault the target) |
| 0x10000000‑0x80000000 | `1`..`4` | USER_DEFINED1‑4 | vendor-private flags (top 4 bits) |

Combine by OR: a live computed output = `TRANSIENT|SUMMARY|READONLY` (2|8|1 = 11); an operator tunable =
`SUMMARY|OPERATOR` (8|256 = 264); a hidden timer action = `HIDDEN|ASYNC` (4|16 = 20). `Flags.isSummary(comp,
slot)` / `isHidden(...)` test one bit; `Flags.encodeToString` renders the letter codes (`f=…` in the `.bog`).

## 755.2 — `BStatus` — an 8-bit condition field on every `BStatusValue` `[CERT, BStatus.java:46-53]`
The status carried alongside every point/output value (B736). Bits OR together — a value can be both FAULT and
OVERRIDDEN.

| Bit (dec / hex) | Name | Meaning |
|---|---|---|
| 1 / 0x01 | DISABLED | slot administratively disabled |
| 2 / 0x02 | FAULT | a fault condition (bad config / sensor fault) |
| 4 / 0x04 | DOWN | comms down to the source device |
| 8 / 0x08 | ALARM | in an alarm (offnormal) state |
| 16 / 0x10 | STALE | value older than its tuning allows |
| 32 / 0x20 | OVERRIDDEN | value forced by an override/HOA |
| 64 / 0x40 | NULL | no valid value |
| 128 / 0x80 | UNACKED_ALARM | an unacknowledged alarm |
| **0** | (ok) | `BStatus.ok` — no bits set |

Author rule (B736/B730): gate control on `getStatus().isValid()` (validity = none of FAULT/DOWN/STALE/NULL
set), set NULL/OVERRIDDEN honestly, and PROPAGATE the aggregate of input statuses to the output — never
collapse a bad input to a fake `ok` reading.

## 755.3 — `BPermissions` — the RBAC bits our servlets gate on `[CERT, BPermissions.java:26-31]`
A user's permission for a component category is a bit-set. Two tiers (operator / admin) × three verbs
(read / write / invoke).

| Bit (dec / hex) | Name | Grants |
|---|---|---|
| 1 / 0x01 | OPERATOR_READ | read as an operator |
| **2** / 0x02 | **OPERATOR_WRITE** | write a value as an operator ← DashboardPan/chihuahua gate on THIS bit |
| 4 / 0x04 | OPERATOR_INVOKE | invoke an operator action |
| 16 / 0x10 | ADMIN_READ | read config/admin |
| 32 / 0x20 | ADMIN_WRITE | write config/admin |
| 64 / 0x40 | ADMIN_INVOKE | invoke an admin action |

(Bits 8 and 128 are unused gaps between the operator and admin nibbles.) The **`OPERATOR` slot flag** (§755.1,
bit 256) marks a slot as operator-level; the framework then checks the user's `OPERATOR_WRITE` bit for that
component's category before allowing the write. Our RBAC helpers test the BIT
(`checkCanWrite → BPermissions.OPERATOR_WRITE`), never a role NAME (B752 §752.5) — the correct, forgeable-name-proof way.

## 755.4 — `BVersion` relation bits — how a dependency is judged satisfied `[CERT, BVersion.java:58-66]`
The install/platform layer compares a required vs installed version into a bit-flag result (B754 §754.1),
then masks it:

| Bit (dec) | Name | Meaning |
|---|---|---|
| 1 | LATER_VERSION | installed is newer |
| 2 | SAME_VERSION | equal |
| 4 | EARLIER_VERSION | installed is older |
| 8 | DIFFERENT_VERSION | different vendor (short-circuit — never satisfies) |
| 16 | EQUIVALENT_VERSION | compatible-equivalent |
| 32 | MORE_SPECIFIC_VERSION | installed has more digits |
| 64 | LESS_SPECIFIC_VERSION | installed has fewer digits |
| **115** | **MEETS_MINIMUM** | mask = 1+2+16+32+64 (LATER∨SAME∨EQUIVALENT∨MORE∨LESS) — everything EXCEPT EARLIER(4) and DIFFERENT(8) |
| **118** | **MEETS_MAXIMUM** | mask = 2+4+16+32+64 — everything EXCEPT LATER(1) and DIFFERENT(8) |

`meetsVersionRequirement`: a `minimum` relation passes when `(result & 115) > 0`, a `maximum` when
`(result & 118) > 0`. So a `<dependency vendorVersion="4.14">` (minimum) is satisfied by 4.14, 4.15, … but
NOT by 4.13 (EARLIER) nor by a different vendor (DIFFERENT). This is the install-time mirror of the runtime
`required ≤ installed` check (B754 §754.2). NOTE this is a SEPARATE model from the runtime dewey `Version`
(B754) — same intent, different representation.

## 755.5 — Why bits, and the one trap `[INFER]`
All four are bit-fields for the same reasons: O(1) test/set, cheap OR-combination, compact `.bog`/wire
encoding. **The trap**: because these are raw ints, an author can write a nonsense combination (e.g.
`TRANSIENT` on a config slot you WANT persisted → it silently won't survive a restart; or forgetting
`READONLY` on a computed output → an operator can scribble on it). The letter codes in Workbench (`rtshaox…`)
are your read-back — verify them on the property sheet.

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | Slot Flags 32-bit set with exact values/letters (READONLY=1…LINK_TARGET=32768, user-defined top bits) | [CERT] | Flags.java:23-44 |
| 2 | SUMMARY=8 = wire-sheet pin; HIDDEN=4 removes from all UI; TRANSIENT=2 not persisted | [CERT] | Flags.java:24-26; B735/B747 |
| 3 | BStatus 8 bits DISABLED=1…UNACKED_ALARM=128; ok=0; OR-combine | [CERT] | BStatus.java:46-53 |
| 4 | BPermissions OPERATOR_READ=1/WRITE=2/INVOKE=4, ADMIN_READ=16/WRITE=32/INVOKE=64; our gate = OPERATOR_WRITE bit | [CERT] | BPermissions.java:26-31; B752 |
| 5 | BVersion relation bits LATER=1…LESS_SPECIFIC=64; MEETS_MINIMUM=115, MEETS_MAXIMUM=118 with the exact masks | [CERT] | BVersion.java:58-66 |
| 6 | MEETS_MINIMUM excludes EARLIER(4)+DIFFERENT(8); mask logic (result & 115)>0 | [CERT] | BVersion.java:65,135-142 (B754) |

**Tally**: 6 [CERT]. No unmarked claims. All values verbatim from source this session.

## Connections
- **B735** (flags in the link picker / SUMMARY pin), **B736** (BStatus model), **B730** (flags in practice),
  **B708**/**B752** (RBAC / OPERATOR_WRITE), **B754** (the BVersion model in the upgrade story), **B740** (why
  a shared enum breaks — a different, class-level concern than these bits).

## Open gaps
- **B755-G1**: the `BFacets` internal flag/option bits (a separate small set) — not consolidated here; add if a
  facet-bit question arises.
