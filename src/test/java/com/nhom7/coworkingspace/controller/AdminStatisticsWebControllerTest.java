package com.nhom7.coworkingspace.controller;

import com.nhom7.coworkingspace.config.JwtProperties;
import com.nhom7.coworkingspace.controller.web.AdminStatisticsWebController;
import com.nhom7.coworkingspace.dto.response.RevenueStatisticsResponse;
import com.nhom7.coworkingspace.dto.response.StatisticsOverviewResponse;
import com.nhom7.coworkingspace.security.CustomUserDetailsService;
import com.nhom7.coworkingspace.security.JwtAuthenticationFilter;
import com.nhom7.coworkingspace.security.JwtTokenProvider;
import com.nhom7.coworkingspace.service.StatisticsService;
import com.nhom7.coworkingspace.service.TokenBlacklistService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(AdminStatisticsWebController.class)
@EnableMethodSecurity
@Import({JwtAuthenticationFilter.class, JwtProperties.class})
@DisplayName("AdminStatisticsWebController tests")
class AdminStatisticsWebControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private StatisticsService statisticsService;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private CustomUserDetailsService customUserDetailsService;
    @MockBean private TokenBlacklistService tokenBlacklistService;
    @MockBean private Clock clock;

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanViewDashboardForSelectedYear() throws Exception {
        given(clock.instant()).willReturn(Instant.parse("2026-08-26T00:00:00Z"));
        given(clock.getZone()).willReturn(ZoneOffset.UTC);
        given(statisticsService.getOverview()).willReturn(StatisticsOverviewResponse.builder()
                .totalUsers(20).successfulBookings(12).activeVenues(5).build());
        given(statisticsService.getRevenueByYear(2025)).willReturn(RevenueStatisticsResponse.builder()
                .year(2025)
                .totalRevenue(new BigDecimal("3000000"))
                .monthlyRevenue(List.of(
                        RevenueStatisticsResponse.MonthlyRevenue.builder()
                                .month(1).revenue(new BigDecimal("1000000")).build(),
                        RevenueStatisticsResponse.MonthlyRevenue.builder()
                                .month(2).revenue(new BigDecimal("3000000")).build()))
                .build());

        mockMvc.perform(get("/admin/statistics").param("year", "2025"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/statistics"))
                .andExpect(model().attribute("selectedYear", 2025))
                .andExpect(model().attributeExists("overview", "revenue", "revenueBars"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("class=\"moderator-layout\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "href=\"/admin/statistics\""
                )))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "class=\"sidebar-link  active\""
                )));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void serviceFailureRendersErrorState() throws Exception {
        given(clock.instant()).willReturn(Instant.parse("2026-08-26T00:00:00Z"));
        given(clock.getZone()).willReturn(ZoneOffset.UTC);
        given(statisticsService.getOverview()).willThrow(new IllegalStateException("database unavailable"));

        mockMvc.perform(get("/admin/statistics"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/statistics"))
                .andExpect(model().attribute("loadError", true));
    }

    @Test
    @WithMockUser(roles = "USER")
    void userCannotViewDashboard() throws Exception {
        mockMvc.perform(get("/admin/statistics")).andExpect(status().isForbidden());
    }

    @Test
    void anonymousCannotViewDashboard() throws Exception {
        mockMvc.perform(get("/admin/statistics")).andExpect(status().isUnauthorized());
    }
}
