package com.nhom7.coworkingspace.service;

import com.nhom7.coworkingspace.dto.request.VenueRequest;
import com.nhom7.coworkingspace.dto.response.PageResponse;
import com.nhom7.coworkingspace.dto.response.AmenityResponse;
import com.nhom7.coworkingspace.dto.response.SpaceResponse;
import com.nhom7.coworkingspace.dto.response.VenueDetailResponse;
import com.nhom7.coworkingspace.dto.response.VenueResponse;
import com.nhom7.coworkingspace.entity.Amenity;
import com.nhom7.coworkingspace.entity.Role;
import com.nhom7.coworkingspace.entity.Space;
import com.nhom7.coworkingspace.entity.User;
import com.nhom7.coworkingspace.entity.Venue;
import com.nhom7.coworkingspace.enums.SpaceStatus;
import com.nhom7.coworkingspace.enums.UserStatus;
import com.nhom7.coworkingspace.enums.VenueStatus;
import com.nhom7.coworkingspace.exception.AppException;
import com.nhom7.coworkingspace.exception.VenueNotFoundException;
import com.nhom7.coworkingspace.mapper.VenueMapper;
import com.nhom7.coworkingspace.mapper.SpaceMapper;
import com.nhom7.coworkingspace.repository.AmenityRepository;
import com.nhom7.coworkingspace.repository.SpaceRepository;
import com.nhom7.coworkingspace.repository.UserRepository;
import com.nhom7.coworkingspace.repository.VenueRepository;
import com.nhom7.coworkingspace.service.impl.VenueServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("VenueServiceImpl - Unit Tests")
class VenueServiceImplTest {

    @Mock
    private VenueRepository venueRepository;

    @Mock
    private AmenityRepository amenityRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SpaceRepository spaceRepository;

    @Mock
    private VenueMapper venueMapper;

    @Mock
    private SpaceMapper spaceMapper;

    private VenueServiceImpl venueService;

    private static final String HOST_EMAIL = "host@coworking.test";

    @BeforeEach
    void setUp() {
        venueService = new VenueServiceImpl(
                venueRepository,
                amenityRepository,
                userRepository,
                spaceRepository,
                venueMapper,
                spaceMapper
        );
    }

    @Nested
    @DisplayName("getVenueDetail")
    class GetVenueDetailTests {

        @Test
        @DisplayName("Maps venue, host, amenities, spaces and current block reason")
        void getVenueDetail_MapsCompleteResponse() {
            User owner = hostUser(1L);
            owner.setName("Nguyễn Văn Host");
            owner.setPhone("0901234567");
            owner.setStatus(UserStatus.ACTIVE);
            owner.setIsIdentityVerified(true);
            owner.setIsBusinessVerified(false);

            Amenity wifi = Amenity.builder().id(10L).name("Wi-Fi").build();
            Amenity airConditioner = Amenity.builder().id(11L).name("Điều hòa").build();
            Venue venue = Venue.builder()
                    .id(100L)
                    .owner(owner)
                    .name("Innovation Hub")
                    .description("Không gian làm việc")
                    .address("1 Đại Cồ Việt")
                    .city("Hà Nội")
                    .street("Đại Cồ Việt")
                    .status(VenueStatus.BLOCKED)
                    .blockReason("Vi phạm chính sách")
                    .amenities(Set.of(wifi, airConditioner))
                    .deleted(false)
                    .build();
            Space space = Space.builder().id(20L).venue(venue).name("Meeting Room").build();
            SpaceResponse spaceResponse = SpaceResponse.builder().id(20L).name("Meeting Room").build();

            given(venueRepository.findByIdAndDeletedFalse(100L)).willReturn(Optional.of(venue));
            given(venueMapper.toAmenityResponse(wifi))
                    .willReturn(AmenityResponse.builder().id(10L).name("Wi-Fi").build());
            given(venueMapper.toAmenityResponse(airConditioner))
                    .willReturn(AmenityResponse.builder().id(11L).name("Điều hòa").build());
            given(spaceRepository.findByVenueId(100L)).willReturn(List.of(space));
            given(spaceMapper.toSpaceResponse(space)).willReturn(spaceResponse);

            VenueDetailResponse result = venueService.getVenueDetail(100L);

            assertThat(result.getName()).isEqualTo("Innovation Hub");
            assertThat(result.getStatus()).isEqualTo(VenueStatus.BLOCKED);
            assertThat(result.getBlockReason()).isEqualTo("Vi phạm chính sách");
            assertThat(result.getHost().getEmail()).isEqualTo(HOST_EMAIL);
            assertThat(result.getHost().getPhone()).isEqualTo("0901234567");
            assertThat(result.getHost().getIsIdentityVerified()).isTrue();
            assertThat(result.getAmenities()).extracting(AmenityResponse::getName)
                    .containsExactly("Wi-Fi", "Điều hòa");
            assertThat(result.getSpaces()).extracting(SpaceResponse::getName)
                    .containsExactly("Meeting Room");
        }

