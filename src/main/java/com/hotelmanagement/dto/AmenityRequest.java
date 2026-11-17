package com.hotelmanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AmenityRequest(
        @NotBlank(message = "O nome é obrigatório.")
        @Size(max = 255, message = "O nome deve ter no máximo 255 caracteres.") String name,
        @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres.") String description
) {
}
