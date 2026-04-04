package com.barinventory.admin.entity;

import java.math.BigDecimal;

import com.barinventory.admin.enums.DistributionStatus;
import com.barinventory.brands.entity.BrandSize;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "distribution_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DistributionRecord {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

 

	@ManyToOne
	@JoinColumn(name = "brand_size_id", nullable = false)
	private BrandSize brandSize;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "session_id")
	private InventorySession session;	// ── Stock & Allocation ───────────────────────────────────
	
	
	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal quantityFromStockroom = BigDecimal.ZERO; // Must match stockroom transferred

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal totalAllocated = BigDecimal.ZERO; // Sum of all well allocations

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal unallocated = BigDecimal.ZERO; // quantityFromStockroom - totalAllocated

	// ── Status ──────────────────────────────────────────────
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private DistributionStatus status = DistributionStatus.PENDING_ALLOCATION;

	@Column(length = 200)
	private String notes;

	// ── Auto-calculate unallocated & status ─────────────────
	@PrePersist
	@PreUpdate
	public void calculateUnallocated() {

	    if (this.quantityFromStockroom == null) {
	        this.quantityFromStockroom = BigDecimal.ZERO;
	    }

	    if (this.totalAllocated == null) {
	        this.totalAllocated = BigDecimal.ZERO;
	    }

	    this.unallocated = this.quantityFromStockroom.subtract(this.totalAllocated);

	    if (this.totalAllocated.compareTo(BigDecimal.ZERO) == 0) {
	        this.status = DistributionStatus.PENDING_ALLOCATION;
	    } else if (this.unallocated.compareTo(BigDecimal.ZERO) == 0) {
	        this.status = DistributionStatus.ALLOCATED;
	    } else {
	        this.status = DistributionStatus.PENDING_ALLOCATION;
	    }
	}
}