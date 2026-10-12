package com.hosiyar.lms.user.service;

import com.hosiyar.lms.common.exception.BadRequestException;
import com.hosiyar.lms.common.exception.ResourceNotFoundException;
import com.hosiyar.lms.user.dto.RegisterRequest;
import com.hosiyar.lms.user.dto.UserResponse;
import com.hosiyar.lms.user.entity.Role;
import com.hosiyar.lms.user.entity.User;
import com.hosiyar.lms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BadRequestException("An account with this email already exists");	
        }

        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.STUDENT);

        User saved;
        try {
            saved = userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            // Closes the race condition the existsByEmail() check above can't:
            // two concurrent registrations with the same email can both pass
            // that check before either commits. The database-level unique
            // constraint (see the Flyway migration) is the real guarantee -
            // this just turns its violation into the same clean error
            // instead of a raw SQL exception leaking out.
            throw new BadRequestException("An account with this email already exists");
        }

        return toResponse(saved);
    }

    /**
     * The email comes from the validated JWT, never from anything the client
     * sent in the request body or a path variable - otherwise anyone could
     * read anyone else's profile just by passing a different value.
     */
    @Transactional(readOnly = true)
    public UserResponse getByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toResponse(user);
    }

    /**
     * Method-level authorization: the rule lives next to the operation it
     * protects, so it holds no matter which controller or path reaches it.
     * Note @EnableMethodSecurity in SecurityConfig - without it, this
     * annotation is silently ignored.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}

