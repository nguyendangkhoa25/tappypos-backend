package com.tappy.pos.service.auth;

import com.tappy.pos.exception.ForbiddenException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.exception.UnauthorizedException;
import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.model.dto.auth.ProfileRequest;
import com.tappy.pos.model.dto.auth.UserProfile;
import com.tappy.pos.model.entity.auth.Role;
import com.tappy.pos.model.entity.auth.User;
import com.tappy.pos.repository.auth.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.tappy.pos.service.MessageService;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProfileService Unit Tests")
class ProfileServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private MessageService messageService;

    @InjectMocks
    private ProfileServiceImpl profileService;

    private User user;

    @BeforeEach
    void setUp() {
        Role role = Role.builder()
                .name("SHOP_OWNER")
                .description("Shop Owner")
                .build();
        role.setId(1L);

        user = User.builder()
                .username("testuser")
                .password("hashedPassword")
                .email("test@example.com")
                .fullName("John Doe")
                .active(true)
                .colorPreference("dark")
                .roles(Set.of(role))
                .build();
        user.setId(1L);
        user.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should get user profile successfully")
    void testGetUserDetail_Success() {
        // Given
        when(userRepository.findByUsernameActive("testuser")).thenReturn(Optional.of(user));

        // When
        UserProfile result = profileService.getUserDetail("testuser");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getFullName()).isEqualTo("John Doe");
        verify(userRepository).findByUsernameActive("testuser");
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void testGetUserDetail_NotFound() {
        // Given
        when(userRepository.findByUsernameActive("nonexistent")).thenReturn(Optional.empty());
        when(messageService.getMessage(anyString(), anyString())).thenReturn("User not found");

        // When & Then
        assertThatThrownBy(() -> profileService.getUserDetail("nonexistent"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should update profile color preference successfully")
    void testUpdateProfileColor_Success() {
        // Given
        ProfileRequest request = ProfileRequest.builder()
                .username("testuser")
                .colorPreference("light")
                .build();

        when(userRepository.findByUsernameActive("testuser")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        UserProfile result = profileService.updateProfileColor("testuser", request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");
        verify(userRepository).findByUsernameActive("testuser");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when updating other user's color preference")
    void testUpdateProfileColor_Forbidden() {
        // Given
        ProfileRequest request = ProfileRequest.builder()
                .username("otheruser")
                .colorPreference("light")
                .build();

        when(messageService.getMessage(anyString())).thenReturn("Permission denied");

        // When & Then
        assertThatThrownBy(() -> profileService.updateProfileColor("testuser", request))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Should update profile avatar successfully")
    void testUpdateProfileAvatar_Success() {
        // Given
        ProfileRequest request = ProfileRequest.builder()
                .username("testuser")
                .avatar("new-avatar-url")
                .build();

        when(userRepository.findByUsernameActive("testuser")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        UserProfile result = profileService.updateProfileAvatar("testuser", request);

        // Then
        assertThat(result).isNotNull();
        verify(userRepository).findByUsernameActive("testuser");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should update password successfully")
    void testUpdateProfilePassword_Success() {
        // Given
        ProfileRequest request = ProfileRequest.builder()
                .username("testuser")
                .oldPassword("oldPassword")
                .newPassword("newPassword")
                .build();

        String originalPassword = user.getPassword(); // Capture original password before service call
        when(userRepository.findByUsernameActive("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPassword", originalPassword)).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("hashedNewPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        UserProfile result = profileService.updateProfilePassword("testuser", request);

        // Then
        assertThat(result).isNotNull();
        verify(passwordEncoder).matches("oldPassword", originalPassword);
        verify(passwordEncoder).encode("newPassword");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when old password is incorrect")
    void testUpdateProfilePassword_IncorrectOldPassword() {
        // Given
        ProfileRequest request = ProfileRequest.builder()
                .username("testuser")
                .oldPassword("wrongPassword")
                .newPassword("newPassword")
                .build();

        when(userRepository.findByUsernameActive("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", user.getPassword())).thenReturn(false);
        when(messageService.getMessage(anyString())).thenReturn("Old password is incorrect");

        // When & Then
        assertThatThrownBy(() -> profileService.updateProfilePassword("testuser", request))
                .isInstanceOf(UnauthorizedException.class);
    }

    // ==================== Additional Tests for Coverage ====================

    @Test
    @DisplayName("Should throw exception when updating avatar with forbidden user")
    void testUpdateProfileAvatar_Forbidden() {
        // Given
        ProfileRequest request = ProfileRequest.builder()
                .username("otheruser")
                .avatar("new-avatar-url")
                .build();

        when(messageService.getMessage(anyString())).thenReturn("Permission denied");

        // When & Then
        assertThatThrownBy(() -> profileService.updateProfileAvatar("testuser", request))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Should throw exception when avatar is empty")
    void testUpdateProfileAvatar_EmptyAvatar() {
        // Given
        ProfileRequest request = ProfileRequest.builder()
                .username("testuser")
                .avatar("")
                .build();

        when(userRepository.findByUsernameActive("testuser")).thenReturn(Optional.of(user));
        when(messageService.getMessage(anyString())).thenReturn("Avatar cannot be empty");

        // When & Then
        assertThatThrownBy(() -> profileService.updateProfileAvatar("testuser", request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Should throw exception when avatar is null")
    void testUpdateProfileAvatar_NullAvatar() {
        // Given
        ProfileRequest request = ProfileRequest.builder()
                .username("testuser")
                .avatar(null)
                .build();

        when(userRepository.findByUsernameActive("testuser")).thenReturn(Optional.of(user));
        when(messageService.getMessage(anyString())).thenReturn("Avatar cannot be empty");

        // When & Then
        assertThatThrownBy(() -> profileService.updateProfileAvatar("testuser", request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Should throw exception when avatar is whitespace only")
    void testUpdateProfileAvatar_WhitespaceAvatar() {
        // Given
        ProfileRequest request = ProfileRequest.builder()
                .username("testuser")
                .avatar("   ")
                .build();

        when(userRepository.findByUsernameActive("testuser")).thenReturn(Optional.of(user));
        when(messageService.getMessage(anyString())).thenReturn("Avatar cannot be empty");

        // When & Then
        assertThatThrownBy(() -> profileService.updateProfileAvatar("testuser", request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Should throw exception when updating avatar for user not found")
    void testUpdateProfileAvatar_UserNotFound() {
        // Given
        ProfileRequest request = ProfileRequest.builder()
                .username("testuser")
                .avatar("new-avatar-url")
                .build();

        when(userRepository.findByUsernameActive("testuser")).thenReturn(Optional.empty());
        when(messageService.getMessage(anyString(), anyString())).thenReturn("User not found");

        // When & Then
        assertThatThrownBy(() -> profileService.updateProfileAvatar("testuser", request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw exception when updating color preference for user not found")
    void testUpdateProfileColor_UserNotFound() {
        // Given
        ProfileRequest request = ProfileRequest.builder()
                .username("testuser")
                .colorPreference("light")
                .build();

        when(userRepository.findByUsernameActive("testuser")).thenReturn(Optional.empty());
        when(messageService.getMessage(anyString(), anyString())).thenReturn("User not found");

        // When & Then
        assertThatThrownBy(() -> profileService.updateProfileColor("testuser", request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw exception when password too short")
    void testUpdateProfilePassword_PasswordTooShort() {
        // Given
        ProfileRequest request = ProfileRequest.builder()
                .username("testuser")
                .oldPassword("oldPassword")
                .newPassword("short")
                .build();

        when(userRepository.findByUsernameActive("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPassword", user.getPassword())).thenReturn(true);
        when(messageService.getMessage(anyString())).thenReturn("Password too short");

        // When & Then
        assertThatThrownBy(() -> profileService.updateProfilePassword("testuser", request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Should throw exception when new password is same as old")
    void testUpdateProfilePassword_SameAsOld() {
        // Given
        ProfileRequest request = ProfileRequest.builder()
                .username("testuser")
                .oldPassword("samePassword")
                .newPassword("samePassword")
                .build();

        when(userRepository.findByUsernameActive("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("samePassword", user.getPassword())).thenReturn(true);
        when(messageService.getMessage(anyString())).thenReturn("Password cannot be same");

        // When & Then
        assertThatThrownBy(() -> profileService.updateProfilePassword("testuser", request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Should throw exception when old password is empty")
    void testUpdateProfilePassword_EmptyOldPassword() {
        // Given
        ProfileRequest request = ProfileRequest.builder()
                .username("testuser")
                .oldPassword("")
                .newPassword("newPassword")
                .build();

        when(userRepository.findByUsernameActive("testuser")).thenReturn(Optional.of(user));
        when(messageService.getMessage(anyString())).thenReturn("Old password required");

        // When & Then
        assertThatThrownBy(() -> profileService.updateProfilePassword("testuser", request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Should throw exception when old password is null")
    void testUpdateProfilePassword_NullOldPassword() {
        // Given
        ProfileRequest request = ProfileRequest.builder()
                .username("testuser")
                .oldPassword(null)
                .newPassword("newPassword")
                .build();

        when(userRepository.findByUsernameActive("testuser")).thenReturn(Optional.of(user));
        when(messageService.getMessage(anyString())).thenReturn("Old password required");

        // When & Then
        assertThatThrownBy(() -> profileService.updateProfilePassword("testuser", request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Should throw exception when new password is empty")
    void testUpdateProfilePassword_EmptyNewPassword() {
        // Given
        ProfileRequest request = ProfileRequest.builder()
                .username("testuser")
                .oldPassword("oldPassword")
                .newPassword("")
                .build();

        when(userRepository.findByUsernameActive("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPassword", user.getPassword())).thenReturn(true);
        when(messageService.getMessage(anyString())).thenReturn("New password required");

        // When & Then
        assertThatThrownBy(() -> profileService.updateProfilePassword("testuser", request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Should throw exception when new password is null")
    void testUpdateProfilePassword_NullNewPassword() {
        // Given
        ProfileRequest request = ProfileRequest.builder()
                .username("testuser")
                .oldPassword("oldPassword")
                .newPassword(null)
                .build();

        when(userRepository.findByUsernameActive("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPassword", user.getPassword())).thenReturn(true);
        when(messageService.getMessage(anyString())).thenReturn("New password required");

        // When & Then
        assertThatThrownBy(() -> profileService.updateProfilePassword("testuser", request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Should throw exception when updating password for forbidden user")
    void testUpdateProfilePassword_Forbidden() {
        // Given
        ProfileRequest request = ProfileRequest.builder()
                .username("otheruser")
                .oldPassword("oldPassword")
                .newPassword("newPassword")
                .build();

        when(messageService.getMessage(anyString())).thenReturn("Permission denied");

        // When & Then
        assertThatThrownBy(() -> profileService.updateProfilePassword("testuser", request))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Should throw exception when updating password for user not found")
    void testUpdateProfilePassword_UserNotFound() {
        // Given
        ProfileRequest request = ProfileRequest.builder()
                .username("testuser")
                .oldPassword("oldPassword")
                .newPassword("newPassword")
                .build();

        when(userRepository.findByUsernameActive("testuser")).thenReturn(Optional.empty());
        when(messageService.getMessage(anyString(), anyString())).thenReturn("User not found");

        // When & Then
        assertThatThrownBy(() -> profileService.updateProfilePassword("testuser", request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should get user detail when user has multiple roles")
    void testGetUserDetail_MultipleRoles() {
        // Given
        Role ownerRole = Role.builder()
                .name("SHOP_OWNER")
                .description("Shop Owner")
                .build();
        ownerRole.setId(1L);

        Role managerRole = Role.builder()
                .name("MANAGER")
                .description("Manager")
                .build();
        managerRole.setId(2L);

        user.setRoles(Set.of(ownerRole, managerRole));

        when(userRepository.findByUsernameActive("testuser")).thenReturn(Optional.of(user));

        // When
        UserProfile result = profileService.getUserDetail("testuser");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRoles()).hasSize(2);
        assertThat(result.getRoles()).contains("SHOP_OWNER", "MANAGER");
    }

    @Test
    @DisplayName("Should get user detail with all profile fields populated")
    void testGetUserDetail_AllFieldsPopulated() {
        // Given
        user.setLang("vi");
        user.setActive(true);
        user.setAccountNonLocked(true);
        user.setCredentialsNonExpired(true);
        user.setAccountNonExpired(true);
        user.setRequireAction(null);
        user.setNotes("Test user notes");

        when(userRepository.findByUsernameActive("testuser")).thenReturn(Optional.of(user));

        // When
        UserProfile result = profileService.getUserDetail("testuser");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getLang()).isEqualTo("vi");
        assertThat(result.getActive()).isTrue();
        assertThat(result.getAccountNonLocked()).isTrue();
        assertThat(result.getCredentialsNonExpired()).isTrue();
        assertThat(result.getAccountNonExpired()).isTrue();
        assertThat(result.getNotes()).isEqualTo("Test user notes");
    }

    @Test
    @DisplayName("Should update profile color and verify all fields in response")
    void testUpdateProfileColor_VerifyResponseFields() {
        // Given
        ProfileRequest request = ProfileRequest.builder()
                .username("testuser")
                .colorPreference("light")
                .build();

        // Setup user with colorPreference already set to "dark"
        when(userRepository.findByUsernameActive("testuser")).thenReturn(Optional.of(user));
        // After update, the user's colorPreference will be "light"
        user.setColorPreference("light");
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        UserProfile result = profileService.updateProfileColor("testuser", request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getFullName()).isEqualTo("John Doe");
        assertThat(result.getColorPreference()).isEqualTo("light");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should update password and clear requireAction flag")
    void testUpdateProfilePassword_ClearRequireAction() {
        // Given
        user.setRequireAction("FORCE_CHANGE_PASSWORD");
        
        ProfileRequest request = ProfileRequest.builder()
                .username("testuser")
                .oldPassword("oldPassword")
                .newPassword("newPassword123")
                .build();

        when(userRepository.findByUsernameActive("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPassword", user.getPassword())).thenReturn(true);
        when(passwordEncoder.encode("newPassword123")).thenReturn("hashedNewPassword123");
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        UserProfile result = profileService.updateProfilePassword("testuser", request);

        // Then
        assertThat(result).isNotNull();
        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode("newPassword123");
    }

    @Test
    @DisplayName("Should handle update profile avatar with very long URL")
    void testUpdateProfileAvatar_LongUrl() {
        // Given
        String longUrl = "https://example.com/" + "a".repeat(500);
        ProfileRequest request = ProfileRequest.builder()
                .username("testuser")
                .avatar(longUrl)
                .build();

        when(userRepository.findByUsernameActive("testuser")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        UserProfile result = profileService.updateProfileAvatar("testuser", request);

        // Then
        assertThat(result).isNotNull();
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should update password with exact minimum length")
    void testUpdateProfilePassword_MinimumLength() {
        // Given
        ProfileRequest request = ProfileRequest.builder()
                .username("testuser")
                .oldPassword("oldPassword")
                .newPassword("123456")  // Exactly 6 characters
                .build();

        when(userRepository.findByUsernameActive("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPassword", user.getPassword())).thenReturn(true);
        when(passwordEncoder.encode("123456")).thenReturn("hashedNewPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        UserProfile result = profileService.updateProfilePassword("testuser", request);

        // Then
        assertThat(result).isNotNull();
        verify(passwordEncoder).encode("123456");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should update avatar and preserve other user fields")
    void testUpdateProfileAvatar_PreserveOtherFields() {
        // Given
        user.setNotes("Important notes");
        user.setRequireAction("ACTION_FLAG");
        
        ProfileRequest request = ProfileRequest.builder()
                .username("testuser")
                .avatar("new-avatar.jpg")
                .build();

        when(userRepository.findByUsernameActive("testuser")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        UserProfile result = profileService.updateProfileAvatar("testuser", request);

        // Then
        assertThat(result).isNotNull();
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should update color preference with null value")
    void testUpdateProfileColor_WithNullPreference() {
        // Given
        ProfileRequest request = ProfileRequest.builder()
                .username("testuser")
                .colorPreference(null)
                .build();

        when(userRepository.findByUsernameActive("testuser")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        UserProfile result = profileService.updateProfileColor("testuser", request);

        // Then
        assertThat(result).isNotNull();
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should update color preference with empty roles")
    void testGetUserDetail_EmptyRoles() {
        // Given
        user.setRoles(Set.of());

        when(userRepository.findByUsernameActive("testuser")).thenReturn(Optional.of(user));

        // When
        UserProfile result = profileService.getUserDetail("testuser");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRoles()).isEmpty();
    }

    // ── updateProfileInfo ─────────────────────────────────────────────────────

    @Test
    @DisplayName("updateProfileInfo: updates fullName and email")
    void testUpdateProfileInfo_Success() {
        ProfileRequest request = ProfileRequest.builder()
                .username("testuser").fullName("New Name").email("new@example.com").build();

        when(userRepository.findByUsernameActive("testuser")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserProfile result = profileService.updateProfileInfo("testuser", request);

        assertThat(result).isNotNull();
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("updateProfileInfo: throws ForbiddenException when username mismatch")
    void testUpdateProfileInfo_Forbidden() {
        ProfileRequest request = ProfileRequest.builder()
                .username("otheruser").fullName("New Name").build();
        when(messageService.getMessage(anyString())).thenReturn("denied");

        assertThatThrownBy(() -> profileService.updateProfileInfo("testuser", request))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("updateProfileInfo: throws BadRequestException when email is invalid")
    void testUpdateProfileInfo_InvalidEmail() {
        ProfileRequest request = ProfileRequest.builder()
                .username("testuser").email("not-an-email").build();

        when(userRepository.findByUsernameActive("testuser")).thenReturn(Optional.of(user));
        when(messageService.getMessage(anyString())).thenReturn("error");

        assertThatThrownBy(() -> profileService.updateProfileInfo("testuser", request))
                .isInstanceOf(BadRequestException.class);
    }

    // ── updateProfileLang ─────────────────────────────────────────────────────

    @Test
    @DisplayName("updateProfileLang: updates language preference")
    void testUpdateProfileLang_Success() {
        ProfileRequest request = ProfileRequest.builder()
                .username("testuser").lang("vi").build();

        when(userRepository.findByUsernameActive("testuser")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserProfile result = profileService.updateProfileLang("testuser", request);

        assertThat(result).isNotNull();
        assertThat(user.getLang()).isEqualTo("vi");
    }

    @Test
    @DisplayName("updateProfileLang: throws ForbiddenException when username mismatch")
    void testUpdateProfileLang_Forbidden() {
        ProfileRequest request = ProfileRequest.builder().username("otheruser").lang("en").build();
        when(messageService.getMessage(anyString())).thenReturn("denied");

        assertThatThrownBy(() -> profileService.updateProfileLang("testuser", request))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("updateProfileLang: null lang — skips update, saves unchanged user")
    void testUpdateProfileLang_NullLang_SkipsUpdate() {
        ProfileRequest request = ProfileRequest.builder().username("testuser").lang(null).build();
        when(userRepository.findByUsernameActive("testuser")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserProfile result = profileService.updateProfileLang("testuser", request);

        assertThat(result).isNotNull();
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("updateProfileLang: blank lang — skips update")
    void testUpdateProfileLang_BlankLang_SkipsUpdate() {
        ProfileRequest request = ProfileRequest.builder().username("testuser").lang("  ").build();
        when(userRepository.findByUsernameActive("testuser")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserProfile result = profileService.updateProfileLang("testuser", request);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("updateProfileLang: user not found → ResourceNotFoundException")
    void testUpdateProfileLang_UserNotFound() {
        ProfileRequest request = ProfileRequest.builder().username("testuser").lang("en").build();
        when(userRepository.findByUsernameActive("testuser")).thenReturn(Optional.empty());
        when(messageService.getMessage(anyString(), anyString())).thenReturn("Not found");

        assertThatThrownBy(() -> profileService.updateProfileLang("testuser", request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── updateProfileInfo edge cases ──────────────────────────────────────────

    @Test
    @DisplayName("updateProfileInfo: null fullName — skips fullName update")
    void testUpdateProfileInfo_NullFullName_Skips() {
        ProfileRequest request = ProfileRequest.builder()
                .username("testuser").fullName(null).email(null).build();

        when(userRepository.findByUsernameActive("testuser")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserProfile result = profileService.updateProfileInfo("testuser", request);

        assertThat(result).isNotNull();
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("updateProfileInfo: empty email after trim — sets email to null")
    void testUpdateProfileInfo_EmptyEmail_SetsNull() {
        ProfileRequest request = ProfileRequest.builder()
                .username("testuser").email("   ").build();

        when(userRepository.findByUsernameActive("testuser")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        profileService.updateProfileInfo("testuser", request);

        verify(userRepository).save(argThat((User u) -> u.getEmail() == null));
    }

    @Test
    @DisplayName("updateProfileInfo: user not found → ResourceNotFoundException")
    void testUpdateProfileInfo_UserNotFound() {
        ProfileRequest request = ProfileRequest.builder().username("testuser").fullName("Name").build();
        when(userRepository.findByUsernameActive("testuser")).thenReturn(Optional.empty());
        when(messageService.getMessage(anyString(), anyString())).thenReturn("Not found");

        assertThatThrownBy(() -> profileService.updateProfileInfo("testuser", request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getPreferences / updatePreferences ────────────────────────────────────

    @Test
    @DisplayName("getPreferences: returns '{}' when user has null preferences")
    void testGetPreferences_NullPreferences_ReturnsEmpty() {
        user.setPreferences(null);
        when(userRepository.findByUsernameActive("testuser")).thenReturn(Optional.of(user));

        String result = profileService.getPreferences("testuser");

        assertThat(result).isEqualTo("{}");
    }

    @Test
    @DisplayName("getPreferences: returns stored preferences JSON")
    void testGetPreferences_ReturnsStoredValue() {
        user.setPreferences("{\"theme\":\"dark\"}");
        when(userRepository.findByUsernameActive("testuser")).thenReturn(Optional.of(user));

        String result = profileService.getPreferences("testuser");

        assertThat(result).isEqualTo("{\"theme\":\"dark\"}");
    }

    @Test
    @DisplayName("updatePreferences: saves preferences JSON to user")
    void testUpdatePreferences_SavesSuccessfully() {
        when(userRepository.findByUsernameActive("testuser")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        profileService.updatePreferences("testuser", "{\"theme\":\"light\"}");

        verify(userRepository).save(argThat((User u) -> "{\"theme\":\"light\"}".equals(u.getPreferences())));
    }

    @Test
    @DisplayName("updatePreferences: user not found → ResourceNotFoundException")
    void testUpdatePreferences_UserNotFound() {
        when(userRepository.findByUsernameActive("nobody")).thenReturn(Optional.empty());
        when(messageService.getMessage(anyString(), anyString())).thenReturn("Not found");

        assertThatThrownBy(() -> profileService.updatePreferences("nobody", "{}"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

