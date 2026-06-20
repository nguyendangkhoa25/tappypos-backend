-- ============================================================
-- TENANT SEED — DEFAULT DATA: BOOK STORE (NHÀ SÁCH)
-- PostgreSQL / shared-DB version.
-- All INSERTs are idempotent (ON CONFLICT DO NOTHING/DO UPDATE or WHERE NOT EXISTS).
-- tenant_id sourced from app.current_tenant session variable.
--
-- Dominant product type: BOOKS. A nhà sách sells books AND a long non-book tail
-- (văn phòng phẩm, đồ chơi, quà lưu niệm, dụng cụ học tập), so the file seeds the
-- BOOKS attribute domain (ISBN / tác giả / NXB / thể loại / năm XB …) plus a
-- multi-category retail taxonomy. BOOK_STORE has no seedShopTypeTemplates() branch
-- in TenantSeedService, so the default POS_RECEIPT is seeded HERE (§9).
-- ============================================================

-- ── 1. Product types (BOOKS dominant + non-book tail) ────────
INSERT INTO product_type (tenant_id, code, name, description, default_inventory_mode, default_unit) VALUES
    (current_setting('app.current_tenant', true), 'BOOKS',        'Sách',                       'Sách trong nước và ngoại văn', 'TRACKED', 'piece'),
    (current_setting('app.current_tenant', true), 'OFFICE',       'Văn phòng phẩm',             'Bút, vở, giấy và đồ dùng văn phòng', 'TRACKED', 'piece'),
    (current_setting('app.current_tenant', true), 'TOYS',         'Đồ chơi / Trò chơi',        'Đồ chơi và trò chơi trẻ em', 'TRACKED', 'piece'),
    (current_setting('app.current_tenant', true), 'CONVENIENCE',  'Quà lưu niệm / Hàng khác',   'Quà lưu niệm và hàng tiêu dùng', 'TRACKED', 'piece'),
    (current_setting('app.current_tenant', true), 'BEVERAGE',     'Đồ uống',                    'Nước giải khát', 'TRACKED', 'bottle')
ON CONFLICT (code, tenant_id) DO UPDATE SET name = EXCLUDED.name, description = EXCLUDED.description, default_inventory_mode = EXCLUDED.default_inventory_mode;

-- ── 2. Categories (book + non-book taxonomy) ─────────────────
-- category has no natural unique key, so guard each insert with WHERE NOT EXISTS.
-- Parent categories
INSERT INTO category (tenant_id, name, parent_id)
SELECT current_setting('app.current_tenant', true), v.name, NULL
FROM (VALUES
    ('Sách trong nước'), ('Sách ngoại văn'), ('Văn phòng phẩm'),
    ('Đồ chơi'), ('Quà lưu niệm'), ('Dụng cụ học tập')
) AS v(name)
WHERE NOT EXISTS (
    SELECT 1 FROM category c
    WHERE c.tenant_id = current_setting('app.current_tenant', true)
      AND c.name = v.name AND c.parent_id IS NULL
);

-- Child categories
INSERT INTO category (tenant_id, name, parent_id)
SELECT current_setting('app.current_tenant', true), c.name, p.id
FROM (VALUES
    ('Văn học',                   'Sách trong nước'),
    ('Thiếu nhi',                 'Sách trong nước'),
    ('Giáo khoa & Tham khảo',     'Sách trong nước'),
    ('Kinh tế & Kỹ năng',         'Sách trong nước'),
    ('Truyện tranh',              'Sách trong nước'),
    ('Tâm lý & Self-help',        'Sách trong nước'),
    ('Sách tiếng Anh',            'Sách ngoại văn'),
    ('Từ điển & Học ngoại ngữ',   'Sách ngoại văn'),
    ('Bút viết',                  'Văn phòng phẩm'),
    ('Vở & Sổ tay',               'Văn phòng phẩm'),
    ('Giấy & Bao bì',             'Văn phòng phẩm'),
    ('Dụng cụ vẽ',                'Dụng cụ học tập'),
    ('Balo & Cặp sách',           'Dụng cụ học tập')
) AS c(name, parent_name)
JOIN category p ON p.name = c.parent_name
    AND p.tenant_id = current_setting('app.current_tenant', true)
    AND p.parent_id IS NULL
