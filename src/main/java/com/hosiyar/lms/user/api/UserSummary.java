package com.hosiyar.lms.user.api;

import java.util.UUID;

/**
 * What other modules are allowed to see of a user.
 *
 * Deliberately narrow: no password hash, no role, no timestamps. Other
 * modules get exactly what they need to display a person, and nothing more.
 * See ADR-006.
 */
public record UserSummary(
        UUID id,
        String name
) {}
