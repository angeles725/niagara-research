# Block 134 — Fox/Foxs protocol wire: the line-oriented frame envelope, frame opcodes, channel/circuit multiplexing, the hello/tune login state machine, and the SCRAM-SHA-256 authentication digest

> Research of the **Niagara N4 Fox protocol at the wire level** as actually implemented in the shipped
> Java runtime jar — the bytes a Fox connection puts on the stream, NOT the architecture/session
> integration view. Fox is the station-to-station and Workbench-to-station RPC/streaming protocol
> (plaintext TCP **1911**, TLS **4911/foxs**). The Fox ARCHITECTURE (session lifecycle, where
> `platform.fox` lives, the daemon surface) is already covered by B1/B13/B27/B129; this block documents
> the layers those abstractions serialize INTO: the **frame envelope** (the literal `fox ` line header +
> `;;` footer), the **7 frame opcodes**, the **FoxMessage tuple body codec** (a text/binary hybrid), the
> **named-channel + numbered-circuit multiplexing**, the **hello/tune login state machine** (the
> handshake commands `hello`/`kerberos`/`username`/`challenge`/`welcome`/`rejected`/`retry`/`redirect`),
> and — the highest-value part — the **SCRAM-SHA-256 authentication digest** (RFC-5802-style: PBKDF2 +
> HMAC-SHA-256 + ClientKey/ServerKey, computed in `nre.jar`, transported by Fox as base64 strings).
> READ-ONLY. Corpus language: ENGLISH.
>
> Sources (primary, decompiled with Vineflower from the live install):
> `/mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162/modules/fox-rt.jar` (sha256 7d7d144f…) — the Fox
> framing, multiplexing, tune state machine, schemes; and
> `/mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162/bin/ext/nre.jar` (sha256 33aaaac5…) — the
> `com.tridium.nre.auth.Scram*`/`Pbkdf2`/`NiagaraStationAlgorithmBundle` digest classes that fox-rt only
> *invokes*. Both preserved under `sources/decompiled/fox-rt/` and `sources/decompiled/nre-auth/` and
> registered in `sources/SOURCES.md`.
> Method: `decompile-java.sh` (Vineflower) + targeted reading of `com.tridium.fox.{session,message,sys,
> authn,util}` and `com.tridium.nre.auth` + `grep` token confirmation of every literal byte/string.
> Markers:
> `[CERT]` local primary source (`file:line`) · `[CERT-doc]` downloaded doc · `[CERT-web]` official web ·
> `[CERT-a]` secondary/forum · `[INFER]` deduction.
>
> Layer 26 (Communication protocols — wire-level focus). Connects [Block 1] / [Block 13] / [Block 27] /
> [Block 129] (Fox architecture, session muxing, platform.fox daemon) and [Block 131] / [Block 132] /
> [Block 133] (sibling protocol blocks — Modbus, OPC, BACnet wire encoding).

---

## 134.1 — Module map: where each Fox wire layer lives `[CERT]`

The whole Fox codec is Tridium-written and split across packages inside `fox-rt.jar`; the **only**
external dependency for the wire is the SCRAM digest engine, which lives in `nre.jar` (the bootstrap
runtime, not a module). The layers separate cleanly:

| Wire layer | Package / jar | Key classes |
|---|---|---|
| Frame envelope (on-stream framing) | `com.tridium.fox.session` (fox-rt) | `FoxFrame` (write/read), `FoxSession` (`readFrame`/`writeFrame`) |
| Message body codec | `com.tridium.fox.message` (fox-rt) | `FoxMessage`, `FoxTuple`, `MessageWriter`, `MessageReader`, value types `FoxString/Integer/Float/Boolean/Time/Blob/Object` |
| Multiplexing (channels + circuits) | `com.tridium.fox.session` (fox-rt) | `SessionCircuits`, `FoxCircuit`, `SessionDispatcher`; channels `com.tridium.fox.sys.*` (`BFoxChannel`, `BSysChannel`, `BFileChannel`, `BUserChannel`, `BSpyChannel`, `BBrokerChannel`, `BDataChannel`) |
| Session I/O threads | `com.tridium.fox.session` (fox-rt) | `SessionSender`, `SessionReceiver`, `SessionDispatcher`, `SessionBedroom` (sync-reply parking) |
| Login / tune state machine | `com.tridium.fox.session` (fox-rt) | `Tuner` (openClient / openServer / run), `FoxScramShaUtil` (auth transport) |
| Scheme + port + discovery | `com.tridium.fox.sys` (fox-rt) | `BFoxScheme` (1911), `BFoxsScheme` (4911), `Fox` (multicast 224.0.1.84:1911) |
| **Auth digest (invoked, not in fox-rt)** | `com.tridium.nre.auth` (**nre.jar**) | `Scram` (base), `ScramClient`, `ScramServer`, `Pbkdf2`, `NiagaraStationAlgorithmBundle` |

