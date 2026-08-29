# Niagara N4 — module-anatomy (MA7): module `<permissions>` → Java security policy — two tracks (`<java-permissions>` enforced by the SecurityManager per-CodeSource, `<niagara-permission-groups>` granted by a DEFAULT grant-all store), atop an always-restricted base grant

**Focus**: module-anatomy · **Gap**: MA7 (module permissions → policy) · **Session**: 2026-08-29 · **Block**: B635
**Sources** (`[CERT]` decompiled Java, vineflower `decompiled/` tree):
- `organized/baja/baja/decompiled/com/tridium/sys/module/NModule.java` (`readPermissions`)
- `organized/baja/baja/decompiled/com/tridium/sys/module/ModuleClassLoader.java`
- `organized/baja/baja/decompiled/com/tridium/sys/Nre.java`
- `organized/baja/baja/decompiled/com/tridium/security/GrantAllPermissionGroupStore.java`

**Scope**: how a module.xml `<permissions>` declaration becomes runtime Java-security state. RBAC/user permissions = [B11]/[B558] (different subsystem). Module-permission ARCHITECTURE overview = [B18] (REMIT); this block traces the MODULE-LOAD wiring and the enforcement premise.

---

## 635.1 `readPermissions`: an always-on base grant + two optional tracks

`[CERT]` `NModule.java:447-468` — every module gets a fixed base grant, then optionally more:
```java
this.permissions = new PermissionsCache();
this.permissions.add(new PropertyPermission("niagara." + moduleName + ".*", "read,write"));  // its own props
this.permissions.add(new KeyRingPermission(moduleName));                                      // its own keyring
this.permissions.add(new KeyRingPermission(moduleName + ".*"));
XElem permissions = manifest.elem("permissions");
if (permissions != null) {
    if (permissions.elem("java-permissions") != null) {          // TRACK 1
        this.checkTpk = true;
        JavaPermissionsFactory.parse(this, permissions, NiagaraPolicyUtil.getPolicyType()).forEach(this.permissions::add);
    }
    if (permissions.elem("niagara-permission-groups") != null)   // TRACK 2
        this.requestedPermissions = NiagaraPermissionGroupFactory.parse(this, permissions, ...);
}
this.permissions.setReadOnly();
```
**Default (no `<permissions>`)** = the three base grants only: read/write of its OWN `niagara.<moduleName>.*` properties + its own keyring. No `FilePermission`, no `RuntimePermission`. A module that declares nothing is minimally scoped, not all-powerful (D).

Two tracks with different enforcement paths:

