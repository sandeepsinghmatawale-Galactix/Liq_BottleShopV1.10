package com.barinventory.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsumptionLogDTO {
	private String brandName;
	private String sizeLabel;
	private Integer volumeMl;
	private Double abvPercent;
	private Integer unitsConsumed;
}