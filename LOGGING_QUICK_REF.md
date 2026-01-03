# Quick Reference - Tenant Logging

## What Changed?
✅ Fixed duplicate logging issue
✅ Tenant logs → ONLY in `tenant-{id}/app.log`
✅ Non-tenant logs → ONLY in `app.log`
✅ Daily rotation at midnight
✅ 30-day retention

## Log Locations

| Request Type | Log File |
|-------------|----------|
| With `X-Tenant-ID: phuc-barber` | `logs/tenant-phuc-barber/app.log` |
| With `X-Tenant-ID: shop-a` | `logs/tenant-shop-a/app.log` |
| Public endpoints (no header) | `logs/app.log` |
| Auth without tenant | `logs/app.log` |

## Quick Commands

```bash
# View tenant logs
tail -f logs/tenant-phuc-barber/app.log

# View all tenants
tail -f logs/tenant-*/app.log

# Count errors
grep -c "ERROR" logs/tenant-phuc-barber/app.log

# View main app log
tail -f logs/app.log

# List all log files
find logs -name "*.log" -type f

# Check log sizes
du -sh logs/tenant-*/
```

## After Restart

1. Old logs remain (until 30-day retention expires)
2. New logs will be correctly separated
3. No more duplicates
4. `tenant-default` folder won't be created anymore

## Need Help?

See detailed docs:
- `LOGGING.md` - Overview and usage
- `LOGGING_IMPLEMENTATION.md` - Technical details and testing

