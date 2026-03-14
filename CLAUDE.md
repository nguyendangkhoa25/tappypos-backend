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

The `X-Tenant-ID: master` special value routes to the master database without tenant validation. `@MasterDatabaseOnly` + `MasterDatabaseAccessAspect` (AOP) enforces that annotated endpoints are only callable with no tenant context and `MASTER_TENANT` role.

`DatasourceManager` supports **runtime add/remove** of tenant datasources without restarting — used when provisioning new tenants (`TenantProvisioningService`).

### Auth & Session Model

- Login (`POST /api/auth/login`) returns a short-lived JWT (access token in body) and optionally a `refresh-token` HttpOnly cookie when `rememberMe=true`.
- The JWT payload carries `roles`, `features` (intersection of tenant-assigned features and role-assigned features), `isMasterUser`, and `sessionId`.
- **Single-device enforcement**: `SessionRegistry` (in-memory `ConcurrentHashMap`, keyed by `tenantKey → username → SessionInfo`) allows only one active session per user per tenant. A second login from a different device throws `DeviceConflictException` (HTTP 409) with the existing session's IP, user-agent, and login time. The client can force-login (`POST /api/auth/force-login`) to evict the existing session.
- Sessions are also persisted to `active_sessions` table (best-effort, for audit and restart recovery — the in-memory map is authoritative).
- Account is locked after 5 consecutive failed login attempts (`failedLoginAttempts` column on `users`).
- Token refresh (`POST /api/auth/refresh`) re-computes the feature intersection at refresh time so plan changes take effect without re-login.

### Feature Access Model

Features are gated at two levels stored in the **master database**:
1. **Tenant features** — comma-separated string on the `Tenant.features` column (e.g., `"DASHBOARD,ORDER,PRODUCT"`).
2. **Role features** — `role_features` join table linking `roles` to `features`.

`TenantFeatureService.getAccessibleFeaturesByRoleAndTenant()` returns the **intersection** of both sets and embeds it in the JWT. This means tenant subscription changes only take effect after the next token refresh.

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

### Database Schema Management

DDL is managed manually (`spring.jpa.hibernate.ddl-auto=none`). Flyway runs numbered migrations (`src/main/resources/db/V0XX__*.sql`) against **tenant** databases. The master database is set up separately using:
- `master-database-schema.sql` — table definitions
- `master-database-default-data.sql` — seed data
- `master-setup-roles-and-features.sql` — roles/features bootstrap

When adding a column, write a new `V0XX__*.sql` migration for tenant DBs and update `master-database-schema.sql` manually for the master DB.

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
