package com.hotelmanagement.controller;

import java.time.LocalDate;

import com.hotelmanagement.dto.RoomAvailabilityResponse;
import com.hotelmanagement.service.ReservationService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms")
public class RoomAvailabilityController {

    private final ReservationService reservationService;

    public RoomAvailabilityController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping("/{id}/availability")
    public RoomAvailabilityResponse availability(@PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOutDate) {
        return reservationService.availability(id, checkInDate, checkOutDate);
    }
}
