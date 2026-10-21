# Spec Delta

## Purpose

Defines the observable token and login security contract for authenticated users, including the claims carried by issued tokens, client validation, expiration, and protection of sensitive values.

## ADDED Requirements

### Requirement: Login preserves the documented endpoint contract
The login endpoint MUST preserve its documented route, successful response fields, user data, and existing username/password validation and credential-error behavior, except where this change explicitly defines a token or client contract change.

#### Scenario: Successful login response
- **WHEN** a user submits valid credentials and an allowlisted client to `POST /auth/login`
- **THEN** the endpoint responds with HTTP 200 and includes `token`, `expiresIn`, `user`, and the documented `debug` field
- **AND** `user.username`, `user.displayName`, and `user.role` describe the authenticated user

#### Scenario: Missing username or password
- **WHEN** the request is null or has a null, empty, or blank username or password
- **THEN** the endpoint responds with HTTP 400 using the documented validation message

#### Scenario: Invalid credentials
- **WHEN** the username and password do not authenticate a user
- **THEN** the endpoint responds with HTTP 401 using the documented invalid-credentials response

### Requirement: Login accepts only an allowlisted client
The login endpoint MUST require a client value that belongs to the documented allowlist, and MUST reject absent, blank, or non-allowlisted values without issuing a successful token. The allowlist values are a required product decision to record before implementation.

#### Scenario: Allowlisted client
- **WHEN** a login request contains a client identifier in the documented allowlist and valid credentials
- **THEN** the login may succeed and the issued token's `client` claim equals that identifier

#### Scenario: Missing or blank client
- **WHEN** a login request omits `client` or supplies a null, empty, or blank value
- **THEN** the endpoint responds with HTTP 400 and does not issue a token

#### Scenario: Client is not allowlisted
- **WHEN** a login request contains a client identifier outside the documented allowlist
- **THEN** the endpoint responds with HTTP 400 and does not issue a token

### Requirement: Issued tokens contain the required identity and time claims
A token issued after successful login MUST contain `sub`, `client`, `iat`, and `exp`; `sub` MUST identify the authenticated user and `client` MUST equal the validated request value. Any existing user-role information carried by the token MUST continue to reflect the authenticated user's role.

#### Scenario: Required claims match the login
- **WHEN** the endpoint issues a token for a successful login
- **THEN** the token contains `sub`, `client`, `iat`, and `exp`
- **AND** `sub` identifies the authenticated user and `client` matches the request
- **AND** any role information in the token matches the authenticated user's role

### Requirement: Token expiration agrees with the login response
An issued token MUST have an expiration later than its issue time, and `expiresIn` MUST represent the remaining lifetime in seconds within a documented tolerance. The tolerance MUST be documented before implementation.

#### Scenario: Response lifetime matches token expiration
- **WHEN** a successful login response is inspected at issuance time
- **THEN** `expiresIn` is a non-negative integer in seconds
- **AND** it agrees with `exp` minus the current time within the documented tolerance

#### Scenario: Expired token
- **WHEN** a token is evaluated at or after its `exp` time
- **THEN** it is considered expired and MUST NOT be accepted as valid

### Requirement: The signing secret is not exposed outside the laboratory contract
The signing secret MUST NOT be exposed in issued token contents or application logs. A default secret MUST NOT be used outside the laboratory sandbox; if retained for the sandbox, its lab-only status MUST be documented.

#### Scenario: Token and logs do not expose secrets
- **WHEN** a token is issued and the login flow is processed
- **THEN** neither the token contents nor application logs expose the signing secret

#### Scenario: Non-laboratory configuration
- **WHEN** the service runs outside the laboratory sandbox
- **THEN** token signing does not rely on a laboratory default secret

### Requirement: Authentication evidence covers the documented outcomes
The authentication behavior MUST have verification evidence for successful login, invalid credentials, and token expiration, without exposing tokens or secrets in that evidence.

#### Scenario: Required behavior is covered
- **WHEN** the authentication verification evidence is reviewed
- **THEN** it covers successful login, invalid credentials, and an expired token
- **AND** it contains no token or secret values