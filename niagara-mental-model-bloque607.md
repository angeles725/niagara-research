# Block 607 — ⚠ CONFIG MUTATION — B458-G2 / B290-G2: the oBIX write surface is live and effective (`set`/`override`/`auto`/`emergency*` ops on a writable point), a `set` persists to the fallback/default level confirmed by an independent read, and the write requires NO CSRF token — settling B602's open `[INFER]`: oBIX is NOT behind the `CsrfProtectedFilter`

**Session**: 2026-08-29
**Focus**: `api-access` (B458-G2 — oBIX write/commit paths) + `px-menu` (B290-G2 — oBIX write surface). §12
DYNAMIC, **rung-2 reversible write** under the operator's session-scoped write grant (station confirmed TEST).
**Distribution / live target**: OptimizerSupervisor-N4.14.0.162, `127.0.0.1` (`DESKTOP-4AAQ77H`), app `CODIGOS`,
scratch point `NumericWritable5`. `live-install` → SECRETS DISCIPLINE.
**Method**: §12 LIVE-WRITE recipe — out-of-band auth (`API2`/SCRAM, scratchpad `-K`), independent-read oracle,
benign disposable marker (`12345.0`), byte-identical restore + verify. `no·inline`.
**Primary source**: `[CERT-live]` `sources/probes/B607-obix-write/write-oracle-restore.txt`.
**Scope**: prove the oBIX write path end-to-end and characterize its CSRF/priority behavior. Closes B458-G2 and
B290-G2. Does NOT re-derive the writable-point priority model ([B536] REMITTANCE) — it validates the WIRE write.

## ⚠ CONFIG MUTATION — before/after
- **Target**: `Drivers/CODIGOS/NumericWritable5.out` (fallback/default level).
- **Before**: `27.0 {ok} @ def`. **During**: `12345.0 {ok} @ def` (marker). **After (restored)**: `27.0 {ok} @ def`.
- Byte-identical revert performed and verified by independent read. No other state touched.

---

## 607.1 The write ops — reachable and effective [CERT-live]

A `control:NumericWritable` point exposes these oBIX ops (read-only listing, [B605] campaign): `set`,
`override`, `auto`, `emergencyOverride`, `emergencyAuto` — the priority-array write verbs ([B536] model).
Executed live `[CERT-live]` `sources/probes/B607-obix-write/write-oracle-restore.txt`:

| step | request | independent-read result |
|---|---|---|
| baseline | GET point | `out = 27.0 @ def` |
| write | `POST set/` `<real val="12345.0"/>` → **HTTP 200** | `out = 12345.0 @ def` |
| relinquish | `POST auto/` → HTTP 200 | `out = 12345.0 @ def` (unchanged!) |
| restore | `POST set/` `<real val="27.0"/>` → HTTP 200 | `out = 27.0 @ def` |

The write is confirmed by the INDEPENDENT GET (the value changed 27→12345→27), never by the write's own 200 —
satisfying the §12 oracle rule.

## 607.2 `set` writes the FALLBACK, not a priority level — why `auto` did not revert it [CERT-live]

The point stayed `@ def` (default level) through the whole sequence, and `auto` (relinquish) did NOT restore
the baseline. Mechanism: the oBIX `set` op writes the point's **fallback/default value** (the persistent
default the point falls back to when no priority level is active), not a transient priority-array override.
`auto` relinquishes priority levels 1–16 — but here those were already null (`in10`/`in16` null), so relinquish
was a no-op and the mutated FALLBACK remained. Operational consequence (load-bearing for anyone scripting
writes): **`set` is a PERSISTENT config change (it re-writes the default and dirties the BOG), NOT a temporary
override** — to restore you must `set` the original value back (as done), not `auto`. For a transient,
self-reverting write use `override` (a timed priority-level write); for a persistent one use `set`.

## 607.3 oBIX write requires NO CSRF token — settles B602 §602.4 [CERT-live]

The `set` POST carried **no `x-niagara-csrfToken` header and no `csrfToken` param**, yet returned HTTP 200 and
mutated the point. Therefore the oBIX servlet is **NOT** mapped behind `CsrfProtectedFilter` — a valid oBIX
write is accepted without the synchronizer token. This CONFIRMS the hypothesis [B602] §602.4 left as `[INFER]`
(the earlier malformed-URI probe was inconclusive; this is a real, successful, token-less write). Security
consequence: oBIX write CSRF protection depends entirely on the outer session cookie + `SameSite=Lax`
([B602]); there is no per-request token defence on the oBIX write path — a same-site authenticated context is
sufficient to write. (The web login/servlet paths that ARE mapped to `CsrfProtectedFilter` still require the
token — this finding is scoped to the oBIX servlet.)

## 607.4 Verdict [CERT-live]

- **B458-G2 (api-access)**: CLOSED — oBIX write paths are `set`/`override`/`auto`/`emergency*`; `set` persists to
  fallback, confirmed by oracle, reversible.
- **B290-G2 (px-menu)**: CLOSED — the oBIX write surface (`op name="set"`, writable points) exercised live; a
  non-browser SCRAM client ([B605]) can write, not only read.
- Station restored pristine; no residual mutation.

---

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | set/override/auto/emergency* ops on writable point | [CERT-live] | write-oracle-restore.txt | ✓ live |
| 2 | set 12345 → independent read 12345 (write effective) | [CERT-live] | write-oracle-restore.txt | ✓ live |
| 3 | set writes fallback (@ def); auto did NOT revert | [CERT-live] | write-oracle-restore.txt | ✓ live |
| 4 | write succeeded with NO csrf token (oBIX not CSRF-gated) | [CERT-live] | write-oracle-restore.txt | ✓ live |
| 5 | restored to 27.0 byte-identical, verified | [CERT-live] | write-oracle-restore.txt | ✓ live |

**Marker tally**: [CERT-live] ×5, [INFER] 0. Ratio 0. **Block type: EVIDENCE (§12 live, ⚠ CONFIG MUTATION).**
CLOSES B458-G2 + B290-G2. **§12 verdict: CONFIRMED** — write path live and effective, CSRF-exempt on oBIX.
Zero secrets. Station restored pristine (byte-identical revert verified).

## Connections

- [Block 602] §602.4 — the CSRF `[INFER]` this block settles (oBIX write is token-less).
- [Block 536] — the writable-point priority-array model (set=fallback, override=priority level, auto=relinquish).
- [Block 600]/[Block 605] — the read surface + non-browser SCRAM client this write extends.
- [Block 160] — a read-level user overwrote Reflow config; the oBIX-write ACL for a low-priv user is a separate
  test (pending a low-priv principal — ES4-G1/W7-G1 batch).
- api-access focus: B458-G2 closed → api-access fully closed. px-menu: B290-G2 closed.
