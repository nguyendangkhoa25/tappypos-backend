-- V010: Add product_suggestions for pub shop types (PUB, PUB_SEAFOOD, PUB_GOAT, PUB_BEEF)

INSERT INTO product_suggestions
    (name, name_en, emoji, default_price, unit, product_type_code, dynamic_price, shop_types, category_name, display_order)
VALUES
-- ── Shared pub beverages ──────────────────────────────────────────────────
('Bia Saigon Special',   'Saigon Special Beer',    '🍺', 20000,  'Lon',  'BEVERAGE', FALSE, ARRAY['PUB','PUB_SEAFOOD','PUB_GOAT','PUB_BEEF'], 'Bia & Rượu',        1),
('Bia Tiger Crystal',    'Tiger Crystal Beer',     '🍺', 22000,  'Lon',  'BEVERAGE', FALSE, ARRAY['PUB','PUB_SEAFOOD','PUB_GOAT','PUB_BEEF'], 'Bia & Rượu',        2),
('Bia Heineken',         'Heineken Beer',          '🍺', 25000,  'Lon',  'BEVERAGE', FALSE, ARRAY['PUB','PUB_SEAFOOD','PUB_GOAT','PUB_BEEF'], 'Bia & Rượu',        3),
('Bia 333',              '333 Beer',               '🍺', 18000,  'Lon',  'BEVERAGE', FALSE, ARRAY['PUB','PUB_SEAFOOD','PUB_GOAT','PUB_BEEF'], 'Bia & Rượu',        4),
('Két bia Saigon',       'Case of Saigon Beer',    '🍺', 400000, 'Két',  'BEVERAGE', FALSE, ARRAY['PUB','PUB_SEAFOOD','PUB_GOAT','PUB_BEEF'], 'Bia & Rượu',        5),
('Rượu đế / rượu gạo',  'Rice Wine',              '🥃', 50000,  'Chai', 'BEVERAGE', FALSE, ARRAY['PUB','PUB_SEAFOOD','PUB_GOAT','PUB_BEEF'], 'Bia & Rượu',        6),
('Nước ngọt (lon)',      'Soft Drink (can)',       '🥤', 12000,  'Lon',  'BEVERAGE', FALSE, ARRAY['PUB','PUB_SEAFOOD','PUB_GOAT','PUB_BEEF'], 'Bia & Rượu',        7),
('Nước suối',            'Water',                  '💧', 5000,   'Chai', 'BEVERAGE', FALSE, ARRAY['PUB','PUB_SEAFOOD','PUB_GOAT','PUB_BEEF'], 'Bia & Rượu',        8),

-- ── General pub snacks / mồi ─────────────────────────────────────────────
('Đậu phộng rang muối',  'Salted Roasted Peanuts', '🥜', 30000,  'Đĩa', 'FOOD', FALSE, ARRAY['PUB','PUB_SEAFOOD','PUB_GOAT','PUB_BEEF'], 'Đồ nhậu',            9),
('Khô mực nướng',        'Grilled Dried Squid',    '🦑', 80000,  'Đĩa', 'FOOD', FALSE, ARRAY['PUB','PUB_SEAFOOD','PUB_GOAT','PUB_BEEF'], 'Đồ nhậu',            10),
('Hột vịt lộn',          'Balut Eggs',             '🥚', 15000,  'Trứng','FOOD', FALSE, ARRAY['PUB','PUB_SEAFOOD','PUB_GOAT','PUB_BEEF'], 'Đồ nhậu',            11),
('Gà nướng muối ớt',     'Salt & Chili Grilled Chicken', '🍗', 180000, 'Con', 'FOOD', FALSE, ARRAY['PUB'], 'Đồ nhậu',             12),

