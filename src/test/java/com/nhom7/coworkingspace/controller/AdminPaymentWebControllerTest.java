package com.nhom7.coworkingspace.controller;

import com.nhom7.coworkingspace.config.JwtProperties;
import com.nhom7.coworkingspace.controller.web.AdminPaymentWebController;
import com.nhom7.coworkingspace.dto.request.PaymentSearchRequest;
import com.nhom7.coworkingspace.dto.response.PageResponse;
import com.nhom7.coworkingspace.dto.response.PaymentResponse;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(AdminPaymentWebController.class)
@EnableMethodSecurity
@Import({JwtAuthenticationFilter.class, JwtProperties.class})
@DisplayName("AdminPaymentWebController tests")
class AdminPaymentWebControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private StatisticsService statisticsService;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private CustomUserDetailsService customUserDetailsService;
    @MockBean private TokenBlacklistService tokenBlacklistService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanFilterAndViewPaymentHistory() throws Exception {
        PaymentResponse payment = PaymentResponse.builder()
                .id(1L)
                .bookingId(9L)
                .amount(new BigDecimal("500000"))
                .paymentMethod("BANK_TRANSFER")
                .status("COMPLETED")
                .transactionId("TXN-001")
                .paidAt(LocalDateTime.of(2026, 8, 20, 10, 30))
                .build();
        given(statisticsService.searchPayments(any(PaymentSearchRequest.class)))
                .willReturn(PageResponse.<PaymentResponse>builder()
                        .content(List.of(payment))
                        .pageNumber(0).pageSize(20).totalElements(1).totalPages(1).last(true)
                        .build());

        mockMvc.perform(get("/admin/payments")
                        .param("status", "COMPLETED")
                        .param("fromDate", "2026-08-01")
                        .param("toDate", "2026-08-31"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/payments"))
                .andExpect(model().attributeExists("payments", "searchRequest"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("TXN-001")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("class=\"moderator-layout\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "href=\"/admin/payments\""
                )))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "class=\"sidebar-link  active\""
                )));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void invalidDateRangeRendersValidationErrorWithoutQueryingService() throws Exception {
        mockMvc.perform(get("/admin/payments")
                        .param("fromDate", "2026-08-31")
                        .param("toDate", "2026-08-01"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("validationError", true));

        verify(statisticsService, never()).searchPayments(any(PaymentSearchRequest.class));
    }

    @Test
    @WithMockUser(roles = "USER")
    void userCannotViewPaymentHistory() throws Exception {
        mockMvc.perform(get("/admin/payments")).andExpect(status().isForbidden());
    }

    @Test
    void anonymousCannotViewPaymentHistory() throws Exception {
        mockMvc.perform(get("/admin/payments")).andExpect(status().isUnauthorized());
    }
}
