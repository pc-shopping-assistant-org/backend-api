-- =====================================================================
-- ECM Database Schema - Flyway Migration V1__init.sql
-- =====================================================================

-- ------------------------------------------------------------------
-- 0. Extensions & Helper Functions
-- ------------------------------------------------------------------
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Polyfill for uuidv7() across PostgreSQL versions (PostgreSQL 17 / 18 / 16)
CREATE OR REPLACE FUNCTION uuidv7()
RETURNS uuid AS $$
DECLARE
    unix_ts_ms bytea;
    uuid_bytes bytea;
BEGIN
    unix_ts_ms := substring(send(floor(extract(epoch FROM clock_timestamp()) * 1000)::bigint) FROM 3 FOR 6);
    uuid_bytes := unix_ts_ms || gen_random_bytes(10);
    uuid_bytes := set_byte(uuid_bytes, 6, (get_byte(uuid_bytes, 6) & 15) | 112); -- version 7: 0x70
    uuid_bytes := set_byte(uuid_bytes, 8, (get_byte(uuid_bytes, 8) & 63) | 128); -- variant: 0x80
    RETURN encode(uuid_bytes, 'hex')::uuid;
END;
$$ LANGUAGE plpgsql VOLATILE;

-- Trigger function to auto-update updated_at timestamp on UPDATE
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = current_timestamp;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;


-- =====================================================================
-- 1. ROLES - vai trò / phân quyền hệ thống
-- =====================================================================
CREATE TABLE roles (
    id          uuid            PRIMARY KEY DEFAULT uuidv7(),
    name        varchar(50)     NOT NULL UNIQUE,
    status      varchar(20)     NOT NULL DEFAULT 'ACTIVE'
                    CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED'))
);
COMMENT ON TABLE roles IS 'Vai trò/phân quyền hệ thống cho tài khoản';


-- =====================================================================
-- 2. ACCOUNTS - bảng lưu thông tin đăng nhập hệ thống
-- =====================================================================
CREATE TABLE accounts (
    id              uuid            PRIMARY KEY DEFAULT uuidv7(),
    username        varchar(50)     NOT NULL UNIQUE,
    password        varchar(255)    NOT NULL,
    role_id         uuid            REFERENCES roles(id),
    status          varchar(20)     NOT NULL DEFAULT 'ACTIVE'
                        CHECK (status IN ('ACTIVE', 'INACTIVE', 'LOCKED', 'DELETED')),
    created_at      timestamp       NOT NULL DEFAULT current_timestamp,
    updated_at      timestamp
);
COMMENT ON TABLE accounts IS 'Tài khoản đăng nhập hệ thống';

CREATE TRIGGER trg_accounts_updated_at
    BEFORE UPDATE ON accounts
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- =====================================================================
-- 3. EMPLOYEES - hồ sơ nhân viên (liên kết với accounts)
-- =====================================================================
CREATE TABLE employees (
    id          uuid            PRIMARY KEY DEFAULT uuidv7(),
    account_id  uuid            UNIQUE REFERENCES accounts(id) ON DELETE CASCADE,
    full_name   varchar(100)    NOT NULL,
    email       varchar(100)    NOT NULL UNIQUE,
    phone       varchar(20)     UNIQUE,
    gender      varchar(10),
    birthday    date,
    address     varchar(255),
    status      varchar(20)     NOT NULL DEFAULT 'ACTIVE'
                    CHECK (status IN ('ACTIVE', 'INACTIVE', 'BLOCKED', 'DELETED')),
    created_at  timestamp       NOT NULL DEFAULT current_timestamp
);
COMMENT ON TABLE employees IS 'Thông tin hồ sơ nhân viên';


-- =====================================================================
-- 4. CUSTOMERS - hồ sơ khách hàng (liên kết với accounts)
-- =====================================================================
CREATE TABLE customers (
    id          uuid            PRIMARY KEY DEFAULT uuidv7(),
    account_id  uuid            UNIQUE REFERENCES accounts(id) ON DELETE CASCADE,
    full_name   varchar(100)    NOT NULL,
    email       varchar(100)    NOT NULL UNIQUE,
    phone       varchar(20)     NOT NULL UNIQUE,
    gender      varchar(10),
    birthday    date,
    address     varchar(255),
    status      varchar(20)     NOT NULL DEFAULT 'ACTIVE'
                    CHECK (status IN ('ACTIVE', 'INACTIVE', 'BLOCKED', 'DELETED')),
    created_at  timestamp       NOT NULL DEFAULT current_timestamp
);
COMMENT ON TABLE customers IS 'Thông tin hồ sơ khách hàng';


