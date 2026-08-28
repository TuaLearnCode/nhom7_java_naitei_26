package com.nhom7.coworkingspace.controller;

import com.nhom7.coworkingspace.config.JwtProperties;
import com.nhom7.coworkingspace.controller.web.ModeratorUserWebController;
import com.nhom7.coworkingspace.dto.request.UserSearchRequest;
import com.nhom7.coworkingspace.dto.response.PageResponse;
import com.nhom7.coworkingspace.dto.response.UserSearchResponse;
import com.nhom7.coworkingspace.enums.UserStatus;
import com.nhom7.coworkingspace.security.CustomUserDetailsService;
import com.nhom7.coworkingspace.security.JwtAuthenticationFilter;
import com.nhom7.coworkingspace.security.JwtTokenProvider;
import com.nhom7.coworkingspace.service.TokenBlacklistService;
import com.nhom7.coworkingspace.service.UserService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.mockito.ArgumentCaptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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


@WebMvcTest(ModeratorUserWebController.class)
@EnableMethodSecurity
@Import({
        JwtAuthenticationFilter.class,
        JwtProperties.class
})
@DisplayName(
        "ModeratorUserWebController - Thymeleaf Web MVC & Security Tests"
)
class ModeratorUserWebControllerTest {

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


    // =========================================================
    // TASK #99297 - LIST USERS
    // =========================================================

    @Test
    @WithMockUser(
            username = "moderator@test.com",
            roles = {"MODERATOR"}
    )
    @DisplayName(
            "MODERATOR -> GET /moderator/users renders user list"
    )
    void givenModerator_whenListUsers_thenReturnUserListView()
            throws Exception {

        UserSearchResponse user =
                UserSearchResponse.builder()
                        .id(1L)
                        .name("Nguyen Van A")
                        .email("user@test.com")
                        .phone("0123456789")
                        .status(UserStatus.ACTIVE)
                        .roles(Set.of("USER"))
                        .build();

        PageResponse<UserSearchResponse> pageResponse =
                PageResponse.<UserSearchResponse>builder()
                        .content(List.of(user))
                        .pageNumber(0)
                        .pageSize(20)
                        .totalElements(1)
                        .totalPages(1)
                        .last(true)
                        .build();

        given(
                userService.searchUsers(
                        any(UserSearchRequest.class),
                        eq("moderator@test.com")
                )
        ).willReturn(pageResponse);


        mockMvc.perform(
                        get("/moderator/users")
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        view().name("moderator/users")
                )
                .andExpect(
                        model().attributeExists("users")
                )
                .andExpect(
                        model().attributeExists("statuses")
                )
                .andExpect(
                        model().attributeExists("searchRequest")
                );
    }


