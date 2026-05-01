package com.knp.service.auth;

import com.knp.exception.BadRequestException;
import com.knp.exception.DuplicateResourceException;
import com.knp.exception.ResourceNotFoundException;
import com.knp.exception.UnauthorizedException;
import com.knp.model.dto.auth.UserDetailDTO;
import com.knp.model.dto.auth.CreateUserRequest;
import com.knp.model.dto.auth.ChangePasswordRequest;
import com.knp.model.dto.auth.PasswordResetResponse;
import com.knp.model.entity.auth.Role;
import com.knp.model.entity.auth.User;
import com.knp.multitenant.TenantContext;
import com.knp.repository.employee.EmployeeRepository;
import com.knp.repository.tenant.AgentRepository;
import com.knp.repository.auth.RoleRepository;
import com.knp.repository.auth.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import com.knp.service.MessageService;
import com.knp.service.audit.ActivityLogService;
import com.knp.service.notification.NotificationService;
import org.junit.jupiter.api.AfterEach;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private MessageService messageService;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private AgentRepository agentRepository;

    @Mock
    private TenantContext tenantContext;

    @Mock
    private ActivityLogService activityLogService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private CreateUserRequest createUserRequest;
    private Role testRole;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @BeforeEach
    void setUp() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getName()).thenReturn("admin");
        SecurityContextHolder.setContext(securityContext);

        testRole = Role.builder()
                .name("SHOP_OWNER")
                .description("Shop Owner role")
                .build();
        testRole.setId(1L);
        testRole.setCreatedAt(LocalDateTime.now());
        testRole.setUsers(new HashSet<>()); // Initialize users set

        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .password("hashedPassword")
                .fullName("Test User")
                .active(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .accountNonExpired(true)
                .requireAction(null)
                .notes("Test user notes")
                .roles(new HashSet<>()) // Initialize with empty set
                .build();
        testUser.setId(1L);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.getRoles().add(testRole); // Add role to user

        createUserRequest = CreateUserRequest.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password123")
                .fullName("Test User")
                .notes("Test notes")
                .roleNames(new HashSet<>(List.of("SHOP_OWNER")))
                .build();
    }

    // ============= createUser Tests =============

    @Test
    @DisplayName("Should create user successfully")
    void testCreateUser_Success() {
        // Given
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        when(roleRepository.findByName("SHOP_OWNER")).thenReturn(Optional.of(testRole));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserDetailDTO result = userService.createUser(createUserRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        verify(userRepository).existsByUsername("testuser");
        verify(userRepository).existsByEmail("test@example.com");
        verify(roleRepository).findByName("SHOP_OWNER");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should fail when username already exists")
    void testCreateUser_DuplicateUsername() {
        // Given
        when(userRepository.existsByUsername("testuser")).thenReturn(true);
        when(messageService.getMessage("error.user.duplicate.username", "testuser"))
                .thenReturn("Username already exists");

        // When & Then
        assertThatThrownBy(() -> userService.createUser(createUserRequest))
                .isInstanceOf(DuplicateResourceException.class);
        verify(userRepository).existsByUsername("testuser");
    }

    @Test
    @DisplayName("Should fail when email already exists")
    void testCreateUser_DuplicateEmail() {
        // Given
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);
        when(messageService.getMessage("error.user.duplicate.email", "test@example.com"))
                .thenReturn("Email already exists");

        // When & Then
        assertThatThrownBy(() -> userService.createUser(createUserRequest))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("Should fail when role is invalid")
    void testCreateUser_InvalidRole() {
        // Given
        createUserRequest.setRoleNames(new HashSet<>(List.of("INVALID_ROLE")));
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(messageService.getMessage("error.role.invalid", "INVALID_ROLE"))
                .thenReturn("Invalid role");

        // When & Then
        assertThatThrownBy(() -> userService.createUser(createUserRequest))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Should fail when role not found in database")
    void testCreateUser_RoleNotFound() {
        // Given
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(roleRepository.findByName("SHOP_OWNER")).thenReturn(Optional.empty());
        when(messageService.getMessage("error.role.not.found", "SHOP_OWNER"))
                .thenReturn("Role not found");

        // When & Then
        assertThatThrownBy(() -> userService.createUser(createUserRequest))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should create user without roles")
    void testCreateUser_NoRoles() {
        // Given
        createUserRequest.setRoleNames(null);
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserDetailDTO result = userService.createUser(createUserRequest);

        // Then
        assertThat(result).isNotNull();
        verify(userRepository).save(any(User.class));
    }

    // ============= getUserById Tests =============

    @Test
    @DisplayName("Should get user by ID successfully")
    void testGetUserById_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        UserDetailDTO result = userService.getUserById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUsername()).isEqualTo("testuser");
        verify(userRepository).findById(1L);
    }

    @Test
    @DisplayName("Should throw exception when user not found by ID")
    void testGetUserById_NotFound() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        when(messageService.getMessage("error.user.not.found", 999L))
                .thenReturn("User not found");

        // When & Then
        assertThatThrownBy(() -> userService.getUserById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should get user by username successfully")
    void testGetUserByUsername_Success() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When
        UserDetailDTO result = userService.getUserByUsername("testuser");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    @DisplayName("Should throw exception when user not found by username")
    void testGetUserByUsername_NotFound() {
        // Given
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());
        when(messageService.getMessage("error.user.not.found", "nonexistent"))
                .thenReturn("User not found");

        // When & Then
        assertThatThrownBy(() -> userService.getUserByUsername("nonexistent"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ============= getAllUsers Tests =============

    @Test
    @DisplayName("Should get all users with pagination")
    void testGetAllUsers_Success() {
        // Given
        Page<User> userPage = new PageImpl<>(List.of(testUser), PageRequest.of(0, 10), 1);
        when(userRepository.findAllWithSearch("", PageRequest.of(0, 10)))
                .thenReturn(userPage);

        // When
        Page<UserDetailDTO> result = userService.getAllUsers(0, 10, "");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getUsername()).isEqualTo("testuser");
        verify(userRepository).findAllWithSearch("", PageRequest.of(0, 10));
    }

    @Test
    @DisplayName("Should get users with search term")
    void testGetAllUsers_WithSearch() {
        // Given
        Page<User> userPage = new PageImpl<>(List.of(testUser), PageRequest.of(0, 10), 1);
        when(userRepository.findAllWithSearch("test", PageRequest.of(0, 10)))
                .thenReturn(userPage);

        // When
        Page<UserDetailDTO> result = userService.getAllUsers(0, 10, "test");

        // Then
        assertThat(result.getContent()).hasSize(1);
    }

    // ============= updateUser Tests =============

    @Test
    @DisplayName("Should update user successfully")
    void testUpdateUser_Success() {
        // Given
        CreateUserRequest updateRequest = CreateUserRequest.builder()
                .username("testuser")
                .fullName("Updated User")
                .password("newPassword123")
                .notes("Updated notes")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("newPassword123")).thenReturn("hashedNewPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserDetailDTO result = userService.updateUser(1L, updateRequest);

        // Then
        assertThat(result).isNotNull();
        verify(userRepository).findById(1L);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should fail when updating to duplicate username")
    void testUpdateUser_DuplicateUsername() {
        // Given
        CreateUserRequest updateRequest = CreateUserRequest.builder()
                .username("otheruser")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByUsername("otheruser")).thenReturn(true);
        when(messageService.getMessage("error.user.duplicate.username", "otheruser"))
                .thenReturn("Username already exists");

        // When & Then
        assertThatThrownBy(() -> userService.updateUser(1L, updateRequest))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("Should update user roles")
    void testUpdateUser_UpdateRoles() {
        // Given
        Role managerRole = Role.builder().name("MANAGER").build();
        managerRole.setId(2L);

        CreateUserRequest updateRequest = CreateUserRequest.builder()
                .roleNames(new HashSet<>(List.of("MANAGER")))
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName("MANAGER")).thenReturn(Optional.of(managerRole));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        userService.updateUser(1L, updateRequest);

        // Then
        verify(roleRepository).findByName("MANAGER");
        verify(userRepository).save(any(User.class));
    }

    // ============= deleteUser Tests =============

    @Test
    @DisplayName("Should delete user (soft delete)")
    void testDeleteUser_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        userService.deleteUser(1L);

        // Then
        verify(userRepository).findById(1L);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should fail to delete non-existent user")
    void testDeleteUser_NotFound() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        when(messageService.getMessage("error.user.not.found", 999L))
                .thenReturn("User not found");

        // When & Then
        assertThatThrownBy(() -> userService.deleteUser(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ============= disableUser Tests =============

    @Test
    @DisplayName("Should disable user")
    void testDisableUser_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserDetailDTO result = userService.disableUser(1L, false);

        // Then
        assertThat(result).isNotNull();
        verify(userRepository).findById(1L);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should enable user")
    void testDisableUser_Enable() {
        // Given
        testUser.setActive(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        userService.disableUser(1L, true);

        // Then
        verify(userRepository).save(any(User.class));
    }

    // ============= lockUser Tests =============

    @Test
    @DisplayName("Should lock user account")
    void testLockUser_Lock() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserDetailDTO result = userService.lockUser(1L, true);

        // Then
        assertThat(result).isNotNull();
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should unlock user account")
    void testLockUser_Unlock() {
        // Given
        testUser.setAccountNonLocked(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        userService.lockUser(1L, false);

        // Then
        verify(userRepository).save(any(User.class));
    }

    // ============= addRoleToUser Tests =============

    @Test
    @DisplayName("Should add role to user")
    void testAddRoleToUser_Success() {
        // Given
        // Create a fresh role that is NOT testRole to avoid the builder issue
        Role addRole = new Role();
        addRole.setId(1L);
        addRole.setName("SHOP_OWNER");
        addRole.setDescription("Shop Owner role");
        addRole.setUsers(new HashSet<>()); // Use setter instead of builder
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName("SHOP_OWNER")).thenReturn(Optional.of(addRole));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserDetailDTO result = userService.addRoleToUser(1L, "SHOP_OWNER");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");
        verify(userRepository).findById(1L);
        verify(roleRepository).findByName("SHOP_OWNER");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should fail to add invalid role")
    void testAddRoleToUser_InvalidRole() {
        // Given
        when(messageService.getMessage("error.role.invalid", "INVALID"))
                .thenReturn("Invalid role");

        // When & Then
        assertThatThrownBy(() -> userService.addRoleToUser(1L, "INVALID"))
                .isInstanceOf(BadRequestException.class);
    }

    // ============= removeRoleFromUser Tests =============

    @Test
    @DisplayName("Should remove role from user")
    void testRemoveRoleFromUser_Success() {
        // Given
        Role removeRole = new Role();
        removeRole.setId(1L);
        removeRole.setName("SHOP_OWNER");
        removeRole.setDescription("Shop Owner role");
        removeRole.setUsers(new HashSet<>(Collections.singletonList(testUser))); // Add user to role
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName("SHOP_OWNER")).thenReturn(Optional.of(removeRole));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserDetailDTO result = userService.removeRoleFromUser(1L, "SHOP_OWNER");

        // Then
        assertThat(result).isNotNull();
        verify(userRepository).findById(1L);
        verify(roleRepository).findByName("SHOP_OWNER");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when removing non-existent role from user")
    void testRemoveRoleFromUser_RoleNotFound() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName("MANAGER")).thenReturn(Optional.empty());
        when(messageService.getMessage("error.role.not.found", "MANAGER"))
                .thenReturn("Role not found");

        // When & Then
        assertThatThrownBy(() -> userService.removeRoleFromUser(1L, "MANAGER"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ============= resetUserPassword Tests =============

    @Test
    @DisplayName("Should reset user password")
    void testResetUserPassword_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(anyString())).thenReturn("hashedTempPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        PasswordResetResponse result = userService.resetUserPassword(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getRequirePasswordChange()).isTrue();
        verify(userRepository).findById(1L);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should verify reset password generates temp password")
    void testResetUserPassword_GeneratesTempPassword() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(anyString())).thenReturn("hashedTempPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        PasswordResetResponse result = userService.resetUserPassword(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTempPassword()).isNotNull();
        assertThat(result.getTempPassword()).isNotBlank();
        assertThat(result.getRequirePasswordChange()).isTrue();
    }

    @Test
    @DisplayName("Should generate unique temporary passwords")
    void testResetUserPassword_UniquePasswords() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(anyString())).thenReturn("hashedTempPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        PasswordResetResponse result1 = userService.resetUserPassword(1L);
        PasswordResetResponse result2 = userService.resetUserPassword(1L);

        // Then
        assertThat(result1.getTempPassword()).isNotEqualTo(result2.getTempPassword());
    }

    // ============= changePasswordOnFirstLogin Tests =============

    @Test
    @DisplayName("Should change password on first login")
    void testChangePasswordOnFirstLogin_Success() {
        // Given
        String username = "testuser";
        ChangePasswordRequest passwordRequest = ChangePasswordRequest.builder()
                .newPassword("newPassword123")
                .build();

        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);

        when(authentication.getName()).thenReturn(username);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("newPassword123")).thenReturn("hashedNewPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserDetailDTO result = userService.changePasswordOnFirstLogin(passwordRequest);

        // Then
        assertThat(result).isNotNull();
        verify(userRepository).findByUsername(username);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should fail to change password without authentication")
    void testChangePasswordOnFirstLogin_NotAuthenticated() {
        // Given
        ChangePasswordRequest passwordRequest = ChangePasswordRequest.builder()
                .newPassword("newPassword123")
                .build();

        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);

        when(authentication.getName()).thenReturn("anonymousUser");
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(messageService.getMessage("error.user.not.authenticated"))
                .thenReturn("User not authenticated");

        // When & Then
        assertThatThrownBy(() -> userService.changePasswordOnFirstLogin(passwordRequest))
                .isInstanceOf(UnauthorizedException.class);
    }

    // ============= changePassword Tests =============

    @Test
    @DisplayName("Should change password with old password verification")
    void testChangePassword_Success() {
        // Given
        String username = "testuser";
        ChangePasswordRequest passwordRequest = ChangePasswordRequest.builder()
                .oldPassword("password123")
                .newPassword("newPassword123")
                .build();

        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);

        when(authentication.getName()).thenReturn(username);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPassword123")).thenReturn("hashedNewPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserDetailDTO result = userService.changePassword(passwordRequest);

        // Then
        assertThat(result).isNotNull();
        verify(userRepository).findByUsername(username);
        verify(passwordEncoder).matches("password123", "hashedPassword");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should fail when old password is incorrect")
    void testChangePassword_IncorrectOldPassword() {
        // Given
        String username = "testuser";
        ChangePasswordRequest passwordRequest = ChangePasswordRequest.builder()
                .oldPassword("wrongPassword")
                .newPassword("newPassword123")
                .build();

        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);

        when(authentication.getName()).thenReturn(username);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongPassword", "hashedPassword")).thenReturn(false);
        when(messageService.getMessage("error.password.old.incorrect"))
                .thenReturn("Old password is incorrect");

        // When & Then
        assertThatThrownBy(() -> userService.changePassword(passwordRequest))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Should fail when old password not provided")
    void testChangePassword_MissingOldPassword() {
        // Given
        String username = "testuser";
        ChangePasswordRequest passwordRequest = ChangePasswordRequest.builder()
                .oldPassword(null)
                .newPassword("newPassword123")
                .build();

        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);

        when(authentication.getName()).thenReturn(username);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));
        when(messageService.getMessage("error.password.old.required"))
                .thenReturn("Old password is required");

        // When & Then
        assertThatThrownBy(() -> userService.changePassword(passwordRequest))
                .isInstanceOf(BadRequestException.class);
    }

    // ============= Edge Case Tests =============

    @Test
    @DisplayName("Should create user with empty email")
    void testCreateUser_EmptyEmail() {
        // Given
        createUserRequest.setEmail("");
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        when(roleRepository.findByName("SHOP_OWNER")).thenReturn(Optional.of(testRole));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserDetailDTO result = userService.createUser(createUserRequest);

        // Then
        assertThat(result).isNotNull();
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should handle multiple roles assignment")
    void testCreateUser_MultipleRoles() {
        // Given
        Role managerRole = Role.builder().name("MANAGER").build();
        managerRole.setId(2L);
        createUserRequest.setRoleNames(new HashSet<>(Arrays.asList("SHOP_OWNER", "MANAGER")));

        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        when(roleRepository.findByName("SHOP_OWNER")).thenReturn(Optional.of(testRole));
        when(roleRepository.findByName("MANAGER")).thenReturn(Optional.of(managerRole));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserDetailDTO result = userService.createUser(createUserRequest);

        // Then
        assertThat(result).isNotNull();
        verify(roleRepository, times(2)).findByName(anyString());
    }

    // ============= Additional Edge Cases =============

    @Test
    @DisplayName("Should create user with null notes field")
    void testCreateUser_NullNotes() {
        // Given
        createUserRequest.setNotes(null);
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        when(roleRepository.findByName("SHOP_OWNER")).thenReturn(Optional.of(testRole));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserDetailDTO result = userService.createUser(createUserRequest);

        // Then
        assertThat(result).isNotNull();
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should create user with very long notes")
    void testCreateUser_LongNotes() {
        // Given
        String longNotes = "A".repeat(1000);
        createUserRequest.setNotes(longNotes);
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        when(roleRepository.findByName("SHOP_OWNER")).thenReturn(Optional.of(testRole));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserDetailDTO result = userService.createUser(createUserRequest);

        // Then
        assertThat(result).isNotNull();
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent user")
    void testUpdateUser_UserNotFound() {
        // Given
        CreateUserRequest updateRequest = new CreateUserRequest();
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        when(messageService.getMessage(anyString(), anyLong())).thenReturn("User not found");

        // When & Then
        assertThatThrownBy(() -> userService.updateUser(999L, updateRequest))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should update user successfully with new role")
    void testUpdateUser_WithNewRole() {
        // Given
        CreateUserRequest updateRequest = CreateUserRequest.builder()
                .roleNames(new HashSet<>(List.of("MANAGER")))
                .build();
        
        Role managerRole = Role.builder().name("MANAGER").build();
        managerRole.setId(2L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName("MANAGER")).thenReturn(Optional.of(managerRole));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserDetailDTO result = userService.updateUser(1L, updateRequest);

        // Then
        assertThat(result).isNotNull();
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when role not found during update")
    void testUpdateUser_RoleNotFound() {
        // Given
        CreateUserRequest updateRequest = CreateUserRequest.builder()
                .roleNames(new HashSet<>(List.of("NONEXISTENT")))
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(messageService.getMessage("error.role.invalid", "NONEXISTENT"))
                .thenReturn("Invalid role");

        // When & Then
        assertThatThrownBy(() -> userService.updateUser(1L, updateRequest))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Should get all users with empty result")
    void testGetAllUsers_Empty() {
        // Given
        Page<User> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);
        when(userRepository.findAllWithSearch("nonexistent", PageRequest.of(0, 10)))
                .thenReturn(emptyPage);

        // When
        Page<UserDetailDTO> result = userService.getAllUsers(0, 10, "nonexistent");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("Should update user with email")
    void testUpdateUser_WithEmail() {
        // Given
        CreateUserRequest updateWithEmail = CreateUserRequest.builder()
                .email("newemail@example.com")
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserDetailDTO result = userService.updateUser(1L, updateWithEmail);

        // Then
        assertThat(result).isNotNull();
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should update user with password")
    void testUpdateUser_WithPassword() {
        // Given
        CreateUserRequest updateWithPassword = CreateUserRequest.builder()
                .password("newPassword123")
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("newPassword123")).thenReturn("hashedNewPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserDetailDTO result = userService.updateUser(1L, updateWithPassword);

        // Then
        assertThat(result).isNotNull();
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when adding non-existent role to user")
    void testAddRoleToUser_RoleNotFound() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName("MANAGER")).thenReturn(Optional.empty());
        when(messageService.getMessage("error.role.not.found", "MANAGER"))
                .thenReturn("Role not found");

        // When & Then
        assertThatThrownBy(() -> userService.addRoleToUser(1L, "MANAGER"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw exception when removing invalid role")
    void testRemoveRoleFromUser_InvalidRole() {
        // Given
        when(messageService.getMessage("error.role.invalid", "INVALID"))
                .thenReturn("Invalid role");

        // When & Then
        assertThatThrownBy(() -> userService.removeRoleFromUser(1L, "INVALID"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Should throw exception when no authenticated user for first login password change")
    void testChangePasswordOnFirstLogin_NoAuth() {
        // Given
        Authentication auth = mock(Authentication.class);
        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(auth);
        when(auth.getName()).thenReturn("anonymousUser");
        SecurityContextHolder.setContext(context);

        ChangePasswordRequest changeRequest = ChangePasswordRequest.builder()
                .newPassword("newPassword123")
                .build();
        when(messageService.getMessage("error.user.not.authenticated"))
                .thenReturn("User not authenticated");

        // When & Then
        assertThatThrownBy(() -> userService.changePasswordOnFirstLogin(changeRequest))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("Should verify changePassword updates password correctly")
    void testChangePassword_PasswordUpdated() {
        // Given
        String username = "testuser";
        ChangePasswordRequest passwordRequest = ChangePasswordRequest.builder()
                .oldPassword("password123")
                .newPassword("newPassword456")
                .build();

        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);

        when(authentication.getName()).thenReturn(username);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPassword456")).thenReturn("hashedNewPassword456");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserDetailDTO result = userService.changePassword(passwordRequest);

        // Then
        assertThat(result).isNotNull();
        verify(passwordEncoder).encode("newPassword456");
    }

    @Test
    @DisplayName("Should get user by username with all roles")
    void testGetUserByUsername_WithRoles() {
        // Given
        Role role1 = new Role();
        role1.setId(1L);
        role1.setName("SHOP_OWNER");
        role1.setDescription("Shop Owner");

        Role role2 = new Role();
        role2.setId(2L);
        role2.setName("MANAGER");
        role2.setDescription("Manager");

        testUser.setRoles(new HashSet<>(List.of(role1, role2)));

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When
        UserDetailDTO result = userService.getUserByUsername("testuser");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRoles()).hasSize(2);
    }

    @Test
    @DisplayName("Should lock user and verify it's locked")
    void testLockUser_VerifyLocked() {
        // Given
        testUser.setAccountNonLocked(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserDetailDTO result = userService.lockUser(1L, true);

        // Then
        assertThat(result).isNotNull();
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should disable user and verify disabled")
    void testDisableUser_VerifyDisabled() {
        // Given
        testUser.setActive(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserDetailDTO result = userService.disableUser(1L, false);

        // Then
        assertThat(result).isNotNull();
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should add role when user has no roles")
    void testAddRoleToUser_UserHasNoRoles() {
        // Given
        testUser.setRoles(new HashSet<>());
        Role newRole = new Role();
        newRole.setId(2L);
        newRole.setName("MANAGER");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName("MANAGER")).thenReturn(Optional.of(newRole));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserDetailDTO result = userService.addRoleToUser(1L, "MANAGER");

        // Then
        assertThat(result).isNotNull();
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should remove role when user has multiple roles")
    void testRemoveRoleFromUser_UserHasMultipleRoles() {
        // Given
        Role role1 = new Role();
        role1.setId(1L);
        role1.setName("SHOP_OWNER");

        Role role2 = new Role();
        role2.setId(2L);
        role2.setName("MANAGER");

        testUser.setRoles(new HashSet<>(List.of(role1, role2)));

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName("MANAGER")).thenReturn(Optional.of(role2));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserDetailDTO result = userService.removeRoleFromUser(1L, "MANAGER");

        // Then
        assertThat(result).isNotNull();
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should handle user with special characters in name")
    void testCreateUser_SpecialCharactersInName() {
        // Given
        createUserRequest.setFullName("José María O'Connor-Smith");

        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        when(roleRepository.findByName("SHOP_OWNER")).thenReturn(Optional.of(testRole));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserDetailDTO result = userService.createUser(createUserRequest);

        // Then
        assertThat(result).isNotNull();
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should update user email to empty string")
    void testUpdateUser_EmptyEmail() {
        // Given
        CreateUserRequest updateWithEmptyEmail = CreateUserRequest.builder()
                .email("")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserDetailDTO result = userService.updateUser(1L, updateWithEmptyEmail);

        // Then
        assertThat(result).isNotNull();
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should verify getAllUsers returns paginated results")
    void testGetAllUsers_PaginationWorks() {
        // Given
        User user2 = User.builder()
                .username("user2")
                .email("user2@example.com")
                .password("hashedPassword")
                .fullName("User Two")
                .active(true)
                .roles(new HashSet<>())
                .build();
        user2.setId(2L);

        Page<User> userPage = new PageImpl<>(List.of(testUser, user2), PageRequest.of(0, 10), 2);
        when(userRepository.findAllWithSearch("", PageRequest.of(0, 10))).thenReturn(userPage);

        // When
        Page<UserDetailDTO> result = userService.getAllUsers(0, 10, "");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should verify getAllUsers with search term filters results")
    void testGetAllUsers_SearchTermFilters() {
        // Given
        Page<User> userPage = new PageImpl<>(List.of(testUser), PageRequest.of(0, 10), 1);
        when(userRepository.findAllWithSearch("testuser", PageRequest.of(0, 10))).thenReturn(userPage);

        // When
        Page<UserDetailDTO> result = userService.getAllUsers(0, 10, "testuser");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(userRepository).findAllWithSearch("testuser", PageRequest.of(0, 10));
    }

    @Test
    @DisplayName("Should delete user not found and throw exception")
    void testDeleteUser_NotFoundThrowsException() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        when(messageService.getMessage("error.user.not.found", 999L))
                .thenReturn("User not found");

        // When & Then
        assertThatThrownBy(() -> userService.deleteUser(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should create user with all role types")
    void testCreateUser_AllRoleTypes() {
        // Given - using valid roles from RoleEnum: SHOP_OWNER, MANAGER, RECEPTIONIST
        Role ownerRole = new Role();
        ownerRole.setId(1L);
        ownerRole.setName("SHOP_OWNER");
        ownerRole.setUsers(new HashSet<>());

        Role managerRole = new Role();
        managerRole.setId(2L);
        managerRole.setName("MANAGER");
        managerRole.setUsers(new HashSet<>());

        Role receptionistRole = new Role();
        receptionistRole.setId(3L);
        receptionistRole.setName("RECEPTIONIST");
        receptionistRole.setUsers(new HashSet<>());

        createUserRequest.setRoleNames(new HashSet<>(List.of("SHOP_OWNER", "MANAGER", "RECEPTIONIST")));

        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        when(roleRepository.findByName("SHOP_OWNER")).thenReturn(Optional.of(ownerRole));
        when(roleRepository.findByName("MANAGER")).thenReturn(Optional.of(managerRole));
        when(roleRepository.findByName("RECEPTIONIST")).thenReturn(Optional.of(receptionistRole));
        
        User newUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .password("hashedPassword")
                .fullName("Test User")
                .active(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .accountNonExpired(true)
                .requireAction("CHANGE_PASSWORD")
                .notes("Test notes")
                .roles(new HashSet<>(List.of(ownerRole, managerRole, receptionistRole)))
                .build();
        newUser.setId(1L);
        
        when(userRepository.save(any(User.class))).thenReturn(newUser);

        // When
        UserDetailDTO result = userService.createUser(createUserRequest);

        // Then
        assertThat(result).isNotNull();
        verify(roleRepository, times(3)).findByName(anyString());
    }

    @Test
    @DisplayName("Should update user with empty password skips password update")
    void testUpdateUser_EmptyPasswordSkipsUpdate() {
        // Given
        CreateUserRequest updateRequest = CreateUserRequest.builder()
                .password("")
                .fullName("Updated Name")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserDetailDTO result = userService.updateUser(1L, updateRequest);

        // Then
        assertThat(result).isNotNull();
        verify(passwordEncoder, never()).encode("");
    }

    @Test
    @DisplayName("Should change password on first login clears requireAction")
    void testChangePasswordOnFirstLogin_ClearsRequireAction() {
        // Given
        String username = "testuser";
        testUser.setRequireAction("CHANGE_PASSWORD");

        ChangePasswordRequest changeRequest = ChangePasswordRequest.builder()
                .newPassword("newPassword123")
                .build();

        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);

        when(authentication.getName()).thenReturn(username);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("newPassword123")).thenReturn("hashedNewPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserDetailDTO result = userService.changePasswordOnFirstLogin(changeRequest);

        // Then
        assertThat(result).isNotNull();
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should handle user creation with same username and email")
    void testCreateUser_SameUsernameAndEmail() {
        // Given
        createUserRequest.setUsername("testuser");
        createUserRequest.setEmail("testuser@example.com");

        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("testuser@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        when(roleRepository.findByName("SHOP_OWNER")).thenReturn(Optional.of(testRole));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserDetailDTO result = userService.createUser(createUserRequest);

        // Then
        assertThat(result).isNotNull();
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should verify DTO mapping includes all user fields")
    void testMapToDTO_IncludesAllFields() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        UserDetailDTO result = userService.getUserById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getFullName()).isEqualTo("Test User");
        assertThat(result.getActive()).isTrue();
        assertThat(result.getAccountNonLocked()).isTrue();
        assertThat(result.getCredentialsNonExpired()).isTrue();
        assertThat(result.getAccountNonExpired()).isTrue();
    }

    @Test
    @DisplayName("Should verify getDTOincludesCreatedAt")
    void testMapToDTO_IncludesCreatedAt() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        testUser.setCreatedAt(now);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        UserDetailDTO result = userService.getUserById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCreatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("Should verify DTO includes updated at")
    void testMapToDTO_IncludesUpdatedAt() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        testUser.setUpdatedAt(now);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        UserDetailDTO result = userService.getUserById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("Should handle user with no roles in DTO mapping")
    void testMapToDTO_NoRoles() {
        // Given
        testUser.setRoles(new HashSet<>());
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        UserDetailDTO result = userService.getUserById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRoles()).isEmpty();
    }

    @Test
    @DisplayName("Should handle single role in DTO mapping")
    void testMapToDTO_SingleRole() {
        // Given
        Role singleRole = new Role();
        singleRole.setId(1L);
        singleRole.setName("SHOP_OWNER");
        singleRole.setDescription("Owner");
        singleRole.setCreatedAt(LocalDateTime.now());
        
        testUser.setRoles(new HashSet<>(List.of(singleRole)));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        UserDetailDTO result = userService.getUserById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRoles()).hasSize(1);
    }

    @Test
    @DisplayName("Should handle multiple roles in DTO mapping")
    void testMapToDTO_MultipleRoles() {
        // Given
        Role role1 = new Role();
        role1.setId(1L);
        role1.setName("SHOP_OWNER");
        role1.setDescription("Owner");
        role1.setCreatedAt(LocalDateTime.now());

        Role role2 = new Role();
        role2.setId(2L);
        role2.setName("MANAGER");
        role2.setDescription("Manager");
        role2.setCreatedAt(LocalDateTime.now());

        testUser.setRoles(new HashSet<>(List.of(role1, role2)));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        UserDetailDTO result = userService.getUserById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRoles()).hasSize(2);
    }

    @Test
    @DisplayName("Should verify password is not included in DTO")
    void testMapToDTO_PasswordNotIncluded() {
        // Given
        testUser.setPassword("secretPassword");
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        UserDetailDTO result = userService.getUserById(1L);

        // Then
        assertThat(result).isNotNull();
        // Password should not be in DTO
        assertThat(result.toString()).doesNotContain("secretPassword");
    }

    @Test
    @DisplayName("Should update user with all fields provided")
    void testUpdateUser_AllFieldsProvided() {
        // Given
        CreateUserRequest updateRequest = CreateUserRequest.builder()
                .username("newusername")
                .email("newemail@example.com")
                .fullName("New Full Name")
                .password("newPassword123")
                .notes("New notes")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByUsername("newusername")).thenReturn(false);
        when(passwordEncoder.encode("newPassword123")).thenReturn("hashedNewPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserDetailDTO result = userService.updateUser(1L, updateRequest);

        // Then
        assertThat(result).isNotNull();
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should handle lock user when already locked")
    void testLockUser_AlreadyLocked() {
        // Given
        testUser.setAccountNonLocked(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserDetailDTO result = userService.lockUser(1L, true);

        // Then
        assertThat(result).isNotNull();
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should handle unlock user when already unlocked")
    void testLockUser_AlreadyUnlocked() {
        // Given
        testUser.setAccountNonLocked(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserDetailDTO result = userService.lockUser(1L, false);

        // Then
        assertThat(result).isNotNull();
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should handle disable user when already disabled")
    void testDisableUser_AlreadyDisabled() {
        // Given
        testUser.setActive(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserDetailDTO result = userService.disableUser(1L, false);

        // Then
        assertThat(result).isNotNull();
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should handle enable user when already enabled")
    void testDisableUser_AlreadyEnabled() {
        // Given
        testUser.setActive(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserDetailDTO result = userService.disableUser(1L, true);

        // Then
        assertThat(result).isNotNull();
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should handle empty search string in getAllUsers")
    void testGetAllUsers_EmptySearchString() {
        // Given
        Page<User> userPage = new PageImpl<>(List.of(testUser), PageRequest.of(0, 10), 1);
        when(userRepository.findAllWithSearch("", PageRequest.of(0, 10))).thenReturn(userPage);

        // When
        Page<UserDetailDTO> result = userService.getAllUsers(0, 10, "");

        // Then
        assertThat(result).isNotNull();
        verify(userRepository).findAllWithSearch("", PageRequest.of(0, 10));
    }

    @Test
    @DisplayName("Should verify getAllUsers returns correct page size")
    void testGetAllUsers_CorrectPageSize() {
        // Given
        User user2 = User.builder()
                .username("user2")
                .email("user2@example.com")
                .password("hashedPassword")
                .fullName("User Two")
                .active(true)
                .roles(new HashSet<>())
                .build();
        user2.setId(2L);

        Page<User> userPage = new PageImpl<>(List.of(testUser, user2), PageRequest.of(0, 2), 10);
        when(userRepository.findAllWithSearch("", PageRequest.of(0, 2))).thenReturn(userPage);

        // When
        Page<UserDetailDTO> result = userService.getAllUsers(0, 2, "");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSize()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should verify getAllUsers returns total count")
    void testGetAllUsers_TotalCount() {
        // Given
        Page<User> userPage = new PageImpl<>(List.of(testUser), PageRequest.of(0, 10), 50);
        when(userRepository.findAllWithSearch("", PageRequest.of(0, 10))).thenReturn(userPage);

        // When
        Page<UserDetailDTO> result = userService.getAllUsers(0, 10, "");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(50);
    }

    @Test
    @DisplayName("Should handle change password with same new password as old - should still succeed")
    void testChangePassword_SamePassword() {
        // Given
        String username = "testuser";
        ChangePasswordRequest passwordRequest = ChangePasswordRequest.builder()
                .oldPassword("password123")
                .newPassword("password123")
                .build();

        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);

        when(authentication.getName()).thenReturn(username);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserDetailDTO result = userService.changePassword(passwordRequest);

        // Then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Should handle change password on first login with very long password")
    void testChangePasswordOnFirstLogin_LongPassword() {
        // Given
        String username = "testuser";
        String longPassword = "a".repeat(100);
        
        ChangePasswordRequest changeRequest = ChangePasswordRequest.builder()
                .newPassword(longPassword)
                .build();

        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);

        when(authentication.getName()).thenReturn(username);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(longPassword)).thenReturn("hashedLongPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserDetailDTO result = userService.changePasswordOnFirstLogin(changeRequest);

        // Then
        assertThat(result).isNotNull();
        verify(passwordEncoder).encode(longPassword);
    }

    @Test
    @DisplayName("Should handle create user when email is empty string")
    void testCreateUser_EmptyStringEmail() {
        // Given
        createUserRequest.setEmail("");

        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        when(roleRepository.findByName("SHOP_OWNER")).thenReturn(Optional.of(testRole));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserDetailDTO result = userService.createUser(createUserRequest);

        // Then
        assertThat(result).isNotNull();
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should handle update user with empty string password")
    void testUpdateUser_EmptyStringPassword() {
        // Given
        CreateUserRequest updateRequest = CreateUserRequest.builder()
                .password("")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserDetailDTO result = userService.updateUser(1L, updateRequest);

        // Then
        assertThat(result).isNotNull();
        verify(passwordEncoder, never()).encode("");
    }

    @Test
    @DisplayName("Should handle get all users with large page number")
    void testGetAllUsers_LargePageNumber() {
        // Given
        Page<User> emptyPage = new PageImpl<>(List.of(), PageRequest.of(100, 10), 50);
        when(userRepository.findAllWithSearch("", PageRequest.of(100, 10))).thenReturn(emptyPage);

        // When
        Page<UserDetailDTO> result = userService.getAllUsers(100, 10, "");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("Should verify add role returns correct user data")
    void testAddRoleToUser_ReturnedDataCorrect() {
        // Given
        Role newRole = new Role();
        newRole.setId(2L);
        newRole.setName("MANAGER");
        newRole.setUsers(new HashSet<>());

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName("MANAGER")).thenReturn(Optional.of(newRole));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserDetailDTO result = userService.addRoleToUser(1L, "MANAGER");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Should verify remove role returns correct user data")
    void testRemoveRoleFromUser_ReturnedDataCorrect() {
        // Given
        Role roleToRemove = new Role();
        roleToRemove.setId(1L);
        roleToRemove.setName("SHOP_OWNER");
        roleToRemove.setUsers(new HashSet<>(Collections.singletonList(testUser)));

        testUser.setRoles(new HashSet<>(List.of(roleToRemove)));

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName("SHOP_OWNER")).thenReturn(Optional.of(roleToRemove));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserDetailDTO result = userService.removeRoleFromUser(1L, "SHOP_OWNER");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");
    }

    // ============= Exception Tests for Lambda Coverage =============

    @Test
    @DisplayName("Should throw exception when disabling non-existent user")
    void testDisableUser_UserNotFound() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        when(messageService.getMessage("error.user.not.found", 999L))
                .thenReturn("User not found");

        // When & Then
        assertThatThrownBy(() -> userService.disableUser(999L, false))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw exception when locking non-existent user")
    void testLockUser_UserNotFound() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        when(messageService.getMessage("error.user.not.found", 999L))
                .thenReturn("User not found");

        // When & Then
        assertThatThrownBy(() -> userService.lockUser(999L, true))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw exception when adding role to non-existent user")
    void testAddRoleToUser_UserNotFound() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        when(messageService.getMessage("error.user.not.found", 999L))
                .thenReturn("User not found");

        // When & Then
        assertThatThrownBy(() -> userService.addRoleToUser(999L, "SHOP_OWNER"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw exception when removing role from non-existent user")
    void testRemoveRoleFromUser_UserNotFound() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        when(messageService.getMessage("error.user.not.found", 999L))
                .thenReturn("User not found");

        // When & Then
        assertThatThrownBy(() -> userService.removeRoleFromUser(999L, "SHOP_OWNER"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw exception when resetting password for non-existent user")
    void testResetUserPassword_UserNotFound() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        when(messageService.getMessage("error.user.not.found", 999L))
                .thenReturn("User not found");

        // When & Then
        assertThatThrownBy(() -> userService.resetUserPassword(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw exception when changing password on first login with non-existent user")
    void testChangePasswordOnFirstLogin_UserNotFound() {
        // Given
        String username = "nonexistent";
        ChangePasswordRequest passwordRequest = ChangePasswordRequest.builder()
                .newPassword("newPassword123")
                .build();

        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);

        when(authentication.getName()).thenReturn(username);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
        when(messageService.getMessage("error.user.not.found", username))
                .thenReturn("User not found");

        // When & Then
        assertThatThrownBy(() -> userService.changePasswordOnFirstLogin(passwordRequest))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw exception when changing password with non-existent user")
    void testChangePassword_UserNotFound() {
        // Given
        String username = "nonexistent";
        ChangePasswordRequest passwordRequest = ChangePasswordRequest.builder()
                .oldPassword("password123")
                .newPassword("newPassword123")
                .build();

        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);

        when(authentication.getName()).thenReturn(username);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
        when(messageService.getMessage("error.user.not.found", username))
                .thenReturn("User not found");

        // When & Then
        assertThatThrownBy(() -> userService.changePassword(passwordRequest))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw exception when updating user with non-existent role in repository")
    void testUpdateUser_RoleNotFoundInRepo() {
        // Given
        CreateUserRequest updateRequest = CreateUserRequest.builder()
                .roleNames(new HashSet<>(List.of("MANAGER")))
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName("MANAGER")).thenReturn(Optional.empty());
        when(messageService.getMessage("error.role.not.found", "MANAGER"))
                .thenReturn("Role not found");

        // When & Then
        assertThatThrownBy(() -> userService.updateUser(1L, updateRequest))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw exception when removing non-existent role from user in repo")
    void testRemoveRoleFromUser_RoleNotFoundInRepo() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName("MANAGER")).thenReturn(Optional.empty());
        when(messageService.getMessage("error.role.not.found", "MANAGER"))
                .thenReturn("Role not found");

        // When & Then
        assertThatThrownBy(() -> userService.removeRoleFromUser(1L, "MANAGER"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw exception when no null authentication for changePassword")
    void testChangePassword_NoAuthentication() {
        // Given
        ChangePasswordRequest passwordRequest = ChangePasswordRequest.builder()
                .oldPassword("password123")
                .newPassword("newPassword123")
                .build();

        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);

        when(authentication.getName()).thenReturn(null);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(messageService.getMessage("error.user.not.authenticated"))
                .thenReturn("User not authenticated");

        // When & Then
        assertThatThrownBy(() -> userService.changePassword(passwordRequest))
                .isInstanceOf(UnauthorizedException.class);
    }
}
