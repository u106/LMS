package com.hosiyar.lms.course.entity;

import com.hosiyar.lms.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Contrast this with Course.instructorId deliberately.
 *
 * Lesson and Course both live in the course module, so an ordinary
 * @ManyToOne with a real foreign key is exactly right here. ADR-006 was
 * about crossing module boundaries, not about avoiding relationships
 * everywhere - inside a module, use JPA normally.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "lessons")
public class Lesson extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    /**
     * Explicit ordering. Without this, lesson order would depend on whatever
     * the database happened to return - JPA guarantees no ordering otherwise.
     */
    @Column(nullable = false)
    private Integer position;

    /**
     * Where the bytes live in object storage. Null until a file is uploaded.
     * The file itself is never stored in this table - see ADR-007.
     */
    @Column(name = "file_key", length = 512)
    private String fileKey;

    /**
     * The name the instructor's file had on their machine. Kept for display
     * only - it never influences the storage key, because a client-supplied
     * filename is untrusted input.
     */
    @Column(name = "file_name")
    private String fileName;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "file_size")
    private Long fileSize;

    public boolean hasFile() {
        return fileKey != null;
    }

	public Course getCourse() {
		return course;
	}

	public void setCourse(Course course) {
		this.course = course;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public Integer getPosition() {
		return position;
	}

	public void setPosition(Integer position) {
		this.position = position;
	}

	public String getFileKey() {
		return fileKey;
	}

	public void setFileKey(String fileKey) {
		this.fileKey = fileKey;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public Long getFileSize() {
		return fileSize;
	}

	public void setFileSize(Long fileSize) {
		this.fileSize = fileSize;
	}
    
}
