package com.hotelmanagement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "O token é obrigatório.")
        @Pattern(regexp = "[A-Za-z0-9_-]{43}", message = "O link de recuperação é inválido ou expirou.")
        @Schema(accessMode = Schema.AccessMode.WRITE_ONLY) String token,
        @NotBlank(message = "A nova senha é obrigatória.")
        @Size(min = 8, max = 128, message = "A senha deve ter entre 8 e 128 caracteres.")
        @Schema(format = "password", accessMode = Schema.AccessMode.WRITE_ONLY) String newPassword
) {

    @Override
    public String toString() {
        return "ResetPasswordRequest[redacted]";
    }
}
