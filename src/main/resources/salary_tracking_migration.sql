-- SQL Migration Script for Salary Calculation Tracking
-- This script adds columns to order_items table to track which salary includes each item

-- Add columns to track salary calculation
ALTER TABLE order_items
ADD COLUMN included_in_salary_id BIGINT NULL,
ADD COLUMN is_salary_calculated BOOLEAN NOT NULL DEFAULT FALSE;

-- Add foreign key constraint for included_in_salary_id
ALTER TABLE order_items
ADD CONSTRAINT fk_order_items_salary_id
    FOREIGN KEY (included_in_salary_id) REFERENCES salaries(id);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_order_items_salary_calculated ON order_items(is_salary_calculated);
CREATE INDEX IF NOT EXISTS idx_order_items_included_in_salary ON order_items(included_in_salary_id);

-- Create a composite index for the salary query
CREATE INDEX idx_order_items_employee_status_calculated
    ON order_items(assigned_employee_id, status, is_salary_calculated);



