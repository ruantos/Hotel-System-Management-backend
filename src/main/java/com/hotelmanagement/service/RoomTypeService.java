package com.hotelmanagement.service;

import java.util.List;

import com.hotelmanagement.dto.RoomTypeRequest;
import com.hotelmanagement.dto.RoomTypeResponse;
import com.hotelmanagement.entity.RoomType;
import com.hotelmanagement.exception.ResourceInUseException;
import com.hotelmanagement.exception.ResourceNotFoundException;
import com.hotelmanagement.repository.RoomRepository;
import com.hotelmanagement.repository.RoomTypeRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RoomTypeService {

    private final RoomTypeRepository roomTypeRepository;
    private final RoomRepository roomRepository;

    public RoomTypeService(RoomTypeRepository roomTypeRepository, RoomRepository roomRepository) {
        this.roomTypeRepository = roomTypeRepository;
        this.roomRepository = roomRepository;
    }

    public List<RoomTypeResponse> findAll() {
        return roomTypeRepository.findAll(Sort.by("id")).stream()
                .map(this::toResponse)
                .toList();
    }

    public RoomTypeResponse findById(Long id) {
        return toResponse(getRoomType(id));
    }

    @Transactional
    public RoomTypeResponse create(RoomTypeRequest request) {
        RoomType roomType = new RoomType();
        applyRequest(roomType, request);
        return toResponse(roomTypeRepository.save(roomType));
    }

    @Transactional
    public RoomTypeResponse update(Long id, RoomTypeRequest request) {
        RoomType roomType = getRoomType(id);
        applyRequest(roomType, request);
        return toResponse(roomTypeRepository.save(roomType));
    }

    @Transactional
    public void delete(Long id) {
        RoomType roomType = getRoomType(id);
        if (roomRepository.existsByType_Id(id)) {
            throw new ResourceInUseException("O tipo de quarto está vinculado a quartos e não pode ser excluído.");
        }
        roomTypeRepository.delete(roomType);
        roomTypeRepository.flush();
    }

    private RoomType getRoomType(Long id) {
        return roomTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de quarto não encontrado."));
    }

    private void applyRequest(RoomType roomType, RoomTypeRequest request) {
        roomType.setName(request.name().strip());
        roomType.setDescription(request.description());
    }

    private RoomTypeResponse toResponse(RoomType roomType) {
        return new RoomTypeResponse(roomType.getId(), roomType.getName(), roomType.getDescription());
    }
}
