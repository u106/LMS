package com.hosiyar.lms.user.api;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The user module's public face. This interface is the only thing other
 * modules are allowed to depend on - no entity, no repository, no internal
 * service. See ADR-001 and ADR-006.
 */
public interface UserDirectory {

    Optional<UserSummary> findById(UUID userId);

    /**
     * Batch lookup, keyed by user id.
     *
     * This exists specifically so a caller rendering a page of rows can
     * resolve every user in one query instead of one query per row. Without
     * it, listing 20 courses means 20 round trips - the N+1 problem that
     * ADR-006 flagged as the real cost of referencing across modules by id.
     */
    Map<UUID, UserSummary> findAllByIds(Set<UUID> userIds);
}
