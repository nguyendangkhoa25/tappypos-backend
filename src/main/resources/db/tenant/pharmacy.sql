-- ============================================================
-- TENANT SEED — DEFAULT DATA: PHARMACY (NHÀ THUỐC / DƯỢC PHẨM)
-- PostgreSQL / shared-DB version.
-- All INSERTs are idempotent (ON CONFLICT DO NOTHING or DO UPDATE).
-- tenant_id sourced from app.current_tenant session variable.
--
-- Dominant product type: DRUG ('Dược phẩm'). Unlike general.sql, this file
-- seeds the full DRUG attribute set (SĐK, hoạt chất, hàm lượng, dạng bào chế,
-- hạn dùng, số lô, thuốc kê đơn, …) so a nhà thuốc is pharmacy-shaped on day one.
-- ============================================================

-- ── 1. Product types (standard retail set; DRUG dominant) ─────
INSERT INTO product_type (tenant_id, code, name, description, default_inventory_mode, default_unit) VALUES
    (current_setting('app.current_tenant', true), 'DRUG',         'Dược phẩm',                  'Thuốc và sản phẩm dược', 'TRACKED', 'box'),
    (current_setting('app.current_tenant', true), 'HEALTH',       'Sức khỏe / Dinh dưỡng',      'Thực phẩm chức năng, vitamin, thiết bị y tế', 'TRACKED', 'piece'),
    (current_setting('app.current_tenant', true), 'BEAUTY',       'Dược mỹ phẩm',               'Dược mỹ phẩm và chăm sóc da', 'TRACKED', 'piece'),
    (current_setting('app.current_tenant', true), 'CONVENIENCE',  'Hàng chăm sóc cá nhân',      'Hàng tiêu dùng và chăm sóc cá nhân', 'TRACKED', 'piece'),
    (current_setting('app.current_tenant', true), 'FOOD',         'Thực phẩm',                  'Thực phẩm bổ sung', 'TRACKED', 'piece'),
    (current_setting('app.current_tenant', true), 'BEVERAGE',     'Đồ uống',                    'Nước và đồ uống', 'TRACKED', 'bottle')
ON CONFLICT (code, tenant_id) DO UPDATE SET name = EXCLUDED.name, description = EXCLUDED.description, default_inventory_mode = EXCLUDED.default_inventory_mode;

-- ── 2. Categories ─────────────────────────────────────────────
-- Parent categories
INSERT INTO category (tenant_id, name, parent_id) VALUES
    (current_setting('app.current_tenant', true), 'Thuốc kê đơn (ETC)',          NULL),
    (current_setting('app.current_tenant', true), 'Thuốc không kê đơn (OTC)',    NULL),
    (current_setting('app.current_tenant', true), 'Thực phẩm chức năng',         NULL),
    (current_setting('app.current_tenant', true), 'Vitamin & Khoáng chất',       NULL),
    (current_setting('app.current_tenant', true), 'Thiết bị & Dụng cụ y tế',     NULL),
    (current_setting('app.current_tenant', true), 'Dược mỹ phẩm',                NULL),
    (current_setting('app.current_tenant', true), 'Mẹ & Bé',                     NULL),
    (current_setting('app.current_tenant', true), 'Chăm sóc cá nhân',            NULL);

-- Child categories
INSERT INTO category (tenant_id, name, parent_id)
SELECT current_setting('app.current_tenant', true), c.name, p.id
FROM (VALUES
    ('Kháng sinh',                   'Thuốc kê đơn (ETC)'),
    ('Tim mạch & Huyết áp',          'Thuốc kê đơn (ETC)'),
    ('Tiểu đường',                   'Thuốc kê đơn (ETC)'),
    ('Giảm đau & Hạ sốt',            'Thuốc không kê đơn (OTC)'),
    ('Ho & Cảm cúm',                 'Thuốc không kê đơn (OTC)'),
    ('Tiêu hóa',                     'Thuốc không kê đơn (OTC)'),
    ('Dị ứng',                       'Thuốc không kê đơn (OTC)'),
    ('Da liễu',                      'Thuốc không kê đơn (OTC)'),
    ('Vitamin tổng hợp',             'Vitamin & Khoáng chất'),
    ('Canxi & Xương khớp',           'Vitamin & Khoáng chất')
) AS c(name, parent_name)
JOIN category p ON p.name = c.parent_name
    AND p.tenant_id = current_setting('app.current_tenant', true)
    AND p.parent_id IS NULL;

