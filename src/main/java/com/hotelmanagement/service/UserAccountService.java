package com.hotelmanagement.service;

import java.util.List;
import java.util.Locale;

import com.hotelmanagement.dto.ChangePasswordRequest;
import com.hotelmanagement.dto.RegisterReceptionistRequest;
import com.hotelmanagement.dto.UpdateAccountRequest;
import com.hotelmanagement.dto.UserResponse;
import com.hotelmanagement.entity.User;
import com.hotelmanagement.enums.UserRole;
import com.hotelmanagement.exception.InvalidRequestException;
import com.hotelmanagement.exception.ResourceConflictException;
import com.hotelmanagement.exception.ResourceInUseException;
import com.hotelmanagement.exception.ResourceNotFoundException;
import com.hotelmanagement.repository.CustomerProfileRepository;
import com.hotelmanagement.repository.ReservationRepository;
import com.hotelmanagement.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserAccountService {

    private final UserRepository userRepository;
    private final CustomerProfileRepository profileRepository;
    private final ReservationRepository reservationRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserService currentUserService;

    public UserAccountService(UserRepository userRepository, CustomerProfileRepository profileRepository,
            ReservationRepository reservationRepository, PasswordEncoder passwordEncoder,
            CurrentUserService currentUserService) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.reservationRepository = reservationRepository;
        this.passwordEncoder = passwordEncoder;
        this.currentUserService = currentUserService;
    }

    public List<UserResponse> findAll(String email) {
        requireReceptionist(email);
        return userRepository.findAll(Sort.by("id")).stream().map(currentUserService::toResponse).toList();
    }

    public UserResponse findById(String email, Long id) {
        requireReceptionist(email);
        return currentUserService.toResponse(getUser(id));
    }

    @Transactional
    public UserResponse registerReceptionist(String actorEmail, RegisterReceptionistRequest request) {
        requireReceptionist(actorEmail);
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResourceConflictException("Já existe uma conta com esse e-mail.");
        }
        User user = new User();
        user.setName(request.name().strip());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(UserRole.RECEPTIONIST);
        user.setActive(true);
        return currentUserService.toResponse(userRepository.saveAndFlush(user));
    }

    @Transactional
    public UserResponse updateOwnAccount(String email, UpdateAccountRequest request) {
        User user = getLockedActiveUser(email);
        String normalizedEmail = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(normalizedEmail, user.getId())) {
            throw new ResourceConflictException("Já existe uma conta com esse e-mail.");
        }
        var profile = profileRepository.findByUser_Id(user.getId());
        if (profile.isEmpty() && (request.phone() != null || request.document() != null)) {
            throw new InvalidRequestException("Telefone e documento pertencem ao perfil de cliente.");
        }
        user.setName(request.name().strip());
        user.setEmail(normalizedEmail);
        user.invalidateSessions();
        profile.ifPresent(customer -> {
            customer.setPhone(request.phone());
            customer.setDocument(request.document());
            profileRepository.save(customer);
        });
        userRepository.saveAndFlush(user);
        return currentUserService.toResponse(user);
    }

    @Transactional
    public void changeOwnPassword(String email, ChangePasswordRequest request) {
        User user = getLockedActiveUser(email);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new InvalidRequestException("A senha atual está incorreta.");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.invalidateSessions();
        userRepository.saveAndFlush(user);
    }

    @Transactional
    public void delete(String email, Long id) {
        var receptionists = userRepository.findByRoleOrderById(UserRole.RECEPTIONIST);
        User actor = requireReceptionist(email);
        if (actor.getId().equals(id)) {
            throw new ResourceConflictException("Não é permitido excluir a própria conta de recepcionista.");
        }
        User user = userRepository.findLockedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
        if (user.getRole() == UserRole.RECEPTIONIST && user.isActive()
                && receptionists.stream().filter(User::isActive).count() <= 1) {
            throw new ResourceConflictException("O último recepcionista ativo não pode ser excluído.");
        }
        if (reservationRepository.existsByCustomer_User_Id(id)) {
            throw new ResourceInUseException("O usuário possui reservas e não pode ser excluído.");
        }
        profileRepository.findByUser_Id(id).ifPresent(profileRepository::delete);
        profileRepository.flush();
        userRepository.delete(user);
        userRepository.flush();
    }

    private User requireReceptionist(String email) {
        User user = getActiveUser(email);
        if (user.getRole() != UserRole.RECEPTIONIST) {
            throw new AccessDeniedException("Operação exclusiva de recepcionistas.");
        }
        return user;
    }

    private User getActiveUser(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new AccessDeniedException("Conta indisponível."));
        if (!user.isActive()) {
            throw new AccessDeniedException("Conta indisponível.");
        }
        return user;
    }

    private User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
    }

    private User getLockedActiveUser(String email) {
        User user = userRepository.findLockedByEmail(email)
                .orElseThrow(() -> new AccessDeniedException("Conta indisponível."));
        if (!user.isActive()) {
            throw new AccessDeniedException("Conta indisponível.");
        }
        return user;
    }

    private String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