WHERE NOT EXISTS (
    SELECT 1 FROM category ex
    WHERE ex.tenant_id = current_setting('app.current_tenant', true)
      AND ex.name = c.name AND ex.parent_id = p.id
);

-- ── 3. Vendors (publishers / distributors) ───────────────────
INSERT INTO vendors (tenant_id, name, code, contact_name, phone, payment_terms, is_active, deleted)
VALUES
    (current_setting('app.current_tenant', true), 'NXB Kim Đồng',                     'VND-001', NULL, NULL, 'NET_30', TRUE, FALSE),
    (current_setting('app.current_tenant', true), 'Nhà sách Fahasa / NXB Trẻ',        'VND-002', NULL, NULL, 'NET_30', TRUE, FALSE)
ON CONFLICT (code, tenant_id) DO NOTHING;

-- ── 5. Sample products ────────────────────────────────────────
-- Starter catalogue (per the seed convention every shop type ships 15–40 items).
-- Realistic Vietnamese nhà sách inventory: sách + văn phòng phẩm + đồ chơi.
INSERT INTO product
    (tenant_id, sku, name, description, price, cost_price, unit, product_type_id, status)
SELECT
    current_setting('app.current_tenant', true),
    CONCAT('BOK-DEMO-', LPAD(seq.n::text, 3, '0')),
    seq.item_name, seq.item_desc, seq.item_price, seq.item_cost, seq.item_unit,
    pt.id, 'ACTIVE'
FROM (VALUES
    (1,  'Đắc Nhân Tâm',                     'Dale Carnegie — kỹ năng giao tiếp',         88000,  60000,  'piece',  'BOOKS'),
    (2,  'Nhà Giả Kim',                      'Paulo Coelho — tiểu thuyết',                79000,  54000,  'piece',  'BOOKS'),
    (3,  'Tuổi Trẻ Đáng Giá Bao Nhiêu',      'Rosie Nguyễn — kỹ năng sống',               90000,  62000,  'piece',  'BOOKS'),
    (4,  'Cây Cam Ngọt Của Tôi',             'José Mauro de Vasconcelos — văn học',        98000,  68000,  'piece',  'BOOKS'),
    (5,  'Dế Mèn Phiêu Lưu Ký',              'Tô Hoài — sách thiếu nhi',                  52000,  35000,  'piece',  'BOOKS'),
    (6,  'Doraemon Tập 1',                   'Fujiko F. Fujio — truyện tranh',            18000,  12000,  'piece',  'BOOKS'),
    (7,  'Conan Tập 100',                    'Aoyama Gosho — truyện tranh',               25000,  17000,  'piece',  'BOOKS'),
    (8,  'Toán Lớp 5 - Tập 1',               'Sách giáo khoa Bộ GD&ĐT',                   18000,  13000,  'piece',  'BOOKS'),
    (9,  'Từ Điển Anh - Việt',               'Oxford — từ điển bỏ túi',                  120000,  85000,  'piece',  'BOOKS'),
    (10, 'Atomic Habits (English)',          'James Clear — bản tiếng Anh',              185000, 135000,  'piece',  'BOOKS'),
    (11, 'Bút bi Thiên Long TL-027',         'Hộp 20 cây — bút bi xanh',                  50000,  34000,  'box',    'OFFICE'),
    (12, 'Bút chì 2B Staedtler',             'Cây — bút chì gỗ 2B',                        8000,   5000,  'piece',  'OFFICE'),
    (13, 'Vở ô ly Campus 96 trang',          'Cuốn — vở học sinh',                        12000,   8000,  'piece',  'OFFICE'),
    (14, 'Giấy A4 Double A 70gsm',           'Ream 500 tờ — giấy in',                     78000,  62000,  'pack',   'OFFICE'),
    (15, 'Bộ màu sáp 24 màu',                'Hộp — sáp màu cho bé',                      45000,  30000,  'box',    'OFFICE'),
    (16, 'Thước kẻ 30cm',                    'Cây — thước nhựa trong',                     6000,   3500,  'piece',  'OFFICE'),
    (17, 'Bộ Lego lắp ráp mini',             'Hộp — đồ chơi lắp ráp',                    120000,  80000,  'box',    'TOYS'),
    (18, 'Búp bê công chúa',                 'Con — đồ chơi trẻ em',                       95000,  62000,  'piece',  'TOYS'),
    (19, 'Balo học sinh chống gù',           'Cái — balo cấp 1',                         185000, 130000,  'piece',  'CONVENIENCE'),
    (20, 'Bookmark da kẹp sách',             'Cái — quà lưu niệm',                        15000,   8000,  'piece',  'CONVENIENCE')
) AS seq(n, item_name, item_desc, item_price, item_cost, item_unit, type_code)
JOIN product_type pt
    ON pt.code = seq.type_code
   AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (sku, tenant_id) DO NOTHING;

