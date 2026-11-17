package com.hotelmanagement.controller;

import java.net.URI;
import java.util.List;

import com.hotelmanagement.dto.CreateReservationRequest;
import com.hotelmanagement.dto.ReservationResponse;
import com.hotelmanagement.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    public ResponseEntity<ReservationResponse> create(@AuthenticationPrincipal UserDetails user,
            @Valid @RequestBody CreateReservationRequest request) {
        ReservationResponse response = reservationService.create(user.getUsername(), request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public List<ReservationResponse> findAll(@AuthenticationPrincipal UserDetails user) {
        return reservationService.findAll(user.getUsername());
    }

    @GetMapping("/{id}")
    public ReservationResponse findById(@AuthenticationPrincipal UserDetails user, @PathVariable Long id) {
        return reservationService.findById(user.getUsername(), id);
    }

    @PutMapping("/{id}/approve")
    public ReservationResponse approve(@AuthenticationPrincipal UserDetails user, @PathVariable Long id) {
        return reservationService.approve(user.getUsername(), id);
    }

    @PutMapping("/{id}/cancel")
    public ReservationResponse cancel(@AuthenticationPrincipal UserDetails user, @PathVariable Long id) {
        return reservationService.cancel(user.getUsername(), id);
    }
}