-- ── 3. Vendors ────────────────────────────────────────────────
INSERT INTO vendors (tenant_id, name, code, contact_name, phone, payment_terms, is_active, deleted)
VALUES
    (current_setting('app.current_tenant', true), 'Công ty Dược phẩm Trung ương',        'VND-001', NULL, NULL, 'NET_30', TRUE, FALSE),
    (current_setting('app.current_tenant', true), 'Nhà phân phối dược & thiết bị y tế',  'VND-002', NULL, NULL, 'NET_30', TRUE, FALSE)
ON CONFLICT (code, tenant_id) DO NOTHING;

-- ── 5. Sample products ────────────────────────────────────────
-- Starter catalogue (per the seed convention every shop type ships 15–40 items).
-- Realistic Vietnamese nhà thuốc inventory: OTC, kê đơn, TPCN, thiết bị y tế.
INSERT INTO product
    (tenant_id, sku, name, description, price, cost_price, unit, product_type_id, status)
SELECT
    current_setting('app.current_tenant', true),
    CONCAT('PHA-DEMO-', LPAD(seq.n::text, 3, '0')),
    seq.item_name, seq.item_desc, seq.item_price, seq.item_cost, seq.item_unit,
    pt.id, 'ACTIVE'
FROM (VALUES
    (1,  'Paracetamol 500mg (Panadol)',     'Hộp 10 vỉ x 10 viên — hạ sốt, giảm đau',    35000,  24000,  'box',    'DRUG'),
    (2,  'Efferalgan 500mg',                'Hộp 16 viên sủi — hạ sốt, giảm đau',         42000,  30000,  'box',    'DRUG'),
    (3,  'Hapacol 650mg',                   'Hộp 10 vỉ x 10 viên — hạ sốt người lớn',     38000,  26000,  'box',    'DRUG'),
    (4,  'Amoxicillin 500mg',               'Hộp 10 vỉ x 10 viên — kháng sinh',           45000,  30000,  'box',    'DRUG'),
    (5,  'Augmentin 625mg',                 'Hộp 2 vỉ x 7 viên — kháng sinh',            120000,  90000,  'box',    'DRUG'),
    (6,  'Decolgen Forte',                  'Hộp 25 vỉ x 4 viên — cảm cúm',               60000,  42000,  'box',    'DRUG'),
    (7,  'Tiffy Dey',                       'Hộp 25 vỉ x 4 viên — cảm cúm, sổ mũi',       55000,  38000,  'box',    'DRUG'),
    (8,  'Cetirizine 10mg',                 'Hộp 10 vỉ x 10 viên — chống dị ứng',         28000,  18000,  'box',    'DRUG'),
    (9,  'Omeprazole 20mg',                 'Hộp 3 vỉ x 10 viên — dạ dày',                40000,  27000,  'box',    'DRUG'),
    (10, 'Smecta (Diosmectite)',            'Hộp 30 gói — tiêu chảy',                     72000,  50000,  'box',    'DRUG'),
    (11, 'Oresol (gói bù nước)',            'Hộp 20 gói — bù nước, điện giải',            36000,  24000,  'box',    'DRUG'),
    (12, 'Berberin',                        'Lọ 100 viên — tiêu chảy, rối loạn tiêu hóa', 22000,  14000,  'bottle', 'DRUG'),
    (13, 'Loperamide (Imodium)',            'Hộp 2 vỉ x 10 viên — cầm tiêu chảy',         48000,  33000,  'box',    'DRUG'),
    (14, 'Nước muối sinh lý NaCl 0.9%',     'Chai 500ml — vệ sinh, nhỏ mũi',             12000,   7000,  'bottle', 'DRUG'),
    (15, 'Salonpas (miếng dán giảm đau)',   'Gói 10 miếng — giảm đau cơ, khớp',          25000,  16000,  'piece',  'DRUG'),
    (16, 'Vitamin C 1000mg',                'Hộp 10 viên sủi — tăng đề kháng',            45000,  30000,  'box',    'HEALTH'),
    (17, 'Vitamin 3B',                      'Hộp 100 viên — bổ thần kinh',                65000,  45000,  'box',    'HEALTH'),
    (18, 'Canxi Corbiere',                  'Hộp 30 ống — bổ sung canxi',                 78000,  55000,  'box',    'HEALTH'),
    (19, 'Khẩu trang y tế 4 lớp',           'Hộp 50 cái — khẩu trang kháng khuẩn',        35000,  22000,  'box',    'HEALTH'),
    (20, 'Nhiệt kế điện tử',                'Nhiệt kế đo nhanh đầu mềm',                  85000,  55000,  'piece',  'HEALTH')
) AS seq(n, item_name, item_desc, item_price, item_cost, item_unit, type_code)
JOIN product_type pt
    ON pt.code = seq.type_code
   AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (sku, tenant_id) DO NOTHING;

