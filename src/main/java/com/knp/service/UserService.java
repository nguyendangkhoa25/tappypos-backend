package com.knp.service;

import com.knp.exception.BadRequestException;
import com.knp.exception.DuplicateResourceException;
import com.knp.exception.ResourceNotFoundException;
import com.knp.exception.UnauthorizedException;
import com.knp.model.dto.*;
import com.knp.model.entity.Role;
import com.knp.model.entity.User;
import com.knp.model.enums.RoleEnum;
import com.knp.repository.EmployeeRepository;
import com.knp.repository.RoleRepository;
import com.knp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final MessageService messageService;
    private final EmployeeRepository employeeRepository;

    /**
     * Create a new user
     */
    public UserDTO createUser(CreateUserRequest request) {
        log.info("Creating new user: {}", request.getUsername());

        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException(messageService.getMessage("error.user.duplicate.username", request.getUsername()));
        }

        // Check if email already exists
        if (StringUtils.isNoneEmpty(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException(messageService.getMessage("error.user.duplicate.email", request.getEmail()));
        }

        // Create user
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .notes(request.getNotes())
                .active(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .accountNonExpired(true)
                .requireAction("CHANGE_PASSWORD")
                .build();

        // Assign roles if provided
        if (request.getRoleNames() != null && !request.getRoleNames().isEmpty()) {
            Set<Role> roles = new HashSet<>();
            for (String roleName : request.getRoleNames()) {
                // Validate role code against predefined roles
                if (!RoleEnum.isValidRole(roleName)) {
                    String errorMessage = messageService.getMessage("error.role.invalid", roleName);
                    throw new BadRequestException(errorMessage);
                }

                Role role = roleRepository.findByName(roleName)
                        .orElseThrow(() -> {
                            String errorMessage = messageService.getMessage("error.role.not.found", roleName);
                            return new ResourceNotFoundException(errorMessage);
                        });
                roles.add(role);
            }
            user.setRoles(roles);
        }

        User createdUser = userRepository.save(user);
        log.info("User created successfully: {} with id: {}", createdUser.getUsername(), createdUser.getId());

        return mapToDTO(createdUser);
    }

    /**
     * Get user by ID
     */
    public UserDTO getUserById(Long id) {
        log.info("Fetching user by id: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    String errorMessage = messageService.getMessage("error.user.not.found", id);
                    return new ResourceNotFoundException(errorMessage);
                });
        return mapToDTO(user);
    }

    /**
     * Get user by username
     */
    public UserDTO getUserByUsername(String username) {
        log.info("Fetching user by username: {}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    String errorMessage = messageService.getMessage("error.user.not.found", username);
                    return new ResourceNotFoundException(errorMessage);
                });
        return mapToDTO(user);
    }

    /**
     * Get all users with pagination and search
     */
    public Page<UserDTO> getAllUsers(int page, int size, String search) {
        log.info("Fetching all users with page: {}, size: {}, search: {}", page, size, search);
        Pageable pageable = PageRequest.of(page, size);
        Page<User> users = userRepository.findAllWithSearch(search, pageable);
        return users.map(this::mapToDTO);
    }

    /**
     * Update user
     */
    public UserDTO updateUser(Long id, CreateUserRequest request) {
        log.info("Updating user: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    String errorMessage = messageService.getMessage("error.user.not.found", id);
                    return new ResourceNotFoundException(errorMessage);
                });

        // Update username if provided
        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                String errorMessage = messageService.getMessage("error.user.duplicate.username", request.getUsername());
                throw new DuplicateResourceException(errorMessage);
            }
            user.setUsername(request.getUsername());
        }

        // Update password if provided
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        // Update full name if provided
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }

        // Update roles if provided
        if (request.getRoleNames() != null) {
            Set<Role> roles = new HashSet<>();
            for (String roleName : request.getRoleNames()) {
                // Validate role code against predefined roles
                if (!RoleEnum.isValidRole(roleName)) {
                    String errorMessage = messageService.getMessage("error.role.invalid", roleName);
                    throw new BadRequestException(errorMessage);
                }

                Role role = roleRepository.findByName(roleName)
                        .orElseThrow(() -> {
                            String errorMessage = messageService.getMessage("error.role.not.found", roleName);
                            return new ResourceNotFoundException(errorMessage);
                        });
                roles.add(role);
            }
            user.setRoles(roles);
        }

        // Update notes if provided
        if (request.getNotes() != null) {
            user.setNotes(request.getNotes());
        }

        User updatedUser = userRepository.save(user);
        log.info("User updated successfully: {}", updatedUser.getUsername());

        return mapToDTO(updatedUser);
    }

    /**
     * Delete user (soft delete)
     */
    public void deleteUser(Long id) {
        log.info("Deleting user: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    String errorMessage = messageService.getMessage("error.user.not.found", id);
                    return new ResourceNotFoundException(errorMessage);
                });

        user.setDeleted(true);
        userRepository.save(user);
        log.info("User deleted: {}", user.getUsername());
    }

    /**
     * Disable/Enable user
     */
    public UserDTO disableUser(Long id, boolean active) {
        log.info("Setting user {} active status to: {}", id, active);
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    String errorMessage = messageService.getMessage("error.user.not.found", id);
                    return new ResourceNotFoundException(errorMessage);
                });
        user.setActive(active);
        User updatedUser = userRepository.save(user);
        log.info("User {} status updated", updatedUser.getUsername());
        return mapToDTO(updatedUser);
    }

    /**
     * Lock/Unlock user account
     */
    public UserDTO lockUser(Long id, boolean locked) {
        log.info("Setting user {} locked status to: {}", id, locked);
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    String errorMessage = messageService.getMessage("error.user.not.found", id);
                    return new ResourceNotFoundException(errorMessage);
                });
        user.setAccountNonLocked(!locked);
        if (!locked) {
            user.setFailedLoginAttempts(0);
        }
        User updatedUser = userRepository.save(user);
        log.info("User {} lock status updated to locked={}", updatedUser.getUsername(), locked);
        return mapToDTO(updatedUser);
    }

    /**
     * Add role to user (must be predefined role)
     */
    public UserDTO addRoleToUser(Long userId, String roleName) {
        log.info("Adding role {} to user {}", roleName, userId);

        // Validate role code against predefined roles
        if (!RoleEnum.isValidRole(roleName)) {
            String errorMessage = messageService.getMessage("error.role.invalid", roleName);
            throw new BadRequestException(errorMessage);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    String errorMessage = messageService.getMessage("error.user.not.found", userId);
                    return new ResourceNotFoundException(errorMessage);
                });
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> {
                    String errorMessage = messageService.getMessage("error.role.not.found", roleName);
                    return new ResourceNotFoundException(errorMessage);
                });

        user.addRole(role);
        User updatedUser = userRepository.save(user);
        log.info("Role {} added to user {}", roleName, user.getUsername());
        return mapToDTO(updatedUser);
    }

    /**
     * Remove role from user
     */
    public UserDTO removeRoleFromUser(Long userId, String roleName) {
        log.info("Removing role {} from user {}", roleName, userId);

        // Validate role code against predefined roles
        if (!RoleEnum.isValidRole(roleName)) {
            String errorMessage = messageService.getMessage("error.role.invalid", roleName);
            throw new BadRequestException(errorMessage);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    String errorMessage = messageService.getMessage("error.user.not.found", userId);
                    return new ResourceNotFoundException(errorMessage);
                });
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> {
                    String errorMessage = messageService.getMessage("error.role.not.found", roleName);
                    return new ResourceNotFoundException(errorMessage);
                });

        user.removeRole(role);
        User updatedUser = userRepository.save(user);
        log.info("Role {} removed from user {}", roleName, user.getUsername());
        return mapToDTO(updatedUser);
    }

    /**
     * Map User entity to UserDTO
     */
    private UserDTO mapToDTO(User user) {
        Set<RoleDTO> roleDTOs = user.getRoles().stream()
                .map(role -> RoleDTO.builder()
                        .id(role.getId())
                        .name(role.getName())
                        .description(role.getDescription())
                        .createdAt(role.getCreatedAt())
                        .updatedAt(role.getUpdatedAt())
                        .build())
                .collect(Collectors.toSet());

        Long employeeId = null;
        try {
            employeeId = employeeRepository.findByUserId(user.getId())
                    .map(e -> e.getId())
                    .orElse(null);
        } catch (Exception ignored) {
            // Employee table may not exist in all contexts (e.g. master DB)
        }
        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .active(user.getActive())
                .accountNonLocked(user.getAccountNonLocked())
                .failedLoginAttempts(user.getFailedLoginAttempts() != null ? user.getFailedLoginAttempts() : 0)
                .credentialsNonExpired(user.getCredentialsNonExpired())
                .accountNonExpired(user.getAccountNonExpired())
                .employeeId(employeeId)
                .roles(roleDTOs)
                .notes(user.getNotes())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    public PasswordResetResponse resetUserPassword(Long userId) {
        log.info("Request: Reset user password - userId: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found for password reset - userId: {}", userId);
                    String errorMessage = messageService.getMessage("error.user.not.found", userId);
                    return new ResourceNotFoundException(errorMessage);
                });

        // Generate temporary password
        String tempPassword = generateTemporaryPassword();

        // Update user password and set requireAction
        user.setPassword(passwordEncoder.encode(tempPassword));
        user.setRequireAction("CHANGE_PASSWORD");

        User updatedUser = userRepository.save(user);

        log.info("User password reset successfully - userId: {}, username: {}", userId, user.getUsername());

        return PasswordResetResponse.builder()
                .userId(updatedUser.getId())
                .username(updatedUser.getUsername())
                .email(updatedUser.getEmail())
                .tempPassword(tempPassword)
                .message("Password has been reset. User must change password on next login.")
                .requirePasswordChange(true)
                .build();
    }

    /**
     * Change user password on first login (does not require old password)
     * Used when user logs in for the first time with a temporary password
     * Sets requireAction to null after successful change
     */
    public UserDTO changePasswordOnFirstLogin(ChangePasswordRequest request) {
        log.info("Request: Change password on first login");

        // Get current username from SecurityContext
        String username = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        if (username == null || "anonymousUser".equals(username)) {
            log.error("No authenticated user found for password change");
            String errorMessage = messageService.getMessage("error.user.not.authenticated");
            throw new UnauthorizedException(errorMessage);
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("User not found for password change - username: {}", username);
                    String errorMessage = messageService.getMessage("error.user.not.found", username);
                    return new ResourceNotFoundException(errorMessage);
                });

        // Update password and clear requireAction flag
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setRequireAction(null);

        User updatedUser = userRepository.save(user);

        log.info("Password changed on first login successfully - username: {}", username);

        return mapToDTO(updatedUser);
    }

    /**
     * Change user password (requires old password)
     * Standard password change when user is already logged in
     */
    public UserDTO changePassword(ChangePasswordRequest request) {
        log.info("Request: Change password with old password verification");

        // Get current username from SecurityContext
        String username = org.springframework.security.core.context.SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        if (username == null || "anonymousUser".equals(username)) {
            log.error("No authenticated user found for password change");
            String errorMessage = messageService.getMessage("error.user.not.authenticated");
            throw new UnauthorizedException(errorMessage);
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("User not found for password change - username: {}", username);
                    String errorMessage = messageService.getMessage("error.user.not.found", username);
                    return new ResourceNotFoundException(errorMessage);
                });

        // Verify old password
        if (request.getOldPassword() == null || request.getOldPassword().isEmpty()) {
            log.warn("Old password not provided for password change - username: {}", username);
            String errorMessage = messageService.getMessage("error.password.old.required");
            throw new BadRequestException(errorMessage);
        }

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            log.warn("Old password verification failed - username: {}", username);
            String errorMessage = messageService.getMessage("error.password.old.incorrect");
            throw new BadRequestException(errorMessage);
        }

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));

        User updatedUser = userRepository.save(user);

        log.info("Password changed successfully - username: {}", username);

        return mapToDTO(updatedUser);
    }

    /**
     * log.info("Request: Reset user password - userId: {}", userId);
     * <p>
     * User user = userRepository.findById(userId)
     * .orElseThrow(() -> {
     * log.error("User not found for password reset - userId: {}", userId);
     * return new NoSuchElementException("User not found: " + userId);
     * });
     * <p>
     * // Generate temporary password
     * String tempPassword = generateTemporaryPassword();
     * <p>
     * // Update user password and set requireAction
     * user.setPassword(passwordEncoder.encode(tempPassword));
     * user.setRequireAction("CHANGE_PASSWORD");
     * <p>
     * User updatedUser = userRepository.save(user);
     * <p>
     * log.info("User password reset successfully - userId: {}, username: {}", userId, user.getUsername());
     * <p>
     * return PasswordResetResponse.builder()
     * .userId(updatedUser.getId())
     * .username(updatedUser.getUsername())
     * .email(updatedUser.getEmail())
     * .tempPassword(tempPassword)
     * .message("Password has been reset. User must change password on next login.")
     * .requirePasswordChange(true)
     * .build();
     * }
     * <p>
     * /**
     * Generate a temporary password for password reset
     * Format: Random UUID (first 12 characters) + random numbers
     * Example: a1f2b3c4d5e6-1234
     */
    private String generateTemporaryPassword() {
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String numbers = String.format("%04d", (int) (Math.random() * 10000));
        String tempPassword = uuid + "-" + numbers;
        log.debug("Generated temporary password for reset");
        return tempPassword;
    }
}

