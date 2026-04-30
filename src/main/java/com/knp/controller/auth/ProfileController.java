package com.knp.controller.auth;

import com.knp.model.dto.ApiResponse;
import com.knp.model.dto.auth.ProfileRequest;
import com.knp.model.dto.auth.UserProfile;
import com.knp.service.auth.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * ProfileController - REST API endpoints for user profile management
 *
 * Provides endpoints for authenticated users to view and manage their profile information.
 * This controller allows users to update their personal preferences, security settings,
 * and profile customization options.
 *
 * All endpoints require authentication via JWT token.
 * Operations are restricted to the currently authenticated user (own profile only).
 * Multi-tenant support is provided via X-Tenant-Id header.
 *
 * Main Responsibilities:
 * - Retrieve user profile information
 * - Update user UI preferences (color scheme, avatar)
 * - Manage user security (password changes)
 * - Support user personalization settings
 *
 * Access Control:
 * - Users can only view and modify their own profile
 * - Attempts to modify other users' profiles will result in ForbiddenException
 * - Authentication is required for all endpoints
 *
 * @author Barber Shop Team
 * @version 1.0
 * @since 2024
 */
@RestController
@RequestMapping("/profiles")
@RequiredArgsConstructor
@Slf4j
public class ProfileController {
    private final ProfileService profileService;

    /**
     * GET /profile
     * Get current authenticated user's profile information
     *
     * @return ApiResponse with UserProfile containing all user profile details
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfile>> getUserProfile() {
        log.info("Request: Get user profile for username: {}", getCurrentUsername());
        String username = getCurrentUsername();
        UserProfile response = profileService.getUserDetail(username);
        log.info("User profile retrieved: {}", username);
        return ResponseEntity.ok(
                ApiResponse.success(response, "User profile retrieved successfully")
        );
    }

    /**
     * PUT /profiles/color
     * Update user profile UI color preference/theme
     *
     * @param request ProfileRequest with color preference value
     * @return ApiResponse with updated UserProfile
     */
    @PutMapping("/color")
    public ResponseEntity<ApiResponse<UserProfile>> updateProfileColor(@RequestBody ProfileRequest request) {
        log.info("Request: Update profile color for username: {}", request.getUsername());
        validateUsername(request.getUsername());
        UserProfile response = profileService.updateProfileColor(getCurrentUsername(), request);
        log.info("Profile color updated: {}", request.getUsername());
        return ResponseEntity.ok(
                ApiResponse.success(response, "Profile color updated successfully")
        );
    }

    /**
     * PUT /profiles/avatar
     * Update user profile avatar/picture
     *
     * @param request ProfileRequest with avatar image data (base64 encoded)
     * @return ApiResponse with updated UserProfile
     */
    @PutMapping("/avatar")
    public ResponseEntity<ApiResponse<UserProfile>> updateProfileAvatar(@RequestBody ProfileRequest request) {
        log.info("Request: Update profile avatar for username: {}", request.getUsername());
        validateUsername(request.getUsername());
        UserProfile response = profileService.updateProfileAvatar(getCurrentUsername(), request);
        log.info("Profile avatar updated: {}", request.getUsername());
        return ResponseEntity.ok(
                ApiResponse.success(response, "Profile avatar updated successfully")
        );
    }

    /**
     * PUT /profiles/password
     * Update user profile password (secure operation)
     *
     * @param request ProfileRequest with oldPassword and newPassword
     * @return ApiResponse with updated UserProfile
     */
    @PutMapping("/password")
    public ResponseEntity<ApiResponse<UserProfile>> updateProfilePassword(@RequestBody ProfileRequest request) {
        log.info("Request: Update profile password for username: {}", request.getUsername());
        validateUsername(request.getUsername());
        UserProfile response = profileService.updateProfilePassword(getCurrentUsername(), request);
        log.info("Profile password updated: {}", request.getUsername());
        return ResponseEntity.ok(
                ApiResponse.success(response, "Profile password updated successfully")
        );
    }

    /**
     * PUT /profiles/info
     * Update user basic info (fullName, email)
     */
    @PutMapping("/info")
    public ResponseEntity<ApiResponse<UserProfile>> updateProfileInfo(@RequestBody ProfileRequest request) {
        log.info("Request: Update profile info for username: {}", request.getUsername());
        validateUsername(request.getUsername());
        UserProfile response = profileService.updateProfileInfo(getCurrentUsername(), request);
        log.info("Profile info updated: {}", request.getUsername());
        return ResponseEntity.ok(
                ApiResponse.success(response, "Profile info updated successfully")
        );
    }

    /**
     * PUT /profiles/lang
     * Update user language preference
     */
    @PutMapping("/lang")
    public ResponseEntity<ApiResponse<UserProfile>> updateProfileLang(@RequestBody ProfileRequest request) {
        log.info("Request: Update language preference for username: {}", request.getUsername());
        validateUsername(request.getUsername());
        UserProfile response = profileService.updateProfileLang(getCurrentUsername(), request);
        log.info("Language preference updated: {}", request.getUsername());
        return ResponseEntity.ok(
                ApiResponse.success(response, "Language preference updated successfully")
        );
    }

    /**
     * Get the current authenticated username
     */
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }

    /**
     * Validate that the request username matches the authenticated user
     * This ensures users can only modify their own profile
     */
    private void validateUsername(String requestUsername) {
        String authenticatedUsername = getCurrentUsername();
        if (!authenticatedUsername.equals(requestUsername)) {
            log.warn("Unauthorized profile update attempt. Requested: {}, Authenticated: {}",
                    requestUsername, authenticatedUsername);
            throw new com.knp.exception.ForbiddenException("error.profile.canOnlyUpdateOwn");
        }
    }
}
