# Design

## Context

See [proposal.md](proposal.md) for motivation and [specs/auth-token/spec.md](specs/auth-token/spec.md) for the behavior contract.

The service is a Java 21 / Quarkus 3.27.4 application. `AuthResource` handles `POST /auth/login`, validates username and password, queries the workshop database, and builds the response. `LoginRequest` already accepts `client`, but the endpoint currently ignores it. `LegacyTokenUtil` currently Base64-encodes username, role, expiry, and a compile-time secret; Base64 is encoding, not a signature. The current code does not read `AUTH_TOKEN_SECRET`, despite the Issue describing that configuration. `application.properties` has no token or client configuration, and the existing login test covers only a successful request.

The documented baseline includes `token`, fixed `expiresIn: 3600`, `user`, and `debug`, plus the existing 400 username/password and 401 credential errors. Requiring an allowlisted client, changing token contents, and deriving `expiresIn` from `exp` are intentional contract changes. Preserve the remaining response fields and health endpoint.

## Goals / Non-Goals

**Goals:**
- Produce a standard signed token with `sub`, `client`, `iat`, and `exp`, retaining the authenticated role claim where present.
- Keep token signing material and client allowlist in environment-specific configuration, with no production defaults.
- Preserve the documented login response shape and username/password error behavior except for the intentional client and token changes.
- Make expiration and sensitive-data properties deterministic and verifiable.

**Non-Goals:**
- Add token refresh, revocation, user sessions, authorization policies, or new endpoints.
- Change `/q/health`, CORS policy, database authentication, or user profile fields.
- Accept both legacy and new token formats as a compatibility mode.
- Decide the concrete client identifiers; the product owner must provide them before implementation/deployment.

## Protected Contracts

- Keep `POST /auth/login` and its JSON response envelope: `token`, `expiresIn`, `user`, and `debug`.
- Preserve `user.username`, escaped `user.displayName`, and `user.role`, and preserve the documented 400 username/password and 401 invalid-credential outcomes.
- Explicitly migrate the previously optional `client` to a required allowlisted value. Missing, blank, or non-allowlisted values return 400 and never issue a token.
- Replace the legacy token payload and fixed lifetime with the claims and expiration relationship in the delta spec. `expiresIn` must agree with `exp - now` within the agreed one-second tolerance.
- Leave `GET /q/health` and unrelated endpoint behavior unchanged.

## Components Affected

- `AuthResource`: validate the client before issuing a token and preserve the response contract and existing username/password handling.
- `LoginRequest`: retain the existing `client` input; no new request field is needed.
- `LegacyTokenUtil`: replace its insecure encoding responsibility with a token-issuance boundary; avoid leaking the signing secret or token through logs.
- Configuration: provide `AUTH_TOKEN_SECRET` and an explicit client allowlist per environment. An absent/empty allowlist must fail closed; production must not use a laboratory secret default.
- `pom.xml`: add a maintained JWT issuance dependency compatible with the Quarkus version if the selected Quarkus-integrated library is not already available.
- `AuthResourceTest` and focused token tests: characterize the current contract, then verify the changed contract and security properties. Update `docs/ENDPOINTS.md` and `docs/TESTING.md` when implementation is authorized; this design step does not edit them.

## Decisions

### Use a standard JWT library, not a hand-rolled token format

Use a maintained JWT library integrated with or compatible with Quarkus, with an HMAC-SHA-256 signature backed by external configuration. Prefer the Quarkus SmallRye JWT build integration after confirming its signing/configuration support against Quarkus 3.27.4. The emitted token carries the required standard claims and must not contain the signing secret.

Alternatives considered:
- Keep the custom Base64 format: rejected because it is not signed and exposes the embedded secret; adding custom signing and parsing would preserve a private format and increase cryptographic risk.
- Build JWT serialization/signing directly with JCA: rejected because it duplicates security-sensitive library behavior.
- Use asymmetric signing: defer because the current scope has one issuer and no verifier component in this service. Revisit if consumers need verification without access to signing material.

### Externalize signing secret and client allowlist

Read signing material from `AUTH_TOKEN_SECRET`; never compile it into the token utility. Supply a separate value per environment, prohibit a laboratory fallback outside the sandbox, and ensure logs redact tokens and secrets. Configure the allowed client identifiers explicitly per environment (proposed key: `AUTH_ALLOWED_CLIENTS`). An unset or empty allowlist fails closed. The actual list is a rollout input and is not inferred from the single `angular-workshop` example in the contract.

Alternatives considered:
- Hard-code client identifiers: rejected because the permitted set is environment/product policy and would require code changes to update.
- Accept any nonblank client: rejected because it does not satisfy the allowlist requirement.
- Default to a permissive list when configuration is absent: rejected because it can silently bypass client validation.

