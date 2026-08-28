package com.nhom7.coworkingspace.service.impl;

import com.nhom7.coworkingspace.dto.request.AddSpaceManagerRequest;
import com.nhom7.coworkingspace.dto.request.SpaceCreateRequest;
import com.nhom7.coworkingspace.dto.request.SpaceSearchRequest;
import com.nhom7.coworkingspace.dto.request.SpaceUpdateRequest;
import com.nhom7.coworkingspace.dto.response.PageResponse;
import com.nhom7.coworkingspace.dto.response.SpaceResponse;
import com.nhom7.coworkingspace.entity.Space;
import com.nhom7.coworkingspace.entity.User;
import com.nhom7.coworkingspace.entity.Venue;
import com.nhom7.coworkingspace.enums.PriceUnit;
import com.nhom7.coworkingspace.enums.SpaceStatus;
import com.nhom7.coworkingspace.exception.AppException;
import com.nhom7.coworkingspace.mapper.SpaceMapper;
import com.nhom7.coworkingspace.repository.SpaceRepository;
import com.nhom7.coworkingspace.repository.UserRepository;
import com.nhom7.coworkingspace.repository.VenueRepository;
import com.nhom7.coworkingspace.service.SpaceService;
import com.nhom7.coworkingspace.specification.SpaceSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class SpaceServiceImpl implements SpaceService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "name", "price", "createdAt", "type", "priceUnit");

    private final SpaceRepository spaceRepository;
    private final VenueRepository venueRepository;
    private final UserRepository userRepository;
    private final SpaceMapper spaceMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SpaceResponse> searchSpaces(SpaceSearchRequest request) {
        log.debug("[SpaceService] Searching spaces with params: name={}, city={}, type={}, priceUnit={}",
                request.getName(), request.getCity(), request.getType(), request.getPriceUnit());

        validateSearchRequest(request);

        Sort.Direction direction = Sort.Direction.fromString(request.getSortDir());
        String sortBy = request.getSortBy().trim();
        int page = request.getPage();
        int size = request.getSize();

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Specification<Space> spec = SpaceSpecification.buildSearchSpecification(request);
        Page<Space> spacePage = spaceRepository.findAll(spec, pageable);

        Page<SpaceResponse> dtoPage = spacePage.map(spaceMapper::toSpaceResponse);
        return PageResponse.fromPage(dtoPage);
    }

    @Override
    @Transactional
    public SpaceResponse createSpace(Long venueId, SpaceCreateRequest request, String hostEmail) {
        Venue venue = venueRepository.findByIdAndDeletedFalse(venueId)
                .orElseThrow(() -> new AppException("venue.not.found", HttpStatus.NOT_FOUND));

        if (!venue.getOwner().getEmail().equals(hostEmail)) {
            throw new AppException("venue.access.denied", HttpStatus.FORBIDDEN);
        }

        if (venue.getStatus() != com.nhom7.coworkingspace.enums.VenueStatus.APPROVE) {
            throw new AppException("venue.not.approved", HttpStatus.BAD_REQUEST);
        }

        if (request.getOpenTime() != null && request.getCloseTime() != null
                && !request.getOpenTime().isBefore(request.getCloseTime())) {
            throw new AppException("booking.operating.hours.invalid", HttpStatus.BAD_REQUEST);
        }

        PriceUnit priceUnit = parsePriceUnit(request.getPriceUnit());

        Space space = Space.builder()
                .venue(venue)
                .name(request.getName())
                .type(request.getType() != null ? request.getType().trim() : null)
                .capacity(request.getCapacity())
                .description(request.getDescription())
                .price(request.getPrice())
                .priceUnit(priceUnit.name().toLowerCase(Locale.ROOT))
                .openTime(request.getOpenTime())
                .closeTime(request.getCloseTime())
                .status(SpaceStatus.ACTIVE)
                .hosts(new HashSet<>(Set.of(venue.getOwner())))
                .build();

        Space savedSpace = spaceRepository.save(space);
        log.info("[SpaceService] Created space id={} for venueId={} by host={}", savedSpace.getId(), venueId, hostEmail);
        return spaceMapper.toSpaceResponse(savedSpace);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SpaceResponse> getMySpaces(String hostEmail, int page, int size) {
        validatePageRequest(page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));

        Page<Space> spacePage = spaceRepository.findMySpaces(hostEmail, pageable);
        Page<SpaceResponse> dtoPage = spacePage.map(spaceMapper::toSpaceResponse);
        return PageResponse.fromPage(dtoPage);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SpaceResponse> getSpacesByVenue(Long venueId, int page, int size) {
        venueRepository.findByIdAndDeletedFalse(venueId)
                .orElseThrow(() -> new AppException("venue.not.found", HttpStatus.NOT_FOUND));

        validatePageRequest(page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));

        Page<Space> spacePage = spaceRepository.findByVenueIdAndVenueDeletedFalse(venueId, pageable);
        Page<SpaceResponse> dtoPage = spacePage.map(spaceMapper::toSpaceResponse);
        return PageResponse.fromPage(dtoPage);
    }

    @Override
    @Transactional
    public SpaceResponse updateSpace(Long id, SpaceUpdateRequest request, String hostEmail) {
        Space space = spaceRepository.findById(id)
                .orElseThrow(() -> new AppException("space.not.found", HttpStatus.NOT_FOUND));

        checkSpaceAndHostAuthorization(space, hostEmail);

        if (request.getOpenTime() != null && request.getCloseTime() != null
                && !request.getOpenTime().isBefore(request.getCloseTime())) {
            throw new AppException("booking.operating.hours.invalid", HttpStatus.BAD_REQUEST);
        }

        PriceUnit priceUnit = parsePriceUnit(request.getPriceUnit());

        space.setName(request.getName());
        space.setType(request.getType() != null ? request.getType().trim() : space.getType());
        space.setCapacity(request.getCapacity());
        space.setDescription(request.getDescription());
        space.setPrice(request.getPrice());
        space.setPriceUnit(priceUnit.name().toLowerCase(Locale.ROOT));
        space.setOpenTime(request.getOpenTime());
        space.setCloseTime(request.getCloseTime());
        if (request.getStatus() != null) {
            space.setStatus(request.getStatus());
        }

        Space updatedSpace = spaceRepository.save(space);
        log.info("[SpaceService] Updated space id={} by host={}", updatedSpace.getId(), hostEmail);
        return spaceMapper.toSpaceResponse(updatedSpace);
    }

    @Override
    @Transactional
    public SpaceResponse addManagerToSpace(Long spaceId, AddSpaceManagerRequest request, String hostEmail) {
        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new AppException("space.not.found", HttpStatus.NOT_FOUND));

        checkSpaceAndHostAuthorization(space, hostEmail);

        User manager = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new AppException("user.not.found", HttpStatus.NOT_FOUND));

        boolean isHost = manager.getRoles() != null && manager.getRoles().stream()
                .anyMatch(role -> "HOST".equalsIgnoreCase(role.getName()));
        if (!isHost) {
            throw new AppException("space.manager.host.required", HttpStatus.BAD_REQUEST);
        }

        space.getHosts().add(manager);
        Space updatedSpace = spaceRepository.save(space);
        log.info("[SpaceService] Added manager userId={} to space id={} by host={}", request.getUserId(), spaceId, hostEmail);
        return spaceMapper.toSpaceResponse(updatedSpace);
    }

    @Override
    @Transactional
    public void deleteSpace(Long id, String hostEmail) {
        Space space = spaceRepository.findById(id)
                .orElseThrow(() -> new AppException("space.not.found", HttpStatus.NOT_FOUND));

        checkSpaceAndHostAuthorization(space, hostEmail);

        spaceRepository.delete(space);
        log.info("[SpaceService] Deleted space id={} by host={}", id, hostEmail);
    }

    private void checkSpaceAndHostAuthorization(Space space, String hostEmail) {
        if (space.getVenue() == null || Boolean.TRUE.equals(space.getVenue().getDeleted())) {
            throw new AppException("venue.not.found", HttpStatus.NOT_FOUND);
        }

        boolean isOwner = space.getVenue().getOwner() != null
                && hostEmail.equals(space.getVenue().getOwner().getEmail());
        boolean isManager = space.getHosts() != null
                && space.getHosts().stream().anyMatch(h -> hostEmail.equals(h.getEmail()));

        if (!isOwner && !isManager) {
            throw new AppException("common.forbidden", HttpStatus.FORBIDDEN);
        }
    }

    private void validateSearchRequest(SpaceSearchRequest request) {
        validatePageRequest(request.getPage(), request.getSize());

        if (request.getSortBy() == null || !ALLOWED_SORT_FIELDS.contains(request.getSortBy().trim())) {
            throw new AppException("validation.space.sortBy.invalid", HttpStatus.BAD_REQUEST);
        }
        if (request.getSortDir() == null
                || !("ASC".equalsIgnoreCase(request.getSortDir())
                || "DESC".equalsIgnoreCase(request.getSortDir()))) {
            throw new AppException("validation.sortDir.invalid", HttpStatus.BAD_REQUEST);
        }
        if (request.getMinPrice() != null && request.getMinPrice().signum() < 0
                || request.getMaxPrice() != null && request.getMaxPrice().signum() < 0) {
            throw new AppException("validation.space.searchPrice.min", HttpStatus.BAD_REQUEST);
        }
        if (request.getMinPrice() != null && request.getMaxPrice() != null
                && request.getMinPrice().compareTo(request.getMaxPrice()) > 0) {
            throw new AppException("validation.space.priceRange.invalid", HttpStatus.BAD_REQUEST);
        }
        if (request.getOpenTime() != null && request.getCloseTime() != null
                && !request.getOpenTime().isBefore(request.getCloseTime())) {
            throw new AppException("validation.space.operatingHours.invalid", HttpStatus.BAD_REQUEST);
        }

        boolean hasBookingStart = request.getBookingStart() != null;
        boolean hasBookingEnd = request.getBookingEnd() != null;
        if (hasBookingStart != hasBookingEnd) {
            throw new AppException(
                    hasBookingStart ? "validation.space.bookingEnd.required" : "validation.space.bookingStart.required",
                    HttpStatus.BAD_REQUEST
            );
        }
        if (hasBookingStart && !request.getBookingStart().isBefore(request.getBookingEnd())) {
            throw new AppException("validation.space.bookingRange.invalid", HttpStatus.BAD_REQUEST);
        }

        if (request.getPriceUnit() != null && !request.getPriceUnit().isBlank()) {
            PriceUnit unit = parsePriceUnit(request.getPriceUnit());
            request.setPriceUnit(unit.name().toLowerCase(Locale.ROOT));
        }
    }

    private void validatePageRequest(int page, int size) {
        if (page < 0) {
            throw new AppException("validation.page.min", HttpStatus.BAD_REQUEST);
        }
        if (size < 1) {
            throw new AppException("validation.size.min", HttpStatus.BAD_REQUEST);
        }
        if (size > 100) {
            throw new AppException("validation.size.max", HttpStatus.BAD_REQUEST);
        }
    }

    private PriceUnit parsePriceUnit(String value) {
        try {
            return PriceUnit.fromStringStrict(value);
        } catch (IllegalArgumentException ex) {
            throw new AppException("validation.space.priceUnit.invalid", HttpStatus.BAD_REQUEST);
        }
    }
}
