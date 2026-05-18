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

**Multi-tenant Spring Boot 3.3.5 / Java 21 REST API** with shared-database + Row-Level Security isolation. All tenants share a single PostgreSQL database (`retail-platform`). Master tables (tenants, users, roles, features, agents, banks) are unfiltered; tenant tables (products, inventory, orders, customers, …) have `FORCE ROW SECURITY` policies that restrict every query to rows where `tenant_id = current_setting('app.current_tenant', true)`.

### Multi-Tenancy Request Flow

Every request carries an `X-Tenant-ID` header:

1. `TenantInterceptor` validates the header. Public paths (`/api/tenants`, `/api/swagger-ui`, `/api/v3/api-docs`, `/actuator`) skip validation. Flexible paths (`/api/auth`, `/api/users`, `/api/employees`, `/api/multi-tenants`, `/api/profiles`) work with or without the header.
2. When a tenant ID is present and valid, `TenantInterceptor` calls `tenantContext.setCurrentTenant(tenant)`.
3. `TenantContext` stores the `Tenant` entity in a `ThreadLocal` and writes `tenantId` to MDC for logging.
4. `TenantRlsAspect` fires at `@Transactional` boundaries and executes `SET LOCAL app.current_tenant = '<tenantId>'` on the JDBC connection so PostgreSQL RLS policies automatically scope every subsequent query in that transaction to the current tenant.
5. `TenantInterceptor.afterCompletion` always calls `tenantContext.clear()` to prevent cross-request leakage.

The `X-Tenant-ID: master` special value leaves tenant context empty, disabling RLS on tenant tables and giving access to master tables only. Two AOP annotations enforce access:

> **Critical gotcha — `current_tenant_id()` in auth flows:**
> `TenantRlsAspect` sets `app.current_tenant` once at `@Transactional` start. If a method begins with no tenant context (e.g. login without `X-Tenant-ID`) and then calls `tenantContext.setCurrentTenant(tenant)` mid-method, the DB session variable remains NULL for the rest of that transaction — even though `tenantContext.getCurrentTenant()` now returns non-null. Any query using `current_tenant_id()` or `r.tenant_id IS NOT DISTINCT FROM current_tenant_id()` will still see NULL and return wrong results (master rows only, not shop rows). **Rule:** queries called from auth flows (login, refresh, onboarding) that need tenant-scoped data must use an explicit `tenantId` bind parameter instead of `current_tenant_id()`. See `RoleFeatureRepository.findActiveFeatureNamesByRoleNamesAndTenantId()` as the reference implementation.
- `@MasterDatabaseOnly` — endpoint requires no tenant context + `MASTER_TENANT` role + `isMasterUser=true` (used on master-admin controllers like `MultiTenantController`, `FeedbackController` admin endpoints).
- `@RequiresFeature("NAME")` — endpoint requires the named feature in the caller's JWT (used on all shop and shared controllers). See Feature Access Model below.

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
- `@RequiresFeature` accepts a single string **or an array** — `@RequiresFeature({"POS", "CUSTOMER"})` grants access when the user has **any** of the listed features (OR logic).
- **MASTER_TENANT** role features: `[USER, TENANT_MGMT, AGENT_MGMT, ACTIVITY_LOG, FEEDBACK_MGMT, MASTER_DASHBOARD, NOTIFICATION, CONTACT_LEAD_MGMT, PRODUCT_CATALOG]`
- **AGENT** role features: `[TENANT_MGMT, MASTER_DASHBOARD, NOTIFICATION]` — agents see shops and the dashboard but cannot access product catalog, feedback management, or contact leads.
- Master-only endpoints (tenant provisioning, feedback admin, contact leads) use `@MasterDatabaseOnly` instead — checks `MASTER_TENANT` role + `isMasterUser` flag, not features.

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
| `AGENT_MGMT` | `AgentController` (master-only; manages platform agents) |
| `VENDOR` | `VendorController`, `PurchaseOrderController` |
| `PAWN` | `PawnController`, `BuybackOrderController`, `MarketPriceController`, `GoldPriceController` |
| `ACTIVITY_LOG` | `ActivityLogController` |
| `NOTIFICATION` | `NotificationController` — includes `GET/PUT /notifications/preferences` for per-user type opt-in |
| `FEEDBACK` | `FeedbackController` (shop-user submit methods only; admin review methods use `@MasterDatabaseOnly`) |
| `MASTER_DASHBOARD` | `MasterDashboardController` (MASTER_TENANT + AGENT) |
| `PRODUCT_CATALOG` | `ProductCatalogController` (MASTER_TENANT only; AGENT excluded) |
| `CONTACT_LEAD_MGMT` | `ContactController` admin methods use `@MasterDatabaseOnly` (MASTER_TENANT only) |
| `FEEDBACK_MGMT` | `FeedbackController` admin methods use `@MasterDatabaseOnly` (MASTER_TENANT only) |