The on-the-wire nesting is `TCP(/TLS) ▸ Fox frame ▸ FoxMessage body ▸ (for stream channels) circuit
chunks`. §134.2–§134.4 document the envelope + body; §134.5–§134.8 the handshake + auth; §134.9 the
multiplexing; §134.10 the transport selection.

## 134.2 — The Fox frame envelope: a line-oriented text header with `fox ` magic `[CERT]`

Unlike the binary PDUs of Modbus (B131), OPC-UA (B132) and BACnet (B133), a Fox frame is framed by a
**human-readable ASCII header line** followed by the message body and a `;;` footer. `FoxFrame.writeHeader`
emits, in order `[CERT]` `FoxFrame.java:45-60`:

| Bytes (decimal `MessageWriter.write(int)`) | ASCII | Meaning |
|---|---|---|
| `102 111 120 32` | `f o x <SP>` | **frame magic / hello literal** — every frame starts with `"fox "` |
| `<frameType>` | one of `s a r e n k c` | the frame opcode byte (§134.3) |
| `32` | `<SP>` | separator |
| `writeInt(sequenceNumber)` | decimal ASCII | per-direction monotonic sequence (decimal digits, not binary) |
| `32` | `<SP>` | separator |
| `writeInt(replyNumber)` | decimal ASCII | reply correlation id, `-1` if none |
| `32` | `<SP>` | separator |
| `writeName(channel)` | name chars | target channel (e.g. `fox`, `circuit`, `sys`) |
| `32` | `<SP>` | separator |
| `writeName(command)` | name chars | command (e.g. `hello`, `challenge`, `open`) |
| `10` | `\n` | end of header line |

Then `message.writeValue(out)` (the body, §134.4), then `writeFooter` emits **`59 59 10` = `;;\n`**
`[CERT]` `FoxFrame.java:62-64`. The reader is symmetric: `FoxFrame.read` does `in.consume("fox ")`,
reads the type byte, the two ints, the channel/command names, `consume(10)`, parses the body, then
`consume(59); consume(59); consume(10)` `[CERT]` `FoxFrame.java:66-98`. **`writeInt`/`writeName` are
ASCII** — `MessageWriter.writeInt` is literally `writeName(Integer.toString(value))` `[CERT]`
`MessageWriter.java:86-88`, and `MessageReader.readInt` parses decimal digits from the stream `[CERT]`
`MessageReader.java:144-171`. So a real Confirmed login hello on the wire begins with the printable text
`fox a 1 -1 fox hello\n{...}` — i.e. an **ASYNC frame, seq 1, no reply, channel `fox`, command `hello`**.

**No length prefix.** The frame has no binary length field; the body is self-delimiting (`{…}` braces,
§134.4) and the `;;\n` footer closes it. To bound pre-auth memory, the reader applies a **size limit**:
`FoxSession.readFrame` sets `in.setReadLimit(this.frameSizeLimit)` before each frame `[CERT]`
`FoxSession.java:896`, and a server starts at `Fox.getPreAuthFrameSizeLimit()` = **65536 bytes**
(`DEFAULT_PRE_AUTH_FRAME_SIZE_LIMIT = 65536L`) `[CERT]` `Fox.java:37-38`, `FoxSession.java:133-135`; a
`ReadLimitExceededException` increments an invalid-frame counter `[CERT]` `FoxFrame.java:99-100`.

## 134.3 — Frame opcodes (the 7 frame types) + sequence/reply numbering `[CERT]`

The frame-type byte is one printable ASCII char; the seven constants are declared on `FoxFrame` `[CERT]`
`FoxFrame.java:13-19`:

