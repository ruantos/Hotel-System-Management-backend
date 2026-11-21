package com.hotelmanagement.controller;

import com.hotelmanagement.dto.ForgotPasswordRequest;
import com.hotelmanagement.dto.ForgotPasswordResponse;
import com.hotelmanagement.dto.ResetPasswordRequest;
import com.hotelmanagement.service.PasswordResetRequestService;
import com.hotelmanagement.service.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class PasswordResetController {

    private final PasswordResetRequestService requestService;
    private final PasswordResetService passwordResetService;

    public PasswordResetController(PasswordResetRequestService requestService, PasswordResetService passwordResetService) {
        this.requestService = requestService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ForgotPasswordResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest body,
            HttpServletRequest request) {
        requestService.requestReset(body.email(), request.getRemoteAddr());
        return ResponseEntity.accepted().cacheControl(CacheControl.noStore()).body(new ForgotPasswordResponse(
                "Se existir uma conta ativa com este e-mail, você receberá instruções para redefinir sua senha."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest body,
            HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        passwordResetService.resetPassword(body, request.getRemoteAddr());
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }
}
