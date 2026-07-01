-- ════════════════════════════════════════════════════════════
-- V013: expand product_suggestions for the beauty / service verticals
--
-- The 9 beauty/service shop types already ship rich seed lists in V001. This adds
-- ~12 more services each, reusing each type's existing category_name groups and the
-- 'SERVICE' product type. One INSERT per shop type (names unique within a statement);
-- ON CONFLICT (name) DO UPDATE merges shop_types so a service shared across types
-- (e.g. "Tẩy tóc nam" in both barber types) accumulates every type that offers it.
-- ════════════════════════════════════════════════════════════

-- ── BARBER_SHOP — Tiệm cắt tóc / Salon ────────────────────────
INSERT INTO product_suggestions (name, emoji, default_price, unit, product_type_code, dynamic_price, shop_types, display_order, category_name) VALUES
('Cắt tóc kiểu Hàn Quốc','💇‍♂️',120000,'Lần','SERVICE',FALSE,ARRAY['BARBER_SHOP'],500,'Cắt tóc'),
('Cắt tóc Mohican / Pompadour','💇‍♂️',130000,'Lần','SERVICE',FALSE,ARRAY['BARBER_SHOP'],500,'Cắt tóc'),
('Nhổ tóc bạc','✂️',50000,'Lần','SERVICE',FALSE,ARRAY['BARBER_SHOP'],500,'Cắt tóc'),
('Uốn tóc nam Hàn Quốc','💈',300000,'Lần','SERVICE',FALSE,ARRAY['BARBER_SHOP'],510,'Uốn & Nhuộm'),
('Nhuộm tóc nam thời trang','💈',350000,'Lần','SERVICE',FALSE,ARRAY['BARBER_SHOP'],510,'Uốn & Nhuộm'),
('Tẩy tóc nam','💈',300000,'Lần','SERVICE',FALSE,ARRAY['BARBER_SHOP'],510,'Uốn & Nhuộm'),
('Cạo mặt lấy nhân mụn','🪒',60000,'Lần','SERVICE',FALSE,ARRAY['BARBER_SHOP'],520,'Cạo & Chăm sóc râu'),
('Đắp mặt nạ dưỡng da nam','🧖',80000,'Lần','SERVICE',FALSE,ARRAY['BARBER_SHOP'],520,'Cạo & Chăm sóc râu'),
('Gội đầu dưỡng sinh thảo dược','💆',120000,'Lần','SERVICE',FALSE,ARRAY['BARBER_SHOP'],530,'Gội đầu & Massage'),
('Massage cổ vai gáy','💆',100000,'Lần','SERVICE',FALSE,ARRAY['BARBER_SHOP'],530,'Gội đầu & Massage'),
('Combo cắt + gội + cạo râu + ráy tai','💈',200000,'Lần','SERVICE',FALSE,ARRAY['BARBER_SHOP'],590,'Combo & Gói'),
('Gói chăm sóc VIP nam','💈',400000,'Gói','SERVICE',FALSE,ARRAY['BARBER_SHOP'],590,'Combo & Gói')
ON CONFLICT (name) DO UPDATE SET shop_types = ARRAY(SELECT DISTINCT u FROM unnest(product_suggestions.shop_types || EXCLUDED.shop_types) AS u);

