package com.hotelmanagement.repository;

import java.util.Optional;

import com.hotelmanagement.entity.Room;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomRepository extends JpaRepository<Room, Long> {

    boolean existsByType_Id(Long typeId);

    boolean existsByAmenities_Id(Long amenityId);

    boolean existsByNumber(int number);

    boolean existsByNumberAndIdNot(int number, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Room r where r.id = :id")
    Optional<Room> findLockedById(@Param("id") Long id);
}