-- ── 6. Product → category links ───────────────────────────────
INSERT INTO product_category (tenant_id, product_id, category_id)
SELECT current_setting('app.current_tenant', true), p.id, c.id
FROM (VALUES
    ('PHA-DEMO-001', 'Thuốc không kê đơn (OTC)'),
    ('PHA-DEMO-002', 'Thuốc không kê đơn (OTC)'),
    ('PHA-DEMO-003', 'Thuốc không kê đơn (OTC)'),
    ('PHA-DEMO-004', 'Thuốc kê đơn (ETC)'),
    ('PHA-DEMO-005', 'Thuốc kê đơn (ETC)'),
    ('PHA-DEMO-006', 'Thuốc không kê đơn (OTC)'),
    ('PHA-DEMO-007', 'Thuốc không kê đơn (OTC)'),
    ('PHA-DEMO-008', 'Thuốc không kê đơn (OTC)'),
    ('PHA-DEMO-009', 'Thuốc không kê đơn (OTC)'),
    ('PHA-DEMO-010', 'Thuốc không kê đơn (OTC)'),
    ('PHA-DEMO-011', 'Thuốc không kê đơn (OTC)'),
    ('PHA-DEMO-012', 'Thuốc không kê đơn (OTC)'),
    ('PHA-DEMO-013', 'Thuốc không kê đơn (OTC)'),
    ('PHA-DEMO-014', 'Thuốc không kê đơn (OTC)'),
    ('PHA-DEMO-015', 'Thuốc không kê đơn (OTC)'),
    ('PHA-DEMO-016', 'Vitamin & Khoáng chất'),
    ('PHA-DEMO-017', 'Vitamin & Khoáng chất'),
    ('PHA-DEMO-018', 'Vitamin & Khoáng chất'),
    ('PHA-DEMO-019', 'Thiết bị & Dụng cụ y tế'),
    ('PHA-DEMO-020', 'Thiết bị & Dụng cụ y tế')
) AS m(sku, cat)
JOIN product p ON p.sku = m.sku AND p.tenant_id = current_setting('app.current_tenant', true)
JOIN category c ON c.name = m.cat AND c.parent_id IS NULL
    AND c.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (product_id, category_id) DO NOTHING;

-- ── 7. Inventory for sample products (with batch + expiry) ────
-- Pharmacy = expiry/lot critical, so sample drugs carry a batch number and a
-- forward-dated expiry to exercise the batch/expiry fields out of the box.
INSERT INTO inventory
    (tenant_id, product_id, quantity_in_stock, reorder_level, reorder_quantity,
     unit_cost, warehouse_location, batch_number, expiry_date, deleted)
SELECT
    p.tenant_id, p.id, 100, 20, 50, p.cost_price, 'Quầy thuốc',
    CONCAT('LOT-', SUBSTRING(p.sku FROM 'PHA-DEMO-(.*)')),
    (CURRENT_DATE + INTERVAL '18 months')::date,
    FALSE
FROM product p
WHERE p.sku LIKE 'PHA-DEMO-%'
  AND p.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (product_id) WHERE variant_id IS NULL AND deleted = false DO NOTHING;

-- ── 8. Loyalty program ───────────────────────────────────────
INSERT INTO loyalty_programs
    (tenant_id, points_per_amount, amount_per_points, redemption_points_per_discount,
     redemption_discount_amount, min_redemption_points, is_active)
VALUES
    (current_setting('app.current_tenant', true), 1, 10000, 100, 10000.00, 100, TRUE);

-- ── 9. Loyalty tiers ──────────────────────────────────────────
INSERT INTO loyalty_tiers
    (tenant_id, name, min_spend, points_multiplier, color, description, sort_order)
VALUES
    (current_setting('app.current_tenant', true), 'Đồng',      0,         1.00, '#CD7F32', 'Thành viên cơ bản',        1),
    (current_setting('app.current_tenant', true), 'Bạc',       2000000,   1.25, '#9E9E9E', 'Chi tiêu từ 2 triệu VND',  2),
    (current_setting('app.current_tenant', true), 'Vàng',      10000000,  1.50, '#FFC107', 'Chi tiêu từ 10 triệu VND', 3),
    (current_setting('app.current_tenant', true), 'Kim cương', 50000000,  2.00, '#00BCD4', 'Chi tiêu từ 50 triệu VND', 4);