**Do NOT use `@PreAuthorize`** — `@EnableMethodSecurity` is not configured; these annotations are silently ignored.

### Granular Sub-Feature Pattern (row-level access control within a module)

Some features need **within-module** access differentiation — e.g., all staff can access the Order module, but only shop owners should see every employee's orders. The standard approach is a **sub-feature flag** that gates the broader view; absence of the flag restricts to the user's own data.

**Pattern:** `<MODULE>_VIEW_ALL` (or `<MODULE>_MANAGE_ALL` for write operations)

**Implemented example: `ORDER` + `ORDER_VIEW_ALL`**
- `ORDER` — required to open the Order module at all (route guard + `@RequiresFeature`)
- `ORDER_VIEW_ALL` — within the module, determines query scope:
  - Present → `findAllActive()` / `searchByKeyword()` — all tenant orders visible
  - Absent → `findAllActiveByCreatedBy()` / `searchByKeywordAndCreatedBy()` — only `created_by = currentUsername`
  - `getOrderById()` also enforces ownership when flag is absent (throws 404, not 403, to avoid leaking existence)

**Backend implementation checklist** (service layer, never controller):
1. Add the sub-feature to `FeatureEnum` with a Vietnamese name and description.
2. Add a Flyway migration (`V0XX__...sql`) to insert it into the `features` table.
3. Inject `FeatureContext` into the service (it is already a `@Component` bean).
4. Branch on `featureContext.hasFeature("FEATURE_VIEW_ALL")` — call the scoped repository method when flag is absent.
5. Add scoped repository queries (e.g. `findAllActiveByCreatedBy`, `findAllActiveByStatusAndCreatedBy`, `searchByKeywordAndCreatedBy`).
6. For single-resource fetch (`getById`), throw `ResourceNotFoundException` (same as "not found") rather than 403 — never expose that the record exists but is owned by someone else.

**Master admin setup:** assign the sub-feature to the shop at tenant creation; shop owner assigns it to the SHOP_OWNER role in `role_features`. Staff roles (CASHIER, etc.) do not get it.

**Never** branch on role name, `tenantId`, or any heuristic — always read from `FeatureContext`. This keeps the auth model consistent: token is the single source of truth.

**Apply this pattern to other modules** whenever you need the same owner-vs-all split (e.g. `EXPENSE_VIEW_ALL`, `SALARY_VIEW_ALL`).

### Async Side-Effect Pattern

`@EnableAsync` is active. Any operation that is a side effect of the main flow (notifications, audit log) must be **fire-and-forget** — it must never block or fail the caller.

**Rules:**
1. Annotate with `@Async` on the method (not the class).
2. Pass all required context (e.g. `tenantId`, `username`) as method parameters — `TenantContext` and `SecurityContextHolder` are ThreadLocal and are **not** inherited by the async thread.
3. Catch and log all exceptions internally — never let them propagate to the caller.
4. If the async method needs a DB write in tenant context, call `tenantContext.setCurrentTenant(...)` at the start and `tenantContext.clear()` in `finally`.
5. **If the enclosing service class carries `@Transactional(readOnly = true)` at the class level**, also annotate the `@Async` method with `@Transactional(propagation = Propagation.NOT_SUPPORTED)`. Spring's AOP proxy includes the `TransactionInterceptor` in the lambda captured by the async thread pool, so the class-level `readOnly=true` transaction would be applied in the new thread — preventing any `INSERT`/`UPDATE`. `NOT_SUPPORTED` overrides this: no outer transaction starts, and each downstream repository call manages its own writable transaction independently. `REQUIRES_NEW` is **not** the right fix here — it would open a writable transaction before `TenantContext` is set, breaking RLS.

**Existing implementations:**
- `ActivityLogServiceImpl.logAsync(tenantId, ...)` — audit writes; sets its own TenantContext in the async thread.
- `NotificationService.pushToMasterUsersAsync/pushToRolesAsync(...)` — both annotated `@Transactional(propagation = Propagation.NOT_SUPPORTED)` to override the class-level `readOnly=true`; called from `MultiTenantController` after tenant creation.

When adding new side-effects, follow this same pattern.

### Activity Logging — Coverage & Conventions

Every mutating service method must call `activityLogService.logAsync(...)` after a successful save. The call is fire-and-forget; exceptions are swallowed inside `logAsync`.

