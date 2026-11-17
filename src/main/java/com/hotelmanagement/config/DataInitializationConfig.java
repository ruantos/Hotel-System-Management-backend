package com.hotelmanagement.config;

import com.hotelmanagement.service.RoomTypeInitializationService;
import com.hotelmanagement.service.ReceptionistInitializationService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializationConfig {

    @Bean
    public ApplicationRunner initializeRoomTypes(RoomTypeInitializationService service) {
        return args -> service.initialize();
    }

    @Bean
    public ApplicationRunner initializeReceptionist(ReceptionistInitializationService service) {
        return args -> service.initialize();
    }
}
