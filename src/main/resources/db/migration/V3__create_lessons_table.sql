-- Contrast with V2's courses table: this one DOES have a foreign key.
-- Lessons and courses live in the same module, so there's no boundary being
-- crossed and nothing to unpick later. ON DELETE CASCADE means removing a
-- course removes its lessons, enforced by the database rather than hoped for
-- in application code.

CREATE TABLE lessons (
    id         CHAR(36)     NOT NULL,
    course_id  CHAR(36)     NOT NULL,
    title      VARCHAR(255) NOT NULL,
    content    TEXT,
    position   INT          NOT NULL,
    file_key   VARCHAR(512),
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_lessons_course
        FOREIGN KEY (course_id) REFERENCES courses (id)
        ON DELETE CASCADE,
    INDEX idx_lessons_course_position (course_id, position)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
