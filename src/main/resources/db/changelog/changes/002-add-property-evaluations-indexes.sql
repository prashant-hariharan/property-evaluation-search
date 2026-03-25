--liquibase formatted sql

--changeset prashant:003-indexes-properties-and-evaluations
CREATE INDEX idx_properties_city ON properties (city);
CREATE INDEX idx_properties_postal_code ON properties (postal_code);
CREATE INDEX idx_properties_property_type ON properties (property_type);
CREATE INDEX idx_properties_construction_year ON properties (construction_year);
CREATE INDEX idx_properties_area_in_square_meter ON properties (area_in_square_meter);
CREATE INDEX idx_properties_city_type ON properties (city, property_type);
CREATE INDEX idx_property_evaluations_property_id ON property_evaluations (property_id);
CREATE INDEX idx_property_evaluations_market_value ON property_evaluations (market_value);
CREATE INDEX idx_property_eval_property_value ON property_evaluations (property_id, market_value);
