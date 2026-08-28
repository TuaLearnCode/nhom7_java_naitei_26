package com.nhom7.coworkingspace.controller.web;

import com.nhom7.coworkingspace.enums.VenueStatus;
import com.nhom7.coworkingspace.exception.AppException;
import com.nhom7.coworkingspace.service.VenueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Locale;

@Slf4j
@Controller
@RequestMapping("/moderator/venues")
@RequiredArgsConstructor
public class ModeratorVenueWebController {

    private final VenueService venueService;
    private final MessageSource messageSource;

    @GetMapping
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public String listVenues(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) VenueStatus status,
            Model model) {
        model.addAttribute("venues", venueService.getAllVenues(page, size, status));
        model.addAttribute("statuses", VenueStatus.values());
        model.addAttribute("selectedStatus", status);
        return "moderator/venues";
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public String updateVenueStatus(
            @PathVariable Long id,
            @RequestParam VenueStatus status,
            @RequestParam(required = false) String reason,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        Locale locale = LocaleContextHolder.getLocale();
        try {
            venueService.updateVenueStatus(id, status, reason, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage",
                    messageSource.getMessage("venue.status.updated", null, locale));
        } catch (AppException ex) {
            log.warn("[ModeratorVenueWebController] Failed to update venue status (id={}): {}",
                    id, ex.getMessageKey());
            redirectAttributes.addFlashAttribute("errorMessage",
                    messageSource.getMessage(ex.getMessageKey(), null, ex.getMessageKey(), locale));
        } catch (Exception ex) {
            log.error("[ModeratorVenueWebController] Unexpected error updating venue status (id={})", id, ex);
            redirectAttributes.addFlashAttribute("errorMessage",
                    messageSource.getMessage("common.error", null, locale));
        }
        return "redirect:/moderator/venues";
    }
}
