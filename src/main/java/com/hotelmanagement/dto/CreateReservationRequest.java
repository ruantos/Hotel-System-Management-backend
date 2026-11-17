package com.hotelmanagement.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateReservationRequest(
        @NotNull(message = "O quarto é obrigatório.")
        @Positive(message = "O ID do quarto deve ser positivo.")
        Long roomId,
        @NotNull(message = "A data de entrada é obrigatória.")
        LocalDate checkInDate,
        @NotNull(message = "A data de saída é obrigatória.")
        LocalDate checkOutDate,
        @NotNull(message = "A quantidade de hóspedes é obrigatória.")
        @Positive(message = "A quantidade de hóspedes deve ser positiva.")
        Integer guestCount
) {
}
