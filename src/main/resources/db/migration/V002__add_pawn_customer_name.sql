-- B4: Store guest name at pawn level so visiting-guest names aren't lost
-- when the shared walk-in customer record (phone=0000000000) already exists.
ALTER TABLE pawn ADD COLUMN IF NOT EXISTS customer_name VARCHAR(255);
