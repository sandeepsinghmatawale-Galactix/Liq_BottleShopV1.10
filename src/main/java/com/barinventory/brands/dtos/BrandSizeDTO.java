package com.barinventory.brands.dtos;

import java.math.BigDecimal;

import com.barinventory.brands.entity.BrandSize;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BrandSizeDTO {

    private Long id;
    private String sizeLabel;
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
    private boolean active;

    // ── CASE PURCHASING (new) ─────────────────────────────────
    // Mirrors the two new fields added to BrandSize entity.
    // Populated when reading from DB; bound when saving via form.
    private Integer bottlesPerCase;
    private BigDecimal casePrice;
}