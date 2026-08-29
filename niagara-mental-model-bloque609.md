# Block 609 — W7-G1 code confirmed [CERT]: the `/schedule` and `/boxTable` handlers call `resp.sendError(404)` on `!target.canRead()` but DO NOT `return` — execution falls through to `target.get()` + `encodeSchedule`/`encodeTableData`, reading and writing the protected body; the sibling `encodeHistoryData` shows the CORRECT pattern (it `throw`s). Live leak-vs-no-leak verdict deferred: it fires only for a read-denied principal, which cannot be minted over the available surfaces

**Session**: 2026-08-29
**Focus**: `webChart` (gap W7-G1 — does `/schedule`/`/boxTable`'s `sendError(404)`-without-`return` leak the body
to an unauthorized user?). §12 DYNAMIC.
**Distribution / live target**: OptimizerSupervisor-N4.14.0.162, `127.0.0.1`. Code from
`WebChartQueryServlet.java`. `live-install` → SECRETS DISCIPLINE.
**Method**: DISK-FIRST (§12) — the code path is now confirmed `[CERT]` exactly; the LIVE trigger requires a
read-denied session, tested for reachability with `API2`/SCRAM (`no·inline`).
**Primary source**: `[CERT]` `organized/webChart/webChart-rt/vineflower/com/tridium/webChart/WebChartQueryServlet.java:94-116,135`.
**Scope**: nail the defect in code (upgrading [B374] §374.2's description) and determine what the live verdict
needs. Does NOT re-derive the servlet's 3 routes ([B374] REMITTANCE).

---

## 609.1 The defect — confirmed verbatim [CERT]

Both write-capable routes share the SAME missing-`return` bug `[CERT] :94-116`:
```java
// /schedule
OrdTarget target = ord.resolve(null, cx);
if (!target.canRead()) {
   resp.sendError(404);
}                                            // <-- NO return
BControlSchedule schedule = (BControlSchedule)target.get();   // reads the protected object
writer = this.getWriter(resp);
this.encodeSchedule(writer, schedule, ...);  // writes it to the response

// /boxTable  — identical shape
if (!target.canRead()) {
   resp.sendError(404);
}                                            // <-- NO return
BITable<?> dataTable = (BITable<?>)target.get();
writer = this.getWriter(resp);
this.encodeTableData(writer, dataTable, ...);
```
Two faults, not one: (a) the missing `return` after `sendError(404)`; (b) `target.get()` is then called on a
target the user **cannot read**, materializing the protected object regardless. The permission check is present
but INERT — it sets a 404 status and keeps going.

## 609.2 The correct pattern exists in the SAME file [CERT]

`encodeHistoryData` in the same servlet handles the identical situation CORRECTLY `[CERT] :135`:
```java
if (!target.canRead()) {
   throw new PermissionException();   // stops — no fall-through, no read, no body
}
```
So the `/schedule` and `/boxTable` paths are a genuine defect against the file's own established pattern, not an
intended design — the author knew to `throw`, and didn't on these two routes. This is new evidence beyond
[B374] §374.2 (which described the missing-return but not the in-file correct counterexample nor the
second-order `get()`).

## 609.3 Whether the body actually LEAKS is container-dependent — and needs a read-denied principal [CERT]/§12-wall

After `sendError(404)`, whether the subsequent `getWriter()` + `encode…` bytes reach the client depends on the
servlet container: if the response is already COMMITTED by `sendError`, further writes are discarded (404, no
leak); if not, the encoded body follows the 404 (leak). [B374] §374.2 correctly called this "container-commit-
dependent" — it is a runtime property of the deployed Jetty, resolvable ONLY by a live probe.

**The live probe requires a READ-DENIED principal**, and one cannot be obtained on this deployment with the
tooling in hand `[CERT-live]`:
- `API2` (and `admin`/`API`/`TUNEL`/`test`) all hold role `admin` → `canRead()` is TRUE → the defect branch
  never executes for them.
- The station HAS a zero-role user — **`BACnet`** (roles empty, `DigestScheme`) — which is exactly a read-denied
  principal, BUT its password is not held (SECRETS DISCIPLINE: never guessed/extracted).
- The oBIX surface exposes NO `add` op (UserService/RoleService) and NO `setPassword` op (users expose only
  `clearLockOut`/`setModified`) — so a low-priv principal cannot be MINTED or repurposed over oBIX. Minting one
  needs a Fox/BOX component-add + password action, or Workbench — a separate capability not built.

## 609.4 Verdict — code CLOSED, live DEFERRED-on-principal [CERT]

- **W7-G1 code question**: CLOSED `[CERT]` — the defect is real, on both routes, against the file's own correct
  pattern, with a second-order protected `get()`.
- **W7-G1 live question** (leak vs no-leak): **DEFERRED-requires-principal** — needs a read-denied session
  (`BACnet`'s password, or a Fox/BOX user-mint tool). This is the honest §12 wall: capability present in code,
  live trigger unreachable with the current principal/tooling.

---

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | /schedule: sendError(404) on !canRead, no return, then get()+encode | [CERT] | WebChartQueryServlet:94-101 | ✓ token |
| 2 | /boxTable: identical missing-return + get()+encode | [CERT] | WebChartQueryServlet:109-116 | ✓ token |
| 3 | encodeHistoryData throws PermissionException (correct pattern) | [CERT] | WebChartQueryServlet:135 | ✓ token |
| 4 | admin-role users canRead=true → branch never fires for them | [CERT-live] | UserService probe | ✓ live |
| 5 | BACnet is a zero-role (read-denied) user; password not held | [CERT-live] | UserService probe | ✓ live |
| 6 | No add/setPassword op over oBIX → cannot mint a principal | [CERT-live] | UserService/RoleService probe | ✓ live |

**Marker tally**: [CERT] ×3, [CERT-live] ×3, [INFER] 0. **Block type: EVIDENCE (§12, disk+live-wall).**
W7-G1 CODE closed; LIVE trigger DEFERRED-requires-principal. Zero secrets. Read-only.

## Connections

- [Block 374] §374.2 — described the missing-return; this block confirms it verbatim + adds the in-file correct
  counterexample and the second-order `get()`.
- [Block 605] — non-browser SCRAM (the client that would drive the read-denied probe if a principal existed).
- Shared blocker with `electronicSignature` ES4-G1 (also needs a low-priv principal) — see terminal report.
- webChart focus: W7-G1 code closed; live verdict is the sole residue (blocked-on-low-priv-principal).
