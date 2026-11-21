package com.hotelmanagement.service;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

import com.hotelmanagement.exception.RateLimitExceededException;
import com.hotelmanagement.security.PasswordResetRateLimiter;
import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.mail.MailSendException;
import org.springframework.stereotype.Service;

@Service
public class PasswordResetRequestService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetRequestService.class);
    private static final Pattern SMTP_STATUS = Pattern.compile("(?m)^([45]\\d{2})(?:[ -]|$)");

    private final PasswordResetService passwordResetService;
    private final PasswordResetRateLimiter rateLimiter;
    private final TaskExecutor executor;

    public PasswordResetRequestService(PasswordResetService passwordResetService,
            PasswordResetRateLimiter rateLimiter,
            @Qualifier("passwordResetExecutor") TaskExecutor executor) {
        this.passwordResetService = passwordResetService;
        this.rateLimiter = rateLimiter;
        this.executor = executor;
    }

    public void requestReset(String email, String remoteAddress) {
        passwordResetService.requireEnabled();
        rateLimiter.check("forgot", remoteAddress, 20);
        try {
            executor.execute(() -> {
                try {
                    logger.info("Recuperação de senha: processando solicitação em segundo plano.");
                    passwordResetService.sendResetEmail(email);
                } catch (RuntimeException exception) {
                    logDeliveryFailure(exception);
                }
            });
        } catch (TaskRejectedException exception) {
            throw new RateLimitExceededException(60);
        }
    }

    private void logDeliveryFailure(RuntimeException exception) {
        ArrayDeque<Throwable> pending = new ArrayDeque<>();
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<String> types = new LinkedHashSet<>();
        Set<String> smtpStatuses = new LinkedHashSet<>();
        pending.add(exception);
        while (!pending.isEmpty() && visited.size() < 20) {
            Throwable failure = pending.removeFirst();
            if (!visited.add(failure)) {
                continue;
            }
            types.add(failure.getClass().getSimpleName());
            if (failure instanceof MessagingException mailException) {
                String message = mailException.getMessage();
                if (message != null) {
                    SMTP_STATUS.matcher(message).results().limit(10)
                            .forEach(match -> smtpStatuses.add(match.group(1)));
                }
                if (mailException.getNextException() != null) {
                    pending.addLast(mailException.getNextException());
                }
            }
            if (failure instanceof MailSendException sendException) {
                Collections.addAll(pending, sendException.getMessageExceptions());
            }
            if (failure.getCause() != null) {
                pending.addLast(failure.getCause());
            }
        }
        logger.error("Falha na recuperação de senha: tipos={}, codigosSMTP={}. Verifique a conexão, as credenciais e o status transacional do provedor; falhas de banco também aparecem em tipos.",
                types, smtpStatuses);
    }
}
