-- Order table_label i18n: lodging check-out stored a frozen "Phòng {roomNumber}" string into
-- orders.table_label. Store a message key + JSON args for that system case so the label renders in
-- the reader's locale at read time (OrderServiceImpl.mapToDTO + receipt). The literal table_label
-- column stays for real F&B table names / booking resource names (user/shop data) and legacy rows.
-- No backfill.

ALTER TABLE orders ADD COLUMN IF NOT EXISTS table_label_key  VARCHAR(150) DEFAULT NULL;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS table_label_args TEXT         DEFAULT NULL;
