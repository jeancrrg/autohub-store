-- User Service — schema inicial
-- V1: tabelas de usuários e endereços

CREATE TABLE users (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) NOT NULL UNIQUE,
    full_name     VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    role          VARCHAR(20)  NOT NULL DEFAULT 'USER',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE addresses (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    street     VARCHAR(255) NOT NULL,
    number     VARCHAR(20)  NOT NULL,
    complement VARCHAR(100),
    city       VARCHAR(100) NOT NULL,
    state      VARCHAR(2)   NOT NULL,
    zip_code   VARCHAR(9)   NOT NULL,
    is_default BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email         ON users (email);
CREATE INDEX idx_addresses_user_id   ON addresses (user_id);
