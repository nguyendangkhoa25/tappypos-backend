-- Expand e_invoice_password and e_invoice_key to TEXT so that
-- AES-256-GCM ciphertexts (which are longer than the original VARCHAR values)
-- can be stored without truncation.
-- Run against every tenant database.

ALTER TABLE shop_info
    MODIFY COLUMN e_invoice_password TEXT NULL,
    MODIFY COLUMN e_invoice_key       TEXT NULL;