| Const | Byte (dec / char) | Role | Cite |
|---|---|---|---|
| `SYNC` | `115` / `s` | synchronous request — caller blocks for a reply (`sendSync`) | `FoxFrame.java:13`, `FoxSession.java:680` |
| `ASYNC` | `97` / `a` | asynchronous request / tuning frame (no blocking) | `FoxFrame.java:14`, `FoxSession.java:615` |
| `REPLY` | `114` / `r` | reply to a SYNC request (`FoxResponse` body) | `FoxFrame.java:15`, `FoxSession.java:687-688` |
| `ERROR` | `101` / `e` | error reply / `busy` rejection | `FoxFrame.java:16`, `FoxSession.java:618-619` |
| `NULL` | `110` / `n` | null reply (request completed, no payload) | `FoxFrame.java:17`, `FoxSession.java:689-690` |
| `KEEPALIVE` | `107` / `k` | idle keepalive (no sequence) | `FoxFrame.java:18`, `SessionSender.java:9` |
| `CLOSE` | `99` / `c` | session close notification | `FoxFrame.java:19`, `SessionSender.java:47` |

**Sequence/reply discipline.** Each side increments its own sequence: client starts `localNextSequence=1`
/ `remoteNextSequence=0`, server starts `0`/`1` `[CERT]` `FoxSession.java:140-141`. The reader **enforces
in-order sequencing** — `readFrame` throws if `f.sequenceNumber != remoteNextSequence` (modulo
`Integer.MAX_VALUE`) `[CERT]` `FoxSession.java:903-907`, except KEEPALIVE (`107`) frames are silently
skipped (`do…while(f.frameType==107)`) `[CERT]` `FoxSession.java:900` and CLOSE (`99`) frames bypass the
sequence check `[CERT]` `FoxSession.java:902`. A SYNC request carries a `replyNumber` allocated from a
"bedroom" of parked threads (`SessionBedroom.Bed`), and the matching REPLY/NULL/ERROR frame carries that
number to wake the caller `[CERT]` `FoxSession.java:679-697`. The keepalive frame is a fixed singleton
`new FoxFrame(107, -1, -1, "fox", "keepalive", …)` emitted whenever the send queue idles past
`Fox.keepAliveInterval` (default **5000 ms**) `[CERT]` `SessionSender.java:9, 71-78`, `Fox.java:29`.

## 134.4 — The FoxMessage body: a brace-delimited tuple list with typed values `[CERT]`

Every frame body is a `FoxMessage` — a `{`-delimited list of named, typed tuples. `FoxMessage.writeValue`
emits `123 ('{') 10 ('\n')`, then each tuple followed by `\n`, then `125 ('}')` `[CERT]`
`FoxMessage.java:32-52`; the reader loops reading tuples until it hits `}` `[CERT]` `FoxMessage.java:55-68`.
Each tuple is written by `FoxTuple.write` as **`<name> = <typeChar> : <value>`** — literally
`writeName(name).write(61 '=').write(getType()).write(58 ':').writeValue(out)` `[CERT]` `FoxTuple.java:11-14`,
parsed back by `FoxTuple.read` (`readName`, `consume('=')`, read type byte, `consume(':')`, value,
`consume('\n')`) `[CERT]` `FoxTuple.java:18-71`. The **type byte = the value class id** `[CERT]`
`FoxTuple.java:23-65`:

| Type byte (dec/char) | Class | Wire value encoding | Cite |
|---|---|---|---|
| `122` / `z` | FoxBoolean | `t`/`f` text | `FoxTuple.java:63-64` |
| `105` / `i` | FoxInteger | decimal ASCII | `FoxTuple.java:48-50` |
| `102` / `f` | FoxFloat | text | `FoxTuple.java:45-47` |
| `116` / `t` | FoxTime | millis as text | `FoxTuple.java:60-62` |
| `115` / `s` | FoxString | **escaped** text via `writeSafe`/`readSafe` | `FoxTuple.java:57-59`, `FoxString.java:22-29` |
| `109` / `m` | FoxMessage | nested `{…}` (recursive) | `FoxTuple.java:51-53`, `FoxMessage.java:27` |
| `98` / `b` | FoxBlob | `<len>[<raw bytes>]` | `FoxTuple.java:24-25`, `FoxBlob.java:40-50` |
| `111` / `o` | FoxObject | `<encoding> <len>[<raw bytes>]` | `FoxTuple.java:54-55`, `FoxObject.java:26-38` |

