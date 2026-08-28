package com.nhom7.coworkingspace.controller.api;

import com.nhom7.coworkingspace.dto.request.UpdateVenueStatusRequest;
import com.nhom7.coworkingspace.dto.response.ApiResponse;
import com.nhom7.coworkingspace.dto.response.PageResponse;
import com.nhom7.coworkingspace.dto.response.VenueDetailResponse;
import com.nhom7.coworkingspace.dto.response.VenueResponse;
import com.nhom7.coworkingspace.enums.VenueStatus;
import com.nhom7.coworkingspace.service.VenueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;

@RestController
@RequestMapping("/api/moderator/venues")
@RequiredArgsConstructor
@Tag(name = "Moderator Venue API", description = "Endpoints for Moderator and Admin to moderate venues")
@SecurityRequirement(name = "BearerAuth")
public class ModeratorVenueController {

    private final VenueService venueService;
    private final MessageSource messageSource;

    @GetMapping
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    @Operation(summary = "List Venues", description = "Lists non-deleted venues with optional status filtering.")
    public ResponseEntity<ApiResponse<PageResponse<VenueResponse>>> listVenues(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) VenueStatus status) {
        PageResponse<VenueResponse> response = venueService.getAllVenues(page, size, status);
        String message = messageSource.getMessage("venue.list.success", null, LocaleContextHolder.getLocale());
        return ResponseEntity.ok(ApiResponse.success(response, message));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    @Operation(
            summary = "Get Venue Detail",
            description = "Returns venue information together with its host, amenities, and spaces."
    )
    public ResponseEntity<ApiResponse<VenueDetailResponse>> getVenueDetail(
            @PathVariable Long id
    ) {
        VenueDetailResponse response = venueService.getVenueDetail(id);
        String message = messageSource.getMessage(
                "venue.detail.success",
                null,
                LocaleContextHolder.getLocale()
        );
        return ResponseEntity.ok(ApiResponse.success(response, message));
    }

    // Approve or block a venue. A HOST can never reach this - status changes are moderator/admin only.
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    @Operation(
            summary = "Update Venue Status (Approve/Block)",
            description = "Allows Moderator or Admin to approve a PENDING venue, then switch it between APPROVE and BLOCKED."
    )
    public ResponseEntity<ApiResponse<VenueResponse>> updateVenueStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateVenueStatusRequest request,
            Authentication authentication) {
        VenueResponse response = venueService.updateVenueStatus(
                id,
                request.getStatus(),
                request.getReason(),
                authentication.getName()
        );
        Locale locale = LocaleContextHolder.getLocale();
        String message = messageSource.getMessage("venue.status.updated", null, locale);
        return ResponseEntity.ok(ApiResponse.success(response, message));
    }
}
