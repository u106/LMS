# Module 3 — Enrollment & Progress: Requirements

## Purpose

Turn the catalogue into a classroom. Students enrol in courses, work through
lessons, and the system remembers exactly where each of them is - including
the position they paused a video at. This is the first module whose entities
exist mainly to connect the user and course modules.

## User stories

### US-1: Enrol in a course
As a student, I want to enrol in a published course, so that I can start
learning and track my progress.

**Acceptance criteria**
- Any authenticated user may enrol; enrolment is what makes them a student of
  that course
- Only `PUBLISHED` courses can be enrolled in; enrolling in a draft or a
  non-existent course is rejected
- A user cannot enrol in the same course twice - the second attempt is
  rejected cleanly, not with a duplicate row or a 500
- Enrolment records who enrolled from the authenticated token, never from the
  request body

### US-2: See my enrolments
As a student, I want a list of the courses I'm enrolled in, so I can pick up
where I left off.

**Acceptance criteria**
- Returns only the authenticated student's own enrolments
- Each entry shows the course and the student's overall completion for it
- Paginated

### US-3: Leave a course
As a student, I want to unenrol from a course I no longer want.

**Acceptance criteria**
- A student may unenrol only themselves
- **Unenrolling does NOT delete progress.** If the student re-enrols later,
  their previous position and completed lessons are still there. Progress is
  the student's history, not a property of the current enrolment.

### US-4: Resume a lesson where I left off
As a student, I want a lesson to remember the position I paused at, so I don't
have to find my place again.

**Acceptance criteria**
- The client reports the current position periodically while the lesson plays,
  and once when the student pauses or leaves
- Reopening the lesson returns the last saved position
- Progress can only be recorded for a course the student is enrolled in
- Frequent position updates must not translate into one database write each -
  see the non-functional requirements

### US-5: Have lessons complete themselves
As a student, I want a lesson to count as done once I've watched essentially
all of it, without having to tick a box.

**Acceptance criteria**
- A lesson becomes `completed` once watched position reaches **90%** of its
  length
- Completion is recorded once and does not flip back if the student rewatches
- Completing a lesson is a noteworthy event other parts of the system may
  react to (see US-7)

### US-6: See how far through a course I am
As a student, I want to see my overall completion for a course.

**Acceptance criteria**
- Completion is completed-lessons over total-lessons for that course
- Reflects progress that survived an unenrol/re-enrol cycle

### US-7: The system reacts to enrolment and completion
As the platform, when a student enrols or finishes a lesson, other features
(welcome messages, instructor notifications, certificates) should be able to
respond - without the enrolment or progress code knowing those features exist.

**Acceptance criteria**
- Enrolling publishes a `StudentEnrolled` event
- Crossing the completion threshold publishes a `LessonCompleted` event
- The code that enrols or records progress does not call those features
  directly; it announces what happened and returns
- For this module the events are handled in-process; Kafka comes in Module 7

## Non-functional requirements

- Position heartbeats must not each become a MySQL write. The current position
  is held in a fast cache (Redis) and flushed to MySQL periodically. A student
  scrubbing a video must not generate hundreds of database writes (ADR-009).
- Cross-module reads (is this course published? how many lessons does it have?)
  go through the course module's public interface only (ADR-006).
- Enrolment uniqueness is enforced at the database level, not only in
  application code.

## Out of scope (this module)

- Certificates for finishing a course (Module 5)
- Payment before enrolment (Module 6)
- Real cross-service events over Kafka (Module 7)
- Quizzes or graded assessments (Module 5)

## Traceability

Chapter 6's tests map back to these criteria - especially US-1's
enrol-twice rule, US-4's enrolled-only gate, and US-7's event publication.
