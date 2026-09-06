# [CERT-live] PANCCADIA room setpoint over oBIX — consolidated live record (viewer session, 2026-09-05/06)

Source: cross-session report from the `viewer` session (frontend `angeles725/panccadia-3d-viewer` @ dbbbc5c, pipeline
`angeles725/pancaddia-leon-tunnel` write-server.mjs/poller.mjs @ 8d738a2). Probes authorized directly by Cristian. JACE
192.168.200.137, Niagara 4.14. Verbatim record kept as evidence for B823 §823.8, B825, the retro
`obix-statusnumeric-wrapped-put` and the kit slot-type doctrine (PR15). Lead (investigador) also read the live
`config.bog`: ColdRoom_1 (h:44b7b) carries `Link2` sourceOrd h:44d51 (= Cuarto1) `setpoint → setpoint`, target flags `sL`.

## 1) Propagation — confirmed twice (2.5, then back to 3.0)
PUT `DashboardService/Cuarto{N}/setpoint` (RoomPanel) → wait ~3 s (link settle) → GET `Programacion/ColdRoom_{N}/setpoint`
(control) matches → Supabase `latest` (poller, ~5 s) matches. An earlier "stays 3.0" was a read BEFORE the link fired:
discarded. Discipline: always read back after a short settle.

## 2) Working write form (the only one that writes the intended value)
PUT text/xml body `<obj is="/obix/def/baja:StatusNumeric"><real name="value" val="N"/></obj>` → 200, no `<err>`, value = N.
Failing forms, verbatim:
- `<real val="N"/>` → `<err display="Cannot translate: <real val='N'>">`
- `<real val="N" is="/obix/def/baja:StatusNumeric" unit="obix:units/celsius"/>` → `Cannot translate`
- `<obj is="…baja:StatusNumeric"><real val="N"/></obj>` (child without name) → `Missing attr 'name' [line 1]`
- `<real name="value" val="N"/>` (no obj wrapper) → `Cannot translate`
- HAZARD: `<obj is="…baja:StatusNumeric" val="N"/>` (attr-only, no value child) → 200 OK but SILENTLY writes 0.0
  (zeroed Cuarto1 once before correction). Rule: a body builder must always emit the `<obj>…<real name="value" val>…</obj>`
  shape; never bare or attr-only.

## 3) Structure (GET, read-only)
- `/obix/config/Services/DashboardService/Cuarto1/` → is="/obix/def/DashboardPan:RoomPanel"; children are baja:StatusNumeric
  mirrors (zoneTemp1/2, evapTemp1..3, setpoint, …). `setpoint` = StatusNumeric, display "Setpoint", unit celsius, NO
  `writable="true"`, no `<op>` seen. It still accepts the wrapped write and propagates.
- `/obix/config/Programacion/` (baja:Folder): ColdRoom_1..5 (ColdRoomPan:ColdRoom), CompressorControl
  (CompPan:CompressorControl), kitControl points.
- `/obix/config/Programacion/ColdRoom_1/`: `setpoint` → `<real is="baja:StatusNumeric" displayName="Consigna">`, no
  writable attr; `differentialUp`/`differentialDown` → plain `<real writable="true" min="0.0">`; `roomHighAlarmLimit` →
  `<real writable="true">`; `coolOnSensorFault` → `<bool writable="true">`; `zone1`/`zone2` StatusNumeric mirrors;
  `stagingMode` → `<enum>`.
  So the plain writable doubles/bool take a bare `<real>`/`<bool>`; both setpoints are StatusNumerics without
  `writable="true"` that accept the wrapped-obj PUT (RoomPanel propagates to control through the bog link).

## 4) Method / path used for the probe
Not via the write-server (rejects setpoint at :232; its NUM builder emits a bare `<real>`). Direct JACE oBIX from the
mini-PC (JACE LAN) over SSH/SCP through Cloudflare Access, a Node script (https, rejectUnauthorized:false, Basic auth from
the pipeline config.env). PowerShell 5.1 could not negotiate the self-signed TLS; Node could. Each probe: scp .mjs, run,
delete.

## 5) Write-server enablement (surface A of C9 S12)
Add `setpoint` to WRITABLE with a new type whose obixBody emits the wrapped-obj form; keep the strict-shape rule; for the
audit, read back after settle before recording success (link delay). The additive `applySetpoint(BDouble)` action (B822)
is optional/cleaner, not required for propagation.

## 6) Final state
Restored to the original 3.0: RoomPanel 3.0, ColdRoom_1 3.0, latest 3.0; the room controls to 3.0. No lasting change, no
temp files left on the mini-PC.

## 7) Timed latency (measured on the mini-PC, LAN to the JACE, monotonic clock, no tunnel skew)
- oBIX PUT round-trip: 638 ms first call (cold TLS handshake), 132 ms warm.
- Propagation RoomPanel → ColdRoom_1 control: near-instant — the control side reflected the new value within 665 ms total
  on the 2.5 write and 155 ms total on the 3.0 restore, i.e. the link fires ~20-30 ms after the PUT completes, same
  engine cycle. The earlier "read too soon" miss was an isolated first-hit warm-up fluke; measured tightly it propagates
  in < 1 s every time, both directions.
- Dashboard / Supabase `latest`: ~6 s, bounded by the poller's 5 s poll + changed-rows write (poller artifact, not the
  control write).
- Doctrine for the C9 audit read-back: a ~1 s settle before reading the control side is enough; the slow leg is only the
  DB/dashboard. Final state restored to 3.0 (latest = 3, verified).

## 8) The child ORD `…/setpoint/value` IS served and advertised writable (GET only, no PUT) — closes B826-G1
- GET `/obix/config/Services/DashboardService/Cuarto1/setpoint/value` → 200
  `<real val="3.0" display="3.00 °C" displayName="Setpoint" unit="obix:units/celsius" writable="true"/>`
- GET `/obix/config/Programacion/ColdRoom_1/setpoint/value` → 200 `<real val="3.0" display="3.00" displayName="Consigna" writable="true"/>`
So the encoder collapses the StatusNumeric parent to a leaf and never advertises the child, but the slot-path decoder
resolves `…/setpoint/value` verbatim and serves it as a `writable="true"` BDouble leaf on both the RoomPanel and the
ColdRoom (B826). Implication [INFER until a PUT is authorized]: a bare `<real val="N"/>` PUT to `…/setpoint/value` would be
the clean write form (no wrapped obj, no `name="value"`, no silent-zero hazard, which was specific to the parent
StatusNumeric); propagation through the link would then rely on nested-child bubbling (B825 §825.3), to be confirmed by
one authorized PUT + control-side read-back before any write-server change targets the child. No PUT was made. All five
rooms read RoomPanel == ColdRoom (3 / 3 / -13 / 3 / 20), consistent with the link present on every room.
