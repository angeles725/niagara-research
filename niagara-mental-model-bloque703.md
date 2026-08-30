# B703 — provisioning .hdb + focus SYNTHESIS (HD5): the history stores independently confirm the seed-station and weak-data-at-rest theses — a bench unit's trace, cleartext and rewritable on the card

> Focus: **jace-history-audit** · Gap **HD5** (the 3 provisioning .hdb + synthesis) — FOCUS-CLOSING block.
> Sources: `DeviceStep/NetworkStep/DeviceNetworkJobHistoryRecord.hdb` (SD P2) via `tools/hdbread.py` +
> [Block 699]–[Block 702]. Evidence: `sources/probes/B699-jace-history-audit/provisioning-synthesis.txt`.
> Block TYPE = **SYNTHESIS** (high [INFER] ratio expected). Marker `[CERT-hw]` for disk facts.

## 703.1 — The 3 provisioning .hdb are empty batchJob stores (§14 confirms B689)

[CERT-hw] The three files share the `.hdb` format ([Block 699]) but their record types are
**`batchJob:DeviceStepHistoryRecord`** (10 fields), **`batchJob:NetworkStepHistoryRecord`** (11 fields), and
**`batchJob:DeviceNetworkJobHistoryRecord`** (9 fields, incl. `submitUser`, `jobType`, `jobState`). This
**upgrades [Block 689] §689.3 from [INFER] to [CERT-hw]** (§14 back-pointer added): they are written by the
batchJob/provisioning subsystem, not the declared history services. Their record regions are ~244 bytes with
**no real records — empty**: no provisioning job ever ran, matching the empty `ProvisioningNwExt` ([Block 686]
§686.3 / [Block 691] §691.2).

## 703.2 — Focus synthesis: the operational trace of JACE_UMBRELLA

[CERT-hw across HD1–HD4] The five stores, read together, give the station's whole life:

| store | content | what it says |
|---|---|---|
| AuditHistory (HD1/HD2) | 30 config edits (18 Add/5 Chg/7 Rem), all `admin` | configured once, lightly, by one admin |
| SecurityHistory (HD2) | 58 Login / 59 Logout / 28 Session / **1 Fail** | low-use, single-operator |
| LogHistory (HD3) | Fox sessions + NRIO discovery churn vs the down IO-34 + transient Fox IOExceptions | bench connectivity, one unreachable field module |
| alarm.adb (HD4) | ~15 NRIO ping-fail/success, **routed nowhere** | the only alarms ever = the module flapping; no egress |
| provisioning .hdb (HD5) | empty batchJob stores | no fleet/provisioning job ran |

**Verdict:** the history/audit stores independently confirm — from the OPERATIONAL-RECORD angle — the two theses
this SD's focuses reached from config and crypto:
- **Seed station** (focus `jace-station-config` [Block 692]): the records show a unit configured once and left
  idle, with one down field point and no production activity.
- **Weak data-at-rest** (focus `jace-data-at-rest` [Block 698]): every store is **cleartext on the card**
  (`.hdb`/`.adb` unencrypted, [Block 699]) with **no off-box replica** ([Block 689]) — so the complete record
  of who logged in, what they changed, and what alarmed is both readable AND silently rewritable by anyone
  holding the SD. For an audit trail that is the worst case: no confidentiality, no tamper-evidence.

## 703.3 — Deliverable

[CERT-hw] The reusable artifact of this focus is **`tools/hdbread.py`** — a read-only reader for the Niagara
`.hdb` history format (magic `A106F11E`, len-prefixed HistoryConfig XML, cleartext records) with a `--mask`
mode for secrets-safe inspection. It parses any station's history files offline without a running Niagara.

## Connections

- Consolidates HD1 [Block 699] · HD2 [Block 700] · HD3 [Block 701] · HD4 [Block 702]. §14 confirms [Block 689]
  §689.3. Seed-station → [Block 692]; weak-data-at-rest → [Block 698]; no-egress/no-tamper-evidence →
  [Block 689]/[Block 566]. IO-34 down → [Block 687]. Alarm egress need → focus `alarm-webhook` [Block 666].

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | 3 provisioning .hdb = batchJob record types, empty (§14 confirms B689) | [CERT-hw] | hdbread --schema | measured |
| 2 | 5-store trace = configured-once, single-operator, one down module, no egress, no jobs | [CERT-hw] | HD1-4 | consolidated |
| 3 | independently confirms seed-station (B692) + weak-data-at-rest (B698) | [CERT-hw]+[INFER] | HD1-4 + focuses | synthesized |
| 4 | audit trail cleartext + rewritable, no replica | [CERT-hw] | [Block 699]/[Block 689] | cited |

**Tally:** [CERT-hw] ×3 · [INFER] ×1. Ratio expected-high for SYNTHESIS. No secret value; `--mask` used
throughout. §14 back-pointer to B689 git-verified.

## Focus status

**HD5 CLOSED → jace-history-audit investigable = 0 → focus STOP.** 5/5 investigable gaps closed (HD1–HD5); no
requires-execution, no blocked gaps. Deliverable `tools/hdbread.py`. Next: §18 retro + push.
