-- Run against retail-platform-master when deploying the single-device-login feature
CREATE TABLE IF NOT EXISTS active_sessions (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    username    VARCHAR(50)  NOT NULL,
    session_id  VARCHAR(36)  NOT NULL COMMENT 'UUID embedded in JWT',
    ip_address  VARCHAR(45)  NULL     COMMENT 'IPv4 or IPv6',
    user_agent  VARCHAR(500) NULL,
    login_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_active DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_active_sessions_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='One active session per user. Replaced on force-login.';
