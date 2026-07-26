# Block 287 — BACnet/SC topology and security binding: primary/failover with automatic failback, the two-certificate model, and how an inbound SC connection becomes a Niagara `BUser`

> Closes **B280-G2**, and with it every gap [B280] opened. This is the layer above the codec of B280/B286:
> who connects to whom, what happens when a hub dies, and — the part no code-only reading would surface —
> **how BACnet/SC authentication is delegated to Niagara's own `UserService`**.
>
> **Step 2 paid off, as predicted.** The official guide `SettingUpAHub-A10110E5.txt` supplies the component
> names, the certificate roles and the user-association workflow; the code then shows the mechanism. This is
> the second time (after B285) that niagara-help delivered on a **workflow/security** topic while returning
> zero on five consecutive encoding topics — the calibration is now well established.
>
> **Sources**: `niagara-help/guides-clean/Bacnet/SettingUpAHub-A10110E5.txt` + Vineflower decompile of
> `com.tridium.bacnet.stack.link.sc`. Markers: `[CERT]` verbatim · `[CERT-doc]` official Tridium doc ·
> `[INFER]` derived. **Corpus language: ENGLISH.**

---

## 287.1 — The workflow, from the official guide `[CERT-doc]`

Verbatim from `SettingUpAHub`:

> *"A secure **primary or failover** hub accepts connections from remote devices, usually remote
> controllers. **One or two certificates** in the hub authenticate server connections and encrypt these
> messages."*

The engineering sequence:

1. Add an **`ScHubPort`** from the `bacnet` palette under `BacnetNetwork > BacnetComm > Network`, and give
   it a **Network Number**. — so the user-facing component is `ScHubPort`, not `BScLinkLayer` directly.
2. *"To assign the **operational (client) certificate**, click Link, click Credentials … select the
   certificate's alias"*. Its role: *"used to make a local connection from a node that hosts the hub
   function."*
3. *"To assign an **issuer certificate (CA)** … an exported Secure Connect site CA certificate **without its
   private key**. This certificate verifies a remote device's authenticity when it makes a connection to
   the hub."*
4. *"To specify the behavior of the node … towards incoming and outgoing **direct connections**, double-click
   Link … and expand **Node Switch**."* — *"This configures the node that hosts the hub to accept and/or
   initiate direct connections."*
5. Enable the **HubFunction**.
6. *"To associate a user, right-click HubFunction and select **Actions > Add Sc User**."*

**Two certificates, two directions**: the operational certificate is this node's identity; the issuer
certificate is the trust anchor used to validate *incoming* peers. A hub therefore needs both, and the
issuer copy deliberately carries no private key. `[CERT-doc]`

**Hub function and node switch are orthogonal**: a node can host a hub (accepting spokes) *and*
independently accept or initiate direct connections. That is the `BHubFunction` / `BNodeSwitch` split
B280 §280.2 saw in the class list. `[INFER]`

---

## 287.2 — Failover is a six-state machine with automatic failback `[CERT]`

`BHubConnectorSubState`:

```java
public static final int NO_HUB_CONNECTION      = 0;
public static final int CONNECTING_TO_PRIMARY  = 1;
public static final int CONNECTED_TO_PRIMARY   = 2;
public static final int CONNECTING_TO_FAILOVER = 3;
public static final int CONNECTED_TO_FAILOVER  = 4;
public static final int RECONNECTING_TO_PRIMARY = 5;
public static final BHubConnectorSubState DEFAULT = noHubConnection;
```

```
NO_HUB_CONNECTION
   └→ CONNECTING_TO_PRIMARY ──→ CONNECTED_TO_PRIMARY
         └(fail)→ CONNECTING_TO_FAILOVER ──→ CONNECTED_TO_FAILOVER
                                                 └→ RECONNECTING_TO_PRIMARY ──┐
                                                                              │
                                     ◄────────────────────────────────────────┘
```

**State 5 is the finding.** `RECONNECTING_TO_PRIMARY` exists as a distinct state *while connected to the
failover* — so a node that fell back does **not** stay there. It keeps trying to return to the primary, and
the attempt is observable as its own state rather than hidden inside the connected state. `[INFER]` on the
failback semantics; the state and its name are `[CERT]`.

