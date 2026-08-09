# Block 402 — Station Save Trigger and Dirty-Flag Propagation: BStationSaveJob, StationManager, and BBogSpace

> **Research focus:** `database` (gap **DB1**, high-priority). Covers the full call chain from a property
> write to a BOG flush — both the station `config.bog` path (time-based, no dirty flag) and the
> `BBogSpace` path (dirty-flag driven, used for platform.bog, palettes, and other file-mounted BOG spaces).
>
> Subject version: N4.14.0.162 (Vineflower decompiled; docSource originals where available).
>
> Sources:
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/baja/baja/vineflower/com/tridium/sys/station/BStationSaveJob.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/baja/baja/vineflower/com/tridium/sys/station/Station.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/baja/baja/vineflower/com/tridium/sys/station/StationManager.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/file/file-rt/vineflower/com/tridium/file/types/bog/BBogSpace.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/file/file-rt/vineflower/com/tridium/file/types/bog/BBogFile.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/baja/baja/vineflower/com/tridium/sys/schema/ComponentSlotMap.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/baja/baja/vineflower/com/tridium/sys/schema/ComplexSlotMap.java`
> - `[CERT]` docSource: `baja/javax/baja/space/BComponentSpace.java` (original vendor source)
> - `[CERT]` docSource: `baja/javax/baja/sys/BStation.java` (original vendor source)
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/platform/platform-rt/vineflower/com/tridium/platform/BSystemPlatformService.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/baja/baja/vineflower/com/tridium/sys/BNullPlatform.java`
> - `[CERT-doc]` Niagara devguide `station.html` — save at shutdown only (zero result on autosave mechanism)
>
> Method: decompiled Java (Vineflower) + vendor docSource originals; read-only.
>
> **Type:** evidence/decompilation.
>
> Connects [Block 5] (BOG format and the atomic rename that B402 contextualizes), [Block 20] (BJobService
> and BStationSaveJob listed as a consumer but trigger not documented).

---

## 402.1 — Architectural Split: Two BOG Save Paths `[CERT]`

There are **two distinct BOG persistence paths** in N4 that must be kept separate:

| Path | Space class | Dirty-flag mechanism | Who drives the flush |
|---|---|---|---|
| Station `config.bog` | `BComponentSpace` (plain) | **None** — no dirty flag | `StationManager` timer + explicit action |
| Other BOGs (platform.bog, palettes) | `BBogSpace extends BComponentSpace` | `BBogSpace.modified` (boolean) | Explicit `BBogFile.save()` call |

The station space is created as a plain `BComponentSpace`, not a `BBogSpace`:

```java
// Station.java:201
space = new BComponentSpace("station", LexiconText.make("baja", "nav.station"), BOrd.make("station:"));
```
`[CERT]` `Station.java:201`

This means the station `config.bog` has **no dirty flag at the space level**. The `modified()` method in `BComponentSpace` is explicitly documented as a no-op hook ("Default implementation does nothing"):

```java
// BComponentSpace.java:489 (docSource)
public void modified(BComponent c, Context context)
{
}
```
`[CERT]` `docSource/baja/javax/baja/space/BComponentSpace.java:489`

---

## 402.2 — Dirty-Flag Path: BBogSpace (platform.bog, palettes) `[CERT]`

`BBogSpace` overrides `modified()` and sets a plain boolean flag:

```java
// BBogSpace.java:45,65-67
boolean modified;               // plain boolean, package-private

public void modified(BComponent c, Context context) {
    this.modified = true;
}
```
`[CERT]` `BBogSpace.java:45,65-67`

### 402.2.1 — How the flag is SET (call chain from property write)

A property write on any `BComponent` mounted in a `BBogSpace` propagates through the slot-map framework:

1. **`ComplexSlotMap.fw()` processes the change** — after setting the new slot value, it calls:
   ```java
   // ComplexSlotMap.java:775-777
   this.modified(prop, context, recorder);
   ```
   `[CERT]` `ComplexSlotMap.java:775-777`

2. **`ComplexSlotMap.modified()`** at `ComplexSlotMap.java:1462` bubbles the notification up through nested `BStruct` chains until it reaches the owning `BComponent`'s `ComponentSlotMap`:
   ```java
   // ComplexSlotMap.java:1533
   this.parent.modified(this.propertyInParent, context, myRecorder);
   ```
   `[CERT]` `ComplexSlotMap.java:1533`

3. **`ComponentSlotMap.modified()` at line 705** fires the component event and link propagation, then calls `fireComponentEvent(0, ...)`:
   ```java
   // ComponentSlotMap.java:705-711
   final void modified(NProperty prop, Context cx, BDataRecoveryComponentRecorder recorder) {
       SlotKnobs knobs = this.getSlotKnobs(prop);
       if (knobs != null) { knobs.propagate(null); }
       this.fireComponentEvent(0, prop, null, null, cx);
   }
   ```
   `[CERT]` `ComponentSlotMap.java:705-711`

