WITH owner_pool AS MATERIALIZED (
    SELECT id FROM landlord
    WHERE individual_subject_identifier LIKE 'urn:fdc:gov.uk:2022:%'
    ORDER BY individual_subject_identifier
    OFFSET 2200 LIMIT 200
), invitee_pool AS MATERIALIZED (
    SELECT id FROM landlord
    WHERE individual_subject_identifier LIKE 'urn:fdc:gov.uk:2022:%'
    ORDER BY individual_subject_identifier
    OFFSET 2400 LIMIT 200
), invitation_emails AS MATERIALIZED (
    SELECT DISTINCT lower(invited_email) AS email FROM joint_landlord_invitation
), unique_invitees AS MATERIALIZED (
    -- Only invitations whose email identifies exactly one landlord can be attributed to an invitee.
    SELECT min(landlord.id) AS id, invitation_emails.email
    FROM landlord
    JOIN invitation_emails ON lower(landlord.individual_email) = invitation_emails.email
    GROUP BY invitation_emails.email
    HAVING count(*) = 1
), candidates AS (
    SELECT DISTINCT ON (invitation.registered_propertyid)
           owner.individual_subject_identifier AS owner_subject,
           invitation.registered_propertyid AS property_id,
           address.single_line_address AS property_address,
           invitation.id AS invitation_id,
           invitation.token,
           invitee.individual_subject_identifier AS invitee_subject,
           invitation.invited_email
    FROM joint_landlord_invitation invitation
    JOIN property_ownership property ON property.id = invitation.registered_propertyid
    JOIN address ON address.id = property.address_id
    JOIN ownership_link owner_link ON owner_link.landlordship_id = property.id
    JOIN landlord owner ON owner.id = owner_link.landlord_id
    JOIN owner_pool ON owner_pool.id = owner.id
    JOIN unique_invitees ON unique_invitees.email = lower(invitation.invited_email)
    JOIN landlord invitee ON invitee.id = unique_invitees.id
    JOIN invitee_pool ON invitee_pool.id = invitee.id
    WHERE invitation.created_date >= current_timestamp - interval '1 day'
      AND invitation.is_hidden IS FALSE
      AND invitation.invitation_expired_email_sent IS FALSE
      AND owner.is_active
      AND owner.individual_has_accepted_privacy_notice
      AND owner.individual_subject_identifier LIKE 'urn:fdc:gov.uk:2022:%'
      AND invitee.is_active
      AND invitee.individual_has_accepted_privacy_notice
      AND invitee.individual_subject_identifier LIKE 'urn:fdc:gov.uk:2022:%'
      AND invitee.id <> owner.id
      AND NOT EXISTS (
          SELECT 1
          FROM ownership_link existing_owner
          WHERE existing_owner.landlordship_id = property.id
            AND existing_owner.landlord_id = invitee.id
      )
      AND NOT EXISTS (
          SELECT 1
          FROM landlord_incomplete_properties incomplete
          WHERE incomplete.user_id = invitee.individual_subject_identifier
      )
    ORDER BY invitation.registered_propertyid, invitation.id, owner.individual_subject_identifier
), ranked AS (
    SELECT candidates.*,
           row_number() OVER (PARTITION BY owner_subject ORDER BY invitation_id) AS owner_rank,
           row_number() OVER (PARTITION BY invitee_subject ORDER BY invitation_id) AS invitee_rank
    FROM candidates
), eligible AS (
    SELECT *
    FROM ranked
    WHERE owner_rank = 1 AND invitee_rank = 1
    ORDER BY invitation_id
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
            'invitationId', invitation_id,
            'token', token,
            'inviteeSubject', invitee_subject,
            'invitedEmail', invited_email
        )
        ORDER BY invitation_id
    ),
    '[]'::json
)
FROM eligible
LEFT JOIN fixture_files ON fixture_files.property_id = eligible.property_id;
