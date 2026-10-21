# Security Policy

## Project Status

Janus is an identity and access management (IAM) project under active development. The controls described below reflect
the code reviewed in the repository and should not be interpreted as a security certification or a statement that the
system is production-ready.

**Do not use the current development configuration to protect production identities or sensitive data until the
production-hardening items in this document have been addressed and tested.**

## Supported Versions

No stable release or formal support window is currently defined. Security reports should target the latest commit on the
repository's default branch. If versioned releases are introduced, this section should be updated with their support
status.

## Reporting a Vulnerability

Please do not disclose exploitable security issues in a public issue, pull request, discussion, or social media post
before the maintainers have had an opportunity to investigate.

1. Open the repository's **Security** tab and use **Report a vulnerability** / private vulnerability reporting if that
   feature is enabled.
2. If private reporting is unavailable, contact the repository maintainer privately through an established private
   channel before publishing technical details.
3. Include the affected commit or version, impact, prerequisites, and a concise reproduction procedure. Redact
   credentials, tokens, personal data, and production information.

Please report suspected credential exposure immediately and rotate the affected credential rather than waiting for a
response. No response-time SLA is currently defined.

## Existing Security Controls

The current code includes the following controls; each should be validated with automated tests before relying on it in
a deployment:

- **Password hashing:** password credentials are processed with Argon2id. The current configuration uses 2 iterations,
  65,536 KiB of memory, and parallelism of 1. These values should be benchmarked and reviewed against the deployment's
  threat model and hardware.
- **JWT signing and validation:** access tokens are signed with RSA/SHA-256 (RS256), and the code configures public-key
  verification. Signing-key storage, issuer configuration, and expiration behavior still require hardening as described
  below.
- **Authorization:** permission checks are implemented through `@PermissionsAllowed` and permissions included in JWT
  claims. Because claims are copied into a token, permission changes may not take effect for already-issued tokens until
  they expire unless a live authorization or revocation check is added.
- **Login-attempt tracking:** failed and successful login attempts are recorded. The current login use case checks a
  threshold of three failed attempts within a 15-minute window for the supplied IP address and user account. This is not
  a substitute for edge-level rate limiting, and its behavior must be validated under concurrency and database failures.
- **Refresh-token lifecycle:** refresh-token records are associated with users and sessions and support revocation
  operations. The current implementation still needs the storage and lifecycle hardening described below.
- **Idempotency:** selected authentication and token-revocation operations use an idempotency annotation. Verify replay
  handling and persistence behavior with integration tests.

## Required Hardening Before Production

The reviewed development branch contains security-sensitive configuration and implementation gaps. Treat the following
as release blockers unless they are resolved or a documented, tested mitigation is in place.

### 1. Remove hard-coded credentials

The application configuration currently contains a default bootstrap-administrator password and literal production
datasource credentials. Do not reuse these values or deploy with them.

- Remove credentials from `application.yaml` and source control.
- Inject required values using environment-specific secret management, such as a cloud secret manager or orchestrator
  secret mount. Production startup should fail when required secrets are missing.
- Make bootstrap credentials one-time or externally supplied, require rotation during initial provisioning, and disable
  or remove the bootstrap path after setup where appropriate.
- If any of these values have ever been used outside a local environment or published in a repository, rotate them.
  Removing a value from the latest commit does not remove it from Git history.

### 2. Protect the JWT signing key

`TokenService` currently loads `/privateKey.pem` as a classpath resource. The `.gitignore` excludes the local key files,
but placing the private key inside application resources or a built artifact can expose it to anyone who obtains that
artifact. The private-key file was not present in the ZIP reviewed.

- Load the signing key from a protected external secret or key-management service rather than bundling it in the
  application artifact.
- Restrict access to the private key, document a rotation procedure, and rotate it immediately if exposure is suspected.
- Keep the verification key available only where needed and verify that key rotation does not leave an unintended trust
  path.
- Never commit private keys, real `.env` files, tokens, certificates containing private material, or backups of secrets.

### 3. Store refresh tokens as digests, not bearer credentials

The current code generates a random refresh-token value, assigns it to a field named `tokenHash`, persists that value,
and returns it to the client. The inspected path does not appear to hash the value before persistence; the field name
alone does not make the stored value a hash.

- Return the raw, cryptographically random token to the client only when issuing it.
- Persist only a one-way digest of the token (for example, SHA-256 over a high-entropy opaque token) and digest incoming
  tokens before lookup.
- Ensure refresh-token values are never written to application logs, audit messages, error responses, or telemetry
  attributes.
- Implement and test expiration, single-use rotation, replay detection, reuse response, and revocation semantics. Review
  the database schema and existing token flows together before changing this behavior.

