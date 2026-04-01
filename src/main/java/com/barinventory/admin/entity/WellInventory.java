package com.barinventory.admin.entity;

import java.math.BigDecimal;

import com.barinventory.brands.entity.BrandSize;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "well_inventory")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WellInventory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "session_id", nullable = false)
	@JsonIgnore
	private InventorySession session;

	// ── BRAND SIZE
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "brand_size_id", nullable = false)
	private BrandSize brandSize;
	
	@Column(nullable = false, length = 50)
	private String wellName; // BAR_1, BAR_2, SERVICE_BAR, etc.

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal openingStock = BigDecimal.ZERO;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal receivedFromDistribution = BigDecimal.ZERO;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal closingStock = BigDecimal.ZERO;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal consumed = BigDecimal.ZERO; // Opening + Received - Closing

	@Column(length = 200)
	private String remarks;

	@PrePersist
	@PreUpdate
	public void calculateConsumed() {
		this.consumed = this.openingStock.add(this.receivedFromDistribution).subtract(this.closingStock);
	}
}
