# C9 PR11 (write-path matrix rows, docs-only) read — pr11-matrix-rows f1137fd vs 6d13d84

investigador1, 2026-09-06. Read-only in `Leon-Guanjuato-worktrees/pr11-matrix-rows` @ f1137fd. SC-9 lint run.
`[ev: lint-write-path + find srcTest]`

## Verdict
SC-9 passes (all three roots exit 0 after the rows land) and the rotation rows are not duplicated. But check (a) fails:
**~6 rows cite JUnit test classes that do NOT exist in srcTest, marked ✅/🔶 (covered) rather than ❌ (proposed).** The
lint is row-presence-only, so it cannot catch this — exactly why the manual check was asked. Docs-integrity finding, not
a runtime defect; the matrix over-claims coverage. (b)/(c)/(d)/(e) pass.

## (a) FINDING — phantom test citations (row → evidence)
srcTest actually contains: `ColdRoomControl{,Delay,Sequence}Test`, `ColdRoomWritePathTest`, `CompressorControlTest`,
`CompressorRotationTest`, `CompressorWritePathTest`, `DashboardDispatchTest`, `DashboardWriteGuardsTest`,
`FreezeAlarmWiringTest`, `JsonUtilTest`, `ResistanceLockoutTest`. NOT present: `DashboardServletTest`,
`DefrostControllerTest`, `RoomPanelStateTest`, `EvaporatorUnitTest`, `CompressorAlarmEdgeTest`.

| Row | Symbol | Cites | Problem | Fix |
|---|---|---|---|---|
| `:163 zoneTemp1Label` | ✅ | `DashboardServletTest` | ✅="covered this campaign" but the class does not exist (real: `DashboardDispatchTest`) | cite `DashboardDispatchTest`, or mark ❌ |
| `:152 resistanceTempThreshold` | 🔶 | `DefrostControllerTest` | 🔶="earlier-campaign test" but the class does not exist | drop the JUnit cite (keep "structural" bog check) or ❌-propose |
| `:153 staggerDelay` | 🔶 | `DefrostControllerTest` | same | same |
| `:160 resistHighLimit` | 🔶 | `RoomPanelStateTest.limitsColourTile` | 🔶 but the class does not exist | ❌-propose |
| `:66 fanMode` | 🔶 | `EvaporatorUnitTest` — "add fanModeOffOverridesAuto" | class absent AND symbol/content mismatch (🔶=covered vs "add") | ❌-propose |
| `:71 coolOnSensorFault` (ColdRoomPan logic) | 🔶 | "add sensorFaultFollowsCoolOnSensorFault; wire the link" | 🔶 but the row PROPOSES a test + a link (not covered) | ❌-propose |
| `:118 suctionLowLimit` | ✅ | `CompressorControlTest.cp1*` **+ PR9 `CompressorAlarmEdgeTest`** | cp1* EXISTS (✅ justified for the shed); `CompressorAlarmEdgeTest` is a **forward ref** to unmerged PR9 | keep ✅ via cp1*, label the PR9 test "(forward — lands with PR9)" |
Root cause: the legend's 🔶 = "covered by an earlier-campaign test (ColdRoomControlTest / CompressorControlTest)" is being
applied to rows whose cited class is neither of those and does not exist. `[ev: find srcTest @ f1137fd; matrix rows :66/:71/:118/:152/:153/:160/:163]`

## (b)/(c)/(d)/(e) — PASS
- **(b) ❌ rows carry a real next action**: `:158 intercambiadorMode` ❌ "create the station control + link (1a) or drop
  (2)"; `:159 coolOnSensorFault` (façade) ❌ "wire CuartoN → ColdRoom_N.coolOnSensorFault station-side (issue)" — both point
  at the dead-panel-writes issue. ✓
- **(c) façade pass-throughs cite the real bog link target**, including the Cuarto1 tile crossing: `:130 evap1FanMode →
  EvaporatorUnit_M.fanMode (Cuarto1 crossed: evap1→Unit_3)`, `:132 evap3→Unit_1`, valve rows likewise — matches the bog
  measurement / my line map. ✓
- **(d) rotation rows not duplicated**: `rotationInterval` ×1, `rotationMode` ×1 (PR1 added them at `:95-96`; PR11 does not
  re-add). ✓
- **(e) format/legend consistency**: legend (✅/🔶/❌) present and the table format matches the C8 rows; the only issue is
  the SEMANTIC misuse of 🔶/✅ in (a), not the layout. ✓
- **SC-9**: `lint-write-path.sh <root> --matrix …` → CompPan-rt / ColdRoomPan-rt / DashboardPan-rt all **exit 0, FAIL 0**. ✓

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | SC-9 exit 0 on all three roots; rotation not duplicated | [CERT] | lint run; grep ×1 |
| 2 | 5 cited classes absent from srcTest; ~6 rows mark them ✅/🔶 not ❌ | [CERT] | find + row symbols |
| 3 | dead-write ❌ actions + façade crossed-tile link targets correct | [CERT] | :158/:159; :130/:132 |
Tally: 3 [CERT] · 0 [INFER] · 0 unmarked.
