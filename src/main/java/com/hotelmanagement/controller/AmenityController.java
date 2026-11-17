package com.hotelmanagement.controller;

import java.net.URI;
import java.util.List;

import com.hotelmanagement.dto.AmenityRequest;
import com.hotelmanagement.dto.AmenityResponse;
import com.hotelmanagement.service.AmenityService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/amenities")
public class AmenityController {

    private final AmenityService amenityService;

    public AmenityController(AmenityService amenityService) {
        this.amenityService = amenityService;
    }

    @GetMapping
    public List<AmenityResponse> findAll() {
        return amenityService.findAll();
    }

    @GetMapping("/{id}")
    public AmenityResponse findById(@PathVariable Long id) {
        return amenityService.findById(id);
    }

    @PostMapping
    public ResponseEntity<AmenityResponse> create(@Valid @RequestBody AmenityRequest request) {
        AmenityResponse response = amenityService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    public AmenityResponse update(@PathVariable Long id, @Valid @RequestBody AmenityRequest request) {
        return amenityService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        amenityService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
