# Block 384 — `nverify.exe` NG1-G1 closed: the three untraced `skip-*` flags placed at their enforcement gates — `--skip-signature-check` jumps past the ENTIRE verify

> **Focus `platform-native` — Ghidra sub-pass NG1-G1 (child gap of [B379]).** B379 documented `nverify.exe`'s
> eleven options and traced four `skip-*` flags to their parser globals, but left THREE gate sites untraced:
> `skip-signature-check` (`DAT_14007c03c`), `skip-cert-validity` (`DAT_14007c03f`), `require-timestamp`
> (`DAT_14007c03b`). This block places all three by call-graph, confirming B379's "total bypass" claim at the
> instruction level. READ-ONLY. Small evidence block (call-graph placement).
>
> Sources (primary): `/mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162/bin/nverify.exe` (PE32+ x64, base
> `0x140000000`, sha256 in evidence). Method: radare2 6.1.6 full-analysis (`r2 -A`) data/call xrefs
> (`axt`) over the flag globals and their getter bank; cross-read against [B379]'s decompiled functions.
> Raw evidence: `audits/B384-nverify-gatesites.txt`.
> Markers: `[CERT]` observed via r2 xref/disasm (address cited) · `[INFER]` deduction.
>
> Native platform layer (Capa 25). Closes the child gap [B379] opened (NG1-G1); the four already-traced
> flags and the option catalog are [B379].

---

## 384.1 — The flag getter bank `[CERT]`

All eight parser flags are read through a contiguous getter bank at stride `0x10`, each a two-instruction
`movzx eax, byte [DAT]; ret` (r2 disasm, `audits/B384-nverify-gatesites.txt`): `[CERT]`

| Getter | Global | Flag |
|---|---|---|
| `0x1400091e0` | `c03b` | require-timestamp |
| `0x1400091f0` | `c03f` | skip-cert-validity |
| `0x140009200` | `c03e` | skip-root-cert |
| `0x140009210` | `c03c` | skip-signature-check |
| `0x140009220` | `c03d` | skip-tpk-check |
| `0x140009230` | `c040` | trusted-certificates (ptr) |
| `0x140009240` | `c048` | unsigned-entry-whitelist (ptr) |
| `0x140009250` | `c03a` | validate-all-signatures |

(The four `0x140009200/220/240/250` getters were already mapped in [B379 §379.2]; NG1-G1 adds the three at
`0x1400091e0/1f0/210` and their gate sites below.) The globals are all zero-initialized by the parser
`fcn.140009260` (`0x140009272`-`0x1400092b1`), which [B379 §379.1] decompiled. `[CERT]`

---

## 384.2 — The three gate sites `[CERT]`

r2 `axt` over each getter gives its single caller = the enforcement site: `[CERT]`

- **`skip-signature-check` (`c03c`, getter `0x140009210`) → `fcn.140005070` @ `0x14000508e`.** This is the
  per-archive verify orchestrator (itself called from `fcn.1400050f0`/`fcn.1400051b0`). Its logic
  (`audits/B384-nverify-gatesites.txt`, disasm): `[CERT]`
  ```asm
  0x14000508e  call fcn.140009210        ; skip-signature-check?
  0x140005093  test al, al
  0x140005095  jne  0x1400050c8          ; if set -> jump PAST the whole verify, return
  0x14000509f  call fcn.14000ac10        ; else: discover signed manifest  ([B379] FUN_14000ac10)
  0x1400050b7  call fcn.14000a580        ; else: per-entry SHA-256 + unsigned-whitelist ([B379] FUN_14000a580)
  ```
  So `--skip-signature-check` makes the orchestrator **branch over both the manifest-discovery AND the
  per-entry integrity loop**, returning success without any signature or digest check. This is the gate-level
  confirmation of [B379 §379.1]'s "total bypass, stronger than `--unsigned *`": the wildcard still runs
  `fcn.14000a580` (and only whitelists unsigned entries), whereas `skip-signature-check` never enters it. `[CERT]`
- **`skip-cert-validity` (`c03f`, getter `0x1400091f0`) → `fcn.140001cc0` @ `0x140001d5e`.** `fcn.140001cc0`
  is the per-certificate X.509 verify that [B379 §379.3]'s chain validator (`FUN_140002b50`) calls for each
  link; the flag gates the **certificate validity-date (notBefore/notAfter) check** inside it. So
  `--skip-cert-validity` accepts expired/not-yet-valid certs while still checking chain/trust. `[CERT]/[INFER]`
  (CERT: the read is inside the per-cert verify `fcn.140001cc0`; INFER: that it gates the date check
  specifically, from the flag's help "Skip checking the validity dates of certificates").
- **`require-timestamp` (`c03b`, getter `0x1400091e0`) → `fcn.140002500` @ `0x140002a69`.** `fcn.140002500`
  is the signature-file (.SF/PKCS#7) validation function (the timestamp-EKU path [B126 §126.4] saw in
  strings); the flag makes a present-and-valid timestamp **mandatory** rather than optional. `[CERT]/[INFER]`.

---

## 384.3 — Closing note `[CERT]`

NG1-G1 is closed: the three flags are placed at `fcn.140005070` (skip-signature-check, the archive
orchestrator), `fcn.140001cc0` (skip-cert-validity, per-cert verify), and `fcn.140002500` (require-timestamp,
.SF validation). The security picture from [B379 §379.7] is unchanged but now gate-complete: **the four
`skip-*` flags peel off, in order of severity, the whole verify (`skip-signature-check`), the TPK pin
(`skip-tpk-check`, [B379 §379.4]), the root-trust search (`skip-root-cert`), and the validity dates
(`skip-cert-validity`)** — each a single flag, each read once through the getter bank. `[CERT]`

---

## 384.4 — Self-verify

**Token re-checks** (r2 xref/disasm, `audits/B384-nverify-gatesites.txt`):
1. Getter `0x140009210` = `movzx eax,[c03c]`, caller `fcn.140005070 @ 0x14000508e` — ✓.
2. Getter `0x1400091f0` = `[c03f]`, caller `fcn.140001cc0 @ 0x140001d5e` — ✓.
3. Getter `0x1400091e0` = `[c03b]`, caller `fcn.140002500 @ 0x140002a69` — ✓.
4. `fcn.140005070`: `call 0x140009210; test al,al; jne 0x1400050c8` over `call fcn.14000ac10` + `call fcn.14000a580` — ✓ (disasm).
5. Parser `fcn.140009260` zero-inits `c03a..c03f` (`0x140009272`-`0x1400092b1`) — ✓.

**5/5 tokens re-verified.**

**Marker tally**: `[CERT]` ≈ 10 · `[INFER]` 2 (which specific sub-check `skip-cert-validity`/`require-timestamp`
gate — from the flag help text + containing function). Ratio ≈ 0.20 — low; EVIDENCE block (call-graph). This
CLOSES B379's child gap NG1-G1; no re-derivation.

---

## 384.x — Connections

- **[B379]** — closes its NG1-G1 child gap; `fcn.14000ac10`/`fcn.14000a580` are B379's `FUN_14000ac10`
  (manifest) / `FUN_14000a580` (per-entry), and `fcn.140002b50` (chain) calls `fcn.140001cc0` (per-cert).
  Confirms §379.1's "`--skip-signature-check` = total bypass" at the gate.
- **[B126]** — `fcn.140002500` (.SF/timestamp validation) is the decompiled form of §126.4's timestamp-EKU strings.
- **Remaining reopened focus**: NG2b (`nre.dll` ~104 NativePlatformProvider native bodies) — the last
  investigable Ghidra sub-pass gap.
