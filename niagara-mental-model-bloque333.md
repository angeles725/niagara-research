# Block 333 — The browser layer: `email-ux` is a thin bajaux registration shell over `webEditors`, pairing each Workbench editor with a JS module, and it too has no test-send

> Focus **email** — evidence block E10 (low priority). READ-ONLY. Corpus language: ENGLISH.
>
> Scope: `email-ux` (11 Java classes, 372 lines) — the modern browser (bajaux) UI layer: the account manager,
> the type-ext editors for address values, the field editors, and the JS-build/CSS registration. This is the
> THIRD email UI, alongside the Workbench Swing editors and the Hx HTML editors [Block 331].
>
> Sources (primary, decompiled N4.14.0.162 + packaged JS resources), read inline:
> `organized/email/email-ux/vineflower/com/tridium/email/ux/**` (Java shells) and the packaged JS under
> `organized/email/email-ux/extracted/rc/**` (the actual browser code).
>
> Markers: `[CERT]` local primary source (`file:line` / packaged resource) · `[INFER]` deduction. Layer 8
> (notification — browser UI) + Layer (bajaux web). Block TYPE: **evidence** (small subsystem).

---

## 333.1 — The pattern: a Java `BSingleton` shell pointing at a JS module

Every `email-ux` UI class is a thin registration shell: a `BSingleton` implementing `BIJavaScript` (+ a
form-factor marker), holding a `JsInfo` that names a `module://email/rc/...js` resource built by `BEmailJsBuild`
`[CERT]`. Examples:

- `BEmailAccountUxManager` — `BSingleton implements BIJavaScript, BIFormFactorMax`, `@AgentOn email:EmailService`,
  `JsInfo → module://email/rc/mgr/EmailAccountManager.js` `[CERT]` (`BEmailAccountUxManager.java:16-24`).
- `BEmailEditor` — `BSingleton implements BIJavaScript, BIFormFactorMini, BIOffline`, `@AgentOn email:Email`,
  `JsInfo → module://email/rc/fe/EmailEditor.js` `[CERT]` (`fe/BEmailEditor.java:18-26`).
- `BEmailAddressTypeExt extends BBajaScriptTypeExt`, `JsInfo → module://email/rc/types/EmailAddress.js` `[CERT]`
  (`BEmailAddressTypeExt.java:14-22`) — a bajaux type-ext for the address value type; siblings cover
  AddressList and the ...ToString variants (the ux counterpart of the E9 converters [Block 332] §332.5).

So the Java side carries NO logic beyond the `JsInfo` mapping of a value type / view to a JS file `[CERT]`
(the shells contain only the `JsInfo` decl + `@AgentOn`). The `fe/` editors
(`BEmailAddressEditor`, `BEmailAddressListEditor`, `BEmailEditor`, `BTextPartEditor`) are the same shape `[CERT]`
(their `JsInfo` decls).

## 333.2 — `BEmailJsBuild`: the bundle and its dependencies

`BEmailJsBuild extends BJsBuild` declares the module bundle `[CERT]` (`BEmailJsBuild.java:14,23`):

```java
super("email", BOrd.make("module://email/rc/email.built.min.js"),
      new Type[]{ BWebEditorsJsBuild.TYPE, BConvertersJsBuild.TYPE, BEmailCssResource.TYPE });
```

