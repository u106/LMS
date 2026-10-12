-- Like the courses table, and even more so: no foreign keys at all. Both
-- student_id and course_id point into other modules, so referential integrity
-- across those boundaries is upheld in application code, not by MySQL. See
-- docs/adr/adr-006-cross-module-references-by-id.md.
--
-- The unique constraint is the real enforcement of "you can't enrol twice":
-- even if two concurrent requests both pass the application-level check, the
-- database will reject the second insert. Application code catches that and
-- turns it into a clean error rather than a 500 - the same race-condition
-- lesson as the very first user-registration video.

CREATE TABLE enrollments (
    id          CHAR(36)     NOT NULL,
    student_id  CHAR(36)     NOT NULL,
    course_id   CHAR(36)     NOT NULL,
    enrolled_at TIMESTAMP(6) NOT NULL,
    created_at  TIMESTAMP(6) NOT NULL,
    updated_at  TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_enrollment_student_course UNIQUE (student_id, course_id),
    INDEX idx_enrollments_student (student_id),
    INDEX idx_enrollments_course (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
