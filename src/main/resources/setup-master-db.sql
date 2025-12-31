-- Master database setup (barber-shop-mgmt)
-- This script should be run on the master database

-- Tenants Table (Multi-tenancy support)
CREATE TABLE IF NOT EXISTS tenants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    db_name VARCHAR(100) NOT NULL UNIQUE,
    db_url VARCHAR(255) NOT NULL,
    db_username VARCHAR(100) NOT NULL,
    db_password VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_active (active)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Users Table (Authentication)
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    full_name VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    INDEX idx_username (username),
    INDEX idx_active (active)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Refresh Tokens Table (Remember Me functionality)
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

-- Insert test tenant 'phuc-barber'
INSERT INTO tenants (tenant_id, name, db_name, db_url, db_username, db_password, active, created_at, updated_at)
VALUES (
           'phuc-barber',
           'Phuc Barber Shop',
           'phuc_barber_db',
           'jdbc:mysql://localhost:3306/phuc_barber_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC',
           'root',
           'Root@123',
           true,
           UNIX_TIMESTAMP() * 1000,
           UNIX_TIMESTAMP() * 1000
       ) ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    db_url = VALUES(db_url),
    updated_at = UNIX_TIMESTAMP() * 1000;

