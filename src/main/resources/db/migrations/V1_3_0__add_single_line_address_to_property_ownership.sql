ALTER TABLE property_ownership
    ADD single_line_address VARCHAR(1000) DEFAULT '' NOT NULL;

UPDATE property_ownership po SET single_line_address = (
    SELECT a.single_line_address
    FROM address a
    JOIN property p ON a.id = p.address_id
    WHERE p.id = po.property_id
);

CREATE FUNCTION update_property_ownership_single_line_address()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    propertyOwnershipId BIGINT;
    propertyId BIGINT;
    addressId BIGINT;
BEGIN
    CASE
        WHEN TG_TABLE_NAME = 'property_ownership' THEN
            propertyOwnershipId := NEW.id;
            UPDATE property_ownership po SET single_line_address = (
                SELECT a.single_line_address
                FROM address a
                JOIN property p ON a.id = p.address_id
                WHERE p.id = po.property_id
            )
            WHERE po.id = propertyOwnershipId;

        WHEN TG_TABLE_NAME = 'property' THEN
            propertyId := NEW.id;
            UPDATE property_ownership po SET single_line_address = (
                SELECT a.single_line_address
                FROM address a
                JOIN property p ON a.id = p.address_id
                WHERE p.id = property_id
            )
            WHERE po.property_id = propertyId;

        WHEN TG_TABLE_NAME = 'address' THEN
            addressId := NEW.id;
            UPDATE property_ownership po SET single_line_address = (
                SELECT a.single_line_address
                FROM address a
                WHERE a.id = addressId
            )
            WHERE po.property_id = (SELECT p.id FROM property p WHERE p.address_id = addressId);
    END CASE;
    RETURN NULL;
END;
$$;

CREATE TRIGGER insert_property_ownership_single_line_address
AFTER INSERT ON property_ownership
FOR EACH ROW
EXECUTE FUNCTION update_property_ownership_single_line_address();

CREATE TRIGGER update_property_ownership_single_line_address
AFTER UPDATE OF property_id ON property_ownership
FOR EACH ROW
WHEN (OLD.property_id IS DISTINCT FROM NEW.property_id)
EXECUTE FUNCTION update_property_ownership_single_line_address();

CREATE TRIGGER update_property_ownership_single_line_address
AFTER UPDATE OF address_id ON property
FOR EACH ROW
WHEN (OLD.address_id IS DISTINCT FROM NEW.address_id)
EXECUTE FUNCTION update_property_ownership_single_line_address();

CREATE TRIGGER update_property_ownership_single_line_address
AFTER UPDATE OF single_line_address ON address
FOR EACH ROW
WHEN (OLD.single_line_address IS DISTINCT FROM NEW.single_line_address)
EXECUTE FUNCTION update_property_ownership_single_line_address();

CREATE INDEX property_ownership_single_line_address_idx ON property_ownership USING gist (single_line_address gist_trgm_ops(siglen=2024)) WHERE is_active;
