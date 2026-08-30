# B682 — How QNX is invoked on the JACE-8000, and why its OS image can't be unpacked offline: the boot chain ROM→MLO→U-Boot→`go 0x80FFFC00` executes the CertISW-wrapped `n4-titan-am335x.signed`, whose payload is OPAQUE — entropy ≈ 7.998 bits/byte, zero binwalk signatures ⇒ ENCRYPTED (not merely signed), so the QNX IFS/procnto image is not statically extractable from the SD; the running OS is QNX Neutrino 7.0.0 on ARM (focus jace8000-qnx-native, QN6; §19 [CERT] + blocked child)

> **Focus:** `jace8000-qnx-native` (§16). **Gap addressed:** QN6 (how QNX boots/is invoked; unpack the OS
> image). **Phase:** static RE, READ-ONLY. **Marker:** `[CERT]` for the measurements + chain; `[INFER]` for
> "encrypted".
> **Sources:** `sources/probes/B672-jace8000-sd/qn6-qnx-payload-entropy.txt` · the payload
> `local-sd-image/…/n4-titan.signed` (gitignored) · `[CERT]` [Block 672] (boot chain), [Block 675]/[Block 676]
> (CertISW header), [Block 679] (QNX 7.0.0 from niagarad), [Block 459]/[Block 474] (QNX/OpenJDK).
>
> **Bottom line:** the JACE invokes QNX through the standard TI AM335x chain — ROM → `MLO` (SPL) → `u-boot.img`
> → `uEnv.txt` (`fatload … n4-titan-am335x.signed; go 0x80FFFC00`) → the CertISW-wrapped firmware ([Block 672]).
> But the firmware **payload is opaque**: measured entropy **≈ 7.998 bits/byte** across the whole payload and
> **binwalk finds zero signatures**, so the payload is **encrypted** (or headerless-max-compressed) — the QNX
> **IFS / procnto / startup / driver image cannot be extracted from the SD offline**. So the OS image is
> protected for **confidentiality**, not just integrity (a step beyond the "signed" finding of [Block 676]).
> What QNX *is* on this unit is still known from the running system: **QNX Neutrino 7.0.0 on ARM** ([Block 679]),
> OpenJDK ([Block 474]), with the resource-manager drivers of [Block 680]/[Block 681].

---

## §682.1 — The QNX invocation chain (end to end) `[CERT]`

From the boot artifacts already documented ([Block 672] §672.3):
```
AM335x ROM  →  MLO (SPL, /mlo)  →  u-boot.img  →  reads uEnv.txt:
   uenvcmd = mmcinfo; fatload mmc 0 0x80FFFC00 n4-titan-am335x.signed; go 0x80FFFC00
→  n4-titan-am335x.signed  (CertISW-wrapped: cert header ~0x350 + payload + sig, [Block 676])
→  (SPL/ROM verify + decrypt)  →  QNX Neutrino 7.0.0  →  procnto + startup → drivers → niagarad → JVM
```
`go 0x80FFFC00` is a **raw jump** to the loaded image (not `bootm`/uImage), consistent with a TI GP secure-boot
payload the earlier stages verify before executing ([Block 676] §676.3). Once QNX is up, `niagarad` (the
privilege-dropping JVM launcher, [Block 679]) starts the platform daemon, and the field-bus resource managers
(`/dev/ccn`, `/dev/bn-…`, MS/TP devctl — [Block 680]/[Block 681]) service I/O. That is "cómo se manda a llamar
a QNX y a Niagara" from cold boot.

## §682.2 — The payload is opaque: encrypted, not just signed `[CERT] measurement + [INFER] encryption`

Measured on `n4-titan-am335x.signed` (P3 factory copy, 27,149,316 B) `[CERT qn6-qnx-payload-entropy.txt]`:

| Region | Shannon entropy (bits/byte) | Reading |
|---|---|---|
| header `0x0–0x400` | 5.158 | structured (CertISW header) |
| `0x400–0x1000` | 7.936 | signature/key material |
| payload @ 1 MB | 7.997 | opaque |
| payload @ 13 MB | 7.998 | opaque |

`binwalk` reports **0 signatures / 0 embedded files** across the whole image `[CERT]`. Flat ~8.0-bit/byte
entropy with no gzip/lzo/uImage/IFS magic anywhere ⇒ the payload is **encrypted, or maximally compressed with
a stripped header**; a normal (even compressed) QNX IFS leaves detectable structure/headers, so **encryption
is the strong reading** `[INFER]`. Either way, the QNX **IFS/procnto image is NOT statically extractable from
the SD** — there is nothing to `dumpifs`/`binwalk` into.

**Security consequence:** the firmware is protected for **confidentiality** (the OS image is unreadable off
the device), which is *more* than [Block 676]'s "JAR-signed / GP-signed" integrity finding. Combined with the
ECC508 secure element ([Block 678]) and the machine-key domain ([Block 466]), the decryption key is almost
certainly device-bound — so unpacking needs the running unit or its key, not the card. `[INFER]`

## §682.3 — What remains (blocked child) `[CERT]`

- **QN6-G1 (blocked / requires-execution):** the actual QNX **IFS contents** — procnto version banner, the
  `startup-*` line, the boot script, the driver/resource-manager list (`io-pkt`, `devc-ser…`, `devb-…`) — are
  **behind the payload encryption**. Closing it needs either the running JACE (dump the live IFS / read
  `/proc/boot`, `pidin`, `uname -a` over serial), or the device-bound decryption key. `tried:` static
  entropy + binwalk on the SD payload → opaque; no offline unpack path from the card alone.
- The QNX **version/identity** is nonetheless established: **Neutrino 7.0.0** ([Block 679] crt strings; [Block
  459]/[Block 474] live), ARM (AM335x), OpenJDK.

## §682.4 — Self-verify

| # | Claim | Marker | Cite |
|---|---|---|---|
| 1 | Invocation chain ROM→MLO→u-boot→go 0x80FFFC00→CertISW payload→QNX→niagarad→JVM | [CERT] | [Block 672]/[Block 676]/[Block 679] |
| 2 | Payload entropy ≈ 7.997–7.998 bits/byte; header 5.158 | [CERT] | qn6-qnx-payload-entropy.txt |
| 3 | binwalk finds 0 signatures / no embedded IFS | [CERT] | binwalk run |
| 4 | ⇒ payload encrypted (or headerless-compressed); QNX IFS not extractable offline | [INFER] | §682.2 |
| 5 | Firmware protected for confidentiality, not only integrity (beyond B676) | [INFER] | §682.2 |
| 6 | Running OS = QNX Neutrino 7.0.0 / ARM / OpenJDK | [CERT] | [Block 679]/[Block 474] |
| 7 | IFS contents = blocked child (needs live device or device-bound key) | [CERT] | §682.3 |

**Tally:** 7 claims — 5 [CERT], 2 [INFER] (encryption + confidentiality reading, both grounded in the entropy
measurement). 0 unmarked.

## §682.5 — Connections

- **[Block 672]** — the boot chain (MLO/u-boot/uEnv) this executes.
- **[Block 676]** — the CertISW header (integrity/signing); this block adds the payload confidentiality.
- **[Block 679]** — QNX 7.0.0 + `niagarad` as the post-boot entry.
- **[Block 680]/[Block 681]** — the QNX drivers/watchdog that run once QNX is up.
- **[Block 678]** — the ECC508 secure element (likely the root for the payload's device-bound key).
- **[Block 466]** — the machine-key at-rest domain (same device-binding that blocks offline unpack).
