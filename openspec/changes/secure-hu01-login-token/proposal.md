# Proposal

## Why

Issue [#12](https://github.com/user-bcp-student15/workshop-auth-backend/issues/12) commits the login flow to issue a signed token with a minimal set of claims and a verifiable expiration. The documented endpoint currently emits a legacy Base64 value with a fixed lifetime, does not use the accepted `client` input, and embeds its secret in the token payload; a clear contract is needed to make the security expectations testable without weakening the existing login behavior.

## What Changes

- **BREAKING**: require `client` on `POST /auth/login` and accept only clients in an allowlist. The issue does not supply the allowlist values; that set must be recorded before implementation.
- Define the successful token claims as `sub`, `client`, `iat`, and `exp`; `client` must match the validated request value.
- Define `expiresIn` as seconds remaining until `exp`, within a documented tolerance, instead of assuming the current fixed value of 3600.
- Require protection of the signing secret outside the laboratory sandbox, documentation if a lab-only default remains, and no logging of tokens or secrets.
- Require observable coverage for successful login, invalid credentials, invalid/expired tokens, and the documented expiration relationship. The HMAC-versus-JWT choice remains a technical decision and is not made by this proposal.
- Preserve the existing endpoint path, JSON login flow, username/password validation, invalid-credential response, user data, and other response fields. The token claims and `expiresIn` semantics, and the newly required allowlisted `client`, are intentional contract changes.
- Leave `GET /q/health` and unrelated authentication behavior unchanged.

## Capabilities

### New Capabilities
- `auth-token`: Defines the observable claims, client validation, expiration, and sensitive-data handling for tokens issued by login.

### Modified Capabilities

None. The repository has no existing OpenSpec capabilities.

## Impact

The change affects the documented contract for `POST /auth/login`, token consumers, and the security/configuration and test evidence around token issuance. It does not add endpoints or choose a signing algorithm. Before implementation, the allowed client identifiers and the documented expiration tolerance must be settled; this proposal does not derive either value from the current contract.