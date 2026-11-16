WITH fixtures AS (
    SELECT DISTINCT ON (l.id)
           u.subject_identifier AS subject,
           l.id AS "organisationId",
           r.number AS "registrationNumber",
           l.organisation_landlord_name AS name
    FROM organisational_landlord_user u
    JOIN landlord l ON l.id = u.organisation_landlord_id
    JOIN registration_number r ON r.id = l.registration_number_id
    WHERE l.landlord_type = 1 AND l.id > 26 AND u.role = 0
      AND u.subject_identifier LIKE 'urn:fdc:gov.uk:2022:%'
      AND EXISTS (
          SELECT 1 FROM organisational_landlord_user editor
          WHERE editor.organisation_landlord_id = l.id AND editor.role = 1
      )
      AND NOT EXISTS (
          SELECT 1 FROM landlord individual
          WHERE individual.individual_subject_identifier = u.subject_identifier
      )
    ORDER BY l.id, u.id
    LIMIT 101
)
SELECT COALESCE(json_agg(fixtures ORDER BY "organisationId"), '[]'::json) FROM fixtures;
