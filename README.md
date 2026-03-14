# Retail Platform - Backend Application

A robust, scalable Java-based REST API backend for a multi-tenant retail platform built with Spring Boot 4.0.0 and Java 17, providing comprehensive e-commerce and inventory management capabilities.

## 🎯 Project Overview

The Retail Platform Backend is a production-ready microservice that powers the retail platform ecosystem. It provides secure, RESTful APIs for managing products, inventory, orders, users, and tenants with advanced features like JWT authentication, multi-tenancy support, and comprehensive business logic.

**Framework**: Spring Boot 4.0.0  
**Language**: Java 17  
**Database**: MySQL 8.0+  
**Frontend Repository**: [https://github.com/nguyendangkhoa25/retail-platform-client]  
**API Base URL**: `http://localhost:8080/api/v1`

---

## ✨ Key Features

### Core Functionality

- **🔐 User Authentication & Authorization**
  - JWT token-based authentication
  - Role-based access control (RBAC)
  - Multi-level user roles (Shop Owner, Manager, Staff)
  - Secure password hashing and reset flow
  - Automatic token refresh mechanism

- **📦 Product Management System**
  - Support for 18 different product types
  - Dynamic attribute system (Food, Electronics, Drugs, etc.)
  - Product categorization with hierarchical support
  - SKU management with uniqueness validation
  - Full-text search capabilities

- **📊 Inventory Management**
  - Real-time stock level tracking
  - Multiple batch/lot management per product
  - Expiry date tracking and alerts
  - Low-stock threshold monitoring
  - Warehouse location management
  - Reorder point configuration

- **📋 Order Management**
  - Complete order lifecycle management
  - Order status tracking (Pending, Processing, Shipped, Delivered, Cancelled)
  - Order item management with pricing
  - Invoice generation and tracking

- **👤 User & Customer Management**
  - User profile management
  - Customer data handling
  - Address management
  - Role and permission assignment

- **📊 Admin Features**
  - Comprehensive user management
  - Permission control and auditing
  - System configuration management
  - Activity logging and audit trails

### Multi-Tenancy Features

- **🏢 Tenant Management**
  - Create and manage multiple retail stores
  - Tenant activation/deactivation
  - Subscription and expiry management
  - Feature allocation per tenant
  - Automatic data isolation per tenant

- **🔑 Feature-Based Access Control**
  - Granular feature assignment to tenants
  - Dynamic permission management
  - Audit logging of all access
  - Master-only endpoint protection

### Advanced Features

- **📈 Analytics & Reporting**
  - Sales metrics and KPIs
  - User behavior tracking
  - Product performance analysis
  - Revenue reports

- **🌐 Internationalization**
  - Multi-language error messages
  - English and Vietnamese support
  - Locale-specific formatting

- **🔒 Enterprise Security**
  - Spring Security integration
  - CSRF protection
  - SQL injection prevention (Parameterized queries)
  - XSS protection
  - Secure token storage and validation

---

## 🛠️ Technology Stack

### Core Technologies

| Technology | Version | Purpose |
|-----------|---------|---------|
| **Java** | 17 | Programming Language |
| **Spring Boot** | 4.0.0 | Web Framework |
| **Spring Security** | Latest | Authentication & Authorization |
| **Spring Data JPA** | Latest | Data Access Layer |
| **Hibernate** | Latest | ORM Framework |
| **MySQL** | 8.0+ | Relational Database |

### Key Libraries

| Library | Version | Purpose |
|---------|---------|---------|
| **JWT (jjwt)** | 0.13.0 | Token Management |
| **Lombok** | Latest | Reduce Boilerplate Code |
| **Validation API** | Latest | Input Validation |
| **FastExcel** | 0.18.0 | Excel Export Support |
| **ZXing** | 3.5.3 | Barcode Generation |
| **iTextPDF** | 5.5.13 | PDF Generation |
| **MapStruct** | 1.4.2 | DTO Mapping |

### Development Tools

| Tool | Purpose |
|------|---------|
| **Maven** | Build and dependency management |
| **Git** | Version control |
| **Docker** | Containerization |
| **Postman** | API testing |
| **MySQL Workbench** | Database management |

---

## 📋 Project Structure

```
backend/
├── src/
│   ├── main/
│   │   ├── java/com/knp/
│   │   │   ├── JewelryShopApplication.java     # Main application entry point
│   │   │   ├── controller/                     # REST API endpoints
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── ProductController.java
│   │   │   │   ├── InventoryController.java
│   │   │   │   ├── OrderController.java
│   │   │   │   ├── UserController.java
│   │   │   │   └── SystemController.java
│   │   │   ├── service/                        # Business logic layer
│   │   │   │   ├── ProductService.java
│   │   │   │   ├── ProductServiceImpl.java
│   │   │   │   ├── InventoryService.java
│   │   │   │   ├── OrderService.java
│   │   │   │   ├── AuthService.java
│   │   │   │   └── UserService.java
│   │   │   ├── repository/                     # Data access layer
│   │   │   │   ├── ProductRepository.java
│   │   │   │   ├── ProductTypeRepository.java
│   │   │   │   ├── InventoryRepository.java
│   │   │   │   ├── OrderRepository.java
│   │   │   │   └── UserRepository.java
│   │   │   ├── model/
│   │   │   │   ├── entity/                     # JPA Entities
│   │   │   │   │   ├── Product.java
│   │   │   │   │   ├── ProductType.java
│   │   │   │   │   ├── Inventory.java
│   │   │   │   │   ├── Order.java
│   │   │   │   │   └── User.java
│   │   │   │   ├── dto/                        # Data Transfer Objects
│   │   │   │   │   ├── product/
│   │   │   │   │   ├── inventory/
│   │   │   │   │   ├── order/
│   │   │   │   │   └── user/
│   │   │   │   └── enums/                      # Enumerations
│   │   │   │       ├── ProductStatus.java
│   │   │   │       ├── OrderStatus.java
│   │   │   │       └── UserRole.java
│   │   │   ├── mapper/                         # MapStruct Mappers
│   │   │   │   ├── ProductMapper.java
│   │   │   │   ├── InventoryMapper.java
│   │   │   │   └── OrderMapper.java
│   │   │   ├── exception/                      # Custom Exceptions
│   │   │   │   ├── ResourceNotFoundException.java
│   │   │   │   ├── BadRequestException.java
│   │   │   │   ├── UnauthorizedException.java
│   │   │   │   └── ForbiddenException.java
│   │   │   ├── config/                         # Spring Configurations
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   ├── JwtTokenProvider.java
│   │   │   │   ├── WebConfig.java
│   │   │   │   └── DataSourceConfig.java
│   │   │   ├── filter/                         # JWT and Auth Filters
│   │   │   │   ├── JwtTokenFilter.java
│   │   │   │   └── TenantInterceptor.java
│   │   │   ├── multitenant/                    # Multi-tenancy Support
│   │   │   │   ├── TenantContext.java
│   │   │   │   ├── RoutingDataSource.java
│   │   │   │   └── DatasourceManager.java
│   │   │   ├── aspect/                         # AOP Aspects
│   │   │   │   └── MasterDatabaseAccessAspect.java
│   │   │   ├── util/                           # Utility Classes
│   │   │   │   ├── DateTimeUtil.java
│   │   │   │   ├── ValidationUtil.java
│   │   │   │   └── FileUtil.java
│   │   │   └── common/                         # Constants
│   │   │       ├── SysConstant.java
│   │   │       ├── InventoryConstant.java
│   │   │       └── ProductSystemConstant.java
│   │   └── resources/
│   │       ├── application.properties          # Main configuration
│   │       ├── application-dev.properties
│   │       ├── application-prod.properties
│   │       ├── db/migration/                   # Flyway migrations
│   │       │   ├── V001__initial_schema.sql
│   │       │   ├── V003__create_inventory_table.sql
│   │       │   └── V004__create_product_type_system.sql
│   │       └── i18n/                           # Internationalization
│   │           ├── messages.properties
│   │           └── messages_vi.properties
│   └── test/
│       ├── java/com/knp/
│       │   ├── service/                        # Service layer tests
│       │   ├── controller/                     # Controller tests
│       │   └── repository/                     # Repository tests
│       └── resources/
│           └── application-test.properties
├── pom.xml                                     # Maven configuration
├── Dockerfile                                  # Docker containerization
├── README.md                                   # This file
└── .gitignore                                  # Git ignore rules
```

---

## 🚀 Getting Started

### Prerequisites

- **Java 17 JDK** or higher
- **Maven 3.8.1** or higher
- **MySQL 8.0** or higher
- **Git**
- **Docker** (optional, for containerization)

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/nguyendangkhoa25/retail-platform.git
   cd retail-platform/backend
   ```

2. **Build the project**
   ```bash
   mvn clean install
   ```

3. **Configure environment**
   ```bash
   # Create or edit application.properties
   cp src/main/resources/application.properties src/main/resources/application.properties.local
   ```

4. **Application Configuration**
   ```properties
   # Database Configuration
   spring.datasource.url=jdbc:mysql://localhost:3306/retail_platform?serverTimezone=UTC
   spring.datasource.username=root
   spring.datasource.password=your_password
   spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
   
   # JPA/Hibernate Configuration
   spring.jpa.hibernate.ddl-auto=validate
   spring.jpa.show-sql=false
   spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect
   
   # JWT Configuration
   jwt.secret=your-super-secret-key-min-256-bits-long
   jwt.expiration=86400000
   jwt.refresh-expiration=604800000
   
   # Server Configuration
   server.port=8080
   server.servlet.context-path=/api
   ```

5. **Create MySQL Database**
   ```bash
   mysql -u root -p
   CREATE DATABASE retail_platform CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

6. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

   The application will be available at `http://localhost:8080/api`

---

## 📦 Available Maven Commands

### Build Commands

```bash
# Clean and build
mvn clean install

# Build without running tests
mvn clean install -DskipTests

# Package only (no install to local repository)
mvn clean package

# Compile code only
mvn clean compile
```

### Running the Application

```bash
# Run with Maven
mvn spring-boot:run

# Run JAR file after build
java -jar target/retail-platform-backend-1.0.0.jar

# Run with custom profile
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

### Testing

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=ProductServiceTest

# Run tests with coverage
mvn clean test jacoco:report
```

### Code Quality

```bash
# Check with SonarQube
mvn clean verify sonar:sonar

# Analyze with SpotBugs
mvn spotbugs:check
```

---

## ⚙️ Configuration

### Database Configuration

MySQL database setup is handled by Flyway migrations:

```sql
-- Migrations are in: src/main/resources/db/migration/
-- They run automatically on application startup
-- Current migrations:
-- V001__initial_schema.sql - Core tables
-- V003__create_inventory_table.sql - Inventory system
-- V004__create_product_type_system.sql - Product types with attributes
```

### JWT Configuration

Configure JWT settings in `application.properties`:

```properties
# Secret key should be at least 256 bits for HS256
jwt.secret=${JWT_SECRET:your-super-secret-key-min-256-bits-long}

# Token expiration in milliseconds (24 hours)
jwt.expiration=${JWT_EXPIRATION:86400000}

# Refresh token expiration (7 days)
jwt.refresh-expiration=${JWT_REFRESH_EXPIRATION:604800000}
```

### Multi-Tenancy Configuration

Multi-tenant routing is configured in `DataSourceConfig`:

```java
// Master database configuration
spring.datasource.master.url=jdbc:mysql://localhost:3306/master_db
spring.datasource.master.username=root
spring.datasource.master.password=password

// Tenant databases are loaded dynamically
// See: multitenant/DatasourceManager.java
```

### Logging Configuration

Configure logging in `application.properties`:

```properties
# Root logger level
logging.level.root=INFO

# Application package logging
logging.level.com.knp=DEBUG

# External library logging
logging.level.org.springframework=INFO
logging.level.org.hibernate=INFO

# Log file
logging.file.name=logs/app.log
logging.file.max-size=10MB
logging.file.max-history=30
```

---

## 🔌 API Endpoints

### Authentication Endpoints

```
POST   /auth/authenticate              Login with username/password
POST   /auth/refresh-token             Refresh JWT token
POST   /auth/logout                    Logout and invalidate token
```

### Product Management Endpoints

```
POST   /products                       Create new product
GET    /products                       List all products (paginated)
GET    /products/{id}                  Get product by ID
GET    /products/type/{typeId}         Get products by type
GET    /products/search                Search products
PUT    /products/{id}                  Update product
DELETE /products/{id}                  Delete product (soft delete)
GET    /products/types/all             Get all product types
GET    /products/types/{typeId}/attributes  Get product type attributes
```

### Inventory Endpoints

```
POST   /inventory                      Create inventory record
GET    /inventory                      List inventory (paginated)
GET    /inventory/{id}                 Get inventory by ID
GET    /inventory/product/{productId}  Get inventory for product (all batches)
PUT    /inventory/{id}                 Update inventory
DELETE /inventory/{id}                 Delete inventory (soft delete)
GET    /inventory/alerts/low-stock     Get low-stock items
GET    /inventory/alerts/expired       Get expired items
GET    /inventory/alerts/expiring-soon Get items expiring soon
PATCH  /inventory/{id}/add-stock       Add stock quantity
PATCH  /inventory/{id}/remove-stock    Remove stock quantity
GET    /inventory/value/total          Calculate total inventory value
```

### Order Endpoints

```
POST   /orders                         Create order
GET    /orders                         List orders (paginated)
GET    /orders/{id}                    Get order by ID
PUT    /orders/{id}                    Update order
DELETE /orders/{id}                    Cancel order
GET    /orders/search                  Search orders
```

### User Management Endpoints

```
POST   /users                          Create user
GET    /users                          List users (paginated)
GET    /users/{id}                     Get user by ID
PUT    /users/{id}                     Update user
DELETE /users/{id}                     Delete user
GET    /users/profile                  Get current user profile
PUT    /users/profile                  Update current user profile
POST   /users/change-password          Change password
```

### System/Admin Endpoints

```
GET    /systems/config                 Get system configuration
PUT    /systems/config                 Update system configuration
GET    /systems/audit-log              Get audit logs
```

---

## 📊 Database Schema

### Core Tables

**Users Table**
```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(100) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('SHOP_OWNER', 'MANAGER', 'STAFF') NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

**Products Table**
```sql
CREATE TABLE product (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_type_id BIGINT NOT NULL,
    sku VARCHAR(100) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    price DECIMAL(15, 2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (product_type_id) REFERENCES product_type(id),
    INDEX idx_sku (sku),
    INDEX idx_status (status),
    INDEX idx_deleted (deleted)
);
```

**Inventory Table**
```sql
CREATE TABLE inventory (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id BIGINT NOT NULL UNIQUE,
    quantity_in_stock BIGINT NOT NULL,
    reorder_level BIGINT NOT NULL,
    reorder_quantity BIGINT NOT NULL,
    unit_cost DECIMAL(15, 2) NOT NULL,
    warehouse_location VARCHAR(255),
    expiry_date DATE,
    batch_number VARCHAR(100),
    status VARCHAR(50) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (product_id) REFERENCES product(id),
    INDEX idx_product_id (product_id),
    INDEX idx_low_stock (quantity_in_stock, reorder_level),
    INDEX idx_expired (expiry_date, deleted)
);
```

---

## 🔐 Security Implementation

### JWT Authentication Flow

1. **Login Request**
   ```
   POST /auth/authenticate
   {
     "username": "user@example.com",
     "password": "password123"
   }
   ```

2. **Server Response**
   ```json
   {
     "success": true,
     "data": {
       "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
       "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
       "expiresIn": 86400000
     }
   }
   ```

3. **Subsequent Requests**
   ```
   Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
   ```

### Role-Based Access Control

```java
@PreAuthorize("hasAnyRole('SHOP_OWNER', 'MANAGER')")
@PostMapping("/products")
public ResponseEntity<ApiResponse<ProductDTO>> createProduct(...) {
    // Only SHOP_OWNER and MANAGER can create products
}
```

### Multi-Tenancy Security

```java
// Tenant context is automatically set from X-Tenant-ID header
@GetMapping("/products")
public ResponseEntity<ApiResponse<Page<ProductDTO>>> getProducts(...) {
    // Query automatically filters by current tenant
    // Data from other tenants is never visible
}
```

---

## 📚 API Documentation Requirements

### ✅ Mandatory API Documentation

**Every REST API endpoint MUST include comprehensive OpenAPI/Swagger documentation.** This is enforced as a code quality standard and is required before merging any feature branch.

### Using SpringDoc OpenAPI Annotations

All endpoints must be documented with the following annotations:

#### 1. Controller Level Documentation

```java
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Product Management", description = "APIs for managing products and inventory")
public class ProductController {
    // ... endpoints
}
```

#### 2. Endpoint Documentation

```java
@PostMapping
@Operation(
    summary = "Create a new product",
    description = "Creates a new product in the system with validation. " +
                  "Requires SHOP_OWNER or MANAGER role.",
    tags = {"Product Management"}
)
@ApiResponse(
    responseCode = "201",
    description = "Product created successfully",
    content = @Content(schema = @Schema(implementation = ProductDTO.class))
)
@ApiResponse(
    responseCode = "400",
    description = "Invalid product data",
    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
)
@ApiResponse(
    responseCode = "403",
    description = "User does not have permission to create products",
    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
)
@PreAuthorize("hasAnyRole('SHOP_OWNER', 'MANAGER')")
public ResponseEntity<ApiResponse<ProductDTO>> createProduct(
        @RequestBody 
        @Valid 
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Product creation request payload",
            required = true,
            content = @Content(schema = @Schema(implementation = CreateProductRequest.class))
        )
        CreateProductRequest request) {
    log.info("Creating product: {}", request.getName());
    ProductDTO product = productService.createProduct(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(product, "Product created successfully"));
}
```

#### 3. Parameter Documentation

```java
@GetMapping
@Operation(summary = "List products with pagination and filtering")
public ResponseEntity<ApiResponse<Page<ProductDTO>>> listProducts(
        @RequestParam(defaultValue = "0")
        @Parameter(description = "Zero-indexed page number", example = "0")
        int page,
        
        @RequestParam(defaultValue = "20")
        @Parameter(description = "Number of records per page", example = "20")
        int pageSize,
        
        @RequestParam(required = false)
        @Parameter(description = "Search keyword to filter products by name or SKU")
        String searchTerm,
        
        @RequestParam(defaultValue = "createdAt")
        @Parameter(description = "Field to sort by", example = "name")
        String sortBy,
        
        @RequestParam(defaultValue = "DESC")
        @Parameter(description = "Sort direction (ASC or DESC)", example = "DESC")
        String sortDirection) {
    // Implementation
}
```

#### 4. DTO Documentation

```java
@Data
@Builder
@Schema(
    description = "Request payload for creating a new product",
    example = "{\"sku\": \"PROD-001\", \"name\": \"Product Name\", \"price\": 99.99}"
)
public class CreateProductRequest {
    
    @NotBlank(message = "SKU is required")
    @Schema(
        description = "Unique product SKU",
        example = "PROD-001",
        minLength = 3,
        maxLength = 50
    )
    private String sku;
    
    @NotBlank(message = "Product name is required")
    @Schema(
        description = "Product display name",
        example = "Wireless Mouse",
        minLength = 1,
        maxLength = 255
    )
    private String name;
    
    @NotNull(message = "Price is required")
    @Positive(message = "Price must be greater than zero")
    @Schema(
        description = "Product price",
        example = "99.99",
        minimum = "0.01"
    )
    private BigDecimal price;
    
    @Schema(
        description = "Product description",
        example = "High precision wireless mouse with 2.4GHz connection",
        maxLength = 1000
    )
    private String description;
}
```

### Accessing API Documentation

After starting the application:

- **Swagger UI**: [http://localhost:8080/api/swagger-ui.html](http://localhost:8080/api/swagger-ui.html)
- **OpenAPI JSON**: [http://localhost:8080/api/v3/api-docs](http://localhost:8080/api/v3/api-docs)
- **OpenAPI YAML**: [http://localhost:8080/api/v3/api-docs.yaml](http://localhost:8080/api/v3/api-docs.yaml)

### Documentation Checklist

Before submitting a pull request, ensure:

- ✅ Controller class has `@Tag` annotation
- ✅ Every endpoint has `@Operation` annotation with clear summary and description
- ✅ All request/response DTOs have `@Schema` annotations with examples
- ✅ All path variables and query parameters have `@Parameter` annotations
- ✅ All `@ApiResponse` annotations document possible HTTP status codes
- ✅ Error responses document error conditions and formats
- ✅ Request/response examples are realistic and valid
- ✅ Swagger UI renders correctly without warnings
- ✅ Documentation is visible on Swagger UI

### Example: Complete Documented Endpoint

```java
@GetMapping("/{id}")
@Operation(
    summary = "Get product by ID",
    description = "Retrieves detailed information about a specific product by its ID. " +
                  "The product must exist and not be deleted.",
    operationId = "getProductById",
    tags = {"Product Management"}
)
@ApiResponse(
    responseCode = "200",
    description = "Product found and returned successfully",
    content = @Content(
        mediaType = "application/json",
        schema = @Schema(implementation = ProductDTO.class)
    )
)
@ApiResponse(
    responseCode = "404",
    description = "Product not found",
    content = @Content(
        mediaType = "application/json",
        schema = @Schema(implementation = ErrorResponse.class)
    )
)
public ResponseEntity<ApiResponse<ProductDTO>> getProductById(
        @PathVariable
        @Parameter(
            name = "id",
            description = "Unique product identifier",
            required = true,
            example = "123"
        )
        Long id) {
    log.info("Fetching product with id: {}", id);
    ProductDTO product = productService.getProductById(id);
    return ResponseEntity.ok(ApiResponse.success(product, "Product retrieved successfully"));
}
```

---

## 🧪 Unit Testing Requirements

### ✅ Mandatory Test Coverage: >90%

**All implemented code MUST have unit test coverage of at least 90%.** This is enforced as a quality gate and blocking requirement for code merges.

### Coverage Targets by Module

| Module | Coverage Target | Verification |
|--------|-----------------|---------------|
| **Service Layer** | 95%+ | Critical business logic |
| **Controller Layer** | 90%+ | API endpoints |
| **Repository Layer** | 85%+ | Data access logic |
| **Utility Classes** | 90%+ | Helper functions |
| **Exceptions** | 80%+ | Error handling |
| **Model Classes** | Excluded | DTOs, Entities (auto-generated) |
| **Configuration** | 80%+ | Spring configuration |

### Running Coverage Reports

#### Generate Jacoco Coverage Report

```bash
# Run tests with coverage report
mvn clean test jacoco:report

# Report location
target/site/jacoco/index.html

# Open in browser (macOS)
open target/site/jacoco/index.html

# Open in browser (Linux)
xdg-open target/site/jacoco/index.html
```

#### View Coverage by Class

The Jacoco report provides detailed breakdown:
- **Overall coverage**: Total line and branch coverage
- **By package**: Coverage for each package
- **By class**: Individual class coverage percentages
- **By method**: Method-level coverage details

### Test Structure & Patterns

#### 1. Unit Test Template

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService Unit Tests")
class ProductServiceTest {
    
    @Mock
    private ProductRepository productRepository;
    
    @Mock
    private ProductMapper productMapper;
    
    @InjectMocks
    private ProductService productService;
    
    private Product testProduct;
    private CreateProductRequest createRequest;
    
    @BeforeEach
    void setUp() {
        // Initialize test data
        testProduct = Product.builder()
            .id(1L)
            .sku("TEST-001")
            .name("Test Product")
            .price(new BigDecimal("99.99"))
            .build();
        
        createRequest = CreateProductRequest.builder()
            .sku("TEST-001")
            .name("Test Product")
            .price(new BigDecimal("99.99"))
            .build();
    }
    
    @Test
    @DisplayName("Should create product successfully")
    void testCreateProduct_Success() {
        // Arrange
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);
        
        // Act
        ProductDTO result = productService.createProduct(createRequest);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Test Product");
        verify(productRepository).save(any(Product.class));
    }
}
```

#### 2. Testing Success Paths

```java
@Test
@DisplayName("Should retrieve product by valid ID")
void testGetProductById_Success() {
    // Arrange - Setup mock repository to return product
    when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
    
    // Act - Call the service method
    ProductDTO result = productService.getProductById(1L);
    
    // Assert - Verify the result
    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(1L);
    assertThat(result.getSku()).isEqualTo("TEST-001");
}
```

#### 3. Testing Error Paths

```java
@Test
@DisplayName("Should throw exception when product not found")
void testGetProductById_NotFound() {
    // Arrange
    when(productRepository.findById(999L)).thenReturn(Optional.empty());
    when(messageService.getMessage("error.product.not.found", 999L))
        .thenReturn("Product not found");
    
    // Act & Assert
    assertThatThrownBy(() -> productService.getProductById(999L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Product not found");
}
```

#### 4. Testing Edge Cases

```java
@Test
@DisplayName("Should handle very long product name")
void testCreateProduct_LongName() {
    // Arrange
    CreateProductRequest request = CreateProductRequest.builder()
        .sku("LONG-NAME-001")
        .name("A".repeat(500))  // Very long name
        .price(new BigDecimal("99.99"))
        .build();
    
    when(productRepository.save(any(Product.class))).thenReturn(testProduct);
    
    // Act
    ProductDTO result = productService.createProduct(request);
    
    // Assert
    assertThat(result).isNotNull();
    verify(productRepository).save(any(Product.class));
}

@Test
@DisplayName("Should handle null optional fields")
void testCreateProduct_NullOptionalFields() {
    // Arrange
    CreateProductRequest request = CreateProductRequest.builder()
        .sku("NULL-TEST")
        .name("Null Test")
        .price(new BigDecimal("99.99"))
        .description(null)  // Optional field is null
        .build();
    
    when(productRepository.save(any(Product.class))).thenReturn(testProduct);
    
    // Act
    ProductDTO result = productService.createProduct(request);
    
    // Assert
    assertThat(result).isNotNull();
}
```

#### 5. Testing Data Validation

```java
@Test
@DisplayName("Should reject invalid product data")
void testCreateProduct_InvalidData() {
    // Arrange
    CreateProductRequest invalidRequest = CreateProductRequest.builder()
        .sku("")  // Empty SKU
        .name("")  // Empty name
        .price(new BigDecimal("-10"))  // Negative price
        .build();
    
    // Act & Assert - Validation should fail
    Set<ConstraintViolation<CreateProductRequest>> violations = 
        validator.validate(invalidRequest);
    assertThat(violations).isNotEmpty();
}
```

### Coverage Exclusions

The following are excluded from coverage requirements:

- **Model Classes**: `com.knp.model.*`
  - DTOs (Data Transfer Objects)
  - Entity classes (with Lombok)
  - Enum types
  - Auto-generated getters/setters

- **Configuration Classes** (if minimal business logic)
- **Main Application Class** (`JewelryShopApplication.java`)

### Achieving >90% Coverage

#### 1. Test All Public Methods

```java
// ✅ Test all public methods
@Test void testPublicMethod1() { }
@Test void testPublicMethod2() { }
@Test void testPublicMethod3() { }

// ❌ Don't leave public methods untested
// @Test void testPublicMethodX() { }  // Missing!
```

#### 2. Test All Code Branches

```java
// Method with conditional logic
public boolean isProductAvailable(Product product) {
    if (product.isDeleted()) {
        return false;  // Branch 1 - test this
    }
    if (product.getStock() <= 0) {
        return false;  // Branch 2 - test this
    }
    return true;  // Branch 3 - test this
}

// Tests for all branches
@Test void testIsProductAvailable_DeletedProduct() { }
@Test void testIsProductAvailable_OutOfStock() { }
@Test void testIsProductAvailable_Available() { }
```

#### 3. Test Exception Paths

```java
// ✅ Test both success and error paths
@Test void testOperation_Success() { }
@Test void testOperation_ThrowsException() { }
@Test void testOperation_InvalidInput() { }

// Use ArgumentCaptor for complex verifications
@Test void testServiceCall_VerifyParameters() {
    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    service.doSomething("test-value");
    verify(dependency).process(captor.capture());
    assertThat(captor.getValue()).isEqualTo("test-value");
}
```

#### 4. Use Mockito Effectively

```java
// ✅ Mock external dependencies
@Mock
private ExternalService externalService;

// ✅ Inject the service being tested
@InjectMocks
private ServiceUnderTest service;

// ✅ Configure mocks for different scenarios
when(externalService.call()).thenReturn(expectedResult);
when(externalService.call()).thenThrow(new Exception("Error"));

// ✅ Verify mock interactions
verify(externalService).call();
verify(externalService, times(2)).call();
verify(externalService, never()).call();
```

### Verification Checklist Before Merging

- ✅ Run `mvn clean test jacoco:report`
- ✅ Verify overall coverage is ≥90%
- ✅ Check module-level coverage meets targets
- ✅ All new methods have corresponding tests
- ✅ All code branches are tested
- ✅ Error cases are tested
- ✅ Edge cases are tested
- ✅ Integration with dependencies is verified
- ✅ No warnings in Jacoco report

### Code Coverage Guidelines

| Coverage | Status | Action Required |
|----------|--------|-----------------|
| **95-100%** | ✅ Excellent | Ready to merge |
| **90-94%** | ✅ Good | Ready to merge |
| **85-89%** | ⚠️ Warning | Add more tests |
| **<85%** | ❌ Failing | Must add tests |

### Example: Full Test Coverage for Service

```java
class ProductServiceTest {
    
    // Setup and success path tests...
    
    @Test
    @DisplayName("Should update product successfully")
    void testUpdateProduct_Success() { }
    
    @Test
    @DisplayName("Should throw exception when updating non-existent product")
    void testUpdateProduct_NotFound() { }
    
    @Test
    @DisplayName("Should delete product successfully")
    void testDeleteProduct_Success() { }
    
    @Test
    @DisplayName("Should throw exception when deleting non-existent product")
    void testDeleteProduct_NotFound() { }
    
    @Test
    @DisplayName("Should search products with valid criteria")
    void testSearchProducts_Success() { }
    
    @Test
    @DisplayName("Should return empty results for no matching products")
    void testSearchProducts_NoResults() { }
    
    @Test
    @DisplayName("Should handle pagination correctly")
    void testGetProductsWithPagination_Success() { }
    
    @Test
    @DisplayName("Should handle concurrent product updates")
    void testConcurrentProductUpdates() { }
}
```

---

## 🛑 Error Handling & API Message Standards

### ✅ Mandatory: All API Errors Must Return Messages

**Critical Requirement**: Every API error response MUST include a descriptive human-readable message. Generic error codes are NOT acceptable.

### Error Response Format Standard

**Required Response Structure**:

```json
{
  "success": false,
  "data": null,
  "message": "Descriptive error message in user's language",
  "errors": [
    {
      "field": "fieldName",
      "message": "Field-specific error message",
      "code": "ERROR_CODE"
    }
  ]
}
```

### Service Implementation Pattern

#### 1. Using MessageService for Localized Messages

**REQUIRED**: Always use `MessageService` to retrieve error messages. This ensures:
- ✅ Consistent error messages across application
- ✅ Localization support (English, Vietnamese, etc.)
- ✅ Easy message updates without code changes
- ✅ Professional user-facing messages

```java
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ProductServiceImpl implements ProductService {
    
    private final ProductRepository productRepository;
    private final MessageService messageService;
    
    @Override
    public ProductDTO getProductById(Long id) {
        log.info("Fetching product with id: {}", id);
        
        // ✅ CORRECT: Using MessageService for error message
        Product product = productRepository.findById(id)
            .orElseThrow(() -> {
                String errorMessage = messageService.getMessage("error.product.not.found", id);
                return new ResourceNotFoundException(errorMessage);
            });
        
        return mapToDTO(product);
    }
    
    @Override
    public ProductDTO createProduct(CreateProductRequest request) {
        log.info("Creating product: {}", request.getName());
        
        // ✅ CORRECT: Validate and provide meaningful error message
        if (productRepository.existsBySku(request.getSku())) {
            String errorMessage = messageService.getMessage("error.product.duplicate.sku", request.getSku());
            throw new DuplicateResourceException(errorMessage);
        }
        
        // ... rest of implementation
    }
}
```

**❌ INCORRECT Examples to Avoid**:

```java
// BAD: Generic error code without message
throw new RuntimeException("Error");

// BAD: Hard-coded message without MessageService
throw new ResourceNotFoundException("Product not found");

// BAD: No error context
throw new BadRequestException();

// BAD: Exposing internal system messages
throw new RuntimeException("SQLException: Connection timeout");
```

#### 2. Message Properties Files

**English Messages** (`src/main/resources/i18n/messages.properties`):

```properties
# Product-related messages
error.product.not.found=Product with ID {0} not found in the system
error.product.duplicate.sku=Product with SKU {0} already exists
error.product.invalid.price=Product price must be greater than zero (provided: {0})
error.product.invalid.name=Product name cannot be empty or exceed 255 characters
error.product.update.failed=Failed to update product: {0}
error.product.delete.failed=Failed to delete product: {0}

# Customer-related messages
error.customer.not.found=Customer with ID {0} not found
error.customer.invalid.phone=Invalid phone number format: {0}
error.customer.duplicate.phone=Customer with phone number {0} already exists

# User-related messages
error.user.not.found=User with ID {0} not found
error.user.duplicate.username=Username {0} is already taken
error.user.invalid.password=Password must be at least 8 characters
error.user.password.mismatch=Current password is incorrect
error.user.role.invalid=Role {0} is not a valid system role

# Validation messages
error.validation.required.field={0} is required and cannot be empty
error.validation.invalid.email=Invalid email format: {0}
error.validation.invalid.date=Invalid date format: {0}
error.validation.unauthorized=You do not have permission to perform this action
error.validation.forbidden=Access to this resource is forbidden

# Tenant-related messages
error.tenant.not.found=Tenant with ID {0} not found
error.tenant.expired=Tenant subscription has expired
error.tenant.datasource.creation.failed=Failed to create datasource for tenant: {0}

# Generic messages
error.internal.server=An unexpected error occurred. Please contact support if the problem persists
error.database.connection=Database connection failed. Please try again later
error.timeout=Request timeout. Please try again
```

**Vietnamese Messages** (`src/main/resources/i18n/messages_vi.properties`):

```properties
# Product-related messages
error.product.not.found=Không tìm thấy sản phẩm với ID {0}
error.product.duplicate.sku=Sản phẩm với SKU {0} đã tồn tại trong hệ thống
error.product.invalid.price=Giá sản phẩm phải lớn hơn 0 (giá trị được cung cấp: {0})
error.product.invalid.name=Tên sản phẩm không thể trống hoặc vượt quá 255 ký tự
error.product.update.failed=Cập nhật sản phẩm thất bại: {0}
error.product.delete.failed=Xoá sản phẩm thất bại: {0}

# Customer-related messages
error.customer.not.found=Không tìm thấy khách hàng với ID {0}
error.customer.invalid.phone=Định dạng số điện thoại không hợp lệ: {0}
error.customer.duplicate.phone=Khách hàng với số điện thoại {0} đã tồn tại

# User-related messages
error.user.not.found=Không tìm thấy người dùng với ID {0}
error.user.duplicate.username=Tên tài khoản {0} đã được sử dụng
error.user.invalid.password=Mật khẩu phải có ít nhất 8 ký tự
error.user.password.mismatch=Mật khẩu hiện tại không chính xác
error.user.role.invalid=Vai trò {0} không phải vai trò hợp lệ của hệ thống

# Validation messages
error.validation.required.field={0} là bắt buộc và không thể trống
error.validation.invalid.email=Định dạng email không hợp lệ: {0}
error.validation.invalid.date=Định dạng ngày tháng không hợp lệ: {0}
error.validation.unauthorized=Bạn không có quyền thực hiện hành động này
error.validation.forbidden=Truy cập vào tài nguyên này bị cấm

# Tenant-related messages
error.tenant.not.found=Không tìm thấy tenant với ID {0}
error.tenant.expired=Gói dịch vụ của tenant đã hết hạn
error.tenant.datasource.creation.failed=Tạo datasource cho tenant {0} thất bại

# Generic messages
error.internal.server=Đã xảy ra lỗi không mong muốn. Vui lòng liên hệ với support
error.database.connection=Kết nối database thất bại. Vui lòng thử lại
error.timeout=Yêu cầu hết thời gian chờ. Vui lòng thử lại
```

### Controller Error Handling Pattern

```java
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Product Management", description = "APIs for managing products")
public class ProductController {
    
    private final ProductService productService;
    
    @PostMapping
    @Operation(summary = "Create a new product")
    public ResponseEntity<ApiResponse<ProductDTO>> createProduct(
            @RequestBody @Valid CreateProductRequest request) {
        try {
            log.info("Creating product: {}", request.getName());
            ProductDTO product = productService.createProduct(request);
            
            // ✅ Success response with message
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(product, "Product created successfully"));
        } catch (DuplicateResourceException e) {
            // ✅ Error already has MessageService-generated message
            log.warn("Duplicate product creation attempt: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating product", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to create product"));
        }
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID")
    public ResponseEntity<ApiResponse<ProductDTO>> getProductById(
            @PathVariable Long id) {
        try {
            ProductDTO product = productService.getProductById(id);
            return ResponseEntity.ok(
                ApiResponse.success(product, "Product retrieved successfully"));
        } catch (ResourceNotFoundException e) {
            // ✅ Error message comes from MessageService
            log.warn("Product not found: id={}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(e.getMessage()));
        }
    }
}
```

### Global Exception Handler Pattern

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(
            ResourceNotFoundException ex) {
        log.error("Resource not found: {}", ex.getMessage());
        
        // ✅ Error message from exception (which came from MessageService)
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(ex.getMessage()));
    }
    
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateResource(
            DuplicateResourceException ex) {
        log.error("Duplicate resource: {}", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiResponse.error(ex.getMessage()));
    }
    
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(
            BadRequestException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(ex.getMessage()));
    }
    
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorized(
            UnauthorizedException ex) {
        log.warn("Unauthorized access: {}", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error(ex.getMessage()));
    }
    
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponse<Void>> handleForbidden(
            ForbiddenException ex) {
        log.warn("Forbidden access: {}", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error(ex.getMessage()));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(
            Exception ex) {
        log.error("Unexpected error", ex);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("An unexpected error occurred. Please contact support."));
    }
}
```

### Validation Error Response with Multiple Errors

```java
@PostMapping
@Operation(summary = "Create product")
public ResponseEntity<ApiResponse<Void>> createProduct(
        @RequestBody @Valid CreateProductRequest request) {
    // Validation happens automatically - returns detailed errors
    // ...
}

