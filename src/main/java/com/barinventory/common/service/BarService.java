package com.barinventory.common.service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.barinventory.admin.dto.BarSubscriptionRequest;
import com.barinventory.admin.entity.Bar;
import com.barinventory.admin.entity.BarWell;
import com.barinventory.admin.entity.User;
import com.barinventory.admin.entity.UserBarAccess;
import com.barinventory.admin.enums.BarSubscriptionStatus;
import com.barinventory.admin.repository.BarRepository;
import com.barinventory.admin.repository.BarWellRepository;
import com.barinventory.admin.repository.UserBarAccessRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BarService {

	private final BarRepository barRepository;
	private final BarWellRepository barWellRepository;
	private final UserBarAccessRepository userBarAccessRepository;

	public List<Bar> getAllActiveBars() {
		return barRepository.findByActiveTrueAndHiddenOnPlatformFalseOrderByBarNameAsc();
	}

	public Bar getBarById(Long barId) {
		return barRepository.findById(barId).orElseThrow(() -> new RuntimeException("Bar not found"));
	}

	@Transactional
	public Bar createBar(Bar bar) {
		if (barRepository.existsByBarName(bar.getBarName())) {
			throw new RuntimeException("Bar with this name already exists");
		}

		initializeSubscriptionDefaults(bar);
		return barRepository.save(bar);
	}

	@Transactional
	public Bar updateBar(Long barId, Bar barDetails) {
		Bar bar = getBarById(barId);
		bar.setBarName(barDetails.getBarName());
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

		barWellRepository.deleteByBarBarId(barId);

		int count = Integer.parseInt(form.getOrDefault("wellCount", "1"));
		for (int i = 1; i <= count; i++) {
			String name = form.get("wellName_" + i);
			if (name != null && !name.isBlank()) {
				BarWell well = BarWell.builder().bar(bar).wellName(name.toUpperCase().replace(" ", "_")).active(true)
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
		normalizeSubscriptionState(bar);
		barRepository.save(bar);
	}

	public List<Bar> getBarsForUser(User user) {
		return userBarAccessRepository.findByUser(user).stream().filter(uba -> Boolean.TRUE.equals(uba.isActive())) // only
																													// active
																													// links
				.map(UserBarAccess::getBar).toList(); // use collect(...) if Java < 16
	}

	public List<Bar> getBarsForSelection(User user) {
		return user.getBarAccesses().stream().map(UserBarAccess::getBar).toList();
	}

	@Transactional
	public BarActivationResult activateBarSelection(User user, Long barId) {

		Bar bar = resolveAssignedBar(user, barId);
		BarSubscriptionStatus effectiveStatus = normalizeSubscriptionState(bar);

		ensureBarCanOperate(bar);

		System.out.println("👉 Bar: " + bar.getBarId());
		System.out.println("👉 Status: " + effectiveStatus);
		System.out.println("👉 Free Limit: " + bar.getFreeLoginLimit());
		System.out.println("👉 Free Used: " + bar.getFreeLoginUsed());

		if (effectiveStatus == BarSubscriptionStatus.ACTIVE) {
			return new BarActivationResult(barRepository.save(bar), false);
		}

		if (bar.hasRemainingFreeLogins()) {
			bar.setFreeLoginUsed(safeInt(bar.getFreeLoginUsed()) + 1);
			bar.setSubscriptionStatus(BarSubscriptionStatus.TRIAL);

			return new BarActivationResult(barRepository.save(bar), true);
		}

		throw new AccessDeniedException("Subscription expired for this bar. Please contact admin.");
	}

	@Transactional
	public Bar validateActiveBarSession(User user, Long activeBarId, boolean freeSessionGranted) {
		Bar bar = resolveAssignedBar(user, activeBarId);
		BarSubscriptionStatus effectiveStatus = normalizeSubscriptionState(bar);

		ensureBarCanOperate(bar);

		if (effectiveStatus == BarSubscriptionStatus.ACTIVE) {
			return bar;
		}

		if (freeSessionGranted) {
			return bar;
		}

		if (bar.hasRemainingFreeLogins()) {
			return bar;
		}

		throw new AccessDeniedException("Subscription expired for this bar. Please switch or contact admin.");
	}

	public void validateSelectedBarAccess(User user, Long activeBarId, Long requestedBarId,
			boolean freeSessionGranted) {
		if (!Objects.equals(activeBarId, requestedBarId)) {
			throw new AccessDeniedException("Please switch to the requested bar before continuing.");
		}

		validateActiveBarSession(user, requestedBarId, freeSessionGranted);
	}

	public void validateUserBarAccess(User user, Long barId) {
		resolveAssignedBar(user, barId);
	}

	@Transactional
	public Bar renewSubscription(Long barId, BarSubscriptionRequest request) {
		Bar bar = getBarById(barId);

		if (request.getSubscriptionStartDate() != null) {
			bar.setSubscriptionStartDate(request.getSubscriptionStartDate());
		} else if (bar.getSubscriptionStartDate() == null) {
			bar.setSubscriptionStartDate(LocalDate.now());
		}

		if (request.getSubscriptionEndDate() != null) {
			bar.setSubscriptionEndDate(request.getSubscriptionEndDate());
		}

		if (request.getFreeLoginLimit() != null) {
			bar.setFreeLoginLimit(request.getFreeLoginLimit());
		}

		if (Boolean.TRUE.equals(request.getResetFreeLogins())) {
			bar.setFreeLoginUsed(0);
		}

		if (request.getSubscriptionNotes() != null) {
			bar.setSubscriptionNotes(request.getSubscriptionNotes());
		}

		bar.setAdminBlocked(false);
		bar.setSubscriptionStatus(BarSubscriptionStatus.ACTIVE);
		validateSubscriptionConfiguration(bar);
		normalizeSubscriptionState(bar);
		return barRepository.save(bar);
	}

	@Transactional
	public Bar updateSubscription(Long barId, BarSubscriptionRequest request) {
		Bar bar = getBarById(barId);

		if (request.getSubscriptionStartDate() != null) {
			bar.setSubscriptionStartDate(request.getSubscriptionStartDate());
		}
		if (request.getSubscriptionEndDate() != null) {
			bar.setSubscriptionEndDate(request.getSubscriptionEndDate());
		}
		if (request.getFreeLoginLimit() != null) {
			bar.setFreeLoginLimit(request.getFreeLoginLimit());
		}
		if (request.getFreeLoginUsed() != null) {
			bar.setFreeLoginUsed(request.getFreeLoginUsed());
		}
		if (Boolean.TRUE.equals(request.getResetFreeLogins())) {
			bar.setFreeLoginUsed(0);
		}
		if (request.getHiddenOnPlatform() != null) {
			bar.setHiddenOnPlatform(request.getHiddenOnPlatform());
		}
		if (request.getAdminBlocked() != null) {
			bar.setAdminBlocked(request.getAdminBlocked());
		}
		if (request.getSubscriptionNotes() != null) {
			bar.setSubscriptionNotes(request.getSubscriptionNotes());
		}
		if (request.getSubscriptionStatus() != null) {
			bar.setSubscriptionStatus(request.getSubscriptionStatus());
		}

		validateSubscriptionConfiguration(bar);
		normalizeSubscriptionState(bar);
		return barRepository.save(bar);
	}

	@Transactional
	public Bar blockBar(Long barId, String notes) {
		Bar bar = getBarById(barId);
		bar.setAdminBlocked(true);
		bar.setSubscriptionStatus(BarSubscriptionStatus.BLOCKED);
		if (notes != null && !notes.isBlank()) {
			bar.setSubscriptionNotes(notes);
		}
		return barRepository.save(bar);
	}

	@Transactional
	public Bar unblockBar(Long barId) {
		Bar bar = getBarById(barId);
		bar.setAdminBlocked(false);
		normalizeSubscriptionState(bar);
		return barRepository.save(bar);
	}

	@Transactional
	public Bar hideBar(Long barId) {
		Bar bar = getBarById(barId);
		bar.setHiddenOnPlatform(true);
		return barRepository.save(bar);
	}

	@Transactional
	public Bar unhideBar(Long barId) {
		Bar bar = getBarById(barId);
		bar.setHiddenOnPlatform(false);
		return barRepository.save(bar);
	}

	@Transactional
	public int expireDueSubscriptions() {
		List<Bar> barsToExpire = barRepository.findBarsToExpire(LocalDate.now(),
				List.of(BarSubscriptionStatus.ACTIVE, BarSubscriptionStatus.TRIAL));

		for (Bar bar : barsToExpire) {
			if (!Boolean.TRUE.equals(bar.getAdminBlocked())) {
				bar.setSubscriptionStatus(BarSubscriptionStatus.EXPIRED);
			}
		}

		if (!barsToExpire.isEmpty()) {
			barRepository.saveAll(barsToExpire);
		}

		return barsToExpire.size();
	}

	private Bar resolveAssignedBar(User user, Long barId) {
		if (user.isAdmin()) {
			return getBarById(barId);
		}

		return user.getBarAccesses().stream().filter(UserBarAccess::isActive).map(UserBarAccess::getBar)
				.filter(bar -> bar.getBarId().equals(barId)).findFirst()
				.orElseThrow(() -> new AccessDeniedException("No access to this bar."));
	}

	private void ensureBarCanOperate(Bar bar) {
		if (Boolean.TRUE.equals(bar.getHiddenOnPlatform())) {
			throw new AccessDeniedException("This bar is hidden on the platform.");
		}

		if (Boolean.TRUE.equals(bar.getAdminBlocked())
				|| bar.getSubscriptionStatus() == BarSubscriptionStatus.BLOCKED) {
			throw new AccessDeniedException("This bar is blocked by admin.");
		}

		if (!Boolean.TRUE.equals(bar.getActive()) || !Boolean.TRUE.equals(bar.getSetupComplete())) {
			throw new AccessDeniedException("This bar is not activated yet.");
		}
	}

	private void initializeSubscriptionDefaults(Bar bar) {
		if (bar.getSubscriptionStatus() == null) {
			bar.setSubscriptionStatus(BarSubscriptionStatus.TRIAL);
		}
		if (bar.getFreeLoginLimit() == null) {
			bar.setFreeLoginLimit(0);
		}
		if (bar.getFreeLoginUsed() == null) {
			bar.setFreeLoginUsed(0);
		}
		if (bar.getAdminBlocked() == null) {
			bar.setAdminBlocked(false);
		}
		if (bar.getHiddenOnPlatform() == null) {
			bar.setHiddenOnPlatform(false);
		}
		validateSubscriptionConfiguration(bar);
	}

	private void validateSubscriptionConfiguration(Bar bar) {
		int freeLoginLimit = safeInt(bar.getFreeLoginLimit());
		int freeLoginUsed = safeInt(bar.getFreeLoginUsed());

		if (freeLoginLimit < 0) {
			throw new IllegalArgumentException("Free login limit cannot be negative.");
		}
		if (freeLoginUsed < 0) {
			throw new IllegalArgumentException("Free login used cannot be negative.");
		}
		if (bar.getSubscriptionStartDate() != null && bar.getSubscriptionEndDate() != null
				&& bar.getSubscriptionEndDate().isBefore(bar.getSubscriptionStartDate())) {
			throw new IllegalArgumentException("Subscription end date cannot be before the start date.");
		}

		bar.setFreeLoginLimit(freeLoginLimit);
		bar.setFreeLoginUsed(Math.min(freeLoginUsed, freeLoginLimit));
	}

	private BarSubscriptionStatus normalizeSubscriptionState(Bar bar) {
		BarSubscriptionStatus normalized = determineSubscriptionState(bar, LocalDate.now());
		if (bar.getSubscriptionStatus() != normalized) {
			bar.setSubscriptionStatus(normalized);
		}
		return normalized;
	}

	private BarSubscriptionStatus determineSubscriptionState(Bar bar, LocalDate today) {
		if (Boolean.TRUE.equals(bar.getAdminBlocked())) {
			return BarSubscriptionStatus.BLOCKED;
		}

		if (bar.getSubscriptionEndDate() != null) {
			if (!bar.getSubscriptionEndDate().isBefore(today)) {
				return BarSubscriptionStatus.ACTIVE;
			}
			return bar.hasRemainingFreeLogins() ? BarSubscriptionStatus.EXPIRED : BarSubscriptionStatus.EXPIRED;
		}

		if (bar.getSubscriptionStatus() == BarSubscriptionStatus.ACTIVE) {
			return BarSubscriptionStatus.ACTIVE;
		}

		if (bar.hasRemainingFreeLogins()) {
			return BarSubscriptionStatus.TRIAL;
		}

		return BarSubscriptionStatus.EXPIRED;
	}

	private int safeInt(Integer value) {
		return value == null ? 0 : value;
	}

	public record BarActivationResult(Bar bar, boolean usedFreeLogin) {
	}

	public List<Bar> getActiveBarsForUser(User user) {

	    if (user == null) {
	        return Collections.emptyList();
	    }

	    return userBarAccessRepository
	            .findByUser_IdAndActiveTrue(user.getId())
	            .stream()
	            .map(UserBarAccess::getBar)
	            .filter(Objects::nonNull)

	            // ✅ Correct for Boolean field
	            .filter(bar -> Boolean.TRUE.equals(bar.getActive()))

	            .distinct()
	            .collect(Collectors.toList());
	}

}
