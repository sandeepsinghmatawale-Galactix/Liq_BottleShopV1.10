package com.barinventory.customer.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDTO {
	private Long id;
	private Long brandSizeId;
	private String brandName;
	private String sizeLabel;
	private Integer quantity;
	private BigDecimal price;
	private BigDecimal subtotal;
}