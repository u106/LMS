# ADR-003: MySQL as the primary DBMS

**Status:** Accepted
**Date:** 2026-07-17

## Context

PostgreSQL (mature ecosystem, native `pgvector` extension for the
planned AI Doubt Solver module) and MariaDB (MySQL-compatible fork,
native `VECTOR` type generally available since 11.8 LTS) were both
considered as alternatives. MySQL was chosen for its broad industry
adoption and familiarity to the widest audience segment.

Plain, self-hosted MySQL Community does not include native vector
search in the free edition — that capability currently ships as part
of Oracle's commercial MySQL AI / HeatWave offering, not the
standard Docker-friendly distribution.

## Decision

Use MySQL as the DBMS for every module.

The vector store needed for Module 4 (AI Doubt Solver) is an **open
decision**, deferred until that module. Redis Stack's vector search
is the current leading candidate, since Redis is already part of the
stack.

## Consequences

**Positive**
- One relational engine across the entire project, widely known by
  the target audience
- Simpler local dev — no second database system to run for most of
  the series

**Negative**
- Module 4 will need either an external/secondary vector store or a
  workaround, unlike a Postgres+pgvector or MariaDB-only setup where
  the same database would have handled it natively
- This decision is explicitly revisited, not fully resolved, as of
  this ADR