-- ── BARBER_SHOP_MEN — Tiệm tóc nam / Barber ───────────────────
INSERT INTO product_suggestions (name, emoji, default_price, unit, product_type_code, dynamic_price, shop_types, display_order, category_name) VALUES
('Cắt tóc Pompadour','💇‍♂️',160000,'Lần','SERVICE',FALSE,ARRAY['BARBER_SHOP_MEN'],500,'Cắt tóc'),
('Cắt tóc Crew Cut','💇‍♂️',90000,'Lần','SERVICE',FALSE,ARRAY['BARBER_SHOP_MEN'],500,'Cắt tóc'),
('Cắt tóc Slick Back','💇‍♂️',150000,'Lần','SERVICE',FALSE,ARRAY['BARBER_SHOP_MEN'],500,'Cắt tóc'),
('Uốn tóc nam (Perm)','💈',350000,'Lần','SERVICE',FALSE,ARRAY['BARBER_SHOP_MEN'],510,'Tạo kiểu'),
('Duỗi tóc nam','💈',300000,'Lần','SERVICE',FALSE,ARRAY['BARBER_SHOP_MEN'],510,'Tạo kiểu'),
('Nhuộm highlight nam','💈',400000,'Lần','SERVICE',FALSE,ARRAY['BARBER_SHOP_MEN'],510,'Tạo kiểu'),
('Tẩy tóc nam','💈',300000,'Lần','SERVICE',FALSE,ARRAY['BARBER_SHOP_MEN'],510,'Tạo kiểu'),
('Nhuộm phủ bạc nam','💈',200000,'Lần','SERVICE',FALSE,ARRAY['BARBER_SHOP_MEN'],510,'Tạo kiểu'),
('Đắp mặt nạ than hoạt tính','🧖',80000,'Lần','SERVICE',FALSE,ARRAY['BARBER_SHOP_MEN'],520,'Cạo & Chăm sóc râu'),
('Lấy nhân mụn mặt nam','🪒',60000,'Lần','SERVICE',FALSE,ARRAY['BARBER_SHOP_MEN'],520,'Cạo & Chăm sóc râu'),
('Gội đầu dưỡng sinh (nam)','💆',120000,'Lần','SERVICE',FALSE,ARRAY['BARBER_SHOP_MEN'],530,'Gội đầu & Massage'),
('Combo cắt + gội + ráy tai + massage','💈',250000,'Lần','SERVICE',FALSE,ARRAY['BARBER_SHOP_MEN'],590,'Combo')
ON CONFLICT (name) DO UPDATE SET shop_types = ARRAY(SELECT DISTINCT u FROM unnest(product_suggestions.shop_types || EXCLUDED.shop_types) AS u);

-- ── HAIR_SALON — Salon tóc / Làm tóc ──────────────────────────
INSERT INTO product_suggestions (name, emoji, default_price, unit, product_type_code, dynamic_price, shop_types, display_order, category_name) VALUES
('Cắt tóc tém / Pixie','💇',130000,'Lần','SERVICE',FALSE,ARRAY['HAIR_SALON'],500,'Cắt tóc'),
('Cắt mái / Tỉa mái','💇',50000,'Lần','SERVICE',FALSE,ARRAY['HAIR_SALON'],500,'Cắt tóc'),
('Nhuộm Balayage','💈',700000,'Lần','SERVICE',FALSE,ARRAY['HAIR_SALON'],510,'Nhuộm tóc'),
('Nhuộm khói / Ash','💈',650000,'Lần','SERVICE',FALSE,ARRAY['HAIR_SALON'],510,'Nhuộm tóc'),
('Uốn lạnh Setting','💈',550000,'Lần','SERVICE',FALSE,ARRAY['HAIR_SALON'],520,'Uốn & Duỗi'),
('Uốn cụp đuôi (C-curl)','💈',400000,'Lần','SERVICE',FALSE,ARRAY['HAIR_SALON'],520,'Uốn & Duỗi'),
('Bấm xù chân tóc','💈',450000,'Lần','SERVICE',FALSE,ARRAY['HAIR_SALON'],520,'Uốn & Duỗi'),
('Hấp dầu phục hồi Keratin','💆',350000,'Lần','SERVICE',FALSE,ARRAY['HAIR_SALON'],530,'Chăm sóc tóc'),
('Ủ tóc Botox','💆',500000,'Lần','SERVICE',FALSE,ARRAY['HAIR_SALON'],530,'Chăm sóc tóc'),
('Cấy Protein phục hồi tóc','💆',600000,'Lần','SERVICE',FALSE,ARRAY['HAIR_SALON'],530,'Chăm sóc tóc'),
('Nối tóc (mỗi bộ)','💇',1500000,'Lần','SERVICE',FALSE,ARRAY['HAIR_SALON'],540,'Tạo kiểu & Combo'),
('Búi tóc cô dâu / dự tiệc','💇',300000,'Lần','SERVICE',FALSE,ARRAY['HAIR_SALON'],540,'Tạo kiểu & Combo'),
('Combo cắt + nhuộm + hấp','💈',700000,'Gói','SERVICE',FALSE,ARRAY['HAIR_SALON'],590,'Tạo kiểu & Combo')
ON CONFLICT (name) DO UPDATE SET shop_types = ARRAY(SELECT DISTINCT u FROM unnest(product_suggestions.shop_types || EXCLUDED.shop_types) AS u);