// Example error response for validation failure:
{
  "success": false,
  "data": null,
  "message": "Validation failed for 2 fields",
  "errors": [
    {
      "field": "sku",
      "message": "SKU is required and cannot be empty",
      "code": "VALIDATION_ERROR"
    },
    {
      "field": "price",
      "message": "Price must be greater than zero",
      "code": "VALIDATION_ERROR"
    }
  ]
}
```

### Error Message Testing Requirements

All error paths must have corresponding tests:

```java
@Test
@DisplayName("Should return meaningful error message when product not found")
void testGetProductById_ErrorMessage() {
    // Given
    when(productRepository.findById(999L)).thenReturn(Optional.empty());
    when(messageService.getMessage("error.product.not.found", 999L))
        .thenReturn("Product with ID 999 not found in the system");
    
    // When & Then
    assertThatThrownBy(() -> productService.getProductById(999L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Product with ID 999 not found in the system");
}

@Test
@DisplayName("Should return error response with message from controller")
void testGetProductById_ErrorResponse() {
    // Test that controller returns proper error response with message
    when(productService.getProductById(999L))
        .thenThrow(new ResourceNotFoundException("Product not found"));
    
    // Verify response status and message
}
```

### Error Handling Checklist

Before submitting PR, ensure:

- ✅ All exception throwing points use `MessageService.getMessage()`
- ✅ Error messages are descriptive and user-friendly
- ✅ Error messages support both English and Vietnamese
- ✅ All error paths return proper HTTP status codes
- ✅ Error responses follow ApiResponse format
- ✅ No hard-coded error messages in code
- ✅ No internal system errors exposed to clients
- ✅ All error scenarios have test coverage
- ✅ MessageService keys exist in both properties files
- ✅ Error messages use placeholder parameters `{0}`, `{1}` for dynamic values

### HTTP Status Code Standards

| Status | When to Use | Example |
|--------|------------|---------|
| **400** | Invalid request data | Missing required field, invalid format |
| **401** | Authentication failed | Invalid token, expired token |
| **403** | Authorization failed | User lacks required role/permission |
| **404** | Resource not found | Product ID doesn't exist |
| **409** | Conflict/Duplicate | SKU already exists |
| **422** | Validation failure | Business rule violation |
| **500** | Server error | Unexpected exception |
| **503** | Service unavailable | Database down, external service error |

### Example: Complete Error Flow

**1. Service Layer** - Generate error with MessageService:
```java
String msg = messageService.getMessage("error.product.duplicate.sku", request.getSku());
throw new DuplicateResourceException(msg);
```

**2. Controller Layer** - Catch and return with HTTP status:
```java
catch (DuplicateResourceException e) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ApiResponse.error(e.getMessage()));
}
```

**3. Client Receives** - Meaningful error message:
```json
{
  "success": false,
  "message": "Product with SKU PROD-001 already exists",
  "data": null,
  "errors": []
}
```

---

## 📝 Code Style & Conventions



### Error Messages

Localized error messages are in:
- `src/main/resources/i18n/messages.properties` - English
- `src/main/resources/i18n/messages_vi.properties` - Vietnamese

### Usage Example

```java
String errorMsg = messageService.getMessage("error.product.not.found", productId);
// Returns: "Product not found with id: 123" (English)
// Returns: "Không tìm thấy sản phẩm với id: 123" (Vietnamese)
```

---

## 🧪 Testing

### Unit Tests

```bash
# Run all unit tests
mvn test