-- =====================================================================
-- 5. BRANDS - thương hiệu sản phẩm
-- =====================================================================
CREATE TABLE brands (
    id          uuid            PRIMARY KEY DEFAULT uuidv7(),
    name        varchar(100)    NOT NULL UNIQUE,
    description text,
    logo_url    varchar(255),
    status      varchar(20)     NOT NULL DEFAULT 'ACTIVE'
                    CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED')),
    created_at  timestamp       NOT NULL DEFAULT current_timestamp
);


-- =====================================================================
-- 6. CATEGORIES - danh mục sản phẩm (cây danh mục cha-con)
-- =====================================================================
CREATE TABLE categories (
    id          uuid            PRIMARY KEY DEFAULT uuidv7(),
    name        varchar(255)    NOT NULL UNIQUE,
    seo_name    varchar(255)    NOT NULL UNIQUE,
    parent_id   uuid            REFERENCES categories(id),
    status      varchar(15)     NOT NULL DEFAULT 'ACTIVE'
                    CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED')),
    created_at  timestamp       NOT NULL DEFAULT current_timestamp
);


-- =====================================================================
-- 7. ATTRIBUTE_DEFINITIONS - từ điển metadata thông số kỹ thuật (dùng chung)
-- =====================================================================
CREATE TABLE attribute_definitions (
    id              uuid            PRIMARY KEY DEFAULT uuidv7(),
    key             varchar(100)    NOT NULL UNIQUE,
    display_name    varchar(255)    NOT NULL,
    data_type       varchar(20)     NOT NULL
                        CHECK (data_type IN ('NUMBER', 'STRING', 'ENUM', 'BOOLEAN')),
    unit            varchar(50),
    allowed_values  jsonb,
    aliases         jsonb,
    filterable      boolean         NOT NULL DEFAULT false,
    comparable      boolean         NOT NULL DEFAULT false,
    status          varchar(20)     NOT NULL DEFAULT 'ACTIVE'
                        CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED')),
    created_at      timestamp       NOT NULL DEFAULT current_timestamp
);
COMMENT ON TABLE attribute_definitions IS 'Từ điển metadata các thông số kỹ thuật (dùng chung cho mọi danh mục)';


-- =====================================================================
-- 8. CATEGORY_ATTRIBUTE_GROUPS - nhóm thông số kỹ thuật theo từng danh mục
-- =====================================================================
CREATE TABLE category_attribute_groups (
    id              uuid            PRIMARY KEY DEFAULT uuidv7(),
    category_id     uuid            NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    name            varchar(100)    NOT NULL,
    display_order   int             NOT NULL DEFAULT 0,
    status          varchar(20)     NOT NULL DEFAULT 'ACTIVE'
                        CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED')),
    created_at      timestamp       NOT NULL DEFAULT current_timestamp
);
COMMENT ON TABLE category_attribute_groups IS 'Nhóm thông số kỹ thuật gắn với từng danh mục (vd: Điện thoại -> Màn hình, Camera, Pin...)';


-- =====================================================================
-- 9. CATEGORY_ATTRIBUTES - gán thuộc tính vào nhóm của danh mục
-- =====================================================================
CREATE TABLE category_attributes (
    id                  uuid            PRIMARY KEY DEFAULT uuidv7(),
    category_group_id   uuid            NOT NULL REFERENCES category_attribute_groups(id) ON DELETE CASCADE,
    attribute_id        uuid            NOT NULL REFERENCES attribute_definitions(id) ON DELETE CASCADE,
    required            boolean         NOT NULL DEFAULT false,
    display_order       int             NOT NULL DEFAULT 0,
    status              varchar(20)     NOT NULL DEFAULT 'ACTIVE'
                            CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED')),
    created_at          timestamp       NOT NULL DEFAULT current_timestamp,
    UNIQUE (category_group_id, attribute_id)
);
COMMENT ON TABLE category_attributes IS 'Gán thuộc tính kỹ thuật vào nhóm cụ thể của danh mục';


