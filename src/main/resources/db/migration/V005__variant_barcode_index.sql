-- ══════════════════════════════════════════════════════════════════════════════
-- Per-variant barcode lookup index (fashion 4b — scan a size/color SKU at POS)
--
-- product_variants.barcode already exists (V001). This adds a partial index so the
-- POS barcode scan can resolve a variant SKU directly. RLS on product_variants is
-- already in place (V001), so this index is automatically tenant-scoped at query time.
-- ══════════════════════════════════════════════════════════════════════════════

CREATE INDEX IF NOT EXISTS idx_product_variants_barcode
    ON product_variants (tenant_id, barcode)
    WHERE barcode IS NOT NULL AND deleted = FALSE;
