package com.hosiyar.lms.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Turns on the auditing that fills in BaseEntity's createdAt / updatedAt.
 *
 * This lives on its own config class rather than on LmsApplication for a
 * concrete reason: @EnableJpaAuditing registers a bean that demands a JPA
 * metamodel (the set of entities). A @WebMvcTest slice loads only the web
 * layer with no entities, so if this annotation sat on the main application
 * class, every controller slice test would drag in JPA auditing and fail
 * with "JPA metamodel must not be empty".
 *
 * Keeping it in a persistence-layer config class means the full app still
 * gets auditing, while web-only tests never touch it.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