-- =====================================================================
-- 10. SUPPLIERS - nhà cung cấp
-- =====================================================================
CREATE TABLE suppliers (
    id          uuid            PRIMARY KEY DEFAULT uuidv7(),
    name        varchar(255)    NOT NULL UNIQUE,
    email       varchar(255)    NOT NULL UNIQUE,
    phone       varchar(15)     NOT NULL,
    address     varchar(255),
    description text,
    status      varchar(15)     NOT NULL DEFAULT 'ACTIVE'
                    CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED')),
    created_at  timestamp       NOT NULL DEFAULT current_timestamp
);


-- =====================================================================
-- 11. PRODUCTS - sản phẩm gốc
-- =====================================================================
CREATE TABLE products (
    id              uuid            PRIMARY KEY DEFAULT uuidv7(),
    name            varchar(255)    NOT NULL,
    seo_name        varchar(255)    NOT NULL UNIQUE,
    brand_id        uuid            REFERENCES brands(id),
    category_id     uuid            NOT NULL REFERENCES categories(id),
    supplier_id     uuid            REFERENCES suppliers(id),
    specifications  jsonb,
    description     text,
    image_url       varchar(255),
    status          varchar(15)     NOT NULL DEFAULT 'ACTIVE'
                        CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED')),
    created_at      timestamp       NOT NULL DEFAULT current_timestamp,
    created_by      uuid            REFERENCES employees(id),
    updated_at      timestamp,
    updated_by      uuid            REFERENCES employees(id)
);

CREATE TRIGGER trg_products_updated_at
    BEFORE UPDATE ON products
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- =====================================================================
-- 12. PRODUCT_VARIANTS - biến thể cụ thể của sản phẩm
-- =====================================================================
CREATE TABLE product_variants (
    id                  uuid            PRIMARY KEY DEFAULT uuidv7(),
    product_id          uuid            NOT NULL REFERENCES products(id),
    price               int             NOT NULL,
    price_sale          int             NOT NULL,
    quantity            int             NOT NULL DEFAULT 0,
    sku                 varchar(100)    NOT NULL UNIQUE,
    model               varchar(100),
    inventory_policy    varchar(15)     NOT NULL DEFAULT 'DENY'
                            CHECK (inventory_policy IN ('DENY', 'CONTINUE', 'BACKORDER')),
    specifications      jsonb,
    description         text,
    warranty            varchar(100),
    barcode             varchar(100)    UNIQUE,
    image_url           varchar(255),
    release_at          date,
    status              varchar(15)     NOT NULL DEFAULT 'ACTIVE'
                            CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED')),
    created_at          timestamp       NOT NULL DEFAULT current_timestamp,
    created_by          uuid            NOT NULL REFERENCES employees(id),
    updated_at          timestamp,
    updated_by          uuid            REFERENCES employees(id)
);

CREATE TRIGGER trg_product_variants_updated_at
    BEFORE UPDATE ON product_variants
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- =====================================================================
-- 13. PRODUCT_IMAGES - hình ảnh của từng biến thể sản phẩm
-- =====================================================================
CREATE TABLE product_images (
    id                  uuid            PRIMARY KEY DEFAULT uuidv7(),
    name                varchar(255),
    product_variant_id  uuid            NOT NULL REFERENCES product_variants(id),
    image_url           varchar(255)    NOT NULL,
    is_main             boolean         NOT NULL DEFAULT false,
    status              varchar(15)     NOT NULL DEFAULT 'ACTIVE'
                            CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED')),
    created_at          timestamp       NOT NULL DEFAULT current_timestamp
);


-- =====================================================================
-- 14. PRODUCT_REVIEWS - đánh giá sản phẩm từ khách hàng
-- =====================================================================
CREATE TABLE product_reviews (
    id          uuid            PRIMARY KEY DEFAULT uuidv7(),
    product_id  uuid            NOT NULL REFERENCES products(id),
    customer_id uuid            NOT NULL REFERENCES customers(id),
    rating      int             NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment     text,
    status      varchar(15)     NOT NULL DEFAULT 'ACTIVE'
                    CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED')),
    created_at  timestamp       NOT NULL DEFAULT current_timestamp
);


