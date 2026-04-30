package com.knp.service.auth;

import com.knp.exception.BadRequestException;
import com.knp.service.MessageService;
import com.knp.exception.ForbiddenException;
import com.knp.exception.ResourceNotFoundException;
import com.knp.exception.UnauthorizedException;
import com.knp.model.dto.auth.ProfileRequest;
import com.knp.model.dto.auth.UserProfile;
import com.knp.model.entity.auth.Role;
import com.knp.model.entity.auth.User;
import com.knp.repository.auth.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
     * Map User entity to UserProfile DTO
     */
    private UserProfile mapToUserProfile(User user) {
        return UserProfile.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatar(user.getAvatar())
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

