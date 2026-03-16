package com.barinventory.invoice.port;

import java.time.LocalDate;

public interface StockroomPort {

    /**
     * @param brandMasterId     BrandSize.id from your brand module
     * @param brandName         display name — fallback if id lookup fails
     * @param sizeMl            bottle size in ml
     * @param bottlesReceived   net bottles after breakage deduction
     * @param stockReceivedDate actual vehicle arrival date — stock posting date
     * @param reference         audit trail e.g. "INVOICE:ICDC037190620002339"
     */
    void addStock(Long brandMasterId,
                  String brandName,
                  Integer sizeMl,
                  Integer bottlesReceived,
                  LocalDate stockReceivedDate,
                  String reference);
}