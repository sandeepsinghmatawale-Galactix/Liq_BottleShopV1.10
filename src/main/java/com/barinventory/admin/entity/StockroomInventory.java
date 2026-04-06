package com.barinventory.admin.entity;

import java.math.BigDecimal;

import com.barinventory.brands.entity.BrandSize;
import com.fasterxml.jackson.annotation.JsonBackReference;

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
@Table(name = "stockroom_inventory")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockroomInventory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "product_id")
	private Product product;
	
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "brand_size_id", nullable = false)
	private BrandSize brandSize;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "bar_id")
	private Bar bar;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "session_id")
	@JsonBackReference
	private InventorySession session;

	@Column(nullable = false)
	private boolean opening = false;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal openingStock = BigDecimal.ZERO; // Previous closing

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal receivedStock = BigDecimal.ZERO; // New deliveries

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal closingStock = BigDecimal.ZERO; // Physical count

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal transferredOut = BigDecimal.ZERO; // Opening + Received - Closing

	@Column(length = 200)
	private String remarks;

	@PrePersist
	@PreUpdate
	public void calculateTransferred() {
		this.transferredOut = this.openingStock.add(this.receivedStock).subtract(this.closingStock);
	}
}