**Signature:**
```java
activityLogService.logAsync(tenantId, actorUsername, actorFullName, action, targetType, targetId, description, ipAddress);
```

**`tenantId` rules — critical for RLS correctness:**
- Shop operations: pass `tenantContext.getCurrentTenantId()` — stored as the actual tenant ID in the DB.
- Master/agent operations: pass `"master"` — `ActivityLogServiceImpl` translates this to `NULL` before saving. This is intentional: the `activity_log` RLS policy uses `IS NOT DISTINCT FROM current_tenant_id()`, so master-context queries (where `current_tenant_id()` = NULL) correctly see `tenant_id IS NULL` rows.
- Never pass `null` directly — the method returns early if `tenantId == null`.

**Capture `actorUsername` before any async boundary:**
```java
// In the calling service (main thread, SecurityContextHolder is populated)
String actor = SecurityContextHolder.getContext().getAuthentication().getName();
activityLogService.logAsync(tenantContext.getCurrentTenantId(), actor, null, ...);
```
For services that use `AuthContext`, use `authContext.getCurrentUsername()` instead.

**Checklist when adding a new service method:**
1. Add the action to `ActivityAction` enum if it doesn't exist.
2. Call `logAsync` after the successful repository save (not before, not in a finally block).
3. Use Vietnamese for `description` — it appears directly in the UI.
4. Add the action to `ACTION_OPTIONS` and `ACTION_COLORS` in `ActivityLogPage.jsx` (frontend).
5. Add Vietnamese + English translations in `src/i18n/features/activityLog.js`.

**Currently logged actions by module:**

| Module | Actions |
|--------|---------|
| Auth | `LOGIN`, `LOGOUT` (shop + master + agent) |
| Master: Tenants | `TENANT_CREATED`, `TENANT_UPDATED` |
| Master: Agents | `AGENT_CREATED`, `AGENT_UPDATED` |
| Master: Feedback | `FEEDBACK_REVIEWED` |
| Orders (via Cart checkout) | `ORDER_CREATED`, `ORDER_COMPLETED`, `ORDER_CANCELLED`, `ORDER_VOIDED` |
| Products | `PRODUCT_CREATED`, `PRODUCT_UPDATED`, `PRODUCT_DELETED` |
| Customers | `CUSTOMER_CREATED`, `CUSTOMER_UPDATED` |
| Employees | `EMPLOYEE_CREATED`, `EMPLOYEE_UPDATED` |
| Vendors | `VENDOR_CREATED`, `VENDOR_UPDATED` |
| Purchase Orders | `PURCHASE_ORDER_CREATED`, `PURCHASE_ORDER_RECEIVED` |
| Inventory | `INVENTORY_ADJUSTED` |
| Expenses | `EXPENSE_CREATED` |
| Pawn | `PAWN_CREATED`, `PAWN_UPDATED`, `PAWN_DELETED`, `PAWN_CANCEL`, `PAWN_FORFEIT`, `PAWN_REDEEMED`, `PAWN_EXTEND`, `PAWN_REQUEST_MONEY` |
| Users | `USER_CREATED` |

### Product System (EAV)

Products use Entity-Attribute-Value to support 18+ types (Food, Beverage, Drug, Electronics, etc.) without schema changes:
`ProductType` → `AttributeGroup` → `AttributeDefinition` → `ProductAttributeValue`

Base `Product` holds SKU, price, cost, unit, vendor, shelf location, and status. Dynamic attributes live in `ProductAttributeValue`.

#### Dynamic-Price Product Types

`DynamicPriceProductTypes` (`com.knp.model.enums`) — currently `CODES = Set.of("JEWELRY")`.

For these types the `price` column on `Product` is stored as `0`. The real sell price is computed at cart-add time in `CartServiceImpl.calculateDynamicUnitPrice()`:

```
unitPrice = gold_weight (EAV) × GoldPriceService.getPriceForCategory(categoryId).sell
          + sell_proc_price (EAV, falls back to proc_price)
```

`CartServiceImpl.addItemToCart()` resolves `unitPrice` before building the `CartItemEntity`:
```java
BigDecimal resolvedUnitPrice = DynamicPriceProductTypes.isDynamicPrice(productTypeCode)
    ? calculateDynamicUnitPrice(product)
    : product.getPrice();
```

Throws `BadRequestException` (with Vietnamese message) if the product has no category or no gold price is configured for that category.

**To add a new dynamic-price type:** add its code to `DynamicPriceProductTypes.CODES`; extend `calculateDynamicUnitPrice()` if the formula differs from gold.

### Tenant Provisioning

