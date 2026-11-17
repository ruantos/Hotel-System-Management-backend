package com.hotelmanagement.security;

import java.util.Locale;

import com.hotelmanagement.entity.User;
import com.hotelmanagement.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public DatabaseUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {
        User user = userRepository.findByEmailIgnoreCase(email.strip().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas."));
        return new HotelUserDetails(user);
    }
}
