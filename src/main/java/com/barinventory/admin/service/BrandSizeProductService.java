package com.barinventory.admin.service;

 

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.barinventory.admin.dto.BrandSizeProductDTO;
import com.barinventory.brands.entity.Brand;
import com.barinventory.brands.entity.BrandSize;
import com.barinventory.brands.repository.BrandRepository;

import lombok.RequiredArgsConstructor;

 
@Service
@RequiredArgsConstructor
public class BrandSizeProductService {

    private final BrandRepository brandRepository;

    

    private BrandSizeProductDTO toDTO(Brand brand, BrandSize size) {
        // productName = "Old Monk 750ml" — matches what admin sees on screen
        String name = brand.getBrandName()
                    + (size.getSizeLabel() != null ? " " + size.getSizeLabel() : "");

        String category = brand.getCategory() != null
                ? brand.getCategory().name() : null;

        BigDecimal volumeML = size.getVolumeMl() != null
                ? BigDecimal.valueOf(size.getVolumeMl()) : null;

        return new BrandSizeProductDTO(
                size.getId(),           // BrandSize.id → used as productId in field names
                name,
                category,
                brand.getBrandName(),
                volumeML
        );
    }
  @Transactional(readOnly = true)
    public List<BrandSizeProductDTO> getAllActiveProducts() {

        return brandRepository.findAllActiveWithActiveSizes()
                .stream()

                .flatMap(brand -> brand.getSizes().stream()
                        .filter(BrandSize::isActive)
                        .map(size -> new BrandSizeProductDTO(

                                size.getId(),

                                brand.getBrandName() + " " + size.getSizeLabel(),

                                brand.getCategory() != null
                                        ? brand.getCategory().name()
                                        : "",

                                brand.getBrandName(),

                                BigDecimal.valueOf(size.getVolumeMl())   // FIX HERE
                        ))
                )

                .sorted((a, b) -> {

                    int c = a.getCategory().compareToIgnoreCase(b.getCategory());
                    if (c != 0) return c;

                    int b1 = a.getBrand().compareToIgnoreCase(b.getBrand());
                    if (b1 != 0) return b1;

                    return a.getVolumeML().compareTo(b.getVolumeML());

                })

                .toList();
    }
  
  
  
  
}