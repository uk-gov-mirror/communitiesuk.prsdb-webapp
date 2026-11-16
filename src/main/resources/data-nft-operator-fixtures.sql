-- Read-only. One synthetic operator and one exclusive synthetic administrator
-- per pass. Even administrator IDs are reserved for council actors.
WITH operators AS (
    SELECT subject_identifier AS subject, row_number() OVER (ORDER BY id) AS ordinal
    FROM system_operator
    WHERE id > 50 AND subject_identifier LIKE 'urn:fdc:gov.uk:2022:%'
      AND NOT EXISTS (
          SELECT 1 FROM landlord l WHERE l.individual_subject_identifier = system_operator.subject_identifier
      )
      AND NOT EXISTS (
          SELECT 1 FROM local_council_user c WHERE c.subject_identifier = system_operator.subject_identifier
      )
      AND NOT EXISTS (
          SELECT 1 FROM organisational_landlord_user u WHERE u.subject_identifier = system_operator.subject_identifier
      )
    ORDER BY id LIMIT 101
), targets AS (
    SELECT u.id AS "targetId", u.name AS "targetName", u.email AS "targetEmail",
           u.subject_identifier AS "targetSubject", u.is_manager AS "targetManager",
           u.local_council_id AS "councilId", c.name AS "councilName",
           row_number() OVER (ORDER BY u.id) AS ordinal
    FROM local_council_user u
    JOIN local_council c ON c.id = u.local_council_id
    WHERE u.id > 100 AND u.id % 2 = 1 AND u.is_manager AND u.email LIKE '%@example.com'
    ORDER BY u.id LIMIT 101
), fixtures AS (
    SELECT o.subject, t."targetId", t."targetName", t."targetEmail", t."targetSubject",
           t."targetManager", t."councilId", t."councilName"
    FROM operators o JOIN targets t USING (ordinal)
)
SELECT COALESCE(json_agg(fixtures ORDER BY subject), '[]'::json) FROM fixtures;
