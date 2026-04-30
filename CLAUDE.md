# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Build
mvn clean install              # Full build with tests
mvn clean install -DskipTests  # Skip tests

# Run (dev profile: port 6868)
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"

# Test
mvn test                                 # All tests
mvn test -Dtest=ClassName                # Single test class
mvn clean test jacoco:report             # With coverage report
```

API base path: `http://localhost:6868/api` (dev) or `http://localhost:8080/api` (default).

Swagger UI: `http://localhost:6868/api/swagger-ui.html`

## Architecture Overview

**Multi-tenant Spring Boot 3.3.5 / Java 21 REST API** with database-per-tenant isolation. The master database (`retail-platform-master`) stores tenants, users, roles, and features. Each tenant gets its own database (`retail-platform-{tenantId}`) for products, inventory, orders, carts, and customers.

### Multi-Tenancy Request Flow

Every request carries an `X-Tenant-ID` header:

1. `TenantInterceptor` validates the header. Public paths (`/api/tenants`, `/api/swagger-ui`, `/api/v3/api-docs`, `/actuator`) skip validation. Flexible paths (`/api/auth`, `/api/users`, `/api/employees`, `/api/multi-tenants`, `/api/profiles`) work with or without the header.
2. When a tenant ID is present and valid, `TenantInterceptor` calls `tenantContext.setCurrentTenant(tenant)`.
3. `TenantContext` stores the `Tenant` entity in a `ThreadLocal` and writes `tenantId` to MDC for logging.
4. `RoutingDataSource` (extends `AbstractRoutingDataSource`) calls `tenantContext.getCurrentTenantId()` before each DB operation to select the right connection pool from `DatasourceManager`. Returns `"master"` when no tenant is set.
5. `TenantInterceptor.afterCompletion` always calls `tenantContext.clear()` to prevent cross-request leakage.

The `X-Tenant-ID: master` special value routes to the master database without tenant validation. Two AOP annotations enforce access:
- `@MasterDatabaseOnly` — endpoint requires no tenant context + `MASTER_TENANT` role + `isMasterUser=true` (used on master-admin controllers like `MultiTenantController`, `FeedbackController` admin endpoints).
- `@RequiresFeature("NAME")` — endpoint requires the named feature in the caller's JWT (used on all shop and shared controllers). See Feature Access Model below.

`DatasourceManager` supports **runtime add/remove** of tenant datasources without restarting — used when provisioning new tenants (`TenantProvisioningService`).

### Auth & Session Model

- Login (`POST /api/auth/login`) returns a short-lived JWT (access token in body) and optionally a `refresh-token` HttpOnly cookie when `rememberMe=true`.
- The JWT payload carries `roles`, `features` (intersection of tenant-assigned features and role-assigned features), `isMasterUser`, and `sessionId`.
- **Single-device enforcement**: `SessionRegistry` (in-memory `ConcurrentHashMap`, keyed by `tenantKey → username → SessionInfo`) allows only one active session per user per tenant. A second login from a different device throws `DeviceConflictException` (HTTP 409) with the existing session's IP, user-agent, and login time. The client can force-login (`POST /api/auth/force-login`) to evict the existing session.
- Sessions are also persisted to `active_sessions` table (best-effort, for audit and restart recovery — the in-memory map is authoritative).
- Account is locked after 5 consecutive failed login attempts (`failedLoginAttempts` column on `users`).
- Token refresh (`POST /api/auth/refresh`) re-computes the feature intersection at refresh time so plan changes take effect without re-login.

### Feature Access Model

**A user can access only what is in their JWT `features` claim.** There is no hierarchy, no role-based override, and no implicit access to any endpoint — including feedback or notifications. Master tenant assigns features to the shop; the shop assigns features to roles; the intersection goes into the JWT. That token is the sole source of truth at every layer.

Features are enforced at three levels:

**1. Computation (master database, at login/refresh)**
- **Tenant features** — comma-separated string on `Tenant.features` column. Controlled exclusively by the master admin.
- **Role features** — `role_features` join table in the tenant DB. Controlled by the shop owner via role management.
- `TenantFeatureService.getAccessibleFeaturesByRoleAndTenant()` returns the **intersection** (tenant features ∩ role features) and embeds it in the JWT. No feature outside this intersection is ever accessible. Changes only take effect after the next token refresh.

**2. Backend enforcement (`@RequiresFeature` + AOP)**
- `JwtAuthenticationFilter` extracts the `features` claim from the JWT and stores it in `FeatureContext` (ThreadLocal). Cleared in `finally` after each request.
- **Every shop controller must be annotated** `@RequiresFeature("FEATURE_NAME")`. No exceptions — if a controller handles shop data, it must have this annotation. Controllers without it are accessible to all authenticated users, which is wrong.
- `FeatureAccessAspect` intercepts all annotated controllers, reads `FeatureContext`, and throws `ForbiddenException("error.access.feature_required")` → HTTP 403 if the feature is absent.
- Master-tenant users have features `[DASHBOARD, USER, TENANT_MGMT, VENDOR_MGMT, ACTIVITY_LOG, FEEDBACK_MGMT]` — they are automatically blocked from all shop endpoints by this mechanism. No special-casing needed.
- Master-only endpoints (tenant provisioning, feedback admin) use `@MasterDatabaseOnly` instead — checks `MASTER_TENANT` role + `isMasterUser` flag, not features.

**3. Frontend gating (`FeatureProtectedRoute`)**
- Decodes features from the JWT in `localStorage` and blocks navigation client-side. Every shop route must be wrapped in `<FeatureProtectedRoute featureName="...">`. Routes without this wrapper are accessible to all authenticated users — only add unwrapped routes for truly global pages (login, 404, maintenance).

**Feature → controller mapping:**

| Feature | Controllers |
|---------|------------|
| `DASHBOARD` | `DashboardController` |
| `ORDER` | `OrderController` |
| `POS` | `CartController` |
| `PRODUCT` | `ProductController`, `CategoryController`, `VariantTypeController` |
| `INVENTORY` | `InventoryController` |
| `PROMOTION` | `PromotionController` |
| `EMPLOYEE` | `EmployeeController` |
| `SALARY` | `SalaryController` |
| `CUSTOMER` | `CustomerController` |
| `LOYALTY` | `LoyaltyController` |
| `INVOICE` | `InvoiceController` |
| `REVENUE` | `RevenueController` |
| `EXPENSE` | `ShopExpenseController` |
| `USER` | `UserController` |
| `SHOP_INFO` | `ShopInfoController` |
| `PRINT_TEMPLATE` | `PrintTemplateController` |
| `BANK_ACCOUNT` | `BankAccountController` |
| `ACCOUNTING` | (stub page — no controller yet) |
| `VENDOR` | `VendorController`, `PurchaseOrderController` |
| `PAWN` | `PawnController`, `BuybackOrderController`, `MarketPriceController`, `GoldPriceController` |
| `ACTIVITY_LOG` | `ActivityLogController` |
| `NOTIFICATION` | `NotificationController` |
| `FEEDBACK` | `FeedbackController` (shop-user methods only; admin methods use `@MasterDatabaseOnly`) |

**Do NOT use `@PreAuthorize`** — `@EnableMethodSecurity` is not configured; these annotations are silently ignored.

### Product System (EAV)

Products use Entity-Attribute-Value to support 18+ types (Food, Beverage, Drug, Electronics, etc.) without schema changes:
`ProductType` → `AttributeGroup` → `AttributeDefinition` → `ProductAttributeValue`

Base `Product` holds SKU, price, cost, unit, vendor, shelf location, and status. Dynamic attributes live in `ProductAttributeValue`.

### Tenant Provisioning

