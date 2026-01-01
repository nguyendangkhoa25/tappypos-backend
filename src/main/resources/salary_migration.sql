-- SQL Migration Script for Salary Feature
-- This script creates the necessary database tables for the salary management feature

-- Create salaries table
CREATE TABLE IF NOT EXISTS salaries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    month INT NOT NULL,
    year INT NOT NULL,
    net_salary DECIMAL(12, 2) NOT NULL DEFAULT 0,
    commission_amount DECIMAL(12, 2) NOT NULL DEFAULT 0,
    deduction_amount DECIMAL(12, 2) NOT NULL DEFAULT 0,
    overtime_amount DECIMAL(12, 2) NOT NULL DEFAULT 0,
    bonus_amount DECIMAL(12, 2) NOT NULL DEFAULT 0,
    allowance_amount DECIMAL(12, 2) NOT NULL DEFAULT 0,
    notes TEXT,
    status ENUM('SUBMITTED', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'SUBMITTED',
    approved_at TIMESTAMP NULL,
    approved_by_user_id BIGINT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (employee_id) REFERENCES employees(id),
    FOREIGN KEY (approved_by_user_id) REFERENCES users(id),

    -- Indexes for common queries
    INDEX idx_employee_id (employee_id),
    INDEX idx_month_year (month, year),
    INDEX idx_status (status),
    INDEX idx_deleted (deleted),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=2120260001;
