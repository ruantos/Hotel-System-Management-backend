package com.hotelmanagement.service;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Comparator;

import com.hotelmanagement.dto.RoomRequest;
import com.hotelmanagement.dto.RoomResponse;
import com.hotelmanagement.dto.RoomTypeResponse;
import com.hotelmanagement.dto.AmenityResponse;
import com.hotelmanagement.repository.AmenityRepository;
import com.hotelmanagement.entity.Room;
import com.hotelmanagement.entity.RoomType;
import com.hotelmanagement.enums.ReservationStatus;
import com.hotelmanagement.exception.ResourceInUseException;
import com.hotelmanagement.exception.ResourceConflictException;
import com.hotelmanagement.exception.ResourceNotFoundException;
import com.hotelmanagement.repository.RoomRepository;
import com.hotelmanagement.repository.RoomTypeRepository;
import com.hotelmanagement.repository.ReservationRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final ReservationRepository reservationRepository;
    private final AmenityRepository amenityRepository;

    public RoomService(RoomRepository roomRepository, RoomTypeRepository roomTypeRepository,
            ReservationRepository reservationRepository, AmenityRepository amenityRepository) {
        this.roomRepository = roomRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.reservationRepository = reservationRepository;
        this.amenityRepository = amenityRepository;
    }

    public List<RoomResponse> findAll() {
        return roomRepository.findAll(Sort.by("number")).stream()
                .map(this::toResponse)
                .toList();
    }

    public RoomResponse findById(Long id) {
        return toResponse(getRoom(id));
    }

    @Transactional
    public RoomResponse create(RoomRequest request) {
        if (roomRepository.existsByNumber(request.number())) {
            throw new ResourceConflictException("Já existe um quarto com esse número.");
        }
        Room room = new Room();
        applyRequest(room, request);
        return toResponse(roomRepository.saveAndFlush(room));
    }

    @Transactional
    public RoomResponse update(Long id, RoomRequest request) {
        Room room = getLockedRoom(id);
        if (roomRepository.existsByNumberAndIdNot(request.number(), id)) {
            throw new ResourceConflictException("Já existe um quarto com esse número.");
        }
        if (reservationRepository.existsByRoom_IdAndStatusInAndGuestCountGreaterThan(id,
                Set.of(ReservationStatus.PENDING, ReservationStatus.APPROVED), request.capacity())) {
            throw new ResourceConflictException("A capacidade informada não atende às reservas existentes.");
        }
        applyRequest(room, request);
        return toResponse(roomRepository.saveAndFlush(room));
    }

    @Transactional
    public void delete(Long id) {
        Room room = getLockedRoom(id);
        if (reservationRepository.existsByRoom_Id(id)) {
            throw new ResourceInUseException("O quarto possui reservas e não pode ser excluído.");
        }
        roomRepository.delete(room);
        roomRepository.flush();
    }

    private Room getRoom(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quarto não encontrado."));
    }

    private Room getLockedRoom(Long id) {
        return roomRepository.findLockedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quarto não encontrado."));
    }

    private void applyRequest(Room room, RoomRequest request) {
        RoomType type = roomTypeRepository.findById(request.typeId())
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de quarto não encontrado."));
        var amenities = amenityRepository.findAllById(request.amenityIds());
        if (amenities.size() != request.amenityIds().size()) {
            throw new ResourceNotFoundException("Uma ou mais características de quarto não foram encontradas.");
        }
        room.setName(request.name());
        room.setNumber(request.number());
        room.setType(type);
        room.setDescription(request.description());
        room.setCapacity(request.capacity());
        room.setPricePerNight(request.pricePerNight());
        room.setActive(request.active());
        room.setAmenities(new HashSet<>(amenities));
    }

    private RoomResponse toResponse(Room room) {
        RoomType type = room.getType();
        RoomTypeResponse typeResponse = new RoomTypeResponse(type.getId(), type.getName(), type.getDescription());
        return new RoomResponse(room.getId(), room.getName(), room.getNumber(), typeResponse,
                room.getDescription(), room.getCapacity(), room.getPricePerNight(),
                room.isActive(), room.getCreatedAt(), room.getAmenities().stream()
                        .map(amenity -> new AmenityResponse(amenity.getId(), amenity.getName(), amenity.getDescription()))
                        .sorted(Comparator.comparing(amenity -> amenity.id())).toList());
    }
}
