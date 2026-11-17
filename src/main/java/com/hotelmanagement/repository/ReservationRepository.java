package com.hotelmanagement.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.hotelmanagement.entity.Reservation;
import com.hotelmanagement.enums.ReservationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    boolean existsByRoom_IdAndStatusInAndGuestCountGreaterThan(
            Long roomId, Collection<ReservationStatus> statuses, int capacity);

    boolean existsByRoom_Id(Long roomId);

    List<Reservation> findByCustomer_User_IdOrderById(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reservation r where r.id = :id")
    Optional<Reservation> findLockedById(@Param("id") Long id);

    @Query("""
            select count(r) > 0 from Reservation r
            where r.room.id = :roomId and r.status in :statuses
            and r.checkInDate < :checkOut and r.checkOutDate > :checkIn
            and r.id <> :excludedId
            """)
    boolean existsOverlapping(@Param("roomId") Long roomId,
            @Param("checkIn") LocalDate checkIn, @Param("checkOut") LocalDate checkOut,
            @Param("statuses") Collection<ReservationStatus> statuses,
            @Param("excludedId") Long excludedId);
}