-- ── 10. Default print templates ───────────────────────────────
INSERT INTO print_templates (tenant_id, template_type, name, config_json, is_default) VALUES
    -- POS receipt: "Hóa đơn thuốc" — tax breakdown + tax ID (matches TenantSeedService PHARMACY branch)
    (current_setting('app.current_tenant', true), 'POS_RECEIPT', 'Hóa đơn thuốc', '{
  "headerText": "",
  "footerText": "Cảm ơn quý khách!\nChúc bạn mau hồi phục!",
  "showAddress": true,
  "showTaxId": true,
  "showOrderNumber": true,
  "showDateTime": true,
  "showCustomer": true,
  "showTaxBreakdown": true,
  "showCashDetails": true,
  "paperWidth": "80mm",
  "autoClose": true,
  "showVietQr": false
}', TRUE),
    -- Product stamp: barcode + expiry (critical for drugs)
    (current_setting('app.current_tenant', true), 'PRODUCT_STAMP', 'Tem thuốc', '{
  "showShopName": true,
  "showSku": true,
  "showPrice": true,
  "showBarcode": true,
  "showLocation": false,
  "showBatch": true,
  "showExpiry": true,
  "labelWidth": 60,
  "labelHeight": 38
}', TRUE),
    -- Inventory stamp: batch + expiry + location for stock management
    (current_setting('app.current_tenant', true), 'INVENTORY_STAMP', 'Tem kho', '{
  "showShopName": true,
  "showSku": true,
  "showPrice": false,
  "showBarcode": true,
  "showLocation": true,
  "showBatch": true,
  "showExpiry": true,
  "labelWidth": 60,
  "labelHeight": 38
}', TRUE)
ON CONFLICT (template_type, name, tenant_id) DO NOTHING;

-- ── 11. Attribute groups (DRUG type) ──────────────────────────
INSERT INTO attribute_group (tenant_id, product_type_id, code, name, display_order)
SELECT current_setting('app.current_tenant', true), id, 'thong_tin_thuoc', 'Thông tin thuốc', 1
FROM product_type WHERE code = 'DRUG' AND tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name, display_order = EXCLUDED.display_order;

INSERT INTO attribute_group (tenant_id, product_type_id, code, name, display_order)
SELECT current_setting('app.current_tenant', true), id, 'thanh_phan_lieu_dung', 'Thành phần & Liều dùng', 2
FROM product_type WHERE code = 'DRUG' AND tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name, display_order = EXCLUDED.display_order;

INSERT INTO attribute_group (tenant_id, product_type_id, code, name, display_order)
SELECT current_setting('app.current_tenant', true), id, 'bao_quan_han_dung', 'Bảo quản & Hạn dùng', 3
FROM product_type WHERE code = 'DRUG' AND tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name, display_order = EXCLUDED.display_order;

INSERT INTO attribute_group (tenant_id, product_type_id, code, name, display_order)
SELECT current_setting('app.current_tenant', true), id, 'phan_loai_quy_dinh', 'Phân loại & Quy định', 4
FROM product_type WHERE code = 'DRUG' AND tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name, display_order = EXCLUDED.display_order;

-- ── 11a. Attribute definitions — group "Thông tin thuốc" ──────
INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'drug_registration_number', 'Số đăng ký (SĐK)', 'STRING', TRUE, TRUE, FALSE, 1
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'thong_tin_thuoc'
WHERE pt.code = 'DRUG' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'manufacturer', 'Nhà sản xuất', 'STRING', TRUE, TRUE, TRUE, 2
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'thong_tin_thuoc'
WHERE pt.code = 'DRUG' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'country_of_origin', 'Xuất xứ', 'STRING', FALSE, TRUE, TRUE, 3
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'thong_tin_thuoc'
WHERE pt.code = 'DRUG' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'package_size', 'Quy cách đóng gói', 'STRING', FALSE, TRUE, FALSE, 4
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'thong_tin_thuoc'
WHERE pt.code = 'DRUG' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

