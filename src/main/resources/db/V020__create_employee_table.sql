CREATE TABLE IF NOT EXISTS employees (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name       VARCHAR(255) NOT NULL,
    phone           VARCHAR(50),
    email           VARCHAR(255),
    position        VARCHAR(50) NOT NULL,
    department      VARCHAR(255),
    hire_date       DATE,
    active          TINYINT(1) NOT NULL DEFAULT 1,
    base_wage       DECIMAL(15, 2),
    commission_rate DECIMAL(5, 2),
    notes           TEXT,
    avatar          VARCHAR(512),
    user_id         BIGINT,
    created_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted         TINYINT(1) NOT NULL DEFAULT 0,
    deleted_at      DATETIME(6),
    CONSTRAINT fk_employee_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
