-- ════════════════════════════════════════════════════════════
-- V012: product_suggestions seed data for the F&B verticals
--
-- One INSERT statement per shop type (names are unique within a statement).
-- The ON CONFLICT (name) DO UPDATE merges the shop_types arrays, so an item
-- shared across verticals (Nước suối, Bia Saigon, Trà đá …) accumulates every
-- shop type that uses it instead of failing the uq_product_suggestion_name
-- constraint or overwriting prior tags. Prices/units/categories come from the
-- FIRST statement that creates a given name; later statements only widen scope.
--
-- display_order convention within a shop: 10 = mains, 50 = sides/extras, 90 = drinks.
-- ════════════════════════════════════════════════════════════

-- ── PHO_NOODLE — Quán phở / bún bò ────────────────────────────
INSERT INTO product_suggestions (name, emoji, default_price, unit, product_type_code, dynamic_price, shop_types, display_order, category_name) VALUES
('Phở bò tái','🍜',50000,'Tô','FOOD',FALSE,ARRAY['PHO_NOODLE'],10,'Món chính'),
('Phở bò nạm','🍜',50000,'Tô','FOOD',FALSE,ARRAY['PHO_NOODLE'],10,'Món chính'),
('Phở bò gân','🍜',55000,'Tô','FOOD',FALSE,ARRAY['PHO_NOODLE'],10,'Món chính'),
('Phở bò tái nạm','🍜',55000,'Tô','FOOD',FALSE,ARRAY['PHO_NOODLE'],10,'Món chính'),
('Phở bò tái gân','🍜',55000,'Tô','FOOD',FALSE,ARRAY['PHO_NOODLE'],10,'Món chính'),
('Phở bò viên','🍜',50000,'Tô','FOOD',FALSE,ARRAY['PHO_NOODLE'],10,'Món chính'),
('Phở bò đặc biệt','🍜',70000,'Tô','FOOD',FALSE,ARRAY['PHO_NOODLE'],10,'Món chính'),
('Phở gà','🍜',45000,'Tô','FOOD',FALSE,ARRAY['PHO_NOODLE'],10,'Món chính'),
('Bún bò Huế','🍲',50000,'Tô','FOOD',FALSE,ARRAY['PHO_NOODLE'],10,'Món chính'),
('Bún bò giò heo','🍲',60000,'Tô','FOOD',FALSE,ARRAY['PHO_NOODLE'],10,'Món chính'),
('Bún bò tái','🍲',50000,'Tô','FOOD',FALSE,ARRAY['PHO_NOODLE'],10,'Món chính'),
('Bún bò đặc biệt','🍲',70000,'Tô','FOOD',FALSE,ARRAY['PHO_NOODLE'],10,'Món chính'),
('Miến gà','🍜',45000,'Tô','FOOD',FALSE,ARRAY['PHO_NOODLE'],10,'Món chính'),
('Miến đùi gà','🍜',50000,'Tô','FOOD',FALSE,ARRAY['PHO_NOODLE'],10,'Món chính'),
('Miến gà xé','🍜',45000,'Tô','FOOD',FALSE,ARRAY['PHO_NOODLE'],10,'Món chính'),
('Hủ tiếu Nam Vang','🍜',50000,'Tô','FOOD',FALSE,ARRAY['PHO_NOODLE'],10,'Món chính'),
('Thêm tái','🥩',20000,'Phần','FOOD',FALSE,ARRAY['PHO_NOODLE'],50,'Gọi thêm'),
('Thêm nạm','🥩',20000,'Phần','FOOD',FALSE,ARRAY['PHO_NOODLE'],50,'Gọi thêm'),
('Thêm gân','🥩',25000,'Phần','FOOD',FALSE,ARRAY['PHO_NOODLE'],50,'Gọi thêm'),
('Thêm bò viên','🍢',20000,'Phần','FOOD',FALSE,ARRAY['PHO_NOODLE'],50,'Gọi thêm'),
('Trứng chần','🥚',10000,'Quả','FOOD',FALSE,ARRAY['PHO_NOODLE'],50,'Gọi thêm'),
('Quẩy','🥖',10000,'Phần','FOOD',FALSE,ARRAY['PHO_NOODLE'],50,'Gọi thêm'),
('Trà đá','🧊',3000,'Ly','BEVERAGE',FALSE,ARRAY['PHO_NOODLE'],90,'Nước uống'),
('Nước suối','💧',5000,'Chai','BEVERAGE',FALSE,ARRAY['PHO_NOODLE'],90,'Nước uống'),
('Coca Cola','🥤',12000,'Lon','BEVERAGE',FALSE,ARRAY['PHO_NOODLE'],90,'Nước uống'),
('Sữa đậu nành','🥛',10000,'Ly','BEVERAGE',FALSE,ARRAY['PHO_NOODLE'],90,'Nước uống'),
('Trà xanh không độ','🍵',15000,'Chai','BEVERAGE',FALSE,ARRAY['PHO_NOODLE'],90,'Nước uống')
ON CONFLICT (name) DO UPDATE SET shop_types = ARRAY(SELECT DISTINCT u FROM unnest(product_suggestions.shop_types || EXCLUDED.shop_types) AS u);

-- ── BANH_MI — Quán bánh mì ────────────────────────────────────
INSERT INTO product_suggestions (name, emoji, default_price, unit, product_type_code, dynamic_price, shop_types, display_order, category_name) VALUES
('Bánh mì thịt nguội','🥖',20000,'Ổ','FOOD',FALSE,ARRAY['BANH_MI'],10,'Bánh mì'),
('Bánh mì chả lụa','🥖',20000,'Ổ','FOOD',FALSE,ARRAY['BANH_MI'],10,'Bánh mì'),
('Bánh mì xíu mại','🥖',25000,'Ổ','FOOD',FALSE,ARRAY['BANH_MI'],10,'Bánh mì'),
('Bánh mì thịt nướng','🥖',25000,'Ổ','FOOD',FALSE,ARRAY['BANH_MI'],10,'Bánh mì'),
('Bánh mì gà','🥖',25000,'Ổ','FOOD',FALSE,ARRAY['BANH_MI'],10,'Bánh mì'),
('Bánh mì pate trứng','🥖',18000,'Ổ','FOOD',FALSE,ARRAY['BANH_MI'],10,'Bánh mì'),
('Bánh mì ốp la','🍳',25000,'Ổ','FOOD',FALSE,ARRAY['BANH_MI'],10,'Bánh mì'),
('Bánh mì chả cá','🥖',25000,'Ổ','FOOD',FALSE,ARRAY['BANH_MI'],10,'Bánh mì'),
('Bánh mì heo quay','🥖',30000,'Ổ','FOOD',FALSE,ARRAY['BANH_MI'],10,'Bánh mì'),
('Bánh mì bò kho','🥖',35000,'Phần','FOOD',FALSE,ARRAY['BANH_MI'],10,'Bánh mì'),
('Bánh mì xíu mại chén','🍲',30000,'Phần','FOOD',FALSE,ARRAY['BANH_MI'],10,'Bánh mì'),
('Bánh mì không','🥖',5000,'Ổ','FOOD',FALSE,ARRAY['BANH_MI'],10,'Bánh mì'),
('Thêm chả','🍖',10000,'Phần','FOOD',FALSE,ARRAY['BANH_MI'],50,'Gọi thêm'),
('Thêm trứng','🥚',7000,'Quả','FOOD',FALSE,ARRAY['BANH_MI'],50,'Gọi thêm'),
('Thêm pate','🥫',5000,'Phần','FOOD',FALSE,ARRAY['BANH_MI'],50,'Gọi thêm'),
('Cà phê sữa đá','☕',15000,'Ly','BEVERAGE',FALSE,ARRAY['BANH_MI'],90,'Nước uống'),
('Trà tắc','🍵',12000,'Ly','BEVERAGE',FALSE,ARRAY['BANH_MI'],90,'Nước uống'),
('Sữa đậu nành','🥛',10000,'Ly','BEVERAGE',FALSE,ARRAY['BANH_MI'],90,'Nước uống'),
('Nước suối','💧',5000,'Chai','BEVERAGE',FALSE,ARRAY['BANH_MI'],90,'Nước uống')
ON CONFLICT (name) DO UPDATE SET shop_types = ARRAY(SELECT DISTINCT u FROM unnest(product_suggestions.shop_types || EXCLUDED.shop_types) AS u);

-- ── BANH_CUON — Quán bánh cuốn ────────────────────────────────
INSERT INTO product_suggestions (name, emoji, default_price, unit, product_type_code, dynamic_price, shop_types, display_order, category_name) VALUES
('Bánh cuốn thịt','🫓',30000,'Phần','FOOD',FALSE,ARRAY['BANH_CUON'],10,'Bánh cuốn'),
('Bánh cuốn trứng','🫓',35000,'Phần','FOOD',FALSE,ARRAY['BANH_CUON'],10,'Bánh cuốn'),
('Bánh cuốn chả','🫓',35000,'Phần','FOOD',FALSE,ARRAY['BANH_CUON'],10,'Bánh cuốn'),
('Bánh cuốn tôm','🫓',40000,'Phần','FOOD',FALSE,ARRAY['BANH_CUON'],10,'Bánh cuốn'),
('Bánh cuốn thập cẩm','🫓',45000,'Phần','FOOD',FALSE,ARRAY['BANH_CUON'],10,'Bánh cuốn'),
('Bánh cuốn chay','🫓',25000,'Phần','FOOD',FALSE,ARRAY['BANH_CUON'],10,'Bánh cuốn'),
('Bánh ướt lòng gà','🍽️',35000,'Phần','FOOD',FALSE,ARRAY['BANH_CUON'],10,'Bánh cuốn'),
('Bánh ướt','🫓',25000,'Phần','FOOD',FALSE,ARRAY['BANH_CUON'],10,'Bánh cuốn'),
('Thêm chả lụa','🍖',10000,'Phần','FOOD',FALSE,ARRAY['BANH_CUON'],50,'Gọi thêm'),
('Thêm nem chua','🌭',10000,'Phần','FOOD',FALSE,ARRAY['BANH_CUON'],50,'Gọi thêm'),
('Thêm bánh','🫓',10000,'Phần','FOOD',FALSE,ARRAY['BANH_CUON'],50,'Gọi thêm'),
('Trà đá','🧊',3000,'Ly','BEVERAGE',FALSE,ARRAY['BANH_CUON'],90,'Nước uống'),
('Sữa đậu nành','🥛',10000,'Ly','BEVERAGE',FALSE,ARRAY['BANH_CUON'],90,'Nước uống'),
('Nước ngọt','🥤',15000,'Lon','BEVERAGE',FALSE,ARRAY['BANH_CUON'],90,'Nước uống'),
('Nước suối','💧',5000,'Chai','BEVERAGE',FALSE,ARRAY['BANH_CUON'],90,'Nước uống')
ON CONFLICT (name) DO UPDATE SET shop_types = ARRAY(SELECT DISTINCT u FROM unnest(product_suggestions.shop_types || EXCLUDED.shop_types) AS u);

