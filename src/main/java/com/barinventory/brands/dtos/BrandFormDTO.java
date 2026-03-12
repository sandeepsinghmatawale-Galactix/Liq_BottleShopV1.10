package com.barinventory.brands.dtos;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.barinventory.brands.entity.Brand;
import com.barinventory.brands.entity.BrandSize;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BrandFormDTO {

    private Long id;

    // ── REQUIRED ──────────────────────────────────────────────
    @NotBlank(message = "Brand code is required")
    @Size(max = 20, message = "Brand code max 20 characters")
    private String brandCode;

    @NotBlank(message = "Brand name is required")
    private String brandName;

    // ── OPTIONAL ──────────────────────────────────────────────
    private String parentCompany;
    private Brand.Category category;
    private Brand.SubCategory subCategory;
    private String exciseCode;
    private BigDecimal exciseCessPercent;
    private BigDecimal tcsPercent;
    private BigDecimal gstPercent;

    @Builder.Default
    private boolean active = true;

    @Valid
    @Builder.Default
    private List<SizeRow> sizes = new ArrayList<>();

    // ── Nested size row ───────────────────────────────────────
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SizeRow {

        // Only sizeLabel is required on a size row
        private String sizeLabel;

        // Everything else is optional
        private Integer volumeMl;
        private BrandSize.Packaging packaging;
        private BigDecimal purchasePrice;
        private BigDecimal sellingPrice;
        private BigDecimal mrp;
        private BrandSize.MrpRounding mrpRounding;
        private BigDecimal exciseCessPercent;
        private BigDecimal tcsPercent;
        private BigDecimal gstPercent;
        private Double abvPercent;
        private String barcode;
        private String hsnCode;
        private Integer displayOrder;

        // ── CASE PURCHASING (new) ─────────────────────────────
        // How many bottles come in one depot case for this size.
        // e.g. 750ml=12, 375ml=24, 180ml=48, 90ml=96
        private Integer bottlesPerCase;

        // What the bar owner pays for one full case from the depot.
        private BigDecimal casePrice;

        @Builder.Default
        private boolean active = true;
    }
}