There is a coarser `BScHubConnectorState` (a `@NiagaraEnum` with `noHubConnection`, `connectedToPrimary`, …)
alongside the sub-state — a two-level status model, presumably summary-vs-detail for the UI. `[INFER]`

`BHubConnectorHealth` (463 ln) is a whole class for connection health, and `BScDashboardProvider` (216)
feeds a dashboard — so SC connection state is a first-class operational surface, not just internal
bookkeeping. `[CERT]` on their existence; not traced — gap **B287-G1**.

---

## 287.3 — The credential model `[CERT]`

`BScCredentials` slots:

| Slot | Notes |
|---|---|
| `operationalCertificate` | `BFacets.SECURITY`, custom field editor |
| `operationalCertificateAliasAndPassword` | `BCertificateAliasAndPassword.DEFAULT` |
| **`issuerCertificate1`** | `new BIssuerCertAndCrl()` |
| **`issuerCertificate2`** | `new BIssuerCertAndCrl()` |
| `retryTrigger` | `new BTimeTrigger(BIntervalTriggerMode.make(BRelTime.makeHours(12)))` |

Three things:

1. **Two issuer certificate slots**, matching the guide's *"one or two certificates"*. That supports a CA
   rollover: trust the old and new issuer simultaneously while certificates are re-issued across a site.
   `[INFER]`
2. **`BIssuerCertAndCrl`** — the issuer slot carries a **CRL** (certificate revocation list) alongside the
   certificate. Revocation is modelled, not just trust. `[CERT]` on the type name; `[INFER]` on the CRL
   expansion.
3. **A 12-hour retry trigger.** Certificate-related retry runs on a half-day interval by default —
   consistent with certificate operations (renewal, CRL refresh) rather than connection retry, which
   B280 §280.6 showed is 2 s → 600 s. Two different retry regimes for two different concerns. `[INFER]`

Every certificate slot carries `BFacets.SECURITY` — the flag that drives redacted display and permission
gating in Workbench. `[CERT]`

---

## 287.4 — The security binding: an SC connection authenticates as a Niagara user `[CERT]`

This is the mechanism behind the guide's *"machine user"* sentence. `BAbstractConnectionManager.doAddScUser()`:

```java
public final void doAddScUser() {
   BUserService userService = BUserService.getService();
   if (this.scLinkLayer.hasAssociatedUser())
      throw new LocalizableRuntimeException("bacnet", "scLinkLayer.userAlreadyExists");

   String scSchemeName = addScScheme();
   BUser user = new BUser();
   user.setEnabled(true);
   user.setAuthenticationSchemeName(scSchemeName);

   BBacnetScAuthenticator authenticator = new BBacnetScAuthenticator();
   authenticator.setScPorts(BOrdList.make(this.scLinkLayer.getParentSlotPathOrd()));
   user.setAuthenticator(authenticator);

   userService.add("BACnetSC_" + this.scLinkLayer.getParent().getName() + '?', user);
}

private static String addScScheme() {
   BAuthenticationService service = BAuthenticationService.getService();
   BAuthenticationSchemeFolder schemeFolder = service.getAuthenticationSchemes();
   BBacnetScAuthenticationScheme[] scSchemes = schemeFolder.getChildren(BBacnetScAuthenticationScheme.class);
   if (scSchemes.length == 0) {
      String scSchemeName = "BACnetScScheme";
      schemeFolder.add(scSchemeName, new BBacnetScAuthenticationScheme());
   }
   …
}
```

What this establishes:

1. **BACnet/SC gets a first-class Niagara authentication scheme** — `BBacnetScAuthenticationScheme`, added
   to the station's `AuthenticationService` under the name `BACnetScScheme`, created lazily on first use
   (`if (scSchemes.length == 0)`). It sits alongside the station's other auth schemes. `[CERT]`
2. **The user is named `BACnetSC_<portName>?`** — the `?` is Baja's auto-numbering suffix, so the guide's
   observed `BACnetSC_ScHubPort` is `"BACnetSC_" + parent name`. `[CERT]`
3. **The binding is by ORD, and it is a list**:
   `authenticator.setScPorts(BOrdList.make(scLinkLayer.getParentSlotPathOrd()))`. A `BOrdList` — so one
   authenticator can in principle cover several SC ports, though `doAddScUser` creates it with exactly one.
   `[CERT]`; the multi-port capability is `[INFER]`.
