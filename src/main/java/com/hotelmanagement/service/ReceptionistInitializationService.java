package com.hotelmanagement.service;

import java.util.Locale;

import com.hotelmanagement.dto.RegisterReceptionistRequest;
import com.hotelmanagement.entity.User;
import com.hotelmanagement.enums.UserRole;
import com.hotelmanagement.repository.UserRepository;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReceptionistInitializationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Validator validator;
    private final boolean enabled;
    private final RegisterReceptionistRequest account;

    public ReceptionistInitializationService(UserRepository userRepository, PasswordEncoder passwordEncoder,
            Validator validator, @Value("${app.bootstrap.receptionist.enabled}") boolean enabled,
            @Value("${app.bootstrap.receptionist.name}") String name,
            @Value("${app.bootstrap.receptionist.email}") String email,
            @Value("${app.bootstrap.receptionist.password}") String password) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.validator = validator;
        this.enabled = enabled;
        this.account = new RegisterReceptionistRequest(name, email, password);
    }

    @Transactional
    public void initialize() {
        if (!enabled || userRepository.existsByRole(UserRole.RECEPTIONIST)) {
            return;
        }
        if (!validator.validate(account).isEmpty()) {
            throw new IllegalStateException("Configure nome, e-mail válido e senha de 8 a 128 caracteres para o recepcionista inicial.");
        }
        String email = account.email().strip().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalStateException("O e-mail configurado para o recepcionista inicial já pertence a outra conta.");
        }
        User user = new User();
        user.setName(account.name().strip());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(account.password()));
        user.setRole(UserRole.RECEPTIONIST);
        user.setActive(true);
        userRepository.saveAndFlush(user);
    }
}
