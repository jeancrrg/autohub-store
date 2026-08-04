-- V2: adiciona sku (identidade de negócio) e slug (URL amigável) ao produto,
-- separados do id técnico (UUID). Ver docs/planning/specs/03-catalog-service.md.

ALTER TABLE products ADD COLUMN sku VARCHAR(50);
ALTER TABLE products ADD COLUMN slug VARCHAR(255);

UPDATE products
SET sku  = 'SKU-' || UPPER(SUBSTRING(id::text, 1, 8)),
    slug = 'produto-' || SUBSTRING(id::text, 1, 8)
WHERE sku IS NULL;

ALTER TABLE products ALTER COLUMN sku SET NOT NULL;
ALTER TABLE products ALTER COLUMN slug SET NOT NULL;
ALTER TABLE products ADD CONSTRAINT uq_products_sku UNIQUE (sku);
ALTER TABLE products ADD CONSTRAINT uq_products_slug UNIQUE (slug);

CREATE INDEX idx_products_sku ON products(sku);
CREATE INDEX idx_products_slug ON products(slug);
