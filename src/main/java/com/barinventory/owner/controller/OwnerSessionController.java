package com.barinventory.owner.controller;

import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.barinventory.admin.entity.Bar;
import com.barinventory.admin.entity.InventorySession;
import com.barinventory.admin.entity.User;
import com.barinventory.admin.enums.SessionStatus;
import com.barinventory.admin.repository.BarRepository;
import com.barinventory.common.controller.BaseBarController;
import com.barinventory.common.service.InventorySessionService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/owner/sessions")
@RequiredArgsConstructor
@Slf4j
public class OwnerSessionController extends BaseBarController {

    private final InventorySessionService sessionService;
    private final BarRepository barRepository;

    // =========================================================
    // SESSIONS LIST
    // =========================================================

    @GetMapping("/list")
    public String listSessions(@AuthenticationPrincipal User user,
                               HttpSession httpSession,
                               Model model) {

        requireOwner(httpSession);
        Long barId = requireBar(httpSession);

        Bar bar = barRepository.findById(barId).orElseThrow();
        model.addAttribute("bar", bar);
        model.addAttribute("sessions", sessionService.getSessionsByBar(barId));

        log.info("[OWNER][SESSIONS][LIST] barId={} userId={} userName={}",
                barId,
                (user != null ? user.getId() : null),
                (user != null ? user.getName() : null));

        return "owner/sessions/list";
    }

    // =========================================================
    // CREATE NEW SESSION
    // =========================================================

    @GetMapping("/new")
    public String newSessionPage(@AuthenticationPrincipal User user,
                                 HttpSession httpSession,
                                 Model model) {

        requireOwner(httpSession);
        Long barId = requireBar(httpSession);

        Bar bar = barRepository.findById(barId).orElseThrow();
        model.addAttribute("bar", bar);

        log.info("[OWNER][SESSIONS][NEW][GET] barId={} userId={} userName={}",
                barId,
                (user != null ? user.getId() : null),
                (user != null ? user.getName() : null));

        return "owner/sessions/new";
    }