-- ── BUN_RIEU — Quán bún / bún riêu ────────────────────────────
INSERT INTO product_suggestions (name, emoji, default_price, unit, product_type_code, dynamic_price, shop_types, display_order, category_name) VALUES
('Bún riêu cua','🦀',40000,'Tô','FOOD',FALSE,ARRAY['BUN_RIEU'],10,'Món chính'),
('Bún riêu giò','🦀',50000,'Tô','FOOD',FALSE,ARRAY['BUN_RIEU'],10,'Món chính'),
('Bún riêu ốc','🦀',50000,'Tô','FOOD',FALSE,ARRAY['BUN_RIEU'],10,'Món chính'),
('Bún ốc','🐚',45000,'Tô','FOOD',FALSE,ARRAY['BUN_RIEU'],10,'Món chính'),
('Bún mọc','🍲',45000,'Tô','FOOD',FALSE,ARRAY['BUN_RIEU'],10,'Món chính'),
('Bún chả','🍢',50000,'Phần','FOOD',FALSE,ARRAY['BUN_RIEU'],10,'Món chính'),
('Bún thịt nướng','🍢',45000,'Tô','FOOD',FALSE,ARRAY['BUN_RIEU'],10,'Món chính'),
('Bún chả cá','🐟',45000,'Tô','FOOD',FALSE,ARRAY['BUN_RIEU'],10,'Món chính'),
('Canh bún','🍲',40000,'Tô','FOOD',FALSE,ARRAY['BUN_RIEU'],10,'Món chính'),
('Thêm riêu','🦀',15000,'Phần','FOOD',FALSE,ARRAY['BUN_RIEU'],50,'Gọi thêm'),
('Thêm giò','🍖',15000,'Phần','FOOD',FALSE,ARRAY['BUN_RIEU'],50,'Gọi thêm'),
('Thêm ốc','🐚',20000,'Phần','FOOD',FALSE,ARRAY['BUN_RIEU'],50,'Gọi thêm'),
('Thêm đậu hũ','🧈',10000,'Phần','FOOD',FALSE,ARRAY['BUN_RIEU'],50,'Gọi thêm'),
('Trà đá','🧊',3000,'Ly','BEVERAGE',FALSE,ARRAY['BUN_RIEU'],90,'Nước uống'),
('Nước ngọt','🥤',15000,'Lon','BEVERAGE',FALSE,ARRAY['BUN_RIEU'],90,'Nước uống'),
('Nước suối','💧',5000,'Chai','BEVERAGE',FALSE,ARRAY['BUN_RIEU'],90,'Nước uống'),
('Sữa đậu nành','🥛',10000,'Ly','BEVERAGE',FALSE,ARRAY['BUN_RIEU'],90,'Nước uống')
ON CONFLICT (name) DO UPDATE SET shop_types = ARRAY(SELECT DISTINCT u FROM unnest(product_suggestions.shop_types || EXCLUDED.shop_types) AS u);

-- ── OFFICE_RICE — Quán cơm văn phòng ──────────────────────────
INSERT INTO product_suggestions (name, emoji, default_price, unit, product_type_code, dynamic_price, shop_types, display_order, category_name) VALUES
('Cơm sườn','🍱',40000,'Phần','FOOD',FALSE,ARRAY['OFFICE_RICE'],10,'Cơm phần'),
('Cơm gà','🍱',40000,'Phần','FOOD',FALSE,ARRAY['OFFICE_RICE'],10,'Cơm phần'),
('Cơm cá kho','🍱',40000,'Phần','FOOD',FALSE,ARRAY['OFFICE_RICE'],10,'Cơm phần'),
('Cơm thịt kho trứng','🍱',40000,'Phần','FOOD',FALSE,ARRAY['OFFICE_RICE'],10,'Cơm phần'),
('Cơm sườn trứng','🍱',45000,'Phần','FOOD',FALSE,ARRAY['OFFICE_RICE'],10,'Cơm phần'),
('Cơm gà chiên mắm','🍱',45000,'Phần','FOOD',FALSE,ARRAY['OFFICE_RICE'],10,'Cơm phần'),
('Cơm bò xào','🍱',45000,'Phần','FOOD',FALSE,ARRAY['OFFICE_RICE'],10,'Cơm phần'),
('Cơm tôm rim','🍱',45000,'Phần','FOOD',FALSE,ARRAY['OFFICE_RICE'],10,'Cơm phần'),
('Cơm chay đậu hũ','🥗',35000,'Phần','FOOD',FALSE,ARRAY['OFFICE_RICE'],10,'Cơm phần'),
('Cơm thập cẩm','🍱',50000,'Phần','FOOD',FALSE,ARRAY['OFFICE_RICE'],10,'Cơm phần'),
('Cơm trắng','🍚',10000,'Chén','FOOD',FALSE,ARRAY['OFFICE_RICE'],50,'Món thêm'),
('Canh chua rau','🥬',10000,'Tô','FOOD',FALSE,ARRAY['OFFICE_RICE'],50,'Món thêm'),
('Trứng chiên','🍳',10000,'Phần','FOOD',FALSE,ARRAY['OFFICE_RICE'],50,'Món thêm'),
('Thêm món mặn','🍖',15000,'Phần','FOOD',FALSE,ARRAY['OFFICE_RICE'],50,'Món thêm'),
('Rau xào','🥬',15000,'Phần','FOOD',FALSE,ARRAY['OFFICE_RICE'],50,'Món thêm'),
('Trà đá','🧊',3000,'Ly','BEVERAGE',FALSE,ARRAY['OFFICE_RICE'],90,'Nước uống'),
('Nước ngọt','🥤',15000,'Lon','BEVERAGE',FALSE,ARRAY['OFFICE_RICE'],90,'Nước uống'),
('Nước suối','💧',5000,'Chai','BEVERAGE',FALSE,ARRAY['OFFICE_RICE'],90,'Nước uống')
ON CONFLICT (name) DO UPDATE SET shop_types = ARRAY(SELECT DISTINCT u FROM unnest(product_suggestions.shop_types || EXCLUDED.shop_types) AS u);

-- ── CLAY_POT_RICE — Quán cơm niêu ─────────────────────────────
INSERT INTO product_suggestions (name, emoji, default_price, unit, product_type_code, dynamic_price, shop_types, display_order, category_name) VALUES
('Cơm niêu trắng','🍚',30000,'Phần','FOOD',FALSE,ARRAY['CLAY_POT_RICE'],10,'Cơm niêu'),
('Cơm niêu cá kho tộ','🍲',90000,'Phần','FOOD',FALSE,ARRAY['CLAY_POT_RICE'],10,'Cơm niêu'),
('Cơm niêu sườn nướng','🍖',80000,'Phần','FOOD',FALSE,ARRAY['CLAY_POT_RICE'],10,'Cơm niêu'),
('Cá lóc kho tộ','🐟',120000,'Phần','FOOD',FALSE,ARRAY['CLAY_POT_RICE'],50,'Món ăn kèm'),
('Gà kho gừng','🍗',120000,'Phần','FOOD',FALSE,ARRAY['CLAY_POT_RICE'],50,'Món ăn kèm'),
('Thịt kho tàu','🍖',100000,'Phần','FOOD',FALSE,ARRAY['CLAY_POT_RICE'],50,'Món ăn kèm'),
('Tôm rim','🦐',120000,'Phần','FOOD',FALSE,ARRAY['CLAY_POT_RICE'],50,'Món ăn kèm'),
('Mực xào','🦑',120000,'Phần','FOOD',FALSE,ARRAY['CLAY_POT_RICE'],50,'Món ăn kèm'),
('Canh chua','🍲',80000,'Tô','FOOD',FALSE,ARRAY['CLAY_POT_RICE'],50,'Món ăn kèm'),
('Rau muống xào tỏi','🥬',50000,'Đĩa','FOOD',FALSE,ARRAY['CLAY_POT_RICE'],50,'Món ăn kèm'),
('Đậu hũ chiên sả','🧈',50000,'Đĩa','FOOD',FALSE,ARRAY['CLAY_POT_RICE'],50,'Món ăn kèm'),
('Trà đá','🧊',3000,'Ly','BEVERAGE',FALSE,ARRAY['CLAY_POT_RICE'],90,'Nước uống'),
('Nước ngọt','🥤',15000,'Lon','BEVERAGE',FALSE,ARRAY['CLAY_POT_RICE'],90,'Nước uống'),
('Nước suối','💧',5000,'Chai','BEVERAGE',FALSE,ARRAY['CLAY_POT_RICE'],90,'Nước uống'),
('Bia Saigon','🍺',15000,'Lon','BEVERAGE',FALSE,ARRAY['CLAY_POT_RICE'],90,'Nước uống')
ON CONFLICT (name) DO UPDATE SET shop_types = ARRAY(SELECT DISTINCT u FROM unnest(product_suggestions.shop_types || EXCLUDED.shop_types) AS u);

-- ── COM_TAM — Quán cơm tấm ────────────────────────────────────
INSERT INTO product_suggestions (name, emoji, default_price, unit, product_type_code, dynamic_price, shop_types, display_order, category_name) VALUES
('Cơm tấm sườn','🍚',40000,'Phần','FOOD',FALSE,ARRAY['COM_TAM'],10,'Cơm tấm'),
('Cơm tấm sườn bì chả','🍚',50000,'Phần','FOOD',FALSE,ARRAY['COM_TAM'],10,'Cơm tấm'),
('Cơm tấm bì chả','🍚',40000,'Phần','FOOD',FALSE,ARRAY['COM_TAM'],10,'Cơm tấm'),
('Cơm tấm sườn ốp la','🍳',50000,'Phần','FOOD',FALSE,ARRAY['COM_TAM'],10,'Cơm tấm'),
('Cơm tấm sườn trứng','🍚',45000,'Phần','FOOD',FALSE,ARRAY['COM_TAM'],10,'Cơm tấm'),
('Cơm tấm gà','🍚',40000,'Phần','FOOD',FALSE,ARRAY['COM_TAM'],10,'Cơm tấm'),
('Cơm tấm đặc biệt','🍚',60000,'Phần','FOOD',FALSE,ARRAY['COM_TAM'],10,'Cơm tấm'),
('Cơm thêm','🍚',5000,'Chén','FOOD',FALSE,ARRAY['COM_TAM'],50,'Gọi thêm'),
('Thêm sườn','🍖',25000,'Phần','FOOD',FALSE,ARRAY['COM_TAM'],50,'Gọi thêm'),
('Thêm bì','🥩',10000,'Phần','FOOD',FALSE,ARRAY['COM_TAM'],50,'Gọi thêm'),
('Thêm ốp la','🍳',10000,'Quả','FOOD',FALSE,ARRAY['COM_TAM'],50,'Gọi thêm'),
('Trà đá','🧊',3000,'Ly','BEVERAGE',FALSE,ARRAY['COM_TAM'],90,'Nước uống'),
('Nước ngọt','🥤',15000,'Lon','BEVERAGE',FALSE,ARRAY['COM_TAM'],90,'Nước uống'),
('Nước suối','💧',5000,'Chai','BEVERAGE',FALSE,ARRAY['COM_TAM'],90,'Nước uống')
ON CONFLICT (name) DO UPDATE SET shop_types = ARRAY(SELECT DISTINCT u FROM unnest(product_suggestions.shop_types || EXCLUDED.shop_types) AS u);

