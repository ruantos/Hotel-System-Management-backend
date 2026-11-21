package com.hotelmanagement.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

import com.hotelmanagement.config.PasswordResetProperties;
import com.hotelmanagement.dto.ResetPasswordRequest;
import com.hotelmanagement.entity.User;
import com.hotelmanagement.exception.InvalidRequestException;
import com.hotelmanagement.exception.ServiceUnavailableException;
import com.hotelmanagement.repository.UserRepository;
import com.hotelmanagement.security.PasswordResetRateLimiter;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final PasswordResetProperties properties;
    private final PasswordResetRateLimiter rateLimiter;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetService(UserRepository userRepository, PasswordEncoder passwordEncoder,
            JavaMailSender mailSender, PasswordResetProperties properties,
            PasswordResetRateLimiter rateLimiter, Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
        this.properties = properties;
        this.rateLimiter = rateLimiter;
        this.clock = clock;
    }

    public void requireEnabled() {
        if (!properties.enabled()) {
            throw new ServiceUnavailableException("A recuperação de senha está temporariamente indisponível.");
        }
    }

    @Transactional
    public void sendResetEmail(String email) {
        User user = userRepository.findLockedByEmail(email.strip().toLowerCase(Locale.ROOT))
                .filter(User::isActive).orElse(null);
        if (user == null) {
            return;
        }
        Instant now = clock.instant();
        if (user.getPasswordResetRequestedAt() != null
                && now.isBefore(user.getPasswordResetRequestedAt().plus(properties.resendCooldown()))) {
            return;
        }
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        user.beginPasswordReset(hashToken(token), now.plus(properties.tokenTtl()), now);
        userRepository.saveAndFlush(user);

        String link = UriComponentsBuilder.fromUriString(properties.frontendUrl())
                .queryParam("token", token).build().toUriString();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.from());
        message.setTo(user.getEmail());
        message.setSubject("Recuperação de senha — Sistema Hoteleiro");
        message.setText("""
                Recebemos uma solicitação para redefinir sua senha.

                Acesse o link abaixo para escolher uma nova senha:
                %s

                Este link é de uso único e expira em %d minutos.
                Se você não solicitou a alteração, ignore esta mensagem. Sua senha permanece a mesma.
                """.formatted(link, properties.tokenTtl().toMinutes()));
        mailSender.send(message);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request, String remoteAddress) {
        requireEnabled();
        rateLimiter.check("reset", remoteAddress, 30);
        User user = userRepository.findLockedByPasswordResetTokenHash(hashToken(request.token()))
                .filter(User::isActive)
                .filter(account -> account.getPasswordResetExpiresAt() != null
                        && clock.instant().isBefore(account.getPasswordResetExpiresAt()))
                .orElseThrow(() -> new InvalidRequestException("O link de recuperação é inválido ou expirou. Solicite um novo link."));
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.invalidateSessions();
        userRepository.saveAndFlush(user);
    }

    private String hashToken(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível.", exception);
        }
    }
}