### 4. Align JWT issuer and expiration settings

The configuration contains two different JWT issuer values, while `TokenService` sets the issuer claim to
`https://janus.api`. Confirm which verification settings are active in the deployed Quarkus configuration, make the
issuer configuration consistent, and add tests proving that tokens with the wrong issuer are rejected.

The expiration calculations also appear inconsistent: JWT generation calculates access-token expiry in minutes, while
the `TokenResponse` expiry field is calculated in days. Refresh-token expiry is currently calculated in minutes from the
configured value. Standardize the units and ensure the response advertises the same expiration as the signed token and
persisted refresh-token record.

### 5. Define revocation semantics for access tokens

Revoking a session or refresh token does not automatically invalidate an already-issued signed access token unless each
protected request checks session/revocation state or another revocation mechanism is used. Confirm this behavior before
promising immediate logout or immediate permission changes.

Choose and document an explicit strategy, such as short-lived access tokens, a session identifier checked on each
request, a denylist/version mechanism, or token introspection. Add tests for logout-all, user deactivation, role
changes, permission changes, and revoked sessions.

### 6. Trust client IP addresses only through known proxies

The login and registration controllers read `X-Forwarded-For` directly. A caller can submit this header themselves
unless the application is behind a trusted proxy and forwarding headers are configured and sanitized correctly.

- Configure the application and ingress/reverse proxy to trust forwarding headers only from known proxy addresses.
- Derive the client address using the trusted-proxy configuration; do not use an arbitrary client-supplied value for
  security decisions.
- Add edge-level throttling for authentication and registration endpoints, and test lockout behavior under parallel
  requests. Consider how account lockout could be abused to deny legitimate users access.

### 7. Tighten production HTTP and CORS configuration

The current CORS configuration allows all request headers while the allowed origin is configurable. Set explicit
production origins, methods, and headers according to the frontend and client requirements. Do not use wildcard origins
with credentialed requests.

- Require HTTPS for all public traffic; if TLS terminates at a proxy, secure the proxy-to-application path and configure
  forwarded headers deliberately.
- Do not expose management, health, metrics, or administrative endpoints publicly unless they are intentionally
  protected.
- Apply request-size limits, timeouts, and rate limits at the edge as appropriate.

### 8. Verify authorization and error handling

- Test that every sensitive endpoint denies anonymous requests and users without the exact required permission.
- Ensure privileged roles, permission management, impersonation, API-key operations, and audit access follow least
  privilege.
- Avoid returning parser, database, or internal exception details to clients. Keep detailed diagnostics in
  access-controlled logs with secrets and personal data redacted.
- Confirm that changes to a user's roles and permissions are handled correctly while JWTs remain valid.

### 9. Protect audit and login telemetry

IP addresses, user-agent strings, email addresses, and login-attempt reasons may be personal or security-sensitive data.
Define access controls, retention periods, and redaction rules for logs and audit records. Never log raw passwords,
access tokens, refresh tokens, private keys, or authorization headers.

### 10. Test and maintain the security boundary

Before release, include automated tests for at least:

- Missing, malformed, expired, incorrectly signed, and wrong-issuer JWTs.
- Authentication and authorization failures, including cross-user access and privilege escalation.
- Brute-force protection, spoofed forwarding headers, concurrent login attempts, and database failure behavior.
- Refresh-token digest lookup, expiry, rotation, replay, and revocation.
- Logout and permission changes while previously issued access tokens remain unexpired.
- Idempotency-key reuse and concurrent duplicate requests.

Run dependency and secret scanning in CI, keep Quarkus and dependencies patched, review security advisories, and avoid
disabling TLS or certificate verification to make local development work.

## Deployment Checklist

Before exposing a deployment to untrusted clients, confirm all of the following:

- [ ] All bootstrap and database credentials are supplied securely outside source control and have been rotated from
  development defaults.
- [ ] JWT signing keys are externalized and access-controlled; issuer and expiration behavior are consistent and tested.
- [ ] Refresh tokens are stored as digests and have tested rotation, replay, expiration, and revocation behavior.
- [ ] Access-token revocation behavior is explicit and covered by tests.
- [ ] HTTPS, trusted proxy handling, CORS allowlists, and edge-level rate limits are configured.
- [ ] Administrative endpoints and sensitive permissions follow least privilege.
- [ ] Logs, audit data, backups, and telemetry do not expose credentials or unnecessary personal information.
- [ ] Automated security tests and dependency/secret scans pass for the release commit.

## Scope

This file documents the project's security-reporting process and security concerns identified during code review. It is
not a complete penetration test, formal threat model, or guarantee that all vulnerabilities have been found.
