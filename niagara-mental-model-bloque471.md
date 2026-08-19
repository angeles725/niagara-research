# B471 — A working hand-rolled Fox client authenticates to the live JACE-8000 over foxs:4911 (SCRAM-SHA-256, mutual-auth verified) — J8-G1 done, the sys-channel path to the .bog identified (focus jace8000, J8-G1)

> **Focus:** `jace8000` (§16). **Gap:** J8-G1 — build a minimal Fox client to reach the station without
> Workbench (the concrete step toward pulling a `.bog`).
> **Phase:** §12 dynamic — **live authenticated probe** against the real JACE (`192.168.1.140:4911`).
> Read-only (login only; no station mutation). `live-install` → SECRETS DISCIPLINE (password never sent by
> SCRAM nor recorded; ephemeral handshake values redacted in the preserved trace).
> **Block type: EVIDENCE (live, requires-execution close §19).**
> **Sources:** `[CERT-hw]` live Fox handshake this session (`sources/probes/B471-fox-client/`) · `[CERT]`
> corpus [Block 134] (Fox wire spec), [Block 457] (SCRAM) · [Block 460]/[Block 464] (this focus).
>
> **Bottom line:** a ~180-line stdlib Python client that speaks the Fox wire protocol (corpus B134) directly
> over TLS **successfully authenticated to the live JACE** — full handshake `hello → kerberos → username →
> challenge → authMessage1/2 (SCRAM-SHA-256) → welcome`, with the **server signature verified** (mutual
> auth). This proves the no-Workbench station channel is reachable by a hand-rolled client and closes B134's
> only open item (the live handshake trace). The remaining step to a `.bog` is invoking
> `BackupService.backup()` via the `sys` channel's `niagaraRpc` verb — identified here, scoped as J8-G2.

## §471.1 — The tool

`sources/probes/B471-fox-client/niagara-fox-client.py` — stdlib only, credentials out-of-band
(`$N4_PW`/prompt, never argv). It implements from [Block 134]:
- the **frame envelope** `fox <type> <seq> <reply> <channel> <command>\n{ body };;\n` (ASCII, no length
  prefix);
- the **FoxMessage body** `{ \n name=<T>:<value>\n … }` with type chars `s`/`i`/`z` and `writeSafe` `#hex;`
  escaping;
- the **client tune state machine** (`Tuner.openClient`): hello, receive kerberos, send username, receive
  challenge, send `clientKeyExchangeMethod`, run SCRAM `authMessage1/2`, receive `welcome`;
- **SCRAM-SHA-256** reused byte-identical from the web client [Block 457] (PBKDF2WithHmacSHA256, `Client
  Key`/`Server Key`, proof = clientKey XOR clientSignature), because [Block 134] §134.7 proved the Fox login
  digest is the *same* SCRAM as the web login — only the transport differs.

## §471.2 — Live handshake result (`[CERT-hw]`)

Against `192.168.1.140:4911`, exit 0, `FOX-LOGIN-OK`, **`server-signature-verified=True`**. Observed live
(structural trace preserved, ephemeral SCRAM values redacted —
`sources/probes/B471-fox-client/fox-login-trace-structure.log`, sha256 `316781ee3f4c…`):

| Step | Frame | Live finding |
|---|---|---|
| server hello | `fox a 0 -1 fox hello` | **`app.name=Station`** (confirms we reached the station, not the platform), `fox.version=1.0.2`, `n4Id`/`n4SuperId` session ids |
| kerberos | `fox a 1 -1 fox kerberos` | `useKerberos=z:f` — no Kerberos, plain SCRAM path |
| challenge | `fox a 2 -1 fox challenge` | **`method=n4digest`**, **`keyExchangeMethods=null.1`** (the TLS "no key exchange" bundle name — resolved live, not guessed) |
| clientKeyExchangeMethod | (client→) | echoed `null.1` — accepted |
| authMessage1 | reply | server-first carries **`i=10000`** PBKDF2 iterations + salt + server nonce |
| authMessage2 | reply | server sent `v=…` (server signature) → **verified by the client** |
| welcome | `fox a 5 -1 fox welcome` | empty body — **authentication complete** |

Key live facts not knowable statically: the digest scheme name is **`n4digest`** (the N4 label for
SCRAM-SHA-256), the TLS key-exchange bundle is **`null.1`**, and the live PBKDF2 iteration count is **10000**.
These close the "runtime salt/iteration/nonce" item [Block 134] §134.11 flagged as requires-execution.

