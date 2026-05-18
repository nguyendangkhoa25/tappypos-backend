package com.tappy.pos.controller.auth;

import com.tappy.pos.config.AuthContext;
import com.tappy.pos.model.dto.ApiResponse;
import com.tappy.pos.model.dto.auth.CreateUserRequest;
import com.tappy.pos.model.dto.auth.UserDetailDTO;
import com.tappy.pos.model.dto.auth.PasswordResetResponse;
import com.tappy.pos.service.auth.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.tappy.pos.annotation.RequiresFeature;

/**
 * UserController - REST API endpoints for user management
 * All endpoints require authentication and appropriate roles
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
@RequiresFeature("USER")
public class UserController {

    private final UserService userService;
    private final AuthContext authContext;

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
    public ResponseEntity<ApiResponse<UserDetailDTO>> createUser(@RequestBody CreateUserRequest request) {
        log.info("Endpoint: POST /users - Create user: {}", request.getUsername());
        UserDetailDTO createdUser = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(createdUser, "User created successfully"));
    }

    /**
     * GET /api/users/{id}
     * Get user by ID
     * Required role: ROLE_ADMIN, ROLE_MANAGER
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDetailDTO>> getUserById(@PathVariable Long id) {
        log.info("Endpoint: GET /users/{} - Get user by id", id);
        UserDetailDTO user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(user, "User retrieved successfully"));
    }

    /**
     * GET /api/users/username/{username}
     * Get user by username
     * Required role: ROLE_ADMIN, ROLE_MANAGER
     */
    @GetMapping("/username/{username}")
    public ResponseEntity<ApiResponse<UserDetailDTO>> getUserByUsername(@PathVariable String username) {
        log.info("Endpoint: GET /users/username/{} - Get user by username", username);
        UserDetailDTO user = userService.getUserByUsername(username);
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
    public ResponseEntity<ApiResponse<Page<UserDetailDTO>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search) {
        log.info("Endpoint: GET /users - Get all users, page: {}, size: {}, search: {}", page, size, search);
        Page<UserDetailDTO> users = userService.getAllUsers(page, size, search);
        return ResponseEntity.ok(ApiResponse.success(users, "Users retrieved successfully"));
    }

    /**
     * PUT /api/users/{id}
     * Update user
     * Required role: ROLE_ADMIN
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDetailDTO>> updateUser(
            @PathVariable Long id,
            @RequestBody CreateUserRequest request) {
        log.info("Endpoint: PUT /users/{} - Update user", id);
        UserDetailDTO updatedUser = userService.updateUser(id, request);
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
    public ResponseEntity<ApiResponse<UserDetailDTO>> enableUser(
            @PathVariable Long id,
            @RequestParam boolean enabled) {
        log.info("Endpoint: PUT /users/{}/enable - Set enabled: {}", id, enabled);
        UserDetailDTO updatedUser = userService.disableUser(id, enabled);
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
    public ResponseEntity<ApiResponse<UserDetailDTO>> lockUser(
            @PathVariable Long id,
            @RequestParam boolean locked) {
        log.info("Endpoint: PUT /users/{}/lock - Set locked: {}", id, locked);
        UserDetailDTO updatedUser = userService.lockUser(id, locked);
        return ResponseEntity.ok(ApiResponse.success(updatedUser, "User lock status updated successfully"));
    }

    /**
     * POST /api/users/{id}/roles/{roleName}
     * Add role to user
     * Required role: ROLE_ADMIN
     */
    @PostMapping("/{id}/roles/{roleName}")
    public ResponseEntity<ApiResponse<UserDetailDTO>> addRoleToUser(
            @PathVariable Long id,
            @PathVariable String roleName) {
        log.info("Endpoint: POST /users/{}/roles/{} - Add role to user", id, roleName);
        UserDetailDTO updatedUser = userService.addRoleToUser(id, roleName);
        return ResponseEntity.ok(ApiResponse.success(updatedUser, "Role added to user successfully"));
    }

    /**
     * DELETE /api/users/{id}/roles/{roleName}
     * Remove role from user
     * Required role: ROLE_ADMIN
     */
    @DeleteMapping("/{id}/roles/{roleName}")
    public ResponseEntity<ApiResponse<UserDetailDTO>> removeRoleFromUser(
            @PathVariable Long id,
            @PathVariable String roleName) {
        log.info("Endpoint: DELETE /users/{}/roles/{} - Remove role from user", id, roleName);
        UserDetailDTO updatedUser = userService.removeRoleFromUser(id, roleName);
        return ResponseEntity.ok(ApiResponse.success(updatedUser, "Role removed from user successfully"));
    }

    /**
     * DELETE /api/users/me
     * Soft-delete the currently authenticated user's own account
     */
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deleteMe() {
        String username = authContext.getCurrentUsername();
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("UNAUTHORIZED", "Not authenticated"));
        }
        log.info("Endpoint: DELETE /users/me - username: {}", username);
        UserDetailDTO user = userService.getUserByUsername(username);
        userService.deleteUser(user.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "Account deleted"));
    }

    /**
     * Reset user password with auto-generated default password
     * Required role: ROLE_ADMIN
     *
     * Path parameter:
     * - id: User ID (required)
     *
     * Response:
     * Returns the newly generated temporary password
     * Sets requireAction to "CHANGE_PASSWORD"
     * User must change password on next login
     *
     * Examples:
     * - POST /api/users/1/reset-password
     */
    @PostMapping("/{id}/reset-password")
    public ResponseEntity<ApiResponse<PasswordResetResponse>> resetUserPassword(@PathVariable Long id) {
        log.info("Endpoint: POST /users/{}/reset-password - Reset user password", id);
        PasswordResetResponse response = userService.resetUserPassword(id);
        return ResponseEntity.ok(ApiResponse.success(response, "User password reset successfully. User must change password on next login"));
    }
}

