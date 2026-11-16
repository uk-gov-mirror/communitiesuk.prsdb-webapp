WITH owner_pool AS MATERIALIZED (
    SELECT id FROM landlord
    WHERE individual_subject_identifier LIKE 'urn:fdc:gov.uk:2022:%'
    ORDER BY individual_subject_identifier
    OFFSET 1400 LIMIT 200
), eligible AS (
    SELECT DISTINCT ON (owner.individual_subject_identifier)
           owner.individual_subject_identifier AS owner_subject,
           property.id AS property_id,
           address.single_line_address AS property_address,
           access.token,
           access.invited_email,
           access.encoded_password IS NOT NULL AS has_password
    FROM letting_agent_access access
    JOIN property_ownership property ON property.id = access.property_ownership_id
    JOIN address ON address.id = property.address_id
    JOIN ownership_link owner_link ON owner_link.landlordship_id = property.id
    JOIN landlord owner ON owner.id = owner_link.landlord_id
    JOIN owner_pool ON owner_pool.id = owner.id
    WHERE property.is_active
      AND property.is_occupied
      AND access.encoded_password IS NULL
      AND access.created_date >= current_timestamp - interval '1 day'
      AND access.invited_email = 'agent-access-' || property.id || '@example.test'
      AND owner.is_active
      AND owner.individual_has_accepted_privacy_notice
      AND owner.individual_subject_identifier LIKE 'urn:fdc:gov.uk:2022:%'
      AND NOT EXISTS (
          SELECT 1
          FROM landlord_incomplete_properties incomplete
          WHERE incomplete.user_id = owner.individual_subject_identifier
      )
    ORDER BY owner.individual_subject_identifier, property.id
    LIMIT 101
), fixture_compliance AS MATERIALIZED (
    SELECT eligible.property_id, compliance.id AS compliance_id
    FROM eligible
    JOIN property_compliance compliance ON compliance.property_ownership_id = eligible.property_id
), fixture_file_rows AS MATERIALIZED (
    SELECT fixture_compliance.property_id, upload.object_key AS "objectKey", upload.extension
    FROM fixture_compliance
    JOIN gas_safety_uploads gas ON gas.property_compliance_id = fixture_compliance.compliance_id
    JOIN file_upload upload ON upload.id = gas.gas_safety_file_uploads_id
    UNION
    SELECT fixture_compliance.property_id, upload.object_key AS "objectKey", upload.extension
    FROM fixture_compliance
    JOIN electrical_safety_uploads electrical ON electrical.property_compliance_id = fixture_compliance.compliance_id
    JOIN file_upload upload ON upload.id = electrical.electrical_safety_file_uploads_id
), fixture_files AS (
    SELECT property_id, json_agg(json_build_object('objectKey', "objectKey", 'extension', extension) ORDER BY "objectKey") AS files
    FROM fixture_file_rows
    GROUP BY property_id
)
SELECT COALESCE(
    json_agg(
        json_build_object(
            'ownerSubject', owner_subject,
            'propertyOwnershipId', eligible.property_id,
            'files', COALESCE(fixture_files.files, '[]'::json),
            'address', property_address,
            'token', token,
            'invitedEmail', invited_email,
            'hasPassword', has_password
        )
        ORDER BY owner_subject
    ),
    '[]'::json
)
FROM eligible
LEFT JOIN fixture_files ON fixture_files.property_id = eligible.property_id;
