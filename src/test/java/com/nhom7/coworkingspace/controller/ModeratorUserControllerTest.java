package com.nhom7.coworkingspace.controller;

import com.nhom7.coworkingspace.config.JwtProperties;
import com.nhom7.coworkingspace.controller.api.ModeratorUserController;
import com.nhom7.coworkingspace.dto.request.UserSearchRequest;
import com.nhom7.coworkingspace.dto.response.PageResponse;
import com.nhom7.coworkingspace.dto.response.UpdateUserStatusResponse;
import com.nhom7.coworkingspace.dto.response.UpdateUserVerificationResponse;
import com.nhom7.coworkingspace.dto.response.UserSearchResponse;
import com.nhom7.coworkingspace.enums.UserStatus;
import com.nhom7.coworkingspace.security.CustomUserDetailsService;
import com.nhom7.coworkingspace.security.JwtAuthenticationFilter;
import com.nhom7.coworkingspace.security.JwtTokenProvider;
import com.nhom7.coworkingspace.service.TokenBlacklistService;
import com.nhom7.coworkingspace.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@WebMvcTest(ModeratorUserController.class)
@EnableMethodSecurity
@Import({ JwtAuthenticationFilter.class, JwtProperties.class })
@DisplayName("ModeratorUserController - WebMvc & Security Tests")
class ModeratorUserControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private UserService userService;

        @MockBean
        private JwtTokenProvider jwtTokenProvider;

        @MockBean
        private CustomUserDetailsService customUserDetailsService;

        @MockBean
        private TokenBlacklistService tokenBlacklistService;

        @Test
        @WithMockUser(username = "moderator@test.com", roles = { "MODERATOR" })
        @DisplayName("Authenticated MODERATOR -> GET /api/moderator/users returns 200 OK with PageResponse")
        void givenModeratorRole_whenSearchUsers_thenReturn200() throws Exception {
                UserSearchResponse userDto = UserSearchResponse.builder()
                                .id(1L)
                                .name("Nguyen Van A")
                                .email("user@test.com")
                                .phone("0912345678")
                                .status(UserStatus.ACTIVE)
                                .roles(Set.of("USER"))
                                .build();

                PageResponse<UserSearchResponse> pageResponse = PageResponse.<UserSearchResponse>builder()
                                .content(List.of(userDto))
                                .pageNumber(0)
                                .pageSize(10)
                                .totalElements(1)
                                .totalPages(1)
                                .last(true)
                                .build();

                given(userService.searchUsers(any(UserSearchRequest.class), eq("moderator@test.com")))
                                .willReturn(pageResponse);

                mockMvc.perform(get("/api/moderator/users")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.data.content[0].id").value(1))
                                .andExpect(jsonPath("$.data.content[0].name").value("Nguyen Van A"))
                                .andExpect(jsonPath("$.data.content[0].email").value("user@test.com"))
                                .andExpect(jsonPath("$.data.totalElements").value(1));
        }

        @Test
        @WithMockUser(username = "admin@test.com", roles = { "ADMIN" })
        @DisplayName("Authenticated ADMIN -> GET /api/moderator/users returns 200 OK")
        void givenAdminRole_whenSearchUsers_thenReturn200() throws Exception {
                PageResponse<UserSearchResponse> pageResponse = PageResponse.<UserSearchResponse>builder()
                                .content(List.of())
                                .pageNumber(0)
                                .pageSize(10)
                                .totalElements(0)
                                .totalPages(0)
                                .last(true)
                                .build();

                given(userService.searchUsers(any(UserSearchRequest.class), eq("admin@test.com")))
                                .willReturn(pageResponse);

                mockMvc.perform(get("/api/moderator/users")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        @WithMockUser(username = "user@test.com", roles = { "USER" })
        @DisplayName("Authenticated USER -> GET /api/moderator/users returns 403 Forbidden")
        void givenUserRole_whenSearchUsers_thenReturn403() throws Exception {
                mockMvc.perform(get("/api/moderator/users")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Unauthenticated request -> GET /api/moderator/users returns 401 Unauthorized")
        void givenUnauthenticated_whenSearchUsers_thenReturn401() throws Exception {
                mockMvc.perform(get("/api/moderator/users")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(username = "moderator@test.com", roles = { "MODERATOR" })
        @DisplayName("Authenticated MODERATOR -> PUT /api/moderator/users/{id}/status returns 200 OK")
        void givenModeratorRole_whenUpdateUserStatus_thenReturn200() throws Exception {
                UpdateUserStatusResponse response = UpdateUserStatusResponse.builder()
                                .id(5L)
                                .name("Target User")
                                .email("target@test.com")
                                .status(UserStatus.BLOCKED)
                                .roles(Set.of("USER"))
                                .build();

                given(userService.updateUserStatus(eq(5L), eq(UserStatus.BLOCKED), eq("moderator@test.com")))
                                .willReturn(response);

                mockMvc.perform(put("/api/moderator/users/5/status")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\": \"BLOCKED\"}"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.data.id").value(5))
                                .andExpect(jsonPath("$.data.status").value("BLOCKED"));
        }

        @Test
        @WithMockUser(username = "moderator@test.com", roles = { "MODERATOR" })
        @DisplayName("MODERATOR locking another MODERATOR -> PUT /api/moderator/users/{id}/status returns 403 Forbidden")
        void givenModeratorLockingModerator_whenUpdateUserStatus_thenReturn403() throws Exception {
                given(userService.updateUserStatus(eq(5L), eq(UserStatus.BLOCKED), eq("moderator@test.com")))
                                .willThrow(new com.nhom7.coworkingspace.exception.AppException(
                                                "user.cannot.modify.peer.moderator",
                                                org.springframework.http.HttpStatus.FORBIDDEN));

                mockMvc.perform(put("/api/moderator/users/5/status")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\": \"BLOCKED\"}"))
                                .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "admin@test.com", roles = { "ADMIN" })
        @DisplayName("ADMIN locking another ADMIN -> PUT /api/moderator/users/{id}/status returns 403 Forbidden")
        void givenAdminLockingAdmin_whenUpdateUserStatus_thenReturn403() throws Exception {
                given(userService.updateUserStatus(eq(5L), eq(UserStatus.BLOCKED), eq("admin@test.com")))
                                .willThrow(new com.nhom7.coworkingspace.exception.AppException(
                                                "user.cannot.modify.peer.admin",
                                                org.springframework.http.HttpStatus.FORBIDDEN));

                mockMvc.perform(put("/api/moderator/users/5/status")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\": \"BLOCKED\"}"))
                                .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "moderator@test.com", roles = { "MODERATOR" })
        @DisplayName("Invalid status in body -> PUT /api/moderator/users/{id}/status returns 400 Bad Request")
        void givenInvalidStatus_whenUpdateUserStatus_thenReturn400() throws Exception {
                mockMvc.perform(put("/api/moderator/users/5/status")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\": null}"))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = "moderator@test.com", roles = { "MODERATOR" })
        @DisplayName("Authenticated MODERATOR -> PUT /api/moderator/users/{id}/verify-identity returns 200 OK")
        void givenModeratorRole_whenUpdateIdentityVerification_thenReturn200() throws Exception {
                UpdateUserVerificationResponse response = UpdateUserVerificationResponse.builder()
                                .id(5L)
                                .name("Target User")
                                .email("target@test.com")
                                .isIdentityVerified(true)
                                .build();

                given(userService.updateIdentityVerification(eq(5L), eq(true), eq("moderator@test.com")))
                                .willReturn(response);

                mockMvc.perform(put("/api/moderator/users/5/verify-identity")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"verified\": true}"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.data.id").value(5))
                                .andExpect(jsonPath("$.data.isIdentityVerified").value(true));
        }

        @Test
        @WithMockUser(username = "moderator@test.com", roles = { "MODERATOR" })
        @DisplayName("Authenticated MODERATOR -> PUT /api/moderator/users/{id}/verify-business returns 200 OK")
        void givenModeratorRole_whenUpdateBusinessVerification_thenReturn200() throws Exception {
                UpdateUserVerificationResponse response = UpdateUserVerificationResponse.builder()
                                .id(5L)
                                .name("Target User")
                                .email("target@test.com")
                                .isBusinessVerified(true)
                                .build();

                given(userService.updateBusinessVerification(eq(5L), eq(true), eq("moderator@test.com")))
                                .willReturn(response);

                mockMvc.perform(put("/api/moderator/users/5/verify-business")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"verified\": true}"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.data.id").value(5))
                                .andExpect(jsonPath("$.data.isBusinessVerified").value(true));
        }
}
