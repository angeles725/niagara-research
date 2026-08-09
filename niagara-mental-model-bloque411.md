# Block 411 — BOG Boot-Time Crash Recovery: `checkForWorkingFile`, Backup Naming, and Rename Semantics

> Research focus: `database` (gap **DB10**, low-priority). Answers the boot-time crash-recovery path
> for `config.bog`: whether the NRE checks for `.bog.bak` / `.bog.working` at station startup and
> recovers, the exact backup filename format (correcting B5 §5.2.8), and the Windows vs POSIX
> rename atomicity question left open by [Block 32] §32.9.5.
>
> Scope: `Station.checkForWorkingFile()` (recovery guard at boot), `Station.saveSync()` (save-path
> callees — Station.java:496-503), `FileUtil.renameToBackup()` (backup naming), and
> `BBogSpace.save()` (non-station BOG path). Does NOT re-derive the save trigger, dirty-flag chain,
> or BOG format — remitted to [Block 402] and [Block 5] respectively.
>
> Subject version: N4.14.0.162 (Vineflower decompiled; vendor docSource originals where available).
>
> Sources:
> - `[CERT]` Station.java (Vineflower): `/home/cristian/modules/Prototipos/modulos/organized/baja/baja/vineflower/com/tridium/sys/station/Station.java`
> - `[CERT]` BBogSpace.java (Vineflower): `/home/cristian/modules/Prototipos/modulos/organized/file/file-rt/vineflower/com/tridium/file/types/bog/BBogSpace.java`
> - `[CERT]` FileUtil.java (docSource): `/home/cristian/modules/Prototipos/modulos/organized/docSource/docSource-doc/extracted/nre/javax/baja/nre/util/FileUtil.java`
> - `[CERT]` Station.java (Procyon — corroboration): `/home/cristian/modules/Prototipos/modulos/organized/baja/baja/pipeline/procyon/com/tridium/sys/station/Station.java`
>
> Method: Vineflower decompile + vendor docSource originals; call chain traced from
> station-startup entry through `checkForWorkingFile()` → `bootStation()` → `loadStation()`.
> Scope-widened grep over all `organized/` modules to confirm no other BOG recovery site.
>
> Markers: `[CERT]` local primary source (`file:line`) · `[INFER]` deduction.
>
> `database` focus. Connects [Block 402] (save path — this block covers the boot-time recovery of
> the `.bog.working` artifact that save creates), [Block 5] (§5.2.8 — backup naming corrected;
> `.bog.bak` does not exist in code), [Block 32] (§32.9.5 open Windows-rename edge case —
> closed with code evidence here).

---

## 411.1 — Boot-Time Recovery Guard: `checkForWorkingFile()` `[CERT]`

A crash-recovery guard for `config.bog.working` **exists** in the NRE and fires at every station
boot before the config is loaded.

```java
// Station.java:582-589
private static void checkForWorkingFile(File bootFile) throws IOException {
    if (!bootFile.exists()) {
        File workingFile = new File(bootFile.getCanonicalPath() + ".working");
        if (workingFile.exists() && !workingFile.renameTo(bootFile)) {
            throw new IOException("Failed to rename working file");
        }
    }
}
```
`[CERT]` `Station.java:582-589`

It is called at two points in the station-startup sequence — one for the `--bootFile` command-line
option and one for the default `config.bog` path — **before** `bootStation()` is invoked:

```java
// Station.java:749-754 — --bootFile option path
try {
    checkForWorkingFile(bootFile);
} catch (IOException var9) {
    logger.log(Level.SEVERE, "FATAL: Could not rename working file for: " + bootFile, var9);
    System.exit(-4);
}

// Station.java:763-768 — default config.bog path
File bog = new File(Nre.protectedStationHome, "config.bog");
try {
    checkForWorkingFile(bog);
} catch (IOException var8) {
    logger.log(Level.SEVERE, "FATAL: Could not rename working file for: " + bog, var8);
    System.exit(-4);
}
```
`[CERT]` `Station.java:749-768`

**Startup order** (recovery precedes loading):

| Step | Call | Line |
|---|---|---|
| 1 | `checkForWorkingFile(bog)` | 763-768 |
| 2 | `bootStation(bootFile)` | 779 |
| 3 — inside bootStation | `loadStation(bootFile)` | 130 |
| 4 — inside loadStation | `ValueDocDecoder.decodeDocument()` | 174 |

`[CERT]` `Station.java:779, 130, 174`

A rename failure (returns `false` from `File.renameTo()`) causes `checkForWorkingFile()` to throw
`IOException`, which propagates to `System.exit(-4)` — the same exit code as "Station database not
found". The station never attempts to load a BOG in a partially-recovered state.
`[CERT]` `Station.java:585-586`

---

## 411.2 — Recovery Trigger Condition: Only When `config.bog` Is Absent `[CERT]`

The outer guard `if (!bootFile.exists())` determines when recovery can fire:

