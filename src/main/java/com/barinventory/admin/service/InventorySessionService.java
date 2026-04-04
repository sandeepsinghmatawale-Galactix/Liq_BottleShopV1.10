package com.barinventory.admin.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.barinventory.admin.entity.*;
import com.barinventory.admin.enums.DistributionStatus;
import com.barinventory.admin.enums.SessionStatus;
import com.barinventory.admin.repository.*;
import com.barinventory.brands.repository.BrandSizeRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventorySessionService {

    // =========================================================
    // REPOSITORIES
    // =========================================================
    private final StockroomInventoryRepository stockroomRepository;
    private final WellInventoryRepository wellRepository;
    private final DistributionRecordRepository distributionRepository;
    private final InventorySessionRepository sessionRepository;
    private final SalesRecordRepository salesRepository;
    private final BrandSizeRepository brandSizeRepository;
    private final BarWellRepository barWellRepository;
    private final BarRepository barRepository;

    // =========================================================
    // SESSION MANAGEMENT
    // =========================================================

    /**
     * Get session with all related data (eager load to avoid N+1)
     */
    public InventorySession getSession(Long sessionId) {
        return sessionRepository.findByIdWithBar(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found with id: " + sessionId));
    }

    /**
     * Create new inventory session for a bar
     */
    @Transactional
    public InventorySession createSession(Long barId) {
        Bar bar = barRepository.findById(barId)
                .orElseThrow(() -> new RuntimeException("Bar not found: " + barId));

        InventorySession session = InventorySession.builder()
                .bar(bar)
                .status(SessionStatus.SETUP)
                .sessionStartTime(LocalDateTime.now())
                .build();

        return sessionRepository.save(session);
    }

    /**
     * Get the previous session (for reference closing stock)
     */
    public Optional<InventorySession> getPreviousSession(Long barId) {
        List<InventorySession> completedSessions = 
            sessionRepository.findCompletedSessionsByBar(barId);
        
        if (completedSessions.isEmpty()) {
            return Optional.empty();
        }
        
        // Return the first (latest) completed session
        return Optional.of(completedSessions.get(0));
    }

    /**
     * Get all active sessions for a bar
     */
    public List<InventorySession> getSessionsByBar(Long barId) {
        return sessionRepository.findByBarBarIdOrderBySessionStartTimeDesc(barId);
    }

    /**
     * Update session status
     */
    @Transactional
    public void updateSessionStatus(Long sessionId, SessionStatus status) {
        InventorySession session = getSession(sessionId);
        session.setStatus(status);
        sessionRepository.save(session);
    }

    // =========================================================
    // STOCKROOM OPERATIONS
    // =========================================================

    /**
     * Parse and save stockroom form data
     * Form pattern: brandSize_[id]_opening, brandSize_[id]_received, etc.
     */
    @Transactional
    public void saveStockroomFromForm(Long sessionId, Map<String, String> formData) {
        InventorySession session = getSession(sessionId);
        List<StockroomInventory> stockroomList = new ArrayList<>();

        // Group form data by brandSizeId
        Map<Long, Map<String, String>> groupedData = groupFormDataByKey(formData, "brandSize");

        for (Map.Entry<Long, Map<String, String>> entry : groupedData.entrySet()) {
            Long brandSizeId = entry.getKey();
            Map<String, String> data = entry.getValue();

            com.barinventory.brands.entity.BrandSize brandSize = 
                brandSizeRepository.findByIdAndActiveTrue(brandSizeId)
                    .orElseThrow(() -> new RuntimeException("BrandSize not found: " + brandSizeId));

            // Validate before creating
            BigDecimal opening = parseDecimal(data.get("opening"));
            BigDecimal received = parseDecimal(data.get("received"));
            BigDecimal closing = parseDecimal(data.get("closing"));
            BigDecimal transferred = parseDecimal(data.get("transferred"));

            validateStock(opening, received, closing);

            StockroomInventory stockroom = StockroomInventory.builder()
                    .session(session)
                    .brandSize(brandSize)
                    .openingStock(opening)
                    .receivedStock(received)
                    .closingStock(closing)
                    .transferredOut(transferred)
                    .build();

            stockroomList.add(stockroom);
        }

        saveStockroomInventory(sessionId, stockroomList);
    }

    /**
     * Save multiple stockroom records and create/update distribution
     */
    @Transactional
    public void saveStockroomInventory(Long sessionId, List<StockroomInventory> stockroomList) {
        InventorySession session = getSession(sessionId);

        for (StockroomInventory stockroom : stockroomList) {
            stockroom.setSession(session);

            // Default null values to zero
            if (stockroom.getOpeningStock() == null)
                stockroom.setOpeningStock(BigDecimal.ZERO);
            if (stockroom.getReceivedStock() == null)
                stockroom.setReceivedStock(BigDecimal.ZERO);
            if (stockroom.getClosingStock() == null)
                stockroom.setClosingStock(BigDecimal.ZERO);
            if (stockroom.getTransferredOut() == null)
                stockroom.setTransferredOut(BigDecimal.ZERO);

            stockroomRepository.save(stockroom);
        }

        // Auto-create/update distribution records
        createOrUpdateDistribution(sessionId);
    }

    /**
     * Get all stockroom records for a session
     */
    public List<StockroomInventory> getStockroomBySession(Long sessionId) {
        return stockroomRepository.findBySessionSessionId(sessionId);
    }

    // =========================================================
    // DISTRIBUTION OPERATIONS
    // =========================================================

    /**
     * Create or update distribution records based on stockroom transferred quantities
     */
    @Transactional
    public void createOrUpdateDistribution(Long sessionId) {
        InventorySession session = getSession(sessionId);
        List<StockroomInventory> stockrooms = getStockroomBySession(sessionId);

        for (StockroomInventory stockroom : stockrooms) {
            BigDecimal transferred = safe(stockroom.getTransferredOut());
            Long brandSizeId = stockroom.getBrandSize().getId();

            // Try to update existing distribution record
            int updated = distributionRepository.updateDistributionBulk(
                    sessionId,
                    brandSizeId,
                    transferred
            );

            // If no record exists, create new one
            if (updated == 0) {
                DistributionRecord dr = DistributionRecord.builder()
                        .session(session)
                        .brandSize(stockroom.getBrandSize())
                        .quantityFromStockroom(transferred)
                        .totalAllocated(BigDecimal.ZERO)
                        .unallocated(transferred)
                        .status(DistributionStatus.PENDING_ALLOCATION)
                                                .build();

                distributionRepository.save(dr);
            }
        }

        log.info("Distribution created/updated for session: {}", sessionId);
    }

    /**
     * Parse and save distribution allocations
     * Form pattern: dist_[brandSizeId]_well_[wellId]=[quantity]
     */
    @Transactional
    public void saveDistributionAllocations(Long sessionId, Map<String, String> formData) {
        InventorySession session = getSession(sessionId);

        // Group by brandSizeId, then by wellId
        Map<Long, Map<Long, BigDecimal>> allocations = new HashMap<>();

        for (Map.Entry<String, String> entry : formData.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (key.startsWith("dist_")) {
                // Format: dist_[brandSizeId]_well_[wellId]
                String[] parts = key.split("_");
                if (parts.length >= 4) {
                    try {
                        Long brandSizeId = Long.parseLong(parts[1]);
                        Long wellId = Long.parseLong(parts[3]);
                        BigDecimal qty = parseDecimal(value);

                        if (qty.compareTo(BigDecimal.ZERO) > 0) {
                            allocations.computeIfAbsent(brandSizeId, k -> new HashMap<>())
                                       .put(wellId, qty);
                        }
                    } catch (NumberFormatException e) {
                        log.warn("Failed to parse distribution form field: {}", key);
                    }
                }
            }
        }

        // Update distribution records with allocations
        for (Map.Entry<Long, Map<Long, BigDecimal>> entry : allocations.entrySet()) {
            Long brandSizeId = entry.getKey();
            Map<Long, BigDecimal> wellAllocations = entry.getValue();

            BigDecimal totalQty = wellAllocations.values().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            DistributionRecord dr = distributionRepository
                    .findBySessionSessionIdAndBrandSizeId(sessionId, brandSizeId)
                    .orElseThrow(() -> new RuntimeException(
                            "Distribution not found for brandSize: " + brandSizeId));

            dr.setTotalAllocated(totalQty);
            dr.setUnallocated(safe(dr.getQuantityFromStockroom()).subtract(totalQty));
            dr.setStatus(DistributionStatus.ALLOCATED);

            distributionRepository.save(dr);
        }

        log.info("Distribution allocations saved for session: {}", sessionId);
    }

    /**
     * Get all distributions for a session
     */
    public List<DistributionRecord> getDistributionsBySession(Long sessionId) {
        return distributionRepository.findBySessionSessionId(sessionId);
    }

    /**
     * Get distribution map (brandSizeId -> allocated quantity)
     */
    public Map<Long, BigDecimal> getDistributionMapForSession(Long sessionId) {
        return wellRepository.findBySessionSessionId(sessionId).stream()
                .filter(w -> w.getReceivedFromDistribution() != null &&
                        w.getReceivedFromDistribution().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.groupingBy(
                        w -> w.getBrandSize().getId(),
                        Collectors.mapping(
                                WellInventory::getReceivedFromDistribution,
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                        )
                ));
    }

    // =========================================================
    // WELL OPERATIONS
    // =========================================================

    /**
     * Parse and save well inventory form data
     * Form pattern: well_[wellId]_opening, well_[wellId]_received, etc.
     */
    @Transactional
    public void saveWellInventoryFromForm(Long sessionId, Map<String, String> formData) {
        InventorySession session = getSession(sessionId);
        List<WellInventory> wellList = new ArrayList<>();

        // Group form data by wellId
        Map<Long, Map<String, String>> groupedData = groupFormDataByKey(formData, "well");

        for (Map.Entry<Long, Map<String, String>> entry : groupedData.entrySet()) {
            Long wellId = entry.getKey();
            Map<String, String> data = entry.getValue();

            BarWell well = barWellRepository.findById(wellId)
                    .orElseThrow(() -> new RuntimeException("Well not found: " + wellId));

            BigDecimal opening = parseDecimal(data.get("opening"));
            BigDecimal received = parseDecimal(data.get("received"));
            BigDecimal closing = parseDecimal(data.get("closing"));

            WellInventory wellInv = WellInventory.builder()
                    .session(session)
                    .barWell(well)   // ✅ CLEAN RELATION
                    .openingStock(opening)
                    .receivedFromDistribution(received)
                    .closingStock(closing)
                    .build();

            wellList.add(wellInv);
        }

        saveWellInventory(sessionId, wellList);
    }

    /**
     * Save well inventory list
     */
    @Transactional
    public void saveWellInventory(Long sessionId, List<WellInventory> wells) {
        InventorySession session = getSession(sessionId);

        for (WellInventory well : wells) {
            well.setSession(session);

            if (well.getOpeningStock() == null)
                well.setOpeningStock(BigDecimal.ZERO);
            if (well.getReceivedFromDistribution() == null)
                well.setReceivedFromDistribution(BigDecimal.ZERO);
            if (well.getClosingStock() == null)
                well.setClosingStock(BigDecimal.ZERO);

            wellRepository.save(well);
        }

        log.info("Well inventory saved for session: {}", sessionId);
    }

    /**
     * Get all wells for a session
     */
    public List<WellInventory> getWellsBySession(Long sessionId) {
        return wellRepository.findBySessionSessionId(sessionId);
    }

    // =========================================================
    // DATA FETCH OPERATIONS
    // =========================================================

    /**
     * Get all active brand sizes
     */
    public List<com.barinventory.brands.entity.BrandSize> getAllActiveBrandSizes() {
        return brandSizeRepository.findAll().stream()
                .filter(bs -> bs.isActive())
                .collect(Collectors.toList());
    }

    /**
     * Get all well names for a bar
     */
    public List<String> getWellNamesForBar(Long barId) {
        return barWellRepository.findByBarBarIdAndActiveTrue(barId).stream()
                .map(BarWell::getWellName)
                .collect(Collectors.toList());
    }

    /**
     * Get all wells for a bar (with IDs)
     */
    public List<BarWell> getWellsByBar(Long barId) {
        return barWellRepository.findByBarBarIdAndActiveTrue(barId);
    }

    /**
     * Get previous session's closing stock for stockroom (reference)
     */
    public BigDecimal getPreviousClosingForStockroom(Long barId) {
        Optional<InventorySession> previousSession = getPreviousSession(barId);

        if (previousSession.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return stockroomRepository.findBySessionSessionId(previousSession.get().getSessionId())
                .stream()
                .map(s -> safe(s.getClosingStock()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Get previous session's closing stock for wells (reference)
     */
    public BigDecimal getPreviousClosingForWells(Long barId) {
        Optional<InventorySession> previousSession = getPreviousSession(barId);

        if (previousSession.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return wellRepository.findBySessionSessionId(previousSession.get().getSessionId())
                .stream()
                .map(w -> safe(w.getClosingStock()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Get product-wise summary for a bar within date range
     */
    public Map<String, Object> getProductWiseSummaryOptimized(
            Long barId,
            LocalDateTime startDate,
            LocalDateTime endDate) {

        List<Object[]> rows = salesRepository.getProductSummaryRaw(barId, startDate, endDate);

        Map<String, Map<String, Object>> result = new HashMap<>();

        for (Object[] r : rows) {
            String key = r[1] + " " + r[2]; // brandName + sizeLabel

            Map<String, Object> data = new HashMap<>();
            data.put("totalQuantity", r[3] != null ? r[3] : BigDecimal.ZERO);
            data.put("totalRevenue", r[4] != null ? r[4] : BigDecimal.ZERO);

            result.put(key, data);
        }

        return Map.of("productSummary", result);
    }

    // =========================================================
    // VALIDATION OPERATIONS
    // =========================================================

    /**
     * Validate that stockroom transferred = distribution quantity
     */
    public boolean validateStockroomToDistribution(Long sessionId, StringBuilder errors) {
        List<StockroomInventory> stockrooms = getStockroomBySession(sessionId);
        List<DistributionRecord> distributions = getDistributionsBySession(sessionId);

        for (StockroomInventory stockroom : stockrooms) {
            BigDecimal transferred = safe(stockroom.getTransferredOut());

            DistributionRecord distribution = distributions.stream()
                    .filter(d -> d.getBrandSize().getId()
                            .equals(stockroom.getBrandSize().getId()))
                    .findFirst()
                    .orElse(null);

            if (distribution == null && transferred.compareTo(BigDecimal.ZERO) > 0) {
                errors.append("BrandSize ").append(stockroom.getBrandSize().getId())
                        .append(": Missing distribution. ");
                return false;
            }

            if (distribution != null &&
                    transferred.compareTo(distribution.getQuantityFromStockroom()) != 0) {
                errors.append("BrandSize ").append(stockroom.getBrandSize().getId())
                        .append(": quantity mismatch. ");
                return false;
            }
        }

        return true;
    }

    /**
     * Validate that distribution allocated = wells received
     */
    public boolean validateDistributionToWells(Long sessionId, StringBuilder errors) {
        List<DistributionRecord> distributions = getDistributionsBySession(sessionId);

        for (DistributionRecord dr : distributions) {
            BigDecimal wellsTotal = wellRepository.sumReceivedBySessionAndBrandSize(
                    sessionId,
                    dr.getBrandSize().getId()
            );

            if (safe(dr.getTotalAllocated()).compareTo(safe(wellsTotal)) != 0) {
                errors.append("BrandSize ").append(dr.getBrandSize().getId())
                        .append(": allocation mismatch. ");
                return false;
            }
        }

        return true;
    }

    /**
     * Validate no unallocated stock remains
     */
    public boolean validateNoUnallocatedStock(Long sessionId, StringBuilder errors) {
        List<DistributionRecord> distributions = getDistributionsBySession(sessionId);

        for (DistributionRecord dr : distributions) {
            if (safe(dr.getUnallocated()).compareTo(BigDecimal.ZERO) > 0) {
                errors.append("BrandSize ").append(dr.getBrandSize().getId())
                        .append(": unallocated stock detected. ");
                return false;
            }
        }

        return true;
    }

    // =========================================================
    // HELPER METHODS
    // =========================================================

    /**
     * Group form data by key prefix
     * e.g., groupFormDataByKey(data, "brandSize") groups brandSize_1_opening, brandSize_1_received
     */
    private Map<Long, Map<String, String>> groupFormDataByKey(Map<String, String> formData, String prefix) {
        Map<Long, Map<String, String>> grouped = new HashMap<>();

        for (Map.Entry<String, String> entry : formData.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (key.startsWith(prefix + "_")) {
                String[] parts = key.split("_");
                if (parts.length >= 3) {
                    try {
                        Long id = Long.parseLong(parts[1]);
                        String field = parts[2];

                        grouped.computeIfAbsent(id, k -> new HashMap<>())
                               .put(field, value);
                    } catch (NumberFormatException e) {
                        log.warn("Failed to parse form field: {}", key);
                    }
                }
            }
        }

        return grouped;
    }

    /**
     * Safe null-to-zero conversion
     */
    private BigDecimal safe(BigDecimal val) {
        return val == null ? BigDecimal.ZERO : val;
    }

    /**
     * Parse string to BigDecimal, default to ZERO
     */
    private BigDecimal parseDecimal(String val) {
        if (val == null || val.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(val.trim());
        } catch (Exception e) {
            log.warn("Failed to parse decimal value: {}", val);
            return BigDecimal.ZERO;
        }
    }

    /**
     * Validate stock amounts (opening + received >= closing)
     */
    private void validateStock(BigDecimal opening, BigDecimal received, BigDecimal closing) {
        if (opening == null) opening = BigDecimal.ZERO;
        if (received == null) received = BigDecimal.ZERO;
        if (closing == null) closing = BigDecimal.ZERO;

        BigDecimal total = opening.add(received);

        if (closing.compareTo(total) > 0) {
            throw new RuntimeException("Closing stock cannot be greater than available stock");
        }

        if (opening.compareTo(BigDecimal.ZERO) < 0 ||
            received.compareTo(BigDecimal.ZERO) < 0 ||
            closing.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Stock values cannot be negative");
        }
    }
}