-- V2__profile_images.sql
-- Foto de perfil (avatar) y banner del usuario, guardados en Postgres (BYTEA) en vez de S3.
-- Una fila por (username, image_type): subir una nueva imagen reemplaza (UPDATE) la anterior.

CREATE TABLE profile_images (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(120) NOT NULL,
    image_type    VARCHAR(10)  NOT NULL,
    content_type  VARCHAR(100) NOT NULL,
    image_data    BYTEA        NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uk_profile_images_username_type UNIQUE (username, image_type),
    CONSTRAINT chk_profile_images_type CHECK (image_type IN ('AVATAR', 'BANNER'))
);

CREATE INDEX idx_profile_images_username ON profile_images (username);
