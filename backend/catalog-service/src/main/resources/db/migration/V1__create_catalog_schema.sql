CREATE TABLE brands (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(100) NOT NULL,
    slug       VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_brands_slug ON brands(slug);


CREATE TABLE categories (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(100) NOT NULL,
    slug       VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_categories_slug ON categories(slug);


CREATE TABLE products (
    id             UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    name           VARCHAR(255)   NOT NULL,
    sku            VARCHAR(50)    NOT NULL UNIQUE,
    slug           VARCHAR(255)   NOT NULL UNIQUE,
    description    TEXT,
    price          NUMERIC(10,2)  NOT NULL CHECK (price >= 0),
    brand_id       UUID NOT NULL REFERENCES brands(id),
    category_id    UUID           NOT NULL REFERENCES categories(id),
    status         VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_status   ON products(status);
CREATE INDEX idx_products_sku      ON products(sku);
CREATE INDEX idx_products_slug     ON products(slug);


CREATE TABLE product_images (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID         NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    url        VARCHAR(1024) NOT NULL,
    is_primary BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_product_images_product_id ON product_images(product_id);
