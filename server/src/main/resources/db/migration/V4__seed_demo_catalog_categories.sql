-- =====================================================================
-- Demo catalog taxonomy
-- =====================================================================
-- These rows are deliberately small local/demo fixtures so the storefront
-- can exercise a realistic ecommerce category menu. They are normal category
-- records and remain managed through the public/admin category APIs later.

-- V1 used send(bigint), which is not available on PostgreSQL 16. Replace the
-- helper before relying on category UUID defaults so a fresh/local migration
-- can run on the supported PostgreSQL version.
CREATE OR REPLACE FUNCTION uuidv7()
RETURNS uuid AS $$
DECLARE
    unix_ts_ms bytea;
    uuid_bytes bytea;
BEGIN
    unix_ts_ms := substring(int8send(floor(extract(epoch FROM clock_timestamp()) * 1000)::bigint) FROM 3 FOR 6);
    uuid_bytes := unix_ts_ms || gen_random_bytes(10);
    uuid_bytes := set_byte(uuid_bytes, 6, (get_byte(uuid_bytes, 6) & 15) | 112);
    uuid_bytes := set_byte(uuid_bytes, 8, (get_byte(uuid_bytes, 8) & 63) | 128);
    RETURN encode(uuid_bytes, 'hex')::uuid;
END;
$$ LANGUAGE plpgsql VOLATILE;

INSERT INTO categories (name, seo_name, parent_id, status)
VALUES ('Máy tính', 'may-tinh', NULL, 'ACTIVE')
ON CONFLICT (seo_name) DO NOTHING;

WITH seed(name, seo_name) AS (
    VALUES
        ('Laptop Gaming', 'laptop-gaming'),
        ('PC GVN', 'pc-gvn'),
        ('Main, CPU, VGA', 'main-cpu-vga'),
        ('Case, Nguồn, Tản', 'case-nguon-tan'),
        ('Ổ cứng, RAM, Thẻ nhớ', 'o-cung-ram-the-nho'),
        ('Loa, Micro, Webcam', 'loa-micro-webcam'),
        ('Màn hình', 'man-hinh'),
        ('Bàn phím', 'ban-phim'),
        ('Chuột + Lót chuột', 'chuot-lot-chuot'),
        ('Tai Nghe', 'tai-nghe'),
        ('Ghế - Bàn', 'ghe-ban'),
        ('Phần mềm, mạng', 'phan-mem-mang'),
        ('Phụ kiện - Console', 'phu-kien-console')
)
INSERT INTO categories (name, seo_name, parent_id, status)
SELECT seed.name,
       seed.seo_name,
       parent.id,
       'ACTIVE'
FROM seed
CROSS JOIN (
    SELECT id
    FROM categories
    WHERE seo_name = 'may-tinh'
) parent
WHERE NOT EXISTS (
    SELECT 1
    FROM categories existing
    WHERE existing.seo_name = seed.seo_name
       OR existing.name = seed.name
);
