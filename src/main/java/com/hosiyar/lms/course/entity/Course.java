package com.hosiyar.lms.course.entity;

import com.hosiyar.lms.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

/**
 * Matches docs/design/module-02-course-management.md.
 *
 * Note what is deliberately NOT here: a @ManyToOne to User. The instructor
 * is referenced by plain id, because User belongs to another module and a
 * JPA relationship would let this module reach straight into it. See
 * ADR-006 - and note the cost: no database foreign key, and instructor
 * names have to be resolved through UserDirectory.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "courses")
public class Course extends BaseEntity {

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "instructor_id", length = 36, nullable = false)
    private UUID instructorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CourseStatus status;

    /**
     * Ownership question, asked in one place so every caller phrases it the
     * same way. Used heavily from chapter 3 onward.
     */
    public boolean isOwnedBy(UUID userId) {
        return instructorId.equals(userId);
    }

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public UUID getInstructorId() {
		return instructorId;
	}

	public void setInstructorId(UUID instructorId) {
		this.instructorId = instructorId;
	}

	public CourseStatus getStatus() {
		return status;
	}

	public void setStatus(CourseStatus status) {
		this.status = status;
	}
    
}
