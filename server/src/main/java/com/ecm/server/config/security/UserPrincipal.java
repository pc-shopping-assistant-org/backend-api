package com.ecm.server.config.security;

import com.ecm.server.model.Account;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class UserPrincipal implements UserDetails {

    private final UUID accountId;
    private final UUID employeeId;
    private final String username;
    private final String password;
    private final String role;
    private final String status;
    private final Collection<? extends GrantedAuthority> authorities;

    public static UserPrincipal create(Account account, UUID employeeId) {
        String roleName = account.getRole() != null ? account.getRole().getName() : "ROLE_CUSTOMER";
        List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(roleName));

        return UserPrincipal.builder()
                .accountId(account.getId())
                .employeeId(employeeId)
                .username(account.getUsername())
                .password(account.getPassword())
                .role(roleName)
                .status(account.getStatus())
                .authorities(authorities)
                .build();
    }

    public static UserPrincipal create(Account account) {
        return create(account, null);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
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
        return !"LOCKED".equalsIgnoreCase(status) && !"BLOCKED".equalsIgnoreCase(status);
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return "ACTIVE".equalsIgnoreCase(status);
    }
}
