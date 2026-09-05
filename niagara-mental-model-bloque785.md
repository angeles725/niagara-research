# B785 · The rdb DIALECT EXTENSION SPI — add a 5th RDBMS without touching rdb-rt (MAE14, D10)

> **Scope**: the WRITE side of the `rdb` framework — the extension points a module author uses to add a NEW RDBMS
> dialect, vs the READ side (B403/B407/B409 cover the four built-in dialects as fixed consumers). A dialect is a
> `@NiagaraType` `BComponent` satisfying three abstract methods, where the SQL shape is a 60-method `RdbmsDialect`
> object; the framework is open-for-extension with no central dialect registry. Focus: `module-authoring-exemplars`
> (MAE14 / dimension D10). Kit destination: `types/logic.md`.
>
> **Sources**: FUENTE 3 decompiled — `rdb-rt` (`BRdbms`, `BEncryptableTransportRdbms`, `jdbc/RdbmsDialect`),
> `rdbMySQL-rt` (`BMySQLDatabase`, `BMySQLConnectionPool`, module.xml); `rdbOracle`/`rdbSqlServer`/`rdbHsqlDb` present;
> verified this session at `organized/`. FUENTE 1: database B402-B413, B114 (encryption). READ-ONLY. English (post-B115).

---

## 785.1 — The SPI: `BRdbms` abstract, three methods `[CERT]`
`public abstract class BRdbms extends BDevice implements BILicensed` (`rdb-rt/.../javax/baja/rdb/BRdbms.java:103`) is
the root extension point — a database is a `BDevice`. Its three abstract methods are the minimum contract every
dialect satisfies:
- `abstract Feature getLicenseFeature()` (:290) — each dialect is a separately-licensed feature.
- `abstract Connection getConnection(String user, BPassword pw)` (:308) — the JDBC connection.
- `abstract RdbmsContext getRdbmsContext()` (:326) — returns the DIALECT object (§785.3).
Hierarchy (from `module_nav hierarchy`): `BMySQLDatabase → BEncryptableTransportRdbms → BRdbms → BDevice → BComponent`.
`BHsqlDatabase` extends `BRdbms` directly (embedded, no TLS); MySQL/Oracle/SqlServer extend the encryptable base.

## 785.2 — A concrete dialect: `BMySQLDatabase` `[CERT]`
`public class BMySQLDatabase` (`rdbMySQL-rt/.../BMySQLDatabase.java:82`) implements the contract:
- **JDBC driver** — `ds.setDriverClassName("com.mysql.cj.jdbc.Driver")` (`BMySQLConnectionPool.java:86`); **URL** —
  `"jdbc:mysql:"` + `//host:port/db?...` (:99).
- **`getConnection()`** delegates to the dialect's `BConnectionPool`; **`getLicenseFeature()`** (:420) →
  `getFeature("tridium","rdbMySQL")`; **`getRdbmsContext()`** (:435) returns an anonymous `RdbmsDialect`.
- The anon `RdbmsDialect` encodes every SQL-shape decision: validation query (`"select 1;"`), IDENTITY strategy
  (`getIdentityCreation()`→`"AUTO_INCREMENT"`, sequence methods throw Unsupported — MySQL has no sequences, contrast
  Oracle), TYPE mapping (`getIntType`→INTEGER, `getBooleanType`→TINYINT, `getUuidType`→BINARY(16), `getBlobType`→
  MEDIUMBLOB), IDENTIFIER quoting (MySQL backticks), size limits (table/column name → 64), `getAlterColumn`→"MODIFY".

## 785.3 — `RdbmsDialect`: the 60-method SQL-shape interface `[CERT]`
`public interface RdbmsDialect extends RdbmsContext` (`rdb-rt/.../jdbc/RdbmsDialect.java:9`) — ~60 methods covering
capability flags, type mapping, identifier/quoting rules, identity-vs-sequence strategy, size limits, and JDBC type
codes. This is what `getRdbmsContext()` hands back, and it is where a new dialect expresses everything that differs
between RDBMSs. There is NO central switch enumerating dialects — each dialect SELF-DESCRIBES through this object.

## 785.4 — `BEncryptableTransportRdbms`: the TLS-transport intermediate `[CERT]`
`public abstract class BEncryptableTransportRdbms extends BRdbms` (`rdb-rt/.../BEncryptableTransportRdbms.java:58`) is
the base for the three TLS-capable dialects; it adds a truststore/TLS SPI (the encryption obligation, B114): abstract
`loadTrustStore`/`saveTrustStore`, `getTrustStorePath`, `getServerCertificateProperty`,
`getServerCertificateSubjectIdentifier`. A no-TLS/embedded dialect (HSQLDB) skips this and extends `BRdbms` directly.

