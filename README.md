# DAO 4 – data access objects for Java

A small library for loading, saving, deleting and tracking changes of **data objects** in a storage
(file system, serialized files, a database via Hibernate, or memory), behind one interface:
`DAO<ID, DO>` (**DAO** = Data Access Object; **DO** = data object).

> **Status:** version 4.0.0 is under development and not released yet. This page describes the contract of 4.0.0.
> The Javadoc of each type is the complete reference; this page is the entry point.

## Modules

All artifacts have the group id `com.schoste.ddd.infrastructure` and are published to GitHub Packages
(`https://maven.pkg.github.com/phisch84/dao`). Each artifact also comes with a Javadoc jar (`javadoc` classifier).

| Artifact | Package | Content |
|---|---|---|
| `dao4` | `com.schoste.ddd.infrastructure.dao.v4.*` | The `DAO` interface, the base class `AbstractDAO` for implementations, `DataObject`, listeners, exceptions |
| `dao4` with classifier `tests` | `….dao.v4.inmemory`, `….dao.v4.services` | An in-memory DAO for your own tests, and contract tests that DAO implementations can run |
| `dao4-fs-serialization` | `….dao.v4.fs`, `….dao.v4.serialization` | DAOs which store each data object in its own file, as raw bytes or serialized |
| `dao4-hibernate` | `….dao.v4.hibernate` | A DAO which stores data objects in a database via Hibernate |

