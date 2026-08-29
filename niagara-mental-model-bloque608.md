# Block 608 — jsonToolkit-G1 + G2 (merged): B341's inbound-trust defects (export-marker registration with NO ACL, and spoofable alarm-ack attribution) have ZERO live attack surface on this station — its jsonToolkit is configured OUTBOUND-ONLY (one `JsonSchemaQuery`, no inbound SetPoint/Ack/ExportMarker handler is instantiated anywhere in the component space). GATED-BY-DEPLOYMENT: the code defects stand, the deployment does not expose them

**Session**: 2026-08-29
**Focus**: `jsonToolkit` (gaps G1 export-marker ACL bypass + G2 alarm-ack attribution spoof — both
`requires-execution → §12`). Merged per §13 SPLIT/MERGE (both resolve to one deployment fact).
**Distribution / live target**: OptimizerSupervisor-N4.14.0.162, `127.0.0.1`, component `JsonSchema`
(`jsonToolkit:JsonSchema`). §12 DYNAMIC, READ-ONLY.
**Method**: DISK-FIRST (§12) — B341 already proves the defects `[CERT]` from code; the live question is whether
the vulnerable inbound handlers are DEPLOYED here. Two independent live enumerations (`API2`/SCRAM), `no·inline`.
**Primary sources**:
- `[CERT-live]` `sources/probes/B608-jsontoolkit-deployment/inbound-surface-absent.txt`.
- `[CERT]` REMITTANCE [Block 341] §341.3 (`BAlarmUuidAckHandler` `record.setUser` verbatim), §341.4
  (export-marker registration, no ACL / no Context check).
**Scope**: determine the LIVE exposure of B341's inbound defects on this deployment. Does NOT re-derive the code
defects ([B341] REMITTANCE, `[CERT]`).

---

## 608.1 The station's jsonToolkit is OUTBOUND-ONLY [CERT-live]

The single `JsonSchema` component (`/obix/config/JsonSchema/`) is configured for JSON GENERATION only
`[CERT-live]`:
- `queries/` holds one `jsonToolkit:JsonSchemaQuery` (the OUTBOUND generate-JSON query).
- ops present: `generateJson`, `forceGenerateJson`, `executeQueries`, `clearCache`, `clearOutput` — all
  outbound/marshalling.
- `config/` holds only `tuningPolicy`, `overrides`, `debug` folders.
- NO inbound handler type is present (`sourceSlotName`/`targetSlotName` slots exist but wire the outbound
  mapping, not an inbound `BJsonSetPointHandler`/`BAlarmUuidAckHandler`/export-marker handler).

## 608.2 Independent re-measurement — zero inbound handlers anywhere [CERT-live]

Because "no inbound surface" is a dramatic negative (RE-MEASURE rule), it was re-derived by a SECOND method: a
full config-tree walk (depth 4) matching every `is="…jsonToolkit…"` type against
`SetPointHandler|AlarmUuidAck|ExportMarker|InboundQuery|JsonImport|inbound` `[CERT-live]`
`sources/probes/B608-jsontoolkit-deployment/inbound-surface-absent.txt`:
- **JsonSchema instances: 1** (the one component).
- **jsonToolkit INBOUND handler instances: NONE.**

Two independent enumerations agree: the inbound handler classes that carry B341's defects are not instantiated
in this station's component space.

## 608.3 Verdict — GATED-BY-DEPLOYMENT (§12 scope-clarify) [CERT-live] + [CERT]

- **The code defects STAND** (`[CERT]` [B341]): export-marker registration resolves an attacker-chosen ORD with
  no ACL / no `Context` check (§341.4); the alarm-ack handler writes `record.setUser(ackUserName)` from the
  message verbatim (§341.3). Nothing here refutes B341.
- **The LIVE deployment does NOT expose them**: with no inbound handler instantiated, there is no message entry
  point that reaches those code paths on this station. B341's inbound-trust surface is latent, not live, here —
  exactly the §12 "code-path real, live deployment does not instantiate it" scope-clarification (the hardware→code
  softer move), NOT a refutation.
- **Why no synthetic proof was manufactured** (§18 honesty + §12 DISK-FIRST): confirming the bypass would
  require DEPLOYING an inbound handler + crafted JSON — i.e. building the vulnerable scaffold myself and then
  attacking my own scaffold. That validates B341's already-`[CERT]` code, not this station's exposure, and
  DISK-FIRST forbids spending a live write to re-prove certain code. The truthful live finding is the
  deployment posture: this station cannot be hit through jsonToolkit inbound because it has no inbound.

## 608.4 What this means operationally [CERT-live]

For THIS supervisor: the jsonToolkit inbound-trust risk (marker-registry poisoning, ack-attribution forgery,
sender-chosen priority) is **not present** — no remediation needed here. The risk becomes live the moment an
inbound `BJsonSetPointHandler` / ack / export-marker handler is added (e.g. to accept setpoints from an external
system); at that point B341's mitigations (front the inbound with authenticated transport + ACL, do not trust
the message's user field) apply. The gap closes as "no live surface on this deployment", with the code caveat
carried forward.

---

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | JsonSchema is outbound-only (JsonSchemaQuery, generate ops) | [CERT-live] | inbound-surface-absent.txt | ✓ live |
| 2 | Zero inbound handler instances (2 independent enumerations) | [CERT-live] | inbound-surface-absent.txt | ✓ live×2 |
| 3 | Export-marker registration has no ACL (code) | [CERT] | [B341] §341.4 REMITTANCE | ✓ prior |
| 4 | Alarm-ack writes setUser verbatim (code) | [CERT] | [B341] §341.3 REMITTANCE | ✓ prior |

**Marker tally**: [CERT-live] ×2, [CERT] ×2 (remittance), [INFER] 0. **Block type: EVIDENCE (§12 live).** CLOSES
jsonToolkit-G1 + G2. **§12 verdict: GATED-BY-DEPLOYMENT** — code defects real (B341), no live attack surface on
this outbound-only deployment. NOT-REPRODUCED-because-not-deployed, honestly distinguished from refuted. Zero
secrets. Read-only.

## Connections

- [Block 341] — the inbound-trust defects (§341.2 priority-choice, §341.3 ack-spoof, §341.4 export-marker
  no-ACL); this block measures their live exposure = none on this station.
- [Block 334] (email) — the sibling inbound-ack spoof; email-G1 is its live gap (blocked on a mailbox).
- [Block 600] — the oBIX read surface used to enumerate the schema.
- jsonToolkit focus: G1 + G2 closed (GATED-BY-DEPLOYMENT). Focus now investigable=0, requires-execution=0.
