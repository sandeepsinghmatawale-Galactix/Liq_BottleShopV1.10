package com.barinventory.admin.repository;

import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.barinventory.admin.entity.Bar;
import com.barinventory.admin.enums.BarSubscriptionStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface BarRepository extends JpaRepository<Bar, Long> {
    
    List<Bar> findByActiveTrue();

    List<Bar> findByActiveTrueAndHiddenOnPlatformFalseOrderByBarNameAsc();
    
    Optional<Bar> findByBarName(String barName);
    
    boolean existsByBarName(String barName);

    @Query("SELECT b FROM Bar b WHERE b.subscriptionEndDate IS NOT NULL "
            + "AND b.subscriptionEndDate < :today "
            + "AND b.subscriptionStatus IN :statuses")
    List<Bar> findBarsToExpire(@Param("today") LocalDate today,
            @Param("statuses") List<BarSubscriptionStatus> statuses);
}
