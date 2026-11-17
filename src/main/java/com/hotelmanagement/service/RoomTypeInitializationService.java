package com.hotelmanagement.service;

import com.hotelmanagement.entity.RoomType;
import com.hotelmanagement.repository.RoomTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomTypeInitializationService {

    private final RoomTypeRepository roomTypeRepository;

    public RoomTypeInitializationService(RoomTypeRepository roomTypeRepository) {
        this.roomTypeRepository = roomTypeRepository;
    }

    @Transactional
    public void initialize() {
        createIfMissing("Normal", "Acomodação padrão do hotel.");
        createIfMissing("Suíte", "Acomodação do tipo suíte.");
    }

    private void createIfMissing(String name, String description) {
        if (roomTypeRepository.existsByNameIgnoreCase(name)) {
            return;
        }

        RoomType roomType = new RoomType();
        roomType.setName(name);
        roomType.setDescription(description);
        roomTypeRepository.save(roomType);
    }
}