    @Test
    @WithMockUser(
            username = "moderator@test.com",
            roles = {"MODERATOR"}
    )
    @DisplayName(
            "MODERATOR -> own account row is marked as self for drawer permissions"
    )
    void givenCurrentModeratorInList_whenListUsers_thenMarkOwnAccountAsSelf()
            throws Exception {

        UserSearchResponse currentUser =
                UserSearchResponse.builder()
                        .id(5L)
                        .name("Current Moderator")
                        .email("moderator@test.com")
                        .status(UserStatus.ACTIVE)
                        .roles(Set.of("MODERATOR"))
                        .build();

        UserSearchResponse otherUser =
                UserSearchResponse.builder()
                        .id(6L)
                        .name("Other User")
                        .email("user@test.com")
                        .status(UserStatus.ACTIVE)
                        .roles(Set.of("USER"))
                        .build();

        PageResponse<UserSearchResponse> pageResponse =
                PageResponse.<UserSearchResponse>builder()
                        .content(List.of(currentUser, otherUser))
                        .pageNumber(0)
                        .pageSize(20)
                        .totalElements(2)
                        .totalPages(1)
                        .last(true)
                        .build();

        given(
                userService.searchUsers(
                        any(UserSearchRequest.class),
                        eq("moderator@test.com")
                )
        ).willReturn(pageResponse);

        mockMvc.perform(
                        get("/moderator/users")
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        model().attribute(
                                "currentUserEmail",
                                "moderator@test.com"
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "data-is-self=\"true\""
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "data-is-self=\"false\""
                                )
                        )
                );
    }


    @Test
    @WithMockUser(
            username = "admin@test.com",
            roles = {"ADMIN"}
    )
    @DisplayName(
            "ADMIN -> GET /moderator/users renders user list"
    )
    void givenAdmin_whenListUsers_thenReturnUserListView()
            throws Exception {

        given(
                userService.searchUsers(
                        any(UserSearchRequest.class),
                        eq("admin@test.com")
                )
        ).willReturn(emptyPage());


        mockMvc.perform(
                        get("/moderator/users")
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        view().name("moderator/users")
                );
    }


    @Test
    @WithMockUser(
            username = "moderator@test.com",
            roles = {"MODERATOR"}
    )
    @DisplayName(
            "MODERATOR -> search users by keyword"
    )
    void givenKeyword_whenListUsers_thenBindKeyword()
            throws Exception {

        given(
                userService.searchUsers(
                        any(UserSearchRequest.class),
                        eq("moderator@test.com")
                )
        ).willReturn(emptyPage());


        mockMvc.perform(
                        get("/moderator/users")
                                .param(
                                        "keyword",
                                        "hieu"
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        view().name("moderator/users")
                );


        ArgumentCaptor<UserSearchRequest> captor =
                ArgumentCaptor.forClass(
                        UserSearchRequest.class
                );

        verify(
                userService
        ).searchUsers(
                captor.capture(),
                eq("moderator@test.com")
        );

        assertEquals(
                "hieu",
                captor.getValue().getKeyword()
        );
    }


    @Test
    @WithMockUser(
            username = "moderator@test.com",
            roles = {"MODERATOR"}
    )
    @DisplayName(
            "MODERATOR -> filter users by ACTIVE"
    )
    void givenActiveStatus_whenListUsers_thenBindActiveStatus()
            throws Exception {

        given(
                userService.searchUsers(
                        any(UserSearchRequest.class),
                        eq("moderator@test.com")
                )
        ).willReturn(emptyPage());


        mockMvc.perform(
                        get("/moderator/users")
                                .param(
                                        "status",
                                        "ACTIVE"
                                )
                )
                .andExpect(
                        status().isOk()
                );


        ArgumentCaptor<UserSearchRequest> captor =
                ArgumentCaptor.forClass(
                        UserSearchRequest.class
                );

        verify(
                userService
        ).searchUsers(
                captor.capture(),
                eq("moderator@test.com")
        );

        assertEquals(
                UserStatus.ACTIVE,
                captor.getValue().getStatus()
        );
    }


    @Test
    @WithMockUser(
            username = "moderator@test.com",
            roles = {"MODERATOR"}
    )
    @DisplayName(
            "MODERATOR -> filter users by INACTIVE"
    )
    void givenInactiveStatus_whenListUsers_thenBindInactiveStatus()
            throws Exception {

        given(
                userService.searchUsers(
                        any(UserSearchRequest.class),
                        eq("moderator@test.com")
                )
        ).willReturn(emptyPage());


        mockMvc.perform(
                        get("/moderator/users")
                                .param(
                                        "status",
                                        "INACTIVE"
                                )
                )
                .andExpect(
                        status().isOk()
                );


        ArgumentCaptor<UserSearchRequest> captor =
                ArgumentCaptor.forClass(
                        UserSearchRequest.class
                );

        verify(
                userService
        ).searchUsers(
                captor.capture(),
                eq("moderator@test.com")
        );

        assertEquals(
                UserStatus.INACTIVE,
                captor.getValue().getStatus()
        );
    }


    @Test
    @WithMockUser(
            username = "moderator@test.com",
            roles = {"MODERATOR"}
    )
    @DisplayName(
            "MODERATOR -> filter users by BLOCKED"
    )
    void givenBlockedStatus_whenListUsers_thenBindBlockedStatus()
            throws Exception {

        given(
                userService.searchUsers(
                        any(UserSearchRequest.class),
                        eq("moderator@test.com")
                )
        ).willReturn(emptyPage());


        mockMvc.perform(
                        get("/moderator/users")
                                .param(
                                        "status",
                                        "BLOCKED"
                                )
                )
                .andExpect(
                        status().isOk()
                );


        ArgumentCaptor<UserSearchRequest> captor =
                ArgumentCaptor.forClass(
                        UserSearchRequest.class
                );

        verify(
                userService
        ).searchUsers(
                captor.capture(),
                eq("moderator@test.com")
        );

        assertEquals(
                UserStatus.BLOCKED,
                captor.getValue().getStatus()
        );
    }


    @Test
    @WithMockUser(
            username = "moderator@test.com",
            roles = {"MODERATOR"}
    )
    @DisplayName(
            "MODERATOR -> search and filter users together"
    )
    void givenKeywordAndStatus_whenListUsers_thenBindBoth()
            throws Exception {

        given(
                userService.searchUsers(
                        any(UserSearchRequest.class),
                        eq("moderator@test.com")
                )
        ).willReturn(emptyPage());


        mockMvc.perform(
                        get("/moderator/users")
                                .param(
                                        "keyword",
                                        "nguyen"
                                )
                                .param(
                                        "status",
                                        "ACTIVE"
                                )
                )
                .andExpect(
                        status().isOk()
                );


        ArgumentCaptor<UserSearchRequest> captor =
                ArgumentCaptor.forClass(
                        UserSearchRequest.class
                );

        verify(
                userService
        ).searchUsers(
                captor.capture(),
                eq("moderator@test.com")
        );

        UserSearchRequest request =
                captor.getValue();

        assertEquals(
                "nguyen",
                request.getKeyword()
        );

        assertEquals(
                UserStatus.ACTIVE,
                request.getStatus()
        );
    }


    @Test
    @WithMockUser(
            username = "moderator@test.com",
            roles = {"MODERATOR"}
    )
    @DisplayName(
            "MODERATOR -> paginate user list"
    )
    void givenPageAndSize_whenListUsers_thenBindPagination()
            throws Exception {

        given(
                userService.searchUsers(
                        any(UserSearchRequest.class),
                        eq("moderator@test.com")
                )
        ).willReturn(
                PageResponse.<UserSearchResponse>builder()
                        .content(List.of())
                        .pageNumber(1)
                        .pageSize(10)
                        .totalElements(15)
                        .totalPages(2)
                        .last(true)
                        .build()
        );


        mockMvc.perform(
                        get("/moderator/users")
                                .param(
                                        "page",
                                        "1"
                                )
                                .param(
                                        "size",
                                        "10"
                                )
                )
                .andExpect(
                        status().isOk()
                );


        ArgumentCaptor<UserSearchRequest> captor =
                ArgumentCaptor.forClass(
                        UserSearchRequest.class
                );

        verify(
                userService
        ).searchUsers(
                captor.capture(),
                eq("moderator@test.com")
        );

        UserSearchRequest request =
                captor.getValue();

        assertEquals(
                1,
                request.getPage()
        );

        assertEquals(
                10,
                request.getSize()
        );
    }


    @Test
    @WithMockUser(
            username = "moderator@test.com",
            roles = {"MODERATOR"}
    )
    @DisplayName(
            "MODERATOR -> pagination keeps keyword and status"
    )
    void givenPaginationAndFilter_whenListUsers_thenBindAllParams()
            throws Exception {

        given(
                userService.searchUsers(
                        any(UserSearchRequest.class),
                        eq("moderator@test.com")
                )
        ).willReturn(
                PageResponse.<UserSearchResponse>builder()
                        .content(List.of())
                        .pageNumber(1)
                        .pageSize(10)
                        .totalElements(15)
                        .totalPages(2)
                        .last(true)
                        .build()
        );


        mockMvc.perform(
                        get("/moderator/users")
                                .param(
                                        "page",
                                        "1"
                                )
                                .param(
                                        "size",
                                        "10"
                                )
                                .param(
                                        "keyword",
                                        "hieu"
                                )
                                .param(
                                        "status",
                                        "BLOCKED"
                                )
                )
                .andExpect(
                        status().isOk()
                );


        ArgumentCaptor<UserSearchRequest> captor =
                ArgumentCaptor.forClass(
                        UserSearchRequest.class
                );

        verify(
                userService
        ).searchUsers(
                captor.capture(),
                eq("moderator@test.com")
        );

        UserSearchRequest request =
                captor.getValue();

        assertEquals(
                1,
                request.getPage()
        );

        assertEquals(
                10,
                request.getSize()
        );

        assertEquals(
                "hieu",
                request.getKeyword()
        );

        assertEquals(
                UserStatus.BLOCKED,
                request.getStatus()
        );
    }


    // =========================================================
    // SECURITY REGRESSION
    // =========================================================

    @Test
    @WithMockUser(
            username = "user@test.com",
            roles = {"USER"}
    )
    @DisplayName(
            "USER -> GET /moderator/users returns 403"
    )
    void givenUserRole_whenListUsers_thenReturn403()
            throws Exception {

        mockMvc.perform(
                        get("/moderator/users")
                )
                .andExpect(
                        status().isForbidden()
                );
    }


    @Test
    @DisplayName(
            "Unauthenticated -> GET /moderator/users returns 401"
    )
    void givenUnauthenticated_whenListUsers_thenReturn401()
            throws Exception {

        mockMvc.perform(
                        get("/moderator/users")
                )
                .andExpect(
                        status().isUnauthorized()
                );
    }


    // =========================================================
    // EXISTING STATUS UPDATE REGRESSION
    // =========================================================

    @Test
    @WithMockUser(
            username = "moderator@test.com",
            roles = {"MODERATOR"}
    )
    @DisplayName(
            "MODERATOR -> update user status and redirect"
    )
    void givenModeratorRole_whenUpdateUserStatus_thenRedirect()
            throws Exception {

        mockMvc.perform(
                        post(
                                "/moderator/users/5/status"
                        )
                                .with(csrf())
                                .param(
                                        "status",
                                        "BLOCKED"
                                )
                )
                .andExpect(
                        status().is3xxRedirection()
                )
                .andExpect(
                        redirectedUrl(
                                "/moderator/users"
                        )
                )
                .andExpect(
                        flash().attributeExists(
                                "successMessage"
                        )
                );
    }


    @Test
    @WithMockUser(
            username = "user@test.com",
            roles = {"USER"}
    )
    @DisplayName(
            "USER -> update status returns 403"
    )
    void givenUserRole_whenUpdateUserStatus_thenReturn403()
            throws Exception {

        mockMvc.perform(
                        post(
                                "/moderator/users/5/status"
                        )
                                .with(csrf())
                                .param(
                                        "status",
                                        "BLOCKED"
                                )
                )
                .andExpect(
                        status().isForbidden()
                );
    }


    // =========================================================
    // EXISTING KYC REGRESSION
    // =========================================================

    @Test
    @WithMockUser(
            username = "moderator@test.com",
            roles = {"MODERATOR"}
    )
    @DisplayName(
            "MODERATOR -> update identity verification and redirect"
    )
    void givenModeratorRole_whenUpdateIdentityVerification_thenRedirect()
            throws Exception {

        mockMvc.perform(
                        post(
                                "/moderator/users/5/verify-identity"
                        )
                                .with(csrf())
                                .param(
                                        "verified",
                                        "true"
                                )
                )
                .andExpect(
                        status().is3xxRedirection()
                )
                .andExpect(
                        redirectedUrl(
                                "/moderator/users"
                        )
                )
                .andExpect(
                        flash().attributeExists(
                                "successMessage"
                        )
                );
    }


    @Test
    @WithMockUser(
            username = "moderator@test.com",
            roles = {"MODERATOR"}
    )
    @DisplayName(
            "MODERATOR -> update business verification and redirect"
    )
    void givenModeratorRole_whenUpdateBusinessVerification_thenRedirect()
            throws Exception {

        mockMvc.perform(
                        post(
                                "/moderator/users/5/verify-business"
                        )
                                .with(csrf())
                                .param(
                                        "verified",
                                        "true"
                                )
                )
                .andExpect(
                        status().is3xxRedirection()
                )
                .andExpect(
                        redirectedUrl(
                                "/moderator/users"
                        )
                )
                .andExpect(
                        flash().attributeExists(
                                "successMessage"
                        )
                );
    }


    // =========================================================
    // USER DETAIL / ROLE MANAGEMENT
    // =========================================================

    @Test
    @WithMockUser(
            username = "moderator@test.com",
            roles = {"MODERATOR"}
    )
    @DisplayName(
            "Authenticated MODERATOR -> GET /moderator/users renders template with model attributes"
    )
    void givenModeratorRole_whenListUsers_thenReturnViewWithModel()
            throws Exception {

        UserSearchResponse userDto =
                UserSearchResponse.builder()
                        .id(1L)
                        .name("Nguyen Van A")
                        .email("user@test.com")
                        .status(UserStatus.ACTIVE)
                        .roles(Set.of("USER"))
                        .build();

        PageResponse<UserSearchResponse> pageResponse =
                PageResponse.<UserSearchResponse>builder()
                        .content(List.of(userDto))
                        .pageNumber(0)
                        .pageSize(10)
                        .totalElements(1)
                        .totalPages(1)
                        .last(true)
                        .build();

        given(
                userService.searchUsers(
                        any(UserSearchRequest.class),
                        eq("moderator@test.com")
                )
        ).willReturn(pageResponse);

        mockMvc.perform(
                        get("/moderator/users")
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        view().name(
                                "moderator/users"
                        )
                )
                .andExpect(
                        model().attributeExists(
                                "users"
                        )
                )
                .andExpect(
                        model().attributeExists(
                                "statuses"
                        )
                )
                .andExpect(
                        model().attributeExists(
                                "searchRequest"
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "/moderator/users/1"
                                )
                        )
                );
    }


    @Test
    @WithMockUser(
            username = "admin@test.com",
            roles = {"ADMIN"}
    )
    @DisplayName(
            "ADMIN -> GET user detail renders assignable role options without ADMIN"
    )
    void givenAdminRole_whenViewUserDetail_thenReturnViewWithRoleOptions()
            throws Exception {

        UserSearchResponse userDto =
                UserSearchResponse.builder()
                        .id(5L)
                        .name("Nguyen Van A")
                        .email("user@test.com")
                        .status(UserStatus.ACTIVE)
                        .roles(Set.of("USER"))
                        .build();

        given(
                userService.getUserById(5L)
        ).willReturn(userDto);

        given(
                userService.getAvailableRoleNames()
        ).willReturn(
                List.of(
                        "HOST",
                        "MODERATOR",
                        "USER"
                )
        );

        mockMvc.perform(
                        get("/moderator/users/5")
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        view().name(
                                "moderator/user-detail"
                        )
                )
                .andExpect(
                        model().attribute(
                                "user",
                                userDto
                        )
                )
                .andExpect(
                        model().attribute(
                                "availableRoles",
                                List.of(
                                        "HOST",
                                        "MODERATOR",
                                        "USER"
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "Cập nhật vai trò"
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "/moderator/users/5/role"
                                )
                        )
                );
    }


    @Test
    @WithMockUser(
            username = "moderator@test.com",
            roles = {"MODERATOR"}
    )
    @DisplayName(
            "MODERATOR -> GET user detail hides role update form"
    )
    void givenModeratorRole_whenViewUserDetail_thenHideRoleUpdateForm()
            throws Exception {

        UserSearchResponse userDto =
                UserSearchResponse.builder()
                        .id(5L)
                        .name("Nguyen Van A")
                        .email("user@test.com")
                        .status(UserStatus.ACTIVE)
                        .roles(Set.of("USER"))
                        .build();

        given(
                userService.getUserById(5L)
        ).willReturn(userDto);

        given(
                userService.getAvailableRoleNames()
        ).willReturn(
                List.of(
                        "HOST",
                        "MODERATOR",
                        "USER"
                )
        );

        mockMvc.perform(
                        get("/moderator/users/5")
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        content().string(
                                not(
                                        containsString(
                                                "Cập nhật vai trò"
                                        )
                                )
                        )
                )
                .andExpect(
                        content().string(
                                not(
                                        containsString(
                                                "/moderator/users/5/role"
                                        )
                                )
                        )
                );
    }


    @Test
    @WithMockUser(
            username = "admin@test.com",
            roles = {"ADMIN"}
    )
    @DisplayName(
            "ADMIN -> POST user role changes role and redirects to detail"
    )
    void givenAdminRole_whenUpdateUserRole_thenRedirectToDetail()
            throws Exception {

        mockMvc.perform(
                        post(
                                "/moderator/users/5/role"
                        )
                                .with(csrf())
                                .param(
                                        "role",
                                        "MODERATOR"
                                )
                )
                .andExpect(
                        status().is3xxRedirection()
                )
                .andExpect(
                        redirectedUrl(
                                "/moderator/users/5"
                        )
                )
                .andExpect(
                        flash().attributeExists(
                                "successMessage"
                        )
                );

        verify(userService)
                .changeRole(
                        5L,
                        "MODERATOR"
                );
    }


    @Test
    @WithMockUser(
            username = "moderator@test.com",
            roles = {"MODERATOR"}
    )
    @DisplayName(
            "MODERATOR -> POST user role returns 403 Forbidden"
    )
    void givenModeratorRole_whenUpdateUserRole_thenReturn403()
            throws Exception {

        mockMvc.perform(
                        post(
                                "/moderator/users/5/role"
                        )
                                .with(csrf())
                                .param(
                                        "role",
                                        "ADMIN"
                                )
                )
                .andExpect(
                        status().isForbidden()
                );
    }


    private PageResponse<UserSearchResponse> emptyPage() {

        return PageResponse
                .<UserSearchResponse>builder()
                .content(List.of())
                .pageNumber(0)
                .pageSize(20)
                .totalElements(0)
                .totalPages(0)
                .last(true)
                .build();
    }
}