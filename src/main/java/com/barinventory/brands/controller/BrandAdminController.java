package com.barinventory.brands.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.barinventory.brands.dtos.BrandDTO;
import com.barinventory.brands.dtos.BrandFormDTO;
import com.barinventory.brands.dtos.BrandSizeDTO;
import com.barinventory.brands.entity.Brand;
import com.barinventory.brands.entity.BrandSize;
import com.barinventory.brands.service.BrandService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/brands")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class BrandAdminController {

    private final BrandService brandService;

    // ── LIST ──────────────────────────────────────────────────────────
    @GetMapping
    public String listBrands(Model model) {
        List<Brand> brands = brandService.getAllBrands();

        long activeCount   = brands.stream().filter(Brand::isActive).count();
        long totalSizes    = brands.stream().mapToLong(b -> b.getSizes().size()).sum();
        long categoryCount = brands.stream().map(Brand::getCategory).distinct().count();

        model.addAttribute("brands",        brands);
        model.addAttribute("activeCount",   activeCount);
        model.addAttribute("totalSizes",    totalSizes);
        model.addAttribute("categoryCount", categoryCount);

        return "admin/brands/brand-list";
    }

    // ── CREATE: show form ─────────────────────────────────────────────
    @GetMapping("/create")
    public String createForm(Model model) {
        BrandFormDTO form = new BrandFormDTO();
        form.getSizes().add(new BrandFormDTO.SizeRow()); // one empty row by default

        model.addAttribute("brandFormDTO",     form);
        model.addAttribute("categories",       Brand.Category.values());
        model.addAttribute("subCategories",    Brand.SubCategory.values());
        model.addAttribute("packagingOptions", BrandSize.Packaging.values());
        model.addAttribute("roundingOptions",  BrandSize.MrpRounding.values());
        model.addAttribute("isEdit",           false);
        return "admin/brands/brand-form";
    }

    // ── CREATE: save ──────────────────────────────────────────────────
    @PostMapping("/new-with-sizes")
    public String saveWithSizes(@ModelAttribute BrandFormDTO brandFormDTO,
                                RedirectAttributes ra) {
        try {
            brandService.createBrandWithSizes(brandFormDTO);
            ra.addFlashAttribute("successMsg",
                    "Brand '" + brandFormDTO.getBrandName() + "' created successfully!");
            return "redirect:/admin/brands";
        } catch (RuntimeException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/admin/brands/create";
        }
    }

    // ── EDIT: show pre-populated form ─────────────────────────────────
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        BrandDTO dto = brandService.getBrandById(id);

        BrandFormDTO form = new BrandFormDTO();
        form.setId(dto.getId());
        form.setBrandCode(dto.getBrandCode());
        form.setBrandName(dto.getBrandName());
        form.setParentCompany(dto.getParentCompany());
        form.setCategory(dto.getCategory());
        form.setSubCategory(dto.getSubCategory());
        form.setExciseCode(dto.getExciseCode());
        form.setExciseCessPercent(dto.getExciseCessPercent());
        form.setTcsPercent(dto.getTcsPercent());
        form.setGstPercent(dto.getGstPercent());
        form.setActive(dto.isActive());

        if (dto.getSizes() != null) {
            dto.getSizes().forEach(s -> {
                BrandFormDTO.SizeRow row = new BrandFormDTO.SizeRow();
                row.setSizeLabel(s.getSizeLabel());
                row.setVolumeMl(s.getVolumeMl());
                row.setPackaging(s.getPackaging());
                row.setPurchasePrice(s.getPurchasePrice());
                row.setSellingPrice(s.getSellingPrice());
                row.setMrp(s.getMrp());
                row.setMrpRounding(s.getMrpRounding());
                row.setExciseCessPercent(s.getExciseCessPercent());
                row.setTcsPercent(s.getTcsPercent());
                row.setGstPercent(s.getGstPercent());
                row.setAbvPercent(s.getAbvPercent());
                row.setBarcode(s.getBarcode());
                row.setHsnCode(s.getHsnCode());
                row.setDisplayOrder(s.getDisplayOrder());
                row.setActive(s.isActive());

                // ── CASE FIELDS (new) — must be copied or edit
                //    form will show blank case pricing for existing sizes
                row.setBottlesPerCase(s.getBottlesPerCase());
                row.setCasePrice(s.getCasePrice());

                form.getSizes().add(row);
            });
        }

        model.addAttribute("brandFormDTO",     form);
        model.addAttribute("categories",       Brand.Category.values());
        model.addAttribute("subCategories",    Brand.SubCategory.values());
        model.addAttribute("packagingOptions", BrandSize.Packaging.values());
        model.addAttribute("roundingOptions",  BrandSize.MrpRounding.values());
        model.addAttribute("isEdit",           true);
        return "admin/brands/brand-form";
    }

    // ── EDIT: save ────────────────────────────────────────────────────
    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @ModelAttribute BrandFormDTO brandFormDTO,
                         RedirectAttributes ra) {
        try {
            brandService.updateBrandWithSizes(id, brandFormDTO);
            ra.addFlashAttribute("successMsg",
                    "Brand '" + brandFormDTO.getBrandName() + "' updated successfully!");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/admin/brands";
    }

    // ── DEACTIVATE ────────────────────────────────────────────────────
    @PostMapping("/{id}/delete")
    public String deactivate(@PathVariable Long id, RedirectAttributes ra) {
        try {
            brandService.deactivateBrand(id);
            ra.addFlashAttribute("successMsg", "Brand deactivated.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/admin/brands";
    }

    // ── Legacy redirect ───────────────────────────────────────────────
    @GetMapping("/new")
    public String legacyNew() {
        return "redirect:/admin/brands/create";
    }
}