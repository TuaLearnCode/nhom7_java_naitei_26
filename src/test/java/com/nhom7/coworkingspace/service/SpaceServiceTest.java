package com.nhom7.coworkingspace.service;

import com.nhom7.coworkingspace.dto.request.AddSpaceManagerRequest;
import com.nhom7.coworkingspace.dto.request.SpaceCreateRequest;
import com.nhom7.coworkingspace.dto.request.SpaceSearchRequest;
import com.nhom7.coworkingspace.dto.request.SpaceUpdateRequest;
import com.nhom7.coworkingspace.dto.response.PageResponse;
import com.nhom7.coworkingspace.dto.response.SpaceResponse;
import com.nhom7.coworkingspace.entity.Space;
import com.nhom7.coworkingspace.entity.Role;
import com.nhom7.coworkingspace.entity.User;
import com.nhom7.coworkingspace.entity.Venue;
import com.nhom7.coworkingspace.enums.SpaceStatus;
import com.nhom7.coworkingspace.exception.AppException;
import com.nhom7.coworkingspace.mapper.SpaceMapper;
import com.nhom7.coworkingspace.repository.SpaceRepository;
import com.nhom7.coworkingspace.repository.UserRepository;
import com.nhom7.coworkingspace.repository.VenueRepository;
import com.nhom7.coworkingspace.service.impl.SpaceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("SpaceService - Unit Tests")
class SpaceServiceTest {

    @Mock
    private SpaceRepository spaceRepository;

    @Mock
    private VenueRepository venueRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SpaceMapper spaceMapper;

    @InjectMocks
    private SpaceServiceImpl spaceService;

    private User hostOwner;
    private User newManager;
    private Venue mockVenue;
    private Space mockSpace;
    private SpaceResponse mockResponse;

    @BeforeEach
    void setUp() {
        hostOwner = User.builder()
                .id(1L)
                .email("host@example.com")
                .name("Host Owner")
                .build();

        newManager = User.builder()
                .id(2L)
                .email("manager@example.com")
                .name("New Manager")
                .roles(new HashSet<>(Set.of(Role.builder().id(2L).name("HOST").build())))
                .build();

        mockVenue = Venue.builder()
                .id(100L)
                .owner(hostOwner)
                .name("Innovation Hub")
                .city("Hanoi")
                .street("Kim Ma")
                .address("123 Kim Ma, Ba Dinh, Hanoi")
                .status(com.nhom7.coworkingspace.enums.VenueStatus.APPROVE)
                .deleted(false)
                .build();

        mockSpace = Space.builder()
                .id(10L)
                .venue(mockVenue)
                .name("Private Office A")
                .type("private office")
                .price(new BigDecimal("5000000.00"))
                .priceUnit("month")
                .openTime(LocalTime.of(8, 0))
                .closeTime(LocalTime.of(22, 0))
                .capacity(4)
                .status(SpaceStatus.ACTIVE)
                .hosts(new HashSet<>(Set.of(hostOwner)))
                .build();

        mockResponse = SpaceResponse.builder()
                .id(10L)
                .venueId(100L)
                .venueName("Innovation Hub")
                .name("Private Office A")
                .type("private office")
                .price(new BigDecimal("5000000.00"))
                .priceUnit("month")
                .openTime(LocalTime.of(8, 0))
                .closeTime(LocalTime.of(22, 0))
                .capacity(4)
                .status(SpaceStatus.ACTIVE)
                .managerIds(Set.of(1L))
                .build();
    }