`POST /api/multi-tenants` (master-only) calls `TenantProvisioningService.provision()` with `TenantContext` set to the new tenant, which seeds roles, features, role-feature mappings, shop info, admin user, and a default walk-in customer (`phone=0000000000`, name `"Khách lẻ"`).

### Layer Conventions

| Layer | Pattern |
|-------|---------|
| Controllers | `@RestController` in `com.knp.controller` |
| Services | Interface + `Impl` class in `com.knp.service` |
| Repositories | Spring Data JPA in `com.knp.repository` |
| Entities | `com.knp.model.entity` |
| DTOs | `com.knp.model.dto` (organized by domain) |
| Enums | `com.knp.model.enums` |
| Exceptions | `com.knp.exception` |

Constructor injection via Lombok `@RequiredArgsConstructor` is the project standard.

All API responses use `ApiResponse<T>` with `success`, `message`, `data`, and `error` fields. `GlobalExceptionHandler` maps all exception types to appropriate HTTP status codes and `ApiResponse.error(...)`.

### Shop-Type DML Files

Each shop type gets its own seed data file under `src/main/resources/db/tenant/`. `TenantDatabaseSetupService` maps `ShopType` → DML path:

| ShopType | DML file |
|----------|----------|
| `CONVENIENCE_STORE` | `tenant/convenience_store.sql` |
| `PAWN_SHOP` | `tenant/pawn_shop.sql` |
| Everything else | `tenant/general.sql` |

**When to add a dedicated DML instead of falling through to `general.sql`:** whenever a shop type has a dominant product type with non-trivial attribute needs (e.g. `PHARMACY` → `DRUG`, `ELECTRONICS` → `ELECTRONICS`, `JEWELRY` → JEWELRY/gold). Add `Map.entry(ShopType.PHARMACY, "db/tenant/pharmacy.sql")` in `TenantDatabaseSetupService` and create the file.

#### Required sections in every DML file

```
1. Shop info          — INSERT INTO shop_info (shop_name, ...)
2. Product types      — INSERT INTO product_type ... ON DUPLICATE KEY UPDATE
3. Categories         — INSERT INTO category ... ON DUPLICATE KEY UPDATE
4. Walk-in customer   — INSERT IGNORE INTO customers (id=790000001, phone='0000000000', ...)
5. Vendors (optional) — 1–2 placeholder suppliers relevant to the shop type
6. Sample products    — 15–40 realistic items the shop actually sells
7. Product→category   — INSERT IGNORE INTO product_category
8. Inventory          — INSERT INTO inventory for every product
9. Loyalty program    — single default program + 4 tiers
10. Print template    — RECEIPT template with INSERT IGNORE
11. Attribute groups  — per product type, Vietnamese names, display_order
12. Attribute defs    — per group, Vietnamese names (see below)
13. Banks             — INSERT IGNORE INTO banks (full Vietnamese bank list)
```

#### Attribute definitions are the most important part

**This is the section that differs most between shop types and must not be skimped on.** The EAV system is only useful if the attributes actually cover what the shop owner needs to track.

Rules:
- Every primary product type for the shop must have attribute groups and definitions.
- Must-have attributes (`required=TRUE`) are what the owner **cannot** create a product without. Keep this list short — 2–5 max.
- Nice-to-have attributes (`required=FALSE`) are everything useful for search, filtering, or compliance.
- Set `searchable=TRUE` for any attribute users type to search by (brand, model, ingredient). Set `filterable=TRUE` for faceted filtering (category, condition, dosage form). Rarely both false.
- Use `ON DUPLICATE KEY UPDATE name = VALUES(name)` on every INSERT so the script is idempotent.

#### Per-shop-type attribute reference

**PHARMACY (`DRUG` product type)**

Attribute groups: `Thông tin thuốc` · `Thành phần & Liều dùng` · `Bảo quản & Hạn dùng` · `Phân loại & Quy định`

