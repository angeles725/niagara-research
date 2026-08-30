# B668 — The minimal `-rt` `module.xml` for a custom alarm recipient: `<dependency name="alarm-rt">` + one `<type class="…" name="…"/>` — and the correction that `routeAlarm` is NOT declared in `module.xml` (it is an INHERITED `@NiagaraAction` slot); the module DOES need its own file + socket/URL permissions (focus alarm-webhook, AW3)

> **Focus:** `alarm-webhook` (§16). **Gap closed:** AW3 (minimal `module.xml` for a `-rt` that depends on
> `alarm` and registers a `BRecoverableRecipient` subclass). **Phase:** static, READ-ONLY.
> **Sources** (all `[CERT]`, real built modules on disk):
> - `organized/email/email-rt/vineflower/META-INF/module.xml` (the reference: an `-rt` that depends on `alarm`
>   and registers an alarm recipient type).
> - `organized/alarm/alarm-rt/vineflower/META-INF/module.xml` (registers `BRecoverableRecipient`, and the
>   alarm file-permission block).
> - `[CERT]` corpus [Block 631] (type-registration pipeline `@NiagaraType`→`module-include.xml`→Registry),
>   [Block 636] (reference `-rt` skeleton) — REMITTANCE for the generic build mechanics.
>
> **Bottom line for the PoC:** the `module.xml` is *generated* by the build from your `@NiagaraType`
> annotations + `module-include.xml`; you do **not** hand-author the `<type>` rows. What you must get right is
> (1) declare `<dependency name="alarm-rt">` (and `baja`), (2) let the build emit one
> `<type class="…BMiWebhookRecipient" name="MiWebhookRecipient"/>`, (3) grant the runtime permissions your
> code needs — a socket/URL permission for the POST, and a `${protected.station.home}/alarm` file permission
> if `persistent=true`. There is **no `routeAlarm` entry in `module.xml`** — that action is inherited.

---

## §668.1 — The shape of a real `-rt` module that depends on `alarm` `[CERT]`

From `email-rt/module.xml` (trimmed to the load-bearing parts):

```xml
<module name="email-rt" bajaVersion="0" vendor="Tridium" vendorVersion="4.14.0.162"
        preferredSymbol="e" nre="true" autoload="true" installable="true"
        moduleName="email" runtimeProfile="rt" releaseDate="2024-05-28">
 <dependencies>
  <dependency name="alarm-rt" vendor="Tridium" vendorVersion="4.14.0"/>   <!-- ← the one that matters -->
  <dependency name="baja"     vendor="Tridium" vendorVersion="4.14.0"/>
  <!-- email also pulls fox-rt, web-rt, net-rt, oauth2-rt … only what its code imports -->
 </dependencies>
 <types>
  <type class="com.tridium.email.alarm.BEmailRecipient" name="EmailRecipient"/>   <!-- the registration -->
  <!-- … other email types … -->
 </types>
 <permissions> … </permissions>
 <moduleParts> <modulePart name="email-ux" runtimeProfile="ux"/> … </moduleParts>
</module>
```
`[CERT email-rt/module.xml:2-4, 93]`. The dependency you cannot omit for an alarm recipient is
`alarm-rt` (line 4) — it provides `BAlarmRecipient` / `BRecoverableRecipient` / `BAlarmRecord`. `baja` is
implicit-but-declared. Add others **only** as your code imports them (for a webhook you likely need nothing
beyond `alarm-rt` + `baja`; the HTTP client is `java.net.HttpURLConnection` from the JDK, no module dep).

For the base types themselves, `alarm-rt/module.xml` registers `BRecoverableRecipient`:
```xml
<type class="javax.baja.alarm.BRecoverableRecipient" name="RecoverableRecipient"/>   <!-- alarm-rt:30 -->
<type class="javax.baja.alarm.BAlarmRecipient"       name="AlarmRecipient"/>          <!-- alarm-rt:20 -->
```
`[CERT alarm-rt/module.xml:20,30]`.

## §668.2 — The registration is `@NiagaraType` on the class ↔ one `<type>` row — NOT a `routeAlarm` declaration `[CERT]`

`BEmailRecipient` is the concrete pattern: the class carries **`@NiagaraType`** (plus its own
`@NiagaraProperty` slots) and **nothing else** about routing:

```java
@NiagaraType
@NiagaraProperties({ @NiagaraProperty(name="to", …), … })
public class BEmailRecipient extends BAlarmRecipient implements BIUserAlarmRecipient { … }
```
`[CERT email-rt/…/BEmailRecipient.java:30-63]`.

**Correction to the intake request**, which asked to *"register a `BAlarmRecipient/BRecoverableRecipient` with
`@NiagaraType` + Action `routeAlarm(BAlarmRecord)`"*:
- `routeAlarm` (and `routeAlarmAck`, and the `newUnackedAlarm` topic, and `handleAlarm`) are declared **once,
  on the base `BAlarmRecipient`** via `@NiagaraAction` `[CERT BAlarmRecipient.java:81-95, 232-248]`. A subclass
  **must not** re-declare them — doing so would shadow the base slot.
- Your subclass declares **only** `@NiagaraType` + whatever config slots it adds (url, token, …), and
  implements `sendAlarm` ([Block 666]). The `<type>` row in `module.xml` is emitted by the build from
  `@NiagaraType`; you do not write it by hand ([Block 631] — the `@NiagaraType`→`module-include.xml`→
  `Registry` pipeline).
- There is **no `<action>` element** in `module.xml` at all — actions/properties/topics live in the compiled
  class's slot map (Slot-o-Matic generated code), not in `module.xml`. `module.xml` maps only
  `class ↔ typeName` (+ deps, permissions, agents). `[CERT — no action elements anywhere in either module.xml]`

