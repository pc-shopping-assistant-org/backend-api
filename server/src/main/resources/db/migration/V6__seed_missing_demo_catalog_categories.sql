-- Keep every category referenced by the repeatable demo catalog available on a
-- fresh database.  DataInitializer deliberately skips products whose category
-- is missing, so these rows must be part of the Flyway-owned bootstrap data.

INSERT INTO categories (name, seo_name, parent_id, status)
SELECT seed.name, seed.seo_name, parent.id, 'ACTIVE'
FROM (
    VALUES
        ('PC để bàn', 'pc-de-ban'),
        ('Điện thoại', 'dien-thoai')
) AS seed(name, seo_name)
CROSS JOIN (
    SELECT id
    FROM categories
    WHERE seo_name = 'may-tinh'
      AND status = 'ACTIVE'
) AS parent
WHERE NOT EXISTS (
    SELECT 1
    FROM categories existing
    WHERE existing.seo_name = seed.seo_name
       OR existing.name = seed.name
);