-- ── OC_QUAN — Quán ốc ─────────────────────────────────────────
INSERT INTO product_suggestions (name, emoji, default_price, unit, product_type_code, dynamic_price, shop_types, display_order, category_name) VALUES
('Ốc hương rang me','🐚',120000,'Phần','FOOD',FALSE,ARRAY['OC_QUAN'],10,'Món chính'),
('Ốc len xào dừa','🐚',80000,'Phần','FOOD',FALSE,ARRAY['OC_QUAN'],10,'Món chính'),
('Ốc bươu nướng tiêu','🐚',70000,'Phần','FOOD',FALSE,ARRAY['OC_QUAN'],10,'Món chính'),
('Nghêu hấp sả','🦪',80000,'Phần','FOOD',FALSE,ARRAY['OC_QUAN'],10,'Món chính'),
('Sò huyết rang me','🦪',120000,'Phần','FOOD',FALSE,ARRAY['OC_QUAN'],10,'Món chính'),
('Sò điệp nướng mỡ hành','🦪',90000,'Phần','FOOD',FALSE,ARRAY['OC_QUAN'],10,'Món chính'),
('Hàu nướng phô mai','🦪',100000,'Phần','FOOD',FALSE,ARRAY['OC_QUAN'],10,'Món chính'),
('Ốc móng tay xào bơ tỏi','🐚',100000,'Phần','FOOD',FALSE,ARRAY['OC_QUAN'],10,'Món chính'),
('Tôm hấp','🦐',150000,'Phần','FOOD',FALSE,ARRAY['OC_QUAN'],10,'Món chính'),
('Mực nướng','🦑',150000,'Phần','FOOD',FALSE,ARRAY['OC_QUAN'],10,'Món chính'),
('Càng ghẹ rang muối','🦀',120000,'Phần','FOOD',FALSE,ARRAY['OC_QUAN'],10,'Món chính'),
('Khoai tây chiên','🍟',40000,'Đĩa','FOOD',FALSE,ARRAY['OC_QUAN'],50,'Ăn kèm'),
('Bánh mì bơ','🥖',10000,'Ổ','FOOD',FALSE,ARRAY['OC_QUAN'],50,'Ăn kèm'),
('Rau muống xào','🥬',50000,'Đĩa','FOOD',FALSE,ARRAY['OC_QUAN'],50,'Ăn kèm'),
('Bia Saigon','🍺',15000,'Lon','BEVERAGE',FALSE,ARRAY['OC_QUAN'],90,'Nước uống'),
('Bia Tiger','🍺',16000,'Lon','BEVERAGE',FALSE,ARRAY['OC_QUAN'],90,'Nước uống'),
('Nước ngọt','🥤',15000,'Lon','BEVERAGE',FALSE,ARRAY['OC_QUAN'],90,'Nước uống'),
('Nước suối','💧',5000,'Chai','BEVERAGE',FALSE,ARRAY['OC_QUAN'],90,'Nước uống')
ON CONFLICT (name) DO UPDATE SET shop_types = ARRAY(SELECT DISTINCT u FROM unnest(product_suggestions.shop_types || EXCLUDED.shop_types) AS u);

-- ── CHAO_QUAN — Quán cháo ─────────────────────────────────────
INSERT INTO product_suggestions (name, emoji, default_price, unit, product_type_code, dynamic_price, shop_types, display_order, category_name) VALUES
('Cháo lòng','🥣',30000,'Tô','FOOD',FALSE,ARRAY['CHAO_QUAN'],10,'Cháo'),
('Cháo gà','🥣',35000,'Tô','FOOD',FALSE,ARRAY['CHAO_QUAN'],10,'Cháo'),
('Cháo vịt','🥣',40000,'Tô','FOOD',FALSE,ARRAY['CHAO_QUAN'],10,'Cháo'),
('Cháo sườn','🥣',30000,'Tô','FOOD',FALSE,ARRAY['CHAO_QUAN'],10,'Cháo'),
('Cháo cá','🥣',40000,'Tô','FOOD',FALSE,ARRAY['CHAO_QUAN'],10,'Cháo'),
('Cháo bò','🥣',35000,'Tô','FOOD',FALSE,ARRAY['CHAO_QUAN'],10,'Cháo'),
('Cháo ếch','🥣',50000,'Tô','FOOD',FALSE,ARRAY['CHAO_QUAN'],10,'Cháo'),
('Cháo tim cật','🥣',45000,'Tô','FOOD',FALSE,ARRAY['CHAO_QUAN'],10,'Cháo'),
('Cháo trắng hột vịt muối','🥣',20000,'Tô','FOOD',FALSE,ARRAY['CHAO_QUAN'],10,'Cháo'),
('Giò cháo quẩy','🥖',10000,'Phần','FOOD',FALSE,ARRAY['CHAO_QUAN'],50,'Gọi thêm'),
('Trứng bắc thảo','🥚',10000,'Quả','FOOD',FALSE,ARRAY['CHAO_QUAN'],50,'Gọi thêm'),
('Thêm lòng','🍖',20000,'Phần','FOOD',FALSE,ARRAY['CHAO_QUAN'],50,'Gọi thêm'),
('Trà đá','🧊',3000,'Ly','BEVERAGE',FALSE,ARRAY['CHAO_QUAN'],90,'Nước uống'),
('Nước ngọt','🥤',15000,'Lon','BEVERAGE',FALSE,ARRAY['CHAO_QUAN'],90,'Nước uống'),
('Nước suối','💧',5000,'Chai','BEVERAGE',FALSE,ARRAY['CHAO_QUAN'],90,'Nước uống')
ON CONFLICT (name) DO UPDATE SET shop_types = ARRAY(SELECT DISTINCT u FROM unnest(product_suggestions.shop_types || EXCLUDED.shop_types) AS u);

-- ── XOI_QUAN — Quán xôi ───────────────────────────────────────
INSERT INTO product_suggestions (name, emoji, default_price, unit, product_type_code, dynamic_price, shop_types, display_order, category_name) VALUES
('Xôi mặn','🍙',25000,'Phần','FOOD',FALSE,ARRAY['XOI_QUAN'],10,'Xôi'),
('Xôi gà','🍙',30000,'Phần','FOOD',FALSE,ARRAY['XOI_QUAN'],10,'Xôi'),
('Xôi xá xíu','🍙',30000,'Phần','FOOD',FALSE,ARRAY['XOI_QUAN'],10,'Xôi'),
('Xôi xéo','🍙',20000,'Phần','FOOD',FALSE,ARRAY['XOI_QUAN'],10,'Xôi'),
('Xôi gấc','🍙',15000,'Phần','FOOD',FALSE,ARRAY['XOI_QUAN'],10,'Xôi'),
('Xôi đậu xanh','🍙',15000,'Phần','FOOD',FALSE,ARRAY['XOI_QUAN'],10,'Xôi'),
('Xôi bắp','🌽',15000,'Phần','FOOD',FALSE,ARRAY['XOI_QUAN'],10,'Xôi'),
('Xôi sườn','🍙',35000,'Phần','FOOD',FALSE,ARRAY['XOI_QUAN'],10,'Xôi'),
('Thêm chả lụa','🍖',10000,'Phần','FOOD',FALSE,ARRAY['XOI_QUAN'],50,'Gọi thêm'),
('Thêm trứng','🥚',7000,'Quả','FOOD',FALSE,ARRAY['XOI_QUAN'],50,'Gọi thêm'),
('Thêm pate','🥫',5000,'Phần','FOOD',FALSE,ARRAY['XOI_QUAN'],50,'Gọi thêm'),
('Sữa đậu nành','🥛',10000,'Ly','BEVERAGE',FALSE,ARRAY['XOI_QUAN'],90,'Nước uống'),
('Cà phê sữa đá','☕',15000,'Ly','BEVERAGE',FALSE,ARRAY['XOI_QUAN'],90,'Nước uống'),
('Nước suối','💧',5000,'Chai','BEVERAGE',FALSE,ARRAY['XOI_QUAN'],90,'Nước uống')
ON CONFLICT (name) DO UPDATE SET shop_types = ARRAY(SELECT DISTINCT u FROM unnest(product_suggestions.shop_types || EXCLUDED.shop_types) AS u);

-- ── BANH_XEO — Quán bánh xèo ──────────────────────────────────
INSERT INTO product_suggestions (name, emoji, default_price, unit, product_type_code, dynamic_price, shop_types, display_order, category_name) VALUES
('Bánh xèo tôm thịt','🫓',50000,'Cái','FOOD',FALSE,ARRAY['BANH_XEO'],10,'Món chính'),
('Bánh xèo thập cẩm','🫓',60000,'Cái','FOOD',FALSE,ARRAY['BANH_XEO'],10,'Món chính'),
('Bánh xèo mực','🫓',60000,'Cái','FOOD',FALSE,ARRAY['BANH_XEO'],10,'Món chính'),
('Bánh xèo chay','🫓',40000,'Cái','FOOD',FALSE,ARRAY['BANH_XEO'],10,'Món chính'),
('Bánh khọt tôm','🥘',50000,'Phần','FOOD',FALSE,ARRAY['BANH_XEO'],10,'Món chính'),
('Bánh khọt thập cẩm','🥘',55000,'Phần','FOOD',FALSE,ARRAY['BANH_XEO'],10,'Món chính'),
('Nem lụi','🍢',50000,'Phần','FOOD',FALSE,ARRAY['BANH_XEO'],10,'Món chính'),
('Bò lá lốt','🍢',50000,'Phần','FOOD',FALSE,ARRAY['BANH_XEO'],10,'Món chính'),
('Gỏi cuốn','🥬',30000,'Phần','FOOD',FALSE,ARRAY['BANH_XEO'],10,'Món chính'),
('Rau sống ăn kèm','🥬',15000,'Đĩa','FOOD',FALSE,ARRAY['BANH_XEO'],50,'Gọi thêm'),
('Bánh tráng','🫓',10000,'Phần','FOOD',FALSE,ARRAY['BANH_XEO'],50,'Gọi thêm'),
('Trà đá','🧊',3000,'Ly','BEVERAGE',FALSE,ARRAY['BANH_XEO'],90,'Nước uống'),
('Nước ngọt','🥤',15000,'Lon','BEVERAGE',FALSE,ARRAY['BANH_XEO'],90,'Nước uống'),
('Nước suối','💧',5000,'Chai','BEVERAGE',FALSE,ARRAY['BANH_XEO'],90,'Nước uống')
ON CONFLICT (name) DO UPDATE SET shop_types = ARRAY(SELECT DISTINCT u FROM unnest(product_suggestions.shop_types || EXCLUDED.shop_types) AS u);