**String escaping** (`MessageWriter.writeSafe`): printable ASCII `' '`..`127` except `#` is passed
through; everything else (incl. `\n`, non-ASCII) is escaped as **`#<hex>;`**, and a null string is the
literal `#null;` `[CERT]` `MessageWriter.java:102-120`; `readSafe` reverses it `[CERT]`
`MessageReader.java:238-269`. This escaping is what keeps the body line-safe so the `\n`/`}`/`;;` framing
stays unambiguous. **Binary payloads ride as FoxBlob/FoxObject**: a length in decimal ASCII, then the raw
bytes verbatim between `[` (`91`) and `]` (`93`) `[CERT]` `FoxBlob.java:40-42` — the only truly binary span
inside the otherwise text framing, read with `readFully(length)` `[CERT]` `FoxBlob.java:45-50`. A
`FoxRequest`/`FoxResponse` is just a `FoxMessage` whose name is `channel.command` `[CERT]`
`FoxRequest.java:9-13`.

## 134.5 — The hello handshake + Fox version negotiation `[CERT]`

A connection opens with both sides exchanging a `fox`/`hello` ASYNC frame. The client sends first
(`Tuner.openClient` → `session.sendHello`) `[CERT]` `Tuner.java:96-104`; the server replies after
`receiveHello` `[CERT]` `Tuner.java:381-452`. The hello body is built by `FoxSession.initHello` `[CERT]`
`FoxSession.java:501-547`:

| Hello key | Value | Cite |
|---|---|---|
| `fox.version` | **`"1.0.2"`** (hard-coded) | `FoxSession.java:503` |
| `id` | legacy integer session id | `FoxSession.java:504` |
| `n4Id` / `n4SuperId` | N4 session id / super-session id | `FoxSession.java:505-506` |
| `hostName` / `hostAddress` | local identity | `FoxSession.java:517-530` |
| `app.name` (+ legacy `app.version`,`vm.*`,`os.*`,`niagaraPlatformType`) | `Station` / `Workbench` etc. | `FoxSession.java:532-545` |
| `availableSchemes` / `defaultScheme` | (server, strict-auth only) comma list of auth schemes | `Tuner.java:440-450` |

`receiveHello` requires the peer's `fox.version` to **start with `"1.0"`** else `IOException("Unsupported
fox.version")` `[CERT]` `FoxSession.java:579-581`. The constant `FOX_VERSION_1_0_2 = new Version("1.0.2")`
`[CERT]` `FoxSession.java:49` is the **N4-vs-legacy-AX cut line**: a peer below 1.0.2 is treated as a
NiagaraAX legacy client, and `BFoxService.allowLegacyClients()` gates whether it is accepted at all
`[CERT]` `Tuner.java:397-405`. Cross-version Station↔Workbench below 4.0.0 is explicitly rejected with
`IncompatibleVersionException` `[CERT]` `FoxSession.java:593-604`. The hello can also be answered with a
`redirect` (→ foxs, §134.10) or `busy` (server at `maxServerSessions`, default 100) `[CERT]`
`FoxSession.java:568-572`, `Tuner.java:347-351`, `Fox.java:33`.

## 134.6 — The tune (login) state machine: command sequence `[CERT]`

After hello, authentication runs as a sequence of `fox`-channel ASYNC "tuning" frames
(`sendTuning`/`receiveTuning` are just ASYNC frames on channel `fox`) `[CERT]` `FoxSession.java:614-616,
622-647`. The server side (`Tuner.run`) and client side (`Tuner.openClient`) drive these commands in
lock-step:

| Step | Direction | `fox` command | Body | Cite |
|---|---|---|---|---|
| 1 | C→S, S→C | `hello` | §134.5 | `Tuner.java:96-104, 452` |
| 2 | S→C | `kerberos` | `useKerberos` bool | `Tuner.java:469-472`, client `Tuner.java:125-130` |
| 3 | C→S | `username` | `username`, `kerbKey`, extra attrs | `Tuner.java:147-152, 473-474` |
| 4 | S→C | `challenge` | `method` (scheme name), `keyExchangeMethods`, `keyExchangeCiphers` | `Tuner.java:557-567` |
| 5 | C→S | `clientKeyExchangeMethod` | chosen KDF/cipher (non-secure only) | `Tuner.java:206-208, 570` |
| 6 | C↔S | `authMessage1` / `authMessage2` | SCRAM handshakes (§134.7) | `FoxScramShaUtil.java:45-74, 166-177` |
| 7 | S→C | `welcome` \| `retry` \| `rejected` | success / try-next-scheme / fail | `Tuner.java:614-625, 692-693`, `receiveWelcome` `Tuner.java:312-340` |

