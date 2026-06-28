-- V006: render-at-read i18n key+args columns for previously frozen localized text.
-- These tenant tables already exist (with RLS) since V001, so ADD COLUMN needs no new RLS policy.

-- Appointment pickup-reminder note (AppointmentServiceImpl.createPickupReminder)
ALTER TABLE appointments
    ADD COLUMN note_key VARCHAR(150),
    ADD COLUMN note_args TEXT;

-- Repair warranty-claim reported fault + note (RepairTicketServiceImpl.createWarrantyClaim)
ALTER TABLE repair_tickets
    ADD COLUMN reported_fault_key VARCHAR(150),
    ADD COLUMN reported_fault_args TEXT,
    ADD COLUMN note_key VARCHAR(150),
    ADD COLUMN note_args TEXT;
