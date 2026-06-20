-- ══════════════════════════════════════════════════════════════════════════════
-- V030 — TRADE_IN + INSTALLMENT feature flags · VEHICLE_SHOP_SHOP_TYPE_PLAN §4c/§4e.
--
-- Two new gated capabilities for the VEHICLE_SHOP vertical (cửa hàng xe):
--   • TRADE_IN     — thu cũ đổi mới: value a used vehicle and net it against a new-vehicle
--                    sale, or buy it outright; auto-creates a resale vehicle_unit + Product.
--   • INSTALLMENT  — trả góp: sell over N kỳ with a per-kỳ schedule on top of CUSTOMER_DEBT.
-- Each carries a granular *_VIEW_ALL sub-feature (own-vs-all scope, mirrors ORDER_VIEW_ALL).
--
-- Master/global `features` rows only — role_features are seeded per-tenant by
-- TenantProvisioningService at provisioning time (same as V029 BUYBACK). Additive, idempotent.
-- ══════════════════════════════════════════════════════════════════════════════

INSERT INTO features (name, display_name, description, active, deleted) VALUES
    ('TRADE_IN', 'Thu Cũ Đổi Mới',
     'Định giá xe cũ của khách và quy đổi vào đơn bán xe mới hoặc mua đứt; dùng cho cửa hàng xe',
     TRUE, FALSE),
    ('TRADE_IN_VIEW_ALL', 'Xem Tất Cả Phiếu Thu Cũ',
     'Xem phiếu thu cũ đổi mới của tất cả nhân viên; nếu không có quyền này, chỉ xem được phiếu tự tạo',
     TRUE, FALSE),
    ('INSTALLMENT', 'Bán Trả Góp',
     'Bán hàng trả góp theo nhiều kỳ: lập lịch trả, theo dõi kỳ đến hạn và thu tiền từng kỳ',
     TRUE, FALSE),
    ('INSTALLMENT_VIEW_ALL', 'Xem Tất Cả Hợp Đồng Trả Góp',
     'Xem hợp đồng trả góp của tất cả nhân viên; nếu không có quyền này, chỉ xem được hợp đồng tự tạo',
     TRUE, FALSE)
ON CONFLICT (name) DO NOTHING;
