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

-- Insert default customer if not exists
INSERT INTO customers (id,name,phone,email,notes,zalo_id,allergies_or_sensitivities,deleted)
VALUES (100202600000,'Khách lẻ','0000.000.000','khach_le@email.com','Khách lẻ, Sử dụng khi không cần tạo thông tin khách hàng','0000.000.000','Không',0);

--- shop_info definition
CREATE TABLE `shop_info` (
                             `id` bigint NOT NULL AUTO_INCREMENT,
                             `shop_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Tiệm tóc của tôi',
                             `address` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '',
                             `company_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT '',
                             `default_tax_rate` decimal(5,2) NOT NULL DEFAULT '0.00',
                             `e_invoice_username` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT '',
                             `e_invoice_password` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '',
                             `e_invoice_key` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '',
                             `phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT '',
                             `email` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT '',
                             `supplier_tax_code` varchar(150) COLLATE utf8mb4_unicode_ci DEFAULT '',
                             `invoice_vendor` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT '',
                             `website` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT '',
                             `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
                             `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                             `deleted` tinyint(1) NOT NULL DEFAULT '0',
                             `deleted_at` timestamp NULL DEFAULT NULL,
                             `template_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
                             `invoice_system` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'S-INVOICE' COMMENT 'Invoice system type: S-INVOICE, M-INVOICE, MOCK',
                             `invoice_series` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Invoice series for S-Invoice (e.g., C22TAA)',
                             PRIMARY KEY (`id`),
                             KEY `idx_deleted` (`deleted`),
                             KEY `idx_deleted_at` (`deleted_at`)
) ENGINE=InnoDB AUTO_INCREMENT=3920260102 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
-- Insert default shop info if not exists
INSERT IGNORE INTO shop_info (id, shop_name, address, company_name, default_tax_rate, e_invoice_username, e_invoice_password, e_invoice_key, phone, email, supplier_tax_code, invoice_vendor, website, deleted)
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
    INDEX idx_active (active)
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
    INDEX idx_status (status),
    INDEX idx_customer_id (customer_id),
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

