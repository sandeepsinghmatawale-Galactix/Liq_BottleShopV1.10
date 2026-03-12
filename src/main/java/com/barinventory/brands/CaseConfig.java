package com.barinventory.brands;



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
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents one case-pack configuration for a BrandSize.
 *
 * Why a separate table?
 * ─────────────────────
 * A single bottle size can be sold in multiple case formats.
 * For example, a depot may offer Blenders Pride 750ml as:
 *   • Standard case  → 12 bottles @ ₹6,000
 *   • Economy case   →  6 bottles @ ₹3,100
 *
 * If your depot only ever has one case size per SKU, you can
 * ignore this entity entirely and just use the two shortcut
 * fields (bottlesPerCase / casePrice) on BrandSize directly.
 *
 * DB table: brand_size_case_configs
 */
@Entity
@Table(
    name = "brand_size_case_configs",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_size_case_label",
        columnNames = {"brand_size_id", "case_label"}
    )
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CaseConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Parent size ───────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_size_id", nullable = false)
    private BrandSize brandSize;

    // ── REQUIRED ──────────────────────────────────────────────

    /**
     * Human label for this case configuration.
     * e.g. "Standard 12", "Half-case 6", "Bulk 24"
     */
    @Column(name = "case_label", nullable = false, length = 50)
    private String caseLabel;

    /**
     * Number of individual bottles inside this case.
     * e.g. 12 for 750ml, 24 for 375ml, 48 for 180ml, 96 for 90ml
     */
    @Column(name = "bottles_per_case", nullable = false)
    private Integer bottlesPerCase;

    /**
     * Price the bar owner pays for this entire case from the depot.
     */
    @Column(name = "case_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal casePrice;

    // ── OPTIONAL ──────────────────────────────────────────────

    /**
     * Marks which case config is the default/primary one used in
     * purchase orders and stock calculations.
     */
    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private boolean isDefault = false;

    /**
     * Optional: depot-side barcode or SKU for this case pack.
     */
    @Column(name = "case_barcode", length = 50)
    private String caseBarcode;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    // ── Transient helpers ─────────────────────────────────────

    /**
     * Per-bottle cost derived from this case configuration.
     */
    @Transient
    public BigDecimal getCostPerBottle() {
        if (bottlesPerCase == null || bottlesPerCase == 0 || casePrice == null) return null;
        return casePrice.divide(BigDecimal.valueOf(bottlesPerCase), 2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Total cost to buy N cases.
     */
    @Transient
    public BigDecimal totalCostForCases(int numberOfCases) {
        if (casePrice == null) return null;
        return casePrice.multiply(BigDecimal.valueOf(numberOfCases));
    }

    /**
     * Total bottles received for N cases.
     */
    @Transient
    public int totalBottlesForCases(int numberOfCases) {
        return bottlesPerCase * numberOfCases;
    }
}