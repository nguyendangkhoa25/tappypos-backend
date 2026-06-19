-- ============================================================
-- TENANT SEED — DEFAULT DATA: FASHION / CLOTHING (THỜI TRANG / MAY MẶC)
-- PostgreSQL / shared-DB version.
-- All INSERTs are idempotent (ON CONFLICT DO NOTHING/DO UPDATE or WHERE NOT EXISTS).
-- tenant_id sourced from app.current_tenant session variable.
--
-- Dominant product type: CLOTHING. The defining fashion mechanism is the
-- size × color variant matrix — Size + Màu sắc variant types are pre-seeded
-- (§ "Variant types" below) so the owner can generate SKUs on their first product.
-- The POS_RECEIPT default ("Phiếu bảo hành") is seeded in Java by
-- TenantSeedService.seedShopTypeTemplates(); it is intentionally NOT seeded here.
-- ============================================================

-- ── 1. Product types (18 standard types) ─────────────────────
INSERT INTO product_type (tenant_id, code, name, description, default_inventory_mode, default_unit) VALUES
    (current_setting('app.current_tenant', true), 'CLOTHING',     'Quần áo / May mặc',          'Quần áo, trang phục và phụ kiện thời trang', 'TRACKED', 'piece'),
    (current_setting('app.current_tenant', true), 'BEAUTY',       'Làm đẹp / Chăm sóc cá nhân','Mỹ phẩm, nước hoa và phụ kiện làm đẹp', 'TRACKED', 'piece'),
    (current_setting('app.current_tenant', true), 'SPORTS',       'Thể thao / Ngoài trời',     'Đồ thể thao và phụ kiện', 'TRACKED', 'piece'),
    (current_setting('app.current_tenant', true), 'CONVENIENCE',  'Hàng tiêu dùng',             'Hàng tiêu dùng thiết yếu', 'TRACKED', 'piece'),
    (current_setting('app.current_tenant', true), 'FOOD',         'Thực phẩm',                  'Thực phẩm và đồ ăn', 'TRACKED', 'piece'),
    (current_setting('app.current_tenant', true), 'BEVERAGE',     'Đồ uống',                    'Nước giải khát, bia, nước suối', 'TRACKED', 'bottle'),
    (current_setting('app.current_tenant', true), 'DRUG',         'Dược phẩm',                  'Thuốc và sản phẩm dược', 'TRACKED', 'box'),
    (current_setting('app.current_tenant', true), 'BIKE',         'Xe đạp / Xe máy',            'Xe đạp và phụ tùng xe máy', 'UNIQUE', 'piece'),
    (current_setting('app.current_tenant', true), 'HARDWARE',     'Đồ sắt / Dụng cụ',          'Đồ sắt và dụng cụ', 'TRACKED', 'piece'),
    (current_setting('app.current_tenant', true), 'ELECTRONICS',  'Điện tử',                    'Thiết bị điện tử', 'TRACKED', 'piece'),
    (current_setting('app.current_tenant', true), 'FURNITURE',    'Đồ nội thất',                'Nội thất gia đình', 'TRACKED', 'piece'),
    (current_setting('app.current_tenant', true), 'TOYS',         'Đồ chơi / Trò chơi',        'Đồ chơi và trò chơi', 'TRACKED', 'piece'),
    (current_setting('app.current_tenant', true), 'BOOKS',        'Sách / Văn phòng phẩm',     'Sách và văn phòng phẩm', 'TRACKED', 'piece'),
    (current_setting('app.current_tenant', true), 'AUTO_PARTS',   'Phụ tùng ô tô',             'Phụ tùng và phụ kiện ô tô', 'TRACKED', 'piece'),
    (current_setting('app.current_tenant', true), 'APPLIANCES',   'Đồ gia dụng',                'Thiết bị gia dụng', 'TRACKED', 'piece'),
    (current_setting('app.current_tenant', true), 'OFFICE',       'Văn phòng phẩm',             'Đồ dùng văn phòng', 'TRACKED', 'piece'),
    (current_setting('app.current_tenant', true), 'PET',          'Thú cưng',                  'Thức ăn và phụ kiện thú cưng', 'TRACKED', 'piece'),
    (current_setting('app.current_tenant', true), 'HEALTH',       'Sức khỏe / Dinh dưỡng',     'Sản phẩm sức khỏe và dinh dưỡng', 'TRACKED', 'piece')
ON CONFLICT (code, tenant_id) DO UPDATE SET name = EXCLUDED.name, description = EXCLUDED.description, default_inventory_mode = EXCLUDED.default_inventory_mode;