| State at boot | What `checkForWorkingFile()` does |
|---|---|
| `config.bog` present, `.working` absent | Returns immediately — **no recovery attempt** |
| `config.bog` present, `.working` also present | Returns immediately — **`.working` is orphaned** (old BOG loaded) |
| `config.bog` absent, `.working` present | Renames `.working` → `config.bog` — **recovery succeeds** |
| `config.bog` absent, `.working` absent | Returns silently; subsequent "database not found" check fires → exit -4 |

`[CERT]` `Station.java:582-583` (the `!bootFile.exists()` guard)

There is **no** code that checks for `config.bog.bak` (a file by that name never appears in any
of the decompiled or docSource trees). Recovery is exclusively for `.bog.working`.
`[CERT]` (absence across all `.java` files in `organized/` — grep for `bog.bak` returns zero results
in baja, file-rt, or any other module; the backup name format is different — see §411.4)

---

## 411.3 — The Orphan Scenario: Crash Before the Backup Rename `[CERT + INFER]`

The save sequence in `Station.saveSync()` produces two distinct crash windows ([Block 402] §402.3.3):

```
saveSync():
  Step A → encode to config.bog.working   (Station.java:484-495)
  Step B → renameToBackup(config.bog)     (Station.java:496-498)  ← backup rename
  Step C → workingFile.renameTo(saveFile) (Station.java:499-503)  ← restore from working
```
`[CERT]` `Station.java:484-503`

| Crash window | State on disk | `checkForWorkingFile()` result |
|---|---|---|
| During step A (partial write) | `config.bog` = OLD · `.working` = PARTIAL | config.bog exists → recovery skipped · OLD BOG loaded · `.working` orphaned |
| After A completes, before B | `config.bog` = OLD · `.working` = NEW | config.bog exists → recovery skipped · OLD BOG loaded · `.working` orphaned |
| After B completes, before C | `config.bog` ABSENT · `.working` = NEW · backup exists | **Recovery fires** → `.working` renamed → `config.bog` · NEW state recovered |
| After C completes | `config.bog` = NEW · `.working` ABSENT | Normal boot |

`[INFER]` The orphan-working-file scenario (crash in windows A or B-before-B) is **not
catastrophic**: the old `config.bog` is intact and is loaded. The last save is lost, but the
station boots cleanly from the prior state.

This is **not an NTFS-specific edge case** — it occurs equally on POSIX. It follows from the
`!bootFile.exists()` guard condition and the two-step rename pattern, not from filesystem
platform behavior.

---

## 411.4 — Backup File Naming: Not `.bog.bak` `[CERT]`

[Block 5] §5.2.8 used the approximate name `.bog.bak`. No such file ever appears in the code.
The actual format is produced by `FileUtil.renameToBackup()`:

```java
// FileUtil.java:313-325
String pattern = "yyMMdd_HHmm";
SimpleDateFormat format = new SimpleDateFormat(pattern);
format.setTimeZone(TimeZone.getDefault());
String ts = format.format(new Date(file.lastModified()));

File newFile = new File(parent, name + "_backup_" + ts + ext);
for (int i = 1; newFile.exists(); ++i)
    newFile = new File(parent, name + "_backup_" + ts + "_" + i + ext);
if (!file.renameTo(newFile)) throw new IOException("Cannot rename");
```
`[CERT]` `FileUtil.java:313-325`

**Resulting filename**: `config_backup_yyMMdd_HHmm.bog` (with `_N` collision suffix if needed).

The backup files are **not** crash-recovery artifacts — they are timestamped archives.
`Station.getBackups()` lists them at runtime via `FileUtil.getBackups()` using the `"_backup_"`
infix pattern:
`[CERT]` `Station.java:405-407`

The maximum number of retained backups is governed by `Nre.getPlatform().getStationSaveBackupCount()`:
`[CERT]` `Station.java:416`

---

## 411.5 — Non-Station BOGs: No Crash Recovery in `BBogSpace.save()` `[CERT]`

The crash-recovery guard above is **exclusive to `config.bog`** via `Station.saveSync()`. The
`BBogSpace.save()` path — used for `platform.bog`, palette files, and any other file-mounted BOG
space — writes directly to the file's output stream with no temporary file:

```java
// BBogSpace.java:243-252
BogEncoderPlugin plugin = new BogEncoderPlugin(this.bogFile.getOutputStream());
plugin.setPassPhrase(this.reversibleEncryptionPassPhrase);
ValueDocEncoder encoder = new ValueDocEncoder(plugin);
plugin.setBogPasswordObjectEncoder(this.bogPasswordObjectEncoder);
encoder.setZipped(true);
encoder.encodeDocument(this.getRootComponent());
encoder.close();
this.bogVersion = plugin.version();
this.bogPasswordObjectEncoder = plugin.getBogPasswordObjectEncoder();
this.modified = false;
```
`[CERT]` `BBogSpace.java:243-252`

