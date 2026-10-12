# ADR-006: Cross-module references by ID, not by entity

**Status:** Accepted
**Date:** 2026-08-09

## Context

A `Course` is owned by an instructor, who is a `User` - an entity owned by
the `user` module. This is the first time a feature module needs to refer to
something another module owns, so it's the first real test of the boundary
rule set out in ADR-001: modules talk through public service interfaces or
events, never by reaching into each other's internals.

Two options:

**A. A JPA relationship.** `Course` holds `@ManyToOne User instructor`. The
database gets a real foreign key, and a single query can join and fetch the
instructor's name alongside the course.

**B. A plain ID.** `Course` holds `UUID instructorId`. When course code needs
the instructor's name, it asks the user module through its public service
interface.

Option A is more convenient and is what most Spring tutorials show. It also
means the `course` module imports `User` directly, can lazily traverse into
it, and can accidentally modify it - the boundary exists on paper only.

## Decision

Option B. Feature modules reference entities owned by other modules by ID,
and obtain any details they need through the owning module's public service
interface.

The `user` module exposes a narrow, read-only lookup for this purpose,
returning a DTO rather than the `User` entity - so no other module ever
holds a persistent `User`.

## Consequences

**Positive**
- The boundary is real, not aspirational. `course` cannot compile against
  `User` internals at all.
- Each module's tables can be reasoned about, migrated, and eventually
  extracted independently - directly enabling the monolith-to-microservice
  bonus module, where a foreign key across the seam would have to be
  unpicked anyway.
- Forces us to be explicit about exactly what one module needs from another,
  rather than lazily traversing an object graph.

**Negative**
- No database-level foreign key across the boundary, so referential
  integrity between `courses.instructor_id` and `users.id` is not enforced
  by MySQL. It has to be upheld in application code.
- Listing courses with instructor names becomes an N+1 problem by default -
  one lookup per course - unless the course module deliberately batches the
  request. This is a genuine cost, and the batching fix is taught in the
  chapter where it first bites.
- More code than a single `@ManyToOne` would have been.

**Note**
Within a single module, ordinary JPA relationships are still used normally -
`Lesson` to `Course` is a plain `@ManyToOne`, since both live in the
`course` module. This ADR is only about crossing module boundaries.
