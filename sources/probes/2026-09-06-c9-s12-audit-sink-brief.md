# S12 audit-sink reconciliation brief — `change_log` vs the client's ask vs the B829 station trail

Author: investigador1, 2026-09-06. Inputs: proposal §5 (decision), design D6/D7/D7a/D8, S12 plan `1ecdf437c`, B829
`d26305d21` (+G1 close `7218fdad7`), the viewer echo of tunnel main **9acb47c** (repo not clonable here). Purpose: one page
that says what each of the three things records today, maps the client's four words onto columns, and confirms the single
sink the servlet (S12-B) must write to. Recommendation first.

**Recommendation (confirms proposal §5):** ONE canonical sink = Supabase **`public.change_log`, extended in place, additive
only**. Both surfaces write the same schema: surface A (write-server) directly; surface B (servlet) through the flag-gated
AuditHistory→`change_log` mirror (R7). The JSON-lines file is a failure spool, not a trail. The client's "explicit logout" is
answered from the same table by session lifecycle rows. An audit-append failure never fails the write.

---

## 1. What exists today — three records that do not agree `[CERT]` / `[CERT via viewer echo]`

| Record | Where | What it captures | What it cannot answer |
|---|---|---|---|
| **A. `public.change_log`** (tunnel main 9acb47c) | Supabase, inserted best-effort per `/write` | `user_email, user_id, room, slot, label, old_value, new_value, area, ok` — the JWT bearer's identity `[CERT via viewer echo]` | **when** (no `ts`), **result** of the station write, **which session / step-up**, explicit **logout**, `client_ip`; `old_value` provenance unknown (request body vs a read) |
| **B. Niagara AuditHistory** (`/PANCCADIA/AuditHistory`, installed — B829-G1 CLOSED) | station | surface A's oBIX PUT **is** audited (`ObixUtils.serviceWrite:558` passes `ot.getUser()`) — but to the ONE shared oBIX write user; surface B's servlet `parent.set(prop, val, null)` is **not audited at all** — `ComplexSlotMap.set:662` gates the `AuditEvent` on `context.getUser() != null` `[CERT B829]` | **who** for surface A (always the shared login); **anything** for surface B until the real-Context fix (B829-G2) |
| **C. write-server JSON-lines** (S12 plan) | mini-PC file | proposed as a parallel trail | two primary trails will diverge (proposal §5) — demote to a spool |

The client's ask is one sentence — **who / what / old→new / when + an explicit logout** — and no record above answers all
five. `[ev: S12 plan 1ecdf437c]` `[ev: corpus B829 §829.1/§829.2]`

## 2. The client's words → columns (the reconciliation) `[CERT]` where the column exists, `[INFER]` for additions

| Ask | Canonical column | Source of truth | Note |
|---|---|---|---|
| **who** | `user_email` (+`user_id`) | write-server, from the **config-session** identity (re-verified password, D6), not merely the JWT bearer | The station can never supply this for surface A (shared login). For surface B the station user IS per-operator if the panel login is per-operator — see §4 |
| **what** | `room`, `slot`, `label`, `area` | write-server ORD → façade mapping (`FACADE_PATH/CuartoN/<slot>`) | keep the raw `ord` too `[INFER]` — `room/slot` are derived and lossy for non-room targets (alarm ack, intercambiador) |
| **old → new** | `old_value`, `new_value` | `old_value` from a **pre-write GET of the slot** (D7), never the request body; `new_value` = what was sent; `result` = what the station answered | store both as text in the same formatting so the diff is comparable `[INFER]` |
| **when** | **`ts`** — ADD (timestamptz, server clock) | write-server | missing today; the single most important addition |
| **explicit logout** | **`config_session`** — ADD (opaque id) + lifecycle rows `result ∈ {config_login, config_logout}` with `slot/room` NULL | write-server `/config/login` mints, `/config/logout` revokes (server-held, D6) | ONE table still answers "when did this operator's session start/end"; a separate sessions table is the alternative if event rows in `change_log` are unwanted — tradeoff: two tables vs. nullable columns `[INFER — recommendation]` |
| — | **`result`** — ADD (`ok` stays; `result` carries the station outcome / error class) | write-server | lets an audit-append-failure row (`ok=false`, `result='audit_spool'`) sit beside real writes (SC-3) |
| — | **`surface`** — ADD (`'write-server' \| 'servlet'`) | both writers | the only way "unified" is queryable |
| — | **`client_ip`** — ADD | write-server / mirror | forensics; NULL for mirrored rows |