-- ── BUN_DAU — Quán bún đậu ────────────────────────────────────
INSERT INTO product_suggestions (name, emoji, default_price, unit, product_type_code, dynamic_price, shop_types, display_order, category_name) VALUES
('Bún đậu cơ bản','🍢',35000,'Phần','FOOD',FALSE,ARRAY['BUN_DAU'],10,'Món chính'),
('Bún đậu thập cẩm','🍢',70000,'Phần','FOOD',FALSE,ARRAY['BUN_DAU'],10,'Món chính'),
('Bún đậu chả cốm','🍢',50000,'Phần','FOOD',FALSE,ARRAY['BUN_DAU'],10,'Món chính'),
('Bún đậu lòng','🍢',60000,'Phần','FOOD',FALSE,ARRAY['BUN_DAU'],10,'Món chính'),
('Bún đậu nem rán','🍢',50000,'Phần','FOOD',FALSE,ARRAY['BUN_DAU'],10,'Món chính'),
('Đậu hũ chiên','🧈',20000,'Phần','FOOD',FALSE,ARRAY['BUN_DAU'],50,'Gọi thêm'),
('Chả cốm','🍘',25000,'Phần','FOOD',FALSE,ARRAY['BUN_DAU'],50,'Gọi thêm'),
('Lòng rán','🍖',40000,'Phần','FOOD',FALSE,ARRAY['BUN_DAU'],50,'Gọi thêm'),
('Nem chua rán','🌭',30000,'Phần','FOOD',FALSE,ARRAY['BUN_DAU'],50,'Gọi thêm'),
('Thêm mắm tôm','🥫',5000,'Phần','FOOD',FALSE,ARRAY['BUN_DAU'],50,'Gọi thêm'),
('Thêm đậu','🧈',15000,'Phần','FOOD',FALSE,ARRAY['BUN_DAU'],50,'Gọi thêm'),
('Thêm bún','🍜',10000,'Phần','FOOD',FALSE,ARRAY['BUN_DAU'],50,'Gọi thêm'),
('Trà đá','🧊',3000,'Ly','BEVERAGE',FALSE,ARRAY['BUN_DAU'],90,'Nước uống'),
('Nước ngọt','🥤',15000,'Lon','BEVERAGE',FALSE,ARRAY['BUN_DAU'],90,'Nước uống'),
('Nước suối','💧',5000,'Chai','BEVERAGE',FALSE,ARRAY['BUN_DAU'],90,'Nước uống')
ON CONFLICT (name) DO UPDATE SET shop_types = ARRAY(SELECT DISTINCT u FROM unnest(product_suggestions.shop_types || EXCLUDED.shop_types) AS u);

-- ── BANH_CANH — Quán bánh canh ────────────────────────────────
INSERT INTO product_suggestions (name, emoji, default_price, unit, product_type_code, dynamic_price, shop_types, display_order, category_name) VALUES
('Bánh canh cua','🦀',50000,'Tô','FOOD',FALSE,ARRAY['BANH_CANH'],10,'Bánh canh'),
('Bánh canh giò heo','🍲',45000,'Tô','FOOD',FALSE,ARRAY['BANH_CANH'],10,'Bánh canh'),
('Bánh canh cá lóc','🐟',45000,'Tô','FOOD',FALSE,ARRAY['BANH_CANH'],10,'Bánh canh'),
('Bánh canh ghẹ','🦀',60000,'Tô','FOOD',FALSE,ARRAY['BANH_CANH'],10,'Bánh canh'),
('Bánh canh tôm','🦐',50000,'Tô','FOOD',FALSE,ARRAY['BANH_CANH'],10,'Bánh canh'),
('Bánh canh chả cá','🐟',40000,'Tô','FOOD',FALSE,ARRAY['BANH_CANH'],10,'Bánh canh'),
('Bánh canh bột lọc','🍲',40000,'Tô','FOOD',FALSE,ARRAY['BANH_CANH'],10,'Bánh canh'),
('Thêm giò','🍖',15000,'Phần','FOOD',FALSE,ARRAY['BANH_CANH'],50,'Gọi thêm'),
('Thêm cua','🦀',25000,'Phần','FOOD',FALSE,ARRAY['BANH_CANH'],50,'Gọi thêm'),
('Thêm chả','🍖',10000,'Phần','FOOD',FALSE,ARRAY['BANH_CANH'],50,'Gọi thêm'),
('Thêm trứng cút','🥚',10000,'Phần','FOOD',FALSE,ARRAY['BANH_CANH'],50,'Gọi thêm'),
('Trà đá','🧊',3000,'Ly','BEVERAGE',FALSE,ARRAY['BANH_CANH'],90,'Nước uống'),
('Nước ngọt','🥤',15000,'Lon','BEVERAGE',FALSE,ARRAY['BANH_CANH'],90,'Nước uống'),
('Nước suối','💧',5000,'Chai','BEVERAGE',FALSE,ARRAY['BANH_CANH'],90,'Nước uống')
ON CONFLICT (name) DO UPDATE SET shop_types = ARRAY(SELECT DISTINCT u FROM unnest(product_suggestions.shop_types || EXCLUDED.shop_types) AS u);

-- ── AN_VAT — Quán ăn vặt ──────────────────────────────────────
INSERT INTO product_suggestions (name, emoji, default_price, unit, product_type_code, dynamic_price, shop_types, display_order, category_name) VALUES
('Bánh tráng trộn','🥗',25000,'Phần','FOOD',FALSE,ARRAY['AN_VAT'],10,'Ăn vặt'),
('Bánh tráng nướng','🫓',25000,'Cái','FOOD',FALSE,ARRAY['AN_VAT'],10,'Ăn vặt'),
('Phá lấu','🍲',35000,'Phần','FOOD',FALSE,ARRAY['AN_VAT'],10,'Ăn vặt'),
('Chân gà sả tắc','🍗',50000,'Phần','FOOD',FALSE,ARRAY['AN_VAT'],10,'Ăn vặt'),
('Chân gà nướng','🍗',45000,'Phần','FOOD',FALSE,ARRAY['AN_VAT'],10,'Ăn vặt'),
('Cá viên chiên','🍢',25000,'Phần','FOOD',FALSE,ARRAY['AN_VAT'],10,'Ăn vặt'),
('Xúc xích nướng','🌭',20000,'Cây','FOOD',FALSE,ARRAY['AN_VAT'],10,'Ăn vặt'),
('Khoai tây lắc','🍟',30000,'Phần','FOOD',FALSE,ARRAY['AN_VAT'],10,'Ăn vặt'),
('Gỏi khô bò','🥗',35000,'Phần','FOOD',FALSE,ARRAY['AN_VAT'],10,'Ăn vặt'),
('Bắp xào','🌽',20000,'Phần','FOOD',FALSE,ARRAY['AN_VAT'],10,'Ăn vặt'),
('Cút lộn xào me','🥚',35000,'Phần','FOOD',FALSE,ARRAY['AN_VAT'],10,'Ăn vặt'),
('Trà tắc','🍵',12000,'Ly','BEVERAGE',FALSE,ARRAY['AN_VAT'],90,'Nước uống'),
('Nước ngọt','🥤',15000,'Lon','BEVERAGE',FALSE,ARRAY['AN_VAT'],90,'Nước uống'),
('Nước suối','💧',5000,'Chai','BEVERAGE',FALSE,ARRAY['AN_VAT'],90,'Nước uống')
ON CONFLICT (name) DO UPDATE SET shop_types = ARRAY(SELECT DISTINCT u FROM unnest(product_suggestions.shop_types || EXCLUDED.shop_types) AS u);

-- ── GRILL_BBQ — Nhà hàng nướng ────────────────────────────────
INSERT INTO product_suggestions (name, emoji, default_price, unit, product_type_code, dynamic_price, shop_types, display_order, category_name) VALUES
('Buffet nướng 1 người','🔥',199000,'Suất','FOOD',FALSE,ARRAY['GRILL_BBQ'],10,'Buffet & Combo'),
('Buffet nướng cao cấp','🔥',299000,'Suất','FOOD',FALSE,ARRAY['GRILL_BBQ'],10,'Buffet & Combo'),
('Combo nướng 2 người','🔥',350000,'Phần','FOOD',FALSE,ARRAY['GRILL_BBQ'],10,'Buffet & Combo'),
('Combo nướng 4 người','🔥',650000,'Phần','FOOD',FALSE,ARRAY['GRILL_BBQ'],10,'Buffet & Combo'),
('Ba chỉ bò Mỹ','🥩',150000,'Phần','FOOD',FALSE,ARRAY['GRILL_BBQ'],20,'Món nướng'),
('Ba chỉ heo nướng','🥓',90000,'Phần','FOOD',FALSE,ARRAY['GRILL_BBQ'],20,'Món nướng'),
('Sườn bò nướng','🍖',180000,'Phần','FOOD',FALSE,ARRAY['GRILL_BBQ'],20,'Món nướng'),
('Sườn heo nướng','🍖',130000,'Phần','FOOD',FALSE,ARRAY['GRILL_BBQ'],20,'Món nướng'),
('Bạch tuộc nướng sa tế','🐙',150000,'Phần','FOOD',FALSE,ARRAY['GRILL_BBQ'],20,'Món nướng'),
('Tôm nướng','🦐',180000,'Phần','FOOD',FALSE,ARRAY['GRILL_BBQ'],20,'Món nướng'),
('Lưỡi bò nướng','🥩',160000,'Phần','FOOD',FALSE,ARRAY['GRILL_BBQ'],20,'Món nướng'),
('Cánh gà nướng','🍗',90000,'Phần','FOOD',FALSE,ARRAY['GRILL_BBQ'],20,'Món nướng'),
('Nấm đùi gà nướng','🍄',60000,'Phần','FOOD',FALSE,ARRAY['GRILL_BBQ'],20,'Món nướng'),
('Kim chi','🥬',30000,'Phần','FOOD',FALSE,ARRAY['GRILL_BBQ'],50,'Ăn kèm'),
('Rau nhúng','🥬',40000,'Phần','FOOD',FALSE,ARRAY['GRILL_BBQ'],50,'Ăn kèm'),
('Cơm chiên','🍚',50000,'Phần','FOOD',FALSE,ARRAY['GRILL_BBQ'],50,'Ăn kèm'),
('Bia Tiger','🍺',16000,'Lon','BEVERAGE',FALSE,ARRAY['GRILL_BBQ'],90,'Nước uống'),
('Bia Saigon','🍺',15000,'Lon','BEVERAGE',FALSE,ARRAY['GRILL_BBQ'],90,'Nước uống'),
('Soju','🍶',80000,'Chai','BEVERAGE',FALSE,ARRAY['GRILL_BBQ'],90,'Nước uống'),
('Nước ngọt','🥤',15000,'Lon','BEVERAGE',FALSE,ARRAY['GRILL_BBQ'],90,'Nước uống'),
('Nước suối','💧',5000,'Chai','BEVERAGE',FALSE,ARRAY['GRILL_BBQ'],90,'Nước uống')
ON CONFLICT (name) DO UPDATE SET shop_types = ARRAY(SELECT DISTINCT u FROM unnest(product_suggestions.shop_types || EXCLUDED.shop_types) AS u);

