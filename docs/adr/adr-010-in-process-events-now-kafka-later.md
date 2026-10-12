# ADR-010: In-process domain events now, Kafka later

**Status:** Accepted
**Date:** 2026-09-01

## Context

Enrolling and completing a lesson are moments other features will want to react
to: welcome messages, instructor notifications, certificates. If the enrolment
code calls each of those directly, it accumulates knowledge of unrelated
features and becomes a magnet for change.

Events decouple this: the enrolment code announces "this happened" and returns;
whoever cares subscribes. The question is only the mechanism - Spring's
in-process events, or Kafka across services.

## Decision

Use Spring's `ApplicationEventPublisher` for in-process domain events now
(`StudentEnrolled`, `LessonCompleted`). Defer Kafka to Module 7, where the
Notifications module and the move toward cross-service messaging make it earn
its keep.

## Consequences

**Positive**
- Enrolment and progress code stay focused: they publish and return
- The decoupling principle is taught with zero new infrastructure
- Module 7's Kafka work becomes "the same idea across a network" rather than a
  brand-new concept and a new tool at once

**Negative**
- In-process events are synchronous by default and live and die with the app -
  they are not durable, and they do not cross service boundaries. That is fine
  for a monolith and is exactly the limitation Kafka later removes.
- One more layer of indirection than a direct method call, which in a
  single-listener case is slightly more than strictly needed - justified by
  the teaching payoff and the Module 7 setup.

**Rejected alternative**
Introducing Kafka here. It would teach the messaging idea and a heavyweight
tool simultaneously, and add infrastructure this module does not otherwise
need.
