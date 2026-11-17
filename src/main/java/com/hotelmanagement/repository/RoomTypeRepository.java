package com.hotelmanagement.repository;

import com.hotelmanagement.entity.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomTypeRepository extends JpaRepository<RoomType, Long> {

    boolean existsByNameIgnoreCase(String name);
}
