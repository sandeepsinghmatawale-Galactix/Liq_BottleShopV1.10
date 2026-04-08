package com.barinventory.common.service;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.barinventory.admin.entity.Bar;
import com.barinventory.admin.entity.BarProductPrice;
import com.barinventory.admin.repository.BarProductPriceRepository;
import com.barinventory.brands.entity.BrandSize;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PricingService {
    
    private final BarProductPriceRepository priceRepository;
    private final BarService barService;
    private final BrandSizeProductService brandSizeService; // ✅ FIXED
    
    // ---------------------------
    // GET ALL PRICES
    // ---------------------------
    public List<BarProductPrice> getPricesByBar(Long barId) {
        return priceRepository.findByBar_BarIdAndActiveTrue(barId);
    }
    
    // ---------------------------
    // GET SINGLE PRICE
    // ---------------------------
    public BarProductPrice getPrice(Long barId, Long brandSizeId) {
        return priceRepository
            .findByBar_BarIdAndBrandSize_Id(barId, brandSizeId)
            .orElseThrow(() -> new RuntimeException("Price not configured for this brand size"));
    }
    
    // ---------------------------
    // SET / UPDATE PRICE
    // ---------------------------
    @Transactional
    public BarProductPrice setPrice(Long barId, Long brandSizeId, BarProductPrice priceDetails) {
        
        Bar bar = barService.getBarById(barId);
        BrandSize brandSize = brandSizeService.getById(brandSizeId); // ✅ FIX
        
        BarProductPrice price = priceRepository
            .findByBar_BarIdAndBrandSize_Id(barId, brandSizeId)
            .orElse(BarProductPrice.builder()
                .bar(bar)
                .brandSize(brandSize)
                .build());
        
        price.setSellingPrice(priceDetails.getSellingPrice());
        price.setCostPrice(priceDetails.getCostPrice());
        price.setActive(true);
        
        return priceRepository.save(price);
    }
    
    // ---------------------------
    // DEACTIVATE
    // ---------------------------
    @Transactional
    public void deactivatePrice(Long priceId) {
        BarProductPrice price = priceRepository.findById(priceId)
            .orElseThrow(() -> new RuntimeException("Price not found"));
        price.setActive(false);
        priceRepository.save(price);
    }
    
    // ---------------------------
    // PRICE MAP (IMPORTANT FIX)
    // ---------------------------
    public Map<Long, BarProductPrice> getPriceMapForBar(Long barId) {

        List<BarProductPrice> prices =
                priceRepository.findByBar_BarIdAndActiveTrue(barId);

        return prices.stream()
                .collect(Collectors.toMap(
                        price -> price.getBrandSize().getId(), // ✅ FIXED KEY
                        price -> price
                ));
    }
    
    // ---------------------------
    // ALIAS
    // ---------------------------
    public List<BarProductPrice> getPricesForBar(Long barId) {
        return priceRepository.findByBar_BarIdAndActiveTrue(barId);
    }

    // ---------------------------
    // FORM SAVE (UPDATED)
    // ---------------------------
    @Transactional
    public void savePricesFromForm(Long barId, Map<String, String> formData) {

        Bar bar = barService.getBarById(barId);
        List<BrandSize> brandSizes = brandSizeService.getAll(); // ✅ FIX

        for (BrandSize brandSize : brandSizes) {

            String sellKey = "sell_" + brandSize.getId();
            String costKey = "cost_" + brandSize.getId();

            String sellVal = formData.get(sellKey);
            String costVal = formData.get(costKey);

            if (sellVal == null || sellVal.isBlank()) continue;

            BigDecimal sellingPrice = new BigDecimal(sellVal);
            BigDecimal costPrice = (costVal != null && !costVal.isBlank())
                    ? new BigDecimal(costVal)
                    : null;

            BarProductPrice price = priceRepository
                    .findByBar_BarIdAndBrandSize_Id(barId, brandSize.getId())
                    .orElse(BarProductPrice.builder()
                            .bar(bar)
                            .brandSize(brandSize)
                            .build());

            price.setSellingPrice(sellingPrice);
            price.setCostPrice(costPrice);
            price.setActive(true);

            priceRepository.save(price);
        }
    }
}