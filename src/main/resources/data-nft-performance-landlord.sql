-- Apply after bulk seeding, before creating the reusable performance dump.
DO $$
DECLARE
    fixture_subject TEXT := 'urn:fdc:gov.uk:2022:mGHDySEVfCsvfvc6lVWf6Qt9Dv0ZxPQWKoEzcjnBlUo';
    candidate_landlord_id BIGINT;
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM landlord WHERE individual_subject_identifier = fixture_subject
    ) THEN
        SELECT id
        INTO candidate_landlord_id
        FROM landlord l
        WHERE l.landlord_type = 0
          AND l.is_active
          AND l.individual_has_accepted_privacy_notice
          AND NOT l.individual_is_verified
          AND l.individual_subject_identifier IS NOT NULL
          AND EXISTS (
              SELECT 1 FROM ownership_link ol
              JOIN property_ownership po ON po.id = ol.landlordship_id
              WHERE ol.landlord_id = l.id AND po.is_active
          )
        ORDER BY l.id
        LIMIT 1;

        IF candidate_landlord_id IS NULL THEN
            RAISE EXCEPTION 'Cannot find an active individual landlord with a property for the Gatling fixture subject';
        END IF;

        INSERT INTO prsdb_user (id, created_date)
        VALUES (fixture_subject, current_timestamp)
        ON CONFLICT (id) DO NOTHING;

        UPDATE landlord
        SET individual_subject_identifier = fixture_subject
        WHERE id = candidate_landlord_id;
    END IF;
END $$;