The major version is part of both the artifact id and the package name, so version 4 can be used side by side with
the older versions (`dao` 2.x and 3.x, which share the package `….dal.v2`) while a project migrates; see
[Migrating from DAO 2.x and 3.x](#migrating-from-dao-2x-and-3x).

```xml
<!-- the interface and everything common to all storages -->
<dependency>
    <groupId>com.schoste.ddd.infrastructure</groupId>
    <artifactId>dao4</artifactId>
    <version>4.0.0</version>
</dependency>

<!-- a storage: files and serialized files ... -->
<dependency>
    <groupId>com.schoste.ddd.infrastructure</groupId>
    <artifactId>dao4-fs-serialization</artifactId>
    <version>4.0.0</version>
</dependency>

<!-- ... or a database via Hibernate 5.6 (brings hibernate-core only: add the JDBC driver of your database yourself,
     and a connection pool such as commons-dbcp or Spring ORM if you use them) -->
<dependency>
    <groupId>com.schoste.ddd.infrastructure</groupId>
    <artifactId>dao4-hibernate</artifactId>
    <version>4.0.0</version>
</dependency>

<!-- for your tests: the in-memory DAO and the DAO contract tests (need JUnit 4) -->
<dependency>
    <groupId>com.schoste.ddd.infrastructure</groupId>
    <artifactId>dao4</artifactId>
    <version>4.0.0</version>
    <classifier>tests</classifier>
    <type>test-jar</type>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>junit</groupId>
    <artifactId>junit</artifactId>
    <version>4.13.2</version>
    <scope>test</scope>
</dependency>
```

The library is compiled for Java 8 and doesn't need Spring. Spring can create and inject DAOs as usual, but any DAO can also
be created with `new`.

## Core rules

These rules hold for every DAO, whatever storage it uses:

- **Data objects** extend `DataObject<ID>`. A new data object (`createDataObject()`) has a *virtual* id; saving it gives it its
  *actual* id. `isVirtualId(id)` tells them apart without accessing the storage.
- **Deleting is logical.** `delete(…)` flags a data object as deleted (`isDeleted()`) instead of removing it physically; physical removal
  is up to the storage's operators (e.g. a clean-up after backups). Some implementations delete physically and say so in their
  class documentation. Therefore: **a data object counts as deleted if it is flagged *or* missing.**
- **Change tracking is stateless.** `getModifiedSince(timestamp)` returns the data objects modified at or after a time stamp.
  The caller remembers the highest modification time stamp it has processed and passes it next time; several callers
  can use one DAO independently. The storage's clock sets the time stamps. Several changes can get the same time stamp,
  so the data objects with exactly the passed time stamp are returned again; callers ignore the ones they have processed.
  A change committed late by a database transaction can carry an older time stamp; callers which must see it pass their
  time stamp minus a safety margin.
- **Streams must be closed.** Methods returning several data objects return a lazy `Stream`, so that large amounts of data never
  have to fit into memory. Consume it once, in one thread, and close it, e.g. with try-with-resources: the DAO keeps resources
  (files, database sessions) open until then.
- **A DAO instance is thread-safe.** A returned stream is not; it belongs to its consumer.
- **Listeners** (`GetListener`, `SaveListener`, `DeleteListener`) can inspect, change or cancel operations. They are called in
  the order they were registered.

## Example

```java
DAO<Long, CustomerDO> dao = ...; // e.g. new CustomerDAO(...) or injected by Spring

CustomerDO customer = dao.createDataObject();       // virtual id
customer.setName("Example Ltd.");
dao.save(customer);                                 // actual id from now on

long lastSeen = Long.MIN_VALUE;

try (Stream<CustomerDO> changed = dao.getModifiedSince(lastSeen, DeletedDataObjects.INCLUDE))
{
    for (CustomerDO dataObject : (Iterable<CustomerDO>) changed::iterator)
    {
        // data objects with exactly lastSeen are returned again: skip the ones already processed
        if (dataObject.isDeleted()) { /* remove it from your cache */ }
        else                        { /* update your cache */ }

        lastSeen = Math.max(lastSeen, dataObject.getModifiedTimestamp());
    }
}
```

## Using a ready-made storage

The storage artifacts contain abstract DAOs; a few lines turn them into a DAO for your data objects. The Javadoc of their
packages contains complete examples:

| Storage | Base class | Package documentation with example |
|---|---|---|
| Serialized files | `AbstractSerializationDAO` | `com.schoste.ddd.infrastructure.dao.v4.serialization` |
| Raw files (e.g. images) | `AbstractRawFileDAO` with `FileDataObject` | `com.schoste.ddd.infrastructure.dao.v4.fs` |
| Database via Hibernate | `AbstractHibernateDAO` | `com.schoste.ddd.infrastructure.dao.v4.hibernate` |
| Memory, for your tests | `AbstractInMemoryDAO` (in the `tests` artifact) | `com.schoste.ddd.infrastructure.dao.v4.inmemory` |

The Hibernate DAO takes the time stamps from the database's clock, so their precision and time zone are the database's
(e.g. SQLite's clock has whole seconds only). A DAO which needs another clock overrides
`AbstractHibernateDAO.getStorageTimestamp(Session)`, e.g. to return `System.currentTimeMillis()` when the application is
the only writer of the database.

## Implementing a DAO for another storage

Extend `AbstractDAO<ID, DO>` and implement its protected methods starting with `do` (e.g. `doGet`, `doSave`, `doCreateLoader`).
Everything else (listeners, logical deletion, argument checks, exception wrapping) is done by the base class. The Javadoc of
`AbstractDAO` describes what each method must guarantee. To check an implementation, extend `AbstractDAOContractTest` from the
`tests` artifact: an implementation is correct if it passes all of its tests.

## Migrating from DAO 2.x and 3.x

The old versions (artifacts `dao`, `dao-fs-serialization`, `dao-h`, package `com.schoste.ddd.infrastructure.dal.v2`) and
version 4 can be on the class path at the same time, so a project can migrate one DAO after the other.

**Behavior that changed without a compile error** – check these first:

- **Ids are generic and virtual ids are unique.** `DataObject<ID>` replaces the `int` id. A new data object no longer has the
  id `0`: every one gets its own virtual id from its DAO (e.g. `-1`, `-2`, …); `isVirtualId(id)` tells whether it has been
  saved.
- **Deleting is logical.** The old DAOs removed data objects physically; `delete(…)` now flags them as deleted (see
  [Core rules](#core-rules)). `getAll` and `getModifiedSince` skip flagged data objects unless asked for them
  (`DeletedDataObjects`), but `get(id)` and `get(ids)` return them too: check `isDeleted()` where it matters.
- **Data objects are only equal to themselves.** Compare them with `isSameRecordAs` (same id) or `isSameVersionAs` (same id
  and modification time stamp) instead of `equals`.
- **Results are streams that must be closed**, instead of collections.
- **Time stamps come from the storage's clock**, set by the DAO when saving (the Hibernate DAO: the database's clock, see
  above).

**Using a DAO:**

| Old (`dal.v2`) | New (`dao.v4`) |
|---|---|
| `models.GenericDataObject` | `models.DataObject<ID>` |
| `getCreatedTimeStamp()` / `getModifiedTimeStamp()` / `getIsDeleted()` | `getCreatedTimestamp()` / `getModifiedTimestamp()` / `isDeleted()` |
| `annotations.AutoSet` | `annotations.AutoSet` (same attributes) |
| `services.GenericDataAccessObject<T>` | `services.DAO<ID, DO>` |
| `exceptions.DALException` | `exceptions.DAOException` |
| `get(int)` | `get(ID)` |
| `get(int[])` returning a `Collection` | `get(Collection<ID>)` returning a `Stream` |
| `getAll()` returning a `Collection`, `getAll(Predicate)` | `getAll()` / `getAll(DeletedDataObjects)` returning a `Stream` (filter it yourself) |
| `reloadAll()`, `ReloadListener` | `getModifiedSince(timestamp)` for change tracking |
| `save(GenericDataObject[])`, `save(Object)` | `save(DO)`, `save(Collection<DO>)` |
| `delete(GenericDataObject[])`, `delete(Object)`, `delete(int[])` | `delete(DO)`, `delete(Collection<DO>)`, `deleteById(ID)`, `deleteByIds(Collection<ID>)` |

**Implementing a DAO:**

| Old | New |
|---|---|
| `GenericDAO` | `AbstractDAO<ID, DO>`: implement the protected methods starting with `do` |
| `GenericSerializationDAO` | `AbstractSerializationDAO<ID, DO>`: additionally `toId(String)` and `doCreateActualId(ID)` |
| `GenericHibernateDAO` | `AbstractHibernateDAO<ID, DO>` |
| overriding `createDataObject()` | `doCreateDataObject()`, plus `doCreateVirtualId()` and `doIsVirtualId(ID)` |

**Hibernate (`dao-h` → `dao4-hibernate`):**

- Hibernate 5.6 instead of Hibernate 4. Own dialects have to be adapted to Hibernate 5.6's API.
- Map `DataObject` as mapped superclass with field access; its attributes are `id`, `createdTimestamp`, `modifiedTimestamp`
  and `deleted` (the column names are free). The Javadoc of the package `com.schoste.ddd.infrastructure.dao.v4.hibernate` contains
  a complete mapping.
- With Spring, `org.springframework.orm.hibernate5.LocalSessionFactoryBean` (from `spring-orm`) creates the session factory,
  as an alternative to `SessionFactoryBuilder`.
- `dao4-hibernate` doesn't bring commons-dbcp or Spring ORM along; declare them yourself if you use them.

## Building

```
mvn clean verify
```

Builds with JDK 21 (or newer), targets Java 8, runs all tests and creates the jars, test jars and Javadoc jars.
The build fails on any Javadoc problem, so the documentation stays complete.

## License

MIT, see [LICENSE](LICENSE).