- **TRACK 1 — `<java-permissions>`** (e.g. `FilePermission <<ALL FILES>>`, `RuntimePermission exitVM.*`, [B434]): parsed into real `java.security.Permission`s. Declaring this flips `checkTpk = true` — a module asking for raw Java permissions is marked for TPK (Tridium/vendor key) scrutiny ([B392]/[B482], signing-pki REMIT). These are the powerful grants.
- **TRACK 2 — `<niagara-permission-groups>`** (e.g. chihuahua's `type="all"/"workbench"/"station"`): parsed into `Set<NiagaraPermissionGroup> requestedPermissions` — Niagara's own coarse group model, NOT JVM permissions.

---

## 635.2 Wiring to the module's ClassLoader

`ModuleClassLoader` (a `URLClassLoader`, [B617]) bridges both tracks:

`[CERT]` `ModuleClassLoader.java:111,115` — the module jar's URL becomes the `CodeSource`, and the niagara-group track is registered with the central policy:
```java
this.codeSource = new CodeSource(url, (Certificate[])null);
if (!module.getRequestedNiagaraPermissions().isEmpty())
    NiagaraPolicyUtil.requestPermissions(NiagaraPolicyUtil.canonicalizeCodeSource(url), module.getRequestedNiagaraPermissions());
```
And `getPermissions(CodeSource)` returns the module's TRACK-1 `PermissionCollection` for classes loaded from that jar (sweep: `ModuleClassLoader.getPermissions` → `module.getModuleJavaPermissions()`). So when the JVM's `AccessController.checkPermission` runs, a class from `chihuahua-rt.jar` is evaluated against exactly the permissions `readPermissions` built for that CodeSource — the module's declared `<java-permissions>` plus the base grant.

---

## 635.3 The enforcement premise — and why it is soft in practice

Two things must hold for `<permissions>` to actually RESTRICT anything:

**(a) A real, enforcing SecurityManager.** `[CERT]` `Nre.java:746,948-957` — N4 installs a SecurityManager by default; it is only removed/replaced when:
```java
checkSecurityManagerDisable():
  if (hasSecurityManagerExemption()) SecurityManagerUtil.disableSecurityManager();   // OFF
  else if (System.getProperty("niagara.security.manager.disable") != null) {
       licenseManager.checkFeature("tridium", "smDeveloperMode");                     // license-gated
       DeveloperSecurityManager.enableDeveloperSecurityManager(policyType);           // LOGGING-only SM
  }
```
So the enforcing SM can be swapped for a **`DeveloperSecurityManager` (logging-only)** when `niagara.security.manager.disable` is set and the `smDeveloperMode` license feature is present. **[B398] found `smDeveloperMode` + `developer{skipModuleValidation=true}` live on the production supervisor** ([B18] §18.3.2) — i.e. exactly the posture that downgrades enforcement to logging. On such a host, `<java-permissions>` denials become log lines, not blocks.

**(b) A restrictive niagara-group store.** `[CERT]` `Nre.java:685,1081` — the policy is initialized with the **`GrantAllPermissionGroupStore`**:
```java
NiagaraPolicyUtil.init(new GrantAllPermissionGroupStore(), policyType);   // station AND workbench
```
`GrantAllPermissionGroupStore` (real class, `com.tridium.security`) grants every requested niagara-permission-group by default. So TRACK 2 (`<niagara-permission-groups>`) is, out of the box, **granted whatever it requests** — the declaration is a REQUEST that the default store rubber-stamps, not an enforced sandbox, unless a non-grant-all store is installed.

**Net**: the `<permissions>` element is a real security surface for TRACK 1 under an enforcing SecurityManager, but TRACK 2 is advisory under the default store, and the live supervisor's developer-mode posture softens TRACK 1 to logging. ([B18] §18: the SM evaluates java-permissions as an INTERSECTION of module-declared ∩ policy — REMIT for the group model.)

---

## 635.4 What this means for building/distributing — and the chihuahua verdict

- **Declare the minimum.** Requesting `<java-permissions>` sets `checkTpk=true`, drawing signature scrutiny; if you don't need raw file/VM/socket permissions, don't declare them — the base grant (own props + keyring) covers a normal component/service module.
- **`<niagara-permission-groups type="all">` is a code smell, not (today) an escalation.** Under `GrantAllPermissionGroupStore` it is granted anyway, so it changes nothing on a default install — but it advertises maximal intent and would become genuinely over-broad the day a restrictive group store is deployed. **Chihuahua deviation (MA8)**: `chihuahua-rt` and `chihuahua-ux` both declare `<niagara-permission-groups type="all"/> + workbench + station` ([B632]). A dashboard module needs none of that — the correct declaration is NO `<permissions>` (fall to the minimal base grant) or a scoped group. It is not exploiting anything (default store grants all; no `<java-permissions>` so `checkTpk` stays false), but it is the maximally-permissive request where the minimal one was correct — a concrete cleanup.
- **Don't rely on module permissions as a sandbox on a dev-mode host.** If the target runs `smDeveloperMode`/SM-disabled ([B398]), `<java-permissions>` is not enforced. Module permissions harden a properly-configured production station, not a developer supervisor.

---

## Self-verify

| # | Claim | Marker | Citation | Checked |
|---|---|---|---|---|
| 1 | base grant (always): PropertyPermission niagara.<mod>.* + 2× KeyRingPermission | [CERT] | NModule.java:451-454 | ✅ read verbatim |
| 2 | `<java-permissions>` → JavaPermissionsFactory + sets checkTpk=true; `<niagara-permission-groups>` → requestedPermissions | [CERT] | NModule.java:457-464 | ✅ read verbatim |
| 3 | default (no `<permissions>`) = base grant only (minimal, not all-perms) | [CERT] | NModule.java:447-468 (no else-broadening) | ✅ read |
| 4 | ModuleClassLoader: CodeSource(jarUrl); niagara groups → NiagaraPolicyUtil.requestPermissions; getPermissions returns module perms | [CERT] | ModuleClassLoader.java:111,115 | ✅ read verbatim |
| 5 | SecurityManager installed by default; disabled via exemption, or replaced by logging DeveloperSecurityManager under smDeveloperMode | [CERT] | Nre.java:746,948-957 | ✅ read verbatim |
| 6 | niagara-group store default = GrantAllPermissionGroupStore (grants all requested groups) | [CERT] | Nre.java:685,1081 + class exists | ✅ read + fd |
| 7 | live supervisor has smDeveloperMode + skipModuleValidation → enforcement softened | [CERT-live] | [B398] §SEC-06 / [B18] §18.3.2 | ✅ cross-ref |
| 8 | chihuahua-rt/ux declare `<niagara-permission-groups all+workbench+station>`; no `<java-permissions>` | [CERT] | [B632] chihuahua jar module.xml | ✅ artifact |

**Tally**: [CERT] ×6 · [CERT-live] ×1 · [INFER] ×0 (the "advisory today / risk tomorrow" reading is stated as a conditional, not a marker claim) · ratio 0.0 (EVIDENCE block; investigable evidence for the module-load wiring exhausted). All primary citations token-checked verbatim.

## Connections

- **[B18]** — module-permissions architecture + SM intersection model (REMIT). **[B398]** — live `smDeveloperMode`/`skipModuleValidation` posture. **[B617]** — the `ModuleClassLoader` this wires. **[B392]/[B482]** — `checkTpk`/TPK signing (REMIT).
- **[B632]** — chihuahua's `<permissions>` bytes. Forward: MA8 folds this into the chihuahua case study + operator recs.

## Gaps uncovered

- None new for module-anatomy. The FULL `NiagaraPolicy`/group-model enforcement (intersection semantics, `NiagaraPolicyUtil` internals in the non-decompiled `nre`) belongs to a security focus, not here — REMIT to [B18], not a new backlog row.