-- ── HOTPOT — Nhà hàng lẩu ─────────────────────────────────────
INSERT INTO product_suggestions (name, emoji, default_price, unit, product_type_code, dynamic_price, shop_types, display_order, category_name) VALUES
('Lẩu Thái hải sản','🍲',350000,'Nồi','FOOD',FALSE,ARRAY['HOTPOT'],10,'Lẩu'),
('Lẩu bò nhúng giấm','🍲',320000,'Nồi','FOOD',FALSE,ARRAY['HOTPOT'],10,'Lẩu'),
('Lẩu gà lá é','🍲',280000,'Nồi','FOOD',FALSE,ARRAY['HOTPOT'],10,'Lẩu'),
('Lẩu cá kèo','🍲',280000,'Nồi','FOOD',FALSE,ARRAY['HOTPOT'],10,'Lẩu'),
('Lẩu riêu cua bắp bò','🍲',320000,'Nồi','FOOD',FALSE,ARRAY['HOTPOT'],10,'Lẩu'),
('Lẩu kim chi','🍲',300000,'Nồi','FOOD',FALSE,ARRAY['HOTPOT'],10,'Lẩu'),
('Lẩu nấm chay','🍲',220000,'Nồi','FOOD',FALSE,ARRAY['HOTPOT'],10,'Lẩu'),
('Buffet lẩu 1 người','🍲',199000,'Suất','FOOD',FALSE,ARRAY['HOTPOT'],10,'Buffet'),
('Buffet lẩu cao cấp','🍲',299000,'Suất','FOOD',FALSE,ARRAY['HOTPOT'],10,'Buffet'),
('Bò Mỹ nhúng lẩu','🥩',120000,'Phần','FOOD',FALSE,ARRAY['HOTPOT'],50,'Đồ nhúng'),
('Hải sản tổng hợp','🦐',150000,'Phần','FOOD',FALSE,ARRAY['HOTPOT'],50,'Đồ nhúng'),
('Cá viên bò viên','🍢',60000,'Phần','FOOD',FALSE,ARRAY['HOTPOT'],50,'Đồ nhúng'),
('Rau nhúng lẩu','🥬',40000,'Phần','FOOD',FALSE,ARRAY['HOTPOT'],50,'Đồ nhúng'),
('Nấm các loại','🍄',50000,'Phần','FOOD',FALSE,ARRAY['HOTPOT'],50,'Đồ nhúng'),
('Mì bún miến','🍜',15000,'Vắt','FOOD',FALSE,ARRAY['HOTPOT'],50,'Đồ nhúng'),
('Đậu hũ','🧈',20000,'Phần','FOOD',FALSE,ARRAY['HOTPOT'],50,'Đồ nhúng'),
('Bia Tiger','🍺',16000,'Lon','BEVERAGE',FALSE,ARRAY['HOTPOT'],90,'Nước uống'),
('Bia Saigon','🍺',15000,'Lon','BEVERAGE',FALSE,ARRAY['HOTPOT'],90,'Nước uống'),
('Nước ngọt','🥤',15000,'Lon','BEVERAGE',FALSE,ARRAY['HOTPOT'],90,'Nước uống'),
('Nước suối','💧',5000,'Chai','BEVERAGE',FALSE,ARRAY['HOTPOT'],90,'Nước uống')
ON CONFLICT (name) DO UPDATE SET shop_types = ARRAY(SELECT DISTINCT u FROM unnest(product_suggestions.shop_types || EXCLUDED.shop_types) AS u);

-- ── FRIED_CHICKEN — Gà rán / Fast food ────────────────────────
INSERT INTO product_suggestions (name, emoji, default_price, unit, product_type_code, dynamic_price, shop_types, display_order, category_name) VALUES
('Gà rán 1 miếng','🍗',30000,'Miếng','FOOD',FALSE,ARRAY['FRIED_CHICKEN'],10,'Món chính'),
('Combo 2 miếng + nước','🍗',65000,'Combo','FOOD',FALSE,ARRAY['FRIED_CHICKEN'],10,'Món chính'),
('Combo 3 miếng + khoai + nước','🍗',95000,'Combo','FOOD',FALSE,ARRAY['FRIED_CHICKEN'],10,'Món chính'),
('Gà sốt cay','🍗',35000,'Miếng','FOOD',FALSE,ARRAY['FRIED_CHICKEN'],10,'Món chính'),
('Gà popcorn','🍿',35000,'Phần','FOOD',FALSE,ARRAY['FRIED_CHICKEN'],10,'Món chính'),
('Burger gà','🍔',40000,'Cái','FOOD',FALSE,ARRAY['FRIED_CHICKEN'],10,'Món chính'),
('Burger bò','🍔',45000,'Cái','FOOD',FALSE,ARRAY['FRIED_CHICKEN'],10,'Món chính'),
('Hamburger phô mai','🍔',50000,'Cái','FOOD',FALSE,ARRAY['FRIED_CHICKEN'],10,'Món chính'),
('Khoai tây chiên','🍟',25000,'Phần','FOOD',FALSE,ARRAY['FRIED_CHICKEN'],50,'Ăn kèm'),
('Khoai tây phô mai','🍟',35000,'Phần','FOOD',FALSE,ARRAY['FRIED_CHICKEN'],50,'Ăn kèm'),
('Hot dog','🌭',30000,'Cái','FOOD',FALSE,ARRAY['FRIED_CHICKEN'],50,'Ăn kèm'),
('Cơm gà rán','🍚',45000,'Phần','FOOD',FALSE,ARRAY['FRIED_CHICKEN'],50,'Ăn kèm'),
('Pepsi','🥤',15000,'Lon','BEVERAGE',FALSE,ARRAY['FRIED_CHICKEN'],90,'Nước uống'),
('Nước cam','🍊',25000,'Ly','BEVERAGE',FALSE,ARRAY['FRIED_CHICKEN'],90,'Nước uống'),
('Nước suối','💧',5000,'Chai','BEVERAGE',FALSE,ARRAY['FRIED_CHICKEN'],90,'Nước uống')
ON CONFLICT (name) DO UPDATE SET shop_types = ARRAY(SELECT DISTINCT u FROM unnest(product_suggestions.shop_types || EXCLUDED.shop_types) AS u);

-- ── VEGETARIAN — Quán chay ────────────────────────────────────
INSERT INTO product_suggestions (name, emoji, default_price, unit, product_type_code, dynamic_price, shop_types, display_order, category_name) VALUES
('Cơm chay phần','🥗',35000,'Phần','FOOD',FALSE,ARRAY['VEGETARIAN'],10,'Món chay'),
('Cơm chay thập cẩm','🥗',45000,'Phần','FOOD',FALSE,ARRAY['VEGETARIAN'],10,'Món chay'),
('Bún chay','🍜',35000,'Tô','FOOD',FALSE,ARRAY['VEGETARIAN'],10,'Món chay'),
('Phở chay','🍜',35000,'Tô','FOOD',FALSE,ARRAY['VEGETARIAN'],10,'Món chay'),
('Mì xào chay','🍝',40000,'Phần','FOOD',FALSE,ARRAY['VEGETARIAN'],10,'Món chay'),
('Đậu hũ kho','🧈',40000,'Phần','FOOD',FALSE,ARRAY['VEGETARIAN'],10,'Món chay'),
('Nấm kho tiêu','🍄',50000,'Phần','FOOD',FALSE,ARRAY['VEGETARIAN'],10,'Món chay'),
('Rau xào thập cẩm','🥬',45000,'Đĩa','FOOD',FALSE,ARRAY['VEGETARIAN'],10,'Món chay'),
('Canh chua chay','🍲',40000,'Tô','FOOD',FALSE,ARRAY['VEGETARIAN'],10,'Món chay'),
('Chả giò chay','🥢',35000,'Phần','FOOD',FALSE,ARRAY['VEGETARIAN'],10,'Món chay'),
('Lẩu nấm chay','🍲',200000,'Nồi','FOOD',FALSE,ARRAY['VEGETARIAN'],10,'Món chay'),
('Gỏi cuốn chay','🥬',25000,'Phần','FOOD',FALSE,ARRAY['VEGETARIAN'],10,'Món chay'),
('Nước rau má','🥤',15000,'Ly','BEVERAGE',FALSE,ARRAY['VEGETARIAN'],90,'Nước uống'),
('Sữa đậu nành','🥛',10000,'Ly','BEVERAGE',FALSE,ARRAY['VEGETARIAN'],90,'Nước uống'),
('Trà đá','🧊',3000,'Ly','BEVERAGE',FALSE,ARRAY['VEGETARIAN'],90,'Nước uống'),
('Nước suối','💧',5000,'Chai','BEVERAGE',FALSE,ARRAY['VEGETARIAN'],90,'Nước uống')
ON CONFLICT (name) DO UPDATE SET shop_types = ARRAY(SELECT DISTINCT u FROM unnest(product_suggestions.shop_types || EXCLUDED.shop_types) AS u);

-- ── PIZZA_PASTA — Pizza / Mì Ý ────────────────────────────────
INSERT INTO product_suggestions (name, emoji, default_price, unit, product_type_code, dynamic_price, shop_types, display_order, category_name) VALUES
('Pizza hải sản','🍕',150000,'Cái','FOOD',FALSE,ARRAY['PIZZA_PASTA'],10,'Pizza'),
('Pizza bò','🍕',140000,'Cái','FOOD',FALSE,ARRAY['PIZZA_PASTA'],10,'Pizza'),
('Pizza phô mai','🍕',120000,'Cái','FOOD',FALSE,ARRAY['PIZZA_PASTA'],10,'Pizza'),
('Pizza thập cẩm','🍕',160000,'Cái','FOOD',FALSE,ARRAY['PIZZA_PASTA'],10,'Pizza'),
('Pizza gà BBQ','🍕',140000,'Cái','FOOD',FALSE,ARRAY['PIZZA_PASTA'],10,'Pizza'),
('Mì Ý sốt bò bằm','🍝',75000,'Phần','FOOD',FALSE,ARRAY['PIZZA_PASTA'],20,'Mì Ý'),
('Mì Ý hải sản','🍝',90000,'Phần','FOOD',FALSE,ARRAY['PIZZA_PASTA'],20,'Mì Ý'),
('Mì Ý sốt kem','🍝',80000,'Phần','FOOD',FALSE,ARRAY['PIZZA_PASTA'],20,'Mì Ý'),
('Mì Ý sốt cà chua','🍝',65000,'Phần','FOOD',FALSE,ARRAY['PIZZA_PASTA'],20,'Mì Ý'),
('Khoai tây chiên','🍟',40000,'Phần','FOOD',FALSE,ARRAY['PIZZA_PASTA'],50,'Khai vị'),
('Súp bí đỏ','🍲',45000,'Phần','FOOD',FALSE,ARRAY['PIZZA_PASTA'],50,'Khai vị'),
('Salad','🥗',50000,'Phần','FOOD',FALSE,ARRAY['PIZZA_PASTA'],50,'Khai vị'),
('Bánh mì bơ tỏi','🥖',35000,'Phần','FOOD',FALSE,ARRAY['PIZZA_PASTA'],50,'Khai vị'),
('Coca Cola','🥤',12000,'Lon','BEVERAGE',FALSE,ARRAY['PIZZA_PASTA'],90,'Nước uống'),
('Nước cam','🍊',30000,'Ly','BEVERAGE',FALSE,ARRAY['PIZZA_PASTA'],90,'Nước uống'),
('Nước suối','💧',5000,'Chai','BEVERAGE',FALSE,ARRAY['PIZZA_PASTA'],90,'Nước uống')
ON CONFLICT (name) DO UPDATE SET shop_types = ARRAY(SELECT DISTINCT u FROM unnest(product_suggestions.shop_types || EXCLUDED.shop_types) AS u);