`POST /api/multi-tenants` (master-only) calls `TenantProvisioningService.provision()` which:
1. Inserts a row into the `tenants` master table.
2. Sets `TenantContext` to the new tenant ID so RLS scopes subsequent writes to that tenant.
3. Executes the shop-type DML file (e.g. `pawn_shop.sql`) — this uses `current_setting('app.current_tenant', true)` as the `tenant_id` value so all seed rows land in the correct tenant's partition.
4. Seeds roles, role-feature mappings, shop info, default walk-in customer (`phone=0000000000`, name `"Khách lẻ"`), default shop configs (`DEFAULT_TAX_RATE`, `POS_MODE`), and admin user via `TenantProvisioningService` — these are **not** in the DML files.

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
1. Shop info          — INSERT INTO shop_info (shop_name, ...) ON CONFLICT DO NOTHING
2. Product types      — INSERT INTO product_type ... ON CONFLICT (code) DO UPDATE
3. Categories         — INSERT INTO category ... ON CONFLICT (name, tenant_id) DO UPDATE
4. Vendors (optional) — 1–2 placeholder suppliers relevant to the shop type
5. Sample products    — 15–40 realistic items the shop actually sells
6. Product→category   — INSERT INTO product_category ... ON CONFLICT DO NOTHING
7. Inventory          — INSERT INTO inventory for every product
8. Loyalty program    — single default program + 4 tiers
9. Print template     — RECEIPT template with ON CONFLICT DO NOTHING
10. Attribute groups  — per product type, Vietnamese names, display_order
11. Attribute defs    — per group, Vietnamese names (see below)
12. Shop config       — cash_denominations (all shops); pawn_* keys (pawn/jewelry only)
```

**Do NOT add walk-in customer to DML files.** It is seeded exclusively by `TenantProvisioningService.seedWalkInCustomer()` in Java so each tenant gets an isolated row with correct RLS scoping.

Banks are seeded globally in `V001__initial_schema.sql` — do **not** add bank INSERTs to DML files.

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
INSERT INTO attribute_group (product_type_id, code, name, display_order)
SELECT id, 'basic_info', 'Thông tin cơ bản', 1 FROM product_type WHERE code = 'DRUG'
ON CONFLICT (product_type_id, code) DO UPDATE SET display_order = EXCLUDED.display_order;

-- Definition
INSERT INTO attribute_definition
    (product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT pt.id, ag.id, 'drug_registration_number', 'Số đăng ký (SDK)', 'STRING', TRUE, TRUE, FALSE, 1
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'basic_info'
WHERE pt.code = 'DRUG'
ON CONFLICT (product_type_id, code) DO UPDATE SET name = EXCLUDED.name;
```

Always join on `ag.code`, never on `ag.name`. Vietnamese names in `name` column. Use PostgreSQL `ON CONFLICT … DO UPDATE/DO NOTHING` — never MySQL's `ON DUPLICATE KEY UPDATE`.

### External / Legacy ID Convention

Every major tenant entity table has a `legacy_id VARCHAR(50) DEFAULT NULL` column. Its purpose is to store the original primary key from **any external or legacy system** — the old platform's integer ID, a third-party UUID, an ERP code, or any opaque string key we don't know the shape of yet.

**Design decisions:**
- `VARCHAR(50)` instead of `BIGINT` — accepts integers, UUIDs, and arbitrary string keys without a schema change when a new source system appears.
- `DEFAULT NULL` — existing rows are unaffected; only migrated or synced rows carry a value.
- **Partial index** on `(tenant_id, legacy_id) WHERE legacy_id IS NOT NULL` — efficient reverse-lookup (old ID → new UUID) with no overhead on the majority of rows that have no legacy ID.

**Tables that already have this column:**

| Table | Index |
|---|---|
| `product` | `idx_product_legacy_id` |
| `customers` | `idx_customers_legacy_id` |
| `orders` | `idx_orders_legacy_id` |
| `employees` | `idx_employees_legacy_id` |
| `pawn` | `idx_pawn_legacy_id` |
| `pawn_req_money` | `idx_pawn_req_money_legacy_id` |

**When to add `legacy_id` to a new table:**
1. Add `legacy_id VARCHAR(50) DEFAULT NULL` in a new Flyway migration.
2. Add the partial index: `CREATE INDEX IF NOT EXISTS idx_<table>_legacy_id ON <table> (tenant_id, legacy_id) WHERE legacy_id IS NOT NULL;`
3. Add `@Column(name = "legacy_id", length = 50) private String legacyId;` to the entity class.
4. Expose it in the DTO if the migration script or import API needs to read it back.

