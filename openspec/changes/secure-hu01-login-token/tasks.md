# Tasks

Implementation checklist for GitHub Issue [#12](https://github.com/user-bcp-student15/workshop-auth-backend/issues/12), tracked by OpenSpec change `secure-hu01-login-token`.

## 1. Characterize the Existing Login Contract

- [ ] 1.1 Extend the successful-login test to assert the current HTTP 200 response envelope (`token`, `expiresIn`, `user`, `debug`), user fields, and `expiresIn: 3600`; verify with `mvn test` without decoding or snapshotting the legacy token contents.
- [ ] 1.2 Characterize current acceptance of a request without `client`, username/password validation, and invalid-credential status/message; verify each expected baseline response with `mvn test` before changing endpoint behavior.

## 2. Implement Token Issuance and Security Configuration

- [ ] 2.1 Confirm a maintained JWT signing library compatible with Quarkus 3.27.4 (prefer the Quarkus-integrated option from design.md), add its required dependency, and verify dependency resolution with `mvn test`.
- [ ] 2.2 Replace the legacy token payload with a signed token containing `sub`, `client`, `iat`, and `exp`, preserving the authenticated user's role claim; verify claims and signature with focused tests and a standards-based verifier, and assert that the signing secret is absent from token contents.
- [ ] 2.3 Derive `expiresIn` from the same issuance time and `exp`; use controlled time to verify the one-second tolerance, `exp > iat`, and rejection of an expired token by a standards-based verifier, without adding a token-consuming backend endpoint.
- [ ] 2.4 Load signing material from `AUTH_TOKEN_SECRET` and allowed clients from explicit environment configuration; verify missing/empty configuration fails closed outside the lab and captured application logs contain neither token nor secret values.

## 3. Integrate the Client Contract and Preserve Login Behavior

- [ ] 3.1 Require a nonblank client present in the configured allowlist before issuing a token; test that missing, blank, and non-allowlisted clients receive HTTP 400 with no token, and that an allowlisted client reaches authentication successfully.
- [ ] 3.2 Add endpoint-level regression tests proving successful login returns the required claims and preserves `user`, `debug`, username/password validation, and the documented invalid-credential 401 response; verify with `mvn test`.
- [ ] 3.3 Update `docs/ENDPOINTS.md` and `docs/TESTING.md` with the required client/allowlist configuration, JWT claims, `expiresIn` tolerance, secret configuration, sensitive-log constraint, breaking token-format change, and safe rollback note; verify examples and expected responses match the endpoint tests.

## 4. Verify the Issue Scope

- [ ] 4.1 Run `mvn verify` and `openspec validate "secure-hu01-login-token"`; confirm the login tests pass, token/secret values are not exposed, and no health endpoint or unrelated authentication behavior was changed.