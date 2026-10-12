# Module 1 — Users & Authentication: Requirements

## Purpose

Establish the foundational identity system for the LMS: registration, login,
and role-based access. Every later module depends on this one.

## User stories

### US-1: Register an account
As a visitor, I want to register with my name, email, and password, so that
I can access the platform.

**Acceptance criteria**
- Email must be unique; a duplicate registration attempt returns a clear error
- Password is never stored in plaintext
- A successful registration returns the created user's `id`, `name`, `email`,
  and `role` — never the password or its hash
- Invalid input (malformed email, missing fields, weak password) is rejected
  with `400` and field-level error details

### US-2: Log in
As a registered user, I want to log in with my email and password, so that
I can access protected resources.

**Acceptance criteria**
- Correct credentials return an access token and a refresh token
- Incorrect credentials return `401` without revealing whether the email or
  the password was the wrong part
- Tokens carry the user's id and role as claims

### US-3: Refresh my session
As a logged-in user, I want to refresh my access token without re-entering
my password, so my session doesn't abruptly expire while I'm active.

**Acceptance criteria**
- A valid refresh token issues a new access token
- An expired or invalid refresh token is rejected with `401`

### US-4: Distinct roles
As the platform, I need users to have a role (`STUDENT`, `INSTRUCTOR`,
`ADMIN`), so access to instructor/admin-only actions can be restricted.

**Acceptance criteria**
- Role defaults to `STUDENT` at registration
- Endpoints requiring a specific role reject users without it, with `403`

### US-5: View my own profile
As a logged-in user, I want to fetch my own profile, so the frontend can
display who's logged in.

**Acceptance criteria**
- `GET /api/v1/users/me` returns the current user's `id`, `name`, `email`,
  `role`
- Requires a valid access token; returns `401` without one

## Non-functional requirements

- Passwords hashed with BCrypt — never logged, never returned in any response
- Access tokens short-lived, refresh tokens longer-lived — exact values
  finalized during chapter 4 implementation
- All `/auth` and `/users` request bodies validated with Bean Validation
  (`@Valid`)
- Sessions are stateless — no server-side session storage

## Out of scope (this module)

Explicitly deferred, not forgotten:

- OAuth2 / social login
- Email verification
- Password reset flow
- Account deactivation / soft delete

## Traceability

Chapter 6's tests should map directly back to these acceptance criteria —
each user story here becomes at least one test case.
