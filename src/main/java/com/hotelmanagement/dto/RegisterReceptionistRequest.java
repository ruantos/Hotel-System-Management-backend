package com.hotelmanagement.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

public record RegisterReceptionistRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 128)
        @Schema(format = "password", accessMode = Schema.AccessMode.WRITE_ONLY) String password
) {

    public RegisterReceptionistRequest {
        if (email != null) {
            email = email.strip();
        }
    }

    @Override
    public String toString() {
        return "RegisterReceptionistRequest[redacted]";
    }
}