-- Create invoice_buyers table
CREATE TABLE IF NOT EXISTS invoice_buyers (
                                              id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                              customer_id BIGINT,
                                              buyer_name VARCHAR(255),
    buyer_legal_name VARCHAR(255),
    buyer_tax_code VARCHAR(50),
    buyer_address VARCHAR(500),
    buyer_phone_number VARCHAR(20),
    buyer_email VARCHAR(255),
    buyer_bank_name VARCHAR(255),
    buyer_bank_account VARCHAR(50),
    buyer_id_number VARCHAR(50),
    is_visiting_guest BOOLEAN DEFAULT false,
    INDEX idx_customer_id (customer_id),
    INDEX idx_buyer_tax_code (buyer_tax_code)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=5520260001;
-- Invoices Table
CREATE TABLE `invoices` (
                            `id` bigint NOT NULL AUTO_INCREMENT,
                            `order_id` bigint NOT NULL,
                            `invoice_number` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
                            `invoice_series` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
                            `total_amount` decimal(10,2) NOT NULL,
                            `tax` decimal(10,2) DEFAULT '0.00',
                            `status` enum('DRAFT','COMPLETED','FAILED','CANCELLED') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DRAFT',
                            `external_invoice_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
                            `external_sync_at` timestamp NULL DEFAULT NULL,
                            `notes` text COLLATE utf8mb4_unicode_ci,
                            `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
                            `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                            `deleted_at` timestamp NULL DEFAULT NULL,
                            `issued_date` datetime DEFAULT NULL,
                            `total_amount_without_tax` decimal(19,2) NOT NULL DEFAULT '0.00',
                            `tax_amount` decimal(19,2) NOT NULL DEFAULT '0.00',
                            `tax_percentage` decimal(5,2) NOT NULL DEFAULT '0.00',
                            `payment_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
                            `invoice_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
                            `currency_code` varchar(3) COLLATE utf8mb4_unicode_ci DEFAULT 'VND',
                            `error_message` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
                            `deleted` tinyint(1) NOT NULL DEFAULT '0',
                            `buyer_id` bigint DEFAULT NULL,
                            `transaction_uuid` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
                            PRIMARY KEY (`id`),
                            UNIQUE KEY `invoice_number` (`invoice_number`),
                            KEY `idx_order_id` (`order_id`),
                            KEY `idx_status` (`status`),
                            KEY `idx_invoice_number` (`invoice_number`),
                            KEY `idx_deleted_at` (`deleted_at`),
                            KEY `fk_invoices_buyer` (`buyer_id`),
                            KEY `idx_external_invoice_id` (`external_invoice_id`),
                            KEY `idx_deleted` (`deleted`),
                            CONSTRAINT `fk_invoices_buyer` FOREIGN KEY (`buyer_id`) REFERENCES `invoice_buyers` (`id`) ON DELETE SET NULL,
                            CONSTRAINT `invoices_ibfk_1` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=21202600002 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
-- Indexes for better query performance
CREATE INDEX idx_order_created_at ON orders(created_at);
CREATE INDEX idx_order_completed_at ON orders(completed_at);
CREATE INDEX idx_employee_status ON employees(status);
CREATE INDEX idx_invoice_created_at ON invoices(created_at);


CREATE TABLE IF NOT EXISTS api_audit_log (
                                             log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                             trace_id VARCHAR(100) NOT NULL COMMENT 'Unique trace ID for tracking requests across systems',
    api_endpoint VARCHAR(500) NOT NULL COMMENT 'API endpoint URL that was called',
    http_method VARCHAR(20) NOT NULL COMMENT 'HTTP method (GET, POST, PUT, DELETE, etc.)',
    request_body LONGTEXT COMMENT 'Request payload in JSON format',
    request_headers LONGTEXT COMMENT 'Request headers in JSON format',
    response_body LONGTEXT COMMENT 'Response payload in JSON format',
    response_headers LONGTEXT COMMENT 'Response headers in JSON format',
    response_status INT COMMENT 'HTTP response status code',
    request_size BIGINT COMMENT 'Size of request in bytes',
    response_size BIGINT COMMENT 'Size of response in bytes',
    execution_time_ms BIGINT COMMENT 'Execution time in milliseconds',
    error_message LONGTEXT COMMENT 'Error message if request failed',
    exception_stack_trace LONGTEXT COMMENT 'Exception stack trace for debugging',
    user_id VARCHAR(100) COMMENT 'ID of the user who made the request',
    ip_address VARCHAR(50) COMMENT 'IP address of the client',
    status VARCHAR(20) COMMENT 'Status: SUCCESS, FAILURE, PARTIAL_FAILURE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'When the log was created',
    description VARCHAR(255) COMMENT 'Optional description or notes',

    -- Indexes for better query performance
    INDEX idx_api_endpoint (api_endpoint(255)),
    INDEX idx_method (http_method),
    INDEX idx_timestamp (created_at),
    INDEX idx_status (response_status),
    INDEX idx_trace_id (trace_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci

-- Create promotions table
CREATE TABLE IF NOT EXISTS promotions (
                                          id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                          name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    promotion_type VARCHAR(50) NOT NULL,
    discount_type VARCHAR(50) NOT NULL,
    discount_value DECIMAL(10, 2),
    discount_percentage DECIMAL(5, 2),
    start_date DATETIME,
    end_date DATETIME,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    min_purchase_amount DECIMAL(10, 2),
    max_discount_amount DECIMAL(10, 2),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at DATETIME,
    INDEX idx_promotion_type (promotion_type),
    INDEX idx_is_active (is_active),
    INDEX idx_dates (start_date, end_date),
    INDEX idx_deleted (deleted),
    INDEX idx_deleted_at (deleted_at)
    )ENGINE=InnoDB AUTO_INCREMENT=7620260101 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create promotion_products table (for combo promotions)
CREATE TABLE IF NOT EXISTS promotion_products (
                                                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                  promotion_id BIGINT NOT NULL,
                                                  product_id BIGINT NOT NULL,
                                                  quantity INT,
                                                  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                                  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                                  deleted BOOLEAN NOT NULL DEFAULT FALSE,
                                                  deleted_at DATETIME,
                                                  FOREIGN KEY (promotion_id) REFERENCES promotions(id),
    FOREIGN KEY (product_id) REFERENCES products(id),
    INDEX idx_promotion_id (promotion_id),
    INDEX idx_product_id (product_id),
    INDEX idx_deleted (deleted),
    INDEX idx_deleted_at (deleted_at)
    )ENGINE=InnoDB AUTO_INCREMENT=8820260102 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
