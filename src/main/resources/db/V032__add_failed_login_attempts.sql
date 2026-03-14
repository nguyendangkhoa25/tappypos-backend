-- Add failed_login_attempts column to users table
-- Run this against BOTH retail-platform-master AND each retail-platform-{tenantId} database

ALTER TABLE `users`
    ADD COLUMN `failed_login_attempts` int NOT NULL DEFAULT 0
        COMMENT 'Number of consecutive failed login attempts; reset to 0 on success';
