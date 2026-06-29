-- V009: Remove the direct Zalo integration.
--
-- OTP and reminders are now sent by the central Tappy Message service (msg.tappy.vn), which owns the
-- Zalo OA credentials and the message audit log. POS no longer stores any Zalo credential or template,
-- so the two tables that backed the old integration are dropped:
--   * zalo_zns_credential   — master table holding the platform OA token (V002)
--   * zalo_message_templates — per-tenant ZNS template IDs + RLS policy (V001)
--
-- Dropping a table also drops its indexes and RLS policies, so no separate DROP POLICY is needed.
--
-- Intentionally KEPT (these are plain contact / bookkeeping columns, not part of the integration):
--   * customers.zalo_id, tenants.contact_person_zalo_id  — contact handles
--   * appointments.reminder_sent                          — guards against re-sending a reminder
--   * password_reset_otps.zns_message_id                  — historical delivery id (now unused, harmless)

DROP TABLE IF EXISTS zalo_message_templates;
DROP TABLE IF EXISTS zalo_zns_credential;
