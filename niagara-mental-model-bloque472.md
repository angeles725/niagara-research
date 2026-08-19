# B472 — The backup-over-Fox mechanism fully reverse-engineered: the `backup` Fox channel streams a `config.bog` ZIP; `save=false` makes it read-only; the gate is permission bit 48 (focus jace8000, J8-G2 spec)

> **Focus:** `jace8000` (§16). **Gap:** J8-G2 — how the station `.dist`/`config.bog` is produced and streamed
> over Fox, so the B471 login can be extended to actually pull it.
> **Phase:** §12 + `[CERT]` decompiled source. Read-only (spec block; the live pull is §472.5/B473).
> **Block type: EVIDENCE (mechanism, cited).**
> **Sources:** `[CERT]` `com.tridium.backup.BBackupChannel` (backup-rt) · `javax.baja.backup.BBackupService`
> (backup-rt, Tridium original source) · [Block 134] (Fox circuit protocol) · [Block 471] (the working login).
>
> **Bottom line:** a station backup is NOT a `niagaraRpc` call — it is a dedicated **`backup` Fox channel**
> that opens a **circuit**, receives a one-tuple request `{save}`, and **streams a ZIP of `config.bog` +
> static files back over the circuit's byte stream**. Setting **`save=false` makes it purely read-only** (it
> does not `Station.saveSync()` — it just zips the on-disk config). The only gate is **permission bit 48** on
> `BBackupService`, which `admin` holds. This reduces the no-Workbench `.bog` grab to a bounded circuit
> implementation on top of B471.

## §472.1 — The `backup` channel

`[CERT]` `BBackupChannel extends BFoxChannel`, channel name **`"backup"`** (`BBackupChannel.java:26,34-35`).
It is a `BFoxChannel` (a named channel, [Block 134] §134.9), registered on the station's Fox connection. A
client drives it by opening a **circuit** with command `"backup"` (`circuitOpened` dispatches `backup` →
`this.backup(circuit)`, `restore` → `this.restore(circuit)`; anything else → `InvalidCommandException`)
`[CERT]` `BBackupChannel.java:43-50`.

## §472.2 — The client sequence (what Workbench does)

`[CERT]` `BBackupChannel.backup(boolean save)` (`:54-72`):
1. `FoxCircuit circuit = this.openCircuit("backup")` — open a circuit (command `"backup"`) on the `backup`
   channel. (Circuit = a numbered stream on the `circuit` channel, [Block 134] §134.9: client ids odd,
   4096-byte chunks.)
2. build `FoxMessage req; req.add("save", save)` → **`circuit.writeMessage(req)`** — write the request
   FoxMessage into the circuit byte stream. The whole request body is a single tuple: `{save=z:<t|f>}`.
3. `FoxMessage resp = circuit.readMessage()` — read the response FoxMessage from the stream. If
   `resp.getBoolean("failure")` → error (optional `exception`), else success.
4. `return circuit.getInputStream()` — **the remaining circuit bytes are the ZIP** (the `.dist` payload).

So over the circuit the byte stream is: `[req FoxMessage]` → `[resp FoxMessage]` → `[ZIP bytes …]`. The ZIP
is standard (PK…), so once the two small FoxMessages are consumed, the rest is a `.dist` you can open.

## §472.3 — The server side: what it zips, and the `save` switch

`[CERT]` `BBackupChannel.backup(FoxCircuit)` (`:75-111`):
- reads the req, gets `save`.
- **permission gate:** `if (!service.getPermissions(sessionContext).has(48)) throw new PermissionException()`
  (`:79-80`) — **bit 48** is required. `admin` (super user) holds it; a lesser role may not (J8-G2 child).
- **`if (save) Station.saveSync();`** (`:85-87`) — a `save=true` backup first flushes the running station to
  disk (a WRITE). **`save=false` skips this** — it only reads and zips the current on-disk config → the
  read-only path. This is the switch that keeps the pull non-destructive.
- `service.zip(null, out, true, null)` (`:108`) — `BBackupService.zip` streams the ZIP into the circuit
  output stream.

