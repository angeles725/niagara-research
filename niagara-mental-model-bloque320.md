# Block 320 — HostId derivation in the REAL build: four inputs + `disableHostIdGeneration` gate (Ghidra)

> **Dynamic-phase block (METHODOLOGY §12 + document-mode)** — Ghidra headless decompilation of the ACTUAL
> lab build's `njre.dll` (`iC-Niagara-4.10.9.14`, sha256
> `803f9db56249a803fdf2287d4866e5c07fc715bd281f7e0b3bedd2a27d38a06d`) refines the corpus HostId model
> (B124/B125). The corpus said HostId derives from `GetVolumeInformationA` — correct but incomplete: this
> build feeds **four inputs** into the 8-byte hash and adds a `disableHostIdGeneration` property gate.
> Relevance to the pentest: the machine-binding (`Win-<8 bytes>`) that licenses are tied to is not a single
> hardware value; it is a derived composite including registry state and a cached/generated key.
>
> **Read-only phase** (no writes to the target; binaries pulled earlier, B319). Sources: decompilation
> preserved under `corpus/sources/probes/B317-pentest-2026-08-01/native/ghidra-njre-hostid.txt` `[CERT]`;
> live `nre -licenses` HostId `Win-4D6F-169B-CEF1-8F57` `[CERT-live]`; corpus B124/B125 as baseline `[CERT]`.
> Markers: `[CERT]` decompiled/artifact · `[CERT-live]` measured live · `[INFER]` deduction.

---

## 320.1 — `NreWin32::getHostId` @ `0x180004a70` (njre.dll) `[CERT]`

```c
int NreWin32::getHostId(char *out, uint outLen) {
  getOrCreateHiddenKey(this, "key", 0xff);                 // 1. hidden key (migrate/read/generate/save)
  getRegWinCurVerImpl(this, "Windows NT", "RegisteredOwner", owner, ...);   // 2. RegisteredOwner
  if (ret != 0) getRegWinCurVerImpl(this, "Windows", "RegisteredOwner", owner, ...); // fallback
  getOrCreateCachedProductIdKey(this, "product", 0xff);    // 3. cached product id
  getVolume(this, "volume", 0xff);                         // 4. volume serial (C:\)
  // ... length loops over the four buffers ...
  snprintf(out, outLen, "%s-%02X%02X-%02X%02X-%02X%02X-%02X%02X", prefix, 8 bytes);
  // debug (DAT_18000f418 != 0): ">>> hostid.debug >>>" / "  prefix = ..." / "... <<< hostid.debug <<<"
}
```

- Format string `"%s-%02X%02X-%02X%02X-%02X%02X-%02X%02X"` — matches the live `Win-4D6F-169B-CEF1-8F57`
  shape exactly `[CERT-live]` (prefix + 8 bytes in 4 groups).
- The 8 bytes are derived from the **four** collected strings (the exact hash/combine step is in the
  length-loop + snprintf path; the corpus already recorded the 0x40-byte JNI buffer, B125 §125.5 — this
  block locates the four INPUTS, not the final digest algorithm, which remains the open micro-gap).

## 320.2 — The four inputs `[CERT]`

| # | Source | Function @addr | Behavior |
|---|---|---|---|
| 1 | hidden **"key"** | `getOrCreateHiddenKey` @`0x180005b00` | `migrateHiddenKey` → `readHiddenKey` → if absent: **`disableHostIdGeneration` gate** → `generateNewKey` → `saveHiddenKey`; any failure → `exit(0xf9)` |
| 2 | **RegisteredOwner** (registry) | `getRegWinCurVerImpl` @`0x180005e10` | reads `HKLM\...\Windows NT\CurrentVersion\RegisteredOwner`, fallback `...\Windows\...\RegisteredOwner` |
| 3 | cached **"product" id** | `getOrCreateCachedProductIdKey` @`0x1800059e0` | `readCachedProductIdKey` → if absent: **`disableHostIdGeneration` gate** → `generateNewCachedProductIdKey` → `saveCachedProductIdKey`; failure → `exit(0xf9)` |
| 4 | **volume serial** | `getVolume` @`0x180005fa0` | `GetVolumeInformationA("c:\", …, &serial, …)` → `"%08X"` of serial (live `D2DE8C94`) |