| Code | Name (VI) | Type | required | searchable | filterable |
|------|-----------|------|----------|------------|------------|
| `drug_registration_number` | Số đăng ký (SDK) | STRING | TRUE | TRUE | FALSE |
| `manufacturer` | Nhà sản xuất | STRING | TRUE | TRUE | TRUE |
| `country_of_origin` | Xuất xứ | STRING | FALSE | TRUE | TRUE |
| `active_ingredient` | Hoạt chất chính | STRING | TRUE | TRUE | TRUE |
| `concentration` | Hàm lượng / Nồng độ | STRING | TRUE | FALSE | TRUE |
| `dosage_form` | Dạng bào chế | STRING | TRUE | FALSE | TRUE |
| `package_size` | Quy cách đóng gói | STRING | FALSE | TRUE | FALSE |
| `indication` | Chỉ định (tác dụng) | TEXT | FALSE | TRUE | FALSE |
| `contraindication` | Chống chỉ định | TEXT | FALSE | FALSE | FALSE |
| `side_effects` | Tác dụng phụ | TEXT | FALSE | FALSE | FALSE |
| `prescription_required` | Thuốc kê đơn | BOOLEAN | TRUE | FALSE | TRUE |
| `expiry_date` | Hạn sử dụng | DATE | TRUE | FALSE | TRUE |
| `storage_condition` | Điều kiện bảo quản | STRING | FALSE | FALSE | TRUE |
| `lot_number` | Số lô sản xuất | STRING | FALSE | TRUE | FALSE |
| `drug_category` | Nhóm dược lý | STRING | FALSE | TRUE | TRUE |

**JEWELRY (`JEWELRY` product type, or use `HEALTH` for gold/silver)**

Attribute groups: `Thông tin vật liệu` · `Thông số kỹ thuật` · `Giám định & Chứng chỉ`

| Code | Name (VI) | Type | required | searchable | filterable |
|------|-----------|------|----------|------------|------------|
| `material` | Chất liệu (vàng/bạc/bạch kim) | STRING | TRUE | TRUE | TRUE |
| `purity` | Tuổi / Độ tinh khiết | STRING | TRUE | FALSE | TRUE |
| `weight_grams` | Trọng lượng (gram) | NUMBER | TRUE | FALSE | TRUE |
| `gem_type` | Loại đá quý | STRING | FALSE | TRUE | TRUE |
| `hallmark` | Dấu kiểm định | STRING | FALSE | TRUE | FALSE |
| `certificate_number` | Số chứng chỉ giám định | STRING | FALSE | TRUE | FALSE |
| `origin` | Xuất xứ trang sức | STRING | FALSE | TRUE | TRUE |
| `style` | Kiểu dáng | STRING | FALSE | TRUE | TRUE |
| `size` | Kích thước / Cỡ | STRING | FALSE | FALSE | TRUE |

**PAWN_SHOP (`ELECTRONICS`, `BIKE`, `JEWELRY`, `APPLIANCES` product types)**

ELECTRONICS — groups: `Thông tin thiết bị` · `Tình trạng & Phụ kiện`

| Code | Name (VI) | Type | required | searchable | filterable |
|------|-----------|------|----------|------------|------------|
| `brand` | Hãng sản xuất | STRING | TRUE | TRUE | TRUE |
| `model` | Model / Phiên bản | STRING | TRUE | TRUE | TRUE |
| `serial_number` | Số serial / IMEI | STRING | FALSE | TRUE | FALSE |
| `storage_capacity` | Dung lượng bộ nhớ | STRING | FALSE | FALSE | TRUE |
| `color` | Màu sắc | STRING | FALSE | FALSE | TRUE |
| `condition_grade` | Tình trạng (Mới/Tốt/Trung bình/Hỏng) | STRING | TRUE | FALSE | TRUE |
| `accessories_included` | Phụ kiện kèm theo | TEXT | FALSE | FALSE | FALSE |
| `warranty_status` | Tình trạng bảo hành | STRING | FALSE | FALSE | TRUE |
| `purchase_year` | Năm mua | NUMBER | FALSE | FALSE | TRUE |

