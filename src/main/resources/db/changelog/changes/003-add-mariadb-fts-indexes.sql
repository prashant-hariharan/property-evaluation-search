--liquibase formatted sql

--changeset prashant:004-add-fulltext-indexes-properties
CREATE FULLTEXT INDEX ftx_properties_title_description
    ON properties (title, description);
