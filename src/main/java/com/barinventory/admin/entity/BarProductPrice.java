package com.barinventory.admin.entity;

import java.math.BigDecimal;

import com.barinventory.brands.entity.BrandSize;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "bar_product_prices",
    uniqueConstraints = @UniqueConstraint(columnNames = {"bar_id", "brand_size_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BarProductPrice {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // ✅ BAR
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bar_id", nullable = false)
    private Bar bar;
    
    // ✅ FIX: Replace Product with BrandSize
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_size_id", nullable = false)
    private BrandSize brandSize;
    
    // 💰 Selling price (per peg / bottle depending on usage)
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal sellingPrice;
    
    // 💰 Cost price (optional but useful)
    @Column(precision = 10, scale = 2)
    private BigDecimal costPrice;
    
    // ✅ Active flag
    @Column(nullable = false)
    private Boolean active = true;
}