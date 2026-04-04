package com.barinventory.customer.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthStatsDTO {
	private Integer totalUnitsConsumed;
	private BigDecimal totalSpent;
	private LocalDateTime periodStart;
	private LocalDateTime periodEnd;
}