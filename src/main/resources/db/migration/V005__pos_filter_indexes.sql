-- V005 — Indexes for the POS product-grid filters (Phase 3).
--
-- The POS grid added category and product-type filtering (InventoryRepository.findAllActiveByCategoryId /
-- searchByKeywordAndCategoryId and ProductRepository.searchByKeywordFiltered). Those run on the hottest
-- POS path (every chip tap + every debounced keystroke) and filter on columns that had no leading index:
--
--   * product_category.category_id — the PRIMARY KEY is (product_id, category_id), so a filter by
--     category_id alone could not use it and fell back to a sequential scan of the join table.
--   * product.product_type_id — no index existed (only idx_product_tenant_id on tenant_id alone).
--
-- Both filters always run tenant-scoped (RLS adds tenant_id = current_setting('app.current_tenant')),
-- so the indexes lead with tenant_id to match the actual predicate.

CREATE INDEX IF NOT EXISTS idx_product_category_category
    ON product_category (tenant_id, category_id);

CREATE INDEX IF NOT EXISTS idx_product_tenant_type
    ON product (tenant_id, product_type_id);