# Run specific test
mvn test -Dtest=ProductServiceTest
```

### Integration Tests

Uses TestContainers for MySQL integration:

```java
@Testcontainers
@SpringBootTest
public class ProductRepositoryIntegrationTest {
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");
    // Tests run against actual MySQL instance
}
```

### Test Coverage

```bash
# Generate coverage report
mvn clean test jacoco:report
# Open: target/site/jacoco/index.html
```

---

## 🚀 Deployment

### Docker Deployment

```bash
# Build Docker image
docker build -t retail-platform-backend:latest .

# Run container
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://mysql-db:3306/retail_platform \
  -e SPRING_DATASOURCE_USERNAME=root \
  -e SPRING_DATASOURCE_PASSWORD=password \
  retail-platform-backend:latest
```

### Docker Compose

```yaml
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: password
      MYSQL_DATABASE: retail_platform
    ports:
      - "3306:3306"
  
  backend:
    build: .
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/retail_platform
    depends_on:
      - mysql
```

### AWS Deployment

1. **Build JAR**
   ```bash
   mvn clean package -DskipTests
   ```

2. **Upload to AWS**
   ```bash
   # Using AWS CLI or Elastic Beanstalk
   aws elasticbeanstalk create-application-version \
     --application-name retail-platform-backend \
     --version-label v1.0.0 \
     --source-bundle S3Bucket=my-bucket,S3Key=backend.jar
   ```

3. **Environment Configuration**
   - Set environment variables on AWS
   - Configure RDS MySQL instance
   - Set up security groups for database access

---

## 📚 API Documentation

### Using Swagger/Springdoc OpenAPI

The API is documented with Swagger/OpenAPI:

```bash
# Access Swagger UI
http://localhost:8080/api/swagger-ui.html