Migration: **additive only** — never drop or retype a column; `schema-risk.sh` has no jurisdiction over Postgres so R5 carries
its own additive-only pin (D7). The 9acb47c insert must keep passing after the rebase (RK3). `[ev: proposal §5]` `[ev: design D7]`

## 3. What step-up and logout ADD to the trail (why the JWT alone is not "who")

Today's row is attributed to a long-lived JWT bearer: a leaked/stale token writes rows in the operator's name. With the
config-mode step-up (B803 §803.6 pattern, D6): the `who` on every row is backed by a **fresh password proof within a short
TTL**, bound to `(email, purpose="config-write")`, held **server-side** (the client gets an opaque handle), with a sliding
inactivity window; `/config/logout` **revokes** it immediately, so a stolen handle dies and the trail records the session's end.
Order per write (D7): GET old → PUT/POST station → capture `result` → insert exactly ONE row; on insert failure the write
**still returns the station's outcome** and the row goes to the local spool, drained on the next success. `[ev: corpus B803 §803.6]`
`[ev: design D6/D7]`

## 4. Surface B — the servlet writes the SAME schema, via the mirror `[CERT B829]` + `[INFER]`

- **Precondition (S12-B / B829-G2):** `parent.set(prop, toSet, cx)` with the authenticated request user. `ComplexSlotMap.set:662`
  then builds the `AuditEvent`; `Nre.auditor != null` (`:1685`) already holds because `AuditHistoryService` is installed. This
  turns the servlet write from **suppressed** to Niagara-audited — schema-neutral, `schema-risk.sh` SAFE. `[ev: corpus B829]`
- **Attribution quality:** the AuditEvent's user is the servlet's authenticated STATION user. If each operator logs into the
  HMI panel with their own station account, surface B attributes the real operator natively — **better** than surface A's
  shared oBIX user. If the panel uses one shared account, surface B degrades to the same "shared login" problem. State which
  it is before enabling the mirror `[INFER — station config, not code]`.
- **Mirror (R7, flag-gated OFF by default):** reads `/PANCCADIA/AuditHistory`, inserts `surface='servlet'` rows with
  `config_session = NULL` (surface-B step-up is S12 plan Part 3, not in C9 — a faked id is worse than an empty column),
  dedupe key `(ts, user, target, old, new)` as a unique index so a replay is a no-op; proven by replaying a recorded fixture
  twice with a row-count assertion (SC-6). The station gains no outbound internet dependency and the servlet holds no Supabase
  credential. `[ev: design D7a]` `[ev: proposal §5]`
- The servlet's existing fire-and-forget module `auditLog` (`appendAudit`) stays unchanged — it already implements
  audit-fail-never-fails-the-write; the RED pins that it stays that way. `[ev: design D8b]`

## 5. Open items (none block a PR)
| Item | Owner / gate |
|---|---|
| **B829 live**: both trails merge on the shared schema on a real write from each surface | requires-execution; gates the R7 mirror's LIVE enablement only |
| Rebase `qa/c9-s12-write-server` 24adcba onto tunnel 9acb47c before apply; keep the existing `change_log` insert green | RK3, PR4 |
| Decide: session lifecycle rows IN `change_log` (one table) vs a `config_sessions` table | propose/design — recommendation above is one table |
| `old_value` formatting parity with `new_value` (text, same renderer) | R5 schema pin |
| Whether the HMI panel login is per-operator (decides surface B's attribution quality) | station config question for Cristian |

## 6. Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | 9acb47c `change_log` columns and best-effort JWT-only insert | [CERT via viewer echo] | lead relay 2026-09-06 |
| 2 | servlet null-Context write not audited; oBIX PUT audited to the shared user | [CERT] | B829 `ComplexSlotMap.set:662`, `ObixUtils:558` |
| 3 | AuditHistoryService installed on PANCCADIA | [CERT] | B829-G1 (bog read `/PANCCADIA/AuditHistory`) |
| 4 | One sink + spool + mirror decision | [CERT] | proposal §5, design D7/D7a |
| 5 | `ts`/`config_session`/`result`/`surface`/`client_ip` additions; lifecycle rows; raw `ord` | [INFER — recommendation] | §2 |
| 6 | Surface B attribution depends on per-operator panel login | [INFER] | §4 |
Tally: 4 [CERT] · 2 [INFER] · 0 unmarked.
