package com.hotelmanagement.service;

import com.hotelmanagement.exception.RateLimitExceededException;
import com.hotelmanagement.security.PasswordResetRateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;

@Service
public class PasswordResetRequestService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetRequestService.class);

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
                    passwordResetService.sendResetEmail(email);
                } catch (RuntimeException exception) {
                    logger.error("Não foi possível processar um envio de recuperação de senha; verifique o SMTP e o banco.");
                }
            });
        } catch (TaskRejectedException exception) {
            throw new RateLimitExceededException(60);
        }
    }
}
