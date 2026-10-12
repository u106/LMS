-- Note: instructor_id has NO foreign key to users, on purpose.
-- Courses and users live in different modules, and a database-level FK
-- across that boundary would make the two tables impossible to separate
-- later. See docs/adr/adr-006-cross-module-references-by-id.md.
-- The index is still worth having - "my courses" filters on it constantly.

CREATE TABLE courses (
    id            CHAR(36)     NOT NULL,
    title         VARCHAR(255) NOT NULL,
    description   TEXT,
    instructor_id CHAR(36)     NOT NULL,
    status        VARCHAR(20)  NOT NULL,
    created_at    TIMESTAMP(6) NOT NULL,
    updated_at    TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_courses_instructor_id (instructor_id),
    INDEX idx_courses_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