-- ── 2. Categories (fashion taxonomy) ─────────────────────────
-- category has no natural unique key, so guard each insert with WHERE NOT EXISTS.
-- Parent categories
INSERT INTO category (tenant_id, name, parent_id)
SELECT current_setting('app.current_tenant', true), v.name, NULL
FROM (VALUES
    ('Áo'), ('Quần'), ('Váy & Đầm'), ('Đồ thể thao'),
    ('Đồ lót & Đồ ngủ'), ('Giày dép'), ('Phụ kiện'), ('Thời trang trẻ em')
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
    ('Áo thun',          'Áo'),
    ('Áo sơ mi',         'Áo'),
    ('Áo polo',          'Áo'),
    ('Áo khoác',         'Áo'),
    ('Áo len & Hoodie',  'Áo'),
    ('Quần jean',        'Quần'),
    ('Quần tây',         'Quần'),
    ('Quần short',       'Quần'),
    ('Quần kaki',        'Quần'),
    ('Quần legging',     'Quần'),
    ('Chân váy',         'Váy & Đầm'),
    ('Đầm liền',         'Váy & Đầm'),
    ('Váy maxi',         'Váy & Đầm'),
    ('Giày thể thao',    'Giày dép'),
    ('Giày cao gót',     'Giày dép'),
    ('Dép & Sandal',     'Giày dép'),
    ('Túi xách',         'Phụ kiện'),
    ('Thắt lưng',        'Phụ kiện'),
    ('Mũ & Nón',         'Phụ kiện'),
    ('Khăn choàng',      'Phụ kiện')
) AS c(name, parent_name)
JOIN category p ON p.name = c.parent_name
    AND p.tenant_id = current_setting('app.current_tenant', true)
    AND p.parent_id IS NULL
WHERE NOT EXISTS (
    SELECT 1 FROM category ex
    WHERE ex.tenant_id = current_setting('app.current_tenant', true)
      AND ex.name = c.name AND ex.parent_id = p.id
);

-- ── 3. Vendors ────────────────────────────────────────────────
INSERT INTO vendors (tenant_id, name, code, contact_name, phone, payment_terms, is_active, deleted)
VALUES
    (current_setting('app.current_tenant', true), 'Xưởng may & Nhà cung cấp thời trang', 'VND-001', NULL, NULL, 'NET_30', TRUE, FALSE),
    (current_setting('app.current_tenant', true), 'Nhà phân phối phụ kiện & giày dép',   'VND-002', NULL, NULL, 'NET_30', TRUE, FALSE)
ON CONFLICT (code, tenant_id) DO NOTHING;

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
-- NOTE: the default POS_RECEIPT ("Phiếu bảo hành") is seeded by
-- TenantSeedService.seedShopTypeTemplates() in Java — do not duplicate it here.
-- A clothing shop tags garments, so seed a hang-tag product stamp (SKU + price + barcode).
INSERT INTO print_templates (tenant_id, template_type, name, config_json, is_default) VALUES
    (current_setting('app.current_tenant', true), 'PRODUCT_STAMP', 'Tem treo sản phẩm', '{
  "showShopName": true,
  "showSku": true,
  "showPrice": true,
  "showBarcode": true,
  "showLocation": false,
  "showBatch": false,
  "showExpiry": false,
  "labelWidth": 40,
  "labelHeight": 30
}', TRUE)
ON CONFLICT (template_type, name, tenant_id) DO NOTHING;

-- ── 11. Attribute groups (CLOTHING type) ──────────────────────
INSERT INTO attribute_group (tenant_id, product_type_id, code, name, display_order)
SELECT current_setting('app.current_tenant', true), id, 'basic_info', 'Thông tin sản phẩm', 1
FROM product_type WHERE code = 'CLOTHING' AND tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name, display_order = EXCLUDED.display_order;

INSERT INTO attribute_group (tenant_id, product_type_id, code, name, display_order)
SELECT current_setting('app.current_tenant', true), id, 'size_color', 'Kích thước & Màu sắc', 2
FROM product_type WHERE code = 'CLOTHING' AND tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name, display_order = EXCLUDED.display_order;

-- ── 11b. Attribute definitions (CLOTHING type) ────────────────
-- Group: basic_info — describes the style
INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'brand', 'Thương hiệu', 'STRING', FALSE, TRUE, TRUE, 1
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'basic_info'
WHERE pt.code = 'CLOTHING' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'material', 'Chất liệu', 'STRING', FALSE, TRUE, TRUE, 2
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'basic_info'
WHERE pt.code = 'CLOTHING' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'gender', 'Đối tượng (Nam/Nữ/Unisex/Trẻ em)', 'STRING', FALSE, FALSE, TRUE, 3
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'basic_info'
WHERE pt.code = 'CLOTHING' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'season', 'Mùa vụ (Xuân-Hè/Thu-Đông/Quanh năm)', 'STRING', FALSE, FALSE, TRUE, 4
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'basic_info'
WHERE pt.code = 'CLOTHING' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