-- ── NAIL_SHOP — Tiệm nail / Làm móng ──────────────────────────
INSERT INTO product_suggestions (name, emoji, default_price, unit, product_type_code, dynamic_price, shop_types, display_order, category_name) VALUES
('Sơn gel ombre','💅',200000,'Lần','SERVICE',FALSE,ARRAY['NAIL_SHOP'],500,'Gel & Acrylic'),
('Sơn gel cat-eye (đá mèo)','💅',220000,'Lần','SERVICE',FALSE,ARRAY['NAIL_SHOP'],500,'Gel & Acrylic'),
('Đắp móng bột màu','💅',320000,'Lần','SERVICE',FALSE,ARRAY['NAIL_SHOP'],500,'Gel & Acrylic'),
('Úp móng (Press-on)','💅',150000,'Lần','SERVICE',FALSE,ARRAY['NAIL_SHOP'],500,'Gel & Acrylic'),
('Vẽ nail 3D nổi','🎨',400000,'Lần','SERVICE',FALSE,ARRAY['NAIL_SHOP'],510,'Vẽ nail & Nghệ thuật'),
('Vẽ nail hoa nổi (mỗi ngón)','🎨',50000,'Lần','SERVICE',FALSE,ARRAY['NAIL_SHOP'],510,'Vẽ nail & Nghệ thuật'),
('Dán sticker nail','🎨',30000,'Lần','SERVICE',FALSE,ARRAY['NAIL_SHOP'],510,'Vẽ nail & Nghệ thuật'),
('Cắt da tay + dưỡng','✋',60000,'Lần','SERVICE',FALSE,ARRAY['NAIL_SHOP'],520,'Chăm sóc bàn tay'),
('Nhúng parafin dưỡng tay','✋',120000,'Lần','SERVICE',FALSE,ARRAY['NAIL_SHOP'],520,'Chăm sóc bàn tay'),
('Ngâm chân thảo dược + massage','🦶',150000,'Lần','SERVICE',FALSE,ARRAY['NAIL_SHOP'],530,'Chăm sóc bàn chân'),
('Sơn móng chân trẻ em','🦶',40000,'Lần','SERVICE',FALSE,ARRAY['NAIL_SHOP'],530,'Chăm sóc bàn chân'),
('Combo tháo + sơn gel mới','💅',200000,'Lần','SERVICE',FALSE,ARRAY['NAIL_SHOP'],590,'Combo & Gói dịch vụ'),
('Gói nail tháng (4 lần)','💅',800000,'Gói','SERVICE',FALSE,ARRAY['NAIL_SHOP'],590,'Combo & Gói dịch vụ')
ON CONFLICT (name) DO UPDATE SET shop_types = ARRAY(SELECT DISTINCT u FROM unnest(product_suggestions.shop_types || EXCLUDED.shop_types) AS u);

