package com.hosiyar.lms.user.service;

import com.hosiyar.lms.user.api.UserDirectory;
import com.hosiyar.lms.user.api.UserSummary;
import com.hosiyar.lms.user.entity.User;
import com.hosiyar.lms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Lives inside the user module; only the UserDirectory interface it
 * implements is visible to anyone else.
 */
@Service
@RequiredArgsConstructor
public class UserDirectoryService implements UserDirectory {

    private final  UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<UserSummary> findById(UUID userId) {
        return userRepository.findById(userId).map(this::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, UserSummary> findAllByIds(Set<UUID> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(userIds).stream()
                .map(this::toSummary)
                .collect(Collectors.toMap(UserSummary::id, Function.identity()));
    }

    private UserSummary toSummary(User user) {
        return new UserSummary(user.getId(), user.getName());
    }
}