-- Bộ sưu tập (collection) — searchable + filterable so styles can be grouped by drop/collection.
INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'collection', 'Bộ sưu tập', 'STRING', FALSE, TRUE, TRUE, 8
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'basic_info'
WHERE pt.code = 'CLOTHING' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'style', 'Kiểu dáng', 'STRING', FALSE, TRUE, TRUE, 5
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'basic_info'
WHERE pt.code = 'CLOTHING' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'pattern', 'Họa tiết (Trơn/Kẻ sọc/Hoa/Caro)', 'STRING', FALSE, FALSE, TRUE, 6
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'basic_info'
WHERE pt.code = 'CLOTHING' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'country_of_origin', 'Xuất xứ', 'STRING', FALSE, TRUE, TRUE, 7
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'basic_info'
WHERE pt.code = 'CLOTHING' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'care_instruction', 'Hướng dẫn giặt là', 'TEXT', FALSE, FALSE, FALSE, 8
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'basic_info'
WHERE pt.code = 'CLOTHING' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

-- Group: size_color — single-SKU products set these directly; variant products
-- carry size/color per SKU instead, so these are optional (required=FALSE).
INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'size', 'Kích thước (S/M/L/XL/XXL)', 'STRING', FALSE, FALSE, TRUE, 1
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'size_color'
WHERE pt.code = 'CLOTHING' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'color', 'Màu sắc', 'STRING', FALSE, FALSE, TRUE, 2
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'size_color'
WHERE pt.code = 'CLOTHING' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'fit', 'Form dáng (Ôm/Vừa/Rộng)', 'STRING', FALSE, FALSE, TRUE, 3
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'size_color'
WHERE pt.code = 'CLOTHING' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

-- ── 11c. Variant types (size × color) — the fashion SKU axes ───
-- Seeded global (product_type_id = NULL) so they also apply to accessories/shoes.
-- variant_types has no natural unique key, so guard with WHERE NOT EXISTS.
INSERT INTO variant_types (tenant_id, name, description, product_type_id, sort_order)
SELECT current_setting('app.current_tenant', true), 'Kích thước', 'Size quần áo / giày dép', NULL, 1
WHERE NOT EXISTS (
    SELECT 1 FROM variant_types
    WHERE tenant_id = current_setting('app.current_tenant', true) AND name = 'Kích thước' AND deleted = FALSE
);

INSERT INTO variant_types (tenant_id, name, description, product_type_id, sort_order)
SELECT current_setting('app.current_tenant', true), 'Màu sắc', 'Màu sắc sản phẩm', NULL, 2
WHERE NOT EXISTS (
    SELECT 1 FROM variant_types
    WHERE tenant_id = current_setting('app.current_tenant', true) AND name = 'Màu sắc' AND deleted = FALSE
);

-- Size options
INSERT INTO variant_type_options (tenant_id, variant_type_id, value, sort_order)
SELECT current_setting('app.current_tenant', true), vt.id, opt.value, opt.ord
FROM variant_types vt
CROSS JOIN (VALUES ('S', 1), ('M', 2), ('L', 3), ('XL', 4), ('XXL', 5)) AS opt(value, ord)
WHERE vt.tenant_id = current_setting('app.current_tenant', true)
  AND vt.name = 'Kích thước' AND vt.deleted = FALSE
  AND NOT EXISTS (
      SELECT 1 FROM variant_type_options o
      WHERE o.variant_type_id = vt.id AND o.value = opt.value AND o.deleted = FALSE
  );

-- Color options
INSERT INTO variant_type_options (tenant_id, variant_type_id, value, sort_order)
SELECT current_setting('app.current_tenant', true), vt.id, opt.value, opt.ord
FROM variant_types vt
CROSS JOIN (VALUES
    ('Trắng', 1), ('Đen', 2), ('Xám', 3), ('Xanh navy', 4), ('Đỏ', 5), ('Be', 6)
) AS opt(value, ord)
WHERE vt.tenant_id = current_setting('app.current_tenant', true)
  AND vt.name = 'Màu sắc' AND vt.deleted = FALSE
  AND NOT EXISTS (
      SELECT 1 FROM variant_type_options o
      WHERE o.variant_type_id = vt.id AND o.value = opt.value AND o.deleted = FALSE
  );

-- ── 12. Shop configuration ────────────────────────────────────
INSERT INTO shop_config (tenant_id, config_key, config_value, config_group, encrypted) VALUES
    (current_setting('app.current_tenant', true), 'cash_denominations', '1000,2000,5000,10000,20000,50000,100000,200000,500000', 'POS', FALSE)
ON CONFLICT (config_key, tenant_id) DO NOTHING;
