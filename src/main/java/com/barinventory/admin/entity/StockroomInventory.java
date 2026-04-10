package com.barinventory.admin.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    private BigDecimal openingStock = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal receivedStock = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal closingStock = BigDecimal.ZERO;

    @Column(name = "transferred_out", nullable = false, precision = 10, scale = 2)
    private BigDecimal transferredOut = BigDecimal.ZERO;

    @Column(length = 200)
    private String remarks;

    @Column(name = "current_stock", nullable = false, precision = 10, scale = 2)
    private BigDecimal currentStock = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;

    @PrePersist
    @PreUpdate
    private void syncDerivedFields() {
        if (openingStock == null) openingStock = BigDecimal.ZERO;
        if (receivedStock == null) receivedStock = BigDecimal.ZERO;
        if (closingStock == null) closingStock = BigDecimal.ZERO;

        transferredOut = openingStock.add(receivedStock).subtract(closingStock);
        currentStock = closingStock;

        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        lastUpdated = now;
    }
}
