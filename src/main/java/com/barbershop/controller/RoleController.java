package com.barbershop.controller;

import com.barbershop.model.dto.ApiResponse;
import com.barbershop.model.dto.RoleDTO;
import com.barbershop.service.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * RoleController - REST API endpoints for role management
 * Only predefined roles are available: SHOP_OWNER, MANAGER, RECEPTIONIST, CLEANER, TECHNICIAN
 * All endpoints require authentication and appropriate role
 */
@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
@Slf4j
public class RoleController {

    private final RoleService roleService;

    /**
     * GET /api/roles
     * Get all predefined roles available in the system
     * Required role: ROLE_ADMIN, ROLE_MANAGER
     * <p>
     * Returns list of 5 predefined roles:
     * - SHOP_OWNER: Shop Owner - Full access to all features
     * - MANAGER: Manager - Can manage shop, employees, and reports
     * - RECEPTIONIST: Receptionist - Can manage appointments and customers
     * - CLEANER: Cleaner - Can manage cleaning tasks and inventory
     * - TECHNICIAN: Technician/Employee - Can view appointments and customer info
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<RoleDTO>>> getAllRoles() {
        log.info("Endpoint: GET /roles - Get all predefined roles");
        List<RoleDTO> roles = roleService.getAllRoles();
        return ResponseEntity.ok(ApiResponse.success(roles, "Predefined roles retrieved successfully"));
    }

    /**
     * GET /api/roles/{code}
     * Get specific role by code
     * Required role: ROLE_ADMIN, ROLE_MANAGER
     * <p>
     * Path parameter:
     * - code: Role code (SHOP_OWNER, MANAGER, RECEPTIONIST, CLEANER, TECHNICIAN)
     */
    @GetMapping("/{code}")
    public ResponseEntity<ApiResponse<RoleDTO>> getRoleByCode(@PathVariable String code) {
        log.info("Endpoint: GET /roles/{} - Get role by code", code);
        RoleDTO role = roleService.getRoleByCode(code);
        return ResponseEntity.ok(ApiResponse.success(role, "Role retrieved successfully"));
    }
}


