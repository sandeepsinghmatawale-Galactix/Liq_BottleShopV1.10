package com.barinventory.admin.service;

import java.util.List;
import java.util.Map;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.barinventory.admin.entity.Bar;
import com.barinventory.admin.entity.BarWell;
import com.barinventory.admin.entity.User;
import com.barinventory.admin.entity.UserBarAccess;
import com.barinventory.admin.repository.BarRepository;
import com.barinventory.admin.repository.BarWellRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BarService {
    
    private final BarRepository barRepository;
    private final BarWellRepository barWellRepository;
    
    public List<Bar> getAllActiveBars() {
        return barRepository.findByActiveTrue();
    }
    
    public Bar getBarById(Long barId) {
        return barRepository.findById(barId)
            .orElseThrow(() -> new RuntimeException("Bar not found"));
    }
    
    @Transactional
    public Bar createBar(Bar bar) {
        if (barRepository.existsByBarName(bar.getBarName())) {
            throw new RuntimeException("Bar with this name already exists");
        }
        return barRepository.save(bar);
    }
    
    @Transactional
    public Bar updateBar(Long barId, Bar barDetails) {
        Bar bar = getBarById(barId);
        bar.setBarName(barDetails.getBarName());
       // bar.setLocation(barDetails.getLocation());
        bar.setContactNumber(barDetails.getContactNumber());
        bar.setOwnerName(barDetails.getOwnerName());
        return barRepository.save(bar);
    }
    
    @Transactional
    public void deactivateBar(Long barId) {
        Bar bar = getBarById(barId);
        bar.setActive(false);
        barRepository.save(bar);
    }
    
    @Transactional
    public void saveWellsConfig(Long barId, Map<String, String> form) {
        Bar bar = getBarById(barId);

        // Delete old well config for this bar before re-saving
        barWellRepository.deleteByBarBarId(barId);

        int count = Integer.parseInt(form.getOrDefault("wellCount", "1"));
        for (int i = 1; i <= count; i++) {
            String name = form.get("wellName_" + i);
            if (name != null && !name.isBlank()) {
                BarWell well = BarWell.builder()
                    .bar(bar)
                    .wellName(name.toUpperCase().replace(" ", "_"))
                    .active(true)
                    .build();
                barWellRepository.save(well);
            }
        }
    }

    @Transactional
    public void updateOnboardingStep(Long barId, String step) {
        Bar bar = getBarById(barId);
        bar.setOnboardingStep(step);
        barRepository.save(bar);
    }

    @Transactional
    public void activateBar(Long barId) {
        Bar bar = getBarById(barId);
        bar.setActive(true);
        bar.setSetupComplete(true);
        bar.setOnboardingStep("COMPLETE");
        barRepository.save(bar);
    }
    
 // All bars a specific user owns/has access to
    @Transactional
    public List<Bar> getBarsForUser(User user) {
        return user.getBarAccesses().stream()
                   .filter(UserBarAccess::isActive)
                   .map(UserBarAccess::getBar)
                   .toList();
    }
    // Validate bar belongs to user — call this on EVERY request
    public void validateUserBarAccess(User user, Long barId) {
        if (user.isAdmin()) return;

        boolean hasAccess = user.getBarAccesses().stream()
            .anyMatch(a -> a.isActive() &&
                           a.getBar().getBarId().equals(barId));

        if (!hasAccess) {
            throw new AccessDeniedException("No access to this bar");
        }
    }
}
