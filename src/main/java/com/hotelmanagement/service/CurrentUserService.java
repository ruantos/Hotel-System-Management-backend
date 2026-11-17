package com.hotelmanagement.service;

import com.hotelmanagement.dto.CustomerProfileResponse;
import com.hotelmanagement.dto.UserResponse;
import com.hotelmanagement.entity.User;
import com.hotelmanagement.exception.ResourceNotFoundException;
import com.hotelmanagement.repository.CustomerProfileRepository;
import com.hotelmanagement.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;
    private final CustomerProfileRepository customerProfileRepository;

    public CurrentUserService(UserRepository userRepository, CustomerProfileRepository customerProfileRepository) {
        this.userRepository = userRepository;
        this.customerProfileRepository = customerProfileRepository;
    }

    @Transactional(readOnly = true)
    public UserResponse findByEmail(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
        return toResponse(user);
    }

    public UserResponse toResponse(User user) {
        CustomerProfileResponse profile = customerProfileRepository.findByUser_Id(user.getId())
                .map(customer -> new CustomerProfileResponse(customer.getId(),
                        customer.getPhone(), customer.getDocument()))
                .orElse(null);
        return new UserResponse(user.getId(), user.getName(), user.getEmail(),
                user.getRole(), user.isActive(), profile);
    }
}
