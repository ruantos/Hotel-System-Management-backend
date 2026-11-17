package com.hotelmanagement.controller;

import java.net.URI;
import java.util.List;

import com.hotelmanagement.dto.ChangePasswordRequest;
import com.hotelmanagement.dto.RegisterReceptionistRequest;
import com.hotelmanagement.dto.UpdateAccountRequest;
import com.hotelmanagement.dto.UserResponse;
import com.hotelmanagement.service.CurrentUserService;
import com.hotelmanagement.service.UserAccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserAccountService accountService;
    private final CurrentUserService currentUserService;

    public UserController(UserAccountService accountService, CurrentUserService currentUserService) {
        this.accountService = accountService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        return currentUserService.findByEmail(authentication.getName());
    }

    @PutMapping("/me")
    public UserResponse updateMe(Authentication authentication, @Valid @RequestBody UpdateAccountRequest body,
            HttpServletRequest request, HttpServletResponse response) {
        UserResponse result = accountService.updateOwnAccount(authentication.getName(), body);
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        return result;
    }

    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest body,
            HttpServletRequest request, HttpServletResponse response) {
        accountService.changeOwnPassword(authentication.getName(), body);
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public List<UserResponse> findAll(Authentication authentication) {
        return accountService.findAll(authentication.getName());
    }

    @GetMapping("/{id}")
    public UserResponse findById(Authentication authentication, @PathVariable Long id) {
        return accountService.findById(authentication.getName(), id);
    }

    @PostMapping("/receptionists")
    public ResponseEntity<UserResponse> registerReceptionist(Authentication authentication,
            @Valid @RequestBody RegisterReceptionistRequest body) {
        UserResponse result = accountService.registerReceptionist(authentication.getName(), body);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/users/{id}").buildAndExpand(result.id()).toUri();
        return ResponseEntity.created(location).body(result);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable Long id) {
        accountService.delete(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