-- =====================================================================
-- 15. OPTIONS - thuộc tính tùy chọn biến thể (màu sắc, dung lượng, RAM...)
-- =====================================================================
CREATE TABLE options (
    id          uuid            PRIMARY KEY DEFAULT uuidv7(),
    type        varchar(50)     NOT NULL,
    name        varchar(100)    NOT NULL UNIQUE,
    value       text            NOT NULL,
    status      varchar(20)     NOT NULL DEFAULT 'ACTIVE'
                    CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED')),
    created_at  timestamp       NOT NULL DEFAULT current_timestamp
);
COMMENT ON TABLE options IS 'Thuộc tính tùy chọn dùng để tạo biến thể sản phẩm (màu sắc, dung lượng, RAM...)';


-- =====================================================================
-- 16. VARIANT_OPTIONS - bảng trung gian product_variants <-> options
-- =====================================================================
CREATE TABLE variant_options (
    id                  uuid            PRIMARY KEY DEFAULT uuidv7(),
    product_variant_id  uuid            NOT NULL REFERENCES product_variants(id),
    option_id           uuid            NOT NULL REFERENCES options(id),
    status              varchar(15)     NOT NULL DEFAULT 'ACTIVE'
                            CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED')),
    created_at          timestamp       NOT NULL DEFAULT current_timestamp,
    UNIQUE (product_variant_id, option_id)
);


-- =====================================================================
-- 17. DISCOUNTS - chương trình giảm giá / mã khuyến mãi
-- =====================================================================
CREATE TABLE discounts (
    id                  uuid            PRIMARY KEY DEFAULT uuidv7(),
    code                varchar(50)     NOT NULL UNIQUE,
    title               varchar(255)    NOT NULL,
    type                varchar(10)     NOT NULL CHECK (type IN ('PERCENT', 'FIXED')),
    value               int             NOT NULL,
    start_at            timestamp       NOT NULL,
    end_at              timestamp       NOT NULL,
    scope               varchar(50)     NOT NULL,
    min_order_amount    bigint          NOT NULL DEFAULT 0,
    description         text,
    created_by          uuid            REFERENCES employees(id),
    updated_by          uuid            REFERENCES employees(id),
    created_at          timestamp       NOT NULL DEFAULT current_timestamp,
    updated_at          timestamp,
    status              varchar(20)     NOT NULL DEFAULT 'ACTIVE'
                            CHECK (status IN ('ACTIVE', 'INACTIVE', 'EXPIRED', 'DISABLED', 'DELETED'))
);

CREATE TRIGGER trg_discounts_updated_at
    BEFORE UPDATE ON discounts
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- =====================================================================
-- 18. ORDERS - đơn hàng
-- =====================================================================
CREATE TABLE orders (
    id                  uuid            PRIMARY KEY DEFAULT uuidv7(),
    customer_id         uuid            REFERENCES customers(id),
    discount_id         uuid            REFERENCES discounts(id),
    total_amount        bigint          NOT NULL,
    ship_amount         int             NOT NULL,
    discount_amount     int             NOT NULL DEFAULT 0,
    order_time          timestamp       NOT NULL DEFAULT current_timestamp,
    note                text,
    delivery_address    varchar(255),
    recipient_name      varchar(100),
    recipient_phone     varchar(15),
    delivered_at        timestamp,
    created_at          timestamp       NOT NULL DEFAULT current_timestamp,
    created_by          uuid            REFERENCES employees(id),
    updated_at          timestamp,
    updated_by          uuid            REFERENCES employees(id),
    status              varchar(20)     NOT NULL DEFAULT 'PENDING'
                            CHECK (status IN ('PENDING', 'CONFIRM', 'SHIPPING', 'COMPLETED', 'CANCELLED', 'DELETED'))
);