**Usage pattern in migration scripts:** populate `legacy_id` during the INSERT/UPDATE that creates the new-platform row. After migration, use `SELECT id FROM <table> WHERE tenant_id = ? AND legacy_id = ?` to resolve old IDs to new UUIDs in any cross-reference joins.

### Database Schema Management

DDL is managed by Flyway (`spring.jpa.hibernate.ddl-auto=none`). There is a **single shared PostgreSQL database** (`retail-platform`) — no per-tenant databases.

`V001__initial_schema.sql` is the canonical bootstrap migration. It runs automatically at Spring Boot startup on a fresh install and creates **everything** in one pass:
- All master tables: `tenants`, `users`, `roles`, `features`, `role_features`, `agents`, `banks`, `active_sessions`, …
- All tenant tables with RLS: `products`, `inventory`, `orders`, `customers`, `shop_config`, `shop_info`, …
- All RLS policies (`CREATE POLICY … USING (tenant_id = current_setting('app.current_tenant', true))`)
- All seed data: features (`AGENT_MGMT`, `TENANT_MGMT`, …), roles (`MASTER_TENANT`, `AGENT`), role-feature mappings, default admin user, full Vietnamese bank list (49 banks)

When adding a column or table:
- Write a new numbered `V0XX__*.sql` Flyway migration — applies to both master and tenant tables in the shared DB.
- Never manually patch the DB without a corresponding migration file.

**`src/main/resources/db/` folder layout:**

| Path | Purpose |
|------|---------|
| `db/migration/V001__initial_schema.sql` | Flyway bootstrap — all DDL + master seed data (runs automatically) |
| `db/migration/V0XX__*.sql` | Subsequent Flyway migrations for schema changes |
| `db/tenant/general.sql` | Default seed data for generic shop types (product types, categories, sample products, loyalty, print template, shop config) |
| `db/tenant/pawn_shop.sql` | Seed data for pawn shops — includes pawn-specific product types, attributes, and pawn config keys |
| `db/tenant/convenience_store.sql` | Seed data for convenience stores |
| `db/tenant/jewelry.sql` | Seed data for jewelry/gold shops — includes jewelry attributes and pawn config keys |
| `db/*.sql` (prefixed `master-*`) | Ad-hoc one-off data patches for existing deployments — not applied automatically; run manually via psql |

The `db/tenant/` files are executed by `TenantProvisioningService` at shop creation time (not by Flyway). Each file uses `current_setting('app.current_tenant', true)` as the `tenant_id` so all seed rows are automatically scoped to the new tenant via RLS.

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
| `jwt.refresh-expiration` | 2592000000 (30d) | Refresh token TTL ms |
| `APP_ENCRYPTION_KEY` | empty | Required for e-invoice features |
| `system.corsAllowedOrigin` | `https://quanlycuahang.com/` | CORS origin |

## Timezone

All timestamps are stored and served in **Asia/Ho_Chi_Minh (UTC+7)**. This is enforced at four layers — do not change any of them independently or timestamps will become inconsistent:

| Layer | Where set | What it controls |
|-------|-----------|-----------------|
| JVM default | `RetailPlatformApplication.main()` — `TimeZone.setDefault("Asia/Ho_Chi_Minh")` before Spring starts | `@CreationTimestamp`, `@UpdateTimestamp`, `LocalDateTime.now()` |
| Hibernate JDBC | `spring.jpa.properties.hibernate.jdbc.time_zone=Asia/Ho_Chi_Minh` in `application.properties` | How Hibernate serialises `java.time.*` values onto the JDBC wire |
| Jackson | `spring.jackson.time-zone=Asia/Ho_Chi_Minh` in `application.properties` | `LocalDateTime` fields in JSON API responses |
| Docker image | `ENV TZ="Asia/Ho_Chi_Minh"` in `backend/Dockerfile` | OS timezone inside the container (also sets JVM `TZ` env var) |
| PostgreSQL container | `TZ: Asia/Ho_Chi_Minh` in `docker-compose.yml` (both dev and prod) | `NOW()`, `CURRENT_TIMESTAMP`, `DEFAULT NOW()` in raw SQL |

When running locally outside Docker (`mvn spring-boot:run`), the `TimeZone.setDefault()` in `main()` acts as a safety net so the JVM uses UTC+7 regardless of the developer's machine timezone.

## Testing

- **Unit tests**: JUnit 5 + Mockito (`@ExtendWith(MockitoExtension.class)`) for services, utilities, config.
- **Repository tests**: `@DataJpaTest` with H2 in-memory database.
- JaCoCo excludes `model/**`, `controller/**`, `util/**` from coverage.

Tests live under `src/test/java/com/knp/` mirroring the main package structure.
