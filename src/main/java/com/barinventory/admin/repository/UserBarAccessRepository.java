package com.barinventory.admin.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.barinventory.admin.entity.Role;
import com.barinventory.admin.entity.User;
import com.barinventory.admin.entity.UserBarAccess;

@Repository
public interface UserBarAccessRepository extends JpaRepository<UserBarAccess, Long> {

    long countByBar_BarIdAndUser_Role(Long barId, Role role);

    List<UserBarAccess> findByBar_BarId(Long barId);

    List<UserBarAccess> findByUser_Id(Long userId);

    List<UserBarAccess> findByUser_IdAndActiveTrue(Long userId);

    List<UserBarAccess> findByBar_BarIdAndActiveTrue(Long barId);

    List<UserBarAccess> findByUser(User user);

    List<UserBarAccess> findByUserId(Long userId);

    long countByBar_BarIdAndBarRole(Long barId, Role barRole);

    // 🔥 CRITICAL METHODS
    Optional<UserBarAccess> findByUser_IdAndBar_BarId(Long userId, Long barId);

    Optional<UserBarAccess> findByUser_IdAndBar_BarIdAndActiveTrue(Long userId, Long barId);
    
    Optional<UserBarAccess> findByUser_IdAndBar_BarIdAndActiveTrueAndBar_ActiveTrue(
    	    Long userId, Long barId
    	);
}