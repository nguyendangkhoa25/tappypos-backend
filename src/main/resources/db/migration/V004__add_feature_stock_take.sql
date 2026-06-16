-- ════════════════════════════════════════════════════════════
-- V002: Add STOCK_TAKE feature ("Kiểm Kho")
-- Stocktake / inventory verification: scan products, enter the real
-- counted quantity, reconcile against system stock, and apply adjustments.
-- Depends on INVENTORY (enforced in the app's feature dependency graph).
-- ════════════════════════════════════════════════════════════

INSERT INTO features (id, name, display_name, description, active, deleted)
VALUES (202601042, 'STOCK_TAKE', 'Kiểm Kho',
        'Kiểm kê tồn kho thực tế bằng cách quét mã, đối chiếu và điều chỉnh chênh lệch so với hệ thống',
        TRUE, FALSE)
ON CONFLICT (name) DO NOTHING;

-- Keep the BIGSERIAL sequence ahead of the explicitly-inserted id.
SELECT setval(pg_get_serial_sequence('features', 'id'),
              GREATEST((SELECT MAX(id) FROM features), 202601042), true);
