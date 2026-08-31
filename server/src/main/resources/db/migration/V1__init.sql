-- =====================================================================
-- ECM canonical database baseline
-- Source of truth: docs/02-architecture/db.dbml
-- Flyway: V1__init.sql
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- PostgreSQL 16/17 compatibility helper. PostgreSQL versions that already
-- expose uuidv7() can use the built-in function instead.
CREATE OR REPLACE FUNCTION uuidv7()
RETURNS uuid AS $$
DECLARE
    unix_ts_ms bytea;
    uuid_bytes bytea;
BEGIN
    unix_ts_ms := substring(send(floor(extract(epoch FROM clock_timestamp()) * 1000)::bigint) FROM 3 FOR 6);
    uuid_bytes := unix_ts_ms || gen_random_bytes(10);
    uuid_bytes := set_byte(uuid_bytes, 6, (get_byte(uuid_bytes, 6) & 15) | 112);
    uuid_bytes := set_byte(uuid_bytes, 8, (get_byte(uuid_bytes, 8) & 63) | 128);
    RETURN encode(uuid_bytes, 'hex')::uuid;
END;
$$ LANGUAGE plpgsql VOLATILE;

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = current_timestamp;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- =====================================================================
-- 1. Identity, files and profiles
-- =====================================================================

CREATE TABLE roles (
    id          uuid        PRIMARY KEY DEFAULT uuidv7(),
    name        varchar(50) NOT NULL UNIQUE,
    status      varchar(20) NOT NULL DEFAULT 'ACTIVE'
                CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED'))
);

CREATE TABLE files (
    id                uuid         PRIMARY KEY DEFAULT uuidv7(),
    storage_provider  varchar(30)  NOT NULL,
    storage_key       varchar(500) NOT NULL,
    original_name     varchar(255) NOT NULL,
    mime_type         varchar(100) NOT NULL,
    size_bytes        bigint       NOT NULL CHECK (size_bytes >= 0),
    public_url        varchar(2048),
    status            varchar(20)  NOT NULL DEFAULT 'ACTIVE'
                      CHECK (status IN ('ACTIVE', 'DELETED')),
    created_at        timestamp    NOT NULL DEFAULT current_timestamp,
    UNIQUE (storage_provider, storage_key)
);

CREATE TABLE accounts (
    id            uuid         PRIMARY KEY DEFAULT uuidv7(),
    email         varchar(255) NOT NULL UNIQUE,
    phone         varchar(15)  NOT NULL UNIQUE,
    password_hash varchar(255) NOT NULL,
    role_id       uuid         NOT NULL REFERENCES roles(id),
    status        varchar(20)  NOT NULL DEFAULT 'ACTIVE'
                  CHECK (status IN ('ACTIVE', 'INACTIVE', 'LOCKED', 'DELETED')),
    created_at    timestamp    NOT NULL DEFAULT current_timestamp,
    updated_at    timestamp
);

CREATE TABLE employees (
    account_id     uuid         PRIMARY KEY REFERENCES accounts(id),
    first_name     varchar(100) NOT NULL,
    last_name      varchar(100) NOT NULL,
    avatar_file_id uuid         REFERENCES files(id),
    address        varchar(255),
    gender         varchar(10)  NOT NULL CHECK (gender IN ('MALE', 'FEMALE')),
    salary         bigint       NOT NULL DEFAULT 0 CHECK (salary >= 0),
    birthday       date,
    joined_at      date         NOT NULL DEFAULT current_date,
    created_at     timestamp    NOT NULL DEFAULT current_timestamp,
    updated_at     timestamp
);

CREATE TABLE customers (
    account_id     uuid         PRIMARY KEY REFERENCES accounts(id),
    first_name     varchar(100) NOT NULL,
    last_name      varchar(100) NOT NULL,
    avatar_file_id uuid         REFERENCES files(id),
    gender         varchar(10)  CHECK (gender IN ('MALE', 'FEMALE', 'OTHER')),
    birthday       date,
    created_at     timestamp    NOT NULL DEFAULT current_timestamp,
    updated_at     timestamp
);

CREATE TABLE customer_addresses (
    id             uuid         PRIMARY KEY DEFAULT uuidv7(),
    customer_id    uuid         NOT NULL REFERENCES customers(account_id),
    recipient_name varchar(100) NOT NULL,
    phone          varchar(15)  NOT NULL,
    address_line   varchar(500) NOT NULL,
    is_default     boolean      NOT NULL DEFAULT false,
    created_at     timestamp    NOT NULL DEFAULT current_timestamp,
    updated_at     timestamp
);

