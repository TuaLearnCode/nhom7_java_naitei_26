package com.nhom7.coworkingspace.controller;

import com.nhom7.coworkingspace.config.JwtProperties;
import com.nhom7.coworkingspace.controller.web.AdminRoleWebController;
import com.nhom7.coworkingspace.security.CustomUserDetailsService;
import com.nhom7.coworkingspace.security.JwtAuthenticationFilter;
import com.nhom7.coworkingspace.security.JwtTokenProvider;
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

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(AdminRoleWebController.class)
@EnableMethodSecurity
@Import({JwtAuthenticationFilter.class, JwtProperties.class})
@DisplayName("AdminRoleWebController tests")
class AdminRoleWebControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private CustomUserDetailsService customUserDetailsService;
    @MockBean private TokenBlacklistService tokenBlacklistService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanViewRoleManagementWithActiveSharedSidebar() throws Exception {
        mockMvc.perform(get("/admin/roles"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/roles"))
                .andExpect(content().string(containsString("class=\"moderator-layout\"")))
                .andExpect(content().string(containsString("href=\"/admin/roles\"")))
                .andExpect(content().string(containsString("class=\"sidebar-link  active\"")))
                .andExpect(content().string(containsString("Tìm kiếm")))
                .andExpect(content().string(containsString("/js/admin-roles.js")));
    }

    @Test
    @WithMockUser(roles = "MODERATOR")
    void moderatorCannotViewRoleManagement() throws Exception {
        mockMvc.perform(get("/admin/roles")).andExpect(status().isForbidden());
    }

    @Test
    void anonymousCannotViewRoleManagement() throws Exception {
        mockMvc.perform(get("/admin/roles")).andExpect(status().isUnauthorized());
    }
}