No `.working` temp file, no `renameToBackup()` call, no crash-recovery guard at any level in
`BBogFile.java` or `BBogSpace.java`. A crash mid-save corrupts the target file with no automatic
recovery path.
`[CERT]` (absence of any working-file, bak, or recovery pattern in BBogSpace.java and BBogFile.java —
grep confirms zero hits for `working`, `bak`, `rename`, `recovery` across both files)

---

## 411.6 — Windows vs POSIX Rename Semantics `[CERT + INFER]`

Both the save path and the boot-recovery path use `java.io.File.renameTo()`:

| Call site | From | To | Line |
|---|---|---|---|
| Save (normal) | `config.bog.working` | `config.bog` | `Station.java:500` |
| Boot recovery | `config.bog.working` | `config.bog` | `Station.java:585` |
| Backup rename | `config.bog` | `config_backup_yyMMdd_HHmm.bog` | `FileUtil.java:325` |

`[CERT]` cited lines above

`FileUtil.move()` uses `Files.move(oldFile.toPath(), newFile.toPath(), StandardCopyOption.ATOMIC_MOVE)`
`[CERT]` `FileUtil.java:396` — but this method is **not called** in the BOG save or recovery path.
The BOG paths use the older `File.renameTo()` API. `[CERT]` (no `Files.move` call in Station.java)

**`File.renameTo()` characteristics**:
- Returns `false` rather than throwing when the rename fails.
- Java specification does NOT guarantee atomicity on any platform.
- On POSIX: delegates to `rename(2)`, which is atomic per POSIX standard.
- On Windows: delegates to `MoveFileExW(src, dst, 0)` (no `MOVEFILE_REPLACE_EXISTING`) — fails
  and returns `false` if the destination exists; for a non-existing destination, the NTFS journal
  makes it atomic at the metadata level.
  `[INFER]` (Java → native mapping from JDK source and platform documentation; not cited by code)

In the save and recovery paths, **the destination does not exist at rename time** — the backup step
removes `config.bog` before `.working` is renamed, so `REPLACE_EXISTING` is not needed. The
`REPLACE_EXISTING` concern raised in [Block 32] §32.9.3 does not apply to this code path.
`[CERT]` `Station.java:497-500` (backup precedes restore — ordering is explicit in the code)

**Failure handling**:
- Save path (Station.java:500-503): `false` return → `jobLog.endFailed(...)` → save fails; the
  station retries on the next scheduled cycle. `config.bog` is absent at this point (renamed to
  backup); `config.bog.working` is still on disk — boot recovery will handle it next restart.
- Boot path (Station.java:585-586): `false` return → `IOException` → `System.exit(-4)`.
`[CERT]` `Station.java:500-503, 585-586`

---

## 411.7 — Back-pointer: B32 §32.9.5 Open Question Closed `[CERT]`

[Block 32] §32.9.5 left this open: *"Si crash DURANTE rename en Windows → NTFS marker files
posibles corruption edge case (raro pero documentado en issue trackers Tridium)"*.

Code evidence from this block closes that question:

1. **Recovery mechanism is application-level**, not NTFS-specific: `checkForWorkingFile()` at
   Station.java:582-589 handles the crash-after-backup-rename scenario on both POSIX and Windows.
2. **No "NTFS marker files corruption" in code**: NTFS journal provides mid-rename atomicity
   transparently; Niagara does not interact with NTFS journal markers directly.
3. **The real unhandled case** (crash before backup rename, §411.3) is platform-independent —
   old `config.bog` remains intact, `.working` orphaned.
4. **B32's `REPLACE_EXISTING` concern** (§32.9.3) does not apply: destination is always absent.

`[CERT]` (see §411.1–411.6 for evidence; absence of NTFS-specific code verified across baja module)

---

## 411.8 — Connections

- **[Block 402]** — §402.3.3 documents the full `saveSync()` sequence. Block 411 covers the
  complementary boot-time recovery of the `.bog.working` artifact produced by that sequence.
  Remittance to B402 for save trigger, dirty-flag chain, and save sequence detail.

- **[Block 5]** — §5.2.8 approximations (further to B402's §402.4 correction):
  - Step 4 names the backup `.bog.bak` — does not exist; actual format is `config_backup_yyMMdd_HHmm.bog`.
  - Step 5 says ".bog.bak preserves previous version for recovery" — backup files are not crash-recovery
    artifacts; recovery uses `.bog.working` exclusively.
  - B402 already corrected `.bog.tmp` → `.bog.working`. Block 411 corrects the backup naming only.

- **[Block 32]** — §32.9.5 open question closed (see §411.7). §32.9.3's `MoveFileEx(REPLACE_EXISTING)`
  concern does not apply to BOG save path (destination absent before rename). The "issue trackers
  Tridium" citation in §32.9.5 remains uncorroborated; no NTFS-specific edge case appears in the
  decompiled code for this version.