-- =====================================================================
-- 2. Catalog and media
-- =====================================================================

CREATE TABLE brands (
    id            uuid         PRIMARY KEY DEFAULT uuidv7(),
    name          varchar(255) NOT NULL UNIQUE,
    seo_name      varchar(255) NOT NULL UNIQUE,
    description   text,
    image_file_id uuid         REFERENCES files(id),
    status        varchar(15)  NOT NULL DEFAULT 'ACTIVE'
                  CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED')),
    created_at    timestamp    NOT NULL DEFAULT current_timestamp
);

CREATE TABLE categories (
    id         uuid         PRIMARY KEY DEFAULT uuidv7(),
    name       varchar(255) NOT NULL UNIQUE,
    seo_name   varchar(255) NOT NULL UNIQUE,
    parent_id  uuid         REFERENCES categories(id),
    status     varchar(15)  NOT NULL DEFAULT 'ACTIVE'
               CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED')),
    created_at timestamp    NOT NULL DEFAULT current_timestamp
);

CREATE TABLE attribute_definitions (
    id             uuid         PRIMARY KEY DEFAULT uuidv7(),
    key            varchar(100) NOT NULL UNIQUE,
    display_name   varchar(255) NOT NULL,
    data_type      varchar(20)  NOT NULL
                   CHECK (data_type IN ('NUMBER', 'STRING', 'ENUM', 'BOOLEAN')),
    unit           varchar(50),
    allowed_values jsonb,
    aliases        jsonb,
    filterable     boolean      NOT NULL DEFAULT false,
    comparable     boolean      NOT NULL DEFAULT false,
    status         varchar(20)  NOT NULL DEFAULT 'ACTIVE'
                   CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED')),
    created_at     timestamp    NOT NULL DEFAULT current_timestamp
);

CREATE TABLE category_attribute_groups (
    id            uuid         PRIMARY KEY DEFAULT uuidv7(),
    category_id   uuid         NOT NULL REFERENCES categories(id),
    name          varchar(100) NOT NULL,
    display_order int          NOT NULL DEFAULT 0,
    status        varchar(20)  NOT NULL DEFAULT 'ACTIVE'
                  CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED')),
    created_at    timestamp    NOT NULL DEFAULT current_timestamp
);

CREATE TABLE category_attributes (
    id               uuid         PRIMARY KEY DEFAULT uuidv7(),
    category_group_id uuid         NOT NULL REFERENCES category_attribute_groups(id),
    attribute_id     uuid         NOT NULL REFERENCES attribute_definitions(id),
    required         boolean      NOT NULL DEFAULT false,
    display_order    int          NOT NULL DEFAULT 0,
    status           varchar(20)  NOT NULL DEFAULT 'ACTIVE'
                     CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED')),
    created_at       timestamp    NOT NULL DEFAULT current_timestamp,
    UNIQUE (category_group_id, attribute_id)
);

CREATE TABLE suppliers (
    id          uuid         PRIMARY KEY DEFAULT uuidv7(),
    name        varchar(255) NOT NULL UNIQUE,
    email       varchar(255) UNIQUE,
    phone       varchar(15),
    address     varchar(255),
    description text,
    status      varchar(15)  NOT NULL DEFAULT 'ACTIVE'
                CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED')),
    created_at  timestamp    NOT NULL DEFAULT current_timestamp,
    updated_at  timestamp
);

CREATE TABLE products (
    id             uuid         PRIMARY KEY DEFAULT uuidv7(),
    name           varchar(255) NOT NULL,
    seo_name       varchar(255) NOT NULL UNIQUE,
    brand_id       uuid         REFERENCES brands(id),
    category_id    uuid         NOT NULL REFERENCES categories(id),
    specifications jsonb,
    description    text,
    status         varchar(15)  NOT NULL DEFAULT 'ACTIVE'
                   CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED')),
    created_at     timestamp    NOT NULL DEFAULT current_timestamp,
    created_by     uuid         REFERENCES employees(account_id),
    updated_at     timestamp,
    updated_by     uuid         REFERENCES employees(account_id)
);

