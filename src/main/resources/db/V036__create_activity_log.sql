-- Activity log table: records business-level events per tenant
CREATE TABLE activity_log (
    id            BIGINT NOT NULL AUTO_INCREMENT,
    actor_username VARCHAR(50)  NOT NULL  COMMENT 'Username of who performed the action',
    actor_full_name VARCHAR(100) NULL     COMMENT 'Display name at time of action',
    action        VARCHAR(50)  NOT NULL   COMMENT 'ActivityAction enum value',
    target_type   VARCHAR(50)  NULL       COMMENT 'e.g. ORDER, PRODUCT, CUSTOMER',
    target_id     VARCHAR(100) NULL       COMMENT 'ID or reference number of the target',
    description   VARCHAR(500) NOT NULL   COMMENT 'Human-readable description of the event',
    ip_address    VARCHAR(45)  NULL       COMMENT 'IPv4 or IPv6 of the request origin',
    created_at    DATETIME     NOT NULL   DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_activity_actor     (actor_username),
    INDEX idx_activity_action    (action),
    INDEX idx_activity_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Business-level activity log for shop admin visibility';

-- Seed ACTIVITY_LOG feature for this tenant
INSERT IGNORE INTO features (name, display_name, description, active, created_at, updated_at, deleted)
VALUES ('ACTIVITY_LOG', 'Nhật Ký Hoạt Động', 'Xem nhật ký hoạt động của người dùng trong cửa hàng', 1, NOW(), NOW(), 0);

-- Assign ACTIVITY_LOG to SHOP_OWNER and MANAGER roles
INSERT IGNORE INTO role_features (role_id, feature_id, created_at)
SELECT r.id, f.id, NOW()
FROM roles r
JOIN features f ON f.name = 'ACTIVITY_LOG'
WHERE r.name IN ('SHOP_OWNER', 'MANAGER')
  AND r.deleted = 0;