-- ── LASH_PMU_STUDIO — Tiệm mi / Xăm thẩm mỹ ───────────────────
INSERT INTO product_suggestions (name, emoji, default_price, unit, product_type_code, dynamic_price, shop_types, display_order, category_name) VALUES
('Nối mi Classic 1-1','👁️',250000,'Lần','SERVICE',FALSE,ARRAY['LASH_PMU_STUDIO'],500,'Nối mi'),
('Nối mi khối (Volume nhẹ)','👁️',300000,'Lần','SERVICE',FALSE,ARRAY['LASH_PMU_STUDIO'],500,'Nối mi'),
('Uốn mi (Lift mi)','👁️',200000,'Lần','SERVICE',FALSE,ARRAY['LASH_PMU_STUDIO'],500,'Nối mi'),
('Nhuộm mi','👁️',100000,'Lần','SERVICE',FALSE,ARRAY['LASH_PMU_STUDIO'],500,'Nối mi'),
('Phun mày hạt bột (Shading)','🖌️',1800000,'Lần','SERVICE',FALSE,ARRAY['LASH_PMU_STUDIO'],510,'Xăm mày'),
('Điêu khắc mày sợi','🖌️',2200000,'Lần','SERVICE',FALSE,ARRAY['LASH_PMU_STUDIO'],510,'Xăm mày'),
('Phun môi Collagen','💋',2800000,'Lần','SERVICE',FALSE,ARRAY['LASH_PMU_STUDIO'],520,'Xăm môi'),
('Phun môi pha lê','💋',3200000,'Lần','SERVICE',FALSE,ARRAY['LASH_PMU_STUDIO'],520,'Xăm môi'),
('Khử thâm môi','💋',1500000,'Lần','SERVICE',FALSE,ARRAY['LASH_PMU_STUDIO'],520,'Xăm môi'),
('Xăm mí dưới','👁️',1200000,'Lần','SERVICE',FALSE,ARRAY['LASH_PMU_STUDIO'],530,'Xăm mí mắt'),
('Dưỡng hồng môi sau phun','💋',300000,'Lần','SERVICE',FALSE,ARRAY['LASH_PMU_STUDIO'],540,'Chăm sóc & Tháo'),
('Xóa / Sửa mày cũ (laser nhẹ)','🖌️',800000,'Lần','SERVICE',FALSE,ARRAY['LASH_PMU_STUDIO'],540,'Chăm sóc & Tháo'),
('Combo nối mi + nhuộm mi','👁️',350000,'Lần','SERVICE',FALSE,ARRAY['LASH_PMU_STUDIO'],590,'Combo')
ON CONFLICT (name) DO UPDATE SET shop_types = ARRAY(SELECT DISTINCT u FROM unnest(product_suggestions.shop_types || EXCLUDED.shop_types) AS u);

-- ── SPA_SHOP — Spa / Thẩm mỹ viện ─────────────────────────────
INSERT INTO product_suggestions (name, emoji, default_price, unit, product_type_code, dynamic_price, shop_types, display_order, category_name) VALUES
('Massage Thái cổ truyền','💆',400000,'Lần','SERVICE',FALSE,ARRAY['SPA_SHOP'],500,'Massage'),
('Massage bà bầu','💆',350000,'Lần','SERVICE',FALSE,ARRAY['SPA_SHOP'],500,'Massage'),
('Massage trị liệu cổ vai gáy','💆',300000,'Lần','SERVICE',FALSE,ARRAY['SPA_SHOP'],500,'Massage'),
('Chăm sóc da chuyên sâu Hàn Quốc','✨',500000,'Lần','SERVICE',FALSE,ARRAY['SPA_SHOP'],510,'Chăm sóc da mặt'),
('Điện di Vitamin C','✨',350000,'Lần','SERVICE',FALSE,ARRAY['SPA_SHOP'],510,'Chăm sóc da mặt'),
('Trẻ hóa da bằng ánh sáng LED','💡',300000,'Lần','SERVICE',FALSE,ARRAY['SPA_SHOP'],520,'Điều trị đặc biệt'),
('Cấy tảo biển phục hồi da','🌿',600000,'Lần','SERVICE',FALSE,ARRAY['SPA_SHOP'],520,'Điều trị đặc biệt'),
('Tắm trắng phi thuyền','🛁',500000,'Lần','SERVICE',FALSE,ARRAY['SPA_SHOP'],530,'Chăm sóc cơ thể'),
('Ủ dưỡng trắng body cao cấp','🧖',550000,'Lần','SERVICE',FALSE,ARRAY['SPA_SHOP'],530,'Chăm sóc cơ thể'),
('Triệt lông nách (liệu trình)','✨',300000,'Gói','SERVICE',FALSE,ARRAY['SPA_SHOP'],540,'Waxing & Triệt lông'),
('Triệt lông tay chân (liệu trình)','✨',600000,'Gói','SERVICE',FALSE,ARRAY['SPA_SHOP'],540,'Waxing & Triệt lông'),
('Wax lông tay','🧴',100000,'Lần','SERVICE',FALSE,ARRAY['SPA_SHOP'],540,'Waxing & Triệt lông'),
('Gói chăm sóc da 10 buổi','✨',3000000,'Gói','SERVICE',FALSE,ARRAY['SPA_SHOP'],590,'Combo & Liệu trình')
ON CONFLICT (name) DO UPDATE SET shop_types = ARRAY(SELECT DISTINCT u FROM unnest(product_suggestions.shop_types || EXCLUDED.shop_types) AS u);

