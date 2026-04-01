package com.barinventory.admin.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;

 
public class BrandSizeProductDTO {

 
	private Long id;
	private String productName; // = "Old Monk 750ml"
	private String category; // = "RUM"
	private String brand; // = "Old Monk"
	private BigDecimal volumeML; // = 750.00
	private Boolean active = true;

	public BrandSizeProductDTO() {
	}

	public BrandSizeProductDTO(Long brandSizeId, String productName, String category, String brand,
			BigDecimal volumeML) {
		this.id = brandSizeId;
		this.productName = productName;
		this.category = category;
		this.brand = brand;
		this.volumeML = volumeML;
		this.active = true;
	}

	public Long getId() {
        return id;
    }
	public Long getProductId() {
		return id;
	}

	public String getProductName() {
		return productName;
	}

	public String getCategory() {
		return category;
	}

	public String getBrand() {
		return brand;
	}

	public BigDecimal getVolumeML() {
		return volumeML;
	}

	public Boolean getActive() {
		return active;
	}

	public void setProductId(Long v) {
		this.id = v;
	}

	public void setProductName(String v) {
		this.productName = v;
	}

	public void setCategory(String v) {
		this.category = v;
	}

	public void setBrand(String v) {
		this.brand = v;
	}

	public void setVolumeML(BigDecimal v) {
		this.volumeML = v;
	}

	public void setActive(Boolean v) {
		this.active = v;
	}
}