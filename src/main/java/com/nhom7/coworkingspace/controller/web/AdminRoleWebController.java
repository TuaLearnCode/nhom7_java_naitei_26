package com.nhom7.coworkingspace.controller.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/roles")
public class AdminRoleWebController {

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String roleManagementPage() {
        return "admin/roles";
    }
}
