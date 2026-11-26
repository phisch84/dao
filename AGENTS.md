# Working on DAO 4

Instructions for AI agents and contributors. What the library does: [README.md](README.md). Why it is the way it is:
[docs/decisions.md](docs/decisions.md) – read it before changing behavior. What changed: [CHANGELOG.md](CHANGELOG.md).

## The documentation is part of the product

Users get the artifacts from Maven and usually never see the source code. What they read: the **Javadoc jars** (in their IDE),
the **POM** (name, description) and, at most, the README on GitHub. Therefore:

- Every public or protected element has Javadoc; `package-info.java` and `dao/src/main/javadoc/overview.html` are the entry
  points. The build fails on any Javadoc problem (doclint), also in the test jars (except missing comments).
- A change of behavior updates, in the same step: the Javadoc where users read the rule, the README if it mentions it,
  `CHANGELOG.md` (under the version in development), and `docs/decisions.md` if a decision changes.
- The test jars are public too: the in-memory DAO and `AbstractDAOContractTest` are meant for users.

## Layout

| Directory | Artifact | Java | Content |
|---|---|---|---|
| `dao` | `dao4` (+ test jar) | 8 | `DAO`, `AbstractDAO`, `DataObject`, listeners, transactions; tests: in-memory DAO, `AbstractDAOContractTest` |
| `dao-fs-serialization` | `dao4-fs-serialization` | 8 | file and serialization DAOs |
| `dao-h` | `dao4-hibernate` | 8 | Hibernate 5.6 |
| `dao-h-jakarta` | `dao4-hibernate-jakarta` | 17 | Hibernate 6.6 and later; **compiles the sources and tests of `dao-h`** |

The Hibernate modules share code (build-helper plugin):

- `dao-h/src/main/java`, `dao-h/src/test/java` and `dao-h/src/test/resources` are compiled by **both** modules. Code there
  must compile with Java 8 and Hibernate 5.6 *and* run with Hibernate 6.6 and 7. Test contexts are loaded with `classpath:`.
- A call whose API differs between Hibernate versions goes into `HibernateVersionSupport`, which exists twice:
  `dao-h/src/main/java-hibernate5/…` and `dao-h-jakarta/src/main/java/…`. So does `package-info.java`.
- Tests which only one module has live in that module's own `src/test/java`.

## Build and test

- JDK 21, Maven 3.9. **Always build with `clean`**: IDEs (e.g. VS Code) write into `target/classes`, so an incremental build can
  report success for code which doesn't compile.
- Everything: `mvn clean install` (all tests, Javadoc jars).
- One module with what it needs: `mvn clean install -pl dao-h-jakarta -am`.
- `dao-h-jakarta` with the oldest supported Hibernate, after `install`: `mvn -pl dao-h-jakarta -P hibernate-6.6 surefire:test`
  (CI runs it too; it reuses the classes built with Hibernate 7).
- A DAO implementation is correct if it passes `AbstractDAOContractTest`. A new rule of the contract gets a test there first.
- Test names: `method_condition_expectation`, e.g. `save_newDataObjectRefusedByTheDatabase_keepsItsVirtualId`.

## Conventions

- **Line endings are mixed and must be kept per file**: Java sources, POMs and XML resources mostly CRLF; `README.md`,
  `CHANGELOG.md`, `AGENTS.md`, `docs/` and the workflows LF. New files take the endings of their neighbors. Git doesn't
  convert them (no `.gitattributes`); `sed -i` in Git Bash turns CRLF into LF, so check endings after shell edits.
- Java style: tabs and braces on their own line, `this.` for members; some files use spaces – keep the style of the file.
- Versions: all modules have the version of the parent POM; when it changes, change the `<parent>` version of every module too.
  Release versions end with `.RELEASE`, development versions with `-SNAPSHOT` (see decision 19).
- Dependencies: the main artifacts depend on nothing but `dao4` and Hibernate. JDBC drivers, connection pools and Spring are
  test dependencies only.

## Working with the owner

- Don't stage or commit; the owner reviews the diff and commits. Use plain `mv`/`rm`, not `git mv`/`git rm`.
- Work in small steps and summarize each one for review.
- Other projects depend on `dao4` (e.g. `domain-repository3`): breaking changes need a new major version (decision 12).
