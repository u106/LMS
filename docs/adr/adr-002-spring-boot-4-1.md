# ADR-002: Spring Boot 4.1 over Spring Boot 3.5

**Status:** Accepted
**Date:** 2026-07-17

## Context

Spring Boot 3.5.x reached end-of-life on June 30, 2026, and no longer
receives security patches. Spring Boot 4.1 (Spring Framework 7) is the
current stable release, but is recent enough that some third-party
libraries, tutorials, and community answers haven't fully caught up —
notably the modularized starter artifact names (e.g.
`spring-boot-starter-web` → `spring-boot-starter-webmvc`) and the
Spring Security 7 DSL changes.

## Decision

Target Spring Boot 4.1 for the whole project.

## Consequences

**Positive**
- The project targets an actively patched, current stack — more
  defensible for a series pitched as "industry standard"
- Forces the series to teach the new modular starter names and
  Security 7 DSL as real content, rather than glossing over them
- No migration cost, since this is a greenfield project

**Negative**
- Thinner community documentation and fewer existing tutorials to
  cross-reference when something doesn't behave as expected
- Some third-party libraries may lag in official Boot 4
  compatibility, requiring extra verification before adoption
