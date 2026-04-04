package com.barinventory.customer.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExploreProductDTO {

    private Long brandSizeId;
    private String productName;
    private String category;
    private String brand;
    private Integer volumeML;
     
}
