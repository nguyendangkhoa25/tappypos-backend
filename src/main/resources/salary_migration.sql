-- SQL Migration Script for Salary Feature
-- This script creates the necessary database tables for the salary management feature

-- Create salaries table
CREATE TABLE IF NOT EXISTS salaries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    month INT NOT NULL,
    year INT NOT NULL,
    total_earnings DECIMAL(12, 2) NOT NULL DEFAULT 0,
    deductions DECIMAL(12, 2) NOT NULL DEFAULT 0,
    overtime DECIMAL(12, 2) NOT NULL DEFAULT 0,
    bonus DECIMAL(12, 2) NOT NULL DEFAULT 0,
    net_salary DECIMAL(12, 2) NOT NULL DEFAULT 0,
    notes TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    approved_at TIMESTAMP NULL,
    approved_by_user_id BIGINT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (employee_id) REFERENCES employees(id),
    FOREIGN KEY (approved_by_user_id) REFERENCES users(id),

    -- Unique constraint to ensure only one salary per employee per month/year (when not deleted)
    UNIQUE KEY unique_employee_month_year (employee_id, month, year, deleted),

    -- Indexes for common queries
    INDEX idx_employee_id (employee_id),
    INDEX idx_month_year (month, year),
    INDEX idx_status (status),
    INDEX idx_deleted (deleted),
    INDEX idx_created_at (created_at)
);

-- Comments for documentation
ALTER TABLE salaries COMMENT = 'Salary records for employees with monthly earnings, deductions, overtime, and bonuses';
ALTER TABLE salaries MODIFY COLUMN total_earnings COMMENT 'Total earnings from completed orders in the month';
ALTER TABLE salaries MODIFY COLUMN deductions COMMENT 'Deductions from salary';
ALTER TABLE salaries MODIFY COLUMN overtime COMMENT 'Overtime payments';
ALTER TABLE salaries MODIFY COLUMN bonus COMMENT 'Bonus amount';
ALTER TABLE salaries MODIFY COLUMN net_salary COMMENT 'Calculated net salary = earnings - deductions + overtime + bonus';
ALTER TABLE salaries MODIFY COLUMN status COMMENT 'Salary status: DRAFT, SUBMITTED, APPROVED, REJECTED';

