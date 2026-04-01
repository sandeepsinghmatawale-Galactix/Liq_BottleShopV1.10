package com.barinventory.admin.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

import com.barinventory.brands.entity.BrandSize;

@Entity
@Table(name = "sales_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private InventorySession session;
    
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "brand_size_id", nullable = false)
    private BrandSize brandSize; // Specific SKU/variant
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal quantitySold = BigDecimal.ZERO;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal sellingPricePerUnit = BigDecimal.ZERO;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalRevenue = BigDecimal.ZERO;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal costPricePerUnit = BigDecimal.ZERO;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal totalCost = BigDecimal.ZERO;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal profit = BigDecimal.ZERO;
    
    @PrePersist
    @PreUpdate
    public void calculateTotals() {
        this.totalRevenue = this.quantitySold.multiply(this.sellingPricePerUnit);
        this.totalCost = this.quantitySold.multiply(this.costPricePerUnit);
        this.profit = this.totalRevenue.subtract(this.totalCost);
    }
}