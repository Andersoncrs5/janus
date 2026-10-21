# Janus — Identity and Access Management API

Janus is a backend API focused on **identity, authentication, and authorization**. It provides user and credential
management, role-based access control (RBAC), JWT-based authentication, and supporting mechanisms for safer request
processing and operational reliability.

The project is built with Java and Quarkus and follows a modular, ports-and-adapters (hexagonal) organization,
separating web adapters, application use cases, domain entities, and persistence adapters.

## Features

- **Identity and authentication** — user registration and login, credential updates, RSA-signed JWT access tokens, and
  Argon2id password hashing.
- **Authorization and access control** — roles, permissions, user-role assignments, role-permission assignments, and
  permission checks at the application boundary.
- **Token and login security** — refresh-token revocation/deletion, session and refresh-token domain services, and
  login-attempt tracking.
- **Idempotent requests** — an `Idempotency-Key`-based mechanism is available on selected endpoints to help handle
  retries safely.
- **Reliability data** — Inbox operations and database migrations for Inbox/Outbox, audit logs, password history, API
  keys, and login attempts.
- **Database migrations** — Flyway migrations are configured to run when the application starts.
- **Observability support** — Quarkus health checks, Micrometer/Prometheus metrics, and OpenTelemetry dependencies.
- **Automated tests** — unit and HTTP-level tests using Quarkus testing utilities, REST Assured, Mockito, and AssertJ.

## Technology Stack

| Area                    | Technologies                                           |
|-------------------------|--------------------------------------------------------|
| Language and runtime    | Java 25                                                |
| Framework               | Quarkus 3.38.x                                         |
| Build                   | Maven Wrapper                                          |
| Database                | PostgreSQL, JDBC                                       |
| Schema migrations       | Flyway                                                 |
| Authentication          | JWT, RSA keys, Argon2id                                |
| API                     | REST, JSON, OpenAPI                                    |
| Mapping and boilerplate | MapStruct, Lombok                                      |
| Observability           | OpenTelemetry, Micrometer, Prometheus, SmallRye Health |
| Testing                 | JUnit 5, Quarkus Test, REST Assured, Mockito, AssertJ  |
| Containerization        | Dockerfiles for JVM and native builds                  |

## Architecture

The source code is organized by business module under `org.janus.modules`, with shared domain and infrastructure code
under `org.janus.shared`.

```text
src/main/java/org/janus/
├── configs/                 # JSON, exception, conversion, and mapper configuration
├── modules/
│   ├── authentication/      # Sessions, refresh tokens, and login attempts
│   ├── authorization/       # Roles, permissions, and their assignments
│   ├── bootstrap/           # Initial roles, permissions, and master user setup
│   ├── identity/            # Users, credentials, login, and JWT token services
│   └── reliability/         # Idempotency-related support and Inbox operations
└── shared/
    ├── domain/              # Shared models, results, paging, errors, and ports
    └── infrastructure/      # JDBC, transactions, security, and idempotency
```

Within modules, the code commonly separates:

- **Adapters** — entry points such as REST controllers and persistence implementations.
- **Application services/use cases** — business operations and orchestration.
- **Domain** — entities and business concepts.
- **Ports** — contracts used to connect application logic to adapters.

## API Overview

The API uses JSON request and response bodies. Most protected resources require a bearer access token in the
`Authorization` header. Endpoints that support idempotency can also accept an `Idempotency-Key` header.

| Area                        | Base path               | Main operations                                                                             |
|-----------------------------|-------------------------|---------------------------------------------------------------------------------------------|
| Authentication              | `/v1/auth`              | Register, login, and log out all sessions/tokens for the authenticated user                 |
| Users                       | `/v1/user`              | List users, retrieve the current user, update the current user, and delete the current user |
| User credentials            | `/v1/user-credentials`  | Update a user's credentials                                                                 |
| Permissions                 | `/v1/permission`        | Create, list, retrieve, update, delete, and check permissions                               |
| Roles                       | `/v1/role`              | Create, list, retrieve, update, delete, and check roles                                     |
| User-role assignments       | `/v1/user-role`         | Assign roles to users, remove assignments, and check assignments                            |
| Role-permission assignments | `/v1/role-permission`   | Assign, list, update, and remove permissions for roles                                      |
| Refresh tokens              | `/api/v1/refresh-token` | Revoke all refresh tokens for the authenticated user or delete a token by ID                |
| Inbox                       | `/v1/inbox`             | List, retrieve, update, delete, and check Inbox records                                     |

The full endpoint contract should be treated as the source of truth. In Quarkus development mode, the OpenAPI document
and Swagger UI are typically available at:

