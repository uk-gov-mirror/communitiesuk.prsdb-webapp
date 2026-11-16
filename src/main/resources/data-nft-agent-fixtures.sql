WITH landlord_pool AS MATERIALIZED (
    SELECT id, individual_subject_identifier, is_active, individual_has_accepted_privacy_notice
    FROM landlord
    WHERE individual_subject_identifier LIKE 'urn:fdc:gov.uk:2022:%'
    ORDER BY individual_subject_identifier
    OFFSET 1400 LIMIT 200
), app_created_properties AS MATERIALIZED (
    -- Properties created by app journeys after the base load, or holding non-seed uploads, belong to other families.
    SELECT created.id AS property_ownership_id
    FROM property_ownership created
    WHERE created.created_date > (SELECT max(base.created_date) FROM property_ownership base WHERE base.id <= 26)
    UNION
    SELECT seeded.property_ownership_id
    FROM file_upload upload
    JOIN gas_safety_uploads gas ON gas.gas_safety_file_uploads_id = upload.id
    JOIN property_compliance seeded ON seeded.id = gas.property_compliance_id
    WHERE upload.object_key !~ '^certificateUpload\.seed-[0-9]+\.[a-z-]+\.member-[0-9]+$'
    UNION
    SELECT seeded.property_ownership_id
    FROM file_upload upload
    JOIN electrical_safety_uploads electrical ON electrical.electrical_safety_file_uploads_id = upload.id
    JOIN property_compliance seeded ON seeded.id = electrical.property_compliance_id
    WHERE upload.object_key !~ '^certificateUpload\.seed-[0-9]+\.[a-z-]+\.member-[0-9]+$'
), app_touched_landlords AS MATERIALIZED (
    SELECT DISTINCT ol.landlord_id
    FROM ownership_link ol
    JOIN app_created_properties created ON created.property_ownership_id = ol.landlordship_id
), candidates AS MATERIALIZED (
    SELECT l.individual_subject_identifier AS subject, p.id AS property_id
    FROM landlord_pool l
    JOIN ownership_link ol ON ol.landlord_id = l.id
    JOIN property_ownership p ON p.id = ol.landlordship_id
    WHERE l.is_active AND l.individual_has_accepted_privacy_notice
      AND p.id > 26 AND p.is_active AND p.is_occupied
      AND p.marked_joint_landlord IS NOT TRUE
      AND NOT EXISTS (SELECT 1 FROM app_touched_landlords touched WHERE touched.landlord_id = l.id)
    ORDER BY l.individual_subject_identifier, p.id
    LIMIT 4096
), candidate_ids AS MATERIALIZED (
    SELECT DISTINCT property_id FROM candidates
), owner_counts AS MATERIALIZED (
    SELECT ol.landlordship_id, count(*) AS owner_count
    FROM ownership_link ol
    JOIN candidate_ids candidate ON candidate.property_id = ol.landlordship_id
    GROUP BY ol.landlordship_id
), eligible AS (
    SELECT DISTINCT ON (l.individual_subject_identifier)
           l.individual_subject_identifier AS subject,
           p.id AS property_id,
           a.single_line_address AS address
    FROM candidates candidate
    JOIN landlord l ON l.individual_subject_identifier = candidate.subject
    JOIN property_ownership p ON p.id = candidate.property_id
    JOIN owner_counts owners ON owners.landlordship_id = p.id AND owners.owner_count = 1
    JOIN address a ON a.id = p.address_id
    WHERE l.is_active
      AND l.individual_has_accepted_privacy_notice
      AND l.individual_subject_identifier LIKE 'urn:fdc:gov.uk:2022:%'
      AND p.id > 26
      AND p.is_active
      AND p.is_occupied
      AND p.marked_joint_landlord IS NOT TRUE
      AND NOT EXISTS (
          SELECT 1
          FROM letting_agent_access access
          WHERE access.property_ownership_id = p.id
      )
      AND NOT EXISTS (
          SELECT 1
          FROM landlord_incomplete_properties incomplete
          WHERE incomplete.user_id = l.individual_subject_identifier
      )
    ORDER BY l.individual_subject_identifier, p.id
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
            'ownerSubject', subject,
            'propertyOwnershipId', eligible.property_id,
            'files', COALESCE(fixture_files.files, '[]'::json),
            'address', address
        )
        ORDER BY subject
    ),
    '[]'::json
)
FROM eligible
LEFT JOIN fixture_files ON fixture_files.property_id = eligible.property_id;
