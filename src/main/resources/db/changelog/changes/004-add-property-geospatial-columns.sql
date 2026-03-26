--liquibase formatted sql

--changeset prashant:005-add-property-geospatial-columns
ALTER TABLE properties
    ADD COLUMN latitude DECIMAL(9,6) NULL,
    ADD COLUMN longitude DECIMAL(9,6) NULL,
    ADD CONSTRAINT chk_property_latitude_range CHECK (latitude IS NULL OR latitude BETWEEN -90 AND 90),
    ADD CONSTRAINT chk_property_longitude_range CHECK (longitude IS NULL OR longitude BETWEEN -180 AND 180),
    ADD CONSTRAINT chk_property_geo_pair CHECK (
        (latitude IS NULL AND longitude IS NULL)
        OR (latitude IS NOT NULL AND longitude IS NOT NULL)
    );

CREATE INDEX idx_properties_latitude_longitude ON properties (latitude, longitude);