-- ── MASSAGE_SHOP — Tiệm massage / Xoa bóp ─────────────────────
INSERT INTO product_suggestions (name, emoji, default_price, unit, product_type_code, dynamic_price, shop_types, display_order, category_name) VALUES
('Massage Shiatsu Nhật Bản','💆',400000,'Lần','SERVICE',FALSE,ARRAY['MASSAGE_SHOP'],500,'Massage toàn thân'),
('Massage Thụy Điển','💆',380000,'Lần','SERVICE',FALSE,ARRAY['MASSAGE_SHOP'],500,'Massage toàn thân'),
('Massage giác hơi (Cupping)','💆',250000,'Lần','SERVICE',FALSE,ARRAY['MASSAGE_SHOP'],510,'Massage lưng & cổ'),
('Đánh cảm giác hơi','💆',150000,'Lần','SERVICE',FALSE,ARRAY['MASSAGE_SHOP'],510,'Massage lưng & cổ'),
('Massage cổ vai gáy chuyên sâu 60p','💆',250000,'Lần','SERVICE',FALSE,ARRAY['MASSAGE_SHOP'],510,'Massage lưng & cổ'),
('Massage bấm huyệt bàn chân Trung Hoa','🦶',200000,'Lần','SERVICE',FALSE,ARRAY['MASSAGE_SHOP'],520,'Massage chân phản xạ'),
('Massage mặt thư giãn','💆',150000,'Lần','SERVICE',FALSE,ARRAY['MASSAGE_SHOP'],530,'Massage đầu & vai gáy'),
('Gội đầu dưỡng sinh + massage','💆',180000,'Lần','SERVICE',FALSE,ARRAY['MASSAGE_SHOP'],530,'Massage đầu & vai gáy'),
('Ngâm chân ngải cứu','🦶',100000,'Lần','SERVICE',FALSE,ARRAY['MASSAGE_SHOP'],540,'Xông hơi & Ngâm'),
('Xông hơi tinh dầu sả','♨️',120000,'Lần','SERVICE',FALSE,ARRAY['MASSAGE_SHOP'],540,'Xông hơi & Ngâm'),
('Combo massage toàn thân + giác hơi','💆',450000,'Lần','SERVICE',FALSE,ARRAY['MASSAGE_SHOP'],590,'Combo'),
('Gói massage 10 buổi','💆',2500000,'Gói','SERVICE',FALSE,ARRAY['MASSAGE_SHOP'],590,'Combo')
ON CONFLICT (name) DO UPDATE SET shop_types = ARRAY(SELECT DISTINCT u FROM unnest(product_suggestions.shop_types || EXCLUDED.shop_types) AS u);

-- ── BEAUTY_CLINIC — Thẩm mỹ viện ──────────────────────────────
INSERT INTO product_suggestions (name, emoji, default_price, unit, product_type_code, dynamic_price, shop_types, display_order, category_name) VALUES
('Tiêm Meso trẻ hóa da','💉',1500000,'Lần','SERVICE',FALSE,ARRAY['BEAUTY_CLINIC'],500,'Công nghệ thẩm mỹ'),
('Tiêm Filler má / cằm','💉',3000000,'Lần','SERVICE',FALSE,ARRAY['BEAUTY_CLINIC'],500,'Công nghệ thẩm mỹ'),
('Tiêm Botox thon gọn hàm','💉',2500000,'Lần','SERVICE',FALSE,ARRAY['BEAUTY_CLINIC'],500,'Công nghệ thẩm mỹ'),
('Căng chỉ nâng cơ mặt','✨',5000000,'Lần','SERVICE',FALSE,ARRAY['BEAUTY_CLINIC'],500,'Công nghệ thẩm mỹ'),
('Cấy tinh chất trắng da','💉',1200000,'Lần','SERVICE',FALSE,ARRAY['BEAUTY_CLINIC'],500,'Công nghệ thẩm mỹ'),
('Điều trị sẹo rỗ (Laser CO2 Fractional)','🔬',1500000,'Lần','SERVICE',FALSE,ARRAY['BEAUTY_CLINIC'],510,'Trị mụn & Nám'),
('Trị tàn nhang Laser Pico','🔬',1800000,'Lần','SERVICE',FALSE,ARRAY['BEAUTY_CLINIC'],510,'Trị mụn & Nám'),
('Trị hồng ban / mao mạch','🔬',1200000,'Lần','SERVICE',FALSE,ARRAY['BEAUTY_CLINIC'],510,'Trị mụn & Nám'),
('Thu nhỏ lỗ chân lông','🔬',800000,'Lần','SERVICE',FALSE,ARRAY['BEAUTY_CLINIC'],510,'Trị mụn & Nám'),
('Triệt lông toàn thân (liệu trình)','✨',5000000,'Gói','SERVICE',FALSE,ARRAY['BEAUTY_CLINIC'],520,'Waxing & Triệt lông'),
('Triệt lông mặt','✨',400000,'Lần','SERVICE',FALSE,ARRAY['BEAUTY_CLINIC'],520,'Waxing & Triệt lông'),
('Gói trẻ hóa da công nghệ cao 5 buổi','✨',6000000,'Gói','SERVICE',FALSE,ARRAY['BEAUTY_CLINIC'],590,'Combo & Liệu trình')
ON CONFLICT (name) DO UPDATE SET shop_types = ARRAY(SELECT DISTINCT u FROM unnest(product_suggestions.shop_types || EXCLUDED.shop_types) AS u);

