# ADR-008: Rich progress tracking with resume position

**Status:** Accepted
**Date:** 2026-09-01

## Context

Lesson progress can be modelled two ways. The simple way is a boolean per
lesson: done or not done, like ticking a checklist. The rich way also records
*where* the student is - the position they paused a video at - so a lesson can
resume where they left off, the way any real streaming product does.

The simple model is one write per lesson and nothing new to teach. The rich
model is what learners actually expect from a platform, and it introduces a
genuinely new engineering concern: frequent position updates.

## Decision

Track rich progress. `LessonProgress` stores `lastPositionSeconds` alongside
the completion state. A lesson is marked `completed` automatically once
watched position reaches 90% of its length (ADR references US-5), rather than
by a manual tick.

Progress is keyed by (student, lesson) and stored independently of the
enrolment row, so it survives unenrolling and re-enrolling.

## Consequences

**Positive**
- Lessons resume where the student left off - a real product feature, not a
  toy checklist
- Completion is automatic and consistent
- Progress is durable across enrol/unenrol cycles

**Negative**
- Position is reported frequently while a lesson plays, so a naive
  implementation would flood MySQL with writes. This cost is real and is
  addressed separately in ADR-009 - rich tracking is what *creates* the need
  for the write-behind cache.
- "What counts as a position" and "when does the client report it" become API
  design decisions we have to make explicit.
