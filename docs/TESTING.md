# Testing

This document describes how to run and extend the Janus automated test suite. It reflects the test sources and Maven
configuration currently present in the repository; it does not imply that every recommended scenario is already covered.

## Testing Stack

Janus uses the following testing tools:

- **JUnit 5** for test organization and execution.
- **Mockito** for isolating application use cases from dependencies.
- **AssertJ** for readable assertions.
- **REST Assured** for HTTP endpoint tests.
- **Quarkus `@QuarkusTest`** for tests that boot the application and exercise CDI, REST resources, configuration, or
  JDBC repositories.
- **PostgreSQL and Flyway** for persistence tests against the application's relational schema and migrations.

Most test sources are under `src/test/java`. Unit tests commonly mock ports or repositories; Quarkus tests exercise a
larger part of the application.

## Prerequisites

Before running the suite, make sure you have:

1. **JDK 25 or newer.** The Maven configuration sets `maven.compiler.release` to `25`.
2. **Docker available and running** for the default Quarkus PostgreSQL Dev Services setup. The `%test` profile leaves
   the JDBC URL unset, allowing Quarkus to provision a test database when Dev Services is available.
3. The JWT key files expected by the application configuration. `application.yaml` references `privateKey.pem` and
   `publicKey.pem`, but these files were not included in the project archive reviewed for this document. Provide a
   **test-only key pair** on the test classpath or configure test-specific key locations before running tests that boot
   the application or generate tokens. Do not use production private keys in tests.
4. No connection from the test profile to production or other shared data. Tests and Flyway migrations can modify the
   database.

The Maven Wrapper is included in the repository, so a separate Maven installation is not normally required.

## Run the Test Suite

From the repository root, run:

```bash
./mvnw test
```

On Windows Command Prompt, use:

```bat
mvnw.cmd test
```

This invokes Maven Surefire, which runs the current unit tests and `@QuarkusTest` classes. The first Quarkus test run
may take longer while dependencies and the test database are initialized.

To start from a clean build directory:

```bash
./mvnw clean test
```

## Run Specific Tests

Use Maven's `-Dtest` property to run one test class at a time:

```bash
./mvnw -Dtest=CreateUserUseCaseTest test
./mvnw -Dtest=TokenServiceTest test
./mvnw -Dtest=AuthControllerTest test
./mvnw -Dtest=PermissionControllerTest test
```

Choose a test class that exists under `src/test/java`. `@QuarkusTest` classes boot Quarkus and may require
Docker/PostgreSQL and the JWT test keys described above.

## Database Configuration

The test profile in `src/main/resources/application.yaml` sets the datasource JDBC URL to an unset value. When Docker is
available and no explicit JDBC URL is configured, Quarkus Dev Services can start a disposable PostgreSQL instance for
tests.

If you prefer a dedicated local PostgreSQL instance, create a database used **only for testing**, then provide its
connection details through environment variables. For example, on Linux or macOS:

```bash
export QUARKUS_DATASOURCE_JDBC_URL='jdbc:postgresql://localhost:5432/janus_test'
export QUARKUS_DATASOURCE_USERNAME='janus_test'
export QUARKUS_DATASOURCE_PASSWORD='replace-with-a-local-test-password'
./mvnw test
```

Use the equivalent environment-variable configuration for your shell on Windows. Create the database and user yourself
before running the tests. Never point these variables at a production database or a database containing data you need to
preserve.

Flyway is configured to migrate the database at startup. If a test fails during application startup, check the database
logs, connection settings, migration output, and presence of the test JWT key pair before investigating the test
assertion itself.

## What the Current Tests Cover

The current test tree contains tests in these areas:

| Area           | Examples of existing test coverage                                                                                            |
|----------------|-------------------------------------------------------------------------------------------------------------------------------|
| Identity       | User and credential use cases, login behavior, JWT token service, auth/user/credential HTTP resources, and JDBC repositories. |
| Authorization  | Permission and role use cases, role-permission and user-role relationships, HTTP resources, and JDBC repositories.            |
| Authentication | Login-attempt tracking, refresh-token operations, session use cases, and JDBC repositories.                                   |
| Bootstrap      | Initialization of the master user, default roles, permissions, and their relationships.                                       |
| Reliability    | Inbox use cases and the Inbox JDBC repository.                                                                                |

This list describes where test source files exist; it is not a statement that all edge cases or production failure modes
are covered.

## Test Design Guidelines

When adding tests, follow these conventions:

- **Unit-test use cases in isolation.** Mock outbound ports or repositories and assert the returned result, status, and
  interactions.
- **Use Quarkus tests for integration boundaries.** Keep `@QuarkusTest` for behavior that needs CDI, HTTP routing,
  configuration, or a real database.
- **Keep test data independent.** Prefer unique emails, usernames, slugs, and idempotency keys so test execution order
  does not affect results.
- **Test both success and failure paths.** Include invalid payloads, missing records, duplicate values, inactive users,
  expired credentials, and persistence errors where applicable.
- **Assert observable behavior.** For HTTP tests, verify status codes and important response fields. For repository
  tests, verify persisted data after reads or updates.
- **Never include production secrets.** Use disposable test credentials and a dedicated test key pair.

## Recommended Additional Coverage

The following scenarios should receive focused tests as the implementation is completed and stabilized:

### Identity and JWT

- Login with invalid credentials, an inactive account, or an account with repeated failed attempts.
- JWT signature, issuer, expiration, required claims, and rejection of malformed tokens.
- Consistency between the token's expiration claim and the expiration returned by the API.
- Credential changes and password-history rules, if enabled for the relevant flow.

### Authorization

- Requests with no token, an invalid token, or insufficient permissions are rejected.
- Users with the required role/permission are allowed to perform the operation.
- Role changes and permission revocation take effect according to the documented token and authorization model.
- Users cannot access or modify another user's resources unless explicitly authorized.

### Sessions and Refresh Tokens

- Expired, revoked, unknown, or replayed refresh tokens are rejected.
- Logout and session revocation produce the expected effect on subsequent requests.
- Refresh-token rotation and persistence behavior remain consistent under repeated or concurrent requests.

### Idempotency and Reliability

- Repeating a request with the same idempotency key does not repeat its side effects.
- Reusing an idempotency key with a different payload follows the documented conflict behavior.
- Duplicate Inbox messages are detected using the intended message key and consumer group.
- Inbox processing remains safe when a transaction or downstream action fails.
- Add dedicated Outbox tests for event persistence, dispatch, retries, failure recovery, and duplicate publication
  behavior once those flows are implemented.

## Known Gaps in the Current Test Setup

Based on the reviewed source tree and Maven configuration:

- `src/test/java/org/janus/modules/reliability/controller/InboxControllerTest.java` is currently an empty test-class
  placeholder.
- No dedicated Outbox test class was found under `src/test/java`.
- The Maven Failsafe plugin is configured with `skipITs=true` by default. There are no dedicated `*IT` test classes in
  the reviewed test tree. If separate Failsafe integration tests are added, enable them explicitly, for example with
  `./mvnw clean verify -DskipITs=false`.
- No JaCoCo plugin/report configuration was found in the current `pom.xml`; the commands above run tests but do not
  generate a coverage report by themselves.
- The reviewed archive does not contain `privateKey.pem` or `publicKey.pem`, although the application configuration
  references them. Supply test-specific key files before running test flows that need JWT signing or verification.

Keep this section updated as test infrastructure and coverage evolve.