-- ── KOREAN — Quán Hàn Quốc ────────────────────────────────────
INSERT INTO product_suggestions (name, emoji, default_price, unit, product_type_code, dynamic_price, shop_types, display_order, category_name) VALUES
('Mì cay hải sản','🍜',60000,'Tô','FOOD',FALSE,ARRAY['KOREAN'],10,'Món chính'),
('Mì cay bò','🍜',60000,'Tô','FOOD',FALSE,ARRAY['KOREAN'],10,'Món chính'),
('Cơm trộn Hàn Quốc','🍚',65000,'Phần','FOOD',FALSE,ARRAY['KOREAN'],10,'Món chính'),
('Cơm trộn bò','🍚',70000,'Phần','FOOD',FALSE,ARRAY['KOREAN'],10,'Món chính'),
('Tokbokki','🍢',50000,'Phần','FOOD',FALSE,ARRAY['KOREAN'],10,'Món chính'),
('Gà sốt cay Hàn Quốc','🍗',90000,'Phần','FOOD',FALSE,ARRAY['KOREAN'],10,'Món chính'),
('Gà sốt phô mai','🍗',100000,'Phần','FOOD',FALSE,ARRAY['KOREAN'],10,'Món chính'),
('Kimbap','🍙',45000,'Phần','FOOD',FALSE,ARRAY['KOREAN'],10,'Món chính'),
('Canh kimchi','🍲',50000,'Tô','FOOD',FALSE,ARRAY['KOREAN'],10,'Món chính'),
('Lẩu kimchi','🍲',250000,'Nồi','FOOD',FALSE,ARRAY['KOREAN'],10,'Món chính'),
('Cơm cuộn phô mai','🍙',50000,'Phần','FOOD',FALSE,ARRAY['KOREAN'],10,'Món chính'),
('Trà bắp','🍵',20000,'Ly','BEVERAGE',FALSE,ARRAY['KOREAN'],90,'Nước uống'),
('Soju','🍶',80000,'Chai','BEVERAGE',FALSE,ARRAY['KOREAN'],90,'Nước uống'),
('Coca Cola','🥤',12000,'Lon','BEVERAGE',FALSE,ARRAY['KOREAN'],90,'Nước uống'),
('Nước suối','💧',5000,'Chai','BEVERAGE',FALSE,ARRAY['KOREAN'],90,'Nước uống')
ON CONFLICT (name) DO UPDATE SET shop_types = ARRAY(SELECT DISTINCT u FROM unnest(product_suggestions.shop_types || EXCLUDED.shop_types) AS u);

-- ── JAPANESE — Quán Nhật / Sushi ──────────────────────────────
INSERT INTO product_suggestions (name, emoji, default_price, unit, product_type_code, dynamic_price, shop_types, display_order, category_name) VALUES
('Sushi cá hồi','🍣',40000,'Phần','FOOD',FALSE,ARRAY['JAPANESE'],10,'Món chính'),
('Sushi tổng hợp','🍣',150000,'Phần','FOOD',FALSE,ARRAY['JAPANESE'],10,'Món chính'),
('Sashimi cá hồi','🍣',120000,'Phần','FOOD',FALSE,ARRAY['JAPANESE'],10,'Món chính'),
('Maki cuộn','🍙',60000,'Phần','FOOD',FALSE,ARRAY['JAPANESE'],10,'Món chính'),
('Cơm cá hồi (Donburi)','🍱',90000,'Phần','FOOD',FALSE,ARRAY['JAPANESE'],10,'Món chính'),
('Cơm lươn (Unadon)','🍱',120000,'Phần','FOOD',FALSE,ARRAY['JAPANESE'],10,'Món chính'),
('Mì Ramen','🍜',80000,'Tô','FOOD',FALSE,ARRAY['JAPANESE'],10,'Món chính'),
('Mì Udon','🍜',70000,'Tô','FOOD',FALSE,ARRAY['JAPANESE'],10,'Món chính'),
('Tempura tôm','🍤',90000,'Phần','FOOD',FALSE,ARRAY['JAPANESE'],10,'Món chính'),
('Cơm bò (Gyudon)','🍱',75000,'Phần','FOOD',FALSE,ARRAY['JAPANESE'],10,'Món chính'),
('Takoyaki','🐙',45000,'Phần','FOOD',FALSE,ARRAY['JAPANESE'],10,'Món chính'),
('Salad rong biển','🥗',40000,'Phần','FOOD',FALSE,ARRAY['JAPANESE'],10,'Món chính'),
('Trà xanh Nhật','🍵',20000,'Ly','BEVERAGE',FALSE,ARRAY['JAPANESE'],90,'Nước uống'),
('Ramune','🥤',30000,'Chai','BEVERAGE',FALSE,ARRAY['JAPANESE'],90,'Nước uống'),
('Bia Asahi','🍺',40000,'Lon','BEVERAGE',FALSE,ARRAY['JAPANESE'],90,'Nước uống'),
('Nước suối','💧',5000,'Chai','BEVERAGE',FALSE,ARRAY['JAPANESE'],90,'Nước uống')
ON CONFLICT (name) DO UPDATE SET shop_types = ARRAY(SELECT DISTINCT u FROM unnest(product_suggestions.shop_types || EXCLUDED.shop_types) AS u);

-- ════════════════ DRINK / DESSERT VERTICALS ════════════════

-- ── MILK_TEA — Quán trà sữa ───────────────────────────────────
INSERT INTO product_suggestions (name, emoji, default_price, unit, product_type_code, dynamic_price, shop_types, display_order, category_name) VALUES
('Trà sữa truyền thống','🧋',30000,'Ly','BEVERAGE',FALSE,ARRAY['MILK_TEA'],10,'Đồ uống'),
('Trà sữa trân châu đường đen','🧋',40000,'Ly','BEVERAGE',FALSE,ARRAY['MILK_TEA'],10,'Đồ uống'),
('Trà sữa matcha','🧋',40000,'Ly','BEVERAGE',FALSE,ARRAY['MILK_TEA'],10,'Đồ uống'),
('Trà sữa socola','🧋',38000,'Ly','BEVERAGE',FALSE,ARRAY['MILK_TEA'],10,'Đồ uống'),
('Trà sữa khoai môn','🧋',38000,'Ly','BEVERAGE',FALSE,ARRAY['MILK_TEA'],10,'Đồ uống'),
('Hồng trà sữa','🧋',35000,'Ly','BEVERAGE',FALSE,ARRAY['MILK_TEA'],10,'Đồ uống'),
('Trà sữa Thái xanh','🧋',38000,'Ly','BEVERAGE',FALSE,ARRAY['MILK_TEA'],10,'Đồ uống'),
('Sữa tươi trân châu đường đen','🥛',40000,'Ly','BEVERAGE',FALSE,ARRAY['MILK_TEA'],10,'Đồ uống'),
('Trà đào','🍑',35000,'Ly','BEVERAGE',FALSE,ARRAY['MILK_TEA'],20,'Trà trái cây'),
('Trà vải','🍒',35000,'Ly','BEVERAGE',FALSE,ARRAY['MILK_TEA'],20,'Trà trái cây'),
('Trà chanh giã tay','🍋',30000,'Ly','BEVERAGE',FALSE,ARRAY['MILK_TEA'],20,'Trà trái cây')
ON CONFLICT (name) DO UPDATE SET shop_types = ARRAY(SELECT DISTINCT u FROM unnest(product_suggestions.shop_types || EXCLUDED.shop_types) AS u);

-- ── DESSERT_CHE — Quán chè ────────────────────────────────────
INSERT INTO product_suggestions (name, emoji, default_price, unit, product_type_code, dynamic_price, shop_types, display_order, category_name) VALUES
('Chè đậu đỏ','🍧',20000,'Ly','FOOD',FALSE,ARRAY['DESSERT_CHE'],10,'Chè'),
('Chè đậu xanh','🍧',20000,'Ly','FOOD',FALSE,ARRAY['DESSERT_CHE'],10,'Chè'),
('Chè ba màu','🍧',25000,'Ly','FOOD',FALSE,ARRAY['DESSERT_CHE'],10,'Chè'),
('Chè Thái','🍧',30000,'Ly','FOOD',FALSE,ARRAY['DESSERT_CHE'],10,'Chè'),
('Chè khúc bạch','🍧',30000,'Ly','FOOD',FALSE,ARRAY['DESSERT_CHE'],10,'Chè'),
('Chè bưởi','🍧',25000,'Ly','FOOD',FALSE,ARRAY['DESSERT_CHE'],10,'Chè'),
('Chè trôi nước','🍡',20000,'Ly','FOOD',FALSE,ARRAY['DESSERT_CHE'],10,'Chè'),
('Chè chuối','🍌',25000,'Ly','FOOD',FALSE,ARRAY['DESSERT_CHE'],10,'Chè'),
('Chè sương sa hạt lựu','🍧',25000,'Ly','FOOD',FALSE,ARRAY['DESSERT_CHE'],10,'Chè'),
('Chè khoai môn','🍠',25000,'Ly','FOOD',FALSE,ARRAY['DESSERT_CHE'],10,'Chè'),
('Tào phớ','🍮',20000,'Ly','FOOD',FALSE,ARRAY['DESSERT_CHE'],10,'Chè'),
('Sữa chua nếp cẩm','🍚',25000,'Ly','FOOD',FALSE,ARRAY['DESSERT_CHE'],10,'Chè')
ON CONFLICT (name) DO UPDATE SET shop_types = ARRAY(SELECT DISTINCT u FROM unnest(product_suggestions.shop_types || EXCLUDED.shop_types) AS u);

-- ── JUICE — Quán nước ép / sinh tố ────────────────────────────
INSERT INTO product_suggestions (name, emoji, default_price, unit, product_type_code, dynamic_price, shop_types, display_order, category_name) VALUES
('Nước ép cam','🍊',30000,'Ly','BEVERAGE',FALSE,ARRAY['JUICE'],10,'Nước ép'),
('Nước ép cà rốt','🥕',30000,'Ly','BEVERAGE',FALSE,ARRAY['JUICE'],10,'Nước ép'),
('Nước ép dứa','🍍',30000,'Ly','BEVERAGE',FALSE,ARRAY['JUICE'],10,'Nước ép'),
('Nước ép ổi','🫐',30000,'Ly','BEVERAGE',FALSE,ARRAY['JUICE'],10,'Nước ép'),
('Nước ép táo','🍎',35000,'Ly','BEVERAGE',FALSE,ARRAY['JUICE'],10,'Nước ép'),
('Nước ép dưa hấu','🍉',25000,'Ly','BEVERAGE',FALSE,ARRAY['JUICE'],10,'Nước ép'),
('Nước ép cần tây','🥬',35000,'Ly','BEVERAGE',FALSE,ARRAY['JUICE'],10,'Nước ép'),
('Nước ép lựu','🫐',40000,'Ly','BEVERAGE',FALSE,ARRAY['JUICE'],10,'Nước ép'),
('Sinh tố bơ','🥑',40000,'Ly','BEVERAGE',FALSE,ARRAY['JUICE'],20,'Sinh tố'),
('Sinh tố xoài','🥭',35000,'Ly','BEVERAGE',FALSE,ARRAY['JUICE'],20,'Sinh tố'),
('Sinh tố dâu','🍓',35000,'Ly','BEVERAGE',FALSE,ARRAY['JUICE'],20,'Sinh tố'),
('Sinh tố mãng cầu','🍈',40000,'Ly','BEVERAGE',FALSE,ARRAY['JUICE'],20,'Sinh tố'),
('Sinh tố việt quất','🫐',45000,'Ly','BEVERAGE',FALSE,ARRAY['JUICE'],20,'Sinh tố'),
('Sinh tố sapoche','🥝',35000,'Ly','BEVERAGE',FALSE,ARRAY['JUICE'],20,'Sinh tố')
ON CONFLICT (name) DO UPDATE SET shop_types = ARRAY(SELECT DISTINCT u FROM unnest(product_suggestions.shop_types || EXCLUDED.shop_types) AS u);

