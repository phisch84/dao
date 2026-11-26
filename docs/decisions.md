# Design decisions

Why DAO 4 is the way it is. The rules themselves are documented in the Javadoc (start with the `DAO` interface); this page
keeps the reasons, so that nobody "fixes" a deliberate choice. Change a decision only on purpose: update this page, the
Javadoc, the README and the contract tests together.

## Contract

| # | Decision | Why | So don't … |
|---|---|---|---|
| 1 | **Deleting is logical** by contract (flag + time stamp); an implementation may delete physically if its class Javadoc says so. Callers treat a data object as deleted if it is *flagged or missing*. | Change tracking can only report a deletion while the object still exists. Physical removal is a job of the storage's operators (e.g. a clean-up after backups), so objects can vanish anyway – hence "or missing". | … delete physically in a DAO without declaring it, or drop the "or missing" rule. |
| 2 | **The storage's clock sets the time stamps** (database time, file modification time); after saving, the data object carries the stored value. | All hosts of an application get time stamps from one clock (no clock skew), and writes which bypass the DAO follow the same clock. The storage is the source of truth. | … set time stamps from the JVM clock in the library (a single DAO may override `getStorageTimestamp` for itself). |
| 3 | **Change tracking is stateless** and **inclusive**: `getModifiedSince(t)` returns everything modified at or after `t`; the caller keeps its own `t`. | Several callers can track one DAO independently. With "after" (`>`), a change saved in the same millisecond as the last processed one was lost (found by a test: 45 of 200 runs). Late database commits can carry older time stamps, so tracking is "at least once" and callers use a safety margin. | … change `>=` to `>`, or keep a "last read" time stamp in the DAO. |
| 4 | **Data objects are only equal to themselves** (`equals`/`hashCode` not overridden); `isSameRecordAs` / `isSameVersionAs` compare explicitly. | The id changes when a new object is saved (virtual → actual) and time stamps change on every save; an id-based `hashCode` breaks hash sets which hold the object. | … override `equals`/`hashCode` in `DataObject`. |
| 5 | **Virtual ids**: every new data object gets its own virtual id from its DAO; `isVirtualId` decides from the id alone. | New data objects are distinct from each other before they are saved (a shared "new" id such as `0` made them collide); telling new from saved needs no storage access, so it is cheap and never changes for an id. | … use `0` or `null` as "new" id. |
| 6 | **Results are lazy streams which must be closed.** | Large amounts of data never have to fit into memory; files and database sessions stay open only while a stream is consumed. | … collect results eagerly in an implementation (loaders must stay lazy). |
| 7 | **A DAO instance is thread-safe**; a returned stream belongs to one consumer. | DAOs are shared singletons (e.g. Spring beans). | … keep per-call state in DAO fields. |
| 8 | **Listeners are hooks** which may inspect, change, replace or cancel an operation; they run in registration order. | They exist to influence the process, not only to observe it. | … turn them into pure notifications. |
| 9 | **Narrow API**: one object, a `Collection`, one id or a `Collection` of ids; `null` (also as element) is an `IllegalArgumentException`; every other failure a `DAOException`. | One way to do each thing; callers know what can fail. | … add `Object`/array overloads back. |
| 10 | **`@AutoSet` lives in `dao4`** although code of the domain layer processes it. | It annotates data objects (infrastructure layer); the infrastructure layer must never depend on a layer above it. | … move it to a domain library. |
| 11 | **No framework needed**: DAOs can be created with `new`; Spring is optional. Examples show plain construction first. | The library must work in any application; Spring is the common case, not a requirement. | … add a Spring dependency to a main artifact. |

## Versions and artifacts

| # | Decision | Why | So don't … |
|---|---|---|---|
| 12 | **The major version is part of the artifact ids and packages** (`dao4`, `….dao.v4`). | Maven resolves one version per artifact id; with the major version in it, DAO 3 and 4 can be used side by side while a project migrates (precedent: commons-lang3). | … publish a new major version under the old artifact ids. |
| 13 | **The stack is chosen by artifact id, never by a version suffix** (`dao4-hibernate` / `dao4-hibernate-jakarta`, same version). | A suffix such as `4.1.0-hibernate7` sorts after `4.1.0`, so update tools would move Java 8 projects to the Hibernate 7 jar; every fix would need two version numbers. | … create versions like `4.1.0-jakarta`. |
| 14 | **Java 8 for everything that doesn't need more**; `dao4-hibernate-jakarta` needs Java 17 (Hibernate 7). | Existing Java 8 / Spring 5.3 applications keep working; new ones get current Hibernate. | … raise the Java version of `dao4` or of the shared Hibernate code. |
| 15 | **One adapter module per Hibernate generation, sharing all code** (`dao-h/src/main/java`, `dao-h/src/test`); only `HibernateVersionSupport` and `package-info` differ. | The Hibernate API differences are tiny (the time query, one deprecated call); copies would drift apart. | … copy classes into `dao-h-jakarta`; put version-specific calls into `HibernateVersionSupport`. |
| 16 | **Hibernate 6.6 is supported because the tests prove it**: the classes built with Hibernate 7 pass all tests with 6.6 (CI). Versions below 6.6 aren't supported. | 6.6 changed `merge()` for unknown ids; older 6.x versions are untested. | … claim support for a version which the build doesn't test. |
| 17 | **The database time is queried as point in time** (HQL `select instant`) in `dao4-hibernate-jakarta`. | HQL `current_timestamp` is local time; with SQLite (UTC) it was off by the JVM's UTC offset. | … switch back to `current_timestamp`. |
| 18 | **New data objects are merged without their virtual id**, which is restored if the merge fails. | Hibernate 6.6 and later throw for an id which isn't in the database; Hibernate 5.6 behaves the same either way. | … pass virtual ids to `merge`. |
| 19 | **Release versions end with `.RELEASE`** and never change; development versions are `-SNAPSHOT`. A release is tagged on `master`. | A `.RELEASE` version exists exactly once; Maven sorts it like the plain version, after the snapshot. | … rebuild or re-publish a release version. |
