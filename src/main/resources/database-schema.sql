-- Revenue Management Tables Schema
-- Generated for Barber Shop Management System

-- =====================================================
-- REVENUES TABLE
-- =====================================================


CREATE TABLE IF NOT EXISTS revenues (
                                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                        year INT NOT NULL,
                                        month INT NOT NULL,
                                        gross_revenue DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    total_employee_salary DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    other_costs DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    total_costs DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    net_revenue DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT false,

    -- Indexes for better query performance
    INDEX idx_year (year),
    INDEX idx_month (month),
    INDEX idx_year_month (year, month),
    INDEX idx_deleted (deleted),
    INDEX idx_created_at (created_at),

    CONSTRAINT check_valid_month CHECK (month >= 1 AND month <= 12),
    CONSTRAINT check_valid_year CHECK (year >= 2000 AND year <= 2100),
    CONSTRAINT check_gross_revenue CHECK (gross_revenue >= 0),
    CONSTRAINT check_employee_salary CHECK (total_employee_salary >= 0),
    CONSTRAINT check_other_costs CHECK (other_costs >= 0),
    CONSTRAINT check_total_costs CHECK (total_costs >= 0),
    CONSTRAINT check_net_revenue CHECK (net_revenue >= -999999999.99)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=6920260001;

-- =====================================================
-- REVENUE_OTHER_COSTS TABLE
-- =====================================================
-- Stores individual cost items for each revenue record
CREATE TABLE IF NOT EXISTS revenue_other_costs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    revenue_id BIGINT NOT NULL,
    description VARCHAR(200) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT false,

    -- Foreign key to revenues table
    FOREIGN KEY (revenue_id) REFERENCES revenues(id) ON DELETE CASCADE,

    -- Indexes for better query performance
    INDEX idx_revenue_id (revenue_id),
    INDEX idx_deleted (deleted),
    INDEX idx_created_at (created_at),

    CONSTRAINT check_cost_amount CHECK (amount >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=6620260001;

-- =====================================================
-- ORDERS TABLE (Reference - showing relevant columns)
-- =====================================================
-- Note: This table should already exist. This shows the structure
-- relevant to revenue calculation
/*
CREATE TABLE IF NOT EXISTS orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    customer_id BIGINT NOT NULL,
    assigned_employee_id BIGINT,
    total DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    completed_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT false,

    INDEX idx_status (status),
    INDEX idx_completed_at (completed_at),
    INDEX idx_customer_id (customer_id),
    INDEX idx_assigned_employee_id (assigned_employee_id),
    INDEX idx_deleted (deleted),

    FOREIGN KEY (customer_id) REFERENCES customers(id),
    FOREIGN KEY (assigned_employee_id) REFERENCES employees(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
*/

-- =====================================================
-- SALARIES TABLE (Reference - showing relevant columns)
-- =====================================================
-- Note: This table should already exist. This shows the structure
-- relevant to revenue calculation
/*
CREATE TABLE IF NOT EXISTS salaries (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    year INT NOT NULL,
    month INT NOT NULL,
    net_salary DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    commission_amount DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    deduction_amount DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    overtime_amount DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    bonus_amount DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    allowance_amount DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(50) NOT NULL DEFAULT 'SUBMITTED',
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT false,

    UNIQUE KEY unique_employee_month_year (employee_id, month, year, deleted),
    INDEX idx_employee_id (employee_id),
    INDEX idx_year (year),
    INDEX idx_month (month),
    INDEX idx_status (status),
    INDEX idx_deleted (deleted),

    FOREIGN KEY (employee_id) REFERENCES employees(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
*/

-- =====================================================
-- EMPLOYEES TABLE (Reference - showing relevant columns)
-- =====================================================
-- Note: This table should already exist
/*
CREATE TABLE IF NOT EXISTS employees (
    id BIGINT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(20) NOT NULL UNIQUE,
    email VARCHAR(255),
    position VARCHAR(50) NOT NULL,
    base_salary DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    commission_rate DECIMAL(5, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT false,

    INDEX idx_status (status),
    INDEX idx_deleted (deleted),
    INDEX idx_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
*/

-- =====================================================
-- SAMPLE QUERIES FOR REVENUE CALCULATION
-- =====================================================

-- Query 1: Calculate Gross Revenue for a specific month
-- This sums all completed orders for a given month/year
/*
SELECT
    YEAR(o.completed_at) AS year,
    MONTH(o.completed_at) AS month,
    SUM(o.total) AS gross_revenue
FROM orders o
WHERE o.deleted = false
    AND o.status = 'COMPLETED'
    AND YEAR(o.completed_at) = 2026
    AND MONTH(o.completed_at) = 1
GROUP BY YEAR(o.completed_at), MONTH(o.completed_at);
*/

-- Query 2: Calculate Total Employee Salaries for a specific month
-- This sums all salary records for a given month/year
/*
SELECT
    s.year,
    s.month,
    SUM(s.net_salary) AS total_employee_salary
FROM salaries s
WHERE s.deleted = false
    AND s.year = 2026
    AND s.month = 1
GROUP BY s.year, s.month;
*/

-- Query 3: Get Revenue Summary for a specific month
-- Shows all components of the revenue calculation
/*
SELECT
    r.id,
    r.year,
    r.month,
    r.gross_revenue,
    r.total_employee_salary,
    r.other_costs,
    r.total_costs,
    r.net_revenue,
    r.notes,
    r.created_at,
    r.updated_at
FROM revenues r
WHERE r.deleted = false
    AND r.year = 2026
    AND r.month = 1;
*/

-- Query 4: Get all revenues for a specific year
-- Ordered by month (descending - most recent first)
/*
SELECT
    r.id,
    r.month,
    r.year,
    r.gross_revenue,
    r.total_employee_salary,
    r.other_costs,
    r.total_costs,
    r.net_revenue,
    r.created_at
FROM revenues r
WHERE r.deleted = false
    AND r.year = 2026
ORDER BY r.month DESC;
*/

-- Query 5: Get all costs for a specific revenue
-- Shows breakdown of individual cost items
/*
SELECT
    rc.id,
    rc.description,
    rc.amount,
    rc.created_at
FROM revenue_other_costs rc
WHERE rc.revenue_id = 1
    AND rc.deleted = false
ORDER BY rc.created_at DESC;
*/

-- Query 6: Get cost breakdown for a specific month/year
-- Shows all costs grouped with their revenue
/*
SELECT
    r.id AS revenue_id,
    r.year,
    r.month,
    r.gross_revenue,
    r.total_employee_salary,
    rc.description AS cost_description,
    rc.amount AS cost_amount,
    r.total_costs,
    r.net_revenue
FROM revenues r
LEFT JOIN revenue_other_costs rc ON r.id = rc.revenue_id AND rc.deleted = false
WHERE r.deleted = false
    AND r.year = 2026
    AND r.month = 1
ORDER BY r.id, rc.created_at DESC;
*/

-- Query 7: Revenue Trend Analysis - Last 12 months
-- Shows revenue trend across months
/*
SELECT
    r.year,
    r.month,
    r.gross_revenue,
    r.total_costs,
    r.net_revenue,
    CASE
        WHEN r.net_revenue > 0 THEN 'Profitable'
        WHEN r.net_revenue = 0 THEN 'Break-even'
        ELSE 'Loss'
    END AS status
FROM revenues r
WHERE r.deleted = false
    AND (r.year = 2025 OR r.year = 2026)
ORDER BY r.year DESC, r.month DESC
LIMIT 12;
*/

-- =====================================================
-- INDEX STATISTICS
-- =====================================================
-- To rebuild indexes and improve query performance:
/*
OPTIMIZE TABLE revenues;
ANALYZE TABLE revenues;
*/

-- =====================================================
-- NOTES
-- =====================================================
-- 1. Revenue Tables:
--    - revenues: Main revenue record with totals and calculations
--    - revenue_other_costs: Individual cost items linked to revenue via foreign key
--
-- 2. The UNIQUE constraint in revenues ensures only one revenue record per month/year
--    The "deleted" field is included to allow soft deletes while maintaining uniqueness
--
-- 3. Revenue Costs:
--    - Stores individual cost items (electricity, water, rent, insurance, etc.)
--    - Each cost has description, amount, and timestamp
--    - Linked to revenue via revenue_id foreign key
--    - Cascade delete enabled - deleting a revenue also deletes its costs
--    - Soft delete support for audit trail
--
-- 4. Indexes are created on frequently queried columns:
--    - In revenues: year, month, deleted, created_at
--    - In revenue_other_costs: revenue_id, deleted, created_at
--
-- 5. CHECK constraints validate data integrity:
--    - Month must be between 1-12
--    - Year must be between 2000-2100
--    - All monetary values must be >= 0 (except net_revenue which can be negative)
--
-- 6. Foreign key constraints:
--    - revenue_other_costs.revenue_id references revenues.id with ON DELETE CASCADE
--    - This ensures costs are automatically deleted when revenue is deleted
--
-- 7. BigDecimal(19,2) provides:
--    - Up to 19 total digits
--    - 2 decimal places
--    - Can represent values up to 9,999,999,999,999,999.99