Three auth modes are enumerated on `Fox` — `BASIC_AUTHENTICATION=0`, `DIGEST_AUTHENTICATION=1`,
`TRANSACTIONAL_AUTHENTICATION=2` `[CERT]` `Fox.java:17-19`. In N4 the live path is the **SCRAM digest
scheme** (`BDigestAuthenticationScheme`), with `BLegacyDigestAuthenticationScheme` for AX peers `[CERT]`
`Tuner.java:457-458, 541-542`; the scheme's Fox agent is `BFoxDigestClientAuthnHandler`, which delegates to
`FoxScramShaUtil.handleClientAuthentication` `[CERT]` `BFoxDigestClientAuthnHandler.java:36-46`. On final
failure the server sends `rejected` (with optional `authfail_reason`, e.g. `"User Lockout"`) and throws
`FoxAuthenticationException` `[CERT]` `Tuner.java:732-737`, `FoxSession.java:638-640`.

## 134.7 — AUTH digest: SCRAM-SHA-256, computed in nre.jar `[CERT]`

This is the highest-value finding and it is **fully static-knowable**. Fox itself carries no crypto — it
ships the SCRAM messages as base64 strings inside `authMessage1`/`authMessage2` tuning frames. The actual
digest is RFC-5802 **SCRAM-SHA-256**, computed by `com.tridium.nre.auth.ScramClient`/`ScramServer` over
the `NiagaraStationAlgorithmBundle`. Fox→SCRAM wiring `[CERT]` `FoxScramShaUtil.java:161-177`:

1. Client builds `ScramClient(NiagaraStationAlgorithmBundle.getInstance(), username, password)` `[CERT]`
   `FoxScramShaUtil.java:163-164`, then sends `authInput=authInputScram` + `authHandshake1 =
   client.createClientFirstMessage()` in `authMessage1` `[CERT]` `FoxScramShaUtil.java:167-170`.
2. Server replies `authHandshake1 = server.createServerFirstMessage(...)` (carries salt + iteration count
   + server nonce) `[CERT]` `FoxScramShaUtil.java:48, 64-66`.
3. Client sends `authHandshake2 = client.createClientFinalMessage(serverFirstMessage)` (carries the proof)
   `[CERT]` `FoxScramShaUtil.java:173-177`; server validates and returns `authHandshake2 =
   server.createServerFinalMessage(...)`, which the client verifies via `processServerFinalMessage`
   `[CERT]` `FoxScramShaUtil.java:69-74, 206-209`.

The **algorithm bundle** pins every primitive `[CERT]` `NiagaraStationAlgorithmBundle.java:5-41`:
`algorithmType="pbkdf2-sha256"`, KDF `"PBKDF2WithHmacSHA256"`, **key length 256 bits**, MAC `HmacSha256`,
digest `Sha-256`.

The **SCRAM message format** (`Scram` base) `[CERT]`:
- client-first-bare = `"n=" + username + ",r=" + clientNonce` `[CERT]` `Scram.java:31`; full client-first =
  `"n,," + bare` (GS2 header, no channel binding) `[CERT]` `ScramClient.java:71-72`.
- **clientNonce = base64(16 random bytes from `SecureRandom`)** `[CERT]` `ScramClient.java:67-69`.
- client-final-without-proof = `"c=biws,r=" + clientNonce + serverNonce` (`biws` = base64 of `n,,`)
  `[CERT]` `Scram.java:35`.
- authMessage = `clientFirstBare + "," + serverFirstMessage + "," + clientFinalWithoutProof` `[CERT]`
  `Scram.java:38-39`.

The **digest computation** (`ScramClient.createClientFinalMessage`) `[CERT]` `ScramClient.java:79-100`:
1. parse `i` (iteration count) and `s` (salt, base64) from serverFirstMessage `[CERT]`
   `ScramClient.java:82-83`.