        @Test
        @DisplayName("Missing venue returns 404")
        void getVenueDetail_NotFound() {
            given(venueRepository.findByIdAndDeletedFalse(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> venueService.getVenueDetail(999L))
                    .isInstanceOf(VenueNotFoundException.class)
                    .hasMessage("venue.not.found");
        }
    }

    private User hostUser(Long id) {
        return User.builder()
                .id(id)
                .email(HOST_EMAIL)
                .name("Host User")
                .roles(Set.of(Role.builder().id(1L).name("HOST").build()))
                .build();
    }

    private User nonHostUser(Long id) {
        return User.builder()
                .id(id)
                .email(HOST_EMAIL)
                .name("Regular User")
                .roles(Set.of(Role.builder().id(2L).name("USER").build()))
                .build();
    }

    @Nested
    @DisplayName("createVenue")
    class CreateVenueTests {

        @Test
        @DisplayName("HOST creates venue successfully, owner resolved from SecurityContext (never from request)")
        void createVenue_Success() {
            User host = hostUser(1L);
            VenueRequest request = VenueRequest.builder()
                    .name("Innovation Hub")
                    .city("Hanoi")
                    .amenityIds(Set.of(10L))
                    .build();

            Amenity amenity = Amenity.builder().id(10L).name("Wifi").build();
            Venue savedVenue = Venue.builder().id(100L).owner(host).name("Innovation Hub").deleted(false).build();
            VenueResponse expectedResponse = VenueResponse.builder().id(100L).ownerId(1L).name("Innovation Hub").build();

            given(userRepository.findByEmail(HOST_EMAIL)).willReturn(Optional.of(host));
            given(amenityRepository.findAllById(Set.of(10L))).willReturn(List.of(amenity));
            given(venueRepository.save(any(Venue.class))).willReturn(savedVenue);
            given(venueMapper.toVenueResponse(savedVenue)).willReturn(expectedResponse);

            VenueResponse response = venueService.createVenue(request, HOST_EMAIL);

            assertThat(response.getId()).isEqualTo(100L);
            assertThat(response.getOwnerId()).isEqualTo(1L);

            org.mockito.ArgumentCaptor<Venue> captor = org.mockito.ArgumentCaptor.forClass(Venue.class);
            verify(venueRepository).save(captor.capture());
            assertThat(captor.getValue().getOwner()).isEqualTo(host);
            assertThat(captor.getValue().getDeleted()).isFalse();
            assertThat(captor.getValue().getStatus()).isEqualTo(VenueStatus.PENDING);
        }

        @Test
        @DisplayName("Non-HOST user creates venue -> 403 with venue.host.required message")
        void createVenue_NonHost_Forbidden() {
            User user = nonHostUser(2L);
            VenueRequest request = VenueRequest.builder().name("Some Venue").build();

            given(userRepository.findByEmail(HOST_EMAIL)).willReturn(Optional.of(user));

            assertThatThrownBy(() -> venueService.createVenue(request, HOST_EMAIL))
                    .isInstanceOf(AppException.class)
                    .hasMessage("venue.host.required")
                    .extracting("status")
                    .isEqualTo(HttpStatus.FORBIDDEN);

            verify(venueRepository, never()).save(any(Venue.class));
        }