CREATE TABLE product_variants (
    id              uuid         PRIMARY KEY DEFAULT uuidv7(),
    product_id      uuid         NOT NULL REFERENCES products(id),
    list_price      bigint       NOT NULL CHECK (list_price >= 0),
    quantity        int          NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    sku             varchar(100) NOT NULL UNIQUE,
    model           varchar(100),
    description     text,
    warranty_months int          NOT NULL CHECK (warranty_months > 0),
    barcode         varchar(100) UNIQUE,
    release_at      date,
    status          varchar(15)  NOT NULL DEFAULT 'ACTIVE'
                    CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED')),
    created_at      timestamp    NOT NULL DEFAULT current_timestamp,
    created_by      uuid         NOT NULL REFERENCES employees(account_id),
    updated_at      timestamp,
    updated_by      uuid         REFERENCES employees(account_id)
);

CREATE TABLE product_suppliers (
    product_id  uuid      NOT NULL REFERENCES products(id),
    supplier_id uuid      NOT NULL REFERENCES suppliers(id),
    created_at  timestamp NOT NULL DEFAULT current_timestamp,
    PRIMARY KEY (product_id, supplier_id)
);

CREATE TABLE product_images (
    id                 uuid         PRIMARY KEY DEFAULT uuidv7(),
    name               varchar(255),
    product_variant_id uuid         NOT NULL REFERENCES product_variants(id),
    file_id            uuid         NOT NULL REFERENCES files(id),
    is_main            boolean      NOT NULL DEFAULT false,
    status             varchar(15)  NOT NULL DEFAULT 'ACTIVE'
                       CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED')),
    created_at         timestamp    NOT NULL DEFAULT current_timestamp,
    UNIQUE (product_variant_id, file_id)
);

CREATE TABLE options (
    id         uuid         PRIMARY KEY DEFAULT uuidv7(),
    type       varchar(50)  NOT NULL,
    name       varchar(100) NOT NULL UNIQUE,
    value      text         NOT NULL,
    status     varchar(20)  NOT NULL DEFAULT 'ACTIVE'
               CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED')),
    created_at timestamp    NOT NULL DEFAULT current_timestamp
);

CREATE TABLE variant_options (
    id                 uuid        PRIMARY KEY DEFAULT uuidv7(),
    product_variant_id uuid        NOT NULL REFERENCES product_variants(id),
    option_id          uuid        NOT NULL REFERENCES options(id),
    status             varchar(15) NOT NULL DEFAULT 'ACTIVE'
                       CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED')),
    created_at         timestamp   NOT NULL DEFAULT current_timestamp,
    UNIQUE (product_variant_id, option_id)
);

-- =====================================================================
-- 3. Pricing, shipping, order and payment
-- =====================================================================

CREATE TABLE discounts (
    id                uuid         PRIMARY KEY DEFAULT uuidv7(),
    code              varchar(50)  UNIQUE,
    title             varchar(255) NOT NULL,
    discount_type     varchar(10)  NOT NULL
                      CHECK (discount_type IN ('PERCENT', 'FIXED')),
    value             int          NOT NULL,
    application_scope varchar(20)  NOT NULL
                      CHECK (application_scope IN ('ORDER', 'ALL_ITEMS', 'CATEGORY', 'VARIANT')),
    min_order_amount  bigint       NOT NULL DEFAULT 0 CHECK (min_order_amount >= 0),
    start_at          timestamp    NOT NULL,
    end_at            timestamp    NOT NULL,
    description       text,
    created_by        uuid         REFERENCES employees(account_id),
    updated_by        uuid         REFERENCES employees(account_id),
    created_at        timestamp    NOT NULL DEFAULT current_timestamp,
    updated_at        timestamp,
    status            varchar(20)  NOT NULL DEFAULT 'ACTIVE'
                      CHECK (status IN ('ACTIVE', 'INACTIVE', 'EXPIRED', 'DISABLED', 'DELETED')),
    CHECK (start_at < end_at),
    CHECK (
        (discount_type = 'PERCENT' AND value > 0 AND value <= 100)
        OR (discount_type = 'FIXED' AND value > 0)
    )
);

CREATE TABLE discount_categories (
    discount_id uuid NOT NULL REFERENCES discounts(id),
    category_id uuid NOT NULL REFERENCES categories(id),
    PRIMARY KEY (discount_id, category_id)
);

CREATE TABLE discount_variants (
    discount_id uuid NOT NULL REFERENCES discounts(id),
    variant_id  uuid NOT NULL REFERENCES product_variants(id),
    PRIMARY KEY (discount_id, variant_id)
);

