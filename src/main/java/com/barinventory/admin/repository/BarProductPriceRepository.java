package com.barinventory.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.barinventory.admin.entity.BarProductPrice;

import java.util.List;
import java.util.Optional;

@Repository
public interface BarProductPriceRepository extends JpaRepository<BarProductPrice, Long> {

	List<BarProductPrice> findByBarBarId(Long barId);

	List<BarProductPrice> findByBarBarIdAndActiveTrue(Long barId);

	Optional<BarProductPrice> findByBarBarIdAndBrandSizeId(Long barId, Long brandSizeId);

	Optional<BarProductPrice> findByBar_BarIdAndBrandSize_Id(Long barId, Long brandSizeId);
	
	 List<BarProductPrice> findByBar_BarId(Long barId);

	    List<BarProductPrice> findByBar_BarIdAndActiveTrue(Long barId);

	    
	    boolean existsByBar_BarIdAndBrandSize_Id(Long barId, Long brandSizeId);

}
