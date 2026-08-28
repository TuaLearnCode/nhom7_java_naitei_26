package com.nhom7.coworkingspace.service;

import com.nhom7.coworkingspace.dto.request.VenueRequest;
import com.nhom7.coworkingspace.dto.response.PageResponse;
import com.nhom7.coworkingspace.dto.response.VenueDetailResponse;
import com.nhom7.coworkingspace.dto.response.VenueResponse;
import com.nhom7.coworkingspace.enums.VenueStatus;

public interface VenueService {

    /**
     * Create a venue owned by the currently authenticated HOST.
     *
     * @param request   venue creation payload
     * @param hostEmail email of the authenticated user (from SecurityContext)
     * @return created venue details
     */
    VenueResponse createVenue(VenueRequest request, String hostEmail);

    /**
     * List all non-deleted venues owned by the currently authenticated HOST.
     *
     * @param hostEmail email of the authenticated user (from SecurityContext)
     * @param page      zero-based page index
     * @param size      page size
     * @return paginated venue responses
     */
    PageResponse<VenueResponse> getMyVenues(String hostEmail, int page, int size);

    /** List non-deleted venues for moderator/admin management. */
    PageResponse<VenueResponse> getAllVenues(int page, int size, VenueStatus status);

    /** Get a complete venue moderation view including host, amenities, and spaces. */
    VenueDetailResponse getVenueDetail(Long venueId);

    /**
     * Update a venue owned by the currently authenticated HOST.
     *
     * @param venueId   id of the venue to update
     * @param request   updated venue payload
     * @param hostEmail email of the authenticated user (from SecurityContext)
     * @return updated venue details
     */
    VenueResponse updateVenue(Long venueId, VenueRequest request, String hostEmail);

    /**
     * Approve or block a venue (moderation). Only Moderator/Admin can call this; a HOST can
     * never set their own venue's status via createVenue/updateVenue.
     *
     * @param venueId        id of the venue to moderate
     * @param newStatus      new moderation status
     * @param moderatorEmail email of the authenticated moderator/admin (from SecurityContext)
     * @return updated venue details
     */
    VenueResponse updateVenueStatus(
            Long venueId,
            VenueStatus newStatus,
            String reason,
            String moderatorEmail
    );

    /**
     * Soft delete a venue owned by the currently authenticated HOST.
     *
     * @param venueId   id of the venue to delete
     * @param hostEmail email of the authenticated user (from SecurityContext)
     */
    void deleteVenue(Long venueId, String hostEmail);
}
