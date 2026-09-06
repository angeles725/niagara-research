# Wave-2 doc-PR doctrine drafts — PR13 / PR14 / PR15 (ready-to-paste)

Author: investigador1 (Opus). Drafts for the wave-2 build-kit doc PRs — **companero does the fidelity read** against the
source blocks. Same format as the PR7 draft: per PR = target file/§ · grep-before-fold evidence · ready-to-paste lines
with `[ev: corpus B<n>]` tokens · ≤15 lines per block. Source §s cited so the fidelity reader confirms exact wording.

---

## PR13 — Post-deploy verification checklist → `build-n4-module-kit/BUILD-LOOP.md` §6

**grep-before-fold**: `grep -cE 'post-deploy|snapshot|health surface|triage' BUILD-LOOP.md` (record hits; expect 0 for the new §).

**Ready-to-paste** (a new §6 "Post-deploy verification", from B811 §811.5):
```
Within ≤5 min of a hot module reload (Out-of-date: Module changed):
1. PRE: BackupService "Backup Station" → .dist; sha256 config.bog → pre.hash.                 [ev: corpus B811]
2. Snapshot the running station WITHOUT mounting the FS (fox:|file backup / station-load.sh).  [ev: corpus B811]
3. Fetch latest console_backup_*.txt (parse latin-1); scan "Cannot load station" /
   "Missing frozen property" / "ClassCastException" / "Missing class for \"<own-prefix>:\"".   [ev: corpus B800, B818]
4. sha256 config.bog again → diff vs pre.hash; any retype/slot-remove → schema-risk.sh (OUTAGE gate). [ev: corpus B795]
5. Proxy-link safety: every OUR-output→writable-proxy has a SAFE non-null fallback + writeOnUp
   (a null fallback HOLDS the relay on stop/reload — resistance/compressor left ON).            [ev: corpus B810]
6. Health surface: the logic component exposes a fault-status slot + alarm ext + heartbeat lastTick
   (a LOGIC fault must reach the operator, not only the engine console).                        [ev: corpus B808, B812]
Order of checks: triage-console → bog-audit → report-module.                                    [ev: corpus B800, B816]
```

## PR14 — Build pipeline + module versioning → `BUILD-LOOP.md` (build-task matrix) + `METHODOLOGY.md`

**grep-before-fold**: `grep -cE 'gradle|niagara-module|Out-of-date|vendorVersion|mirror' BUILD-LOOP.md METHODOLOGY.md`.

**Ready-to-paste**:
```
- Build-task matrix: what each `niagara-module` gradle task DOES, cited to the plugin source (B807 §matrix). [ev: corpus B807]
- Version-bump checklist: bump `vendorVersion` on EVERY schema change; the reload path re-decodes config.bog
  against the installed module, so a retype/remove is a schema-risk OUTAGE (run schema-risk.sh).            [ev: corpus B807, B795]
- Exit-31 mirror recipe: mirror niagara-home with the source cite for the station LOCK (B807 §station-lock).  [ev: corpus B807]
- Delete a @NiagaraType and its module-include.xml <type> line in the SAME change (dangling = live "Missing class"). [ev: corpus B818]
- Structure lints L1–L11 (package/one-type-per-file/model-seam/lexicon/palette/module-include/dep-floor + L10/L11). [ev: corpus B817]
```

## PR15 — RT control doctrine → `types/logic.md` + `types/logic-authoring.md`

**grep-before-fold**: `grep -cE 'PID|anti-windup|deadband|write-path|flowchart|HOA|precedence' types/logic.md types/logic-authoring.md`.

**Ready-to-paste**:
```
- Control-logic patterns + the §805.9 FLOWCHART template (states·inputs·timers·control·protections·outputs·feedback). [ev: corpus B805]
- PID anti-windup (clamp errorSum), deadband anti-short-cycle, D-latch; Tridium ships NO SR latch / NO ODE/matrix
  (a physics step() model is beyond stock); output has a fail-safe value on fault (never NaN).               [ev: corpus B805]
- History-ext: BHistoryExt IS a point ext; Interval vs COV; capacity + fullPolicy(roll/stop); ONE ext per slot. [ev: corpus B804]
- Heartbeat/liveness monitor: lastTick TRANSIENT slot; STALLED when age > factor×period; re-arm floored ≥1s.   [ev: corpus B812, B801]
- Health surface: fault-status slot + BAlarmSourceExt — a LOGIC fault must reach the operator.                [ev: corpus B808]
- Write-path & overlap MATRIX (slot × writer × timing → invariant) + the LINK_TARGET line: a dashboard write to a
  link-target slot LANDS then is SILENTLY overwritten next propagation; set() serializes only the raw store.   [ev: corpus B816]
- HOA PRECEDENCE for a dangerous actuator (heat/compression): **OFF > sequence > HAND > AUTO**. OFF is a LOCKOUT
  that dominates EVERY automation INCLUDING a sequence that "owns" the output (e.g. defrost); HAND is dominated by
  safety interlocks; AUTO is the computed value. Maps to Niagara's priority array (BBooleanWritable: emergency/manual
  levels in1–in2 vs automation in8–in16 + relinquish) — our plain-double HOA emulates priority 1–2 for OFF. Test:
  a pure `resistanceCommand(inDefrost, mode, auto)` (mutation: swap OFF/defrost order) + a "HOA × mid-cycle" write-path
  matrix row the coverage lint MUST demand. Live bug fixed on fix/resistance-off-lockout (v2.0.6).            [ev: corpus B805, B810, B816]
```

---

## Notes for the fidelity reader (companero)
- B811 §811.5 and B808 §808.4 are quoted near-verbatim; B807's task-matrix + station-lock cites are `extern`-decompiled
  plugin lines (verify-block cannot resolve them — driver-token-verified in B807). B817 §817.9 lists L1–L7 explicitly;
  confirm L8–L11 exist in the block tail before folding the "L1–L11" range.
- The HOA-precedence block is NEW doctrine (lead directive today); its own evidence lands in a B805 §805.11 addendum +
  a B816 write-path matrix row (being written) — cite those once pushed, not this draft.
- Every line carries a standalone `[ev: corpus B<n>]` token so `sweep-fold-audit.sh --strict` credits it after the fold.