## §471.3 — SECRETS DISCIPLINE upheld

- The **password is never transmitted** (SCRAM sends only a proof) and never recorded. It was supplied
  out-of-band via `$N4_PW` from scratchpad.
- The preserved trace **redacts** the ephemeral per-session values (client/server nonces, salt, proof `p=`,
  server signature `v=`, and the `n4Id`/`n4SuperId` session ids) — they are single-session and carry no
  reusable secret, but are masked anyway. Only the **frame structure** is preserved as `[CERT-hw]` evidence.

## §471.4 — The path from here to the `.bog` (J8-G2)

Login proves the channel; the `.bog` needs a post-auth request. The `sys` channel (`BSysChannel`) exposes
these verbs `[CERT]` `BSysChannel.java:101-119`: `navEvent`, `summary`, `stationCall`, `stationEvent`,
`listLocalSpaces`, `makeBrokerChannel`, `subNavEvents`, `unsubNavEvents`, and **`niagaraRpc`**. Invoking
`Services/BackupService.backup()` (or a `stationCall`) rides **`niagaraRpc`** — Niagara's remote method
invocation. The remaining RE is the **`niagaraRpc` payload serialization** (how the target ORD, method, and
typed arguments are encoded over a `FoxObject`), plus retrieving the produced `.dist` through a `FoxCircuit`
(the `file` channel / numbered-circuit streaming of [Block 134] §134.9). That is a clean, well-scoped next
step — **J8-G2** — not a new unknown: the transport (authenticated Fox session) and the verb (`niagaraRpc`)
are both in hand.

## §471.5 — Verdict update for J8

[Block 464] said the station BackupService route was viable "only the Fox client is unbuilt." **The Fox client
is now built and live-verified through authentication.** The no-Workbench `.bog` route is therefore
demonstrated feasible up to and including an authenticated station session; the final leg (invoke backup +
stream the `.dist`) is J8-G2. The two walls from [Block 464]/[Block 466] still stand: the `.dist` secrets are
**passphrase-encrypted** and a raw daemon-home `.bog` is **machine-key-sealed** — the Fox client does not
change what a copy *yields*, only that it can be *obtained* with station admin and no Workbench.

## Self-verify

| # | Claim | Marker | Citation | Checked |
|---|---|---|---|---|
| 1 | Fox client authenticated live (welcome, exit 0) | [CERT-hw] | this session; fox-login-trace-structure.log | ✓ FOX-LOGIN-OK |
| 2 | server signature verified (mutual auth) | [CERT-hw] | this session | ✓ server-signature-verified=True |
| 3 | reached the STATION (app.name=Station) | [CERT-hw] | trace | ✓ |
| 4 | scheme=n4digest, keyExchange=null.1, PBKDF2 i=10000 | [CERT-hw] | trace | ✓ |
| 5 | Fox login SCRAM = web login SCRAM (reuse valid) | [CERT] | [Block 134] §134.7 / [Block 457] | ✓ corpus |
| 6 | frame envelope + tune sequence per spec | [CERT] | [Block 134] §134.2/§134.6 | ✓ corpus, matched live |
| 7 | sys channel verbs incl. niagaraRpc (path to backup) | [CERT] | BSysChannel.java:101-119 | ✓ |
| 8 | password never sent (SCRAM) / not recorded | [CERT-hw] | this session | ✓ SECRETS DISCIPLINE |

Marker tally: [CERT-hw] ×5 · [CERT] ×3 (corpus/source) · [INFER] 0 load-bearing. **Block type: EVIDENCE
(live).** Ratio ≈ 0. This is a live requires-execution close (§19): findings preserved under `sources/probes/`
before the block, cited `[CERT-hw]`.

## Connections

- **[Block 134]** (`protocols`) — the Fox wire spec this client implements; §471.2 **closes its §134.11
  live-handshake requires-execution item** (a cross-focus contribution: the runtime scheme name, key-exchange
  bundle, and iteration count).
- **[Block 457]** (`api-access`) — the SCRAM engine reused; **[Block 464]/[Block 466]** — the J8 routes and
  the encryption walls this does not change.

## Open gaps

**J8-G2** (requires-execution, next): RE the `niagaraRpc` payload serialization + `FoxCircuit` file streaming
to invoke `BackupService.backup()` and retrieve the `.dist` — the final leg of the no-Workbench `.bog` grab.
Tool: `T: sources/probes/B471-fox-client/niagara-fox-client.py · created · minimal Fox foxs client (login).`
