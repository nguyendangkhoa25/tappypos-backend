-- V003: Add PAWN_VIEW_ALL sub-feature
-- Follows the same pattern as ORDER_VIEW_ALL / COMMISSION_VIEW_ALL (see CLAUDE.md).
-- When PAWN_VIEW_ALL is absent from a user's JWT, PawnServiceImpl.getPawns() scopes
-- the contract list to rows where created_by = current username.

INSERT INTO features (name, display_name, description)
VALUES (
    'PAWN_VIEW_ALL',
    'Xem Tất Cả Cầm Đồ',
    'Xem hợp đồng cầm đồ của tất cả nhân viên; nếu không có quyền này, chỉ xem được hợp đồng tự tạo'
) ON CONFLICT (name) DO NOTHING;

-- Assign to SHOP_OWNER and MANAGER by default (same as ORDER_VIEW_ALL)
INSERT INTO role_features (role_id, feature_id)
SELECT r.id, f.id
FROM roles r, features f
WHERE r.name IN ('SHOP_OWNER', 'MANAGER')
  AND f.name = 'PAWN_VIEW_ALL'
ON CONFLICT DO NOTHING;