# Access OpenAPI JSON
http://localhost:8080/api/v3/api-docs
```

### Example API Request

```bash
curl -X POST http://localhost:8080/api/v1/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "productTypeId": 1,
    "sku": "PROD-001",
    "name": "Product Name",
    "description": "Product Description",
    "price": 99.99
  }'
```

---

## 🐛 Troubleshooting

### Database Connection Issues

```
Error: Access denied for user 'root'@'localhost'
Solution: Verify MySQL credentials in application.properties
```

```
Error: Unknown database 'retail_platform'
Solution: Create database first: CREATE DATABASE retail_platform;
```

### JWT Token Issues

```
Error: Token expired
Solution: Client should request new token using refresh token endpoint
```

```
Error: Invalid signature
Solution: Ensure JWT secret is consistent between signing and validation
```

### Multi-Tenancy Issues

```
Error: No tenant context found
Solution: Ensure X-Tenant-ID header is included in all requests
```

---

## 🤝 Contributing

1. **Create feature branch**
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. **Follow code style**
   - Use Spring Boot conventions
   - Follow naming patterns
   - Write unit tests for new code

3. **Commit with clear messages**
   ```bash
   git commit -m "feat: add new feature"
   ```

4. **Push and create Pull Request**
   ```bash
   git push origin feature/your-feature-name
   ```

---

## 📝 Code Style & Conventions

### Naming Conventions

- **Classes**: PascalCase (`ProductService.java`)
- **Methods**: camelCase (`getProductById()`)
- **Constants**: UPPER_SNAKE_CASE (`DEFAULT_PAGE_SIZE`)
- **Variables**: camelCase (`productId`)

### Service Layer Pattern

```java
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ProductServiceImpl implements ProductService {
    
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    
    @Override
    public ProductDTO getProductById(Long id) {
        log.info("Fetching product with id: {}", id);
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        return productMapper.toDTO(product);
    }
}
```

### Controller Pattern

```java
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@Slf4j
public class ProductController {
    