BIKE — groups: `Thông tin xe` · `Tình trạng`

| Code | Name (VI) | Type | required | searchable | filterable |
|------|-----------|------|----------|------------|------------|
| `brand` | Hãng xe | STRING | TRUE | TRUE | TRUE |
| `model` | Model | STRING | TRUE | TRUE | TRUE |
| `engine_cc` | Dung tích động cơ (cc) | NUMBER | FALSE | FALSE | TRUE |
| `year_of_manufacture` | Năm sản xuất | NUMBER | TRUE | FALSE | TRUE |
| `license_plate` | Biển số xe | STRING | FALSE | TRUE | FALSE |
| `chassis_number` | Số khung | STRING | FALSE | TRUE | FALSE |
| `engine_number` | Số máy | STRING | FALSE | TRUE | FALSE |
| `condition_grade` | Tình trạng | STRING | TRUE | FALSE | TRUE |
| `color` | Màu sắc | STRING | FALSE | FALSE | TRUE |

**CONVENIENCE_STORE (`FOOD`, `BEVERAGE`, `CONVENIENCE`, `BEAUTY` product types)**

FOOD — groups: `Thông tin sản phẩm` · `Dinh dưỡng` · `Bảo quản`

| Code | Name (VI) | Type | required | searchable | filterable |
|------|-----------|------|----------|------------|------------|
| `brand` | Thương hiệu | STRING | FALSE | TRUE | TRUE |
| `package_size` | Dung tích / Trọng lượng | STRING | FALSE | TRUE | TRUE |
| `country_of_origin` | Xuất xứ | STRING | FALSE | TRUE | TRUE |
| `barcode` | Mã vạch | STRING | FALSE | TRUE | FALSE |
| `expiry_date` | Hạn sử dụng | DATE | FALSE | FALSE | TRUE |
| `storage_condition` | Bảo quản | STRING | FALSE | FALSE | TRUE |
| `ingredients` | Thành phần | TEXT | FALSE | TRUE | FALSE |

**ELECTRONICS shop type (`ELECTRONICS` product type)**

Groups: `Thông số kỹ thuật` · `Bảo hành & Xuất xứ` · `Tình trạng`

| Code | Name (VI) | Type | required | searchable | filterable |
|------|-----------|------|----------|------------|------------|
| `brand` | Hãng sản xuất | STRING | TRUE | TRUE | TRUE |
| `model` | Model | STRING | TRUE | TRUE | TRUE |
| `serial_number` | Số serial | STRING | FALSE | TRUE | FALSE |
| `cpu` | Bộ xử lý (CPU) | STRING | FALSE | TRUE | TRUE |
| `ram_gb` | RAM (GB) | NUMBER | FALSE | FALSE | TRUE |
| `storage_gb` | Bộ nhớ trong (GB) | NUMBER | FALSE | FALSE | TRUE |
| `screen_size` | Kích thước màn hình | STRING | FALSE | FALSE | TRUE |
| `color` | Màu sắc | STRING | FALSE | FALSE | TRUE |
| `warranty_months` | Bảo hành (tháng) | NUMBER | FALSE | FALSE | TRUE |
| `country_of_origin` | Xuất xứ | STRING | FALSE | TRUE | TRUE |
| `condition_grade` | Tình trạng | STRING | FALSE | FALSE | TRUE |

**FASHION (`CLOTHING` product type)**

Groups: `Thông tin sản phẩm` · `Kích thước & Màu sắc`

| Code | Name (VI) | Type | required | searchable | filterable |
|------|-----------|------|----------|------------|------------|
| `brand` | Thương hiệu | STRING | FALSE | TRUE | TRUE |
| `material` | Chất liệu | STRING | FALSE | TRUE | TRUE |
| `size` | Kích thước (S/M/L/XL) | STRING | TRUE | FALSE | TRUE |
| `color` | Màu sắc | STRING | TRUE | FALSE | TRUE |
| `gender` | Đối tượng (Nam/Nữ/Unisex) | STRING | FALSE | FALSE | TRUE |
| `season` | Mùa vụ | STRING | FALSE | FALSE | TRUE |
| `care_instruction` | Hướng dẫn giặt | TEXT | FALSE | FALSE | FALSE |
| `country_of_origin` | Xuất xứ | STRING | FALSE | TRUE | TRUE |

