# Block 348 — The official `docJsonToolkit` as a source: 114 files, 33 cited, accurate on the happy path — and the decompilation-only findings it never mentions (the security surfaces, the synchronous engine-thread regen, `null`→`""`)

> Focus **jsonToolkit** — evidence block J14 (doc-synthesis). READ-ONLY. Corpus language: ENGLISH.
>
> Scope: `docJsonToolkit` as a `[CERT-doc]` SOURCE — its coverage, its self-framing, and (the load-bearing part)
> the "what this doc does NOT resolve" register: the behaviours decompilation established (J1–J13) that the
> official documentation is silent on. This is the corpus's FIRST use of `docJsonToolkit` as a source
> [Block 336] J2.
>
> Sources: the `docJsonToolkit-doc` corpus (114 `.html` files), of which **33 are cited + preserved** across
> B335–B347 (`sources/manuals/jsonToolkit-docs/`, registered in `sources/SOURCES.md`). Overview docs read for
> grounding: `Introduction-JSON-6DA55A8D.html`, `LicenseRequirementsJson-6DBD2640.html`, `ComparisontoAlternatives-json.html`.
>
> Markers: `[CERT-doc]` official doc · `[CERT]`/`[Block N]` remission to a decompilation block · `[INFER]`
> deduction. Block TYPE: **doc-synthesis** (a high `[CERT-doc]`/`[INFER]` ratio is expected and healthy, §11).

---

## 348.1 — The doc as a source, and its self-framing

The `docJsonToolkit` corpus is 114 HTML topics — a full product manual (Introduction, DeveloperGuide, Features,
LicenseRequirements, per-component reference, examples, a DocumentChangeLog). The corpus cited 33 of them across
J1–J13 — the FIRST time niagara-research uses `docJsonToolkit` as `[CERT-doc]` `[INFER]`. The doc frames the
module as **schema-based marshalling of station data to/from JSON for the cloud** (Introduction stresses "schema"
heavily, with "cloud"/"MQTT"/"import"/"export") `[CERT-doc]` (`Introduction-JSON-6DA55A8D.html`), and positions it
against **oBIX** as the alternative `[CERT-doc]` (`ComparisontoAlternatives-json.html`) — which matches the J5
finding that the output slot is consumed like an obix point [Block 339] §339.2 `[INFER]`.

## 348.2 — Where doc and code AGREE (the happy path is accurate)

The doc is RELIABLE on the declarative model — every block that cited it found the code matched `[INFER]`:
- the schema = recursive member tree → one `JSONWriter` [Block 336] §336.5 `[CERT-doc]`;
- the tuning policy (minWrite/maxWrite debounce, cov vs onDemand, force-override) [Block 337] §337.6 `[CERT-doc]`;
- queries = "any transform/neql/bql returning a BITable", run in parallel, `queriesMaxExecutionTime` [Block 338]
  §338.5 `[CERT-doc]`;
- the four slot-selection modes [Block 336] §336.5, the relative/supervisor base-query aggregation [Block 343]
  §343.3, the inbound receive→select→route model [Block 340] §340.6, the alarm recipient [Block 345] §345.6
  `[CERT-doc]`;
- licensing: `LicenseRequirements` documents the `DR-JSON/DR-S-JSON` feature, the import/export attributes, and
  the SMA requirement (SMA is its heaviest theme) [Block 335] §335.2 `[CERT-doc]`.
So for CONFIGURATION, the manual is trustworthy — a rare corpus case where doc↔code agreement held across a whole
focus `[INFER]`.

## 348.3 — What the doc does NOT resolve (the decompilation-only findings)

Vendor docs document the happy path; decompilation revealed the failure modes and defaults the manual omits
`[INFER]`. The register:

- **The regeneration is SYNCHRONOUS on the engine thread.** The doc describes the minWrite/maxWrite tuning as a
  message-rate control, but never says a COV event re-runs the WHOLE schema inline on the engine thread, or that a
  slow query blocks it up to `queriesMaxExecutionTime` (default 30 s) — [Block 337] §337.3, [Block 338] §338.2.
  The doc's tuning framing hides an engine-thread-latency reality `[INFER]`.
