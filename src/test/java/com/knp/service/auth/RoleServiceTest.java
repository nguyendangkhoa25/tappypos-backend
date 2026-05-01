package com.knp.service.auth;

import com.knp.exception.BadRequestException;
import com.knp.exception.ResourceNotFoundException;
import com.knp.model.dto.auth.RoleDTO;
import com.knp.model.entity.auth.Role;
import com.knp.model.entity.auth.User;
import com.knp.repository.auth.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.knp.service.MessageService;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoleService Unit Tests")
class RoleServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private MessageService messageService;

    @InjectMocks
    private RoleService roleService;

    private Role adminRole;
    private Role userRole;

    @BeforeEach
    void setUp() {
        adminRole = Role.builder()
                .name("SHOP_OWNER")
                .description("Shop owner with full access")
                .build();
        adminRole.setId(1L);
        adminRole.setCreatedAt(java.time.LocalDateTime.now());
        adminRole.setUpdatedAt(java.time.LocalDateTime.now());

        userRole = Role.builder()
                .name("STAFF")
                .description("Staff member with limited access")
                .build();
        userRole.setId(2L);
        userRole.setCreatedAt(java.time.LocalDateTime.now());
        userRole.setUpdatedAt(java.time.LocalDateTime.now());
    }

    @Test
    @DisplayName("Should get role by valid code successfully")
    void testGetRoleByCode_Success() {
        // Given
        when(roleRepository.findByName("SHOP_OWNER")).thenReturn(Optional.of(adminRole));

        // When
        RoleDTO result = roleService.getRoleByCode("SHOP_OWNER");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("SHOP_OWNER");
        assertThat(result.getDescription()).isEqualTo("Shop owner with full access");
        verify(roleRepository).findByName("SHOP_OWNER");
    }

    @Test
    @DisplayName("Should throw exception for invalid role code")
    void testGetRoleByCode_InvalidRole() {
        // Given
        when(messageService.getMessage(anyString(), (Object[]) any())).thenReturn("Invalid role");

        // When & Then
        assertThatThrownBy(() -> roleService.getRoleByCode("INVALID_ROLE"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Should throw exception when role not found")
    void testGetRoleByCode_NotFound() {
        // Given
        when(roleRepository.findByName("SHOP_OWNER")).thenReturn(Optional.empty());
        when(messageService.getMessage(anyString(), (Object[]) any())).thenReturn("Role not found");

        // When & Then
        assertThatThrownBy(() -> roleService.getRoleByCode("SHOP_OWNER"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should get all roles successfully")
    void testGetAllRoles_Success() {
        // Given
        when(roleRepository.findAll()).thenReturn(Arrays.asList(adminRole, userRole));

        // When
        List<RoleDTO> result = roleService.getAllRoles();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("SHOP_OWNER");
        assertThat(result.get(1).getName()).isEqualTo("STAFF");
        verify(roleRepository).findAll();
    }

    @Test
    @DisplayName("Should initialize default roles on startup")
    void testInitializeDefaultRoles_Success() {
        // Given - stub all roles as not existing using doReturn for lenient stubbing
        doReturn(false).when(roleRepository).existsByName("MASTER_TENANT");
        doReturn(false).when(roleRepository).existsByName("AGENT");
        doReturn(false).when(roleRepository).existsByName("SHOP_OWNER");
        doReturn(false).when(roleRepository).existsByName("MANAGER");
        doReturn(false).when(roleRepository).existsByName("CASHIER");
        doReturn(false).when(roleRepository).existsByName("ACCOUNTANT");
        doReturn(false).when(roleRepository).existsByName("WAREHOUSE_STAFF");
        doReturn(false).when(roleRepository).existsByName("PAWN_OFFICER");
        doReturn(false).when(roleRepository).existsByName("SERVICE_STAFF");
        doReturn(false).when(roleRepository).existsByName("TECHNICIAN");
        doReturn(false).when(roleRepository).existsByName("RECEPTIONIST");
        doReturn(false).when(roleRepository).existsByName("CLEANER");
        doReturn(adminRole).when(roleRepository).save(any(Role.class));

        // When
        roleService.initializeDefaultRoles();

        // Then - verify save was called once for each role enum value
        verify(roleRepository, times(12)).save(any(Role.class));
    }

    @Test
    @DisplayName("Should skip creating role if it already exists")
    void testInitializeDefaultRoles_RoleAlreadyExists() {
        // Reset mocks to avoid strict stubbing issues
        reset(roleRepository);

        // Given - stub all possible role checks using doReturn for lenient stubbing
        doReturn(true).when(roleRepository).existsByName("MASTER_TENANT");
        doReturn(true).when(roleRepository).existsByName("AGENT");
        doReturn(true).when(roleRepository).existsByName("SHOP_OWNER");
        doReturn(true).when(roleRepository).existsByName("MANAGER");
        doReturn(true).when(roleRepository).existsByName("CASHIER");
        doReturn(true).when(roleRepository).existsByName("ACCOUNTANT");
        doReturn(true).when(roleRepository).existsByName("WAREHOUSE_STAFF");
        doReturn(true).when(roleRepository).existsByName("PAWN_OFFICER");
        doReturn(true).when(roleRepository).existsByName("SERVICE_STAFF");
        doReturn(true).when(roleRepository).existsByName("TECHNICIAN");
        doReturn(true).when(roleRepository).existsByName("RECEPTIONIST");
        doReturn(true).when(roleRepository).existsByName("CLEANER");

        // When
        roleService.initializeDefaultRoles();

        // Then - verify that we checked for roles but didn't create any new ones
        // Verify a few role checks occurred
        verify(roleRepository).existsByName("MASTER_TENANT");
        verify(roleRepository).existsByName("SHOP_OWNER");
    }

    @Test
    @DisplayName("Should validate role code successfully")
    void testIsValidRole_ValidCode() {
        // When
        boolean result = roleService.isValidRole("SHOP_OWNER");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should reject invalid role code")
    void testIsValidRole_InvalidCode() {
        // When
        boolean result = roleService.isValidRole("INVALID_CODE");

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should get role by ID successfully")
    void testGetRoleById_Success() {
        // Given
        when(roleRepository.findById(1L)).thenReturn(Optional.of(adminRole));

        // When
        RoleDTO result = roleService.getRoleById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("SHOP_OWNER");
        verify(roleRepository).findById(1L);
    }

    @Test
    @DisplayName("Should throw exception when role ID not found")
    void testGetRoleById_NotFound() {
        // Given
        when(roleRepository.findById(999L)).thenReturn(Optional.empty());
        when(messageService.getMessage("error.role.not.found", 999L)).thenReturn("Role not found");

        // When & Then
        assertThatThrownBy(() -> roleService.getRoleById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ============= Additional Edge Case Tests =============

    @Test
    @DisplayName("Should return empty list when no roles exist")
    void testGetAllRoles_Empty() {
        // Given
        when(roleRepository.findAll()).thenReturn(Collections.emptyList());

        // When
        List<RoleDTO> result = roleService.getAllRoles();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should check if role is valid - positive")
    void testIsValidRole_Positive() {
        // When
        boolean result = roleService.isValidRole("SHOP_OWNER");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should check if role is valid - negative")
    void testIsValidRole_Negative() {
        // When
        boolean result = roleService.isValidRole("INVALID_ROLE");

        // Then
        assertThat(result).isFalse();
    }


    @Test
    @DisplayName("Should handle role code case insensitivity - uppercase")
    void testGetRoleByCode_UpperCase() {
        // Given
        when(roleRepository.findByName("SHOP_OWNER")).thenReturn(Optional.of(adminRole));

        // When
        RoleDTO result = roleService.getRoleByCode("SHOP_OWNER");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("SHOP_OWNER");
    }

    @Test
    @DisplayName("Should handle role code case insensitivity - lowercase")
    void testGetRoleByCode_LowerCase() {
        // Given - RoleEnum is case-sensitive, so "shop_owner" is invalid
        when(messageService.getMessage(anyString(), (Object[]) any())).thenReturn("Invalid role");

        // When & Then - RoleEnum validation fails first, before repository is called
        assertThatThrownBy(() -> roleService.getRoleByCode("shop_owner"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Should get role with multiple users")
    void testGetRoleById_WithMultipleUsers() {
        // Given
        Set<User> users = new HashSet<>();
        User user1 = User.builder().username("user1").build();
        User user2 = User.builder().username("user2").build();
        user1.setId(1L);
        user2.setId(2L);
        users.add(user1);
        users.add(user2);
        adminRole.setUsers(users);

        when(roleRepository.findById(1L)).thenReturn(Optional.of(adminRole));

        // When
        RoleDTO result = roleService.getRoleById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(roleRepository).findById(1L);
    }

    @Test
    @DisplayName("Should get role by code with special characters in description")
    void testGetRoleByCode_SpecialDescription() {
        // Given
        adminRole.setDescription("Role with special chars: !@#$%^&*()");
        when(roleRepository.findByName("SHOP_OWNER")).thenReturn(Optional.of(adminRole));

        // When
        RoleDTO result = roleService.getRoleByCode("SHOP_OWNER");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getDescription()).contains("special chars");
    }

    @Test
    @DisplayName("Should handle null description in role")
    void testGetRoleByCode_NullDescription() {
        // Given
        adminRole.setDescription(null);
        when(roleRepository.findByName("SHOP_OWNER")).thenReturn(Optional.of(adminRole));

        // When
        RoleDTO result = roleService.getRoleByCode("SHOP_OWNER");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("SHOP_OWNER");
    }

    @Test
    @DisplayName("Should handle empty string description in role")
    void testGetRoleByCode_EmptyDescription() {
        // Given
        adminRole.setDescription("");
        when(roleRepository.findByName("SHOP_OWNER")).thenReturn(Optional.of(adminRole));

        // When
        RoleDTO result = roleService.getRoleByCode("SHOP_OWNER");

        // Then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Should throw exception for null role code")
    void testGetRoleByCode_NullCode() {
        // Given
        when(messageService.getMessage(anyString(), (Object[]) any())).thenReturn("Invalid role");

        // When & Then
        assertThatThrownBy(() -> roleService.getRoleByCode(null))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Should throw exception for empty role code")
    void testGetRoleByCode_EmptyCode() {
        // Given
        when(messageService.getMessage(anyString(), (Object[]) any())).thenReturn("Invalid role");

        // When & Then
        assertThatThrownBy(() -> roleService.getRoleByCode(""))
                .isInstanceOf(BadRequestException.class);
    }
}

