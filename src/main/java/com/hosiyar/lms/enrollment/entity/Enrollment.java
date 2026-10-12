package com.hosiyar.lms.enrollment.entity;

import com.hosiyar.lms.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Links a student to a course - and that is its entire reason to exist.
 *
 * Note there are no JPA relationships here at all. Both the student and the
 * course live in other modules, so both are referenced by plain UUID and
 * resolved through those modules' public interfaces. This is the purest
 * example of ADR-006 in the whole project: an entity whose only job is to
 * join two modules, doing it without a single foreign key across a boundary.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "enrollments",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_enrollment_student_course",
                columnNames = {"student_id", "course_id"}
        )
)
public class Enrollment extends BaseEntity {

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "student_id", length = 36, nullable = false)
    private UUID studentId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "course_id", length = 36, nullable = false)
    private UUID courseId;

    @Column(name = "enrolled_at", nullable = false)
    private Instant enrolledAt;

	public UUID getStudentId() {
		return studentId;
	}

	public void setStudentId(UUID studentId) {
		this.studentId = studentId;
	}

	public UUID getCourseId() {
		return courseId;
	}

	public void setCourseId(UUID courseId) {
		this.courseId = courseId;
	}

	public Instant getEnrolledAt() {
		return enrolledAt;
	}

	public void setEnrolledAt(Instant enrolledAt) {
		this.enrolledAt = enrolledAt;
	}
    
}
