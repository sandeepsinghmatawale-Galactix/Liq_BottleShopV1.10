package com.barinventory.brands.service;

import java.util.List;

import com.barinventory.admin.dto.BrandSizeProductDTO;
import com.barinventory.billing.dtos.BrandBillingDTO;
import com.barinventory.brands.dtos.BrandDTO;
import com.barinventory.brands.dtos.BrandFormDTO;
import com.barinventory.brands.dtos.BrandSizeDTO;
import com.barinventory.brands.entity.Brand;
 

public interface BrandService {

	 List<Brand>    getAllBrands();
	    List<BrandDTO> getAllActiveBrands();
	    BrandDTO       getBrandById(Long id);
	    BrandDTO       createBrand(BrandDTO dto);
	    BrandDTO       createBrandWithSizes(BrandFormDTO form);
	    BrandDTO       updateBrand(Long id, BrandDTO dto);
	    BrandDTO       updateBrandWithSizes(Long id, BrandFormDTO form);
	    void           deactivateBrand(Long id);

	    // Size operations
	    void addSizeToBrand(Long brandId, BrandSizeDTO dto);
	    void deactivateSize(Long sizeId);
	    
	 
	    List<BrandSizeProductDTO> getAllActiveProducts();
	    
	    List<BrandSizeDTO> getAllBrandSizes();
	    
	    List<Brand> getAllActiveBrandsForCustomer();
	    
	    public List<BrandBillingDTO> getAllActiveBrands2();
	    
	 

}