### Derive response lifetime from the issued expiration

Establish one issuance instant, derive `iat` and `exp` from it, and compute integer `expiresIn` from that same expiration. Allow a maximum one-second difference for epoch-second rounding, as agreed for this change. Keep the current 3600-second duration only if retained as the configured/default lifetime; the response value must not be independently hard-coded.

Alternatives considered:
- Keep returning fixed `3600`: rejected because it can disagree with the actual token expiration.
- Compare with zero tolerance: rejected because the claim is represented in whole epoch seconds and rounding can create a one-second difference.

### Keep the login boundary narrow

Perform client validation in the login request flow before token issuance. Keep credential lookup and the user response unchanged; do not add a token validation endpoint or broaden this change into authorization middleware.

Alternatives considered:
- Introduce a full authentication/authorization framework: rejected as unrelated to the committed login-token contract and too broad for this change.

## Incremental Strategy

1. Add characterization coverage before changing token or client behavior: successful login response fields and user data, current `expiresIn`, username/password validation, invalid-credential response, and the documented acceptance of a request without `client`. Do not assert or snapshot the current secret-bearing token contents.
2. Add focused tests for the token issuance boundary using deterministic time. Verify required claims, role preservation, signature verification, expiration arithmetic, and absence of secret material.
3. Introduce explicit secret and allowlist configuration with fail-closed behavior, then add endpoint tests for allowlisted, missing, blank, and non-allowlisted clients.
4. Integrate issuance into the endpoint while preserving the response envelope and credential error behavior. Replace characterization assertions for optional `client` and fixed lifetime with the intentional new contract assertions.
5. Verify logs/configuration and update endpoint/testing documentation. Coordinate token consumers before rollout because the legacy and JWT formats are incompatible.

The current `AuthResourceTest` does not contain the broader cases listed in `docs/TESTING.md`; add or verify characterization against the running code rather than assuming the documentation's test snippets already exist.

## Tests Before Critical Behavior Changes

- Characterize successful login with all currently documented response fields, including `debug`, user data, and the existing 3600-second response value.
- Characterize the existing optional-client behavior as a baseline test, then deliberately replace its expected success with the new required-client 400 scenario when the contract changes.
- Cover null/blank username and password and invalid credentials, asserting current status codes and documented messages.
- Do not capture the legacy token payload or embedded secret in fixtures, snapshots, or logs; characterization must not cement sensitive implementation details.
- Before switching production behavior, verify a signed token with an independent standards-based verifier and exercise expiration at the boundary with controlled time.

## Risks / Trade-offs

- **Legacy consumers expect the Base64 format** -> Coordinate consumer migration; do not silently dual-accept insecure tokens. Treat token format change as a compatibility break.
- **Issue description and source differ on HMAC/configuration** -> Base implementation decisions on observed code and make the new `AUTH_TOKEN_SECRET` source explicit; confirm deployment configuration before release.
- **Client allowlist values are not supplied** -> Do not deploy enforcement until the owner provides the exact values for each environment; empty configuration rejects all clients.
- **HMAC verifiers need access to signing material** -> Restrict secret access and distribution; reconsider asymmetric signatures if independent verifiers are introduced.
- **No token verifier or protected route exists in this service** -> The current service can issue an expiration claim but cannot demonstrate local rejection of an expired token. If Issue #12's expired-token criterion requires server-side acceptance/rejection here, clarify and expand scope before implementation.
- **Clock/rounding differences affect `expiresIn` assertions** -> Derive both values from one issuance instant and test against the agreed one-second tolerance.
- **Rollback can reintroduce the exposed-secret token** -> In production, roll back only to a secure compatible release or disable login; never restore the legacy issuer outside the laboratory. Expect clients to obtain a new token after rollback because formats/keys may differ.

## Migration Plan and Rollback

1. Provide the signing secret and non-empty allowlist through the deployment's secret/configuration mechanism before enabling the new issuer; validate configuration in each environment.
2. Confirm token consumers can validate the new JWT and agree on the signing algorithm, claims, and expiration semantics before switching login responses.
3. Deploy the issuer and client validation as one contract change, monitor login failures and configuration errors, and avoid logging token values.
4. If rollback is needed, prefer a secure prior release compatible with the new token contract. If none exists, disable login rather than restore the legacy token in a non-laboratory environment. After rollback, clients may need to authenticate again.

## Open Questions

- The exact `AUTH_ALLOWED_CLIENTS` values per environment must be supplied before implementation; the current contract only gives `angular-workshop` as an example, not an authoritative allowlist.
- Confirm that the expired-token criterion is about downstream JWT validation rather than server-side validation in this backend, which currently has no token-consuming endpoint.