2. **saltedPassword = PBKDF2WithHmacSHA256(password, salt, iterationCount, dkLen=256)** via
   `Pbkdf2.deriveKey` → `PBEKeySpec` + `SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")` `[CERT]`
   `ScramClient.java:30-34, 89`, `Pbkdf2.java:44-46`.
3. **clientKey = HMAC-SHA256(saltedPassword, "Client Key")** `[CERT]` `Scram.java:57-59`,
   `ScramClient.java:59`.
4. **storedKey = SHA-256(clientKey)** `[CERT]` `ScramClient.java:60` (`h()` `Scram.java:96-100`).
5. **clientSignature = HMAC-SHA256(storedKey, authMessage)** `[CERT]` `ScramClient.java:61`.
6. **clientProof = clientKey XOR clientSignature** `[CERT]` `ScramClient.java:62`, `Scram.java:102-114`.
7. emit `clientFinalWithoutProof + ",p=" + base64(clientProof)` `[CERT]` `ScramClient.java:94`.

Mutual auth: server signature `v=` = HMAC-SHA256(**serverKey = HMAC-SHA256(saltedPassword, "Server Key")**,
authMessage), checked constant-time by the client `[CERT]` `Scram.java:61-64`, `ScramClient.java:102-110`.
Username/password are SASLprep-normalized (`Normalizer.normalize(input, 5)`) and the username `=`/`,` are
escaped `=3D`/`=2C` `[CERT]` `Scram.java:86-94`. **Conclusion:** the Fox login proof is a standard
SCRAM-SHA-256 over a PBKDF2(HMAC-SHA-256, 256-bit) salted password — no Tridium-proprietary digest. This
is static-complete; only the *runtime* salt/iteration values and the live byte trace need a capture
(§134.11).

## 134.8 — Optional SRP6 key exchange + session-key data encryption `[CERT]`

On a **non-secure (plaintext 1911)** connection that is not legacy, the challenge advertises
`keyExchangeMethods` + `keyExchangeCiphers` `[CERT]` `Tuner.java:559-561`; if both sides agree, after SCRAM
a second SRP6 round runs (`srp6ClientA`/`srp6ServerB`/`srp6M1`/`srp6M2` tuning messages) `[CERT]`
`FoxScramShaUtil.java:105-148, 210-242`, keyed by the SCRAM **saltedPassword** as the shared secret
(`keyExchanger.doInitialStep(saltedPassword)`) `[CERT]` `FoxScramShaUtil.java:84-85, 183-187`. The result
is a negotiated **session key** (`session.setSessionKey`) `[CERT]` `FoxScramShaUtil.java:141-146, 239-241`
used to encrypt sensitive data over the otherwise-plaintext socket — `supportsKeyExchange()` is true only
for a non-legacy connection with a real (non-`Null`) key-exchange bundle `[CERT]` `FoxSession.java:422`,
and `supportsSecureData() = isSecure() || supportsKeyExchange()` `[CERT]` `FoxSession.java:482-483`. On a
**TLS (foxs/4911)** connection the key exchange is skipped — the challenge sends
`keyExchangeMethods = NullAlgorithmBundle` because the TLS layer already provides confidentiality `[CERT]`
`Tuner.java:559-564`.

## 134.9 — Multiplexing: named channels carry numbered circuits `[CERT]`

Fox multiplexes two ways over one socket. **(1) Named channels** — the `channel` field of every frame
(§134.2) routes to a handler: `fox` is the built-in tuning/control channel (`processFoxChannelRequest`
handles `ping`/`close`) `[CERT]` `FoxSession.java:769-785`; other channels are registered `BFoxChannel`
subclasses with fixed names — **`sys`** (`BSysChannel`, super `"sys"`), **`user`** (`BUserChannel`),
**`spy`** (`BSpyChannel`), **`broker`** (`BBrokerChannel`), **`data`** (`BDataChannel`), and **`file`**
(`BFileChannel`) `[CERT]` `BSysChannel.java:82`, `BUserChannel.java:38`, `BSpyChannel.java:28`,
`BBrokerChannel.java:149`, `BDataChannel.java:117`. `SessionDispatcher.dispatch` forks on
`frame.channel=="fox"` vs delegating to the connection's channel processor `[CERT]`
`SessionDispatcher.java:61-67`.