- **`null` serializes as `""`, never JSON `null`.** A consumer cannot distinguish empty from absent —
  [Block 346] §346.4. Not in the doc `[INFER]`.
- **The queues reject on overflow (cap 1000, `QueueFullException`), one item per engine cycle** — [Block 346]
  §346.2. The doc mentions no queue bound or backpressure policy `[INFER]`.
- **Security surfaces the doc frames as FEATURES with no warning** `[INFER]`:
  - export-marker registration has **NO ACL** — any inbound JSON can register any ORD [Block 341] §341.4; the doc
    presents it as "the cloud assigns its own identifier" `[CERT-doc]` (`ExportSetpointHandlerAndExportRegis`),
    silent on the trust assumption.
  - the alarm-ack records a **spoofable acker name** from the JSON [Block 341] §341.3; the doc names the
    `runAsUser` adminWrite gate but not the unvalidated `user` field `[CERT-doc]` (`HandlersAndAlarmAcknowledgments`).
  - `BJsonArrayForEachSelector` has **no size guard** and `learnMode` auto-creates slots [Block 340] §340.3-340.5
    — undocumented.
  Of 114 doc files, only ~10 mention security/permission at all `[CERT]` (grep) — and none flags these `[INFER]`.
- **"Marshaller, not transport."** The doc positions the module as a cloud connector; the reality that it ships
  NO autonomous transport (output slot + `BExporter`, everything else external) is implicit at best — [Block 339]
  §339.1 made it explicit `[INFER]`.
- **An expired SMA DISABLES import/export at runtime** (not just blocks upgrades) — the enforcement
  (`FeatureNotLicensedException` on every op) is code [Block 335] §335.3; `LicenseRequirements` documents SMA as a
  requirement but not this fail-closed runtime behaviour `[INFER]`.

## 348.4 — Connections

- Every J-block that carries `[CERT-doc]` (B336–B347) is the per-topic grounding this block consolidates.
- [Block 335] §335.3 / [Block 337] §337.3 / [Block 340] §340.3 / [Block 341] §341.3-341.4 / [Block 346] §346.4 —
  the specific decompilation findings §348.3 names as doc-silent.
- The modbus focus [Block 314]/[Block 315] and tags focus [Block 269] — prior corpus blocks that likewise added
  official Tridium docs as `[CERT-doc]`; jsonToolkit joins them.

## 348.5 — Self-verify

Block TYPE: **doc-synthesis** (`[CERT-doc]` + remissions + `[INFER]`; per §11 a high `[INFER]` ratio is EXPECTED
for a "what the doc omits" block — the omissions are inferences cross-referencing the decompilation blocks). The
114-file count and the 33-cited count are from `fd` + `SOURCES.md` grep; the "only ~10 files mention security"
is a driver grep over the doc corpus. Every §348.3 omission cites the decompilation block that established the
fact. Overview docs (`Introduction-JSON`, `LicenseRequirements`, `ComparisontoAlternatives`) registered +
preserved under `sources/manuals/jsonToolkit-docs/`.

`verify-block.sh` marker tally (computed):

| Marker | count (adj) |
|---|---|
| CERT (grep-derived counts) | 1 |
| CERT-doc | 13 |
| CERT-hw / CERT-live / CERT-web / CERT-a | 0 |
| INFER | 14 |
| INFER/CERT ratio | 1.00 (EXPECTED for doc-synthesis — the "what the doc omits" claims are inferences cross-referencing the decompilation blocks; §11) |

`verify-block.sh` exit 0. The "zero file:line" WARN is BY DESIGN for a doc-synthesis block: it cites `[CERT-doc]`
topics + `[Block N]` remissions, not fresh `file:line` — each omission's fact was established (with its own
citations) in the remitted decompilation block.

This closes the `jsonToolkit` backlog (14/14). The focus SYNTHESIS follows in the next block.
