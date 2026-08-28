package com.nhom7.coworkingspace.config;

import com.nhom7.coworkingspace.security.CustomUserDetailsService;
import com.nhom7.coworkingspace.security.JwtAuthErrorHandler;
import com.nhom7.coworkingspace.security.JwtAuthenticationFilter;
import com.nhom7.coworkingspace.security.JwtTokenProvider;
import com.nhom7.coworkingspace.service.TokenBlacklistService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(
        controllers = SecurityTestController.class,
        properties = {
                "app.cors.allowed-origins=http://localhost:8080"
        }
)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtAuthErrorHandler.class
})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;


    /*
     * Dependencies of JwtAuthenticationFilter.
     *
     * These are mocked because this test only verifies
     * authorization rules in SecurityConfig.
     */
    @MockBean
    private JwtTokenProvider jwtTokenProvider;


    @MockBean
    private CustomUserDetailsService customUserDetailsService;


    @MockBean
    private TokenBlacklistService tokenBlacklistService;


    /*
     * ADMIN inherits MODERATOR permission.
     *
     * Expected:
     * ADMIN -> /api/moderator/** -> allowed
     */
    @Test
    @WithMockUser(
            username = "admin@test.com",
            roles = "ADMIN"
    )
    void adminShouldAccessModeratorApi()
            throws Exception {

        mockMvc.perform(
                        get("/api/moderator/test")
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        content().string("Moderator API")
                );
    }


    /*
     * MODERATOR keeps access to MODERATOR APIs.
     */
    @Test
    @WithMockUser(
            username = "moderator@test.com",
            roles = "MODERATOR"
    )
    void moderatorShouldAccessModeratorApi()
            throws Exception {

        mockMvc.perform(
                        get("/api/moderator/test")
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        content().string("Moderator API")
                );
    }


    /*
     * USER must not access MODERATOR APIs.
     */
    @Test
    @WithMockUser(
            username = "user@test.com",
            roles = "USER"
    )
    void userShouldNotAccessModeratorApi()
            throws Exception {

        mockMvc.perform(
                        get("/api/moderator/test")
                )
                .andExpect(
                        status().isForbidden()
                );
    }


    /*
     * HOST must not access MODERATOR APIs.
     */
    @Test
    @WithMockUser(
            username = "host@test.com",
            roles = "HOST"
    )
    void hostShouldNotAccessModeratorApi()
            throws Exception {

        mockMvc.perform(
                        get("/api/moderator/test")
                )
                .andExpect(
                        status().isForbidden()
                );
    }


    /*
     * Unauthenticated user must not access MODERATOR APIs.
     */
    @Test
    void unauthenticatedUserShouldNotAccessModeratorApi()
            throws Exception {

        mockMvc.perform(
                        get("/api/moderator/test")
                )
                .andExpect(
                        status().isForbidden()
                );
    }


    /*
     * MODERATOR must not inherit ADMIN permission.
     *
     * ADMIN -> MODERATOR : allowed
     * MODERATOR -> ADMIN : forbidden
     */
    @Test
    @WithMockUser(
            username = "moderator@test.com",
            roles = "MODERATOR"
    )
    void moderatorShouldNotAccessAdminApi()
            throws Exception {

        mockMvc.perform(
                        get("/api/admin/test")
                )
                .andExpect(
                        status().isForbidden()
                );
    }
}


/*
 * Controller used only by SecurityConfigTest.
 *
 * Keeping it as a separate package-private class allows
 * @WebMvcTest(controllers = SecurityTestController.class)
 * to load only this controller instead of every controller
 * in the application.
 */
@RestController
class SecurityTestController {

    @GetMapping("/api/moderator/test")
    String moderatorApi() {

        return "Moderator API";
    }


    @GetMapping("/api/admin/test")
    String adminApi() {

        return "Admin API";
    }
}