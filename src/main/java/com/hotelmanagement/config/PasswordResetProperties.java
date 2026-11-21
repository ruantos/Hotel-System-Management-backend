package com.hotelmanagement.config;

import java.net.URI;
import java.time.Duration;
import java.util.Set;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.password-reset")
public record PasswordResetProperties(
        boolean enabled,
        String frontendUrl,
        @NotBlank @Email String from,
        Duration tokenTtl,
        Duration resendCooldown
) {

    public PasswordResetProperties {
        if (tokenTtl == null || tokenTtl.compareTo(Duration.ofMinutes(1)) < 0
                || tokenTtl.compareTo(Duration.ofHours(1)) > 0) {
            throw new IllegalArgumentException("A validade do token deve estar entre 1 minuto e 1 hora.");
        }
        if (resendCooldown == null || resendCooldown.compareTo(Duration.ofSeconds(1)) < 0
                || resendCooldown.compareTo(tokenTtl) > 0) {
            throw new IllegalArgumentException("O intervalo de reenvio deve ser positivo e não exceder a validade do token.");
        }
        URI uri = frontendUrl == null ? URI.create("") : URI.create(frontendUrl);
        boolean localHttp = "http".equals(uri.getScheme())
                && Set.of("localhost", "127.0.0.1", "[::1]").contains(uri.getHost() == null ? "" : uri.getHost());
        if (uri.getHost() == null || !("https".equals(uri.getScheme()) || localHttp)
                || uri.getRawUserInfo() != null || uri.getRawQuery() != null || uri.getRawFragment() != null) {
            throw new IllegalArgumentException("Configure uma URL HTTPS de redefinição, sem credenciais, query ou fragmento; HTTP somente em localhost.");
        }
    }
}
