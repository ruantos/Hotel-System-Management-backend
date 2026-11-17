package com.hotelmanagement.service;

import java.util.List;

import com.hotelmanagement.dto.AmenityRequest;
import com.hotelmanagement.dto.AmenityResponse;
import com.hotelmanagement.entity.Amenity;
import com.hotelmanagement.exception.ResourceInUseException;
import com.hotelmanagement.exception.ResourceNotFoundException;
import com.hotelmanagement.repository.AmenityRepository;
import com.hotelmanagement.repository.RoomRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AmenityService {

    private final AmenityRepository amenityRepository;
    private final RoomRepository roomRepository;

    public AmenityService(AmenityRepository amenityRepository, RoomRepository roomRepository) {
        this.amenityRepository = amenityRepository;
        this.roomRepository = roomRepository;
    }

    public List<AmenityResponse> findAll() {
        return amenityRepository.findAll(Sort.by("id")).stream().map(this::toResponse).toList();
    }

    public AmenityResponse findById(Long id) {
        return toResponse(getAmenity(id));
    }

    @Transactional
    public AmenityResponse create(AmenityRequest request) {
        Amenity amenity = new Amenity();
        apply(amenity, request);
        return toResponse(amenityRepository.saveAndFlush(amenity));
    }

    @Transactional
    public AmenityResponse update(Long id, AmenityRequest request) {
        Amenity amenity = getAmenity(id);
        apply(amenity, request);
        return toResponse(amenityRepository.saveAndFlush(amenity));
    }

    @Transactional
    public void delete(Long id) {
        Amenity amenity = getAmenity(id);
        if (roomRepository.existsByAmenities_Id(id)) {
            throw new ResourceInUseException("A característica está associada a quartos e não pode ser excluída.");
        }
        amenityRepository.delete(amenity);
        amenityRepository.flush();
    }

    private Amenity getAmenity(Long id) {
        return amenityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Característica de quarto não encontrada."));
    }

    private void apply(Amenity amenity, AmenityRequest request) {
        amenity.setName(request.name().strip());
        amenity.setDescription(request.description());
    }

    private AmenityResponse toResponse(Amenity amenity) {
        return new AmenityResponse(amenity.getId(), amenity.getName(), amenity.getDescription());
    }
}
