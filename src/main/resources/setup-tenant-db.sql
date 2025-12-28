-- Tenant database setup (phuc_barber_db)
-- This script should be run on the tenant database

-- Products Table
CREATE TABLE IF NOT EXISTS products (
                                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                        name VARCHAR(255) NOT NULL,
    description TEXT,
    price_before_tax DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    tax DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    price DECIMAL(10, 2) NOT NULL,
    duration_minutes INT NOT NULL,
    commission_rate DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    quantity INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    product_as_service BOOLEAN NOT NULL DEFAULT TRUE,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_active (active),
    INDEX idx_created_at (created_at),
    INDEX idx_updated_at (updated_at)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=20260001;

-- Employees Table
CREATE TABLE IF NOT EXISTS employees (
                                         id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                         name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL UNIQUE,
    email VARCHAR(100) NULL,
    position VARCHAR(50) NOT NULL,
    user_id BIGINT NULL,
    hire_date DATE,
    status ENUM('ACTIVE', 'INACTIVE', 'ON_LEAVE') NOT NULL DEFAULT 'ACTIVE',
    description TEXT,
    base_salary DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    total_earned DECIMAL(10, 2) DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP NULL,
    INDEX idx_status (status),
    INDEX idx_phone (phone),
    INDEX idx_deleted_at (deleted_at)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=79202600001;

-- Customers Table
CREATE TABLE IF NOT EXISTS customers (
                                         id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                         name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL UNIQUE,
    email VARCHAR(100),
    notes TEXT,
    zalo_id VARCHAR(100),
    facebook_id VARCHAR(100),
    preferred_services VARCHAR(500),
    allergies_or_sensitivities VARCHAR(500),
    hair_type VARCHAR(100),
    special_requests VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP NULL,
    INDEX idx_phone (phone),
    INDEX idx_deleted (deleted),
    INDEX idx_deleted_at (deleted_at)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=68202600001;

-- Shop Info Table
CREATE TABLE IF NOT EXISTS shop_info (
                                         id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                         shop_name VARCHAR(100) NOT NULL DEFAULT 'Tiệm tóc của tôi',
    address VARCHAR(500) DEFAULT '',
    company_name VARCHAR(100) DEFAULT '',
    default_tax_rate DECIMAL(5, 2) NOT NULL DEFAULT 0.00,
    e_invoice_username VARCHAR(100) DEFAULT '',
    e_invoice_password VARCHAR(500) DEFAULT '',
    e_invoice_key VARCHAR(500) DEFAULT '',
    phone VARCHAR(20) DEFAULT '',
    email VARCHAR(100) DEFAULT '',
    tax_code VARCHAR(150) DEFAULT '',
    invoice_vendor VARCHAR(50) DEFAULT '',
    website VARCHAR(200) DEFAULT '',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP NULL,
    INDEX idx_deleted (deleted),
    INDEX idx_deleted_at (deleted_at)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=3920260101;

-- Insert default shop info if not exists
INSERT IGNORE INTO shop_info (id, shop_name, address, company_name, default_tax_rate, e_invoice_username, e_invoice_password, e_invoice_key, phone, email, tax_code, invoice_vendor, website, deleted)
VALUES (3920260101, 'Tiệm tóc của tôi', '', '', 0.00, '', '', '', '', '', '', 'VIETTEL','', FALSE);

-- Roles Table - Only predefined roles are allowed
CREATE TABLE IF NOT EXISTS roles (
                                     id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                     name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP NULL,
    INDEX idx_name (name),
    INDEX idx_deleted (deleted),
    INDEX idx_deleted_at (deleted_at)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=202600001;
-- Insert 5 predefined roles (only these roles are available in the system)
    INSERT IGNORE INTO roles (id, name, description) VALUES
    (1, 'SHOP_OWNER', 'Shop Owner - Full access to all features'),
    (2, 'MANAGER', 'Manager - Can manage shop, employees, and reports'),
    (3, 'RECEPTIONIST', 'Receptionist - Can manage appointments and customers'),
    (4, 'CLEANER', 'Cleaner - Can manage cleaning tasks and inventory'),
    (5, 'TECHNICIAN', 'Technician/Employee - Can view appointments and customer info');

-- Users Table
CREATE TABLE IF NOT EXISTS users (
                                     id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                     username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100),
    password VARCHAR(255) NOT NULL,
    require_action VARCHAR(50) NULL,
    full_name VARCHAR(100),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    account_non_locked BOOLEAN NOT NULL DEFAULT TRUE,
    credentials_non_expired BOOLEAN NOT NULL DEFAULT TRUE,
    account_non_expired BOOLEAN NOT NULL DEFAULT TRUE,
    notes VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP NULL,
    INDEX idx_username (username),
    INDEX idx_active (active),
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=36202600001;

CREATE TABLE IF NOT EXISTS refresh_tokens (
                                              id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                              user_id BIGINT NOT NULL,
                                              token VARCHAR(500) NOT NULL UNIQUE,
    expiry_date BIGINT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_token (token),
    INDEX idx_user_id (user_id),
    INDEX idx_active (active)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- User Roles Junction Table
CREATE TABLE IF NOT EXISTS user_roles (
                                          user_id BIGINT NOT NULL,
                                          role_id BIGINT NOT NULL,
                                          PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_role_id (role_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Orders Table
CREATE TABLE IF NOT EXISTS orders (
                                      id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                      customer_id BIGINT NOT NULL,
                                      assigned_employee_id BIGINT,
                                      status ENUM('PENDING', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED') NOT NULL DEFAULT 'PENDING',
    total_amount DECIMAL(10, 2) NOT NULL,
    discount_amount DECIMAL(10, 2) DEFAULT 0.00,
    tax_percentage DECIMAL(5, 2) DEFAULT 0.00,
    tax_amount DECIMAL(10, 2) DEFAULT 0.00,
    commission_amount DECIMAL(10,2) DEFAULT 0,
    notes TEXT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    completed_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    FOREIGN KEY (customer_id) REFERENCES customers(id),
    FOREIGN KEY (assigned_employee_id) REFERENCES employees(id),
    INDEX idx_status (status),
    INDEX idx_customer_id (customer_id),
    INDEX idx_employee_id (assigned_employee_id),
    INDEX idx_deleted_at (deleted_at)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=39202600001;

-- Order Items Table
CREATE TABLE IF NOT EXISTS order_items (
                                           id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                           order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(255) NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    status ENUM('PENDING', 'IN_PROGRESS', 'COMPLETED') NOT NULL DEFAULT 'PENDING',
    tax_percentage DECIMAL(5, 2) DEFAULT 0.00,
    tax_amount DECIMAL(10, 2) DEFAULT 0.00,
    commission_rate DECIMAL(5,2) DEFAULT 0,
    commission_amount DECIMAL(10,2) DEFAULT 0,
    amount_before_tax DECIMAL(10,2) DEFAULT 0,
    assigned_employee_id BIGINT NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    completed_at DATETIME NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    FOREIGN KEY (assigned_employee_id) REFERENCES employees(id),
    INDEX idx_order_id (order_id),
    INDEX idx_status (status),
    INDEX idx_assigned_employee (assigned_employee_id),
    INDEX idx_deleted_at (deleted_at)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=38202600001;

-- Invoices Table
CREATE TABLE IF NOT EXISTS invoices (
                                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                        order_id BIGINT NOT NULL UNIQUE,
                                        invoice_number VARCHAR(50) NOT NULL UNIQUE,
    total_amount DECIMAL(10, 2) NOT NULL,
    tax DECIMAL(10, 2) DEFAULT 0.00,
    status ENUM('DRAFT', 'ISSUED', 'PAID', 'CANCELLED', 'SYNCED_WITH_EXTERNAL') NOT NULL DEFAULT 'DRAFT',
    external_invoice_id VARCHAR(100),
    external_sync_at TIMESTAMP NULL,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    FOREIGN KEY (order_id) REFERENCES orders(id),
    INDEX idx_status (status),
    INDEX idx_invoice_number (invoice_number),
    INDEX idx_deleted_at (deleted_at)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=21202600001;

-- Indexes for better query performance
CREATE INDEX idx_order_created_at ON orders(created_at);
CREATE INDEX idx_order_completed_at ON orders(completed_at);
CREATE INDEX idx_employee_status ON employees(status);
CREATE INDEX idx_invoice_created_at ON invoices(created_at);