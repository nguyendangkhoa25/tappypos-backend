-- ════════════════════════════════════════════════════════════
-- V014: fix cross-type tagging + price realism for beauty suggestions
--
-- 1. "Lấy ráy tai" / ear-cleaning services were tagged to both barber shops AND spa
--    (from V001). Keep them in the barber family only — a spa shouldn't be offered
--    barber ear-cleaning at onboarding. array_remove drops the SPA_SHOP tag; the
--    barber tags (BARBER_SHOP, BARBER_SHOP_MEN) remain.
-- 2. Correct one unrealistic default price (full-face thread lift ≈ 15tr in VN, not 5tr).
-- ════════════════════════════════════════════════════════════

UPDATE product_suggestions
SET shop_types = array_remove(shop_types, 'SPA_SHOP')
WHERE name IN (
    'Lấy ráy tai thường',
    'Lấy ráy tai chuyên sâu',
    'Lấy ráy tai bằng máy',
    'Massage tai + lấy ráy tai',
    'Vệ sinh tai toàn diện'
)
AND 'SPA_SHOP' = ANY(shop_types);

UPDATE product_suggestions
SET default_price = 15000000
WHERE name = 'Căng chỉ nâng cơ mặt';
