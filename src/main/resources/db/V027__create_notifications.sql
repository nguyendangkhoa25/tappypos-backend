CREATE TABLE notifications (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id        VARCHAR(50)  NOT NULL COMMENT 'Target username',
    title          VARCHAR(200) NOT NULL,
    message        TEXT         NULL,
    type           VARCHAR(30)  NOT NULL DEFAULT 'INFO' COMMENT 'SYSTEM, ORDER, ANNOUNCEMENT, LOW_STOCK, INFO',
    reference_type VARCHAR(50)  NULL COMMENT 'E.g. ORDER, PRODUCT, INVENTORY',
    reference_id   BIGINT       NULL,
    is_read        TINYINT(1)   NOT NULL DEFAULT 0,
    read_at        DATETIME(6)  NULL,
    created_by     VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    created_at     DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at     DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted        TINYINT(1)   NOT NULL DEFAULT 0,
    deleted_at     DATETIME(6)  NULL,
    INDEX idx_notifications_user_read (user_id, is_read, deleted)
);
