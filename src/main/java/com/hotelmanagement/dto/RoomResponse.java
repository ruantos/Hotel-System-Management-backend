package com.hotelmanagement.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record RoomResponse(
        Long id,
        String name,
        int number,
        RoomTypeResponse type,
        String description,
        int capacity,
        BigDecimal pricePerNight,
        boolean active,
        LocalDateTime createdAt,
        List<AmenityResponse> amenities
) {
}
