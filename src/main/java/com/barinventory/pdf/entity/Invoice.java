package com.barinventory.pdf.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.Data;

@Entity
@Data
public class Invoice {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String invoiceNumber;
	private LocalDate invoiceDate;
	private String retailerName;
	private Double totalAmount;

	private String shopName;
	private String licenseNo;
	private String address;

	private BigDecimal taxAmount;
	private BigDecimal grandTotal;

	private String retailerCode;

	private String licenseName;
	private String licensePhone;

	private Double invoiceValue;
	private Double mrpRounding;

	private Double echallan;
	private Double previousCredit;
	private Double subtotal;
	private Double lessInvoice;
	private Double specialCess;
	private Double tcs;

	private Integer imflCases;
	private Integer beerCases;
	private Integer totalCases;
	private Integer imflBottles;
	private Integer beerBottles;
	private Integer totalBottles;
	private Double eChallanAmount;
	@Column(name = "net_invoice_value")
	private Double netInvoiceValue;

	@Column(name = "retailer_credit_balance")
	private Double retailerCreditBalance;

	private Double lessInvoiceValue;
	private Double specialExciseCess;

	@OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL)
	private List<InvoiceItem> items;
}