-- Art Bazaar core schema.
-- Enum-like columns are VARCHAR (mapped with @Enumerated(STRING)) so new values never need an ALTER.

CREATE TABLE users (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP(6) NOT NULL,
    updated_at    TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE artist_profiles (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    bio         TEXT,
    city        VARCHAR(100),
    avatar_url  VARCHAR(500),
    approved    BOOLEAN      NOT NULL DEFAULT FALSE,
    approved_at TIMESTAMP(6) NULL,
    created_at  TIMESTAMP(6) NOT NULL,
    updated_at  TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_artist_profiles_user UNIQUE (user_id),
    CONSTRAINT fk_artist_profiles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE categories (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    name       VARCHAR(60)  NOT NULL,
    slug       VARCHAR(60)  NOT NULL,
    icon_url   VARCHAR(500),
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_categories_name UNIQUE (name),
    CONSTRAINT uk_categories_slug UNIQUE (slug)
);

CREATE TABLE artworks (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    title       VARCHAR(150)  NOT NULL,
    description TEXT,
    price       DECIMAL(10,2) NOT NULL,
    stock       INT           NOT NULL DEFAULT 1,
    image_url   VARCHAR(500)  NOT NULL,
    status      VARCHAR(20)   NOT NULL,
    artist_id   BIGINT        NOT NULL,
    category_id BIGINT        NOT NULL,
    version     BIGINT        NOT NULL DEFAULT 0,
    created_at  TIMESTAMP(6)  NOT NULL,
    updated_at  TIMESTAMP(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_artworks_artist   FOREIGN KEY (artist_id)   REFERENCES users (id),
    CONSTRAINT fk_artworks_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT chk_artworks_price CHECK (price >= 0),
    CONSTRAINT chk_artworks_stock CHECK (stock >= 0)
);
CREATE INDEX idx_artworks_status_category ON artworks (status, category_id);
CREATE INDEX idx_artworks_artist ON artworks (artist_id);

CREATE TABLE cart_items (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    user_id    BIGINT       NOT NULL,
    artwork_id BIGINT       NOT NULL,
    quantity   INT          NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_cart_items_user_artwork UNIQUE (user_id, artwork_id),
    CONSTRAINT fk_cart_items_user    FOREIGN KEY (user_id)    REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_items_artwork FOREIGN KEY (artwork_id) REFERENCES artworks (id) ON DELETE CASCADE,
    CONSTRAINT chk_cart_items_quantity CHECK (quantity > 0)
);

CREATE TABLE orders (
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    buyer_id          BIGINT        NOT NULL,
    total_amount      DECIMAL(12,2) NOT NULL,
    status            VARCHAR(20)   NOT NULL,
    payment_status    VARCHAR(20)   NOT NULL,
    payment_reference VARCHAR(100),
    shipping_name     VARCHAR(100)  NOT NULL,
    shipping_phone    VARCHAR(20)   NOT NULL,
    shipping_address  VARCHAR(500)  NOT NULL,
    shipping_city     VARCHAR(100)  NOT NULL,
    shipping_state    VARCHAR(100)  NOT NULL,
    shipping_pincode  VARCHAR(10)   NOT NULL,
    created_at        TIMESTAMP(6)  NOT NULL,
    updated_at        TIMESTAMP(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_orders_buyer FOREIGN KEY (buyer_id) REFERENCES users (id)
);
CREATE INDEX idx_orders_buyer ON orders (buyer_id, created_at);

-- Each line keeps the artist and a snapshot of title/price, so one order can span several
-- artists and later edits to an artwork never rewrite order history.
CREATE TABLE order_items (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    order_id      BIGINT        NOT NULL,
    artwork_id    BIGINT        NULL,
    artist_id     BIGINT        NOT NULL,
    artwork_title VARCHAR(150)  NOT NULL,
    image_url     VARCHAR(500)  NOT NULL,
    unit_price    DECIMAL(10,2) NOT NULL,
    quantity      INT           NOT NULL,
    status        VARCHAR(20)   NOT NULL,
    created_at    TIMESTAMP(6)  NOT NULL,
    updated_at    TIMESTAMP(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_order_items_order   FOREIGN KEY (order_id)   REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_artwork FOREIGN KEY (artwork_id) REFERENCES artworks (id) ON DELETE SET NULL,
    CONSTRAINT fk_order_items_artist  FOREIGN KEY (artist_id)  REFERENCES users (id),
    CONSTRAINT chk_order_items_quantity CHECK (quantity > 0)
);
CREATE INDEX idx_order_items_artist ON order_items (artist_id, status);

CREATE TABLE reviews (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    artwork_id BIGINT       NOT NULL,
    buyer_id   BIGINT       NOT NULL,
    rating     INT          NOT NULL,
    comment    VARCHAR(1000),
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_reviews_artwork_buyer UNIQUE (artwork_id, buyer_id),
    CONSTRAINT fk_reviews_artwork FOREIGN KEY (artwork_id) REFERENCES artworks (id) ON DELETE CASCADE,
    CONSTRAINT fk_reviews_buyer   FOREIGN KEY (buyer_id)   REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_reviews_rating CHECK (rating BETWEEN 1 AND 5)
);
