package com.hotelmanagement.repository;

import java.util.Optional;
import java.util.List;

import com.hotelmanagement.entity.User;
import com.hotelmanagement.enums.UserRole;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmailIgnoreCase(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    boolean existsByRole(UserRole role);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<User> findByRoleOrderById(UserRole role);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findLockedById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where lower(u.email) = lower(:email)")
    Optional<User> findLockedByEmail(@Param("email") String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.passwordResetTokenHash = :tokenHash")
    Optional<User> findLockedByPasswordResetTokenHash(@Param("tokenHash") String tokenHash);
}
