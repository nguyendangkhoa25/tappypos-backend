# Release Notes — TappyPOS Backend

## [1.0.0] — 2026-04-28

### 🚀 Initial Production Release

First public release of the TappyPOS retail platform backend.

### Features

**Multi-Tenant Architecture**
- Database-per-tenant isolation via `RoutingDataSource` and `TenantContext`
- Runtime tenant provisioning without server restart
- Master database for tenant/user management; separate DB per retailer
- `@MasterDatabaseOnly` AOP guard for super-admin endpoints

**Authentication & Session Management**
- JWT access token (short-lived) + HttpOnly refresh token cookie
- Single-device enforcement — second login returns `409 DEVICE_CONFLICT` with existing session info
- Force-login flow to evict an existing session
- Account lock after 5 consecutive failed login attempts
- Token refresh recomputes feature flags at refresh time (no re-login needed for plan changes)

**Role-Based Access Control**
- Feature flags embedded in JWT payload (intersection of tenant features × role features)
- Master tenant (`MASTER_TENANT` role) for super-admin operations

**Product Management**
- Entity-Attribute-Value (EAV) system supporting 18+ product types
- `ProductType` → `AttributeGroup` → `AttributeDefinition` → `ProductAttributeValue`
- Soft delete on products and attribute values

**Inventory Management**
- Stock tracking with `IN` / `OUT` / `ADJUSTMENT` transaction types
- Low stock, expiring, and expired item alerts
- Reorder level tracking with automatic LOW_STOCK notifications

**Point of Sale (POS)**
- Cart management with product lookup and quantity adjustment
- Order creation with cash/transfer payment support
- Order cancellation and void flows with audit trail
- Table label support for restaurant/café mode

**Customer & Loyalty**
- Customer profiles with identity card fields
- Loyalty points system — earn on purchase, redeem at checkout

**Promotions**
- Discount rules applied at order creation

**Invoicing**
- E-invoice generation with encrypted credential storage (AES-256)

**Vendor & Purchase Orders**
- Vendor management linked to products
- Purchase order tracking

**Employee Management**
- Employee records linked to tenant users

**Activity Log**
- Full audit trail of user actions (login, orders, product/customer changes)
- Filterable by user, action type, and date range

**Notifications**
- In-app notification system (LOW_STOCK and custom types)

**API Infrastructure**
- Spring Boot 3.3.5 / Java 21
- All responses via `ApiResponse<T>` envelope
- `GlobalExceptionHandler` with consistent error codes
- Request/response audit logging with sanitized headers
- Scheduled cleanup of old audit logs
- Swagger UI at `/api/swagger-ui.html`
- Actuator health at `/api/actuator/health` (internal only)

**Internationalisation**
- Vietnamese (default) and English via `MessageSource`
- Locale driven by `Accept-Language` request header

---

## Upgrade Notes

_Not applicable — initial release._

---

## Known Limitations

See `MISSING_FEATURES.md` at the repo root for features with UI stubs but no backend implementation:
Revenue/Reporting, Employee Salary/Payroll, Promotions (full engine), My Work (staff view), Product Variants.

---

<!-- Template for future releases:

## [X.Y.Z] — YYYY-MM-DD

### Breaking Changes
-

### Features
-

### Bug Fixes
-

### Performance
-

### Security
-

### Dependencies
-

-->