So the browser bundle `email.built.min.js` depends on **`webEditors`** (the shared bajaux editor framework),
the **converters** JsBuild, and the email CSS resource `BEmailCssResource` `[CERT]`. email-ux is therefore a
direct CONSUMER of `webEditors` `[CERT]` (the dependency is verbatim in the bundle's `Type[]`) — the un-opened
`webEditors` module (95 classes, px-tail focus) is the base this layer is built on `[INFER]` (cross-focus).

## 333.3 — The manager JS: a webEditors `Manager` subclass, marked Private

The real manager logic is `rc/mgr/EmailAccountManager.js`, an AMD bajaux module `[CERT]` (packaged resource):

```js
/** API Status: **Private**  @module nmodule/email/rc/mgr/EmailAccountManager */
define(['baja!', 'baja!email:EmailAccount,email:IncomingAccount,email:OutgoingAccount', 'lex!email',
        'nmodule/webEditors/rc/wb/mgr/Manager', 'nmodule/email/rc/mgr/EmailAccountManagerModel',
        'nmodule/email/rc/mgr/EmailAccountColumn', 'nmodule/email/rc/mgr/EmailAuthenticationColumn',
        'nmodule/email/rc/mgr/EmailProtocolColumn', ...], function (...) { ... }
```

It extends the webEditors `Manager`, backed by `EmailAccountManagerModel`, with the same domain columns as the
Workbench manager — `EmailAccountColumn`, `EmailAuthenticationColumn`, `EmailProtocolColumn` — plus inbox/outbox
icons `[CERT]` (`EmailAccountManager.js` header + `define` deps). It is tagged **`API Status: Private`** (Tridium
-internal, copyright 2016) `[CERT]` — not a public extension point `[INFER]`. So the browser manager MIRRORS the
Workbench manager [Block 331] §331.1 in a different framework, with parallel column classes `[INFER]`.

## 333.4 — No test-send here either

Grepping the ux JS (`rc/mgr/*.js`, `rc/fe/*.js`) for a `test`/`send` affordance returns only unrelated
polyfill boilerplate — no send-test control `[CERT]` (grep, verified). So ALL THREE email UIs (Workbench Swing
[Block 331] §331.1, Hx §331.5, and bajaux ux here) lack a test-send: the finding is consistent across the whole
UI surface `[INFER]`. Commissioning still requires a real alarm or a Program `BEmailService.send` [Block 324]
§324.5.

## 333.5 — The three UI layers, consolidated

Email is edited through three parallel UI stacks `[INFER]` (cross-block synthesis):

| Layer | Framework | Manager | Editor | Block |
|---|---|---|---|---|
| Workbench | Swing (`workbench.mgr`) | `BEmailAccountManager` | `BEmailFE` + address FEs | [Block 331] §331.1-331.4 |
| Hx | legacy server HTML | — | `BHxEmailFE` | [Block 331] §331.5 |
| ux | bajaux over `webEditors` | `EmailAccountManager.js` | `EmailEditor.js` + type-exts | this block |

The N4 dual/triple-UI pattern the corpus has seen elsewhere (px, charts) holds for email too `[INFER]`.

## 333.6 — Connections

- [Block 331] §331.1-331.5 — the Workbench + Hx UIs this bajaux layer parallels; §331.1's no-test-send finding
  is confirmed here (§333.4) for the browser.
- [Block 332] §332.5 — the runtime converters whose ux type-ext counterparts (`BEmailAddress*TypeExt`) register
  here.
- px-tail focus (`webEditors`, 95 classes, un-opened) — the framework `BEmailJsBuild` depends on (§333.2); a
  cross-focus pointer.
- [Block 210] — the `@AgentOn` extension pattern the shells use.

## 333.7 — Self-verify

Block TYPE: **evidence** (small registration subsystem; the substance is packaged JS, read as a resource per the
"resources before inferring from logic" rule). Load-bearing anchors (extern — token-checked by read):
`BEmailAccountUxManager.java:24` (JsInfo → EmailAccountManager.js), `fe/BEmailEditor.java:26` (JsInfo →
EmailEditor.js), `BEmailJsBuild.java:23` (bundle + webEditors/converters/CSS deps), `BEmailAddressTypeExt.java:22`
(type-ext JsInfo), and the packaged `EmailAccountManager.js` header (`API Status: Private` + webEditors `Manager`
dep). The no-test-send absence was grep-verified across `rc/mgr` and `rc/fe`.

`verify-block.sh` marker tally (computed):

| Marker | count (adj) |
|---|---|
| CERT (extern file:line / resource) | 14 |
| CERT-doc / CERT-hw / CERT-live / CERT-web / CERT-a | 0 |
| INFER | 7 |
| INFER/CERT ratio | 0.50 |

`verify-block.sh` exit 0.

Evidence block: `[INFER]`s are the three-UI synthesis, the webEditors-consumer link, and the mirror-of-Workbench
reading — each anchored to a cited `[CERT]` shell or resource.
