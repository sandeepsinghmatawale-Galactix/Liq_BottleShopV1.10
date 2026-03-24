package com.barinventory.admin.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.barinventory.admin.entity.Role;
import com.barinventory.admin.entity.UserBarAccess;

@Repository
public interface UserBarAccessRepository extends JpaRepository<UserBarAccess, Long> {

	// ✅ Count users in a bar by GLOBAL role (ADMIN / STAFF)
	long countByBar_BarIdAndUser_Role(Long barId, Role role);

	// ✅ Get all access entries for a bar
	List<UserBarAccess> findByBar_BarId(Long barId);

	// ✅ Get all bars of a user
	List<UserBarAccess> findByUser_Id(Long userId);

	// ✅ Fetch all active UserBarAccess records for a given user ID
	List<UserBarAccess> findByUser_IdAndActiveTrue(Long userId);

	// ✅ Fetch all active UserBarAccess records for a given bar ID
	List<UserBarAccess> findByBar_BarIdAndActiveTrue(Long barId);

	// ✅ Count BAR_STAFF for a given bar ID
	long countByBar_BarIdAndBarRole(Long barId, Role barRole);

}