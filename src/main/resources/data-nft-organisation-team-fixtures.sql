-- Reserve sequential correctness actors after the concurrent profile and team pools.
-- Invitations are created through the HTTP team journey, never by SQL.
WITH admins AS (
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
), allocated AS (
    SELECT * FROM admins ORDER BY "organisationId" OFFSET 118 LIMIT 17
)
SELECT COALESCE(json_agg(json_strip_nulls(row_to_json(fixture)) ORDER BY "organisationId"), '[]'::json)
FROM (
    SELECT a.*, invitation.token::text AS "invitationToken", invitation.invited_email AS "invitedEmail"
    FROM allocated a
    LEFT JOIN organisational_landlord_invitation invitation ON invitation.organisation_landlord_id = a."organisationId"
      AND NOT invitation.is_hidden
      AND invitation.created_date >= current_timestamp - interval '1 day'
      AND invitation.invited_email LIKE 'org-team-%-' || a."organisationId" || '-kept@example.com'
      AND invitation.invited_email NOT LIKE '%-load-%'
) fixture;
