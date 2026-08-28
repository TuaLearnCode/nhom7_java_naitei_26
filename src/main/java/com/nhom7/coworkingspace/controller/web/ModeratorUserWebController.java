package com.nhom7.coworkingspace.controller.web;

import com.nhom7.coworkingspace.dto.request.UserSearchRequest;
import com.nhom7.coworkingspace.dto.response.PageResponse;
import com.nhom7.coworkingspace.dto.response.UserSearchResponse;
import com.nhom7.coworkingspace.enums.UserStatus;
import com.nhom7.coworkingspace.exception.AppException;
import com.nhom7.coworkingspace.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Locale;

@Slf4j
@Controller
@RequestMapping("/moderator/users")
@RequiredArgsConstructor
public class ModeratorUserWebController {

    private final UserService userService;
    private final MessageSource messageSource;

    /**
     * Render HTML page for moderator/admin to view, search, filter, and paginate
     * users.
     *
     * @param request search parameters
     * @param model   Spring MVC model
     * @return Thymeleaf template name
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public String listUsers(
            @ModelAttribute("searchRequest") UserSearchRequest request,
            Authentication authentication,
            Model model) {

        PageResponse<UserSearchResponse> userPage =
                userService.searchUsers(
                        request,
                        authentication.getName()
                );

        model.addAttribute(
                "users",
                userPage
        );

        model.addAttribute(
                "statuses",
                UserStatus.values()
        );

        /*
         * Email của Moderator/Admin đang đăng nhập.
         * users.html dùng giá trị này để xác định
         * tài khoản đang được mở có phải chính mình hay không.
         */
        model.addAttribute(
                "currentUserEmail",
                authentication.getName()
        );

        boolean isCurrentAdmin =
                authentication.getAuthorities()
                        .stream()
                        .anyMatch(authority ->
                                authority.getAuthority()
                                        .equals("ROLE_ADMIN")
                        );

        model.addAttribute(
                "currentUserIsAdmin",
                isCurrentAdmin
        );

        return "moderator/users";
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public String viewUserDetail(
            @PathVariable Long id,
            Model model) {

        model.addAttribute(
                "user",
                userService.getUserById(id)
        );

        model.addAttribute(
                "availableRoles",
                userService.getAvailableRoleNames()
        );

        return "moderator/user-detail";
    }

    @PostMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public String updateUserRole(
            @PathVariable Long id,
            @RequestParam("role") String role,
            RedirectAttributes redirectAttributes) {

        Locale locale =
                LocaleContextHolder.getLocale();

        try {

            userService.changeRole(
                    id,
                    role
            );

            String successMsg =
                    messageSource.getMessage(
                            "user.role.updated",
                            null,
                            locale
                    );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    successMsg
            );

        } catch (AppException ex) {

            log.warn(
                    "[ModeratorUserWebController] Failed to update user role (id={}): {}",
                    id,
                    ex.getMessageKey()
            );

            String errorMsg =
                    messageSource.getMessage(
                            ex.getMessageKey(),
                            null,
                            ex.getMessageKey(),
                            locale
                    );

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    errorMsg
            );

        } catch (Exception ex) {

            log.error(
                    "[ModeratorUserWebController] Unexpected error updating role (id={}): {}",
                    id,
                    ex.getMessage(),
                    ex
            );

            String errorMsg =
                    messageSource.getMessage(
                            "common.error",
                            null,
                            locale
                    );

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    errorMsg
            );
        }

        return "redirect:/moderator/users/" + id;
    }

    // Handle form submission to update user status for moderator/admin.
    @PostMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public String updateUserStatus(
            @PathVariable Long id,
            @RequestParam("status") UserStatus status,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        Locale locale =
                LocaleContextHolder.getLocale();

        try {

            userService.updateUserStatus(
                    id,
                    status,
                    authentication.getName()
            );

            String successMsg =
                    messageSource.getMessage(
                            "user.status.updated",
                            null,
                            locale
                    );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    successMsg
            );

        } catch (AppException ex) {

            log.warn(
                    "[ModeratorUserWebController] Failed to update user status (id={}): {}",
                    id,
                    ex.getMessageKey()
            );

            String errorMsg =
                    messageSource.getMessage(
                            ex.getMessageKey(),
                            null,
                            ex.getMessageKey(),
                            locale
                    );

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    errorMsg
            );

        } catch (Exception ex) {

            log.error(
                    "[ModeratorUserWebController] Unexpected error updating status (id={}): {}",
                    id,
                    ex.getMessage(),
                    ex
            );

            String errorMsg =
                    messageSource.getMessage(
                            "common.error",
                            null,
                            locale
                    );

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    errorMsg
            );
        }

        return "redirect:/moderator/users";
    }

    // Handle form submission to update identity verification (CCCD KYC).
    @PostMapping("/{id}/verify-identity")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public String updateIdentityVerification(
            @PathVariable Long id,
            @RequestParam("verified") Boolean verified,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        Locale locale =
                LocaleContextHolder.getLocale();

        try {

            userService.updateIdentityVerification(
                    id,
                    verified,
                    authentication.getName()
            );

            String successMsg =
                    messageSource.getMessage(
                            "user.identity.verified.updated",
                            null,
                            locale
                    );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    successMsg
            );

        } catch (AppException ex) {

            log.warn(
                    "[ModeratorUserWebController] Failed to update identity verification (id={}): {}",
                    id,
                    ex.getMessageKey()
            );

            String errorMsg =
                    messageSource.getMessage(
                            ex.getMessageKey(),
                            null,
                            ex.getMessageKey(),
                            locale
                    );

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    errorMsg
            );

        } catch (Exception ex) {

            log.error(
                    "[ModeratorUserWebController] Unexpected error updating identity verification (id={}): {}",
                    id,
                    ex.getMessage(),
                    ex
            );

            String errorMsg =
                    messageSource.getMessage(
                            "common.error",
                            null,
                            locale
                    );

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    errorMsg
            );
        }

        return "redirect:/moderator/users";
    }

    // Handle form submission to update business verification (Business License KYC).
    @PostMapping("/{id}/verify-business")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public String updateBusinessVerification(
            @PathVariable Long id,
            @RequestParam("verified") Boolean verified,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        Locale locale =
                LocaleContextHolder.getLocale();

        try {

            userService.updateBusinessVerification(
                    id,
                    verified,
                    authentication.getName()
            );

            String successMsg =
                    messageSource.getMessage(
                            "user.business.verified.updated",
                            null,
                            locale
                    );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    successMsg
            );

        } catch (AppException ex) {

            log.warn(
                    "[ModeratorUserWebController] Failed to update business verification (id={}): {}",
                    id,
                    ex.getMessageKey()
            );

            String errorMsg =
                    messageSource.getMessage(
                            ex.getMessageKey(),
                            null,
                            ex.getMessageKey(),
                            locale
                    );

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    errorMsg
            );

        } catch (Exception ex) {

            log.error(
                    "[ModeratorUserWebController] Unexpected error updating business verification (id={}): {}",
                    id,
                    ex.getMessage(),
                    ex
            );

            String errorMsg =
                    messageSource.getMessage(
                            "common.error",
                            null,
                            locale
                    );

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    errorMsg
            );
        }

        return "redirect:/moderator/users";
    }
}