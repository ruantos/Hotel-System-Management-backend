package com.hotelmanagement.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.hotelmanagement.dto.CreateReservationRequest;
import com.hotelmanagement.dto.ReservationResponse;
import com.hotelmanagement.dto.RoomAvailabilityResponse;
import com.hotelmanagement.entity.AdditionalService;
import com.hotelmanagement.entity.CustomerProfile;
import com.hotelmanagement.entity.Reservation;
import com.hotelmanagement.entity.Room;
import com.hotelmanagement.entity.User;
import com.hotelmanagement.enums.ReservationStatus;
import com.hotelmanagement.enums.UserRole;
import com.hotelmanagement.exception.InvalidRequestException;
import com.hotelmanagement.exception.ResourceConflictException;
import com.hotelmanagement.exception.ResourceNotFoundException;
import com.hotelmanagement.repository.AdditionalServiceRepository;
import com.hotelmanagement.repository.CustomerProfileRepository;
import com.hotelmanagement.repository.ReservationRepository;
import com.hotelmanagement.repository.RoomRepository;
import com.hotelmanagement.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReservationService {

    private static final Set<ReservationStatus> BLOCKING_STATUSES =
            Set.of(ReservationStatus.PENDING, ReservationStatus.APPROVED);

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final AdditionalServiceRepository additionalServiceRepository;
    private final Clock clock;

    public ReservationService(ReservationRepository reservationRepository, RoomRepository roomRepository,
            UserRepository userRepository, CustomerProfileRepository customerProfileRepository,
            AdditionalServiceRepository additionalServiceRepository, Clock clock) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.additionalServiceRepository = additionalServiceRepository;
        this.clock = clock;
    }

    @Transactional
    public ReservationResponse create(String email, CreateReservationRequest request) {
        User user = getActiveUser(email);
        requireRole(user, UserRole.CLIENT);
        validateDates(request.checkInDate(), request.checkOutDate());
        CustomerProfile customer = customerProfileRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new ResourceConflictException("A conta não possui um perfil de cliente."));
        Room room = getLockedRoom(request.roomId());
        validateRoom(room, request.guestCount());
        requireAvailability(room.getId(), request.checkInDate(), request.checkOutDate(), 0L);

        Reservation reservation = new Reservation();
        reservation.setCustomer(customer);
        reservation.setRoom(room);
        reservation.setCheckInDate(request.checkInDate());
        reservation.setCheckOutDate(request.checkOutDate());
        reservation.setGuestCount(request.guestCount());
        reservation.setStatus(ReservationStatus.PENDING);
        reservation.setCreatedAt(LocalDateTime.now(clock));
        reservation.setAdditionalServices(getActiveServices(request.additionalServiceIds()));
        return toResponse(reservationRepository.saveAndFlush(reservation));
    }

    public List<ReservationResponse> findAll(String email) {
        User user = getActiveUser(email);
        List<Reservation> reservations;
        if (user.getRole() == UserRole.RECEPTIONIST) {
            reservations = reservationRepository.findAll(Sort.by("id"));
        } else {
            requireRole(user, UserRole.CLIENT);
            reservations = reservationRepository.findByCustomer_User_IdOrderById(user.getId());
        }
        return reservations.stream().map(this::toResponse).toList();
    }

    public ReservationResponse findById(String email, Long id) {
        User user = getActiveUser(email);
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva não encontrada."));
        requireAccess(user, reservation);
        return toResponse(reservation);
    }

    @Transactional
    public ReservationResponse approve(String email, Long id) {
        User user = getActiveUser(email);
        requireRole(user, UserRole.RECEPTIONIST);
        Reservation reservation = getLockedReservation(id);
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new ResourceConflictException("Somente reservas pendentes podem ser aprovadas.");
        }
        Room room = getLockedRoom(reservation.getRoom().getId());
        if (reservation.getCheckInDate().isBefore(LocalDate.now(clock))) {
            throw new ResourceConflictException("A data de entrada da reserva já passou.");
        }
        validateRoom(room, reservation.getGuestCount());
        requireAvailability(room.getId(), reservation.getCheckInDate(), reservation.getCheckOutDate(), id);
        reservation.setStatus(ReservationStatus.APPROVED);
        return toResponse(reservationRepository.saveAndFlush(reservation));
    }

    @Transactional
    public ReservationResponse cancel(String email, Long id) {
        User user = getActiveUser(email);
        requireRole(user, UserRole.CLIENT);
        Reservation reservation = getLockedReservation(id);
        requireAccess(user, reservation);
        getLockedRoom(reservation.getRoom().getId());
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new ResourceConflictException("A reserva já está cancelada.");
        }
        if (!LocalDate.now(clock).isBefore(reservation.getCheckInDate())) {
            throw new ResourceConflictException("O cancelamento é permitido somente até o dia anterior à entrada.");
        }
        reservation.setStatus(ReservationStatus.CANCELLED);
        return toResponse(reservationRepository.saveAndFlush(reservation));
    }

    public RoomAvailabilityResponse availability(Long roomId, LocalDate checkIn, LocalDate checkOut) {
        validateDates(checkIn, checkOut);
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Quarto não encontrado."));
        boolean available = room.isActive() && !reservationRepository.existsOverlapping(
                roomId, checkIn, checkOut, BLOCKING_STATUSES, 0L);
        return new RoomAvailabilityResponse(roomId, checkIn, checkOut, available);
    }

    private User getActiveUser(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new AccessDeniedException("Conta indisponível."));
        if (!user.isActive()) {
            throw new AccessDeniedException("Conta indisponível.");
        }
        return user;
    }

    private void requireRole(User user, UserRole role) {
        if (user.getRole() != role) {
            throw new AccessDeniedException("Perfil sem permissão para esta operação.");
        }
    }

    private void requireAccess(User user, Reservation reservation) {
        if (user.getRole() != UserRole.RECEPTIONIST
                && !reservation.getCustomer().getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Reserva não encontrada.");
        }
    }

    private Reservation getLockedReservation(Long id) {
        return reservationRepository.findLockedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva não encontrada."));
    }

    private Room getLockedRoom(Long id) {
        return roomRepository.findLockedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quarto não encontrado."));
    }

    private void validateDates(LocalDate checkIn, LocalDate checkOut) {
        if (!checkOut.isAfter(checkIn)) {
            throw new InvalidRequestException("A saída deve ocorrer depois da entrada.");
        }
        if (checkIn.isBefore(LocalDate.now(clock))) {
            throw new InvalidRequestException("A entrada não pode ocorrer em uma data passada.");
        }
    }

    private void validateRoom(Room room, int guestCount) {
        if (!room.isActive()) {
            throw new ResourceConflictException("O quarto está inativo.");
        }
        if (guestCount > room.getCapacity()) {
            throw new InvalidRequestException("A quantidade de hóspedes excede a capacidade do quarto.");
        }
    }

    private void requireAvailability(Long roomId, LocalDate checkIn, LocalDate checkOut, Long excludedId) {
        if (reservationRepository.existsOverlapping(roomId, checkIn, checkOut, BLOCKING_STATUSES, excludedId)) {
            throw new ResourceConflictException("O quarto não está disponível no período informado.");
        }
    }

    private Set<AdditionalService> getActiveServices(Set<Long> ids) {
        List<AdditionalService> services = additionalServiceRepository.findAllById(ids);
        if (services.size() != ids.size()) {
            throw new ResourceNotFoundException("Um ou mais serviços adicionais não foram encontrados.");
        }
        if (services.stream().anyMatch(service -> !service.isActive())) {
            throw new ResourceConflictException("Serviços inativos não podem ser adicionados à reserva.");
        }
        return new HashSet<>(services);
    }

    private ReservationResponse toResponse(Reservation reservation) {
        List<Long> serviceIds = reservation.getAdditionalServices().stream()
                .map(AdditionalService::getId).sorted().toList();
        return new ReservationResponse(reservation.getId(), reservation.getCustomer().getId(),
                reservation.getRoom().getId(), reservation.getCheckInDate(), reservation.getCheckOutDate(),
                reservation.getGuestCount(), reservation.getStatus(), reservation.getCreatedAt(), serviceIds);
    }
}
