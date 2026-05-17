package com.eprocure.iam.common.security;

import com.eprocure.iam.domain.model.User;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class UserPrincipal implements UserDetails {
    private final UUID id;
    private final String username;
    private final String fullName;
    private final String tokenHash;
    private final Set<String> permissions;

    private UserPrincipal(UUID id, String username, String fullName, String tokenHash, Set<String> permissions) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.tokenHash = tokenHash;
        this.permissions = Set.copyOf(permissions);
    }

    public static UserPrincipal from(User user, String tokenHash, Set<String> permissions) {
        return new UserPrincipal(user.getId(), user.getUsername(), user.getFullName(), tokenHash, permissions);
    }

    public UUID getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return permissions.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return username;
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