CREATE TABLE shipping_methods (
    id         uuid         PRIMARY KEY DEFAULT uuidv7(),
    code       varchar(50)  NOT NULL UNIQUE,
    name       varchar(100) NOT NULL,
    status     varchar(20)  NOT NULL DEFAULT 'ACTIVE'
               CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at timestamp    NOT NULL DEFAULT current_timestamp,
    updated_at timestamp
);

CREATE TABLE orders (
    id                 uuid         PRIMARY KEY DEFAULT uuidv7(),
    customer_id        uuid         REFERENCES customers(account_id),
    order_discount_id  uuid         REFERENCES discounts(id),
    shipping_method_id uuid         NOT NULL REFERENCES shipping_methods(id),
    subtotal_amount    bigint       NOT NULL CHECK (subtotal_amount >= 0),
    discount_amount    bigint       NOT NULL DEFAULT 0 CHECK (discount_amount >= 0),
    shipping_fee       bigint       NOT NULL CHECK (shipping_fee >= 0),
    total_amount       bigint       NOT NULL CHECK (total_amount >= 0),
    order_time         timestamp    NOT NULL DEFAULT current_timestamp,
    note               text,
    delivery_address   varchar(500) NOT NULL,
    recipient_name     varchar(100) NOT NULL,
    recipient_phone    varchar(15)  NOT NULL,
    delivered_at       timestamp,
    created_at         timestamp    NOT NULL DEFAULT current_timestamp,
    created_by         uuid         REFERENCES employees(account_id),
    updated_at         timestamp,
    updated_by         uuid         REFERENCES employees(account_id),
    status             varchar(30)  NOT NULL DEFAULT 'PENDING_CONFIRMATION'
                       CHECK (status IN (
                           'PENDING_PAYMENT', 'PENDING_CONFIRMATION', 'CONFIRMED',
                           'SHIPPING', 'COMPLETED', 'CANCELLED'
                       ))
);

CREATE TABLE order_items (
    id                 uuid         PRIMARY KEY DEFAULT uuidv7(),
    order_id           uuid         NOT NULL REFERENCES orders(id),
    product_variant_id uuid         NOT NULL REFERENCES product_variants(id),
    quantity           int          NOT NULL CHECK (quantity > 0),
    unit_price         bigint       NOT NULL CHECK (unit_price >= 0),
    item_discount_id   uuid         REFERENCES discounts(id),
    item_discount      bigint       NOT NULL DEFAULT 0 CHECK (item_discount >= 0),
    status             varchar(20)  NOT NULL DEFAULT 'ACTIVE'
                       CHECK (status IN ('ACTIVE', 'CANCELLED')),
    created_at         timestamp    NOT NULL DEFAULT current_timestamp
);

CREATE TABLE carts (
    id            uuid         PRIMARY KEY DEFAULT uuidv7(),
    customer_id   uuid         REFERENCES customers(account_id),
    session_token varchar(255),
    status        varchar(20)  NOT NULL DEFAULT 'ACTIVE'
                  CHECK (status IN ('ACTIVE', 'CONVERTED', 'ABANDONED', 'EXPIRED')),
    created_at    timestamp    NOT NULL DEFAULT current_timestamp,
    updated_at    timestamp,
    CHECK ((customer_id IS NOT NULL) <> (session_token IS NOT NULL))
);

CREATE TABLE cart_items (
    cart_id    uuid NOT NULL REFERENCES carts(id),
    variant_id uuid NOT NULL REFERENCES product_variants(id),
    quantity   int  NOT NULL CHECK (quantity > 0),
    PRIMARY KEY (cart_id, variant_id)
);

CREATE TABLE payment_methods (
    id         uuid         PRIMARY KEY DEFAULT uuidv7(),
    code       varchar(50)  NOT NULL UNIQUE,
    name       varchar(100) NOT NULL,
    status     varchar(20)  NOT NULL DEFAULT 'ACTIVE'
               CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at timestamp    NOT NULL DEFAULT current_timestamp,
    updated_at timestamp
);

