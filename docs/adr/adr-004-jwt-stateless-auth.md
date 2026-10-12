# ADR-004: JWT-based stateless authentication over server-side sessions

**Status:** Accepted
**Date:** 2026-07-17

## Context

The Users & Auth module needs to authenticate requests to a REST API
that may be consumed by a decoupled frontend and, potentially, future
mobile clients.

## Decision

Use stateless JWT access + refresh tokens via Spring Security 7,
rather than server-side session storage.

## Consequences

**Positive**
- Scales horizontally without sticky sessions or a shared session
  store
- Natural fit for REST APIs and future mobile clients
- No server-side session state to manage or replicate

**Negative**
- Revoking a token before its natural expiry is harder than
  invalidating a server-side session — would require additional
  infrastructure (e.g. a token blocklist in Redis) if needed later
- Requires careful tuning of access/refresh token expiry to balance
  security against how often users are forced to re-authenticate
