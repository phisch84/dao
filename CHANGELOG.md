# Changelog

What changed between the versions of DAO 4, for users of the artifacts. Versions without a release date aren't released yet.
All artifacts of a version are released together and have the same version (release versions end with `.RELEASE`).

## 4.1.0 (2026-11-26)

**Added**

- `dao4-hibernate-jakarta`: the Hibernate DAO for Hibernate 6.6 and later, e.g. Hibernate 7 (Java 17 and later,
  `jakarta.persistence`). It has the same classes as `dao4-hibernate` and passes the same tests, with Hibernate 6.6 and 7.
  Choose the artifact which fits the Hibernate version of your project, never both.
- `dao4-hibernate-jakarta` queries the database's time as point in time (HQL `select instant`), so the time stamps are right
  also if the database's time zone isn't the JVM's (e.g. SQLite, whose time is in UTC). `dao4-hibernate` still queries it
  with the select statement of the SQL dialect, as in 4.0.0.

**Changed**

- `dao4-hibernate`: the DAO passes a new data object to Hibernate without its virtual id (Hibernate 6.6 and later refuse to
  merge an entity whose id isn't in the database). There is no visible change: if saving fails, the data object keeps its
  virtual id, as before. Reads use `Session.find` instead of `Session.get`.
- The POMs describe the artifacts and name license, source repository and issue tracker.
- Documented that the time stamps of the raw file DAO have the file system's precision (on Linux, files written a few
  milliseconds apart can get the same time stamp), and that contract tests of such storages override
  `AbstractDAOContractTest.awaitStorageClockTick()`.

**Upgrading from 4.0.0 (development versions)**

- Nothing to do for `dao4`, `dao4-fs-serialization` and `dao4-hibernate`: change the version only.
- If you used `dao4-hibernate` 4.0.0 with Hibernate 6 or 7 and overrode `getStorageTimestamp`, `doGet` or `doSave` to make it
  work, switch to `dao4-hibernate-jakarta` and remove these overrides.

**Tested with** Hibernate 5.6.15 (`dao4-hibernate`), 6.6.59 and 7.4.15 (`dao4-hibernate-jakarta`); built with JDK 21.

## 4.0.0 (not released)

4.0.0 wasn't released on its own; 4.1.0 is the first release of DAO 4 and contains all of it.

The first version of DAO 4: new artifacts (`dao4`, `dao4-fs-serialization`, `dao4-hibernate`) and packages
(`com.schoste.ddd.infrastructure.dao.v4`), which can be used side by side with DAO 2.x and 3.x. The contract changed in many
places (generic ids, logical deletion, stateless change tracking, streams, transactions); see
[Migrating from DAO 2.x and 3.x](README.md#migrating-from-dao-2x-and-3x) in the README.
