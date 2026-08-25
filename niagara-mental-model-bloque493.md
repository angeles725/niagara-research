# Block 493 — `oem-honeywell-tail` U1b+U1c: the honIrmConfig Workbench/UX layer adds no IRM-specific auth gate (stock `ri` + three `unrestricted` RPCs), and Nano-command authentication is UNIFORM transport-AES keyed on the device password — not per-opcode — bottoming out at the hardcoded `irmn4encryption1`⊕serial default key

> **Focus:** `oem-honeywell-tail`, gaps U1b (honIrmConfig `-wb`/`-ux` UI) + U1c (per-opcode auth trace).
> Finishes the honIrmConfig module started in [B242]. READ-ONLY, decompiled source; no binary run. Markers §3.
> Pre-flight (verified): `honIrmConfig-{wb=51, ux=9, rt=481}` classes.

## §493.1 — U1b: the `-wb`/`-ux` layer `[CERT]`

- **`-wb` (Workbench UI):** views/managers (`BIrmBacnetPointManager` `requiredPermissions="ri"` `:83`,
  `BIrmWiresheet` FB editor with online-debug cmd `:264`, `BIrmDeviceModel`, learn/progress/batch-IP dialogs);
  field editors (`extends BWbFieldEditor`) for Ethernet/WiFi/application/units/memory config
  (`BWiFiConfigurationAxFE.java:67` writes the WiFi `BPassword` as a field value); menu agents
  (`BIrmBacnetDeviceMenuAgent` etc., `default='true'`); commands (`IrmCommandBase extends HonMgrCommand`:
  Learn/Teach/Clear/Clone/MacConfig/IpConfig/MasterSync/Swap… — resolve selected `BIrmBacnetDevice`s and invoke
  rt-tier actions, `IrmCommandBase.java:36-56`).
- **`-ux` (server-side):** `BPeripheralRPC` — three `@NiagaraRpc` methods (`getUnitaryControllerData`/
  `getPeripheralStatus`/`getPeripheralDatas`, `:77-212`) doing live BACnet read/write; two
  `BIServerSideCallHandler`s (terminal-assignment save, controller-errors poll, `requiredPermissions="ri"`);
  UX field editors + views. JS front-ends call via `comp.serverSideCall(...)`.

## §493.2 — U1b finding: NO IRM-specific auth gate `[CERT]`

The UI/UX layer adds **no** IRM auth of its own — it uses only the **stock Niagara station RBAC**
(`requiredPermissions="ri"` = read+invoke) evaluated by the framework, and delegates device authentication to the
rt transport layer. **Security finding:** `BPeripheralRPC`'s three RPCs are `permissions="unrestricted"`
(`BPeripheralRPC.java:78,99,140`) — reachable over `web`/`box` with NO station permission, and they perform live
controller BACnet reads/writes. No UI/UX class references the device password or `BIrmCtrlSecStateEnum` (grep
empty) — the only password *value* in the UI is WiFi/cert config being written down, not an auth check.

## §493.3 — U1c: authentication is UNIFORM (transport-level), not per-opcode `[CERT]`

`NanoCmdIds` (`protocol/NanoCmdIds.java:3-69`) spans reads (`GET_*`, `READ_FILE*`) and writes
(`CREATE_CHILD 0x10`, `SET_PROPERTIES`, `WRITE_FILE`, …, `SET_CONTROLLER_PASSWORD 0x41`). The single dispatch/
encode path for EVERY opcode is `BBacnetProtocolService.runCommand`→`WriteWorker.encodeValue`
(`network/BBacnetProtocolService.java:436-499,1051-1089`). **There is no per-opcode auth switch** — every command
is symmetrically encrypted when `useEncryption()` is true (`AesSymmetricCryptographer.encrypt`, `:1062`), reads
decrypted the same way (`:644-684`). `useEncryption()` is a **firmware-version + model-capability** decision
(`BIrmBacnetDevice.useEncryption`→`isPasswordSupportedVersionCheckFromDevice`, `manager/BIrmBacnetDevice.java:2598-2612`),
NOT per-command or per-user.
- **Only opcode-conditional line** = a client safety guard, not auth: `runCommand` refuses `SET_CONTROLLER_PASSWORD`
  when `!useEncryption()` → `SET_CONTROLLER_PASSWORD_NOT_ALLOWED` (`:464-470`).