    @Test
    @DisplayName("Should return paginated space search results")
    void givenSearchRequest_whenSearchSpaces_thenReturnPageResponse() {
        SpaceSearchRequest request = SpaceSearchRequest.builder()
                .name("Private Office")
                .city("Hanoi")
                .type("private office")
                .page(0)
                .size(10)
                .build();

        Page<Space> page = new PageImpl<>(List.of(mockSpace));

        given(spaceRepository.findAll(
                ArgumentMatchers.<Specification<Space>>any(),
                any(Pageable.class))).willReturn(page);
        given(spaceMapper.toSpaceResponse(mockSpace)).willReturn(mockResponse);

        PageResponse<SpaceResponse> response = spaceService.searchSpaces(request);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getName()).isEqualTo("Private Office A");
        assertThat(response.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should search spaces with available time filter")
    void givenAvailableTimeFilter_whenSearchSpaces_thenReturnMatchingSpaces() {
        SpaceSearchRequest request = SpaceSearchRequest.builder()
                .openTime(LocalTime.of(9, 0))
                .closeTime(LocalTime.of(18, 0))
                .bookingStart(LocalDateTime.of(2026, 8, 25, 9, 0))
                .bookingEnd(LocalDateTime.of(2026, 8, 25, 17, 0))
                .page(0)
                .size(10)
                .build();

        Page<Space> page = new PageImpl<>(List.of(mockSpace));

        given(spaceRepository.findAll(
                ArgumentMatchers.<Specification<Space>>any(),
                any(Pageable.class))).willReturn(page);
        given(spaceMapper.toSpaceResponse(mockSpace)).willReturn(mockResponse);

        PageResponse<SpaceResponse> response = spaceService.searchSpaces(request);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("Should create space successfully when openTime < closeTime")
    void givenValidRequest_whenCreateSpace_thenReturnSpaceResponse() {
        SpaceCreateRequest createRequest = SpaceCreateRequest.builder()
                .name("Meeting Room B")
                .type("meeting space")
                .capacity(10)
                .price(new BigDecimal("200000"))
                .priceUnit("HOUR")
                .openTime(LocalTime.of(8, 0))
                .closeTime(LocalTime.of(20, 0))
                .build();

        given(venueRepository.findByIdAndDeletedFalse(100L)).willReturn(Optional.of(mockVenue));
        given(spaceRepository.save(any(Space.class))).willReturn(mockSpace);
        given(spaceMapper.toSpaceResponse(mockSpace)).willReturn(mockResponse);

        SpaceResponse response = spaceService.createSpace(100L, createRequest, "host@example.com");

        assertThat(response).isNotNull();
        verify(spaceRepository).save(any(Space.class));
    }

    @Test
    @DisplayName("Should throw AppException when openTime >= closeTime during space creation")
    void givenInvalidOperatingHours_whenCreateSpace_thenThrowException() {
        SpaceCreateRequest invalidRequest = SpaceCreateRequest.builder()
                .name("Invalid Hours Space")
                .capacity(5)
                .price(new BigDecimal("100000"))
                .priceUnit("HOUR")
                .openTime(LocalTime.of(18, 0))
                .closeTime(LocalTime.of(8, 0))
                .build();

        given(venueRepository.findByIdAndDeletedFalse(100L)).willReturn(Optional.of(mockVenue));

        assertThatThrownBy(() -> spaceService.createSpace(100L, invalidRequest, "host@example.com"))
                .isInstanceOf(AppException.class)
                .hasMessage("booking.operating.hours.invalid")
                .extracting("status")
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Should reject an unsupported price unit instead of defaulting to HOUR")
    void givenInvalidPriceUnit_whenCreateSpace_thenThrowBadRequest() {
        SpaceCreateRequest request = SpaceCreateRequest.builder()
                .name("Invalid Price Unit")
                .capacity(5)
                .price(new BigDecimal("100000"))
                .priceUnit("WEEK")
                .openTime(LocalTime.of(8, 0))
                .closeTime(LocalTime.of(18, 0))
                .build();
        given(venueRepository.findByIdAndDeletedFalse(100L)).willReturn(Optional.of(mockVenue));

        assertThatThrownBy(() -> spaceService.createSpace(100L, request, "host@example.com"))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getMessageKey())
                        .isEqualTo("validation.space.priceUnit.invalid"));
    }

    @Test
    @DisplayName("Should reject invalid search ranges and pagination")
    void givenInvalidSearchInput_whenSearchSpaces_thenThrowBadRequest() {
        SpaceSearchRequest invalidRange = SpaceSearchRequest.builder()
                .minPrice(new BigDecimal("500000"))
                .maxPrice(new BigDecimal("100000"))
                .page(0)
                .size(10)
                .sortBy("id")
                .sortDir("ASC")
                .build();

        assertThatThrownBy(() -> spaceService.searchSpaces(invalidRange))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getMessageKey())
                        .isEqualTo("validation.space.priceRange.invalid"));

