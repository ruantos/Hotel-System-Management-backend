package com.hotelmanagement.dto;

import java.time.LocalDate;

public record RoomAvailabilityResponse(Long roomId, LocalDate checkInDate,
        LocalDate checkOutDate, boolean available) {
}
