WITH owners AS (
    SELECT lip.user_id
    FROM landlord_incomplete_properties lip
    JOIN landlord l ON l.individual_subject_identifier = lip.user_id
    WHERE l.is_active AND l.individual_has_accepted_privacy_notice
      AND lip.user_id LIKE 'urn:fdc:gov.uk:2022:%'
    GROUP BY lip.user_id
    HAVING count(*) >= 2
    ORDER BY lip.user_id
    LIMIT 101
), fixtures AS (
    SELECT owners.user_id AS subject,
           first_state.journey_id AS "resumeJourneyId",
           first_state.serialized_state::jsonb #>> '{journeyData,select-address,address}' AS address,
           second_state.journey_id AS "deleteJourneyId"
    FROM owners
    CROSS JOIN LATERAL (
        SELECT s.*
        FROM landlord_incomplete_properties lip
        JOIN saved_journey_state s ON s.id = lip.saved_journey_state_id
        WHERE lip.user_id = owners.user_id AND s.subject_identifier = owners.user_id
        ORDER BY s.created_date, s.id LIMIT 1
    ) first_state
    CROSS JOIN LATERAL (
        SELECT s.*
        FROM landlord_incomplete_properties lip
        JOIN saved_journey_state s ON s.id = lip.saved_journey_state_id
        WHERE lip.user_id = owners.user_id AND s.subject_identifier = owners.user_id
        ORDER BY s.created_date, s.id LIMIT 1 OFFSET 1
    ) second_state
)
SELECT COALESCE(json_agg(fixtures ORDER BY subject), '[]'::json) FROM fixtures;
