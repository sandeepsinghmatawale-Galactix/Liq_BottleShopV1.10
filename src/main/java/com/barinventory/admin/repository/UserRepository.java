package com.barinventory.admin.repository;

import com.barinventory.admin.entity.Role;
import com.barinventory.admin.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // ✅ Find user by email
    Optional<User> findByEmail(String email);

    // ✅ Check if email already exists
    boolean existsByEmail(String email);

    // ✅ Find users by global role
    List<User> findByRole(Role role);

    // ✅ Find all active users
    List<User> findByActiveTrue();

    // =========================
    // Bar access-related queries
    // =========================

    // ✅ Get all users assigned to a specific bar
    @Query("SELECT DISTINCT u FROM User u JOIN u.barAccesses ba " +
           "WHERE ba.bar.barId = :barId AND ba.active = true")
    List<User> findByBarId(@Param("barId") Long barId);

    // ✅ Get all users assigned to a specific bar with a specific role
    @Query("SELECT DISTINCT u FROM User u JOIN u.barAccesses ba " +
           "WHERE ba.bar.barId = :barId AND u.role = :role AND ba.active = true")
    List<User> findByBarIdAndRole(@Param("barId") Long barId, @Param("role") Role role);

    

    // ✅ Check if user has active access to a bar
    @Query("SELECT COUNT(ba) > 0 FROM UserBarAccess ba " +
           "WHERE ba.user.id = :userId AND ba.bar.barId = :barId AND ba.active = true")
    boolean hasAccessToBar(@Param("userId") Long userId, @Param("barId") Long barId);

    // ✅ Get all bar IDs that a user has access to
    @Query("SELECT DISTINCT ba.bar.barId FROM UserBarAccess ba " +
           "WHERE ba.user.id = :userId AND ba.active = true")
    List<Long> findBarIdsByUserId(@Param("userId") Long userId);
    
    
 // Count users with BAR_STAFF role assigned to a specific bar
    @Query("SELECT COUNT(u) FROM User u JOIN u.barAccesses ba " +
           "WHERE ba.bar.barId = :barId AND u.role = :role AND ba.active = true")
    long countUsersByBarAndRole(@Param("barId") Long barId, @Param("role") Role role);
}