package com.hotelmanagement.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.hotelmanagement.enums.ReservationStatus;

public record ReservationResponse(Long id, Long customerProfileId, Long roomId,
        LocalDate checkInDate, LocalDate checkOutDate, int guestCount,
        ReservationStatus status, LocalDateTime createdAt, List<Long> additionalServiceIds) {
}
