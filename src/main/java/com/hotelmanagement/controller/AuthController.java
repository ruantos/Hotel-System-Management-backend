package com.hotelmanagement.controller;

import com.hotelmanagement.dto.RegisterCustomerRequest;
import com.hotelmanagement.dto.CsrfResponse;
import com.hotelmanagement.dto.UserResponse;
import com.hotelmanagement.service.CustomerRegistrationService;
import com.hotelmanagement.service.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final CustomerRegistrationService customerRegistrationService;
    private final CurrentUserService currentUserService;

    public AuthController(CustomerRegistrationService customerRegistrationService, CurrentUserService currentUserService) {
        this.customerRegistrationService = customerRegistrationService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken token) {
        return new CsrfResponse(token.getHeaderName(), token.getToken());
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal UserDetails user) {
        return currentUserService.findByEmail(user.getUsername());
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterCustomerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerRegistrationService.register(request));
    }
}
