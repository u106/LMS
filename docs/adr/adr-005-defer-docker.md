# ADR-005: Assume a locally-installed MySQL for early modules, defer Docker

**Status:** Accepted
**Date:** 2026-07-17

## Context

The original bootstrap plan used Docker Compose to run MySQL (and Redis)
locally, so viewers wouldn't need to install a database directly. In
practice, requiring Docker before writing a single line of application
code adds friction for anyone following along who hasn't set it up
before — it's an extra tool to install and understand before the series
has taught anything about *why* it's useful.

## Decision

For the early modules, assume the viewer already has MySQL installed and
running locally (`localhost:3306`), and connect directly to it via the
`dev` profile. No Docker Compose file exists yet in the project.

Docker is introduced properly in Module 11 (Containerization & CI/CD),
once there's a real, multi-piece local environment (MySQL, Redis, Kafka)
worth automating — and once viewers have felt the manual setup firsthand,
so the value of Docker Compose actually lands instead of being taken on
faith.

## Consequences

**Positive**
- Removes an early barrier to entry — no Docker installation required to
  follow the first several modules
- Module 11 has a stronger payoff: "here's what this automates" lands
  better after doing it manually at least once

**Negative**
- Each viewer's local MySQL setup (version, auth plugin, existing
  databases) may differ slightly, which can surface small environment
  differences a container would have hidden
- Redis and Kafka, when introduced, will also need local installation
  instructions until Module 11 — revisit this ADR if that becomes
  cumbersome before then
