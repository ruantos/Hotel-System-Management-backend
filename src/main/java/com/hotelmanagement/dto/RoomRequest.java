package com.hotelmanagement.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record RoomRequest(
        @Size(max = 255, message = "O nome deve ter no máximo 255 caracteres.")
        String name,
        @NotNull(message = "O número é obrigatório.")
        @Positive(message = "O número deve ser positivo.")
        Integer number,
        @NotNull(message = "O tipo de quarto é obrigatório.")
        @Positive(message = "O ID do tipo de quarto deve ser positivo.")
        Long typeId,
        @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres.")
        String description,
        @NotNull(message = "A capacidade é obrigatória.")
        @Positive(message = "A capacidade deve ser positiva.")
        Integer capacity,
        @NotNull(message = "O valor da diária é obrigatório.")
        @DecimalMin(value = "0.00", message = "O valor da diária não pode ser negativo.")
        @Digits(integer = 8, fraction = 2, message = "A diária deve ter até 8 dígitos inteiros e 2 decimais.")
        BigDecimal pricePerNight,
        @NotNull(message = "Informe se o quarto está ativo.")
        Boolean active
) {
}