**(2) Numbered circuits** — bulk/streaming transfers ride a virtual byte-stream multiplexed as a `FoxCircuit`
identified by an integer `id`. `SessionCircuits.alloc` assigns ids with a **parity-by-role / step-2** rule:
server starts at `0`, client at `1`, each `+= 2` `[CERT]` `SessionCircuits.java:29, 33-34` — so client
circuits are odd, server circuits even, guaranteeing no id collision without negotiation. A circuit is
driven by three commands on the **`circuit` channel** `[CERT]` `SessionCircuits.java:70-81`,
`FoxCircuit.java:220-250`:

| Command | Body | Effect | Cite |
|---|---|---|---|
| `open` | `id`, `channel`, `command`, `metadata` | allocate a stream + spawn a service thread | `FoxCircuit.java:220-227`, `SessionCircuits.java:83-98` |
| `stream` | `id`, `data` (Blob) | push a chunk into the circuit's input buffer | `FoxCircuit.java:229-242`, `SessionCircuits.java:100-111` |
| `close` | `id` | tear down | `FoxCircuit.java:244-250`, `SessionCircuits.java:113-123` |

Stream payload is chunked at **`Fox.circuitChunkSize` = 4096 bytes** with a per-circuit receive backlog
cap **`Fox.circuitMaxReceiveBuffer` = 102400 bytes** (flow control: `pushIn` blocks the sender when the
backlog is full) `[CERT]` `Fox.java:35-36`, `FoxCircuit.java:70-93, 147-168`. Each `stream` command is
itself an ordinary ASYNC frame carrying a FoxBlob (§134.4), so circuit bytes are wrapped twice: raw bytes →
FoxBlob → `circuit`/`stream` frame. Inbound circuits are serviced by a 2-thread pool
(`SessionCircuits.THREAD_POOL_SIZE=2`, overflow → non-pooled threads) `[CERT]` `SessionCircuits.java:16,
161-191`. This is the wire realization of the session muxing B27/B13 described at the architecture level.

## 134.10 — Plaintext (fox/1911) vs TLS (foxs/4911) selection, redirect, multicast discovery `[CERT]`

The two ORD schemes pin the default ports: **`BFoxScheme` (`fox`) → `DEFAULT_PORT = 1911`** `[CERT]`
`BFoxScheme.java:26, 75-77` and **`BFoxsScheme` (`foxs`) → `DEFAULT_PORT = 4911`** `[CERT]`
`BFoxsScheme.java:26, 75-77`; both build a `BFoxSession` differing only by a `secure` boolean
(`BFoxSession.make(..., false)` vs `..., true`) `[CERT]` `BFoxScheme.java:56` / `BFoxsScheme.java:56`.
**Security at the wire is purely "is the socket an `SSLSocket`"**: `FoxSession.isSecure()` returns
`this.socket instanceof SSLSocket` `[CERT]` `FoxSession.java:478-479` — i.e. foxs is the *same* Fox framing
of §134.2–§134.9 running inside a TLS-wrapped socket, **not a different on-stream format**. The socket's
input/output are wrapped only in `BufferedInputStream`/`BufferedOutputStream` (no Fox-level encryption
layer) `[CERT]` `FoxSession.java:137-139`; the chunked stream classes B129 referenced for `platform.fox`
are the platform daemon's own `MessageClient`, distinct from this Fox `FoxCircuit`/`MessageReader` stream
stack — Fox's circuit chunking (§134.9) is the analogous mechanism on the station-protocol side.

**Foxs-only redirect.** If the server is configured foxs-only, a plaintext `fox` connection gets a
`fox`/`redirect` tuning frame carrying the foxs port, and the client throws `FoxsRedirectException(port)`
to retry on TLS `[CERT]` `Tuner.java:409-413`, `FoxSession.java:495-499, 568-569`.

**Multicast discovery.** `Fox` also defines a station-discovery multicast group **`224.0.1.84`**
(IPv6 `FF02::137`) on **port 1911**, TTL 4 `[CERT]` `Fox.java:20-22, 39` (handled by `MulticastServer`/
`MulticastUtil`) — the same 1911 number as the unicast plaintext port, but UDP multicast.

## 134.11 — Self-verify

