package com.nhom7.coworkingspace.controller;

import com.nhom7.coworkingspace.config.JwtProperties;
import com.nhom7.coworkingspace.config.SecurityConfig;
import com.nhom7.coworkingspace.controller.web.AuthWebController;
import com.nhom7.coworkingspace.security.CustomUserDetailsService;
import com.nhom7.coworkingspace.security.JwtAuthErrorHandler;
import com.nhom7.coworkingspace.security.JwtAuthenticationFilter;
import com.nhom7.coworkingspace.security.JwtTokenProvider;
import com.nhom7.coworkingspace.service.TokenBlacklistService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(AuthWebController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtProperties.class
})
@DisplayName("AuthWebController - Session Login Tests")
class WebAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private TokenBlacklistService tokenBlacklistService;

    @MockBean
    private JwtAuthErrorHandler jwtAuthErrorHandler;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("GET /login renders the API-backed moderator login form")
    void givenAnonymousUser_whenOpenLogin_thenRenderLoginForm()
            throws Exception {

        mockMvc.perform(
                        get("/login")
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        view().name(
                                "auth/login"
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "/api/auth/login"
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "/moderator/users"
                                )
                        )
                );
    }

    @Test
    @DisplayName("Valid moderator credentials create a reusable web session")
    void givenValidModeratorCredentials_whenLogin_thenStoreSecurityContextInSession()
            throws Exception {

        given(
                customUserDetailsService.loadUserByUsername(
                        "moderator@test.com"
                )
        ).willReturn(
                org.springframework.security.core.userdetails.User
                        .withUsername(
                                "moderator@test.com"
                        )
                        .password(
                                passwordEncoder.encode(
                                        "secret123"
                                )
                        )
                        .roles(
                                "MODERATOR"
                        )
                        .build()
        );

        MvcResult result =
                mockMvc.perform(
                                post("/login")
                                        .with(csrf())
                                        .param(
                                                "username",
                                                "moderator@test.com"
                                        )
                                        .param(
                                                "password",
                                                "secret123"
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
                        .andReturn();

        assertThat(
                result.getRequest()
                        .getSession(false)
        ).isNotNull();

        assertThat(
                result.getRequest()
                        .getSession(false)
                        .getAttribute(
                                "SPRING_SECURITY_CONTEXT"
                        )
        ).isNotNull();
    }
}