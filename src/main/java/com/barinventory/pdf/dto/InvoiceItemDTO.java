package com.barinventory.pdf.dto;

import lombok.Data;

@Data
public class InvoiceItemDTO {
    private String brandName;
    private Integer casesQty;
    private Double rate;
}