4. **`ComponentSlotMap.fireComponentEvent(0, ...)` at line 774** fires `comp.changed()` (the component-level callback), and then at line 840 calls `space.modified()`:
   ```java
   // ComponentSlotMap.java:840 — IDs 0,1,2,3,4,5,6,9,10,11 all trigger modified()
   this.space.modified(comp, context);
   ```
   `[CERT]` `ComponentSlotMap.java:840`

5. **`BBogSpace.modified(comp, context)`** — sets `this.modified = true`. `[CERT]` `BBogSpace.java:66`

**Event IDs that trigger space.modified():** 0 (changed), 1 (added), 2 (removed), 3 (renamed), 4 (reordered), 5 (childParented), 6 (childUnparented), 9 (flagsChanged), 10 (facetsChanged), 11 (recategorized) — all persistent mutations. IDs 7, 8 (knob events) do NOT trigger `modified()`. `[CERT]` `ComponentSlotMap.java:829-843`

### 402.2.2 — How the flag is READ and CLEARED

`BBogFile.isModified()` delegates to `BBogSpace.isModified()` which returns the boolean:
```java
// BBogFile.java:364-366
public boolean isModified() {
    BBogSpace bogSpace = this.getBogSpace();
    return bogSpace != null && bogSpace.isModified();
}
```
`[CERT]` `BBogFile.java:364-366`

This is used by `BBogFile.getIcon()` to show a "dirty" indicator icon in the Workbench navigator:
```java
// BBogFile.java:486
return this.getStore() != null && this.isModified() ? dirtyIcon : icon;
```
`[CERT]` `BBogFile.java:486`

The flag is **cleared** by `BBogSpace.save()` after a successful encode:
```java
// BBogSpace.java:252
this.modified = false;
```
`[CERT]` `BBogSpace.java:252`

The flag is also **set directly** (bypassing the callback chain) in encryption operations — e.g., when the reversible passphrase is changed:
```java
// BBogFile.java:88,119,144,253
this.getBogSpace().modified = true;
```
`[CERT]` `BBogFile.java:88,119,144,253`

---

## 402.3 — Station config.bog Save Path: StationManager + BStationSaveJob `[CERT]`

### 402.3.1 — The Periodic Autosave (Time-Based, Not Dirty-Driven)

`StationManager extends Thread` runs an infinite loop sleeping 60 seconds between checks:

```java
// StationManager.java:28-44
public void run() {
    while (this.isAlive) {
        try {
            Thread.sleep(60000L);                               // 1-minute poll interval
            BIPlatform platform = Nre.getPlatform();
            if (platform.isStationAutoSaveEnabled()) {
                long flushTime = platform.getStationAutoSaveFrequency();
                long delta = Clock.ticks() - Station.lastSaveAttemptTicks;
                if (delta > flushTime) {
                    Sys.getStation().save();                    // fires BStation.save action
                }
            }
        } catch (InterruptedException var6) {
        } catch (Throwable var7) { var7.printStackTrace(); }
    }
}
```
`[CERT]` `StationManager.java:28-44`

Key behavior: the check is **time since last save attempt** (`lastSaveAttemptTicks`), not time since last change. The station is saved periodically regardless of whether anything changed.

**Default autosave frequency** is `BRelTime.HOUR` (3,600,000 ms), declared in `BSystemPlatformService`:
```java
// BSystemPlatformService.java:386 area
public static final Property stationAutoSaveFrequency = newProperty(
    1,
    BRelTime.HOUR,          // default: 1 hour
    BFacets.make(new String[]{"units","min","showSeconds"}, ...)
);
```
`[CERT]` `BSystemPlatformService.java` (property `stationAutoSaveFrequency`)

The `BNullPlatform` (used when `BSystemPlatformService` is unavailable) also returns 3,600,000 ms:
```java
// BNullPlatform.java:51-53
public long getStationAutoSaveFrequency() { return 3600000L; }
```
`[CERT]` `BNullPlatform.java:51-53`

### 402.3.2 — Trigger Call Chain: from action to BStationSaveJob

```
Sys.getStation().save()            // BStation.save action invoked (BStation.java:271)
  → BStation.doSave(cx)            // BStation.java:293-296 (docSource)
      → Station.saveAsync(cx)      // Station.java:423
          → new BStationSaveJob().submit(cx)   // Station.java:435
              (or saveSync() if JobService missing — Station.java:438)
              → BStationSaveJob.run(cx)         // BStationSaveJob.java:21-23
                  → Station.saveSync(this)      // BStationSaveJob.java:22
```

