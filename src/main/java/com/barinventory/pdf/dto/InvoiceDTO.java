package com.barinventory.pdf.dto;

import java.util.List;

import lombok.Data;

@Data
public class InvoiceDTO {
    private String invoiceNumber;
    private String retailerName;
    private List<InvoiceItemDTO> items;
}