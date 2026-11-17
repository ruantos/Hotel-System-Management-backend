package com.hotelmanagement.security;

import java.io.IOException;

import com.hotelmanagement.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.filter.OncePerRequestFilter;

public class SessionValidationFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;
    private final SecurityErrorHandler errors;

    public SessionValidationFilter(UserRepository userRepository, SecurityErrorHandler errors) {
        this.userRepository = userRepository;
        this.errors = errors;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof HotelUserDetails principal) {
            boolean valid = userRepository.findById(principal.getUserId())
                    .filter(user -> user.isActive()
                            && user.getSessionVersion() == principal.getSessionVersion()
                            && user.getEmail().equals(principal.getUsername())
                            && principal.getAuthorities().contains(
                                    new SimpleGrantedAuthority("ROLE_" + user.getRole().name())))
                    .isPresent();
            if (!valid) {
                new SecurityContextLogoutHandler().logout(request, response, authentication);
                errors.commence(request, response, new BadCredentialsException("Sessão inválida."));
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