CREATE TABLE payments (
    id                         uuid         PRIMARY KEY DEFAULT uuidv7(),
    order_id                   uuid         NOT NULL REFERENCES orders(id),
    payment_method_id          uuid         NOT NULL REFERENCES payment_methods(id),
    amount                     bigint       NOT NULL CHECK (amount >= 0),
    paid_at                    timestamp,
    provider_transaction_code  varchar(100) UNIQUE,
    created_at                 timestamp    NOT NULL DEFAULT current_timestamp,
    created_by                 uuid         REFERENCES employees(account_id),
    updated_at                 timestamp,
    updated_by                 uuid         REFERENCES employees(account_id),
    status                     varchar(20)  NOT NULL DEFAULT 'PENDING'
                               CHECK (status IN ('PENDING', 'PAID', 'FAILED')),
    CHECK (
        (status = 'PAID' AND paid_at IS NOT NULL)
        OR (status IN ('PENDING', 'FAILED') AND paid_at IS NULL)
    )
);

CREATE TABLE product_reviews (
    id           uuid         PRIMARY KEY DEFAULT uuidv7(),
    order_item_id uuid        NOT NULL UNIQUE REFERENCES order_items(id),
    rating       int          NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment      text,
    status       varchar(15)  NOT NULL DEFAULT 'ACTIVE'
                 CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED')),
    created_at   timestamp    NOT NULL DEFAULT current_timestamp
);

-- =====================================================================
-- 4. Updated-at triggers
-- =====================================================================

CREATE TRIGGER trg_accounts_updated_at
    BEFORE UPDATE ON accounts
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_employees_updated_at
    BEFORE UPDATE ON employees
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_customers_updated_at
    BEFORE UPDATE ON customers
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_customer_addresses_updated_at
    BEFORE UPDATE ON customer_addresses
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_suppliers_updated_at
    BEFORE UPDATE ON suppliers
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_products_updated_at
    BEFORE UPDATE ON products
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_product_variants_updated_at
    BEFORE UPDATE ON product_variants
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_discounts_updated_at
    BEFORE UPDATE ON discounts
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_shipping_methods_updated_at
    BEFORE UPDATE ON shipping_methods
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_orders_updated_at
    BEFORE UPDATE ON orders
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_carts_updated_at
    BEFORE UPDATE ON carts
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_payment_methods_updated_at
    BEFORE UPDATE ON payment_methods
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_payments_updated_at
    BEFORE UPDATE ON payments
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- =====================================================================
-- 5. Indexes and cross-table invariants
-- =====================================================================

CREATE INDEX idx_accounts_role_id             ON accounts(role_id);
CREATE INDEX idx_employees_avatar_file_id     ON employees(avatar_file_id);
CREATE INDEX idx_customers_avatar_file_id     ON customers(avatar_file_id);
CREATE INDEX idx_customer_addresses_customer   ON customer_addresses(customer_id);
CREATE INDEX idx_customer_addresses_default    ON customer_addresses(customer_id, is_default);
CREATE INDEX idx_categories_parent_id          ON categories(parent_id);
CREATE INDEX idx_cat_attr_groups_category_id   ON category_attribute_groups(category_id);
CREATE INDEX idx_category_attrs_group_id       ON category_attributes(category_group_id);
CREATE INDEX idx_category_attrs_attribute_id   ON category_attributes(attribute_id);
CREATE INDEX idx_products_brand_id             ON products(brand_id);
CREATE INDEX idx_products_category_id          ON products(category_id);
CREATE INDEX idx_products_created_by           ON products(created_by);
CREATE INDEX idx_products_updated_by           ON products(updated_by);
CREATE INDEX idx_product_variants_product_id   ON product_variants(product_id);
CREATE INDEX idx_product_variants_created_by   ON product_variants(created_by);
CREATE INDEX idx_product_variants_updated_by   ON product_variants(updated_by);
CREATE INDEX idx_product_suppliers_supplier_id ON product_suppliers(supplier_id);
CREATE INDEX idx_product_images_variant_id     ON product_images(product_variant_id);
CREATE INDEX idx_product_images_file_id        ON product_images(file_id);
CREATE INDEX idx_variant_options_variant_id    ON variant_options(product_variant_id);
CREATE INDEX idx_variant_options_option_id     ON variant_options(option_id);
CREATE INDEX idx_discount_categories_category  ON discount_categories(category_id);
CREATE INDEX idx_discount_variants_variant     ON discount_variants(variant_id);
CREATE INDEX idx_orders_customer_id            ON orders(customer_id);
CREATE INDEX idx_orders_order_discount_id      ON orders(order_discount_id);
CREATE INDEX idx_orders_shipping_method_id     ON orders(shipping_method_id);
CREATE INDEX idx_order_items_order_id          ON order_items(order_id);
CREATE INDEX idx_order_items_variant_id        ON order_items(product_variant_id);
CREATE INDEX idx_order_items_item_discount_id  ON order_items(item_discount_id);
CREATE INDEX idx_carts_customer_id             ON carts(customer_id);
CREATE INDEX idx_carts_session_token           ON carts(session_token);
CREATE INDEX idx_cart_items_variant_id         ON cart_items(variant_id);
CREATE INDEX idx_payments_order_id             ON payments(order_id);
CREATE INDEX idx_payments_payment_method_id    ON payments(payment_method_id);
CREATE INDEX idx_payments_created_by           ON payments(created_by);
CREATE INDEX idx_payments_updated_by           ON payments(updated_by);

