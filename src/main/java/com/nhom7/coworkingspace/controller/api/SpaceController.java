package com.nhom7.coworkingspace.controller.api;

import com.nhom7.coworkingspace.dto.request.AddSpaceManagerRequest;
import com.nhom7.coworkingspace.dto.request.SpaceCreateRequest;
import com.nhom7.coworkingspace.dto.request.SpaceSearchRequest;
import com.nhom7.coworkingspace.dto.request.SpaceUpdateRequest;
import com.nhom7.coworkingspace.dto.response.ApiResponse;
import com.nhom7.coworkingspace.dto.response.PageResponse;
import com.nhom7.coworkingspace.dto.response.SpaceResponse;
import com.nhom7.coworkingspace.service.SpaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Validated
@Tag(name = "Space API", description = "Endpoints for Co-working Space search and management")
@SecurityRequirement(name = "BearerAuth")
public class SpaceController {

    private final SpaceService spaceService;
    private final MessageSource messageSource;

    /**
     * Search and filter co-working spaces.
     */
    @GetMapping("/spaces/search")
    @PreAuthorize("hasAnyRole('USER', 'HOST', 'MODERATOR', 'ADMIN')")
    @Operation(summary = "Search & Filter Co-working spaces", description = "Allows users to search and filter co-working spaces by name, address, type, price, and availability time.")
    public ResponseEntity<ApiResponse<PageResponse<SpaceResponse>>> searchSpaces(
            @ParameterObject @Valid @ModelAttribute SpaceSearchRequest request) {
        PageResponse<SpaceResponse> result = spaceService.searchSpaces(request);
        String message = resolveMessage("space.list.fetched");
        return ResponseEntity.ok(ApiResponse.success(result, message));
    }

    /**
     * Create a new space inside a venue owned by the host.
     */
    @PostMapping("/venues/{venueId}/spaces")
    @PreAuthorize("hasRole('HOST')")
    @Operation(summary = "Create Space in Venue", description = "Allows an authenticated HOST to add a new space to their venue.")
    public ResponseEntity<ApiResponse<SpaceResponse>> createSpace(
            @Parameter(example = "13") @Positive(message = "{validation.id.positive}") @PathVariable Long venueId,
            @Valid @RequestBody SpaceCreateRequest request,
            Authentication authentication
    ) {
        SpaceResponse response = spaceService.createSpace(venueId, request, authentication.getName());
        String message = resolveMessage("space.created");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), message, response));
    }

    /**
     * List spaces owned or managed by the currently authenticated HOST.
     */
    @GetMapping("/spaces/my-spaces")
    @PreAuthorize("hasRole('HOST')")
    @Operation(summary = "List My Spaces", description = "Allows an authenticated HOST to retrieve paginated list of their spaces.")
    public ResponseEntity<ApiResponse<PageResponse<SpaceResponse>>> getMySpaces(
            @Min(value = 0, message = "{validation.page.min}")
            @RequestParam(defaultValue = "0") int page,
            @Min(value = 1, message = "{validation.size.min}")
            @Max(value = 100, message = "{validation.size.max}")
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication
    ) {
        PageResponse<SpaceResponse> response = spaceService.getMySpaces(authentication.getName(), page, size);
        String message = resolveMessage("space.list.fetched");
        return ResponseEntity.ok(ApiResponse.success(response, message));
    }

    /**
     * List spaces inside a specific venue.
     */
    @GetMapping("/venues/{venueId}/spaces")
    @PreAuthorize("hasAnyRole('USER', 'HOST', 'MODERATOR', 'ADMIN')")
    @Operation(summary = "List Spaces in Venue", description = "Retrieve paginated list of spaces inside a specific venue.")
    public ResponseEntity<ApiResponse<PageResponse<SpaceResponse>>> getSpacesByVenue(
            @Positive(message = "{validation.id.positive}") @PathVariable Long venueId,
            @Min(value = 0, message = "{validation.page.min}")
            @RequestParam(defaultValue = "0") int page,
            @Min(value = 1, message = "{validation.size.min}")
            @Max(value = 100, message = "{validation.size.max}")
            @RequestParam(defaultValue = "10") int size
    ) {
        PageResponse<SpaceResponse> response = spaceService.getSpacesByVenue(venueId, page, size);
        String message = resolveMessage("space.list.fetched");
        return ResponseEntity.ok(ApiResponse.success(response, message));
    }

    /**
     * Update details of a space owned or managed by the host.
     */
    @PutMapping("/spaces/{id}")
    @PreAuthorize("hasRole('HOST')")
    @Operation(summary = "Update Space", description = "Allows an authenticated HOST to update details of their space.")
    public ResponseEntity<ApiResponse<SpaceResponse>> updateSpace(
            @Positive(message = "{validation.id.positive}") @PathVariable Long id,
            @Valid @RequestBody SpaceUpdateRequest request,
            Authentication authentication
    ) {
        SpaceResponse response = spaceService.updateSpace(id, request, authentication.getName());
        String message = resolveMessage("space.updated");
        return ResponseEntity.ok(ApiResponse.success(response, message));
    }

    /**
     * Delete a space owned or managed by the host.
     */
    @DeleteMapping("/spaces/{id}")
    @PreAuthorize("hasRole('HOST')")
    @Operation(summary = "Delete Space", description = "Allows an authenticated HOST to delete their space.")
    public ResponseEntity<ApiResponse<Void>> deleteSpace(
            @Positive(message = "{validation.id.positive}") @PathVariable Long id,
            Authentication authentication
    ) {
        spaceService.deleteSpace(id, authentication.getName());
        String message = resolveMessage("space.deleted");
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), message, null));
    }

    /**
     * Add a manager (host) to manage a space.
     */
    @PostMapping("/spaces/{id}/managers")
    @PreAuthorize("hasRole('HOST')")
    @Operation(summary = "Add Manager to Space", description = "Allows an authenticated HOST to assign a manager (user) to a space.")
    public ResponseEntity<ApiResponse<SpaceResponse>> addManagerToSpace(
            @Positive(message = "{validation.id.positive}") @PathVariable Long id,
            @Valid @RequestBody AddSpaceManagerRequest request,
            Authentication authentication
    ) {
        SpaceResponse response = spaceService.addManagerToSpace(id, request, authentication.getName());
        String message = resolveMessage("space.manager.added");
        return ResponseEntity.ok(ApiResponse.success(response, message));
    }

    private String resolveMessage(String key) {
        Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(key, null, locale);
    }
}
