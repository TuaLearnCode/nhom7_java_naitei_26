package com.nhom7.coworkingspace.controller;

import com.nhom7.coworkingspace.config.JwtProperties;
import com.nhom7.coworkingspace.config.SecurityConfig;
import com.nhom7.coworkingspace.controller.web.ModeratorVenueWebController;
import com.nhom7.coworkingspace.dto.response.PageResponse;
import com.nhom7.coworkingspace.dto.response.VenueResponse;
import com.nhom7.coworkingspace.enums.VenueStatus;
import com.nhom7.coworkingspace.security.CustomUserDetailsService;
import com.nhom7.coworkingspace.security.JwtAuthenticationFilter;
import com.nhom7.coworkingspace.security.JwtAuthErrorHandler;
import com.nhom7.coworkingspace.security.JwtTokenProvider;
import com.nhom7.coworkingspace.service.TokenBlacklistService;
import com.nhom7.coworkingspace.service.VenueService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(
        ModeratorVenueWebController.class
)
@EnableMethodSecurity
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtProperties.class
})
@DisplayName(
        "ModeratorVenueWebController - Thymeleaf Web MVC & Security Tests"
)
class ModeratorVenueWebControllerTest {

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

    @MockBean
    private JwtAuthErrorHandler jwtAuthErrorHandler;

    @Test
    @WithMockUser(
            username = "moderator@test.com",
            roles = "MODERATOR"
    )
    @DisplayName(
            "MODERATOR can view the paginated venue management page"
    )
    void givenModeratorRole_whenListVenues_thenRenderManagementPage()
            throws Exception {

        VenueResponse venue =
                VenueResponse.builder()
                        .id(1L)
                        .name("Innovation Hub")
                        .ownerName("Host User")
                        .status(
                                VenueStatus.PENDING
                        )
                        .build();

        PageResponse<VenueResponse> venues =
                PageResponse
                        .<VenueResponse>builder()
                        .content(
                                List.of(venue)
                        )
                        .pageNumber(0)
                        .pageSize(10)
                        .totalElements(1)
                        .totalPages(1)
                        .last(true)
                        .build();

        given(
                venueService.getAllVenues(
                        0,
                        10,
                        null
                )
        ).willReturn(venues);

        mockMvc.perform(
                        get("/moderator/venues")
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        view().name(
                                "moderator/venues"
                        )
                )
                .andExpect(
                        model().attribute(
                                "venues",
                                venues
                        )
                )
                .andExpect(
                        model().attributeExists(
                                "statuses"
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "class=\"moderator-layout\""
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "/css/moderator-venues.css"
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString("Xem chi tiết")
                        )
                )
                .andExpect(
                        content().string(
                                containsString("id=\"venueDetailDrawer\"")
                        )
                )
                .andExpect(
                        content().string(
                                org.hamcrest.Matchers.not(containsString("Thống kê hệ thống"))
                        )
                );
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    @DisplayName("ADMIN sees the three admin-only sidebar entries")
    void givenAdminRole_whenListVenues_thenShowAdminSidebar() throws Exception {
        given(venueService.getAllVenues(0, 10, null)).willReturn(
                PageResponse.<VenueResponse>builder()
                        .content(List.of()).pageNumber(0).pageSize(10)
                        .totalElements(0).totalPages(0).last(true).build()
        );

        mockMvc.perform(get("/moderator/venues"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Thống kê hệ thống")))
                .andExpect(content().string(containsString("Lịch sử thanh toán")))
                .andExpect(content().string(containsString("Đổi role")));
    }

    @Test
    @WithMockUser(
            username = "moderator@test.com",
            roles = "MODERATOR"
    )
    @DisplayName(
            "MODERATOR can approve a venue from the management page"
    )
    void givenModeratorRole_whenApproveVenue_thenUpdateStatusAndRedirect()
            throws Exception {

        mockMvc.perform(
                        post(
                                "/moderator/venues/7/status"
                        )
                                .with(csrf())
                                .param(
                                        "status",
                                        "APPROVE"
                                )
                )
                .andExpect(
                        status().is3xxRedirection()
                )
                .andExpect(
                        redirectedUrl(
                                "/moderator/venues"
                        )
                )
                .andExpect(
                        flash().attributeExists(
                                "successMessage"
                        )
                );

        verify(
                venueService
        ).updateVenueStatus(
                7L,
                VenueStatus.APPROVE,
                null,
                "moderator@test.com"
        );
    }

    @Test
    @WithMockUser(
            username = "moderator@test.com",
            roles = "MODERATOR"
    )
    @DisplayName(
            "Pending venue row exposes both approve and block actions"
    )
    void givenPendingVenue_whenRenderList_thenShowApproveAndBlockActions()
            throws Exception {

        VenueResponse venue =
                VenueResponse.builder()
                        .id(7L)
                        .name("Innovation Hub")
                        .ownerName("Host User")
                        .status(
                                VenueStatus.PENDING
                        )
                        .build();

        given(
                venueService.getAllVenues(
                        0,
                        10,
                        null
                )
        ).willReturn(
                PageResponse
                        .<VenueResponse>builder()
                        .content(
                                List.of(venue)
                        )
                        .pageNumber(0)
                        .pageSize(10)
                        .totalElements(1)
                        .totalPages(1)
                        .last(true)
                        .build()
        );

        mockMvc.perform(
                        get("/moderator/venues")
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "venue-action-approve"
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "venue-action-block"
                                )
                        )
                );
    }

    @Test
    @WithMockUser(
            username = "user@test.com",
            roles = "USER"
    )
    @DisplayName(
            "Regular USER cannot access venue management"
    )
    void givenUserRole_whenListVenues_thenReturn403()
            throws Exception {

        mockMvc.perform(
                        get("/moderator/venues")
                )
                .andExpect(
                        status().isForbidden()
                );
    }

    @Test
    @DisplayName(
            "Anonymous browser request is redirected to the web login page"
    )
    void givenAnonymousUser_whenListVenues_thenRedirectToLogin()
            throws Exception {

        mockMvc.perform(
                        get("/moderator/venues")
                )
                .andExpect(
                        status().is3xxRedirection()
                )
                .andExpect(
                        redirectedUrl(
                                "http://localhost/login"
                        )
                );
    }
}
