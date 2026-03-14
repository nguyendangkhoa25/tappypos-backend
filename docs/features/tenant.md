# 🏢 Tenant Management Feature

## Overview

The Tenant Management feature handles multi-tenancy in the retail platform. It manages tenant registration, tenant-specific configurations, data isolation, and dynamic datasource management. Each tenant operates in a completely isolated environment with their own database and data.

**Feature Status**: ✅ Production Ready  
**Last Updated**: March 15, 2026  
**Coverage**: >95%

---

## 📋 Table of Contents

- [Feature Overview](#overview)
- [Multi-Tenancy Architecture](#multi-tenancy-architecture)
- [Entities & Models](#entities--models)
- [API Endpoints](#api-endpoints)
- [Service Layer](#service-layer)
- [Data Isolation](#data-isolation)
- [Coding Conventions](#coding-conventions)
- [Testing Strategy](#testing-strategy)
- [Error Handling](#error-handling)

---

## 🏗️ Multi-Tenancy Architecture

### Tenant Isolation Pattern

```
Master Database (System-wide)
├── users (admin/master users)
├── roles (system roles)
├── tenants (tenant registry)
├── features (feature definitions)
└── feature_assignments (tenant features)

Tenant 1 Database (kim-ngan-phat)
├── customers
├── products
├── orders
└── ... (tenant-specific data)

Tenant 2 Database (other-shop)
├── customers
├── products
├── orders
└── ... (tenant-specific data)
```

### Request Routing

```
HTTP Request with X-Tenant-Id Header
    ↓
TenantInterceptor extracts header
    ↓
TenantContext.setCurrentTenant(tenantId)
    ↓
Database routing to tenant-specific DB
    ↓
All queries use tenant's database
    ↓
Response sent to client
    ↓
TenantContext.clear()
```

---

## 🏗️ Entities & Models

### Tenant Entity (Master DB)

**Location**: `com.knp.model.entity.Tenant`

```java
@Entity
@Table(name = "tenants")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Tenant extends BaseEntity {
    
    @NotBlank(message = "Tenant ID is required")
    @Column(nullable = false, unique = true, length = 100)
    private String tenantId;
    
    @NotBlank(message = "Tenant name is required")
    @Column(nullable = false, length = 255)
    private String name;
    
    @Column(name = "database_name", nullable = false)
    private String databaseName;
    
    @Column(name = "database_host", nullable = false)
    private String databaseHost = "localhost";
    
    @Column(name = "database_port")
    private Integer databasePort = 3306;
    
    @Column(name = "database_username")
    private String databaseUsername;
    
    @Column(name = "database_password")
    private String databasePassword;
    
    @Column(name = "subscription_plan")
    private String subscriptionPlan; // BASIC, PROFESSIONAL, ENTERPRISE
    
    @Column(name = "expiration_date")
    private LocalDate expirationDate;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "max_users")
    private Integer maxUsers = 10;
    
    @Column(name = "max_customers")
    private Integer maxCustomers;
    
    @Column(name = "config", columnDefinition = "JSON")
    private String config; // JSON configuration
    
    @ManyToMany
    @JoinTable(
        name = "tenant_features",
        joinColumns = @JoinColumn(name = "tenant_id"),
        inverseJoinColumns = @JoinColumn(name = "feature_id")
    )
    private Set<Feature> features = new HashSet<>();
    
    @Column(name = "created_by")
    private String createdBy;
}
```

### Request DTOs

#### CreateTenantRequest

```java
@Data @Builder
@Schema(description = "Request for creating a new tenant")
public class CreateTenantRequest {
    
    @NotBlank(message = "Tenant ID is required")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Tenant ID must be lowercase with hyphens only")
    @Schema(description = "Unique tenant identifier", example = "kim-ngan-phat")
    private String tenantId;
    
    @NotBlank(message = "Tenant name is required")
    @Schema(description = "Display name for tenant", example = "Kim Ngân Phát Jewelry")
    private String name;
    
    @Schema(description = "Subscription plan", example = "PROFESSIONAL")
    private String subscriptionPlan = "BASIC";
    
    @FutureOrPresent(message = "Expiration date must be in future")
    @Schema(description = "Subscription expiration date", example = "2027-03-15")
    private LocalDate expirationDate;
    
    @Schema(description = "Maximum users allowed", example = "10")
    private Integer maxUsers = 10;
    
    @Schema(description = "Maximum customers allowed", example = "1000")
    private Integer maxCustomers;
    
    @Schema(description = "Feature names to assign")
    private Set<String> features;
    
    @NotBlank(message = "Database host is required")
    @Schema(description = "MySQL host for tenant database", example = "localhost")
    private String databaseHost = "localhost";
    
    @Schema(description = "MySQL port", example = "3306")
    private Integer databasePort = 3306;
}
```

### Response DTO

#### TenantDTO

```java
@Data @Builder
@Schema(description = "Tenant information response")
public class TenantDTO {
    
    @Schema(description = "Tenant ID", example = "1")
    private Long id;
    
    @Schema(description = "Unique tenant identifier", example = "kim-ngan-phat")
    private String tenantId;
    
    @Schema(description = "Display name", example = "Kim Ngân Phát Jewelry")
    private String name;
    
    @Schema(description = "Subscription plan", example = "PROFESSIONAL")
    private String subscriptionPlan;
    
    @Schema(description = "Expiration date")
    private LocalDate expirationDate;
    
    @Schema(description = "Is subscription expired")
    private Boolean isExpired;
    
    @Schema(description = "Active status")
    private Boolean isActive;
    
    @Schema(description = "Maximum users allowed")
    private Integer maxUsers;
    
    @Schema(description = "Maximum customers allowed")
    private Integer maxCustomers;
    
    @Schema(description = "Assigned features")
    private Set<FeatureDTO> features;
    
    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;
}
```

---

## 🔌 API Endpoints

### Base URL (Master Database Only)
```
http://localhost:8080/api/v1/tenants
```

### Endpoints

#### 1. Create Tenant

```http
POST /api/v1/tenants
Content-Type: application/json
Authorization: Bearer {MASTER_JWT_TOKEN}

{
  "tenantId": "kim-ngan-phat",
  "name": "Kim Ngân Phát Jewelry",
  "subscriptionPlan": "PROFESSIONAL",
  "expirationDate": "2027-03-15",
  "maxUsers": 10,
  "maxCustomers": 1000,
  "databaseHost": "localhost",
  "databasePort": 3306,
  "features": ["DASHBOARD", "CUSTOMER", "ORDER", "INVENTORY"]
}
```

**Response** (201 Created): Returns TenantDTO

#### 2. Get All Tenants

```http
GET /api/v1/tenants?page=0&pageSize=20
Authorization: Bearer {MASTER_JWT_TOKEN}
```

**Response** (200 OK): Returns Page<TenantDTO>

#### 3. Get Tenant by ID

```http
GET /api/v1/tenants/{id}
Authorization: Bearer {MASTER_JWT_TOKEN}
```

**Response** (200 OK): Returns TenantDTO

#### 4. Update Tenant

```http
PUT /api/v1/tenants/{id}
Content-Type: application/json
Authorization: Bearer {MASTER_JWT_TOKEN}

{
  "subscriptionPlan": "ENTERPRISE",
  "maxUsers": 20,
  "expirationDate": "2028-03-15"
}
```

**Response** (200 OK): Returns updated TenantDTO

#### 5. Delete Tenant

```http
DELETE /api/v1/tenants/{id}
Authorization: Bearer {MASTER_JWT_TOKEN}
```

**Response** (204 No Content)

#### 6. Activate Tenant

```http
POST /api/v1/tenants/{id}/activate
Authorization: Bearer {MASTER_JWT_TOKEN}
```

**Response** (200 OK): Returns TenantDTO with isActive=true

#### 7. Deactivate Tenant

```http
POST /api/v1/tenants/{id}/deactivate
Authorization: Bearer {MASTER_JWT_TOKEN}
```

**Response** (200 OK): Returns TenantDTO with isActive=false

#### 8. Assign Feature

```http
POST /api/v1/tenants/{id}/features/{featureName}
Authorization: Bearer {MASTER_JWT_TOKEN}
```

**Response** (200 OK): Returns updated TenantDTO

#### 9. Remove Feature

```http
DELETE /api/v1/tenants/{id}/features/{featureName}
Authorization: Bearer {MASTER_JWT_TOKEN}
```

**Response** (200 OK): Returns updated TenantDTO

---

## 🔧 Service Layer

### TenantService Interface

```java
public interface TenantService {
    TenantDTO createTenant(CreateTenantRequest request);
    Page<TenantDTO> getAllTenants(Pageable pageable);
    TenantDTO getTenantById(Long id);
    TenantDTO getTenantByTenantId(String tenantId);
    TenantDTO updateTenant(Long id, UpdateTenantRequest request);
    void deleteTenant(Long id);
    TenantDTO activateTenant(Long id);
    TenantDTO deactivateTenant(Long id);
    void validateTenantNotExpired(String tenantId);
    void createTenantDatasource(String tenantId, String dbName);
    void removeTenantDatasource(String tenantId);
    void reloadAllDatasource();
}
```

### Key Implementation Patterns

#### 1. Tenant Creation Flow

```java
@Transactional
public TenantDTO createTenant(CreateTenantRequest request) {
    log.info("Creating tenant: {}", request.getTenantId());
    
    // Validate tenant ID
    if (tenantRepository.existsByTenantId(request.getTenantId())) {
        throw new DuplicateResourceException("Tenant already exists");
    }
    
    // Create entity
    Tenant tenant = Tenant.builder()
        .tenantId(request.getTenantId())
        .name(request.getName())
        .databaseName(generateDatabaseName(request.getTenantId()))
        .subscriptionPlan(request.getSubscriptionPlan())
        .expirationDate(request.getExpirationDate())
        .databaseHost(request.getDatabaseHost())
        .databasePort(request.getDatabasePort())
        .maxUsers(request.getMaxUsers())
        .maxCustomers(request.getMaxCustomers())
        .createdBy(authContext.getCurrentUsername())
        .build();
    
    Tenant savedTenant = tenantRepository.save(tenant);
    
    // Create datasource asynchronously
    createTenantDatasource(tenant.getTenantId(), tenant.getDatabaseName());
    
    // Assign features
    if (request.getFeatures() != null) {
        request.getFeatures().forEach(featureName -> {
            Feature feature = featureRepository.findByName(featureName)
                .orElseThrow(() -> new ResourceNotFoundException("Feature not found"));
            savedTenant.getFeatures().add(feature);
        });
    }
    
    return mapToDTO(tenantRepository.save(savedTenant));
}
```

#### 2. Tenant Validation

```java
@Transactional
public void validateTenantNotExpired(String tenantId) {
    Tenant tenant = getTenantByTenantId(tenantId);
    
    if (tenant.getExpirationDate().isBefore(LocalDate.now())) {
        throw new TenantExpiredException("Tenant subscription has expired");
    }
    
    if (!tenant.getIsActive()) {
        throw new TenantExpiredException("Tenant is not active");
    }
}
```

#### 3. Datasource Management

```java
@Transactional
public void createTenantDatasource(String tenantId, String dbName) {
    try {
        log.info("Creating datasource for tenant: {}", tenantId);
        datasourceManager.addOrUpdateTenantDatasource(tenantId, dbName);
        datasourceManager.reloadAllTenantDatasource();
        log.info("Datasource created successfully for: {}", tenantId);
    } catch (Exception e) {
        log.error("Failed to create datasource for tenant: {}", tenantId, e);
        throw new RuntimeException("Failed to create datasource: " + e.getMessage());
    }
}
```

---

## 🔐 Data Isolation

### TenantContext Pattern

```java
public class TenantContext {
    private static final ThreadLocal<String> currentTenant = new ThreadLocal<>();
    
    public static void setCurrentTenant(String tenantId) {
        currentTenant.set(tenantId);
    }
    
    public static String getCurrentTenant() {
        return currentTenant.get();
    }
    
    public static void clear() {
        currentTenant.remove();
    }
}
```

### Tenant Interceptor

```java
@Component
@RequiredArgsConstructor
public class TenantInterceptor implements HandlerInterceptor {
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String tenantId = request.getHeader("X-Tenant-Id");
        
        if (tenantId != null && !tenantId.isEmpty()) {
            TenantContext.setCurrentTenant(tenantId);
            // Validate tenant not expired
            tenantService.validateTenantNotExpired(tenantId);
        }
        
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        TenantContext.clear();
    }
}
```

### Using Tenant Context in Queries

```java
// ✅ GOOD - Automatic routing via RoutingDataSource
@Transactional
public Page<Customer> getCustomers(Pageable pageable) {
    // TenantContext.getCurrentTenant() automatically used by RoutingDataSource
    // Query routes to tenant-specific database
    return customerRepository.findAll(pageable);
}

// All data is isolated per tenant automatically
```

---

## 📐 Coding Conventions

### 1. Master-Only Endpoint Pattern

```java
@PostMapping("/{id}/activate")
@MasterDatabaseOnly  // Enforces MASTER_TENANT role
public ResponseEntity<ApiResponse<TenantDTO>> activateTenant(@PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.success(
        tenantService.activateTenant(id), 
        "Tenant activated"
    ));
}
```

### 2. Tenant ID Validation

```java
// ✅ GOOD - Validate format
if (!tenantId.matches("^[a-z0-9-]+$")) {
    throw new BadRequestException("Invalid tenant ID format");
}

// ✅ GOOD - Check uniqueness
if (tenantRepository.existsByTenantId(tenantId)) {
    throw new DuplicateResourceException("Tenant already exists");
}
```

### 3. Tenant Context Usage

```java
// Request must include X-Tenant-Id header
headers.put("X-Tenant-Id", "kim-ngan-phat");

// Or in code
TenantContext.setCurrentTenant("kim-ngan-phat");

// All queries automatically use tenant database
```

### 4. Expiration Checking

```java
// ✅ GOOD - Always validate before operations
public void validateTenantNotExpired(String tenantId) {
    Tenant tenant = getTenantByTenantId(tenantId);
    
    if (tenant.getExpirationDate().isBefore(LocalDate.now())) {
        throw new TenantExpiredException("Subscription expired");
    }
}
```

---

## 🧪 Testing Strategy

### Test Categories

#### 1. Tenant CRUD Tests
```java
@Test void testCreateTenant_Success() { }
@Test void testCreateTenant_DuplicateId() { }
@Test void testGetTenantById_Success() { }
@Test void testUpdateTenant_Success() { }
@Test void testDeleteTenant_Success() { }
```

#### 2. Datasource Tests
```java
@Test void testCreateTenantDatasource_Success() { }
@Test void testCreateTenantDatasource_ExceptionHandling() { }
@Test void testRemoveTenantDatasource_Success() { }
```

#### 3. Feature Assignment Tests
```java
@Test void testAssignFeatureToTenant_Success() { }
@Test void testRemoveFeatureFromTenant_Success() { }
```

#### 4. Validation Tests
```java
@Test void testValidateTenantNotExpired_Success() { }
@Test void testValidateTenantNotExpired_Expired() { }
```

### Coverage Targets
- Service methods: 95%+
- Datasource operations: 90%+
- Feature management: 90%+
- Validation logic: 100%

---

## 🛑 Error Handling

### Error Messages

**English** (`messages.properties`):
```properties
error.tenant.not.found=Tenant with ID {0} not found
error.tenant.duplicate.id=Tenant with ID {0} already exists
error.tenant.expired=Tenant subscription has expired
error.tenant.not.active=Tenant is not active
error.tenant.invalid.plan=Invalid subscription plan: {0}
error.tenant.datasource.creation.failed=Failed to create datasource for tenant: {0}
error.tenant.datasource.removal.failed=Failed to remove datasource for tenant: {0}
```

**Vietnamese** (`messages_vi.properties`):
```properties
error.tenant.not.found=Không tìm thấy tenant với ID {0}
error.tenant.duplicate.id=Tenant với ID {0} đã tồn tại
error.tenant.expired=Gói dịch vụ của tenant đã hết hạn
error.tenant.not.active=Tenant không hoạt động
error.tenant.invalid.plan=Gói dịch vụ không hợp lệ: {0}
error.tenant.datasource.creation.failed=Tạo datasource cho tenant {0} thất bại
error.tenant.datasource.removal.failed=Xóa datasource cho tenant {0} thất bại
```

---

## ✅ Best Practices

### 1. Always Set Tenant Context

```java
// ✅ GOOD - Set before operations
TenantContext.setCurrentTenant("kim-ngan-phat");
// ... perform operations ...
TenantContext.clear();

// ✅ GOOD - Use interceptor for automatic handling
// TenantInterceptor handles setCurrentTenant() and clear()
```

### 2. Validate Tenant Before Operations

```java
// ✅ GOOD - Validate early
tenantService.validateTenantNotExpired(tenantId);
// Continue with operations

// ❌ BAD - No validation
customerRepository.findAll(); // May fail if tenant expired
```

### 3. Master Database Only Operations

```java
// ✅ GOOD - Use annotation for master-only endpoints
@MasterDatabaseOnly
public ResponseEntity<ApiResponse<TenantDTO>> createTenant(...) { }

// ✅ GOOD - Enforced by aspect
// Checks: MASTER_TENANT role + isMasterUser=true
```

### 4. Database Name Generation

```java
// ✅ GOOD - Consistent naming
private String generateDatabaseName(String tenantId) {
    return "tenant_" + tenantId.replace("-", "_");
}
// Result: "tenant_kim_ngan_phat"
```

---

## 🔗 References

- **Controller**: `src/main/java/com/knp/controller/TenantController.java`
- **Service**: `src/main/java/com/knp/service/TenantServiceImpl.java`
- **Entity**: `src/main/java/com/knp/model/entity/Tenant.java`
- **Context**: `src/main/java/com/knp/multitenant/TenantContext.java`
- **Routing**: `src/main/java/com/knp/multitenant/RoutingDataSource.java`
- **Tests**: `src/test/java/com/knp/service/TenantServiceTest.java`

---

**Last Updated**: March 15, 2026  
**Version**: 1.0.0

