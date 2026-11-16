WITH fixtures AS (
    SELECT DISTINCT ON (l.individual_subject_identifier)
           l.individual_subject_identifier AS subject,
           p.id AS "propertyOwnershipId",
           a.single_line_address AS address
    FROM (
        SELECT * FROM landlord
        WHERE individual_subject_identifier LIKE 'urn:fdc:gov.uk:2022:%'
        ORDER BY individual_subject_identifier LIMIT 1000
    ) l
    JOIN ownership_link ol ON ol.landlord_id = l.id
    JOIN property_ownership p ON p.id = ol.landlordship_id
    JOIN address a ON a.id = p.address_id
    JOIN property_compliance pc ON pc.property_ownership_id = p.id
    WHERE l.is_active AND l.individual_has_accepted_privacy_notice
      AND l.individual_subject_identifier LIKE 'urn:fdc:gov.uk:2022:%'
      AND p.id > 26 AND p.is_active AND p.is_occupied
      AND p.tenancy_provide_later IS NOT TRUE
      AND NOT EXISTS (
          SELECT 1 FROM ownership_link other
          WHERE other.landlordship_id = p.id AND other.landlord_id <> l.id
      )
      AND NOT EXISTS (
          SELECT 1 FROM letting_agent_access la WHERE la.property_ownership_id = p.id
      )
      AND NOT EXISTS (
          SELECT 1 FROM landlord_incomplete_properties lip
          WHERE lip.user_id = l.individual_subject_identifier
      )
    ORDER BY l.individual_subject_identifier, p.id
    LIMIT 121
), fixture_files AS MATERIALIZED (
    SELECT pc.property_ownership_id, f.object_key AS "objectKey", f.extension
    FROM fixtures
    JOIN property_compliance pc ON pc.property_ownership_id = fixtures."propertyOwnershipId"
    JOIN gas_safety_uploads g ON g.property_compliance_id = pc.id
    JOIN file_upload f ON f.id = g.gas_safety_file_uploads_id
    UNION
    SELECT pc.property_ownership_id, f.object_key AS "objectKey", f.extension
    FROM fixtures
    JOIN property_compliance pc ON pc.property_ownership_id = fixtures."propertyOwnershipId"
    JOIN electrical_safety_uploads e ON e.property_compliance_id = pc.id
    JOIN file_upload f ON f.id = e.electrical_safety_file_uploads_id
), with_files AS (
    SELECT fixtures.*,
           (
               SELECT COALESCE(json_agg(objects ORDER BY "objectKey"), '[]'::json)
               FROM (
                   SELECT "objectKey", extension FROM fixture_files
                   WHERE property_ownership_id = fixtures."propertyOwnershipId"
               ) objects
           ) AS files
    FROM fixtures
)
SELECT COALESCE(json_agg(with_files ORDER BY subject), '[]'::json) FROM with_files;
