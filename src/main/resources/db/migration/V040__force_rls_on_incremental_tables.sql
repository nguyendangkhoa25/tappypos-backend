-- V040: Add FORCE ROW LEVEL SECURITY to incremental tenant tables that enabled RLS but omitted FORCE.
--
-- Every V001 tenant table (and V003/V029/V035) uses both ENABLE and FORCE ROW LEVEL SECURITY.
-- The incremental migrations below enabled RLS but never FORCEd it. Without FORCE, the policies are
-- bypassed for the table-owner role. Today the app connects as the `postgres` superuser (which bypasses
-- RLS regardless), so this is not currently exploitable — but the project's standard is FORCE everywhere
-- so isolation holds the day a non-superuser owner role is adopted. This migration closes that gap.
-- (tenant-isolation-reviewer HIGH finding, 2026-06-21 audit.)

ALTER TABLE repair_tickets          FORCE ROW LEVEL SECURITY;
ALTER TABLE repair_parts            FORCE ROW LEVEL SECURITY;
ALTER TABLE modifier_groups         FORCE ROW LEVEL SECURITY;
ALTER TABLE modifier_options        FORCE ROW LEVEL SECURITY;
ALTER TABLE product_modifier_groups FORCE ROW LEVEL SECURITY;
ALTER TABLE gold_price_history      FORCE ROW LEVEL SECURITY;
ALTER TABLE table_reservations      FORCE ROW LEVEL SECURITY;
ALTER TABLE customer_debt           FORCE ROW LEVEL SECURITY;
ALTER TABLE debt_payment            FORCE ROW LEVEL SECURITY;
ALTER TABLE booking_resource_rate   FORCE ROW LEVEL SECURITY;
ALTER TABLE vehicle_unit            FORCE ROW LEVEL SECURITY;
ALTER TABLE trade_in                FORCE ROW LEVEL SECURITY;
ALTER TABLE installment_schedule    FORCE ROW LEVEL SECURITY;
