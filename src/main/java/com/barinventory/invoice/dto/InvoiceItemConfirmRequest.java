package com.barinventory.invoice.dto;

 

import lombok.*;

/**
 * Request DTO — actual quantities entered by owner per line item during review.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceItemConfirmRequest {

    private Long itemId;

    /** Actual cases physically counted on arrival — may be less than invoicedCases */
    private Integer receivedCases;

    /** Number of broken/damaged bottles found on arrival */
    private Integer breakageQty;

    /** Confirmed brand ID from master list after owner verifies fuzzy match */
    private Long brandMasterId;

    /** Confirmed brand name after owner verifies or corrects fuzzy match */
    private String brandNameMatched;
}