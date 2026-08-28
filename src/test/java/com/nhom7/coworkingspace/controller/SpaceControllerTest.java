package com.nhom7.coworkingspace.controller;

import com.nhom7.coworkingspace.config.JwtProperties;
import com.nhom7.coworkingspace.controller.api.SpaceController;
import com.nhom7.coworkingspace.dto.request.SpaceSearchRequest;
import com.nhom7.coworkingspace.dto.response.PageResponse;
import com.nhom7.coworkingspace.dto.response.SpaceResponse;
import com.nhom7.coworkingspace.security.CustomUserDetailsService;
import com.nhom7.coworkingspace.security.JwtAuthenticationFilter;
import com.nhom7.coworkingspace.security.JwtTokenProvider;
import com.nhom7.coworkingspace.service.SpaceService;
import com.nhom7.coworkingspace.service.TokenBlacklistService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SpaceController.class)
@Import({JwtAuthenticationFilter.class, JwtProperties.class})
@DisplayName("SpaceController - Integration & Security Tests")
class SpaceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SpaceService spaceService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private TokenBlacklistService tokenBlacklistService;

    @Test
    @WithMockUser(username = "user@test.com", roles = {"USER"})
    @DisplayName("Authenticated USER -> GET /api/spaces/search returns 200 OK")
    void givenUserRole_whenSearchSpaces_thenReturn200() throws Exception {
        SpaceResponse responseDto = SpaceResponse.builder()
                .id(1L)
                .name("Desk 101")
                .type("working desk")
                .price(new BigDecimal("150000.00"))
                .priceUnit("day")
                .venueCity("Da Nang")
                .build();

        PageResponse<SpaceResponse> pageResponse = PageResponse.<SpaceResponse>builder()
                .content(List.of(responseDto))
                .pageNumber(0)
                .pageSize(10)
                .totalElements(1)
                .totalPages(1)
                .last(true)
                .build();

        given(spaceService.searchSpaces(any(SpaceSearchRequest.class))).willReturn(pageResponse);

        mockMvc.perform(get("/api/spaces/search")
                        .param("name", "Desk")
                        .param("city", "Da Nang")
                        .param("type", "working desk"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.data.content[0].name").value("Desk 101"))

                .andExpect(jsonPath("$.data.content[0].type").value("working desk"))
                .andExpect(jsonPath("$.data.content[0].venueCity").value("Da Nang"));
    }

    @Test
    @DisplayName("Unauthenticated request -> GET /api/spaces/search returns 401 Unauthorized")
    void givenUnauthenticated_whenSearchSpaces_thenReturn401() throws Exception {
        mockMvc.perform(get("/api/spaces/search"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = {"USER"})
    @DisplayName("Search binds ISO time and datetime query parameters")
    void givenIsoTimeParameters_whenSearchSpaces_thenReturn200() throws Exception {
        PageResponse<SpaceResponse> emptyPage = emptyPage();
        given(spaceService.searchSpaces(any(SpaceSearchRequest.class))).willReturn(emptyPage);

        mockMvc.perform(get("/api/spaces/search")
                        .param("openTime", "08:00:00")
                        .param("closeTime", "18:00:00")
                        .param("bookingStart", "2026-08-28T08:00:00")
                        .param("bookingEnd", "2026-08-28T10:00:00")
                        .param("priceUnit", "PER_HOUR")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "id")
                        .param("sortDir", "ASC"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = {"USER"})
    @DisplayName("Invalid search time returns a field-level 400 response")
    void givenInvalidTime_whenSearchSpaces_thenReturnFieldError() throws Exception {
        mockMvc.perform(get("/api/spaces/search").param("openTime", "invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.data.openTime").exists());
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = {"USER"})
    @DisplayName("Invalid search ranges return field-level 400 responses")
    void givenInvalidRanges_whenSearchSpaces_thenReturnFieldErrors() throws Exception {
        mockMvc.perform(get("/api/spaces/search")
                        .param("minPrice", "500000")
                        .param("maxPrice", "100000")
                        .param("bookingStart", "2026-08-28T10:00:00")
                        .param("bookingEnd", "2026-08-28T08:00:00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.maxPrice").exists())
                .andExpect(jsonPath("$.data.bookingEnd").exists());
    }

    @Test
    @WithMockUser(username = "host@test.com", roles = {"HOST"})
    @DisplayName("All HOST space management endpoints accept valid input")
    void givenValidInput_whenCallingSpaceManagementEndpoints_thenSucceed() throws Exception {
        SpaceResponse response = SpaceResponse.builder().id(10L).name("Meeting Room").build();
        PageResponse<SpaceResponse> page = emptyPage();
        given(spaceService.createSpace(anyLong(), any(), anyString())).willReturn(response);
        given(spaceService.updateSpace(anyLong(), any(), anyString())).willReturn(response);
        given(spaceService.addManagerToSpace(anyLong(), any(), anyString())).willReturn(response);
        given(spaceService.getMySpaces(anyString(), anyInt(), anyInt())).willReturn(page);
        given(spaceService.getSpacesByVenue(anyLong(), anyInt(), anyInt())).willReturn(page);

        String spaceBody = """
                {
                  "name": "Meeting Room",
                  "type": "meeting space",
                  "capacity": 10,
                  "description": "A quiet meeting room",
                  "price": 200000,
                  "priceUnit": "PER_HOUR",
                  "openTime": "08:00:00",
                  "closeTime": "18:00:00"
                }
                """;

        mockMvc.perform(post("/api/venues/13/spaces")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(spaceBody))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/api/venues/13/spaces").param("page", "0").param("size", "10"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/spaces/my-spaces").param("page", "0").param("size", "10"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/spaces/10")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(spaceBody))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/spaces/10/managers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":2}"))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/spaces/10").with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "host@test.com", roles = {"HOST"})
    @DisplayName("Object-shaped LocalTime is rejected with the affected field")
    void givenObjectTime_whenCreateSpace_thenReturnFieldError() throws Exception {
        String body = """
                {
                  "name": "Meeting Room",
                  "capacity": 10,
                  "price": 200000,
                  "priceUnit": "HOUR",
                  "openTime": {"hour":8,"minute":0,"second":0,"nano":0},
                  "closeTime": "18:00:00"
                }
                """;

        mockMvc.perform(post("/api/venues/13/spaces")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.openTime").exists());
    }

    @Test
    @WithMockUser(username = "host@test.com", roles = {"HOST"})
    @DisplayName("Strict pagination and positive IDs return field-level errors")
    void givenInvalidPaginationAndId_thenReturnBadRequest() throws Exception {
        mockMvc.perform(get("/api/spaces/my-spaces").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.size").exists());

        mockMvc.perform(delete("/api/spaces/0").with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.id").exists());
    }

    private PageResponse<SpaceResponse> emptyPage() {
        return PageResponse.<SpaceResponse>builder()
                .content(List.of())
                .pageNumber(0)
                .pageSize(10)
                .totalElements(0)
                .totalPages(0)
                .last(true)
                .build();
    }
}