-- ── 6. Product → category links ───────────────────────────────
INSERT INTO product_category (tenant_id, product_id, category_id)
SELECT current_setting('app.current_tenant', true), p.id, c.id
FROM (VALUES
    ('BOK-DEMO-001', 'Sách trong nước'),
    ('BOK-DEMO-002', 'Sách trong nước'),
    ('BOK-DEMO-003', 'Sách trong nước'),
    ('BOK-DEMO-004', 'Sách trong nước'),
    ('BOK-DEMO-005', 'Sách trong nước'),
    ('BOK-DEMO-006', 'Sách trong nước'),
    ('BOK-DEMO-007', 'Sách trong nước'),
    ('BOK-DEMO-008', 'Sách trong nước'),
    ('BOK-DEMO-009', 'Sách ngoại văn'),
    ('BOK-DEMO-010', 'Sách ngoại văn'),
    ('BOK-DEMO-011', 'Văn phòng phẩm'),
    ('BOK-DEMO-012', 'Văn phòng phẩm'),
    ('BOK-DEMO-013', 'Văn phòng phẩm'),
    ('BOK-DEMO-014', 'Văn phòng phẩm'),
    ('BOK-DEMO-015', 'Dụng cụ học tập'),
    ('BOK-DEMO-016', 'Văn phòng phẩm'),
    ('BOK-DEMO-017', 'Đồ chơi'),
    ('BOK-DEMO-018', 'Đồ chơi'),
    ('BOK-DEMO-019', 'Dụng cụ học tập'),
    ('BOK-DEMO-020', 'Quà lưu niệm')
) AS m(sku, cat)
JOIN product p ON p.sku = m.sku AND p.tenant_id = current_setting('app.current_tenant', true)
JOIN category c ON c.name = m.cat AND c.parent_id IS NULL
    AND c.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (product_id, category_id) DO NOTHING;

-- ── 7. Inventory for sample products ──────────────────────────
INSERT INTO inventory
    (tenant_id, product_id, quantity_in_stock, reorder_level, reorder_quantity,
     unit_cost, warehouse_location, deleted)
SELECT
    p.tenant_id, p.id, 50, 10, 30, p.cost_price, 'Kệ sách', FALSE
