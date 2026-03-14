# 👥 Customer Management Feature

## Overview

The Customer Management feature handles all customer-related operations in the retail platform. It provides comprehensive APIs for creating, updating, searching, and managing customer information including contact details, preferences, and interaction history.

**Feature Status**: ✅ Production Ready  
**Last Updated**: March 15, 2026  
**Coverage**: >95%

---

## 📋 Table of Contents

- [Feature Overview](#overview)
- [Entities & Models](#entities--models)
- [API Endpoints](#api-endpoints)
- [Service Layer](#service-layer)
- [Data Flow](#data-flow)
- [Coding Conventions](#coding-conventions)
- [Testing Strategy](#testing-strategy)
- [Error Handling](#error-handling)
- [Best Practices](#best-practices)

---

## 🏗️ Entities & Models

### Customer Entity

**Location**: `com.knp.model.entity.Customer`

```java
@Entity
@Table(name = "customers")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Customer extends BaseEntity {
    
    @NotBlank(message = "Customer name is required")
    @Column(nullable = false)
    private String name;
    
    @NotBlank(message = "Phone number is required")
    @Column(nullable = false, unique = true)
    private String phone;
    
    @Email(message = "Email should be valid")
    private String email;
    
    @Column(length = 500)
    private String notes;
    
    @Column(name = "zalo_id")
    private String zaloId;
    
    @Column(name = "facebook_id")
    private String facebookId;
    
    @Column(name = "preferred_services", length = 500)
    private String preferredServices;
    
    @Column(name = "allergies_or_sensitivities", length = 500)
    private String allergiesOrSensitivities;
    
    @Column(name = "hair_type")
    private String hairType;
    
    @Column(name = "special_requests", length = 500)
    private String specialRequests;
}
```

### Request DTOs

#### CreateCustomerRequest

**Location**: `com.knp.model.dto.customer.CreateCustomerRequest`

```java
@Data @Builder
@Schema(description = "Request payload for creating a new customer")
public class CreateCustomerRequest {
    
    @NotBlank(message = "Customer name is required")
    @Schema(description = "Customer full name", example = "John Doe")
    private String name;
    
    @NotBlank(message = "Phone number is required")
    @Schema(description = "Customer phone number", example = "0987654321")
    private String phone;
    
    @Email(message = "Email should be valid")
    @Schema(description = "Customer email address", example = "john@example.com")
    private String email;
    
    @Schema(description = "Customer notes", example = "Regular customer")
    private String notes;
    
    @Schema(description = "Zalo ID for contact", example = "zalo123")
    private String zaloId;
    
    @Schema(description = "Facebook ID for contact", example = "fb123")
    private String facebookId;
    
    @Schema(description = "Preferred services", example = "Haircut, Styling")
    private String preferredServices;
    
    @Schema(description = "Allergies or sensitivities", example = "None")
    private String allergiesOrSensitivities;
    
    @Schema(description = "Hair type", example = "Curly")
    private String hairType;
    
    @Schema(description = "Special requests", example = "Extra long on top")
    private String specialRequests;
}
```

#### UpdateCustomerRequest

**Location**: `com.knp.model.dto.customer.UpdateCustomerRequest`

```java
@Data @Builder
@Schema(description = "Request payload for updating customer information")
public class UpdateCustomerRequest {
    
    @Schema(description = "Updated customer name")
    private String name;
    
    @Schema(description = "Updated phone number")
    private String phone;
    
    @Email
    @Schema(description = "Updated email address")
    private String email;
    
    @Schema(description = "Updated notes")
    private String notes;
    
    @Schema(description = "Updated Zalo ID")
    private String zaloId;
    
    @Schema(description = "Updated Facebook ID")
    private String facebookId;
    
    @Schema(description = "Updated preferred services")
    private String preferredServices;
    
    @Schema(description = "Updated allergies or sensitivities")
    private String allergiesOrSensitivities;
    
    @Schema(description = "Updated hair type")
    private String hairType;
    
    @Schema(description = "Updated special requests")
    private String specialRequests;
}
```

### Response DTO

#### CustomerDTO

**Location**: `com.knp.model.dto.customer.CustomerDTO`

```java
@Data @Builder
@Schema(description = "Customer information response")
public class CustomerDTO {
    
    @Schema(description = "Unique customer identifier", example = "1")
    private Long id;
    
    @Schema(description = "Customer full name", example = "John Doe")
    private String name;
    
    @Schema(description = "Customer phone number", example = "0987654321")
    private String phone;
    
    @Schema(description = "Customer email address", example = "john@example.com")
    private String email;
    
    @Schema(description = "Customer notes", example = "Regular customer")
    private String notes;
    
    @Schema(description = "Zalo ID", example = "zalo123")
    private String zaloId;
    
    @Schema(description = "Facebook ID", example = "fb123")
    private String facebookId;
    
    @Schema(description = "Preferred services", example = "Haircut, Styling")
    private String preferredServices;
    
    @Schema(description = "Allergies or sensitivities", example = "None")
    private String allergiesOrSensitivities;
    
    @Schema(description = "Hair type", example = "Curly")
    private String hairType;
    
    @Schema(description = "Special requests", example = "Extra long on top")
    private String specialRequests;
    
    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;
    
    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedAt;
}
```

---

## 🔌 API Endpoints

### Base URL
```
http://localhost:8080/api/v1/customers
```

### Endpoints

#### 1. Create Customer

```http
POST /api/v1/customers
Content-Type: application/json
Authorization: Bearer {JWT_TOKEN}

{
  "name": "John Doe",
  "phone": "0987654321",
  "email": "john@example.com",
  "notes": "Regular customer",
  "zaloId": "zalo123",
  "facebookId": "fb123",
  "preferredServices": "Haircut, Styling",
  "allergiesOrSensitivities": "None",
  "hairType": "Curly",
  "specialRequests": "Extra long on top"
}
```

**Response** (201 Created):
```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "John Doe",
    "phone": "0987654321",
    "email": "john@example.com",
    "notes": "Regular customer",
    "zaloId": "zalo123",
    "facebookId": "fb123",
    "preferredServices": "Haircut, Styling",
    "allergiesOrSensitivities": "None",
    "hairType": "Curly",
    "specialRequests": "Extra long on top",
    "createdAt": "2026-03-15T10:00:00"
  },
  "message": "Customer created successfully"
}
```

#### 2. Get All Customers

```http
GET /api/v1/customers?page=0&pageSize=20&search=John&sortBy=name&sortDirection=ASC
Authorization: Bearer {JWT_TOKEN}
```

**Query Parameters**:
- `page` (int, default=0): Page number for pagination
- `pageSize` (int, default=20): Number of records per page
- `search` (string, optional): Search keyword for filtering
- `sortBy` (string, default=createdAt): Field to sort by
- `sortDirection` (string, default=DESC): ASC or DESC

**Response** (200 OK):
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "name": "John Doe",
        "phone": "0987654321",
        "email": "john@example.com",
        "createdAt": "2026-03-15T10:00:00"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 20,
      "totalElements": 1,
      "totalPages": 1
    }
  },
  "message": "Customers retrieved successfully"
}
```

#### 3. Get Customer by ID

```http
GET /api/v1/customers/{id}
Authorization: Bearer {JWT_TOKEN}
```

**Response** (200 OK): Returns CustomerDTO

#### 4. Update Customer

```http
PUT /api/v1/customers/{id}
Content-Type: application/json
Authorization: Bearer {JWT_TOKEN}

{
  "name": "Jane Doe",
  "phone": "0987654322",
  "notes": "VIP customer"
}
```

**Response** (200 OK): Returns updated CustomerDTO

#### 5. Delete Customer

```http
DELETE /api/v1/customers/{id}
Authorization: Bearer {JWT_TOKEN}
```

**Response** (204 No Content)

#### 6. Get or Create Customer

```http
POST /api/v1/customers/get-or-create
Content-Type: application/json
Authorization: Bearer {JWT_TOKEN}

{
  "name": "Jane Smith",
  "phone": "0912345678",
  "email": "jane@example.com"
}
```

**Response** (200 OK): Returns existing or newly created CustomerDTO

#### 7. Get Customer by Phone

```http
GET /api/v1/customers/search/by-phone?phone=0987654321
Authorization: Bearer {JWT_TOKEN}
```

**Response** (200 OK): Returns CustomerDTO

#### 8. Get Customer by Email

```http
GET /api/v1/customers/search/by-email?email=john@example.com
Authorization: Bearer {JWT_TOKEN}
```

**Response** (200 OK): Returns CustomerDTO

#### 9. Get Customer Count

```http
GET /api/v1/customers/count
Authorization: Bearer {JWT_TOKEN}
```

**Response** (200 OK):
```json
{
  "success": true,
  "data": 42,
  "message": "Customer count retrieved"
}
```

---

## 🔧 Service Layer

### CustomerService Interface

**Location**: `com.knp.service.CustomerService`

```java
public interface CustomerService {
    CustomerDTO createCustomer(CreateCustomerRequest request);
    Page<CustomerDTO> getAllCustomers(String search, String sortBy, String sortDirection, Pageable pageable);
    CustomerDTO getCustomerById(Long id);
    CustomerDTO updateCustomer(Long id, UpdateCustomerRequest request);
    void deleteCustomer(Long id);
    CustomerDTO getCustomerByPhone(String phone);
    CustomerDTO getCustomerByEmail(String email);
    CustomerDTO getOrCreateCustomer(CreateCustomerRequest request);
    Long getCustomerCount();
}
```

### CustomerServiceImpl Implementation

**Location**: `com.knp.service.CustomerServiceImpl`

Key Implementation Points:

1. **Dependency Injection**:
```java
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CustomerServiceImpl implements CustomerService {
    
    private final CustomerRepository customerRepository;
    private final MessageService messageService;
    
    // Implementation...
}
```

2. **Error Handling Pattern**:
```java
public CustomerDTO getCustomerById(Long id) {
    log.info("Fetching customer: {}", id);
    Customer customer = customerRepository.findByIdActive(id)
        .orElseThrow(() -> {
            String errorMsg = messageService.getMessage("error.customer.not.found", id);
            return new ResourceNotFoundException(errorMsg);
        });
    return mapToDTO(customer);
}
```

3. **Soft Delete Pattern**:
- Customers are soft-deleted (marked with `deleted = true`)
- Use `findByIdActive()` for active customers only
- Use `findByPhone()` for phone search on active customers

---

## 📊 Data Flow

### Customer Creation Flow

```
POST /api/v1/customers
    ↓
CustomerController.createCustomer()
    ↓
CustomerService.createCustomer()
    ├─ Validate input (Spring Validation)
    ├─ Check for duplicates (optional)
    ├─ Create Customer entity
    ├─ Save to database
    └─ Map to DTO
    ↓
Return 201 Created with CustomerDTO
```

### Customer Search Flow

```
GET /api/v1/customers?search=John
    ↓
CustomerController.getAllCustomers()
    ↓
CustomerService.getAllCustomers()
    ├─ Build search specification
    ├─ Apply pagination
    ├─ Query repository
    ├─ Map to DTOs
    └─ Return Page<CustomerDTO>
    ↓
Return 200 OK with paginated results
```

### Get or Create Flow

```
POST /api/v1/customers/get-or-create
    ↓
CustomerController.getOrCreateCustomer()
    ↓
CustomerService.getOrCreateCustomer()
    ├─ If phone provided:
    │  ├─ Search by phone
    │  ├─ If found: Return existing customer
    │  └─ If not found: Create new customer
    └─ If no phone: Create new customer
    ↓
Return 200 OK with CustomerDTO
```

---

## 📐 Coding Conventions

### 1. Naming Conventions

| Element | Convention | Example |
|---------|-----------|---------|
| Class | PascalCase | `CustomerService`, `CreateCustomerRequest` |
| Method | camelCase | `getCustomerById()`, `createCustomer()` |
| Variable | camelCase | `customerId`, `customerName` |
| Constant | UPPER_SNAKE_CASE | `DEFAULT_PAGE_SIZE`, `MAX_SEARCH_TERM_LENGTH` |
| Package | lowercase | `com.knp.service.customer` |

### 2. Class Structure Pattern

```java
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CustomerServiceImpl implements CustomerService {
    
    // 1. Injected dependencies
    private final CustomerRepository customerRepository;
    private final MessageService messageService;
    
    // 2. Public methods (override interface)
    @Override
    public CustomerDTO createCustomer(CreateCustomerRequest request) {
        // Implementation
    }
    
    // 3. Private helper methods
    private CustomerDTO mapToDTO(Customer entity) {
        // Implementation
    }
}
```

### 3. Error Handling Convention

**✅ CORRECT - Using MessageService**:
```java
String errorMsg = messageService.getMessage("error.customer.not.found", id);
throw new ResourceNotFoundException(errorMsg);
```

**❌ INCORRECT - Hard-coded message**:
```java
throw new ResourceNotFoundException("Customer not found");
```

### 4. Logging Convention

```java
// INFO: Track important operations
log.info("Creating customer: {}", request.getName());

// DEBUG: Detailed trace for debugging
log.debug("Customer found: id={}, phone={}", id, phone);

// WARN: Suspicious but not critical
log.warn("Customer not found for ID: {}", id);

// ERROR: Failures and exceptions
log.error("Failed to create customer: {}", ex.getMessage(), ex);
```

### 5. Method Documentation

```java
/**
 * Creates a new customer with provided information.
 * 
 * @param request Customer creation request containing name, phone, and optional fields
 * @return Created CustomerDTO with assigned ID
 * @throws DuplicateResourceException if phone already exists in system
 * @throws BadRequestException if input validation fails
 */
@Override
public CustomerDTO createCustomer(CreateCustomerRequest request) {
    // Implementation
}
```

### 6. Transactional Boundary

```java
// Service-level transaction - handles all data modifications
@Transactional
public CustomerDTO updateCustomer(Long id, UpdateCustomerRequest request) {
    Customer customer = findById(id); // Read
    customer.setName(request.getName()); // Modify
    customerRepository.save(customer); // Write (auto-committed on method exit)
    return mapToDTO(customer);
}
```

### 7. Repository Pattern

```java
// Use repository methods consistently
customerRepository.findById(id)           // Optional<T>
customerRepository.findByIdActive(id)     // Optional<T> - active only
customerRepository.findByPhone(phone)     // Optional<T>
customerRepository.findAll(pageable)      // Page<T>
customerRepository.save(entity)           // T
customerRepository.delete(entity)         // void
```

### 8. DTO Mapping Convention

```java
// Service-level mapping
private CustomerDTO mapToDTO(Customer entity) {
    return CustomerDTO.builder()
        .id(entity.getId())
        .name(entity.getName())
        .phone(entity.getPhone())
        .email(entity.getEmail())
        .createdAt(entity.getCreatedAt())
        .build();
}

// Alternative: Use MapStruct if complex mapping
@Mapper
public interface CustomerMapper {
    CustomerDTO toDTO(Customer entity);
    Customer fromRequest(CreateCustomerRequest request);
}
```

### 9. Pagination Convention

```java
// Always use Spring Data Pageable
public Page<CustomerDTO> getAllCustomers(
        String search, 
        String sortBy, 
        String sortDirection,
        Pageable pageable) {
    
    // Build query with pagination
    Page<Customer> customers = customerRepository.findAll(pageable);
    
    // Map to DTOs
    return customers.map(this::mapToDTO);
}
```

### 10. Soft Delete Convention

```java
// Mark as deleted, don't remove from DB
public void deleteCustomer(Long id) {
    Customer customer = customerRepository.findByIdActive(id)
        .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
    
    customer.setDeleted(true);
    customer.setDeletedAt(LocalDateTime.now());
    customerRepository.save(customer);
}

// Always query active records only
customerRepository.findByIdActive(id);  // Returns only if deleted=false
customerRepository.countAllActive();    // Count only active
```

---

## 🧪 Testing Strategy

### Test File Structure

**Location**: `src/test/java/com/knp/service/CustomerServiceTest.java`

### Test Categories

#### 1. CRUD Operations
```java
@Test @DisplayName("Should create customer successfully")
void testCreateCustomer_Success() { }

@Test @DisplayName("Should retrieve customer by ID")
void testGetCustomerById_Success() { }

@Test @DisplayName("Should update customer with valid data")
void testUpdateCustomer_Success() { }

@Test @DisplayName("Should delete customer (soft delete)")
void testDeleteCustomer_Success() { }
```

#### 2. Error Cases
```java
@Test @DisplayName("Should throw exception when customer not found")
void testGetCustomerById_NotFound() { }

@Test @DisplayName("Should throw exception on duplicate phone")
void testCreateCustomer_DuplicatePhone() { }

@Test @DisplayName("Should throw exception on invalid email")
void testCreateCustomer_InvalidEmail() { }
```

#### 3. Search & Pagination
```java
@Test @DisplayName("Should retrieve paginated customers")
void testGetAllCustomers_Pagination() { }

@Test @DisplayName("Should search customers by keyword")
void testGetAllCustomers_WithSearch() { }

@Test @DisplayName("Should return empty results when no matches")
void testGetAllCustomers_NoResults() { }
```

#### 4. Special Operations
```java
@Test @DisplayName("Should get or create customer")
void testGetOrCreateCustomer_ExistingCustomer() { }

@Test @DisplayName("Should create new customer if not found")
void testGetOrCreateCustomer_NewCustomer() { }

@Test @DisplayName("Should get customer by phone")
void testGetCustomerByPhone_Success() { }

@Test @DisplayName("Should get customer by email")
void testGetCustomerByEmail_Success() { }
```

### Coverage Requirements

**Minimum Coverage**: >95%

```bash
# Run tests with coverage
mvn clean test jacoco:report

# View coverage report
open target/site/jacoco/index.html
```

**Coverage Breakdown**:
- Service methods: 95%+
- Controller endpoints: 90%+
- Error paths: 100%
- Edge cases: 90%+

### Test Template

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerService Unit Tests")
class CustomerServiceTest {
    
    @Mock
    private CustomerRepository customerRepository;
    
    @Mock
    private MessageService messageService;
    
    @InjectMocks
    private CustomerServiceImpl customerService;
    
    private Customer testCustomer;
    private CreateCustomerRequest createRequest;
    
    @BeforeEach
    void setUp() {
        testCustomer = Customer.builder()
            .id(1L)
            .name("John Doe")
            .phone("0987654321")
            .email("john@example.com")
            .build();
        testCustomer.setCreatedAt(LocalDateTime.now());
        testCustomer.setDeleted(false);
        
        createRequest = CreateCustomerRequest.builder()
            .name("John Doe")
            .phone("0987654321")
            .email("john@example.com")
            .build();
    }
    
    @Test
    @DisplayName("Should create customer successfully")
    void testCreateCustomer_Success() {
        // Arrange
        when(customerRepository.save(any(Customer.class)))
            .thenReturn(testCustomer);
        
        // Act
        CustomerDTO result = customerService.createCustomer(createRequest);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("John Doe");
        verify(customerRepository).save(any(Customer.class));
    }
}
```

---

## 🛑 Error Handling

### Error Messages

All errors use MessageService for localized messages.

**English** (`messages.properties`):
```properties
error.customer.not.found=Customer with ID {0} not found
error.customer.invalid.phone=Invalid phone number format: {0}
error.customer.duplicate.phone=Customer with phone {0} already exists
error.customer.invalid.email=Invalid email format: {0}
error.customer.update.failed=Failed to update customer: {0}
error.customer.delete.failed=Failed to delete customer: {0}
```

**Vietnamese** (`messages_vi.properties`):
```properties
error.customer.not.found=Không tìm thấy khách hàng với ID {0}
error.customer.invalid.phone=Định dạng số điện thoại không hợp lệ: {0}
error.customer.duplicate.phone=Khách hàng với số điện thoại {0} đã tồn tại
error.customer.invalid.email=Định dạng email không hợp lệ: {0}
error.customer.update.failed=Cập nhật khách hàng thất bại: {0}
error.customer.delete.failed=Xoá khách hàng thất bại: {0}
```

### HTTP Status Codes

| Status | Scenario | Example |
|--------|----------|---------|
| 201 | Customer created | POST /customers |
| 200 | Customer retrieved/updated | GET/PUT /customers/{id} |
| 204 | Customer deleted | DELETE /customers/{id} |
| 400 | Invalid input | Missing required fields |
| 404 | Customer not found | GET /customers/999 |
| 409 | Duplicate phone | POST with existing phone |
| 500 | Server error | Database error |

### Error Response Format

```json
{
  "success": false,
  "data": null,
  "message": "Customer with ID 999 not found",
  "errors": []
}
```

---

## ✅ Best Practices

### 1. Always Use MessageService
```java
// ✅ GOOD
String msg = messageService.getMessage("error.customer.not.found", id);
throw new ResourceNotFoundException(msg);

// ❌ BAD
throw new ResourceNotFoundException("Customer not found");
```

### 2. Validate Early
```java
// ✅ GOOD - Check duplicates before saving
if (customerRepository.findByPhone(phone).isPresent()) {
    throw new DuplicateResourceException("Phone already exists");
}
customerRepository.save(customer);
```

### 3. Use Optional Properly
```java
// ✅ GOOD
Customer customer = customerRepository.findById(id)
    .orElseThrow(() -> new ResourceNotFoundException("Not found"));

// ❌ BAD
Customer customer = customerRepository.findById(id).get(); // Throws NoSuchElementException
```

### 4. Soft Delete Consistently
```java
// ✅ GOOD - Use active-only queries
customerRepository.findByIdActive(id);

// ❌ BAD - Returns deleted customers
customerRepository.findById(id);
```

### 5. Proper Logging
```java
// ✅ GOOD
log.info("Creating customer: name={}, phone={}", request.getName(), request.getPhone());
log.error("Failed to create customer", exception);

// ❌ BAD
System.out.println("Customer created");
```

### 6. Transaction Boundaries
```java
// ✅ GOOD - Entire operation is atomic
@Transactional
public void deleteCustomer(Long id) {
    Customer customer = findById(id);
    customer.setDeleted(true);
    customer.setDeletedAt(LocalDateTime.now());
    customerRepository.save(customer);
}

// ❌ BAD - No transaction management
public void deleteCustomer(Long id) {
    customerRepository.deleteById(id);
}
```

### 7. DTO Separation
```java
// ✅ GOOD - Separate request/response DTOs
@PostMapping
public ResponseEntity<ApiResponse<CustomerDTO>> create(
    @RequestBody @Valid CreateCustomerRequest request) {
    // ...
}

// ❌ BAD - Using entity directly
@PostMapping
public ResponseEntity<Customer> create(@RequestBody Customer customer) {
    // ...
}
```

### 8. Pagination Always
```java
// ✅ GOOD - Returns paginated results
@GetMapping
public Page<CustomerDTO> getAll(Pageable pageable) {
    return customerService.getAll(pageable);
}

// ❌ BAD - Returns all results at once
@GetMapping
public List<CustomerDTO> getAll() {
    return customerService.getAll();
}
```

---

## 📚 Related Documentation

- [Backend README](../../README.md) - Main backend documentation
- [API Documentation Standards](../../README.md#-api-documentation-requirements)
- [Error Handling Standards](../../README.md#-error-handling--api-message-standards)
- [Testing Requirements](../../README.md#-unit-testing-requirements)

---

## 🔗 References

- **Controller**: `src/main/java/com/knp/controller/CustomerController.java`
- **Service**: `src/main/java/com/knp/service/CustomerServiceImpl.java`
- **Entity**: `src/main/java/com/knp/model/entity/Customer.java`
- **Tests**: `src/test/java/com/knp/service/CustomerServiceTest.java`
- **Messages**: `src/main/resources/i18n/messages.properties`

---

## 📞 Support

For questions or issues related to the Customer feature:
- Check this documentation first
- Review test examples in CustomerServiceTest.java
- Check API documentation at `/api/swagger-ui.html`
- Review error messages in properties files

---

**Last Updated**: March 15, 2026  
**Maintainer**: Development Team  
**Version**: 1.0.0

