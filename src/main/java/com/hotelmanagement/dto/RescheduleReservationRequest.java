package com.hotelmanagement.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record RescheduleReservationRequest(
        @NotNull(message = "A data de entrada é obrigatória.") LocalDate checkInDate,
        @NotNull(message = "A data de saída é obrigatória.") LocalDate checkOutDate
) {
}