-- ── ICE_CREAM — Quán kem ──────────────────────────────────────
INSERT INTO product_suggestions (name, emoji, default_price, unit, product_type_code, dynamic_price, shop_types, display_order, category_name) VALUES
('Kem ly 1 viên','🍨',15000,'Ly','FOOD',FALSE,ARRAY['ICE_CREAM'],10,'Kem'),
('Kem ly 2 viên','🍨',25000,'Ly','FOOD',FALSE,ARRAY['ICE_CREAM'],10,'Kem'),
('Kem ly 3 viên','🍨',35000,'Ly','FOOD',FALSE,ARRAY['ICE_CREAM'],10,'Kem'),
('Kem ốc quế','🍦',20000,'Cái','FOOD',FALSE,ARRAY['ICE_CREAM'],10,'Kem'),
('Kem ký','🍨',30000,'Phần','FOOD',FALSE,ARRAY['ICE_CREAM'],10,'Kem'),
('Kem xôi','🍨',30000,'Phần','FOOD',FALSE,ARRAY['ICE_CREAM'],10,'Kem'),
('Kem trái dừa','🥥',40000,'Trái','FOOD',FALSE,ARRAY['ICE_CREAM'],10,'Kem'),
('Kem flan','🍮',20000,'Ly','FOOD',FALSE,ARRAY['ICE_CREAM'],10,'Kem'),
('Kem chuối','🍌',15000,'Cây','FOOD',FALSE,ARRAY['ICE_CREAM'],10,'Kem'),
('Sundae','🍨',40000,'Ly','FOOD',FALSE,ARRAY['ICE_CREAM'],10,'Kem'),
('Sinh tố kem','🥤',40000,'Ly','BEVERAGE',FALSE,ARRAY['ICE_CREAM'],20,'Đồ uống'),
('Nước suối','💧',5000,'Chai','BEVERAGE',FALSE,ARRAY['ICE_CREAM'],90,'Đồ uống')
ON CONFLICT (name) DO UPDATE SET shop_types = ARRAY(SELECT DISTINCT u FROM unnest(product_suggestions.shop_types || EXCLUDED.shop_types) AS u);

-- ── YOGURT — Quán sữa chua ────────────────────────────────────
INSERT INTO product_suggestions (name, emoji, default_price, unit, product_type_code, dynamic_price, shop_types, display_order, category_name) VALUES
('Sữa chua trân châu đường đen','🥛',30000,'Ly','BEVERAGE',FALSE,ARRAY['YOGURT'],10,'Sữa chua'),
('Sữa chua đá','🥛',15000,'Ly','BEVERAGE',FALSE,ARRAY['YOGURT'],10,'Sữa chua'),
('Sữa chua nếp cẩm','🍚',25000,'Ly','BEVERAGE',FALSE,ARRAY['YOGURT'],10,'Sữa chua'),
('Sữa chua mít','🥭',25000,'Ly','BEVERAGE',FALSE,ARRAY['YOGURT'],10,'Sữa chua'),
('Sữa chua trái cây','🍓',30000,'Ly','BEVERAGE',FALSE,ARRAY['YOGURT'],10,'Sữa chua'),
('Sữa chua dẻo','🥛',20000,'Ly','BEVERAGE',FALSE,ARRAY['YOGURT'],10,'Sữa chua'),
('Sữa chua việt quất','🫐',35000,'Ly','BEVERAGE',FALSE,ARRAY['YOGURT'],10,'Sữa chua'),
('Sữa chua đánh đá','🥛',20000,'Ly','BEVERAGE',FALSE,ARRAY['YOGURT'],10,'Sữa chua'),
('Yaourt phô mai','🧀',35000,'Ly','BEVERAGE',FALSE,ARRAY['YOGURT'],10,'Sữa chua')
ON CONFLICT (name) DO UPDATE SET shop_types = ARRAY(SELECT DISTINCT u FROM unnest(product_suggestions.shop_types || EXCLUDED.shop_types) AS u);

-- ── STREET_TEA — Quán trà chanh ───────────────────────────────
INSERT INTO product_suggestions (name, emoji, default_price, unit, product_type_code, dynamic_price, shop_types, display_order, category_name) VALUES
('Trà chanh','🍋',12000,'Ly','BEVERAGE',FALSE,ARRAY['STREET_TEA'],10,'Trà trái cây'),
('Trà tắc','🍵',12000,'Ly','BEVERAGE',FALSE,ARRAY['STREET_TEA'],10,'Trà trái cây'),
('Trà chanh sả','🍋',15000,'Ly','BEVERAGE',FALSE,ARRAY['STREET_TEA'],10,'Trà trái cây'),
('Trà ổi hồng','🫐',20000,'Ly','BEVERAGE',FALSE,ARRAY['STREET_TEA'],10,'Trà trái cây'),
('Trà me','🍵',18000,'Ly','BEVERAGE',FALSE,ARRAY['STREET_TEA'],10,'Trà trái cây'),
('Trà dâu','🍓',20000,'Ly','BEVERAGE',FALSE,ARRAY['STREET_TEA'],10,'Trà trái cây'),
('Soda chanh','🥤',20000,'Ly','BEVERAGE',FALSE,ARRAY['STREET_TEA'],10,'Trà trái cây'),
('Chanh muối','🍋',15000,'Ly','BEVERAGE',FALSE,ARRAY['STREET_TEA'],10,'Trà trái cây')
ON CONFLICT (name) DO UPDATE SET shop_types = ARRAY(SELECT DISTINCT u FROM unnest(product_suggestions.shop_types || EXCLUDED.shop_types) AS u);

-- ════════════════ REUSED VERTICALS (enrich existing codes) ════════════════

-- ── PUB_GOAT — Quán nhậu chuyên dê ────────────────────────────
INSERT INTO product_suggestions (name, emoji, default_price, unit, product_type_code, dynamic_price, shop_types, display_order, category_name) VALUES
('Lẩu dê','🐐',350000,'Nồi','FOOD',FALSE,ARRAY['PUB_GOAT'],10,'Món chính'),
('Dê nướng tảng','🐐',250000,'Phần','FOOD',FALSE,ARRAY['PUB_GOAT'],10,'Món chính'),
('Dê hấp tía tô','🐐',220000,'Phần','FOOD',FALSE,ARRAY['PUB_GOAT'],10,'Món chính'),
('Dê xào lăn','🐐',180000,'Phần','FOOD',FALSE,ARRAY['PUB_GOAT'],10,'Món chính'),
('Dê nướng ngũ vị','🐐',200000,'Phần','FOOD',FALSE,ARRAY['PUB_GOAT'],10,'Món chính'),
('Tái dê chanh','🐐',150000,'Phần','FOOD',FALSE,ARRAY['PUB_GOAT'],10,'Món chính'),
('Ngọc dương hầm thuốc bắc','🐐',250000,'Phần','FOOD',FALSE,ARRAY['PUB_GOAT'],10,'Món chính'),
('Pín dê hầm','🐐',200000,'Phần','FOOD',FALSE,ARRAY['PUB_GOAT'],10,'Món chính'),
('Dê nướng chao','🐐',200000,'Phần','FOOD',FALSE,ARRAY['PUB_GOAT'],10,'Món chính'),
('Cháo dê','🥣',60000,'Tô','FOOD',FALSE,ARRAY['PUB_GOAT'],10,'Món chính'),
('Đậu phộng rang','🥜',20000,'Đĩa','FOOD',FALSE,ARRAY['PUB_GOAT'],50,'Mồi nhậu'),
('Khô bò','🥩',50000,'Đĩa','FOOD',FALSE,ARRAY['PUB_GOAT'],50,'Mồi nhậu'),
('Khô mực nướng','🦑',80000,'Đĩa','FOOD',FALSE,ARRAY['PUB_GOAT'],50,'Mồi nhậu'),
('Salad dê','🥗',60000,'Đĩa','FOOD',FALSE,ARRAY['PUB_GOAT'],50,'Mồi nhậu'),
('Rau nhúng dê','🥬',30000,'Đĩa','FOOD',FALSE,ARRAY['PUB_GOAT'],50,'Mồi nhậu'),
('Bia Saigon','🍺',15000,'Lon','BEVERAGE',FALSE,ARRAY['PUB_GOAT'],90,'Nước uống'),
('Bia Tiger','🍺',16000,'Lon','BEVERAGE',FALSE,ARRAY['PUB_GOAT'],90,'Nước uống'),
('Bia Heineken','🍺',25000,'Lon','BEVERAGE',FALSE,ARRAY['PUB_GOAT'],90,'Nước uống'),
('Bia 333','🍺',18000,'Lon','BEVERAGE',FALSE,ARRAY['PUB_GOAT'],90,'Nước uống'),
('Nước ngọt','🥤',15000,'Lon','BEVERAGE',FALSE,ARRAY['PUB_GOAT'],90,'Nước uống'),
('Nước suối','💧',5000,'Chai','BEVERAGE',FALSE,ARRAY['PUB_GOAT'],90,'Nước uống'),
('Rượu thuốc','🍶',30000,'Chén','BEVERAGE',FALSE,ARRAY['PUB_GOAT'],90,'Nước uống')
ON CONFLICT (name) DO UPDATE SET shop_types = ARRAY(SELECT DISTINCT u FROM unnest(product_suggestions.shop_types || EXCLUDED.shop_types) AS u);