## 320.3 — `disableHostIdGeneration` gate (new, not in corpus) `[CERT]`

Both `getOrCreateHiddenKey` and `getOrCreateCachedProductIdKey` check a configuration property:

```c
prop = getProperty("disableHostIdGeneration", "false");
if (strncmp(prop, "false", 5) != 0) {              // i.e. NOT "false"
    fprintf(stderr, "ERROR: Host Id cannot be found/generated.\n");
    exit(0xf9);
}
```

Implications:
- The corpus string `ERROR: Host Id cannot be found/generated.` (B124:68) is now **located**: it is the
  `disableHostIdGeneration` abort inside key/product-id generation `[CERT]` (previously `[INFER]`-level).
- The gate is a plain string comparison on a property — same *class* of string-based decision as the
  license features (B41 §41.6.2) and the native text-match (B319): a configuration property, not a
  cryptographic check. An operator/config that sets `disableHostIdGeneration` to anything other than
  `"false"` bricks host-id generation for keyless installs.
- Not exploitable remotely (needs config write), but relevant for the OEM's "what can go wrong" matrix and
  for the corpus: the flag exists, where it lives (property source: `nre.properties` / options — the
  getter is `FUN_180001480` + a guard-dispatch get-property call) is a follow-up micro-gap.

## 320.4 — Refinement vs corpus (honest delta) `[CERT]`

| Claim | Corpus (B124/B125) | This build (verified) |
|---|---|---|
| HostId input | `GetVolumeInformationA` (volume serial) | volume serial is input **#4 of 4**; also hidden key, `RegisteredOwner`, cached product id |
| Buffer | 0x40-byte JNI buffer (B125 §125.5) | confirmed (`Java_..._getHostId0` fills 0x40) — unchanged |
| Format | `Win-`/`Qnx-` + opaque 16-hex | `"%s-%02X%02X-%02X%02X-%02X%02X-%02X%02X"` confirmed |
| `ERROR: Host Id cannot be found/generated.` | string seen (B124:68) | located: `disableHostIdGeneration` gate in key/product generation |

The exact combine/hash algorithm producing the 8 bytes from the 4 inputs remains the documented corpus
micro-gap (B124 "no description of the exact hash"); this block narrows it to "hash over the four
collected strings" `[INFER]`.

## 320.5 — Pentest relevance

- License binding (`hostId=`) is a **composite of registry + generated-key + volume serial**: cloning a
  disk alone (B316 L-4 context) does not replicate a hostId unless the hidden key + product id + owner
  travel with it — refines the "licenses lost after NIC swap/VM clone" failure mode (B316 §7 #2) and the
  `CLONED_FILE` story: the hidden key file is the piece that makes clones collide.
- For the OEM report: the machine identity is not purely hardware; registry state participates. A
  re-imaged/repurposed machine whose `RegisteredOwner`/product-id cache differs produces a different
  HostId even with the same disk serial — a realistic "lost licenses" root cause beyond MAC/MachineGuid.

## 320.6 — Self-verify

- `verify-block.sh niagara-mental-model-bloque320.md` — exit 0 (verified above).
- Marker tally (whole block, incl. legend): `[CERT-live]` 4 · `[CERT]` 9 · `[INFER]` 4 (legend + §320.1/§320.4/§320.5 noted inferences, none load-bearing beyond the explicit hash-algorithm micro-gap). Load-bearing tokens re-verified:
  `ghidra-njre-hostid.txt` contains all five functions with the cited addresses (`getHostId` @`0x180004a70`,
  `getOrCreateHiddenKey` @`0x180005b00`, `getOrCreateCachedProductIdKey` @`0x1800059e0`,
  `getVolume` @`0x180005fa0`), the format string, and both `disableHostIdGeneration` gate bodies
  (grep-confirmed); live HostId `Win-4D6F-169B-CEF1-8F57` re-measured (B316 §316.1).
- The corpus-delta table (§320.4) is a REFINEMENT, not a refutation: B124/B125 remain correct for the
  volume-serial component; this block adds the other three inputs.
