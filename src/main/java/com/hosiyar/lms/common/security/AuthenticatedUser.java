package com.hosiyar.lms.common.security;

import java.util.UUID;

/**
 * What any module is allowed to know about whoever is making the request.
 *
 * Without this, a controller in the course module would have to import
 * CustomUserDetails from the user module just to read the caller's id -
 * quietly breaking the boundary rule the moment we tried to honour it.
 * Defining the contract in the shared kernel keeps the dependency pointing
 * the right way: every module depends on common, common depends on nobody.
 */
public interface AuthenticatedUser {

    UUID getId();

    String getEmail();
}