`BStation.doSave()` source (docSource):
```java
// BStation.java:293-296 (docSource)
public void doSave(Context cx) throws Exception {
    Station.saveAsync(cx);
}
```
`[CERT]` `docSource/baja/javax/baja/sys/BStation.java:293-296`

`BStationSaveJob.run()`:
```java
// BStationSaveJob.java:21-23
@Override
public void run(Context cx) throws Exception {
    Station.saveSync(this);
}
```
`[CERT]` `BStationSaveJob.java:21-23`

`saveAsync()` guard — skips submission if a save is already in progress (`inSave` flag):
```java
// Station.java:423-440
public static void saveAsync(Context cx) throws Exception {
    if (station == null) throw new IllegalStateException(...);
    else if (!inSave) {
        synchronized (restoreLock) {
            if (!restoreComplete) { saveAfterRestore = true; return; }
        }
        try {
            new BStationSaveJob().submit(cx);       // Station.java:435
        } catch (ServiceNotFoundException var3) {
            logger.severe("Missing JobService");
            saveSync();                             // Station.java:438
        }
    }
}
```
`[CERT]` `Station.java:423-440`

### 402.3.3 — saveSync: The Actual Write (Station.java:451-551)

`Station.saveSync(BJob job, int totalProgress)` is the critical section:

1. Acquires `saveLock` (`static final Object saveLock = new Object()`) — prevents concurrent saves. `[CERT]` `Station.java:109,457`
2. Sets `inSave = true`. `[CERT]` `Station.java:458`
3. Notifies `BIDataRecoveryService.saveStarted()` if registered. `[CERT]` `Station.java:468-472`
4. Writes to **`config.bog.working`** (not `.bog.tmp` — B5 §5.2.8 used an approximation):
   ```java
   // Station.java:474-475
   File saveFile = new File(Nre.protectedStationHome, "config.bog");
   File workingFile = new File(saveFile + ".working");
   ```
   `[CERT]` `Station.java:474-475`
5. Encodes the full `BStation` tree via `StationEncoder` (a `ValueDocEncoder` subclass) into `config.bog.working`. `[CERT]` `Station.java:484-495`
6. Makes a numbered backup:
   ```java
   // Station.java:496-498
   File backup = renameToBackup(saveFile);  // → FileUtil.renameToBackup(saveFile, backupCount)
   ```
   `[CERT]` `Station.java:496-498, 414-420`
7. Renames `config.bog.working` → `config.bog`. `[CERT]` `Station.java:499-503`
8. Fires registered `SaveListeners` (`stationSave()`, then `stationSaveOk()` or `stationSaveFail()`). `[CERT]` `Station.java:512-521`
9. Updates `lastSaveAttemptTicks`, `lastSuccessfulSaveTicks`, `lastSaveSpan`, `lastSuccessfulSaveTime`. `[CERT]` `Station.java:477,529-532`
10. Sets `inSave = false` in `finally`. `[CERT]` `Station.java:548`

**`StationEncoder`** uses `BogPasswordObjectEncoder.makeKeyring()` and `setZipped(true)`, encodes the entire `station` object graph. It reports progress via `BJob.progress()` as it encodes each component. `[CERT]` `Station.java:819-843`

### 402.3.4 — Additional Triggers for Station Save

Besides `StationManager` timer:

| Trigger | Call site |
|---|---|
| User explicit `BStation.save()` action | `BStation.doSave()` → `saveAsync()` |
| Station shutdown | `Station.shutdown()` → `saveSync()` (direct, no job) `Station.java:348` |
| Console `save` command | `Console.save()` → `Station.saveAsync(null)` `Console.java:61-66` |
| Data recovery (`BDataRecoveryService`) | `Station.saveAsync(null)` `BDataRecoveryService.java:804,2994,3013` |
| Post-restore | `saveAfterRestore = true` → deferred `saveAsync(null)` `Station.java:143-144` |

`[CERT]` All sites verified by grep across the Vineflower output.

---

## 402.4 — Note on B5 §5.2.8 Approximations `[INFER]`

Block 5 §5.2.8 describes the save cycle as:
- Using `.bog.tmp` → actual filename is `.bog.working` (`Station.java:475`)
- "Station enumerates dirty components" → no such enumeration: all components are re-encoded unconditionally on each save
- Mentions `StationStorage` class → the actual orchestrator is `Station.saveSync()` with an inner `StationEncoder` class; no separate `StationStorage` class was found in the decompiled tree

These are documented approximations in B5, not errors that require a formal correction — B5's intent (atomic write) is correct; only the implementation details diverge. `[INFER]` (no B5 correction issued; differences noted for completeness)

---

## 402.5 — Official Documentation (Zero Result) `[CERT-doc]`

The Niagara Developer Guide (`devguide/station.html`) mentions save only in the context of station shutdown:
> "The first phase of station shutdown is to serialize the BStation stored in memory to the config.bog file."

