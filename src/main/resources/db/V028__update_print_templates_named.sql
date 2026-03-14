-- Allow multiple templates per type (named templates with one default per type).
-- Run against every tenant database.

-- 1. Drop the old single-column unique constraint
ALTER TABLE print_templates DROP INDEX template_type;

-- 2. Add name and is_default columns
ALTER TABLE print_templates
    ADD COLUMN name       VARCHAR(100) NOT NULL DEFAULT 'Mặc định' AFTER template_type,
    ADD COLUMN is_default TINYINT(1)  NOT NULL DEFAULT 1           AFTER name;

-- 3. Existing rows already have is_default = 1 (the default above covers it).
--    Add the new composite unique constraint.
ALTER TABLE print_templates
    ADD UNIQUE KEY uq_print_templates_type_name (template_type, name);