-- ── PUB_BEEF — Quán nhậu chuyên bò ────────────────────────────
INSERT INTO product_suggestions (name, emoji, default_price, unit, product_type_code, dynamic_price, shop_types, display_order, category_name) VALUES
('Lẩu bò','🐄',320000,'Nồi','FOOD',FALSE,ARRAY['PUB_BEEF'],10,'Món chính'),
('Bò nướng lá lốt','🐄',120000,'Phần','FOOD',FALSE,ARRAY['PUB_BEEF'],10,'Món chính'),
('Bò nướng mỡ chài','🐄',130000,'Phần','FOOD',FALSE,ARRAY['PUB_BEEF'],10,'Món chính'),
('Bò lúc lắc','🐄',150000,'Phần','FOOD',FALSE,ARRAY['PUB_BEEF'],10,'Món chính'),
('Bắp bò luộc','🐄',180000,'Phần','FOOD',FALSE,ARRAY['PUB_BEEF'],10,'Món chính'),
('Gân bò hầm','🐄',150000,'Phần','FOOD',FALSE,ARRAY['PUB_BEEF'],10,'Món chính'),
('Đuôi bò hầm tiêu xanh','🐄',250000,'Phần','FOOD',FALSE,ARRAY['PUB_BEEF'],10,'Món chính'),
('Bò nhúng giấm','🐄',280000,'Phần','FOOD',FALSE,ARRAY['PUB_BEEF'],10,'Món chính'),
('Lòng bò xào nghệ','🐄',120000,'Phần','FOOD',FALSE,ARRAY['PUB_BEEF'],10,'Món chính'),
('Cháo bò','🥣',50000,'Tô','FOOD',FALSE,ARRAY['PUB_BEEF'],10,'Món chính'),
('Đậu phộng rang','🥜',20000,'Đĩa','FOOD',FALSE,ARRAY['PUB_BEEF'],50,'Mồi nhậu'),
('Khô bò','🥩',50000,'Đĩa','FOOD',FALSE,ARRAY['PUB_BEEF'],50,'Mồi nhậu'),
('Bánh tráng nướng','🫓',25000,'Cái','FOOD',FALSE,ARRAY['PUB_BEEF'],50,'Mồi nhậu'),
('Rau sống','🥬',30000,'Đĩa','FOOD',FALSE,ARRAY['PUB_BEEF'],50,'Mồi nhậu'),
('Bia Saigon','🍺',15000,'Lon','BEVERAGE',FALSE,ARRAY['PUB_BEEF'],90,'Nước uống'),
('Bia Tiger','🍺',16000,'Lon','BEVERAGE',FALSE,ARRAY['PUB_BEEF'],90,'Nước uống'),
('Bia Heineken','🍺',25000,'Lon','BEVERAGE',FALSE,ARRAY['PUB_BEEF'],90,'Nước uống'),
('Bia 333','🍺',18000,'Lon','BEVERAGE',FALSE,ARRAY['PUB_BEEF'],90,'Nước uống'),
('Nước ngọt','🥤',15000,'Lon','BEVERAGE',FALSE,ARRAY['PUB_BEEF'],90,'Nước uống'),
('Nước suối','💧',5000,'Chai','BEVERAGE',FALSE,ARRAY['PUB_BEEF'],90,'Nước uống')
ON CONFLICT (name) DO UPDATE SET shop_types = ARRAY(SELECT DISTINCT u FROM unnest(product_suggestions.shop_types || EXCLUDED.shop_types) AS u);

-- ── PUB_SEAFOOD — Quán nhậu hải sản ───────────────────────────
INSERT INTO product_suggestions (name, emoji, default_price, unit, product_type_code, dynamic_price, shop_types, display_order, category_name) VALUES
('Lẩu hải sản','🦐',350000,'Nồi','FOOD',FALSE,ARRAY['PUB_SEAFOOD'],10,'Món chính'),
('Tôm nướng muối ớt','🦐',200000,'Phần','FOOD',FALSE,ARRAY['PUB_SEAFOOD'],10,'Món chính'),
('Tôm hấp bia','🦐',180000,'Phần','FOOD',FALSE,ARRAY['PUB_SEAFOOD'],10,'Món chính'),
('Mực nướng sa tế','🦑',180000,'Phần','FOOD',FALSE,ARRAY['PUB_SEAFOOD'],10,'Món chính'),
('Mực hấp gừng','🦑',160000,'Phần','FOOD',FALSE,ARRAY['PUB_SEAFOOD'],10,'Món chính'),
('Nghêu hấp sả','🦪',100000,'Phần','FOOD',FALSE,ARRAY['PUB_SEAFOOD'],10,'Món chính'),
('Ốc hương rang me','🐚',200000,'Phần','FOOD',FALSE,ARRAY['PUB_SEAFOOD'],10,'Món chính'),
('Sò huyết nướng mỡ hành','🦪',150000,'Phần','FOOD',FALSE,ARRAY['PUB_SEAFOOD'],10,'Món chính'),
('Cua rang me','🦀',350000,'Phần','FOOD',FALSE,ARRAY['PUB_SEAFOOD'],10,'Món chính'),
('Ghẹ hấp bia','🦀',300000,'Phần','FOOD',FALSE,ARRAY['PUB_SEAFOOD'],10,'Món chính'),
('Hàu nướng phô mai','🦪',120000,'Phần','FOOD',FALSE,ARRAY['PUB_SEAFOOD'],10,'Món chính'),
('Cá nướng muối ớt','🐟',200000,'Phần','FOOD',FALSE,ARRAY['PUB_SEAFOOD'],10,'Món chính'),
('Đậu phộng rang','🥜',20000,'Đĩa','FOOD',FALSE,ARRAY['PUB_SEAFOOD'],50,'Mồi nhậu'),
('Khô mực','🦑',80000,'Đĩa','FOOD',FALSE,ARRAY['PUB_SEAFOOD'],50,'Mồi nhậu'),
('Bánh phồng tôm','🍤',25000,'Đĩa','FOOD',FALSE,ARRAY['PUB_SEAFOOD'],50,'Mồi nhậu'),
('Rau sống','🥬',30000,'Đĩa','FOOD',FALSE,ARRAY['PUB_SEAFOOD'],50,'Mồi nhậu'),
('Bia Saigon','🍺',15000,'Lon','BEVERAGE',FALSE,ARRAY['PUB_SEAFOOD'],90,'Nước uống'),
('Bia Tiger','🍺',16000,'Lon','BEVERAGE',FALSE,ARRAY['PUB_SEAFOOD'],90,'Nước uống'),
('Bia Heineken','🍺',25000,'Lon','BEVERAGE',FALSE,ARRAY['PUB_SEAFOOD'],90,'Nước uống'),
('Nước ngọt','🥤',15000,'Lon','BEVERAGE',FALSE,ARRAY['PUB_SEAFOOD'],90,'Nước uống'),
('Nước suối','💧',5000,'Chai','BEVERAGE',FALSE,ARRAY['PUB_SEAFOOD'],90,'Nước uống')
ON CONFLICT (name) DO UPDATE SET shop_types = ARRAY(SELECT DISTINCT u FROM unnest(product_suggestions.shop_types || EXCLUDED.shop_types) AS u);

-- ── RESTAURANT — Nhà hàng / Quán ăn ───────────────────────────
INSERT INTO product_suggestions (name, emoji, default_price, unit, product_type_code, dynamic_price, shop_types, display_order, category_name) VALUES
('Cơm chiên Dương Châu','🍚',60000,'Phần','FOOD',FALSE,ARRAY['RESTAURANT'],10,'Món chính'),
('Cơm chiên hải sản','🍚',70000,'Phần','FOOD',FALSE,ARRAY['RESTAURANT'],10,'Món chính'),
('Gà chiên nước mắm','🍗',150000,'Phần','FOOD',FALSE,ARRAY['RESTAURANT'],10,'Món chính'),
('Gà hấp hành','🍗',200000,'Con','FOOD',FALSE,ARRAY['RESTAURANT'],10,'Món chính'),
('Sườn xào chua ngọt','🍖',120000,'Phần','FOOD',FALSE,ARRAY['RESTAURANT'],10,'Món chính'),
('Bò xào rau muống','🥩',100000,'Phần','FOOD',FALSE,ARRAY['RESTAURANT'],10,'Món chính'),
('Canh chua cá','🍲',100000,'Tô','FOOD',FALSE,ARRAY['RESTAURANT'],10,'Món chính'),
('Cá kho tộ','🐟',120000,'Phần','FOOD',FALSE,ARRAY['RESTAURANT'],10,'Món chính'),
('Đậu hũ sốt cà','🧈',50000,'Phần','FOOD',FALSE,ARRAY['RESTAURANT'],10,'Món chính'),
('Lẩu Thái','🍲',250000,'Nồi','FOOD',FALSE,ARRAY['RESTAURANT'],10,'Món chính'),
('Mực xào thập cẩm','🦑',120000,'Phần','FOOD',FALSE,ARRAY['RESTAURANT'],10,'Món chính'),
('Chả giò','🥢',50000,'Phần','FOOD',FALSE,ARRAY['RESTAURANT'],10,'Món chính'),
('Cơm trắng','🍚',10000,'Chén','FOOD',FALSE,ARRAY['RESTAURANT'],50,'Món thêm')
ON CONFLICT (name) DO UPDATE SET shop_types = ARRAY(SELECT DISTINCT u FROM unnest(product_suggestions.shop_types || EXCLUDED.shop_types) AS u);

-- ── COFFEE_SHOP — Quán cà phê ─────────────────────────────────
INSERT INTO product_suggestions (name, emoji, default_price, unit, product_type_code, dynamic_price, shop_types, display_order, category_name) VALUES
('Cà phê đen','☕',20000,'Ly','BEVERAGE',FALSE,ARRAY['COFFEE_SHOP'],10,'Cà phê'),
('Cà phê sữa','☕',25000,'Ly','BEVERAGE',FALSE,ARRAY['COFFEE_SHOP'],10,'Cà phê'),
('Bạc xỉu','☕',30000,'Ly','BEVERAGE',FALSE,ARRAY['COFFEE_SHOP'],10,'Cà phê'),
('Cà phê muối','☕',30000,'Ly','BEVERAGE',FALSE,ARRAY['COFFEE_SHOP'],10,'Cà phê'),
('Cold brew','☕',40000,'Ly','BEVERAGE',FALSE,ARRAY['COFFEE_SHOP'],10,'Cà phê'),
('Espresso','☕',35000,'Ly','BEVERAGE',FALSE,ARRAY['COFFEE_SHOP'],10,'Cà phê'),
('Americano','☕',35000,'Ly','BEVERAGE',FALSE,ARRAY['COFFEE_SHOP'],10,'Cà phê'),
('Cappuccino','☕',45000,'Ly','BEVERAGE',FALSE,ARRAY['COFFEE_SHOP'],10,'Cà phê'),
('Latte','☕',45000,'Ly','BEVERAGE',FALSE,ARRAY['COFFEE_SHOP'],10,'Cà phê'),
('Trà đào cam sả','🍑',35000,'Ly','BEVERAGE',FALSE,ARRAY['COFFEE_SHOP'],20,'Trà & Đá xay'),
('Matcha latte','🍵',45000,'Ly','BEVERAGE',FALSE,ARRAY['COFFEE_SHOP'],20,'Trà & Đá xay'),
('Matcha đá xay','🍵',50000,'Ly','BEVERAGE',FALSE,ARRAY['COFFEE_SHOP'],20,'Trà & Đá xay'),
('Sô-cô-la đá xay','🍫',50000,'Ly','BEVERAGE',FALSE,ARRAY['COFFEE_SHOP'],20,'Trà & Đá xay')
ON CONFLICT (name) DO UPDATE SET shop_types = ARRAY(SELECT DISTINCT u FROM unnest(product_suggestions.shop_types || EXCLUDED.shop_types) AS u);
