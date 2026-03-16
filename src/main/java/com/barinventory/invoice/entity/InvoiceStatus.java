package com.barinventory.invoice.entity;

public enum InvoiceStatus {

	PENDING,              // PDF uploaded, extraction done, owner reviewing
    REVIEWED,             // Owner has reviewed extracted data
    CONFIRMED,            // Owner confirmed, stock posted to stockroom successfully
    DISCREPANCY,          // Confirmed but shortage/breakage found — flagged for review
    PARTIALLY_RECEIVED,   // Some items received, some short/missing
    CANCELLED             // Invoice cancelled / rejected
}