        @Test
        @DisplayName("Invalid amenity id -> 400 with amenity.not.found message")
        void createVenue_InvalidAmenity_BadRequest() {
            User host = hostUser(1L);
            VenueRequest request = VenueRequest.builder()
                    .name("Innovation Hub")
                    .amenityIds(Set.of(10L, 20L))
                    .build();

            given(userRepository.findByEmail(HOST_EMAIL)).willReturn(Optional.of(host));
            given(amenityRepository.findAllById(Set.of(10L, 20L)))
                    .willReturn(List.of(Amenity.builder().id(10L).name("Wifi").build()));

            assertThatThrownBy(() -> venueService.createVenue(request, HOST_EMAIL))
                    .isInstanceOf(AppException.class)
                    .hasMessage("amenity.not.found")
                    .extracting("status")
                    .isEqualTo(HttpStatus.BAD_REQUEST);

            verify(venueRepository, never()).save(any(Venue.class));
        }
    }

    @Nested
    @DisplayName("getMyVenues")
    class GetMyVenuesTests {

        @Test
        @DisplayName("Returns only the authenticated HOST's non-deleted venues (paginated)")
        void getMyVenues_ReturnsOwnNonDeletedVenues() {
            User host = hostUser(1L);
            Venue venue = Venue.builder().id(100L).owner(host).name("Innovation Hub").deleted(false).build();
            VenueResponse response = VenueResponse.builder().id(100L).ownerId(1L).name("Innovation Hub").build();

            given(userRepository.findByEmail(HOST_EMAIL)).willReturn(Optional.of(host));
            given(venueRepository.findByOwnerIdAndDeletedFalse(eq(1L), any(Pageable.class)))
                    .willReturn(new PageImpl<>(List.of(venue)));
            given(venueMapper.toVenueResponse(venue)).willReturn(response);

            PageResponse<VenueResponse> result = venueService.getMyVenues(HOST_EMAIL, 0, 10);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getId()).isEqualTo(100L);
            verify(venueRepository).findByOwnerIdAndDeletedFalse(eq(1L), any(Pageable.class));
        }

