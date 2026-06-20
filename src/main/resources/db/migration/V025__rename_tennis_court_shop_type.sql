-- ══════════════════════════════════════════════════════════════════════════════
-- Rename the TENNIS_COURT ShopType enum value to SPORT_COURT (SPORT_COURT_SHOP_TYPE_PLAN).
-- The enum now spans tennis / cầu lông / pickleball / sân bóng mini — any court rented by
-- time — so the broader SPORT_COURT name is more accurate.
--
-- `tenants.shop_type` and `contact_leads.shop_type` persist the enum as a STRING
-- (@Enumerated(EnumType.STRING)), so any existing row holding the old literal would fail to
-- deserialize once the Java enum constant is renamed. Migrate the stored values in place.
-- Both are master tables (no RLS); this is a plain data update.
-- ══════════════════════════════════════════════════════════════════════════════

UPDATE tenants      SET shop_type = 'SPORT_COURT' WHERE shop_type = 'TENNIS_COURT';
UPDATE contact_leads SET shop_type = 'SPORT_COURT' WHERE shop_type = 'TENNIS_COURT';
