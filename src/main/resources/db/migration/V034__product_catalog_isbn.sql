-- ══════════════════════════════════════════════════════════════════════════════
-- V034 — product_catalog ISBN support · BOOK_STORE_SHOP_TYPE_PLAN §4c.
--
-- The shared product_catalog (global, non-RLS master reference) gains an `isbn`
-- column so book lookups via Google Books can be cached alongside the Open Food
-- Facts barcode rows. ISBNs are EAN-13 (978/979 "Bookland" prefix) so they still
-- key on `barcode`; `isbn` is the human-facing book identifier and a reverse-lookup
-- key. `source` now also takes 'GOOGLE_BOOKS' (alongside 'OPEN_FOOD_FACTS'/'MANUAL').
--
-- Not a tenant table — no RLS / legacy_id (product_catalog is shared across tenants).
-- ══════════════════════════════════════════════════════════════════════════════

ALTER TABLE product_catalog ADD COLUMN IF NOT EXISTS isbn VARCHAR(20) DEFAULT NULL;

CREATE INDEX IF NOT EXISTS idx_product_catalog_isbn
    ON product_catalog (isbn) WHERE isbn IS NOT NULL;