CREATE TRIGGER trg_orders_updated_at
    BEFORE UPDATE ON orders
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- =====================================================================
-- 19. ORDER_ITEMS - chi tiết từng dòng sản phẩm trong đơn hàng
-- =====================================================================
CREATE TABLE order_items (
    id                  uuid            PRIMARY KEY DEFAULT uuidv7(),
    order_id            uuid            NOT NULL REFERENCES orders(id),
    product_variant_id  uuid            NOT NULL REFERENCES product_variants(id),
    quantity            int             NOT NULL CHECK (quantity > 0),
    unit_amount         int             NOT NULL,
    discount_id         uuid            REFERENCES discounts(id),
    discount_amount     int             NOT NULL DEFAULT 0,
    status              varchar(20)     NOT NULL DEFAULT 'ACTIVE'
                            CHECK (status IN ('ACTIVE', 'CANCELLED', 'DELETED')),
    created_at          timestamp       NOT NULL DEFAULT current_timestamp
);


-- =====================================================================
-- 20. DISCOUNT_PRODUCT_VARIANTS - bảng trung gian discounts <-> product_variants
-- =====================================================================
CREATE TABLE discount_product_variants (
    id                  uuid            PRIMARY KEY DEFAULT uuidv7(),
    discount_id         uuid            NOT NULL REFERENCES discounts(id),
    product_variant_id  uuid            NOT NULL REFERENCES product_variants(id),
    status              varchar(20)     NOT NULL DEFAULT 'ACTIVE'
                            CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED')),
    created_at          timestamp       NOT NULL DEFAULT current_timestamp,
    UNIQUE (discount_id, product_variant_id)
);


-- =====================================================================
-- 21. PAYMENTS - thanh toán cho đơn hàng
-- =====================================================================
CREATE TABLE payments (
    id                  uuid            PRIMARY KEY DEFAULT uuidv7(),
    order_id            uuid            NOT NULL REFERENCES orders(id),
    method              varchar(100)    NOT NULL,
    paid_at             timestamp,
    transaction_code    varchar(100)    UNIQUE,
    created_at          timestamp       NOT NULL DEFAULT current_timestamp,
    created_by          uuid            REFERENCES employees(id),
    updated_at          timestamp,
    updated_by          uuid            REFERENCES employees(id),
    status              varchar(20)     NOT NULL DEFAULT 'PENDING'
                            CHECK (status IN ('PENDING', 'PAID', 'FAILED'))
);

CREATE TRIGGER trg_payments_updated_at
    BEFORE UPDATE ON payments
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- =====================================================================
-- 22. INDEXES
-- =====================================================================
CREATE INDEX idx_accounts_role_id              ON accounts(role_id);
CREATE INDEX idx_employees_account_id          ON employees(account_id);
CREATE INDEX idx_customers_account_id          ON customers(account_id);
CREATE INDEX idx_categories_parent_id          ON categories(parent_id);
CREATE INDEX idx_cat_attr_groups_cat_id        ON category_attribute_groups(category_id);
CREATE INDEX idx_category_attrs_group_id       ON category_attributes(category_group_id);
CREATE INDEX idx_category_attrs_attr_id        ON category_attributes(attribute_id);
CREATE INDEX idx_products_brand_id             ON products(brand_id);
CREATE INDEX idx_products_category_id          ON products(category_id);
CREATE INDEX idx_products_supplier_id          ON products(supplier_id);
CREATE INDEX idx_product_variants_product_id   ON product_variants(product_id);
CREATE INDEX idx_product_images_variant_id     ON product_images(product_variant_id);
CREATE INDEX idx_product_reviews_product_id    ON product_reviews(product_id);
CREATE INDEX idx_product_reviews_customer_id   ON product_reviews(customer_id);
CREATE INDEX idx_variant_options_variant_id    ON variant_options(product_variant_id);
CREATE INDEX idx_variant_options_option_id     ON variant_options(option_id);
CREATE INDEX idx_orders_customer_id            ON orders(customer_id);
CREATE INDEX idx_orders_discount_id            ON orders(discount_id);
CREATE INDEX idx_order_items_order_id          ON order_items(order_id);
CREATE INDEX idx_order_items_variant_id        ON order_items(product_variant_id);
CREATE INDEX idx_discount_pv_discount_id       ON discount_product_variants(discount_id);
CREATE INDEX idx_discount_pv_variant_id        ON discount_product_variants(product_variant_id);
CREATE INDEX idx_payments_order_id             ON payments(order_id);
