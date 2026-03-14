CREATE TABLE variant_types (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    name           VARCHAR(100) NOT NULL,
    description    VARCHAR(500) NULL,
    product_type_id BIGINT NULL COMMENT 'NULL = applies to all product types',
    sort_order     INT NOT NULL DEFAULT 0,
    created_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted        TINYINT(1)  NOT NULL DEFAULT 0,
    deleted_at     DATETIME(6) NULL
);

CREATE TABLE variant_type_options (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    variant_type_id BIGINT      NOT NULL,
    value           VARCHAR(100) NOT NULL,
    sort_order      INT NOT NULL DEFAULT 0,
    created_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted         TINYINT(1)  NOT NULL DEFAULT 0,
    deleted_at      DATETIME(6) NULL,
    FOREIGN KEY (variant_type_id) REFERENCES variant_types(id)
);