FROM product p
WHERE p.sku LIKE 'BOK-DEMO-%'
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
-- BOOK_STORE has no seedShopTypeTemplates() branch, so the default POS_RECEIPT
-- ("Mặc định", a standard sales receipt — adequate for books) is seeded here.
-- A nhà sách also tags shelves, so seed a "Tem sách" product stamp with barcode.
INSERT INTO print_templates (tenant_id, template_type, name, config_json, is_default) VALUES
    (current_setting('app.current_tenant', true), 'POS_RECEIPT', 'Mặc định', '{
  "headerText": "",
  "footerText": "Cảm ơn quý khách!\nHẹn gặp lại!",
  "showAddress": true,
  "showTaxId": false,
  "showOrderNumber": true,
  "showDateTime": true,
  "showCustomer": true,
  "showTaxBreakdown": false,
  "showCashDetails": true,
  "paperWidth": "80mm",
  "autoClose": true,
  "showVietQr": true
}', TRUE),
    (current_setting('app.current_tenant', true), 'PRODUCT_STAMP', 'Tem sách', '{
  "showShopName": true,
  "showSku": true,
  "showPrice": true,
  "showBarcode": true,
  "showLocation": false,
  "showBatch": false,
  "showExpiry": false,
  "labelWidth": 50,
  "labelHeight": 30
}', TRUE)
ON CONFLICT (template_type, name, tenant_id) DO NOTHING;

-- ── 11. Attribute groups (BOOKS type) ─────────────────────────
INSERT INTO attribute_group (tenant_id, product_type_id, code, name, display_order)
SELECT current_setting('app.current_tenant', true), id, 'thong_tin_sach', 'Thông tin sách', 1
FROM product_type WHERE code = 'BOOKS' AND tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name, display_order = EXCLUDED.display_order;

INSERT INTO attribute_group (tenant_id, product_type_id, code, name, display_order)
SELECT current_setting('app.current_tenant', true), id, 'phan_loai_an_ban', 'Phân loại & Ấn bản', 2
FROM product_type WHERE code = 'BOOKS' AND tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name, display_order = EXCLUDED.display_order;

-- ── 11a. Attribute definitions — group "Thông tin sách" ───────
INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'isbn', 'ISBN / Mã sách', 'STRING', TRUE, TRUE, FALSE, 1
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'thong_tin_sach'
WHERE pt.code = 'BOOKS' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'author', 'Tác giả', 'STRING', TRUE, TRUE, TRUE, 2
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'thong_tin_sach'
WHERE pt.code = 'BOOKS' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'publisher', 'Nhà xuất bản (NXB)', 'STRING', TRUE, TRUE, TRUE, 3
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'thong_tin_sach'
WHERE pt.code = 'BOOKS' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'translator', 'Người dịch', 'STRING', FALSE, TRUE, FALSE, 4
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'thong_tin_sach'
WHERE pt.code = 'BOOKS' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

-- ── 11b. Attribute definitions — group "Phân loại & Ấn bản" ───
INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'genre', 'Thể loại', 'STRING', FALSE, TRUE, TRUE, 1
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'phan_loai_an_ban'
WHERE pt.code = 'BOOKS' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'publish_year', 'Năm xuất bản', 'NUMBER', FALSE, FALSE, TRUE, 2
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'phan_loai_an_ban'
WHERE pt.code = 'BOOKS' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'language', 'Ngôn ngữ', 'STRING', FALSE, FALSE, TRUE, 3
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'phan_loai_an_ban'
WHERE pt.code = 'BOOKS' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'cover_type', 'Loại bìa (cứng/mềm)', 'STRING', FALSE, FALSE, TRUE, 4
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'phan_loai_an_ban'
WHERE pt.code = 'BOOKS' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'page_count', 'Số trang', 'NUMBER', FALSE, FALSE, FALSE, 5
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'phan_loai_an_ban'
WHERE pt.code = 'BOOKS' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

-- ── 12. Shop configuration ────────────────────────────────────
INSERT INTO shop_config (tenant_id, config_key, config_value, config_group, encrypted) VALUES
    (current_setting('app.current_tenant', true), 'cash_denominations', '1000,2000,5000,10000,20000,50000,100000,200000,500000', 'POS', FALSE)
ON CONFLICT (config_key, tenant_id) DO NOTHING;