    private final ProductService productService;
    
    @PostMapping
    @PreAuthorize("hasAnyRole('SHOP_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<ProductDTO>> createProduct(
            @RequestBody @Valid CreateProductRequest request) {
        log.info("Creating product: {}", request.getName());
        ProductDTO product = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(product, "Product created successfully"));
    }
}
```

---

## 📄 License

This project is proprietary and confidential. All rights reserved.

---

## 🆘 Support

### Getting Help

- **Documentation**: Check docs folder for detailed guides
- **Issues**: Report bugs on GitHub Issues
- **Email**: support@retailplatform.com
- **Technical Issues**: Check application logs in `logs/app.log`

### Contact Information

- **Project Lead**: [Contact Info]
- **Development Team**: [Contact Info]
- **DevOps Team**: [Contact Info]

---

## 📈 Roadmap

### Upcoming Features

- [ ] **Advanced Payment Integration** (Stripe, PayPal)
- [ ] **Email Notifications** (Order confirmations, alerts)
- [ ] **SMS Notifications** (Stock alerts, promotions)
- [ ] **Real-time Updates** (WebSocket support)
- [ ] **GraphQL API** (Alternative to REST)
- [ ] **Machine Learning** (Inventory forecasting)
- [ ] **Microservices Architecture** (Service decomposition)

### Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2026-03 | Initial release with core features |
| 1.1.0 | 2026-04 | (Planned) Enhanced APIs |
| 1.2.0 | 2026-05 | (Planned) Performance optimization |

---

## 🙏 Acknowledgments

- **Spring Boot** community for excellent framework
- **MySQL** for reliable database
- **JWT** for secure authentication
- **Development Team** for robust implementation

---

**Last Updated**: March 14, 2026  
**Current Version**: 1.0.0  
**Status**: Production Ready ✅

---

**Happy Coding!** 🚀

For more information, visit our [Project Repository](https://github.com/nguyendangkhoa25/retail-platform) or check the [Backend Wiki](https://github.com/nguyendangkhoa25/retail-platform/wiki).

