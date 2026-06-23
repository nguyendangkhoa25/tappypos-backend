package com.tappy.pos.service.auth;

import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.exception.DuplicateResourceException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.exception.UnauthorizedException;
import com.tappy.pos.model.dto.auth.UserDetailDTO;
import com.tappy.pos.model.dto.auth.CreateUserRequest;
import com.tappy.pos.model.dto.auth.ChangePasswordRequest;
import com.tappy.pos.model.dto.auth.PasswordResetResponse;
import com.tappy.pos.model.dto.auth.RoleDTO;
import com.tappy.pos.model.entity.auth.Feature;
import com.tappy.pos.model.entity.auth.Role;
import com.tappy.pos.model.entity.auth.User;
import com.tappy.pos.repository.auth.RoleFeatureRepository;
import com.tappy.pos.model.enums.RoleEnum;
import com.tappy.pos.model.enums.ActivityAction;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.employee.EmployeeRepository;
import com.tappy.pos.repository.tenant.AgentRepository;
import com.tappy.pos.repository.tenant.TenantRepository;
import com.tappy.pos.repository.auth.RoleRepository;
import com.tappy.pos.repository.auth.UserRepository;
import com.tappy.pos.service.audit.ActivityLogService;
import com.tappy.pos.model.i18n.LocalizedText;
import com.tappy.pos.service.notification.NotificationService;
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