- OpenAPI: `http://localhost:8080/q/openapi`
- Swagger UI: `http://localhost:8080/q/swagger-ui`

## Prerequisites

- JDK 25
- Git
- Docker, if you want to run PostgreSQL in a container
- OpenSSL, to generate local RSA keys

## Getting Started

### 1. Clone the repository

```bash
git clone <repository-url>
cd janus
```

Replace `<repository-url>` with the repository's Git URL.

### 2. Start PostgreSQL

The repository does not include a Docker Compose file. You can start a local PostgreSQL instance with Docker:

```bash
docker run --name janus-postgres \
  -e POSTGRES_DB=janus \
  -e POSTGRES_USER=janus \
  -e POSTGRES_PASSWORD=change-me \
  -p 5432:5432 \
  -d postgres:17
```

Set the connection details for the application. Replace the example password with a strong local value matching the
container configuration:

```bash
export QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://localhost:5432/janus
export QUARKUS_DATASOURCE_USERNAME=janus
export QUARKUS_DATASOURCE_PASSWORD='change-me'
```

Flyway is configured to apply the SQL migrations in `src/main/resources/db/migrations` during startup.

### 3. Generate local JWT keys

The application expects an RSA private key and public key on the classpath. The private key is used to sign JWTs; the
public key is used for validation.

From the repository root, run:

```bash
openssl genpkey -algorithm RSA \
  -out src/main/resources/privateKey.pem \
  -pkeyopt rsa_keygen_bits:2048

openssl pkey \
  -in src/main/resources/privateKey.pem \
  -pubout \
  -out src/main/resources/publicKey.pem
```

Both files are intentionally excluded from version control. **Never commit private keys or production credentials.**
Generate separate keys for each environment and use a secret-management solution in deployed environments.

### 4. Configure bootstrap credentials

On startup, Janus bootstraps configured roles, permissions, and a master user using the `janus.bootstrap` section in
`src/main/resources/application.yaml`.

Before running the application outside a disposable local environment, override the bootstrap master user's default
credentials through Quarkus configuration/environment variables. For example:

```bash
export JANUS_BOOTSTRAP_MASTER_USERNAME=admin
export JANUS_BOOTSTRAP_MASTER_EMAIL=admin@example.com
export JANUS_BOOTSTRAP_MASTER_FULL_NAME='System Administrator'
export JANUS_BOOTSTRAP_MASTER_PASSWORD='use-a-strong-unique-password'
```

Do not reuse development credentials in staging or production. Review JWT issuer and expiration settings in
`application.yaml` for the environment in which the API will run.

### 5. Run the application

Start Quarkus in development mode:

```bash
./mvnw quarkus:dev
```

On Windows, use `mvnw.cmd quarkus:dev`.

By default, the HTTP server listens on port `8080`. The configured CORS origin defaults to `http://localhost:3000` and
can be overridden with `FRONT_URL`.

## Testing

Run the test suite with:

```bash
./mvnw test
```

The test suite includes coverage for authentication, token/session services, users and credentials, authorization
operations, Inbox operations, repositories, and application use cases. Ensure the required database and test
configuration are available before running the tests; this repository does not currently include a Compose-based test
environment.

## Packaging and Docker

Build the application package:

```bash
./mvnw package
```

Build the provided JVM container image:

```bash
docker build -f src/main/docker/Dockerfile.jvm -t janus:local .
```

The Docker image still requires valid database configuration, JWT key material included at build time, and secure
bootstrap configuration. Treat keys copied into an image as secrets and use an appropriate deployment strategy for
production workloads.

## Current Scope Notes

Some areas have supporting domain/application code or database schema but are not yet exposed as complete REST APIs. In
particular, the current `SessionController` and `OutboxController` are placeholders. Review the source and generated
OpenAPI document before relying on a specific endpoint or integration in another system.

## Security Notes

- Keep `privateKey.pem`, database passwords, and bootstrap credentials out of source control.
- Replace sample and default bootstrap credentials before any shared or production deployment.
- Use HTTPS and an appropriate secret-management system outside local development.
- Verify JWT issuer, token lifetimes, CORS origins, role/permission assignments, and endpoint authorization rules for
  each environment.
- **JWT issuer configuration needs review:** `TokenService` issues tokens with the issuer `https://janus.api`; the
  supplied `application.yaml` also configures `quarkus.smallrye-jwt.verify.issuer` as `https://controllibrary.org`. Make
  the verification issuer consistent with the issuer used in generated tokens before relying on JWT authentication.
- The supplied `application.yaml` defines a default bootstrap password. Override it before running the application in
  any shared environment.

## License

No license file was present in the supplied repository archive. Add a `LICENSE` file if you intend to publish the
project with an explicit license.
