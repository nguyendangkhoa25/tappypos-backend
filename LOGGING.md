# Tenant-Specific Logging

## Overview
Each tenant gets its own log folder with daily rotation. Logs are separated to prevent any data mixing between tenants.

## Log Structure
```
logs/
├── app.log                          # Non-tenant requests only
├── app-2026-01-02.log              # Archived non-tenant logs
├── tenant-phuc-barber/
│   ├── app.log                      # Current day (all levels)
│   ├── app-2026-01-01.log          # Archived
│   └── app-2026-01-02.log
└── tenant-another-shop/
    ├── app.log
    └── app-2026-01-01.log
```

## Features
- ✅ **Tenant Isolation**: Each tenant has separate log folder
- ✅ **No Duplicates**: Logs only written to tenant file OR main file, never both
- ✅ **Daily Rotation**: Automatic rotation at midnight
- ✅ **30-Day Retention**: Old logs auto-deleted after 30 days
- ✅ **1GB Limit**: Per-tenant size limit

## How It Works

### Filters
- **TenantLogFilter**: Only allows logs with valid tenant ID → tenant-specific files
- **NonTenantLogFilter**: Only allows logs without tenant ID → main app.log

### MDC (Mapped Diagnostic Context)
- Tenant ID automatically added to MDC by `TenantInterceptor`
- Visible in log pattern: `[tenantId]`
- Used by filters to route logs correctly

## Examples

### View tenant logs
```bash
# Current logs
tail -f logs/tenant-phuc-barber/app.log

# Archived logs
cat logs/tenant-phuc-barber/app-2026-01-02.log

# Search errors
grep "ERROR" logs/tenant-phuc-barber/*.log
```

### Monitor all tenants
```bash
# Watch all tenant logs
tail -f logs/tenant-*/app.log

# Count errors per tenant
for dir in logs/tenant-*/; do
  tenant=$(basename "$dir")
  errors=$(grep -c "ERROR" "$dir/app.log" 2>/dev/null || echo 0)
  echo "$tenant: $errors errors"
done
```

## Configuration

Edit `logback-spring.xml` to adjust:
- **Retention**: `<maxHistory>30</maxHistory>` (days)
- **Size Limit**: `<totalSizeCap>1GB</totalSizeCap>`
- **Log Pattern**: `<property name="LOG_PATTERN" .../>`

