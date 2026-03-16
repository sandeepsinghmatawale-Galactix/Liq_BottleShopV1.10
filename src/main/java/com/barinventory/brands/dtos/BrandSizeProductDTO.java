package com.barinventory.brands.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BrandSizeProductDTO {
	

    private Long productId;
    private String productName;
    private String category;
    private String brand;
    private Integer volumeML ;

    
 
    
}