        assertThatThrownBy(() -> spaceService.getMySpaces("host@example.com", 0, 0))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getMessageKey())
                        .isEqualTo("validation.size.min"));
    }

    @Test
    @DisplayName("Should throw AppException when host does not own the venue")
    void givenUnauthorizedHost_whenCreateSpace_thenThrowForbidden() {
        SpaceCreateRequest request = SpaceCreateRequest.builder()
                .name("Private Office C")
                .capacity(2)
                .price(new BigDecimal("150000"))
                .priceUnit("HOUR")
                .openTime(LocalTime.of(8, 0))
                .closeTime(LocalTime.of(18, 0))
                .build();

        given(venueRepository.findByIdAndDeletedFalse(100L)).willReturn(Optional.of(mockVenue));

        assertThatThrownBy(() -> spaceService.createSpace(100L, request, "otherhost@example.com"))
                .isInstanceOf(AppException.class)
                .hasMessage("venue.access.denied")
                .extracting("status")
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Should throw AppException when creating space in a PENDING venue")
    void givenPendingVenue_whenCreateSpace_thenThrowVenueNotApproved() {
        Venue pendingVenue = Venue.builder()
                .id(101L)
                .owner(hostOwner)
                .name("Pending Venue")
                .status(com.nhom7.coworkingspace.enums.VenueStatus.PENDING)
                .deleted(false)
                .build();

        SpaceCreateRequest request = SpaceCreateRequest.builder()
                .name("Meeting Room C")
                .capacity(5)
                .price(new BigDecimal("100000"))
                .priceUnit("HOUR")
                .openTime(LocalTime.of(8, 0))
                .closeTime(LocalTime.of(18, 0))
                .build();

        given(venueRepository.findByIdAndDeletedFalse(101L)).willReturn(Optional.of(pendingVenue));

        assertThatThrownBy(() -> spaceService.createSpace(101L, request, "host@example.com"))
                .isInstanceOf(AppException.class)
                .hasMessage("venue.not.approved")
                .extracting("status")
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Should add manager to space successfully")
    void givenValidManagerRequest_whenAddManagerToSpace_thenReturnUpdatedSpace() {
        AddSpaceManagerRequest managerRequest = AddSpaceManagerRequest.builder()
                .userId(2L)
                .build();

        given(spaceRepository.findById(10L)).willReturn(Optional.of(mockSpace));
        given(userRepository.findById(2L)).willReturn(Optional.of(newManager));
        given(spaceRepository.save(any(Space.class))).willReturn(mockSpace);
        given(spaceMapper.toSpaceResponse(mockSpace)).willReturn(mockResponse);

        SpaceResponse response = spaceService.addManagerToSpace(10L, managerRequest, "host@example.com");

        assertThat(response).isNotNull();
        assertThat(mockSpace.getHosts()).contains(newManager);
        verify(spaceRepository).save(mockSpace);
    }

    @Test
    @DisplayName("Should throw AppException when adding manager and user is not found")
    void givenUserNotFound_whenAddManagerToSpace_thenThrowNotFound() {
        AddSpaceManagerRequest managerRequest = AddSpaceManagerRequest.builder()
                .userId(99L)
                .build();

        given(spaceRepository.findById(10L)).willReturn(Optional.of(mockSpace));
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> spaceService.addManagerToSpace(10L, managerRequest, "host@example.com"))
                .isInstanceOf(AppException.class)
                .hasMessage("user.not.found")
                .extracting("status")
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Should reject a manager without HOST role")
    void givenNonHostUser_whenAddManagerToSpace_thenThrowBadRequest() {
        AddSpaceManagerRequest managerRequest = AddSpaceManagerRequest.builder()
                .userId(2L)
                .build();
        User regularUser = User.builder()
                .id(2L)
                .email("user@example.com")
                .roles(new HashSet<>(Set.of(Role.builder().id(1L).name("USER").build())))
                .build();

        given(spaceRepository.findById(10L)).willReturn(Optional.of(mockSpace));
        given(userRepository.findById(2L)).willReturn(Optional.of(regularUser));

        assertThatThrownBy(() -> spaceService.addManagerToSpace(10L, managerRequest, "host@example.com"))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appException = (AppException) ex;
                    assertThat(appException.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(appException.getMessageKey()).isEqualTo("space.manager.host.required");
                });
    }

    @Test
    @DisplayName("Should update space successfully")
    void givenValidUpdateRequest_whenUpdateSpace_thenReturnUpdatedResponse() {
        SpaceUpdateRequest updateRequest = SpaceUpdateRequest.builder()
                .name("Updated Office")
                .capacity(8)
                .price(new BigDecimal("300000"))
                .priceUnit("HOUR")
                .openTime(LocalTime.of(7, 0))
                .closeTime(LocalTime.of(21, 0))
                .status(SpaceStatus.ACTIVE)
                .build();

        given(spaceRepository.findById(10L)).willReturn(Optional.of(mockSpace));
        given(spaceRepository.save(any(Space.class))).willReturn(mockSpace);
        given(spaceMapper.toSpaceResponse(mockSpace)).willReturn(mockResponse);

        SpaceResponse response = spaceService.updateSpace(10L, updateRequest, "host@example.com");

        assertThat(response).isNotNull();
        verify(spaceRepository).save(mockSpace);
    }

    @Test
    @DisplayName("Should delete space successfully")
    void givenExistingSpaceId_whenDeleteSpace_thenDeleteFromRepo() {
        given(spaceRepository.findById(10L)).willReturn(Optional.of(mockSpace));

        spaceService.deleteSpace(10L, "host@example.com");

        verify(spaceRepository).delete(mockSpace);
    }

    @Test
    @DisplayName("Should return my spaces for host")
    void givenHostEmail_whenGetMySpaces_thenReturnPageResponse() {
        Page<Space> page = new PageImpl<>(List.of(mockSpace));
        given(spaceRepository.findMySpaces("host@example.com", PageRequest.of(0, 10, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "id"))))
                .willReturn(page);
        given(spaceMapper.toSpaceResponse(mockSpace)).willReturn(mockResponse);

        PageResponse<SpaceResponse> response = spaceService.getMySpaces("host@example.com", 0, 10);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("Should return spaces inside a venue")
    void givenVenueId_whenGetSpacesByVenue_thenReturnPageResponse() {
        Page<Space> page = new PageImpl<>(List.of(mockSpace));
        given(venueRepository.findByIdAndDeletedFalse(100L)).willReturn(Optional.of(mockVenue));
        given(spaceRepository.findByVenueIdAndVenueDeletedFalse(100L, PageRequest.of(0, 10, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "id"))))
                .willReturn(page);
        given(spaceMapper.toSpaceResponse(mockSpace)).willReturn(mockResponse);

        PageResponse<SpaceResponse> response = spaceService.getSpacesByVenue(100L, 0, 10);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
    }
}
