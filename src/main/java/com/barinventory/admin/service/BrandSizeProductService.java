package com.barinventory.admin.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.barinventory.admin.dto.BrandSizeProductDTO;
import com.barinventory.brands.entity.Brand;
import com.barinventory.brands.entity.BrandSize;
import com.barinventory.brands.repository.BrandRepository;
import com.barinventory.brands.repository.BrandSizeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BrandSizeProductService {

    private final BrandRepository brandRepository;
    private final BrandSizeRepository brandSizeRepository;
    
    public BrandSize getById(Long brandSizeId) {
        return brandSizeRepository.findById(brandSizeId)
                .orElseThrow(() -> new RuntimeException("BrandSize not found: " + brandSizeId));
    }

    public List<BrandSize> getAll() {
        return brandSizeRepository.findAll();
    }

    // ---------------------------
    // COMMON DTO MAPPER
    // ---------------------------
    private BrandSizeProductDTO toDTO(Brand brand, BrandSize size) {

        String name = brand.getBrandName()
                + (size.getSizeLabel() != null ? " " + size.getSizeLabel() : "");

        String category = brand.getCategory() != null
                ? brand.getCategory().name()
                : "";

        BigDecimal volumeML = size.getVolumeMl() != null
                ? BigDecimal.valueOf(size.getVolumeMl())
                : BigDecimal.ZERO; // ✅ SAFE

        return new BrandSizeProductDTO(
                size.getId(),     // ✅ this is your "productId"
                name,
                category,
                brand.getBrandName(),
                volumeML
        );
    }
 
    @Transactional
    public List<BrandSizeProductDTO> getAllActiveProducts() {

        return brandRepository.findAllActiveWithActiveSizes()
                .stream()

                .flatMap(brand -> brand.getSizes().stream()
                        .filter(BrandSize::isActive)
                        .map(size -> toDTO(brand, size)) // ✅ FIXED
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