## §472.4 — What the ZIP contains (and excludes)

`[CERT]` `BBackupService` (Tridium original source) — the service defines the backup contents. Default
`excludeFiles = "*.hdb;*.adb;*.lock;*backup*;console.*;config.bog.b*;config_backup*"` and excluded ords
`file:^^history`, `file:^^alarm`, `file:^^webFileCache` (`BBackupService.java:150-163,202`). So the ZIP
**includes `config.bog`** (the station database) and static files (px, html, png, jpg — per the class javadoc
"files included in a configuration backup such as config.bog and supporting static files") and **excludes**
histories (`*.hdb`), alarm dbs (`*.adb`), locks, and prior backups. Exactly the station engineering — which
is what a `.bog` grab wants, and it lands with the same at-rest encryption of [Block 466] (portable domain →
passphrase-derived key on the sensitive fields).

## §472.5 — Implementation reduction (J8-G2 → a bounded build)

With B471's authenticated session, the pull needs only the **circuit layer** of [Block 134] §134.9 added to
the client:
1. after `welcome`, start reading/writing frames (no threads needed — synchronous is fine);
2. send `circuit`/`open` (ASYNC): `{id=i:<odd>, channel=s:backup, command=s:backup}`;
3. send `circuit`/`stream`: `{id=i:<odd>, data=b:<len>[<bytes>]}` where bytes = the serialized req
   `{\nsave=z:f\n}` (read-only);
4. read inbound `circuit`/`stream` frames for that id, concatenating the `data` blob bytes into a buffer;
5. parse ONE FoxMessage (`{…}`) off the front of the buffer = `resp`; the **remainder is the ZIP** → write to
   `sources/probes/` and open it;
6. `circuit`/`close` when the stream ends.

The one binary subtlety: the circuit `data` is a **FoxBlob** (`<len>[<rawbytes>]`, [Block 134] §134.4), so the
frame reader must be **byte-accurate** (not line-based) for this phase — the ZIP contains `\n`/`{`/`}` bytes.
That is the entire remaining work; the transport (authenticated Fox), the channel (`backup`), the request
(`{save=false}`), and the response framing are all pinned here.

## Self-verify

| # | Claim | Marker | Citation | Checked |
|---|---|---|---|---|
| 1 | backup channel name = "backup"; circuit command "backup" | [CERT] | BBackupChannel.java:34-35,43-46 | ✓ |
| 2 | client: openCircuit→writeMessage{save}→readMessage→getInputStream(ZIP) | [CERT] | BBackupChannel.java:58-71 | ✓ |
| 3 | server gates on permission bit 48 | [CERT] | BBackupChannel.java:79-80 | ✓ |
| 4 | save=true → Station.saveSync() (write); save=false → read-only | [CERT] | BBackupChannel.java:85-87 | ✓ |
| 5 | service.zip streams the ZIP to the circuit output | [CERT] | BBackupChannel.java:108 | ✓ |
| 6 | ZIP includes config.bog, excludes hdb/adb/history/alarm | [CERT] | BBackupService.java:150-163,202 | ✓ |
| 7 | circuit stream = FoxBlob chunks (byte-accurate needed) | [CERT] | [Block 134] §134.9 | ✓ corpus |

Marker tally: [CERT] ×7 (source/corpus) · [INFER] 0 load-bearing. **Block type: EVIDENCE (mechanism).**
Ratio ≈ 0.

## Connections

- **[Block 471]** — the authenticated Fox session this extends; **[Block 134]** — the circuit protocol
  (§134.9) the pull rides. **[Block 464]/[Block 466]** — the J8 route and the encryption the ZIP inherits.

## Open gaps

**J8-G2 impl** (requires-execution, next block): add the byte-accurate circuit layer to the B471 client, run
`backup(save=false)` live, and retrieve/verify the `.dist` ZIP (handle its `config.bog` in scratchpad by
sha256+bytes per SECRETS DISCIPLINE, never its body). Child **J8-G3**: confirm bit-48 reachability for a
non-super role.
