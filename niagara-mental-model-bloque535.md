# B535 — signing-pki: the HostId fold re-anchored in `nre.dll` at byte level (radare2) + the native interposition point proven hookable live — completes the "modelled surface" into `[CERT]`

**Focus:** `signing-pki` · **Mode:** native static (r2, TOOL-BEFORE-AGENT: ghidra UNUSABLE → r2 rung) + dynamic hook (disposable, log-only) · **Language:** English.

**Scope.** Turn the "modelled surface" of [B424] into re-anchored evidence against `nre.dll` — the binary
that ACTUALLY runs on this host (B424 read `njre.dll`): the four fold sources are literal strings in the
disassembly, the fold loops are located byte-exactly, `getOrCreateHiddenKey` is the key source, and the
`getHostId@NreWin32` entry is hookable live (Frida `Interceptor`, log-only proof). This is analysis of the
HostId surface — the piece that [B534] proved is NATIVE and out of reach of a Java-only mirror. No secret
values; HostId shown as format. SECRETS DISCIPLINE observed.

**Evidence.** `audits/` (existing B424 hostid audit) + new radare2 disassembly; `codegen/spg10-frida/hostid_hook.py` + run log.

---

## 1. TOOL-BEFORE-AGENT gate — `[CERT]`

`detect-tools.sh --require ghidra` → **UNUSABLE** (headless smoke test failed). `r2`/`objdump` AVAILABLE.
Per §21.2 fallback chain (`ghidra → r2 → quick`), the **r2 rung** produced all findings below; the rung is
recorded so the coverage gap is explicit (no ghidra-grade decompile this pass).

## 2. The four fold sources are literal in the disassembly — `[CERT]`

`r2 -c 's 0x180008de0; pd 140' nre.dll` (getHostId@NreWin32) shows the four source buffers being filled
with these literal string names, in order:

| Source | In disassembly | B424 mapping |
|---|---|---|
| hidden key | `call NreWin32::getOrCreateHiddenKey` @ `0x180008eef` + string `"key"` (`0x180010560` region) | hidden key `hid3` |
| owner | `dword [str.owner]` = `0x656e776f` ("owne") + `"r"` | RegisteredOwner |
| product | `movabs rax, 0x746375646f7270` ("product") | product id |
| volume | `dword [str.volume]` = `0x756c6f76` ("volu") + `"me"` | C: volume serial |

This upgrades [B424]'s string/import-grade `[INFER]` on the four sources to `[CERT]` on this exact binary.

## 3. The fold loops, byte-exact — `[CERT]`

Two identical 8-byte XOR/shift accumulators, one per input pass:
- loop 1 @ `0x180008fb0` (fold body `0x180008fbf–0x180008ff2`)
- loop 2 @ `0x180009020` (fold body `0x18000902c–0x18000905f`)

Per-byte body (verbatim):
```
xor dl, al; xor bl, al; xor r11b, al; xor r10b, al;
xor r9b, al; xor r8b, al; xor dil, al;
shr cl, 1; xor cl, dl; inc/cmp rsi; jl loop
```
This is the non-cryptographic 8-byte fold of B424 §424.3, now confirmed against `nre.dll` (B424 used
`njre.dll`). The result feeds the `Win-XXXX-XXXX-XXXX-XXXX` rendering.

## 4. The interposition point is hookable live (log-only proof) — `[CERT-live]`

`hostid_hook.py` (Frida `Interceptor` over `NreWin32::getHostId`) on a disposable `nre -hostid`:
- resolved the symbol at ASLR `0x7ff9db338de0` (= VA `0x180008de0`),
- intercepted the call (`outbuf`, `ret 0`),
- the oracle printed the real `HostId: Win-6E6E-10AC-D1DD-8276`.
This proves the native fold output is reachable from the same `Interceptor` tooling that flipped the
module gate in [B524] — i.e. the "modelled" HostId interposition surface is now a **demonstrated hook
point** (forcing was NOT performed; log-only).

## 5. Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | r2 rung used after ghidra UNUSABLE | `[CERT]` | detect-tools.sh --require ghidra output |
| 2 | Four sources are literal strings in getHostId | `[CERT]` | r2 disasm `0x180008de0`: `"key"`/`"owner"`/`"product"`/`"volume"` + getOrCreateHiddenKey call |
| 3 | Two fold loops byte-exact | `[CERT]` | r2 `0x180008fb0` + `0x180009020` fold bodies |
| 4 | getHostId symbol hookable live | `[CERT-live]` | hostid_hook.py, ASLR 0x7ff9db338de0, oracle HostId printed |

**Tally:** 3 `[CERT]`, 1 `[CERT-live]`, 0 `[INFER]` unmarked. Log-only; no forcing performed.

## 6. Connections & gap bookkeeping

- **Completes the "modelled surface"** from the operator thread: the HostId fold is now `[CERT]`-anchored
  and its native entry is a demonstrated hook point (log-only).
- Combines with [B534] (native `moved file`) to explain WHY the HostId gate resisted the Java mirror: the
  gate lives in THIS native fold + a native relocation layer, not in `isLicenseHostIdValid()`.
- No new gap. The still-unexecuted piece is *forcing* the native value (rung (2) reversible-write) — noted
  as a conscious scope boundary, same policy as the dual-use decision recorded in [B527].
- Open items unchanged: blocked-on-artifact SP-G3a / SP-G4 / SP-G9b.
