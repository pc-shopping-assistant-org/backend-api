-- Keep the base catalog complete on a fresh database as well as on local
-- databases that already contained the legacy Laptop category.
INSERT INTO categories (name, seo_name, parent_id, status)
SELECT 'Laptop', 'laptop', parent.id, 'ACTIVE'
FROM categories parent
WHERE parent.seo_name = 'may-tinh'
  AND NOT EXISTS (
      SELECT 1
      FROM categories existing
      WHERE existing.seo_name = 'laptop'
         OR existing.name = 'Laptop'
  );
