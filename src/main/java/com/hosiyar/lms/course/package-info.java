/**
 * Course catalogue, lessons and content.
 *
 * Course CRUD with ownership-based authorization, and lessons (chapters
 * 2-3). File upload lands in chapter 4.
 *
 * This module references users by id only, through the user module's public
 * UserDirectory interface - never its entity or repository. See ADR-006.
 * Inside the module, ordinary JPA relationships are used normally: Lesson
 * has a real @ManyToOne to Course.
 */
package com.hosiyar.lms.course;
