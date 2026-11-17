package com.hotelmanagement.dto;

import com.hotelmanagement.enums.UserRole;

public record UserResponse(
        Long id,
        String name,
        String email,
        UserRole role,
        boolean active,
        CustomerProfileResponse customerProfile
) {
}
