package com.barinventory.admin.adapter;
 
import java.time.LocalDate;

import org.springframework.stereotype.Service;

import com.barinventory.invoice.port.StockroomPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockroomPortAdapter implements StockroomPort {

    // TODO: uncomment and inject your StockroomService when ready
    // private final StockroomService stockroomService;

    @Override
    public void addStock(Long brandMasterId,
                         String brandName,
                         Integer sizeMl,
                         Integer bottlesReceived,
                         LocalDate stockReceivedDate,
                         String reference) {

        log.info("Stockroom post — BrandSizeId: {}, Brand: {}, " +
                 "Size: {}ml, Bottles: {}, Date: {}, Ref: {}",
                brandMasterId, brandName, sizeMl,
                bottlesReceived, stockReceivedDate, reference);

        // TODO: replace with your actual stockroom service call e.g.
        // stockroomService.addOpeningStock(
        //         brandMasterId,
        //         bottlesReceived,
        //         stockReceivedDate,
        //         reference);
    }
}