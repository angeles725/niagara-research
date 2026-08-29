# Web snapshot — graphql-java Java 11 requirement (for B616 / GQL-G8)

- **Query**: which graphql-java version last supports Java 8; which first requires Java 11.
- **Primary source (accessed 2026-08-29)**: https://github.com/graphql-java/graphql-java/discussions/3052
  — "GraphQL Java will require Java 11 going forward" (official maintainer announcement discussion).
- **Official blog (same announcement)**: https://www.graphql-java.com/blog/java-11-required/
  — returned HTTP 403 to the fetcher on 2026-08-29; the GitHub discussion #3052 (crawler-readable) was used
  as the §5 fallback and carries the same maintainer statement.

## Verbatim facts extracted

- **v20.0 (released December 2022)** is the LAST graphql-java release line to support Java 8 as its minimum.
- **v21.0 (released early July 2023)** is the FIRST version to REQUIRE Java 11 as the minimum.
- Maintainer support-window statement: security updates are backported for ~18 months after a release
  (so v20's security backports were planned into mid-2024).

## Relevance

Niagara N4.14 modules are compiled to and run on **Java 8** (class-file major version 52 — verified locally
on `BComponent.class`, `BWebServlet.class`, `ModuleClassLoader.class`; and B176 records `java-8-openjdk-amd64`
mandatory). Therefore a graphql-java JAR bundled into a Niagara module must be **≤ v20.x**; v21+ (Java-11
bytecode, class major 55) would throw `UnsupportedClassVersionError` at load under N4's Java 8 JVM.
