package com.barbershop.service;

import com.barbershop.model.dto.CreateUserRequest;
import com.barbershop.model.dto.RoleDTO;
import com.barbershop.model.dto.UserDTO;
import com.barbershop.model.entity.Employee;
import com.barbershop.model.entity.Role;
import com.barbershop.model.entity.User;
import com.barbershop.model.enums.RoleEnum;
import com.barbershop.repository.EmployeeRepository;
import com.barbershop.repository.RoleRepository;
import com.barbershop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;
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

    /**
     * Create a new user
     */
    public UserDTO createUser(CreateUserRequest request) {
        log.info("Creating new user: {}", request.getUsername());

        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists: " + request.getUsername());
        }

        // Check if email already exists
        if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + request.getEmail());
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

        // Assign username to employee if provided
        if (request.getEmployeeId() != null) {
            Employee employee = employeeRepository.findByIdActive(request.getEmployeeId())
                    .orElseThrow(() -> new NoSuchElementException("Employee not found: " + request.getEmployeeId()));

            // Check if another user already has this username assigned to this employee
            if (employee.getUsername() != null && !employee.getUsername().equals(createdUser.getUsername())) {
                log.warn("Employee {} already has username: {}, overwriting with: {}",
                        employee.getId(), employee.getUsername(), createdUser.getUsername());
            }

            employee.setUsername(createdUser.getUsername());
            employeeRepository.save(employee);
            log.info("Username {} assigned to employee {}", createdUser.getUsername(), employee.getId());
        }

        return mapToDTO(createdUser);
    }

    /**
     * Get user by ID
     */
    public UserDTO getUserById(Long id) {
        log.info("Fetching user by id: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + id));
        return mapToDTO(user);
    }

    /**
     * Get user by username
     */
    public UserDTO getUserByUsername(String username) {
        log.info("Fetching user by username: {}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + username));
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

        // Update email if provided
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new IllegalArgumentException("Email already exists: " + request.getEmail());
            }
            user.setEmail(request.getEmail());
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

        // Assign username to employee if provided
        if (request.getEmployeeId() != null) {
            Employee employee = employeeRepository.findByIdActive(request.getEmployeeId())
                    .orElseThrow(() -> new NoSuchElementException("Employee not found: " + request.getEmployeeId()));

            employee.setUsername(updatedUser.getUsername());
            employeeRepository.save(employee);
            log.info("Username {} assigned to employee {}", updatedUser.getUsername(), employee.getId());
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

        // Find employee ID by matching username
        Long employeeId = null;
        if (user.getUsername() != null) {
            employeeId = employeeRepository.findByUsername(user.getUsername())
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
}

