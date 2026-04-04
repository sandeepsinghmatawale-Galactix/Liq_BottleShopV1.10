package com.barinventory.brands.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.barinventory.admin.dto.BrandSizeProductDTO;
import com.barinventory.billing.dtos.BrandBillingDTO;
import com.barinventory.brands.dtos.BrandDTO;
import com.barinventory.brands.dtos.BrandFormDTO;
import com.barinventory.brands.dtos.BrandSizeDTO;
import com.barinventory.brands.entity.Brand;
import com.barinventory.brands.entity.BrandSize;
import com.barinventory.brands.repository.BrandRepository;
import com.barinventory.brands.repository.BrandSizeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BrandServiceImpl implements BrandService {

	private final BrandRepository brandRepository;
	private final BrandSizeRepository brandSizeRepository;

	// ── GET ALL ───────────────────────────────────────────────────────
	@Override
	@Transactional(readOnly = true)
	public List<Brand> getAllBrands() {
		return brandRepository.findAll();
	}

	// ── GET ACTIVE ────────────────────────────────────────────────────
	@Override
	@Transactional(readOnly = true)
	public List<BrandDTO> getAllActiveBrands() {
		return brandRepository.findAllActiveWithActiveSizes().stream().map(this::mapToDTO).toList();
	}

	// ── GET BY ID ─────────────────────────────────────────────────────
	@Override
	@Transactional(readOnly = true)
	public BrandDTO getBrandById(Long id) {
		return brandRepository.findByIdWithSizes(id).map(this::mapToDTO)
				.orElseThrow(() -> new RuntimeException("Brand not found: " + id));
	}

	// ── CREATE (basic, API use) ────────────────────────────────────────
	@Override
	@Transactional
	public BrandDTO createBrand(BrandDTO dto) {
		if (brandRepository.existsByBrandNameIgnoreCase(dto.getBrandName()))
			throw new RuntimeException("Brand already exists: " + dto.getBrandName());

		Brand brand = Brand.builder().brandCode(dto.getBrandCode()).brandName(dto.getBrandName())
				.parentCompany(dto.getParentCompany()).category(dto.getCategory()).subCategory(dto.getSubCategory())
				.exciseCode(dto.getExciseCode()).exciseCessPercent(dto.getExciseCessPercent())
				.tcsPercent(dto.getTcsPercent()).gstPercent(dto.getGstPercent()).active(true).build();
		return mapToDTO(brandRepository.save(brand));
	}

	@Override
	@Transactional(readOnly = true)
	public List<BrandBillingDTO> getAllActiveBrands2() {
		List<Brand> brands = brandRepository.findAllActiveWithActiveSizes();

		return brands.stream()
				.map(brand -> new BrandBillingDTO(brand.getId(), brand.getBrandName(),
						brand.getSizes().stream().filter(BrandSize::isActive) // only active sizes
								.map(size -> new BrandBillingDTO.BrandSizeDTO(size.getId(), size.getSizeLabel(),
										size.getMrp(), size.isActive()))
								.toList()))
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<Brand> getAllActiveBrandsForCustomer() {
		return brandRepository.findByActiveTrueOrderByBrandNameAsc();
	}

	// ── CREATE WITH SIZES (form) ──────────────────────────────────────
	@Override
	@Transactional
	public BrandDTO createBrandWithSizes(BrandFormDTO form) {
		if (brandRepository.existsByBrandNameIgnoreCase(form.getBrandName()))
			throw new RuntimeException("Brand already exists: " + form.getBrandName());

		if (brandRepository.existsByBrandCodeIgnoreCase(form.getBrandCode()))
			throw new RuntimeException("Brand code already in use: " + form.getBrandCode());

		Brand brand = Brand.builder().brandCode(form.getBrandCode().trim().toUpperCase())
				.brandName(form.getBrandName().trim())

				.parentCompany(form.getParentCompany()).category(form.getCategory()).subCategory(form.getSubCategory())
				.exciseCode(form.getExciseCode()).exciseCessPercent(form.getExciseCessPercent())
				.tcsPercent(form.getTcsPercent()).gstPercent(form.getGstPercent()).active(form.isActive()).build();

		addSizeRows(brand, form);
		return mapToDTO(brandRepository.save(brand));
	}

	@Override
	@Transactional(readOnly = true)
	public List<BrandSizeProductDTO> getAllActiveProducts() {

		return brandRepository.findAllActiveWithActiveSizes().stream()
				.flatMap(brand -> brand.getSizes().stream().filter(BrandSize::isActive)
						.map(size -> new BrandSizeProductDTO(

								size.getId(),

								brand.getBrandName() + " " + size.getSizeLabel(),

								brand.getCategory() != null ? brand.getCategory().name() : "",

								brand.getBrandName(),

								size.getVolumeMl() != null ? BigDecimal.valueOf(size.getVolumeMl()) : BigDecimal.ZERO)))
				.sorted((a, b) -> {

					int c = a.getCategory().compareToIgnoreCase(b.getCategory());
					if (c != 0)
						return c;

					int b1 = a.getBrand().compareToIgnoreCase(b.getBrand());
					if (b1 != 0)
						return b1;

					return a.getVolumeML().compareTo(b.getVolumeML());

				}).toList();
	}

	// ── UPDATE WITH SIZES (form) ──────────────────────────────────────
	@Override
	@Transactional
	public BrandDTO updateBrandWithSizes(Long id, BrandFormDTO form) {
		Brand brand = brandRepository.findByIdWithSizes(id)
				.orElseThrow(() -> new RuntimeException("Brand not found: " + id));

		// Check code uniqueness (exclude self)
		brandRepository.findByBrandCodeIgnoreCase(form.getBrandCode()).filter(b -> !b.getId().equals(id))
				.ifPresent(b -> {
					throw new RuntimeException("Brand code in use: " + form.getBrandCode());
				});

		brand.setBrandCode(form.getBrandCode().trim().toUpperCase());
		brand.setBrandName(form.getBrandName().trim());
		brand.setParentCompany(form.getParentCompany());
		brand.setCategory(form.getCategory());
		brand.setSubCategory(form.getSubCategory());
		brand.setExciseCode(form.getExciseCode());
		brand.setExciseCessPercent(form.getExciseCessPercent());
		brand.setTcsPercent(form.getTcsPercent());
		brand.setGstPercent(form.getGstPercent());
		brand.setActive(form.isActive());

		brand.clearSizes(); // orphanRemoval deletes old rows
		addSizeRows(brand, form);

		return mapToDTO(brandRepository.save(brand));
	}

	// ── UPDATE BRAND FIELDS ONLY ──────────────────────────────────────
	@Override
	@Transactional
	public BrandDTO updateBrand(Long id, BrandDTO dto) {
		Brand brand = brandRepository.findByIdWithSizes(id)
				.orElseThrow(() -> new RuntimeException("Brand not found: " + id));
		brand.setBrandCode(dto.getBrandCode());
		brand.setBrandName(dto.getBrandName());
		brand.setParentCompany(dto.getParentCompany());
		brand.setCategory(dto.getCategory());
		brand.setSubCategory(dto.getSubCategory());
		brand.setExciseCode(dto.getExciseCode());
		brand.setExciseCessPercent(dto.getExciseCessPercent());
		brand.setTcsPercent(dto.getTcsPercent());
		brand.setGstPercent(dto.getGstPercent());
		return mapToDTO(brandRepository.save(brand));
	}

	// ── DEACTIVATE ────────────────────────────────────────────────────
	@Override
	@Transactional
	public void deactivateBrand(Long id) {
		Brand brand = brandRepository.findById(id).orElseThrow(() -> new RuntimeException("Brand not found: " + id));
		brand.setActive(false);
		brandRepository.save(brand);
	}

	// ── ADD SINGLE SIZE ───────────────────────────────────────────────
	@Override
	@Transactional
	public void addSizeToBrand(Long brandId, BrandSizeDTO dto) {
		Brand brand = brandRepository.findByIdWithSizes(brandId)
				.orElseThrow(() -> new RuntimeException("Brand not found: " + brandId));
		if (brandSizeRepository.existsByBrandIdAndSizeLabelIgnoreCase(brandId, dto.getSizeLabel()))
			throw new RuntimeException("Size '" + dto.getSizeLabel() + "' already exists");

		BrandSize size = BrandSize.builder().sizeLabel(dto.getSizeLabel()).volumeMl(dto.getVolumeMl())
				.packaging(dto.getPackaging() != null ? dto.getPackaging() : BrandSize.Packaging.GLASS_BOTTLE)
				.purchasePrice(dto.getPurchasePrice()).sellingPrice(dto.getSellingPrice()).mrp(dto.getMrp())
				.mrpRounding(dto.getMrpRounding()).exciseCessPercent(dto.getExciseCessPercent())
				.tcsPercent(dto.getTcsPercent()).gstPercent(dto.getGstPercent()).abvPercent(dto.getAbvPercent())
				.barcode(dto.getBarcode()).hsnCode(dto.getHsnCode()).displayOrder(dto.getDisplayOrder()).active(true)
				.build();
		brand.addSize(size);
		brandRepository.save(brand);
	}

	// ── DEACTIVATE SIZE ───────────────────────────────────────────────
	@Override
	@Transactional
	public void deactivateSize(Long sizeId) {
		BrandSize size = brandSizeRepository.findByIdAndActiveTrue(sizeId)
				.orElseThrow(() -> new RuntimeException("Size not found: " + sizeId));
		size.setActive(false);
		brandSizeRepository.save(size);
	}

	// ── PRIVATE: build BrandSize from SizeRow ─────────────────────────
	private void addSizeRows(Brand brand, BrandFormDTO form) {
		if (form.getSizes() == null)
			return;
		for (BrandFormDTO.SizeRow row : form.getSizes()) {
			// Only skip rows where even the label is blank (user added an empty row by
			// mistake)
			if (row.getSizeLabel() == null || row.getSizeLabel().isBlank())
				continue;

			BrandSize size = BrandSize.builder().sizeLabel(row.getSizeLabel().trim()).volumeMl(row.getVolumeMl())
					.packaging(row.getPackaging() != null ? row.getPackaging() : BrandSize.Packaging.GLASS_BOTTLE)
					.purchasePrice(row.getPurchasePrice()).sellingPrice(row.getSellingPrice()).mrp(row.getMrp())
					.mrpRounding(row.getMrpRounding() != null ? row.getMrpRounding() : BrandSize.MrpRounding.NONE)
					.exciseCessPercent(row.getExciseCessPercent()).tcsPercent(row.getTcsPercent())
					.gstPercent(row.getGstPercent()).abvPercent(row.getAbvPercent()).barcode(row.getBarcode())
					.hsnCode(row.getHsnCode()).displayOrder(row.getDisplayOrder()).active(row.isActive()).build();
			brand.addSize(size);
		}
	}

	// ── MAPPER: Entity → DTO ──────────────────────────────────────────
	private BrandDTO mapToDTO(Brand brand) {
		return BrandDTO.builder().id(brand.getId()).brandCode(brand.getBrandCode()).brandName(brand.getBrandName())
				.parentCompany(brand.getParentCompany()).category(brand.getCategory())
				.subCategory(brand.getSubCategory()).exciseCode(brand.getExciseCode())
				.exciseCessPercent(brand.getExciseCessPercent()).tcsPercent(brand.getTcsPercent())
				.gstPercent(brand.getGstPercent()).active(brand.isActive())
				.sizes(brand.getSizes().stream().filter(BrandSize::isActive)
						.map(s -> BrandSizeDTO.builder().id(s.getId()).sizeLabel(s.getSizeLabel())
								.volumeMl(s.getVolumeMl()).packaging(s.getPackaging())
								.purchasePrice(s.getPurchasePrice()).sellingPrice(s.getSellingPrice()).mrp(s.getMrp())
								.mrpRounding(s.getMrpRounding()).exciseCessPercent(s.getExciseCessPercent())
								.tcsPercent(s.getTcsPercent()).gstPercent(s.getGstPercent())
								.abvPercent(s.getAbvPercent()).barcode(s.getBarcode()).hsnCode(s.getHsnCode())
								.displayOrder(s.getDisplayOrder()).active(s.isActive()).build())
						.toList())
				.build();
	}

	private BrandSizeDTO convertToDTO(BrandSize bs) {
		return BrandSizeDTO.builder().id(bs.getId()).brandName(bs.getBrand().getBrandName()) // ✅ NOW VALID
				.sizeLabel(bs.getSizeLabel()).volumeMl(bs.getVolumeMl()).packaging(bs.getPackaging())
				.purchasePrice(bs.getPurchasePrice()).sellingPrice(bs.getSellingPrice()).mrp(bs.getMrp())
				.mrpRounding(bs.getMrpRounding()).exciseCessPercent(bs.getExciseCessPercent())
				.tcsPercent(bs.getTcsPercent()).gstPercent(bs.getGstPercent()).abvPercent(bs.getAbvPercent())
				.barcode(bs.getBarcode()).hsnCode(bs.getHsnCode()).displayOrder(bs.getDisplayOrder())
				.active(bs.isActive()).bottlesPerCase(bs.getBottlesPerCase()).casePrice(bs.getCasePrice()).build();
	}

	@Override
	public List<BrandSizeDTO> getAllBrandSizes() {
		return brandSizeRepository.findAll().stream().filter(BrandSize::isActive) // ✅ only active
				.map(this::convertToDTO).toList();
	}

}