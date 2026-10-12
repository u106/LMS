package com.hosiyar.lms.user.security;

import com.hosiyar.lms.common.security.AuthenticatedUser;
import com.hosiyar.lms.user.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Adapts our User entity to what Spring Security expects, without making the
 * JPA entity itself implement UserDetails - keeps persistence and security
 * concerns separate, the same way DTOs keep persistence and API concerns
 * separate.
 *
 * Also implements the shared kernel's AuthenticatedUser, which is how other
 * modules read the caller's id without importing anything from this module.
 */
public class CustomUserDetails implements UserDetails, AuthenticatedUser {

    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    @Override
    public UUID getId() {
        return user.getId();
    }

    @Override
    public String getEmail() {
        return user.getEmail();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
