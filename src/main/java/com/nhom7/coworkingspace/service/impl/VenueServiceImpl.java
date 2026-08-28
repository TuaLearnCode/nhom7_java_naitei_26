package com.nhom7.coworkingspace.service.impl;

import com.nhom7.coworkingspace.dto.request.VenueRequest;
import com.nhom7.coworkingspace.dto.response.AmenityResponse;
import com.nhom7.coworkingspace.dto.response.PageResponse;
import com.nhom7.coworkingspace.dto.response.SpaceResponse;
import com.nhom7.coworkingspace.dto.response.VenueDetailResponse;
import com.nhom7.coworkingspace.dto.response.VenueHostResponse;
import com.nhom7.coworkingspace.dto.response.VenueResponse;
import com.nhom7.coworkingspace.entity.Amenity;
import com.nhom7.coworkingspace.entity.Space;
import com.nhom7.coworkingspace.entity.User;
import com.nhom7.coworkingspace.entity.Venue;
import com.nhom7.coworkingspace.enums.SpaceStatus;
import com.nhom7.coworkingspace.enums.VenueStatus;
import com.nhom7.coworkingspace.exception.AppException;
import com.nhom7.coworkingspace.exception.VenueNotFoundException;
import com.nhom7.coworkingspace.mapper.SpaceMapper;
import com.nhom7.coworkingspace.mapper.VenueMapper;
import com.nhom7.coworkingspace.repository.AmenityRepository;
import com.nhom7.coworkingspace.repository.SpaceRepository;
import com.nhom7.coworkingspace.repository.UserRepository;
import com.nhom7.coworkingspace.repository.VenueRepository;
import com.nhom7.coworkingspace.service.VenueService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class VenueServiceImpl implements VenueService {

    private static final String HOST_ROLE = "HOST";

    private final VenueRepository venueRepository;
    private final AmenityRepository amenityRepository;
    private final UserRepository userRepository;
    private final SpaceRepository spaceRepository;
    private final VenueMapper venueMapper;
    private final SpaceMapper spaceMapper;

    @Override
    @Transactional
    public VenueResponse createVenue(VenueRequest request, String hostEmail) {
        User host = resolveHostUser(hostEmail);
        Set<Amenity> amenities = resolveAmenities(request.getAmenityIds());

        // A newly created venue always starts PENDING moderator review - never taken from the
        // client - so a HOST cannot self-approve (or self-block) their own venue on creation.
        Venue venue = Venue.builder()
                .owner(host)
                .name(request.getName())
                .description(request.getDescription())
                .address(request.getAddress())
                .city(request.getCity())
                .street(request.getStreet())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .status(VenueStatus.PENDING)
                .amenities(amenities)
                .deleted(false)
                .build();

        Venue savedVenue = venueRepository.save(venue);
        return venueMapper.toVenueResponse(savedVenue);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<VenueResponse> getMyVenues(String hostEmail, int page, int size) {
        User host = resolveHostUser(hostEmail);

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(Sort.Direction.DESC, "id"));
        Page<Venue> venuePage = venueRepository.findByOwnerIdAndDeletedFalse(host.getId(), pageable);

        return PageResponse.fromPage(venuePage.map(venueMapper::toVenueResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<VenueResponse> getAllVenues(int page, int size, VenueStatus status) {
        Pageable pageable = PageRequest.of(
                Math.max(0, page),
                Math.min(Math.max(1, size), 100),
                Sort.by(Sort.Direction.DESC, "id"));
        Page<Venue> venuePage = status == null
                ? venueRepository.findByDeletedFalse(pageable)
                : venueRepository.findByStatusAndDeletedFalse(status, pageable);
        return PageResponse.fromPage(venuePage.map(venueMapper::toVenueResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public VenueDetailResponse getVenueDetail(Long venueId) {
        Venue venue = getActiveVenueOrThrow(venueId);
        User owner = venue.getOwner();

        List<AmenityResponse> amenities = venue.getAmenities().stream()
                .map(venueMapper::toAmenityResponse)
                .sorted((left, right) -> left.getName().compareToIgnoreCase(right.getName()))
                .toList();

        List<SpaceResponse> spaces = spaceRepository.findByVenueId(venueId).stream()
                .map(spaceMapper::toSpaceResponse)
                .toList();

        VenueHostResponse host = VenueHostResponse.builder()
                .id(owner.getId())
                .name(owner.getName())
                .email(owner.getEmail())
                .phone(owner.getPhone())
                .status(owner.getStatus())
                .isIdentityVerified(owner.getIsIdentityVerified())
                .isBusinessVerified(owner.getIsBusinessVerified())
                .build();

        return VenueDetailResponse.builder()
                .id(venue.getId())
                .name(venue.getName())
                .description(venue.getDescription())
                .address(venue.getAddress())
                .city(venue.getCity())
                .street(venue.getStreet())
                .latitude(venue.getLatitude())
                .longitude(venue.getLongitude())
                .status(venue.getStatus())
                .blockReason(venue.getBlockReason())
                .host(host)
                .amenities(amenities)
                .spaces(spaces)
                .build();
    }

    @Override
    @Transactional
    public VenueResponse updateVenue(Long venueId, VenueRequest request, String hostEmail) {
        User host = resolveHostUser(hostEmail);
        Venue venue = getActiveVenueOrThrow(venueId);
        assertOwnership(venue, host);

        Set<Amenity> amenities = resolveAmenities(request.getAmenityIds());

        venue.setName(request.getName());
        venue.setDescription(request.getDescription());
        venue.setAddress(request.getAddress());
        venue.setCity(request.getCity());
        venue.setStreet(request.getStreet());
        venue.setLatitude(request.getLatitude());
        venue.setLongitude(request.getLongitude());
        venue.setAmenities(amenities);

        Venue savedVenue = venueRepository.save(venue);
        return venueMapper.toVenueResponse(savedVenue);
    }

    // Status is moderation-only: a HOST can never set it via createVenue/updateVenue, only
    // Moderator/Admin can, through this method (see ModeratorVenueController).
    @Override
    @Transactional
    public VenueResponse updateVenueStatus(
            Long venueId,
            VenueStatus newStatus,
            String reason,
            String moderatorEmail
    ) {
        Venue venue = getActiveVenueOrThrow(venueId);
        User moderator = userRepository.findByEmail(moderatorEmail)
                .orElseThrow(() -> new AppException("user.not.found", HttpStatus.NOT_FOUND));

        String normalizedReason = normalizeBlockReason(newStatus, reason);

        if (venue.getOwner().getId().equals(moderator.getId())) {
            throw new AppException("venue.cannot.moderate.self", HttpStatus.FORBIDDEN);
        }

        if (venue.getStatus() == newStatus) {
            return venueMapper.toVenueResponse(venue);
        }

        if (!isAllowedStatusTransition(venue.getStatus(), newStatus)) {
            throw new AppException("venue.status.transition.invalid", HttpStatus.BAD_REQUEST);
        }

        venue.setStatus(newStatus);
        venue.setBlockReason(normalizedReason);
        Venue savedVenue = venueRepository.save(venue);

        // Blocking a venue also takes its Spaces off the booking market, same as a soft delete;
        // unblocking (APPROVE) does NOT auto-reactivate them - a HOST/moderator may have had
        // other reasons for a given Space being inactive before the block.
        if (newStatus == VenueStatus.BLOCKED) {
            deactivateSpaces(venueId);
        }

        return venueMapper.toVenueResponse(savedVenue);
    }

    private String normalizeBlockReason(VenueStatus newStatus, String reason) {
        if (newStatus != VenueStatus.BLOCKED) {
            return null;
        }

        if (reason == null || reason.isBlank()) {
            throw new AppException("venue.block.reason.required", HttpStatus.BAD_REQUEST);
        }

        String normalizedReason = reason.trim();
        if (normalizedReason.length() > 500) {
            throw new AppException("venue.block.reason.size", HttpStatus.BAD_REQUEST);
        }

        return normalizedReason;
    }

    /**
     * A venue is approved exactly once after its initial review. It can subsequently be
     * blocked and re-approved, but must never return to the review queue.
     */
    private boolean isAllowedStatusTransition(VenueStatus currentStatus, VenueStatus newStatus) {
        return (currentStatus == VenueStatus.PENDING && newStatus == VenueStatus.APPROVE)
                || (currentStatus == VenueStatus.APPROVE && newStatus == VenueStatus.BLOCKED)
                || (currentStatus == VenueStatus.BLOCKED && newStatus == VenueStatus.APPROVE);
    }

    @Override
    @Transactional
    public void deleteVenue(Long venueId, String hostEmail) {
        User host = resolveHostUser(hostEmail);
        Venue venue = getActiveVenueOrThrow(venueId);
        assertOwnership(venue, host);

        venue.setDeleted(true);
        venueRepository.save(venue);

        deactivateSpaces(venueId);
    }

    // Spaces are kept (not deleted) so existing bookings/history stay intact; they are just
    // marked INACTIVE so the space's own booking-eligibility check (see BookingServiceImpl)
    // rejects new bookings once the parent venue is gone.
    private void deactivateSpaces(Long venueId) {
        List<Space> spaces = spaceRepository.findByVenueId(venueId);
        spaces.forEach(space -> space.setStatus(SpaceStatus.INACTIVE));
        spaceRepository.saveAll(spaces);
    }

    private User resolveHostUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("user.not.found", HttpStatus.NOT_FOUND));

        boolean isHost = user.getRoles().stream()
                .anyMatch(role -> HOST_ROLE.equalsIgnoreCase(role.getName()));
        if (!isHost) {
            throw new AppException("venue.host.required", HttpStatus.FORBIDDEN);
        }
        return user;
    }

    private Venue getActiveVenueOrThrow(Long venueId) {
        return venueRepository.findByIdAndDeletedFalse(venueId)
                .orElseThrow(VenueNotFoundException::new);
    }

    private void assertOwnership(Venue venue, User host) {
        if (!venue.getOwner().getId().equals(host.getId())) {
            throw new AppException("venue.access.denied", HttpStatus.FORBIDDEN);
        }
    }

    private Set<Amenity> resolveAmenities(Set<Long> amenityIds) {
        if (amenityIds == null || amenityIds.isEmpty()) {
            return new HashSet<>();
        }

        List<Amenity> foundAmenities = amenityRepository.findAllById(amenityIds);
        if (foundAmenities.size() != amenityIds.size()) {
            throw new AppException("amenity.not.found", HttpStatus.BAD_REQUEST);
        }

        return new HashSet<>(foundAmenities);
    }
}