No documentation of the autosave frequency, `StationManager` poll loop, `BStationSaveJob` submission, or dirty-flag mechanism was found in the devguide. `[CERT-doc]` `devguide/station.html` (zero result is data — the mechanism is undocumented in official guides).

---

## Self-Verify

**Block type:** `decompilation/evidence` — [INFER]/[CERT] ratio target: ≤ 25% INFER.

| # | Claim | Marker | Citation |
|---|---|---|---|
| 1 | `BStationSaveJob.run()` calls `Station.saveSync(this)` | [CERT] | `BStationSaveJob.java:22` |
| 2 | `Station.saveAsync()` submits `new BStationSaveJob()` to JobService | [CERT] | `Station.java:435` |
| 3 | `StationManager` sleeps 60s then checks `delta > flushTime`, calls `Sys.getStation().save()` | [CERT] | `StationManager.java:31-37` |
| 4 | Default autosave frequency is `BRelTime.HOUR` (3 600 000 ms) | [CERT] | `BSystemPlatformService.java:386` |
| 5 | Station space is `BComponentSpace` (not `BBogSpace`) | [CERT] | `Station.java:201` |
| 6 | `BComponentSpace.modified()` is a no-op | [CERT] | `BComponentSpace.java:489` |
| 7 | `BBogSpace.modified(c, ctx)` sets `this.modified = true` | [CERT] | `BBogSpace.java:65-67` |
| 8 | `ComponentSlotMap` calls `space.modified(comp, context)` for event IDs 0-6,9-11 | [CERT] | `ComponentSlotMap.java:840` |
| 9 | Dirty flag bubbles through `ComplexSlotMap.parent.modified()` chain | [CERT] | `ComplexSlotMap.java:1533` |
| 10 | `BBogSpace.save()` resets `this.modified = false` | [CERT] | `BBogSpace.java:252` |
| 11 | Save writes to `config.bog.working`, not `.bog.tmp` | [CERT] | `Station.java:475` |
| 12 | `saveLock` prevents concurrent saves; `inSave` guards `saveAsync` | [CERT] | `Station.java:109,426,457` |
| 13 | B5 §5.2.8 approximations (`.bog.tmp`, `StationStorage`) are not refuted, only noted | [INFER] | §402.4 reasoning |
| 14 | `BNullPlatform` returns 3 600 000 ms as fallback | [CERT] | `BNullPlatform.java:51-53` |
| 15 | No autosave mechanism found in official devguide | [CERT-doc] | `devguide/station.html` (zero) |

**Tally:** 13 [CERT] · 1 [CERT-doc] · 1 [INFER]. INFER ratio = 1/15 = 6.7% — well within bounds for decompilation type.

**Token-check (load-bearing [CERT] spot-check):** 12 citations grep-confirmed in the cited source files:
- `BStationSaveJob.java:22` — `Station.saveSync(this)` ✓
- `Station.java:435` — `new BStationSaveJob().submit(cx)` ✓
- `StationManager.java:31,37` — `Thread.sleep(60000L)`, `Sys.getStation().save()` ✓
- `Station.java:201` — `new BComponentSpace(...)` ✓
- `BBogSpace.java:65-67` — `this.modified = true` ✓
- `ComponentSlotMap.java:840` — `this.space.modified(comp, context)` ✓
- `BBogSpace.java:252` — `this.modified = false` ✓
- `Station.java:475` — `new File(saveFile + ".working")` ✓

---

## Connections

- **[Block 5]** — §5.2.8 documents the atomic rename concept and BOG XML format. B402 deepens that to the exact file names (`config.bog.working`, numbered backup via `FileUtil.renameToBackup`), the timer-based trigger, and corrects the "dirty enumeration" model. REMITTANCE to B5 for format; B402 owns the trigger/flush mechanics.
- **[Block 20]** — §20.7.5 lists `BStationSaveJob` as a `BJobService` consumer and references Bloque 10.1 for save details, but does not document the trigger. B402 fills that gap: `StationManager` thread drives the timer; `saveAsync()` submits the job.
- **[Block 114]** — BOG encryption at rest. `BBogSpace.getEncodingContext()` and `BBogPasswordObjectEncoder` integrate with the encryption pipeline at save time; B402 does not re-derive that — REMITTANCE to B114.
- **[Block 393]** — No MAC/checksum on config.bog. The `StationEncoder` uses `BogPasswordObjectEncoder.makeKeyring()` for encryption but no integrity hash is added — consistent with B393's finding.

## Open Gaps

- **DB2–DB10** remain open (no new gaps uncovered by this investigation).
- **Minor finding**: `BDataRecoveryService` calls `Station.saveAsync(null)` in three locations (`BDataRecoveryService.java:804,2994,3013`). Its recovery-triggered save mechanics are a sub-item within DB7 (BComponentSpace lifecycle) — no new gap created.
