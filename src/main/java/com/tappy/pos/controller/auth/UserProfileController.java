package com.tappy.pos.controller.auth;

import com.tappy.pos.model.dto.ApiResponse;
import com.tappy.pos.model.dto.auth.ChangePasswordRequest;
import com.tappy.pos.model.dto.auth.UserDetailDTO;
import com.tappy.pos.service.auth.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Password-change endpoints for any authenticated user regardless of role or feature set.
 * No @RequiresFeature — password management is part of authentication, not a shop feature.
 * This allows master users, agent users, and shop users all to change their own password.
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
public class UserProfileController {

    private final UserService userService;

    @PostMapping("/change-password-first-login")
    public ResponseEntity<ApiResponse<UserDetailDTO>> changePasswordOnFirstLogin(@RequestBody ChangePasswordRequest request) {
        log.info("Endpoint: POST /users/change-password-first-login - Change password on first login");
        UserDetailDTO updatedUser = userService.changePasswordOnFirstLogin(request);
        return ResponseEntity.ok(ApiResponse.success(updatedUser, "Password changed successfully on first login"));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<UserDetailDTO>> changePassword(@RequestBody ChangePasswordRequest request) {
        log.info("Endpoint: POST /users/change-password - Change password with old password verification");
        UserDetailDTO updatedUser = userService.changePassword(request);
        return ResponseEntity.ok(ApiResponse.success(updatedUser, "Password changed successfully"));
    }
}
