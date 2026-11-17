package com.hotelmanagement.service;

import java.util.Locale;

import com.hotelmanagement.dto.CustomerProfileResponse;
import com.hotelmanagement.dto.RegisterCustomerRequest;
import com.hotelmanagement.dto.UserResponse;
import com.hotelmanagement.entity.CustomerProfile;
import com.hotelmanagement.entity.User;
import com.hotelmanagement.enums.UserRole;
import com.hotelmanagement.exception.ResourceConflictException;
import com.hotelmanagement.repository.CustomerProfileRepository;
import com.hotelmanagement.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerRegistrationService {

    private final UserRepository userRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final PasswordEncoder passwordEncoder;

    public CustomerRegistrationService(UserRepository userRepository,
            CustomerProfileRepository customerProfileRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse register(RegisterCustomerRequest request) {
        String email = request.email().strip().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResourceConflictException("Já existe uma conta com esse e-mail.");
        }

        User user = new User();
        user.setName(request.name().strip());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(UserRole.CLIENT);
        user.setActive(true);
        userRepository.saveAndFlush(user);

        CustomerProfile profile = new CustomerProfile();
        profile.setUser(user);
        profile.setPhone(request.phone());
        profile.setDocument(request.document());
        customerProfileRepository.saveAndFlush(profile);

        CustomerProfileResponse profileResponse = new CustomerProfileResponse(
                profile.getId(), profile.getPhone(), profile.getDocument());
        return new UserResponse(user.getId(), user.getName(), user.getEmail(),
                user.getRole(), user.isActive(), profileResponse);
    }
}