## §668.3 — Permissions the webhook module must declare `[CERT for the analogues]`

Permissions are **per-module** (the `<java-permissions>` block defines the protection domain for that module's
classes). Your webhook module inherits nothing from `alarm-rt`; it must grant its own. Two are relevant:

1. **Outbound HTTP (the POST).** `email-rt` grants, under `type="station"`:
   ```xml
   <java-permission action="accept,connect,listen,resolve"
                    class="com.tridium.nre.security.NiagaraSocketPermission" name="*:1-100000"/>   <!-- :117 -->
   <java-permission action="*:*" class="java.net.URLPermission" name="https:*"/>                   <!-- :130 -->
   ```
   `[CERT email-rt/module.xml:117,130]`. A webhook to `https://<backend>` needs a
   `NiagaraSocketPermission` for `connect,resolve` to the host:port (narrow it to your backend, e.g.
   `name="mybackend.example:443"`), and if you use `HttpURLConnection` over https, the `URLPermission`.
   **Least privilege:** scope both to your exact backend host/port, not the wildcard email uses.

2. **The persistent queue (only if `persistent=true`).** The disk queue lives under
   `<stationHome>/alarm/<name>AlarmQueue/` ([Block 666] §666.2). `alarm-rt` grants that tree to *its own*
   classes:
   ```xml
   <java-permission action="read, write, delete" class="java.io.FilePermission"
                    name="${protected.station.home}${/}alarm"/>            <!-- alarm-rt:115 -->
   <java-permission action="read, write, delete" class="java.io.FilePermission"
                    name="${protected.station.home}${/}alarm${/}-"/>       <!-- alarm-rt:116 (recursive) -->
   ```
   `[CERT alarm-rt/module.xml:115-116]`. **Open question for the PoC (AW3-G1, requires-execution):** the disk
   write in `handleAlarm` executes in `alarm-rt`'s own code (the `ValueDocEncoder` call is in
   `BRecoverableRecipient`, [Block 666] §666.2), so it may run under `alarm-rt`'s protection domain rather
   than the subclass's — in which case the subclass module would **not** need the `alarm` FilePermission for
   persistence. This depends on the effective `AccessControlContext` at the call site and is **not decidable by
   static reading alone**; verify on a live station (grant nothing first, set `persistent=true`, force a failed
   send, and check for an `AccessControlException`). Until then, declaring the `${protected.station.home}/alarm`
   FilePermission in the webhook module is the safe default. `[INFER — protection-domain reasoning]`

## §668.4 — Minimal source-side inputs (what you actually author) `[INFER — from B631/B636 build mechanics + the CERT module.xml shape]`

You write these; the build produces the `module.xml` above:

- **`build.gradle` / `module-include.xml`** — declare `moduleName`, `runtimeProfile=rt`, and the
  `alarm`/`baja` dependencies; the `@NiagaraType` sweep fills the `<type>` list ([Block 631], [Block 636]).
- **The class** — `@NiagaraType` + config `@NiagaraProperty`s + `sendAlarm` ([Block 666] §666.6).
- **Permissions** — the `NiagaraSocketPermission`/`URLPermission` (and optionally `alarm` FilePermission)
  above, in the module's permission descriptor.
- **Signing** — any module loaded by a station must be signed per the station's `moduleVerificationMode`
  (REMITTANCE: signing focus B392-B396; own-module build/sign B637). Not an `alarm` concern, but a hard gate
  for the module to load at all.

## §668.5 — Self-verify

| # | Claim | Marker | Cite |
|---|---|---|---|
| 1 | An `-rt` alarm recipient module declares `<dependency name="alarm-rt">` + `baja` | [CERT] | email-rt/module.xml:4-5 |
| 2 | Registration = one `<type class="…" name="…"/>` row; `alarm-rt` registers `BRecoverableRecipient` | [CERT] | email-rt:93; alarm-rt:30 |
| 3 | Concrete recipient carries `@NiagaraType` only (routing inherited) | [CERT] | BEmailRecipient.java:30-63 |
| 4 | `routeAlarm`/`routeAlarmAck`/`handleAlarm` declared once on base `BAlarmRecipient`; NOT re-declared, NOT in module.xml | [CERT] | BAlarmRecipient.java:81-95,232-248 |
| 5 | Outbound POST needs `NiagaraSocketPermission` (+ `URLPermission` for https) | [CERT analogue] | email-rt:117,130 |
| 6 | Persistent queue path is granted via `${protected.station.home}/alarm[/-]` FilePermission | [CERT] | alarm-rt:115-116 |
| 7 | Whether the SUBCLASS module needs the alarm FilePermission is protection-domain-dependent → live check (AW3-G1) | [INFER] | §668.3 |
| 8 | `<type>` rows are build-generated from `@NiagaraType`, not hand-authored | [CERT-remit] | [Block 631] |

**Tally:** 8 claims — 6 [CERT]/[CERT-remit], 1 [CERT analogue] (#5), 1 [INFER] (#7, spawns AW3-G1). 0 unmarked.

## §668.6 — Connections

- **[Block 666]** — the class you register; `sendAlarm`; the queue path this module permissions.
- **[Block 667]** — why the POST (which needs the socket permission here) must be fast/bounded.
- **[Block 669]** — the `BPassword` token slot (a `@NiagaraProperty type="BPassword"`, registered the same way).
- **[Block 631]** — `@NiagaraType`→`module-include.xml`→Registry pipeline (how the `<type>` row is produced).
- **[Block 636]** — the reference `-rt` module skeleton (build/layout).
- **[Block 392]-[Block 396], [Block 637]** — module signing gate (REMITTANCE).