CREATE UNIQUE INDEX ux_customer_default_address
    ON customer_addresses(customer_id)
    WHERE is_default = true;

CREATE UNIQUE INDEX ux_variant_main_image
    ON product_images(product_variant_id)
    WHERE is_main = true AND status = 'ACTIVE';

CREATE UNIQUE INDEX ux_carts_active_customer
    ON carts(customer_id)
    WHERE status = 'ACTIVE' AND customer_id IS NOT NULL;

CREATE UNIQUE INDEX ux_carts_active_session
    ON carts(session_token)
    WHERE status = 'ACTIVE' AND session_token IS NOT NULL;

CREATE UNIQUE INDEX ux_payments_one_paid_per_order
    ON payments(order_id)
    WHERE status = 'PAID';

-- A variant may have at most one non-deleted option for each option type.
CREATE OR REPLACE FUNCTION enforce_variant_option_type_unique()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM variant_options vo
        JOIN options o ON o.id = vo.option_id
        WHERE vo.status <> 'DELETED'
          AND o.status <> 'DELETED'
        GROUP BY vo.product_variant_id, upper(trim(o.type))
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION
            'A product variant cannot have multiple options of the same type';
    END IF;

    RETURN NULL;
END;
$$;

CREATE CONSTRAINT TRIGGER ct_variant_option_type_unique
AFTER INSERT OR UPDATE OR DELETE ON variant_options
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW EXECUTE FUNCTION enforce_variant_option_type_unique();

CREATE CONSTRAINT TRIGGER ct_option_type_unique_after_option_change
AFTER UPDATE ON options
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW EXECUTE FUNCTION enforce_variant_option_type_unique();

-- Scope/target is a cross-table invariant and is checked at transaction commit.
CREATE OR REPLACE FUNCTION enforce_discount_scope_targets()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    invalid_discount_id uuid;
BEGIN
    SELECT d.id
    INTO invalid_discount_id
    FROM discounts d
    WHERE
        (
            d.application_scope IN ('ORDER', 'ALL_ITEMS')
            AND (
                EXISTS (SELECT 1 FROM discount_categories dc WHERE dc.discount_id = d.id)
                OR EXISTS (SELECT 1 FROM discount_variants dv WHERE dv.discount_id = d.id)
            )
        )
        OR (
            d.application_scope = 'CATEGORY'
            AND (
                NOT EXISTS (SELECT 1 FROM discount_categories dc WHERE dc.discount_id = d.id)
                OR EXISTS (SELECT 1 FROM discount_variants dv WHERE dv.discount_id = d.id)
            )
        )
        OR (
            d.application_scope = 'VARIANT'
            AND (
                EXISTS (SELECT 1 FROM discount_categories dc WHERE dc.discount_id = d.id)
                OR NOT EXISTS (SELECT 1 FROM discount_variants dv WHERE dv.discount_id = d.id)
            )
        )
    LIMIT 1;

    IF invalid_discount_id IS NOT NULL THEN
        RAISE EXCEPTION
            'Discount % has targets inconsistent with application_scope',
            invalid_discount_id;
    END IF;

    RETURN NULL;
END;
$$;

CREATE CONSTRAINT TRIGGER ct_discount_scope_targets
AFTER INSERT OR UPDATE ON discounts
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW EXECUTE FUNCTION enforce_discount_scope_targets();

CREATE CONSTRAINT TRIGGER ct_discount_category_targets
AFTER INSERT OR UPDATE OR DELETE ON discount_categories
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW EXECUTE FUNCTION enforce_discount_scope_targets();

CREATE CONSTRAINT TRIGGER ct_discount_variant_targets
AFTER INSERT OR UPDATE OR DELETE ON discount_variants
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW EXECUTE FUNCTION enforce_discount_scope_targets();
