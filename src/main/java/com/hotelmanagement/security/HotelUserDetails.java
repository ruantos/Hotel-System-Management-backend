package com.hotelmanagement.security;

import java.io.Serial;
import java.util.List;

import com.hotelmanagement.entity.User;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public class HotelUserDetails extends org.springframework.security.core.userdetails.User {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Long userId;
    private final long sessionVersion;

    public HotelUserDetails(User user) {
        super(user.getEmail(), user.getPasswordHash(), user.isActive(), true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
        this.userId = user.getId();
        this.sessionVersion = user.getSessionVersion();
    }

    public Long getUserId() {
        return userId;
    }

    public long getSessionVersion() {
        return sessionVersion;
    }
}
