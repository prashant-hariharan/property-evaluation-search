--liquibase formatted sql

--changeset prashant:001-create-properties
CREATE TABLE properties (
    id BINARY(16) NOT NULL,
    title VARCHAR(255) NOT NULL,
    address VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    postal_code VARCHAR(20) NOT NULL,
    area_in_square_meter DECIMAL(10,2) NOT NULL,
    construction_year INT NOT NULL,
    property_type VARCHAR(50) NOT NULL,
    description TEXT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    modified_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_properties PRIMARY KEY (id),
    CONSTRAINT chk_property_title_not_blank CHECK (CHAR_LENGTH(TRIM(title)) > 0),
    CONSTRAINT chk_property_area_positive CHECK (area_in_square_meter > 0),
    CONSTRAINT chk_property_construction_year_range CHECK (construction_year BETWEEN 1800 AND 2100),
    CONSTRAINT chk_property_description_length CHECK (description IS NULL OR CHAR_LENGTH(description) <= 2000)
);

--changeset prashant:002-create-property-evaluations
CREATE TABLE property_evaluations (
    id BINARY(16) NOT NULL,
    property_id BINARY(16) NOT NULL,
    market_value DECIMAL(15,2) NOT NULL,
    notes TEXT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    modified_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_property_evaluations PRIMARY KEY (id),
    CONSTRAINT fk_property_evaluations_property FOREIGN KEY (property_id) REFERENCES properties(id),
    CONSTRAINT chk_property_market_value_non_negative CHECK (market_value >= 0),
    CONSTRAINT chk_property_notes_length CHECK (notes IS NULL OR CHAR_LENGTH(notes) <= 5000)
);
