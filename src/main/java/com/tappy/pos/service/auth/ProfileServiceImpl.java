package com.tappy.pos.service.auth;

import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.exception.ForbiddenException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.exception.UnauthorizedException;
import com.tappy.pos.model.dto.auth.ProfileRequest;
import com.tappy.pos.model.dto.auth.UserProfile;
import com.tappy.pos.model.entity.auth.Role;
import com.tappy.pos.model.entity.auth.User;
import com.tappy.pos.model.enums.ActivityAction;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.auth.UserRepository;
import com.tappy.pos.service.audit.ActivityLogService;
import com.tappy.pos.service.storage.R2CleanupService;
import com.tappy.pos.service.storage.R2StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.stream.Collectors;

/**
 * ProfileServiceImpl
 * Implementation for user profile management operations
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ProfileServiceImpl implements ProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MessageService messageService;
    private final R2StorageService r2StorageService;
    private final R2CleanupService r2CleanupService;
    private final ActivityLogService activityLogService;
    private final TenantContext tenantContext;

    /**
     * Get user profile by username
     */
    @Override
    public UserProfile getUserDetail(String username) {
        log.info("Fetching user profile for username: {}", username);

        User user = userRepository.findByUsernameActive(username)
                .orElseThrow(() -> {
                    log.error("User not found: {}", username);
                    String message = messageService.getMessage("error.user.not.found", username);
                    return new ResourceNotFoundException(message);
                });

        log.info("User profile fetched successfully: {}", username);
        return mapToUserProfile(user);
    }

    /**
     * Update user profile color preference
     */
    @Override
    public UserProfile updateProfileColor(String username, ProfileRequest request) {
        log.info("Updating color preference for user: {}", username);

        // Validate that user can only update their own profile
        if (!username.equals(request.getUsername())) {
            log.warn("Unauthorized attempt to update color preference for user: {}", request.getUsername());
            throw new ForbiddenException(messageService.getMessage("error.permission.denied"));
        }

        User user = userRepository.findByUsernameActive(username)
                .orElseThrow(() -> {
                    log.error("User not found: {}", username);
                    String message = messageService.getMessage("error.user.not.found", username);
                    return new ResourceNotFoundException(message);
                });

        user.setColorPreference(request.getColorPreference());
        User updatedUser = userRepository.save(user);
        log.info("Color preference updated for user: {}", username);
        return mapToUserProfile(updatedUser);
    }

    /**
     * Update user profile avatar
     */
    @Override
    public UserProfile updateProfileAvatar(String username, ProfileRequest request) {
        log.info("Updating avatar for user: {}", username);

        // Validate that user can only update their own profile
        if (!username.equals(request.getUsername())) {
            log.warn("Unauthorized attempt to update avatar for user: {}", request.getUsername());
            throw new ForbiddenException(messageService.getMessage("error.permission.denied"));
        }

        User user = userRepository.findByUsernameActive(username)
                .orElseThrow(() -> {
                    log.error("User not found: {}", username);
                    String message = messageService.getMessage("error.user.not.found", username);
                    return new ResourceNotFoundException(message);
                });

        // Validate avatar is not empty
        if (request.getAvatar() == null || request.getAvatar().trim().isEmpty()) {
            log.warn("Avatar cannot be empty for user: {}", username);
            String message = messageService.getMessage("error.avatar.empty");
            throw new BadRequestException(message);
        }

        user.setAvatar(request.getAvatar());
        User updatedUser = userRepository.save(user);
        log.info("Avatar updated for user: {}", username);
        return mapToUserProfile(updatedUser);
    }

    /**
     * Update user profile password
     */
    @Override
    public UserProfile updateProfilePassword(String username, ProfileRequest request) {
        log.info("Updating password for user: {}", username);

        // Validate that user can only update their own profile
        if (!username.equals(request.getUsername())) {
            log.warn("Unauthorized attempt to update password for user: {}", request.getUsername());
            throw new ForbiddenException(messageService.getMessage("error.permission.denied"));
        }

        User user = userRepository.findByUsernameActive(username)
                .orElseThrow(() -> {
                    log.error("User not found: {}", username);
                    String message = messageService.getMessage("error.user.not.found", username);
                    return new ResourceNotFoundException(message);
                });

        // Validate old password
        if (request.getOldPassword() == null || request.getOldPassword().trim().isEmpty()) {
            log.warn("Old password not provided for user: {}", username);
            String message = messageService.getMessage("error.old.password.required");
            throw new BadRequestException(message);
        }

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            log.warn("Old password mismatch for user: {}", username);
            String message = messageService.getMessage("error.old.password.invalid");
            throw new UnauthorizedException(message);
        }

        // Validate new password
        if (request.getNewPassword() == null || request.getNewPassword().trim().isEmpty()) {
            log.warn("New password not provided for user: {}", username);
            String message = messageService.getMessage("error.new.password.required");
            throw new BadRequestException(message);
        }

        if (request.getNewPassword().length() < 6) {
            log.warn("New password too short for user: {}", username);
            String message = messageService.getMessage("error.password.too.short");
            throw new BadRequestException(message);
        }

        // Check if old and new passwords are the same
        if (request.getOldPassword().equals(request.getNewPassword())) {
            log.warn("New password cannot be same as old password for user: {}", username);
            String message = messageService.getMessage("error.password.same");
            throw new BadRequestException(message);
        }

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setRequireAction(null); // Clear requireAction flag
        User updatedUser = userRepository.save(user);
        log.info("Password updated for user: {}", username);
        return mapToUserProfile(updatedUser);
    }

    /**
     * Update user basic info (fullName, email)
     */
    @Override
    public UserProfile updateProfileInfo(String username, ProfileRequest request) {
        log.info("Updating profile info for user: {}", username);

        if (!username.equals(request.getUsername())) {
            throw new ForbiddenException(messageService.getMessage("error.permission.denied"));
        }

        User user = userRepository.findByUsernameActive(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageService.getMessage("error.user.not.found", username)));

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName().trim());
        }
        if (request.getEmail() != null) {
            String email = request.getEmail().trim();
            if (!email.isEmpty() && !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                throw new BadRequestException(messageService.getMessage("error.user.invalid.email"));
            }
            user.setEmail(email.isEmpty() ? null : email);
        }

        User updatedUser = userRepository.save(user);
        log.info("Profile info updated for user: {}", username);
        return mapToUserProfile(updatedUser);
    }

    /**
     * Update user language preference
     */
    @Override
    public UserProfile updateProfileLang(String username, ProfileRequest request) {
        log.info("Updating language preference for user: {}", username);

        if (!username.equals(request.getUsername())) {
            throw new ForbiddenException(messageService.getMessage("error.permission.denied"));
        }

        User user = userRepository.findByUsernameActive(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageService.getMessage("error.user.not.found", username)));

        if (request.getLang() != null && !request.getLang().isBlank()) {
            user.setLang(request.getLang());
        }

        User updatedUser = userRepository.save(user);
        log.info("Language preference updated for user: {}", username);
        return mapToUserProfile(updatedUser);
    }

    /**
     * Get user preferences JSON
     */
    @Override
    public String getPreferences(String username) {
        User user = userRepository.findByUsernameActive(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageService.getMessage("error.user.not.found", username)));
        return user.getPreferences() != null ? user.getPreferences() : "{}";
    }

    /**
     * Save user preferences JSON
     */
    @Override
    public void updatePreferences(String username, String preferences) {
        User user = userRepository.findByUsernameActive(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageService.getMessage("error.user.not.found", username)));
        user.setPreferences(preferences);
        userRepository.save(user);
        log.info("Preferences updated for user: {}", username);
    }

    /**
     * Upload and store a new avatar image for the user.
     * Resizes to 512×512 JPEG (85%), stores in R2 under avatars/{userId}.jpg,
     * clears the old avatar after the new one is safely committed.
     */
    @Override
    public UserProfile uploadAvatar(String username, MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.startsWith("image/jpeg")
                && !contentType.startsWith("image/png")
                && !contentType.startsWith("image/webp"))) {
            throw new BadRequestException(messageService.getMessage("error.user.avatar.invalid.type"));
        }

        User user = userRepository.findByUsernameActive(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageService.getMessage("error.user.not.found", username)));

        String oldAvatarKey = r2StorageService.keyFromUrl(user.getAvatarUrl());

        // Resize to 512×512 max, JPEG 85% quality (also corrects EXIF orientation)
        byte[] compressed;
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Thumbnails.of(file.getInputStream())
                    .size(512, 512)
                    .keepAspectRatio(true)
                    .outputFormat("jpg")
                    .outputQuality(0.85)
                    .toOutputStream(out);
            compressed = out.toByteArray();
        } catch (IOException e) {
            log.error("Failed to process avatar image for user: {}", username, e);
            throw new BadRequestException(messageService.getMessage("error.user.avatar.process.failed"));
        }

        // Upload new image first — only clean up old after DB is committed
        String key = "avatars/" + user.getId() + ".jpg";
        String url = r2StorageService.upload(key, compressed, "image/jpeg");
        user.setAvatarUrl(url.isBlank() ? null : url);
        User saved = userRepository.save(user);

        // Fire-and-forget: remove old avatar from R2
        r2CleanupService.deleteAsync(oldAvatarKey);

        String actor = SecurityContextHolder.getContext().getAuthentication().getName();
        String tid = tenantContext.getCurrentTenantId() != null ? tenantContext.getCurrentTenantId() : "master";
        activityLogService.logAsync(tid, actor, null,
                ActivityAction.USER_AVATAR_UPDATED, "USER", user.getId().toString(),
                "Cập nhật ảnh đại diện: " + username, null);

        log.info("Avatar uploaded — userId: {}, key: {}", user.getId(), key);
        return mapToUserProfile(saved);
    }

    /**
     * Delete the current user's avatar from R2 and clear avatarUrl.
     */
    @Override
    public void deleteAvatar(String username) {
        User user = userRepository.findByUsernameActive(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageService.getMessage("error.user.not.found", username)));

        String oldAvatarKey = r2StorageService.keyFromUrl(user.getAvatarUrl());
        if (oldAvatarKey == null) return; // no avatar to delete

        user.setAvatarUrl(null);
        userRepository.save(user);

        // Fire-and-forget: remove from R2 after DB is committed
        r2CleanupService.deleteAsync(oldAvatarKey);

        String actor = SecurityContextHolder.getContext().getAuthentication().getName();
        String tid = tenantContext.getCurrentTenantId() != null ? tenantContext.getCurrentTenantId() : "master";
        activityLogService.logAsync(tid, actor, null,
                ActivityAction.USER_AVATAR_DELETED, "USER", user.getId().toString(),
                "Xóa ảnh đại diện: " + username, null);

        log.info("Avatar deleted — userId: {}", user.getId());
    }

    /**
     * Map User entity to UserProfile DTO
     */
    private UserProfile mapToUserProfile(User user) {
        return UserProfile.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .avatarUrl(user.getAvatarUrl())
                .colorPreference(user.getColorPreference())
                .lang(user.getLang())
                .active(user.getActive())
                .accountNonLocked(user.getAccountNonLocked())
                .credentialsNonExpired(user.getCredentialsNonExpired())
                .accountNonExpired(user.getAccountNonExpired())
                .requireAction(user.getRequireAction())
                .notes(user.getNotes())
                .roles(user.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.toSet()))
                .build();
    }
}

