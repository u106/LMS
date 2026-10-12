# ADR-009: Redis as a write-behind cache for progress heartbeats

**Status:** Accepted
**Date:** 2026-09-01

## Context

Rich progress tracking (ADR-008) means the client reports the current playback
position every few seconds. Written straight to MySQL, a single ten-minute
lesson watched once could be a hundred-plus UPDATE statements - per student,
per lesson. That does not scale, and it is wasteful: only the *latest*
position actually matters.

## Decision

Hold the current position in Redis, and flush it to MySQL periodically (and on
pause/leave). The heartbeat endpoint writes to Redis - cheap to overwrite
constantly. A scheduled flush, plus an explicit flush when the student stops,
persists the latest value to `lesson_progress`. Completion detection (crossing
90%) also happens as positions come in, publishing `LessonCompleted` once.

This is the first real use of Redis, which has been in the stack since Module 1
but unused until now.

## Consequences

**Positive**
- MySQL sees a trickle of writes instead of a storm, regardless of how much a
  student scrubs
- Reads of "current position" are fast
- A genuinely production-grade pattern most tutorials never show

**Negative**
- A crash between flushes can lose the last few seconds of position - an
  acceptable loss for this data (you rewind a little), but it must be a
  conscious choice, not an accident
- Two stores now hold progress state, so the flush path and its failure modes
  have to be reasoned about
- Redis becomes a runtime dependency for a core feature, not just a nice-to-have

**Rejected alternative**
Writing every heartbeat straight to MySQL. Simplest to build, but does not
survive real usage - the exact anti-pattern this module demonstrates before
fixing.