-- ── MAKEUP_STUDIO — Tiệm trang điểm / Studio cô dâu ───────────
INSERT INTO product_suggestions (name, emoji, default_price, unit, product_type_code, dynamic_price, shop_types, display_order, category_name) VALUES
('Trang điểm kỷ yếu','💄',250000,'Lần','SERVICE',FALSE,ARRAY['MAKEUP_STUDIO'],500,'Trang điểm ngày thường'),
('Dạy trang điểm cá nhân (buổi)','💄',500000,'Lần','SERVICE',FALSE,ARRAY['MAKEUP_STUDIO'],500,'Trang điểm ngày thường'),
('Trang điểm dạm ngõ / ăn hỏi','💄',600000,'Lần','SERVICE',FALSE,ARRAY['MAKEUP_STUDIO'],510,'Trang điểm cô dâu'),
('Trang điểm cô dâu Concept','💄',2000000,'Lần','SERVICE',FALSE,ARRAY['MAKEUP_STUDIO'],510,'Trang điểm cô dâu'),
('Make up nam (dự sự kiện)','💄',300000,'Lần','SERVICE',FALSE,ARRAY['MAKEUP_STUDIO'],520,'Trang điểm đi tiệc'),
('Trang điểm nghệ thuật / Body paint','🎨',800000,'Lần','SERVICE',FALSE,ARRAY['MAKEUP_STUDIO'],520,'Trang điểm đi tiệc'),
('Gắn mi giả','👁️',100000,'Lần','SERVICE',FALSE,ARRAY['MAKEUP_STUDIO'],530,'Làm tóc & Phụ kiện'),
('Làm tóc xoăn tạm thời','💇',200000,'Lần','SERVICE',FALSE,ARRAY['MAKEUP_STUDIO'],530,'Làm tóc & Phụ kiện'),
('Cho thuê áo dài / phụ kiện cưới','👗',500000,'Lần','SERVICE',FALSE,ARRAY['MAKEUP_STUDIO'],530,'Làm tóc & Phụ kiện'),
('Trang điểm + làm tóc chụp kỷ yếu nhóm','💄',1000000,'Gói','SERVICE',FALSE,ARRAY['MAKEUP_STUDIO'],590,'Combo & Gói cưới'),
('Gói cưới trọn gói (makeup + tóc + áo)','💄',3500000,'Gói','SERVICE',FALSE,ARRAY['MAKEUP_STUDIO'],590,'Combo & Gói cưới'),
('Trang điểm cô dâu 2 Concept trong ngày','💄',3000000,'Gói','SERVICE',FALSE,ARRAY['MAKEUP_STUDIO'],590,'Combo & Gói cưới')
ON CONFLICT (name) DO UPDATE SET shop_types = ARRAY(SELECT DISTINCT u FROM unnest(product_suggestions.shop_types || EXCLUDED.shop_types) AS u);
