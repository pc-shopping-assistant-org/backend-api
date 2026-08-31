-- =====================================================================
-- Shipping tariff configuration
-- =====================================================================
-- Keep the tariff on the normalized shipping method. Checkout snapshots the
-- current value into orders.shipping_fee, so changing this value never changes
-- historical orders.

ALTER TABLE shipping_methods
    ADD COLUMN IF NOT EXISTS fee bigint NOT NULL DEFAULT 0;

-- Be defensive if a partially applied/manual migration already created the
-- column without the canonical nullability/default.
UPDATE shipping_methods
SET fee = 0
WHERE fee IS NULL;

ALTER TABLE shipping_methods
    ALTER COLUMN fee SET DEFAULT 0;

ALTER TABLE shipping_methods
    ALTER COLUMN fee SET NOT NULL;

ALTER TABLE shipping_methods
    DROP CONSTRAINT IF EXISTS shipping_methods_fee_check;

ALTER TABLE shipping_methods
    ADD CONSTRAINT shipping_methods_fee_check CHECK (fee >= 0);

-- The previous seed only created methods and left their fee implicit. Migrate
-- those canonical seed rows to the initial tariffs used by checkout.
UPDATE shipping_methods
SET fee = CASE code
    WHEN 'STANDARD' THEN 0
    WHEN 'EXPRESS' THEN 30000
    WHEN 'SAME_DAY' THEN 50000
    ELSE fee
END
WHERE code IN ('STANDARD', 'EXPRESS', 'SAME_DAY');
