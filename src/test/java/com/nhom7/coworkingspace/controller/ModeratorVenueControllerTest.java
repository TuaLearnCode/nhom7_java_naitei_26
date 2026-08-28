package com.nhom7.coworkingspace.controller;

import com.nhom7.coworkingspace.config.JwtProperties;
import com.nhom7.coworkingspace.controller.api.ModeratorVenueController;
import com.nhom7.coworkingspace.dto.response.PageResponse;
import com.nhom7.coworkingspace.dto.response.VenueDetailResponse;
import com.nhom7.coworkingspace.dto.response.VenueResponse;
import com.nhom7.coworkingspace.enums.VenueStatus;
import com.nhom7.coworkingspace.exception.AppException;
import com.nhom7.coworkingspace.security.CustomUserDetailsService;
import com.nhom7.coworkingspace.security.JwtAuthenticationFilter;
import com.nhom7.coworkingspace.security.JwtTokenProvider;
import com.nhom7.coworkingspace.service.TokenBlacklistService;
import com.nhom7.coworkingspace.service.VenueService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ModeratorVenueController.class)
@EnableMethodSecurity
@Import({JwtAuthenticationFilter.class, JwtProperties.class})
@DisplayName("ModeratorVenueController - WebMvc & Security Tests")
class ModeratorVenueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VenueService venueService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private TokenBlacklistService tokenBlacklistService;

    @Test
    @WithMockUser(username = "moderator@test.com", roles = {"MODERATOR"})
    @DisplayName("Authenticated MODERATOR -> GET /api/moderator/venues returns a filtered page")
    void givenModeratorRole_whenListVenues_thenReturn200() throws Exception {
        VenueResponse venue = VenueResponse.builder().id(1L).name("Innovation Hub")
                .status(VenueStatus.PENDING).build();
        PageResponse<VenueResponse> page = PageResponse.<VenueResponse>builder()
                .content(List.of(venue)).pageNumber(0).pageSize(10).totalElements(1).totalPages(1).last(true).build();
        given(venueService.getAllVenues(0, 10, VenueStatus.PENDING)).willReturn(page);

        mockMvc.perform(get("/api/moderator/venues").param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].name").value("Innovation Hub"))
                .andExpect(jsonPath("$.data.content[0].status").value("PENDING"));
    }

    @Test
    @WithMockUser(username = "moderator@test.com", roles = {"MODERATOR"})
    @DisplayName("Authenticated MODERATOR can view venue detail")
    void givenModeratorRole_whenGetVenueDetail_thenReturn200() throws Exception {
        VenueDetailResponse detail = VenueDetailResponse.builder()
                .id(1L).name("Innovation Hub").status(VenueStatus.APPROVE).build();
        given(venueService.getVenueDetail(1L)).willReturn(detail);

        mockMvc.perform(get("/api/moderator/venues/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("Innovation Hub"));
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = {"ADMIN"})
    @DisplayName("Authenticated ADMIN can view venue detail")
    void givenAdminRole_whenGetVenueDetail_thenReturn200() throws Exception {
        given(venueService.getVenueDetail(1L))
                .willReturn(VenueDetailResponse.builder().id(1L).name("Venue").build());

        mockMvc.perform(get("/api/moderator/venues/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = {"USER"})
    @DisplayName("USER cannot view moderator venue detail")
    void givenUserRole_whenGetVenueDetail_thenReturn403() throws Exception {
        mockMvc.perform(get("/api/moderator/venues/1"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(venueService);
    }

    @Test
    @DisplayName("Anonymous cannot view moderator venue detail")
    void givenAnonymous_whenGetVenueDetail_thenReturn401() throws Exception {
        mockMvc.perform(get("/api/moderator/venues/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "moderator@test.com", roles = {"MODERATOR"})
    @DisplayName("Authenticated MODERATOR -> PUT /api/moderator/venues/{id}/status returns 200 OK")
    void givenModeratorRole_whenUpdateVenueStatus_thenReturn200() throws Exception {
        VenueResponse response = VenueResponse.builder().id(1L).ownerId(10L).status(VenueStatus.APPROVE).build();

        given(venueService.updateVenueStatus(eq(1L), eq(VenueStatus.APPROVE), eq(null), eq("moderator@test.com")))
                .willReturn(response);

        mockMvc.perform(put("/api/moderator/venues/1/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"APPROVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("APPROVE"));
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = {"ADMIN"})
    @DisplayName("Authenticated ADMIN -> PUT /api/moderator/venues/{id}/status returns 200 OK")
    void givenAdminRole_whenUpdateVenueStatus_thenReturn200() throws Exception {
        VenueResponse response = VenueResponse.builder().id(1L).ownerId(10L).status(VenueStatus.BLOCKED).build();

        given(venueService.updateVenueStatus(eq(1L), eq(VenueStatus.BLOCKED), eq("Vi phạm chính sách"), eq("admin@test.com")))
                .willReturn(response);

        mockMvc.perform(put("/api/moderator/venues/1/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"BLOCKED\", \"reason\": \"Vi phạm chính sách\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("BLOCKED"));
    }

    @Test
    @WithMockUser(username = "host@test.com", roles = {"HOST"})
    @DisplayName("@PreAuthorize blocks HOST before the service is ever called (cannot self-moderate)")
    void givenHostRole_whenUpdateVenueStatus_thenServiceNeverInvoked() throws Exception {
        mockMvc.perform(put("/api/moderator/venues/1/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"APPROVE\"}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(venueService);
    }

    @Test
    @DisplayName("Unauthenticated request -> PUT /api/moderator/venues/{id}/status returns 401 Unauthorized")
    void givenUnauthenticated_whenUpdateVenueStatus_thenReturn401() throws Exception {
        mockMvc.perform(put("/api/moderator/venues/1/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"APPROVE\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "moderator@test.com", roles = {"MODERATOR"})
    @DisplayName("Null status -> PUT /api/moderator/venues/{id}/status returns 400 Bad Request")
    void givenNullStatus_whenUpdateVenueStatus_thenReturn400() throws Exception {
        mockMvc.perform(put("/api/moderator/venues/1/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": null}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "moderator@test.com", roles = {"MODERATOR"})
    @DisplayName("Block without reason returns 400")
    void givenBlockedWithoutReason_whenUpdateVenueStatus_thenReturn400() throws Exception {
        mockMvc.perform(put("/api/moderator/venues/1/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"BLOCKED\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(venueService);
    }

    @Test
    @WithMockUser(username = "moderator@test.com", roles = {"MODERATOR"})
    @DisplayName("Block with a blank reason returns 400")
    void givenBlockedWithBlankReason_whenUpdateVenueStatus_thenReturn400() throws Exception {
        mockMvc.perform(put("/api/moderator/venues/1/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"BLOCKED\", \"reason\": \"   \"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(venueService);
    }

    @Test
    @WithMockUser(username = "moderator@test.com", roles = {"MODERATOR"})
    @DisplayName("Block reason longer than 500 characters returns 400")
    void givenBlockedWithLongReason_whenUpdateVenueStatus_thenReturn400() throws Exception {
        String longReason = "a".repeat(501);

        mockMvc.perform(put("/api/moderator/venues/1/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"BLOCKED\", \"reason\": \"" + longReason + "\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(venueService);
    }

    @Test
    @WithMockUser(username = "moderator@test.com", roles = {"MODERATOR"})
    @DisplayName("Unknown status returns 400 with the list of accepted values")
    void givenUnknownStatus_whenUpdateVenueStatus_thenReturnHelpfulBadRequest() throws Exception {
        mockMvc.perform(put("/api/moderator/venues/1/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"REJECTED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(containsString("PENDING, APPROVE, BLOCKED")));

        verifyNoInteractions(venueService);
    }

    @Test
    @WithMockUser(username = "moderator@test.com", roles = {"MODERATOR"})
    @DisplayName("Moderator moderating their own venue -> 403 with venue.cannot.moderate.self message")
    void givenModeratorOwnsVenue_whenUpdateVenueStatus_thenReturn403() throws Exception {
        given(venueService.updateVenueStatus(eq(1L), eq(VenueStatus.APPROVE), eq(null), eq("moderator@test.com")))
                .willThrow(new AppException("venue.cannot.moderate.self", HttpStatus.FORBIDDEN));

        mockMvc.perform(put("/api/moderator/venues/1/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"APPROVE\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }
}