- **Token check**: grep-confirmed **all 28 load-bearing `[CERT]` tokens** present in their cited source
  (run in §134.2/§134.7 grep passes). Frame envelope: `FoxFrame.java:45-64` (`write(102/111/120/32)`,
  `write(59).write(59).write(10)`, `consume("fox ")`); 7 opcodes `FoxFrame.java:13-19`; sequencing
  `FoxSession.java:140-141, 900-907`; keepalive `SessionSender.java:9`; body codec `FoxTuple.java:11-71`,
  `FoxMessage.java:32-52`, `FoxBlob.java:40-50`, `MessageWriter.java:102-120` (writeSafe `#hex;`/`#null;`);
  hello `FoxSession.java:503, 579-581`, version `FoxSession.java:49`; tune commands `Tuner.java:96-104,
  452, 469-474, 557-567, 614-625`; SCRAM literals `Scram.java:31, 35, 38-39, 57-64`, `ScramClient.java:59-
  62, 67-72, 89-94`, `NiagaraStationAlgorithmBundle.java:19-25` (`PBKDF2WithHmacSHA256`, key 256),
  `Pbkdf2.java:44-46`; ports `BFoxScheme.java:26`, `BFoxsScheme.java:26`; `isSecure`
  `FoxSession.java:478-479`; circuits `SessionCircuits.java:29-34, 70-81`, `FoxCircuit.java:220-250`,
  `Fox.java:35-36`; multicast `Fox.java:20-22`.
- **Marker tally**: ~78 `[CERT]` · 0 `[CERT-doc]` · 0 `[CERT-web]` · 0 `[CERT-a]` · 0 standalone `[INFER]`.
  **[INFER]/[CERT] ratio ≈ 0.00** — the entire Fox wire (framing, opcodes, body codec, multiplexing,
  handshake, and the full SCRAM-SHA-256 digest) is source-confirmed across `fox-rt.jar` + `nre.jar`. The
  one genuinely non-static item is the **live byte trace** of an actual handshake (the concrete salt,
  iteration count, nonces and proof bytes on the socket) — registered as a `requires-execution` gap
  (§134.x / RESEARCH-STATE), NOT padded with inference.
- **Artifacts**: block file written; `sources/decompiled/fox-rt/` (111 classes) and
  `sources/decompiled/nre-auth/` (7 SCRAM classes) preserved; `SOURCES.md` (fox-rt sha256 7d7d144f…,
  nre.jar sha256 33aaaac5…), `INDEX.md` Layer 26, `RESEARCH-STATE-protocols.md` updated; CATALOG
  regenerated.

## 134.x — Connections

- **[Block 1]** — Fox/station architecture overview: B1 established Fox as the station protocol and the
  1911/4911 port split at the session level; B134 supplies the actual frame envelope (`fox ` line header),
  the 7 opcodes, and the body codec those sessions speak.
- **[Block 13] / [Block 27]** — Fox session muxing / channel architecture: B13/B27 described logical
  channels and session multiplexing conceptually; B134 §134.9 pins the wire mechanism — named `BFoxChannel`
  routing (`sys`/`user`/`spy`/`broker`/`data`/`file`) plus numbered `FoxCircuit` streams (parity-by-role
  ids, 4096-byte chunks, 100 KB backlog flow control) over the `circuit` channel.
- **[Block 129]** — platform.fox daemon (platform 3011/5011, `MessageClient`/chunked streams): B129
  documented the *platform* daemon's own protocol surface; B134 documents the *station* Fox protocol
  (1911/4911). They are siblings, not the same stream stack — Fox's `FoxCircuit` chunking (§134.9) is the
  station-side analogue of the platform daemon's chunked `MessageClient`; both are Tridium line/text-framed
  but in distinct packages.
- **[Block 131] / [Block 132] / [Block 133]** — sibling wire-level protocol blocks. **Sharpest contrast**:
  Modbus/OPC-UA/BACnet are **binary** PDU encodings (fixed-offset fields, big- or little-endian integers);
  Fox is uniquely a **line-oriented text envelope** (`fox <type> <seq> <reply> <chan> <cmd>\n{…};;\n`) with
  binary only inside FoxBlob `[len][bytes]` spans. And where the field-bus protocols carry no auth on the
  wire, Fox embeds a full **SCRAM-SHA-256** login (§134.7) — the only one of the four protocols with a
  cryptographic authentication handshake encoded in the shipped Java.
