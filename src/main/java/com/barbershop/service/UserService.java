package com.barbershop.service;

import com.barbershop.exception.BadRequestException;
import com.barbershop.exception.DuplicateResourceException;
import com.barbershop.exception.ResourceNotFoundException;
import com.barbershop.exception.UnauthorizedException;
import com.barbershop.model.dto.*;
import com.barbershop.model.entity.Employee;
import com.barbershop.model.entity.Role;
import com.barbershop.model.entity.User;
import com.barbershop.model.enums.RoleEnum;
import com.barbershop.repository.EmployeeRepository;
import com.barbershop.repository.RoleRepository;
import com.barbershop.repository.UserRepository;
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
import java.util.NoSuchElementException;
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
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final MessageService messageService;

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
                    throw new IllegalArgumentException("Invalid role: " + roleName +
                            ". Valid roles are: SHOP_OWNER, MANAGER, RECEPTIONIST, CLEANER, TECHNICIAN");
                }

                Role role = roleRepository.findByName(roleName)
                        .orElseThrow(() -> new NoSuchElementException("Role not found: " + roleName));
                roles.add(role);
            }
            user.setRoles(roles);
        }

        User createdUser = userRepository.save(user);
        log.info("User created successfully: {} with id: {}", createdUser.getUsername(), createdUser.getId());

        // Assign user to employee if provided
        if (request.getEmployeeId() != null) {
            Employee employee = employeeRepository.findByIdActive(request.getEmployeeId())
                    .orElseThrow(() -> new NoSuchElementException("Employee not found: " + request.getEmployeeId()));

            // Check if another user is already assigned to this employee
            if (employee.getUser() != null && !employee.getUser().getId().equals(createdUser.getId())) {
                log.warn("Employee {} already has user assigned: {}, overwriting with: {}",
                        employee.getId(), employee.getUser().getId(), createdUser.getId());
            }

            employee.setUser(createdUser);
            employeeRepository.save(employee);
            log.info("User {} assigned to employee {}", createdUser.getId(), employee.getId());
        }

        return mapToDTO(createdUser);
    }

    /**
     * Get user by ID
     */
    public UserDTO getUserById(Long id) {
        log.info("Fetching user by id: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        return mapToDTO(user);
    }

    /**
     * Get user by username
     */
    public UserDTO getUserByUsername(String username) {
        log.info("Fetching user by username: {}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
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
                .orElseThrow(() -> new NoSuchElementException("User not found: " + id));

        // Update username if provided
        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new IllegalArgumentException("Username already exists: " + request.getUsername());
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
                    throw new IllegalArgumentException("Invalid role: " + roleName +
                            ". Valid roles are: SHOP_OWNER, MANAGER, RECEPTIONIST, CLEANER, TECHNICIAN");
                }

                Role role = roleRepository.findByName(roleName)
                        .orElseThrow(() -> new NoSuchElementException("Role not found: " + roleName));
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

        // Assign user to employee if provided
        if (request.getEmployeeId() != null) {
            Employee employee = employeeRepository.findByIdActive(request.getEmployeeId())
                    .orElseThrow(() -> new NoSuchElementException("Employee not found: " + request.getEmployeeId()));

            employee.setUser(updatedUser);
            employeeRepository.save(employee);
            log.info("User {} assigned to employee {}", updatedUser.getId(), employee.getId());
        }

        return mapToDTO(updatedUser);
    }

    /**
     * Delete user (soft delete)
     */
    public void deleteUser(Long id) {
        log.info("Deleting user: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + id));

        // Remove user from associated employee if exists
        employeeRepository.findByUserId(id).ifPresent(employee -> {
            employee.setUser(null);
            employeeRepository.save(employee);
            log.info("Removed user from employee: {}", employee.getId());
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
                .orElseThrow(() -> new NoSuchElementException("User not found: " + id));
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
                .orElseThrow(() -> new NoSuchElementException("User not found: " + id));
        user.setAccountNonLocked(!locked);
        User updatedUser = userRepository.save(user);
        log.info("User {} lock status updated", updatedUser.getUsername());
        return mapToDTO(updatedUser);
    }

    /**
     * Add role to user (must be predefined role)
     */
    public UserDTO addRoleToUser(Long userId, String roleName) {
        log.info("Adding role {} to user {}", roleName, userId);

        // Validate role code against predefined roles
        if (!RoleEnum.isValidRole(roleName)) {
            throw new IllegalArgumentException("Invalid role: " + roleName +
                    ". Valid roles are: SHOP_OWNER, MANAGER, RECEPTIONIST, CLEANER, TECHNICIAN");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new NoSuchElementException("Role not found: " + roleName));

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
            throw new IllegalArgumentException("Invalid role: " + roleName +
                    ". Valid roles are: SHOP_OWNER, MANAGER, RECEPTIONIST, CLEANER, TECHNICIAN");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new NoSuchElementException("Role not found: " + roleName));

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

        // Find employee ID by matching user ID
        Long employeeId = null;
        if (user.getId() != null) {
            employeeId = employeeRepository.findByUserId(user.getId())
                    .map(Employee::getId)
                    .orElse(null);
        }

        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .active(user.getActive())
                .accountNonLocked(user.getAccountNonLocked())
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
                    return new NoSuchElementException("User not found: " + userId);
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
            throw new UnauthorizedException("User must be authenticated to change password");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("User not found for password change - username: {}", username);
                    return new ResourceNotFoundException("User not found: " + username);
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
            throw new UnauthorizedException("User must be authenticated to change password");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("User not found for password change - username: {}", username);
                    return new ResourceNotFoundException("User not found: " + username);
                });

        // Verify old password
        if (request.getOldPassword() == null || request.getOldPassword().isEmpty()) {
            log.warn("Old password not provided for password change - username: {}", username);
            throw new BadRequestException("Old password is required");
        }

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            log.warn("Old password verification failed - username: {}", username);
            throw new BadRequestException("Old password is incorrect");
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

