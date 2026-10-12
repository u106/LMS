# Module 2 — Course Management: Requirements

## Purpose

Instructors author courses and the lessons inside them. Students browse the
catalogue and view course content. This is the first module to build on
Module 1's identity system, and the first to reference another module's data.

## User stories

### US-1: Create a course
As an instructor, I want to create a course with a title and description, so
that I can start building content for students.

**Acceptance criteria**
- Only a user with the `INSTRUCTOR` role can create a course; a `STUDENT`
  attempting it is rejected with `403`
- The course is owned by whoever created it - ownership is taken from the
  authenticated user, never from the request body
- A new course starts as `DRAFT`, not visible in the public catalogue
- Title is required; invalid input is rejected with `400` and field-level
  detail

### US-2: Browse the catalogue
As a visitor - logged in or not - I want to browse available courses, so I
can decide what to learn.

**Acceptance criteria**
- The catalogue endpoint requires no authentication
- **Only `PUBLISHED` courses appear.** Drafts must never leak into the
  public list or be viewable by a non-owner
- Results are paginated
- Each entry shows title, short description, and the instructor's name

### US-3: View a single course
As a visitor, I want to open a course and see its details and lesson list,
so I know what it covers before enrolling.

**Acceptance criteria**
- A `PUBLISHED` course is viewable by anyone
- A `DRAFT` course is viewable only by its owning instructor; anyone else
  gets `404`, not `403` - a stranger shouldn't be able to tell whether a
  draft exists at all

### US-4: See my own courses
As an instructor, I want a list of the courses I've created, including
drafts, so I can manage my own work.

**Acceptance criteria**
- Returns only courses owned by the authenticated instructor
- Includes both `DRAFT` and `PUBLISHED`
- The list is scoped by the token, never by an id supplied by the client

### US-5: Edit and delete my own course
As an instructor, I want to update or delete a course I own, so I can
correct and manage my content.

**Acceptance criteria**
- An instructor can update or delete **only their own** courses
- An instructor attempting to modify another instructor's course is rejected
  with `403`, and nothing is changed
- Publishing is an update: moving `DRAFT` to `PUBLISHED` makes the course
  publicly visible

### US-6: Manage lessons within a course
As an instructor, I want to add, reorder, edit and remove lessons in my own
course, so that the course has structure and content.

**Acceptance criteria**
- Lessons belong to exactly one course
- Lessons have an explicit position, so the order is deliberate rather than
  incidental
- The same ownership rule as US-5 applies: only the owning instructor of the
  parent course may modify its lessons
- Deleting a course removes its lessons

### US-7: Attach a file to a lesson
As an instructor, I want to upload a video or document for a lesson, so
students have something to learn from.

**Acceptance criteria**
- Only the owning instructor may upload to a lesson
- File type and size are validated before the file is stored
- The database stores file *metadata*; the file itself is not stored as
  database bytes
- The storage location is an implementation detail behind an interface -
  see the open decision below

## Non-functional requirements

- Course listings are paginated; no unbounded "return everything" endpoint
- Cross-module access to user data goes through the user module's public
  service interface only (ADR-006)
- Listing courses must not perform one instructor lookup per course

## Open decision (settled in chapter 4)

Where uploaded files live - local disk or cloud object storage. Deliberately
deferred; the storage interface is designed so the choice can change without
touching calling code.

## Out of scope (this module)

- Enrolment and progress tracking (Module 3)
- Payments and pricing (Module 6)
- Course reviews and ratings
- Categories and search

## Traceability

Chapter 5's tests map back to these acceptance criteria - in particular
US-5's rejection case, which is the security-critical one for this module.