-- ── 11b. Attribute definitions — group "Thành phần & Liều dùng" ──
INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'active_ingredient', 'Hoạt chất chính', 'STRING', TRUE, TRUE, TRUE, 1
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'thanh_phan_lieu_dung'
WHERE pt.code = 'DRUG' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'concentration', 'Hàm lượng / Nồng độ', 'STRING', TRUE, FALSE, TRUE, 2
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'thanh_phan_lieu_dung'
WHERE pt.code = 'DRUG' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'dosage_form', 'Dạng bào chế', 'STRING', TRUE, FALSE, TRUE, 3
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'thanh_phan_lieu_dung'
WHERE pt.code = 'DRUG' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'indication', 'Chỉ định (tác dụng)', 'TEXT', FALSE, TRUE, FALSE, 4
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'thanh_phan_lieu_dung'
WHERE pt.code = 'DRUG' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'contraindication', 'Chống chỉ định', 'TEXT', FALSE, FALSE, FALSE, 5
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'thanh_phan_lieu_dung'
WHERE pt.code = 'DRUG' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'side_effects', 'Tác dụng phụ', 'TEXT', FALSE, FALSE, FALSE, 6
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'thanh_phan_lieu_dung'
WHERE pt.code = 'DRUG' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

-- ── 11c. Attribute definitions — group "Bảo quản & Hạn dùng" ──
INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'expiry_date', 'Hạn sử dụng', 'DATE', TRUE, FALSE, TRUE, 1
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'bao_quan_han_dung'
WHERE pt.code = 'DRUG' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'storage_condition', 'Điều kiện bảo quản', 'STRING', FALSE, FALSE, TRUE, 2
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'bao_quan_han_dung'
WHERE pt.code = 'DRUG' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'lot_number', 'Số lô sản xuất', 'STRING', FALSE, TRUE, FALSE, 3
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'bao_quan_han_dung'
WHERE pt.code = 'DRUG' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

-- ── 11d. Attribute definitions — group "Phân loại & Quy định" ──
INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'prescription_required', 'Thuốc kê đơn', 'BOOLEAN', TRUE, FALSE, TRUE, 1
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'phan_loai_quy_dinh'
WHERE pt.code = 'DRUG' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'drug_category', 'Nhóm dược lý', 'STRING', FALSE, TRUE, TRUE, 2
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'phan_loai_quy_dinh'
WHERE pt.code = 'DRUG' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

-- ── 12. Sample drug attribute values — prescription flag ──────
-- Seed the `prescription_required` BOOLEAN on the sample DRUG products so the POS grid badges
-- ("Kê đơn") and the checkout warning are visible out of the box: TRUE for the two ETC antibiotics,
-- FALSE for the OTC items. Joins the DRUG type's attribute definition (not HEALTH products).
INSERT INTO product_attribute_value (tenant_id, product_id, attribute_id, value_boolean)
SELECT current_setting('app.current_tenant', true), p.id, ad.id, v.rx
FROM (VALUES
    ('PHA-DEMO-001', FALSE),  -- Paracetamol (OTC)
    ('PHA-DEMO-002', FALSE),  -- Efferalgan (OTC)
    ('PHA-DEMO-003', FALSE),  -- Hapacol (OTC)
    ('PHA-DEMO-004', TRUE),   -- Amoxicillin (kháng sinh — kê đơn)
    ('PHA-DEMO-005', TRUE),   -- Augmentin (kháng sinh — kê đơn)
    ('PHA-DEMO-006', FALSE),  -- Decolgen (OTC)
    ('PHA-DEMO-007', FALSE),  -- Tiffy (OTC)
    ('PHA-DEMO-008', FALSE),  -- Cetirizine (OTC)
    ('PHA-DEMO-009', FALSE),  -- Omeprazole (OTC)
    ('PHA-DEMO-010', FALSE),  -- Smecta (OTC)
    ('PHA-DEMO-011', FALSE),  -- Oresol (OTC)
    ('PHA-DEMO-012', FALSE),  -- Berberin (OTC)
    ('PHA-DEMO-013', FALSE),  -- Loperamide (OTC)
    ('PHA-DEMO-014', FALSE),  -- Nước muối sinh lý (OTC)
    ('PHA-DEMO-015', FALSE)   -- Salonpas (OTC)
) AS v(sku, rx)
JOIN product p
    ON p.sku = v.sku AND p.tenant_id = current_setting('app.current_tenant', true)
JOIN attribute_definition ad
    ON ad.code = 'prescription_required'
   AND ad.product_type_id = p.product_type_id
   AND ad.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (product_id, attribute_id) DO UPDATE SET value_boolean = EXCLUDED.value_boolean;

-- ── Shop configuration ────────────────────────────────────────
INSERT INTO shop_config (tenant_id, config_key, config_value, config_group, encrypted) VALUES
    (current_setting('app.current_tenant', true), 'cash_denominations', '1000,2000,5000,10000,20000,50000,100000,200000,500000', 'POS', FALSE)
ON CONFLICT (config_key, tenant_id) DO NOTHING;
