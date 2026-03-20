package com.barinventory.pdf.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Data;

@Entity
@Data
public class InvoiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String brandName;
    private String category;
    private String pack;
    private Integer casesQty;
    private Integer bottlesQty;
    private Double rate;
    private Double total;

    @ManyToOne
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;
}