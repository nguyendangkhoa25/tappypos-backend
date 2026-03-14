# 👤 User Management Feature

## Overview

The User Management feature handles user accounts, authentication, authorization, role assignments, and permission management in the retail platform. It provides comprehensive APIs for managing user credentials, roles, and system access.

**Feature Status**: ✅ Production Ready  
**Last Updated**: March 15, 2026  
**Coverage**: >95%

---

## 📋 Table of Contents

- [Feature Overview](#overview)
- [Entities & Models](#entities--models)
- [API Endpoints](#api-endpoints)
- [Service Layer](#service-layer)
- [Authentication Flow](#authentication-flow)
- [Coding Conventions](#coding-conventions)
- [Testing Strategy](#testing-strategy)
- [Error Handling](#error-handling)
- [Best Practices](#best-practices)

---

## 🏗️ Entities & Models

### User Entity

**Location**: `com.knp.model.entity.User`

```java
@Entity
@Table(name = "users")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class User extends BaseEntity {
    
    @NotBlank(message = "Username is required")
    @Column(nullable = false, unique = true, length = 100)
    private String username;
    
    @NotBlank(message = "Email is required")
    @Column(nullable = false, unique = true, length = 100)
    private String email;
    
    @NotBlank(message = "Password is required")
    @Column(nullable = false)
    private String password;
    
    @Column(name = "full_name", length = 255)
    private String fullName;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "is_locked")
    private Boolean isLocked = false;
    
    @Column(name = "change_password_on_login")
    private Boolean changePasswordOnLogin = false;
    
    @Column(name = "failed_login_attempts")
    private Integer failedLoginAttempts = 0;
    
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;
    
    @ManyToMany
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();
}
```

### Request DTOs

#### CreateUserRequest

```java
@Data @Builder
@Schema(description = "Request for creating a new user")
public class CreateUserRequest {
    
    @NotBlank(message = "Username is required")
    @Schema(description = "Unique username", example = "john.doe")
    private String username;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Schema(description = "User email address", example = "john@example.com")
    private String email;
    
    @NotBlank(message = "Password is required")
    @Schema(description = "User password (min 8 chars)", example = "SecurePass123!")
    private String password;
    
    @Schema(description = "User full name", example = "John Doe")
    private String fullName;
    
    @NotEmpty(message = "At least one role is required")
    @Schema(description = "List of role names to assign", example = "[\"MANAGER\", \"SHOP_OWNER\"]")
    private Set<String> roleNames;
}
```

### Response DTO

#### UserDTO

```java
@Data @Builder
@Schema(description = "User information response (password excluded)")
public class UserDTO {
    
    @Schema(description = "User ID", example = "1")
    private Long id;
    
    @Schema(description = "Username", example = "john.doe")
    private String username;
    
    @Schema(description = "Email address", example = "john@example.com")
    private String email;
    
    @Schema(description = "Full name", example = "John Doe")
    private String fullName;
    
    @Schema(description = "Whether user is active")
    private Boolean isActive;
    
    @Schema(description = "Whether user is locked")
    private Boolean isLocked;
    
    @Schema(description = "Assigned roles")
    private Set<RoleDTO> roles;
    
    @Schema(description = "Last login timestamp")
    private LocalDateTime lastLoginAt;
    
    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;
}
```

---

## 🔌 API Endpoints

### Base URL
```
http://localhost:8080/api/v1/users
```

### Endpoints

#### 1. Create User

```http
POST /api/v1/users
Content-Type: application/json
Authorization: Bearer {JWT_TOKEN}

{
  "username": "john.doe",
  "email": "john@example.com",
  "password": "SecurePass123!",
  "fullName": "John Doe",
  "roleNames": ["MANAGER", "SHOP_OWNER"]
}
```

**Response** (201 Created): Returns UserDTO

#### 2. Get All Users

```http
GET /api/v1/users?page=0&pageSize=20&sortBy=username&sortDirection=ASC
Authorization: Bearer {JWT_TOKEN}
```

**Response** (200 OK): Returns Page<UserDTO>

#### 3. Get User by ID

```http
GET /api/v1/users/{id}
Authorization: Bearer {JWT_TOKEN}
```

**Response** (200 OK): Returns UserDTO

#### 4. Update User

```http
PUT /api/v1/users/{id}
Content-Type: application/json
Authorization: Bearer {JWT_TOKEN}

{
  "username": "john.doe.updated",
  "email": "john.updated@example.com",
  "fullName": "John Doe Updated",
  "roleNames": ["MANAGER"]
}
```

**Response** (200 OK): Returns updated UserDTO

#### 5. Reset Password

```http
POST /api/v1/users/{id}/reset-password
Content-Type: application/json
Authorization: Bearer {JWT_TOKEN}

{}
```

**Response** (200 OK):
```json
{
  "success": true,
  "message": "Password reset successfully",
  "data": {
    "temporaryPassword": "TempPass123456"
  }
}
```

#### 6. Add Role to User

```http
POST /api/v1/users/{id}/roles/{roleName}
Authorization: Bearer {JWT_TOKEN}
```

**Response** (200 OK): Returns updated UserDTO

#### 7. Remove Role from User

```http
DELETE /api/v1/users/{id}/roles/{roleName}
Authorization: Bearer {JWT_TOKEN}
```

**Response** (200 OK): Returns updated UserDTO

#### 8. Lock User

```http
POST /api/v1/users/{id}/lock
Authorization: Bearer {JWT_TOKEN}
```

**Response** (200 OK): Returns UserDTO with isLocked=true

#### 9. Unlock User

```http
POST /api/v1/users/{id}/unlock
Authorization: Bearer {JWT_TOKEN}
```

**Response** (200 OK): Returns UserDTO with isLocked=false

#### 10. Delete User (Soft Delete)

```http
DELETE /api/v1/users/{id}
Authorization: Bearer {JWT_TOKEN}
```

**Response** (204 No Content)

---

## 🔐 Service Layer

### UserService Interface

```java
public interface UserService {
    UserDTO createUser(CreateUserRequest request);
    Page<UserDTO> getAllUsers(String search, String sortBy, String sortDirection, Pageable pageable);
    UserDTO getUserById(Long id);
    UserDTO updateUser(Long id, CreateUserRequest request);
    void deleteUser(Long id);
    UserDTO resetUserPassword(Long id);
    UserDTO changePassword(Long id, String oldPassword, String newPassword);
    UserDTO changePasswordOnFirstLogin(Long id, String newPassword);
    UserDTO addRoleToUser(Long userId, String roleName);
    UserDTO removeRoleFromUser(Long userId, String roleName);
    UserDTO lockUser(Long id);
    UserDTO unlockUser(Long id);
    UserDTO disableUser(Long id);
    UserDTO enableUser(Long id);
}
```

### Key Implementation Patterns

#### 1. Password Management
```java
@Transactional
public UserDTO resetUserPassword(Long id) {
    User user = findById(id);
    String temporaryPassword = generateSecurePassword();
    user.setPassword(passwordEncoder.encode(temporaryPassword));
    user.setChangePasswordOnLogin(true);
    return mapToDTO(userRepository.save(user));
}
```

#### 2. Role Management
```java
@Transactional
public UserDTO addRoleToUser(Long userId, String roleName) {
    if (!RoleEnum.isValidRole(roleName)) {
        throw new BadRequestException("Invalid role: " + roleName);
    }
    
    User user = findById(userId);
    Role role = roleRepository.findByName(roleName)
        .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
    
    user.getRoles().add(role);
    return mapToDTO(userRepository.save(user));
}
```

#### 3. User Lock/Unlock
```java
@Transactional
public UserDTO lockUser(Long id) {
    User user = findById(id);
    user.setIsLocked(true);
    user.setFailedLoginAttempts(0);
    return mapToDTO(userRepository.save(user));
}
```

---

## 🔐 Authentication Flow

### JWT Token Generation

```
Login Request (username/password)
    ↓
Validate credentials
    ↓
Generate JWT Token with claims:
  - username
  - roles: [MANAGER, SHOP_OWNER]
  - features: [DASHBOARD, ORDER, CUSTOMER]
  - isMasterUser: true/false
    ↓
Return Access Token + Refresh Token
```

### Token Claim Structure

```json
{
  "username": "john.doe",
  "roles": ["MANAGER", "SHOP_OWNER"],
  "features": ["DASHBOARD", "ORDER", "CUSTOMER"],
  "isMasterUser": false,
  "exp": 1710610800,
  "iat": 1710524400
}
```

---

## 📐 Coding Conventions

### 1. User Validation Pattern

```java
// ✅ GOOD - Comprehensive validation
if (!RoleEnum.isValidRole(roleName)) {
    String msg = messageService.getMessage("error.role.invalid", roleName);
    throw new BadRequestException(msg);
}

if (userRepository.existsByUsername(username)) {
    String msg = messageService.getMessage("error.user.duplicate.username", username);
    throw new DuplicateResourceException(msg);
}
```

### 2. Password Handling

```java
// ✅ GOOD - Always encode passwords
user.setPassword(passwordEncoder.encode(request.getPassword()));

// ✅ GOOD - Verify with encoder
if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
    throw new InvalidRequestException("Incorrect password");
}

// ❌ BAD - Store plain text
user.setPassword(request.getPassword());
```

### 3. Role Assignment

```java
// ✅ GOOD - Clear role management
user.getRoles().add(roleEntity);
userRepository.save(user); // Transactional boundary

// ✅ GOOD - Check role exists
Role role = roleRepository.findByName(roleName)
    .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
```

### 4. User Lock Management

```java
// ✅ GOOD - Track failed attempts
if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
    user.setIsLocked(true);
    userRepository.save(user);
}

// ✅ GOOD - Reset on successful login
user.setFailedLoginAttempts(0);
user.setLastLoginAt(LocalDateTime.now());
```

---

## 🧪 Testing Strategy

### Test Categories

#### 1. User CRUD Tests
```java
@Test void testCreateUser_Success() { }
@Test void testCreateUser_DuplicateUsername() { }
@Test void testGetUserById_Success() { }
@Test void testGetUserById_NotFound() { }
@Test void testUpdateUser_Success() { }
@Test void testDeleteUser_Success() { }
```

#### 2. Password Management Tests
```java
@Test void testResetUserPassword_Success() { }
@Test void testChangePassword_Success() { }
@Test void testChangePassword_WrongOldPassword() { }
@Test void testChangePasswordOnFirstLogin_Success() { }
```

#### 3. Role Management Tests
```java
@Test void testAddRoleToUser_Success() { }
@Test void testAddRoleToUser_InvalidRole() { }
@Test void testRemoveRoleFromUser_Success() { }
@Test void testRemoveRoleFromUser_RoleNotFound() { }
```

#### 4. User Status Tests
```java
@Test void testLockUser_Success() { }
@Test void testUnlockUser_Success() { }
@Test void testDisableUser_Success() { }
@Test void testEnableUser_Success() { }
```

### Coverage Targets
- Service methods: 95%+
- Password operations: 100%
- Role management: 95%+
- User status changes: 90%+

---

## 🛑 Error Handling

### Error Messages

**English** (`messages.properties`):
```properties
error.user.not.found=User with ID {0} not found
error.user.duplicate.username=Username {0} is already taken
error.user.invalid.password=Password must be at least 8 characters
error.user.password.mismatch=Current password is incorrect
error.user.role.invalid=Role {0} is not a valid system role
error.user.locked=User account is locked
error.user.disabled=User account is disabled
```

**Vietnamese** (`messages_vi.properties`):
```properties
error.user.not.found=Không tìm thấy người dùng với ID {0}
error.user.duplicate.username=Tên tài khoản {0} đã được sử dụng
error.user.invalid.password=Mật khẩu phải có ít nhất 8 ký tự
error.user.password.mismatch=Mật khẩu hiện tại không chính xác
error.user.role.invalid=Vai trò {0} không phải vai trò hợp lệ
error.user.locked=Tài khoản người dùng bị khóa
error.user.disabled=Tài khoản người dùng bị vô hiệu hóa
```

---

## ✅ Best Practices

### 1. Password Security
```java
// ✅ GOOD - Use BCrypt encoder
private final PasswordEncoder passwordEncoder;
user.setPassword(passwordEncoder.encode(password));

// ✅ GOOD - Validate password strength
if (password.length() < 8) {
    throw new BadRequestException("Password too short");
}

// ❌ BAD - Plain text passwords
user.setPassword(password);
```

### 2. Role Validation
```java
// ✅ GOOD - Validate role exists
if (!RoleEnum.isValidRole(roleName)) {
    throw new BadRequestException("Invalid role");
}

Role role = roleRepository.findByName(roleName)
    .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
```

### 3. Account Lockout
```java
// ✅ GOOD - Lock after failed attempts
if (user.getFailedLoginAttempts() >= 5) {
    user.setIsLocked(true);
}

// ✅ GOOD - Reset on successful login
user.setFailedLoginAttempts(0);
user.setLastLoginAt(LocalDateTime.now());
```

### 4. Soft Delete
```java
// ✅ GOOD - Mark as deleted
user.setDeleted(true);
user.setDeletedAt(LocalDateTime.now());
userRepository.save(user);

// Always query active users
userRepository.findByIdActive(id);
```

---

## 🔗 References

- **Controller**: `src/main/java/com/knp/controller/UserController.java`
- **Service**: `src/main/java/com/knp/service/UserServiceImpl.java`
- **Entity**: `src/main/java/com/knp/model/entity/User.java`
- **Tests**: `src/test/java/com/knp/service/UserServiceTest.java`
- **Messages**: `src/main/resources/i18n/messages.properties`

---

**Last Updated**: March 15, 2026  
**Version**: 1.0.0

