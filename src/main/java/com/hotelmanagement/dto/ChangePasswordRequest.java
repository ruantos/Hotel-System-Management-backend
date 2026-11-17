package com.hotelmanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

public record ChangePasswordRequest(
        @NotBlank @Size(max = 128)
        @Schema(format = "password", accessMode = Schema.AccessMode.WRITE_ONLY) String currentPassword,
        @NotBlank @Size(min = 8, max = 128)
        @Schema(format = "password", accessMode = Schema.AccessMode.WRITE_ONLY) String newPassword
) {

    @Override
    public String toString() {
        return "ChangePasswordRequest[redacted]";
    }
}
