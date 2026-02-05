package com.example.ForDay.global.oauth;

import com.example.ForDay.domain.user.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;

public class CustomUserDetails implements UserDetails {

    private final String userId;
    private final String socialId;
    private final String role;
    private final User user;

    public CustomUserDetails(User user) {
        this.userId = user.getId();
        this.socialId = user.getSocialId();
        this.role = user.getRole().name();
        this.user = user;
    }
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {

        Collection<GrantedAuthority> authorities = new ArrayList<>();

        authorities.add(
                new SimpleGrantedAuthority("ROLE_" + role)
        );

        return authorities;
    }

    public User getUser() {
        return user;
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return socialId;
    }

    public String getUserId() {
        return userId;
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}
