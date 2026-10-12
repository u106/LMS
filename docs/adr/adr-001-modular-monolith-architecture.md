# ADR-001: Modular monolith over microservices or a plain monolith

**Status:** Accepted
**Date:** 2026-07-17

## Context

Building a project-based tutorial series (LMS) meant to demonstrate
industry-standard practices, developed and maintained by a small team.
The architecture needs to teach real separation of concerns without
the operational overhead of running several independently deployed
services for a learning project.

## Decision

Adopt a modular monolith: a single deployable Spring Boot application,
organized into feature packages (`user`, `course`, `enrollment`, etc.)
plus a shared kernel for cross-cutting concerns. Modules communicate
only through public service interfaces or published events — never by
reaching into another module's repository or entities directly.

## Consequences

**Positive**
- Simple local dev and deployment — one JAR, one process
- Still teaches real module boundaries, since they're enforced by
  convention and package visibility, not by network calls
- Sets up a believable "extract to microservice" bonus module later,
  since the boundaries already exist

**Negative**
- Nothing at the build level stops a boundary violation beyond
  Java's package-private visibility — requires discipline to keep
  modules honest
- Doesn't teach the operational realities of distributed systems
  (network failures, service discovery, distributed tracing) — those
  are explicitly out of scope until the bonus module