4. **One user per link layer, enforced**: `hasAssociatedUser()` throws
   `scLinkLayer.userAlreadyExists` rather than creating a second. `[CERT]`

**The consequence worth stating plainly**: an inbound BACnet/SC connection is not authenticated by the
BACnet stack alone. It resolves to a **Niagara `BUser`**, through a **Niagara authentication scheme**, with
a **custom `Authenticator`** that matches the connection against the SC port ORD. Everything Niagara applies
to users — enabling/disabling, permissions, audit — therefore applies to SC peers. Disabling that user
disables the hub's ability to accept connections. `[INFER]` on the consequences; the wiring is `[CERT]`.

This is the third place the corpus has found the BACnet subsystem reaching into station-wide Niagara
services: `BacnetDescriptorUtil` creating points (B276 §276.6), `addExtIfMissing` creating alarm extensions
(B283 §283.5), and now the SC connection manager creating **users and authentication schemes**. `[INFER]`

---

## 287.5 — Self-verify

| Claim | Evidence | Marker |
|---|---|---|
| Primary/failover hubs, one or two certificates | guide, quoted verbatim | `[CERT-doc]` |
| User-facing component is `ScHubPort` | guide step 2 | `[CERT-doc]` |
| Operational cert = this node's identity | guide: *"make a local connection from a node that hosts the hub function"* | `[CERT-doc]` |
| Issuer cert = trust anchor, no private key | guide, verbatim | `[CERT-doc]` |
| Node Switch governs direct connections independently | guide step 4 | `[CERT-doc]` |
| Six failover sub-states | `BHubConnectorSubState` constants quoted | `[CERT]` |
| `RECONNECTING_TO_PRIMARY` = automatic failback | state name + position | `[INFER]` |
| Two-level state model (state + sub-state) | both classes exist | `[INFER]` |
| Two issuer-certificate slots | `issuerCertificate1` / `issuerCertificate2` | `[CERT]` |
| ⇒ supports CA rollover | derived | `[INFER]` |
| Issuer slot carries a CRL | type `BIssuerCertAndCrl` | `[CERT]` / `[INFER]` on expansion |
| 12-hour retry trigger | `BRelTime.makeHours(12)` default | `[CERT]` |
| Cert slots flagged `BFacets.SECURITY` | facet on each | `[CERT]` |
| SC has its own auth scheme, created lazily | `addScScheme()` + `if (scSchemes.length == 0)` | `[CERT]` |
| User named `BACnetSC_<port>` | `"BACnetSC_" + getParent().getName() + '?'` | `[CERT]` |
| Binding is an ORD **list** | `BOrdList.make(getParentSlotPathOrd())` | `[CERT]` |
| One user per link layer, enforced | `hasAssociatedUser()` → throws | `[CERT]` |
| ⇒ SC peers are subject to Niagara user management | derived | `[INFER]` |
| Third place BACnet mutates station-wide services | composed with B276 §276.6 and B283 §283.5 | `[INFER]` |

Tally: **[CERT] 10 / [CERT-doc] 5 / [INFER] 7.**

---

## 287.x — Connections and gaps

- **B280** — **G2 closed here; all of B280's gaps are now closed** (G1 by B286, G2 here). §287.3's
  certificate model fills in what §280.7 could only infer about TLS.
- **B286** — the option codec; this block is the layer that owns the connections those messages ride.
- **B276 §276.6 / B283 §283.5** — the other two places BACnet code mutates station-wide Niagara state.
  §287.4 is the third and the most security-relevant.
- **B23 §23.24** — its certificate description (`EKU=serverAuth+clientAuth`, self-signed root CA) is
  consistent with the operational/issuer split found here. No correction needed.

| ID | Gap | Class |
|---|---|---|
| **B287-G1** (new) | `BHubConnectorHealth` (463 ln) + `BScDashboardProvider` (216) — the operational/health surface. | STATIC-investigable |
| **B287-G2** (new) | `BBacnetScAuthenticator` — how it actually matches an inbound connection against the ORD list at authentication time. | STATIC-investigable |
| **B287-G3** (new) | `BAbstractConnectionManager`'s remaining ~550 lines: connection lifecycle, `BConnectionContainer`, the accept path. | STATIC-investigable |
| **Next** | **B276-G1** — the rest of `BacnetDescriptorUtil`. | STATIC-investigable |