## 785.5 — Registration + author obligation `[CERT/INFER]`
Registration is a plain manifest `<type>` — `<type name="MySQLDatabase" class="com.tridium.rdb.mysql.BMySQLDatabase"/>`
(`rdbMySQL-rt/.../module.xml:26`), plus the dialect's connection pool and an ORD scheme
`<type name="MySQLScheme" … ordScheme="mysql"/>` (:27) (the §778.2 ORD-scheme pattern). The class carries the standard
`@NiagaraType` + `TYPE = Sys.loadType(...)`. **To add a 5th dialect (e.g. PostgreSQL)** [INFER from the exemplar]:
(1) subclass `BEncryptableTransportRdbms` (TLS) or `BRdbms` (embedded), `@NiagaraType`; (2) implement the three BRdbms
methods — `getLicenseFeature`, `getConnection` (via a `BConnectionPool` setting the JDBC driver + URL),
`getRdbmsContext`; (3) implement `RdbmsDialect` (type mapping, quoting, identity strategy, limits); (4) if encryptable,
implement the 5 truststore hooks; (5) register `<type>` entries in the new module's module.xml. **Contrast READ side**
(B403/B407/B409): those consume the four built-ins; this SPI adds a fifth WITHOUT touching `rdb-rt`.

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | `BRdbms extends BDevice` is the abstract SPI; 3 abstract methods getLicenseFeature/getConnection/getRdbmsContext | [CERT] | BRdbms.java:103,290,308,326 |
| 2 | Hierarchy BMySQLDatabase → BEncryptableTransportRdbms → BRdbms; HSQLDB extends BRdbms directly | [CERT] | module_nav hierarchy; BEncryptableTransportRdbms.java:58; BMySQLDatabase.java:82 |
| 3 | A dialect supplies JDBC driver + URL + getRdbmsContext returning an RdbmsDialect; license per-dialect | [CERT] | BMySQLConnectionPool.java:86,99; BMySQLDatabase.java:420,435 |
| 4 | `RdbmsDialect extends RdbmsContext` (~60 methods) encodes all SQL-shape decisions; no central registry | [CERT] | RdbmsDialect.java:9 |
| 5 | `BEncryptableTransportRdbms` adds the TLS truststore SPI (B114) for the 3 TLS dialects | [CERT] | BEncryptableTransportRdbms.java:58 |
| 6 | Registration = manifest `<type>` (+ ordScheme + @NiagaraType); adds a dialect without touching rdb-rt | [CERT] | rdbMySQL-rt/module.xml:26,27 |

**Tally**: 6 [CERT], 0 [INFER on claims] (the "add a 5th" steps are [INFER] from the exemplar pattern). Spine
grep-verified inline this session at `organized/` (BRdbms line numbers corrected to the vineflower tree :290/308/326).

## Connections
- **B402-B413** (database — the READ side / built-in dialects this SPI extends). **B114** (encryption — the
  `BEncryptableTransportRdbms` truststore hooks). **B778** (a dialect registers a `…Scheme … ordScheme="mysql"` — the
  same ORD-scheme author pattern). **B784** (rdbMySQL ships as its own module with a `Tridium`/floor dep on rdb-rt).

## Open gaps
- **MAE14-G1** — the Oracle SEQUENCE path (`getSequenceName`/`getIdentityLookup`, which MySQL throws Unsupported for)
  is named as the contrast but not walked; a bounded follow-up if a builder targets a sequence-based RDBMS.

## Kit implication (→ `types/logic.md`)
Document the "extend a framework via a Device + a self-describing SPI object" pattern with rdb as the exemplar: a new
RDBMS dialect is a `@NiagaraType B<X>Database extends BRdbms` (or `BEncryptableTransportRdbms` for TLS) implementing
three abstract methods — `getLicenseFeature`, `getConnection`, `getRdbmsContext` — where `getRdbmsContext()` returns a
60-method `RdbmsDialect` carrying every SQL-shape decision, wired in purely by a `<type>` entry in the module's
module.xml. There is no central dialect registry — the framework is open-for-extension by subclass + SPI-object +
manifest registration. (This generalizes: services B778, ORD schemes B778, query providers B782, and rdb dialects all
follow "subclass a framework base + register a `<type>`/agent + hand back a self-describing SPI object.")
