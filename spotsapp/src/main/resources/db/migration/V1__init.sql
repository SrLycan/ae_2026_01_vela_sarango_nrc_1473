-- V1__init.sql
-- Schema inicial de Spots App: categories, spots, media, reviews, follows

-- ==========================================================================
-- categories
-- ==========================================================================
CREATE TABLE categories (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(60)  NOT NULL,
    description VARCHAR(255),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uk_categories_name UNIQUE (name)
);

-- ==========================================================================
-- spots
-- ==========================================================================
CREATE TABLE spots (
    id               BIGSERIAL PRIMARY KEY,
    name             VARCHAR(120)  NOT NULL,
    description      VARCHAR(1000) NOT NULL,
    latitude         DOUBLE PRECISION NOT NULL,
    longitude        DOUBLE PRECISION NOT NULL,
    address          VARCHAR(255)  NOT NULL,
    category_id      BIGINT        NOT NULL,
    owner_username   VARCHAR(120)  NOT NULL,
    status           VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    rarity           VARCHAR(20),
    points_reward    INTEGER,
    rejection_reason VARCHAR(500),
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT fk_spots_category FOREIGN KEY (category_id)
        REFERENCES categories (id) ON DELETE RESTRICT,

    CONSTRAINT chk_spots_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    CONSTRAINT chk_spots_rarity CHECK (rarity IS NULL OR rarity IN ('COMMON', 'RARE', 'EPIC', 'LEGENDARY')),
    CONSTRAINT chk_spots_points_reward_non_negative CHECK (points_reward IS NULL OR points_reward >= 0)
);

CREATE INDEX idx_spots_status ON spots (status);
CREATE INDEX idx_spots_owner_username ON spots (owner_username);
CREATE INDEX idx_spots_category_id ON spots (category_id);

-- ==========================================================================
-- media
-- ==========================================================================
CREATE TABLE media (
    id                    BIGSERIAL PRIMARY KEY,
    spot_id               BIGINT        NOT NULL,
    url                   VARCHAR(500)  NOT NULL,
    type                  VARCHAR(10)   NOT NULL,
    uploaded_by_username  VARCHAR(120)  NOT NULL,
    created_at            TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT fk_media_spot FOREIGN KEY (spot_id)
        REFERENCES spots (id) ON DELETE CASCADE,

    CONSTRAINT chk_media_type CHECK (type IN ('IMAGE', 'VIDEO'))
);

CREATE INDEX idx_media_spot_id ON media (spot_id);

-- ==========================================================================
-- reviews
-- ==========================================================================
CREATE TABLE reviews (
    id          BIGSERIAL PRIMARY KEY,
    spot_id     BIGINT        NOT NULL,
    username    VARCHAR(120)  NOT NULL,
    rating      INTEGER       NOT NULL,
    comment     VARCHAR(1000),
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT fk_reviews_spot FOREIGN KEY (spot_id)
        REFERENCES spots (id) ON DELETE CASCADE,

    -- Una única reseña por usuario y spot
    CONSTRAINT uk_reviews_spot_username UNIQUE (spot_id, username),

    CONSTRAINT chk_reviews_rating_range CHECK (rating BETWEEN 1 AND 5)
);

CREATE INDEX idx_reviews_spot_id ON reviews (spot_id);

-- ==========================================================================
-- follows
-- ==========================================================================
CREATE TABLE follows (
    id                  BIGSERIAL PRIMARY KEY,
    follower_username   VARCHAR(120) NOT NULL,
    following_username  VARCHAR(120) NOT NULL,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),

    -- Un usuario no puede seguir dos veces al mismo usuario
    CONSTRAINT uk_follows_follower_following UNIQUE (follower_username, following_username),

    -- Un usuario no puede seguirse a sí mismo (reforzado también en FollowService)
    CONSTRAINT chk_follows_no_self_follow CHECK (follower_username <> following_username)
);

CREATE INDEX idx_follows_follower ON follows (follower_username);
CREATE INDEX idx_follows_following ON follows (following_username);