-- ── PUB_SEAFOOD — hải sản ────────────────────────────────────────────────
('Tôm sú nướng muối ớt', 'Grilled Tiger Prawns',   '🦐', 250000, 'Kg',  'FOOD', FALSE, ARRAY['PUB_SEAFOOD'], 'Hải sản tươi sống',  1),
('Cua rang muối',         'Salt & Pepper Crab',     '🦀', 350000, 'Con', 'FOOD', FALSE, ARRAY['PUB_SEAFOOD'], 'Hải sản tươi sống',  2),
('Mực chiên giòn',        'Crispy Fried Squid',     '🦑', 180000, 'Đĩa', 'FOOD', FALSE, ARRAY['PUB_SEAFOOD'], 'Hải sản tươi sống',  3),
('Nghêu hấp sả',          'Steamed Clams with Lemongrass', '🐚', 120000, 'Kg', 'FOOD', FALSE, ARRAY['PUB_SEAFOOD'], 'Hải sản tươi sống', 4),
('Bạch tuộc nướng',       'Grilled Octopus',        '🐙', 220000, 'Đĩa', 'FOOD', FALSE, ARRAY['PUB_SEAFOOD'], 'Hải sản tươi sống',  5),
('Cá lóc nướng trui',     'Grilled Snakehead Fish', '🐟', 200000, 'Con', 'FOOD', FALSE, ARRAY['PUB_SEAFOOD'], 'Hải sản tươi sống',  6),
('Ghẹ hấp bia',           'Beer-steamed Blue Crab', '🦀', 280000, 'Con', 'FOOD', FALSE, ARRAY['PUB_SEAFOOD'], 'Hải sản tươi sống',  7),
('Lẩu hải sản thập cẩm',  'Mixed Seafood Hot Pot',  '🫕', 350000, 'Nồi', 'FOOD', FALSE, ARRAY['PUB_SEAFOOD'], 'Lẩu hải sản',        8),
('Lẩu tôm cua',           'Prawn & Crab Hot Pot',   '🫕', 420000, 'Nồi', 'FOOD', FALSE, ARRAY['PUB_SEAFOOD'], 'Lẩu hải sản',        9),

-- ── PUB_GOAT — thịt dê ───────────────────────────────────────────────────
('Thịt dê xào lăn',      'Sautéed Goat with Lemongrass', '🐐', 200000, 'Đĩa', 'FOOD', FALSE, ARRAY['PUB_GOAT'], 'Thịt dê',           1),
('Dê nướng nguyên con',  'Whole Roasted Goat',     '🐐', 1500000,'Con', 'FOOD', FALSE, ARRAY['PUB_GOAT'], 'Thịt dê',              2),
('Dê nướng bếp than',    'Charcoal Grilled Goat',  '🔥', 250000, 'Đĩa', 'FOOD', FALSE, ARRAY['PUB_GOAT'], 'Thịt dê',              3),
('Lẩu dê',               'Goat Hot Pot',           '🫕', 350000, 'Nồi', 'FOOD', FALSE, ARRAY['PUB_GOAT'], 'Thịt dê',              4),
('Tiết canh dê',         'Goat Blood Pudding',     '🍲', 80000,  'Bát', 'FOOD', FALSE, ARRAY['PUB_GOAT'], 'Thịt dê',              5),
('Dê hấp gừng',          'Steamed Goat with Ginger','🐐', 220000, 'Đĩa', 'FOOD', FALSE, ARRAY['PUB_GOAT'], 'Thịt dê',             6),
('Dồi dê nướng',         'Grilled Goat Sausage',   '🌭', 150000, 'Đĩa', 'FOOD', FALSE, ARRAY['PUB_GOAT'], 'Thịt dê',              7),
('Dê sốt vang',          'Goat in Red Wine Sauce', '🍷', 200000, 'Đĩa', 'FOOD', FALSE, ARRAY['PUB_GOAT'], 'Thịt dê',              8),

-- ── PUB_BEEF — thịt bò ───────────────────────────────────────────────────
('Bò nhúng dấm',         'Beef Dipped in Vinegar', '🥢', 280000, 'Phần','FOOD', FALSE, ARRAY['PUB_BEEF'], 'Thịt bò',              1),
('Bò nướng ngũ vị',      'Five-Spice Grilled Beef','🔥', 250000, 'Đĩa', 'FOOD', FALSE, ARRAY['PUB_BEEF'], 'Thịt bò',              2),
('Lẩu bò',               'Beef Hot Pot',           '🫕', 320000, 'Nồi', 'FOOD', FALSE, ARRAY['PUB_BEEF'], 'Thịt bò',              3),
('Bắp bò kho gừng',      'Braised Beef with Ginger','🍲', 180000, 'Đĩa', 'FOOD', FALSE, ARRAY['PUB_BEEF'], 'Thịt bò',             4),
('Gân bò hầm',           'Braised Beef Tendon',    '🍖', 150000, 'Đĩa', 'FOOD', FALSE, ARRAY['PUB_BEEF'], 'Thịt bò',              5),
('Bò tái chanh',         'Rare Beef with Lime',    '🍋', 170000, 'Đĩa', 'FOOD', FALSE, ARRAY['PUB_BEEF'], 'Thịt bò',              6),
('Bò nướng lá lốt',      'Beef Wrapped in Betel Leaf','🌿', 180000, 'Đĩa', 'FOOD', FALSE, ARRAY['PUB_BEEF'], 'Thịt bò',            7),
('Bò kho bánh mì',       'Beef Stew with Bread',   '🥖', 120000, 'Bát', 'FOOD', FALSE, ARRAY['PUB_BEEF'], 'Thịt bò',              8)
ON CONFLICT (name) DO NOTHING;
