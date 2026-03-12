package com.barinventory.brands.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.barinventory.brands.CaseConfig;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "brand_sizes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BrandSize {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Parent ────────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

    // ── REQUIRED ──────────────────────────────────────────────
    @Column(name = "size_label", nullable = false, length = 20)
    private String sizeLabel;           // e.g. "750ml"

    // ── OPTIONAL — all nullable so partial saves work ─────────
    @Column(name = "volume_ml")
    private Integer volumeMl;

    @Enumerated(EnumType.STRING)
    @Column(name = "packaging")
    private Packaging packaging;

    @Column(name = "purchase_price", precision = 10, scale = 2)
    private BigDecimal purchasePrice;

    @Column(name = "selling_price", precision = 10, scale = 2)
    private BigDecimal sellingPrice;

    @Column(name = "mrp", precision = 10, scale = 2)
    private BigDecimal mrp;

    @Enumerated(EnumType.STRING)
    @Column(name = "mrp_rounding")
    private MrpRounding mrpRounding;

    @Column(name = "excise_cess_percent", precision = 5, scale = 2)
    private BigDecimal exciseCessPercent;

    @Column(name = "tcs_percent", precision = 5, scale = 2)
    private BigDecimal tcsPercent;

    @Column(name = "gst_percent", precision = 5, scale = 2)
    private BigDecimal gstPercent;

    // ✅ Double → SQL DOUBLE (floating point) — NO precision/scale allowed
    @Column(name = "abv_percent")
    private Double abvPercent;

    @Column(name = "barcode", length = 50)
    private String barcode;

    @Column(name = "hsn_code", length = 20)
    private String hsnCode;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    // ── CASE PURCHASING ───────────────────────────────────────
    // A case is the wholesale unit received from depot.
    // e.g. 750ml → 12 bottles/case, 180ml → 48 bottles/case

    /**
     * How many individual bottles come in one depot case for this size.
     * Nullable — bar owner can leave blank if they buy loose bottles.
     * Examples: 750ml=12, 375ml=24, 180ml=48, 90ml=96
     */
    @Column(name = "bottles_per_case")
    private Integer bottlesPerCase;

    /**
     * Price the bar owner pays for one full case from the depot.
     * Per-bottle cost = casePrice / bottlesPerCase (see helper below).
     */
    @Column(name = "case_price", precision = 10, scale = 2)
    private BigDecimal casePrice;

    // ── Case config variants (optional — for brands that sell
    //    the same size in multiple case-pack sizes, e.g. 6-pack & 12-pack)
    @OneToMany(mappedBy = "brandSize", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CaseConfig> caseConfigs = new ArrayList<>();

    public void addCaseConfig(CaseConfig config) {
        caseConfigs.add(config);
        config.setBrandSize(this);
    }

    public void removeCaseConfig(CaseConfig config) {
        caseConfigs.remove(config);
        config.setBrandSize(null);
    }

    // ── Transient helpers ─────────────────────────────────────

    @Transient
    public BigDecimal getEffectiveMrp() {
        if (mrp == null) return null;
        if (mrpRounding == null || mrpRounding == MrpRounding.NONE) return mrp;
        return mrpRounding.apply(mrp);
    }

    @Transient
    public BigDecimal getGrossMargin() {
        if (sellingPrice == null || purchasePrice == null) return null;
        return sellingPrice.subtract(purchasePrice);
    }

    /**
     * Cost per bottle derived from case price.
     * Returns null if either field is missing or bottlesPerCase is zero.
     */
    @Transient
    public BigDecimal getCostPerBottleFromCase() {
        if (casePrice == null || bottlesPerCase == null || bottlesPerCase == 0) return null;
        return casePrice.divide(BigDecimal.valueOf(bottlesPerCase), 2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Margin per bottle when bar owner sells at sellingPrice
     * and sources bottles via casePrice.
     */
    @Transient
    public BigDecimal getCaseMarginPerBottle() {
        BigDecimal costPerBottle = getCostPerBottleFromCase();
        if (costPerBottle == null || sellingPrice == null) return null;
        return sellingPrice.subtract(costPerBottle);
    }

    // ── Enums ─────────────────────────────────────────────────
    public enum Packaging {
        GLASS_BOTTLE, PET, CAN, TETRA, KEG
    }

    public enum MrpRounding {
        NONE {
            @Override public BigDecimal apply(BigDecimal v) { return v; }
        },
        ROUND_UP {
            @Override public BigDecimal apply(BigDecimal v) {
                return v.setScale(0, java.math.RoundingMode.CEILING);
            }
        },
        ROUND_DOWN {
            @Override public BigDecimal apply(BigDecimal v) {
                return v.setScale(0, java.math.RoundingMode.FLOOR);
            }
        },
        NEAREST_5 {
            @Override public BigDecimal apply(BigDecimal v) {
                return v.divide(BigDecimal.valueOf(5), 0, java.math.RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(5));
            }
        },
        NEAREST_10 {
            @Override public BigDecimal apply(BigDecimal v) {
                return v.divide(BigDecimal.TEN, 0, java.math.RoundingMode.HALF_UP)
                        .multiply(BigDecimal.TEN);
            }
        };

        public abstract BigDecimal apply(BigDecimal value);
    }
}