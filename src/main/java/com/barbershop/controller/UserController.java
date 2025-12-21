package com.barbershop.controller;

import com.barbershop.model.dto.ApiResponse;
import com.barbershop.model.dto.CreateUserRequest;
import com.barbershop.model.dto.UserDTO;
import com.barbershop.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * UserController - REST API endpoints for user management
 * All endpoints require authentication and appropriate roles
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    /**
     * POST /api/users
     * Create a new user
     * Required role: ROLE_ADMIN
     *
     * Request body:
     * - username: Unique username (required)
     * - email: User email (optional)
     * - password: User password (required)
     * - fullName: User full name (optional)
     * - employeeId: Link to employee (optional)
     * - roleNames: Set of role names to assign (optional)
     * - notes: Additional notes (optional)
     */
    @PostMapping
    public ResponseEntity<ApiResponse<UserDTO>> createUser(@RequestBody CreateUserRequest request) {
        log.info("Endpoint: POST /users - Create user: {}", request.getUsername());
        UserDTO createdUser = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(createdUser, "User created successfully"));
    }

    /**
     * GET /api/users/{id}
     * Get user by ID
     * Required role: ROLE_ADMIN, ROLE_MANAGER
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDTO>> getUserById(@PathVariable Long id) {
        log.info("Endpoint: GET /users/{} - Get user by id", id);
        UserDTO user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(user, "User retrieved successfully"));
    }

    /**
     * GET /api/users/username/{username}
     * Get user by username
     * Required role: ROLE_ADMIN, ROLE_MANAGER
     */
    @GetMapping("/username/{username}")
    public ResponseEntity<ApiResponse<UserDTO>> getUserByUsername(@PathVariable String username) {
        log.info("Endpoint: GET /users/username/{} - Get user by username", username);
        UserDTO user = userService.getUserByUsername(username);
        return ResponseEntity.ok(ApiResponse.success(user, "User retrieved successfully"));
    }

    /**
     * GET /api/users
     * Get all users with pagination and search
     * Required role: ROLE_ADMIN, ROLE_MANAGER
     *
     * Query parameters:
     * - page: Page number (0-based, default: 0)
     * - size: Page size (default: 20)
     * - search: Search term to search in username, email, fullName (optional)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<UserDTO>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search) {
        log.info("Endpoint: GET /users - Get all users, page: {}, size: {}, search: {}", page, size, search);
        Page<UserDTO> users = userService.getAllUsers(page, size, search);
        return ResponseEntity.ok(ApiResponse.success(users, "Users retrieved successfully"));
    }

    /**
     * PUT /api/users/{id}
     * Update user
     * Required role: ROLE_ADMIN
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDTO>> updateUser(
            @PathVariable Long id,
            @RequestBody CreateUserRequest request) {
        log.info("Endpoint: PUT /users/{} - Update user", id);
        UserDTO updatedUser = userService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.success(updatedUser, "User updated successfully"));
    }

    /**
     * DELETE /api/users/{id}
     * Delete user (soft delete)
     * Required role: ROLE_ADMIN
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        log.info("Endpoint: DELETE /users/{} - Delete user", id);
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success(null, "User deleted successfully"));
    }

    /**
     * PUT /api/users/{id}/enable
     * Enable/Disable user
     * Required role: ROLE_ADMIN
     *
     * Query parameter:
     * - enabled: true to enable, false to disable
     */
    @PutMapping("/{id}/enable")
    public ResponseEntity<ApiResponse<UserDTO>> enableUser(
            @PathVariable Long id,
            @RequestParam boolean enabled) {
        log.info("Endpoint: PUT /users/{}/enable - Set enabled: {}", id, enabled);
        UserDTO updatedUser = userService.disableUser(id, enabled);
        return ResponseEntity.ok(ApiResponse.success(updatedUser, "User status updated successfully"));
    }

    /**
     * PUT /api/users/{id}/lock
     * Lock/Unlock user account
     * Required role: ROLE_ADMIN
     *
     * Query parameter:
     * - locked: true to lock, false to unlock
     */
    @PutMapping("/{id}/lock")
    public ResponseEntity<ApiResponse<UserDTO>> lockUser(
            @PathVariable Long id,
            @RequestParam boolean locked) {
        log.info("Endpoint: PUT /users/{}/lock - Set locked: {}", id, locked);
        UserDTO updatedUser = userService.lockUser(id, locked);
        return ResponseEntity.ok(ApiResponse.success(updatedUser, "User lock status updated successfully"));
    }

    /**
     * POST /api/users/{id}/roles/{roleName}
     * Add role to user
     * Required role: ROLE_ADMIN
     */
    @PostMapping("/{id}/roles/{roleName}")
    public ResponseEntity<ApiResponse<UserDTO>> addRoleToUser(
            @PathVariable Long id,
            @PathVariable String roleName) {
        log.info("Endpoint: POST /users/{}/roles/{} - Add role to user", id, roleName);
        UserDTO updatedUser = userService.addRoleToUser(id, roleName);
        return ResponseEntity.ok(ApiResponse.success(updatedUser, "Role added to user successfully"));
    }

    /**
     * DELETE /api/users/{id}/roles/{roleName}
     * Remove role from user
     * Required role: ROLE_ADMIN
     */
    @DeleteMapping("/{id}/roles/{roleName}")
    public ResponseEntity<ApiResponse<UserDTO>> removeRoleFromUser(
            @PathVariable Long id,
            @PathVariable String roleName) {
        log.info("Endpoint: DELETE /users/{}/roles/{} - Remove role from user", id, roleName);
        UserDTO updatedUser = userService.removeRoleFromUser(id, roleName);
        return ResponseEntity.ok(ApiResponse.success(updatedUser, "Role removed from user successfully"));
    }
}

