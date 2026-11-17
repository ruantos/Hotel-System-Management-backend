package com.hotelmanagement.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateAccountRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Email @Size(max = 255) String email,
        @Size(max = 255) String phone,
        @Size(max = 255) String document
) {

    public UpdateAccountRequest {
        if (email != null) {
            email = email.strip();
        }
    }
}