#### SQL pattern to use

```sql
-- Group
INSERT INTO `attribute_group` (`product_type_id`, `code`, `name`, `display_order`)
SELECT id, 'basic_info', 'Thông tin cơ bản', 1 FROM `product_type` WHERE code = 'DRUG'
ON DUPLICATE KEY UPDATE display_order = 1;

-- Definition
INSERT INTO `attribute_definition`
    (`product_type_id`, `attribute_group_id`, `code`, `name`, `data_type`,
     `required`, `searchable`, `filterable`, `display_order`)
SELECT pt.id, ag.id, 'drug_registration_number', 'Số đăng ký (SDK)', 'STRING', TRUE, TRUE, FALSE, 1
FROM `product_type` pt
JOIN `attribute_group` ag ON ag.product_type_id = pt.id AND ag.code = 'basic_info'
WHERE pt.code = 'DRUG'
ON DUPLICATE KEY UPDATE name = VALUES(name);
```

Always join on `ag.code`, never on `ag.name`. Vietnamese names in `name` column.

### Database Schema Management

DDL is managed manually (`spring.jpa.hibernate.ddl-auto=none`). Flyway runs numbered migrations (`src/main/resources/db/V0XX__*.sql`) against **tenant** databases only.

The master database is set up separately using two canonical files:
- `src/main/resources/db/master/ddl.sql` — master schema (tables, indexes)
- `src/main/resources/db/master/data.sql` — seed data: features (IDs `202601001`–`202601019`), roles (`MASTER_TENANT`, `VENDOR_ADMIN`), role-feature mappings, default admin user, bank list

When adding a column:
- Tenant DB: write a new `V0XX__*.sql` Flyway migration
- Master DB: update `db/master/ddl.sql` manually and run it

The `db/` folder also contains ad-hoc patch scripts (prefixed `master-*`) used for one-off data fixes — these are not applied automatically.

### Internationalization

All user-facing strings go through `MessageService` (wraps Spring `MessageSource`). Never hardcode error messages. Locale files:
- `src/main/resources/i18n/messages.properties` (English)
- `src/main/resources/i18n/messages_vi.properties` (Vietnamese)

### Audit Logging

`ApiAuditLogService` logs every API request/response to the `api_audit_log` table with trace ID, execution time, status, and sanitized headers (sensitive headers are redacted). `ApiAuditLogCleanupTask` (scheduled) purges old logs.

### Sensitive Field Encryption

`EncryptedStringConverter` (JPA `AttributeConverter`) transparently encrypts/decrypts fields like e-invoice credentials using AES-256. Requires `APP_ENCRYPTION_KEY` env var (Base64-encoded 32-byte key). Generate: `openssl rand -base64 32`.

## Key Configuration

| Property | Default | Note |
|----------|---------|------|
| `jwt.secret` | hardcoded default | Override in prod |
| `jwt.expiration` | 86400000 (24h) | Access token TTL ms |
| `jwt.refresh-expiration` | 604800000 (7d) | Refresh token TTL ms |
| `APP_ENCRYPTION_KEY` | empty | Required for e-invoice features |
| `system.corsAllowedOrigin` | `https://quanlycuahang.com/` | CORS origin |

## Testing

- **Unit tests**: JUnit 5 + Mockito (`@ExtendWith(MockitoExtension.class)`) for services, utilities, config.
- **Repository tests**: `@DataJpaTest` with H2 in-memory database.
- JaCoCo excludes `model/**`, `controller/**`, `util/**` from coverage.

Tests live under `src/test/java/com/knp/` mirroring the main package structure.