        @Test
        @DisplayName("Non-HOST user cannot list venues -> 403")
        void getMyVenues_NonHost_Forbidden() {
            given(userRepository.findByEmail(HOST_EMAIL)).willReturn(Optional.of(nonHostUser(2L)));

            assertThatThrownBy(() -> venueService.getMyVenues(HOST_EMAIL, 0, 10))
                    .isInstanceOf(AppException.class)
                    .hasMessage("venue.host.required")
                    .extracting("status")
                    .isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    @Nested
    @DisplayName("updateVenue")
    class UpdateVenueTests {

        @Test
        @DisplayName("Owner HOST updates own venue successfully")
        void updateVenue_Owner_Success() {
            User host = hostUser(1L);
            Venue existingVenue = Venue.builder().id(100L).owner(host).name("Old Name").deleted(false).build();
            VenueRequest request = VenueRequest.builder().name("New Name").city("Hanoi").build();
            VenueResponse response = VenueResponse.builder().id(100L).ownerId(1L).name("New Name").build();

            given(userRepository.findByEmail(HOST_EMAIL)).willReturn(Optional.of(host));
            given(venueRepository.findByIdAndDeletedFalse(100L)).willReturn(Optional.of(existingVenue));
            given(venueRepository.save(existingVenue)).willReturn(existingVenue);
            given(venueMapper.toVenueResponse(existingVenue)).willReturn(response);

            VenueResponse result = venueService.updateVenue(100L, request, HOST_EMAIL);

            assertThat(result.getName()).isEqualTo("New Name");
            assertThat(existingVenue.getName()).isEqualTo("New Name");
            assertThat(existingVenue.getCity()).isEqualTo("Hanoi");
        }

        @Test
        @DisplayName("HOST updates a venue owned by another HOST -> 403 with venue.access.denied message")
        void updateVenue_NotOwner_Forbidden() {
            User otherHost = hostUser(1L);
            User currentHost = hostUser(2L);
            Venue existingVenue = Venue.builder().id(100L).owner(otherHost).name("Old Name").deleted(false).build();
            VenueRequest request = VenueRequest.builder().name("New Name").build();

            given(userRepository.findByEmail(HOST_EMAIL)).willReturn(Optional.of(currentHost));
            given(venueRepository.findByIdAndDeletedFalse(100L)).willReturn(Optional.of(existingVenue));

            assertThatThrownBy(() -> venueService.updateVenue(100L, request, HOST_EMAIL))
                    .isInstanceOf(AppException.class)
                    .hasMessage("venue.access.denied")
                    .extracting("status")
                    .isEqualTo(HttpStatus.FORBIDDEN);

            verify(venueRepository, never()).save(any(Venue.class));
        }

        @Test
        @DisplayName("Updating a non-existent (or soft-deleted) venue -> 404 with venue.not.found message")
        void updateVenue_NotFound() {
            User host = hostUser(1L);
            VenueRequest request = VenueRequest.builder().name("New Name").build();

            given(userRepository.findByEmail(HOST_EMAIL)).willReturn(Optional.of(host));
            given(venueRepository.findByIdAndDeletedFalse(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> venueService.updateVenue(999L, request, HOST_EMAIL))
                    .isInstanceOf(VenueNotFoundException.class)
                    .hasMessage("venue.not.found")
                    .extracting("status")
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("getAllVenues")
    class GetAllVenuesTests {

        @Test
        @DisplayName("Moderator listing can filter non-deleted venues by status")
        void getAllVenues_WithStatus_ReturnsFilteredPage() {
            Venue venue = Venue.builder().id(100L).name("Innovation Hub").status(VenueStatus.PENDING).build();
            VenueResponse response = VenueResponse.builder().id(100L).name("Innovation Hub")
                    .status(VenueStatus.PENDING).build();
            given(venueRepository.findByStatusAndDeletedFalse(eq(VenueStatus.PENDING), any(Pageable.class)))
                    .willReturn(new PageImpl<>(List.of(venue)));
            given(venueMapper.toVenueResponse(venue)).willReturn(response);

            PageResponse<VenueResponse> result = venueService.getAllVenues(0, 10, VenueStatus.PENDING);

            assertThat(result.getContent()).containsExactly(response);
            verify(venueRepository).findByStatusAndDeletedFalse(eq(VenueStatus.PENDING), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("updateVenueStatus")
    class UpdateVenueStatusTests {

        private static final String MODERATOR_EMAIL = "moderator@coworking.test";

        private User moderatorUser(Long id) {
            return User.builder()
                    .id(id)
                    .email(MODERATOR_EMAIL)
                    .name("Moderator User")
                    .roles(Set.of(Role.builder().id(3L).name("MODERATOR").build()))
                    .build();
        }

        @Test
        @DisplayName("Moderator approves a venue owned by someone else -> status updated")
        void updateVenueStatus_Success() {
            User owner = hostUser(1L);
            User moderator = moderatorUser(99L);
            Venue existingVenue = Venue.builder().id(100L).owner(owner).name("Venue").deleted(false)
                    .status(VenueStatus.PENDING).build();
            VenueResponse response = VenueResponse.builder().id(100L).status(VenueStatus.APPROVE).build();

            given(venueRepository.findByIdAndDeletedFalse(100L)).willReturn(Optional.of(existingVenue));
            given(userRepository.findByEmail(MODERATOR_EMAIL)).willReturn(Optional.of(moderator));
            given(venueRepository.save(existingVenue)).willReturn(existingVenue);
            given(venueMapper.toVenueResponse(existingVenue)).willReturn(response);

            VenueResponse result = venueService.updateVenueStatus(100L, VenueStatus.APPROVE, null, MODERATOR_EMAIL);

            assertThat(result.getStatus()).isEqualTo(VenueStatus.APPROVE);
            assertThat(existingVenue.getStatus()).isEqualTo(VenueStatus.APPROVE);
            verify(venueRepository).save(existingVenue);
            verify(spaceRepository, never()).findByVenueId(any());
        }

        @Test
        @DisplayName("Blocking a venue also marks its Spaces INACTIVE, same as deleteVenue")
        void updateVenueStatus_ToBlocked_DeactivatesSpaces() {
            User owner = hostUser(1L);
            User moderator = moderatorUser(99L);
            Venue existingVenue = Venue.builder().id(100L).owner(owner).name("Venue").deleted(false)
                    .status(VenueStatus.APPROVE).build();
            Space space = Space.builder().id(1L).venue(existingVenue).name("Room A").status(SpaceStatus.ACTIVE).build();
            VenueResponse response = VenueResponse.builder().id(100L).status(VenueStatus.BLOCKED).build();

            given(venueRepository.findByIdAndDeletedFalse(100L)).willReturn(Optional.of(existingVenue));
            given(userRepository.findByEmail(MODERATOR_EMAIL)).willReturn(Optional.of(moderator));
            given(venueRepository.save(existingVenue)).willReturn(existingVenue);
            given(spaceRepository.findByVenueId(100L)).willReturn(List.of(space));
            given(venueMapper.toVenueResponse(existingVenue)).willReturn(response);

            VenueResponse result = venueService.updateVenueStatus(
                    100L, VenueStatus.BLOCKED, "Vi phạm chính sách", MODERATOR_EMAIL
            );

            assertThat(result.getStatus()).isEqualTo(VenueStatus.BLOCKED);
            assertThat(existingVenue.getBlockReason()).isEqualTo("Vi phạm chính sách");
            assertThat(space.getStatus()).isEqualTo(SpaceStatus.INACTIVE);
            verify(spaceRepository).saveAll(List.of(space));
        }

        @Test
        @DisplayName("Blocking requires a non-blank reason")
        void updateVenueStatus_ToBlocked_RequiresReason() {
            User owner = hostUser(1L);
            User moderator = moderatorUser(99L);
            Venue venue = Venue.builder().id(100L).owner(owner).name("Venue").deleted(false)
                    .status(VenueStatus.APPROVE).build();
            given(venueRepository.findByIdAndDeletedFalse(100L)).willReturn(Optional.of(venue));
            given(userRepository.findByEmail(MODERATOR_EMAIL)).willReturn(Optional.of(moderator));

            assertThatThrownBy(() -> venueService.updateVenueStatus(
                    100L, VenueStatus.BLOCKED, "   ", MODERATOR_EMAIL
            ))
                    .isInstanceOf(AppException.class)
                    .hasMessage("venue.block.reason.required");

            verify(venueRepository, never()).save(any(Venue.class));
        }

        @Test
        @DisplayName("Approving a blocked venue clears its current block reason")
        void updateVenueStatus_ToApprove_ClearsReason() {
            User owner = hostUser(1L);
            User moderator = moderatorUser(99L);
            Venue venue = Venue.builder().id(100L).owner(owner).name("Venue").deleted(false)
                    .status(VenueStatus.BLOCKED).blockReason("Lý do cũ").build();
            VenueResponse response = VenueResponse.builder().id(100L).status(VenueStatus.APPROVE).build();
            given(venueRepository.findByIdAndDeletedFalse(100L)).willReturn(Optional.of(venue));
            given(userRepository.findByEmail(MODERATOR_EMAIL)).willReturn(Optional.of(moderator));
            given(venueRepository.save(venue)).willReturn(venue);
            given(venueMapper.toVenueResponse(venue)).willReturn(response);

            venueService.updateVenueStatus(100L, VenueStatus.APPROVE, null, MODERATOR_EMAIL);

            assertThat(venue.getStatus()).isEqualTo(VenueStatus.APPROVE);
            assertThat(venue.getBlockReason()).isNull();
        }

        @Test
        @DisplayName("PENDING venue can only be approved, not blocked")
        void updateVenueStatus_PendingToBlocked_BadRequest() {
            User owner = hostUser(1L);
            User moderator = moderatorUser(99L);
            Venue existingVenue = Venue.builder().id(100L).owner(owner).name("Venue").deleted(false)
                    .status(VenueStatus.PENDING).build();

            given(venueRepository.findByIdAndDeletedFalse(100L)).willReturn(Optional.of(existingVenue));
            given(userRepository.findByEmail(MODERATOR_EMAIL)).willReturn(Optional.of(moderator));

            assertThatThrownBy(() -> venueService.updateVenueStatus(
                    100L, VenueStatus.BLOCKED, "Vi phạm chính sách", MODERATOR_EMAIL
            ))
                    .isInstanceOf(AppException.class)
                    .hasMessage("venue.status.transition.invalid")
                    .extracting("status")
                    .isEqualTo(HttpStatus.BAD_REQUEST);

            assertThat(existingVenue.getStatus()).isEqualTo(VenueStatus.PENDING);
            verify(venueRepository, never()).save(any(Venue.class));
        }

        @Test
        @DisplayName("An approved or blocked venue cannot return to PENDING")
        void updateVenueStatus_ApprovedToPending_BadRequest() {
            User owner = hostUser(1L);
            User moderator = moderatorUser(99L);
            Venue existingVenue = Venue.builder().id(100L).owner(owner).name("Venue").deleted(false)
                    .status(VenueStatus.APPROVE).build();

            given(venueRepository.findByIdAndDeletedFalse(100L)).willReturn(Optional.of(existingVenue));
            given(userRepository.findByEmail(MODERATOR_EMAIL)).willReturn(Optional.of(moderator));

            assertThatThrownBy(() -> venueService.updateVenueStatus(100L, VenueStatus.PENDING, null, MODERATOR_EMAIL))
                    .isInstanceOf(AppException.class)
                    .hasMessage("venue.status.transition.invalid")
                    .extracting("status")
                    .isEqualTo(HttpStatus.BAD_REQUEST);

            assertThat(existingVenue.getStatus()).isEqualTo(VenueStatus.APPROVE);
            verify(venueRepository, never()).save(any(Venue.class));
        }

        @Test
        @DisplayName("Setting the same status again is a no-op (idempotent, no save)")
        void updateVenueStatus_SameStatus_NoOp() {
            User owner = hostUser(1L);
            User moderator = moderatorUser(99L);
            Venue existingVenue = Venue.builder().id(100L).owner(owner).name("Venue").deleted(false)
                    .status(VenueStatus.APPROVE).build();
            VenueResponse response = VenueResponse.builder().id(100L).status(VenueStatus.APPROVE).build();

            given(venueRepository.findByIdAndDeletedFalse(100L)).willReturn(Optional.of(existingVenue));
            given(userRepository.findByEmail(MODERATOR_EMAIL)).willReturn(Optional.of(moderator));
            given(venueMapper.toVenueResponse(existingVenue)).willReturn(response);

            VenueResponse result = venueService.updateVenueStatus(100L, VenueStatus.APPROVE, null, MODERATOR_EMAIL);

            assertThat(result.getStatus()).isEqualTo(VenueStatus.APPROVE);
            verify(venueRepository, never()).save(any(Venue.class));
        }

        @Test
        @DisplayName("Moderator moderating their own venue -> 403 with venue.cannot.moderate.self message")
        void updateVenueStatus_SelfModeration_Forbidden() {
            User ownerAndModerator = moderatorUser(1L);
            Venue existingVenue = Venue.builder().id(100L).owner(ownerAndModerator).name("Venue").deleted(false)
                    .status(VenueStatus.PENDING).build();

            given(venueRepository.findByIdAndDeletedFalse(100L)).willReturn(Optional.of(existingVenue));
            given(userRepository.findByEmail(MODERATOR_EMAIL)).willReturn(Optional.of(ownerAndModerator));

            assertThatThrownBy(() -> venueService.updateVenueStatus(100L, VenueStatus.APPROVE, null, MODERATOR_EMAIL))
                    .isInstanceOf(AppException.class)
                    .hasMessage("venue.cannot.moderate.self")
                    .extracting("status")
                    .isEqualTo(HttpStatus.FORBIDDEN);

            verify(venueRepository, never()).save(any(Venue.class));
        }

        @Test
        @DisplayName("Updating status of a non-existent (or soft-deleted) venue -> 404 with venue.not.found message")
        void updateVenueStatus_NotFound() {
            given(venueRepository.findByIdAndDeletedFalse(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> venueService.updateVenueStatus(999L, VenueStatus.APPROVE, null, MODERATOR_EMAIL))
                    .isInstanceOf(VenueNotFoundException.class)
                    .hasMessage("venue.not.found")
                    .extracting("status")
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("deleteVenue")
    class DeleteVenueTests {

        @Test
        @DisplayName("Owner HOST soft deletes own venue (flag set, not physically removed)")
        void deleteVenue_Owner_SoftDeletes() {
            User host = hostUser(1L);
            Venue existingVenue = Venue.builder().id(100L).owner(host).name("Venue").deleted(false).build();

            given(userRepository.findByEmail(HOST_EMAIL)).willReturn(Optional.of(host));
            given(venueRepository.findByIdAndDeletedFalse(100L)).willReturn(Optional.of(existingVenue));

            venueService.deleteVenue(100L, HOST_EMAIL);

            assertThat(existingVenue.getDeleted()).isTrue();
            verify(venueRepository).save(existingVenue);
            verify(venueRepository, never()).delete(any(Venue.class));
            verify(venueRepository, never()).deleteById(any(Long.class));
        }

        @Test
        @DisplayName("Deleting a venue marks its Spaces INACTIVE instead of removing them")
        void deleteVenue_Owner_DeactivatesSpaces() {
            User host = hostUser(1L);
            Venue existingVenue = Venue.builder().id(100L).owner(host).name("Venue").deleted(false).build();
            Space space1 = Space.builder().id(1L).venue(existingVenue).name("Room A").status(SpaceStatus.ACTIVE).build();
            Space space2 = Space.builder().id(2L).venue(existingVenue).name("Room B").status(SpaceStatus.ACTIVE).build();

            given(userRepository.findByEmail(HOST_EMAIL)).willReturn(Optional.of(host));
            given(venueRepository.findByIdAndDeletedFalse(100L)).willReturn(Optional.of(existingVenue));
            given(spaceRepository.findByVenueId(100L)).willReturn(List.of(space1, space2));

            venueService.deleteVenue(100L, HOST_EMAIL);

            assertThat(space1.getStatus()).isEqualTo(SpaceStatus.INACTIVE);
            assertThat(space2.getStatus()).isEqualTo(SpaceStatus.INACTIVE);
            verify(spaceRepository).saveAll(List.of(space1, space2));
        }

        @Test
        @DisplayName("HOST deletes a venue owned by another HOST -> 403 with venue.access.denied message")
        void deleteVenue_NotOwner_Forbidden() {
            User otherHost = hostUser(1L);
            User currentHost = hostUser(2L);
            Venue existingVenue = Venue.builder().id(100L).owner(otherHost).name("Venue").deleted(false).build();

            given(userRepository.findByEmail(HOST_EMAIL)).willReturn(Optional.of(currentHost));
            given(venueRepository.findByIdAndDeletedFalse(100L)).willReturn(Optional.of(existingVenue));

            assertThatThrownBy(() -> venueService.deleteVenue(100L, HOST_EMAIL))
                    .isInstanceOf(AppException.class)
                    .hasMessage("venue.access.denied")
                    .extracting("status")
                    .isEqualTo(HttpStatus.FORBIDDEN);

            verify(venueRepository, never()).save(any(Venue.class));
        }

        @Test
        @DisplayName("Deleting a non-existent (or already soft-deleted) venue -> 404 with venue.not.found message")
        void deleteVenue_NotFound() {
            User host = hostUser(1L);

            given(userRepository.findByEmail(HOST_EMAIL)).willReturn(Optional.of(host));
            given(venueRepository.findByIdAndDeletedFalse(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> venueService.deleteVenue(999L, HOST_EMAIL))
                    .isInstanceOf(VenueNotFoundException.class)
                    .hasMessage("venue.not.found")
                    .extracting("status")
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }
    }
}