    @PostMapping("/new")
    public String createSession(@AuthenticationPrincipal User user,
                                HttpSession httpSession,
                                RedirectAttributes redirectAttributes) {
        try {
            requireOwner(httpSession);
            Long barId = requireBar(httpSession);

            log.info("[OWNER][SESSIONS][NEW][POST][START] barId={} userId={} userName={}",
                    barId,
                    (user != null ? user.getId() : null),
                    (user != null ? user.getName() : null));

            InventorySession created = sessionService.createSession(barId);

            log.info("[OWNER][SESSIONS][NEW][POST][OK] barId={} sessionId={} -> stockroom",
                    barId, created.getSessionId());

            return "redirect:/owner/sessions/" + created.getSessionId() + "/stockroom";
        } catch (Exception e) {
            log.error("[OWNER][SESSIONS][NEW][POST][FAIL] msg={}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/owner/sessions/list";
        }
    }

    // =========================================================
    // STOCKROOM PAGE
    // =========================================================

    @GetMapping("/{sessionId}/stockroom")
    public String stockroom(@PathVariable Long sessionId,
                            @AuthenticationPrincipal User user,
                            HttpSession httpSession,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        try {
            requireOwner(httpSession);
            Long barId = requireBar(httpSession);

            log.info("[OWNER][STOCKROOM][GET][START] sessionId={} barId={} userId={} userName={}",
                    sessionId,
                    barId,
                    (user != null ? user.getId() : null),
                    (user != null ? user.getName() : null));

            validateSessionAccess(sessionId, httpSession, sessionService);

            InventorySession invSession = sessionService.getSession(sessionId);

            // IMPORTANT: do NOT use model key "session" (Thymeleaf has built-in "session")
            model.addAttribute("invSession", invSession);
            model.addAttribute("brandSizes", sessionService.getAllActiveBrandSizes());

            log.info("[OWNER][STOCKROOM][GET][OK] sessionId={} status={} brandSizesCount={}",
                    sessionId,
                    invSession.getStatus(),
                    sessionService.getAllActiveBrandSizes().size());

            return "owner/sessions/stockroom";
        } catch (Exception ex) {
            log.error("[OWNER][STOCKROOM][GET][FAIL] sessionId={} msg={}", sessionId, ex.getMessage(), ex);
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/owner/sessions/list";
        }
    }

    @PostMapping("/{sessionId}/stockroom")
    public String saveStockroom(@PathVariable Long sessionId,
                                @AuthenticationPrincipal User user,
                                @RequestParam Map<String, String> formData,
                                HttpSession httpSession,
                                RedirectAttributes redirectAttributes) {
        try {
            requireOwner(httpSession);
            Long barId = requireBar(httpSession);

            log.info("[OWNER][STOCKROOM][POST][START] sessionId={} barId={} userId={} userName={} paramCount={} sampleKeys={}",
                    sessionId,
                    barId,
                    (user != null ? user.getId() : null),
                    (user != null ? user.getName() : null),
                    (formData != null ? formData.size() : 0),
                    (formData != null ? formData.keySet().stream().limit(15).toList() : null));

            validateSessionAccess(sessionId, httpSession, sessionService);

            sessionService.saveStockroomFromForm(sessionId, formData);

            log.info("[OWNER][STOCKROOM][POST][OK] sessionId={} -> distribution", sessionId);
            return "redirect:/owner/sessions/" + sessionId + "/distribution";
        } catch (Exception e) {
            log.error("[OWNER][STOCKROOM][POST][FAIL] sessionId={} msg={} paramCount={} sampleKeys={}",
                    sessionId,
                    e.getMessage(),
                    (formData != null ? formData.size() : 0),
                    (formData != null ? formData.keySet().stream().limit(15).toList() : null),
                    e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/owner/sessions/" + sessionId + "/stockroom";
        }
    }

    // =========================================================
    // DISTRIBUTION PAGE
    // =========================================================

    @GetMapping("/{sessionId}/distribution")
    public String distribution(@PathVariable Long sessionId,
                               @AuthenticationPrincipal User user,
                               HttpSession httpSession,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        try {
            requireOwner(httpSession);
            Long barId = requireBar(httpSession);

            log.info("[OWNER][DISTRIBUTION][GET][START] sessionId={} barId={} userId={} userName={}",
                    sessionId,
                    barId,
                    (user != null ? user.getId() : null),
                    (user != null ? user.getName() : null));

            validateSessionAccess(sessionId, httpSession, sessionService);

            InventorySession invSession = sessionService.getSession(sessionId);

            model.addAttribute("invSession", invSession);
            model.addAttribute("wells", sessionService.getWellsByBar(invSession.getBar().getBarId()));
            model.addAttribute("distributions", sessionService.getDistributionsBySession(sessionId));

            log.info("[OWNER][DISTRIBUTION][GET][OK] sessionId={} distributionsCount={} wellsCount={}",
                    sessionId,
                    sessionService.getDistributionsBySession(sessionId).size(),
                    sessionService.getWellsByBar(invSession.getBar().getBarId()).size());

            return "owner/sessions/distribution";
        } catch (Exception ex) {
            log.error("[OWNER][DISTRIBUTION][GET][FAIL] sessionId={} msg={}", sessionId, ex.getMessage(), ex);
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/owner/sessions/list";
        }
    }

    @PostMapping("/{sessionId}/distribution/save")
    public String saveDistribution(@PathVariable Long sessionId,
                                   @AuthenticationPrincipal User user,
                                   @RequestParam Map<String, String> formData,
                                   HttpSession httpSession,
                                   RedirectAttributes redirectAttributes) {
        try {
            requireOwner(httpSession);
            Long barId = requireBar(httpSession);

            log.info("[OWNER][DISTRIBUTION][POST][START] sessionId={} barId={} userId={} userName={} paramCount={} sampleKeys={}",
                    sessionId,
                    barId,
                    (user != null ? user.getId() : null),
                    (user != null ? user.getName() : null),
                    (formData != null ? formData.size() : 0),
                    (formData != null ? formData.keySet().stream().limit(15).toList() : null));

            validateSessionAccess(sessionId, httpSession, sessionService);

            sessionService.saveDistributionAllocations(sessionId, formData);

            log.info("[OWNER][DISTRIBUTION][POST][OK] sessionId={} -> wells", sessionId);
            return "redirect:/owner/sessions/" + sessionId + "/wells";
        } catch (Exception e) {
            log.error("[OWNER][DISTRIBUTION][POST][FAIL] sessionId={} msg={}", sessionId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/owner/sessions/" + sessionId + "/distribution";
        }
    }

    // =========================================================
    // WELLS PAGE
    // =========================================================

    @GetMapping("/{sessionId}/wells")
    public String wells(@PathVariable Long sessionId,
                        @AuthenticationPrincipal User user,
                        HttpSession httpSession,
                        Model model,
                        RedirectAttributes redirectAttributes) {
        try {
            requireOwner(httpSession);
            Long barId = requireBar(httpSession);

            log.info("[OWNER][WELLS][GET][START] sessionId={} barId={} userId={} userName={}",
                    sessionId,
                    barId,
                    (user != null ? user.getId() : null),
                    (user != null ? user.getName() : null));

            validateSessionAccess(sessionId, httpSession, sessionService);

            InventorySession invSession = sessionService.getSession(sessionId);

            model.addAttribute("invSession", invSession);
            model.addAttribute("wells", sessionService.getWellsByBar(invSession.getBar().getBarId()));
            model.addAttribute("brandSizes", sessionService.getAllActiveBrandSizes());
            model.addAttribute("wellInventories", sessionService.getWellsBySession(sessionId));

            log.info("[OWNER][WELLS][GET][OK] sessionId={} wellsCount={} brandSizesCount={} wellInvCount={}",
                    sessionId,
                    sessionService.getWellsByBar(invSession.getBar().getBarId()).size(),
                    sessionService.getAllActiveBrandSizes().size(),
                    sessionService.getWellsBySession(sessionId).size());

            return "owner/sessions/wells";
        } catch (Exception ex) {
            log.error("[OWNER][WELLS][GET][FAIL] sessionId={} msg={}", sessionId, ex.getMessage(), ex);
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/owner/sessions/list";
        }
    }

    @PostMapping("/{sessionId}/wells")
    public String saveWells(@PathVariable Long sessionId,
                            @AuthenticationPrincipal User user,
                            @RequestParam Map<String, String> formData,
                            HttpSession httpSession,
                            RedirectAttributes redirectAttributes) {
        try {
            requireOwner(httpSession);
            Long barId = requireBar(httpSession);

            log.info("[OWNER][WELLS][POST][START] sessionId={} barId={} userId={} userName={} paramCount={} sampleKeys={}",
                    sessionId,
                    barId,
                    (user != null ? user.getId() : null),
                    (user != null ? user.getName() : null),
                    (formData != null ? formData.size() : 0),
                    (formData != null ? formData.keySet().stream().limit(15).toList() : null));

            validateSessionAccess(sessionId, httpSession, sessionService);

            sessionService.saveWellInventoryFromForm(sessionId, formData);

            sessionService.updateSessionStatus(sessionId, SessionStatus.COMPLETED);

            log.info("[OWNER][WELLS][POST][OK] sessionId={} -> completed -> sessions list", sessionId);

            redirectAttributes.addFlashAttribute("success", "Session completed successfully!");
            return "redirect:/owner/sessions/list";
        } catch (Exception e) {
            log.error("[OWNER][WELLS][POST][FAIL] sessionId={} msg={}", sessionId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/owner/sessions/" + sessionId + "/wells";
        }
    }
}
