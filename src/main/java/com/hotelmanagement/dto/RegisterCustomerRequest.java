package com.hotelmanagement.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterCustomerRequest(
        @NotBlank(message = "O nome é obrigatório.")
        @Size(max = 255, message = "O nome deve ter no máximo 255 caracteres.")
        String name,
        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "Informe um e-mail válido.")
        @Size(max = 255, message = "O e-mail deve ter no máximo 255 caracteres.")
        String email,
        @NotBlank(message = "A senha é obrigatória.")
        @Size(min = 8, max = 128, message = "A senha deve ter entre 8 e 128 caracteres.")
        String password,
        @Size(max = 255, message = "O telefone deve ter no máximo 255 caracteres.")
        String phone,
        @Size(max = 255, message = "O documento deve ter no máximo 255 caracteres.")
        String document
) {

    public RegisterCustomerRequest {
        if (email != null) {
            email = email.strip();
        }
    }

    @Override
    public String toString() {
        return "RegisterCustomerRequest[redacted]";
    }
}