- **The real gate is device-firmware-side:** `BIrmCtrlSecStateEnum` (`FirmwareVersion/OldNanoCommands/
  SecureNanoCommandsOnly`, `manager/BIrmCtrlSecStateEnum.java:10-20`) is **defined but referenced nowhere** in
  rt/wb/ux — the controller firmware enforces accept/reject; the tool never checks it. No LOGIN/CHALLENGE/NONCE
  opcode exists → "authentication" == possession of the correct AES key. So it is **all-or-nothing at the
  transport layer**, not reads-unauth/writes-auth.

## §493.4 — U1c/E: the key regime (confirms [B242 §242.9]) `[CERT]`

`AesSymmetricCryptographer` (`network/AesSymmetricCryptographer.java`): AES/GCM/NoPadding, 12-byte random IV,
128-bit tag. Two key regimes by device password state:
1. **Default password** → `getSecureNanoSecret` null (`:103-111`) → `getKey(serial)` = hardcoded bytes
   `{105,114,109,110,…}` = ASCII **`irmn4encryption1`** (AES-128) XOR the 16-byte device serial (`:113-124`).
   This is the [B242 §242.9] weakness.
2. **User password** → key = **unsalted single-round `MD5(password)`** (`:152-154`); set on the controller via
   `NanoCmdSetControllerPassword` (`BIrmControlManager.java:10702-10706`).

**Residual risk (factual, no exploit):** there is no per-opcode auth to strengthen — the WHOLE opcode set reduces
to this key. Default-password devices are **effectively unauthenticated**: the key = a public constant
(`irmn4encryption1`, now in the clear) XOR the serial (queryable, e.g. `BPeripheralRPC.java:303` + BACnet identity),
so anyone with the algorithm + serial can forge/decrypt any command including `SET_CONTROLLER_PASSWORD` (its guard
only checks `useEncryption()`, not prior-secret knowledge). User-password devices collapse to offline MD5
brute-force given one known-plaintext (ECHO/GET_INFO). `SecureNanoKeyStore` DOES use PBKDF2-HMAC-SHA256/10000/salt
but is a SEPARATE store, NOT used for Nano auth. Amplifier: the `unrestricted` `BPeripheralRPC` (§493.2).

## §493.5 — Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|---|---|---|---|
| 1 | -wb=51/-ux=9/-rt=481; UI = views/FEs/menu-agents/commands invoking rt | `[CERT]` | pre-flight; `module.xml`; `IrmCommandBase.java:36-56` | PASS |
| 2 | No IRM auth gate — stock `ri`; 3 `unrestricted` peripheral RPCs (web/box live BACnet) | `[CERT]` | `BPeripheralRPC.java:78,99,140` | PASS |
| 3 | Auth uniform transport-AES for ALL opcodes; useEncryption = firmware/model flag | `[CERT]` | `BBacnetProtocolService.java:1051-1089`; `BIrmBacnetDevice.java:2598-2612` | PASS |
| 4 | Only opcode-conditional = SET_CONTROLLER_PASSWORD refused if !useEncryption | `[CERT]` | `BBacnetProtocolService.java:464-470` | PASS |
| 5 | Real gate device-side (BIrmCtrlSecStateEnum unreferenced); no login/nonce opcode | `[CERT]` | `BIrmCtrlSecStateEnum.java:10-20` (grep-empty consumers) | PASS |
| 6 | Key: default = irmn4encryption1⊕serial (AES-128); user = unsalted MD5(pw) | `[CERT]` | `AesSymmetricCryptographer.java:113-124,152-154` | PASS (confirms B242 §242.9) |

**Tally:** 6 claims, all `[CERT]`, 0 `[INFER]`.

## §493.6 — Connections

- Finishes honIrmConfig (U1b/U1c) over [B242] (rt spine, §242.9 hardcoded-key). Advances `oem-honeywell-tail`.
- Security ties: the `unrestricted` RPC surface + the transport-only, hardcoded-default-key auth are OEM-driver
  analogues of the licensing/trust weaknesses ([B490] posture); worth flagging in a client threat review.
- Open (this focus): U10 (other-vendor OEM drivers), U11-U13/U15 (mostly out-of-mission LOW), U14 (extended
  auth — in flight → B494).