import com.tappy.pos.model.entity.employee.Employee;
import com.tappy.pos.model.enums.EmployeePosition;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Map;
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
    private final RoleFeatureRepository roleFeatureRepository;
    private final PasswordEncoder passwordEncoder;
    private final MessageService messageService;
    private final EmployeeRepository employeeRepository;
    private final AgentRepository agentRepository;
    private final TenantContext tenantContext;
    private final ActivityLogService activityLogService;
    private final NotificationService notificationService;
    private final TenantRepository tenantRepository;

    /**
     * Create a new user
     */
    public UserDetailDTO createUser(CreateUserRequest request) {
        log.info("Creating new user: {}", request.getUsername());

        // Check if username already exists within current tenant
        if (userRepository.existsByUsernameTenantScoped(request.getUsername())) {
            throw new DuplicateResourceException(messageService.getMessage("error.user.duplicate.username", request.getUsername()));
        }

        // Check if email already exists within current tenant
        if (StringUtils.isNoneEmpty(request.getEmail()) && userRepository.existsByEmailTenantScoped(request.getEmail())) {
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
        user.setTenantId(tenantContext.getCurrentTenantId());

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

        // Assign per-user feature overrides if provided
        if (request.getFeatureNames() != null && !request.getFeatureNames().isEmpty()) {
            Set<Feature> features = roleFeatureRepository.findByNameIn(request.getFeatureNames());
            user.setUserFeatures(features);
            log.info("Setting {} user-specific features for new user: {}", features.size(), request.getUsername());
        }

        User createdUser = userRepository.save(user);
        log.info("User created successfully: {} with id: {}", createdUser.getUsername(), createdUser.getId());

        autoCreateEmployee(createdUser, request.getRoleNames());
        assignVendor(createdUser.getId(), request.getVendorId());

        if (tenantContext.getCurrentTenant() == null) {
            String actorUsername = SecurityContextHolder.getContext().getAuthentication().getName();
            String actorFullName = userRepository.findByUsernameTenantScoped(actorUsername)
                    .map(User::getFullName).orElse(actorUsername);
            activityLogService.logAsync("master", actorUsername, actorFullName,
                    ActivityAction.USER_CREATED, "USER", String.valueOf(createdUser.getId()),
                    "activity.user.created", null, createdUser.getUsername());
            notificationService.pushToMasterUsers(
                    LocalizedText.of("notification.master.user.created.title"),
                    LocalizedText.of("notification.master.user.created.message",
                            createdUser.getUsername(), actorUsername),
                    "USER", createdUser.getId());
        }

        return mapToDTO(createdUser);
    }

    /**
     * Get user by ID
     */
    public UserDetailDTO getUserById(Long id) {
        log.info("Fetching user by id: {}", id);
        User user = userRepository.findByIdTenantScoped(id)
                .orElseThrow(() -> {
                    String errorMessage = messageService.getMessage("error.user.not.found", id);
                    return new ResourceNotFoundException(errorMessage);
                });
        return mapToDTO(user);
    }

    /**
     * Get user by username
     */
    public UserDetailDTO getUserByUsername(String username) {
        log.info("Fetching user by username: {}", username);
        User user = userRepository.findByUsernameTenantScoped(username)
                .orElseThrow(() -> {
                    String errorMessage = messageService.getMessage("error.user.not.found", username);
                    return new ResourceNotFoundException(errorMessage);
                });
        return mapToDTO(user);
    }

    /**
     * Get all users with pagination and search
     */
    public Page<UserDetailDTO> getAllUsers(int page, int size, String search) {
        log.info("Fetching all users with page: {}, size: {}, search: {}", page, size, search);
        Pageable pageable = PageRequest.of(page, size);
        Page<User> users = userRepository.findAllWithSearch(search, pageable);
        return users.map(this::mapToDTO);
    }

    /**
     * Update user
     */
    public UserDetailDTO updateUser(Long id, CreateUserRequest request) {
        log.info("Updating user: {}", id);
        User user = userRepository.findByIdTenantScoped(id)
                .orElseThrow(() -> {
                    String errorMessage = messageService.getMessage("error.user.not.found", id);
                    return new ResourceNotFoundException(errorMessage);
                });

        // Update username if provided
        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsernameTenantScoped(request.getUsername())) {
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

        // Track whether this edit changes the user's effective features (roles or per-user
        // overrides), so we only fire the stale-token signal when access actually changes.
        boolean permissionsChanged = false;

        // Update roles if provided
        if (request.getRoleNames() != null) {
            Set<String> oldRoleNames = user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
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
            Set<String> newRoleNames = roles.stream().map(Role::getName).collect(Collectors.toSet());
            if (!oldRoleNames.equals(newRoleNames)) permissionsChanged = true;
        }

        // Update notes if provided
        if (request.getNotes() != null) {
            user.setNotes(request.getNotes());
        }

        // Update per-user feature overrides
        // null = don't touch; empty set = clear overrides (revert to role defaults); non-empty = replace
        if (request.getFeatureNames() != null) {
            Set<String> oldFeatureNames = user.getUserFeatures() == null ? Set.of()
                    : user.getUserFeatures().stream().map(Feature::getName).collect(Collectors.toSet());
            if (request.getFeatureNames().isEmpty()) {
                user.setUserFeatures(new HashSet<>());
                log.info("Cleared user-specific features for user: {}", id);
            } else {
                Set<Feature> features = roleFeatureRepository.findByNameIn(request.getFeatureNames());
                user.setUserFeatures(features);
                log.info("Updated {} user-specific features for user: {}", features.size(), id);
            }
            if (!oldFeatureNames.equals(new HashSet<>(request.getFeatureNames()))) permissionsChanged = true;
        }

        User updatedUser = userRepository.save(user);
        log.info("User updated successfully: {}", updatedUser.getUsername());

        assignVendor(updatedUser.getId(), request.getVendorId());

        // A role/override change alters this user's JWT feature set — fire the stale-token signal
        // so they refresh within seconds (see TenantInterceptor + TenantRepository.bumpFeaturesVersion).
        if (permissionsChanged) {
            bumpFeaturesVersionForCurrentTenant();
        }

        return mapToDTO(updatedUser);
    }

    /**
     * Delete user (soft delete)
     */
    public void deleteUser(Long id) {
        log.info("Deleting user: {}", id);
        User user = userRepository.findByIdTenantScoped(id)
                .orElseThrow(() -> {
                    String errorMessage = messageService.getMessage("error.user.not.found", id);
                    return new ResourceNotFoundException(errorMessage);
                });

        user.setDeleted(true);
        userRepository.save(user);
        clearVendorAssignment(id);
        log.info("User deleted: {}", user.getUsername());
    }

    /**
     * Disable/Enable user
     */
    public UserDetailDTO disableUser(Long id, boolean active) {
        log.info("Setting user {} active status to: {}", id, active);
        User user = userRepository.findByIdTenantScoped(id)
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
    public UserDetailDTO lockUser(Long id, boolean locked) {
        log.info("Setting user {} locked status to: {}", id, locked);
        User user = userRepository.findByIdTenantScoped(id)
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
    public UserDetailDTO addRoleToUser(Long userId, String roleName) {
        log.info("Adding role {} to user {}", roleName, userId);

        // Validate role code against predefined roles
        if (!RoleEnum.isValidRole(roleName)) {
            String errorMessage = messageService.getMessage("error.role.invalid", roleName);
            throw new BadRequestException(errorMessage);
        }

        User user = userRepository.findByIdTenantScoped(userId)
                .orElseThrow(() -> {
                    String errorMessage = messageService.getMessage("error.user.not.found", userId);
                    return new ResourceNotFoundException(errorMessage);
                });
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> {
                    String errorMessage = messageService.getMessage("error.role.not.found", roleName);
                    return new ResourceNotFoundException(errorMessage);
                });

        boolean changed = user.getRoles().stream().noneMatch(r -> roleName.equals(r.getName()));
        user.addRole(role);
        User updatedUser = userRepository.save(user);
        log.info("Role {} added to user {}", roleName, user.getUsername());
        // Only fire the stale-token signal if the role was not already present.
        if (changed) bumpFeaturesVersionForCurrentTenant();
        return mapToDTO(updatedUser);
    }

    /**
     * Remove role from user
     */
    public UserDetailDTO removeRoleFromUser(Long userId, String roleName) {
        log.info("Removing role {} from user {}", roleName, userId);

        // Validate role code against predefined roles
        if (!RoleEnum.isValidRole(roleName)) {
            String errorMessage = messageService.getMessage("error.role.invalid", roleName);
            throw new BadRequestException(errorMessage);
        }

        User user = userRepository.findByIdTenantScoped(userId)
                .orElseThrow(() -> {
                    String errorMessage = messageService.getMessage("error.user.not.found", userId);
                    return new ResourceNotFoundException(errorMessage);
                });
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> {
                    String errorMessage = messageService.getMessage("error.role.not.found", roleName);
                    return new ResourceNotFoundException(errorMessage);
                });

        boolean changed = user.getRoles().stream().anyMatch(r -> roleName.equals(r.getName()));
        user.removeRole(role);
        User updatedUser = userRepository.save(user);
        log.info("Role {} removed from user {}", roleName, user.getUsername());
        // Only fire the stale-token signal if the role was actually present.
        if (changed) bumpFeaturesVersionForCurrentTenant();
        return mapToDTO(updatedUser);
    }

    /**
     * Fire the stale-token signal for the current tenant after a per-user permission change
     * (role assignment or per-user feature override). Bumps features_version so every active
     * token for the tenant is detected as stale on its next request and silently refreshed.
     * No-op outside a tenant context (e.g. master-side operations).
     */
    private void bumpFeaturesVersionForCurrentTenant() {
        String tenantId = tenantContext.getCurrentTenantId();
        if (tenantId != null) {
            tenantRepository.bumpFeaturesVersion(tenantId);
            log.info("Bumped features_version for tenant {} after user permission change", tenantId);
        }
    }

    /**
     * Map User entity to UserDetailDTO
     */
    private UserDetailDTO mapToDTO(User user) {
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
        if (tenantContext.getCurrentTenantId() != null) {
            employeeId = employeeRepository.findByUserId(user.getId())
                    .map(e -> e.getId())
                    .orElse(null);
        }

        Long vendorId = null;
        String vendorName = null;
        if (tenantContext.getCurrentTenantId() == null) {
            var agentOpt = agentRepository.findByUserId(user.getId());
            if (agentOpt.isPresent()) {
                vendorId = agentOpt.get().getId();
                vendorName = agentOpt.get().getName();
            }
        }

        return UserDetailDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .active(user.getActive())
                .accountNonLocked(user.getAccountNonLocked())
                .failedLoginAttempts(user.getFailedLoginAttempts() != null ? user.getFailedLoginAttempts() : 0)
                .credentialsNonExpired(user.getCredentialsNonExpired())
                .accountNonExpired(user.getAccountNonExpired())
                .avatarUrl(user.getAvatarUrl())
                .employeeId(employeeId)
                .vendorId(vendorId)
                .vendorName(vendorName)
                .roles(roleDTOs)
                .userFeatureNames(user.getUserFeatures() != null
                        ? user.getUserFeatures().stream().map(Feature::getName).collect(Collectors.toSet())
                        : null)
                .notes(user.getNotes())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    public PasswordResetResponse resetUserPassword(Long userId) {
        log.info("Request: Reset user password - userId: {}", userId);

        User user = userRepository.findByIdTenantScoped(userId)
                .orElseThrow(() -> {
                    log.error("User not found for password reset - userId: {}", userId);
                    return new ResourceNotFoundException(messageService.getMessage("error.user.not.found", userId));
                });

        String tempPassword = generateTemporaryPassword();
        user.setPassword(passwordEncoder.encode(tempPassword));
        user.setRequireAction("CHANGE_PASSWORD");

        User updatedUser = userRepository.save(user);
        log.info("User password reset successfully - userId: {}, username: {}", userId, user.getUsername());

        return PasswordResetResponse.builder()
                .userId(updatedUser.getId())
                .username(updatedUser.getUsername())
                .email(updatedUser.getEmail())
                .tempPassword(tempPassword)
                .requirePasswordChange(true)
                .build();
    }

    /**
     * Change user password on first login (does not require old password)
     * Used when user logs in for the first time with a temporary password
     * Sets requireAction to null after successful change
     */
    public UserDetailDTO changePasswordOnFirstLogin(ChangePasswordRequest request) {
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

        User user = userRepository.findByUsernameTenantScoped(username)
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
    public UserDetailDTO changePassword(ChangePasswordRequest request) {
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

        User user = userRepository.findByUsernameTenantScoped(username)
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
    // Maps an access role to an HR employee position when auto-creating an employee on join.
    // Only roles that correspond to a distinct EmployeePosition are listed; others get a null position.
    private static final Map<String, EmployeePosition> ROLE_TO_POSITION = Map.of(
        "TECHNICIAN", EmployeePosition.TECHNICIAN
    );

    private void autoCreateEmployee(User user, java.util.Set<String> roleNames) {
        String currentTenantId = tenantContext.getCurrentTenantId();
        if (currentTenantId == null) return;
        if (roleNames == null || roleNames.isEmpty()) return;
        if (employeeRepository.existsByUserId(user.getId())) return;

        EmployeePosition position = roleNames.stream()
                .map(ROLE_TO_POSITION::get)
                .filter(p -> p != null)
                .findFirst()
                .orElse(null);

        if (position == null) return;

        Employee emp = new Employee();
        emp.setTenantId(currentTenantId);
        emp.setFullName(user.getFullName() != null ? user.getFullName() : user.getUsername());
        emp.setPhone(user.getUsername());
        emp.setPosition(position);
        emp.setHireDate(java.time.LocalDate.now());
        emp.setActive(true);
        emp.setUserId(user.getId());
        emp.setDeleted(false);
        log.info("Creating employee for user {} in tenant {}", user.getUsername(), currentTenantId);
        employeeRepository.save(emp);
        log.info("Auto-created employee record for user {} with position {}", user.getUsername(), position);
    }

    private void assignVendor(Long userId, Long vendorId) {
        if (tenantContext.getCurrentTenantId() != null) return;
        clearVendorAssignment(userId);
        if (vendorId != null) {
            agentRepository.findById(vendorId).ifPresent(a -> {
                a.setUserId(userId);
                agentRepository.save(a);
            });
        }
    }

    private void clearVendorAssignment(Long userId) {
        if (tenantContext.getCurrentTenantId() != null) return;
        agentRepository.findByUserId(userId).ifPresent(a -> {
            a.setUserId(null);
            agentRepository.save(a);
        });
    }

    private String generateTemporaryPassword() {
        int num = 1000 + (int) (Math.random() * 9000);
        return "TAPPY-" + num;
    }
}

